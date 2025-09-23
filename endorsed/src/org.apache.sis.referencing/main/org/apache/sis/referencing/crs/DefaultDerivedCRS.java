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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.xml.bind.referencing.SC_SingleCRS;
import org.apache.sis.xml.bind.referencing.SC_DerivedCRSType;
import org.apache.sis.xml.bind.referencing.CS_CoordinateSystem;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;
import org.apache.sis.referencing.internal.shared.WKTUtilities;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import static org.apache.sis.referencing.internal.Legacy.DERIVED_TYPE_KEY;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.collection.Containers;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;
import org.opengis.referencing.datum.ParametricDatum;
import org.opengis.referencing.crs.ParametricCRS;
import org.opengis.referencing.cs.ParametricCS;
import org.opengis.coordinate.MismatchedDimensionException;


/**
 * A coordinate reference system that is defined by its coordinate conversion from another CRS.
 * A {@code DerivedCRS} instance may implement one of the interfaces listed below,
 * provided that the conditions in the right column are met (derived from ISO 19162):
 *
 * <table class="sis">
 *   <caption>Derived CRS types</caption>
 *   <tr><th>Type</th>                   <th>Conditions</th></tr>
 *   <tr><td>{@link GeodeticCRS}</td>    <td>Base CRS is also a {@code GeodeticCRS} and is associated to the same type of coordinate system.</td></tr>
 *   <tr><td>{@link VerticalCRS}</td>    <td>Base CRS is also a {@code VerticalCRS} and coordinate system is a {@code VerticalCS}.</td></tr>
 *   <tr><td>{@link TemporalCRS}</td>    <td>Base CRS is also a {@code TemporalCRS} and coordinate system is a {@code TimeCS}.</td></tr>
 *   <tr><td>{@link ParametricCRS}</td>  <td>Base CRS is also a {@code ParametricCRS} and coordinate system is a {@code ParametricCS}.</td></tr>
 *   <tr><td>{@link EngineeringCRS}</td> <td>Base CRS is a {@code GeodeticCRS}, {@code ProjectedCRS} or {@code EngineeringCRS}.</td></tr>
 * </table>
 *
 * Those specialized subclasses can be inferred automatically by the {@link #create create(…)} static method.
 * Alternatively, users can create their own {@code DefaultDerivedCRS} subclass implementing the desired interface.
 *
 * <h2>Immutability and thread safety</h2>
 * This base class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Most SIS subclasses and related classes are immutable under similar
 * conditions. This means that unless otherwise noted in the javadoc, {@code DerivedCRS} instances created using only
 * SIS factories and static constants can be shared by many objects and passed between threads without synchronization.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createDerivedCRS(String)
 *
 * @since 0.6
 */
@XmlType(name="DerivedCRSType", propOrder = {
    "baseCRS",
    "type",
    "coordinateSystem"
})
@XmlRootElement(name = "DerivedCRS")
public class DefaultDerivedCRS extends AbstractDerivedCRS implements DerivedCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8149602276542469876L;

    /**
     * Creates a derived CRS from a defining conversion.
     * The properties given in argument follow the same rules as for the
     * {@linkplain AbstractCRS#AbstractCRS(Map, CoordinateSystem) super-class constructor}.
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
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#DOMAINS_KEY}</td>
     *     <td>{@link org.opengis.referencing.ObjectDomain} (optionally as array)</td>
     *     <td>{@link #getDomains()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * The supplied {@code conversion} argument shall <strong>not</strong> includes the operation steps
     * for performing {@linkplain org.apache.sis.referencing.cs.CoordinateSystems#swapAndScaleAxes unit
     * conversions and change of axis order} since those operations will be inferred by this constructor.
     *
     * @param  properties  the properties to be given to the new derived CRS object.
     * @param  baseCRS     coordinate reference system to base the derived CRS on.
     * @param  conversion  the defining conversion from a {@linkplain AxesConvention#NORMALIZED normalized}
     *                     base to a normalized derived CRS.
     * @param  derivedCS   the coordinate system for the derived CRS. The number of axes must match
     *                     the target dimension of the {@code baseToDerived} transform.
     * @throws MismatchedDimensionException if the source and target dimensions of {@code baseToDerived}
     *         do not match the dimensions of {@code base} and {@code derivedCS} respectively.
     *
     * @see #create(Map, SingleCRS, Conversion, CoordinateSystem)
     */
    protected DefaultDerivedCRS(final Map<String,?>    properties,
                                final SingleCRS        baseCRS,
                                final Conversion       conversion,
                                final CoordinateSystem derivedCS)
            throws MismatchedDimensionException
    {
        super(properties, baseCRS, conversion, derivedCS);
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     * This is for implementing the {@link #createSameType(AbstractCS)} method only.
     */
    DefaultDerivedCRS(final DefaultDerivedCRS original, final AbstractCS derivedCS) {
        super(original, derivedCS);
    }

    /**
     * Creates a derived CRS from a math transform. The given {@code MathTransform} shall transform coordinate
     * values specifically from the {@code baseCRS} to {@code this} CRS (optionally with an interpolation CRS);
     * there is no consideration about <q>normalized CRS</q> in this constructor.
     *
     * <h4>Conversion properties</h4>
     * The {@code properties} map given in argument can contain any entries documented in the
     * {@linkplain #DefaultDerivedCRS(Map, SingleCRS, Conversion, CoordinateSystem) above constructor},
     * together with any entries documented by the {@linkplain DefaultConversion#DefaultConversion(Map,
     * CoordinateReferenceSystem, CoordinateReferenceSystem, CoordinateReferenceSystem, OperationMethod, MathTransform)
     * conversion constructor} provided that the {@code Conversion} entry keys are prefixed by {@code "conversion."}.
     * In particular, the two first properties listed below are mandatory:
     *
     * <table class="sis">
     *   <caption>Mandatory properties and some optional properties</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@code this.getName()}</td>
     *   </tr><tr>
     *     <td>"conversion.name"</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@code conversionFromBase.getName()}</td>
     *   </tr><tr>
     *     <th colspan="3" class="hsep">Optional properties (non exhaustive list)</th>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@code this.getIdentifiers()}</td>
     *   </tr><tr>
     *     <td>{@value org.opengis.referencing.ObjectDomain#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@code domain.getDomainOfValidity()}</td>
     *   </tr>
     * </table>
     *
     * @param  properties        the properties to be given to the {@link DefaultConversion} object
     *                           (with keys prefixed by {@code "conversion."}) and to the new derived CRS object.
     * @param  baseCRS           coordinate reference system to base the derived CRS on.
     * @param  interpolationCRS  the CRS of additional coordinates needed for the operation, or {@code null} if none.
     * @param  method            the coordinate operation method (mandatory in all cases).
     * @param  baseToDerived     transform from positions in the base CRS to positions in this target CRS.
     * @param  derivedCS         the coordinate system for the derived CRS.
     * @throws IllegalArgumentException if at least one argument has an incompatible number of dimensions.
     *
     * @see #create(Map, SingleCRS, CoordinateReferenceSystem, OperationMethod, MathTransform, CoordinateSystem)
     */
    protected DefaultDerivedCRS(final Map<String,?>             properties,
                                final SingleCRS                 baseCRS,
                                final CoordinateReferenceSystem interpolationCRS,
                                final OperationMethod           method,
                                final MathTransform             baseToDerived,
                                final CoordinateSystem          derivedCS)
    {
        super(properties, baseCRS, interpolationCRS, method, baseToDerived, derivedCS);
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
     * @see #castOrCopy(DerivedCRS)
     */
    protected DefaultDerivedCRS(final DerivedCRS crs) {
        super(crs);
    }

    /**
     * Creates a derived CRS from a defining conversion and a type inferred from the given arguments.
     * This method expects the same arguments and performs the same work as the
     * {@linkplain #DefaultDerivedCRS(Map, SingleCRS, Conversion, CoordinateSystem) above constructor},
     * except that the {@code DerivedCRS} instance returned by this method may additionally implement
     * the {@link GeodeticCRS}, {@link VerticalCRS}, {@link TemporalCRS}, {@link ParametricCRS} or
     * {@link EngineeringCRS} interface.
     * See the class javadoc for more information.
     *
     * @param  properties     the properties to be given to the new derived CRS object.
     * @param  baseCRS        coordinate reference system to base the derived CRS on.
     * @param  baseToDerived  the defining conversion from a normalized base to a normalized derived CRS.
     * @param  derivedCS      the coordinate system for the derived CRS. The number of axes
     *                        must match the target dimension of the {@code baseToDerived} transform.
     * @return the newly created derived CRS, potentially implementing an additional CRS interface.
     * @throws MismatchedDimensionException if the source and target dimensions of {@code baseToDerived}
     *         do not match the dimensions of {@code base} and {@code derivedCS} respectively.
     *
     * @see #DefaultDerivedCRS(Map, SingleCRS, Conversion, CoordinateSystem)
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createDerivedCRS(Map, CoordinateReferenceSystem, Conversion, CoordinateSystem)
     */
    public static DefaultDerivedCRS create(final Map<String,?>    properties,
                                           final SingleCRS        baseCRS,
                                           final Conversion       baseToDerived,
                                           final CoordinateSystem derivedCS)
            throws MismatchedDimensionException
    {
        if (baseCRS != null && derivedCS != null) {
            final String type = getTypeKeyword(properties, baseCRS, derivedCS);
            if (type != null) switch (type) {
                case WKTKeywords.GeodeticCRS:   return new Geodetic  (properties, (GeodeticCRS)   baseCRS, baseToDerived,                derivedCS);
                case WKTKeywords.VerticalCRS:   return new Vertical  (properties, (VerticalCRS)   baseCRS, baseToDerived,   (VerticalCS) derivedCS);
                case WKTKeywords.TimeCRS:       return new Temporal  (properties, (TemporalCRS)   baseCRS, baseToDerived,       (TimeCS) derivedCS);
                case WKTKeywords.ParametricCRS: return new Parametric(properties, (ParametricCRS) baseCRS, baseToDerived, (ParametricCS) derivedCS);
                case WKTKeywords.EngineeringCRS: {
                    /*
                     * This case may happen for baseCRS of kind GeodeticCRS, ProjectedCRS or EngineeringCRS.
                     * But only the latter is associated to EngineeringDatum; the two formers are associated
                     * to GeodeticDatum. Consequently, we can implement the EngineeringCRS.getDatum() method
                     * only if the base CRS is itself of kind EngineeringCRS.  Otherwise we will return the
                     * "type-neutral" DefaultDerivedCRS implementation.
                     */
                    if (baseCRS instanceof EngineeringCRS) {
                        return new Engineering(properties, (EngineeringCRS) baseCRS, baseToDerived, derivedCS);
                    }
                    break;
                }
            }
        }
        return new DefaultDerivedCRS(properties, baseCRS, baseToDerived, derivedCS);
    }

    /**
     * Creates a derived CRS from a math transform and a type inferred from the given arguments.
     * This method expects the same arguments and performs the same work as the
     * {@linkplain #DefaultDerivedCRS(Map, SingleCRS, CoordinateReferenceSystem, OperationMethod, MathTransform,
     * CoordinateSystem) above constructor},
     * except that the {@code DerivedCRS} instance returned by this method may additionally implement
     * the {@link GeodeticCRS}, {@link VerticalCRS}, {@link TemporalCRS}, {@link ParametricCRS} or
     * {@link EngineeringCRS} interface.
     * See the class javadoc for more information.
     *
     * @param  properties        the properties to be given to the {@link DefaultConversion} object
     *                           (with keys prefixed by {@code "conversion."}) and to the new derived CRS object.
     * @param  baseCRS           coordinate reference system to base the derived CRS on.
     * @param  interpolationCRS  the CRS of additional coordinates needed for the operation, or {@code null} if none.
     * @param  method            the coordinate operation method (mandatory in all cases).
     * @param  baseToDerived     transform from positions in the base CRS to positions in this target CRS.
     * @param  derivedCS         the coordinate system for the derived CRS.
     * @return the newly created derived CRS, potentially implementing an additional CRS interface.
     * @throws IllegalArgumentException if at least one argument has an incompatible number of dimensions.
     *
     * @see #DefaultDerivedCRS(Map, SingleCRS, CoordinateReferenceSystem, OperationMethod, MathTransform, CoordinateSystem)
     */
    public static DefaultDerivedCRS create(final Map<String,?>             properties,
                                           final SingleCRS                 baseCRS,
                                           final CoordinateReferenceSystem interpolationCRS,
                                           final OperationMethod           method,
                                           final MathTransform             baseToDerived,
                                           final CoordinateSystem          derivedCS)
    {
        if (baseCRS != null && derivedCS != null) {
            final String type = getTypeKeyword(properties, baseCRS, derivedCS);
            if (type != null) switch (type) {
                case WKTKeywords.GeodeticCRS:   return new Geodetic  (properties, (GeodeticCRS)   baseCRS, interpolationCRS, method, baseToDerived,                derivedCS);
                case WKTKeywords.VerticalCRS:   return new Vertical  (properties, (VerticalCRS)   baseCRS, interpolationCRS, method, baseToDerived,  (VerticalCS)  derivedCS);
                case WKTKeywords.TimeCRS:       return new Temporal  (properties, (TemporalCRS)   baseCRS, interpolationCRS, method, baseToDerived,      (TimeCS)  derivedCS);
                case WKTKeywords.ParametricCRS: return new Parametric(properties, (ParametricCRS) baseCRS, interpolationCRS, method, baseToDerived, (ParametricCS) derivedCS);
                case WKTKeywords.EngineeringCRS: {
                    if (baseCRS instanceof EngineeringCRS) {
                        // See the comment in create(Map, SingleCRS, Conversion, CoordinateSystem)
                        return new Engineering(properties, (EngineeringCRS) baseCRS, interpolationCRS, method, baseToDerived, derivedCS);
                    }
                    break;
                }
            }
        }
        return new DefaultDerivedCRS(properties, baseCRS, interpolationCRS, method, baseToDerived, derivedCS);
    }

    /**
     * Returns a SIS coordinate reference system implementation with the same values as the given
     * arbitrary implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDerivedCRS castOrCopy(final DerivedCRS object) {
        if (object == null || object instanceof DefaultDerivedCRS) {
            return (DefaultDerivedCRS) object;
        } else {
            final String type = getTypeKeyword(null, object.getBaseCRS(), object.getCoordinateSystem());
            if (type != null) switch (type) {
                case WKTKeywords.GeodeticCRS:    return new Geodetic   (object);
                case WKTKeywords.VerticalCRS:    return new Vertical   (object);
                case WKTKeywords.TimeCRS:        return new Temporal   (object);
                case WKTKeywords.ParametricCRS:  return new Parametric (object);
                case WKTKeywords.EngineeringCRS: return new Engineering(object);
            }
            return new DefaultDerivedCRS(object);
        }
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code DerivedCRS.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code DerivedCRS}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with
     * their own set of interfaces.
     *
     * @return {@code DerivedCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends DerivedCRS> getInterface() {
        return DerivedCRS.class;
    }

    /**
     * Returns the datum of the base <abbr>CRS</abbr>.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     *
     * @return the datum of the {@linkplain #getBaseCRS() base CRS}, or {@code null} if this <abbr>CRS</abbr>
     *         is related to an object identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     */
    @Override
    public Datum getDatum() {
        return getBaseCRS().getDatum();
    }

    /**
     * Returns the datum ensemble of the base <abbr>CRS</abbr>.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a {@linkplain #getDatum() reference frame}.
     *
     * @return the datum ensemble of the {@linkplain #getBaseCRS() base CRS}, or {@code null} if this
     *         <abbr>CRS</abbr> is related to an object identified only by a {@linkplain #getDatum() datum}.
     *
     * @since 1.5
     */
    @Override
    public DatumEnsemble<?> getDatumEnsemble() {
        return getBaseCRS().getDatumEnsemble();
    }

    /**
     * Returns the CRS on which the conversion is applied.
     * This CRS defines the {@linkplain #getDatum() datum} of this CRS and (at least implicitly)
     * the {@linkplain DefaultConversion#getSourceCRS() source} of
     * the {@linkplain #getConversionFromBase() conversion from base}.
     *
     * @return the base coordinate reference system.
     */
    @Override
    @XmlElement(name = "baseCRS", required = true)
    @XmlJavaTypeAdapter(SC_SingleCRS.class)
    public SingleCRS getBaseCRS() {
        return (SingleCRS) super.getConversionFromBase().getSourceCRS();
    }

    /**
     * Returns the conversion from the {@linkplain #getBaseCRS() base CRS} to this CRS.
     * In Apache SIS, the conversion source and target CRS are set to the following values:
     *
     * <ul>
     *   <li>The conversion {@linkplain DefaultConversion#getSourceCRS() source CRS}
     *       is the {@linkplain #getBaseCRS() base CRS} of {@code this} CRS.</li>
     *   <li>The conversion {@linkplain DefaultConversion#getTargetCRS() target CRS} is {@code this} CRS.
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * This is different than ISO 19111, which allows source and target CRS to be {@code null}.</div>
     *
     * @return the conversion to this CRS.
     */
    @Override
    public Conversion getConversionFromBase() {
        return super.getConversionFromBase();
    }

    /**
     * Returns the coordinate system.
     *
     * @return the coordinate system.
     */
    @Override
    @XmlElement(name = "coordinateSystem", required = true)
    @XmlJavaTypeAdapter(CS_CoordinateSystem.class)
    public CoordinateSystem getCoordinateSystem() {
        return super.getCoordinateSystem();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultDerivedCRS forConvention(final AxesConvention convention) {
        return (DefaultDerivedCRS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate reference system of the same type as this CRS but with different axes.
     *
     * @param  cs  the coordinate system with new axes.
     * @return new CRS of the same type and datum than this CRS, but with the given axes.
     */
    @Override
    AbstractCRS createSameType(final AbstractCS derivedCS) {
        return new DefaultDerivedCRS(this, derivedCS);
    }

    /**
     * Compares this coordinate reference system with the specified object for equality.
     * In addition to the metadata documented in the
     * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#equals(Object, ComparisonMode) parent class},
     * this method considers coordinate system axes of the {@linkplain #getBaseCRS() base CRS} as metadata.
     * This means that if the given {@code ComparisonMode} is {@code IGNORE_METADATA} or more permissive,
     * then axis order of the base <abbr>CRS</abbr> is ignored
     * (but <strong>not</strong> axis order of <strong>this</strong> derived CRS).
     *
     * @param  object  the object to compare to {@code this}.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        return super.equals(object, mode);
    }

    /**
     * @hidden because nothing new to said.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode();
    }

    /**
     * Formats the inner part of the <i>Well Known Text</i> (WKT) representation of this CRS.
     *
     * @return {@code "Fitted_CS"} (WKT 1) or a type-dependent keyword (WKT 2).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        final Conversion baseToDerived = getConversionFromBase();  // Gives to users a chance to override.
        if (baseToDerived == null) {
            /*
             * Should never happen except temporarily at construction time, or if the user invoked the copy
             * constructor with an invalid Conversion, or if the user overrides the getConversionFromBase()
             * method. Delegates to the super-class method for avoiding a NullPointerException. That method
             * returns 'null', which will cause the WKT to be declared invalid.
             */
            return super.formatTo(formatter);
        }
        WKTUtilities.appendName(this, formatter, null);
        final Convention convention = formatter.getConvention();
        final boolean isWKT1 = (convention.majorVersion() == 1);
        /*
         * Both WKT 1 and WKT 2 format the base CRS. But WKT 1 formats the MathTransform before the base CRS,
         * while WKT 2 formats the conversion method and parameter values after the base CRS.
         */
        if (isWKT1) {
            MathTransform inverse = baseToDerived.getMathTransform();
            try {
                inverse = inverse.inverse();
            } catch (NoninvertibleTransformException exception) {
                formatter.setInvalidWKT(this, exception);
                inverse = null;
            }
            formatter.newLine();
            formatter.append(inverse);
        }
        formatter.newLine();
        formatter.append(WKTUtilities.toFormattable(getBaseCRS()));
        if (isWKT1) {
            return WKTKeywords.Fitted_CS;
        } else {
            formatter.newLine();
            formatter.append(new ExplicitParameters(this, WKTKeywords.DerivingConversion));     // Format inside a "DefiningConversion" element.
            if (convention == Convention.INTERNAL || !isBaseCRS(formatter)) {
                final CoordinateSystem cs = getCoordinateSystem();
                formatCS(formatter, cs, ReferencingUtilities.getUnit(cs), isWKT1);
            }
            return keyword(formatter);
        }
    }

    /**
     * Returns the WKT 2 keyword for this {@code DerivedCRS}, or {@code null} if unknown.
     * Inner subclasses will override this method for returning a constant value instead
     * than trying to infer it from the components.
     */
    String keyword(final Formatter formatter) {
        final String longKeyword = getTypeKeyword(null, getBaseCRS(), getCoordinateSystem());
        final String shortKeyword;
        if (longKeyword == null) {
            return null;
        }
        switch (longKeyword) {
            case WKTKeywords.GeodeticCRS:    shortKeyword = WKTKeywords.GeodCRS; break;
            case WKTKeywords.VerticalCRS:    shortKeyword = WKTKeywords.VertCRS; break;
            case WKTKeywords.EngineeringCRS: shortKeyword = WKTKeywords.EngCRS;  break;
            default: return longKeyword;
        }
        return formatter.shortOrLong(shortKeyword, longKeyword);
    }

    /**
     * Returns the WKT 2 keyword for a {@code DerivedCRS} having the given base CRS and derived coordinate system.
     * If the type cannot be identified, then this method returns {@code null}.
     *
     * @param  properties  user-specified properties, or {@code null} if none.
     * @param  baseCRS     the base CRS of the derived CRS to construct.
     * @param  derivedCS   the coordinate system of the derived CRS to construct.
     * @return the WKT keyword of the derived CRS type, or {@code null} if it cannot be inferred or if
     *         the type specified in the properties map is incompatible with the given coordinate system.
     */
    static String getTypeKeyword(final Map<String,?> properties, final SingleCRS baseCRS, final CoordinateSystem derivedCS) {
        Class<?> type = Containers.property(properties, DERIVED_TYPE_KEY, Class.class);
        if (type == null) {
            if (baseCRS instanceof AbstractIdentifiedObject) {
                /*
                 * For avoiding ambiguity if a user chooses to implement more
                 * than 1 CRS interface (not recommended, but may happen).
                 */
                type = ((AbstractIdentifiedObject) baseCRS).getInterface();
            } else if (baseCRS != null) {
                type = baseCRS.getClass();
            } else {
                return null;
            }
        }
        if (GeodeticCRS.class.isAssignableFrom(type) && CoordinateSystems.isGeodetic(derivedCS)) {
            return WKTKeywords.GeodeticCRS;
        } else if (VerticalCRS.class.isAssignableFrom(type) && derivedCS instanceof VerticalCS) {
            return WKTKeywords.VerticalCRS;
        } else if (TemporalCRS.class.isAssignableFrom(type) && derivedCS instanceof TimeCS) {
            return WKTKeywords.TimeCRS;
        } else if (ParametricCRS.class.isAssignableFrom(type) && derivedCS instanceof ParametricCS) {
            return WKTKeywords.ParametricCRS;
        } else if (ProjectedCRS.class.isAssignableFrom(type) || EngineeringCRS.class.isAssignableFrom(type)) {
            return WKTKeywords.EngineeringCRS;
        } else {
            return null;
        }
    }




    /**
     * A derived geodetic CRS.  Note that base CRS of kind {@link GeodeticCRS} can be used both with this class
     * and with {@link org.apache.sis.referencing.crs.DefaultDerivedCRS.Engineering}. Consequently, an ambiguity
     * may exist when choosing the kind if {@code DerivedCRS} to create for a given {@code GeodeticCRS}.
     */
    @XmlTransient
    private static final class Geodetic extends DefaultDerivedCRS implements GeodeticCRS {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -1263243517380302846L;

        /** Creates a copy of the given CRS. */
        Geodetic(DerivedCRS other) {
            super(other);
        }

        /** Creates a new CRS derived from the specified one, but with different axis order or unit. */
        private Geodetic(final Geodetic original, final AbstractCS derivedCS) {
            super(original, derivedCS);
        }

        /** Creates a new geodetic CRS from the given properties. */
        Geodetic(Map<String,?> properties, GeodeticCRS baseCRS, Conversion baseToDerived, CoordinateSystem derivedCS) {
            super(properties, baseCRS, baseToDerived, derivedCS);
        }

        /** Creates a new geodetic CRS from the given properties. */
        Geodetic(Map<String,?> properties, GeodeticCRS baseCRS, CoordinateReferenceSystem interpolationCRS,
                OperationMethod method, MathTransform baseToDerived, CoordinateSystem derivedCS)
        {
            super(properties, baseCRS, interpolationCRS, method, baseToDerived, derivedCS);
        }

        /** Returns the datum of the base geodetic CRS. */
        @Override public GeodeticDatum getDatum() {
            return (GeodeticDatum) super.getDatum();
        }

        /** Returns the datum ensemble of the base geodetic CRS. */
        @Override public DatumEnsemble<GeodeticDatum> getDatumEnsemble() {
            return ((GeodeticCRS) getBaseCRS()).getDatumEnsemble();
        }

        /** Returns a coordinate reference system of the same type as this CRS but with different axes. */
        @Override AbstractCRS createSameType(final AbstractCS derivedCS) {
            return new Geodetic(this, derivedCS);
        }

        /** Returns the WKT keyword for this derived CRS type. */
        @Override String keyword(final Formatter formatter) {
            if (formatter.getConvention().supports(Convention.WKT2_2019) && getCoordinateSystem() instanceof EllipsoidalCS) {
                return formatter.shortOrLong(WKTKeywords.GeogCRS, WKTKeywords.GeographicCRS);
            }
            return formatter.shortOrLong(WKTKeywords.GeodCRS, WKTKeywords.GeodeticCRS);
        }

        /** Returns the GML code for this derived CRS type. */
        @Override SC_DerivedCRSType getType() {
            return new SC_DerivedCRSType("geodetic");
        }
    }

    /**
     * A derived vertical CRS.
     */
    @XmlTransient
    private static final class Vertical extends DefaultDerivedCRS implements VerticalCRS {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -5599709829566076972L;

        /** Creates a copy of the given CRS. */
        Vertical(DerivedCRS other) {
            super(other);
        }

        /** Creates a new CRS derived from the specified one, but with different axis order or unit. */
        private Vertical(final Vertical original, final AbstractCS derivedCS) {
            super(original, derivedCS);
        }

        /** Creates a new vertical CRS from the given properties. */
        Vertical(Map<String,?> properties, VerticalCRS baseCRS, Conversion baseToDerived, VerticalCS derivedCS) {
            super(properties, baseCRS, baseToDerived, derivedCS);
        }

        /** Creates a new vertical CRS from the given properties. */
        Vertical(Map<String,?> properties, VerticalCRS baseCRS, CoordinateReferenceSystem interpolationCRS,
                OperationMethod method, MathTransform baseToDerived, VerticalCS derivedCS)
        {
            super(properties, baseCRS, interpolationCRS, method, baseToDerived, derivedCS);
        }

        /** Returns the datum of the base vertical CRS. */
        @Override public VerticalDatum getDatum() {
            return (VerticalDatum) super.getDatum();
        }

        /** Returns the datum ensemble of the base vertical CRS. */
        @Override public DatumEnsemble<VerticalDatum> getDatumEnsemble() {
            return ((VerticalCRS) getBaseCRS()).getDatumEnsemble();
        }

        /** Returns the coordinate system given at construction time. */
        @Override public VerticalCS getCoordinateSystem() {
            return (VerticalCS) super.getCoordinateSystem();
        }

        /** Returns a coordinate reference system of the same type as this CRS but with different axes. */
        @Override AbstractCRS createSameType(final AbstractCS derivedCS) {
            return new Vertical(this, derivedCS);
        }

        /** Returns the WKT keyword for this derived CRS type. */
        @Override String keyword(final Formatter formatter) {
            return formatter.shortOrLong(WKTKeywords.VertCRS, WKTKeywords.VerticalCRS);
        }

        /** Returns the GML code for this derived CRS type. */
        @Override SC_DerivedCRSType getType() {
            return new SC_DerivedCRSType("vertical");
        }
    }

    /**
     * A derived temporal CRS.
     */
    @XmlTransient
    private static final class Temporal extends DefaultDerivedCRS implements TemporalCRS {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -4721311735720248819L;

        /** Creates a copy of the given CRS. */
        Temporal(DerivedCRS other) {
            super(other);
        }

        /** Creates a new CRS derived from the specified one, but with different axis order or unit. */
        private Temporal(final Temporal original, final AbstractCS derivedCS) {
            super(original, derivedCS);
        }

        /** Creates a new temporal CRS from the given properties. */
        Temporal(Map<String,?> properties, TemporalCRS baseCRS, Conversion baseToDerived, TimeCS derivedCS) {
            super(properties, baseCRS, baseToDerived, derivedCS);
        }

        /** Creates a new temporal CRS from the given properties. */
        Temporal(Map<String,?> properties, TemporalCRS baseCRS, CoordinateReferenceSystem interpolationCRS,
                OperationMethod method, MathTransform baseToDerived, TimeCS derivedCS)
        {
            super(properties, baseCRS, interpolationCRS, method, baseToDerived, derivedCS);
        }

        /** Returns the datum of the base temporal CRS. */
        @Override public TemporalDatum getDatum() {
            return (TemporalDatum) super.getDatum();
        }

        /** Returns the datum ensemble of the base temporal CRS. */
        @Override public DatumEnsemble<TemporalDatum> getDatumEnsemble() {
            return ((TemporalCRS) getBaseCRS()).getDatumEnsemble();
        }

        /** Returns the coordinate system given at construction time. */
        @Override public TimeCS getCoordinateSystem() {
            return (TimeCS) super.getCoordinateSystem();
        }

        /** Returns a coordinate reference system of the same type as this CRS but with different axes. */
        @Override AbstractCRS createSameType(final AbstractCS derivedCS) {
            return new Temporal(this, derivedCS);
        }

        /** Returns the WKT keyword for this derived CRS type. */
        @Override String keyword(final Formatter formatter) {
            return WKTKeywords.TimeCRS;
        }

        /** Returns the GML code for this derived CRS type. */
        @Override SC_DerivedCRSType getType() {
            return new SC_DerivedCRSType("time");
        }
    }

    /**
     * A derived parametric CRS.
     */
    @XmlTransient
    private static final class Parametric extends DefaultDerivedCRS implements ParametricCRS {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 2344979923957294024L;

        /** Creates a copy of the given CRS. */
        Parametric(DerivedCRS other) {
            super(other);
        }

        /** Creates a new CRS derived from the specified one, but with different axis order or unit. */
        private Parametric(final Parametric original, final AbstractCS derivedCS) {
            super(original, derivedCS);
        }

        /** Creates a new parametric CRS from the given properties. */
        Parametric(Map<String,?> properties, ParametricCRS baseCRS, Conversion baseToDerived, ParametricCS derivedCS) {
            super(properties, baseCRS, baseToDerived, derivedCS);
        }

        /** Creates a new parametric CRS from the given properties. */
        Parametric(Map<String,?> properties, ParametricCRS baseCRS, CoordinateReferenceSystem interpolationCRS,
                OperationMethod method, MathTransform baseToDerived, ParametricCS derivedCS)
        {
            super(properties, baseCRS, interpolationCRS, method, baseToDerived, derivedCS);
        }

        /** Returns the datum of the base parametric CRS. */
        @Override public ParametricDatum getDatum() {
            return (ParametricDatum) super.getDatum();
        }

        /** Returns the datum ensemble of the base parametric CRS. */
        @Override public DatumEnsemble<ParametricDatum> getDatumEnsemble() {
            return ((ParametricCRS) getBaseCRS()).getDatumEnsemble();
        }

        /** Returns the coordinate system given at construction time. */
        @Override public ParametricCS getCoordinateSystem() {
            return (ParametricCS) super.getCoordinateSystem();
        }

        /** Returns a coordinate reference system of the same type as this CRS but with different axes. */
        @Override AbstractCRS createSameType(final AbstractCS derivedCS) {
            return new Parametric(this, derivedCS);
        }

        /** Returns the WKT keyword for this derived CRS type. */
        @Override String keyword(final Formatter formatter) {
            return WKTKeywords.ParametricCRS;
        }

        /** Returns the GML code for this derived CRS type. */
        @Override SC_DerivedCRSType getType() {
            return new SC_DerivedCRSType("parametric");
        }
    }

    /**
     * An derived engineering CRS. ISO 19162 restricts the base CRS to {@code EngineeringCRS}, {@code ProjectedCRS}
     * or {@code GeodeticCRS}. Note that in the latter case, an ambiguity may exist with the
     * {@link org.apache.sis.referencing.crs.DefaultDerivedCRS.Geodetic} when deciding which {@code DerivedCRS} to
     * create.
     */
    @XmlTransient
    private static final class Engineering extends DefaultDerivedCRS implements EngineeringCRS {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 42334975023270039L;

        /** Creates a copy of the given CRS. */
        Engineering(DerivedCRS other) {
            super(other);
        }

        /** Creates a new CRS derived from the specified one, but with different axis order or unit. */
        private Engineering(final Engineering original, final AbstractCS derivedCS) {
            super(original, derivedCS);
        }

        /** Creates a new engineering CRS from the given properties. */
        Engineering(Map<String,?> properties, EngineeringCRS baseCRS, Conversion baseToDerived, CoordinateSystem derivedCS) {
            super(properties, baseCRS, baseToDerived, derivedCS);
        }

        /** Creates a new engineering CRS from the given properties. */
        Engineering(Map<String,?> properties, EngineeringCRS baseCRS, CoordinateReferenceSystem interpolationCRS,
                OperationMethod method, MathTransform baseToDerived, CoordinateSystem derivedCS)
        {
            super(properties, baseCRS, interpolationCRS, method, baseToDerived, derivedCS);
        }

        /** Returns the datum of the base engineering CRS. */
        @Override public EngineeringDatum getDatum() {
            return (EngineeringDatum) super.getDatum();
        }

        /** Returns the datum ensemble of the base engineering CRS. */
        @Override public DatumEnsemble<EngineeringDatum> getDatumEnsemble() {
            return ((EngineeringCRS) getBaseCRS()).getDatumEnsemble();
        }

        /** Returns a coordinate reference system of the same type as this CRS but with different axes. */
        @Override AbstractCRS createSameType(final AbstractCS derivedCS) {
            return new Engineering(this, derivedCS);
        }

        /** Returns the WKT keyword for this derived CRS type. */
        @Override String keyword(final Formatter formatter) {
            return formatter.shortOrLong(WKTKeywords.EngCRS, WKTKeywords.EngineeringCRS);
        }

        /** Returns the GML code for this derived CRS type. */
        @Override SC_DerivedCRSType getType() {
            return new SC_DerivedCRSType("engineering");
        }
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
     * Constructs a new object in which every attributes are set to a default value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    private DefaultDerivedCRS() {
    }

    /**
     * Returns the {@code <gml:derivedCRSType>} element to marshal. The default implementation tries to infer this
     * information from the {@code DefaultDerivedCRS} properties, but subclasses will override for more determinism.
     *
     * <h4>API note</h4>
     * There is no setter at this time because SIS does not store this information in a {@code DefaultDerivedCRS}
     * field. Instead, we rely on the interface that we implement. For example, a {@code DefaultDerivedCRS} of type
     * {@code SC_DerivedCRSType.vertical} will implement the {@link VerticalCRS} interface.
     */
    @XmlElement(name = "derivedCRSType", required = true)
    SC_DerivedCRSType getType() {
        return SC_DerivedCRSType.fromWKT(getTypeKeyword(null, getBaseCRS(), getCoordinateSystem()));
    }

    /**
     * Used by JAXB only (invoked by reflection).
     *
     * @see #getBaseCRS()
     */
    private void setBaseCRS(final SingleCRS crs) {
        setBaseCRS("baseCRS", crs);
    }

    /**
     * Used by JAXB only (invoked by reflection).
     *
     * @see #getCoordinateSystem()
     */
    private void setCoordinateSystem(final CoordinateSystem cs) {
        setCoordinateSystem("coordinateSystem", cs);
    }
}
