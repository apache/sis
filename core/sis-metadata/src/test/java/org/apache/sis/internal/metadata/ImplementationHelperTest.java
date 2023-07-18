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

import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.util.Locale.*;


/**
 * Tests the {@link ImplementationHelper} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   0.3
 */
public final class ImplementationHelperTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ImplementationHelperTest() {
    }

    /**
     * Tests {@link ImplementationHelper#toMilliseconds(Date)}.
     */
    @Test
    public void testToMilliseconds() {
        assertEquals(1000,           ImplementationHelper.toMilliseconds(new Date(1000)));
        assertEquals(Long.MIN_VALUE, ImplementationHelper.toMilliseconds(null));
    }

    /**
     * Tests {@link ImplementationHelper#toDate(long)}.
     */
    @Test
    public void testToDate() {
        assertEquals(new Date(1000), ImplementationHelper.toDate(1000));
        assertNull(ImplementationHelper.toDate(Long.MIN_VALUE));
    }

    /**
     * Tests the {@link ImplementationHelper#setFirst(Collection, Object)} method.
     */
    @Test
    public void testSetFirst() {
        Collection<Locale> locales = ImplementationHelper.setFirst(null, null);
        assertTrue(locales.isEmpty());

        locales = ImplementationHelper.setFirst(null, GERMAN);
        assertArrayEquals(new Locale[] {GERMAN}, locales.toArray());

        locales = Arrays.asList(ENGLISH, JAPANESE, FRENCH);                 // Content will be modified.
        assertSame("Shall set value in-place.", locales, ImplementationHelper.setFirst(locales, GERMAN));
        assertArrayEquals(new Locale[] {GERMAN, JAPANESE, FRENCH}, locales.toArray());

        locales = new LinkedHashSet<>(List.of(ENGLISH, JAPANESE, FRENCH));
        locales = ImplementationHelper.setFirst(locales, ITALIAN);
        assertArrayEquals(new Locale[] {ITALIAN, JAPANESE, FRENCH}, locales.toArray());

        locales = ImplementationHelper.setFirst(locales, null);
        assertArrayEquals(new Locale[] {JAPANESE, FRENCH}, locales.toArray());
    }
}
