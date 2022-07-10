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
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import javax.measure.Quantity;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.RegionOfInterest;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.image.DataType;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.Interpolation;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.FinalFieldSetter;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.measure.NumberRange;

import static java.util.logging.Logger.getLogger;


/**
 * A predefined set of operations on grid coverages as convenience methods.
 *
 * <h2>Thread-safety</h2>
 * {@code GridCoverageProcessor} is safe for concurrent use in multi-threading environment.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see org.apache.sis.image.ImageProcessor
 *
 * @since 1.1
 * @module
 */
public class GridCoverageProcessor implements Cloneable {
    /**
     * Configured {@link ImageProcessor} instances used by {@link GridCoverage}s created by processors.
     * We use this set for sharing common instances in {@link GridCoverage} instances, which is okay
     * provided that we do not modify the {@link ImageProcessor} configuration.
     */
    private static final WeakHashSet<ImageProcessor> PROCESSORS = new WeakHashSet<>(ImageProcessor.class);

    /**
     * Returns an unique instance of the given processor. Both the given and the returned processors
     * shall be unmodified, because they may be shared by many {@link GridCoverage} instances.
     */
    static ImageProcessor unique(final ImageProcessor image) {
        return PROCESSORS.unique(image);
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
        ArgumentChecks.ensureNonNull("processor", processor);
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
     * For example the processor may, in some cases, replace an operation by a more efficient one.
     * Those optimizations should not change significantly the sample values at any given location,
     * but may change other aspects (in a compatible way) such as the {@link GridCoverage} subclass
     * returned or the size of the underlying rendered images.
     *
     * <p>By default all optimizations are enabled. Users may want to disable some optimizations
     * for example in order to get more predictable results.</p>
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
         *
         * <div class="note"><b>Example:</b>
         * if the {@link #resample(GridCoverage, GridGeometry) resample(…)} method is invoked with parameter values
         * that cause the resampling to be a translation of the grid by an integer amount of cells, then by default
         * {@link GridCoverageProcessor} will use the {@link #translateGrid(GridCoverage, long...) translateGrid(…)}
         * algorithm instead. This option can be cleared for forcing a full resampling operation in all cases.</div>
         */
        REPLACE_OPERATION,

        /**
         * Allows the replacement of source parameter by a more fundamental source.
         *
         * <div class="note"><b>Example:</b>
         * if the {@link #resample(GridCoverage, GridGeometry) resample(…)} method is invoked with a source
         * grid coverage which is itself the result of a previous resampling, then instead of resampling an
         * already resampled coverage, by default {@link GridCoverageProcessor} will resample the original
         * coverage. This option can be cleared for disabling that replacement.</div>
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
     * Returns the values to use for pixels that can not be computed.
     * The default implementation delegates to the image processor.
     *
     * @return fill values to use for pixels that can not be computed, or {@code null} for the defaults.
     *
     * @see ImageProcessor#getFillValues()
     *
     * @since 1.2
     */
    public Number[] getFillValues() {
        return imageProcessor.getFillValues();
    }

    /**
     * Sets the values to use for pixels that can not be computed.
     * The default implementation delegates to the image processor.
     *
     * @param  values  fill values to use for pixels that can not be computed, or {@code null} for the defaults.
     *
     * @see ImageProcessor#setFillValues(Number...)
     *
     * @since 1.2
     */
    public void setFillValues(final Number... values) {
        imageProcessor.setFillValues(values);
    }

    /**
     * Applies a mask defined by a region of interest (ROI). If {@code maskInside} is {@code true},
     * then all pixels inside the given ROI are set to the {@linkplain #getFillValues() fill values}.
     * If {@code maskInside} is {@code false}, then the mask is reversed:
     * the pixels set to fill values are the ones outside the ROI.
     *
     * @param  source      the coverage on which to apply a mask.
     * @param  mask        region (in arbitrary CRS) of the mask.
     * @param  maskInside  {@code true} for masking pixels inside the shape, or {@code false} for masking outside.
     * @return a coverage with mask applied.
     * @throws TransformException if ROI coordinates can not be transformed to grid coordinates.
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
        final Shape roi = mask.toShape2D(source.getGridGeometry());
        RenderedImage data = source.render(null);
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
     * @param  source      the coverage for which to convert sample values.
     * @param  converters  the transfer functions to apply on each sample dimension of the source coverage.
     * @param  sampleDimensionModifier  a callback for modifying the {@link SampleDimension.Builder} default
     *         configuration for each sample dimension of the target coverage, or {@code null} if none.
     * @return the coverage which computes converted values from the given source.
     *
     * @see ImageProcessor#convert(RenderedImage, NumberRange<?>[], MathTransform1D[], DataType, ColorModel)
     *
     * @since 1.3
     */
    public GridCoverage convert(final GridCoverage source, MathTransform1D[] converters,
            Function<SampleDimension.Builder, SampleDimension> sampleDimensionModifier)
    {
        ArgumentChecks.ensureNonNull("source",     source);
        ArgumentChecks.ensureNonNull("converters", converters);
        final List<SampleDimension> sourceBands = source.getSampleDimensions();
        ArgumentChecks.ensureSizeBetween("converters", 1, sourceBands.size(), converters.length);
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
                                         converters, true, unique(imageProcessor), true);
    }

    /**
     * Returns a coverage with a grid translated by the given amount of cells compared to the source.
     * The translated grid has the same {@linkplain GridExtent#getSize(int) size} than the source,
     * i.e. both low and high grid coordinates are displaced by the same amount of cells.
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
     * @param  source       the grid coverage to translate.
     * @param  translation  translation to apply on each grid axis in order.
     * @return a grid coverage whose grid coordinates (both low and high ones) and
     *         the "grid to CRS" transforms have been translated by given amounts.
     *         If the given translation is a no-op (no value or only 0 ones), then the source is returned as is.
     * @throws ArithmeticException if the translation results in coordinates that overflow 64-bits integer.
     *
     * @see GridExtent#translate(long...)
     * @see GridGeometry#translate(long...)
     *
     * @since 1.3
     */
    public GridCoverage translateGrid(final GridCoverage source, long... translation) {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("translation", translation);
        final boolean allowSourceReplacement;
        synchronized (this) {
            allowSourceReplacement = optimizations.contains(Optimization.REPLACE_SOURCE);
        }
        return TranslatedGridCoverage.create(source, null, translation, allowSourceReplacement);
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
     * @param  source  the grid coverage to resample.
     * @param  target  the desired geometry of returned grid coverage. May be incomplete.
     * @return a grid coverage with the characteristics specified in the given grid geometry.
     * @throws IncompleteGridGeometryException if the source grid geometry is missing an information.
     *         It may be the source CRS, the source extent, <i>etc.</i> depending on context.
     * @throws TransformException if some coordinates can not be transformed to the specified target.
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
                if (derived.IsNotRepleacable()) break;
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
        if (!Interpolation.NEAREST.equals(imageProcessor.getInterpolation())) {
            source = source.forConvertedValues(true);
        }
        final GridCoverage resampled;
        try {
            resampled = ResampledGridCoverage.create(source, target, imageProcessor, allowOperationReplacement);
        } catch (IllegalGridGeometryException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof TransformException) {
                throw (TransformException) cause;
            } else {
                throw e;
            }
        } catch (FactoryException e) {
            throw new TransformException(e);
        }
        return resampled.forConvertedValues(isConverted);
    }

    /**
     * Invoked when an ignorable exception occurred.
     *
     * @param  caller  the method where the exception occurred.
     * @param  ex      the ignorable exception.
     */
    static void recoverableException(final String caller, final Exception ex) {
        Logging.recoverableException(getLogger(Modules.RASTER), GridCoverageProcessor.class, caller, ex);
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
            final GridCoverageProcessor other = (GridCoverageProcessor) object;
            return imageProcessor.equals(other.imageProcessor);
        }
        return false;
    }

    /**
     * Returns a hash code value for this coverage processor based on its current configuration.
     *
     * @return a hash code value for this processor.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getClass(), imageProcessor);
    }

    /**
     * Returns a coverage processor with the same configuration than this processor.
     *
     * @return a clone of this coverage processor.
     */
    @Override
    public GridCoverageProcessor clone() {
        try {
            final GridCoverageProcessor clone = (GridCoverageProcessor) super.clone();
            FinalFieldSetter.set(GridCoverageProcessor.class, "imageProcessor", clone, imageProcessor.clone());
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        } catch (ReflectiveOperationException e) {
            throw FinalFieldSetter.cloneFailure(e);
        }
    }
}
