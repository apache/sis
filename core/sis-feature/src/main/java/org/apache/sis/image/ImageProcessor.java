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

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.NavigableMap;
import java.util.function.Function;
import java.util.logging.LogRecord;
import java.util.function.DoubleUnaryOperator;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRenderedImage;
import javax.measure.Quantity;
import org.apache.sis.coverage.Category;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.internal.coverage.j2d.ImageLayout;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.coverage.j2d.TiledImage;
import org.apache.sis.internal.processing.image.Isolines;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;

// For javadoc
import org.apache.sis.coverage.RegionOfInterest;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverageProcessor;


/**
 * A predefined set of operations on images as convenience methods.
 * After instantiation, {@code ImageProcessor} can be configured for the following aspects:
 *
 * <ul class="verbose">
 *   <li>
 *     {@linkplain #setInterpolation(Interpolation) Interpolation method} to use during resampling operations.
 *   </li><li>
 *     {@linkplain #setFillValues(Number...) Fill values} to use for pixels that can not be computed.
 *   </li><li>
 *     {@linkplain #setCategoryColors(Function) Category colors} for mapping sample values
 *     (identified by their range, name or unit of measurement) to colors.
 *   </li><li>
 *     {@linkplain #setImageResizingPolicy(Resizing) Image resizing policy} to apply
 *     if a requested image size prevent the image to be tiled.
 *   </li><li>
 *     {@linkplain #setPositionalAccuracyHints(Quantity...) Positional accuracy hints}
 *     for enabling the use of faster algorithm when a lower accuracy is acceptable.
 *   </li><li>
 *     Whether operations can be executed in parallel. By default operations on unknown
 *     {@link RenderedImage} implementations are executed sequentially in the caller thread, for safety reasons.
 *     Some operations can be parallelized, but it should be enabled only if the {@link RenderedImage} is known
 *     to be thread-safe and has concurrent (or fast) implementation of {@link RenderedImage#getTile(int, int)}.
 *     Apache SIS implementations of {@link RenderedImage} can be parallelized, but it may not be the case of
 *     images from other libraries.
 *   </li><li>
 *     Whether the operations should fail if an exception is thrown while processing a tile.
 *     By default errors during calculation are propagated as an {@link ImagingOpException},
 *     in which case no result is available. But errors can also be notified as a {@link LogRecord} instead,
 *     in which case partial results may be available.
 *   </li>
 * </ul>
 *
 * For each image operations, above properties are combined with parameters given to the operation method.
 * Each method in this {@code ImageProcessor} class documents the properties used in addition of method parameters.
 *
 * <div class="note"><b>API design:</b>
 * properties (setter methods) are used for values that can be applied unchanged on many different images.
 * For example the {@linkplain #getInterpolation() interpolation method} can be specified once and used
 * unchanged for many {@link #resample resample(…)} operations.
 * On the other hand, method arguments are used for values that are usually specific to the image to process.
 * For example the {@link MathTransform} argument given to the {@link #resample resample(…)} operation depends
 * tightly on the source image and destination bounds (also given in arguments); those information usually need
 * to be recomputed for each image.</div>
 *
 * <h2>Deferred calculations</h2>
 * Methods in this class may compute the result at some later time after the method returned, instead of computing
 * the result immediately on method call. Consequently unless otherwise specified, {@link RenderedImage} arguments
 * should be <em>stable</em>, i.e. pixel values should not be modified after method return.
 *
 * <h2>Area of interest</h2>
 * Some operations accept an optional <cite>area of interest</cite> argument specified as a {@link Shape} instance in
 * pixel coordinates. If a shape is given, it should not be modified after {@code ImageProcessor} method call because
 * the given object may be retained directly (i.e. the {@code Shape} is not always cloned; it depends on its class).
 * In addition, the {@code Shape} implementation shall be thread-safe (assuming its state stay unmodified)
 * unless the execution mode is set to {@link Mode#PARALLEL}.
 *
 * <h2>Error handling</h2>
 * If an exception occurs during the computation of a tile, then the {@code ImageProcessor} behavior
 * is controlled by the {@link #getErrorHandler() errorHandler} property:
 *
 * <ul>
 *   <li>If {@link ErrorHandler#THROW}, the exception is wrapped in an {@link ImagingOpException} and thrown.</li>
 *   <li>If {@link ErrorHandler#LOG}, the exception is logged and a partial result is returned.</li>
 *   <li>If any other value, the exception is wrapped in a {@link LogRecord} and sent to that filter.
 *     The filter can store the log record, for example for showing later in a graphical user interface (GUI).
 *     If the filter returns {@code true}, the log record is also logged, otherwise it is silently discarded.
 *     In both cases a partial result is returned.</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * {@code ImageProcessor} is safe for concurrent use in multi-threading environment.
 * Note however that {@code ImageProcessor} instances are mutable;
 * consider {@linkplain #clone() cloning} if setter methods are invoked on a shared instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see org.apache.sis.coverage.grid.GridCoverageProcessor
 *
 * @since 1.1
 * @module
 */
public class ImageProcessor implements Cloneable {
    /**
     * Cache of previously created images. We use this cache only for images of known implementations,
     * especially the ones which may be costly to compute. Reusing an existing instance avoid to repeat
     * the computation.
     */
    private static final WeakHashSet<RenderedImage> CACHE = new WeakHashSet<>(RenderedImage.class);

    /**
     * Returns an unique instance of the given image. This method should be invoked only for images
     * of known implementation, especially the ones which are costly to compute. The implementation
     * shall override {@link Object#equals(Object)} and {@link Object#hashCode()} methods.
     */
    static RenderedImage unique(final RenderedImage image) {
        return CACHE.unique(image);
    }

    /**
     * Properties (size, tile size, sample model, <i>etc.</i>) of destination images.
     *
     * @see #getImageLayout()
     * @see #setImageLayout(ImageLayout)
     */
    private ImageLayout layout;

    /**
     * Whether {@code ImageProcessor} can produce an image of different size compared to requested size.
     * An image may be resized if the requested size can not be subdivided into tiles of reasonable size.
     * For example if the image width is a prime number, there is no way to divide the image horizontally with
     * an integer number of tiles. The only way to get an integer number of tiles is to change the image size.
     *
     * <p>The image resizing policy may be used by any operation that involve a {@linkplain #resample resampling}.
     * If a resizing is applied, the new size will be written in the {@code bounds} argument (a {@link Rectangle}).</p>
     *
     * @see #getImageResizingPolicy()
     * @see #setImageResizingPolicy(Resizing)
     */
    public enum Resizing {
        /**
         * Image size is unmodified; the requested value is used unconditionally.
         * It may result in big tiles (potentially a single tile for the whole image)
         * if the image size is not divisible by a tile size.
         */
        NONE,

        /**
         * Image size can be increased. {@code ImageProcessor} will try to increase
         * by the smallest amount of pixels allowing the image to be subdivided in tiles.
         */
        EXPAND
    }

    /**
     * Interpolation to use during resample operations.
     *
     * @see #getInterpolation()
     * @see #setInterpolation(Interpolation)
     */
    private Interpolation interpolation;

    /**
     * The values to use for pixels that can not be computed.
     * This array may be {@code null} or may contain {@code null} elements.
     * This is a "copy on write" array (elements are not modified).
     *
     * @see #getFillValues()
     * @see #setFillValues(Number...)
     */
    private Number[] fillValues;

    /**
     * Colors to use for arbitrary categories of sample values. This function can return {@code null}
     * or empty arrays for some categories, which are interpreted as fully transparent pixels.
     *
     * @see #getCategoryColors()
     * @see #setCategoryColors(Function)
     */
    private Function<Category,Color[]> colors;

    /**
     * Hints about the desired positional accuracy (in "real world" units or in pixel units),
     * or {@code null} if unspecified. In order to avoid the need to clone this array in the
     * {@link #clone()} method, the content of this array should not be modified.
     * For setting new values, a new array should be created.
     *
     * @see #getPositionalAccuracyHints()
     * @see #setPositionalAccuracyHints(Quantity...)
     */
    private Quantity<?>[] positionalAccuracyHints;

    /**
     * Whether the operations can be executed in parallel.
     *
     * @see #getExecutionMode()
     * @see #setExecutionMode(Mode)
     */
    private Mode executionMode;

    /**
     * Execution modes specifying whether operations can be executed in parallel.
     * If {@link #SEQUENTIAL}, operations are executed sequentially in the caller thread.
     * If {@link #PARALLEL}, some operations may be parallelized using an arbitrary number of threads.
     *
     * @see #getExecutionMode()
     * @see #setExecutionMode(Mode)
     */
    public enum Mode {
        /**
         * Operations executed in multi-threaded mode if possible.
         * This mode can be used if the {@link RenderedImage} instances are thread-safe and provide
         * a concurrent (or very fast) implementation of {@link RenderedImage#getTile(int, int)}.
         */
        PARALLEL,

        /**
         * Operations executed in the caller thread, without parallelization.
         * Sequential operations may be useful for processing {@link RenderedImage}
         * implementations that may not be thread-safe.
         */
        SEQUENTIAL,

        /**
         * Operations are executed in multi-threaded mode if the {@link RenderedImage} instance
         * is an implementation known to be thread-safe. All operations on image implementations
         * unknown to Apache SIS are executed in sequential mode.
         */
        DEFAULT
    }

    /**
     * Whether errors occurring during computation should be propagated or wrapped in a {@link LogRecord}.
     * If errors are wrapped in a {@link LogRecord}, this field specifies what to do with the record.
     * Only one log record is created for all tiles that failed for the same operation on the same image.
     *
     * @see #getErrorHandler()
     * @see #setErrorHandler(ErrorHandler)
     */
    private ErrorHandler errorHandler;

    /**
     * Creates a new processor with default configuration.
     * The execution mode is initialized to {@link Mode#DEFAULT}
     * and the error handler to {@link ErrorHandler#THROW}.
     */
    public ImageProcessor() {
        layout        = ImageLayout.DEFAULT;
        executionMode = Mode.DEFAULT;
        errorHandler  = ErrorHandler.THROW;
        interpolation = Interpolation.BILINEAR;
    }

    /**
     * Returns the properties (size, tile size, sample model, <i>etc.</i>) of destination images.
     * This method is not yet public because {@link ImageLayout} is not a public class.
     */
    final synchronized ImageLayout getImageLayout() {
        return layout;
    }

    /**
     * Sets the properties (size, tile size, sample model, <i>etc.</i>) of destination images.
     * This method is not yet public because {@link ImageLayout} is not a public class.
     */
    final synchronized void setImageLayout(final ImageLayout layout) {
        ArgumentChecks.ensureNonNull("layout", layout);
        this.layout = layout;
    }

    /**
     * Returns the interpolation method to use during resample operations.
     *
     * @return interpolation method to use during resample operations.
     *
     * @see #resample(RenderedImage, Rectangle, MathTransform)
     */
    public synchronized Interpolation getInterpolation() {
        return interpolation;
    }

    /**
     * Sets the interpolation method to use during resample operations.
     *
     * @param  method  interpolation method to use during resample operations.
     *
     * @see #resample(RenderedImage, Rectangle, MathTransform)
     */
    public synchronized void setInterpolation(final Interpolation method) {
        ArgumentChecks.ensureNonNull("method", method);
        interpolation = method;
    }

    /**
     * Returns the values to use for pixels that can not be computed.
     * This method returns a copy of the array set by the last call to {@link #setFillValues(Number...)}.
     *
     * @return fill values to use for pixels that can not be computed, or {@code null} for the defaults.
     */
    public synchronized Number[] getFillValues() {
        return (fillValues != null) ? fillValues.clone() : null;
    }

    /**
     * Sets the values to use for pixels that can not be computed. The given array may be {@code null} or may contain
     * {@code null} elements for default values. Those defaults are zero for images storing sample values as integers,
     * or {@link Float#NaN} or {@link Double#NaN} for images storing sample values as floating point numbers. If the
     * given array contains less elements than the number of bands in an image, missing elements will be assumed null.
     * If the given array contains more elements than the number of bands, extraneous elements will be ignored.
     *
     * @param  values  fill values to use for pixels that can not be computed, or {@code null} for the defaults.
     */
    public synchronized void setFillValues(final Number... values) {
        fillValues = (values != null) ? values.clone() : null;
    }

    /**
     * Returns the colors to use for given categories of sample values, or {@code null} is unspecified.
     * This method returns the function set by the last call to {@link #setCategoryColors(Function)}.
     *
     * @return colors to use for arbitrary categories of sample values, or {@code null} for default.
     */
    public synchronized Function<Category,Color[]> getCategoryColors() {
        return colors;
    }

    /**
     * Sets the colors to use for given categories in image, or {@code null} is unspecified.
     * This function provides a way to colorize images without knowing in advance the numerical values of pixels.
     * For example instead of specifying <cite>"pixel value 0 in blue, 1 in green, 2 in yellow"</cite>,
     * this function allows to specify <cite>"Lakes in blue, Forests in green, Sand in yellow"</cite>.
     * It is still possible however to use numerical values if the function desires to do so,
     * since this information is available with {@link Category#getSampleRange()}.
     *
     * <p>This function is used by methods expecting {@link SampleDimension} arguments such as
     * {@link #visualize(RenderedImage, List)}. The given function can return {@code null} or
     * empty arrays for some categories, which are interpreted as fully transparent pixels.</p>
     *
     * @param  colors  colors to use for arbitrary categories of sample values, or {@code null} for default.
     */
    public synchronized void setCategoryColors(final Function<Category,Color[]> colors) {
        this.colors = colors;
    }

    /**
     * Returns whether {@code ImageProcessor} can produce an image of different size compared to requested size.
     * If this processor can use a different size, the enumeration value specifies what kind of changes may be applied.
     *
     * @return the image resizing policy.
     */
    public synchronized Resizing getImageResizingPolicy() {
        return layout.isBoundsAdjustmentAllowed ? Resizing.EXPAND : Resizing.NONE;
    }

    /**
     * Sets whether {@code ImageProcessor} can produce an image of different size compared to requested size.
     *
     * @param  policy   the new image resizing policy.
     */
    public synchronized void setImageResizingPolicy(final Resizing policy) {
        ArgumentChecks.ensureNonNull("policy", policy);
        layout = (policy == Resizing.EXPAND) ? ImageLayout.SIZE_ADJUST : ImageLayout.DEFAULT;
    }

    /**
     * Returns hints about the desired positional accuracy, in "real world" units or in pixel units.
     * This is an empty array by default, which means that {@code ImageProcessor} aims for the best
     * accuracy it can produce. If the returned array is non-empty and contains accuracies large enough,
     * {@code ImageProcessor} may use some slightly faster algorithms at the expense of accuracy.
     *
     * @return desired accuracy in no particular order, or an empty array if none.
     */
    public synchronized Quantity<?>[] getPositionalAccuracyHints() {
        return (positionalAccuracyHints != null) ? positionalAccuracyHints.clone() : new Quantity<?>[0];
    }

    /**
     * Sets hints about desired positional accuracy, in "real world" units or in pixel units.
     * More than one hint can be specified for allowing the use of different units.
     * For example the given array can contain an accuracy in metres and an accuracy in seconds,
     * for specifying desired accuracies in both spatial dimensions and in the temporal dimension.
     * Accuracy can also be specified in both real world units such as {@linkplain Units#METRE metres}
     * and in {@linkplain Units#PIXEL pixel units}, which are converted to real world units depending
     * on image resolution. If more than one value is applicable to a dimension
     * (after unit conversion if needed), the smallest value is taken.
     *
     * <p>Those values are only hints, the {@code ImageProcessor} is free to ignore them.
     * In any cases there is no guarantee that computed images will met those accuracies.
     * The given values are honored on a <em>best effort</em> basis only.</p>
     *
     * <p>In current implementation, {@code ImageProcessor} recognizes only accuracies in {@link Units#PIXEL}.
     * A value such as 0.125 pixel may cause {@code ImageProcessor} to use some a slightly faster algorithm
     * at the expense of accuracy during {@linkplain #resample resampling operations}.</p>
     *
     * @param  hints  desired accuracy in no particular order, or a {@code null} array if none.
     *                Null elements in the array are ignored.
     */
    public synchronized void setPositionalAccuracyHints(final Quantity<?>... hints) {
        if (hints != null) {
            final Quantity<?>[] copy = new Quantity<?>[hints.length];
            int n = 0;
            for (final Quantity<?> hint : hints) {
                if (hint != null) copy[n++] = hint;
            }
            if (n != 0) {
                positionalAccuracyHints = ArraysExt.resize(copy, n);
                return;
            }
        }
        positionalAccuracyHints = null;
    }

    /**
     * Returns whether operations can be executed in parallel.
     * If {@link Mode#SEQUENTIAL}, operations are executed sequentially in the caller thread.
     * If {@link Mode#PARALLEL}, some operations may be parallelized using an arbitrary number of threads.
     *
     * @return whether the operations can be executed in parallel.
     */
    public synchronized Mode getExecutionMode() {
        return executionMode;
    }

    /**
     * Sets whether operations can be executed in parallel.
     * This value can be set to {@link Mode#PARALLEL} if the {@link RenderedImage} instances are thread-safe
     * and provide a concurrent (or very fast) implementation of {@link RenderedImage#getTile(int, int)}.
     * If {@link Mode#SEQUENTIAL}, only the caller thread is used. Sequential operations may be useful
     * for processing {@link RenderedImage} implementations that may not be thread-safe.
     *
     * <p>It is safe to set this flag to {@link Mode#PARALLEL} with {@link java.awt.image.BufferedImage}
     * or with Apache SIS implementations of {@link RenderedImage}.</p>
     *
     * @param  mode  whether the operations can be executed in parallel.
     */
    public synchronized void setExecutionMode(final Mode mode) {
        ArgumentChecks.ensureNonNull("mode", mode);
        executionMode = mode;
    }

    /**
     * Whether the operations can be executed in parallel for the specified image.
     * This method shall be invoked in a method synchronized on {@code this}.
     */
    private boolean parallel(final RenderedImage source) {
        assert Thread.holdsLock(this);
        switch (executionMode) {
            case PARALLEL:   return true;
            case SEQUENTIAL: return false;
            default: {
                if (source instanceof BufferedImage) {
                    return true;
                }
                return source.getClass().getName().startsWith(Modules.CLASSNAME_PREFIX);
            }
        }
    }

    /**
     * Returns whether exceptions occurring during computation are propagated or logged.
     * If {@link ErrorHandler#THROW} (the default), exceptions are wrapped in {@link ImagingOpException} and thrown.
     * If {@link ErrorHandler#LOG}, exceptions are wrapped in a {@link LogRecord}, filtered then eventually logged.
     *
     * @return whether exceptions occurring during computation are propagated or logged.
     */
    public synchronized ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Sets whether exceptions occurring during computation are propagated or logged.
     * The default behavior is to wrap exceptions in {@link ImagingOpException} and throw them.
     * If this property is set to {@link ErrorHandler#LOG}, then exceptions will be wrapped in
     * {@link LogRecord} instead, in which case a partial result may be available.
     * Only one log record is created for all tiles that failed for the same operation on the same image.
     *
     * <h4>Limitations</h4>
     * In current {@code ImageProcessor} implementation, the error handler is not honored by all operations.
     * Some operations may continue to throw an exception on failure (the behavior of default error handler)
     * even if a different handler has been specified.
     * Each operation specifies in its Javadoc whether the operation uses error handler or not.
     *
     * @param  handler  handler to notify when an operation failed on one or more tiles,
     *                  or {@link ErrorHandler#THROW} for propagating the exception.
     */
    public synchronized void setErrorHandler(final ErrorHandler handler) {
        ArgumentChecks.ensureNonNull("handler", handler);
        errorHandler = handler;
    }

    /**
     * Whether errors occurring during computation should be propagated instead of wrapped in a {@link LogRecord}.
     * This method shall be invoked in a method synchronized on {@code this}.
     */
    private boolean failOnException() {
        assert Thread.holdsLock(this);
        return errorHandler == ErrorHandler.THROW;
    }

    /**
     * Builds an operator which can be used for filtering "no data" sample values.
     * Calls to the operator {@code applyAsDouble(x)} will return {@link Double#NaN}
     * if the <var>x</var> value is equal to one of the given no-data {@code values},
     * and will return <var>x</var> unchanged otherwise.
     *
     * <h4>Usage</h4>
     * This operator can be used as a {@code sampleFilters} argument in calls to
     * {@link #statistics statistics(…)} or {@link #valueOfStatistics valueOfStatistics(…)} methods.
     * It is redundant with {@linkplain SampleDimension#getTransferFunction() transfer function} work,
     * but can be useful for images not managed by a {@link org.apache.sis.coverage.grid.GridCoverage}.
     *
     * @param  values  the "no data" values, or {@code null} if none. Null and NaN elements are ignored.
     * @return an operator for filtering the given "no data" values,
     *         or {@code null} if there is no non-NaN value to filter.
     *
     * @see #statistics(RenderedImage, Shape, DoubleUnaryOperator[])
     * @see SampleDimension#getTransferFunction()
     *
     * @since 1.2
     */
    public DoubleUnaryOperator filterNodataValues(final Number... values) {
        return (values != null) ? StatisticsCalculator.filterNodataValues(values) : null;
    }

    /**
     * Returns statistics (minimum, maximum, mean, standard deviation) on each bands of the given image.
     * Invoking this method is equivalent to invoking the {@link #statistics statistics(…)} method and
     * extracting immediately the statistics property value, except that custom
     * {@linkplain #setErrorHandler error handlers} are supported.
     *
     * <p>If {@code areaOfInterest} is null and {@code sampleFilters} is {@code null} or empty,
     * then the default behavior is as below:</p>
     * <ul>
     *   <li>If the {@value PlanarImage#STATISTICS_KEY} property value exists in the given image,
     *       then that value is returned. Note that they are not necessarily statistics for the whole image.
     *       They are whatever statistics the property provider considered as representative.</li>
     *   <li>Otherwise statistics are computed for the whole image.</li>
     * </ul>
     *
     * <h4>Sample converters</h4>
     * An arbitrary {@link DoubleUnaryOperator} can be applied on sample values before to add them to statistics.
     * The main purpose is to replace "no-data values" by {@link Double#NaN} values for instructing
     * {@link Statistics#accept(double)} to ignore them. The {@link #filterNodataValues(Number...)}
     * convenience method can be used for building an operator filtering "no data" sample values.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getExecutionMode() Execution mode} (parallel or sequential).</li>
     *   <li>{@linkplain #getErrorHandler() Error handler} (custom action executed if an exception is thrown).</li>
     * </ul>
     *
     * <h4>Result relationship with source</h4>
     * This method computes statistics immediately.
     * Changes in the {@code source} image after this method call do not change the results.
     *
     * @param  source          the image for which to compute statistics.
     * @param  areaOfInterest  pixel coordinates of the area of interest, or {@code null} for the default.
     * @param  sampleFilters   converters to apply on sample values before to add them to statistics, or
     *         {@code null} or an empty array if none. The array may have any length and may contain null elements.
     *         For all {@code i < numBands}, non-null {@code sampleFilters[i]} are applied to band <var>i</var>.
     * @return the statistics of sample values in each band.
     * @throws ImagingOpException if an error occurred during calculation
     *         and the error handler is {@link ErrorHandler#THROW}.
     *
     * @see #statistics(RenderedImage, Shape, DoubleUnaryOperator...)
     * @see #filterNodataValues(Number...)
     * @see PlanarImage#STATISTICS_KEY
     */
    public Statistics[] valueOfStatistics(final RenderedImage source, final Shape areaOfInterest,
                                          final DoubleUnaryOperator... sampleFilters)
    {
        ArgumentChecks.ensureNonNull("source", source);
        if (areaOfInterest == null && (sampleFilters == null || ArraysExt.allEquals(sampleFilters, null))) {
            final Object property = source.getProperty(PlanarImage.STATISTICS_KEY);
            if (property instanceof Statistics[]) {
                return (Statistics[]) property;
            }
        }
        final boolean parallel, failOnException;
        final ErrorHandler errorListener;
        synchronized (this) {
            parallel        = parallel(source);
            failOnException = failOnException();
            errorListener   = errorHandler;
        }
        /*
         * No need to check if the given source is already an instance of StatisticsCalculator.
         * The way AnnotatedImage cache mechanism is implemented, if statistics results already
         * exist, they will be used.
         */
        final AnnotatedImage calculator = new StatisticsCalculator(source, areaOfInterest, sampleFilters, parallel, failOnException);
        final Object property = calculator.getProperty(PlanarImage.STATISTICS_KEY);
        calculator.logAndClearError(ImageProcessor.class, "valueOfStatistics", errorListener);
        return (Statistics[]) property;
    }

    /**
     * Returns an image with statistics (minimum, maximum, mean, standard deviation) on each bands.
     * The property value will be computed when first requested (it is not computed immediately by this method).
     *
     * <p>If {@code areaOfInterest} is null and {@code sampleFilters} is {@code null} or empty,
     * then the default is as below:</p>
     * <ul>
     *   <li>If the {@value PlanarImage#STATISTICS_KEY} property value exists in the given image,
     *       then that image is returned as-is. Note that the existing property value is not necessarily
     *       statistics for the whole image.
     *       They are whatever statistics the property provider considers as representative.</li>
     *   <li>Otherwise an image augmented with a {@value PlanarImage#STATISTICS_KEY} property value
     *       is returned.</li>
     * </ul>
     *
     * <h4>Sample converters</h4>
     * An arbitrary {@link DoubleUnaryOperator} can be applied on sample values before to add them to statistics.
     * The main purpose is to replace "no-data values" by {@link Double#NaN} values for instructing
     * {@link Statistics#accept(double)} to ignore them. The {@link #filterNodataValues(Number...)}
     * convenience method can be used for building an operator filtering "no data" sample values.
     *
     * <div class="note"><b>API design note:</b>
     * the {@code areaOfInterest} and {@code sampleFilters} arguments are complementary.
     * Both of them filter the data accepted for statistics. In ISO 19123 terminology,
     * the {@code areaOfInterest} argument filters the <cite>coverage domain</cite> while
     * the {@code sampleFilters} argument filters the <cite>coverage range</cite>.
     * Another connection with OGC/ISO standards is that {@link DoubleUnaryOperator} in this context
     * does the same work than {@linkplain SampleDimension#getTransferFunction() transfer function}.
     * It can be useful for images not managed by a {@link org.apache.sis.coverage.grid.GridCoverage}.
     * </div>
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getExecutionMode() Execution mode} (parallel or sequential).</li>
     *   <li>{@linkplain #getErrorHandler() Error handler} (whether to fail if an exception is thrown).</li>
     * </ul>
     *
     * @param  source          the image for which to provide statistics.
     * @param  areaOfInterest  pixel coordinates of the area of interest, or {@code null} for the default.
     * @param  sampleFilters   converters to apply on sample values before to add them to statistics, or
     *         {@code null} or an empty array if none. The array may have any length and may contain null elements.
     *         For all {@code i < numBands}, non-null {@code sampleFilters[i]} are applied to band <var>i</var>.
     * @return an image with an {@value PlanarImage#STATISTICS_KEY} property.
     *         May be {@code image} if the given argument already has a statistics property.
     *
     * @see #valueOfStatistics(RenderedImage, Shape, DoubleUnaryOperator...)
     * @see #filterNodataValues(Number...)
     * @see PlanarImage#STATISTICS_KEY
     */
    public RenderedImage statistics(final RenderedImage source, final Shape areaOfInterest,
                                    final DoubleUnaryOperator... sampleFilters)
    {
        ArgumentChecks.ensureNonNull("source", source);
        if (areaOfInterest == null && (sampleFilters == null || ArraysExt.allEquals(sampleFilters, null))
                && ArraysExt.contains(source.getPropertyNames(), PlanarImage.STATISTICS_KEY))
        {
            return source;
        }
        final boolean parallel, failOnException;
        synchronized (this) {
            parallel        = parallel(source);
            failOnException = failOnException();
        }
        return new StatisticsCalculator(source, areaOfInterest, sampleFilters, parallel, failOnException).unique();
    }

    /**
     * Returns an image with the same sample values than the given image, but with its color ramp stretched between
     * specified or inferred bounds. For example in a gray scale image, pixels with the minimum value will be black
     * and pixels with the maximum value will be white. This operation is a kind of <cite>tone mapping</cite>,
     * a technique used in image processing to map one set of colors to another. The mapping applied by this method
     * is conceptually a simple linear transform (a scale and an offset) applied on sample values before they are
     * mapped to their colors.
     *
     * <p>The minimum and maximum value can be either specified explicitly,
     * or determined from {@link #valueOfStatistics statistics} on the image.
     * In the latter case a range of value is determined first from the {@linkplain Statistics#minimum() minimum}
     * and {@linkplain Statistics#maximum() maximum} values found in the image, optionally narrowed to an interval
     * of some {@linkplain Statistics#standardDeviation(boolean) standard deviations} around the mean value.</p>
     *
     * <p>Narrowing with standard deviations is useful for data having a Gaussian distribution, as often seen in nature.
     * In such distribution, 99.9% of the data are between the mean ± 3×<var>standard deviation</var>, but some values
     * may still appear much further. The minimum and maximum values alone are not a robust way to compute a range of
     * values for the color ramp because a single value very far from other values is sufficient for making the colors
     * difficult to distinguish for 99.9% of the data.</p>
     *
     * <p>The range of values for the color ramp can be narrowed with following modifiers
     * (a {@link Map} is used for allowing addition of more modifiers in future Apache SIS versions).
     * All unrecognized modifiers are silently ignored. If no modifier is specified, then the color ramp
     * will be stretched from minimum to maximum values found in specified image.</p>
     *
     * <table class="sis">
     *   <caption>Value range modifiers</caption>
     *   <tr>
     *     <th>Key</th>
     *     <th>Purpose</th>
     *     <th>Values</th>
     *   </tr><tr>
     *     <td>{@code "minimum"}</td>
     *     <td>Minimum value (omitted if computed from statistics).</td>
     *     <td>{@link Number}</td>
     *   </tr><tr>
     *     <td>{@code "maximum"}</td>
     *     <td>Maximum value (omitted if computed from statistics).</td>
     *     <td>{@link Number}</td>
     *   </tr><tr>
     *     <td>{@code "multStdDev"}</td>
     *     <td>Multiple of the standard deviation.</td>
     *     <td>{@link Number} (typical values: 1.5, 2 or 3)</td>
     *   </tr><tr>
     *     <td>{@code "statistics"}</td>
     *     <td>Statistics or image from which to get statistics.</td>
     *     <td>{@link Statistics} or {@link RenderedImage}</td>
     *   </tr><tr>
     *     <td>{@code "areaOfInterest"}</td>
     *     <td>Pixel coordinates of the region for which to compute statistics.</td>
     *     <td>{@link Shape}</td>
     *   </tr><tr>
     *     <td>{@code "nodataValues"}</td>
     *     <td>Values to ignore in statistics.</td>
     *     <td>{@link Number} or {@code Number[]}</td>
     *   </tr><tr>
     *     <td>{@code "sampleDimensions"}</td>
     *     <td>Meaning of pixel values.</td>
     *     <td>{@link SampleDimension}</td>
     *   </tr>
     * </table>
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>(none)</li>
     * </ul>
     *
     * <h4>Limitation</h4>
     * Current implementation can stretch only gray scale images (a future version may extend support to images
     * using {@linkplain java.awt.image.IndexColorModel index color models}). If this method can not stretch the
     * color ramp, for example because the given image is an RGB image, then the image is returned unchanged.
     *
     * @param  source     the image to recolor.
     * @param  modifiers  modifiers for narrowing the range of values, or {@code null} if none.
     * @return the image with color ramp stretched between the specified or calculated bounds,
     *         or {@code image} unchanged if the operation can not be applied on the given image.
     * @throws IllegalArgumentException if the value associated to one of about keys is not of expected type.
     */
    public RenderedImage stretchColorRamp(final RenderedImage source, final Map<String,?> modifiers) {
        ArgumentChecks.ensureNonNull("source", source);
        return RecoloredImage.stretchColorRamp(this, source, modifiers);
    }

    /**
     * Selects a subset of bands in the given image. This method can also be used for changing band order
     * or repeating the same band from the source image. If the specified {@code bands} are the same than
     * the source image bands in the same order, then {@code source} is returned directly.
     *
     * <p>This method returns an image sharing the same data buffer than the source image;
     * pixel values are not copied. Consequently changes in the source image are reflected
     * immediately in the returned image.</p>
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>(none)</li>
     * </ul>
     *
     * @param  source  the image in which to select bands.
     * @param  bands   indices of bands to retain.
     * @return image width selected bands.
     * @throws IllegalArgumentException if a band index is invalid.
     */
    public RenderedImage selectBands(final RenderedImage source, final int... bands) {
        ArgumentChecks.ensureNonNull("source", source);
        return BandSelectImage.create(source, bands);
    }

    /**
     * Applies a mask defined by a geometric shape. If {@code maskInside} is {@code true},
     * then all pixels inside the given shape are set to the {@linkplain #getFillValues() fill values}.
     * If {@code maskInside} is {@code false}, then the mask is reversed:
     * the pixels set to fill values are the ones outside the shape.
     *
     * @param  source      the image on which to apply a mask.
     * @param  mask        geometric area (in pixel coordinates) of the mask.
     * @param  maskInside  {@code true} for masking pixels inside the shape, or {@code false} for masking outside.
     * @return an image with mask applied.
     *
     * @see GridCoverageProcessor#mask(GridCoverage, RegionOfInterest, boolean)
     *
     * @since 1.2
     */
    public RenderedImage mask(final RenderedImage source, final Shape mask, final boolean maskInside) {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("mask",   mask);
        final Number[] fillValues;
        synchronized (this) {
            fillValues = this.fillValues;
        }
        return unique(new MaskedImage(source, mask, maskInside, fillValues));
    }

    /**
     * Returns an image with sample values converted by the given functions. The results can be stored as
     * {@code byte}, {@code short}, {@code int}, {@code float} or {@code double} values, not necessarily
     * the same type than the source values. If the result values are stored as integers, then they are
     * {@linkplain Math#round(double) rounded to nearest integers} and clamped in the valid range of the
     * target integer type.
     *
     * <p>If the source image is a {@link WritableRenderedImage} and the given converters are invertible, then
     * the returned image will also be a {@link WritableRenderedImage} instance. In such case values written in
     * the returned image will be reflected in the source image, with {@linkplain Math#round(double) rounding}
     * and clamping if the source values are stored as integers.</p>
     *
     * <p>The number of bands in the returned image is the length of the {@code converters} array,
     * which must be greater than 0 and not greater than the number of bands in the source image.
     * If the {@code converters} array length is less than the number of source bands, all source
     * bands at index ≥ {@code converters.length} will be ignored.</p>
     *
     * <p>The {@code sourceRanges} array is only a hint for this method. The array may be {@code null}
     * or contain {@code null} elements, and may be of any length. Missing elements are considered null
     * and extraneous elements are ignored. Those ranges do not need to encompass all possible values;
     * it is sufficient to provide only typical or "most interesting" ranges.</p>
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>(none)</li>
     * </ul>
     *
     * <h4>Result relationship with source</h4>
     * Changes in the source image are reflected in the returned image
     * if the source image notifies {@linkplain java.awt.image.TileObserver tile observers}.
     *
     * @param  source        the image for which to convert sample values.
     * @param  sourceRanges  approximate ranges of values for each band in source image, or {@code null} if unknown.
     * @param  converters    the transfer functions to apply on each band of the source image.
     * @param  targetType    the type of data in the image resulting from conversions.
     * @param  colorModel    color model of resulting image, or {@code null}.
     * @return the image which computes converted values from the given source.
     *
     * @see GridCoverageProcessor#convert(GridCoverage, MathTransform1D[], Function)
     */
    public RenderedImage convert(final RenderedImage source, final NumberRange<?>[] sourceRanges,
                MathTransform1D[] converters, final DataType targetType, final ColorModel colorModel)
    {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("converters", converters);
        ArgumentChecks.ensureNonNull("targetType", targetType);
        ArgumentChecks.ensureSizeBetween("converters", 1, ImageUtilities.getNumBands(source), converters.length);
        converters = converters.clone();
        for (int i=0; i<converters.length; i++) {
            ArgumentChecks.ensureNonNullElement("converters", i, converters[i]);
        }
        final ImageLayout layout;
        synchronized (this) {
            layout = this.layout;
        }
        // No need to clone `sourceRanges` because it is not stored by `BandedSampleConverter`.
        return unique(BandedSampleConverter.create(source, layout,
                sourceRanges, converters, targetType.toDataBufferType(), colorModel));
    }

    /**
     * Verifies that the given rectangle, if non-null, is non-empty.
     * This method assumes that the argument name is "bounds".
     */
    private static void ensureNonEmpty(final Rectangle bounds) {
        if (bounds != null && bounds.isEmpty()) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "bounds"));
        }
    }

    /**
     * Creates a new image which will resample the given image. The resampling operation is defined
     * by a potentially non-linear transform from the <em>new</em> image to the specified <em>source</em> image.
     * That transform should map {@linkplain org.opengis.referencing.datum.PixelInCell#CELL_CENTER pixel centers}.
     * If that transform produces coordinates that are outside source envelope bounds, then the corresponding pixels
     * in the new image are set to {@linkplain #getFillValues() fill values}. Otherwise sample values are interpolated
     * using the method given by {@link #getInterpolation()}.
     *
     * <p>If the given source is an instance of {@link ResampledImage},
     * then this method will use {@linkplain PlanarImage#getSources() the source} of the given source.
     * The intent is to avoid resampling a resampled image; instead this method works on the original data.</p>
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getInterpolation() Interpolation method} (nearest neighbor, bilinear, <i>etc</i>).</li>
     *   <li>{@linkplain #getFillValues() Fill values} for pixels outside source image.</li>
     *   <li>{@linkplain #getImageResizingPolicy() Image resizing policy} to apply
     *       if {@code bounds} size is not divisible by a tile size.</li>
     *   <li>{@linkplain #getPositionalAccuracyHints() Positional accuracy hints}
     *       for enabling faster resampling at the cost of lower precision.</li>
     * </ul>
     *
     * <h4>Result relationship with source</h4>
     * Changes in the source image are reflected in the returned images
     * if the source image notifies {@linkplain java.awt.image.TileObserver tile observers}.
     *
     * @param  source    the image to be resampled.
     * @param  bounds    domain of pixel coordinates of resampled image to create.
     *                   Updated by this method if {@link Resizing#EXPAND} policy is applied.
     * @param  toSource  conversion of pixel coordinates from resampled image to {@code source} image.
     * @return resampled image (may be {@code source}).
     *
     * @see GridCoverageProcessor#resample(GridCoverage, GridGeometry)
     */
    public RenderedImage resample(RenderedImage source, final Rectangle bounds, MathTransform toSource) {
        ArgumentChecks.ensureNonNull("source",   source);
        ArgumentChecks.ensureNonNull("bounds",   bounds);
        ArgumentChecks.ensureNonNull("toSource", toSource);
        ensureNonEmpty(bounds);
        final RenderedImage colored = source;
        final SampleModel sm = source.getSampleModel();
        boolean isIdentity = toSource.isIdentity();
        RenderedImage resampled = null;
        for (;;) {
            if (isIdentity && bounds.x == source.getMinX() && bounds.y == source.getMinY() &&
                    bounds.width == source.getWidth() && bounds.height == source.getHeight())
            {
                resampled = source;
                break;
            }
            if (Objects.equals(sm, source.getSampleModel())) {
                if (source instanceof ImageAdapter) {
                    source = ((ImageAdapter) source).source;
                    continue;
                }
                if (source instanceof ResampledImage) {
                    final List<RenderedImage> sources = source.getSources();
                    if (sources != null && sources.size() == 1) {                         // Paranoiac check.
                        toSource   = MathTransforms.concatenate(toSource, ((ResampledImage) source).toSource);
                        isIdentity = toSource.isIdentity();
                        source     = sources.get(0);
                        continue;
                    }
                }
            }
            /*
             * All accesses to ImageProcessor fields done by this method should be isolated in this single
             * synchronized block. All arrays are "copy on write", so they do not need to be cloned.
             */
            final ImageLayout   layout;
            final Interpolation interpolation;
            final Number[]      fillValues;
            final Quantity<?>[] positionalAccuracyHints;
            synchronized (this) {
                layout                  = this.layout;
                interpolation           = this.interpolation;
                fillValues              = this.fillValues;
                positionalAccuracyHints = this.positionalAccuracyHints;
            }
            resampled = unique(new ResampledImage(source,
                    layout.createCompatibleSampleModel(source, bounds), layout.getMinTile(),
                    bounds, toSource, interpolation, fillValues, positionalAccuracyHints));
            break;
        }
        /*
         * Preserve the color model of the original image, including information about how it
         * has been constructed. If the source image was not an instance of `RecoloredImage`,
         * then above call should return `resampled` unchanged.
         */
        return RecoloredImage.applySameColors(resampled, colored);
    }

    /**
     * Computes immediately all tiles in the given region of interest, then return an image will those tiles ready.
     * Computations will use many threads if {@linkplain #getExecutionMode() execution mode} is parallel.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getExecutionMode() Execution mode} (parallel or sequential).</li>
     *   <li>{@linkplain #getErrorHandler() Error handler} (whether to fail if an exception is thrown).</li>
     * </ul>
     *
     * @param  source          the image to compute immediately (may be {@code null}).
     * @param  areaOfInterest  pixel coordinates of the region to prefetch, or {@code null} for the whole image.
     * @return image with all tiles intersecting the AOI computed, or {@code null} if the given image was null.
     * @throws ImagingOpException if an exception occurred during {@link RenderedImage#getTile(int, int)} call.
     *         This exception wraps the original exception as its {@linkplain ImagingOpException#getCause() cause}.
     */
    public RenderedImage prefetch(RenderedImage source, final Rectangle areaOfInterest) {
        if (source == null || source instanceof BufferedImage || source instanceof TiledImage) {
            return source;
        }
        while (source instanceof PrefetchedImage) {
            source = ((PrefetchedImage) source).source;
        }
        final boolean parallel;
        final ErrorHandler errorListener;
        synchronized (this) {
            parallel = parallel(source);
            errorListener = errorHandler;
        }
        final PrefetchedImage image = new PrefetchedImage(source, areaOfInterest, errorListener, parallel);
        return image.isEmpty() ? source : image;
    }

    /**
     * Returns an image where all sample values are indices of colors in an {@link IndexColorModel}.
     * If the given image stores sample values as unsigned bytes or short integers, then those values
     * are used as-is (they are not copied or converted). Otherwise this operation will convert sample
     * values to unsigned bytes in order to enable the use of {@link IndexColorModel}.
     *
     * <p>The given map specifies the color to use for different ranges of values in the source image.
     * The ranges of values in the returned image may not be the same; this method is free to rescale them.
     * The {@link Color} arrays may have any length; colors will be interpolated as needed for fitting
     * the ranges of values in the destination image.</p>
     *
     * <p>The resulting image is suitable for visualization purposes, but should not be used for computation purposes.
     * There is no guarantee about the number of bands in returned image or about which formula is used for converting
     * floating point values to integer values.</p>
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>(none)</li>
     * </ul>
     *
     * @param  source  the image to recolor for visualization purposes.
     * @param  colors  colors to use for each range of values in the source image.
     * @return recolored image for visualization purposes only.
     */
    public RenderedImage visualize(final RenderedImage source, final Map<NumberRange<?>,Color[]> colors) {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("colors", colors);
        return visualize(new Visualization.Builder(source, colors.entrySet()));
    }

    /**
     * Returns an image where all sample values are indices of colors in an {@link IndexColorModel}.
     * If the given image stores sample values as unsigned bytes or short integers, then those values
     * are used as-is (they are not copied or converted). Otherwise this operation will convert sample
     * values to unsigned bytes in order to enable the use of {@link IndexColorModel}.
     *
     * <p>This method is similar to {@link #visualize(RenderedImage, Map)}
     * except that the {@link Map} argument is splitted in two parts: the ranges (map keys) are
     * {@linkplain Category#getSampleRange() encapsulated in <code>Category</code>} objects, themselves
     * {@linkplain SampleDimension#getCategories() encapsulated in <code>SampleDimension</code>} objects.
     * The colors (map values) are determined by a function receiving {@link Category} inputs.
     * This separation makes easier to apply colors based on criterion other than numerical values.
     * For example colors could be determined from {@linkplain Category#getName() category name} such as "Temperature",
     * or {@linkplain org.apache.sis.measure.MeasurementRange#unit() units of measurement}.
     * The {@link Color} arrays may have any length; colors will be interpolated as needed for fitting
     * the ranges of values in the destination image.</p>
     *
     * <p>The resulting image is suitable for visualization purposes, but should not be used for computation purposes.
     * There is no guarantee about the number of bands in returned image or about which formula is used for converting
     * floating point values to integer values.</p>
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getCategoryColors() Category colors}.</li>
     * </ul>
     *
     * @param  source  the image to recolor for visualization purposes.
     * @param  ranges  description of {@code source} bands, or {@code null} if none. This is typically
     *                 obtained by {@link org.apache.sis.coverage.grid.GridCoverage#getSampleDimensions()}.
     * @return recolored image for visualization purposes only.
     */
    public RenderedImage visualize(final RenderedImage source, final List<SampleDimension> ranges) {
        ArgumentChecks.ensureNonNull("source", source);
        return visualize(new Visualization.Builder(null, source, null, ranges));
    }

    /**
     * Returns an image as the resampling of the given image followed by a conversion to integer sample values.
     * This is a combination of the following methods, as a single image operation for avoiding creation of an
     * intermediate image step:
     *
     * <ol>
     *   <li><code>{@linkplain #resample(RenderedImage, Rectangle, MathTransform) resample}(source, bounds, toSource)</code></li>
     *   <li><code>{@linkplain #visualize(RenderedImage, List) visualize}(resampled, ranges)</code></li>
     * </ol>
     *
     * Combining above steps may be advantageous when the {@code resample(…)} result is not needed for anything
     * else than visualization. If the same resampling may be needed for computational purposes, then it may be
     * more advantageous to keep above method calls separated instead of using this {@code visualize(…)} method.
     *
     * <p>The resulting image is suitable for visualization purposes, but should not be used for computation purposes.
     * There is no guarantee about the number of bands in returned image or about which formula is used for converting
     * floating point values to integer values.</p>
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getInterpolation() Interpolation method} (nearest neighbor, bilinear, <i>etc</i>).</li>
     *   <li>{@linkplain #getFillValues() Fill values} for pixels outside source image.</li>
     *   <li>{@linkplain #getImageResizingPolicy() Image resizing policy} to apply
     *       if {@code bounds} size is not divisible by a tile size.</li>
     *   <li>{@linkplain #getPositionalAccuracyHints() Positional accuracy hints}
     *       for enabling faster resampling at the cost of lower precision.</li>
     *   <li>{@linkplain #getCategoryColors() Category colors}.</li>
     * </ul>
     *
     * @param  source    the image to be resampled and recolored.
     * @param  bounds    domain of pixel coordinates of resampled image to create.
     *                   Updated by this method if {@link Resizing#EXPAND} policy is applied.
     * @param  toSource  conversion of pixel coordinates from resampled image to {@code source} image.
     * @param  ranges    description of {@code source} bands, or {@code null} if none. This is typically
     *                   obtained by {@link org.apache.sis.coverage.grid.GridCoverage#getSampleDimensions()}.
     * @return resampled and recolored image for visualization purposes only.
     */
    public RenderedImage visualize(final RenderedImage source, final Rectangle bounds, final MathTransform toSource,
                                   final List<SampleDimension> ranges)
    {
        ArgumentChecks.ensureNonNull("source",   source);
        ArgumentChecks.ensureNonNull("bounds",   bounds);
        ArgumentChecks.ensureNonNull("toSource", toSource);
        ensureNonEmpty(bounds);
        return visualize(new Visualization.Builder(bounds, source, toSource, ranges));
    }

    /**
     * Finishes builder configuration and creates the {@link Visualization} image.
     */
    private RenderedImage visualize(final Visualization.Builder builder) {
        synchronized (this) {
            builder.layout                  = layout;
            builder.interpolation           = interpolation;
            builder.categoryColors          = colors;
            builder.fillValues              = fillValues;
            builder.positionalAccuracyHints = positionalAccuracyHints;
        }
        try {
            return builder.create(this);
        } catch (IllegalStateException | NoninvertibleTransformException e) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.UnconvertibleSampleValues), e);
        }
    }

    /**
     * Generates isolines at the specified levels computed from data provided by the given image.
     * Isolines will be computed for every bands in the given image.
     * For each band, the result is given as a {@code Map} where keys are the specified {@code levels}
     * and values are the isolines at the associated level.
     * If there are no isolines for a given level, there will be no corresponding entry in the map.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getExecutionMode() Execution mode} (parallel or sequential).</li>
     * </ul>
     *
     * @param  data       image providing source values.
     * @param  levels     values for which to compute isolines. An array should be provided for each band.
     *                    If there is more bands than {@code levels.length}, the last array is reused for
     *                    all remaining bands.
     * @param  gridToCRS  transform from pixel coordinates to geometry coordinates, or {@code null} if none.
     *                    Integer source coordinates are located at pixel centers.
     * @return the isolines for specified levels in each band. The {@code List} size is the number of bands.
     *         For each band, the {@code Map} size is equal or less than {@code levels[band].length}.
     *         Map keys are the specified levels, excluding those for which there are no isolines.
     *         Map values are the isolines as a Java2D {@link Shape}.
     * @throws ImagingOpException if an error occurred during calculation.
     */
    public List<NavigableMap<Double,Shape>> isolines(final RenderedImage data, final double[][] levels, final MathTransform gridToCRS) {
        final boolean parallel;
        synchronized (this) {
            parallel = parallel(data);
        }
        if (parallel) {
            return Isolines.toList(Isolines.parallelGenerate(data, levels, gridToCRS));
        } else try {
            return Isolines.toList(Isolines.generate(data, levels, gridToCRS));
        } catch (TransformException e) {
            throw (ImagingOpException) new ImagingOpException(null).initCause(e);
        }
    }

    /**
     * Returns {@code true} if the given object is an image processor
     * of the same class with the same configuration.
     *
     * @param  object  the other object to compare with this processor.
     * @return whether the other object is an image processor of the same class with the same configuration.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final ImageProcessor other = (ImageProcessor) object;
            final Mode          executionMode;
            final ErrorHandler  errorHandler;
            final Interpolation interpolation;
            final Number[]      fillValues;
            final Function<Category,Color[]> colors;
            final Quantity<?>[] positionalAccuracyHints;
            synchronized (this) {
                executionMode           = this.executionMode;
                errorHandler            = this.errorHandler;
                interpolation           = this.interpolation;
                fillValues              = this.fillValues;
                colors                  = this.colors;
                positionalAccuracyHints = this.positionalAccuracyHints;
            }
            synchronized (other) {
                return errorHandler.equals(other.errorHandler)    &&
                      executionMode.equals(other.executionMode)   &&
                      interpolation.equals(other.interpolation)   &&
                      Objects.equals(colors, other.colors)        &&
                      Arrays.equals(fillValues, other.fillValues) &&
                      Arrays.equals(positionalAccuracyHints, other.positionalAccuracyHints);
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for this image processor based on its current configuration.
     *
     * @return a hash code value for this processor.
     */
    @Override
    public synchronized int hashCode() {
        return Objects.hash(getClass(), errorHandler, executionMode, interpolation)
                + 37 * Arrays.hashCode(fillValues) + 31 * Objects.hashCode(colors)
                + 39 * Arrays.hashCode(positionalAccuracyHints);
    }

    /**
     * Returns an image processor with the same configuration than this processor.
     *
     * @return a clone of this image processor.
     */
    @Override
    public synchronized ImageProcessor clone() {
        try {
            return (ImageProcessor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
