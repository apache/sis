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

import java.io.IOException;
import java.util.Locale;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.opengis.util.InternationalString;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link IndexedResourceBundle} subclasses.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.2)
 * @version 0.3
 */
@DependsOn(org.apache.sis.util.ArraysTest.class)
public final strictfp class IndexedResourceBundleTest extends TestCase {
    /**
     * Tests the {@link Errors#getResources(Locale)} method on different locales.
     */
    @Test
    public void testGetResources() {
        final Errors english = Errors.getResources(Locale.ENGLISH);
        final Errors french  = Errors.getResources(Locale.FRENCH);
        assertNotSame(english, french);

        assertSame(english, Errors.getResources(Locale.ENGLISH));
        assertSame(english, Errors.getResources(Locale.US));
        assertSame(english, Errors.getResources(Locale.UK));
        assertSame(english, Errors.getResources(Locale.CANADA));
        assertSame(french,  Errors.getResources(Locale.FRENCH));
        assertSame(french,  Errors.getResources(Locale.CANADA_FRENCH));
    }

    /**
     * Tests the {@link IndexedResourceBundle#list(Appendable)} method.
     *
     * @throws IOException Should never happen.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testList() throws IOException {
        final StringBuilder buffer = new StringBuilder(4096);
        Errors.getResources(Locale.ENGLISH).list(buffer);
        final String text = buffer.toString();
        final int key     = text.indexOf("NullArgument_1");
        final int value   = text.indexOf("Argument ‘{0}’ shall not be null.");
        assertTrue(key   > 0);
        assertTrue(value > key);
    }

    /**
     * Tests the {@link IndexedResourceBundle#getKeys()} method.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testGetKeys() {
        final Enumeration<String> e=Errors.getResources(Locale.ENGLISH).getKeys();
        int count = 0;
        boolean foundNullArgument_1 = false;
        while (e.hasMoreElements()) {
            final String key = e.nextElement();
            if (key.equals("NullArgument_1")) {
                foundNullArgument_1 = true;
            }
            count++;
        }
        assertTrue("foundNullArgument_1:", foundNullArgument_1);
        assertTrue("count > 5", count > 5);
    }

    /**
     * Tests the {@link IndexedResourceBundle#getString(int)} method on different locales.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testGetString() {
        final Errors english = Errors.getResources(Locale.ENGLISH);
        final Errors french  = Errors.getResources(Locale.FRENCH);

        assertEquals("Argument ‘{0}’ shall not be null.",     english.getString(Errors.Keys.NullArgument_1));
        assertEquals("L’argument ‘{0}’ ne doit pas être nul.", french.getString(Errors.Keys.NullArgument_1));
    }

    /**
     * Tests the {@link IndexedResourceBundle#getString(String)} method on different locales.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testGetStringByName() {
        final Errors english = Errors.getResources(Locale.ENGLISH);
        final Errors french  = Errors.getResources(Locale.FRENCH);

        assertEquals("Argument ‘{0}’ shall not be null.",     english.getString("NullArgument_1"));
        assertEquals("L’argument ‘{0}’ ne doit pas être nul.", french.getString("NullArgument_1"));
    }

    /**
     * Tests the {@link IndexedResourceBundle#getString(int, Object)} method on different locales.
     */
    @Test
    @DependsOnMethod("testGetString")
    public void testGetStringWithParameter() {
        assertEquals("Argument ‘CRS’ shall not be null.",
                Errors.getResources(Locale.ENGLISH).getString(Errors.Keys.NullArgument_1, "CRS"));
        assertEquals("L’argument ‘CRS’ ne doit pas être nul.",
                Errors.getResources(Locale.FRENCH).getString(Errors.Keys.NullArgument_1, "CRS"));
    }

    /**
     * Tests the formatting of an international string.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testFormatInternational() {
        InternationalString i18n = Errors.formatInternational(Errors.Keys.NullArgument_1);
        assertEquals("Argument ‘{0}’ shall not be null.",      i18n.toString(Locale.ENGLISH));
        assertEquals("L’argument ‘{0}’ ne doit pas être nul.", i18n.toString(Locale.FRENCH));

        i18n = Errors.formatInternational(Errors.Keys.NullArgument_1, "CRS");
        assertEquals("Argument ‘CRS’ shall not be null.",      i18n.toString(Locale.ENGLISH));
        assertEquals("L’argument ‘CRS’ ne doit pas être nul.", i18n.toString(Locale.FRENCH));
    }

    /**
     * Tests the {@link IndexedResourceBundle#getLogRecord(Level, int, Object)} method.
     */
    @Test
    @DependsOnMethod("testGetResources")
    public void testGetLogRecord() {
        final IndexedResourceBundle resources = Errors.getResources(Locale.ENGLISH);
        final LogRecord record = resources.getLogRecord(Level.FINE, Errors.Keys.NullArgument_1, "CRS");
        assertEquals("NullArgument_1", record.getMessage());

        final SimpleFormatter formatter = new SimpleFormatter();
        final String message = formatter.format(record);
        assertTrue(message.contains("Argument ‘CRS’ shall not be null."));
    }
}
