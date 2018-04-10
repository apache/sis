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
package org.apache.sis.sql.postgres;

import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.sis.internal.sql.reverse.DataBaseModel;
import org.apache.sis.sql.AbstractSQLStore;
import org.apache.sis.sql.dialect.SQLDialect;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.apache.sis.util.logging.Logging;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
class PostgresStore extends AbstractSQLStore {

    private static final Logger LOGGER = Logging.getLogger("org.apache.sis.sql");

    private final PostgresDialect dialect = new PostgresDialect();

    private final DataSource source;
    private final String schema;
    private final String table;

    private final DataBaseModel model;

    public PostgresStore(DataSource source, String schema, String table) {
        this.source = source;
        this.schema = schema;
        this.table = table;
        this.model = new DataBaseModel(this, LOGGER, schema, table);
    }

    @Override
    public SQLDialect getDialect() {
        return dialect;
    }

    @Override
    public DataSource getDataSource() {
        return source;
    }

    @Override
    public DataBaseModel getDatabaseModel() {
        return model;
    }

    @Override
    public ParameterValueGroup getOpenParameters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws DataStoreException {
    }

    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

}
