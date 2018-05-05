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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.GenericName;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.FeatureExpression;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.Names;

/**
 * A simple query mimics SQL SELECT using OGC Filter and Expressions.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class SimpleQuery implements Query {

    private static final SortBy[] EMPTY_SORTBY = new SortBy[0];

    private List<Column> columns;
    private Filter filter = Filter.INCLUDE;
    private long offset;
    private long limit = -1;
    private SortBy[] sortBy = EMPTY_SORTBY;
    private final Map<String,Object> hints = new HashMap<>();

    public SimpleQuery() {
    }

    /**
     * Set query columns.
     * A query column may use a simple or complex expression and a alias
     * to create a new type of returned feature.<br>
     *
     * @return query columns or null to get all feature properties.
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * Returns the query columns.
     * @param columns query expressions or null to get all properties.
     */
    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    /**
     * Get query filter.
     * The filter is used to trim features, features who do not pass the filter
     * are discarded.<br>
     * Discarded features are not counted is there is a query limit defined.
     *
     * @return query filter, never null
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Set query filter.
     *
     * @param filter not null, use Filter.INCLUDE for all results
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * Returns the query offset.
     * The offset is the number of records to skip from the beginning.<br>
     * Offset and limit are often combined to obtain paging.
     *
     * @return offset
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Set query start offset.
     *
     * @param offset zero or positive
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
     * Returns the query limit.
     * The limit is the maximum number of records that will contain the FeatureSet.<br>
     * Offset and limit are often combined to obtain paging.
     *
     * @return limit or -1 for unlimited
     */
    public long getLimit() {
        return limit;
    }

    /**
     * Set query limit.
     *
     * @param limit positive or -1 for unlimited
     */
    public void setLimit(long limit) {
        this.limit = limit;
    }

    /**
     * Returns the query sort by parameters.
     * SortBy objects are used to order Features returned by the FeatureSet.<br>
     * The first SortBy is applied first then the others as in SQL.
     *
     * @return sort by array, never null, can be empty
     */
    public SortBy[] getSortBy() {
        return sortBy == EMPTY_SORTBY ? EMPTY_SORTBY : sortBy.clone();
    }

    /**
     * Set query sort by elements.
     *
     * @param sortBy
     */
    public void setSortBy(SortBy... sortBy) {
        this.sortBy = sortBy == null ? EMPTY_SORTBY : sortBy;
    }

    /**
     * Different FeatureSet may have more capabilities then what is provided
     * with the SimpleQuery class.<br>
     * Hints allow the user to pass more query parameters.<br>
     * Unsupported hints will be ignored by the FeatureSet.<br>
     * The returned map is modifiable.
     *
     * @return modifiable map of query hints
     */
    public Map<String, Object> getHints() {
        return hints;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.columns);
        hash = 97 * hash + Objects.hashCode(this.filter);
        hash = 97 * hash + (int) (this.offset ^ (this.offset >>> 32));
        hash = 97 * hash + (int) (this.limit ^ (this.limit >>> 32));
        hash = 97 * hash + Arrays.deepHashCode(this.sortBy);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimpleQuery other = (SimpleQuery) obj;
        if (this.offset != other.offset) {
            return false;
        }
        if (this.limit != other.limit) {
            return false;
        }
        if (!Objects.equals(this.columns, other.columns)) {
            return false;
        }
        if (!Objects.equals(this.filter, other.filter)) {
            return false;
        }
        if (!Arrays.deepEquals(this.sortBy, other.sortBy)) {
            return false;
        }
        if (!Objects.equals(this.hints, other.hints)) {
            return false;
        }
        return true;
    }


    /**
     * A query column.
     * Just an expression and an optional alias.
     */
    public static class Column {
        public final Expression expression;
        public final GenericName alias;

        public Column(Expression expression) {
            ArgumentChecks.ensureNonNull("expression", expression);
            this.expression = expression;
            this.alias = null;
        }

        public Column(Expression expression, GenericName alias) {
            ArgumentChecks.ensureNonNull("expression", expression);
            this.expression = expression;
            this.alias = alias;
        }

        public Column(Expression expression, String alias) {
            ArgumentChecks.ensureNonNull("expression", expression);
            this.alias = alias == null ? null : Names.createLocalName(null, null, alias);
            this.expression = expression;
        }

        /**
         * Returns the column expected property type.
         * @param type
         * @return
         */
        public PropertyType expectedType(FeatureType type) {
            PropertyType resultType;
            if (expression instanceof FeatureExpression) {
                resultType = ((FeatureExpression) expression).expectedType(type);
            } else {
                resultType = expression.evaluate(type, PropertyType.class);
            }
            if (alias != null) {
                //rename result type
                if (resultType instanceof AttributeType) {
                    resultType = new FeatureTypeBuilder().addAttribute((AttributeType<?>) resultType).setName(alias).build();
                } else if (resultType instanceof FeatureAssociationRole) {
                    resultType = new FeatureTypeBuilder().addAssociation((FeatureAssociationRole) resultType).setName(alias).build();
                } else {
                    throw new BackingStoreException("Expression "+expression+" returned an unexpected property type result "+resultType);
                }
            }
            return resultType;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + Objects.hashCode(this.expression);
            hash = 29 * hash + Objects.hashCode(this.alias);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Column other = (Column) obj;
            if (!Objects.equals(this.expression, other.expression)) {
                return false;
            }
            if (!Objects.equals(this.alias, other.alias)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Execute the query on the CPU.
     * <p>
     * All operations are processed in java on the CPU, this may use considerable
     * resources. Consider giving the query to the FeatureSet to allow it to
     * optimize the query.
     * <p>
     * <p>
     * The returned FeatureSet do not cache the resulting Features, the query is
     * processed on each call to features() method.
     * </p>
     *
     * @param source base FeatureSet to process.
     * @param query Query to apply on source
     * @return resulting query FeatureSet
     */
    public static FeatureSet executeOnCPU(FeatureSet source, SimpleQuery query) {
        return new SimpleQueryFeatureSet(source, query);
    }

    /**
     * Evaluate the expected returned type of a simple query.
     *
     * @param source
     * @param query
     * @return
     */
    public static FeatureType expectedType(FeatureType source, SimpleQuery query) {

        final List<Column> columns = query.getColumns();
        if (columns == null) return source;

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName(source.getName());
        for (Column col : columns) {
            ftb.addProperty(col.expectedType(source));
        }
        return ftb.build();
    }
}
