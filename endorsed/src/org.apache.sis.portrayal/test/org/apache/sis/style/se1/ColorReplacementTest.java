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

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.awt.Color;
import java.util.List;
import org.apache.sis.style.se1.function.Recode.MapItem;
import org.apache.sis.style.se1.function.Recode;


/**
 * Tests for {@link ColorReplacement}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class ColorReplacementTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public ColorReplacementTest() {
    }

    /**
     * Test of {@code Function} property.
     */
    @Test
    public void testFunction() {
        final var cdt = factory.createColorReplacement();

        // Check default
        assertEmpty(cdt.getFunction());

        // Check get/set
        final var function = new Recode<>(null, List.of(new MapItem<>(literal(Color.RED), literal(Color.BLUE))), null);
        cdt.setFunction(function);
        assertSame(function, cdt.getFunction().orElseThrow());

        // Equality and copy.
        assertEquals(cdt, new ColorReplacement<>(cdt));
        assertEquals(cdt, cdt.clone());
        cdt.setFunction(null);
        assertEmpty(cdt.getFunction());
    }
}
