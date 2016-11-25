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

import java.io.Serializable;
import org.apache.sis.util.collection.WeakHashSet;


/**
 * A value class for rational numbers. {@code Fraction} objects are represented by a {@linkplain #numerator} and
 * a {@linkplain #denominator} stored at 32 bits integers. Fractions can be {@linkplain #simplify() simplified}.
 * All {@code Fraction} instances are immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final class Fraction extends Number implements Comparable<Fraction>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4501644254763471216L;

    /**
     * Pool of fractions for which the {@link #unique()} method has been invoked.
     */
    private static final WeakHashSet<Fraction> POOL = new WeakHashSet<>(Fraction.class);

    /**
     * The <var>a</var> term in the <var>a</var>/<var>b</var> fraction.
     * Can be positive, negative or zero.
     *
     * @see #doubleValue()
     */
    public final int numerator;

    /**
     * The <var>b</var> term in the <var>a</var>/<var>b</var> fraction.
     * Can be positive, negative or zero. If zero, then the fraction {@linkplain #doubleValue() floating point}
     * value will be positive infinity, negative infinity or NaN depending on the {@linkplain #numerator} value.
     *
     * @see #doubleValue()
     */
    public final int denominator;

    /**
     * Creates a new fraction. This constructor stores the fraction exactly as specified; it does not simplify it.
     * The fraction can be simplified after construction by a call to {@link #simplify()}.
     *
     * @param numerator    the <var>a</var> term in the <var>a</var>/<var>b</var> fraction.
     * @param denominator  the <var>b</var> term in the <var>a</var>/<var>b</var> fraction.
     */
    public Fraction(final int numerator, final int denominator) {
        this.numerator   = numerator;
        this.denominator = denominator;
    }

    /**
     * Returns a unique fraction instance equals to {@code this}.
     * If this method has been invoked previously on another {@code Fraction} with the same value than {@code this},
     * then that previous instance is returned (provided that it has not yet been garbage collected). Otherwise this
     * method adds this fraction to the pool of fractions that may be returned in next {@code unique()} invocations,
     * then returns {@code this}.
     *
     * <p>This method is useful for saving memory when a potentially large amount of {@code Fraction} instances will
     * be kept for a long time and many instances are likely to have the same values.
     * It is usually not worth to invoke this method for short-lived instances.</p>
     *
     * @return a unique instance of a fraction equals to {@code this}.
     */
    public Fraction unique() {
        return POOL.unique(this);
    }

    /**
     * Returns a fraction equivalent to {@code this} but represented by the smallest possible numerator
     * and denominator values. If this fraction can not be simplified, then this method returns {@code this}.
     *
     * @return the simplest fraction equivalent to this fraction.
     */
    public Fraction simplify() {
        return simplify(numerator, denominator);
    }

    /**
     * Returns a fraction equivalent to {@code num} / {@code den} after simplification.
     * If the simplified fraction is equals to {@code this}, then this method returns {@code this}.
     *
     * <p>The arguments given to this method are the results of multiplications and additions of {@code int} values.
     * This method fails if any argument value is {@link Long#MIN_VALUE} because that value can not be made positive.
     * However it should never happen. Even in the worst scenario:</p>
     *
     * {@prefomat java
     *     long n = Integer.MIN_VALUE * (long) Integer.MAX_VALUE;
     *     n += n;
     * }
     *
     * Above result still slightly smaller in magnitude than {@code Long.MIN_VALUE}.
     */
    private Fraction simplify(long num, long den) {
        if (num == Long.MIN_VALUE || den == Long.MIN_VALUE) {
            throw new ArithmeticException();
        }
        if (num == 0) {
            den = Long.signum(den);             // Simplify  0/x  as  0/±1 or 0/0.
        } else if (den == 0) {
            num = Long.signum(num);             // Simplify  x/0  as  ±1/0
        } else if (den % num == 0) {
            den /= num;                         // Simplify  x/xy  as  1/y
            if (den < 0) {
                den = -den;                     // Math.negateExact(long) not needed - see javadoc.
                num = -1;
            } else {
                num = 1;
            }
        } else {
            long a   = Math.abs(num);
            long gcd = Math.abs(den);
            long remainder = a % gcd;
            if (remainder == 0) {
                num /= den;                     // Simplify  xy/x  as  y/1
                den = 1;
            } else {
                do {                            // Search for greater common divisor with Euclid's algorithm.
                    a   = gcd;
                    gcd = remainder;
                    remainder = a % gcd;
                } while (remainder != 0);
                num /= gcd;
                den /= gcd;
                if (den < 0) {
                    num = -num;                 // Math.negateExact(long) not needed - see javadoc.
                    den = -den;
                }
            }
        }
        return (num == numerator && den == denominator) ? this
               : new Fraction(Math.toIntExact(num), Math.toIntExact(den));
    }

    /**
     * Returns the negative value of this fraction.
     * This method does not simplify the fraction.
     *
     * @return the result of {@code -this}.
     * @throws ArithmeticException if the result overflows.
     */
    public Fraction negate() {
        return (numerator == 0) ? this : new Fraction(Math.negateExact(numerator), denominator);
    }

    /**
     * Returns the simplified result of adding the given fraction to this fraction.
     *
     * @param  other  the fraction to add to this fraction.
     * @return the simplified result of {@code this} + {@code other}.
     * @throws ArithmeticException if the result overflows.
     */
    public Fraction add(final Fraction other) {
        // Intermediate result must be computed in a type wider that the 'numerator' and 'denominator' type.
        final long td = this .denominator;
        final long od = other.denominator;
        return simplify(Math.addExact(od * numerator, td * other.numerator), od * td);
    }

    /**
     * Returns the simplified result of subtracting the given fraction from this fraction.
     *
     * @param  other  the fraction to subtract from this fraction.
     * @return the simplified result of {@code this} - {@code other}.
     * @throws ArithmeticException if the result overflows.
     */
    public Fraction subtract(final Fraction other) {
        // Intermediate result must be computed in a type wider that the 'numerator' and 'denominator' type.
        final long td = this .denominator;
        final long od = other.denominator;
        return simplify(Math.subtractExact(od * numerator, td * other.numerator), od * td);
    }

    /**
     * Returns the simplified result of multiplying the given fraction with this fraction.
     *
     * @param  other  the fraction to multiply with this fraction.
     * @return the simplified result of {@code this} × {@code other}.
     * @throws ArithmeticException if the result overflows.
     */
    public Fraction multiply(final Fraction other) {
        return simplify(numerator   * (long) other.numerator,
                        denominator * (long) other.denominator);
    }

    /**
     * Returns the simplified result of dividing this fraction by the given fraction.
     *
     * @param  other  the fraction by which to divide this fraction.
     * @return the simplified result of {@code this} ∕ {@code other}.
     * @throws ArithmeticException if the result overflows.
     */
    public Fraction divide(final Fraction other) {
        return simplify(numerator   * (long) other.denominator,
                        denominator * (long) other.numerator);
    }

    /**
     * Returns this fraction rounded toward nearest integer. If the result is located
     * at equal distance from the two nearest integers, then rounds to the even one.
     *
     * @return {@link #numerator} / {@link denominator} rounded toward nearest integer.
     */
    public int round() {
        if (denominator == Integer.MIN_VALUE) {
            if (numerator < (Integer.MIN_VALUE / +2)) return +1;
            if (numerator > (Integer.MIN_VALUE / -2)) return -1;
            return 0;
        }
        int n = numerator / denominator;
        int r = numerator % denominator;
        if (r != 0) {
            r = Math.abs(r << 1);
            final int d = Math.abs(denominator);
            if (r > d || (r == d && (n & 1) != 0)) {
                if ((numerator ^ denominator) >= 0) {
                    n++;
                } else {
                    n--;
                }
            }
        }
        return n;
    }

    /**
     * Returns this fraction rounded toward negative infinity.
     * This is different from the default operation on primitive types, which rounds toward zero.
     *
     * <p><b>Tip:</b> if the numerator and the denominator are both positive or both negative,
     * then the result is positive and identical to {@code numerator / denominator}.</p>
     *
     * @return {@link #numerator} / {@link denominator} rounded toward negative infinity.
     */
    public int floor() {
        int n = numerator / denominator;
        if ((numerator ^ denominator) < 0 && (numerator % denominator) != 0) {
            n--;
        }
        return n;
    }

    /**
     * Returns this fraction rounded toward positive infinity.
     * This is different from the default operation on primitive types, which rounds toward zero.
     *
     * @return {@link #numerator} / {@link denominator} rounded toward positive infinity.
     */
    public int ceil() {
        int n = numerator / denominator;
        if ((numerator ^ denominator) >= 0 && (numerator % denominator) != 0) {
            n++;
        }
        return n;
    }

    /**
     * Returns the fraction as a double-precision floating point number.
     * Special cases:
     *
     * <ul>
     *   <li>If the {@linkplain #numerator} and the {@linkplain #denominator} are both 0,
     *       then this method returns {@link Double#NaN}.</li>
     *   <li>If only the {@linkplain #denominator} is zero, then this method returns
     *       {@linkplain Double#POSITIVE_INFINITY positive infinity} or
     *       {@linkplain Double#NEGATIVE_INFINITY negative infinity} accordingly the {@linkplain #numerator} sign.</li>
     * </ul>
     *
     * @return this fraction as a floating point number.
     */
    @Override
    public double doubleValue() {
        return numerator / (double) denominator;
    }

    /**
     * Returns the fraction as a single-precision floating point number.
     * Special cases:
     *
     * <ul>
     *   <li>If the {@linkplain #numerator} and the {@linkplain #denominator} are both 0,
     *       then this method returns {@link Float#NaN}.</li>
     *   <li>If only the {@linkplain #denominator} is zero, then this method returns
     *       {@linkplain Float#POSITIVE_INFINITY positive infinity} or
     *       {@linkplain Float#NEGATIVE_INFINITY negative infinity} accordingly the {@linkplain #numerator} sign.</li>
     * </ul>
     *
     * @return this fraction as a floating point number.
     */
    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    /**
     * Returns this fraction rounded toward zero.
     *
     * @return this fraction rounded toward zero.
     */
    @Override
    public long longValue() {
        return intValue();
    }

    /**
     * Returns this fraction rounded toward zero.
     *
     * @return {@link #numerator} / {@link denominator} rounded toward zero.
     *
     * @see #round()
     * @see #floor()
     * @see #ceil()
     */
    @Override
    public int intValue() {
        return numerator / denominator;
    }

    /**
     * Returns this fraction rounded toward zero, if the result can be represented as a short integer.
     *
     * @return this fraction rounded toward zero.
     * @throws ArithmeticException if the result can not be represented as a short integer.
     */
    @Override
    public short shortValue() {
        final int n = intValue();
        if ((n & ~0xFFFF) == 0) return (short) n;
        throw new ArithmeticException();
    }

    /**
     * Returns this fraction rounded toward zero, if the result can be represented as a signed byte.
     *
     * @return this fraction rounded toward zero.
     * @throws ArithmeticException if the result can not be represented as a signed byte.
     */
    @Override
    public byte byteValue() {
        final int n = intValue();
        if ((n & ~0xFF) == 0) return (byte) n;
        throw new ArithmeticException();
    }

    /**
     * Compares this fraction with the given one for order.
     *
     * @param  other  the fraction to compare to this fraction for ordering.
     * @return a negative number if this fraction is smaller than the given fraction,
     *         a positive number if greater, or 0 if equals.
     */
    @Override
    public int compareTo(final Fraction other) {
        return Long.signum(numerator * (long) other.denominator - other.numerator * (long) denominator);
    }

    /**
     * Compares this fraction with the given object for equality. This method returns {@code true} only if
     * the two objects are fractions with same {@linkplain #numerator} and {@linkplain #denominator} values.
     * Fractions with different values are not considered equal even if the two fraction are equivalent.
     *
     * @param  other  the object to compare with this fraction for equality.
     * @return {@code true} if the given object is an other fraction with the same numerator and denominator values.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Fraction) {
            final Fraction that = (Fraction) other;
            return numerator   == that.numerator &&
                   denominator == that.denominator;
        }
        return false;
    }

    /**
     * Returns a hash code value for this fraction.
     */
    @Override
    public int hashCode() {
        return (numerator + 31 * denominator) ^ (int) serialVersionUID;
    }

    /**
     * The matrix of Unicode symbols available for some fractions. Each row contains all symbols for the same numerator.
     * For example the first row contains the symbol of all fractions of the form 0/x, the second row all fractions of
     * the form 1/x, <i>etc.</i>. In each row, the character at column <var>i</var> is for the fraction having the
     * denominator i + (row index) + 1.
     */
    private static final char[][] UNICODES = {
        {0,  0,  '↉'},
        {   '½', '⅓', '¼', '⅕', '⅙', '⅐', '⅛', '⅑', '⅒'},
        {        '⅔',  0,  '⅖'},
        {             '¾', '⅗',  0,   0,  '⅜'},
        {                  '⅘'},
        {                       '⅚',  0,  '⅝'},
        {},
        {                                 '⅞'}
    };

    /**
     * Returns a string representation of this fraction.
     * This method returns Unicode symbol if possible.
     *
     * @return a string representation of this fraction.
     */
    @Override
    public String toString() {
        switch (denominator) {
            case 0: {
                if (numerator != 0) {
                    return (numerator >= 0) ? "∞" : "−∞";
                }
                break;
            }
            case 1: {
                return String.valueOf(numerator);
            }
            default: {
                if (numerator >= 0 && numerator < UNICODES.length) {
                    final int d = denominator - numerator - 1;
                    if (d >= 0) {
                        final char[] r = UNICODES[numerator];
                        if (d < r.length) {
                            final char c = r[d];
                            if (c != 0) {
                                return String.valueOf(c);
                            }
                        }
                    }
                }
                break;
            }
        }
        return new StringBuilder().append(numerator).append('⁄').append(denominator).toString();
    }
}
