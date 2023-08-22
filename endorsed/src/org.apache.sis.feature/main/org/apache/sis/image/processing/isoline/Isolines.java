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
package org.apache.sis.image.processing.isoline;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.util.TreeMap;
import java.util.NavigableMap;
import java.util.function.BiConsumer;
import java.util.concurrent.Future;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.image.RenderedImage;
import org.opengis.coverage.grid.SequenceType;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Debug;

import static org.apache.sis.image.processing.isoline.Tracer.UPPER_LEFT;
import static org.apache.sis.image.processing.isoline.Tracer.UPPER_RIGHT;
import static org.apache.sis.image.processing.isoline.Tracer.LOWER_RIGHT;


/**
 * Creates isolines at specified levels from grid values provided in a {@link RenderedImage}.
 * Isolines are created by calls to the {@link #generate generate(…)} static method.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see <a href="https://en.wikipedia.org/wiki/Marching_squares">Marching squares on Wikipedia</a>
 *
 * @since 1.1
 */
public final class Isolines {
    /**
     * Isoline data for each level, sorted in ascending order of {@link Tracer.Level#value}.
     */
    private final Tracer.Level[] levels;

    /**
     * A consumer to notify about the current state of isoline generation, or {@code null} if none.
     * This is used for debugging purposes only. This field is temporarily set to a non-null value
     * when using {@code StepsViewer} (in test package) for following an isoline generation step
     * by step.
     */
    @Debug
    private static final BiConsumer<String,Isolines> LISTENER = null;

    /**
     * Creates an initially empty set of isolines for the given levels. The given {@code values}
     * array should be one of the arrays validated by {@link #cloneAndSort(double[][])}.
     */
    private Isolines(final Tracer tracer, final int band, final double[] values, final int width) {
        levels = new Tracer.Level[values.length];
        for (int i=0; i<values.length; i++) {
            levels[i] = tracer.new Level(band, values[i], width);
        }
    }

    /**
     * Ensures that the given arrays are sorted and contains no NaN value.
     */
    private static double[][] cloneAndSort(double[][] levels) {
        levels.clone();
        for (int j=0; j<levels.length; j++) {
            double[] values = levels[j];
            ArgumentChecks.ensureNonNullElement("levels", j, values);
            values = values.clone();
            Arrays.sort(values);
            int n = values.length;
            while (n > 0 && Double.isNaN(values[n-1])) n--;
            for (int i=n; --i>0;) {
                if (values[i] == values[i-1]) {
                    // Remove duplicated elements. May replace -0 by +0.
                    System.arraycopy(values, i, values, i-1, n-- - i);
                }
            }
            levels[j] = ArraysExt.resize(values, n);
        }
        return levels;
    }

    /**
     * Sets the specified bit on {@link Tracer.Level#isDataAbove} for all levels lower than given value.
     *
     * <h4>How strict equalities are handled</h4>
     * Sample values exactly equal to the isoline value are handled as if they were greater. It does not matter
     * for interpolations: we could flip this convention randomly, the interpolated points would still the same.
     * However, it could change the way line segments are assembled in a single polyline, but the algorithm stay
     * consistent if we always apply the same rule for all points.
     *
     * <h4>How NaN values are handled</h4>
     * {@link Double#NaN} sample values are considered higher than any level values. The algorithm does not need
     * special attention for those values; bit patterns will be computed in a consistent way, and interpolations
     * will produce NaN values and append them to polylines like real values.  Those NaN values will be filtered
     * out in the final stage, when copying coordinates in {@link Path2D} objects.
     *
     * @param  value a sample values from the image.
     * @param  bit   {@value Tracer#UPPER_LEFT}, {@value Tracer#UPPER_RIGHT},
     *               {@value Tracer#LOWER_LEFT} or {@value Tracer#LOWER_RIGHT}.
     *
     * @see Tracer.Level#nextColumn()
     */
    private void setMaskBit(final double value, final int bit) {
        for (final Tracer.Level level : levels) {
            if (level.value > value) break;                 // See above javadoc for NaN handling.
            level.isDataAbove |= bit;
        }
    }

    /**
     * Generates isolines at the specified levels computed from data provided by the given image.
     * Isolines will be created for every bands in the given image.
     * This method performs all computation sequentially in current thread.
     *
     * @param  data       image providing source values.
     * @param  levels     values for which to compute isolines. An array should be provided for each band.
     *                    If there is more bands than {@code levels.length}, the last array is reused for
     *                    all remaining bands.
     * @param  gridToCRS  transform from pixel coordinates to geometry coordinates, or {@code null} if none.
     *                    Integer source coordinates are located at pixel centers.
     * @return the isolines for each band in the given image.
     * @throws TransformException if an interpolated point cannot be transformed using the given transform.
     */
    public static Isolines[] generate(final RenderedImage data, final double[][] levels,
                                      final MathTransform gridToCRS) throws TransformException
    {
        ArgumentChecks.ensureNonNull("data",   data);
        ArgumentChecks.ensureNonNull("levels", levels);
        return flush(generate(iterators().create(data), cloneAndSort(levels), gridToCRS));
    }

    /**
     * Generates isolines in background using an arbitrary number of processors.
     * This method returns immediately (i.e. the current thread is not used for isoline computation).
     * The result will become available at a later time in the {@link Future} object.
     *
     * @param  data       image providing source values.
     * @param  levels     values for which to compute isolines. An array should be provided for each band.
     *                    If there is more bands than {@code levels.length}, the last array is reused for
     *                    all remaining bands.
     * @param  gridToCRS  transform from pixel coordinates to geometry coordinates, or {@code null} if none.
     *                    Integer source coordinates are located at pixel centers.
     * @return a {@code Future} representing pending completion of isoline computation.
     */
    public static Future<Isolines[]> parallelGenerate(final RenderedImage data, final double[][] levels,
                                                      final MathTransform gridToCRS)
    {
        ArgumentChecks.ensureNonNull("data",   data);
        ArgumentChecks.ensureNonNull("levels", levels);
        return new Parallelized(data, cloneAndSort(levels), gridToCRS).execute();
    }

    /**
     * Merges results of two partial computations (tiles).
     * This is invoked only in multi-threaded isoline computation.
     * The {@code isolines} instances are modified in-place.
     * The {@code neighbor} instances can be discarded after the merge.
     *
     * @param  isolines  the first set of isolines to merge.
     * @param  neighbor  another set of isolines close to the first set.
     *
     * @see Parallelized
     */
    static void merge(final Isolines[] isolines, final Isolines[] neighbor) throws TransformException {
        for (int i=0; i<isolines.length; i++) {
            final Isolines target = isolines[i];
            final Isolines source = neighbor[i];
            for (int j=0; j<target.levels.length; j++) {
                target.levels[j].merge(source.levels[j]);
            }
        }
    }

    /**
     * Returns a provider of {@link PixelIterator} suitable to isoline computations.
     * It is critical that iterators use {@link SequenceType#LINEAR} iterator order.
     */
    static PixelIterator.Builder iterators() {
        return new PixelIterator.Builder().setIteratorOrder(SequenceType.LINEAR);
    }

    /**
     * Creates all polylines that were still pending. This method should be the very last step,
     * when all other computations finished. Pending polylines are sequences of points not yet
     * stored in geometry objects because they were waiting to see if additional points would
     * close them as polygons. This {@code flush()} method is invoked when we know that it will
     * not be the case because there are no more points to add.
     *
     * @param  isolines  the isolines for which to write pending polylines.
     * @return the {@code isolines} array, returned for convenience.
     * @throws TransformException if an error occurred during polylines creation.
     */
    static Isolines[] flush(final Isolines[] isolines) throws TransformException {
        for (final Isolines isoline : isolines) {
            for (final Tracer.Level level : isoline.levels) {
                level.flush();
            }
        }
        return isolines;
    }

    /**
     * Generates isolines for the specified levels in the region traversed by the given iterator.
     * This is the implementation of {@link #generate(RenderedImage, double[][], MathTransform)}
     * but potentially on a sub-region for parallel "fork-join" execution.
     */
    static Isolines[] generate(final PixelIterator iterator, final double[][] levels, final MathTransform gridToCRS)
            throws TransformException
    {
        /*
         * Prepares a window of size 2×2 pixels over pixel values. Window elements are traversed
         * by incrementing indices in following order: band, column, row. The window content will
         * be written in this method and read by Tracer.
         */
        final int numBands = iterator.getNumBands();
        final double[] window = new double[numBands * 4];
        final Tracer tracer = new Tracer(window, numBands, iterator.getDomain(), gridToCRS);
        /*
         * Prepare the set of isolines for each band in the image.
         * The number of cells on the horizontal axis is one less
         * than the image width.
         */
        final int width = iterator.getDomain().width - 1;
        final Isolines[] isolines = new Isolines[numBands];
        {   // For keeping variable locale.
            double[] levelValues = ArraysExt.EMPTY_DOUBLE;
            for (int b=0; b<numBands; b++) {
                if (b < levels.length) {
                    levelValues = levels[b];
                    ArgumentChecks.ensureNonNullElement("levels", b, levelValues);
                    levelValues = levelValues.clone();
                }
                isolines[b] = new Isolines(tracer, b, levelValues, width);
            }
        }
        /*
         * Cache sample values on the top row. Those values are reused by the row just below row
         * of cached values. This array is updated during iteration with values of current cell.
         */
        final double[] pixelValues = new double[numBands];
        final double[] valuesOnPreviousRow = new double[numBands * (width+1)];
        for (int i=0; i < valuesOnPreviousRow.length; i += numBands) {
            if (!iterator.next()) return isolines;
            System.arraycopy(iterator.getPixel(pixelValues), 0, valuesOnPreviousRow, i, numBands);
        }
        /*
         * Compute isolines for all bands. Iteration over bands must be the innermost loop because
         * data layout in buffer is band index varying fastest, then column index, then row index.
         */
        final int twoPixels = numBands * 2;
        final int lastPixel = numBands * 3;
abort:  while (iterator.next()) {
            /*
             * Process the first cell of a new row:
             *
             *  - Get values on the 4 corners.
             *  - Save value of lower-left corner for use by next row.
             *  - Initialize `Tracer.Level.isDataAbove` bits for all levels.
             *  - Interpolate the first cell.
             */
            System.arraycopy(valuesOnPreviousRow, 0, window, 0, twoPixels);
            System.arraycopy(iterator.getPixel(pixelValues), 0, window, twoPixels, numBands);
            if (!iterator.next()) break;
            System.arraycopy(iterator.getPixel(pixelValues), 0, window, lastPixel, numBands);
            System.arraycopy(window, twoPixels, valuesOnPreviousRow, 0, twoPixels);
            for (int i=0, flag = UPPER_LEFT; flag <= LOWER_RIGHT; flag <<= 1) {
                for (int b=0; b<numBands; b++) {        // Must be the inner loop (see above comment).
                    isolines[b].setMaskBit(window[i++], flag);
                }
            }
            for (final Isolines iso : isolines) {
                for (final Tracer.Level level : iso.levels) {
                    level.interpolate();
                }
            }
            /*
             * Process all pixels on a row after the first column. We can reuse the bitmask of previous
             * iteration with a simple bit shift operation. This is done by the `nextColumn()` call.
             * The series for `System.arraycopy(…)` calls are for moving 3 pixel values of previous
             * iteration that we can reuse, then fetch the only new value from the iterator.
             */
            for (tracer.x = 1; tracer.x < width; tracer.x++) {
                final int offsetOnPreviousRow = (tracer.x + 1) * numBands;
                if (!iterator.next()) break abort;                              // Should never abort
                if (numBands == 1) {                                            // Optimization for a common case
                    window[2] = window[3];                                      // Lower-right → Lower-left
                    window[0] = window[1];                                      // Upper-right → Upper-left
                    window[1] = valuesOnPreviousRow[offsetOnPreviousRow];       // Take upper-right from previous row
                    window[3] = valuesOnPreviousRow[offsetOnPreviousRow] = iterator.getSampleDouble(0);
                } else {
                    System.arraycopy(window, numBands,  window, 0,         numBands);   // Upper-right → Upper-left
                    System.arraycopy(window, lastPixel, window, twoPixels, numBands);   // Lower-right → Lower-left
                    System.arraycopy(valuesOnPreviousRow, offsetOnPreviousRow, window, numBands, numBands);
                    System.arraycopy(iterator.getPixel(pixelValues), 0, window, lastPixel, numBands);
                    System.arraycopy(window, lastPixel, valuesOnPreviousRow, offsetOnPreviousRow, numBands);
                }
                for (int b=0; b<numBands; b++) {
                    final Isolines iso = isolines[b];
                    for (final Tracer.Level level : iso.levels) {
                        level.nextColumn();
                    }
                    iso.setMaskBit(window[numBands  + b], UPPER_RIGHT);
                    iso.setMaskBit(window[lastPixel + b], LOWER_RIGHT);
                    for (final Tracer.Level level : iso.levels) {
                        level.interpolate();
                    }
                }
            }
            /*
             * Finished iteration on a row. Clear flags and update position before to move to next row.
             * If there is listeners to notify (for debugging purposes), notify them.
             */
            for (int b=0; b<numBands; b++) {
                for (final Tracer.Level level : isolines[b].levels) {
                    level.finishedRow();
                }
                if (LISTENER != null) {
                    final int y = tracer.y;
                    final int h = iterator.getDomain().height;
                    LISTENER.accept(String.format("After row %d of %d (%3.1f%%)", y, h, 100f*y/h), isolines[b]);
                }
            }
            tracer.x = 0;
            tracer.y++;
        }
        /*
         * Finished iteration over the whole image.
         */
        for (int b=0; b<numBands; b++) {
            for (final Tracer.Level level : isolines[b].levels) {
                level.finish();
            }
            if (LISTENER != null) {
                LISTENER.accept("Finished band " + b, isolines[b]);
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
        for (final Tracer.Level level : levels) {
            final Shape path = level.shape;
            if (path != null) {
                paths.put(level.value, path);
            }
        }
        return paths;
    }

    /**
     * Returns the isolines for each band, then for each values in the band.
     *
     * @param  isolines  result of {@code generate(…)} or {@code parallelGenerate(…)} method call.
     * @return isoline shapes for each values in each band.
     */
    static NavigableMap<Double,Shape>[] toArray(final Isolines[] isolines) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        final NavigableMap<Double,Shape>[] result = new NavigableMap[isolines.length];
        for (int i=0; i<result.length; i++) {
            result[i] = isolines[i].polylines();
        }
        return result;
    }

    /**
     * Returns the isolines for each band, then for each values in the band.
     *
     * @param  isolines  result of {@code generate(…)} or {@code parallelGenerate(…)} method call.
     * @return isoline shapes for each values in each band.
     */
    public static List<NavigableMap<Double,Shape>> toList(final Isolines[] isolines) {
        return Arrays.asList(toArray(isolines));
    }

    /**
     * Returns deferred isolines for each band, then for each values in the band.
     * The {@link Future} result is requested the first time that {@link List#get(int)} is invoked.
     *
     * @param  isolines  result of {@code generate(…)} or {@code parallelGenerate(…)} method call.
     * @return isoline shapes for each values in each band.
     */
    public static List<NavigableMap<Double,Shape>> toList(final Future<Isolines[]> isolines) {
        return new Result(isolines);
    }

    /**
     * Returns the pixel coordinates of all level, for debugging purposes only.
     * The {@link #gridToCRS} transform is <em>not</em> applied by this method.
     * For avoiding confusing behavior, that transform should be null.
     *
     * @return the pixel coordinates.
     */
    @Debug
    final Map<PolylineStage,Path2D> toRawPath() {
        final Map<PolylineStage,Path2D> appendTo = new EnumMap<>(PolylineStage.class);
        for (final Tracer.Level level : levels) {
            level.toRawPath(appendTo);
        }
        return appendTo;
    }
}
