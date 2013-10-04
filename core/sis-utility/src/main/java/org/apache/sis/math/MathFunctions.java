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
import org.apache.sis.util.Workaround;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

import static java.lang.Float.intBitsToFloat;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Double.doubleToRawLongBits;
import org.apache.sis.internal.util.DoubleDouble;
import static org.apache.sis.internal.util.Numerics.SIGN_BIT_MASK;


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
 * @since   0.3 (derived from geotk-1.0)
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.util.Numbers
 */
public final class MathFunctions extends Static {
    /**
     * The square root of 2, which is {@value}.
     *
     * @see Math#sqrt(double)
     */
    public static final double SQRT_2 = 1.4142135623730951;

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
     * {@note The current value is the highest prime number representable as an unsigned 16 bits
     *        integer. This is enough for current needs because 16 bits prime numbers are sufficient
     *        for finding the divisors of any 32 bits integers.}
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
     * {@section Implementation note}
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
        return Math.sqrt(sum.value);
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
     *   <li>If {@code accuracy} is 0, {@link Double#NaN NaN} or infinity,
     *       then this method returns 0.</li>
     *   <li>If {@code accuracy} is greater than 1, then this method returns
     *       the number of "unnecessary" trailing zeros as a negative number.
     *       For example {@code fractionDigitsForDelta(100, …)} returns -2.</li>
     *   <li>If the first non-zero digits of {@code accuracy} are equal or greater than 95
     *       (e.g. 0.00099) and the {@code strict} argument is {@code true}, then this method
     *       increases the number of needed fraction digits in order to prevent the rounded
     *       number to be collapsed into the next integer value.
     *
     *       {@example
     *       If {@code accuracy} is 0.95, then a return value of 1 is not sufficient since the
     *       rounded value of 0.95 with 1 fraction digit would be 1.0. Such value would be a
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
                return 0;
            }
            i = -((int) Math.floor(y));
            scale = pow10(i);
        }
        while ((accuracy *= scale) >= 9.5) {
            i++; // The 0.…95 special case.
            accuracy -= Math.floor(accuracy);
            scale = 10;
        }
        return i;
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
    public static double pow10(final double x) {
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
    public static double pow10(final int x) {
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

    /**
     * Returns the inverse hyperbolic tangent of the given value.
     * This is the inverse of the {@linkplain Math#tanh(double) tanh} method.
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
     */
    public static boolean isPositive(final double value) {
        return (doubleToRawLongBits(value) & SIGN_BIT_MASK) == 0 && !Double.isNaN(value);
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
     */
    public static boolean isNegative(final double value) {
        return (doubleToRawLongBits(value) & SIGN_BIT_MASK) != 0 && !Double.isNaN(value);
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
     * Returns the sign of <var>x</var>. This method returns
     *    -1 if <var>x</var> is negative,
     *     0 if <var>x</var> is zero or {@code NaN} and
     *    +1 if <var>x</var> is positive.
     *
     * @param x The number from which to get the sign.
     * @return {@code +1} if <var>x</var> is positive, {@code -1} if negative, or 0 otherwise.
     *
     * @see Math#signum(double)
     */
    public static int sgn(final double x) {
        if (x > 0) return +1;
        if (x < 0) return -1;
        else       return  0;
    }

    /**
     * Returns the sign of <var>x</var>. This method returns
     *    -1 if <var>x</var> is negative,
     *     0 if <var>x</var> is zero or {@code NaN} and
     *    +1 if <var>x</var> is positive.
     *
     * @param x The number from which to get the sign.
     * @return {@code +1} if <var>x</var> is positive, {@code -1} if negative, or 0 otherwise.
     *
     * @see Math#signum(float)
     */
    public static int sgn(final float x) {
        if (x > 0) return +1;
        if (x < 0) return -1;
        else       return  0;
    }

    /**
     * Returns the sign of <var>x</var>. This method returns
     *    -1 if <var>x</var> is negative,
     *     0 if <var>x</var> is zero and
     *    +1 if <var>x</var> is positive.
     *
     * @param x The number from which to get the sign.
     * @return {@code +1} if <var>x</var> is positive, {@code -1} if negative, or 0 otherwise.
     */
    public static int sgn(long x) {
        if (x > 0) return +1;
        if (x < 0) return -1;
        else       return  0;
    }

    /**
     * Returns the sign of <var>x</var>. This method returns
     *    -1 if <var>x</var> is negative,
     *     0 if <var>x</var> is zero and
     *    +1 if <var>x</var> is positive.
     *
     * @param x The number from which to get the sign.
     * @return {@code +1} if <var>x</var> is positive, {@code -1} if negative, or 0 otherwise.
     */
    public static int sgn(int x) {
        if (x > 0) return +1;
        if (x < 0) return -1;
        else       return  0;
    }

    /**
     * Returns the sign of <var>x</var>. This method returns
     *    -1 if <var>x</var> is negative,
     *     0 if <var>x</var> is zero and
     *    +1 if <var>x</var> is positive.
     *
     * @param x The number from which to get the sign.
     * @return {@code +1} if <var>x</var> is positive, {@code -1} if negative, or 0 otherwise.
     */
    public static short sgn(short x) {
        if (x > 0) return (short) +1;
        if (x < 0) return (short) -1;
        else       return (short)  0;
    }

    /**
     * Returns the sign of <var>x</var>. This method returns
     *    -1 if <var>x</var> is negative,
     *     0 if <var>x</var> is zero and
     *    +1 if <var>x</var> is positive.
     *
     * @param x The number from which to get the sign.
     * @return {@code +1} if <var>x</var> is positive, {@code -1} if negative, or 0 otherwise.
     */
    public static byte sgn(byte x) {
        if (x > 0) return (byte) +1;
        if (x < 0) return (byte) -1;
        else       return (byte)  0;
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
     * Valid NaN numbers in Java can have bit fields in the ranges listed below.
     * This method allocates one of valid NaN bit fields to each ordinal value.
     *
     * <ul>
     *   <li>[{@code 0x7F800001} … {@code 0x7FFFFFFF}], with
     *        {@code 0x7FC00000} as the bit fields of the standard {@link Float#NaN} value</li>
     *   <li>[{@code 0xFF800001} … {@code 0xFFFFFFFF}]</li>
     * </ul>
     *
     * The relationship between bit fields and ordinal values is implementation dependent and may
     * change in any future version of the SIS library. The current implementation restricts the
     * range of allowed ordinal values to a smaller one than the range of all possible NaN values.
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
        final int resourceKey;
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
