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
package org.apache.sis.io.wkt;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Symbols} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-2.4)
 * @version 0.4
 * @module
 */
public final strictfp class SymbolsTest extends TestCase {
    /**
     * Tests the {@link Symbols#containsAxis(CharSequence)} method.
     */
    @Test
    public void testContainsAxis() {
        final Symbols s = Symbols.DEFAULT;
        assertTrue("At beginning of a line.",   s.containsAxis(                  "AXIS[\"Long\", EAST]"));
        assertTrue("Embeded in GEOGCS.",        s.containsAxis("GEOGCS[\"WGS84\", AXIS[\"Long\", EAST]]"));
        assertTrue("Using different brackets.", s.containsAxis("GEOGCS[\"WGS84\", AXIS (\"Long\", EAST)]"));
        assertTrue("Mixed cases.",              s.containsAxis("GEOGCS[\"WGS84\", aXis[\"Long\", EAST]]"));
        assertFalse("AXIS in quoted text.",     s.containsAxis("GEOGCS[\"AXIS\"]"));
        assertFalse("Without opening bracket.", s.containsAxis("GEOGCS[\"WGS84\", AXIS]"));
        assertFalse("No AXIS.",                 s.containsAxis("GEOGCS[\"WGS84\"]"));
    }
}
