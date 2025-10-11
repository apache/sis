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

import java.util.Collection;
import java.util.ServiceLoader;
import org.opengis.util.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.referencing.internal.EPSGFactoryProxy;
import org.apache.sis.referencing.factory.CommonAuthorityFactory;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCaseWithLogs;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertNotDeepEquals;


/**
 * Tests {@link AuthorityFactories}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class AuthorityFactoriesTest extends TestCaseWithLogs {
    /**
     * Creates a new test case.
     */
    public AuthorityFactoriesTest() {
        super(Loggers.CRS_FACTORY);
    }

    /**
     * Ensures that {@link EPSGFactoryProxy} is declared before {@link CommonAuthorityFactory}.
     * This is preferable (but not mandatory) because of the way we implemented {@link AuthorityFactories}.
     */
    @Test
    public void testFactoryOrder() {
        boolean foundProxy  = false;
        boolean foundCommon = false;
        for (CRSAuthorityFactory factory : ServiceLoader.load(CRSAuthorityFactory.class, AuthorityFactories.class.getClassLoader())) {
            if (factory instanceof CommonAuthorityFactory) {
                foundCommon = true;
                assertTrue(foundProxy, "Should not have found EPSGFactoryProxy after CommonAuthorityFactory.");
            }
            if (factory instanceof EPSGFactoryProxy) {
                foundProxy = true;
                assertFalse(foundCommon, "Should not have found EPSGFactoryProxy after CommonAuthorityFactory.");
            }
        }
        assertTrue(foundCommon, "Factory not found.");
        assertTrue(foundProxy,  "Factory not found.");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link CRSAuthorityFactory#getDescriptionText(Class, String)}.
     *
     * @throws FactoryException if the EPSG:4326 name cannot be obtained.
     */
    @Test
    public void testGetDescriptionText() throws FactoryException {
        assertDescriptionEquals("WGS 84", "EPSG:4326");
        assertDescriptionEquals("WGS 84", "urn:ogc:def:crs:epsg::4326");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Asserts that the description is equal to the expected value.
     *
     * @param expected  the expected description.
     * @param code      the code of the object for which to fetch the description.
     */
    private void assertDescriptionEquals(String expected, String code) throws FactoryException {
        assertEquals(expected, AuthorityFactories.ALL.getDescriptionText(CoordinateReferenceSystem.class, code).orElseThrow().toString());
    }

    /**
     * Tests creation of {@code CRS:84} from various codes.
     *
     * @throws FactoryException if a CRS:84 creation failed.
     */
    @Test
    public void testCRS84() throws FactoryException {
        final CRSAuthorityFactory factory = AuthorityFactories.ALL;
        final GeographicCRS crs = factory.createGeographicCRS("CRS:84");
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:CRS::84"));
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:CRS:1.3:84"));
        assertSame(crs, factory.createGeographicCRS("URN:OGC:DEF:CRS:CRS:1.3:84"));
        assertSame(crs, factory.createGeographicCRS("URN:OGC:DEF:CRS:CRS::84"));
        assertSame(crs, factory.createGeographicCRS("urn:x-ogc:def:crs:CRS:1.3:84"));
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:OGC:1.3:CRS84"));

        // Following are just wrappers for above factory.
        assertSame(crs, CRS.forCode("urn:ogc:def:crs:CRS:1.3:84"));
        assertSame(crs, CRS.forCode("urn:ogc:def:crs:OGC:1.3:CRS84"));
        assertSame(crs, CRS.forCode("CRS:84"));
        assertSame(crs, CRS.forCode("OGC:CRS84"));

        assertNotDeepEquals(crs, CRS.forCode("CRS:83"));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests creation of {@code EPSG:4326} from codes for various versions of the EPSG database.
     * This test verifies the logged messages.
     *
     * @throws FactoryException if an EPSG:4326 creation failed.
     */
    @Test
    public void testVersionedEPSG() throws FactoryException {
        final CRSAuthorityFactory factory = AuthorityFactories.ALL;
        final GeographicCRS crs = factory.createGeographicCRS("EPSG:4326");
        loggings.assertNoUnexpectedLog();

        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:EPSG:6.11.2:4326"));
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:EPSG:6.11.2:4326"));
        loggings.assertNextLogContains("6.11.2");
        loggings.assertNoUnexpectedLog();

        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:EPSG:7.04:4326"));
        loggings.assertNextLogContains("7.04");
        loggings.assertNoUnexpectedLog();

        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:EPSG:7.10:4326"));
        loggings.assertNextLogContains("7.10");
        loggings.assertNoUnexpectedLog();

        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:EPSG::4326"));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the {@code createCoordinateReferenceSystem(…)} method with various code.
     *
     * @throws FactoryException if a CRS creation failed.
     */
    @Test
    public void testCreateCRS() throws FactoryException {
        final GeodeticAuthorityFactory factory = AuthorityFactories.ALL;
        final CRSAuthorityFactory wms = AuthorityFactories.ALL.getAuthorityFactory(CRSAuthorityFactory.class, Constants.OGC, null);
        CoordinateReferenceSystem actual, expected;

        actual   = factory.createCoordinateReferenceSystem("CRS:84");
        expected = wms.createCoordinateReferenceSystem("84");
        assertSame(expected, actual);
        assertSame(expected, factory.createObject("CRS:84"));

        actual   = factory .createCoordinateReferenceSystem("AUTO:42001,0,0");
        expected = wms.createCoordinateReferenceSystem("42001,0,0");
        assertSame(expected, actual);
        assertSame(expected, factory.createObject("AUTO:42001,0,0"));

        actual   = factory.createCoordinateReferenceSystem("CRS:27");
        expected = wms.createCoordinateReferenceSystem("27");
        assertSame(expected, actual);
        assertSame(expected, factory.createObject("CRS:27"));

        NoSuchAuthorityCodeException exception;
        exception = assertThrows(NoSuchAuthorityCodeException.class,
                () -> factory.createCoordinateReferenceSystem("84"),
                "Should not work without authority.");
        assertEquals("84", exception.getAuthorityCode());

        exception = assertThrows(NoSuchAuthorityCodeException.class,
                () -> factory.createCoordinateReferenceSystem("FOO:84"),
                "Should not work with unknown authority.");
        assertEquals("FOO", exception.getAuthority());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests creation of CRS from codes in the {@code "http://www.opengis.net/gml/srs/"} name space.
     *
     * @throws FactoryException if a CRS creation failed.
     */
    @Test
    public void testHttp() throws FactoryException {
        final CRSAuthorityFactory factory = AuthorityFactories.ALL;
        final CRSAuthorityFactory wms = AuthorityFactories.ALL.getAuthorityFactory(CRSAuthorityFactory.class, Constants.OGC, null);
        CoordinateReferenceSystem actual, expected;

        actual   = factory.createCoordinateReferenceSystem("http://www.opengis.net/gml/srs/CRS#84");
        expected = wms.createCoordinateReferenceSystem("84");
        assertSame(expected, actual);

        actual = factory.createCoordinateReferenceSystem("HTTP://WWW.OPENGIS.NET/GML/SRS/crs#84");
        assertSame(expected, actual);

        actual = factory.createCoordinateReferenceSystem("http://www.opengis.net/gml/srs/CRS.xml#84");
        assertSame(expected, actual);

        NoSuchAuthorityCodeException exception;
        exception = assertThrows(NoSuchAuthorityCodeException.class,
                () -> factory.createCoordinateReferenceSystem("http://www.dummy.net/gml/srs/CRS#84"),
                "Should not accept http://www.dummy.net");
        assertMessageContains(exception);

        exception = assertThrows(NoSuchAuthorityCodeException.class,
                () -> factory.createCoordinateReferenceSystem("http://www.opengis.net/gml/dummy/CRS#84"),
                "Should not accept “dummy” as an authority");
        assertMessageContains(exception);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the {@code getAuthorityCodes(…)} method.
     *
     * @throws FactoryException if an error occurred while fetching the codes.
     */
    @Test
    public void testGetAuthorityCodes() throws FactoryException {
        final CRSAuthorityFactory factory = AuthorityFactories.ALL;
        try {
            final Collection<String> codes = factory.getAuthorityCodes(CoordinateReferenceSystem.class);
            assertFalse(codes.isEmpty());
            assertTrue(codes.contains("CRS:84"));
            assertTrue(codes.contains("AUTO:42001") || codes.contains("AUTO2:42001"));
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the {@code IdentifiedObjectFinder.find(…)} method.
     *
     * @throws FactoryException if the operation failed creation failed.
     */
    @Test
    public void testFind() throws FactoryException {
        final CRSAuthorityFactory factory = AuthorityFactories.ALL;
        final IdentifiedObjectFinder finder = AuthorityFactories.ALL.newIdentifiedObjectFinder();
        final IdentifiedObject find = finder.findSingleton(HardCodedCRS.WGS84);
        assertNotNull(find, "With scan allowed, should find the CRS.");
        assertTrue(HardCodedCRS.WGS84.equals(find, ComparisonMode.DEBUG));
        assertSame(factory.createCoordinateReferenceSystem("CRS:84"), find);
        loggings.assertNoUnexpectedLog();
    }
}
