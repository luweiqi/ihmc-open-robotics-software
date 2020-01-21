package us.ihmc.valkyrie;

import com.google.common.base.CaseFormat;
import controller_msgs.msg.dds.FootstepPlanningRequestPacket;
import controller_msgs.msg.dds.FootstepPlanningRequestPacketPubSubType;
import us.ihmc.commons.nio.FileTools;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ROS2Tools.MessageTopicNameGenerator;
import us.ihmc.footstepPlanning.communication.FootstepPlannerCommunicationProperties;
import us.ihmc.idl.serializers.extra.JSONSerializer;
import us.ihmc.log.LogTools;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.ros2.RealtimeRos2Node;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ValkyrieFootstepPlannerMessageLogger
{
   private static final PubSubImplementation pubSubImplementation = PubSubImplementation.FAST_RTPS;
   private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
   private static final String logDirectory = System.getProperty("user.home") + File.separator + "Documents" + File.separator;

   private final RealtimeRos2Node ros2Node;
   private final JSONSerializer<FootstepPlanningRequestPacket> robotConfigurationDataSerializer = new JSONSerializer<>(new FootstepPlanningRequestPacketPubSubType());

   public ValkyrieFootstepPlannerMessageLogger()
   {
      String robotName = "Valkyrie";
      ros2Node = ROS2Tools.createRealtimeRos2Node(pubSubImplementation, "ihmc_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getClass().getSimpleName()));

      MessageTopicNameGenerator toolboxSubTopicNameGenerator = FootstepPlannerCommunicationProperties.subscriberTopicNameGenerator(robotName);
      ROS2Tools.createCallbackSubscription(ros2Node,
                                           FootstepPlanningRequestPacket.class,
                                           toolboxSubTopicNameGenerator,
                                           s -> logMessage(s.takeNextData()));
      ros2Node.spin();
   }

   private void logMessage(FootstepPlanningRequestPacket message)
   {
      LogTools.info("Logging message...");
      String fileName = logDirectory + dateFormat.format(new Date()) + "_" + "ValkyriePlannerRequestPacket.json";
      try
      {
         FileTools.ensureFileExists(new File(fileName).toPath());
         FileWriter fileWriter = new FileWriter(fileName);
         fileWriter.write(robotConfigurationDataSerializer.serializeToString(message));
         LogTools.info("Done: " + fileName);
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
   }

   public static void main(String[] args)
   {
      new ValkyrieFootstepPlannerMessageLogger();
      LogTools.info("Listening for request message...");
   }
}