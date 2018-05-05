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
import org.apache.sis.util.logging.WarningListeners;


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
     * Creates a new resource.
     *
     * @param listeners  the set of registered warning listeners for the data store, or {@code null} if none.
     */
    protected AbstractFeatureSet(final WarningListeners<DataStore> listeners) {
        super(listeners);
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
            return SimpleQuery.executeOnCPU(this, (SimpleQuery) query);
        } else {
            return FeatureSet.super.subset(query);
        }
    }
}
