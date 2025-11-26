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

import org.apache.sis.filter.base.BinaryFunction;
import java.time.DateTimeException;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.temporal.TimeMethods;
import org.apache.sis.feature.internal.shared.FeatureExpression;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.temporal.Period;
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.TemporalOperator;
import org.opengis.filter.TemporalOperatorName;
import org.opengis.filter.InvalidFilterValueException;


/**
 * Temporal operations between a period and an instant or between two periods.
 * The base class represents the general case when don't know if the the argument are periods or not.
 * The subclasses represent specializations when at least one of the arguments is known to be a period.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <T>  the base type of temporal objects, or {@code Object.class} for any type.
 */
class TemporalFilter<R,T> extends BinaryFunction<R,T,T>
        implements TemporalOperator<R>, Optimization.OnFilter<R>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8248634286785309435L;

    /**
     * The operation to apply on instants or periods.
     */
    protected final TemporalOperation<T> operation;

    /**
     * Creates a new temporal function.
     *
     * @param  operation    the operation to apply on instants or periods.
     * @param  expression1  the first of the two expressions to be used by this function.
     * @param  expression2  the second of the two expressions to be used by this function.
     */
    private TemporalFilter(final TemporalOperation<T> operation,
                           final Expression<R, ? extends T> expression1,
                           final Expression<R, ? extends T> expression2)
    {
        super(expression1, expression2);
        this.operation = operation;
    }

    /**
     * Creates a new temporal function.
     *
     * @param  expression1  the first of the two expressions to be used by this function.
     * @param  expression2  the second of the two expressions to be used by this function.
     * @param  operation    the operation to apply on instants or periods.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <R,V> TemporalFilter<R,?> create(
            final Class<V> type,
            final TemporalOperation.Factory factory,
            final Expression<R, ? extends V> expression1,
            final Expression<R, ? extends V> expression2)
    {
        final Class<? extends V> c1 = getValueClass(expression1, type);
        final Class<? extends V> c2 = getValueClass(expression2, type);
        Class<? extends V> commonType = type;
        if (type.isInterface()) {
            for (final Class<?> c : Classes.findCommonInterfaces(c1, c2)) {
                if (commonType.isAssignableFrom(c)) {
                    commonType = (Class<? extends V>) c;        // Safe because verified by `isAssignableFrom(c)`.
                }
            }
        } else {
            final Class<?> c = Classes.findCommonClass(c1, c2);
            if (commonType.isAssignableFrom(c)) {
                commonType = (Class<? extends V>) c;            // Safe because verified by `isAssignableFrom(c)`.
            }
        }
        /*
         * We cannot use a more specific type here because the `find(…)` method argument is parameterized
         * with `<? extends T>` while its return value is parameterized with `<? super T>`. Java language
         * has no parameterized type that can express those conflicting covariance and contra-variance,
         * therefore we must use `<?>`.
         *
         * Creations of `TemporalFilter` instances below are safe because `TimeMethods.type` is a parent
         * of both `expression1` and `expression2` value types (verified by assertions). Therefore, with
         * `commonType` of type `Class<T>` no matter if <T> is a super-type or a sub-type of <V>, we can
         * assert that the parmeterized type of the two expressions is `<? extends T>`.
         */
        final TemporalOperation<?> operation = factory.create(TimeMethods.find(commonType)).unique();
        assert operation.comparators.type.isAssignableFrom(commonType) : commonType;
        assert commonType.isAssignableFrom(c1) : c1;
        assert commonType.isAssignableFrom(c2) : c2;
        if (Period.class.isAssignableFrom(commonType)) {
            // Safe because `commonType` extends both Period and T.
            return new Periods(operation, expression1, expression2);
        }
        if (operation.comparators.isDynamic()) {
            return new TemporalFilter(operation, expression1, expression2);
        }
        return new Instants(operation, expression1, expression2);
    }

    /**
     * Returns the class of values computed by the given expression, or {@code type} if unknown.
     *
     * @param  e     the expression from which to get the type.
     * @param  type  the base type, used as a default type.
     */
    private static <T> Class<? extends T> getValueClass(final Expression<?, ? extends T> e, final Class<T> type) {
        if (e instanceof FeatureExpression<?,?>) {
            return ((FeatureExpression<?, ? extends T>) e).getResultClass();
        }
        return type;
    }

    /**
     * Returns an identification of this operation.
     */
    @Override
    public final TemporalOperatorName getOperatorType() {
        return operation.getOperatorType();
    }

    /**
     * Returns the mathematical symbol for this temporal operation.
     *
     * @return the mathematical symbol, or 0 if none.
     */
    @Override
    protected final char symbol() {
        return operation.symbol();
    }

    /**
     * Casts an expression returning values of unknown type.
     * This is a helper function for {@code recreate(…)} method implementations.
     *
     * @param  effective  the expression to cast.
     * @return an expression that can be used with this temporal filter.
     * @throws ClassCastException if the expression cannot be cast.
     */
    protected final Expression<R, ? extends T> cast(final Expression<R,?> effective) {
        return effective.toValueType(operation.comparators.type);
    }

    /**
     * Creates a new filter of the same type but different parameters.
     */
    @Override
    public Filter<R> recreate(final Expression<R,?>[] effective) {
        return new TemporalFilter<>(operation, cast(effective[0]), cast(effective[1]));
    }

    /**
     * Determines if the test(s) represented by this filter passes with the given operands.
     * Values of {@link #expression1} and {@link #expression2} shall be two single values.
     *
     * @throws InvalidFilterValueException if two temporal objects cannot be compared.
     */
    @Override
    public boolean test(final R candidate) {
        final T left = expression1.apply(candidate);
        if (left != null) {
            final T right = expression2.apply(candidate);
            if (right != null) try {
                if (left instanceof Period) {
                    if (right instanceof Period) {
                        return operation.evaluate((Period) left, (Period) right);
                    } else {
                        return operation.evaluate((Period) left, right);
                    }
                } else if (right instanceof Period) {
                    return operation.evaluate(left, (Period) right);
                } else {
                    return operation.evaluate(left, right);
                }
            } catch (DateTimeException e) {
                throw new InvalidFilterValueException(Errors.format(
                        Errors.Keys.CannotCompareInstanceOf_2, left.getClass(), right.getClass()), e);
            }
        }
        return false;
    }


    /**
     * A temporal filters where both operands are ISO 19108 instants.
     *
     * @param  <R>  the type of resources used as inputs.
     * @param  <T>  the base type of temporal objects.
     */
    private static final class Instants<R,T> extends TemporalFilter<R,T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -3176521794130878518L;

        /** Creates a new filter. */
        Instants(TemporalOperation<T> operation,
                 Expression<R, ? extends T> expression1,
                 Expression<R, ? extends T> expression2)
        {
            super(operation, expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<R> recreate(final Expression<R,?>[] effective) {
            return new Instants<>(operation, cast(effective[0]), cast(effective[1]));
        }

        /** Tests if this filter passes on the given resource. */
        @Override public boolean test(final R candidate) {
            final T left = expression1.apply(candidate);
            if (left != null) {
                final T right = expression2.apply(candidate);
                if (right != null) try {
                    return operation.evaluate(left, right);
                } catch (DateTimeException e) {
                    throw new InvalidFilterValueException(Errors.format(
                            Errors.Keys.CannotCompareInstanceOf_2, left.getClass(), right.getClass()), e);
                }
            }
            return false;
        }
    }


    /**
     * A temporal filters where both operands are ISO 19108 periods.
     *
     * @param  <R>  the type of resources used as inputs.
     * @param  <T>  the base type of temporal objects.
     */
    private static final class Periods<R, T extends Period> extends TemporalFilter<R,T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 7570449007668484459L;

        /** Creates a new filter. */
        Periods(TemporalOperation<T> operation,
                Expression<R, ? extends T> expression1,
                Expression<R, ? extends T> expression2)
        {
            super(operation, expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<R> recreate(final Expression<R,?>[] effective) {
            return new Periods<>(operation, cast(effective[0]), cast(effective[1]));
        }

        /** Tests if this filter passes on the given resource. */
        @Override public boolean test(final R candidate) {
            final Period left = expression1.apply(candidate);
            if (left != null) {
                final Period right = expression2.apply(candidate);
                if (right != null) try {
                    return operation.evaluate(left, right);
                } catch (DateTimeException e) {
                    throw new InvalidFilterValueException(Errors.format(
                            Errors.Keys.CannotCompareInstanceOf_2, left.getClass(), right.getClass()), e);
                }
            }
            return false;
        }
    }
}
