package us.ihmc.sensorProcessing.stateEstimation.evaluation;

import java.util.ArrayList;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.controlFlow.ControlFlowGraph;
import us.ihmc.sensorProcessing.simulatedSensors.InverseDynamicsJointsFromSCSRobotGenerator;
import us.ihmc.sensorProcessing.simulatedSensors.SensorFilterParameters;
import us.ihmc.sensorProcessing.simulatedSensors.SensorNoiseParameters;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReader;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReaderFactory;
import us.ihmc.sensorProcessing.simulatedSensors.SimulatedSensorHolderAndReaderFromRobotFactory;
import us.ihmc.sensorProcessing.stateEstimation.DesiredCoMAccelerationsFromRobotStealerController;
import us.ihmc.sensorProcessing.stateEstimation.JointAndIMUSensorDataSource;
import us.ihmc.sensorProcessing.stateEstimation.PointMeasurementNoiseParameters;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimationDataFromController;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorWithPorts;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.AfterJointReferenceFrameNameMap;

import com.yobotics.simulationconstructionset.IMUMount;
import com.yobotics.simulationconstructionset.Joint;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.simulatedSensors.WrenchCalculatorInterface;


public class ComposableStateEstimatorEvaluator
{
   private static final boolean SHOW_GUI = true;
   private final boolean assumePerfectIMU = false;
   private final boolean useSimpleComEstimator = false;
   private final double simDT = 1e-3;
   private final int simTicksPerControlDT = 5;
   private final double controlDT = simDT * simTicksPerControlDT;
   private final int simTicksPerRecord = simTicksPerControlDT;

   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private static final boolean INITIALIZE_ANGULAR_VELOCITY_ESTIMATE_TO_ACTUAL = true;    // false;

   public ComposableStateEstimatorEvaluator()
   {
      StateEstimatorEvaluatorRobot robot = new StateEstimatorEvaluatorRobot();

      InverseDynamicsJointsFromSCSRobotGenerator generator = new InverseDynamicsJointsFromSCSRobotGenerator(robot);
      ArrayList<IMUMount> imuMounts = new ArrayList<IMUMount>();
      robot.getIMUMounts(imuMounts);

      FullInverseDynamicsStructure inverseDynamicsStructure = generator.getInverseDynamicsStructure();

//      SensorNoiseParameters simulatedSensorNoiseParameters = SensorNoiseParametersForEvaluator.createVeryLittleSensorNoiseParameters();
      SensorNoiseParameters simulatedSensorNoiseParameters = SensorNoiseParametersForEvaluator.createSensorNoiseParameters();
//      SensorNoiseParameters simulatedSensorNoiseParameters = SensorNoiseParametersForEvaluator.createZeroNoiseParameters();

      double jointPositionFilterFrequencyInHertz = 12.0;
      double jointVelocityFilterFrequencyInHertz = 12.0; // 16.0
      double orientationFilterFrequencyInHertz = 12.0;
      double angularVelocityFilterFrequencyInHertz = 12.0;
      double linearAccelerationFilterFrequencyInHertz = 12.0;
      double jointVelocitySlopTimeForBacklashCompensation = 0.007;
      final SensorFilterParameters sensorFilterParameters = new SensorFilterParameters(jointPositionFilterFrequencyInHertz, jointVelocityFilterFrequencyInHertz,
            orientationFilterFrequencyInHertz, angularVelocityFilterFrequencyInHertz, linearAccelerationFilterFrequencyInHertz,
            jointVelocitySlopTimeForBacklashCompensation, controlDT);
      
      SensorReaderFactory simulatedSensorHolderAndReaderFromRobotFactory = new SimulatedSensorHolderAndReaderFromRobotFactory(robot,
            simulatedSensorNoiseParameters, sensorFilterParameters, imuMounts, new ArrayList<WrenchCalculatorInterface>(), registry);
      
      boolean addLinearAccelerationSensors = true;
      
      simulatedSensorHolderAndReaderFromRobotFactory.build(inverseDynamicsStructure.getRootJoint(), null, addLinearAccelerationSensors, registry);
      
      SensorReader simulatedSensorHolderAndReader = simulatedSensorHolderAndReaderFromRobotFactory.getSensorReader();
      
      Joint estimationJoint = robot.getRootJoint();
      robot.update();

      ReferenceFrame estimationFrame = inverseDynamicsStructure.getEstimationFrame();

      double comAccelerationProcessNoiseStandardDeviation = simulatedSensorNoiseParameters.getComAccelerationProcessNoiseStandardDeviation();
      double angularAccelerationProcessNoiseStandardDeviation = simulatedSensorNoiseParameters.getAngularAccelerationProcessNoiseStandardDeviation();
      DesiredCoMAccelerationsFromRobotStealerController desiredCoMAccelerationsFromRobotStealerController =
         new DesiredCoMAccelerationsFromRobotStealerController(estimationFrame, comAccelerationProcessNoiseStandardDeviation,
               angularAccelerationProcessNoiseStandardDeviation, generator, estimationJoint, controlDT);

      double gravitationalAcceleration = robot.getGravityZ();

      // The following few lines are what you need to do to get the state estimator working with a robot.
      // You also need to either add the controlFlowGraph to another one, or make sure to run it's startComputation method at the right time:
//      SensorNoiseParameters sensorNoiseParametersForEstimator = SensorNoiseParametersForEvaluator.createVeryLittleSensorNoiseParameters();
//      SensorNoiseParameters sensorNoiseParametersForEstimator = SensorNoiseParametersForEvaluator.createSensorNoiseParameters();
//      SensorNoiseParameters sensorNoiseParametersForEstimator = SensorNoiseParametersForEvaluator.createLotsOfSensorNoiseParameters();
      final SensorNoiseParameters sensorNoiseParametersForEstimator = SensorNoiseParametersForEvaluator.createTunedNoiseParametersForEvaluator();

      RigidBodyToIndexMap rigidBodyToIndexMap = new RigidBodyToIndexMap(inverseDynamicsStructure.getElevator());

      AfterJointReferenceFrameNameMap referenceFrameMap = new AfterJointReferenceFrameNameMap(inverseDynamicsStructure.getElevator());
      final StateEstimationDataFromController stateEstimatorDataFromControllerSource = new StateEstimationDataFromController(estimationFrame);
      final StateEstimationDataFromController stateEstimationDataFromControllerSink = new StateEstimationDataFromController(estimationFrame);
      
      double pointVelocityXYMeasurementStandardDeviation = 2.0;
      double pointPositionZMeasurementStandardDeviation = 0.1;
      double pointVelocityZMeasurementStandardDeviation = 2.0;
      double pointPositionXYMeasurementStandardDeviation = 0.1;
      
      final PointMeasurementNoiseParameters pointMeasurementNoiseParameters = new  PointMeasurementNoiseParameters(
            pointVelocityXYMeasurementStandardDeviation,
            pointVelocityZMeasurementStandardDeviation,
            pointPositionXYMeasurementStandardDeviation,
            pointPositionZMeasurementStandardDeviation);
      
      StateEstimatorParameters stateEstimatorParameters = new StateEstimatorParameters()
      {
         public boolean useKinematicsBasedStateEstimator()
         {
            return false;
         }
         
         public boolean isRunningOnRealRobot()
         {
            return false;
         }
         
         public SensorNoiseParameters getSensorNoiseParameters()
         {
            return sensorNoiseParametersForEstimator;
         }
         
         public SensorFilterParameters getSensorFilterParameters()
         {
            return sensorFilterParameters;
         }
         
         public PointMeasurementNoiseParameters getPointMeasurementNoiseParameters()
         {
            return pointMeasurementNoiseParameters;
         }
         
         public double getEstimatorDT()
         {
            return controlDT;
         }
         
         public boolean getAssumePerfectIMU()
         {
            return assumePerfectIMU;
         }
      };
      
      SensorAndEstimatorAssembler sensorAndEstimatorAssembler = new SensorAndEstimatorAssembler(stateEstimatorDataFromControllerSource,
            simulatedSensorHolderAndReaderFromRobotFactory.getStateEstimatorSensorDefinitions(), stateEstimatorParameters, gravitationalAcceleration,
            inverseDynamicsStructure, referenceFrameMap, rigidBodyToIndexMap, registry);

      ControlFlowGraph controlFlowGraph = sensorAndEstimatorAssembler.getControlFlowGraph();
      StateEstimatorWithPorts orientationEstimator = sensorAndEstimatorAssembler.getEstimator();
      JointAndIMUSensorDataSource jointSensorDataSource = sensorAndEstimatorAssembler.getJointAndIMUSensorDataSource();
      
      simulatedSensorHolderAndReader.setJointAndIMUSensorDataSource(jointSensorDataSource);

      desiredCoMAccelerationsFromRobotStealerController.attachStateEstimationDataFromControllerSink(stateEstimationDataFromControllerSink);
      

      ControlFlowGraphExecutorController controlFlowGraphExecutorController = new ControlFlowGraphExecutorController(controlFlowGraph);
      
      StateEstimatorErrorCalculatorController composableStateEstimatorEvaluatorController =
         new StateEstimatorErrorCalculatorController(orientationEstimator, robot, estimationJoint, assumePerfectIMU, useSimpleComEstimator);
      
      
      robot.setController(desiredCoMAccelerationsFromRobotStealerController, simTicksPerControlDT);
      RunnableRunnerController runnableRunnerController = new RunnableRunnerController();
      runnableRunnerController.addRunnable((Runnable) simulatedSensorHolderAndReader);
      runnableRunnerController.addRunnable(new Runnable()
      {
         
         public void run()
         {
            stateEstimatorDataFromControllerSource.set(stateEstimationDataFromControllerSink);
         }
      });
      robot.setController(runnableRunnerController, simTicksPerControlDT);
      
      
      robot.setController(controlFlowGraphExecutorController, simTicksPerControlDT);
      robot.setController(composableStateEstimatorEvaluatorController, simTicksPerControlDT);

      if (INITIALIZE_ANGULAR_VELOCITY_ESTIMATE_TO_ACTUAL)
      {
         boolean updateRootJoints = true;
         boolean updateDesireds = false;
         generator.updateInverseDynamicsRobotModelFromRobot(updateRootJoints, updateDesireds);


         Matrix3d rotationMatrix = new Matrix3d();
         robot.getRootJoint().getRotationToWorld(rotationMatrix);
         Vector3d angularVelocityInBody = robot.getRootJoint().getAngularVelocityInBody();

         FrameOrientation estimatedOrientation = new FrameOrientation(ReferenceFrame.getWorldFrame());
         orientationEstimator.getEstimatedOrientation(estimatedOrientation);
         estimatedOrientation.set(rotationMatrix);
         orientationEstimator.setEstimatedOrientation(estimatedOrientation);

         FrameVector estimatedAngularVelocity = new FrameVector();
         orientationEstimator.getEstimatedAngularVelocity(estimatedAngularVelocity);
         estimatedAngularVelocity.set(angularVelocityInBody);
         orientationEstimator.setEstimatedAngularVelocity(estimatedAngularVelocity);

         // TODO: This wasn't doing anything.
         // DenseMatrix64F x = orientationEstimator.getState();
         // MatrixTools.insertTuple3dIntoEJMLVector(angularVelocityInBody, x, 3);
         // orientationEstimator.setState(x, orientationEstimator.getCovariance());
      }

      SimulationConstructionSet scs = new SimulationConstructionSet(robot, SHOW_GUI, 32000);
      scs.addYoVariableRegistry(registry);

      scs.setDT(simDT, simTicksPerRecord);
      scs.setSimulateDuration(45.0);
      scs.startOnAThread();
      scs.simulate();
   }

   public static void main(String[] args)
   {
      new ComposableStateEstimatorEvaluator();
   }

}
