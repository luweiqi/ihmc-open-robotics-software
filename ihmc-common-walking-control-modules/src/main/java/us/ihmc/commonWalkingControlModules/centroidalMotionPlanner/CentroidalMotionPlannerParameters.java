package us.ihmc.commonWalkingControlModules.centroidalMotionPlanner;

import us.ihmc.euclid.tuple3D.Vector3D;

public class CentroidalMotionPlannerParameters
{
   private Vector3D gravity = new Vector3D();
   private double robotMass;
   private double forceRegulizationWeight;
   private double dForceRegularizationWeight;
   private double nominalIxx;
   private double nominalIyy;
   private double nominalIzz;
   private double deltaTMin;

   public double getNominalIxx()
   {
      return nominalIxx;
   }

   public void setNominalIxx(double nominalIxx)
   {
      this.nominalIxx = nominalIxx;
   }

   public double getNominalIyy()
   {
      return nominalIyy;
   }

   public void setNominalIyy(double nominalIyy)
   {
      this.nominalIyy = nominalIyy;
   }

   public double getNominalIzz()
   {
      return nominalIzz;
   }

   public void setNominalIzz(double nominalIzz)
   {
      this.nominalIzz = nominalIzz;
   }

   public double getDeltaTMin()
   {
      return deltaTMin;
   }

   public void setDeltaTMin(double deltaTMin)
   {
      this.deltaTMin = deltaTMin;
   }

   public double getGravityX()
   {
      return gravity.getX();
   }

   public void setGravityX(double gravityX)
   {
      this.gravity.setX(gravityX);
   }

   public double getGravityY()
   {
      return gravity.getY();
   }

   public void setGravityY(double gravityY)
   {
      this.gravity.setY(gravityY);
   }

   public double getGravityZ()
   {
      return gravity.getZ();
   }

   public void setGravityZ(double gravityZ)
   {
      this.gravity.setZ(gravityZ);
   }

   public double getRobotMass()
   {
      return robotMass;
   }

   public void setRobotMass(double robotMass)
   {
      this.robotMass = robotMass;
   }

   public double getForceRegulizationWeight()
   {
      return forceRegulizationWeight;
   }

   public void setForceRegulizationWeight(double forceRegulizationWeight)
   {
      this.forceRegulizationWeight = forceRegulizationWeight;
   }

   public double getdForceRegularizationWeight()
   {
      return dForceRegularizationWeight;
   }

   public void setdForceRegularizationWeight(double dForceRegularizationWeight)
   {
      this.dForceRegularizationWeight = dForceRegularizationWeight;
   }

   public void setGravity(Vector3D gravity)
   {
      this.gravity.set(gravity);
   }
   
   public void getGravity(Vector3D gravityToPack)
   {
      gravityToPack.set(this.gravity);
   }
}
