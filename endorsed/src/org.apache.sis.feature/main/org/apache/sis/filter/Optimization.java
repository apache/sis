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

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.ConcurrentModificationException;
import java.util.function.Predicate;
import org.opengis.util.CodeList;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.filter.internal.Node;
import org.apache.sis.util.privy.CollectionsExt;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Filter;
import org.opengis.filter.Literal;
import org.opengis.filter.Expression;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.feature.FeatureType;


/**
 * Description of optimizations or simplifications to attempt on filters and expressions.
 * Optimizations can include the following changes:
 *
 * <ul>
 *   <li>Application of some logical identities such as {@code NOT(NOT(A)) == A}.</li>
 *   <li>Application of logical short circuits such as {@code A & FALSE == FALSE}.</li>
 *   <li>Immediate evaluation of expressions where all parameters are literal values.</li>
 * </ul>
 *
 * The following options can enable some additional optimizations:
 *
 * <ul>
 *   <li>The type of the {@code Feature} instances to be filtered.</li>
 * </ul>
 *
 * <h2>Usage in multi-threads context</h2>
 * This class is <strong>not</strong> thread-safe.
 * A new instance shall be created for each thread applying optimizations. Example:
 *
 * {@snippet lang="java" :
 *     Filter<R> filter = ...;
 *     filter = new Optimization().apply(filter);
 *     }
 *
 * <h2>How optimizations are applied</h2>
 * Optimizations are specific to each expression and filter type.
 * For optimizations to happen, classes must implement the {@link OnExpression} or {@link OnFilter} interface.
 * The {@link #apply(Filter)} and {@link #apply(Expression)} methods in this {@code Optimization} class merely
 * delegate to the methods defined in above-cited interfaces, with safety guards against infinite recursion.
 *
 * <h2>Behavioral changes</h2>
 * Optimized filters shall produce the same results as non-optimized filters.
 * However side-effects may differ, in particular regarding exceptions that may be thrown.
 * For example if a filter tests {@code A & B} and if {@code Optimization} determines that the {@code B}
 * condition will always evaluate to {@code false}, then the {@code A} condition will never be tested.
 * If that condition had side-effects or threw an exception,
 * those effects will disappear in the optimized filter.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
public class Optimization {
    /**
     * An arbitrary object meaning that a filter or expression optimization is under progress.
     */
    private static final Object COMPUTING = Void.TYPE;

    /**
     * The type of feature instances to be filtered, or {@code null} if unknown.
     */
    private FeatureType featureType;

    /**
     * Filters and expressions already optimized. Also used for avoiding never-ending loops.
     * The map is created when first needed.
     *
     * <h4>Implementation note</h4>
     * The same map is used for filters and expressions.
     * It is not a problem if keys do not implement the two interfaces at the same time.
     * If it happens anyway, it should still be okay because the method signatures are
     * the same in both interfaces (only the return type changes), so the same methods
     * would be invoked no matter if we consider the keys as a filter or an expression.
     */
    private Map<Object,Object> done;

    /**
     * Creates a new instance.
     */
    public Optimization() {
    }

    /**
     * Returns the type of feature instances to be filtered, or {@code null} if unknown.
     * This is the last value specified by a call to {@link #setFeatureType(FeatureType)}.
     * The default value is {@code null}.
     *
     * @return the type of feature instances to be filtered, or {@code null} if unknown.
     */
    public FeatureType getFeatureType() {
        return featureType;
    }

    /**
     * Sets the type of feature instances to be filtered.
     * If this type is known in advance, specifying it may allow to compute more specific
     * {@link org.apache.sis.util.ObjectConverter}s or to apply some geometry reprojection
     * in advance.
     *
     * @param  type  the type of feature instances to be filtered, or {@code null} if unknown.
     */
    public void setFeatureType(final FeatureType type) {
        featureType = type;
    }

    /**
     * Optimizes or simplifies the given filter. If the given instance implements the {@link OnFilter} interface,
     * then its {@code optimize(this)} method is invoked. Otherwise this method returns the given filter as-is.
     *
     * @param  <R>     the type of resources (e.g. {@code Feature}) used as inputs.
     * @param  filter  the filter to optimize, or {@code null}.
     * @return the optimized filter, or {@code null} if the given filter was null.
     *         May be {@code filter} if no optimization or simplification has been applied.
     * @throws IllegalArgumentException if the given filter is already in process of being optimized
     *         (i.e. there is a recursive call to {@code apply(…)} for the same filter).
     */
    public <R> Filter<R> apply(final Filter<R> filter) {
        if (!(filter instanceof OnFilter<?>)) {
            return filter;
        }
        final boolean isFirstCall = (done == null);
        if (isFirstCall) {
            done = new IdentityHashMap<>();
        }
        try {
            final Object previous = done.putIfAbsent(filter, COMPUTING);
            if (previous == COMPUTING) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.RecursiveCreateCallForKey_1, filter.getOperatorType()));
            }
            @SuppressWarnings("unchecked")
            Filter<R> result = (Filter<R>) previous;
            if (result == null) {
                result = ((OnFilter<R>) filter).optimize(this);
                if (done.put(filter, result) != COMPUTING) {
                    // Should not happen unless this `Optimization` is used concurrently in many threads.
                    throw new ConcurrentModificationException();
                }
            }
            return result;
        } finally {
            if (isFirstCall) {
                done = null;
            }
        }
    }

    /**
     * Filter that can be optimized. Each filter implementation knows which rules can be applied.
     * For that reason, the optimization algorithms are kept in each implementation class.
     * This interface allows {@link Optimization} to invoke that code.
     *
     * <p>Implementations need to override only one of the 2 methods defined in this interface.</p>
     *
     * @param  <R>  the type of resources to filter.
     */
    public interface OnFilter<R> extends Filter<R> {
        /**
         * Tries to optimize this filter. The default implementation performs the following steps:
         *
         * <ul>
         *   <li>If all expressions are literals, evaluate this filter immediately.</li>
         *   <li>Otherwise if at least one child expression can be optimized,
         *       {@linkplain #recreate(Expression[]) recreate} the filter.</li>
         *   <li>Otherwise returns {@code this}.</li>
         * </ul>
         *
         * @param  optimization  the simplifications or optimizations to apply on this filter.
         * @return the simplified or optimized filter, or {@code this} if no optimization has been applied.
         */
        default Filter<R> optimize(final Optimization optimization) {
            final List<Expression<R,?>> expressions = getExpressions();
            @SuppressWarnings({"unchecked", "rawtypes"})
            final Expression<R,?>[] effective = new Expression[expressions.size()];
            boolean unchanged = true;       // Will be `false` if at least one optimization has been applied.
            boolean immediate = true;
            for (int i=0; i<effective.length; i++) {
                Expression<R,?> e = expressions.get(i);
                unchanged &= (e == (e = optimization.apply(e)));
                immediate &= (e instanceof Literal<?,?>);
                effective[i] = e;
            }
            if (immediate) {
                return test(null) ? Filter.include() : Filter.exclude();
            } else if (unchanged) {
                return this;
            } else {
                return recreate(effective);
            }
        }

        /**
         * Creates a new filter of the same type and parameters than this filter, except for the expressions.
         * The expressions given to this method shall be equivalent to the expressions used by this filter,
         * potentially more efficient.
         *
         * <p>This method is used by the default implementation of {@link #optimize(Optimization)}
         * and can be ignored if above method is overridden.</p>
         *
         * @param  effective  the expressions to use as a replacement of this filter expressions.
         * @return the new filter, or {@code this} if unsupported.
         */
        default Filter<R> recreate(Expression<R,?>[] effective) {
            return this;
        }

        /**
         * If the given predicate can be cast to a filter of the same parameterized type as this,
         * returns {@code other} cast to that type. Otherwise returns {@code null}.
         *
         * @param  other  the predicate to cast to a filter compatible with this.
         * @return the cast predicate, or {@code null} if it cannot be cast.
         */
        @SuppressWarnings("unchecked")
        private Filter<R> castOrNull(final Predicate<? super R> other) {
            if (other instanceof Filter<?>) {
                final Class<?> type = getResourceClass();
                if (type != null) {
                    final Class<?> to = ((Filter<?>) other).getResourceClass();
                    if (to != null && type.isAssignableFrom(to)) {
                        return (Filter<R>) other;
                    }
                }
            }
            return null;
        }

        /**
         * Returns the {@code AND} logical operation between this filter and the given predicate.
         * If the given predicate is an instance of {@code Filter<R>}, then the returned predicate
         * is also an instance of {@code Filter<R>}.
         *
         * @param  other  the other predicate.
         * @return the {@code AND} logical operation between this filter and the given predicate.
         *
         * @see DefaultFilterFactory#and(Filter, Filter)
         */
        @Override
        default Predicate<R> and(final Predicate<? super R> other) {
            final Filter<R> filter = castOrNull(other);
            if (filter != null) {
                return new LogicalFilter.And<>(this, filter);
            } else {
                return Filter.super.and(other);
            }
        }

        /**
         * Returns the {@code OR} logical operation between this filter and the given predicate.
         * If the given predicate is an instance of {@code Filter<R>}, then the returned predicate
         * is also an instance of {@code Filter<R>}.
         *
         * @param  other  the other predicate.
         * @return the {@code OR} logical operation between this filter and the given predicate.
         *
         * @see DefaultFilterFactory#or(Filter, Filter)
         */
        @Override
        default Predicate<R> or(final Predicate<? super R> other) {
            final Filter<R> filter = castOrNull(other);
            if (filter != null) {
                return new LogicalFilter.Or<>(this, filter);
            } else {
                return Filter.super.and(other);
            }
        }

        /**
         * Returns the logical negation of this filter.
         * The returned predicate is an instance of {@code Optimization.OnFilter}.
         *
         * @return the logical negation of this filter.
         */
        @Override
        default Predicate<R> negate() {
            return new LogicalFilter.Not<>(this);
        }
    }

    /**
     * Optimizes or simplifies the given expression. If the given instance implements the {@link OnExpression}
     * interface, then its {@code optimize(this)} method is invoked. Otherwise this method returns the given
     * expression as-is.
     *
     * @param  <R>         the type of resources (e.g. {@code Feature}) used as inputs.
     * @param  <V>         the type of values computed by the expression.
     * @param  expression  the expression to optimize, or {@code null}.
     * @return the optimized expression, or {@code null} if the given expression was null.
     *         May be {@code expression} if no optimization or simplification has been applied.
     * @throws IllegalArgumentException if the given expression is already in process of being optimized
     *         (i.e. there is a recursive call to {@code apply(…)} for the same expression).
     */
    public <R,V> Expression<R, ? extends V> apply(final Expression<R,V> expression) {
        if (!(expression instanceof OnExpression<?,?>)) {
            return expression;
        }
        final boolean isFirstCall = (done == null);
        if (isFirstCall) {
            done = new IdentityHashMap<>();
        }
        try {
            final Object previous = done.putIfAbsent(expression, COMPUTING);
            if (previous == COMPUTING) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.RecursiveCreateCallForKey_1, expression.getFunctionName()));
            }
            @SuppressWarnings("unchecked")
            Expression<R, ? extends V> result = (Expression<R, ? extends V>) previous;
            if (result == null) {
                result = ((OnExpression<R,V>) expression).optimize(this);
                if (done.put(expression, result) != COMPUTING) {
                    // Should not happen unless this `Optimization` is used concurrently in many threads.
                    throw new ConcurrentModificationException();
                }
            }
            return result;
        } finally {
            if (isFirstCall) {
                done = null;
            }
        }
    }

    /**
     * Expression that can be optimized. Each expression implementation knows which rules can be applied.
     * For that reason, the optimization algorithms are kept in each implementation class.
     * This interface allows {@link Optimization} to invoke that code.
     *
     * <p>Implementations need to override only one of the 2 methods defined in this interface.</p>
     *
     * @param  <R>  the type of resources used as inputs.
     * @param  <V>  the type of values computed by the expression.
     */
    public interface OnExpression<R,V> extends Expression<R,V> {
        /**
         * Tries to optimize this expression. The default implementation performs the following steps:
         *
         * <ul>
         *   <li>If all expressions are {@linkplain Literal literals} and this expression is not a
         *       {@linkplain FunctionProperty#VOLATILE volatile function}, evaluate this expression immediately.</li>
         *   <li>Otherwise if at least one parameter can be optimized,
         *       {@linkplain #recreate(Expression[]) recreate} the expression.</li>
         *   <li>Otherwise returns {@code this}.</li>
         * </ul>
         *
         * @param  optimization  the simplifications or optimizations to apply on this expression.
         * @return the simplified or optimized expression, or {@code this} if no optimization has been applied.
         */
        default Expression<R, ? extends V> optimize(final Optimization optimization) {
            final List<Expression<R,?>> parameters = getParameters();
            @SuppressWarnings({"unchecked", "rawtypes"})
            final Expression<R,?>[] effective = new Expression[parameters.size()];
            boolean unchanged = true;       // Will be `false` if at least one optimization has been applied.
            boolean immediate = true;
            for (int i=0; i<effective.length; i++) {
                Expression<R,?> e = parameters.get(i);
                unchanged &= (e == (e = optimization.apply(e)));
                immediate &= (e instanceof Literal<?,?>);
                effective[i] = e;
            }
            if (immediate && !properties(this).contains(FunctionProperty.VOLATILE)) {
                return literal(apply(null));
            } else if (unchanged) {
                return this;
            } else {
                return recreate(effective);
            }
        }

        /**
         * Creates a new expression of the same type as this expression, but with optimized parameters.
         * The expressions given to this method shall be equivalent to the expressions used by this instance,
         * potentially more efficient.
         *
         * <p>This method is used by the default implementation of {@link #optimize(Optimization)}
         * and can be ignored if above method is overridden.</p>
         *
         * @param  effective  the expressions to use as a replacement of this expression parameters.
         * @return the new expression, or {@code this} if unsupported.
         */
        default Expression<R,V> recreate(Expression<R,?>[] effective) {
            return this;
        }
    }

    /**
     * Optimizes or simplifies the given filter and returns it as a list of {@code AND} operands.
     * If such list cannot be built, then this method returns the optimized filter in a singleton list.
     *
     * <h4>Use case</h4>
     * This method tries to transform a filter into a {@code F₀ AND F₁ AND F₂ AND F₃ AND ...} sequence.
     * This transformation is useful when some operands can be handled by the storage engine
     * (for example a SQL database) and other operands cannot.
     * For example, when reading features from a relational database,
     * the implementation may choose to express the F₁ and F₃ operands as SQL statements
     * and apply the other operands in Java code.
     *
     * @param  <R>     the type of resources (e.g. {@code Feature}) used as inputs.
     * @param  filter  the filter to decompose.
     * @return a sequence of {@code AND} operands, or an empty list if the given filter was null.
     * @throws ClassCastException if a filter declares the {@code AND}, {@code OR} or {@code NOT} type
     *         without implementing the {@link LogicalOperator} interface.
     */
    public <R> List<Filter<R>> applyAndDecompose(final Filter<R> filter) {
        return toAndOperands(apply(filter));
    }

    /**
     * Returns the given filter as a list of {@code AND} operands.
     * If such list cannot be built, then this method returns the given filter in a singleton list.
     *
     * @param  <R>     the type of resources (e.g. {@code Feature}) used as inputs.
     * @param  filter  the filter to decompose.
     * @return a sequence of {@code AND} operands, or an empty list if the given filter was null.
     * @throws ClassCastException if a filter declares the {@code AND}, {@code OR} or {@code NOT} type
     *         without implementing the {@link LogicalOperator} interface.
     */
    private static <R> List<Filter<R>> toAndOperands(final Filter<R> filter) {
        if (filter == null) {
            return List.of();
        }
        final CodeList<?> type = filter.getOperatorType();
        if (type == LogicalOperatorName.AND) {
            return ((LogicalOperator<R>) filter).getOperands();
        }
        if (type == LogicalOperatorName.NOT) {
            final Filter<R> nop = getNotOperand(filter);
            if (nop.getOperatorType() == LogicalOperatorName.OR) {
                final List<Filter<R>> operands = ((LogicalOperator<R>) nop).getOperands();
                final List<Filter<R>> result = new ArrayList<>(operands.size());
                for (Filter<R> operand : operands) {
                    if (operand.getOperatorType() == LogicalOperatorName.NOT) {
                        operand = getNotOperand(operand);
                    } else {
                        operand = new LogicalFilter.Not<>(operand);
                    }
                    result.add(operand);
                }
                return result;
            }
        }
        return List.of(filter);
    }

    /**
     * Returns the singleton operand of the given {@code NOT} filter.
     *
     * @param  filter  the {@code NOT} filter.
     * @return the single operand of the {@code NOT} filter (never null).
     * @throws ClassCastException if the filter does not implement the {@link LogicalOperator} interface.
     * @throws IllegalArgumentException if the filter does not have a single operand.
     */
    private static <R> Filter<R> getNotOperand(final Filter<R> filter) {
        final Filter<R> operand = CollectionsExt.singletonOrNull(((LogicalOperator<R>) filter).getOperands());
        if (operand != null) {
            return operand;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the manner in which values are computed from resources given to the specified filter.
     * This set of properties may determine which optimizations are allowed.
     * The values of particular interest are:
     *
     * <ul>
     *   <li>{@link FunctionProperty#VOLATILE} if the computed value changes each time that the filter is evaluated,
     *       even if the resource to evaluate stay the same immutable instance.</li>
     * </ul>
     *
     * @param  filter  the filter for which to query function properties.
     * @return the manners in which values are computed from resources.
     *
     * @since 1.4
     */
    public static Set<FunctionProperty> properties(final Filter<?> filter) {
        return Node.transitiveProperties(filter.getExpressions());
    }

    /**
     * Returns the manner in which values are computed from resources given to the specified expression.
     * This set of properties may determine which optimizations are allowed.
     * The values of particular interest are:
     *
     * <ul>
     *   <li>{@link FunctionProperty#INJECTIVE} if the computed value is unique for each resource instance (e.g. identifiers).</li>
     *   <li>{@link FunctionProperty#VOLATILE} if the computed value changes each time that the expression is evaluated,
     *       even if the resource to evaluate stay the same immutable instance.</li>
     *   <li>{@link FunctionProperty#isConstant(Set)} if the expression returns a constant value.</li>
     * </ul>
     *
     * @param  expression  the expression for which to query function properties.
     * @return the manners in which values are computed from resources.
     *
     * @since 1.4
     */
    public static Set<FunctionProperty> properties(final Expression<?,?> expression) {
        return Node.properties(expression);
    }

    /**
     * Creates a constant, literal value that can be used in expressions.
     * This is a helper methods for optimizations which simplified an expression to a constant value.
     *
     * @param  <R>    the type of resources used as inputs.
     * @param  <V>    the type of the value of the literal.
     * @param  value  the literal value. May be {@code null}.
     * @return a literal for the given value.
     *
     * @see DefaultFilterFactory#literal(Object)
     */
    public static <R,V> Literal<R,V> literal(final V value) {
        return new LeafExpression.Literal<>(value);
    }
}
