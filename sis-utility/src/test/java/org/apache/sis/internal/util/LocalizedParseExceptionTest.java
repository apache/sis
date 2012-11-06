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
package org.apache.sis.internal.util;

import java.util.Locale;
import java.text.ParseException;
import java.text.ParsePosition;
import org.apache.sis.measure.Angle;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.Exceptions;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link LocalizedParseException} class.
 *
 * <p><b>Note:</b> this test case contains a method which was previously part of public API,
 * but has been removed because considered too convolved.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.04)
 * @version 0.3
 * @module
 */
@DependsOn(org.apache.sis.util.resources.IndexedResourceBundleTest.class)
public final strictfp class LocalizedParseExceptionTest extends TestCase {
    /**
     * Creates a {@link ParseException} with a localized message built from the given parsing
     * information. The exception returned by this method contains the error message in two
     * languages:
     *
     * <ul>
     *   <li>{@link ParseException#getMessage()} returns the message in the default locale.</li>
     *   <li>{@link ParseException#getLocalizedMessage()} returns the message in the locale given
     *       in argument to this method.</li>
     * </ul>
     *
     * This locale given to this method is usually the {@link java.text.Format} locale,
     * which is presumed to be the end-user locale.
     *
     * @param  locale The locale for {@link ParseException#getLocalizedMessage()}.
     * @param  type   The type of objects parsed by the {@link java.text.Format}.
     * @param  text   The text that {@code Format} failed to parse.
     * @param  pos    Index of the {@linkplain ParsePosition#getIndex() first parsed character},
     *                together with the {@linkplain ParsePosition#getErrorIndex() error index}.
     * @return The localized exception.
     */
    public static ParseException createParseException(final Locale locale, final Class<?> type,
            final CharSequence text, final ParsePosition pos)
    {
        final int offset = pos.getIndex();
        final int errorOffset = Math.max(offset, pos.getErrorIndex());
        return new LocalizedParseException(locale,
                LocalizedParseException.arguments(type, text, offset, errorOffset), errorOffset);
    }

    /**
     * Tests the {@link LocalizedParseException} constructor. This method also tests
     * {@link Exceptions#getLocalizedMessage(Throwable, Locale)} as a side-effect.
     */
    @Test
    public void testCreateParseException() {
        final ParsePosition pos = new ParsePosition(0);
        pos.setErrorIndex(5);
        final ParseException e = createParseException(
                Locale.CANADA, Angle.class, "Some text to parse", pos);
        String message = e.getLocalizedMessage();
        assertTrue(message, message.contains("Some text to parse"));
        assertTrue(message, message.contains("can not be parsed"));
        assertTrue(message, message.contains("Angle"));

        assertEquals(message, Exceptions.getLocalizedMessage(e, Locale.CANADA));
        message = Exceptions.getLocalizedMessage(e, Locale.FRANCE);
        assertTrue(message, message.contains("Some text to parse"));
        assertTrue(message, message.contains("n’est pas reconnu"));
        assertTrue(message, message.contains("Angle"));
    }

    /**
     * Tests the undocumented special case, for SIS internal usage only.
     * Such special cases many change in any future version without notice.
     */
    @Test
    public void testSpecialCase() {
        final ParsePosition pos = new ParsePosition(0);
        pos.setErrorIndex(5);
        final ParseException e = createParseException(
                Locale.CANADA, TreeTable.Node.class, "Some text to parse", pos);
        String message = e.getLocalizedMessage();
        assertTrue(message, message.contains("Node “text” has no parent."));
    }
}
