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

import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.util.ArgumentChecks;

// Branch-dependent imports
import org.apache.sis.feature.DefaultFeatureType;


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
public class SimpleQuery extends Query {
    /**
     * Sentinel limit value for queries of unlimited length.
     * This value can be given to {@link #setLimit(long)} or retrieved from {@link #getLimit()}.
     */
    private static final long UNLIMITED = -1;

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
     * Creates a new query retrieving no column and applying no filter.
     */
    public SimpleQuery() {
        limit  = UNLIMITED;
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
     * Returns the type or values evaluated by this query when executed on features of the given type.
     *
     * @param  valueType  the type of features to be evaluated by the expressions in this query.
     * @return type resulting from expressions evaluation (never null).
     * @throws IllegalArgumentException if this method can operate only on some feature types
     *         and the given type is not one of them.
     */
    final DefaultFeatureType expectedType(final DefaultFeatureType valueType) {
        return valueType;       // More elaborated code in geoapi-4.0 branch.
    }

    /**
     * Returns a hash code value for this query.
     *
     * @return a hash value for this query.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(limit ^ skip);
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
                   limit == other.limit;
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
        sb.append('*');
        if (limit != UNLIMITED) {
            sb.append(" LIMIT ").append(limit);
        }
        if (skip != 0) {
            sb.append(" OFFSET ").append(skip);
        }
        return sb.toString();
    }
}
