package us.ihmc.commonWalkingControlModules.controlModules.foot.toeOffCalculator;

import us.ihmc.robotics.geometry.*;
import us.ihmc.robotics.robotSide.RobotSide;


public interface ToeOffCalculator
{
   public void clear();

   public void setExitCMP(FramePoint exitCMP, RobotSide trailingLeg);

   public void computeToeOffContactPoint(FramePoint2d desiredCMP, RobotSide trailingLeg);

   public void getToeOffContactPoint(FramePoint2d contactPointToPack, RobotSide trailingLeg);
}
