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

import org.junit.Test;


/**
 * Tests for {@link GraphicStroke}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class GraphicStrokeTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public GraphicStrokeTest() {
    }

    /**
     * Test of {@code InitialGap} property.
     */
    @Test
    public void testInitialGap() {
        final var cdt = factory.createGraphicStroke();

        // Check default
        assertLiteralEquals(0.0, cdt.getInitialGap());

        // Check get/set
        cdt.setInitialGap(literal(9));
        assertLiteralEquals(9, cdt.getInitialGap());
    }

    /**
     * Test of {@code Gap} property.
     */
    @Test
    public void testGap() {
        final var cdt = factory.createGraphicStroke();

        // Check default
        assertLiteralEquals(0.0, cdt.getGap());

        // Check get/set
        cdt.setGap(literal(6));
        assertLiteralEquals(6, cdt.getGap());
    }
}
