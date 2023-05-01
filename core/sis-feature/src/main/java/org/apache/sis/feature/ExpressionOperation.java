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
import org.opengis.util.CodeList;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.internal.feature.FeatureUtilities;
import org.apache.sis.internal.filter.FunctionNames;
import org.apache.sis.internal.filter.Visitor;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.IdentifiedType;
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.ValueReference;


/**
 * A feature property which is an operation implemented by a filter expression.
 * This operation computes expression results from given feature instances only,
 * there is no parameters.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.4
 * @since   1.4
 */
final class ExpressionOperation<V> extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5411697964136428848L;

    /**
     * The parameter descriptor for the "Expression" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup PARAMETERS = FeatureUtilities.parameters("Expression");

    /**
     * The expression on which to delegate the execution of this operation.
     */
    @SuppressWarnings("serial")                         // Not statically typed as serializable.
    private final Function<Feature, ? extends V> expression;

    /**
     * The type of result of evaluating the expression.
     */
    @SuppressWarnings("serial")                         // Apache SIS implementations are serializable.
    private final AttributeType<? super V> result;

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
     * @param result          type of values computed by the expression.
     */
    ExpressionOperation(final Map<String,?> identification,
                        final Function<Feature, ? extends V> expression,
                        final AttributeType<? super V> result)
    {
        super(identification);
        this.expression = expression;
        this.result     = result;
        if (expression instanceof Expression<?,?>) {
            dependencies = DependencyFinder.search((Expression<Feature,?>) expression);
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
    public IdentifiedType getResult() {
        return result;
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
    public Property apply(final Feature feature, ParameterValueGroup parameters) {
        final Attribute<? super V> instance = result.newInstance();
        instance.setValue(expression.apply(feature));
        return instance;
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
    private static final class DependencyFinder extends Visitor<Feature, Collection<String>> {
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
        static Set<String> search(final Expression<Feature,?> expression) {
            final Set<String> dependencies = new HashSet<>();
            VISITOR.visit(expression, dependencies);
            return Set.copyOf(dependencies);
        }

        /**
         * Constructor for the unique instance.
         */
        private DependencyFinder() {
            setLogicalHandlers((f, dependencies) -> {
                final var filter = (LogicalOperator<Feature>) f;
                for (Filter<Feature> child : filter.getOperands()) {
                    visit(child, dependencies);
                }
            });
            setExpressionHandler(FunctionNames.ValueReference, (e, dependencies) -> {
                final var expression = (ValueReference<Feature,?>) e;
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
        protected void typeNotFound(final CodeList<?> type, final Filter<Feature> filter, final Collection<String> dependencies) {
            for (final Expression<Feature,?> f : filter.getExpressions()) {
                visit(f, dependencies);
            }
        }

        /**
         * Fallback for all expressions not explicitly handled by the setting applied in the constructor.
         */
        @Override
        protected void typeNotFound(final String type, final Expression<Feature,?> expression, final Collection<String> dependencies) {
            for (final Expression<Feature,?> p : expression.getParameters()) {
                visit(p, dependencies);
            }
        }
    }
}
