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
package org.apache.sis.storage.netcdf.base;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ucar.nc2.constants.CDM;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Time;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.Units;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.privy.Constants;


/**
 * Helper class for transforming a variable from one type to another type.
 * This is used mostly for converting dates encoded in some calendar form
 * to a number of temporal units since an epoch. Use cases:
 *
 * <ul>
 *   <li>Time axis with temporal coordinates encoded as character strings.</li>
 *   <li>Temporal coordinates encoded as numbers with "day as %Y%m%d.%f" pattern.</li>
 * </ul>
 *
 * In the current implementation, the special cases handled by this class
 * are detected solely from the unit of measurement.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-315">SIS-315</a>
 */
final class VariableTransformer {
    /**
     * Patterns of unit of measurements that need to be handled in a special way.
     * This enumeration contains special cases that we have met in practice.
     * The pattern are tested in the order of enumeration values.
     */
    private enum UnitPattern {
        /**
         * The pattern to use for identifying temporal units of the form "day as %Y%m%d.%f".
         * "%Y" is year formatted as at least four digits, "%m" is month formatted as two digits,
         * and "%d" is day of month formatted as two digits.
         * For example, 20181017.0000 stands for 2018-10-17.
         *
         * <p>In HYCOM files, the transformations needs to be applied on "ordinary" variables.
         * It should not be restricted to variables that are coordinate system axes.</p>
         */
        HYCOM("days?\\s+as\\s+(?-i)%Y%m%d.*", Units.DAY, false, (matcher) -> new int[] {100, 100}),

        /**
         * The pattern to use for identifying temporal units of the form "CCYYMMDDHHMMSS".
         * The "CC" prefix identifies climatological time, for data repeated every year.
         * The number of characters in each group determines the number of digits.
         * The actual values are sometime stored as character strings.
         *
         * <p>The transformation will be applied on variables that are coordinate system axes.</p>
         *
         * @see <a href="https://www.admiralty.co.uk/defence/additional-military-layers#Specifications">Additional
         *      Military Layers (<abbr>AML</abbr>)</a> — Integrated water column (<abbr>IWC</abbr>) Annex C
         */
        CLIMATOLOGICAL("C+Y+(M+)?(D+)?(H+)?(M+)?(S+)?", null, true, (matcher) -> {
            final String order = "MDHMS";
            final int count = matcher.groupCount();
            final int[] bases = new int[count];
            for (int i=0; i<count; i++) {
                String value = matcher.group(i+1);
                int n = value.length();
                if (--n < 0 || Character.toUpperCase(value.charAt(0)) != order.charAt(i)) {
                    return null;    // Fields out of order.
                }
                int base = 10;
                while (--n >= 0) {
                    if ((base *= 10) < 0) return null;      // Return null if overflow.
                }
                bases[i] = base;
            }
            return bases;
        });

        /**
         * The compiled pattern.
         */
        final Pattern pattern;

        /**
         * Units of measurement implied by the pattern,
         * or {@code null} if more analysis is needed for determining the unit.
         */
        final Unit<Time> unit;

        /**
         * A function providing values of 10ⁿ where <var>n</var> is the number of digits for the month, day, hour,
         * minute and second fields, in that order. The year field is omitted because implicit. If the dates have
         * no time of day, then the array length should be 2 instead of 5. Intermediate lengths are also allowed.
         * Returns {@code null} if the match is invalid.
         */
        final Function<Matcher, int[]> fieldBases;

        /**
         * Whether to restrict the match to variables that are identified as time axis. This is a safety for avoiding
         * false positive when the unit does not contain a clear keyword such as "days". We do not require time axis
         * in all cases because {@link #datesToTimeSinceEpoch}, for example, needs to be applied on "ordinary"
         * variables (i.e., variables that are not coordinate system axes).
         */
        final boolean requireTimeAxis;

        /**
         * Creates a new enumeration value.
         *
         * @param regex the regular expression to compile.
         * @param unit  units of measurement implied by the pattern, or {@code null}.
         */
        private UnitPattern(final String regex, final Unit<Time> unit, final boolean requireTimeAxis,
                            final Function<Matcher, int[]> fieldBases)
        {
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.requireTimeAxis = requireTimeAxis;
            this.fieldBases = fieldBases;
            this.unit = unit;
        }
    }

    /**
     * The decoder that produced the variables to transform.
     */
    private final Decoder decoder;

    /**
     * The matcher for each pattern to test, created when first needed.
     */
    private Matcher[] matchers;

    /**
     * Creates a new transformer for the given variable.
     *
     * @param  decoder the decoder that produced the variables to transform.
     */
    VariableTransformer(final Decoder decoder) {
        this.decoder = decoder;
    }

    /**
     * Analyzes and (if needed) transforms the given variable.
     * This method applies heuristic rules for detecting the transformation to apply.
     *
     * @param  variable  the variable to analyze and (if needed) to transform.
     */
    final void analyze(final Variable variable) throws IOException, DataStoreException {
        /*
         * Heuristic rule #1: accept only one-dimensional variables.
         * Character strings are two dimensional, but considered as
         * one dimensional after each string is parsed as a number.
         */
        switch (variable.getNumDimensions()) {
            default: return;
            case 1: break;
            case Variable.STRING_DIMENSION: {
                if (variable.getDataType() != DataType.CHAR) return;
                break;
            }
        }
        /*
         * Heuristic rule #2: identify the special case from the unit of measurement
         * and the axis type. The axis type is itself determined from the axis name.
         */
        final String units = variable.getUnitsString();
        if (units != null) {
            final UnitPattern[] candidates = UnitPattern.values();
            if (matchers == null) {
                matchers = new Matcher[candidates.length];
            }
            for (final UnitPattern candidate : candidates) {
                if (!candidate.requireTimeAxis || AxisType.valueOf(variable, false) == AxisType.T) {
                    Matcher matcher = matchers[candidate.ordinal()];
                    if (matcher == null) {
                        matcher = candidate.pattern.matcher(units);
                        matchers[candidate.ordinal()] = matcher;
                    } else {
                        matcher.reset(units);
                    }
                    if (matcher.matches()) {
                        final int[] fieldBases = candidate.fieldBases.apply(matcher);
                        if (fieldBases != null) {
                            datesToTimeSinceEpoch(variable, candidate.unit, fieldBases,
                                    candidate == UnitPattern.CLIMATOLOGICAL);
                        }
                    }
                }
            }
        }
    }

    /**
     * Converts from "Year Month Day Hours Minutes Seconds" pseudo-units to an amounts of time since the epoch.
     * The {@code fieldBases} argument gives the values of 10ⁿ where <var>n</var> is the number of digits for
     * the month, day, hour, minute and second fields, in that order. Trailing fields that do no exist should
     * be omitted or have their value set to 1.
     *
     * @param  variable        the variable to transform.
     * @param  unit            the target unit, or {@code null} keeping the current unit unchanged.
     * @param  fieldBases      number of digits in month, day, hour, minute, second fields, as powers of 10.
     * @param  climatological  whether a climatological calendar is used (data repeated each year).
     */
    private void datesToTimeSinceEpoch(final Variable variable, Unit<Time> unit, final int[] fieldBases,
            final boolean climatological) throws IOException, DataStoreException
    {
        final Unit<Time> unitOfLast;
        switch (fieldBases.length) {
            case 2:  unitOfLast = Units.DAY;    break;
            case 3:  unitOfLast = Units.HOUR;   break;
            case 4:  unitOfLast = Units.MINUTE; break;
            case 5:  unitOfLast = Units.SECOND; break;
            default: return;
        }
        /*
         * The unit and epoch sometime need to be taken from another variable.
         * In the following example from HYCOM, the real unit of `Date(MT)` will be taken from `MT(MT)`:
         *
         *     variables:
         *         double MT(MT)
         *             MT:long_name = "time"
         *             MT:units = "days since 1900-12-31 00:00:00"
         *             MT:axis = "T"
         *         double Date(MT)
         *             Date:long_name = "date"
         *             Date:units = "day as %Y%m%d.%f"
         *     data:
         *         MT = 43024
         *         Date = 20181017.0000
         */
        Instant epoch = Instant.EPOCH;
        final Variable axis = decoder.findVariable(variable.getGridDimensions().get(0).getName());
        if (axis != variable && axis != null) {
            Unit<?> t = axis.getUnit();     // Unconditional call because computes `epoch` as a side-effect.
            if (unit == null) try {
                unit = t.asType(Time.class);
            } catch (ClassCastException e) {
                decoder.illegalAttributeValue(CDM.UNITS, axis.getUnitsString(), e);
            }
            if (axis.epoch != null) {
                epoch = axis.epoch;         // Need to be after the call to `getUnit()`.
            }
        }
        if (unit == null) {
            unit = Units.DAY;
        }
        variable.setUnit(unit, epoch);
        /*
         * Prepares the conversion factors. The offset takes in account the change of epoch from Unix epoch
         * to the target epoch of the variable, including timezone shift. The `year` field is relevant only
         * if a climatological calendar is used, in which case we need a pseudo-year.
         */
        final int year = climatological ? LocalDate.ofInstant(epoch, ZoneOffset.UTC).getYear() : 0;
        double offset = epoch.toEpochMilli() + Constants.MILLIS_PER_SECOND * decoder.getTimeZone().getTotalSeconds();
        offset = Units.MILLISECOND.getConverterTo(unit).convert(offset);
        final UnitConverter toUnit = unitOfLast.getConverterTo(unit);
        /*
         * Convert all dates to amounts of time since the epoch. This code takes in account change
         * of epoch from the Unix epoch to the target epoch of the variable, including timezone.
         */
        int[] fields = new int[fieldBases.length];
        final Vector values = variable.read();
        final var times = new double[values.size()];
        for (int i=0; i < times.length; i++) {
            double value = values.doubleValue(i);    // Date encoded as a double (e.g. 20181017)
            long   time  = (long) value;             // Intentional rounding toward zero.
            value -= time;                           // Fractional part to be added at the end.
            for (int j = fields.length; --j >= 0;) {
                final int base = fieldBases[j];
                fields[j] = Math.abs((int) (time % base));    // Will apply the minus sign to the year field only.
                time /= base;
            }
            /*
             * After completion of the loop, `time` should be the year (potentially negative).
             * Convert to a number of days since the epoch, then add hours, minutes and seconds.
             *
             * TODO: if we provide an API for returning `java.time` objects, then we should build
             *       instances of `Month` or `MonthDay` below in the case of climatological data.
             */
            var date = LocalDate.of(Math.toIntExact(year + time), fields[0], fields[1]);
            time = date.toEpochDay();
            for (int j=2; j < fields.length; j++) {
                time *= (j == 2) ? 24 : 60;
                time += fields[j];
            }
            value += time - offset;
            times[i] = toUnit.convert(value);
        }
        variable.setValues(times, true);
    }
}
