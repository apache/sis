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
import java.util.List;
import java.util.Objects;
import org.opengis.util.GenericName;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Debug;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.expression.Expression;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;


/**
 * Mimics {@code SQL SELECT} statements using OGC Filter and Expressions.
 * Information stored in this query can be used directly with {@link java.util.stream.Stream} API.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class SimpleQuery implements Query {
    /**
     * Sentinel limit value for queries of unlimited length.
     * This value can be given to {@link #setLimit(long)} or retrieved from {@link #getLimit()}.
     */
    private static final long UNLIMITED = -1;

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
     * @param columns columns to retrieve, or null to retrieve all properties.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setColumns(Column... columns) {
        columns = columns.clone();
        for (int i=0; i<columns.length; i++) {
            ArgumentChecks.ensureNonNullElement("columns", i, columns[i]);
        }
        this.columns = columns;
    }

    /**
     * Returns the columns to retrieve, or {@code null} if all columns shall be included in the query.
     * This is the columns specified in the last call to {@link #setColumns(Column...)}.
     *
     * @return columns to retrieve, or null to retrieve all feature properties.
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
     * returned by the {@link org.apache.sis.storage.FeatureSet}. {@code SortBy} clauses are applied
     * in declaration order, like SQL.
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
         * Returns the expected property type for this column.
         *
         * @see SimpleQuery#expectedType(FeatureType)
         */
        final PropertyType expectedType(final FeatureType type) {
            PropertyType resultType;
            if (expression instanceof FeatureExpression) {
                resultType = ((FeatureExpression) expression).expectedType(type);
            } else {
                // TODO: remove this hack if we can get more type-safe Expression.
                resultType = expression.evaluate(type, PropertyType.class);
            }
            if (alias != null && !alias.equals(resultType.getName())) {
                // Rename the result type.
                resultType = new FeatureTypeBuilder().addProperty(resultType).setName(alias).build();
                if (!(resultType instanceof AttributeType<?>) && !(resultType instanceof FeatureAssociationRole)) {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalPropertyValueClass_3,
                                alias, AttributeType.class, Classes.getStandardType(Classes.getClass(resultType))));
                }
            }
            return resultType;
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
        @Debug
        @Override
        public String toString() {
            final StringBuilder b = new StringBuilder(getClass().getSimpleName()).append('[');
            if (alias != null) {
                b.append('"').append(alias).append('"');
            }
            return b.append(']').toString();
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
        return new FeatureSubset(source, this);
    }

    /**
     * Returns the expected property type for this query executed on features of the given type.
     */
    final FeatureType expectedType(final FeatureType source) {
        if (columns == null) {
            return source;          // All columns included: result is of the same type.
        }
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName(source.getName());
        for (final Column col : columns) {
            ftb.addProperty(col.expectedType(source));
        }
        return ftb.build();
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
        return true;
    }
}
