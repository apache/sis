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
 * Tests for {@link org.apache.sis.internal.style.Graphic}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GraphicTest extends AbstractStyleTests {

    /**
     * Test of graphicalSymbols methods.
     */
    @Test
    public void testGraphicalSymbols() {
        Graphic cdt = new Graphic();

        //check defaults
        assertTrue(cdt.graphicalSymbols().isEmpty());

        //check get/set
        cdt.graphicalSymbols().add(new Mark());
        assertEquals(1, cdt.graphicalSymbols().size());
    }

    /**
     * Test of Opacity methods.
     */
    @Test
    public void testOpacity() {
        Graphic cdt = new Graphic();

        //check default
        assertEquals(null, cdt.getOpacity());

        //check get/set
        cdt.setOpacity(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getOpacity());
    }

    /**
     * Test of Size methods.
     */
    @Test
    public void testSize() {
        Graphic cdt = new Graphic();

        //check default
        assertEquals(null, cdt.getSize());

        //check get/set
        cdt.setSize(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getSize());
    }

    /**
     * Test of Rotation methods.
     */
    @Test
    public void testRotation() {
        Graphic cdt = new Graphic();

        //check default
        assertEquals(null, cdt.getRotation());

        //check get/set
        cdt.setRotation(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getRotation());
    }

    /**
     * Test of AnchorPoint methods.
     */
    @Test
    public void testAnchorPoint() {
        Graphic cdt = new Graphic();

        //check default
        assertEquals(null, cdt.getAnchorPoint());

        //check get/set
        AnchorPoint value = new AnchorPoint(EXP_DOUBLE, EXP_DOUBLE_2);
        cdt.setAnchorPoint(value);
        assertEquals(value, cdt.getAnchorPoint());
    }

    /**
     * Test of Displacement methods.
     */
    @Test
    public void testDisplacement() {
        Graphic cdt = new Graphic();

        //check default
        assertEquals(null, cdt.getDisplacement());

        //check get/set
        Displacement value = new Displacement(EXP_DOUBLE, EXP_DOUBLE_2);
        cdt.setDisplacement(value);
        assertEquals(value, cdt.getDisplacement());
    }

}
