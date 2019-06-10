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
package org.apache.sis.filter;

import java.util.Arrays;
import java.util.Collection;
import java.math.BigInteger;
import java.math.BigDecimal;
import org.apache.sis.util.Numbers;
import org.apache.sis.math.Fraction;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.ArgumentChecks;

// Branch-dependent imports
import org.opengis.filter.expression.Expression;


/**
 * Base class for expressions, comparators or filters performing operations on two expressions.
 * The nature of the operation depends on the subclass. If operands are numerical values, they
 * may be converted to a common type before the operation is performed. That operation is not
 * necessarily an arithmetic operation; it may be a comparison for example.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class BinaryFunction extends Node {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8632475810190545852L;

    /**
     * The first of the two expressions to be used by this function.
     *
     * @see #getExpression1()
     */
    protected final Expression expression1;

    /**
     * The second of the two expressions to be used by this function.
     *
     * @see #getExpression2()
     */
    protected final Expression expression2;

    /**
     * Creates a new binary function.
     *
     * @param  expression1  the first of the two expressions to be used by this function.
     * @param  expression2  the second of the two expressions to be used by this function.
     */
    protected BinaryFunction(final Expression expression1, final Expression expression2) {
        ArgumentChecks.ensureNonNull("expression1", expression1);
        ArgumentChecks.ensureNonNull("expression2", expression2);
        this.expression1 = expression1;
        this.expression2 = expression2;
    }

    /**
     * Returns the first of the two expressions to be used by this function.
     * This is the value specified at construction time.
     */
    public final Expression getExpression1() {
        return expression1;
    }

    /**
     * Returns the second of the two expressions to be used by this function.
     * This is the value specified at construction time.
     */
    public final Expression getExpression2() {
        return expression2;
    }

    /**
     * Returns the two expressions in a list of size 2.
     * This is used for {@link #toString()} implementation.
     */
    @Override
    protected final Collection<?> getChildren() {
        return Arrays.asList(expression1, expression2);
    }

    /**
     * Evaluates the expression for producing a result of numeric type.
     * This method delegates to one of the {@code applyAs(…)} methods.
     *
     * @param  left   the left operand. Can not be null.
     * @param  right  the right operand. Can not be null.
     * @return result of this function applied on the two given operands.
     * @throws ArithmeticException if the operation overflows the capacity of the type used.
     */
    protected final Number apply(final Number left, final Number right) {
        switch (Math.max(Numbers.getEnumConstant(left.getClass()),
                         Numbers.getEnumConstant(right.getClass())))
        {
            case Numbers.BIG_DECIMAL: {
                return applyAsDecimal(Numbers.cast(left,  BigDecimal.class),
                                      Numbers.cast(right, BigDecimal.class));
            }
            case Numbers.BIG_INTEGER: {
                return applyAsInteger(Numbers.cast(left,  BigInteger.class),
                                      Numbers.cast(right, BigInteger.class));
            }
            case Numbers.FRACTION: {
                return applyAsFraction(Numbers.cast(left,  Fraction.class),
                                       Numbers.cast(right, Fraction.class));
            }
            case Numbers.LONG:
            case Numbers.INTEGER:
            case Numbers.SHORT:
            case Numbers.BYTE: {
                return applyAsLong(left.longValue(), right.longValue());
            }
        }
        return applyAsDouble((left  instanceof Float) ? DecimalFunctions.floatToDouble((Float) left)  : left.doubleValue(),
                             (right instanceof Float) ? DecimalFunctions.floatToDouble((Float) right) : right.doubleValue());
    }

    /**
     * Calculates this function using given operands of {@code long} primitive type. If this function is a
     * filter, then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link Long}, except for division which may produce a floating point number.
     *
     * @throws ArithmeticException if the operation overflows the 64 bits integer capacity.
     */
    protected abstract Number applyAsLong(long left, long right);

    /**
     * Calculates this function using given operands of {@code double} primitive type. If this function is a
     * filter, then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link Double}.
     */
    protected abstract Number applyAsDouble(double left, double right);

    /**
     * Calculates this function using given operands of {@code Fraction} type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link Fraction}.
     */
    protected abstract Number applyAsFraction(Fraction left, Fraction right);

    /**
     * Calculates this function using given operands of {@code BigInteger} type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link BigInteger}.
     */
    protected abstract Number applyAsInteger(BigInteger left, BigInteger right);

    /**
     * Calculates this function using given operands of {@code BigDecimal} type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link BigDecimal}.
     */
    protected abstract Number applyAsDecimal(BigDecimal left, BigDecimal right);

    /**
     * Returns a hash code value for this function.
     */
    @Override
    public int hashCode() {
        return (31 * expression1.hashCode() + expression2.hashCode()) ^ getClass().hashCode();
    }

    /**
     * Compares this function with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj.getClass() == getClass()) {
            final BinaryFunction other = (BinaryFunction) obj;
            return expression1.equals(other.expression1) &&
                   expression2.equals(other.expression2);
        }
        return false;
    }
}
