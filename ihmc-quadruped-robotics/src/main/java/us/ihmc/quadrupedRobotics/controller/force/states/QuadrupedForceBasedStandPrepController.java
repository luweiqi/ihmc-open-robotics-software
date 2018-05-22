package us.ihmc.quadrupedRobotics.controller.force.states;

import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.quadrupedRobotics.controlModules.QuadrupedControlManagerFactory;
import us.ihmc.quadrupedRobotics.controlModules.foot.QuadrupedFeetManager;
import us.ihmc.quadrupedRobotics.controller.ControllerEvent;
import us.ihmc.quadrupedRobotics.controller.QuadrupedController;
import us.ihmc.quadrupedRobotics.controller.force.QuadrupedForceControllerToolbox;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.QuadrupedTaskSpaceController;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.QuadrupedWaypointCallback;
import us.ihmc.quadrupedRobotics.estimator.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.quadrupedRobotics.planning.ContactState;
import us.ihmc.quadrupedRobotics.planning.QuadrupedSoleWaypointList;
import us.ihmc.quadrupedRobotics.planning.SoleWaypoint;
import us.ihmc.robotModels.FullQuadrupedRobotModel;
import us.ihmc.robotics.partNames.JointRole;
import us.ihmc.robotics.partNames.QuadrupedJointName;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.sensorProcessing.outputData.JointDesiredControlMode;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputList;
import us.ihmc.yoVariables.parameters.BooleanParameter;
import us.ihmc.yoVariables.parameters.DoubleParameter;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;

public class QuadrupedForceBasedStandPrepController implements QuadrupedController, QuadrupedWaypointCallback
{
   //Yo Variables
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final DoubleParameter trajectoryTimeParameter = new DoubleParameter("trajectoryTime", registry, 1.0);
   private final DoubleParameter stanceLengthParameter = new DoubleParameter("stanceLength", registry, 1.0);
   private final DoubleParameter stanceWidthParameter = new DoubleParameter("stanceWidth", registry, 0.35);
   private final DoubleParameter stanceHeightParameter = new DoubleParameter("stanceHeight", registry, 0.6);
   private final DoubleParameter stanceXOffsetParameter = new DoubleParameter("stanceXOffset", registry, 0.05);
   private final DoubleParameter stanceYOffsetParameter = new DoubleParameter("stanceYOffset", registry, 0.0);
   private final DoubleParameter stancePitchParameter = new DoubleParameter("stancePitch", registry, 0.0);
   private final DoubleParameter jointDampingParameter = new DoubleParameter("jointDamping", registry, 15.0);
   private final DoubleParameter jointPositionLimitDampingParameter = new DoubleParameter("jointPositionLimitDamping", registry, 10.0);
   private final DoubleParameter jointPositionLimitStiffnessParameter = new DoubleParameter("jointPositionLimitStiffness", registry, 100.0);
   private final BooleanParameter requestUseForceFeedbackControlParameter = new BooleanParameter("requestUseForceFeedbackControl", registry, false);

   // Yo variables
   private final YoBoolean forceFeedbackControlEnabled;

   // Task space controller
   private final QuadrupedTaskSpaceController.Commands taskSpaceControllerCommands;
   private final QuadrupedTaskSpaceController.Settings taskSpaceControllerSettings;
   private final QuadrupedTaskSpaceController taskSpaceController;

   private final QuadrantDependentList<QuadrupedSoleWaypointList> quadrupedSoleWaypointLists = new QuadrantDependentList<>();
   private final QuadrupedFeetManager feetManager;
   private final QuadrupedReferenceFrames referenceFrames;
   private FramePoint3D solePositionSetpoint;
   private final Vector3D zeroVelocity;
   private final double robotLength;
   private final FullQuadrupedRobotModel fullRobotModel;

   private final YoBoolean isDoneMoving = new YoBoolean("standPrepDoneMoving", registry);

   private final QuadrupedForceControllerToolbox controllerToolbox;
   private final JointDesiredOutputList jointDesiredOutputList;

   public QuadrupedForceBasedStandPrepController(QuadrupedForceControllerToolbox controllerToolbox, QuadrupedControlManagerFactory controlManagerFactory,
                                                 YoVariableRegistry parentRegistry)
   {
      this.controllerToolbox = controllerToolbox;
      this.jointDesiredOutputList = controllerToolbox.getRuntimeEnvironment().getJointDesiredOutputList();

      feetManager = controlManagerFactory.getOrCreateFeetManager();
      referenceFrames = controllerToolbox.getReferenceFrames();
      solePositionSetpoint = new FramePoint3D();
      for (RobotQuadrant quadrant : RobotQuadrant.values)
      {
         QuadrupedSoleWaypointList quadrupedSoleWaypointList = new QuadrupedSoleWaypointList();
         quadrupedSoleWaypointList.add(new SoleWaypoint());
         quadrupedSoleWaypointList.add(new SoleWaypoint());

         quadrupedSoleWaypointLists.set(quadrant, quadrupedSoleWaypointList);
      }
      zeroVelocity = new Vector3D(0, 0, 0);
      taskSpaceControllerCommands = new QuadrupedTaskSpaceController.Commands();
      taskSpaceControllerSettings = new QuadrupedTaskSpaceController.Settings();
      this.taskSpaceController = controllerToolbox.getTaskSpaceController();
      forceFeedbackControlEnabled = new YoBoolean("forceFeedbackControlEnabled", registry);

      // Calculate the robot length
      referenceFrames.updateFrames();
      FramePoint3D frontLeftHipRollFrame = new FramePoint3D();
      frontLeftHipRollFrame.setToZero(referenceFrames.getLegAttachmentFrame(RobotQuadrant.FRONT_LEFT));
      frontLeftHipRollFrame.changeFrame(referenceFrames.getBodyFrame());
      FramePoint3D hindLeftHipRollFrame = new FramePoint3D();
      hindLeftHipRollFrame.setToZero(referenceFrames.getLegAttachmentFrame(RobotQuadrant.HIND_LEFT));
      hindLeftHipRollFrame.changeFrame(referenceFrames.getBodyFrame());
      robotLength = frontLeftHipRollFrame.getX() - hindLeftHipRollFrame.getX();
      fullRobotModel = controllerToolbox.getRuntimeEnvironment().getFullRobotModel();
      parentRegistry.addChild(registry);
   }

   @Override
   public void isDoneMoving(boolean doneMoving)
   {
      boolean done = doneMoving && isDoneMoving.getBooleanValue();
      isDoneMoving.set(done);
   }

   @Override
   public void onEntry()
   {
      controllerToolbox.update();
      // Create sole waypoint trajectories
      for (RobotQuadrant quadrant : RobotQuadrant.values)
      {
         solePositionSetpoint.setIncludingFrame(controllerToolbox.getTaskSpaceEstimates().getSolePosition(quadrant));
         solePositionSetpoint.changeFrame(referenceFrames.getBodyFrame());
         quadrupedSoleWaypointLists.get(quadrant).get(0).set(solePositionSetpoint, zeroVelocity, 0.0);
         solePositionSetpoint.setToZero(referenceFrames.getBodyFrame());
         solePositionSetpoint.add(quadrant.getEnd().negateIfHindEnd(stanceLengthParameter.getValue() / 2.0), 0.0, 0.0);
         solePositionSetpoint.add(0.0, quadrant.getSide().negateIfRightSide(stanceWidthParameter.getValue() / 2.0), 0.0);
         solePositionSetpoint.add(stanceXOffsetParameter.getValue(), stanceYOffsetParameter.getValue(),
               quadrant.getEnd().negateIfHindEnd(Math.sin(stancePitchParameter.getValue())) * robotLength / 2 - stanceHeightParameter.getValue());
         quadrupedSoleWaypointLists.get(quadrant).get(1).set(solePositionSetpoint, zeroVelocity, trajectoryTimeParameter.getValue());
      }
      feetManager.initializeWaypointTrajectory(quadrupedSoleWaypointLists, false);

      // Initialize task space controller
      taskSpaceControllerSettings.initialize();
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointDamping(jointDampingParameter.getValue());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointPositionLimitDamping(jointPositionLimitDampingParameter.getValue());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointPositionLimitStiffness(jointPositionLimitStiffnessParameter.getValue());
      for (RobotQuadrant quadrant : RobotQuadrant.values)
      {
         taskSpaceControllerSettings.setContactState(quadrant, ContactState.NO_CONTACT);
      }
      taskSpaceController.reset();

      // Initialize force feedback
      forceFeedbackControlEnabled.set(requestUseForceFeedbackControlParameter.getValue());
      for (OneDoFJoint oneDoFJoint : fullRobotModel.getOneDoFJoints())
      {
         QuadrupedJointName jointName = fullRobotModel.getNameForOneDoFJoint(oneDoFJoint);
         if (oneDoFJoint != null && jointName.getRole().equals(JointRole.LEG))
         {
            if (forceFeedbackControlEnabled.getValue())
               jointDesiredOutputList.getJointDesiredOutput(oneDoFJoint).setControlMode(JointDesiredControlMode.EFFORT);
            else
               jointDesiredOutputList.getJointDesiredOutput(oneDoFJoint).setControlMode(JointDesiredControlMode.POSITION);
         }
      }

      feetManager.registerWaypointCallback(this);
   }

   @Override
   public ControllerEvent process()
   {
      controllerToolbox.update();
      feetManager.compute(taskSpaceControllerCommands.getSoleForce());
      taskSpaceController.compute(taskSpaceControllerSettings, taskSpaceControllerCommands);
      return isDoneMoving.getBooleanValue() ? ControllerEvent.DONE : null;
   }

   @Override
   public void onExit()
   {
      forceFeedbackControlEnabled.set(false);  // This bool should match that used in the standReady controller (freeze)
      for (OneDoFJoint oneDoFJoint : fullRobotModel.getOneDoFJoints())
      {
         QuadrupedJointName jointName = fullRobotModel.getNameForOneDoFJoint(oneDoFJoint);
         if (oneDoFJoint != null && jointName.getRole().equals(JointRole.LEG))
         {
            if (forceFeedbackControlEnabled.getBooleanValue())
               jointDesiredOutputList.getJointDesiredOutput(oneDoFJoint).setControlMode(JointDesiredControlMode.EFFORT);
            else
               jointDesiredOutputList.getJointDesiredOutput(oneDoFJoint).setControlMode(JointDesiredControlMode.POSITION);
         }
      }

      feetManager.registerWaypointCallback(null);
   }
}