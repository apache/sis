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

import java.util.Locale;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.StringBuilders;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link Symbols} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
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
        assertEquals(message, expected, Symbols.getDefault().containsAxis(wkt));
        final StringBuilder buffer = new StringBuilder(wkt);
        StringBuilders.replace(buffer, '“', '"');
        StringBuilders.replace(buffer, '”', '"');
        assertFalse(wkt.contentEquals(buffer));
        assertEquals(message, expected, Symbols.getDefault().containsAxis(buffer));
    }

    /**
     * Ensures that the static constants are immutable.
     */
    @Test
    public void testImmutability() {
        try {
            Symbols.SQUARE_BRACKETS.setPairedBrackets("()", "[]");
            fail("Constant shall be immutable.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message, message.contains("Symbols"));
        }
        try {
            Symbols.CURLY_BRACKETS.setLocale(Locale.FRENCH);
            fail("Constant shall be immutable.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message, message.contains("Symbols"));
        }
    }

    /**
     * Tests {@link Symbols} serialization.
     */
    @Test
    public void testSerialization() {
        assertSame(Symbols.SQUARE_BRACKETS, assertSerializedEquals(Symbols.SQUARE_BRACKETS));
        assertSame(Symbols.CURLY_BRACKETS,  assertSerializedEquals(Symbols.CURLY_BRACKETS));
        /*
         * Test with a new instance using a closing quote symbol different than the opening one.
         * This is necessary in order to ensure that the symbol is recomputed correctly.
         */
        final Symbols symbols = new Symbols(Symbols.CURLY_BRACKETS);
        assertEquals("quote", "\"", symbols.getQuote());
        symbols.setPairedQuotes("“”", "\"\"");
        assertEquals("quote", "”", symbols.getQuote());
        final Symbols c = assertSerializedEquals(symbols);
        assertNotSame(symbols, c); // Expect a new instance.
        assertEquals("quote", "”", c.getQuote()); // Verify the recomputed value.
    }
}
