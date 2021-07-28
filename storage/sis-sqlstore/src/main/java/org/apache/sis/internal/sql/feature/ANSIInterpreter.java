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
package org.apache.sis.internal.sql.feature;

import java.util.List;
import java.util.Collections;
import java.util.function.BiConsumer;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.WraparoundMethod;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.filter.FunctionNames;
import org.apache.sis.internal.filter.Visitor;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.setup.GeometryLibrary;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.Literal;
import org.opengis.filter.Expression;
import org.opengis.filter.ValueReference;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.ComparisonOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.SpatialOperator;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.BetweenComparisonOperator;
import org.opengis.filter.LikeOperator;


/**
 * Writes SQL statement for a filter or an expression.
 * This base class is restricted to ANSI compliant SQL.
 *
 * @implNote For now, we over-use parenthesis to ensure consistent operator priority. In the future, we could evolve
 * this component to provide more elegant transcription of filter groups.
 *
 * No case insensitive support of binary comparison is done.
 *
 * TODO: define a set of accepter property names (even better: link to {@link FeatureAdapter}), so any {@link ValueReference}
 * filter refering to non pure SQL property (like relations) will cause a failure.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
class ANSIInterpreter extends Visitor<Feature,StringBuilder> {
    /**
     * TODO
     */
    private static final GeometryLibrary LIBRARY = null;

    private final NameFactory nameFactory;

    private final NameSpace scope;

    public ANSIInterpreter() {
        nameFactory = DefaultFactories.forBuildin(NameFactory.class);
        scope = nameFactory.createNameSpace(nameFactory.createLocalName(null, "xpath"),
                                            Collections.singletonMap("separator", "/"));

        setFilterHandler(LogicalOperatorName.AND, new BinaryLogicJoin(" AND "));
        setFilterHandler(LogicalOperatorName.OR,  new BinaryLogicJoin(" OR "));
        setFilterHandler(LogicalOperatorName.NOT, (f,sb) -> {
            final LogicalOperator<Feature> filter = (LogicalOperator<Feature>) f;
            evaluateMandatory(sb.append("NOT ("), filter.getOperands().get(0));
            sb.append(')');
        });
        setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_BETWEEN), (f,sb) -> {
            final BetweenComparisonOperator<Feature>  filter = (BetweenComparisonOperator<Feature>) f;
            evaluateMandatory(sb,                     filter.getExpression());
            evaluateMandatory(sb.append(" BETWEEN "), filter.getLowerBoundary());
            evaluateMandatory(sb.append(" AND "),     filter.getUpperBoundary());
        });
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO,                 new JoinMatchCase(" = "));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO,             new JoinMatchCase(" <> "));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_GREATER_THAN,             new JoinMatchCase(" > "));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO, new JoinMatchCase(" >= "));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_LESS_THAN,                new JoinMatchCase(" < "));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO,    new JoinMatchCase(" <= "));
        setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_LIKE), (f,sb) -> {
            final LikeOperator<Feature> filter = (LikeOperator<Feature>) f;
            ensureMatchCase(filter.isMatchingCase());
            // TODO: port Geotk
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 30/09/2019
        });
        setNullAndNilHandlers((f,sb) -> {
            final ComparisonOperator<Feature> filter = (ComparisonOperator<Feature>) f;
            evaluateMandatory(sb, filter.getExpressions().get(0));
            sb.append(" IS NULL");
        });
        /*
         * SPATIAL FILTERS
         */
        setFilterHandler(SpatialOperatorName.BBOX, (f,sb) -> {
            final SpatialOperator<Feature> filter = (SpatialOperator<Feature>) f;
            // TODO: This is a wrong interpretation, but sqlmm has no equivalent of filter encoding bbox, so we'll
            // fallback on a standard intersection. However, PostGIS, H2, etc. have their own versions of such filters.
            for (final Expression<? super Feature, ?> e : filter.getExpressions()) {
                if (e == null) {
                    throw new UnsupportedOperationException("Not supported yet: bbox over all geometric properties");
                }
            }
            bbox(sb, filter);
        });
        setFilterHandler(SpatialOperatorName.CONTAINS,   new Function(FunctionNames.ST_Contains));
        setFilterHandler(SpatialOperatorName.CROSSES,    new Function(FunctionNames.ST_Crosses));
        setFilterHandler(SpatialOperatorName.DISJOINT,   new Function(FunctionNames.ST_Disjoint));
        setFilterHandler(SpatialOperatorName.EQUALS,     new Function(FunctionNames.ST_Equals));
        setFilterHandler(SpatialOperatorName.INTERSECTS, new Function(FunctionNames.ST_Intersects));
        setFilterHandler(SpatialOperatorName.OVERLAPS,   new Function(FunctionNames.ST_Overlaps));
        setFilterHandler(SpatialOperatorName.TOUCHES,    new Function(FunctionNames.ST_Touches));
        setFilterHandler(SpatialOperatorName.WITHIN,     new Function(FunctionNames.ST_Within));
        /*
         * Expression visitor
         */
        setExpressionHandler(FunctionNames.Add,      new Join(" + "));
        setExpressionHandler(FunctionNames.Subtract, new Join(" - "));
        setExpressionHandler(FunctionNames.Divide,   new Join(" / "));
        setExpressionHandler(FunctionNames.Multiply, new Join(" * "));
        setExpressionHandler(FunctionNames.Literal, (e,sb) -> writeLiteral(sb, (Literal<Feature,?>) e));
        setExpressionHandler(FunctionNames.ValueReference, (e,sb) -> writeColumnName(sb, (ValueReference<Feature,?>) e));
        // Temporary workaround. Filters created from Filter Encoding XML can specify "PropertyName" instead of "Value reference".
        setExpressionHandler("PropertyName", (e,sb) -> writeColumnName(sb, (ValueReference<Feature,?>) e));
    }

    /**
     * Returns the SQL fragment to use for {@link SpatialOperatorName#BBOX} type of filter.
     */
    void bbox(final StringBuilder sb, final SpatialOperator<Feature> filter) {
        function(sb, "ST_Intersects", filter);
    }

    private static void writeLiteral(final StringBuilder sb, final Literal<Feature,?> literal) {
        Object value;
        if (literal == null || (value = literal.getValue()) == null) {
            sb.append("NULL");
        } else if (value instanceof CharSequence) {
            String text = value.toString();
            text = text.replace("'", "''");
            sb.append('\'').append(text).append('\'');
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else {
            // Geometric special cases
            if (value instanceof GeographicBoundingBox) {
                value = new GeneralEnvelope((GeographicBoundingBox) value);
            }
            if (value instanceof Envelope) {
                value = asGeometry((Envelope) value);
            }
            if (value instanceof Geometry) {
                format(sb, (Geometry) value);
            } else {
                throw new UnsupportedOperationException("Not supported yet: Literal value of type " + value.getClass());
            }
        }
    }

    /**
     * Beware ! This implementation is a naïve one, expecting given property name to match exactly SQL database names.
     * In the future, it would be appreciable to be able to configure a mapper between feature and SQL names.
     *
     * @param  candidate  name of property to insert in SQL statement.
     */
    private void writeColumnName(final StringBuilder sb, final ValueReference<Feature,?> candidate) {
        final GenericName name = nameFactory.parseGenericName(scope, candidate.getXPath());
        final List<? extends LocalName> components = name.getParsedNames();
        final int n = components.size();
        for (int i=0; i<n; i++) {
            if (i != 0) sb.append('.');
            sb.append('"').append(components.get(i)).append('"');
        }
    }

    private final class BinaryLogicJoin implements BiConsumer<Filter<Feature>, StringBuilder> {
        private final String operator;

        BinaryLogicJoin(final String operator) {
            this.operator = operator;
        }

        @Override
        public void accept(final Filter<Feature> f, final StringBuilder sb) {
            final LogicalOperator<Feature> filter = (LogicalOperator<Feature>) f;
            final List<Filter<? super Feature>> subFilters = filter.getOperands();
            final int n = subFilters.size();
            if (n != 0) {
                sb.append('(');
                for (int i=0; i<n; i++) {
                    if (i != 0) sb.append(operator);
                    evaluateMandatory(sb, subFilters.get(i));
                }
                sb.append(')');
            }
        }
    }

    private void join(final StringBuilder sb,
                      final List<Expression<? super Feature, ?>> expressions,
                      final int maxCount, final String operator)
    {
        sb.append('(');
        final int n = Math.min(expressions.size(), maxCount);
        for (int i=0; i<n; i++) {
            if (i != 0) sb.append(operator);
            evaluateMandatory(sb, expressions.get(i));
        }
        sb.append(')');
    }

    private final class JoinMatchCase implements BiConsumer<Filter<Feature>, StringBuilder> {
        private final String operator;

        JoinMatchCase(final String operator) {
            this.operator = operator;
        }

        @Override
        public void accept(final Filter<Feature> f, final StringBuilder sb) {
            final BinaryComparisonOperator<Feature> filter = (BinaryComparisonOperator<Feature>) f;
            ensureMatchCase(filter.isMatchingCase());
            join(sb, filter.getExpressions(), 2, operator);
        }
    }

    protected final void join(final StringBuilder sb, final SpatialOperator<Feature> op, final String operator) {
        join(sb, op.getExpressions(), 2, operator);
    }

    private final class Join implements BiConsumer<Expression<Feature,?>, StringBuilder> {
        private final String operator;

        Join(final String operator) {
            this.operator = operator;
        }

        @Override
        public void accept(final Expression<Feature,?> expression, final StringBuilder sb) {
            join(sb, expression.getParameters(), 2, operator);
        }
    }

    private final class Function implements BiConsumer<Filter<Feature>, StringBuilder> {
        private final String name;

        Function(final String name) {
            this.name = name;
        }

        @Override
        public void accept(final Filter<Feature> f, final StringBuilder sb) {
            function(sb, name, (SpatialOperator<Feature>) f);
        }
    }

    private void function(final StringBuilder sb, final String name, final SpatialOperator<Feature> filter) {
        join(sb.append(name), filter.getExpressions(), Integer.MAX_VALUE, ", ");
    }

    /**
     * Executes the registered action for the given filter.
     * Throws an exception if the filter did not write anything in the buffer.
     *
     * <h4>Note on type safety</h4>
     * This method signature uses {@code <? super R>} for caller's convenience because this is the type that
     * we get from {@link LogicalOperator#getOperands()}. But the {@link BiConsumer} uses exactly {@code <R>}
     * type because doing otherwise causes complications with types that can not be expressed in Java (kinds
     * of {@code <? super ? super R>}). The cast in this method is okay if we do not invoke any {@code filter}
     * method with a return value (directly or indirectly as list elements) of exactly {@code <R>} type.
     * Such methods do not exist in the GeoAPI interfaces, so we are safe if the {@link BiConsumer}
     * does not invoke implementation-specific methods.
     *
     * @param  sb      where to write the result of all actions.
     * @param  filter  the filter for which to execute an action based on its type.
     * @throws UnsupportedOperationException if there is no action registered for the given filter.
     */
    @SuppressWarnings("unchecked")
    private void evaluateMandatory(final StringBuilder sb, final Filter<? super Feature> filter) {
        final int pos = sb.length();
        visit((Filter<Feature>) filter, sb);
        if (sb.length() <= pos) {
            throw new IllegalArgumentException("Filter evaluate to an empty text: " + filter);
        }
    }

    /**
     * Executes the registered action for the given expression.
     * Throws an exception if the expression did not write anything in the buffer.
     *
     * <h4>Note on type safety</h4>
     * This method signature uses {@code <? super R>} for caller's convenience because this is the type that
     * we get from {@link Expression#getParameters()}. But the {@link BiConsumer} expects exactly {@code <R>}
     * type because doing otherwise causes complications with types that can not be expressed in Java (kinds
     * of {@code <? super ? super R>}). The cast in this method is okay if we do not invoke any {@code exp}
     * method with a return value (directly or indirectly as list elements) of exactly {@code <R>} type.
     * Such methods do not exist in the GeoAPI interfaces, so we are safe if the {@link BiConsumer}
     * does not invoke implementation-specific methods.
     *
     * @param  sb   where to write the result of all actions.
     * @param  exp  the expression for which to execute an action based on its type.
     * @throws UnsupportedOperationException if there is no action registered for the given expression.
     */
    @SuppressWarnings("unchecked")
    private void evaluateMandatory(final StringBuilder sb, final Expression<? super Feature, ?> exp) {
        final int pos = sb.length();
        visit((Expression<Feature, ?>) exp, sb);
        if (sb.length() <= pos) {
            throw new IllegalArgumentException("Expression evaluate to an empty text: " + exp);
        }
    }

    private static void ensureMatchCase(final boolean isMatchingCase) {
        if (!isMatchingCase) {
            throw new UnsupportedOperationException("case insensitive match is not defined by ANSI SQL");
        }
    }

    private static Geometry asGeometry(final Envelope source) {
        final double[] lower = source.getLowerCorner().getCoordinate();
        final double[] upper = source.getUpperCorner().getCoordinate();
        for (int i = 0 ; i < lower.length ; i++) {
            if (Double.isNaN(lower[i]) || Double.isNaN(upper[i])) {
                throw new IllegalArgumentException("Cannot use envelope containing NaN for filter");
            }
            lower[i] = clampInfinity(lower[i]);
            upper[i] = clampInfinity(upper[i]);
        }
        final GeneralEnvelope env = new GeneralEnvelope(lower, upper);
        env.setCoordinateReferenceSystem(source.getCoordinateReferenceSystem());
        return Geometries.implementation(LIBRARY).toGeometry2D(env, WraparoundMethod.SPLIT);
    }

    private static void format(final StringBuilder sb, final Geometry source) {
        final GeometryWrapper<?> wrapper = Geometries.wrap(source).orElseThrow(
                () -> new IllegalArgumentException("Unsupported geometry implementation."));
        // TODO: find a better approximation of desired "flatness"
        final Envelope env = wrapper.getEnvelope();
        final int n = Math.min(env.getDimension(), 2);
        double span = 0;
        for (int i=0; i<n; i++) {
            span += env.getSpan(i);
        }
        sb.append("ST_GeomFromText('").append(wrapper.formatWKT(0.05 * span/n)).append("')");
    }

    private static double clampInfinity(final double candidate) {
        if (candidate == Double.NEGATIVE_INFINITY) {
            return -Double.MAX_VALUE;
        } else if (candidate == Double.POSITIVE_INFINITY) {
            return Double.MAX_VALUE;
        }
        return candidate;
    }
}
