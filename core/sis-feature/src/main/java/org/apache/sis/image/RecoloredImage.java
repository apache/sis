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
import java.util.function.DoubleUnaryOperator;
import java.awt.Shape;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.math.Statistics;
import org.apache.sis.measure.NumberRange;


/**
 * An image with the same sample values than the wrapped image but a different color model.
 * The only interesting member method is {@link #getColorModel()}, which returns the model
 * specified at construction time. All other non-trivial methods are static helper methods
 * for {@link ImageProcessor}, defined here for reducing {@link ImageProcessor} size.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
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
     * The minimum and maximum values used for computing the color model.
     * This is used for preserving color ramp stretching when a new color ramp is applied.
     *
     * <p>Current implementation can only describes a uniform stretching between a minimum and maximum value.
     * Future version may allow more sophisticated ways to redistribute the colors. The possibilities are
     * determined by {@link #stretchColorRamp(ImageProcessor, RenderedImage, Map)} implementation.</p>
     */
    final double minimum, maximum;

    /**
     * Creates a new recolored image with the given colors.
     *
     * @param  source   the image to wrap.
     * @param  colors   the new color model.
     * @param  minimum  the minimal sample value used for computing the color model.
     * @param  maximum  the maximal sample value used for computing the color model.
     */
    private RecoloredImage(final RenderedImage source, final ColorModel colors, final double minimum, final double maximum) {
        super(source);
        this.colors  = colors;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    /**
     * Returns a recolored image with the same colors than the given image.
     * This method may return an existing ancestor if one is found with the desired color model.
     *
     * @param  source   the image to wrap.
     * @param  colored  the image from which to preserve the color model.
     */
    static RenderedImage applySameColors(RenderedImage source, RenderedImage colored) {
        final ColorModel colors = colored.getColorModel();
        if (colors == null) {
            return source;
        }
        /*
         * Find the image which contains the minimum and maximum values that we want to keep.
         * We can skip `ImageAdapter` because those images may modify properties but not sample values.
         */
        RecoloredImage expected = null;
        while (colored instanceof ImageAdapter) {
            if (colored instanceof RecoloredImage) {
                expected = (RecoloredImage) colored;
                break;
            }
            colored = ((ImageAdapter) colored).source;
        }
        /*
         * Verify if the given image, or one of its sources, has the expected color model.
         * We explore only the sources that are themselves `RecoloredImage` instances,
         * because other kind of images are result of operations that we want to keep.
         */
        for (;;) {
            if (colors.equals(source.getColorModel())) {
                if (expected != null && source instanceof RecoloredImage) {
                    final RecoloredImage actual = (RecoloredImage) source;
                    if (!(Numerics.equals(expected.minimum, actual.minimum) &&
                          Numerics.equals(expected.maximum, actual.maximum)))
                    {
                        continue;
                    }
                }
                return source;
            }
            if (source instanceof RecoloredImage) {
                source = ((RecoloredImage) source).source;
            } else {
                break;
            }
        }
        /*
         * At this point we found no existing image with the desired color model,
         * or the minimum/maximum information would be lost. Create a new image.
         */
        final RecoloredImage image;
        if (expected != null) {
            image = new RecoloredImage(source, colors, expected.minimum, expected.maximum);
        } else {
            image = new RecoloredImage(source, colors, Double.NaN, Double.NaN);
        }
        return ImageProcessor.unique(image);
    }

    /**
     * Returns an image with the same sample values than the given image, but with its color ramp stretched
     * between specified or inferred bounds. The mapping applied by this method is conceptually a linear
     * transform applied on sample values before they are mapped to their colors.
     *
     * <p>Current implementation can stretch gray scale and {@linkplain IndexColorModel index color models}).
     * If this method can not stretch the color ramp, for example because the given image is an RGB image,
     * then the image is returned unchanged.</p>
     *
     * @param  processor  the processor to use for computing statistics if needed.
     * @param  source     the image to recolor (can be {@code null}).
     * @param  modifiers  modifiers for narrowing the range of values, or {@code null} if none.
     * @return the image with color ramp stretched between the automatic bounds,
     *         or {@code image} unchanged if the operation can not be applied on the given image.
     *
     * @see ImageProcessor#stretchColorRamp(RenderedImage, Map)
     */
    static RenderedImage stretchColorRamp(final ImageProcessor processor, RenderedImage source,
                                          final Map<String,?> modifiers)
    {
        /*
         * Images having more than one band (without any band marked as the single band to show) are probably
         * RGB images. It would be possible to stretch the Red, Green and Blue bands separately, but current
         * implementation don't do that because we do not have yet a clear use case.
         */
        final int visibleBand = ImageUtilities.getVisibleBand(source);
        if (visibleBand < 0) {
            return source;
        }
        /*
         * Main use case: color model is (probably) an IndexColorModel or ScaledColorModel instance,
         * or something we can handle in the same way.
         */
        RenderedImage   statsSource    = source;
        Statistics[]    statsAllBands  = null;
        Statistics      statistics     = null;
        Shape           areaOfInterest = null;
        Number[]        nodataValues   = null;
        SampleDimension range          = null;
        double          minimum        = Double.NaN;
        double          maximum        = Double.NaN;
        double          deviations     = Double.POSITIVE_INFINITY;
        /*
         * Extract and validate parameter values.
         * No calculation started at this stage.
         */
        if (modifiers != null) {
            final Number minValue = Containers.property(modifiers, "minimum", Number.class);
            final Number maxValue = Containers.property(modifiers, "maximum", Number.class);
            if (minValue != null) minimum = minValue.doubleValue();
            if (maxValue != null) maximum = maxValue.doubleValue();
            if (minimum >= maximum) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, minValue, maxValue));
            }
            {   // For keeping `value` in local scope.
                final Number value = Containers.property(modifiers, "multStdDev", Number.class);
                if (value != null) {
                    deviations = value.doubleValue();
                    ArgumentChecks.ensureStrictlyPositive("multStdDev", deviations);
                }
            }
            areaOfInterest = Containers.property(modifiers, "areaOfInterest", Shape.class);
            Object value = modifiers.get("nodataValues");
            if (value != null) {
                if (value instanceof Number) {
                    nodataValues = new Number[] {(Number) value};
                } else if (value instanceof Number[]) {
                    nodataValues = (Number[]) value;
                } else {
                    throw illegalPropertyType(modifiers, "nodataValues", value);
                }
            }
            value = modifiers.get("statistics");
            if (value != null) {
                if (value instanceof RenderedImage) {
                    statsSource = (RenderedImage) value;
                } else if (value instanceof Statistics) {
                    statistics = (Statistics) value;
                } else if (value instanceof Statistics[]) {
                    statsAllBands = (Statistics[]) value;
                } else {
                    throw illegalPropertyType(modifiers, "statistics", value);
                }
            }
            value = modifiers.get("sampleDimensions");
            if (value != null) {
                if (value instanceof List<?>) {
                    final List<?> ranges = (List<?>) value;
                    if (visibleBand < ranges.size()) {
                        value = ranges.get(visibleBand);
                    }
                }
                if (value != null) {
                    if (value instanceof SampleDimension) {
                        range = (SampleDimension) value;
                    } else {
                        throw illegalPropertyType(modifiers, "sampleDimensions", value);
                    }
                }
            }
        }
        /*
         * If minimum and maximum values were not explicitly specified, compute them from statistics.
         * If the range is not valid, then the image will be silently returned as-is.
         */
        if (Double.isNaN(minimum) || Double.isNaN(maximum)) {
            if (statistics == null) {
                if (statsAllBands == null) {
                    final DoubleUnaryOperator[] sampleFilters = new DoubleUnaryOperator[visibleBand + 1];
                    sampleFilters[visibleBand] = processor.filterNodataValues(nodataValues);
                    statsAllBands = processor.valueOfStatistics(statsSource, areaOfInterest, sampleFilters);
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
        if (!(minimum < maximum)) {     // Use ! for catching NaN.
            return source;
        }
        /*
         * Finished to collect information. Derive a new color model from the existing one.
         */
        final ColorModel cm;
        if (source.getColorModel() instanceof IndexColorModel) {
            /*
             * Get the range of indices of RGB values that can be used for interpolations.
             * We want to exclude qualitative categories (no data, clouds, forests, etc.).
             * In the vast majority of cases, we have at most one quantitative category.
             * But if there is 2 or more, then we select the one having largest intersection
             * with the [minimum … maximum] range.
             */
            final IndexColorModel icm = (IndexColorModel) source.getColorModel();
            final int size = icm.getMapSize();
            int validMin = 0;
            int validMax = size - 1;        // Inclusive.
            if (range != null) {
                double span = 0;
                for (final Category category : range.getCategories()) {
                    if (category.isQuantitative()) {
                        final NumberRange<?> r = category.getSampleRange();
                        final double min = Math.max(r.getMinDouble(true), 0);
                        final double max = Math.min(r.getMaxDouble(true), size - 1);
                        final double s   = Math.min(max, maximum) - Math.max(min, minimum);    // Intersection.
                        if (s > span) {
                            validMin = (int) min;
                            validMax = (int) max;
                            span = s;
                        }
                    }
                }
            }
            /*
             * Create a copy of RGB codes and replace values in the range of the quantitative category.
             * Values for other categories (qualitative) are left unmodified.
             */
            final int   start = Math.max((int) minimum, validMin);
            final int   end   = Math.min((int) maximum, validMax);          // Inclusive.
            final int[] ARGB  = new int[size];
            icm.getRGBs(ARGB);                                              // Initialize to a copy of current colors.
            Arrays.fill(ARGB, validMin, start,   icm.getRGB(validMin));     // Part of quantitative category outside the new range.
            Arrays.fill(ARGB, end+1, validMax+1, icm.getRGB(validMax));
            final float scale = (float) ((validMax - validMin) / (maximum - minimum));
            for (int i = start; i <= end; i++) {
                final float s = (i - start) * scale + validMin;
                ARGB[i] = icm.getRGB(Math.round(s));
            }
            final SampleModel sm = source.getSampleModel();
            cm = ColorModelFactory.createIndexColorModel(sm.getNumBands(), visibleBand, ARGB, icm.hasAlpha(), icm.getTransparentPixel());
        } else {
            /*
             * Wraps the given image with its colors ramp scaled between the given bounds. If the given image is
             * already using a color ramp for the given range of values, then that image is returned unchanged.
             */
            final SampleModel sm = source.getSampleModel();
            cm = ColorModelFactory.createGrayScale(sm.getDataType(), sm.getNumBands(), visibleBand, minimum, maximum);
        }
        /*
         * Verify if an existing ancestor already have the specified color model.
         * If not, built the new `RecoloredImage` here.
         */
        for (;;) {
            if (cm.equals(source.getColorModel())) {
                if (source instanceof RecoloredImage) {
                    final RecoloredImage colored = (RecoloredImage) source;
                    if (colored.minimum != minimum || colored.maximum != maximum) {
                        continue;
                    }
                }
                return source;
            } else if (source instanceof RecoloredImage) {
                source = ((RecoloredImage) source).source;
            } else {
                break;
            }
        }
        return ImageProcessor.unique(new RecoloredImage(source, cm, minimum, maximum));
    }

    /**
     * Returns the exception to be thrown when a property is of illegal type.
     */
    private static IllegalArgumentException illegalPropertyType(
            final Map<String,?> properties, final String key, final Object value)
    {
        return new IllegalArgumentException(Errors.getResources(properties)
                .getString(Errors.Keys.IllegalPropertyValueClass_2, key, value.getClass()));
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
        if (super.equals(object)) {
            final RecoloredImage other = (RecoloredImage) object;
            return Numerics.equals(minimum, other.minimum) &&
                   Numerics.equals(maximum, other.maximum) &&
                   colors.equals(other.colors);
        }
        return false;
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
