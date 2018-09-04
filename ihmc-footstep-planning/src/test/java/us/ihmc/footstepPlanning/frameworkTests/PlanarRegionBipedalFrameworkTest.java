package us.ihmc.footstepPlanning.frameworkTests;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.continuousIntegration.ContinuousIntegrationTools;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.footstepPlanning.FootstepPlannerType;
import us.ihmc.footstepPlanning.ui.StandaloneFootstepPlannerUILauncher;

@ContinuousIntegrationPlan(categories = IntegrationCategory.FAST)
public class PlanarRegionBipedalFrameworkTest extends FootstepPlannerFrameworkTest
{
   @Override
   public FootstepPlannerType getPlannerType()
   {
      return FootstepPlannerType.PLANAR_REGION_BIPEDAL;
   }

   @Test(timeout = 500000)
   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 90.0)
   public void testDatasetsWithoutOcclusion()
   {
      runAssertionsOnAllDatasets(dataset -> runAssertionsWithoutOcclusion(dataset));
   }


   @Before
   public void setup()
   {
      VISUALIZE = VISUALIZE && !ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer();


      StandaloneFootstepPlannerUILauncher launcher = new StandaloneFootstepPlannerUILauncher(VISUALIZE);
      PlatformImpl.startup(() -> {
         Platform.runLater(new Runnable()
         {
            @Override
            public void run()
            {
               try
               {
                  launcher.start(new Stage());
               }
               catch (Exception e)
               {
                  e.printStackTrace();
               }
            }
         });
      });

      while (launcher.getUI() == null)
         ThreadTools.sleep(100);

      ui = launcher.getUI();
   }

   @After
   public void tearDown() throws Exception
   {
      launcher.stop();
      ui = null;
      launcher = null;
   }

   public static void main(String[] args) throws Exception
   {
      VISUALIZE = true;
      PlanarRegionBipedalFrameworkTest test = new PlanarRegionBipedalFrameworkTest();
      String prefix = "unitTestData/testable/";
      test.setup();
      test.runAssertionsOnDataset(dataset -> test.runAssertionsWithoutOcclusion(dataset), prefix + "20171218_205120_BodyPathPlannerEnvironment");
      test.tearDown();

   }
}
