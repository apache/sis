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
package org.apache.sis.index;

import java.util.Map;
import java.util.HashMap;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests methods from the {@link GeoHashCoder} class.
 *
 * @author  Ross Laidlaw
 * @since   0.1
 * @version 0.3
 * @module
 */
public final strictfp class GeoHashCoderTest extends TestCase {
    /**
     * Tolerance factor for floating point comparison.
     */
    private static final double TOLERANCE = 0.000001;

    /**
     * A geographic coordinates together with the expected geohash.
     */
    private static final class Place {
        final double latitude;
        final double longitude;
        final String geohash;

        Place(final double latitude, final double longitude, final String geohash) {
            this.latitude  = latitude;
            this.longitude = longitude;
            this.geohash   = geohash;
        }
    }

    /**
     * Returns a map of places with their expected geohash.
     */
    private Map<String, Place> places() {
        final Map<String, Place> places = new HashMap<>(12);
        places.put("Empire State Building", new Place(40.748433,  -73.985656, "dr5ru6j2c62q"));
        places.put("Statue Of Liberty",     new Place(40.689167,  -74.044444, "dr5r7p4rx6kz"));
        places.put("The White House",       new Place(38.897669,  -77.036550, "dqcjqcpeq70c"));
        places.put("Hoover Dam",            new Place(36.015556, -114.737778, "9qqkvh6mzfpz"));
        places.put("Golden Gate Bridge",    new Place(37.819722, -122.478611, "9q8zhuvgce0m"));
        places.put("Mount Rushmore",        new Place(43.878947, -103.459825, "9xy3teyv7ke4"));
        places.put("Space Needle",          new Place(47.620400, -122.349100, "c22yzvh0gmfy"));
        return places;
    }

    /**
     * Tests the {@link GeoHashCoder#encode(double, double)} method.
     */
    @Test
    public void testEncode() {
        final GeoHashCoder coder = new GeoHashCoder();
        for (final Map.Entry<String, Place> entry : places().entrySet()) {
            final Place place = entry.getValue();
            assertEquals(entry.getKey(), place.geohash,
                    coder.encode(place.latitude, place.longitude));
        }
    }

    /**
     * Tests the {@link GeoHashCoder#decode(String)} method.
     */
    @Test
    public void testDecode() {
        final GeoHashCoder coder = new GeoHashCoder();
        for (final Map.Entry<String, Place> entry : places().entrySet()) {
            final String name = entry.getKey();
            final Place place = entry.getValue();
            final double[] result = coder.decode(place.geohash);
            assertEquals(name, place.latitude,  result[0], TOLERANCE);
            assertEquals(name, place.longitude, result[1], TOLERANCE);
        }
    }
}
