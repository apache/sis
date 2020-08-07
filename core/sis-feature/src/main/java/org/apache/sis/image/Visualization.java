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
import java.util.Collection;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.DataBuffer;
import java.nio.DoubleBuffer;
import javax.measure.Quantity;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.internal.coverage.CompoundTransform;
import org.apache.sis.internal.coverage.j2d.Colorizer;
import org.apache.sis.internal.coverage.j2d.ImageLayout;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Image generated for visualization purposes only (not to be used for computation purposes).
 * This class merges {@link ResampledImage}, {@link BandedSampleConverter} and {@link RecoloredImage} operations
 * in a single operation for efficiency. This merge avoids creating intermediate tiles of {@code float} values.
 * By writing directly {@code byte} values, we save memory and CPU since {@link WritableRaster#setPixel(int, int, int[])}
 * has more efficient implementations for integers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class Visualization extends ResampledImage {
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
     * See parent class for more details on arguments.
     *
     * @param source         image to be resampled and converted.
     * @param layout         computer of tile size.
     * @param bounds         domain of pixel coordinates of this image, or {@code null} if same as {@code source} image.
     * @param toSource       conversion of pixel coordinates of this image to pixel coordinates of {@code source} image.
     * @param isIdentity     value of {@code toSource.isIdentity()}.
     * @param interpolation  object to use for performing interpolations.
     * @param converters     transfer functions to apply on each band of the source image. This array is not cloned.
     * @param fillValues     values to use for pixels in this image that can not be mapped to pixels in source image.
     * @param colorModel     color model of this image.
     * @param accuracy       values of {@value #POSITIONAL_ACCURACY_KEY} property, or {@code null} if none.
     */
    Visualization(final RenderedImage source,         final ImageLayout layout,  final Rectangle bounds,
                  final MathTransform toSource,       final boolean isIdentity,  final Interpolation interpolation,
                  final MathTransform1D[] converters, final Number[] fillValues, final ColorModel colorModel,
                  final Quantity<?>[] accuracy)
    {
        super(source,
              layout.createBandedSampleModel(Colorizer.TYPE_COMPACT, converters.length, source, bounds),
              (bounds != null) ? bounds : ImageUtilities.getBounds(source),
              toSource,
              isIdentity ? Interpolation.NEAREST : combine(interpolation, converters),
              fillValues,
              accuracy);

        this.colorModel = colorModel;
        this.converters = isIdentity ? converters : null;
    }

    /**
     * Combines the given interpolation method with the given sample conversion.
     */
    static Interpolation combine(final Interpolation interpolation, final MathTransform1D[] converters) {
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
    static class InterpConvert implements Interpolation {
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
     * Returns {@code true} if this image can not have mask.
     */
    @Override
    final boolean hasNoMask() {
        return !(interpolation instanceof InterpConvert) && super.hasNoMask();
    }

    /**
     * Returns an image where all sample values are indices of colors in an {@link IndexColorModel}.
     * If the given image stores sample values as unsigned bytes or short integers, then those values
     * are used as-is (they are not copied or converted). Otherwise this operation will convert sample
     * values to unsigned bytes in order to enable the use of {@link IndexColorModel}.
     *
     * <p>This method accepts two kinds of input. Use only one of the followings:</p>
     * <ul>
     *   <li>Non-null {@code sourceBands} and {@link ImageProcessor#getCategoryColors()}.</li>
     *   <li>Non-null {@code rangesAndColors}.</li>
     * </ul>
     *
     * The resulting image is suitable for visualization purposes but should not be used for computation purposes.
     * There is no guarantees about the number of bands in returned image and the formulas used for converting
     * floating point values to integer values.
     *
     * <h4>Resampling</h4>
     * This operation can optionally be combined with a {@link ResampledImage} operation.
     * This can be done by providing a non-null value to the {@code toSource} argument.
     *
     * @param  processor        the processor invoking this method.
     * @param  bounds           desired domain of pixel coordinates, or {@code null} if same as {@code source} image.
     * @param  source           the image for which to replace the color model.
     * @param  toSource         pixel coordinates conversion to {@code source} image, or {@code null} if none.
     * @param  sourceBands      description of {@code source} bands, or {@code null} if none.
     * @param  rangesAndColors  range of sample values in source image associated to colors to apply,
     *                          or {@code null} for using {@code sourceBands} instead.
     * @return resampled and recolored image for visualization purposes only.
     * @throws NoninvertibleTransformException if sample values in source image can not be converted
     *         to sample values in the recolored image.
     *
     * @see ImageProcessor#visualize(RenderedImage, Map)
     */
    static RenderedImage create(final ImageProcessor processor, final Rectangle bounds,
                                RenderedImage source, MathTransform toSource,
                                final List<SampleDimension> sourceBands,
                                final Collection<Map.Entry<NumberRange<?>,Color[]>> rangesAndColors)
            throws NoninvertibleTransformException
    {
        final int visibleBand = ImageUtilities.getVisibleBand(source);
        if (visibleBand < 0) {
            // This restriction may be relaxed in a future version if we implement conversion to RGB images.
            throw new IllegalArgumentException(Resources.format(Resources.Keys.OperationRequiresSingleBand));
        }
        boolean initialized;
        final Colorizer colorizer;
        if (rangesAndColors != null) {
            colorizer = new Colorizer(rangesAndColors);
            initialized = true;
        } else {
            /*
             * Ranges of sample values were not specified explicitly. Instead we will try to infer them
             * in various ways: sample dimensions, scaled color model, statistics in last resort.
             */
            colorizer = new Colorizer(processor.getCategoryColors());
            initialized = (sourceBands != null) && colorizer.initialize(sourceBands.get(visibleBand));
            if (initialized) {
                /*
                 * If we have been able to configure Colorizer using the SampleModel, apply an adjustment based
                 * on the ScaledColorModel if it exists.  Use case: an image is created with an IndexColorModel
                 * determined by the SampleModel, then user enhanced contrast by a call to `stretchColorRamp(…)`
                 * above. We want to preserve that contrast enhancement.
                 */
                colorizer.rescaleMainRange(source.getColorModel());
            } else {
                /*
                 * If we have not been able to use the SampleDimension, try to use the ColorModel or SampleModel.
                 * There is no call to `rescaleMainRange(…)` because the following code already uses the range
                 * specified by the ColorModel, if available.
                 */
                initialized = colorizer.initialize(source.getColorModel()) ||
                              colorizer.initialize(source.getSampleModel(), visibleBand);
            }
        }
        source = BandSelectImage.create(source, new int[] {visibleBand});               // Make single-banded.
        if (!initialized) {
            /*
             * If none of above Colorizer configurations worked, use statistics in last resort. We do that
             * after we reduced the image to a single band, in order to reduce the amount of calculations.
             */
            final Statistics statistics = processor.getStatistics(source, null)[0];
            colorizer.initialize(statistics.minimum(), statistics.maximum());
        }
        /*
         * If the source image uses unsigned integer types and there is no resampling operation, we can
         * update the color model without changing sample values. This is much cheaper and as accurate.
         */
        final int dataType = source.getSampleModel().getDataType();
        if (dataType == DataBuffer.TYPE_BYTE || dataType == DataBuffer.TYPE_USHORT) {
            if (toSource != null && !toSource.isIdentity()) {
                source = processor.resample(source, bounds, toSource);
            }
            return RecoloredImage.create(source, colorizer.createColorModel(dataType, 1, 0));
        }
        /*
         * If we reach this point, sample values need to be converted to integers in [0 … 255] range.
         * Skip any previous `RecoloredImage` since we are replacing the `ColorModel` by a new one.
         */
        while (source instanceof RecoloredImage) {
            source = ((RecoloredImage) source).source;
        }
        if (toSource == null) {
            toSource = MathTransforms.identity(BIDIMENSIONAL);
        }
        final ColorModel      colorModel = colorizer.compactColorModel(1, 0);           // Must be first.
        final MathTransform1D converter  = colorizer.getSampleToIndexValues();
        return processor.resampleAndConvert(source, toSource,
                new MathTransform1D[] {converter}, bounds, colorModel);
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
            // Most expansive operation (resampling + conversion).
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
}
