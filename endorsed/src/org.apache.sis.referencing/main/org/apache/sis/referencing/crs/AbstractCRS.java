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
package org.apache.sis.referencing.crs;

import java.util.Map;
import java.util.EnumMap;
import java.util.Objects;
import java.util.function.Function;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import javax.measure.Unit;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.cs.AffineCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.datum.AbstractDatum;
import org.apache.sis.referencing.datum.DefaultDatumEnsemble;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.metadata.privy.ImplementationHelper;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.opengis.geometry.MismatchedDimensionException;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.pending.geoapi.referencing.MissingMethods;


/**
 * Coordinate reference system, defined by a {@linkplain AbstractCS coordinate system}
 * and (usually) a {@linkplain org.apache.sis.referencing.datum.AbstractDatum datum}.
 * A coordinate reference system (CRS) consists of an ordered sequence of
 * {@linkplain org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis coordinate system axes}
 * that are related to the earth through the datum.
 * Most coordinate reference system do not move relative to the earth, except for
 * {@linkplain DefaultEngineeringCRS engineering coordinate reference systems}
 * defined on moving platforms such as cars, ships, aircraft, and spacecraft.
 *
 * <p>Coordinate reference systems can have an arbitrary number of dimensions.
 * The actual dimension of a given instance can be determined as below:</p>
 *
 * {@snippet lang="java" :
 *     int dimension = crs.getCoordinateSystem().getDimension();
 *     }
 *
 * However, most subclasses restrict the allowed number of dimensions.
 *
 * <h2>Instantiation</h2>
 * This class is conceptually <i>abstract</i>, even if it is technically possible to instantiate it.
 * Typical applications should create instances of the most specific subclass prefixed by {@code Default} instead.
 * An exception to this rule may occur when it is not possible to identify the exact CRS type.
 *
 * <h2>Immutability and thread safety</h2>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code CoordinateReferenceSystem} instances
 * created using only SIS factories and static constants can be shared by many objects and passed between threads
 * without synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see AbstractCS
 * @see org.apache.sis.referencing.datum.AbstractDatum
 *
 * @since 0.4
 */
@XmlType(name = "AbstractCRSType")
@XmlRootElement(name = "AbstractCRS")
@XmlSeeAlso({
    AbstractSingleCRS.class,
    DefaultCompoundCRS.class
})
public class AbstractCRS extends AbstractReferenceSystem implements CoordinateReferenceSystem {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4925108294894867598L;

    /**
     * The coordinate system.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setCoordinateSystem(String, CoordinateSystem)}.</p>
     *
     * @see #getCoordinateSystem()
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private CoordinateSystem coordinateSystem;

    /**
     * Other coordinate reference systems computed from this CRS by the application of an axes convention.
     * This map is shared by all instances derived from the same original {@code AbstractCRS} instance.
     * It is serialized in order to preserve metadata about the original instance.
     * All accesses to this map shall be synchronized on {@code forConvention}.
     *
     * @see #forConvention(AxesConvention)
     */
    private final EnumMap<AxesConvention,AbstractCRS> forConvention;

    /**
     * Creates the value to assign to the {@link #forConvention} map by constructors.
     * {@code this} instance will be the <abbr>CRS</abbr> to declare as the original one.
     *
     * @return map to assign to the {@link #forConvention} field.
     */
    private EnumMap<AxesConvention,AbstractCRS> forConvention() {
        var m = new EnumMap<AxesConvention,AbstractCRS>(AxesConvention.class);
        m.put(AxesConvention.ORIGINAL, this);
        return m;
    }

    /**
     * Creates a coordinate reference system from the given properties and coordinate system.
     * The properties given in argument follow the same rules as for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
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
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ReferenceIdentifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>"domains"</td>
     *     <td>{@link org.apache.sis.referencing.DefaultObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties  the properties to be given to the coordinate reference system.
     * @param cs          the coordinate system.
     */
    @SuppressWarnings("this-escape")
    public AbstractCRS(final Map<String,?> properties, final CoordinateSystem cs) {
        super(properties);
        coordinateSystem = Objects.requireNonNull(cs);
        forConvention = forConvention();
    }

    /**
     * Verifies that the given coordinate system has a number of dimensions in the expected range.
     *
     * @param min  minimum number of dimensions, inclusive.
     * @param max  maximum number of dimensions, inclusive.
     * @param cs   the coordinate system for which to validate the number of dimensions.
     * @throws MismatchedDimensionException if the actual number of dimension is out of bounds.
     */
    static void checkDimension(final int min, final int max, final CoordinateSystem cs) {
        final int actual = cs.getDimension();
        final int expected;
        if (actual < min) {
            expected = min;
        } else if (actual > max) {
            expected = max;
        } else {
            return;
        }
        throw new MismatchedDimensionException(Errors.format(
                Errors.Keys.MismatchedDimension_3, "cs", expected, actual));
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     *
     * @param original  the original CRS from which to derive a new one.
     * @param id        new identifier for this CRS, or {@code null} if none.
     * @param cs        coordinate system with new axis order or units of measurement.
     *
     * @see #createSameType(AbstractCS)
     */
    AbstractCRS(final AbstractCRS original, final ReferenceIdentifier id, final AbstractCS cs) {
        super(ReferencingUtilities.getPropertiesWithoutIdentifiers(original, (id == null) ? null : Map.of(IDENTIFIERS_KEY, id)));
        coordinateSystem = cs;
        forConvention = cs.hasSameAxes(original.coordinateSystem) ? original.forConvention : original.forConvention();
    }

    /**
     * Constructs a new coordinate reference system with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  crs  the coordinate reference system to copy.
     *
     * @see #castOrCopy(CoordinateReferenceSystem)
     */
    @SuppressWarnings("this-escape")
    protected AbstractCRS(final CoordinateReferenceSystem crs) {
        super(crs);
        coordinateSystem = crs.getCoordinateSystem();
        if (coordinateSystem == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MissingValueForProperty_1, "coordinateSystem"));
        }
        forConvention = forConvention();
    }

    /**
     * Returns a SIS coordinate reference system implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of
     *       {@link org.opengis.referencing.crs.GeodeticCRS} (including the
     *       {@link org.opengis.referencing.crs.GeographicCRS} and
     *       {@link org.opengis.referencing.crs.GeocentricCRS} subtypes),
     *       {@link org.opengis.referencing.crs.VerticalCRS},
     *       {@link org.opengis.referencing.crs.TemporalCRS},
     *       {@link org.opengis.referencing.crs.EngineeringCRS},
     *       {@link org.opengis.referencing.crs.ImageCRS} or
     *       {@link org.apache.sis.referencing.cs.DefaultCompoundCS},
     *       then this method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractCRS}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractCRS} instance is created using the
     *       {@linkplain #AbstractCRS(CoordinateReferenceSystem) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractCRS castOrCopy(final CoordinateReferenceSystem object) {
        return SubTypes.castOrCopy(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The default implementation returns {@code CoordinateReferenceSystem.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return the coordinate reference system interface implemented by this class.
     */
    @Override
    public Class<? extends CoordinateReferenceSystem> getInterface() {
        return CoordinateReferenceSystem.class;
    }

    /**
     * Returns the datum ensemble of the given <abbr>CRS</abbr>.
     *
     * @param  crs  the <abbr>CRS</abbr> from which to get the datum ensemble.
     * @return the datum ensemble, or {@code null} if none.
     */
    static DefaultDatumEnsemble<?> getDatumEnsemble(final CoordinateReferenceSystem crs) {
        return (crs instanceof AbstractCRS) ? ((AbstractCRS) crs).getDatumEnsemble() : null;
    }

    /**
     * Returns the datum ensemble.
     *
     * @return the datum ensemble, or {@code null} if none.
     */
    DefaultDatumEnsemble<?> getDatumEnsemble() {
        return null;
    }

    /**
     * Initializes the handler for getting datum ensemble of an arbitrary CRS.
     */
    static {
        MissingMethods.datumEnsemble = AbstractCRS::getDatumEnsemble;
    }

    /**
     * Returns the coordinate system.
     *
     * @return the coordinate system.
     */
    @Override
    public CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Returns the coordinate system if it is of the given type, or {@code null} otherwise.
     * This method is invoked by subclasses that can accept more than one CS type.
     */
    @SuppressWarnings("unchecked")
    final <T extends CoordinateSystem> T getCoordinateSystem(final Class<T> type) {
        final CoordinateSystem cs = coordinateSystem;
        if (type.isInstance(cs)) {
            // Special case for AffineCS: must ensure that the cs is not the CartesianCS subtype.
            if (type != AffineCS.class || !(cs instanceof CartesianCS)) {
                return (T) cs;
            }
        }
        return null;
    }

    /**
     * Returns the cached <abbr>CRS</abbr> for the given axes convention.
     *
     * @return the cached <abbr>CRS</abbr>, or {@code null} if none.
     */
    final AbstractCRS getCached(final AxesConvention convention) {
        synchronized (forConvention) {
            return forConvention.get(convention);
        }
    }

    /**
     * Sets the <abbr>CRS</abbr>  for the given axes convention.
     *
     * @param  crs  the <abbr>CRS</abbr> to cache.
     * @return the cached CRS. May be different than the given {@code crs} if an existing instance has been found.
     */
    final AbstractCRS setCached(final AxesConvention convention, AbstractCRS crs) {
        synchronized (forConvention) {
            return forConvention.computeIfAbsent(convention, (c) -> {
                for (final AbstractCRS existing : forConvention.values()) {
                    if (crs.equals(existing, ComparisonMode.IGNORE_METADATA)) {
                        return existing;
                    }
                }
                return crs;
            });
        }
    }

    /**
     * Returns a <abbr>CRS</abbr> equivalent to this one but with axes rearranged according the given convention.
     * If this <abbr>CRS</abbr> is already compatible with the given convention, then this method returns {@code this}.
     *
     * @param  convention  the axes convention for which a coordinate reference system is desired.
     * @return a coordinate reference system compatible with the given convention (may be {@code this}).
     *
     * @see AbstractCS#forConvention(AxesConvention)
     */
    public AbstractCRS forConvention(final AxesConvention convention) {
        AbstractCRS crs = getCached(Objects.requireNonNull(convention));
        if (crs == null) {
            final AbstractCS cs = AbstractCS.castOrCopy(coordinateSystem);
            final AbstractCS candidate = cs.forConvention(convention);
            if (candidate.equals(cs, ComparisonMode.IGNORE_METADATA)) {
                crs = this;
            } else try {
                crs = createSameType(candidate);
                crs.getCoordinateSystem();          // Throws ClassCastException if the CS type is invalid.
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.CanNotCompute_1, convention), e);
            }
            crs = setCached(convention, crs);
        }
        return crs;
    }

    /**
     * Returns a coordinate reference system of the same type as this CRS but with different axes.
     * This method shall be overridden by all {@code AbstractCRS} subclasses in this package.
     *
     * @param  cs  the coordinate system with new axes.
     * @return new CRS of the same type and datum than this CRS, but with the given axes.
     */
    AbstractCRS createSameType(final AbstractCS cs) {
        return new AbstractCRS(this, null, cs);
    }

    /**
     * Compares this coordinate reference system with the specified object for equality.
     * If the {@code mode} argument value is {@link ComparisonMode#STRICT STRICT} or
     * {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available properties
     * are compared including the {@linkplain #getDomains() domains} and remarks.
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    final var that = (AbstractCRS) object;
                    return Objects.equals(coordinateSystem, that.coordinateSystem);
                }
                default: {
                    final var that = (CoordinateReferenceSystem) object;
                    return Utilities.deepEquals(getCoordinateSystem(), that.getCoordinateSystem(), mode);
                }
            }
        }
        return false;
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
        return super.computeHashCode() + coordinateSystem.hashCode();
    }

    /**
     * Formats the inner part of the <i>Well Known Text</i> (WKT) representation of this CRS.
     * The default implementation writes the following elements in WKT 2 format:
     *
     * <ul>
     *   <li>The object {@linkplain #getName() name}.</li>
     *   <li>The datum, if any.</li>
     *   <li>All {@linkplain #getCoordinateSystem() coordinate system}'s axis.</li>
     *   <li>The unit if all axes use the same unit, or nothing otherwise.</li>
     * </ul>
     *
     * The WKT 1 format is similar to the WKT 2 one with two differences:
     * <ul>
     *   <li>Units are formatted before the axes instead of after the axes.</li>
     *   <li>If no unit can be formatted because not all axes use the same unit, then the WKT is
     *       {@linkplain Formatter#setInvalidWKT(IdentifiedObject, Exception) flagged as invalid}.</li>
     * </ul>
     *
     * @return {@inheritDoc}
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final String keyword = super.formatTo(formatter);
        formatter.newLine();
        formatDatum(formatter);
        formatter.newLine();
        final Convention convention = formatter.getConvention();
        final boolean isWKT1 = convention.majorVersion() == 1;
        if (isWKT1 || convention == Convention.INTERNAL || !isBaseCRS(formatter)) {
            final CoordinateSystem cs = getCoordinateSystem();
            formatCS(formatter, cs, ReferencingUtilities.getUnit(cs), isWKT1);
        }
        return keyword;
    }

    /**
     * Formats the datum or a view of the ensemble as a datum.
     * Subclasses should override for invoking the static {@code formatDatum(…)} with more specifiy types.
     */
    void formatDatum(final Formatter formatter) {
        /*
         * User could provide his own CRS implementation outside this SIS package, so we have
         * to check for SingleCRS interface. But all SIS classes override this implementation.
         */
        if (this instanceof SingleCRS) {
            final var sc = (SingleCRS) this;
            formatDatum(formatter, sc, sc.getDatum(), AbstractDatum::castOrCopy, AbstractCRS::getDatumEnsemble);
        }
    }

    /**
     * Formats the datum or a view of the ensemble as a datum.
     *
     * @param  <D>            the type of datum or ensemble members.
     * @param  formatter      the formatter where to write the datum.
     * @param  crs            the coordinate reference system.
     * @param  datum          the datum to format, or {@code null} if none.
     * @param  toFormattable  the function to invoke for converting the datum to a formattable object.
     * @param  asDatum        the function to invoke for getting an ensemble viewed as a datum.
     */
    static <C extends SingleCRS, D extends Datum> void formatDatum(
            final Formatter formatter,
            final C crs,
            final D datum,
            final Function<D, FormattableObject> toFormattable,
            final Function<C, D> asDatum)
    {
        final boolean supportsDynamic = formatter.getConvention().supports(Convention.WKT2_2019);
        if (datum != null) {
            if (supportsDynamic) {
                formatter.append(DynamicCRS.createIfDynamic(datum));
                formatter.newLine();
            }
            formatter.appendFormattable(datum, toFormattable);
        } else if (supportsDynamic) {
            formatter.append(getDatumEnsemble(crs));
        } else {
            // Apply `toFormattable` unconditionally for forcing a conversion of ensemble to datum.
            formatter.append(toFormattable.apply(asDatum.apply(crs)));
        }
    }

    /**
     * Returns {@code true} if the given formatter is in the process of formatting the base CRS of an
     * {@link AbstractDerivedCRS}. In such case, the coordinate system axes shall not be formatted.
     *
     * <p>This method should return {@code true} when {@code this} CRS is the value returned by
     * {@link GeneralDerivedCRS#getBaseCRS()} (typically {@link AbstractDerivedCRS#getBaseCRS()}).
     * Since the base CRS is the only CRS enclosed in derived CRS, we should have no ambiguity
     * (assuming that the user did not created some weird subclass).</p>
     *
     * <p>This method should be invoked for WKT 2 formatting only.</p>
     */
    static boolean isBaseCRS(final Formatter formatter) {
        return formatter.getEnclosingElement(1) instanceof GeneralDerivedCRS;
    }

    /**
     * Formats the given coordinate system.
     *
     * <p>In WKT 2 format, this method should not be invoked if {@link #isBaseCRS(Formatter)} returned {@code true}
     * because ISO 19162 excludes the coordinate system definition in base CRS. Note however that WKT 1 includes the
     * coordinate systems. The SIS-specific {@link Convention#INTERNAL} formats also those coordinate systems.</p>
     *
     * <h4>API note</h4>
     * The {@code unit} and {@code isWKT1} arguments could be computed by this method,
     * but are requested in order to avoid computing them twice, because the caller usually have them anyway.
     *
     * @param  formatter  the formatter where to append the coordinate system.
     * @param  cs         the coordinate system to append.
     * @param  unit       the value of {@code ReferencingUtilities.getUnit(cs)}.
     * @param  isWKT1     {@code true} if formatting WKT 1, or {@code false} for WKT 2.
     */
    final void formatCS(final Formatter formatter, final CoordinateSystem cs, final Unit<?> unit, final boolean isWKT1) {
        assert unit == ReferencingUtilities.getUnit(cs) : unit;
        assert (formatter.getConvention().majorVersion() == 1) == isWKT1 : isWKT1;
        assert isWKT1 || !isBaseCRS(formatter) || formatter.getConvention() == Convention.INTERNAL;    // Condition documented in javadoc.

        final Unit<?> oldUnit = formatter.addContextualUnit(unit);
        if (isWKT1) {                               // WKT 1 writes unit before axes, while WKT 2 writes them after axes.
            formatter.append(unit);
            if (unit == null) {
                formatter.setInvalidWKT(this, null);
            }
        } else {
            // WKT2 only, since the concept of CoordinateSystem was not explicit in WKT 1.
            formatter.appendFormattable(cs, AbstractCS::castOrCopy);
            formatter.indent(+1);
        }
        if (!isWKT1 || formatter.getConvention() != Convention.WKT1_IGNORE_AXES) {
            // Should never be null, except sometimes temporarily during construction.
            if (cs != null) {
                final int dimension = cs.getDimension();
                for (int i=0; i<dimension; i++) {
                    formatter.newLine();
                    formatter.appendFormattable(cs.getAxis(i), DefaultCoordinateSystemAxis::castOrCopy);
                }
            }
        }
        // WKT 2 writes unit after axes, while WKT 1 wrote them before axes.
        if (!isWKT1) {
            formatter.newLine();
            formatter.append(unit);
            formatter.indent(-1);
        }
        formatter.restoreContextualUnit(unit, oldUnit);
        formatter.newLine();                        // For writing the ID[…] element on its own line.
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
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    AbstractCRS() {
        super(org.apache.sis.referencing.privy.NilReferencingObject.INSTANCE);
        forConvention = forConvention();
        /*
         * The coordinate system is mandatory for SIS working. We do not verify its presence here
         * because the verification would have to be done in an `afterMarshal(…)` method and throwing
         * an exception in that method causes the whole unmarshalling to fail. But the SC_CRS adapter
         * does some verifications.
         */
    }

    /**
     * Sets the coordinate system to the given value.
     * This method is indirectly invoked by JAXB at unmarshalling time.
     *
     * @param  name  the property name, used only in case of error message to format. Can be null for auto-detect.
     * @throws IllegalStateException if the coordinate system has already been set.
     */
    final void setCoordinateSystem(String name, final CoordinateSystem cs) {
        if (coordinateSystem == null) {
            coordinateSystem = cs;
        } else {
            if (name == null) {
                name = String.valueOf(ReferencingUtilities.toPropertyName(CoordinateSystem.class, cs.getClass()));
            }
            ImplementationHelper.propertyAlreadySet(AbstractCRS.class, "setCoordinateSystem", name);
        }
    }
}
