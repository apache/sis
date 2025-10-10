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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Iterator;
import java.util.Collections;
import java.lang.reflect.Field;
import javax.measure.IncommensurableException;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.provider.TransverseMercator;
import org.apache.sis.measure.Quantities;
import static org.apache.sis.metadata.internal.shared.ReferencingServices.NAUTICAL_MILE;
import static org.apache.sis.measure.Units.ARC_MINUTE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.gazetteer.Location;
import org.opengis.referencing.gazetteer.LocationType;


/**
 * Tests {@link MilitaryGridReferenceSystem}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class MilitaryGridReferenceSystemTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MilitaryGridReferenceSystemTest() {
    }

    /**
     * Verifies the metadata.
     */
    @Test
    public void verifyMetadata() {
        final MilitaryGridReferenceSystem rs = new MilitaryGridReferenceSystem();
        assertEquals("Mapping",      rs.getTheme().toString(Locale.ENGLISH));
        assertEquals("Cartographie", rs.getTheme().toString(Locale.FRENCH));

        final LocationType gzd = assertSingleton(rs.getLocationTypes());
        assertEquals("Grid zone designator", gzd.getName().toString(Locale.ENGLISH));
        assertEquals(0, gzd.getParents().size());

        final LocationType sid = assertSingleton(gzd.getChildren());
        assertEquals("100 km square identifier", sid.getName().toString(Locale.ENGLISH));
        assertSame(gzd, assertSingleton(sid.getParents()));

        final LocationType gc = assertSingleton(sid.getChildren());
        assertEquals("Grid coordinate", gc.getName().toString(Locale.ENGLISH));
        assertSame(sid, assertSingleton(gc.getParents()));
    }

    /**
     * Verifies relationship between static fields documented in {@link MilitaryGridReferenceSystem}.
     */
    @Test
    public void verifyInvariants() {
        assertEquals(StrictMath.pow(10, MilitaryGridReferenceSystem.METRE_PRECISION_DIGITS),
                                        MilitaryGridReferenceSystem.GRID_SQUARE_SIZE);
    }

    /**
     * Tests {@link MilitaryGridReferenceSystem.Encoder#latitudeBand(double)}.
     */
    @Test
    public void testLatitudeBand() {
        assertEquals('C', MilitaryGridReferenceSystem.Encoder.latitudeBand(-80), "80°S");
        assertEquals('T', MilitaryGridReferenceSystem.Encoder.latitudeBand( 45), "45°N");
        assertEquals('U', MilitaryGridReferenceSystem.Encoder.latitudeBand( 55), "55°N");
        assertEquals('V', MilitaryGridReferenceSystem.Encoder.latitudeBand( 56), "56°N");
        assertEquals('V', MilitaryGridReferenceSystem.Encoder.latitudeBand( 63), "63°N");
        assertEquals('W', MilitaryGridReferenceSystem.Encoder.latitudeBand( 64), "64°N");
        assertEquals('W', MilitaryGridReferenceSystem.Encoder.latitudeBand( 71), "71°N");
        assertEquals('X', MilitaryGridReferenceSystem.Encoder.latitudeBand( 72), "72°N");
        assertEquals('X', MilitaryGridReferenceSystem.Encoder.latitudeBand( 84), "84°N");
    }

    /**
     * Verifies that {@link TransverseMercator.Zoner#isNorway(double)} and
     * {@link TransverseMercator.Zoner#isSvalbard(double)} are consistent with the latitude bands.
     */
    @Test
    public void verifyZonerConsistency() {
        for (double φ = TransverseMercator.Zoner.SOUTH_BOUNDS; φ < TransverseMercator.Zoner.NORTH_BOUNDS; φ++) {
            final String latitude = String.valueOf(φ);
            final char band = MilitaryGridReferenceSystem.Encoder.latitudeBand(φ);
            assertTrue  (band >= 'C' && band <= 'X', latitude);
            assertEquals(band == 'V', TransverseMercator.Zoner.isNorway(φ), latitude);
            assertEquals(band == 'X', TransverseMercator.Zoner.isSvalbard(φ), latitude);
        }
    }

    /**
     * Verifies the hard-coded tables used for decoding purpose.
     * This method performs its computation using UTM zone 31, which is the zone from 0° to 6°E.
     * The results should be the same for all other zones.
     *
     * @throws TransformException if an error occurred while projecting a geographic coordinate.
     * @throws ReflectiveOperationException if this test method cannot access the table to verify.
     */
    @Test
    public void verifyDecoderTables() throws TransformException, ReflectiveOperationException {
        final int              numBands   = 20;
        final double           zoneCentre = 3;
        final double           zoneBorder = 0;
        final CommonCRS        datum      = CommonCRS.WGS84;
        final DirectPosition2D geographic = new DirectPosition2D();
        final DirectPosition2D projected  = new DirectPosition2D();
        final MathTransform    northMT    = datum.universal(+1, zoneCentre).getConversionFromBase().getMathTransform();
        final MathTransform    southMT    = datum.universal(-1, zoneCentre).getConversionFromBase().getMathTransform();
        final int[] table;
        {
            // Use reflection for keeping MilitaryGridReferenceSystem.Decoder.ROW_RESOLVER private.
            final Field f = MilitaryGridReferenceSystem.Decoder.class.getDeclaredField("ROW_RESOLVER");
            f.setAccessible(true);
            table = (int[]) f.get(null);
            assertEquals(numBands, table.length, "ROW_RESOLVER.length");
        }
        for (int band = 0; band < numBands; band++) {
            final double φ = band * MilitaryGridReferenceSystem.LATITUDE_BAND_HEIGHT + TransverseMercator.Zoner.SOUTH_BOUNDS;
            final boolean isSouth = (φ < 0);
            final MathTransform projection = (isSouth) ? southMT : northMT;
            /*
             * Computes the smallest possible northing value. In the North hemisphere, this is the value
             * on the central meridian because northing values tends toward the poles as we increase the
             * distance from that centre.  In the South hemisphere, this is the value on the zone border
             * where we have the maximal distance from the centre.
             */
            geographic.x = φ;
            geographic.y = isSouth ? zoneBorder : zoneCentre;
            final double ymin = projection.transform(geographic, projected).getCoordinate(1);
            /*
             * Computes the largest possible northing value. This is not only the value of the next latitude band;
             * we also need to interchange the "zone centre" and "zone border" logic described in previous comment.
             * The result is that we will have some overlap in the northing value of consecutive latitude bands.
             */
            geographic.y = isSouth ? zoneCentre : zoneBorder;
            geographic.x = MilitaryGridReferenceSystem.Decoder.upperBound(φ);
            final double ymax = projection.transform(geographic, projected).getCoordinate(1);
            /*
             * Computes the value that we will encode in the MilitaryGridReferenceSystem.Decoder.ROW_RESOLVER table.
             * The lowest 4 bits are the number of the row cycle (a cycle of 2000 km). The remaining bits tell which
             * rows are valid in that latitude band.
             */
            final int rowCycle = (int) StrictMath.floor(ymin / (MilitaryGridReferenceSystem.GRID_SQUARE_SIZE * MilitaryGridReferenceSystem.GRID_ROW_COUNT));
            final int lowerRow = (int) StrictMath.floor(ymin /  MilitaryGridReferenceSystem.GRID_SQUARE_SIZE);    // Inclusive
            final int upperRow = (int) StrictMath.ceil (ymax /  MilitaryGridReferenceSystem.GRID_SQUARE_SIZE);    // Exclusive
            assertTrue(rowCycle >= 0 && rowCycle <= MilitaryGridReferenceSystem.Decoder.NORTHING_BITS_MASK);
            assertTrue(lowerRow >= 0);
            assertTrue(upperRow >= 0);
            int validRows = 0;
            for (int i = lowerRow; i < upperRow; i++) {
                validRows |= 1 << (i % MilitaryGridReferenceSystem.GRID_ROW_COUNT);
            }
            final int bits = (validRows << MilitaryGridReferenceSystem.Decoder.NORTHING_BITS_COUNT) | rowCycle;
            /*
             * Verification. If it fails, format the line of code that should be inserted
             * in the MilitaryGridReferenceSystem.Decoder.ROW_RESOLVER table definition.
             */
            if (table[band] != bits) {
                String bitMasks = Integer.toBinaryString(validRows);
                bitMasks = "00000000000000000000".substring(bitMasks.length()).concat(bitMasks);
                fail(String.format("ROW_RESOLVER[%d]: expected %d but got %d. Below is suggested line of code:%n"
                        + "/* Latitude band %c (from %3.0f°) */   %d  |  0b%s_0000%n", band, bits, table[band],
                        MilitaryGridReferenceSystem.Encoder.latitudeBand(φ), φ, rowCycle, bitMasks));
            }
        }
    }

    /**
     * Returns a coder instance to test.
     */
    private static MilitaryGridReferenceSystem.Coder coder() {
        return new MilitaryGridReferenceSystem().createCoder();
    }

    /**
     * Decodes the given reference and returns its direct position.
     */
    private static DirectPosition decode(final MilitaryGridReferenceSystem.Coder coder, final String reference)
            throws TransformException
    {
        final Location loc = coder.decode(reference);
        final Envelope2D envelope = new Envelope2D(loc.getEnvelope());
        final DirectPosition2D pos = new DirectPosition2D(loc.getPosition());
        assertTrue(envelope.contains(pos), reference);
        return pos;
    }

    /**
     * Tests encoding of various coordinates in Universal Transverse Mercator (UTM) projection.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    public void testEncodeUTM() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        final DirectPosition2D position = new DirectPosition2D();
        /*
         * 41°N 10°E (UTM zone 32)
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.universal(41, 10));
        position.x =  584102;
        position.y = 4539239;
        assertEquals("32TNL8410239239", coder.encode(position));
        /*
         * 82°N 10°W (UTM zone 29) — should instantiate a new MilitaryGridReferenceSystem.Encoder.
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.universal(82, -10));
        position.x =  484463;
        position.y = 9104963;
        assertEquals("29XMM8446304963", coder.encode(position));
        /*
         * 41°S 10°E (UTM zone 32) — should reuse the Encoder created in first test.
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.universal(-41, 10));
        position.x =  584102;
        position.y = 5460761;
        assertEquals("32GNV8410260761", coder.encode(position));
        /*
         * 82°N 10°E (UTM zone 32) — in this special case, zone 32 is replaced by zone 33.
         * Call to WGS84.UTM(φ,λ) needs to specify a smaller latitude for getting zone 32.
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.universal(40, 10));
        position.x =  515537;
        position.y = 9104963;
        assertEquals("33XVM2240708183", coder.encode(position));
        /*
         * Same position as previously tested, but using geographic coordinates.
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.geographic());
        position.x = 82;
        position.y = 10;
        assertEquals("33XVM2240608183", coder.encode(position));
        position.x = -41;
        position.y = 10;
        assertEquals("32GNV8410260761", coder.encode(position));
    }

    /**
     * Tests decoding of various coordinates in Universal Transverse Mercator (UTM) projection,
     * all of them at metric precision.
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    public void testDecodeUTM() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        DirectPosition position;

        position = decode(coder, "32TNL8410239239");
        assertSame(CommonCRS.WGS84.universal(41, 10), position.getCoordinateReferenceSystem());
        assertEquals( 584102.5, position.getCoordinate(0));
        assertEquals(4539239.5, position.getCoordinate(1));

        position = decode(coder, "29XMM8446304963");
        assertSame(CommonCRS.WGS84.universal(82, -10), position.getCoordinateReferenceSystem());
        assertEquals( 484463.5, position.getCoordinate(0));
        assertEquals(9104963.5, position.getCoordinate(1));

        position = decode(coder, "32GNV8410260761");
        assertSame(CommonCRS.WGS84.universal(-41, 10), position.getCoordinateReferenceSystem());
        assertEquals( 584102.5, position.getCoordinate(0));
        assertEquals(5460761.5, position.getCoordinate(1));

        position = decode(coder, "33XVM2240708183");
        assertSame(CommonCRS.WGS84.universal(82, 10), position.getCoordinateReferenceSystem());
        assertEquals( 422407.5, position.getCoordinate(0));
        assertEquals(9108183.5, position.getCoordinate(1));

        position = decode(coder, "32FNL9360826322");
        assertSame(CommonCRS.WGS84.universal(-49.4, 10.3), position.getCoordinateReferenceSystem());
        assertEquals( 593608.5, position.getCoordinate(0));
        assertEquals(4526322.5, position.getCoordinate(1));
    }

    /**
     * Tests decoding of values that are close to a change of zones.
     * Values tested in this methods were used to cause a {@link ReferenceVerifyException}
     * before we debugged the verification algorithm in the {@code decode(…)} method.
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    public void testDecodeLimitCases() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        DirectPosition position;
        ProjectedCRS crs;
        /*
         * Cell on the West border of a UTM zone in the South hemisphere.
         * Easting value before clipping: 250000
         * Easting value after  clipping: 251256
         */
        coder.setClipToValidArea(false);
        position = decode(coder, "19JBK");                                            // South hemisphere
        crs = CommonCRS.WGS84.universal(-10, -69);
        assertSame(crs, position.getCoordinateReferenceSystem());
        assertEquals( 250000, position.getCoordinate(0), 1);
        assertEquals(6950000, position.getCoordinate(1));

        coder.setClipToValidArea(true);
        position = decode(coder, "19JBK");
        assertSame(crs, position.getCoordinateReferenceSystem());
        assertEquals( 251256, position.getCoordinate(0), 1);
        assertEquals(6950000, position.getCoordinate(1));
        /*
         * Easting range before clipping is [300000 … 400000] metres.
         * The east bound become 343828 metres after clipping.
         * Easting value before clipping: 350000
         * Easting value after  clipping: 371914
         */
        coder.setClipToValidArea(false);
        position = decode(coder, "1VCK");                                // North of Norway latitude band
        crs = CommonCRS.WGS84.universal(62, -180);
        assertSame(crs, position.getCoordinateReferenceSystem());
        assertEquals( 350000, position.getCoordinate(0), 1);
        assertEquals(6950000, position.getCoordinate(1));

        coder.setClipToValidArea(true);
        position = decode(coder, "1VCK");
        assertSame(crs, position.getCoordinateReferenceSystem());
        assertEquals( 371914, position.getCoordinate(0), 1);
        assertEquals(6950000, position.getCoordinate(1));
        /*
         * Northing value before clipping: 7350000
         * Northing value after  clipping: 7371306
         */
        coder.setClipToValidArea(false);
        position = decode(coder, "57KTP");
        crs = CommonCRS.WGS84.universal(-24, 156);
        assertSame(crs, position.getCoordinateReferenceSystem());
        assertEquals( 250000, position.getCoordinate(0));
        assertEquals(7350000, position.getCoordinate(1), 1);

        coder.setClipToValidArea(true);
        position = decode(coder, "57KTP");
        assertSame(crs, position.getCoordinateReferenceSystem());
        assertEquals( 250000, position.getCoordinate(0));
        assertEquals(7371306, position.getCoordinate(1), 1);
        /*
         * Easting and northing values before clipping:  650000   6250000
         * Easting and northing values after  clipping:  643536   6253618
         */
        coder.setClipToValidArea(false);
        position = decode(coder, "56VPH");
        crs = CommonCRS.WGS84.universal(55, 154);
        assertSame(crs, position.getCoordinateReferenceSystem());
        assertEquals( 650000, position.getCoordinate(0), 1);
        assertEquals(6250000, position.getCoordinate(1), 1);

        coder.setClipToValidArea(true);
        position = decode(coder, "56VPH");
        assertSame(crs, position.getCoordinateReferenceSystem());
        assertEquals( 643536, position.getCoordinate(0), 1);
        assertEquals(6253618, position.getCoordinate(1), 1);
    }

    /**
     * Tests encoding of various coordinates in Universal Polar Stereographic (UPS) projection.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    public void testEncodeUPS() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        final DirectPosition2D position = new DirectPosition2D();
        /*
         * South case.
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.universal(-90, 0));
        position.x = 2000010;
        position.y = 2000010;
        assertEquals("BAN0001000010", coder.encode(position));
        position.x = 1999990;
        position.y = 1999990;
        assertEquals("AZM9999099990", coder.encode(position));
        position.x = 2806727;
        position.y = 1602814;
        assertEquals("BLJ0672702814", coder.encode(position));
        /*
         * North case.
         */
        position.setCoordinateReferenceSystem(CommonCRS.WGS84.universal(90, 0));
        position.x = 2000010;
        position.y = 2000010;
        assertEquals("ZAH0001000010", coder.encode(position));
        position.x = 1999990;
        position.y = 1999990;
        assertEquals("YZG9999099990", coder.encode(position));
        position.x = 1386727;
        position.y = 2202814;
        assertEquals("YRK8672702814", coder.encode(position));
    }

    /**
     * Tests decoding of various coordinates in Universal Polar Stereographic (UPS) projection,
     * all at the same resolution.
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    public void testDecodeUPS() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        DirectPosition position;
        /*
         * South case.
         */
        position = decode(coder, "BAN0001000010");
        assertSame(CommonCRS.WGS84.universal(-90, 0), position.getCoordinateReferenceSystem());
        assertEquals(2000010.5, position.getCoordinate(0));
        assertEquals(2000010.5, position.getCoordinate(1));

        position = decode(coder, "AZM9999099990");
        assertSame(CommonCRS.WGS84.universal(-90, 0), position.getCoordinateReferenceSystem());
        assertEquals(1999990.5, position.getCoordinate(0));
        assertEquals(1999990.5, position.getCoordinate(1));

        position = decode(coder, "BLJ0672702814");
        assertSame(CommonCRS.WGS84.universal(-90, 0), position.getCoordinateReferenceSystem());
        assertEquals(2806727.5, position.getCoordinate(0));
        assertEquals(1602814.5, position.getCoordinate(1));
        /*
         * North case.
         */
        position = decode(coder, "ZAH0001000010");
        assertSame(CommonCRS.WGS84.universal(90, 0), position.getCoordinateReferenceSystem());
        assertEquals(2000010.5, position.getCoordinate(0));
        assertEquals(2000010.5, position.getCoordinate(1));

        position = decode(coder, "YZG9999099990");
        assertSame(CommonCRS.WGS84.universal(90, 0), position.getCoordinateReferenceSystem());
        assertEquals(1999990.5, position.getCoordinate(0));
        assertEquals(1999990.5, position.getCoordinate(1));

        position = decode(coder, "YRK8672702814");
        assertSame(CommonCRS.WGS84.universal(90, 0), position.getCoordinateReferenceSystem());
        assertEquals(1386727.5, position.getCoordinate(0));
        assertEquals(2202814.5, position.getCoordinate(1));
    }

    /**
     * Tests encoding of the same coordinate at various precision.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    public void testPrecision() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        final DirectPosition2D position = new DirectPosition2D(CommonCRS.WGS84.universal(13, 103));
        position.x =  377299;
        position.y = 1483035;
        assertEquals(1, coder.getPrecision());
        assertEquals("48PUV7729983035", coder.encode(position));
        coder.setPrecision(10);
        assertEquals(10, coder.getPrecision());
        assertEquals("48PUV77298303", coder.encode(position));
        coder.setPrecision(304);
        assertEquals(100, coder.getPrecision());
        assertEquals("48PUV772830", coder.encode(position));
        coder.setPrecision(1002);
        assertEquals(1000, coder.getPrecision());
        assertEquals("48PUV7783", coder.encode(position));
        coder.setPrecision(10000);
        assertEquals(10000, coder.getPrecision());
        assertEquals("48PUV78", coder.encode(position));
        coder.setPrecision(990004);
        assertEquals(100000, coder.getPrecision());
        assertEquals("48PUV", coder.encode(position));
        coder.setPrecision(1000000);
        assertEquals(1000000, coder.getPrecision());
        assertEquals("48P", coder.encode(position));
    }

    /**
     * Tests encoding of the same coordinate at various precision specified as an angular value.
     * The encoder is expected to transform the angular value into a linear value.
     *
     * @throws IncommensurableException if the quantity type is not accepted.
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    public void testAngularPrecision() throws IncommensurableException, TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        final DirectPosition2D position = new DirectPosition2D(CommonCRS.WGS84.universal(13, 103));
        position.x =  377299;
        position.y = 1483035;
        coder.setPrecision(Quantities.create(1010 / NAUTICAL_MILE, ARC_MINUTE), null);
        assertEquals(1000, coder.getPrecision());
        assertEquals("48PUV7783", coder.encode(position));
        /*
         * Same value closer to a pole. It forces the encoder to use a finer precision,
         * because a degree of longitude represent a smaller distance.
         */
        coder.setPrecision(Quantities.create(1010 / NAUTICAL_MILE, ARC_MINUTE), position);
        assertEquals(100, coder.getPrecision());
        assertEquals("48PUV772830", coder.encode(position));
    }

    /**
     * Tests encoding of the same coordinate with various separators, mixed with various precisions.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    public void testSeparator() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        final DirectPosition2D position = new DirectPosition2D(CommonCRS.WGS84.universal(13, 103));
        position.x =  377299;
        position.y = 1483035;
        assertEquals("", coder.getSeparator());
        assertEquals("48PUV7729983035", coder.encode(position));

        coder.setSeparator(" ");
        assertEquals(" ", coder.getSeparator());
        assertEquals("48 P UV 77299 83035", coder.encode(position));

        coder.setSeparator("/");
        coder.setPrecision(100000);
        assertEquals("/", coder.getSeparator());
        assertEquals("48/P/UV", coder.encode(position));
    }

    /**
     * Tests decoding the same coordinates with different separators at different resolutions.
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    public void testDecodeVariants() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        coder.setSeparator(" / ");
        DirectPosition position;

        position = decode(coder, "32TNL8410239239");
        assertEquals(position, decode(coder, "32/T/NL/84102/39239"));
        assertEquals( 584102.5, position.getCoordinate(0));
        assertEquals(4539239.5, position.getCoordinate(1));

        position = decode(coder, "32TNL8439");
        assertEquals(position, decode(coder, "32/T/NL/84/39"));
        assertEquals( 584500.0, position.getCoordinate(0));
        assertEquals(4539500.0, position.getCoordinate(1));

        position = decode(coder, "32TNL83");
        assertEquals(position, decode(coder, "32/T/NL/8/3"));
        assertEquals( 585000.0, position.getCoordinate(0));
        assertEquals(4535000.0, position.getCoordinate(1));

        position = decode(coder, "32TNL");
        assertEquals(position, decode(coder, "32/T/NL"));
        assertEquals( 550000.0, position.getCoordinate(0));
        assertEquals(4550000.0, position.getCoordinate(1));

        position = decode(coder, "32T");
        assertEquals(position, decode(coder, "32/T"));
        assertEquals( 500000.0, position.getCoordinate(0));
        assertEquals(9000000.0, position.getCoordinate(1));
    }

    /**
     * Verifies the exceptions thrown when an invalid reference is given.
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    public void testErrorDetection() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        GazetteerException e;

        e = assertThrows(GazetteerException.class, () -> coder.decode("32TNL841023923"),
                         "Shall not accept numeric identifier of odd length.");
        assertMessageContains(e, "8410", "23923");

        e = assertThrows(GazetteerException.class, () -> coder.decode("32TN"),
                         "Shall not accept half of a grid zone designator.");
        assertMessageContains(e, "32TN");

        e = assertThrows(GazetteerException.class, () -> coder.decode("32SNL8410239239"),
                         "Shall report an invalid latitude band.");
        assertMessageContains(e, "32SNL8410239239", "32TNL");
    }

    /**
     * Encodes random coordinates, decodes them and verifies that the results are approximately equal
     * to the original coordinates.
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    public void verifyConsistency() throws TransformException {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final MilitaryGridReferenceSystem.Coder coder = coder();
        final DirectPosition2D expected = new DirectPosition2D();
        final DirectPosition2D position = new DirectPosition2D(CommonCRS.WGS84.geographic());
        for (int i=0; i<100; i++) {
            position.x = random.nextDouble() * 180 -  90;       // Latitude  (despite the 'x' field name)
            position.y = random.nextDouble() * 358 - 179;       // Longitude (despite the 'y' field name)
            final String reference = coder.encode(position);
            final DirectPosition r = decode(coder, reference);
            final ProjectedCRS crs = (ProjectedCRS) r.getCoordinateReferenceSystem();
            assertSame(expected, crs.getConversionFromBase().getMathTransform().transform(position, expected));
            final double distance = expected.distance(r.getCoordinate(0), r.getCoordinate(1));
            if (!(distance < 1.5)) {    // Use '!' for catching NaN.
                final String lineSeparator = System.lineSeparator();
                fail("Consistency check failed for φ = " + position.x + " and λ = " + position.y + lineSeparator
                   + "MGRS reference = " + reference + lineSeparator
                   + "Parsing result = " + r         + lineSeparator
                   + "Projected φ, λ = " + expected  + lineSeparator
                   + "Distance (m)   = " + distance  + lineSeparator);
            }
        }
    }

    /**
     * Tests iteration over all codes in a given area of interest. The geographic area used for this test is based on
     * <a href="https://www.ff-reichertshausen.de/cms/wp-content/uploads/2012/10/utmmeldegitter.jpg">this picture</a>
     * (checked on March 2017).
     *
     * <div class="note"><b>Tip:</b> in case of test failure, see {@link LocationViewer} as a debugging tool.</div>
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    public void testIteratorNorthUTM() throws TransformException {
        /*
         * Following is the list of MGRS references that we expect to find in the above area of interest.
         * The references are distributed in 3 zones (31, 32 and 33) and 3 latitude bands (T, U and V).
         * This test includes the Norway special case: between 56° and 64°N (latitude band V), zone 32
         * is widened to 9° at the expense of zone 31. The test needs to be insensitive to iteration order.
         */
        testIterator(new Envelope2D(CommonCRS.defaultGeographic(), 5, 47, 8, 10), Arrays.asList(
            "31TFN", "31TGN",    "32TKT", "32TLT", "32TMT", "32TNT", "32TPT", "32TQT",    "33TTN", "33TUN",
            "31TFP", "31TGP",    "32TKU", "32TLU", "32TMU", "32TNU", "32TPU", "32TQU",    "33TTP", "33TUP",
            "31UFP", "31UGP",    "32UKU", "32ULU", "32UMU", "32UNU", "32UPU", "32UQU",    "33UTP", "33UUP",
            "31UFQ", "31UGQ",    "32UKV", "32ULV", "32UMV", "32UNV", "32UPV", "32UQV",    "33UTQ", "33UUQ",
            "31UFR", "31UGR",    "32UKA", "32ULA", "32UMA", "32UNA", "32UPA", "32UQA",    "33UTR", "33UUR",
            "31UFS", "31UGS",    "32UKB", "32ULB", "32UMB", "32UNB", "32UPB", "32UQB",    "33UTS", "33UUS",
            "31UFT", "31UGT",    "32UKC", "32ULC", "32UMC", "32UNC", "32UPC", "32UQC",    "33UTT", "33UUT",
            "31UFU", "31UGU",    "32UKD", "32ULD", "32UMD", "32UND", "32UPD", "32UQD",    "33UTU", "33UUU",
            "31UFV", "31UGV",    "32UKE", "32ULE", "32UME", "32UNE", "32UPE", "32UQE",    "33UTV", "33UUV",
            "31UFA",                      "32ULF", "32UMF", "32UNF", "32UPF",             "33UUA",
            "31UFB",                      "32ULG", "32UMG", "32UNG", "32UPG",             "33UUB",
            "31UFC",                      "32ULH", "32UMH", "32UNH", "32UPH",             "33UUC",
            /* Norway case */    "32VKH", "32VLH", "32VMH", "32VNH", "32VPH",             "33VUC",
            /* Norway case */    "32VKJ", "32VLJ", "32VMJ", "32VNJ", "32VPJ",             "33VUD"));
    }

    /**
     * Tests iteration over codes in an area in South hemisphere.
     *
     * <div class="note"><b>Tip:</b> in case of test failure, see {@link LocationViewer} as a debugging tool.</div>
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    public void testIteratorSouthUTM() throws TransformException {
        testIterator(new Envelope2D(CommonCRS.defaultGeographic(), 5, -42, 8, 4), Arrays.asList(
            "31HFT", "31HGT", "32HKC", "32HLC", "32HMC", "32HNC", "32HPC", "32HQC", "33HTT", "33HUT",
            "31HFS", "31HGS", "32HKB", "32HLB", "32HMB", "32HNB", "32HPB", "32HQB", "33HTS", "33HUS",
            "31HFR", "31HGR", "32HKA", "32HLA", "32HMA", "32HNA", "32HPA", "32HQA", "33HTR", "33HUR",
            "31GFR", "31GGR", "32GKA", "32GLA", "32GMA", "32GNA", "32GPA", "32GQA", "33GTR", "33GUR",
            "31GFQ", "31GGQ", "32GKV", "32GLV", "32GMV", "32GNV", "32GPV", "32GQV", "33GTQ", "33GUQ",
            "31GFP", "31GGP", "32GKU", "32GLU", "32GMU", "32GNU", "32GPU", "32GQU", "33GTP", "33GUP"));
    }

    /**
     * Tests iteration crossing the anti-meridian.
     *
     * <div class="note"><b>Tip:</b> in case of test failure, see {@link LocationViewer} as a debugging tool.</div>
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    public void testIteratorOverAntiMeridian() throws TransformException {
        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(CommonCRS.defaultGeographic());
        areaOfInterest.setRange(0, 170, -175);
        areaOfInterest.setRange(1,  40,  42);
        testIterator(areaOfInterest, Arrays.asList(
            "59SME", "59SNE", "59SPE", "59SQE", "60STK", "60SUK", "60SVK", "60SWK", "60SXK", "60SYK", "1SBE", "1SCE", "1SDE", "1SEE", "1SFE",
            "59TME", "59TNE", "59TPE", "59TQE", "60TTK", "60TUK", "60TVK", "60TWK", "60TXK", "60TYK", "1TBE", "1TCE", "1TDE", "1TEE", "1TFE",
            "59TMF", "59TNF", "59TPF", "59TQF", "60TTL", "60TUL", "60TVL", "60TWL", "60TXL", "60TYL", "1TBF", "1TCF", "1TDF", "1TEF", "1TFF",
            "59TMG", "59TNG", "59TPG", "59TQG", "60TTM", "60TUM", "60TVM", "60TWM", "60TXM", "60TYM", "1TBG", "1TCG", "1TDG", "1TEG", "1TFG"));
    }

    /**
     * Tests iterating over part of North pole, in an area between 10°W to 70°E.
     * This area is restricted to the lower part of UPS projection, which allow
     * {@code IteratorAllZones} to simplify to a single {@code IteratorOneZone}.
     *
     * <div class="note"><b>Tip:</b> in case of test failure, see {@link LocationViewer} as a debugging tool.</div>
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    public void testIteratorNorthPole() throws TransformException {
        testIterator(new Envelope2D(CommonCRS.defaultGeographic(), -10, 85, 80, 5), Arrays.asList(
            "YZG", "ZAG", "ZBG", "ZCG",
            "YZF", "ZAF", "ZBF", "ZCF", "ZFF", "ZGF", "ZHF",
            "YZE", "ZAE", "ZBE", "ZCE", "ZFE", "ZGE", "ZHE",
            "YZD", "ZAD", "ZBD", "ZCD", "ZFD", "ZGD",
            "YZC", "ZAC", "ZBC", "ZCC", "ZFC",
            "YZB", "ZAB", "ZBB", "ZCB"));
    }

    /**
     * Tests iterating over part of South pole, both lower and upper parts of UPS projection
     * together with some UTM zones. This is a test mixing a bit of everything together.
     *
     * <div class="note"><b>Tip:</b> in case of test failure, see {@link LocationViewer} as a debugging tool.</div>
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    public void testIteratorSouthPole() throws TransformException {
        testIterator(new Envelope2D(CommonCRS.defaultGeographic(), -120, -83, 50, 5), Arrays.asList(
                   "AKR", "ALR", "APR",
                   "AKQ", "ALQ", "APQ", "AQQ",
            "AJP", "AKP", "ALP", "APP", "AQP",
            "AJN", "AKN", "ALN", "APN", "AQN",
            "AJM", "AKM", "ALM", "APM", "AQM",
            "AJL", "AKL", "ALL", "APL", "AQL",
                   "AKK", "ALK", "APK", "AQK",
                   "AKJ", "ALJ", "APJ", "AQJ", "ARJ",
                   "AKH", "ALH", "APH", "AQH", "ARH",
                          "ALG", "APG",

            "11CMP", "11CNP", "12CVU", "12CWU", "13CDP", "13CEP", "14CMU", "14CNU", "15CVP", "15CWP", "16CDU", "16CEU", "17CMP", "17CNP", "18CVU", "18CWU", "19CDP",
            "11CMN", "11CNN", "12CVT", "12CWT", "13CDN", "13CEN", "14CMT", "14CNT", "15CVN", "15CWN", "16CDT", "16CET", "17CMN", "17CNN", "18CVT", "18CWT", "19CDN",
            "11CMM", "11CNM", "12CVS", "12CWS", "13CDM", "13CEM", "14CMS", "14CNS", "15CVM", "15CWM", "16CDS", "16CES", "17CMM", "17CNM", "18CVS", "18CWS", "19CDM"));
    }

    /**
     * Implementation of {@code testIteratorXXX()} methods.
     */
    private static void testIterator(final Envelope areaOfInterest, final List<String> expected) throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        coder.setClipToValidArea(false);
        coder.setPrecision(100000);
        /*
         * Test sequential iteration using iterator.
         */
        final var remaining = new HashSet<String>(expected);
        assertEquals(expected.size(), remaining.size(), "List of expected codes has duplicated values.");
        for (final Iterator<String> it = coder.encode(areaOfInterest); it.hasNext();) {
            final String code = it.next();
            assertTrue(remaining.remove(code), code);
        }
        assertTrue(remaining.isEmpty(), remaining.toString());
        /*
         * Test parallel iteration using stream.
         */
        assertTrue(remaining.addAll(expected));
        final Set<String> sync = Collections.synchronizedSet(remaining);
        assertEquals(expected.size(), sync.size(), "List of expected codes has duplicated values.");
        coder.encode(areaOfInterest, true).forEach((code) -> assertTrue(sync.remove(code), code));
        assertTrue(sync.isEmpty(), sync.toString());
    }
}
