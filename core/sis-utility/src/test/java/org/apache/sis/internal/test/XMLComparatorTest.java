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
package org.apache.sis.internal.test;

import org.apache.sis.test.TestCase;
import org.apache.sis.test.XMLComparator;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link XMLComparator} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final strictfp class XMLComparatorTest extends TestCase {
    /**
     * Tests the {@link XMLComparator#ignoredAttributes} and {@link XMLComparator#ignoredNodes} sets.
     *
     * @throws Exception Shall never happen.
     */
    @Test
    public void testIgnore() throws Exception {
        final XMLComparator cmp = new XMLComparator(
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
     * @throws Exception Shall never happen.
     */
    @Test
    public void testNamespaceAware() throws Exception {
        XMLComparator cmp = new XMLComparator(
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
        cmp = new XMLComparator(
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
     * Ensures that the call to {@link XMLComparator#compare()} fails. This method is
     * invoked in order to test that the comparator rightly detected an error that we
     * were expected to detect.
     *
     * @param message The message for JUnit if the comparison does not fail.
     * @param cmp The comparator on which to invoke {@link XMLComparator#compare()}.
     */
    private static void assertFail(final String message, final XMLComparator cmp) {
        try {
            cmp.compare();
        } catch (AssertionError e) {
            return;
        }
        fail(message);
    }
}
