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
package org.apache.sis.measure;

import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Angle;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.math.MathFunctions;
import static org.apache.sis.math.MathFunctions.truncate;
import static org.apache.sis.pending.jdk.JDK19.DOUBLE_PRECISION;


/**
 * A converter from decimal degrees to sexagesimal degrees. Sexagesimal degrees are pseudo-unit
 * in the <i>sign - degrees - decimal point - minutes (two digits) - integer seconds (two digits) -
 * fraction of seconds (any precision)</i> format.
 *
 * <p>When possible, Apache SIS always handles angles in radians, decimal degrees or any other proportional units.
 * Sexagesimal angles are considered a string representation issue (handled by {@link AngleFormat}) rather than a
 * unit issue. Unfortunately, this pseudo-unit is extensively used in the EPSG database, so we have to support it.</p>
 *
 * <h2>Immutability and thread safety</h2>
 * This class and all inner classes are immutable, and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
class SexagesimalConverter extends AbstractConverter {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -2119974989555436361L;

    /**
     * Small tolerance factor when comparing numbers close to 1.
     * For comparing numbers other than 1, multiply by the number magnitude.
     */
    private static final double EPS = 1E-10;

    /**
     * Pseudo-unit for sexagesimal degree. Numbers in this pseudo-unit have the following format:
     *
     * <i>sign - degrees - decimal point - minutes (two digits) - fraction of minutes (any precision)</i>.
     *
     * Using this unit is loosely equivalent to formatting decimal degrees with the
     * {@code "D.MMm"} {@link AngleFormat} pattern.
     *
     * <p>This unit is non-linear and not practical for computation. Consequently, it should be
     * avoided as much as possible. This pseudo-unit is defined only because used in the EPSG
     * database (code 9111).</p>
     */
    static final ConventionalUnit<Angle> DM;

    /**
     * Pseudo-unit for sexagesimal degree. Numbers in this pseudo-unit have the following format:
     *
     * <i>sign - degrees - decimal point - minutes (two digits) - integer seconds (two digits) -
     * fraction of seconds (any precision)</i>.
     *
     * Using this unit is loosely equivalent to formatting decimal degrees with the
     * {@code "D.MMSSs"} {@link AngleFormat} pattern.
     *
     * <p>This unit is non-linear and not practical for computation. Consequently, it should be
     * avoided as much as possible. This pseudo-unit is defined only because extensively used in
     * the EPSG database (code 9110).</p>
     */
    static final Unit<Angle> DMS;

    /**
     * Pseudo-unit for degree - minute - second.
     * Numbers in this pseudo-unit have the following format:
     *
     * <i>signed degrees (integer) - arc-minutes (integer) - arc-seconds
     * (real, any precision)</i>.
     *
     * Using this unit is loosely equivalent to formatting decimal degrees with the
     * {@code "DMMSS.s"} {@link AngleFormat} pattern.
     *
     * <p>This unit is non-linear and not practical for computation. Consequently, it should be
     * avoided as much as possible. This pseudo-unit is defined only because extensively used in
     * EPSG database (code 9107).</p>
     */
    static final Unit<Angle> DMS_SCALED;
    static {
        final SystemUnit<Angle> rad = (SystemUnit<Angle>) Units.RADIAN;
        final UnitConverter toRadian = Units.DEGREE.getConverterTo(rad);
        DM = new ConventionalUnit<>(rad, new ConcatenatedConverter(
                new SexagesimalConverter(false, 100).inverse(), toRadian), "D.M", UnitRegistry.OTHER, (short) 9111);

        DMS = new ConventionalUnit<>(rad, new ConcatenatedConverter(
                new SexagesimalConverter(true, 10000).inverse(), toRadian), "D.MS", UnitRegistry.OTHER, (short) 9110);

        DMS_SCALED = new ConventionalUnit<>(rad, new ConcatenatedConverter(
                new SexagesimalConverter(true, 1).inverse(), toRadian), "DMS", UnitRegistry.OTHER, (short) 9107);
    }

    /**
     * {@code true} if the seconds field is present.
     */
    final boolean hasSeconds;

    /**
     * The value to divide DMS unit by.
     * For "degree minute second" (EPSG code 9107), this is 1.
     * For "sexagesimal degree" (EPSG code 9110), this is 10000.
     */
    final double divider;

    /**
     * The inverse of this converter.
     */
    private final SexagesimalConverter inverse;

    /**
     * Constructs a converter for sexagesimal units.
     *
     * @param hasSeconds  {@code true} if the seconds field is present.
     * @param divider     the value to divide DMS unit by.
     *        For "degree minute second" (EPSG code 9107), this is 1.
     *        For "sexagesimal degree" (EPSG code 9110), this is 10000.
     */
    private SexagesimalConverter(final boolean hasSeconds, final double divider) {
        this.hasSeconds = hasSeconds;
        this.divider    = divider;
        this.inverse    = new Inverse(this);
    }

    /**
     * Constructs a converter for sexagesimal units.
     * This constructor is for {@link Inverse} usage only.
     */
    private SexagesimalConverter(final SexagesimalConverter inverse) {
        this.hasSeconds = inverse.hasSeconds;
        this.divider    = inverse.divider;
        this.inverse    = inverse;
    }

    /**
     * Returns the inverse of this converter.
     */
    @Override
    public final UnitConverter inverse() {
        return inverse;
    }

    /**
     * Performs a conversion from fractional degrees to sexagesimal degrees.
     */
    @Override
    public double convert(double angle) {
        final double deg = truncate(angle);
        angle = (angle - deg) * 60;
        if (hasSeconds) {
            final double min = truncate(angle);
            angle  = (angle - min) * 60;                // Secondes
            angle += (deg*100 + min)*100;
        } else {
            angle += deg * 100;
        }
        return angle / divider;
    }

    /**
     * Considers this converter as non-derivable. Actually it would be possible to provide a derivative value
     * for input values other than the discontinuities points, but for now we presume that it is less dangerous
     * to return NaN every time, so the user cannot miss that this function is not derivable everywhere.
     */
    @Override
    public final double derivative(double value) {
        return Double.NaN;
    }

    /**
     * Compares this converter with the specified object.
     */
    @Override
    public final boolean equals(final Object object) {
        return object != null && object.getClass() == getClass() &&
                ((SexagesimalConverter) object).divider == divider;
    }

    /**
     * Returns a hash value for this converter.
     */
    @Override
    public final int hashCode() {
        return ((int) divider) ^ getClass().hashCode();
    }

    /**
     * The inverse of {@link SexagesimalConverter}, i.e. the converter from sexagesimal degrees to decimal degrees.
     */
    private static final class Inverse extends SexagesimalConverter {
        /**
         * Serial number for compatibility with different versions.
         */
        private static final long serialVersionUID = -1928146841653975281L;

        /**
         * Constructs a converter.
         */
        public Inverse(final SexagesimalConverter inverse) {
            super(inverse);
        }

        /**
         * After calculation of the remaining seconds or minutes, trims the rounding errors presumably
         * caused by rounding errors in floating point arithmetic. This is required for avoiding the
         * following conversion issue:
         *
         * <ol>
         *   <li>Sexagesimal value: 46.570866 (from 46°57'8.66"N in EPSG:2056 projected CRS)</li>
         *   <li>value * 10000 = 465708.66000000003</li>
         *   <li>deg = 46, min = 57, deg = 8.660000000032596</li>
         * </ol>
         *
         * We perform a rounding based on the representation in base 10 because extractions of degrees and
         * minutes fields from the sexagesimal value themselves use arithmetic in base 10. This conversion
         * is used in contexts where the sexagesimal value, as shown in a number in base 10, is definitive.
         *
         * @param  remainder  the value to fix, after other fields (degrees and/or minutes) have been subtracted.
         * @param  magnitude  value of {@code remainder} before the degrees and/or minutes were subtracted.
         */
        private static double fixRoundingError(double remainder, final double magnitude) {
            /*
             * We use 1 ULP because the double value parsed from a string representation was at 0.5 ULP
             * from the real value, and the multiplication by 'divider' add another 0.5 ULP rounding error.
             * Removal of degrees and/or minutes fields as integers do not add rounding errors.
             */
            int p = Math.getExponent(Math.ulp(magnitude));          // Power of 2 (negative for fractional value).
            if (p < 0 && p > -DOUBLE_PRECISION) {                   // Precision is a fraction digit >= Math.ulp(1).
                p = Numerics.toExp10(-p);                           // Positive power of 10, rounded to lower value.
                final double scale = MathFunctions.pow10(p);
                remainder = Math.rint(remainder * scale) / scale;
            }
            return remainder;
        }

        /**
         * Performs a conversion from sexagesimal degrees to fractional degrees.
         *
         * @throws IllegalArgumentException If the given angle cannot be converted.
         */
        @Override
        public double convert(final double angle) throws IllegalArgumentException {
            double deg,min,sec,mgn;
            if (hasSeconds) {
                sec = mgn = angle * divider;
                deg = truncate(sec/10000); sec -= 10000*deg;
                min = truncate(sec/  100); sec -=   100*min;
                sec = fixRoundingError(sec, mgn);
            } else {
                sec = 0;
                min = mgn = angle * divider;
                deg = truncate(min / 100);
                min -= deg * 100;
                min = fixRoundingError(min, mgn);
            }
            if (min <= -60 || min >= 60) {                              // Do not enter for NaN
                if (Math.abs(Math.abs(min) - 100) <= (EPS * 100)) {
                    if (min >= 0) deg++; else deg--;
                    min = 0;
                } else {
                    throw illegalField(angle, min, 0);
                }
            }
            if (sec <= -60 || sec >= 60) {                              // Do not enter for NaN
                if (Math.abs(Math.abs(sec) - 100) <= (EPS * 100)) {
                    if (sec >= 0) min++; else min--;
                    sec = 0;
                } else {
                    throw illegalField(angle, sec, 1);
                }
            }
            return (sec/60 + min)/60 + deg;
        }

        /**
         * Creates an exception for an illegal field.
         *
         * @param  value  the user supplied angle value.
         * @param  field  the value of the illegal field.
         * @param  unit   0 for minutes or 1 for seconds.
         * @return the exception to throw.
         */
        private static IllegalArgumentException illegalField(final double value, final double field, final int unit) {
            return new IllegalArgumentException(Errors.format(Errors.Keys.IllegalSexagesimalField_3, value, unit, field));
        }
    }
}
