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
 * Tests for {@link org.apache.sis.internal.style.Mark}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class MarkTest extends AbstractStyleTests {

    /**
     * Test of WellKnownName methods.
     */
    @Test
    public void testWellKnownName() {
        Mark cdt = new Mark();

        //check default
        assertEquals(FF.literal("square"), cdt.getWellKnownName());

        //check get/set
        cdt.setWellKnownName(EXP_STRING);
        assertEquals(EXP_STRING, cdt.getWellKnownName());
    }

    /**
     * Test of ExternalMark methods.
     */
    @Test
    public void testExternalMark() {
        Mark cdt = new Mark();

        //check default
        assertEquals(null, cdt.getExternalMark());

        //check get/set
        ExternalMark value = new ExternalMark();
        cdt.setExternalMark(value);
        assertEquals(value, cdt.getExternalMark());
    }

    /**
     * Test of Fill methods.
     */
    @Test
    public void testFill() {
        Mark cdt = new Mark();

        //check default
        assertEquals(new Fill(), cdt.getFill());

        //check get/set
        cdt.setFill(new Fill(null, EXP_COLOR, EXP_DOUBLE));
        assertEquals(new Fill(null, EXP_COLOR, EXP_DOUBLE), cdt.getFill());
    }

    /**
     * Test of getStroke methods.
     */
    @Test
    public void testStroke() {
        Mark cdt = new Mark();

        //check default
        assertEquals(new Stroke(), cdt.getStroke());

        //check get/set
        Stroke value = new Stroke();
        cdt.setStroke(value);
        assertEquals(value, cdt.getStroke());
    }

}
