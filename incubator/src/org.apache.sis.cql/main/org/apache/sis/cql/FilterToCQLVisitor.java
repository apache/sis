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
package org.apache.sis.cql;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.time.temporal.TemporalAccessor;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import javax.measure.Unit;
import javax.measure.Quantity;
import org.opengis.util.CodeList;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.DistanceOperatorName;
import org.opengis.filter.TemporalOperatorName;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.BetweenComparisonOperator;
import org.opengis.filter.LikeOperator;
import org.opengis.filter.Expression;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.filter.visitor.FunctionNames;
import org.apache.sis.filter.visitor.Visitor;
import org.apache.sis.temporal.LenientDateFormat;


/**
 * Visitor to convert a Filter in CQL.
 *
 * @author  Johann Sorel (Geomatys)
 */
final class FilterToCQLVisitor extends Visitor<Feature,StringBuilder> {

    static final FilterToCQLVisitor INSTANCE = new FilterToCQLVisitor();

    /**
     * Pattern to check for property name to escape against regExp
     */
    private final Pattern patternPropertyName;

    /**
     * Formatter to use for unit symbol. Not thread-safe; usage must be synchronized.
     */
    private final UnitFormat unitFormat;

    /**
     * Creates a new visitor.
     */
    private FilterToCQLVisitor() {
        patternPropertyName = Pattern.compile("[,+\\-/*\\t\\n\\r\\d\\s]");
        unitFormat = new UnitFormat(Locale.US);
        unitFormat.setStyle(UnitFormat.Style.NAME);

        constant(Filter.exclude(), "1=0");
        constant(Filter.include(), "1=1");
        operatorBetweenValues(LogicalOperatorName.AND, "AND");
        operatorBetweenValues(LogicalOperatorName.OR,  "OR");
        setFilterHandler(LogicalOperatorName.NOT, (f,sb) -> {
            final LogicalOperator<Feature> filter = (LogicalOperator<Feature>) f;
            format(sb.append("NOT "), filter.getOperands().get(0));
        });
        setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_BETWEEN), (f,sb) -> {
            final BetweenComparisonOperator<Feature> filter = (BetweenComparisonOperator<Feature>) f;
            format(sb, filter.getExpression());
            format(sb.append(" BETWEEN "), filter.getLowerBoundary());
            format(sb.append(" AND "), filter.getUpperBoundary());
        });
        operatorBetweenValues(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO,                 "=");
        operatorBetweenValues(ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO,             "<>");
        operatorBetweenValues(ComparisonOperatorName.PROPERTY_IS_GREATER_THAN,             ">");
        operatorBetweenValues(ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO, ">=");
        operatorBetweenValues(ComparisonOperatorName.PROPERTY_IS_LESS_THAN,                "<");
        operatorBetweenValues(ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO,    "<=");
        setFilterHandler(ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_LIKE), (f,sb) -> {
            final LikeOperator<Feature> filter = (LikeOperator<Feature>) f;
            List<Expression<Feature,?>> operands = f.getExpressions();
            format(sb, operands.get(0));
            // TODO: ILIKE is not standard SQL.
            sb.append(filter.isMatchingCase() ? " LIKE " : " ILIKE ");
            // TODO: convert wildcards and escape to SQL 92.
            format(sb, operands.get(1));
        });
        operatorAfterValue(FunctionNames.PROPERTY_IS_NULL, " IS NULL");
        operatorAfterValue(FunctionNames.PROPERTY_IS_NIL,  " IS NIL");
        /*
         * Spatial filters.
         */
        setFilterHandler(SpatialOperatorName.BBOX, (f,sb) -> {
            final BinarySpatialOperator<Feature> filter = (BinarySpatialOperator<Feature>) f;
            final Expression<Feature,?> left  = filter.getOperand1();
            final Expression<Feature,?> right = filter.getOperand2();
            final ValueReference<Feature,?> pName =
                    (left  instanceof ValueReference) ? (ValueReference<Feature,?>) left :
                    (right instanceof ValueReference) ? (ValueReference<Feature,?>) right : null;
            final Object lit = ((left instanceof Literal)
                    ? (Literal<Feature,?>) left
                    : (Literal<Feature,?>) right).getValue();      // TODO: potential classCastException.

            final GeneralEnvelope e = Geometries.wrap(lit).map(GeometryWrapper::getEnvelope).orElse(null);
            if (e != null) {
                if (e.getDimension() > 2) {
                    throw new UnsupportedOperationException("Only 2D envelopes accepted");
                }
                sb.append("BBOX(").append(pName.getXPath()).append(", ");
                sb.append(e.getMinimum(0)).append(", ")
                  .append(e.getMaximum(0)).append(", ")
                  .append(e.getMinimum(1)).append(", ")
                  .append(e.getMaximum(1)).append(')');
            } else {
                // Use writing BBOX(exp1,exp2).
                format(sb.append("BBOX("), left);
                format(sb.append(','), right);
                sb.append(')');
            }
        });
        for (final SpatialOperatorName type : SpatialOperatorName.values()) {
            if (type != SpatialOperatorName.BBOX) {
                function(type, type.identifier().orElse(type.name()).toUpperCase(Locale.US));
                if (type == SpatialOperatorName.OVERLAPS) break;
            }
        }
        function(DistanceOperatorName.WITHIN, "DWITHIN");
        function(DistanceOperatorName.BEYOND, "BEYOND");
        for (final TemporalOperatorName type : TemporalOperatorName.values()) {
            function(type, type.identifier().orElse(type.name()).toUpperCase(Locale.US));
            if (type == TemporalOperatorName.ANY_INTERACTS) break;
        }
        /*
         * Expressions
         */
        setExpressionHandler(FunctionNames.Literal, (e,sb) -> {
            final Literal<Feature,?> exp = (Literal<Feature,?>) e;
            final Object value = exp.getValue();
            if (value instanceof Quantity<?>) {
                final Quantity<?> q = (Quantity<?>) value;
                final Unit<?> unit = q.getUnit();
                sb.append(q.getValue().doubleValue()).append(", '");
                try {
                    synchronized (unitFormat) {
                        unitFormat.format(unit, sb);
                    }
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);     // Should never happen.
                }
                sb.append('\'');
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof Date) {
                final Date date = (Date) value;
                sb.append(LenientDateFormat.FORMAT.format(date.toInstant()));
            } else if (value instanceof TemporalAccessor) {
                final TemporalAccessor date = (TemporalAccessor) value;
                sb.append(LenientDateFormat.FORMAT.format(date));
            } else if (value instanceof Geometry) {
                final Geometry geometry = (Geometry) value;
                final WKTWriter writer = new WKTWriter();
                sb.append(writer.write(geometry));
            } else {
                sb.append('\'').append(value).append('\'');
            }
        });
        setExpressionHandler(FunctionNames.ValueReference, (e,sb) -> {
            final ValueReference<Feature,?> exp = (ValueReference<Feature,?>) e;
            final String name = exp.getXPath();
            if (patternPropertyName.matcher(name).find()) {
                // Escape for special chars
                sb.append('"').append(name).append('"');
            } else {
                sb.append(name);
            }
        });
        arithmetic(FunctionNames.Add,      '+');
        arithmetic(FunctionNames.Divide,   '/');
        arithmetic(FunctionNames.Multiply, '*');
        arithmetic(FunctionNames.Subtract, '-');
    }

    private void constant(final Filter<?> type, final String text) {
        setFilterHandler(type.getOperatorType(), (f,sb) -> sb.append(text));
    }

    private void operatorAfterValue(final String type, final String operator) {
        setFilterHandler(ComparisonOperatorName.valueOf(type), (f,sb) -> {
            format(sb, f.getExpressions().get(0));
            sb.append(operator);
        });
    }

    private void operatorBetweenValues(final ComparisonOperatorName type, final String operator) {
        setFilterHandler(type, (f,sb) -> {
            final List<Expression<Feature,?>> operands = f.getExpressions();
            format(sb, operands.get(0));
            final int n = operands.size();
            for (int i=1; i<n; i++) {
                // Should execute only once. If n>2, make the problem visible in the CQL.
                format(sb.append(' ').append(operator).append(' '), operands.get(i));
            }
        });
    }

    private void operatorBetweenValues(final LogicalOperatorName type, final String operator) {
        setFilterHandler(type, (f,sb) -> {
            final LogicalOperator<Feature> filter = (LogicalOperator<Feature>) f;
            final List<Filter<Feature>> operands = filter.getOperands();
            format(sb.append('('), operands.get(0));
            final int n = operands.size();
            for (int i=1; i<n; i++) {
                format(sb.append(' ').append(operator).append(' '), operands.get(i));
            }
            sb.append(')');
        });
    }

    private void function(final CodeList<?> type, final String operator) {
        setFilterHandler(type, (f,sb) -> {
            final List<Expression<Feature,?>> operands = f.getExpressions();
            sb.append(operator).append('(');
            final int n = operands.size();
            for (int i=0; i<n; i++) {
                if (i != 0) sb.append(", ");
                format(sb, operands.get(i));
            }
            sb.append(')');
        });
    }

    private void arithmetic(final String type, final char operator) {
        setExpressionHandler(type, (e,sb) -> {
            final List<Expression<Feature,?>> parameters = e.getParameters();
            format(sb, parameters.get(0));
            final int n = parameters.size();
            for (int i=1; i<n; i++) {
                format(sb.append(' ').append(operator).append(' '), parameters.get(i));
            }
        });
    }

    /**
     * Executes the registered action for the given filter.
     *
     * <h4>Note on type safety</h4>
     * This method signature uses {@code <? super R>} for caller's convenience because this is the type that
     * we get from {@link LogicalOperator#getOperands()}. But the {@link BiConsumer} uses exactly {@code <R>}
     * type because doing otherwise causes complications with types that cannot be expressed in Java (kinds
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
    private void format(final StringBuilder sb, final Filter<Feature> filter) {
        visit((Filter<Feature>) filter, sb);
    }

    /**
     * Executes the registered action for the given expression.
     * Throws an exception if the expression did not write anything in the buffer.
     *
     * <h4>Note on type safety</h4>
     * This method signature uses {@code <? super R>} for caller's convenience because this is the type that
     * we get from {@link Expression#getParameters()}. But the {@link BiConsumer} expects exactly {@code <R>}
     * type because doing otherwise causes complications with types that cannot be expressed in Java (kinds
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
    private void format(final StringBuilder sb, final Expression<Feature,?> expression) {
        visit((Expression<Feature,?>) expression, sb);
    }

    @Override
    protected void typeNotFound(final String type, final Expression<Feature,?> e, final StringBuilder sb) {
        final List<Expression<Feature,?>> exps = e.getParameters();
        sb.append(type).append('(');
        final int n = exps.size();
        for (int i=0; i<n; i++) {
            if (i != 0) sb.append(", ");
            format(sb, exps.get(i));
        }
        sb.append(')');
    }
}
