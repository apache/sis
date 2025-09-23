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
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.datum.EngineeringDatum;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.cs.*;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.xml.bind.referencing.CS_CoordinateSystem;
import org.apache.sis.io.wkt.Formatter;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;


/**
 * A 1-, 2- or 3-dimensional contextually local coordinate reference system.
 * It can be divided into two broad categories:
 *
 * <ul>
 *   <li>earth-fixed systems applied to engineering activities on or near the surface of the earth;</li>
 *   <li>CRSs on moving platforms such as road vehicles, vessels, aircraft, or spacecraft.</li>
 * </ul>
 *
 * <p><b>Used with datum type:</b>
 *   {@linkplain org.apache.sis.referencing.datum.DefaultEngineeringDatum Engineering}.<br>
 * <b>Used with coordinate system types:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultAffineCS Affine},
 *   {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian},
 *   {@linkplain org.apache.sis.referencing.cs.DefaultCylindricalCS Cylindrical},
 *   {@linkplain org.apache.sis.referencing.cs.DefaultLinearCS Linear}.
 *   {@linkplain org.apache.sis.referencing.cs.DefaultPolarCS Polar},
 *   {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS Spherical} or
 *   {@linkplain org.apache.sis.referencing.cs.DefaultUserDefinedCS User Defined}.
 * </p>
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.referencing.datum.DefaultEngineeringDatum
 * @see org.apache.sis.referencing.factory.GeodeticAuthorityFactory#createEngineeringCRS(String)
 *
 * @since 0.4
 */
@XmlType(name = "EngineeringCRSType", propOrder = {
    "abstractCS",
    "affineCS",
    "cartesianCS",
    "cylindricalCS",
    "linearCS",
    "polarCS",
    "sphericalCS",
    "userDefinedCS",
    "datum"
})
@XmlRootElement(name = "EngineeringCRS")
public class DefaultEngineeringCRS extends AbstractSingleCRS<EngineeringDatum> implements EngineeringCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5716016061569447341L;

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * At least one of the {@code datum} and {@code ensemble} arguments shall be non-null.
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
     * @param  properties  the properties to be given to the coordinate reference system.
     * @param  datum       the datum, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of reference frames which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the coordinate system.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createEngineeringCRS(Map, EngineeringDatum, DatumEnsemble, CoordinateSystem)
     *
     * @since 1.5
     */
    public DefaultEngineeringCRS(final Map<String,?> properties,
                                 final EngineeringDatum datum,
                                 final DatumEnsemble<EngineeringDatum> ensemble,
                                 final CoordinateSystem cs)
    {
        super(properties, EngineeringDatum.class, datum, ensemble, cs);
    }

    /**
     * @deprecated A {@code DatumEnsemble} argument has been added.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public DefaultEngineeringCRS(final Map<String,?> properties,
                                 final EngineeringDatum datum,
                                 final CoordinateSystem cs)
    {
        this(properties, datum, null, cs);
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     * This is for implementing the {@link #createSameType(AbstractCS)} method only.
     */
    private DefaultEngineeringCRS(final DefaultEngineeringCRS original, final AbstractCS cs) {
        super(original, null, cs);
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
     * @see #castOrCopy(EngineeringCRS)
     */
    protected DefaultEngineeringCRS(final EngineeringCRS crs) {
        super(crs);
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
    public static DefaultEngineeringCRS castOrCopy(final EngineeringCRS object) {
        return (object == null) || (object instanceof DefaultEngineeringCRS)
                ? (DefaultEngineeringCRS) object : new DefaultEngineeringCRS(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code EngineeringCRS.class}.
     *
     * <h4>Note for implementers</h4>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code EngineeringCRS}
     * sub-interface. Overriding possibility is left mostly for implementers who wish to extend GeoAPI with their
     * own set of interfaces.
     *
     * @return {@code EngineeringCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends EngineeringCRS> getInterface() {
        return EngineeringCRS.class;
    }

    /**
     * Returns the identification of the origin of this engineering (or local) <abbr>CRS</abbr>.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     *
     * @return the engineering datum, or {@code null} if this <abbr>CRS</abbr> is related to
     *         an object identified only by a {@linkplain #getDatumEnsemble() datum ensemble}.
     */
    @Override
    @XmlElement(name = "engineeringDatum", required = true)
    public EngineeringDatum getDatum() {
        return super.getDatum();
    }

    /**
     * Returns a collection of datums which, for low accuracy requirements,
     * may be considered to be insignificantly different from each other.
     * This property may be null if this <abbr>CRS</abbr> is related to an object
     * identified only by a single {@linkplain #getDatum() datum}.
     *
     * @return the datum ensemble, or {@code null} if this <abbr>CRS</abbr> is related
     *         to an object identified only by a single {@linkplain #getDatum() datum}.
     *
     * @since 1.5
     */
    @Override
    public DatumEnsemble<EngineeringDatum> getDatumEnsemble() {
        return super.getDatumEnsemble();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public DefaultEngineeringCRS forConvention(final AxesConvention convention) {
        return (DefaultEngineeringCRS) super.forConvention(convention);
    }

    /**
     * Returns a coordinate reference system of the same type as this CRS but with different axes.
     *
     * @param  cs  the coordinate system with new axes.
     * @return new CRS of the same type and datum than this CRS, but with the given axes.
     */
    @Override
    final AbstractCRS createSameType(final AbstractCS cs) {
        return new DefaultEngineeringCRS(this, cs);
    }

    /**
     * Formats this CRS as a <i>Well Known Text</i> {@code EngineeringCRS[…]} element.
     *
     * @return {@code "EngineeringCRS"} (WKT 2) or {@code "Local_CS"} (WKT 1).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        return (formatter.getConvention().majorVersion() == 1) ? WKTKeywords.Local_CS
               : isBaseCRS(formatter) ? WKTKeywords.BaseEngCRS
                 : formatter.shortOrLong(WKTKeywords.EngCRS, WKTKeywords.EngineeringCRS);
    }

    /**
     * Formats the datum or a view of the ensemble as a datum.
     */
    @Override
    final void formatDatum(final Formatter formatter) {
        formatDatum(formatter, this, getDatum(), DefaultEngineeringDatum::castOrCopy, DatumOrEnsemble::asDatum);
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
    private DefaultEngineeringCRS() {
        /*
         * The datum and the coordinate system are mandatory for SIS working. We do not verify their presence
         * here because the verification would have to be done in an 'afterMarshal(…)' method and throwing an
         * exception in that method causes the whole unmarshalling to fail.  But the SC_CRS adapter does some
         * verifications.
         */
    }

    /**
     * Invoked by JAXB at unmarshalling time.
     *
     * @see #getDatum()
     */
    private void setDatum(final EngineeringDatum value) {
        setDatum("engineeringDatum", value);
    }

    /**
     * Used by JAXB only (invoked by reflection).
     * Only one of {@code getFooCS()} methods can return a non-null value.
     *
     * <h4>Implementation note</h4>
     * The usual way to handle {@code <xs:choice>} with JAXB is to annotate a single method like below:
     *
     * {@snippet lang="java" :
     *     @Override
     *     @XmlElements({
     *       @XmlElement(name = "cartesianCS",   type = DefaultCartesianCS.class),
     *       @XmlElement(name = "affineCS",      type = DefaultAffineCS.class),
     *       @XmlElement(name = "cylindricalCS", type = DefaultCylindricalCS.class),
     *       @XmlElement(name = "linearCS",      type = DefaultLinearCS.class),
     *       @XmlElement(name = "polarCS",       type = DefaultPolarCS.class),
     *       @XmlElement(name = "sphericalCS",   type = DefaultSphericalCS.class),
     *       @XmlElement(name = "userDefinedCS", type = DefaultUserDefinedCS.class)
     *     })
     *     public CoordinateSystem getCoordinateSystem() {
     *         return super.getCoordinateSystem();
     *     }
     * }
     *
     * However, our attempts to apply this approach worked for {@code DefaultParameterValue} but not for this class:
     * for an unknown reason, the unmarshalled CS object is empty.
     *
     * @see <a href="http://issues.apache.org/jira/browse/SIS-166">SIS-166</a>
     */
    @XmlElement(name="affineCS")      private AffineCS      getAffineCS()      {return getCoordinateSystem(AffineCS     .class);}
    @XmlElement(name="cartesianCS")   private CartesianCS   getCartesianCS()   {return getCoordinateSystem(CartesianCS  .class);}
    @XmlElement(name="cylindricalCS") private CylindricalCS getCylindricalCS() {return getCoordinateSystem(CylindricalCS.class);}
    @XmlElement(name="linearCS")      private LinearCS      getLinearCS()      {return getCoordinateSystem(LinearCS     .class);}
    @XmlElement(name="polarCS")       private PolarCS       getPolarCS()       {return getCoordinateSystem(PolarCS      .class);}
    @XmlElement(name="sphericalCS")   private SphericalCS   getSphericalCS()   {return getCoordinateSystem(SphericalCS  .class);}

    // Types that do not exist anymore in lastest ISO 19111 standard.
    @Deprecated(since = "1.5")
    @XmlElement(name="userDefinedCS") private UserDefinedCS getUserDefinedCS() {return getCoordinateSystem(UserDefinedCS.class);}

    /**
     * Invoked by JAXB at unmarshalling time.
     */
    private void setAffineCS     (final AffineCS      cs) {super.setCoordinateSystem("affineCS",      cs);}
    private void setCartesianCS  (final CartesianCS   cs) {super.setCoordinateSystem("cartesianCS",   cs);}
    private void setCylindricalCS(final CylindricalCS cs) {super.setCoordinateSystem("cylindricalCS", cs);}
    private void setLinearCS     (final LinearCS      cs) {super.setCoordinateSystem("linearCS",      cs);}
    private void setPolarCS      (final PolarCS       cs) {super.setCoordinateSystem("polarCS",       cs);}
    private void setSphericalCS  (final SphericalCS   cs) {super.setCoordinateSystem("sphericalCS",   cs);}

    // Types that do not exist anymore in lastest ISO 19111 standard.
    @Deprecated(since = "1.5")
    private void setUserDefinedCS(final UserDefinedCS cs) {super.setCoordinateSystem("userDefinedCS", cs);}

    /**
     * The types for which a specialized method exists.
     * Not including {@link CartesianCS}, because this case is already covered by {@link AffineCS}.
     */
    @SuppressWarnings("deprecation")
    private static final Class<?>[] SPECIALIZED_TYPES = {
        AffineCS.class, SphericalCS.class, CylindricalCS.class, PolarCS.class, LinearCS.class, UserDefinedCS.class
    };

    /**
     * Returns the coordinate system if it is not an instance of any of the types handled by specialized methods.
     * It is the case of {@link EllipsoidalCS}, {@link VerticalCS}, {@link TimeCS} and {@link ParametricCS}.
     */
    @XmlElement(name = "coordinateSystem", required = true)
    @XmlJavaTypeAdapter(CS_CoordinateSystem.class)
    private CoordinateSystem getAbstractCS() {
        final CoordinateSystem cs = getCoordinateSystem();
        for (final Class<?> t : SPECIALIZED_TYPES) {
            if (t.isInstance(cs)) return null;
        }
        return cs;
    }

    /**
     * Used by JAXB only (invoked by reflection).
     */
    private void setAbstractCS(final CoordinateSystem cs) {
        setCoordinateSystem(null, cs);      // `null` here means to infer the XML property name from the cs type.
    }
}
