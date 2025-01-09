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
import org.opengis.util.InternationalString;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the {@link DefaultInternationalString} implementation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class DefaultInternationalStringTest extends TestCase {
    /** {@value} */ static final String MESSAGE       = "This is an unlocalized message.";
    /** {@value} */ static final String MESSAGE_en    = "This is a localized message.";
    /** {@value} */ static final String MESSAGE_fr    = "Voici un message.";
    /** {@value} */ static final String MESSAGE_fr_CA = "Swing la baguette dans le fond de la boîte à bois!";

    /**
     * Creates a new test case.
     */
    public DefaultInternationalStringTest() {
    }

    /**
     * Tests an instance having only the English locale.
     */
    @Test
    public void testEnglishOnly() {
        final var toTest = new DefaultInternationalString();
        toTest.add(Locale.ENGLISH, MESSAGE);
        assertSame(MESSAGE, toTest.toString());
        assertSame(MESSAGE, toTest.toString(null));
        assertSame(MESSAGE, toTest.toString(Locale.ROOT));
        validate(toTest);
    }

    /**
     * Tests an instance having English and French sentences.
     */
    @Test
    public void testEnglishAndFrench() {
        final var toTest = new DefaultInternationalString(MESSAGE);
        assertSame(MESSAGE, toTest.toString());
        toTest.add(Locale.ENGLISH,       MESSAGE_en);
        toTest.add(Locale.FRENCH,        MESSAGE_fr);     assertLocalized(toTest, MESSAGE_fr);
        toTest.add(Locale.CANADA_FRENCH, MESSAGE_fr_CA);  assertLocalized(toTest, MESSAGE_fr_CA);
        validate(toTest);
    }

    /**
     * Tests the {@link DefaultInternationalString} serialization.
     */
    @Test
    public void testSerialization() {
        final var toTest = new DefaultInternationalString(MESSAGE);
        toTest.add(Locale.ENGLISH,       MESSAGE_en);
        toTest.add(Locale.FRENCH,        MESSAGE_fr);
        toTest.add(Locale.CANADA_FRENCH, MESSAGE_fr_CA);
        assertLocalized(assertSerializedEquals(toTest), MESSAGE_fr_CA);
    }

    /**
     * Ensures that the given international string contains the expected localized texts.
     *
     * @param quebecker  either {@link #MESSAGE_fr} or {@link #MESSAGE_fr_CA},
     *                   depending on the localization details being tested.
     */
    private static void assertLocalized(final InternationalString toTest, final String quebecker) {
        assertEquals (MESSAGE,    toTest.toString(null));
        assertEquals (MESSAGE,    toTest.toString(Locale.ROOT));
        assertEquals (MESSAGE_en, toTest.toString(Locale.ENGLISH));
        assertEquals (MESSAGE_fr, toTest.toString(Locale.FRENCH));
        assertEquals (quebecker,  toTest.toString(Locale.CANADA_FRENCH));
        assertNotNull(            toTest.toString(Locale.JAPANESE));
    }

    /**
     * Tests the {@link java.util.Formattable} interface implementation.
     */
    @Test
    public void testFormattable() {
        final var toTest = new DefaultInternationalString(MESSAGE);
        toTest.add(Locale.ENGLISH,       MESSAGE_en);
        toTest.add(Locale.FRENCH,        MESSAGE_fr);
        toTest.add(Locale.CANADA_FRENCH, MESSAGE_fr_CA);

        assertEquals(MESSAGE,    String.format(Locale.ROOT,    "%s", toTest));
        assertEquals(MESSAGE_en, String.format(Locale.ENGLISH, "%s", toTest));
        assertEquals(MESSAGE_fr, String.format(Locale.FRENCH,  "%s", toTest));

        assertEquals("  Thi…", String.format(Locale.ROOT,    "%6.4s",  toTest));
        assertEquals("  Thi…", String.format(Locale.ENGLISH, "%6.4s",  toTest));
        assertEquals(" Voic…", String.format(Locale.FRENCH,  "%6.5s",  toTest));
        assertEquals("THIS… ", String.format(Locale.ROOT,    "%-6.5S", toTest));
        assertEquals("THIS… ", String.format(Locale.ENGLISH, "%-6.5S", toTest));
        assertEquals("VOIC… ", String.format(Locale.FRENCH,  "%-6.5S", toTest));
    }
}
