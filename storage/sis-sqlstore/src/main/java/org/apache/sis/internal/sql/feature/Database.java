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
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.WarningListeners;


/**
 * Represent the structure of features in the database.
 * The work done here is similar to reverse engineering.
 * Instances of this class are thread-safe after construction;
 * if the database schema changes, then a new {@code Database} instance shall be created.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class Database {
    /**
     * The SQL wildcard for any characters. A string containing only this wildcard
     * means "any value" and can sometime be replaced by {@code null}.
     */
    public static final String WILDCARD = "%";

    /**
     * All tables known to this {@code Database}.
     */
    public final FeatureNaming<FeatureSet> tables;

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
     * <p>The table names shall be qualified names of 1, 2 or 3 components.
     * The components are {@code <catalog>.<schema pattern>.<table pattern>} where:</p>
     *
     * <ul>
     *   <li>{@code <catalog>}, if present, shall be the name of a catalog as it is stored in the database.</li>
     *   <li>{@code <schema pattern>}, if present, shall be the pattern of a schema.
     *       The pattern can use {@code '_'} and {@code '%'} wildcards characters.</li>
     *   <li>{@code <table pattern>} (mandatory) shall be the pattern of a table.
     *       The pattern can use {@code '_'} and {@code '%'} wildcards characters.</li>
     * </ul>
     *
     * @param  store        the data store for which we are creating a model. Used only in case of error.
     * @param  connection   connection to the database.
     * @param  tableNames   qualified name of the tables.
     * @param  listeners    where to send the warnings.
     * @throws SQLException if a database error occurred while reading metadata.
     * @throws DataStoreException if a logical error occurred while analyzing the database structure.
     */
    public Database(final SQLStore store, final Connection connection, final GenericName[] tableNames,
            final WarningListeners<DataStore> listeners) throws SQLException, DataStoreException
    {
        tables = new FeatureNaming<>();
        final Analyzer analyzer = new Analyzer(connection.getMetaData(), listeners);
        final String[] tableTypes = getTableTypes(analyzer.metadata);
        for (final GenericName tableName : tableNames) {
            String[] names = tableName.getParsedNames().stream().map(LocalName::toString).toArray(String[]::new);
            ArraysExt.reverse(names);               // Reorganize in (catalog, schemaPattern, tablePattern) order.
            names = ArraysExt.resize(names, 3);     // Pad with null values if necessary.
            try (ResultSet reflect = analyzer.metadata.getTables(names[2], names[1], names[0], tableTypes)) {
                while (reflect.next()) {
                    final String table = reflect.getString(Reflection.TABLE_NAME);
                    if (analyzer.isIgnoredTable(table)) {
                        continue;
                    }
                    String remarks = reflect.getString(Reflection.REMARKS);
                    remarks = (remarks != null) ? remarks.trim() : "";      // Empty string means that we verified that there is no remarks.
                    analyzer.addDependency(new TableReference(
                            reflect.getString(Reflection.TABLE_CAT),
                            reflect.getString(Reflection.TABLE_SCHEM),
                            table, remarks));
                }
            }
        }
        TableReference dependency;
        while ((dependency = analyzer.nextDependency()) != null) {
            final Table table = new Table(analyzer, dependency);
            tables.add(store, table.getType().getName(), table);
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
}
