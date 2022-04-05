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
package org.apache.sis.internal.coverage.j2d;

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
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Helper classes for allowing an image to be colorized, by building an {@link IndexColorModel} if needed.
 * Image created by this class are suitable for visualization purposes but generally not for computations.
 * Usage:
 *
 * <ol>
 *   <li>Create a new {@link Colorizer} instance.</li>
 *   <li>Invoke one of {@code initialize(…)} methods.</li>
 *   <li>Invoke {@link #createColorModel(int, int, int)}.</li>
 *   <li>Discards {@code Colorizer}; each instance should be used only once.</li>
 * </ol>
 *
 * There is no {@code initialize(Raster)} or {@code initialize(RenderedImage)} method because if those methods
 * were present, users may expect them to iterate over sample values for finding minimum and maximum values.
 * We do not perform such iteration because they are potentially costly and give unstable results:
 * the resulting color model varies from image to image, which is confusing when many images exist
 * for the same product at different times or at different depths.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see ColorModelType
 * @see ColorModelFactory#createColorModel(int, int, int, Collection)
 *
 * @since 1.1
 * @module
 */
public final class Colorizer {
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
     * The type resulting from sample values conversion applied by {@link #compactColorModel(int, int)}.
     * Current value is {@link DataBuffer#TYPE_BYTE}.
     */
    public static final int TYPE_COMPACT = DataBuffer.TYPE_BYTE;

    /**
     * Applies a gray scale to quantitative category and transparent colors to qualitative categories.
     * This is a possible argument for the {@link #Colorizer(Function)} constructor.
     */
    public static final Function<Category,Color[]> GRAYSCALE =
            (category) -> category.isQuantitative() ? new Color[] {Color.BLACK, Color.WHITE} : null;

    /**
     * The colors to use for each category. Never {@code null}.
     * The function may return {@code null}, which means transparent.
     */
    private final Function<Category,Color[]> colors;

    /**
     * The colors to use for each range of values in the source image.
     * Entries will be sorted and modified in place.
     * The array may be null if unspecified, but shall not contain null element.
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
     * May be the same than {@link #source} or {@code source.forConvertedValues(true)} if one
     * of those values is suitable, or a new sample dimension created by {@link #compact()}.
     *
     * <p>This sample dimension should not be returned to the user because it may not contain meaningful values.
     * For example it may contain an "artificial" transfer function for computing a {@link MathTransform1D} from
     * source range to the [0 … 255] value range.</p>
     */
    private SampleDimension target;

    /**
     * Default range of values to use if no explicitly specified by a {@link Category}.
     */
    private NumberRange<?> defaultRange;

    /**
     * Creates a new colorizer which will apply colors on the given range of values in source image.
     * The {@code Colorizer} is considered initialized after this constructor;
     * callers shall <strong>not</strong> invoke an {@code initialize(…)} method.
     *
     * @param  colors  the colors to use for each range of values in source image.
     *                 A {@code null} entry value means transparent.
     */
    public Colorizer(final Collection<Map.Entry<NumberRange<?>,Color[]>> colors) {
        ArgumentChecks.ensureNonNull("colors", colors);
        entries = ColorsForRange.list(colors);
        this.colors = GRAYSCALE;
    }

    /**
     * Creates a new colorizer which will use the given function for determining the colors to apply.
     * Callers need to invoke an {@code initialize(…)} method after this constructor.
     *
     * @param  colors  the colors to use for each category, or {@code null} for default.
     *                 The function may return {@code null}, which means transparent.
     */
    public Colorizer(final Function<Category,Color[]> colors) {
        this.colors = (colors != null) ? colors : GRAYSCALE;
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
                boolean missingNodata = true;
                ColorsForRange[] entries = new ColorsForRange[categories.size()];
                for (int i=0; i<entries.length; i++) {
                    final Category category = categories.get(i);
                    entries[i] = new ColorsForRange(category, colors);
                    missingNodata &= category.isQuantitative();
                }
                /*
                 * If the model uses floating point values and there is no "no data" category, add one.
                 * We force a "no data" category because floating point values may be NaN.
                 */
                if (missingNodata && (model == null || !ImageUtilities.isIntegerType(model))) {
                    final int count = entries.length;
                    entries = Arrays.copyOf(entries, count + 1);
                    entries[count] = new ColorsForRange(TRANSPARENT,
                            NumberRange.create(Float.class, Float.NaN), null, false);
                }
                // Leave `target` to null. It will be computed by `compact()` if needed.
                this.entries = entries;
                return true;
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
     * are ignored (because they will be replaced by colors specified by this {@code Colorizer}); only the range
     * of values will be fetched, if such range exists.
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
                initialize(scs.offset, scs.maximum);
                return true;
            }
        }
        return false;
    }

    /*
     * Do not provide methods taking Raster or RenderedImage argument.
     * See class javadoc for rational.
     */

    /**
     * Applies colors on the given range of values. The 0 index will be reserved for NaN value,
     * and indices in the [1 … 255] will be mapped to the given range.
     *
     * <p>This method is typically used as a last resort fallback when all other {@code initialize(…)}
     * methods failed or can not be applied. This method assumes that no {@link Category} information
     * is available.</p>
     *
     * @param  minimum  minimum value, inclusive.
     * @param  maximum  maximum value, inclusive.
     * @throws IllegalStateException if a sample dimension is already defined on this colorizer.
     */
    public void initialize(final double minimum, final double maximum) {
        checkInitializationStatus(false);
        ArgumentChecks.ensureFinite("minimum", minimum);
        ArgumentChecks.ensureFinite("maximum", maximum);
        defaultRange = NumberRange.create(minimum, true, maximum, true);
        target = new SampleDimension.Builder()
                .setBackground(TRANSPARENT, 0)
                .addQuantitative(COLOR_INDEX, NumberRange.create(1, true, MAX_VALUE, true), defaultRange)
                .setName(VISUAL).build();
        /*
         * We created a synthetic `SampleDimension` with the specified range of values.
         * Recompute `source` and `entries` fields for consistency with the new ranges.
         * The `source` is recreated as a matter of principle, but will not be used by
         * `compact()` because `target` will take precedence.
         */
        source = target.forConvertedValues(true);
        final List<Category> categories = target.getCategories();
        final ColorsForRange[] entries = new ColorsForRange[categories.size()];
        for (int i=0; i<entries.length; i++) {
            final Category category = categories.get(i);
            entries[i] = new ColorsForRange(category, category.getName() == TRANSPARENT ? GRAYSCALE : colors);
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
     * <p>There is two outputs: the {@link #target} sample dimension, and modifications done in-place in the
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
        ColorsForRange[] entries = this.entries;
reuse:  if (source != null) {
            target = source.forConvertedValues(false);
            if (target.getSampleRange().filter(Colorizer::isAlreadyScaled).isPresent()) {
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
         * If we reach this point, `source` sample dimensions were not specified or can not be used for
         * getting a transfer function to the [0 … 255] range of values. We will need to create our own.
         * First, sort the entries for having transparent colors first.
         */
        Arrays.sort(entries);                               // Move transparent colors in first positions.
        double span  = 0;                                   // Total span of all non-NaN ranges.
        int lower    = 0;                                   // First available index in the [0 … 255] range.
        int deferred = 0;                                   // Number of entries deferred to next loop.
        int count    = entries.length;                      // Total number of valid entries.
        NumberRange<?> themes = null;                       // The range of values in a thematic map.
        final Map<NumberRange<Integer>,ColorsForRange> mapper = new HashMap<>();
        final SampleDimension.Builder builder = new SampleDimension.Builder();
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
                        builder.mapQualitative(entry.name, targetRange, (float) value);
                    } else {
                        if (value == entry.sampleRange.getMaxDouble()) {
                            sourceRange = NumberRange.create(value - 0.5, true, value + 0.5, false);
                        }
                        builder.addQuantitative(entry.name, targetRange, sourceRange);
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
                tmp[deferred-1] = new ColorsForRange(null, sourceRange, new Color[] {Color.BLACK, Color.WHITE}, true);
                entries = tmp;
            }
        }
        this.entries = entries = ArraysExt.resize(entries, count);      // Should be a no-op most of the times.
        /*
         * Above loop mapped all NaN values. Now map the real values. Usually, there is exactly one entry taking
         * all remaining values in the [0 … 255] range, but code below is tolerant to arbitrary amount of ranges.
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
                builder.addQuantitative(entry.name, samples, entry.sampleRange);
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
     * @param  dataType     the color model type. One of {@link DataBuffer#TYPE_BYTE}, {@link DataBuffer#TYPE_USHORT},
     *                      {@link DataBuffer#TYPE_SHORT}, {@link DataBuffer#TYPE_INT}, {@link DataBuffer#TYPE_FLOAT}
     *                      or {@link DataBuffer#TYPE_DOUBLE}.
     * @param  numBands     the number of bands for the color model (usually 1). The returned color model will render only
     *                      the {@code visibleBand} and ignore the others, but the existence of all {@code numBands} will
     *                      be at least tolerated. Supplemental bands, even invisible, are useful for processing.
     * @param  visibleBand  the band to be made visible (usually 0). All other bands, if any, will be ignored.
     * @return a color model suitable for {@link java.awt.image.RenderedImage} objects with values in the given ranges.
     */
    public ColorModel createColorModel(final int dataType, final int numBands, final int visibleBand) {
        checkInitializationStatus(true);
        ArgumentChecks.ensureStrictlyPositive("numBands", numBands);
        ArgumentChecks.ensureBetween("visibleBand", 0, numBands - 1, visibleBand);
        return ColorModelFactory.createPiecewise(dataType, numBands, visibleBand, entries);
    }

    /**
     * Returns a color model with colors interpolated in the [0 … 255] range of values.
     * Conversions from range specified at construction time to the [0 … 255] range is
     * given by {@link #getSampleToIndexValues()}. Images using this color model shall
     * use a {@link DataBuffer} of type {@link #TYPE_COMPACT}.
     *
     * @param  numBands     the number of bands for the color model (usually 1). The returned color model will render only
     *                      the {@code visibleBand} and ignore the others, but the existence of all {@code numBands} will
     *                      be at least tolerated. Supplemental bands, even invisible, are useful for processing.
     * @param  visibleBand  the band to be made visible (usually 0). All other bands, if any, will be ignored.
     * @return a color model suitable for {@link java.awt.image.RenderedImage} objects with values in the given ranges.
     */
    public ColorModel compactColorModel(final int numBands, final int visibleBand) {
        checkInitializationStatus(true);
        compact();
        return createColorModel(TYPE_COMPACT, numBands, visibleBand);
    }

    /**
     * Returns the conversion from sample values in the source image to sample values in the recolored image.
     *
     * @return conversion to sample values in recolored image.
     * @throws NoninvertibleTransformException if the conversion can not be created.
     */
    public MathTransform1D getSampleToIndexValues() throws NoninvertibleTransformException {
        checkInitializationStatus(true);
        return (target != null) ? target.getTransferFunction().orElseGet(Colorizer::identity).inverse() : identity();
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
