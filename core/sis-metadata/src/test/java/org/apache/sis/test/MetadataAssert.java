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

import java.util.Collection;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.io.wkt.Symbols;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.internal.util.Citations;


/**
 * Assertion methods used by the {@code sis-metadata} module in addition of the ones inherited
 * from other modules and libraries.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.00)
 * @version 0.5
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
     * Asserts that the given identifier has the expected code and the {@code "EPSG"} code space.
     * The authority is expected to have the {@code "OGP"} title or alternate title.
     *
     * @param expected   The expected identifier code.
     * @param identifier The identifier to verify.
     *
     * @since 0.5
     */
    public static void assertEpsgIdentifierEquals(final String expected, final Identifier identifier) {
        assertNotNull(identifier);
        assertEquals("code",      expected, identifier.getCode());
        assertEquals("codeSpace", Citations.EPSG, identifier.getCodeSpace());
        assertEquals("authority", "OGP",  Citations.getIdentifier(identifier.getAuthority()));
    }

    /**
     * Asserts that the given collection contains exactly one identifier with the given
     * {@linkplain Identifier#getCode() code}. The {@linkplain Identifier#getCodeSpace()
     * code space} and authority are ignored.
     *
     * @param expected The expected identifier code (typically {@code "ISO"} or {@code "EPSG"}).
     * @param identifiers The collection to validate. Should be a collection of {@link Identifier}.
     *
     * @since 0.5
     */
    public static void assertContainsIdentifierCode(final String expected, final Collection<?> identifiers) {
        assertNotNull("identifiers", identifiers);
        int count = 0;
        for (final Object id : identifiers) {
            assertInstanceOf("identifier", Identifier.class, id);
            if (((Identifier) id).getCode().equals(expected)) {
                count++;
            }
        }
        assertEquals("Unexpected amount of identifiers.", 1, count);
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
}
