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
package org.apache.sis.io.wkt;

import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.RealizationMethod;
import org.apache.sis.metadata.internal.shared.AxisNames;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.system.Loggers;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opengis.test.referencing.WKTParserTest;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.FailureDetailsReporter;
import org.apache.sis.test.TestCase;


/**
 * Tests Well-Known Text parser using the tests defined in GeoAPI. Those tests use the
 * {@link org.apache.sis.referencing.factory.GeodeticObjectFactory#createFromWKT(String)} method.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings("exports")
@ExtendWith(FailureDetailsReporter.class)
public final class CRSParserTest extends WKTParserTest {
    /**
     * Forces the check of whether the EPSG database exists before to start any test.
     * This is done for avoiding race conditions logging the same message many times.
     */
    static {
        // Will do nothing, the intent is only to force class initialization.
        TestCase.out.flush();
    }

    /**
     * A JUnit extension for listening to log events.
     */
    @RegisterExtension
    public final LoggingWatcher loggings;

    /**
     * Whether the test should replace the curly quotation marks “ and ” by the straight quotation mark ".
     * The ISO 19162 specification uses only straight quotation marks, but SIS supports both.
     * Curly quotation marks are convenient for identifying bugs, so we test them first.
     */
    private boolean useStraightQuotes;

    /**
     * Creates a new test case using the default {@code CRSFactory} implementation.
     */
    public CRSParserTest() {
        super(GeodeticObjectFactory.provider());
        loggings = new LoggingWatcher(Loggers.WKT);
    }

    /**
     * Pre-processes the <abbr>WKT</abbr> string before parsing. This method may replace
     * curly quotation marks ({@code “} and {@code ”}) by straight quotation marks ({@code "}).
     * The Apache SIS parser should understand both forms transparently.
     *
     * @param  wkt  the Well-Known Text to pre-process.
     * @return the Well-Known Text to parse.
     */
    @Override
    protected String preprocessWKT(String wkt) {
        if (useStraightQuotes) {
            wkt = super.preprocessWKT(wkt);
        }
        return wkt;
    }

    /**
     * Verifies the axis names of a geographic CRS. This method is invoked when the parsed object is
     * expected to have <q>Geodetic latitude</q> and <q>Geodetic longitude</q> names.
     */
    @SuppressWarnings("fallthrough")
    private void verifyEllipsoidalCS() {
        final CoordinateSystem cs = object.getCoordinateSystem();
        switch (cs.getDimension()) {
            default: assertEquals(AxisNames.ELLIPSOIDAL_HEIGHT, cs.getAxis(2).getName().getCode(), "name");
            case 2:  assertEquals(AxisNames.GEODETIC_LONGITUDE, cs.getAxis(1).getName().getCode(), "name");
            case 1:  assertEquals(AxisNames.GEODETIC_LATITUDE,  cs.getAxis(0).getName().getCode(), "name");
            case 0:  break;
        }
        switch (cs.getDimension()) {
            default: assertEquals("h", cs.getAxis(2).getAbbreviation(), "abbreviation");
            case 2:  assertEquals("λ", cs.getAxis(1).getAbbreviation(), "abbreviation");
            case 1:  assertEquals("φ", cs.getAxis(0).getAbbreviation(), "abbreviation");
            case 0:  break;
        }
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  GEODCRS[“WGS 84”,
     *    DATUM[“World Geodetic System 1984”,
     *      ELLIPSOID[“WGS 84”, 6378137, 298.257223563,
     *        LENGTHUNIT[“metre”,1.0]]],
     *    CS[ellipsoidal,3],
     *      AXIS[“(lat)”,north,ANGLEUNIT[“degree”,0.0174532925199433]],
     *      AXIS[“(lon)”,east,ANGLEUNIT[“degree”,0.0174532925199433]],
     *      AXIS[“ellipsoidal height (h)”,up,LENGTHUNIT[“metre”,1.0]]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographic3D() throws FactoryException {
        super.testGeographic3D();
        verifyEllipsoidalCS();
        useStraightQuotes = true;
        super.testGeographic3D();                           // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  GEODCRS[“S-95”,
     *    DATUM[“Pulkovo 1995”,
     *      ELLIPSOID[“Krassowsky 1940”, 6378245, 298.3,
     *        LENGTHUNIT[“metre”,1.0]]],
     *    CS[ellipsoidal,2],
     *      AXIS[“latitude”,north,ORDER[1]],
     *      AXIS[“longitude”,east,ORDER[2]],
     *      ANGLEUNIT[“degree”,0.0174532925199433],
     *    REMARK[“Система Геодеэических Координвт года 1995(СК-95)”]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographicWithUnicode() throws FactoryException {
        super.testGeographicWithUnicode();
        verifyEllipsoidalCS();
        useStraightQuotes = true;
        super.testGeographicWithUnicode();                  // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  GEODCRS[“NAD83”,
     *    DATUM[“North American Datum 1983”,
     *      ELLIPSOID[“GRS 1980”, 6378137, 298.257222101, LENGTHUNIT[“metre”,1.0]]],
     *    CS[ellipsoidal,2],
     *      AXIS[“latitude”,north],
     *      AXIS[“longitude”,east],
     *      ANGLEUNIT[“degree”,0.017453292519943],
     *    ID[“EPSG”,4269],
     *    REMARK[“1986 realisation”]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographicWithIdentifier() throws FactoryException {
        super.testGeographicWithIdentifier();
        verifyEllipsoidalCS();
        useStraightQuotes = true;
        super.testGeographicWithIdentifier();               // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  GEODCRS[“NTF (Paris)”,
     *    DATUM[“Nouvelle Triangulation Francaise”,
     *      ELLIPSOID[“Clarke 1880 (IGN)”, 6378249.2, 293.4660213]],
     *    PRIMEM[“Paris”,2.5969213],
     *    CS[ellipsoidal,2],
     *      AXIS[“latitude”,north,ORDER[1]],
     *      AXIS[“longitude”,east,ORDER[2]],
     *      ANGLEUNIT[“grad”,0.015707963267949],
     *    REMARK[“Nouvelle Triangulation Française”]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographicWithGradUnits() throws FactoryException {
        super.testGeographicWithGradUnits();
        verifyEllipsoidalCS();
        useStraightQuotes = true;
        super.testGeographicWithGradUnits();                // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  GEODETICCRS[“JGD2000”,
     *    DATUM[“Japanese Geodetic Datum 2000”,
     *      ELLIPSOID[“GRS 1980”, 6378137, 298.257222101]],
     *    CS[Cartesian,3],
     *      AXIS[“(X)”,geocentricX],
     *      AXIS[“(Y)”,geocentricY],
     *      AXIS[“(Z)”,geocentricZ],
     *      LENGTHUNIT[“metre”,1.0],
     *    SCOPE[“Geodesy, topographic mapping and cadastre”],
     *    AREA[“Japan”],
     *    BBOX[17.09,122.38,46.05,157.64],
     *    TIMEEXTENT[2002-04-01,2011-10-21],
     *    ID[“EPSG”,4946,URI[“urn:ogc:def:crs:EPSG::4946”]],
     *    REMARK[“注：JGD2000ジオセントリックは現在JGD2011に代わりました。”]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeocentric() throws FactoryException {
        super.testGeocentric();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals(AxisNames.GEOCENTRIC_X, cs.getAxis(0).getName().getCode(), "name");
        assertEquals(AxisNames.GEOCENTRIC_Y, cs.getAxis(1).getName().getCode(), "name");
        assertEquals(AxisNames.GEOCENTRIC_Z, cs.getAxis(2).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testGeocentric();                             // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Ignored for now, because the Lambert Azimuthal Equal Area projection method is not yet implemented.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    @Disabled("Lambert Azimuthal Equal Area projection method not yet implemented.")
    public void testProjectedYX() throws FactoryException {
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  PROJCRS[“NAD27 / Texas South Central”,
     *    BASEGEODCRS[“NAD27”,
     *      DATUM[“North American Datum 1927”,
     *        ELLIPSOID[“Clarke 1866”, 20925832.164, 294.97869821,
     *          LENGTHUNIT[“US survey foot”,0.304800609601219]]]],
     *    CONVERSION[“Texas South Central SPCS27”,
     *      METHOD[“Lambert Conic Conformal (2SP)”,ID[“EPSG”,9802]],
     *      PARAMETER[“Latitude of false origin”,27.83333333333333,
     *        ANGLEUNIT[“degree”,0.0174532925199433],ID[“EPSG”,8821]],
     *      PARAMETER[“Longitude of false origin”,-99.0,
     *        ANGLEUNIT[“degree”,0.0174532925199433],ID[“EPSG”,8822]],
     *      PARAMETER[“Latitude of 1st standard parallel”,28.383333333333,
     *        ANGLEUNIT[“degree”,0.0174532925199433],ID[“EPSG”,8823]],
     *      PARAMETER[“Latitude of 2nd standard parallel”,30.283333333333,
     *        ANGLEUNIT[“degree”,0.0174532925199433],ID[“EPSG”,8824]],
     *      PARAMETER[“Easting at false origin”,2000000.0,
     *        LENGTHUNIT[“US survey foot”,0.304800609601219],ID[“EPSG”,8826]],
     *      PARAMETER[“Northing at false origin”,0.0,
     *        LENGTHUNIT[“US survey foot”,0.304800609601219],ID[“EPSG”,8827]]],
     *    CS[Cartesian,2],
     *      AXIS[“(x)”,east],
     *      AXIS[“(y)”,north],
     *      LENGTHUNIT[“US survey foot”,0.304800609601219],
     *    REMARK[“Fundamental point: Meade’s Ranch KS, latitude 39°13'26.686"N, longitude 98°32'30.506"W.”]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testProjectedWithFootUnits() throws FactoryException {
        super.testProjectedWithFootUnits();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals(AxisNames.EASTING,  cs.getAxis(0).getName().getCode(), "name");
        assertEquals(AxisNames.NORTHING, cs.getAxis(1).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testProjectedWithFootUnits();                  // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters and the line feed in {@code REMARK}):
     *
     * <blockquote><pre>PROJCRS[“NAD83 UTM 10”,
     *  BASEGEODCRS[“NAD83(86)”,
     *    DATUM[“North American Datum 1983”,
     *      ELLIPSOID[“GRS 1980”,6378137,298.257222101]],
     *    ANGLEUNIT[“degree”,0.0174532925199433],
     *    PRIMEM[“Greenwich”,0]],
     *  CONVERSION[“UTM zone 10N”,ID[“EPSG”,16010],
     *    METHOD[“Transverse Mercator”],
     *    PARAMETER[“Latitude of natural origin”,0.0],
     *    PARAMETER[“Longitude of natural origin”,-123.0],
     *    PARAMETER[“Scale factor”,0.9996],
     *    PARAMETER[“False easting”,500000.0],
     *    PARAMETER[“False northing”,0.0]],
     *  CS[Cartesian,2],
     *    AXIS[“(E)”,east,ORDER[1]],
     *    AXIS[“(N)”,north,ORDER[2]],
     *    LENGTHUNIT[“metre”,1.0],
     *  REMARK[“In this example units are implied. This is allowed for backward compatibility.
     *          It is recommended that units are explicitly given in the string,
     *          as in the previous two examples.”]]</pre></blockquote>
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testProjectedWithImplicitParameterUnits() throws FactoryException {
        super.testProjectedWithImplicitParameterUnits();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals(AxisNames.EASTING,  cs.getAxis(0).getName().getCode(), "name");
        assertEquals(AxisNames.NORTHING, cs.getAxis(1).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testProjectedWithImplicitParameterUnits();    // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis name and vertical datum type.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  VERTCRS[“NAVD88”,
     *    VDATUM[“North American Vertical Datum 1988”],
     *    CS[vertical,1],
     *      AXIS[“gravity-related height (H)”,up],LENGTHUNIT[“metre”,1.0]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testVertical() throws FactoryException {
        super.testVertical();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals(AxisNames.GRAVITY_RELATED_HEIGHT, cs.getAxis(0).getName().getCode(), "name");
        assertEquals(RealizationMethod.GEOID, ((VerticalCRS) object).getDatum().getRealizationMethod().orElse(null));

        useStraightQuotes = true;
        super.testVertical();                               // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis name.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  TIMECRS[“GPS Time”,
     *    TDATUM[“Time origin”,TIMEORIGIN[1980-01-01T00:00:00.0Z]],
     *    CS[temporal,1],AXIS[“time”,future],TIMEUNIT[“day”,86400.0]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testTemporal() throws FactoryException {
        super.testTemporal();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals(AxisNames.TIME, cs.getAxis(0).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testTemporal();                               // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis name.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  PARAMETRICCRS[“WMO standard atmosphere layer 0”,
     *    PDATUM[“Mean Sea Level”,ANCHOR[“1013.25 hPa at 15°C”]],
     *    CS[parametric,1],
     *    AXIS[“pressure (hPa)”,up],
     *    PARAMETRICUNIT[“hPa”,100.0]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testParametric() throws FactoryException {
        super.testParametric();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals("pressure", cs.getAxis(0).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testParametric();                             // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  ENGINEERINGCRS[“Astra Minas Grid”,
     *    ENGINEERINGDATUM[“Astra Minas”],
     *    CS[Cartesian,2],
     *      AXIS[“northing (X)”,north,ORDER[1]],
     *      AXIS[“westing (Y)”,west,ORDER[2]],
     *      LENGTHUNIT[“metre”,1.0],
     *    ID[“EPSG”,5800]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testEngineering() throws FactoryException {
        super.testEngineering();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals(AxisNames.NORTHING, cs.getAxis(0).getName().getCode(), "name");
        assertEquals(AxisNames.WESTING,  cs.getAxis(1).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testEngineering();                            // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  ENGCRS[“A construction site CRS”,
     *    EDATUM[“P1”,ANCHOR[“Peg in south corner”]],
     *    CS[Cartesian,2],
     *      AXIS[“site east”,southWest,ORDER[1]],
     *      AXIS[“site north”,southEast,ORDER[2]],
     *      LENGTHUNIT[“metre”,1.0],
     *    TIMEEXTENT[“date/time t1”,“date/time t2”]]
     *  }
     *
     * In current Apache SIS version, this test produces a logs a warning saying
     * that the {@code TimeExtent[…]} element is not yet supported.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testEngineeringRotated() throws FactoryException {
        super.testEngineeringRotated();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals("site east",  cs.getAxis(0).getName().getCode(), "name");
        assertEquals("site north", cs.getAxis(1).getName().getCode(), "name");
        loggings.assertNextLogContains("A construction site CRS", "TimeExtent[String,String]");

        useStraightQuotes = true;
        super.testEngineeringRotated();                     // Test again with “ and ” replaced by ".
        loggings.assertNextLogContains("A construction site CRS", "TimeExtent[String,String]");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  ENGCRS[“A ship-centred CRS”,
     *    EDATUM[“Ship reference point”,ANCHOR[“Centre of buoyancy”]],
     *    CS[Cartesian,3],
     *      AXIS[“(x)”,forward],
     *      AXIS[“(y)”,starboard],
     *      AXIS[“(z)”,down],
     *      LENGTHUNIT[“metre”,1.0]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testEngineeringForShip() throws FactoryException {
        super.testEngineeringForShip();
        final CoordinateSystem cs = object.getCoordinateSystem();
        /*
         * In this case we had no axis names, so Apache SIS reused the abbreviations.
         * This could change in any future SIS version if we update Transliterator.
         */
        assertEquals("x", cs.getAxis(0).getName().getCode(), "name");
        assertEquals("y", cs.getAxis(1).getName().getCode(), "name");
        assertEquals("z", cs.getAxis(2).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testEngineeringForShip();                     // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  GEODCRS[“ETRS89 Lambert Azimuthal Equal Area CRS”,
     *    BASEGEODCRS[“WGS 84”,
     *      DATUM[“WGS 84”,
     *        ELLIPSOID[“WGS 84”,6378137,298.2572236,LENGTHUNIT[“metre”,1.0]]]],
     *    DERIVINGCONVERSION[“Atlantic pole”,
     *      METHOD[“Pole rotation”,ID[“Authority”,1234]],
     *      PARAMETER[“Latitude of rotated pole”,52.0,
     *        ANGLEUNIT[“degree”,0.0174532925199433]],
     *      PARAMETER[“Longitude of rotated pole”,-30.0,
     *        ANGLEUNIT[“degree”,0.0174532925199433]],
     *      PARAMETER[“Axis rotation”,-25.0,
     *        ANGLEUNIT[“degree”,0.0174532925199433]]],
     *    CS[ellipsoidal,2],
     *      AXIS[“latitude”,north,ORDER[1]],
     *      AXIS[“longitude”,east,ORDER[2]],
     *      ANGLEUNIT[“degree”,0.0174532925199433]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testDerivedGeodetic() throws FactoryException {
        super.testDerivedGeodetic();
        verifyEllipsoidalCS();
        useStraightQuotes = true;
        super.testDerivedGeodetic();                        // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  ENGCRS[“Topocentric example A”,
     *    BASEGEODCRS[“WGS 84”,
     *      DATUM[“WGS 84”,
     *        ELLIPSOID[“WGS 84”, 6378137, 298.2572236, LENGTHUNIT[“metre”,1.0]]]],
     *    DERIVINGCONVERSION[“Topocentric example A”,
     *      METHOD[“Geographic/topocentric conversions”,ID[“EPSG”,9837]],
     *      PARAMETER[“Latitude of topocentric origin”,55.0,
     *        ANGLEUNIT[“degree”,0.0174532925199433]],
     *      PARAMETER[“Longitude of topocentric origin”,5.0,
     *        ANGLEUNIT[“degree”,0.0174532925199433]],
     *      PARAMETER[“Ellipsoidal height of topocentric origin”,0.0,
     *        LENGTHUNIT[“metre”,1.0]]],
     *    CS[Cartesian,3],
     *      AXIS[“Topocentric East (U)”,east,ORDER[1]],
     *      AXIS[“Topocentric North (V)”,north,ORDER[2]],
     *      AXIS[“Topocentric height (W)”,up,ORDER[3]],
     *      LENGTHUNIT[“metre”,1.0]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testDerivedEngineeringFromGeodetic() throws FactoryException {
        super.testDerivedEngineeringFromGeodetic();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals("Topocentric East",   cs.getAxis(0).getName().getCode(), "name");
        assertEquals("Topocentric North",  cs.getAxis(1).getName().getCode(), "name");
        assertEquals("Topocentric height", cs.getAxis(2).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testDerivedEngineeringFromGeodetic();         // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     *
     * @see org.apache.sis.referencing.operation.provider.SeismicBinGridMock
     */
    @Test
    @Override
    @Disabled("Pending implementation of EPSG:1049 — Seismic bin grid.")
    public void testDerivedEngineeringFromProjected() throws FactoryException {
        super.testDerivedEngineeringFromProjected();
        final CoordinateSystem cs = object.getCoordinateSystem();
        /*
         * In this case we had no axis names, so Apache SIS reused the abbreviations.
         * This could change in any future SIS version if we update Transliterator.
         */
        assertEquals("I", cs.getAxis(0).getName().getCode(), "name");
        assertEquals("J", cs.getAxis(1).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testDerivedEngineeringFromProjected();        // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  COMPOUNDCRS[“NAD83 + NAVD88”,
     *    GEODCRS[“NAD83”,
     *      DATUM[“North American Datum 1983”,
     *        ELLIPSOID[“GRS 1980”,6378137,298.257222101,
     *          LENGTHUNIT[“metre”,1.0]]],
     *        PRIMEMERIDIAN[“Greenwich”,0],
     *      CS[ellipsoidal,2],
     *        AXIS[“latitude”,north,ORDER[1]],
     *        AXIS[“longitude”,east,ORDER[2]],
     *        ANGLEUNIT[“degree”,0.0174532925199433]],
     *      VERTCRS[“NAVD88”,
     *        VDATUM[“North American Vertical Datum 1988”],
     *        CS[vertical,1],
     *          AXIS[“gravity-related height (H)”,up],
     *          LENGTHUNIT[“metre”,1]]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testCompoundWithVertical() throws FactoryException {
        super.testCompoundWithVertical();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals(AxisNames.GEODETIC_LATITUDE,      cs.getAxis(0).getName().getCode(), "name");
        assertEquals(AxisNames.GEODETIC_LONGITUDE,     cs.getAxis(1).getName().getCode(), "name");
        assertEquals(AxisNames.GRAVITY_RELATED_HEIGHT, cs.getAxis(2).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testCompoundWithVertical();                   // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  COMPOUNDCRS[“GPS position and time”,
     *    GEODCRS[“WGS 84”,
     *      DATUM[“World Geodetic System 1984”,
     *        ELLIPSOID[“WGS 84”,6378137,298.257223563]],
     *      CS[ellipsoidal,2],
     *        AXIS[“(lat)”,north,ORDER[1]],
     *        AXIS[“(lon)”,east,ORDER[2]],
     *        ANGLEUNIT[“degree”,0.0174532925199433]],
     *    TIMECRS[“GPS Time”,
     *      TIMEDATUM[“Time origin”,TIMEORIGIN[1980-01-01]],
     *      CS[temporal,1],
     *        AXIS[“time (T)”,future],
     *        TIMEUNIT[“day”,86400]]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testCompoundWithTime() throws FactoryException {
        super.testCompoundWithTime();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals(AxisNames.GEODETIC_LATITUDE,  cs.getAxis(0).getName().getCode(), "name");
        assertEquals(AxisNames.GEODETIC_LONGITUDE, cs.getAxis(1).getName().getCode(), "name");
        assertEquals(AxisNames.TIME,               cs.getAxis(2).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testCompoundWithTime();                       // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@snippet lang="wkt" :
     *  COMPOUNDCRS[“ICAO layer 0”,
     *    GEODETICCRS[“WGS 84”,
     *      DATUM[“World Geodetic System 1984”,
     *        ELLIPSOID[“WGS 84”,6378137,298.257223563,
     *          LENGTHUNIT[“metre”,1.0]]],
     *      CS[ellipsoidal,2],
     *        AXIS[“latitude”,north,ORDER[1]],
     *        AXIS[“longitude”,east,ORDER[2]],
     *        ANGLEUNIT[“degree”,0.0174532925199433]],
     *    PARAMETRICCRS[“WMO standard atmosphere”,
     *      PARAMETRICDATUM[“Mean Sea Level”,
     *        ANCHOR[“Mean Sea Level = 1013.25 hPa”]],
     *          CS[parametric,1],
     *            AXIS[“pressure (P)”,unspecified],
     *            PARAMETRICUNIT[“hPa”,100]]]
     *  }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testCompoundWithParametric() throws FactoryException {
        super.testCompoundWithParametric();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals(AxisNames.GEODETIC_LATITUDE,  cs.getAxis(0).getName().getCode(), "name");
        assertEquals(AxisNames.GEODETIC_LONGITUDE, cs.getAxis(1).getName().getCode(), "name");
        assertEquals("pressure",                   cs.getAxis(2).getName().getCode(), "name");

        useStraightQuotes = true;
        super.testCompoundWithParametric();                 // Test again with “ and ” replaced by ".
        loggings.assertNoUnexpectedLog();
    }
}
