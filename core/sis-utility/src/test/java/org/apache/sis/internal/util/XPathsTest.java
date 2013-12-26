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
package org.apache.sis.internal.util;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link XPaths}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class XPathsTest extends TestCase {
    /**
     * Tests {@link XPaths#xpointer(String, String)}.
     */
    @Test
    public void testXPointer() {
        assertEquals("m", XPaths.xpointer("uom", "http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#m"));
        assertEquals("m", XPaths.xpointer("uom", "http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"));
        assertEquals("m", XPaths.xpointer("uom", "http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/uom/ML_gmxUom.xml#xpointer(//*[@gml:id='m'])"));
        assertEquals("m", XPaths.xpointer("uom", "../uom/ML_gmxUom.xml#xpointer(//*[@gml:id='m'])"));
    }
}
