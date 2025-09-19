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

import java.util.OptionalInt;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.Workaround;
import org.apache.sis.gui.internal.Styles;
import org.apache.sis.gui.internal.RecentChoices;


/**
 * Formatter for cell values with a number of fraction digits determined from the sample value resolution.
 * The property value is the localized format pattern as produced by {@link DecimalFormat#toLocalizedPattern()}.
 * This property is usually available but not always, see {@link #hasPattern()}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class CellFormat extends SimpleStringProperty {
    /**
     * Text to format in place of NaN values.
     */
    private static final String SYMBOL_NaN = "⬚";

    /**
     * The "classic" number format pattern (as opposed to scientific notation). This is non-null only after
     * {@link #cellFormat} switched to scientific notation and is used for switching back to classic notation.
     * This is a workaround for the absence of {@code DecimalFormat.useScientificNotation(boolean)} method.
     */
    @Workaround(library="JDK", version="24")
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
     * <p>Note: the use of {@code double} is sufficient since rendered images cannot store {@code long} values,
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
    boolean dataTypeIsInteger;

    /**
     * Whether the pattern has been made shorter by calls to {@link #shorterPattern()}.
     */
    private boolean shortenedPattern;

    /**
     * Temporarily set to {@code true} when the user selects or enters a new pattern in a GUI control, then
     * reset to {@code false} after the new values has been set. This is a safety against recursive calls
     * to {@link #onPatternSelected(ComboBox, String)} because of bi-directional change listeners.
     */
    private boolean isAdjusting;

    /**
     * Creates a new cell formatter which is also a {@code "cellFormatPattern"} property for the given bean.
     */
    CellFormat(final GridView bean) {
        super(bean, "cellFormatPattern");
        cellFormat  = NumberFormat.getInstance();
        formatField = new FieldPosition(0);
        buffer      = new StringBuffer();
        updatePropertyValue();
    }

    /**
     * Returns whether the format can be configured with a pattern. If this method returns {@code false},
     * then the {@link GridView#cellFormatPattern()} property is not available.
     */
    final boolean hasPattern() {
        return (cellFormat instanceof DecimalFormat);
    }

    /**
     * Sets this property to the current {@link DecimalFormat} pattern and notifies all listeners
     * if the new pattern is different than the old one. This method needs to be invoked explicitly
     * after the {@link #cellFormat} has been configured.
     */
    private void updatePropertyValue() {
        if (cellFormat instanceof DecimalFormat) {
            super.setValue(((DecimalFormat) cellFormat).toLocalizedPattern());
        }
    }

    /**
     * Invoked when a new pattern is set programmatically on the {@link GridView#cellFormatPattern()} property.
     * This is also invoked when the used selected or entered a new pattern, by user action through the GUI.
     *
     * @param  pattern  the new pattern.
     * @throws NullPointerException if {@code pattern} is {@code null}.
     * @throws IllegalArgumentException if the given pattern is invalid.
     */
    @Override
    public void setValue(final String pattern) {
        shortenedPattern = false;
        if (cellFormat instanceof DecimalFormat) {
            ((DecimalFormat) cellFormat).applyLocalizedPattern(pattern);
            updatePropertyValue();
            ((GridView) getBean()).updateCellValues();
        }
    }

    /**
     * Tries to reduce the size of the pattern used for formatting the numbers.
     * This method is invoked when the numbers are too large for the cell width.
     *
     * @return whether the pattern has been made smaller.
     */
    final boolean shorterPattern() {
        int n = cellFormat.getMaximumFractionDigits() - 1;
        if (n >= 0) {
            cellFormat.setMaximumFractionDigits(n);
        } else if (cellFormat.isGroupingUsed()) {
            cellFormat.setGroupingUsed(false);
        } else {
            return false;
        }
        shortenedPattern = true;
        formatCell(lastValue);
        return true;
    }

    /**
     * Restores the pattern to the value specified in the pattern property.
     * This method cancels the effect of {@link #shorterPattern()}.
     * This method should be invoked when the cells have been made wider,
     * and this change may allow the original pattern to fit in the new size.
     */
    final void restorePattern() {
        if (shortenedPattern) {
            shortenedPattern = false;
            if (cellFormat instanceof DecimalFormat) {
                ((DecimalFormat) cellFormat).applyLocalizedPattern(getValue());
            }
        }
    }

    /**
     * Invoked when the user selects or enters a new pattern. This method should not be invoked explicitly.
     * This is a callback to be invoked by the {@link javafx.scene.control.SingleSelectionModel} of a control.
     *
     * @param  choices   the control where format pattern is selected.
     * @param  newValue  the new format pattern.
     */
    private void onPatternSelected(final ComboBox<String> choices, final String newValue) {
        if (!isAdjusting) {
            boolean error;
            String message;
            try {
                isAdjusting = true;
                setValue(newValue);
                message = null;
                error = false;
            } catch (IllegalArgumentException e) {
                message = e.getLocalizedMessage();
                error = true;
            } finally {
                isAdjusting = false;
            }
            Tooltip tooltip = null;
            if (message != null) {
                tooltip = choices.getTooltip();
                if (tooltip != null) {
                    tooltip.setText(message);
                } else {
                    tooltip = new Tooltip(message);
                    tooltip.setShowDelay(Duration.seconds(0.1));
                }
            }
            choices.setTooltip(tooltip);
            choices.getEditor().pseudoClassStateChanged(Styles.ERROR, error);
        }
    }

    /**
     * An editable combo box which remember the most recently used values,
     * or {@code null} if the {@link NumberFormat} does not support patterns.
     */
    final ComboBox<String> createEditor() {
        if (!hasPattern()) {
            return null;
        }
        /*
         * Create a few predefined choices of patterns with various number of fraction digits.
         */
        final int min = cellFormat.getMinimumFractionDigits();
        final int max = cellFormat.getMaximumFractionDigits();
        final var patterns = new String[max + 2];
        patterns[max + 1] = getValue();
        cellFormat.setMinimumFractionDigits(max);
        for (int n=max; n >= 0; n--) {
            cellFormat.setMaximumFractionDigits(n);
            patterns[n] = ((DecimalFormat) cellFormat).toLocalizedPattern();
        }
        cellFormat.setMinimumFractionDigits(min);           // Restore previous setting.
        cellFormat.setMaximumFractionDigits(max);
        /*
         * Create the combo-box with above patterns and register listeners in both directions.
         */
        final var choices = new ComboBox<String>();
        choices.setEditable(true);
        choices.getItems().setAll(patterns);
        choices.getSelectionModel().selectFirst();
        choices.getSelectionModel().selectedItemProperty().addListener((e,o,n) -> onPatternSelected(choices, n));
        addListener((e,o,n) -> RecentChoices.setInList(choices, n));
        return choices;
    }

    /**
     * Invoked when the {@link #cellFormat} configuration needs to be updated.
     * Callers should invoke {@link GridView#updateCellValues()} after this method.
     *
     * @param  image  the source image (shall be non-null).
     * @param  band   index of the band to show in this grid view.
     */
    final void configure(final RenderedImage image, final int band) {
        if (dataTypeIsInteger) {
            cellFormat.setMaximumFractionDigits(0);
        } else {
            int n = getFractionDigits(image, band).orElse(1);
            if (n > 6 || n < -9) {      // Arbitrary threshold for switching to scientific notation.
                if (cellFormat instanceof DecimalFormat) {
                    if (classicFormatPattern == null) {
                        final DecimalFormat df = (DecimalFormat) cellFormat;
                        classicFormatPattern = df.toPattern();
                        df.applyPattern("0.000E00");
                    }
                    n = 3;
                }
            } else if (classicFormatPattern != null) {
                ((DecimalFormat) cellFormat).applyPattern(classicFormatPattern);
                classicFormatPattern = null;
            }
            if (n < 0) n = 0;
            cellFormat.setMinimumFractionDigits(n);
            cellFormat.setMaximumFractionDigits(n);
        }
        buffer.setLength(0);
        formatCell(lastValue);
        updatePropertyValue();
    }

    /**
     * Returns the number of fraction digits to use for formatting sample values in the given band of the given image.
     * This method uses the {@value PlanarImage#SAMPLE_RESOLUTIONS_KEY} property value.
     *
     * @param  image  the image from which to get the number of fraction digits.
     * @param  band   the band for which to get the number of fraction digits.
     * @return number of fraction digits. Maybe a negative number if the sample resolution is equal or greater than 10.
     */
    private static OptionalInt getFractionDigits(final RenderedImage image, final int band) {
        final Object property = image.getProperty(PlanarImage.SAMPLE_RESOLUTIONS_KEY);
        if (property != null) {
            if (Numbers.isNumber(property.getClass().getComponentType()) && band < Array.getLength(property)) {
                final double resolution = Math.abs(((Number) Array.get(property, band)).doubleValue());
                if (resolution > 0 && resolution <= Double.MAX_VALUE) {     // Non-zero, non-NaN and finite.
                    return OptionalInt.of(DecimalFunctions.fractionDigitsForDelta(resolution, false));
                }
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Gets the desired sample value from the specified tile and formats its string representation.
     * As a slight optimization, we reuse the previous string representation if the number is the same.
     * It may happen in particular with fill values.
     *
     * @throws IndexOutOfBoundsException if the given pixel coordinates are out of bounds.
     */
    final String format(final Raster tile, final int x, final int y, final int b) {
        buffer.setLength(0);
        if (dataTypeIsInteger) {
            final int  integer = tile.getSample(x, y, b);
            final double value = integer;
            if (Double.doubleToRawLongBits(value) != Double.doubleToRawLongBits(lastValue)) {
                // The `format` method invoked here is not the same as in the `double` case.
                lastValueAsText = cellFormat.format(integer, buffer, formatField).toString();
                lastValue = value;
            }
        } else {
            final double value = tile.getSampleDouble(x, y, b);
            if (Double.doubleToRawLongBits(value) != Double.doubleToRawLongBits(lastValue)) {
                formatCell(value);
                lastValue = value;
            }
        }
        return lastValueAsText;
    }

    /**
     * Formats the given sample value and stores the result in {@link #lastValueAsText}.
     * This method should be invoked only for real numbers, not when the number is known
     * to be an integer value. This method is designed for cell values in {@link GridView},
     * where {@link Double#NaN} values are represented by a more compact symbol.
     * For formatting in other contexts, use {@link #format(Number)} instead.
     */
    private void formatCell(final double value) {
        if (Double.isNaN(value)) {
            lastValueAsText = SYMBOL_NaN;
        } else {
            lastValueAsText = cellFormat.format(value, buffer, formatField).toString();
        }
    }

    /**
     * Formats the given sample value. This is used for formatting the values from another source
     * than a {@link Raster}, such as a {@link org.apache.sis.coverage.SampleDimension}.
     */
    final String format(final Number value) {
        buffer.setLength(0);
        return cellFormat.format(value, buffer, formatField).toString();
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
