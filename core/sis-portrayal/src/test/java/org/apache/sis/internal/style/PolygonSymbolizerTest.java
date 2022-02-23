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
 * Tests for {@link org.apache.sis.internal.style.PolygonSymbolizer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class PolygonSymbolizerTest extends AbstractStyleTests {

    /**
     * Test of Stroke methods.
     */
    @Test
    public void testStroke() {
        PolygonSymbolizer cdt = new PolygonSymbolizer();

        //check default
        assertEquals(null, cdt.getStroke());

        //check get/set
        Stroke value = new Stroke();
        cdt.setStroke(value);
        assertEquals(value, cdt.getStroke());
    }

    /**
     * Test of Fill methods.
     */
    @Test
    public void testFill() {
        PolygonSymbolizer cdt = new PolygonSymbolizer();

        //check default
        assertEquals(null, cdt.getFill());

        //check get/set
        Fill value = new Fill();
        cdt.setFill(value);
        assertEquals(value, cdt.getFill());
    }

    /**
     * Test of Displacement methods.
     */
    @Test
    public void testDisplacement() {
        PolygonSymbolizer cdt = new PolygonSymbolizer();

        //check default
        assertEquals(null, cdt.getDisplacement());

        //check get/set
        Displacement value = new Displacement(EXP_DOUBLE, EXP_DOUBLE_2);
        cdt.setDisplacement(value);
        assertEquals(value, cdt.getDisplacement());
    }

    /**
     * Test of PerpendicularOffset methods.
     */
    @Test
    public void testPerpendicularOffset() {
        PolygonSymbolizer cdt = new PolygonSymbolizer();

        //check default
        assertEquals(null, cdt.getPerpendicularOffset());

        //check get/set
        cdt.setPerpendicularOffset(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getPerpendicularOffset());
    }

}
