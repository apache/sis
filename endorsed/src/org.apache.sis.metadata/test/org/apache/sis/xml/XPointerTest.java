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
package org.apache.sis.xml;

import org.apache.sis.util.Characters;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link XPointer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class XPointerTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public XPointerTest() {
    }

    /**
     * Tests {@link XPointer#UOM}.
     */
    @Test
    public void testUOM() {
        assertEquals("m", XPointer.UOM.reference("http://www.isotc211.org/2005/resources/uom/gmxUom.xml#m"));
        assertEquals("m", XPointer.UOM.reference("http://www.isotc211.org/2005/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"));
        assertEquals("m", XPointer.UOM.reference("http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/uom/ML_gmxUom.xml#xpointer(//*[@gml:id='m'])"));
        assertEquals("m", XPointer.UOM.reference("../uom/ML_gmxUom.xml#xpointer(//*[@gml:id='m'])"));
    }

    /**
     * Tests the {@link XPointer#endOfURI(CharSequence, int)} method.
     */
    @Test
    public void testEndOfURI() {
        assertEquals(26, XPointer.endOfURI("urn:ogc:def:uom:EPSG::9001", 0));
        assertEquals(80, XPointer.endOfURI("http://www.isotc211.org/2005/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])", 0));
        assertEquals(97, XPointer.endOfURI("http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])", 0));
        assertEquals(-1, XPointer.endOfURI("m/s", 0));
        assertEquals(-1, XPointer.endOfURI("m.s", 0));
        assertEquals(11, XPointer.endOfURI("EPSG" + Characters.NO_BREAK_SPACE + ": 9001", 0));
    }
}
