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
package org.apache.sis.feature;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.function.Function;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.filter.internal.shared.FunctionNames;
import org.apache.sis.filter.internal.shared.Visitor;

// Specific to the main branch:
import org.apache.sis.filter.Filter;
import org.apache.sis.filter.Expression;
import org.apache.sis.pending.geoapi.filter.LogicalOperator;
import org.apache.sis.pending.geoapi.filter.ValueReference;


/**
 * A feature property which is an operation implemented by a filter expression.
 * This operation computes expression results from given feature instances only,
 * there is no parameters.
 *
 * @author  Johann Sorel (Geomatys)
 *
 * @param  <V>  class of values computed by the operation.
 */
final class ExpressionOperation<V> extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5411697964136428848L;

    /**
     * The parameter descriptor for the "Expression" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup PARAMETERS = parameters("Expression");

    /**
     * The expression to which to delegate the execution of this operation.
     */
    @SuppressWarnings("serial")                         // Not statically typed as serializable.
    final Function<? super AbstractFeature, ? extends V> expression;

    /**
     * The type of result of evaluating the expression.
     */
    private final DefaultAttributeType<V> resultType;

    /**
     * The name of all feature properties that are known to be read by the expression.
     * This is determined by execution of {@link #VISITOR} on the {@linkplain #expression}.
     * This set may be incomplete if some properties are read otherwise than by {@link ValueReference}.
     */
    @SuppressWarnings("serial")                         // Set.of(…) implementations are serializable.
    private final Set<String> dependencies;

    /**
     * Creates a new operation which will delegate execution to the given expression.
     *
     * @param identification  the name of the operation, together with optional information.
     * @param expression      the expression to evaluate on feature instances.
     * @param resultType      type of values computed by the expression.
     */
    static <V> AbstractOperation create(final Map<String,?> identification,
                                        final Function<? super AbstractFeature, ? extends V> expression,
                                        final DefaultAttributeType<? super V> resultType)
    {
        if (expression instanceof ValueReference<?,?>) {
            final String xpath = ((ValueReference<?,?>) expression).getXPath();
            if (xpath.equals(resultType.getName().toString())) {
                return new LinkOperation(identification, resultType);
            }
        }
        return new ExpressionOperation<>(identification, expression, resultType);
    }

    /**
     * Creates a generic operation when no optimized case has been identifier.
     */
    private ExpressionOperation(final Map<String,?> identification,
                                final Function<? super AbstractFeature, ? extends V> expression,
                                final DefaultAttributeType<V> resultType)
    {
        super(identification);
        this.expression = expression;
        this.resultType = resultType;
        if (expression instanceof Expression<?,?>) {
            @SuppressWarnings("unchecked")
            var c = (Expression<AbstractFeature,?>) expression;     // Cast is okay because we will not pass or request any `Feature` instance.
            dependencies = DependencyFinder.search(c);
        } else {
            dependencies = Set.of();
        }
    }

    /**
     * Returns a description of the input parameters.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return PARAMETERS;
    }

    /**
     * Returns the expected result type.
     */
    @Override
    public AbstractIdentifiedType getResult() {
        return resultType;
    }

    /**
     * Returns the names of feature properties that this operation needs for performing its task.
     * This set may be incomplete if some properties are read otherwise than by {@link ValueReference}.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Because the set is unmodifiable.
    public Set<String> getDependencies() {
        return dependencies;
    }

    /**
     * Returns the value computed by the expression for the given feature instance.
     *
     * @param  feature     the feature to evaluate with the expression.
     * @param  parameters  ignored (can be {@code null}).
     * @return the computed property from the given feature.
     */
    @Override
    public Property apply(final AbstractFeature feature, ParameterValueGroup parameters) {
        return new Result(feature);
    }

    /**
     * The attributes that delegates computation to the expression.
     * Value is calculated each time it is accessed.
     */
    private final class Result extends OperationResult<V> {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -19004252522001532L;

        /**
         * Creates a new attribute for the given feature.
         */
        Result(final AbstractFeature feature) {
            super(resultType, feature);
        }

        /**
         * Delegates the computation to the user supplied expression.
         */
        @Override
        public V getValue() {
            return expression.apply(feature);
        }
    }

    /**
     * Computes a hash-code value for this operation.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + expression.hashCode();
    }

    /**
     * Compares this operation with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        /*
         * `this.result` is compared (indirectly) by the super class.
         * `this.dependencies` does not need to be compared because it is derived from `expression`.
         */
        return super.equals(obj) && expression.equals(((ExpressionOperation) obj).expression);
    }

    /**
     * An expression visitor for finding all dependencies of a given expression.
     * The dependencies are feature properties read by {@link ValueReference} nodes.
     */
    private static final class DependencyFinder extends Visitor<AbstractFeature, Collection<String>> {
        /**
         * The unique instance.
         */
        private static final DependencyFinder VISITOR = new DependencyFinder();

        /**
         * Returns all dependencies read by a {@link ValueReference} node.
         *
         * @param  expression  the expression for which to get dependencies.
         * @return all dependencies recognized by this method.
         */
        static Set<String> search(final Expression<AbstractFeature,?> expression) {
            final Set<String> dependencies = new HashSet<>();
            VISITOR.visit(expression, dependencies);
            return Set.copyOf(dependencies);
        }

        /**
         * Constructor for the unique instance.
         */
        private DependencyFinder() {
            setLogicalHandlers((f, dependencies) -> {
                final var filter = (LogicalOperator<AbstractFeature>) f;
                for (Filter<AbstractFeature> child : filter.getOperands()) {
                    visit(child, dependencies);
                }
            });
            setExpressionHandler(FunctionNames.ValueReference, (e, dependencies) -> {
                final var expression = (ValueReference<AbstractFeature,?>) e;
                final String propName = expression.getXPath();
                if (!propName.trim().isEmpty()) {
                    dependencies.add(propName);
                }
            });
        }

        /**
         * Fallback for all filters not explicitly handled by the setting applied in the constructor.
         */
        @Override
        protected void typeNotFound(final Enum<?> type, final Filter<AbstractFeature> filter, final Collection<String> dependencies) {
            for (final Expression<AbstractFeature,?> f : filter.getExpressions()) {
                visit(f, dependencies);
            }
        }

        /**
         * Fallback for all expressions not explicitly handled by the setting applied in the constructor.
         */
        @Override
        protected void typeNotFound(final String type, final Expression<AbstractFeature,?> expression, final Collection<String> dependencies) {
            for (final Expression<AbstractFeature,?> p : expression.getParameters()) {
                visit(p, dependencies);
            }
        }
    }
}
