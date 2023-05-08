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
package org.apache.sis.geometry;

import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;


/**
 * Tests the static methods provided in {@link AbstractDirectPosition}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.3
 */
public final class AbstractDirectPositionTest extends TestCase {
    /**
     * Tests {@link AbstractDirectPosition#parse(CharSequence)}.
     */
    @Test
    public void testParse() {
        assertArrayEquals(new double[] {6, 10, 2}, AbstractDirectPosition.parse("POINT(6 10 2)"),       STRICT);
        assertArrayEquals(new double[] {3, 14, 2}, AbstractDirectPosition.parse("POINT M [ 3 14 2 ] "), STRICT);
        assertArrayEquals(new double[] {2, 10, 8}, AbstractDirectPosition.parse("POINT Z 2 10 8"),      STRICT);
        assertArrayEquals(new double[] {},         AbstractDirectPosition.parse("POINT()"),             STRICT);
        assertArrayEquals(new double[] {},         AbstractDirectPosition.parse("POINT ( ) "),          STRICT);
    }

    /**
     * Tests {@link AbstractDirectPosition#parse(CharSequence)} with invalid input strings.
     */
    @Test
    public void testParsingFailures() {
        try {
            AbstractDirectPosition.parse("POINT(6 10 2");
            fail("Parsing should fails because of missing parenthesis.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message, message.contains("POINT(6 10 2"));
            assertTrue(message, message.contains("‘)’"));
        }
        try {
            AbstractDirectPosition.parse("POINT 6 10 2)");
            fail("Parsing should fails because of missing parenthesis.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
        }
        try {
            AbstractDirectPosition.parse("POINT(6 10 2) x");
            fail("Parsing should fails because of extra characters.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message, message.contains("POINT(6 10 2) x"));
            assertTrue(message, message.contains("“x”") ||                  // English locale
                                message.contains("« x »"));                 // French locale
        }
    }
}
