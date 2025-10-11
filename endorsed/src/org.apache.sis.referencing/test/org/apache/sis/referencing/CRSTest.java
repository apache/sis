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
import java.util.Arrays;
import java.util.List;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.CartesianCS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCaseWithLogs;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;
import static org.apache.sis.test.Assertions.assertMessageContains;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.ObjectDomain;


/**
 * Tests the {@link CRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
@SuppressWarnings("exports")
public final class CRSTest extends TestCaseWithLogs {
    /**
     * Creates a new test case.
     */
    public CRSTest() {
        super(Loggers.CRS_FACTORY);
    }

    /**
     * Asserts that the result of {@link CRS#forCode(String)} is the given CRS.
     */
    private static void verifyForCode(final SingleCRS expected, final String code) throws FactoryException {
        final CoordinateReferenceSystem actual = CRS.forCode(code);
        assertTrue(Utilities.deepEquals(expected, actual, ComparisonMode.DEBUG), code);
        assertSame(expected, actual, code);
    }

    /**
     * Tests {@link CRS#forCode(String)} with EPSG codes.
     * The codes tested by this method shall be in the list of EPSG codes
     * for which Apache SIS has hard-coded fallbacks to use if no EPSG database is available.
     *
     * @throws FactoryException if a CRS cannot be constructed.
     *
     * @see EPSGFactoryFallbackTest#testCreateCRS()
     */
    @Test
    public void testForEpsgCode() throws FactoryException {
        verifyForCode(CommonCRS.WGS84 .geographic(),           "EPSG:4326");
        verifyForCode(CommonCRS.WGS84 .geographic(),           "urn:ogc:def:crs:EPSG::4326");
        verifyForCode(CommonCRS.WGS84 .geographic(),           "urn:x-ogc:def:crs:EPSG::4326");
        verifyForCode(CommonCRS.WGS84 .geographic(),           "http://www.opengis.net/gml/srs/epsg.xml#4326");
        verifyForCode(CommonCRS.WGS72 .geographic(),           "EPSG:4322");
        verifyForCode(CommonCRS.SPHERE.geographic(),           "EPSG:4047");
        verifyForCode(CommonCRS.NAD83 .geographic(),           "EPSG:4269");
        verifyForCode(CommonCRS.NAD27 .geographic(),           "EPSG:4267");
        verifyForCode(CommonCRS.ETRS89.geographic(),           "EPSG:4258");
        verifyForCode(CommonCRS.ED50  .geographic(),           "EPSG:4230");
        verifyForCode(CommonCRS.WGS84 .geocentric(),           "EPSG:4978");
        verifyForCode(CommonCRS.WGS72 .geocentric(),           "EPSG:4984");
        verifyForCode(CommonCRS.ETRS89.geocentric(),           "EPSG:4936");
        verifyForCode(CommonCRS.WGS84 .geographic3D(),         "EPSG:4979");
        verifyForCode(CommonCRS.WGS72 .geographic3D(),         "EPSG:4985");
        verifyForCode(CommonCRS.ETRS89.geographic3D(),         "EPSG:4937");
        verifyForCode(CommonCRS.WGS84 .universal(88, 120),     "EPSG:5041");
        verifyForCode(CommonCRS.WGS84 .universal(-40, 2),      "EPSG:32731");
        verifyForCode(CommonCRS.Vertical.MEAN_SEA_LEVEL.crs(), "EPSG:5714");
        verifyForCode(CommonCRS.Vertical.DEPTH.crs(),          "EPSG:5715");

        loggings.skipNextLogIfContains("EPSG:6047");
        loggings.skipNextLogIfContains("EPSG:4047");    // No longer supported by EPSG.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#forCode(String)} with CRS codes.
     *
     * @throws FactoryException if a CRS cannot be constructed.
     *
     * @see EPSGFactoryFallbackTest#testCreateCRS()
     */
    @Test
    public void testForCrsCode() throws FactoryException {
        verifyForCode(CommonCRS.WGS84.normalizedGeographic(), "CRS:84");
        verifyForCode(CommonCRS.NAD83.normalizedGeographic(), "CRS:83");
        verifyForCode(CommonCRS.NAD27.normalizedGeographic(), "CRS:27");
        verifyForCode(CommonCRS.WGS84.normalizedGeographic(), "http://www.opengis.net/gml/srs/crs.xml#84");
        verifyForCode(CommonCRS.NAD83.normalizedGeographic(), "http://www.opengis.net/gml/srs/crs.xml#83");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#forCode(String)} with temporal CRS codes.
     *
     * @throws FactoryException if a CRS cannot be constructed.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-490">SIS-490</a>
     */
    @Test
    public void testForTemporalCode() throws FactoryException {
        verifyForCode(CommonCRS.Temporal.JULIAN.crs(), "OGC:JulianDate");
        verifyForCode(CommonCRS.Temporal.UNIX.crs(),   "OGC:UnixTime");
        verifyForCode(CommonCRS.Temporal.JULIAN.crs(), "urn:ogc:def:crs:OGC::JulianDate");
        verifyForCode(CommonCRS.Temporal.TRUNCATED_JULIAN.crs(),
                      "http://www.opengis.net/gml/srs/crs.xml#TruncatedJulianDate");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Test {@link CRS#forCode(String)} with values that should be invalid.
     *
     * @throws FactoryException if an error other than {@link NoSuchAuthorityCodeException} happened.
     */
    @Test
    public void testForInvalidCode() throws FactoryException {
        var e = assertThrows(NoSuchAuthorityCodeException.class, () -> CRS.forCode("EPSG:4"));
        assertEquals("4", e.getAuthorityCode());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Asserts that the result of {@link CRS#forCode(String)} for a compound CRS are the given components.
     */
    private static void verifyForCompoundCode(final String code, final SingleCRS... expected) throws FactoryException {
        final List<SingleCRS> components = CRS.getSingleComponents(CRS.forCode(code));
        final int count = Math.min(components.size(), expected.length);
        for (int i=0; i<count; i++) {
            assertTrue(Utilities.deepEquals(expected[i], components.get(i), ComparisonMode.DEBUG), String.valueOf(i));
        }
        assertEquals(expected.length, components.size());
    }

    /**
     * Tests {@link CRS#forCode(String)} with compound CRS codes.
     *
     * @throws FactoryException if a CRS cannot be constructed.
     */
    @Test
    public void testForCompoundCode() throws FactoryException {
        verifyForCompoundCode("urn:ogc:def:crs,crs:EPSG::4326,crs:EPSG::5714",
                CommonCRS.WGS84.geographic(), CommonCRS.Vertical.MEAN_SEA_LEVEL.crs());
        verifyForCompoundCode("urn:ogc:def:crs,crs:EPSG::4326,crs:EPSG::5714,crs:OGC::TruncatedJulianDate",
                CommonCRS.WGS84.geographic(), CommonCRS.Vertical.MEAN_SEA_LEVEL.crs(), CommonCRS.Temporal.TRUNCATED_JULIAN.crs());

        verifyForCompoundCode("http://www.opengis.net/def/crs-compound?" +
                            "1=http://www.opengis.net/def/crs/epsg/0/4326&" +
                            "2=http://www.opengis.net/def/crs/epsg/0/5715",
                CommonCRS.WGS84.geographic(), CommonCRS.Vertical.DEPTH.crs());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests simple WKT parsing. It is not the purpose of this class to test extensively the WKT parser;
     * those tests are rather done by {@link org.apache.sis.io.wkt.GeodeticObjectParserTest}.
     * Here we merely test that {@link CRS#fromWKT(String)} is connected to the parser.
     *
     * @throws FactoryException if an error occurred while parsing the WKT.
     */
    @Test
    public void testFromWKT() throws FactoryException {
        final CoordinateReferenceSystem crs = CRS.fromWKT(
                "GEOGCS[\"GCS WGS 1984\","
                + "DATUM[\"WGS 1984\",SPHEROID[\"WGS 1984\",6378137,298.257223563]],"
                + "PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433]]");
        assertInstanceOf(DefaultGeographicCRS.class, crs);
        assertEquals("GCS WGS 1984", crs.getName().getCode());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Verifies that parsing a WKT with an unknown operation method throws {@link NoSuchIdentifierException}.
     *
     * @throws FactoryException if an unexpected error occurred.
     */
    @Test
    public void testFromInvalidWKT() throws FactoryException {
        var e = assertThrows(NoSuchIdentifierException.class, () ->
                CRS.fromWKT("PROJCS[\"Foo\", GEOGCS[\"Foo\", DATUM[\"Foo\", SPHEROID[\"Sphere\", 6371000, 0]], " +
                            "UNIT[\"Degree\", 0.0174532925199433]], PROJECTION[\"I do not exist\"], " +
                            "UNIT[\"MEtre\", 1]]"));

        assertMessageContains(e, "I do not exist");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#suggestCommonTarget(GeographicBoundingBox, CoordinateReferenceSystem...)}.
     */
    @Test
    public void testSuggestCommonTarget() {
        /*
         * Prepare 4 CRS with different datum (so we can more easily differentiate them in the assertions) and
         * different domain of validity. CRS[1] is given a domain large enough for all CRS except the last one.
         */
        final Map<String,Object> properties = new HashMap<>(4);
        final CartesianCS cs = HardCodedCS.PROJECTED;
        final ProjectedCRS[] crs = new ProjectedCRS[4];
        for (int i=0; i<crs.length; i++) {
            final CommonCRS baseCRS;
            final double ymin, ymax;
            switch (i) {
                case 0: baseCRS = CommonCRS.WGS84;  ymin = 2; ymax = 4; break;
                case 1: baseCRS = CommonCRS.WGS72;  ymin = 1; ymax = 4; break;
                case 2: baseCRS = CommonCRS.SPHERE; ymin = 2; ymax = 3; break;
                case 3: baseCRS = CommonCRS.NAD27;  ymin = 3; ymax = 5; break;
                default: throw new AssertionError(i);
            }
            properties.put(DefaultProjectedCRS.NAME_KEY, "CRS #" + i);
            properties.put(ObjectDomain.DOMAIN_OF_VALIDITY_KEY, new DefaultExtent(
                    null, new DefaultGeographicBoundingBox(-1, +1, ymin, ymax), null, null));
            crs[i] = new DefaultProjectedCRS(properties, baseCRS.geographic(), HardCodedConversions.MERCATOR, cs);
        }
        loggings.skipNextLogIfContains("EPSG:6047");                        // Datum of EPSG:4047.
        loggings.skipNextLogIfContains("EPSG:4047");                        // No longer supported by EPSG.
        final ProjectedCRS[] overlappingCRS = Arrays.copyOf(crs, 3);        // Exclude the last CRS only.
        /*
         * Test between the 3 overlapping CRS without region of interest. We expect the CRS having a domain
         * of validity large enough for all CRS; this is the second CRS created in above `switch` statement.
         */
        assertSame(crs[1], CRS.suggestCommonTarget(null, overlappingCRS));
        /*
         * If we specify a smaller region of interest, we should get the CRS having the smallest domain of validity that
         * cover the ROI. Following lines gradually increase the ROI size and verify that we get CRS for larger domain.
         */
        final var regionOfInterest = new DefaultGeographicBoundingBox(-1, +1, 2.1, 2.9);
        assertSame(crs[2], CRS.suggestCommonTarget(regionOfInterest, overlappingCRS));      // Best fit for [2.1 … 2.9]°N

        regionOfInterest.setNorthBoundLatitude(3.1);
        assertSame(crs[0], CRS.suggestCommonTarget(regionOfInterest, overlappingCRS));      // Best fit for [2.1 … 3.1]°N

        regionOfInterest.setSouthBoundLatitude(1.9);
        assertSame(crs[1], CRS.suggestCommonTarget(regionOfInterest, overlappingCRS));      // Best fit for [1.9 … 3.1]°N
        /*
         * All above tests returned one of the CRS in the given array. Test now a case where none of those CRS
         * have a domain of validity wide enough, so suggestCommonTarget(…) need to search among the base CRS.
         * We expect a GeodeticCRS since none of the ProjectedCRS have a domain of validity wide enough.
         */
        assertSame(crs[0].getBaseCRS(), CRS.suggestCommonTarget(null, crs));
        /*
         * With the same domain of validity than above, suggestCommonTarget(…) should not need to fallback on the
         * base CRS anymore.
         */
        assertSame(crs[1], CRS.suggestCommonTarget(regionOfInterest, crs));         // Best fit for [1.9 … 3.1]°N
        final ProjectedCRS utm13N = CommonCRS.WGS84.universal(20, 13);
        final ProjectedCRS utm42S = CommonCRS.WGS84.universal(-2, 42);

        // CRS suggestion should fallback on common base geographic system when possible.
        assertSame(CommonCRS.WGS84.geographic(), CRS.suggestCommonTarget(null, utm13N, utm42S));

        // Disjoint systems should return a geographic suggestion when possible.
        assertNotNull(CRS.suggestCommonTarget(null,
                CommonCRS.WGS84.universal(-7,  19),
                CommonCRS.NAD27.universal(20, -101),
                CommonCRS.NAD27.universal(18, -20))
        );
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#isHorizontalCRS(CoordinateReferenceSystem)}.
     */
    @Test
    public void testIsHorizontalCRS() {
        assertFalse(CRS.isHorizontalCRS(HardCodedCRS.TIME));
        assertFalse(CRS.isHorizontalCRS(HardCodedCRS.ELLIPSOIDAL_HEIGHT));
        assertTrue (CRS.isHorizontalCRS(HardCodedCRS.WGS84));
        assertTrue (CRS.isHorizontalCRS(HardCodedCRS.WGS84_LATITUDE_FIRST));
        assertFalse(CRS.isHorizontalCRS(HardCodedCRS.WGS84_3D));
        assertFalse(CRS.isHorizontalCRS(HardCodedCRS.GEOID_4D));
        assertFalse(CRS.isHorizontalCRS(HardCodedCRS.GEOCENTRIC));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#getHorizontalComponent(CoordinateReferenceSystem)}.
     */
    @Test
    public void testGetHorizontalComponent() {
        assertNull(CRS.getHorizontalComponent(HardCodedCRS.TIME));
        assertNull(CRS.getHorizontalComponent(HardCodedCRS.ELLIPSOIDAL_HEIGHT));
        assertNull(CRS.getHorizontalComponent(HardCodedCRS.GEOCENTRIC));

        assertSame(HardCodedCRS.WGS84,                 CRS.getHorizontalComponent(HardCodedCRS.WGS84));
        assertSame(HardCodedCRS.WGS84_LATITUDE_FIRST,  CRS.getHorizontalComponent(HardCodedCRS.WGS84_LATITUDE_FIRST));
        assertEqualsIgnoreMetadata(HardCodedCRS.WGS84, CRS.getHorizontalComponent(HardCodedCRS.WGS84_3D));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#getVerticalComponent(CoordinateReferenceSystem, boolean)}.
     */
    @Test
    public void testGetVerticalComponent() {
        assertNull(CRS.getVerticalComponent(HardCodedCRS.TIME,  false));
        assertNull(CRS.getVerticalComponent(HardCodedCRS.TIME,  true));
        assertNull(CRS.getVerticalComponent(HardCodedCRS.WGS84, false));
        assertNull(CRS.getVerticalComponent(HardCodedCRS.WGS84, true));

        assertSame(HardCodedCRS.ELLIPSOIDAL_HEIGHT,     CRS.getVerticalComponent(HardCodedCRS.ELLIPSOIDAL_HEIGHT, false));
        assertSame(HardCodedCRS.ELLIPSOIDAL_HEIGHT,     CRS.getVerticalComponent(HardCodedCRS.ELLIPSOIDAL_HEIGHT, true));
        assertSame(HardCodedCRS.GRAVITY_RELATED_HEIGHT, CRS.getVerticalComponent(HardCodedCRS.GEOID_4D, false));
        assertSame(HardCodedCRS.GRAVITY_RELATED_HEIGHT, CRS.getVerticalComponent(HardCodedCRS.GEOID_4D, true));

        assertNull(CRS.getVerticalComponent(HardCodedCRS.WGS84_3D, false));
        assertEqualsIgnoreMetadata(HardCodedCRS.ELLIPSOIDAL_HEIGHT,
                CRS.getVerticalComponent(HardCodedCRS.WGS84_3D, true));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#getTemporalComponent(CoordinateReferenceSystem)}.
     */
    @Test
    public void testGetTemporalComponent() {
        assertNull(CRS.getTemporalComponent(HardCodedCRS.ELLIPSOIDAL_HEIGHT));
        assertNull(CRS.getTemporalComponent(HardCodedCRS.WGS84));
        assertNull(CRS.getTemporalComponent(HardCodedCRS.WGS84_LATITUDE_FIRST));
        assertNull(CRS.getTemporalComponent(HardCodedCRS.WGS84_3D));

        assertSame(HardCodedCRS.TIME, CRS.getTemporalComponent(HardCodedCRS.TIME));
        assertSame(HardCodedCRS.TIME, CRS.getTemporalComponent(HardCodedCRS.GEOID_4D));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests getting the horizontal and vertical components of a three-dimensional projected CRS.
     */
    @Test
    public void testComponentsOfProjectedCRS() {
        final ProjectedCRS volumetric = HardCodedConversions.mercator3D();
        assertFalse(CRS.isHorizontalCRS(volumetric));
        assertNull(CRS.getTemporalComponent(volumetric));
        assertNull(CRS.getVerticalComponent(volumetric, false));
        assertEqualsIgnoreMetadata(HardCodedCRS.ELLIPSOIDAL_HEIGHT, CRS.getVerticalComponent(volumetric, true));
        final SingleCRS horizontal = CRS.getHorizontalComponent(volumetric);
        assertInstanceOf(ProjectedCRS.class, horizontal);
        assertEquals(2, horizontal.getCoordinateSystem().getDimension());
        assertTrue(CRS.isHorizontalCRS(horizontal));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#getComponentAt(CoordinateReferenceSystem, int, int)}.
     */
    @Test
    public void testGetComponentAt() {
        testGetComponentAt(
                null,                                 // Null because our CRS has no component for the `x` axis alone.
                null,                                 // Null because our CRS has no component for the `y` axis alone.
                HardCodedCRS.GRAVITY_RELATED_HEIGHT,
                HardCodedCRS.TIME,
                HardCodedCRS.WGS84,
                null,                                 // Null because our CRS has no (x,y,z) component.
                HardCodedCRS.GEOID_4D);
        /*
         * The above tests was for the standard (x,y,z,t) flat view.
         * Now test again, but with a more hierarchical structure: ((x,y,z),t)
         */
        testGetComponentAt(
                null,                                 // Null because our CRS has no component for the `x` axis alone.
                null,                                 // Null because our CRS has no component for the `y` axis alone.
                HardCodedCRS.GRAVITY_RELATED_HEIGHT,
                HardCodedCRS.TIME,
                HardCodedCRS.WGS84,
                HardCodedCRS.GEOID_3D,
                HardCodedCRS.NESTED);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#selectDimensions(CoordinateReferenceSystem, int[])} in the simpler case
     * where there is no three-dimensional geographic CRS to separate.
     *
     * @throws FactoryException if an error occurred while creating a compound CRS.
     */
    @Test
    public void testSelectDimensions() throws FactoryException {
        assertSame(HardCodedCRS.TIME,                     CRS.selectDimensions(HardCodedCRS.GEOID_4D, 3));
        assertSame(HardCodedCRS.GRAVITY_RELATED_HEIGHT,   CRS.selectDimensions(HardCodedCRS.GEOID_4D, 2));
        assertSame(HardCodedCRS.WGS84,                    CRS.selectDimensions(HardCodedCRS.GEOID_4D, 0, 1));
        assertSame(HardCodedCRS.GEOID_4D,                 CRS.selectDimensions(HardCodedCRS.GEOID_4D, 0, 1, 2, 3));
        assertSame(HardCodedCRS.NESTED,                   CRS.selectDimensions(HardCodedCRS.NESTED,   0, 1, 2, 3));
        assertSame(HardCodedCRS.GEOID_3D,                 CRS.selectDimensions(HardCodedCRS.NESTED,   0, 1, 2));
        assertEqualsIgnoreMetadata(HardCodedCRS.GEOID_3D, CRS.selectDimensions(HardCodedCRS.GEOID_4D, 0, 1, 2));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#selectDimensions(CoordinateReferenceSystem, int[])} with
     * a three-dimensional geographic CRS to be reduced to a two-dimensional CRS.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testReduceGeographic3D() throws FactoryException {
        final GeographicCRS crs = HardCodedCRS.WGS84_3D;
        assertSame(CommonCRS.Vertical.ELLIPSOIDAL.crs(),   CRS.selectDimensions(crs, 2));
        assertSame(CommonCRS.WGS84.normalizedGeographic(), CRS.selectDimensions(crs, 0, 1));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#selectDimensions(CoordinateReferenceSystem, int[])} with
     * a three-dimensional projected CRS to be reduced to a two-dimensional CRS.
     *
     * @throws FactoryException if an error occurred while creating a CRS.
     */
    @Test
    public void testReduceProjected3D() throws FactoryException {
        final ProjectedCRS crs = HardCodedConversions.mercator3D();
        assertSame(CommonCRS.Vertical.ELLIPSOIDAL.crs(), CRS.selectDimensions(crs, 2));
        assertEqualsIgnoreMetadata(HardCodedConversions.mercator(), CRS.selectDimensions(crs, 0, 1));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#compound(CoordinateReferenceSystem...)}.
     *
     * @throws FactoryException if an error occurred while creating a compound CRS.
     */
    @Test
    public void testCompound() throws FactoryException {
        var e = assertThrows(IllegalArgumentException.class, () -> CRS.compound());
        assertMessageContains(e, "components");
        assertSame(HardCodedCRS.WGS84, CRS.compound(HardCodedCRS.WGS84));
        assertEqualsIgnoreMetadata(HardCodedCRS.WGS84_3D, CRS.compound(HardCodedCRS.WGS84, HardCodedCRS.ELLIPSOIDAL_HEIGHT));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRS#getComponentAt(CoordinateReferenceSystem, int, int)} on a (x,y,z,t)
     * coordinate reference system having 4 dimensions. All arguments given to this method
     * except the last one are the expected components, which may be {@code null}.
     */
    private static void testGetComponentAt(
            final CoordinateReferenceSystem x,
            final CoordinateReferenceSystem y,
            final CoordinateReferenceSystem z,
            final CoordinateReferenceSystem t,
            final CoordinateReferenceSystem xy,
            final CoordinateReferenceSystem xyz,
            final CoordinateReferenceSystem xyzt)
    {
        assertSame(xyzt, CRS.getComponentAt(xyzt, 0, 4));
        assertSame(xyz,  CRS.getComponentAt(xyzt, 0, 3));
        assertSame(xy,   CRS.getComponentAt(xyzt, 0, 2));
        assertSame(x,    CRS.getComponentAt(xyzt, 0, 1));
        assertSame(y,    CRS.getComponentAt(xyzt, 1, 2));
        assertSame(z,    CRS.getComponentAt(xyzt, 2, 3));
        assertSame(t,    CRS.getComponentAt(xyzt, 3, 4));
        assertNull(      CRS.getComponentAt(xyzt, 1, 3));
        assertNull(      CRS.getComponentAt(xyzt, 1, 4));
        assertNull(      CRS.getComponentAt(xyzt, 2, 4));
        assertNull(      CRS.getComponentAt(xyzt, 4, 4));

        if (xyz != null) {
            assertSame(xyz, CRS.getComponentAt(xyz, 0, 3));
            assertSame(xy,  CRS.getComponentAt(xyz, 0, 2));
            assertSame(x,   CRS.getComponentAt(xyz, 0, 1));
            assertSame(y,   CRS.getComponentAt(xyz, 1, 2));
            assertSame(z,   CRS.getComponentAt(xyz, 2, 3));
        }
        if (xy != null) {
            assertSame(xy, CRS.getComponentAt(xy, 0, 2));
            assertSame(x,  CRS.getComponentAt(xy, 0, 1));
            assertSame(y,  CRS.getComponentAt(xy, 1, 2));
        }
    }

    /**
     * Tests {@link CRS#getGreenwichLongitude(GeodeticCRS)}.
     */
    @Test
    public void testGetGreenwichLongitude() {
        assertEquals(0,          CRS.getGreenwichLongitude(HardCodedCRS.WGS84));
        assertEquals(2.33722917, CRS.getGreenwichLongitude(HardCodedCRS.NTF), 1E-12);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link IdentifiedObjects#lookupEPSG(IdentifiedObject)} and
     * {@link IdentifiedObjects#lookupURN(IdentifiedObject, Citation)}.
     *
     * @throws FactoryException if an error occurred during the lookup.
     */
    @Test
    public void testIdentifiedObjectLookup() throws FactoryException {
        IdentifiedObjectsTest.testLookupEPSG();
        IdentifiedObjectsTest.testLookupWMS();
        loggings.assertNoUnexpectedLog();
    }
}
