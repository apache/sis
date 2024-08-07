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

import java.util.Locale;
import java.util.IllformedLocaleException;
import org.apache.sis.pending.jdk.JDK19;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link Locales} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class LocalesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public LocalesTest() {
    }

    /**
     * Tests the {@link Locales#getAvailableLanguages()} method.
     */
    @Test
    public void testGetAvailableLanguages() {
        final Locale[] locales = Locales.ALL.getAvailableLanguages();
        assertTrue (ArraysExt.contains(locales, Locale.ENGLISH));
        assertFalse(ArraysExt.contains(locales, Locale.US));        // US is a country, not a language.
    }

    /**
     * Tests the {@link Locales#getAvailableLocales()} method.
     */
    @Test
    public void testGetAvailableLocales() {
        final Locale[] locales = Locales.SIS.getAvailableLocales();
        assertTrue(ArraysExt.contains(locales, Locale.ENGLISH));
        assertTrue(ArraysExt.contains(locales, Locale.US));
        assertTrue(ArraysExt.contains(locales, Locale.CANADA));
        assertTrue(ArraysExt.contains(locales, Locale.FRANCE));
        assertTrue(ArraysExt.contains(locales, Locale.CANADA_FRENCH));
    }

    /**
     * Tests the {@link Locales#unique(Locale)} method.
     */
    @Test
    public void testUnique() {
        assertSame(Locale.ENGLISH, Locales.unique(JDK19.localeOf("en")));
        assertSame(Locale.FRENCH,  Locales.unique(JDK19.localeOf("fr")));
    }

    /**
     * Tests the {@link Locales#parse(String)} method.
     */
    @Test
    public void testParse() {
        assertSame(Locale.ENGLISH,       Locales.parse("en"));
        assertSame(Locale.FRENCH,        Locales.parse("fr"));
        assertSame(Locale.FRENCH,        Locales.parse("fra"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fr_CA"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fra_CA"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fr_CAN"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fra_CAN"));
        assertSame(Locale.JAPAN,         Locales.parse("ja_JP"));
        assertSame(Locale.US,            Locales.parse("en; USA"));

        assertEquals(JDK19.localeOf("de", "DE"),            Locales.parse("de_DE"));
        assertEquals(JDK19.localeOf("",   "GB"),            Locales.parse("_GB"));
        assertEquals(JDK19.localeOf("en", "US", "WINDOWS"), Locales.parse("en_US_WINDOWS"));
        assertEquals(JDK19.localeOf("de", "",   "POSIX"),   Locales.parse("de__POSIX"));
    }

    /**
     * Tests the {@link Locales#parse(String)} method with a IETF BCP 47 language tag string.
     */
    @Test
    public void testParseIETF() {
        assertEquals(Locale.JAPAN, Locales.parse("ja-JP"));
        assertEquals(JDK19.localeOf("en", "US", "POSIX"), Locales.parse("en-US-x-lvariant-POSIX"));
    }

    /**
     * Tests that {@link Locales#parse(String)} throw an exception if given an invalid argument.
     */
    @Test
    public void testParseInvalid() {
        var e = assertThrows(IllformedLocaleException.class, () -> Locales.parse("orange_APPLE"));
        assertMessageContains(e, "APPLE");
    }
}
