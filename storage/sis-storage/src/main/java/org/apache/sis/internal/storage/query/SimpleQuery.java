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
package org.apache.sis.internal.storage.query;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.util.GenericName;
import org.apache.sis.filter.InvalidExpressionException;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.builder.PropertyTypeBuilder;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.iso.Names;

// Branch-dependent imports
import org.opengis.feature.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.sort.SortBy;


/**
 * Mimics {@code SQL SELECT} statements using OGC Filter and Expressions.
 * Information stored in this query can be used directly with {@link java.util.stream.Stream} API.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 *
 * @todo Rename {@code FeatureQuery}.
 */
public class SimpleQuery extends Query implements Cloneable {
    /**
     * Sentinel limit value for queries of unlimited length.
     * This value can be given to {@link #setLimit(long)} or retrieved from {@link #getLimit()}.
     */
    public static final long UNLIMITED = -1;

    /**
     * The columns to retrieve, or {@code null} if all columns shall be included in the query.
     *
     * @see #getColumns()
     * @see #setColumns(Column...)
     */
    private Column[] columns;

    /**
     * The filter for trimming feature instances.
     *
     * @see #getFilter()
     * @see #setFilter(Filter)
     */
    private Filter filter;

    /**
     * The number of records to skip from the beginning.
     *
     * @see #getOffset()
     * @see #setOffset(long)
     * @see java.util.stream.Stream#skip(long)
     */
    private long skip;

    /**
     * The maximum number of records contained in the {@code FeatureSet}.
     *
     * @see #getLimit()
     * @see #setLimit(long)
     * @see java.util.stream.Stream#limit(long)
     */
    private long limit;

    /**
     * The expressions to use for sorting the feature instances.
     *
     * @see #getSortBy()
     * @see #setSortBy(SortBy...)
     */
    private SortBy[] sortBy;

    /**
     * Hint use by resources to optimise returned features.
     * Different stores makes use of vector tiles of different scales.
     */
    private Quantity<Length> linearResolution;

    /**
     * Creates a new query retrieving no column and applying no filter.
     */
    public SimpleQuery() {
        filter = Filter.INCLUDE;
        sortBy = SortBy.UNSORTED;
        limit  = UNLIMITED;
    }

    /**
     * Sets the columns to retrieve, or {@code null} if all columns shall be included in the query.
     * A query column may use a simple or complex expression and an alias to create a new type of
     * property in the returned features.
     * This is equivalent to the column names in the {@code SELECT} clause of a SQL statement.
     *
     * @param  columns  columns to retrieve, or {@code null} to retrieve all properties.
     * @throws IllegalArgumentException if a column or an alias is duplicated.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setColumns(Column... columns) {
        if (columns != null) {
            ArgumentChecks.ensureNonEmpty("columns", columns);
            columns = columns.clone();
            final Map<Object,Integer> uniques = new LinkedHashMap<>(Containers.hashMapCapacity(columns.length));
            for (int i=0; i<columns.length; i++) {
                final Column c = columns[i];
                ArgumentChecks.ensureNonNullElement("columns", i, c);
                final Object key = c.alias != null ? c.alias : c.expression;
                final Integer p = uniques.putIfAbsent(key, i);
                if (p != null) {
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.DuplicatedQueryProperty_3, key, p, i));
                }
            }
        }
        this.columns = columns;
    }

    /**
     * Returns the columns to retrieve, or {@code null} if all columns shall be included in the query.
     * This is the columns specified in the last call to {@link #setColumns(Column...)}.
     *
     * @return columns to retrieve, or {@code null} to retrieve all feature properties.
     */
    public List<Column> getColumns() {
        return UnmodifiableArrayList.wrap(columns);
    }

    /**
     * Sets a filter for trimming feature instances.
     * Features that do not pass the filter are discarded.
     * Discarded features are not counted for the {@linkplain #setLimit(long) query limit}.
     *
     * @param  filter  the filter, or {@link Filter#INCLUDE} if none.
     */
    public void setFilter(final Filter filter) {
        ArgumentChecks.ensureNonNull("filter", filter);
        this.filter = filter;
    }

    /**
     * Returns the filter for trimming feature instances.
     * This is the value specified in the last call to {@link #setFilter(Filter)}.
     *
     * @return the filter, or {@link Filter#INCLUDE} if none.
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Sets the number of records to skip from the beginning.
     * Offset and limit are often combined to obtain paging.
     * The offset can not be negative.
     *
     * <p>Note that setting this property can be costly on parallelized streams.
     * See {@link java.util.stream.Stream#skip(long)} for more information.</p>
     *
     * @param  skip  the number of records to skip from the beginning.
     */
    public void setOffset(final long skip) {
        ArgumentChecks.ensurePositive("skip", skip);
        this.skip = skip;
    }

    /**
     * Returns the number of records to skip from the beginning.
     * This is the value specified in the last call to {@link #setOffset(long)}.
     *
     * @return the number of records to skip from the beginning.
     */
    public long getOffset() {
        return skip;
    }

    /**
     * Set the maximum number of records contained in the {@code FeatureSet}.
     * Offset and limit are often combined to obtain paging.
     *
     * <p>Note that setting this property can be costly on parallelized streams.
     * See {@link java.util.stream.Stream#limit(long)} for more information.</p>
     *
     * @param  limit  maximum number of records contained in the {@code FeatureSet}, or {@link #UNLIMITED}.
     */
    public void setLimit(final long limit) {
        if (limit != UNLIMITED) {
            ArgumentChecks.ensurePositive("limit", limit);
        }
        this.limit = limit;
    }

    /**
     * Returns the maximum number of records contained in the {@code FeatureSet}.
     * This is the value specified in the last call to {@link #setLimit(long)}.
     *
     * @return maximum number of records contained in the {@code FeatureSet}, or {@link #UNLIMITED}.
     */
    public long getLimit() {
        return limit;
    }

    /**
     * Sets the expressions to use for sorting the feature instances.
     * {@code SortBy} objects are used to order the {@link org.opengis.feature.Feature} instances
     * returned by the {@link org.apache.sis.storage.FeatureSet}.
     * {@code SortBy} clauses are applied in declaration order, like SQL.
     *
     * @param  sortBy  expressions to use for sorting the feature instances.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setSortBy(SortBy... sortBy) {
        if (sortBy == null || sortBy.length == 0) {
            sortBy = SortBy.UNSORTED;
        } else {
            sortBy = sortBy.clone();
            for (int i=0; i < sortBy.length; i++) {
                ArgumentChecks.ensureNonNullElement("sortBy", i, sortBy[i]);
            }
        }
        this.sortBy = sortBy;
    }

    /**
     * Returns the expressions to use for sorting the feature instances.
     * They are the values specified in the last call to {@link #setSortBy(SortBy...)}.
     *
     * @return expressions to use for sorting the feature instances, or an empty array if none.
     */
    public SortBy[] getSortBy() {
        return (sortBy.length == 0) ? SortBy.UNSORTED : sortBy.clone();
    }

    /**
     * Set linear resolution hint.
     * This hing is optional, resources may ignore it.
     *
     * @param linearResolution can be null.
     */
    public void setLinearResolution(Quantity<Length> linearResolution) {
        this.linearResolution = linearResolution;
    }

    /**
     * Get linear resolution hint.
     *
     * @return linear resolution, may be null.
     */
    public Quantity<Length> getLinearResolution() {
        return linearResolution;
    }

    /**
     * A property or expression to be retrieved by a {@code Query}, together with the name to assign to it.
     * Columns can be given to the {@link SimpleQuery#setColumns(Column...)} method.
     */
    public static class Column {
        /**
         * The literal, property name or more complex expression to be retrieved by a {@code Query}.
         */
        public final Expression expression;

        /**
         * The name to assign to the expression result, or {@code null} if unspecified.
         */
        public final GenericName alias;

        /**
         * Creates a new column with the given expression and no name.
         *
         * @param expression  the literal, property name or expression to be retrieved by a {@code Query}.
         */
        public Column(final Expression expression) {
            ArgumentChecks.ensureNonNull("expression", expression);
            this.expression = expression;
            this.alias = null;
        }

        /**
         * Creates a new column with the given expression and the given name.
         *
         * @param expression  the literal, property name or expression to be retrieved by a {@code Query}.
         * @param alias       the name to assign to the expression result, or {@code null} if unspecified.
         */
        public Column(final Expression expression, final GenericName alias) {
            ArgumentChecks.ensureNonNull("expression", expression);
            this.expression = expression;
            this.alias = alias;
        }

        /**
         * Creates a new column with the given expression and the given name.
         * This constructor creates a {@link org.opengis.util.LocalName} from the given string.
         *
         * @param expression  the literal, property name or expression to be retrieved by a {@code Query}.
         * @param alias       the name to assign to the expression result, or {@code null} if unspecified.
         */
        public Column(final Expression expression, final String alias) {
            ArgumentChecks.ensureNonNull("expression", expression);
            this.expression = expression;
            this.alias = (alias != null) ? Names.createLocalName(null, null, alias) : null;
        }

        /**
         * Adds in the given builder the type of results computed by this column.
         *
         * @param  column     index of this column. Used for error message only.
         * @param  valueType  the type of features to be evaluated by the expression in this column.
         * @param  addTo      where to add the type of properties evaluated by expression in this column.
         * @throws IllegalArgumentException if this method can operate only on some feature types
         *         and the given type is not one of them.
         * @throws InvalidExpressionException if this method can not determine the result type of the expression
         *         in this column. It may be because that expression is backed by an unsupported implementation.
         *
         * @see SimpleQuery#expectedType(FeatureType)
         */
        final void expectedType(final int column, final FeatureType valueType, final FeatureTypeBuilder addTo) {
            final PropertyTypeBuilder resultType = FeatureExpression.expectedType(expression, valueType, addTo);
            if (resultType == null) {
                throw new InvalidExpressionException(expression, column);
            }
            if (alias != null && !alias.equals(resultType.getName())) {
                resultType.setName(alias);
            }
        }

        /**
         * Returns a hash code value for this column.
         *
         * @return a hash code value.
         */
        @Override
        public int hashCode() {
            return 37 * expression.hashCode() + Objects.hashCode(alias);
        }

        /**
         * Compares this column with the given object for equality.
         *
         * @param  obj  the object to compare with this column.
         * @return whether the two objects are equal.
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                final Column other = (Column) obj;
                return expression.equals(other.expression) && Objects.equals(alias, other.alias);
            }
            return false;
        }

        /**
         * Returns a string representation of this column for debugging purpose.
         *
         * @return a string representation of this column.
         */
        @Override
        public String toString() {
            final StringBuilder buffer = new StringBuilder();
            buffer.append(getClass().getSimpleName()).append('[');      // Class name without enclosing class.
            appendTo(buffer);
            return buffer.append(']').toString();
        }

        /**
         * Appends a string representation of this column in the given buffer.
         */
        final void appendTo(final StringBuilder buffer) {
            buffer.append(Classes.getShortClassName(expression));       // Class name with enclosing class if any.
            if (alias != null) {
                buffer.append(" AS “").append(alias).append('”');
            }
        }
    }

    /**
     * Applies this query on the given feature set. The default implementation executes the query using the default
     * {@link java.util.stream.Stream} methods.  Queries executed by this method may not benefit from accelerations
     * provided for example by databases. This method should be used only as a fallback when the query can not be
     * executed natively by {@link FeatureSet#subset(Query)}.
     *
     * <p>The returned {@code FeatureSet} does not cache the resulting {@code Feature} instances;
     * the query is processed on every call to the {@link FeatureSet#features(boolean)} method.</p>
     *
     * @param  source  the set of features to filter, sort or process.
     * @return a view over the given feature set containing only the filtered feature instances.
     */
    public FeatureSet execute(final FeatureSet source) {
        ArgumentChecks.ensureNonNull("source", source);
        return new FeatureSubset(source, clone());
    }

    /**
     * Returns the type or values evaluated by this query when executed on features of the given type.
     *
     * @param  valueType  the type of features to be evaluated by the expressions in this query.
     * @return type resulting from expressions evaluation (never null).
     * @throws IllegalArgumentException if this method can operate only on some feature types
     *         and the given type is not one of them.
     * @throws InvalidExpressionException if this method can not determine the result type of an expression
     *         in this query. It may be because that expression is backed by an unsupported implementation.
     */
    final FeatureType expectedType(final FeatureType valueType) {
        if (columns == null) {
            return valueType;           // All columns included: result is of the same type.
        }
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName(valueType.getName());
        for (int i=0; i<columns.length; i++) {
            columns[i].expectedType(i, valueType, ftb);
        }
        return ftb.build();
    }

    /**
     * Returns a clone of this query.
     *
     * @return a clone of this query.
     */
    @Override
    public SimpleQuery clone() {
        /*
         * Implementation note: no need to clone the arrays. It is safe to share the same array instances
         * because this class does not modify them and does not return them directly to the user.
         */
        try {
            return (SimpleQuery) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns a hash code value for this query.
     *
     * @return a hash value for this query.
     */
    @Override
    public int hashCode() {
        return 97 * Arrays.hashCode(columns) + 31 * filter.hashCode()
                + 7 * Arrays.hashCode(sortBy) + Long.hashCode(limit ^ skip);
    }

    /**
     * Compares this query with the given object for equality.
     *
     * @param  obj  the object to compare with this query.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            final SimpleQuery other = (SimpleQuery) obj;
            return skip  == other.skip &&
                   limit == other.limit &&
                   filter.equals(other.filter) &&
                   Arrays.equals(columns, other.columns) &&
                   Arrays.equals(sortBy,  other.sortBy);
        }
        return false;
    }

    /**
     * Returns a textual representation looking like an SQL Select query.
     *
     * @return textual representation of this query.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(80);
        sb.append("SELECT ");
        if (columns != null) {
            for (int i=0; i<columns.length; i++) {
                if (i != 0) sb.append(", ");
                columns[i].appendTo(sb);
            }
        } else {
            sb.append('*');
        }
        if (filter != Filter.INCLUDE) {
            sb.append(" WHERE ").append(filter);
        }
        if (sortBy != SortBy.UNSORTED) {
            sb.append(" ORDER BY ");
            for (int i=0; i<sortBy.length; i++) {
                if (i != 0) sb.append(", ");
                sb.append(sortBy[i]);
            }
        }
        if (limit != UNLIMITED) {
            sb.append(" LIMIT ").append(limit);
        }
        if (skip != 0) {
            sb.append(" OFFSET ").append(skip);
        }
        return sb.toString();
    }
}
