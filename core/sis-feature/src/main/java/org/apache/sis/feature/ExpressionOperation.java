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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.feature.FeatureUtilities;
import org.apache.sis.internal.filter.FunctionNames;
import org.apache.sis.internal.filter.Visitor;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Property;
import org.opengis.filter.BetweenComparisonOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.LikeOperator;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.ValueReference;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.CodeList;
import org.opengis.util.GenericName;

/**
 * An operation computing the result of expression on current feature.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ExpressionOperation<V> extends AbstractOperation {

    /**
     * The parameter descriptor for the "virtual" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = FeatureUtilities.parameters("Virtual");

    private static final ListPropertyVisitor VISITOR = new ListPropertyVisitor();

    private final FeatureExpression<Feature,V> expression;
    private final AttributeType<V> type;
    private final Set<String> dependencies;

    public ExpressionOperation(GenericName name, FeatureExpression<Feature,V> expression, FeatureType featureType) {
        super(Collections.singletonMap(DefaultAttributeType.NAME_KEY, name));
        this.expression = expression;
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        PropertyTypeBuilder expectedType = expression.expectedType(featureType, ftb);
        expectedType.setName(name);
        type = (AttributeType<V>) expectedType.build();

        final Set<String> dependencies = new HashSet<>();
        VISITOR.visit((Expression) expression, dependencies);
        this.dependencies = Collections.unmodifiableSet(dependencies);
    }

    public FeatureExpression<Feature, V> getExpression() {
        return expression;
    }

    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    @Override
    public IdentifiedType getResult() {
        return type;
    }

    @Override
    public Set<String> getDependencies() {
        return dependencies;
    }

    @Override
    public Property apply(Feature feature, ParameterValueGroup parameters) {
        final Attribute<V> att = type.newInstance();
        att.setValue(expression.apply(feature));
        return att;
    }

    private static final class ListPropertyVisitor extends Visitor<Object,Collection<String>> {

        protected ListPropertyVisitor() {
            setLogicalHandlers((f, names) -> {
                final LogicalOperator<Object> filter = (LogicalOperator<Object>) f;
                for (Filter<Object> child : filter.getOperands()) {
                    visit(child, names);
                }
            });
            setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_BETWEEN), (f, names) -> {
                final BetweenComparisonOperator<Object> filter = (BetweenComparisonOperator<Object>) f;
                visit(filter.getExpression(),    names);
                visit(filter.getLowerBoundary(), names);
                visit(filter.getUpperBoundary(), names);
            });
            setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_LIKE), (f, names) -> {
                final LikeOperator<Object> filter = (LikeOperator<Object>) f;
                visit(filter.getExpressions().get(0), names);
            });
            setExpressionHandler(FunctionNames.ValueReference, (e, names) -> {
                final ValueReference<Object,?> expression = (ValueReference<Object,?>) e;
                final String propName = expression.getXPath();
                if (!propName.trim().isEmpty()) {
                    names.add(propName);
                }
            });
        }

        @Override
        protected void typeNotFound(final CodeList<?> type, final Filter<Object> filter, final Collection<String> names) {
            for (final Expression<? super Object, ?> f : filter.getExpressions()) {
                visit(f, names);
            }
        }

        @Override
        protected void typeNotFound(final String type, final Expression<Object, ?> expression, final Collection<String> names) {
            for (final Expression<? super Object, ?> p : expression.getParameters()) {
                visit(p, names);
            }
        }
    }
}
