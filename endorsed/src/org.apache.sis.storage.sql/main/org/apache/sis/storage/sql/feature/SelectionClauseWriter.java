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
import java.util.HashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.sql.Types;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.sis.filter.visitor.FunctionIdentifier;
import org.apache.sis.filter.visitor.FunctionNames;
import org.apache.sis.filter.visitor.Visitor;
import org.apache.sis.metadata.sql.internal.shared.Reflection;

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
            final var filter = (BetweenComparisonOperator<AbstractFeature>) f;
            /* Nothing to append */  if (write(sql, filter.getExpression()))    return;
            sql.append(" BETWEEN "); if (write(sql, filter.getLowerBoundary())) return;
            sql.append(" AND ");         write(sql, filter.getUpperBoundary());
            sql.declareFunction(JDBCType.BOOLEAN);
        });
        setNullAndNilHandlers((filter, sql) -> {
            final List<Expression<AbstractFeature, ?>> parameters = filter.getExpressions();
            if (parameters.size() == 1) {
                write(sql, parameters.get(0));
                sql.append(" IS NULL");
                sql.declareFunction(JDBCType.BOOLEAN);
            } else {
                sql.invalidate();
            }
        });
        /*
         * Spatial filters.
         */
        setFilterHandler(SpatialOperatorName.CONTAINS,   new SpatialFilter(FunctionNames.ST_Contains));
        setFilterHandler(SpatialOperatorName.CROSSES,    new SpatialFilter(FunctionNames.ST_Crosses));
        setFilterHandler(SpatialOperatorName.DISJOINT,   new SpatialFilter(FunctionNames.ST_Disjoint));
        setFilterHandler(SpatialOperatorName.EQUALS,     new SpatialFilter(FunctionNames.ST_Equals));
        setFilterHandler(SpatialOperatorName.INTERSECTS, new SpatialFilter(FunctionNames.ST_Intersects));
        setFilterHandler(SpatialOperatorName.OVERLAPS,   new SpatialFilter(FunctionNames.ST_Overlaps));
        setFilterHandler(SpatialOperatorName.TOUCHES,    new SpatialFilter(FunctionNames.ST_Touches));
        setFilterHandler(SpatialOperatorName.WITHIN,     new SpatialFilter(FunctionNames.ST_Within));
        /*
         * Mathematical functions.
         */
        addAllOf(org.apache.sis.filter.math.Function.class);
        /*
         * Expression visitor.
         */
        setExpressionHandler(FunctionNames.Add,      new Arithmetic(" + "));
        setExpressionHandler(FunctionNames.Subtract, new Arithmetic(" - "));
        setExpressionHandler(FunctionNames.Divide,   new Arithmetic(" / "));
        setExpressionHandler(FunctionNames.Multiply, new Arithmetic(" * "));
        setExpressionHandler(FunctionNames.Literal, (e,sql) -> sql.appendLiteral(((Literal<AbstractFeature,?>) e).getValue()));
        setExpressionHandler(FunctionNames.ValueReference, (e,sql) -> sql.appendColumnName(((ValueReference<AbstractFeature,?>) e).getXPath()));
        setExpressionHandler(FunctionNames.PropertyName, getExpressionHandler(FunctionNames.ValueReference));
    }

    /**
     * Adds as functions all values defined by the specified enumeration.
     */
    private <E extends Enum<E> & FunctionIdentifier> void addAllOf(final Class<E> functions) {
        for (E id : functions.getEnumConstants()) {
            final String name = id.name();
            setExpressionHandler(name, new Function(id));
        }
    }

    /**
     * Creates a new converter initialized to the same handlers as the specified converter.
     * This constructor is for implementations of {@link #duplicate(boolean, boolean)}.
     * The given source is usually {@link #DEFAULT}.
     *
     * @param  source           the converter from which to copy the handlers.
     * @param  copyFilters      whether to copy the map of filter handlers.
     * @param  copyExpressions  whether to copy the map of expression handlers.
     */
    protected SelectionClauseWriter(SelectionClauseWriter source, boolean copyFilters, boolean copyExpressions) {
        super(source, copyFilters, copyExpressions);
    }

    /**
     * Creates a new converter of the same class as {@code this} and initialized with the same data.
     * This method is invoked before to remove handlers for functions that are unsupported on the target
     * database software.
     *
     * @param  copyFilters      whether to copy the map of filter handlers.
     * @param  copyExpressions  whether to copy the map of expression handlers.
     * @return a converter initialized to a copy of {@code this}.
     */
    protected SelectionClauseWriter duplicate(boolean copyFilters, boolean copyExpressions) {
        return new SelectionClauseWriter(this, copyFilters, copyExpressions);
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
        boolean failure = false;
        final var unsupportedFilters = new HashMap<String, Enum<?>>(16);
        final var unsupportedExpressions = new HashSet<String>();
        final var accessors = GeometryEncoding.initial();
        try (Connection c = database.source.getConnection()) {
            final DatabaseMetaData metadata = c.getMetaData();
            final boolean lowerCase = metadata.storesLowerCaseIdentifiers();
            final boolean upperCase = metadata.storesUpperCaseIdentifiers();
            /*
             * Get the names of all spatial filters for which a handler is registered.
             * These filters are initially assumed unsupported by the target database.
             * We then iterate over the (potentially large) list of supported filters
             * for removing from the map all the filters that we found supported.
             * After that loop, only truly unsupported items should be remaining.
             */
            for (final SpatialOperatorName id : SpatialOperatorName.values()) {
                final BiConsumer<Filter<AbstractFeature>, SelectionClause> handler = getFilterHandler(id);
                if (handler instanceof SpatialFilter) {
                    String name = ((SpatialFilter) handler).name;
                    if (lowerCase) name = name.toLowerCase(Locale.US);
                    if (upperCase) name = name.toUpperCase(Locale.US);
                    unsupportedFilters.put(name, id);
                }
            }
            final String prefix = database.escapeWildcards(lowerCase ? "st_" : "ST_");
            try (ResultSet r = metadata.getFunctions(database.catalogOfSpatialTables,
                                                     database.schemaOfSpatialTables,
                                                     prefix + '%'))
            {
                while (r.next()) {
                    String function = r.getString(Reflection.FUNCTION_NAME);
                    GeometryEncoding.checkSupport(accessors, function);
                    unsupportedFilters.remove(function);
                }
            }
            /*
             * Iterate over all functions (math, etc.) for which a handler is registered.
             * For each of these function, get the parameter types and return value type.
             * We check if a function is supported not only by searching for its name,
             * but also by checking the arguments.
             */
            for (final var entry : expressions.entrySet()) {
                final BiConsumer<Expression<AbstractFeature,?>, SelectionClause> handler = entry.getValue();
                if (handler instanceof Function) {
                    final FunctionIdentifier id = ((Function) handler).function;
                    final int[] signature = id.getSignature();   // May be null.
                    boolean isSupported = false;
                    String specificName = "";
                    String name = id.name();
                    if (lowerCase) name = name.toLowerCase(Locale.US);
                    if (upperCase) name = name.toUpperCase(Locale.US);
                    try (ResultSet r = metadata.getFunctionColumns(null, null, name, "%")) {
                        while (r.next()) {
                            if (!specificName.equals(specificName = r.getString(Reflection.SPECIFIC_NAME))) {
                                if (isSupported) break;     // Found a supported variant of the function.
                                isSupported = true;
                            } else if (!isSupported) {
                                continue;   // Continue the search for the next overload variant.
                            }
                            switch (r.getShort(Reflection.COLUMN_TYPE)) {
                                case DatabaseMetaData.functionColumnIn:
                                case DatabaseMetaData.functionReturn: {
                                    if (signature == null) continue;
                                    final int n = r.getInt(Reflection.ORDINAL_POSITION);
                                    if (n >= 0 && n < signature.length) {
                                        int type = r.getInt(Reflection.DATA_TYPE);
                                        switch (type) {
                                            case Types.SMALLINT:  // Derby does not support `TINYINT`.
                                            case Types.TINYINT:
                                            case Types.BIT:   type = Types.BOOLEAN; break;
                                            case Types.REAL:
                                            case Types.FLOAT: type = Types.DOUBLE; break;
                                        }
                                        if (signature[n] == type) continue;
                                    }
                                }
                            }
                            isSupported = false;
                            // Continue because the `ResultSet` may return many overload variants.
                        }
                    }
                    if (!isSupported) {
                        unsupportedExpressions.add(entry.getKey());
                    }
                }
            }
        } catch (SQLException e) {
            database.listeners.warning(e);
            failure = true;
        }
        /*
         * The remaining items in the `unsupported` collection are functions that are unsupported by the database.
         * If this collection is empty, then all functions are supported and we can use `this` with no change.
         */
        database.setGeometryEncodingFunctions(accessors);
        final boolean copyFilters     = failure || !unsupportedFilters.isEmpty();
        final boolean copyExpressions = failure || !unsupportedExpressions.isEmpty();
        if (copyFilters | copyExpressions) {
            final SelectionClauseWriter copy = duplicate(copyFilters, copyExpressions);
            copy.removeFilterHandlers(unsupportedFilters.values());
            copy.removeFunctionHandlers(unsupportedExpressions);
            if (failure) {
                copy.filters.values().removeIf((handler) -> handler instanceof SpatialFilter);
                copy.expressions.values().removeIf((handler) -> handler instanceof Function);
            }
            return copy;
        }
        return this;
    }

    /**
     * Invoked when an unsupported filter is found. The SQL string is marked as invalid and
     * may be truncated (later) to the length that it had the last time that it was valid.
     */
    @Override
    protected final void typeNotFound(Enum<?> type, Filter<AbstractFeature> filter, SelectionClause sql) {
        sql.invalidate();
    }

    /**
     * Invoked when an unsupported expression is found. The SQL string is marked as invalid
     * and may be truncated (later) to the length that it had the last time that it was valid.
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
     * Executes the registered action for the given expression.
     *
     * <h4>Note on type safety</h4>
     * This method applies a theoretically unsafe cast, which is okay in the context of this class.
     * See <cite>Note on parameterized type</cite> section in {@link Visitor#visit(Filter, Object)}.
     *
     * @param  sql         where to write the result of all actions.
     * @param  expression  the expression for which to execute an action based on its type.
     * @return value of {@link SelectionClause#functionReturnType()}.
     */
    @SuppressWarnings("unchecked")
    final JDBCType writeFunction(final SelectionClause sql, final Expression<? super AbstractFeature, ?> expression) {
        visit((Expression<AbstractFeature, ?>) expression, sql);
        return sql.functionReturnType();
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
     * @param sql         where to append the SQL clause.
     * @param parameters  the expressions to write as parameters.
     * @param separator   the separator to insert between expression.
     * @param binary      whether the list of expressions shall contain exactly 2 elements.
     */
    private void writeParameters(final SelectionClause sql, final List<Expression<AbstractFeature,?>> parameters,
                                 final String separator, final boolean binary)
    {
        final int n = parameters.size();
        if (binary && n != 2) {
            sql.invalidate();
            return;
        }
        // No check for n=0 because we want "()" in that case.
        sql.append('(');
        for (int i=0; i<n; i++) {
            if (i != 0) sql.append(separator);
            if (write(sql, parameters.get(i))) return;
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
            sql.declareFunction(JDBCType.BOOLEAN);
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
            final var filter = (BinaryComparisonOperator<AbstractFeature>) f;
            if (filter.isMatchingCase()) {
                writeBinaryOperator(sql, filter, operator);
                sql.declareFunction(JDBCType.BOOLEAN);
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
            sql.declareFunction(JDBCType.DOUBLE);
        }
    }




    /**
     * Handler for a function with an arbitrary number of parameters (potentially zero).
     * This handler stops immediately if a parameter cannot be expressed in <abbr>SQL</abbr>,
     * leaving the trailing part of the <abbr>SQL</abbr> in an invalid state. Callers should check
     * if this is the case by invoking {@link SelectionClause#isInvalid()} after this method call.
     */
    private final class Function implements BiConsumer<Expression<AbstractFeature,?>, SelectionClause> {
        /** Identification of the function. */
        final FunctionIdentifier function;

        /** The type of values returned by the function. */
        private final JDBCType returnType;

        /** Creates a function. */
        Function(final FunctionIdentifier function) {
            this.function = function;
            returnType = JDBCType.valueOf(function.getSignature()[0]);
        }

        /** Invoked when an expression should be converted to a <abbr>SQL</abbr> clause. */
        @Override public void accept(final Expression<AbstractFeature,?> expression, final SelectionClause sql) {
            sql.append(function.name());
            writeParameters(sql, expression.getParameters(), ", ", false);
            sql.declareFunction(returnType);
        }
    }




    /**
     * Appends a spatial function name followed by an arbitrary number of parameters (potentially zero).
     * This method stops immediately if a parameter cannot be expressed in <abbr>SQL</abbr>, leaving the
     * trailing part of the <abbr>SQL</abbr> in an invalid state. Callers should check if this is the
     * case by invoking {@link SelectionClause#isInvalid()} after this method call.
     */
    private final class SpatialFilter implements BiConsumer<Filter<AbstractFeature>, SelectionClause> {
        /** Name of the function. */
        final String name;

        /** Creates a function of the given name. */
        SpatialFilter(final String name) {
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
            final List<Expression<AbstractFeature, ?>> parameters = filter.getExpressions();
            if (SelectionClause.REPLACE_UNSPECIFIED_CRS) {
                for (Expression<AbstractFeature,?> exp : parameters) {
                    if (exp instanceof ValueReference<?,?>) {
                        if (sql.acceptColumnCRS((ValueReference<AbstractFeature,?>) exp)) {
                            break;
                        }
                    }
                }
            }
            writeParameters(sql, parameters, ", ", false);
            sql.declareFunction(JDBCType.BOOLEAN);
            sql.clearColumnCRS();
        }
    }
}
