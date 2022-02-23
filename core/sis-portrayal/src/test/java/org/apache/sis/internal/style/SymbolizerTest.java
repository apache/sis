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

import org.apache.sis.measure.Units;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link org.apache.sis.internal.style.Symbolizer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SymbolizerTest extends AbstractStyleTests {

    /**
     * Test of UnitOfMeasure methods.
     */
    @Test
    public void testUnitOfMeasure() {
        Symbolizer cdt = new LineSymbolizer();

        //check default
        assertEquals(null, cdt.getUnitOfMeasure());

        //check get/set
        cdt.setUnitOfMeasure(Units.INCH);
        assertEquals(Units.INCH, cdt.getUnitOfMeasure());
    }

    /**
     * Test of Geometry methods.
     */
    @Test
    public void testGeometry() {
        Symbolizer cdt = new LineSymbolizer();

        //check default
        assertEquals(null, cdt.getGeometry());

        //check get/set
        cdt.setGeometry(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getGeometry());
    }

    /**
     * Test of Name methods.
     */
    @Test
    public void testName() {
        Symbolizer cdt = new LineSymbolizer();

        //check defaults
        assertEquals(null, cdt.getName());

        //check get/set
        cdt.setName(SAMPLE_STRING);
        assertEquals(SAMPLE_STRING, cdt.getName());
    }

    /**
     * Test of getDescription methods.
     */
    @Test
    public void testDescription() {
        Symbolizer cdt = new LineSymbolizer();

        //check defaults
        assertEquals(null, cdt.getDescription());

        //check get/set
        cdt.setDescription(new Description(SAMPLE_ISTRING, SAMPLE_ISTRING));
        assertEquals(new Description(SAMPLE_ISTRING, SAMPLE_ISTRING), cdt.getDescription());
    }

}
