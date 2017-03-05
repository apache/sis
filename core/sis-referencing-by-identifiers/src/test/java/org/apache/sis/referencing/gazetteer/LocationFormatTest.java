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
package org.apache.sis.referencing.gazetteer;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link LocationFormat}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final strictfp class LocationFormatTest extends TestCase {
    /**
     * Tests formatting of an instance having only geographic coordinates.
     */
    @Test
    public void testGeographic() {
        final SimpleLocation loc = new SimpleLocation(null, "A location");
        loc.minX =   8;
        loc.maxX =  20;
        loc.minY = -10;
        loc.maxY =  30;
        final LocationFormat format = new LocationFormat(null, null);
        assertMultilinesEquals(
                "Geographic identifier:       A location\n" +
                "East bound:                   8°E\n" +
                "West bound:                  20°E\n" +
                "South bound:                 10°S\n" +
                "North bound:                 30°N\n" +
                "Representative position:     14°E\n" +
                "                             10°N\n" +
                "Coordinate reference system: WGS 84\n", format.format(loc));
    }
}
