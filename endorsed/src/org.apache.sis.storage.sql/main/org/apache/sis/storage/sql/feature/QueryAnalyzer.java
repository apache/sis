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
package org.apache.sis.storage.sql.feature;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.opengis.util.GenericName;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.metadata.sql.privy.Reflection;


/**
 * Constructor for a {@link Table} based on a query, to be considered as a virtual table.
 * User should build a query where each column has a distinct name (using "AS" if needed).
 * Current implementation follows "import" foreigner keys but not "export" foreigner keys.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class QueryAnalyzer extends FeatureAnalyzer {
    /**
     * All columns, without filtering for separating attributes from associations.
     */
    private final Column[] columns;

    /**
     * Columns grouped by the tables that contain them.
     * The {@link String} keys of inner {@code Map}s are {@link Column#name} values.
     */
    private final Map<TableReference, Map<String,Column>> columnsPerTable;

    /**
     * Creates a new analyzer for a query.
     *
     * @param  name        name to give to the virtual table.
     * @param  query       the query submitted by user.
     * @param  definition  concise definition of the resource in natural language.
     */
    QueryAnalyzer(final Analyzer analyzer, final GenericName name, final String query, final String definition)
            throws Exception
    {
        super(analyzer, new TableReference(name, definition));
        /*
         * Get the list of all columns in the query. We do that now because we will need this list
         * for finding primary keys (below) and also before `getForeignerKeys(…)` can be executed.
         */
        final String quote = analyzer.metadata.getIdentifierQuoteString();
        try (PreparedStatement stmt = analyzer.metadata.getConnection().prepareStatement(query)) {
            final ResultSetMetaData meta = stmt.getMetaData();
            columns = new Column[meta.getColumnCount()];
            columnsPerTable = new HashMap<>();
            for (int i=1; i <= columns.length; i++) {
                final var column = new Column(meta, i, quote);
                columns[i-1] = column;
                /*
                 * In order to identify geometry columns, we need to know the table where the column come from.
                 * The `columnsPerTable` will contain the columns grouped by originating table.
                 */
                final String table = Strings.trimOrNull(meta.getTableName(i));
                if (table != null) {
                    final var source = new TableReference(
                            Strings.trimOrNull(meta.getCatalogName(i)),
                            Strings.trimOrNull(meta.getSchemaName(i)),
                            table, null);
                    final Map<String,Column> c = columnsPerTable.computeIfAbsent(source, (k) -> new HashMap<>());
                    if (c.put(column.name, column) != null) {
                        throw duplicatedColumn(column);
                    }
                }
            }
        }
    }

    /**
     * Returns a list of associations between the table read by this method and other tables.
     * Current implementation supports only "import" foreigner keys, i.e. associations from
     * the query to another table (not the opposite direction).
     */
    @Override
    Relation[] getForeignerKeys(final Relation.Direction direction) throws SQLException, DataStoreException {
        final boolean isImport = (direction == Relation.Direction.IMPORT);
        var primaryKeyColumns = isImport ? new ArrayList<String>() : null;
        final var relations = new ArrayList<Relation>();
        for (final Map.Entry<TableReference, Map<String,Column>> entry : columnsPerTable.entrySet()) {
            final Set<String> columnNames = entry.getValue().keySet();
            final TableReference src = entry.getKey();
            /*
             * Search for foreigner keys in the table where the query columns come from.
             * Foreigner keys can be handled as such only if all required columns are in the query.
             * Otherwise we will handle those columns as ordinary attributes.
             */
            try (ResultSet reflect = isImport ? analyzer.metadata.getImportedKeys(src.catalog, src.schema, src.table)
                                              : analyzer.metadata.getExportedKeys(src.catalog, src.schema, src.table))
            {
                if (reflect.next()) do {
                    final var relation = new Relation(analyzer, direction, reflect);
                    if (columnNames.containsAll(relation.getOwnerColumns())) {
                        if (isImport) {
                            addForeignerKeys(relation);
                        }
                        relations.add(relation);
                    }
                } while (!reflect.isClosed());
            }
            /*
             * Opportunistically search for primary keys. They are not needed by this method,
             * but will be needed later by `createAttributes()` and other methods.
             * This is a "all or nothing" operations: if some primary key columns are missing
             * from the query, then we cannot have primary key at all for this query.
             */
            if (primaryKeyColumns != null) {
                try (ResultSet reflect = analyzer.metadata.getPrimaryKeys(src.catalog, src.schema, src.table)) {
                    while (reflect.next()) {
                        primaryKeyColumns.add(analyzer.getUniqueString(reflect, Reflection.COLUMN_NAME));
                    }
                    if (columnNames.containsAll(primaryKeyColumns)) {
                        primaryKey.addAll(primaryKeyColumns);
                        primaryKeyColumns.clear();
                    } else {
                        primaryKey.clear();
                        primaryKeyColumns = null;       // Means to not search anymore.
                    }
                }
            }
        }
        final int size = relations.size();
        return (size != 0) ? relations.toArray(new Relation[size]) : Relation.EMPTY;
    }

    /**
     * Configures the feature builder with attributes inferred from the query.
     */
    @Override
    Column[] createAttributes() throws Exception {
        /*
         * Identify geometry columns. Must be done before the calls to `Analyzer.setValueGetterOf(column)`.
         * If the database does not have a "geometry columns" table, parse field type names as a fallback.
         */
        boolean fallback = true;
        final InfoStatements spatialInformation = analyzer.spatialInformation;
        if (spatialInformation != null) {
            fallback = columnsPerTable.isEmpty();
            for (final Map.Entry<TableReference, Map<String,Column>> entry : columnsPerTable.entrySet()) {
                spatialInformation.completeIntrospection(analyzer, entry.getKey(), entry.getValue());
            }
        }
        /*
         * Creates attributes only after we updated all columns with geometry informations.
         */
        final var attributes = new ArrayList<Column>();
        for (final Column column : columns) {
            if (fallback) {
                column.tryMakeSpatial(analyzer.database);
            }
            if (createAttribute(column)) {
                attributes.add(column);
            }
        }
        return attributes.toArray(Column[]::new);
    }
}
