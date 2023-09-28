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
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests for {@link ShadedRelief}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class ShadedReliefTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public ShadedReliefTest() {
    }

    /**
     * Test of {@code isBrightnessOnly} property.
     */
    @Test
    public void testIsBrightnessOnly() {
        final var cdt = factory.createShadedRelief();

        // Check default
        assertLiteralEquals(Boolean.FALSE, cdt.isBrightnessOnly());

        // Check get/set
        cdt.setBrightnessOnly(literal(true));
        assertLiteralEquals(Boolean.TRUE, cdt.isBrightnessOnly());
    }

    /**
     * Test of {@code ReliefFactor} property.
     */
    @Test
    public void testReliefFactor() {
        final var cdt = factory.createShadedRelief();

        // Check default
        assertNotNull(cdt.getReliefFactor());

        // Check get/set
        cdt.setReliefFactor(literal(0.1));
        assertLiteralEquals(0.1, cdt.getReliefFactor());
    }
}
