package us.ihmc.valkyrie.joystick;

import com.sun.javafx.application.ParametersImpl;

import javafx.application.Application;
import javafx.application.Application.Parameters;
import javafx.application.Platform;
import javafx.stage.Stage;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.joystickBasedJavaFXController.JoystickBasedSteppingMainUI;
import us.ihmc.avatar.joystickBasedJavaFXController.StepGeneratorJavaFXController.SecondaryControlOption;
import us.ihmc.commonWalkingControlModules.configurations.SteppingParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.javaFXToolkit.starter.ApplicationRunner;
import us.ihmc.log.LogTools;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.ros2.Ros2Node;
import us.ihmc.valkyrie.ValkyrieRobotModel;
import us.ihmc.valkyrie.configuration.ValkyrieRobotVersion;

public class ValkyrieJoystickBasedSteppingApplication
{
   private JoystickBasedSteppingMainUI ui;
   private final Ros2Node ros2Node = ROS2Tools.createRos2Node(PubSubImplementation.FAST_RTPS, "ihmc_valkyrie_xbox_joystick_control");

   public ValkyrieJoystickBasedSteppingApplication(Stage primaryStage, Parameters parameters) throws Exception
   {
      String robotTargetString = parameters.getNamed().getOrDefault("robotTarget", "REAL_ROBOT");
      String workingDirectory = parameters.getNamed().get("workingDir");
      RobotTarget robotTarget = RobotTarget.valueOf(robotTargetString);
      LogTools.info("-------------------------------------------------------------------");
      LogTools.info("  -------- Loading parameters for RobotTarget: " + robotTarget + "  -------");
      LogTools.info("-------------------------------------------------------------------");
      ValkyrieRobotModel robotModel = new ValkyrieRobotModel(robotTarget, ValkyrieRobotVersion.DEFAULT);
      String robotName = robotModel.getSimpleRobotName();
      ValkyriePunchMessenger kickAndPunchMessenger = new ValkyriePunchMessenger(robotName, ros2Node);

      WalkingControllerParameters walkingControllerParameters = robotModel.getWalkingControllerParameters();
      SteppingParameters steppingParameters = walkingControllerParameters.getSteppingParameters();
      double footLength = steppingParameters.getFootLength();
      double footWidth = steppingParameters.getFootWidth();
      ConvexPolygon2D footPolygon = new ConvexPolygon2D();
      footPolygon.addVertex(footLength / 2.0, footWidth / 2.0);
      footPolygon.addVertex(footLength / 2.0, -footWidth / 2.0);
      footPolygon.addVertex(-footLength / 2.0, -footWidth / 2.0);
      footPolygon.addVertex(-footLength / 2.0, footWidth / 2.0);
      footPolygon.update();

      SideDependentList<ConvexPolygon2D> footPolygons = new SideDependentList<>(footPolygon, footPolygon);

      ui = new JoystickBasedSteppingMainUI(robotName,
                                           primaryStage,
                                           ros2Node,
                                           workingDirectory,
                                           robotModel,
                                           walkingControllerParameters,
                                           null,
                                           kickAndPunchMessenger,
                                           kickAndPunchMessenger,
                                           footPolygons);
      ui.setActiveSecondaryControlOption(SecondaryControlOption.NONE);
   }

   public void stop()
   {
      ui.stop();
      ros2Node.destroy();
      Platform.exit();
   }

   /**
    * Argument options:
    * <ul>
    * <li>Selecting the robot target: for sim: {@code --robotTarget=SCS}, for hardware: {@code --robotTarget=REAL_ROBOT}.
    * <li>Selecting the working directory (where the profiles are saved): {@code --workingDir=~/home/myWorkingDirectory}. If none provided the default is set to {@code "~/.ihmc/joystick_step_app/"}.
    * </ul>
    * 
    * @param args the array of arguments to use for this run.
    */
   public static void main(String[] args)
   {
      ApplicationRunner.runApplication(new Application()
      {
         private ValkyrieJoystickBasedSteppingApplication app;

         @Override
         public void start(Stage primaryStage) throws Exception
         {
            app = new ValkyrieJoystickBasedSteppingApplication(primaryStage, new ParametersImpl(args));
         }

         @Override
         public void stop() throws Exception
         {
            super.stop();
            app.stop();
         }
      });
   }
}
