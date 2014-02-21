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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.apache.sis.internal.referencing.Legacy;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.io.wkt.Formatter;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.apache.sis.internal.referencing.WKTUtilities.toFormattable;


/**
 * A 2D or 3D coordinate reference system based on a geodetic datum.
 * The CRS is geographic if associated with an ellipsoidal coordinate system,
 * or geocentric if associated with a spherical or Cartesian coordinate system.
 *
 * <p><b>Used with coordinate system types:</b>
 *   {@linkplain org.apache.sis.referencing.cs.DefaultCartesianCS Cartesian},
 *   {@linkplain org.apache.sis.referencing.cs.DefaultSphericalCS Spherical} or
 *   {@linkplain org.apache.sis.referencing.cs.DefaultEllipsoidalCS Ellipsoidal}.
 * </p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
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
     */
    @XmlElement(name = "geodeticDatum")
    private final GeodeticDatum datum;

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    DefaultGeodeticCRS() {
        datum = null;
    }

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
     * This method is overridden is subclasses for documentation purpose only, mostly for showing this method in
     * the appropriate position in javadoc (instead than at the bottom of the page). If {@code DefaultGeodeticCRS}
     * is made public in a future SIS version, then we should make this method final and remove the overridden methods.
     *
     * @return The datum.
     */
    @Override
    public GeodeticDatum getDatum() {
        return datum;
    }

    /**
     * Invoked by JAXB at marshalling time.
     */
    @XmlElement(name="cartesianCS")   private CartesianCS   getCartesianCS()   {return getCoordinateSystem(CartesianCS  .class);}
    @XmlElement(name="sphericalCS")   private SphericalCS   getSphericalCS()   {return getCoordinateSystem(SphericalCS  .class);}
    @XmlElement(name="ellipsoidalCS") private EllipsoidalCS getEllipsoidalCS() {return getCoordinateSystem(EllipsoidalCS.class);}

    /**
     * Invoked by JAXB at unmarshalling time.
     */
    private void setCartesianCS  (final CartesianCS   cs) {super.setCoordinateSystem("cartesianCS",   cs);}
    private void setSphericalCS  (final SphericalCS   cs) {super.setCoordinateSystem("sphericalCS",   cs);}
    private void setEllipsoidalCS(final EllipsoidalCS cs) {super.setCoordinateSystem("ellipsoidalCS", cs);}

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
     *
     * @return {@code "GeodeticCRS"} (WKT 2) or {@code "GeogCS"}/{@code "GeocCS"} (WKT 1).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        WKTUtilities.appendName(this, formatter, null);
        final boolean isWKT1  = formatter.getConvention().majorVersion() == 1;
        final Unit<?> unit    = getUnit();
        final Unit<?> oldUnit = formatter.addContextualUnit(unit);
        formatter.newLine();
        formatter.append(toFormattable(datum));
        formatter.newLine();
        formatter.indent(isWKT1 ? 0 : +1);
        formatter.append(toFormattable(datum.getPrimeMeridian()));
        formatter.indent(isWKT1 ? 0 : -1);
        formatter.newLine();
        CoordinateSystem cs = super.getCoordinateSystem();
        if (isWKT1) { // WKT 1 writes unit before axes, while WKT 2 writes them after axes.
            formatter.append(unit);
            if (unit == null) {
                formatter.setInvalidWKT(this, null);
            }
            /*
             * Replaces the given coordinate system by an instance conform to the conventions used in WKT 1.
             * Note that we can not delegate this task to subclasses, because XML unmarshalling of a geodetic
             * CRS will NOT create an instance of a subclass (because the distinction between geographic and
             * geocentric CRS is not anymore in ISO 19111:2007).
             */
            if (!(cs instanceof EllipsoidalCS)) { // Tested first because this is the most common case.
                if (cs instanceof CartesianCS) {
                    cs = Legacy.forGeocentricCRS((CartesianCS) cs, true);
                } else {
                    formatter.setInvalidWKT(cs, null);
                }
            }
        } else {
            formatter.append(toFormattable(cs)); // The concept of CoordinateSystem was not explicit in WKT 1.
            formatter.indent(+1);
        }
        final int dimension = cs.getDimension();
        for (int i=0; i<dimension; i++) {
            formatter.newLine();
            formatter.append(toFormattable(cs.getAxis(i)));
        }
        if (!isWKT1) { // WKT 2 writes unit after axes, while WKT 1 wrote them before axes.
            formatter.newLine();
            formatter.append(unit);
            formatter.indent(-1);
        }
        formatter.removeContextualUnit(unit);
        formatter.addContextualUnit(oldUnit);
        formatter.newLine(); // For writing the ID[…] element on its own line.
        if (!isWKT1) {
            return "GeodeticCRS";
        }
        /*
         * For WKT1, the keyword depends on the subclass: "GeogCS" for GeographicCRS,
         * or 'GeocCS" for GeocentricCRS. However we can not rely on the subclass for
         * choosing the keyword, because in some situations (after XML unmarhaling)
         * we only have a GeodeticCRS. We need to make the choice here. The CS type
         * is a sufficient criterion.
         */
        return (cs instanceof EllipsoidalCS) ? "GeogCS" : "GeocCS";
    }
}
