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
package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;
import org.apache.sis.storage.sql.SQLQuery;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.storage.AbstractFeatureSet;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;


/**
 * A FeatureSet above a custom SQL query.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class QueriedFeatureSet extends AbstractFeatureSet {

    private final Database model;
    private final SQLStore store;
    private final SQLQuery query;
    private FeatureType type;

    public QueriedFeatureSet(final SQLStore store, final Database model, final SQLQuery query) {
        super((AbstractFeatureSet) null);
        this.store = store;
        this.model = model;
        this.query = query;
    }

    @Override
    public FeatureType getType() throws DataStoreException {
        if (type == null) {
            final String sql = query.getStatement();
            try (Connection cnx = store.getDataSource().getConnection();
                Statement stmt = cnx.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
                type = analyzeResult(rs, query.getName());
            } catch (SQLException ex) {
                throw new DataStoreException(ex);
            }
        }
        return type;
    }

    /**
     * Analyze the metadata of the ResultSet to rebuild a feature type.
     */
    final FeatureType analyzeResult(final ResultSet result, final String name) throws SQLException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName(name);
        final ResultSetMetaData metadata = result.getMetaData();
        final int nbcol = metadata.getColumnCount();
        for (int i=1; i <= nbcol; i++) {
            /*
             * Search if we already have this property.
             */
            PropertyType desc = null; // TODO
//                model.getProperty(metadata.getCatalogName(i),
//                                  metadata.getSchemaName(i),
//                                  metadata.getTableName(i),
//                                  metadata.getColumnName(i));
            if (desc != null) {
                ftb.addProperty(desc);
            } else {
                /*
                 * Could not find the type. This column may be a calculation result.
                 */
                final Class<?> type = model.functions.toJavaType(metadata.getColumnType(i), metadata.getColumnTypeName(i));
                final AttributeTypeBuilder<?> atb = ftb.addAttribute(type).setName(metadata.getColumnLabel(i));
                if (metadata.isNullable(i) == ResultSetMetaData.columnNullable) {
                    atb.setMinimumOccurs(0);
                }
            }
        }
        return ftb.build();
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
