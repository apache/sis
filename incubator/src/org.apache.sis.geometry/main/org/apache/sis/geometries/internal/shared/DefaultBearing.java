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
package org.apache.sis.geometries.internal.shared;

import java.util.Arrays;
import java.util.Objects;
import javax.measure.Quantity;
import javax.measure.quantity.Angle;
import org.apache.sis.geometries.cs.Bearing;
import org.apache.sis.geometries.cs.ReferenceDirection;
import org.apache.sis.geometries.cs.Rotation;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;


/**
 * A direction at a point, stored in both its angular and its vectorial form.
 *
 * <p>Whichever form is given at construction, the other one is derived immediately when the
 * coordinate system allows it, so that both accessors can answer without further computation.
 * Instances are immutable.</p>
 *
 * <h2>Conventions</h2>
 * The azimuth is measured in the plane of the <var>east</var> and <var>north</var> axes, from
 * <var>north</var> toward <var>east</var> when the rotation is {@linkplain Rotation#CLOCKWISE
 * clockwise}, and is expressed in degrees in the range of 0 inclusive to 360 exclusive. The
 * altitude is measured from that plane toward the <var>up</var> axis, in degrees in the range of
 * −90 to +90 inclusive. The rotation affects the azimuth only: the sign of the altitude is fixed
 * by the up axis.
 *
 * <p>Those three axes are located by their {@linkplain AxisDirection direction} in the coordinate
 * reference system of the direction vector, so that a bearing has the same azimuth whether its
 * vector is expressed in a (<var>latitude</var>, <var>longitude</var>) or in a
 * (<var>longitude</var>, <var>latitude</var>) system. When that system is absent or has no such
 * axes, the coordinates are taken in the order in which they are given.</p>
 *
 * <p>Angles can be derived only from a vector of dimension 2 or 3. In any other dimension the
 * mapping from coordinates to a horizontal plane is not known — a fourth axis may be time or a
 * parameter — so both angles are absent and only the direction is available.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.2.22
 */
public final class DefaultBearing implements Bearing {
    /**
     * Maximum number of nested reference directions.
     * ISO 19107 requires the reference direction of a bearing to not refer to that bearing
     * transitively (REQ. 44). Because an implementation of {@link Bearing} other than this one may
     * return a new instance on each call to {@link Bearing#getReference()}, the chain is bounded by
     * a maximum depth.
     */
    private static final int MAX_REFERENCE_DEPTH = 100;

    /**
     * Angle from the reference direction, in degrees in the range of 0 inclusive to 360 exclusive,
     * or {@link Double#NaN} if this bearing has no angular form.
     */
    private final double azimuth;

    /**
     * Angle above the horizontal plane, in degrees in the range of −90 to +90 inclusive,
     * or {@link Double#NaN} if this bearing lies in that plane or has no angular form.
     */
    private final double altitude;

    /**
     * Direction of this bearing as a unit vector. Never null and never exposed directly,
     * a copy being returned instead.
     */
    private final double[] coordinates;

    /**
     * The system in which {@link #coordinates} are expressed. Never null.
     */
    private final SampleSystem system;

    /**
     * Direction from which {@link #azimuth} is measured. Never null.
     */
    private final ReferenceDirection reference;

    /**
     * Sense in which {@link #azimuth} is measured. Never null.
     */
    private final Rotation rotation;

    /**
     * Creates a bearing from its angular form, deriving its direction vector.
     * The derived vector has two coordinates if the altitude is null and three otherwise,
     * given in the <var>east</var>, <var>north</var>, <var>up</var> order.
     *
     * @param  azimuth    angle from the reference direction, not null.
     * @param  altitude   angle above the horizontal plane, or {@code null} if the bearing lies in it.
     * @param  reference  direction from which the azimuth is measured, not null.
     * @param  rotation   sense in which the azimuth is measured, not null.
     * @throws IllegalArgumentException if an angle is not finite, if the altitude is outside the
     *         range of −90 to +90 degrees, or if the reference direction refers to this bearing.
     */
    public DefaultBearing(final Quantity<Angle> azimuth, final Quantity<Angle> altitude,
                          final ReferenceDirection reference, final Rotation rotation)
    {
        ArgumentChecks.ensureNonNull("azimuth",  azimuth);
        ArgumentChecks.ensureNonNull("rotation", rotation);
        this.reference = validate(reference);
        this.rotation  = rotation;
        this.azimuth   = normalizeDegrees(degrees("azimuth", azimuth));
        if (altitude == null) {
            this.altitude = Double.NaN;
        } else {
            final double h = degrees("altitude", altitude);
            if (!(h >= -90 && h <= 90)) {
                throw new IllegalArgumentException("The altitude of a bearing shall be between "
                        + "-90° and +90°, but got " + h + "°.");
            }
            this.altitude = h;
        }
        /*
         * Derive the direction. The azimuth turns from north toward east, which is the direction
         * of decreasing angle in the trigonometric sense, hence the sine on the east coordinate
         * and the cosine on the north one.
         */
        double a = Math.toRadians(this.azimuth);
        if (rotation == Rotation.COUNTER_CLOCKWISE) {
            a = -a;
        }
        if (Double.isNaN(this.altitude)) {
            coordinates = new double[] {Math.sin(a), Math.cos(a)};
        } else {
            final double h = Math.toRadians(this.altitude);
            final double c = Math.cos(h);
            coordinates = new double[] {c * Math.sin(a), c * Math.cos(a), Math.sin(h)};
        }
        system = SampleSystem.ofSize(coordinates.length);
    }

    /**
     * Creates a bearing from a direction vector, deriving its angular form when the dimension of
     * that vector allows it. Only the direction of the given vector is retained; its length is
     * normalized to one.
     *
     * @param  direction  vector giving the direction of the bearing, not null.
     * @param  reference  direction from which the azimuth is measured, not null.
     * @param  rotation   sense in which the azimuth is measured, not null.
     * @throws IllegalArgumentException if the vector has a coordinate which is not finite, if its
     *         length is zero, or if the reference direction refers to this bearing.
     */
    public DefaultBearing(final Vector<?> direction, final ReferenceDirection reference, final Rotation rotation) {
        ArgumentChecks.ensureNonNull("direction", direction);
        ArgumentChecks.ensureNonNull("rotation",  rotation);
        this.reference = validate(reference);
        this.rotation  = rotation;
        /*
         * `toArrayDouble()` returns a new array, which gives the defensive copy for free and at
         * full precision. The vector is not copied then normalized, because a copy keeps the data
         * type of its source and normalizing an integer vector would round every coordinate.
         */
        final double[] c = direction.toArrayDouble();
        for (final double v : c) {
            if (!Double.isFinite(v)) {
                throw new IllegalArgumentException("The direction of a bearing shall have finite "
                        + "coordinates, but got " + Arrays.toString(c) + '.');
            }
        }
        final double length = Math.sqrt(Vectors.lengthSquare(c));
        if (!(length > 0)) {
            throw new IllegalArgumentException("The direction of a bearing shall not have a length "
                    + "of zero, because only its direction is significant.");
        }
        for (int i=0; i<c.length; i++) {
            c[i] /= length;
        }
        coordinates = c;
        final SampleSystem s = direction.getSampleSystem();
        system = (s != null) ? s : SampleSystem.ofSize(c.length);
        /*
         * Derive the angles. `atan2(east, north)` and not `atan2(north, east)`, because the angle
         * is measured from the north axis: north gives 0°, east 90°, south 180° and west 270°.
         * At the zenith and at the nadir the azimuth is undefined; `atan2(0,0)` is zero, so it is
         * arbitrarily reported as 0°.
         */
        final int[] axes = horizontalAxes(system, c.length);
        if (axes == null) {
            azimuth  = Double.NaN;
            altitude = Double.NaN;
        } else {
            double a = Math.toDegrees(Math.atan2(coordinate(c, axes[0]), coordinate(c, axes[1])));
            if (rotation == Rotation.COUNTER_CLOCKWISE) {
                a = -a;
            }
            azimuth = normalizeDegrees(a);
            if (axes.length < 3) {
                altitude = Double.NaN;
            } else {
                final double z = coordinate(c, axes[2]);
                altitude = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, z))));
            }
        }
    }

    /**
     * Returns the value of the given angle in degrees.
     *
     * @param  name   name of the argument, for the error message.
     * @param  angle  the angle to convert, not null.
     * @return the given angle in degrees.
     * @throws IllegalArgumentException if the angle is not finite.
     */
    private static double degrees(final String name, final Quantity<Angle> angle) {
        final double value = angle.to(Units.DEGREE).getValue().doubleValue();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("The " + name + " of a bearing shall be a finite "
                    + "angle, but got " + angle + '.');
        }
        return value;
    }

    /**
     * Returns the given angle in the range of 0 inclusive to 360 exclusive.
     *
     * @param  angle  the angle to reduce, in degrees.
     * @return the given angle reduced to one turn.
     */
    private static double normalizeDegrees(double angle) {
        angle %= 360;
        if (angle < 0) {
            angle += 360;
        }
        if (angle >= 360) {
            /*
             * A very small negative angle added to 360 may round up to exactly 360.
             */
            angle = 0;
        }
        /*
         * Turn a negative zero into a positive one: the remainder of a negative zero is a negative
         * zero, which compares as a different value in `equals(Object)`.
         */
        return angle + 0.0;
    }

    /**
     * Verifies that the given reference direction does not refer to the bearing being created.
     *
     * @param  reference  the reference direction to verify, not null.
     * @return the given reference direction.
     * @throws IllegalArgumentException if the chain of reference directions is too deep,
     *         which is the case in particular if it contains a cycle.
     */
    private static ReferenceDirection validate(final ReferenceDirection reference) {
        ArgumentChecks.ensureNonNull("reference", reference);
        int depth = 0;
        for (ReferenceDirection r = reference; r instanceof Bearing b;) {
            if (++depth > MAX_REFERENCE_DEPTH) {
                throw new IllegalArgumentException("The reference direction of a bearing shall not "
                        + "refer to that bearing transitively, but the chain of reference directions "
                        + "given as \"reference\" is more than " + MAX_REFERENCE_DEPTH + " levels deep.");
            }
            r = b.getReference();
        }
        return reference;
    }

    /**
     * Returns the indices of the east, north and, in three dimensions, up axes of the given system.
     * A negative value <var>v</var> means that the axis at index {@code -1-v} points the opposite
     * way, so that its coordinate shall be negated.
     *
     * @param  system     the system in which the coordinates are expressed, not null.
     * @param  dimension  number of coordinates.
     * @return indices of the east, north and up axes, or {@code null} if no angle can be derived.
     */
    private static int[] horizontalAxes(final SampleSystem system, final int dimension) {
        if (dimension < 2 || dimension > 3) {
            return null;
        }
        final CoordinateReferenceSystem crs = system.getCoordinateReferenceSystem();
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            final int east  = indexOf(cs, AxisDirection.EAST,  dimension);
            final int north = indexOf(cs, AxisDirection.NORTH, dimension);
            if (east != Integer.MIN_VALUE && north != Integer.MIN_VALUE) {
                if (dimension < 3) {
                    return new int[] {east, north};
                }
                final int up = indexOf(cs, AxisDirection.UP, dimension);
                if (up != Integer.MIN_VALUE) {
                    return new int[] {east, north, up};
                }
            }
        }
        /*
         * No coordinate reference system, or no such axes in it. This is the case of the vectors
         * derived from an angular bearing, and of the engineering systems used for geometries
         * which are not georeferenced. Take the coordinates in the order they are given.
         */
        return (dimension < 3) ? new int[] {0, 1} : new int[] {0, 1, 2};
    }

    /**
     * Returns the index of the axis having the given direction, negated as {@code -1-index}
     * if that axis points the opposite way.
     *
     * @param  cs         the coordinate system to inspect, not null.
     * @param  direction  the direction of the axis to search.
     * @param  dimension  number of coordinates available.
     * @return the encoded index, or {@link Integer#MIN_VALUE} if no usable axis was found.
     */
    private static int indexOf(final CoordinateSystem cs, final AxisDirection direction, final int dimension) {
        final int i = AxisDirections.indexOfColinear(cs, direction);
        if (i < 0 || i >= dimension) {
            return Integer.MIN_VALUE;
        }
        return direction.equals(cs.getAxis(i).getDirection()) ? i : (-1 - i);
    }

    /**
     * Returns the coordinate designated by the given index, as encoded by
     * {@link #indexOf(CoordinateSystem, AxisDirection, int)}.
     *
     * @param  coordinates  the coordinates from which to read.
     * @param  index        the encoded index of the coordinate to read.
     * @return the designated coordinate, negated if its axis points the opposite way.
     */
    private static double coordinate(final double[] coordinates, final int index) {
        return (index >= 0) ? coordinates[index] : -coordinates[-1 - index];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quantity<Angle> getAzimuth() {
        return Double.isNaN(azimuth) ? null : Quantities.create(azimuth, Units.DEGREE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Quantity<Angle> getAltitude() {
        return Double.isNaN(altitude) ? null : Quantities.create(altitude, Units.DEGREE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method returns a new vector on each call, because vectors are mutable.
     * Modifying the returned vector has no effect on this bearing.</p>
     */
    @Override
    public Vector<?> getDirection() {
        final Vector<?> v = Vectors.create(system, DataType.DOUBLE);
        for (int i=0; i<coordinates.length; i++) {
            v.set(i, coordinates[i]);
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceDirection getReference() {
        return reference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rotation getRotation() {
        return rotation;
    }

    /**
     * Compares this bearing with the given object for equality.
     * Values are compared exactly, with no tolerance, so a bearing created from angles and a
     * bearing created from an equivalent direction vector are not necessarily equal.
     *
     * @param  other  the object to compare with this bearing, or {@code null}.
     * @return whether the given object is a bearing equal to this one.
     */
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof DefaultBearing)) {
            return false;
        }
        final DefaultBearing that = (DefaultBearing) other;
        return Double.doubleToLongBits(azimuth)  == Double.doubleToLongBits(that.azimuth)
            && Double.doubleToLongBits(altitude) == Double.doubleToLongBits(that.altitude)
            && Arrays.equals(coordinates, that.coordinates)
            && system.equals(that.system)
            && reference.equals(that.reference)
            && rotation == that.rotation;
    }

    /**
     * Returns a hash code value for this bearing.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(azimuth, altitude, system, reference, rotation) ^ Arrays.hashCode(coordinates);
    }

    /**
     * Returns a string representation of this bearing for debugging purposes.
     * The format is not guaranteed to remain the same in future versions.
     *
     * @return a string representation of this bearing.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Bearing[");
        if (!Double.isNaN(azimuth)) {
            sb.append("azimuth=").append(azimuth).append("°, ");
            if (!Double.isNaN(altitude)) {
                sb.append("altitude=").append(altitude).append("°, ");
            }
        }
        return sb.append("direction=").append(Arrays.toString(coordinates))
                 .append(", reference=").append(reference)
                 .append(", rotation=").append(rotation)
                 .append(']').toString();
    }
}
