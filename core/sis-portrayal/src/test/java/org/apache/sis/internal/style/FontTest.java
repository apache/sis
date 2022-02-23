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
 * Tests for {@link org.apache.sis.internal.style.Font}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class FontTest extends AbstractStyleTests {

    /**
     * Test of Family methods.
     */
    @Test
    public void testFamily() {
        Font cdt = new Font();

        //check default
        assertTrue(cdt.getFamily().isEmpty());

        //check get/set
        cdt.getFamily().add(EXP_STRING);
        assertEquals(1, cdt.getFamily().size());
    }
    /**
     * Test of Style methods.
     */
    @Test
    public void testStyle() {
        Font cdt = new Font();

        //check default
        assertEquals(FF.literal("normal"), cdt.getStyle());

        //check get/set
        cdt.setStyle(EXP_STRING);
        assertEquals(EXP_STRING, cdt.getStyle());
    }

    /**
     * Test of Weight methods.
     */
    @Test
    public void testWeight() {
        Font cdt = new Font();

        //check default
        assertEquals(FF.literal("normal"), cdt.getWeight());

        //check get/set
        cdt.setWeight(EXP_STRING);
        assertEquals(EXP_STRING, cdt.getWeight());
    }

    /**
     * Test of Size methods.
     */
    @Test
    public void testSize() {
        Font cdt = new Font();

        //check default
        assertEquals(FF.literal(10.0), cdt.getSize());

        //check get/set
        cdt.setSize(EXP_DOUBLE);
        assertEquals(EXP_DOUBLE, cdt.getSize());
    }

}
