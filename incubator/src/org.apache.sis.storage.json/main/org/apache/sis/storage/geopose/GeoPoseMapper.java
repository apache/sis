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
package org.apache.sis.storage.geopose;

import java.time.Instant;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vector3D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.geopose.binding.Position;
import org.apache.sis.storage.geopose.binding.Quaternion;
import org.apache.sis.storage.geopose.binding.YawPitchRoll;


/**
 * Converts GeoPose JSON bindings to and from Apache SIS referencing and geometry types.
 *
 * <ul>
 *   <li>Positions are expressed in WGS 84 geographic 3D (EPSG:4979), whose axis order is
 *       latitude, longitude, ellipsoidal height.</li>
 *   <li>GeoPose angles are in decimal degrees whereas {@link org.apache.sis.maths.Quaternion}
 *       works in radians; this class converts between the two.</li>
 *   <li>GeoPose times are in milliseconds since 1970-01-01T00:00:00Z.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0"
 */
public class GeoPoseMapper {

    public GeoPoseMapper() {
    }

    /**
     * Returns the given GeoPose position as a direct position in WGS 84 geographic 3D.
     *
     * @param  position  the position to convert
     * @return the position in EPSG::4979, with coordinates in latitude, longitude, height order
     */
    public GeneralDirectPosition toDirectPosition(final Position position) {
        final GeneralDirectPosition target = new GeneralDirectPosition(CommonCRS.WGS84.geographic3D());
        target.setCoordinate(0, position.getLat());
        target.setCoordinate(1, position.getLon());
        target.setCoordinate(2, position.getH());
        return target;
    }

    /**
     * Returns the given direct position as a GeoPose position.
     *
     * @param  position  the position to convert, in latitude, longitude, height order
     * @return the GeoPose position
     * @throws IllegalArgumentException if the given position is not three-dimensional
     */
    public Position toPosition(final DirectPosition position) {
        if (position.getDimension() != 3) {
            throw new IllegalArgumentException("A GeoPose position requires 3 dimensions, found "
                    + position.getDimension() + '.');
        }
        return new Position(position.getCoordinate(0), position.getCoordinate(1), position.getCoordinate(2));
    }

    /**
     * Returns the given GeoPose quaternion as a quaternion which supports arithmetic.
     *
     * @param  quaternion  the quaternion to convert
     * @return the orientation
     */
    public org.apache.sis.maths.Quaternion toOrientation(final Quaternion quaternion) {
        return new org.apache.sis.maths.Quaternion(
                quaternion.getX(), quaternion.getY(), quaternion.getZ(), quaternion.getW());
    }

    /**
     * Returns the orientation described by the given yaw, pitch and roll angles.
     *
     * <p>The three angles are applied as successive rotations about the z, y and x axes,
     * which is the convention used by both GeoPose and {@link org.apache.sis.maths.Quaternion}.</p>
     *
     * @param  angles  the angles to convert, in decimal degrees
     * @return the equivalent orientation
     */
    public org.apache.sis.maths.Quaternion toOrientation(final YawPitchRoll angles) {
        final Vector3D.Double euler = new Vector3D.Double(
                Math.toRadians(angles.getYaw()),
                Math.toRadians(angles.getPitch()),
                Math.toRadians(angles.getRoll()));
        return new org.apache.sis.maths.Quaternion().setFromEuler(euler);
    }

    /**
     * Returns the given orientation as a GeoPose quaternion.
     *
     * @param  orientation  the orientation to convert
     * @return the GeoPose quaternion
     */
    public Quaternion toQuaternion(final org.apache.sis.maths.Quaternion orientation) {
        return new Quaternion(orientation.getX(), orientation.getY(), orientation.getZ(), orientation.getW());
    }

    /**
     * Returns the given orientation as yaw, pitch and roll angles.
     *
     * @param  orientation  the orientation to convert
     * @return the equivalent angles, in decimal degrees
     */
    public YawPitchRoll toYawPitchRoll(final org.apache.sis.maths.Quaternion orientation) {
        final Vector<?> euler = orientation.toEuler();
        return new YawPitchRoll(
                Math.toDegrees(euler.get(0)),
                Math.toDegrees(euler.get(1)),
                Math.toDegrees(euler.get(2)));
    }

    /**
     * Returns the instant denoted by the given GeoPose time.
     *
     * @param  validTime  milliseconds since the Unix epoch, or null
     * @return the instant, or null if the given time was null
     */
    public Instant toInstant(final Long validTime) {
        return (validTime == null) ? null : Instant.ofEpochMilli(validTime);
    }

    /**
     * Returns the GeoPose time of the given instant.
     *
     * @param  instant  the instant to convert, or null
     * @return milliseconds since the Unix epoch, or null if the given instant was null
     */
    public Long toValidTime(final Instant instant) {
        return (instant == null) ? null : instant.toEpochMilli();
    }
}
