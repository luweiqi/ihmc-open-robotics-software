package us.ihmc.robotEnvironmentAwareness;

import java.util.Map;

import com.sun.javafx.application.ParametersImpl;

import javafx.application.Application.Parameters;
import javafx.stage.Stage;
import us.ihmc.javaFXToolkit.starter.ApplicationRunner;
import us.ihmc.log.LogTools;
import us.ihmc.robotEnvironmentAwareness.ui.LIDARBasedEnvironmentAwarenessUI;

public class RemoteLidarBasedREAUILauncher
{
   public RemoteLidarBasedREAUILauncher(Stage primaryStage, Parameters parameters)
   {
      Map<String, String> namedParameters = parameters.getNamed();
      String host = namedParameters.getOrDefault("host", "localhost");
      LogTools.info("Creating REA UI with the module address: " + host);

      if (!parameters.getRaw().isEmpty())
         LogTools.info("Received the program arguments: " + parameters.getRaw());

      try
      {
         LIDARBasedEnvironmentAwarenessUI remoteUI = LIDARBasedEnvironmentAwarenessUI.createRemoteUI(primaryStage, host);
         remoteUI.show();
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public static void main(String[] args)
   {
      LogTools.info("To change the address of the module, enter its IP address as a program argument. For instance: " + "--host=127.0.0.1");
      ApplicationRunner.runApplication(primaryStage -> new RemoteLidarBasedREAUILauncher(primaryStage, new ParametersImpl(args)));
   }
}
