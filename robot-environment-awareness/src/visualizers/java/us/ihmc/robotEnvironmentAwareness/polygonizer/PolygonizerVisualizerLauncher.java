package us.ihmc.robotEnvironmentAwareness.polygonizer;

import javafx.application.Application;
import javafx.stage.Stage;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.javaFXToolkit.messager.SharedMemoryJavaFXMessager;
import us.ihmc.javaFXToolkit.starter.ApplicationRunner;

public class PolygonizerVisualizerLauncher
{
   public static void main(String[] args)
   {
      ApplicationRunner.runApplication(new Application()
      {
         private PolygonizerVisualizerUI ui;

         @Override
         public void start(Stage primaryStage) throws Exception
         {
            JavaFXMessager messager = new SharedMemoryJavaFXMessager(PolygonizerVisualizerUI.getMessagerAPI());
            ui = new PolygonizerVisualizerUI(messager, primaryStage);
            ui.show();
         }

         @Override
         public void stop() throws Exception
         {
            super.stop();
            ui.stop();
         }
      });
   }
}
