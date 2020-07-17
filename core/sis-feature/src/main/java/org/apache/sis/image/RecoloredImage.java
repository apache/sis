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
import java.awt.Shape;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.math.Statistics;
import org.apache.sis.util.Classes;


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
    RecoloredImage(final RenderedImage source, final ColorModel colors) {
        super(source);
        this.colors = colors;
    }

    /**
     * Returns a recolored image with the given colors. This method may return
     * an existing ancestor if one is found with the specified color model.
     */
    private static RenderedImage create(RenderedImage source, final ColorModel colors) {
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
     * Changes the color ramp of the given image. The given image must use an {@link IndexColorModel}
     * with the same number of colors than the given {@code ARGB} array length.
     *
     * @param  source  the image for which to replace the color model.
     * @param  ARGB    Alpha=Red=Green=Blue codes of new color map.
     * @return image using the given color map.
     *
     * @see ImageProcessor#recolor(RenderedImage, int[])
     */
    static RenderedImage recolor(final RenderedImage source, final int[] ARGB) {
        String expected, actual;                                // To be used in case of error.
        final ColorModel cm = source.getColorModel();
        if (cm instanceof IndexColorModel) {
            final IndexColorModel icm = (IndexColorModel) cm;
            if (icm.getMapSize() == ARGB.length) {
                return create(source, ColorModelFactory.createIndexColorModel(
                                        ImageUtilities.getNumBands(source),
                                        ImageUtilities.getVisibleBand(source), ARGB, -1));
            } else {
                expected = Strings.toIndexed("IndexColorModel", icm.getMapSize());
                actual   = Strings.toIndexed("IndexColorModel", ARGB.length);
            }
        } else {
            expected = "IndexColorModel";
            actual   = Classes.getShortClassName(cm.getClass());
        }
        throw new IllegalArgumentException(Resources.format(Resources.Keys.UnsupportedColorModel_2, expected, actual));
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
