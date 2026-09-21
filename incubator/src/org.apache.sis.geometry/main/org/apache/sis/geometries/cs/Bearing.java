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
package org.apache.sis.geometries.cs;

import javax.measure.Quantity;
import javax.measure.quantity.Angle;
import org.apache.sis.geometries.internal.shared.DefaultBearing;
import org.apache.sis.maths.Vector;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A direction at a point, expressed either as a set of angles or as a tangent vector.
 *
 * <p>In the angular form, the first angle is an azimuth measured in the tangent plane from a
 * reference direction, and the second one is an altitude, positive above the horizontal and
 * negative below it. In the vector form, the direction is a unit vector of the coordinate system at
 * the point. Both forms carry the same information; only their interpretation differs, and that
 * interpretation depends on a reference direction giving the zero offset and on a rotation
 * direction.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>A bearing may be valid only at the point from which it is measured: transporting a vector
 *       to another point is valid only if the geometric reference surface is planar.</li>
 *   <li>A fixed reference direction such as true north allows some transport, but only where that
 *       reference exists and is unique. True north does not exist at the North pole and is not
 *       unique at the South pole.</li>
 *   <li>The reference direction of a bearing shall not refer to that bearing transitively.</li>
 *   <li>The magnitude of the vector has no effect: only its direction matters.</li>
 * </ul>
 *
 * <p>Difference with ISO 19107: the {@code angle} attribute, an ordered list of zero to two angles,
 * is split into the two named accessors {@link #getAzimuth()} and {@link #getAltitude()}, which are
 * respectively its first and second element. An altitude is therefore present only if an azimuth is
 * present.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.2.22
 */
@UML(identifier="Bearing", specification=ISO_19107)
public interface Bearing extends ReferenceDirection {
    /**
     * Angle measured in the tangent plane from the {@linkplain #getReference() reference direction},
     * in the sense given by the {@linkplain #getRotation() rotation}, or {@code null} if this bearing
     * has no angular form.
     *
     * <p>The angle is returned in degrees, in the range of 0 inclusive to 360 exclusive. It is
     * absent only when the direction vector has a dimension from which no azimuth can be derived,
     * which is any dimension other than 2 and 3.</p>
     *
     * @return azimuth of this bearing, or {@code null} if none.
     *
     * @see ISO 19107:2019 - 6.2.22.2
     */
    @UML(identifier="angle", specification=ISO_19107)
    Quantity<Angle> getAzimuth();

    /**
     * Angle measured from the plane tangent to the reference surface, positive above it and negative
     * below it, or {@code null} if this bearing is confined to that plane.
     *
     * <p>The angle is returned in degrees, in the range of −90 to +90 inclusive. An altitude is
     * present only if an {@linkplain #getAzimuth() azimuth} is present.</p>
     *
     * @return altitude of this bearing, or {@code null} if none.
     *
     * @see ISO 19107:2019 - 6.2.22.2
     */
    @UML(identifier="angle", specification=ISO_19107)
    Quantity<Angle> getAltitude();

    /**
     * Direction of this bearing as a unit vector of the coordinate system at the point.
     *
     * <p>Only the direction of that vector is significant; its length is always one, whatever the
     * length of the vector this bearing was created from.</p>
     *
     * @return direction of this bearing, or {@code null} if none.
     *
     * @see ISO 19107:2019 - 6.2.22.3
     */
    @UML(identifier="direction", specification=ISO_19107)
    Vector<?> getDirection();

    /**
     * Direction from which the {@linkplain #getAzimuth() azimuth} is measured, that is the direction
     * of a zero angle.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>This direction shall not refer to this bearing transitively.</li>
     * </ul>
     *
     * @return reference direction of this bearing, never null.
     *
     * @see ISO 19107:2019 - 6.2.22.4
     */
    @UML(identifier="reference", specification=ISO_19107)
    ReferenceDirection getReference();

    /**
     * Sense in which the {@linkplain #getAzimuth() azimuth} is measured from the
     * {@linkplain #getReference() reference direction}.
     *
     * @return rotation sense of this bearing, never null.
     *
     * @see ISO 19107:2019 - 6.2.22.5
     */
    @UML(identifier="rotation", specification=ISO_19107)
    Rotation getRotation();

    /**
     * Creates a bearing measured clockwise from true north by the given azimuth.
     *
     * @param  azimuth  angle from the reference direction, not null.
     * @return a bearing at the given azimuth.
     * @throws IllegalArgumentException if the azimuth is not a finite angle.
     *
     * @see ISO 19107:2019 - 6.2.22.6
     */
    static Bearing ofAzimuth(final Quantity<Angle> azimuth) {
        return ofAzimuth(azimuth, null, FixedDirection.TRUE_NORTH, Rotation.CLOCKWISE);
    }

    /**
     * Creates a bearing measured clockwise from true north by the given azimuth and altitude.
     *
     * @param  azimuth   angle from the reference direction, not null.
     * @param  altitude  angle above the tangent plane, or {@code null} if the bearing lies in it.
     * @return a bearing at the given angles.
     * @throws IllegalArgumentException if an angle is not finite, or if the altitude is outside
     *         the range of −90 to +90 degrees.
     *
     * @see ISO 19107:2019 - 6.2.22.6
     */
    static Bearing ofAzimuth(final Quantity<Angle> azimuth, final Quantity<Angle> altitude) {
        return ofAzimuth(azimuth, altitude, FixedDirection.TRUE_NORTH, Rotation.CLOCKWISE);
    }

    /**
     * Creates a bearing measured from the given reference direction in the given sense.
     *
     * @param  azimuth    angle from the reference direction, not null.
     * @param  altitude   angle above the tangent plane, or {@code null} if the bearing lies in it.
     * @param  reference  direction from which the azimuth is measured, not null.
     * @param  rotation   sense in which the azimuth is measured, not null.
     * @return a bearing at the given angles.
     * @throws IllegalArgumentException if an angle is not finite, if the altitude is outside the
     *         range of −90 to +90 degrees, or if the reference direction refers to the bearing
     *         being created.
     *
     * @see ISO 19107:2019 - 6.2.22.6
     */
    static Bearing ofAzimuth(final Quantity<Angle> azimuth, final Quantity<Angle> altitude,
                             final ReferenceDirection reference, final Rotation rotation)
    {
        return new DefaultBearing(azimuth, altitude, reference, rotation);
    }

    /**
     * Creates a bearing in the direction of the given vector, measured clockwise from true north.
     *
     * @param  direction  vector giving the direction of the bearing, not null.
     * @return a bearing in the direction of the given vector.
     * @throws IllegalArgumentException if the vector has a coordinate which is not finite,
     *         or if its length is zero.
     *
     * @see ISO 19107:2019 - 6.2.22.6
     */
    static Bearing ofDirection(final Vector<?> direction) {
        return ofDirection(direction, FixedDirection.TRUE_NORTH, Rotation.CLOCKWISE);
    }

    /**
     * Creates a bearing in the direction of the given vector, with the given reference and sense.
     * The reference direction and the rotation do not change the direction of the bearing; they
     * are the convention under which its {@linkplain #getAzimuth() azimuth} is expressed.
     *
     * @param  direction  vector giving the direction of the bearing, not null.
     * @param  reference  direction from which the azimuth is measured, not null.
     * @param  rotation   sense in which the azimuth is measured, not null.
     * @return a bearing in the direction of the given vector.
     * @throws IllegalArgumentException if the vector has a coordinate which is not finite, if its
     *         length is zero, or if the reference direction refers to the bearing being created.
     *
     * @see ISO 19107:2019 - 6.2.22.6
     */
    static Bearing ofDirection(final Vector<?> direction,
                               final ReferenceDirection reference, final Rotation rotation)
    {
        return new DefaultBearing(direction, reference, rotation);
    }
}
