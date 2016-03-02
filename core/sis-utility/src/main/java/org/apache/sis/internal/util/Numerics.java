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
package org.apache.sis.internal.util;

import java.util.Map;
import java.util.HashMap;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Static;
import org.apache.sis.util.ComparisonMode;

import static java.lang.Math.max;
import static java.lang.Math.abs;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Miscellaneous utilities methods working on floating point numbers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
public final class Numerics extends Static {
    /**
     * Some frequently used {@link Double} values. As of Java 7, those values do not
     * seem to be cached by {@link Double#valueOf(double)} like JDK does for integers.
     */
    private static final Map<Object,Object> CACHE = new HashMap<Object,Object>(32);
    static {
        cache(   0);
        cache(   1);
        cache(  10);
        cache(  60);
        cache(  90);
        cache( 100);
        cache( 180);
        cache( 180*60*60);
        cache( 360);
        cache(1000);
        cache(Double.POSITIVE_INFINITY);
        cache(Double.NaN);
    }

    /**
     * Helper method for the construction of the {@link #CACHE} map.
     */
    private static void cache(final double value) {
        Double boxed;
        boxed =  value; CACHE.put(boxed, boxed);
        boxed = -value; CACHE.put(boxed, boxed);
    }

    /**
     * Relative difference tolerated when comparing floating point numbers using
     * {@link org.apache.sis.util.ComparisonMode#APPROXIMATIVE}.
     *
     * <p>Historically, this was the relative tolerance threshold for considering two
     * matrixes as equal. This value has been determined empirically in order to allow
     * {@code org.apache.sis.referencing.operation.transform.ConcatenatedTransform} to
     * detect the cases where two {@link org.apache.sis.referencing.operation.transform.LinearTransform}
     * are equal for practical purpose. This threshold can be used as below:</p>
     *
     * {@preformat java
     *     Matrix m1 = ...;
     *     Matrix m2 = ...;
     *     if (Matrices.epsilonEqual(m1, m2, COMPARISON_THRESHOLD, true)) {
     *         // Consider that matrixes are equal.
     *     }
     * }
     *
     * By extension, the same threshold value is used for comparing other floating point values.
     *
     * <p>The current value is set to the smallest power of 10 which allow the
     * {@code org.apache.sis.test.integration.ConsistencyTest} to pass.</p>
     *
     * @see org.apache.sis.internal.referencing.Formulas#LINEAR_TOLERANCE
     * @see org.apache.sis.internal.referencing.Formulas#ANGULAR_TOLERANCE
     */
    public static final double COMPARISON_THRESHOLD = 1E-13;

    /**
     * Bit mask to isolate the sign bit of non-{@linkplain Double#isNaN(double) NaN} values in a
     * {@code double}. For any real value, the following code evaluate to 0 if the given value is
     * positive:
     *
     * {@preformat java
     *     Double.doubleToRawLongBits(value) & SIGN_BIT_MASK;
     * }
     *
     * Note that this idiom differentiates positive zero from negative zero.
     * It should be used only when such difference matter.
     *
     * @see org.apache.sis.math.MathFunctions#isPositive(double)
     * @see org.apache.sis.math.MathFunctions#isNegative(double)
     * @see org.apache.sis.math.MathFunctions#isSameSign(double, double)
     * @see org.apache.sis.math.MathFunctions#xorSign(double, double)
     */
    public static final long SIGN_BIT_MASK = Long.MIN_VALUE;

    /**
     * Number of bits in the significand (mantissa) part of IEEE 754 {@code double} representation,
     * <strong>not</strong> including the hidden bit.
     */
    public static final int SIGNIFICAND_SIZE = 52;

    /**
     * Number of bits in the significand (mantissa) part of IEEE 754 {@code float} representation,
     * <strong>not</strong> including the hidden bit.
     */
    public static final int SIGNIFICAND_SIZE_OF_FLOAT = 23;

    /**
     * Do not allow instantiation of this class.
     */
    private Numerics() {
    }

    /**
     * If the given value is presents in the cache, returns the cached value.
     * Otherwise returns the given value as-is.
     *
     * @param  <T> The type of the given value.
     * @param  value The given value for which to get a cached instance, if one exists.
     * @return An object equals to the given value (may be the given instance itself).
     */
    @SuppressWarnings("unchecked")
    public static <T> T cached(final T value) {
        final Object candidate = CACHE.get(value);
        return (candidate != null) ? (T) candidate : value;
    }

    /**
     * Wraps the given {@code value} in a {@link Double} wrapper, using one of the cached instance if possible.
     *
     * @param  value The value to get as a {@code Double}.
     * @return The given value as a {@code Double}.
     */
    public static Double valueOf(final double value) {
        final Double boxed = value;
        final Object candidate = CACHE.get(boxed);
        return (candidate != null) ? (Double) candidate : boxed;
    }

    /**
     * Returns a copy of the given array where each value has been casted to the {@code float} type.
     *
     * @param  data The array to copy, or {@code null}.
     * @return A copy of the given array with values casted to the {@code float} type, or
     *         {@code null} if the given array was null.
     */
    public static float[] copyAsFloats(final double[] data) {
        if (data == null) return null;
        final float[] result = new float[data.length];
        for (int i=0; i<data.length; i++) {
            result[i] = (float) data[i];
        }
        return result;
    }

    /**
     * Returns a copy of the given array where each value has been
     * {@linkplain Math#round(double) rounded} to the {@code int} type.
     *
     * @param  data The array to copy, or {@code null}.
     * @return A copy of the given array with values rounded to the {@code int} type, or
     *         {@code null} if the given array was null.
     */
    public static int[] copyAsInts(final double[] data) {
        if (data == null) return null;
        final int[] result = new int[data.length];
        for (int i=0; i<data.length; i++) {
            result[i] = JDK8.toIntExact(Math.round(data[i]));
        }
        return result;
    }

    /**
     * Returns {@code true} if the given floats are equals. Positive and negative zero are
     * considered different, while a NaN value is considered equal to all other NaN values.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @return {@code true} if both values are equal.
     *
     * @see Float#equals(Object)
     */
    public static boolean equals(final float v1, final float v2) {
        return Float.floatToIntBits(v1) == Float.floatToIntBits(v2);
    }

    /**
     * Returns {@code true} if the given doubles are equal.
     * Positive and negative zeros are considered different.
     * NaN values are considered equal to all other NaN values.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @return {@code true} if both values are equal.
     *
     * @see Double#equals(Object)
     */
    public static boolean equals(final double v1, final double v2) {
        return Double.doubleToLongBits(v1) == Double.doubleToLongBits(v2);
    }

    /**
     * Returns {@code true} if the given doubles are equal, ignoring the sign of zero values.
     * NaN values are considered equal to all other NaN values.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @return {@code true} if both values are equal.
     */
    public static boolean equalsIgnoreZeroSign(final double v1, final double v2) {
        return (v1 == v2) || Double.doubleToLongBits(v1) == Double.doubleToLongBits(v2);
    }

    /**
     * Returns {@code true} if the given values are approximatively equal, up to the given comparison threshold.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @param  threshold The comparison threshold.
     * @return {@code true} if both values are approximatively equal.
     */
    public static boolean epsilonEqual(final double v1, final double v2, final double threshold) {
        return (abs(v1 - v2) <= threshold) || equals(v1, v2);
    }

    /**
     * Returns {@code true} if the given values are approximatively equal given the comparison mode.
     * In mode {@code APPROXIMATIVE} or {@code DEBUG}, this method will compute a relative comparison
     * threshold from the {@link #COMPARISON_THRESHOLD} constant.
     *
     * <p>This method does not thrown {@link AssertionError} in {@link ComparisonMode#DEBUG}.
     * It is caller responsibility to handle the {@code DEBUG} case.</p>
     *
     * @param  v1   The first value to compare.
     * @param  v2   The second value to compare.
     * @param  mode The comparison mode to use for comparing the numbers.
     * @return {@code true} if both values are considered equal for the given comparison mode.
     */
    public static boolean epsilonEqual(final double v1, final double v2, final ComparisonMode mode) {
        if (mode.isApproximative()) {
            final double mg = max(abs(v1), abs(v2));
            if (mg != Double.POSITIVE_INFINITY) {
                return epsilonEqual(v1, v2, COMPARISON_THRESHOLD * mg);
            }
        }
        return equals(v1, v2);
    }

    /**
     * Creates a messages to put in {@link AssertionError} when two values differ in an unexpected way.
     * This is a helper method for debugging purpose only, typically used with {@code assert} statements.
     *
     * @param name The name of the property which differ, or {@code null} if unknown.
     * @param v1   The first value.
     * @param v2   The second value.
     * @return The message to put in {@code AssertionError}.
     *
     * @since 0.6
     */
    @Debug
    public static String messageForDifference(final String name, final double v1, final double v2) {
        final StringBuilder builder = new StringBuilder();
        if (name != null) {
            builder.append(name).append(": ");
        }
        builder.append("values ").append(v1).append(" and ").append(v2).append(" differ");
        final float delta = (float) abs(v1 - v2);
        if (delta < Float.POSITIVE_INFINITY) {
            builder.append(" by ").append(delta);
        }
        return builder.toString();
    }

    /**
     * Returns a hash code value for the given long.
     *
     * @param  c The value to hash.
     * @return Hash code value for the given long.
     */
    public static int hashCode(final long c) {
        return ((int) c) ^ (int) (c >>> Integer.SIZE);
    }

    /**
     * Converts a power of 2 to a power of 10, rounded toward negative infinity.
     * This method is equivalent to the following code, but using only integer arithmetic:
     *
     * {@preformat java
     *     return (int) Math.floor(exp2 * LOG10_2);
     * }
     *
     * This method is valid only for arguments in the [-2620 … 2620] range, which is more than enough
     * for the range of {@code double} exponents. We do not put this method in public API because it
     * does not check the argument validity.
     *
     * <div class="section">Arithmetic notes</div>
     * {@code toExp10(getExponent(10ⁿ))} returns <var>n</var> only for {@code n == 0}, and <var>n</var>-1 in all other
     * cases. This is because 10ⁿ == m × 2<sup>exp2</sup> where the <var>m</var> significand is always greater than 1,
     * which must be compensated by a smaller {@code exp2} value such as {@code toExp10(exp2) < n}. Note that if the
     * {@code getExponent(…)} argument is not a power of 10, then the result can be either <var>n</var> or <var>n</var>-1.
     *
     * @param  exp2 The power of 2 to convert Must be in the [-2620 … 2620] range.
     * @return The power of 10, rounded toward negative infinity.
     *
     * @see org.apache.sis.math.MathFunctions#LOG10_2
     * @see org.apache.sis.math.MathFunctions#getExponent(double)
     */
    public static int toExp10(final int exp2) {
        /*
         * Compute:
         *          exp2 × (log10(2) × 2ⁿ) / 2ⁿ
         * where:
         *          n = 20   (arbitrary value)
         *
         * log10(2) × 2ⁿ  =  315652.82873335475, which we round to 315653.
         *
         * The range of valid values for such approximation is determined
         * empirically by running the NumericsTest.testToExp10() method.
         */
        assert exp2 >= -2620 && exp2 <= 2620 : exp2;
        return (exp2 * 315653) >> 20;
    }

    /**
     * Returns the significand <var>m</var> of the given value such as {@code value = m×2ⁿ}
     * where <var>n</var> is {@link Math#getExponent(double)} - {@value #SIGNIFICAND_SIZE}.
     * For any non-NaN values (including infinity), the following relationship holds:
     *
     * {@preformat java
     *    assert Math.scalb(getSignificand(value), Math.getExponent(value) - SIGNIFICAND_SIZE) == Math.abs(value);
     * }
     *
     * For negative values, this method behaves as if the value was positive.
     *
     * @param  value The value for which to get the significand.
     * @return The significand of the given value.
     */
    public static long getSignificand(final double value) {
        long bits = Double.doubleToRawLongBits(value);
        final long exponent = bits & (0x7FFL << SIGNIFICAND_SIZE);
        bits &= (1L << SIGNIFICAND_SIZE) - 1;
        if (exponent != 0) {
            bits |= (1L << SIGNIFICAND_SIZE);
        } else {
            /*
             * Sub-normal value: compensate for the fact that Math.getExponent(value) returns
             * Double.MIN_EXPONENT - 1 in this case, while we would need Double.MIN_EXPONENT.
             */
            bits <<= 1;
        }
        return bits;
    }

    /**
     * Returns the significand <var>m</var> of the given value such as {@code value = m×2ⁿ} where
     * <var>n</var> is {@link Math#getExponent(float)} - {@value #SIGNIFICAND_SIZE_OF_FLOAT}.
     * For any non-NaN positive values (including infinity), the following relationship holds:
     *
     * {@preformat java
     *    assert Math.scalb(getSignificand(value), Math.getExponent(value) - SIGNIFICAND_SIZE_OF_FLOAT) == value;
     * }
     *
     * For negative values, this method behaves as if the value was positive.
     *
     * @param  value The value for which to get the significand.
     * @return The significand of the given value.
     */
    public static int getSignificand(final float value) {
        int bits = Float.floatToRawIntBits(value);
        final int exponent = bits & (0xFF << SIGNIFICAND_SIZE_OF_FLOAT);
        bits &= (1L << SIGNIFICAND_SIZE_OF_FLOAT) - 1;
        if (exponent != 0) {
            bits |= (1L << SIGNIFICAND_SIZE_OF_FLOAT);
        } else {
            bits <<= 1;
        }
        return bits;
    }
}
