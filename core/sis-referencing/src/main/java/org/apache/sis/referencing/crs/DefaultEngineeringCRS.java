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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.cs.AffineCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.cs.CylindricalCS;
import org.opengis.referencing.cs.LinearCS;
import org.opengis.referencing.cs.PolarCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.UserDefinedCS;
import org.opengis.referencing.datum.EngineeringDatum;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A contextually local coordinate reference system. It can be divided into two broad categories:
 *
 * <ul>
 *   <li>earth-fixed systems applied to engineering activities on or near the surface of the earth;</li>
 *   <li>CRSs on moving platforms such as road vehicles, vessels, aircraft, or spacecraft.</li>
 * </ul>
 *
 * <p><b>Used with coordinate system types:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultAffineCS Affine},
 *   {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian},
 *   {@linkplain org.apache.sis.referencing.cs.DefaultCylindricalCS Cylindrical},
 *   {@linkplain org.apache.sis.referencing.cs.DefaultLinearCS Linear}.
 *   {@linkplain org.apache.sis.referencing.cs.DefaultPolarCS Polar},
 *   {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS Spherical} or
 *   {@linkplain org.apache.sis.referencing.cs.DefaultUserDefinedCS User Defined}.
 * </p>
 *
 * {@section Immutability and thread safety}
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself),
 * the coordinate system and the datum instances given to the constructor are also immutable. Unless otherwise noted
 * in the javadoc, this condition holds if all components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 */
@XmlType(name = "EngineeringCRSType", propOrder = {
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
public class DefaultEngineeringCRS extends AbstractCRS implements EngineeringCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6695541732063382701L;

    /**
     * The datum.
     */
    @XmlElement(name = "engineeringDatum")
    private final EngineeringDatum datum;

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultEngineeringCRS() {
        datum = null;
    }

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
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
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#DOMAIN_OF_VALIDITY_KEY}</td>
     *     <td>{@link org.opengis.metadata.extent.Extent}</td>
     *     <td>{@link #getDomainOfValidity()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.datum.Datum#SCOPE_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getScope()}</td>
     *   </tr>
     * </table>
     *
     * @param properties The properties to be given to the coordinate reference system.
     * @param datum The datum.
     * @param cs The coordinate system.
     */
    public DefaultEngineeringCRS(final Map<String,?> properties,
                                 final EngineeringDatum   datum,
                                 final CoordinateSystem      cs)
    {
        super(properties, cs);
        ensureNonNull("datum", datum);
        this.datum = datum;
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
     * @see #castOrCopy(EngineeringCRS)
     */
    protected DefaultEngineeringCRS(final EngineeringCRS crs) {
        super(crs);
        datum = crs.getDatum();
    }

    /**
     * Returns a SIS coordinate reference system implementation with the same values than the given
     * arbitrary implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code EngineeringCRS}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their
     * own set of interfaces.</div>
     *
     * @return {@code EngineeringCRS.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends EngineeringCRS> getInterface() {
        return EngineeringCRS.class;
    }

    /**
     * Returns the datum.
     *
     * @return The datum.
     */
    @Override
    public final EngineeringDatum getDatum() {
        return datum;
    }

    /**
     * Invoked by JAXB at marshalling time.
     */
    @XmlElement(name="affineCS")      private AffineCS      getAffineCS()      {return getCoordinateSystem(AffineCS     .class);}
    @XmlElement(name="cartesianCS")   private CartesianCS   getCartesianCS()   {return getCoordinateSystem(CartesianCS  .class);}
    @XmlElement(name="cylindricalCS") private CylindricalCS getCylindricalCS() {return getCoordinateSystem(CylindricalCS.class);}
    @XmlElement(name="linearCS")      private LinearCS      getLinearCS()      {return getCoordinateSystem(LinearCS     .class);}
    @XmlElement(name="polarCS")       private PolarCS       getPolarCS()       {return getCoordinateSystem(PolarCS      .class);}
    @XmlElement(name="sphericalCS")   private SphericalCS   getSphericalCS()   {return getCoordinateSystem(SphericalCS  .class);}
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
    private void setUserDefinedCS(final UserDefinedCS cs) {super.setCoordinateSystem("userDefinedCS", cs);}

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
     * Returns a coordinate reference system of the same type than this CRS but with different axes.
     */
    @Override
    final AbstractCRS createSameType(final Map<String,?> properties, final CoordinateSystem cs) {
        return new DefaultEngineeringCRS(properties, datum, cs);
    }

    /**
     * Formats this CRS as a <cite>Well Known Text</cite> {@code EngineeringCRS[…]} element.
     *
     * @return {@code "EngineeringCRS"} (WKT 2) or {@code "Local_CS"} (WKT 1).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        return (formatter.getConvention().majorVersion() == 1) ? "Local_CS" : "EngineeringCRS";
    }
}
