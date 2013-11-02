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

import java.util.Arrays;
import org.apache.sis.util.Static;
import org.apache.sis.util.Workaround;
import org.apache.sis.internal.util.Numerics;

import static org.apache.sis.internal.util.Numerics.SIGNIFICAND_SIZE;


/**
 * Functions working of {@code float} and {@code double} values while taking in account the representation in base 10.
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
     * Table of some integer powers of 10. Used for faster computation in {@link #pow10(int)}.
     *
     * @see #pow10(int)
     */
    private static final double[] POW10 = {
        1E+00, 1E+01, 1E+02, 1E+03, 1E+04, 1E+05, 1E+06, 1E+07, 1E+08, 1E+09,
        1E+10, 1E+11, 1E+12, 1E+13, 1E+14, 1E+15, 1E+16, 1E+17, 1E+18, 1E+19,
        1E+20, 1E+21, 1E+22
        // Do not add more elements, unless we verified that 1/x is accurate.
        // Last time we tried, it was not accurate anymore starting at 1E+23.
    };

    /**
     * Do not allow instantiation of this class.
     */
    private DecimalFunctions() {
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
        final boolean isFraction = (accuracy < 1);
        /*
         * Compute (int) Math.log10(x) with opportunist use of the POW10 array.
         * We use the POW10 array because accurate calculation of log10 is relatively costly,
         * while we only want the integer part. A micro-benchmarking on JDK7 suggested that a
         * binary search on POW10 is about 30% faster than invoking (int) Math.log10(x).
         */
        int i = Arrays.binarySearch(POW10, isFraction ? 1/accuracy : accuracy);
        if (i >= 0) {
            return isFraction ? i : -i;
        }
        i = ~i;
        double scale;
        if (i < POW10.length) {
            scale = POW10[i];
            if (!isFraction) {
                i = -(i-1);
                scale = 10 / scale;
            }
        } else { // 'x' is out of range or NaN.
            final double y = Math.log10(accuracy);
            if (Double.isInfinite(y)) {
                return (accuracy == 0) ? -EXPONENT_FOR_ZERO : 0;
            }
            i = -((int) Math.floor(y));
            scale = pow10(i);
        }
        if (strict) {
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

    /**
     * Computes 10 raised to the power of <var>x</var>. This method delegates to
     * <code>{@linkplain #pow10(int) pow10}((int) x)</code> if <var>x</var> is an
     * integer, or to <code>{@linkplain Math#pow(double, double) Math.pow}(10, x)</code>
     * otherwise.
     *
     * @param x The exponent.
     * @return 10 raised to the given exponent.
     *
     * @see #pow10(int)
     * @see Math#pow(double, double)
     */
    static double pow10(final double x) {
        final int ix = (int) x;
        if (ix == x) {
            return pow10(ix);
        } else {
            return Math.pow(10, x);
        }
    }

    /**
     * Computes 10 raised to the power of <var>x</var>. This method tries to be slightly more
     * accurate than <code>{@linkplain Math#pow(double, double) Math.pow}(10, x)</code>,
     * sometime at the cost of performance.
     *
     * {@note This method has been defined because the standard <code>Math.pow(10, x)</code>
     *        method does not always return the closest IEEE floating point representation.
     *        Slight departures (1 or 2 ULP) are often allowed in math functions for performance
     *        reasons. The most accurate calculations are usually not necessary, but the base 10
     *        is a special case since it is used for scaling axes or formatting human-readable
     *        output.}
     *
     * @param x The exponent.
     * @return 10 raised to the given exponent.
     */
    @Workaround(library="JDK", version="1.4")
    static double pow10(final int x) {
        if (x >= 0) {
            if (x < POW10.length) {
                return POW10[x];
            }
        } else if (x != Integer.MIN_VALUE) {
            final int nx = -x;
            if (nx < POW10.length) {
                return 1 / POW10[nx];
            }
        }
        try {
            /*
             * Double.parseDouble("1E"+x) gives as good or better numbers than Math.pow(10,x)
             * for ALL integer powers, but is slower. We hope that the current workaround is only
             * temporary. See http://developer.java.sun.com/developer/bugParade/bugs/4358794.html
             */
            return Double.parseDouble("1E" + x);
        } catch (NumberFormatException exception) {
            return StrictMath.pow(10, x);
        }
    }
}
