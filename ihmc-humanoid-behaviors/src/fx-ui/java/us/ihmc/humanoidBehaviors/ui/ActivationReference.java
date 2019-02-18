package us.ihmc.humanoidBehaviors.ui;

import java.util.concurrent.atomic.AtomicReference;

/**
 * An atomic reference with one frame of history to give it has changed
 * on the latest call to get().
 */
public class ActivationReference<T>
{
   private final AtomicReference<T> atomicReference;
   private final T activatedValue;
   private boolean activationChanged = false;
   private T lastValue;

   public ActivationReference(AtomicReference<T> atomicReference, T activatedValue)
   {
      this.atomicReference = atomicReference;
      this.activatedValue = activatedValue;
      lastValue = atomicReference.get();
   }

   /**
    * Check if the current referenced value equals the active value, store if the activation changed
    * and store the t-1 value.
    *
    * @return current value equals activated value
    */
   public boolean checkActivated()
   {
      T newValue = atomicReference.get();

      boolean newValueActivated = newValue.equals(activatedValue);

      activationChanged = newValueActivated != lastValue.equals(activatedValue);

      lastValue = newValue;

      return newValueActivated;
   }

   /**
    * @return if the activation changed on the last call to {@link #checkActivated()}
    */
   public boolean activationChanged()
   {
      return activationChanged;
   }
}
