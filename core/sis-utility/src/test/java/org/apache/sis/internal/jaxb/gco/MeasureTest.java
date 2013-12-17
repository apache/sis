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
package org.apache.sis.internal.jaxb.gco;

import java.net.URISyntaxException;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import org.apache.sis.internal.jaxb.Schemas;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Test {@link Measure}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.10)
 * @version 0.3
 * @module
 */
public final strictfp class MeasureTest extends TestCase {
    /**
     * Tests the {@link Measure#setUOM(String)}.
     *
     * @throws URISyntaxException Should not happen.
     */
    @Test
    public void testSetUOM() throws URISyntaxException {
        final Measure measure = new Measure();
        measure.setUOM("http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#m");
        assertEquals(SI.METRE, measure.unit);
        assertEquals("urn:ogc:def:uom:EPSG::9001", measure.getUOM());
        measure.asXPointer = true;
        assertEquals(Schemas.METADATA_ROOT + Schemas.UOM_PATH + "#xpointer(//*[@gml:id='m'])", measure.getUOM());

        measure.unit = null;
        measure.asXPointer = false;
        measure.setUOM("../uom/ML_gmxUom.xml#xpointer(//*[@gml:id='deg'])");
        assertEquals(NonSI.DEGREE_ANGLE, measure.unit);
        assertEquals("urn:ogc:def:uom:EPSG::9102", measure.getUOM());
        measure.asXPointer = true;
        assertEquals(Schemas.METADATA_ROOT + Schemas.UOM_PATH + "#xpointer(//*[@gml:id='deg'])", measure.getUOM());

        measure.unit = null;
        measure.asXPointer = true;
        measure.setUOM("gmxUom.xml#kg"); // Not really an existing unit in 'gmxUom'.
        assertEquals(SI.KILOGRAM, measure.unit);
        assertEquals(Schemas.METADATA_ROOT + Schemas.UOM_PATH + "#xpointer(//*[@gml:id='kg'])", measure.getUOM());
    }
}
