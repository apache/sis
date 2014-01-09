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
package org.apache.sis.referencing;

import java.util.Map;
import java.util.HashMap;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Length;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.cs.DefaultEllipsoidalCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Latitude;

import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.ALIAS_KEY;
import static org.opengis.referencing.IdentifiedObject.IDENTIFIERS_KEY;
import static org.apache.sis.internal.metadata.ReferencingServices.AUTHALIC_RADIUS;


/**
 * Definitions of referencing objects identified by the {@link GeodeticObjects} enumeration values.
 * This class is used only as a fallback if the objects can not be fetched from the EPSG database.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 */
final class StandardDefinitions {
    /**
     * The EPSG code for Greenwich meridian.
     */
    static final String GREENWICH = "8901";

    /**
     * Do not allow instantiation of this class.
     */
    private StandardDefinitions() {
    }

    /**
     * Returns a map of properties for the given EPSG code, name and alias.
     *
     * @param  code  The EPSG code.
     * @param  name  The object name.
     * @param  alias The alias, or {@code null} if none.
     * @return The map of properties to give to constructors or factory methods.
     */
    private static Map<String,Object> properties(final short code, final String name, final String alias) {
        final Map<String,Object> map = new HashMap<String,Object>(8);
        map.put(IDENTIFIERS_KEY, new NamedIdentifier(Citations.EPSG, String.valueOf(code)));
        map.put(NAME_KEY, new NamedIdentifier(Citations.EPSG, name));
        map.put(ALIAS_KEY, alias); // May be null, which is okay.
        return map;
    }

    /**
     * Creates a geodetic CRS from hard-coded values for the given code.
     *
     * @param  code  The EPSG code.
     * @param  datum The geodetic datum.
     * @param  cs    The coordinate system.
     * @return The geographic CRS for the given code.
     */
    static GeographicCRS createGeographicCRS(final short code, final GeodeticDatum datum, final EllipsoidalCS cs) {
        final String name;
        String alias = null;
        switch (code) {
            case 4326: name = "WGS 84"; break;
            case 4322: name = "WGS 72"; break;
            case 4258: name = "ETRS89"; alias = "ETRS89-GRS80"; break;
            case 4269: name = "NAD83"; break;
            case 4267: name = "NAD27"; break;
            case 4230: name = "ED50"; break;
            case 4047: name = "Unspecified datum based upon the GRS 1980 Authalic Sphere"; break;
            default:   throw new AssertionError(code);
        }
        return new DefaultGeographicCRS(properties(code, name, alias), datum, cs);
    }

    /**
     * Creates a geodetic datum from hard-coded values for the given code.
     *
     * @param  code      The EPSG code.
     * @param  ellipsoid The datum ellipsoid.
     * @param  meridian  The datum prime meridian.
     * @return The geodetic datum for the given code.
     */
    static GeodeticDatum createGeodeticDatum(final short code, final Ellipsoid ellipsoid, final PrimeMeridian meridian) {
        final String name;
        final String alias;
        switch (code) {
            case 6326: name = "World Geodetic System 1984";                        alias = "WGS 84"; break;
            case 6322: name = "World Geodetic System 1972";                        alias = "WGS 72"; break;
            case 6258: name = "European Terrestrial Reference System 1989";        alias = "ETRS89"; break;
            case 6269: name = "North American Datum 1983";                         alias = "NAD83";  break;
            case 6267: name = "North American Datum 1927";                         alias = "NAD27";  break;
            case 6230: name = "European Datum 1950";                               alias = "ED50";   break;
            case 6047: name = "Not specified (based on GRS 1980 Authalic Sphere)"; alias = null;     break;
            default:   throw new AssertionError(code);
        }
        return new DefaultGeodeticDatum(properties(code, name, alias), ellipsoid, meridian);
    }

    /**
     * Creates an ellipsoid from hard-coded values for the given code.
     *
     * @param  code The EPSG code.
     * @return The ellipsoid for the given code.
     */
    static Ellipsoid createEllipsoid(final short code) {
        String  name;          // No default value
        String  alias          = null;
        double  semiMajorAxis; // No default value
        double  other;         // No default value
        boolean ivfDefinitive  = true;
        Unit<Length> unit      = SI.METRE;
        switch (code) {
            case 7030: name  = "WGS 84";                   alias = "WGS84";        semiMajorAxis = 6378137.0; other = 298.257223563; break;
            case 7043: name  = "WGS 72";                   alias = "NWL 10D";      semiMajorAxis = 6378135.0; other = 298.26;        break;
            case 7019: alias = "International 1979";       name  = "GRS 1980";     semiMajorAxis = 6378137.0; other = 298.257222101; break;
            case 7022: name  = "International 1924";       alias = "Hayford 1909"; semiMajorAxis = 6378388.0; other = 297.0;         break;
            case 7008: name  = "Clarke 1866";              ivfDefinitive = false;  semiMajorAxis = 6378206.4; other = 6356583.8;     break;
            case 7048: name  = "GRS 1980 Authalic Sphere"; ivfDefinitive = false;  semiMajorAxis = other = AUTHALIC_RADIUS;          break;
            default:   throw new AssertionError(code);
        }
        final Map<String,Object> map = properties(code, name, alias);
        if (ivfDefinitive) {
            return DefaultEllipsoid.createFlattenedSphere(map, semiMajorAxis, other, unit);
        } else {
            return DefaultEllipsoid.createEllipsoid(map, semiMajorAxis, other, unit);
        }
    }

    /**
     * Creates the Greenwich prime meridian. This is the only prime meridian supported by SIS convenience shortcuts.
     * If an other prime meridian is desired, the EPSG database shall be used.
     */
    static PrimeMeridian primeMeridian() {
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        properties.put(NAME_KEY, new NamedIdentifier(Citations.EPSG, "Greenwich")); // Name is fixed by ISO 19111.
        properties.put(IDENTIFIERS_KEY, new NamedIdentifier(Citations.EPSG, GREENWICH));
        return new DefaultPrimeMeridian(properties, 0, NonSI.DEGREE_ANGLE);
    }

    /**
     * Creates a coordinate system from hard-coded values for the given code.
     * The coordinate system names used by this method contains only the first
     * part of the names declared in the EPSG database.
     *
     * @param  code The EPSG code.
     * @return The coordinate system for the given code.
     */
    static CoordinateSystem createCoordinateSystem(final short code) {
        final String name;
        short xc, yc, zc = 0; // Not necessarily (long, lat) order.
        switch (code) {
            case 6422: name = "Ellipsoidal 2D"; xc = 106; yc = 107;           break;
            case 6423: name = "Ellipsoidal 3D"; xc = 108; yc = 109; zc = 110; break;
            default:   throw new AssertionError(code);
        }
        final Map<String,?> properties = properties(code, name, null);
        final CoordinateSystemAxis xAxis = createAxis(xc);
        final CoordinateSystemAxis yAxis = createAxis(yc);
        if (zc != 0) {
            final CoordinateSystemAxis zAxis = createAxis(zc);
            return new DefaultEllipsoidalCS(properties, xAxis, yAxis, zAxis);
        }
        return new DefaultEllipsoidalCS(properties, xAxis, yAxis);
    }

    /**
     * Creates an axis from hard-coded values for the given code.
     *
     * @param  code The EPSG code.
     * @return The coordinate system axis for the given code.
     */
    private static CoordinateSystemAxis createAxis(final short code) {
        final String name, abrv;
        final Unit<?> unit;
        final double min, max;
        final RangeMeaning rm;
        final AxisDirection dir;
        switch (code) {
            case 106:
            case 108:  name = "Geodetic latitude";
                       abrv = "φ";
                       unit = NonSI.DEGREE_ANGLE;
                       dir  = AxisDirection.NORTH;
                       min  = Latitude.MIN_VALUE;
                       max  = Latitude.MAX_VALUE;
                       rm   = RangeMeaning.EXACT;
                       break;
            case 107:
            case 109:  name = "Geodetic longitude";
                       abrv = "λ";
                       unit = NonSI.DEGREE_ANGLE;
                       dir  = AxisDirection.EAST;
                       min  = Longitude.MIN_VALUE;
                       max  = Longitude.MAX_VALUE;
                       rm   = RangeMeaning.WRAPAROUND;
                       break;
            case 110:  name = "llipsoidal height ";
                       abrv = "h";
                       unit = SI.METRE;
                       dir  = AxisDirection.UP;
                       min  = Double.NEGATIVE_INFINITY;
                       max  = Double.POSITIVE_INFINITY;
                       rm   = null;
                       break;
            default:   throw new AssertionError(code);
        }
        return new DefaultCoordinateSystemAxis(properties(code, name, null), abrv, dir, unit, min, max, rm);
    }
}
