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

import java.awt.image.RenderedImage;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.ArgumentChecks;


/**
 * A predefined set of operations on images as convenience methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class ImageOperations {
    /**
     * The set of operations with default configuration. Operations executed by this instance
     * will be multi-threaded if possible, and failures to compute a value cause an exception
     * to be thrown.
     */
    public static final ImageOperations DEFAULT = new ImageOperations(true);

    /**
     * The set of operations where all executions are constrained to a single thread.
     * Only the caller thread is used, with no parallelization. Sequential operations
     * may be useful for processing {@link RenderedImage} that may not be thread-safe.
     * The error handling policy is the same than {@link #DEFAULT}.
     */
    public static final ImageOperations SEQUENTIAL = new ImageOperations(false);

    /**
     * Whether the operations can be executed in parallel.
     */
    private final boolean parallel;

    /**
     * Creates a new set of image operations.
     *
     * @param  parallel  whether the operations can be executed in parallel.
     */
    private ImageOperations(final boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * Returns statistics on all bands of the given image.
     *
     * @param  source  the image for which to compute statistics.
     * @return the statistics of sample values in each band.
     */
    public Statistics[] statistics(final RenderedImage source) {
        ArgumentChecks.ensureNonNull("source", source);
        final StatisticsCalculator calculator = new StatisticsCalculator(source, parallel);
        final Object property = calculator.getProperty(StatisticsCalculator.PROPERTY_NAME);
        if (property instanceof Statistics[]) {
            // TODO: check error condition.
            return (Statistics[]) property;
        }
        return null;
    }
}
