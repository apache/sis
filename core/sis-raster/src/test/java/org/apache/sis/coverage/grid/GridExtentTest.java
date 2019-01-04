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
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.coverage.PointOutsideCoverageException;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link GridExtent}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
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
     * Tests the {@link GridExtent#GridExtent(AbstractEnvelope, GridRoundingMode, int[], GridExtent, int[])} constructor.
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
     * Tests the rounding performed by the {@link GridExtent#GridExtent(AbstractEnvelope, GridRoundingMode, int[], GridExtent, int[])} constructor.
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
     * Tests {@link GridExtent#append(DimensionNameType, long, long, boolean)}.
     */
    @Test
    public void testAppend() {
        GridExtent extent = new GridExtent(new DimensionNameType[] {DimensionNameType.COLUMN, DimensionNameType.ROW},
                                           new long[] {100, 200}, new long[] {500, 800}, true);
        extent = extent.append(DimensionNameType.TIME, 40, 50, false);
        assertEquals("dimension", 3, extent.getDimension());
        assertExtentEquals(extent, 0, 100, 500);
        assertExtentEquals(extent, 1, 200, 800);
        assertExtentEquals(extent, 2,  40,  49);
        assertEquals(DimensionNameType.COLUMN, extent.getAxisType(0).get());
        assertEquals(DimensionNameType.ROW,    extent.getAxisType(1).get());
        assertEquals(DimensionNameType.TIME,   extent.getAxisType(2).get());
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
     * Tests {@link GridExtent#toString()}.
     * Note that the string representation may change in any future SIS version.
     */
    @Test
    public void testToString() {
        final StringBuilder buffer = new StringBuilder(100);
        create3D().appendTo(buffer, Vocabulary.getResources(Locale.ENGLISH));
        assertMultilinesEquals(
                "Column: [100 … 499] (400 cells)\n" +
                "Row:    [200 … 799] (600 cells)\n" +
                "Time:   [ 40 …  49]  (10 cells)\n", buffer);
    }
}
