package us.ihmc.pathPlanning.visibilityGraphs.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import us.ihmc.javaFXToolkit.starter.ApplicationRunner;

public class SimpleUILauncher
{
   private static final boolean SHOW_FILE_CHOOSER_ON_START = false;

   public static void main(String[] args)
   {
      ApplicationRunner.runApplication(new Application()
      {
         SimpleVisibilityGraphsUI ui;

         @Override
         public void start(Stage primaryStage) throws Exception
         {
            ui = new SimpleVisibilityGraphsUI(primaryStage);
            ui.show(SHOW_FILE_CHOOSER_ON_START);
         }

         @Override
         public void stop() throws Exception
         {
            super.stop();
            ui.stop();
            Platform.exit();
         }
      });
   }
}
