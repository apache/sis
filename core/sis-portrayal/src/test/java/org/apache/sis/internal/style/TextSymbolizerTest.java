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
 * Tests for {@link org.apache.sis.internal.style.TextSymbolizer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class TextSymbolizerTest extends AbstractStyleTests {

    /**
     * Test of Label methods.
     */
    @Test
    public void testLabel() {
        TextSymbolizer cdt = new TextSymbolizer();

        //check default
        assertEquals(null, cdt.getLabel());

        //check get/set
        cdt.setLabel(EXP_STRING);
        assertEquals(EXP_STRING, cdt.getLabel());
    }

    /**
     * Test of Font methods.
     */
    @Test
    public void testFont() {
        TextSymbolizer cdt = new TextSymbolizer();

        //check default
        assertEquals(null, cdt.getFont());

        //check get/set
        Font value = new Font();
        cdt.setFont(value);
        assertEquals(value, cdt.getFont());
    }

    /**
     * Test of LabelPlacement methods.
     */
    @Test
    public void testLabelPlacement() {
        TextSymbolizer cdt = new TextSymbolizer();

        //check default
        assertEquals(null, cdt.getLabelPlacement());

        //check get/set
        LabelPlacement value = new PointPlacement();
        cdt.setLabelPlacement(value);
        assertEquals(value, cdt.getLabelPlacement());
    }

    /**
     * Test of Halo methods.
     */
    @Test
    public void testHalo() {
        TextSymbolizer cdt = new TextSymbolizer();

        //check default
        assertEquals(null, cdt.getHalo());

        //check get/set
        Halo value = new Halo();
        cdt.setHalo(value);
        assertEquals(value, cdt.getHalo());
    }

    /**
     * Test of Fill methods.
     */
    @Test
    public void testFill() {
        TextSymbolizer cdt = new TextSymbolizer();

        //check default
        assertEquals(null, cdt.getFill());

        //check get/set
        Fill value = new Fill();
        cdt.setFill(value);
        assertEquals(value, cdt.getFill());
    }

}
