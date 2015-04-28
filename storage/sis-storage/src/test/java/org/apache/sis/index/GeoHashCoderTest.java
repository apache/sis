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

import java.text.ParseException;
import org.opengis.geometry.DirectPosition;
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
        final String name;
        final double longitude;
        final double latitude;
        final String geohash;

        Place(final String name, final double longitude, final double latitude, final String geohash) {
            this.name      = name;
            this.latitude  = latitude;
            this.longitude = longitude;
            this.geohash   = geohash;
        }
    }

    /**
     * A list o places with their expected geohash.
     */
    private static Place[] PLACES = new Place[] {
        new Place("Empire State Building",  -73.985656, 40.748433, "dr5ru6j2c62q"),
        new Place("Statue Of Liberty",      -74.044444, 40.689167, "dr5r7p4rx6kz"),
        new Place("The White House",        -77.036550, 38.897669, "dqcjqcpeq70c"),
        new Place("Hoover Dam",            -114.737778, 36.015556, "9qqkvh6mzfpz"),
        new Place("Golden Gate Bridge",    -122.478611, 37.819722, "9q8zhuvgce0m"),
        new Place("Mount Rushmore",        -103.459825, 43.878947, "9xy3teyv7ke4"),
        new Place("Space Needle",          -122.349100, 47.620400, "c22yzvh0gmfy")
    };

    /**
     * Tests the {@link GeoHashCoder#encode(double, double)} method.
     */
    @Test
    public void testEncode() {
        final GeoHashCoder coder = new GeoHashCoder();
        for (final Place place : PLACES) {
            assertEquals(place.name, place.geohash, coder.encode(place.longitude, place.latitude));
        }
    }

    /**
     * Tests the {@link GeoHashCoder#decode(String)} method.
     *
     * @throws ParseException Should never happen.
     */
    @Test
    public void testDecode() throws ParseException {
        final GeoHashCoder coder = new GeoHashCoder();
        for (final Place place : PLACES) {
            final DirectPosition result = coder.decode(place.geohash);
            assertEquals(place.name, place.longitude, result.getOrdinate(0), TOLERANCE);
            assertEquals(place.name, place.latitude,  result.getOrdinate(1), TOLERANCE);
        }
    }
}
