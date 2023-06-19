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
package org.apache.sis.internal.style;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Tests for {@link PointPlacement}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class PointPlacementTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public PointPlacementTest() {
    }

    /**
     * Test of {@code AnchorPoint} property.
     */
    @Test
    public void testAnchorPoint() {
        PointPlacement cdt = new PointPlacement();

        // Check default
        AnchorPoint value = cdt.getAnchorPoint();
        assertLiteralEquals(0.5, value.getAnchorPointX());
        assertLiteralEquals(0.5, value.getAnchorPointY());

        // Check get/set
        value = new AnchorPoint(3, 1);
        cdt.setAnchorPoint(value);
        assertEquals(value, cdt.getAnchorPoint());
    }

    /**
     * Test of {@code Displacement} property.
     */
    @Test
    public void testDisplacement() {
        PointPlacement cdt = new PointPlacement();

        // Check default
        Displacement value = cdt.getDisplacement();
        assertLiteralEquals(0.0, value.getDisplacementX());
        assertLiteralEquals(0.0, value.getDisplacementY());

        // Check get/set
        value = new Displacement(1, 2);
        cdt.setDisplacement(value);
        assertEquals(value, cdt.getDisplacement());
    }

    /**
     * Test of {@code Rotation} property.
     */
    @Test
    public void testRotation() {
        PointPlacement cdt = new PointPlacement();

        // Check default
        assertLiteralEquals(0.0, cdt.getRotation());

        // Check get/set
        cdt.setRotation(FF.literal(180));
        assertLiteralEquals(180, cdt.getRotation());
    }
}
