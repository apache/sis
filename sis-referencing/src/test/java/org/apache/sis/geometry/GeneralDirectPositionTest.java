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

import java.util.Arrays;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link GeneralDirectPosition} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
public final strictfp class GeneralDirectPositionTest extends TestCase {
    /**
     * Tests the {@link GeneralDirectPosition#toString()} method.
     */
    @Test
    public void testWktFormatting() {
        assertEquals("POINT(6 10 2)", new GeneralDirectPosition(6, 10, 2).toString());
    }

    /**
     * Tests the {@link GeneralDirectPosition#GeneralDirectPosition(String)} constructor.
     */
    @Test
    public void testWktParsing() {
        assertEquals("POINT(6 10 2)", new GeneralDirectPosition("POINT(6 10 2)").toString());
        assertEquals("POINT(3 14 2)", new GeneralDirectPosition("POINT M [ 3 14 2 ] ").toString());
        assertEquals("POINT(2 10 8)", new GeneralDirectPosition("POINT Z 2 10 8").toString());
        assertEquals("POINT()",       new GeneralDirectPosition("POINT()").toString());
        assertEquals("POINT()",       new GeneralDirectPosition("POINT ( ) ").toString());
    }

    /**
     * Tests the {@link GeneralDirectPosition#GeneralDirectPosition(String)} constructor
     * with invalid input strings.
     */
    @Test
    public void testWktParsingFailures() {
        try {
            new GeneralDirectPosition("POINT(6 10 2");
            fail("Parsing should fails because of missing parenthesis.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message.contains("POINT(6 10 2"));
            assertTrue(message.contains("‘)’"));
        }
        try {
            new GeneralDirectPosition("POINT 6 10 2)");
            fail("Parsing should fails because of missing parenthesis.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
        }
        try {
            new GeneralDirectPosition("POINT(6 10 2) x");
            fail("Parsing should fails because of extra characters.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message.contains("POINT(6 10 2) x"));
            assertTrue(message.contains("“x”"));
        }
    }

    /**
     * Tests {@link GeneralDirectPosition#clone()}.
     */
    @Test
    public void testClone() {
        final GeneralDirectPosition p1 = new GeneralDirectPosition(10, 20, 30);
        final GeneralDirectPosition p2 = p1.clone();
        assertEquals ("Expected the same CRS and ordinates.", p1, p2);
        assertTrue   ("Expected the same ordinates.", Arrays.equals(p1.ordinates, p2.ordinates));
        assertNotSame("the ordinates array should have been cloned.", p1.ordinates, p2.ordinates);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialize() {
        final GeneralDirectPosition p = new GeneralDirectPosition(12, -20, 4, 9);
        assertNotSame(p, assertSerializedEquals(p));
    }
}
