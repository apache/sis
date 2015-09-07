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
package org.apache.sis.referencing.crs;

import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link DefaultVerticalCRS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class DefaultVerticalCRSTest extends TestCase {
    /**
     * Tests WKT 1 formatting.
     */
    @Test
    public void testWKT1() {
        assertWktEquals(Convention.WKT1,
                "VERT_CS[“Depth”,\n" +
                "  VERT_DATUM[“Mean Sea Level”, 2005],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“Depth”, DOWN]]",
                HardCodedCRS.DEPTH);
    }

    /**
     * Tests WKT 2 formatting.
     */
    @Test
    @DependsOnMethod("testWKT1")
    public void testWKT2() {
        assertWktEquals(Convention.WKT2,
                "VERTCRS[“Depth”,\n" +
                "  VDATUM[“Mean Sea Level”],\n" +
                "  CS[vertical, 1],\n" +
                "    AXIS[“Depth (D)”, down, ORDER[1]],\n" +
                "    LENGTHUNIT[“metre”, 1]]",
                HardCodedCRS.DEPTH);
    }

    /**
     * Tests WKT 2 "simplified" formatting.
     */
    @Test
    @DependsOnMethod("testWKT2")
    public void testWKT2_Simplified() {
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "VerticalCRS[“Depth”,\n" +
                "  VerticalDatum[“Mean Sea Level”],\n" +
                "  CS[vertical, 1],\n" +
                "    Axis[“Depth (D)”, down],\n" +
                "    Unit[“metre”, 1]]",
                HardCodedCRS.DEPTH);
    }
}
