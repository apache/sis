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

import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.util.logging.WarningListeners;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

// Branch-dependent imports
import org.opengis.feature.FeatureType;


/**
 * Base implementation of feature sets contained in data stores.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public abstract class AbstractFeatureSet extends AbstractResource implements FeatureSet {
    /**
     * A description of this set of features, or {@code null} if not yet computed.
     * Those metadata are created by {@link #getMetadata()} when first needed.
     * Subclasses can set a value to this field directly.
     */
    protected Metadata metadata;

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
     * Returns a description of this set of features.
     * Current implementation sets only the resource name; this may change in any future Apache SIS version.
     *
     * <div class="note"><b>Note:</b>
     * we currently do not set the geographic extent from the envelope because default {@link #getEnvelope()}
     * implementation itself invokes {@code getMetadata()}. Consequently requesting the envelope from this
     * method could create a never-ending loop.</div>
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final DefaultMetadata metadata = new DefaultMetadata();
            final FeatureType type = getType();
            if (type != null) {
                final GenericName name = type.getName();
                if (name != null) {                         // Paranoiac check (should never be null).
                    final DefaultCitation citation = new DefaultCitation(name.toInternationalString());
                    final DefaultDataIdentification identification = new DefaultDataIdentification();
                    identification.setCitation(citation);
                }
            }
            // No geographic extent - see above javadoc.
            metadata.transition(DefaultMetadata.State.FINAL);
            this.metadata = metadata;
        }
        return metadata;
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
