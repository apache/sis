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
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.util.FactoryException;
import org.opengis.test.wkt.CRSParserTest;
import org.apache.sis.internal.metadata.AxisNames;
import org.apache.sis.test.DependsOn;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;


/**
 * Tests Well-Known Text parser using the tests defined in GeoAPI. Those tests use the
 * {@link org.apache.sis.referencing.factory.GeodeticObjectFactory#createFromWKT(String)} method.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@RunWith(JUnit4.class)
@DependsOn(GeodeticObjectParserTest.class)
public class WKTParserTest extends CRSParserTest {
    /**
     * Creates a new test case using the default {@code CRSFactory} implementation.
     */
    public WKTParserTest() {
        super(org.apache.sis.internal.system.DefaultFactories.forClass(CRSFactory.class));
    }

    /**
     * Verifies the axis names of a geographic CRS. This method is invoked when the parsed object is
     * expected to have <cite>"Geodetic latitude"</cite> and <cite>"Geodetic longitude"</cite> names.
     */
    @SuppressWarnings("fallthrough")
    private void verifyEllipsoidalCS() {
        final CoordinateSystem cs = object.getCoordinateSystem();
        switch (cs.getDimension()) {
            default: assertEquals("name", AxisNames.ELLIPSOIDAL_HEIGHT, cs.getAxis(2).getName().getCode());
            case 2:  assertEquals("name", AxisNames.GEODETIC_LONGITUDE, cs.getAxis(1).getName().getCode());
            case 1:  assertEquals("name", AxisNames.GEODETIC_LATITUDE,  cs.getAxis(0).getName().getCode());
            case 0:  break;
        }
        switch (cs.getDimension()) {
            default: assertEquals("abbreviation", "h", cs.getAxis(2).getAbbreviation());
            case 2:  assertEquals("abbreviation", "λ", cs.getAxis(1).getAbbreviation());
            case 1:  assertEquals("abbreviation", "φ", cs.getAxis(0).getAbbreviation());
            case 0:  break;
        }
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@preformat wkt
     *   GEODCRS[“WGS 84”,
     *    DATUM[“World Geodetic System 1984”,
     *      ELLIPSOID[“WGS 84”, 6378137, 298.257223563,
     *        LENGTHUNIT[“metre”,1.0]]],
     *    CS[ellipsoidal,3],
     *      AXIS[“(lat)”,north,ANGLEUNIT[“degree”,0.0174532925199433]],
     *      AXIS[“(lon)”,east,ANGLEUNIT[“degree”,0.0174532925199433]],
     *      AXIS[“ellipsoidal height (h)”,up,LENGTHUNIT[“metre”,1.0]]]
     * }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographic() throws FactoryException {
        super.testGeographic();
        verifyEllipsoidalCS();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@preformat wkt
     *   GEODCRS[“S-95”,
     *    DATUM[“Pulkovo 1995”,
     *      ELLIPSOID[“Krassowsky 1940”, 6378245, 298.3,
     *        LENGTHUNIT[“metre”,1.0]]],
     *    CS[ellipsoidal,2],
     *      AXIS[“latitude”,north,ORDER[1]],
     *      AXIS[“longitude”,east,ORDER[2]],
     *      ANGLEUNIT[“degree”,0.0174532925199433],
     *    REMARK[“Система Геодеэических Координвт года 1995(СК-95)”]]
     * }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographicWithRemark() throws FactoryException {
        super.testGeographicWithRemark();
        verifyEllipsoidalCS();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@preformat wkt
     *   GEODCRS[“NAD83”,
     *    DATUM[“North American Datum 1983”,
     *      ELLIPSOID[“GRS 1980”, 6378137, 298.257222101, LENGTHUNIT[“metre”,1.0]]],
     *    CS[ellipsoidal,2],
     *      AXIS[“latitude”,north],
     *      AXIS[“longitude”,east],
     *      ANGLEUNIT[“degree”,0.017453292519943],
     *    ID[“EPSG”,4269],
     *    REMARK[“1986 realisation”]]
     * }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographicWithId() throws FactoryException {
        super.testGeographicWithId();
        verifyEllipsoidalCS();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@preformat wkt
     *   GEODCRS[“NTF (Paris)”,
     *    DATUM[“Nouvelle Triangulation Francaise”,
     *      ELLIPSOID[“Clarke 1880 (IGN)”, 6378249.2, 293.4660213]],
     *    PRIMEM[“Paris”,2.5969213],
     *    CS[ellipsoidal,2],
     *      AXIS[“latitude”,north,ORDER[1]],
     *      AXIS[“longitude”,east,ORDER[2]],
     *      ANGLEUNIT[“grad”,0.015707963267949],
     *    REMARK[“Nouvelle Triangulation Française”]]
     * }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeographicWithGradUnits() throws FactoryException {
        super.testGeographicWithGradUnits();
        verifyEllipsoidalCS();
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@preformat wkt
     *   GEODETICCRS[“JGD2000”,
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
     * }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testGeocentric() throws FactoryException {
        super.testGeocentric();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals("name", AxisNames.GEOCENTRIC_X, cs.getAxis(0).getName().getCode());
        assertEquals("name", AxisNames.GEOCENTRIC_Y, cs.getAxis(1).getName().getCode());
        assertEquals("name", AxisNames.GEOCENTRIC_Z, cs.getAxis(2).getName().getCode());
    }

    /**
     * Ignored for now, because the Lambert Azimuthal Equal Area projection method is not yet implemented.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    @Ignore("Lambert Azimuthal Equal Area projection method not yet implemented.")
    public void testProjected() throws FactoryException {
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@preformat wkt
     *   PROJCRS[“NAD27 / Texas South Central”,
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
     * }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testProjectedWithFootUnits() throws FactoryException {
        super.testProjectedWithFootUnits();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals("name", AxisNames.EASTING,  cs.getAxis(0).getName().getCode());
        assertEquals("name", AxisNames.NORTHING, cs.getAxis(1).getName().getCode());
    }

    /**
     * Ignored for now, because the Transverse Mercator projection method is not yet implemented.
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    @Ignore("Transverse Mercator projection method not yet implemented.")
    public void testProjectedWithImplicitParameterUnits() throws FactoryException {
    }

    /**
     * Completes the GeoAPI tests with a check of axis name and vertical datum type.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@preformat wkt
     *   VERTCRS[“NAVD88”,
     *    VDATUM[“North American Vertical Datum 1988”],
     *    CS[vertical,1],
     *      AXIS[“gravity-related height (H)”,up],LENGTHUNIT[“metre”,1.0]]
     * }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testVertical() throws FactoryException {
        super.testVertical();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals("name", AxisNames.GRAVITY_RELATED_HEIGHT, cs.getAxis(0).getName().getCode());
        assertEquals("datumType", VerticalDatumType.GEOIDAL, ((VerticalCRS) object).getDatum().getVerticalDatumType());
    }

    /**
     * Completes the GeoAPI tests with a check of axis name.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@preformat wkt
     *   TIMECRS[“GPS Time”,
     *     TDATUM[“Time origin”,TIMEORIGIN[1980-01-01T00:00:00.0Z]],
     *     CS[temporal,1],AXIS[“time”,future],TIMEUNIT[“day”,86400.0]]
     * }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testTemporal() throws FactoryException {
        super.testTemporal();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals("name", AxisNames.TIME, cs.getAxis(0).getName().getCode());
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@preformat wkt
     *   ENGINEERINGCRS[“Astra Minas Grid”,
     *    ENGINEERINGDATUM[“Astra Minas”],
     *    CS[Cartesian,2],
     *      AXIS[“northing (X)”,north,ORDER[1]],
     *      AXIS[“westing (Y)”,west,ORDER[2]],
     *      LENGTHUNIT[“metre”,1.0],
     *    ID[“EPSG”,5800]]
     * }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testEngineering() throws FactoryException {
        super.testEngineering();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals("name", AxisNames.NORTHING, cs.getAxis(0).getName().getCode());
        assertEquals("name", AxisNames.WESTING,  cs.getAxis(1).getName().getCode());
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@preformat wkt
     *   ENGCRS[“A construction site CRS”,
     *    EDATUM[“P1”,ANCHOR[“Peg in south corner”]],
     *    CS[Cartesian,2],
     *      AXIS[“site east”,southWest,ORDER[1]],
     *      AXIS[“site north”,southEast,ORDER[2]],
     *      LENGTHUNIT[“metre”,1.0],
     *    TIMEEXTENT[“date/time t1”,“date/time t2”]]
     * }
     *
     * @throws FactoryException if an error occurred during the WKT parsing.
     */
    @Test
    @Override
    public void testEngineeringInclined() throws FactoryException {
        super.testEngineeringInclined();
        final CoordinateSystem cs = object.getCoordinateSystem();
        assertEquals("name", "site east",  cs.getAxis(0).getName().getCode());
        assertEquals("name", "site north", cs.getAxis(1).getName().getCode());
    }

    /**
     * Completes the GeoAPI tests with a check of axis names.
     * The WKT parsed by this test is (except for quote characters):
     *
     * {@preformat wkt
     *   ENGCRS[“A ship-centred CRS”,
     *    EDATUM[“Ship reference point”,ANCHOR[“Centre of buoyancy”]],
     *    CS[Cartesian,3],
     *      AXIS[“(x)”,forward],
     *      AXIS[“(y)”,starboard],
     *      AXIS[“(z)”,down],
     *      LENGTHUNIT[“metre”,1.0]]
     * }
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
        assertEquals("name", "x", cs.getAxis(0).getName().getCode());
        assertEquals("name", "y", cs.getAxis(1).getName().getCode());
        assertEquals("name", "z", cs.getAxis(2).getName().getCode());
    }
}
