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
package org.apache.sis.filter.base;

import java.util.List;
import java.util.Collection;
import java.util.Objects;
import java.math.BigInteger;
import java.math.BigDecimal;
import org.apache.sis.util.Numbers;
import org.apache.sis.math.Fraction;
import org.apache.sis.math.DecimalFunctions;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;


/**
 * Base class for expressions, comparators or filters performing operations on two expressions.
 * The nature of the operation depends on the subclass. If operands are numerical values, they
 * may be converted to a common type before the operation is performed. That operation is not
 * necessarily an arithmetic operation; it may be a comparison for example.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>   the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <V1>  the type of value computed by the first expression.
 * @param  <V2>  the type of value computed by the second expression.
 */
public abstract class BinaryFunction<R,V1,V2> extends Node {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8632475810190545852L;

    /**
     * The first of the two expressions to be used by this function.
     *
     * @see #getExpression1()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected final Expression<R, ? extends V1> expression1;

    /**
     * The second of the two expressions to be used by this function.
     *
     * @see #getExpression2()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected final Expression<R, ? extends V2> expression2;

    /**
     * Creates a new binary function.
     *
     * @param  expression1  the first of the two expressions to be used by this function.
     * @param  expression2  the second of the two expressions to be used by this function.
     */
    protected BinaryFunction(final Expression<R, ? extends V1> expression1,
                             final Expression<R, ? extends V2> expression2)
    {
        this.expression1 = Objects.requireNonNull(expression1);
        this.expression2 = Objects.requireNonNull(expression2);
    }

    /**
     * Returns the class of resources expected by this filter.
     * Defined for {@link Filter#getResourceClass()} and {@link Expression#getResourceClass()} implementations.
     *
     * @return type of resources accepted by this filter, or {@code null} if inconsistent.
     */
    public final Class<? super R> getResourceClass() {
        return specializedClass(expression1.getResourceClass(),
                                expression2.getResourceClass());
    }

    /**
     * Returns the expressions used as parameters by this function.
     * Defined for {@link Expression#getParameters()} implementations.
     */
    public final List<Expression<R,?>> getParameters() {
        return getExpressions();
    }

    /**
     * Returns the two expressions used as parameters by this filter.
     * Defined for {@link Filter#getExpressions()} implementations.
     *
     * @return a list of size 2 containing the two expressions.
     */
    public final List<Expression<R,?>> getExpressions() {
        return List.of(expression1, expression2);
    }

    /**
     * Returns the two expressions in a list of size 2.
     * This is used for {@link #toString()}, {@link #hashCode()} and {@link #equals(Object)} implementations.
     */
    @Override
    protected final Collection<?> getChildren() {
        return getExpressions();
    }

    /**
     * Evaluates the expression for producing a result of numeric type.
     * This method delegates to one of the {@code applyAs(…)} methods.
     * If no {@code applyAs(…)} implementations can return null values,
     * this this method never return {@code null}.
     *
     * @param  left   the left operand. Cannot be null.
     * @param  right  the right operand. Cannot be null.
     * @return result of this function applied on the two given operands.
     *         May be {@code null} only if an {@code applyAs(…)} implementation returned a null value.
     */
    protected final Number apply(final Number left, final Number right) {
        final int type = Math.max(Numbers.getEnumConstant(left.getClass()),
                                  Numbers.getEnumConstant(right.getClass()));
        try {
            switch (type) {
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
        } catch (IllegalArgumentException | ArithmeticException e) {
            /*
             * Integer overflow, or division by zero, or attempt to convert NaN or infinity
             * to `BigDecimal`, or division does not have a terminating decimal expansion.
             */
            warning(e, true);
        }
        return applyAsDouble((left  instanceof Float) ? DecimalFunctions.floatToDouble((Float) left)  : left.doubleValue(),
                             (right instanceof Float) ? DecimalFunctions.floatToDouble((Float) right) : right.doubleValue());
    }

    /**
     * Calculates this function using given operands of {@code long} primitive type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link Long}, except for division which may produce other types.
     * This method may return {@code null} if the operation cannot apply on numbers.
     *
     * @throws ArithmeticException if the operation overflows or if there is a division by zero.
     */
    protected Number applyAsLong(long left, long right) {
        return null;
    }

    /**
     * Calculates this function using given operands of {@code double} primitive type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link Double}.
     * This method may return {@code null} if the operation cannot apply on numbers.
     */
    protected Number applyAsDouble(double left, double right) {
        return null;
    }

    /**
     * Calculates this function using given operands of {@code Fraction} type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link Fraction}.
     * This method may return {@code null} if the operation cannot apply on numbers.
     *
     * @throws ArithmeticException if the operation overflows or if there is a division by zero.
     */
    protected Number applyAsFraction(Fraction left, Fraction right) {
        return null;
    }

    /**
     * Calculates this function using given operands of {@code BigInteger} type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link BigInteger}, except for division which may produce other types.
     * This method may return {@code null} if the operation cannot apply on numbers.
     *
     * @throws ArithmeticException if there is a division by zero.
     */
    protected Number applyAsInteger(BigInteger left, BigInteger right) {
        return null;
    }

    /**
     * Calculates this function using given operands of {@code BigDecimal} type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link BigDecimal}.
     * This method may return {@code null} if the operation cannot apply on numbers.
     *
     * @throws ArithmeticException if a division does not have a terminating decimal expansion.
     */
    protected Number applyAsDecimal(BigDecimal left, BigDecimal right) {
        return null;
    }
}
