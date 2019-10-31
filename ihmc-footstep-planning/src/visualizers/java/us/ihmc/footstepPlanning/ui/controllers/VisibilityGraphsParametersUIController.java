package us.ihmc.footstepPlanning.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.VisibilityGraphParametersKeys;
import us.ihmc.pathPlanning.visibilityGraphs.parameters.VisibilityGraphsParametersBasics;
import us.ihmc.robotEnvironmentAwareness.ui.properties.JavaFXStoredPropertyMap;

public class VisibilityGraphsParametersUIController
{
   private JavaFXMessager messager;
   private VisibilityGraphsParametersBasics planningParameters;

   @FXML
   private Slider clusterResolution;
   @FXML
   private Slider maxInterRegionConnectionLength;
   @FXML
   private Spinner<Double> explorationDistanceFromStartGoal;

   @FXML
   private Slider extrusionDistance;
   @FXML
   private Slider extrusionDistanceIfNotTooHighToStep;
   @FXML
   private Slider tooHighToStepDistance;


   @FXML
   private Slider planarRegionMinArea;
   @FXML
   private Spinner<Integer> planarRegionMinSize;
   @FXML
   private Slider regionOrthogonalAngle;
   @FXML
   private Slider searchHostRegionEpsilon;
   @FXML
   private Slider normalZThresholdForAccessibleRegions;



   public void attachMessager(JavaFXMessager messager)
   {
      this.messager = messager;
   }

   public void setVisbilityGraphsParameters(VisibilityGraphsParametersBasics parameters)
   {
      this.planningParameters = parameters;
   }

   private void setupControls()
   {
      planarRegionMinSize.setValueFactory(createPlanarRegionMinSizeValueFactory());
      explorationDistanceFromStartGoal.setValueFactory(createExplorationDistanceFroStartGoal());
   }

   public void bindControls()
   {
      setupControls();

      JavaFXStoredPropertyMap javaFXStoredPropertyMap = new JavaFXStoredPropertyMap(planningParameters);
      javaFXStoredPropertyMap.put(maxInterRegionConnectionLength, VisibilityGraphParametersKeys.maxInterRegionConnectionLength);
      javaFXStoredPropertyMap.put(normalZThresholdForAccessibleRegions, VisibilityGraphParametersKeys.normalZThresholdForAccessibleRegions);
      javaFXStoredPropertyMap.put(extrusionDistance, VisibilityGraphParametersKeys.obstacleExtrusionDistance);
      javaFXStoredPropertyMap.put(extrusionDistanceIfNotTooHighToStep, VisibilityGraphParametersKeys.obstacleExtrusionDistanceIfNotTooHighToStep);
      javaFXStoredPropertyMap.put(tooHighToStepDistance, VisibilityGraphParametersKeys.tooHighToStepDistance);
      javaFXStoredPropertyMap.put(clusterResolution, VisibilityGraphParametersKeys.clusterResolution);
      javaFXStoredPropertyMap.put(explorationDistanceFromStartGoal, VisibilityGraphParametersKeys.explorationDistanceFromStartGoal);
      javaFXStoredPropertyMap.put(planarRegionMinArea, VisibilityGraphParametersKeys.planarRegionMinArea);
      javaFXStoredPropertyMap.put(planarRegionMinSize, VisibilityGraphParametersKeys.planarRegionMinSize);
      javaFXStoredPropertyMap.put(regionOrthogonalAngle, VisibilityGraphParametersKeys.regionOrthogonalAngle);
      javaFXStoredPropertyMap.put(searchHostRegionEpsilon, VisibilityGraphParametersKeys.searchHostRegionEpsilon);

      // set messager updates to update all stored properties and select JavaFX properties
      messager.registerTopicListener(FootstepPlannerMessagerAPI.VisibilityGraphsParameters, parameters ->
      {
         planningParameters.set(parameters);

         javaFXStoredPropertyMap.copyStoredToJavaFX();
      });

      // set JavaFX user input to update stored properties and publish messager message
      javaFXStoredPropertyMap.bindStoredToJavaFXUserInput();
      javaFXStoredPropertyMap.bindToJavaFXUserInput(() -> publishParameters());
   }

   private void publishParameters()
   {
      messager.submitMessage(FootstepPlannerMessagerAPI.VisibilityGraphsParameters, planningParameters);
   }


   private SpinnerValueFactory.IntegerSpinnerValueFactory createPlanarRegionMinSizeValueFactory()
   {
      int min = 0;
      int max = 100;
      int amountToStepBy = 1;
      return new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, 0, amountToStepBy);
   }

   private SpinnerValueFactory.DoubleSpinnerValueFactory createExplorationDistanceFroStartGoal()
   {
      double min = 0;
      double max = Double.POSITIVE_INFINITY;
      double amountToStepBy = 1.0;
      return new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, 0, amountToStepBy);
   }
}
