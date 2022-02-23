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
 * Tests for {@link org.apache.sis.internal.style.Style}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class StyleTest extends AbstractStyleTests {

    /**
     * Test of featureTypeStyles methods.
     */
    @Test
    public void testFeatureTypeStyles() {
        Style cdt = new Style();

        //check defaults
        assertTrue(cdt.featureTypeStyles().isEmpty());

        //check get/set
        cdt.featureTypeStyles().add(new FeatureTypeStyle());
        assertEquals(1, cdt.featureTypeStyles().size());
    }

    /**
     * Test of isDefault methods.
     */
    @Test
    public void testIsDefault() {
        Style cdt = new Style();

        //check defaults
        assertFalse(cdt.isDefault());

        //check get/set
        cdt.setDefault(true);
        assertTrue(cdt.isDefault());
    }

    /**
     * Test of DefaultSpecification methods.
     */
    @Test
    public void testDefaultSpecification() {
        Style cdt = new Style();

        //check defaults
        assertEquals(null, cdt.getDefaultSpecification());

        //check get/set
        Symbolizer value = new LineSymbolizer();
        cdt.setDefaultSpecification(value);
        assertEquals(value, cdt.getDefaultSpecification());
    }

    /**
     * Test of getName methods.
     */
    @Test
    public void testName() {
        Style cdt = new Style();

        //check defaults
        assertEquals(null, cdt.getName());

        //check get/set
        cdt.setName(SAMPLE_STRING);
        assertEquals(SAMPLE_STRING, cdt.getName());
    }

    /**
     * Test of Description methods.
     */
    @Test
    public void testDescription() {
        Style cdt = new Style();

        //check defaults
        assertEquals(null, cdt.getDescription());

        //check get/set
        cdt.setDescription(new Description(SAMPLE_ISTRING, SAMPLE_ISTRING));
        assertEquals(new Description(SAMPLE_ISTRING, SAMPLE_ISTRING), cdt.getDescription());
    }

}
