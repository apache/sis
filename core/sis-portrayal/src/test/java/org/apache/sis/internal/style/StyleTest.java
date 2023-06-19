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
 * Tests for {@link Style}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public final class StyleTest extends StyleTestCase {
    /**
     * Creates a new test case.
     */
    public StyleTest() {
    }

    /**
     * Test of {@code featureTypeStyles} property.
     */
    @Test
    public void testFeatureTypeStyles() {
        Style cdt = new Style();

        // Check defaults
        assertTrue(cdt.featureTypeStyles().isEmpty());

        // Check get/set
        cdt.featureTypeStyles().add(new FeatureTypeStyle());
        assertEquals(1, cdt.featureTypeStyles().size());
    }

    /**
     * Test of {@code isDefault} property.
     */
    @Test
    public void testIsDefault() {
        Style cdt = new Style();

        // Check defaults
        assertFalse(cdt.isDefault());

        // Check get/set
        cdt.setDefault(true);
        assertTrue(cdt.isDefault());
    }

    /**
     * Test of {@code DefaultSpecification} property.
     */
    @Test
    public void testDefaultSpecification() {
        Style cdt = new Style();

        // Check defaults
        assertEmpty(cdt.getDefaultSpecification());

        // Check get/set
        Symbolizer value = new LineSymbolizer();
        cdt.setDefaultSpecification(value);
        assertOptionalEquals(value, cdt.getDefaultSpecification());
    }

    /**
     * Test of {@code getName} property.
     */
    @Test
    public void testName() {
        Style cdt = new Style();

        // Check defaults
        assertEmpty(cdt.getName());

        // Check get/set
        String value = "A random name";
        cdt.setName(value);
        assertOptionalEquals(value, cdt.getName());
    }

    /**
     * Test of {@code Description} property.
     */
    @Test
    public void testDescription() {
        Style cdt = new Style();

        // Check defaults
        assertEmpty(cdt.getDescription());

        // Check get/set
        Description desc = anyDescription();
        cdt.setDescription(desc);
        assertOptionalEquals(desc, cdt.getDescription());
    }
}
