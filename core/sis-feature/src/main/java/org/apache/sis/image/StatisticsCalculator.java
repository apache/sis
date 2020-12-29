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
package org.apache.sis.image;

import java.awt.Shape;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import java.util.stream.Collector;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Computes statistics on all pixel values of an image. The results are stored in an array
 * of {@link Statistics} objects (one per band) in a property named {@value #STATISTICS_KEY}.
 * The statistics can be computed in parallel or sequentially for non thread-safe images.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class StatisticsCalculator extends AnnotatedImage {
    /**
     * Creates a new calculator.
     *
     * @param  image            the image for which to compute statistics.
     * @param  areaOfInterest   pixel coordinates of AOI, or {@code null} for the whole image.
     * @param  parallel         whether parallel execution is authorized.
     * @param  failOnException  whether errors occurring during computation should be propagated.
     */
    StatisticsCalculator(final RenderedImage image, final Shape areaOfInterest,
                         final boolean parallel, final boolean failOnException)
    {
        super(image, areaOfInterest, parallel, failOnException);
    }

    /**
     * Returns the name of the property which is computed by this image.
     */
    @Override
    protected String getComputedPropertyName() {
        return STATISTICS_KEY;
    }

    /**
     * Creates the objects where to add sample values for computing statistics.
     * We will have one accumulator for each band in the source image.
     * This is used for both sequential and parallel executions.
     */
    private static Statistics[] createAccumulator(final int numBands) {
        final Statistics[] stats = new Statistics[numBands];
        for (int i=0; i<numBands; i++) {
            stats[i] = new Statistics(Vocabulary.formatInternational(Vocabulary.Keys.Band_1, i));
        }
        return stats;
    }

    /**
     * Computes statistics using the given iterator and accumulates the result for all bands.
     * This method is invoked in both sequential and parallel case. In the sequential case it
     * is invoked for the whole image; in the parallel case it is invoked for only one tile.
     *
     * <p>This method may be invoked concurrently by many threads.
     * Fields used by this method shall be thread-safe when not modified.</p>
     *
     * @param accumulator  where to accumulate the statistics results.
     * @param it           the iterator on a raster or on the whole image.
     */
    private void compute(final Statistics[] accumulator, final PixelIterator it) {
        double[] samples = null;
        while (it.next()) {
            if (areaOfInterest == null || areaOfInterest.contains(it.x, it.y)) {
                samples = it.getPixel(samples);                 // Get values in all bands.
                for (int i=0; i<accumulator.length; i++) {
                    accumulator[i].accept(samples[i]);
                }
            }
        }
    }

    /**
     * Computes the statistics on the image using a single thread. This is used for testing purposes, or when
     * the image has only one tile, or when the implementation of {@link RenderedImage#getTile(int, int)} may
     * be non thread-safe.
     */
    @Override
    protected Object computeSequentially() {
        final PixelIterator it = new PixelIterator.Builder().setRegionOfInterest(boundsOfInterest).create(source);
        final Statistics[] accumulator = createAccumulator(it.getNumBands());
        compute(accumulator, it);
        return accumulator;
    }

    /**
     * Invoked when a property of the given name has been requested and that property is cached.
     * The property should be cloned before to be returned to the user in order to protect this image state.
     */
    @Override
    protected Object cloneProperty(final String name, final Object value) {
        final Statistics[] result = ((Statistics[]) value).clone();
        for (int i=0; i<result.length; i++) {
            result[i] = result[i].clone();
        }
        return result;
    }

    /**
     * Returns the function to execute for parallel computation of statistics,
     * together with other required functions (supplier of accumulator, combiner, finisher).
     */
    @Override
    protected Collector<Raster, Statistics[], Statistics[]> collector() {
        return Collector.of(this::createAccumulator, this::compute, StatisticsCalculator::combine);
    }

    /**
     * Invoked for creating the object holding the information to be computed by a single thread.
     * This method will be invoked for each worker thread before the worker starts its execution.
     *
     * @return a thread-local variable holding information computed by a single thread.
     *         May be {@code null} is such objects are not needed.
     */
    private Statistics[] createAccumulator() {
        return createAccumulator(source.getSampleModel().getNumBands());
    }

    /**
     * Invoked after a thread finished to process all its tiles and wants to combine its result with the
     * result of another thread. This method is invoked only if {@link #createAccumulator()} returned a non-null value.
     * This method does not need to be thread-safe; synchronizations will be done by the caller.
     *
     * @param  previous  the result of another thread (never {@code null}).
     * @param  computed  the result computed by current thread (never {@code null}).
     * @return combination of the two results. May be one of the {@code previous} or {@code computed} instances.
     */
    private static Statistics[] combine(final Statistics[] previous, final Statistics[] computed) {
        for (int i=0; i<computed.length; i++) {
            previous[i].combine(computed[i]);
        }
        return previous;
    }

    /**
     * Executes this operation on the given tile. This method may be invoked from any thread.
     * If an exception occurs during computation, that exception will be logged or wrapped in
     * an {@link ImagingOpException} by the caller.
     *
     * @param  accumulator  the thread-local variable created by {@link #createAccumulator()}.
     * @param  tile         the tile on which to perform a computation.
     * @throws RuntimeException if the calculation failed.
     */
    private void compute(final Statistics[] accumulator, final Raster tile) {
        compute(accumulator, new PixelIterator.Builder().setRegionOfInterest(boundsOfInterest).create(tile));
    }
}
