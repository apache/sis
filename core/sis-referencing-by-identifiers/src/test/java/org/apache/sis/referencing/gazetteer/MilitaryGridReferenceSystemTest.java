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
import java.util.Random;
import java.lang.reflect.Field;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link MilitaryGridReferenceSystem}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn(ReferencingByIdentifiersTest.class)
public final strictfp class MilitaryGridReferenceSystemTest extends TestCase {
    /**
     * Verifies the metadata.
     */
    @Test
    public void verifyMetadata() {
        final MilitaryGridReferenceSystem rs = new MilitaryGridReferenceSystem();
        assertEquals("theme", "Mapping",      rs.getTheme().toString(Locale.ENGLISH));
        assertEquals("theme", "Cartographie", rs.getTheme().toString(Locale.FRENCH));

        final AbstractLocationType gzd = TestUtilities.getSingleton(rs.getLocationTypes());
        assertEquals("type", "Grid zone designator", gzd.getName().toString(Locale.ENGLISH));
        assertEquals("parent", 0, gzd.getParents().size());

        final AbstractLocationType sid = TestUtilities.getSingleton(gzd.getChildren());
        assertEquals("type", "100 km square identifier", sid.getName().toString(Locale.ENGLISH));
        assertSame("parent", gzd, TestUtilities.getSingleton(sid.getParents()));

        final AbstractLocationType gc = TestUtilities.getSingleton(sid.getChildren());
        assertEquals("type", "Grid coordinate", gc.getName().toString(Locale.ENGLISH));
        assertSame("parent", sid, TestUtilities.getSingleton(gc.getParents()));
    }

    /**
     * Verifies relationship between static fields documented in {@link Encoder}.
     */
    @Test
    public void verifyInvariants() {
        assertEquals("GRID_SQUARE_SIZE",
             StrictMath.pow(10, MilitaryGridReferenceSystem.METRE_PRECISION_DIGITS),
                                MilitaryGridReferenceSystem.GRID_SQUARE_SIZE, STRICT);
    }

    /**
     * Tests {@link MilitaryGridReferenceSystem.Encoder#latitudeBand(double)}.
     */
    @Test
    public void testLatitudeBand() {
        assertEquals("80°S", 'C', MilitaryGridReferenceSystem.Encoder.latitudeBand(-80));
        assertEquals("45°N", 'T', MilitaryGridReferenceSystem.Encoder.latitudeBand( 45));
        assertEquals("55°N", 'U', MilitaryGridReferenceSystem.Encoder.latitudeBand( 55));
        assertEquals("56°N", 'V', MilitaryGridReferenceSystem.Encoder.latitudeBand( 56));
        assertEquals("63°N", 'V', MilitaryGridReferenceSystem.Encoder.latitudeBand( 63));
        assertEquals("64°N", 'W', MilitaryGridReferenceSystem.Encoder.latitudeBand( 64));
        assertEquals("71°N", 'W', MilitaryGridReferenceSystem.Encoder.latitudeBand( 71));
        assertEquals("72°N", 'X', MilitaryGridReferenceSystem.Encoder.latitudeBand( 72));
        assertEquals("84°N", 'X', MilitaryGridReferenceSystem.Encoder.latitudeBand( 84));
    }

    /**
     * Verifies that {@link Zoner#isNorway(double)} and {@link Zoner#isSvalbard(double)}
     * are consistent with the latitude bands.
     */
    @Test
    @DependsOnMethod("testLatitudeBand")
    public void verifyZonerConsistency() {
        for (double φ = TransverseMercator.Zoner.SOUTH_BOUNDS; φ < TransverseMercator.Zoner.NORTH_BOUNDS; φ++) {
            final String latitude = String.valueOf(φ);
            final char band = MilitaryGridReferenceSystem.Encoder.latitudeBand(φ);
            assertTrue  (latitude, band >= 'C' && band <= 'X');
            assertEquals(latitude, band == 'V', TransverseMercator.Zoner.isNorway(φ));
            assertEquals(latitude, band == 'X', TransverseMercator.Zoner.isSvalbard(φ));
        }
    }

    /**
     * Verifies the hard-coded tables used for decoding purpose.
     * This method performs its computation using UTM zone 31, which is the zone from 0° to 6°E.
     * The results should be the same for all other zones.
     *
     * @throws TransformException if an error occurred while projecting a geographic coordinate.
     * @throws ReflectiveOperationException if this test method can not access the table to verify.
     */
    @Test
    @DependsOnMethod("verifyInvariants")
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
            assertEquals("ROW_RESOLVER.length", numBands, table.length);
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
            final double ymin = projection.transform(geographic, projected).getOrdinate(1);
            /*
             * Computes the largest possible northing value. This is not only the value of the next latitude band;
             * we also need to interchange the "zone centre" and "zone border" logic described in previous comment.
             * The result is that we will have some overlap in the northing value of consecutive latitude bands.
             */
            geographic.y = isSouth ? zoneCentre : zoneBorder;
            geographic.x = MilitaryGridReferenceSystem.Decoder.upperBound(φ);
            final double ymax = projection.transform(geographic, projected).getOrdinate(1);
            /*
             * Computes the value that we will encode in the MilitaryGridReferenceSystem.Decoder.ROW_RESOLVER table.
             * The lowest 4 bits are the number of the row cycle (a cycle of 2000 km). The remaining bits tell which
             * rows are valid in that latitude band.
             */
            final int rowCycle = (int) Math.floor(ymin / (MilitaryGridReferenceSystem.GRID_SQUARE_SIZE * MilitaryGridReferenceSystem.GRID_ROW_COUNT));
            final int lowerRow = (int) Math.floor(ymin /  MilitaryGridReferenceSystem.GRID_SQUARE_SIZE);    // Inclusive
            final int upperRow = (int) Math.ceil (ymax /  MilitaryGridReferenceSystem.GRID_SQUARE_SIZE);    // Exclusive
            assertTrue("rowCycle", rowCycle >= 0 && rowCycle <= MilitaryGridReferenceSystem.Decoder.NORTHING_BITS_MASK);
            assertTrue("lowerRow", lowerRow >= 0);
            assertTrue("upperRow", upperRow >= 0);
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
    private MilitaryGridReferenceSystem.Coder coder() {
        return new MilitaryGridReferenceSystem().createCoder();
    }

    /**
     * Decodes the given reference and returns its direct position.
     */
    private static DirectPosition decode(final MilitaryGridReferenceSystem.Coder coder, final String reference)
            throws TransformException
    {
        final AbstractLocation loc = coder.decode(reference);
        final Envelope2D envelope = new Envelope2D(loc.getEnvelope());
        final DirectPosition2D pos = new DirectPosition2D(loc.getPosition().getDirectPosition());
        assertTrue(reference, envelope.contains(pos));
        return pos;
    }

    /**
     * Tests encoding of various coordinates in Universal Transverse Mercator (UTM) projection.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    @DependsOnMethod({"verifyInvariants", "testLatitudeBand"})
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
    @DependsOnMethod({"verifyInvariants", "verifyDecoderTables"})
    public void testDecodeUTM() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        DirectPosition position;

        position = decode(coder, "32TNL8410239239");
        assertSame("crs", CommonCRS.WGS84.universal(41, 10), position.getCoordinateReferenceSystem());
        assertEquals("Easting",   584102.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 4539239.5, position.getOrdinate(1), STRICT);

        position = decode(coder, "29XMM8446304963");
        assertSame("crs", CommonCRS.WGS84.universal(82, -10), position.getCoordinateReferenceSystem());
        assertEquals("Easting",   484463.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 9104963.5, position.getOrdinate(1), STRICT);

        position = decode(coder, "32GNV8410260761");
        assertSame("crs", CommonCRS.WGS84.universal(-41, 10), position.getCoordinateReferenceSystem());
        assertEquals("Easting",   584102.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 5460761.5, position.getOrdinate(1), STRICT);

        position = decode(coder, "33XVM2240708183");
        assertSame("crs", CommonCRS.WGS84.universal(82, 10), position.getCoordinateReferenceSystem());
        assertEquals("Easting",   422407.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 9108183.5, position.getOrdinate(1), STRICT);

        position = decode(coder, "32FNL9360826322");
        assertSame("crs", CommonCRS.WGS84.universal(-49.4, 10.3), position.getCoordinateReferenceSystem());
        assertEquals("Easting",   593608.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 4526322.5, position.getOrdinate(1), STRICT);
    }

    /**
     * Tests decoding of values that are close to a change of zones.
     * Values tested in this methods were used to cause a {@link ReferenceVerifyException}
     * before we debugged the verification algorithm in the {@code decode(…)} method.
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    @DependsOnMethod("testDecodeUTM")
    public void testDecodeLimitCases() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        DirectPosition position;
        /*
         * Cell on the West border of a UTM zone in the South hemisphere.
         * The Easting value would be 250000 if the cell was not clipped.
         */
        position = decode(coder, "19JBK");                                            // South hemisphere
        assertSame("crs", CommonCRS.WGS84.universal(-10, -69), position.getCoordinateReferenceSystem());
        assertEquals("Easting",   251256, position.getOrdinate(0), 1);
        assertEquals("Northing", 6950000, position.getOrdinate(1), STRICT);
        /*
         * Easting range before clipping is [300000 … 400000] metres.
         * The east boung become 343828 metres after clipping.
         * The easting value would be 350000 if the cell was not clipped.
         */
        position = decode(coder, "1VCK");                                // North of Norway latitude band
        assertSame("crs", CommonCRS.WGS84.universal(62, -180), position.getCoordinateReferenceSystem());
        assertEquals("Easting",   371914, position.getOrdinate(0), 1);
        assertEquals("Northing", 6950000, position.getOrdinate(1), STRICT);
        /*
         * Northing value would be 7350000 if the cell was not clipped.
         */
        position = decode(coder, "57KTP");
        assertSame("crs", CommonCRS.WGS84.universal(-24, 156), position.getCoordinateReferenceSystem());
        assertEquals("Easting",   250000, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 7371306, position.getOrdinate(1), 1);
        /*
         * Easting  value would be  650000 if the cell was not clipped.
         * Northing value would be 6250000 if the cell was not clipped.
         */
        position = decode(coder, "56VPH");
        assertSame("crs", CommonCRS.WGS84.universal(55, 154), position.getCoordinateReferenceSystem());
        assertEquals("Easting",   643536, position.getOrdinate(0), 1);
        assertEquals("Northing", 6253618, position.getOrdinate(1), 1);
    }

    /**
     * Tests encoding of various coordinates in Universal Polar Stereographic (UPS) projection.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    @DependsOnMethod("verifyInvariants")
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
    @DependsOnMethod("verifyInvariants")
    public void testDecodeUPS() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        DirectPosition position;
        /*
         * South case.
         */
        position = decode(coder, "BAN0001000010");
        assertSame("crs", CommonCRS.WGS84.universal(-90, 0), position.getCoordinateReferenceSystem());
        assertEquals("Easting",  2000010.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 2000010.5, position.getOrdinate(1), STRICT);

        position = decode(coder, "AZM9999099990");
        assertSame("crs", CommonCRS.WGS84.universal(-90, 0), position.getCoordinateReferenceSystem());
        assertEquals("Easting",  1999990.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 1999990.5, position.getOrdinate(1), STRICT);

        position = decode(coder, "BLJ0672702814");
        assertSame("crs", CommonCRS.WGS84.universal(-90, 0), position.getCoordinateReferenceSystem());
        assertEquals("Easting",  2806727.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 1602814.5, position.getOrdinate(1), STRICT);
        /*
         * North case.
         */
        position = decode(coder, "ZAH0001000010");
        assertSame("crs", CommonCRS.WGS84.universal(90, 0), position.getCoordinateReferenceSystem());
        assertEquals("Easting",  2000010.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 2000010.5, position.getOrdinate(1), STRICT);

        position = decode(coder, "YZG9999099990");
        assertSame("crs", CommonCRS.WGS84.universal(90, 0), position.getCoordinateReferenceSystem());
        assertEquals("Easting",  1999990.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 1999990.5, position.getOrdinate(1), STRICT);

        position = decode(coder, "YRK8672702814");
        assertSame("crs", CommonCRS.WGS84.universal(90, 0), position.getCoordinateReferenceSystem());
        assertEquals("Easting",  1386727.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 2202814.5, position.getOrdinate(1), STRICT);
    }

    /**
     * Tests encoding of the same coordinate at various precision.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    @DependsOnMethod("testEncodeUTM")
    public void testPrecision() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        final DirectPosition2D position = new DirectPosition2D(CommonCRS.WGS84.universal(13, 103));
        position.x =  377299;
        position.y = 1483035;
        assertEquals("precision", 1, coder.getPrecision(), STRICT);
        assertEquals("48PUV7729983035", coder.encode(position));
        coder.setPrecision(10);
        assertEquals("precision", 10, coder.getPrecision(), STRICT);
        assertEquals("48PUV77298303", coder.encode(position));
        coder.setPrecision(304);
        assertEquals("precision", 100, coder.getPrecision(), STRICT);
        assertEquals("48PUV772830", coder.encode(position));
        coder.setPrecision(1002);
        assertEquals("precision", 1000, coder.getPrecision(), STRICT);
        assertEquals("48PUV7783", coder.encode(position));
        coder.setPrecision(10000);
        assertEquals("precision", 10000, coder.getPrecision(), STRICT);
        assertEquals("48PUV78", coder.encode(position));
        coder.setPrecision(990004);
        assertEquals("precision", 100000, coder.getPrecision(), STRICT);
        assertEquals("48PUV", coder.encode(position));
        coder.setPrecision(1000000);
        assertEquals("precision", 1000000, coder.getPrecision(), STRICT);
        assertEquals("48P", coder.encode(position));
    }

    /**
     * Tests encoding of the same coordinate with various separators, mixed with various precisions.
     *
     * @throws TransformException if an error occurred while computing the MGRS label.
     */
    @Test
    @DependsOnMethod("testPrecision")
    public void testSeparator() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        final DirectPosition2D position = new DirectPosition2D(CommonCRS.WGS84.universal(13, 103));
        position.x =  377299;
        position.y = 1483035;
        assertEquals("separator", "", coder.getSeparator());
        assertEquals("48PUV7729983035", coder.encode(position));

        coder.setSeparator(" ");
        assertEquals("separator", " ", coder.getSeparator());
        assertEquals("48 P UV 77299 83035", coder.encode(position));

        coder.setSeparator("/");
        coder.setPrecision(100000);
        assertEquals("separator", "/", coder.getSeparator());
        assertEquals("48/P/UV", coder.encode(position));
    }

    /**
     * Tests decoding the same coordinates with different separators at different resolutions.
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    @DependsOnMethod("testDecodeUTM")
    public void testDecodeVariants() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        coder.setSeparator(" / ");
        DirectPosition position;

        position = decode(coder, "32TNL8410239239");
        assertEquals("32TNL8410239239", position, decode(coder, "32/T/NL/84102/39239"));
        assertEquals("Easting",   584102.5, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 4539239.5, position.getOrdinate(1), STRICT);

        position = decode(coder, "32TNL8439");
        assertEquals("32TNL8439", position, decode(coder, "32/T/NL/84/39"));
        assertEquals("Easting",   584500.0, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 4539500.0, position.getOrdinate(1), STRICT);

        position = decode(coder, "32TNL83");
        assertEquals("32TNL83",   position, decode(coder, "32/T/NL/8/3"));
        assertEquals("Easting",   585000.0, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 4535000.0, position.getOrdinate(1), STRICT);

        position = decode(coder, "32TNL");
        assertEquals("32TNL", position, decode(coder, "32/T/NL"));
        assertEquals("Easting",   550000.0, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 4550000.0, position.getOrdinate(1), STRICT);

        position = decode(coder, "32T");
        assertEquals("32T", position, decode(coder, "32/T"));
        assertEquals("Easting",   500000.0, position.getOrdinate(0), STRICT);
        assertEquals("Northing", 9000000.0, position.getOrdinate(1), STRICT);
    }

    /**
     * Verifies the exceptions thrown when an invalid reference is given.
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    @DependsOnMethod("testDecodeUTM")
    public void testErrorDetection() throws TransformException {
        final MilitaryGridReferenceSystem.Coder coder = coder();
        try {
            coder.decode("32TNL841023923");
            fail("Shall not accept numeric identifier of odd length.");
        } catch (GazetteerException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("8410"));
            assertTrue(message, message.contains("23923"));
        }
        try {
            coder.decode("32TN");
            fail("Shall not accept half of a grid zone designator.");
        } catch (GazetteerException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("32TN"));
        }
        try {
            coder.decode("32SNL8410239239");
            fail("Shall report an invalid latitude band.");
        } catch (ReferenceVerifyException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("32SNL8410239239"));
            assertTrue(message, message.contains("32TNL"));
        }
    }

    /**
     * Encodes random coordinates, decodes them and verifies that the results are approximatively equal
     * to the original coordinates.
     *
     * @throws TransformException if an error occurred while computing the coordinate.
     */
    @Test
    @DependsOnMethod({"testEncodeUTM", "testDecodeUTM"})
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
            final double distance = expected.distance(r.getOrdinate(0), r.getOrdinate(1));
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
}
