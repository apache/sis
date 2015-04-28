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
package org.apache.sis.internal.metadata;

import java.util.Locale;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.junit.Test;
import org.apache.sis.test.TestCase;

import static java.util.Locale.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link OtherLocales} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class OtherLocalesTest extends TestCase {
    /**
     * Tests {@link OtherLocales#filter(Collection)}.
     */
    @Test
    public void testFilter() {
        final Collection<Locale> languages = new LinkedHashSet<Locale>();
        final Collection<Locale> otherLocales = OtherLocales.filter(languages);
        assertEquals("size", 0, otherLocales.size());
        /*
         * The first locale in the 'languages' list is taken as the default locale.
         * It shall not appears in the 'other locales' collection.
         */
        assertTrue(languages.add(ENGLISH));
        assertEquals("size", 0, otherLocales.size());
        /*
         * All elements after the first one in the 'language' list are "other locales".
         */
        assertTrue(languages.add(FRENCH));
        assertEquals("size", 1, otherLocales.size());
        assertArrayEquals(new Locale[] {FRENCH}, otherLocales.toArray());
        /*
         * Adding to the "other locales" collection shall delegate to the 'languages' list.
         */
        assertTrue(otherLocales.add(JAPANESE));
        assertEquals("size", 2, otherLocales.size());
        assertArrayEquals(new Locale[] {FRENCH, JAPANESE}, otherLocales.toArray());
        assertArrayEquals(new Locale[] {ENGLISH, FRENCH, JAPANESE}, languages.toArray());
        /*
         * Clearing the "other locales" list shall not remove the default locale.
         */
        otherLocales.clear();
        assertEquals("size", 0, otherLocales.size());
        assertArrayEquals(new Locale[] {ENGLISH}, languages.toArray());
        /*
         * The first 'add' operation on an empty 'languages' list generates a default locale.
         * Note that we can not test the first element of 'languages', since it is system-dependent.
         */
        languages.clear();
        assertTrue(otherLocales.add(FRENCH));
        assertArrayEquals(new Locale[] {FRENCH}, otherLocales.toArray());
        assertEquals("size", 2, languages.size());
    }

    /**
     * Tests {@link OtherLocales#merge(Locale, Collection)}.
     */
    @Test
    public void testMerge() {
        Collection<Locale> merged = OtherLocales.merge(null, null);
        assertTrue(merged.isEmpty());

        merged = OtherLocales.merge(ENGLISH, null);
        assertArrayEquals(new Locale[] {ENGLISH}, merged.toArray());

        merged = OtherLocales.merge(ENGLISH, Arrays.asList(FRENCH, JAPANESE));
        assertArrayEquals(new Locale[] {ENGLISH, FRENCH, JAPANESE}, merged.toArray());
        /*
         * The tricky case: a default locale will be generated. That locale is system-dependent.
         */
        merged = OtherLocales.merge(null, Arrays.asList(FRENCH, JAPANESE));
        final Iterator<Locale> it = merged.iterator();
        assertNotNull(it.next()); // System-dependent value.
        assertEquals(FRENCH,   it.next());
        assertEquals(JAPANESE, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Tests the {@link OtherLocales#setFirst(Collection, Object)} method.
     */
    @Test
    public void testSetFirst() {
        Collection<Locale> locales = OtherLocales.setFirst(null, null);
        assertTrue(locales.isEmpty());

        locales = OtherLocales.setFirst(null, GERMAN);
        assertArrayEquals(new Locale[] {GERMAN}, locales.toArray());

        locales = Arrays.asList(ENGLISH, JAPANESE, FRENCH);
        assertSame("Shall set value in-place.", locales, OtherLocales.setFirst(locales, GERMAN));
        assertArrayEquals(new Locale[] {GERMAN, JAPANESE, FRENCH}, locales.toArray());

        locales = new LinkedHashSet<Locale>(Arrays.asList(ENGLISH, JAPANESE, FRENCH));
        locales = OtherLocales.setFirst(locales, ITALIAN);
        assertArrayEquals(new Locale[] {ITALIAN, JAPANESE, FRENCH}, locales.toArray());

        locales = OtherLocales.setFirst(locales, null);
        assertArrayEquals(new Locale[] {JAPANESE, FRENCH}, locales.toArray());
    }
}
