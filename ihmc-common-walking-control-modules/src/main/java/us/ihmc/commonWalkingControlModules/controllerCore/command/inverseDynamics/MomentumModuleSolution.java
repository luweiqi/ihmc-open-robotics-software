package us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics;

import java.util.List;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.mecano.spatial.SpatialForce;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.RigidBody;

public class MomentumModuleSolution
{
   private InverseDynamicsJoint[] jointsToOptimizeFor;
   private DenseMatrix64F jointAccelerations;
   private DenseMatrix64F rhoSolution;
   private SpatialForce centroidalMomentumRateSolution;
   private Map<RigidBody, Wrench> externalWrenchSolution;
   private List<RigidBody> rigidBodiesWithExternalWrench;

   public void setJointsToOptimizeFor(InverseDynamicsJoint[] jointsToOptimizeFor)
   {
      this.jointsToOptimizeFor = jointsToOptimizeFor;
   }

   public void setJointAccelerations(DenseMatrix64F jointAccelerations)
   {
      this.jointAccelerations = jointAccelerations;
   }
   public void setRhoSolution(DenseMatrix64F rhoSolution)
   {
      this.rhoSolution = rhoSolution;
   }

   public void setCentroidalMomentumRateSolution(SpatialForce centroidalMomentumRateSolution)
   {
      this.centroidalMomentumRateSolution = centroidalMomentumRateSolution;
   }

   public void setExternalWrenchSolution(Map<RigidBody, Wrench> externalWrenchSolution)
   {
      this.externalWrenchSolution = externalWrenchSolution;
   }

   public void setRigidBodiesWithExternalWrench(List<RigidBody> rigidBodiesWithExternalWrench)
   {
      this.rigidBodiesWithExternalWrench = rigidBodiesWithExternalWrench;
   }

   public SpatialForce getCentroidalMomentumRateSolution()
   {
      return centroidalMomentumRateSolution;
   }

   public Map<RigidBody, Wrench> getExternalWrenchSolution()
   {
      return externalWrenchSolution;
   }

   public List<RigidBody> getRigidBodiesWithExternalWrench()
   {
      return rigidBodiesWithExternalWrench;
   }

   public InverseDynamicsJoint[] getJointsToOptimizeFor()
   {
      return jointsToOptimizeFor;
   }

   public DenseMatrix64F getJointAccelerations()
   {
      return jointAccelerations;
   }

   public DenseMatrix64F getRhoSolution()
   {
      return rhoSolution;
   }
}
