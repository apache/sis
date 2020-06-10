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
package org.apache.sis.gui.map;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.BitSet;
import java.util.Locale;
import java.util.Optional;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javax.measure.Unit;
import org.opengis.geometry.DirectPosition;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.metadata.content.TransferFunctionType;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.gui.coverage.CoverageCanvas;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.Evaluator;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.Category;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Localized;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Provider of textual content to show in a {@link StatusBar} for values under cursor position.
 * Different subtypes are defined for different data sources such as {@link GridCoverage}.
 * Instances of {@code ValueUnderCursor} do not need to be thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class ValuesUnderCursor {
    /**
     * The status bar for which this object is providing values.
     * Each {@link ValuesUnderCursor} instance is used by at most {@link StatusBar} instance.
     *
     * @see #update(StatusBar, ValuesUnderCursor, ValuesUnderCursor)
     */
    private StatusBar owner;

    /**
     * Menu offering choices among the values that this {@code ValuesUnderCursor} can show.
     * This menu will be available as a contextual menu in the {@link StatusBar}.
     * It is subclass responsibility to listen to menu selections and adapt their
     * {@link #evaluate(DirectPosition)} output accordingly.
     */
    protected final Menu valueChoices;

    /**
     * Message of the last exception, used for avoiding flooding the logger with repetitive errors.
     *
     * @see #recoverableException(String, Exception)
     */
    private String lastErrorMessage;

    /**
     * Creates a new instance.
     */
    protected ValuesUnderCursor() {
        valueChoices = new Menu();
    }

    /**
     * Returns {@code true} if this {@code ValuesUnderCursor} has currently no data to show.
     * A {@code ValuesUnderCursor} may be empty for example if user unselected all bands from
     * the contextual menu.
     *
     * @return {@code true} if there is no data to show yet.
     */
    public abstract boolean isEmpty();

    /**
     * Returns a string representation of data under given position.
     * The position may be in any CRS; this method should convert coordinates as needed.
     *
     * @param  point  the cursor location in arbitrary CRS (usually the CRS shown in the status bar).
     *                May be {@code null} for declaring that the point is outside canvas region.
     * @return string representation of data under given position, or {@code null} if none.
     */
    public abstract String evaluate(final DirectPosition point);

    /**
     * Invoked when a new source of values is known for computing the expected size.
     * The given {@code main} text should be an example of the longest expected text,
     * ignoring "special" labels like "no data" values (those special cases are listed
     * in the {@code others} argument).
     *
     * <p>If {@code main} is an empty string, then no values are expected and {@link MapCanvas}
     * may hide the space normally used for showing values.</p>
     *
     * @param  main    a prototype of longest normal text that we expect.
     * @param  others  some other texts that may appear, such as labels for missing data.
     * @return {@code true} on success, or {@code false} if this method should be invoked again.
     */
    final boolean prototype(final String main, final Iterable<String> others) {
        return (owner == null) || owner.computeSizeOfSampleValues(main, others);
    }

    /**
     * Returns the locale of the JavaBean containing the given property, or {@code null} if unknown.
     * The bean is typically an instance of {@link MapCanvas}.
     *
     * @see MapCanvas#getLocale()
     */
    private static Locale getLocale(final ObservableValue<?> property) {
        if (property instanceof ReadOnlyProperty<?>) {
            final Object bean = ((ReadOnlyProperty<?>) property).getBean();
            if (bean instanceof Localized) {
                return ((Localized) bean).getLocale();
            }
        }
        return null;
    }

    /**
     * Invoked when {@link StatusBar#sampleValuesProvider} changed. Each {@link ValuesUnderCursor} instance
     * can be used by at most one {@link StatusBar} instance. Current implementation silently does nothing
     * if this is not the case.
     */
    static void update(final StatusBar owner, final ValuesUnderCursor oldValue, final ValuesUnderCursor newValue) {
        if (oldValue != null && oldValue.owner == owner) {
            oldValue.owner = null;
        }
        if (newValue != null && newValue.owner == null) {
            newValue.owner = owner;
        }
    }

    /**
     * Creates a new instance for the given canvas and registers as a listener by weak reference.
     * Caller must retain the returned reference somewhere, e.g. in {@link StatusBar#sampleValuesProvider}.
     *
     * @param  canvas  the canvas for which to create a {@link ValuesUnderCursor}, or {@code null}.
     * @return the sample values provider, or {@code null} if none.
     */
    static ValuesUnderCursor create(final MapCanvas canvas) {
        if (canvas instanceof CoverageCanvas) {
            final FromCoverage listener = new FromCoverage();
            final ObjectProperty<GridCoverage> coverageProperty = ((CoverageCanvas) canvas).coverageProperty;
            coverageProperty.addListener(new WeakChangeListener<>(listener));
            final GridCoverage coverage = coverageProperty.get();
            if (coverage != null) {
                listener.changed(null, null, coverage);
            }
            return listener;
        } else {
            // More cases may be added in the future.
        }
        return null;
    }

    /**
     * Provider of textual content to show in {@link StatusBar} for {@link GridCoverage} values under cursor position.
     * This object should be registered as a listener of some coverage property,
     * for example {@link CoverageCanvas#coverageProperty}.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.1
     * @since   1.1
     * @module
     */
    public static class FromCoverage extends ValuesUnderCursor implements ChangeListener<GridCoverage> {
        /**
         * The separator to insert between sample values. We use EM space.
         */
        private static final char SEPARATOR = '\u2003';

        /**
         * Pseudo amount of fraction digits for default format.
         * Used when we don't know how many fraction digits to use.
         */
        private static final int DEFAULT_FORMAT = -1;

        /**
         * Pseudo amount of fraction digits for scientific notation.
         */
        private static final int SCIENTIFIC_NOTATION = -2;

        /**
         * The object computing or interpolation sample values in the coverage.
         */
        private Evaluator evaluator;

        /**
         * The selection status of each band.
         */
        private final BitSet selectedBands;

        /**
         * Formatter for the values computed or interpolated by {@link #evaluator}.
         * The number of fraction digits is computed from transfer function resolution.
         * The same {@link NumberFormat} instance may appear at more than one index.
         */
        private NumberFormat[] sampleFormats;

        /**
         * Buffer where to format the textual content.
         */
        private final StringBuffer buffer;

        /**
         * Ignored but required by {@link NumberFormat}.
         */
        private final FieldPosition field;

        /**
         * Unit symbol to write after each value.
         */
        private String[] units;

        /**
         * The text to show when value under cursor is a NaN value.
         * Values are packed with band number in low bits and float ordinal value in high bits.
         *
         * @see #toNodataKey(int, float)
         * @see MathFunctions#toNanOrdinal(float)
         */
        private final Map<Long,String> nodata;

        /**
         * The text to show when cursor is outside coverage area. It should contain dimension names,
         * for example "(SST)". A {@code null} value means that {@link #onBandSelectionChanged()}
         * needs to be invoked.
         */
        private String outsideText;

        /**
         * Creates a new provider of textual values for a {@link GridCoverage}.
         */
        public FromCoverage() {
            buffer        = new StringBuffer();
            field         = new FieldPosition(0);
            nodata        = new HashMap<>();
            selectedBands = new BitSet();
        }

        /**
         * Returns {@code true} if all bands are unselected.
         */
        @Override
        public boolean isEmpty() {
            return selectedBands.isEmpty();
        }

        /**
         * Notifies this {@code ValuesUnderCursor} object that it needs to display values for a new coverage.
         * The {@code previous} argument should be the argument given in the last call to this method and is
         * used as an optimization hint. In case of doubt, it can be {@code null}.
         *
         * @param  property  the property which has been updated, or {@code null} if unknown.
         * @param  previous  previous property value, of {@code null} if none or unknown.
         * @param  coverage  new coverage for which to show sample values, or {@code null} if none.
         */
        @Override
        public void changed(final ObservableValue<? extends GridCoverage> property,
                            final GridCoverage previous, final GridCoverage coverage)
        {
            final List<SampleDimension> bands;      // Should never be null, but check anyway.
            if (coverage == null || (bands = coverage.getSampleDimensions()) == null) {
                evaluator     = null;
                units         = null;
                sampleFormats = null;
                outsideText   = null;
                nodata.clear();
                selectedBands.clear();
                valueChoices.getItems().clear();
                return;
            }
            evaluator = coverage.forConvertedValues(true).evaluator();
            evaluator.setNullIfOutside(true);
            if (previous != null && bands.equals(previous.getSampleDimensions())) {
                // Same configuration than previous coverage.
                return;
            }
            final int n   = bands.size();
            units         = new String[n];
            sampleFormats = new NumberFormat[n];
            outsideText = null;                     // Will be recomputed on next `evaluate(…)` call.
            /*
             * Only the first band is initially selected, unless the image has only 2 or 3 bands
             * in which case all bands are selected. An image with two bands is often giving the
             * (u,v) components of velocity vectors, which we want to keep together by default.
             */
            selectedBands.clear();
            selectedBands.set(0, (n <= 3) ? n : 1, true);
            nodata.clear();
            /*
             * Loop below initializes number formats and unit symbols for all bands, regardless
             * if selected or not. We do that on the assumption that the same format and symbol
             * are typically shared by all bands.
             */
            final Map<Integer,NumberFormat> sharedFormats = new HashMap<>();
            final Map<Unit<?>,String>       sharedSymbols = new HashMap<>();
            final Locale                    locale        = getLocale(property);
            final UnitFormat                unitFormat    = new UnitFormat(locale);
            final CheckMenuItem[]           menuItems     = new CheckMenuItem[n];
            valueChoices.setText(Vocabulary.getResources(locale).getString(Vocabulary.Keys.SampleDimensions));
            for (int i=0; i<n; i++) {
                final SampleDimension sd = bands.get(i);
                menuItems[i] = createMenuItem(i, sd, locale);
                /*
                 * Build the list of texts to show for missing values. A coverage can have
                 * different NaN values representing different kind of missing values.
                 */
                for (final Category c : sd.forConvertedValues(true).getCategories()) {
                    final float value = ((Number) c.getSampleRange().getMinValue()).floatValue();
                    if (Float.isNaN(value)) try {
                        nodata.putIfAbsent(toNodataKey(i, value), c.getName().toString(locale));
                    } catch (IllegalArgumentException e) {
                        recoverableException("changed", e);
                    }
                }
                /*
                 * Format in advance the units of measurement. If none, an empty string is used.
                 * Note: it is quite common that all bands use the same unit of measurement.
                 */
                units[i] = sd.getUnits().map((unit) -> sharedSymbols.computeIfAbsent(unit,
                                              (key) -> format(unitFormat, key))).orElse("");
                /*
                 * Infer a number of fraction digits to use for the resolution of sample values in each band.
                 */
                final SampleDimension isd = sd.forConvertedValues(false);
                final Integer nf = isd.getTransferFunctionFormula().map(
                        (formula) -> suggestFractionDigits(formula, isd)).orElse(DEFAULT_FORMAT);
                /*
                 * Create number formats with a number of fraction digits inferred from sample value resolution.
                 * The same format instances are shared when possible. Keys are the number of fraction digits.
                 * Special values:
                 *
                 *   - Key  0 is for integer values.
                 *   - Key -1 is for default format with unspecified number of fraction digits.
                 *   - Key -2 is for scientific notation.
                 */
                sampleFormats[i] = sharedFormats.computeIfAbsent(nf, (precision) -> {
                    switch (precision) {
                        case 0:              return NumberFormat.getIntegerInstance(locale);
                        case DEFAULT_FORMAT: return NumberFormat.getNumberInstance(locale);
                        case SCIENTIFIC_NOTATION: {
                            final NumberFormat format = NumberFormat.getNumberInstance(locale);
                            if (precision == SCIENTIFIC_NOTATION && format instanceof DecimalFormat) {
                                ((DecimalFormat) format).applyPattern("0.000E00");
                            }
                            return format;
                        }
                        default: {
                            final NumberFormat format = NumberFormat.getNumberInstance(locale);
                            format.setMinimumFractionDigits(precision);
                            format.setMaximumFractionDigits(precision);
                            return format;
                        }
                    }
                });
            }
            valueChoices.getItems().setAll(menuItems);
            if (!onBandSelectionChanged()) {
                outsideText = null;             // For forcing a new computation after canvas is added to scene graph.
            }
        }

        /**
         * Returns the key to use in {@link #nodata} map for the given "no data" value.
         * The band number can be obtained by {@link Long#intValue()}.
         *
         * @param  band   band index.
         * @param  value  the NaN value used for "no data".
         * @return key to use in {@link #nodata} map.
         * @throws IllegalArgumentException if the given value is not a NaN value
         *         or does not use a supported bits pattern.
         */
        private static Long toNodataKey(final int band, final float value) {
            return (((long) MathFunctions.toNanOrdinal(value)) << Integer.SIZE) | band;
        }

        /**
         * Suggests a number of fraction digits for numbers formatted after conversion by the given formula.
         * This is either a positive number (including 0 for integers), or the {@value #SCIENTIFIC_NOTATION}
         * or {@value #DEFAULT_FORMAT} sentinel values.
         */
        private static Integer suggestFractionDigits(final TransferFunction formula, final SampleDimension isd) {
            int nf;
            if (formula.getType() != TransferFunctionType.LINEAR) {
                nf = SCIENTIFIC_NOTATION;
            } else {
                double resolution = formula.getScale();
                if (resolution > 0 && resolution <= Double.MAX_VALUE) {     // Non-zero, non-NaN and finite.
                    final Optional<NumberRange<?>> range = isd.getSampleRange();
                    if (range.isPresent()) {
                        // See StatusBar.inflatePrecisions for rationale.
                        resolution *= (0.5 / range.get().getSpan()) + 1;
                    }
                    nf = DecimalFunctions.fractionDigitsForDelta(resolution, false);
                    if (nf < -9 || nf > 6) nf = SCIENTIFIC_NOTATION;        // Arbitrary thresholds.
                } else {
                    nf = DEFAULT_FORMAT;
                }
            }
            return nf;
        }

        /**
         * Creates a new menu item for the given sample dimension.
         *
         * @param  index   index of the sample dimension.
         * @param  sd      the sample dimension for which to create a menu item.
         * @param  locale  the locale to use for fetching the sample dimension name.
         */
        private CheckMenuItem createMenuItem(final int index, final SampleDimension sd, final Locale locale) {
            final CheckMenuItem item = new CheckMenuItem(sd.getName().toInternationalString().toString(locale));
            item.setSelected(selectedBands.get(index));
            item.selectedProperty().addListener((p,o,n) -> {
                selectedBands.set(index, n);
                if (!onBandSelectionChanged()) {
                    outsideText = null;                         // Will be recomputed on next `evaluate(…)` call.
                }
            });
            return item;
        }

        /**
         * Returns a string representation of data under given position.
         * The position may be in any CRS; this method will convert coordinates as needed.
         *
         * @param  point  the cursor location in arbitrary CRS, or {@code null} if outside canvas region.
         * @return string representation of data under given position, or {@code null} if none.
         *
         * @see Evaluator#apply(DirectPosition)
         */
        @Override
        public String evaluate(final DirectPosition point) {
            if (outsideText == null && evaluator != null) {
                onBandSelectionChanged();
            }
            if (point != null) {
                /*
                 * Take lock once instead than at each StringBuffer method call. It makes this method thread-safe,
                 * but this is a side effect of the fact that `NumberFormat` accepts only `StringBuffer` argument.
                 * We do not document this thread-safety in method contract since it is not guaranteed to apply in
                 * future SIS versions if a future `NumberFormat` version accepts non-synchronized `StringBuilder`.
                 */
                synchronized (buffer) {
                    buffer.setLength(0);
                    if (evaluator != null) try {
                        final double[] results = evaluator.apply(point);
                        if (results != null) {
                            for (int i = -1; (i = selectedBands.nextSetBit(i+1)) >= 0;) {
                                if (buffer.length() != 0) {
                                    buffer.append(SEPARATOR);
                                }
                                final double value = results[i];
                                if (Double.isNaN(value)) try {
                                    /*
                                     * If a value is NaN, returns its label as the whole content. Numerical values
                                     * in other bands are lost. We do that because "no data" strings are often too
                                     * long for being shown together with numerical values, and are often the same
                                     * for all bands. Users can see numerical values by hiding the band containing
                                     * "no data" values with contextual menu on the status bar.
                                     */
                                    final String label = nodata.get(toNodataKey(i, (float) value));
                                    if (label != null) return label;
                                } catch (IllegalArgumentException e) {
                                    recoverableException("evaluate", e);
                                }
                                sampleFormats[i].format(value, buffer, field).append(units[i]);
                            }
                            return buffer.toString();
                        }
                    } catch (PointOutsideCoverageException e) {
                        // Ignore.
                    } catch (CannotEvaluateException e) {
                        recoverableException("evaluate", e);
                    }
                }
            }
            /*
             * Coordinate is considered outside coverage area.
             * Format the sample dimension names.
             */
            return outsideText;
        }

        /**
         * Formats the unit symbol to append after a sample value. The unit symbols are created in advance
         * and reused for all sample value formatting as long as the sample dimensions do not change.
         */
        private String format(final UnitFormat format, final Unit<?> unit) {
            synchronized (buffer) {         // Take lock once instead than at each StringBuffer method call.
                buffer.setLength(0);
                format.format(unit, buffer, field);
                if (buffer.length() != 0 && Character.isLetterOrDigit(buffer.codePointAt(0))) {
                    buffer.insert(0, Characters.NO_BREAK_SPACE);
                }
                return buffer.toString();
            }
        }

        /**
         * Formats the widest text that we expect. This text is used for computing the label width.
         * Also computes the text to show when cursor is outside coverage area. This method is invoked
         * when the bands selection changed, either because of selection in contextual menu or because
         * {@link ValuesUnderCursor} is providing data for a new coverage.
         *
         * <p>We use {@link #outsideText} null value as a flag meaning meaning that this method needs
         * to be invoked. This method invocation sometime needs to be delayed because calculation of
         * text width may be wrong (produce 0 values) if invoked before {@link StatusBar#sampleValues}
         * label is added in the scene graph.</p>
         *
         * @return {@code true} on success, or {@code false} if this method should be invoked again.
         */
        private boolean onBandSelectionChanged() {
            final ObservableList<MenuItem> menus = valueChoices.getItems();
            final List<SampleDimension>    bands = evaluator.getCoverage().getSampleDimensions();
            final StringBuilder            names = new StringBuilder().append('(');
            final String text;
            synchronized (buffer) {
                buffer.setLength(0);
                for (int i = -1; (i = selectedBands.nextSetBit(i+1)) >= 0;) {
                    if (buffer.length() != 0) {
                        buffer.append(SEPARATOR);
                        names.append(", ");
                    }
                    names.append(menus.get(i).getText());
                    final int start = buffer.length();
                    final Comparable<?>[] sampleValues = bands.get(i).forConvertedValues(true)
                            .getSampleRange().map((r) -> new Comparable<?>[] {r.getMinValue(), r.getMaxValue()})
                            .orElseGet(() -> new Comparable<?>[] {0xFF});                   // Arbitrary value.
                    for (final Comparable<?> value : sampleValues) {
                        final int end = buffer.length();
                        sampleFormats[i].format(value, buffer, field);
                        final int length = buffer.length();
                        if (length - end >= end - start) {
                            buffer.delete(start, end);      // Delete first number if it was shorter.
                        } else {
                            buffer.setLength(end);          // Delete second number if it is shorter.
                        }
                    }
                    buffer.append(units[i]);
                }
                text = buffer.toString();
            }
            /*
             * At this point, `text` is the longest string of numerical values that we expect.
             * We also need to take in account the width required for displaying "no data" labels.
             * If a "no data" label is shown, it will be shown alone (we do not need to compute a
             * sum of "no data" label widths).
             */
            outsideText = text.isEmpty() ? "" : names.append(')').toString();
            final HashSet<String> others = new HashSet<>();
            for (final Map.Entry<Long,String> other : nodata.entrySet()) {
                if (selectedBands.get(other.getKey().intValue())) {
                    others.add(other.getValue());
                }
            }
            return prototype(text, others);
        }
    }

    /**
     * Invoked when an exception occurred while computing values.
     */
    final void recoverableException(final String method, final Exception e) {
        final String message = e.getMessage();
        if (!message.equals(lastErrorMessage)) {
            lastErrorMessage = message;
            Logging.recoverableException(Logging.getLogger(Modules.APPLICATION), ValuesUnderCursor.class, method, e);
        }
    }
}
