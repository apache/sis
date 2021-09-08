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
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;
import org.opengis.filter.SortBy;
import org.opengis.filter.SortProperty;
import org.opengis.filter.InvalidFilterValueException;


/**
 * Mimics {@code SQL SELECT} statements using OGC Filter and Expressions.
 * Information stored in this query can be used directly with {@link java.util.stream.Stream} API.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public class FeatureQuery extends Query implements Cloneable {
    /**
     * Sentinel limit value for queries of unlimited length.
     * This value can be given to {@link #setLimit(long)} or retrieved from {@link #getLimit()}.
     */
    public static final long UNLIMITED = -1;

    /**
     * The properties to retrieve, or {@code null} if all properties shall be included in the query.
     * In a database, "properties" are table columns.
     * Subset of columns is called <cite>projection</cite> in relational database terminology.
     *
     * @see #getProjection()
     * @see #setProjection(NamedExpression[])
     */
    private NamedExpression[] projection;

    /**
     * The filter for trimming feature instances.
     * In a database, "feature instances" are table rows.
     * Subset of rows is called <cite>selection</cite> in relational database terminology.
     *
     * @see #getSelection()
     * @see #setSelection(Filter)
     */
    private Filter<? super Feature> selection;

    /**
     * The number of feature instances to skip from the beginning.
     *
     * @see #getOffset()
     * @see #setOffset(long)
     * @see java.util.stream.Stream#skip(long)
     */
    private long skip;

    /**
     * The maximum number of feature instances contained in the {@code FeatureSet}.
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
     * @see #setSortBy(SortBy)
     */
    private SortBy<Feature> sortBy;

    /**
     * Hint used by resources to optimize returned features.
     * Different stores makes use of vector tiles of different scales.
     * A {@code null} value means to query data at their full resolution.
     *
     * @see #getLinearResolution()
     * @see #setLinearResolution(Quantity)
     */
    private Quantity<Length> linearResolution;

    /**
     * Creates a new query retrieving no property and applying no filter.
     */
    public FeatureQuery() {
        selection = Filter.include();
        limit = UNLIMITED;
    }

    /**
     * Sets the properties to retrieve, or {@code null} if all properties shall be included in the query.
     * A query column may use a simple or complex expression and an alias to create a new type of property
     * in the returned features.
     *
     * <p>This is equivalent to the column names in the {@code SELECT} clause of a SQL statement.
     * Subset of columns is called <cite>projection</cite> in relational database terminology.</p>
     *
     * @param  projection  properties to retrieve, or {@code null} to retrieve all properties.
     * @throws IllegalArgumentException if a property or an alias is duplicated.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setProjection(NamedExpression... projection) {
        if (projection != null) {
            ArgumentChecks.ensureNonEmpty("projection", projection);
            projection = projection.clone();
            final Map<Object,Integer> uniques = new LinkedHashMap<>(Containers.hashMapCapacity(projection.length));
            for (int i=0; i<projection.length; i++) {
                final NamedExpression c = projection[i];
                ArgumentChecks.ensureNonNullElement("projection", i, c);
                final Object key = c.alias != null ? c.alias : c.expression;
                final Integer p = uniques.putIfAbsent(key, i);
                if (p != null) {
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.DuplicatedQueryProperty_3, key, p, i));
                }
            }
        }
        this.projection = projection;
    }

    /**
     * Returns the properties to retrieve, or {@code null} if all properties shall be included in the query.
     * This is the list of expressions specified in the last call to {@link #setProjection(NamedExpression[])}.
     *
     * @return properties to retrieve, or {@code null} to retrieve all feature properties.
     */
    public List<NamedExpression> getProjection() {
        return UnmodifiableArrayList.wrap(projection);
    }

    /**
     * Sets a filter for trimming feature instances.
     * Features that do not pass the filter are discarded.
     * Discarded features are not counted for the {@linkplain #setLimit(long) query limit}.
     *
     * @param  selection  the filter, or {@link Filter#include()} if none.
     */
    public void setSelection(final Filter<? super Feature> selection) {
        ArgumentChecks.ensureNonNull("selection", selection);
        this.selection = selection;
    }

    /**
     * Returns the filter for trimming feature instances.
     * This is the value specified in the last call to {@link #setSelection(Filter)}.
     *
     * @return the filter, or {@link Filter#include()} if none.
     */
    public Filter<? super Feature> getSelection() {
        return selection;
    }

    /**
     * Sets the number of feature instances to skip from the beginning.
     * Offset and limit are often combined to obtain paging.
     * The offset can not be negative.
     *
     * <p>Note that setting this property can be costly on parallelized streams.
     * See {@link java.util.stream.Stream#skip(long)} for more information.</p>
     *
     * @param  skip  the number of feature instances to skip from the beginning.
     */
    public void setOffset(final long skip) {
        ArgumentChecks.ensurePositive("skip", skip);
        this.skip = skip;
    }

    /**
     * Returns the number of feature instances to skip from the beginning.
     * This is the value specified in the last call to {@link #setOffset(long)}.
     *
     * @return the number of feature instances to skip from the beginning.
     */
    public long getOffset() {
        return skip;
    }

    /**
     * Set the maximum number of feature instances contained in the {@code FeatureSet}.
     * Offset and limit are often combined to obtain paging.
     *
     * <p>Note that setting this property can be costly on parallelized streams.
     * See {@link java.util.stream.Stream#limit(long)} for more information.</p>
     *
     * @param  limit  maximum number of feature instances contained in the {@code FeatureSet}, or {@link #UNLIMITED}.
     */
    public void setLimit(final long limit) {
        if (limit != UNLIMITED) {
            ArgumentChecks.ensurePositive("limit", limit);
        }
        this.limit = limit;
    }

    /**
     * Returns the maximum number of feature instances contained in the {@code FeatureSet}.
     * This is the value specified in the last call to {@link #setLimit(long)}.
     *
     * @return maximum number of feature instances contained in the {@code FeatureSet}, or {@link #UNLIMITED}.
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
     * @param  properties  expressions to use for sorting the feature instances,
     *                     or {@code null} or an empty array if none.
     */
    @SafeVarargs
    public final void setSortBy(final SortProperty<Feature>... properties) {
        SortBy<Feature> sortBy = null;
        if (properties != null && properties.length != 0) {
            sortBy = new SortByComparator(properties);
        }
        setSortBy(sortBy);
    }

    /**
     * Sets the expressions to use for sorting the feature instances.
     * {@code SortBy} objects are used to order the {@link org.opengis.feature.Feature} instances
     * returned by the {@link org.apache.sis.storage.FeatureSet}.
     *
     * @param  sortBy  expressions to use for sorting the feature instances, or {@code null} if none.
     */
    public void setSortBy(final SortBy<Feature> sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * Returns the expressions to use for sorting the feature instances.
     * They are the values specified in the last call to {@link #setSortBy(SortBy)}.
     *
     * @return expressions to use for sorting the feature instances, or {@code null} if none.
     */
    public SortBy<Feature> getSortBy() {
        return sortBy;
    }

    /**
     * Sets the desired spatial resolution of geometries.
     * This property is an optional hint; resources may ignore it.
     *
     * @param  linearResolution  desired spatial resolution, or {@code null} for full resolution.
     */
    public void setLinearResolution(final Quantity<Length> linearResolution) {
        this.linearResolution = linearResolution;
    }

    /**
     * Returns the desired spatial resolution of geometries.
     * A {@code null} value means that data are queried at their full resolution.
     *
     * @return  desired spatial resolution, or {@code null} for full resolution.
     */
    public Quantity<Length> getLinearResolution() {
        return linearResolution;
    }

    /**
     * An expression to be retrieved by a {@code Query}, together with the name to assign to it.
     * In relational database terminology, subset of columns is called <cite>projection</cite>.
     * Columns can be given to the {@link FeatureQuery#setProjection(NamedExpression[])} method.
     */
    public static class NamedExpression {
        /**
         * The literal, value reference or more complex expression to be retrieved by a {@code Query}.
         */
        public final Expression<? super Feature, ?> expression;

        /**
         * The name to assign to the expression result, or {@code null} if unspecified.
         */
        public final GenericName alias;

        /**
         * Creates a new column with the given expression and no name.
         *
         * @param expression  the literal, value reference or expression to be retrieved by a {@code Query}.
         */
        public NamedExpression(final Expression<? super Feature, ?> expression) {
            ArgumentChecks.ensureNonNull("expression", expression);
            this.expression = expression;
            this.alias = null;
        }

        /**
         * Creates a new column with the given expression and the given name.
         *
         * @param expression  the literal, value reference or expression to be retrieved by a {@code Query}.
         * @param alias       the name to assign to the expression result, or {@code null} if unspecified.
         */
        public NamedExpression(final Expression<? super Feature, ?> expression, final GenericName alias) {
            ArgumentChecks.ensureNonNull("expression", expression);
            this.expression = expression;
            this.alias = alias;
        }

        /**
         * Creates a new column with the given expression and the given name.
         * This constructor creates a {@link org.opengis.util.LocalName} from the given string.
         *
         * @param expression  the literal, value reference or expression to be retrieved by a {@code Query}.
         * @param alias       the name to assign to the expression result, or {@code null} if unspecified.
         */
        public NamedExpression(final Expression<? super Feature, ?> expression, final String alias) {
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
         * @throws InvalidFilterValueException if this method can not determine the result type of the expression
         *         in this column. It may be because that expression is backed by an unsupported implementation.
         *
         * @see FeatureQuery#expectedType(FeatureType)
         */
        final void expectedType(final int column, final FeatureType valueType, final FeatureTypeBuilder addTo) {
            final PropertyTypeBuilder resultType = FeatureExpression.expectedType(expression, valueType, addTo);
            if (resultType == null) {
                throw new InvalidFilterValueException(Resources.format(Resources.Keys.InvalidExpression_2,
                            expression.getFunctionName().toInternationalString(), column));
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
                final NamedExpression other = (NamedExpression) obj;
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
            if (expression instanceof Literal<?,?>) {
                buffer.append('“').append(((Literal<?,?>) expression).getValue()).append('”');
            } else if (expression instanceof ValueReference<?,?>) {
                buffer.append(((ValueReference<?,?>) expression).getXPath());
            } else {
                buffer.append(Classes.getShortClassName(expression));   // Class name with enclosing class if any.
            }
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
     * Returns the type of values evaluated by this query when executed on features of the given type.
     *
     * @param  valueType  the type of features to be evaluated by the expressions in this query.
     * @return type resulting from expressions evaluation (never null).
     * @throws IllegalArgumentException if this method can operate only on some feature types
     *         and the given type is not one of them.
     * @throws InvalidFilterValueException if this method can not determine the result type of an expression
     *         in this query. It may be because that expression is backed by an unsupported implementation.
     */
    final FeatureType expectedType(final FeatureType valueType) {
        if (projection == null) {
            return valueType;           // All columns included: result is of the same type.
        }
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName(valueType.getName());
        for (int i=0; i<projection.length; i++) {
            projection[i].expectedType(i, valueType, ftb);
        }
        return ftb.build();
    }

    /**
     * Returns a clone of this query.
     *
     * @return a clone of this query.
     */
    @Override
    public FeatureQuery clone() {
        /*
         * Implementation note: no need to clone the arrays. It is safe to share the same array instances
         * because this class does not modify them and does not return them directly to the user.
         */
        try {
            return (FeatureQuery) super.clone();
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
        return 97 * Arrays.hashCode(projection) + 31 * selection.hashCode()
              + 7 * Objects.hashCode(sortBy) + Long.hashCode(limit ^ skip)
              + 3 * Objects.hashCode(linearResolution);
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
            final FeatureQuery other = (FeatureQuery) obj;
            return skip  == other.skip &&
                   limit == other.limit &&
                   selection.equals(other.selection) &&
                   Arrays .equals(projection,       other.projection) &&
                   Objects.equals(sortBy,           other.sortBy) &&
                   Objects.equals(linearResolution, other.linearResolution);
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
        if (projection != null) {
            for (int i=0; i<projection.length; i++) {
                if (i != 0) sb.append(", ");
                projection[i].appendTo(sb);
            }
        } else {
            sb.append('*');
        }
        if (selection != Filter.include()) {
            sb.append(" WHERE ").append(selection);
        }
        if (sortBy != null) {
            String separator = " ORDER BY ";
            for (final SortProperty<Feature> p : sortBy.getSortProperties()) {
                sb.append(separator);
                separator = ", ";
                sb.append(p.getValueReference().getXPath()).append(' ').append(p.getSortOrder());
            }
        }
        if (linearResolution != null) {
            sb.append(" RESOLUTION ").append(linearResolution);
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
