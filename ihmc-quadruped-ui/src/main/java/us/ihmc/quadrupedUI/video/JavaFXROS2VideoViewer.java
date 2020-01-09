package us.ihmc.quadrupedUI.video;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.javaFXToolkit.starter.ApplicationRunner;
import us.ihmc.log.LogTools;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.ros2.Ros2Node;

public class JavaFXROS2VideoViewer
{
   private static final int width = 1024;
   private static final int height = 544;

   public JavaFXROS2VideoViewer(Stage primaryStage) throws Exception
   {
      Ros2Node ros2Node = ROS2Tools.createRos2Node(PubSubImplementation.FAST_RTPS, "video_viewer");

      JavaFXROS2VideoView ros2VideoView = new JavaFXROS2VideoView(width, height, false, false);

      StackPane stackPaneNode = new StackPane(ros2VideoView);
      stackPaneNode.setPrefSize(width, height);
      Scene scene = new Scene(stackPaneNode);
      primaryStage.setOnCloseRequest((e) ->
      {
         ros2VideoView.stop();
         ros2Node.destroy();
      });
      primaryStage.setX(0); // essentially monitor selection
      primaryStage.setY(0);
      primaryStage.initStyle(StageStyle.DECORATED);
      primaryStage.setScene(scene);
      primaryStage.setTitle(getClass().getSimpleName());
      primaryStage.show();

      ros2VideoView.start(ros2Node);
   }

   public void stop()
   {
      LogTools.info("JavaFX stop() called");
   }

   public static void main(String[] args)
   {
      ApplicationRunner.runApplication(new Application()
      {
         private JavaFXROS2VideoViewer viewer;

         @Override
         public void start(Stage primaryStage) throws Exception
         {
            viewer = new JavaFXROS2VideoViewer(primaryStage);
         }

         @Override
         public void stop() throws Exception
         {
            super.stop();
            viewer.stop();
         }
      });
   }
}