package us.ihmc.avatar.networkProcessor.footstepPlanPostProcessingModule;

import controller_msgs.msg.dds.FootstepPlanningRequestPacket;
import controller_msgs.msg.dds.FootstepPlanningToolboxOutputStatus;

import java.util.EnumMap;

public class CompositeFootstepPlanPostProcessing implements FootstepPlanPostProcessingElement
{
   private final EnumMap<PostProcessingEnum, FootstepPlanPostProcessingElement> postProcessingElements = new EnumMap<>(PostProcessingEnum.class);

   public void addPostProcessingElement(FootstepPlanPostProcessingElement postProcessingElement)
   {
      if (postProcessingElements.containsKey(postProcessingElement.getElementName()))
         throw new RuntimeException("The composite builder already contains this element!");

      postProcessingElements.put(postProcessingElement.getElementName(), postProcessingElement);
   }

   public void setIsActive(PostProcessingEnum postProcessingEnum, boolean isActive)
   {
      postProcessingElements.get(postProcessingEnum).setIsActive(isActive);
   }

   public void setIsActive(boolean isActive)
   {
      throw new RuntimeException("Not a valid argument for the composite");
   }

   /** {@inheritDoc} **/
   @Override
   public boolean isActive()
   {
      return true;
   }

   /** {@inheritDoc} **/
   @Override
   public FootstepPlanningToolboxOutputStatus postProcessFootstepPlan(FootstepPlanningRequestPacket request, FootstepPlanningToolboxOutputStatus outputStatus)
   {
      FootstepPlanningToolboxOutputStatus currentOutputStatus = outputStatus;
      for (FootstepPlanPostProcessingElement element : postProcessingElements.values())
      {
         if (!element.isActive())
            continue;

         currentOutputStatus = element.postProcessFootstepPlan(request, currentOutputStatus);
      }

      return currentOutputStatus;
   }

   /** {@inheritDoc} **/
   @Override
   public PostProcessingEnum getElementName()
   {
      return PostProcessingEnum.COMPOSITE;
   }
}
