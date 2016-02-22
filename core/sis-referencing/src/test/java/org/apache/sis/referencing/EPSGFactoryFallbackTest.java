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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;

// Test dependencies
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link EPSGFactoryFallback} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
  StandardDefinitionsTest.class,
  CommonCRSTest.class
})
public final strictfp class EPSGFactoryFallbackTest extends TestCase {
    /**
     * Tests {@link EPSGFactoryFallback#getAuthorityCodes(Class)}.
     *
     * @throws FactoryException if the set of authority codes can not be fetched.
     */
    @Test
    public void testGetAuthorityCodes() throws FactoryException {
        assertSetEquals(Arrays.asList("4978", "4984", "4936"),
                EPSGFactoryFallback.INSTANCE.getAuthorityCodes(GeocentricCRS.class));
        assertSetEquals(Arrays.asList("4326", "4322", "4047", "4269", "4267", "4258", "4230", "4979", "4985", "4937"),
                EPSGFactoryFallback.INSTANCE.getAuthorityCodes(GeographicCRS.class));
        assertSetEquals(Arrays.asList("5714", "5715", "5703"),
                EPSGFactoryFallback.INSTANCE.getAuthorityCodes(VerticalCRS.class));
    }

    /**
     * Tests {@link EPSGFactoryFallback#createCoordinateReferenceSystem(String)}.
     *
     * @throws FactoryException if a CRS can not be constructed.
     *
     * @see CRSTest#testForEpsgCode()
     * @see CRSTest#testForCrsCode()
     */
    @Test
    public void testCreateCRS() throws FactoryException {
        verifyCreate(CommonCRS.WGS84 .geographic(),            "4326");
        verifyCreate(CommonCRS.WGS72 .geographic(),            "4322");
        verifyCreate(CommonCRS.SPHERE.geographic(),            "4047");
        verifyCreate(CommonCRS.NAD83 .geographic(),            "4269");
        verifyCreate(CommonCRS.NAD27 .geographic(),            "4267");
        verifyCreate(CommonCRS.ETRS89.geographic(),            "4258");
        verifyCreate(CommonCRS.ED50  .geographic(),            "4230");
        verifyCreate(CommonCRS.WGS84 .geocentric(),            "4978");
        verifyCreate(CommonCRS.WGS72 .geocentric(),            "4984");
        verifyCreate(CommonCRS.ETRS89.geocentric(),            "4936");
        verifyCreate(CommonCRS.WGS84 .geographic(),       "EPSG:4326");
        verifyCreate(CommonCRS.WGS72 .geographic(),      "EPSG::4322");
        verifyCreate(CommonCRS.WGS84 .geographic3D(),          "4979");
        verifyCreate(CommonCRS.WGS72 .geographic3D(),          "4985");
        verifyCreate(CommonCRS.ETRS89.geographic3D(),          "4937");
        verifyCreate(CommonCRS.Vertical.MEAN_SEA_LEVEL.crs(),  "5714");
        verifyCreate(CommonCRS.Vertical.DEPTH.crs(),           "5715");
    }

    /**
     * Asserts that the result of {@link CommonCRS#forCode(String, String, FactoryException)} is the given CRS.
     */
    private static void verifyCreate(final SingleCRS expected, final String code) throws FactoryException {
        assertSame(code, expected, EPSGFactoryFallback.INSTANCE.createCoordinateReferenceSystem(code));
    }

    /**
     * Sets the EPSG factory to the given instance and clears the cache of all {@link CommonCRS} enumeration values.
     */
    private static void setEPSGFactory(final CRSAuthorityFactory factory) {
        AuthorityFactories.EPSG(factory);
        for (final CommonCRS          crs : CommonCRS         .values()) crs.clear();
        for (final CommonCRS.Vertical crs : CommonCRS.Vertical.values()) crs.clear();
        for (final CommonCRS.Temporal crs : CommonCRS.Temporal.values()) crs.clear();
    }

    /**
     * Compares all CRS created by {@link EPSGFactoryFallback} with CRS created by the real EPSG database.
     *
     * @throws FactoryException if a CRS can not be constructed.
     */
    @Test
    @DependsOnMethod({"testGetAuthorityCodes", "testCreateCRS"})
    public void compareAllCodes() throws FactoryException {
        final CRSAuthorityFactory EPSG = (CRSAuthorityFactory) AuthorityFactories.EPSG();
        try {
            setEPSGFactory(EPSGFactoryFallback.INSTANCE);
            final ArrayList<String> codes = new ArrayList<String>(EPSGFactoryFallback.INSTANCE.getAuthorityCodes(CoordinateReferenceSystem.class));
            Collections.shuffle(codes, TestUtilities.createRandomNumberGenerator());
            for (final String code : codes) {
                final CoordinateReferenceSystem crs = EPSGFactoryFallback.INSTANCE.createCoordinateReferenceSystem(code);
                final CoordinateReferenceSystem expected = EPSG.createCoordinateReferenceSystem(code);
                assertTrue(code, Utilities.deepEquals(expected, crs, ComparisonMode.DEBUG));
            }
        } finally {
            setEPSGFactory(EPSG);
        }
    }
}
