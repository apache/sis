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

import org.apache.sis.util.Static;
import org.apache.sis.util.ComparisonMode;

import static java.lang.Math.max;
import static java.lang.Math.abs;


/**
 * Miscellaneous utilities methods working on floating point numbers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.4
 * @module
 */
public final class Numerics extends Static {
    /**
     * Relative difference tolerated when comparing floating point numbers using
     * {@link org.apache.sis.util.ComparisonMode#APPROXIMATIVE}.
     *
     * <p>Historically, this was the relative tolerance threshold for considering two
     * matrixes as equal. This value has been determined empirically in order to allow
     * {@link org.apache.sis.referencing.operation.transform.ConcatenatedTransform} to
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
     * @see org.apache.sis.internal.referencing.Formulas#LINEAR_TOLERANCE
     * @see org.apache.sis.internal.referencing.Formulas#ANGULAR_TOLERANCE
     */
    public static final double COMPARISON_THRESHOLD = 1E-14;

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
     * A prime number used for hash code computation. Value 31 is often used because
     * some modern compilers can optimize {@code x*31} as {@code (x << 5) - x}
     * (Josh Bloch, <cite>Effective Java</cite>).
     */
    private static final int PRIME_NUMBER = 31;

    /**
     * Do not allow instantiation of this class.
     */
    private Numerics() {
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
            result[i] = (int) Math.round(data[i]);
        }
        return result;
    }

    /**
     * Returns {@code true} if the given floats are equals. Positive and negative zero are
     * considered different, while a NaN value is considered equal to all other NaN values.
     *
     * @param  o1 The first value to compare.
     * @param  o2 The second value to compare.
     * @return {@code true} if both values are equal.
     *
     * @see Float#equals(Object)
     */
    public static boolean equals(final float o1, final float o2) {
        return Float.floatToIntBits(o1) == Float.floatToIntBits(o2);
    }

    /**
     * Returns {@code true} if the given doubles are equals. Positive and negative zero are
     * considered different, while a NaN value is considered equal to all other NaN values.
     *
     * @param  o1 The first value to compare.
     * @param  o2 The second value to compare.
     * @return {@code true} if both values are equal.
     *
     * @see Double#equals(Object)
     */
    public static boolean equals(final double o1, final double o2) {
        return Double.doubleToLongBits(o1) == Double.doubleToLongBits(o2);
    }

    /**
     * Returns {@code true} if the given values are approximatively equal,
     * up to the {@linkplain #COMPARISON_THRESHOLD comparison threshold}.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @return {@code true} If both values are approximatively equal.
     */
    public static boolean epsilonEqual(final double v1, final double v2) {
        final double threshold = COMPARISON_THRESHOLD * max(abs(v1), abs(v2));
        if (threshold == Double.POSITIVE_INFINITY || Double.isNaN(threshold)) {
            return Double.doubleToLongBits(v1) == Double.doubleToLongBits(v2);
        }
        return abs(v1 - v2) <= threshold;
    }

    /**
     * Returns {@code true} if the given values are approximatively equal given the comparison mode.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @param  mode The comparison mode to use for comparing the numbers.
     * @return {@code true} If both values are approximatively equal.
     */
    public static boolean epsilonEqual(final double v1, final double v2, final ComparisonMode mode) {
        switch (mode) {
            default: return equals(v1, v2);
            case APPROXIMATIVE: return epsilonEqual(v1, v2);
            case DEBUG: {
                final boolean equal = epsilonEqual(v1, v2);
                assert equal : "v1=" + v1 + " v2=" + v2 + " Δv=" + abs(v1-v2);
                return equal;
            }
        }
    }

    /**
     * Returns {@code true} if the following objects are floating point numbers ({@link Float} or
     * {@link Double} types) and approximatively equal. If the given object are not floating point
     * numbers, then this method returns {@code false} unconditionally on the assumption that
     * strict equality has already been checked before this method call.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @return {@code true} If both values are real number and approximatively equal.
     */
    public static boolean floatEpsilonEqual(final Object v1, final Object v2) {
        return (v1 instanceof Float || v1 instanceof Double) &&
               (v2 instanceof Float || v2 instanceof Double) &&
               epsilonEqual(((Number) v1).doubleValue(), ((Number) v2).doubleValue());
    }

    /**
     * Alters the given seed with the hash code value computed from the given value.
     *
     * @param  value The value whose hash code to compute.
     * @param  seed  The hash code value computed so far. If this method is invoked for the first
     *               field, then any arbitrary value (preferably different for each class) is okay.
     * @return An updated hash code value.
     */
    public static int hash(final float value, final int seed) {
        return seed * PRIME_NUMBER + Float.floatToIntBits(value);
    }

    /**
     * Alters the given seed with the hash code value computed from the given value.
     *
     * @param  value The value whose hash code to compute.
     * @param  seed  The hash code value computed so far. If this method is invoked for the first
     *               field, then any arbitrary value (preferably different for each class) is okay.
     * @return An updated hash code value.
     */
    public static int hash(final double value, final int seed) {
        return hash(Double.doubleToLongBits(value), seed);
    }

    /**
     * Alters the given seed with the hash code value computed from the given value.
     *
     * @param  value The value whose hash code to compute.
     * @param  seed  The hash code value computed so far. If this method is invoked for the first
     *               field, then any arbitrary value (preferably different for each class) is okay.
     * @return An updated hash code value.
     */
    public static int hash(final long value, final int seed) {
        return seed * PRIME_NUMBER + (((int) value) ^ ((int) (value >>> 32)));
    }
}
