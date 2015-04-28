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
package org.apache.sis.xml;

import java.util.Locale;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.StandardCharsets;


/**
 * Tests the {@link ValueConverter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 */
@DependsOn(LegacyCodesTest.class)
public final strictfp class ValueConverterTest extends TestCase {
    /**
     * Tests {@link ValueConverter#toLanguageCode(MarshalContext, Locale)}.
     * The results should be ISO 639-2 codes (3 letters language codes).
     */
    @Test
    public void testToLanguageCode() {
        assertEquals("eng", ValueConverter.DEFAULT.toLanguageCode(null, Locale.US));
        assertEquals("eng", ValueConverter.DEFAULT.toLanguageCode(null, Locale.UK));
        assertEquals("eng", ValueConverter.DEFAULT.toLanguageCode(null, Locale.ENGLISH));
        assertEquals("fra", ValueConverter.DEFAULT.toLanguageCode(null, Locale.FRANCE));
        assertEquals("fra", ValueConverter.DEFAULT.toLanguageCode(null, Locale.FRENCH));
        assertEquals("jpn", ValueConverter.DEFAULT.toLanguageCode(null, Locale.JAPAN));
        assertEquals("jpn", ValueConverter.DEFAULT.toLanguageCode(null, Locale.JAPANESE));
    }

    /**
     * Tests {@link ValueConverter#toCountryCode(MarshalContext, Locale)}.
     * The results should be ISO 3166 codes (2 letters country codes).
     *
     * Note that we do not expect the 3 letters code, because the {@code schemas.opengis.net}
     * practice seems to be to use 2 letter codes.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-153">SIS-153</a>
     */
    @Test
    public void testToCountryCode() {
        assertEquals("US", ValueConverter.DEFAULT.toCountryCode(null, Locale.US));
        assertEquals("GB", ValueConverter.DEFAULT.toCountryCode(null, Locale.UK));
        assertNull  (      ValueConverter.DEFAULT.toCountryCode(null, Locale.ENGLISH));
        assertEquals("FR", ValueConverter.DEFAULT.toCountryCode(null, Locale.FRANCE));
        assertNull  (      ValueConverter.DEFAULT.toCountryCode(null, Locale.FRENCH));
        assertEquals("JP", ValueConverter.DEFAULT.toCountryCode(null, Locale.JAPAN));
        assertNull  (      ValueConverter.DEFAULT.toCountryCode(null, Locale.JAPANESE));
    }

    /**
     * Tests {@link ValueConverter#toCharsetCode(MarshalContext, Charset)}.
     */
    @Test
    public void testToCharsetCode() {
        assertEquals("utf8",      ValueConverter.DEFAULT.toCharsetCode(null, StandardCharsets.UTF_8));
        assertEquals("utf16",     ValueConverter.DEFAULT.toCharsetCode(null, StandardCharsets.UTF_16));
        assertEquals("8859part1", ValueConverter.DEFAULT.toCharsetCode(null, StandardCharsets.ISO_8859_1));
    }

    /**
     * Tests {@link ValueConverter#toCharset(MarshalContext, String)}.
     */
    @Test
    public void testToCharset() {
        assertEquals(StandardCharsets.UTF_8,      ValueConverter.DEFAULT.toCharset(null, "utf8"));
        assertEquals(StandardCharsets.UTF_8,      ValueConverter.DEFAULT.toCharset(null, "UTF-8"));
        assertEquals(StandardCharsets.UTF_16,     ValueConverter.DEFAULT.toCharset(null, "utf16"));
        assertEquals(StandardCharsets.ISO_8859_1, ValueConverter.DEFAULT.toCharset(null, "8859part1"));
        assertEquals(StandardCharsets.ISO_8859_1, ValueConverter.DEFAULT.toCharset(null, "ISO-8859-1"));
    }
}
