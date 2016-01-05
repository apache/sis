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

import java.util.Collections;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;

// Test imports
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link IdentifiedObjectFinder}.
 * This test uses {@link CommonAuthorityFactory} as a simple factory implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({CommonAuthorityFactoryTest.class, AuthorityFactoryProxyTest.class})
public final strictfp class IdentifiedObjectFinderTest extends TestCase {
    /**
     * The factory to use for the test.
     */
    private GeodeticAuthorityFactory factory;

    /**
     * Initializes the factory to test.
     */
    public IdentifiedObjectFinderTest() {
        factory = new CommonAuthorityFactory(DefaultFactories.forBuildin(NameFactory.class));
    }

    /**
     * Tests the {@link IdentifiedObjectFinder#find(IdentifiedObject)} method.
     *
     * @throws FactoryException if the creation of a CRS failed.
     */
    @Test
    public void testFind() throws FactoryException {
        final GeographicCRS CRS84 = factory.createGeographicCRS("CRS:84");
        final IdentifiedObjectFinder finder = factory.createIdentifiedObjectFinder(CoordinateReferenceSystem.class);
        assertTrue("Newly created finder should default to full scan.", finder.isFullScanAllowed());

        finder.setFullScanAllowed(false);
        assertSame("Should find without the need for scan, since we can use the CRS:84 identifier.",
                   CRS84, finder.find(CRS84));

        finder.setFullScanAllowed(true);
        assertSame("Allowing scanning should not make any difference for this CRS84 instance.",
                   CRS84, finder.find(CRS84));
        /*
         * Same test than above, using a CRS without identifier.
         * The intend is to force a full scan.
         */
        final CoordinateReferenceSystem search = new DefaultGeographicCRS(
                Collections.singletonMap(DefaultGeographicCRS.NAME_KEY, CRS84.getName()),
                CRS84.getDatum(), CRS84.getCoordinateSystem());
        assertEqualsIgnoreMetadata(CRS84, search);              // Required condition for next test.

        finder.setFullScanAllowed(false);
        assertNull("Should not find WGS84 without a full scan, since it does not contains the CRS:84 identifier.",
                   finder.find(search));

        finder.setFullScanAllowed(true);
        assertSame("A full scan should allow us to find WGS84, since it is equals ignoring metadata to CRS:84.",
                   CRS84, finder.find(search));

        assertEquals("CRS:84", finder.findIdentifier(search));
    }

    /**
     * Tests the {@link IdentifiedObjectFinder#find(IdentifiedObject)} method through the finder provided by
     * {@link ConcurrentAuthorityFactory}. The objects found are expected to be cached.
     *
     * @throws FactoryException if the creation of a CRS failed.
     */
    @Test
    @DependsOnMethod("testFind")
    public void testFindOnCachedInstance() throws FactoryException {
        factory = new Cached(factory);
        testFind();
    }

    /**
     * An authority factory to be used by {@link IdentifiedObjectFinderTest#testFindOnCachedInstance()}.
     */
    private static final strictfp class Cached extends ConcurrentAuthorityFactory<GeodeticAuthorityFactory>
            implements CRSAuthorityFactory
    {
        private final GeodeticAuthorityFactory factory;

        public Cached(final GeodeticAuthorityFactory factory) {
            super(GeodeticAuthorityFactory.class, factory.nameFactory);
            this.factory = factory;
        }

        @Override
        protected GeodeticAuthorityFactory newDataAccess() {
            return factory;
        }
    }
}
