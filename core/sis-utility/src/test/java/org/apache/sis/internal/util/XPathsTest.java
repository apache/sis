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

import org.apache.sis.util.Characters;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link XPaths}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.4
 * @module
 */
public final strictfp class XPathsTest extends TestCase {
    /**
     * Tests the {@link XPaths#endOfURI(CharSequence, int)} method.
     */
    @Test
    public void testEndOfURI() {
        assertEquals(26, XPaths.endOfURI("urn:ogc:def:uom:EPSG::9001", 0));
        assertEquals(80, XPaths.endOfURI("http://www.isotc211.org/2005/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])", 0));
        assertEquals(97, XPaths.endOfURI("http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])", 0));
        assertEquals(-1, XPaths.endOfURI("m/s", 0));
        assertEquals(-1, XPaths.endOfURI("m.s", 0));
        assertEquals(11, XPaths.endOfURI("EPSG" + Characters.NO_BREAK_SPACE + ": 9001", 0));
    }

    /**
     * Tests {@link XPaths#split(String)}.
     */
    @Test
    public void testSplit() {
        assertNull(XPaths.split("property"));
        assertArrayEquals(new String[] {"/property"},                    XPaths.split("/property").toArray());
        assertArrayEquals(new String[] {"Feature", "property", "child"}, XPaths.split("Feature/property/child").toArray());
        assertArrayEquals(new String[] {"/Feature", "property"},         XPaths.split("/Feature/property").toArray());
    }
}
