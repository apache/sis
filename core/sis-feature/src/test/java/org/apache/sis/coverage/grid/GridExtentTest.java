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
package org.apache.sis.coverage.grid;

import java.util.Locale;
import java.io.IOException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.coverage.PointOutsideCoverageException;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link GridExtent}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public final strictfp class GridExtentTest extends TestCase {
    /**
     * Creates a three-dimensional grid extent to be shared by different tests.
     */
    private static GridExtent create3D() {
        return new GridExtent(
                new DimensionNameType[] {DimensionNameType.COLUMN, DimensionNameType.ROW, DimensionNameType.TIME},
                new long[] {100, 200, 40}, new long[] {500, 800, 50}, false);
    }

    /**
     * Verifies the low and high values in the specified dimension of the given extent
     */
    static void assertExtentEquals(final GridExtent extent, final int dimension, final int low, final int high) {
        assertEquals("low",  low,  extent.getLow (dimension));
        assertEquals("high", high, extent.getHigh(dimension));
    }

    /**
     * Tests the {@link GridExtent#subsample(int...)}.
     */
    @Test
    public void testSubsample() {
        GridExtent extent = create3D();
        extent = extent.subsample(4, 3, 9);
        assertExtentEquals(extent, 0, 25, 124);                 // 100 cells
        assertExtentEquals(extent, 1, 66, 265);                 // 200 cells
        assertExtentEquals(extent, 2,  4,   5);                 //   2 cells
    }

    /**
     * Tests the {@link GridExtent#GridExtent(AbstractEnvelope,
     * GridRoundingMode, int[], GridExtent, int[])} constructor.
     */
    @Test
    public void testCreateFromEnvelope() {
        final GeneralEnvelope env = new GeneralEnvelope(HardCodedCRS.IMAGE);
        env.setRange(0, -23.01, 30.107);
        env.setRange(1,  12.97, 18.071);
        GridExtent extent = new GridExtent(env, GridRoundingMode.NEAREST, null, null, null);
        assertExtentEquals(extent, 0, -23, 29);
        assertExtentEquals(extent, 1,  13, 17);
        assertEquals(DimensionNameType.COLUMN, extent.getAxisType(0).get());
        assertEquals(DimensionNameType.ROW,    extent.getAxisType(1).get());
    }

    /**
     * Tests the {@link GridExtent#GridExtent(AbstractEnvelope, GridRoundingMode, int[],
     * GridExtent, int[])} constructor when an envelope has a span close to zero.
     */
    @Test
    public void testCreateFromThinEnvelope() {
        final GeneralEnvelope env = new GeneralEnvelope(3);
        env.setRange(0,  11.22,  11.23);
        env.setRange(1, -23.02, -23.01);
        env.setRange(2,  34.91,  34.92);
        GridExtent extent = new GridExtent(env, GridRoundingMode.NEAREST, null, null, null);
        assertExtentEquals(extent, 0,  11,  11);
        assertExtentEquals(extent, 1, -24, -24);
        assertExtentEquals(extent, 2,  34,  34);
    }

    /**
     * Tests the rounding performed by the {@link GridExtent#GridExtent(AbstractEnvelope,
     * GridRoundingMode, int[], GridExtent, int[])} constructor.
     */
    @Test
    public void testRoundings() {
        final GeneralEnvelope env = new GeneralEnvelope(6);
        env.setRange(0, 1.49999, 3.49998);      // Round to [1…3), stored as [1…2].
        env.setRange(1, 1.50001, 3.49998);      // Round to [2…3), stored as [1…2] (not [2…2]) because the span is close to 2.
        env.setRange(2, 1.49998, 3.50001);      // Round to [1…4), stored as [1…2] (not [1…3]) because the span is close to 2.
        env.setRange(3, 1.49999, 3.50002);      // Round to [1…4), stored as [2…3] because the upper part is closer to integer.
        env.setRange(4, 1.2,     3.8);          // Round to [1…4), stores as [1…3] because the span is not close enough to integer.
        GridExtent extent = new GridExtent(env, GridRoundingMode.NEAREST, null, null, null);
        assertExtentEquals(extent, 0, 1, 2);
        assertExtentEquals(extent, 1, 1, 2);
        assertExtentEquals(extent, 2, 1, 2);
        assertExtentEquals(extent, 3, 2, 3);
        assertExtentEquals(extent, 4, 1, 3);
        assertExtentEquals(extent, 5, 0, 0);    // Unitialized envelope values were [0…0].
    }

    /**
     * Tests {@link GridExtent#insert(int, DimensionNameType, long, long, boolean)}
     * with {@code offset} set to {@link GridExtent#getDimension()}.
     */
    @Test
    public void testAppend() {
        appendOrInsert(2, 1);
    }

    /**
     * Tests {@link GridExtent#insert(int, DimensionNameType, long, long, boolean)}.
     */
    @Test
    public void testInsert() {
        appendOrInsert(1, 2);
    }

    /**
     * Implementation of {@link #testAppend()} and {@link #testInsert()}.
     */
    private void appendOrInsert(final int offset, final int rowIndex) {
        GridExtent extent = new GridExtent(new DimensionNameType[] {DimensionNameType.COLUMN, DimensionNameType.ROW},
                                           new long[] {100, 200}, new long[] {500, 800}, true);
        extent = extent.insert(offset, DimensionNameType.TIME, 40, 50, false);
        assertEquals("dimension", 3, extent.getDimension());
        assertExtentEquals(extent, 0,        100, 500);
        assertExtentEquals(extent, rowIndex, 200, 800);
        assertExtentEquals(extent, offset,    40,  49);
        assertEquals(DimensionNameType.COLUMN, extent.getAxisType(0).get());
        assertEquals(DimensionNameType.ROW,    extent.getAxisType(rowIndex).get());
        assertEquals(DimensionNameType.TIME,   extent.getAxisType(offset).get());
    }

    /**
     * Tests {@link GridExtent#expand(long...)}.
     */
    @Test
    public void testExpand() {
        GridExtent extent = create3D();
        extent = extent.expand(20, -10);
        assertExtentEquals(extent, 0,  80, 519);
        assertExtentEquals(extent, 1, 210, 789);
        assertExtentEquals(extent, 2,  40,  49);
    }

    /**
     * Tests {@link GridExtent#resize(long...)}.
     */
    @Test
    public void testResize() {
        GridExtent extent = create3D();
        extent = extent.resize(200, 150);
        assertExtentEquals(extent, 0, 50, 249);
        assertExtentEquals(extent, 1, 50, 199);
        assertExtentEquals(extent, 2, 40,  49);
    }

    /**
     * Tests {@link GridExtent#reduce(int...)}.
     */
    @Test
    public void testReduce() {
        final GridExtent extent = create3D();
        GridExtent reduced = extent.reduce(0, 1);
        assertEquals("dimension", 2, reduced.getDimension());
        assertExtentEquals(reduced, 0, 100, 499);
        assertExtentEquals(reduced, 1, 200, 799);
        assertEquals(DimensionNameType.COLUMN, reduced.getAxisType(0).get());
        assertEquals(DimensionNameType.ROW,    reduced.getAxisType(1).get());

        reduced = extent.reduce(2);
        assertEquals("dimension", 1, reduced.getDimension());
        assertExtentEquals(reduced, 0, 40, 49);
        assertEquals(DimensionNameType.TIME, reduced.getAxisType(0).get());
    }

    /**
     * Tests {@link GridExtent#slice(DirectPosition, int[])}.
     */
    @Test
    public void testSlice() {
        final GeneralDirectPosition slicePoint = new GeneralDirectPosition(226.7, 47.2);
        final GridExtent extent = create3D();
        final GridExtent slice  = extent.slice(slicePoint, new int[] {1, 2});
        assertEquals("dimension", 3, slice.getDimension());
        assertExtentEquals(slice, 0, 100, 499);
        assertExtentEquals(slice, 1, 227, 227);
        assertExtentEquals(slice, 2,  47,  47);
        /*
         * Verify that point outside the GridExtent causes an exception to be thrown.
         * The message is localized but the grid coordinates "(900, 47)" are currently
         * unlocalized, so the check below should work in any locale (note that it may
         * change in future SIS version).
         */
        slicePoint.setOrdinate(0, 900);
        try {
            extent.slice(slicePoint, new int[] {1, 2});
            fail("Expected PointOutsideCoverageException");
        } catch (PointOutsideCoverageException e) {
            final String message = e.getLocalizedMessage();
            assertTrue(message, message.contains("(900, 47)"));     // See above comment.
        }
    }

    /**
     * Tests {@link GridExtent#getSubspaceDimensions(int)}.
     */
    @Test
    public void testGetSubspaceDimensions() {
        final GridExtent extent = new GridExtent(null, new long[] {100, 5, 200, 40}, new long[] {500, 5, 800, 40}, true);
        assertArrayEquals(new int[] {0,  2  }, extent.getSubspaceDimensions(2));
        assertArrayEquals(new int[] {0,1,2  }, extent.getSubspaceDimensions(3));
        assertArrayEquals(new int[] {0,1,2,3}, extent.getSubspaceDimensions(4));
        try {
            extent.getSubspaceDimensions(1);
            fail("Should not reduce to 1 dimension.");
        } catch (SubspaceNotSpecifiedException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests {@link GridExtent#cornerToCRS(Envelope)}.
     */
    @Test
    public void testCornerToCRS() {
        final GeneralEnvelope aoi = new GeneralEnvelope(HardCodedCRS.WGS84);
        aoi.setRange(0,  40, 55);
        aoi.setRange(1, -10, 70);
        final GridExtent extent = new GridExtent(null, new long[] {-20, -25}, new long[] {10, 15}, false);
        assertMatrixEquals("cornerToCRS", new Matrix3(
                0.5,  0,   50,
                0,    2,   40,
                0,    0,    1), extent.cornerToCRS(aoi), STRICT);
    }

    /**
     * Tests {@link GridExtent#toString()}.
     * Note that the string representation may change in any future SIS version.
     *
     * @throws IOException should never happen since we are writing to a {@link StringBuilder}.
     */
    @Test
    public void testToString() throws IOException {
        final StringBuilder buffer = new StringBuilder(100);
        create3D().appendTo(buffer, Vocabulary.getResources(Locale.ENGLISH));
        assertMultilinesEquals(
                "Column: [100 … 499] (400 cells)\n" +
                "Row:    [200 … 799] (600 cells)\n" +
                "Time:   [ 40 …  49]  (10 cells)\n", buffer);
    }
}
