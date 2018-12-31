package us.ihmc.pathPlanning.visibilityGraphs.tools;

import java.util.Arrays;
import java.util.List;

import us.ihmc.euclid.geometry.BoundingBox2D;
import us.ihmc.euclid.geometry.tools.EuclidGeometryPolygonTools;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DBasics;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Point3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster.ExtrusionSide;
import us.ihmc.robotics.geometry.PlanarRegion;

import static us.ihmc.euclid.geometry.tools.EuclidGeometryTools.*;
import static us.ihmc.euclid.geometry.tools.EuclidGeometryTools.distanceSquaredFromPoint2DToLineSegment2D;
import static us.ihmc.euclid.tools.EuclidCoreTools.normSquared;

public class VisibilityTools
{
   public static boolean isPointVisible(Point2DReadOnly observer, Point2DReadOnly targetPoint, List<? extends Point2DReadOnly> listOfPointsInCluster,
                                        boolean closed)
   {
      int size = listOfPointsInCluster.size();
      int endIndex = size - 1;
      if (closed)
         endIndex++;

      for (int i = 0; i < endIndex; i++)
      {
         Point2DReadOnly first = listOfPointsInCluster.get(i);

         int nextIndex = i + 1;
         if (nextIndex == size)
            nextIndex = 0;

         Point2DReadOnly second = listOfPointsInCluster.get(nextIndex);

         if (EuclidGeometryTools.doLineSegment2DsIntersect(first, second, observer, targetPoint))
         {
            return false;
         }
      }
      return true;
   }

   public static double distanceToCluster(Point2DReadOnly firstPoint, Point2DReadOnly secondPoint, List<? extends Point2DReadOnly> listOfPointsInCluster,
                                          Point2DBasics closestPointToPack, boolean closed)
   {
      int numberOfVertices = listOfPointsInCluster.size();

      if (numberOfVertices == 0)
      {
         closestPointToPack.setToNaN();
         return Double.NaN;
      }

      if (numberOfVertices == 1)
      {
         orthogonalProjectionOnLineSegment2D(listOfPointsInCluster.get(0), firstPoint, secondPoint, closestPointToPack);
         return listOfPointsInCluster.get(0).distance(closestPointToPack);
      }

      if (numberOfVertices == 2)
         return closestPoint2DsBetweenTwoLineSegment2Ds(firstPoint, secondPoint, listOfPointsInCluster.get(0), listOfPointsInCluster.get(1), closestPointToPack,
                                                        null);

      boolean pointIsVisible = isPointVisible(firstPoint, secondPoint, listOfPointsInCluster, closed);

      double minDistance = Double.POSITIVE_INFINITY;

      Point2DBasics closestPoint = new Point2D();
      for (int index = 0; index < numberOfVertices; index++)
      {
         Point2DReadOnly edgeStart = listOfPointsInCluster.get(index);
         Point2DReadOnly edgeEnd = listOfPointsInCluster.get(next(index, numberOfVertices));

         double distance = closestPoint2DsBetweenTwoLineSegment2Ds(firstPoint, secondPoint, edgeStart, edgeEnd, closestPoint, null);
         if (distance < minDistance)
         {
            minDistance = distance;
            closestPointToPack.set(closestPoint);
         }
      }

      minDistance = Math.sqrt(minDistance);

      if (!pointIsVisible)
         minDistance = -minDistance;
      return minDistance;
   }

   /**
    * This methods computes the minimum distance between the two 2D line segments with finite length.
    * <a href="http://geomalgorithms.com/a07-_distance.html"> Useful link</a>.
    *
    * @param lineSegmentStart1 the first endpoint of the first line segment. Not modified.
    * @param lineSegmentEnd1 the second endpoint of the first line segment. Not modified.
    * @param lineSegmentStart2 the first endpoint of the second line segment. Not modified.
    * @param lineSegmentEnd2 the second endpoint of the second line segment. Not modified.
    * @return the minimum distance between the two line segments.
    */
   public static double distanceBetweenTwoLineSegment2Ds(Point2DReadOnly lineSegmentStart1, Point2DReadOnly lineSegmentEnd1, Point2DReadOnly lineSegmentStart2,
                                                         Point2DReadOnly lineSegmentEnd2)
   {
      return closestPoint2DsBetweenTwoLineSegment2Ds(lineSegmentStart1, lineSegmentEnd1, lineSegmentStart2, lineSegmentEnd2, null, null);
   }


   /**
    * Given two 2D line segments with finite length, this methods computes two points P &in;
    * lineSegment1 and Q &in; lineSegment2 such that the distance || P - Q || is the minimum distance
    * between the two 2D line segments. <a href="http://geomalgorithms.com/a07-_distance.html"> Useful
    * link</a>.
    *
    * @param lineSegmentStart1 the first endpoint of the first line segment. Not modified.
    * @param lineSegmentEnd1 the second endpoint of the first line segment. Not modified.
    * @param lineSegmentStart2 the first endpoint of the second line segment. Not modified.
    * @param lineSegmentEnd2 the second endpoint of the second line segment. Not modified.
    * @param closestPointOnLineSegment1ToPack the 2D coordinates of the point P are packed in this 2D
    *           point. Modified. Can be {@code null}.
    * @param closestPointOnLineSegment2ToPack the 2D coordinates of the point Q are packed in this 2D
    *           point. Modified. Can be {@code null}.
    * @return the minimum distance between the two line segments.
    */
   public static double closestPoint2DsBetweenTwoLineSegment2Ds(Point2DReadOnly lineSegmentStart1, Point2DReadOnly lineSegmentEnd1,
                                                                Point2DReadOnly lineSegmentStart2, Point2DReadOnly lineSegmentEnd2,
                                                                Point2DBasics closestPointOnLineSegment1ToPack, Point2DBasics closestPointOnLineSegment2ToPack)
   {
      // Switching to the notation used in http://geomalgorithms.com/a07-_distance.html.
      // The line1 is defined by (P0, u) and the line2 by (Q0, v).
      Point2DReadOnly P0 = lineSegmentStart1;
      double ux = lineSegmentEnd1.getX() - lineSegmentStart1.getX();
      double uy = lineSegmentEnd1.getY() - lineSegmentStart1.getY();
      Point2DReadOnly Q0 = lineSegmentStart2;
      double vx = lineSegmentEnd2.getX() - lineSegmentStart2.getX();
      double vy = lineSegmentEnd2.getY() - lineSegmentStart2.getY();

      Point2DBasics Psc = closestPointOnLineSegment1ToPack;
      Point2DBasics Qtc = closestPointOnLineSegment2ToPack;

      double w0X = P0.getX() - Q0.getX();
      double w0Y = P0.getY() - Q0.getY();

      double a = ux * ux + uy * uy;
      double b = ux * vx + uy * vy;
      double c = vx * vx + vy * vy;
      double d = ux * w0X + uy * w0Y;
      double e = vx * w0X + vy * w0Y;

      double delta = a * c - b * b;

      double sc, sNumerator, sDenominator = delta;
      double tc, tNumerator, tDenominator = delta;

      // check to see if the lines are parallel
      if (delta <= ONE_MILLIONTH)
      {
         /*
          * The lines are parallel, there's an infinite number of pairs, but for one chosen point on one of
          * the lines, there's only one closest point to it on the other line. So let's choose arbitrarily a
          * point on the lineSegment1 and calculate the point that is closest to it on the lineSegment2.
          */
         sNumerator = 0.0;
         sDenominator = 1.0;
         tNumerator = e;
         tDenominator = c;
      }
      else
      {
         sNumerator = b * e - c * d;
         tNumerator = a * e - b * d;

         if (sNumerator < 0.0)
         {
            sNumerator = 0.0;
            tNumerator = e;
            tDenominator = c;
         }
         else if (sNumerator > sDenominator)
         {
            sNumerator = sDenominator;
            tNumerator = e + b;
            tDenominator = c;
         }
      }

      if (tNumerator < 0.0)
      {
         tNumerator = 0.0;
         sNumerator = -d;
         if (sNumerator < 0.0)
            sNumerator = 0.0;
         else if (sNumerator > a)
            sNumerator = a;
         sDenominator = a;
      }
      else if (tNumerator > tDenominator)
      {
         tNumerator = tDenominator;
         sNumerator = -d + b;
         if (sNumerator < 0.0)
            sNumerator = 0.0;
         else if (sNumerator > a)
            sNumerator = a;
         sDenominator = a;
      }

      sc = Math.abs(sNumerator) < ONE_MILLIONTH ? 0.0 : sNumerator / sDenominator;
      tc = Math.abs(tNumerator) < ONE_MILLIONTH ? 0.0 : tNumerator / tDenominator;

      double PscX = sc * ux + P0.getX();
      double PscY = sc * uy + P0.getY();

      double QtcX = tc * vx + Q0.getX();
      double QtcY = tc * vy + Q0.getY();

      if (Psc != null)
         Psc.set(PscX, PscY);
      if (Qtc != null)
         Qtc.set(QtcX, QtcY);

      double dx = PscX - QtcX;
      double dy = PscY - QtcY;
      return Math.sqrt(normSquared(dx, dy));
   }

   /**
    * Increments then recomputes the given {@code index} such that it is &in; [0, {@code listSize}[.
    * Examples:
    * <ul>
    * <li>{@code next(-1, 10)} returns 0.
    * <li>{@code next(10, 10)} returns 1.
    * <li>{@code next( 5, 10)} returns 6.
    * <li>{@code next(15, 10)} returns 6.
    * </ul>
    * </p>
    *
    * @param index the index to be incremented and wrapped if necessary.
    * @param listSize the size of the list around which the index is to be wrapped.
    * @return the wrapped incremented index.
    */
   public static int next(int index, int listSize)
   {
      return wrap(index + 1, listSize);
   }

   /**
    * Recomputes the given {@code index} such that it is &in; [0, {@code listSize}[.
    * <p>
    * The {@code index} remains unchanged if already &in; [0, {@code listSize}[.
    * <p>
    * Examples:
    * <ul>
    * <li>{@code wrap(-1, 10)} returns 9.
    * <li>{@code wrap(10, 10)} returns 0.
    * <li>{@code wrap( 5, 10)} returns 5.
    * <li>{@code wrap(15, 10)} returns 5.
    * </ul>
    * </p>
    *
    * @param index the index to be wrapped if necessary.
    * @param listSize the size of the list around which the index is to be wrapped.
    * @return the wrapped index.
    */
   public static int wrap(int index, int listSize)
   {
      index %= listSize;
      if (index < 0)
         index += listSize;
      return index;
   }

   private static boolean isNotInsideANonNavigableZone(Point2DReadOnly query, List<Cluster> clusters)
   {
      return clusters.stream().noneMatch(cluster -> cluster.isInsideNonNavigableZone(query));
   }

   public static boolean[] checkIfPointsInsidePlanarRegionAndOutsideNonNavigableZones(PlanarRegion homeRegion, List<Cluster> allClusters,
                                                                                      List<? extends Point2DReadOnly> navigableExtrusionPoints)
   {
      // We first go through the extrusions and check if they are actually navigable, i.e. inside the home region and not inside any non-navigable zone.
      boolean[] arePointsActuallyNavigable = new boolean[navigableExtrusionPoints.size()];
      Arrays.fill(arePointsActuallyNavigable, true);

      for (int i = 0; i < navigableExtrusionPoints.size(); i++)
      {
         // Check that the point is actually navigable
         Point2DReadOnly query = navigableExtrusionPoints.get(i);

         boolean isNavigable = PlanarRegionTools.isPointInLocalInsidePlanarRegion(homeRegion, query);

         if (isNavigable)
         {
            isNavigable = isNotInsideANonNavigableZone(query, allClusters);
         }

         arePointsActuallyNavigable[i] = isNavigable;
      }
      return arePointsActuallyNavigable;
   }

   //TODO: Rename.
   public static boolean isPointVisibleForStaticMaps(List<Cluster> clusters, Point2DReadOnly observer, Point2DReadOnly targetPoint)
   {
      for (Cluster cluster : clusters)
      {
         if (cluster.getExtrusionSide() == ExtrusionSide.OUTSIDE)
         {
            BoundingBox2D boundingBox = cluster.getNonNavigableExtrusionsBoundingBox();

            // If either the target or observer or both are in the bounding box, we have to do the thorough check.
            // If both are outside the bounding box, then we can check if the line segment does not intersect.
            // If that is the case, then the point is visible and we can check the next one.
            if (!boundingBox.isInsideInclusive(observer) && !boundingBox.isInsideInclusive(targetPoint))
            {
               if (!boundingBox.doesIntersectWithLineSegment2D(observer, targetPoint))
               {
                  continue;
               }
            }
         }

         boolean closed = cluster.isClosed();
         if (!VisibilityTools.isPointVisible(observer, targetPoint, cluster.getNonNavigableExtrusionsInLocal(), closed))
         {
            return false;
         }
      }

      return true;
   }
}
