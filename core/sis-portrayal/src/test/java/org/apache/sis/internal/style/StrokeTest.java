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

import java.awt.Color;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link org.apache.sis.internal.style.Stroke}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class StrokeTest extends AbstractStyleTests {

    /**
     * Test of GraphicFill methods.
     */
    @Test
    public void testGraphicFill() {
        Stroke cdt = new Stroke();

        //check default
        assertEquals(null, cdt.getGraphicFill());

        //check get/set
        GraphicFill value = new GraphicFill();
        cdt.setGraphicFill(value);
        assertEquals(value, cdt.getGraphicFill());
    }

    /**
     * Test of GraphicStroke methods.
     */
    @Test
    public void testGraphicStroke() {
        Stroke cdt = new Stroke();

        //check default
        assertEquals(null, cdt.getGraphicStroke());

        //check get/set
        GraphicStroke value = new GraphicStroke();
        cdt.setGraphicStroke(value);
        assertEquals(value, cdt.getGraphicStroke());
    }

    /**
     * Test of Color methods.
     */
    @Test
    public void testColor() {
        Stroke cdt = new Stroke();

        //check default
        assertEquals(FF.literal(Color.BLACK), cdt.getColor());

        //check get/set
        cdt.setColor(EXP_COLOR);
        assertEquals(EXP_COLOR, cdt.getColor());
    }

    /**
     * Test of Opacity methods.
     */
    @Test
    public void testOpacity() {
        Stroke cdt = new Stroke();

        //check default
        assertEquals(FF.literal(1.0), cdt.getOpacity());

        //check get/set
        cdt.setOpacity(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getOpacity());
    }

    /**
     * Test of Width methods.
     */
    @Test
    public void testWidth() {
        Stroke cdt = new Stroke();

        //check default
        assertEquals(FF.literal(1.0), cdt.getWidth());

        //check get/set
        cdt.setWidth(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getWidth());
    }

    /**
     * Test of LineJoin methods.
     */
    @Test
    public void testLineJoin() {
        Stroke cdt = new Stroke();

        //check default
        assertEquals(FF.literal("bevel"), cdt.getLineJoin());

        //check get/set
        cdt.setLineJoin(EXP_STRING);
        assertEquals(EXP_STRING, cdt.getLineJoin());
    }

    /**
     * Test of LineCap methods.
     */
    @Test
    public void testLineCap() {
        Stroke cdt = new Stroke();

        //check default
        assertEquals(FF.literal("square"), cdt.getLineCap());

        //check get/set
        cdt.setLineCap(EXP_STRING);
        assertEquals(EXP_STRING, cdt.getLineCap());
    }

    /**
     * Test of DashArray methods.
     */
    @Test
    public void testDashArray() {
        Stroke cdt = new Stroke();

        //check default
        assertEquals(null, cdt.getDashArray());

        //check get/set
        cdt.setDashArray(new float[]{1,2,3});
        assertArrayEquals(new float[]{1,2,3}, cdt.getDashArray(), 0.0f);
    }

    /**
     * Test of DashOffset methods.
     */
    @Test
    public void testDashOffset() {
        Stroke cdt = new Stroke();

        //check default
        assertEquals(FF.literal(0.0), cdt.getDashOffset());

        //check get/set
        cdt.setDashOffset(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getDashOffset());
    }

}
