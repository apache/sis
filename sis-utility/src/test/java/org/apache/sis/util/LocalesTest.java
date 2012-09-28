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

import org.junit.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link Locales} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.04)
 * @version 0.3
 * @module
 */
public final strictfp class LocalesTest {
    /**
     * Tests the {@link Locales#getAvailableLanguages()} method.
     */
    @Test
    public void testGetAvailableLanguages() {
        final Locale[] locales = Locales.ALL.getAvailableLanguages();
        assertTrue ("Expected English locale.",         Arrays.contains(locales, Locale.ENGLISH));
        assertFalse("US is a country, not a language.", Arrays.contains(locales, Locale.US));
    }

    /**
     * Tests the {@link Locales#getAvailableLocales()} method.
     */
    @Test
    public void testGetAvailableLocales() {
        final Locale[] locales = Locales.SIS.getAvailableLocales();
        assertTrue(Arrays.contains(locales, Locale.ENGLISH));
        assertTrue(Arrays.contains(locales, Locale.US));
        assertTrue(Arrays.contains(locales, Locale.CANADA));
        assertTrue(Arrays.contains(locales, Locale.FRANCE));
        assertTrue(Arrays.contains(locales, Locale.CANADA_FRENCH));
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
     * Depends on {@link #testUnique()}.
     */
    @Test
    public void testParse() {
        assertSame(Locale.FRENCH,        Locales.parse("fr"));
        assertSame(Locale.FRENCH,        Locales.parse("fra"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fr_CA"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fra_CA"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fr_CAN"));
        assertSame(Locale.CANADA_FRENCH, Locales.parse("fra_CAN"));
        assertSame(Locale.ENGLISH,       Locales.parse("en"));

        assertEquals(new Locale("de", "DE"),        Locales.parse("de_DE"));
        assertEquals(new Locale("",   "GB"),        Locales.parse("_GB"));
        assertEquals(new Locale("en", "US", "WIN"), Locales.parse("en_US_WIN"));
        assertEquals(new Locale("de", "", "POSIX"), Locales.parse("de__POSIX"));
        assertEquals(new Locale("fr", "", "MAC"),   Locales.parse("fr__MAC"));
    }
}
