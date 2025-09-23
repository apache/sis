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
package org.apache.sis.metadata.sql.internal.shared;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link SQLUtilities} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SQLUtilitiesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public SQLUtilitiesTest() {
    }

    /**
     * Tests {@link SQLUtilities#escape(String, String)}.
     */
    @Test
    public void testEscape() {
        assertEquals("foo",             SQLUtilities.escape("foo",          "\\"));
        assertEquals("foo\\_biz\\%bar", SQLUtilities.escape("foo_biz%bar",  "\\"));
        assertEquals("foo\\\\bar",      SQLUtilities.escape("foo\\bar",     "\\"));
        assertEquals("foo#!#!bar#not",  SQLUtilities.escape("foo#!bar#not", "#!"));
    }

    /**
     * Tests {@link SQLUtilities#toLikePattern(String, int, int, boolean, boolean, StringBuilder)}.
     */
    @Test
    public void testToLikePattern() {
        final var buffer = new StringBuilder(30);
        assertEquals("WGS84",                       toLikePattern(buffer, "WGS84"));
        assertEquals("WGS%84",                      toLikePattern(buffer, "WGS 84"));
        assertEquals("A%text%with%random%symbols%", toLikePattern(buffer, "A text !* with_random:/symbols;+"));
        assertEquals("*%With%non%letter%start",     toLikePattern(buffer, "*_+%=With non-letter  start"));
        assertEquals("\\%Special%case",             toLikePattern(buffer, "%Special_case"));
    }

    /**
     * Helper method for {@link #testToLikePattern()}.
     */
    private static String toLikePattern(final StringBuilder buffer, final String identifier) {
        buffer.setLength(0);
        SQLUtilities.toLikePattern(identifier, 0, identifier.length(), false, false, "\\", buffer);
        return buffer.toString();
    }
}
