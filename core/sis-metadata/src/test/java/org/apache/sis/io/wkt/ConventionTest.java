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

import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Convention} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class ConventionTest extends TestCase {
    /**
     * Tests {@link Convention#getNameAuthority()}.
     */
    @Test
    public void testGetNameAuthority() {
        assertSame(Citations.EPSG, Convention.WKT2.getNameAuthority());
        assertSame(Citations.EPSG, Convention.WKT2_SIMPLIFIED.getNameAuthority());
        assertSame(Citations.OGC,  Convention.WKT1.getNameAuthority());
        assertSame(Citations.OGC,  Convention.WKT1_COMMON_UNITS.getNameAuthority());
        assertSame(Citations.EPSG, Convention.INTERNAL.getNameAuthority());
    }

    /**
     * Tests {@link Convention#majorVersion()}.
     */
    @Test
    public void testVersion() {
        assertEquals(2, Convention.WKT2.majorVersion());
        assertEquals(2, Convention.WKT2_SIMPLIFIED.majorVersion());
        assertEquals(1, Convention.WKT1.majorVersion());
        assertEquals(1, Convention.WKT1_COMMON_UNITS.majorVersion());
        assertEquals(2, Convention.INTERNAL.majorVersion());
    }

    /**
     * Tests {@link Convention#isSimplified()}.
     */
    @Test
    public void testIsSimple() {
        assertFalse(Convention.WKT2.isSimplified());
        assertTrue (Convention.WKT2_SIMPLIFIED.isSimplified());
        assertTrue (Convention.WKT1.isSimplified());
        assertTrue (Convention.WKT1_COMMON_UNITS.isSimplified());
        assertTrue (Convention.INTERNAL.isSimplified());
    }
}
