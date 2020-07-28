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
import java.util.function.Function;
import java.awt.Shape;
import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.DataBuffer;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.internal.coverage.j2d.BandedSampleConverter;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.coverage.j2d.Colorizer;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.Statistics;


/**
 * An image with the same sample values than the wrapped image but a different color model.
 * The only interesting member method is {@link #getColorModel()}, which returns the model
 * specified at construction time. All other non-trivial methods are static helper methods
 * for {@link ImageProcessor}, defined here for reducing {@link ImageProcessor} size.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class RecoloredImage extends ImageAdapter {
    /**
     * The color model to associate with this recolored image.
     *
     * @see #getColorModel()
     */
    private final ColorModel colors;

    /**
     * Creates a new recolored image with the given colors.
     */
    private RecoloredImage(final RenderedImage source, final ColorModel colors) {
        super(source);
        this.colors = colors;
    }

    /**
     * Returns a recolored image with the given colors. This method may return
     * an existing ancestor if one is found with the specified color model.
     */
    static RenderedImage create(RenderedImage source, final ColorModel colors) {
        if (colors == null) {
            return source;
        }
        for (;;) {
            if (colors.equals(source.getColorModel())) {
                return source;
            } else if (source instanceof RecoloredImage) {
                source = ((RecoloredImage) source).source;
            } else {
                break;
            }
        }
        return ImageProcessor.unique(new RecoloredImage(source, colors));
    }

    /**
     * Returns an image with the same sample values than the given image, but with its color ramp stretched
     * between specified or inferred bounds. The mapping applied by this method is conceptually a linear
     * transform applied on sample values before they are mapped to their colors.
     *
     * <p>Current implementation can stretch only gray scale images (it may be extended to indexed color models
     * in a future version). If this method can not stretch the color ramp, for example because the given image
     * is an RGB image, then the image is returned unchanged.</p>
     *
     * @param  processor  the processor to use for computing statistics if needed.
     * @param  source     the image to recolor (can be {@code null}).
     * @param  modifiers  modifiers for narrowing the range of values, or {@code null} if none.
     * @return the image with color ramp stretched between the automatic bounds,
     *         or {@code image} unchanged if the operation can not be applied on the given image.
     *
     * @see ImageProcessor#stretchColorRamp(RenderedImage, Map)
     */
    static RenderedImage stretchColorRamp(final ImageProcessor processor, final RenderedImage source, final Map<String,?> modifiers) {
        final int visibleBand = ImageUtilities.getVisibleBand(source);
        if (visibleBand >= 0) {
            RenderedImage statsSource   = source;
            Statistics[]  statsAllBands = null;
            Statistics    statistics    = null;
            double        minimum       = Double.NaN;
            double        maximum       = Double.NaN;
            double        deviations    = Double.POSITIVE_INFINITY;
            /*
             * Extract and validate parameter values.
             * No calculation started at this stage.
             */
            if (modifiers != null) {
                final Object minValue = modifiers.get("minimum");
                if (minValue instanceof Number) {
                    minimum = ((Number) minValue).doubleValue();
                }
                final Object maxValue = modifiers.get("maximum");
                if (maxValue instanceof Number) {
                    maximum = ((Number) maxValue).doubleValue();
                }
                if (minimum >= maximum) {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, minValue, maxValue));
                }
                Object value = modifiers.get("multStdDev");
                if (value instanceof Number) {
                    deviations = ((Number) value).doubleValue();
                    ArgumentChecks.ensureStrictlyPositive("multStdDev", deviations);
                }
                value = modifiers.get("statistics");
                if (value instanceof RenderedImage) {
                    statsSource = (RenderedImage) value;
                } else if (value instanceof Statistics) {
                    statistics = (Statistics) value;
                } else if (value instanceof Statistics[]) {
                    statsAllBands = (Statistics[]) value;
                }
            }
            /*
             * If minimum and maximum values were not explicitly specified,
             * compute them from statistics.
             */
            if (Double.isNaN(minimum) || Double.isNaN(maximum)) {
                if (statistics == null) {
                    if (statsAllBands == null) {
                        final Object areaOfInterest = modifiers.get("areaOfInterest");
                        statsAllBands = processor.getStatistics(statsSource,
                                (areaOfInterest instanceof Shape) ? (Shape) areaOfInterest : null);
                    }
                    if (statsAllBands != null && visibleBand < statsAllBands.length) {
                        statistics = statsAllBands[visibleBand];
                    }
                }
                if (statistics != null) {
                    deviations *= statistics.standardDeviation(true);
                    final double mean = statistics.mean();
                    if (Double.isNaN(minimum)) minimum = Math.max(statistics.minimum(), mean - deviations);
                    if (Double.isNaN(maximum)) maximum = Math.min(statistics.maximum(), mean + deviations);
                }
            }
            /*
             * Wraps the given image with its colors ramp scaled between the given bounds. If the given image is
             * already using a color ramp for the given range of values, then that image is returned unchanged.
             */
            if (minimum < maximum) {
                final SampleModel sm = source.getSampleModel();
                return create(source, ColorModelFactory.createGrayScale(
                        sm.getDataType(), sm.getNumBands(), visibleBand, minimum, maximum));
            }
        }
        return source;
    }

    /**
     * Returns an image where all sample values are indices of colors in an {@link IndexColorModel}.
     * If the given image stores sample values as unsigned bytes or short integers, then those values
     * are used as-is (they are not copied or converted). Otherwise this operation will convert sample
     * values to unsigned bytes in order to enable the use of {@link IndexColorModel}.
     *
     * <p>This method accepts two kinds of input. Use only one of the followings:</p>
     * <ul>
     *   <li>Non-null {@code sourceBands} and {@code colors}.</li>
     *   <li>Non-null {@code rangesAndColors}.</li>
     * </ul>
     *
     * The resulting image is suitable for visualization purposes but should not be used for computation purposes.
     * There is no guarantees about the number of bands in returned image and the formulas used for converting
     * floating point values to integer values.
     *
     * @param  processor        the processor to use for computing statistics if needed.
     * @param  source           the image for which to replace the color model.
     * @param  sourceBands      description of {@code source} bands, or {@code null} if none.
     * @param  colors           the colors to use for each category. The function may return {@code null}, which
     *                          means transparent. This parameter is used only if {@code rangesAndColors} is null.
     * @param  rangesAndColors  range of sample values in source image associated to colors to apply,
     *                          or {@code null} for using {@code sourceBands} and {@code colors} instead.
     * @return recolored image for visualization purposes only.
     * @throws NoninvertibleTransformException if sample values in source image can not be converted
     *         to sample values in the recolored image.
     *
     * @see ImageProcessor#toIndexedColors(RenderedImage, Map)
     */
    static RenderedImage toIndexedColors(final ImageProcessor processor, RenderedImage source,
            final List<SampleDimension> sourceBands, final Function<Category,Color[]> colors,
            final Collection<Map.Entry<NumberRange<?>,Color[]>> rangesAndColors)
            throws NoninvertibleTransformException
    {
        final int visibleBand = ImageUtilities.getVisibleBand(source);
        if (visibleBand < 0) {
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
            colorizer = new Colorizer(colors);
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
         * If the source image uses unsigned integer types, we can update the color model without changing
         * the sample values. This is much cheaper and as accurate.
         */
        final int dataType = ImageUtilities.getDataType(source);
        if (dataType == DataBuffer.TYPE_BYTE || dataType == DataBuffer.TYPE_USHORT) {
            return create(source, colorizer.createColorModel(dataType, 1, 0));
        }
        /*
         * Sample values can not be reused as-is; we need to convert them to integers in [0 … 255] range.
         * Skip any previous `RecoloredImage` since we are replacing the `ColorModel` by a new one.
         */
        while (source instanceof RecoloredImage) {
            source = ((RecoloredImage) source).source;
        }
        final ColorModel      colorModel = colorizer.compactColorModel(1, 0);           // Must be first.
        final MathTransform1D converter  = colorizer.getSampleToIndexValues();
        final NumberRange<?>  range      = colorizer.getRepresentativeRange();
        return BandedSampleConverter.create(source, null, Colorizer.TYPE_COMPACT, colorModel,
                                            new NumberRange<?>[] {range}, converter);
    }

    /**
     * Returns the color model of this image.
     */
    @Override
    public ColorModel getColorModel() {
        return colors;
    }

    /**
     * Compares the given object with this image for equality.
     */
    @Override
    public boolean equals(final Object object) {
        return super.equals(object) && colors.equals(((RecoloredImage) object).colors);
    }

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 37 * colors.hashCode();
    }

    /**
     * Appends a content to show in the {@link #toString()} representation,
     * after the class name and before the string representation of the wrapped image.
     */
    @Override
    final Class<RecoloredImage> appendStringContent(final StringBuilder buffer) {
        buffer.append(colors.getColorSpace());
        return RecoloredImage.class;
    }
}
