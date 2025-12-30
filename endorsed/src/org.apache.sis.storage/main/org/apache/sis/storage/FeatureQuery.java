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
package org.apache.sis.storage;

import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.io.Serializable;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.opengis.util.GenericName;
import org.opengis.geometry.Envelope;
import org.apache.sis.feature.FeatureOperations;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.feature.internal.shared.FeatureExpression;
import org.apache.sis.feature.internal.shared.FeatureProjection;
import org.apache.sis.feature.internal.shared.FeatureProjectionBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.filter.Optimization;
import org.apache.sis.filter.visitor.ListingPropertyVisitor;
import org.apache.sis.storage.base.SortByComparator;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.pending.jdk.JDK19;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Emptiable;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.iso.Names;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.InvalidFilterValueException;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;
import org.opengis.filter.SortBy;
import org.opengis.filter.SortProperty;


/**
 * Definition of filtering to apply for fetching a subset of {@link FeatureSet}.
 * This query mimics {@code SQL SELECT} statements using OGC Filter and Expressions.
 * Information stored in this query can be used directly with {@link java.util.stream.Stream} API.
 *
 * <h2>Terminology</h2>
 * This class uses relational database terminology:
 * <ul>
 *   <li>A <dfn>selection</dfn> is a filter choosing the features instances to include in the subset.
 *       In relational databases, a feature instances are mapped to table rows.</li>
 *   <li>A <dfn>projection</dfn> (not to be confused with map projection) is the set of feature properties to keep.
 *       In relational databases, feature properties are mapped to table columns.</li>
 * </ul>
 *
 * <h2>Optional values</h2>
 * All aspects of this query are optional and initialized to "none".
 * Unless otherwise specified, all methods accept a null argument or can return a null value, which means "none".
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   1.1
 */
public class FeatureQuery extends Query implements Cloneable, Emptiable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5841189659773611160L;

    /**
     * Sentinel limit value for queries of unlimited length.
     * This value applies to the {@link #limit} field.
     */
    private static final long UNLIMITED = -1;

    /**
     * The properties to retrieve, or {@code null} if all properties shall be included in the query.
     * In a database, "properties" are table columns.
     * Subset of columns is called <dfn>projection</dfn> in relational database terminology.
     *
     * @see #getProjection()
     * @see #setProjection(NamedExpression[])
     */
    private NamedExpression[] projection;

    /**
     * The filter for trimming feature instances, or {@code null} if none.
     * In a database, "feature instances" are table rows.
     * Subset of rows is called <dfn>selection</dfn> in relational database terminology.
     *
     * @see #getSelection()
     * @see #setSelection(Filter)
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private Filter<? super Feature> selection;

    /**
     * The number of feature instances to skip from the beginning.
     * This is zero if there are no instances to skip.
     *
     * @see #getOffset()
     * @see #setOffset(long)
     * @see java.util.stream.Stream#skip(long)
     */
    private long skip;

    /**
     * The maximum number of feature instances contained in the {@code FeatureSet}.
     * This is {@link #UNLIMITED} if there is no limit.
     *
     * @see #getLimit()
     * @see #setLimit(long)
     * @see java.util.stream.Stream#limit(long)
     */
    private long limit;

    /**
     * The expressions to use for sorting the feature instances, or {@code null} if none.
     *
     * @see #getSortBy()
     * @see #setSortBy(SortBy)
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private SortBy<Feature> sortBy;

    /**
     * Hint used by resources to optimize returned features, or {@code null} for full resolution.
     * Different stores make use of vector tiles of different scales.
     * A {@code null} value means to query data at their full resolution.
     *
     * @see #getLinearResolution()
     * @see #setLinearResolution(Quantity)
     */
    @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
    private Quantity<Length> linearResolution;

    /**
     * Creates a new query applying no filter.
     */
    public FeatureQuery() {
        limit = UNLIMITED;
    }

    /**
     * Creates a new query initialized to the same values than the given query.
     * This is an alternative to the {@link #clone()} method when the caller
     * wants to change the implementation class.
     *
     * @param  other  the other query from which to copy the configuration.
     *
     * @see #clone()
     *
     * @since 1.5
     */
    public FeatureQuery(final FeatureQuery other) {
        projection       = other.projection;
        selection        = other.selection;
        skip             = other.skip;
        limit            = other.limit;
        sortBy           = other.sortBy;
        linearResolution = other.linearResolution;
    }

    /**
     * Returns {@code true} if this query do not specify any filtering.
     *
     * @return if this query performs no filtering.
     *
     * @since 1.5
     */
    @Override
    public boolean isEmpty() {
        return (projection == null) && (selection == null) && (skip == 0) && (limit < 0) && (sortBy == null)
                && (linearResolution == null);
    }

    /**
     * Sets the properties to retrieve by their names. This convenience method wraps the
     * given names in {@link ValueReference} expressions without alias and delegates to
     * {@link #setProjection(NamedExpression...)}.
     *
     * @param  properties  properties to retrieve, or {@code null} to retrieve all properties.
     * @throws IllegalArgumentException if the given array is empty of if a property is duplicated.
     */
    @Override
    public void setProjection(final String... properties) {
        NamedExpression[] wrappers = null;
        if (properties != null) {
            ArgumentChecks.ensureNonEmpty("properties", properties);
            final var ff = DefaultFilterFactory.forFeatures();
            wrappers = new NamedExpression[properties.length];
            for (int i=0; i<wrappers.length; i++) {
                final String p = properties[i];
                ArgumentChecks.ensureNonNullElement("properties", i, p);
                wrappers[i] = new NamedExpression(ff.property(p));
            }
        }
        setProjection(wrappers);
    }

    /**
     * Sets the properties to retrieve, or {@code null} if all properties shall be included in the query.
     * This convenience method wraps the given expression in {@link NamedExpression}s without alias and
     * delegates to {@link #setProjection(NamedExpression...)}.
     *
     * @param  properties  properties to retrieve, or {@code null} to retrieve all properties.
     * @throws IllegalArgumentException if the given array is empty of if a property is duplicated.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public final void setProjection(final Expression<? super Feature, ?>... properties) {
        NamedExpression[] wrappers = null;
        if (properties != null) {
            ArgumentChecks.ensureNonEmpty("properties", properties);
            wrappers = new NamedExpression[properties.length];
            for (int i=0; i<wrappers.length; i++) {
                final Expression<? super Feature, ?> e = properties[i];
                ArgumentChecks.ensureNonNullElement("properties", i, e);
                wrappers[i] = new NamedExpression(e);
            }
        }
        setProjection(wrappers);
    }

    /**
     * Sets the properties to retrieve, or {@code null} if all properties shall be included in the query.
     * A query column may use a simple or complex expression and an alias to create a new type of property
     * in the returned features.
     *
     * <p>This is equivalent to the column names in the {@code SELECT} clause of a SQL statement.
     * Subset of columns is called <dfn>projection</dfn> in relational database terminology.</p>
     *
     * @param  properties  properties to retrieve, or {@code null} to retrieve all properties.
     * @throws IllegalArgumentException if a property or an alias is duplicated.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setProjection(NamedExpression... properties) {
        if (properties != null) {
            ArgumentChecks.ensureNonEmpty("properties", properties);
            properties = properties.clone();
            final var uniques = JDK19.<Object,Integer>newLinkedHashMap(properties.length);
            for (int i=0; i<properties.length; i++) {
                final NamedExpression c = properties[i];
                ArgumentChecks.ensureNonNullElement("properties", i, c);
                GenericName alias = c.alias();
                Object key = (alias != null) ? alias : c.expression();
                final Integer p = uniques.putIfAbsent(key, i);
                if (p != null) {
                    if (key instanceof Expression) {
                        key = label((Expression) key);
                    }
                    throw new IllegalArgumentException(Resources.format(Resources.Keys.DuplicatedQueryProperty_3, key, p, i));
                }
            }
        }
        this.projection = properties;
    }

    /**
     * Returns the properties to retrieve, or {@code null} if all properties shall be included in the query.
     * This is the expressions specified in the last call to {@link #setProjection(NamedExpression[])}.
     * The default value is null.
     *
     * @return properties to retrieve, or {@code null} to retrieve all feature properties.
     */
    public NamedExpression[] getProjection() {
        return (projection != null) ? projection.clone() : null;
    }

    /**
     * Sets the approximate area of feature instances to include in the subset.
     * This convenience method creates a filter that checks if the bounding box
     * of the feature's {@code "sis:geometry"} property interacts with the given envelope.
     *
     * @param  domain  the approximate area of interest, or {@code null} if none.
     */
    @Override
    public void setSelection(final Envelope domain) {
        Filter<Feature> filter = null;
        if (domain != null) {
            final FilterFactory<Feature,Object,?> ff = DefaultFilterFactory.forFeatures();
            filter = ff.bbox(ff.property(AttributeConvention.GEOMETRY), domain);
        }
        setSelection(filter);
    }

    /**
     * Sets a filter for trimming feature instances.
     * Features that do not pass the filter are discarded.
     * Discarded features are not counted for the {@linkplain #setLimit(long) query limit}.
     *
     * @param  selection  the filter, or {@code null} if none.
     */
    public void setSelection(final Filter<? super Feature> selection) {
        this.selection = selection;
    }

    /**
     * Returns the filter for trimming feature instances.
     * This is the value specified in the last call to {@link #setSelection(Filter)}.
     * The default value is {@code null}, which means that no filtering is applied.
     *
     * @return the filter, or {@code null} if none.
     */
    public Filter<? super Feature> getSelection() {
        return selection;
    }

    /**
     * Sets the number of feature instances to skip from the beginning.
     * Offset and limit are often combined to obtain paging.
     * The offset cannot be negative.
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
     * The default value is zero, which means that no features are skipped.
     *
     * @return the number of feature instances to skip from the beginning.
     */
    public long getOffset() {
        return skip;
    }

    /**
     * Removes any limit defined by {@link #setLimit(long)}.
     */
    public void setUnlimited() {
        limit = UNLIMITED;
    }

    /**
     * Set the maximum number of feature instances contained in the {@code FeatureSet}.
     * Offset and limit are often combined to obtain paging.
     *
     * <p>Note that setting this property can be costly on parallelized streams.
     * See {@link java.util.stream.Stream#limit(long)} for more information.</p>
     *
     * @param  limit  maximum number of feature instances contained in the {@code FeatureSet}.
     */
    public void setLimit(final long limit) {
        ArgumentChecks.ensurePositive("limit", limit);
        this.limit = limit;
    }

    /**
     * Returns the maximum number of feature instances contained in the {@code FeatureSet}.
     * This is the value specified in the last call to {@link #setLimit(long)}.
     *
     * @return maximum number of feature instances contained in the {@code FeatureSet}, or empty if none.
     */
    public OptionalLong getLimit() {
        return (limit >= 0) ? OptionalLong.of(limit) : OptionalLong.empty();
    }

    /**
     * Sets the expressions to use for sorting the feature instances.
     * {@code SortBy} objects are used to order the {@link Feature} instances returned by the {@link FeatureSet}.
     * {@code SortBy} clauses are applied in declaration order, like SQL.
     *
     * @param  properties  expressions to use for sorting the feature instances,
     *                     or {@code null} or an empty array if none.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public final void setSortBy(final SortProperty<Feature>... properties) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        SortBy<Feature> sortBy = null;
        if (properties != null) {
            sortBy = SortByComparator.create(properties);
        }
        setSortBy(sortBy);
    }

    /**
     * Sets the expressions to use for sorting the feature instances.
     * {@code SortBy} objects are used to order the {@link Feature} instances returned by the {@link FeatureSet}.
     *
     * @param  sortBy  expressions to use for sorting the feature instances, or {@code null} if none.
     */
    public void setSortBy(final SortBy<Feature> sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * Returns the expressions to use for sorting the feature instances.
     * This is the value specified in the last call to {@link #setSortBy(SortBy)}.
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
     * Whether a property evaluated by a query is computed on the fly or stored.
     * By default, an expression is evaluated only once for each feature instance,
     * then the result is stored as a feature {@link Attribute} value.
     * But the same expression can also be wrapped in a feature {@link Operation}
     * and evaluated every times that the value is requested.
     *
     * <h2>Analogy with relational databases</h2>
     * The terminology used in this enumeration is close to the one used in relational database.
     * A <dfn>projection</dfn> is the set of feature properties to keep in the query results.
     * The projection may contain <dfn>generated columns</dfn>, which are specified in SQL by
     * {@code SQL GENERATED ALWAYS} statement, optionally with {@code STORED} or {@code VIRTUAL}
     * modifier.
     *
     * @version 1.4
     * @since   1.4
     */
    public enum ProjectionType {
        /**
         * The expression is evaluated exactly once when a feature instance is created,
         * and the result is stored as a feature attribute.
         * The feature property type will be {@link Attribute} and its value will be modifiable.
         * This is the default projection type.
         *
         * <h4>Feature instances in expression evaluation</h4>
         * The features given in calls to {@link Expression#apply(Object)} are instances from the
         * <em>source</em> {@link FeatureSet}, before filtering.
         */
        STORED,

        /*
         * The expression is evaluated every times that the property value is requested.
         * This projection type is similar to {@link #COMPUTING}, except that the features
         * given in calls to {@link Expression#apply(Object)} are the same instances as
         * the ones used by {@link #STORED}.
         *
         * <div class="note"><b>Note on naming:</b>
         * the {@code STORED} and {@code VIRTUAL} enumeration values are named according usage in SQL
         * {@code GENERATE ALWAYS} statement. Those two keywords work on columns in the source tables.
         * </div>
         *
         * <h4>Feature instances in expression evaluation</h4>
         * The combination of deferred calculation (like {@link #COMPUTING}) and usage of feature instances
         * from the <em>source</em> {@link FeatureSet} (like {@link #STORED}) may cause this projection type
         * to retain the source feature instances for a longer time than other types.
         *
         * @todo Waiting to see if there is a need for this type before to implement it.
         */
      // VIRTUAL,

        /**
         * The expression is evaluated every times that the property value is requested.
         * The feature property type will be {@link Operation}.
         * This projection type may be preferable to {@link #STORED} in the following circumstances:
         *
         * <ul>
         *   <li>The expression may produce different results every times that it is evaluated.</li>
         *   <li>The feature property should be a {@linkplain FeatureOperations#link link} to another attribute.</li>
         *   <li>Potentially expensive computation should be deferred until first needed.</li>
         *   <li>Computation result should not be stored in order to reduce memory usage.</li>
         * </ul>
         *
         * <h4>Feature instances in expression evaluation</h4>
         * The features given in calls to {@link Expression#apply(Object)} are instances from the <em>target</em>
         * {@link FeatureSet}, after filtering. The instances from the source {@code FeatureSet} are no longer
         * available when the expression is executed. Consequently, all fields that are necessary for computing
         * a {@code COMPUTING} field shall have been first copied in {@link #STORED} fields.
         *
         * <div class="note"><b>Note on naming:</b>
         * verb tense <i>-ing</i> instead of <i>-ed</i> is for emphasizing that the data used for computation
         * are current (filtered) data instead of past (original) data.</div>
         *
         * @see FeatureOperations#function(Map, Function, AttributeType)
         */
        COMPUTING
    }

    /**
     * An expression to be retrieved by a {@code Query}, together with the name to assign to it.
     * {@code NamedExpression} specifies also if the expression should be evaluated exactly once
     * and its value stored, or evaluated every times that the value is requested.
     *
     * <h2>Analogy with relational databases</h2>
     * A {@code NamedExpression} instance can be understood as the definition of a column in a SQL database table.
     * In relational database terminology, subset of columns is called <dfn>projection</dfn>.
     * A projection is specified by a SQL {@code SELECT} statement, which maps to {@code NamedExpression} as below:
     *
     * <p>{@code SELECT} {@link #expression} {@code AS} {@link #alias}</p>
     *
     * Columns can be given to the {@link FeatureQuery#setProjection(NamedExpression[])} method.
     *
     * @version 1.6
     * @since   1.1
     */
    public static final class NamedExpression implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 4547204390645035145L;

        /**
         * The literal, value reference or more complex expression to be retrieved by a {@code Query}.
         * Never {@code null}.
         */
        @SuppressWarnings("serial")
        private final Expression<? super Feature, ?> expression;

        /**
         * The name to assign to the expression result, or {@code null} if unspecified.
         */
        @SuppressWarnings("serial")                 // Most SIS implementations are serializable.
        private final GenericName alias;

        /**
         * Whether the expression result should be stored or evaluated every times that it is requested.
         * A stored value will exist as a feature {@link Attribute}, while a virtual value will exist as
         * a feature {@link Operation}. The latter are commonly called "computed fields" and are equivalent
         * to SQL {@code GENERATED ALWAYS} keyword for columns.
         */
        private final ProjectionType type;

        /**
         * Creates a new stored column with the given expression and no name.
         *
         * @param expression  the literal, value reference or expression to be retrieved by a {@code Query}.
         */
        public NamedExpression(final Expression<? super Feature, ?> expression) {
            this(expression, (GenericName) null);
        }

        /**
         * Creates a new stored column with the given expression and the given name.
         *
         * @param expression  the literal, value reference or expression to be retrieved by a {@code Query}.
         * @param alias       the name to assign to the expression result, or {@code null} if unspecified.
         */
        public NamedExpression(final Expression<? super Feature,?> expression, final GenericName alias) {
            this(expression, alias, ProjectionType.STORED);
        }

        /**
         * Creates a new stored column with the given expression and the given name.
         * This constructor creates a {@link org.opengis.util.LocalName} from the given string.
         *
         * @param expression  the literal, value reference or expression to be retrieved by a {@code Query}.
         * @param alias       the name to assign to the expression result, or {@code null} if unspecified.
         */
        public NamedExpression(final Expression<? super Feature,?> expression, final String alias) {
            this.expression = Objects.requireNonNull(expression);
            this.alias = (alias != null) ? Names.createLocalName(null, null, alias) : null;
            this.type = ProjectionType.STORED;
        }

        /**
         * Creates a new column with the given expression, the given name and the given projection type.
         *
         * @param expression  the literal, value reference or expression to be retrieved by a {@code Query}.
         * @param alias       the name to assign to the expression result, or {@code null} if unspecified.
         * @param type        whether to create a feature {@link Attribute} or a feature {@link Operation}.
         *
         * @since 1.4
         */
        public NamedExpression(final Expression<? super Feature,?> expression, final GenericName alias, ProjectionType type) {
            this.expression = Objects.requireNonNull(expression);
            this.type       = Objects.requireNonNull(type);
            this.alias      = alias;
        }

        /**
         * Adds this named expression as a property into the given builder.
         *
         * @param  builder  the builder where to add the property.
         * @return whether the property has been successfully added.
         * @throws InvalidFilterValueException if {@linkplain #expression} is invalid.
         * @throws PropertyNotFoundException if the property was not found in {@code builder.source()}.
         * @throws UnconvertibleObjectException if the property default value cannot be converted to the expected type.
         */
        final boolean addTo(final FeatureProjectionBuilder builder) {
            final FeatureExpression<? super Feature, ?> fex = FeatureExpression.castOrCopy(expression);
            if (fex != null) {
                final FeatureProjectionBuilder.Item item = fex.expectedType(builder);
                if (item != null) {
                    item.setPreferredName(alias);   // Need to be invoked even if the alias is null.
                    item.setValueGetter(expression, type == ProjectionType.STORED);
                    return true;
                }
            }
            return false;
        }

        /**
         * The literal, value reference or more complex expression to be retrieved by a {@code Query}.
         * Never {@code null}.
         *
         * @return the expression (often a literal) for the value to retrieve.
         *
         * @since 1.5
         */
        public Expression<? super Feature, ?> expression() {
            return expression;
        }

        /**
         * The name to assign to the expression result, or {@code null} if unspecified.
         *
         * @return optional name for the expression result.
         *
         * @since 1.5
         */
        public GenericName alias() {
            return alias;
        }

        /**
         * Whether the expression result should be stored or evaluated every times that it is requested.
         * A stored value will exist as a feature {@link Attribute}, while a virtual value will exist as
         * a feature {@link Operation}. The latter are commonly called "computed fields" and are equivalent
         * to SQL {@code GENERATED ALWAYS} keyword for columns.
         *
         * @return the projection type (stored or computing).
         *
         * @since 1.5
         */
        public ProjectionType type() {
            return type;
        }

        /**
         * Returns a hash code value for this column.
         *
         * @return a hash code value.
         */
        @Override
        public int hashCode() {
            return 37 * expression.hashCode() + Objects.hashCode(alias) + type.hashCode();
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
                final var other = (NamedExpression) obj;
                return expression.equals(other.expression) && Objects.equals(alias, other.alias) && type == other.type;
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
            final var buffer = new StringBuilder("SELECT ");
            appendTo(buffer);
            return buffer.toString();
        }

        /**
         * Appends a string representation of this column in the given buffer.
         */
        final void appendTo(final StringBuilder buffer) {
            if (expression instanceof Literal<?,?>) {
                buffer.append('‘').append(((Literal<?,?>) expression).getValue()).append('’');
            } else if (expression instanceof ValueReference<?,?>) {
                buffer.append('“').append(((ValueReference<?,?>) expression).getXPath()).append('”');
            } else {
                buffer.append("=“").append(expression.getFunctionName()).append("”()");
            }
            if (type != ProjectionType.STORED) {
                buffer.append(' ').append(type);
            }
            if (alias != null) {
                buffer.append(" AS “").append(alias).append('”');
            }
        }
    }

    /**
     * Returns a label for the given expression for reporting to human (e.g. in exception messages).
     * This method uses the value reference (XPath) or literal value if applicable, truncated to an
     * arbitrary length.
     */
    private static String label(final Expression<?,?> expression) {
        final String text;
        if (expression instanceof Literal<?,?>) {
            text = String.valueOf(((Literal<?,?>) expression).getValue());
        } else if (expression instanceof ValueReference<?,?>) {
            text = ((ValueReference<?,?>) expression).getXPath();
        } else {
            return expression.getFunctionName().toString();
        }
        return CharSequences.shortSentence(text, 40).toString();
    }

    /**
     * Returns all XPaths used, directly or indirectly, by this query.
     * The XPath values are extracted from all {@link ValueReference} expressions found in the
     * {@linkplain #getSelection() selection} and in the {@linkplain #getProjection() projection}.
     * The {@linkplain NamedExpression#alias aliases} are ignored.
     *
     * <p>The elements in the returned set are in no particular order.
     * The set may be empty but never null.</p>
     *
     * @return all XPaths used, directly or indirectly, by this query.
     *
     * @since 1.5
     */
    public Set<String> getXPaths() {
        Set<String> xpaths = ListingPropertyVisitor.xpaths(selection, null);
        if (projection != null) {
            for (NamedExpression e : projection) {
                xpaths = ListingPropertyVisitor.xpaths(e.expression(), xpaths);
            }
        }
        return xpaths;
    }

    /**
     * Creates the projection (in <abbr>SQL</abbr> sense) of the given feature type.
     * If some expressions have no name, default names are computed as below:
     *
     * <ul>
     *   <li>If the expression is an instance of {@link ValueReference}, the name of the
     *       property referenced by the {@linkplain ValueReference#getXPath() XPath}.</li>
     *   <li>Otherwise the localized string "Unnamed #1" with increasing numbers.</li>
     * </ul>
     *
     * @param  sourceType  the feature type to project.
     * @param  locale      locale for error messages, or {@code null} for the default locale.
     * @throws InvalidFilterValueException if an {@linkplain NamedExpression#expression expression} is invalid.
     * @throws PropertyNotFoundException if a property referenced by an expression was not found in {@code sourceType}.
     * @throws UnconvertibleObjectException if a property default value cannot be converted to the expected type.
     * @throws UnsupportedOperationException if there is an attempt to rename a property which is used by an operation.
     */
    final Optional<FeatureProjection> project(final FeatureType sourceType, final Locale locale) {
        if (projection == null) {
            return Optional.empty();
        }
        final var builder = new FeatureProjectionBuilder(sourceType, locale);
        for (int column = 0; column < projection.length; column++) {
            final NamedExpression item = projection[column];
            if (!item.addTo(builder)) {
                final var name = item.expression().getFunctionName().toInternationalString();
                throw new InvalidFilterValueException(Resources.forLocale(locale)
                            .getString(Resources.Keys.InvalidExpression_2, column, name));
            }
        }
        builder.setName(sourceType.getName());
        return builder.project();
    }

    /**
     * Applies this query on the given feature set.
     * This method is invoked by the default implementation of {@link FeatureSet#subset(Query)}.
     * The default implementation executes the query using the default {@link java.util.stream.Stream} methods.
     * Queries executed by this method may not benefit from accelerations provided for example by databases.
     * This method should be used only as a fallback when the query cannot be executed natively
     * by {@link FeatureSet#subset(Query)}.
     *
     * <p>The returned {@code FeatureSet} does not cache the resulting {@code Feature} instances;
     * the query is processed on every call to the {@link FeatureSet#features(boolean)} method.</p>
     *
     * @param  source  the set of features to filter, sort or process.
     * @return a view over the given feature set containing only the filtered feature instances.
     * @throws DataStoreException if an error occurred during creation of the subset.
     *
     * @see FeatureSet#subset(Query)
     * @see CoverageQuery#execute(GridCoverageResource)
     *
     * @since 1.2
     */
    protected FeatureSet execute(final FeatureSet source) throws DataStoreException {
        if (isEmpty()) {
            return source;
        }
        final FeatureQuery query = clone();
        query.optimize(source);
        return new FeatureSubset(source, query);
    }

    /**
     * Optimizes this query before execution. This method is invoked by {@link #execute(FeatureSet)}
     * on a {@linkplain #clone() clone} of the user-provided query. The default implementations tries
     * to optimize the {@linkplain #getSelection() selection} filter using {@link Optimization}.
     * Subclasses can override for modifying the optimization algorithm.
     *
     * @param  source  the set of features given to the {@code execute(FeatureSet)} method.
     * @throws DataStoreException if an error occurred during the optimization of this query.
     *
     * @since 1.5
     *
     * @deprecated Moved to {@link AbstractFeatureSet#prepareQueryOptimization(FeatureQuery, Optimization)}
     * because experience suggests that this is the class that know best how to configure.
     */
    @Deprecated(since = "1.6", forRemoval = true)
    protected void optimize(final FeatureSet source) throws DataStoreException {
        if (selection != null) {
            final var optimizer = new Optimization();
            if (source instanceof AbstractFeatureSet) {
                ((AbstractFeatureSet) source).prepareQueryOptimization(this, optimizer);
            }
            selection = optimizer.apply(selection);
        }
    }

    /**
     * Returns a clone of this query.
     *
     * @return a clone of this query.
     *
     * @see #FeatureQuery(FeatureQuery)
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
        return 97 * Arrays.hashCode(projection) + 31 * Objects.hashCode(selection)
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
            final var other = (FeatureQuery) obj;
            return skip  == other.skip &&
                   limit == other.limit &&
                   Objects.equals(selection,        other.selection) &&
                   Arrays .equals(projection,       other.projection) &&
                   Objects.equals(sortBy,           other.sortBy) &&
                   Objects.equals(linearResolution, other.linearResolution);
        }
        return false;
    }

    /**
     * Returns a textual representation of this query for debugging purposes.
     * The default implementation returns a string that looks like an SQL Select query.
     *
     * @return textual representation of this query.
     */
    @Override
    public String toString() {
        final var sb = new StringBuilder(80);
        sb.append("SELECT ");
        if (projection != null) {
            for (int i=0; i<projection.length; i++) {
                if (i != 0) sb.append(", ");
                projection[i].appendTo(sb);
            }
        } else {
            sb.append('*');
        }
        if (selection != null) {
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
