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
package org.apache.sis.util.resources;

import java.util.Locale;

import org.junit.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link IndexedResourceBundle} subclasses.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.2)
 * @version 0.3
 */
public final strictfp class IndexedResourceBundleTest {
    /**
     * Tests the {@link IndexedResourceBundle#getString(int)} method on different locales.
     */
    @Test
    public void testGetString() {
        final Errors english = Errors.getResources(Locale.ENGLISH);
        final Errors french  = Errors.getResources(Locale.FRENCH);
        assertNotSame(english, french);

        assertSame(english, Errors.getResources(Locale.ENGLISH));
        assertSame(english, Errors.getResources(Locale.US));
        assertSame(english, Errors.getResources(Locale.UK));
        assertSame(english, Errors.getResources(Locale.CANADA));
        assertSame(french,  Errors.getResources(Locale.FRENCH));
        assertSame(french,  Errors.getResources(Locale.CANADA_FRENCH));

        assertEquals("Argument ‘{0}’ shall not be null.",     english.getString(Errors.Keys.NullArgument_1));
        assertEquals("L’argument ‘{0}’ ne doit pas être nul.", french.getString(Errors.Keys.NullArgument_1));
    }

    /**
     * Tests the {@link IndexedResourceBundle#getString(int, Object)} method on different locales.
     */
    @Test
    public void testGetStringWithParameter() {
        assertEquals("Argument ‘CRS’ shall not be null.",
                Errors.getResources(Locale.ENGLISH).getString(Errors.Keys.NullArgument_1, "CRS"));
        assertEquals("L’argument ‘CRS’ ne doit pas être nul.",
                Errors.getResources(Locale.FRENCH).getString(Errors.Keys.NullArgument_1, "CRS"));
    }
}
