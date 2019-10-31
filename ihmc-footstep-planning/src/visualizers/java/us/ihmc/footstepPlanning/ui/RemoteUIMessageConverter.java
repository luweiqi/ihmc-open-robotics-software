package us.ihmc.footstepPlanning.ui;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import controller_msgs.msg.dds.*;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.IHMCRealtimeROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ROS2Tools.MessageTopicNameGenerator;
import us.ihmc.communication.ROS2Tools.ROS2TopicQualifier;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.communication.packets.ToolboxState;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.*;
import us.ihmc.footstepPlanning.communication.FootstepPlannerCommunicationProperties;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.footstepPlanning.tools.FootstepPlannerMessageTools;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.pathPlanning.visibilityGraphs.VisibilityGraphMessagesConverter;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMapWithNavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.VisibilityGraphsParametersReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityMapHolder;
import us.ihmc.pubsub.DomainFactory;
import us.ihmc.robotEnvironmentAwareness.communication.REACommunicationProperties;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.ros2.RealtimeRos2Node;

/**
 * This class is required when using a local version of the Footstep Planner UI and the footstep planning algorithms
 * located in the Footstep Planner Toolbox. It allows users to view the resulting plans calculated by the toolbox. It
 * also allows the user to tune the planner parameters, and request a new plan from the planning toolbox.
 *
 * This class is used to convert the shared memory messages from Java FX used by the Footstep Planner UI
 * to RTPS messages, and vice-versa. It specifically listens to the planning request messages and the output
 * status messages sent to and from the toolbox, and visualizes those. It also allows the user to request a
 * new plan using the local version of the edit footstep planning parameters and a footstep request, requiring
 * conversion from the Java FX messages to the RTPS messages.
 */
public class RemoteUIMessageConverter
{
   private static final boolean verbose = false;

   private final RealtimeRos2Node ros2Node;

   private final Messager messager;

   private final String robotName;

   private final AtomicReference<FootstepPlannerParametersReadOnly> plannerParametersReference;
   private final AtomicReference<VisibilityGraphsParametersReadOnly> visibilityGraphParametersReference;
   private final AtomicReference<Point3D> plannerStartPositionReference;
   private final AtomicReference<Quaternion> plannerStartOrientationReference;
   private final AtomicReference<Point3D> plannerGoalPositionReference;
   private final AtomicReference<Quaternion> plannerGoalOrientationReference;
   private final AtomicReference<PlanarRegionsList> plannerPlanarRegionReference;
   private final AtomicReference<FootstepPlannerType> plannerTypeReference;
   private final AtomicReference<Double> plannerTimeoutReference;
   private final AtomicReference<RobotSide> plannerInitialSupportSideReference;
   private final AtomicReference<Integer> plannerRequestIdReference;
   private final AtomicReference<Double> plannerHorizonLengthReference;
   private final AtomicReference<Boolean> acceptNewPlanarRegionsReference;
   private final AtomicReference<Integer> currentPlanRequestId;
   private final AtomicReference<Boolean> assumeFlatGround;
   private final AtomicReference<Boolean> ignorePartialFootholds;
   private final AtomicReference<Double> goalDistanceProximity;
   private final AtomicReference<Double> goalYawProximity;

   private IHMCRealtimeROS2Publisher<ToolboxStateMessage> toolboxStatePublisher;
   private IHMCRealtimeROS2Publisher<FootstepPlannerParametersPacket> plannerParametersPublisher;
   private IHMCRealtimeROS2Publisher<VisibilityGraphsParametersPacket> visibilityGraphsParametersPublisher;
   private IHMCRealtimeROS2Publisher<FootstepPlanningRequestPacket> footstepPlanningRequestPublisher;
   private IHMCRealtimeROS2Publisher<PlanningStatisticsRequestMessage> plannerStatisticsRequestPublisher;
   private IHMCRealtimeROS2Publisher<FootstepDataListMessage> footstepDataListPublisher;
   private IHMCRealtimeROS2Publisher<GoHomeMessage> goHomePublisher;
   private IHMCRealtimeROS2Publisher<ToolboxStateMessage> walkingPreviewToolboxStatePublisher;
   private IHMCRealtimeROS2Publisher<WalkingControllerPreviewInputMessage> walkingPreviewRequestPublisher;

   public static RemoteUIMessageConverter createRemoteConverter(Messager messager, String robotName)
   {
      return createConverter(messager, robotName, DomainFactory.PubSubImplementation.FAST_RTPS);
   }

   public static RemoteUIMessageConverter createIntraprocessConverter(Messager messager, String robotName)
   {
      return createConverter(messager, robotName, DomainFactory.PubSubImplementation.INTRAPROCESS);
   }

   public static RemoteUIMessageConverter createConverter(Messager messager, String robotName, DomainFactory.PubSubImplementation implementation)
   {
      RealtimeRos2Node ros2Node = ROS2Tools.createRealtimeRos2Node(implementation, "ihmc_footstep_planner_ui");
      return new RemoteUIMessageConverter(ros2Node, messager, robotName);
   }

   public RemoteUIMessageConverter(RealtimeRos2Node ros2Node, Messager messager, String robotName)
   {
      this.messager = messager;
      this.robotName = robotName;
      this.ros2Node = ros2Node;

      plannerParametersReference = messager.createInput(FootstepPlannerMessagerAPI.PlannerParameters, null);
      visibilityGraphParametersReference = messager.createInput(FootstepPlannerMessagerAPI.VisibilityGraphsParameters, null);
      plannerStartPositionReference = messager.createInput(FootstepPlannerMessagerAPI.StartPosition);
      plannerStartOrientationReference = messager.createInput(FootstepPlannerMessagerAPI.StartOrientation, new Quaternion());
      plannerGoalPositionReference = messager.createInput(FootstepPlannerMessagerAPI.GoalPosition);
      plannerGoalOrientationReference = messager.createInput(FootstepPlannerMessagerAPI.GoalOrientation, new Quaternion());
      plannerPlanarRegionReference = messager.createInput(FootstepPlannerMessagerAPI.PlanarRegionData);
      plannerTypeReference = messager.createInput(FootstepPlannerMessagerAPI.PlannerType, FootstepPlannerType.A_STAR);
      plannerTimeoutReference = messager.createInput(FootstepPlannerMessagerAPI.PlannerTimeout, 5.0);
      plannerInitialSupportSideReference = messager.createInput(FootstepPlannerMessagerAPI.InitialSupportSide, RobotSide.LEFT);
      plannerRequestIdReference = messager.createInput(FootstepPlannerMessagerAPI.PlannerRequestId);
      plannerHorizonLengthReference = messager.createInput(FootstepPlannerMessagerAPI.PlannerHorizonLength);
      acceptNewPlanarRegionsReference = messager.createInput(FootstepPlannerMessagerAPI.AcceptNewPlanarRegions, true);
      currentPlanRequestId = messager.createInput(FootstepPlannerMessagerAPI.PlannerRequestId, 0);
      assumeFlatGround = messager.createInput(FootstepPlannerMessagerAPI.AssumeFlatGround, false);
      ignorePartialFootholds = messager.createInput(FootstepPlannerMessagerAPI.IgnorePartialFootholds, false);
      goalDistanceProximity = messager.createInput(FootstepPlannerMessagerAPI.GoalDistanceProximity, 0.0);
      goalYawProximity = messager.createInput(FootstepPlannerMessagerAPI.GoalYawProximity, 0.0);

      registerPubSubs(ros2Node);

      ros2Node.spin();
   }

   public void destroy()
   {
      ros2Node.destroy();
   }

   private void registerPubSubs(RealtimeRos2Node ros2Node)
   {
      /* subscribers */
      // we want to listen to the incoming request to the planning toolbox
      ROS2Tools.createCallbackSubscription(ros2Node, FootstepPlanningRequestPacket.class,
                                           FootstepPlannerCommunicationProperties.subscriberTopicNameGenerator(robotName),
                                           s -> processFootstepPlanningRequestPacket(s.takeNextData()));
      // we want to listen to the resulting body path plan from the toolbox
      ROS2Tools.createCallbackSubscription(ros2Node, BodyPathPlanMessage.class, FootstepPlannerCommunicationProperties.publisherTopicNameGenerator(robotName),
                                           s -> processBodyPathPlanMessage(s.takeNextData()));
      ROS2Tools.createCallbackSubscription(ros2Node, BodyPathPlanStatisticsMessage.class,
                                           FootstepPlannerCommunicationProperties.publisherTopicNameGenerator(robotName),
                                           s -> processBodyPathPlanStatistics(s.takeNextData()));
      ROS2Tools.createCallbackSubscription(ros2Node, FootstepPlannerStatusMessage.class,
                                           FootstepPlannerCommunicationProperties.publisherTopicNameGenerator(robotName),
                                           s -> processFootstepPlannerStatus(s.takeNextData()));
      // we want to listen to the resulting footstep plan from the toolbox
      ROS2Tools.createCallbackSubscription(ros2Node, FootstepPlanningToolboxOutputStatus.class,
                                           FootstepPlannerCommunicationProperties.publisherTopicNameGenerator(robotName),
                                           s -> processFootstepPlanningOutputStatus(s.takeNextData()));
      // we want to also listen to incoming REA planar region data.
      ROS2Tools.createCallbackSubscription(ros2Node, PlanarRegionsListMessage.class, REACommunicationProperties.publisherTopicNameGenerator,
                                           s -> processIncomingPlanarRegionMessage(s.takeNextData()));
      ROS2Tools.createCallbackSubscription(ros2Node, FootstepNodeDataListMessage.class,
                                           FootstepPlannerCommunicationProperties.publisherTopicNameGenerator(robotName),
                                           s -> messager.submitMessage(FootstepPlannerMessagerAPI.NodeData, s.takeNextData()));
      ROS2Tools.createCallbackSubscription(ros2Node, FootstepPlannerOccupancyMapMessage.class,
                                           FootstepPlannerCommunicationProperties.publisherTopicNameGenerator(robotName),
                                           s -> messager.submitMessage(FootstepPlannerMessagerAPI.OccupancyMap, s.takeNextData()));

      ROS2Tools.createCallbackSubscription(ros2Node, RobotConfigurationData.class,
                                           ControllerAPIDefinition.getPublisherTopicNameGenerator(robotName),
                                           s -> messager.submitMessage(FootstepPlannerMessagerAPI.RobotConfigurationData, s.takeNextData()));

      MessageTopicNameGenerator controllerPreviewOutputTopicNameGenerator = ROS2Tools.getTopicNameGenerator(robotName, ROS2Tools.WALKING_PREVIEW_TOOLBOX, ROS2TopicQualifier.OUTPUT);
      ROS2Tools.createCallbackSubscription(ros2Node, WalkingControllerPreviewOutputMessage.class, controllerPreviewOutputTopicNameGenerator, s -> messager.submitMessage(FootstepPlannerMessagerAPI.WalkingPreviewOutput, s.takeNextData()));

      // publishers
      plannerParametersPublisher = ROS2Tools
            .createPublisher(ros2Node, FootstepPlannerParametersPacket.class, FootstepPlannerCommunicationProperties.subscriberTopicNameGenerator(robotName));
      visibilityGraphsParametersPublisher = ROS2Tools
            .createPublisher(ros2Node, VisibilityGraphsParametersPacket.class, FootstepPlannerCommunicationProperties.subscriberTopicNameGenerator(robotName));
      toolboxStatePublisher = ROS2Tools.createPublisher(ros2Node, ToolboxStateMessage.class,
                                                        FootstepPlannerCommunicationProperties.subscriberTopicNameGenerator(robotName));
      footstepPlanningRequestPublisher = ROS2Tools
            .createPublisher(ros2Node, FootstepPlanningRequestPacket.class, FootstepPlannerCommunicationProperties.subscriberTopicNameGenerator(robotName));
      plannerStatisticsRequestPublisher = ROS2Tools
            .createPublisher(ros2Node, PlanningStatisticsRequestMessage.class, FootstepPlannerCommunicationProperties.subscriberTopicNameGenerator(robotName));
      footstepDataListPublisher = ROS2Tools.createPublisher(ros2Node, FootstepDataListMessage.class, ControllerAPIDefinition.getSubscriberTopicNameGenerator(robotName));
      goHomePublisher = ROS2Tools.createPublisher(ros2Node, GoHomeMessage.class, ControllerAPIDefinition.getSubscriberTopicNameGenerator(robotName));

      MessageTopicNameGenerator controllerPreviewInputTopicNameGenerator = ROS2Tools.getTopicNameGenerator(robotName, ROS2Tools.WALKING_PREVIEW_TOOLBOX, ROS2TopicQualifier.INPUT);
      walkingPreviewToolboxStatePublisher = ROS2Tools.createPublisher(ros2Node, ToolboxStateMessage.class, controllerPreviewInputTopicNameGenerator);
      walkingPreviewRequestPublisher = ROS2Tools.createPublisher(ros2Node, WalkingControllerPreviewInputMessage.class, controllerPreviewInputTopicNameGenerator);

      messager.registerTopicListener(FootstepPlannerMessagerAPI.ComputePath, request -> requestNewPlan());
      messager.registerTopicListener(FootstepPlannerMessagerAPI.RequestPlannerStatistics, request -> requestPlannerStatistics());
      messager.registerTopicListener(FootstepPlannerMessagerAPI.AbortPlanning, request -> requestAbortPlanning());
      messager.registerTopicListener(FootstepPlannerMessagerAPI.GoHomeTopic, goHomePublisher::publish);
      messager.registerTopicListener(FootstepPlannerMessagerAPI.RequestWalkingPreview, request ->
      {
         ToolboxStateMessage toolboxStateMessage = new ToolboxStateMessage();
         toolboxStateMessage.setRequestedToolboxState(ToolboxState.WAKE_UP.toByte());
         walkingPreviewToolboxStatePublisher.publish(toolboxStateMessage);
         walkingPreviewRequestPublisher.publish(request);
      });

      messager.registerTopicListener(FootstepPlannerMessagerAPI.FootstepPlanToRobot, footstepDataListMessage ->
      {
         if(ignorePartialFootholds.get())
         {
            footstepDataListMessage.getFootstepDataList().forEach(m -> m.getPredictedContactPoints2d().clear());
         }

         footstepDataListPublisher.publish(footstepDataListMessage);
      });

      IHMCRealtimeROS2Publisher<BipedalSupportPlanarRegionParametersMessage> supportRegionsParametersPublisher = ROS2Tools
            .createPublisher(ros2Node, BipedalSupportPlanarRegionParametersMessage.class,
                             ROS2Tools.getTopicNameGenerator(robotName, ROS2Tools.BIPED_SUPPORT_REGION_PUBLISHER, ROS2TopicQualifier.INPUT));
      messager.registerTopicListener(FootstepPlannerMessagerAPI.BipedalSupportRegionsParameters, supportRegionsParametersPublisher::publish);
   }

   private void processFootstepPlanningRequestPacket(FootstepPlanningRequestPacket packet)
   {
      if (verbose)
         LogTools.info("Received a planning request.");

      Point3D goalPosition = packet.getGoalPositionInWorld();
      Quaternion goalOrientation = packet.getGoalOrientationInWorld();
      Point3D startPosition = packet.getStanceFootPositionInWorld();
      Quaternion startOrientation = packet.getStanceFootOrientationInWorld();
      FootstepPlannerType plannerType = FootstepPlannerType.fromByte(packet.getRequestedFootstepPlannerType());
      RobotSide initialSupportSide = RobotSide.fromByte(packet.getInitialStanceRobotSide());
      int plannerRequestId = packet.getPlannerRequestId();

      double timeout = packet.getTimeout();
      double horizonLength = packet.getHorizonLength();

      messager.submitMessage(FootstepPlannerMessagerAPI.StartPosition, startPosition);
      messager.submitMessage(FootstepPlannerMessagerAPI.GoalPosition, goalPosition);

      messager.submitMessage(FootstepPlannerMessagerAPI.StartOrientation, startOrientation);
      messager.submitMessage(FootstepPlannerMessagerAPI.GoalOrientation, goalOrientation);

      messager.submitMessage(FootstepPlannerMessagerAPI.PlannerType, plannerType);

      messager.submitMessage(FootstepPlannerMessagerAPI.PlannerTimeout, timeout);
      messager.submitMessage(FootstepPlannerMessagerAPI.InitialSupportSide, initialSupportSide);

      messager.submitMessage(FootstepPlannerMessagerAPI.PlannerRequestId, plannerRequestId);

      messager.submitMessage(FootstepPlannerMessagerAPI.PlannerHorizonLength, horizonLength);
   }

   private void processBodyPathPlanMessage(BodyPathPlanMessage packet)
   {
      PlanarRegionsListMessage planarRegionsListMessage = packet.getPlanarRegionsList();
      PlanarRegionsList planarRegionsList = PlanarRegionMessageConverter.convertToPlanarRegionsList(planarRegionsListMessage);
      FootstepPlanningResult result = FootstepPlanningResult.fromByte(packet.getFootstepPlanningResult());
      List<? extends Pose3DReadOnly> bodyPath = packet.getBodyPath();

      messager.submitMessage(FootstepPlannerMessagerAPI.PlanarRegionData, planarRegionsList);
      messager.submitMessage(FootstepPlannerMessagerAPI.PlanningResult, result);
      messager.submitMessage(FootstepPlannerMessagerAPI.BodyPathData, bodyPath);

      if (verbose)
         LogTools.info("Received a body path planning result from the toolbox.");
   }

   private void processBodyPathPlanStatistics(BodyPathPlanStatisticsMessage packet)
   {
      VisibilityMapHolder startVisibilityMap = VisibilityGraphMessagesConverter.convertToSingleSourceVisibilityMap(packet.getStartVisibilityMap());
      VisibilityMapHolder goalVisibilityMap = VisibilityGraphMessagesConverter.convertToSingleSourceVisibilityMap(packet.getGoalVisibilityMap());
      VisibilityMapHolder interRegionVisibilityMap = VisibilityGraphMessagesConverter.convertToInterRegionsVisibilityMap(packet.getInterRegionsMap());

      List<VisibilityMapWithNavigableRegion> navigableRegionList = VisibilityGraphMessagesConverter.convertToNavigableRegionsList(packet.getNavigableRegions());

      messager.submitMessage(FootstepPlannerMessagerAPI.StartVisibilityMap, startVisibilityMap);
      messager.submitMessage(FootstepPlannerMessagerAPI.GoalVisibilityMap, goalVisibilityMap);
      messager.submitMessage(FootstepPlannerMessagerAPI.VisibilityMapWithNavigableRegionData, navigableRegionList);
      messager.submitMessage(FootstepPlannerMessagerAPI.InterRegionVisibilityMap, interRegionVisibilityMap);
   }

   private void processFootstepPlannerStatus(FootstepPlannerStatusMessage packet)
   {
      messager.submitMessage(FootstepPlannerMessagerAPI.PlannerStatus, FootstepPlannerStatus.fromByte(packet.getFootstepPlannerStatus()));
   }

   private void processFootstepPlanningOutputStatus(FootstepPlanningToolboxOutputStatus packet)
   {
      FootstepDataListMessage footstepDataListMessage = packet.getFootstepDataList();
      int plannerRequestId = packet.getPlanId();
      FootstepPlanningResult result = FootstepPlanningResult.fromByte(packet.getFootstepPlanningResult());
      List<? extends Pose3DReadOnly> bodyPath = packet.getBodyPath();
      Pose3D lowLevelGoal = packet.getLowLevelPlannerGoal();

      if (plannerRequestId > currentPlanRequestId.get())
         messager.submitMessage(FootstepPlannerMessagerAPI.PlannerRequestId, plannerRequestId);
     
      ThreadTools.sleep(100);

      messager.submitMessage(FootstepPlannerMessagerAPI.FootstepPlanResponse, footstepDataListMessage);
      messager.submitMessage(FootstepPlannerMessagerAPI.ReceivedPlanId, plannerRequestId);
      messager.submitMessage(FootstepPlannerMessagerAPI.PlanningResult, result);
      messager.submitMessage(FootstepPlannerMessagerAPI.PlannerTimeTaken, packet.getFootstepPlanningStatistics().getTimeTaken());
      messager.submitMessage(FootstepPlannerMessagerAPI.BodyPathData, bodyPath);
      if (lowLevelGoal != null)
      {
         messager.submitMessage(FootstepPlannerMessagerAPI.LowLevelGoalPosition, lowLevelGoal.getPosition());
         messager.submitMessage(FootstepPlannerMessagerAPI.LowLevelGoalOrientation, lowLevelGoal.getOrientation());
      }

      messager.submitMessage(FootstepPlannerMessagerAPI.PlannerStatistics, packet.getFootstepPlanningStatistics());

      if (verbose)
         LogTools.info("Received a footstep planning result from the toolbox.");
   }

   private void processIncomingPlanarRegionMessage(PlanarRegionsListMessage packet)
   {
      if (acceptNewPlanarRegionsReference.get())
      {
         messager.submitMessage(FootstepPlannerMessagerAPI.PlanarRegionData, PlanarRegionMessageConverter.convertToPlanarRegionsList(packet));

         if (verbose)
            LogTools.info("Received updated planner regions.");
      }
   }

   private void requestNewPlan()
   {
      if (!checkRequireds())
      {
         return;
      }

      toolboxStatePublisher.publish(MessageTools.createToolboxStateMessage(ToolboxState.WAKE_UP));

      if (verbose)
         LogTools.info("Told the toolbox to wake up.");
      
      FootstepPlannerParametersReadOnly footstepPlannerParameters = plannerParametersReference.get();
      if(footstepPlannerParameters != null)
      {
         FootstepPlannerParametersPacket plannerParametersPacket = new FootstepPlannerParametersPacket();
         FootstepPlannerMessageTools.copyParametersToPacket(plannerParametersPacket, footstepPlannerParameters);
         plannerParametersPublisher.publish(plannerParametersPacket);
      }

      VisibilityGraphsParametersReadOnly visibilityGraphsParameters = visibilityGraphParametersReference.get();
      if(visibilityGraphsParameters != null)
      {
         VisibilityGraphsParametersPacket visibilityGraphsParametersPacket = new VisibilityGraphsParametersPacket();
         FootstepPlannerMessageTools.copyParametersToPacket(visibilityGraphsParametersPacket, visibilityGraphsParameters);
         visibilityGraphsParametersPublisher.publish(visibilityGraphsParametersPacket);
      }

      if (verbose)
         LogTools.info("Sent out some parameters");

      submitFootstepPlanningRequestPacket();
   }

   private boolean checkRequireds()
   {
      if (plannerStartPositionReference.get() == null)
      {
         LogTools.warn("Need to set start position.");
         return false;
      }
      if (plannerGoalPositionReference.get() == null)
      {
         LogTools.warn("Need to set goal position.");
         return false;
      }
      return true;
   }

   private void requestPlannerStatistics()
   {
      plannerStatisticsRequestPublisher.publish(new PlanningStatisticsRequestMessage());
   }

   private void requestAbortPlanning()
   {
      if (verbose)
         LogTools.info("Sending out a sleep request.");
      toolboxStatePublisher.publish(MessageTools.createToolboxStateMessage(ToolboxState.SLEEP));
   }

   private void submitFootstepPlanningRequestPacket()
   {
      FootstepPlanningRequestPacket packet = new FootstepPlanningRequestPacket();
      packet.getStanceFootPositionInWorld().set(plannerStartPositionReference.get());
      packet.getStanceFootOrientationInWorld().set(plannerStartOrientationReference.get());
      packet.getGoalPositionInWorld().set(plannerGoalPositionReference.get());
      packet.getGoalOrientationInWorld().set(plannerGoalOrientationReference.get());
      if (plannerInitialSupportSideReference.get() != null)
         packet.setInitialStanceRobotSide(plannerInitialSupportSideReference.get().toByte());
      if (plannerTimeoutReference.get() != null)
         packet.setTimeout(plannerTimeoutReference.get());
      if (plannerTypeReference.get() != null)
         packet.setRequestedFootstepPlannerType(plannerTypeReference.get().toByte());
      if (plannerRequestIdReference.get() != null)
         packet.setPlannerRequestId(plannerRequestIdReference.get());
      if (plannerHorizonLengthReference.get() != null)
         packet.setHorizonLength(plannerHorizonLengthReference.get());
      if (plannerPlanarRegionReference.get() != null)
         packet.getPlanarRegionsListMessage().set(PlanarRegionMessageConverter.convertToPlanarRegionsListMessage(plannerPlanarRegionReference.get()));
      packet.setAssumeFlatGround(assumeFlatGround.get());
      packet.setGoalDistanceProximity(goalDistanceProximity.get());
      packet.setGoalYawProximity(goalYawProximity.get());

      footstepPlanningRequestPublisher.publish(packet);
   }
}
