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
package org.apache.sis.math;

import org.apache.sis.util.Static;
import org.apache.sis.util.Workaround;
import org.apache.sis.internal.util.Numerics;

import static org.apache.sis.internal.util.Numerics.SIGNIFICAND_SIZE;


/**
 * Functions working of {@code float} and {@code double} values while taking in account the representation in base 10.
 * Methods in this class may be helpful when used immediately after parsing (i.e. before any calculations are applied
 * on the value), or just before formatting a number in base 10. Methods in this class are usually <strong>not</strong>
 * recommended for intermediate calculations, since base 10 is not more "real" than base 2 for natural phenomenon.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.util.Numbers
 */
public final class DecimalFunctions extends Static {
    /**
     * The greatest power of 10 such as {@code Math.pow(10, E10_FOR_ZERO) == 0}.
     * This is the exponent in {@code parseDouble("1E-324")} &lt; {@link Double#MIN_VALUE},
     * which is stored as zero because non-representable as a {@code double} value.
     * The next power, {@code parseDouble("1E-323")}, is a non-zero {@code double} value.
     *
     * @see Double#MIN_VALUE
     */
    static final int EXPONENT_FOR_ZERO = -324;

    /**
     * The maximal exponent value such as {@code parseDouble("1E+308")} still a finite number.
     *
     * @see Double#MAX_VALUE
     */
    static final int EXPONENT_FOR_MAX = 308;

    /**
     * Table of integer powers of 10, precomputed both for performance and accuracy reasons.
     * This table consumes 4.9 kb of memory. We pay this cost because integer powers of ten
     * are requested often, and {@link Math#pow(double, double)} has slight rounding errors.
     *
     * @see #pow10(int)
     */
    @Workaround(library="JDK", version="1.4")
    private static final double[] POW10 = new double[EXPONENT_FOR_MAX - EXPONENT_FOR_ZERO];
    static {
        final StringBuilder buffer = new StringBuilder("1E");
        for (int i=0; i<POW10.length; i++) {
            buffer.setLength(2);
            buffer.append(i + (EXPONENT_FOR_ZERO + 1));
            /*
             * Double.parseDouble("1E"+i) gives as good or better numbers than Math.pow(10,i)
             * for ALL integer powers, but is slower. We hope that the current workaround is only
             * temporary. See http://developer.java.sun.com/developer/bugParade/bugs/4358794.html
             */
            POW10[i] = Double.parseDouble(buffer.toString());
        }
    }

    /**
     * Do not allow instantiation of this class.
     */
    private DecimalFunctions() {
    }

    /**
     * Computes 10 raised to the power of <var>x</var>. This is the implementation of the
     * public {@link MathFunctions#pow10(int)} method, defined here in order to allow the
     * JVM to initialize the {@link #POW10} table only when first needed.
     *
     * @param x The exponent.
     * @return 10 raised to the given exponent.
     */
    static double pow10(int x) {
        x -= EXPONENT_FOR_ZERO + 1;
        return (x >= 0) ? (x < POW10.length ? POW10[x] : Double.POSITIVE_INFINITY) : 0;
    }

    /**
     * Converts the given {@code float} value to a {@code double} with the extra <em>decimal</em> fraction digits
     * set to zero. This is different than the standard cast in the Java language, which set the extra <em>binary</em>
     * fraction digits to zero.
     * For example {@code (double) 0.1f} gives 0.10000000149011612 while {@code floatToDouble(0.1f)} returns 0.1.
     *
     * {@note This method is <strong>not</strong> more accurate than the standard Java cast —
     *        it is only more intuitive for human used to base 10.
     *        If the value come from a call to <code>Float.parseFloat(String)</code> (directly or indirectly),
     *        and if that call can not be replaced by a call to <code>Double.parseDouble(String)</code>, then
     *        this method may be useful since the definitive <code>String</code> value was expressed in base 10.
     *        But if the value come from an instrument measurement or a calculation, then there is probably
     *        no reason to use this method because base 10 is not more "real" than base 2 or any other base
     *        for natural phenomenon.
     *
     *        <p><b>Use case:</b></p>
     *        <ul>
     *          <li>Producer A provides data in ASCII files using less fraction digits than the <code>float</code>
     *              storage capability. This is not uncommon when the primary accuracy constraint is the instrument
     *              precision.</li>
     *          <li>Producer B converts the above ASCII files to the NetCDF binary format for efficiency, with data
     *              stored as <code>float</code> values. Producer B does not distribute the original ASCII files.</li>
     *          <li>Client of producer B wants to use those data with a library working with <code>double</code> values.
     *              For some reason (e.g. formatting), the client wants the same values as if the ASCII files had been
     *              parsed with <code>Double.parseDouble(String)</code> in the first place.</li>
     *        </ul>}
     *
     * This method is equivalent to the following code, except that it is potentially faster since the
     * actual implementation avoid to format and parse the value:
     *
     * {@preformat java
     *   return Double.parseDouble(Float.toString(value));
     * }
     *
     * @param  value The {@code float} value to convert as a {@code double}.
     * @return The given value as a {@code double} with the extra decimal fraction digits set to zero.
     */
    public static double floatToDouble(final float value) {
        if (Float.isNaN(value) || Float.isInfinite(value) || value == 0f) {
            return value;
        }
        /*
         * Decompose  value == m × 2^e  where m and e are integers. If the exponent is not negative, then
         * there is no fractional part in the value, in which case there is no rounding error to fix.
         */
        final int e = Math.getExponent(value) - Numerics.SIGNIFICAND_SIZE_OF_FLOAT;
        if (e >= 0) {
            return value;
        }
        final int m = Numerics.getSignificand(value);
        assert Math.scalb((float) m, e) == value : value;
        /*
         * Get the factor for converting the significand from base 2 to base 10, such as:
         *
         *    m × (2^e)  ==  m × c × (10 ^ -e10)
         *
         * where e10 is the smallest exponent which allow to represent the value without precision lost when (m × c)
         * is rounded to an integer. Because the number of significant digits in base 2 does not correspond to an
         * integer number of significand digits in base 10, we have slightly more precision than what the 'float'
         * value had: we have something between 0 and 1 extraneous digits.
         */
        final int    e10 = -Numerics.toExp10(e);      // Make positive
        final double c   = Math.scalb(pow10(e10), e); // Conversion factor, also 1 ULP in base 10.
        final double mc  = m * c;
        /*
         * First, presume that our representation in base 10 has one extranous digit, so we will round
         * to the tens instead of unities. If the difference appears to not be smaller than half a ULP,
         * then the last digit was not extranous - we need to keep it.
         */
        double r = Math.rint(mc / 10) * 10;
        if (Math.abs(r - mc) >= c/2) {
            r = Math.rint(mc);
        }
        return Math.scalb(r / c, e);
    }

    /**
     * Returns the number of fraction digits needed for formatting in base 10 numbers of the given
     * accuracy. If the {@code strict} argument is {@code true}, then for any given {@code accuracy}
     * this method returns a value <var>n</var> such as the difference between adjacent numbers
     * formatted in base 10 with <var>n</var> fraction digits will always be equal or smaller
     * than {@code accuracy}. Examples:
     *
     * <ul>
     *   <li>{@code fractionDigitsForDelta(0.001, true)} returns 3.</li>
     *   <li>{@code fractionDigitsForDelta(0.009, true)} returns 3.</li>
     *   <li>{@code fractionDigitsForDelta(0.010, true)} returns 2.</li>
     *   <li>{@code fractionDigitsForDelta(0.099, true)} returns 3 (special case).</li>
     * </ul>
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>If {@code accuracy} is {@link Double#NaN NaN} or infinity, then this method returns 0
     *       since those values are not represented by decimal digits.</li>
     *
     *   <li>If {@code accuracy} is 0, then this method returns 324 since 10<sup>-324</sup> is the first power of 10
     *       smaller than the minimal strictly positive {@code double} value ({@value java.lang.Double#MIN_VALUE}).
     *
     *       {@note The above value can be understood in an other way: if the first 324 fraction digits are zero,
     *              then the IEEE <code>double</code> value is guaranteed to be rounded to exactly 0 no matter
     *              what the next fraction digits are.}</li>
     *
     *   <li>If {@code accuracy} is greater than 1, then this method returns
     *       the number of "unnecessary" trailing zeros as a negative number.
     *
     *       {@example <code>fractionDigitsForDelta(100, …)</code> returns -2.}</li>
     *
     *   <li>If the first non-zero digits of {@code accuracy} are equal or greater than 95
     *       (e.g. 0.00099) and the {@code strict} argument is {@code true}, then this method
     *       increases the number of needed fraction digits in order to prevent the rounded
     *       number to be collapsed into the next integer value.
     *
     *       {@example
     *       If <code>accuracy</code> is 0.95, then a return value of 1 is not sufficient since
     *       the rounded value of 0.95 with 1 fraction digit would be 1.0. Such value would be a
     *       violation of this method contract since the difference between 0 and that formatted
     *       value would be greater than the accuracy. Note that this is not an artificial rule;
     *       this is related to the fact that 0.9999… is mathematically strictly equals to 1.}</li>
     * </ul>
     *
     * <p>Invoking this method is equivalent to computing <code>(int)
     * -{@linkplain Math#floor(double) floor}({@linkplain Math#log10(double) log10}(accuracy))</code>
     * except for the 0, {@code NaN}, infinities and {@code 0.…95} special cases.</p>
     *
     * @param  accuracy The desired accuracy of numbers to format in base 10.
     * @param  strict {@code true} for checking the {@code 0.…95} special case.
     *         If {@code false}, then the difference between adjacent formatted numbers is not
     *         guaranteed to be smaller than {@code accuracy} in every cases.
     * @return Number of fraction digits needed for formatting numbers with the given accuracy.
     *         May be negative.
     *
     * @see java.text.NumberFormat#setMaximumFractionDigits(int)
     */
    public static int fractionDigitsForDelta(double accuracy, final boolean strict) {
        accuracy = Math.abs(accuracy);
        int i = MathFunctions.getExponent(accuracy);
        if (i == Double.MAX_EXPONENT + 1) {
            return 0; // NaN or infinities.
        }
        i = Numerics.toExp10(i);
        if (accuracy >= pow10(i+1)) {
            i++;
        }
        i = -i;
        if (strict) {
            double scale = pow10(i);
            while ((accuracy *= scale) >= 9.5) {
                i++; // The 0.…95 special case.
                accuracy -= Math.floor(accuracy);
                scale = 10;
            }
        }
        return i;
    }

    /**
     * Returns the number of significant fraction digits when formatting the given number in base 10.
     * This method does <strong>not</strong> ignore trailing zeros.
     * For example {@code fractionDigitsForValue(1.0)} returns 16,
     * because the {@code double} format can store <i>almost</i> 16 decimal digits after 1.
     *
     * {@note We said <i>almost</i> because the very last digit may be able to store only a subset of the
     *        [0 … 9] digits.}
     *
     * Invoking this method is equivalent to invoking <code>{@linkplain #fractionDigitsForDelta(double, boolean)
     * fractionDigitsForDelta}(Math.{@linkplain Math#ulp(double) ulp}(value), false)</code>, except that it is
     * potentially faster.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>If {@code value} is {@link Double#NaN NaN} or infinity, then this method returns 0
     *       since those values are not represented by decimal digits.</li>
     *
     *   <li>If {@code value} is 0, then this method returns 324 since
     *       {@code Math.ulp(0)} = {@value java.lang.Double#MIN_VALUE}.</li>
     * </ul>
     *
     * {@example This method is useful with <code>NumberFormat</code> for formatting all significant digits
     *           of a <code>double</code> value, padding with trailing zeros if necessary, but no more than
     *           necessary.}
     *
     * @param  value The value for which to get the number of significant digits.
     * @return The number of significant digits (may be negative), or 0 if {@code value} is NaN or infinity.
     *
     * @see java.text.NumberFormat#setMinimumFractionDigits(int)
     */
    public static int fractionDigitsForValue(final double value) {
        /*
         * We really need Math.getExponent(value) here rather than MathFunctions.getExponent(value).
         * What we actually want is MathFunctions.getExponent(Math.ulp(value)), but we get the same
         * result more efficiently if we replace the call to Math.ulp(double) by a SIGNIFICAND_SIZE
         * subtraction in the exponent, provided that the exponent has NOT been corrected for sub-
         * normal numbers (in order to reproduce the Math.ulp behavior).
         */
        final int exponent = Math.getExponent(value);
        if (exponent <= Double.MAX_EXPONENT) { // Exclude NaN and ±∞ cases.
            return -Numerics.toExp10(exponent - SIGNIFICAND_SIZE);
        }
        return 0;
    }
}
