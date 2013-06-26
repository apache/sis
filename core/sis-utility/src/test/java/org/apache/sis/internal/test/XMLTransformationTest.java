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

import java.util.regex.Matcher;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.XMLTransformation.GML;


/**
 * Tests {@link XMLTransformation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class XMLTransformationTest extends TestCase {
    /**
     * Tests the {@link org.apache.sis.test.XMLTransformation#GML} pattern
     * with a string containing {@code xmlns:gml="…"}.
     */
    @Test
    public void testPrefixGML() {
        final Matcher matcher = GML.findPrefix.matcher("<gml:TimeInstant xmlns:gml=\"" + Namespaces.GML + "\">");
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        assertEquals(":gml", matcher.group(1));
    }

    /**
     * Tests the {@link org.apache.sis.test.XMLTransformation#GML} pattern
     * with a string containing {@code xmlns="…"} (without {@code :gml}).
     */
    @Test
    public void testPrefixMissing() {
        final Matcher matcher = GML.findPrefix.matcher("<TimeInstant xmlns=\"" + Namespaces.GML + "\">");
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        assertNull(matcher.group(1));
    }

    /**
     * Tests the {@link org.apache.sis.test.XMLTransformation#GML} pattern
     * with a string containing {@code xmlns:gmd="…"}. We expect no match.
     */
    @Test
    public void testPrefixMismatch() {
        final Matcher matcher = GML.findPrefix.matcher("<gml:TimeInstant xmlns:gmd=\"" + Namespaces.GMD + "\">");
        assertFalse(matcher.matches());
    }

    /**
     * Tests {@link org.apache.sis.test.XMLTransformation#optionallyRemovePrefix(String, CharSequence)}.
     */
    @Test
    @DependsOnMethod({
        "testPrefixGML",
        "testPrefixMissing",
        "testPrefixMismatch"
    })
    public void testOptionallyRemovePrefix() {
        final String expected =
            "<gml:TimeInstant xmlns:gml=\"" + Namespaces.GML + "\">\n" +
            "  <gml:timePosition>1992-01-01T01:00:00.000+01:00</gml:timePosition>\n" +
            "</gml:TimeInstant>\n";
        assertSame(expected, GML.optionallyRemovePrefix(expected, expected));

        final String actual =
            "<TimeInstant xmlns=\"" + Namespaces.GML + "\">\n" +
            "  <timePosition>1992-01-01T01:00:00.000+01:00</timePosition>\n" +
            "</TimeInstant>\n";
        assertEquals(actual, GML.optionallyRemovePrefix(expected, actual));
    }
}
