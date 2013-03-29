package us.ihmc.commonWalkingControlModules.stateEstimation.measurementModelElements;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SpatialAccelerationCalculator;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

public class LinearAccelerationMeasurementModelJacobianAssembler
{
   private final TwistCalculator twistCalculator;
   private final SpatialAccelerationCalculator spatialAccelerationCalculator;

   private final RigidBody measurementLink;
   private final ReferenceFrame measurementFrame;
   private final ReferenceFrame estimationFrame;

   private final Matrix3d rotationFromWorldToMeasurement = new Matrix3d();

   private final Matrix3d omegaTilde = new Matrix3d();
   private final Matrix3d vTilde = new Matrix3d();
   private final Matrix3d pTilde = new Matrix3d();
   private final Matrix3d omegadTilde = new Matrix3d();
   private final Matrix3d zTildeRWP = new Matrix3d();

   private final Vector3d omega = new Vector3d();
   private final Vector3d v = new Vector3d();
   private final FramePoint p = new FramePoint(ReferenceFrame.getWorldFrame());
   private final Vector3d omegad = new Vector3d();

   private final Twist twistOfMeasurementLink = new Twist();
   private final SpatialAccelerationVector spatialAccelerationOfMeasurementLink = new SpatialAccelerationVector();

   private final Transform3D tempTransform = new Transform3D();
   private final Matrix3d tempMatrix = new Matrix3d();
   private final Matrix3d tempMatrix2 = new Matrix3d();

   public LinearAccelerationMeasurementModelJacobianAssembler(TwistCalculator twistCalculator, SpatialAccelerationCalculator spatialAccelerationCalculator,
           RigidBody measurementLink, ReferenceFrame measurementFrame, ReferenceFrame estimationFrame)
   {
      this.twistCalculator = twistCalculator;
      this.spatialAccelerationCalculator = spatialAccelerationCalculator;
      this.measurementLink = measurementLink;
      this.measurementFrame = measurementFrame;
      this.estimationFrame = estimationFrame;
   }

   public void preCompute(Vector3d estimatedMeasurement)
   {
      ReferenceFrame elevatorFrame = spatialAccelerationCalculator.getRootBody().getBodyFixedFrame();

      // T, Td
      twistCalculator.packTwistOfBody(twistOfMeasurementLink, measurementLink);
      spatialAccelerationCalculator.packAccelerationOfBody(spatialAccelerationOfMeasurementLink, measurementLink);
      twistOfMeasurementLink.changeBaseFrameNoRelativeTwist(elevatorFrame);
      spatialAccelerationOfMeasurementLink.changeBaseFrameNoRelativeAcceleration(elevatorFrame);
      spatialAccelerationOfMeasurementLink.changeFrame(elevatorFrame, twistOfMeasurementLink, twistOfMeasurementLink);
      twistOfMeasurementLink.changeFrame(elevatorFrame);

      // \tilde{\omega}, \tilde{v}
      twistOfMeasurementLink.packAngularPart(omega);
      MatrixTools.toTildeForm(omegaTilde, omega);
      twistOfMeasurementLink.packLinearPart(v);
      MatrixTools.toTildeForm(vTilde, v);

      // \tilde{p}
      p.setToZero(measurementFrame);
      p.changeFrame(elevatorFrame);
      MatrixTools.toTildeForm(pTilde, p.getPoint());

      // \tilde{\omegad}
      spatialAccelerationOfMeasurementLink.packAngularPart(omegad);
      MatrixTools.toTildeForm(omegadTilde, omegad);

      // rotation matrix
      elevatorFrame.getTransformToDesiredFrame(tempTransform, measurementFrame);
      tempTransform.get(rotationFromWorldToMeasurement);

      // z
      estimationFrame.getTransformToDesiredFrame(tempTransform, elevatorFrame);
      tempTransform.get(tempMatrix);
      MatrixTools.toTildeForm(zTildeRWP, estimatedMeasurement);
      zTildeRWP.mul(tempMatrix);
   }

   public void assembleMeasurementJacobian(Matrix3d ret, Matrix3d jPhi, Matrix3d jOmega, Matrix3d jV, Matrix3d jOmegad, Matrix3d jVd, Matrix3d jP)
   {
      ret.setZero();

      if (jP != null)
      {
         tempMatrix.mul(omegaTilde, omegaTilde);
         tempMatrix.add(omegadTilde);
         tempMatrix.mul(jP);
         ret.add(tempMatrix);
      }

      if (jOmega != null)
      {
         tempMatrix.mul(omegaTilde, pTilde);
         tempMatrix.mul(2.0);
         tempMatrix2.mul(pTilde, omegaTilde);
         tempMatrix.sub(tempMatrix2);
         tempMatrix.add(vTilde);
         tempMatrix.mul(jOmega);
         ret.sub(tempMatrix);
      }

      if (jV != null)
      {
         tempMatrix.mul(omegaTilde, jV);
         ret.add(tempMatrix);
      }

      if (jOmegad != null)
      {
         tempMatrix.mul(pTilde, jOmegad);
         ret.sub(tempMatrix);
      }

      if (jVd != null)
      {
         ret.add(jVd);
      }

      ret.mul(rotationFromWorldToMeasurement, ret);

      if (jPhi != null)
      {
         tempMatrix.mul(zTildeRWP, jPhi);
         ret.add(tempMatrix);
      }
   }
}
