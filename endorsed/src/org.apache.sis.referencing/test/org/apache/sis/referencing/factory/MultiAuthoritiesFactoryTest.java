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
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.util.FactoryException;
import org.apache.sis.system.Loggers;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.measure.Units;
import org.apache.sis.util.collection.BackingStoreException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCaseWithLogs;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.datum.HardCodedDatum;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSetEquals;


/**
 * Tests {@link MultiAuthoritiesFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MultiAuthoritiesFactoryTest extends TestCaseWithLogs {
    /**
     * Creates a new test case.
     */
    public MultiAuthoritiesFactoryTest() {
        super(Loggers.CRS_FACTORY);
    }

    /**
     * Tests consistency of the mock factory used by other tests in this class.
     *
     * @throws FactoryException if no object was found for a code.
     */
    @Test
    public void testAuthorityFactoryMock() throws FactoryException {
        final var factory = new AuthorityFactoryMock("MOCK", null);
        final Class<?>[] types = {
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
        for (final Class<?> type : types) try {
            for (final String code : factory.getAuthorityCodes(type.asSubclass(IdentifiedObject.class))) {
                assertInstanceOf(type, factory.createObject(code), code);
            }
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
    }

    /**
     * Tests {@link MultiAuthoritiesFactory#getCodeSpaces()}.
     *
     * @throws FactoryException if a problem occurred with the factory to test.
     */
    @Test
    public void testGetCodeSpaces() throws FactoryException {
        final var mock1 = new AuthorityFactoryMock("MOCK1", "2.3");
        final var mock2 = new AuthorityFactoryMock("MOCK2", null);
        final var mock3 = new AuthorityFactoryMock("MOCK3", null);
        final var factory = new MultiAuthoritiesFactory(
                List.of(mock1, mock2), null,
                List.of(mock1, mock3), null);
        assertSetEquals(List.of("MOCK1", "MOCK2", "MOCK3"), factory.getCodeSpaces());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link MultiAuthoritiesFactory#getAuthorityFactory(Class, String, String)}.
     *
     * @throws NoSuchAuthorityFactoryException if an authority is not recognized.
     * @throws FactoryException if a problem occurred with the factory to test.
     */
    @Test
    public void testGetAuthorityFactory() throws FactoryException {
        final var mock1 = new AuthorityFactoryMock("MOCK1", null);
        final var mock2 = new AuthorityFactoryMock("MOCK2", "1.2");
        final var mock3 = new AuthorityFactoryMock("MOCK1", "2.3");
        final var factory = new MultiAuthoritiesFactory(
                List.of(mock1, mock2, mock3), null,
                List.of(mock1, mock3), null);

        assertSame(mock2, factory.getAuthorityFactory(  CRSAuthorityFactory.class, "mock2", null));
        assertSame(mock1, factory.getAuthorityFactory(  CRSAuthorityFactory.class, "mock1", null));
        assertSame(mock2, factory.getAuthorityFactory(  CRSAuthorityFactory.class, "mock2", "1.2"));
        assertSame(mock3, factory.getAuthorityFactory(  CRSAuthorityFactory.class, "mock1", "2.3"));
        assertSame(mock3, factory.getAuthorityFactory(DatumAuthorityFactory.class, "mock1", "2.3"));
        assertSame(mock1, factory.getAuthorityFactory(DatumAuthorityFactory.class, "mock1", null));

        NoSuchAuthorityFactoryException e;
        e = assertThrows(NoSuchAuthorityFactoryException.class,
                () -> factory.getAuthorityFactory(DatumAuthorityFactory.class, "mock2", null),
                "Should not have found a 'mock2' factory for datum objects.");
        assertMessageContains(e, "MOCK2");

        e = assertThrows(NoSuchAuthorityFactoryException.class,
                () -> factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock1", "9.9"),
                "Should not have found a 'mock1' factory for the 9.9 version.");
        assertMessageContains(e, "MOCK1", "9.9");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link MultiAuthoritiesFactory#getAuthorityFactory(Class, String, String)}
     * with a conflict in the factory namespace.
     *
     * @throws NoSuchAuthorityFactoryException if an authority is not recognized.
     * @throws FactoryException if a problem occurred with the factory to test.
     */
    @Test
    public void testConflict() throws FactoryException {
        final var mock1 = new AuthorityFactoryMock("MOCK1", "2.3");
        final var mock2 = new AuthorityFactoryMock("MOCK1", "2.3");
        final var mock3 = new AuthorityFactoryMock("MOCK3", "1.2");
        final var mock4 = new AuthorityFactoryMock("MOCK3", null);
        final var mock5 = new AuthorityFactoryMock("MOCK5", null);
        final var factory = new MultiAuthoritiesFactory(
                List.of(mock1, mock2, mock3, mock4, mock5), null, null, null);

        assertSame(mock1, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock1", null));
        assertSame(mock1, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock1", "2.3"));
        loggings.assertNoUnexpectedLog();

        assertSame(mock3, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock3", null));
        loggings.assertNextLogContains("CRSAuthorityFactory", "AuthorityFactoryMock", "MOCK1", "2.3");
        loggings.assertNoUnexpectedLog();

        assertSame(mock5, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock5", null));
        loggings.assertNextLogContains("CRSAuthorityFactory", "AuthorityFactoryMock", "MOCK3");
        loggings.assertNoUnexpectedLog();

        // Ask again the same factories. No logging should be emitted now, because we already logged.
        assertSame(mock3, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock3", null));
        assertSame(mock5, factory.getAuthorityFactory(CRSAuthorityFactory.class, "mock5", null));
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@code MultiAuthoritiesFactory.createFoo(String)} from codes in the {@code "AUTHORITY:CODE"} form.
     * Tests also {@code "AUTHORITY:VERSION:CODE"}.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    public void testCreateFromSimpleCodes() throws FactoryException {
        final Set<AuthorityFactoryMock> mock = Set.of(new AuthorityFactoryMock("MOCK", "2.3"));
        final var factory = new MultiAuthoritiesFactory(mock, mock, mock, null);

        assertSame(HardCodedCRS  .WGS84_LATITUDE_FIRST, factory.createGeographicCRS("MOCK:4326"));
        assertSame(HardCodedCRS  .WGS84,                factory.createGeographicCRS("  mock :  84 "));
        assertSame(HardCodedDatum.WGS84,                factory.createGeodeticDatum("mock:2.3:6326"));
        assertSame(HardCodedDatum.GREENWICH,            factory.createPrimeMeridian(" MoCk :: 8901"));
        assertSame(HardCodedCRS  .DEPTH,                factory.createVerticalCRS  (" MoCk : : 9905"));
        assertSame(HardCodedDatum.SPHERE,               factory.createGeodeticDatum("MOCK: 0:6047"));
        assertSame(Extents       .WORLD,                factory.createExtent       ("MOCK: 2.3 : 1262"));
        assertSame(Units         .METRE,                factory.createUnit         (" MoCK : : 9001 "));
        assertEquals("Greenwich", factory.getDescriptionText(PrimeMeridian.class, "MOCK:8901").get().toString());

        var e = assertThrows(NoSuchAuthorityFactoryException.class,
                () -> factory.createGeodeticDatum("MOCK2:4326"),
                "Should not have found an object from a non-existent factory.");
        assertMessageContains(e, "MOCK2");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@code MultiAuthoritiesFactory.createFoo(String)} from codes in the
     * {@code "urn:ogc:def:type:authority:version:code"} form.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    public void testCreateFromURNs() throws FactoryException {
        final Set<AuthorityFactoryMock> mock = Set.of(new AuthorityFactoryMock("MOCK", "2.3"));
        final var factory = new MultiAuthoritiesFactory(mock, mock, mock, null);

        assertSame(HardCodedCRS  .WGS84_LATITUDE_FIRST, factory.createGeographicCRS("urn:ogc:def:crs:MOCK::4326"));
        assertSame(HardCodedCRS  .WGS84,                factory.createGeographicCRS(" urn : ogc  : def:crs :  mock : :  84 "));
        assertSame(HardCodedCRS  .DEPTH,                factory.createVerticalCRS  (" Urn : OGC : dEf : CRS : MoCk : : 9905"));
        assertSame(HardCodedDatum.WGS84,                factory.createDatum        ("urn:ogc:def:datum:mock:2.3:6326"));
        assertSame(HardCodedDatum.GREENWICH,            factory.createObject       ("urn:ogc:def:meridian: MoCk :: 8901"));
        assertSame(HardCodedDatum.SPHERE,               factory.createGeodeticDatum("urn:ogc:def:datum:MOCK: 0 :6047"));
        assertSame(Units         .METRE,                factory.createUnit         ("URN:OGC:DEF:UOM:MOCK::9001"));

        var e = assertThrows(NoSuchAuthorityCodeException.class,
                () -> factory.createGeographicCRS("urn:ogc:def:datum:MOCK::4326"),
                "Should not create an object of the wrong type.");
        assertMessageContains(e, "datum", "GeographicCRS");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@code MultiAuthoritiesFactory.createFoo(String)} from codes in the
     * {@code "http://www.opengis.net/gml/srs/authority.xml#code"} form.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    public void testCreateFromHTTPs() throws FactoryException {
        final Set<AuthorityFactoryMock> mock = Set.of(new AuthorityFactoryMock("MOCK", "2.3"));
        final var factory = new MultiAuthoritiesFactory(mock, mock, mock, null);

        assertSame(HardCodedCRS.WGS84_LATITUDE_FIRST, factory.createGeographicCRS("http://www.opengis.net/def/crs/mock/0/4326"));
        assertSame(HardCodedCRS.WGS84_LATITUDE_FIRST, factory.createObject       ("http://www.opengis.net/gml/srs/mock.xml#4326"));
        assertSame(HardCodedCRS.WGS84,                factory.createGeographicCRS("http://www.opengis.net/gml/srs/ mock.xml # 84 "));
        assertSame(HardCodedCRS.DEPTH,                factory.createVerticalCRS  ("HTTP://www.OpenGIS.net/GML/SRS/MoCk.xml#9905"));

        var e = assertThrows(NoSuchAuthorityCodeException.class,
                () -> factory.createDatum("http://www.opengis.net/gml/srs/mock.xml#6326"),
                "Should not create an object of the wrong type.");
        assertMessageContains(e, "crs", "Datum");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@code MultiAuthoritiesFactory.createFoo(String)} from codes in the
     * {@code "urn:ogc:def:type, type₁:authority₁:version₁:code₁, type₂:authority₂:version₂:code₂"} form.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    public void testCreateFromCombinedURNs() throws FactoryException {
        final Set<AuthorityFactoryMock> mock = Set.of(new AuthorityFactoryMock("MOCK", "2.3"));
        final var factory = new MultiAuthoritiesFactory(mock, mock, mock, null);
        testCreateFromCombinedURIs(factory, "urn:ogc:def:crs, crs:MOCK::4326, crs:MOCK::5714");
        /*
         * Following are more unusual combinations described in OGC 07-092r1 (2007)
         * "Definition identifier URNs in OGC namespace".
         */
        SingleCRS crs = factory.createGeographicCRS("urn:ogc:def:crs, datum:MOCK::6326, cs:MOCK::6424");
        assertSame(HardCodedDatum.WGS84, crs.getDatum());
        assertSame(HardCodedCS.GEODETIC_2D, crs.getCoordinateSystem());
        /*
         * Verify that invalid combined URIs are rejected.
         */
        FactoryException e;
        e = assertThrows(FactoryException.class,
                () -> factory.createObject("urn:ogc:def:cs, crs:MOCK::4326, crs:MOCK::5714"),
                "Shall not accept to create CoordinateSystem from combined URI.");
        assertMessageContains(e, "CoordinateSystem");

        e = assertThrows(FactoryException.class,
                () -> factory.createObject("urn:ogc:def:crs, datum:MOCK::6326, cs:MOCK::6424, cs:MOCK::6422"),
                "Shall not accept to create combined URI with unexpected objects.");
        assertMessageContains(e);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@code MultiAuthoritiesFactory.createFoo(String)} from codes in the
     * {@code "http://www.opengis.net/def/crs-compound?1=(…)/code₁&2=(…)/code₂"} form.
     *
     * @throws FactoryException if an authority or a code is not recognized.
     */
    @Test
    public void testCreateFromCombinedHTTPs() throws FactoryException {
        final Set<AuthorityFactoryMock> mock = Set.of(new AuthorityFactoryMock("MOCK", "2.3"));
        final var factory = new MultiAuthoritiesFactory(mock, mock, mock, null);
        testCreateFromCombinedURIs(factory, "http://www.opengis.net/def/crs-compound?"
                                        + "1=http://www.opengis.net/def/crs/MOCK/0/4326&"
                                        + "2=http://www.opengis.net/def/crs/MOCK/0/5714");
        testCreateFromCombinedURIs(factory, "http://www.opengis.net/def/crs-compound?"
                                        + "2=http://www.opengis.net/def/crs/MOCK/0/5714&"
                                        + "1=http://www.opengis.net/def/crs/MOCK/0/4326");
        /*
         * Contrarily to URN, the HTTP form shall not accept Datum + CoordinateSystem combination.
         */
        var e = assertThrows(FactoryException.class,
                () -> factory.createObject("http://www.opengis.net/def/crs-compound?"
                                       + "1=http://www.opengis.net/def/datum/MOCK/0/6326&"
                                       + "2=http://www.opengis.net/def/cs/MOCK/0/6424"),
                "Shall not accept Datum + CoordinateSystem combination.");
        assertMessageContains(e);
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Implementation of {@link #testCreateFromCombinedURNs()} and {@link #testCreateFromCombinedHTTPs()}.
     */
    private static void testCreateFromCombinedURIs(final MultiAuthoritiesFactory factory, final String heightOnWGS84)
            throws FactoryException
    {
        CompoundCRS crs = factory.createCompoundCRS(heightOnWGS84);
        assertArrayEquals(new SingleCRS[] {HardCodedCRS.WGS84_LATITUDE_FIRST, HardCodedCRS.GRAVITY_RELATED_HEIGHT},
                          crs.getComponents().toArray(), "WGS 84 + MSL height");
    }

    /**
     * Tests {@link MultiAuthoritiesFactory#getAuthorityCodes(Class)}.
     *
     * @throws FactoryException if an error occurred while fetching the set of codes.
     */
    @Test
    public void testGetAuthorityCodes() throws FactoryException {
        final List<AuthorityFactoryMock> mock = List.of(
                new AuthorityFactoryMock("MOCK", null),
                new AuthorityFactoryMock("MOCK", "2.3"));
        final var factory = new MultiAuthoritiesFactory(mock, mock, mock, null);
        try {
            final Set<String> codes = factory.getAuthorityCodes(CoordinateReferenceSystem.class);

            assertTrue(codes.contains("MOCK:4979"));     // A geocentric CRS.
            assertTrue(codes.contains(" mock :: 84"));   // A geographic CRS.
            assertTrue(codes.contains("http://www.opengis.net/gml/srs/mock.xml#4326"));

            assertFalse(codes.contains("MOCK:6326"));    // A geodetic reference frame.
            assertFalse(codes.isEmpty());
            assertArrayEquals(new String[] {"MOCK:4979", "MOCK:84", "MOCK:4326", "MOCK:5714", "MOCK:9905"}, codes.toArray());
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link MultiAuthoritiesFactory#createFromCoordinateReferenceSystemCodes(String, String)}.
     *
     * @throws FactoryException if an error occurred while creating the operation.
     */
    @Test
    public void testCreateFromCoordinateReferenceSystemCodes() throws FactoryException {
        final List<AuthorityFactoryMock> mock = List.of(
                new AuthorityFactoryMock("MOCK", null),
                new AuthorityFactoryMock("MOCK", "2.3"));
        final var factory = new MultiAuthoritiesFactory(mock, null, null, mock);
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
        final List<AuthorityFactoryMock> mock = List.of(
                new AuthorityFactoryMock("MOCK", null),
                new AuthorityFactoryMock("MOCK", "2.3"));
        final var factory = new MultiAuthoritiesFactory(mock, null, mock, null);
        final IdentifiedObjectFinder finder = factory.newIdentifiedObjectFinder();
        assertSame(HardCodedDatum.WGS72, finder.findSingleton(HardCodedDatum.WGS72));
        loggings.assertNoUnexpectedLog();
    }
}
