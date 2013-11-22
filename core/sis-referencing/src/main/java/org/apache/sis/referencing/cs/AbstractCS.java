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
import java.util.Arrays;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.xml.bind.annotation.XmlElement;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.util.Utilities.deepEquals;


/**
 * The set of {@linkplain DefaultCoordinateSystemAxis coordinate system axes} that spans a given coordinate space.
 * The type of the coordinate system implies the set of mathematical rules for calculating geometric properties
 * like angles, distances and surfaces.
 *
 * <p>This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Typical applications should create instances of the most specific subclass with {@code Default} prefix instead.
 * An exception to this rule may occurs when it is not possible to identify the exact type. For example it is not
 * possible to infer the exact coordinate system from <cite>Well Known Text</cite> (WKT) version 1 in some cases
 * (e.g. in a {@code LOCAL_CS} element). In such exceptional situation, a plain {@code AbstractCS} object may be
 * instantiated.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 *
 * @see DefaultCoordinateSystemAxis
 * @see org.apache.sis.referencing.crs.AbstractCRS
 */
@Immutable
public class AbstractCS extends AbstractIdentifiedObject implements CoordinateSystem {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6757665252533744744L;

    /**
     * The sequence of axes for this coordinate system.
     */
    @XmlElement(name = "axis")
    private final CoordinateSystemAxis[] axes;

    /**
     * Constructs a coordinate system from a set of properties and a sequence of axes.
     * The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
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
     *     <td>{@link ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties The properties to be given to the identified object.
     * @param axes       The sequence of axes.
     */
    public AbstractCS(final Map<String,?> properties, CoordinateSystemAxis... axes) {
        super(properties);
        ensureNonNull("axes", axes);
        this.axes = axes = axes.clone();
        for (int i=0; i<axes.length; i++) {
            final CoordinateSystemAxis axis = axes[i];
            ensureNonNullElement("axes", i, axis);
            final ReferenceIdentifier name = axis.getName();
            ensureNonNullElement("axes[#].name", i, name);
            final AxisDirection direction = axis.getDirection();
            ensureNonNullElement("axes[#].direction", i, direction);
            /*
             * Ensures that axis direction and units are compatible with the
             * coordinate system to be created. For example CartesianCS will
             * accepts only linear or dimensionless units.
             */
            if (!isCompatibleDirection(direction)) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.IllegalAxisDirection_2, getClass(), direction));
            }
            final Unit<?> unit = axis.getUnit();
            ensureNonNullElement("axes[#].unit", i, unit);
            if (!isCompatibleUnit(direction, unit)) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.IllegalUnitFor_2, name, unit));
            }
            /*
             * Ensures there is no axis along the same direction
             * (e.g. two North axes, or an East and a West axis).
             */
            final AxisDirection dir = AxisDirections.absolute(direction);
            if (!dir.equals(AxisDirection.OTHER)) {
                for (int j=i; --j>=0;) {
                    final AxisDirection other = axes[j].getDirection();
                    if (dir.equals(AxisDirections.absolute(other))) {
                        throw new IllegalArgumentException(Errors.format(
                                Errors.Keys.ColinearAxisDirections_2, direction, other));
                    }
                }
            }
        }
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
     * @see #castOrCopy(CoordinateSystem)
     */
    protected AbstractCS(final CoordinateSystem cs) {
        super(cs);
        if (cs instanceof AbstractCS) {
            axes = ((AbstractCS) cs).axes;
        } else {
            axes = new CoordinateSystemAxis[cs.getDimension()];
            for (int i=0; i<axes.length; i++) {
                axes[i] = cs.getAxis(i);
            }
        }
    }

    /**
     * Returns {@code true} if the specified axis direction is allowed for this coordinate system.
     * This method is invoked at construction time for checking argument validity. The default implementation
     * returns {@code true} for all axis directions. Subclasses will override this method in order to put more
     * restrictions on allowed axis directions.
     *
     * <p><b>Note for implementors:</b> since this method is invoked at construction time, it shall not depends
     * on this object's state. This method is not in public API for that reason.</p>
     *
     * @param  direction The direction to test for compatibility.
     * @return {@code true} if the given direction is compatible with this coordinate system.
     */
    boolean isCompatibleDirection(final AxisDirection direction) {
        return true;
    }

    /**
     * Returns {@code true} is the specified unit is legal for the specified axis direction.
     * This method is invoked at construction time for checking units compatibility. The default implementation
     * returns {@code true} in all cases. Subclasses can override this method and check for compatibility with
     * {@linkplain SI#METRE metre} or {@linkplain NonSI#DEGREE_ANGLE degree} units.
     *
     * <p><b>Note for implementors:</b> since this method is invoked at construction time, it shall not depends
     * on this object's state. This method is not in public API for that reason.</p>
     *
     * @param  direction The direction of the axis having the given unit.
     * @param  unit The unit to test for compatibility.
     * @return {@code true} if the given unit is compatible with this coordinate system.
     */
    boolean isCompatibleUnit(final AxisDirection direction, final Unit<?> unit) {
        return true;
    }

    /**
     * Returns the number of dimensions of this coordinate system.
     * This is the number of axes.
     *
     * @return The number of dimensions of this coordinate system.
     */
    @Override
    public int getDimension() {
        return axes.length;
    }

    /**
     * Returns the axis for this coordinate system at the specified dimension.
     *
     * @param  dimension The zero based index of axis.
     * @return The axis at the specified dimension.
     * @throws IndexOutOfBoundsException if {@code dimension} is out of bounds.
     */
    @Override
    public CoordinateSystemAxis getAxis(final int dimension) throws IndexOutOfBoundsException {
        return axes[dimension];
    }

    /**
     * Compares the specified object with this coordinate system for equality.
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
        if (!(object instanceof CoordinateSystem && super.equals(object, mode))) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                return Arrays.equals(axes, ((AbstractCS) object).axes);
            }
            default: {
                final CoordinateSystem that = (CoordinateSystem) object;
                final int dimension = getDimension();
                if (dimension != that.getDimension()) {
                    return false;
                }
                for (int i=0; i<dimension; i++) {
                    if (!deepEquals(getAxis(i), that.getAxis(i), mode)) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    /**
     * Computes a hash value consistent with the given comparison mode.
     *
     * @return The hash code value for the given comparison mode.
     */
    @Override
    public int hashCode(final ComparisonMode mode) throws IllegalArgumentException {
        return Arrays.hashCode(axes) + 31*super.hashCode(mode);
    }

    /**
     * Formats the inner part of a <cite>Well Known Text</cite> (WKT) element.
     * Note that WKT version 1 does not define any keyword for coordinate system.
     *
     * @param  formatter The formatter to use.
     * @return The WKT element name.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        for (final CoordinateSystemAxis axe : axes) {
            formatter.append(axe);
        }
        return super.formatTo(formatter);
    }
}
