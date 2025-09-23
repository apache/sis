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
package org.apache.sis.referencing.internal;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import org.opengis.referencing.operation.Matrix;        // For javadoc.
import org.apache.sis.system.Configuration;
import org.apache.sis.util.internal.shared.DoubleDouble;
import org.apache.sis.math.Fraction;
import org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix;


/**
 * Apply arithmetic operations between number of arbitrary types.
 * Null numbers are interpreted as zero.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public enum Arithmetic {
    /**
     * The addition operator.
     */
    ADD(DoubleDouble::add, Fraction::add, Math::addExact),

    /**
     * The subtraction operator.
     */
    SUBTRACT(DoubleDouble::subtract, Fraction::subtract, Math::subtractExact),

    /**
     * The multiplication operator.
     */
    MULTIPLY(DoubleDouble::multiply, Fraction::multiply, Math::multiplyExact),

    /**
     * The division operator.
     */
    DIVIDE(DoubleDouble::divide, Fraction::divide, Fraction::valueOf),

    /**
     * The inverse operation. Operand <var>b</var> is ignored and can be null.
     */
    INVERSE((a,b) -> a.inverse(),
            (a,b) -> a.inverse(),
            (a,b) -> new Fraction(1, Math.toIntExact(a))),

    /**
     * The negation operation. Operand <var>b</var> is ignored and can be null.
     */
    NEGATE((a,b) -> a.negate(),
           (a,b) -> a.negate(),
           (a,b) -> Math.negateExact(a)),

    /**
     * The square root operation. Operand <var>b</var> is ignored and can be null.
     */
    SQRT((a,b) -> a.sqrt(),
         (a,b) -> DoubleDouble.of(a, false).sqrt(),
         (a,b) -> DoubleDouble.of(a).sqrt());

    /**
     * Whether to assume that {@code float} and {@code double} values
     * were intended to be exact in base 10.
     */
    @Configuration
    public static final boolean DECIMAL = true;

    /**
     * The arithmetic operation applied with double-double arithmetic.
     */
    private final BinaryOperator<DoubleDouble> onDoubleDouble;

    /**
     * The arithmetic operation applied on fractions.
     * The operation may throw {@link ArithmeticException},
     * in which case {@link #onDoubleDouble} will be used as fallback.
     */
    private final BiFunction<Fraction,Fraction,Number> onFraction;

    /**
     * The arithmetic operation applied on long integers.
     * The operation may throw {@link ArithmeticException},
     * in which case {@link #onDoubleDouble} will be used as fallback.
     */
    private final BiFunction<Long,Long,Number> onLong;

    /**
     * Creates a new arithmetic operator.
     */
    private Arithmetic(final BinaryOperator<DoubleDouble> onDoubleDouble,
                       final BiFunction<Fraction,Fraction,Number> onFraction,
                       final BiFunction<Long,Long,Number> onLong)
    {
        this.onDoubleDouble = onDoubleDouble;
        this.onFraction     = onFraction;
        this.onLong         = onLong;
    }

    /**
     * Returns the value of the given number as a long integer if possible.
     * If the conversion is not exact, then this method returns {@code null}.
     *
     * @param  element  the value to return as a long integer, or {@code null} if zero.
     * @return the value as a long integer, or {@code null} if it cannot be converted.
     */
    private static Long tryLongValue(final Number element) {
        if (element == null || element instanceof Long) {
            return (Long) element;
        }
        final long a = element.longValue();
        return (a == element.doubleValue()) ? a : null;
    }

    /**
     * Applies the operation on the given number.
     *
     * @param  a  the first operand. Shall not be null.
     * @param  b  the second operation. May be null if it does not apply.
     */
    private Number apply(final Number a, final Number b) {
        Number result = null;
        try {
            final Long aLong = tryLongValue(a);
            final Long bLong = tryLongValue(b);
            if (aLong != null && (bLong != null || b == null)) {
                result = onLong.apply(aLong, bLong);
            } else {
                Fraction aFrac = (a instanceof Fraction) ? (Fraction) a : null;
                Fraction bFrac = (b instanceof Fraction) ? (Fraction) b : null;
                if (aFrac != null || bFrac != null) {
                    if (aFrac == null && aLong != null) aFrac = new Fraction(Math.toIntExact(aLong), 1);
                    if (bFrac == null && bLong != null) bFrac = new Fraction(Math.toIntExact(bLong), 1);
                    if (aFrac != null && (bFrac != null || b == null)) {
                        result = onFraction.apply(aFrac, bFrac);
                    }
                }
            }
        } catch (ArithmeticException e) {
            // Ignore and fallback on double-double precision.
        }
        if (result == null) {
            result = onDoubleDouble.apply(DoubleDouble.of(a, DECIMAL),
                                          DoubleDouble.of(b, DECIMAL));
        }
        if (ExtendedPrecisionMatrix.isZero(result)) return null;
        if (result.equals(a)) return a;
        if (result.equals(b)) return b;
        final Number simplified = tryLongValue(result);
        return (simplified != null) ? simplified : result;
    }

    /**
     * Returns the sum of the given numbers.
     *
     * @param  a  the first number,  or {@code null} for zero.
     * @param  b  the second number, or {@code null} for zero.
     * @return the addition result,  or {@code null} for zero.
     */
    public static Number add(final Number a, final Number b) {
        if (a == null) return b;
        if (b == null) return a;
        return ADD.apply(a, b);
    }

    /**
     * Returns the difference between the given numbers.
     *
     * @param  a  the first number, or {@code null} for zero.
     * @param  b  the second number, or {@code null} for zero.
     * @return the subtraction result, or {@code null} for zero.
     */
    public static Number subtract(final Number a, final Number b) {
        if (b == null) return a;
        if (a == null) return NEGATE.apply(b, null);
        return SUBTRACT.apply(a, b);
    }

    /**
     * Returns the product of the given numbers. If any argument is null (zero),
     * then this method returns {@code null} regardless the value of the other argument.
     * In particular, 0 × NaN = 0 instead of NaN (contrarily to standard floating point).
     * This is intentional and a strong requirement for supporting matrix multiplication
     * and inversion where some dimensions have unknown (NaN) scale factor.
     *
     * @param  a  the first number,  or {@code null} for zero.
     * @param  b  the second number, or {@code null} for zero.
     * @return the multiplication result,  or {@code null} for zero.
     */
    public static Number multiply(final Number a, final Number b) {
        if (a == null || b == null) return null;
        if (isOne(a)) return b;     // Avoid changing the number type.
        if (isOne(b)) return a;
        return MULTIPLY.apply(a, b);
    }

    /**
     * Returns the quotient of the given numbers.
     * If the numerator is null (zero) and the denominator is non-null, then this method returns null (zero).
     * In particular, 0 / NaN = 0 instead of NaN. See {@link #multiply(Number, Number)}
     *
     * @param  a  the first number,  or {@code null} for zero.
     * @param  b  the second number, or {@code null} for zero.
     * @return the division result,  or {@code null} for zero.
     */
    public static Number divide(final Number a, final Number b) {
        if (b != null) {
            if (a == null || isOne(b)) return a;
            if (isOne(a)) return INVERSE.apply(b, null);    // Avoid changing the type.
        }
        return DIVIDE.apply(a, b);
    }

    /**
     * Returns the inverse of the given number.
     *
     * @param  a  the number, or {@code null} for zero.
     * @return the inverse result, or {@code null} for zero.
     */
    public static Number inverse(final Number a) {
        if (a == null) return Double.POSITIVE_INFINITY;
        if (isOne(a))  return a;
        return INVERSE.apply(a, null);
    }

    /**
     * Returns the negative of the given number.
     *
     * @param  a  the number, or {@code null} for zero.
     * @return the result, or {@code null} for zero.
     */
    public static Number negate(final Number a) {
        if (a == null) return null;
        return NEGATE.apply(a, null);
    }

    /**
     * Returns the square of the given number.
     *
     * @param  a  the number, or {@code null} for zero.
     * @return the square result, or {@code null} for zero.
     */
    public static Number square(final Number a) {
        if (a == null || isOne(a)) return a;
        return MULTIPLY.apply(a, a);
    }

    /**
     * Returns the square root of the given number.
     *
     * @param  a  the number, or {@code null} for zero.
     * @return the square root result, or {@code null} for zero.
     */
    public static Number sqrt(final Number a) {
        if (a == null || isOne(a)) return a;
        return SQRT.apply(a, null);
    }

    /**
     * Returns {@code true} if the given number is one, ignoring {@code DoubleDouble} error term.
     * This method does not check the error terms because those terms are not visible to the user
     * (they cannot appear in the value returned by {@link Matrix#getElement(int, int)},
     * and are not shown by {@link #toString()}) — returning {@code false} while the matrix clearly
     * looks like identity would be confusing for the user. Furthermore, the errors can be non-zero
     * only on the diagonal, and those values are always smaller than 2.3E-16.
     *
     * <p>Another argument is that the extended precision is for reducing rounding errors during
     * matrix arithmetic. But since the user provided the original data as {@code double} values,
     * the extra precision may have no real meaning.</p>
     *
     * @param  element  the value to test (can be {@code null}).
     * @return whether the given element is equal to one.
     *
     * @see org.apache.sis.referencing.internal.shared.ExtendedPrecisionMatrix#isZero(Number)
     */
    public static boolean isOne(final Number element) {
        return (element != null) && element.doubleValue() == 1;
    }
}
