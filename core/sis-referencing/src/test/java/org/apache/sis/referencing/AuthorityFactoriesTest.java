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

import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.logging.Logging;
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
     * A JUnit {@linkplain Rule rule} for listening to log events. This field is public because JUnit requires
     * us to do so, but should be considered as an implementation details (it should have been a private field).
     */
    @Rule
    public final LoggingWatcher listener = new Listener();

    /**
     * Implementation of the {@link AuthorityFactoriesTest#listener} rule.
     */
    private static final class Listener extends LoggingWatcher {
        /** The logged message. */
        String message;

        /** Creates a new log listener. */
        Listener() {super(Logging.getLogger(Loggers.CRS_FACTORY));}

        /** Stores the log message. */
        @Override protected void verifyMessage(final String message) {
            this.message = message;
        }
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

        listener.maximumLogCount = 1;
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:EPSG:6.11.2:4326"));
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:EPSG:6.11.2:4326"));
        String message = ((Listener) listener).message;
        assertTrue(message, message.contains("6.11.2"));

        listener.maximumLogCount = 1;
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:EPSG:7.04:4326"));
        message = ((Listener) listener).message;
        assertTrue(message, message.contains("7.04"));

        listener.maximumLogCount = 1;
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:EPSG:7.10:4326"));
        message = ((Listener) listener).message;
        assertTrue(message, message.contains("7.10"));

        assertEquals(0, listener.maximumLogCount);
        assertSame(crs, factory.createGeographicCRS("urn:ogc:def:crs:EPSG::4326"));
    }
}
