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
package org.apache.sis.io.wkt;

import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Convention} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.20)
 * @version 0.4
 * @module
 */
public final strictfp class ConventionTest extends TestCase {
    /**
     * Tests all citations associated with enum values.
     */
    @Test
    public void testGetCitation() {
        for (final Convention convention : Convention.values()) {
            final Citation citation = convention.getAuthority();
            if (convention != Convention.PROJ4 && convention != Convention.INTERNAL) {
                assertTrue(convention.name(), convention.name().equalsIgnoreCase(Citations.getIdentifier(citation)));
            }
        }
    }

    /**
     * Tests {@link Convention#forCitation(Citation, Convention)}.
     */
    @Test
    public void testForCitation() {
        assertSame(Convention.OGC,     Convention.forCitation(Citations.OGC,     null));
        assertSame(Convention.EPSG,    Convention.forCitation(Citations.EPSG,    null));
        assertSame(Convention.ESRI,    Convention.forCitation(Citations.ESRI,    null));
        assertSame(Convention.ORACLE,  Convention.forCitation(Citations.ORACLE,  null));
        assertSame(Convention.NETCDF,  Convention.forCitation(Citations.NETCDF,  null));
        assertSame(Convention.GEOTIFF, Convention.forCitation(Citations.GEOTIFF, null));
        assertSame(Convention.PROJ4,   Convention.forCitation(Citations.PROJ4,   null));
    }

    /**
     * Tests {@link Convention#forIdentifier(String, Convention)}.
     */
    @Test
    public void testForIdentifier() {
        assertSame(Convention.OGC,     Convention.forIdentifier("OGC",     null));
        assertSame(Convention.EPSG,    Convention.forIdentifier("EPSG",    null));
        assertSame(Convention.ESRI,    Convention.forIdentifier("ESRI",    null));
        assertSame(Convention.ORACLE,  Convention.forIdentifier("ORACLE",  null));
        assertSame(Convention.NETCDF,  Convention.forIdentifier("NETCDF",  null));
        assertSame(Convention.GEOTIFF, Convention.forIdentifier("GEOTIFF", null));
        assertSame(Convention.PROJ4,   Convention.forIdentifier("PROJ4",   null));
    }
}
