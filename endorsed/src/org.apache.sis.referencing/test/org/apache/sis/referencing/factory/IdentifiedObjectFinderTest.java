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

import java.util.Map;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;


/**
 * Tests {@link IdentifiedObjectFinder}.
 * This test uses {@link CommonAuthorityFactory} as a simple factory implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class IdentifiedObjectFinderTest extends TestCase {
    /**
     * The factory to use for the test.
     */
    private GeodeticAuthorityFactory factory;

    /**
     * Initializes the factory to test.
     */
    public IdentifiedObjectFinderTest() {
        factory = new CommonAuthorityFactory();
    }

    /**
     * Tests the {@link IdentifiedObjectFinder#findSingleton(IdentifiedObject)} method.
     *
     * @throws FactoryException if the creation of a CRS failed.
     */
    @Test
    public void testFindSingleton() throws FactoryException {
        final GeographicCRS CRS84 = factory.createGeographicCRS("CRS:84");
        final IdentifiedObjectFinder finder = factory.newIdentifiedObjectFinder();
        assertEquals(IdentifiedObjectFinder.Domain.VALID_DATASET, finder.getSearchDomain(),
                     "Newly created finder should default to full scan.");

        finder.setSearchDomain(IdentifiedObjectFinder.Domain.DECLARATION);
        assertSame(CRS84, finder.findSingleton(CRS84),
                   "Should find without the need for scan, since we can use the CRS:84 identifier.");

        finder.setSearchDomain(IdentifiedObjectFinder.Domain.VALID_DATASET);
        assertSame(CRS84, finder.findSingleton(CRS84),
                   "Allowing scanning should not make any difference for this CRS84 instance.");
        /*
         * Same test as above, using a CRS without identifier.
         * The intent is to force a full scan.
         */
        final var search = new DefaultGeographicCRS(
                Map.of(DefaultGeographicCRS.NAME_KEY, CRS84.getName()),
                CRS84.getDatum(), CRS84.getDatumEnsemble(), CRS84.getCoordinateSystem());
        assertEqualsIgnoreMetadata(CRS84, search);              // Required condition for next test.

        finder.setSearchDomain(IdentifiedObjectFinder.Domain.DECLARATION);
        assertNull(finder.findSingleton(search),
                   "Should not find WGS84 without a full scan, since it does not contains the CRS:84 identifier.");

        finder.setSearchDomain(IdentifiedObjectFinder.Domain.VALID_DATASET);
        assertSame(CRS84, finder.findSingleton(search),
                   "A full scan should allow us to find WGS84, since it is equal ignoring metadata to CRS:84.");
    }

    /**
     * Tests the {@link IdentifiedObjectFinder#findSingleton(IdentifiedObject)} method through the finder
     * provided by {@link ConcurrentAuthorityFactory}. The objects found are expected to be cached.
     *
     * @throws FactoryException if the creation of a CRS failed.
     */
    @Test
    public void testFindOnCachingInstance() throws FactoryException {
        factory = new Cached(factory);
        testFindSingleton();
    }

    /**
     * An authority factory to be used by {@link IdentifiedObjectFinderTest#testFindOnCachingInstance()}.
     */
    private static final class Cached extends ConcurrentAuthorityFactory<GeodeticAuthorityFactory>
            implements CRSAuthorityFactory
    {
        private final GeodeticAuthorityFactory factory;

        public Cached(final GeodeticAuthorityFactory factory) {
            super(GeodeticAuthorityFactory.class);
            this.factory = factory;
        }

        @Override
        protected GeodeticAuthorityFactory newDataAccess() {
            return factory;
        }
    }
}
