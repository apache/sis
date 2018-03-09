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
import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.io.wkt.Symbols;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.LegacyNamespaces;

// Branch-specific imports
import org.opengis.metadata.citation.Responsibility;


/**
 * Assertion methods used by the {@code sis-metadata} module in addition of the ones inherited
 * from other modules and libraries.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.4
 * @module
 */
public strictfp class MetadataAssert extends Assert {
    /**
     * The formatter to be used by {@link #assertWktEquals(String, Object)}.
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
     * @param message   the message to report in case of test failure.
     * @param expected  the expected English title.
     * @param citation  the citation to test.
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
     * Asserts that the given citation has only one responsible party,
     * and its English name is equals to the expected string.
     *
     * @param message   the message to report in case of test failure.
     * @param expected  the expected English responsibly party name.
     * @param citation  the citation to test.
     *
     * @since 0.8
     */
    public static void assertPartyNameEquals(final String message, final String expected, final Citation citation) {
        assertNotNull(message, citation);
        final Responsibility r = TestUtilities.getSingleton(citation.getCitedResponsibleParties());
        final InternationalString name = TestUtilities.getSingleton(r.getParties()).getName();
        assertNotNull(message, name);
        assertEquals(message, expected, name.toString(Locale.US));
    }

    /**
     * Asserts that the WKT 2 of the given object is equal to the expected one.
     * This method expected the {@code “…”} quotation marks instead of {@code "…"}
     * for easier readability of {@link String} constants in Java code.
     *
     * @param expected  the expected text, or {@code null} if {@code object} is expected to be null.
     * @param object    the object to format in <cite>Well Known Text</cite> format, or {@code null}.
     */
    public static void assertWktEquals(final String expected, final Object object) {
        assertWktEquals(Convention.WKT2, expected, object);
    }

    /**
     * Asserts that the WKT of the given object according the given convention is equal to the expected one.
     * This method expected the {@code “…”} quotation marks instead of {@code "…"} for easier readability of
     * {@link String} constants in Java code.
     *
     * @param convention  the WKT convention to use.
     * @param expected    the expected text, or {@code null} if {@code object} is expected to be null.
     * @param object      the object to format in <cite>Well Known Text</cite> format, or {@code null}.
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
     * @param convention  the WKT convention to use.
     * @param expected    the expected regular expression, or {@code null} if {@code object} is expected to be null.
     * @param object      the object to format in <cite>Well Known Text</cite> format, or {@code null}.
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
            if (!wkt.matches(expected.replace("\n", System.lineSeparator()))) {
                fail("WKT does not match the expected regular expression. The WKT that we got is:\n" + wkt);
            }
        }
    }

    /**
     * Parses two XML trees as DOM documents, and compares the nodes.
     * The inputs given to this method can be any of the following types:
     *
     * <ul>
     *   <li>{@link org.w3c.dom.Node}: used directly without further processing.</li>
     *   <li>{@link java.io.File}, {@link java.net.URL} or {@link java.net.URI}: the
     *       stream is opened and parsed as a XML document.</li>
     *   <li>{@link String}: The string content is parsed directly as a XML document.</li>
     * </ul>
     *
     * The comparison will ignore comments and the optional attributes given in arguments.
     *
     * <div class="section">Ignored attributes substitution</div>
     * For convenience, this method replaces some well known prefixes in the {@code ignoredAttributes}
     * array by their full namespace URLs. For example this method replaces{@code "xsi:schemaLocation"}
     * by {@code "http://www.w3.org/2001/XMLSchema-instance:schemaLocation"}.
     * If such substitution is not desired, consider using {@link XMLComparator} directly instead.
     *
     * <p>The current substitution map is as below (may be expanded in any future SIS version):</p>
     *
     * <table class="sis">
     *   <caption>Predefined prefix mapping</caption>
     *   <tr><th>Prefix</th> <th>URL</th></tr>
     *   <tr><td>xmlns</td>  <td>{@code "http://www.w3.org/2000/xmlns"}</td></tr>
     *   <tr><td>xlink</td>  <td>{@value Namespaces#XLINK}</td></tr>
     *   <tr><td>xsi</td>    <td>{@value Namespaces#XSI}</td></tr>
     *   <tr><td>gml</td>    <td>{@value Namespaces#GML}</td></tr>
     *   <tr><td>gco</td>    <td>{@value Namespaces#GCO}</td></tr>
     *   <tr><td>gmd</td>    <td>{@value LegacyNamespaces#GMD}</td></tr>
     *   <tr><td>gmx</td>    <td>{@value LegacyNamespaces#GMX}</td></tr>
     *   <tr><td>gmi</td>    <td>{@value LegacyNamespaces#GMI}</td></tr>
     * </table>
     *
     * <p>For example in order to ignore the namespace, type and schema location declaration,
     * the following strings can be given to the {@code ignoredAttributes} argument:</p>
     *
     * {@preformat text
     *   "xmlns:*", "xsi:schemaLocation", "xsi:type"
     * }
     *
     * @param  expected           the expected XML document.
     * @param  actual             the XML document to compare.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     *
     * @see XMLComparator
     */
    public static void assertXmlEquals(final Object expected, final Object actual, final String... ignoredAttributes) {
        assertXmlEquals(expected, actual, TestCase.STRICT, null, ignoredAttributes);
    }

    /**
     * Parses two XML trees as DOM documents, and compares the nodes with the given tolerance
     * threshold for numerical values. The inputs given to this method can be any of the types
     * documented {@linkplain #assertXmlEquals(Object, Object, String[]) above}. This method
     * will ignore comments and the optional attributes given in arguments as documented in the
     * above method.
     *
     * @param  expected           the expected XML document.
     * @param  actual             the XML document to compare.
     * @param  tolerance          the tolerance threshold for comparison of numerical values.
     * @param  ignoredNodes       the fully-qualified names of the nodes to ignore, or {@code null} if none.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     *
     * @see XMLComparator
     */
    public static void assertXmlEquals(final Object expected, final Object actual,
            final double tolerance, final String[] ignoredNodes, final String[] ignoredAttributes)
    {
        final XMLComparator comparator;
        try {
            comparator = new XMLComparator(expected, actual);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            // We don't throw directly those exceptions since failing to parse the XML file can
            // be considered as part of test failures and the JUnit exception for such failures
            // is AssertionError. Having no checked exception in "assert" methods allow us to
            // declare the checked exceptions only for the library code being tested.
            throw new AssertionError(e);
        }
        comparator.tolerance = tolerance;
        comparator.ignoreComments = true;
        if (ignoredNodes != null) {
            for (final String node : ignoredNodes) {
                comparator.ignoredNodes.add(XMLComparator.substitutePrefix(node));
            }
        }
        if (ignoredAttributes != null) {
            for (final String attribute : ignoredAttributes) {
                comparator.ignoredAttributes.add(XMLComparator.substitutePrefix(attribute));
            }
        }
        comparator.compare();
    }
}
