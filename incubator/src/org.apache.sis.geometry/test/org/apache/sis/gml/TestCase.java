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
package org.apache.sis.gml;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.apache.sis.xml.Namespaces;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 */
@Execution(ExecutionMode.SAME_THREAD)
public abstract class TestCase {
    /**
     * Parses two <abbr>XML</abbr> trees as <abbr>DOM</abbr> documents and compares the nodes.
     * The inputs given to this method can be any of the following types:
     *
     * <ul>
     *   <li>{@link org.w3c.dom.Node}: used directly without further processing.</li>
     *   <li>{@link java.io.InputStream}: the stream is parsed as an XML document, then closed.</li>
     *   <li>{@link java.nio.file.Path}, {@link java.io.File}, {@link java.net.URL} or {@link java.net.URI}:
     *       the stream is opened, parsed as an XML document, then closed.</li>
     *   <li>{@link String}: The string content is parsed directly as a XML document.</li>
     * </ul>
     *
     * The comparison will ignore comments and the optional attributes given in arguments.
     *
     * <h4>Ignored attributes substitution</h4>
     * For convenience, this method replaces some well known prefixes in the {@code ignoredAttributes}
     * array by their full namespace URLs. For example, this method replaces{@code "xsi:schemaLocation"}
     * by {@code "http://www.w3.org/2001/XMLSchema-instance:schemaLocation"}.
     * If such substitution is not desired, consider using {@link DocumentComparator} directly instead.
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
     * <p>For example, in order to ignore the namespace, type and schema location declaration,
     * the following strings can be given to the {@code ignoredAttributes} argument:</p>
     *
     * {@snippet :
     *   "xmlns:*", "xsi:schemaLocation", "xsi:type"
     *   }
     *
     * @param  expected           the expected XML document.
     * @param  actual             the XML document to compare.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     *
     * @see DocumentComparator
     */
    public static void assertXmlEquals(final Object expected, final Object actual, final String... ignoredAttributes) {
        assertXmlEquals(expected, actual, 0, null, ignoredAttributes);
    }

    /**
     * Parses two <abbr>XML</abbr> trees as <abbr>DOM</abbr> documents, and compares the nodes with the
     * given tolerance threshold for numerical values. The inputs given to this method can be any of the
     * types documented {@linkplain #assertXmlEquals(Object, Object, String[]) above}. This method will
     * ignore comments and the optional attributes given in arguments as documented in the above method.
     *
     * @param  expected           the expected XML document.
     * @param  actual             the XML document to compare.
     * @param  tolerance          the tolerance threshold for comparison of numerical values.
     * @param  ignoredNodes       the fully-qualified names of the nodes to ignore, or {@code null} if none.
     * @param  ignoredAttributes  the fully-qualified names of attributes to ignore
     *                            (typically {@code "xmlns:*"} and {@code "xsi:schemaLocation"}).
     *
     * @see DocumentComparator
     */
    public static void assertXmlEquals(final Object expected, final Object actual,
            final double tolerance, final String[] ignoredNodes, final String[] ignoredAttributes)
    {
        final DocumentComparator comparator;
        try {
            comparator = new DocumentComparator(expected, actual);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            /*
             * We don't throw directly those exceptions since failing to parse the XML file can
             * be considered as part of test failures and the JUnit exception for such failures
             * is AssertionError. Having no checked exception in "assert" methods allow us to
             * declare the checked exceptions only for the library code being tested.
             */
            throw new AssertionError(e);
        }
        comparator.tolerance = tolerance;
        comparator.ignoreComments = true;
        if (ignoredNodes != null) {
            for (final String node : ignoredNodes) {
                comparator.ignoredNodes.add(DocumentComparator.substitutePrefix(node));
            }
        }
        if (ignoredAttributes != null) {
            for (final String attribute : ignoredAttributes) {
                comparator.ignoredAttributes.add(DocumentComparator.substitutePrefix(attribute));
            }
        }
        comparator.compare();
    }

}
