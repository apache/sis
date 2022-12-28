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
package org.apache.sis.internal.processing.isoline;

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.feature.j2d.PathBuilder;
import org.apache.sis.util.Debug;


/**
 * Iterator over contouring grid cells together with an interpolator and an assembler of polyline segments.
 * A single instance of this class is created by {@code Isolines.generate(…)} for all bands to process in a
 * given image. {@code Tracer} is used for doing a single iteration over all image pixels.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see <a href="https://en.wikipedia.org/wiki/Marching_squares">Marching squares on Wikipedia</a>
 *
 * @since 1.1
 * @module
 */
final class Tracer {
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
     * The length of this array is <var>(number of bands)</var> × 2 (width) × 2 (height).
     */
    private final double[] window;

    /**
     * Increment to the position for reading next sample value.
     * It corresponds to the number of bands in {@link #window}.
     */
    private final int pixelStride;

    /**
     * Pixel coordinate on the left side of the cell where to interpolate.
     * The range is 0 inclusive to {@code domain.width} exclusive.
     */
    int x;

    /**
     * Pixel coordinate on the top side of the cell where to interpolate.
     * The range is 0 inclusive to {@code domain.height} exclusive.
     */
    int y;

    /**
     * Translation to apply on coordinates. For isolines computed sequentially, this is the image origin
     * (often 0,0 but not necessarily). For isolines computed in parallel, the translations are different
     * for each computation tile.
     */
    private final double translateX, translateY;

    /**
     * Final transform to apply on coordinates (integer source coordinates at pixel centers).
     * Can be {@code null} if none.
     */
    private final MathTransform gridToCRS;

    /**
     * Creates a new position for the given data window.
     *
     * @param  window       the 2×2 window containing pixel values in the 4 corners of current contouring grid cell.
     * @param  pixelStride  increment to the position in {@code window} for reading next sample value.
     * @param  domain       pixel coordinates where iteration will happen.
     * @param  gridToCRS    final transform to apply on coordinates (integer source coordinates at pixel centers).
     */
    Tracer(final double[] window, final int pixelStride, final Rectangle domain, final MathTransform gridToCRS) {
        this.window      = window;
        this.pixelStride = pixelStride;
        this.translateX  = domain.x;
        this.translateY  = domain.y;
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
         * Band number where to read values in the {@link #window} array.
         */
        private final int band;

        /**
         * The level value.
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
         * <pre class="text">
         *     (0)╌╌╌(1)
         *      ╎     ╎
         *     (2)╌╌╌(3)</pre>
         *
         * Bits are set to 1 where the data value is above the isoline {@linkplain #value}, and 0 where the data value
         * is below the isoline value. Data values exactly equal to the isoline value are handled as if they were greater.
         * It does not matter for interpolations: we could flip this convention randomly, the interpolated points would
         * still be the same. It could change the way line segments are assembled in a single {@link PolylineBuffer},
         * but the algorithm stay consistent if we always apply the same rule for all points.
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
         * <pre class="text">
         *     ●╌╌╌╌╌╌●              ○╌╱╌╌╌╌●╱             ○╌╌╌╌╲╌●
         *     ╎      ╎              ╎╱     ╱              ╎     ╲╎
         *    ─┼──────┼─             ╱     ╱╎              ╎      ╲
         *     ○╌╌╌╌╌╌○             ╱●╌╌╌╌╱╌○              ○╌╌╌╌╌╌○╲</pre>
         *
         * This field {@link PolylineBuffer#isEmpty() is empty} if the cell in previous iteration was like below
         * (no line cross the right border):
         *
         * <pre class="text">
         *     ○╌╲╌╌╌╌●              ○╌╌╌┼╌╌●
         *     ╎  ╲   ╎              ╎   │  ╎
         *     ╎   ╲  ╎              ╎   │  ╎
         *     ○╌╌╌╌╲╌●              ○╌╌╌┼╌╌●</pre>
         */
        private final PolylineBuffer polylineOnLeft;

        /**
         * The polylines in each column which need to be continued on the next row.
         * This array contains empty instances in columns where there are no polylines to continue on next row.
         * For non-empty element at index <var>x</var>, values on the left border are given by pixels at coordinate
         * {@code x} and values on the right border are given by pixels at coordinate {@code x+1}. Example:
         *
         * <pre class="text">
         *            ○╌╌╌╌╌╌●╱
         *            ╎ Top  ╱
         *            ╎ [x] ╱╎
         *     ●╌╌╌╌╌╌●╌╌╌╌╱╌○
         *     ╎ Left ╎██████╎ ← Cell where to create a segment
         *    ─┼──────┼██████╎
         *     ○╌╌╌╌╌╌○╌╌╌╌╌╌○
         *            ↑
         *     x coordinate of first pixel (upper-left corner)</pre>
         */
        private final PolylineBuffer[] polylinesOnTop;

        /**
         * Paths that have not yet been closed. The {@link PolylineBuffer} coordinates are copied in this map when
         * iteration finished on a row but the polyline under construction will not be continued by the next row,
         * or when the {@link #closeLeftWithTop(PolylineBuffer)} method has been invoked but the geometry to close
         * is still not complete. This map accumulates those partial shapes for assembling them later when missing
         * parts become available.
         *
         * <h4>Map keys</h4>
         * Keys are grid coordinates rounded toward 0. The coordinate having fraction digits has its bits inverted
         * by the {@code ~} operator. For each point, there is at most one coordinate having such fraction digits.
         *
         * <h4>Map values</h4>
         * {@code Fragments} instances are list of {@code double[]} arrays to be concatenated in a single polygon later.
         * For a given {@code Fragments} list, all {@code double[]} arrays at even indices shall have their points read
         * in reverse order and all {@code double[]} arrays at odd indices shall have their points read in forward order.
         * The list may contain null elements when there is no data in the corresponding iteration order.
         *
         * @see #closeLeftWithTop(PolylineBuffer)
         */
        private final Map<Point,Fragments> partialPaths;

        /**
         * Builder of isolines as a Java2D shape, created when first needed.
         * The {@link PolylineBuffer} coordinates are copied in this path when a geometry is closed
         * and transformed using {@link #gridToCRS}. This is almost final result; the only difference
         * compared to {@link #shape} is that the coordinates are not yet wrapped in a {@link Shape}.
         *
         * @see #writeTo(Joiner, PolylineBuffer[], boolean)
         * @see PolylineStage#FINAL
         */
        private Joiner path;

        /**
         * The isolines as a Java2D shape, created by {@link #finish()}.
         * This is the shape to be returned to user for this level after we finished to process all cells.
         *
         * @see PolylineStage#FINAL
         */
        Shape shape;

        /**
         * Creates new isoline levels for the given value.
         *
         * @param  band   band number where to read values in the {@link #window} array.
         * @param  value  the isoline level value.
         * @param  width  the contouring grid cell width (one cell smaller than image width).
         */
        Level(final int band, final double value, final int width) {
            this.band      = band;
            this.value     = value;
            partialPaths   = new HashMap<>();
            polylineOnLeft = new PolylineBuffer();
            polylinesOnTop = new PolylineBuffer[width];
            for (int i=0; i<width; i++) {
                polylinesOnTop[i] = new PolylineBuffer();
            }
        }

        /**
         * Initializes the {@link #isDataAbove} value with values for the column on the right side.
         * After this method call, the {@link #UPPER_RIGHT} and {@link #LOWER_RIGHT} bits still need to be set.
         *
         * @see Isolines#setMaskBit(double, int)
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
         * the {@link PathBuilder}.
         */
        @SuppressWarnings("AssertWithSideEffects")
        final void interpolate() throws TransformException {
            /*
             * Note: `interpolateMissingLeftSide()` and `interpolateMissingTopSide(…)` should do interpolations
             * only for cells in the first column and first row respectively. We could avoid those method calls
             * for all other cells if we add two flags in the `isDataAbove` bitmask: FIRST_ROW and FIRST_COLUMN.
             * The switch cases then become something like below:
             *
             *     case <bitmask> | FIRST_COLUMN | FIRST_ROW:
             *     case <bitmask> | FIRST_COLUMN: {
             *         interpolateMissingLeftSide();
             *         // Fall through
             *     }
             *     case <bitmask> | FIRST_ROW:
             *     case <bitmask>: {
             *         // Interpolations on other borders.
             *         break;
             *     }
             *
             * We tried that approach, but benchmarking on Java 15 suggested a small performance decrease
             * instead of an improvement. It may be worth to try again in the future, after advancement
             * in compiler technology.
             */
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
                    interpolateMissingLeftSide();
                    interpolateOnRightSide();                   // Will be the left side of next column.
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
                    final PolylineBuffer polylineOnTop = polylinesOnTop[x];
                    interpolateMissingTopSide(polylineOnTop);
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
                    interpolateMissingLeftSide();
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
                    interpolateMissingTopSide(polylineOnLeft.transferFrom(polylinesOnTop[x]));
                    interpolateOnRightSide();
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
                    interpolateOnRightSide();
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
                    closeLeftWithTop(polylinesOnTop[x]);
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
                        final double[] data = window;
                        int p = band;
                        do average += data[p];
                        while ((p += pixelStride) < data.length);
                        assert (p -= band) == pixelStride * 4 : p;
                        average /= 4;
                    }
                    boolean LLtoUR = isDataAbove == (LOWER_LEFT | UPPER_RIGHT);
                    LLtoUR ^= (average <= value);
                    final PolylineBuffer polylineOnTop = polylinesOnTop[x];
                    if (LLtoUR) {
                        closeLeftWithTop(polylineOnTop);
                        interpolateOnRightSide();
                        interpolateOnBottomSide(polylineOnTop.attach(polylineOnLeft));
                    } else {
                        interpolateMissingLeftSide();
                        final PolylineBuffer swap = new PolylineBuffer().transferFrom(polylineOnTop);
                        interpolateOnBottomSide(polylineOnTop.transferFrom(polylineOnLeft));
                        interpolateMissingTopSide(polylineOnLeft.transferFrom(swap));
                        interpolateOnRightSide();
                    }
                    break;
                }
            }
        }

        /**
         * Appends to {@link #polylineOnLeft} a point interpolated on the left side if that point is missing.
         * This interpolation should happens only in the first column.
         */
        private void interpolateMissingLeftSide() {
            if (polylineOnLeft.size == 0) {
                polylineOnLeft.append(translateX + (x),
                                      translateY + (y + interpolate(0, 2*pixelStride)));
            }
        }

        /**
         * Appends to {@code polylineOnTop} a point interpolated on the top side if that point is missing.
         * This interpolation should happens only in the first row.
         */
        private void interpolateMissingTopSide(final PolylineBuffer polylineOnTop) {
            if (polylineOnTop.size == 0) {
                interpolateOnTopSide(polylineOnTop);
            }
        }

        /**
         * Appends to the given polyline a point interpolated on the top side.
         */
        private void interpolateOnTopSide(final PolylineBuffer appendTo) {
            appendTo.append(translateX + (x + interpolate(0, pixelStride)),
                            translateY + (y));
        }

        /**
         * Appends to {@link #polylineOnLeft} a point interpolated on the right side.
         * The polyline on right side will become {@code polylineOnLeft} in next column.
         */
        private void interpolateOnRightSide() {
            polylineOnLeft.append(translateX + (x + 1),
                                  translateY + (y + interpolate(pixelStride, 3*pixelStride)));
        }

        /**
         * Appends to the given polyline a point interpolated on the bottom side.
         * The polyline on top side will become a {@code polylineOnBottoù} in next row.
         */
        private void interpolateOnBottomSide(final PolylineBuffer polylineOnTop) {
            polylineOnTop.append(translateX + (x + interpolate(2*pixelStride, 3*pixelStride)),
                                 translateY + (y + 1));
        }

        /**
         * Interpolates the position where the isoline passes between two values.
         *
         * @param  i1  index of first value in the buffer, ignoring band offset.
         * @param  i2  index of second value in the buffer, ignoring band offset.
         * @return a value interpolated between the values at the two given indices.
         */
        private double interpolate(final int i1, final int i2) {
            final double[] data = window;
            final int    p  = band;
            final double v1 = data[p + i1];
            final double v2 = data[p + i2];
            return (value - v1) / (v2 - v1);
        }

        /**
         * Joins {@link #polylineOnLeft} with {@code polylineOnTop}, saves their coordinates
         * and clear those {@link PolylineBuffer} instances for use in next cell.
         * The coordinates are written directly to {@link #path} if we got a closed polygon,
         * or otherwise are saved in {@link #partialPaths} for later processing.
         * This method is invoked for cells like below:
         *
         * <pre class="text">
         *     ●╌╱╌╌╌╌○        ○╌╱╌╌╌╌●        ○╌╱╌╌╌╌●╱
         *     ╎╱     ╎        ╎╱     ╎        ╎╱     ╱
         *     ╱      ╎        ╱      ╎        ╱     ╱╎
         *    ╱○╌╌╌╌╌╌○       ╱●╌╌╌╌╌╌●       ╱●╌╌╌╌╱╌○</pre>
         *
         * This method does itself the interpolations on left side and top side. The two polylines
         * {@link #polylineOnLeft} and {@code polylineOnTop} will become empty after this method call.
         *
         * @param  polylineOnTop  value of {@code polylinesOnTop[x]}.
         * @throws TransformException if the {@link Tracer#gridToCRS} transform cannot be applied.
         */
        private void closeLeftWithTop(final PolylineBuffer polylineOnTop) throws TransformException {
            interpolateMissingLeftSide();
            interpolateMissingTopSide(polylineOnTop);
            final PolylineBuffer[] polylines;
            if (polylineOnLeft.opposite == polylineOnTop) {
                assert polylineOnTop.opposite == polylineOnLeft;
                /*
                 * We have a loop: the polygon can be closed now, without copying coordinates to temporary buffers.
                 * Points in `PolylineBuffer` instances will be iterated in (reverse, forward) order respectively.
                 * Consequently, the points we just interpolated will be first point and last point before closing.
                 */
                polylines = new PolylineBuffer[] {polylineOnTop, polylineOnLeft};    // (reverse, forward) point order.
            } else {
                /*
                 * Joining left and top polylines do not yet create a closed shape. Consequently, we may not write
                 * in the `path` now. But maybe we can close the polygon later after more polylines are attached.
                 */
                final Fragments fragment = new Fragments(polylineOnLeft, polylineOnTop);
                if (fragment.isEmpty()) {
                    /*
                     * Fragment starts and ends with NaN values. We will not be able to complete a polygon.
                     * Better to write the polylines now for avoiding temporary copies of their coordinates.
                     */
                    polylines = new PolylineBuffer[] {
                        polylineOnLeft.opposite, polylineOnLeft, polylineOnTop, polylineOnTop.opposite
                    };
                } else if (fragment.addOrMerge(partialPaths)) {
                    /*
                     * The fragment has been merged with previously existing fragments and became a polygon.
                     * We can write the polygon immediately. There are no more references to those coordinates
                     * in the `partialPaths` map.
                     */
                    polylines = fragment.toPolylines();
                } else {
                    return;
                }
            }
            path = writeTo(path, polylines, true);
        }

        /**
         * Writes the content of given polyline without closing it as a polygon.
         * The given polyline will become empty after this method call.
         */
        private void writeFragment(final PolylineBuffer polyline) throws TransformException {
            final Fragments fragment = new Fragments(polyline, null);
            final PolylineBuffer[] polylines;
            final boolean close;
            if (fragment.isEmpty()) {
                close = false;
                polylines = new PolylineBuffer[] {polyline.opposite, polyline};     // (reverse, forward) point order.
            } else {
                close = fragment.addOrMerge(partialPaths);
                if (!close) {
                    // Keep in `partialPaths`. Maybe it can be closed later.
                    return;
                }
                polylines = fragment.toPolylines();
            }
            path = writeTo(path, polylines, close);
        }

        /**
         * Invoked after iteration on a single row has been completed. If there is a polyline
         * finishing on the right image border, the coordinates needs to be saved somewhere
         * because that {@link PolylineBuffer} will not be continued by cells on next rows.
         */
        final void finishedRow() throws TransformException {
            if (!polylineOnLeft.transferToOpposite()) {
                writeFragment(polylineOnLeft);
            }
            isDataAbove = 0;
        }

        /**
         * Invoked after the iteration has been completed on the full area of interest.
         * This method writes all remaining polylines to {@link #partialPaths}.
         * It assumes that {@link #finishedRow()} has already been invoked.
         * This {@link Level} instance cannot be used anymore after this call.
         */
        final void finish() throws TransformException {
            assert polylineOnLeft.isEmpty();
            polylineOnLeft.coordinates = null;
            /*
             * This method sets various values to null for letting the garbage collector do its work.
             * This is okay for a `Level` instance which is not going to be used anymore, except for
             * reading the `shape` field.
             */
            for (int i=0; i < polylinesOnTop.length; i++) {
                writeFragment(polylinesOnTop[i]);
                polylinesOnTop[i] = null;
            }
            assert isConsistent();
        }

        /**
         * Verifies that {@link #partialPaths} consistency. Used for assertions only.
         */
        private boolean isConsistent() {
            for (final Map.Entry<Point,Fragments> entry : partialPaths.entrySet()) {
                if (!entry.getValue().isExtremity(entry.getKey())) return false;
            }
            return true;
        }

        /**
         * Transfers all {@code other} polylines into this instance. The {@code other} instance should be a neighbor,
         * i.e. an instance sharing a border with this instance. The {@code other} instance will become empty after
         * this method call.
         *
         * @param  other  a neighbor level (on top, left, right or bottom) to merge with this level.
         * @throws TransformException if an error occurred during polylines creation.
         */
        final void merge(final Level other) throws TransformException {
            assert other != this && other.value == value;
            if (path == null) {
                path = other.path;
            } else {
                path.append(other.path);
            }
            other.path = null;
            assert  this.isConsistent();
            assert other.isConsistent();
            final IdentityHashMap<Fragments,Boolean> done = new IdentityHashMap<>(other.partialPaths.size() / 2);
            for (final Map.Entry<Point,Fragments> entry : other.partialPaths.entrySet()) {
                final Fragments fragment = entry.getValue();
                if (done.put(fragment, Boolean.TRUE) == null) {
                    assert fragment.isExtremity(entry.getKey());
                    if (fragment.addOrMerge(partialPaths)) {
                        path = writeTo(path, fragment.toPolylines(), true);
                        fragment.clear();
                    }
                }
                entry.setValue(null);       // Let the garbage collector do its work.
            }
        }

        /**
         * Flushes any pending {@link #partialPaths} to {@link #path}. This method is invoked after
         * {@link #finish()} has been invoked for all sub-regions (many sub-regions may exist if
         * isoline generation has been splitted for parallel computation).
         *
         * @throws TransformException if an error occurred during polylines creation.
         */
        final void flush() throws TransformException {
            for (final Map.Entry<Point,Fragments> entry : partialPaths.entrySet()) {
                final Fragments fragment = entry.getValue();
                assert fragment.isExtremity(entry.getKey());
                if (!fragment.isEmpty()) {
                    path = writeTo(path, fragment.toPolylines(), false);
                    fragment.clear();       // Necessary because the same list appears twice in the map.
                }
                entry.setValue(null);       // Let the garbage collector do its work.
            }
            if (path != null) {
                shape = path.build();
                path  = null;
            }
        }

        /**
         * Appends the pixel coordinates of this level to the given path, for debugging purposes only.
         * The {@link #gridToCRS} transform is <em>not</em> applied by this method.
         * For avoiding confusing behavior, that transform should be null.
         *
         * @param  appendTo  where to append the coordinates.
         *
         * @see Isolines#toRawPath()
         */
        @Debug
        final void toRawPath(final Map<PolylineStage,Path2D> appendTo) {
            PolylineStage.FINAL.add(appendTo, (path != null) ? path.snapshot() : shape);
            PolylineStage.FRAGMENT.add(appendTo, partialPaths);
            polylineOnLeft.toRawPath(appendTo);
            for (final PolylineBuffer p : polylinesOnTop) {
                if (p != null) p.toRawPath(appendTo);
            }
        }
    }

    /**
     * Writes all given polylines to the specified path builder. Null {@code PolylineBuffer} instances are ignored.
     * {@code PolylineBuffer} instances at even index are written with their points in reverse order.
     * All given polylines are cleared by this method.
     *
     * @param  path       where to write the polylines, or {@code null} if not yet created.
     * @param  polylines  the polylines to write.
     * @param  close      whether to close the polygon.
     * @return the given path builder, or a newly created builder if the argument was null.
     * @throws TransformException if the {@link #gridToCRS} transform cannot be applied.
     */
    private Joiner writeTo(Joiner path, final PolylineBuffer[] polylines, final boolean close) throws TransformException {
        for (int pi=0; pi < polylines.length; pi++) {
            final PolylineBuffer p = polylines[pi];
            if (p == null) {
                continue;
            }
            final int size = p.size;
            if (size == 0) {
                assert p.isEmpty();
                continue;
            }
            if (path == null) {
                path = new Joiner(gridToCRS);
            }
            path.append(p.coordinates, size, (pi & 1) == 0);
            p.clear();
        }
        if (path != null) {
            path.createPolyline(close);
        }
        return path;
    }
}
