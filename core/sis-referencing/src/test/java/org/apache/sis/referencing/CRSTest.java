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
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.crs.HardCodedCRS;
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
 * @version 0.4
 * @module
 */
@DependsOn({
    CommonCRSTest.class
})
public final strictfp class CRSTest extends TestCase {
    /**
     * Asserts that the result of {@link CRS#forCode(String)} is the given CRS.
     */
    private static void verifyForCode(final SingleCRS expected, final String code) throws FactoryException {
        assertSame(code, expected, CRS.forCode(code));
    }

    /**
     * Tests {@link CRS#forCode(String)} with EPSG codes.
     *
     * @throws FactoryException If a CRS can not be constructed.
     */
    @Test
    public void testForEpsgCode() throws FactoryException {
        verifyForCode(CommonCRS.WGS84 .geographic(),   "EPSG:4326");
        verifyForCode(CommonCRS.WGS84 .geographic(),   "urn:ogc:def:crs:EPSG::4326");
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
     */
    @Test
    @DependsOnMethod("testForEpsgCode")
    public void testForCrsCode() throws FactoryException {
        verifyForCode(CommonCRS.WGS84.normalizedGeographic(), "CRS:84");
        verifyForCode(CommonCRS.NAD83.normalizedGeographic(), "CRS:83");
        verifyForCode(CommonCRS.NAD27.normalizedGeographic(), "CRS:27");
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
}
