package us.ihmc.robotEnvironmentAwareness.ui.properties;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import us.ihmc.tools.property.StoredPropertyBasics;
import us.ihmc.tools.property.StoredPropertyKey;
import us.ihmc.tools.property.StoredPropertySetBasics;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class JavaFXStoredPropertyMap
{
   private final StoredPropertySetBasics storedPropertySet;
   private final HashMap<JavaFXPropertyHolder, StoredPropertyBasics> map = new HashMap<>();

   public JavaFXStoredPropertyMap(StoredPropertySetBasics storedPropertySet)
   {
      this.storedPropertySet = storedPropertySet;
   }

   public void put(CheckBox checkBox, StoredPropertyKey<Boolean> booleanKey)
   {
      map.put(new JavaFXPropertyHolder<>(() -> checkBox.selectedProperty().getValue(),
                                         value -> checkBox.selectedProperty().setValue(value),
                                         runnable -> checkBox.selectedProperty().addListener(observable -> runnable.run())),
              storedPropertySet.getProperty(booleanKey));
      checkBox.setSelected(storedPropertySet.get(booleanKey));
   }

   public <T> void put(Spinner<T> doubleSpinner, StoredPropertyKey<T> doubleKey)
   {
      map.put(new JavaFXPropertyHolder<>(() -> doubleSpinner.getValueFactory().valueProperty().getValue(),
                                         value -> doubleSpinner.getValueFactory().valueProperty().setValue(value),
                                         runnable -> doubleSpinner.getValueFactory().valueProperty().addListener(observable -> runnable.run())),
              storedPropertySet.getProperty(doubleKey));
      doubleSpinner.getValueFactory().setValue(storedPropertySet.get(doubleKey));
   }

   public void put(Slider slider, StoredPropertyKey<Double> doubleKey)
   {
      AtomicBoolean changing = new AtomicBoolean(false); // unfortunately this is necessary for both click and drag to work
      map.put(new JavaFXPropertyHolder<>(slider::getValue,
                                         value -> slider.valueProperty().setValue(value),
                                         runnable ->
                                         {
                                            slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                                               if (!changing.get())
                                               {
                                                  runnable.run();
                                               }
                                            });
                                            slider.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {
                                               changing.set(isChanging);
                                               if (wasChanging)
                                               {
                                                  runnable.run();
                                               }
                                            });
                                         }),
              storedPropertySet.getProperty(doubleKey));
      slider.setValue(storedPropertySet.get(doubleKey));
   }

   public void putIntegerSlider(Slider slider, StoredPropertyKey<Integer> integerKey)
   {
      AtomicBoolean changing = new AtomicBoolean(false); // unfortunately this is necessary for both click and drag to work
      map.put(new JavaFXPropertyHolder<>(() -> (int) slider.getValue(),
                                         value -> slider.valueProperty().setValue(value),
                                         runnable ->
                                         {
                                            slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                                               if (!changing.get())
                                               {
                                                  runnable.run();
                                               }
                                            });
                                            slider.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {
                                               changing.set(isChanging);
                                               if (wasChanging)
                                               {
                                                  runnable.run();
                                               }
                                            });
                                         }),
              storedPropertySet.getProperty(integerKey));
      slider.setValue(storedPropertySet.get(integerKey));
   }

   public void copyJavaFXToStored()
   {
      for (JavaFXPropertyHolder javaFXProperty : map.keySet())
      {
         map.get(javaFXProperty).set(javaFXProperty.getValue());
      }
   }

   public void copyStoredToJavaFX()
   {
      for (JavaFXPropertyHolder javaFXProperty : map.keySet())
      {
         javaFXProperty.setValue(map.get(javaFXProperty).get());
      }
   }

   public void bindStoredToJavaFXUserInput()
   {
      for (JavaFXPropertyHolder javaFXProperty : map.keySet())
      {
         javaFXProperty.addValueChangedListener(() -> map.get(javaFXProperty).set(javaFXProperty.getValue()));
      }
   }

   public void bindToJavaFXUserInput(Runnable runnable)
   {
      for (JavaFXPropertyHolder javaFXProperty : map.keySet())
      {
         javaFXProperty.addValueChangedListener(runnable);
      }
   }
}
