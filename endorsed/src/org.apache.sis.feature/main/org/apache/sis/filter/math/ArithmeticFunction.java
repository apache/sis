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
package org.apache.sis.filter.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.opengis.util.ScopedName;
import org.apache.sis.feature.internal.shared.FeatureExpression;
import org.apache.sis.feature.internal.shared.FeatureProjectionBuilder;
import org.apache.sis.filter.Optimization;
import org.apache.sis.filter.visitor.FunctionNames;
import org.apache.sis.filter.base.BinaryFunctionWidening;
import org.apache.sis.math.Fraction;
import org.apache.sis.math.NumberType;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.AttributeType;
import org.opengis.filter.Expression;


/**
 * Arithmetic operations between two numerical values.
 * The nature of the operation depends on the subclass.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (typically {@code Feature}) used as inputs.
 * @param  <A>  the type of value computed by the two expressions used as inputs.
 */
public abstract class ArithmeticFunction<R, A extends Number> extends BinaryFunctionWidening<R, A, A>
        implements FeatureExpression<R, Number>, Optimization.OnExpression<R, Number>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2818625862630588268L;

    /**
     * Creates a new arithmetic function.
     *
     * @param  expression1  the first of the two expressions to be used by this function.
     * @param  expression2  the second of the two expressions to be used by this function.
     */
    protected ArithmeticFunction(final Expression<R, ? extends A> expression1,
                                 final Expression<R, ? extends A> expression2)
    {
        super(expression1, expression2);
    }

    /**
     * Returns the type of values computed by this expression.
     * It should be {@code Long} if we are certain that all results will be of that type.
     * The default implementation is suitable to addition, subtraction and multiplication.
     * Other operations should override this method.
     */
    @Override
    public Class<? extends Number> getResultClass() {
        NumberType type = widestOperandType();
        switch (type) {
            case BYTE:
            case SHORT:
            case INTEGER: return Long.class;    // `applyAsLong(…)` is used for all these types.
            case LONG:    break;                // Because of the possibility of overflow.
            default: {
                type = effective(type);
                if (type.isReal()) {
                    return type.classOfValues(false).asSubclass(Number.class);
                }
            }
        }
        return Number.class;
    }

    /**
     * Creates an attribute type for numeric values of the given name.
     * The attribute is mandatory, unbounded and has no default value.
     * This is used for implementations of {@link #expectedType()}.
     *
     * @param  name  name of the attribute to create.
     * @return an attribute of the given name for numbers.
     */
    private static AttributeType<Number> createNumericType(final String name) {
        return createType(Number.class, name);
    }

    /**
     * Returns the type of results computed by this arithmetic function.
     * It should be a constant of the following form:
     *
     * {@snippet lang="java" :
     *     private static final AttributeType<Number> TYPE = createNumericType("Add");
     *
     *     @Override
     *     protected AttributeType<Number> expectedType() {
     *         return TYPE;
     *     }
     * }
     *
     * @return the type of result computed by this arithmetic function.
     */
    protected abstract AttributeType<Number> expectedType();

    /**
     * Provides the type of results computed by this expression. That type depends only
     * on the {@code ArithmeticFunction} subclass and is given by {@link #expectedType()}.
     */
    @Override
    public final FeatureProjectionBuilder.Item expectedType(FeatureProjectionBuilder addTo) {
        return addTo.addSourceProperty(expectedType(), false);
    }

    /**
     * Tries to optimize this function. Fist, this method applies the optimization documented
     * in the {@linkplain Optimization.OnExpression#optimize default method impmementation}.
     * Then, if it is possible to avoid to inspect the number types every time that the function
     * is evaluated, this method returns a more direct implementation.
     *
     * @param  optimization  the simplifications or optimizations to apply on this arithmetic function.
     * @return the simplified or optimized function, or {@code this} if no optimization has been applied.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final Expression<R, ? extends Number> optimize(final Optimization optimization) {
        final Expression<R, ? extends Number> result = Optimization.OnExpression.super.optimize(optimization);
        if (result instanceof ArithmeticFunction<?,?>) {
            final var optimized = ((ArithmeticFunction<R,?>) result).specialize();
            if (optimized != null) {
                return optimized;
            }
        }
        return result;
    }

    /**
     * Evaluates the expression for producing a result of numeric type.
     * This method delegates to one of the {@code applyAs(…)} methods.
     * Thus is the default implementation used when no specialization
     * was found by the {@link #optimize(Optimization)} method.
     *
     * <h4>Null values</h4>
     * This method returns {@code null} if at least one operand evaluated to {@code null}.
     * Otherwise, this method never return {@code null} if no {@code applyAs(…)} implementation can return null.
     *
     * @param  feature  the resource (usually a feature instance) from which to take the inputs.
     * @return result of the arithmetic operation, or {@code null} if at least one operand is null.
     */
    @Override
    public final Number apply(final R feature) {
        final Number left  = expression1.apply(feature);
        if (left != null) {
            final Number right = expression2.apply(feature);
            if (right != null) {
                return apply(left, right);
            }
        }
        return null;
    }

    /**
     * The "Add" (+) expression.
     *
     * @param  <R>  the type of resources (typically {@code Feature}) used as inputs.
     * @param  <V>  the type of value computed by the two expressions used as inputs.
     */
    public static final class Add<R, V extends Number> extends ArithmeticFunction<R,V> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 5445433312445869201L;

        /**
         * Creates a new expression for the {@code "Add"} operation.
         *
         * @param  expression1  the first of the two expressions to be used by this operation.
         * @param  expression2  the second of the two expressions to be used by this operation.
         */
        public Add(final Expression<R, ? extends V> expression1,
                   final Expression<R, ? extends V> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new arithmetic function for the same operation but using different parameters. */
        @Override public Expression<R, Number> recreate(final Expression<R,?>[] effective) {
            return new Add<>(effective[0].toValueType(Number.class),
                             effective[1].toValueType(Number.class));
        }

        /** Description of results of the {@code "Add"} expression. */
        @Override protected AttributeType<Number> expectedType() {return TYPE;}
        private static final AttributeType<Number> TYPE = createNumericType(FunctionNames.Add);

        /** Representation of the {@code "Add"} operation. */
        @Override protected char symbol() {return '+';}
        @Override public ScopedName getFunctionName() {return NAME;}
        private static final ScopedName NAME = createName(FunctionNames.Add);

        /** Applies this expression to the given operands. */
        @Override protected Number applyAsDouble  (double     left, double     right) {return left + right;}
        @Override protected Number applyAsFraction(Fraction   left, Fraction   right) {return left.add(right);}
        @Override protected Number applyAsDecimal (BigDecimal left, BigDecimal right) {return left.add(right);}
        @Override protected Number applyAsInteger (BigInteger left, BigInteger right) {return left.add(right);}
        @Override protected Number applyAsLong    (long       left, long       right) {return Math.addExact(left, right);}
    }


    /**
     * The "Subtract" (−) expression.
     *
     * @param  <R>  the type of resources (typically {@code Feature}) used as inputs.
     * @param  <V>  the type of value computed by the two expressions used as inputs.
     */
    public static final class Subtract<R, V extends Number> extends ArithmeticFunction<R,V> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 3048878022726271508L;

        /**
         * Creates a new expression for the {@code "Subtract"} operation.
         *
         * @param  expression1  the first of the two expressions to be used by this operation.
         * @param  expression2  the second of the two expressions to be used by this operation.
         */
        public Subtract(final Expression<R, ? extends V> expression1,
                        final Expression<R, ? extends V> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new arithmetic function for the same operation but using different parameters. */
        @Override public Expression<R, Number> recreate(final Expression<R,?>[] effective) {
            return new Subtract<>(effective[0].toValueType(Number.class),
                                  effective[1].toValueType(Number.class));
        }

        /** Description of results of the {@code "Subtract"} expression. */
        @Override protected AttributeType<Number> expectedType() {return TYPE;}
        private static final AttributeType<Number> TYPE = createNumericType(FunctionNames.Subtract);

        /** Representation of the {@code "Subtract"} operation. */
        @Override protected char symbol() {return '−';}
        @Override public ScopedName getFunctionName() {return NAME;}
        private static final ScopedName NAME = createName(FunctionNames.Subtract);

        /** Applies this expression to the given operands. */
        @Override protected Number applyAsDouble  (double     left, double     right) {return left - right;}
        @Override protected Number applyAsFraction(Fraction   left, Fraction   right) {return left.subtract(right);}
        @Override protected Number applyAsDecimal (BigDecimal left, BigDecimal right) {return left.subtract(right);}
        @Override protected Number applyAsInteger (BigInteger left, BigInteger right) {return left.subtract(right);}
        @Override protected Number applyAsLong    (long       left, long       right) {return Math.subtractExact(left, right);}
    }


    /**
     * The "Multiply" (×) expression.
     *
     * @param  <R>  the type of resources (typically {@code Feature}) used as inputs.
     * @param  <V>  the type of value computed by the two expressions used as inputs.
     */
    public static final class Multiply<R, V extends Number> extends ArithmeticFunction<R,V> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -1300022614832645625L;

        /**
         * Creates a new expression for the {@code "Multiply"} operation.
         *
         * @param  expression1  the first of the two expressions to be used by this operation.
         * @param  expression2  the second of the two expressions to be used by this operation.
         */
        public Multiply(final Expression<R, ? extends V> expression1,
                        final Expression<R, ? extends V> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new arithmetic function for the same operation but using different parameters. */
        @Override public Expression<R, Number> recreate(final Expression<R,?>[] effective) {
            return new Multiply<>(effective[0].toValueType(Number.class),
                                  effective[1].toValueType(Number.class));
        }

        /** Description of results of the {@code "Multiply"} expression. */
        @Override protected AttributeType<Number> expectedType() {return TYPE;}
        private static final AttributeType<Number> TYPE = createNumericType(FunctionNames.Multiply);

        /** Representation of the {@code "Multiply"} operation. */
        @Override protected char symbol() {return '×';}
        @Override public ScopedName getFunctionName() {return NAME;}
        private static final ScopedName NAME = createName(FunctionNames.Multiply);

        /** Applies this expression to the given operands. */
        @Override protected Number applyAsDouble  (double     left, double     right) {return left * right;}
        @Override protected Number applyAsFraction(Fraction   left, Fraction   right) {return left.multiply(right);}
        @Override protected Number applyAsDecimal (BigDecimal left, BigDecimal right) {return left.multiply(right);}
        @Override protected Number applyAsInteger (BigInteger left, BigInteger right) {return left.multiply(right);}
        @Override protected Number applyAsLong    (long       left, long       right) {return Math.multiplyExact(left, right);}
    }


    /**
     * The "Divide" (÷) expression.
     *
     * @param  <R>  the type of resources (typically {@code Feature}) used as inputs.
     * @param  <V>  the type of value computed by the two expressions used as inputs.
     */
    public static final class Divide<R, V extends Number> extends ArithmeticFunction<R,V> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -7709291845568648891L;

        /**
         * Creates a new expression for the {@code "Divide"} operation.
         *
         * @param  expression1  the first of the two expressions to be used by this operation.
         * @param  expression2  the second of the two expressions to be used by this operation.
         */
        public Divide(final Expression<R, ? extends V> expression1,
                      final Expression<R, ? extends V> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new arithmetic function for the same operation but using different parameters. */
        @Override public Expression<R, Number> recreate(final Expression<R,?>[] effective) {
            return new Divide<>(effective[0].toValueType(Number.class),
                                effective[1].toValueType(Number.class));
        }

        /** Description of results of the {@code "Divide"} expression. */
        @Override protected AttributeType<Number> expectedType() {return TYPE;}
        private static final AttributeType<Number> TYPE = createNumericType(FunctionNames.Divide);

        /** Representation of the {@code "Divide"} operation. */
        @Override protected char symbol() {return '÷';}
        @Override public ScopedName getFunctionName() {return NAME;}
        private static final ScopedName NAME = createName(FunctionNames.Divide);

        /** Divides the given integers, changing the type if the result is not an integer. */
        @Override protected Number applyAsDouble  (double     left, double     right) {return left / right;}
        @Override protected Number applyAsFraction(Fraction   left, Fraction   right) {return left.divide(right);}
        @Override protected Number applyAsDecimal (BigDecimal left, BigDecimal right) {return left.divide(right);}
        @Override protected Number applyAsInteger (BigInteger left, BigInteger right) {
            BigInteger[] r = left.divideAndRemainder(right);
            if (BigInteger.ZERO.equals(r[1])) {
                return r[0];
            } else {
                return Fraction.valueOf(r[1].longValueExact(), right.longValueExact())
                      .add(new Fraction(r[0].intValueExact(),  1));
            }
        }

        /** Divides the given integers, changing the type if the result is not an integer. */
        @Override protected Number applyAsLong(final long left, final long right) {
            final long r = left / right;
            if (left % right == 0) {
                return r;
            } else {
                return Fraction.valueOf(left, right);
            }
        }

        /** {@return {@code Number} by default because the result may change the class} */
        @Override public Class<? extends Number> getResultClass() {
            return effective(widestOperandType()) == NumberType.DOUBLE ? Double.class : Number.class;
        }
    }
}
