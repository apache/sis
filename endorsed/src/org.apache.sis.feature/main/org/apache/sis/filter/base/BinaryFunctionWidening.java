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
import java.math.BigInteger;
import java.math.BigDecimal;
import org.apache.sis.util.ConditionallySafe;
import org.apache.sis.math.Fraction;
import org.apache.sis.math.NumberType;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.feature.internal.shared.FeatureExpression;
import org.apache.sis.feature.internal.shared.FeatureProjectionBuilder;
import org.apache.sis.filter.Optimization;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.util.ScopedName;


/**
 * Expression performing an operation on two expressions with values convertible to {@code Number}.
 * Each {@link Number} instance will be converted to {@code long}, {@code double}, {@link Fraction},
 * {@link BigInteger} or {@link BigDecimal} before the operation is applied. This class is used for
 * operations where specialized methods exist for each of above-cited types.
 *
 * <p>The inputs are not necessarily of the same class, but typically need to be promoted to the widest type
 * before the operation is executed. The result may be of a type different to all input types. For example,
 * a division of two {@link Integer} values may produce a {@link Fraction}, and a multiplication of the same
 * {@link Integer} values may produce a {@link Long}.</p>
 *
 * <p>The current version does not provide optimization for every cases. It is not clear that it is worth
 * to optimize the {@link Fraction}, {@link BigInteger} and {@link BigDecimal} cases.</p>
 *
 * <h2>Requirement</h2>
 * If a subclass implements {@link Expression}, then it shall also implement {@link FeatureExpression}
 * and the type parameters <strong>must</strong> be {@code Expression<R, Number>}. That subclass shall
 * also implement {@link Optimization.OnExpression}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>   the type of resources (e.g. {@code Feature}) used as inputs.
 * @param  <A1>  the type of value computed by the first expression (left operand).
 * @param  <A2>  the type of value computed by the second expression (right operand).
 */
public abstract class BinaryFunctionWidening<R, A1, A2> extends BinaryFunction<R, A1, A2> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2515131813531876123L;

    /**
     * Creates a new binary function.
     *
     * @param  expression1  the first of the two expressions to be used by this function.
     * @param  expression2  the second of the two expressions to be used by this function.
     */
    protected BinaryFunctionWidening(final Expression<R, ? extends A1> expression1,
                                     final Expression<R, ? extends A2> expression2)
    {
        super(expression1, expression2);
    }

    /**
     * Tries to return an expression which will invoke more directly an {@code applyAsXXX(…)} method,
     * without the need to inspect the argument type. The returned expression, if non-null, should be
     * more efficient than {@link #apply(Number, Number)}.
     *
     * @return the simplified or optimized function, or {@code null} if no optimization has been applied.
     */
    @SuppressWarnings("unchecked")
    protected final Expression<R, ? extends Number> specialize() {
        switch (effective(widestOperandType())) {
            case LONG:   return new Longs<>  ((BinaryFunctionWidening<R, ? extends Number, ? extends Number>) this);
            case DOUBLE: return new Doubles<>((BinaryFunctionWidening<R, ? extends Number, ? extends Number>) this);
            default:     return null;
        }
    }

    /**
     * Returns the type of values computed by this expression.
     * In case of doubt, this method returns the {@link Number} base class.
     *
     * @return the type of values computed by this expression.
     *
     * @see #widestOperandType()
     * @see #effective(NumberType)
     */
    protected Class<? extends Number> getResultClass() {
        return Number.class;
    }

    /**
     * Returns an enumeration value identifying the type of return value of the given expression.
     * If the expression result cannot be mapped to a number type, returns {@link NumberType#NULL}.
     *
     * @param  expression  the expression for which to identifying the type of return value.
     * @return type of numbers computed by the given expression.
     *
     * @see FeatureExpression#getResultClass()
     */
    private static NumberType getNumberType(final Expression<?,?> expression) {
        return (expression instanceof FeatureExpression<?,?>)
                ? NumberType.forClass(((FeatureExpression<?,?>) expression).getResultClass()).orElse(NumberType.NULL) : NumberType.NULL;
    }

    /**
     * Returns the widest type of the given arguments, or {@link NumberType#NULL} if none.
     * Note that conversions to the returned type are not guaranteed to be lossless.
     * For example, conversions from {@code long} to {@code double} may loss accuracy.
     *
     * <p>Conversion from {@code float} to {@code double} is disallowed because the
     * {@link #apply(Number, Number)} method handles the decimal representation.</p>
     */
    private static NumberType widest(final NumberType t1, final NumberType t2) {
        if (t1 == t2) return t1;
        if (t1.isWiderThan(t2)) {
            if (t2 != NumberType.FLOAT || t1 == NumberType.BIG_DECIMAL) return t1;
        } else if (t2.isWiderThan(t1)) {
            if (t1 != NumberType.FLOAT || t2 == NumberType.BIG_DECIMAL) return t2;
        }
        return NumberType.NULL;
    }

    /**
     * Returns the widest operand type, or {@link NumberType#NULL} if it cannot be determined.
     * Note that conversions to the returned type are not guaranteed to be lossless.
     * For example, conversions from {@code long} to {@code double} may loss accuracy.
     *
     * @return the widest operand type, or {@link NumberType#NULL}.
     *
     * @see FeatureExpression#getResultClass()
     */
    protected final NumberType widestOperandType() {
        return widest(getNumberType(expression1), getNumberType(expression2));
    }

    /**
     * Simplifies the given type to one of the types handled as a special case in this class.
     * The {@code switch} statement in this method's body shall be consistent with the switch
     * statement in {@link #apply(Number, Number)}, with the addition of the {@code NULL} and
     * {@code NUMBER} cases.
     *
     * @param  type  a number type.
     * @return one of {@code LONG}, {@code DOUBLE}, {@code FRACTION}, {@code BIG_INTEGER},
     *         {@code BIG_DECIMAL}, {@code NUMBER} or {@code NULL}.
     */
    protected static NumberType effective(final NumberType type) {
        switch (type) {
            case NULL:      // Case of expressions without `FeatureExpression.getResultType()`.
            case NUMBER:    // Case of expressions that declare only the generic `Number` class.
            case FRACTION:
            case BIG_INTEGER:
            case BIG_DECIMAL: return type;
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG: return NumberType.LONG;
            default: return NumberType.DOUBLE;  // The fallback used for unrecognized types.
        }
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
        final NumberType type = widest(
                NumberType.forNumberClass(left.getClass()),
                NumberType.forNumberClass(right.getClass()));
        try {
            switch (type) {
                case FRACTION: {
                    return applyAsFraction((Fraction) type.cast(left),
                                           (Fraction) type.cast(right));
                }
                case BIG_INTEGER: {
                    return applyAsInteger((BigInteger) type.cast(left),
                                          (BigInteger) type.cast(right));
                }
                case BIG_DECIMAL: {
                    return applyAsDecimal((BigDecimal) type.cast(left),
                                          (BigDecimal) type.cast(right));
                }
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG: {
                    return applyAsLong(left.longValue(), right.longValue());
                }
            }
        } catch (IllegalArgumentException | ArithmeticException e) {
            /*
             * Integer overflow, or division by zero, or attempt to convert NaN or infinity
             * to `BigDecimal`, or division does not have a terminating decimal expansion.
             * This is recoverable because we can fallback on floating point arithmetic.
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
     * @param  left   the first operand.
     * @param  right  the second operand.
     * @return the result of applying the function on the given operands.
     * @throws ArithmeticException if the operation overflows or if there is a division by zero.
     */
    protected abstract Number applyAsLong(long left, long right);

    /**
     * Calculates this function using given operands of {@code double} primitive type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link Double}.
     * This method may return {@code null} if the operation cannot apply on numbers.
     *
     * @param  left   the first operand.
     * @param  right  the second operand.
     * @return the result of applying the function on the given operands.
     */
    protected abstract Number applyAsDouble(double left, double right);

    /**
     * Calculates this function using given operands of {@code Fraction} type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link Fraction}.
     * This method may return {@code null} if the operation cannot apply on numbers.
     *
     * @param  left   the first operand.
     * @param  right  the second operand.
     * @return the result of applying the function on the given operands.
     * @throws ArithmeticException if the operation overflows or if there is a division by zero.
     */
    protected abstract Number applyAsFraction(Fraction left, Fraction right);

    /**
     * Calculates this function using given operands of {@code BigInteger} type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link BigInteger}, except for division which may produce other types.
     * This method may return {@code null} if the operation cannot apply on numbers.
     *
     * @param  left   the first operand.
     * @param  right  the second operand.
     * @return the result of applying the function on the given operands.
     * @throws ArithmeticException if there is a division by zero.
     */
    protected abstract Number applyAsInteger(BigInteger left, BigInteger right);

    /**
     * Calculates this function using given operands of {@code BigDecimal} type. If this function is a filter,
     * then this method should returns an {@link Integer} value 0 or 1 for false or true respectively.
     * Otherwise the result is usually a {@link BigDecimal}.
     * This method may return {@code null} if the operation cannot apply on numbers.
     *
     * @param  left   the first operand.
     * @param  right  the second operand.
     * @return the result of applying the function on the given operands.
     * @throws ArithmeticException if a division does not have a terminating decimal expansion.
     */
    protected abstract Number applyAsDecimal(BigDecimal left, BigDecimal right);




    /**
     * An expression which will invoke more directly an {@code applyAsXXX(…)} method,
     * without the need to inspect the argument type.
     *
     * @param  <R>  the type of resources (typically {@code Feature}) used as inputs.
     * @param  <A>  the type of value computed by the two expressions used as inputs.
     */
    private static abstract class Specialization<R, A extends Number> extends Node
            implements FeatureExpression<R, Number>, Optimization.OnExpression<R, Number>
    {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -6902891170861955149L;

        /** The implementation of the function. */
        protected final BinaryFunctionWidening<R, ? extends A, ? extends A> delegate;

        /** Creates a new specialization which will delegate the work to the given implementation. */
        protected Specialization(final BinaryFunctionWidening<R, ? extends A, ? extends A> delegate) {
            this.delegate = delegate;
        }

        /** Delegates to the function. */
        @Override public    final ScopedName            getFunctionName()  {return ((Expression<?,?>) delegate).getFunctionName();}
        @Override public    final Class<? super R>      getResourceClass() {return delegate.getResourceClass();}
        @Override public    final List<Expression<R,?>> getParameters()    {return delegate.getParameters();}
        @Override protected final Collection<?>         getChildren()      {return delegate.getChildren();}

        /** Returns the type of values computed by this expression. */
        @Override public final Class<? extends Number> getResultClass() {
            return delegate.getResultClass();
        }

        /**
         * Provides the type of results computed by the implementation of the function.
         * The value type is declared as the generic {@link Number} type rather than {@code <V>},
         * but this is desired as the result of division is not always of type {@code <V>}.
         */
        @Override public final FeatureProjectionBuilder.Item expectedType(FeatureProjectionBuilder addTo) {
            return ((FeatureExpression<?,?>) delegate).expectedType(addTo);
        }

        /**
         * Delegates the optimization to the implementation and checks if the result is the same.
         * This method performs a cast which is safe only if the requirement documented in the
         * {@link BinaryFunctionWidening} javadoc is true.
         */
        @Override public final Expression<R, ? extends Number> optimize(final Optimization optimization) {
            @ConditionallySafe
            @SuppressWarnings("unchecked")  // See Javadoc
            final Expression<R, ? extends Number> result = ((Optimization.OnExpression<R, Number>) delegate).optimize(optimization);
            if (result.getClass() == getClass() && ((Specialization<?,?>) result).delegate == delegate) {
                return this;
            }
            return result;
        }
    }




    /**
     * An expression which will invoke more directly the {@code applyAsLong(…)} method.
     * This implementation can be used with operands of type {@link Byte}, {@link Short},
     * {@link Integer} and {@link Long}.
     *
     * @param  <R>  the type of resources (typically {@code Feature}) used as inputs.
     */
    private static final class Longs<R> extends Specialization<R, Number> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 8799719407972742175L;

        /** Creates a new specialization for integers. */
        Longs(BinaryFunctionWidening<R, ? extends Number, ? extends Number> delegate) {
            super(delegate);
        }

        /** Executes the operation with the assumption that values are convertible to {@code long}. */
        @Override public Number apply(final R feature) {
            final Number left  = delegate.expression1.apply(feature);
            if (left != null) {
                final Number right = delegate.expression2.apply(feature);
                if (right != null) try {
                    return delegate.applyAsLong(left.longValue(), right.longValue());
                } catch (IllegalArgumentException | ArithmeticException e) {
                    warning(e, true);
                    return delegate.applyAsDouble(left.doubleValue(), right.doubleValue());
                }
            }
            return null;
        }
    }




    /**
     * An expression which will invoke more directly the {@code applyAsDouble(…)} method.
     * This implementation can be used with operands of type {@link Byte}, {@link Short},
     * {@link Integer}, {@link Long}, {@link Fraction} and {@link Double}. Note that the
     * {@link Float} type is excluded because we use a conversion that tries to preserve
     * the decimal representation.
     *
     * @param  <R>  the type of resources (typically {@code Feature}) used as inputs.
     */
    private static final class Doubles<R> extends Specialization<R, Number> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -1962350161229383018L;

        /** Creates a new specialization for integers. */
        Doubles(BinaryFunctionWidening<R, ? extends Number, ? extends Number> delegate) {
            super(delegate);
        }

        /** Executes the operation with the assumption that values are convertible to {@code long}. */
        @Override public Number apply(final R feature) {
            final Number left  = delegate.expression1.apply(feature);
            if (left != null) {
                final Number right = delegate.expression2.apply(feature);
                if (right != null) {
                    return delegate.applyAsDouble(left.doubleValue(), right.doubleValue());
                }
            }
            return null;
        }
    }
}
