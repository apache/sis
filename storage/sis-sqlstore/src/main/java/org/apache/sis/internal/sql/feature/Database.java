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

import java.util.Set;
import java.util.HashSet;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;

// Branch-dependent imports
import org.opengis.feature.FeatureType;


/**
 * Represent the structure of features in the database.
 * The work done here is similar to reverse engineering.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class Database {
    /**
     * All tables known to this {@code Database}. Each table contains a {@link Table#featureType}.
     */
    private final FeatureNaming<Table> tables;

    /**
     * Functions that may be specific to the geospatial database in use.
     */
    final SpatialFunctions functions;

    /**
     * Creates a new model about the specified tables in a database.
     * This constructor requires a list of tables to include in the model,
     * but this list should not include the dependencies; this constructor
     * will follow foreigner keys automatically.
     *
     * @param  store          the data store for which we are creating a model. Used only in case of error.
     * @param  connection     connection to the database.
     * @param  catalog        name of a catalog as it is stored in the database, or {@code null} for any catalog.
     * @param  schemaPattern  pattern (with {@code '_'} and {@code '%'} wildcards) of a schema, or {@code null} for any.
     * @param  tablePatterns  pattern (with {@code '_'} and {@code '%'} wildcards) of tables to include in the model.
     * @throws SQLException if a database error occurred while reading metadata.
     * @throws DataStoreException if a logical error occurred while analyzing the database structure.
     */
    public Database(final SQLStore store, final Connection connection, final String catalog,
            final String schemaPattern, final String[] tablePatterns)
            throws SQLException, DataStoreException
    {
        tables = new FeatureNaming<>();
        final Analyzer analyzer = new Analyzer(connection.getMetaData());
        final String[] tableTypes = getTableTypes(analyzer.metadata);
        for (final String tablePattern : tablePatterns) {
            try (ResultSet reflect = analyzer.metadata.getTables(catalog, schemaPattern, tablePattern, tableTypes)) {
                while (reflect.next()) {
                    final String table = reflect.getString(Reflection.TABLE_NAME);
                    if (analyzer.isIgnoredTable(table)) {
                        continue;
                    }
                    String remarks = reflect.getString(Reflection.REMARKS);
                    remarks = (remarks != null) ? remarks.trim() : "";      // Empty string means that we verified that there is no remarks.
                    analyzer.addDependency(new TableName(remarks,           // Opportunistically use the 'name' field for storing remarks.
                            reflect.getString(Reflection.TABLE_CAT),
                            reflect.getString(Reflection.TABLE_SCHEM),
                            table));
                }
            }
        }
        TableName dependency;
        while ((dependency = analyzer.nextDependency()) != null) {
            final Table table = new Table(analyzer, dependency);
            tables.add(store, table.featureType.getName(), table);
        }
        functions = analyzer.functions;
    }

    /**
     * Returns the "TABLE" and "VIEW" keywords for table type, with unsupported keywords omitted.
     */
    private static String[] getTableTypes(final DatabaseMetaData metadata) throws SQLException {
        final Set<String> types = new HashSet<>(4);
        try (ResultSet reflect = metadata.getTableTypes()) {
            while (reflect.next()) {
                final String type = reflect.getString(Reflection.TABLE_TYPE);
                if ("TABLE".equalsIgnoreCase(type) || "VIEW".equalsIgnoreCase(type)) {
                    types.add(type);
                }
            }
        }
        return types.toArray(new String[types.size()]);
    }

    /**
     * Returns the feature type of the given name.
     *
     * @param  store  the data store for which we are created the model. Used only in case of error.
     * @param  name   name of the feature type to fetch.
     * @return the feature type of the given name.
     * @throws IllegalNameException if the given name is unknown or ambiguous.
     */
    public FeatureType getFeatureType(final SQLStore store, final String name) throws IllegalNameException {
        return tables.get(store, name).featureType;
    }
}
