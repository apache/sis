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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;

/**
 * A FeatureSet above a custom SQL query.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class SQLQueryFeatureSet implements FeatureSet {

    private final AbstractSQLStore store;
    private final SQLQuery query;
    private FeatureType type;

    public SQLQueryFeatureSet(AbstractSQLStore store, SQLQuery query) {
        this.store = store;
        this.query = query;
    }

    @Override
    public FeatureType getType() throws DataStoreException {
        if (type == null) {
            final String sql = query.getStatement();
            try (Connection cnx = store.getDataSource().getConnection();
                 Statement stmt = cnx.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                type = store.getDatabaseModel().analyzeResult(rs, query.getName());
            } catch (SQLException ex) {
                throw new DataStoreException(ex);
            }
        }
        return type;
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Envelope getEnvelope() throws DataStoreException {
        return null;
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

}
