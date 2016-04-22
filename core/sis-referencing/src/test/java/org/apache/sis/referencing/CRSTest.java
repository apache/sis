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

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;

// Test imports
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link CRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@DependsOn({
    CommonCRSTest.class,
    AuthorityFactoriesTest.class
})
public final strictfp class CRSTest extends TestCase {
    /**
     * Asserts that the result of {@link CRS#forCode(String)} is the given CRS.
     */
    private static void verifyForCode(final SingleCRS expected, final String code) throws FactoryException {
        final CoordinateReferenceSystem actual = CRS.forCode(code);
        assertTrue(code, Utilities.deepEquals(expected, actual, ComparisonMode.DEBUG));
        if (!EPSGFactoryFallback.FORCE_HARDCODED) {
            assertSame(code, expected, actual);
        }
    }

    /**
     * Tests {@link CRS#forCode(String)} with EPSG codes.
     *
     * @throws FactoryException If a CRS can not be constructed.
     *
     * @see CommonCRSTest#testForCode()
     */
    @Test
    public void testForEpsgCode() throws FactoryException {
        verifyForCode(CommonCRS.WGS84 .geographic(),   "EPSG:4326");
        verifyForCode(CommonCRS.WGS84 .geographic(),   "urn:ogc:def:crs:EPSG::4326");
        verifyForCode(CommonCRS.WGS84 .geographic(),   "urn:x-ogc:def:crs:EPSG::4326");
        verifyForCode(CommonCRS.WGS84 .geographic(),   "http://www.opengis.net/gml/srs/epsg.xml#4326");
        verifyForCode(CommonCRS.WGS72 .geographic(),   "EPSG:4322");
        verifyForCode(CommonCRS.SPHERE.geographic(),   "EPSG:4047");
        verifyForCode(CommonCRS.NAD83 .geographic(),   "EPSG:4269");
        verifyForCode(CommonCRS.NAD27 .geographic(),   "EPSG:4267");
        verifyForCode(CommonCRS.ETRS89.geographic(),   "EPSG:4258");
        verifyForCode(CommonCRS.ED50  .geographic(),   "EPSG:4230");
        verifyForCode(CommonCRS.WGS84 .geocentric(),   "EPSG:4978");
        verifyForCode(CommonCRS.WGS72 .geocentric(),   "EPSG:4984");
        verifyForCode(CommonCRS.ETRS89.geocentric(),   "EPSG:4936");
        verifyForCode(CommonCRS.WGS84 .geographic3D(), "EPSG:4979");
        verifyForCode(CommonCRS.WGS72 .geographic3D(), "EPSG:4985");
        verifyForCode(CommonCRS.ETRS89.geographic3D(), "EPSG:4937");
        verifyForCode(CommonCRS.Vertical.MEAN_SEA_LEVEL.crs(), "EPSG:5714");
        verifyForCode(CommonCRS.Vertical.DEPTH.crs(), "EPSG:5715");
    }

    /**
     * Tests {@link CRS#forCode(String)} with CRS codes.
     *
     * @throws FactoryException If a CRS can not be constructed.
     *
     * @see CommonCRSTest#testForCode()
     */
    @Test
    @DependsOnMethod("testForEpsgCode")
    public void testForCrsCode() throws FactoryException {
        verifyForCode(CommonCRS.WGS84.normalizedGeographic(), "CRS:84");
        verifyForCode(CommonCRS.NAD83.normalizedGeographic(), "CRS:83");
        verifyForCode(CommonCRS.NAD27.normalizedGeographic(), "CRS:27");
        verifyForCode(CommonCRS.WGS84.normalizedGeographic(), "http://www.opengis.net/gml/srs/crs.xml#84");
        verifyForCode(CommonCRS.NAD83.normalizedGeographic(), "http://www.opengis.net/gml/srs/crs.xml#83");
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
        assertInstanceOf("GEOGCS", DefaultGeographicCRS.class, crs);
        assertEquals("GCS WGS 1984", crs.getName().getCode());
    }

    /**
     * Tests {@link CRS#isHorizontalCRS(CoordinateReferenceSystem)}.
     */
    @Test
    public void testIsHorizontalCRS() {
        assertFalse(CRS.isHorizontalCRS(HardCodedCRS.TIME));
        assertFalse(CRS.isHorizontalCRS(HardCodedCRS.ELLIPSOIDAL_HEIGHT));
        assertTrue (CRS.isHorizontalCRS(HardCodedCRS.WGS84));
        assertTrue (CRS.isHorizontalCRS(HardCodedCRS.WGS84_φλ));
        assertFalse(CRS.isHorizontalCRS(HardCodedCRS.WGS84_3D));
        assertFalse(CRS.isHorizontalCRS(HardCodedCRS.GEOID_4D));
        assertFalse(CRS.isHorizontalCRS(HardCodedCRS.GEOCENTRIC));
    }

    /**
     * Tests {@link CRS#getHorizontalComponent(CoordinateReferenceSystem)}.
     */
    @Test
    @DependsOnMethod("testIsHorizontalCRS")
    public void testGetHorizontalComponent() {
        assertNull(CRS.getHorizontalComponent(HardCodedCRS.TIME));
        assertNull(CRS.getHorizontalComponent(HardCodedCRS.ELLIPSOIDAL_HEIGHT));
        assertNull(CRS.getHorizontalComponent(HardCodedCRS.GEOCENTRIC));

        assertSame(HardCodedCRS.WGS84,    CRS.getHorizontalComponent(HardCodedCRS.WGS84));
        assertSame(HardCodedCRS.WGS84_φλ, CRS.getHorizontalComponent(HardCodedCRS.WGS84_φλ));

        assertEqualsIgnoreMetadata(HardCodedCRS.WGS84, CRS.getHorizontalComponent(HardCodedCRS.WGS84_3D));
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
    }

    /**
     * Tests {@link CRS#getTemporalComponent(CoordinateReferenceSystem)}.
     */
    @Test
    public void testGetTemporalComponent() {
        assertNull(CRS.getTemporalComponent(HardCodedCRS.ELLIPSOIDAL_HEIGHT));
        assertNull(CRS.getTemporalComponent(HardCodedCRS.WGS84));
        assertNull(CRS.getTemporalComponent(HardCodedCRS.WGS84_φλ));
        assertNull(CRS.getTemporalComponent(HardCodedCRS.WGS84_3D));

        assertSame(HardCodedCRS.TIME, CRS.getTemporalComponent(HardCodedCRS.TIME));
        assertSame(HardCodedCRS.TIME, CRS.getTemporalComponent(HardCodedCRS.GEOID_4D));
    }

    /**
     * Tests {@link CRS#getComponentAt(CoordinateReferenceSystem, int, int)}.
     *
     * @since 0.5
     */
    @Test
    public void testGetComponentAt() {
        testGetComponentAt(
                null, // Null because our CRS has no component for the 'x' axis alone.
                null, // Null because our CRS has no component for the 'y' axis alone.
                HardCodedCRS.GRAVITY_RELATED_HEIGHT,
                HardCodedCRS.TIME,
                HardCodedCRS.WGS84,
                null, // Null because our CRS has no (x,y,z) component.
                HardCodedCRS.GEOID_4D);
        /*
         * The above tests was for the standard (x,y,z,t) flat view.
         * Now test again, but with a more hierarchical structure: ((x,y,z),t)
         */
        testGetComponentAt(
                null, // Null because our CRS has no component for the 'x' axis alone.
                null, // Null because our CRS has no component for the 'y' axis alone.
                HardCodedCRS.GRAVITY_RELATED_HEIGHT,
                HardCodedCRS.TIME,
                HardCodedCRS.WGS84,
                HardCodedCRS.GEOID_3D,
                new DefaultCompoundCRS(IdentifiedObjects.getProperties(HardCodedCRS.GEOID_4D),
                        HardCodedCRS.GEOID_3D, HardCodedCRS.TIME));
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
        assertSame("[0…4]", xyzt, CRS.getComponentAt(xyzt, 0, 4));
        assertSame("[0…3]", xyz,  CRS.getComponentAt(xyzt, 0, 3));
        assertSame("[0…2]", xy,   CRS.getComponentAt(xyzt, 0, 2));
        assertSame("[0…1]", x,    CRS.getComponentAt(xyzt, 0, 1));
        assertSame("[1…2]", y,    CRS.getComponentAt(xyzt, 1, 2));
        assertSame("[2…3]", z,    CRS.getComponentAt(xyzt, 2, 3));
        assertSame("[3…4]", t,    CRS.getComponentAt(xyzt, 3, 4));
        assertNull("[1…3]",       CRS.getComponentAt(xyzt, 1, 3));
        assertNull("[1…4]",       CRS.getComponentAt(xyzt, 1, 4));
        assertNull("[2…4]",       CRS.getComponentAt(xyzt, 2, 4));
        assertNull("[4…4]",       CRS.getComponentAt(xyzt, 4, 4));

        if (xyz != null) {
            assertSame("[0…3]", xyz, CRS.getComponentAt(xyz, 0, 3));
            assertSame("[0…2]", xy,  CRS.getComponentAt(xyz, 0, 2));
            assertSame("[0…1]", x,   CRS.getComponentAt(xyz, 0, 1));
            assertSame("[1…2]", y,   CRS.getComponentAt(xyz, 1, 2));
            assertSame("[2…3]", z,   CRS.getComponentAt(xyz, 2, 3));
        }
        if (xy != null) {
            assertSame("[0…2]", xy, CRS.getComponentAt(xy, 0, 2));
            assertSame("[0…1]", x,  CRS.getComponentAt(xy, 0, 1));
            assertSame("[1…2]", y,  CRS.getComponentAt(xy, 1, 2));
        }
    }

    /**
     * Tests {@link CRS#getGreenwichLongitude(GeodeticCRS)}.
     */
    @Test
    public void testGetGreenwichLongitude() {
        assertEquals(0,          CRS.getGreenwichLongitude(HardCodedCRS.WGS84), STRICT);
        assertEquals(2.33722917, CRS.getGreenwichLongitude(HardCodedCRS.NTF),   1E-12);
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
    }
}
