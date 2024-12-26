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
import java.util.Collections;
import java.util.function.Function;
import java.util.function.DoubleUnaryOperator;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.nio.DoubleBuffer;
import javax.measure.Quantity;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.internal.CompoundTransform;
import org.apache.sis.coverage.privy.ImageUtilities;
import org.apache.sis.coverage.privy.SampleDimensions;
import org.apache.sis.coverage.privy.ColorScaleBuilder;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.privy.UnmodifiableArrayList;


/**
 * Image generated for visualization purposes only (not to be used for computation purposes).
 * This class merges {@link ResampledImage}, {@link BandedSampleConverter} and {@link RecoloredImage} operations
 * in a single operation for efficiency. This merge avoids creating intermediate tiles of {@code float} values.
 * By writing directly {@code byte} values, we save memory and CPU because
 * {@link WritableRaster#setPixel(int, int, int[])} has more efficient implementations for integers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Visualization extends ResampledImage {
    /**
     * A colorization target handled in a special way by {@link Colorizer} factory methods.
     * The fields in this class are set when the {@link Colorizer#apply(Target)} work needs
     * to be done by the caller instead. This special case is needed because the ranges and
     * categories specified by the user are relative to the source image while colorization
     * operation needs ranges and categories relative to the target image.
     */
    static final class Target extends Colorizer.Target {
        /**
         * Colors to apply on the sample value ranges, as supplied by user.
         */
        List<Map.Entry<NumberRange<?>,Color[]>> rangeColors;

        /**
         * Colors to apply on the sample dimensions, as supplied by user.
         */
        Function<Category,Color[]> categoryColors;

        /**
         * Whether the {@link Builder} has information about {@link SampleDimension} categories.
         */
        private final boolean hasCategories;

        /**
         * Creates a new target with the sample model of the image to colorize.
         *
         * @param  model          sample model of the computed image to colorize (mandatory).
         * @param  visibleBand    the band to colorize if the colorization algorithm uses only one band, or -1 if none.
         * @param  hasCategories  whether the builder has information about {@link SampleDimension} categories.
         */
        Target(final SampleModel model, final int visibleBand, final boolean hasCategories) {
            super(model, null, visibleBand);
            this.hasCategories = hasCategories;
        }

        /**
         * Returns {@code true} if {@code orElse(…)} should not try alternative colorizers.
         *
         * @return whether {@link #orElse(Colorizer)} should not try alternative.
         */
        @Override
        boolean isConsumed() {
            return (rangeColors != null) || (hasCategories && categoryColors != null);
        }
    }

    /**
     * Builds an image where all sample values are indices of colors in an {@link IndexColorModel}.
     * If the given image stores sample values as unsigned bytes or short integers, then those values
     * are used as-is (they are not copied or converted). Otherwise {@link Visualization} will convert
     * sample values to unsigned bytes in order to enable the use of {@link IndexColorModel}.
     *
     * <p>This builder accepts two kinds of input:</p>
     * <ul>
     *   <li>Non-null {@link #sampleDimensions} and {@link Target#categoryColors}.</li>
     *   <li>Non-null {@link Target#rangeColors}.</li>
     * </ul>
     *
     * The resulting image is suitable for visualization purposes but should not be used for computation purposes.
     * There is no guarantee about the number of bands in returned image and the formulas used for converting
     * floating point values to integer values.
     *
     * <h2>Resampling</h2>
     * {@link Visualization} can optionally be combined with a {@link ResampledImage} operation.
     * This can be done by providing a non-null value to the {@code toSource} argument.
     *
     * @see ImageProcessor#visualize(RenderedImage, Map)
     */
    static final class Builder {
        /** Number of bands of the image to create. */
        private static final int NUM_BANDS = 1;

        /** Band to make visible among the remaining {@value #NUM_BANDS} bands. */
        private static final int VISIBLE_BAND = 0;

        //  ┌─────────────────────────────────────┐
        //  │ Arguments given by user             │
        //  └─────────────────────────────────────┘

        /** Pixel coordinates of the visualization image, or {@code null} if same as {@link #source} image. */
        private Rectangle bounds;

        /** Image to be resampled and converted. */
        private RenderedImage source;

        /** Conversion from pixel coordinates of visualization image to pixel coordinates of {@link #source} image. */
        private MathTransform toSource;

        /** Description of {@link #source} bands, or {@code null} if none. */
        private List<SampleDimension> sampleDimensions;

        //  ┌─────────────────────────────────────┐
        //  │ Given by ImageProcesor.configure(…) │
        //  └─────────────────────────────────────┘

        /** Computer of tile size. */
        ImageLayout layout;

        /** Object to use for performing interpolations. */
        Interpolation interpolation;

        /** Provider of colors to apply for range of sample values in source image, or {@code null} if none. */
        Colorizer colorizer;

        /** Values to use for pixels in this image that cannot be mapped to pixels in source image. */
        Number[] fillValues;

        /** Values of {@value #POSITIONAL_ACCURACY_KEY} property, or {@code null} if none. */
        Quantity<?>[] positionalAccuracyHints;

        //  ┌─────────────────────────────────────┐
        //  │ Computed by `create(…)`             │
        //  └─────────────────────────────────────┘

        /** Transfer functions to apply on each band of the source image. */
        private MathTransform1D[] converters;

        /** Sample model of {@link Visualization} image. */
        private SampleModel sampleModel;

        /** Color model of {@link Visualization} image. */
        private ColorModel colorModel;

        /**
         * Creates a builder for a visualization image with colors inferred from sample dimensions.
         *
         * @param bounds    desired domain of pixel coordinates, or {@code null} if same as {@code source} image.
         * @param source    the image for which to replace the color model.
         * @param toSource  pixel coordinates conversion to {@code source} image, or {@code null} if none.
         */
        Builder(final Rectangle bounds, final RenderedImage source, final MathTransform toSource) {
            this.bounds   = bounds;
            this.source   = source;
            this.toSource = toSource;
            sampleDimensions = SampleDimensions.IMAGE_PROCESSOR_ARGUMENT.get();
            if (sampleDimensions == null) {
                Object ranges = source.getProperty(SAMPLE_DIMENSIONS_KEY);
                if (ranges instanceof SampleDimension[]) {
                    sampleDimensions = UnmodifiableArrayList.wrap((SampleDimension[]) ranges);
                }
            }
        }

        /**
         * Returns an image where all sample values are indices of colors in an {@link IndexColorModel}.
         * If the source image stores sample values as unsigned bytes or short integers, then those values
         * are used as-is (they are not copied or converted). Otherwise this operation will convert sample
         * values to unsigned bytes in order to enable the use of {@link IndexColorModel}.
         *
         * <p>The resulting image is suitable for visualization but should not be used for computational purposes.
         * There is no guarantee about the number of bands in returned image and the formulas used for converting
         * floating point values to integer values.</p>
         *
         * <h4>Resampling</h4>
         * This operation can optionally be combined with a {@link ResampledImage} operation.
         * This can be done by providing a non-null value to the {@link #toSource} field.
         *
         * @param  processor  the processor invoking this constructor.
         * @return resampled and recolored image for visualization purposes only.
         * @throws NoninvertibleTransformException if sample values in source image
         *         cannot be converted to sample values in the recolored image.
         */
        RenderedImage create(final ImageProcessor processor) throws NoninvertibleTransformException {
            final RenderedImage coloredSource = source;
            final int visibleBand = ImageUtilities.getVisibleBand(coloredSource);
            if (visibleBand < 0) {
                // This restriction may be relaxed in a future version if we implement conversion to RGB images.
                throw new IllegalArgumentException(Resources.format(Resources.Keys.OperationRequiresSingleBand));
            }
            /*
             * Skip any previous `RecoloredImage` since we will replace the `ColorModel` by a new one.
             * Discards image properties such as statistics because this image is not for computation.
             * Keep only the band to make visible in order to reduce the amount of calculation during
             * resampling and for saving memory.
             */
            if (toSource == null) {
                toSource = MathTransforms.identity(BIDIMENSIONAL);
            }
            for (;;) {
                if (source instanceof ImageAdapter) {
                    source = ((ImageAdapter) source).source;
                } else if (source instanceof ResampledImage) {
                    final ResampledImage r = (ResampledImage) source;
                    toSource = MathTransforms.concatenate(toSource, r.toSource);
                    source   = r.getSource();
                } else {
                    break;
                }
            }
            source = BandSelectImage.create(source, true, visibleBand);
            final SampleDimension visibleSD = (sampleDimensions != null && visibleBand < sampleDimensions.size())
                                            ? sampleDimensions.get(visibleBand) : null;
            /*
             * If there is no conversion of pixel coordinates, there is no need for interpolations.
             * In such case the `Visualization.computeTile(…)` implementation takes a shortcut which
             * requires the tile layout of destination image to be the same as source image.
             * Otherwise combine interpolation and value conversions in a single operation.
             */
            final boolean shortcut;
            if (bounds == null) {
                bounds = ImageUtilities.getBounds(source);
                shortcut = toSource.isIdentity();
            } else {
                shortcut = toSource.isIdentity() && ImageUtilities.getBounds(source).contains(bounds);
            }
            if (shortcut) {
                layout = ImageLayout.DEFAULT.withTileMatrix(source).allowTileSizeAdjustments(false);
            }
            /*
             * Sample values will be unconditionally converted to integers in the [0 … 255] range.
             * The sample model is a mandatory argument before we invoke user supplied colorizer,
             * which must be done before to build the color model.
             */
            final DataType dataType = DataType.forDataBufferType(ColorScaleBuilder.TYPE_COMPACT);
            sampleModel = layout.createSampleModel(dataType, bounds, NUM_BANDS);
            final Target target = new Target(sampleModel, VISIBLE_BAND, visibleSD != null);
            if (colorizer != null) {
                colorModel = colorizer.apply(target).orElse(null);
            }
            final SampleModel sourceSM = coloredSource.getSampleModel();
            final ColorModel  sourceCM = coloredSource.getColorModel();
            /*
             * Get a `ColorScaleBuilder` which will compute the `ColorModel` of destination image.
             * There is different ways to setup the builder, depending on which `Colorizer` is used.
             * In precedence order:
             *
             *    - rangeColors      : Map<NumberRange<?>,Color[]>
             *    - sampleDimensions : List<SampleDimension>
             *    - statistics
             */
            boolean initialized;
            final ColorScaleBuilder builder;
            if (target.rangeColors != null) {
                builder = new ColorScaleBuilder(target.rangeColors, sourceCM);
                initialized = true;
            } else {
                /*
                 * Ranges of sample values were not specified explicitly. Instead, we will try to infer them
                 * in various ways: sample dimensions, scaled color model, or image statistics in last resort.
                 */
                builder = new ColorScaleBuilder(target.categoryColors, sourceCM, true);
                initialized = builder.initialize(sourceSM, visibleSD);
                if (initialized) {
                    /*
                     * If we have been able to configure ColorScaleBuilder using SampleDimension, apply an adjustment
                     * based on the ScaledColorModel if it exists. Use case: image is created with an IndexColorModel
                     * determined by the SampleModel, then user enhanced contrast by a call to `stretchColorRamp(…)`.
                     * We want to preserve that contrast enhancement.
                     */
                    builder.rescaleMainRange(sourceCM);
                } else {
                    /*
                     * At this point there is no more user supplied colors (through `Colorizer`) that we can use.
                     * If we have not been able to use the SampleDimension, try to use the ColorModel or SampleModel.
                     * There is no call to `rescaleMainRange(…)` because the following code already uses the range
                     * specified by the ColorModel, if available.
                     */
                    initialized = builder.initialize(sourceCM);
                    if (!initialized) {
                        if (coloredSource instanceof RecoloredImage) {
                            final var colored = (RecoloredImage) coloredSource;
                            if (colored.minimum < colored.maximum) {    // Do not execute if values are NaN.
                                builder.initialize(colored.minimum, colored.maximum, sourceSM.getDataType());
                                initialized = true;
                            }
                        }
                        if (!initialized) {
                            initialized = builder.initialize(sourceSM, visibleBand);
                        }
                    }
                }
            }
            if (!initialized) {
                /*
                 * If none of above `ColorScaleBuilder` configurations worked, use statistics in last resort.
                 * We do that after we reduced the image to a single band in order to reduce the amount of calculation.
                 */
                final DoubleUnaryOperator[] sampleFilters = SampleDimensions.toSampleFilters(Collections.singletonList(visibleSD));
                final Statistics statistics = processor.valueOfStatistics(source, null, sampleFilters)[VISIBLE_BAND];
                builder.initialize(statistics.minimum(), statistics.maximum(), sourceSM.getDataType());
            }
            if (colorModel == null) {
                colorModel = builder.createColorModel(dataType, NUM_BANDS, VISIBLE_BAND);
            }
            converters = new MathTransform1D[] {
                builder.getSampleToIndexValues()            // Must be after `createColorModel(…)`.
            };
            if (shortcut) {
                if (converters[0].isIdentity() && colorModel.equals(sourceCM)) {
                    return coloredSource;
                }
                interpolation = Interpolation.NEAREST;
            } else {
                interpolation = combine(interpolation.toCompatible(source), converters);
                converters    = null;
            }
            return ImageProcessor.unique(new Visualization(this));
        }
    }

    /**
     * Combines the given interpolation method with the given sample conversion.
     */
    private static Interpolation combine(final Interpolation interpolation, final MathTransform1D[] converters) {
        final MathTransform converter = CompoundTransform.create(converters);
        if (converter.isIdentity()) {
            return interpolation;
        } else if (converter instanceof MathTransform1D) {
            return new InterpConvertOneBand(interpolation, (MathTransform1D) converter);
        } else {
            return new InterpConvert(interpolation, converter);
        }
    }

    /**
     * Interpolation followed by conversion from floating point values to the values to store as integers in the
     * destination image. This class is used for combining {@link ResampledImage} and {@link BandedSampleConverter}
     * in a single operation.
     */
    static class InterpConvert extends Interpolation {
        /**
         * The object to use for performing interpolations.
         *
         * @see ResampledImage#interpolation
         */
        final Interpolation interpolation;

        /**
         * Conversion from floating point values resulting from interpolations to values to store as integers
         * in the destination image. This transform shall operate on all bands in one {@code transform(…)} call.
         */
        final MathTransform converter;

        /**
         * Creates a new object combining the given interpolation with the given conversion of sample values.
         */
        InterpConvert(final Interpolation interpolation, final MathTransform converter) {
            this.interpolation = interpolation;
            this.converter = converter;
        }

        /**
         * Delegates to {@link Interpolation#getSupportSize()}.
         */
        @Override
        public final Dimension getSupportSize() {
            return interpolation.getSupportSize();
        }

        /**
         * Delegates to {@link #interpolation}, then convert sample values in all bands.
         *
         * @throws BackingStoreException if an error occurred while converting sample values.
         *         This exception should be unwrapped by {@link #computeTile(int, int, WritableRaster)}.
         */
        @Override
        public void interpolate(final DoubleBuffer source, final int numBands,
                                final double xfrac, final double yfrac,
                                final double[] writeTo, final int writeToOffset)
        {
            interpolation.interpolate(source, numBands, xfrac, yfrac, writeTo, writeToOffset);
            try {
                converter.transform(writeTo, writeToOffset, writeTo, writeToOffset, 1);
            } catch (TransformException e) {
                throw new BackingStoreException(e);     // Will be unwrapped by computeTile(…).
            }
        }

        /** This interpolation never need to be disabled. */
        @Override Interpolation toCompatible(final RenderedImage source) {
            return this;
        }
    }

    /**
     * Same as {@link InterpConvert} optimized for the single-band case.
     * This class uses the more efficient {@link MathTransform1D#transform(double)} method.
     */
    private static final class InterpConvertOneBand extends InterpConvert {
        /** Conversion from floating point values to values to store as integers in the destination image. */
        private final MathTransform1D singleConverter;

        /** Creates a new object combining the given interpolation with the given conversion of sample values. */
        InterpConvertOneBand(final Interpolation interpolation, final MathTransform1D converter) {
            super(interpolation, converter);
            singleConverter = converter;
        }

        /** Delegates to {@link #interpolation}, then convert sample values in all bands. */
        @Override public void interpolate(final DoubleBuffer source, final int numBands,
                                          final double xfrac, final double yfrac,
                                          final double[] writeTo, final int writeToOffset)
        {
            interpolation.interpolate(source, numBands, xfrac, yfrac, writeTo, writeToOffset);
            try {
                writeTo[writeToOffset] = singleConverter.transform(writeTo[writeToOffset]);
            } catch (TransformException e) {
                throw new BackingStoreException(e);     // Will be unwrapped by computeTile(…).
            }
        }
    }

    /**
     * Transfer functions to apply on each band of the source image, or {@code null} if those conversions are done
     * by {@link InterpConvert}. Non-null array is used for allowing {@link #computeTile(int, int, WritableRaster)}
     * to use a shortcut avoiding {@link ResampledImage} cost. Outputs should be values in the [0 … 255] range;
     * values outside that ranges will be clamped.
     */
    private final MathTransform1D[] converters;

    /**
     * The color model for the expected range of values. Typically an {@link IndexColorModel} for byte values.
     * May be {@code null} if the color model is unknown.
     */
    private final ColorModel colorModel;

    /**
     * Creates a new image which will resample and convert values of the given image.
     * See parent class for more details about arguments.
     */
    private Visualization(final Builder builder) {
        super(builder.source,
              builder.sampleModel,
              builder.layout.getPreferredMinTile(),
              builder.bounds,
              builder.toSource,
              builder.interpolation,
              builder.fillValues,
              builder.positionalAccuracyHints);

        this.colorModel = builder.colorModel;
        this.converters = builder.converters;
    }

    /**
     * Returns {@code true} if this image cannot have mask.
     */
    @Override
    final boolean hasNoMask() {
        return !(interpolation instanceof InterpConvert) && super.hasNoMask();
    }

    /**
     * Returns the color model associated with all rasters of this image.
     */
    @Override
    public ColorModel getColorModel() {
        return colorModel;
    }

    /**
     * Invoked when a tile need to be computed or updated.
     *
     * @throws TransformException if an error occurred while computing pixel coordinates or converting sample values.
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY, WritableRaster tile) throws TransformException {
        if (converters == null) try {
            // Most expensive operation (resampling + conversion).
            return super.computeTile(tileX, tileY, tile);
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(TransformException.class);
        }
        if (tile == null) {
            tile = createTile(tileX, tileY);
        }
        // Conversion only, when no resampling is needed.
        Transferer.create(getSource(), tile).compute(converters);
        return tile;
    }

    /**
     * Compares the given object with this image for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (super.equals(object)) {
            final Visualization other = (Visualization) object;
            return Arrays .equals(converters, other.converters) &&
                   Objects.equals(colorModel, other.colorModel);
        }
        return false;
    }

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 67 *  Arrays.hashCode(converters)
                                + 97 * Objects.hashCode(colorModel);
    }
}
