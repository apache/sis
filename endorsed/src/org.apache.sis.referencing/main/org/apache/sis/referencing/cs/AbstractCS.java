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
import java.util.ConcurrentModificationException;
import java.util.logging.Logger;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import javax.measure.Unit;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CSAuthorityFactory;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.system.Modules;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.io.wkt.ElementKind;
import org.apache.sis.io.wkt.Formatter;
import static org.apache.sis.util.ArgumentChecks.*;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.geometry.MismatchedDimensionException;


/**
 * The set of {@linkplain DefaultCoordinateSystemAxis coordinate system axes} that spans a given coordinate space.
 * The type of the coordinate system implies the set of mathematical rules for calculating geometric properties
 * like angles, distances and surfaces.
 *
 * <p>This class is conceptually <i>abstract</i>, even if it is technically possible to instantiate it.
 * Typical applications should create instances of the most specific subclass with {@code Default} prefix instead.
 * An exception to this rule may occurs when it is not possible to identify the exact type. For example, it is not
 * possible to infer the exact coordinate system from <i>Well Known Text</i> (WKT) version 1 in some cases
 * (e.g. in a {@code LOCAL_CS} element). In such exceptional situation, a plain {@code AbstractCS} object may be
 * instantiated.</p>
 *
 * <h2>Immutability and thread safety</h2>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * and the {@link CoordinateSystemAxis} instances given to the constructor are also immutable. Most SIS subclasses and
 * related classes are immutable under similar conditions. This means that unless otherwise noted in the javadoc,
 * {@code CoordinateSystem} instances created using only SIS factories and static constants can be shared by many
 * objects and passed between threads without synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see DefaultCoordinateSystemAxis
 * @see org.apache.sis.referencing.crs.AbstractCRS
 *
 * @since 0.4
 */
@XmlType(name = "AbstractCoordinateSystemType")
@XmlRootElement(name = "AbstractCoordinateSystem")
@XmlSeeAlso({
    DefaultAffineCS.class,
    DefaultCartesianCS.class,               // Not an AffineCS subclass in GML schema.
    DefaultSphericalCS.class,
    DefaultEllipsoidalCS.class,
    DefaultCylindricalCS.class,
    DefaultPolarCS.class,
    DefaultLinearCS.class,
    DefaultVerticalCS.class,
    DefaultTimeCS.class,
    DefaultParametricCS.class,
    DefaultUserDefinedCS.class
})
public class AbstractCS extends AbstractIdentifiedObject implements CoordinateSystem {
    /**
     * The logger for referencing operations.
     */
    static final Logger LOGGER = Logger.getLogger(Modules.REFERENCING);

    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3394376886951478970L;

    /**
     * Return value for {@link #validateAxis(AxisDirection, Unit)}
     */
    static final int VALID = 0, INVALID_DIRECTION = 1, INVALID_UNIT = 2;

    /**
     * The sequence of axes for this coordinate system.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setAxis(CoordinateSystemAxis[])}</p>
     *
     * @see #getAxis(int)
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private CoordinateSystemAxis[] axes;

    /**
     * Other coordinate systems derived from this coordinate systems for other axes conventions.
     * This map is shared by all instances derived from the same original {@code AbstractCRS} instance.
     * It is serialized in order to preserve metadata about the original instance.
     * All accesses to this map shall be synchronized on {@code forConvention}.
     *
     * @see #forConvention(AxesConvention)
     */
    final EnumMap<AxesConvention,AbstractCS> forConvention;

    /**
     * Creates the value to assign to the {@link #forConvention} map by constructors.
     *
     * @param  original  the coordinate system to declare as the original one.
     * @return map to assign to the {@link #forConvention} field.
     */
    private static EnumMap<AxesConvention,AbstractCS> forConvention(final AbstractCS original) {
        var m = new EnumMap<AxesConvention,AbstractCS>(AxesConvention.class);
        m.put(AxesConvention.ORIGINAL, original);
        return m;
    }

    /**
     * Constructs a coordinate system from a set of properties and a sequence of axes.
     * The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties  the properties to be given to the identified object.
     * @param  axes        the sequence of axes.
     * @throws IllegalArgumentException if an axis has an illegal direction or an illegal unit of measurement.
     */
    @SuppressWarnings("this-escape")
    public AbstractCS(final Map<String,?> properties, final CoordinateSystemAxis... axes) {
        super(properties);
        this.axes = axes.clone();
        validate(properties);
        forConvention = forConvention(this);
    }

    /**
     * Verifies that the coordinate system axes are non-null, then validates their directions and units of measurement.
     * Subclasses may override for adding more verifications, for example ensuring that all axes are perpendicular.
     *
     * @param  properties  properties given at construction time, or {@code null} if none.
     * @throws IllegalArgumentException if an axis has an illegal direction or an illegal unit of measurement.
     */
    void validate(final Map<String,?> properties) {
        for (int i=0; i<axes.length; i++) {
            final CoordinateSystemAxis axis = axes[i];
            ensureNonNullElement("axes", i, axis);
            final Identifier name = axis.getName();
            ensureNonNullElement("axes[#].name", i, name);
            final AxisDirection direction = axis.getDirection();
            ensureNonNullElement("axes[#].direction", i, direction);
            final Unit<?> unit = axis.getUnit();
            ensureNonNullElement("axes[#].unit", i, unit);
            /*
             * Ensures that axis direction and units are compatible with the
             * coordinate system to be created. For example, CartesianCS will
             * accept only linear or dimensionless units.
             */
            switch (validateAxis(direction, unit)) {
                case INVALID_DIRECTION: {
                    throw new IllegalArgumentException(Resources.forProperties(properties).getString(
                            Resources.Keys.IllegalAxisDirection_2, getClass(), direction));
                }
                case INVALID_UNIT: {
                    throw new IllegalArgumentException(Resources.forProperties(properties).getString(
                            Resources.Keys.IllegalUnitFor_2, name, unit));
                }
            }
            /*
             * Ensures there are no axes along the same direction (e.g. two North axes, or an East and a West axis).
             * An exception to this rule is the time axis, since ISO 19107 explicitly allows compound CRS to have
             * more than one time axis. Such case happen in meteorological models.
             */
            final AxisDirection dir = AxisDirections.absolute(direction);
            if (dir != AxisDirections.UNSPECIFIED && dir != AxisDirection.OTHER) {
                for (int j=i; --j>=0;) {
                    final AxisDirection other = axes[j].getDirection();
                    final AxisDirection abs = AxisDirections.absolute(other);
                    if (dir == abs && abs != AxisDirection.FUTURE) {
                        throw new IllegalArgumentException(Resources.forProperties(properties).getString(
                                Resources.Keys.ColinearAxisDirections_2, direction, other));
                    }
                }
            }
        }
    }

    /**
     * Returns {@link #VALID} if the given argument values are allowed for an axis in this coordinate system,
     * or an {@code INVALID_*} error code otherwise. This method is invoked at construction time for checking
     * argument validity. The default implementation returns {@code VALID} in all cases. Subclasses override
     * this method in order to put more restrictions on allowed axis directions and check for compatibility
     * with {@linkplain org.apache.sis.measure.Units#METRE metre} or
     * {@linkplain org.apache.sis.measure.Units#DEGREE degree} units.
     *
     * <p><b>Note for implementers:</b> since this method is invoked at construction time, it shall not depend
     * on this object's state. This method is not in public API for that reason.</p>
     *
     * @param  direction  the direction to test for compatibility (never {@code null}).
     * @param  unit       the unit to test for compatibility (never {@code null}).
     * @return {@link #VALID} if the given direction and unit are compatible with this coordinate system,
     *         {@link #INVALID_DIRECTION} if the direction is invalid or {@link #INVALID_UNIT} if the unit
     *         is invalid.
     */
    int validateAxis(final AxisDirection direction, final Unit<?> unit) {
        return VALID;
    }

    /**
     * Creates a new CS derived from the specified one, but with different axis order or unit.
     *
     * @param  original  the original coordinate system from which to derive a new one.
     * @param  name      name of the new coordinate system, or {@code null} to inherit.
     * @param  axes      the new axes. This array is not cloned.
     * @throws IllegalArgumentException if an axis has illegal unit or direction.
     *
     * @see #createForAxes(String, CoordinateSystemAxis[])
     */
    AbstractCS(final AbstractCS original, final String name, final CoordinateSystemAxis[] axes) {
        super(original.getPropertiesWithoutIdentifiers(name));
        this.axes = axes;
        validate(null);
        forConvention = hasSameAxes(original) ? original.forConvention : forConvention(original);
    }

    /**
     * Creates a new coordinate system with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  original  the coordinate system to copy.
     *
     * @see #castOrCopy(CoordinateSystem)
     */
    @SuppressWarnings("this-escape")
    protected AbstractCS(final CoordinateSystem original) {
        super(original);
        axes = (original instanceof AbstractCS) ? ((AbstractCS) original).axes : getAxes(original);
        validate(null);
        forConvention = forConvention(this);
    }

    /**
     * Returns the axes of the given coordinate system.
     */
    private static CoordinateSystemAxis[] getAxes(final CoordinateSystem cs) {
        final CoordinateSystemAxis[] axes = new CoordinateSystemAxis[cs.getDimension()];
        for (int i=0; i<axes.length; i++) {
            axes[i] = cs.getAxis(i);
        }
        return axes;
    }

    /**
     * Returns a SIS coordinate system implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of
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
     *       {@linkplain #AbstractCS(CoordinateSystem) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractCS castOrCopy(final CoordinateSystem object) {
        return SubTypes.castOrCopy(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code CoordinateSystem.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return the coordinate system interface implemented by this class.
     */
    @Override
    public Class<? extends CoordinateSystem> getInterface() {
        return CoordinateSystem.class;
    }

    /**
     * Returns the properties (scope, domain of validity) except the identifiers and the EPSG namespace.
     *
     * @param  name  name to associate to the {@link #NAME_KEY} in the returned map, or {@code null} to inherit.
     * @return the identified object properties without identifier.
     */
    final Map<String,?> getPropertiesWithoutIdentifiers(final String name) {
        return ReferencingUtilities.getPropertiesWithoutIdentifiers(this, (name == null) ? null : Map.of(NAME_KEY, name));
    }

    /**
     * Returns the number of dimensions of this coordinate system.
     * This is the number of axes given at construction time.
     *
     * @return the number of dimensions of this coordinate system.
     */
    @Override
    public final int getDimension() {
        return axes.length;
    }

    /**
     * Returns the axis for this coordinate system at the specified dimension.
     *
     * @param  dimension  the zero based index of axis.
     * @return the axis at the specified dimension.
     * @throws IndexOutOfBoundsException if {@code dimension} is out of bounds.
     */
    @Override
    public final CoordinateSystemAxis getAxis(final int dimension) throws IndexOutOfBoundsException {
        return axes[dimension];
    }

    /**
     * {@return whether this coordinate system has the same axes as the specified CS, ignoring axis order}.
     * If true, then the two coordinate systems have the same number of dimensions and the same set of axes.
     * Axis instances are compared by the identity operator ({@code ==}), not by {@code equals(Object)}.
     *
     * <h4>Purpose</h4>
     * This method can be invoked after a call to {@link #forConvention(AxesConvention)} for checking if that
     * method changed only the axis order, with no change to axis directions, axis ranges or units of measurement.
     *
     * @param  other  the other coordinate system to compare with this CS.
     *
     * @since 1.5
     */
    public final boolean hasSameAxes(final CoordinateSystem other) {
        if (other.getDimension() != axes.length) {
            return false;
        }
        /*
         * Need an unconditional copy of the axes of the other coordinate system, because the array will be modified.
         * The implementation is okay for small arrays, but would be inefficient if the number of axes was very large.
         */
        final CoordinateSystemAxis[] copy = getAxes(other);
        int n = copy.length;
next:   for (final CoordinateSystemAxis axis : axes) {
            for (int i=0; i<n; i++) {
                if (axis == copy[i]) {
                    System.arraycopy(copy, i+1, copy, i, --n - i);
                    continue next;
                }
            }
            return false;
        }
        return n == 0;
    }

    /**
     * Sets the CS for the given axes convention.
     *
     * @param  cs  the CS to cache.
     * @return the cached CS. May be different than the given {@code cs} if an existing instance has been found.
     */
    final AbstractCS setCached(final AxesConvention convention, AbstractCS cs) {
        assert Thread.holdsLock(forConvention);
        /*
         * It happens often that the CRS created by RIGHT_HANDED, DISPLAY_ORIENTED and NORMALIZED are the same.
         * Sharing the same instance not only saves memory, but can also makes future comparisons faster.
         */
        for (final AbstractCS existing : forConvention.values()) {
            if (cs.equals(existing, ComparisonMode.IGNORE_METADATA)) {
                cs = existing;
                break;
            }
        }
        if (forConvention.put(convention, cs) != null) {
            throw new ConcurrentModificationException();    // Should never happen, unless we have a synchronization bug.
        }
        return cs;
    }

    /**
     * Returns a coordinate system equivalent to this one but with axes rearranged according the given convention.
     * If this coordinate system is already compatible with the given convention, then this method returns {@code this}.
     *
     * @param  convention  the axes convention for which a coordinate system is desired.
     * @return a coordinate system compatible with the given convention (may be {@code this}).
     *
     * @see org.apache.sis.referencing.crs.AbstractCRS#forConvention(AxesConvention)
     */
    public AbstractCS forConvention(final AxesConvention convention) {
        synchronized (forConvention) {
            AbstractCS cs = forConvention.get(convention);
            if (cs == null) {
                cs = Normalizer.forConvention(this, convention);
                if (cs == null) {
                    cs = this;          // This coordinate system is already normalized.
                } else if (convention != AxesConvention.POSITIVE_RANGE) {
                    cs = cs.resolveEPSG(this);
                }
                cs = setCached(convention, cs);
            }
            return cs;
        }
    }

    /**
     * Returns a coordinate system usually of the same type as this CS but with different axes.
     * This method shall be overridden by all {@code AbstractCS} subclasses in this package.
     *
     * <p>This method returns a coordinate system of the same type if the number of axes is unchanged.
     * But if the given {@code axes} array has less elements than this coordinate system dimension, then
     * this method may return another kind of coordinate system. See {@link AxisFilter} for an example.</p>
     *
     * @param  name   name of the new coordinate system.
     * @param  axes   the set of axes to give to the new coordinate system.
     * @return a new coordinate system of the same type as {@code this}, but using the given axes.
     * @throws IllegalArgumentException if {@code axes} contains an unexpected number of axes,
     *         or if an axis has an unexpected direction or unexpected unit of measurement.
     */
    AbstractCS createForAxes(final String name, final CoordinateSystemAxis[] axes) {
        return new AbstractCS(this, name, axes);
    }

    /**
     * Verify if we can get a coordinate system from the EPSG database with the same axes.
     * Such CS gives more information (better name and remarks). This is a "would be nice"
     * feature; if we fail, we keep the CS built by {@link Normalizer}.
     *
     * @param  original  the coordinate system from which this CS is derived.
     * @return the resolved CS, or {@code this} if none.
     */
    private AbstractCS resolveEPSG(final AbstractCS original) {
        if (IdentifiedObjects.getIdentifier(original, Citations.EPSG) != null) {
            final Integer epsg = CoordinateSystems.getEpsgCode(getInterface(), axes);
            if (epsg != null) try {
                final AuthorityFactory factory = CRS.getAuthorityFactory(Constants.EPSG);
                if (factory instanceof CSAuthorityFactory) {
                    final CoordinateSystem fromDB = ((CSAuthorityFactory) factory).createCoordinateSystem(epsg.toString());
                    if (fromDB instanceof AbstractCS) {
                        /*
                         * We should compare axes strictly using Arrays.equals(…). However, axes in different order
                         * get different codes in EPSG database, which may them not strictly equal. We would need
                         * another comparison mode ignoring only the authority code. We don't add this complexity
                         * for now, and rather rely on the check for EPSG code done by the caller. If the original
                         * CS was an EPSG object, then we assume that we still want an EPSG object here.
                         */
                        if (Utilities.equalsIgnoreMetadata(axes, ((AbstractCS) fromDB).axes)) {
                            return (AbstractCS) fromDB;
                        }
                    }
                }
            } catch (FactoryException e) {
                /*
                 * NoSuchAuthorityCodeException may happen if factory is EPSGFactoryFallback.
                 * Other exceptions would probably be more serious errors, but it still non-fatal
                 * for this method since we can continue with what Normalizer created.
                 */
                Logging.recoverableException(LOGGER, getClass(), "forConvention", e);
            }
        }
        return this;
    }

    /**
     * Convenience method for implementations of {@code createForAxes(…)}
     * when the resulting coordinate system would have an unexpected number of dimensions.
     *
     * @param  axes  the axes which were supposed to be given to the constructor.
     * @param  min   minimum number of dimensions, inclusive.
     * @param  max   maximum number of dimensions, inclusive.
     *
     * @see #createForAxes(String, CoordinateSystemAxis[])
     */
    static IllegalArgumentException unexpectedDimension(final CoordinateSystemAxis[] axes, final int min, final int max) {
        final int n = axes.length;
        final int e = (n < min) ? min : max;
        return new MismatchedDimensionException(Errors.format(Errors.Keys.MismatchedDimension_3, "filter(cs)", e, n));
    }

    /**
     * Compares the specified object with this coordinate system for equality.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *                 {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only
     *                 properties relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     *
     * @hidden because nothing new to said.
     */
    @Override
    @SuppressWarnings({"AssertWithSideEffects", "fallthrough"})
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;                                            // Slight optimization.
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                // No need to check the class - this check has been done by super.equals(…).
                return Arrays.equals(axes, ((AbstractCS) object).axes);
            }
            case DEBUG: {
                final int d1, d2;
                assert (d1 = axes.length) == (d2 = ((CoordinateSystem) object).getDimension())
                        : Errors.format(Errors.Keys.MismatchedDimension_2, d1, d2);
                // Fall through
            }
            default: {
                final CoordinateSystem that = (CoordinateSystem) object;
                final int dimension = getDimension();
                if (dimension != that.getDimension()) {
                    return false;
                }
                if (!mode.allowsVariant()) {
                    for (int i=0; i<dimension; i++) {
                        if (!Utilities.deepEquals(getAxis(i), that.getAxis(i), mode)) {
                            return false;
                        }
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
     * @return the hash code value. This value may change in any future Apache SIS version.
     *
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Arrays.hashCode(axes);
    }

    /**
     * Formats the inner part of the <i>Well Known Text</i> (WKT) representation of this coordinate system.
     * This method does <strong>not</strong> format the axes, because they shall appear outside
     * the {@code CS[…]} element for historical reasons. Axes shall be formatted by the enclosing
     * element (usually an {@link org.apache.sis.referencing.crs.AbstractCRS}).
     *
     * <h4>Example</h4>
     * Well-Known Text of a two-dimensional {@code EllipsoidalCS}
     * having (φ,λ) axes in a unit defined by the enclosing CRS (usually degrees).
     *
     * {@snippet lang="wkt" :
     *   CS[ellipsoidal, 2],
     *   Axis["latitude", north],
     *   Axis["longitude", east]
     * }
     *
     * <h4>Compatibility note</h4>
     * {@code CS} is defined in the WKT 2 specification only.
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "CS"}.
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#36">WKT 2 specification §7.5</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final String type = WKTUtilities.toType(CoordinateSystem.class, getInterface());
        if (type == null) {
            formatter.setInvalidWKT(this, null);
        }
        formatter.append(type, ElementKind.CODE_LIST);
        formatter.append(getDimension());
        return WKTKeywords.CS;
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * An empty array of axes, used only for JAXB.
     */
    private static final CoordinateSystemAxis[] EMPTY = new CoordinateSystemAxis[0];

    /**
     * Constructs a new object in which every attributes are set to a null or empty value.
     * <strong>This is not a valid object.</strong> This constructor is strictly reserved
     * to JAXB, which will assign values to the fields using reflection.
     */
    @SuppressWarnings("this-escape")
    AbstractCS() {
        super(org.apache.sis.referencing.privy.NilReferencingObject.INSTANCE);
        forConvention = forConvention(this);
        axes = EMPTY;
        /*
         * Coordinate system axes are mandatory for SIS working. We do not verify their presence here
         * (because the verification would have to be done in an 'afterMarshal(…)' method and throwing
         * an exception in that method causes the whole unmarshalling to fail). But the CS_CoordinateSystem
         * adapter does some verifications.
         */
    }

    /**
     * Invoked by JAXB at marshalling time.
     */
    @XmlElement(name = "axis")
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private CoordinateSystemAxis[] getAxis() {
        return axes;
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    private void setAxis(final CoordinateSystemAxis[] values) {
        axes = values;
    }
}
