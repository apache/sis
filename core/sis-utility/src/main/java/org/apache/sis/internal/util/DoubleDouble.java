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

import java.util.Arrays;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.math.DecimalFunctions;
// No BigDecimal dependency - see class javadoc

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Basic arithmetic methods for extended precision numbers using the <cite>double-double</cite> algorithm.
 * This class implements some of the methods published in the following paper:
 *
 * <ul>
 *   <li>Yozo Hida, Xiaoye S. Li, David H. Bailey.
 *       <a href="http://web.mit.edu/tabbott/Public/quaddouble-debian/qd-2.3.4-old/docs/qd.pdf">Library
 *       for Double-Double and Quad-Double arithmetic</a>, 2007.</li>
 *   <li>Jonathan R. Shewchuk. Adaptive precision floating-point arithmetic and fast robust geometric predicates.
 *       Discrete &amp; Computational Geometry, 18(3):305–363, 1997.</li>
 * </ul>
 *
 * {@code DoubleDouble} is used as an alternative to {@link java.math.BigDecimal} when we do not need arbitrary
 * precision, we do not want to convert from base 2 to base 10, we need support for NaN and infinities, we want
 * more compact storage and better performance. {@code DoubleDouble} can be converted to {@code BigDecimal} as
 * below:
 *
 * {@preformat java
 *     BigDecimal decimal = new BigDecimal(dd.value).add(new BigDecimal(dd.error));
 * }
 *
 * We do not provide convenience method for the above in order to avoid dependency to {@code BigDecimal}.
 *
 * <div class="section">Impact of availability of FMA instructions</div>
 * If <cite>fused multiply-add</cite> (FMA) instruction are available in a future Java version
 * (see <a href="https://issues.apache.org/jira/browse/SIS-136">SIS-136</a> on Apache SIS JIRA),
 * then the following methods should be revisited:
 *
 * <ul>
 *   <li>{@link #setToProduct(double, double)} - revisit with [Hida &amp; al.] algorithm 7.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 *
 * @see <a href="http://en.wikipedia.org/wiki/Double-double_%28arithmetic%29#Double-double_arithmetic">Wikipedia: Double-double arithmetic</a>
 */
public final class DoubleDouble extends Number {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7602414219228638550L;

    /**
     * {@code true} for disabling the extended precision. This variable should always be {@code false},
     * except for testing purpose. If set to {@code true}, then all double-double arithmetic operations
     * are immediately followed by a clearing of {@link DoubleDouble#error}.  The result should then be
     * identical to computation performed using the normal {@code double} arithmetic.
     *
     * <p>Since this flag is static final, all expressions of the form {@code if (DISABLED)} should be
     * omitted by the compiler from the class files in normal operations.</p>
     *
     * <p>Setting this flag to {@code true} causes some JUnit tests to fail. This is normal. The main
     * purpose of this flag is to allow {@link org.apache.sis.referencing.operation.matrix.MatrixTestCase}
     * to perform strict comparisons of matrix operation results with JAMA, which is taken as the reference
     * implementation. Since JAMA uses {@code double} arithmetic, SIS needs to disable {@code double-double}
     * arithmetic if the results are to be compared for strict equality.</p>
     */
    public static final boolean DISABLED = false;

    /**
     * When computing <var>a</var> - <var>b</var> as a double-double (106 significand bits) value,
     * if the amount of non-zero significand bits is equals or lower than {@code ZERO_THRESHOLD+1},
     * consider the result as zero.
     */
    private static final int ZERO_THRESHOLD = 2;

    /**
     * The split constant used as part of multiplication algorithms. The split algorithm is as below
     * (we have to inline it in multiplication methods because Java can not return multi-values):
     *
     * {@preformat java
     *     private void split(double a) {
     *         double t   = SPLIT * a;
     *         double ahi = t - (t - a);
     *         double alo = a - ahi;
     *     }
     * }
     *
     * <p>Source: [Hida &amp; al.] page 4 algorithm 5, itself reproduced from [Shewchuk] page 325.</p>
     */
    private static final double SPLIT = (1 << 27) + 1;

    /**
     * Maximal value that can be handled by {@link #multiply(double, double)}.
     * If a multiplication is using a value greater than {@code MAX_VALUE},
     * then the result will be infinity or NaN.
     */
    public static final double MAX_VALUE = Double.MAX_VALUE / SPLIT;

    /**
     * Pre-defined constants frequently used in SIS, sorted in increasing order. This table contains only
     * constants that can not be inferred by {@link DecimalFunctions#deltaForDoubleToDecimal(double)},
     * for example some transcendental values.
     *
     * <p>Elements in this array shall be sorted in strictly increasing order.
     * For any value at index <var>i</var>, the associated error is {@code ERRORS[i]}.
     *
     * @see #errorForWellKnownValue(double)
     */
    private static final double[] VALUES = {
        // Some of the following constants have more fraction digits than necessary. We declare the extra
        // digits for documentation purpose, and in order to have identical content than DoubleDoubleTest
        // so that a plain copy-and-paste can be performed between those two classes.
         0.000004848136811095359935899141023579480, // Arc-second to radians
         0.0002777777777777777777777777777777778,   // Second to degrees
         0.002777777777777777777777777777777778,    // 1/360°
         0.01666666666666666666666666666666667,     // Minute to degrees
         0.01745329251994329576923690768488613,     // Degrees to radians
         0.785398163397448309615660845819876,       // π/4
         1.111111111111111111111111111111111,       // Gradian to degrees
         1.414213562373095048801688724209698,       // √2
         1.570796326794896619231321691639751,       // π/2
         2.356194490192344928846982537459627,       // π * 3/4
         3.14159265358979323846264338327950,        // π
         6.28318530717958647692528676655901,        // 2π
        57.2957795130823208767981548141052          // Radians to degrees
    };

    /**
     * The errors associated to the values in the {@link #VALUES} array.
     *
     * <p>Tips:</p>
     * <ul>
     *   <li>To compute a new value in this array, just put zero and execute
     *       {@code DoubleDoubleTest.testErrorForWellKnownValue()}.
     *       The error message will give the expected value.</li>
     *   <li>If a computed value is zero, then there is no point to create an entry
     *       in the {@code (VALUES, ERRORS)} arrays for that value.</li>
     * </ul>
     */
    private static final double[] ERRORS = {
        /*  0.000004… */  9.320078015422868E-23,
        /*  0.000266… */  2.4093381610788987E-22,
        /*  0.002666… */ -1.0601087908747154E-19,
        /*  0.016666… */  2.312964634635743E-19,
        /*  0.017453… */  2.9486522708701687E-19,
        /*  0.785398… */  3.061616997868383E-17,
        /*  1.111111… */ -4.9343245538895844E-17,
        /*  1.414213… */ -9.667293313452913E-17,
        /*  1.570796… */  6.123233995736766E-17,
        /*  2.356194… */  9.184850993605148E-17,
        /*  3.141592… */  1.2246467991473532E-16,
        /*  6.283185… */  2.4492935982947064E-16,
        /* 57.295779… */ -1.9878495670576283E-15
    };

    /**
     * The main value, minus the {@link #error}.
     */
    public double value;

    /**
     * The error that shall be added to the main {@link #value} in order to get the
     * <cite>"real"</cite> (actually <cite>"the most accurate that we can"</cite>) value.
     */
    public double error;

    /**
     * Creates a new value initialized to zero.
     */
    public DoubleDouble() {
    }

    /**
     * Creates a new value initialized to the given value.
     *
     * @param other The other value to copy.
     */
    public DoubleDouble(final DoubleDouble other) {
        value = other.value;
        error = other.error;
    }

    /**
     * Creates a new value initialized to the given number. If the given number is an
     * instance of {@code DoubleDouble}, then its error term will be taken in account.
     *
     * @param otherValue The initial value.
     */
    public DoubleDouble(final Number otherValue) {
        value = otherValue.doubleValue();
        error = (otherValue instanceof DoubleDouble) ? ((DoubleDouble) otherValue).error : errorForWellKnownValue(value);
    }

    /**
     * Creates a new value initialized to the given value and an error term inferred by
     * {@link #errorForWellKnownValue(double)}.
     *
     * <b>Tip:</b> if the other value is known to be an integer or a power of 2, then invoking
     * <code>{@linkplain #DoubleDouble(double, double) DoubleDouble}(otherValue, 0)</code> is more efficient.
     *
     * @param value The initial value.
     */
    public DoubleDouble(final double value) {
        this.value = value;
        this.error = errorForWellKnownValue(value);
    }

    /**
     * Creates a new value initialized to the given value and error.
     * It is caller's responsibility to ensure that the (value, error) pair is normalized.
     *
     * @param value The initial value.
     * @param error The initial error.
     */
    public DoubleDouble(final double value, final double error) {
        this.value = value;
        this.error = error;
        assert !(Math.abs(error) >= Math.ulp(value)) : this; // Use ! for being tolerant to NaN.
    }

    /**
     * Uses the given value verbatim, without inferring an error term for double-double arithmetic.
     * We use this method when the value has been computed using transcendental functions (cosine,
     * logarithm, <i>etc.</i>) in which case there is no way we can infer a meaningful error term.
     *
     * <p>We use this method both for readability and for making easier to search where such thing occur.</p>
     *
     * @param  value The value to wrap in a {@code DoubleDouble} instance.
     * @return A {@code DoubleDouble} containing exactly the given value, without error term.
     */
    public static DoubleDouble verbatim(final double value) {
        return new DoubleDouble(value, 0);
    }

    /**
     * Returns a new {@code DoubleDouble} instance initialized to the conversion factor
     * from radians to angular degrees.
     *
     * @return An instance initialized to the 57.2957795130823208767981548141052 value.
     */
    public static DoubleDouble createRadiansToDegrees() {
        return new DoubleDouble(57.2957795130823208767981548141052, -1.9878495670576283E-15);
    }

    /**
     * Returns a new {@code DoubleDouble} instance initialized to the conversion factor
     * from angular degrees to radians.
     *
     * @return An instance initialized to the 0.01745329251994329576923690768488613 value.
     */
    public static DoubleDouble createDegreesToRadians() {
        return new DoubleDouble(0.01745329251994329576923690768488613, 2.9486522708701687E-19);
    }

    /**
     * Returns a new {@code DoubleDouble} instance initialized to the conversion factor
     * from arc-seconds to radians.
     *
     * @return An instance initialized to the 0.000004848136811095359935899141023579480 value.
     */
    public static DoubleDouble createSecondsToRadians() {
        return new DoubleDouble(0.000004848136811095359935899141023579480, 9.320078015422868E-23);
    }

    /** @return {@link #value}. */
    @Override public double doubleValue() {return value;}
    @Override public float  floatValue()  {return (float) value;}
    @Override public long   longValue()   {return Math.round(value);}
    @Override public int    intValue()    {return JDK8.toIntExact(longValue());}

    /**
     * Suggests an {@link #error} for the given value. The {@code DoubleDouble} class contains a hard-coded list
     * of some frequently used constants, for example for various factors of π. If the given value matches exactly
     * one of those constants, then its error term is returned. Otherwise this method assumes that the given value
     * is defined in base 10 (e.g. many unit conversion factors) and tries to compute an error term with
     * {@link DecimalFunctions#deltaForDoubleToDecimal(double)}.
     *
     * <div class="section">Rational</div>
     * SIS often creates matrices for unit conversions, and most conversion factors are defined precisely in base 10.
     * For example the conversion from feet to metres is defined by a factor of exactly 0.3048, which can not be
     * represented precisely as a {@code double}. Consequently if a value of 0.3048 is given, we can assume that
     * the intend was to provide the "feet to metres" conversion factor and complete the double-double instance
     * accordingly.
     *
     * @param  value The value for which to get this error.
     * @return The error for the given value, or 0 if unknown. In the later case,
     *         the base 2 representation of the given value is assumed to be accurate enough.
     */
    public static double errorForWellKnownValue(final double value) {
        if (DISABLED) return 0;
        final int i = Arrays.binarySearch(VALUES, Math.abs(value));
        if (i >= 0) {
            return MathFunctions.xorSign(ERRORS[i], value);
        }
        final double delta = DecimalFunctions.deltaForDoubleToDecimal(value);
        return Double.isNaN(delta) ? 0 : delta;
    }

    /**
     * Returns {@code true} if this {@code DoubleDouble} is equals to zero.
     *
     * @return {@code true} if this {@code DoubleDouble} is equals to zero.
     */
    public boolean isZero() {
        return value == 0 && error == 0;
    }

    /**
     * Resets the {@link #value} and {@link #error} terms to zero.
     */
    public void clear() {
        value = 0;
        error = 0;
    }

    /**
     * Sets this {@code DoubleDouble} to the same value than the given instance.
     *
     * @param other The instance to copy.
     */
    public void setFrom(final DoubleDouble other) {
        value = other.value;
        error = other.error;
    }

    /**
     * Sets the {@link #value} and {@link #error} terms to values read from the given array.
     * This is a convenience method for a frequently used operation, implemented as below:
     *
     * {@preformat java
     *   value = array[index];
     *   error = array[index + errorOffset];
     * }
     *
     * @param array        The array from which to get the value and error.
     * @param index        Index of the value in the given array.
     * @param errorOffset  Offset to add to {@code index} in order to get the index of the error in the given array.
     */
    public void setFrom(final double[] array, final int index, final int errorOffset) {
        value = array[index];
        error = array[index + errorOffset];
    }

    /**
     * Equivalent to a call to {@code setToQuickSum(value, error)} inlined.
     * This is invoked after addition or multiplication operations.
     */
    final void normalize() {
        error += (value - (value += error));
        if (DISABLED) error = 0;
    }

    /**
     * Sets this {@code DoubleDouble} to the sum of the given numbers,
     * to be used only when {@code abs(a) >= abs(b)}.
     *
     * <p>Source: [Hida &amp; al.] page 4 algorithm 3, itself reproduced from [Shewchuk] page 312.</p>
     *
     * @param a The first number to add.
     * @param b The second number to add, which must be smaller than {@code a}.
     */
    public void setToQuickSum(final double a, final double b) {
        value = a + b;
        error = b - (value - a);
        if (DISABLED) error = 0;
    }

    /**
     * Sets this {@code DoubleDouble} to the sum of the given numbers.
     *
     * <p>Source: [Hida &amp; al.] page 4 algorithm 4, itself reproduced from [Shewchuk] page 314.</p>
     *
     * @param a The first number to add.
     * @param b The second number to add.
     */
    public void setToSum(final double a, final double b) {
        value = a + b;
        final double v = value - a;
        error = (a - (value - v)) + (b - v);
        if (DISABLED) error = 0;
    }

    /**
     * Sets this {@code DoubleDouble} to the product of the given numbers.
     * The given numbers shall not be greater than {@value #MAX_VALUE} in magnitude.
     *
     * <p>Source: [Hida &amp; al.] page 4 algorithm 6, itself reproduced from [Shewchuk] page 326.</p>
     *
     * @param a The first number to multiply.
     * @param b The second number to multiply.
     */
    public void setToProduct(final double a, final double b) {
        value = a * b;
        double t = SPLIT * a;
        final double ahi = t - (t - a);
        final double alo = a - ahi;
        t = SPLIT * b;
        final double bhi = t - (t - b);
        final double blo = b - bhi;
        error = ((ahi*bhi - value) + ahi*blo + alo*bhi) + alo*blo;
        if (DISABLED) error = 0;
    }

    /**
     * Stores the {@link #value} and {@link #error} terms in the given array.
     * This is a convenience method for a frequently used operation, implemented as below:
     *
     * {@preformat java
     *   array[index] = value;
     *   array[index + errorOffset] = error;
     * }
     *
     * @param array        The array where to store the value and error.
     * @param index        Index of the value in the given array.
     * @param errorOffset  Offset to add to {@code index} in order to get the index of the error in the given array.
     */
    public void storeTo(final double[] array, final int index, final int errorOffset) {
        array[index] = value;
        array[index + errorOffset] = error;
    }

    /**
     * Swaps two double-double values in the given array.
     *
     * @param array        The array where to swap the values and errors.
     * @param i0           Index of the first value to swap.
     * @param i1           Index of the second value to swap.
     * @param errorOffset  Offset to add to the indices in order to get the error indices in the given array.
     *
     * @see org.apache.sis.util.ArraysExt#swap(double[], int, int)
     */
    public static void swap(final double[] array, int i0, int i1, final int errorOffset) {
        double t = array[i0];
        array[i0] = array[i1];
        array[i1] = t;
        t = array[i0 += errorOffset];
        array[i0] = array[i1 += errorOffset];
        array[i1] = t;
    }

    /**
     * Sets this number to {@code -this}.
     */
    public void negate() {
        value = -value;
        error = -error;
    }

    /**
     * Adds an other double-double value to this {@code DoubleDouble}.
     * This is a convenience method for:
     *
     * {@preformat java
     *    add(other.value, other.error);
     * }
     *
     * @param other The other value to add to this {@code DoubleDouble}.
     */
    public void add(final DoubleDouble other) {
        add(other.value, other.error);
    }

    /**
     * Adds a {@code Number} value to this {@code DoubleDouble}. If the given number is an instance
     * of {@code DoubleDouble}, then its error term will be taken in account.
     *
     * @param other The other value to add to this {@code DoubleDouble}.
     */
    public void add(final Number other) {
        if (other instanceof DoubleDouble) {
            add((DoubleDouble) other);
        } else {
            add(other.doubleValue());
        }
    }

    /**
     * Adds a {@code double} value to this {@code DoubleDouble} with a default error term.
     * This is a convenience method for:
     *
     * {@preformat java
     *    add(otherValue, errorForWellKnownValue(otherValue));
     * }
     *
     * <b>Tip:</b> if the other value is known to be an integer or a power of 2, then invoking
     * <code>{@linkplain #add(double, double) add}(otherValue, 0)</code> is more efficient.
     *
     * @param otherValue The other value to add to this {@code DoubleDouble}.
     */
    public void add(final double otherValue) {
        add(otherValue, errorForWellKnownValue(otherValue));
    }

    /**
     * Adds an other double-double value to this {@code DoubleDouble}.
     * The result is stored in this instance.
     *
     * <div class="section">Implementation</div>
     * If <var>a</var> and <var>b</var> are {@code DoubleDouble} instances, then:
     *
     *   <blockquote>(a + b)</blockquote>
     *
     * can be computed as:
     *
     *   <blockquote>(a.value + a.error) + (b.value + b.error)<br>
     *             = (a.value + b.value) + (a.error + b.error)</blockquote>
     *
     * keeping in mind that the result of (a.value + b.value) has itself an error
     * which needs to be added to (a.error + b.error). In Java code:
     *
     * {@preformat java
     *   final double thisError = this.error;
     *   setToSum(value, otherValue);
     *   error += thisError;
     *   error += otherError;
     *   setToQuickSum(value, error);
     * }
     *
     * @param otherValue The other value to add to this {@code DoubleDouble}.
     * @param otherError The error of the other value to add to this {@code DoubleDouble}.
     */
    public void add(final double otherValue, final double otherError) {
        // Inline expansion of the code in above javadoc.
        double v = value;
        value += otherValue;
        error += v - (value + (v -= value)) + (otherValue + v);
        error += otherError;
        if (value == 0 && error != 0) {
            /*
             * The two values almost cancelled, only their error terms are different.
             * The number of significand bits (mantissa) in the IEEE 'double' representation is 52,
             * not counting the hidden bit. So estimate the accuracy of the double-double number as
             * the accuracy of the 'double' value (which is 1 ULP) scaled as if we had 52 additional
             * significand bits (we ignore some more bits if ZERO_THRESHOLD is greater than 0).
             * If the error is not greater than that value, then assume that it is not significant.
             */
            if (Math.abs(error) <= Math.scalb(Math.ulp(otherValue), ZERO_THRESHOLD - Numerics.SIGNIFICAND_SIZE)) {
                error = 0;
                return;
            }
        }
        normalize();
    }

    /**
     * Adds an other double-double value to this {@code DoubleDouble}, reading the values from an array.
     * This is a convenience method for a frequently used operation, implemented as below:
     *
     * {@preformat java
     *    add(array[index], array[index + errorOffset]);
     * }
     *
     * @param array        The array from which to get the value and error.
     * @param index        Index of the value in the given array.
     * @param errorOffset  Offset to add to {@code index} in order to get the index of the error in the given array.
     */
    public void add(final double[] array, final int index, final int errorOffset) {
        add(array[index], array[index + errorOffset]);
    }

    /**
     * Subtracts an other double-double value from this {@code DoubleDouble}.
     * This is a convenience method for:
     *
     * {@preformat java
     *    subtract(other.value, other.error);
     * }
     *
     * @param other The other value to subtract from this value.
     */
    public void subtract(final DoubleDouble other) {
        subtract(other.value, other.error);
    }

    /**
     * Subtracts a {@code Number} from this {@code DoubleDouble}. If the given number is an instance
     * of {@code DoubleDouble}, then its error term will be taken in account.
     *
     * @param other The other value to subtract from this {@code DoubleDouble}.
     */
    public void subtract(final Number other) {
        if (other instanceof DoubleDouble) {
            subtract((DoubleDouble) other);
        } else {
            subtract(other.doubleValue());
        }
    }

    /**
     * Subtracts a {@code double} from this {@code DoubleDouble} with a default error term.
     * This is a convenience method for:
     *
     * {@preformat java
     *    subtract(otherValue, errorForWellKnownValue(otherValue));
     * }
     *
     * <b>Tip:</b> if the other value is known to be an integer or a power of 2, then invoking
     * <code>{@linkplain #subtract(double, double) subtract}(otherValue, 0)</code> is more efficient.
     *
     * @param otherValue The other value to subtract from this {@code DoubleDouble}.
     */
    public void subtract(final double otherValue) {
        subtract(otherValue, errorForWellKnownValue(otherValue));
    }

    /**
     * Subtracts an other double-double value from this {@code DoubleDouble}.
     * The result is stored in this instance.
     *
     * @param otherValue The other value to subtract from this {@code DoubleDouble}.
     * @param otherError The error of the other value to subtract from this {@code DoubleDouble}.
     */
    public void subtract(final double otherValue, final double otherError) {
        add(-otherValue, -otherError);
    }

    /**
     * Subtracts an other double-double value from this {@code DoubleDouble}, reading the values from an array.
     * This is a convenience method for a frequently used operation, implemented as below:
     *
     * {@preformat java
     *    subtract(array[index], array[index + errorOffset]);
     * }
     *
     * @param array        The array from which to get the value and error.
     * @param index        Index of the value in the given array.
     * @param errorOffset  Offset to add to {@code index} in order to get the index of the error in the given array.
     */
    public void subtract(final double[] array, final int index, final int errorOffset) {
        subtract(array[index], array[index + errorOffset]);
    }

    /**
     * Multiplies this {@code DoubleDouble} by an other double-double value.
     * This is a convenience method for:
     *
     * {@preformat java
     *    multiply(other.value, other.error);
     * }
     *
     * @param other The other value to multiply by this value.
     */
    public void multiply(final DoubleDouble other) {
        multiply(other.value, other.error);
    }

    /**
     * Multiplies this {@code DoubleDouble} by a {@code Number}. If the given number is an instance
     * of {@code DoubleDouble}, then its error term will be taken in account.
     *
     * @param other The other value to multiply by this {@code DoubleDouble}.
     */
    public void multiply(final Number other) {
        if (other instanceof DoubleDouble) {
            multiply((DoubleDouble) other);
        } else {
            multiply(other.doubleValue());
        }
    }

    /**
     * Multiplies this {@code DoubleDouble} by a {@code double} with a default error term.
     * This is a convenience method for:
     *
     * {@preformat java
     *    multiply(otherValue, errorForWellKnownValue(otherValue));
     * }
     *
     * <b>Tip:</b> if the other value is known to be an integer or a power of 2, then invoking
     * <code>{@linkplain #multiply(double, double) multiply}(otherValue, 0)</code> is more efficient.
     *
     * @param otherValue The other value to multiply by this {@code DoubleDouble}.
     */
    public void multiply(final double otherValue) {
        multiply(otherValue, errorForWellKnownValue(otherValue));
    }

    /**
     * Multiplies this {@code DoubleDouble} by an other double-double value.
     * The result is stored in this instance.
     *
     * <div class="section">Implementation</div>
     * If <var>a</var> and <var>b</var> are {@code DoubleDouble} instances, then:
     *
     *   <blockquote>(a * b)</blockquote>
     *
     * can be computed as:
     *
     *   <blockquote>(a.value + a.error) * (b.value + b.error)<br>
     *             = (a.value * b.value) + (a.error * b.value) + (a.value * b.error) + (a.error * b.error)<br>
     *             ≅ (a.value * b.value) + (a.error * b.value) + (a.value * b.error)</blockquote>
     *
     * The first term is the main product. All other terms are added to the error, keeping in mind that the main
     * product has itself an error. The last term (the product of errors) is ignored because presumed very small.
     * In Java code:
     *
     * {@preformat java
     *   final double thisValue = this.value;
     *   final double thisError = this.error;
     *   setToProduct(thisValue, otherValue);
     *   error += otherError * thisValue;
     *   error += otherValue * thisError;
     *   setToQuickSum(value, error);
     * }
     *
     * @param otherValue The other value by which to multiply this {@code DoubleDouble}.
     * @param otherError The error of the other value by which to multiply this {@code DoubleDouble}.
     */
    public void multiply(final double otherValue, final double otherError) {
        final double thisValue = this.value;
        final double thisError = this.error;
        setToProduct(thisValue, otherValue);
        error += otherError * thisValue;
        error += otherValue * thisError;
        normalize();
    }

    /**
     * Multiplies this {@code DoubleDouble} by an other double-double value stored in the given array.
     * This is a convenience method for a frequently used operation, implemented as below:
     *
     * {@preformat java
     *    multiply(array[index], array[index + errorOffset]);
     * }
     *
     * @param array        The array from which to get the value and error.
     * @param index        Index of the value in the given array.
     * @param errorOffset  Offset to add to {@code index} in order to get the index of the error in the given array.
     */
    public void multiply(final double[] array, final int index, final int errorOffset) {
        multiply(array[index], array[index + errorOffset]);
    }

    /**
     * Divides this {@code DoubleDouble} by an other double-double value.
     * This is a convenience method for:
     *
     * {@preformat java
     *    divide(other.value, other.error);
     * }
     *
     * @param other The other value to by which to divide this value.
     */
    public void divide(final DoubleDouble other) {
        divide(other.value, other.error);
    }

    /**
     * Divides this {@code DoubleDouble} by a {@code Number}. If the given number is an instance
     * of {@code DoubleDouble}, then its error term will be taken in account.
     *
     * @param other The other value by which to divide this {@code DoubleDouble}.
     */
    public void divide(final Number other) {
        if (other instanceof DoubleDouble) {
            divide((DoubleDouble) other);
        } else {
            divide(other.doubleValue());
        }
    }

    /**
     * Divides this {@code DoubleDouble} by a {@code double} with a default error term.
     * This is a convenience method for:
     *
     * {@preformat java
     *    divide(otherValue, errorForWellKnownValue(otherValue));
     * }
     *
     * <b>Tip:</b> if the other value is known to be an integer or a power of 2, then invoking
     * <code>{@linkplain #divide(double, double) divide}(otherValue, 0)</code> is more efficient.
     *
     * @param otherValue The other value by which to divide this {@code DoubleDouble}.
     */
    public void divide(final double otherValue) {
        divide(otherValue, errorForWellKnownValue(otherValue));
    }

    /**
     * Divides this {@code DoubleDouble} by an other double-double value.
     * The result is stored in this instance.
     *
     * @param denominatorValue The other value by which to divide this {@code DoubleDouble}.
     * @param denominatorError The error of the other value by which to divide this {@code DoubleDouble}.
     */
    public void divide(final double denominatorValue, final double denominatorError) {
        if (DISABLED) {
            value /= denominatorValue;
            error  = 0;
            return;
        }
        final double numeratorValue = value;
        final double numeratorError = error;
        value = denominatorValue;
        error = denominatorError;
        inverseDivide(numeratorValue, numeratorError);
    }

    /**
     * Divides this {@code DoubleDouble} by an other double-double value stored in the given array.
     * This is a convenience method for a frequently used operation, implemented as below:
     *
     * {@preformat java
     *    divide(array[index], array[index + errorOffset]);
     * }
     *
     * @param array        The array from which to get the value and error.
     * @param index        Index of the value in the given array.
     * @param errorOffset  Offset to add to {@code index} in order to get the index of the error in the given array.
     */
    public void divide(final double[] array, final int index, final int errorOffset) {
        divide(array[index], array[index + errorOffset]);
    }

    /**
     * Divides the given double-double value by this {@code DoubleDouble}.
     * This is a convenience method for:
     *
     * {@preformat java
     *    inverseDivide(other.value, other.error);
     * }
     *
     * @param other The other value to divide by this value.
     */
    public void inverseDivide(final DoubleDouble other) {
        inverseDivide(other.value, other.error);
    }

    /**
     * Divides the given {@code Number} value by this {@code DoubleDouble}. If the given number
     * is an instance of {@code DoubleDouble}, then its error term will be taken in account.
     *
     * @param other The other value to divide by this {@code DoubleDouble}.
     */
    public void inverseDivide(final Number other) {
        if (other instanceof DoubleDouble) {
            inverseDivide((DoubleDouble) other);
        } else {
            inverseDivide(other.doubleValue());
        }
    }

    /**
     * Divides the given {@code double} value by this {@code DoubleDouble} with a default error term.
     * This is a convenience method for:
     *
     * {@preformat java
     *    inverseDivide(numeratorValue, errorForWellKnownValue(numeratorValue));
     * }
     *
     * <b>Tip:</b> if the other value is known to be an integer or a power of 2, then invoking
     * <code>{@linkplain #inverseDivide(double, double) inverseDivide}(otherValue, 0)</code> is more efficient.
     *
     * @param numeratorValue The other value to divide by this {@code DoubleDouble}.
     */
    public void inverseDivide(final double numeratorValue) {
        inverseDivide(numeratorValue, errorForWellKnownValue(numeratorValue));
    }

    /**
     * Divides the given double-double value by this {@code DoubleDouble}.
     * The result is stored in this instance.
     *
     * <div class="section">Implementation</div>
     * If <var>a</var> and <var>b</var> are {@code DoubleDouble} instances, then we estimate:
     *
     *   <blockquote>(a / b) = (a.value / b.value) + remainder / b</blockquote>
     *
     * where:
     *
     *   <blockquote>remainder = a - b * (a.value / b.value)</blockquote>
     *
     * @param numeratorValue The other value to divide by this {@code DoubleDouble}.
     * @param numeratorError The error of the other value to divide by this {@code DoubleDouble}.
     */
    public void inverseDivide(final double numeratorValue, final double numeratorError) {
        if (DISABLED) {
            value = numeratorValue / value;
            error = 0;
            return;
        }
        final double denominatorValue = value;
        /*
         * The 'b * (a.value / b.value)' part in the method javadoc.
         */
        final double quotient = numeratorValue / denominatorValue;
        multiply(quotient, 0);
        /*
         * Compute 'remainder' as 'a - above_product'.
         */
        final double productError = error;
        setToSum(numeratorValue, -value);
        error -= productError;  // Complete the above subtraction
        error += numeratorError;
        /*
         * Adds the 'remainder / b' term, using 'remainder / b.value' as an approximation
         * (otherwise we would have to invoke this method recursively). The approximation
         * is assumed okay since the second term is small compared to the first one.
         */
        setToQuickSum(quotient, (value + error) / denominatorValue);
    }

    /**
     * Divides the given double-double value by this {@code DoubleDouble}.
     * This is a convenience method for a frequently used operation, implemented as below:
     *
     * {@preformat java
     *    inverseDivide(array[index], array[index + errorOffset]);
     * }
     *
     * @param array        The array from which to get the value and error.
     * @param index        Index of the value in the given array.
     * @param errorOffset  Offset to add to {@code index} in order to get the index of the error in the given array.
     */
    public void inverseDivide(final double[] array, final int index, final int errorOffset) {
        inverseDivide(array[index], array[index + errorOffset]);
    }

    /**
     * Computes (1-x)/(1+x) where <var>x</var> is {@code this}.
     * This pattern occurs in map projections.
     */
    public void ratio_1m_1p() {
        final DoubleDouble numerator = new DoubleDouble(1, 0);
        numerator.subtract(this);
        add(1, 0);
        inverseDivide(numerator);
    }

    /**
     * Computes the square of this value.
     */
    public void square() {
        multiply(value, error);
    }

    /**
     * Sets this double-double value to its square root.
     *
     * <div class="section">Implementation</div>
     * This method searches for a {@code (r + ε)} value where:
     *
     * <blockquote>(r + ε)²  =  {@linkplain #value} + {@linkplain #error}</blockquote>
     *
     * If we could compute {@code r = sqrt(value + error)} with enough precision, then ε would be 0.
     * But with the {@code double} type, we can only estimate {@code r ≈ sqrt(value)}. However, since
     * that <var>r</var> value should be close to the "true" value, then ε should be small.
     *
     * <blockquote>value + error  =  (r + ε)²  =  r² + 2rε + ε²</blockquote>
     *
     * Neglecting ε² on the assumption that |ε| ≪ |r|:
     *
     * <blockquote>value + error  ≈  r² + 2rε</blockquote>
     *
     * Isolating ε:
     *
     * <blockquote>ε  ≈  (value + error - r²) / (2r)</blockquote>
     */
    public void sqrt() {
        if (value != 0) {
            final double thisValue = this.value;
            final double thisError = this.error;
            double r = Math.sqrt(thisValue);
            setToProduct(r, r);
            subtract(thisValue, thisError);
            divide(-2*r, 0);                    // Multiplication by 2 does not cause any precision lost.
            setToQuickSum(r, value);
        }
    }

    /**
     * Computes c₀ + c₁x + c₂x² + c₃x³ + c₄x⁴ + … where <var>x</var> is {@code this}.
     * The given <var>c</var> coefficients are presumed accurate in base 2
     * (i.e. this method does not try to apply a correction for base 10).
     *
     * @param coefficients The {@code c} coefficients. The array length must be at least 1.
     */
    public void series(final double... coefficients) {
        final DoubleDouble x = new DoubleDouble(this);
        value = coefficients[0];
        error = 0;
        final int last = coefficients.length - 1;
        if (last >= 1) {
            final DoubleDouble xn = new DoubleDouble(x);
            final DoubleDouble t = new DoubleDouble(xn);
            for (int i=1; i<last; i++) {
                t.multiply(coefficients[i], 0);
                add(t);
                xn.multiply(x);
                t.setFrom(xn);
            }
            t.multiply(coefficients[last], 0);
            add(t);
        }
    }

    /**
     * Returns a hash code value for this number.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        return Numerics.hashCode(Double.doubleToLongBits(value) ^ Double.doubleToLongBits(error));
    }

    /**
     * Compares this number with the given object for equality.
     *
     * @param  obj The other object to compare with this number.
     * @return {@code true} if both object are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DoubleDouble) {
            final DoubleDouble other = (DoubleDouble) obj;
            return Numerics.equals(value, other.value) &&
                   Numerics.equals(error, other.error);
        }
        return false;
    }



    /**
     * Returns a string representation of this number for debugging purpose.
     * The returned string does not need to contains all digits that this {@code DoubleDouble} can handle.
     *
     * @return A string representation of this number.
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
