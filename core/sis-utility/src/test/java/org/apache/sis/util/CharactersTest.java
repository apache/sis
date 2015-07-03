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

import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.util.Characters.*;


/**
 * Tests the {@link Characters} utility methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
public final strictfp class CharactersTest extends TestCase {
    /**
     * Tests the value of {@link Characters#LINE_SEPARATOR}
     * and {@link Characters#PARAGRAPH_SEPARATOR} constant value.
     */
    @Test
    public void testUnicodeSeparators() {
        assertEquals(Character.LINE_SEPARATOR,      Character.getType(LINE_SEPARATOR));
        assertEquals(Character.PARAGRAPH_SEPARATOR, Character.getType(PARAGRAPH_SEPARATOR));
    }

    /**
     * Tests the {@link Characters#isValidWKT(int)} method.
     */
    @Test
    public void testIsValidWKT() {
        final String valids = "_[](){}<=>.,:;+- %&'\"*^/\\?|°";
        for (char c=0; c<256; c++) {
            final boolean valid = (c >= 'A' && c <= 'Z')
                               || (c >= 'a' && c <= 'z')
                               || (c >= '0' && c <= '9')
                               || valids.indexOf(c) >= 0;
            assertEquals(valid, isValidWKT(c));
        }
    }

    /**
     * Tests the {@link Characters#isLineOrParagraphSeparator(int)} method.
     */
    @Test
    @DependsOnMethod("testUnicodeSeparators")
    public void testLineOrParagraphSeparator() {
        assertFalse(isLineOrParagraphSeparator('z'));
        assertFalse(isLineOrParagraphSeparator('\t'));
        assertTrue (isLineOrParagraphSeparator('\r'));
        assertTrue (isLineOrParagraphSeparator('\n'));
        assertTrue (isLineOrParagraphSeparator(LINE_SEPARATOR));
        assertTrue (isLineOrParagraphSeparator(PARAGRAPH_SEPARATOR));
    }

    /**
     * Tests the {@link Characters#isHexadecimal()} method.
     *
     * @since 0.5
     */
    @Test
    public void testIsHexadecimal() {
        assertTrue(isHexadecimal('0'));
        assertTrue(isHexadecimal('5'));
        assertTrue(isHexadecimal('9'));
        assertTrue(isHexadecimal('A'));
        assertTrue(isHexadecimal('C'));
        assertTrue(isHexadecimal('F'));
        assertTrue(isHexadecimal('a'));
        assertTrue(isHexadecimal('c'));
        assertTrue(isHexadecimal('f'));

        assertFalse(isHexadecimal(' '));
        assertFalse(isHexadecimal('_'));
        assertFalse(isHexadecimal(':'));
        assertFalse(isHexadecimal('/'));
        assertFalse(isHexadecimal('>'));
        assertFalse(isHexadecimal('@'));
        assertFalse(isHexadecimal('`'));
        assertFalse(isHexadecimal('G'));
        assertFalse(isHexadecimal('Q'));
        assertFalse(isHexadecimal('g'));
        assertFalse(isHexadecimal('q'));
    }

    /**
     * Tests {@link Characters#toSuperScript(char)}.
     */
    @Test
    public void testSuperScript() {
        for (char c='0'; c<='9'; c++) {
            final char s = toSuperScript(c);
            assertFalse(s == c);
            assertFalse(isSuperScript(c));
            assertTrue (isSuperScript(s));
            assertEquals(c, toNormalScript(s));
        }
        final char c = 'A';
        assertEquals(c, toSuperScript(c));
        assertEquals(c, toNormalScript(c));
        assertFalse(isSuperScript(c));
    }

    /**
     * Tests {@link Characters#toSubScript(char)}.
     */
    @Test
    public void testSubScript() {
        for (char c='0'; c<='9'; c++) {
            final char s = toSubScript(c);
            assertFalse(s == c);
            assertFalse(isSubScript(c));
            assertTrue (isSubScript(s));
            assertEquals(c, toNormalScript(s));
        }
        final char c = 'a';
        assertEquals(c, toSubScript(c));
        assertEquals(c, toNormalScript(c));
        assertFalse(isSubScript(c));
    }

    /**
     * Tests the pre-defined {@link org.apache.sis.util.Characters.Filter} constants.
     */
    @Test
    public void testPredefinedFilters() {
        assertTrue (Filter.UNICODE_IDENTIFIER.contains('a'));
        assertTrue (Filter.LETTERS_AND_DIGITS.contains('a'));
        assertTrue (Filter.UNICODE_IDENTIFIER.contains('_'));
        assertFalse(Filter.LETTERS_AND_DIGITS.contains('_'));
        assertFalse(Filter.UNICODE_IDENTIFIER.contains(' '));
        assertFalse(Filter.LETTERS_AND_DIGITS.contains(' '));
    }

    /**
     * Tests the {@link org.apache.sis.util.Characters.Filter#forTypes(byte[])} method.
     */
    @Test
    public void testFilterForTypes() {
        final Filter filter = Filter.forTypes(Character.SPACE_SEPARATOR, Character.DECIMAL_DIGIT_NUMBER);
        assertTrue (filter.contains('0'));
        assertTrue (filter.contains(' '));
        assertFalse(filter.contains('A'));
    }

    /**
     * Scans the full {@code char} range in order to check for
     * {@link org.apache.sis.util.Characters.Filter} consistency.
     */
    @Test
    public void scanCharacterRange() {
        for (int c=Character.MIN_VALUE; c<=Character.MAX_VALUE; c++) {
            final int type = Character.getType(c);
predefined: for (int i=0; ; i++) {
                final Characters.Filter filter;
                switch (i) {
                    case 0:  filter = Filter.UNICODE_IDENTIFIER; break;
                    case 1:  filter = Filter.LETTERS_AND_DIGITS; break;
                    default: break predefined;
                }
                final boolean cc = filter.contains(c);
                final boolean ct = filter.containsType(type);
                if (cc != ct) {
                    fail(filter + ".contains('" + (char) c + "') == " + cc + " but "
                            + filter + ".containsType(" + type + ") == " + ct);
                }
            }
        }
    }
}
