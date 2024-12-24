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
package org.apache.sis.coverage.privy;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collection;
import java.util.function.Function;
import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import org.opengis.util.InternationalString;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.image.DataType;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Helper classes for allowing an image to be colorized, by building an {@link IndexColorModel} if needed.
 * Image created by this class are suitable for visualization purposes but generally not for computations.
 * Usage:
 *
 * <ol>
 *   <li>Create a new {@link ColorScaleBuilder} instance.</li>
 *   <li>Invoke one of {@code initialize(…)} methods.</li>
 *   <li>Invoke {@link #createColorModel(int, int, int)}.</li>
 *   <li>Invoke {@link #getSampleToIndexValues()} if this auxiliary information is useful.</li>
 *   <li>Discards {@code ColorScaleBuilder}. Each instance shall be used only once.</li>
 * </ol>
 *
 * There is no {@code initialize(Raster)} or {@code initialize(RenderedImage)} method because if those methods
 * were present, users may expect them to iterate over sample values for finding minimum and maximum values.
 * We do not perform such iteration because they are potentially costly and give unstable results:
 * the resulting color model varies from image to image, which is confusing when many images exist
 * for the same product at different times or at different depths.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see ColorModelType
 * @see ColorModelFactory#createColorModel(int, int, int)
 */
public final class ColorScaleBuilder {
    /**
     * A color model constant set to {@code null}, used for identifying code that explicitly set the
     * color model to {@code null}. It may happen when no {@code initialize(…)} method can be applied.
     */
    public static final ColorModel NULL_COLOR_MODEL = null;

    /**
     * Names to use for the synthetic categories and sample dimensions created for visualization purposes.
     * Transparent pixel is usually 0 and opaque pixels are in the range 1 to {@value #MAX_VALUE} inclusive.
     *
     * <p>For safety, we use names that are different than the default "No data" and "Data" names assigned by
     * {@link SampleDimension.Builder}. The "[No] data" default names are often used by formats that are poor
     * in metadata, for example ASCII Grid. If we were using the same names, a {@link #colors} function could
     * confuse synthetic categories with "real" categories with uninformative name, and consequently apply
     * wrong colors.</p>
     */
    private static final InternationalString
            TRANSPARENT = Vocabulary.formatInternational(Vocabulary.Keys.Transparent),
            COLOR_INDEX = Vocabulary.formatInternational(Vocabulary.Keys.ColorIndex),
            VISUAL      = Vocabulary.formatInternational(Vocabulary.Keys.Visual);

    /**
     * Maximal index value which can be used with a 8 bits {@link IndexColorModel}, inclusive.
     * Sample values must be in that range for enabling the use of {@link #TYPE_COMPACT}.
     */
    private static final int MAX_VALUE = 0xFF;

    /**
     * The type resulting from sample values conversion in compact mode.
     * The value is {@link DataBuffer#TYPE_BYTE}.
     */
    public static final int TYPE_COMPACT = DataBuffer.TYPE_BYTE;

    /**
     * Whether to rescale the range of sample values to the {@link #TYPE_COMPACT} range.
     */
    private final boolean compact;

    /**
     * Applies a gray scale to quantitative category and transparent colors to qualitative categories.
     * This is a possible argument for the {@link #ColorScaleBuilder(Function, ColorModel, boolean)} constructor.
     */
    public static final Function<Category,Color[]> GRAYSCALE =
            (category) -> category.isQuantitative() ? new Color[] {Color.BLACK, Color.WHITE} : null;

    /**
     * The colors to use for each category. Never {@code null} (default value is grayscale).
     * The function may return {@code null}, which means that the category is not recognized.
     * If no category is recognized, no {@link ColorModel} will be built using that function.
     * An empty array is interpreted as a color specified as transparent.
     *
     * <h4>Default value</h4>
     * Default value is {@link #GRAYSCALE}.
     * If the function returns {@code null} for an unrecognized category,
     * the default colors for that category will be the same as {@link #GRAYSCALE}:
     * grayscale for quantitative categories and transparent for qualitative categories.
     */
    private final Function<Category,Color[]> colors;

    /**
     * The colors to use for each range of values in the source image.
     * This array is initially null and created by an {@code initialize(…)} method.
     * After initialization, this array shall not contain null element.
     */
    private ColorsForRange[] entries;

    /**
     * The sample dimension for values before conversion, or {@code null} if unspecified.
     * This object describes the range of values found in source image.
     * They are not necessarily the range of values in the colorized image.
     */
    private SampleDimension source;

    /**
     * The sample dimension for values after conversion, or {@code null} if not yet computed.
     * May be the same as {@link #source} or {@code source.forConvertedValues(true)} if one
     * of those values is suitable, or a new sample dimension created by {@link #compact()}.
     *
     * <p>This sample dimension should not be returned to the user because it may not contain meaningful values.
     * For example, it may contain an "artificial" transfer function for computing a {@link MathTransform1D} from
     * source range to the [0 … 255] value range.</p>
     *
     * @see #getSampleToIndexValues()
     */
    private SampleDimension target;

    /**
     * Default range of values to use if not explicitly specified by a {@link Category}.
     */
    private NumberRange<?> defaultRange;

    /**
     * Colors to inherit if a range of values is undefined, or {@code null} if none.
     * This field should be non-null only when this builder is used for styling an image before visualization.
     * This field should be null when this builder is created for creating a new image because the meaning of
     * pixel values may be completely different (i.e. meaning of {@linkplain #source} may not be applicable).
     *
     * @see ColorsForRange#isUndefined()
     */
    private final ColorModel inheritedColors;

    /**
     * Creates a new colorizer which will apply colors on the given ranges of values in source image.
     * The {@code ColorScaleBuilder} is considered initialized after this constructor;
     * callers shall <strong>not</strong> invoke an {@code initialize(…)} method.
     *
     * <p>The {@code colors} map shall not be null or empty but may contain {@code null} values.
     * Null values default to a fully transparent color when the range contains a single value,
     * and to grayscale colors otherwise, unless {@code inherited} is non-null.
     * Empty arrays of colors are interpreted as explicitly transparent.</p>
     *
     * <p>This constructor creates a builder in compact mode, unless all specified ranges
     * already fit in {@link #TYPE_COMPACT} range.</p>
     *
     * @param  colors     the colors to use for each range of values in source image.
     * @param  inherited  the colors to use as fallback if some ranges have undefined colors, or {@code null}.
     *                    Should be non-null only for styling an exiting image before visualization.
     */
    public ColorScaleBuilder(final Collection<Map.Entry<NumberRange<?>,Color[]>> colors, final ColorModel inherited) {
        entries = ColorsForRange.list(colors, inherited);
        inheritedColors = inherited;
        this.colors = GRAYSCALE;
        for (final Map.Entry<NumberRange<?>,Color[]> entry : colors) {
            final NumberRange<?> range = entry.getKey();
            if (range.getMinDouble() < 0 || range.getMaxDouble() >= MAX_VALUE + 1) {
                compact = true;
                return;
            }
        }
        compact = false;
    }

    /**
     * Creates a new colorizer which will use the given function for determining the colors to apply.
     * Callers need to invoke an {@code initialize(…)} method after this constructor.
     *
     * <p>The {@code inherited} parameter is non-null when this builder is created for styling
     * an existing image before visualization. This parameter should be null when this builder
     * is created for creating a new image, even when that new image is derived from a source,
     * because the meaning of pixel values may be completely different.</p>
     *
     * @param  colors     the colors to use for each category, or {@code null} for default.
     *                    The function may return {@code null} for unrecognized categories.
     * @param  inherited  the colors to use as fallback for unrecognized categories, or {@code null}.
     *                    Should be non-null only for styling an exiting image before visualization.
     * @param  compact    Whether to rescale the range of sample values to the {@link #TYPE_COMPACT} range.
     */
    public ColorScaleBuilder(final Function<Category,Color[]> colors, final ColorModel inherited, final boolean compact) {
        this.colors = (colors != null) ? colors : GRAYSCALE;
        inheritedColors = inherited;
        this.compact = compact;
    }

    /**
     * Verifies whether the {@link #entries} value is defined.
     *
     * @param  initialized  the expected initialization state.
     */
    private void checkInitializationStatus(final boolean initialized) {
        if ((entries != null) != initialized) {
            throw new IllegalStateException(Errors.format(
                    initialized ? Errors.Keys.Uninitialized_1 : Errors.Keys.AlreadyInitialized_1, getClass()));
        }
    }

    /**
     * Returns {@code true} if the given range is already the [0 … 255] range.
     *
     * @see #TYPE_COMPACT
     * @see #compact()
     */
    private static boolean isAlreadyScaled(final NumberRange<?> range) {
        return range.getMinDouble(true) == 0 && range.getMaxDouble(true) == MAX_VALUE;
    }

    /**
     * Uses the given sample dimension for mapping range of values to colors. For each category in
     * the sample dimension, colors will be determined by a call to {@code colors.apply(category)}
     * where {@code colors} is the function specified at construction time.
     *
     * @param  model   the sample model used with the data, or {@code null} if unknown.
     * @param  source  description of range of values in the source image, or {@code null}.
     * @return {@code true} on success, or {@code false} if no range of values has been found.
     * @throws IllegalStateException if a sample dimension is already defined on this colorizer.
     */
    public boolean initialize(final SampleModel model, final SampleDimension source) {
        checkInitializationStatus(false);
        if (source != null) {
            this.source = source;
            final List<Category> categories = source.getCategories();
            if (!categories.isEmpty()) {
                boolean isUndefined = true;
                boolean missingNodata = true;

                @SuppressWarnings("LocalVariableHidesMemberVariable")
                ColorsForRange[] entries = new ColorsForRange[categories.size()];
                for (int i=0; i<entries.length; i++) {
                    final var range = new ColorsForRange(categories.get(i), colors, inheritedColors);
                    isUndefined &= range.isUndefined();
                    missingNodata &= range.isData;
                    entries[i] = range;
                }
                if (!isUndefined) {
                    /*
                     * If the model uses floating point values and there is no "no data" category, add one.
                     * We force a "no data" category because floating point values may be NaN.
                     */
                    if (missingNodata && (model == null || !ImageUtilities.isIntegerType(model))) {
                        final int count = entries.length;
                        entries = Arrays.copyOf(entries, count + 1);
                        entries[count] = new ColorsForRange(TRANSPARENT,
                                NumberRange.create(Float.class, Float.NaN), null, false, inheritedColors);
                    }
                    // Leave `target` to null. It will be computed by `compact()` if needed.
                    this.entries = entries;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Applies colors on the range of values of a raster using given sample model. The 0 index will be reserved
     * for NaN value, and indices in the [1 … 255] range will be mapped to the range of sample values that can
     * be stored in the specified band.
     *
     * @param  source  sample model of raster to be colored, or {@code null}.
     * @param  band    raster band to be colored.
     * @return {@code true} on success, or {@code false} if no range of values has been found.
     * @throws IllegalStateException if a sample dimension is already defined on this colorizer.
     */
    public boolean initialize(final SampleModel source, final int band) {
        checkInitializationStatus(false);
        if (ImageUtilities.isIntegerType(source)) {
            long minimum = 0;
            long maximum = Numerics.bitmask(source.getSampleSize(band)) - 1;
            if (!ImageUtilities.isUnsignedType(source)) {
                maximum >>>= 1;
                minimum = ~maximum;
            }
            initialize(minimum, maximum);
            return true;
        }
        return false;
    }

    /**
     * Uses the given color model for mapping range of values to new colors. The colors in the given color model
     * are ignored (because they will be replaced by colors specified by this {@code ColorScaleBuilder});
     * only the range of values will be fetched, if such range exists.
     *
     * @param  source  the color model from which to get a range of values, or {@code null}.
     * @return {@code true} on success, or {@code false} if no range of values has been found.
     * @throws IllegalStateException if a sample dimension is already defined on this colorizer.
     */
    public boolean initialize(final ColorModel source) {
        checkInitializationStatus(false);
        if (source != null) {
            final ColorSpace cs = source.getColorSpace();
            if (cs instanceof ScaledColorSpace) {
                final ScaledColorSpace scs = (ScaledColorSpace) cs;
                initialize(scs.offset, scs.maximum, source.getTransferType());
                return true;
            }
            /*
             * If the color model uses integer type, compute the maximal value based on the number of bits.
             * The main use case is `IndexColorModel` with values on 16 bits but with a color ramp that does
             * not exploit the full range allowed by 16 bits.
             */
            if (ImageUtilities.isIntegerType(source.getTransferType())) {
                long maximum = Numerics.bitmask(source.getPixelSize()) - 1;
                long minimum = 0;
                if (source instanceof IndexColorModel) {
                    final IndexColorModel indexed = (IndexColorModel) source;
                    int t = indexed.getMapSize();
                    if (t <= maximum) maximum = t - 1L;     // Inclusive.
                    t = indexed.getTransparentPixel();
                    if (t == 0) minimum = 1;
                }
                if (minimum < maximum) {
                    initialize(minimum, maximum);
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * Do not provide methods taking Raster or RenderedImage argument.
     * See class javadoc for rational.
     */

    /**
     * Applies colors on the given range of values.
     * In compact mode, the 0 index will be reserved for NaN value
     * and indices in the [1 … 255] will be mapped to the given range.
     *
     * <p>This method is typically used as a last resort fallback when all other {@code initialize(…)}
     * methods failed or cannot be applied. This method assumes that no {@link Category} information
     * is available.</p>
     *
     * @param  minimum   minimum value, inclusive.
     * @param  maximum   maximum value, inclusive.
     * @param  dataType  type of sample values.
     * @throws IllegalStateException if a sample dimension is already defined on this colorizer.
     */
    public void initialize(final double minimum, final double maximum, final int dataType) {
        checkInitializationStatus(false);
        ArgumentChecks.ensureFinite("minimum", minimum);
        ArgumentChecks.ensureFinite("maximum", maximum);
        if (ImageUtilities.isIntegerType(dataType)) {
            defaultRange = NumberRange.create(Math.round(minimum), true, Math.round(maximum), true);
        } else {
            defaultRange = NumberRange.create(minimum, true, maximum, true);
        }
        applyDefaultRange();
    }

    /**
     * Applies colors on the given range of values.
     * This method does the same work as {@link #initialize(double, double, int)},
     * but is preferred to the latter when the sample values are known to be integer values.
     *
     * @param  minimum  minimum value, inclusive.
     * @param  maximum  maximum value, inclusive.
     * @throws IllegalStateException if a sample dimension is already defined on this colorizer.
     */
    public void initialize(final long minimum, final long maximum) {
        checkInitializationStatus(false);
        defaultRange = NumberRange.create(minimum, true, maximum, true);
        applyDefaultRange();
    }

    /**
     * Initializes this builder with the {@link #defaultRange} value.
     */
    private void applyDefaultRange() {
        final var builder = new SampleDimension.Builder().setName(VISUAL);
        if (compact) {
            var samples = NumberRange.create(1, true, MAX_VALUE, true);
            builder.setBackground(TRANSPARENT, 0).addQuantitative(COLOR_INDEX, samples, defaultRange);
        } else {
            builder.addQuantitative(COLOR_INDEX, defaultRange, identity(), null);
        }
        target = builder.build();
        /*
         * We created a synthetic `SampleDimension` with the specified range of values.
         * The `source` is recreated as a matter of principle, but will not be used by
         * `compact()` because `target` will take precedence.
         */
        source = target.forConvertedValues(true);
        final List<Category> categories = target.getCategories();

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final ColorsForRange[] entries = new ColorsForRange[categories.size()];
        for (int i=0; i<entries.length; i++) {
            final Category category = categories.get(i);
            final var range = new ColorsForRange(category.forConvertedValues(true), colors, inheritedColors);
            range.sampleRange = category.getSampleRange();
            entries[i] = range;
        }
        this.entries = entries;
    }

    /**
     * Potentially rescales the range of values of the main category for the given color model.
     * This method can be invoked when the color model may use a range of values different than the range
     * specified by categories. It may happen if the color ramp associated to the quantitative category has
     * been stretched dynamically using a "recolor" operation. We want to preserve that user customization,
     * but we have no explicit information about which category to modify. This method does an heuristic
     * choice based on the category having the largest intersection with the color model value range.
     *
     * <p>An {@code initialize(…)} method must have been invoked successfully before this method can be invoked.</p>
     *
     * @param  original  original color model of image for which a new color map is built, or {@code null} if none.
     * @throws IllegalStateException if {@code initialize(…)} has not been invoked.
     */
    public void rescaleMainRange(final ColorModel original) {
        checkInitializationStatus(true);
        if (original != null) {
            final ColorSpace cs = original.getColorSpace();
            if (cs instanceof ScaledColorSpace) {
                final ScaledColorSpace scs = (ScaledColorSpace) cs;
                final double minimum = scs.offset;
                final double maximum = scs.maximum;
                ColorsForRange widest = null;
                double widestSpan = 0;
                for (final ColorsForRange entry : entries) {
                    final double span = Math.min(entry.sampleRange.getMaxDouble(), maximum)
                                      - Math.max(entry.sampleRange.getMinDouble(), minimum);
                    if (span > widestSpan) {
                        widestSpan = span;
                        widest = entry;
                    }
                }
                defaultRange = NumberRange.create(minimum, true, maximum, false);
                if (widest != null && widestSpan != widest.sampleRange.getSpan()) {
                    widest.sampleRange = defaultRange;
                    target = null;      // For recomputing the transfer function later.
                }
            }
        }
    }

    /**
     * Modifies the sample value ranges to make them fit in valid ranges for an {@link IndexColorModel}.
     * The {@link SampleDimension#getSampleRange()} is constrained to range [0 … 255] inclusive.
     * The {@link SampleDimension#getTransferFunction()} returns the conversion from original ranges
     * to ranges of pixel values in the colorized image.
     *
     * <p>There are two outputs: the {@link #target} sample dimension, and modifications done in-place in the
     * {@link #entries} array. For each {@link ColorsForRange} instance, the {@link ColorsForRange#sampleRange}
     * range is replaced by range of indexed colors. In addition {@code entries} elements may be reordered.</p>
     *
     * <p>If {@lini #entries} has been built from a sample dimension, that {@link SampleDimension} is specified
     * in the {@link #source} field. This is used only for providing a better name to the sample dimension.</p>
     */
    private void compact() {
        if (target != null) {
            return;
        }
        /*
         * If a source SampleDimension has been specified, verify if it provides a transfer function that we can
         * use directly. If this is the case, use the existing transfer function instead of inventing our own.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        ColorsForRange[] entries = this.entries;
reuse:  if (source != null) {
            target = source.forConvertedValues(false);
            if (target.getSampleRange().filter(ColorScaleBuilder::isAlreadyScaled).isPresent()) {
                /*
                 * If we enter in this block, all sample values are already in the [0 … 255] range.
                 * If in addition there is no conversion to apply, then there is nothing to do.
                 */
                if (target == source) {
                    return;
                }
                /*
                 * We will need to replace ranges specified in the source `SampleDimensions` by ranges used in the
                 * colorized images. Prepare in advance a `mapper` with all replacements that we know about.
                 */
                final Map<NumberRange<?>,NumberRange<?>> mapper = new HashMap<>();
                for (final Category category : target.getCategories()) {
                    if (mapper.put(category.forConvertedValues(true).getSampleRange(), category.getSampleRange()) != null) {
                        break reuse;        // Duplicated range of values in source SampleDimensions (should not happen).
                    }
                }
                /*
                 * Do the replacements in a temporary `ranges` array before to write in the `entries` array
                 * because `entries` changes must be a "all or nothing" operation. We allow each range to be
                 * used as most once.
                 */
                final NumberRange<?>[] ranges = new NumberRange<?>[entries.length];
                for (int i=0; i<entries.length; i++) {
                    if ((ranges[i] = mapper.remove(entries[i].sampleRange)) == null) {
                        break reuse;            // Range not found or used twice.
                    }
                }
                for (int i=0; i<entries.length; i++) {
                    entries[i].sampleRange = ranges[i];
                }
                return;
            }
        }
        /*
         * If we reach this point, `source` sample dimensions were not specified or cannot be used for
         * getting a transfer function to the [0 … 255] range of values. We will need to create our own.
         * First, sort the entries for having transparent colors first.
         */
        Arrays.sort(entries);                               // Move transparent colors in first positions.
        double span  = 0;                                   // Total span of all non-NaN ranges.
        int lower    = 0;                                   // First available index in the [0 … 255] range.
        int deferred = 0;                                   // Number of entries deferred to next loop.
        int count    = entries.length;                      // Total number of valid entries.
        NumberRange<?> themes = null;                       // The range of values in a thematic map.
        final var mapper  = new HashMap<NumberRange<Integer>, ColorsForRange>();
        final var builder = new SampleDimension.Builder();
        /*
         * We will use the byte values range [0 … 255] with 0 reserved in priority for the most transparent pixels.
         * The first loop below processes NaN values, which are usually the ones associated to transparent pixels.
         * The second loop (from 0 to `deferred`) will process everything else.
         */
        for (int i=0; i<count; i++) {
            final ColorsForRange entry = entries[i];
            NumberRange<?> sourceRange = entry.sampleRange;
            if (!entry.isData) {
                if (lower >= MAX_VALUE) {
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.TooManyQualitatives));
                }
                final NumberRange<Integer> targetRange = NumberRange.create(lower, true, ++lower, false);
                if (mapper.put(targetRange, entry) == null) {
                    final double value = sourceRange.getMinDouble();
                    /*
                     * In the usual case where we have a mix of quantitative and qualitative categories,
                     * the qualitative ones (typically "no data" categories) are associated to NaN.
                     * Values are real only if all categories are qualitatives (e.g. a thematic map).
                     * In such case we will create pseudo-quantitative categories for the purpose of
                     * computing a transfer function, but those categories should not be returned to user.
                     */
                    if (Double.isNaN(value)) {
                        builder.mapQualitative(entry.name(), targetRange, (float) value);
                    } else {
                        if (value == entry.sampleRange.getMaxDouble()) {
                            sourceRange = NumberRange.create(
                                    Math.min(value - 0.5, Math.nextDown(value)), true,
                                    Math.max(value + 0.5, Math.nextUp(value)), false);
                        }
                        builder.addQuantitative(entry.name(), targetRange, sourceRange);
                        themes = (themes != null) ? themes.unionAny(sourceRange) : sourceRange;
                    }
                }
            } else {
                final double s = sourceRange.getSpan();
                if (s > 0) {
                    // Range of real values: defer processing to next loop.
                    span += s;
                    System.arraycopy(entries, deferred, entries, deferred + 1, i - deferred);
                    entries[deferred++] = entry;
                } else {
                    // Invalid range: silently discard.
                    System.arraycopy(entries, i+1, entries, i, --count - i);
                    entries[count] = null;
                }
            }
        }
        /*
         * Following block is executed only if the sample dimension defines only qualitative categories.
         * This is the case of thematic (or classification) map. It may also happen because the coverage
         * defined only a "no data" value with no information about the "real" values. In such case we
         * generate an artificial quantitative category for mapping all remaining values to [0…255] range.
         * The actual category creation happen in the loop after this block.
         */
        if (deferred == 0 && themes != null) {
            if (defaultRange == null) {
                defaultRange = NumberRange.create(0, true, Short.MAX_VALUE + 1, false);
            }
            // Following loop will usually be executed only once.
            for (final NumberRange<?> sourceRange : defaultRange.subtractAny(themes)) {
                span += sourceRange.getSpan();
                final ColorsForRange[] tmp = Arrays.copyOf(entries, ++count);
                System.arraycopy(entries, deferred, tmp, ++deferred, count - deferred);
                tmp[deferred-1] = new ColorsForRange(null, sourceRange, null, true, null);
                entries = tmp;
            }
        }
        this.entries = entries = ArraysExt.resize(entries, count);      // Should be a no-op most of the times.
        /*
         * Above loop mapped all NaN values. Now map the real values. Usually, there is exactly one entry taking
         * all remaining values in the [0 … 255] range, but code below is tolerant to arbitrary number of ranges.
         */
        final int base = lower;
        final double toIndexRange = (MAX_VALUE + 1 - base) / span;
        span = 0;
        for (int i=0; i<deferred; i++) {
            final ColorsForRange entry = entries[i];
            span += entry.sampleRange.getSpan();
            final int upper = Math.toIntExact(Math.round(span * toIndexRange) + base);
            if (upper <= lower) {
                // May happen if too many qualitative categories have been added by previous loop.
                throw new IllegalArgumentException(Resources.format(Resources.Keys.TooManyQualitatives));
            }
            final NumberRange<Integer> samples = NumberRange.create(lower, true, upper, false);
            if (mapper.put(samples, entry) == null) {
                builder.addQuantitative(entry.name(), samples, entry.sampleRange);
            }
            lower = upper;
        }
        /*
         * At this point we created a `Category` instance for each given `ColorsForRange`.
         * Update the given `ColorsForRange` instances with new range values.
         */
        if (source != null) {
            builder.setName(source.getName());
        } else {
            builder.setName(VISUAL);
        }
        target = builder.build();
        for (final Category category : target.getCategories()) {
            final NumberRange<?> packed = category.getSampleRange();
            mapper.get(packed).sampleRange = packed;
            // A NullPointerException on above line would be a bug in our construction of `mapper`.
        }
    }

    /**
     * Returns a color model with colors interpolated in the ranges of values determined by constructors.
     * This method builds up the color model from each set of colors associated to ranges in the given array.
     * Returned instances of {@link ColorModel} are shared among all callers in the running virtual machine.
     *
     * <h4>Compact mode</h4>
     * If the {@code compact} argument given to the constructor was {@code true},
     * then the color model has colors interpolated in the [0 … 255] range of values.
     * Conversions from range specified at construction time to the [0 … 255] range is
     * given by {@link #getSampleToIndexValues()}. Images using this color model shall
     * use a {@link DataBuffer} of type {@link #TYPE_COMPACT}.
     *
     * @param  dataType     the type of data elements used for storing sample values.
     * @param  numBands     the number of bands for the color model (usually 1). The returned color model will render only
     *                      the {@code visibleBand} and ignore the others, but the existence of all {@code numBands} will
     *                      be at least tolerated. Supplemental bands, even invisible, are useful for processing.
     * @param  visibleBand  the band to be made visible (usually 0). All other bands, if any, will be ignored.
     * @return a color model suitable for {@link java.awt.image.RenderedImage} objects with values in the given ranges.
     */
    public ColorModel createColorModel(final DataType dataType, final int numBands, final int visibleBand) {
        checkInitializationStatus(true);
        int typeCode = dataType.toDataBufferType();
        ArgumentChecks.ensureStrictlyPositive("numBands", numBands);
        ArgumentChecks.ensureBetween("visibleBand", 0, numBands - 1, visibleBand);
        if (compact) {
            compact();
            typeCode = TYPE_COMPACT;
        }
        return ColorModelFactory.createPiecewise(typeCode, numBands, visibleBand, entries);
    }

    /**
     * Returns a color model to use in a rendered image together with the given sample model.
     * This method delegates to {@link #createColorModel(DataType, int, int)} after having
     * inferred the data type from the given sample model.
     *
     * @todo Current implementation does not verify that the color model is compatible
     *       with the sample model. We should add that verification.
     *
     * @param  target       the sample model for which to create a color model.
     * @param  numBands     the number of bands for the color model (usually 1).
     * @param  visibleBand  the band to be made visible (usually 0). All other bands, if any, will be ignored.
     * @return a color model using a data type inferred from the given sample model.
     */
    public ColorModel createColorModel(final SampleModel target, final int numBands, final int visibleBand) {
        return createColorModel(DataType.forDataBufferType(ImageUtilities.getBandType(target)), numBands, visibleBand);
    }

    /**
     * Returns the conversion from sample values in the source image to sample values in the recolored image.
     *
     * @return conversion to sample values in recolored image.
     * @throws NoninvertibleTransformException if the conversion cannot be created.
     */
    public MathTransform1D getSampleToIndexValues() throws NoninvertibleTransformException {
        checkInitializationStatus(true);
        return (target != null) ? target.getTransferFunction().orElseGet(ColorScaleBuilder::identity).inverse() : identity();
    }

    /**
     * Returns the identity transform.
     *
     * @see Category#identity()
     */
    private static MathTransform1D identity() {
        return (MathTransform1D) MathTransforms.identity(1);
    }
}
