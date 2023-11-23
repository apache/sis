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
package org.apache.sis.storage.shapefile;

import java.util.Collection;
import org.apache.sis.filter.internal.FunctionNames;
import org.apache.sis.filter.internal.Visitor;

// Specific to the main branch:
import org.apache.sis.filter.Filter;
import org.apache.sis.filter.Expression;
import org.apache.sis.pending.geoapi.filter.BetweenComparisonOperator;
import org.apache.sis.pending.geoapi.filter.ValueReference;
import org.apache.sis.pending.geoapi.filter.ComparisonOperatorName;
import org.apache.sis.pending.geoapi.filter.LogicalOperator;


/**
 * Expression visitor that returns a list of all Feature attributs requiered by this expression.
 *
 * @author Johann Sorel (Geomatys)
 */
final class ListingPropertyVisitor extends Visitor<Object,Collection<String>> {

    public static final ListingPropertyVisitor VISITOR = new ListingPropertyVisitor();

    protected ListingPropertyVisitor() {
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
            visit(f.getExpressions().get(0), names);
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
    protected void typeNotFound(final Enum<?> type, final Filter<Object> filter, final Collection<String> names) {
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
