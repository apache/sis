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
import java.util.Objects;
import static java.lang.Math.PI;
import static java.lang.Math.min;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static java.lang.Math.cbrt;
import static java.lang.Math.fma;
import static java.lang.Math.cos;
import static java.lang.Math.copySign;
import static java.lang.Math.multiplyFull;
import static java.lang.Math.multiplyExact;
import static java.lang.Float.intBitsToFloat;
import static java.lang.Float.floatToIntBits;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.doubleToRawLongBits;
import org.apache.sis.util.Static;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.DoubleDouble;
import static org.apache.sis.util.internal.shared.Numerics.SIGN_BIT_MASK;
import static org.apache.sis.util.internal.shared.Numerics.SIGNIFICAND_MASK;
import static org.apache.sis.pending.jdk.JDK19.DOUBLE_PRECISION;


/**
 * Simple mathematical functions in addition to the ones provided in {@link Math}.
 * Some methods in this class are very similar to the standard {@link Math} methods
 * or could be implemented with straightforward formulas.
 * However, the methods in this class put an emphasis on:
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
 * @version 1.4
 *
 * @see DecimalFunctions
 * @see org.apache.sis.util.Numbers
 *
 * @since 0.3
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
     * {@snippet lang="java" :
     *   double exp10 = exp2 * LOG10_2;
     *   }
     *
     * @see Math#log10(double)
     * @see #getExponent(double)
     *
     * @since 0.4
     */
    public static final double LOG10_2 = 0.3010299956639812;

    /**
     * The minimal ordinal value for {@code NaN} numbers created by {@link #toNanFloat(int)}.
     * The current value is {@value}.
     *
     * @since 1.0
     */
    public static final int MIN_NAN_ORDINAL = -0x200000;

    /**
     * The maximal ordinal value for {@code NaN} numbers created by {@link #toNanFloat(int)}.
     * The current value is {@value}.
     *
     * @since 1.0
     */
    public static final int MAX_NAN_ORDINAL =  0x1FFFFF;

    /**
     * The beginning of ranges of quiet NaN values.
     * The range is selected in way to restrict ourselves to <i>quiet</i> NaN values.
     * The following is an adaptation of evaluator's comments for bug #4471414
     * (http://developer.java.sun.com/developer/bugParade/bugs/4471414.html):
     *
     * <blockquote>
     *    There are actually two types of NaNs, signaling NaNs and quiet NaNs. Java doesn't support the features necessary
     *    to reliably distinguish the two. However, the relevant point is that copying a signaling NaN may (or may not, at
     *    the implementers discretion) yield a quiet NaN — a NaN with a different bit pattern (IEEE 754 6.2). Therefore, on
     *    IEEE 754 compliant platforms it may be impossible to find a signaling NaN stored in an array since a signaling NaN
     *    passed as an argument to binarySearch may get replaced by a quiet NaN.
     * </blockquote>
     *
     * The relevant thresholds are:
     * <ul>
     *   <li>{@code 7F800000}: positive infinity.</li>
     *   <li>{@code 7F800001}: first signaling NaN in the range of positive values.</li>
     *   <li>{@code 7FBFFFFF}: last signaling NaN.</li>
     *   <li>{@code 7FC00000}: first quiet NaN. Also the standard {@link Double#NaN} value.</li>
     *   <li>{@code 7FFFFFFF}: last quiet NaN.</li>
     *   <li>{@code FF800000}: negative infinity.</li>
     *   <li>{@code FF800001}: first signaling NaN in the range of negative values.</li>
     *   <li>{@code FFBFFFFF}: last signaling NaN.</li>
     *   <li>{@code FFC00000}: first quiet NaN in the range of negative values.</li>
     *   <li>{@code FFFFFFFF}: last quiet NaN.</li>
     * </ul>
     *
     * @see #toNanFloat(int)
     * @see #toNanOrdinal(float)
     */
    static final int POSITIVE_NAN = 0x7FC00000,
                     NEGATIVE_NAN = 0xFFC00000;

    /**
     * The highest prime number supported by the {@link #nextPrimeNumber(int)} method.
     * In the current implementation, this value is {@value}. However, this limit may
     * change in any future Apache SIS version.
     *
     * <h4>Implementation note</h4>
     * The current value is the highest prime number representable as an unsigned 16 bits integer.
     * This is enough for current needs because 16 bits prime numbers are sufficient for finding
     * the divisors of any 32 bits integers.
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
     * Computes the averages of two signed integers without overflow. The calculation is performed with
     * {@code long} arithmetic before to convert the result to the {@code double} floating point number.
     * This function may be more accurate than the classical (x+y)/2 formula when <var>x</var> and/or
     * <var>y</var> are very large, because it will avoid the lost of last digits before averaging.
     * If exactly one of <var>x</var> and <var>y</var> is odd, the result will contain the 0.5 fraction digit.
     *
     * <h4>Reference</h4>
     * This function is adapted from
     * <a href="http://aggregate.org/MAGIC/#Average%20of%20Integers">The Aggregate Magic Algorithms</a>
     * from University of Kentucky.
     *
     * @param  x  the first value to average.
     * @param  y  the second value to average.
     * @return average of given values without integer overflow.
     *
     * @since 1.1
     */
    public static double average(final long x, final long y) {
        final long xor = (x ^ y);
        double c = (x & y) + (xor >> 1);      // Really need >> 1, not /2 (they differ with negative numbers).
        if ((xor & 1) != 0) c += 0.5;
        return c;
    }

    /**
     * Truncates the given value toward zero. Invoking this method is equivalent to invoking
     * {@link Math#floor(double)} if the value is positive, or {@link Math#ceil(double)} if
     * the value is negative.
     *
     * @param  value  the value to truncate.
     * @return the largest in magnitude (further from zero) integer value which is equal
     *         or less in magnitude than the given value.
     */
    public static double truncate(final double value) {
        return (doubleToRawLongBits(value) & SIGN_BIT_MASK) == 0 ? Math.floor(value) : Math.ceil(value);
    }

    /**
     * Returns the magnitude of the given vector. This is defined by:
     *
     * <pre class="math">
     *     sqrt(vector[0]² + vector[1]² + … + vector[length-1]²)</pre>
     *
     * If the given vector contains a NaN value, then the result is NaN.
     *
     * <h4>Implementation note</h4>
     * In the special case where only one element is different than zero, this method
     * returns directly the {@linkplain Math#abs(double) absolute value} of that element
     * without computing {@code sqrt(v²)}, in order to avoid rounding error. This special case
     * has been implemented because this method is often invoked for computing the length of
     * {@linkplain org.opengis.coverage.grid.RectifiedGrid#getOffsetVectors() offset vectors},
     * typically aligned with the axes of a {@linkplain org.opengis.referencing.cs.CartesianCS
     * Cartesian coordinate system}.
     *
     * @param  vector  the vector for which to compute the magnitude.
     * @return the magnitude of the given vector as a positive number, of NaN.
     *
     * @see Math#hypot(double, double)
     */
    public static double magnitude(final double... vector) {
        int i = vector.length;

        // If every elements in the array are zero, returns zero.
        double v1;
        do if (i == 0) return 0;
        while ((v1 = vector[--i]) == 0);

        // We have found a non-zero element. If it is the only one, returns its absolute value.
        double v2;
        do if (i == 0) return abs(v1);
        while ((v2 = vector[--i]) == 0);

        // If there is exactly 2 elements, use Math.hypot which is more robust than our algorithm.
        double v3;
        do if (i == 0) return Math.hypot(v1, v2);
        while ((v3 = vector[--i]) == 0);

        // Usual magnitude computation, but using double-double arithmetic.
        DoubleDouble sum;
        sum =         DoubleDouble.product(v1, v1);
        sum = sum.add(DoubleDouble.product(v2, v2));
        sum = sum.add(DoubleDouble.product(v3, v3));
        while (i != 0) {
            final double v = vector[--i];
            sum = sum.add(DoubleDouble.product(v, v));
        }
        return sum.sqrt().doubleValue();
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
     * <h4>Identities</h4>
     * For any <var>p</var> values in the [-1075 … 1024] range and <var>value</var> = 2<sup>p</sup>:
     * <ul>
     *   <li><code>getExponent(Math.scalb(1.0, p)) == p</code></li>
     *   <li><code>Math.scalb(1.0, getExponent(value)) == value</code></li>
     *   <li><code>Math.floor({@linkplain #LOG10_2} * getExponent(value)) == Math.floor(Math.log10(value))</code></li>
     * </ul>
     *
     * @param  value  the value for which to get the exponent.
     * @return the unbiased exponent, corrected for sub-normal numbers if needed.
     *         Values will be in the [-1075 … 1024] range, inclusive.
     *
     * @see Math#getExponent(double)
     * @see Math#scalb(double, int)
     *
     * @since 0.4
     */
    public static int getExponent(final double value) {
        final long bits = doubleToRawLongBits(value);
        int exponent = (int) ((bits >>> (DOUBLE_PRECISION - 1)) & 0x7FFL);
        if (exponent == 0) {
            /*
             * Number is sub-normal: there is no implicit 1 bit before the significand.
             * We need to search for the position of the first real 1 bit, and fix the
             * exponent accordingly.  Note that numberOfLeadingZeros(…) is relative to
             * 64 bits while the significand size is only 52 bits. The last term below
             * is for fixing this difference.
             */
            exponent -= Long.numberOfLeadingZeros(bits & SIGNIFICAND_MASK) - (Long.SIZE - (DOUBLE_PRECISION - 1));
        }
        return exponent - Double.MAX_EXPONENT;
    }

    /**
     * Computes the result of {@code base} argument raised to the power given by {@code exponent} argument.
     * This method computes the same value as {@link Math#pow(double, double)} but using only integer arithmetic.
     * The result must be representable as a 64 bits integers ({@code long} primitive type),
     * otherwise an {@link ArithmeticException} is thrown. The result is guaranteed exact,
     * in contrast to results represented as {@code double} floating point values
     * which may be approximate for magnitudes greater than 2<sup>52</sup>.
     * This method may also be faster.
     *
     * <p>The type of the {@code base} argument is {@code long} for convenience, since this method is used in contexts
     * where relatively large integers are handled. However, any value greater than the capacity of {@code int} type
     * is guaranteed to fail with {@link ArithmeticException} unless {@code exponent} is 0 or 1.
     * Likewise any {@code exponent} value greater than 62 is guaranteed to fail unless {@code base} is 0 or 1.</p>
     *
     * <h4>Reference</h4>
     * This method uses
     * <a href="https://en.wikipedia.org/wiki/Exponentiation_by_squaring">exponentiation by squaring</a> technic.
     *
     * @param  base      the value to raise to an exponent.
     * @param  exponent  the exponent, as zero or positive number.
     * @return the value <var>base</var><sup><var>exponent</var></sup> as a 64 bits integer.
     * @throws ArithmeticException if the given exponent is negative, or if the result overflow integer arithmetic.
     *
     * @see Math#pow(double, double)
     *
     * @since 1.0
     */
    public static long pow(long base, int exponent) {
        long result = 1;
        if (exponent >= 1) {
            if ((exponent & 1) != 0) {
                result = base;
            }
            while ((exponent >>>= 1) != 0) {
                base = multiplyExact(base, base);
                if ((exponent & 1) != 0) {
                    result = multiplyExact(result, base);
                }
            }
        } else if (exponent < 0) {
            throw new ArithmeticException(Errors.format(Errors.Keys.NegativeArgument_2, "exponent", exponent));
        }
        return result;
    }

    /**
     * Computes 10 raised to the power of <var>x</var>. This method is faster and slightly more accurate
     * than invoking <code>{@linkplain Math#pow(double, double) Math.pow}(10, x)</code>.
     * Special cases:
     * <ul>
     *   <li>If <var>x</var> is equal or lower than -324, then the result is 0.</li>
     *   <li>If <var>x</var> is equal or greater than 309, then the result is {@linkplain Double#POSITIVE_INFINITY positive infinity}.</li>
     *   <li>If <var>x</var> is in the [0 … 18] range inclusive, then the result is exact.</li>
     *   <li>For all other <var>x</var> values, the result is the closest IEEE 754 approximation.</li>
     * </ul>
     *
     * <h4>Purpose</h4>
     * This method has been defined because the standard {@code Math.pow(10, x)} method does not always return
     * the closest IEEE floating point representation. Slight departures (1 or 2 ULP) are often allowed in math
     * functions for performance reasons. The most accurate calculations are usually not necessary, but the base
     * 10 is a special case since it is used for scaling axes or formatting human-readable output.
     *
     * @param  x  the exponent.
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
     * Computes 10 raised to the power of <var>x</var>. Invoking this method is equivalent to invoking
     * <code>{@linkplain Math#pow(double, double) Math.pow}(10, x)</code>, but is slightly more accurate
     * in the special case where the given argument is an integer.
     *
     * @param  x  the exponent.
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
     * Returns the inverse hyperbolic sine of the given value.
     * This is the inverse of the {@link Math#sinh(double)} method.
     *
     * @param  x  the value for which to compute the inverse hyperbolic sine.
     * @return the inverse hyperbolic sine of the given value.
     *
     * @see Math#sinh(double)
     *
     * @since 0.6
     */
    public static double asinh(final double x) {
        return Math.log(x + sqrt(x*x + 1));
    }

    /**
     * Returns the inverse hyperbolic cosine of the given value.
     * This is the inverse of the {@link Math#cosh(double)} method.
     *
     * @param  x  the value for which to compute the inverse hyperbolic cosine.
     * @return the inverse hyperbolic cosine of the given value.
     *
     * @see Math#cosh(double)
     *
     * @since 0.6
     */
    public static double acosh(final double x) {
        return Math.log(x + sqrt(x*x - 1));
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
     * @param  x  the value for which to compute the inverse hyperbolic tangent.
     * @return the inverse hyperbolic tangent of the given value.
     *
     * @see Math#tanh(double)
     */
    public static double atanh(final double x) {
        /*
         * The classical formulas is log((1+x)/(1-x))/2, but the following is more
         * accurate if the (1+x)/(1-x) ratio is close to 1, i.e. if x is close to 0.
         * This is often the case in Apache SIS since x is often a value close to the
         * Earth excentricity, which is a small value (0 would be a perfect sphere).
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
     * @param  value  the value to test.
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
     * {@snippet lang="java" :
     *   return (value == 0) && isPositive(value);
     *   }
     *
     * @param  value  the value to test.
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
     * @param  value  the value to test.
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
     * {@snippet lang="java" :
     *   return (value == 0) && isNegative(value);
     *   }
     *
     * @param  value  the value to test.
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
     * @param  v1  the first value.
     * @param  v2  the second value, to compare the sign with the first value.
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
     * Math.copySign}(value, sign)</code> except that the sign is combined with an <i>exclusive
     * or</i> operation instead of being copied.
     *
     * <p>This method makes no guarantee about whether {@code NaN} values are handled as positive
     * or negative numbers. This is the same policy as {@link Math#copySign(double, double)}.</p>
     *
     * @param  value  the parameter providing the value that may need a sign change.
     * @param  sign   the parameter providing the sign to <i>xor</i> with the value.
     * @return the provided value with its sign reversed if the {@code sign} parameter is negative.
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
     * @param  v1  the first value to compare.
     * @param  v2  the second value to compare.
     * @param  ε   the tolerance threshold, which must be positive.
     * @return {@code true} if both values are equal given the tolerance threshold.
     */
    public static boolean epsilonEqual(final float v1, final float v2, final float ε) {
        return (abs(v1 - v2) <= ε) || floatToIntBits(v1) == floatToIntBits(v2);
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
     * @param  v1  the first value to compare.
     * @param  v2  the second value to compare.
     * @param  ε   the tolerance threshold, which must be positive.
     * @return {@code true} if both values are equal given the tolerance threshold.
     */
    public static boolean epsilonEqual(final double v1, final double v2, final double ε) {
        return (abs(v1 - v2) <= ε) || doubleToLongBits(v1) == doubleToLongBits(v2);
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
     * Some of those bits, named the <i>payload</i>, can be used for storing custom information.
     * This method maps some of the payload values to each ordinal value.
     *
     * <p>This method guarantees that {@code toNanFloat(0)} returns the standard {@link Float#NaN} value.
     * For all other {@code ordinal} values, the relationship to the payload values is implementation dependent
     * and may change in any future version of the SIS library. The current implementation restricts the
     * range of allowed ordinal values to a smaller one than the range of all possible values.</p>
     *
     * @param  ordinal  the NaN ordinal value, from {@value #MIN_NAN_ORDINAL} to {@value #MAX_NAN_ORDINAL} inclusive.
     * @return one of the legal {@linkplain Float#isNaN(float) NaN} values as a float.
     * @throws IllegalArgumentException if the specified ordinal is out of range.
     *
     * @see Float#intBitsToFloat(int)
     */
    public static float toNanFloat(final int ordinal) throws IllegalArgumentException {
        ArgumentChecks.ensureBetween("ordinal", MIN_NAN_ORDINAL, MAX_NAN_ORDINAL, ordinal);
        int bits = (ordinal >= 0) ? ordinal : ~ordinal;
        bits = (bits + POSITIVE_NAN) | (ordinal & Integer.MIN_VALUE);
        assert Integer.compareUnsigned(bits, ordinal >= 0 ? POSITIVE_NAN : NEGATIVE_NAN) >= 0 : ordinal;
        final float value = intBitsToFloat(bits);
        assert Float.isNaN(value) && toNanOrdinal(value) == ordinal : ordinal;
        return value;
    }

    /**
     * Returns the ordinal value of the given NaN number.
     * This method is the converse of {@link #toNanFloat(int)}.
     *
     * <p>If the given float is the standard {@link Float#NaN} value, then this method returns 0.
     * For all other values, the relationship between the float payload and the returned ordinal
     * is implementation dependent and may change in any future Apache SIS version.</p>
     *
     * @param  value  the value from which to get the NaN ordinal value.
     * @return the NaN ordinal value of the given floating point value.
     * @throws IllegalArgumentException if the given value is not a NaN value,
     *         or does not use a supported bits pattern.
     */
    public static int toNanOrdinal(final float value) throws IllegalArgumentException {
        final int bits = floatToRawIntBits(value);
        int ordinal = (bits & Integer.MAX_VALUE) - POSITIVE_NAN;
        if (bits < 0) ordinal = ~ordinal;
        if (ordinal >= MIN_NAN_ORDINAL && ordinal <= MAX_NAN_ORDINAL) {
            return ordinal;
        }
        final short resourceKey;
        final Object obj;
        if (Float.isNaN(value)) {
            resourceKey = Errors.Keys.IllegalBitsPattern_1;
            obj = Integer.toHexString(bits);
        } else {
            resourceKey = Errors.Keys.IllegalArgumentValue_2;
            obj = new Object[] {"value", value};
        }
        throw new IllegalArgumentException(Errors.format(resourceKey, obj));
    }

    /**
     * Converts two long bits values containing a IEEE 754 quadruple precision floating point number
     * to a double precision floating point number. About 17 decimal digits of precision may be lost
     * due to the {@code double} type having only half the capacity of quadruple precision type.
     *
     * <p>Some quadruple precision values cannot be represented in double precision and are mapped
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
     * @param  l0  upper part of the quadruple precision floating point number.
     * @param  l1  lower part of the quadruple precision floating point number.
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
             * Since we convert them to double precision, subnormal numbers cannot be represented
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
             * might change the meaning, which could cause several issues later. Therefore we conservatively
             * collapse all NaNs to the default NaN for now (this may be revisited in a future SIS version).
             */
            return Double.NaN;
        }
        exp -= (16383 - Double.MAX_EXPONENT);       // Change from 15 bias to 11 bias.
        // Check cases where mantissa excess what double can support.
        if (exp < 0)    return Double.NEGATIVE_INFINITY;
        if (exp > 2046) return Double.POSITIVE_INFINITY;

        return Double.longBitsToDouble(sig | (exp << 52) | (l0 << 4) | (l1 >>> 60));
    }

    /**
     * Returns the <var>i</var><sup>th</sup> prime number.
     * This method returns (2, 3, 5, 7, 11, …) for index (0, 1, 2, 3, 4, …).
     *
     * @param  index  the prime number index, starting at index 0 for prime number 2.
     * @return the prime number at the specified index.
     * @throws IndexOutOfBoundsException if the specified index is too large.
     *
     * @see java.math.BigInteger#isProbablePrime(int)
     */
    static int primeNumberAt(final int index) throws IndexOutOfBoundsException {
        Objects.checkIndex(index, PRIMES_LENGTH_16_BITS);
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        short[] primes = MathFunctions.primes;
        if (index >= primes.length) {
            synchronized (MathFunctions.class) {
                primes = MathFunctions.primes;
                if (index >= primes.length) {
                    int i = primes.length;
                    int n = Short.toUnsignedInt(primes[i - 1]);
                    // Compute by block of 16 values, for reducing the number of array resizes.
                    primes = Arrays.copyOf(primes, min((index | 0xF) + 1, PRIMES_LENGTH_16_BITS));
                    do {
testNextNumber:         while (true) {      // Simulate a "goto" statement (usually not recommanded...)
                            final int stopAt = (int) sqrt(n += 2);
                            int prime;
                            int j = 0;
                            do {
                                prime = Short.toUnsignedInt(primes[++j]);
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
        return Short.toUnsignedInt(primes[index]);
    }

    /**
     * Returns the first prime number equals or greater than the given value.
     * Current implementation accepts only values in the
     * [2 … {@value #HIGHEST_SUPPORTED_PRIME_NUMBER}] range.
     *
     * @param  number  the number for which to find the next prime.
     * @return the given number if it is a prime number, or the next prime number otherwise.
     * @throws IllegalArgumentException if the given value is outside the supported range.
     *
     * @see java.math.BigInteger#isProbablePrime(int)
     */
    public static int nextPrimeNumber(final int number) throws IllegalArgumentException {
        ArgumentChecks.ensureBetween("number", 2, HIGHEST_SUPPORTED_PRIME_NUMBER, number);
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final short[] primes = MathFunctions.primes;
        int lower = 0;
        int upper = min(PRIMES_LENGTH_15_BITS, primes.length);
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
        return Short.toUnsignedInt(primes[i]);
    }

    /**
     * Returns the divisors of the specified number as positive integers. For any value other
     * than {@code O} (which returns an empty array), the first element in the returned array
     * is always {@code 1} and the last element is always the absolute value of {@code number}.
     *
     * @param  number  the number for which to compute the divisors.
     * @return the divisors in strictly increasing order.
     */
    public static int[] divisors(int number) {
        if (number == 0) {
            return ArraysExt.EMPTY_INT;
        }
        number = abs(number);
        int[] divisors = new int[16];
        divisors[0] = 1;
        int count = 1;
        /*
         * Searches for the first divisors among the prime numbers. We stop the search at the
         * square root of `n` because every values above that point can be inferred from the
         * values before that point, i.e. if n=p₁⋅p₂ and p₂ is greater than `sqrt`, than p₁
         * must be lower than `sqrt`.
         */
        for (int p,i=0; multiplyFull(p=primeNumberAt(i), p) <= number; i++) {
            if (number % p == 0) {
                if (count == divisors.length) {
                    divisors = Arrays.copyOf(divisors, count*2);
                }
                divisors[count++] = p;
            }
        }
        /*
         * Completes the divisors past `sqrt`. The numbers added here may or may not be prime
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
                final long m = multiplyFull(d1, divisors[j]);
                if (m > number) break;
                d2 = (int) m;
                if (number % d2 == 0) {
                    int p = Arrays.binarySearch(divisors, j, count, d2);
                    if (p < 0) {
                        p = ~p;                                 // tild (~) operator, not minus
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
     * Returns the positive divisors which are common to all the specified numbers.
     * The returned array always starts with value 1, unless the given value is 0
     * in which case this method returns an empty array.
     *
     * @param  numbers  the numbers for which to compute the divisors, in any order.
     * @return the divisors common to all the given numbers, in strictly increasing order.
     */
    public static int[] commonDivisors(final int... numbers) {
        if (numbers.length == 0) {
            return ArraysExt.EMPTY_INT;
        }
        /*
         * Get the smallest value. We will compute the divisors only for this value,
         * since we know that any value greater that the minimal value cannot be a
         * common divisor.
         */
        int minValue = Integer.MAX_VALUE;
        for (int i=0; i<numbers.length; i++) {
            final int n = abs(numbers[i]);
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
            final int n = abs(numbers[i]);
            if (n != minValue) {
                for (int j=count; --j>0;) {         // Do not test j==0, since divisors[0] ==  1.
                    if (n % divisors[j] != 0) {
                        System.arraycopy(divisors, j+1, divisors, j, --count - j);
                    }
                }
            }
        }
        return ArraysExt.resize(divisors, count);
    }

    /**
     * Returns the real (non-complex) roots of a polynomial equation having the given coefficients.
     * This method returns the <var>x</var> values for which <var>y</var>=0 in the following equation:
     *
     * <blockquote><var>y</var> =
     * <var>c<sub>0</sub></var> +
     * <var>c<sub>1</sub></var>⋅<var>x</var> +
     * <var>c<sub>2</sub></var>⋅<var>x</var><sup>2</sup> +
     * <var>c<sub>3</sub></var>⋅<var>x</var><sup>3</sup> + … +
     * <var>c<sub>n</sub></var>⋅<var>x</var><sup>n</sup>
     * </blockquote>
     *
     * Current implementation can resolve polynomials described by a maximum of 5 coefficients, ignoring
     * leading and trailing zeros. They correspond to linear, quadratic, cubic and quartic polynomials.
     *
     * @param  coefficients  the <var>c<sub>0</sub></var>, <var>c<sub>1</sub></var>, <var>c<sub>2</sub></var>, …
     *                       <var>c<sub>n</sub></var> coefficients, in that order.
     * @return the non-complex roots, or an empty array if none.
     * @throws UnsupportedOperationException if given arguments contain more non-zero coefficients than this method can handle.
     *
     * @see java.awt.geom.QuadCurve2D#solveQuadratic(double[])
     * @see java.awt.geom.CubicCurve2D#solveCubic(double[])
     *
     * @since 1.0
     */
    public static double[] polynomialRoots(final double... coefficients) {
        int upper = coefficients.length;
        while (upper > 0) {
            double a = coefficients[--upper];                   // Coefficient of the term with highest exponent.
            if (a == 0) {
                continue;                                       // Search the last non-zero coefficient.
            }
            double c;                                           // Coefficient of the term with lowest exponent.
            int lower = 0;
            while ((c = coefficients[lower]) == 0) lower++;
            switch (upper - lower) {
                /*
                 * c = 0
                 *
                 * Cannot compute x. We could return an arbitrary value if c = 0, but we rather return
                 * an empty array for keeping the number of root equals to the highest exponent.
                 */
                case 0: {
                    break;
                }
                /*
                 * ax + c = 0    →    x = -c/a
                 */
                case 1: {
                    final double x = -c / a;
                    if (Double.isNaN(x)) break;
                    return new double[] {x};
                }
                /*
                 * ax² + bx + c = 0    →    x = (-b ± √[b² - 4ac]) / (2a)
                 *
                 * Above equation is numerically unstable. More stable algorithm is given
                 * by Numerical Recipes §5.6 (quadratic equation), which is applied below.
                 */
                case 2: {
                    final double b = coefficients[lower + 1];
                    final double q = -0.5 * (b + copySign(sqrt(b*b - 4*a*c), b));
                    final double x1 = q/a;
                    final double x2 = c/q;
                    if (Double.isNaN(x1) && Double.isNaN(x2)) break;
                    return (x1 != x2) ? new double[] {x1, x2} : new double[] {x1};
                }
                /*
                 * x³ + ax² + bx + c = 0
                 *
                 * Numerical Recipes §5.6 (cubic equation) is applied below.
                 * Solution usually have either 1 or 3 roots.
                 */
                case 3: {
                    return refineRoots(coefficients, solveCubic(
                                       coefficients[lower + 2] / a,
                                       coefficients[lower + 1] / a,
                                       c / a, false));
                }
                /*
                 * x⁴ + ax³ + bx² + cx + d = 0
                 *
                 * https://dlmf.nist.gov/1.11 in "Quartic equations" section.
                 * This algorithm reduces the equation to a cubic equation.
                 */
                case 4: {
                    double b,d;
                    d = c / a;
                    c = coefficients[lower + 1] / a;
                    b = coefficients[lower + 2] / a;
                    a = coefficients[lower + 3] / a;
                    final double a2 = a*a;
                    final double p = fma(-3./8,   a2,        b);
                    final double q = fma( 1./8*   a2 - 1./2 *b, a, c);
                    final double r = fma(-3./256, a2,  1./16*b)*a2 + fma(-1./4*a, c, d);
                    final double[] roots = solveCubic(-2*p, p*p-4*r, q*q, true);
                    if (roots.length != 4) break;
                    for (int i=0; i<3; i++) {
                        roots[i] = sqrt(-roots[i]);
                    }
                    if (isPositive(q)) {
                        for (int i=0; i<3; i++) {
                            roots[i] = -roots[i];
                        }
                    }
                    final double α = roots[0], β = roots[1], γ = roots[2];
                    final double s = α + β + γ;
                    if (Double.isNaN(s)) break;
                    roots[0] = s/2 - (a /= 4);
                    roots[1] = (+α - β - γ)/2 - a;
                    roots[2] = (-α + β - γ)/2 - a;
                    roots[3] = (-α - β + γ)/2 - a;
                    return refineRoots(coefficients, removeDuplicated(roots));
                }
                default: {
                    throw new UnsupportedOperationException();
                }
            }
            break;
        }
        return ArraysExt.EMPTY_DOUBLE;
    }

    /**
     * Solves cubic equation x³ + ax² + bx + c = 0. The solution before simplification has either 1 or 3 roots.
     * The {@code quartic} argument specifies whether this cubic equation is used as a step for solving a quartic equation:
     *
     * <ul>
     *   <li>If {@code true}, then we are interested only in the 3 roots solution and we do not check for duplicated values.
     *       The length of returned array is 4 for allowing the caller to reuse the same array.</li>
     *   <li>If {@code false}, then this method may simplify the 3 roots to 2 roots if two of them are equal,
     *       or may return the 1 root solution. The length of returned array is the number of roots.</li>
     * </ul>
     */
    private static double[] solveCubic(double a, double b, double c, final boolean quartic) {
        final double Q = (a*a - 3*b) / 9;                           // Q from Numerical Recipes 5.6.10.
        final double R = (a*(a*a - 4.5*b) + 13.5*c) / 27;           // R from Numerical Recipes 5.6.10.
        final double Q3 = Q*Q*Q;
        final double R2 = R*R;
        a /= -3;                                                    // Last term of Numerical Recipes 5.6.12, 17 and 18.
        if (R2 < Q3) {
            /*
             * Numerical Recipes 5.6.11 and 5.6.12 uses acos(R/sqrt(Q³)). It is possible to rewrite as
             * atan2(sqrt(Q3 - R2), R) using the  cos(θ) = 1/√[1 + tan²θ]  trigonometric identity, but
             * this substitution seems to decrease accuracy instead of increasing it in our tests.
             */
            b = Math.acos(R/sqrt(Q3)) / 3;                          // θ from Numerical recipes 5.6.11, then b = θ/3.
            c = -2 * sqrt(Q);                                       // First part of Numerical Recipes 5.6.12.
            double[] roots = new double[quartic ? 4 : 3];
            roots[2] = fma(c, cos(b - 2*PI/3), a);
            roots[1] = fma(c, cos(b + 2*PI/3), a);
            roots[0] = fma(c, cos(b), a);
            if (!quartic) {
                roots = removeDuplicated(roots);
            }
            return roots;
        }
        if (!quartic) {
            b = -copySign(cbrt(abs(R) + sqrt(R2 - Q3)), R);         // A from Numerical Recipes 5.6.15.
            final double x = (b == 0 ? 0 : b + Q/b) + a;
            if (!Double.isNaN(x)) {
                return new double[] {x};
            }
        }
        return ArraysExt.EMPTY_DOUBLE;
    }

    /**
     * Remove duplicated values in the given array. This method is okay only for very small arrays (3 or 4 elements).
     * Duplicated values should be very rare and occur mostly as a consequence of rounding errors while computing the
     * roots of polynomial equations. Because if the algebraic solution has less roots than what we would expect from
     * the largest exponent (for example ax² + bx = 0 has only one root instead of two), then {@link #polynomialRoots}
     * should have reduced the equation to a lower degrees (ax + b = 0 in the above example), in which case there are
     * no duplicated roots to remove.
     */
    private static double[] removeDuplicated(double[] roots) {
        int i = 1;
next:   while (i < roots.length) {
            for (int j=i; --j >= 0;) {
                if (roots[j] == roots[i]) {
                    roots = ArraysExt.remove(roots, i, 1);
                    continue next;
                }
            }
            i++;
        }
        return roots;
    }

    /**
     * Tries to improves accuracy of polynomial roots by applying small displacements
     * to the <var>x</var> values using ∂y/∂x derivative around those values.
     * This refinement is significant in a {@link org.apache.sis.referencing.GeodesicsOnEllipsoid}
     * test checking the value of an μ(x²,y²) function.
     *
     * @param  coefficients  the user-specified coefficients.
     * @param  roots         the roots. This array will be modified in place.
     * @return {@code roots}.
     */
    private static double[] refineRoots(final double[] coefficients, final double[] roots) {
        for (int i=0; i < roots.length; i++) {
            double ymin = Double.POSITIVE_INFINITY;
            double x = roots[i];
            double dx;
            do {
                double px = 1;                              // Power of x: 1, x¹, x², x³, …
                double dy = 0;                              // First derivative of polynomial at x.
                double y  = coefficients[0];                // Value of polynomial at x.
                double ey = 0, edy = 0;                     // Error terms for Kahan summation algorithm.
                for (int j=1; j<coefficients.length; j++) {
                    final double c = coefficients[j];
                    double s;
                    s = c * (px *  i) + edy; edy = s + (dy - (dy += s));        // Kahan summation of dy.
                    s = c * (px *= x) +  ey;  ey = s + ( y - ( y += s));        // Kahan summation of y.
                }
                if (!(ymin > (ymin = abs(y)))) break;       // If result not better than previous result, stop.
                roots[i] = x;
                dx = y/dy;
            } while (x != (x -= dx) && Double.isFinite(x));
        }
        return roots;
    }
}
