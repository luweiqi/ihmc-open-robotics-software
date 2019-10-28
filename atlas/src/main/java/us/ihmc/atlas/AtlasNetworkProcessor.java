package us.ihmc.atlas;

import java.net.URI;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.networkProcessor.DRCNetworkModuleParameters;
import us.ihmc.avatar.networkProcessor.DRCNetworkProcessor;
import us.ihmc.communication.configuration.NetworkParameters;

public class AtlasNetworkProcessor
{
   public static void main(String[] args) throws JSAPException
   {
      JSAP jsap = new JSAP();

      FlaggedOption robotModel = new FlaggedOption("robotModel").setLongFlag("model").setShortFlag('m').setRequired(true).setStringParser(JSAP.STRING_PARSER);

      Switch runningOnRealRobot = new Switch("runningOnRealRobot").setLongFlag("realRobot");
      Switch runningOnGazebo = new Switch("runningOnGazebo").setLongFlag("gazebo");

      FlaggedOption leftHandHost = new FlaggedOption("leftHandHost").setLongFlag("lefthand").setShortFlag('l').setRequired(false)
                                                                    .setStringParser(JSAP.STRING_PARSER);
      FlaggedOption rightHandHost = new FlaggedOption("rightHandHost").setLongFlag("righthand").setShortFlag('r').setRequired(false)
                                                                      .setStringParser(JSAP.STRING_PARSER);

      robotModel.setHelp("Robot models: " + AtlasRobotModelFactory.robotModelsToString());
      jsap.registerParameter(robotModel);

      jsap.registerParameter(runningOnRealRobot);
      jsap.registerParameter(runningOnGazebo);
      jsap.registerParameter(leftHandHost);
      jsap.registerParameter(rightHandHost);

      JSAPResult config = jsap.parse(args);

      if (config.success())
      {
         DRCRobotModel model;

         DRCNetworkModuleParameters networkModuleParams = new DRCNetworkModuleParameters();
         networkModuleParams.enableBehaviorModule(true);
         networkModuleParams.enableBehaviorVisualizer(true);
         networkModuleParams.enableSensorModule(true);
         networkModuleParams.enableRobotEnvironmentAwerenessModule(false);
         networkModuleParams.enableHeightQuadTreeToolbox(true);
         networkModuleParams.enableMocapModule(false);
         networkModuleParams.enableFootstepPlanningToolbox(false);
         networkModuleParams.enableFootstepPlanningToolboxVisualizer(false);
         networkModuleParams.enableKinematicsToolbox(true);
         networkModuleParams.enableKinematicsToolboxVisualizer(false);
         networkModuleParams.enableKinematicsStreamingToolbox(true, AtlasKinematicsStreamingToolboxModule.class);
         networkModuleParams.setFilterControllerInputMessages(true);
         networkModuleParams.enableBipedalSupportPlanarRegionPublisher(true);
         networkModuleParams.enableAutoREAStateUpdater(true);
         networkModuleParams.enableWalkingPreviewToolbox(true);
         networkModuleParams.enableWholeBodyTrajectoryToolbox(true);

         URI rosuri = NetworkParameters.getROSURI();
         if (rosuri != null)
         {
            networkModuleParams.enableRosModule(true);
            networkModuleParams.setRosUri(rosuri);
            System.out.println("ROS_MASTER_URI=" + rosuri);

            createAuxiliaryRobotDataRosPublisher(networkModuleParams, rosuri);
         }
         try
         {
            RobotTarget target;
            if (config.getBoolean(runningOnRealRobot.getID()))
            {
               target = RobotTarget.REAL_ROBOT;
            }
            else if (config.getBoolean(runningOnGazebo.getID()))
            {
               target = RobotTarget.GAZEBO;
            }
            else
            {
               target = RobotTarget.SCS;
            }
            model = AtlasRobotModelFactory.createDRCRobotModel(config.getString("robotModel"), target, true);
            if (model.getHandModel() != null)
               networkModuleParams.enableHandModule(true);
         }
         catch (IllegalArgumentException e)
         {
            System.err.println("Incorrect robot model " + config.getString("robotModel"));
            System.out.println(jsap.getHelp());

            return;
         }

         System.out.println("Using the " + model + " model");

         URI rosMasterURI = NetworkParameters.getROSURI();
         networkModuleParams.setRosUri(rosMasterURI);

         networkModuleParams.enableLocalControllerCommunicator(false);

         new DRCNetworkProcessor(args, model, networkModuleParams);
      }
      else
      {
         System.err.println("Invalid parameters");
         System.out.println(jsap.getHelp());
         return;
      }
   }

   private static void createAuxiliaryRobotDataRosPublisher(DRCNetworkModuleParameters networkModuleParams, URI rosuri)
   {
      // FIXME Do we still need that?
      //      RosAtlasAuxiliaryRobotDataPublisher auxiliaryRobotDataPublisher = new RosAtlasAuxiliaryRobotDataPublisher(rosuri, defaultRosNameSpace);
      //      PacketCommunicator packetCommunicator = PacketCommunicator.createIntraprocessPacketCommunicator(NetworkPorts.ROS_AUXILIARY_ROBOT_DATA_PUBLISHER,
      //            new IHMCCommunicationKryoNetClassList());
      //
      //      packetCommunicator.attachListener(AtlasAuxiliaryRobotData.class, auxiliaryRobotDataPublisher::receivedPacket);
      //
      //      networkModuleParams.addRobotSpecificModuleCommunicatorPort(NetworkPorts.ROS_AUXILIARY_ROBOT_DATA_PUBLISHER, PacketDestination.AUXILIARY_ROBOT_DATA_PUBLISHER);
   }
}