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

import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.style.OverlapBehavior;

/**
 * Tests for {@link org.apache.sis.internal.style.RasterSymbolizer}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class RasterSymbolizerTest extends AbstractStyleTests {

    /**
     * Test of Opacity methods.
     */
    @Test
    public void testOpacity() {
        RasterSymbolizer cdt = new RasterSymbolizer();

        //check default
        assertEquals(null, cdt.getOpacity());

        //check get/set
        cdt.setOpacity(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getOpacity());
    }

    /**
     * Test of ChannelSelection methods.
     */
    @Test
    public void testChannelSelection() {
        RasterSymbolizer cdt = new RasterSymbolizer();

        //check default
        assertEquals(null, cdt.getChannelSelection());

        //check get/set
        ChannelSelection value = new ChannelSelection();
        cdt.setChannelSelection(value);
        assertEquals(value, cdt.getChannelSelection());
    }

    /**
     * Test of OverlapBehavior methods.
     */
    @Test
    public void testOverlapBehavior() {
        RasterSymbolizer cdt = new RasterSymbolizer();

        //check default
        assertEquals(null, cdt.getOverlapBehavior());

        //check get/set
        cdt.setOverlapBehavior(OverlapBehavior.EARLIEST_ON_TOP);
        assertEquals(OverlapBehavior.EARLIEST_ON_TOP, cdt.getOverlapBehavior());
    }

    /**
     * Test of ColorMap methods.
     */
    @Test
    public void testColorMap() {
        RasterSymbolizer cdt = new RasterSymbolizer();

        //check default
        assertEquals(null, cdt.getColorMap());

        //check get/set
        ColorMap value = new ColorMap();
        cdt.setColorMap(value);
        assertEquals(value, cdt.getColorMap());
    }

    /**
     * Test of ContrastEnhancement methods.
     */
    @Test
    public void testGetContrastEnhancement() {
        RasterSymbolizer cdt = new RasterSymbolizer();

        //check default
        assertEquals(null, cdt.getContrastEnhancement());

        //check get/set
        ContrastEnhancement value = new ContrastEnhancement();
        cdt.setContrastEnhancement(value);
        assertEquals(value, cdt.getContrastEnhancement());
    }

    /**
     * Test of ShadedRelief methods.
     */
    @Test
    public void testGetShadedRelief() {
        RasterSymbolizer cdt = new RasterSymbolizer();

        //check default
        assertEquals(null, cdt.getShadedRelief());

        //check get/set
        ShadedRelief value = new ShadedRelief();
        cdt.setShadedRelief(value);
        assertEquals(value, cdt.getShadedRelief());
    }

    /**
     * Test of ImageOutline methods.
     */
    @Test
    public void testImageOutline() {
        RasterSymbolizer cdt = new RasterSymbolizer();

        //check default
        assertEquals(null, cdt.getImageOutline());

        //check get/set
        Symbolizer value = new LineSymbolizer();
        cdt.setImageOutline(value);
        assertEquals(value, cdt.getImageOutline());
    }

}
