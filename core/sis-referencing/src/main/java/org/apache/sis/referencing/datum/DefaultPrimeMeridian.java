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
package org.apache.sis.referencing.datum;

import java.util.Map;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.util.PatchedUnitFormat;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.jaxb.gml.Measure;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.ComparisonMode;

import static org.apache.sis.util.ArgumentChecks.ensureFinite;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.apache.sis.internal.referencing.Formulas;


/**
 * Defines the origin from which longitude values are determined.
 *
 * <div class="section">Creating new prime meridian instances</div>
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * In particular, the <a href="http://www.epsg.org">EPSG</a> database provides definitions for many prime meridians,
 * and Apache SIS provides convenience shortcuts for some of them.
 *
 * <p>Choice 1 in the following list is the easiest but most restrictive way to get a prime meridian.
 * The other choices provide more freedom. Each choice delegates its work to the subsequent items
 * (in the default configuration), so this list can been seen as <cite>top to bottom</cite> API.</p>
 *
 * <ol>
 *   <li>Create a {@code PrimeMeridian} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.CommonCRS#primeMeridian()}.</li>
 *   <li>Create a {@code PrimeMeridian} from an identifier in a database by invoking
 *       {@link org.opengis.referencing.datum.DatumAuthorityFactory#createPrimeMeridian(String)}.</li>
 *   <li>Create a {@code PrimeMeridian} by invoking the {@code DatumFactory.createPrimeMeridian(…)} method
 *       (implemented for example by {@link org.apache.sis.referencing.factory.GeodeticObjectFactory}).</li>
 *   <li>Create a {@code DefaultPrimeMeridian} by invoking the
 *       {@linkplain #DefaultPrimeMeridian(Map, double, Unit) constructor}.</li>
 * </ol>
 *
 * <b>Example:</b> the following code gets the Greenwich prime meridian:
 *
 * {@preformat java
 *     PrimeMeridian pm = CommonCRS.WGS84.primeMeridian();
 * }
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructor are also immutable. Unless otherwise noted in the javadoc, this condition holds if
 * all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.referencing.CommonCRS#primeMeridian()
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createPrimeMeridian(String)
 */
@XmlType(name = "PrimeMeridianType")
@XmlRootElement(name = "PrimeMeridian")
public class DefaultPrimeMeridian extends AbstractIdentifiedObject implements PrimeMeridian {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 541978454643213305L;;

    /**
     * Longitude of the prime meridian measured from the Greenwich meridian, positive eastward.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setGreenwichMeasure(Measure)}</p>
     */
    private double greenwichLongitude;

    /**
     * The angular unit of the {@linkplain #getGreenwichLongitude() Greenwich longitude}.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setGreenwichMeasure(Measure)}</p>
     */
    private Unit<Angle> angularUnit;

    /**
     * Creates a prime meridian from the given properties. The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
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
     * @param properties          The properties to be given to the identified object.
     * @param greenwichLongitude  The longitude value relative to the Greenwich Meridian.
     * @param angularUnit         The angular unit of the longitude.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createPrimeMeridian(Map, double, Unit)
     */
    public DefaultPrimeMeridian(final Map<String,?> properties, final double greenwichLongitude,
                                final Unit<Angle> angularUnit)
    {
        super(properties);
        ensureFinite("greenwichLongitude", greenwichLongitude);
        ensureNonNull("angularUnit", angularUnit);
        this.greenwichLongitude = greenwichLongitude;
        this.angularUnit = angularUnit;
    }

    /**
     * Creates a new prime meridian with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param meridian The prime meridian to copy.
     *
     * @see #castOrCopy(PrimeMeridian)
     */
    protected DefaultPrimeMeridian(final PrimeMeridian meridian) {
        super(meridian);
        greenwichLongitude = meridian.getGreenwichLongitude();
        angularUnit        = meridian.getAngularUnit();
    }

    /**
     * Returns a SIS prime meridian implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultPrimeMeridian castOrCopy(final PrimeMeridian object) {
        return (object == null) || (object instanceof DefaultPrimeMeridian)
                ? (DefaultPrimeMeridian) object : new DefaultPrimeMeridian(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code PrimeMeridian.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code PrimeMeridian}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code PrimeMeridian.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends PrimeMeridian> getInterface() {
        return PrimeMeridian.class;
    }

    /**
     * Longitude of the prime meridian measured from the Greenwich meridian, positive eastward.
     *
     * @return The prime meridian Greenwich longitude, in {@linkplain #getAngularUnit() angular unit}.
     */
    @Override
    public double getGreenwichLongitude() {
        return greenwichLongitude;
    }

    /**
     * Returns the longitude value relative to the Greenwich Meridian, expressed in the specified units.
     * This convenience method makes it easier to obtain longitude in decimal degrees using the following
     * code, regardless of the underlying angular units of this prime meridian:
     *
     * {@preformat java
     *     double longitudeInDegrees = primeMeridian.getGreenwichLongitude(NonSI.DEGREE_ANGLE);
     * }
     *
     * @param unit The unit in which to express longitude.
     * @return The Greenwich longitude in the given units.
     */
    public double getGreenwichLongitude(final Unit<Angle> unit) {
        return getAngularUnit().getConverterTo(unit).convert(getGreenwichLongitude());
    }

    /**
     * Returns the angular unit of the Greenwich longitude.
     *
     * @return The angular unit of the {@linkplain #getGreenwichLongitude() Greenwich longitude}.
     */
    @Override
    public Unit<Angle> getAngularUnit() {
        return angularUnit;
    }

    /**
     * Compares this prime meridian with the specified object for equality.
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
        if (super.equals(object, mode)) switch (mode) {
            case STRICT: {
                final DefaultPrimeMeridian that = (DefaultPrimeMeridian) object;
                return Numerics.equals(this.greenwichLongitude, that.greenwichLongitude) &&
                        Objects.equals(this.angularUnit,        that.angularUnit);
            }
            case BY_CONTRACT: {
                final PrimeMeridian that = (PrimeMeridian) object;
                return Numerics.equals(getGreenwichLongitude(), that.getGreenwichLongitude()) &&
                        Objects.equals(getAngularUnit(),        that.getAngularUnit());
            }
            default: {
                final double v1 = getGreenwichLongitude(NonSI.DEGREE_ANGLE);
                final double v2 = ReferencingUtilities.getGreenwichLongitude((PrimeMeridian) object, NonSI.DEGREE_ANGLE);
                if (mode == ComparisonMode.IGNORE_METADATA) {
                    /*
                     * We relax the check on unit of measurement because EPSG uses sexagesimal degrees
                     * for the Greenwich meridian.  Requirying the same unit would make more difficult
                     * for isWGS84(…) methods to recognize EPSG's WGS84. We allow this relaxation here
                     * because the unit of the prime meridian is usually not inherited by axes (indeed,
                     * they are often not the same in the EPSG dataset). The same is not true for other
                     * objects like DefaultEllipsoid.
                     */
                    return Numerics.equals(v1, v2);
                } else if (Numerics.epsilonEqual(v1, v2, Formulas.ANGULAR_TOLERANCE)) {
                    return true;
                }
                assert (mode != ComparisonMode.DEBUG) : Numerics.messageForDifference("greenwichLongitude", v1, v2);
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
        return super.computeHashCode() + Double.doubleToLongBits(greenwichLongitude) + Objects.hashCode(angularUnit);
    }

    /**
     * Returns {@code true} if the given formatter is in the process of formatting the prime meridian of a base CRS
     * of an {@link AbstractDerivedCRS}. In such case, base CRS coordinate system axes shall not be formatted, which
     * has the consequence of bringing the {@code UNIT[…]} element right below the {@code PRIMEM[…]} one. Example:
     *
     * {@preformat wkt
     *   ProjectedCRS[“NTF (Paris) / Lambert zone II”,
     *     BaseGeodCRS[“NTF (Paris)”,
     *       Datum[“Nouvelle Triangulation Francaise”,
     *         Ellipsoid[“NTF”, 6378249.2, 293.4660212936269]],
     *       PrimeMeridian[“Paris”, 2.5969213],
     *       AngleUnit[“grade”, 0.015707963267948967]],
     *     Conversion[“Lambert zone II”,
     *       etc...
     * }
     *
     * If we were not formatting a base CRS, we would have many lines between {@code PrimeMeridian[…]} and
     * {@code AngleUnit[…]} in the above example, which would make less obvious that the angle unit applies
     * also to the prime meridian. It does not bring any ambiguity from an ISO 19162 standard point of view,
     * but historically some other softwares interpreted the {@code PRIMEM[…]} units wrongly, which is why
     * we try to find a compromise between keeping the WKT simple and avoiding an historical ambiguity.
     *
     * @see org.apache.sis.referencing.crs.AbstractCRS#isBaseCRS(Formatter)
     */
    private static boolean isElementOfBaseCRS(final Formatter formatter) {
        return formatter.getEnclosingElement(2) instanceof GeneralDerivedCRS;
    }

    /**
     * Returns {@code true} if {@link #formatTo(Formatter)} should conservatively format the angular unit
     * even if it would be legal to omit it.
     *
     * <div class="section">Rational</div>
     * According the ISO 19162 standard, it is legal to omit the {@code PrimeMeridian} angular unit when
     * that unit is the same than the unit of the axes of the enclosing {@code GeographicCRS}. However the
     * relationship between the CRS axes and the prime meridian is less obvious in WKT2 than it was in WKT1,
     * because the WKT2 {@code UNIT[…]} element is far from the {@code PRIMEM[…]} element while it was just
     * below it in WKT1.   Furthermore, the {@code PRIMEM[…]} unit is one source of incompatibility between
     * various WKT1 parsers (i.e. some popular libraries are not conform to OGC 01-009 and ISO 19162).
     * So we are safer to unconditionally format any unit other than degrees, even if we could legally
     * omit them.
     *
     * <p>However in order to keep the WKT slightly simpler in {@link Convention#WKT2_SIMPLIFIED} mode,
     * we make an exception to the above-cited safety if the {@code UNIT[…]} element is formatted right
     * below the {@code PRIMEM[…]} one, which happen if we are inside a base CRS.
     * See {@link #isElementOfBaseCRS(Formatter)} for more discussion.
     */
    private static boolean beConservative(final Formatter formatter, final Unit<Angle> contextualUnit) {
        return !contextualUnit.equals(NonSI.DEGREE_ANGLE) && !isElementOfBaseCRS(formatter);
    }

    /**
     * Formats this prime meridian as a <cite>Well Known Text</cite> {@code PrimeMeridian[…]} element.
     *
     * @return {@code "PrimeMeridian"} (WKT 2) or {@code "PrimeM"} (WKT 1).
     *
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#53">WKT 2 specification §8.2.2</a>
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        final Convention  convention = formatter.getConvention();
        final boolean     isWKT1 = (convention.majorVersion() == 1);
        final Unit<Angle> contextualUnit = formatter.toContextualUnit(NonSI.DEGREE_ANGLE);
        Unit<Angle> unit = contextualUnit;
        if (!isWKT1) {
            unit = getAngularUnit();
            if (convention != Convention.INTERNAL) {
                unit = PatchedUnitFormat.toFormattable(unit);
            }
        }
        formatter.append(getGreenwichLongitude(unit));
        if (isWKT1) {
            return WKTKeywords.PrimeM;
        }
        if (!convention.isSimplified() || !contextualUnit.equals(unit) || beConservative(formatter, contextualUnit)) {
            formatter.append(unit);
        }
        return formatter.shortOrLong(WKTKeywords.PrimeM, WKTKeywords.PrimeMeridian);
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
    private DefaultPrimeMeridian() {
        super(org.apache.sis.internal.referencing.NilReferencingObject.INSTANCE);
        /*
         * Angular units are mandatory for SIS working. We do not verify their presence here (because the
         * verification would have to be done in an 'afterMarshal(…)' method and throwing an exception in
         * that method causes the whole unmarshalling to fail). But the CD_PrimeMeridian adapter does some
         * verifications.
         */
    }

    /**
     * Invoked by JAXB for obtaining the Greenwich longitude to marshall together with its {@code "uom"} attribute.
     */
    @XmlElement(name = "greenwichLongitude", required = true)
    private Measure getGreenwichMeasure() {
        return new Measure(greenwichLongitude, angularUnit);
    }

    /**
     * Invoked by JAXB for setting the Greenwich longitude and its unit of measurement.
     */
    private void setGreenwichMeasure(final Measure measure) {
        if (greenwichLongitude == 0 && angularUnit == null) {
            greenwichLongitude = measure.value;
            angularUnit = measure.getUnit(Angle.class);
            if (angularUnit == null) {
                /*
                 * Missing unit: if the Greenwich longitude is zero, any angular unit gives the same result
                 * (assuming that the missing unit was not applying an offset), so we can select a default.
                 * If the Greenwich longitude is not zero, presume egrees but log a warning.
                 */
                angularUnit = NonSI.DEGREE_ANGLE;
                if (greenwichLongitude != 0) {
                    Measure.missingUOM(DefaultPrimeMeridian.class, "setGreenwichMeasure");
                }
            }
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultPrimeMeridian.class, "setGreenwichMeasure", "greenwichLongitude");
        }
    }
}
