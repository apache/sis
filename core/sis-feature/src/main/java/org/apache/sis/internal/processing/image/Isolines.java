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
import java.util.TreeMap;
import java.util.NavigableMap;
import java.nio.DoubleBuffer;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.image.RenderedImage;
import org.opengis.coverage.grid.SequenceType;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.image.TransferType;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;

import static org.apache.sis.internal.processing.image.IsolineTracer.UPPER_LEFT;
import static org.apache.sis.internal.processing.image.IsolineTracer.UPPER_RIGHT;
import static org.apache.sis.internal.processing.image.IsolineTracer.LOWER_RIGHT;


/**
 * Creates isolines at specified levels from grid values provided in a {@link RenderedImage}.
 * Isolines are created by calls to the {@link #generate generate(…)} static method.
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
public final class Isolines {
    /**
     * Isoline data for each level, sorted in ascending order of {@link IsolineTracer.Level#value}.
     */
    private final IsolineTracer.Level[] levels;

    /**
     * Creates an initially empty set of isolines for the given levels.
     * The given array should be a clone of user-provided array because
     * this constructor may modify it in-place.
     */
    private Isolines(final IsolineTracer tracer, final double[] values, final int width) {
        Arrays.sort(values);
        int n = values.length;
        while (n > 0 && Double.isNaN(values[n-1])) n--;
        for (int i=n; --i>0;) {
            if (values[i] == values[i-1]) {
                // Remove duplicated elements. May replace -0 by +0.
                System.arraycopy(values, i, values, i-1, n-- - i);
            }
        }
        levels = new IsolineTracer.Level[n];
        for (int i=0; i<n; i++) {
            levels[i] = tracer.new Level(values[i], width);
        }
    }

    /**
     * Sets the specified bit on {@link IsolineTracer.Level#isDataAbove} for all levels lower than buffer value.
     * The {@code window} buffer shall be positioned on the value to read, consistently with {@code bit} value.
     * After this method call, the buffer position will be incremented to the next band if there is more bands
     * to read, or to the next pixel otherwise.
     *
     * <h4>How strict equalities are handled</h4>
     * Sample values exactly equal to the isoline value are handled as if they were greater. It does not matter
     * for interpolations: we could flip this convention randomly, the interpolated points would still the same.
     * However it could change the way line segments are assembled in a single polyline, but the algorithm stay
     * consistent if we always apply the same rule for all points.
     *
     * <h4>How NaN values are handled</h4>
     * {@link Double#NaN} sample values are considered higher than any level values. The algorithm does not need
     * special attention for those values; bit patterns will be computed in a consistent way, and interpolations
     * will produce NaN values and append them to polylines like real values.  Those NaN values will be filtered
     * out in the final stage, when copying coordinates in {@link Path2D} objects.
     *
     * @param  data  a 2×2 view on pixel values in the image.
     * @param  bit   {@value IsolineTracer#UPPER_LEFT}, {@value IsolineTracer#UPPER_RIGHT},
     *               {@value IsolineTracer#LOWER_LEFT} or {@value IsolineTracer#LOWER_RIGHT}.
     */
    private void setMaskBit(final DoubleBuffer data, final int bit) {
        final double value = data.get();
        for (final IsolineTracer.Level level : levels) {
            if (level.value > value) break;                 // See above javadoc for NaN handling.
            level.isDataAbove |= bit;
        }
    }

    /**
     * Generates isolines at the specified levels computed from data provided by the given image.
     * Isolines will be created for every bands in the given image.
     *
     * @param  data       image providing source values.
     * @param  levels     values for which to compute isolines. An array should be provided for each band.
     *                    If there is more bands than {@code levels.length}, the last array is reused for
     *                    all remaining bands.
     * @param  gridToCRS  transform from pixel coordinates to geometry coordinates, or {@code null} if none.
     * @return the isolines for each band in the given image.
     * @throws TransformException if an interpolated point can not be transformed using the given transform.
     */
    public static Isolines[] generate(final RenderedImage data, final double[][] levels,
                                      MathTransform gridToCRS) throws TransformException
    {
        ArgumentChecks.ensureNonNull("data",   data);
        ArgumentChecks.ensureNonNull("levels", levels);
        {   // For keeping variable locale.
            final MathTransform gridToImage = MathTransforms.translation(data.getMinX(), data.getMinY());
            if (gridToCRS == null) {
                gridToCRS = gridToImage;
            } else {
                ArgumentChecks.ensureDimensionsMatch("gridToCRS", 2, 2, gridToCRS);
                gridToCRS = MathTransforms.concatenate(gridToImage, gridToCRS);
            }
        }
        final PixelIterator iterator = new PixelIterator.Builder().setIteratorOrder(SequenceType.LINEAR)
                                                        .setWindowSize(new Dimension(2,2)).create(data);
        /*
         * A window of size 2×2 pixels over pixel values.
         */
        final PixelIterator.Window<DoubleBuffer> window = iterator.createWindow(TransferType.DOUBLE);
        final DoubleBuffer buffer = window.values;
        final int numBands = iterator.getNumBands();
        final IsolineTracer tracer = new IsolineTracer(buffer, numBands, gridToCRS);
        /*
         * Prepare the set of isolines for each band in the image.
         * The number of cells on the horizontal axis is one less
         * than the image width.
         */
        final int width = data.getWidth() - 1;
        final Isolines[] isolines = new Isolines[numBands];
        {   // For keeping variable locale.
            double[] levelValues = ArraysExt.EMPTY_DOUBLE;
            for (int b=0; b<numBands; b++) {
                if (b < levels.length) {
                    levelValues = levels[b];
                    ArgumentChecks.ensureNonNullElement("levels", b, levelValues);
                    levelValues = levelValues.clone();
                }
                isolines[b] = new Isolines(tracer, levelValues, width);
            }
        }
        /*
         * Compute isolines for all bands. Iteration over bands must be the innermost loop because
         * data layout in buffer is band index varying fastest, then column index, then row index.
         */
        final int lastPixel = numBands * 3;
abort:  while (iterator.next()) {
            /*
             * First pixel of a new row.
             */
            window.update();        // Also reset buffer position to zero.
            for (int flag = UPPER_LEFT; flag <= LOWER_RIGHT; flag <<= 1) {
                for (int b=0; b<numBands; b++) {        // Must be the inner loop (see above comment).
                    isolines[b].setMaskBit(buffer, flag);
                }
            }
            for (int b=0; b<numBands; b++) {
                buffer.position(b);
                for (final IsolineTracer.Level level : isolines[b].levels) {
                    level.interpolate();
                }
            }
            /*
             * All pixels on a row after the first column. We can reuse the bitmask of previous
             * iteration with a simple bit shift operation.
             */
            for (tracer.x = 1; tracer.x < width; tracer.x++) {
                if (!iterator.next()) break abort;
                window.update();
                for (int b=0; b<numBands; b++) {
                    final Isolines iso = isolines[b];
                    for (final IsolineTracer.Level level : iso.levels) {
                        level.nextColumn();
                    }
                    // TODO! remove cast on JDK9.
                    iso.setMaskBit((DoubleBuffer) buffer.position(numBands  + b), UPPER_RIGHT);
                    iso.setMaskBit((DoubleBuffer) buffer.position(lastPixel + b), LOWER_RIGHT);
                    buffer.position(b);
                    for (final IsolineTracer.Level level : iso.levels) {
                        level.interpolate();
                    }
                }
            }
            /*
             * Finished iteration on a row. Clear flags and update position
             * before to move to next row.
             */
            for (int b=0; b<numBands; b++) {
                for (final IsolineTracer.Level level : isolines[b].levels) {
                    level.finishedRow();
                }
            }
            tracer.x = 0;
            tracer.y++;
        }
        /*
         * Finished iteration over the whole image.
         */
        for (int b=0; b<numBands; b++) {
            for (final IsolineTracer.Level level : isolines[b].levels) {
                level.finish();
            }
        }
        return isolines;
    }

    /**
     * Returns the polylines for each level specified to the {@link #generate generate(…)} method.
     *
     * @return the polylines for each level.
     */
    public final NavigableMap<Double,Shape> polylines() {
        final TreeMap<Double,Shape> paths = new TreeMap<>();
        for (final IsolineTracer.Level level : levels) {
            final Shape path = level.shape;
            if (path != null) {
                paths.put(level.value, path);
            }
        }
        return paths;
    }
}
