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
package org.apache.sis.coverage.grid;

import java.util.List;
import java.util.Set;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Function;
import java.time.Instant;
import java.time.Duration;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import javax.measure.Quantity;
import org.opengis.util.FactoryException;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.RegionOfInterest;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.image.DataType;
import org.apache.sis.image.Colorizer;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.Interpolation;
import org.apache.sis.coverage.internal.shared.SampleDimensions;
import org.apache.sis.coverage.internal.shared.BandAggregateArgument;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import org.apache.sis.measure.NumberRange;


/**
 * A predefined set of operations on grid coverages.
 * After instantiation, {@code GridCoverageProcessor} can be configured for the following aspects:
 *
 * <ul class="verbose">
 *   <li>
 *     {@linkplain #setInterpolation(Interpolation) Interpolation method} to use during resampling operations.
 *   </li><li>
 *     {@linkplain #setFillValues(Number...) Fill values} to use for cells that cannot be computed.
 *   </li><li>
 *     {@linkplain #setColorizer(Colorizer) Colorization algorithm} to apply for colorizing a computed image.
 *   </li><li>
 *     {@linkplain #setPositionalAccuracyHints(Quantity...) Positional accuracy hints}
 *     for enabling the use of faster algorithm when a lower accuracy is acceptable.
 *   </li><li>
 *     {@linkplain #setOptimizations(Set) Optimizations} to enable.
 *   </li>
 * </ul>
 *
 * For each coverage operations, above properties are combined with parameters given to the operation method.
 *
 * <h2>Thread-safety</h2>
 * {@code GridCoverageProcessor} is safe for concurrent use in multi-threading environment.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.image.ImageProcessor
 *
 * @since 1.1
 */
public class GridCoverageProcessor implements Cloneable {
    /**
     * Configured {@link ImageProcessor} instances used by {@link GridCoverage}s created by processors.
     * We use this set for sharing common instances in {@link GridCoverage} instances, which is okay
     * provided that we do not modify the {@link ImageProcessor} configuration.
     */
    private static final WeakHashSet<ImageProcessor> PROCESSORS = new WeakHashSet<>(ImageProcessor.class);

    /**
     * Returns a unique instance of the given processor. Both the given and the returned processors shall not
     * be modified after this method call, because they may be shared by many {@link GridCoverage} instances.
     * It implies that the given processor shall <em>not</em> be {@link #imageProcessor}. It must be a clone.
     *
     * @param  clone  a clone of {@link #imageProcessor} for which to return a unique instance.
     * @return a unique instance of the given clone. Shall not be modified by the caller.
     */
    static ImageProcessor unique(final ImageProcessor clone) {
        return PROCESSORS.unique(clone);
    }

    /**
     * Returns a unique instance of the current state of {@link #imageProcessor}.
     * Callers shall not modify the returned object because it may be shared by many {@link GridCoverage} instances.
     */
    private ImageProcessor snapshot() {
        ImageProcessor shared = PROCESSORS.get(imageProcessor);
        if (shared == null) {
            shared = unique(imageProcessor.clone());
        }
        return shared;
    }

    /**
     * The processor to use for operations on two-dimensional slices.
     */
    protected final ImageProcessor imageProcessor;

    /**
     * The set of optimizations that are enabled.
     * By default, this set contains all enumeration values.
     *
     * @see #getOptimizations()
     * @see #setOptimizations(Set)
     *
     * @since 1.3
     */
    protected final EnumSet<Optimization> optimizations = EnumSet.allOf(Optimization.class);

    /**
     * Creates a new processor with default configuration.
     */
    public GridCoverageProcessor() {
        imageProcessor = new ImageProcessor();
    }

    /**
     * Creates a new processor initialized to the given configuration.
     *
     * @param  processor  the processor to use for operations on two-dimensional slices.
     */
    public GridCoverageProcessor(final ImageProcessor processor) {
        imageProcessor = processor.clone();
    }

    /**
     * Returns the interpolation method to use for resampling operations.
     * The default implementation delegates to the image processor.
     *
     * @return interpolation method to use in resampling operations.
     *
     * @see ImageProcessor#getInterpolation()
     */
    public Interpolation getInterpolation() {
        return imageProcessor.getInterpolation();
    }

    /**
     * Sets the interpolation method to use for resampling operations.
     * The default implementation delegates to the image processor.
     *
     * @param  method  interpolation method to use in resampling operations.
     *
     * @see ImageProcessor#setInterpolation(Interpolation)
     */
    public void setInterpolation(final Interpolation method) {
        imageProcessor.setInterpolation(method);
    }

    /**
     * Returns the values to use for pixels that cannot be computed.
     * The default implementation delegates to the image processor.
     *
     * @return fill values to use for pixels that cannot be computed, or {@code null} for the defaults.
     *
     * @see ImageProcessor#getFillValues()
     *
     * @since 1.2
     */
    public Number[] getFillValues() {
        return imageProcessor.getFillValues();
    }

    /**
     * Sets the values to use for pixels that cannot be computed.
     * The default implementation delegates to the image processor.
     *
     * @param  values  fill values to use for pixels that cannot be computed, or {@code null} for the defaults.
     *
     * @see ImageProcessor#setFillValues(Number...)
     *
     * @since 1.2
     */
    public void setFillValues(final Number... values) {
        imageProcessor.setFillValues(values);
    }

    /**
     * Returns the colorization algorithm to apply on computed images.
     * The default implementation delegates to the image processor.
     *
     * @return colorization algorithm to apply on computed image, or {@code null} for default.
     *
     * @see ImageProcessor#getColorizer()
     *
     * @since 1.4
     */
    public Colorizer getColorizer() {
        return imageProcessor.getColorizer();
    }

    /**
     * Sets the colorization algorithm to apply on computed images.
     * The colorizer is used by {@link #convert(GridCoverage, MathTransform1D[], Function) convert(…)}
     * and {@link #aggregateRanges(GridCoverage...) aggregateRanges(…)} operations among others.
     * The default implementation delegates to the image processor.
     *
     * @param colorizer colorization algorithm to apply on computed image, or {@code null} for default.
     *
     * @see ImageProcessor#setColorizer(Colorizer)
     * @see #visualize(GridCoverage, GridExtent)
     *
     * @since 1.4
     */
    public void setColorizer(final Colorizer colorizer) {
        imageProcessor.setColorizer(colorizer);
    }

    /**
     * Returns hints about the desired positional accuracy, in "real world" units or in pixel units.
     * The default implementation delegates to the image processor.
     *
     * @return desired accuracy in no particular order, or an empty array if none.
     *
     * @see ImageProcessor#getPositionalAccuracyHints()
     */
    public Quantity<?>[] getPositionalAccuracyHints() {
        return imageProcessor.getPositionalAccuracyHints();
    }

    /**
     * Sets hints about desired positional accuracy, in "real world" units or in pixel units.
     * The default implementation delegates to the image processor.
     *
     * @param  hints  desired accuracy in no particular order, or a {@code null} array if none.
     *                Null elements in the array are ignored.
     *
     * @see ImageProcessor#setPositionalAccuracyHints(Quantity...)
     */
    public void setPositionalAccuracyHints(final Quantity<?>... hints) {
        imageProcessor.setPositionalAccuracyHints(hints);
    }

    /**
     * Types of changes that a coverage processor can do for executing an operation more efficiently.
     * For example, the processor may, in some cases, replace an operation by a more efficient one.
     * Those optimizations should not change significantly the sample values at any given location,
     * but may change other aspects (in a compatible way) such as the {@link GridCoverage} subclass
     * returned or the size of the underlying rendered images.
     *
     * <p>By default the {@link #REPLACE_OPERATION} and {@link #REPLACE_SOURCE} optimizations are enabled.
     * Users may want to disable some optimizations for example in order to get more predictable results.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.3
     *
     * @see #getOptimizations()
     * @see #setOptimizations(Set)
     *
     * @since 1.3
     */
    public enum Optimization {
        /**
         * Allows the replacement of an operation by a more efficient one.
         * This optimization is enabled by default.
         *
         * <h4>Example</h4>
         * If the {@link #resample(GridCoverage, GridGeometry) resample(…)} method is invoked with parameter values
         * that cause the resampling to be a translation of the grid by an integer number of cells, then by default
         * {@link GridCoverageProcessor} will use the {@link #shiftGrid(GridCoverage, long[]) shiftGrid(…)}
         * algorithm instead. This option can be cleared for forcing a full resampling operation in all cases.
         */
        REPLACE_OPERATION,

        /**
         * Allows the replacement of source parameter by a more fundamental source.
         * This replacement may change the results, but usually with better accuracy.
         * This optimization is enabled by default.
         *
         * <h4>Example</h4>
         * If the {@link #resample(GridCoverage, GridGeometry) resample(…)} method is invoked with a source
         * grid coverage which is itself the result of a previous resampling, then instead of resampling an
         * already resampled coverage, by default {@link GridCoverageProcessor} will resample the original
         * coverage. This option can be cleared for disabling that replacement.
         */
        REPLACE_SOURCE
    }

    /**
     * Returns the set of optimizations that are enabled.
     * By default, the returned set contains all optimizations.
     *
     * <p>The returned set is a copy. Changes in this set will not affect the state of this processor.</p>
     *
     * @return copy of the set of optimizations that are enabled.
     * @since 1.3
     */
    public synchronized Set<Optimization> getOptimizations() {
        return optimizations.clone();
    }

    /**
     * Specifies the set of optimizations to enable.
     * All optimizations not in the given set will be disabled.
     *
     * @param enabled  set of optimizations to enable.
     * @since 1.3
     */
    public synchronized void setOptimizations(final Set<Optimization> enabled) {
        ArgumentChecks.ensureNonNull("enabled", enabled);
        optimizations.clear();
        optimizations.addAll(enabled);
    }

    /**
     * Returns information about conversion from pixel coordinates to "real world" coordinates.
     * This is taken from {@link PlanarImage#GRID_GEOMETRY_KEY} if available, or computed otherwise.
     *
     * @param  image     the image from which to get the conversion.
     * @param  coverage  the coverage to use as a fallback if the information is not provided with the image.
     * @return information about conversion from pixel coordinates to "real world" coordinates.
     */
    private static GridGeometry getImageGeometry(final RenderedImage image, final GridCoverage coverage) {
        final Object value = image.getProperty(PlanarImage.GRID_GEOMETRY_KEY);
        if (value instanceof GridGeometry) {
            return (GridGeometry) value;
        }
        return new ImageRenderer(coverage, null).getImageGeometry(GridCoverage2D.BIDIMENSIONAL);
    }

    /**
     * Applies a mask defined by a region of interest (ROI). If {@code maskInside} is {@code true},
     * then all pixels inside the given ROI are set to the {@linkplain #getFillValues() fill values}.
     * If {@code maskInside} is {@code false}, then the mask is reversed:
     * the pixels set to fill values are the ones outside the ROI.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getFillValues() Fill values} values to assign to pixels inside/outside the region of interest.</li>
     * </ul>
     *
     * @param  source      the coverage on which to apply a mask.
     * @param  mask        region (in arbitrary CRS) of the mask.
     * @param  maskInside  {@code true} for masking pixels inside the shape, or {@code false} for masking outside.
     * @return a coverage with mask applied.
     * @throws TransformException if ROI coordinates cannot be transformed to grid coordinates.
     *
     * @see ImageProcessor#mask(RenderedImage, Shape, boolean)
     *
     * @since 1.2
     */
    public GridCoverage mask(final GridCoverage source, final RegionOfInterest mask, final boolean maskInside)
            throws TransformException
    {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("mask", mask);
        RenderedImage data = source.render(null);
        final Shape roi = mask.toShape2D(getImageGeometry(data, source));
        data = imageProcessor.mask(data, roi, maskInside);
        return new GridCoverage2D(source, data);
    }

    /**
     * Returns a coverage with sample values converted by the given functions.
     * The number of sample dimensions in the returned coverage is the length of the {@code converters} array,
     * which must be greater than 0 and not greater than the number of sample dimensions in the source coverage.
     * If the {@code converters} array length is less than the number of source sample dimensions,
     * then all sample dimensions at index ≥ {@code converters.length} will be ignored.
     *
     * <h4>Sample dimensions customization</h4>
     * By default, this method creates new sample dimensions with the same names and categories than in the
     * previous coverage, but with {@linkplain org.apache.sis.coverage.Category#getSampleRange() sample ranges}
     * converted using the given converters and with {@linkplain SampleDimension#getUnits() units of measurement}
     * omitted. This behavior can be modified by specifying a non-null {@code sampleDimensionModifier} function.
     * If non-null, that function will be invoked with, as input, a pre-configured sample dimension builder.
     * The {@code sampleDimensionModifier} function can {@linkplain SampleDimension.Builder#setName(CharSequence)
     * change the sample dimension name} or {@linkplain SampleDimension.Builder#categories() rebuild the categories}.
     *
     * <h4>Result relationship with source</h4>
     * If the source coverage is backed by a {@link java.awt.image.WritableRenderedImage},
     * then changes in the source coverage are reflected in the returned coverage and conversely.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getColorizer() Colorizer} for customizing the rendered image color model.</li>
     * </ul>
     *
     * @param  source      the coverage for which to convert sample values.
     * @param  converters  the transfer functions to apply on each sample dimension of the source coverage.
     * @param  sampleDimensionModifier  a callback for modifying the {@link SampleDimension.Builder} default
     *         configuration for each sample dimension of the target coverage, or {@code null} if none.
     * @return the coverage which computes converted values from the given source.
     *
     * @see ImageProcessor#convert(RenderedImage, NumberRange<?>[], MathTransform1D[], DataType)
     *
     * @since 1.3
     */
    public GridCoverage convert(final GridCoverage source, MathTransform1D[] converters,
            Function<SampleDimension.Builder, SampleDimension> sampleDimensionModifier)
    {
        ArgumentChecks.ensureNonNull("source",     source);
        ArgumentChecks.ensureNonNull("converters", converters);
        final List<SampleDimension> sourceBands = source.getSampleDimensions();
        ArgumentChecks.ensureCountBetween("converters", true, 1, sourceBands.size(), converters.length);
        final SampleDimension[] targetBands = new SampleDimension[converters.length];
        final SampleDimension.Builder builder = new SampleDimension.Builder();
        if (sampleDimensionModifier == null) {
            sampleDimensionModifier = SampleDimension.Builder::build;
        }
        for (int i=0; i < converters.length; i++) {
            final MathTransform1D converter = converters[i];
            ArgumentChecks.ensureNonNullElement("converters", i, converter);
            final SampleDimension band = sourceBands.get(i);
            band.getBackground().ifPresent(builder::setBackground);
            band.getCategories().forEach((category) -> {
                if (category.isQuantitative()) {
                    // Unit is assumed different as a result of conversion.
                    builder.addQuantitative(category.getName(), category.getSampleRange(), converter, null);
                } else {
                    builder.addQualitative(category.getName(), category.getSampleRange());
                }
            });
            targetBands[i] = sampleDimensionModifier.apply(builder.setName(band.getName())).forConvertedValues(true);
            builder.clear();
        }
        return new ConvertedGridCoverage(source, UnmodifiableArrayList.wrap(targetBands),
                                         converters, true, snapshot(), true);
    }

    /**
     * Translates grid coordinates by the given number of cells without changing "real world" coordinates.
     * The translated grid has the same {@linkplain GridExtent#getSize(int) size} than the source,
     * i.e. both low and high grid coordinates are displaced by the same number of cells.
     * The "grid to CRS" transforms are adjusted accordingly in order to map to the same
     * "real world" coordinates.
     *
     * <h4>Number of arguments</h4>
     * The {@code translation} array length should be equal to the number of dimensions in the source coverage.
     * If the array is shorter, missing values default to 0 (i.e. no translation in unspecified dimensions).
     * If the array is longer, extraneous values are ignored.
     *
     * <h4>Optimizations</h4>
     * The following optimizations are applied by default and can be disabled if desired:
     * <ul>
     *   <li>{@link Optimization#REPLACE_SOURCE} for merging many calls
     *       of this {@code translate(…)} method into a single translation.</li>
     * </ul>
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>(none)</li>
     * </ul>
     *
     * @param  source       the grid coverage to translate.
     * @param  translation  translation to apply on each grid axis in order.
     * @return a grid coverage whose grid coordinates (both low and high ones) and
     *         the "grid to CRS" transforms have been translated by given amounts.
     *         If the given translation is a no-op (no value or only 0 ones), then the source is returned as is.
     * @throws ArithmeticException if the translation results in coordinates that overflow 64-bits integer.
     *
     * @see GridExtent#translate(long...)
     * @see GridGeometry#shiftGrid(long...)
     *
     * @since 1.3
     */
    public GridCoverage shiftGrid(final GridCoverage source, long... translation) {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("translation", translation);
        final boolean allowSourceReplacement;
        synchronized (this) {
            allowSourceReplacement = optimizations.contains(Optimization.REPLACE_SOURCE);
        }
        return TranslatedGridCoverage.create(source, null, translation, allowSourceReplacement);
    }

    /**
     * Returns the intersection of the given coverage with the given extent.
     * The extent shall have the same number of dimensions than the coverage.
     * The "grid to <abbr>CRS</abbr>" transform is unchanged.
     *
     * <p>This method is useful for taking a slice of a multi-dimensional grid.
     * Having a slice allows to invoke {@link GridCoverage#render(GridExtent)}
     * with a null argument value.</p>
     *
     * @param  source  the grid coverage to clip.
     * @param  clip    the clip to apply in units of source grid coordinates.
     * @return a coverage with grid coordinates contained inside the given clip.
     * @throws IncompleteGridGeometryException if the given coverage has no grid extent.
     * @throws DisjointExtentException if the given extent does not intersect the given coverage.
     * @throws IllegalArgumentException if axes of the given extent are inconsistent with the axes of the grid of the given coverage.
     *
     * @since 1.5
     */
    public GridCoverage clip(final GridCoverage source, final GridExtent clip) {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("clip",   clip);
        final boolean allowSourceReplacement;
        synchronized (this) {
            allowSourceReplacement = optimizations.contains(Optimization.REPLACE_SOURCE);
        }
        return ClippedGridCoverage.create(source, clip, allowSourceReplacement);
    }

    /**
     * Creates a new coverage with a different grid extent, resolution or coordinate reference system.
     * The desired properties are specified by the {@link GridGeometry} argument, which may be incomplete.
     * The missing grid geometry components are completed as below:
     *
     * <table class="sis">
     *   <caption>Default values for undefined grid geometry components</caption>
     *   <tr>
     *     <th>Component</th>
     *     <th>Default value</th>
     *   </tr><tr>
     *     <td>{@linkplain GridGeometry#getExtent() Grid extent}</td>
     *     <td>A default size preserving resolution at source
     *       {@linkplain GridExtent#getPointOfInterest(PixelInCell) point of interest}.</td>
     *   </tr><tr>
     *     <td>{@linkplain GridGeometry#getGridToCRS Grid to CRS transform}</td>
     *     <td>Whatever it takes for fitting data inside the supplied extent.</td>
     *   </tr><tr>
     *     <td>{@linkplain GridGeometry#getCoordinateReferenceSystem() Coordinate reference system}</td>
     *     <td>Same as source coverage.</td>
     *   </tr>
     * </table>
     *
     * The interpolation method can be specified by {@link #setInterpolation(Interpolation)}.
     * If the grid coverage values are themselves interpolated, this method tries to use the
     * original data. The intent is to avoid adding interpolations on top of other interpolations.
     *
     * <h4>Optimizations</h4>
     * The following optimizations are applied by default and can be disabled if desired:
     * <ul>
     *   <li>{@link Optimization#REPLACE_SOURCE} for merging many calls of {@code resample(…)}
     *       or {@code translate(…)} method into a single resampling.</li>
     *   <li>{@link Optimization#REPLACE_OPERATION} for replacing {@code resample(…)} operation
     *       by {@code translate(…)} when possible.</li>
     * </ul>
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getInterpolation() Interpolation method} (nearest neighbor, bilinear, <i>etc</i>).</li>
     *   <li>{@linkplain #getFillValues() Fill values} for pixels outside source image.</li>
     *   <li>{@linkplain #getPositionalAccuracyHints() Positional accuracy hints}
     *       for enabling faster resampling at the cost of lower precision.</li>
     * </ul>
     *
     * @param  source  the grid coverage to resample.
     * @param  target  the desired geometry of returned grid coverage. May be incomplete.
     * @return a grid coverage with the characteristics specified in the given grid geometry.
     * @throws IncompleteGridGeometryException if the source grid geometry is missing an information.
     *         It may be the source CRS, the source extent, <i>etc.</i> depending on context.
     * @throws TransformException if some coordinates cannot be transformed to the specified target.
     *
     * @see ImageProcessor#resample(RenderedImage, Rectangle, MathTransform)
     */
    public GridCoverage resample(GridCoverage source, final GridGeometry target) throws TransformException {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        final boolean allowSourceReplacement, allowOperationReplacement;
        synchronized (this) {
            allowSourceReplacement    = optimizations.contains(Optimization.REPLACE_SOURCE);
            allowOperationReplacement = optimizations.contains(Optimization.REPLACE_OPERATION);
        }
        final boolean isConverted = source == source.forConvertedValues(true);
        /*
         * If the source coverage is already the result of a previous "resample" or "translate" operation,
         * use the original data in order to avoid interpolating values that are already interpolated.
         */
        for (;;) {
            if (ResampledGridCoverage.equivalent(source.getGridGeometry(), target)) {
                return source;
            } else if (allowSourceReplacement && source instanceof DerivedGridCoverage) {
                final DerivedGridCoverage derived = (DerivedGridCoverage) source;
                if (derived.isNotRepleacable()) break;
                source = derived.source;
            } else {
                break;
            }
        }
        /*
         * The projection are usually applied on floating-point values, in order
         * to gets maximal precision and to handle correctly the special case of
         * NaN values. However, we can apply the projection on integer values if
         * the interpolation type is "nearest neighbor" since this is not really
         * an interpolation.
         */
        if (imageProcessor.getInterpolation() != Interpolation.NEAREST) {
            source = source.forConvertedValues(true);
        }
        final GridCoverage resampled;
        try {
            // `ResampledGridCoverage` will create itself a clone of `imageProcessor`.
            resampled = ResampledGridCoverage.create(source, target, imageProcessor, allowOperationReplacement);
        } catch (IllegalGridGeometryException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof TransformException) {
                throw (TransformException) cause;
            } else {
                throw e;
            }
        } catch (FactoryException e) {
            throw new TransformException(e.getMessage(), e);
        }
        return resampled.forConvertedValues(isConverted);
    }

    /**
     * Creates a new coverage with a different coordinate reference system.
     * The grid extent and "grid to CRS" transform are determined automatically
     * with default values preserving the resolution of source coverage at its
     * {@linkplain GridExtent#getPointOfInterest(PixelInCell) point of interest}.
     *
     * <p>See {@link #resample(GridCoverage, GridGeometry)} for more information
     * about interpolation and allowed optimizations.</p>
     *
     * @param  source  the grid coverage to resample.
     * @param  target  the desired coordinate reference system.
     * @return a grid coverage with the given coordinate reference system.
     * @throws IncompleteGridGeometryException if the source grid geometry is missing an information.
     * @throws TransformException if some coordinates cannot be transformed to the specified target.
     *
     * @since 1.3
     */
    public GridCoverage resample(final GridCoverage source, final CoordinateReferenceSystem target) throws TransformException {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        return resample(source, new GridGeometry(null, PixelInCell.CELL_CENTER, null, target));
    }

    /**
     * Appends the specified grid dimensions after the dimensions of the given source coverage.
     * This method is typically invoked for adding a vertical or temporal axis to a two-dimensional coverage.
     * The grid extent must have a size of one cell in all the specified additional dimensions.
     *
     * @param  source    the source on which to append dimensions.
     * @param  dimToAdd  the dimensions to append. The grid extent size must be 1 cell in all dimensions.
     * @return a coverage with the specified dimensions added.
     * @throws IllegalGridGeometryException if a dimension has more than one grid cell, or concatenation
     *         would result in duplicated {@linkplain GridExtent#getAxisType(int) grid axis types},
     *         or the compound CRS cannot be created.
     *
     * @since 1.5
     */
    public GridCoverage appendDimensions(final GridCoverage source, final GridGeometry dimToAdd) {
        ArgumentChecks.ensureNonNull("source",   source);
        ArgumentChecks.ensureNonNull("dimToAdd", dimToAdd);
        try {
            return DimensionAppender.create(source, dimToAdd);
        } catch (IllegalGridGeometryException e) {
            throw e;
        } catch (FactoryException | IllegalArgumentException e) {
            throw new IllegalGridGeometryException(e.getMessage(), e);
        }
    }

    /**
     * Appends a single grid dimension after the dimensions of the given source coverage.
     * This method is typically invoked for adding a vertical axis to a two-dimensional coverage.
     * The default implementation delegates to {@link #appendDimensions(GridCoverage, GridGeometry)}.
     *
     * @param  source  the source on which to append a dimension.
     * @param  lower   lower coordinate value of the slice, in units of the CRS.
     * @param  span    size of the slice, in units of the CRS.
     * @param  crs     one-dimensional coordinate reference system of the slice, or {@code null} if unknown.
     * @return a coverage with the specified dimension added.
     * @throws IllegalGridGeometryException if the compound CRS or compound extent cannot be created.
     *
     * @since 1.5
     */
    public GridCoverage appendDimension(final GridCoverage source, double lower, final double span, final SingleCRS crs) {
        /*
         * Choose a cell index such as the translation term in the matrix will be as close as possible to zero.
         * Reducing the magnitude of additions with IEEE 754 arithmetic can help to reduce rounding errors.
         * It also has the desirable side-effect to increase the chances that slices share the same
         * "grid to CRS" transform.
         */
        final long index = Numerics.roundAndClamp(lower / span);
        final long[] indices = new long[] {index};
        final GridExtent extent = new GridExtent(GridExtent.typeFromAxes(crs, 1), indices, indices, true);
        final MathTransform gridToCRS = MathTransforms.linear(span, Math.fma(index, -span, lower));
        return appendDimensions(source, new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, crs));
    }

    /**
     * Appends a temporal grid dimension after the dimensions of the given source coverage.
     * The default implementation delegates to {@link #appendDimensions(GridCoverage, GridGeometry)}.
     *
     * @param  source  the source on which to append a temporal dimension.
     * @param  lower   start time of the slice.
     * @param  span    duration of the slice.
     * @return a coverage with the specified temporal dimension added.
     * @throws IllegalGridGeometryException if the compound CRS or compound extent cannot be created.
     *
     * @since 1.5
     */
    public GridCoverage appendDimension(final GridCoverage source, final Instant lower, final Duration span) {
        final DefaultTemporalCRS crs = DefaultTemporalCRS.castOrCopy(CommonCRS.Temporal.TRUNCATED_JULIAN.crs());
        double scale  = crs.toValue(span);
        double offset = crs.toValue(lower);
        long   index  = Numerics.roundAndClamp(offset / scale);             // See comment in above method.
        offset = crs.toValue(lower.minus(span.multipliedBy(index)));
        final GridExtent extent = new GridExtent(DimensionNameType.TIME, index, index, true);
        final MathTransform gridToCRS = MathTransforms.linear(scale, offset);
        return appendDimensions(source, new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, crs));
    }

    /**
     * Automatically reduces a grid coverage dimensionality by removing all grid axes with an extent size of 1.
     * Axes in the reduced grid coverage will be in the same order as in the source coverage.
     *
     * @param  source  the coverage to reduce to a lower number of dimensions.
     * @return the reduced grid coverage, or {@code source} if no grid dimensions can be removed.
     *
     * @see DimensionalityReduction#reduce(GridGeometry)
     *
     * @since 1.4
     */
    public GridCoverage reduceDimensionality(final GridCoverage source) {
        return DimensionalityReduction.reduce(source.getGridGeometry()).apply(source);
    }

    /**
     * Creates a coverage trimmed from the specified grid dimensions.
     * This is a <i>dimensionality reduction</i> operation applied to the coverage domain.
     * The dimensions to remove are specified as indices of <em>grid extent</em> axes.
     * It may be the same indices as the indices of the CRS axes which will be removed,
     * but not necessarily.
     *
     * <h4>Constraints</h4>
     * If the source coverage contains dimensions that are not
     * {@linkplain org.apache.sis.referencing.operation.transform.TransformSeparator separable}
     * and if only a subset of those dimensions are specified for removal,
     * then this method will throw an {@link IllegalGridGeometryException}.
     *
     * <p>For each dimension that is removed,
     * the {@linkplain GridExtent#getSize(int) size} of the grid extent must be 1 cell.
     * If this condition does not hold, then this method will throw a {@link SubspaceNotSpecifiedException}.
     * If desired, this restriction can be relaxed by direct use of {@link DimensionalityReduction} as below,
     * where (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>) are grid coordinates of a point
     * in the desired slice:</p>
     *
     * {@snippet lang="java" :
     *     var reduction = DimensionalityReduction.remove(source.getGridGeometry(), gridAxesToPass);
     *     reduction = reduction.withSlicePoint(x, y, z, t);
     *     GridCoverage output = reduction.apply(source);
     *     }
     *
     * Alternatively the {@code withSlicePoint(…)} call can be omitted if the caller knows that the source
     * coverage can handle {@linkplain DimensionalityReduction#reverse(GridExtent) ambiguous grid extents}.
     *
     * @param  source            the coverage to reduce to a lower number of dimensions.
     * @param  gridAxesToRemove  indices of each grid dimension to strip from result. Duplicated values are ignored.
     * @return the reduced grid coverage, or {@code source} if no grid dimensions was specified.
     * @throws IndexOutOfBoundsException if a grid axis index is out of bounds.
     * @throws SubspaceNotSpecifiedException if at least one removed dimension has a grid extent size larger than 1 cell.
     * @throws IllegalGridGeometryException if the dimensions to keep cannot be separated from the dimensions to omit.
     *
     * @see DimensionalityReduction#remove(GridGeometry, int...)
     *
     * @since 1.4
     */
    public GridCoverage removeGridDimensions(final GridCoverage source, final int... gridAxesToRemove) {
        var reduction = DimensionalityReduction.remove(source.getGridGeometry(), gridAxesToRemove);
        reduction.ensureIsSlice();
        return reduction.apply(source);
    }

    /**
     * Creates a coverage containing only the specified grid dimensions.
     * This is a <i>dimensionality reduction</i> operation applied to the coverage domain.
     * The dimensions to keep are specified as indices of <em>grid extent</em> axes.
     * It may be the same indices as the indices of the CRS axes which will pass through,
     * but not necessarily.
     *
     * <p>The axis order in the returned coverage is always the same as in the given {@code source} coverage,
     * whatever the order in which axes are specified as input in the {@code gridAxesToPass} array.
     * Duplicated values in the array are also ignored.</p>
     *
     * <h4>Constraints</h4>
     * If the source coverage contains dimensions that are not
     * {@linkplain org.apache.sis.referencing.operation.transform.TransformSeparator separable}
     * and if only a subset of those dimensions are selected in the {@code gridAxesToPass} array,
     * then this method will throw an {@link IllegalGridGeometryException}.
     *
     * <p>For each dimension that is not passed to the output grid coverage,
     * the {@linkplain GridExtent#getSize(int) size} of the grid extent must be 1 cell.
     * If this condition does not hold, then this method will throw a {@link SubspaceNotSpecifiedException}.
     * If desired, this restriction can be relaxed by direct use of {@link DimensionalityReduction} as below,
     * where (<var>x</var>, <var>y</var>, <var>z</var>, <var>t</var>) are grid coordinates of a point
     * in the desired slice:</p>
     *
     * {@snippet lang="java" :
     *     var reduction = DimensionalityReduction.select(source.getGridGeometry(), gridAxesToPass);
     *     reduction = reduction.withSlicePoint(x, y, z, t);
     *     GridCoverage output = reduction.apply(source);
     *     }
     *
     * Alternatively the {@code withSlicePoint(…)} call can be omitted if the caller knows that the source
     * coverage can handle {@linkplain DimensionalityReduction#reverse(GridExtent) ambiguous grid extents}.
     *
     * @param  source          the coverage to reduce to a lower number of dimensions.
     * @param  gridAxesToPass  indices of each grid dimension to maintain in result. Order and duplicated values are ignored.
     * @return the reduced grid coverage, or {@code source} if all grid dimensions where specified.
     * @throws IndexOutOfBoundsException if a grid axis index is out of bounds.
     * @throws SubspaceNotSpecifiedException if at least one removed dimension has a grid extent size larger than 1 cell.
     * @throws IllegalGridGeometryException if the dimensions to keep cannot be separated from the dimensions to omit.
     *
     * @see DimensionalityReduction#select(GridGeometry, int...)
     *
     * @since 1.4
     */
    public GridCoverage selectGridDimensions(final GridCoverage source, final int... gridAxesToPass) {
        var reduction = DimensionalityReduction.select(source.getGridGeometry(), gridAxesToPass);
        reduction.ensureIsSlice();
        return reduction.apply(source);
    }

    /**
     * Selects a subset of sample dimensions (bands) in the given coverage.
     * This method can also be used for changing sample dimension order or
     * for repeating the same sample dimension from the source coverage.
     * If the specified {@code bands} indices select all sample dimensions
     * in the same order, then {@code source} is returned directly.
     *
     * @param  source  the coverage in which to select sample dimensions.
     * @param  bands   indices of sample dimensions to retain.
     * @return coverage width selected sample dimensions.
     * @throws IllegalArgumentException if a sample dimension index is invalid.
     *
     * @see ImageProcessor#selectBands(RenderedImage, int...)
     *
     * @since 1.4
     */
    public GridCoverage selectSampleDimensions(final GridCoverage source, final int... bands) {
        ArgumentChecks.ensureNonNull("source", source);
        return aggregateRanges(new GridCoverage[] {source}, new int[][] {bands});
    }

    /**
     * Aggregates in a single coverage the ranges of all specified coverages, in order.
     * The {@linkplain GridCoverage#getSampleDimensions() list of sample dimensions} of
     * the aggregated coverage will be the concatenation of the lists from all sources.
     *
     * <p>This convenience method delegates to {@link #aggregateRanges(GridCoverage[], int[][])}.
     * See that method for more information on restrictions.</p>
     *
     * @param  sources  coverages whose ranges shall be aggregated, in order. At least one coverage must be provided.
     * @return the aggregated coverage, or {@code sources[0]} returned directly if only one coverage was supplied.
     * @throws IllegalGridGeometryException if a grid geometry is not compatible with the others.
     *
     * @see #aggregateRanges(GridCoverage[], int[][])
     * @see ImageProcessor#aggregateBands(RenderedImage...)
     *
     * @since 1.4
     */
    public GridCoverage aggregateRanges(final GridCoverage... sources) {
        return aggregateRanges(sources, (int[][]) null);
    }

    /**
     * Aggregates in a single coverage the specified bands of a sequence of source coverages, in order.
     * This method performs the same work as {@link #aggregateRanges(GridCoverage...)},
     * but with the possibility to specify the sample dimensions to retain in each source coverage.
     * The {@code bandsPerSource} argument specifies the sample dimensions to keep, in order.
     * That array can be {@code null} for selecting all sample dimensions in all source coverages,
     * or may contain {@code null} elements for selecting all sample dimensions of the corresponding coverage.
     * An empty array element (i.e. zero sample dimension to select) discards the corresponding source coverage.
     *
     * <h4>Restrictions</h4>
     * <ul>
     *   <li>All coverage shall use the same CRS.</li>
     *   <li>All coverage shall use the same <i>grid to CRS</i> transform except for translation terms.</li>
     *   <li>Translation terms in <i>grid to CRS</i> can differ only by an integer number of grid cells.</li>
     *   <li>The intersection of the domain of all coverages shall be non-empty.</li>
     *   <li>All coverages shall use the same data type in their rendered image.</li>
     * </ul>
     *
     * Some of those restrictions may be relaxed in future Apache SIS versions.
     *
     * @param  sources  coverages whose bands shall be aggregated, in order. At least one coverage must be provided.
     * @param  bandsPerSource  bands to use for each source coverage, in order. May contain {@code null} elements.
     * @return the aggregated coverage, or one of the sources if it can be used directly.
     * @throws IllegalGridGeometryException if a grid geometry is not compatible with the others.
     * @throws IllegalArgumentException if some band indices are duplicated or outside their range of validity.
     *
     * @see ImageProcessor#aggregateBands(RenderedImage[], int[][])
     *
     * @since 1.4
     */
    public GridCoverage aggregateRanges(GridCoverage[] sources, int[][] bandsPerSource) {
        final var aggregate = new BandAggregateArgument<>(sources, bandsPerSource);
        aggregate.unwrap(BandAggregateGridCoverage::unwrap);
        aggregate.completeAndValidate(GridCoverage::getSampleDimensions);
        aggregate.mergeConsecutiveSources();
        if (aggregate.isIdentity()) {
            return aggregate.sources()[0];
        }
        return new BandAggregateGridCoverage(aggregate, snapshot());
    }

    /**
     * Renders the given grid coverage as an image suitable for displaying purpose.
     * The resulting image is for visualization only and should not be used for computational purposes.
     * There is no guarantee about the number of bands in returned image or about which formula is used
     * for converting floating point values to integer values.
     *
     * <h4>How to specify colors</h4>
     * The image colors can be controlled by the {@link Colorizer} set on this coverage processor.
     * The recommended way is to associate colors to {@linkplain Category#getName() category names},
     * {@linkplain org.apache.sis.measure.MeasurementRange#unit() units of measurement}
     * or other category properties. Example:
     *
     * {@snippet lang="java" :
     *     Map<String,Color[]> colors = Map.of(
     *         "Temperature", new Color[] {Color.BLUE, Color.MAGENTA, Color.RED},
     *         "Wind speed",  new Color[] {Color.GREEN, Color.CYAN, Color.BLUE});
     *
     *     processor.setColorizer(Colorizer.forCategories((category) ->
     *         colors.get(category.getName().toString(Locale.ENGLISH))));
     *
     *     RenderedImage visualization = processor.visualize(source, slice);
     *     }
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getColorizer() Colorizer} for customizing the rendered image color model.</li>
     * </ul>
     *
     * @param  source  the grid coverage to visualize.
     * @param  slice   the slice and extent to render, or {@code null} for the whole coverage.
     * @return rendered image for visualization purposes only.
     * @throws IllegalArgumentException if the given extent does not have the same number of dimensions
     *         than the specified coverage or does not intersect.
     *
     * @see ImageProcessor#visualize(RenderedImage)
     *
     * @since 1.4
     */
    public RenderedImage visualize(final GridCoverage source, final GridExtent slice) {
        ArgumentChecks.ensureNonNull("source", source);
        final List<SampleDimension> ranges = source.getSampleDimensions();
        final RenderedImage image = source.render(slice);
        try {
            SampleDimensions.IMAGE_PROCESSOR_ARGUMENT.set(ranges);
            return imageProcessor.visualize(image);
        } finally {
            SampleDimensions.IMAGE_PROCESSOR_ARGUMENT.remove();
        }
    }

    /**
     * Invoked when an ignorable exception occurred.
     *
     * @param  caller  the method where the exception occurred.
     * @param  ex      the ignorable exception.
     */
    static void recoverableException(final String caller, final Exception ex) {
        Logging.recoverableException(GridExtent.LOGGER, GridCoverageProcessor.class, caller, ex);
    }

    /**
     * Returns {@code true} if the given object is a coverage processor
     * of the same class with the same configuration.
     *
     * @param  object  the other object to compare with this processor.
     * @return whether the other object is a coverage processor of the same class with the same configuration.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final var other = (GridCoverageProcessor) object;
            if (imageProcessor.equals(other.imageProcessor)) {
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final EnumSet<?> optimizations;
                synchronized (this) {
                    // Clone for allowing comparison outside the synchronized block.
                    optimizations = (EnumSet<?>) this.optimizations.clone();
                }
                synchronized (other) {
                    return optimizations.equals(other.optimizations);
                }
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for this coverage processor based on its current configuration.
     *
     * @return a hash code value for this processor.
     */
    @Override
    public synchronized int hashCode() {
        return Objects.hash(getClass(), imageProcessor, optimizations);
    }

    /**
     * Returns a coverage processor with the same configuration as this processor.
     *
     * @return a clone of this coverage processor.
     */
    @Override
    public GridCoverageProcessor clone() {
        try {
            final var clone = (GridCoverageProcessor) super.clone();
            final Field f = GridCoverageProcessor.class.getDeclaredField("imageProcessor");
            f.setAccessible(true);      // Caller sensitive: must be invoked in same module.
            f.set(clone, imageProcessor.clone());
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        } catch (ReflectiveOperationException e) {
            throw (InaccessibleObjectException) new InaccessibleObjectException().initCause(e);
        }
    }
}
