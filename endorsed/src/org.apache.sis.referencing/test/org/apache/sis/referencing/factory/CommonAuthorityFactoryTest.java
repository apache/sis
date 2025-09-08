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
package org.apache.sis.referencing.factory;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import org.opengis.util.FactoryException;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.datum.Datum;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.provider.TransverseMercator;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSetEquals;
import static org.apache.sis.test.Assertions.assertNotDeepEquals;
import static org.apache.sis.referencing.Assertions.assertWktEqualsRegex;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertAxisDirectionsEqual;


/**
 * Tests {@link CommonAuthorityFactory}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class CommonAuthorityFactoryTest extends TestCase {
    /**
     * The factory to test.
     */
    private final CommonAuthorityFactory factory;

    /**
     * Initializes the factory to test.
     */
    public CommonAuthorityFactoryTest() {
        factory = new CommonAuthorityFactory();
    }

    /**
     * Tests {@link CommonAuthorityFactory#getAuthorityCodes(Class)}.
     *
     * @throws FactoryException if an error occurred while fetching the set of codes.
     */
    @Test
    public void testGetAuthorityCodes() throws FactoryException {
        try {
            assertTrue(factory.getAuthorityCodes(Datum.class).isEmpty());
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        assertSetEquals(List.of("CRS:1", "CRS:27", "CRS:83", "CRS:84", "CRS:88",
                                "AUTO2:42001", "AUTO2:42002", "AUTO2:42003", "AUTO2:42004", "AUTO2:42005",
                                "OGC:JulianDate", "OGC:TruncatedJulianDate", "OGC:UnixTime"),
                factory.getAuthorityCodes(CoordinateReferenceSystem.class));
        assertSetEquals(List.of("AUTO2:42001", "AUTO2:42002", "AUTO2:42003", "AUTO2:42004", "AUTO2:42005"),
                factory.getAuthorityCodes(ProjectedCRS.class));
        assertSetEquals(List.of("CRS:27", "CRS:83", "CRS:84"),
                factory.getAuthorityCodes(GeographicCRS.class));
        assertSetEquals(List.of("CRS:88"),
                factory.getAuthorityCodes(VerticalCRS.class));
        assertSetEquals(List.of("OGC:JulianDate", "OGC:TruncatedJulianDate", "OGC:UnixTime"),
                factory.getAuthorityCodes(TemporalCRS.class));
        assertSetEquals(List.of("CRS:1"),
                factory.getAuthorityCodes(EngineeringCRS.class));

        final Set<String> codes = factory.getAuthorityCodes(GeographicCRS.class);
        assertFalse(codes.contains("CRS:1"));
        assertTrue (codes.contains("CRS:27"));
        assertTrue (codes.contains("CRS:83"));
        assertTrue (codes.contains("CRS:84"));
        assertFalse(codes.contains("CRS:88"));
        assertTrue (codes.contains("0084"));
        assertFalse(codes.contains("0088"));
        assertTrue (codes.contains("OGC:CRS084"));
    }

    /**
     * Verifies that the names of temporal CRS (including namespace)
     * are identical to the one defined by {@link CommonCRS.Temporal}.
     *
     * @throws FactoryException if an error occurred while fetching the set of codes.
     */
    @Test
    public void verifyTemporalCodes() throws FactoryException {
        final Set<String> expected = new HashSet<>();
        for (final CommonCRS.Temporal e : CommonCRS.Temporal.values()) {
            assertTrue(expected.add(IdentifiedObjects.toString(getSingleton(e.crs().getIdentifiers()))));
        }
        assertFalse(expected.isEmpty());
        try {
            for (final String code : factory.getAuthorityCodes(TemporalCRS.class)) {
                assertTrue(expected.remove(code), code);
            }
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        for (final String remaining : expected) {
            assertFalse(remaining.startsWith(Constants.OGC), remaining);
        }
    }

    /**
     * Tests {@link CommonAuthorityFactory#getDescriptionText(Class, String)}.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testDescription() throws FactoryException {
        assertDescriptionEquals("WGS 84",                "CRS:84");
        assertDescriptionEquals("WGS 84 / Auto UTM",     "AUTO:42001");
        assertDescriptionEquals("WGS 84 / UTM zone 10S", "AUTO:42001,-124,-10");
    }

    /**
     * Asserts that the description is equal to the expected value.
     *
     * @param expected  the expected description.
     * @param code      the code of the object for which to fetch the description.
     */
    private void assertDescriptionEquals(String expected, String code) throws FactoryException {
        assertEquals(expected, factory.getDescriptionText(CoordinateReferenceSystem.class, code).orElseThrow().toString());
    }

    /**
     * Checks the value returned by {@link CommonAuthorityFactory#getAuthority()}.
     */
    @Test
    public void testAuthority() {
        final Citation authority = factory.getAuthority();
        assertTrue (Citations.identifierMatches(authority, "WMS"));
        assertFalse(Citations.identifierMatches(authority, "OGP"));
        assertFalse(Citations.identifierMatches(authority, "EPSG"));
        assertEquals(Constants.OGC, Citations.toCodeSpace(authority));
    }

    /**
     * Tests {@link CommonAuthorityFactory#createGeographicCRS(String)} with the {@code "CRS:84"} code.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testCRS84() throws FactoryException {
        GeographicCRS crs = factory.createGeographicCRS("CRS:84");
        assertSame   (crs,  factory.createGeographicCRS("84"));
        assertSame   (crs,  factory.createGeographicCRS("CRS84"));
        assertSame   (crs,  factory.createGeographicCRS("CRS:CRS84"));
        assertSame   (crs,  factory.createGeographicCRS("crs : crs84"));
        assertSame   (crs,  factory.createGeographicCRS("OGC:84"));         // Not in real use as far as I know.
        assertSame   (crs,  factory.createGeographicCRS("OGC:CRS84"));
        assertNotSame(crs,  factory.createGeographicCRS("CRS:83"));
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
    }

    /**
     * Tests {@link CommonAuthorityFactory#createGeographicCRS(String)} with the {@code "CRS:83"} code.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testCRS83() throws FactoryException {
        GeographicCRS crs = factory.createGeographicCRS("CRS:83");
        assertSame   (crs,  factory.createGeographicCRS("83"));
        assertSame   (crs,  factory.createGeographicCRS("CRS83"));
        assertSame   (crs,  factory.createGeographicCRS("CRS:CRS83"));
        assertNotSame(crs,  factory.createGeographicCRS("CRS:84"));
        assertNotDeepEquals(CommonCRS.WGS84.normalizedGeographic(), crs);
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
    }

    /**
     * Tests {@link CommonAuthorityFactory#createVerticalCRS(String)} with the {@code "CRS:88"} code.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testCRS88() throws FactoryException {
        VerticalCRS crs = factory.createVerticalCRS("CRS:88");
        assertSame (crs,  factory.createVerticalCRS("88"));
        assertSame (crs,  factory.createVerticalCRS("CRS88"));
        assertSame (crs,  factory.createVerticalCRS("CRS:CRS 88"));
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.UP);
    }

    /**
     * Tests {@link CommonAuthorityFactory#createEngineeringCRS(String)} with the {@code "CRS:1"} code.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testCRS1() throws FactoryException {
        EngineeringCRS crs = factory.createEngineeringCRS("CRS:1");
        assertSame    (crs,  factory.createEngineeringCRS("1"));
        assertSame    (crs,  factory.createEngineeringCRS("CRS1"));
        assertSame    (crs,  factory.createEngineeringCRS("CRS:CRS 1"));
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.SOUTH);
    }

    /**
     * Tests {@link CommonAuthorityFactory#createProjectedCRS(String)} with the {@code "AUTO:42001"} code.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testAuto42001() throws FactoryException {
        final ProjectedCRS crs = factory.createProjectedCRS("AUTO:42001,-123,0");
        assertSame(crs, factory.createProjectedCRS("AUTO : 42001, -122, 10 "));
        assertSame(crs, factory.createProjectedCRS(" 42001, -122 , 10 "));
        assertSame(crs, factory.createProjectedCRS("AUTO2 :  42001, 1, -122 , 10 "));
        assertSame(crs, factory.createProjectedCRS("AUTO1 :  42001, 9001, -122 , 10 "));
        assertSame(crs, factory.createProjectedCRS("AUTO:42001,9001,-122,10"));
        assertSame(crs, factory.createProjectedCRS("AUTO2:42002,1,-123,0"),
                "When the given parameters match exactly the UTM central meridian and latitude of origin,"
                + " the CRS created by AUTO:42002 should be the same as the CRS created by AUTO:42001.");
        /*
         * Do not use `assertEpsgNameAndIdentifierEqual(…)` because the "EPSG" authority is missing
         * (actually is "Subset of EPSG") if the CRS was built from the fallback factory.
         */
        assertEquals("WGS 84 / UTM zone 10N", crs.getName().getCode());
        assertEquals("32610", getSingleton(crs.getIdentifiers()).getCode());
        final ParameterValueGroup p = crs.getConversionFromBase().getParameterValues();
        assertEquals(TransverseMercator.NAME, crs.getConversionFromBase().getMethod().getName().getCode());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertEquals(-123, p.parameter(Constants.CENTRAL_MERIDIAN)  .doubleValue());
        assertEquals(   0, p.parameter(Constants.LATITUDE_OF_ORIGIN).doubleValue());
        assertEquals(   0, p.parameter(Constants.FALSE_NORTHING)    .doubleValue());
        assertEquals(Units.METRE, crs.getCoordinateSystem().getAxis(0).getUnit());

        var e = assertThrows(NoSuchAuthorityCodeException.class, () -> factory.createObject("AUTO:42001"),
                             "Should not have accepted incomplete code.");
        assertEquals("42001", e.getAuthorityCode());
    }

    /**
     * Tests {@link CommonAuthorityFactory#createProjectedCRS(String)} with the same {@code "AUTO:42001"} code
     * than {@link #testAuto42001()} except that axes are feet.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testAuto42001_foot() throws FactoryException {
        final ProjectedCRS crs = factory.createProjectedCRS("AUTO2:42001, 0.3048, -123, 0");
        assertSame(crs, factory.createProjectedCRS("AUTO:42001,9002,-123,0"), "Legacy namespace.");
        assertEquals("WGS 84 / UTM zone 10N", crs.getName().getCode());
        assertTrue(crs.getIdentifiers().isEmpty(), "Expected no EPSG identifier because the axes are not in metres.");
        assertEquals(Units.FOOT, crs.getCoordinateSystem().getAxis(0).getUnit());
    }

    /**
     * Tests {@link CommonAuthorityFactory#createProjectedCRS(String)} with the {@code "AUTO:42002"} code.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testAuto42002() throws FactoryException {
        final ProjectedCRS crs = factory.createProjectedCRS("AUTO:42002,-122,10");
        assertSame(crs, factory.createProjectedCRS(" 42002, -122 , 10 "), "Omitting namespace.");
        assertSame(crs, factory.createProjectedCRS("AUTO2 :  42002, 1, -122 , 10 "), "With explicit unit.");
        assertEquals("Transverse Mercator", crs.getName().getCode());
        assertTrue(crs.getIdentifiers().isEmpty(), "Expected no EPSG identifier.");

        final ParameterValueGroup p = crs.getConversionFromBase().getParameterValues();
        assertEquals(TransverseMercator.NAME, crs.getConversionFromBase().getMethod().getName().getCode());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertEquals(-122, p.parameter(Constants.CENTRAL_MERIDIAN)  .doubleValue());
        assertEquals(  10, p.parameter(Constants.LATITUDE_OF_ORIGIN).doubleValue());
        assertEquals(   0, p.parameter(Constants.FALSE_NORTHING)    .doubleValue());
    }

    /**
     * Tests {@link CommonAuthorityFactory#createProjectedCRS(String)} with the {@code "AUTO:42003"} code.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    @Disabled("Pending the port of Orthographic projection.")
    public void testAuto42003() throws FactoryException {
        final ProjectedCRS crs = factory.createProjectedCRS("AUTO:42003,9001,10,45");
        final ParameterValueGroup p = crs.getConversionFromBase().getParameterValues();
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertEquals(10, p.parameter(Constants.CENTRAL_MERIDIAN)  .doubleValue());
        assertEquals(45, p.parameter(Constants.LATITUDE_OF_ORIGIN).doubleValue());
    }

    /**
     * Tests {@link CommonAuthorityFactory#createProjectedCRS(String)} with the {@code "AUTO:42004"} code.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testAuto42004() throws FactoryException {
        final ProjectedCRS crs = factory.createProjectedCRS("AUTO2:42004,1,10,45");
        final ParameterValueGroup p = crs.getConversionFromBase().getParameterValues();
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertEquals(10, p.parameter(Constants.CENTRAL_MERIDIAN)   .doubleValue());
        assertEquals(45, p.parameter(Constants.STANDARD_PARALLEL_1).doubleValue());
        assertInstanceOf(LinearTransform.class, crs.getConversionFromBase().getMathTransform(),
                "Opportunistic check: in the special case of Equirectangular projection, "
                + "SIS should have optimized the MathTransform as an affine transform.");
    }

    /**
     * Tests {@link CommonAuthorityFactory#createProjectedCRS(String)} with the {@code "AUTO:42005"} code.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testAuto42005() throws FactoryException {
        final ProjectedCRS crs = factory.createProjectedCRS("AUTO:42005,9001,10,45");
        final ParameterValueGroup p = crs.getConversionFromBase().getParameterValues();
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.EAST, AxisDirection.NORTH);
        assertEquals(10, p.parameter(Constants.CENTRAL_MERIDIAN)  .doubleValue());
    }

    /**
     * Tests two {@code "AUTO:42004"} (Equirectangular projection) case built in such a way that the conversion
     * from one to the other should be the conversion factor from metres to feet.
     *
     * This is an integration test.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     * @throws NoninvertibleTransformException Should never happen.
     */
    @Test
    public void testUnits() throws FactoryException, NoninvertibleTransformException {
        AffineTransform tr1, tr2;
        tr1 = (AffineTransform) factory.createProjectedCRS("AUTO:42004,9001,0,35").getConversionFromBase().getMathTransform();
        tr2 = (AffineTransform) factory.createProjectedCRS("AUTO:42004,9002,0,35").getConversionFromBase().getMathTransform();
        tr2 = tr2.createInverse();
        tr2.concatenate(tr1);
        assertEquals(0, tr2.getType() & ~AffineTransform.TYPE_MASK_SCALE, "Expected any kind of scale.");
        assertEquals(0.3048, tr2.getScaleX(), 1E-9, "Expected the conversion factor from foot to metre.");
        assertEquals(0.3048, tr2.getScaleY(), 1E-9, "Expected the conversion factor from foot to metre.");
    }

    /**
     * Tests the WKT formatting. The main purpose of this test is to ensure that
     * the authority name is "CRS" and not "Web Map Service CRS".
     *
     * @throws FactoryException if an error occurred while creating the CRS.
     */
    @Test
    public void testWKT() throws FactoryException {
        final String WGS84 = "“WGS\\E\\s?(?:19)?\\Q84”";                // Accept "WGS 84" or "WGS 1984"
        GeographicCRS crs = factory.createGeographicCRS("CRS:84");
        assertWktEqualsRegex(Convention.WKT1, "(?m)\\Q" +               // Multilines
                "GEOGCS[" + WGS84 + ",\n" +
                "  DATUM[“World Geodetic System 1984\\E\\s?\\w*\\Q”,\n" +
                "    SPHEROID[" + WGS84 + ", 6378137.0, 298.257223563]],\n" +
                "    PRIMEM[“Greenwich”, 0.0],\n" +
                "  UNIT[“degree”, 0.017453292519943295],\n" +
                "  AXIS[“Longitude”, EAST],\n" +
                "  AXIS[“Latitude”, NORTH],\n" +
                "  AUTHORITY[“CRS”, “84”]]\\E", crs);

        assertWktEqualsRegex(Convention.WKT2_2015, "(?m)\\Q" +
                "GEODCRS[" + WGS84 + ",\n" +
                "  DATUM[“World Geodetic System 1984\\E\\s?\\w*\\Q”,\n" +
                "    ELLIPSOID[" + WGS84 + ", 6378137.0, 298.257223563, LENGTHUNIT[“metre”, 1]]],\n" +
                "    PRIMEM[“Greenwich”, 0.0, ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    AXIS[“Longitude (L)”, east, ORDER[1]],\n" +
                "    AXIS[“Latitude (B)”, north, ORDER[2]],\n" +
                "    ANGLEUNIT[“degree”, 0.017453292519943295],\n" +
                "\\E(?:  SCOPE\\[“.+”\\],\n)?\\Q" +                     // Ignore SCOPE[…] if present.
                "  AREA[“World\\E.*\\Q”],\n" +
                "  BBOX[-90.00, -180.00, 90.00, 180.00],\n" +
                "  ID[“CRS”, 84, CITATION[“WMS”], URI[“urn:ogc:def:crs:OGC:1.3:CRS84”]]" +
                "\\E(?:,\n  REMARK\\[“.+”\\])?\\]",                     // Ignore trailing REMARK[…] if present.
                crs);
        /*
         * Note: the WKT specification defines the ID element as:
         *
         *     ID[authority, code, (version), (authority citation), (URI)]
         *
         * where everything after the code is optional. The difference between "authority" and "authority citation"
         * is unclear. The only example found in OGC 12-063r5 uses CITATION[…] as the source of an EPSG definition
         * (so we could almost said "the authority of the authority").
         */
    }
}
