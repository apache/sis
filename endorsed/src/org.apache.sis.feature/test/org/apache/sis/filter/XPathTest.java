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
package org.apache.sis.filter;

import org.apache.sis.filter.internal.shared.XPath;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link XPath}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class XPathTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public XPathTest() {
    }

    /**
     * Splits a XPath and verifies the result.
     *
     * @param xpath       the XPath to parse.
     * @param isAbsolute  expected value if {@link XPath#isAbsolute}.
     * @param path        expected value if {@link XPath#path}. Can be null.
     * @param tip         expected value if {@link XPath#tip}.
     */
    private static void split(final String xpath, final boolean isAbsolute, final String[] path, final String tip) {
        final var p = new XPath(xpath);
        assertEquals(isAbsolute, p.isAbsolute, "isAbsolute");
        assertArrayEquals(path, (p.path != null) ? p.path.toArray() : null, "path");
        assertEquals(tip, p.tip, "tip");
        assertEquals(xpath.replace(" ", ""), p.toString(), "toString()");
    }

    /**
     * Tests {@link XPath#XPath(String)}.
     */
    @Test
    public void testSplit() {
        split("property", false, null, "property");
        split("/property", true, null, "property");
        split("Feature/property/child",         false, new String[] {"Feature",  "property"}, "child");
        split("/Feature/property",              true,  new String[] {"Feature"}, "property");
        split("  Feature  / property / child ", false, new String[] {"Feature",  "property"}, "child");
        split("  / Feature /  property ",       true,  new String[] {"Feature"}, "property");
    }

    /**
     * Tests with a XPath containing an URL as the property namespace.
     */
    @Test
    public void testQualifiedName() {
        split("Q{http://example.com/foo/bar}property",       false, null,         "http://example.com/foo/bar:property");
        split("/Q{http://example.com/foo/bar}property",      true,  null,         "http://example.com/foo/bar:property");
        split("Q{http://example.com/foo/bar}property/child", false, new String[] {"http://example.com/foo/bar:property"}, "child");
    }

    /**
     * Tests {@link XPath#toString(String)}.
     */
    @Test
    public void testToString() {
        assertEquals("Q{http://example.com/foo/bar}property",
                XPath.toString(null, null, "http://example.com/foo/bar:property"));
        assertEquals("/*/Q{http://example.com/foo/bar}property/child",
                XPath.toString("/*/", new String[] {"http://example.com/foo/bar:property"}, "child"));
    }

    /**
     * Tests {@link XPath#toPropertyName(String)}.
     */
    @Test
    public void testToPropertyName() {
        assertEquals("http://example.com/foo/bar:property",
                XPath.toPropertyName("Q{http://example.com/foo/bar}property"));
    }
}
