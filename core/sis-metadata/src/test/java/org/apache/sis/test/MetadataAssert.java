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
package org.apache.sis.test;

import java.util.Locale;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.io.wkt.Symbols;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Convention;

// Branch-specific imports
import org.apache.sis.internal.jdk7.JDK7;


/**
 * Assertion methods used by the {@code sis-metadata} module in addition of the ones inherited
 * from other modules and libraries.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
public strictfp class MetadataAssert extends Assert {
    /**
     * The formatter to be used by {@link #assertWktEquals(Object, String)}.
     * This formatter uses the {@code “…”} quotation marks instead of {@code "…"}
     * for easier readability of {@link String} constants in Java code.
     */
    private static final WKTFormat WKT_FORMAT = new WKTFormat(null, null);
    static {
        final Symbols s = new Symbols(Symbols.SQUARE_BRACKETS);
        s.setPairedQuotes("“”", "\"\"");
        WKT_FORMAT.setSymbols(s);
    }

    /**
     * For subclass constructor only.
     */
    protected MetadataAssert() {
    }

    /**
     * Asserts that the English title of the given citation is equals to the expected string.
     *
     * @param message  The message to report in case of test failure.
     * @param expected The expected English title.
     * @param citation The citation to test.
     *
     * @since 0.6
     *
     * @see #assertAnyTitleEquals(String, String, Citation)
     */
    public static void assertTitleEquals(final String message, final String expected, final Citation citation) {
        assertNotNull(message, citation);
        final InternationalString title = citation.getTitle();
        assertNotNull(message, title);
        assertEquals(message, expected, title.toString(Locale.US));
    }

    /**
     * Asserts that the WKT 2 of the given object is equal to the expected one.
     * This method expected the {@code “…”} quotation marks instead of {@code "…"}
     * for easier readability of {@link String} constants in Java code.
     *
     * @param expected The expected text, or {@code null} if {@code object} is expected to be null.
     * @param object The object to format in <cite>Well Known Text</cite> format, or {@code null}.
     */
    public static void assertWktEquals(final String expected, final Object object) {
        assertWktEquals(Convention.WKT2, expected, object);
    }

    /**
     * Asserts that the WKT of the given object according the given convention is equal to the expected one.
     * This method expected the {@code “…”} quotation marks instead of {@code "…"} for easier readability of
     * {@link String} constants in Java code.
     *
     * @param convention The WKT convention to use.
     * @param expected   The expected text, or {@code null} if {@code object} is expected to be null.
     * @param object     The object to format in <cite>Well Known Text</cite> format, or {@code null}.
     */
    public static void assertWktEquals(final Convention convention, final String expected, final Object object) {
        if (expected == null) {
            assertNull(object);
        } else {
            assertNotNull(object);
            final String wkt;
            synchronized (WKT_FORMAT) {
                WKT_FORMAT.setConvention(convention);
                wkt = WKT_FORMAT.format(object);
            }
            assertMultilinesEquals((object instanceof IdentifiedObject) ?
                    ((IdentifiedObject) object).getName().getCode() : object.getClass().getSimpleName(), expected, wkt);
        }
    }

    /**
     * Asserts that the WKT of the given object according the given convention is equal to the given regular expression.
     * This method is like {@link #assertWktEquals(String, Object)}, but the use of regular expression allows some
     * tolerance for example on numerical parameter values that may be subject to a limited form of rounding errors.
     *
     * @param convention The WKT convention to use.
     * @param expected   The expected regular expression, or {@code null} if {@code object} is expected to be null.
     * @param object     The object to format in <cite>Well Known Text</cite> format, or {@code null}.
     *
     * @since 0.6
     */
    public static void assertWktEqualsRegex(final Convention convention, final String expected, final Object object) {
        if (expected == null) {
            assertNull(object);
        } else {
            assertNotNull(object);
            final String wkt;
            synchronized (WKT_FORMAT) {
                WKT_FORMAT.setConvention(convention);
                wkt = WKT_FORMAT.format(object);
            }
            if (!wkt.matches(expected.replace("\n", JDK7.lineSeparator()))) {
                fail("WKT does not match the expected regular expression. The WKT that we got is:\n" + wkt);
            }
        }
    }
}
