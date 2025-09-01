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
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.internal.Legacy;
import org.apache.sis.referencing.privy.AxisDirections;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.measure.Units;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.referencing.datum.DefaultDatumEnsemble;
import org.apache.sis.pending.geoapi.referencing.MissingMethods;


/**
 * A 2- or 3-dimensional coordinate reference system based on a geodetic reference frame.
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
 */
@XmlType(name = "GeodeticCRSType", propOrder = {
    "ellipsoidalCS",
    "cartesianCS",
    "sphericalCS",
    "datum"
})
@XmlRootElement(name = "GeodeticCRS")
class DefaultGeodeticCRS extends AbstractSingleCRS<GeodeticDatum> implements GeodeticCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1634312292667977126L;

    /**
     * Creates a coordinate reference system from the given properties, datum and coordinate system.
     * The properties given in argument follow the same rules as for the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
     *
     * <p>This constructor is not public because it does not verify the {@code cs} type.</p>
     *
     * @param  properties  the properties to be given to the coordinate reference system.
     * @param  datum       the datum, or {@code null} if the CRS is associated only to a datum ensemble.
     * @param  ensemble    collection of reference frames which for low accuracy requirements may be considered to be
     *                     insignificantly different from each other, or {@code null} if there is no such ensemble.
     * @param  cs          the coordinate system.
     */
    DefaultGeodeticCRS(final Map<String,?> properties,
                       final GeodeticDatum datum,
                       final DefaultDatumEnsemble<GeodeticDatum> ensemble,
                       final CoordinateSystem cs)
    {
        super(properties, GeodeticDatum.class, datum, ensemble, cs);
    }

    /**
     * Creates a new CRS derived from the specified one, but with different axis order or unit.
     * This is for implementing the {@link #createSameType(AbstractCS)} method only.
     */
    DefaultGeodeticCRS(final DefaultGeodeticCRS original, final ReferenceIdentifier id, final AbstractCS cs) {
        super(original, id, cs);
    }

    /**
     * Constructs a new coordinate reference system with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  crs  the coordinate reference system to copy.
     */
    protected DefaultGeodeticCRS(final GeodeticCRS crs) {
        super(crs);
    }

    /**
     * Creates an exception to throw for a coordinate system of illegal class.
     * This is a helper method for sub-classes.
     *
     * @param cs the user-specified coordinate system.
     * @return the exception to throw.
     */
    static IllegalArgumentException illegalCoordinateSystemType(final CoordinateSystem cs) {
        return new IllegalArgumentException(Errors.format(Errors.Keys.IllegalCoordinateSystem_1,
                ReferencingUtilities.getInterface(CoordinateSystem.class, cs)));
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code GeodeticCRS.class}.
     * Subclasses implementing a more specific GeoAPI interface shall override this method.
     *
     * @return the coordinate reference system interface implemented by this class.
     */
    @Override
    public Class<? extends GeodeticCRS> getInterface() {
        return GeodeticCRS.class;
    }

    /**
     * Returns the datum, or {@code null} if the CRS is associated only to a datum ensemble.
     *
     * This method is overridden in subclasses for documentation purpose only, mostly for showing
     * this method in the appropriate position in javadoc (instead of at the bottom of the page).
     * If {@code DefaultGeodeticCRS} is made public in a future SIS version, then we could remove
     * the overridden methods.
     *
     * @return the datum.
     */
    @Override
    @XmlElement(name = "geodeticDatum", required = true)
    public GeodeticDatum getDatum() {
        return super.getDatum();
    }

    /**
     * Initializes the handler for getting datum ensemble of an arbitrary CRS.
     */
    static {
        MissingMethods.geodeticDatumEnsemble = (crs) ->
                (crs instanceof DefaultGeodeticCRS) ? ((DefaultGeodeticCRS) crs).getDatumEnsemble() : null;
    }

    /**
     * Returns the datum or a view of the ensemble as a datum.
     * The {@code legacy} argument tells whether this method is invoked for formatting in a legacy <abbr>WKT</abbr> format.
     */
    @Override
    final GeodeticDatum getDatumOrEnsemble(final boolean legacy) {
        return legacy ? DatumOrEnsemble.asDatum(this) : getDatum();
    }

    /**
     * Returns a coordinate reference system of the same type as this CRS but with different axes.
     * This method shall be overridden by all {@code DefaultGeodeticCRS} subclasses in this package.
     *
     * @param  cs  the coordinate system with new axes.
     * @return new CRS of the same type and datum than this CRS, but with the given axes.
     */
    @Override
    AbstractCRS createSameType(final AbstractCS cs) {
        return new DefaultGeodeticCRS(this, null, cs);
    }

    /**
     * Formats this CRS as a <i>Well Known Text</i> {@code GeodeticCRS[…]} element.
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
                if (AxisDirections.isVertical(cs.getAxis(0).getDirection())) {
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
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final GeodeticDatum datum = getDatumOrEnsemble(true);
        formatter.newLine();
        formatter.append(DefaultGeodeticDatum.castOrCopy(datum));   // For the conversion of ensemble to datum.
        formatter.newLine();
        final Unit<Angle> angularUnit = AxisDirections.getAngularUnit(cs, null);
        DatumOrEnsemble.getPrimeMeridian(this).ifPresent((PrimeMeridian pm) -> {
            if (convention != Convention.WKT2_SIMPLIFIED ||     // Really this specific enum, not Convention.isSimplified().
                    ReferencingUtilities.getGreenwichLongitude(pm, Units.DEGREE) != 0)
            {
                final Unit<Angle> oldUnit = formatter.addContextualUnit(angularUnit);
                formatter.indent(1);
                formatter.append(WKTUtilities.toFormattable(pm));
                formatter.indent(-1);
                formatter.newLine();
                formatter.restoreContextualUnit(angularUnit, oldUnit);
            }
        });
        /*
         * Get the coordinate system to format. This will also determine the units to write and the keyword to
         * return in WKT 1 format. Note that for the WKT 1 format, we need to replace the coordinate system by
         * an instance conform to the legacy conventions.
         *
         * We cannot delegate the work below to subclasses,  because XML unmarshalling of a geodetic CRS will
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
         * the WKT. The `formatCS(…)` method may write axis unit before or after the axes depending on whether we are
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
         * However, we cannot rely on the subclass for choosing the keyword, because after XML unmarhaling we only
         * have a GeodeticCRS. We need to make the choice in this base class. The CS type is a sufficient criterion.
         */
        if (isWKT1) {
            return isGeographicWKT1 ? WKTKeywords.GeogCS : WKTKeywords.GeocCS;
        } else {
            return isBaseCRS ? WKTKeywords.BaseGeodCRS
                   : formatter.shortOrLong(WKTKeywords.GeodCRS, WKTKeywords.GeodeticCRS);
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
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflection.
     */
    DefaultGeodeticCRS() {
        /*
         * The datum and the coordinate system are mandatory for SIS working. We do not verify their presence
         * here because the verification would have to be done in an `afterMarshal(…)` method and throwing an
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
        setDatum("geodeticDatum", value);
    }

    /**
     * Invoked by JAXB at marshalling time.
     *
     * <h4>Implementation note</h4>
     * The usual way to handle {@code <xs:choice>} with JAXB is to annotate a single method like below:
     *
     * {@snippet lang="java" :
     *     @Override
     *     @XmlElements({
     *       @XmlElement(name = "ellipsoidalCS", type = DefaultEllipsoidalCS.class),
     *       @XmlElement(name = "cartesianCS",   type = DefaultCartesianCS.class),
     *       @XmlElement(name = "sphericalCS",   type = DefaultSphericalCS.class)
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
