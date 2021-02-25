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
import java.util.Collection;
import java.awt.Color;
import java.awt.image.IndexColorModel;
import org.apache.sis.coverage.Category;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 * Colors to apply on a range of sample values. Instances of {@code ColorsForRange} are temporary, used only
 * the time needed for {@link ColorModelFactory#createColorModel(int, int, int, ColorsForRange[])}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see ColorModelFactory#createColorModel(int, int, int, ColorsForRange[])
 *
 * @since 1.1
 * @module
 */
final class ColorsForRange implements Comparable<ColorsForRange> {
    /**
     * If this {@code ColorsForRange} has been created for a category, that category.
     * Otherwise {@code null}.
     */
    private final Category category;

    /**
     * The range of sample values on which the colors will be applied. Shall never be null.
     * May be updated after {@link Colorizer#compact()} mapped range of floating point values
     * to range of {@link IndexColorModel} values.
     */
    NumberRange<?> sampleRange;

    /**
     * The colors to apply on the range of sample values.
     * A null or empty array means transparent.
     */
    private final Color[] colors;

    /**
     * Creates a new instance for the given range of values.
     *
     * @param  category     the category for which this {@code ColorsForRange} is created, or {@code null}.
     * @param  sampleRange  range of sample values on which the colors will be applied.
     * @param  colors       colors to apply on the range of sample values, or {@code null} for transparent.
     */
    ColorsForRange(final Category category, final NumberRange<?> sampleRange, final Color[] colors) {
        ArgumentChecks.ensureNonNull("sampleRange", sampleRange);
        this.category    = category;
        this.sampleRange = sampleRange;
        this.colors      = colors;
    }

    /**
     * Converts {@linkplain Map#entrySet() map entries} to an array of {@code ColorsForRange} entries.
     * The {@link #category} of each entry is left to null.
     */
    static ColorsForRange[] list(final Collection<Map.Entry<NumberRange<?>,Color[]>> colors) {
        final ColorsForRange[] entries = new ColorsForRange[colors.size()];
        int n = 0;
        for (final Map.Entry<NumberRange<?>,Color[]> entry : colors) {
            entries[n++] = new ColorsForRange(null, entry.getKey(), entry.getValue());
        }
        return ArraysExt.resize(entries, n);            // `resize` should not be needed, but we are paranoiac.
    }

    /**
     * Returns {@code true} if this entry should be taken as data, or {@code false} if it should be ignored.
     * Entry to ignore and entries associated to NaN values.
     */
    final boolean isData() {
        return category == null || category.isQuantitative();
    }

    /**
     * Returns a name identifying the range of values. the category name is used if available,
     * otherwise a string representation of the range is created.
     */
    final CharSequence name() {
        if (category != null) {
            final CharSequence name = category.getName();
            if (name != null) return name;
        }
        return sampleRange.toString();
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return name().toString();
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
            for (int i=0; i<colors.length; i++) {
                final int alpha = colors[i].getAlpha();
                if (alpha > max) {
                    if (alpha >= 0xFF) {
                        return 0xFF;
                    }
                    max = alpha;
                }
            }
        }
        return max;
    }

    /**
     * Returns the ARGB codes for the colors.
     * If all colors are transparent, returns an empty array.
     *
     * @return ARGB codes for the given colors. Never {@code null} but may be empty.
     */
    final int[] toARGB() {
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
        }
        return ArraysExt.EMPTY_INT;
    }
}