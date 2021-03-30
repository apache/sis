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
import java.util.List;
import java.util.IdentityHashMap;
import java.util.ConcurrentModificationException;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.opengis.filter.Filter;
import org.opengis.filter.Literal;
import org.opengis.filter.Expression;


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
 * Current version does not yet provide configuration options.
 * But this class is the place where such options may be added in the future.
 *
 * <p>This class is <strong>not</strong> thread-safe. A new instance shall be created
 * for each thread applying optimizations. Example:</p>
 *
 * {@preformat java
 *     Filter<R> filter = ...;
 *     filter = new Optimization().apply(filter);
 * }
 *
 * <h2>How optimizations are applied</h2>
 * Optimizations are specific to each expression and filter type.
 * For optimizations to happen, classes must implement the {@link OnExpression} or {@link OnFilter} interface.
 * The {@link #apply(Filter)} and {@link #apply(Expression)} methods in this {@code Optimization} class merely
 * delegate to the methods defined in above-cited interfaces, with safety guards against infinite recursivity.
 *
 * <h2>Behavioral changes</h2>
 * Optimized filters shall produce the same results than non-optimized filters.
 * However side-effects may differ, in particular regarding exceptions that may be thrown.
 * For example if a filter tests {@code A & B} and if {@code Optimization} determines that the {@code B}
 * condition will always evaluate to {@code false}, then the {@code A} condition will never be tested.
 * If that condition had side-effects or threw an exception,
 * those effects will disappear in the optimized filter.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class Optimization {
    /**
     * An arbitrary object meaning that a filter or expression optimization in under progress.
     */
    private static final Object COMPUTING = Void.TYPE;

    /**
     * Filters and expressions already optimized. Also used for avoiding never-ending loops.
     * The map is created when first needed.
     *
     * <div class="note"><b>Note:</b> the same map is used for filters and expressions.
     * It is not a problem if keys do not implement the two interfaces in same time.
     * If it happens anyway, it should still be okay because the method signatures are
     * the same in both interfaces (only the return type changes), so the same methods
     * would be invoked no matter if we consider the keys as a filter or an expression.</div>
     */
    private Map<Object,Object> done;

    /**
     * Creates a new instance.
     */
    public Optimization() {
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
    public <R> Filter<? super R> apply(final Filter<R> filter) {
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
            Filter<? super R> result = (Filter<? super R>) previous;
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
     * Filter than can be optimized. Each filter implementation knows which rules can be applied.
     * For that reason, the optimization algorithms are keep in each implementation class.
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
        default Filter<? super R> optimize(final Optimization optimization) {
            final List<Expression<? super R, ?>> expressions = getExpressions();
            @SuppressWarnings({"unchecked", "rawtypes"})
            final Expression<? super R, ?>[] effective = new Expression[expressions.size()];
            boolean unchanged = true;       // Will be `false` if at least one optimization has been applied.
            boolean immediate = true;
            for (int i=0; i<effective.length; i++) {
                Expression<? super R, ?> e = expressions.get(i);
                unchanged &= (e == (e = optimization.apply(e)));
                immediate &= (e instanceof Literal<?,?>);
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
        default Filter<R> recreate(Expression<? super R, ?>[] effective) {
            return this;
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
    public <R,V> Expression<? super R, ? extends V> apply(final Expression<R,V> expression) {
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
            Expression<? super R, ? extends V> result = (Expression<? super R, ? extends V>) previous;
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
     * Expression than can be optimized. Each expression implementation knows which rules can be applied.
     * For that reason, the optimization algorithms are keep in each implementation class.
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
         *   <li>If all expressions are literals, evaluate this expression immediately.</li>
         *   <li>Otherwise if at least one parameter can be optimized,
         *       {@linkplain #recreate(Expression[]) recreate} the expression.</li>
         *   <li>Otherwise returns {@code this}.</li>
         * </ul>
         *
         * @param  optimization  the simplifications or optimizations to apply on this expression.
         * @return the simplified or optimized expression, or {@code this} if no optimization has been applied.
         */
        default Expression<? super R, ? extends V> optimize(final Optimization optimization) {
            final List<Expression<? super R, ?>> parameters = getParameters();
            @SuppressWarnings({"unchecked", "rawtypes"})
            final Expression<? super R, ?>[] effective = new Expression[parameters.size()];
            boolean unchanged = true;       // Will be `false` if at least one optimization has been applied.
            boolean immediate = true;
            for (int i=0; i<effective.length; i++) {
                Expression<? super R, ?> e = parameters.get(i);
                unchanged &= (e == (e = optimization.apply(e)));
                immediate &= (e instanceof Literal<?,?>);
            }
            if (immediate) {
                return new LeafExpression.Literal<>(apply(null));
            } else if (unchanged) {
                return this;
            } else {
                return recreate(effective);
            }
        }

        /**
         * Creates a new expression of the same type than this expression, but with optimized parameters.
         * The expressions given to this method shall be equivalent to the expressions used by this instance,
         * potentially more efficient.
         *
         * <p>This method is used by the default implementation of {@link #optimize(Optimization)}
         * and can be ignored if above method is overridden.</p>
         *
         * @param  effective  the expressions to use as a replacement of this expression parameters.
         * @return the new expression, or {@code this} if unsupported.
         */
        default Expression<R,V> recreate(Expression<? super R, ?>[] effective) {
            return this;
        }
    }
}
