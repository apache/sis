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
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.DoubleDouble;

import static java.lang.Float.intBitsToFloat;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Double.doubleToRawLongBits;
import static org.apache.sis.internal.util.Numerics.SIGN_BIT_MASK;
import static org.apache.sis.internal.util.Numerics.SIGNIFICAND_SIZE;


/**
 * Simple mathematical functions in addition to the ones provided in {@link Math}.
 * Some methods in this class are very similar to the standard {@link Math} methods
 * or could be implemented with straightforward formulas.
 * However the methods in this class put an emphasis on:
 *
 * <ul>
 *   <li>Rounding errors:
 *       {@link #magnitude(double[]) magnitude},
 *       {@link #pow10(double) pow10}.</li>
 *   <li>Distinguishing positive zero from negative zero:
 *       {@link #isPositive(double) isPositive},
 *       {@link #isNegative(double) isNegative},
 *       {@link #isSameSign(double, double) isSameSign},
 *       {@link #xorSign(double, double) xorSign}.</li>
 *   <li>Distinguishing the different kinds of NaN numbers:
 *       {@link #toNanFloat(int) toNanFloat},
 *       {@link #toNanOrdinal(float) toNanOrdinal}.</li>
 * </ul>
 *
 * Some additional functions not found in {@code Math} are:
 * {@link #atanh(double) atanh},
 * {@link #nextPrimeNumber(int) nextPrimeNumber}.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see DecimalFunctions
 * @see org.apache.sis.util.Numbers
 */
public final class MathFunctions extends Static {
    /**
     * The square root of 2, which is approximated by {@value}.
     *
     * @see Math#sqrt(double)
     */
    public static final double SQRT_2 = 1.4142135623730951;

    /**
     * The logarithm of 2 in base 10, which is approximated by {@value}.
     * This constant is useful for converting a power of 2 to a power of 10 as below:
     *
     * {@preformat java
     *   double exp10 = exp2 * LOG10_2;
     * }
     *
     * @see Math#log10(double)
     * @see #getExponent(double)
     *
     * @since 0.4
     */
    public static final double LOG10_2 = 0.3010299956639812;

    /**
     * The minimal ordinal value for {@code NaN} numbers created by {@link #toNanFloat(int)}.
     *
     * @see #toNanFloat(int)
     * @see #toNanOrdinal(float)
     */
    private static final int MIN_NAN_ORDINAL = -0x200000;

    /**
     * The maximal ordinal value for {@code NaN} numbers created by {@link #toNanFloat(int)}.
     *
     * @see #toNanFloat(int)
     * @see #toNanOrdinal(float)
     */
    static final int MAX_NAN_ORDINAL = 0x1FFFFF;

    /**
     * The highest prime number supported by the {@link #nextPrimeNumber(int)} method.
     * In the current implementation, this value is {@value}. However this limit may
     * change in any future Apache SIS version.
     *
     * <div class="note"><b>Note:</b>
     * The current value is the highest prime number representable as an unsigned 16 bits integer.
     * This is enough for current needs because 16 bits prime numbers are sufficient for finding
     * the divisors of any 32 bits integers.</div>
     *
     * @see #nextPrimeNumber(int)
     */
    public static final int HIGHEST_SUPPORTED_PRIME_NUMBER = 65521;

    /**
     * Maximal length needed for the {@link #primes} array in order to store prime numbers
     * from 2 to 32749 (15 bits) or {@value #HIGHEST_SUPPORTED_PRIME_NUMBER} (16 bits).
     *
     * @see #primeNumberAt(int)
     */
    static final int PRIMES_LENGTH_15_BITS = 3512,
                     PRIMES_LENGTH_16_BITS = 6542;

    /**
     * The sequence of prime numbers computed so far. Will be expanded as needed.
     * We limit ourself to 16 bits numbers because they are sufficient for computing
     * divisors of any 32 bits number.
     *
     * @see #primeNumberAt(int)
     */
    @SuppressWarnings("VolatileArrayField")     // Because we will not modify array content.
    private static volatile short[] primes = new short[] {2, 3};

    /**
     * Do not allow instantiation of this class.
     */
    private MathFunctions() {
    }

    /**
     * Truncates the given value toward zero. Invoking this method is equivalent to invoking
     * {@link Math#floor(double)} if the value is positive, or {@link Math#ceil(double)} if
     * the value is negative.
     *
     * @param  value The value to truncate.
     * @return The largest in magnitude (further from zero) integer value which is equals
     *         or less in magnitude than the given value.
     */
    public static double truncate(final double value) {
        return (doubleToRawLongBits(value) & SIGN_BIT_MASK) == 0 ? Math.floor(value) : Math.ceil(value);
    }

    /**
     * Returns the magnitude of the given vector. This is defined by:
     *
     * {@preformat math
     *     sqrt(vector[0]² + vector[1]² + … + vector[length-1]²)
     * }
     *
     * <div class="section">Implementation note</div>
     * In the special case where only one element is different than zero, this method
     * returns directly the {@linkplain Math#abs(double) absolute value} of that element
     * without computing {@code sqrt(v²)}, in order to avoid rounding error. This special case
     * has been implemented because this method is often invoked for computing the length of
     * {@linkplain org.opengis.coverage.grid.RectifiedGrid#getOffsetVectors() offset vectors},
     * typically aligned with the axes of a {@linkplain org.opengis.referencing.cs.CartesianCS
     * Cartesian coordinate system}.
     *
     * @param  vector The vector for which to compute the magnitude.
     * @return The magnitude of the given vector.
     *
     * @see Math#hypot(double, double)
     */
    public static double magnitude(final double... vector) {
        int i = vector.length;

        // If every elements in the array are zero, returns zero.
        double v1;
        do if (i == 0) return 0;
        while ((v1 = vector[--i]) == 0);

        // We have found a non-zero element. If it is the only one, returns it directly.
        double v2;
        do if (i == 0) return Math.abs(v1);
        while ((v2 = vector[--i]) == 0);

        // If there is exactly 2 elements, use Math.hypot which is more robust than our algorithm.
        double v3;
        do if (i == 0) return Math.hypot(v1, v2);
        while ((v3 = vector[--i]) == 0);

        // Usual magnitude computation, but using double-double arithmetic.
        final DoubleDouble sum = new DoubleDouble();
        final DoubleDouble dot = new DoubleDouble();
        sum.setToProduct(v1, v1);
        dot.setToProduct(v2, v2); sum.add(dot);
        dot.setToProduct(v3, v3); sum.add(dot);
        while (i != 0) {
            v1 = vector[--i];
            dot.setToProduct(v1, v1);
            sum.add(dot);
        }
        sum.sqrt();
        return sum.value;
    }

    /**
     * Returns the unbiased exponent used in the representation of a {@code double}, with correction for
     * sub-normal numbers. This method is related to {@link Math#getExponent(double)} in the following ways:
     *
     * <ul>
     *   <li>For NaN and all values equal or greater than {@link Double#MIN_NORMAL} in magnitude (including
     *       infinities), this method returns results that are identical to {@code Math.getExponent(double)}.</li>
     *   <li>For values smaller than {@link Double#MIN_NORMAL} in magnitude (including zero), the correction
     *       for sub-normal numbers results in return values smaller than what {@code Math.getExponent(double)}
     *       would return.</li>
     * </ul>
     *
     * Special cases:
     * <ul>
     *   <li>If the argument is NaN or infinite, then the result is {@link Double#MAX_EXPONENT} + 1.</li>
     *   <li>If the argument is {@link Double#MAX_VALUE},  then the result is {@value java.lang.Double#MAX_EXPONENT}.</li>
     *   <li>If the argument is {@link Double#MIN_NORMAL}, then the result is {@value java.lang.Double#MIN_EXPONENT}.</li>
     *   <li>If the argument is {@link Double#MIN_VALUE},  then the result is -1074.</li>
     *   <li>If the argument is zero, then the result is -1075.</li>
     * </ul>
     *
     * <div class="section">Identities</div>
     * For any <var>p</var> values in the [-1075 … 1024] range and <var>value</var> = 2<sup>p</sup>:
     * <ul>
     *   <li><code>getExponent(Math.scalb(1.0, p)) == p</code></li>
     *   <li><code>Math.scalb(1.0, getExponent(value)) == value</code></li>
     *   <li><code>Math.floor({@linkplain #LOG10_2} * getExponent(value)) == Math.floor(Math.log10(value))</code></li>
     * </ul>
     *
     * @param  value The value for which to get the exponent.
     * @return The unbiased exponent, corrected for sub-normal numbers if needed.
     *         Values will be in the [-1075 … 1024] range, inclusive.
     *
     * @see Math#getExponent(double)
     * @see Math#scalb(double, int)
     *
     * @since 0.4
     */
    public static int getExponent(final double value) {
        final long bits = doubleToRawLongBits(value);
        int exponent = (int) ((bits >>> SIGNIFICAND_SIZE) & 0x7FFL);
        if (exponent == 0) {
            /*
             * Number is sub-normal: there is no implicit 1 bit before the significand.
             * We need to search for the position of the first real 1 bit, and fix the
             * exponent accordingly.  Note that numberOfLeadingZeros(…) is relative to
             * 64 bits while the significand size is only 52 bits. The last term below
             * is for fixing this difference.
             */
            exponent -= Long.numberOfLeadingZeros(bits & ((1L << SIGNIFICAND_SIZE) - 1)) - (Long.SIZE - SIGNIFICAND_SIZE);
        }
        return exponent - Double.MAX_EXPONENT;
    }

    /**
     * Computes 10 raised to the power of <var>x</var>. Invoking this method is equivalent to invoking
     * <code>{@linkplain Math#pow(double, double) Math.pow}(10, x)</code>, but is slightly more accurate
     * in the special case where the given argument is an integer.
     *
     * @param x The exponent.
     * @return 10 raised to the given exponent.
     *
     * @see #pow10(int)
     * @see Math#pow(double, double)
     * @see Math#log10(double)
     */
    public static double pow10(final double x) {
        final int ix = (int) x;
        if (ix == x) {
            return DecimalFunctions.pow10(ix);
        } else {
            return Math.pow(10, x);
        }
    }

    /**
     * Computes 10 raised to the power of <var>x</var>. This method is faster and slightly more accurate
     * than invoking <code>{@linkplain Math#pow(double, double) Math.pow}(10, x)</code>.
     *
     * <div class="note"><b>Note:</b>
     * This method has been defined because the standard {@code Math.pow(10, x)} method does not always return
     * the closest IEEE floating point representation. Slight departures (1 or 2 ULP) are often allowed in math
     * functions for performance reasons. The most accurate calculations are usually not necessary, but the base
     * 10 is a special case since it is used for scaling axes or formatting human-readable output.</div>
     *
     * Special cases:
     * <ul>
     *   <li>If <var>x</var> is equals or lower than -324, then the result is 0.</li>
     *   <li>If <var>x</var> is equals or greater than 309, then the result is {@linkplain Double#POSITIVE_INFINITY positive infinity}.</li>
     *   <li>If <var>x</var> is in the [0 … 18] range inclusive, then the result is exact.</li>
     *   <li>For all other <var>x</var> values, the result is the closest IEEE 754 approximation.</li>
     * </ul>
     *
     * @param x The exponent.
     * @return 10 raised to the given exponent.
     *
     * @see #pow10(double)
     * @see #LOG10_2
     * @see DecimalFunctions
     */
    public static double pow10(final int x) {
        return DecimalFunctions.pow10(x);
    }

    /**
     * Returns the inverse hyperbolic sine of the given value.
     * This is the inverse of the {@link Math#sinh(double)} method.
     *
     * @param  x The value for which to compute the inverse hyperbolic sine.
     * @return The inverse hyperbolic sine of the given value.
     *
     * @see Math#sinh(double)
     *
     * @since 0.6
     */
    public static double asinh(final double x) {
        return Math.log(x + Math.sqrt(x*x + 1));
    }

    /**
     * Returns the inverse hyperbolic cosine of the given value.
     * This is the inverse of the {@link Math#cosh(double)} method.
     *
     * @param  x The value for which to compute the inverse hyperbolic cosine.
     * @return The inverse hyperbolic cosine of the given value.
     *
     * @see Math#cosh(double)
     *
     * @since 0.6
     */
    public static double acosh(final double x) {
        return Math.log(x + Math.sqrt(x*x - 1));
    }

    /**
     * Returns the inverse hyperbolic tangent of the given value.
     * This is the inverse of the {@link Math#tanh(double)} method.
     * The range of input values shall be in the [-1 … 1].
     * Special cases:
     *
     * <ul>
     *   <li>For <var>x</var> = NaN, this method returns a {@linkplain Double#isNaN(double) NaN} value.</li>
     *   <li>For <var>x</var> = -1, this method returns {@linkplain Double#NEGATIVE_INFINITY negative infinity}.</li>
     *   <li>For <var>x</var> = +1, this method returns {@linkplain Double#POSITIVE_INFINITY positive infinity}.</li>
     * </ul>
     *
     * @param  x The value for which to compute the inverse hyperbolic tangent.
     * @return The inverse hyperbolic tangent of the given value.
     *
     * @see Math#tanh(double)
     */
    public static double atanh(final double x) {
        /*
         * The classical formulas is log((1+x)/(1-x))/2, but the following is more
         * accurate if the (1+x)/(1-x) ratio is close to 1, i.e. if x is close to 0.
         */
        return 0.5 * Math.log1p(2*x / (1-x));
    }

    /**
     * Returns {@code true} if the given value is positive, <em>excluding</em> negative zero.
     * Special cases:
     *
     * <ul>
     *   <li>If the value is {@code +0.0}, returns {@code true}</li>
     *   <li>If the value is {@code -0.0}, returns <b>{@code false}</b></li>
     *   <li>If the value is {@link Double#isNaN(double) NaN}, returns {@code false}</li>
     * </ul>
     *
     * As seen from the above cases, this method distinguishes positive zero from negative zero.
     * The handling of zero values is the difference between invoking {@code isPositive(double)}
     * and testing if (<var>value</var> {@literal >= 0}).
     *
     * @param  value The value to test.
     * @return {@code true} if the given value is positive, excluding negative zero.
     *
     * @see #isPositiveZero(double)
     * @see #isNegative(double)
     */
    public static boolean isPositive(final double value) {
        return (doubleToRawLongBits(value) & SIGN_BIT_MASK) == 0 && !Double.isNaN(value);
    }

    /**
     * Returns {@code true} if the given value is the positive zero ({@code +0.0}).
     * This method returns {@code false} for the negative zero ({@code -0.0}).
     * This method is equivalent to the following code, but potentially faster:
     *
     * {@preformat java
     *   return (value == 0) && isPositive(value);
     * }
     *
     * @param  value The value to test.
     * @return {@code true} if the given value is +0.0 (not -0.0).
     *
     * @see #isPositive(double)
     * @see #isNegativeZero(double)
     *
     * @since 0.4
     */
    public static boolean isPositiveZero(final double value) {
        return doubleToRawLongBits(value) == 0L;
    }

    /**
     * Returns {@code true} if the given value is negative, <em>including</em> negative zero.
     * Special cases:
     *
     * <ul>
     *   <li>If the value is {@code +0.0}, returns {@code false}</li>
     *   <li>If the value is {@code -0.0}, returns <b>{@code true}</b></li>
     *   <li>If the value is {@link Double#isNaN(double) NaN}, returns {@code false}</li>
     * </ul>
     *
     * As seen from the above cases, this method distinguishes positive zero from negative zero.
     * The handling of zero values is the difference between invoking {@code isNegative(double)}
     * and testing if (<var>value</var> {@literal < 0}).
     *
     * @param  value The value to test.
     * @return {@code true} if the given value is negative, including negative zero.
     *
     * @see #isNegativeZero(double)
     * @see #isPositive(double)
     */
    public static boolean isNegative(final double value) {
        return (doubleToRawLongBits(value) & SIGN_BIT_MASK) != 0 && !Double.isNaN(value);
    }

    /**
     * Returns {@code true} if the given value is the negative zero ({@code -0.0}).
     * This method returns {@code false} for the positive zero ({@code +0.0}).
     * This method is equivalent to the following code, but potentially faster:
     *
     * {@preformat java
     *   return (value == 0) && isNegative(value);
     * }
     *
     * @param  value The value to test.
     * @return {@code true} if the given value is -0.0 (not +0.0).
     *
     * @see #isNegative(double)
     * @see #isPositiveZero(double)
     *
     * @since 0.4
     */
    public static boolean isNegativeZero(final double value) {
        return doubleToRawLongBits(value) == SIGN_BIT_MASK;
    }

    /**
     * Returns {@code true} if the given values have the same sign, differentiating positive
     * and negative zeros.
     * Special cases:
     *
     * <ul>
     *   <li>{@code +0.0} and {@code -0.0} are considered to have opposite sign</li>
     *   <li>If any value is {@link Double#isNaN(double) NaN}, returns {@code false}</li>
     * </ul>
     *
     * @param  v1 The first value.
     * @param  v2 The second value, to compare the sign with the first value.
     * @return {@code true} if the given values are not NaN and have the same sign.
     *
     * @see Math#signum(double)
     */
    public static boolean isSameSign(final double v1, final double v2) {
        return !Double.isNaN(v1) && !Double.isNaN(v2) &&
                ((doubleToRawLongBits(v1) ^ doubleToRawLongBits(v2)) & SIGN_BIT_MASK) == 0;
    }

    /**
     * Returns the first floating-point argument with the sign reversed if the second floating-point
     * argument is negative. This method is similar to <code>{@linkplain Math#copySign(double,double)
     * Math.copySign}(value, sign)</code> except that the sign is combined with an <cite>exclusive
     * or</cite> operation instead than being copied.
     *
     * <p>This method makes no guarantee about whether {@code NaN} values are handled as positive
     * or negative numbers. This is the same policy than {@link Math#copySign(double, double)}.</p>
     *
     * @param  value The parameter providing the value that may need a sign change.
     * @param  sign The parameter providing the sign to <cite>xor</cite> with the value.
     * @return The provided value with its sign reversed if the {@code sign} parameter is negative.
     *
     * @see Math#copySign(double, double)
     */
    public static double xorSign(final double value, final double sign) {
        return longBitsToDouble(doubleToRawLongBits(value) ^
                (doubleToRawLongBits(sign) & SIGN_BIT_MASK));
    }

    /**
     * Returns {@code true} if the given values are {@linkplain Float#equals(Object) equal}
     * or if their difference is not greater than the given threshold. More specifically:
     *
     * <ul>
     *   <li>If both values are {@linkplain Float#POSITIVE_INFINITY positive infinity}, or
     *       if both values are {@linkplain Float#NEGATIVE_INFINITY negative infinity},
     *       then this method returns {@code true}.</li>
     *   <li>If both values {@linkplain Float#isNaN(float) are NaN}, then this method returns {@code true}.
     *       Note that this method does not differentiate the various NaN values.</li>
     *   <li>Otherwise, this method returns the result of the {@code abs(v1 - v2) <= ε} comparison.</li>
     * </ul>
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @param  ε  The tolerance threshold, which must be positive.
     * @return {@code true} If both values are equal given the tolerance threshold.
     */
    public static boolean epsilonEqual(final float v1, final float v2, final float ε) {
        return (Math.abs(v1 - v2) <= ε) || Float.floatToIntBits(v1) == Float.floatToIntBits(v2);
    }

    /**
     * Returns {@code true} if the given values are {@linkplain Double#equals(Object) equal}
     * or if their difference is not greater than the given threshold. More specifically:
     *
     * <ul>
     *   <li>If both values are {@linkplain Double#POSITIVE_INFINITY positive infinity}, or
     *       if both values are {@linkplain Double#NEGATIVE_INFINITY negative infinity},
     *       then this method returns {@code true}.</li>
     *   <li>If both values {@linkplain Double#isNaN(double) are NaN}, then this method returns {@code true}.
     *       Note that this method does not differentiate the various NaN values.</li>
     *   <li>Otherwise, this method returns the result of the {@code abs(v1 - v2) <= ε} comparison.</li>
     * </ul>
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @param  ε  The tolerance threshold, which must be positive.
     * @return {@code true} If both values are equal given the tolerance threshold.
     */
    public static boolean epsilonEqual(final double v1, final double v2, final double ε) {
        return (Math.abs(v1 - v2) <= ε) || Double.doubleToLongBits(v1) == Double.doubleToLongBits(v2);
    }

    /**
     * Returns a {@linkplain Float#isNaN(float) NaN} number for the specified ordinal value.
     * Valid NaN numbers in Java can have bit fields in the ranges listed below:
     *
     * <ul>
     *   <li>[{@code 0x7F800001} … {@code 0x7FFFFFFF}], with
     *        {@code 0x7FC00000} as the bit fields of the standard {@link Float#NaN} value</li>
     *   <li>[{@code 0xFF800001} … {@code 0xFFFFFFFF}]</li>
     * </ul>
     *
     * Some of those bits, named the <cite>payload</cite>, can be used for storing custom information.
     * This method maps some of the payload values to each ordinal value.
     *
     * <p>The relationship between payload values and ordinal values is implementation dependent and
     * may change in any future version of the SIS library. The current implementation restricts the
     * range of allowed ordinal values to a smaller one than the range of all possible values.</p>
     *
     * @param  ordinal The NaN ordinal value, from {@code -0x200000} to {@code 0x1FFFFF} inclusive.
     * @return One of the legal {@linkplain Float#isNaN(float) NaN} values as a float.
     * @throws IllegalArgumentException if the specified ordinal is out of range.
     *
     * @see Float#intBitsToFloat(int)
     */
    public static float toNanFloat(final int ordinal) throws IllegalArgumentException {
        ArgumentChecks.ensureBetween("ordinal", MIN_NAN_ORDINAL, MAX_NAN_ORDINAL, ordinal);
        final float value = intBitsToFloat(0x7FC00000 + ordinal);
        assert Float.isNaN(value) && toNanOrdinal(value) == ordinal : ordinal;
        return value;
    }

    /**
     * Returns the ordinal value of the given NaN number.
     * This method is the converse of {@link #toNanFloat(int)}.
     *
     * @param  value The value from which to get the NaN ordinal value.
     * @return The NaN ordinal value of the given floating point value.
     * @throws IllegalArgumentException If the given value is not a NaN value,
     *         or does not use a supported bits pattern.
     */
    public static int toNanOrdinal(final float value) throws IllegalArgumentException {
        final int ordinal = floatToRawIntBits(value) - 0x7FC00000;
        if (ordinal >= MIN_NAN_ORDINAL && ordinal <= MAX_NAN_ORDINAL) {
            return ordinal;
        }
        final short resourceKey;
        final Object obj;
        if (Float.isNaN(value)) {
            resourceKey = Errors.Keys.IllegalBitsPattern_1;
            obj = Integer.toHexString(ordinal);
        } else {
            resourceKey = Errors.Keys.IllegalArgumentValue_2;
            obj = value;
        }
        throw new IllegalArgumentException(Errors.format(resourceKey, obj));
    }

    /**
     * Converts two long bits values containing a IEEE 754 quadruple precision floating point number
     * to a double precision floating point number. About 17 decimal digits of precision may be lost
     * due to the {@code double} type having only half the capacity of quadruple precision type.
     *
     * <p>Some quadruple precision values can not be represented in double precision and are mapped
     * to {@code double} values as below:</p>
     * <ul>
     *   <li>Values having a magnitude less than {@link Double#MIN_VALUE} are mapped to
     *       positive or negative zero.</li>
     *   <li>Values having a magnitude greater than {@link Double#MAX_VALUE} are mapped to
     *       {@link Double#POSITIVE_INFINITY} or {@link Double#NEGATIVE_INFINITY}.</li>
     *   <li>All NaN values are currently collapsed to the single "canonical" {@link Double#NaN} value
     *       (this policy may be revisited in future SIS version).</li>
     * </ul>
     *
     * @param l0 upper part of the quadruple precision floating point number.
     * @param l1 lower part of the quadruple precision floating point number.
     * @return double precision approximation.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Quadruple-precision_floating-point_format">Quadruple-precision floating-point format on Wikipedia</a>
     *
     * @since 0.7
     */
    public static double quadrupleToDouble(long l0, long l1) {
        // Build double
        long sig = (l0 & 0x8000000000000000L);
        long exp = (l0 & 0x7FFF000000000000L) >> 48;
        l0       = (l0 & 0x0000FFFFFFFFFFFFL);
        if (exp == 0) {
            /*
             * Subnormal number.
             * Since we convert them to double precision, subnormal numbers can not be represented
             * as they are smaller than Double.MIN_VALUE. We map them to zero preserving the sign.
             */
            return Double.longBitsToDouble(sig);
        }
        if (exp == 0x7FFF) {
            /*
             * NaN of infinite number.
             * Mantissa with all bits at 0 is used for infinite.
             * This is the only special number that we can preserve.
             */
            if (l0 == 0 && l1 == 0) {
                return Double.longBitsToDouble(sig | 0x7FF0000000000000L);
            }
            /*
             * Other NaN values might have a meaning (e.g. NaN(1) = forest, NaN(2) = lake, etc.)
             * See above toNanFloat(int) and toNaNOrdinal(float) methods. When truncating the value we
             * might change the meaning, which could cause several issues later. Therefor we conservatively
             * collapse all NaNs to the default NaN for now (this may be revisited in a future SIS version).
             */
            return Double.NaN;
        }
        exp -= (16383 - 1023);      //change from 15 bias to 11 bias
        // Check cases where mantissa excess what double can support
        if (exp < 0)    return Double.NEGATIVE_INFINITY;
        if (exp > 2046) return Double.POSITIVE_INFINITY;

        return Double.longBitsToDouble(sig | (exp << 52) | (l0 << 4) | (l1 >>> 60));
    }

    /**
     * Returns the <var>i</var><sup>th</sup> prime number.
     * This method returns (2, 3, 5, 7, 11, …) for index (0, 1, 2, 3, 4, …).
     *
     * @param  index The prime number index, starting at index 0 for prime number 2.
     * @return The prime number at the specified index.
     * @throws IndexOutOfBoundsException if the specified index is too large.
     *
     * @see java.math.BigInteger#isProbablePrime(int)
     */
    static int primeNumberAt(final int index) throws IndexOutOfBoundsException {
        ArgumentChecks.ensureValidIndex(PRIMES_LENGTH_16_BITS, index);
        short[] primes = MathFunctions.primes;
        if (index >= primes.length) {
            synchronized (MathFunctions.class) {
                primes = MathFunctions.primes;
                if (index >= primes.length) {
                    int i = primes.length;
                    int n = primes[i - 1] & 0xFFFF;
                    // Compute by block of 16 values, for reducing the amount of array resize.
                    primes = Arrays.copyOf(primes, Math.min((index | 0xF) + 1, PRIMES_LENGTH_16_BITS));
                    do {
testNextNumber:         while (true) { // Simulate a "goto" statement (usually not recommanded...)
                            final int stopAt = (int) Math.sqrt(n += 2);
                            int prime;
                            int j = 0;
                            do {
                                prime = primes[++j] & 0xFFFF;
                                if (n % prime == 0) {
                                    continue testNextNumber;
                                }
                            } while (prime <= stopAt);
                            primes[i] = (short) n;
                            break;
                        }
                    } while (++i < primes.length);
                    MathFunctions.primes = primes;
                }
            }
        }
        return primes[index] & 0xFFFF;
    }

    /**
     * Returns the first prime number equals or greater than the given value.
     * Current implementation accepts only values in the
     * [2 … {@value #HIGHEST_SUPPORTED_PRIME_NUMBER}] range.
     *
     * @param  number The number for which to find the next prime.
     * @return The given number if it is a prime number, or the next prime number otherwise.
     * @throws IllegalArgumentException If the given value is outside the supported range.
     *
     * @see java.math.BigInteger#isProbablePrime(int)
     */
    public static int nextPrimeNumber(final int number) throws IllegalArgumentException {
        ArgumentChecks.ensureBetween("number", 2, HIGHEST_SUPPORTED_PRIME_NUMBER, number);
        final short[] primes = MathFunctions.primes;
        int lower = 0;
        int upper = Math.min(PRIMES_LENGTH_15_BITS, primes.length);
        if (number > Short.MAX_VALUE) {
            lower = upper;
            upper = primes.length;
        }
        int i = Arrays.binarySearch(primes, lower, upper, (short) number);
        if (i < 0) {
            i = ~i;
            if (i >= primes.length) {
                int p;
                do p = primeNumberAt(i++);
                while (p < number);
                return p;
            }
        }
        return primes[i] & 0xFFFF;
    }

    /**
     * Returns the divisors of the specified number as positive integers. For any value other
     * than {@code O} (which returns an empty array), the first element in the returned array
     * is always {@code 1} and the last element is always the absolute value of {@code number}.
     *
     * @param number The number for which to compute the divisors.
     * @return The divisors in strictly increasing order.
     */
    public static int[] divisors(int number) {
        if (number == 0) {
            return ArraysExt.EMPTY_INT;
        }
        number = Math.abs(number);
        int[] divisors = new int[16];
        divisors[0] = 1;
        int count = 1;
        /*
         * Searches for the first divisors among the prime numbers. We stop the search at the
         * square root of 'n' because every values above that point can be inferred from the
         * values before that point, i.e. if n=p1*p2 and p2 is greater than 'sqrt', than p1
         * most be lower than 'sqrt'.
         */
        final int sqrt = (int) Math.sqrt(number); // Really wants rounding toward 0.
        for (int p,i=0; (p=primeNumberAt(i)) <= sqrt; i++) {
            if (number % p == 0) {
                if (count == divisors.length) {
                    divisors = Arrays.copyOf(divisors, count*2);
                }
                divisors[count++] = p;
            }
        }
        /*
         * Completes the divisors past 'sqrt'. The numbers added here may or may not be prime
         * numbers. Side note: checking that they are prime numbers would be costly, but this
         * algorithm doesn't need that.
         */
        int source = count;
        if (count*2 > divisors.length) {
            divisors = Arrays.copyOf(divisors, count*2);
        }
        int d1 = divisors[--source];
        int d2 = number / d1;
        if (d1 != d2) {
            divisors[count++] = d2;
        }
        while (--source >= 0) {
            divisors[count++] = number / divisors[source];
        }
        /*
         * Checks the products of divisors found so far. For example if 2 and 3 are divisors,
         * checks if 6 is a divisor as well. The products found will themself be used for
         * computing new products.
         */
        for (int i=1; i<count; i++) {
            d1 = divisors[i];
            for (int j=i; j<count; j++) {
                d2 = d1 * divisors[j];
                if (number % d2 == 0) {
                    int p = Arrays.binarySearch(divisors, j, count, d2);
                    if (p < 0) {
                        p = ~p; // ~ operator, not minus
                        if (count == divisors.length) {
                            divisors = Arrays.copyOf(divisors, count*2);
                        }
                        System.arraycopy(divisors, p, divisors, p+1, count-p);
                        divisors[p] = d2;
                        count++;
                    }
                }
            }
        }
        divisors = ArraysExt.resize(divisors, count);
        assert ArraysExt.isSorted(divisors, true);
        return divisors;
    }

    /**
     * Returns the divisors which are common to all the specified numbers.
     *
     * @param  numbers The numbers for which to compute the divisors.
     * @return The divisors common to all the given numbers, in strictly increasing order.
     */
    public static int[] commonDivisors(final int... numbers) {
        if (numbers.length == 0) {
            return ArraysExt.EMPTY_INT;
        }
        /*
         * Get the smallest value. We will compute the divisors only for this value,
         * since we know that any value greater that the minimal value can not be a
         * common divisor.
         */
        int minValue = Integer.MAX_VALUE;
        for (int i=0; i<numbers.length; i++) {
            final int n = Math.abs(numbers[i]);
            if (n <= minValue) {
                minValue = n;
            }
        }
        int[] divisors = divisors(minValue);
        /*
         * Tests if the divisors we just found are also divisors of all other numbers.
         * Removes those which are not.
         */
        int count = divisors.length;
        for (int i=0; i<numbers.length; i++) {
            final int n = Math.abs(numbers[i]);
            if (n != minValue) {
                for (int j=count; --j>0;) { // Do not test j==0, since divisors[0] ==  1.
                    if (n % divisors[j] != 0) {
                        System.arraycopy(divisors, j+1, divisors, j, --count - j);
                    }
                }
            }
        }
        return ArraysExt.resize(divisors, count);
    }
}
