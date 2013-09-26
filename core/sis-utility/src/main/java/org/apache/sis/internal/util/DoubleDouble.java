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
// No BigDecimal dependency - see class javadoc


/**
 * Basic arithmetic methods for extended precision numbers using the <cite>double-double</cite> algorithm.
 * This class implements some of the methods published in the following paper:
 *
 * <ul>
 *   <li>Yozo Hida, Xiaoye S. Li, David H. Bailey.
 *       <a href="http://web.mit.edu/tabbott/Public/quaddouble-debian/qd-2.3.4-old/docs/qd.pdf">Library
 *       for Double-Double and Quad-Double arithmetic</a>, 2007.</li>
 *   <li>Jonathan R. Shewchuk. Adaptive precision floating-point arithmetic and fast robust geometric predicates.
 *       Discrete & Computational Geometry, 18(3):305–363, 1997.</li>
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
 * {@section Impact of availability of FMA instructions}
 * If <cite>fused multiply-add</cite> (FMA) instruction are available in a future Java version
 * (see <a href="https://issues.apache.org/jira/browse/SIS-136">SIS-136</a> on Apache SIS JIRA),
 * then the following methods should be revisited:
 *
 * <ul>
 *   <li>{@link #setToProduct(double, double)} - revisit with [Hida & al.] algorithm 7.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see <a href="http://en.wikipedia.org/wiki/Double-double_%28arithmetic%29#Double-double_arithmetic">Double-double arithmetic</a>
 */
public final class DoubleDouble extends Number {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7602414219228638550L;

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
     * <p>Source: [Hida & al.] page 4 algorithm 5, itself reproduced from [Shewchuk] page 325.</p>
     */
    private static final double SPLIT = (1 << 27) + 1;

    /**
     * Maximal value that can be handled by {@link #multiply(double, double)}.
     * If a multiplication is using a value greater than {@code MAX_VALUE},
     * then the result will be infinity or NaN.
     */
    public static final double MAX_VALUE = Double.MAX_VALUE / SPLIT;

    /**
     * Pre-defined constants frequently used in SIS. SIS often creates matrices for unit conversions,
     * and most conversion factors are defined precisely in base 10. For example the conversion from
     * feet to metre is defined by a factor of exactly 0.3048, which can not be represented precisely
     * as a {@code double}. Consequently if a value of 0.3048 is given, we can assume that the intend
     * was to provide the "feet to metres" conversion factor and complete the double-double instance
     * accordingly.
     *
     * <p>Elements in this array shall be sorted in strictly increasing order.
     * For any value at index <var>i</var>, the associated error is {@code ERRORS[i]}.
     */
    private static final double[] VALUES = {
        0.000001,
        0.00001,
        0.0001,
        0.001,
        0.01,
        0.1,
        0.3048,     // Feet to metres
        0.9         // Degrees to gradians
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
        /* 0.000001  */  4.525188817411374E-23,
        /* 0.00001   */ -8.180305391403131E-22,
        /* 0.0001    */ -4.79217360238593E-21,
        /* 0.001     */ -2.0816681711721686E-20,
        /* 0.01      */ -2.0816681711721684E-19,
        /* 0.1       */ -5.551115123125783E-18,
        /* 0.3048    */ -1.5365486660812166E-17,
        /* 0.9       */ -2.2204460492503132E-17
    };

    /**
     * The main value, minus the {@link #error}.
     */
    public double value;

    /**
     * The error that shall be added to the main {@link #value} in order to get the
     * "<cite>real</cite>" (actually "<cite>the most accurate that we can</cite>") value.
     */
    public double error;

    /**
     * Creates a new value initialized to zero.
     */
    public DoubleDouble() {
    }

    /** Returns {@link #value}. */
    @Override public double doubleValue() {return value;}
    @Override public float  floatValue()  {return (float) value;}
    @Override public long   longValue()   {return Math.round(value);}
    @Override public int    intValue()    {return (int) longValue();}

    /**
     * If the given value is one of the well known constants, returns the error for that value.
     * Otherwise returns 0.
     *
     * @param  value The value for which to get this error.
     * @return The error for the given value, or 0 if unknown. In the later case,
     *         the given value is assumed to be the most accurate available representation.
     */
    public static double errorForWellKnownValue(final double value) {
        final int i = Arrays.binarySearch(VALUES, Math.abs(value));
        return (i >= 0) ? MathFunctions.xorSign(ERRORS[i], value) : 0;
    }

    /**
     * Equivalent to a call to {@code setToQuickSum(value, error)} inlined.
     * This is invoked after addition or multiplication operations.
     */
    final void normalize() {
        error += (value - (value += error));
    }

    /**
     * Sets this {@code DoubleDouble} to the sum of the given numbers,
     * to be used only when {@code abs(a) >= abs(b)}.
     *
     * <p>Source: [Hida & al.] page 4 algorithm 3, itself reproduced from [Shewchuk] page 312.</p>
     *
     * @param a The first number to add.
     * @param b The second number to add, which must be smaller than {@code a}.
     */
    public void setToQuickSum(final double a, final double b) {
        value = a + b;
        error = b - (value - a);
    }

    /**
     * Sets this {@code DoubleDouble} to the sum of the given numbers.
     *
     * <p>Source: [Hida & al.] page 4 algorithm 4, itself reproduced from [Shewchuk] page 314.</p>
     *
     * @param a The first number to add.
     * @param b The second number to add.
     */
    public void setToSum(final double a, final double b) {
        value = a + b;
        final double v = value - a;
        error = (a - (value - v)) + (b - v);
    }

    /**
     * Sets this {@code DoubleDouble} to the product of the given numbers.
     * The given numbers shall not be greater than {@value #MAX_VALUE} in magnitude.
     *
     * <p>Source: [Hida & al.] page 4 algorithm 6, itself reproduced from [Shewchuk] page 326.</p>
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
    }

    /**
     * Adds an other double-double value to this {@code DoubleDouble}.
     * This is a convenience method for:
     *
     * {@preformat java
     *    add(other.value, other.error);
     * }
     *
     * @param other The other value to add to this value.
     */
    public void add(final DoubleDouble other) {
        add(other.value, other.error);
    }

    /**
     * Adds an other double-double value to this {@code DoubleDouble}.
     * The result is stored in this instance.
     *
     * {@section Implementation}
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
        normalize();
    }

    /**
     * Multiplies this {@code DoubleDouble} by an other double-double value.
     * This is a convenience method for:
     *
     * {@preformat java
     *    multiply(other.value, other.error);
     * }
     *
     * @param other The other value to add to this value.
     */
    public void multiply(final DoubleDouble other) {
        multiply(other.value, other.error);
    }

    /**
     * Multiplies this {@code DoubleDouble} by an other double-double value.
     * The result is stored in this instance.
     *
     * {@section Implementation}
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
     * Divides this {@code DoubleDouble} by an other double-double value.
     * This is a convenience method for:
     *
     * {@preformat java
     *    divide(other.value, other.error);
     * }
     *
     * @param other The other value to add to this value.
     */
    public void divide(final DoubleDouble other) {
        divide(other.value, other.error);
    }

    /**
     * Divides this {@code DoubleDouble} by an other double-double value.
     * The result is stored in this instance.
     *
     * @param denominatorValue The other value by which to divide this {@code DoubleDouble}.
     * @param denominatorError The error of the other value by which to divide this {@code DoubleDouble}.
     */
    public void divide(final double denominatorValue, final double denominatorError) {
        final double numeratorValue = value;
        final double numeratorError = error;
        value = denominatorValue;
        error = denominatorError;
        inverseDivide(numeratorValue, numeratorError);
    }

    /**
     * Divides the given double-double value by this {@code DoubleDouble}.
     * The result is stored in this instance.
     *
     * {@section Implementation}
     * If <var>a</var> and <var>b</var> are {@code DoubleDouble} instances, then we estimate:
     *
     *   <blockquote>(a / b) = (a.value / b.value) + remaining / b</blockquote>
     *
     * where:
     *
     *   <blockquote>remaining = a - b * (a.value / b.value)</blockquote>
     *
     * @param numeratorValue The other value to divide by this {@code DoubleDouble}.
     * @param numeratorError The error of the other value to divide by this {@code DoubleDouble}.
     */
    public void inverseDivide(final double numeratorValue, final double numeratorError) {
        final double denominatorValue = value;
        /*
         * The 'b * (a.value / b.value)' part in the method javadoc.
         */
        final double quotient = numeratorValue / denominatorValue;
        multiply(quotient, 0);
        /*
         * Compute 'remaining' as 'a - above_product'.
         */
        final double productError = error;
        setToSum(numeratorValue, -value);
        error -= productError;  // Complete the above subtraction
        error += numeratorError;
        /*
         * Adds the 'remaining / b' term, using 'remaining / b.value' as an approximation
         * (otherwise we would have to invoke this method recursively). The approximation
         * is assumed okay since the second term is small compared to the first one.
         */
        setToQuickSum(quotient, (value + error) / denominatorValue);
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
