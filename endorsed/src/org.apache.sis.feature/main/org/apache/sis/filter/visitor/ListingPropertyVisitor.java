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
package org.apache.sis.filter.visitor;

import java.util.HashSet;
import java.util.Set;

// Specific to the main branch:
import org.apache.sis.filter.Filter;
import org.apache.sis.filter.Expression;
import org.apache.sis.pending.geoapi.filter.BetweenComparisonOperator;
import org.apache.sis.pending.geoapi.filter.ValueReference;
import org.apache.sis.pending.geoapi.filter.ComparisonOperatorName;
import org.apache.sis.pending.geoapi.filter.LogicalOperator;


/**
 * A collector of all attributes required by a filter or an expression.
 * This visitor collects the XPaths of all {@link ValueReference} found.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ListingPropertyVisitor extends Visitor<Object, Set<String>> {
    /**
     * The unique instance of this visitor.
     */
    private static final ListingPropertyVisitor INSTANCE = new ListingPropertyVisitor();

    /**
     * Creates the unique instance of this visitor.
     */
    private ListingPropertyVisitor() {
        setLogicalHandlers((f, names) -> {
            final var filter = (LogicalOperator<Object>) f;
            for (Filter<Object> child : filter.getOperands()) {
                visit(child, names);
            }
        });
        setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_BETWEEN), (f, names) -> {
            final var filter = (BetweenComparisonOperator<Object>) f;
            visit(filter.getExpression(),    names);
            visit(filter.getLowerBoundary(), names);
            visit(filter.getUpperBoundary(), names);
        });
        setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_LIKE), (f, names) -> {
            visit(f.getExpressions().get(0), names);
        });
        setExpressionHandler(FunctionNames.ValueReference, (e, names) -> {
            final var expression = (ValueReference<Object,?>) e;
            names.add(expression.getXPath());
        });
    }

    /**
     * Visits all operands of the given filter for listing all value references.
     *
     * @param  type    the filter type (may be {@code null}).
     * @param  filter  the filter (may be {@code null}).
     * @param  xpaths  where to add the XPaths.
     */
    @Override
    protected void typeNotFound(final Enum<?> type, final Filter<Object> filter, final Set<String> xpaths) {
        for (final var f : filter.getExpressions()) {
            visit(f, xpaths);
        }
    }

    /**
     * Visits all parameters of the given expression for listing all value references.
     *
     * @param  type        the expression type (may be {@code null}).
     * @param  expression  the expression (may be {@code null}).
     * @param  xpaths      where to add the XPaths.
     */
    @Override
    protected void typeNotFound(final String type, final Expression<Object, ?> expression, final Set<String> xpaths) {
        for (final var p : expression.getParameters()) {
            visit(p, xpaths);
        }
    }

    /**
     * Returns all XPaths used, directly or indirectly, by the given filter.
     * The elements in the set are in no particular order.
     *
     * @param  filter  the filter for which to get the XPaths. May be {@code null}.
     * @param  xpaths  a pre-allocated collection where to add the XPaths, or {@code null} if none.
     * @return the given collection, or a new one if it was {@code null}, with XPaths added.
     */
    @SuppressWarnings("unchecked")
    public static Set<String> xpaths(final Filter<?> filter, Set<String> xpaths) {
        if (xpaths == null) {
            xpaths = new HashSet<>();
        }
        if (filter != null) {
            INSTANCE.visit((Filter) filter, xpaths);
        }
        return xpaths;
    }

    /**
     * Returns all XPaths used, directly or indirectly, by the given expression.
     * The elements in the set are in no particular order.
     *
     * @param  expression  the expression for which to get the XPaths. May be {@code null}.
     * @param  xpaths  a pre-allocated collection where to add the XPaths, or {@code null} if none.
     * @return the given collection, or a new one if it was {@code null}, with XPaths added.
     */
    @SuppressWarnings("unchecked")
    public static Set<String> xpaths(final Expression<?,?> expression, Set<String> xpaths) {
        if (xpaths == null) {
            xpaths = new HashSet<>();
        }
        if (expression != null) {
            INSTANCE.visit((Expression) expression, xpaths);
        }
        return xpaths;
    }
}
