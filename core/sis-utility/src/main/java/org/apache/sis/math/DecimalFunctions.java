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
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.util.Numerics;

import static org.apache.sis.internal.util.Numerics.SIGNIFICAND_SIZE;


/**
 * Functions working on {@code float} and {@code double} values while taking in account their representation in base 10.
 * Methods in this class may be helpful when used after parsing or before formatting a number in base 10:
 *
 * <ul>
 *   <li>Post-parsing methods {@link #floatToDouble(float)} and {@link #deltaForDoubleToDecimal(double)}:
 *     <ul>
 *       <li>for compensating error when the base 10 representation is considered <cite>definitive</cite>.</li>
 *     </ul>
 *   </li>
 *   <li>Pre-formatting methods {@link #fractionDigitsForValue(double)} and
 *       {@link #fractionDigitsForDelta(double, boolean)}:
 *     <ul>
 *       <li>for formatting numbers using the exact amount of significant digits for a given precision.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * Methods in this class are usually <strong>not</strong> recommended for intermediate calculations,
 * since base 10 is not more "real" than base 2 for natural phenomenon.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see MathFunctions#pow10(int)
 * @see Math#log10(double)
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
     * <div class="note"><b>Note:</b>
     * This method is <strong>not</strong> more accurate than the standard Java cast – it should be used only when
     * the base 10 representation of the given value may be of special interest. If the value come from a call to
     * {@link Float#parseFloat(String)} (directly or indirectly), and if that call can not be replaced by a call to
     * {@link Double#parseDouble(String)} (for example because the original {@code String} is not available anymore),
     * then this method may be useful if one consider the {@code String} representation in base 10 as definitive.
     * But if the value come from an instrument measurement or a calculation, then there is probably no reason to use
     * this method because base 10 is not more "real" than base 2 or any other base for natural phenomenon.</div>
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
        /*
         * Decompose  value == m × 2^e  where m and e are integers. If the exponent is not negative, then
         * there is no fractional part in the value, in which case there is no rounding error to fix.
         * (Note: NaN and infinities also have exponent greater than zero).
         */
        final int e = Math.getExponent(value) - Numerics.SIGNIFICAND_SIZE_OF_FLOAT;
        if (e >= 0) {
            return value; // Integer, infinity or NaN.
        }
        final int m = Numerics.getSignificand(value);
        assert Math.scalb((float) m, e) == Math.abs(value) : value;
        /*
         * Get the factor c for converting the significand m from base 2 to base 10, such as:
         *
         *    m × (2 ^ e)  ==  m × c × (10 ^ -e₁₀)
         *
         * where e₁₀ is the smallest exponent which allow to represent the value without precision lost when (m × c)
         * is rounded to an integer. Because the number of significant digits in base 2 does not correspond to an
         * integer number of significand digits in base 10, we have slightly more precision than what the 'float'
         * value had: we have something between 0 and 1 extraneous digits.
         *
         * Note: the conversation factor c is also equals to 1 ULP converted to the units of (m × c).
         */
        final int    e10 = -Numerics.toExp10(e);        // Range: [0 … 45] inclusive.
        final double c   = Math.scalb(pow10(e10), e);   // Range: (1 … 10) exclusive.
        final double mc  = m * c;                       // Only integer part is meaningful.
        /*
         * First, presume that our representation in base 10 has one extranous digit, so we will round
         * to the tens instead of unities. If the difference appears to not be smaller than half a ULP,
         * then the last digit was not extranous - we need to keep it.
         */
        double r = Math.rint(mc / 10) * 10;
        if (Math.abs(r - mc) >= c/2) {
            r = Math.rint(mc);
        }
        r = Math.copySign(Math.scalb(r / c, e), value);
        assert value == (float) r : value;
        return r;
    }

    /**
     * Returns the difference between the given {@code double} value and the representation of that value in base 10.
     * For any value in the method's domain of validity (defined below), this method is <em>approximatively</em>
     * equivalent to the following code except that it is potentially faster since the actual implementation
     * avoids the creation of {@link java.math.BigDecimal} objects:
     *
     * {@preformat java
     *   BigDecimal base2  = new BigDecimal(value);     // Exact same value as stored in IEEE 754 format.
     *   BigDecimal base10 = BigDecimal.valueOf(value); // Exact same value as shown by println(value).
     *   return base10.subtract(base2).doubleValue();
     * }
     *
     * Computing {@code value + deltaForDoubleToDecimal(value)} has no effect since the absolute value of the
     * returned delta is always smaller than <code>{@linkplain Math#ulp(double) Math.ulp}(value) / 2</code>.
     * To see an effect, a type with more precision than the {@code double} type is necessary.
     *
     * <div class="note"><b>Use case:</b>
     * Many international standards define values in base 10. For example the conversion factor from inches
     * to centimetres is defined as exactly 2.54 cm/inch. This is by an internationally accepted definition
     * since 1959, not an approximation. But the 2.54 value can not be represented exactly in the IEEE 754
     * format – the error is approximatively 3.6E-17 cm/inch. In the vast majority of cases such tiny error
     * can be ignored. But in situations where it is desirable to have an error estimation
     * (e.g. in non-linear equations where errors can grow exponentially), this method can be useful.
     * Other examples of values defined in base 10 are conversions from feet to metres and
     * map projection parameters defined by national mapping agencies.
     * </div>
     *
     * <div class="section">Domain of validity</div>
     * The current implementation can not compute delta for {@code abs(value) < 3E-8} approximatively,
     * except for the 0 value which is supported. For any non-zero value closer to zero than the 3E-8
     * threshold, this method returns {@code NaN} because of insufficient algorithm accuracy.
     * This limitation may change in any future SIS version if we find a better algorithm.
     *
     * @param  value The value for which to get the delta compared to its base 10 representation.
     * @return The delta that would need to be added to the given {@code double} value for getting a result
     *         closer to its base 10 representation, or {@link Double#NaN NaN} if it can not be computed.
     */
    public static double deltaForDoubleToDecimal(final double value) {
        /*
         * Decompose  value == m × 2^e  where m and e are integers, then get the
         * factor c for converting the significand m from base 2 to base 10 such as:
         *
         *    m × (2 ^ e)  ==  m × c × (10 ^ -e₁₀)
         */
        final int e = Math.getExponent(value) - SIGNIFICAND_SIZE;
        if (e >= 0) {
            return 0; // Integer, infinity or NaN.
        }
        if (e < -24 - SIGNIFICAND_SIZE) {         // 2.9802322E-8 threshold found empirically.
            return (e == -1075) ? 0 : Double.NaN; // Returns 0 for the 0 value, NaN for all others.
        }
        final long m = Numerics.getSignificand(value);
        assert Math.scalb((double) m, e) == Math.abs(value) : value;
        final int e10 = -Numerics.toExp10(e); // Range: [0 … 324] inclusive.
        /*
         * If we were continuing with the same strategy than in floatToDouble(float), we would compute:
         *
         *    c = Math.scalb(pow10(e10), e);  // Range: (1 … 10) exclusive.
         *
         * Unfortunately this would require a floating point type with twice the accuracy of 'double',
         * which we don't have. Instead, we will apply a trick with integer arithmetic. But before to
         * use integers, we will need to multiply 'c' by a scale factor high enough for promoting all
         * significant fraction digits to the integer part. This factor is 2^56, explanation follows:
         *
         * For reasons explained later we will actually use c/10 instead of c, so the range will be
         * (0.1 … 1) instead of (1 … 10). The exponent of 0.1 in base 2 is -4. Consequently for any
         * value between 0.1 and 1, scaling the value by 56 binary places guarantee that the result
         * will be equals or greater than 2^52. At that threshold, 'double' values can not have
         * fraction digits.
         */
        final int PRECISION = SIGNIFICAND_SIZE + 4;            // Number of bits to use for scaling to integers.
        double cs = Math.scalb(pow10(e10 - 1), e + PRECISION); // Range: (0.1 × 2^56  …  2^56) exclusive.
        /*
         * This is where magic happen: the following multiplication overflow (we would need a 128 bits integer
         * for representing it), but we don't care because we are interrested only in the fraction digits (the
         * 56 lower bits because of the scaling discussed in previous comment). In integer arithmetic, the low
         * bits are always valid even if the multiplication overflow.
         */
        final long ci = (long) cs;
        long mc = (m * ci) & ((1L << PRECISION) - 1);
        /*
         * Because we used c/10 instead than c,  the first (leftmost) decimal digit is potentially the last
         * (rightmost) decimal digit of 'value'. Whether it is really the last 'value' digit or not depends
         * on the magnitude of last decimal digit compared to 1 ULP. The last digit is:
         *
         *    lastDigit = (mc * 10) >>> PRECISION;
         *
         * The above digit is guaranteed to be in the [0 … 9] range. We will wraparound in the [-5 … 4] range
         * because the delta must be no more than ±0.5 ULP of the 'value' argument.
         */
        if (mc >= (5L << PRECISION) / 10) { // The  0.5 × 2^56  threshold.
            mc -= (1L << PRECISION);        // Shift [5 … 9] digits to [-5 … -1].
        }
        /*
         * At this point, 'mc' is less than 0.5 ULP if the last decimal digits were zero.
         * But if the 'value' argument uses the full precision range of the 'double' type,
         * then we still have 'mc' greater than 0.5 ULP is some cases. This difficulty happens
         * because 52 binary digits do not correspond to an integer number of decimal digits.
         *
         * For now, I didn't found a better strategy than choosing the last decimal digit in such
         * a way that 'mc' is as small as possible. We don't really care what this last digit is;
         * we care only about the remainder after we removed the effet of that last digit.
         */
        if (Math.abs(mc) >= ci/2) {
            mc %= (1L << PRECISION) / 10; // Remove the effect of last decimal digit.
            if (mc >= 0) {                // Check if changing the sign would make it smaller.
                if (mc >= (1L << PRECISION) / 20) {
                    mc -= (1L << PRECISION) / 10;
                }
            } else {
                if (mc < (-1L << PRECISION) / 20) {
                    mc += (1L << PRECISION) / 10;
                }
            }
        }
        /*
         * We are done: unscale and return.
         */
        final double delta = MathFunctions.xorSign(-Math.scalb(mc / cs, e), value);
        assert Math.abs(delta) <= Math.ulp(value) / 2 : value;
        return delta;
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
     *       <div class="note"><b>Note:</b>
     *       The above value can be understood in an other way: if the first 324 fraction digits are zero,
     *       then the IEEE {@code double} value is guaranteed to be rounded to exactly 0 no matter what the
     *       next fraction digits are.</div></li>
     *
     *   <li>If {@code accuracy} is greater than 1, then this method returns
     *       the number of "unnecessary" trailing zeros as a negative number.
     *
     *       <div class="note"><b>Example:</b> {@code fractionDigitsForDelta(100, …)} returns -2.</div></li>
     *
     *   <li>If the first non-zero digits of {@code accuracy} are equal or greater than 95
     *       (e.g. 0.00099) and the {@code strict} argument is {@code true}, then this method
     *       increases the number of needed fraction digits in order to prevent the rounded
     *       number to be collapsed into the next integer value.
     *
     *       <div class="note"><b>Example:</b>
     *       If {@code accuracy} is 0.95, then a return value of 1 is not sufficient since
     *       the rounded value of 0.95 with 1 fraction digit would be 1.0. Such value would be a
     *       violation of this method contract since the difference between 0 and that formatted
     *       value would be greater than the accuracy. Note that this is not an artificial rule;
     *       this is related to the fact that 0.9999… is mathematically strictly equals to 1.</div></li>
     * </ul>
     *
     * Invoking this method is equivalent to computing <code>(int)
     * -{@linkplain Math#floor(double) floor}({@linkplain Math#log10(double) log10}(accuracy))</code>
     * except for the 0, {@code NaN}, infinities and {@code 0.…95} special cases.
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
     * <div class="note"><b>Note:</b>
     * We said <cite>almost</cite> because the very last digit may be able to store only a subset
     * of the [0 … 9] digits.</div>
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
     * <div class="note"><b>Example:</b>
     * This method is useful with {@link java.text.NumberFormat} for formatting all significant digits of a
     * {@code double} value, padding with trailing zeros if necessary, but no more than necessary.</div>
     *
     * @param  value The value for which to get the number of significant fraction digits.
     * @return The number of significant fraction digits (may be negative), or 0 if {@code value} is NaN or infinity.
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
     * Returns the number of significant fraction digits, potentially minus trailing digits that may be rounding error.
     * First, this method gets the number of fraction digits as of {@link #fractionDigitsForValue(double)}. Then there
     * is a choice:
     *
     * <ul>
     *   <li>If after rounding the given {@code value} to an amount of fraction digits given by ({@code fractionDigits}
     *       - {@code uncertainDigits}) the 4 last fraction digits before the rounded ones are zero, then this method
     *       returns {@code fractionDigits} - {@code uncertainDigits}.</li>
     *   <li>Otherwise this method returns {@code fractionDigits}.</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * The threshold of 4 trailing fraction digits is arbitrary and may change in any future SIS version.</div>
     *
     * <div class="note"><b>Examples:</b>
     * <ul>
     *   <li>{@code fractionDigitsForValue(179.12499999999824)} returns 14,
     *       the amount of digits after the decimal separator.</li>
     *   <li>{@code fractionDigitsForValue(179.12499999999824, 3)} returns 11 because rounding the 3 last digits
     *       (i.e. rounding after the 11<sup>th</sup> digit) results in 179.125000000000.
     *       Since the 4 last fraction digits are zero, the condition for allowing that rounding is meet.</li>
     *   <li>{@code fractionDigitsForValue(179.12499999999824, 2)} returns 14 because rounding the 2 last digits
     *       (i.e. rounding after the 12<sup>th</sup> digit) results in 179.124999999998.
     *       The condition for 4 trailing zero fraction digits is not meet.</li>
     *   <li>{@code fractionDigitsForValue(179.12499997999999, 3)} returns 14 because rounding the 3 last digits
     *       results in 179.12499997000. The condition for 4 trailing zero fraction digits is not meet.</li>
     * </ul>
     * </div>
     *
     * @param  value The value for which to get the number of significant fraction fraction digits minus rounding error.
     * @param  uncertainDigits Number of trailing fraction digits which may be rounding error artefacts.
     * @return Suggested number of significant digits (may be negative), or 0 if {@code value} is NaN or infinity.
     */
    public static int fractionDigitsForValue(double value, final int uncertainDigits) {
        ArgumentChecks.ensurePositive("uncertainDigits", uncertainDigits);
        int digits = fractionDigitsForValue(value);
        final int reduced = digits - uncertainDigits;
        if (reduced > 0) {
            value *= pow10(reduced);
            if ((Math.rint(value) % 10000) == 0) {
                return reduced;
            }
        }
        return digits;
    }
}
