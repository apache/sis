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
import static java.lang.Math.multiplyFull;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.pending.jdk.JDK15;
import static org.apache.sis.pending.jdk.JDK19.DOUBLE_PRECISION;


/**
 * A value class for rational numbers. {@code Fraction} objects are represented by a {@linkplain #numerator} and
 * a {@linkplain #denominator} stored at 32 bits integers. Fractions can be {@linkplain #simplify() simplified}.
 * All {@code Fraction} instances are immutable and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @version 1.6
 * @since   0.8
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
     * Returns the given fraction after simplification.
     * If the numerator or denominator is still too large for 32 bit integer after simplification,
     * then an {@link ArithmeticException} is thrown.
     *
     * @param  numerator    numerator of the fraction to return.
     * @param  denominator  denominator of the fraction to return.
     * @return the simplified fraction.
     * @throws ArithmeticException if the numerator and denominator cannot be represented by 32 bit integers.
     *
     * @since 1.4
     */
    public static Fraction valueOf(final long numerator, final long denominator) {
        return simplify(null, numerator, denominator);
    }

    /**
     * Converts the given IEEE 754 double-precision value to a fraction. If successful, this method returns a fraction
     * such as {@link #doubleValue()} is equal to the given value in the sense of {@link Double#equals(Object)}:
     * infinities, positive and negative zeros are preserved, but various NaN values are collapsed to a single NaN value.
     *
     * <p>This method accepts only values between {@value Integer#MIN_VALUE} and {@value Integer#MAX_VALUE} inclusive,
     * i.e. values in the range of 32-bits integers. If the given value has fraction digits, then the validity range
     * will be smaller depending on the {@linkplain #denominator} required for representing that value.</p>
     *
     * <h4>Design note</h4>
     * This method does not return approximated values because it is difficult to choose which fraction is best.
     * For example, choosing an approximated fraction for π value is quite arbitrary, and searching the fraction
     * closer than any other fraction representable by this class is computationally expensive.
     * Even with common fractions, the algorithm currently implemented in this class can detect that 1.6666666666666667
     * {@linkplain Double#equals(Object) is equal to} 5⁄3 but cannot detect easily that 1.66666666666666 (same number
     * with two decimal digits dropped) is close to 5⁄3.
     *
     * @param  value  the double-precision value to convert to a fraction.
     * @return a fraction such as {@link #doubleValue()} is equal to the given value.
     * @throws IllegalArgumentException if the given value cannot be converted to a fraction.
     *
     * @since 1.0
     */
    public static Fraction valueOf(final double value) {
        if (value == 0) {
            return new Fraction(0, MathFunctions.isNegativeZero(value) ? -1 : +1);
        }
        if (!Double.isFinite(value)) {
            return new Fraction(Double.isNaN(value) ? 0 : (value >= 0) ? 1 : -1, 0);
        }
        /*
         * If the value has fraction digits, converting that value into a fraction requires that we assume a base,
         * since the fraction denominator will be a multiple of that base. We will try base 2, 10, 3, 5, 7, 13, …,
         * in that order. Base 2 is tried first because it is fast, produces exact results (if successful) and its
         * implementation works with integer numbers too. Base 10 is tried next because it is likely to match user
         * input. Then prime numbers are tried until `doubleValue()` produce a result identical to `value`. If no
         * exact match is found, the best match will be taken.
         */
        long significand = Numerics.getSignificand(value);
        int  exponent    = Math.getExponent(value) - (DOUBLE_PRECISION - 1);                    // Power of 2.
        int  shift       = Long.numberOfTrailingZeros(significand);
        significand >>>= shift;
        exponent      += shift;
        if (exponent > -Integer.SIZE && exponent < Long.numberOfLeadingZeros(significand)) {
            /*
             * Build the fraction using arithmetic in base 2. This path is also executed for all integer values,
             * because they have exact representation in base 2. We do not need to simplify the fraction because
             * the denominator is always a power of 2 while the numerator is always odd; such fractions cannot
             * be simplified. Since we do not need to invoke `simplify(…)`, this is the fatest path.
             */
            final int den;
            if (exponent >= 0) {
                significand <<= exponent;
                den = 1;
            } else {
                den = 1 << -exponent;
            }
            if ((significand & ~Integer.MAX_VALUE) == 0) {
                if (value < 0) significand = -significand;
                return new Fraction((int) significand, den);
            }
        } else {
            /*
             * Cannot build the fraction using exact arithmetic in base 2. Try approximations using arithmetic in other bases,
             * starting with base 10. We will multiply the numerator and denominator by the largest power of 10 (or other base)
             * that can be used without causing an overflow, then simplify the fraction.
             */
            final double toMaximalSignificand = Numerics.SIGNIFICAND_MASK / Math.ceil(Math.abs(value));
            if (toMaximalSignificand > 1) {
                exponent = Numerics.toExp10(Math.getExponent(toMaximalSignificand));        // Power of 10.
                double factor = DecimalFunctions.pow10(exponent + 1);
                if (factor > toMaximalSignificand) {
                    factor = DecimalFunctions.pow10(exponent);
                }
                assert factor >= 1 && factor <= Numerics.SIGNIFICAND_MASK : factor;         // For use as denominator.
                try {
                    final Fraction f = simplify(null, Math.round(value * factor), Math.round(factor));
                    if (f.doubleValue() == value) return f;
                } catch (ArithmeticException e) {
                    // Ignore. We will try other bases below.
                }
                /*
                 * Arithmetic in base 10 failed too. Try prime numbers. This is the same approach
                 * than the one we used for base 10 above, but slower because using more costly math.
                 * The factor is:
                 *                      factor = Bⁿ
                 *
                 * where n is the greatest integer such as factor ≤ toMaximalSignificand.
                 */
                final double logMaxFactor = Math.log(toMaximalSignificand);
                for (int i=1; i<MathFunctions.PRIMES_LENGTH_16_BITS; i++) {
                    final int base = MathFunctions.primeNumberAt(i);
                    if (base > toMaximalSignificand) break;                             // Stop if exponent would be < 1.
                    exponent = (int) (logMaxFactor / Math.log(base));                   // Power of base.
                    final long den = MathFunctions.pow(base, exponent);
                    try {
                        final Fraction f = simplify(null, Math.round(value * den), den);
                        if (f.doubleValue() == value) return f;
                    } catch (ArithmeticException e) {
                        // Ignore. More tries in the loop.
                    }
                }
            }
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.CanNotConvertValue_2, value, Fraction.class));
    }

    /**
     * Returns a unique fraction instance equals to {@code this}.
     * If this method has been invoked previously on another {@code Fraction} with the same value as {@code this},
     * then that previous instance is returned (provided that it has not yet been garbage collected). Otherwise this
     * method adds this fraction to the pool of fractions that may be returned in next {@code unique()} invocations,
     * then returns {@code this}.
     *
     * <p>This method is useful for saving memory when a potentially large number of {@code Fraction} instances will
     * be kept for a long time and many instances are likely to have the same values.
     * It is usually not worth to invoke this method for short-lived instances.</p>
     *
     * @return a unique instance of a fraction equals to {@code this}.
     *
     * @deprecated {@code Fraction} may become a value object with a future Java version,
     *             and this method is incompatible with value objects.
     */
    @Deprecated(since="1.6", forRemoval=true)
    public Fraction unique() {
        return POOL.unique(this);
    }

    /**
     * Returns a fraction equivalent to {@code this} but represented by the smallest possible numerator
     * and denominator values. If this fraction cannot be simplified, then this method returns {@code this}.
     *
     * @return the simplest fraction equivalent to this fraction.
     */
    public Fraction simplify() {
        return simplify(this, numerator, denominator);
    }

    /**
     * Returns a fraction equivalent to {@code num} / {@code den} after simplification.
     * If the simplified fraction is equal to {@code f}, then this method returns {@code f}.
     *
     * <p>The arguments given to this method are the results of multiplications and additions of {@code int} values.
     * This method fails if any argument value is {@link Long#MIN_VALUE} because that value cannot be made positive.
     * However, it should never happen. Even in the worst scenario:</p>
     *
     * {@snippet lang="java" :
     *     long n = Math.multiplyFull(Integer.MIN_VALUE, Integer.MAX_VALUE);
     *     n += n;
     *     }
     *
     * Above result still slightly smaller in magnitude than {@code Long.MIN_VALUE}.
     */
    private static Fraction simplify(final Fraction f, long num, long den) {
        if (num == 0) {
            den = Long.signum(den);             // Simplify  0/x  as  0/±1 or 0/0.
        } else if (den == 0) {
            num = Long.signum(num);             // Simplify  x/0  as  ±1/0.
        } else if (den % num == 0) {
            den /= num;                         // Simplify  x/xy  as  1/y.
            if (den < 0) {
                den = -den;                     // Math.negateExact(long) not needed - see javadoc.
                num = -1;
            } else {
                num = 1;
            }
        } else {
            long a   = JDK15.absExact(num);
            long gcd = JDK15.absExact(den);
            long remainder = a % gcd;
            if (remainder == 0) {
                num /= den;                     // Simplify  xy/x  as  y/1.
                den = 1;
            } else {
                do {                            // Search for greatest common divisor with Euclid's algorithm.
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
        return (f != null && num == f.numerator && den == f.denominator) ? f
               : new Fraction(Math.toIntExact(num), Math.toIntExact(den));
    }

    /**
     * Returns the inverse value of this fraction.
     * This method does not simplify the fraction.
     *
     * @return the result of {@code 1/this}.
     * @throws ArithmeticException if the result overflows.
     *
     * @since 1.4
     *
     * @see #divide(Fraction)
     */
    public Fraction inverse() {
        if (numerator == denominator) return this;
        return new Fraction(denominator, numerator);
    }

    /**
     * Returns the negative value of this fraction.
     * This method does not simplify the fraction.
     *
     * @return the result of {@code -this}.
     * @throws ArithmeticException if the result overflows.
     *
     * @see #subtract(Fraction)
     */
    public Fraction negate() {
        int n = numerator;
        int d = denominator;
        if (n != 0) {
            n = Math.negateExact(n);
        } else if (d != 0) {
            d = Math.negateExact(d);
        } else {
            return this;
        }
        return new Fraction(n, d);
    }

    /**
     * Returns the simplified result of adding the given fraction to this fraction.
     *
     * @param  other  the fraction to add to this fraction.
     * @return the simplified result of {@code this} + {@code other}.
     * @throws ArithmeticException if the result overflows.
     */
    public Fraction add(final Fraction other) {
        long a = multiplyFull(other.denominator, numerator);
        long b = multiplyFull(denominator, other.numerator);
        return simplify(this, Math.addExact(a, b), multiplyFull(other.denominator, denominator));
    }

    /**
     * Returns the simplified result of subtracting the given fraction from this fraction.
     *
     * @param  other  the fraction to subtract from this fraction.
     * @return the simplified result of {@code this} - {@code other}.
     * @throws ArithmeticException if the result overflows.
     *
     * @see #negate()
     */
    public Fraction subtract(final Fraction other) {
        long a = multiplyFull(other.denominator, numerator);
        long b = multiplyFull(denominator, other.numerator);
        return simplify(this, Math.subtractExact(a, b), multiplyFull(other.denominator, denominator));
    }

    /**
     * Returns the simplified result of multiplying the given fraction with this fraction.
     *
     * @param  other  the fraction to multiply with this fraction.
     * @return the simplified result of {@code this} × {@code other}.
     * @throws ArithmeticException if the result overflows.
     */
    public Fraction multiply(final Fraction other) {
        return simplify(this, multiplyFull(numerator,   other.numerator),
                              multiplyFull(denominator, other.denominator));
    }

    /**
     * Returns the simplified result of dividing this fraction by the given fraction.
     *
     * @param  other  the fraction by which to divide this fraction.
     * @return the simplified result of {@code this} ∕ {@code other}.
     * @throws ArithmeticException if the result overflows.
     *
     * @see #inverse()
     */
    public Fraction divide(final Fraction other) {
        return simplify(this, multiplyFull(numerator,   other.denominator),
                              multiplyFull(denominator, other.numerator));
    }

    /**
     * Returns this fraction rounded toward nearest integer. If the result is located
     * at equal distance from the two nearest integers, then rounds to the even one.
     *
     * @return {@link #numerator} / {@link #denominator} rounded toward nearest integer.
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
     * @return {@link #numerator} / {@link #denominator} rounded toward negative infinity.
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
     * @return {@link #numerator} / {@link #denominator} rounded toward positive infinity.
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
     * If the fraction value {@linkplain #isNaN() is NaN}, then this method returns 0.
     * If the fraction value is positive or negative infinity, then this method returns
     * {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE} respectively.
     *
     * @return this fraction rounded toward zero.
     */
    @Override
    public long longValue() {
        if (denominator != 0) {
            return numerator / denominator;
        }
        if (numerator < 0) return Long.MIN_VALUE;
        if (numerator > 0) return Long.MAX_VALUE;
        return 0;
    }

    /**
     * Returns this fraction rounded toward zero.
     * If the fraction value {@linkplain #isNaN() is NaN}, then this method returns 0.
     * If the fraction value is positive or negative infinity, then this method returns
     * {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE} respectively.
     *
     * @return {@link #numerator} / {@link #denominator} rounded toward zero.
     *
     * @see #round()
     * @see #floor()
     * @see #ceil()
     */
    @Override
    public int intValue() {
        if (denominator != 0) {
            return numerator / denominator;
        }
        if (numerator < 0) return Integer.MIN_VALUE;
        if (numerator > 0) return Integer.MAX_VALUE;
        return 0;
    }

    /*
     * Do not override `shortValue()` and `byteValue()` in order to keep a behavior
     * consistent with all `Number` subclasses provided in the standard JDK: first
     * a narrowing conversion to `int` followed by discarding the high order bits.
     * Note than even a direct `(short) value` cast implicitly does above steps.
     */

    /**
     * Returns {@code true} if the numerator and denominator are both zero.
     *
     * @return whether this fraction is 0/0.
     *
     * @since 1.4
     */
    public boolean isNaN() {
        return (numerator | denominator) == 0;
    }

    /**
     * Returns the sign of this fraction. The return value is -1 if this fraction is negative;
     * 0 if the numerator is zero; and 1 if this fraction is positive.
     *
     * @return the sign of this fraction.
     *
     * @see Integer#signum(int)
     *
     * @since 1.0
     */
    public int signum() {
        if (numerator == 0) return 0;
        return ((numerator ^ denominator) >> (Integer.SIZE - 2)) | 1;
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
        return Long.signum(multiplyFull(numerator, other.denominator)
                         - multiplyFull(other.numerator, denominator));
    }

    /**
     * Compares this fraction with the given object for equality. This method returns {@code true} only if
     * the two objects are fractions with same {@linkplain #numerator} and {@linkplain #denominator} values.
     * Fractions with different values are not considered equal even if the two fraction are equivalent.
     *
     * @param  other  the object to compare with this fraction for equality.
     * @return {@code true} if the given object is another fraction with the same numerator and denominator values.
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
     * For example, the first row contains the symbol of all fractions of the form 0/x, the second row all fractions of
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

    /**
     * Creates a new fraction from the given text. This constructor is the converse of {@link #toString()} method.
     * It can parse single numbers like "3", fractions like "2/3", Unicode characters like "⅔" and infinity symbols
     * "∞" and "−∞". The given text shall not contain spaces.
     *
     * @param  s  the text to parse.
     * @throws NumberFormatException if the given text cannot be parsed.
     *
     * @since 1.0
     */
    public Fraction(final String s) throws NumberFormatException {
        ArgumentChecks.ensureNonEmpty("s", s);
        final int length = s.length();
        if (length == 1) {
            final char c = s.charAt(0);
            if (c >= 128) {
                for (int j=0; j<UNICODES.length; j++) {
                    final char[] unicodes = UNICODES[j];
                    for (int i=0; i<unicodes.length; i++) {
                        if (unicodes[i] == c) {
                            numerator   = j;
                            denominator = j + i + 1;
                            return;
                        }
                    }
                }
                if (c == '∞') {
                    numerator   = 1;
                    denominator = 0;
                    return;
                }
            }
        }
        if (s.equals("−∞") || s.equals("-∞")) {
            numerator   = -1;
            denominator =  0;
            return;
        }
        for (int i=0; i<length; i++) {
            switch (s.charAt(i)) {
                case '÷':
                case '⁄':
                case '/':
                case '∕': {
                    numerator   = Integer.parseInt(s, 0, i, 10);
                    denominator = Integer.parseInt(s, i+1, length, 10);
                    return;
                }
            }
        }
        numerator   = Integer.parseInt(s);
        denominator = 1;
    }
}
