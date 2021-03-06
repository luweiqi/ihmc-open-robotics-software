package us.ihmc.commonWalkingControlModules.wrenchDistribution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.spatial.Wrench;

/**
 * @author twan
 *         Date: 5/1/13
 */
public class ContactPointWrenchMatrixCalculator
{
   private final ReferenceFrame centerOfMassFrame;

   private final DenseMatrix64F q;
   private final Map<RigidBodyBasics, Wrench> wrenches = new LinkedHashMap<RigidBodyBasics, Wrench>();

   // intermediate result storage:
   private final ArrayList<FrameVector3D> normalizedSupportVectors = new ArrayList<FrameVector3D>(4);
   private final FramePoint3D tempContactPoint = new FramePoint3D(ReferenceFrame.getWorldFrame());
   private final FrameVector3D tempVector = new FrameVector3D(ReferenceFrame.getWorldFrame());
   private final DenseMatrix64F qBlock = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F rhoBlock = new DenseMatrix64F(1, 1);
   private final DenseMatrix64F wrenchMatrix = new DenseMatrix64F(Wrench.SIZE, 1);
   private final DenseMatrix64F rhoMin;

   public ContactPointWrenchMatrixCalculator(ReferenceFrame centerOfMassFrame, int nSupportVectorsPerContactPoint, int nColumns)
   {
      this.centerOfMassFrame = centerOfMassFrame;

      for (int i = 0; i < nSupportVectorsPerContactPoint; i++)
      {
         normalizedSupportVectors.add(new FrameVector3D(ReferenceFrame.getWorldFrame()));
      }
      q = new DenseMatrix64F(Wrench.SIZE, nColumns);
      rhoMin = new DenseMatrix64F(nColumns, 1);
   }

   public DenseMatrix64F getRhoMin(Collection<? extends PlaneContactState> contactStates, double rhoMinScalar)
   {
      rhoMin.zero();
      int index = 0;
      for (PlaneContactState contactState : contactStates)
      {
         for (int i = 0; i < contactState.getNumberOfContactPointsInContact(); i++)
         {
            for (int j = 0; j < normalizedSupportVectors.size(); j++)
            {
               rhoMin.set(index++, 0, rhoMinScalar);
            }
         }
      }

      return rhoMin;
   }

   public void computeMatrix(Collection<? extends PlaneContactState> contactStates)
   {
      q.zero();

      int column = 0;
      for (PlaneContactState contactState : contactStates)
      {
         List<FramePoint2D> contactPoints2d = contactState.getContactFramePoints2dInContactCopy();
         WrenchDistributorTools.getSupportVectors(normalizedSupportVectors, contactState.getCoefficientOfFriction(), contactState.getPlaneFrame()); // TODO: use normal

         for (FramePoint2D contactPoint2d : contactPoints2d)
         {
            // torque part of A
            tempContactPoint.setIncludingFrame(contactPoint2d.getReferenceFrame(), contactPoint2d.getX(), contactPoint2d.getY(), 0.0);
            tempContactPoint.changeFrame(centerOfMassFrame);

            for (FrameVector3D supportVector : normalizedSupportVectors)
            {
               supportVector.changeFrame(centerOfMassFrame);
               supportVector.get(3, column, q);

               tempVector.setToZero(centerOfMassFrame);
               tempVector.cross(tempContactPoint, supportVector);
               tempVector.get(0, column, q);
               column++;
            }
         }
      }
   }

   public DenseMatrix64F getMatrix()
   {
      return q;
   }

   public Map<RigidBodyBasics, Wrench> computeWrenches(LinkedHashMap<RigidBodyBasics, ? extends PlaneContactState> contactStates, DenseMatrix64F rho)
   {
      wrenches.clear();
      int columnNumber = 0;
      for (RigidBodyBasics rigidBody : contactStates.keySet())
      {
         PlaneContactState contactState = contactStates.get(rigidBody);

         int nColumns = contactState.getNumberOfContactPointsInContact() * normalizedSupportVectors.size();
         if (nColumns > 0)
         {
            qBlock.reshape(Wrench.SIZE, nColumns);
            CommonOps.extract(q, 0, Wrench.SIZE, columnNumber, columnNumber + nColumns, qBlock, 0, 0);

            rhoBlock.reshape(nColumns, 1);
            CommonOps.extract(rho, columnNumber, columnNumber + nColumns, 0, 1, rhoBlock, 0, 0);

            CommonOps.mult(qBlock, rhoBlock, wrenchMatrix);

            Wrench wrench = new Wrench(rigidBody.getBodyFixedFrame(), centerOfMassFrame);
            wrench.setIncludingFrame(centerOfMassFrame, wrenchMatrix);
            wrench.changeFrame(rigidBody.getBodyFixedFrame());
            wrenches.put(rigidBody, wrench);
            columnNumber += nColumns;
         }
      }
      return wrenches;
   }
}
