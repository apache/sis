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

import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.math.Fraction;

// Branch-dependent imports
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.filter.expression.BinaryExpression;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;


/**
 * Arithmetic operations between two numerical values.
 * The nature of the operation depends on the subclass.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class ArithmeticFunction extends BinaryFunction implements BinaryExpression, FeatureExpression {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2818625862630588268L;

    /**
     * Creates a new arithmetic function.
     */
    ArithmeticFunction(final Expression expression1, final Expression expression2) {
        super(expression1, expression2);
    }

    /**
     * Creates an attribute type for numeric values of the given name.
     * The attribute is mandatory, unbounded and has no default value.
     *
     * @param  name  name of the attribute to create.
     * @return an attribute of the given name for numbers.
     */
    static AttributeType<Number> createNumericType(final String name) {
        return createType(Number.class, name);
    }

    /**
     * Returns the type of results computed by this arithmetic function.
     */
    protected abstract AttributeType<Number> expectedType();

    /**
     * Provides the type of results computed by this expression. That type depends only
     * on the {@code ArithmeticFunction} subclass and is given by {@link #expectedType()}.
     */
    @Override
    public final PropertyTypeBuilder expectedType(FeatureType ignored, FeatureTypeBuilder addTo) {
        return addTo.addProperty(expectedType());
    }

    /**
     * Evaluates this expression based on the content of the given object. This method delegates to
     * {@link #applyAsDouble(double, double)}, {@link #applyAsLong(long, long)} or similar methods
     * depending on the value types.
     *
     * @throws ArithmeticException if the operation overflows the capacity of the type used.
     */
    @Override
    public final Object evaluate(final Object feature) {
        return evaluate(feature, Number.class);
    }

    /**
     * Evaluates the expression for producing a result of the given type. This method delegates to
     * {@link #applyAsDouble(double, double)}, {@link #applyAsLong(long, long)} or similar methods
     * depending on the value types. If this method can not produce a value of the given type,
     * then it returns {@code null}.
     *
     * @param  feature  to feature to evaluate with this expression.
     * @param  target   the desired type for the expression result.
     * @return the result, or {@code null} if it can not be of the specified type.
     * @throws ClassCastException if an expression returned the value in an expected type.
     * @throws ArithmeticException if the operation overflows the capacity of the type used.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <T> T evaluate(final Object feature, final Class<T> target) {
        ArgumentChecks.ensureNonNull("target", target);
        if (Number.class.isAssignableFrom(target)) try {
            final Number left = (Number) expression1.evaluate(feature, target);
            if (left != null) {
                final Number right = (Number) expression2.evaluate(feature, target);
                if (right != null) {
                    final Number result = apply(left, right);
                    final Number casted = Numbers.cast(result, (Class<? extends Number>) target);
                    if (Numerics.equals(result.doubleValue(), casted.doubleValue())) {
                        return (T) casted;
                    }
                }
            }
        } catch (ArithmeticException e) {
            warning(e);
        }
        return null;
    }


    /**
     * The "Add" (+) expression.
     */
    static final class Add extends ArithmeticFunction implements org.opengis.filter.expression.Add {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 5445433312445869201L;

        /** Description of results of the {@value #NAME} expression. */
        private static final AttributeType<Number> TYPE = createNumericType(NAME);
        @Override protected AttributeType<Number> expectedType() {return TYPE;}

        /** Creates a new expression for the {@value #NAME} operation. */
        Add(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}
        @Override protected char   symbol()  {return '+';}

        /** Applies this expression to the given operands. */
        @Override protected Number applyAsDouble  (double     left, double     right) {return left + right;}
        @Override protected Number applyAsFraction(Fraction   left, Fraction   right) {return left.add(right);}
        @Override protected Number applyAsDecimal (BigDecimal left, BigDecimal right) {return left.add(right);}
        @Override protected Number applyAsInteger (BigInteger left, BigInteger right) {return left.add(right);}
        @Override protected Number applyAsLong    (long       left, long       right) {return Math.addExact(left, right);}

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(ExpressionVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The "Sub" (−) expression.
     */
    static final class Subtract extends ArithmeticFunction implements org.opengis.filter.expression.Subtract {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 3048878022726271508L;

        /** Description of results of the {@value #NAME} expression. */
        private static final AttributeType<Number> TYPE = createNumericType(NAME);
        @Override protected AttributeType<Number> expectedType() {return TYPE;}

        /** Creates a new expression for the {@value #NAME} operation. */
        Subtract(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}
        @Override protected char   symbol()  {return '−';}

        /** Applies this expression to the given operands. */
        @Override protected Number applyAsDouble  (double     left, double     right) {return left - right;}
        @Override protected Number applyAsFraction(Fraction   left, Fraction   right) {return left.subtract(right);}
        @Override protected Number applyAsDecimal (BigDecimal left, BigDecimal right) {return left.subtract(right);}
        @Override protected Number applyAsInteger (BigInteger left, BigInteger right) {return left.subtract(right);}
        @Override protected Number applyAsLong    (long       left, long       right) {return Math.subtractExact(left, right);}

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(ExpressionVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The "Mul" (×) expression.
     */
    static final class Multiply extends ArithmeticFunction implements org.opengis.filter.expression.Multiply {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -1300022614832645625L;

        /** Description of results of the {@value #NAME} expression. */
        private static final AttributeType<Number> TYPE = createNumericType(NAME);
        @Override protected AttributeType<Number> expectedType() {return TYPE;}

        /** Creates a new expression for the {@value #NAME} operation. */
        Multiply(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}
        @Override protected char   symbol()  {return '×';}

        /** Applies this expression to the given operands. */
        @Override protected Number applyAsDouble  (double     left, double     right) {return left * right;}
        @Override protected Number applyAsFraction(Fraction   left, Fraction   right) {return left.multiply(right);}
        @Override protected Number applyAsDecimal (BigDecimal left, BigDecimal right) {return left.multiply(right);}
        @Override protected Number applyAsInteger (BigInteger left, BigInteger right) {return left.multiply(right);}
        @Override protected Number applyAsLong    (long       left, long       right) {return Math.multiplyExact(left, right);}

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(ExpressionVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The "Div" (÷) expression.
     */
    static final class Divide extends ArithmeticFunction implements org.opengis.filter.expression.Divide {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -7709291845568648891L;

        /** Description of results of the {@value #NAME} expression. */
        private static final AttributeType<Number> TYPE = createNumericType(NAME);
        @Override protected AttributeType<Number> expectedType() {return TYPE;}

        /** Creates a new expression for the {@value #NAME} operation. */
        Divide(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}
        @Override protected char   symbol()  {return '÷';}

        /** Applies this expression to the given operands. */
        @Override protected Number applyAsDouble  (double     left, double     right) {return left / right;}
        @Override protected Number applyAsFraction(Fraction   left, Fraction   right) {return left.divide(right);}
        @Override protected Number applyAsDecimal (BigDecimal left, BigDecimal right) {return left.divide(right);}
        @Override protected Number applyAsInteger (BigInteger left, BigInteger right) {
            BigInteger[] r = left.divideAndRemainder(right);
            if (BigInteger.ZERO.equals(r[1])) {
                return r[0];
            } else {
                return left.doubleValue() / right.doubleValue();
            }
        }

        /** Divides the given integers, changing the type to a floating point type if the result is not an integer. */
        @Override protected Number applyAsLong(final long left, final long right) {
            if (left % right == 0) {
                return left / right;
            } else {
                return left / (double) right;
            }
        }

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(ExpressionVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }
}
