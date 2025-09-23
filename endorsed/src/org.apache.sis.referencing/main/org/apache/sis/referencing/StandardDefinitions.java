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
import java.util.function.Function;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.opengis.util.InternationalString;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
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
import org.opengis.parameter.ParameterValueGroup;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.opengis.referencing.IdentifiedObject.ALIAS_KEY;
import static org.opengis.referencing.IdentifiedObject.REMARKS_KEY;
import static org.opengis.referencing.IdentifiedObject.IDENTIFIERS_KEY;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.metadata.internal.shared.AxisNames;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.provider.Mercator1SP;
import org.apache.sis.referencing.operation.provider.PseudoMercator;
import org.apache.sis.referencing.operation.provider.TransverseMercator;
import org.apache.sis.referencing.operation.provider.PolarStereographicA;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
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
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Units;
import static org.apache.sis.metadata.internal.shared.ReferencingServices.AUTHALIC_RADIUS;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.datum.DatumEnsemble;
import org.opengis.referencing.datum.RealizationMethod;
import static org.opengis.referencing.ObjectDomain.DOMAIN_OF_VALIDITY_KEY;


/**
 * Definitions of referencing objects identified by the {@link CommonCRS} enumeration values.
 * This class is used only as a fallback if the objects cannot be fetched from the EPSG database.
 * This class should not be loaded when a connection to an EPSG geodetic dataset is available.
 *
 * <p>This class uses data available in public sources, with all EPSG metadata omitted except the identifiers.
 * The EPSG identifiers are provided as references where to find the complete definitions.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class StandardDefinitions {
    /**
     * The EPSG database version that most closely match the fallback CRS.
     * We refer to latest version of the 9.x series instead of more recent
     * data because the fallback CRS does not use datum ensemble.
     */
    private static final String VERSION = "9.9.1";

    /**
     * The <abbr>EPSG</abbr> code for Greenwich meridian.
     *
     * @see org.apache.sis.util.internal.shared.Constants#EPSG_GREENWICH
     */
    static final String GREENWICH = "8901";

    /**
     * Notice about the provenance of those data.
     * This is provided as a small clarification because EPSG data should be licensed under EPSG Terms of Use.
     * The approach in this class is to use only the data that are available from public sources,
     * and to add only the EPSG codes as citation references. The notice text is:
     *
     * <blockquote>Definitions from public sources. When a definition corresponds to an EPSG object (ignoring metadata),
     * the EPSG code is provided as a reference where to find the complete definition.</blockquote>
     */
    private static final InternationalString NOTICE =
            Resources.formatInternational(Resources.Keys.FallbackAuthorityNotice);

    /**
     * The authority for this subset of EPSG database.
     */
    static final Citation AUTHORITY;
    static {
        final var c = new DefaultCitation();
        c.setTitle(Vocabulary.formatInternational(Vocabulary.Keys.SubsetOf_1, Constants.EPSG));
        c.setEdition(new SimpleInternationalString(VERSION));
        c.getPresentationForms().add(PresentationForm.DOCUMENT_DIGITAL);
        c.setOtherCitationDetails(NOTICE);
        c.transitionTo(DefaultCitation.State.FINAL);
        AUTHORITY = c;
    }

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
        final var map = new HashMap<String,Object>(8);
        if (code != 0) {
            map.put(IDENTIFIERS_KEY, new NamedIdentifier(AUTHORITY, Constants.EPSG, String.valueOf(code), VERSION, null));
        }
        map.put(NAME_KEY, new NamedIdentifier(AUTHORITY, null, name, null, null));
        if (alias != null) {
            map.put(ALIAS_KEY, alias);
        }
        if (world) {
            map.put(DOMAIN_OF_VALIDITY_KEY, Extents.WORLD);
        }
        map.put(REMARKS_KEY, NOTICE);
        return map;
    }

    /**
     * Adds to the given properties an additional identifier in the {@code "CRS"} namespace.
     * This method presumes that the only identifier that existed before this method call was the EPSG one.
     */
    private static void addWMS(final Map<String,Object> properties, final String code) {
        properties.put(IDENTIFIERS_KEY, new NamedIdentifier[] {
            (NamedIdentifier) properties.get(IDENTIFIERS_KEY),
            new NamedIdentifier(Citations.WMS, Constants.OGC, code, null, null)
        });
    }

    /**
     * Creates a Mercator projection using the Apache <abbr>SIS</abbr> factory implementation.
     *
     * @param  pseudo  whether to create the pseudo-Mercator projection.
     */
    static ProjectedCRS createMercator(final int code, final GeographicCRS baseCRS, final boolean pseudo) {
        return createProjectedCRS(code, baseCRS, defaultCartesianCS(), pseudo ? PseudoMercator.NAME : Mercator1SP.NAME, (parameters) -> {
            return pseudo ? "Pseudo-Mercator" : "World Mercator";
        });
    }

    /**
     * Creates a Universal Transverse Mercator (UTM) or a Universal Polar Stereographic (UPS) projected CRS
     * using the Apache <abbr>SIS</abbr> factory implementation.
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
        return createProjectedCRS(code, baseCRS, derivedCS, isUTM ? TransverseMercator.NAME : PolarStereographicA.NAME, (parameters) -> {
            return isUTM ? TransverseMercator.Zoner.UTM.setParameters(parameters, latitude, longitude)
                         : PolarStereographicA.setParameters(parameters, latitude >= 0);
        });
    }

    /**
     * Creates a projected CRS from hard-coded values for the given code.
     * This method restricts the factory to the <abbr>SIS</abbr> implementation instead of arbitrary factory
     * in order to met the contract saying that {@link CommonCRS} methods should never fail.
     *
     * @param code        the EPSG code, or 0 if none.
     * @param baseCRS     the geographic CRS on which the projected CRS is based.
     * @param derivedCS   the projected coordinate system.
     * @param methodName  name of the operation method to apply.
     * @param setup       a function which setup the parameter values and returns the name of the conversion.
     */
    private static ProjectedCRS createProjectedCRS(final int code, final GeographicCRS baseCRS, final CartesianCS derivedCS,
            final String methodName, final Function<ParameterValueGroup, String> setup)
    {
        final OperationMethod method;
        try {
            method = DefaultMathTransformFactory.provider().getOperationMethod(methodName);
        } catch (NoSuchIdentifierException e) {
            throw new IllegalStateException(e);                     // Should not happen with SIS implementation.
        }
        final ParameterValueGroup parameters = method.getParameters().createValue();
        String name = setup.apply(parameters);
        final var conversion = new DefaultConversion(properties(0, name, null, false), method, null, parameters);
        name = baseCRS.getName().getCode() + " / " + name;
        return new DefaultProjectedCRS(properties(code, name, null, false), baseCRS, conversion, derivedCS);
    }

    /**
     * Creates a geodetic <abbr>CRS</abbr> from hard-coded values for the given code.
     *
     * @param  code      the EPSG code.
     * @param  datum     the geodetic reference frame, or {@code null} if a datum ensemble is used instead.
     * @param  ensemble  the datum ensemble, or {@code null} if none.
     * @param  cs        the coordinate system.
     * @return the geographic <abbr>CRS</abbr> for the given code.
     */
    static GeographicCRS createGeographicCRS(final short code, final GeodeticDatum datum,
            final DatumEnsemble<GeodeticDatum> ensemble, final EllipsoidalCS cs)
    {
        final String name;
        boolean world = false;
        switch (code) {
            case 4326: name = "WGS 84"; world = true; break;
            case 4322: name = "WGS 72"; world = true; break;
            case 4258: name = "ETRS89"; break;
            case 4269: name = "NAD83";  break;
            case 4267: name = "NAD27";  break;
            case 4230: name = "ED50";   break;
            case 4019: name = "Unknown datum based upon the GRS 1980 ellipsoid";           world = true; break;
            case 4047: name = "Unspecified datum based upon the GRS 1980 Authalic Sphere"; world = true; break;
            default:   throw new AssertionError(code);
        }
        final Map<String, Object> properties = properties(code, name, null, world);
        return new DefaultGeographicCRS(properties, datum, ensemble, cs);
    }

    /**
     * Creates a geodetic reference frame from hard-coded values for the given code.
     *
     * @param  code       the EPSG code.
     * @param  ellipsoid  the datum ellipsoid.
     * @param  meridian   the datum prime meridian.
     * @return the geodetic reference frame for the given code.
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
            case 6019: name = "Not specified (based on GRS 1980 ellipsoid)";       alias = null;     world = true; break;
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
            case 7030: name  = "WGS 1984";                 semiMajorAxis = 6378137.0; other = 298.257223563; break;
            case 7043: name  = "WGS 1972";                 semiMajorAxis = 6378135.0; other = 298.26;        break;
            case 7019: name  = "GRS 1980";                 semiMajorAxis = 6378137.0; other = 298.257222101; break;
            case 7022: name  = "International 1924";       semiMajorAxis = 6378388.0; other = 297.0;         break;
            case 7008: name  = "Clarke 1866";              semiMajorAxis = 6378206.4; other = 6356583.8; ivfDefinitive = false; break;
            case 7048: name  = "GRS 1980 Authalic Sphere"; semiMajorAxis = other = AUTHALIC_RADIUS; ivfDefinitive = false; break;
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
     * If another prime meridian is desired, the EPSG database shall be used.
     */
    static PrimeMeridian primeMeridian() {
        final var properties = new HashMap<String,Object>(4);
        properties.put(NAME_KEY, new NamedIdentifier(AUTHORITY, "Greenwich"));          // Name is fixed by ISO 19111.
        properties.put(IDENTIFIERS_KEY, new NamedIdentifier(AUTHORITY, Constants.EPSG, GREENWICH, VERSION, null));
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
        String csName = "Vertical CS. Axis: height (H).";   // Default coordinate system
        short  csCode = 6499;                               // EPSG code of above coordinate system.
        short  axis   = 114;                                // Axis of above coordinate system.
        String wms    = null;
        final  String name, alias;
        switch (code) {
            case 5703: wms    = "88";
                       name   = "NAVD88 height";
                       alias  = "North American Vertical Datum 1988 height";
                       break;
            case 5714: name   = "MSL height";
                       alias  = "Mean Sea Level height";
                       break;
            case 5715: name   = "MSL depth";
                       alias  = "Mean Sea Level depth";
                       csName = "Vertical CS. Axis: depth (D).";
                       csCode = 6498;
                       axis   = 113;
                       break;
            default:   throw new AssertionError(code);
        }
        Map<String,Object> properties = properties(csCode, csName, null, false);
        final var cs = new DefaultVerticalCS(properties, createAxis(axis, true));
        properties = properties(code, name, alias, true);
        if (wms != null) {
            addWMS(properties, wms);
        }
        return new DefaultVerticalCRS(properties, datum, null, cs);
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
        final RealizationMethod method;
        switch (code) {
            case 5100: {
                name   = "Mean Sea Level";
                alias  = "MSL";
                method = RealizationMethod.TIDAL;
                break;
            }
            case 5103: {
                name   = "North American Vertical Datum 1988";
                alias  = "NAVD88";
                method = RealizationMethod.LEVELLING;
                break;
            }
            default: throw new AssertionError(code);
        }
        return new DefaultVerticalDatum(properties(code, name, alias, true), method);
    }

    /**
     * EPSG codes of coordinate systems supported by this class. We provide constants only for
     * coordinate systems because those codes appear directly in method bodies, contrarily to
     * other kinds of object where the code are stored in {@link CommonCRS} fields.
     */
    static final short ELLIPSOIDAL_2D = (short) 6422,
                       ELLIPSOIDAL_3D = (short) 6423,
                       SPHERICAL      = (short) 6404,
                       EARTH_CENTRED  = (short) 6500,
                       CARTESIAN_2D   = (short) 4400,
                       UPS_NORTH      = (short) 1026,
                       UPS_SOUTH      = (short) 1027;

    /**
     * The default coordinate system for projected <abbr>CRS</abbr>, created when first requested.
     */
    private static CartesianCS DEFAULT_CS;

    /**
     * Returns the default coordinate system for projected <abbr>CRS</abbr>.
     */
    static synchronized CartesianCS defaultCartesianCS() {
        if (DEFAULT_CS == null) {
            DEFAULT_CS = (CartesianCS) createCoordinateSystem(CARTESIAN_2D, true);
        }
        return DEFAULT_CS;
    }

    /**
     * Creates a coordinate system from hard-coded values for the given code.
     * The coordinate system names used by this method contains only the first
     * part of the names declared in the EPSG database.
     *
     * @param  code       the EPSG code.
     * @param  mandatory  whether to fail or return {@code null} if the given code is unknown.
     * @return the coordinate system for the given code.
     */
    @SuppressWarnings("fallthrough")
    static CoordinateSystem createCoordinateSystem(final short code, final boolean mandatory) {
        final String name;
        int type = 0;                   // 0= Cartesian (default), 1= Spherical, 2= Ellipsoidal
        int dim = 2;                    // Number of dimension, default to 2.
        short axisCode;                 // Code of first axis + dim (or code after the last axis).
        switch (code) {
            case ELLIPSOIDAL_2D: name = "Ellipsoidal 2D";  type = 2;          axisCode =  108; break;
            case ELLIPSOIDAL_3D: name = "Ellipsoidal 3D";  type = 2; dim = 3; axisCode =  111; break;
            case SPHERICAL:      name = "Spherical";       type = 1; dim = 3; axisCode =   63; break;
            case EARTH_CENTRED:  name = "Cartesian 3D (geocentric)"; dim = 3; axisCode =  118; break;
            case CARTESIAN_2D:   name = "Cartesian 2D";                       axisCode =    3; break;
            case UPS_NORTH:      name = "Cartesian 2D for UPS north";         axisCode = 1067; break;
            case UPS_SOUTH:      name = "Cartesian 2D for UPS south";         axisCode = 1059; break;
            default:   if (!mandatory) return null;
                       throw new AssertionError(code);
        }
        final Map<String,?> properties = properties(code, name, null, false);
        CoordinateSystemAxis xAxis = null, yAxis = null, zAxis = null;
        switch (dim) {
            default: throw new AssertionError(dim);
            case 3:  zAxis = createAxis(--axisCode, true);
            case 2:  yAxis = createAxis(--axisCode, true);
            case 1:  xAxis = createAxis(--axisCode, true);
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
     * @param  code       the EPSG code.
     * @param  mandatory  whether to fail or return {@code null} if the given code is unknown.
     * @return the coordinate system axis for the given code.
     */
    static CoordinateSystemAxis createAxis(final short code, final boolean mandatory) {
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
                       abrv = "Ω";                          // See HardCodedAxes.SPHERICAL_LATITUDE in tests.
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
                       abrv = "r";                          // See HardCodedAxes.GEOCENTRIC_RADIUS in tests.
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
            default:   if (!mandatory) return null;
                       throw new AssertionError(code);
        }
        final Map<String,Object> properties = properties(code, name, null, false);
        properties.put(DefaultCoordinateSystemAxis.MINIMUM_VALUE_KEY, min);
        properties.put(DefaultCoordinateSystemAxis.MAXIMUM_VALUE_KEY, max);
        properties.put(DefaultCoordinateSystemAxis.RANGE_MEANING_KEY, rm);
        return new DefaultCoordinateSystemAxis(properties, abrv, dir, unit);
    }
}
