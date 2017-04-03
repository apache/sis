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
import org.opengis.referencing.operation.TransformException;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.referencing.gazetteer.Location;
import org.opengis.referencing.gazetteer.LocationType;


/**
 * Tests methods from the {@link GeohashReferenceSystem} class.
 *
 * @author  Ross Laidlaw
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.1
 * @module
 */
@DependsOn(ReferencingByIdentifiersTest.class)
public final strictfp class GeohashReferenceSystemTest extends TestCase {
    /**
     * Tolerance factor for floating point comparison.
     */
    private static final double TOLERANCE = 0.000001;

    /**
     * Returns a reference system instance to test.
     */
    private static GeohashReferenceSystem instance() throws GazetteerException {
        return new GeohashReferenceSystem(GeohashReferenceSystem.Format.BASE32, CommonCRS.defaultGeographic());
    }

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
    private static final Place[] PLACES = new Place[] {
        new Place("Empire State Building",  -73.985656, 40.748433, "dr5ru6j2c62q"),
        new Place("Statue Of Liberty",      -74.044444, 40.689167, "dr5r7p4rx6kz"),
        new Place("The White House",        -77.036550, 38.897669, "dqcjqcpeq70c"),
        new Place("Hoover Dam",            -114.737778, 36.015556, "9qqkvh6mzfpz"),
        new Place("Golden Gate Bridge",    -122.478611, 37.819722, "9q8zhuvgce0m"),
        new Place("Mount Rushmore",        -103.459825, 43.878947, "9xy3teyv7ke4"),
        new Place("Space Needle",          -122.349100, 47.620400, "c22yzvh0gmfy")
    };

    /**
     * Tests the {@link GeohashReferenceSystem.Coder#encode(double, double)} method.
     *
     * @throws TransformException if an exception occurred while formatting the geohash.
     */
    @Test
    public void testEncode() throws TransformException {
        final GeohashReferenceSystem.Coder coder = instance().createCoder();
        for (final Place place : PLACES) {
            assertEquals(place.name, place.geohash, coder.encode(place.latitude, place.longitude));
        }
    }

    /**
     * Tests the {@link GeohashReferenceSystem.Coder#encode(DirectPosition)} method.
     *
     * @throws TransformException if an exception occurred while formatting the geohash.
     */
    @Test
    @DependsOnMethod("testEncode")
    public void testEncodePosition() throws TransformException {
        final GeohashReferenceSystem.Coder coder = instance().createCoder();
        final DirectPosition2D position = new DirectPosition2D(CommonCRS.WGS84.geographic());
        for (final Place place : PLACES) {
            position.x = place.latitude;
            position.y = place.longitude;
            assertEquals(place.name, place.geohash, coder.encode(position));
        }
    }

    /**
     * Tests the {@link GeohashReferenceSystem.Coder#decode(CharSequence)} method.
     *
     * @throws TransformException if an exception occurred while parsing the geohash.
     */
    @Test
    public void testDecode() throws TransformException {
        testDecode(instance().createCoder(), 0, 1);
    }

    /**
     * Tests the {@link GeohashReferenceSystem.Coder#decode(CharSequence)} method
     * to a different target CRS than the default one.
     *
     * @throws TransformException if an exception occurred while parsing the geohash.
     */
    @Test
    @DependsOnMethod("testDecode")
    public void testDecodeToCRS() throws TransformException {
        testDecode(new GeohashReferenceSystem(GeohashReferenceSystem.Format.BASE32,
                        CommonCRS.WGS84.geographic()).createCoder(), 1, 0);
    }

    /**
     * Implementation of {@link #testDecode()} and {@link #testDecodeToCRS()}.
     */
    private void testDecode(final GeohashReferenceSystem.Coder coder, final int λi, final int φi) throws TransformException {
        for (final Place place : PLACES) {
            final Location location = coder.decode(place.geohash);
            final DirectPosition result = location.getPosition().getDirectPosition();
            assertEquals(place.name, place.longitude, result.getOrdinate(λi), TOLERANCE);
            assertEquals(place.name, place.latitude,  result.getOrdinate(φi), TOLERANCE);
        }
    }

    /**
     * Verifies the metadata.
     *
     * @throws GazetteerException if an error occurred while creating the instance.
     */
    @Test
    public void verifyMetadata() throws GazetteerException {
        final GeohashReferenceSystem rs = instance();
        assertEquals("theme", "Mapping",      rs.getTheme().toString(Locale.ENGLISH));
        assertEquals("theme", "Cartographie", rs.getTheme().toString(Locale.FRENCH));

        final LocationType type = TestUtilities.getSingleton(rs.getLocationTypes());
        assertEquals("type", "Geohash", type.getName().toString(Locale.ENGLISH));
        assertEquals("parent",   0, type.getParents().size());
        assertEquals("children", 0, type.getChildren().size());
    }
}
