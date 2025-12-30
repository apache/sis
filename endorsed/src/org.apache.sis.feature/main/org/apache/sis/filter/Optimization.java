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
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.BiPredicate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.feature.Features;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.filter.base.Node;
import org.apache.sis.filter.base.WarningEvent;

// Specific to the main branch:
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.pending.geoapi.filter.Literal;
import org.apache.sis.pending.geoapi.filter.LogicalOperator;
import org.apache.sis.pending.geoapi.filter.LogicalOperatorName;
import org.apache.sis.pending.geoapi.filter.ValueReference;


/**
 * Description of optimizations or simplifications to attempt on filters and expressions.
 * Optimizations can include the following changes:
 *
 * <ul>
 *   <li>Application of some logical identities such as {@code NOT(NOT(A)) == A}.</li>
 *   <li>Application of logical short circuits such as {@code A & FALSE == FALSE}.</li>
 *   <li>Replacement of value references to non-existent properties by null literals.</li>
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
 * For example, if a filter tests {@code A & B} and if {@code Optimization} determines that the {@code B}
 * condition will always evaluate to {@code false}, then the {@code A} condition will never be tested.
 * If that condition had side-effects or threw an exception,
 * those effects will disappear in the optimized filter.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   1.1
 */
public class Optimization {
    /**
     * An arbitrary object meaning that a filter or expression optimization is under progress.
     */
    private static final Object COMPUTING = Void.TYPE;

    /**
     * Exhaustive set of types of all feature instances that the filters and expressions may see.
     * This is an empty set if the feature types are unknown or irrelevant for the type of resources to be filtered.
     */
    private Set<DefaultFeatureType> featureTypes;

    /**
     * Filters and expressions already optimized. Also used for avoiding never-ending loops.
     * The map is created when first needed. The null value (not the same as an empty map) is
     * used for identifying the start of recursive invocations of {@code apply(…)} methods.
     *
     * <h4>Implementation note</h4>
     * The same map is used for filters and expressions.
     * It is not a problem if keys do not implement the two interfaces at the same time.
     * If it happens anyway, it should still be okay because the method signatures are
     * the same in both interfaces (only the return type changes), so the same methods
     * would be invoked no matter if we consider the keys as a filter or an expression.
     *
     * @see #apply(Filter)
     * @see #apply(Expression)
     */
    private Map<Object, Object> done;

    /**
     * The filter or expression in process of being optimized.
     * This is used by {@link #warning(Exception)} for reporting the source of errors.
     */
    private Object currentFilterOrExpression;

    /**
     * Set during the execution of an {@code apply(…)} method if it could do better.
     * If {@code true}, then invoking {@code apply(…)} again with, for example, a single
     * {@link #setFinalFeatureType final feature type} may improve the result efficiency.
     * Conversely, a value of {@code false} means that no improvement is expected.
     *
     * @see #apply(Filter)
     * @see #apply(Expression)
     * @see #warning(Exception, boolean)
     * @see #isImprovable()
     */
    private boolean isImprovable;

    /**
     * Creates a new instance.
     */
    public Optimization() {
        featureTypes = Set.of();
    }

    /**
     * Returns the type of feature instances to be filtered, or {@code null} if unknown.
     * This is the last value specified by a call to {@link #setFeatureType(DefaultFeatureType)}.
     * The default value is {@code null}.
     *
     * @return the type of feature instances to be filtered, or {@code null} if unknown.
     *
     * @deprecated Replaced by {@link #getFinalFeatureTypes()}.
     */
    @Deprecated(since = "1.6", forRemoval = true)
    public DefaultFeatureType getFeatureType() {
        return Containers.peekIfSingleton(getFinalFeatureTypes());
    }

    /**
     * Sets the type of feature instances to be filtered.
     * If this type is known in advance, specifying it may allow to compute more specific
     * {@link org.apache.sis.util.ObjectConverter}s or to apply some geometry reprojection
     * in advance.
     *
     * @param  type  the type of feature instances to be filtered, or {@code null} if unknown.
     *
     * @deprecated Replaced by {@link #setFinalFeatureTypes(Collection)}.
     */
    @Deprecated(since = "1.6", forRemoval = true)
    public void setFeatureType(final DefaultFeatureType type) {
        setFinalFeatureType(type);
    }

    /**
     * Returns the exhaustive set of the types of all feature instances that the filters and expressions may see.
     * The super-types should not be included in the set, unless some features may be instances of these specific
     * super-types rather than instances of a some sub-type. If the set of feature types is unknown or irrelevant
     * for the type of resources to be filtered, then this method returns an empty set.
     *
     * <h4>Purpose</h4>
     * A {@link org.apache.sis.storage.DataStore} may contain a hierarchy of feature types instead of a single type.
     * A property may be absent in the parent type but present in some sub-types, or may be overridden in sub-types.
     * If an optimization wants to evaluate once and for all an expression with literal parameters, the optimization
     * needs to verify that the parameters are really literals in all possible sub-types.
     *
     * @return exhaustive set of types of all feature instances that the filters and expressions may see.
     *
     * @since 1.6
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Set<DefaultFeatureType> getFinalFeatureTypes() {
        return featureTypes;
    }

    /**
     * Specifies the exhaustive set of the types of all feature instances that the filters and expressions may see.
     * The given collection should not include super-types, unless some features may be instances of these specific
     * super-types rather than instances of a some sub-type. An empty collection means that feature types are unknown
     * or irrelevant for the type of resources to be filtered.
     *
     * @param  types  exhaustive set of types of all feature instances that the filters and expressions may see.
     *
     * @since 1.6
     */
    public void setFinalFeatureTypes(final Collection<? extends DefaultFeatureType> types) {
        featureTypes = Set.copyOf(types);
    }

    /**
     * Specifies the single type of all feature instances that the filters and expressions may see.
     * This is a convenience method delegating to {@link #setFinalFeatureTypes(Collection)} with a
     * singleton or empty set.
     *
     * <p>Note that the given type should be effectively final, i.e. no subtype should exist.
     * If the feature instances may be of some subtypes, then all subtypes should be enumerated
     * in a call to {@link #setFinalFeatureTypes(Collection)}.</p>
     *
     * @param  type  the type of feature instances to be filtered, or {@code null} if unknown.
     *
     * @since 1.6
     */
    public final void setFinalFeatureType(final DefaultFeatureType type) {
        setFinalFeatureTypes((type != null) ? Set.of(type) : Set.of());
    }

    /**
     * If the result of applying the given function is equal for all feature types, returns that value.
     * Otherwise, returns {@code null}. This is used for implementation of {@code optimize(…)} methods.
     *
     * @param  <R>     type of the result.
     * @param  mapper  the operation to apply on each feature type.
     * @return the constant result, or {@code null} if none.
     */
    final <R> R constantResultForAllTypes(final Function<DefaultFeatureType, R> mapper) {
        final Iterator<DefaultFeatureType> it = getFinalFeatureTypes().iterator();
        if (it.hasNext()) {
            final R value = mapper.apply(it.next());
            if (value != null) {
                while (it.hasNext()) {
                    if (!value.equals(mapper.apply(it.next()))) {
                        return null;
                    }
                }
                return value;
            }
        }
        return null;
    }

    /**
     * Fetches the real name of the given property after resolution of links in all feature types.
     * The real name depends on the feature types declared by {@link #getFinalFeatureTypes()}.
     * If the specified property is present in all declared feature types and all these properties
     * are links referencing the same target property, then this method returns that target property.
     * Otherwise, this method returns {@code property}.
     *
     * <p>If at least one feature type does not have the requested property, then an exception is thrown.
     * This method finished the iteration over all types before to throw {@link IllegalArgumentException}.
     * Therefore, the size of {@code addTo} can be used for detecting if at least one feature type has the
     * property. If {@code addTo} is empty, then the property has not been found in any feature type.</p>
     *
     * @param  property  name of the property to resolve.
     * @param  addTo     where to add the XPath of the specified property for all feature types.
     * @return preferred name to use for fetching the property values for all feature types.
     * @throws IllegalArgumentException if at least one feature type does not have the specified property.
     */
    @SuppressWarnings("ThrowableResultIgnored")
    final String getPreferredPropertyName(final String property, final Set<String> addTo) throws IllegalArgumentException {
        final var exceptions = new HashMap<DefaultFeatureType, IllegalArgumentException>();
        for (final DefaultFeatureType type : getFinalFeatureTypes()) {
            try {
                addTo.add(Features.getLinkTarget(type.getProperty(property)).orElse(property));
            } catch (IllegalArgumentException e) {
                exceptions.putIfAbsent(type, e);
            }
        }
        if (exceptions.isEmpty()) {
            final String name = Containers.peekIfSingleton(addTo);
            return (name != null) ? name : property;
        }
        /*
         * Throws the exception associated with the most basic feature type.
         * The base type search is not mandatory, but provide more useful stack trace.
         */
        final IllegalArgumentException e = valueOfBaseType(exceptions);
        while (!exceptions.isEmpty()) {
            e.addSuppressed(valueOfBaseType(exceptions));
        }
        throw e;
    }

    /**
     * Returns the value associated to the base type among all keys of the given map.
     * If no base type is found, then an arbitrary entry is used.
     * This method always removes exactly one entry from the map.
     */
    private static <E> E valueOfBaseType(final Map<DefaultFeatureType, E> map) {
        E e = map.remove(Features.findCommonParent(map.keySet()));
        if (e == null) {
            Iterator<E> it = map.values().iterator();
            e = it.next();
            it.remove();
        }
        return e;
    }

    /**
     * If the specified parameter should always use the same Coordinate Reference System, returns that <abbr>CRS</abbr>.
     * The {@code parameter} argument is usually one of the elements returned by {@link Expression#getParameters()} or
     * {@link Filter#getExpressions()}, and the <abbr>CRS</abbr> used by that parameter may depend on the feature types
     * declared by {@link #getFinalFeatureTypes()}.
     * The returned value is empty if the <abbr>CRS</abbr> is unknown or not the same for all feature types.
     *
     * @param  parameter  a parameter of a filter or expression.
     * @return the <abbr>CRS</abbr> expected for the specified parameter, or empty if unknown of not unique.
     * @throws IllegalArgumentException if the parameter is a {@link ValueReference} and
     *         the referenced property has not been found in at least one feature type.
     *
     * @since 1.6
     */
    public Optional<CoordinateReferenceSystem> findExpectedCRS(final Expression<?,?> parameter)
            throws IllegalArgumentException
    {
        CoordinateReferenceSystem crs = null;
        if (parameter instanceof Literal<?,?>) {
            crs = Geometries.getCoordinateReferenceSystem(((Literal<?,?>) parameter).getValue());
        } else if (parameter instanceof ValueReference<?,?>) {
            final String xpath = ((ValueReference<?,?>) parameter).getXPath();
            crs = constantResultForAllTypes(
                    (type) -> AttributeConvention.getCRSCharacteristic(type, type.getProperty(xpath)));
        }
        return Optional.ofNullable(crs);
    }

    /**
     * Returns whether an {@code apply(…)} method may obtain a better result with dynamic optimization.
     * A value of {@code true} does not necessarily mean that {@link DynamicOptimization} will be created,
     * because {@code DynamicOptimization.ofAny(…)} will check if the resource type is {@code Feature}.
     *
     * @param  source         the filter or expression to test if it can be improved.
     * @param  isOptimizable  one of the {@code isOptimizable(…)} methods.
     * @return whether it is suggested to use {@link DynamicOptimization}.
     */
    private <T> boolean isImprovable(final T source, final BiPredicate<T, Set<Object>> isOptimizable) {
        if (isImprovable || featureTypes.isEmpty()) {
            return isOptimizable.test(source, new HashSet<>());
        }
        return false;
    }

    /**
     * Verifies whether the given filter can be optimized.
     *
     * @param  filter  the filter to test.
     * @param  done    an initially empty set used as a safety against infinite recursion.
     * @return whether the given filter can be optimized.
     */
    private static boolean isOptimizable(final Filter<?> filter, final Set<Object> done) {
        if (filter instanceof OnFilter<?>) {
            return true;
        }
        for (Expression<?,?> parameter : filter.getExpressions()) {
            if (done.add(parameter) && isOptimizable(parameter, done)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifies whether the given expression can be optimized.
     *
     * @param  expression  the expression to test.
     * @param  done        an initially empty set used as a safety against infinite recursion.
     * @return whether the given expression can be optimized.
     */
    private static boolean isOptimizable(final Expression<?,?> expression, final Set<Object> done) {
        if (expression instanceof OnExpression<?,?>) {
            return true;
        }
        for (Expression<?,?> parameter : expression.getParameters()) {
            if (done.add(parameter) && isOptimizable(parameter, done)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether a call to {@code apply(…)} is the first of possibly recursive calls.
     * This method shall be invoked in all {@code apply(…)} methods before to do the optimization.
     *
     * @return whether this is the entry (non-recursive) call to an {@code apply(…)} method.
     */
    private boolean isFirstCall() {
        if (done != null) {
            return false;
        }
        isImprovable = false;
        done = new IdentityHashMap<>();
        return true;
    }

    /**
     * Returns the previous optimization result for the given filter or expression.
     * The {@link #isFirstCall()} method must have been invoked before this method.
     *
     * @param  original        the filter or expression to optimize.
     * @param  identification  method to invoke for getting an identification of the filter or expression.
     * @return the previous value, or {@code null} if none.
     * @throws IllegalArgumentException if a recursive call is detected for the same filter or expression.
     */
    private <T> Object previousResult(final T original, final Function<T, Object> identification) {
        currentFilterOrExpression = original;
        final Object previous = done.putIfAbsent(original, COMPUTING);
        if (previous != COMPUTING) {
            return previous;
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.RecursiveCreateCallForKey_1, identification.apply(original)));
    }

    /**
     * Stores the result of an optimization.
     *
     * @param original  the filter or expression to optimize.
     * @param result    the optimization result.
     */
    private void storeResult(final Object original, final Object result) {
        if (done.put(original, result) != COMPUTING) {
            // Should not happen unless this `Optimization` is used concurrently in many threads.
            throw new ConcurrentModificationException();
        }
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
    @SuppressWarnings("unchecked")
    public <R> Filter<R> apply(Filter<R> filter) {
        if (filter instanceof OnFilter<?>) {
            final var original = (OnFilter<R>) filter;
            final boolean isFirstCall = isFirstCall();
            final Object oldFilterOrExpression = currentFilterOrExpression;
            try {
                filter = (Filter<R>) previousResult(original, Filter::getOperatorType);
                if (filter == null) {
                    filter = original.optimize(this);
                    storeResult(original, filter);
                }
            } finally {
                currentFilterOrExpression = oldFilterOrExpression;
                if (isFirstCall) {
                    done = null;
                }
            }
            if (isFirstCall && isImprovable(filter, Optimization::isOptimizable)) {
                filter = DynamicOptimization.ofAny(filter);
            }
        }
        return filter;
    }

    /**
     * Filter that can be optimized. Each filter implementation knows which rules can be applied.
     * For that reason, the optimization algorithms are kept in each implementation class.
     * This interface allows {@link Optimization} to invoke that code.
     *
     * <p>Implementations need to override only one of the 2 methods defined in this interface.</p>
     *
     * @param  <R>  the type of resources to filter.
     *
     * @version 1.6
     * @since   1.1
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
    @SuppressWarnings("unchecked")
    public <R,V> Expression<R, ? extends V> apply(Expression<R, ? extends V> expression) {
        if (expression instanceof OnExpression<?,?>) {
            final var original = (OnExpression<R, ? extends V>) expression;
            final boolean isFirstCall = isFirstCall();
            final Object oldFilterOrExpression = currentFilterOrExpression;
            try {
                expression = (Expression<R, ? extends V>) previousResult(original, Expression::getFunctionName);
                if (expression == null) {
                    expression = original.optimize(this);
                    storeResult(original, expression);
                }
                if (isFirstCall && isImprovable(expression, Optimization::isOptimizable)) {
                    expression = DynamicOptimization.ofAny(expression);
                }
            } finally {
                currentFilterOrExpression = oldFilterOrExpression;
                if (isFirstCall) {
                    done = null;
                }
            }
        }
        return expression;
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
     *
     * @version 1.6
     * @since   1.1
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
        final Enum<?> type = filter.getOperatorType();
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
        final Filter<R> operand = Containers.peekIfSingleton(((LogicalOperator<R>) filter).getOperands());
        if (operand != null) {
            return operand;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Reports that a warning occurred during the execution of an {@code apply(…)} method.
     * This method can be invoked by implementations of {@link OnFilter} or {@link OnExpression} interfaces.
     * The exception if often a {@link IllegalArgumentException}, which is not necessarily an error because
     * a property may not exist in a base feature type but exists in some sub-types.
     *
     * <p>If the {@code resolvable} flag is {@code true}, it will be taken as a hint to retry the optimization
     * later if more information about the {@link #getFinalFeatureTypes() final feature types} become available.
     * This flag should be {@code false} if more information would not have a direct impact on the optimization
     * done by the caller. Callers do not need to care about indirect impacts in the parameters of the filter or
     * expression.</p>
     *
     * @param exception   the recoverable exception that occurred.
     * @param resolvable  whether an optimization with more information may avoid this warning.
     *
     * @since 1.6
     */
    public void warning(Exception exception, boolean resolvable) {
        isImprovable |= resolvable;
        final Consumer<WarningEvent> listener = WarningEvent.LISTENER.get();
        if (listener != null) {
            listener.accept(new WarningEvent(currentFilterOrExpression, exception, true));
        } else {
            Logging.recoverableException(Node.LOGGER, Optimization.class, "apply", exception);
        }
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
    public static <R,V> Expression<R,V> literal(final V value) {
        if (value == null) {
            return LeafExpression.NULL();
        }
        return new LeafExpression.Literal<>(value);
    }
}
