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
package org.apache.sis.referencing.cs;

import java.util.Map;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Immutable;


/**
 * A 1-, 2-, or 3-dimensional Cartesian coordinate system. The position of points are relative
 * to orthogonal straight axes in the 2- and 3-dimensional cases. In the 1-dimensional case,
 * the coordinate system contains a single straight coordinate axis. All axes shall have the
 * same linear unit of measure.
 *
 * <table class="sis">
 * <tr><th>Used with CRS type(s)</th></tr>
 * <tr><td>
 *   {@linkplain org.geotoolkit.referencing.crs.DefaultGeocentricCRS  Geocentric},
 *   {@linkplain org.geotoolkit.referencing.crs.DefaultProjectedCRS   Projected},
 *   {@linkplain org.geotoolkit.referencing.crs.DefaultEngineeringCRS Engineering},
 *   {@linkplain org.geotoolkit.referencing.crs.DefaultImageCRS       Image}
 * </td></tr></table>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
@Immutable
public class DefaultCartesianCS extends DefaultAffineCS implements CartesianCS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6182037957705712945L;

    public static final DefaultCartesianCS GEOCENTRIC = null; // TODO: Not supported yet.

    /**
     * Constructs a one-dimensional coordinate system from a set of properties.
     * The properties map is given unchanged to the
     * {@linkplain AbstractCS#AbstractCS(Map,CoordinateSystemAxis[]) super-class constructor}.
     *
     * @param properties The properties to be given to the identified object.
     * @param axis The axis.
     */
    public DefaultCartesianCS(final Map<String,?>   properties,
                              final CoordinateSystemAxis axis)
    {
        super(properties, new CoordinateSystemAxis[] {axis});
        ensurePerpendicularAxis();
    }

    /**
     * Constructs a two-dimensional coordinate system from a set of properties.
     * The properties map is given unchanged to the
     * {@linkplain AbstractCS#AbstractCS(Map,CoordinateSystemAxis[]) super-class constructor}.
     *
     * @param properties The properties to be given to the identified object.
     * @param axis0 The first axis.
     * @param axis1 The second axis.
     */
    public DefaultCartesianCS(final Map<String,?>   properties,
                              final CoordinateSystemAxis axis0,
                              final CoordinateSystemAxis axis1)
    {
        super(properties, axis0, axis1);
        ensurePerpendicularAxis();
    }

    /**
     * Constructs a three-dimensional coordinate system from a set of properties.
     * The properties map is given unchanged to the
     * {@linkplain AbstractCS#AbstractCS(Map,CoordinateSystemAxis[]) super-class constructor}.
     *
     * @param properties The properties to be given to the identified object.
     * @param axis0 The first axis.
     * @param axis1 The second axis.
     * @param axis2 The third axis.
     */
    public DefaultCartesianCS(final Map<String,?>   properties,
                              final CoordinateSystemAxis axis0,
                              final CoordinateSystemAxis axis1,
                              final CoordinateSystemAxis axis2)
    {
        super(properties, axis0, axis1, axis2);
        ensurePerpendicularAxis();
    }

    /**
     * Creates a new coordinate system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param cs The coordinate system to copy.
     *
     * @see #castOrCopy(CartesianCS)
     */
    protected DefaultCartesianCS(final CartesianCS cs) {
        super(cs);
        ensurePerpendicularAxis();
    }

    /**
     * Returns a SIS coordinate system implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCartesianCS castOrCopy(final CartesianCS object) {
        return (object == null) || (object instanceof DefaultCartesianCS)
                ? (DefaultCartesianCS) object : new DefaultCartesianCS(object);
    }

    /**
     * Ensures that all axes are perpendicular.
     */
    private void ensurePerpendicularAxis() throws IllegalArgumentException {
        final int dimension = getDimension();
        for (int i=0; i<dimension; i++) {
            final AxisDirection axis0 = getAxis(i).getDirection();
            for (int j=i; ++j<dimension;) {
                final AxisDirection axis1 = getAxis(j).getDirection();
                final double angle = CoordinateSystems.angle(axis0, axis1); // May be NaN, which we accept.
                if (Math.abs(Math.abs(angle) - 90) > Formulas.ANGULAR_TOLERANCE) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.NonPerpendicularDirections_2, axis0, axis1));
                }
            }
        }
    }

    /**
     * Compares this coordinate system with the specified object for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        return (object instanceof CartesianCS) && super.equals(object, mode);
    }
}
