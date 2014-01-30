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

import org.apache.sis.test.TestCase;
import org.apache.sis.util.StringBuilders;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Symbols} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-2.4)
 * @version 0.4
 * @module
 */
public final strictfp class SymbolsTest extends TestCase {
    /**
     * Tests the {@link Symbols#containsAxis(CharSequence)} method.
     */
    @Test
    public void testContainsAxis() {
        assertContainsAxis("At beginning of a line.",   true,                  "AXIS[“Long”, EAST]");
        assertContainsAxis("Embeded in GEOGCS.",        true,  "GEOGCS[“WGS84”, AXIS[“Long”, EAST]]");
        assertContainsAxis("Using different brackets.", true,  "GEOGCS[“WGS84”, AXIS (“Long”, EAST)]");
        assertContainsAxis("Mixed cases.",              true,  "GEOGCS[“WGS84”, aXis[“Long”, EAST]]");
        assertContainsAxis("AXIS in quoted text.",      false, "GEOGCS[“AXIS”]");
        assertContainsAxis("Without opening bracket.",  false, "GEOGCS[“WGS84”, AXIS]");
        assertContainsAxis("No AXIS.",                  false, "GEOGCS[“WGS84”]");
    }

    /**
     * Asserts that the call to {@link Symbols#containsAxis(CharSequence)} produce the given result.
     * This method expects an array using the {@code “…”} quotation marks, which will be replaced by
     * the standard {@code '"'} quotation mark after we tested the given string.
     */
    private static void assertContainsAxis(final String message, final boolean expected, final String wkt) {
        assertEquals(message, expected, Symbols.DEFAULT.containsAxis(wkt));
        final StringBuilder buffer = new StringBuilder(wkt);
        StringBuilders.replace(buffer, '“', '"');
        StringBuilders.replace(buffer, '”', '"');
        assertFalse(wkt.contentEquals(buffer));
        assertEquals(message, expected, Symbols.DEFAULT.containsAxis(buffer));
    }
}
