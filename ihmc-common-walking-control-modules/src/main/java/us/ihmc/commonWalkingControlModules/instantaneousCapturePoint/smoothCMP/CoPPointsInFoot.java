package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMP;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.angularMomentumTrajectoryGenerator.CoPTrajectoryPoint;
import us.ihmc.commonWalkingControlModules.configurations.CoPPointName;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsList;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.graphicsDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFramePointInMultipleFrames;
import us.ihmc.robotics.math.trajectories.waypoints.FrameEuclideanTrajectoryPoint;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class CoPPointsInFoot
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final FrameVector3D zeroVector = new FrameVector3D();
   private static final FrameEuclideanTrajectoryPoint tempVariableForSetting = new FrameEuclideanTrajectoryPoint();
   private static final int maxNumberOfTrajectoryPoints = 10;
   
   private final List<CoPPointName> copPointsList = new ArrayList<>(maxNumberOfTrajectoryPoints); // List of CoP way points defined for this footstep. Hopefully this does not create garbage

   private final List<CoPTrajectoryPoint> copLocations = new ArrayList<>(maxNumberOfTrajectoryPoints); // Location of CoP points defined 
   private final List<YoFramePoint> copLocationsInWorldFrameReadOnly = new ArrayList<>(maxNumberOfTrajectoryPoints); // YoFramePoints for visualization

   private final YoFramePointInMultipleFrames swingFootCentroid;
   private final YoFramePointInMultipleFrames supportFootCentroid;

   public CoPPointsInFoot(int stepNumber, ReferenceFrame[] framesToRegister, YoVariableRegistry registry)
   {
      for (int i = 0; i < maxNumberOfTrajectoryPoints; i++)
      {
         CoPTrajectoryPoint constantCoP = new CoPTrajectoryPoint("step" + stepNumber + "CoP" + i, "", registry, framesToRegister);
         constantCoP.setToNaN();
         copLocations.add(constantCoP);
         copLocationsInWorldFrameReadOnly.add(constantCoP.buildUpdatedYoFramePointForVisualizationOnly());
      }
      swingFootCentroid = new YoFramePointInMultipleFrames("step" + stepNumber + "swingCentroid", registry, framesToRegister);
      supportFootCentroid = new YoFramePointInMultipleFrames("step" + stepNumber + "supportCentroid", registry, framesToRegister);
   }

   public void setupVisualizers(YoGraphicsList graphicsList, ArtifactList artifactList, double pointSize)
   {
      for (int i = 0; i < copLocationsInWorldFrameReadOnly.size(); i++)
      {
         YoFramePoint copLocation = copLocationsInWorldFrameReadOnly.get(i);
         YoGraphicPosition yoGraphicPosition = new YoGraphicPosition(copLocation.getNamePrefix(), copLocation, pointSize, YoAppearance.Green(),
                                                                     YoGraphicPosition.GraphicType.BALL);
         graphicsList.add(yoGraphicPosition);
         artifactList.add(yoGraphicPosition.createArtifact());
      }
   }

   public void notifyVariableChangedListeners()
   {
      for (int i = 0; i < copLocations.size(); i++)
         copLocations.get(i).notifyVariableChangedListeners();
   }

   public void reset()
   {
      swingFootCentroid.setToNaN();
      supportFootCentroid.setToNaN();
      copPointsList.clear();
      for (int i = 0; i < copLocations.size(); i++)
      {
         copLocations.get(i).setToNaN(worldFrame);
         copLocationsInWorldFrameReadOnly.get(i).setToNaN();
      }
   }

   public void addWayPoint(CoPPointName copPointName)
   {
      this.copPointsList.add(copPointName);
   }

   public void setIncludingFrame(int waypointIndex, double time, FramePoint3D location)
   {
      copLocations.get(waypointIndex).registerReferenceFrame(location.getReferenceFrame());
      zeroVector.setToZero(location.getReferenceFrame());;
      tempVariableForSetting.setIncludingFrame(time, location, zeroVector);
      copLocations.get(waypointIndex).setIncludingFrame(tempVariableForSetting);
   }

   public void setIncludingFrame(int waypointIndex, double time, YoFramePoint location)
   {
      copLocations.get(waypointIndex).registerReferenceFrame(location.getReferenceFrame());
      zeroVector.setToZero(location.getReferenceFrame());
      tempVariableForSetting.setIncludingFrame(time, location.getFrameTuple(), zeroVector);
      copLocations.get(waypointIndex).setIncludingFrame(tempVariableForSetting);
   }

   public void setIncludingFrame(int waypointIndex, double time, CoPTrajectoryPoint location)
   {
      copLocations.get(waypointIndex).registerReferenceFrame(location.getReferenceFrame());
      zeroVector.setToZero(location.getReferenceFrame());;
      tempVariableForSetting.setIncludingFrame(time, location.getPosition().getFrameTuple(), zeroVector);
      copLocations.get(waypointIndex).setIncludingFrame(tempVariableForSetting);
   }

   public void addAndSetIncludingFrame(CoPPointName copPointName, double time, FramePoint3D location)
   {
      setIncludingFrame(copPointsList.size(), time, location);
      addWayPoint(copPointName);
   }

   public void addAndSetIncludingFrame(CoPPointName copPointName, double time, YoFramePoint location)
   {
      setIncludingFrame(copPointsList.size(), time, location);
      addWayPoint(copPointName);
   }

   public void addAndSetIncludingFrame(CoPPointName copPointName, double time, CoPTrajectoryPoint location)
   {
      setIncludingFrame(copPointsList.size(), time, location);
      addWayPoint(copPointName);
   }

   public void setToNaN(int waypointIndex)
   {
      copLocations.get(waypointIndex).setToNaN();
   }

   public void setIncludingFrame(CoPPointsInFoot other)
   {
      this.swingFootCentroid.setIncludingFrame(other.swingFootCentroid);
      this.supportFootCentroid.setIncludingFrame(other.supportFootCentroid);
      for (int i = 0; i < other.copPointsList.size(); i++)
         this.copLocations.get(i).setIncludingFrame(other.get(i));
      this.copPointsList.addAll(other.copPointsList);
   }

   public CoPTrajectoryPoint get(int copPointIndex)
   {
      return copLocations.get(copPointIndex);
   }

   public YoFramePoint getWaypointInWorldFrameReadOnly(int copPointIndex)
   {
      return copLocationsInWorldFrameReadOnly.get(copPointIndex);
   }

   public void changeFrame(ReferenceFrame desiredFrame)
   {
      swingFootCentroid.changeFrame(desiredFrame);
      supportFootCentroid.changeFrame(desiredFrame);
      for (int i = 0; i < copPointsList.size(); i++)
         copLocations.get(i).changeFrame(desiredFrame);
   }

   public void registerReferenceFrame(ReferenceFrame newReferenceFrame)
   {
      swingFootCentroid.registerReferenceFrame(newReferenceFrame);
      supportFootCentroid.registerReferenceFrame(newReferenceFrame);
      for (int i = 0; i < copPointsList.size(); i++)
         copLocations.get(i).registerReferenceFrame(newReferenceFrame);
   }

   public void switchCurrentReferenceFrame(ReferenceFrame desiredFrame)
   {
      swingFootCentroid.switchCurrentReferenceFrame(desiredFrame);
      supportFootCentroid.switchCurrentReferenceFrame(desiredFrame);
      for (int i = 0; i < copPointsList.size(); i++)
         copLocations.get(i).switchCurrentReferenceFrame(desiredFrame);
   }

   public void switchCurrentReferenceFrame(int waypointIndex, ReferenceFrame desiredFrame)
   {
      copLocations.get(waypointIndex).switchCurrentReferenceFrame(desiredFrame);
   }

   public void setSwingFootLocation(FramePoint3D footLocation)
   {
      this.swingFootCentroid.setIncludingFrame(footLocation);
   }

   public void setSwingFootLocation(FramePoint2D footLocation)
   {
      this.swingFootCentroid.setIncludingFrame(footLocation, 0.0);
   }

   public void getSwingFootLocation(FramePoint3D footLocationToPack)
   {
      footLocationToPack.setIncludingFrame(swingFootCentroid.getFrameTuple());
   }

   public void setSupportFootLocation(FramePoint3D footLocation)
   {
      this.supportFootCentroid.setIncludingFrame(footLocation);
   }

   public void setSupportFootLocation(FramePoint2D footLocation)
   {
      this.supportFootCentroid.setIncludingFrame(footLocation, 0.0);
   }

   public void getSupportFootLocation(FramePoint3D footLocationToPack)
   {
      footLocationToPack.setIncludingFrame(supportFootCentroid.getFrameTuple());
   }
   
   public void setFeetLocation(FramePoint3D swingFootLocation, FramePoint3D supportFootLocation)
   {
      setSwingFootLocation(swingFootLocation);
      setSupportFootLocation(supportFootLocation);
   }

   public List<CoPPointName> getCoPPointList()
   {
      return copPointsList;
   }

   public void getFinalCoPPosition(FramePoint3D tempFinalICP)
   {
      copLocations.get(copPointsList.size() - 1).getPosition(tempFinalICP);
   }

   public boolean isEmpty()
   {
      return copPointsList.isEmpty();
   }

   public int getNumberOfCoPPoints()
   {
      return copPointsList.size();
   }
}
