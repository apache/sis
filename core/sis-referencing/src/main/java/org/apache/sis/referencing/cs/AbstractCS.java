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
import java.util.EnumMap;
import java.util.Arrays;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.io.wkt.Formatter;
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
 * {@section Immutability and thread safety}
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * and the {@link CoordinateSystemAxis} instances given to the constructor are also immutable. Most SIS subclasses and
 * related classes are immutable under similar conditions. This means that unless otherwise noted in the javadoc,
 * {@code CoordinateSystem} instances created using only SIS factories and static constants can be shared by many
 * objects and passed between threads without synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 *
 * @see DefaultCoordinateSystemAxis
 * @see org.apache.sis.referencing.crs.AbstractCRS
 */
@XmlType(name = "AbstractCoordinateSystemType")
@XmlRootElement(name = "AbstractCoordinateSystem")
@XmlSeeAlso({
    DefaultAffineCS.class,
    DefaultCartesianCS.class, // Not an AffineCS subclass in GML schema.
    DefaultSphericalCS.class,
    DefaultEllipsoidalCS.class,
    DefaultCylindricalCS.class,
    DefaultPolarCS.class,
    DefaultLinearCS.class,
    DefaultVerticalCS.class,
    DefaultTimeCS.class,
    DefaultUserDefinedCS.class
})
public class AbstractCS extends AbstractIdentifiedObject implements CoordinateSystem {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6757665252533744744L;

    /**
     * Return value for {@link #validateAxis(AxisDirection, Unit)}
     */
    static final int VALID = 0, INVALID_DIRECTION = 1, INVALID_UNIT = 2;

    /**
     * An empty array of axes, used only for JAXB.
     */
    private static final CoordinateSystemAxis[] EMPTY = new CoordinateSystemAxis[0];

    /**
     * The sequence of axes for this coordinate system.
     */
    @XmlElement(name = "axis")
    private final CoordinateSystemAxis[] axes;

    /**
     * Other coordinate systems derived from this coordinate systems for other axes conventions.
     * Created only when first needed.
     *
     * @see #forConvention(AxesConvention)
     */
    private transient Map<AxesConvention,AbstractCS> derived;

    /**
     * Constructs a new object in which every attributes are set to a null or empty value.
     * <strong>This is not a valid object.</strong> This constructor is strictly reserved
     * to JAXB, which will assign values to the fields using reflexion.
     */
    AbstractCS() {
        super(org.apache.sis.internal.referencing.NilReferencingObject.INSTANCE);
        axes = EMPTY;
    }

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
            final Unit<?> unit = axis.getUnit();
            ensureNonNullElement("axes[#].unit", i, unit);
            /*
             * Ensures that axis direction and units are compatible with the
             * coordinate system to be created. For example CartesianCS will
             * accept only linear or dimensionless units.
             */
            switch (validateAxis(direction, unit)) {
                case INVALID_DIRECTION: {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.IllegalAxisDirection_2, getClass(), direction));
                }
                case INVALID_UNIT: {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.IllegalUnitFor_2, name, unit));
                }
            }
            /*
             * Ensures there is no axis along the same direction (e.g. two North axes, or an East and a West axis).
             * An exception to this rule is the time axis, since ISO 19107 explicitely allows compound CRS to have
             * more than one time axis. Such case happen in meteorological models.
             */
            final AxisDirection dir = AxisDirections.absolute(direction);
            if (!dir.equals(AxisDirection.OTHER)) {
                for (int j=i; --j>=0;) {
                    final AxisDirection other = axes[j].getDirection();
                    final AxisDirection abs = AxisDirections.absolute(other);
                    if (dir.equals(abs) && !abs.equals(AxisDirection.FUTURE)) {
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
            axes = ((AbstractCS) cs).axes; // Share the array.
        } else {
            axes = new CoordinateSystemAxis[cs.getDimension()];
            for (int i=0; i<axes.length; i++) {
                axes[i] = cs.getAxis(i);
            }
        }
    }

    /**
     * Returns a SIS coordinate system implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is is an instance of
     *       {@link org.opengis.referencing.cs.AffineCS},
     *       {@link org.opengis.referencing.cs.CartesianCS},
     *       {@link org.opengis.referencing.cs.SphericalCS},
     *       {@link org.opengis.referencing.cs.EllipsoidalCS},
     *       {@link org.opengis.referencing.cs.CylindricalCS},
     *       {@link org.opengis.referencing.cs.PolarCS},
     *       {@link org.opengis.referencing.cs.LinearCS},
     *       {@link org.opengis.referencing.cs.VerticalCS},
     *       {@link org.opengis.referencing.cs.TimeCS} or
     *       {@link org.opengis.referencing.cs.UserDefinedCS},
     *       then this method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractCS}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractCS} instance is created using the
     *       {@linkplain #AbstractCS(CoordinateSystem) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractCS castOrCopy(final CoordinateSystem object) {
        return SubTypes.castOrCopy(object);
    }

    /**
     * Returns {@link #VALID} if the given argument values are allowed for an axis in this coordinate system,
     * or an {@code INVALID_*} error code otherwise. This method is invoked at construction time for checking
     * argument validity. The default implementation returns {@code VALID} in all cases. Subclasses override
     * this method in order to put more restrictions on allowed axis directions and check for compatibility
     * with {@linkplain SI#METRE metre} or {@linkplain NonSI#DEGREE_ANGLE degree} units.
     *
     * <p><b>Note for implementors:</b> since this method is invoked at construction time, it shall not depend
     * on this object's state. This method is not in public API for that reason.</p>
     *
     * @param  direction The direction to test for compatibility.
     * @param  unit The unit to test for compatibility.
     * @return {@link #VALID} if the given direction and unit are compatible with this coordinate system,
     *         {@link #DIRECTION} if the direction is invalid or {@link #UNIT} if the unit is invalid.
     */
    int validateAxis(final AxisDirection direction, final Unit<?> unit) {
        return VALID;
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code CoordinateSystem.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return The coordinate system interface implemented by this class.
     */
    @Override
    public Class<? extends CoordinateSystem> getInterface() {
        return CoordinateSystem.class;
    }

    /**
     * Returns the number of dimensions of this coordinate system.
     * This is the number of axes given at construction time.
     *
     * @return The number of dimensions of this coordinate system.
     */
    @Override
    public final int getDimension() {
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
    public final CoordinateSystemAxis getAxis(final int dimension) throws IndexOutOfBoundsException {
        return axes[dimension];
    }

    /**
     * Returns a coordinate system equivalent to this one but with axes rearranged according the given convention.
     * If this coordinate system is already compatible with the given convention, then this method returns
     * {@code this}.
     *
     * @param  convention The axes convention for which a coordinate system is desired.
     * @return A coordinate system compatible with the given convention (may be {@code this}).
     *
     * @see org.apache.sis.referencing.crs.AbstractCRS#forConvention(AxesConvention)
     */
    public synchronized AbstractCS forConvention(final AxesConvention convention) {
        ensureNonNull("convention", convention);
        if (derived == null) {
            derived = new EnumMap<AxesConvention,AbstractCS>(AxesConvention.class);
        }
        AbstractCS cs = derived.get(convention);
        if (cs == null) {
            switch (convention) {
                case NORMALIZED:     cs = Normalizer.normalize(this, true);  break;
                case RIGHT_HANDED:   cs = Normalizer.normalize(this, false); break;
                case POSITIVE_RANGE: cs = Normalizer.shiftAxisRange(this);   break;
                default: throw new AssertionError(convention);
            }
            for (final AbstractCS existing : derived.values()) {
                if (cs.equals(existing)) {
                    cs = existing;
                    break;
                }
            }
            derived.put(convention, cs);
        }
        return cs;
    }

    /**
     * Returns a coordinate system of the same type than this CS but with different axes.
     * This method shall be overridden by all {@code AbstractCS} subclasses in this package.
     */
    AbstractCS createSameType(final Map<String,?> properties, final CoordinateSystemAxis[] axes) {
        return new AbstractCS(properties, axes);
    }

    /**
     * Compares the specified object with this coordinate system for equality.
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
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                // No need to check the class - this check has been done by super.equals(…).
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
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     * See {@link org.apache.sis.referencing.AbstractIdentifiedObject#computeHashCode()}
     * for more information.
     *
     * @return The hash code value. This value may change in any future Apache SIS version.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Arrays.hashCode(axes);
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
