package us.ihmc.robotEnvironmentAwareness;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import us.ihmc.javaFXToolkit.starter.ApplicationRunner;
import us.ihmc.robotEnvironmentAwareness.ui.LIDARBasedEnvironmentAwarenessUI;
import us.ihmc.robotEnvironmentAwareness.updaters.LIDARBasedREAModule;

public class LidarBasedREAStandaloneLauncher
{
   private static final String MODULE_CONFIGURATION_FILE_NAME = "./Configurations/defaultREAModuleConfiguration.txt";

   public static void main(String[] args)
   {
      ApplicationRunner.runApplication(new Application()
      {
         private LIDARBasedEnvironmentAwarenessUI ui;
         private LIDARBasedREAModule module;

         @Override
         public void start(Stage primaryStage) throws Exception
         {
            ui = LIDARBasedEnvironmentAwarenessUI.createIntraprocessUI(primaryStage);
            module = LIDARBasedREAModule.createIntraprocessModule(MODULE_CONFIGURATION_FILE_NAME);

            ui.show();
            module.start();

         }

         public void stop() throws Exception
         {
            ui.stop();
            module.stop();

            Platform.exit();
         }
      });
   }
}
