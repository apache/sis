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
package org.apache.sis.image.privy;

import java.util.Map;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.awt.Color;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import org.apache.sis.coverage.Category;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 * Colors to apply on a range of sample values. Instances of {@code ColorsForRange} are usually temporary,
 * used only the time needed for {@link ColorModelFactory#createPiecewise(int, int, int, ColorsForRange[])}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see ColorModelFactory#createPiecewise(int, int, int, ColorsForRange[])
 */
final class ColorsForRange implements Comparable<ColorsForRange> {
    /**
     * A name identifying the range of values, or {@code null} if not yet computed.
     * The category name is used if available, otherwise this is a string representation of the range.
     *
     * @see #name()
     */
    private CharSequence name;

    /**
     * The range of sample values on which the colors will be applied. Shall never be null.
     * May be updated after {@link ColorScaleBuilder#compact()} mapped range of floating
     * point values to range of {@link IndexColorModel} values.
     */
    NumberRange<?> sampleRange;

    /**
     * The range of sample values as originally specified.
     * Contrarily to {@link #sampleRange}, this range will not be modified by {@code compact()}.
     * This is used for fetching colors from {@link #inheritedColors} if {@link #colors} is null.
     */
    private final NumberRange<?> originalSampleRange;

    /**
     * The colors to apply on the range of sample values.
     * An empty array means that the category is explicitly specified as transparent.
     * A null value means that the category is unrecognized, in which case the default
     * is grayscale for quantitative category and transparent for qualitative category.
     *
     * @see #isUndefined()
     * @see #toARGB(int)
     */
    private final Color[] colors;

    /**
     * The original colors, or {@code null} if unspecified.
     * This is used as a fallback if {@link #colors} is null.
     * This field should be non-null only when this {@code ColorsForRange} is created for
     * styling an image before visualization. It should be null when creating a new image,
     * because the meaning of pixel values (i.e. the sample dimensions) may be different.
     *
     * @see #originalSampleRange
     * @see ColorScaleBuilder#inheritedColors
     */
    private final ColorModel inheritedColors;

    /**
     * {@code true} if this entry should be taken as data, or {@code false} if it should be ignored.
     * Entry to ignore are entries associated to NaN values.
     */
    final boolean isData;

    /**
     * Creates a new instance for the given category.
     *
     * @param  category   the category for which this {@code ColorsForRange} is created.
     * @param  colors     colors to apply on the category.
     * @param  inherited  the original colors to use as fallback, or {@code null} if none.
     *                    Should be non-null only for styling an exiting image before visualization.
     */
    ColorsForRange(final Category category, final Function<Category,Color[]> colors, final ColorModel inherited) {
        this.name        = category.getName();
        this.sampleRange = category.getSampleRange();
        this.isData      = category.isQuantitative();
        this.colors      = colors.apply(category);
        inheritedColors  = inherited;
        originalSampleRange = sampleRange;
    }

    /**
     * Creates a new instance for the given range of values.
     *
     * @param  name         a name identifying the range of values, or {@code null} for automatic.
     * @param  sampleRange  range of sample values on which the colors will be applied.
     * @param  colors       colors to apply on the range of sample values, or {@code null} for default.
     * @param  isData       whether this entry should be taken as main data (not fill values).
     * @param  inherited    the original colors to use as fallback, or {@code null} if none.
     *                      Should be non-null only for styling an exiting image before visualization.
     */
    ColorsForRange(final CharSequence name, final NumberRange<?> sampleRange, final Color[] colors,
                   final boolean isData, final ColorModel inherited)
    {
        this.name        = name;
        this.sampleRange = originalSampleRange = Objects.requireNonNull(sampleRange);
        this.isData      = isData;
        this.colors      = colors;
        inheritedColors  = inherited;
    }

    /**
     * Returns {@code true} if no color has been specified for this range.
     * Note that "undefined" is not the same as fully transparent color.
     *
     * <p>If no colors were explicitly defined but a fallback exists, then this method considers
     * this range as defined for allowing {@link ColorScaleBuilder} to inherit those colors with
     * the range of values specified by {@link #originalSampleRange}. We conceptually accept any
     * {@link #inheritedColors} even if {@link #toARGB(int)} cannot handle all of them.</p>
     */
    final boolean isUndefined() {
        return colors == null && inheritedColors == null;
    }

    /**
     * Converts {@linkplain Map#entrySet() map entries} to an array of {@code ColorsForRange} entries.
     * The {@link #category} of each entry is left to null.
     *
     * @param  colors     the colors to use for each range of sample values.
     * @param  inherited  the original color model from which to inherit undefined colors, or {@code null} if none.
     * @return colors to use for each range of values in the source image.
     *         Never null and does not contain null elements.
     */
    static ColorsForRange[] list(final Collection<Map.Entry<NumberRange<?>,Color[]>> colors, final ColorModel inherited) {
        ArgumentChecks.ensureNonEmpty("colors", colors);
        final ColorsForRange[] entries = new ColorsForRange[colors.size()];
        int n = 0;
        for (final Map.Entry<NumberRange<?>,Color[]> entry : colors) {
            final NumberRange<?> range = entry.getKey();
            boolean singleton = Objects.equals(range.getMinValue(), range.getMaxValue());
            entries[n++] = new ColorsForRange(null, range, entry.getValue(), !singleton, inherited);
        }
        return ArraysExt.resize(entries, n);            // `resize` should not be needed, but we are paranoiac.
    }

    /**
     * Returns the name pf this range of colors.
     */
    final CharSequence name() {
        if (name == null) {
            name = sampleRange.toString();
        }
        return name;
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(name()).append(": ").append(sampleRange);
        appendColorRange(buffer, toARGB(2));
        return buffer.toString();
    }

    /**
     * Appends the range of ARGB codes as hexadecimal values.
     * If the count of ARGB codes is 0, then this method does nothing.
     * If the count is 1, then this method formats the single value.
     * If the count is 2 or more, then this method formats the first and last values.
     *
     * @param  buffer  where to append the range of ARGB codes.
     * @param  count   number of ARGB codes.
     * @param  colors  providers of ARGB codes for given indices.
     */
    static void appendColorRange(final StringBuilder buffer, final int[] colors) {
        if (colors != null && colors.length != 0) {
            String s = " → ARGB[";
            int i = 0;
            do {
                buffer.append(s).append(Integer.toHexString(colors[i]).toUpperCase());
                s = " … ";
            } while (i < (i = colors.length - 1));
            buffer.append(']');
        }
    }

    /**
     * Comparator for sorting entries by their alpha value.
     * The intent is to have transparent colors first.
     *
     * @param  other  the other instance to compare with this instance.
     * @return -1 if this instance if more transparent, +1 if the other instance is more transparent, 0 if equal.
     */
    @Override
    public int compareTo(final ColorsForRange other) {
        return getAlpha() - other.getAlpha();
    }

    /**
     * Returns the maximal alpha value found in colors.
     */
    private int getAlpha() {
        int max = 0;
        if (colors != null) {
            for (final Color color : colors) {
                final int alpha = color.getAlpha();
                if (alpha > max) {
                    if (alpha >= 0xFF) {
                        return 0xFF;
                    }
                    max = alpha;
                }
            }
        } else if (isData) {
            return 0xFF;
        }
        return max;
    }

    /**
     * Returns the ARGB codes for the colors.
     * If all colors are transparent, returns an empty array.
     *
     * @param  length  desired array length. This is only a hint and may be ignored.
     * @return ARGB codes for this color ramp. Never {@code null} but may be empty.
     */
    final int[] toARGB(final int length) {
        if (colors != null) {
            int combined = 0;
            final int[] ARGB = new int[colors.length];
            for (int i=0; i<ARGB.length; i++) {
                final Color color = colors[i];
                if (color != null) {
                    int c = color.getRGB();                         // Note: getRGB() is really getARGB().
                    combined |= c;
                    ARGB[i]   = c;
                }
            }
            if ((combined & 0xFF000000) != 0) {
                return ARGB;
            }
        } else if (!originalSampleRange.isEmpty() && inheritedColors instanceof IndexColorModel) {
            /*
             * If colors are undefined, try to inherit them from the original colors.
             * If the number of available colors is larger than the desired number,
             * this block returns a subset of the inherited colors.
             */
            final IndexColorModel icm = (IndexColorModel) inheritedColors;
            int offset = Math.round((float) originalSampleRange.getMinDouble(true));
            int numSrc = Math.round((float) originalSampleRange.getMaxDouble()) - offset;
            if (originalSampleRange.isMinIncluded()) numSrc++;
            final int[] ARGB;
            if (numSrc <= length) {
                ARGB = new int[numSrc];
                if (offset == 0 && numSrc == icm.getMapSize()) {
                    icm.getRGBs(ARGB);
                } else for (int i=0; i<numSrc; i++) {
                    ARGB[i] = icm.getRGB(i + offset);
                }
            } else {
                ARGB = new int[length];
                final float scale = ((float) (numSrc-1)) / (length-1);
                for (int i=0; i<length; i++) {
                    ARGB[i] = icm.getRGB(Math.round(i * scale) + offset);
                }
            }
            return ARGB;
        } else if (isData) {
            return new int[] {0xFF000000, 0xFFFFFFFF};
        }
        return ArraysExt.EMPTY_INT;
    }
}
