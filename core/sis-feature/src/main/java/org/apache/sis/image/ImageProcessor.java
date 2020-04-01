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

import java.util.List;
import java.util.Objects;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import java.awt.image.RasterFormatException;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.coverage.j2d.TileOpExecutor;
import org.apache.sis.internal.coverage.j2d.TiledImage;
import org.apache.sis.referencing.operation.transform.MathTransforms;


/**
 * A predefined set of operations on images as convenience methods.
 * After instantiation, {@code ImageProcessor} can be configured for the following aspects:
 *
 * <ul class="verbose">
 *   <li>
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
 * <h2>Error handling</h2>
 * If an exception occurs during the computation of a tile, then the {@code ImageProcessor} behavior
 * is controlled by the {@link #getErrorAction() errorAction} property:
 *
 * <ul>
 *   <li>If {@link ErrorAction#THROW}, the exception is wrapped in an {@link ImagingOpException} and thrown.</li>
 *   <li>If {@link ErrorAction#LOG}, the exception is logged and a partial result is returned.</li>
 *   <li>If any other value, the exception is wrapped in a {@link LogRecord} and sent to that filter.
 *     The filter can store the log record, for example for showing later in a graphical user interface (GUI).
 *     If the filter returns {@code true}, the log record is also logged, otherwise it is silently discarded.
 *     In both cases a partial result is returned.</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * {@code ImageProcessor} is thread-safe if its configuration is not modified after construction.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see org.apache.sis.coverage.grid.GridCoverageProcessor
 *
 * @since 1.1
 * @module
 */
public class ImageProcessor {
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
    private static RenderedImage unique(final RenderedImage image) {
        return CACHE.unique(image);
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
     *
     * @see #getFillValues()
     * @see #setFillValues(Number...)
     */
    private Number[] fillValues;

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
     * @see #getErrorAction()
     * @see #setErrorAction(Filter)
     */
    private Filter errorAction;

    /**
     * Specifies how exceptions occurring during calculation should be handled.
     * This enumeration provides common actions, but the set of values that can
     * be specified to {@link #setErrorAction(Filter)} is not limited to this enumeration.
     *
     * @see #getErrorAction()
     * @see #setErrorAction(Filter)
     */
    public enum ErrorAction implements Filter {
        /**
         * Exceptions are wrapped in an {@link ImagingOpException} and thrown.
         * In such case, no result is available. This is the default action.
         */
        THROW,

        /**
         * Exceptions are wrapped in a {@link LogRecord} and logged at {@link java.util.logging.Level#WARNING}.
         * Only one log record is created for all tiles that failed for the same operation on the same image.
         * A partial result may be available.
         *
         * <p>Users are encouraged to use {@link #THROW} or to specify their own {@link Filter}
         * instead than using this error action, because not everyone read logging records.</p>
         */
        LOG;

        /**
         * Unconditionally returns {@code true} for allowing the given record to be logged.
         * This method is not useful for this {@code ErrorAction} enumeration, but is useful
         * for other instances given to {@link #setErrorAction(Filter)}.
         *
         * @param  record  the error that occurred during computation of a tile.
         * @return always {@code true}.
         */
        @Override
        public boolean isLoggable(final LogRecord record) {
            return true;
        }
    }

    /**
     * Creates a new set of image operations with default configuration.
     * The execution mode is initialized to {@link Mode#DEFAULT} and the error action to {@link ErrorAction#THROW}.
     */
    public ImageProcessor() {
        executionMode = Mode.DEFAULT;
        errorAction   = ErrorAction.THROW;
        interpolation = Interpolation.BILINEAR;
    }

    /**
     * Returns the interpolation method to use during resample operations.
     *
     * @return interpolation method to use during resample operations.
     *
     * @see #resample(Rectangle, MathTransform, RenderedImage)
     */
    public Interpolation getInterpolation() {
        return interpolation;
    }

    /**
     * Sets the interpolation method to use during resample operations.
     *
     * @param  method  interpolation method to use during resample operations.
     *
     * @see #resample(Rectangle, MathTransform, RenderedImage)
     */
    public void setInterpolation(final Interpolation method) {
        ArgumentChecks.ensureNonNull("method", method);
        interpolation = method;
    }

    /**
     * Returns the values to use for pixels that can not be computed.
     * This method returns a copy of the array set by the last call to {@link #setFillValues(Number...)}.
     *
     * @return fill values to use for pixels that can not be computed, or {@code null} for the defaults.
     */
    public Number[] getFillValues() {
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
    public void setFillValues(final Number... values) {
        fillValues = (values != null) ? values.clone() : null;
    }

    /**
     * Returns whether operations can be executed in parallel.
     * If {@link Mode#SEQUENTIAL}, operations are executed sequentially in the caller thread.
     * If {@link Mode#PARALLEL}, some operations may be parallelized using an arbitrary number of threads.
     *
     * @return whether the operations can be executed in parallel.
     */
    public Mode getExecutionMode() {
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
     * (it will actually have no effect in this particular case) or with Apache SIS implementations of
     * {@link RenderedImage}.</p>
     *
     * @param  mode  whether the operations can be executed in parallel.
     */
    public void setExecutionMode(final Mode mode) {
        ArgumentChecks.ensureNonNull("mode", mode);
        executionMode = mode;
    }

    /**
     * Whether the operations can be executed in parallel for the specified image.
     */
    private boolean parallel(final RenderedImage source) {
        switch (executionMode) {
            case PARALLEL:   return true;
            case SEQUENTIAL: return false;
            default:         return source.getClass().getName().startsWith(Modules.CLASSNAME_PREFIX);
        }
    }

    /**
     * Returns whether exceptions occurring during computation are propagated or logged.
     * If {@link ErrorAction#THROW} (the default), exceptions are wrapped in {@link ImagingOpException} and thrown.
     * If any other value, exceptions are wrapped in a {@link LogRecord}, filtered then eventually logged.
     *
     * @return whether exceptions occurring during computation are propagated or logged.
     */
    public Filter getErrorAction() {
        return errorAction;
    }

    /**
     * Sets whether exceptions occurring during computation are propagated or logged.
     * The default behavior is to wrap exceptions in {@link ImagingOpException} and throw them.
     * If this property is set to {@link ErrorAction#LOG} or any other value, then exceptions will
     * be wrapped in {@link LogRecord} instead, in which case a partial result may be available.
     * Only one log record is created for all tiles that failed for the same operation on the same image.
     *
     * @param  action  filter to notify when an operation failed on one or more tiles,
     *                 or {@link ErrorAction#THROW} for propagating the exception.
     */
    public void setErrorAction(final Filter action) {
        ArgumentChecks.ensureNonNull("action", action);
        errorAction = action;
    }

    /**
     * Whether errors occurring during computation should be propagated instead than wrapped in a {@link LogRecord}.
     */
    private boolean failOnException() {
        return errorAction == ErrorAction.THROW;
    }

    /**
     * Where to send exceptions (wrapped in {@link LogRecord}) if an operation failed on one or more tiles.
     * Only one log record is created for all tiles that failed for the same operation on the same image.
     * This is always {@code null} if {@link #failOnException()} is {@code true}.
     */
    private Filter errorListener() {
        return (errorAction instanceof ErrorAction) ? null : errorAction;
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
        final StatisticsCalculator calculator = new StatisticsCalculator(source, parallel(source), failOnException());
        final Object property = calculator.getProperty(StatisticsCalculator.PROPERTY_NAME);
        calculator.logAndClearError(ImageProcessor.class, "statistics", errorListener());
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
            return unique(RecoloredImage.rescale(source, visibleBand, minimum, maximum));
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
                        return unique(RecoloredImage.rescale(source, visibleBand, minimum, maximum));
                    }
                }
            }
        }
        return source;
    }

    /**
     * Creates a new image which will resample the given image. The resampling operation is defined
     * by a non-linear transform from the <em>new</em> image to the specified <em>source</em> image.
     * That transform should map {@linkplain org.opengis.referencing.datum.PixelInCell#CELL_CENTER pixel centers}.
     * If that transform produces coordinates that are outside source envelope bounds, then the corresponding pixels
     * in the new image are set to {@linkplain #getFillValues() fill values}. Otherwise sample values are interpolated
     * using the method given by {@link #getInterpolation()}.
     *
     * <p>If the given source is an instance of {@link ResampledImage} or {@link AnnotatedImage},
     * then this method will use {@linkplain PlanarImage#getSources() the source} of the given source.
     * The intent is to avoid resampling a resampled image and try to work on the original data instead.</p>
     *
     * @param  bounds    domain of pixel coordinates of resampled image.
     * @param  toSource  conversion of pixel coordinates of this image to pixel coordinates of {@code source} image.
     * @param  source    the image to be resampled.
     * @return resampled image (may be {@code source}).
     */
    public RenderedImage resample(final Rectangle bounds, MathTransform toSource, RenderedImage source) {
        ArgumentChecks.ensureNonNull("bounds",   bounds);
        ArgumentChecks.ensureNonNull("toSource", toSource);
        ArgumentChecks.ensureNonNull("source",   source);
        final ColorModel  cm = source.getColorModel();
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
            resampled = new ResampledImage(bounds, toSource, source, interpolation, fillValues);
            break;
        }
        if (cm != null && !cm.equals(resampled.getColorModel())) {
            resampled = new RecoloredImage(resampled, cm);
        }
        return unique(resampled);
    }

    /**
     * Computes all tiles immediately, then return an image will all tiles ready.
     * Computations will use many threads if {@linkplain #getExecutionMode() execution mode} is parallel.
     *
     * @param  source  the image to compute immediately (may be {@code null}).
     * @return image with all tiles computed, or {@code null} if the given image was null.
     * @throws ImagingOpException if an exception occurred during {@link RenderedImage#getTile(int, int)} call.
     *         This exception wraps the original exception as its {@linkplain ImagingOpException#getCause() cause}.
     */
    public RenderedImage prefetch(final RenderedImage source) {
        if (source == null || source instanceof BufferedImage || source instanceof TiledImage) {
            return source;
        }
        final Prefetch worker = new Prefetch(source);
        if (parallel(source)) {
            worker.parallelReadFrom(source);
        } else {
            worker.readFrom(source);
        }
        return new TiledImage(source.getColorModel(), source.getWidth(), source.getHeight(),
                              source.getMinTileX(), source.getMinTileY(), worker.tiles);
    }

    /**
     * A worker for prefetching tiles in an image.
     */
    private static final class Prefetch extends TileOpExecutor {
        /** Number of tiles in a row. */
        private final int numXTiles;

        /** Image properties for converting pixel coordinates to tile indices. */
        private final long tileWidth, tileHeight, tileGridXOffset, tileGridYOffset;

        /** The tiles in a row-major fashion. */
        final Raster[] tiles;

        /** Prepares an instance for prefetching tiles from the given image. */
        Prefetch(final RenderedImage source) {
            super(source, null);
            numXTiles       = source.getNumXTiles();
            tileWidth       = source.getTileWidth();
            tileHeight      = source.getTileHeight();
            tileGridXOffset = source.getTileGridXOffset();
            tileGridYOffset = source.getTileGridYOffset();
            tiles           = new Raster[Math.multiplyExact(source.getNumYTiles(), numXTiles)];
        }

        /** Invoked in a when a tile have been computed, possibly in a background thread. */
        @Override protected void readFrom(final Raster source) {
            final long tx = Math.floorDiv(source.getMinX() - tileGridXOffset, tileWidth);
            final long ty = Math.floorDiv(source.getMinY() - tileGridYOffset, tileHeight);
            final int index = Math.toIntExact(tx + ty*numXTiles);
            synchronized (tiles) {
                if (tiles[index] != null) {
                    throw new RasterFormatException(Errors.format(
                            Errors.Keys.DuplicatedElement_1, "Tile[" + tx + ", " + ty + ']'));
                }
                tiles[index] = source;
            }
        }
    }
}
