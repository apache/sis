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
import org.apache.sis.util.StringBuilders;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the {@link Symbols} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SymbolsTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public SymbolsTest() {
    }

    /**
     * Tests the {@link Symbols#containsElement(CharSequence, String)} method.
     */
    @Test
    public void testContainsElement() {
        assertContainsAxis("At beginning of a line.",   true,                  "AXIS[“Long”, EAST]");
        assertContainsAxis("Embeded in GEOGCS.",        true,  "GEOGCS[“WGS84”, AXIS[“Long”, EAST]]");
        assertContainsAxis("Using different brackets.", true,  "GEOGCS[“WGS84”, AXIS (“Long”, EAST)]");
        assertContainsAxis("Mixed cases.",              true,  "GEOGCS[“WGS84”, aXis[“Long”, EAST]]");
        assertContainsAxis("AXIS in quoted text.",      false, "GEOGCS[“AXIS”]");
        assertContainsAxis("Without opening bracket.",  false, "GEOGCS[“WGS84”, AXIS]");
        assertContainsAxis("No AXIS.",                  false, "GEOGCS[“WGS84”]");
    }

    /**
     * Asserts that the call to {@code Symbols.containsElement(wkt, "AXIS")} produce the given result.
     * This method expects an array using the {@code “…”} quotation marks, which will be replaced by
     * the standard {@code '"'} quotation mark after we tested the given string.
     */
    private static void assertContainsAxis(final String message, final boolean expected, final String wkt) {
        assertEquals(expected, Symbols.getDefault().containsElement(wkt, "AXIS"), message);
        final StringBuilder buffer = new StringBuilder(wkt);
        StringBuilders.replace(buffer, '“', '"');
        StringBuilders.replace(buffer, '”', '"');
        assertFalse(wkt.contentEquals(buffer));
        assertEquals(expected, Symbols.getDefault().containsElement(buffer, "AXIS"), message);
    }

    /**
     * Ensures that the static constants are immutable.
     */
    @Test
    public void testImmutability() {
        UnsupportedOperationException exception;
        exception = assertThrows(UnsupportedOperationException.class,
                () -> Symbols.SQUARE_BRACKETS.setPairedBrackets("()", "[]"), "Constant shall be immutable.");
        assertMessageContains(exception, "Symbols");

        exception = assertThrows(UnsupportedOperationException.class,
                () -> Symbols.CURLY_BRACKETS.setLocale(Locale.FRENCH), "Constant shall be immutable.");
        assertMessageContains(exception, "Symbols");
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
        assertEquals("\"", symbols.getQuote(), "quote");
        symbols.setPairedQuotes("“”", "\"\"");
        assertEquals("”", symbols.getQuote(), "quote");
        final Symbols c = assertSerializedEquals(symbols);
        assertNotSame(symbols, c, "Expected a new instance.");
        assertEquals("”", c.getQuote(), "quote");               // Verify the recomputed value.
    }
}
