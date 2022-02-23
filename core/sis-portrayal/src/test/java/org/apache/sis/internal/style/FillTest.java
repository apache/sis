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
 * Tests for {@link org.apache.sis.internal.style.Fill}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class FillTest extends AbstractStyleTests {

    /**
     * Test of GraphicFill methods.
     */
    @Test
    public void testGraphicFill() {
        Fill cdt = new Fill();

        //check default
        assertEquals(null, cdt.getGraphicFill());

        //check get/set
        final GraphicFill value = new GraphicFill();
        cdt.setGraphicFill(value);
        assertEquals(value, cdt.getGraphicFill());
    }

    /**
     * Test of Color methods.
     */
    @Test
    public void testColor() {
        Fill cdt = new Fill();

        //check default
        assertEquals(FF.literal(Color.GRAY), cdt.getColor());

        //check get/set
        cdt.setColor(EXP_COLOR);
        assertEquals(EXP_COLOR, cdt.getColor());
    }

    /**
     * Test of Opacity methods.
     */
    @Test
    public void testOpacity() {
        Fill cdt = new Fill();

        //check default
        assertEquals(FF.literal(1.0), cdt.getOpacity());

        //check get/set
        cdt.setOpacity(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getOpacity());
    }

}
