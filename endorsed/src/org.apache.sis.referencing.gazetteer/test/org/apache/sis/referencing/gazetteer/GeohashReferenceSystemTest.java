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
import javax.measure.Quantity;
import javax.measure.IncommensurableException;
import javax.measure.quantity.Length;
import org.opengis.referencing.operation.TransformException;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.test.TestCase;


/**
 * Tests methods from the {@link GeohashReferenceSystem} class.
 *
 * @author  Ross Laidlaw
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class GeohashReferenceSystemTest extends TestCase {
    /**
     * Tolerance factor for floating point comparison.
     */
    private static final double TOLERANCE = 0.000001;

    /**
     * WGS84 semi-major axis length divided by semi-minor axis length.
     * This is used for estimating how the precision changes when moving
     * from equator to a pole.
     */
    private static final double B_A = 6356752.314245179 / 6378137;

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
     * Creates a new test case.
     */
    public GeohashReferenceSystemTest() {
    }

    /**
     * Tests the {@link GeohashReferenceSystem.Coder#getPrecision(DirectPosition)} method.
     * Values published in Wikipedia are used as references, with more precision digits
     * added from SIS computation.
     *
     * @throws TransformException if an exception occurred while initializing the reference system.
     */
    @Test
    public void testGetPrecision() throws TransformException {
        final GeohashReferenceSystem.Coder coder = instance().createCoder();
        verifyGetPrecision(coder, 1, 2504689,    0.5);
        verifyGetPrecision(coder, 2,  626172,    0.5);
        verifyGetPrecision(coder, 3,   78272,    0.5);
        verifyGetPrecision(coder, 4,   19568,    0.5);
        verifyGetPrecision(coder, 5,    2446,    0.5);
        verifyGetPrecision(coder, 6,     611.5,  0.05);
        verifyGetPrecision(coder, 7,      76.44, 0.005);
        verifyGetPrecision(coder, 8,      19.11, 0.005);
    }

    /**
     * A single test case of {@link #testGetPrecision()} for the given hash string length.
     */
    private static void verifyGetPrecision(final GeohashReferenceSystem.Coder coder,
            final int length, final double expected, final double tolerance)
    {
        coder.setHashLength(length);
        Quantity<Length> worstCase = coder.getPrecision(null);
        Quantity<Length> atEquator = coder.getPrecision(new DirectPosition2D(0,  0));
        Quantity<Length> atPole    = coder.getPrecision(new DirectPosition2D(0, 90));
        Quantity<Length> somewhere = coder.getPrecision(new DirectPosition2D(0, 40));
        assertEquals(Units.METRE, worstCase.getUnit());
        assertEquals(Units.METRE, atEquator.getUnit());
        assertEquals(Units.METRE, atPole   .getUnit());
        assertEquals(Units.METRE, somewhere.getUnit());
        assertEquals(expected, worstCase.getValue().doubleValue(), tolerance);
        assertEquals(expected, atEquator.getValue().doubleValue(), tolerance);
        /*
         * If the length is even, then longitude values have one more bit than latitudes,
         * which compensate for the fact that the range of longitude values is twice the
         * range of latitude values. Consequently, both coordinate values should have the
         * same precision at equator, and moving to the pole changes only the radius.
         * Otherwise longitude error is twice larger than latitude error. At the pole,
         * the longitude error vanishes and only the latitude error matter.
         */
        final double f = (length & 1) != 0 ? B_A : B_A/2;
        assertEquals(f * expected, atPole.getValue().doubleValue(), tolerance);
        /*
         * Value should be somewhere between the two extrems. We use a simple average for this test.
         */
        final double estimate = (atEquator.getValue().doubleValue() + atPole.getValue().doubleValue()) / 2;
        assertEquals(estimate, somewhere.getValue().doubleValue(), estimate / 25);
    }

    /**
     * Tests the {@link GeohashReferenceSystem.Coder#setPrecision(Quantity, DirectPosition)} method.
     * Values used as a reference are the same as {@link #testGetPrecision()}.
     *
     * @throws TransformException if an exception occurred while initializing the reference system.
     * @throws IncommensurableException if a precision uses incompatible units of measurement.
     */
    @Test
    public void testSetPrecision() throws TransformException, IncommensurableException {
        final GeohashReferenceSystem.Coder coder = instance().createCoder();
        verifySetPrecision(coder, 1, 2504689);
        verifySetPrecision(coder, 2,  626172);
        verifySetPrecision(coder, 3,   78272);
        verifySetPrecision(coder, 4,   19568);
        verifySetPrecision(coder, 5,    2446);
        verifySetPrecision(coder, 6,     611.5);
        verifySetPrecision(coder, 7,      76.44);
        verifySetPrecision(coder, 8,      19.11);
    }

    /**
     * Verifies the value computed by {@link GeohashReferenceSystem.Coder#getPrecision()}
     * for the given hash string length.
     */
    private static void verifySetPrecision(final GeohashReferenceSystem.Coder coder,
            final int length, final double precision) throws IncommensurableException
    {
        final Length atEquator = Quantities.create(precision,     Units.METRE);
        final Length atPole    = Quantities.create(precision*B_A, Units.METRE);
        coder.setPrecision(atEquator, null);
        assertEquals(length, coder.getHashLength());
        coder.setPrecision(atEquator, new DirectPosition2D(0, 0));
        assertEquals(length, coder.getHashLength());
        coder.setPrecision(atPole, new DirectPosition2D(0, 90));
        assertEquals(length, coder.getHashLength());
        /*
         * Request a slightly finer precision at equator.
         * It requires a longer hash code, except for the 2 first cases.
         */
        coder.setPrecision(atPole, null);
        assertEquals(length < 3 ? length : length+1, coder.getHashLength());
    }

    /**
     * Tests the {@link GeohashReferenceSystem.Coder#encode(double, double)} method.
     *
     * @throws TransformException if an exception occurred while formatting the geohash.
     */
    @Test
    public void testEncode() throws TransformException {
        final GeohashReferenceSystem.Coder coder = instance().createCoder();
        for (final Place place : PLACES) {
            assertEquals(place.geohash, coder.encode(place.latitude, place.longitude), place.name);
        }
    }

    /**
     * Tests the {@link GeohashReferenceSystem.Coder#encode(DirectPosition)} method.
     *
     * @throws TransformException if an exception occurred while formatting the geohash.
     */
    @Test
    public void testEncodePosition() throws TransformException {
        final GeohashReferenceSystem.Coder coder = instance().createCoder();
        final DirectPosition2D position = new DirectPosition2D(CommonCRS.WGS84.geographic());
        for (final Place place : PLACES) {
            position.x = place.latitude;
            position.y = place.longitude;
            assertEquals(place.geohash, coder.encode(position), place.name);
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
    public void testDecodeToCRS() throws TransformException {
        testDecode(new GeohashReferenceSystem(GeohashReferenceSystem.Format.BASE32,
                   CommonCRS.WGS84.geographic()).createCoder(), 1, 0);
    }

    /**
     * Implementation of {@link #testDecode()} and {@link #testDecodeToCRS()}.
     */
    private void testDecode(final GeohashReferenceSystem.Coder coder, final int λi, final int φi) throws TransformException {
        for (final Place place : PLACES) {
            final AbstractLocation location = coder.decode(place.geohash);
            final DirectPosition result = location.getPosition();
            assertEquals(place.longitude, result.getOrdinate(λi), TOLERANCE, place.name);
            assertEquals(place.latitude,  result.getOrdinate(φi), TOLERANCE, place.name);
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
        assertEquals("Mapping",      rs.getTheme().toString(Locale.ENGLISH));
        assertEquals("Cartographie", rs.getTheme().toString(Locale.FRENCH));

        final AbstractLocationType type = assertSingleton(rs.getLocationTypes());
        assertEquals("Geohash", type.getName().toString(Locale.ENGLISH));
        assertEquals(0, type.getParents().size());
        assertEquals(0, type.getChildren().size());
    }
}
