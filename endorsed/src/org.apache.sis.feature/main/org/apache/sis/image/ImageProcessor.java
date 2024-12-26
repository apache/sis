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
import java.util.function.DoubleUnaryOperator;
import java.util.logging.LogRecord;
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
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.RegionOfInterest;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.privy.ImageUtilities;
import org.apache.sis.coverage.privy.TiledImage;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.system.Modules;
import org.apache.sis.image.processing.isoline.Isolines;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;


/**
 * A predefined set of operations on images.
 * After instantiation, {@code ImageProcessor} can be configured for the following aspects:
 *
 * <ul class="verbose">
 *   <li>
 *     {@linkplain #setImageLayout(ImageLayout) Preferences about the tiling}
 *     of an image in relationship with a given image size.
 *   </li><li>
 *     {@linkplain #setInterpolation(Interpolation) Interpolation method} to use during resampling operations.
 *   </li><li>
 *     {@linkplain #setFillValues(Number...) Fill values} to use for pixels that cannot be computed.
 *   </li><li>
 *     {@linkplain #setColorizer(Colorizer) Colorization algorithm} to apply for colorizing a computed image.
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
 * <h2>API design</h2>
 * Properties (setter methods) are used for values that can be applied unchanged on many different images.
 * For example, the {@linkplain #getInterpolation() interpolation method} can be specified once and used
 * unchanged for many {@link #resample resample(…)} operations.
 * On the other hand, method arguments are used for values that are usually specific to the image to process.
 * For example, the {@link MathTransform} argument given to the {@link #resample resample(…)} operation depends
 * tightly on the source image and destination bounds (also given in arguments); those information usually need
 * to be recomputed for each image.
 *
 * <h2>Deferred calculations</h2>
 * Methods in this class may compute the result at some later time after the method returned, instead of computing
 * the result immediately on method call. Consequently, unless otherwise specified, {@link RenderedImage} arguments
 * should be <em>stable</em>, i.e. pixel values should not be modified after method return.
 *
 * <h2>Area of interest</h2>
 * Some operations accept an optional <var>area of interest</var> argument specified as a {@link Shape} instance in
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
 * @author  Alexis Manin (Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.coverage.grid.GridCoverageProcessor
 *
 * @since 1.1
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
     * Shall never be null. Default value is {@link ImageLayout#DEFAULT}.
     *
     * @see #getImageLayout()
     * @see #setImageLayout(ImageLayout)
     */
    private ImageLayout layout;

    /**
     * Whether {@code ImageProcessor} can produce an image of different size compared to requested size.
     * An image may be resized if the requested size cannot be subdivided into tiles of reasonable size.
     * For example if the image width is a prime number, there is no way to divide the image horizontally with
     * an integer number of tiles. The only way to get an integer number of tiles is to change the image size.
     *
     * <p>The image resizing policy may be used by any operation that involve a {@linkplain #resample resampling}.
     * If a resizing is applied, the new size will be written in the {@code bounds} argument (a {@link Rectangle}).</p>
     *
     * @see #getImageResizingPolicy()
     * @see #setImageResizingPolicy(Resizing)
     *
     * @deprecated Replaced by {@link ImageLayout}.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public enum Resizing {
        /**
         * Image size is unmodified, the requested value is used unconditionally.
         * It may result in big tiles (potentially a single tile for the whole image)
         * if the image size is not divisible by a tile size.
         *
         * @deprecated Replaced by {@link ImageLayout#DEFAULT}.
         */
        @Deprecated
        NONE(ImageLayout.DEFAULT),

        /**
         * Image size can be increased. {@code ImageProcessor} will try to increase
         * by the smallest number of pixels allowing the image to be subdivided in tiles.
         *
         * @deprecated Replaced by {@code ImageLayout.DEFAULT.allowImageBoundsAdjustments(true)}.
         */
        @Deprecated
        EXPAND(ImageLayout.DEFAULT.allowImageBoundsAdjustments(true));

        /**
         * The layout corresponding to the enumeration value.
         */
        public final ImageLayout layout;

        /**
         * Creates a new enumeration value for the given size policy.
         */
        private Resizing(final ImageLayout layout) {
            this.layout = layout;
        }
    }

    /**
     * Interpolation to use during resample operations.
     *
     * @see #getInterpolation()
     * @see #setInterpolation(Interpolation)
     */
    private Interpolation interpolation;

    /**
     * The values to use for pixels that cannot be computed.
     * This array may be {@code null} or may contain {@code null} elements.
     * This is a "copy on write" array (elements are not modified).
     *
     * @see #getFillValues()
     * @see #setFillValues(Number...)
     */
    private Number[] fillValues;

    /**
     * Colorization algorithm to apply on computed image.
     * A null value means to use implementation-specific default.
     *
     * @see #getColorizer()
     * @see #setColorizer(Colorizer)
     */
    private Colorizer colorizer;

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
     * Returns the preferences about the tiling of an image in relationship with a given image size.
     * The {@code ImageLayout} determines characteristics (size, tile size, sample model, <i>etc.</i>)
     * of destination images.
     *
     * @return preferences about the tiling of an image in relationship with a given image size.
     * @since 1.5
     */
    public synchronized ImageLayout getImageLayout() {
        return layout;
    }

    /**
     * Sets the preferences (size, tile size, sample model, <i>etc.</i>) of destination images.
     *
     * @param layout  new preferences about the tiling of an image in relationship with a given image size.
     * @since 1.5
     */
    public synchronized void setImageLayout(final ImageLayout layout) {
        this.layout = Objects.requireNonNull(layout);
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
        interpolation = Objects.requireNonNull(method);
    }

    /**
     * Returns the values to use for pixels that cannot be computed.
     * This method returns a copy of the array set by the last call to {@link #setFillValues(Number...)}.
     *
     * @return fill values to use for pixels that cannot be computed, or {@code null} for the defaults.
     */
    public synchronized Number[] getFillValues() {
        return (fillValues != null) ? fillValues.clone() : null;
    }

    /**
     * Sets the values to use for pixels that cannot be computed. The given array may be {@code null} or may contain
     * {@code null} elements for default values. Those defaults are zero for images storing sample values as integers,
     * or {@link Float#NaN} or {@link Double#NaN} for images storing sample values as floating point numbers. If the
     * given array contains less elements than the number of bands in an image, missing elements will be assumed null.
     * If the given array contains more elements than the number of bands, extraneous elements will be ignored.
     *
     * @param  values  fill values to use for pixels that cannot be computed, or {@code null} for the defaults.
     */
    public synchronized void setFillValues(final Number... values) {
        fillValues = (values != null) ? values.clone() : null;
    }

    /**
     * Returns the colorization algorithm to apply on computed images, or {@code null} for default.
     * This method returns the value set by the last call to {@link #setColorizer(Colorizer)}.
     *
     * @return colorization algorithm to apply on computed image, or {@code null} for default.
     *
     * @since 1.4
     */
    public synchronized Colorizer getColorizer() {
        return colorizer;
    }

    /**
     * Sets the colorization algorithm to apply on computed images.
     * The colorizer is invoked when the rendered image produced by an {@code ImageProcessor} operation
     * needs a {@link ColorModel} which is not straightforward.
     *
     * <h4>Examples</h4>
     * <p>The color model of a {@link #resample(RenderedImage, Rectangle, MathTransform) resample(…)}
     * operation is straightforward: it is the same {@link ColorModel} than the source image.
     * Consequently the colorizer is not invoked for that operation.</p>
     *
     * <p>But by contrast, the color model of an {@link #aggregateBands(RenderedImage...) aggregateBands(…)}
     * operation cannot be determined in such straightforward way.
     * If three or four bands are aggregated, should they be interpreted as an (A)RGB image?
     * The {@link Colorizer} allows to specify the desired behavior.</p>
     *
     * @param colorizer colorization algorithm to apply on computed image, or {@code null} for default.
     *
     * @since 1.4
     */
    public synchronized void setColorizer(final Colorizer colorizer) {
        this.colorizer = colorizer;
    }

    /**
     * Returns whether {@code ImageProcessor} can produce an image of different size compared to requested size.
     * If this processor can use a different size, the enumeration value specifies what kind of changes may be applied.
     *
     * @return the image resizing policy.
     *
     * @deprecated Replaced by {@link #getImageLayout()}.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public synchronized Resizing getImageResizingPolicy() {
        return layout.isImageBoundsAdjustmentAllowed ? Resizing.EXPAND : Resizing.NONE;
    }

    /**
     * Sets whether {@code ImageProcessor} can produce an image of different size compared to requested size.
     *
     * @param  policy   the new image resizing policy.
     *
     * @deprecated Replaced by {@link #setImageLayout(ImageLayout)}.
     */
    @Deprecated(since="1.5", forRemoval=true)
    public synchronized void setImageResizingPolicy(final Resizing policy) {
        layout = policy.layout;
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
     * For example, the given array can contain an accuracy in metres and an accuracy in seconds,
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
        executionMode = Objects.requireNonNull(mode);
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
        errorHandler = Objects.requireNonNull(handler);
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
    public static DoubleUnaryOperator filterNodataValues(final Number... values) {
        return (values != null) ? StatisticsCalculator.filterNodataValues(values) : null;
    }

    /**
     * Returns statistics (minimum, maximum, mean, standard deviation) on each bands of the given image.
     * Invoking this method is equivalent to invoking the {@link #statistics statistics(…)} method and
     * extracting immediately the statistics property value, except that this method guarantees that all
     * statistics are non-null and supports custom {@linkplain #setErrorHandler error handlers}.
     *
     * <p>If {@code areaOfInterest} is null and {@code sampleFilters} is {@code null} or empty,
     * then the default behavior is as below:</p>
     * <ul>
     *   <li>If the {@value PlanarImage#STATISTICS_KEY} property value exists in the given image,
     *       then that value is returned with the null array elements (if any) replaced by computed values.
     *       Note that the returned statistics are not necessarily for the whole image.
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
     * This method fetches (from property values) or computes statistics immediately.
     * Changes in the {@code source} image after this method call do not change the results.
     *
     * @param  source          the image for which to compute statistics.
     * @param  areaOfInterest  pixel coordinates of the area of interest, or {@code null} for the default.
     * @param  sampleFilters   converters to apply on sample values before to add them to statistics, or
     *         {@code null} or an empty array if none. The array may have any length and may contain null elements.
     *         For all {@code i < numBands}, non-null {@code sampleFilters[i]} are applied to band <var>i</var>.
     * @return the statistics of sample values in each band. Guaranteed non-null and without null element.
     * @throws ImagingOpException if an error occurred during calculation
     *         and the error handler is {@link ErrorHandler#THROW}.
     *
     * @see #statistics(RenderedImage, Shape, DoubleUnaryOperator...)
     * @see #filterNodataValues(Number...)
     * @see PlanarImage#STATISTICS_KEY
     */
    public Statistics[] valueOfStatistics(RenderedImage source, final Shape areaOfInterest,
                                          final DoubleUnaryOperator... sampleFilters)
    {
        ArgumentChecks.ensureNonNull("source", source);
        int[] bandsToCompute = null;
        Statistics[] statistics = null;
        if (areaOfInterest == null && (sampleFilters == null || ArraysExt.allEquals(sampleFilters, null))) {
            final Object property = source.getProperty(PlanarImage.STATISTICS_KEY);
            if (property instanceof Statistics[]) {
                statistics = ArraysExt.resize((Statistics[]) property, ImageUtilities.getNumBands(source));
                /*
                 * Verify that all array elements are non-null. If any null element is found,
                 * we will compute statistics but only for the missing bands.
                 */
                bandsToCompute = new int[statistics.length];
                int n = 0;
                for (int i=0; i<statistics.length; i++) {
                    if (statistics[i] == null) {
                        bandsToCompute[n++] = i;
                    }
                }
                if (n == 0) return statistics;
                bandsToCompute = ArraysExt.resize(bandsToCompute, n);
                source = selectBands(source, bandsToCompute);
            }
        }
        /*
         * Compute statistics either of all bands, or on a subset
         * of the bands if only some of them have null statistics.
         */
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
        final var calculator = new StatisticsCalculator(source, areaOfInterest, sampleFilters, parallel, failOnException);
        final Object property = calculator.getProperty(PlanarImage.STATISTICS_KEY);
        calculator.logAndClearError(ImageProcessor.class, "valueOfStatistics", errorListener);
        final var computed = (Statistics[]) property;
        if (bandsToCompute == null) {
            return computed;
        }
        for (int i=0; i<bandsToCompute.length; i++) {
            statistics[bandsToCompute[i]] = computed[i];
        }
        return statistics;
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
     * <h5>API design note</h5>
     * The {@code areaOfInterest} and {@code sampleFilters} arguments are complementary.
     * Both of them filter the data accepted for statistics. In ISO 19123 terminology,
     * the {@code areaOfInterest} argument filters the <i>coverage domain</i> while
     * the {@code sampleFilters} argument filters the <i>coverage range</i>.
     * Another connection with OGC/ISO standards is that {@link DoubleUnaryOperator} in this context
     * does the same work as {@linkplain SampleDimension#getTransferFunction() transfer function}.
     * It can be useful for images not managed by a {@link org.apache.sis.coverage.grid.GridCoverage}.
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
     * Returns an image with the same sample values as the given image, but with its color ramp stretched between
     * specified or inferred bounds. For example, in a gray scale image, pixels with the minimum value will be black
     * and pixels with the maximum value will be white. This operation is a kind of <i>tone mapping</i>,
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
     *     <td>{@link SampleDimension} or {@code SampleDimension[]}</td>
     *   </tr>
     * </table>
     *
     * <b>Note:</b> if no value is associated to the {@code "sampleDimensions"} key, then the default
     * value will be the {@value PlanarImage#SAMPLE_DIMENSIONS_KEY} image property value if defined.
     * That value can be an array, in which case the sample dimension of the visible band is taken.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>(none)</li>
     * </ul>
     *
     * <h4>Limitation</h4>
     * Current implementation can stretch only gray scale images (a future version may extend support to images
     * using {@linkplain java.awt.image.IndexColorModel index color models}). If this method cannot stretch the
     * color ramp, for example because the given image is an RGB image, then the image is returned unchanged.
     *
     * @param  source     the image to recolor.
     * @param  modifiers  modifiers for narrowing the range of values, or {@code null} if none.
     * @return the image with color ramp stretched between the specified or calculated bounds,
     *         or {@code image} unchanged if the operation cannot be applied on the given image.
     * @throws IllegalArgumentException if the value associated to one of about keys is not of expected type.
     */
    public RenderedImage stretchColorRamp(final RenderedImage source, final Map<String,?> modifiers) {
        ArgumentChecks.ensureNonNull("source", source);
        return RecoloredImage.stretchColorRamp(this, source, modifiers);
    }

    /**
     * Returns an image augmented with user-defined property values.
     * The specified properties overwrite any property that may be defined by the source image.
     * When an {@linkplain RenderedImage#getProperty(String) image property value is requested}, the steps are:
     *
     * <ol>
     *   <li>If the {@code properties} map has an entry for the property name, returns the associated value.
     *       It may be {@code null}.</li>
     *   <li>Otherwise if the property is defined by the source image, returns its value.
     *       It may be {@code null}.</li>
     *   <li>Otherwise returns {@link java.awt.Image#UndefinedProperty}.</li>
     * </ol>
     *
     * The given {@code properties} map is retained by reference in the returned image.
     * The {@code Map} is <em>not</em> copied in order to allow
     * the use of custom implementations doing deferred calculations.
     * If the caller intends to modify the map content after this method call,
     * (s)he should use a {@link java.util.concurrent.ConcurrentMap}.
     *
     * <p>The returned image is "live": changes in {@code source} image properties or in
     * {@code properties} map entries are immediately reflected in the returned image.</p>
     *
     * <p>Null are valid image property values. An entry associated with the {@code null}
     * value in the {@code properties} map is not the same as an absence of entry.</p>
     *
     * @param  source       the source image to augment with user-specified property values.
     * @param  properties   properties overwriting or completing {@code source} properties.
     * @return an image augmented with the specified properties.
     *
     * @see RenderedImage#getPropertyNames()
     * @see RenderedImage#getProperty(String)
     *
     * @since 1.4
     */
    public RenderedImage addUserProperties(final RenderedImage source, final Map<String,Object> properties) {
        return unique(new UserProperties(source, properties));
    }

    /**
     * Selects a subset of bands in the given image. This method can also be used for changing band order
     * or repeating the same band from the source image. If the specified {@code bands} are the same as
     * the source image bands in the same order, then {@code source} is returned directly.
     *
     * <p>This method returns an image sharing the same data buffer as the source image;
     * pixel values are not copied. Consequently, changes in the source image are reflected
     * immediately in the returned image.</p>
     *
     * <p>If the given image is an instance of {@link WritableRenderedImage},
     * then the returned image will also be a {@link WritableRenderedImage}.
     * In such case values written in the returned image will be written directly in the source image.</p>
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
        return BandSelectImage.create(source, true, bands.clone());
    }

    /**
     * Aggregates in a single image all the bands of all specified images, in order.
     * All sources images should map pixel coordinates to the same geospatial locations.
     * A pixel at coordinates (<var>x</var>, <var>y</var>) in the aggregated image will
     * contain values from the pixels at the same coordinates in all source images.
     * The result image will be bounded by the intersection of all source images.
     *
     * <p>This convenience method delegates to {@link #aggregateBands(RenderedImage[], int[][])}.
     * See that method for more information on restrictions, writable images, memory saving and
     * properties used.</p>
     *
     * @param  sources  images whose bands shall be aggregated, in order. At least one image must be provided.
     * @return the aggregated image, or {@code sources[0]} returned directly if only one image was supplied.
     * @throws IllegalArgumentException if there is an incompatibility between some source images.
     *
     * @see #aggregateBands(RenderedImage[], int[][])
     *
     * @since 1.4
     */
    public RenderedImage aggregateBands(final RenderedImage... sources) {
        return aggregateBands(sources, (int[][]) null);
    }

    /**
     * Aggregates in a single image the specified bands of a sequence of source images, in order.
     * This method performs the same work as {@link #aggregateBands(RenderedImage...)},
     * but with the possibility to specify the bands to retain in each source image.
     * The {@code bandsPerSource} argument specifies the bands to select in each source image.
     * That array can be {@code null} for selecting all bands in all source images,
     * or may contain {@code null} elements for selecting all bands of the corresponding image.
     * An empty array element (i.e. zero band to select) discards the corresponding source image.
     * In the latter case, the discarded element in the {@code sources} array may be {@code null}.
     *
     * <h4>Restrictions</h4>
     * All images shall use the same {@linkplain SampleModel#getDataType() data type},
     * and all source images shall intersect each other with a non-empty intersection area.
     * However it is not required that all images have the same bounds or the same tiling scheme.
     *
     * <h4>Writable image</h4>
     * If all source images are {@link WritableRenderedImage} instances,
     * then the returned image will also be a {@link WritableRenderedImage}.
     * In such case values written in the returned image will be copied back
     * to the source images.
     *
     * <h4>Memory saving</h4>
     * The returned image may opportunistically share the underlying data arrays of
     * some source images. Bands are really copied only when sharing is not possible.
     * The actual strategy may be a mix of both arrays sharing and bands copies.
     *
     * <h4>Repeated bands</h4>
     * For any value of <var>i</var>, the array at {@code bandsPerSource[i]} shall not contain duplicated values.
     * This restriction is for capturing common errors, in order to reduce the risk of accidental band repetition.
     * However the same band can be repeated indirectly if the same image is repeated at different values of <var>i</var>.
     * But even when a source band is referenced many times, all occurrences still share pixel data copied at most once.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getColorizer() Colorizer}.</li>
     *   <li>{@linkplain #getExecutionMode() Execution mode} (parallel or sequential).</li>
     * </ul>
     *
     * @param  sources  images whose bands shall be aggregated, in order. At least one image must be provided.
     * @param  bandsPerSource  bands to use for each source image, in order. May contain {@code null} elements.
     * @return the aggregated image, or one of the sources if it can be used directly.
     * @throws IllegalArgumentException if there is an incompatibility between some source images
     *         or if some band indices are duplicated or outside their range of validity.
     *
     * @since 1.4
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public RenderedImage aggregateBands(final RenderedImage[] sources, final int[][] bandsPerSource) {
        ArgumentChecks.ensureNonEmpty("sources", sources);
        final Colorizer colorizer;
        final boolean parallel;
        synchronized (this) {
            colorizer = this.colorizer;
            parallel = executionMode != Mode.SEQUENTIAL;
        }
        // `allowSharing` is currently hard-coded to `true`, but it may change in a future version.
        return BandAggregateImage.create(sources, bandsPerSource, colorizer, true, true, parallel);
    }

    /**
     * Creates a new image overlay or returns one of the given sources if equivalent.
     * All source images shall have the same pixel coordinate system, but they may have different bounding boxes,
     * tile sizes and tile indices. Images are drawn in reverse order: the last source image is drawn first, and
     * the first source image is drawn last on top of all other images. All images are considered fully opaque,
     * including the alpha channel which is handled as an ordinary band.
     *
     * <p>The returned image may have less sources than the ones given in argument if this method determines
     * that some sources will never be drawn (i.e., are fully hidden behind the first images).
     * If only one source appears to be effectively used, this method returns that image directly.</p>
     *
     * <h4>Preconditions</h4>
     * All source images shall have the same number of bands (but not necessarily the same sample model).
     * All source images should have equivalent color model, otherwise color consistency is not guaranteed.
     * At least one image shall intersect the given bounds.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getImageLayout() Image layout} for the desired sample model and color model.</li>
     *   <li>{@linkplain ImageLayout#isImageBoundsAdjustmentAllowed Image bounds adjustment flag} for deciding
     *       whether to use a modified image size if {@code bounds} is not divisible by a tile size.</li>
     *   <li>{@linkplain #getColorizer() Colorizer} for customizing the rendered image color model.</li>
     * </ul>
     *
     * @param  sources  the images to overlay. Null array elements are ignored.
     * @param  bounds   range of pixel coordinates, or {@code null} for the union of all source images.
     * @return the image overlay, or one of the given sources if only one is suitable.
     * @throws IllegalArgumentException if there is an incompatibility between some source images
     *         or if no image intersect the given bounds.
     *
     * @since 1.5
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public RenderedImage overlay(final RenderedImage[] sources, final Rectangle bounds) {
        ArgumentChecks.ensureNonEmpty("sources", sources);
        final ImageLayout layout;
        final Colorizer colorizer;
        final boolean parallel;
        synchronized (this) {
            layout = this.layout;
            colorizer = this.colorizer;
            parallel = executionMode != Mode.SEQUENTIAL;
        }
        return ImageOverlay.create(sources, bounds, layout.sampleModel, colorizer,
                layout.isTileSizeAdjustmentAllowed | (bounds != null), parallel);
    }

    /**
     * Reformats the given image with a different sample model.
     * This operation <em>copies</em> the pixel values in a new image.
     * Despite the copies being done on a tile-by-tile basis when each tile is  first requested,
     * this is still a relatively costly operation compared to the usual Apache <abbr>SIS</abbr>
     * approach of creating views as much as possible. Therefore, this method should be used only
     * when necessary.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getImageLayout() Image layout} for the default sample model.</li>
     *   <li>{@linkplain ImageLayout#isImageBoundsAdjustmentAllowed Image bounds adjustment flag} for deciding
     *       whether to use a modified image size if the source image size is not divisible by a tile size.</li>
     * </ul>
     *
     * @param  source       the images to reformat.
     * @param  sampleModel  the desired sample model.
     *         Can be null only if a default sample model is specified by {@link ImageLayout#sampleModel}.
     * @return the reformatted image.
     *
     * @since 1.5
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public RenderedImage reformat(final RenderedImage source, SampleModel sampleModel) {
        ArgumentChecks.ensureNonNull("source", source);
        final ImageLayout layout;
        final boolean parallel;
        synchronized (this) {
            layout = this.layout;
            parallel = executionMode != Mode.SEQUENTIAL;
        }
        if (sampleModel == null) {
            sampleModel = layout.sampleModel;
            ArgumentChecks.ensureNonNull("sampleModel", sampleModel);
        }
        return ImageOverlay.create(new RenderedImage[] {source}, null, sampleModel, null, layout.isTileSizeAdjustmentAllowed, parallel);
    }

    /**
     * Applies a mask defined by a geometric shape. If {@code maskInside} is {@code true},
     * then all pixels inside the given shape are set to the {@linkplain #getFillValues() fill values}.
     * If {@code maskInside} is {@code false}, then the mask is reversed:
     * the pixels set to fill values are the ones outside the shape.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getFillValues() Fill values} values to assign to pixels inside/outside the shape.</li>
     * </ul>
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
    @SuppressWarnings("LocalVariableHidesMemberVariable")
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
     * the same type as the source values. If the result values are stored as integers, then they are
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
     *   <li>{@linkplain #getColorizer() Colorizer} for customizing the rendered image color model.</li>
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
     * @return the image which computes converted values from the given source.
     *
     * @see GridCoverageProcessor#convert(GridCoverage, MathTransform1D[], Function)
     *
     * @since 1.4
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public RenderedImage convert(final RenderedImage source, final NumberRange<?>[] sourceRanges,
                                 MathTransform1D[] converters, final DataType targetType)
    {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("converters", converters);
        ArgumentChecks.ensureNonNull("targetType", targetType);
        ArgumentChecks.ensureCountBetween("converters", true, 1, ImageUtilities.getNumBands(source), converters.length);
        converters = converters.clone();
        for (int i=0; i<converters.length; i++) {
            ArgumentChecks.ensureNonNullElement("converters", i, converters[i]);
        }
        final ImageLayout layout;
        final Colorizer colorizer;
        synchronized (this) {
            layout = this.layout;
            colorizer = this.colorizer;
        }
        // No need to clone `sourceRanges` because it is not stored by `BandedSampleConverter`.
        return unique(BandedSampleConverter.create(source, layout, sourceRanges, converters, targetType, colorizer));
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
     * That transform should map {@linkplain org.apache.sis.coverage.grid.PixelInCell#CELL_CENTER pixel centers}.
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
     *   <li>{@linkplain ImageLayout#isImageBoundsAdjustmentAllowed Image bounds adjustment flag} for deciding
     *       whether to use a modified image size if {@code bounds} size is not divisible by a tile size.</li>
     *   <li>{@linkplain #getPositionalAccuracyHints() Positional accuracy hints}
     *       for enabling faster resampling at the cost of lower precision.</li>
     * </ul>
     *
     * <h4>Result relationship with source</h4>
     * Changes in the source image are reflected in the returned images
     * if the source image notifies {@linkplain java.awt.image.TileObserver tile observers}.
     *
     * @param  source    the image to be resampled.
     * @param  bounds    domain of pixel coordinates of resampled image to create. Fields are updated in-place
     *                   by this method if the {@link ImageLayout#isImageBoundsAdjustmentAllowed} flag is true.
     * @param  toSource  conversion of pixel center coordinates from resampled image to {@code source} image.
     * @return resampled image (may be {@code source}).
     *
     * @see GridCoverageProcessor#resample(GridCoverage, GridGeometry)
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
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
            final WritableRenderedImage destination = layout.getDestination();
            final SampleModel rsm = layout.createCompatibleSampleModel(source, bounds);
            final var image = new ResampledImage(source, rsm, layout.getPreferredMinTile(), bounds, toSource,
                                                 interpolation, fillValues, positionalAccuracyHints);
            image.setDestination(destination);
            resampled = unique(image);
            if (destination != null) {
                return resampled;           // Preserve the color model of the destination.
            }
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
     * Computes immediately all tiles in the given region of interest, then returns an image with those tiles ready.
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
        if (source instanceof PrefetchedImage) {
            source = ((PrefetchedImage) source).source;
        }
        final boolean parallel;
        final ErrorHandler errorListener;
        synchronized (this) {
            parallel = parallel(source);
            errorListener = errorHandler;
        }
        final var image = new PrefetchedImage(source, areaOfInterest, errorListener, parallel);
        return image.isEmpty() ? source : image;
    }

    /**
     * Returns an image where all sample values are indices of colors in an {@link IndexColorModel}.
     * If the given image stores sample values as unsigned bytes or short integers, then those values
     * are used as-is (they are not copied or converted). Otherwise this operation will convert sample
     * values to unsigned bytes in order to enable the use of {@link IndexColorModel}.
     *
     * <p>The resulting image is suitable for visualization purposes, but should not be used for computation purposes.
     * There is no guarantee about the number of bands in returned image or about which formula is used for converting
     * floating point values to integer values.</p>
     *
     * <h4>How to specify colors</h4>
     * The image colors can be controlled by the {@link Colorizer} set on this image processor.
     * It is possible to {@linkplain Colorizer#forInstance(ColorModel) specify explicitly} the
     * {@link ColorModel} to use, but this approach is unsafe because it depends on the pixel values
     * <em>after</em> their conversion to the visualization image, which is implementation dependent.
     * A safer approach is to define colors relative to pixel values <em>before</em> their conversions.
     * It can be done in two ways, depending on whether the {@value PlanarImage#SAMPLE_DIMENSIONS_KEY}
     * image property is defined or not.
     * Those two ways are described in next sections and can be combined in a chain of fallbacks.
     * For example the following colorizer will choose colors based on sample dimensions if available,
     * or fallback on predefined ranges of pixel values otherwise:
     *
     * {@snippet lang="java" :
     *     Function<Category,Color[]>  flexible   = ...;
     *     Map<NumberRange<?>,Color[]> predefined = ...;
     *     processor.setColorizer(Colorizer.forCategories(flexible)     // Preferred way.
     *                    .orElse(Colorizer.forRanges(predefined)));    // Fallback.
     *     }
     *
     * <h5>Specifying colors for ranges of pixel values</h5>
     * When no {@link SampleDimension} information is available, the recommended way to specify colors is like below.
     * In this example, <var>min</var> and <var>max</var> are minimum and maximum values
     * (inclusive in this example, but they could be exclusive as well) in the <em>source</em> image.
     * Those extrema can be floating point values. This example specifies only one range of values,
     * but arbitrary numbers of non-overlapping ranges are allowed.
     *
     * {@snippet lang="java" :
     *     NumberRange<?> range = NumberRange.create(min, true, max, true);
     *     Color[] colors = {Color.BLUE, Color.MAGENTA, Color.RED};
     *     processor.setColorizer(Colorizer.forRanges(Map.of(range, colors)));
     *     RenderedImage visualization = processor.visualize(source, null);
     *     }
     *
     * The map given to the colorizer specifies the colors to use for different ranges of values in the source image.
     * The ranges of values in the returned image may not be the same; this method is free to rescale them.
     * The {@link Color} arrays may have any length; colors will be interpolated as needed for fitting
     * the ranges of values in the destination image.
     *
     * <h5>Specifying colors for sample dimension categories</h5>
     * If {@link SampleDimension} information is available, a more flexible way to specify colors
     * is to associate colors to category names instead of predetermined ranges of pixel values.
     * The ranges will be inferred indirectly, {@linkplain Category#getSampleRange() from the categories}
     * themselves {@linkplain SampleDimension#getCategories() encapsulated in sample dimensions}.
     * The colors are determined by a function receiving {@link Category} inputs.
     *
     * {@snippet lang="java" :
     *     Map<String,Color[]> colors = Map.of(
     *         "Temperature", new Color[] {Color.BLUE, Color.MAGENTA, Color.RED},
     *         "Wind speed",  new Color[] {Color.GREEN, Color.CYAN, Color.BLUE});
     *
     *     processor.setColorizer(Colorizer.forCategories((category) ->
     *         colors.get(category.getName().toString(Locale.ENGLISH))));
     *
     *     RenderedImage visualization = processor.visualize(source, ranges);
     *     }
     *
     * This separation makes easier to apply colors based on criterion other than numerical values.
     * For example, colors could be determined from {@linkplain Category#getName() category name} such as "Temperature",
     * or {@linkplain org.apache.sis.measure.MeasurementRange#unit() units of measurement}.
     * The {@link Color} arrays may have any length; colors will be interpolated as needed for fitting
     * the ranges of values in the destination image.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getColorizer() Colorizer} for customizing the rendered image color model.</li>
     * </ul>
     *
     * @param  source  the image to recolor for visualization purposes.
     * @return recolored image for visualization purposes only.
     *
     * @see Colorizer#forRanges(Map)
     * @see Colorizer#forCategories(Function)
     * @see PlanarImage#SAMPLE_DIMENSIONS_KEY
     *
     * @since 1.4
     */
    public RenderedImage visualize(final RenderedImage source) {
        ArgumentChecks.ensureNonNull("source", source);
        return visualize(new Visualization.Builder(null, source, null));
    }

    /**
     * Returns an image as the resampling of the given image followed by a conversion to integer sample values.
     * This is a combination of the following methods, as a single image operation for avoiding creation of an
     * intermediate image step:
     *
     * <ol>
     *   <li><code>{@linkplain #resample(RenderedImage, Rectangle, MathTransform) resample}(source, bounds, toSource)</code></li>
     *   <li><code>{@linkplain #visualize(RenderedImage) visualize}(resampled)</code></li>
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
     *   <li>{@linkplain ImageLayout#isImageBoundsAdjustmentAllowed Image bounds adjustment flag} for deciding
     *       whether to use a modified image size if {@code bounds} size is not divisible by a tile size.</li>
     *   <li>{@linkplain #getPositionalAccuracyHints() Positional accuracy hints}
     *       for enabling faster resampling at the cost of lower precision.</li>
     *   <li>{@linkplain #getColorizer() Colorizer} for customizing the rendered image color model.</li>
     * </ul>
     *
     * @param  source    the image to be resampled and recolored.
     * @param  bounds    domain of pixel coordinates of resampled image to create. Fields are updated in-place
     *                   by this method if the {@link ImageLayout#isImageBoundsAdjustmentAllowed} flag is true.
     * @param  toSource  conversion of pixel center coordinates from resampled image to {@code source} image.
     * @return resampled and recolored image for visualization purposes only.
     *
     * @see #resample(RenderedImage, Rectangle, MathTransform)
     *
     * @since 1.4
     */
    public RenderedImage visualize(final RenderedImage source, final Rectangle bounds, final MathTransform toSource) {
        ArgumentChecks.ensureNonNull("source",   source);
        ArgumentChecks.ensureNonNull("bounds",   bounds);
        ArgumentChecks.ensureNonNull("toSource", toSource);
        ensureNonEmpty(bounds);
        return visualize(new Visualization.Builder(bounds, source, toSource));
    }

    /**
     * Finishes builder configuration and creates the {@link Visualization} image.
     */
    private RenderedImage visualize(final Visualization.Builder builder) {
        synchronized (this) {
            builder.layout                  = layout;
            builder.interpolation           = interpolation;
            builder.colorizer               = colorizer;
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
        ArgumentChecks.ensureNonNull("data", data);
        ArgumentChecks.ensureNonNull("levels", levels);
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
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final ImageProcessor other = (ImageProcessor) object;
            final Mode          executionMode;
            final ErrorHandler  errorHandler;
            final Interpolation interpolation;
            final Number[]      fillValues;
            final ImageLayout   layout;
            final Colorizer     colorizer;
            final Quantity<?>[] positionalAccuracyHints;
            synchronized (this) {
                executionMode           = this.executionMode;
                errorHandler            = this.errorHandler;
                interpolation           = this.interpolation;
                fillValues              = this.fillValues;
                layout                  = this.layout;
                colorizer               = this.colorizer;
                positionalAccuracyHints = this.positionalAccuracyHints;
            }
            synchronized (other) {
                return layout.equals(other.layout)                &&
                      errorHandler.equals(other.errorHandler)     &&
                      executionMode.equals(other.executionMode)   &&
                      interpolation.equals(other.interpolation)   &&
                      Objects.equals(colorizer, other.colorizer)  &&
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
        return Objects.hash(getClass(), errorHandler, executionMode, colorizer, interpolation, layout)
                + 37 * Arrays.hashCode(fillValues)
                + 39 * Arrays.hashCode(positionalAccuracyHints);
    }

    /**
     * Returns an image processor with the same configuration as this processor.
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
