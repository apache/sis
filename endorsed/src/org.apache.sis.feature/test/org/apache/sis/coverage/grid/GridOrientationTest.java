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
package org.apache.sis.coverage.grid;

import org.apache.sis.referencing.cs.AxesConvention;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link GridOrientation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GridOrientationTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public GridOrientationTest() {
    }

    /**
     * Tests {@link GridOrientation#useVariantOfCRS(AxesConvention)}. This test may fail if new enumeration values
     * are added in {@link AxesConvention} without updating {@link GridOrientation#useVariantOfCRS(AxesConvention)}
     * code accordingly. The main purpose of this test is to warn the developer if such situation happens.
     */
    @Test
    public void testUseVariantOfCRS() {
        final GridOrientation test = GridOrientation.DISPLAY;
        for (final AxesConvention c : AxesConvention.values()) {
            switch (c) {
                case POSITIVE_RANGE:   // Fallthrough
                case RIGHT_HANDED:     assertNotSame(test, test.useVariantOfCRS(c)); break;
                case DISPLAY_ORIENTED: assertSame   (test, test.useVariantOfCRS(c)); break;
                default: {
                    var e = assertThrows(IllegalArgumentException.class,
                            () -> test.useVariantOfCRS(c),
                            () -> "Should not accept " + c);
                    assertMessageContains(e, c.toString());
                }
            }
        }
    }

    /**
     * Tests {@link GridOrientation#toString()}.
     */
    @Test
    public void testToString() {
        assertEquals("GridOrientation[]",
                GridOrientation.HOMOTHETY.toString());
        assertEquals("GridOrientation[flip={1}]",
                GridOrientation.REFLECTION_Y.toString());
        assertEquals("GridOrientation[flip={1}, crs=DISPLAY_ORIENTED]",
                GridOrientation.DISPLAY.toString());
        assertEquals("GridOrientation[flip={1}, crs=DISPLAY_ORIENTED with grid axis reordering]",
                GridOrientation.DISPLAY.canReorderGridAxis(true).toString());
    }
}
