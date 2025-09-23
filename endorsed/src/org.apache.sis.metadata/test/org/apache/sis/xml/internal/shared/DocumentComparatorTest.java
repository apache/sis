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
package org.apache.sis.xml.internal.shared;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.xml.test.DocumentComparator;


/**
 * Tests the {@link DocumentComparator} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DocumentComparatorTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DocumentComparatorTest() {
    }

    /**
     * Tests the {@link DocumentComparator#ignoredAttributes} and {@link DocumentComparator#ignoredNodes} sets.
     *
     * @throws Exception if an error occurred while reading the XML.
     */
    @Test
    public void testIgnore() throws Exception {
        final DocumentComparator cmp = new DocumentComparator(
            "<body>\n" +
            "  <form id=\"MyForm\">\n" +
            "    <table cellpading=\"1\">\n" +
            "      <tr><td>foo</td></tr>\n" +
            "    </table>\n" +
            "  </form>\n" +
            "</body>",

            "<body>\n" +
            "  <form id=\"MyForm\">\n" +
            "    <table cellpading=\"2\">\n" +
            "      <tr><td>foo</td></tr>\n" +
            "    </table>\n" +
            "  </form>\n" +
            "</body>");

        assertFail("Shall fail because the \"cellpading\" attribute value is different.", cmp);

        // Following comparison should not fail anymore.
        cmp.ignoredAttributes.add("cellpading");
        cmp.compare();

        cmp.ignoredAttributes.clear();
        cmp.ignoredAttributes.add("bgcolor");
        assertFail("The \"cellpading\" attribute should not be ignored anymore.", cmp);

        // Ignore the table node, which contains the faulty attribute.
        cmp.ignoredNodes.add("table");
        cmp.compare();

        // Ignore the form node and all its children.
        cmp.ignoredNodes.clear();
        cmp.ignoredNodes.add("form");
        cmp.compare();
    }

    /**
     * Verifies that comparisons of XML documents compare the namespace URIs, not the prefixes.
     *
     * @see javax.xml.parsers.DocumentBuilderFactory#setNamespaceAware(boolean)
     *
     * @throws Exception if an error occurred while reading the XML.
     */
    @Test
    public void testNamespaceAware() throws Exception {
        DocumentComparator cmp = new DocumentComparator(
            "<ns1:body xmlns:ns1=\"http://myns1\" xmlns:ns2=\"http://myns2\">\n" +
            "  <ns1:table ns2:cellpading=\"1\"/>\n" +
            "</ns1:body>",

            "<ns4:body xmlns:ns4=\"http://myns1\" xmlns:ns3=\"http://myns2\">\n" +
            "  <ns4:table ns3:cellpading=\"1\"/>\n" +
            "</ns4:body>");

        // Following comparison should not fail anymore.
        cmp.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        cmp.compare();
        /*
         * Opposite case: same prefix, but different URL.
         * The XML comparison is expected to fail.
         */
        cmp = new DocumentComparator(
            "<ns1:body xmlns:ns1=\"http://myns1\" xmlns:ns2=\"http://myns2\">\n" +
            "  <ns1:table ns2:cellpading=\"1\"/>\n" +
            "</ns1:body>",

            "<ns1:body xmlns:ns1=\"http://myns1\" xmlns:ns2=\"http://myns3\">\n" +
            "  <ns1:table ns2:cellpading=\"1\"/>\n" +
            "</ns1:body>");

        // Following comparison should not fail anymore.
        cmp.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        assertFail("Shall fail because the \"cellpading\" attribute has a different namespace.", cmp);
    }

    /**
     * Ensures that the call to {@link DocumentComparator#compare()} fails. This method is
     * invoked in order to test that the comparator rightly detected an error that we
     * were expected to detect.
     *
     * @param  message  the message for JUnit if the comparison does not fail.
     * @param  cmp      the comparator on which to invoke {@link DocumentComparator#compare()}.
     */
    private static void assertFail(final String message, final DocumentComparator cmp) {
        try {
            cmp.compare();
        } catch (AssertionError e) {
            return;
        }
        fail(message);
    }
}
