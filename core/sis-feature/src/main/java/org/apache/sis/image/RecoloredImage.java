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
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.math.Statistics;


/**
 * An image with the same sample values than the wrapped image but a different color model.
 * Current implementation can only apply a gray scale. Future implementations may detect
 * the existing color model and try to preserve colors (for example by building an indexed
 * color model).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class RecoloredImage extends ImageAdapter {
    /**
     * The color model to associate with this recolored image.
     */
    private final ColorModel colors;

    /**
     * Creates a new recolored image with the given colors.
     */
    RecoloredImage(final RenderedImage source, final ColorModel colors) {
        super(source);
        this.colors = colors;
    }

    /**
     * Wraps the given image with its colors ramp scaled between the given bounds. If the given image is
     * already using a color ramp for the given range of values, then that image is returned unchanged.
     *
     * @param  source       the image to recolor.
     * @param  visibleBand  the band to make visible.
     * @param  minimum      the sample value to display with the first color of the color ramp (black in a grayscale image).
     * @param  maximum      the sample value to display with the last color of the color ramp (white in a grayscale image).
     * @return the image with color ramp rescaled between the given bounds. May be the given image returned as-is.
     */
    private static RenderedImage create(RenderedImage source, final int visibleBand, final double minimum, final double maximum) {
        final SampleModel sm = source.getSampleModel();
        final int dataType = sm.getDataType();
        final ColorModel colors = ColorModelFactory.createGrayScale(dataType, sm.getNumBands(), visibleBand, minimum, maximum);
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
     * Implementation of {@link ImageProcessor#stretchColorRamp(RenderedImage, double, double)}.
     * Defined in this class for reducing {@link ImageProcessor} size.
     *
     * @param  source    the image to recolor (may be {@code null}).
     * @param  minimum   the sample value to display with the first color of the color ramp (black in a grayscale image).
     * @param  maximum   the sample value to display with the last color of the color ramp (white in a grayscale image).
     * @return the image with color ramp stretched between the given bounds, or {@code image} unchanged if the operation
     *         can not be applied on the given image.
     */
    static RenderedImage create(final RenderedImage source, final double minimum, final double maximum) {
        if (!(minimum < maximum)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalRange_2, minimum, maximum));
        }
        final int visibleBand = ImageUtilities.getVisibleBand(source);
        if (visibleBand >= 0) {
            return create(source, visibleBand, minimum, maximum);
        }
        return source;
    }

    /**
     * Implementation of {@link ImageProcessor#stretchColorRamp(RenderedImage, Map)}.
     * Defined in this class for reducing {@link ImageProcessor} size.
     * See above-cited public method for the list of modifier keys recognized by this method.
     *
     * @param  processor  the processor to use for computing statistics if needed.
     * @param  source     the image to recolor (may be {@code null}).
     * @param  modifiers  modifiers for narrowing the range of values, or {@code null} if none.
     * @return the image with color ramp stretched between the automatic bounds,
     *         or {@code image} unchanged if the operation can not be applied on the given image.
     */
    static RenderedImage create(final ImageProcessor processor, final RenderedImage source, final Map<String,?> modifiers) {
        RenderedImage statsSource   = source;
        Statistics[]  statsAllBands = null;
        Statistics    statistics    = null;
        double        deviations    = Double.POSITIVE_INFINITY;
        if (modifiers != null) {
            Object value = modifiers.get("MultStdDev");
            if (value instanceof Number) {
                deviations = ((Number) value).doubleValue();
                ArgumentChecks.ensureStrictlyPositive("MultStdDev", deviations);
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
        final int visibleBand = ImageUtilities.getVisibleBand(source);
        if (visibleBand >= 0) {
            if (statistics == null) {
                if (statsAllBands == null) {
                    statsAllBands = processor.getStatistics(statsSource);
                }
                if (statsAllBands != null && visibleBand < statsAllBands.length) {
                    statistics = statsAllBands[visibleBand];
                }
            }
            if (statistics != null) {
                deviations *= statistics.standardDeviation(true);
                final double mean    = statistics.mean();
                final double minimum = Math.max(statistics.minimum(), mean - deviations);
                final double maximum = Math.min(statistics.maximum(), mean + deviations);
                if (minimum < maximum) {
                    return create(source, visibleBand, minimum, maximum);
                }
            }
        }
        return source;
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
        return super.hashCode() + 37*colors.hashCode();
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
