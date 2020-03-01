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

import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.ArgumentChecks;
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
    public static final ImageOperations PARALLEL = new ImageOperations(true, true, null);

    /**
     * The set of operations where all executions are constrained to a single thread.
     * Only the caller thread is used, with no parallelization. Sequential operations
     * may be useful for processing {@link RenderedImage} that may not be thread-safe.
     * The error handling policy is the same than {@link #PARALLEL}.
     */
    public static final ImageOperations SEQUENTIAL = new ImageOperations(false, true, null);

    /**
     * The set of operations executed without throwing an exception in case of failure.
     * Instead the warnings are logged. Whether the operations are executed in parallel
     * or not is implementation dependent.
     *
     * <p>Users should prefer {@link #PARALLEL} or {@link #SEQUENTIAL} in most cases since the use
     * of {@code LENIENT} may cause errors to be unnoticed (not everyone read log messages).</p>
     */
    public static final ImageOperations LENIENT = new ImageOperations(true, false, null);

    /**
     * Whether the operations can be executed in parallel.
     */
    private final boolean parallel;

    /**
     * Whether errors occurring during computation should be propagated instead than wrapped in a {@link LogRecord}.
     */
    private final boolean failOnException;

    /**
     * Where to send exceptions (wrapped in {@link LogRecord}) if an operation failed on one or more tiles.
     * Only one log record is created for all tiles that failed for the same operation on the same image.
     * This is always {@code null} if {@link #failOnException} is {@code true}.
     */
    private final Filter errorListener;

    /**
     * Creates a new set of image operations.
     *
     * <h4>Error handling</h4>
     * If an exception occurs during the computation of a tile, then the {@code ImageOperations} behavior
     * is controlled by the following parameters:
     *
     * <ul>
     *   <li>If {@code failOnException} is {@code true}, the exception is thrown as an {@link ImagingOpException}.</li>
     *   <li>If {@code failOnException} is {@code false}, then:<ul>
     *     <li>If {@code errorListener} is {@code null}, the exception is logged and a partial result is returned.</li>
     *     <li>If {@code errorListener} is non-null, the exception is wrapped in a {@link LogRecord} and sent to that handler.
     *         The listener can store the log record, for example for showing later in a graphical user interface (GUI).
     *         If the listener returns {@code true}, the log record is also logged, otherwise it is silently discarded.
     *         In both cases a partial result is returned.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param  parallel         whether the operations can be executed in parallel.
     * @param  failOnException  whether exceptions occurring during computation should be propagated.
     * @param  errorListener     handler to notify when an operation failed on one or more tiles,
     *                          or {@code null} for printing the exceptions with the default logger.
     *                          This is ignored if {@code failOnException} is {@code true}.
     */
    public ImageOperations(final boolean parallel, final boolean failOnException, final Filter errorListener) {
        this.parallel        = parallel;
        this.failOnException = failOnException;
        this.errorListener   = failOnException ? null : errorListener;
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
        final StatisticsCalculator calculator = new StatisticsCalculator(source, parallel(source), failOnException);
        final Object property = calculator.getProperty(StatisticsCalculator.PROPERTY_NAME);
        calculator.logAndClearError(ImageOperations.class, "statistics", errorListener);
        if (property instanceof Statistics[]) {
            return (Statistics[]) property;
        }
        return null;
    }
}
