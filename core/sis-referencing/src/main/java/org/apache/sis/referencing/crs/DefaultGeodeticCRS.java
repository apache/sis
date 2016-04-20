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
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.internal.referencing.Legacy;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.referencing.CRS;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A 2- or 3-dimensional coordinate reference system based on a geodetic datum.
 * The CRS is geographic if associated with an ellipsoidal coordinate system,
 * or geocentric if associated with a spherical or Cartesian coordinate system.
 *
 * <p><b>Used with datum type:</b>
 *   {@linkplain org.apache.sis.referencing.datum.DefaultGeodeticDatum Geodetic}.<br>
 * <b>Used with coordinate system types:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian},
 *   {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS Spherical} or
 *   {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS Ellipsoidal}.
 * </p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@XmlType(name = "GeodeticCRSType", propOrder = {
    "ellipsoidalCS",
    "cartesianCS",
    "sphericalCS",
    "datum"
})
@XmlRootElement(name = "GeodeticCRS")
class DefaultGeodeticCRS extends AbstractCRS implements GeodeticCRS { // If made public, see comment in getDatum().
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6205678223972395910L;

    /**
     * The datum.
     *
     * <p><b>Consider this field as final!</b>
     * This field is modified only at unmarshalling time by {@link #setDatum(GeodeticDatum)}</p>
     *
     * @see #getDatum()
     */
    private GeodeticDatum datum;

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
     *
     * <p>This constructor is not public because it does not verify the {@code cs} type.</p>
     *
     * @param properties The properties to be given to the coordinate reference system.
     * @param datum The datum.
     * @param cs The coordinate system.
     */
    DefaultGeodeticCRS(final Map<String,?> properties,
                       final GeodeticDatum datum,
                       final CoordinateSystem cs)
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
     */
    protected DefaultGeodeticCRS(final GeodeticCRS crs) {
        super(crs);
        datum = crs.getDatum();
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code GeodeticCRS.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return The coordinate reference system interface implemented by this class.
     */
    @Override
    public Class<? extends GeodeticCRS> getInterface() {
        return GeodeticCRS.class;
    }

    /**
     * Returns the datum.
     *
     * This method is overridden is subclasses for documentation purpose only, mostly for showing
     * this method in the appropriate position in javadoc (instead than at the bottom of the page).
     * If {@code DefaultGeodeticCRS} is made public in a future SIS version, then we could remove
     * the overridden methods.
     *
     * @return The datum.
     */
    @Override
    @XmlElement(name = "geodeticDatum", required = true)
    public GeodeticDatum getDatum() {
        return datum;
    }

    /**
     * Returns a coordinate reference system of the same type than this CRS but with different axes.
     * This method shall be overridden by all {@code DefaultGeodeticCRS} subclasses in this package.
     */
    @Override
    AbstractCRS createSameType(final Map<String,?> properties, final CoordinateSystem cs) {
        return new DefaultGeodeticCRS(properties, datum, cs);
    }

    /**
     * Formats this CRS as a <cite>Well Known Text</cite> {@code GeodeticCRS[…]} element.
     * More information about the WKT format is documented in subclasses.
     *
     * @return {@code "GeodeticCRS"} (WKT 2) or {@code "GeogCS"}/{@code "GeocCS"} (WKT 1).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        WKTUtilities.appendName(this, formatter, null);
        CoordinateSystem cs = getCoordinateSystem();
        final Convention convention = formatter.getConvention();
        final boolean isWKT1 = (convention.majorVersion() == 1);
        final boolean isGeographicWKT1 = isWKT1 && (cs instanceof EllipsoidalCS);
        if (isGeographicWKT1 && cs.getDimension() == 3) {
            /*
             * Version 1 of WKT format did not have three-dimensional GeographicCRS. Instead, such CRS were formatted
             * as a CompoundCRS made of a two-dimensional GeographicCRS with a VerticalCRS for the ellipsoidal height.
             * Note that such compound is illegal in WKT 2 and ISO 19111 standard, as ellipsoidal height shall not be
             * separated from the geographic component. So we perform this separation only at WKT 1 formatting time.
             */
            SingleCRS first  = CRS.getHorizontalComponent(this);
            SingleCRS second = CRS.getVerticalComponent(this, true);
            if (first != null && second != null) {                      // Should not be null, but we are paranoiac.
                if (AxisDirection.UP.equals(AxisDirections.absolute(cs.getAxis(0).getDirection()))) {
                    // It is very unusual to have VerticalCRS first, but our code tries to be robust.
                    final SingleCRS t = first;
                    first = second; second = t;
                }
                formatter.newLine(); formatter.append(WKTUtilities.toFormattable(first));
                formatter.newLine(); formatter.append(WKTUtilities.toFormattable(second));
                formatter.newLine();
                return WKTKeywords.Compd_CS;
            }
        }
        /*
         * Unconditionally format the datum element, followed by the prime meridian.
         * The prime meridian is part of datum according ISO 19111, but is formatted
         * as a sibling (rather than a child) element in WKT for historical reasons.
         */
        final GeodeticDatum datum = getDatum();             // Gives subclasses a chance to override.
        formatter.newLine();
        formatter.append(WKTUtilities.toFormattable(datum));
        formatter.newLine();
        final PrimeMeridian pm = datum.getPrimeMeridian();
        final Unit<Angle> angularUnit = AxisDirections.getAngularUnit(cs, null);
        if (convention != Convention.WKT2_SIMPLIFIED ||     // Really this specific enum, not Convention.isSimplified().
                ReferencingUtilities.getGreenwichLongitude(pm, NonSI.DEGREE_ANGLE) != 0)
        {
            final Unit<Angle> oldUnit = formatter.addContextualUnit(angularUnit);
            formatter.indent(1);
            formatter.append(WKTUtilities.toFormattable(pm));
            formatter.indent(-1);
            formatter.newLine();
            formatter.restoreContextualUnit(angularUnit, oldUnit);
        }
        /*
         * Get the coordinate system to format. This will also determine the units to write and the keyword to
         * return in WKT 1 format. Note that for the WKT 1 format, we need to replace the coordinate system by
         * an instance conform to the legacy conventions.
         *
         * We can not delegate the work below to subclasses,  because XML unmarshalling of a geodetic CRS will
         * NOT create an instance of a subclass (because the distinction between geographic and geocentric CRS
         * is not anymore in ISO 19111:2007).
         */
        final boolean isBaseCRS;
        if (isWKT1) {
            if (!isGeographicWKT1) {                        // If not geographic, then presumed geocentric.
                if (cs instanceof CartesianCS) {
                    cs = Legacy.forGeocentricCRS((CartesianCS) cs, true);
                } else {
                    formatter.setInvalidWKT(cs, null);      // SphericalCS was not supported in WKT 1.
                }
            }
            isBaseCRS = false;
        } else {
            isBaseCRS = isBaseCRS(formatter);
        }
        /*
         * Format the coordinate system, except if this CRS is the base CRS of an AbstractDerivedCRS in WKT 2 format.
         * This is because ISO 19162 omits the coordinate system definition of enclosed base CRS in order to simplify
         * the WKT. The 'formatCS(…)' method may write axis unit before or after the axes depending on whether we are
         * formatting WKT version 1 or 2 respectively.
         *
         * Note that even if we do not format the CS, we may still write the units if we are formatting in "simplified"
         * mode (as opposed to the more verbose mode). This looks like the opposite of what we would expect, but this is
         * because formatting the unit here allow us to avoid repeating the unit in projection parameters when this CRS
         * is part of a ProjectedCRS. Note however that in such case, the units to format are the angular units because
         * the linear units will be formatted in the enclosing PROJCS[…] element.
         */
        if (!isBaseCRS || convention == Convention.INTERNAL) {
            formatCS(formatter, cs, ReferencingUtilities.getUnit(cs), isWKT1);    // Will also format the axes unit.
        } else if (convention.isSimplified()) {
            formatter.append(formatter.toContextualUnit(angularUnit));
        }
        /*
         * For WKT 1, the keyword depends on the subclass: "GeogCS" for GeographicCRS or "GeocCS" for GeocentricCRS.
         * However we can not rely on the subclass for choosing the keyword, because after XML unmarhaling we only
         * have a GeodeticCRS. We need to make the choice in this base class. The CS type is a sufficient criterion.
         */
        if (isWKT1) {
            return isGeographicWKT1 ? WKTKeywords.GeogCS : WKTKeywords.GeocCS;
        } else {
            return isBaseCRS ? WKTKeywords.BaseGeodCRS
                   : formatter.shortOrLong(WKTKeywords.GeodCRS, WKTKeywords.GeodeticCRS);
        }
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
    DefaultGeodeticCRS() {
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
    private void setDatum(final GeodeticDatum value) {
        if (datum == null) {
            datum = value;
        } else {
            MetadataUtilities.propertyAlreadySet(DefaultGeodeticCRS.class, "setDatum", "geodeticDatum");
        }
    }

    /**
     * Invoked by JAXB at marshalling time.
     *
     * <div class="note"><b>Implementation note:</b>
     * The usual way to handle {@code <xs:choice>} with JAXB is to annotate a single method like below:
     *
     * {@preformat java
     *     &#64;Override
     *     &#64;XmlElements({
     *       &#64;XmlElement(name = "ellipsoidalCS", type = DefaultEllipsoidalCS.class),
     *       &#64;XmlElement(name = "cartesianCS",   type = DefaultCartesianCS.class),
     *       &#64;XmlElement(name = "sphericalCS",   type = DefaultSphericalCS.class)
     *     })
     *     public CoordinateSystem getCoordinateSystem() {
     *         return super.getCoordinateSystem();
     *     }
     * }
     *
     * However our attempts to apply this approach worked for {@link DefaultEngineeringCRS} but not for this class:
     * for an unknown reason, the unmarshalled CS object is empty. Maybe this is because the covariant return type
     * in the {@link DefaultGeographicCRS} ({@code EllipsoidCS} instead than {@code CoordinateSystem} in above code)
     * is causing confusion.</div>
     *
     * @see <a href="http://issues.apache.org/jira/browse/SIS-166">SIS-166</a>
     */
    @XmlElement(name="ellipsoidalCS") private EllipsoidalCS getEllipsoidalCS() {return getCoordinateSystem(EllipsoidalCS.class);}
    @XmlElement(name="cartesianCS")   private CartesianCS   getCartesianCS()   {return getCoordinateSystem(CartesianCS  .class);}
    @XmlElement(name="sphericalCS")   private SphericalCS   getSphericalCS()   {return getCoordinateSystem(SphericalCS  .class);}

    /**
     * Invoked by JAXB at unmarshalling time.
     *
     * @see #getEllipsoidalCS()
     */
    private void setEllipsoidalCS(final EllipsoidalCS cs) {super.setCoordinateSystem("ellipsoidalCS", cs);}
    private void setCartesianCS  (final CartesianCS   cs) {super.setCoordinateSystem("cartesianCS",   cs);}
    private void setSphericalCS  (final SphericalCS   cs) {super.setCoordinateSystem("sphericalCS",   cs);}
}
