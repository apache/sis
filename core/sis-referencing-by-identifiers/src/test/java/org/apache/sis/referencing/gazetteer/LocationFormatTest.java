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

import java.util.Locale;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assertions.assertMultilinesEquals;


/**
 * Tests {@link LocationFormat}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 */
public final class LocationFormatTest extends TestCase {
    /**
     * Tests formatting of an instance having only geographic coordinates.
     */
    @Test
    public void testGeographic() {
        final SimpleLocation loc = new SimpleLocation(null, "A location");
        loc.minX =  8;
        loc.maxX = 10;
        loc.minY = -2;
        loc.maxY = 30;
        final LocationFormat format = new LocationFormat(Locale.US, null);
        assertMultilinesEquals(
                "┌─────────────────────────────────────────┐\n" +
                "│ Geographic identifier:       A location │\n" +
                "│ West bound:                   8°00′00″E │\n" +
                "│ Representative value:         9°00′00″E │\n" +
                "│ East bound:                  10°00′00″E │\n" +
                "│ South bound:                  2°00′00″S │\n" +
                "│ Representative value:        14°00′00″N │\n" +
                "│ North bound:                 30°00′00″N │\n" +
                "│ Coordinate reference system: WGS 84     │\n" +
                "└─────────────────────────────────────────┘\n", format.format(loc));
    }

    /**
     * Tests formatting of an instance having only projected coordinates.
     */
    @Test
    @DependsOnMethod("testGeographic")
    public void testProjected() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.universal(14, 9);
        final SimpleLocation loc = new SimpleLocation(null, "A location") {
            @Override public CoordinateReferenceSystem getCoordinateReferenceSystem() {return crs;}
            @Override public GeographicExtent          getGeographicExtent()          {return null;}
        };
        loc.minX =  388719.35;
        loc.maxX =  611280.65;
        loc.minY = -221094.87;
        loc.maxY = 3319206.22;
        final LocationFormat format = new LocationFormat(Locale.US, null);
        assertMultilinesEquals(
                "┌────────────────────────────────────────────────────┐\n" +
                "│ Geographic identifier:       A location            │\n" +
                "│ West bound:                    388,719 m           │\n" +
                "│ Representative value:          500,000 m           │\n" +
                "│ East bound:                    611,281 m           │\n" +
                "│ South bound:                  -221,095 m           │\n" +
                "│ Representative value:        1,549,056 m           │\n" +
                "│ North bound:                 3,319,207 m           │\n" +
                "│ Coordinate reference system: WGS 84 / UTM zone 32N │\n" +
                "└────────────────────────────────────────────────────┘\n", format.format(loc));
    }

    /**
     * Tests formatting of an instance having geographic and projected coordinates.
     */
    @Test
    @DependsOnMethod({"testGeographic", "testProjected"})
    public void testGeographicAndProjected() {
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.universal(14, 9);
        final SimpleLocation.Projected loc = new SimpleLocation.Projected(null, "A location") {
            @Override public CoordinateReferenceSystem getCoordinateReferenceSystem() {return crs;}
        };
        loc.minX =  388719.35;
        loc.maxX =  611280.65;
        loc.minY = -221094.87;
        loc.maxY = 3319206.22;
        loc.westBoundLongitude =  8;
        loc.eastBoundLongitude = 10;
        loc.southBoundLatitude = -2;
        loc.northBoundLatitude = 30;
        final LocationFormat format = new LocationFormat(Locale.US, null);
        assertMultilinesEquals(
                "┌─────────────────────────────────────────────────────────────┐\n" +
                "│ Geographic identifier:       A location                     │\n" +
                "│ West bound:                    388,719 m    —     8°00′00″E │\n" +
                "│ Representative value:          500,000 m    —     9°00′00″E │\n" +
                "│ East bound:                    611,281 m    —    10°00′00″E │\n" +
                "│ South bound:                  -221,095 m    —     2°00′00″S │\n" +
                "│ Representative value:        1,549,056 m    —    14°00′43″N │\n" +
                "│ North bound:                 3,319,207 m    —    30°00′00″N │\n" +
                "│ Coordinate reference system: WGS 84 / UTM zone 32N          │\n" +
                "└─────────────────────────────────────────────────────────────┘\n", format.format(loc));
    }
}
