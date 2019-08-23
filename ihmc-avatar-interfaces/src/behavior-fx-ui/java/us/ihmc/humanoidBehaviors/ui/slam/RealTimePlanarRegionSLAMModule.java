package us.ihmc.humanoidBehaviors.ui.slam;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import controller_msgs.msg.dds.PlanarRegionsListMessage;
import us.ihmc.commons.Conversions;
import us.ihmc.communication.ROS2Callback;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidBehaviors.tools.perception.PlanarRegionSLAM;
import us.ihmc.humanoidBehaviors.tools.perception.PlanarRegionSLAMParameters;
import us.ihmc.humanoidBehaviors.tools.perception.PlanarRegionSLAMResult;
import us.ihmc.javaFXToolkit.messager.SharedMemoryJavaFXMessager;
import us.ihmc.messager.Messager;
import us.ihmc.pathPlanning.visibilityGraphs.tools.ConcaveHullGraphicalMergerListener;
import us.ihmc.pathPlanning.visibilityGraphs.tools.ConcaveHullMergerListener;
import us.ihmc.robotEnvironmentAwareness.communication.KryoMessager;
import us.ihmc.robotEnvironmentAwareness.communication.REACommunicationProperties;
import us.ihmc.robotEnvironmentAwareness.tools.ExecutorServiceTools;
import us.ihmc.robotEnvironmentAwareness.tools.ExecutorServiceTools.ExceptionHandling;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.ros2.Ros2Node;

public class RealTimePlanarRegionSLAMModule
{
   private final Messager messager;
   private final Ros2Node ros2Node;

   private long latestBufferUpdatedTime = System.nanoTime();
   private final AtomicReference<PlanarRegionsList> planarRegionBuffer = new AtomicReference<PlanarRegionsList>();
   private final AtomicReference<Boolean> enableIncomingBuffer;
   private final AtomicReference<Boolean> enableSLAM;
   private final AtomicReference<Boolean> clearSLAMMap;
   
   private static final int THREAD_PERIOD_MILLISECONDS = 20;
   private static final int BUFFER_THREAD_PERIOD_MILLISECONDS = 10;

   private ScheduledExecutorService executorService = ExecutorServiceTools.newScheduledThreadPool(3, getClass(), ExceptionHandling.CATCH_AND_REPORT);
   private ScheduledFuture<?> scheduled;

   public RealTimePlanarRegionSLAMModule(Ros2Node ros2Node, SharedMemoryJavaFXMessager messager)
   {
      this.messager = messager;
      this.ros2Node = ros2Node;

      new ROS2Callback<>(ros2Node, PlanarRegionsListMessage.class, null, ROS2Tools.REA, this::updateBuffer);

      enableIncomingBuffer = messager.createInput(RealTimePlanarRegionSLAMAPI.EnablePlanarRegionIncoming, true);
      enableSLAM = messager.createInput(RealTimePlanarRegionSLAMAPI.EnableSLAM, false);
      clearSLAMMap = messager.createInput(RealTimePlanarRegionSLAMAPI.ClearSLAMMap, false);
   }

   public void updateBuffer(PlanarRegionsListMessage message)
   {
      if(enableIncomingBuffer.get())
      {
         PlanarRegionsList buffer = PlanarRegionMessageConverter.convertToPlanarRegionsList(message);
         planarRegionBuffer.set(buffer);
         messager.submitMessage(RealTimePlanarRegionSLAMAPI.PlanarRegionsList, buffer);
         
         double updatingTime = Conversions.nanosecondsToSeconds(System.nanoTime() - latestBufferUpdatedTime);
         DecimalFormat formatter = new DecimalFormat("0.00");
         String statusToUpdate = "size[" + buffer.getNumberOfPlanarRegions() + "] time[" + formatter.format(updatingTime) + " sec]";
         messager.submitMessage(RealTimePlanarRegionSLAMAPI.PlanarRegionStatus, statusToUpdate);
         
         latestBufferUpdatedTime = System.nanoTime();
      }
      else
      {
         planarRegionBuffer.set(null);
         messager.submitMessage(RealTimePlanarRegionSLAMAPI.PlanarRegionStatus, "No Planar Regions Detected.");
      }
   }

   private PlanarRegionSLAMParameters planarRegionSLAMParameters = new PlanarRegionSLAMParameters();
   private PlanarRegionsList map = new PlanarRegionsList();
   
   private void mainUpdate()
   {
      if(clearSLAMMap.getAndSet(false))
      {
         //TODO: clear.
      }
      
      if(enableSLAM.get())
      {
         //TODO: run slam.
         if(planarRegionBuffer.get() != null)
         {
            System.out.println("update");
            PlanarRegionsList newData = planarRegionBuffer.getAndSet(null);
            ConcaveHullMergerListener listener = new ConcaveHullGraphicalMergerListener();
            PlanarRegionSLAMResult slamResult = PlanarRegionSLAM.slam(map, newData, planarRegionSLAMParameters, listener);
            
            RigidBodyTransform transformFromIncomingToMap = slamResult.getTransformFromIncomingToMap();
            System.out.println(new Quaternion(transformFromIncomingToMap.getRotation()));
            map = slamResult.getMergedMap();
         }
      }
   }

   public void stop() throws Exception
   {
      messager.closeMessager();
      ros2Node.destroy();

      if (scheduled != null)
      {
         scheduled.cancel(true);
         scheduled = null;
      }

      if (executorService != null)
      {
         executorService.shutdownNow();
         executorService = null;
      }
   }

   public void start()
   {
      if (scheduled == null)
      {
         scheduled = executorService.scheduleAtFixedRate(this::mainUpdate, 0, THREAD_PERIOD_MILLISECONDS, TimeUnit.MILLISECONDS);
      }
   }

   public static RealTimePlanarRegionSLAMModule createIntraprocessModule(Ros2Node ros2Node, SharedMemoryJavaFXMessager messager) throws IOException
   {
      KryoMessager kryoMessager = KryoMessager.createIntraprocess(RealTimePlanarRegionSLAMAPI.API, NetworkPorts.BEHAVIOUR_MODULE_PORT,
                                                                  REACommunicationProperties.getPrivateNetClassList());
      kryoMessager.setAllowSelfSubmit(true);
      kryoMessager.startMessager();

      return new RealTimePlanarRegionSLAMModule(ros2Node, messager);
   }
}
