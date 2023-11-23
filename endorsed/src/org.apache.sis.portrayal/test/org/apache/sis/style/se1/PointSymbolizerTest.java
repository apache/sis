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

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;


/**
 * Tests for {@link PointSymbolizer}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class PointSymbolizerTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public PointSymbolizerTest() {
    }

    /**
     * Test of {@code Graphic} property.
     */
    @Test
    public void testGraphic() {
        final var cdt = factory.createPointSymbolizer();

        // Check default
        Graphic<AbstractFeature> value = cdt.getGraphic();
        assertLiteralEquals(1.0, value.getOpacity());

        // Check get/set
        value = factory.createGraphic();
        value.setOpacity(literal(0.8));
        cdt.setGraphic(value);
        assertEquals(value, cdt.getGraphic());
    }
}
