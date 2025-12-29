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

import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import org.opengis.util.CodeList;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.util.ScopedName;


/**
 * Optimization of a filter or expression for feature types that are discovered when the filter or expression
 * is executed. This is needed when a feature type may have subtypes, because it may contain properties that
 * are unknown to the base type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <T>  either {@link Filter} or {@link Expression}.
 */
abstract class DynamicOptimization<T> {
    /**
     * The filters or expressions that have already been optimized.
     */
    private final Map<FeatureType, T> cache;

    /**
     * Creates a new dynamic optimization.
     */
    protected DynamicOptimization() {
        cache = new IdentityHashMap<>();
    }

    /**
     * Returns the filter or expression to use for executing this operation
     * on feature instances of the given type.
     *
     * <h4>Implementation note</h4>
     * We could use {@link java.util.concurrent.ConcurrentHashMap} instead of synchronization,
     * but the map is expected to be very small, often with only one element. For such small maps,
     * the additional complexity of concurrent maps may be greater than the synchronization cost.
     * Furthermore, subclasses have an optimization which will avoid this synchronization in the
     * common case where consecutive feature instances have the same feature type.
     *
     * @param  type  type of the feature instance on which this operation will be executed.
     * @return the filter or expression to use for feature instances of the given type.
     */
    protected final T delegate(final FeatureType type) {
        synchronized (cache) {
            return cache.computeIfAbsent(type, (key) -> {
                var optimizer = new Optimization();
                optimizer.setFinalFeatureType(key);
                return optimizeOriginal(optimizer);
            });
        }
    }

    /**
     * Optimizes the original filter or expressions using the given setting.
     *
     * @param  optimizer  the optimization to apply on this filter or expression.
     * @return the optimized filter or expression, or {@link #original} if no change.
     */
    protected abstract T optimizeOriginal(final Optimization optimizer);

    /**
     * Returns a dynamic optimization of the given filter.
     * If dynamic optimization is already enabled, then the filter is returned as-is.
     *
     * @param  original  the filter for which to provide dynamic optimization.
     * @return filter equivalent to the given filter, but with dynamic optimization enabled.
     */
    public static Filter<Feature> of(final Filter<Feature> original) {
        if (original instanceof OfFilter) {
            return original;
        }
        return new OfFilter(original);
    }

    /**
     * Returns a dynamic optimization of the given filter if that filter accepts feature instances.
     * If dynamic optimization is already enabled or is not applicable, then the filter is returned as-is.
     *
     * @param  <R>       type of resources accepted by the filter.
     * @param  original  the filter for which to provide dynamic optimization.
     * @return filter equivalent to the given filter, but with dynamic optimization enabled.
     */
    @SuppressWarnings("unchecked")
    public static <R> Filter<R> ofAny(final Filter<R> original) {
        if (original.getResourceClass().isAssignableFrom(Feature.class)) {
            return of((Filter) original);
        }
        return original;
    }

    /**
     * Dynamic optimization of filters.
     */
    private static final class OfFilter extends DynamicOptimization<Filter<Feature>>
            implements Optimization.OnFilter<Feature>
    {
        /**
         * The original (without optimization) filter.
         */
        private final Filter<Feature> original;

        /**
         * Most recently used entry of {@link #cache}. This is an optimization for the
         * common case where many consecutive features are instances of the same type.
         * There is no need for {@code volatile} keyword if all fields are final.
         */
        private Entry last;

        /**
         * A thread-safe (type, filter) entry. Thread-safety is achieved by making sure that all fields are final.
         * The declared type of {@link #delegate} is {@code Filter} rather than {@code <T>} for reducing the number
         * of casts.
         */
        private static final class Entry {
            /** The type of feature instances expected by {@link #delegate}. */
            final FeatureType type;

            /** The actual filter to apply on features instances. */
            final Filter<Feature> delegate;

            /** Associates the actual filter to feature instances of the given type. */
            Entry(final FeatureType type, final Filter<Feature> delegate) {
                this.type = type;
                this.delegate = delegate;
            }
        }

        /**
         * Creates a new dynamic optimization.
         *
         * @param  original  the original (without optimization) filter.
         */
        OfFilter(final Filter<Feature> original) {
            this.original = original;
            last = new Entry(null, original);
        }

        /**
         * Optimizes the original filter using the given setting.
         * The target feature type shall be set by the caller before to invoke this method.
         *
         * @param  optimizer  the optimization to apply on the original filter.
         * @return the optimized filter, or {@link #original} if no change.
         */
        @Override
        protected Filter<Feature> optimizeOriginal(final Optimization optimizer) {
            return optimizer.apply(original);
        }

        /**
         * Optimizes this filter using the given setting.
         *
         * @param  optimizer  the optimization to apply on this filter.
         * @return the optimized filter, or {@code this} if no change.
         */
        @Override
        public Filter<Feature> optimize(final Optimization optimizer) {
            var result = optimizeOriginal(optimizer);
            return (result != original) ? of(result) : this;
        }

        /**
         * Filters the given feature instance by delegating to a filter optimized for the actual feature type.
         *
         * @param  feature  the feature instance to test.
         * @return whether the given feature instance is accepted.
         */
        @Override
        public boolean test(final Feature feature) {
            final FeatureType type = feature.getType();
            Entry entry = last;
            if (entry.type != type) {
                last = entry = new Entry(type, delegate(type));
            }
            return entry.delegate.test(feature);
        }

        /** Delegates to the original filter. */
        @Override public CodeList<?>                  getOperatorType()  {return original.getOperatorType();}
        @Override public Class<? super Feature>       getResourceClass() {return original.getResourceClass();}
        @Override public List<Expression<Feature, ?>> getExpressions()   {return original.getExpressions();}
        @Override public String                       toString()         {return original.toString();}
    }

    /**
     * Returns a dynamic optimization of the given expression.
     * If dynamic optimization is already enabled, then the expression is returned as-is.
     *
     * @param  original  the expression for which to provide dynamic optimization.
     * @return expression equivalent to the given expression, but with dynamic optimization enabled.
     */
    public static <V> Expression<Feature, V> of(final Expression<Feature, V> original) {
        if (original instanceof OfExpression<?>) {
            return original;
        }
        return new OfExpression<>(original);
    }

    /**
     * Returns a dynamic optimization of the given expression if that expression accepts feature instances.
     * If dynamic optimization is already enabled or is not applicable, then the expression is returned as-is.
     *
     * @param  <R>       type of resources accepted by the expression.
     * @param  <V>       type of values returned by the expression.
     * @param  original  the expression for which to provide dynamic optimization.
     * @return expression equivalent to the given expression, but with dynamic optimization enabled.
     */
    @SuppressWarnings("unchecked")
    public static <R,V> Expression<R,V> ofAny(final Expression<R,V> original) {
        if (original.getResourceClass().isAssignableFrom(Feature.class)) {
            return of((Expression) original);
        }
        return original;
    }

    /**
     * Dynamic optimization of expressions.
     *
     * @param  <V>  the type of values computed by the expression.
     */
    private static final class OfExpression<V> extends DynamicOptimization<Expression<Feature, ? extends V>>
            implements Optimization.OnExpression<Feature, V>
    {
        /**
         * The original (without optimization) expression.
         */
        private final Expression<Feature, ? extends V> original;

        /**
         * Most recently used entry of {@link #cache}. This is an optimization for the
         * common case where many consecutive features are instances of the same type.
         * There is no need for {@code volatile} keyword if all fields are final.
         */
        private Entry<V> last;

        /**
         * A thread-safe (type, expression) entry. Thread-safety is achieved by making sure that all fields are final.
         * The declared type of {@link #delegate} is {@code Expression} rather than {@code <T>} for reducing the number
         * of casts.
         */
        private static final class Entry<V> {
            /** The type of feature instances expected by {@link #delegate}. */
            final FeatureType type;

            /** The actual expression to apply on features instances. */
            final Expression<Feature, ? extends V> delegate;

            /** Associates the actual expression to feature instances of the given type. */
            Entry(final FeatureType type, final Expression<Feature, ? extends V> delegate) {
                this.type = type;
                this.delegate = delegate;
            }
        }

        /**
         * Creates a new dynamic optimization.
         *
         * @param  original  the expression for which to provide dynamic optimization.
         */
        OfExpression(final Expression<Feature, ? extends V> original) {
            this.original = original;
            last = new Entry<>(null, original);
        }

        /**
         * Optimizes the original expressions using the given setting.
         * The target feature type shall be set by the caller before to invoke this method.
         *
         * @param  optimizer  the optimization to apply on the original expression.
         * @return the optimized expression, or {@link #original} if no change.
         */
        @Override
        protected Expression<Feature, ? extends V> optimizeOriginal(final Optimization optimizer) {
            return optimizer.apply(original);
        }

        /**
         * Optimizes this expressions using the given setting.
         *
         * @param  optimizer  the optimization to apply on this expression.
         * @return the optimized expression, or {@code this} if no change.
         */
        @Override
        public Expression<Feature, ? extends V> optimize(final Optimization optimizer) {
            var result = optimizeOriginal(optimizer);
            return (result != original) ? of(result) : this;
        }

        /**
         * Evaluates the given feature instance by delegating to an expression optimized for the actual feature type.
         *
         * @param  feature  the feature instance to evaluate.
         * @return the value evaluated from the given feature.
         */
        @Override
        public V apply(final Feature feature) {
            final FeatureType type = feature.getType();
            Entry<V> entry = last;
            if (entry.type != type) {
                last = entry = new Entry<>(type, delegate(type));
            }
            return entry.delegate.apply(feature);
        }

        /**
         * Returns an expression doing the same evaluation as this method, but returning results
         * as values of the specified type.
         *
         * @param  <N>     compile-time value of {@code target} type.
         * @param  target  desired type of expression results.
         * @return expression doing the same operation this this expression but with results of the specified type.
         * @throws ClassCastException if the specified type is not a target type supported by implementation.
         */
        @Override
        @SuppressWarnings("unchecked")
        public <N> Expression<Feature, N> toValueType(Class<N> target) {
            Expression<Feature, N> other = original.toValueType(target);
            return (other != original) ? of(other) : (Expression<Feature, N>) this;
        }

        /** Delegates to the original expression. */
        @Override public ScopedName                   getFunctionName()  {return original.getFunctionName();}
        @Override public Class<? super Feature>       getResourceClass() {return original.getResourceClass();}
        @Override public List<Expression<Feature, ?>> getParameters()    {return original.getParameters();}
        @Override public String                       toString()         {return original.toString();}
    }
}
