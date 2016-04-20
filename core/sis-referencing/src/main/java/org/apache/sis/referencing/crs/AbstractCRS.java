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
import java.util.ConcurrentModificationException;
import javax.measure.unit.Unit;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.cs.AffineCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.internal.referencing.WKTUtilities.toFormattable;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


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
 * {@preformat java
 *   int dimension = crs.getCoordinateSystem().getDimension();
 * }
 *
 * However most subclasses restrict the allowed number of dimensions.
 *
 * <div class="section">Instantiation</div>
 * This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Typical applications should create instances of the most specific subclass prefixed by {@code Default} instead.
 * An exception to this rule may occur when it is not possible to identify the exact CRS type.
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code CoordinateReferenceSystem} instances
 * created using only SIS factories and static constants can be shared by many objects and passed between threads
 * without synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see AbstractCS
 * @see org.apache.sis.referencing.datum.AbstractDatum
 */
@XmlType(name = "AbstractCRSType", propOrder = {
    "domainOfValidity",
    "scope"
})
@XmlRootElement(name = "AbstractCRS")
@XmlSeeAlso({
    AbstractDerivedCRS.class,
    DefaultGeodeticCRS.class,
    DefaultVerticalCRS.class,
    DefaultTemporalCRS.class,
    DefaultParametricCRS.class,
    DefaultEngineeringCRS.class,
    DefaultImageCRS.class,
    DefaultCompoundCRS.class
})
public class AbstractCRS extends AbstractReferenceSystem implements CoordinateReferenceSystem {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7433284548909530047L;

    /**
     * The coordinate system.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setCoordinateSystem(String, CoordinateSystem)}</p>
     *
     * @see #getCoordinateSystem()
     */
    private CoordinateSystem coordinateSystem;

    /**
     * Other coordinate reference systems computed from this CRS by the application of an axes convention.
     * Created only when first needed.
     *
     * @see #forConvention(AxesConvention)
     */
    private transient Map<AxesConvention,AbstractCRS> forConvention;

    /**
     * Creates a coordinate reference system from the given properties and coordinate system.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
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
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceSystem#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * @param properties The properties to be given to the coordinate reference system.
     * @param cs The coordinate system.
     */
    public AbstractCRS(final Map<String,?> properties, final CoordinateSystem cs) {
        super(properties);
        ensureNonNull("cs", cs);
        coordinateSystem = cs;
    }

    /**
     * Constructs a new coordinate reference system with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param crs The coordinate reference system to copy.
     *
     * @see #castOrCopy(CoordinateReferenceSystem)
     */
    protected AbstractCRS(final CoordinateReferenceSystem crs) {
        super(crs);
        coordinateSystem = crs.getCoordinateSystem();
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
     *       {@linkplain #AbstractCRS(CoordinateReferenceSystem) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       properties contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     * @return The coordinate reference system interface implemented by this class.
     */
    @Override
    public Class<? extends CoordinateReferenceSystem> getInterface() {
        return CoordinateReferenceSystem.class;
    }

    /**
     * Returns the datum, or {@code null} if none.
     *
     * This property does not exist in {@code CoordinateReferenceSystem} interface — it is defined in the
     * {@link SingleCRS} sub-interface instead. But Apache SIS does not define an {@code AbstractSingleCRS} class
     * in order to simplify our class hierarchy, so we provide a datum getter in this class has a hidden property.
     * Subclasses implementing {@code SingleCRS} (basically all SIS subclasses except {@link DefaultCompoundCRS})
     * will override this method with public access and more specific return type.
     *
     * @return The datum, or {@code null} if none.
     */
    Datum getDatum() {
        // User could provide his own CRS implementation outside this SIS package, so we have
        // to check for SingleCRS interface. But all SIS classes override this implementation.
        return (this instanceof SingleCRS) ? ((SingleCRS) this).getDatum() : null;
    }

    /**
     * Returns the coordinate system.
     *
     * @return The coordinate system.
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
            // Special case for AfficeCS: must ensure that the cs is not the CartesianCS subtype.
            if (type != AffineCS.class || !(cs instanceof CartesianCS)) {
                return (T) cs;
            }
        }
        return null;
    }

    /**
     * Returns the existing CRS for the given convention, or {@code null} if none.
     */
    final AbstractCRS getCached(final AxesConvention convention) {
        assert Thread.holdsLock(this);
        return (forConvention != null) ? forConvention.get(convention) : null;
    }

    /**
     * Sets the CRS for the given axes convention.
     *
     * @param crs The CRS to cache.
     * @return The cached CRS. May be different than the given {@code crs} if an existing instance has been found.
     */
    final AbstractCRS setCached(final AxesConvention convention, AbstractCRS crs) {
        assert Thread.holdsLock(this);
        if (forConvention == null) {
            forConvention = new EnumMap<AxesConvention,AbstractCRS>(AxesConvention.class);
        } else if (crs != this) {
            for (final AbstractCRS existing : forConvention.values()) {
                if (crs.equals(existing)) {
                    crs = existing;
                    break;
                }
            }
        }
        if (forConvention.put(convention, crs) != null) {
            throw new ConcurrentModificationException(); // Should never happen, unless we have a synchronization bug.
        }
        return crs;
    }

    /**
     * Returns a coordinate reference system equivalent to this one but with axes rearranged according the given
     * convention. If this CRS is already compatible with the given convention, then this method returns {@code this}.
     *
     * @param  convention The axes convention for which a coordinate reference system is desired.
     * @return A coordinate reference system compatible with the given convention (may be {@code this}).
     *
     * @see AbstractCS#forConvention(AxesConvention)
     */
    public synchronized AbstractCRS forConvention(final AxesConvention convention) {
        ensureNonNull("convention", convention);
        AbstractCRS crs = getCached(convention);
        if (crs == null) {
            final AbstractCS cs = AbstractCS.castOrCopy(coordinateSystem);
            final AbstractCS candidate = cs.forConvention(convention);
            if (candidate == cs) {
                crs = this;
            } else {
                crs = createSameType(IdentifiedObjects.getProperties(this, IDENTIFIERS_KEY), candidate);
            }
            crs = setCached(convention, crs);
        }
        return crs;
    }

    /**
     * Returns a coordinate reference system of the same type than this CRS but with different axes.
     * This method shall be overridden by all {@code AbstractCRS} subclasses in this package.
     */
    AbstractCRS createSameType(final Map<String,?> properties, final CoordinateSystem cs) {
        return new AbstractCRS(properties, cs);
    }

    /**
     * Compares this coordinate reference system with the specified object for equality.
     * If the {@code mode} argument value is {@link ComparisonMode#STRICT STRICT} or
     * {@link ComparisonMode#BY_CONTRACT BY_CONTRACT}, then all available properties are
     * compared including the {@linkplain #getDomainOfValidity() domain of validity} and
     * the {@linkplain #getScope() scope}.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final Datum datum = getDatum();
            switch (mode) {
                case STRICT: {
                    final AbstractCRS that = (AbstractCRS) object;
                    return Objects.equals(datum, that.getDatum()) &&
                           Objects.equals(coordinateSystem, that.coordinateSystem);
                }
                default: {
                    return deepEquals(datum, (object instanceof SingleCRS) ? ((SingleCRS) object).getDatum() : null, mode) &&
                           deepEquals(getCoordinateSystem(), ((CoordinateReferenceSystem) object).getCoordinateSystem(), mode);
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
     * @return The hash code value. This value may change in any future Apache SIS version.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Objects.hash(getDatum(), coordinateSystem);
    }

    /**
     * Formats the inner part of the <cite>Well Known Text</cite> (WKT) representation of this CRS.
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
     *   <li>Units are formatted before the axes instead than after the axes.</li>
     *   <li>If no unit can be formatted because not all axes use the same unit, then the WKT is
     *       {@linkplain Formatter#setInvalidWKT(IdentifiedObject, Exception) flagged as invalid}.</li>
     * </ul>
     *
     * @return {@inheritDoc}
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html">WKT 2 specification</a>
     * @see <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">Legacy WKT 1</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final String keyword = super.formatTo(formatter);
        formatter.newLine();
        formatter.append(toFormattable(getDatum()));
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
     * <div class="note"><b>Note:</b> the {@code unit} and {@code isWKT1} arguments could be computed by this method,
     * but are requested in order to avoid computing them twice, because the caller usually have them anyway.</div>
     *
     * @param formatter The formatter where to append the coordinate system.
     * @param cs        The coordinate system to append.
     * @param unit      The value of {@code ReferencingUtilities.getUnit(cs)}.
     * @param isWKT1    {@code true} if formatting WKT 1, or {@code false} for WKT 2.
     */
    final void formatCS(final Formatter formatter, final CoordinateSystem cs, final Unit<?> unit, final boolean isWKT1) {
        assert unit == ReferencingUtilities.getUnit(cs) : unit;
        assert (formatter.getConvention().majorVersion() == 1) == isWKT1 : isWKT1;
        assert isWKT1 || !isBaseCRS(formatter) || formatter.getConvention() == Convention.INTERNAL;    // Condition documented in javadoc.

        final Unit<?> oldUnit = formatter.addContextualUnit(unit);
        if (isWKT1) { // WKT 1 writes unit before axes, while WKT 2 writes them after axes.
            formatter.append(unit);
            if (unit == null) {
                formatter.setInvalidWKT(this, null);
            }
        } else {
            formatter.append(toFormattable(cs)); // WKT2 only, since the concept of CoordinateSystem was not explicit in WKT 1.
            formatter.indent(+1);
        }
        if (!isWKT1 || formatter.getConvention() != Convention.WKT1_IGNORE_AXES) {
            if (cs != null) { // Should never be null, except sometime temporarily during construction.
                final int dimension = cs.getDimension();
                for (int i=0; i<dimension; i++) {
                    formatter.newLine();
                    formatter.append(toFormattable(cs.getAxis(i)));
                }
            }
        }
        if (!isWKT1) { // WKT 2 writes unit after axes, while WKT 1 wrote them before axes.
            formatter.newLine();
            formatter.append(unit);
            formatter.indent(-1);
        }
        formatter.restoreContextualUnit(unit, oldUnit);
        formatter.newLine(); // For writing the ID[…] element on its own line.
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    AbstractCRS() {
        super(org.apache.sis.internal.referencing.NilReferencingObject.INSTANCE);
        /*
         * The coordinate system is mandatory for SIS working. We do not verify its presence here
         * because the verification would have to be done in an 'afterMarshal(…)' method and throwing
         * an exception in that method causes the whole unmarshalling to fail. But the SC_CRS adapter
         * does some verifications.
         */
    }

    /**
     * Sets the coordinate system to the given value.
     * This method is indirectly invoked by JAXB at unmarshalling time.
     *
     * @param  name The property name, used only in case of error message to format. Can be null for auto-detect.
     * @throws IllegalStateException If the coordinate system has already been set.
     */
    final void setCoordinateSystem(String name, final CoordinateSystem cs) {
        if (coordinateSystem == null) {
            coordinateSystem = cs;
        } else {
            if (name == null) {
                name = String.valueOf(ReferencingUtilities.toPropertyName(CoordinateSystem.class, cs.getClass()));
            }
            MetadataUtilities.propertyAlreadySet(AbstractCRS.class, "setCoordinateSystem", name);
        }
    }
}
