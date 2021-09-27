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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.sql.SQLException;
import java.sql.ResultSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.internal.metadata.sql.SQLUtilities;
import org.apache.sis.internal.util.Strings;


/**
 * Constructor for a {@link Table} based on a "physical" table.
 * The table is identified by {@link #id}, which contains a (catalog, schema, name) tuple.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class TableAnalyzer extends FeatureAnalyzer {
    /**
     * If the analyzed table is imported by the foreigner keys of another table, the parent table.
     * Otherwise {@code null}. This is relevant only for {@link Relation.Direction#EXPORT}.
     */
    private final TableReference importedBy;

    /**
     * The table/schema name width {@code '_'} and {@code '%'} characters escaped.
     * These names are intended for use in arguments expecting a {@code LIKE} pattern.
     */
    private final String tableEsc, schemaEsc;

    /**
     * Creates an analyzer for the table of the given name.
     * The table is identified by {@code id}, which contains a (catalog, schema, name) tuple.
     * The catalog and schema parts are optional and can be null, but the table is mandatory.
     *
     * @param  id          the catalog, schema and table name of the table to analyze.
     * @param  importedBy  if the analyzed table is imported by the foreigner keys of another table,
     *                     the parent table. Otherwise {@code null}.
     * @throws SQLException if an error occurred while fetching information from the database.
     */
    TableAnalyzer(final Analyzer analyzer, final TableReference id, final TableReference importedBy) throws SQLException {
        super(analyzer, id);
        this.importedBy = importedBy;
        this.tableEsc   = escape(id.table);
        this.schemaEsc  = escape(id.schema);
        try (ResultSet reflect = analyzer.metadata.getPrimaryKeys(id.catalog, id.schema, id.table)) {
            while (reflect.next()) {
                primaryKey.add(analyzer.getUniqueString(reflect, Reflection.COLUMN_NAME));
            }
        }
        /*
         * Note: when a table contains no primary keys, we could still look for index columns
         * with unique constraint using `metadata.getIndexInfo(catalog, schema, table, true)`.
         * We don't do that for now because of uncertainties (which index to use if there is many?
         * If they are suitable as identifiers why they are not already defined as primary keys?)
         */
    }

    /**
     * Returns the given pattern with {@code '_'} and {@code '%'} characters escaped by the database-specific
     * escape characters. This method should be invoked for escaping the values of all {@link DatabaseMetaData}
     * method arguments with a name ending by {@code "Pattern"}. Note that not all arguments are pattern; please
     * checks carefully {@link DatabaseMetaData} javadoc for each method.
     *
     * <div class="note"><b>Example:</b> if a method expects an argument named {@code tableNamePattern},
     * then that argument value should be escaped. But if the argument name is only {@code tableName},
     * then the value should not be escaped.</div>
     */
    private String escape(final String pattern) {
        return SQLUtilities.escape(pattern, analyzer.escape);
    }

    /**
     * Returns a list of associations between the table read by this method and other tables.
     * The associations are defined by the foreigner keys referencing primary keys.
     *
     * <h4>Side effects</h4>
     * This method shall be invoked exactly once for each direction.
     * <p><b>Required by this method:</b> none.</p>
     * <p><b>Computed by this method:</b> {@link #foreignerKeys}.</p>
     *
     * @param  direction   direction of the foreigner key for which to return components.
     * @return components of the foreigner key in the requested direction.
     */
    @Override
    final Relation[] getForeignerKeys(final Relation.Direction direction) throws SQLException, DataStoreException {
        final List<Relation> relations = new ArrayList<>();
        final boolean isImport = (direction == Relation.Direction.IMPORT);
        try (ResultSet reflect = isImport ? analyzer.metadata.getImportedKeys(id.catalog, id.schema, id.table)
                                          : analyzer.metadata.getExportedKeys(id.catalog, id.schema, id.table))
        {
            if (reflect.next()) do {
                final Relation relation = new Relation(analyzer, direction, reflect);
                if (isImport) {
                    addForeignerKeys(relation);
                } else if (relation.equals(importedBy)) {
                    continue;
                }
                relations.add(relation);
            } while (!reflect.isClosed());
        }
        final int size = relations.size();
        return (size != 0) ? relations.toArray(new Relation[size]) : Relation.EMPTY;
    }

    /**
     * Configures the feature builder with attributes and associations inferred from the analyzed table.
     * The ordinary attributes and the associations (inferred from foreigner keys) are handled together
     * in order to have properties listed in the same order as the columns in the database table.
     *
     * <h4>Side effects</h4>
     * <p><b>Required by this method:</b> {@link #foreignerKeys}.</p>
     * <p><b>Computed by this method:</b> {@link #primaryKey}, {@link #primaryKeyClass}.</p>
     *
     * @param  feature  the builder where to add attributes and associations.
     * @return the columns for attribute values (not including associations).
     */
    @Override
    final Column[] createAttributes() throws Exception {
        /*
         * Get all columns in advance because `completeGeometryColumns(…)`
         * needs to be invoked before to invoke `database.getMapping(column)`.
         */
        final Map<String,Column> columns = new LinkedHashMap<>();
        try (ResultSet reflect = analyzer.metadata.getColumns(id.catalog, schemaEsc, tableEsc, null)) {
            while (reflect.next()) {
                final Column column = new Column(analyzer, reflect);
                if (columns.put(column.name, column) != null) {
                    throw duplicatedColumn(column);
                }
            }
        }
        final InfoStatements spatialInformation = analyzer.spatialInformation;
        if (spatialInformation != null) {
            spatialInformation.completeGeometryColumns(id, columns);
        }
        /*
         * Analyze the type of each column, which may be geometric as a consequence of above call.
         */
        final List<Column> attributes = new ArrayList<>();
        for (final Column column : columns.values()) {
            if (createAttribute(column)) {
                attributes.add(column);
            }
        }
        return attributes.toArray(new Column[attributes.size()]);
    }

    /**
     * Returns an optional description of the application schema.
     */
    @Override
    public String getRemarks() throws SQLException {
        if (id instanceof Relation) {
            try (ResultSet reflect = analyzer.metadata.getTables(id.catalog, schemaEsc, tableEsc, null)) {
                while (reflect.next()) {
                    final String remarks = Strings.trimOrNull(analyzer.getUniqueString(reflect, Reflection.REMARKS));
                    if (remarks != null) {
                        return remarks;
                    }
                }
            }
        }
        return super.getRemarks();
    }
}
