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


/**
 * Tests for {@link AnchorPoint}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class AnchorPointTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public AnchorPointTest() {
    }

    /**
     * Test of {@code AnchorPointXY} property.
     */
    @Test
    public void testAnchorPointXY() {
        final var cdt = factory.createAnchorPoint();

        // Check defaults
        assertLiteralEquals(0.5, cdt.getAnchorPointX());
        assertLiteralEquals(0.5, cdt.getAnchorPointY());

        // Check get/set
        cdt.setAnchorPointX(literal(8));
        cdt.setAnchorPointY(literal(3));
        assertLiteralEquals(8, cdt.getAnchorPointX());
        assertLiteralEquals(3, cdt.getAnchorPointY());
    }
}
