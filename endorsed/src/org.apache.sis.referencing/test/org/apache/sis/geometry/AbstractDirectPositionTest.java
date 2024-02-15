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

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests the static methods provided in {@link AbstractDirectPosition}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class AbstractDirectPositionTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public AbstractDirectPositionTest() {
    }

    /**
     * Tests {@link AbstractDirectPosition#parse(CharSequence)}.
     */
    @Test
    public void testParse() {
        assertArrayEquals(new double[] {6, 10, 2}, AbstractDirectPosition.parse("POINT(6 10 2)"));
        assertArrayEquals(new double[] {3, 14, 2}, AbstractDirectPosition.parse("POINT M [ 3 14 2 ] "));
        assertArrayEquals(new double[] {2, 10, 8}, AbstractDirectPosition.parse("POINT Z 2 10 8"));
        assertArrayEquals(new double[] {},         AbstractDirectPosition.parse("POINT()"));
        assertArrayEquals(new double[] {},         AbstractDirectPosition.parse("POINT ( ) "));
    }

    /**
     * Tests {@link AbstractDirectPosition#parse(CharSequence)} with invalid input strings.
     */
    @Test
    public void testParsingFailures() {
        IllegalArgumentException e;
        e = assertThrows(IllegalArgumentException.class,
                () -> AbstractDirectPosition.parse("POINT(6 10 2"),
                "Parsing should fails because of missing parenthesis.");
        assertMessageContains(e, "POINT(6 10 2", "‘)’");

        e = assertThrows(IllegalArgumentException.class,
                () -> AbstractDirectPosition.parse("POINT 6 10 2)"),
                "Parsing should fails because of missing parenthesis.");
        assertMessageContains(e);

        e = assertThrows(IllegalArgumentException.class,
                () -> AbstractDirectPosition.parse("POINT(6 10 2) x"),
                "Parsing should fails because of extra characters.");

        assertMessageContains(e, "POINT(6 10 2) x");
    }
}
