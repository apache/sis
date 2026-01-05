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
import java.util.Arrays;
import java.util.Objects;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.awt.image.BandedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.lang.reflect.Array;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.internal.shared.SampleDimensions;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.image.internal.shared.TileOpExecutor;
import org.apache.sis.image.internal.shared.ColorScaleBuilder;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.NumberType;
import org.apache.sis.measure.NumberRange;
import static org.apache.sis.image.internal.shared.ImageUtilities.LOGGER;


/**
 * An image where each sample value is computed independently of other sample values and independently
 * of neighbor points. Values are computed by a separated {@link MathTransform1D} for each band
 * (by contrast, an {@code InterleavedSampleConverter} would handle all sample values as a coordinate tuple).
 * Current implementation makes the following simplifications:
 *
 * <ul>
 *   <li>The image has exactly one source.</li>
 *   <li>Image layout (minimum coordinates, image size, tile grid) is the same as source image layout,
 *     unless the source has too large tiles in which case {@link ImageLayout} automatically subdivides
 *     the tile grid in smaller tiles.</li>
 *   <li>Image is computed and stored on a band-by-band basis using a {@link BandedSampleModel}.</li>
 *   <li>Calculation is performed on {@code float} or {@code double} numbers.</li>
 * </ul>
 *
 * If the given source is writable and the transform are invertible, then the {@code BandedSampleConverter}
 * returned by the {@link #create create(…)} method will implement {@link WritableRenderedImage} interface.
 * In such case, writing converted values will cause the corresponding source values to be updated too.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
class BandedSampleConverter extends WritableComputedImage {
    /*
     * Do not extend `SourceAlignedImage` because we want to inherit the `getNumTiles()`
     * and `getTileGridOffset()` methods defined by `PlanarImage`.
     */

    /**
     * Properties potentially added by this image, no matter if present in source image or not. Must be consistent
     * with the <i>switch case</i> statement doing its own calculation in {@link #getProperty(String)}.
     *
     * @see #getPropertyNames()
     */
    private static final String[] ADDED_PROPERTIES = {SAMPLE_DIMENSIONS_KEY, SAMPLE_RESOLUTIONS_KEY};

    /**
     * The transfer functions to apply on each band of the source image.
     */
    private final MathTransform1D[] converters;

    /**
     * The color model for the expected range of values. May be {@code null}.
     */
    private final ColorModel colorModel;

    /**
     * Description of bands, or {@code null} if unknown.
     * Not used by this class, but provided as a {@value #SAMPLE_DIMENSIONS_KEY} property.
     * The value is fetched from {@link SampleDimensions#IMAGE_PROCESSOR_ARGUMENT} for avoiding
     * to expose a {@code SampleDimension[]} argument in public {@link ImageProcessor} API.
     *
     * @see #getProperty(String)
     */
    private final List<SampleDimension> sampleDimensions;

    /**
     * The sample resolutions, or {@code null} if unknown.
     */
    private final double[] sampleResolutions;

    /**
     * Creates a new image which will compute values using the given converters.
     *
     * @param  source       the image for which to convert sample values.
     * @param  sampleModel  the sample model shared by all tiles in this image.
     * @param  colorModel   the color model for the expected range of values, or {@code null}.
     * @param  ranges       the expected range of values for each band, or {@code null} if unknown.
     * @param  converters   the transfer functions to apply on each band of the source image.
     *                      If this array was a user-provided parameter, should be cloned by caller.
     * @param  sampleDimensions  description of conversion result, or {@code null} if unknown.
     */
    private BandedSampleConverter(final RenderedImage         source,
                                  final BandedSampleModel     sampleModel,
                                  final ColorModel            colorModel,
                                  final NumberRange<?>[]      ranges,
                                  final MathTransform1D[]     converters,
                                  final List<SampleDimension> sampleDimensions)
    {
        super(sampleModel, source);
        this.colorModel       = colorModel;
        this.converters       = converters;
        this.sampleDimensions = sampleDimensions;
        ensureCompatible(sampleModel, colorModel);
        /*
         * Get an estimation of the resolution, arbitrarily looking in the middle of the range of values.
         * If the converters are linear (which is the most common case), the middle value does not matter
         * except if it falls on a "no data" value.
         */
        boolean hasResolutions = false;
        final double[] resolutions = new double[converters.length];
        final Object sr = source.getProperty(SAMPLE_RESOLUTIONS_KEY);
        final int n = (sr != null && NumberType.isReal(sr.getClass().getComponentType())) ? Array.getLength(sr) : 0;
        for (int i=0; i<resolutions.length; i++) {
            /*
             * Get the sample value in the middle of the range of valid values for the current band.
             * If no range was explicitly given, use the approximate average of all possible values.
             */
            double middle = Double.NaN;
            if (ranges != null && i < ranges.length) {
                final NumberRange<?> range = ranges[i];
                if (range != null) {
                    middle = range.getMedian();
                }
            }
            if (!Double.isFinite(middle)) {
                final SampleModel sm = source.getSampleModel();
                if (DataType.isUnsigned(sm)) {
                    middle = Math.scalb(0.5, sm.getSampleSize(i));
                } else {
                    middle = 0;
                }
            }
            /*
             * Get the derivative in the middle value, which is constant everywhere
             * in the common case of a linear transform.
             */
            final MathTransform1D c = converters[i];
            double r;
            try {
                r = c.derivative(middle);
                if (!Double.isFinite(r)) {
                    r = c.derivative(1);                // Second attempt if the middle value didn't work.
                }
            } catch (TransformException e) {
                r = Double.NaN;
            }
            /*
             * The implicit source resolution is 1 on the assumption that we are converting from
             * integer values. But if the source image specifies a resolution, use the specified
             * value instead of the implicit 1 value.
             */
            if (i < n) {
                final Number v = (Number) Array.get(sr, i);
                if (v != null) {
                    double f = (v instanceof Float) ? DecimalFunctions.floatToDouble(v.floatValue()) : v.doubleValue();
                    if (f > 0) r *= f;      // Ignore also NaN.
                }
            }
            resolutions[i] = r;
            hasResolutions |= Double.isFinite(r);
        }
        sampleResolutions = hasResolutions ? resolutions : null;
    }

    /**
     * Creates a new image of the given data type which will compute values using the given converters.
     * The number of bands is the length of the {@code converters} array, which must be greater than 0
     * and not greater than the number of bands in the source image.
     *
     * @param  source        the image for which to convert sample values.
     * @param  layout        object to use for computing tile size.
     * @param  sourceRanges  the expected range of values for each band in source image, or {@code null} if unknown.
     * @param  converters    the transfer functions to apply on each band of the source image.
     * @param  targetType    the type of this image resulting from conversion of given image.
     * @param  colorizer     provider of color model for the expected range of values, or {@code null}.
     * @return the image which compute converted values from the given source.
     *
     * @see ImageProcessor#convert(RenderedImage, NumberRange[], MathTransform1D[], DataType)
     */
    static BandedSampleConverter create(RenderedImage     source,
                                  final ImageLayout       layout,
                                  final NumberRange<?>[]  sourceRanges,
                                  final MathTransform1D[] converters,
                                  final DataType          targetType,
                                  final Colorizer         colorizer)
    {
        /*
         * Since this operation applies its own ColorModel anyway, skip operation that was doing nothing else
         * than changing the color model. The new color model may be specified by the user if (s)he provided
         * a `Colorizer` instance. Otherwise a default color model will be inferred.
         */
        if (source instanceof RecoloredImage) {
            source = ((RecoloredImage) source).source;
        }
        final int numBands = converters.length;
        final BandedSampleModel sampleModel = layout.createBandedSampleModel(source, null, targetType, numBands, 0);
        final List<SampleDimension> sampleDimensions = SampleDimensions.IMAGE_PROCESSOR_ARGUMENT.get();
        final int visibleBand = ImageUtilities.getVisibleBand(source);
        ColorModel colorModel = ColorScaleBuilder.NULL_COLOR_MODEL;
        if (colorizer != null) {
            var target = new Colorizer.Target(sampleModel, sampleDimensions, visibleBand);
            colorModel = colorizer.apply(target).orElse(null);
        }
        if (colorModel == null) {
            /*
             * If no color model was specified or inferred from a colorizer,
             * default to grayscale for a range inferred from the sample dimension.
             * If no sample dimension is specified, infer value range from data type.
             */
            SampleDimension sd = null;
            if (sampleDimensions != null && visibleBand >= 0 && visibleBand < sampleDimensions.size()) {
                sd = sampleDimensions.get(visibleBand);
            }
            final var builder = new ColorScaleBuilder(ColorScaleBuilder.GRAYSCALE, null, false);
            if (builder.initialize(source.getSampleModel(), sd) ||
                builder.initialize(source.getColorModel()))
            {
                colorModel = builder.createColorModel(targetType, numBands, Math.max(visibleBand, 0));
            }
        }
        /*
         * If the source image is writable, then change in the converted image may be retro-propagated
         * to that source image. If we fail to compute the required inverse transforms, log a notice at
         * a low level because this is not a serious problem; writable BandedSampleConverter is a plus
         * but not a requirement.
         */
        if (source instanceof WritableRenderedImage) try {
            final MathTransform1D[] inverses = new MathTransform1D[numBands];
            for (int i=0; i<numBands; i++) {
                inverses[i] = converters[i].inverse();
            }
            return new Writable((WritableRenderedImage) source, sampleModel, colorModel,
                                sourceRanges, converters, inverses, sampleDimensions);
        } catch (NoninvertibleTransformException e) {
            Logging.recoverableException(LOGGER, ImageProcessor.class, "convert", e);
        }
        return new BandedSampleConverter(source, sampleModel, colorModel, sourceRanges, converters, sampleDimensions);
    }

    /**
     * Gets a property from this image. Current implementation recognizes:
     * <ul>
     *   <li>{@value #SAMPLE_RESOLUTIONS_KEY}, computed by this class.</li>
     *   <li>{@value #SAMPLE_DIMENSIONS_KEY}, provided to the constructor.</li>
     *   <li>All positional properties, forwarded to source image.</li>
     * </ul>
     */
    @Override
    public Object getProperty(final String key) {
        switch (key) {
            case SAMPLE_DIMENSIONS_KEY: {
                if (sampleDimensions != null) {
                    return sampleDimensions.toArray(SampleDimension[]::new);
                }
                break;
            }
            case SAMPLE_RESOLUTIONS_KEY: {
                if (sampleResolutions != null) {
                    return sampleResolutions.clone();
                }
                break;
            }
            default: {
                if (SourceAlignedImage.POSITIONAL_PROPERTIES.contains(key)) {
                    return getSource().getProperty(key);
                }
                break;
            }
        }
        return super.getProperty(key);
    }

    /**
     * Returns the names of all recognized properties, or {@code null} if this image has no properties.
     */
    @Override
    public String[] getPropertyNames() {
        return SourceAlignedImage.filterPropertyNames(getSource().getPropertyNames(),
                SourceAlignedImage.POSITIONAL_PROPERTIES, (sampleResolutions != null) ? ADDED_PROPERTIES : null);
    }

    /**
     * Returns the color model associated with all rasters of this image.
     * If the sample values of this image are floating point numbers, then
     * a gray scale color model is computed from the expected range of values.
     *
     * @return the color model of this image, or {@code null} if none.
     */
    @Override
    public ColorModel getColorModel() {
        return colorModel;
    }

    /**
     * Returns the width (in pixels) of this image.
     * This is the same value as the source image (not necessarily zero).
     *
     * @return the width (number of columns) of this image.
     */
    @Override
    public int getWidth() {
        return getSource().getWidth();
    }

    /**
     * Returns the height (in pixels) of this image.
     * This is the same value as the source image (not necessarily zero).
     *
     * @return the height (number of rows) of this image.
     */
    @Override
    public int getHeight() {
        return getSource().getHeight();
    }

    /**
     * Returns the minimum <var>x</var> coordinate (inclusive) of this image.
     * This is the same value as the source image (not necessarily zero).
     *
     * @return the minimum <var>x</var> coordinate (column) of this image.
     */
    @Override
    public int getMinX() {
        return getSource().getMinX();
    }

    /**
     * Returns the minimum <var>y</var> coordinate (inclusive) of this image.
     * This is the same value as the source image (not necessarily zero).
     *
     * @return the minimum <var>y</var> coordinate (row) of this image.
     */
    @Override
    public int getMinY() {
        return getSource().getMinY();
    }

    /**
     * Returns the minimum tile index in the <var>x</var> direction.
     * This is the same value as the source image (not necessarily zero).
     *
     * @return the minimum tile index in the <var>x</var> direction.
     */
    @Override
    public int getMinTileX() {
        return getSource().getMinTileX();
    }

    /**
     * Returns the minimum tile index in the <var>y</var> direction.
     * This is the same value as the source image (not necessarily zero).
     *
     * @return the minimum tile index in the <var>y</var> direction.
     */
    @Override
    public int getMinTileY() {
        return getSource().getMinTileY();
    }

    /**
     * Computes the tile at specified indices.
     *
     * @param  tileX   the column index of the tile to compute.
     * @param  tileY   the row index of the tile to compute.
     * @param  target  if the tile already exists but needs to be updated, the tile to update. Otherwise {@code null}.
     * @return computed tile for the given indices (cannot be null).
     * @throws TransformException if an error occurred while converting a sample value.
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY, WritableRaster target) throws TransformException {
        if (target == null) {
            target = createTile(tileX, tileY);
        }
        Transferer.create(getSource(), target).compute(converters);
        return target;
    }

    /**
     * Notifies the source image that tiles will be computed soon in the given region.
     * If the source image is an instance of {@link PlanarImage}, then this method
     * forwards the notification to it. Otherwise default implementation does nothing.
     */
    @Override
    protected Disposable prefetch(Rectangle tiles) {
        final RenderedImage source = getSource();
        if (source instanceof PlanarImage) {
            tiles = ImageUtilities.convertTileIndices(this, source, tiles);
            return ((PlanarImage) source).prefetch(tiles);
        } else {
            return super.prefetch(tiles);
        }
    }

    /**
     * A {@code BandedSampleConverter} capable to retro-propagate the changes to the source coverage.
     * This class contains the inverse of all {@link MathTransform1D} given to the parent class.
     */
    private static final class Writable extends BandedSampleConverter implements WritableRenderedImage {
        /**
         * The converters for computing the source values from a converted value.
         */
        private final MathTransform1D[] inverses;

        /**
         * Creates a new writable image which will compute values using the given converters.
         */
        Writable(final WritableRenderedImage source,
                 final BandedSampleModel     sampleModel,
                 final ColorModel            colorModel,
                 final NumberRange<?>[]      ranges,
                 final MathTransform1D[]     converters,
                 final MathTransform1D[]     inverses,
                 final List<SampleDimension> sampleDimensions)
        {
            super(source, sampleModel, colorModel, ranges, converters, sampleDimensions);
            this.inverses = inverses;
        }

        /**
         * Sets a region of the image to the contents of the given raster.
         * The raster is assumed to be in the same coordinate space as this image.
         * The operation is clipped to the bounds of this image.
         *
         * @param  data  the values to write in this image.
         */
        @Override
        public void setData(final Raster data) {
            final Rectangle bounds = data.getBounds();
            final WritableRenderedImage target = (WritableRenderedImage) getSource();
            ImageUtilities.clipBounds(target, bounds);
            if (!bounds.isEmpty()) {
                final TileOpExecutor executor = new TileOpExecutor(target, bounds) {
                    @Override protected void writeTo(final WritableRaster target) throws TransformException {
                        final Rectangle aoi = target.getBounds().intersection(bounds);
                        Transferer.create(data, target, aoi).compute(inverses);
                    }
                };
                executor.writeTo(target);
                /*
                 * Request to recompute the tiles of this `BandedSampleConverter` because if the values
                 * in the source image are integers, then converting back to floating point values may
                 * produce slightly different results.
                 */
                markDirtyTiles(executor.getTileIndices());
            }
        }

        /**
         * Restores the identity behavior for writable image,
         * because it may have listeners attached to this specific instance.
         */
        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        /**
         * Restores the identity behavior for writable image,
         * because it may have listeners attached to this specific instance.
         */
        @Override
        public boolean equals(final Object object) {
            return object == this;
        }
    }

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return hashCodeBase() + 37 * Arrays.hashCode(converters) + Objects.hashCode(colorModel);
    }

    /**
     * Compares the given object with this image for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (equalsBase(object)) {
            final BandedSampleConverter other = (BandedSampleConverter) object;
            return Arrays .equals(converters, other.converters) &&
                   Objects.equals(colorModel, other.colorModel) &&
                   Arrays .equals(sampleResolutions, other.sampleResolutions);
        }
        return false;
    }
}
