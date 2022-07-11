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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.feature.j2d.PathBuilder;
import org.apache.sis.internal.util.Numerics;
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
     */
    int x;

    /**
     * Pixel coordinate on the top side of the cell where to interpolate.
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
    IsolineTracer(final double[] window, final int pixelStride, final Rectangle domain, final MathTransform gridToCRS) {
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
         * {@preformat text
         *     (0)╌╌╌(1)
         *      ╎     ╎
         *     (2)╌╌╌(3)
         * }
         *
         * Bits are set to 1 where the data value is above the isoline {@linkplain #value}, and 0 where the data
         * value is below the isoline value. Data values exactly equal to the isoline value are handled as if
         * they were greater. It does not matter for interpolations: we could flip this convention randomly,
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
         * This array contains empty instances in columns where there are no polylines to continue on next row.
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
         * Paths that have not yet been closed. The {@link Polyline} coordinates are copied in this map when iteration
         * finished on a row and the polyline is not reused by next row, or when the {@link #closeLeftWithTop(Polyline)}
         * method has been invoked but the geometry to close is still not complete. This map accumulates those partial
         * shapes for assembling them later when missing parts become available.
         *
         * <h4>Map keys</h4>
         * Keys are grid coordinates rounded toward 0. The coordinate having fraction digits has its bits inverted
         * by the {@code ~} operator. For each point, there is at most one coordinate having such fraction digits.
         *
         * <h4>Map values</h4>
         * {@code Unclosed} instances are list of {@code double[]} arrays to be concatenated in a single polygon later.
         * For a given {@code Unclosed} list, all {@code double[]} arrays at even indices shall have their points read
         * in reverse order and all {@code double[]} arrays at odd indices shall have their points read in forward order.
         * The list may contain null elements when there is no data in the corresponding iteration order.
         *
         * @see #closeLeftWithTop(Polyline)
         */
        private final Map<Point,Unclosed> partialPaths;

        /**
         * Builder of isolines as a Java2D shape, created when first needed.
         * The {@link Polyline} coordinates are copied in this path when a geometry is closed.
         *
         * @see #writeTo(Joiner, Polyline[], boolean)
         */
        private Joiner path;

        /**
         * The isolines as a Java2D shape, created by {@link #finish()}.
         * This is the shape to be returned to user for this level after we finished to process all cells.
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
            polylineOnLeft = new Polyline();
            polylinesOnTop = new Polyline[width];
            for (int i=0; i<width; i++) {
                polylinesOnTop[i] = new Polyline();
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
                    final Polyline polylineOnTop = polylinesOnTop[x];
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
                    final Polyline polylineOnTop = polylinesOnTop[x];
                    if (LLtoUR) {
                        closeLeftWithTop(polylineOnTop);
                        interpolateOnRightSide();
                        interpolateOnBottomSide(polylineOnTop.attach(polylineOnLeft));
                    } else {
                        interpolateMissingLeftSide();
                        final Polyline swap = new Polyline().transferFrom(polylineOnTop);
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
        private void interpolateMissingTopSide(final Polyline polylineOnTop) {
            if (polylineOnTop.size == 0) {
                interpolateOnTopSide(polylineOnTop);
            }
        }

        /**
         * Appends to the given polyline a point interpolated on the top side.
         */
        private void interpolateOnTopSide(final Polyline appendTo) {
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
        private void interpolateOnBottomSide(final Polyline polylineOnTop) {
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
         * Joins {@link #polylineOnLeft} with {@code polylineOnTop}, saves their coordinates and clear
         * those {@code Polyline} instances for use in next cell. The coordinates are written directly
         * to {@link #path} if we got a closed polygon, or otherwise are saved in {@link #partialPaths}
         * for later processing. This method is invoked for cells like below:
         *
         * {@preformat text
         *     ●╌╱╌╌╌╌○        ○╌╱╌╌╌╌●        ○╌╱╌╌╌╌●╱
         *     ╎╱     ╎        ╎╱     ╎        ╎╱     ╱
         *     ╱      ╎        ╱      ╎        ╱     ╱╎
         *    ╱○╌╌╌╌╌╌○       ╱●╌╌╌╌╌╌●       ╱●╌╌╌╌╱╌○
         * }
         *
         * This method does itself the interpolations on left side and top side. The two polylines
         * {@link #polylineOnLeft} and {@code polylineOnTop} will become empty after this method call.
         *
         * @param  polylineOnTop  value of {@code polylinesOnTop[x]}.
         * @throws TransformException if the {@link IsolineTracer#gridToCRS} transform can not be applied.
         */
        private void closeLeftWithTop(final Polyline polylineOnTop) throws TransformException {
            interpolateMissingLeftSide();
            interpolateMissingTopSide(polylineOnTop);
            final Polyline[] polylines;
            if (polylineOnLeft.opposite == polylineOnTop) {
                assert polylineOnTop.opposite == polylineOnLeft;
                /*
                 * We have a loop: the polygon can be closed now, without copying coordinates to temporary buffers.
                 * Points in the two `Polyline` instances will be iterated in (reverse, forward) order respectively.
                 * Consequently the points we just interpolated will be first point and last point before closing.
                 */
                polylines = new Polyline[] {polylineOnTop, polylineOnLeft};     // (reverse, forward) point order.
            } else {
                /*
                 * Joining left and top polylines do not yet create a closed shape. Consequently we may not write
                 * in the `path` now. But maybe we can close the polygon later after more polylines are attached.
                 */
                final Unclosed fragment = new Unclosed(polylineOnLeft, polylineOnTop);
                if (fragment.isEmpty()) {
                    /*
                     * Fragment starts and ends with NaN values. We will not be able to complete a polygon.
                     * Better to write the polylines now for avoiding temporary copies of their coordinates.
                     */
                    polylines = new Polyline[] {
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
        private void writeUnclosed(final Polyline polyline) throws TransformException {
            final Unclosed fragment = new Unclosed(polyline, null);
            final Polyline[] polylines;
            final boolean close;
            if (fragment.isEmpty()) {
                close = false;
                polylines = new Polyline[] {polyline.opposite, polyline};       // (reverse, forward) point order.
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
         * because that {@code Polyline} will not be continued by cells on next rows.
         */
        final void finishedRow() throws TransformException {
            if (!polylineOnLeft.transferToOpposite()) {
                writeUnclosed(polylineOnLeft);
            }
            isDataAbove = 0;
        }

        /**
         * Invoked after the iteration has been completed on the full area of interest.
         * This method writes all remaining polylines to {@link #partialPaths}.
         * It assumes that {@link #finishedRow()} has already been invoked.
         * This {@code Isoline} can not be used anymore after this call.
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
                writeUnclosed(polylinesOnTop[i]);
                polylinesOnTop[i] = null;
            }
            assert isConsistent();
        }

        /**
         * Verifies that {@link #partialPaths} consistency. Used for assertions only.
         */
        private boolean isConsistent() {
            for (final Map.Entry<Point,Unclosed> entry : partialPaths.entrySet()) {
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
            final IdentityHashMap<Unclosed,Boolean> done = new IdentityHashMap<>(other.partialPaths.size() / 2);
            for (final Map.Entry<Point,Unclosed> entry : other.partialPaths.entrySet()) {
                final Unclosed fragment = entry.getValue();
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
            for (final Map.Entry<Point,Unclosed> entry : partialPaths.entrySet()) {
                final Unclosed fragment = entry.getValue();
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
        static final int DIMENSION = 2;

        /**
         * Coordinates as (x,y) tuples. This array is expanded as needed.
         */
        double[] coordinates;

        /**
         * Number of valid elements in the {@link #coordinates} array.
         * This is twice the number of points.
         */
        int size;

        /**
         * If the polyline has points added to its two extremities, the other extremity. Otherwise {@code null}.
         * The first point of {@code opposite} polyline is connected to the first point of this polyline.
         * Consequently when those two polylines are joined in a single polyline, the coordinates of either
         * {@code this} or {@code opposite} must be iterated in reverse order.
         */
        Polyline opposite;

        /**
         * Creates an initially empty polyline.
         */
        Polyline() {
            coordinates = ArraysExt.EMPTY_DOUBLE;
        }

        /**
         * Creates a new polyline wrapping the given coordinates. Used for wrapping {@link Unclosed}
         * instances in objects expected by {@link IsolineTracer#writeTo(Joiner, Polyline[], boolean)}.
         * Those {@code Polyline} instances are temporary.
         */
        Polyline(final double[] data) {
            coordinates = data;
            size = data.length;
        }

        /**
         * Discards all coordinates in this polyline. This method does not clear
         * the {@link #opposite} polyline; it is caller's responsibility to do so.
         */
        final void clear() {
            opposite = null;
            size = 0;
        }

        /**
         * Returns whether this polyline is empty. This method is used only for {@code assert isEmpty()}
         * statement because of the check for {@code opposite == null}: an empty polyline should not have
         * a non-null {@link #opposite} value.
         */
        final boolean isEmpty() {
            return size == 0 & (opposite == null);
        }

        /**
         * Declares that the specified polyline will add points in the direction opposite to this polyline.
         * This happens when the polyline crosses the bottom side and the right side of a cell (assuming an
         * iteration from left to right and top to bottom).
         *
         * <p>This method is typically invoked in the following pattern (but this is not mandatory).
         * An important aspect is that {@code this} and {@code other} should be on perpendicular axes:</p>
         *
         * {@preformat java
         *     interpolateOnBottomSide(polylinesOnTop[x].attach(polylineOnLeft));
         * }
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
            final double[] swap = coordinates;
            coordinates = source.coordinates;
            size        = source.size;
            opposite    = source.opposite;
            if (opposite != null) {
                opposite.opposite = this;
            }
            source.clear();
            source.coordinates = swap;
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
                coordinates = Arrays.copyOf(coordinates, Math.max(Math.multiplyExact(size, 2), 32));
            }
            coordinates[size++] = x;
            coordinates[size++] = y;
        }

        /**
         * Returns a string representation of this {@code Polyline} for debugging purposes.
         */
        @Override
        public String toString() {
            return PathBuilder.toString(coordinates, size);
        }
    }

    /**
     * List of {@code Polyline} coordinates that have not yet been closed. Each {@code double[]} in this list is
     * a copy of a {@code Polyline} used by {@link Level}. Those copies are performed for saving data before they
     * are overwritten by next iterated cell.
     *
     * <h2>List indices and ordering of points</h2>
     * For a given {@code Unclosed} list, all {@code double[]} arrays at even indices shall have their points read
     * in reverse order and all {@code double[]} arrays at odd indices shall have their points read in forward order.
     * The list size must be even and the list may contain null elements when there is no data in the corresponding
     * iteration order. This convention makes easy to reverse the order of all points, simply by reversing the order
     * of {@code double[]} arrays: because even indices become odd and odd indices become even, points order are
     * implicitly reverted without the need to rewrite all {@code double[]} array contents.
     *
     * @see Level#partialPaths
     */
    @SuppressWarnings({"CloneableImplementsClone", "serial"})           // Not intended to be cloned or serialized.
    private static final class Unclosed extends ArrayList<double[]> {
        /**
         * The first points and last point in this list of polylines. By convention the coordinate having fraction
         * digits has all its bits inverted by the {@code ~} operator. May be {@code null} if a coordinate is NaN.
         * Do not modify {@link Point} field values, because those instances are keys in {@link Level#partialPaths}.
         */
        private Point firstPoint, lastPoint;

        /**
         * Creates a list of polylines initialized to the given items.
         * The given polylines and their opposite directions are cleared by this method.
         *
         * @param  polylineOnLeft  first polyline with points in forward order. Shall not be null.
         * @param  polylineOnTop    next polyline with points in reverse order, or {@code null} if none.
         */
        Unclosed(final Polyline polylineOnLeft, final Polyline polylineOnTop) {
            /*
             * Search for first point and last point by inspecting `Polyline`s in the order shown below.
             * The first 4 rows and the last 4 rows search for first point and last point respectively.
             * The empty rows in the middle are an intentional gap for creating a regular pattern that
             * we can exploit for 3 decisions that need to be done during the loop:
             *
             *     ✓ (index & 2) = 0    if using `polylineOnLeft` (otherwise `polylineOnTop`).
             *     ✓ (index % 3) = 0    if using `opposite` value of polyline (may be null).
             *     ✓ (index & 1) = 0    if fetching last point (otherwise fetch first point).
             *
             *  Index   Polyline   (iteration order)  !(i & 2)  !(i % 3)  !(i & 1)   Comment
             *  ────────────────────────────────────────────────────────────────────────────
             *   [0]    polylineOnLeft.opposite  (←)      ✓         ✓         ✓        (1)
             *   [1]    polylineOnLeft           (→)      ✓                            (2)
             *   [2]    polylineOnTop            (←)                          ✓        (1)
             *   [3]    polylineOnTop.opposite   (→)                ✓                  (2)
             *   [4]                                      ✓                   ✓
             *   |5]                                      ✓
             *   [6]    polylineOnTop.opposite   (→)                ✓         ✓        (3)
             *   [7]    polylineOnTop            (←)                                   (4)
             *   [8]    polylineOnLeft           (→)      ✓                   ✓        (3)
             *   [9]    polylineOnLeft.opposite  (←)      ✓         ✓                  (4)
             *
             * Comments:
             *   (1) Last  `Polyline` point is first `Unclosed` point because of reverse iteration order.
             *   (2) First `Polyline` point is first `Unclosed` point because of forward iteration order.
             *   (3) Last  `Polyline` point is last  `Unclosed` point because of forward iteration order.
             *   (4) First `Polyline` point is last  `Unclosed` point because of reverse iteration order.
             */
            int index = 0;
            do {
                Polyline polyline = ((index & 2) == 0) ? polylineOnLeft : polylineOnTop;  // See above table (column 4).
                if (index % 3 == 0 && polyline != null) polyline = polyline.opposite;     // See above table (column 5).
                if (polyline != null) {
                    int n = polyline.size;
                    if (n != 0) {
                        final double[] coordinates = polyline.coordinates;
                        final double x, y;
                        if (((index & 1) == 0)) {                          // See above table in comment (column 6).
                            y = coordinates[--n];
                            x = coordinates[--n];
                        } else {
                            x = coordinates[0];
                            y = coordinates[1];
                        }
                        final boolean isLastPoint = (index >= 6);          // See row [6] in above table.
                        if (Double.isFinite(x) && Double.isFinite(y)) {
                            final Point p = new Point((int) x, (int) y);
                            if (!Numerics.isInteger(x)) p.x = ~p.x;
                            if (!Numerics.isInteger(y)) p.y = ~p.y;
                            if (isLastPoint) {
                                lastPoint = p;
                                break;                                     // Done searching both points.
                            }
                            firstPoint = p;
                        } else if (isLastPoint) {
                            /*
                             * If the last point was NaN, check if it was also the case of first point.
                             * If yes, we will not be able to store this `Unclosed` in `partialPaths`
                             * because we have no point that we can use as key (it would be pointless
                             * to search for another point further in the `coordinates` array because
                             * that point could never be matched with another `Unclosed`). Leave this
                             * list empty for avoiding the copies done by `take(…)` calls. Instead,
                             * callers should write polylines in `Level.path` immediately.
                             */
                            if (firstPoint == null) return;
                            break;
                        }
                        /*
                         * Done searching the first point (may still be null if that point is NaN).
                         * Row [6] in above table is the first row for the search of last point.
                         */
                        index = 6;
                        continue;
                    }
                }
                if (++index == 4) {
                    // Found no non-empty polylines during search for first point. No need to continue searching.
                    return;
                }
            } while (index <= 9);
            /*
             * Copies coordinates only if at least one of `firstPoint` or `lastPoint` is a valid point.
             */
            take(polylineOnLeft.opposite);          // Point will be iterated in reverse order.
            take(polylineOnLeft);                   // Point will be iterated in forward order.
            if (polylineOnTop != null) {
                Polyline suffix = polylineOnTop.opposite;
                take(polylineOnTop);                // Inverse order. Set `polylineOnTop.opposite` to null.
                take(suffix);                       // Forward order.
            }
        }

        /**
         * Takes a copy of coordinate values of given polyline, then clears that polyline.
         */
        private void take(final Polyline polyline) {
            if (polyline != null && polyline.size != 0) {
                add(Arrays.copyOf(polyline.coordinates, polyline.size));
                polyline.clear();
            } else {
                add(null);                  // No data for iteration order at this position.
            }
        }

        /**
         * Returns {@code true} if the given point is equal to the start point or end point.
         * This is used in assertions for checking key validity in {@link Level#partialPaths}.
         */
        final boolean isExtremity(final Point key) {
            return key.equals(firstPoint) || key.equals(lastPoint);
        }

        /**
         * Associates this polyline to its two extremities in the given map. If other polylines already exist
         * for one or both extremities, then this polyline will be merged with previously existing polylines.
         * This method returns {@code true} if the polyline has been closed, in which case caller should store
         * the coordinates in {@link Level#path} immediately.
         *
         * @param  partialPaths  where to add or merge polylines.
         * @return {@code true} if this polyline became a closed polygon as a result of merge operation.
         */
        final boolean addOrMerge(final Map<Point,Unclosed> partialPaths) {
            final Unclosed before = partialPaths.remove(firstPoint);
            final Unclosed after  = partialPaths.remove(lastPoint);
            if (before != null) partialPaths.remove(addAll(before, true));
            if (after  != null) partialPaths.remove(addAll(after, false));
            if (firstPoint != null && firstPoint.equals(lastPoint)) {       // First/last points may have changed.
                partialPaths.remove(firstPoint);
                partialPaths.remove(lastPoint);
                return true;
            } else {
                // Intentionally replace previous values.
                if (firstPoint != null) partialPaths.put(firstPoint, this);
                if (lastPoint  != null) partialPaths.put(lastPoint,  this);
                return false;
            }
        }

        /**
         * Prepends or appends the given polylines to this list of polylines.
         * Points order will be changed as needed in order to match extremities.
         * The {@code other} instance should be forgotten after this method call.
         *
         * @param  other    the other polyline to append or prepend to this polyline.
         * @param  prepend  {@code true} for prepend operation, {@code false} for append.
         * @return extremity of {@code other} which has not been assigned to {@code this}.
         */
        private Point addAll(final Unclosed other, final boolean prepend) {
            assert ((size() | other.size()) & 1) == 0;      // Must have even number of elements in both lists.
            /*
             * In figures below, ● are the extremities to attach together.
             * `r` is a bitmask telling which polylines to reverse:
             * 1=this, 2=other, together with combinations 0=none and 3=other.
             */
            int r; if ( lastPoint != null &&  lastPoint.equals(other.firstPoint)) r = 0;    // ○──────● ●──────○
            else   if (firstPoint != null && firstPoint.equals(other.firstPoint)) r = 1;    // ●──────○ ●──────○
            else   if ( lastPoint != null &&  lastPoint.equals(other. lastPoint)) r = 2;    // ○──────● ○──────●
            else   if (firstPoint != null && firstPoint.equals(other. lastPoint)) r = 3;    // ●──────○ ○──────●
            else {
                // Should never happen because `other` has been obtained using a point of `this`.
                throw new AssertionError();
            }
            if (prepend) r ^= 3;                      // Swap order in above  ○──○ ○──○  figures.
            if ((r & 1) != 0)  this.reverse();
            if ((r & 2) != 0) other.reverse();
            if (prepend) {
                addAll(0, other);
                firstPoint = other.firstPoint;
                return other.lastPoint;
            } else {
                addAll(other);
                lastPoint = other.lastPoint;
                return other.firstPoint;
            }
        }

        /**
         * Reverse the order of all points. The last polyline will become the first polyline and vice-versa.
         * For each polyline, points will be iterated in opposite order. The trick on point order is done by
         * moving polylines at even indices to odd indices, and conversely (see class javadoc for convention
         * about even/odd indices).
         */
        private void reverse() {
            Collections.reverse(this);
            final Point swap = firstPoint;
            firstPoint = lastPoint;
            lastPoint = swap;
        }

        /**
         * Returns the content of this list as an array of {@link Polyline} instances.
         * {@code Polyline} instances at even index should be written with their points in reverse order.
         *
         * @see #writeTo(Joiner, Polyline[], boolean)
         */
        final Polyline[] toPolylines() {
            final Polyline[] polylines = new Polyline[size()];
            for (int i=0; i<polylines.length; i++) {
                final double[] coordinates = get(i);
                if (coordinates != null) {
                    polylines[i] = new Polyline(coordinates);
                }
            }
            return polylines;
        }
    }

    /**
     * Assembles arbitrary amount of {@link Polyline}s in a single Java2D {@link Shape} for a specific isoline level.
     * This class extends {@link PathBuilder} with two additional features: remove spikes caused by ambiguities, then
     * apply a {@link MathTransform} on all coordinate values.
     *
     * <h2>Spikes</h2>
     * If the shape delimited by given polylines has a part with zero width or height ({@literal i.e.} a spike),
     * truncates the polylines for removing that spike. This situation happens when some pixel values are exactly
     * equal to isoline value, as in the picture below:
     *
     * {@preformat text
     *     ●╌╌╌╲╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○
     *     ╎    ╲ ╎      ╎      ╎      ╎
     *     ╎     ╲╎      ╎   →  ╎      ╎
     *     ●╌╌╌╌╌╌●──────●──────●⤸╌╌╌╌╌○
     *     ╎     ╱╎      ╎   ←  ╎      ╎
     *     ╎    ╱ ╎      ╎      ╎      ╎
     *     ●╌╌╌╱╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○╌╌╌╌╌╌○
     * }
     *
     * The spike may appear or not depending on the convention adopted for strictly equal values.
     * In above picture, the spike appears because the convention used in this implementation is:
     *
     * <ul>
     *   <li>○: {@literal pixel value < isoline value}.</li>
     *   <li>●: {@literal pixel value ≥ isoline value}.</li>
     * </ul>
     *
     * If the following convention was used instead, the spike would not appear in above figure
     * (but would appear in different situations):
     *
     * <ul>
     *   <li>○: {@literal pixel value ≤ isoline value}.</li>
     *   <li>●: {@literal pixel value > isoline value}.</li>
     * </ul>
     *
     * This class detects and removes those spikes for avoiding convention-dependent results.
     * We assume that spikes can appear only at the junction between two {@link Polyline} instances.
     * Rational: having a spike require that we move forward then backward on the same coordinates,
     * which is possible only with a non-null {@link Polyline#opposite} field.
     */
    private static final class Joiner extends PathBuilder {
        /**
         * Final transform to apply on coordinates, or {@code null} if none.
         */
        private final MathTransform gridToCRS;

        /**
         * Creates an initially empty set of isoline shapes.
         */
        Joiner(final MathTransform gridToCRS) {
            this.gridToCRS = gridToCRS;
        }

        /**
         * Detects and removes spikes for avoiding convention-dependent results.
         * See {@link Joiner} class-javadoc for a description of the problem.
         *
         * <p>We perform the analysis in this method instead of in {@link #filterFull(double[], int)} on the
         * the assumption that spikes can appear only between two calls to {@code append(…)} (because having a
         * spike require that we move forward then backward on the same coordinates, which happen only with two
         * distinct {@link Polyline} instances). It reduce the amount of coordinates to examine since we can check
         * only the extremities instead of looking for spikes anywhere in the array.</p>
         *
         * @param  coordinates  the coordinates to filter. Values can be modified in-place.
         * @param  lower        index of first coordinate to filter. Always even.
         * @param  upper        index after the last coordinate to filter. Always even.
         * @return number of valid coordinates after filtering.
         */
        @Override
        protected int filterChunk(final double[] coordinates, final int lower, final int upper) {
            int spike0 = lower;         // Will be index where (x,y) become different than (xo,yo).
            int spike1 = lower;         // Idem, but searching forward instead of backward.
            if (spike1 < upper) {
                final double xo = coordinates[spike1++];
                final double yo = coordinates[spike1++];
                int equalityMask = 3;                   // Bit 1 set if (x == xo), bit 2 set if (y == yo).
                while (spike1 < upper) {
                    final int before = equalityMask;
                    if (coordinates[spike1++] != xo) equalityMask &= ~1;
                    if (coordinates[spike1++] != yo) equalityMask &= ~2;
                    if (equalityMask == 0) {
                        equalityMask = before;              // For keeping same search criterion.
                        spike1 -= Polyline.DIMENSION;       // Restore previous position before mismatch.
                        break;
                    }
                }
                while (spike0 > 0) {
                    if (coordinates[--spike0] != yo) equalityMask &= ~2;
                    if (coordinates[--spike0] != xo) equalityMask &= ~1;
                    if (equalityMask == 0) {
                        spike0 += Polyline.DIMENSION;
                        break;
                    }
                }
            }
            /*
             * Here we have range of indices where the polygon has a width or height of zero.
             * Search for a common point, then truncate at that point. Indices are like below:
             *
             *     0       spike0    lower          spike1         upper
             *     ●────●────●────●────●────●────●────●────●────●────●
             *                    └╌╌╌╌remove╌╌╌╌┘
             * where:
             *  - `lower` and `spike0` are inclusive.
             *  - `upper` and `spike1` are exclusive.
             *  - the region to remove are sowewhere between `spike0` and `spike1`.
             */
            final int limit = spike1;
            int base;
            while ((base = spike0 + 2*Polyline.DIMENSION) < limit) {    // Spikes exist only with at least 3 points.
                final double xo = coordinates[spike0++];
                final double yo = coordinates[spike0++];
                spike1 = limit;
                do {
                    if (coordinates[spike1 - 2] == xo && coordinates[spike1 - 1] == yo) {
                        /*
                         * Remove points between the common point (xo,yo). The common point is kept on the
                         * left side (`spike0` is already after that point) and removed on the right side.
                         */
                        System.arraycopy(coordinates, spike1, coordinates, spike0, upper - spike1);
                        return upper - (spike1 - spike0);
                    }
                } while ((spike1 -= Polyline.DIMENSION) > base);
            }
            return upper;       // Nothing to remove.
        }

        /**
         * Applies user-specified coordinate transform on all points of the whole polyline.
         * This method is invoked after {@link #filterChunk(double[], int, int)}.
         */
        @Override
        protected int filterFull(final double[] coordinates, final int upper) throws TransformException {
            if (gridToCRS != null) {
                gridToCRS.transform(coordinates, 0, coordinates, 0, upper / Polyline.DIMENSION);
            }
            return upper;
        }
    }

    /**
     * Writes all given polylines to the specified path builder. Null {@code Polyline} instances are ignored.
     * {@code Polyline} instances at even index are written with their points in reverse order.
     * All given polylines are cleared by this method.
     *
     * @param  path       where to write the polylines, or {@code null} if not yet created.
     * @param  polylines  the polylines to write.
     * @param  close      whether to close the polygon.
     * @return the given path builder, or a newly created builder if the argument was null.
     * @throws TransformException if the {@link #gridToCRS} transform can not be applied.
     */
    private Joiner writeTo(Joiner path, final Polyline[] polylines, final boolean close) throws TransformException {
        for (int pi=0; pi < polylines.length; pi++) {
            final Polyline p = polylines[pi];
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
