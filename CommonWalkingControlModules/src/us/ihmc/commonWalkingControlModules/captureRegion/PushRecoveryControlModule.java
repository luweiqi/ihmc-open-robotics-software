package us.ihmc.commonWalkingControlModules.captureRegion;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Transform3D;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepUtils;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.ICPAndMomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.trajectories.ConstantSwingTimeCalculator;
import us.ihmc.commonWalkingControlModules.trajectories.SwingTimeCalculator;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.statemachines.StateMachine;
import com.yobotics.simulationconstructionset.util.statemachines.StateTransitionCondition;

public class PushRecoveryControlModule
{
   private static final double MINIMUM_TIME_BEFORE_RECOVER_WITH_REDUCED_POLYGON = 2;
   private static final double FAST_SWING_TIME_FOR_CAPTURE_REGION_CALCULATOR = 0.4;
   private static final double DOUBLESUPPORT_SUPPORT_POLYGON_SCALE = 0.8;
   private static final double FAST_SWING_TIME = 0.3;
   private static final double TRUST_TIME_SCALE = 0.9;

   private final BooleanYoVariable enablePushRecoveryFromDoubleSupport;
   private final ICPAndMomentumBasedController icpAndMomentumBasedController;
   private final MomentumBasedController momentumBasedController;
   private final OrientationStateVisualizer orientationStateVisualizer;
   private final FootstepAdjustor footstepAdjustor;
   private final OneStepCaptureRegionCalculator captureRegionCalculator;
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final BooleanYoVariable footstepWasProjectedInCaptureRegion;
   private final BooleanYoVariable enablePushRecovery;
   private final CommonWalkingReferenceFrames referenceFrames;
   private final ConstantSwingTimeCalculator swingTimeCalculator;
   private final StateMachine stateMachine;

   private boolean recoveringFromDoubleSupportFall;
   private Footstep recoverFromDoubleSupportFallFootStep;
   private BooleanYoVariable readyToGrabNextFootstep;
   private double defaultSwingTime;

   public PushRecoveryControlModule(DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, YoVariableRegistry registry,
         MomentumBasedController momentumBasedController, WalkingControllerParameters walkingControllerParameters, BooleanYoVariable readyToGrabNextFootstep,
         ICPAndMomentumBasedController icpAndMomentumBasedController, BooleanYoVariable enablePushRecovery, CommonWalkingReferenceFrames referenceFrames,
         SwingTimeCalculator swingTimeCalculator, StateMachine stateMachine)
   {
      this.momentumBasedController = momentumBasedController;
      this.readyToGrabNextFootstep = readyToGrabNextFootstep;
      this.icpAndMomentumBasedController = icpAndMomentumBasedController;
      this.enablePushRecovery = enablePushRecovery;
      this.referenceFrames = referenceFrames;
      this.swingTimeCalculator = (ConstantSwingTimeCalculator)swingTimeCalculator;
      this.stateMachine = stateMachine;
      
      this.enablePushRecoveryFromDoubleSupport = new BooleanYoVariable("enablePushRecoveryFromDoubleSupport", registry);
      this.enablePushRecoveryFromDoubleSupport.set(false);
      this.defaultSwingTime = walkingControllerParameters.getDefaultSwingTime();

      captureRegionCalculator = new OneStepCaptureRegionCalculator(momentumBasedController.getReferenceFrames(), walkingControllerParameters, registry,
            dynamicGraphicObjectsListRegistry);
      footstepAdjustor = new FootstepAdjustor(registry, dynamicGraphicObjectsListRegistry);
      orientationStateVisualizer = new OrientationStateVisualizer(dynamicGraphicObjectsListRegistry, registry);

      footstepWasProjectedInCaptureRegion = new BooleanYoVariable("footstepWasProjectedInCaptureRegion", registry);

      reset();
   }

   public void reset()
   {
      footstepWasProjectedInCaptureRegion.set(false);
      recoverFromDoubleSupportFallFootStep = null;
      captureRegionCalculator.hideCaptureRegion();

      if (recoveringFromDoubleSupportFall)
      {
         this.swingTimeCalculator.setSwingTime(this.defaultSwingTime);
         recoveringFromDoubleSupportFall = false;
      }
   }

   public class IsFallingFromDoubleSupport implements StateTransitionCondition
   {
      private final FramePoint2d capturePoint2d = new FramePoint2d();
      private RobotSide swingSide = null;
      private RobotSide transferToSide = null;

      private Transform3D fromWorldToPelvis = new Transform3D();
      private Transform3D scaleTransformation = new Transform3D();
      private FrameConvexPolygon2d reducedSupportPolygon;
      private ReferenceFrame midFeetZUp;
      private double capturePointYAxis;
      private FrameVector projectedCapturePoint;
      private Footstep currentFootstep = null;
      private boolean isICPOutside;

      public IsFallingFromDoubleSupport(RobotSide robotSide)
      {
         this.transferToSide = robotSide;
         this.scaleTransformation.setScale(DOUBLESUPPORT_SUPPORT_POLYGON_SCALE);
         this.reducedSupportPolygon = new FrameConvexPolygon2d(icpAndMomentumBasedController.getBipedSupportPolygons().getSupportPolygonInMidFeetZUp());
         this.projectedCapturePoint = new FrameVector(worldFrame, 0, 0, 0);
      }

      @Override
      public boolean checkCondition()
      {
         if (enablePushRecovery.getBooleanValue())
         {
            if (getDoubleSupportEnableState())
            {
               midFeetZUp = icpAndMomentumBasedController.getBipedSupportPolygons().getSupportPolygonInMidFeetZUp().getReferenceFrame();

               // get current robot status
               reducedSupportPolygon.changeFrame(midFeetZUp);
               reducedSupportPolygon.setAndUpdate(icpAndMomentumBasedController.getBipedSupportPolygons().getSupportPolygonInMidFeetZUp());
               icpAndMomentumBasedController.getCapturePoint().getFrameTuple2dIncludingFrame(capturePoint2d);

               capturePoint2d.changeFrame(midFeetZUp);
               reducedSupportPolygon.applyTransform(scaleTransformation);

               // update the visualization
               fromWorldToPelvis = momentumBasedController.getFullRobotModel().getPelvis().getBodyFixedFrame().getTransformToDesiredFrame(worldFrame);
               orientationStateVisualizer.updatePelvisReferenceFrame(fromWorldToPelvis);
               orientationStateVisualizer.updateReducedSupportPolygon(reducedSupportPolygon);
               
               if (stateMachine.timeInCurrentState() < MINIMUM_TIME_BEFORE_RECOVER_WITH_REDUCED_POLYGON)
               {
                  isICPOutside = !icpAndMomentumBasedController.getBipedSupportPolygons().getSupportPolygonInMidFeetZUp().isPointInside(capturePoint2d);
               }
               else
               {
                  isICPOutside = !reducedSupportPolygon.isPointInside(capturePoint2d);
               }

               if (isICPOutside && recoverFromDoubleSupportFallFootStep == null)
               {
                  System.out.println("Robot is falling from double support");
                  projectedCapturePoint.changeFrame(capturePoint2d.getReferenceFrame());
                  projectedCapturePoint.set(capturePoint2d.getX(), capturePoint2d.getY(), 0);
                  projectedCapturePoint.changeFrame(momentumBasedController.getFullRobotModel().getPelvis().getBodyFixedFrame());
                  capturePointYAxis = projectedCapturePoint.getY();
                  if (capturePointYAxis >= 0)
                  {
                     swingSide = RobotSide.LEFT;
                  }
                  else
                  {
                     swingSide = RobotSide.RIGHT;
                  }

                  if (transferToSide == swingSide)
                  {
                     return false;
                  }

                  captureRegionCalculator.calculateCaptureRegion(
                        swingSide,
                        FAST_SWING_TIME_FOR_CAPTURE_REGION_CALCULATOR,
                        capturePoint2d,
                        icpAndMomentumBasedController.getOmega0(),
                        computeFootPolygon(swingSide.getOppositeSide(),
                        momentumBasedController.getReferenceFrames().getAnkleZUpFrame(swingSide.getOppositeSide())));
                  
                  currentFootstep = FootstepUtils.getCurrentFootstep(swingSide, referenceFrames, momentumBasedController.getContactablePlaneFeet());
                  
                  footstepAdjustor.adjustFootstep(currentFootstep, captureRegionCalculator.getCaptureRegion());
                  readyToGrabNextFootstep.set(false);
                  momentumBasedController.getUpcomingSupportLeg().set(transferToSide.getOppositeSide());
                  recoverFromDoubleSupportFallFootStep = currentFootstep;
                  recoveringFromDoubleSupportFall = true;
                     
                  swingTimeCalculator.setSwingTime(FAST_SWING_TIME);
          
                  return true;
               }

               // we need this to reset the reference frame 
               reducedSupportPolygon.changeFrame(worldFrame);
               return false;
            }
         }

         return false;
      }
   }

   public boolean checkAndUpdateFootstep(RobotSide swingSide, double swingTimeRemaining, FramePoint2d capturePoint2d, Footstep nextFootstep, double omega0,
         FrameConvexPolygon2d footPolygon)
   {
      if (enablePushRecovery.getBooleanValue() && !recoveringFromDoubleSupportFall)
      {
         // TODO: find a way to prevent to many replans
//         if (footstepWasProjectedInCaptureRegion.getBooleanValue())
//         {
//            // can not re-plan again
//            return false;
//         }

         captureRegionCalculator.calculateCaptureRegion(swingSide, swingTimeRemaining, capturePoint2d, omega0, footPolygon);
         footstepWasProjectedInCaptureRegion.set(footstepAdjustor.adjustFootstep(nextFootstep, captureRegionCalculator.getCaptureRegion()));

         return footstepWasProjectedInCaptureRegion.getBooleanValue();
      }
      return false;
   }

   private FrameConvexPolygon2d computeFootPolygon(RobotSide robotSide, ReferenceFrame referenceFrame)
   {
      final List<FramePoint> tempContactPoints = new ArrayList<FramePoint>();
      final FrameConvexPolygon2d tempFootPolygon = new FrameConvexPolygon2d(worldFrame);

      momentumBasedController.getContactPoints(momentumBasedController.getContactablePlaneFeet().get(robotSide), tempContactPoints);
      tempFootPolygon.setIncludingFrameByProjectionOntoXYPlaneAndUpdate(referenceFrame, tempContactPoints);

      return tempFootPolygon;
   }

   public boolean getDoubleSupportEnableState()
   {
      return enablePushRecoveryFromDoubleSupport.getBooleanValue();
   }
   
   public double getFastSwingTime()
   {
      return FAST_SWING_TIME;
   }

   public boolean getIsRecoveringFromDoubleSupportFall()
   {
      return recoveringFromDoubleSupportFall;
   }

   public Footstep getRecoverFromDoubleSupportFootStep()
   {
      return recoverFromDoubleSupportFallFootStep;
   }
   
   public double getTrustTimeToConsiderSwingFinished()
   {
      return getFastSwingTime()*TRUST_TIME_SCALE;
   }

   public void setRecoveringFromDoubleSupportState(boolean value)
   {
      recoveringFromDoubleSupportFall = value;
   }

   public void setRecoverFromDoubleSupportFootStep(Footstep recoverFootStep)
   {
      recoverFromDoubleSupportFallFootStep = recoverFootStep;
   }

}
