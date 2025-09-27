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
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.referencing.operation.DefaultConversionTest;
import org.apache.sis.xml.test.TestCase;
import org.apache.sis.referencing.cs.HardCodedCS;
import static org.apache.sis.referencing.Assertions.assertEpsgNameAndIdentifierEqual;
import static org.apache.sis.referencing.Assertions.assertWktEquals;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertAxisDirectionsEqual;
import static org.apache.sis.test.GeoapiAssert.assertMatrixEquals;


/**
 * Tests the {@link DefaultDerivedCRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultDerivedCRSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultDerivedCRSTest() {
    }

    /**
     * Opens the stream to the XML file in this package containing a projected CRS definition.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultDerivedCRSTest.class.getResourceAsStream("DerivedCRS.xml");
    }

    /**
     * Tests {@link DefaultDerivedCRS#getTypeKeyword(Map, SingleCRS, CoordinateSystem)}.
     */
    @Test
    public void testGetType() {
        // Using consistent arguments.
        assertEquals(WKTKeywords.VerticalCRS,
                DefaultDerivedCRS.getTypeKeyword(null, HardCodedCRS.ELLIPSOIDAL_HEIGHT, HardCodedCS.GRAVITY_RELATED_HEIGHT));

        // Using inconsistent arguments.
        assertNull(DefaultDerivedCRS.getTypeKeyword(null, HardCodedCRS.ELLIPSOIDAL_HEIGHT, HardCodedCS.SECONDS));

        // Using consistent arguments.
        assertEquals(WKTKeywords.TimeCRS,
                DefaultDerivedCRS.getTypeKeyword(null, HardCodedCRS.TIME, HardCodedCS.SECONDS));

        // Using inconsistent arguments.
        assertNull(DefaultDerivedCRS.getTypeKeyword(null, HardCodedCRS.TIME, HardCodedCS.GRAVITY_RELATED_HEIGHT));

        // Using consistent arguments.
        assertEquals(WKTKeywords.GeodeticCRS,
                DefaultDerivedCRS.getTypeKeyword(null, HardCodedCRS.WGS84, HardCodedCS.GEODETIC_2D));

        // Using consistent arguments but one more dimension.
        assertEquals(WKTKeywords.GeodeticCRS,
                DefaultDerivedCRS.getTypeKeyword(null, HardCodedCRS.WGS84, HardCodedCS.GEODETIC_3D));

        // Using consistent arguments.
        assertEquals(WKTKeywords.GeodeticCRS,
                DefaultDerivedCRS.getTypeKeyword(null, HardCodedCRS.GEOCENTRIC, HardCodedCS.CARTESIAN_3D));

        // Using consistent arguments but one less dimension.
        assertEquals(WKTKeywords.GeodeticCRS,
                DefaultDerivedCRS.getTypeKeyword(null, HardCodedCRS.GEOCENTRIC, HardCodedCS.CARTESIAN_2D));

        // Using different coordinate system type.
        assertEquals(WKTKeywords.GeodeticCRS,
                DefaultDerivedCRS.getTypeKeyword(null, HardCodedCRS.GEOCENTRIC, HardCodedCS.SPHERICAL));

        // Using different coordinate system type.
        assertEquals(WKTKeywords.GeodeticCRS,
                DefaultDerivedCRS.getTypeKeyword(null, HardCodedCRS.WGS84, HardCodedCS.CARTESIAN_2D));

        // Using illegal coordinate system type.
        assertNull(DefaultDerivedCRS.getTypeKeyword(null, HardCodedCRS.WGS84, HardCodedCS.GRAVITY_RELATED_HEIGHT));
    }

    /**
     * Creates a dummy derived CRS defined by a longitude rotation from Paris to Greenwich prime meridian,
     * and swapping the axis order. The result is equivalent to {@link HardCodedCRS#WGS84_LATITUDE_FIRST},
     * which of course makes the returned {@code DerivedCRS} totally useless.
     * Its purpose is only to perform easy tests.
     */
    private static DefaultDerivedCRS createLongitudeRotation() {
        final DefaultConversion conversion = DefaultConversionTest.createLongitudeRotation(false);
        return new DefaultDerivedCRS(Map.of(DefaultDerivedCRS.NAME_KEY, conversion.getTargetCRS().getName()),
                (SingleCRS) conversion.getSourceCRS(), conversion, HardCodedCS.GEODETIC_φλ);
    }

    /**
     * Tests the construction of a {@link DefaultDerivedCRS}.
     */
    @Test
    public void testConstruction() {
        final DefaultDerivedCRS crs = createLongitudeRotation();
//      Validators.validate(crs);

        assertEquals("Back to Greenwich",                crs.getName().getCode());
        assertEquals("NTF (Paris)",                      crs.getBaseCRS().getName().getCode());
        assertEquals("Nouvelle Triangulation Française", crs.getDatum().getName().getCode());
        assertSame  (HardCodedCS.GEODETIC_φλ,   crs.getCoordinateSystem());

        final Conversion conversion = crs.getConversionFromBase();
        assertSame(crs.getBaseCRS(), conversion.getSourceCRS());
        assertSame(crs,              conversion.getTargetCRS());
        assertMatrixEquals(new Matrix3(0, 1, 0,
                                       1, 0, 2.33722917,
                                       0, 0, 1),
                MathTransforms.getMatrix(conversion.getMathTransform()), STRICT, "Longitude rotation");
    }

    /**
     * Tests the WKT 1 formatting.
     * Note that in the particular case of {@code DerivedCRS}, WKT 1 and WKT 2 formats are very different.
     *
     * The CRS formatted by this test is a dummy CRS which should not exist in the reality.
     * In particular, we use <q>Longitude rotation</q> (EPSG:9601) as if it was a conversion,
     * while in reality it is a transformation. We do that only because this operation is so simple,
     * it is easy to create and test.
     */
    @Test
    public void testWKT1() {
        assertWktEquals(Convention.WKT1,
                "FITTED_CS[“Back to Greenwich”,\n" +
                "  PARAM_MT[“Affine”,\n" +
                "    PARAMETER[“num_row”, 3],\n"  +
                "    PARAMETER[“num_col”, 3],\n"  +
                "    PARAMETER[“elt_0_0”, 0.0],\n" +
                "    PARAMETER[“elt_0_1”, 1.0],\n" +
                "    PARAMETER[“elt_0_2”, -2.33722917],\n" +
                "    PARAMETER[“elt_1_0”, 1.0],\n" +
                "    PARAMETER[“elt_1_1”, 0.0]],\n" +
                "  GEOGCS[“NTF (Paris)”,\n" +
                "    DATUM[“Nouvelle Triangulation Francaise”,\n" +
                "      SPHEROID[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PRIMEM[“Paris”, 2.33722917],\n" +
                "    UNIT[“degree”, 0.017453292519943295],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]]]",
                createLongitudeRotation());
    }

    /**
     * Tests the WKT 2 formatting.
     * Note that in the particular case of {@code DerivedCRS}, WKT 1 and WKT 2 formats are very different.
     *
     * The CRS formatted by this test is a dummy CRS which should not exist in the reality.
     * In particular, we use <q>Longitude rotation</q> (EPSG:9601) as if it was a conversion,
     * while in reality it is a transformation. We do that only because this operation is so simple,
     * it is easy to create and test.
     */
    @Test
    public void testWKT2() {
        assertWktEquals(Convention.WKT2_2015,
                "GEODCRS[“Back to Greenwich”,\n" +
                "  BASEGEODCRS[“NTF (Paris)”,\n" +
                "    DATUM[“Nouvelle Triangulation Francaise”,\n" +
                "      ELLIPSOID[“NTF”, 6378249.2, 293.4660212936269, LENGTHUNIT[“metre”, 1]]],\n" +
                "      PRIMEM[“Paris”, 2.5969213, ANGLEUNIT[“grad”, 0.015707963267948967]]],\n" +
                "  DERIVINGCONVERSION[“Paris to Greenwich”,\n" +
                "    METHOD[“Longitude rotation”, ID[“EPSG”, 9601]],\n" +
                "    PARAMETER[“Longitude offset”, 2.33722917, ID[“EPSG”, 8602]]],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    AXIS[“Latitude (B)”, north, ORDER[1]],\n" +
                "    AXIS[“Longitude (L)”, east, ORDER[2]],\n" +
                "    ANGLEUNIT[“degree”, 0.017453292519943295]]",
                createLongitudeRotation());
    }

    /**
     * Tests the WKT 2 "simplified" formatting.
     */
    @Test
    public void testWKT2_Simplified() {
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "GeodeticCRS[“Back to Greenwich”,\n" +
                "  BaseGeogCRS[“NTF (Paris)”,\n" +
                "    Datum[“Nouvelle Triangulation Francaise”,\n" +
                "      Ellipsoid[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "      PrimeMeridian[“Paris”, 2.5969213, Unit[“grad”, 0.015707963267948967]],\n" +
                "    Unit[“degree”, 0.017453292519943295]],\n" +
                "  DerivingConversion[“Paris to Greenwich”,\n" +
                "    Method[“Longitude rotation”],\n" +
                "    Parameter[“Longitude offset”, 2.33722917]],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Latitude (B)”, north],\n" +
                "    Axis[“Longitude (L)”, east],\n" +
                "    Unit[“degree”, 0.017453292519943295]]",
                createLongitudeRotation());
    }

    /**
     * Tests (un)marshalling of a derived coordinate reference system.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultDerivedCRS crs = unmarshalFile(DefaultDerivedCRS.class, openTestFile());
        Validators.validate(crs);
        assertEpsgNameAndIdentifierEqual("WGS 84", 4979, crs.getBaseCRS());
        assertAxisDirectionsEqual(crs.getBaseCRS().getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH, AxisDirection.UP);

        final Conversion conversion = crs.getConversionFromBase();
        final ParameterValueGroup pg = conversion.getParameterValues();
        assertEpsgNameAndIdentifierEqual("Geographic/topocentric conversions", 9837, conversion.getMethod());
        assertEquals(55, pg.parameter("Latitude of topocentric origin" ).doubleValue(Units.DEGREE));
        assertEquals( 5, pg.parameter("Longitude of topocentric origin").doubleValue(Units.DEGREE));
        assertEquals( 0, pg.parameter("Ellipsoidal height of topocentric origin").doubleValue(Units.METRE));
        /*
         * Test marshalling and compare with the original file.
         */
        assertMarshalEqualsFile(openTestFile(), crs, "xmlns:*", "xsi:schemaLocation");
    }
}
