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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.system.Configuration;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.BackingStoreException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.Isolated;
import org.apache.sis.test.TestCaseWithLogs;
import org.apache.sis.test.TestUtilities;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;
import static org.apache.sis.test.Assertions.assertSetEquals;


/**
 * Tests the {@link EPSGFactoryFallback} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@Isolated("Temporarily modifies the system-wide EPSG factory.")
public final class EPSGFactoryFallbackTest extends TestCaseWithLogs {
    /**
     * Creates a new test case.
     */
    public EPSGFactoryFallbackTest() {
        super(Loggers.CRS_FACTORY);
    }

    /**
     * Tests {@link EPSGFactoryFallback#getAuthorityCodes(Class)}.
     *
     * @throws FactoryException if the set of authority codes cannot be fetched.
     */
    @Test
    public void testGetAuthorityCodes() throws FactoryException {
        assertSetEquals(List.of(StandardDefinitions.GREENWICH),
                EPSGFactoryFallback.INSTANCE.getAuthorityCodes(PrimeMeridian.class));
        assertSetEquals(List.of("7030", "7043", "7019", "7008", "7022", "7048"),
                EPSGFactoryFallback.INSTANCE.getAuthorityCodes(Ellipsoid.class));
        assertSetEquals(List.of("6326", "6322", "6269", "6267", "6258", "6230", "6019", "6047", "5100", "5103"),
                EPSGFactoryFallback.INSTANCE.getAuthorityCodes(Datum.class));
        assertSetEquals(List.of("6422", "6423"),
                EPSGFactoryFallback.INSTANCE.getAuthorityCodes(EllipsoidalCS.class));
        assertSetEquals(List.of("6404"),
                EPSGFactoryFallback.INSTANCE.getAuthorityCodes(SphericalCS.class));
        assertSetEquals(List.of("6500", "4400", "1026", "1027"),
                EPSGFactoryFallback.INSTANCE.getAuthorityCodes(CartesianCS.class));
        assertSetEquals(List.of("4326", "4322", "4019", "4047", "4269", "4267", "4258", "4230", "4979", "4985", "4937"),
                EPSGFactoryFallback.INSTANCE.getAuthorityCodes(GeographicCRS.class));
        assertSetEquals(List.of("5714", "5715", "5703"),
                EPSGFactoryFallback.INSTANCE.getAuthorityCodes(VerticalCRS.class));
        /*
         * Geodetic CRS include geographic CRS with the addition of the following.
         */
        Set<String> geographic = EPSGFactoryFallback.INSTANCE.getAuthorityCodes(GeographicCRS.class);
        Set<String> geodetic   = EPSGFactoryFallback.INSTANCE.getAuthorityCodes(GeodeticCRS.class);
        assertTrue(geodetic.containsAll(geographic));
        assertTrue(geodetic.removeAll(geographic));
        assertSetEquals(List.of("4978", "4984", "4936"), geodetic);
        /*
         * There is too many ProjectedCRS codes for enumerating all of them, so test only a sampling.
         */
        final Set<String> codes = EPSGFactoryFallback.INSTANCE.getAuthorityCodes(ProjectedCRS.class);
        assertTrue(codes.containsAll(List.of("5041", "5042", "32601", "32660", "32701", "32760")));
        assertTrue(Collections.disjoint(codes, List.of("7030", "6326", "4326", "4978", "32600", "32700", "5714")));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link EPSGFactoryFallback#createPrimeMeridian(String)}.
     *
     * @throws FactoryException if a prime meridian cannot be constructed.
     */
    @Test
    public void testCreatePrimeMeridian() throws FactoryException {
        verifyCreatePrimeMeridian(CommonCRS.WGS84.primeMeridian(), StandardDefinitions.GREENWICH);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link EPSGFactoryFallback#createEllipsoid(String)}.
     *
     * @throws FactoryException if an ellipsoid cannot be constructed.
     */
    @Test
    public void testCreateEllipsoid() throws FactoryException {
        verifyCreateEllipsoid(CommonCRS.WGS84 .ellipsoid(), "7030");
        verifyCreateEllipsoid(CommonCRS.WGS72 .ellipsoid(), "7043");
        verifyCreateEllipsoid(CommonCRS.NAD83 .ellipsoid(), "7019");
        verifyCreateEllipsoid(CommonCRS.NAD27 .ellipsoid(), "7008");
        verifyCreateEllipsoid(CommonCRS.ED50  .ellipsoid(), "7022");
        verifyCreateEllipsoid(CommonCRS.SPHERE.ellipsoid(), "7048");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link EPSGFactoryFallback#createEllipsoid(String)}.
     *
     * @throws FactoryException if an ellipsoid cannot be constructed.
     */
    @Test
    public void testCreateDatum() throws FactoryException {
        verifyCreateDatum(CommonCRS.WGS84 .datum(true), "6326");
        verifyCreateDatum(CommonCRS.WGS72 .datum(true), "6322");
        verifyCreateDatum(CommonCRS.NAD83 .datum(true), "6269");
        verifyCreateDatum(CommonCRS.NAD27 .datum(true), "6267");
        verifyCreateDatum(CommonCRS.ED50  .datum(true), "6230");
        verifyCreateDatum(CommonCRS.SPHERE.datum(true), "6047");
        loggings.skipNextLogIfContains("EPSG:6047");    // Deprecated EPSG entry for datum.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link EPSGFactoryFallback#createCoordinateSystem(String)}.
     *
     * @throws FactoryException if a CS cannot be constructed.
     */
    @Test
    public void testCreateCS() throws FactoryException {
        verifyCreateCS(CommonCRS.DEFAULT.geographic(),   "6422");
        verifyCreateCS(CommonCRS.DEFAULT.geographic3D(), "6423");
        verifyCreateCS(CommonCRS.DEFAULT.spherical(),    "6404");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link EPSGFactoryFallback#createCoordinateReferenceSystem(String)}.
     *
     * @throws FactoryException if a CRS cannot be constructed.
     *
     * @see CRSTest#testForEpsgCode()
     * @see CRSTest#testForCrsCode()
     */
    @Test
    public void testCreateCRS() throws FactoryException {
        verifyCreateCRS(CommonCRS.WGS84 .geographic(),            "4326");
        verifyCreateCRS(CommonCRS.WGS72 .geographic(),            "4322");
        verifyCreateCRS(CommonCRS.SPHERE.geographic(),            "4047");
        verifyCreateCRS(CommonCRS.NAD83 .geographic(),            "4269");
        verifyCreateCRS(CommonCRS.NAD27 .geographic(),            "4267");
        verifyCreateCRS(CommonCRS.ETRS89.geographic(),            "4258");
        verifyCreateCRS(CommonCRS.ED50  .geographic(),            "4230");
        verifyCreateCRS(CommonCRS.WGS84 .geocentric(),            "4978");
        verifyCreateCRS(CommonCRS.WGS72 .geocentric(),            "4984");
        verifyCreateCRS(CommonCRS.ETRS89.geocentric(),            "4936");
        verifyCreateCRS(CommonCRS.WGS84 .geographic(),       "EPSG:4326");
        verifyCreateCRS(CommonCRS.WGS72 .geographic(),      "EPSG::4322");
        verifyCreateCRS(CommonCRS.WGS84 .geographic3D(),          "4979");
        verifyCreateCRS(CommonCRS.WGS72 .geographic3D(),          "4985");
        verifyCreateCRS(CommonCRS.ETRS89.geographic3D(),          "4937");
        verifyCreateCRS(CommonCRS.WGS84 .universal(-88, 120),     "5042");
        verifyCreateCRS(CommonCRS.WGS84 .universal( 40, 14),     "32633");
        verifyCreateCRS(CommonCRS.Vertical.MEAN_SEA_LEVEL.crs(),  "5714");
        verifyCreateCRS(CommonCRS.Vertical.DEPTH.crs(),           "5715");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Asserts that the result of {@link EPSGFactoryFallback#createObject(String)} is the given prime meridian.
     */
    private static void verifyCreatePrimeMeridian(final PrimeMeridian expected, final String code) throws FactoryException {
        assertSame(expected, EPSGFactoryFallback.INSTANCE.createPrimeMeridian(code), code);
        assertSame(expected, EPSGFactoryFallback.INSTANCE.createObject(code), code);
    }

    /**
     * Asserts that the result of {@link EPSGFactoryFallback#createObject(String)} is the given ellipsoid.
     */
    private static void verifyCreateEllipsoid(final Ellipsoid expected, final String code) throws FactoryException {
        assertSame(expected, EPSGFactoryFallback.INSTANCE.createEllipsoid(code), code);
        assertSame(expected, EPSGFactoryFallback.INSTANCE.createObject(code), code);
    }

    /**
     * Asserts that the result of {@link EPSGFactoryFallback#createObject(String)} is the given datum.
     */
    private static void verifyCreateDatum(final Datum expected, final String code) throws FactoryException {
        assertSame(expected, EPSGFactoryFallback.INSTANCE.createDatum (code), code);
        assertSame(expected, EPSGFactoryFallback.INSTANCE.createObject(code), code);
    }

    /**
     * Asserts that the result of {@link EPSGFactoryFallback#createObject(String)} is the given CRS.
     */
    private static void verifyCreateCRS(final SingleCRS expected, final String code) throws FactoryException {
        assertSame(expected, EPSGFactoryFallback.INSTANCE.createCoordinateReferenceSystem(code), code);
        assertSame(expected, EPSGFactoryFallback.INSTANCE.createObject(code), code);
    }

    /**
     * Asserts that the result of {@link EPSGFactoryFallback#createObject(String)} is CS of the given CRS.
     * Contrarily to other kinds of objects, coordinate systems are currently not cached. Consequently, we
     * cannot assert that instances are the same.
     */
    private static void verifyCreateCS(final CoordinateReferenceSystem crs, final String code) throws FactoryException {
        final CoordinateSystem expected = crs.getCoordinateSystem();
        final CoordinateSystem actual = EPSGFactoryFallback.INSTANCE.createCoordinateSystem(code);
        assertEquals(actual, EPSGFactoryFallback.INSTANCE.createObject(code), code);
        assertEqualsIgnoreMetadata(expected, actual);
    }

    /**
     * Sets the EPSG factory to the given instance and clears the cache of all {@link CommonCRS} enumeration values.
     */
    @Configuration
    private static void setEPSGFactory(final GeodeticAuthorityFactory factory) {
        AuthorityFactories.setEPSG(factory);
        for (final CommonCRS          crs : CommonCRS         .values()) crs.clear();
        for (final CommonCRS.Vertical crs : CommonCRS.Vertical.values()) crs.clear();
        for (final CommonCRS.Temporal crs : CommonCRS.Temporal.values()) crs.clear();
    }

    /**
     * Compares all CRS created by {@link EPSGFactoryFallback} with CRS created by the real EPSG database.
     * This test must be run in a class annotated with {@link Isolated}.
     *
     * @throws FactoryException if a CRS cannot be constructed.
     */
    @Test
    public void compareAllCodes() throws FactoryException {
        final GeodeticAuthorityFactory EPSG = AuthorityFactories.getEPSG(false);
        try {
            setEPSGFactory(EPSGFactoryFallback.INSTANCE);
            final var codes = new ArrayList<String>(EPSGFactoryFallback.INSTANCE.getAuthorityCodes(CoordinateReferenceSystem.class));
            Collections.shuffle(codes, TestUtilities.createRandomNumberGenerator());
            for (final String code : codes) {
                final CoordinateReferenceSystem crs = EPSGFactoryFallback.INSTANCE.createCoordinateReferenceSystem(code);
                final CoordinateReferenceSystem expected = EPSG.createCoordinateReferenceSystem(code);
                assertTrue(Utilities.deepEquals(expected, crs, ComparisonMode.DEBUG), code);
            }
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        } finally {
            setEPSGFactory(EPSG);
        }
        boolean repeat = false;
        do {
            loggings.skipNextLogIfContains("EPSG:6047");        // Deprecated EPSG entry for datum.
            loggings.skipNextLogIfContains("EPSG:4047");        // Deprecated EPSG entry for CRS.
            loggings.skipNextLogIfContains("EPSG:6019");        // Deprecated EPSG entry for datum.
            loggings.skipNextLogIfContains("EPSG:4019");        // Deprecated EPSG entry for CRS.
        } while ((repeat = !repeat) == true);                   // Repeated because order may vary.
        loggings.assertNoUnexpectedLog();
    }
}
