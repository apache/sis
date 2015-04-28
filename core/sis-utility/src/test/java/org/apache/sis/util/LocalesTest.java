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
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Locales} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@DependsOn(ArraysExtTest.class)
public final strictfp class LocalesTest extends TestCase {
    /**
     * Tests the {@link Locales#getAvailableLanguages()} method.
     */
    @Test
    public void testGetAvailableLanguages() {
        final Locale[] locales = Locales.ALL.getAvailableLanguages();
        assertTrue ("Expected English locale.",         ArraysExt.contains(locales, Locale.ENGLISH));
        assertFalse("US is a country, not a language.", ArraysExt.contains(locales, Locale.US));
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
        assertSame(Locale.ENGLISH, Locales.unique(new Locale("en")));
        assertSame(Locale.FRENCH,  Locales.unique(new Locale("fr")));
    }

    /**
     * Tests the {@link Locales#parse(String)} method.
     */
    @Test
    @DependsOnMethod("testUnique")
    public void testParse() {
        assertSame(Locale.ENGLISH,       Locales.parse("en"));
        assertSame(Locale.FRENCH,        Locales.parse("fr"));
        assertSame(Locale.FRENCH,        Locales.parse("fra"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fr_CA"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fra_CA"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fr_CAN"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fra_CAN"));
        assertSame(Locale.JAPAN,         Locales.parse("ja_JP"));

        assertEquals(new Locale("de", "DE"),            Locales.parse("de_DE"));
        assertEquals(new Locale("",   "GB"),            Locales.parse("_GB"));
        assertEquals(new Locale("en", "US", "WINDOWS"), Locales.parse("en_US_WINDOWS"));
        assertEquals(new Locale("de", "",   "POSIX"),   Locales.parse("de__POSIX"));
    }

    /**
     * Tests the {@link Locales#parse(String)} method with a IETF BCP 47 language tag string.
     * This functionality is supported only on the JDK7 branch.
     */
    @Test
    @org.junit.Ignore("Not supported on the JDK6 branch")
    public void testParseIETF() {
        assertEquals(Locale.JAPAN, Locales.parse("ja-JP"));
        assertEquals(new Locale("en", "US", "POSIX"), Locales.parse("en-US-x-lvariant-POSIX"));
    }

    /**
     * Tests that {@link Locales#parse(String)} throw an exception if given an invalid argument.
     */
    @Test
    public void testParseInvalid() {
        try {
            Locales.parse("orange_APPLE");
            fail("Shall not parse invalid locale.");
        } catch (RuntimeException e) { // IllformedLocaleException on the JDK7 branch.
            final String message = e.getMessage();
            assertTrue(message, message.contains("APPLE"));
        }
    }
}
