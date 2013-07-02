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

package org.apache.sis.core;

//JDK imports
import java.awt.geom.Rectangle2D;

//SIS imports
import org.apache.sis.geometry.Envelope2D;

/**
 * Represents a 2D rectangle on earth surface specified by lower left and upper
 * right coordinates.
 *
 * @deprecated Replaced by {@link org.opengis.geometry.Envelope}, which is derived from OGC/ISO specifications.
 */
@Deprecated
public class LatLonRect extends Envelope2D {

  /**
   * Creates representation of 2D rectangle.
   *
   * @param lowerLeft
   *          lower left coordinatee of rectangle
   * @param upperRight
   *          upper right coordinate of rectangle
   */
  public LatLonRect(LatLon lowerLeft, LatLon upperRight) {
    super(lowerLeft, upperRight);
  }

  /**
   * Returns true if the rectangle crosses the international dateline.
   *
   * @return true if the rectangle crosses the international dateline, false
   *         otherwise
   */
  private boolean crossesDateLine() {
    return width < 0;
  }

  /**
   * Calculates the rectangles that makes up this rectangle. Need to do some
   * calculation because rectangular region can cross the dateline and will need
   * to be represented as two separate rectangles
   *
   * @return an array of Java Rectangle2D representing this rectangular region
   *         on the earth surface
   */
  public Rectangle2D[] getJavaRectangles() {
    Rectangle2D[] rect;
    if (crossesDateLine()) {
      rect = new Rectangle2D[2];

      LatLonRect west = new LatLonRect(new LatLon(y,
          -180.0), new LatLon(y + height, x + width));
      LatLonRect east = new LatLonRect(new LatLon(y,
          x), new LatLon(y + height, 180.0));
      rect[0] = getJavaRectangle(west);
      rect[1] = getJavaRectangle(east);
    } else {
      rect = new Rectangle2D[1];
      rect[0] = getJavaRectangle(this);
    }
    return rect;
  }

  /**
   * Creates a Java Rectangle2D of the specified LatLonRect.
   *
   * @param rect
   *          specified LatLonRect
   * @return Java Rectangle2D
   */
  private Rectangle2D getJavaRectangle(LatLonRect rect) {
    return new Rectangle2D.Double(rect.x + 180,
        rect.y + 90, rect.width, rect.height);
  }
}
