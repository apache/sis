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
package org.apache.sis.sql;

import javax.sql.DataSource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.StorageConnector;


/**
 * Parent store class for {@code DataStore} implementations using JDBC backend.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public abstract class SQLStore extends DataStore {
    /**
     * The data source to use for obtaining connections to the database.
     */
    private final DataSource source;

    /**
     * Creates a new instance for the given storage.
     * The {@code provider} argument is an optional information.
     * The {@code connector} argument is mandatory.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (JDBC data source, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    protected SQLStore(final SQLStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        source = connector.getStorageAs(DataSource.class);
    }

    /**
     * Returns the data source used for obtaining connections to the database.
     *
     * @return the data source for obtaining connections to the database.
     */
    public DataSource getDataSource() {
        return source;
    }

    /**
     * Executes a query directly on the database.
     *
     * @param  query the query to execute (can not be null).
     * @return the features obtained by the given given query.
     */
    public abstract FeatureSet query(SQLQuery query);
}
