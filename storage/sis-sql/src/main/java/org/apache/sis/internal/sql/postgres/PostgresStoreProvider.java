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
package org.apache.sis.internal.sql.postgres;

import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * The provider of {@link PostgresStore}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 *
 * @todo We should not have specialized data store provider for PostgreSQL.
 *       Instead we should detect from the data source or the connection.
 */
final class PostgresStoreProvider extends SQLStoreProvider {
    /**
     * Name of the data store.
     */
    private static final String NAME = "PostgreSQL";

    /**
     * Creates a new provider.
     */
    public PostgresStoreProvider() {
    }

    /**
     * Returns a short name for the data store, which is {@value #NAME}.
     *
     * @return {@value #NAME}.
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
