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
package org.apache.sis.internal.storage;

import java.util.OptionalLong;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.util.logging.WarningListeners;
import org.opengis.util.GenericName;

// Branch-dependent imports
import org.opengis.feature.FeatureType;


/**
 * Base implementation of feature sets contained in data stores. This class provides a {@link #getMetadata()}
 * which extracts information from other methods. Subclasses shall or should override the following methods:
 *
 * <ul>
 *   <li>{@link #getType()} (mandatory)</li>
 *   <li>{@link #getFeatureCount()} (recommended)</li>
 *   <li>{@link #getEnvelope()} (recommended)</li>
 *   <li>{@link #createMetadata(MetadataBuilder)} (optional)</li>
 *   <li>{@link #features(boolean parallel)} (mandatory)</li>
 * </ul>
 *
 * {@section Thread safety}
 * Default methods of this abstract class are thread-safe.
 * Synchronization, when needed, uses {@code this} lock.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public abstract class AbstractFeatureSet extends AbstractResource implements FeatureSet {
    /**
     * Creates a new resource.
     *
     * @param listeners  the set of registered warning listeners for the data store, or {@code null} if none.
     */
    protected AbstractFeatureSet(final WarningListeners<DataStore> listeners) {
        super(listeners);
    }

    /**
     * Creates a new feature set with the same warning listeners than the given resource,
     * or {@code null} if the listeners are unknown.
     *
     * @param resource  the resources from which to get the listeners, or {@code null} if none.
     */
    protected AbstractFeatureSet(final FeatureSet resource) {
        super(resource);
    }

    /**
     * Returns the feature type name as the identifier for this resource.
     *
     * @return the resource identifier inferred from metadata, or {@code null} if none or ambiguous.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     *
     * @see DataStore#getIdentifier()
     */
    @Override
    public GenericName getIdentifier() throws DataStoreException {
        final FeatureType type = getType();
        if (type != null) {
            return type.getName();
        }
        return null;
    }

    /**
     * Returns an estimation of the number of features in this set, or empty if unknown.
     * The default implementation returns an empty value.
     *
     * @return estimation of the number of features.
     */
    protected OptionalLong getFeatureCount() {
        return OptionalLong.empty();
    }

    /**
     * Invoked the first time that {@link #getMetadata()} is invoked. The default implementation populates metadata
     * based on information provided by {@link #getType()}, {@link #getIdentifier()} and {@link #getEnvelope()}.
     * Subclasses should override if they can provide more information.
     *
     * @param  metadata  the builder where to set metadata properties.
     * @throws DataStoreException if an error occurred while reading metadata from the data store.
     */
    @Override
    protected void createMetadata(final MetadataBuilder metadata) throws DataStoreException {
        super.createMetadata(metadata);
        metadata.addFeatureType(getType(), getFeatureCount().orElse(-1));
    }

    /**
     * Requests a subset of features and/or feature properties from this resource.
     * The default implementation try to execute the queries by filtering the
     * {@linkplain #features(boolean) stream of features}, which may be inefficient.
     * Subclasses are encouraged to override.
     *
     * @param  query  definition of feature and feature properties filtering applied at reading time.
     * @return resulting subset of features (never {@code null}).
     * @throws UnsupportedQueryException if this {@code FeatureSet} can not execute the given query.
     * @throws DataStoreException if another error occurred while processing the query.
     */
    @Override
    public FeatureSet subset(final Query query) throws DataStoreException {
        if (query instanceof SimpleQuery) {
            return ((SimpleQuery) query).execute(this);
        } else {
            return FeatureSet.super.subset(query);
        }
    }
}
