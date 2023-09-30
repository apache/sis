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

import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.ValueReference;


/**
 * Tests for {@link Symbolizer}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class SymbolizerTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public SymbolizerTest() {
    }

    /**
     * Test of {@code UnitOfMeasure} property.
     */
    @Test
    public void testUnitOfMeasure() {
        final var cdt = factory.createLineSymbolizer();

        // Check default
        assertEquals(Units.PIXEL, cdt.getUnitOfMeasure());

        // Check get/set
        cdt.setUnitOfMeasure(Units.INCH);
        assertEquals(Units.INCH, cdt.getUnitOfMeasure());
    }

    /**
     * Test of {@code Geometry} property.
     */
    @Test
    public void testGeometry() {
        final var cdt = factory.createLineSymbolizer();

        // Check default
        assertInstanceOf("geometry", ValueReference.class, cdt.getGeometry());

        // Check get/set
        cdt.setGeometry(literal(8));
        assertLiteralEquals(8, cdt.getGeometry());
    }

    /**
     * Test of {@code Name} property.
     */
    @Test
    public void testName() {
        final var cdt = factory.createLineSymbolizer();

        // Check defaults
        assertEmpty(cdt.getName());

        // Check get/set
        String value = "A random name";
        cdt.setName(value);
        assertOptionalEquals(value, cdt.getName());
    }

    /**
     * Test of {@code getDescription} property.
     */
    @Test
    public void testDescription() {
        final var cdt = factory.createLineSymbolizer();

        // Check defaults
        assertEmpty(cdt.getDescription());

        // Check get/set
        var desc = anyDescription();
        cdt.setDescription(desc);
        assertOptionalEquals(desc, cdt.getDescription());
    }
}
