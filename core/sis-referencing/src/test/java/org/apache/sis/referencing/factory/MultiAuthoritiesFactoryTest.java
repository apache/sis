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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.measure.unit.SI;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link MultiAuthoritiesFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(AuthorityFactoryProxyTest.class)
public final strictfp class MultiAuthoritiesFactoryTest extends TestCase {
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
     * Tests consistency of the mock factory used by other tests in this class.
     *
     * @throws FactoryException if no object was found for a code.
     */
    @Test
    public void testAuthorityFactoryMock() throws FactoryException {
        final AuthorityFactoryMock factory = new AuthorityFactoryMock("MOCK", null);
        final Class<?>[] types = {
            GeocentricCRS.class,
            GeographicCRS.class,
            GeodeticDatum.class,
            VerticalDatum.class,
            VerticalCRS.class,
            GeodeticCRS.class,
            PrimeMeridian.class,
            Datum.class,
            CoordinateReferenceSystem.class,
            IdentifiedObject.class
        };
        for (final Class<?> type : types) {
            for (final String code : factory.getAuthorityCodes(type.asSubclass(IdentifiedObject.class))) {
                assertInstanceOf(code, type, factory.createObject(code));
            }
        }
    }

    /**
     * Tests {@link MultiAuthoritiesFactory#getCodeSpaces()}.
     */
    @Test
    public void testGetCodeSpaces() {
        final AuthorityFactoryMock mock1 = new AuthorityFactoryMock("MOCK1", "2.3");
        final AuthorityFactoryMock mock2 = new AuthorityFactoryMock("MOCK2", null);
        final AuthorityFactoryMock mock3 = new AuthorityFactoryMock("MOCK3", null);
        final MultiAuthoritiesFactory factory = new MultiAuthoritiesFactory(
                Arrays.asList(mock1, mock2), null,
                Arrays.asList(mock1, mock3), null);
        assertSetEquals(Arrays.asList("MOCK1", "MOCK2", "MOCK3"), factory.getCodeSpaces());
    }

    /**
     * Tests {@link MultiAuthoritiesFactory#getAuthorityFactory(Class, String, String)}.
     *
     * @throws NoSuchAuthorityFactoryException if an authority is not recognized.
     */
    @Test
    public void testGetAuthorityFactory() throws NoSuchAuthorityFactoryException {
        final AuthorityFactoryMock mock1 = new AuthorityFactoryMock("MOCK1", null);
        final AuthorityFactoryMock mock2 = new AuthorityFactoryMock("MOCK2", "1.2");
        final AuthorityFactoryMock mock3 = new AuthorityFactoryMock("MOCK1", "2.3");
        final MultiAuthoritiesFactory factory = new MultiAuthoritiesFactory(
                Arrays.asList(mock1, mock2, mock3), null,
                Arrays.asList(mock1, mock3), null);

        assertSame("MOCK2", mock2, factory.getAuthorityFactory(  CRSAuthorityFactory.class, "mock2", null));
        assertSame("MOCK1", mock1, factory.getAuthorityFactory(  CRSAuthorityFactory.class, "mock1", null));
        assertSame("MOCK2", mock2, factory.getAuthorityFactory(  CRSAuthorityFactory.class, "mock2", "1.2"));
        assertSame("MOCK3", mock3, factory.getAuthorityFactory(  CRSAuthorityFactory.class, "mock1", "2.3"));
        assertSame("MOCK3", mock3, factory.getAuthorityFactory(DatumAuthorityFactory.class, "mock1", "2.3"));
        assertSame("MOCK1", mock1, factory.getAuthorityFactory(DatumAuthorityFactory.class, "mock1", null));
        try {
            factory.getAuthorityFactory(DatumAuthorityFactory.class, "mock2", null);
            fail("Should not have found a 'mock2' factory for datum objects.");
        } catch (NoSuchAuthorityFactoryException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("MOCK2"));
        }
        try {
            factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock1", "9.9");
            fail("Should not have found a 'mock1' factory for the 9.9 version.");
        } catch (NoSuchAuthorityFactoryException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("MOCK1"));
            assertTrue(message, message.contains("9.9"));
        }
    }

    /**
     * Tests {@link MultiAuthoritiesFactory#getAuthorityFactory(Class, String, String)}
     * with a conflict in the factory namespace.
     *
     * @throws NoSuchAuthorityFactoryException if an authority is not recognized.
     */
    @Test
    @DependsOnMethod("testGetAuthorityFactory")
    public void testConflict() throws NoSuchAuthorityFactoryException {
        final AuthorityFactoryMock mock1 = new AuthorityFactoryMock("MOCK1", "2.3");
        final AuthorityFactoryMock mock2 = new AuthorityFactoryMock("MOCK1", "2.3");
        final AuthorityFactoryMock mock3 = new AuthorityFactoryMock("MOCK3", "1.2");
        final AuthorityFactoryMock mock4 = new AuthorityFactoryMock("MOCK3", null);
        final AuthorityFactoryMock mock5 = new AuthorityFactoryMock("MOCK5", null);
        final MultiAuthoritiesFactory factory = new MultiAuthoritiesFactory(
                Arrays.asList(mock1, mock2, mock3, mock4, mock5), null, null, null);

        assertSame("MOCK1", mock1, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock1", null));
        assertSame("MOCK1", mock1, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock1", "2.3"));
        loggings.assertNoUnexpectedLog();

        assertSame("MOCK3", mock3, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock3", null));
        loggings.assertNextLogContains("CRSAuthorityFactory", "AuthorityFactoryMock", "MOCK1", "2.3");
        loggings.assertNoUnexpectedLog();

        assertSame("MOCK5", mock5, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock5", null));
        loggings.assertNextLogContains("CRSAuthorityFactory", "AuthorityFactoryMock", "MOCK3");
        loggings.assertNoUnexpectedLog();

        // Ask again the same factories. No logging should be emitted now, because we already logged.
        assertSame("MOCK3", mock3, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock3", null));
        assertSame("MOCK5", mock5, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock5", null));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@code MultiAuthoritiesFactory.createFoo(String)} from codes in the {@code "AUTHORITY:CODE"} form.
     * Tests also {@code "AUTHORITY:VERSION:CODE"}.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    @DependsOnMethod("testGetAuthorityFactory")
    public void testCreateFromSimpleCodes() throws FactoryException {
        final Set<AuthorityFactoryMock> mock = Collections.singleton(new AuthorityFactoryMock("MOCK", "2.3"));
        final MultiAuthoritiesFactory factory = new MultiAuthoritiesFactory(mock, mock, mock, null);

        assertSame("Straight",      HardCodedCRS  .WGS84_φλ,  factory.createGeographicCRS("MOCK:4326"));
        assertSame("With spaces",   HardCodedCRS  .WGS84,     factory.createGeographicCRS("  mock :  84 "));
        assertSame("With version",  HardCodedDatum.WGS84,     factory.createGeodeticDatum("mock:2.3:6326"));
        assertSame("Empty version", HardCodedDatum.GREENWICH, factory.createPrimeMeridian(" MoCk :: 8901"));
        assertSame("With spaces",   HardCodedCRS  .DEPTH,     factory.createVerticalCRS  (" MoCk : : 9905"));
        assertSame("Version 0",     HardCodedDatum.SPHERE,    factory.createGeodeticDatum("MOCK: 0:6047"));
        assertSame("With spaces",   Extents       .WORLD,     factory.createExtent       ("MOCK: 2.3 : 1262"));
        assertSame("With spaces",   SI            .METRE,     factory.createUnit         (" MoCK : : 9001 "));
        assertEquals("Greenwich",   factory.getDescriptionText("MOCK:8901").toString());
        try {
            factory.createGeodeticDatum("MOCK2:4326");
            fail("Should not have found an object from a non-existent factory.");
        } catch (NoSuchAuthorityFactoryException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("MOCK2"));
        }
    }

    /**
     * Tests {@code MultiAuthoritiesFactory.createFoo(String)} from codes in the
     * {@code "urn:ogc:def:type:authority:version:code"} form.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    @DependsOnMethod("testCreateFromSimpleCodes")
    public void testCreateFromURNs() throws FactoryException {
        final Set<AuthorityFactoryMock> mock = Collections.singleton(new AuthorityFactoryMock("MOCK", "2.3"));
        final MultiAuthoritiesFactory factory = new MultiAuthoritiesFactory(mock, mock, mock, null);

        assertSame("Empty version", HardCodedCRS  .WGS84_φλ,  factory.createGeographicCRS("urn:ogc:def:crs:MOCK::4326"));
        assertSame("With spaces",   HardCodedCRS  .WGS84,     factory.createGeographicCRS(" urn : ogc  : def:crs :  mock : :  84 "));
        assertSame("Mixed case",    HardCodedCRS  .DEPTH,     factory.createVerticalCRS  (" Urn : OGC : dEf : CRS : MoCk : : 9905"));
        assertSame("With version",  HardCodedDatum.WGS84,     factory.createDatum        ("urn:ogc:def:datum:mock:2.3:6326"));
        assertSame("Empty version", HardCodedDatum.GREENWICH, factory.createObject       ("urn:ogc:def:meridian: MoCk :: 8901"));
        assertSame("Version 0",     HardCodedDatum.SPHERE,    factory.createGeodeticDatum("urn:ogc:def:datum:MOCK: 0 :6047"));
        assertSame("Upper case",    SI            .METRE,     factory.createUnit         ("URN:OGC:DEF:UOM:MOCK::9001"));
        try {
            factory.createGeographicCRS("urn:ogc:def:datum:MOCK::4326");
            fail("Should create an object of the wrong type.");
        } catch (NoSuchAuthorityCodeException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("datum"));
            assertTrue(message, message.contains("GeographicCRS"));
        }
    }

    /**
     * Tests {@code MultiAuthoritiesFactory.createFoo(String)} from codes in the
     * {@code "http://www.opengis.net/gml/srs/authority.xml#code"} form.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    @DependsOnMethod("testCreateFromURNs")
    public void testCreateFromHTTPs() throws FactoryException {
        final Set<AuthorityFactoryMock> mock = Collections.singleton(new AuthorityFactoryMock("MOCK", "2.3"));
        final MultiAuthoritiesFactory factory = new MultiAuthoritiesFactory(mock, mock, mock, null);

        assertSame("HTTP",          HardCodedCRS  .WGS84_φλ,  factory.createGeographicCRS("http://www.opengis.net/def/crs/mock/0/4326"));
        assertSame("GML",           HardCodedCRS  .WGS84_φλ,  factory.createObject       ("http://www.opengis.net/gml/srs/mock.xml#4326"));
        assertSame("With spaces",   HardCodedCRS  .WGS84,     factory.createGeographicCRS("http://www.opengis.net/gml/srs/ mock.xml # 84 "));
        assertSame("Mixed case",    HardCodedCRS  .DEPTH,     factory.createVerticalCRS  ("HTTP://www.OpenGIS.net/GML/SRS/MoCk.xml#9905"));
        try {
            factory.createDatum("http://www.opengis.net/gml/srs/mock.xml#6326");
            fail("Should create an object of the wrong type.");
        } catch (NoSuchAuthorityCodeException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("crs"));
            assertTrue(message, message.contains("Datum"));
        }
    }

    /**
     * Tests {@link MultiAuthoritiesFactory#getAuthorityCodes(Class)}.
     *
     * @throws FactoryException if an error occurred while fetching the set of codes.
     */
    @Test
    public void testGetAuthorityCodes() throws FactoryException {
        final List<AuthorityFactoryMock> mock = Arrays.asList(
                new AuthorityFactoryMock("MOCK", null),
                new AuthorityFactoryMock("MOCK", "2.3"));
        final MultiAuthoritiesFactory factory = new MultiAuthoritiesFactory(mock, mock, mock, null);
        final Set<String> codes = factory.getAuthorityCodes(CoordinateReferenceSystem.class);

        assertTrue("MOCK:4979",   codes.contains("MOCK:4979"));     // A geocentric CRS.
        assertTrue(" mock :: 84", codes.contains(" mock :: 84"));   // A geographic CRS.
        assertTrue("http://www.opengis.net/gml/srs/mock.xml#4326",
                codes.contains("http://www.opengis.net/gml/srs/mock.xml#4326"));

        assertFalse("MOCK:6326", codes.contains("MOCK:6326"));      // A geodetic datum.
        assertFalse("isEmpty()", codes.isEmpty());
        assertArrayEquals(new String[] {"MOCK:4979", "MOCK:84", "MOCK:4326", "MOCK:5714", "MOCK:9905"}, codes.toArray());
    }

    /**
     * Tests {@link MultiAuthoritiesFactory#createFromCoordinateReferenceSystemCodes(String, String)}.
     *
     * @throws FactoryException if an error occurred while creating the operation.
     */
    @Test
    public void testCreateFromCoordinateReferenceSystemCodes() throws FactoryException {
        final List<AuthorityFactoryMock> mock = Arrays.asList(
                new AuthorityFactoryMock("MOCK", null),
                new AuthorityFactoryMock("MOCK", "2.3"));
        final MultiAuthoritiesFactory factory = new MultiAuthoritiesFactory(mock, null, null, mock);
        /*
         * Our mock factory does not implement createFromCoordinateReferenceSystemCodes(String, String),
         * so we just test that we didn't got an exception and no message were logged.
         */
        assertTrue(factory.createFromCoordinateReferenceSystemCodes("MOCK:4326", "MOCK:84").isEmpty());
        /*
         * Following should log a warning telling that the authority factories do not match.
         */
        assertTrue(factory.createFromCoordinateReferenceSystemCodes("MOCK:4326", "MOCK:2.3:84").isEmpty());
        loggings.assertNextLogContains("MOCK:4326", "MOCK:2.3:84");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link MultiAuthoritiesFactory#newIdentifiedObjectFinder()}.
     *
     * @throws FactoryException if an error occurred while creating the finder.
     */
    @Test
    public void testNewIdentifiedObjectFinder() throws FactoryException {
        final List<AuthorityFactoryMock> mock = Arrays.asList(
                new AuthorityFactoryMock("MOCK", null),
                new AuthorityFactoryMock("MOCK", "2.3"));
        final MultiAuthoritiesFactory factory = new MultiAuthoritiesFactory(mock, null, mock, null);
        final IdentifiedObjectFinder finder = factory.newIdentifiedObjectFinder();
        assertSame(HardCodedDatum.WGS72, finder.findSingleton(HardCodedDatum.WGS72));
    }
}
