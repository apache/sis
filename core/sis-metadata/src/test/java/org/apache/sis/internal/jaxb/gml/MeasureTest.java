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
package org.apache.sis.internal.jaxb.gml;

import java.net.URISyntaxException;
import org.apache.sis.measure.Units;
import org.apache.sis.internal.jaxb.cat.CodeListUID;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Measure}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.3
 * @module
 */
public final strictfp class MeasureTest extends TestCase {
    /**
     * The URL used in {@code uom} attribute of XML elements which contains a measurements.
     * Example:
     *
     * {@preformat xml
     *     <gco:Distance uom="http://www.isotc211.org/2005/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])">1000.0</gco:Distance>
     * }
     */
    public static final String UOM_URL = CodeListUID.METADATA_ROOT_LEGACY + CodeListUID.UOM_PATH;

    /**
     * Tests the {@link Measure#getUOM()}.
     */
    @Test
    public void testGetUOM() {
        final Measure measure = new Measure(10, Units.METRE);
        assertEquals("urn:ogc:def:uom:EPSG::9001", measure.getUOM());
        measure.unit = Units.DEGREE;
        assertEquals("urn:ogc:def:uom:EPSG::9102", measure.getUOM());
        measure.unit = Units.UNITY;
        assertEquals("urn:ogc:def:uom:EPSG::9201", measure.getUOM());
    }

    /**
     * Tests the {@link Measure#setUOM(String)}.
     *
     * @throws URISyntaxException if the URI used by the test is invalid.
     */
    @Test
    @DependsOnMethod("testGetUOM")
    public void testSetUOM() throws URISyntaxException {
        final Measure measure = new Measure();
        measure.setUOM("http://www.isotc211.org/2005/resources/uom/gmxUom.xml#m");
        assertEquals(Units.METRE, measure.unit);
        assertEquals("urn:ogc:def:uom:EPSG::9001", measure.getUOM());
        measure.asXPointer = true;
        assertEquals(UOM_URL + "#xpointer(//*[@gml:id='m'])", measure.getUOM());

        measure.unit = null;
        measure.asXPointer = false;
        measure.setUOM("../uom/ML_gmxUom.xml#xpointer(//*[@gml:id='deg'])");
        assertEquals(Units.DEGREE, measure.unit);
        assertEquals("urn:ogc:def:uom:EPSG::9102", measure.getUOM());
        measure.asXPointer = true;
        assertEquals(UOM_URL + "#xpointer(//*[@gml:id='deg'])", measure.getUOM());

        measure.unit = null;
        measure.asXPointer = true;
        measure.setUOM("gmxUom.xml#kg");                        // Not really an existing unit in 'gmxUom'.
        assertEquals(Units.KILOGRAM, measure.unit);
        assertEquals(UOM_URL + "#xpointer(//*[@gml:id='kg'])", measure.getUOM());
    }
}
