/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sis.distance;

//JDK imports
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

// GeoAPI import
import org.opengis.geometry.DirectPosition;

//SIS imports
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.measure.Longitude;

/**
 * Represents a 2D point associated with a radius to enable great circle
 * estimation on earth surface.
 *
 * <div class="warning"><b>Warning:</b> This class may be refactored as a geometric object in a future SIS version.
 * Current implementation does not verify the CRS of circle center or the datum.</div>
 */
public class LatLonPointRadius {

  private final DirectPosition center;
  private final double radius;

  /**
   * Creates a representation of point-radius search region.
   *
   * @param center
   *          the center of the search region
   * @param radius
   *          the radius of the search region
   */
  public LatLonPointRadius(DirectPosition center, double radius) {
    this.center = center;
    this.radius = radius;
  }

  /**
   * Gets the circular region approximation on the earth surface using haversine
   * formula.
   *
   * @param numberOfPoints
   *          the number of points used to estimate the circular region
   * @return an array of DirectPosition2D representing the points that estimate the
   *         circular region
   */
  public DirectPosition2D[] getCircularRegionApproximation(int numberOfPoints) {
    if (radius >= DistanceUtils.HALF_EARTH_CIRCUMFERENCE) {
      DirectPosition2D[] points = new DirectPosition2D[5];
      points[0] = new DirectPosition2D(-180.0, -90.0);
      points[1] = new DirectPosition2D(-180.0, 90.0);
      points[2] = new DirectPosition2D(180.0, 90.0);
      points[3] = new DirectPosition2D(180.0, -90.0);
      points[4] = points[0];
      return points;
    }
    // plus one to add closing point
    DirectPosition2D[] points = new DirectPosition2D[numberOfPoints + 1];

    double bearingIncrement = 0;
    if (numberOfPoints > 0) { bearingIncrement = 360/numberOfPoints; }

    for (int i = 0; i < numberOfPoints; i++)
    {
      points[i] = DistanceUtils.getPointOnGreatCircle(center.getOrdinate(1),
          center.getOrdinate(0), radius, i * bearingIncrement);
    }

    points[numberOfPoints] = points[0];

    return points;
  }

  /**
   * Calculates the rectangular region enclosing the circular search region.
   *
   * @param numberOfPoints
   *          the number of points used to estimate the circular search region
   * @return Java Rectangle2D object that bounds the circlar search region
   */
  public Rectangle2D getRectangularRegionApproximation(int numberOfPoints) {
    if (radius >= DistanceUtils.HALF_EARTH_CIRCUMFERENCE) {
      return new Rectangle2D.Double(0.0, 0.0, 360.0, 180.0);
    }
    int numberOfCrossOvers = 0;

    Path2D path = new Path2D.Double();
    DirectPosition2D initPT = DistanceUtils.getPointOnGreatCircle(center.getOrdinate(1),
        center.getOrdinate(0), radius, 0);
    path.moveTo(initPT.x + 180.0, initPT.y + 90.0);

    DirectPosition2D currPT = initPT;

    for (int i = 1; i < 360; i++) {

      DirectPosition2D pt = DistanceUtils.getPointOnGreatCircle(center.getOrdinate(1),
          center.getOrdinate(0), radius, i);
      path.lineTo(pt.x + 180.0, pt.y + 90.0);

      if (dateLineCrossOver(Longitude.normalize(currPT.x), Longitude.normalize(pt.x))) {
        numberOfCrossOvers++;
      }
      currPT = pt;
    }
    if (dateLineCrossOver(Longitude.normalize(initPT.x), Longitude.normalize(currPT.x))) {
      numberOfCrossOvers++;
    }

    /**
     * If the path crosses the dateline once, it's a special case, so take care
     * of it differently. It will need to include areas around the pole.
     */
    if (numberOfCrossOvers == 1) {
      Rectangle2D r = path.getBounds2D();
      Rectangle2D lowerHalf = new Rectangle2D.Double(0.0, 0.0, 360.0, r.getMaxY());
      if (lowerHalf.contains(center.getOrdinate(0) + 180.0, center.getOrdinate(1) + 90.0)) {
        return lowerHalf;
      } else {
        return new Rectangle2D.Double(0.0, r.getMinY(), 360.0, 180.0 - r.getMinY());
      }
    }

    if (path.contains(center.getOrdinate(0) + 180.0, center.getOrdinate(1) + 90.0)) {
      Rectangle2D r = path.getBounds2D();
      if ((r.getMaxX() - r.getMinX()) > 359.0) {
        return new Rectangle2D.Double(0.0, 0.0, 360.0, 180.0);
      } else if (r.getMinX() < 0 || r.getMaxX() > 360.0) {
        /**
         * For circles that crosses the dateline instead of splitting in half
         * and having to go down the tree twice, for first version span
         * longitude 360.0 and use the exact height of the box
         */
        return new Rectangle2D.Double(0.0, r.getY(), 360.0, r.getHeight());
      } else {
        return path.getBounds2D();
      }
    } else {
      Area pathArea = new Area(path);
      Area wholeMap = new Area(new Rectangle2D.Double(0.0, 0.0, 360.0, 180.0));
      wholeMap.subtract(pathArea);
      return wholeMap.getBounds2D();
    }
  }

  /**
   * Returns true if the line segment connecting the two specified longitudes
   * crosses the international dateline.
   *
   * @param longitude1
   *          first longitude
   * @param longitude2
   *          second longitude
   * @return true if the line segment crosses the internation dateline, false
   *         otherwise
   */
  private static boolean dateLineCrossOver(double longitude1, double longitude2) {
    return (Math.abs(longitude1 - longitude2) > 180.0);
  }
}
