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
package org.apache.sis.internal.processing.image;

import java.util.Arrays;
import java.nio.DoubleBuffer;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.ArraysExt;


/**
 * Iterator over contouring grid cells together with an interpolator and an assembler of polyline segments.
 * A single instance of this class is created by {@code Isolines.generate(…)} for all bands to process in a
 * given image. {@code IsolineTracer} is used for doing a single iteration over all image pixels.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see <a href="https://en.wikipedia.org/wiki/Marching_squares">Marching squares on Wikipedia</a>
 *
 * @since 1.1
 * @module
 */
final class IsolineTracer {
    /**
     * Mask to apply on {@link Level#isDataAbove} for telling that value in a corner is higher than the level value.
     * Values are defined in {@code PixelIterator.Window} iteration order: from left to right, then top to bottom.
     *
     * <p>Note: there is some hard-coded dependencies to those exact values.
     * If values are changed, search for example for {@code log2(UPPER_RIGHT)} in comments.</p>
     */
    static final int UPPER_LEFT = 1, UPPER_RIGHT = 2, LOWER_LEFT = 4, LOWER_RIGHT = 8;

    /**
     * The 2×2 window containing pixel values in the 4 corners of current contouring grid cell.
     * Values are always stored with band index varying fastest, then column index, then row index.
     * Capacity and limit of data buffer is <var>(number of bands)</var> × 2 (width) × 2 (height).
     */
    private final DoubleBuffer window;

    /**
     * Increment to the position for reading next sample value.
     * It corresponds to the number of bands in {@link #window}.
     */
    private final int pixelStride;

    /**
     * Pixel coordinate on the left side of the cell where to interpolate.
     */
    int x;

    /**
     * Pixel coordinate on the top side of the cell where to interpolate.
     */
    int y;

    /**
     * Threshold for considering two coordinates as equal.
     * Shall be a value between 0 and 0.5.
     */
    private final double tolerance;

    /**
     * Final transform to apply on coordinates.
     */
    private final MathTransform gridToCRS;

    /**
     * Creates a new position for the given data window.
     */
    IsolineTracer(final DoubleBuffer window, final int pixelStride, double tolerance, final MathTransform gridToCRS) {
        this.window      = window;
        this.pixelStride = pixelStride;
        this.tolerance   = (tolerance = Math.min(Math.abs(tolerance), 0.5)) >= 0 ? tolerance : 0;
        this.gridToCRS   = gridToCRS;
    }

    /**
     * Builder of polylines for a single level. The segments to create are determined by a set
     * of {@linkplain #isDataAbove four flags} (one for each corner) encoded in an integer.
     * The meaning of those flags is described in Wikipedia "Marching squares" article,
     * except that this implementation uses different values.
     */
    final class Level {
        /**
         * The level value. This is a copy of {@link Isolines#levelValues} at the index of this level.
         *
         * @see #interpolate(int, int)
         */
        final double value;

        /**
         * Bitset telling which corners have a value greater than this isoline level {@linkplain #value}.
         * Each corner is associated to one of the bits illustrated below, where bit (0) is the less significant.
         * Note that this bit order is different than the order used in Wikipedia "Marching squares" article.
         * The order used in this class allows more direct bitwise operations as described in next section.
         *
         * {@preformat text
         *     (0)╌╌╌(1)
         *      ╎     ╎
         *     (2)╌╌╌(3)
         * }
         *
         * Bits are set to 1 where the data value is above the isoline {@linkplain #value}, and 0 where the data
         * value is equal or below the isoline value. Data values exactly equal to the isoline value are handled
         * as if they were greater. It does not matter for interpolations: we could flip this convention randomly,
         * the interpolated points would still the same. It could change the way line segments are assembled in a
         * single {@link Polyline}, but the algorithm stay consistent if we always apply the same rule for all points.
         *
         * <h4>Reusing bits from previous iteration</h4>
         * We will iterate on pixels from left to right, then from top to bottom. With that iteration order,
         * bits 0 and 2 can be obtained from the bit pattern of previous iteration with a simple bit shift.
         *
         * @see #UPPER_LEFT
         * @see #UPPER_RIGHT
         * @see #LOWER_LEFT
         * @see #LOWER_RIGHT
         */
        int isDataAbove;

        /**
         * The polyline to be continued on the next column. This is a single instance because iteration happens
         * from left to right before top to bottom. This instance is non-empty if the cell in previous iteration
         * was like below (all those examples have a line crossing the right border):
         *
         * {@preformat text
         *     ●╌╌╌╌╌╌●              ○╌╱╌╌╌╌●╱             ○╌╌╌╌╲╌●
         *     ╎      ╎              ╎╱     ╱              ╎     ╲╎
         *    ─┼──────┼─             ╱     ╱╎              ╎      ╲
         *     ○╌╌╌╌╌╌○             ╱●╌╌╌╌╱╌○              ○╌╌╌╌╌╌○╲
         * }
         *
         * This field {@linkplain Polyline#isEmpty() is empty} if the cell in previous iteration was like below
         * (no line cross the right border):
         *
         * {@preformat text
         *     ○╌╲╌╌╌╌●              ○╌╌╌┼╌╌●
         *     ╎  ╲   ╎              ╎   │  ╎
         *     ╎   ╲  ╎              ╎   │  ╎
         *     ○╌╌╌╌╲╌●              ○╌╌╌┼╌╌●
         * }
         */
        private final Polyline polylineOnLeft;

        /**
         * The polylines in each column which need to be continued on the next row.
         * This array contains empty instances in columns where there is no polyline to continue on next row.
         * For non-empty element at index <var>x</var>, values on the left border are given by pixels at coordinate
         * {@code x} and values on the right border are given by pixels at coordinate {@code x+1}. Example:
         *
         * {@preformat text
         *            ○╌╌╌╌╌╌●╱
         *            ╎ Top  ╱
         *            ╎ [x] ╱╎
         *     ●╌╌╌╌╌╌●╌╌╌╌╱╌○
         *     ╎ Left ╎██████╎ ← Cell where to create a segment
         *    ─┼──────┼██████╎
         *     ○╌╌╌╌╌╌○╌╌╌╌╌╌○
         *            ↑
         *     x coordinate of first pixel (upper-left corner)
         * }
         */
        private final Polyline[] polylinesOnTop;

        /**
         * The isolines as a Java2D shape, created when first needed. The {@link Polyline} coordinates are copied in
         * this path when a geometry is closed or when iteration finished on a row and the polyline is not reused by
         * next row. This is the shape to be returned to user for this level after we finished to process all cells.
         */
        Path2D path;

        /**
         * Creates new isoline levels for the given value.
         *
         * @param  value  the isoline level value.
         * @param  width  the contouring grid cell width (one cell smaller than image width).
         */
        Level(final double value, final int width) {
            this.value = value;
            polylineOnLeft = new Polyline();
            polylinesOnTop = new Polyline[width];
            for (int i=0; i<width; i++) {
                polylinesOnTop[i] = new Polyline();
            }
        }

        /**
         * Initializes the {@link #isDataAbove} value with values for the column on the right side.
         * After this method call, the {@link #UPPER_RIGHT} and {@link #LOWER_RIGHT} bits still need to be set.
         */
        final void nextColumn() {
            /*
             * Move bits on the right side to the left side.
             * The 1 operand in >>> is the hard-coded value
             * of    log2(UPPER_RIGHT) - log2(UPPER_LEFT)
             * and   log2(LOWER_RIGHT) - log2(LOWER_LEFT).
             */
            isDataAbove = (isDataAbove & (UPPER_RIGHT | LOWER_RIGHT)) >>> 1;
        }

        /**
         * Adds segments computed for values in a single pixel. Interpolations are determined by the 4 lowest bits
         * of {@link #isDataAbove}. The {@link #polylineOnLeft} and {@code polylinesOnTop[x]} elements are updated
         * by this method.
         *
         * <h4>How NaN values are handled</h4>
         * This algorithm does not need special attention for {@link Double#NaN} values. Interpolations will produce
         * {@code NaN} values and append them to the correct polyline (which does not depend on interpolation result)
         * like real values. Those NaN values will be filtered later in another method, when copying coordinates in
         * {@link Path2D} objects.
         */
        final void interpolate() throws TransformException {
            switch (isDataAbove) {
                default: {
                    throw new AssertionError(isDataAbove);      // Should never happen.
                }
                /*     ○╌╌╌╌╌╌○        ●╌╌╌╌╌╌●
                 *     ╎      ╎        ╎      ╎
                 *     ╎      ╎        ╎      ╎
                 *     ○╌╌╌╌╌╌○        ●╌╌╌╌╌╌●
                 */
                case 0:
                case UPPER_LEFT | UPPER_RIGHT | LOWER_LEFT | LOWER_RIGHT: {
                    assert polylinesOnTop[x].isEmpty();
                    assert polylineOnLeft   .isEmpty();
                    break;
                }
                /*     ○╌╌╌╌╌╌○        ●╌╌╌╌╌╌●
                 *    ─┼──────┼─      ─┼──────┼─
                 *     ╎      ╎        ╎      ╎
                 *     ●╌╌╌╌╌╌●        ○╌╌╌╌╌╌○
                 */
                case LOWER_LEFT | LOWER_RIGHT:
                case UPPER_LEFT | UPPER_RIGHT: {
                    assert polylinesOnTop[x].isEmpty();
                    if (polylineOnLeft.isEmpty()) {
                        interpolateOnLeftSide();
                    }
                    interpolateOnRightSide(polylineOnLeft);     // Will be the left side of next column.
                    break;
                }
                /*     ○╌╌╌┼╌╌●        ●╌╌╌┼╌╌○
                 *     ╎   │  ╎        ╎   │  ╎
                 *     ╎   │  ╎        ╎   │  ╎
                 *     ○╌╌╌┼╌╌●        ●╌╌╌┼╌╌○
                 */
                case UPPER_RIGHT | LOWER_RIGHT:
                case UPPER_LEFT  | LOWER_LEFT: {
                    assert polylineOnLeft.isEmpty();
                    final Polyline polylineOnTop = polylinesOnTop[x];
                    if (polylineOnTop.isEmpty()) {
                        interpolateOnTopSide(polylineOnTop);
                    }
                    interpolateOnBottomSide(polylineOnTop);     // Will be top side of next row.
                    break;
                }
                /*    ╲○╌╌╌╌╌╌○       ╲●╌╌╌╌╌╌●
                 *     ╲      ╎        ╲      ╎
                 *     ╎╲     ╎        ╎╲     ╎
                 *     ●╌╲╌╌╌╌○        ○╌╲╌╌╌╌●
                 */
                case LOWER_LEFT:
                case UPPER_LEFT | UPPER_RIGHT | LOWER_RIGHT: {
                    assert polylinesOnTop[x].isEmpty();
                    if (polylineOnLeft.isEmpty()) {
                        interpolateOnLeftSide();
                    }
                    interpolateOnBottomSide(polylinesOnTop[x].transferFrom(polylineOnLeft));
                    break;
                }
                /*     ○╌╌╌╌╲╌●        ●╌╌╌╌╲╌○
                 *     ╎     ╲╎        ╎     ╲╎
                 *     ╎      ╲        ╎      ╲
                 *     ○╌╌╌╌╌╌○╲       ●╌╌╌╌╌╌●╲
                 */
                case UPPER_RIGHT:
                case UPPER_LEFT | LOWER_LEFT | LOWER_RIGHT: {
                    assert polylineOnLeft.isEmpty();
                    if (polylineOnLeft.transferFrom(polylinesOnTop[x]).isEmpty()) {
                        interpolateOnTopSide(polylineOnLeft);
                    }
                    interpolateOnRightSide(polylineOnLeft);
                    break;
                }
                /*     ○╌╌╌╌╌╌○╱       ●╌╌╌╌╌╌●╱
                 *     ╎      ╱        ╎      ╱
                 *     ╎     ╱╎        ╎     ╱╎
                 *     ○╌╌╌╌╱╌●        ●╌╌╌╌╱╌○
                 */
                case LOWER_RIGHT:
                case UPPER_LEFT | UPPER_RIGHT | LOWER_LEFT: {
                    assert polylinesOnTop[x].isEmpty();
                    assert polylineOnLeft   .isEmpty();
                    interpolateOnRightSide (polylineOnLeft);
                    interpolateOnBottomSide(polylinesOnTop[x].attach(polylineOnLeft));
                    // Bottom of this cell will be top of next row.
                    break;
                }
                /*     ●╌╱╌╌╌╌○        ○╌╱╌╌╌╌●
                 *     ╎╱     ╎        ╎╱     ╎
                 *     ╱      ╎        ╱      ╎
                 *    ╱○╌╌╌╌╌╌○       ╱●╌╌╌╌╌╌●
                 */
                case UPPER_LEFT:
                case UPPER_RIGHT | LOWER_LEFT | LOWER_RIGHT: {
                    if (polylineOnLeft.isEmpty()) {
                        interpolateOnLeftSide();
                    }
                    interpolateOnTopSide(polylineOnLeft);
                    path = close(polylineOnLeft, polylinesOnTop[x], path);
                    break;
                }
                /*     ○╌╱╌╌╌╌●╱      ╲●╌╌╌╌╲╌○
                 *     ╎╱     ╱        ╲     ╲╎
                 *     ╱     ╱╎        ╎╲     ╲
                 *    ╱●╌╌╌╌╱╌○        ○╌╲╌╌╌╌●╲
                 *
                 * Disambiguation of saddle points: use the average data value for the center of the cell.
                 * If the estimated center value is greater than the isoline value, the above drawings are
                 * okay and we do not need to change `isDataAbove`. This is the left side illustrated below.
                 * But if the center value is below isoline value, then we need to flip `isDataAbove` bits
                 * (conceptually; not really because we need to keep `isDataAbove` value for next iteration).
                 * This is the right side illustrated below.
                 *
                 *     ○╱╌╌●╱      ╲●╌╌╲○                        ╲○╌╌╲●        ●╱╌╌○╱
                 *     ╱ ● ╱        ╲ ● ╲                         ╲ ○ ╲        ╱ ○ ╱
                 *    ╱●╌╌╱○        ○╲╌╌●╲                        ●╲╌╌○╲      ╱○╌╌╱●
                 */
                case UPPER_RIGHT | LOWER_LEFT:
                case UPPER_LEFT | LOWER_RIGHT: {
                    double average = 0;
                    {   // Compute sum of 4 corners.
                        final DoubleBuffer data = window;
                        final int limit = data.limit();
                        int p = data.position();
                        do average += data.get(p);
                        while ((p += pixelStride) < limit);
                        average /= 4;
                    }
                    boolean LLtoUR = isDataAbove == (LOWER_LEFT | UPPER_RIGHT);
                    LLtoUR ^= (average <= value);
                    if (polylineOnLeft.isEmpty()) {
                        interpolateOnLeftSide();
                    }
                    if (LLtoUR) {
                        interpolateOnTopSide(polylineOnLeft);
                        path = close(polylineOnLeft, polylinesOnTop[x], path);
                        interpolateOnRightSide (polylineOnLeft);
                        interpolateOnBottomSide(polylinesOnTop[x].attach(polylineOnLeft));
                    } else {
                        final Polyline swap = new Polyline();
                        final Polyline polylineOnTop = polylinesOnTop[x];
                        if (swap.transferFrom(polylineOnTop).isEmpty()) {
                            interpolateOnTopSide(swap);
                        }
                        interpolateOnRightSide(swap);
                        interpolateOnBottomSide(polylineOnTop.transferFrom(polylineOnLeft));
                        polylineOnLeft.transferFrom(swap);
                    }
                    break;
                }
            }
        }

        /** Appends to {@link #polylineOnLeft} a point interpolated on the left side. */
        private void interpolateOnLeftSide() {
            polylineOnLeft.append(x, y + interpolate(0, 2 * pixelStride));
        }

        /** Appends to the given polyline a point interpolated on the right side. */
        private void interpolateOnRightSide(final Polyline appendTo) {
            appendTo.append(x + 1, y + interpolate(pixelStride, 3 * pixelStride));
        }

        /** Appends to the given polyline a point interpolated on the top side. */
        private void interpolateOnTopSide(final Polyline appendTo) {
            appendTo.append(x + interpolate(0, pixelStride), y);
        }

        /** Appends to the given polyline a point interpolated on the bottom side. */
        private void interpolateOnBottomSide(final Polyline appendTo) {
            appendTo.append(x + interpolate(2 * pixelStride, 3 * pixelStride), y + 1);
        }

        /**
         * Interpolates the position where the isoline passes between two values.
         * The {@link #window} buffer position shall be the first sample value
         * for the band to process.
         *
         * @param  i1  index of first value in the buffer, ignoring band offset.
         * @param  i2  index of second value in the buffer, ignoring band offset.
         * @return a value interpolated between the values at the two given indices.
         */
        private double interpolate(final int i1, final int i2) {
            final DoubleBuffer data = window;
            final int    p  = data.position();
            final double v1 = data.get(p + i1);
            final double v2 = data.get(p + i2);
            return (value - v1) / (v2 - v1);
        }

        /**
         * Invoked after iteration on a single row has been completed. If there is a polyline
         * finishing on the right image border, that polyline needs to be written now because
         * it will not be continued by cells on next rows.
         */
        final void finishedRow() throws TransformException {
            if (!polylineOnLeft.transferToOpposite()) {
                path = writeTo(polylineOnLeft, path);
            }
            isDataAbove = 0;
        }

        /**
         * Invoked after the iteration has been completed on the full image.
         * This method flushes all reminding polylines to the {@link #path}.
         * It assumes that {@link #finishedRow()} has already been invoked.
         */
        final void finish() throws TransformException {
            for (int i=0; i < polylinesOnTop.length; i++) {
                path = writeTo(polylinesOnTop[i], path);
                polylinesOnTop[i] = null;
            }
        }
    }

    /**
     * Coordinates of a polyline under construction. Coordinates can be appended in only one direction.
     * If the polyline may growth on both directions (which happens if the polyline crosses the bottom
     * side and the right side of a cell), then the two directions are handled by two distinct instances
     * connected by their {@link #opposite} field.
     *
     * <p>When a polyline has been completed, its content is copied to {@link Level#path}
     * and the {@code Polyline} object is recycled for a new polyline.</p>
     */
    private static final class Polyline {
        /**
         * Number of coordinates in a tuple.
         */
        private static final int DIMENSION = 2;

        /**
         * Coordinates as (x,y) tuples. This array is expanded as needed.
         */
        private double[] coordinates;

        /**
         * Number of valid elements in the {@link #coordinates} array.
         * This is twice the number of points.
         */
        private int size;

        /**
         * If the polyline has points added to its two extremities, the other extremity.
         * Otherwise {@code null}.
         */
        private Polyline opposite;

        /**
         * Creates an initially empty polyline.
         */
        Polyline() {
            coordinates = ArraysExt.EMPTY_DOUBLE;
        }

        /**
         * Discards all coordinates in this polyline.
         */
        final void clear() {
            opposite = null;
            size = 0;
        }

        /**
         * Returns whether this polyline is empty.
         */
        final boolean isEmpty() {
            return size == 0;
        }

        /**
         * Declares that the specified polyline will add points in the direction opposite to this polyline.
         * This happens when the polyline crosses the bottom side and the right side of a cell (assuming an
         * iteration from left to right and top to bottom).
         *
         * @return {@code this} for method calls chaining.
         */
        final Polyline attach(final Polyline other) {
            assert (opposite == null) & (other.opposite == null);
            other.opposite = this;
            opposite = other;
            return this;
        }

        /**
         * Transfers all coordinates from given polylines to this polylines, in same order.
         * This is used when polyline on the left side continues on bottom side,
         * or conversely when polyline on the top side continues on right side.
         * This polyline shall be empty before this method is invoked.
         * The given source will become empty after this method returned.
         *
         * @param  source  the source from which to take data.
         * @return {@code this} for method calls chaining.
         */
        final Polyline transferFrom(final Polyline source) {
            assert isEmpty();
            final double[] buffer = coordinates;
            coordinates = source.coordinates;
            size        = source.size;
            opposite    = source.opposite;
            if (opposite != null) {
                opposite.opposite = this;
            }
            source.clear();
            source.coordinates = buffer;
            return this;
        }

        /**
         * Transfers all coordinates from this polyline to the polyline going in opposite direction.
         * This is used when this polyline reached the right image border, in which case its data
         * will be lost if we do not copy them somewhere.
         *
         * @return {@code true} if coordinates have been transferred,
         *         or {@code false} if there is no opposite direction.
         */
        final boolean transferToOpposite() {
            if (opposite == null) {
                return false;
            }
            final int sum = size + opposite.size;
            double[] data = opposite.coordinates;
            if (sum > data.length) {
                data = new double[sum];
            }
            System.arraycopy(opposite.coordinates, 0, data, size, opposite.size);
            for (int i=0, t=size; (t -= DIMENSION) >= 0;) {
                data[t  ] = coordinates[i++];
                data[t+1] = coordinates[i++];
            }
            opposite.size = sum;
            opposite.coordinates = data;
            opposite.opposite = null;
            clear();
            return true;
        }

        /**
         * Appends given coordinates to this polyline.
         *
         * @param  x  first coordinate of the (x,y) tuple to add.
         * @param  y  second coordinate of the (x,y) tuple to add.
         */
        final void append(final double x, final double y) {
            if (size >= coordinates.length) {
                coordinates = Arrays.copyOf(coordinates, Math.max(size * 2, 32));
            }
            coordinates[size++] = x;
            coordinates[size++] = y;
        }

        /**
         * Returns a string representation for debugging purposes.
         */
        @Override
        public String toString() {
            final StringBuilder b = new StringBuilder(30).append('[');
            if (size >= DIMENSION) {
                b.append((float) coordinates[0]).append(", ").append((float) coordinates[1]);
                final int n = size - DIMENSION;
                if (n >= DIMENSION) {
                    b.append(", ");
                    if (size >= DIMENSION*3) {
                        b.append(" … (").append(size / DIMENSION).append(" pts) … ");
                    }
                    b.append((float) coordinates[n]).append(", ").append((float) coordinates[n+1]);
                }
            }
            return b.append(']').toString();
        }
    }

    /**
     * Creates a polygon by joining first polyline with the specified polyline, then writes
     * it to the specified path. The two polylines will become empty after this method call.
     *
     * @param  join  the other polyline to join.
     * @param  path  where to write the polygon, or {@code null} if not yet created.
     * @return the given path, or a newly created path if the argument was null.
     */
    final Path2D close(final Polyline polyline, final Polyline join, final Path2D path) throws TransformException {
        return writeTo(path, polyline.opposite, polyline, join, join.opposite);
    }

    /**
     * Writes the content of this polyline to the given path.
     * This polyline will become empty after this method call.
     *
     * @param  path  where to write the polylines, or {@code null} if not yet created.
     * @return the given path, or a newly created path if the argument was null.
     */
    final Path2D writeTo(final Polyline polyline, final Path2D path) throws TransformException {
        return writeTo(path, polyline.opposite, polyline);
    }

    /**
     * Writes all given polylines to the specified path. Null {@code Polyline} instances are ignored.
     * {@code Polyline} instances at even index are written with their coordinates in reverse order.
     *
     * @param  path       where to write the polylines, or {@code null} if not yet created.
     * @param  polylines  the polylines to write.
     * @return the given path, or a newly created path if the argument was null.
     */
    private Path2D writeTo(Path2D path, final Polyline... polylines) throws TransformException {
        double xo = Double.NaN;
        double yo = Double.NaN;
        double px = Double.NaN;
        double py = Double.NaN;
        int state = PathIterator.SEG_MOVETO;
        for (int pi=0; pi < polylines.length; pi++) {
            final Polyline p = polylines[pi];
            if (p == null) {
                continue;
            }
            final boolean reverse = (pi & 1) == 0;
            final double[] coordinates = p.coordinates;
            final int size = p.size;
            gridToCRS.transform(coordinates, 0, coordinates, 0, size / Polyline.DIMENSION);
            for (int i=0; i<size;) {
                final double x, y;
                if (reverse) {
                    y = coordinates[size - ++i];
                    x = coordinates[size - ++i];
                } else {
                    x = coordinates[i++];
                    y = coordinates[i++];
                }
                if (Double.isNaN(x) || Double.isNaN(y) || (Math.abs(x - px) <= tolerance && Math.abs(y - py) <= tolerance)) {
                    continue;
                }
                switch (state) {
                    case PathIterator.SEG_MOVETO: {
                        px = xo = x;
                        py = yo = y;
                        state = PathIterator.SEG_LINETO;
                        break;
                    }
                    case PathIterator.SEG_LINETO: {
                        if (path == null) {
                            int s = size - (i - 2*Polyline.DIMENSION);
                            for (int k=pi; ++k < polylines.length;) {
                                final Polyline next = polylines[k];
                                if (next != null) {
                                    s = Math.addExact(s, next.size);
                                }
                            }
                            path = new Path2D.Double(Path2D.WIND_NON_ZERO, s / Polyline.DIMENSION);
                        }
                        path.moveTo(xo, yo);
                        path.lineTo(x, y);
                        state = PathIterator.SEG_CLOSE;
                        break;
                    }
                    default: {
                        if (Math.abs(x - xo) <= tolerance && Math.abs(y - yo) <= tolerance) {
                            path.closePath();
                            state = PathIterator.SEG_MOVETO;
                        } else {
                            path.lineTo(x, y);
                        }
                        break;
                    }
                }
            }
            p.clear();
        }
        return path;
    }
}
