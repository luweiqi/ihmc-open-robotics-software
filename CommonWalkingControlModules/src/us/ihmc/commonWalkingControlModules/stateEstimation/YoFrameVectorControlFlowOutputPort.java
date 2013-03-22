package us.ihmc.commonWalkingControlModules.stateEstimation;

import us.ihmc.controlFlow.ControlFlowElement;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;

public class YoFrameVectorControlFlowOutputPort extends ControlFlowOutputPort<FrameVector>
{
   private final YoFrameVector yoFrameVector;

   public YoFrameVectorControlFlowOutputPort(ControlFlowElement controlFlowElement, String namePrefix, ReferenceFrame frame, YoVariableRegistry registry)
   {
      super(controlFlowElement);
      yoFrameVector = new YoFrameVector(namePrefix, frame, registry);
      super.setData(new FrameVector(frame));
   }

   @Override
   public FrameVector getData()
   {
      yoFrameVector.getFrameVector(super.getData());

      return super.getData();
   }

   @Override
   public void setData(FrameVector data)
   {
      yoFrameVector.set(data);
   }
}
