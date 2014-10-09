package us.ihmc.commonWalkingControlModules.configurations;

import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MomentumOptimizationSettings;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.yoUtilities.controllers.YoOrientationPIDGains;
import us.ihmc.yoUtilities.controllers.YoPDGains;
import us.ihmc.yoUtilities.controllers.YoSE3PIDGains;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;


public interface WalkingControllerParameters extends HeadOrientationControllerParameters
{
   public abstract SideDependentList<RigidBodyTransform> getDesiredHandPosesWithRespectToChestFrame();

   public abstract String[] getDefaultChestOrientationControlJointNames();

   public abstract boolean checkOrbitalEnergyCondition();

   public abstract double getGroundReactionWrenchBreakFrequencyHertz();

   public abstract boolean resetDesiredICPToCurrentAtStartOfSwing();

   public abstract double getFootForwardOffset();

   public abstract double getFootBackwardOffset();

   public abstract double getFootWidth();

   public abstract double getToeWidth();

   public abstract double getFootLength();

   public abstract double getAnkleHeight();

   public abstract double getLegLength();

   public abstract double getMinLegLengthBeforeCollapsingSingleSupport();

   public abstract double minimumHeightAboveAnkle();

   public abstract double nominalHeightAboveAnkle();

   public abstract double maximumHeightAboveAnkle();

   public abstract boolean finishSwingWhenTrajectoryDone();

   public abstract boolean stayOnToes();

   public abstract boolean doToeOffIfPossible();

   public abstract double getMaximumToeOffAngle();

   public abstract double getFootSwitchCoPThresholdFraction();

   public abstract boolean doToeTouchdownIfPossible();

   public abstract double getToeTouchdownAngle();

   public abstract boolean doHeelTouchdownIfPossible();

   public abstract double getHeelTouchdownAngle();

   public abstract double getFinalToeOffPitchAngularVelocity();

   public abstract double getInPlaceWidth();

   public abstract double getDesiredStepForward();

   public abstract double getMaxStepLength();

   public abstract double getMinStepWidth();

   public abstract double getMaxStepWidth();

   public abstract double getStepPitch();

   public abstract double getCaptureKpParallelToMotion();

   public abstract double getCaptureKpOrthogonalToMotion();

   public abstract double getCaptureKi();

   public abstract double getCaptureKiBleedoff();

   public abstract double getCaptureFilterBreakFrequencyInHz();

   public abstract double getCMPRateLimit();

   public abstract double getCMPAccelerationLimit();

   public abstract double getDefaultDesiredPelvisPitch();

   public abstract YoOrientationPIDGains createPelvisOrientationControlGains(YoVariableRegistry registry);

   public abstract YoPDGains createCoMHeightControlGains(YoVariableRegistry registry);

   public abstract boolean getCoMHeightDriftCompensation();

   public abstract double getTrajectoryTimeHeadOrientation();

   public abstract double getKpUpperBody();

   public abstract double getZetaUpperBody();

   public abstract double getMaxAccelerationUpperBody();

   public abstract double getMaxJerkUpperBody();

   public abstract YoOrientationPIDGains createChestControlGains(YoVariableRegistry registry);
   
   public abstract YoSE3PIDGains createSwingFootControlGains(YoVariableRegistry registry);

   public abstract YoSE3PIDGains createHoldPositionFootControlGains(YoVariableRegistry registry);

   public abstract YoSE3PIDGains createToeOffFootControlGains(YoVariableRegistry registry);

   public abstract YoSE3PIDGains createSupportFootControlGains(YoVariableRegistry registry);

   public abstract double getSwingHeightMaxForPushRecoveryTrajectory();

   public abstract double getSupportSingularityEscapeMultiplier();

   public abstract double getSwingSingularityEscapeMultiplier();

   public abstract boolean doPrepareManipulationForLocomotion();

   public abstract boolean isRunningOnRealRobot();

   public abstract double getDefaultTransferTime();

   public abstract double getDefaultSwingTime();

   public abstract double getPelvisPitchUpperLimit();

   public abstract double getPelvisPitchLowerLimit();

   public abstract boolean isPelvisPitchReversed();

   public abstract double getFoot_start_toetaper_from_back();

   public abstract double getSideLengthOfBoundingBoxForFootstepHeight();

   public abstract double getDesiredTouchdownVelocity();

   public abstract double getContactThresholdForce();

   public abstract double getCoPThresholdFraction();

   public abstract String[] getJointsToIgnoreInController();

   public abstract void setupMomentumOptimizationSettings(MomentumOptimizationSettings momentumOptimizationSettings);

}