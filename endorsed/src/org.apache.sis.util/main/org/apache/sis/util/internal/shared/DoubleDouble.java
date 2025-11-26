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
package org.apache.sis.util.internal.shared;

import java.util.Arrays;
import java.time.Duration;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;
import org.apache.sis.math.Fraction;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.system.Configuration;
import org.apache.sis.util.Debug;
import static org.apache.sis.pending.jdk.JDK19.DOUBLE_PRECISION;


/**
 * Basic arithmetic methods for extended precision numbers using the <i>double-double</i> algorithm.
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
 * {@snippet lang="java" :
 *     BigDecimal decimal = new BigDecimal(dd.value).add(new BigDecimal(dd.error));
 *     }
 *
 * {@code DoubleDouble} is a <em>value object</em>, immutable and thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://en.wikipedia.org/wiki/Double-double_%28arithmetic%29#Double-double_arithmetic">Wikipedia: Double-double arithmetic</a>
 */
public final class DoubleDouble extends Number implements Comparable<DoubleDouble> {
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
     * <p>Because this flag is static final, all expressions of the form {@code if (DISABLED)} should be
     * omitted by the compiler from the class files in normal operations.</p>
     *
     * <p>Setting this flag to {@code true} causes some JUnit tests to fail, which is expected. The main
     * purpose of this flag is to allow {@code org.apache.sis.referencing.operation.matrix.MatrixTestCase}
     * to perform strict comparisons of matrix operation results with JAMA, which is taken as the reference
     * implementation. Since JAMA uses {@code double} arithmetic, SIS needs to disable double-double
     * arithmetic if the results are to be compared for strict equality.</p>
     */
    @Debug
    public static final boolean DISABLED = false;

    /**
     * A margin in number of bits for determining if an error term should be considered as zero.
     * When computing <var>a</var> - <var>b</var> as a double-double (106 significand bits) value,
     * if the number of non-zero significand bits is equal or lower than {@code ZERO_THRESHOLD+1},
     * consider the result as zero.
     */
    @Configuration
    private static final int ZERO_THRESHOLD = 2;

    /**
     * Predefined constants frequently used in SIS, sorted in increasing order. This table contains only
     * constants that cannot be inferred by {@link DecimalFunctions#deltaForDoubleToDecimal(double)},
     * for example some transcendental values.
     *
     * <p>Elements in this array shall be sorted in strictly increasing order.
     * For any value at index <var>i</var>, the associated error is {@code ERRORS[i]}.
     *
     * @see #errorForWellKnownValue(double)
     */
    private static final double[] VALUES = {
        /*
         * Some of the following constants have more fraction digits than necessary. We declare the extra
         * digits for documentation purpose, and in order to have identical content than DoubleDoubleTest
         * so that a plain copy-and-paste can be performed between those two classes.
         */
         0.000004848136811095359935899141023579480, // Arc-second to radians
         0.0002777777777777777777777777777777778,   // Second to degrees
         0.002777777777777777777777777777777778,    // 1/360°
         0.01666666666666666666666666666666667,     // Minute to degrees
         0.01745329251994329576923690768488613,     // Degree to radians
         0.785398163397448309615660845819876,       // π/4
         1.111111111111111111111111111111111,       // Grad to degrees
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
     * A {@code DoubleDouble} instance for the 0 value.
     *
     * @see #isZero()
     */
    public static final DoubleDouble ZERO = new DoubleDouble(0, 0);

    /**
     * A {@code DoubleDouble} instance for the 1 value.
     */
    public static final DoubleDouble ONE = new DoubleDouble(1, 0);

    /**
     * A {@code DoubleDouble} instance for the π value.
     */
    public static final DoubleDouble PI =
            new DoubleDouble(3.14159265358979323846264338327950, 1.2246467991473532E-16);

    /**
     * A {@code DoubleDouble} instance for the conversion factor from radians to angular degrees.
     */
    public static final DoubleDouble RADIANS_TO_DEGREES =
            new DoubleDouble(57.2957795130823208767981548141052, -1.9878495670576283E-15);

    /**
     * A {@code DoubleDouble} instance for the conversion factor from angular degrees to radians.
     */
    public static final DoubleDouble DEGREES_TO_RADIANS =
            new DoubleDouble(0.01745329251994329576923690768488613, 2.9486522708701687E-19);

    /**
     * A {@code DoubleDouble} instance for the conversion factor from arc-seconds to radians.
     */
    public static final DoubleDouble SECONDS_TO_RADIANS =
            new DoubleDouble(0.000004848136811095359935899141023579480, 9.320078015422868E-23);

    /**
     * A {@code DoubleDouble} instance for the NaN value.
     *
     * @see #isNaN()
     */
    public static final DoubleDouble NaN = new DoubleDouble(Double.NaN, Double.NaN);

    /**
     * The main value, minus the {@link #error}.
     */
    public final double value;

    /**
     * The error that shall be added to the main {@link #value} in order to get the
     * <q>real</q> (actually <q>the most accurate that we can</q>) value.
     */
    public final double error;

    /**
     * Creates a new instance for the given value and error.
     * It is caller's responsibility to ensure that the (value, error) pair is normalized.
     *
     * @param  value  the value.
     * @param  error  the error.
     */
    private DoubleDouble(final double value, final double error) {
        this.value = value;
        this.error = DISABLED ? 0 : error;
        assert !(Math.abs(error) >= Math.ulp(value)) : this;            // Use ! for being tolerant to NaN.
    }

    /**
     * Returns an instance for the given number. If the given number is an instance of
     * {@code DoubleDouble}, {@link BigDecimal}, {@link BigInteger} or {@link Fraction},
     * then the error term will be taken in account.
     *
     * @param  value    the value, or {@code null}.
     * @param  decimal  whether {@code float} and {@code double} values were intended to be exact in base 10.
     * @return the value as a double-double number, or {@code null} if the given number was null.
     */
    public static DoubleDouble of(Number value, final boolean decimal) {
        if (value == null) {
            return null;
        }
        if (value instanceof DoubleDouble) {
            return (DoubleDouble) value;
        }
        if (value instanceof Fraction) {
            final Fraction f = (Fraction) value;
            return new DoubleDouble(f.numerator, 0)
                            .divide(f.denominator);
        }
        final double v, error;
        if (value instanceof Float) {
            final float f = (Float) value;
            value = decimal ? DecimalFunctions.floatToDouble(f) : f;
        } else if (value instanceof BigInteger) {
            value = new BigDecimal((BigInteger) value, MathContext.DECIMAL128);
        }
        v = value.doubleValue();
        if (value instanceof Integer) {
            error = 0;
        } else if (value instanceof Long) {
            error = value.longValue() - (long) v;       // Need rounding toward zero.
        } else if (value instanceof BigDecimal) {
            // Really need new BigDecimal(value) below, not BigDecimal.valueOf(value).
            error = ((BigDecimal) value).subtract(new BigDecimal(v), MathContext.DECIMAL64).doubleValue();
        } else {
            error = decimal ? errorForWellKnownValue(v) : 0;
        }
        return new DoubleDouble(v, error);
    }

    /**
     * Returns an instance for the given duration in nanoseconds.
     *
     * @param  value  the duration to convert.
     * @return the given duration, in nanoseconds.
     */
    public static DoubleDouble of(final Duration value) {
        return of(value.getSeconds()).multiply(Constants.NANOS_PER_SECOND).add(value.getNano());
    }

    /**
     * Returns an instance for the given integer.
     *
     * @param  value  the integer value to wrap in a {@code DoubleDouble}.
     * @return the value as a double-double number.
     */
    public static DoubleDouble of(final int value) {
        return new DoubleDouble(value, 0);
    }

    /**
     * Returns an instance for the given long integer.
     *
     * @param  value  the long integer value to wrap in a {@code DoubleDouble}.
     * @return the value as a double-double number.
     */
    public static DoubleDouble of(final long value) {
        final double f = value;
        return new DoubleDouble(f, value - (long) f);       // Rounding toward zero.
    }

    /**
     * Returns an instance for the given value.
     * If {@code decimal} is {@code true}, then an error term is inferred for well-known values.
     * {@code decimal} should be {@code false} when the value has been computed using transcendental functions
     * (cosine, logarithm, <i>etc.</i>), in which case there is no way we can infer a meaningful error term.
     * Should also be {@code false} (for performance reason) when the value is an exact representation in base 2.
     *
     * @param  value    the value.
     * @param  decimal  whether the value was intended to be exact in base 10.
     * @return the value as a double-double number.
     */
    public static DoubleDouble of(final double value, final boolean decimal) {
        return new DoubleDouble(value, decimal ? errorForWellKnownValue(value) : 0);
    }

    /**
     * Returns an instance for the given value and error.
     * It is caller's responsibility to ensure the {@code error} term is less than 1 ULP of {@code value}.
     *
     * @param  value  the value.
     * @param  error  the error.
     * @return the value as a double-double number.
     */
    public static DoubleDouble of(final double value, final double error) {
        return new DoubleDouble(value, error);
    }

    /**
     * Return value + error. The result should be identical to {@link #value},
     * but we nevertheless do the sum as a safety.
     *
     * @return {@link #value} + {@link #error}.
     */
    @Override public double doubleValue() {return value + error;}
    @Override public float  floatValue()  {return (float) doubleValue();}
    @Override public int    intValue()    {return Numerics.clamp(longValue());}
    @Override public long   longValue()   {return Numerics.saturatingAdd(Math.round(value), (long) error);}
    /*
     * Do not override `shortValue()` and `byteValue()` in order to keep a behavior
     * consistent with all `Number` subclasses provided in the standard JDK: first
     * a narrowing conversion to `int` followed by discarding the high order bits.
     * Note than even a direct `(short) value` cast implicitly does above steps.
     */

    /**
     * Suggests an {@link #error} for the given value. The {@code DoubleDouble} class contains a hard-coded list
     * of some frequently used constants, for example for various factors of π. If the given value matches exactly
     * one of those constants, then its error term is returned. Otherwise this method assumes that the given value
     * is defined in base 10 (e.g. many unit conversion factors) and tries to compute an error term with
     * {@link DecimalFunctions#deltaForDoubleToDecimal(double)}.
     *
     * <h4>Rational</h4>
     * SIS often creates matrices for unit conversions, and most conversion factors are defined precisely in base 10.
     * For example, the conversion from feet to metres is defined by a factor of exactly 0.3048, which cannot be
     * represented precisely as a {@code double}. Consequently, if a value of 0.3048 is given, we can assume that
     * the intent was to provide the "feet to metres" conversion factor and complete the double-double instance
     * accordingly.
     *
     * @param  value  the value for which to get this error.
     * @return the error for the given value, or 0 if unknown. In the latter case,
     *         the base 2 representation of the given value is assumed to be accurate enough.
     */
    static double errorForWellKnownValue(final double value) {
        if (DISABLED) return 0;
        final int i = Arrays.binarySearch(VALUES, Math.abs(value));
        final double error;
        if (i >= 0) {
            error = MathFunctions.xorSign(ERRORS[i], value);
        } else {
            final double delta = DecimalFunctions.deltaForDoubleToDecimal(value);
            error = Double.isNaN(delta) ? 0 : delta;
        }
        assert !(Math.abs(error) >= Math.ulp(value)) : value;       // Use ! for being tolerant to NaN.
        return error;
    }

    /**
     * Returns {@code true} if this {@code DoubleDouble} is equal to zero.
     *
     * @return {@code true} if this {@code DoubleDouble} is equal to zero.
     *
     * @see #ZERO
     */
    public boolean isZero() {
        return value == 0 && error == 0;
    }

    /**
     * Returns {@code true} if this {@code DoubleDouble} is not a number.
     *
     * @return {@code true} if this {@code DoubleDouble} is not a number.
     *
     * @see #NaN
     */
    public boolean isNaN() {
        return Double.isNaN(value) || Double.isNaN(error);
    }

    /**
     * Returns the sum of the given numbers,
     * to be used only when {@code abs(a) >= abs(b)}.
     *
     * <p>Source: [Hida &amp; al.] page 4 algorithm 3, itself reproduced from [Shewchuk] page 312.</p>
     *
     * @param  a  the first number to add.
     * @param  b  the second number to add, which must be smaller than {@code a}.
     * @return sum of the given numbers.
     */
    static DoubleDouble quickSum(final double a, final double b) {
        final double value = a + b;
        return new DoubleDouble(value, b - (value - a));
    }

    /**
     * Returns the sum of the given numbers.
     * The double-double accuracy is useful when the largest value is exact in base 2.
     * A typical example is: 1 - (small value).
     *
     * <p>Source: [Hida &amp; al.] page 4 algorithm 4, itself reproduced from [Shewchuk] page 314.</p>
     *
     * @param  a  the first number to add.
     * @param  b  the second number to add.
     * @return sum of the given numbers.
     */
    public static DoubleDouble sum(final double a, final double b) {
        final double value = a + b;
        final double v = value - a;
        return new DoubleDouble(value, (a - (value - v)) + (b - v));
    }

    /**
     * Returns the product of the given numbers.
     * Note that unless the given arguments are exact in base 2,
     * the result is not more accurate than a {@code double}.
     *
     * <p>Source: [Hida &amp; al.] page 5 algorithm 7, itself reproduced from [Shewchuk] page 326.
     * This is the algorithm used when FMA instruction is available. For an algorithm without FMA,
     * see [Hida &amp; al.] page 4 algorithm 6 (it is more complicated).</p>
     *
     * @param  a  the first number to multiply.
     * @param  b  the second number to multiply.
     * @return product of the given numbers.
     */
    public static DoubleDouble product(final double a, final double b) {
        final double value = a * b;
        return new DoubleDouble(value, Math.fma(a, b, -value));     // Really needs the `fma(…)` precision here.
    }

    /**
     * Returns {@code 1/this}.
     *
     * @return a new number which is the inverse of this number.
     */
    public DoubleDouble inverse() {
        return ONE.divide(this);
    }

    /**
     * Returns {@code -this}.
     *
     * @return a new number which is the negative of this number.
     */
    public DoubleDouble negate() {
        return new DoubleDouble(-value, -error);
    }

    /**
     * Adds another double-double value to this {@code DoubleDouble}.
     *
     * @param  other  the other value to add to this {@code DoubleDouble}.
     * @return the sum of {@code this} with the given number.
     */
    public DoubleDouble add(final DoubleDouble other) {
        return add(other.value, other.error);
    }

    /**
     * Adds a {@code Number} value to this {@code DoubleDouble}. If the given number is an instance
     * of {@code DoubleDouble} or {@link Fraction}, then the error term will be taken in account.
     *
     * @param  other    the other value to add to this {@code DoubleDouble}.
     * @param  decimal  whether {@code float} and {@code double} values were intended to be exact in base 10.
     * @return the sum of {@code this} with the given number.
     */
    public DoubleDouble add(final Number other, final boolean decimal) {
        return add(of(other, decimal));
    }

    /**
     * Adds a {@code int} value to this {@code DoubleDouble}.
     *
     * @param  other  the other value to add to this {@code DoubleDouble}.
     * @return the sum of {@code this} with the given number.
     */
    public DoubleDouble add(final int other) {
        return add(other, 0);
    }

    /**
     * Adds a {@code long} value to this {@code DoubleDouble}.
     *
     * @param  other  the other value to add to this {@code DoubleDouble}.
     * @return the sum of {@code this} with the given number.
     */
    public DoubleDouble add(final long other) {
        return add(of(other));
    }

    /**
     * Adds a {@code double} value to this {@code DoubleDouble}.
     * If {@code decimal} is {@code true}, then an error term is inferred for well-known values.
     *
     * @param  other    the other value to add to this {@code DoubleDouble}.
     * @param  decimal  whether the value was intended to be exact in base 10.
     * @return the sum of {@code this} with the given number.
     */
    public DoubleDouble add(final double other, final boolean decimal) {
        return add(other, decimal ? errorForWellKnownValue(other) : 0);
    }

    /**
     * Adds another double-double value to this {@code DoubleDouble}.
     *
     * <h4>Implementation</h4>
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
     * {@snippet lang="java" :
     *   final double thisError = this.error;
     *   setToSum(value, otherValue);
     *   error += thisError;
     *   error += otherError;
     *   setToQuickSum(value, error);
     *   }
     *
     * @param  otherValue  the other value to add to this {@code DoubleDouble}.
     * @param  otherError  the error of the other value to add to this {@code DoubleDouble}.
     * @return the sum of {@code this} with the given number.
     */
    private DoubleDouble add(final double otherValue, final double otherError) {
        double s = value + otherValue;
        double v = s - value;
        double e = (value - (s - v)) + (otherValue - v) + (error + otherError);
        if (s == 0 && e != 0) {
            /*
             * The two values almost cancelled, only their error terms are different.
             * The number of significand bits (mantissa) in the IEEE `double` representation is 52,
             * not counting the hidden bit. So estimate the accuracy of the double-double number as
             * the accuracy of the `double` value (which is 1 ULP) scaled as if we had 52 additional
             * significand bits (we ignore some more bits if ZERO_THRESHOLD is greater than 0).
             * If the error is not greater than that value, then assume that it is not significant.
             */
            if (Math.abs(e) <= Math.scalb(Math.ulp(otherValue), ZERO_THRESHOLD - (DOUBLE_PRECISION - 1))) {
                return new DoubleDouble(s, 0);
            }
        }
        return quickSum(s, e);
    }

    /**
     * Subtracts another double-double value from this {@code DoubleDouble}.
     *
     * @param  other  the other value to subtract from this value.
     * @return the difference between {@code this} and the given number.
     */
    public DoubleDouble subtract(final DoubleDouble other) {
        return add(-other.value, -other.error);
    }

    /**
     * Subtracts a {@code Number} from this {@code DoubleDouble}. If the given number is an instance
     * of {@code DoubleDouble} or {@link Fraction}, then the error term will be taken in account.
     *
     * @param  other    the other value to subtract from this {@code DoubleDouble}.
     * @param  decimal  whether {@code float} and {@code double} values were intended to be exact in base 10.
     * @return the difference between {@code this} and the given number.
     */
    public DoubleDouble subtract(final Number other, final boolean decimal) {
        return subtract(of(other, decimal));
    }

    /**
     * Subtracts an {@code int} from this {@code DoubleDouble}.
     *
     * @param  other  the other value to subtract from this {@code DoubleDouble}.
     * @return the difference between {@code this} and the given number.
     */
    public DoubleDouble subtract(final int other) {
        return add(-((double) other), 0);
    }

    /**
     * Subtracts a {@code long} from this {@code DoubleDouble}.
     *
     * @param  other  the other value to subtract from this {@code DoubleDouble}.
     * @return the difference between {@code this} and the given number.
     */
    public DoubleDouble subtract(final long other) {
        return subtract(of(other));
    }

    /**
     * Subtracts a {@code double} from this {@code DoubleDouble} with a default error term.
     * If {@code decimal} is {@code true}, then an error term is inferred for well-known values.
     *
     * @param  other    the other value to subtract from this {@code DoubleDouble}.
     * @param  decimal  whether the value was intended to be exact in base 10.
     * @return the difference between {@code this} and the given number.
     */
    public DoubleDouble subtract(double other, final boolean decimal) {
        other = -other;
        return add(other, decimal ? errorForWellKnownValue(other) : 0);
    }

    /**
     * Multiplies this {@code DoubleDouble} by another double-double value.
     *
     * @param  other  the other value to multiply by this value.
     * @return the product of {@code this} with the given number.
     */
    public DoubleDouble multiply(final DoubleDouble other) {
        return multiply(other.value, other.error);
    }

    /**
     * Multiplies this {@code DoubleDouble} by a {@code Number}. If the given number is an instance
     * of {@code DoubleDouble} or {@link Fraction}, then the error term will be taken in account.
     *
     * @param  other    the other value to multiply by this {@code DoubleDouble}.
     * @param  decimal  whether {@code float} and {@code double} values were intended to be exact in base 10.
     * @return the product of {@code this} with the given number.
     */
    public DoubleDouble multiply(final Number other, final boolean decimal) {
        return multiply(of(other, decimal));
    }

    /**
     * Multiplies this {@code DoubleDouble} by an {@code int}.
     *
     * @param  other  the other value to multiply by this {@code DoubleDouble}.
     * @return the product of {@code this} with the given number.
     */
    public DoubleDouble multiply(final int other) {
        return multiply(other, 0);
    }

    /**
     * Multiplies this {@code DoubleDouble} by a {@code long}.
     *
     * @param  other  the other value to multiply by this {@code DoubleDouble}.
     * @return the product of {@code this} with the given number.
     */
    public DoubleDouble multiply(final long other) {
        return multiply(of(other));
    }

    /**
     * Multiplies this {@code DoubleDouble} by a {@code double} with a default error term.
     * If {@code decimal} is {@code true}, then an error term is inferred for well-known values.
     *
     * @param  other    the other value to multiply by this {@code DoubleDouble}.
     * @param  decimal  whether the value was intended to be exact in base 10.
     * @return the product of {@code this} with the given number.
     */
    public DoubleDouble multiply(final double other, final boolean decimal) {
        return multiply(other, decimal ? errorForWellKnownValue(other) : 0);
    }

    /**
     * Multiplies this {@code DoubleDouble} by another double-double value.
     *
     * <h4>Implementation</h4>
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
     *
     * @param  otherValue  the other value by which to multiply this {@code DoubleDouble}.
     * @param  otherError  the error of the other value by which to multiply this {@code DoubleDouble}.
     * @return the product of {@code this} with the given number.
     */
    private DoubleDouble multiply(final double otherValue, final double otherError) {
        double v = value * otherValue, e;
        e = Math.fma(value, otherValue, -v);    // Really needs the `fma(…)` precision here.
        e = Math.fma(otherError, value, e);
        e = Math.fma(otherValue, error, e);
        return quickSum(v, e);
    }

    /**
     * Divides this {@code DoubleDouble} by another double-double value.
     *
     * @param  other  the other value to by which to divide this value.
     * @return the ratio between {@code this} and the given number.
     */
    public DoubleDouble divide(final DoubleDouble other) {
        return divide(other.value, other.error);
    }

    /**
     * Divides this {@code DoubleDouble} by a {@code Number}. If the given number is an instance
     * of {@code DoubleDouble} or {@link Fraction}, then the error term will be taken in account.
     *
     * @param  other    the other value by which to divide this {@code DoubleDouble}.
     * @param  decimal  whether {@code float} and {@code double} values were intended to be exact in base 10.
     * @return the ratio between {@code this} and the given number.
     */
    public DoubleDouble divide(final Number other, final boolean decimal) {
        return divide(of(other, decimal));
    }

    /**
     * Divides this {@code DoubleDouble} by an {@code int}.
     *
     * @param  other  the other value by which to divide this {@code DoubleDouble}.
     * @return the ratio between {@code this} and the given number.
     */
    public DoubleDouble divide(final int other) {
        return divide(other, 0);
    }

    /**
     * Divides this {@code DoubleDouble} by a {@code long}.
     *
     * @param  other  the other value by which to divide this {@code DoubleDouble}.
     * @return the ratio between {@code this} and the given number.
     */
    public DoubleDouble divide(final long other) {
        return divide(of(other));
    }

    /**
     * Divides this {@code DoubleDouble} by a {@code double} with a default error term.
     * If {@code decimal} is {@code true}, then an error term is inferred for well-known values.
     *
     * @param  other    the other value by which to divide this {@code DoubleDouble}.
     * @param  decimal  whether the value was intended to be exact in base 10.
     * @return the ratio between {@code this} and the given number.
     */
    public DoubleDouble divide(final double other, final boolean decimal) {
        return divide(other, decimal ? errorForWellKnownValue(other) : 0);
    }

    /**
     * Divides this {@code DoubleDouble} by another double-double value.
     *
     * <h4>Implementation</h4>
     * If <var>a</var> and <var>b</var> are {@code DoubleDouble} instances, then we estimate:
     *
     *   <blockquote>(a / b) = (a.value / b.value) + remainder / b</blockquote>
     *
     * where:
     *
     *   <blockquote>remainder = a - b * (a.value / b.value)</blockquote>
     *
     * @param  otherValue  the other value by which to divide this {@code DoubleDouble}.
     * @param  otherError  the error of the other value by which to divide this {@code DoubleDouble}.
     * @return the ratio between {@code this} and the given number.
     */
    private DoubleDouble divide(final double otherValue, final double otherError) {
        if (DISABLED) {
            return new DoubleDouble(value / otherValue, 0);
        }
        /*
         * The `b * (a.value / b.value)` part in the method javadoc.
         * The result should be `a` ± some error to be determined.
         */
        double pv, pe, s;
        final double quotient = value / otherValue;
        pe  = Math.fma(quotient, otherValue, -value);   // Really needs the `fma(…)` precision here.
        pe  = Math.fma(quotient, otherError, pe);
        pv  = value + pe;                               // `quickSum(value, r)` inlined on 3 lines.
        s   = value - pv;
        pe += s;
        /*
         * Compute the remainder as `a - above_product` where the product is the (pv + pe) pair.
         * Code below is a call to `sum(-pv, -pe)` inlined and without final `quickSum(…)` call.
         */
        final double v = s - value;
        double e = (value - (s - v)) - (pv + v) + (error - pe);
        /*
         * Adds the `remainder / b` term, using `remainder / b.value` as an approximation
         * (otherwise we would have to invoke this method recursively). The approximation
         * is assumed okay because the second term is small compared to the first one.
         */
        return quickSum(quotient, (s + e) / otherValue);
    }

    /**
     * Computes (1-x)/(1+x) where <var>x</var> is {@code this}.
     * This pattern occurs in map projections.
     *
     * @return (1-x)/(1+x).
     */
    public DoubleDouble ratio_1m_1p() {
        return ONE.subtract(this).divide(add(1));
    }

    /**
     * Returns {@code this} × 2ⁿ. Typical usages are
     * {@code scalb(1)} for an efficient multiplication by 2 and
     * {@code scalb(-1)} for an efficient division by 2.
     *
     * @param  n  power of 2 used to scale {@code this}.
     * @return {@code this} × 2ⁿ.
     */
    public DoubleDouble scalb(final int n) {
        return new DoubleDouble(Math.scalb(value, n), Math.scalb(error, n));
    }

    /**
     * Computes the square of this value.
     *
     * @return {@code this * this}.
     */
    public DoubleDouble square() {
        return multiply(value, error);
    }

    /**
     * Sets this double-double value to its square root.
     *
     * <h4>Implementation</h4>
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
     *
     * @return the square root of this value.
     */
    public DoubleDouble sqrt() {
        if (value == 0) {
            return ZERO;
        }
        double r = Math.sqrt(value);
        DoubleDouble t = product(r, r);
        t = t.subtract(this);
        t = t.divide(-2*r, 0);                       // Multiplication by 2 does not cause any precision lost.
        return quickSum(r, t.value);
    }

    /**
     * Computes c₀ + c₁x + c₂x² + c₃x³ + c₄x⁴ + … where <var>x</var> is {@code this}.
     * The given <var>c</var> coefficients are presumed accurate in base 2
     * (i.e. this method does not try to apply a correction for base 10).
     *
     * @param  coefficients the {@code c} coefficients. The array length must be at least 1.
     * @return the series sum.
     */
    public DoubleDouble series(final double... coefficients) {
        DoubleDouble sum = new DoubleDouble(coefficients[0], 0);
        DoubleDouble xn  = this;
        for (int i=1; i < coefficients.length; i++) {
            sum = sum.add(xn.multiply(coefficients[i], 0));
            xn  = multiply(xn);
        }
        return sum;
    }

    /**
     * Compares this value with the given value for order.
     *
     * @param   other  the value to be compared.
     */
    @Override
    public int compareTo(final DoubleDouble other) {
        int c = Double.compare(value, other.value);
        if (c == 0) {
            c = Double.compare(error, other.error);
        }
        return c;
    }

    /**
     * Compares this number with the given object for equality.
     *
     * @param  obj  the other object to compare with this number.
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
     * Returns a hash code value for this number.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(Double.doubleToLongBits(value) ^ Double.doubleToLongBits(error));
    }

    /**
     * Returns a string representation of this number.
     * This method prints only {@link #value} digits because the {@link #error} term is not always significant.
     * For example SIS sometime uses {@code DoubleDouble} in calculations involving trigonometric operations
     * where operands have only {@code double} precision. Because users may see {@code DoubleDouble} values
     * returned by {@code MatrixSIS}, we want to avoid misleading them with non-realistic precision.
     *
     * @return a string representation of this number.
     */
    @Override
    public String toString() {
        return Double.toString(doubleValue());
    }
}
