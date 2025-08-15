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
package org.apache.sis.util;

import static org.apache.sis.util.StringBuilders.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link StringBuilders} methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class StringBuildersTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public StringBuildersTest() {
    }

    /**
     * Tests the {@link StringBuilders#trimWhitespaces(StringBuilder, int, int)} method.
     */
    @Test
    public void testTrimWhitespaces() {
        final var buffer = new StringBuilder("   Foo bar ");
        int expected = 4;
        do {
            assertEquals(expected, StringBuilders.trimWhitespaces(buffer, 0, buffer.length()));
            assertEquals("Foo bar", buffer.toString());
        } while ((expected ^= 4) == 0);
    }

    /**
     * Tests the {@link StringBuilders#replace(StringBuilder, String, String)} method.
     */
    @Test
    public void testReplace() {
        final var buffer = new StringBuilder("One two three two one");
        replace(buffer, "two", "zero");
        assertEquals("One zero three zero one", buffer.toString());
        replace(buffer, "zero", "ten");
        assertEquals("One ten three ten one", buffer.toString());
    }

    /**
     * Tests the {@link StringBuilders#replace(StringBuilder, char, char)} method.
     */
    @Test
    public void testReplaceChar() {
        final var buffer = new StringBuilder("ABCDEFBCD");
        replace(buffer, 'B', '*');
        assertEquals("A*CDEF*CD", buffer.toString());
    }

    /**
     * Tests the {@link StringBuilders#replace(StringBuilder, int, int, char[])} method.
     */
    @Test
    public void testReplaceChars() {
        final var buffer = new StringBuilder("ABCD1234EFGH");
        replace(buffer, 4, 8, new char[] {'5','6','7','8'});
        assertEquals("ABCD5678EFGH", buffer.toString());
        replace(buffer, 4, 6, new char[] {'1','2','3','4'});
        assertEquals("ABCD123478EFGH", buffer.toString());
        replace(buffer, 8, 10, new char[] {'a','b','c','d'});
        assertEquals("ABCD1234abcdEFGH", buffer.toString());
    }

    /**
     * Tests the {@link StringBuilders#remove(StringBuilder, String)} method.
     */
    @Test
    public void testRemove() {
        final var buffer = new StringBuilder("EPSG.6.7");
        remove(buffer, ".");
        assertEquals("EPSG67", buffer.toString());
    }

    /**
     * Tests the {@link StringBuilders#repeat(StringBuilder, int, char, int)} method.
     */
    @Test
    public void testRepeat() {
        final var buffer = new StringBuilder("AB12");
        repeat(buffer, 2, 'C', 0);
        assertEquals("AB12", buffer.toString());
        repeat(buffer, 2, 'C', 1);
        assertEquals("ABC12", buffer.toString());
        repeat(buffer, 3, '0', 4);
        assertEquals("ABC000012", buffer.toString());
        repeat(buffer, 6, ' ', 2);
        assertEquals("ABC000  012", buffer.toString());
        repeat(buffer, 6, '.', 3);
        assertEquals("ABC000...  012", buffer.toString());
    }

    /**
     * Tests the {@link StringBuilders#trimFractionalPart(StringBuilder)} method.
     */
    @Test
    public void testTrimFractionalPart() {
        final var buffer = new StringBuilder("4.10");
        trimFractionalPart(buffer);
        assertEquals("4.10", buffer.toString());
        buffer.setCharAt(2, '0');                                   // Replace the '1' by '0'.
        trimFractionalPart(buffer);
        assertEquals("4", buffer.toString());
    }

    /**
     * Tests the {@link StringBuilders#toASCII(StringBuilder)} method.
     *
     * @see CharSequencesTest#testToASCII()
     */
    @Test
    public void testToASCII() {
        final var s = new StringBuilder(
                "mètres" + Characters.PARAGRAPH_SEPARATOR +
                " ‘single’, “double”, \"ascii' 30°20′10″.");
        toASCII(s);
        assertEquals("metres\n 'single', \"double\", \"ascii' 30°20'10\".", s.toString());
        /*
         * Characters below are in the Unicode CJK compatibility block. They are not intended for normal use.
         * Note that in the decomposition form of ㎲, the first character is the Greek letter μ (U+03BC) instead
         * of the micro sign µ (U+0085).
         */
        s.setLength(0);
        toASCII(s.append("㎏, ㎎, ㎝, ㎞, ㎢, ㎦, ㎖, ㎧, ㎩, ㎐, ㏖, ㎳, ㎲, ㎥, ㎭"));
        assertEquals("kg, mg, cm, km, km2, km3, ml, m/s, Pa, Hz, mol, ms, μs, m3, rad", s.toString());
        /*
         * Characters below are not from Unicode compatibility block, but nevertheless need decomposition or
         * replacement. Note that the first K is the Kelvin sign (U+212A), to be replaced by the K letter.
         */
        s.setLength(0);
        toASCII(s.append("℃, K, m⋅s"));
        assertEquals("°C, K, m*s", s.toString());
        /*
         * Tests the shortcut code path.
         */
        s.setLength(0);
        toASCII(s.append("ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ"));
        assertEquals(    "AAAAAAÆCEEEEIIIIDNOOOOO*OUUUUYÞsaaaaaaæceeeeiiiionooooo/ouuuuyþy", s.toString());
    }
}
