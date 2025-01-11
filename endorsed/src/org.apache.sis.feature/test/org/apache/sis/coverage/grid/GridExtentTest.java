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

import java.util.Map;
import java.util.Locale;
import java.io.IOException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.pending.jdk.JDK18;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.test.Assertions.assertMapEquals;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertMultilinesEquals;
import static org.apache.sis.referencing.Assertions.assertEnvelopeEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.PointOutsideCoverageException;
import static org.opengis.test.Assertions.assertAxisDirectionsEqual;
import static org.opengis.test.Assertions.assertMatrixEquals;


/**
 * Tests {@link GridExtent}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class GridExtentTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public GridExtentTest() {
    }

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
        assertEquals(low,  extent.getLow (dimension), "low");
        assertEquals(high, extent.getHigh(dimension), "high");
        assertEquals(high - low + 1, extent.getSize(dimension), "size");
        assertEquals(JDK18.ceilDiv(high + low, 2), extent.getMedian(dimension), "median");
    }

    /**
     * Tests {@link GridExtent#subsample(long...)}.
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
     * Tests {@link GridExtent#upsample(long...)}.
     */
    @Test
    public void testUpsample() {
        GridExtent extent = create3D();
        extent = extent.upsample(4, 3, 9);
        assertExtentEquals(extent, 0, 400, 1999);               // 1600 cells
        assertExtentEquals(extent, 1, 600, 2399);               // 1800 cells
        assertExtentEquals(extent, 2, 360,  449);               //   90 cells
    }

    /**
     * Tests the {@link GridExtent#GridExtent(AbstractEnvelope,
     * GridRoundingMode, int[], int[], GridExtent, int[])} constructor.
     */
    @Test
    public void testCreateFromEnvelope() {
        final var env = new GeneralEnvelope(HardCodedCRS.IMAGE);
        env.setRange(0, -23.01, 30.107);
        env.setRange(1,  12.97, 18.071);
        var extent = new GridExtent(env, false, GridRoundingMode.NEAREST, GridClippingMode.STRICT, null, null, null, null);
        assertExtentEquals(extent, 0, -23, 29);
        assertExtentEquals(extent, 1,  13, 17);
        assertEquals(DimensionNameType.COLUMN, extent.getAxisType(0).get());
        assertEquals(DimensionNameType.ROW,    extent.getAxisType(1).get());
    }

    /**
     * Tests the {@link GridExtent#GridExtent(AbstractEnvelope, GridRoundingMode, int[],
     * int[], GridExtent, int[])} constructor when an envelope has a span close to zero.
     */
    @Test
    public void testCreateFromThinEnvelope() {
        final var env = new GeneralEnvelope(3);
        env.setRange(0,  11.22,  11.23);
        env.setRange(1, -23.02, -23.01);
        env.setRange(2,  34.91,  34.92);
        var extent = new GridExtent(env, false, GridRoundingMode.NEAREST, GridClippingMode.STRICT, null, null, null, null);
        assertExtentEquals(extent, 0,  11,  11);
        assertExtentEquals(extent, 1, -24, -24);
        assertExtentEquals(extent, 2,  34,  34);
    }

    /**
     * Tests the rounding performed by the {@link GridExtent#GridExtent(AbstractEnvelope,
     * GridRoundingMode, int[], int[], GridExtent, int[])} constructor.
     */
    @Test
    public void testRoundings() {
        final var env = new GeneralEnvelope(6);
        env.setRange(0, 1.49999, 3.49998);      // Round to [1…3), stored as [1…2].
        env.setRange(1, 1.50001, 3.49998);      // Round to [2…3), stored as [1…2] (not [2…2]) because the span is close to 2.
        env.setRange(2, 1.49998, 3.50001);      // Round to [1…4), stored as [1…2] (not [1…3]) because the span is close to 2.
        env.setRange(3, 1.49999, 3.50002);      // Round to [1…4), stored as [2…3] because the upper part is closer to integer.
        env.setRange(4, 1.2,     3.8);          // Round to [1…4), stores as [1…3] because the span is not close enough to integer.
        var extent = new GridExtent(env, false, GridRoundingMode.NEAREST, GridClippingMode.STRICT, null, null, null, null);
        assertExtentEquals(extent, 0, 1, 2);
        assertExtentEquals(extent, 1, 1, 2);
        assertExtentEquals(extent, 2, 1, 2);
        assertExtentEquals(extent, 3, 2, 3);
        assertExtentEquals(extent, 4, 1, 3);
        assertExtentEquals(extent, 5, 0, 0);    // Unitialized envelope values were [0…0].
    }

    /**
     * Tests {@link GridExtent#insertDimension(int, DimensionNameType, long, long, boolean)}
     * with {@code offset} set to {@link GridExtent#getDimension()}.
     */
    @Test
    public void testAppendDimension() {
        appendOrInsert(2, 1);
    }

    /**
     * Tests {@link GridExtent#insertDimension(int, DimensionNameType, long, long, boolean)}
     * with {@code offset} somewhere in the middle of the extent.
     */
    @Test
    public void testInsertDimension() {
        appendOrInsert(1, 2);
    }

    /**
     * Implementation of {@link #testAppend()} and {@link #testInsert()}.
     */
    private void appendOrInsert(final int offset, final int rowIndex) {
        var extent = new GridExtent(new DimensionNameType[] {DimensionNameType.COLUMN, DimensionNameType.ROW},
                                    new long[] {100, 200}, new long[] {500, 800}, true);
        extent = extent.insertDimension(offset, DimensionNameType.TIME, 40, 50, false);
        assertEquals(3, extent.getDimension(), "dimension");
        assertExtentEquals(extent, 0,        100, 500);
        assertExtentEquals(extent, rowIndex, 200, 800);
        assertExtentEquals(extent, offset,    40,  49);
        assertEquals(DimensionNameType.COLUMN, extent.getAxisType(0).get());
        assertEquals(DimensionNameType.ROW,    extent.getAxisType(rowIndex).get());
        assertEquals(DimensionNameType.TIME,   extent.getAxisType(offset).get());
    }

    /**
     * Tests {@link GridExtent#selectDimensions(int[])}.
     */
    @Test
    public void testSelectDimensions() {
        final GridExtent extent = create3D();
        GridExtent reduced = extent.selectDimensions(0, 1);
        assertEquals(2, reduced.getDimension(), "dimension");
        assertExtentEquals(reduced, 0, 100, 499);
        assertExtentEquals(reduced, 1, 200, 799);
        assertEquals(DimensionNameType.COLUMN, reduced.getAxisType(0).get());
        assertEquals(DimensionNameType.ROW,    reduced.getAxisType(1).get());

        reduced = extent.selectDimensions(2);
        assertEquals(1, reduced.getDimension(), "dimension");
        assertExtentEquals(reduced, 0, 40, 49);
        assertEquals(DimensionNameType.TIME, reduced.getAxisType(0).get());
    }

    /**
     * Tests {@link GridExtent#withRange(int, long, long)}.
     */
    @Test
    public void testWithRange() {
        GridExtent extent = create3D();
        assertSame(extent, extent.withRange(1, 200, 799));
        extent = extent.withRange(2, 30, 60);
        assertExtentEquals(extent, 0, 100, 499);
        assertExtentEquals(extent, 1, 200, 799);
        assertExtentEquals(extent, 2,  30,  60);
    }

    /**
     * Tests {@link GridExtent#expand(long...)}.
     */
    @Test
    public void testExpand() {
        GridExtent extent = create3D();
        assertSame(extent, extent.expand(new long[3]));
        extent = extent.expand(20, -10);            // One less dimension than `exent` dimension.
        assertExtentEquals(extent, 0,  80, 519);
        assertExtentEquals(extent, 1, 210, 789);
        assertExtentEquals(extent, 2,  40,  49);
    }

    /**
     * Tests {@link GridExtent#forChunkSize(int[])}.
     */
    @Test
    public void testForChunkSize() {
        GridExtent extent = create3D();
        extent = extent.forChunkSize(300, 200, 15);
        assertExtentEquals(extent, 0,   0, 599);
        assertExtentEquals(extent, 1, 200, 799);
        assertExtentEquals(extent, 2,  30,  59);
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
     * Tests {@link GridExtent#contains(long...)}.
     */
    @Test
    public void testContains() {
        final GridExtent extent = create3D();
        assertTrue (extent.contains(100, 200, 40));
        assertFalse(extent.contains(100, 200, 39));
    }

    /**
     * Tests {@link GridExtent#contains(GridExtent)} and {@link GridExtent#intersects(GridExtent)}.
     */
    @Test
    public void testContainsAndIntersects() {
        final var t1 = new GridExtent(null, new long[] {100, 600}, new long[] {300, 800}, true);
        final var t2 = new GridExtent(null, new long[] {200, 600}, new long[] {300, 700}, true);
        final var t3 = new GridExtent(null, new long[] { 50, 700}, new long[] {150, 800}, true);
        assertTrue (t1.contains  (t2));
        assertFalse(t2.contains  (t1));
        assertTrue (t1.intersects(t2));
        assertTrue (t2.intersects(t1));
        assertFalse(t1.contains  (t3));
        assertFalse(t3.contains  (t1));
        assertTrue (t1.intersects(t3));
        assertTrue (t3.intersects(t1));
        assertFalse(t2.contains  (t3));
        assertFalse(t3.contains  (t2));
        assertFalse(t2.intersects(t3));
        assertFalse(t3.intersects(t2));
    }

    /**
     * Creates another arbitrary extent for tests of union and intersection.
     */
    private static GridExtent createOther() {
        return new GridExtent(
                new DimensionNameType[] {DimensionNameType.COLUMN, DimensionNameType.ROW, DimensionNameType.TIME},
                new long[] {150, 220, 35}, new long[] {400, 820, 47}, false);
    }

    /**
     * Tests {@link GridExtent#intersect(GridExtent)}.
     */
    @Test
    public void testIntersect() {
        final GridExtent domain = createOther();
        final GridExtent extent = create3D().intersect(domain);
        assertExtentEquals(extent, 0, 150, 399);
        assertExtentEquals(extent, 1, 220, 799);
        assertExtentEquals(extent, 2, 40,  46);
        assertSame(extent.intersect(domain), extent);

        final GridExtent disjoint = domain.translate(0, 1000);
        var exception = assertThrows(DisjointExtentException.class, () -> extent.intersect(disjoint));
        assertMessageContains(exception);
    }

    /**
     * Tests {@link GridExtent#union(GridExtent)}.
     */
    @Test
    public void testUnion() {
        final GridExtent domain = createOther();
        final GridExtent extent = create3D().union(domain);
        assertExtentEquals(extent, 0, 100, 499);
        assertExtentEquals(extent, 1, 200, 819);
        assertExtentEquals(extent, 2, 35,  49);
        assertSame(extent.union(domain), extent);
    }

    /**
     * Tests {@link GridExtent#intersect(GridExtent)} with inconsistent axis types.
     * An exception should be thrown.
     */
    @Test
    public void testCombineInvalid() {
        final GridExtent domain = createOther();
        final GridExtent other = new GridExtent(
                new DimensionNameType[] {DimensionNameType.COLUMN, DimensionNameType.TRACK, DimensionNameType.TIME},
                new long[] {100, 200, 40}, new long[] {500, 800, 50}, false);

        var exception = assertThrows(IllegalArgumentException.class, () -> domain.intersect(other));
        assertMessageContains(exception);
    }

    /**
     * Tests {@link GridExtent#GridExtent(GridExtent, GridExtent)}.
     */
    @Test
    public void testConcatenate() {
        final GridExtent domain = create3D();
        final GridExtent other = new GridExtent(
                new DimensionNameType[] {DimensionNameType.VERTICAL},
                new long[] {-4}, new long[] {17}, true);
        final var extent = new GridExtent(domain, other);

        assertArrayEquals(new DimensionNameType[] {
            DimensionNameType.COLUMN, DimensionNameType.ROW, DimensionNameType.TIME, DimensionNameType.VERTICAL
        }, extent.getAxisTypes());

        assertArrayEquals(new long[] {
            100, 200, 40, -4,
            499, 799, 49, 17
        }, extent.getCoordinates());
    }

    /**
     * Tests {@link GridExtent#slice(DirectPosition, int[])}.
     */
    @Test
    public void testSlice() {
        final var slicePoint = new GeneralDirectPosition(226.7, 47.2);
        final GridExtent extent = create3D();
        final GridExtent slice  = extent.slice(slicePoint, new int[] {1, 2});
        assertEquals(3, slice.getDimension(), "dimension");
        assertExtentEquals(slice, 0, 100, 499);
        assertExtentEquals(slice, 1, 227, 227);
        assertExtentEquals(slice, 2,  47,  47);
        /*
         * Verify that point outside the GridExtent causes an exception to be thrown.
         * The message is localized but the grid coordinates "(900, 47)" are currently
         * unlocalized, so the check below should work in any locale (note that it may
         * change in future SIS version).
         */
        slicePoint.setCoordinate(0, 900);
        var exception = assertThrows(PointOutsideCoverageException.class, () -> extent.slice(slicePoint, new int[] {1, 2}));
        assertMessageContains(exception, "(900, 47)");         // See above comment.
    }

    /**
     * Tests {@link GridExtent#getSubspaceDimensions(int)} and {@link GridExtent#getLargestDimensions(int)}.
     * Opportunistically tests {@link GridExtent#getSliceCoordinates()} since the two methods closely related.
     */
    @Test
    public void testGetSubspaceDimensions() {
        var extent = new GridExtent(null, new long[] {100, 5, 200, 40}, new long[] {500, 5, 800, 40}, true);
        assertMapEquals(Map.of(1, 5L, 3, 40L), extent.getSliceCoordinates());
        assertSubspaceEquals(extent, 0,  2  );
        assertSubspaceEquals(extent, 0,1,2  );
        assertSubspaceEquals(extent, 0,1,2,3);

        var exception = assertThrows(SubspaceNotSpecifiedException.class, () -> extent.getSubspaceDimensions(1));
        assertMessageContains(exception);
    }

    /**
     * Verifies the result of {@code getSubspaceDimensions(…)} and {@code getLargestDimensions(…)}.
     * In this test, the two methods should produce the same results.
     *
     * @param extent    the grid extent to test.
     * @param expected  the expected result.
     */
    private static void assertSubspaceEquals(final GridExtent extent, final int... expected) {
        assertArrayEquals(expected, extent.getSubspaceDimensions(expected.length));
        assertArrayEquals(expected, extent.getLargestDimensions (expected.length));
    }

    /**
     * Tests {@link GridExtent#cornerToCRS(Envelope, long, int[])}.
     */
    @Test
    public void testCornerToCRS() {
        final var aoi = new GeneralEnvelope(HardCodedCRS.WGS84);
        aoi.setRange(0,  40, 55);
        aoi.setRange(1, -10, 70);
        final var extent = new GridExtent(null,
                new long[] {-20, -25},
                new long[] { 10,  15}, false);
        /*
         * No axis flip.
         * Verification:  y  =  2 × −25 + 40  =  −10  (the minimum value declared in envelope).
         */
        assertMatrixEquals(new Matrix3(0.5,  0,   50,
                                       0,    2,   40,
                                       0,    0,    1),
                extent.cornerToCRS(aoi, 0, null), STRICT, "cornerToCRS");
        /*
         * Y axis flip.
         * Verification:  y  =  −2 × −25 + 20  =  70  (the maximum value declared in envelope).
         */
        assertMatrixEquals(new Matrix3(0.5,  0,   50,
                                       0,   -2,   20,
                                       0,    0,    1),
                extent.cornerToCRS(aoi, 2, null), STRICT, "cornerToCRS");
        /*
         * Swap axis order. The {1,0} indices apply to grid dimensions, not to CRS dimensions.
         * Verification:  x  =  0.375 × −25 + 49.375  =  40  (the minimum value declared in envelope).
         *                y  =  2.667 × −20 + 43.333  ≈ −10  (idem).
         */
        assertMatrixEquals(new Matrix3(0,                   0.375,   49.375,
                                       2.6666666666666667,  0,       43.333333333333333,
                                       0,                   0,        1),
                extent.cornerToCRS(aoi, 0, new int[] {1,0}), 1E-15, "cornerToCRS");
    }

    /**
     * Tests {@link GridExtent#toEnvelope(MathTransform)} with an identity transform.
     *
     * @throws TransformException if an error occurred while transforming to an envelope.
     */
    @Test
    public void testToEnvelope() throws TransformException {
        final var extent = new GridExtent(new DimensionNameType[] {
            DimensionNameType.COLUMN,
            DimensionNameType.ROW,
            DimensionNameType.TIME
        }, new long[] {0, 0, 741}, new long[] {13, 9, 741}, true);
        final GeneralEnvelope envelope = extent.toEnvelope(MathTransforms.identity(3));

        assertEnvelopeEquals(new GeneralEnvelope(
                new double[] { 0,  0, 741},
                new double[] {14, 10, 742}), envelope);

        assertAxisDirectionsEqual(envelope.getCoordinateReferenceSystem().getCoordinateSystem(),
                AxisDirection.COLUMN_POSITIVE,
                AxisDirection.ROW_POSITIVE,
                AxisDirection.FUTURE);
    }

    /**
     * Tests {@link GridExtent#toEnvelope(MathTransform)} with a non-identity transform.
     *
     * @throws TransformException if an error occurred while transforming to an envelope.
     */
    @Test
    public void testToTransformedEnvelope() throws TransformException {
        final var extent = new GridExtent(new DimensionNameType[] {
            DimensionNameType.ROW,
            DimensionNameType.TIME,
            DimensionNameType.COLUMN,
            DimensionNameType.VERTICAL
        }, new long[] {100, 5, 200, 40}, new long[] {500, 7, 800, 50}, false);
        final GeneralEnvelope envelope = extent.toEnvelope(MathTransforms.linear(Matrices.create(5, 5, new double[] {
            0,  0,  1,  0,  0,
           -1,  0,  0,  0,  0,
            0,  0,  0, -1,  0,
            0,  1,  0,  0,  0,
            0,  0,  0,  0,  1})));

        assertEnvelopeEquals(new GeneralEnvelope(
                new double[] {200, -500, -50, 5},
                new double[] {800, -100, -40, 7}), envelope);

        assertAxisDirectionsEqual(envelope.getCoordinateReferenceSystem().getCoordinateSystem(),
                AxisDirection.COLUMN_POSITIVE,
                AxisDirection.ROW_NEGATIVE,
                AxisDirection.DOWN,
                AxisDirection.FUTURE);
    }

    /**
     * Tests {@link GridExtent#toString()}.
     * Note that the string representation may change in any future SIS version.
     *
     * @throws IOException should never happen since we are writing to a {@link StringBuilder}.
     */
    @Test
    public void testToString() throws IOException {
        final var buffer = new StringBuilder(100);
        create3D().appendTo(buffer, Vocabulary.forLocale(Locale.ENGLISH));
        assertMultilinesEquals(
                "Column: [100 … 499] (400 cells)\n" +
                "Row:    [200 … 799] (600 cells)\n" +
                "Time:   [ 40 …  49]  (10 cells)\n", buffer);
    }

    /**
     * Verifies that a translation of zero cell results in the same {@link GridExtent} instance.
     */
    @Test
    @DisplayName("Empty translation returns same extent instance")
    public void testZeroTranslation() {
        final var extent = new GridExtent(10, 10);
        assertSame(extent, extent.translate());
        assertSame(extent, extent.translate(0));
        assertSame(extent, extent.translate(0, 0));
    }

    /**
     * Verifies that {@link GridExtent#translate(long...)} accepts a vector with less dimensions
     * than the extent number of dimensions. No translation shall be applied in missing dimensions.
     */
    @Test
    @DisplayName("Translating only first dimensions leave others untouched")
    public void testTranslateOneDimension() {
        final var base = new GridExtent(null, new long[] {
            0, 0, 0,
            2, 2, 2
        });
        final GridExtent translatedByX = base.translate(1);
        assertArrayEquals(new long[] {1, 0, 0}, translatedByX.getLow() .getCoordinateValues(), "Lower corner");
        assertArrayEquals(new long[] {3, 2, 2}, translatedByX.getHigh().getCoordinateValues(), "Upper corner");

        final GridExtent translatedByY = base.translate(0, -1);
        assertArrayEquals(new long[] {0, -1, 0}, translatedByY.getLow() .getCoordinateValues(), "Lower corner");
        assertArrayEquals(new long[] {2,  1, 2}, translatedByY.getHigh().getCoordinateValues(), "Upper corner");

        final GridExtent translatedByXAndY = base.translate(-1, 4);
        assertArrayEquals(new long[] {-1, 4, 0}, translatedByXAndY.getLow() .getCoordinateValues(), "Lower corner");
        assertArrayEquals(new long[] { 1, 6, 2}, translatedByXAndY.getHigh().getCoordinateValues(), "Upper corner");

        // Paranoiac check: ensure that base extent has been left untouched.
        assertArrayEquals(new long[] {0, 0, 0}, base.getLow() .getCoordinateValues(), "Base lower corner");
        assertArrayEquals(new long[] {2, 2, 2}, base.getHigh().getCoordinateValues(), "Base lower corner");
    }

    /**
     * Verifies that {@link GridExtent#translate(long...)} applies a translation on all dimensions
     * when the given vector is long enough.
     */
    @Test
    @DisplayName("Translating all dimensions")
    public void testTranslateAllDimensions() {
        final var base = new GridExtent(null, new long[] {
            -1, -1, -2, 10,
             2,  2,  2, 20
        });
        final GridExtent translated = base.translate(-2, 1, 1, 100);
        assertArrayEquals(new long[] {-3, 0, -1, 110}, translated.getLow() .getCoordinateValues(), "Lower corner");
        assertArrayEquals(new long[] { 0, 3,  3, 120}, translated.getHigh().getCoordinateValues(), "Upper corner");

        // Paranoiac check: ensure that base extent has been left untouched.
        assertArrayEquals(new long[] {-1, -1, -2, 10}, base.getLow() .getCoordinateValues(), "Base lower corner");
        assertArrayEquals(new long[] { 2,  2,  2, 20}, base.getHigh().getCoordinateValues(), "Base lower corner");
    }
}
