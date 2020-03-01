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

import java.util.WeakHashMap;
import java.util.logging.LogRecord;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.internal.system.Modules;


/**
 * A predefined set of operations on images as convenience methods.
 * Operations can be executed in parallel if applied on image with a thread-safe
 * and concurrent implementation of {@link RenderedImage#getTile(int, int)}.
 * Otherwise the same operations can be executed sequentially in the caller thread.
 * Errors during calculation can either be propagated as an {@link ImagingOpException}
 * (in which case no result is available), or notified as a {@link LogRecord}
 * (in which case partial results may be available).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class ImageOperations {
    /**
     * The set of operations with default configuration. Operations executed by this instance
     * will be multi-threaded if possible, and failures to compute a value cause an exception
     * to be thrown.
     */
    public static final ImageOperations PARALLEL = new ImageOperations(true, true);

    /**
     * The set of operations where all executions are constrained to a single thread.
     * Only the caller thread is used, with no parallelization. Sequential operations
     * may be useful for processing {@link RenderedImage} that may not be thread-safe.
     * The error handling policy is the same than {@link #PARALLEL}.
     */
    public static final ImageOperations SEQUENTIAL = new ImageOperations(PARALLEL, false);

    /**
     * The set of operations executed without throwing an exception in case of failure.
     * Instead the warnings are logged. Whether the operations are executed in parallel
     * or not is implementation dependent.
     *
     * <p>Users should prefer {@link #PARALLEL} or {@link #SEQUENTIAL} in most cases since the use
     * of {@code LENIENT} may cause errors to be unnoticed (not everyone read log messages).</p>
     */
    public static final ImageOperations LENIENT = new ImageOperations(true, false);

    /**
     * Whether the operations can be executed in parallel.
     */
    private final boolean parallel;

    /**
     * Whether errors occurring during computation should be propagated instead than wrapped in a {@link LogRecord}.
     */
    private final boolean failOnException;

    /**
     * Cache of properties already computed for images. That map shall contains computation result only,
     * never the {@link AnnotatedImage} instances that computed those results, as doing so would create
     * memory leak (because of {@link AnnotatedImage#source} preventing the key to be garbage-collected).
     * All accesses to this cache shall be synchronized on the {@link WeakHashMap} instance.
     */
    private final WeakHashMap<RenderedImage, Cache<String,Object>> cache;

    /**
     * Creates a new set of image operations.
     *
     * @param  parallel         whether the operations can be executed in parallel.
     * @param  failOnException  whether errors occurring during computation should be propagated.
     */
    public ImageOperations(final boolean parallel, final boolean failOnException) {
        this.parallel        = parallel;
        this.failOnException = failOnException;
        cache = new WeakHashMap<>();
    }

    /**
     * Creates a new set of operations sharing the same cache then the given instance.
     * The two sets of operations must have the same {@link #failOnException} policy;
     * only their parallelism can differ.
     */
    private ImageOperations(final ImageOperations other, final boolean parallel) {
        this.parallel = parallel;
        failOnException = other.failOnException;
        cache = other.cache;
    }

    /**
     * Returns the cache for properties computed on the specified image.
     */
    final Cache<String,Object> cache(final RenderedImage source) {
        synchronized (cache) {
            return cache.computeIfAbsent(source, (k) -> new Cache<>(8, 1000, true));
        }
    }

    /**
     * Whether the operations can be executed in parallel for the specified image.
     * Should be a method overridden by {@link #LENIENT}, but for this simple need
     * it is not yet worth to do sub-classing.
     */
    private boolean parallel(final RenderedImage source) {
        return (this == LENIENT) ? source.getClass().getName().startsWith(Modules.CLASSNAME_PREFIX) : parallel;
    }

    /**
     * Returns statistics (minimum, maximum, mean, standard deviation) on each bands of the given image.
     *
     * @param  source  the image for which to compute statistics.
     * @return the statistics of sample values in each band.
     * @throws ImagingOpException if an error occurred during calculation and {@code failOnException} is {@code true}.
     */
    public Statistics[] statistics(final RenderedImage source) {
        ArgumentChecks.ensureNonNull("source", source);
        final StatisticsCalculator calculator = new StatisticsCalculator(this, source, parallel(source), failOnException);
        final Object property = calculator.getProperty(StatisticsCalculator.PROPERTY_NAME);
        calculator.logAndClearError(ImageOperations.class, "statistics");
        if (property instanceof Statistics[]) {
            return (Statistics[]) property;
        }
        return null;
    }
}
