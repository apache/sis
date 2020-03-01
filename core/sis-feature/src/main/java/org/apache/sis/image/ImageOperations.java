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
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;


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

    /**
     * Returns an image with the same sample values than the given image, but with its color ramp rescaled between the specified bounds.
     * For example in a gray scale image, pixels with the minimum value will be black and pixels with the maximum value will be white.
     * This operation is a kind of <cite>tone mapping</cite>, a technique used in image processing to map one set of colors to another.
     * The mapping applied by this method is conceptually a simple linear transform (a scale and an offset)
     * applied on sample values before they are mapped to their colors.
     *
     * <p>Current implementation can remap only gray scale images (it may be extended to indexed color models
     * in a future version). If this method can not rescale the color ramp, for example because the given image
     * is an RGB image, then the image is returned unchanged.</p>
     *
     * @param  source    the image to recolor (may be {@code null}).
     * @param  minimum   the sample value to display with the first color of the color ramp (black in a grayscale image).
     * @param  maximum   the sample value to display with the last color of the color ramp (white in a grayscale image).
     * @return the image with color ramp rescaled between the given bounds, or {@code image} unchanged if the operation
     *         can not be applied on the given image.
     */
    public RenderedImage rescaleColorRamp(final RenderedImage source, final double minimum, final double maximum) {
        ArgumentChecks.ensureFinite("minimum", minimum);
        ArgumentChecks.ensureFinite("maximum", maximum);
        if (!(minimum < maximum)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, minimum, maximum));
        }
        final int visibleBand = ImageUtilities.getVisibleBand(source);
        if (visibleBand >= 0) {
            return RecoloredImage.rescale(source, visibleBand, minimum, maximum);
        }
        return source;
    }

    /**
     * Returns an image with the same sample values than the given image, but with its color ramp rescaled between
     * automatically determined bounds. This is the same operation than {@link #rescaleColorRamp rescaleColorRamp(…)}
     * except that the minimum and maximum values are determined by {@linkplain #statistics(RenderedImage) statistics}
     * on the image: a range of value is determined first from the {@linkplain Statistics#minimum() minimum} and
     * {@linkplain Statistics#maximum() maximum} values found in the image, optionally narrowed to an interval
     * of some {@linkplain Statistics#standardDeviation(boolean) standard deviations} around the mean value.
     *
     * <p>Narrowing with standard deviations is useful for data having a Gaussian distribution, as often seen in nature.
     * In such distribution, 99.9% of the data are between the mean ± 3×<var>standard deviation</var>, but some values
     * may still appear much further. The minimum and maximum values alone are not a robust way to compute a range of
     * values for the color ramp because a single value very far from other values is sufficient for making the colors
     * difficult to distinguish for 99.9% of the data.</p>
     *
     * @param  source      the image to recolor (may be {@code null}).
     * @param  deviations  multiple of standard deviations around the mean, of {@link Double#POSITIVE_INFINITY}
     *                     for not using standard deviation for narrowing the range of values.
     *                     Some values giving good results for a Gaussian distribution are 1.5, 2 or 3.
     * @return the image with color ramp rescaled between the automatic bounds,
     *         or {@code image} unchanged if the operation can not be applied on the given image.
     */
    public RenderedImage automaticColorRamp(final RenderedImage source, double deviations) {
        ArgumentChecks.ensureStrictlyPositive("deviations", deviations);
        final int visibleBand = ImageUtilities.getVisibleBand(source);
        if (visibleBand >= 0) {
            final Statistics[] statistics = statistics(source);
            if (statistics != null && visibleBand < statistics.length) {
                final Statistics s = statistics[visibleBand];
                if (s != null) {
                    deviations *= s.standardDeviation(true);
                    final double mean    = s.mean();
                    final double minimum = Math.max(s.minimum(), mean - deviations);
                    final double maximum = Math.min(s.maximum(), mean + deviations);
                    if (minimum < maximum) {
                        return RecoloredImage.rescale(source, visibleBand, minimum, maximum);
                    }
                }
            }
        }
        return source;
    }
}
