package us.ihmc.exampleSimulations.genericQuadruped.controller.force;

      import controller_msgs.msg.dds.*;
      import org.junit.Test;
      import us.ihmc.commons.PrintTools;
      import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
      import us.ihmc.euclid.tuple3D.Point3D;
      import us.ihmc.exampleSimulations.genericQuadruped.GenericQuadrupedTestFactory;
      import us.ihmc.exampleSimulations.genericQuadruped.parameters.GenericQuadrupedDefaultInitialPosition;
      import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
      import us.ihmc.quadrupedRobotics.QuadrupedTestFactory;
      import us.ihmc.quadrupedRobotics.communication.QuadrupedMessageTools;
      import us.ihmc.quadrupedRobotics.controller.force.QuadrupedTowrTrajectoryTest;
      import us.ihmc.quadrupedRobotics.model.QuadrupedInitialPositionParameters;
      import us.ihmc.quadrupedRobotics.planning.trajectoryConverter.QuadrupedTowrTrajectoryConverter;
      import us.ihmc.quadrupedRobotics.planning.trajectoryConverter.TowrCartesianStates;
      import us.ihmc.quadrupedRobotics.util.TimeInterval;
      import us.ihmc.robotics.robotSide.RobotQuadrant;
      import us.ihmc.ros2.Ros2Node;
      import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;

      import java.io.IOException;
      import java.util.ArrayList;
      import java.util.List;

public class GenericQuadrupedTowrDurationTest extends QuadrupedTowrTrajectoryTest
{

   TowrCartesianStates towrCartesianStates = new TowrCartesianStates(200);
   @Override
   public QuadrupedTestFactory createQuadrupedTestFactory()
   {
      return new GenericQuadrupedTestFactory();
   }

   public QuadrupedInitialPositionParameters getInitialPositionParameters()
   {
      return new GenericQuadrupedDefaultInitialPosition()
   {
      @Override
      public Point3D getInitialBodyPosition()
      {
         return new Point3D(0.0, 0.0, 0.52);
      }


      @Override
      public double getHipRollAngle()
      {
         return 0.3;
      }
   };
   }


   @Override
   @ContinuousIntegrationTest(estimatedDuration = 74.7)
   @Test(timeout = 370000)
   public void testQuadrupedTowrTrajectory() throws BlockingSimulationRunner.SimulationExceededMaximumTimeException
   {
      super.testQuadrupedTowrTrajectory();
   }

   @Override
   public Point3D getFinalPlanarPosition()
   {
      return new Point3D(1.684, 0.077, 0.0);
   }

   //private final SideDependentList<RobotStateCartesianTrajectory> subscribers = new SideDependentList<>();
   int durationValue = 0;

   public static void subscribeToTopic() throws IOException, InterruptedException
   {
      Ros2Node node = new Ros2Node(PubSubImplementation.FAST_RTPS, "Ros2ListenerExample");
      node.createSubscription(Duration.getPubSubType().get(), subscriber -> {
         Duration durationMessage = new Duration();
         if (subscriber.takeNextData(durationMessage, null)) {
            System.out.println(durationMessage.getSec());
         }
      }, "duration_topic");

      //Thread.currentThread().join(); // keep thread alive to receive more messages
   }

   public boolean listenToTowr()
   {
      boolean messageReceived = false;
      boolean useLoggedTrajectories = false;
      if (!useLoggedTrajectories)
      {
         try
         {
            towrCartesianStates = QuadrupedTowrTrajectoryConverter.subscribeToTowrRobotStateCartesianTrajectory();
            QuadrupedTowrTrajectoryConverter.printTowrTrajectory(towrCartesianStates);
            //QuadrupedTowrTrajectoryConverter.printDataSet(towrCartesianStates);
            PrintTools.info("received trajectory from towr!");
            messageReceived = true;
         }
         catch (Exception e)
         {
         }
      }
      else
      {
         towrCartesianStates = QuadrupedTowrTrajectoryConverter.loadExistingDataSet();
         PrintTools.info("load predefined trajectory!");
         messageReceived = true;
         //QuadrupedTowrTrajectoryConverter.printTowrTrajectory(towrCartesianStates);
      }

      return messageReceived;
   }


   @Override
   public QuadrupedTimedStepListMessage getSteps()
   {
      try
      {
         subscribeToTopic();
      }
      catch (Exception e)
      {
      }

      PrintTools.info("initial base pos TOWR:"+durationValue);

      ArrayList<QuadrupedTimedStepMessage> steps = new ArrayList<>();
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.HIND_RIGHT, new Point3D(-0.550, -0.100, -0.012), 0.1, new TimeInterval(0.200, 0.530)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_LEFT, new Point3D(0.550, 0.100, -0.012), 0.1, new TimeInterval(0.200, 0.530)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.HIND_LEFT, new Point3D(-0.547, 0.100, -0.012), 0.1, new TimeInterval(0.630, 0.960)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_RIGHT, new Point3D(0.553, -0.100, -0.012), 0.1, new TimeInterval(0.630, 0.960)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.HIND_RIGHT, new Point3D(-0.544, -0.101, -0.012), 0.1, new TimeInterval(1.060, 1.390)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_LEFT, new Point3D(0.556, 0.099, -0.012), 0.1, new TimeInterval(1.060, 1.390)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.HIND_LEFT, new Point3D(-0.541, 0.096, -0.012), 0.1, new TimeInterval(1.490, 1.820)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_RIGHT, new Point3D(0.559, -0.104, -0.012), 0.1, new TimeInterval(1.490, 1.820)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_LEFT, new Point3D(0.890, 0.096, -0.000), 0.1, new TimeInterval(2.710, 3.040)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.HIND_RIGHT, new Point3D(0.005, -0.104, -0.000), 0.1, new TimeInterval(2.925, 3.255)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_RIGHT, new Point3D(1.311, -0.100, -0.000), 0.1, new TimeInterval(3.140, 3.470)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.HIND_LEFT, new Point3D(0.380, 0.109, -0.000), 0.1, new TimeInterval(3.355, 3.685)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_LEFT, new Point3D(1.486, 0.118, -0.000), 0.1, new TimeInterval(3.570, 3.900)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.HIND_RIGHT, new Point3D(0.595, -0.073, -0.000), 0.1, new TimeInterval(3.785, 4.115)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_RIGHT, new Point3D(1.912, -0.066, -0.000), 0.1, new TimeInterval(4.000, 4.330)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.HIND_LEFT, new Point3D(1.023, 0.149, -0.000), 0.1, new TimeInterval(4.215, 4.545)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_LEFT, new Point3D(2.284, 0.137, -0.000), 0.1, new TimeInterval(4.430, 4.760)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.HIND_RIGHT, new Point3D(1.404, -0.083, -0.000), 0.1, new TimeInterval(4.645, 4.975)));
      steps.add(QuadrupedMessageTools
                      .createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_RIGHT, new Point3D(2.708, -0.080, -0.000), 0.1, new TimeInterval(4.860, 5.190)));

      QuadrupedTimedStepListMessage message = QuadrupedMessageTools.createQuadrupedTimedStepListMessage(steps, false);
      return message;
   }

   @Override
   public CenterOfMassTrajectoryMessage getCenterOfMassTrajectoryMessage(){
      QuadrupedTowrTrajectoryConverter towrTrajectoryConverter = new QuadrupedTowrTrajectoryConverter();
      CenterOfMassTrajectoryMessage message = new CenterOfMassTrajectoryMessage();
      return message;
   }

   @Override
   public QuadrupedBodyHeightMessage getBodyHeightMessage()
   {

      TowrCartesianStates towrCartesianStates = new TowrCartesianStates(200);
      return QuadrupedTowrTrajectoryConverter.createBodyHeightMessage(towrCartesianStates);
   }
}