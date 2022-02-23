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

import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * Tests for {@link org.apache.sis.internal.style.Rule}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class RuleTest extends AbstractStyleTests {

    /**
     * Test of Name methods.
     */
    @Test
    public void testGetName() {
        Rule cdt = new Rule();

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
        Rule cdt = new Rule();

        //check defaults
        assertEquals(null, cdt.getDescription());

        //check get/set
        cdt.setDescription(new Description(SAMPLE_ISTRING, SAMPLE_ISTRING));
        assertEquals(new Description(SAMPLE_ISTRING, SAMPLE_ISTRING), cdt.getDescription());
    }

    /**
     * Test of Legend methods.
     */
    @Test
    public void testLegend() {
        Rule cdt = new Rule();

        //check defaults
        assertEquals(null, cdt.getLegend());

        //check get/set
        GraphicLegend value = new GraphicLegend();
        cdt.setLegend(value);
        assertEquals(value, cdt.getLegend());
    }

    /**
     * Test of Filter methods.
     */
    @Test
    public void testFilter() {
        Rule cdt = new Rule();

        //check defaults
        assertEquals(null, cdt.getFilter());

        //check get/set
        Filter value = FF.equal(EXP_STRING, EXP_STRING);
        cdt.setFilter(value);
        assertEquals(value, cdt.getFilter());
    }

    /**
     * Test of ElseFilter methods.
     */
    @Test
    public void testIsElseFilter() {
        Rule cdt = new Rule();

        //check defaults
        assertFalse(cdt.isElseFilter());

        //check get/set
        cdt.setElseFilter(true);
        assertTrue(cdt.isElseFilter());
    }

    /**
     * Test of MinScaleDenominator methods.
     */
    @Test
    public void testMinScaleDenominator() {
        Rule cdt = new Rule();

        //check defaults
        assertEquals(0.0, cdt.getMinScaleDenominator(), 0.0);

        //check get/set
        cdt.setMinScaleDenominator(10.0);
        assertEquals(10.0, cdt.getMinScaleDenominator(), 0.0);
    }

    /**
     * Test of MaxScaleDenominator methods.
     */
    @Test
    public void testGetMaxScaleDenominator() {
        Rule cdt = new Rule();

        //check defaults
        assertEquals(Double.MAX_VALUE, cdt.getMaxScaleDenominator(), 0.0);

        //check get/set
        cdt.setMaxScaleDenominator(10.0);
        assertEquals(10.0, cdt.getMaxScaleDenominator(), 0.0);
    }

    /**
     * Test of symbolizers methods.
     */
    @Test
    public void testSymbolizers() {
        Rule cdt = new Rule();

        //check defaults
        assertTrue(cdt.symbolizers().isEmpty());

        //check get/set
        cdt.symbolizers().add(new LineSymbolizer());
        assertEquals(1, cdt.symbolizers().size());
    }

    /**
     * Test of OnlineResource methods.
     */
    @Test
    public void testGetOnlineResource() {
        Rule cdt = new Rule();

        //check defaults
        assertEquals(null, cdt.getOnlineResource());

        //check get/set
        cdt.setOnlineResource(new DefaultOnlineResource());
        assertEquals(new DefaultOnlineResource(), cdt.getOnlineResource());
    }

}
