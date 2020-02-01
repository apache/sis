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
package org.apache.sis.gui.coverage;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.Workaround;


/**
 * Formatter for cell values with a number of fraction digits determined from the sample value resolution.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CellFormat {
    /**
     * The "classic" number format pattern (as opposed to scientific notation). This is non-null only after
     * {@link #cellFormat} switched to scientific notation and is used for switching back to classic notation.
     * This is a workaround for the absence of `DecimalFormat.useScientificNotation(boolean)` method.
     */
    @Workaround(library="JDK", version="13")
    private String classicFormatPattern;

    /**
     * The formatter to use for writing sample values.
     */
    private final NumberFormat cellFormat;

    /**
     * Required when invoking {@link #cellFormat} methods but not used by this class.
     * This argument is not allowed to be {@code null}, so we create an instance once
     * and reuse it at each method call.
     */
    private final FieldPosition formatField;

    /**
     * A buffer for writing sample values with {@link #cellFormat}, reused for each value to format.
     */
    private final StringBuffer buffer;

    /**
     * The last value formatted by {@link #cellFormat}. We keep this information because it happens often
     * that the same value is repeated for many cells, especially in area containing fill or missing values.
     * If the value is the same, we will reuse the {@link #lastValueAsText}.
     *
     * <p>Note: the use of {@code double} is sufficient since rendered images can not store {@code long} values,
     * so there is no precision lost that we could have with conversions from {@code long} to {@code double}.</p>
     */
    private double lastValue;

    /**
     * The formatting of {@link #lastValue}.
     */
    private String lastValueAsText;

    /**
     * Whether the sample values are integers. We use this flag for deciding which {@code Raster.getSampleValue(…)}
     * method to invoke, which {@code NumberFormat.format(…)} method to invoke, and whether to set a format pattern
     * with fraction digits.
     */
    boolean dataTypeisInteger;

    /**
     * Creates a new cell formatter.
     */
    CellFormat() {
        cellFormat  = NumberFormat.getInstance();
        formatField = new FieldPosition(0);
        buffer      = new StringBuffer();
    }

    /**
     * Invoked when the {@link #cellFormat} configuration needs to be updated.
     * Callers should invoke {@link GridView#contentChanged(boolean)} after this method.
     *
     * @param  image  the source image (shall be non-null).
     * @param  band   index of the band to show in this grid view.
     */
    final void configure(final RenderedImage image, final int band) {
        if (dataTypeisInteger) {
            cellFormat.setMaximumFractionDigits(0);
        } else {
            int n = 1;          // Default value if we can not determine the number of fraction digits.
            final Object property = image.getProperty(PlanarImage.SAMPLE_RESOLUTIONS_KEY);
            if (property != null) {
                final int c = Numbers.getEnumConstant(property.getClass().getComponentType());
                if (c >= Numbers.BYTE && c <= Numbers.BIG_DECIMAL && band < Array.getLength(property)) {
                    final double resolution = Math.abs(((Number) Array.get(property, band)).doubleValue());
                    if (resolution > 0 && resolution <= Double.MAX_VALUE) {     // Non-zero, non-NaN and finite.
                        n = DecimalFunctions.fractionDigitsForDelta(resolution, false);
                        if (n > 6 || n < -9) {      // Arbitrary threshold for switching to scientific notation.
                            if (cellFormat instanceof DecimalFormat) {
                                if (classicFormatPattern == null) {
                                    final DecimalFormat df = (DecimalFormat) cellFormat;
                                    classicFormatPattern = df.toPattern();
                                    df.applyPattern("0.###E00");
                                }
                                n = 3;
                            }
                        } else if (classicFormatPattern != null) {
                            ((DecimalFormat) cellFormat).applyPattern(classicFormatPattern);
                            classicFormatPattern = null;
                        }
                        if (n < 0) n = 0;
                    }
                }
            }
            cellFormat.setMinimumFractionDigits(n);
            cellFormat.setMaximumFractionDigits(n);
        }
        buffer.setLength(0);
        format(lastValue);
    }

    /**
     * Get the desired sample value from the specified tile and formats its string representation.
     * As a slight optimization, we reuse the previous string representation if the number is the same.
     * It may happen in particular with fill values.
     */
    final String format(final Raster tile, final int x, final int y, final int b) {
        buffer.setLength(0);
        if (dataTypeisInteger) {
            final int  integer = tile.getSample(x, y, b);
            final double value = integer;
            if (Double.doubleToRawLongBits(value) != Double.doubleToRawLongBits(lastValue)) {
                // The `format` method invoked here is not the same than in `double` case.
                lastValueAsText = cellFormat.format(integer, buffer, formatField).toString();
                lastValue = value;
            }
        } else {
            final double value = tile.getSampleDouble(x, y, b);
            if (Double.doubleToRawLongBits(value) != Double.doubleToRawLongBits(lastValue)) {
                format(value);
                lastValue = value;
            }
        }
        return lastValueAsText;
    }

    /**
     * Formats the given sample value and stores the result in {@link #lastValueAsText}.
     */
    private void format(final double value) {
        if (Double.isNaN(value)) {
            lastValueAsText = "⬚";
        } else {
            lastValueAsText = cellFormat.format(value, buffer, formatField).toString();
        }
    }

    /**
     * Formats the given value using the given formatter. This is used for indices
     * in row header or column header, which have their own formatter.
     */
    final String format(final NumberFormat headerFormat, final long value) {
        buffer.setLength(0);
        return headerFormat.format(value, buffer, formatField).toString();
    }
}
