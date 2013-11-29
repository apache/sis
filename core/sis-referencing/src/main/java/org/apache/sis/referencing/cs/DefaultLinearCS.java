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
import javax.measure.unit.Unit;
import org.opengis.referencing.cs.LinearCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.measure.Units;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Immutable;


/**
 * A 1-dimensional coordinate system for points that lie on a single axis (not necessarily a straight line).
 * Ordinate values are distances from the axis origin to the point along the axis.
 * A typical example is an axis for representing the points along a road.
 *
 * <table class="sis"><tr>
 *   <th>Used with</th>
 *   <th>Permitted axis names</th>
 * </tr><tr>
 *   <td>{@linkplain org.geotoolkit.referencing.crs.DefaultEngineeringCRS Engineering CRS}</td>
 *   <td>unspecified</td>
 * </tr></table>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
@Immutable
public class DefaultLinearCS extends AbstractCS implements LinearCS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6890723478287625763L;

    /**
     * Constructs a coordinate system from a set of properties.
     * The properties map is given unchanged to the
     * {@linkplain AbstractCS#AbstractCS(Map,CoordinateSystemAxis[]) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties The properties to be given to the identified object.
     * @param axis       The axis.
     */
    public DefaultLinearCS(final Map<String,?> properties, final CoordinateSystemAxis axis) {
        super(properties, axis);
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
     * @see #castOrCopy(LinearCS)
     */
    protected DefaultLinearCS(final LinearCS cs) {
        super(cs);
    }

    /**
     * Returns a SIS coordinate system implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultLinearCS castOrCopy(final LinearCS object) {
        return (object == null) || (object instanceof DefaultLinearCS)
                ? (DefaultLinearCS) object : new DefaultLinearCS(object);
    }

    /**
     * Returns {@code VALID} if the given argument values are allowed for this coordinate system,
     * or an {@code INVALID_*} error code otherwise. This method is invoked at construction time.
     *
     * <p>The current implementation rejects all directions that are known to be non-spatial.
     * We conservatively accept custom axis directions because some of them are created from
     * strings like "South along 90°E".</p>
     */
    @Override
    final int validateAxis(final AxisDirection direction, final Unit<?> unit) {
        if (!AxisDirections.isSpatialOrCustom(direction, false)) {
            return INVALID_DIRECTION;
        }
        if (!Units.isLinear(unit)) {
            return INVALID_UNIT;
        }
        return VALID;
    }

    /**
     * Compares this coordinate system with the specified object for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        return (object instanceof LinearCS) && super.equals(object, mode);
    }
}
