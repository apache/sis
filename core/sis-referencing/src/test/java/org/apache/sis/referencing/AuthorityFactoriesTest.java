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
import org.opengis.util.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.factory.NoSuchAuthorityFactoryException;

// Test imports
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.TestCase;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link AuthorityFactories}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class AuthorityFactoriesTest extends TestCase {
    /**
     * A JUnit {@link Rule} for listening to log events. This field is public because JUnit requires us to
     * do so, but should be considered as an implementation details (it should have been a private field).
     */
    @Rule
    public final LoggingWatcher loggings = new LoggingWatcher(Loggers.CRS_FACTORY);

    /**
     * Verifies that no unexpected warning has been emitted in any test defined in this class.
     */
    @After
    public void assertNoUnexpectedLog() {
        loggings.assertNoUnexpectedLog();
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
    @DependsOnMethod("testCRS84")
    public void testCreateCRS() throws FactoryException {
        final CRSAuthorityFactory factory = AuthorityFactories.ALL;
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

        try {
            factory.createCoordinateReferenceSystem("84");
            fail("Should not work without authority.");
        } catch (NoSuchAuthorityCodeException exception) {
            // This is the expected exception.
            assertEquals("84", exception.getAuthorityCode());
        }

        try {
            factory.createCoordinateReferenceSystem("FOO:84");
            fail("Should not work with unknown authority.");
        } catch (NoSuchAuthorityFactoryException exception) {
            // This is the expected exception.
            assertEquals("FOO", exception.getAuthority());
        }
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

        try {
            factory.createCoordinateReferenceSystem("http://www.dummy.net/gml/srs/CRS#84");
            fail("Should not accept http://www.dummy.net");
        } catch (NoSuchAuthorityCodeException e) {
            assertNotNull(e.getMessage());
        }

        try {
            factory.createCoordinateReferenceSystem("http://www.opengis.net/gml/dummy/CRS#84");
            fail("Should not accept “dummy” as an authority");
        } catch (NoSuchAuthorityCodeException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests the {@code getAuthorityCodes(…)} method.
     *
     * @throws FactoryException if an error occurred while fetching the codes.
     */
    @Test
    public void testGetAuthorityCodes() throws FactoryException {
        final CRSAuthorityFactory factory = AuthorityFactories.ALL;
        final Collection<String> codes = factory.getAuthorityCodes(CoordinateReferenceSystem.class);
        assertFalse(codes.isEmpty());
        assertTrue(codes.contains("CRS:84"));
        assertTrue(codes.contains("AUTO:42001") || codes.contains("AUTO2:42001"));
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
        assertNotNull("With scan allowed, should find the CRS.", find);
        assertTrue(HardCodedCRS.WGS84.equals(find, ComparisonMode.DEBUG));
        assertSame(factory.createCoordinateReferenceSystem("CRS:84"), find);
    }
}
