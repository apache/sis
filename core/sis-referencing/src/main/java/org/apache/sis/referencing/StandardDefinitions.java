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
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.NoSuchIdentifierException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.metadata.AxisNames;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.internal.referencing.provider.PolarStereographicA;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.datum.DefaultVerticalDatum;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.cs.DefaultVerticalCS;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultSphericalCS;
import org.apache.sis.referencing.cs.DefaultEllipsoidalCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.crs.DefaultVerticalCRS;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Units;

import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.ALIAS_KEY;
import static org.opengis.referencing.IdentifiedObject.IDENTIFIERS_KEY;
import static org.opengis.referencing.crs.GeographicCRS.SCOPE_KEY;
import static org.opengis.referencing.datum.Datum.DOMAIN_OF_VALIDITY_KEY;
import static org.apache.sis.internal.metadata.ReferencingServices.AUTHALIC_RADIUS;


/**
 * Definitions of referencing objects identified by the {@link CommonCRS} enumeration values.
 * This class is used only as a fallback if the objects can not be fetched from the EPSG database.
 * This class should not be loaded when a connection to an EPSG geodetic dataset is available.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.4
 * @module
 */
final class StandardDefinitions {
    /**
     * The EPSG code for Greenwich meridian.
     *
     * @see org.apache.sis.internal.util.Constants#EPSG_GREENWICH
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
     * @param  code   the EPSG code, or 0 if none.
     * @param  name   the object name.
     * @param  alias  the alias, or {@code null} if none.
     * @param  world  {@code true} if the properties shall have an entry for the domain of validity.
     * @return the map of properties to give to constructors or factory methods.
     */
    private static Map<String,Object> properties(final int code, final String name, final String alias, final boolean world) {
        final Map<String,Object> map = new HashMap<>(8);
        if (code != 0) {
            map.put(IDENTIFIERS_KEY, new NamedIdentifier(Citations.EPSG, String.valueOf(code)));
        }
        map.put(NAME_KEY, new NamedIdentifier(Citations.EPSG, name));
        map.put(ALIAS_KEY, alias);                                      // May be null, which is okay.
        if (world) {
            map.put(DOMAIN_OF_VALIDITY_KEY, Extents.WORLD);
        }
        return map;
    }

    /**
     * Adds to the given properties an additional identifier in the {@code "CRS"} namespace.
     * This method presumes that the only identifier that existed before this method call was the EPSG one.
     */
    private static void addWMS(final Map<String,Object> properties, final String code) {
        properties.put(IDENTIFIERS_KEY, new NamedIdentifier[] {
            (NamedIdentifier) properties.get(IDENTIFIERS_KEY),
            new NamedIdentifier(Citations.WMS, code)
        });
    }

    /**
     * Creates a Universal Transverse Mercator (UTM) or a Universal Polar Stereographic (UPS) projected CRS
     * using the Apache SIS factory implementation. This method restricts the factory to SIS implementation
     * instead than arbitrary factory in order to meet the contract saying that {@link CommonCRS} methods
     * should never fail.
     *
     * @param code       the EPSG code, or 0 if none.
     * @param baseCRS    the geographic CRS on which the projected CRS is based.
     * @param isUTM      {@code true} for UTM or {@code false} for UPS. Note: redundant with the given latitude.
     * @param latitude   a latitude in the zone of the desired projection, to be snapped to 0°, 90°S or 90°N.
     * @param longitude  a longitude in the zone of the desired projection, to be snapped to UTM central meridian.
     * @param derivedCS  the projected coordinate system.
     */
    static ProjectedCRS createUniversal(final int code, final GeographicCRS baseCRS, final boolean isUTM,
            final double latitude, final double longitude, final CartesianCS derivedCS)
    {
        final OperationMethod method;
        try {
            method = DefaultFactories.forBuildin(MathTransformFactory.class, DefaultMathTransformFactory.class)
                            .getOperationMethod(isUTM ? TransverseMercator.NAME : PolarStereographicA.NAME);
        } catch (NoSuchIdentifierException e) {
            throw new IllegalStateException(e);                     // Should not happen with SIS implementation.
        }
        final ParameterValueGroup parameters = method.getParameters().createValue();
        String name = isUTM ? TransverseMercator.Zoner.UTM.setParameters(parameters, latitude, longitude)
                            : PolarStereographicA.setParameters(parameters, latitude >= 0);
        final DefaultConversion conversion = new DefaultConversion(properties(0, name, null, false), method, null, parameters);

        name = baseCRS.getName().getCode() + " / " + name;
        return new DefaultProjectedCRS(properties(code, name, null, false), baseCRS, conversion, derivedCS);
    }

    /**
     * Creates a geodetic CRS from hard-coded values for the given code.
     *
     * @param  code   the EPSG code.
     * @param  datum  the geodetic datum.
     * @param  cs     the coordinate system.
     * @return the geographic CRS for the given code.
     */
    static GeographicCRS createGeographicCRS(final short code, final GeodeticDatum datum, final EllipsoidalCS cs) {
        final String name;
        String  alias = null;
        String  scope = null;
        boolean world = false;
        switch (code) {
            case 4326: name = "WGS 84"; world = true;           scope = "Horizontal component of 3D system."; break;
            case 4322: name = "WGS 72"; world = true;           scope = "Horizontal component of 3D system."; break;
            case 4258: name = "ETRS89"; alias = "ETRS89-GRS80"; scope = "Horizontal component of 3D system."; break;
            case 4269: name = "NAD83";                          scope = "Geodetic survey.";                   break;
            case 4267: name = "NAD27";                          scope = "Geodetic survey.";                   break;
            case 4230: name = "ED50";                           scope = "Geodetic survey.";                   break;
            case 4047: name = "Unspecified datum based upon the GRS 1980 Authalic Sphere"; world = true;      break;
            default:   throw new AssertionError(code);
        }
        final Map<String, Object> properties = properties(code, name, alias, world);
        properties.put(SCOPE_KEY, scope);
        return new DefaultGeographicCRS(properties, datum, cs);
    }

    /**
     * Creates a geodetic datum from hard-coded values for the given code.
     *
     * @param  code       the EPSG code.
     * @param  ellipsoid  the datum ellipsoid.
     * @param  meridian   the datum prime meridian.
     * @return the geodetic datum for the given code.
     */
    static GeodeticDatum createGeodeticDatum(final short code, final Ellipsoid ellipsoid, final PrimeMeridian meridian) {
        final String name;
        final String alias;
        boolean world = false;
        switch (code) {
            case 6326: name = "World Geodetic System 1984";                        alias = "WGS 84"; world = true; break;
            case 6322: name = "World Geodetic System 1972";                        alias = "WGS 72"; world = true; break;
            case 6258: name = "European Terrestrial Reference System 1989";        alias = "ETRS89";               break;
            case 6269: name = "North American Datum 1983";                         alias = "NAD83";                break;
            case 6267: name = "North American Datum 1927";                         alias = "NAD27";                break;
            case 6230: name = "European Datum 1950";                               alias = "ED50";                 break;
            case 6047: name = "Not specified (based on GRS 1980 Authalic Sphere)"; alias = null;     world = true; break;
            default:   throw new AssertionError(code);
        }
        return new DefaultGeodeticDatum(properties(code, name, alias, world), ellipsoid, meridian);
    }

    /**
     * Creates an ellipsoid from hard-coded values for the given code.
     *
     * @param  code  the EPSG code.
     * @return the ellipsoid for the given code.
     */
    static Ellipsoid createEllipsoid(final short code) {
        String  name;          // No default value
        String  alias          = null;
        double  semiMajorAxis; // No default value
        double  other;         // No default value
        boolean ivfDefinitive  = true;
        Unit<Length> unit      = Units.METRE;
        switch (code) {
            case 7030: name  = "WGS 84";                   alias = "WGS84";        semiMajorAxis = 6378137.0; other = 298.257223563; break;
            case 7043: name  = "WGS 72";                   alias = "NWL 10D";      semiMajorAxis = 6378135.0; other = 298.26;        break;
            case 7019: alias = "International 1979";       name  = "GRS 1980";     semiMajorAxis = 6378137.0; other = 298.257222101; break;
            case 7022: name  = "International 1924";       alias = "Hayford 1909"; semiMajorAxis = 6378388.0; other = 297.0;         break;
            case 7008: name  = "Clarke 1866";              ivfDefinitive = false;  semiMajorAxis = 6378206.4; other = 6356583.8;     break;
            case 7048: name  = "GRS 1980 Authalic Sphere"; ivfDefinitive = false;  semiMajorAxis = other = AUTHALIC_RADIUS;          break;
            default:   throw new AssertionError(code);
        }
        final Map<String,Object> map = properties(code, name, alias, false);
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
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(NAME_KEY, new NamedIdentifier(Citations.EPSG, "Greenwich")); // Name is fixed by ISO 19111.
        properties.put(IDENTIFIERS_KEY, new NamedIdentifier(Citations.EPSG, GREENWICH));
        return new DefaultPrimeMeridian(properties, 0, Units.DEGREE);
    }

    /**
     * Creates a vertical CRS from hard-coded values for the given code.
     *
     * @param  code   the EPSG code.
     * @param  datum  the vertical datum.
     * @return the vertical CRS for the given code.
     */
    static VerticalCRS createVerticalCRS(final short code, final VerticalDatum datum) {
        String cs   = "Vertical CS. Axis: height (H).";   // Default coordinate system
        short  c    = 6499;                               // EPSG code of above coordinate system.
        short  axis = 114;                                // Axis of above coordinate system.
        String wms  = null;
        final  String name, alias;
        switch (code) {
            case 5703: wms   = "88";
                       name  = "NAVD88 height";
                       alias = "North American Vertical Datum of 1988 height (m)";
                       break;
            case 5714: name  = "MSL height";
                       alias = "mean sea level height";
                       break;
            case 5715: name  = "MSL depth";
                       alias = "mean sea level depth";
                       cs    = "Vertical CS. Axis: depth (D).";
                       c     = 6498;
                       axis  = 113;
                       break;
            default:   throw new AssertionError(code);
        }
        final Map<String,Object> properties = properties(code, name, alias, true);
        if (wms != null) {
            addWMS(properties, wms);
        }
        return new DefaultVerticalCRS(properties, datum, new DefaultVerticalCS(properties(c, cs, null, false), createAxis(axis)));
    }

    /**
     * Creates a vertical datum from hard-coded values for the given code.
     *
     * @param  code  the EPSG code.
     * @return the vertical datum for the given code.
     */
    static VerticalDatum createVerticalDatum(final short code) {
        final String name;
        final String alias;
        switch (code) {
            case 5100: name = "Mean Sea Level";                     alias = "MSL";    break;
            case 5103: name = "North American Vertical Datum 1988"; alias = "NAVD88"; break;
            default:   throw new AssertionError(code);
        }
        return new DefaultVerticalDatum(properties(code, name, alias, true), VerticalDatumType.GEOIDAL);
    }

    /**
     * Creates a coordinate system from hard-coded values for the given code.
     * The coordinate system names used by this method contains only the first
     * part of the names declared in the EPSG database.
     *
     * @param  code  the EPSG code.
     * @return the coordinate system for the given code.
     */
    @SuppressWarnings("fallthrough")
    static CoordinateSystem createCoordinateSystem(final short code) {
        final String name;
        int type = 0;                   // 0= Cartesian (default), 1= Spherical, 2= Ellipsoidal
        int dim = 2;                    // Number of dimension, default to 2.
        short axisCode;                 // Code of first axis + dim (or code after the last axis).
        switch (code) {
            case 6422: name = "Ellipsoidal 2D"; type = 2;          axisCode =  108; break;
            case 6423: name = "Ellipsoidal 3D"; type = 2; dim = 3; axisCode =  111; break;
            case 6404: name = "Spherical";      type = 1; dim = 3; axisCode =   63; break;
            case 6500: name = "Earth centred";            dim = 3; axisCode =  118; break;
            case 4400: name = "Cartesian 2D";                      axisCode =    3; break;
            case 1026: name = "Cartesian 2D for UPS north";        axisCode = 1067; break;
            case 1027: name = "Cartesian 2D for UPS south";        axisCode = 1059; break;
            default:   throw new AssertionError(code);
        }
        final Map<String,?> properties = properties(code, name, null, false);
        CoordinateSystemAxis xAxis = null, yAxis = null, zAxis = null;
        switch (dim) {
            default: throw new AssertionError(dim);
            case 3:  zAxis = createAxis(--axisCode);
            case 2:  yAxis = createAxis(--axisCode);
            case 1:  xAxis = createAxis(--axisCode);
            case 0:  break;
        }
        switch (type) {
            default: throw new AssertionError(type);
            case 0:  return (zAxis != null) ? new DefaultCartesianCS  (properties, xAxis, yAxis, zAxis)
                                            : new DefaultCartesianCS  (properties, xAxis, yAxis);
            case 1:  return                   new DefaultSphericalCS  (properties, xAxis, yAxis, zAxis);
            case 2:  return (zAxis != null) ? new DefaultEllipsoidalCS(properties, xAxis, yAxis, zAxis)
                                            : new DefaultEllipsoidalCS(properties, xAxis, yAxis);
        }
    }

    /**
     * Creates an axis from hard-coded values for the given code.
     *
     * @param  code  the EPSG code.
     * @return the coordinate system axis for the given code.
     */
    static CoordinateSystemAxis createAxis(final short code) {
        final String name, abrv;
        Unit<?> unit = Units.METRE;
        double min = Double.NEGATIVE_INFINITY;
        double max = Double.POSITIVE_INFINITY;
        RangeMeaning rm = null;
        final AxisDirection dir;
        switch (code) {
            case 1:    name = "Easting";
                       abrv = "E";
                       dir  = AxisDirection.EAST;
                       break;
            case 2:    name = "Northing";
                       abrv = "N";
                       dir  = AxisDirection.NORTH;
                       break;
            case 60:   name = "Spherical latitude";
                       abrv = "φ′";                         // See HardCodedAxes.SPHERICAL_LATITUDE in tests.
                       unit = Units.DEGREE;
                       dir  = AxisDirection.NORTH;
                       min  = Latitude.MIN_VALUE;
                       max  = Latitude.MAX_VALUE;
                       rm   = RangeMeaning.EXACT;
                       break;
            case 61:   name = "Spherical longitude";
                       abrv = "θ";                          // See HardCodedAxes.SPHERICAL_LONGITUDE in tests.
                       unit = Units.DEGREE;
                       dir  = AxisDirection.EAST;
                       min  = Longitude.MIN_VALUE;
                       max  = Longitude.MAX_VALUE;
                       rm   = RangeMeaning.WRAPAROUND;
                       break;
            case 62:   name = "Geocentric radius";
                       abrv = "R";                          // See HardCodedAxes.GEOCENTRIC_RADIUS in tests.
                       dir  = AxisDirection.UP;
                       rm   = RangeMeaning.EXACT;
                       min  = 0;
                       break;
            case 108:  // Used in Ellipsoidal 3D.
            case 106:  name = AxisNames.GEODETIC_LATITUDE;
                       abrv = "φ";
                       unit = Units.DEGREE;
                       dir  = AxisDirection.NORTH;
                       min  = Latitude.MIN_VALUE;
                       max  = Latitude.MAX_VALUE;
                       rm   = RangeMeaning.EXACT;
                       break;
            case 109:  // Used in Ellipsoidal 3D.
            case 107:  name = AxisNames.GEODETIC_LONGITUDE;
                       abrv = "λ";
                       unit = Units.DEGREE;
                       dir  = AxisDirection.EAST;
                       min  = Longitude.MIN_VALUE;
                       max  = Longitude.MAX_VALUE;
                       rm   = RangeMeaning.WRAPAROUND;
                       break;
            case 110:  name = AxisNames.ELLIPSOIDAL_HEIGHT;
                       abrv = "h";
                       dir  = AxisDirection.UP;
                       break;
            case 114:  name = AxisNames.GRAVITY_RELATED_HEIGHT;
                       abrv = "H";
                       dir  = AxisDirection.UP;
                       break;
            case 113:  name = AxisNames.DEPTH;
                       abrv = "D";
                       dir  = AxisDirection.DOWN;
                       break;
            case 115:  name = AxisNames.GEOCENTRIC_X;
                       abrv = "X";
                       dir  = AxisDirection.GEOCENTRIC_X;
                       break;
            case 116:  name = AxisNames.GEOCENTRIC_Y;
                       abrv = "Y";
                       dir  = AxisDirection.GEOCENTRIC_Y;
                       break;
            case 117:  name = AxisNames.GEOCENTRIC_Z;
                       abrv = "Z";
                       dir  = AxisDirection.GEOCENTRIC_Z;
                       break;
            case 1057: // Actually no axis allocated by EPSG here, but createCoordinateSystem(1027) needs this number.
            case 1056: name = "Easting";
                       abrv = "E";
                       dir  = CoordinateSystems.directionAlongMeridian(AxisDirection.NORTH, 90);
                       break;
            case 1058: name = "Northing";
                       abrv = "N";
                       dir  = CoordinateSystems.directionAlongMeridian(AxisDirection.NORTH, 0);
                       break;
            case 1065: name = "Easting";
                       abrv = "E";
                       dir  = CoordinateSystems.directionAlongMeridian(AxisDirection.SOUTH, 90);
                       break;
            case 1066: name = "Northing";
                       abrv = "N";
                       dir  = CoordinateSystems.directionAlongMeridian(AxisDirection.SOUTH, 180);
                       break;
            default:   throw new AssertionError(code);
        }
        final Map<String,Object> properties = properties(code, name, null, false);
        properties.put(DefaultCoordinateSystemAxis.MINIMUM_VALUE_KEY, min);
        properties.put(DefaultCoordinateSystemAxis.MAXIMUM_VALUE_KEY, max);
        properties.put(DefaultCoordinateSystemAxis.RANGE_MEANING_KEY, rm);
        return new DefaultCoordinateSystemAxis(properties, abrv, dir, unit);
    }
}
