/**
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

import org.apache.sis.projections.CartesianTierPlotter;
import org.apache.sis.projections.Projection;
import org.apache.sis.projections.SinusoidalProjector;

public class TierBoundaries {

  CartesianTierPlotter ctp;
  public int defaultLevel = 16;
  Projection projection = new SinusoidalProjector();
  PointsD topLeft, topRight, bottomLeft, bottomRight;

  public PointsD getTopLeft() {
    return topLeft;
  }

  public void setTopLeft(PointsD topLeft) {
    this.topLeft = topLeft;
  }

  public PointsD getTopRight() {
    return topRight;
  }

  public void setTopRight(PointsD topRight) {
    this.topRight = topRight;
  }

  public PointsD getBottomLeft() {
    return bottomLeft;
  }

  public void setBottomLeft(PointsD bottomLeft) {
    this.bottomLeft = bottomLeft;
  }

  public PointsD getBottomRight() {
    return bottomRight;
  }

  public void setBottomRight(PointsD bottomRight) {
    this.bottomRight = bottomRight;
  }

  public static void main(String[] args) {
    TierBoundaries tb = new TierBoundaries();
    tb.getBoundaries(37.43629392840975, -122.13848663330079);
  }

  public TierBoundaries() {
    ctp = new CartesianTierPlotter(defaultLevel, projection);

  }

  public TierBoundaries(Projection Projection) {
    this.projection = Projection;
    ctp = new CartesianTierPlotter(defaultLevel, Projection);
  }

  public TierBoundaries(CartesianTierPlotter ctp) {
    this.ctp = ctp;
  }

  public void getBoundaries(double lat, double lng) {

    double baseBoxId = ctp.getTierBoxId(lat, lng);
    double currentBoxLeft = baseBoxId, currentBoxRight = baseBoxId;
    double incVal = 0.000000001;
    double rightLng = lng, leftLng = lng;
    do {
      if (currentBoxLeft == baseBoxId) {
        rightLng -= incVal;
        currentBoxLeft = ctp.getTierBoxId(lat, rightLng);
      }

      if (currentBoxRight == baseBoxId) {
        leftLng += incVal;
        currentBoxRight = ctp.getTierBoxId(lat, leftLng);
      }

    } while (currentBoxLeft == baseBoxId || currentBoxRight == baseBoxId);

    double downLat = lat, upLat = lat;
    currentBoxLeft = baseBoxId;
    currentBoxRight = baseBoxId;
    do {
      if (currentBoxLeft == baseBoxId) {
        downLat -= incVal;
        currentBoxLeft = ctp.getTierBoxId(downLat, lng);
      }

      if (currentBoxRight == baseBoxId) {
        upLat += incVal;
        currentBoxRight = ctp.getTierBoxId(upLat, lng);
      }

    } while (currentBoxLeft == baseBoxId || currentBoxRight == baseBoxId);

    topLeft = new PointsD(upLat, leftLng);
    topRight = new PointsD(upLat, rightLng);
    bottomLeft = new PointsD(downLat, leftLng);
    bottomRight = new PointsD(downLat, rightLng);
  }

  public String toKMLCoords() {

    return topLeft.toKMLCoord() + ",0\n" + topRight.toKMLCoord() + ",0\n"
        + bottomRight.toKMLCoord() + ",0\n" + bottomLeft.toKMLCoord() + ",0\n"
        + topLeft.toKMLCoord() + ",0\n";
  }

  @Override
  public String toString() {

    return topLeft.toString() + "\n" + topRight.toString() + "\n"
        + bottomRight.toString() + "\n" + bottomLeft.toString() + "\n"
        + topLeft.toString() + "n";
  }
}
