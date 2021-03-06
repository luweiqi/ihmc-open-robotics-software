package us.ihmc.pathPlanning.visibilityGraphs.tools;

import static us.ihmc.robotics.Assert.*;

import org.junit.jupiter.api.Test;

import us.ihmc.commons.MutationTestFacilitator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import us.ihmc.euclid.geometry.BoundingBox2D;
import us.ihmc.euclid.geometry.interfaces.BoundingBox2DReadOnly;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster.ClusterType;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster.ExtrusionSide;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.ExtrusionHull;

import java.util.ArrayList;
import java.util.List;

public class ClusterTest
{
   private static final double EPSILON = 1e-10;

   @Test
   public void testIsInsideNonNavigableZone()
   {
      Point2D pointA = new Point2D(0.0, 0.0);
      Point2D pointB = new Point2D(1.0, 0.0);
      Point2D pointC = new Point2D(1.0, 1.0);
      Point2D pointD = new Point2D(0.0, 1.0);

      Cluster obstacleCluster = new Cluster(ExtrusionSide.OUTSIDE, ClusterType.POLYGON);

      ExtrusionHull clusterPoints = new ExtrusionHull();
      clusterPoints.addPoint(new Point2D(0.1, -0.1));
      clusterPoints.addPoint(new Point2D(1.1, -0.1));
      clusterPoints.addPoint(new Point2D(1.1, 1.1));
      clusterPoints.addPoint(new Point2D(0.1, 1.1));
      obstacleCluster.addNonNavigableExtrusionsInLocal(clusterPoints);

      assertFalse(obstacleCluster.isInsideNonNavigableZone(pointA));
      assertTrue(obstacleCluster.isInsideNonNavigableZone(pointB));
      assertTrue(obstacleCluster.isInsideNonNavigableZone(pointC));
      assertFalse(obstacleCluster.isInsideNonNavigableZone(pointD));
   }

   @Test
   public void testBoundingBox()
   {
      Cluster obstacleCluster = new Cluster(ExtrusionSide.OUTSIDE, ClusterType.POLYGON);

      ExtrusionHull clusterPoints = new ExtrusionHull();
      clusterPoints.addPoint(new Point2D(0.1, -0.1));
      clusterPoints.addPoint(new Point2D(1.1, -0.1));
      clusterPoints.addPoint(new Point2D(1.1, 1.6));
      clusterPoints.addPoint(new Point2D(0.3, 1.1));
      obstacleCluster.addNonNavigableExtrusionsInLocal(clusterPoints);

      BoundingBox2DReadOnly boundingBox = obstacleCluster.getNonNavigableExtrusionsBoundingBox();
      assertEquals(0.1, boundingBox.getMinX(), EPSILON);
      assertEquals(-0.1, boundingBox.getMinY(), EPSILON);
      assertEquals(1.1, boundingBox.getMaxX(), EPSILON);
      assertEquals(1.6, boundingBox.getMaxY(), EPSILON);
   }

   public static void main(String[] args)
   {
      MutationTestFacilitator.facilitateMutationTestForClass(Cluster.class, ClusterTest.class);
   }

}
