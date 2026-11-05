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

import java.awt.Color;
import java.util.List;
import org.apache.sis.style.se1.function.Categorize;
import org.apache.sis.style.se1.function.Interpolate;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link ColorMap}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class ColorMapTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public ColorMapTest() {
    }

    /**
     * Test of {@code Function} property.
     */
    @Test
    public void testFunction() {
        final var cdt = factory.createColorMap();

        // Check default
        assertEmpty(cdt.getFunction());

        // Check get/set
        final var function = new Categorize<>(null, List.of(literal(Color.RED)), List.of(), null, null);
        cdt.setFunction(function);
        assertSame(function, cdt.getFunction().orElseThrow());

        // Any expression is accepted.
        cdt.setFunction(literal(Color.BLUE));
        assertLiteralEquals(Color.BLUE, cdt.getFunction().orElseThrow());

        cdt.setFunction(null);
        assertEmpty(cdt.getFunction());
    }

    /**
     * Tests that clones share the function, which is immutable.
     */
    @Test
    public void testClone() {
        final var cdt = factory.createColorMap();
        cdt.setFunction(new Interpolate<>(null, List.of(), null, null, null));
        final var clone = cdt.clone();
        assertEquals(cdt, clone);
        assertSame(cdt.getFunction().orElseThrow(), clone.getFunction().orElseThrow());
    }
}
