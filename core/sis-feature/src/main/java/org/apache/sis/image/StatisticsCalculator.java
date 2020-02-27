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

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.coverage.j2d.PropertyCalculator;


/**
 * Computes statistics on all pixel values of an image.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class StatisticsCalculator extends PropertyCalculator<Statistics[]> {
    /**
     * Name of the property computed by this class.
     */
    static final String PROPERTY_NAME = "statistics";

    /**
     * Creates a new calculator.
     *
     * @param  image  the image for which to compute statistics.
     */
    StatisticsCalculator(final RenderedImage image) {
        super(image);
    }

    /**
     * Returns the name of the property which is computed by this image.
     */
    @Override
    protected String getComputedPropertyName() {
        return PROPERTY_NAME;
    }

    /**
     * Invoked for creating the objects holding the statistics to be computed by a single thread.
     * This method will be invoked for each worker threads.
     */
    @Override
    public Statistics[] get() {
        final Statistics[] stats = new Statistics[source.getSampleModel().getNumBands()];
        for (int i=0; i<stats.length; i++) {
            stats[i] = new Statistics(Vocabulary.formatInternational(Vocabulary.Keys.Band_1, i));
        }
        return stats;
    }

    /**
     * Invoked after a thread finished to process all its tiles and wants to combine its statistics
     * with the ones computed by another thread. This method does not need to be thread-safe;
     * synchronizations will be done by the caller.
     *
     * @param  previous  the statistics computed by another thread (never {@code null}).
     * @param  computed  the statistics computed by current thread (never {@code null}).
     * @return combination of the two results, stored in {@code previous} instances.
     */
    @Override
    public Statistics[] apply(final Statistics[] previous, final Statistics[] computed) {
        for (int i=0; i<computed.length; i++) {
            previous[i].combine(computed[i]);
        }
        return previous;
    }

    /**
     * Invoked for computing statistics on all pixel values in a raster.
     *
     * @param  accumulator  where to store statistics.
     * @param  tile         the tile for which to compute statistics.
     */
    @Override
    public void accept(final Statistics[] accumulator, final Raster tile) {
        final PixelIterator it = new PixelIterator.Builder().create(tile);
        double[] samples = null;
        while (it.next()) {
            samples = it.getPixel(samples);         // Get values in all bands.
            for (int i=0; i<samples.length; i++) {
                accumulator[i].accept(samples[i]);
            }
        }
    }
}
