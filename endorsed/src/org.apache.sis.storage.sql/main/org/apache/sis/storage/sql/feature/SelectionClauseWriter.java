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
package org.apache.sis.storage.sql.feature;

import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.sis.filter.internal.shared.FunctionNames;
import org.apache.sis.filter.internal.shared.Visitor;

// Specific to the main branch:
import org.apache.sis.filter.Filter;
import org.apache.sis.filter.Expression;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.pending.geoapi.filter.Literal;
import org.apache.sis.pending.geoapi.filter.ValueReference;
import org.apache.sis.pending.geoapi.filter.LogicalOperator;
import org.apache.sis.pending.geoapi.filter.LogicalOperatorName;
import org.apache.sis.pending.geoapi.filter.ComparisonOperatorName;
import org.apache.sis.pending.geoapi.filter.BinaryComparisonOperator;
import org.apache.sis.pending.geoapi.filter.SpatialOperatorName;
import org.apache.sis.pending.geoapi.filter.BetweenComparisonOperator;


/**
 * Converter from filters/expressions to the {@code WHERE} part of SQL statement.
 * This base class handles ANSI compliant SQL. Subclasses can add database-specific syntax.
 *
 * <p>As soon as a filter or expression is not supported by this interpreter, the writing
 * of the SQL statement stops and next filters operations will be executed with Java code.</p>
 *
 * <h2>Implementation notes</h2>
 * For now, we over-use parenthesis to ensure consistent operator priority.
 * In the future, we could evolve this component to provide more elegant transcription of filter groups.
 *
 * <h2>Thread-safety</h2>
 * Instances of this classes shall be unmodified after construction and thus thread-safe.
 * Information about the state of a conversion to SQL is stored in {@link SelectionClause}.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class SelectionClauseWriter extends Visitor<AbstractFeature, SelectionClause> {
    /**
     * The default instance.
     */
    protected static final SelectionClauseWriter DEFAULT = new SelectionClauseWriter();

    /**
     * Creates a new converter from filters/expressions to SQL.
     */
    private SelectionClauseWriter() {
        setFilterHandler(LogicalOperatorName.AND, new Logic(" AND ", false));
        setFilterHandler(LogicalOperatorName.OR,  new Logic(" OR ",  false));
        setFilterHandler(LogicalOperatorName.NOT, new Logic( "NOT ", true));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO,                 new Comparison(" = "));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO,             new Comparison(" <> "));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_GREATER_THAN,             new Comparison(" > "));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO, new Comparison(" >= "));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_LESS_THAN,                new Comparison(" < "));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO,    new Comparison(" <= "));
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_BETWEEN, (f,sql) -> {
            final BetweenComparisonOperator<AbstractFeature>  filter = (BetweenComparisonOperator<AbstractFeature>) f;
            /* Nothing to append */  if (write(sql, filter.getExpression()))    return;
            sql.append(" BETWEEN "); if (write(sql, filter.getLowerBoundary())) return;
            sql.append(" AND ");         write(sql, filter.getUpperBoundary());
        });
        setNullAndNilHandlers((filter, sql) -> {
            final List<Expression<AbstractFeature, ?>> expressions = filter.getExpressions();
            if (expressions.size() == 1) {
                write(sql, expressions.get(0));
                sql.append(" IS NULL");
            } else {
                sql.invalidate();
            }
        });
        /*
         * Spatial filters.
         */
        setFilterHandler(SpatialOperatorName.CONTAINS,   new Function(FunctionNames.ST_Contains));
        setFilterHandler(SpatialOperatorName.CROSSES,    new Function(FunctionNames.ST_Crosses));
        setFilterHandler(SpatialOperatorName.DISJOINT,   new Function(FunctionNames.ST_Disjoint));
        setFilterHandler(SpatialOperatorName.EQUALS,     new Function(FunctionNames.ST_Equals));
        setFilterHandler(SpatialOperatorName.INTERSECTS, new Function(FunctionNames.ST_Intersects));
        setFilterHandler(SpatialOperatorName.OVERLAPS,   new Function(FunctionNames.ST_Overlaps));
        setFilterHandler(SpatialOperatorName.TOUCHES,    new Function(FunctionNames.ST_Touches));
        setFilterHandler(SpatialOperatorName.WITHIN,     new Function(FunctionNames.ST_Within));
        /*
         * Expression visitor.
         */
        setExpressionHandler(FunctionNames.Add,      new Arithmetic(" + "));
        setExpressionHandler(FunctionNames.Subtract, new Arithmetic(" - "));
        setExpressionHandler(FunctionNames.Divide,   new Arithmetic(" / "));
        setExpressionHandler(FunctionNames.Multiply, new Arithmetic(" * "));
        setExpressionHandler(FunctionNames.Literal, (e,sql) -> sql.appendLiteral(((Literal<AbstractFeature,?>) e).getValue()));
        setExpressionHandler(FunctionNames.ValueReference, (e,sql) -> sql.appendColumnName((ValueReference<AbstractFeature,?>) e));
        // Filters created from Filter Encoding XML can specify "PropertyName" instead of "Value reference".
        setExpressionHandler("PropertyName", getExpressionHandler(FunctionNames.ValueReference));
    }

    /**
     * Creates a new converter initialized to the same handlers as the specified converter.
     * The given source is usually {@link #DEFAULT}.
     *
     * @param  source  the converter from which to copy the handlers.
     */
    protected SelectionClauseWriter(final SelectionClauseWriter source) {
        super(source, true, false);
    }

    /**
     * Creates a new converter of the same class as {@code this} and initialized with the same data.
     * This method is invoked before to remove handlers for functions that are unsupported on the target
     * database software.
     *
     * @return a converter initialized to a copy of {@code this}.
     */
    protected SelectionClauseWriter duplicate() {
        return new SelectionClauseWriter(this);
    }

    /**
     * Returns a writer without the functions that are unsupported by the database software.
     * If the database supports all functions, then this method returns {@code this}.
     * Otherwise it returns a copy of {@code this} with unsupported functions removed.
     * This method should be invoked at most once for a {@link Database} instance.
     *
     * @param  database  information about the database software.
     * @return a writer with unsupported functions removed.
     */
    final SelectionClauseWriter removeUnsupportedFunctions(final Database<?> database) {
        final var unsupported = new HashMap<String, SpatialOperatorName>();
        final var accessors = GeometryEncoding.initial();
        try (Connection c = database.source.getConnection()) {
            final DatabaseMetaData metadata = c.getMetaData();
            /*
             * Get the names of all spatial functions for which a handler is registered.
             * All those handlers should be instances of `Function`, otherwise we do not
             * know how to determine whether the function is supported or not.
             */
            final boolean lowerCase = metadata.storesLowerCaseIdentifiers();
            final boolean upperCase = metadata.storesUpperCaseIdentifiers();
            for (final SpatialOperatorName type : SpatialOperatorName.values()) {
                final BiConsumer<Filter<AbstractFeature>, SelectionClause> function = getFilterHandler(type);
                if (function instanceof Function) {
                    String name = ((Function) function).name;
                    if (lowerCase) name = name.toLowerCase(Locale.US);
                    if (upperCase) name = name.toUpperCase(Locale.US);
                    unsupported.put(name, type);
                }
            }
            /*
             * Remove from above map all functions that are supported by the database.
             * This list is potentially large so we do not put those items in a map.
             */
            final String prefix = database.escapeWildcards(lowerCase ? "st_" : "ST_");
            try (ResultSet r = metadata.getFunctions(database.catalogOfSpatialTables,
                                                     database.schemaOfSpatialTables,
                                                     prefix + '%'))
            {
                while (r.next()) {
                    final String function = r.getString("FUNCTION_NAME");
                    GeometryEncoding.checkSupport(accessors, function);
                    unsupported.remove(function);
                }
            }
        } catch (SQLException e) {
            /*
             * If this exception happens before `unsupported` entries were removed,
             * this is equivalent to assuming that all functions are unsupported.
             */
            database.listeners.warning(e);
        }
        database.setGeometryEncodingFunctions(accessors);
        /*
         * Remaining functions are unsupported functions.
         */
        if (unsupported.isEmpty()) {
            return this;
        }
        final SelectionClauseWriter copy = duplicate();
        copy.removeFilterHandlers(unsupported.values());
        return copy;
    }

    /**
     * Invoked when an unsupported filter is found. The SQL string is marked as invalid and
     * may be truncated (later) to the length that it has the last time that it was valid.
     */
    @Override
    protected final void typeNotFound(Enum<?> type, Filter<AbstractFeature> filter, SelectionClause sql) {
        sql.invalidate();
    }

    /**
     * Invoked when an unsupported expression is found. The SQL string is marked as invalid
     * and may be truncated (later) to the length that it has the last time that it was valid.
     */
    @Override
    protected final void typeNotFound(String type, Expression<AbstractFeature,?> expression, SelectionClause sql) {
        sql.invalidate();
    }

    /**
     * Executes the registered action for the given filter.
     *
     * <h4>Note on type safety</h4>
     * This method applies a theoretically unsafe cast, which is okay in the context of this class.
     * See <cite>Note on parameterized type</cite> section in {@link Visitor#visit(Filter, Object)}.
     *
     * @param  sql     where to write the result of all actions.
     * @param  filter  the filter for which to execute an action based on its type.
     * @return value of {@link SelectionClause#isInvalid} flag, for allowing caller to short-circuit.
     */
    @SuppressWarnings("unchecked")
    final boolean write(final SelectionClause sql, final Filter<? super AbstractFeature> filter) {
        visit((Filter<AbstractFeature>) filter, sql);
        return sql.isInvalid();
    }

    /**
     * Executes the registered action for the given expression.
     *
     * @param  sql         where to write the result of all actions.
     * @param  expression  the expression for which to execute an action based on its type.
     * @return value of {@link SelectionClause#isInvalid} flag, for allowing caller to short-circuit.
     */
    private boolean write(final SelectionClause sql, final Expression<AbstractFeature, ?> expression) {
        visit(expression, sql);
        return sql.isInvalid();
    }

    /**
     * Writes the expressions of a filter as a binary operator.
     * The filter must have exactly two expressions, otherwise the SQL will be declared invalid.
     *
     * @param sql       where to append the SQL clause.
     * @param filter    the filter for which to append the expressions.
     * @param operator  the operator to write between the expressions.
     */
    protected final void writeBinaryOperator(final SelectionClause sql, final Filter<AbstractFeature> filter, final String operator) {
        writeParameters(sql, filter.getExpressions(), operator, true);
    }

    /**
     * Writes the parameters of a function or a binary operator.
     *
     * @param sql          where to append the SQL clause.
     * @param expressions  the expressions to write.
     * @param separator    the separator to insert between expression.
     * @param binary       whether the list of expressions shall contain exactly 2 elements.
     */
    private void writeParameters(final SelectionClause sql, final List<Expression<AbstractFeature,?>> expressions,
                                 final String separator, final boolean binary)
    {
        final int n = expressions.size();
        if (binary && n != 2) {
            sql.invalidate();
            return;
        }
        // No check for n=0 because we want "()" in that case.
        sql.append('(');
        for (int i=0; i<n; i++) {
            if (i != 0) sql.append(separator);
            if (write(sql, expressions.get(i))) return;
        }
        sql.append(')');
    }




    /**
     * Handler for converting an {@code AND}, {@code OR} or {@code NOT} filter into SQL clauses.
     * The filter can contain an arbitrary number of operands, all separated by the same keyword.
     * All operands are grouped between parenthesis.
     */
    private final class Logic implements BiConsumer<Filter<AbstractFeature>, SelectionClause> {
        /**
         * The {@code AND}, {@code OR} or {@code NOT} keyword.
         * Shall contain a trailing space and eventually a leading space.
         */
        private final String operator;

        /**
         * Whether this operator is the unary operator. In that case exactly one operand is expected
         * and the keyword will be written before the operand instead of between the operands.
         */
        private final boolean unary;

        /** Creates a handler using the given SQL keyword. */
        Logic(final String operator, final boolean unary) {
            this.operator = operator;
            this.unary    = unary;
        }

        /** Invoked when a logical filter needs to be converted to SQL clause. */
        @Override public void accept(final Filter<AbstractFeature> f, final SelectionClause sql) {
            final var filter = (LogicalOperator<AbstractFeature>) f;
            final List<Filter<AbstractFeature>> operands = filter.getOperands();
            final int n = operands.size();
            if (unary ? (n != 1) : (n == 0)) {
                sql.invalidate();
            } else {
                if (unary) {
                    sql.append(operator);
                }
                sql.append('(');
                for (int i=0; i<n; i++) {
                    if (i != 0) sql.append(operator);
                    if (write(sql, operands.get(i))) return;
                }
                sql.append(')');
            }
        }
    }




    /**
     * Handler for converting {@code =}, {@code <}, {@code >}, {@code <=} or {@code >=} filter
     * into SQL clauses. The filter is expected to contain exactly two operands, otherwise the
     * SQL is declared invalid.
     */
    private final class Comparison implements BiConsumer<Filter<AbstractFeature>, SelectionClause> {
        /** The comparison operator symbol. */
        private final String operator;

        /** Creates a new handler for the given operator. */
        Comparison(final String operator) {
            this.operator = operator;
        }

        /** Invoked when a comparison needs to be converted to SQL clause. */
        @Override public void accept(final Filter<AbstractFeature> f, final SelectionClause sql) {
            final BinaryComparisonOperator<AbstractFeature> filter = (BinaryComparisonOperator<AbstractFeature>) f;
            if (filter.isMatchingCase()) {
                writeBinaryOperator(sql, filter, operator);
            } else {
                sql.invalidate();
            }
        }
    }




    /**
     * Handler for converting {@code +}, {@code -}, {@code *} or {@code /} filter into SQL clauses.
     * The filter is expected to contain exactly two operands, otherwise the SQL is declared invalid.
     */
    private final class Arithmetic implements BiConsumer<Expression<AbstractFeature,?>, SelectionClause> {
        /** The arithmetic operator symbol. */
        private final String operator;

        /** Creates a new handler for the given operator. */
        Arithmetic(final String operator) {
            this.operator = operator;
        }

        /** Invoked when an arithmetic expression needs to be converted to SQL clause. */
        @Override public void accept(final Expression<AbstractFeature,?> expression, final SelectionClause sql) {
            writeParameters(sql, expression.getParameters(), operator, true);
        }
    }




    /**
     * Appends a function name with an arbitrary number of parameters (potentially zero).
     * This method stops immediately if a parameter cannot be expressed in SQL, leaving
     * the trailing part of the SQL in an invalid state. Callers should check if this is
     * the case by invoking {@link SelectionClause#isInvalid()} after this method call.
     */
    private final class Function implements BiConsumer<Filter<AbstractFeature>, SelectionClause> {
        /** Name the function. */
        final String name;

        /** Creates a function of the given name. */
        Function(final String name) {
            this.name = name;
        }

        /**
         * Writes the function as an SQL statement. The function is usually spatial (with geometry operands),
         * but not necessarily. If the given {@code filter} contains geometry operands specified as literal,
         * {@link org.apache.sis.filter.Optimization} should have already transformed the literals to the CRS
         * of the geometry column when those CRS are known. Therefore, it should not be needed to perform any
         * geometry transformation in this method.
         */
        @Override public void accept(final Filter<AbstractFeature> filter, final SelectionClause sql) {
            sql.appendSpatialFunction(name);
            final List<Expression<AbstractFeature, ?>> expressions = filter.getExpressions();
            if (SelectionClause.REPLACE_UNSPECIFIED_CRS) {
                for (Expression<AbstractFeature,?> exp : expressions) {
                    if (exp instanceof ValueReference<?,?>) {
                        if (sql.acceptColumnCRS((ValueReference<AbstractFeature,?>) exp)) {
                            break;
                        }
                    }
                }
            }
            writeParameters(sql, expressions, ", ", false);
            sql.clearColumnCRS();
        }
    }
}
