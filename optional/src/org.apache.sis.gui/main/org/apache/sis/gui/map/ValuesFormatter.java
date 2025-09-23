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
import javax.measure.Unit;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.AbstractDirectPosition;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.util.Characters;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.internal.shared.Numerics;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;

// Specific to the main branch:
import org.apache.sis.coverage.CannotEvaluateException;


/**
 * Fetches values from the coverage and formats them. This task is executed in a background thread
 * because calls to {@link GridCoverage#render(GridExtent)} can take an arbitrary amount of time.
 * The same {@code Formatter} instance can be reused as long as the configuration does not change.
 *
 * <p>As a rule of thumbs, all fields in {@link ValuesFromCoverage} class shall be read and written
 * from the JavaFX thread, while all fields in this {@code Formatter} class can be read and written
 * from a background thread.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ValuesFormatter extends ValuesUnderCursor.Formatter {
    /**
     * The separator to insert between sample values. We use EM space.
     */
    private static final char SEPARATOR = '\u2003';

    /**
     * Pseudo number of fraction digits for default format.
     * Used when we don't know how many fraction digits to use.
     */
    private static final int DEFAULT_FORMAT = -1;

    /**
     * Pseudo number of fraction digits for scientific notation.
     */
    private static final int SCIENTIFIC_NOTATION = -2;

    /**
     * The object computing or interpolation sample values in the coverage.
     */
    private final GridCoverage.Evaluator evaluator;

    /**
     * Formatter for the values computed or interpolated by {@link #evaluator}.
     * The number of fraction digits is computed from transfer function resolution.
     * The same {@link NumberFormat} instance may appear at more than one index.
     * Array shall not be modified after construction.
     */
    private final NumberFormat[] sampleFormats;

    /**
     * Buffer where to format the textual content.
     * We use this buffer as a synchronization lock because this class is already synchronized,
     * so synchronizing on {@code buffer} allows us to use only one lock.
     */
    private final StringBuffer buffer;

    /**
     * Ignored but required by {@link NumberFormat}.
     */
    private final FieldPosition field;

    /**
     * Unit symbol to write after each value, including the leading space separator if needed.
     * The content of this array shall not be modified after construction.
     */
    private final String[] units;

    /**
     * The text to show when value under cursor is a NaN value.
     * Values are packed with band number in low bits and float ordinal value in high bits.
     * Map content shall not be modified after construction.
     *
     * @see #toNodataKey(int, float)
     * @see MathFunctions#toNanOrdinal(float)
     */
    private final Map<Long,String> nodata;

    /**
     * The text to show when cursor is outside coverage area. It should contain dimension names, for example "(SST)".
     * May be {@code null} if {@link #setSelectedBands(BitSet, String[], HashSet)} needs to be invoked.
     */
    private String outsideText;

    /**
     * The selection status of each band at the time of {@link #setSelectedBands(BitSet, String[], HashSet)} invocation.
     * We need a copy of {@link ValuesFromCoverage} field because the two sets are read and updated in different threads.
     * This set should not be modified; instead, copy should be made.
     *
     * @see ValuesFromCoverage#selectedBands
     */
    private BitSet selectedBands;

    /**
     * Non-null when a new slice needs to be passed to {@link #evaluator}.
     * A new value is set when {@link CoverageCanvas#sliceExtentProperty} changed.
     * This value is reset to {@code null} after the slice has been taken in account.
     *
     * @see #setSlice(GridExtent)
     */
    private GridExtent newSlice;

    /**
     * Creates a new formatter for the specified coverage.
     * This constructor should be invoked in a background thread.
     *
     * @param owner     the instance which will evaluate values under cursor position.
     * @param inherit   formatter from which to inherit band configuration, or {@code null} if none.
     * @param coverage  new coverage. Shall not be null.
     * @param slice     initial value of {@link #newSlice}.
     * @param bands     sample dimensions of the new coverage.
     * @param locale    locale of number formats to create.
     */
    ValuesFormatter(final ValuesUnderCursor owner, final ValuesFormatter inherit, final GridCoverage coverage,
                    final GridExtent slice, final List<SampleDimension> bands, final Locale locale)
    {
        super(owner);
        buffer    = new StringBuffer();
        field     = new FieldPosition(0);
        newSlice  = slice;
        evaluator = coverage.forConvertedValues(true).evaluator();
        evaluator.setNullIfOutside(true);
        evaluator.setWraparoundEnabled(true);
        if (inherit != null) {
            // Same configuration as previous coverage.
            synchronized (inherit.buffer) {
                units         = inherit.units;
                nodata        = inherit.nodata;
                outsideText   = inherit.outsideText;
                selectedBands = inherit.selectedBands;
                sampleFormats = inherit.sampleFormats.clone();
                for (int i=0; i < sampleFormats.length; i++) {
                    sampleFormats[i] = (NumberFormat) sampleFormats[i].clone();
                }
            }
            return;
        }
        final int numBands = bands.size();
        sampleFormats = new NumberFormat[numBands];
        units         = new String[numBands];
        nodata        = new HashMap<>();
        selectedBands = new BitSet();
        selectedBands.set(0, (numBands <= 3) ? numBands : 1, true);
        /*
         * Loop below initializes number formats and unit symbols for all bands, regardless
         * if selected or not. We do that on the assumption that the same format and symbol
         * are typically shared by all bands.
         */
        final var sharedFormats = new HashMap<Integer,NumberFormat>();
        final var sharedSymbols = new HashMap<Unit<?>,String>();
        final var unitFormat    = new UnitFormat(locale);
        for (int b=0; b<numBands; b++) {
            /*
             * Build the list of texts to show for missing values. A coverage can have
             * different NaN values representing different kind of missing values.
             */
            final SampleDimension sd = bands.get(b);
            for (final Category c : sd.forConvertedValues(true).getCategories()) {
                final float value = ((Number) c.getSampleRange().getMinValue()).floatValue();
                if (Float.isNaN(value)) try {
                    nodata.putIfAbsent(toNodataKey(b, value), c.getName().toString(locale));
                } catch (IllegalArgumentException e) {
                    recoverableException("changed", e);
                }
            }
            /*
             * Format in advance the units of measurement. If none, an empty string is used.
             * Note: it is quite common that all bands use the same unit of measurement.
             */
            units[b] = sd.getUnits().map((unit) -> sharedSymbols.computeIfAbsent(unit,
                                          (key) -> format(unitFormat, key))).orElse("");
            /*
             * Infer a number of fraction digits to use for the resolution of sample values in each band.
             */
            final SampleDimension isd = sd.forConvertedValues(false);
            final Integer nf = isd.getTransferFunctionFormula()
                    .map((formula) -> suggestFractionDigits(formula, isd))
                    .orElse(DEFAULT_FORMAT);
            /*
             * Create number formats with a number of fraction digits inferred from sample value resolution.
             * The same format instances are shared when possible. Keys are the number of fraction digits.
             * Special values:
             *
             *   - Key  0 is for integer values.
             *   - Key -1 is for default format with unspecified number of fraction digits.
             *   - Key -2 is for scientific notation.
             */
            sampleFormats[b] = sharedFormats.computeIfAbsent(nf, (precision) -> {
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
    }

    /**
     * Formats the unit symbol to append after a sample value. The unit symbols are created in advance
     * and reused for all sample value formatting as long as the sample dimensions do not change.
     */
    private String format(final UnitFormat format, final Unit<?> unit) {
        synchronized (buffer) {         // Take lock once instead of at each StringBuffer method call.
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
     * <p>We use {@link ValuesFromCoverage#needsBandRefresh} as a flag meaning that this method needs to be invoked.
     * This method invocation sometimes needs to be delayed because calculation of text width may be wrong
     * (produce 0 values) if invoked before {@link StatusBar#sampleValues} label is added in the scene graph.</p>
     *
     * <p>This method uses the same synchronization lock as {@link #evaluate(DirectPosition)}.
     * Consequently, this method may block if data loading are in progress in another thread.</p>
     *
     * @param  selection  copy of {@link ValuesFromCoverage#selectedBands} made by the caller in JavaFX thread.
     * @param  labels     labels of {@link ValuesFromCoverage#valueChoices} menu items computed by caller in JavaFX thread.
     * @param  others     an initially empty set where to put textual representation of "no data" values.
     * @return the text to use as a prototype for sample values.
     */
    final String setSelectedBands(final BitSet selection, final String[] labels, final HashSet<String> others) {
        synchronized (buffer) {
            final List<SampleDimension> bands = evaluator.getCoverage().getSampleDimensions();
            final var names = new StringBuilder().append('(');
            buffer.setLength(0);
            for (int i = -1; (i = selection.nextSetBit(i+1)) >= 0;) {
                if (buffer.length() != 0) {
                    buffer.append(SEPARATOR);
                    names.append(", ");
                }
                names.append(labels[i]);
                final int start = buffer.length();
                final Comparable<?>[] sampleValues = bands.get(i)
                        .forConvertedValues(true)
                        .getSampleRange()
                        .map((range)  -> new Comparable<?>[] {range.getMinValue(), range.getMaxValue()})
                        .orElseGet(() -> new Comparable<?>[] {0xFFFF});     // Arbitrary value.
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
            final String text = buffer.toString();
            /*
             * At this point, `text` is the longest string of numerical values that we expect.
             * We also need to take in account the width required for displaying "no data" labels.
             * If a "no data" label is shown, it will be shown alone (we do not need to compute a
             * sum of "no data" label widths).
             */
            for (final Map.Entry<Long,String> other : nodata.entrySet()) {
                if (selection.get(other.getKey().intValue())) {
                    others.add(other.getValue());
                }
            }
            outsideText = text.isEmpty() ? "" : names.append(')').toString();
            selectedBands = selection;              // Set only on success.
            return text;
        }
    }

    /**
     * Sets the slice in grid coverages where sample values should be evaluated for next positions.
     * The given slice will apply to all positions formatted after this method call,
     * until this method is invoked again for a new slice.
     *
     * <p>This method shall be synchronized on the same lock as {@link #copy(DirectPosition)},
     * which is the lock used by {@link #evaluateLater(DirectPosition)}.</p>
     *
     * @param  slice  grid coverage slice where to evaluate the sample values.
     */
    final synchronized void setSlice(final GridExtent slice) {
        newSlice = slice;
    }

    /**
     * Position of next point to evaluate, together with the grid slice where sample values should be evaluated.
     * Those two information are kept together because they are closely related: the slice depends on position
     * in dimensions not necessarily expressed in the given {@link DirectPosition}, and we want to take those
     * two information in the same synchronized block.
     */
    private static final class Position extends AbstractDirectPosition {
        /** Coordinates of this position. */
        private final double[] coordinates;

        /** Coordinate reference system of this position. */
        private final CoordinateReferenceSystem crs;

        /**
         * Non-null when a new slice needs to be passed to {@link #evaluator}.
         * Should be null if the slice did not changed since last invocation.
         * This is a copy of {@link ValuesFormatter#newSlice}.
         */
        final GridExtent newSlice;

        /**
         * Creates a copy of the given position. If {@link #evaluator} needs to be set
         * to a new default slice position in order to evaluate the given coordinates,
         * that position should be given as a non-null {@code slice} argument.
         */
        Position(final DirectPosition position, final GridExtent slice) {
            coordinates = position.getCoordinate();
            crs         = position.getCoordinateReferenceSystem();
            newSlice    = slice;
        }

        /** Returns the number of dimensions of this position. */
        @Override public int getDimension() {
            return coordinates.length;
        }

        /** Returns the coordinate value in given dimension. */
        @Override public double getCoordinate(final int dimension) {
            return coordinates[dimension];
        }

        /** Returns the CRS of this position, or {@code null} if unspecified. */
        @Override public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return crs;
        }
    }

    /**
     * Invoked in JavaFX thread for creating a copy of the given position together with related information.
     * The related information is the grid coverage slice where the given position can be evaluated.
     * The instance returned by this method will be given to {@link #evaluate(DirectPosition)}.
     *
     * @param  point  coordinates of the point for which to evaluate the grid coverage value.
     * @return a copy of the given point, augmented with the slice where the point can be evaluated.
     */
    @Override
    DirectPosition copy(final DirectPosition point) {
        assert Thread.holdsLock(this);
        final Position position = new Position(point, newSlice);
        newSlice = null;
        return position;
    }

    /**
     * Computes a string representation of data under the given position.
     * The position may be in any CRS; this method will convert coordinates as needed.
     * This method should be invoked in a background thread.
     *
     * @param  point  the cursor location in arbitrary CRS, or {@code null} if outside canvas region.
     * @return string representation of data under given position.
     *
     * @see GridCoverage.Evaluator#apply(DirectPosition)
     */
    @Override
    public String evaluate(final DirectPosition point) {
        synchronized (buffer) {
            buffer.setLength(0);
            if (point != null) try {
                final GridExtent slice = ((Position) point).newSlice;       // This cast should never fail.
                if (slice != null) {
                    evaluator.setDefaultSlice(slice.getSliceCoordinates());
                }
                final double[] results = evaluator.apply(point);
                if (results != null) {
                    final BitSet selection = selectedBands;
                    for (int i = -1; (i = selection.nextSetBit(i+1)) >= 0;) {
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
                            if (label != null) {
                                return label;
                            }
                        } catch (IllegalArgumentException e) {
                            recoverableException("evaluate", e);
                        }
                        sampleFormats[i].format(value, buffer, field).append(units[i]);
                    }
                    return buffer.toString();
                }
            } catch (CannotEvaluateException e) {
                recoverableException("evaluate", e);
            }
            /*
             * Point is considered outside coverage area.
             * We will write the sample dimension names.
             */
            return outsideText;
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
        return Numerics.tuple(MathFunctions.toNanOrdinal(value), band);
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
     * Message of the last exception, used for avoiding flooding the logger with repetitive errors.
     *
     * @see #recoverableException(String, Exception)
     */
    private String lastErrorMessage;

    /**
     * Invoked when an exception occurred while computing values.
     */
    private void recoverableException(final String method, final Exception e) {
        final String message = e.getMessage();
        if (!message.equals(lastErrorMessage)) {
            lastErrorMessage = message;
            Logging.recoverableException(LOGGER, ValuesUnderCursor.class, method, e);
        }
    }
}
