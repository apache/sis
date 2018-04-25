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

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.WarningListeners;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.metadata.Metadata;

/**
 * FeatureSet implementation stored in memory.
 *
 * <p>
 * Note-1 : This implementation is read-only for now but will become writable.
 * </p>
 * <p>
 * Note-2 : this class is experimental.
 * </p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ArrayFeatureSet extends AbstractFeatureSet implements FeatureSet {

    private final Metadata metadata;
    private final FeatureType type;
    private final Collection<Feature> features;

    /**
     * Creates a new feature set in memory.
     *
     * @param listeners the set of registered warning listeners for the data store.
     * @param type stored features type.
     * @param features collection of stored features, this collection will not be copied.
     * @param metadata can be null
     */
    public ArrayFeatureSet(final WarningListeners<DataStore> listeners, FeatureType type, Collection<Feature> features, Metadata metadata) {
        super(listeners);
        ArgumentChecks.ensureNonNull("type", type);
        ArgumentChecks.ensureNonNull("features", type);
        this.type = type;
        this.features = features;

        if (metadata == null) {
            final DefaultDataIdentification identification = new DefaultDataIdentification();
            final NamedIdentifier identifier = new NamedIdentifier(type.getName());
            final DefaultCitation citation = new DefaultCitation(type.getName().toString());
            citation.setIdentifiers(Collections.singleton(identifier));
            identification.setCitation(citation);

            final DefaultMetadata md = new DefaultMetadata();
            md.setIdentificationInfo(Collections.singleton(identification));
            md.freeze();
            metadata = md;
        }

        this.metadata = metadata;
    }

    @Override
    public FeatureType getType() throws DataStoreException {
        return type;
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return metadata;
    }

    @Override
    public Stream<Feature> features(boolean bln) throws DataStoreException {
        return bln ? features.parallelStream() : features.stream();
    }

    @Override
    public FeatureSet subset(Query query) throws UnsupportedQueryException, DataStoreException {
        if (query instanceof SimpleQuery) {
            return SimpleQuery.executeOnCPU(this, (SimpleQuery) query);
        }
        return FeatureSet.super.subset(query);
    }

}
