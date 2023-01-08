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
package org.apache.sis.internal.referencing;

import java.util.function.BinaryOperator;
import org.apache.sis.internal.util.DoubleDouble;


/**
 * Apply arithmetic operations between number of arbitrary types.
 * Null numbers are interpreted as zero.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 * @since   1.4
 */
public enum Arithmetic {
    /**
     * The addition operator.
     */
    ADD(DoubleDouble::add),

    /**
     * The subtraction operator.
     */
    SUBTRACT(DoubleDouble::subtract),

    /**
     * The multiplication operator.
     */
    MULTIPLY(DoubleDouble::multiply),

    /**
     * The division operator.
     */
    DIVIDE(DoubleDouble::divide),

    /**
     * The inverse operation. Operand <var>b</var> is ignored and can be null.
     */
    INVERSE((a,b) -> a.inverse()),

    /**
     * The negation operation. Operand <var>b</var> is ignored and can be null.
     */
    NEGATE((a,b) -> a.negate()),

    /**
     * The square root operation. Operand <var>b</var> is ignored and can be null.
     */
    SQRT((a,b) -> a.sqrt());

    /**
     * Whether to assume that {@code float} and {@code double} values
     * were intended to be exact in base 10.
     */
    public static final boolean DECIMAL = true;

    /**
     * The arithmetic operation applied with double-double arithmetic.
     */
    private final BinaryOperator<DoubleDouble> onDoubleDouble;

    /**
     * Creates a new arithmetic operator.
     */
    private Arithmetic(final BinaryOperator<DoubleDouble> onDoubleDouble) {
        this.onDoubleDouble = onDoubleDouble;
    }

    /**
     * Applies the operation on the given number.
     *
     * @todo Current implementation handles only {@link DoubleDouble} values,
     *       but more types will be added later.
     */
    private Number apply(final Number a, final Number b) {
        final DoubleDouble result = onDoubleDouble.apply(
                DoubleDouble.of(a, DECIMAL),
                DoubleDouble.of(b, DECIMAL));

        if (result.isZero())  return null;
        if (result.equals(a)) return a;
        if (result.equals(b)) return b;
        return result;
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
     * (they cannot appear in the value returned by {@link #getElement(int, int)}, and are not shown
     * by {@link #toString()}) - returning {@code false} while the matrix clearly looks like identity
     * would be confusing for the user. Furthermore, the errors can be non-zero only on the diagonal,
     * and those values are always smaller than 2.3E-16.
     *
     * <p>Another argument is that the extended precision is for reducing rounding errors during
     * matrix arithmetic. But since the user provided the original data as {@code double} values,
     * the extra precision may have no real meaning.</p>
     *
     * @param  element  the value to test (can be {@code null}).
     * @return whether the given element is equal to one.
     *
     * @see org.apache.sis.internal.referencing.ExtendedPrecisionMatrix#isZero(Number)
     */
    public static boolean isOne(final Number element) {
        return (element != null) && element.doubleValue() == 1;
    }
}
