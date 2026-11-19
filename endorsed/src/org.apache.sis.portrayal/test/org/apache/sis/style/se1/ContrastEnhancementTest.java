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
package org.apache.sis.style.se1;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link ContrastEnhancement}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class ContrastEnhancementTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public ContrastEnhancementTest() {
    }

    /**
     * Test of {@code Method} property.
     */
    @Test
    public void testMethod() {
        final var cdt = factory.createContrastEnhancement();

        // Check default
        assertEquals(ContrastMethod.NONE, cdt.getMethod());

        // Check get/set
        cdt.setMethod(ContrastMethod.HISTOGRAM);
        assertEquals(ContrastMethod.HISTOGRAM, cdt.getMethod());
    }

    /**
     * Test of {@code GammaValue} property.
     */
    @Test
    public void testGammaValue() {
        final var cdt = factory.createContrastEnhancement();

        // Check default
        assertLiteralEquals(1.0, cdt.getGammaValue());

        // Check get/set
        cdt.setGammaValue(literal(2));
        assertLiteralEquals(2, cdt.getGammaValue());
        assertEquals(ContrastMethod.NONE, cdt.getMethod());
    }

    /**
     * Verifies that a gamma value can be combined with the normalize or histogram method.
     * OGC 05-077r4 and OGC 02-070 allow {@code <GammaValue>} after {@code <Normalize>} or {@code <Histogram>}.
     * The result shall not depend on the order in which the properties are set.
     */
    @Test
    public void testMethodWithGammaValue() {
        var cdt = factory.createContrastEnhancement();
        cdt.setMethod(ContrastMethod.NORMALIZE);
        cdt.setGammaValue(literal(2.5));
        assertEquals(ContrastMethod.NORMALIZE, cdt.getMethod());
        assertLiteralEquals(2.5, cdt.getGammaValue());

        cdt = factory.createContrastEnhancement();
        cdt.setGammaValue(literal(2.5));
        cdt.setMethod(ContrastMethod.HISTOGRAM);
        assertEquals(ContrastMethod.HISTOGRAM, cdt.getMethod());
        assertLiteralEquals(2.5, cdt.getGammaValue());
    }
}
