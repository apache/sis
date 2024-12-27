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

import java.util.ArrayList;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.privy.ColorModelBuilder;
import org.apache.sis.coverage.privy.ColorScaleBuilder;
import org.apache.sis.coverage.privy.ColorModelFactory;
import org.apache.sis.coverage.privy.ImageUtilities;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.privy.UnmodifiableArrayList;


/**
 * Colorization algorithm to apply for colorizing a computed image.
 * The {@link #apply(Target)} method is invoked when {@link ImageProcessor} needs a new color model for
 * the computation result. The {@link Target} argument contains information about the image to colorize,
 * in particular the {@link SampleModel} of the computed image. The colorization result is optional,
 * i.e. the {@code apply(Target)} method may return an empty value if it does not support the target.
 * In the latter case the caller will fallback on a default color model, typically a grayscale.
 *
 * <p>Constants or static methods in this interface provide colorizers for common cases.
 * For example {@link #ARGB} interprets image bands as Red, Green, Blue and optionally Alpha channels.
 * Colorizers can be chained with {@link #orElse(Colorizer)} for trying different strategies until one succeeds.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
public interface Colorizer extends Function<Colorizer.Target, Optional<ColorModel>> {
    /**
     * Information about the computed image to colorize.
     * The most important information is the {@link SampleModel}, as the inferred color model must be
     * {@linkplain ColorModel#isCompatibleSampleModel(SampleModel) compatible with the sample model}.
     * A {@code Target} instance may also contain contextual information
     * such as the {@link SampleDimension}s of the target coverage.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.5
     * @since   1.4
     */
    class Target {
        /**
         * Sample model of the computed image to colorize.
         */
        private final SampleModel model;

        /**
         * Description of the bands of the computed image to colorize, or {@code null} if none.
         */
        private final List<SampleDimension> ranges;

        /**
         * The band to colorize if the colorization algorithm uses only one band.
         * Ignored if the colorization uses many bands (e.g. {@link #ARGB}).
         * A negative value means that no visible band has been specified.
         */
        private final int visibleBand;

        /**
         * Creates a new target with the sample model of the image to colorize.
         *
         * @param  model        sample model of the computed image to colorize (mandatory).
         * @param  ranges       description of the bands of the computed image to colorize, or {@code null} if none.
         * @param  visibleBand  the band to colorize if the colorization algorithm uses only one band, or -1 if none.
         */
        public Target(final SampleModel model, final List<SampleDimension> ranges, final int visibleBand) {
            this.model  = Objects.requireNonNull(model);
            this.ranges = (ranges != null) ? List.copyOf(ranges) : null;
            final int numBands = model.getNumBands();
            if (visibleBand < 0) {
                if (numBands == 1) {
                    this.visibleBand = ColorModelFactory.DEFAULT_VISIBLE_BAND;
                    return;
                }
            } else if (visibleBand < numBands) {
                this.visibleBand = visibleBand;
                return;
            }
            this.visibleBand = -1;
        }


        /**
         * Creates a new target with the same sample dimensions and visible band as the given image.
         * This is a convenience constructor for operations producing the same kind of data than an
         * existing image, taken as a template. The list of sample dimensions is fetched from the
         * image property associated to the {@value PlanarImage#SAMPLE_DIMENSIONS_KEY} key.
         *
         * @param model     sample model of the computed image to colorize (mandatory).
         * @param template  the image from which to get the sample dimensions and visible band, or {@code null} if none.
         * @since 1.5
         */
        public Target(SampleModel model, final RenderedImage template) {
            this.model  = Objects.requireNonNull(model);
            visibleBand = ImageUtilities.getVisibleBand(template);
            if (template != null) {
                final Object value = template.getProperty(PlanarImage.SAMPLE_DIMENSIONS_KEY);
                if (value instanceof SampleDimension[]) {
                    ranges = UnmodifiableArrayList.wrap((SampleDimension[]) value);
                    return;
                }
            }
            ranges = null;
        }

        /**
         * Returns the sample model of the computed image to colorize.
         * The color model created by {@link #apply(Target)}
         * must be compatible with this sample model.
         *
         * @return computed image sample model (never null).
         * @see ColorModel#isCompatibleSampleModel(SampleModel)
         */
        public SampleModel getSampleModel() {
            return model;
        }

        /**
         * Returns a description of the bands of the image to colorize.
         * This information may be present if the image operation is invoked by a
         * {@link org.apache.sis.coverage.grid.GridCoverageProcessor} operation,
         * or if the source image contains the {@value PlanarImage#SAMPLE_DIMENSIONS_KEY} property
         * Note that in the latter case, the list may contain null elements if this information is
         * missing in some bands.
         *
         * @return description of the bands of the image to colorize.
         * @see org.apache.sis.coverage.grid.GridCoverage#getSampleDimensions()
         */
        public Optional<List<SampleDimension>> getRanges() {
            return Optional.ofNullable(ranges);
        }

        /**
         * Returns the band to colorize if the colorization algorithm uses only one band.
         * The value is always positive and less than the number of bands of the sample model.
         * This information is ignored if the colorization uses many bands (e.g. {@link #ARGB}).
         *
         * @return the band to colorize if the colorization algorithm uses only one band.
         * @see org.apache.sis.coverage.grid.ImageRenderer#setVisibleBand(int)
         */
        public OptionalInt getVisibleBand() {
            return (visibleBand >= 0) ? OptionalInt.of(visibleBand) : OptionalInt.empty();
        }

        /**
         * Returns {@code true} if {@code orElse(…)} should not try alternative colorizers.
         * This is used only for {@link Visualization} operation, which is a special case
         * because it merges 3 operations in a single one.
         *
         * @return whether {@link #orElse(Colorizer)} should not try alternative.
         */
        boolean isConsumed() {
            return false;
        }
    }

    /**
     * RGB(A) color model for images storing 8 bits integer on 3 or 4 bands.
     * The color model is <abbr>RGB</abbr> for image having 3 bands, or <abbr>ARGB</abbr> for images having 4 bands.
     * In the latter case, the color components are considered <em>not</em> premultiplied by the alpha value.
     */
    Colorizer ARGB = (target) -> Optional.ofNullable(new ColorModelBuilder().createRGB(target.getSampleModel()));

    /**
     * Creates a colorizer which will interpolate the given colors in the given range of values.
     * When the image data type is 8 or 16 bits integer, this colorizer creates {@link IndexColorModel} instances.
     * For other kinds of data type such as floating points,
     * this colorizer creates a non-standard (and potentially slow) color model.
     *
     * <h4>Limitations</h4>
     * In current implementation, the non-standard color model ignores the specified colors.
     * If the image data type is not 8 or 16 bits integer, the colors are always grayscale.
     *
     * @param  lower   the minimum sample value, inclusive.
     * @param  upper   the maximum sample value, exclusive.
     * @param  colors  the colors to use for the specified range of sample values.
     * @return a colorizer which will interpolate the given colors in the given range of values.
     */
    public static Colorizer forRange(final double lower, final double upper, final Color... colors) {
        ArgumentChecks.ensureNonEmpty("colors", colors);
        return forRanges(Map.of(new NumberRange<>(Double.class, lower, true, upper, false), colors));
    }

    /**
     * Creates a colorizer which will interpolate colors in multiple ranges of values.
     * The range of pixel values are specified by {@link NumberRange} elements,
     * and the colors to interpolate in each range are specified by {@code Color[]} arrays.
     * Empty arrays (i.e. no color) are interpreted as an explicit request for full transparency.
     *
     * <p>When the image data type is 8 or 16 bits integer,
     * this colorizer creates {@link IndexColorModel} instances.
     * For other kinds of data type such as floating points,
     * this colorizer creates a non-standard (and potentially slow) color model.</p>
     *
     * <h4>Default colors</h4>
     * The given {@code colors} map can associate to some keys a null or an empty color arrays.
     * An empty array (i.e. no color) is interpreted as an explicit request for transparency.
     * But null values are interpreted as unspecified colors,
     * in which case the defaults are implementation dependent.
     * In current implementation, the defaults are:
     *
     * <ul>
     *   <li>If this colorizer is used for {@linkplain ImageProcessor#visualize(RenderedImage) visualization},
     *       try to keep the existing colors of the image to visualize.</li>
     *   <li>Otherwise if the range minimum and maximum values are not equal, default to grayscale colors.</li>
     *   <li>Otherwise default to a fully transparent color.</li>
     * </ul>
     *
     * <h4>Limitations</h4>
     * In current implementation, the non-standard color model ignores the specified colors.
     * If the image data type is not 8 or 16 bits integer, the colors are always grayscale.
     *
     * @param  colors  the colors to use for the specified range of sample values.
     * @return a colorizer which will interpolate the given colors in the given range of values.
     *
     * @see ImageProcessor#visualize(RenderedImage)
     */
    public static Colorizer forRanges(final Map<NumberRange<?>,Color[]> colors) {
        final var list = new ArrayList<Map.Entry<NumberRange<?>,Color[]>>(colors.size());
        for (final Map.Entry<NumberRange<?>,Color[]> entry : colors.entrySet()) {
            var range = entry.getKey();
            var value = entry.getValue();
            if (value != null) {
                value = value.clone();
            }
            list.add(new SimpleImmutableEntry<>(range, value));
        }
        final var entries = List.copyOf(list);
        final var factory = ColorModelFactory.piecewise(entries);
        return (target) -> {
            if (target instanceof Visualization.Target) {
                ((Visualization.Target) target).rangeColors = entries;
            } else {
                final OptionalInt visibleBand = target.getVisibleBand();
                if (!visibleBand.isEmpty()) {
                    final SampleModel model = target.getSampleModel();
                    final int numBands = model.getNumBands();
                    return Optional.ofNullable(factory.createColorModel(model.getDataType(), numBands, visibleBand.getAsInt()));
                }
            }
            return Optional.empty();
        };
    }

    /**
     * Creates a colorizer which will interpolate colors in ranges identified by categories.
     * This colorizer is similar to {@link #forRanges(Map)} (with the same limitations) except that instead of mapping
     * colors to predefined ranges of pixel values, it maps colors to {@linkplain Category#getName() category names},
     * {@linkplain org.apache.sis.measure.MeasurementRange#unit() units of measurement} or other properties.
     * The given function provides a way to colorize images without knowing in advance the numerical values of pixels.
     * For example, instead of specifying <q>pixel value 0 is blue, 1 is green, 2 is yellow</q>,
     * the given function allows to specify <q>Lakes are blue, Forests are green, Sand is yellow</q>.
     *
     * <h4>Default colors</h4>
     * The given function can return {@code null} or empty color arrays for some categories.
     * An empty array (i.e. no color) is interpreted as an explicit request for transparency.
     * But null arrays are interpreted as unrecognized category,
     * in which case the defaults are implementation dependent.
     * In current implementation, the defaults are:
     *
     * <ul>
     *   <li>If this colorizer is used for {@linkplain ImageProcessor#visualize(RenderedImage) visualization},
     *       try to keep the existing colors of the image to visualize.</li>
     *   <li>Otherwise if all categories are unrecognized, then the colorizer returns an empty value.</li>
     *   <li>Otherwise, {@linkplain Category#isQuantitative() quantitative} categories default to grayscale colors.</li>
     *   <li>Otherwise qualitative categories default to a fully transparent color.</li>
     * </ul>
     *
     * <h4>Conditions</h4>
     * This colorizer is used when {@link Target#getRanges()} provides a non-empty value.
     * That value is typically fetched from the {@value PlanarImage#SAMPLE_DIMENSIONS_KEY} image property,
     * which is itself typically fetched from {@link org.apache.sis.coverage.grid.GridCoverage#getSampleDimensions()}.
     * If no sample dimension information is available,
     * or if the specified function did not returned at non-null value for at least one category,
     * then this colorizer does not build a color model.
     * A fallback can be specified with {@link #orElse(Colorizer)}.
     *
     * @param  colors  colors to use for arbitrary categories of sample values.
     * @return a colorizer which will apply colors determined by the {@link Category} of sample values.
     *
     * @see ImageProcessor#visualize(RenderedImage)
     */
    public static Colorizer forCategories(final Function<Category,Color[]> colors) {
        ArgumentChecks.ensureNonNull("colors", colors);
        return (target) -> {
            if (target instanceof Visualization.Target) {
                ((Visualization.Target) target).categoryColors = colors;
            } else {
                final int visibleBand = target.getVisibleBand().orElse(-1);
                if (visibleBand >= 0) {
                    final List<SampleDimension> ranges = target.getRanges().orElse(null);
                    if (visibleBand < ranges.size()) {
                        final SampleModel model = target.getSampleModel();
                        final var c = new ColorScaleBuilder(colors, null, false);
                        if (c.initialize(model, ranges.get(visibleBand))) {
                            return Optional.ofNullable(c.createColorModel(model, model.getNumBands(), visibleBand));
                        }
                    }
                }
            }
            return Optional.empty();
        };
    }

    /**
     * Creates a colorizer which will use the specified color model instance if compatible with the target.
     *
     * @param  colors  the color model instance to use.
     * @return a colorizer which will try to apply the specified color model <i>as-is</i>.
     */
    public static Colorizer forInstance(final ColorModel colors) {
        ArgumentChecks.ensureNonNull("colors", colors);
        return (target) -> colors.isCompatibleSampleModel(target.getSampleModel()) ? Optional.of(colors) : Optional.empty();
    }

    /**
     * Returns the color model to use for an image having the given sample model.
     * If this function does not support the creation of a color model for the given sample model,
     * then an empty value is returned. In the latter case, caller will typically fallback on grayscale.
     * Otherwise if a non-empty value is returned, then that color model shall be
     * {@linkplain ColorModel#isCompatibleSampleModel(SampleModel) compatible}
     * with the {@linkplain Target#getSampleModel() target sample model}.
     *
     * @param  model  the sample model of the image for which to create a color model.
     * @return the color model to use for the specified sample model.
     */
    @Override
    Optional<ColorModel> apply(Target model);

    /**
     * Returns a new colorizer which will apply the specified alternative
     * if this colorizer cannot infer a color model.
     *
     * @param  alternative  the alternative strategy for creating a color model.
     * @return a new colorizer which will attempt to apply {@code this} first,
     *         then fallback on the specified alternative this colorizer did not produced a result.
     */
    default Colorizer orElse(final Colorizer alternative) {
        ArgumentChecks.ensureNonNull("alternative", alternative);
        return (target) -> {
            var result = apply(target);
            if (result.isEmpty() && !target.isConsumed()) {
                result = alternative.apply(target);
            }
            return result;
        };
    }
}
