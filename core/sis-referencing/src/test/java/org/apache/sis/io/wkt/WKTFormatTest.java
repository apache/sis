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

import java.util.Collections;
import java.text.ParseException;
import javax.measure.unit.NonSI;
import org.opengis.referencing.crs.VerticalCRS;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.datum.DefaultPrimeMeridian;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link WKTFormat}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
@DependsOn(GeodeticObjectParserTest.class)
public final strictfp class WKTFormatTest extends TestCase {
    /**
     * The instance to use for the test, or {@code null} if none.
     */
    private WKTFormat format;

    /**
     * An instance used by {@link #testConsistency()} for parsing the WKT.
     * May be a different instance than {@link #format} if the two instances
     * do not use the same {@link Convention}.
     */
    private WKTFormat parser;

    /**
     * Tests integration in {@link WKTFormat#parse(CharSequence, ParsePosition)}.
     * This method tests only a simple WKT because it is not the purpose of this
     * method to test the parser itself. We only want to tests its integration in
     * the {@link WKTFormat} class.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testParse() throws ParseException {
        format = new WKTFormat(null, null);
        final VerticalCRS crs = (VerticalCRS) format.parseObject(
                "VERT_CS[“Gravity-related height”,\n" +
                "  VERT_DATUM[“Mean Sea Level”, 2005],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“Gravity-related height”, UP]]");

        GeodeticObjectParserTest.assertNameAndIdentifierEqual("Gravity-related height", 0, crs);
        GeodeticObjectParserTest.assertNameAndIdentifierEqual("Mean Sea Level", 0, crs.getDatum());
    }

    /**
     * Tests consistency between the parser and the formatter when using the WKT 1 format.
     * This test parses a WKT, formats it then parses again. We should obtain the same result.
     *
     * @throws ParseException if a parsing failed.
     */
    @Test
    public void testConsistencyOfWKT1() throws ParseException {
        format = new WKTFormat(null, null);
        format.setConvention(Convention.WKT1);
        parser = format;
        testConsistency();
        testConsistencyWithDenormalizedBaseCRS();
    }

    /**
     * Tests consistency between the parser and the formatter when using the WKT 1 format.
     * This test parses a WKT, formats it then parses again. We should obtain the same result.
     *
     * @throws ParseException if a parsing failed.
     */
    @Test
    @DependsOnMethod("testConsistencyOfWKT1")
    public void testConsistencyOfWKT1_WithCommonUnits() throws ParseException {
        format = new WKTFormat(null, null);
        format.setConvention(Convention.WKT1_COMMON_UNITS);
        parser = new WKTFormat(null, null);
        parser.setConvention(Convention.WKT1);
        testConsistency();
        testConsistencyWithDenormalizedBaseCRS();
    }

    /**
     * Tests consistency between the parser and the formatter when using the WKT 2 format.
     * This test parses a WKT, formats it then parses again. We should obtain the same result.
     *
     * @throws ParseException if a parsing failed.
     */
    @Test
    @DependsOnMethod("testConsistencyOfWKT1")
    public void testConsistencyOfWKT2() throws ParseException {
        format = new WKTFormat(null, null);
        format.setConvention(Convention.WKT2);
        parser = format;
        testConsistency();
    }

    /**
     * Tests consistency between the parser and the formatter when using the WKT 2 simplified format.
     * This test parses a WKT, formats it then parses again. We should obtain the same result.
     *
     * @throws ParseException if a parsing failed.
     */
    @Test
    @DependsOnMethod("testConsistencyOfWKT2")
    public void testConsistencyOfWKT2_Simplified() throws ParseException {
        format = new WKTFormat(null, null);
        format.setConvention(Convention.WKT2_SIMPLIFIED);
        parser = format;
        testConsistency();
    }

    /**
     * Implementation of {@link #testConsistencyOfWKT1()} and variants.
     *
     * @throws ParseException if a parsing failed.
     */
    private void testConsistency() throws ParseException {
        testConsistency(
                "GEOGCS[“Tokyo”,"
                + "DATUM[“Tokyo”,"
                +   "SPHEROID[“Bessel 1841”, 6377397.155, 299.1528128, AUTHORITY[“EPSG”,“7004”]],"
                +   "TOWGS84[-148,507,685,0,0,0,0],AUTHORITY[“EPSG”,“6301”]],"
                + "PRIMEM[“Greenwich”,0,AUTHORITY[“EPSG”,“8901”]],"
                + "UNIT[“DMSH”,0.0174532925199433,AUTHORITY[“EPSG”,“9108”]],"
                + "AXIS[“Lat”,NORTH],"
                + "AXIS[“Long”,EAST],"
                + "AUTHORITY[“EPSG”,“4301”]]");

        testConsistency(
                "GEOGCS[“NTF (Paris)”,"
                + "DATUM[“Nouvelle_Triangulation_Francaise”,"
                +   "SPHEROID[“Clarke 1880 (IGN)”, 6378249.2, 293.466021293627, AUTHORITY[“EPSG”,“7011”]],"
                +   "TOWGS84[-168,-60,320,0,0,0,0],AUTHORITY[“EPSG”,“6275”]],"
                + "PRIMEM[“Paris”,2.5969213,AUTHORITY[“EPSG”,“8903”]],"
                + "UNIT[“grad”,0.015707963267949,AUTHORITY[“EPSG”,“9105”]],"
                + "AXIS[“Lat”,NORTH],"
                + "AXIS[“Long”,EAST],"
                + "AUTHORITY[“EPSG”,“4807”]]");

        testConsistency(
                "PROJCS[“NAD27 / Texas South Central”,"
                + "GEOGCS[“NAD27”,"
                +   "DATUM[“North American Datum 1927”,"
                +     "SPHEROID[“Clarke 1866”, 6378206.4, 294.97869821]],"
                +   "UNIT[“degree”,0.0174532925199433],"
                +   "AXIS[“Lat”,NORTH],"
                +   "AXIS[“Long”,EAST]],"
                + "PROJECTION[“Lambert_Conformal_Conic_2SP”],"
                + "PARAMETER[“latitude_of_origin”,27.83333333333333],"
                + "PARAMETER[“central_meridian”,-99.0],"
                + "PARAMETER[“standard_parallel_1”,28.383333333333],"
                + "PARAMETER[“standard_parallel_2”,30.283333333333],"
                + "PARAMETER[“false_easting”,2000000],"
                + "PARAMETER[“false_northing”,0],"
                + "UNIT[“US survey foot”,0.304800609601219],"
                + "AXIS[“Y”,NORTH],"
                + "AXIS[“X”,EAST]]");

        testConsistency(
                "VERT_CS[“mean sea level depth”,"
                + "VERT_DATUM[“Mean Sea Level”,2005,AUTHORITY[“EPSG”,“5100”]],"
                + "UNIT[“kilometre”,1000],AXIS[“Z”,DOWN]]");
    }

    /**
     * Similar to {@link #testConsistency()}, but using a base CRS that do not have normalized axes.
     * Since base CRS axes are formatted in WKT 1 but not in WKT 2, we have an information lost in WKT 2.
     * This information lost was considered unimportant in WKT 2 because the ISO 19111 ProjectedCRS model
     * does not have a MathTransform. But in the Apache SIS case, this causes the 'conversionFromBase'
     * property to have a different MathTransform, and consequently cause a test failure.
     * In brief, the tests in this class can not be run on those WKT using the WKT 2 format.
     *
     * @throws ParseException if a parsing failed.
     */
    private void testConsistencyWithDenormalizedBaseCRS() throws ParseException {
        testConsistency(
                "PROJCS[“NTF (Paris) / France I”,"
                + "GEOGCS[“NTF (Paris)”,"
                +   "DATUM[“Nouvelle_Triangulation_Francaise”,"
                +     "SPHEROID[“Clarke 1880 (IGN)”, 6378249.2, 293.466021293627, AUTHORITY[“EPSG”,“7011”]],"
                +     "TOWGS84[-168,-60,320,0,0,0,0],"
                +     "AUTHORITY[“EPSG”,“6275”]],"
                +   "PRIMEM[“Paris”,2.5969213,AUTHORITY[“EPSG”,“8903”]],"
                +   "UNIT[“grad”,0.015707963267949,AUTHORITY[“EPSG”,“9105”]],"
                +   "AXIS[“Lat”,NORTH],"
                +   "AXIS[“Long”,EAST],"
                +   "AUTHORITY[“EPSG”,“4807”]],"
                + "PROJECTION[“Lambert_Conformal_Conic_1SP”],"
                + "PARAMETER[“latitude_of_origin”,55],"             // 55 grads = 49.5 degrees
                + "PARAMETER[“central_meridian”,0],"
                + "PARAMETER[“scale_factor”,0.999877341],"
                + "PARAMETER[“false_easting”,600],"
                + "PARAMETER[“false_northing”,1200],"
                + "UNIT[“km”,1000],"
                + "AXIS[“X”,EAST],"
                + "AXIS[“Y”,NORTH]]");
    }

    /**
     * Implementation of {@link #testConsistency()} for a single WKT.
     *
     * @throws ParseException if the parsing failed.
     */
    private void testConsistency(final String wkt) throws ParseException {
        final Object expected = parser.parseObject(wkt);
        final String reformat = format.format(expected);
        final Object reparsed = format.parseObject(reformat);
        assertEqualsIgnoreMetadata(expected, reparsed);
    }

    /**
     * Tests the formatting using the name of different authorities.
     * This test uses WKT 1 for parsing and formatting.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testVariousConventions() throws ParseException {
        final Symbols symbols = new Symbols(Symbols.SQUARE_BRACKETS);
        symbols.setPairedQuotes("“”");
        parser = format = new WKTFormat(null, null);
        format.setSymbols(symbols);
        final DefaultProjectedCRS crs = (DefaultProjectedCRS) parser.parseObject(
            "PROJCS[“OSGB 1936 / British National Grid”,\n" +
            "  GEOGCS[“OSGB 1936”,\n" +
            "    DATUM[“OSGB_1936”,\n" +
            "      SPHEROID[“Airy 1830”, 6377563.396, 299.3249646],\n" +
            "      TOWGS84[375.0, -111.0, 431.0, 0.0, 0.0, 0.0, 0.0]],\n" +
            "      PRIMEM[“Greenwich”,0.0],\n" +
            "    UNIT[“DMSH”,0.0174532925199433],\n" +
            "    AXIS[“Lat”,NORTH],AXIS[“Long”,EAST]],\n" +
            "  PROJECTION[“Transverse_Mercator”],\n" +
            "  PARAMETER[“latitude_of_origin”, 49.0],\n" +
            "  PARAMETER[“central_meridian”, -2.0],\n" +
            "  PARAMETER[“scale_factor”, 0.999601272],\n" +
            "  PARAMETER[“false_easting”, 400000.0],\n" +
            "  PARAMETER[“false_northing”, -100000.0],\n" +
            "  UNIT[“metre”, 1],\n" +
            "  AXIS[“E”,EAST],\n" +
            "  AXIS[“N”,NORTH]]");
        /*
         * Formats using OGC identifiers. Use of OGC identifiers is implicit with Convention.WKT1, unless we
         * set explicitely another authority. The result should be the same than the above string, except:
         *
         *   - The TOWGS84 parameters have been trimmed to 3 values.
         *   - The AUTHORITY elements has been inferred for the PROJECTION element.
         *   - "Latitude of origin" parameter is before "central meridian" parameter.
         */
        format.setConvention(Convention.WKT1);
        assertMultilinesEquals(
            "PROJCS[“OSGB 1936 / British National Grid”,\n" +
            "  GEOGCS[“OSGB 1936”,\n" +
            "    DATUM[“OSGB_1936”,\n" +
            "      SPHEROID[“Airy 1830”, 6377563.396, 299.3249646],\n" +
            "      TOWGS84[375.0, -111.0, 431.0]],\n" +                             // Trimmed to 3 parameters.
            "      PRIMEM[“Greenwich”, 0.0],\n" +
            "    UNIT[“degree”, 0.017453292519943295],\n" +
            "    AXIS[“Latitude”, NORTH],\n" +
            "    AXIS[“Longitude”, EAST]],\n" +
            "  PROJECTION[“Transverse_Mercator”, AUTHORITY[“EPSG”, “9807”]],\n" +   // AUTHORITY code automatically found.
            "  PARAMETER[“latitude_of_origin”, 49.0],\n" +                          // Sorted before central meridian.
            "  PARAMETER[“central_meridian”, -2.0],\n" +
            "  PARAMETER[“scale_factor”, 0.999601272],\n" +
            "  PARAMETER[“false_easting”, 400000.0],\n" +
            "  PARAMETER[“false_northing”, -100000.0],\n" +
            "  UNIT[“metre”, 1],\n" +
            "  AXIS[“Easting”, EAST],\n" +
            "  AXIS[“Northing”, NORTH]]",
            format.format(crs));
        /*
         * Formats using GeoTiff identifiers. We should get different strings in PROJECTION[...]
         * and PARAMETER[...] elements, but the other ones (especially DATUM[...]) are unchanged.
         */
        format.setNameAuthority(Citations.GEOTIFF);
        assertMultilinesEquals(
            "PROJCS[“OSGB 1936 / British National Grid”,\n" +
            "  GEOGCS[“OSGB 1936”,\n" +
            "    DATUM[“OSGB_1936”,\n" +
            "      SPHEROID[“Airy 1830”, 6377563.396, 299.3249646],\n" +
            "      TOWGS84[375.0, -111.0, 431.0]],\n" +
            "      PRIMEM[“Greenwich”, 0.0],\n" +
            "    UNIT[“degree”, 0.017453292519943295],\n" +
            "    AXIS[“Latitude”, NORTH],\n" +
            "    AXIS[“Longitude”, EAST]],\n" +
            "  PROJECTION[“CT_TransverseMercator”, AUTHORITY[“GeoTIFF”, “1”]],\n" +
            "  PARAMETER[“NatOriginLat”, 49.0],\n" +
            "  PARAMETER[“NatOriginLong”, -2.0],\n" +
            "  PARAMETER[“ScaleAtNatOrigin”, 0.999601272],\n" +
            "  PARAMETER[“FalseEasting”, 400000.0],\n" +
            "  PARAMETER[“FalseNorthing”, -100000.0],\n" +
            "  UNIT[“metre”, 1],\n" +
            "  AXIS[“Easting”, EAST],\n" +
            "  AXIS[“Northing”, NORTH]]",
            format.format(crs));
        /*
         * Formats using ESRI identifiers. The most important change we are looking for is
         * the name inside DATUM[...].
         */
        format.setNameAuthority(Citations.ESRI);
        format.setConvention(Convention.WKT1_COMMON_UNITS);
        assertMultilinesEquals(
            "PROJCS[“OSGB 1936 / British National Grid”,\n" +
            "  GEOGCS[“OSGB 1936”,\n" +
            "    DATUM[“D_OSGB_1936”,\n" +
            "      SPHEROID[“Airy 1830”, 6377563.396, 299.3249646],\n" +
            "      TOWGS84[375.0, -111.0, 431.0]],\n" +
            "      PRIMEM[“Greenwich”, 0.0],\n" +
            "    UNIT[“degree”, 0.017453292519943295],\n" +
            "    AXIS[“Latitude”, NORTH],\n" +
            "    AXIS[“Longitude”, EAST]],\n" +
            "  PROJECTION[“Transverse_Mercator”, AUTHORITY[“EPSG”, “9807”]],\n" +
            "  PARAMETER[“Latitude_Of_Origin”, 49.0],\n" +
            "  PARAMETER[“Central_Meridian”, -2.0],\n" +
            "  PARAMETER[“Scale_Factor”, 0.999601272],\n" +
            "  PARAMETER[“False_Easting”, 400000.0],\n" +
            "  PARAMETER[“False_Northing”, -100000.0],\n" +
            "  UNIT[“meter”, 1],\n" +
            "  AXIS[“Easting”, EAST],\n" +
            "  AXIS[“Northing”, NORTH]]",
            format.format(crs));
        /*
         * Formats using EPSG identifiers. We expect different names in
         * DATUM[...], PROJECTION[...] and PARAMETER[...].
         */
        format.setNameAuthority(Citations.EPSG);
        assertMultilinesEquals(
            "PROJCS[“OSGB 1936 / British National Grid”,\n" +
            "  GEOGCS[“OSGB 1936”,\n" +
            "    DATUM[“OSGB_1936”,\n" +
            "      SPHEROID[“Airy 1830”, 6377563.396, 299.3249646],\n" +
            "      TOWGS84[375.0, -111.0, 431.0]],\n" +
            "      PRIMEM[“Greenwich”, 0.0],\n" +
            "    UNIT[“degree”, 0.017453292519943295],\n" +
            "    AXIS[“Latitude”, NORTH],\n" +
            "    AXIS[“Longitude”, EAST]],\n" +
            "  PROJECTION[“Transverse Mercator”, AUTHORITY[“EPSG”, “9807”]],\n" +
            "  PARAMETER[“Latitude of natural origin”, 49.0],\n" +
            "  PARAMETER[“Longitude of natural origin”, -2.0],\n" +
            "  PARAMETER[“Scale factor at natural origin”, 0.999601272],\n" +
            "  PARAMETER[“False easting”, 400000.0],\n" +
            "  PARAMETER[“False northing”, -100000.0],\n" +
            "  UNIT[“meter”, 1],\n" +
            "  AXIS[“Easting”, EAST],\n" +
            "  AXIS[“Northing”, NORTH]]",
            format.format(crs));
    }

    /**
     * Tests the production of a warning messages when the WKT contains unformattable elements.
     *
     * @throws ParseException if the parsing (tested after formatting) failed.
     */
    @Test
    public void testWarnings() throws ParseException {
        DefaultPrimeMeridian pm = new DefaultPrimeMeridian(Collections.singletonMap(
                DefaultPrimeMeridian.NAME_KEY, "Invalid “$name” here"), -10, NonSI.DEGREE_ANGLE);
        format = new WKTFormat(null, null);
        final String   wkt      = format.format(pm);
        final Warnings warnings = format.getWarnings();
        assertNotNull("warnings", warnings);
        assertEquals ("warnings.numMessages", 1, warnings.getNumMessages());
        assertEquals ("PRIMEM[\"Invalid \"\"$name\"\" here\", -10.0, ANGLEUNIT[\"degree\", 0.017453292519943295]]", wkt);
        assertEquals ("The “$” character in “\"$name\"” is not permitted by the “Well-Known Text” format.", warnings.getMessage(0));
        assertNull   (warnings.getException(0));
        /*
         * Verify that FormattableObject.toWKT() reports that the WKT is invalid.
         */
        try {
            pm.toWKT();
            fail("Expected UnformattableObjectException.");
        } catch (UnformattableObjectException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("$name"));
        }
        /*
         * Verify that the WKT is still parseable despite the warning.
         */
        pm = (DefaultPrimeMeridian) format.parseObject(wkt);
        assertEquals("Invalid \"$name\" here", pm.getName().getCode());
    }

    /**
     * Tests the usage of {@code WKTFormat} with WKT fragments.
     *
     * @throws ParseException if the parsing failed.
     */
    @Test
    public void testFragments() throws ParseException {
        format = new WKTFormat(null, null);
        format.addFragment("deg",    "UNIT[“degree”, 0.0174532925199433]");
        format.addFragment("Bessel", "SPHEROID[“Bessel 1841”, 6377397.155, 299.1528128, AUTHORITY[“EPSG”,“7004”]]");
        format.addFragment("Tokyo",  "DATUM[“Tokyo”, $Bessel]");
        format.addFragment("Lat",    "AXIS[“Lat”, NORTH, $deg]");
        format.addFragment("Lon",    "AXIS[“Long”, EAST, $deg]");
        final Object crs = format.parseObject("GEOGCS[“Tokyo”, $Tokyo, $Lat, $Lon]");
        final String wkt = format.format(crs);
        assertMultilinesEquals(
                "GEODCRS[\"Tokyo\",\n" +
                "  DATUM[\"Tokyo\",\n" +
                "    ELLIPSOID[\"Bessel 1841\", 6377397.155, 299.1528128, LENGTHUNIT[\"metre\", 1]]],\n" +
                "    PRIMEM[\"Greenwich\", 0.0, ANGLEUNIT[\"degree\", 0.017453292519943295]],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    AXIS[\"Latitude (B)\", north, ORDER[1]],\n" +
                "    AXIS[\"Longitude (L)\", east, ORDER[2]],\n" +
                "    ANGLEUNIT[\"degree\", 0.017453292519943295]]", wkt);
    }
}
