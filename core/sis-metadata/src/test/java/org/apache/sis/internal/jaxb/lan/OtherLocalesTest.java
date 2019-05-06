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
package org.apache.sis.internal.jaxb.lan;

import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.nio.charset.Charset;
import org.junit.Test;
import org.apache.sis.test.TestCase;

import static java.util.Locale.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link OtherLocales} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.5
 * @module
 */
public final strictfp class OtherLocalesTest extends TestCase {
    /**
     * Returns the locales in an array. Character sets are ignored.
     */
    private static Locale[] toArray(final Set<PT_Locale> locales) {
        final Locale[] languages = new Locale[locales.size()];
        int i = 0;
        for (final PT_Locale p : locales) {
            languages[i++] = p.getLocale();
        }
        assertEquals(i, languages.length);
        return languages;
    }

    /**
     * Tests {@link OtherLocales#filter(Map)}.
     */
    @Test
    public void testFilter() {
        final Map<Locale,Charset> languages = new LinkedHashMap<>();
        assertNull(OtherLocales.filter(languages));
        /*
         * The first locale in the 'languages' list is taken as the default locale.
         * It shall not appears in the 'other locales' collection.
         */
        assertNull(languages.put(ENGLISH, null));
        final Set<PT_Locale> otherLocales = OtherLocales.filter(languages);
        assertEquals("size", 0, otherLocales.size());
        /*
         * All elements after the first one in the 'language' list are "other locales".
         */
        assertNull(languages.put(FRENCH, null));
        assertEquals("size", 1, otherLocales.size());
        assertArrayEquals(new Locale[] {FRENCH}, toArray(otherLocales));
        /*
         * Adding to the "other locales" collection shall delegate to the 'languages' list.
         */
        assertTrue(otherLocales.add(new PT_Locale(JAPANESE)));
        assertEquals("size", 2, otherLocales.size());
        assertArrayEquals(new Locale[] {FRENCH, JAPANESE}, toArray(otherLocales));
        assertArrayEquals(new Locale[] {ENGLISH, FRENCH, JAPANESE}, languages.keySet().toArray());
        /*
         * Clearing the "other locales" list shall not remove the default locale.
         */
        otherLocales.clear();
        assertEquals("size", 0, otherLocales.size());
        assertArrayEquals(new Locale[] {ENGLISH}, languages.keySet().toArray());
        /*
         * The first 'add' operation on an empty 'languages' list generates a default locale.
         * Note that we can not test the first element of 'languages', since it is system-dependent.
         */
        languages.clear();
        assertTrue(otherLocales.add(new PT_Locale(FRENCH)));
        assertArrayEquals(new Locale[] {FRENCH}, toArray(otherLocales));
        assertEquals("size", 2, languages.size());
    }

    /**
     * Tests {@link OtherLocales#setFirst(Map, PT_Locale)}.
     */
    @Test
    public void testSetFirst() {
        Map<Locale,Charset> merged = OtherLocales.setFirst(null, null);
        assertNull(merged);

        merged = OtherLocales.setFirst(null, new PT_Locale(ENGLISH));
        assertArrayEquals(new Locale[] {ENGLISH}, merged.keySet().toArray());

        merged.put(FRENCH, null);
        merged.put(JAPANESE, null);
        merged = OtherLocales.setFirst(merged, new PT_Locale(GERMAN));
        assertArrayEquals(new Locale[] {GERMAN, FRENCH, JAPANESE}, merged.keySet().toArray());
    }
}
