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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.SQLFeatureNotSupportedException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.metadata.sql.privy.Reflection;
import org.apache.sis.util.privy.Strings;


/**
 * Constructor for a {@link Table} based on a "physical" table.
 * The table is identified by {@link #id}, which contains a (catalog, schema, name) tuple.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
final class TableAnalyzer extends FeatureAnalyzer {
    /**
     * If the analyzed table is imported/exported by foreigner keys,
     * the table that "contains" this table. Otherwise {@code null}.
     */
    private final TableReference dependencyOf;

    /**
     * The table/schema name width {@code '_'} and {@code '%'} characters escaped.
     * These names are intended for use in arguments expecting a {@code LIKE} pattern.
     */
    private final String tableEsc, schemaEsc;

    /**
     * Whether the filtering by schema name and table name is reliable.
     * This is usually true, but may be false with incomplete drivers
     * that do not declare search escape characters.
     */
    private final boolean isSearchReliable;

    /**
     * Creates an analyzer for the table of the given name.
     * The table is identified by {@code id}, which contains a (catalog, schema, name) tuple.
     * The catalog and schema parts are optional and can be null, but the table is mandatory.
     *
     * @param  id            the catalog, schema and table name of the table to analyze.
     * @param  dependencyOf  if the analyzed table is imported/exported by foreigner keys,
     *                       the table that "contains" this table. Otherwise {@code null}.
     * @throws SQLException if an error occurred while fetching information from the database.
     */
    @SuppressWarnings("StringEquality")
    TableAnalyzer(final Analyzer analyzer, final TableReference id, final TableReference dependencyOf) throws SQLException {
        super(analyzer, id);
        this.dependencyOf = dependencyOf;
        this.tableEsc     = analyzer.database.escapeWildcards(id.table);
        this.schemaEsc    = analyzer.database.escapeWildcards(id.schema);
        isSearchReliable  = analyzer.database.canEscapeWildcards()
                || (tableEsc == id.table && schemaEsc == id.schema);    // Identity checks are okay.

        try (ResultSet reflect = analyzer.metadata.getPrimaryKeys(id.catalog, id.schema, id.table)) {
            while (reflect.next()) {
                primaryKey.add(analyzer.getUniqueString(reflect, Reflection.COLUMN_NAME));
            }
        } catch (SQLFeatureNotSupportedException e) {
            analyzer.unavailableMetadata(e);
        }
        /*
         * Note: when a table contains no primary keys, we could still look for index columns
         * with unique constraint using `metadata.getIndexInfo(catalog, schema, table, true)`.
         * We don't do that for now because of uncertainties (which index to use if there is many?
         * If they are suitable as identifiers why they are not already defined as primary keys?)
         */
    }

    /**
     * Double-checks whether the metadata about a table are for the item that we requested.
     * We perform this double check because some database drivers have no predefined escape
     * characters for wildcards.
     */
    private boolean filterMetadata(final ResultSet reflect) throws SQLException {
        return isSearchReliable || Database.filterMetadata(reflect, id.schema, id.table, null);
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
        final var relations = new ArrayList<Relation>();
        final boolean isImport = (direction == Relation.Direction.IMPORT);
        try (ResultSet reflect = isImport ? analyzer.metadata.getImportedKeys(id.catalog, id.schema, id.table)
                                          : analyzer.metadata.getExportedKeys(id.catalog, id.schema, id.table))
        {
            if (reflect.next()) do {
                final Relation relation = new Relation(analyzer, direction, reflect);
                if (relation.equals(dependencyOf)) {
                    if (!isImport) {
                        /*
                         * For export keys we can just forget the relation because there is no column in the
                         * analyzed table for the relation (i.e. the foreigner keys are not in this table).
                         */
                        continue;
                    }
                    final SchemaModifier customizer = analyzer.customizer;
                    relation.excluded = (customizer == null) || !customizer.isCyclicAssociationAllowed(id);
                }
                if (isImport) {
                    addForeignerKeys(relation);
                } else if (relation.excluded) {
                    continue;
                }
                relations.add(relation);
            } while (!reflect.isClosed());
        } catch (SQLFeatureNotSupportedException e) {
            /*
             * Some database implementations cannot not provide information about foreigner keys.
             * We consider this limitation as non-fatal. The users will still see the table that,
             * only their dependencies will not be visible. Instead, the foreigner key will appear
             * as an ordinary attribute value.
             */
            analyzer.unavailableMetadata(e);
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
     * @return the columns for attribute values (not including associations).
     */
    @Override
    final Column[] createAttributes() throws Exception {
        /*
         * Get all columns in advance because `completeIntrospection(…)`
         * needs to be invoked before to invoke `database.getMapping(column)`.
         */
        final var columns = new LinkedHashMap<String,Column>();
        final String quote = analyzer.metadata.getIdentifierQuoteString();
        try (ResultSet reflect = analyzer.metadata.getColumns(id.catalog, schemaEsc, tableEsc, null)) {
            while (reflect.next()) {
                if (filterMetadata(reflect)) {
                    final var column = new Column(analyzer, reflect, quote);
                    if (columns.put(column.name, column) != null) {
                        throw duplicatedColumn(column);
                    }
                }
            }
        }
        final InfoStatements spatialInformation = analyzer.spatialInformation;
        if (spatialInformation != null) {
            spatialInformation.completeIntrospection(analyzer, id, columns);
        }
        /*
         * Analyze the type of each column, which may be geometric as a consequence of above call.
         */
        final var attributes = new ArrayList<Column>();
        for (final Column column : columns.values()) {
            if (spatialInformation == null || spatialInformation.geometryColumns == null) {
                // Fallback for databases without "geometry columns" table.
                column.tryMakeSpatial(analyzer);
            }
            if (createAttribute(column)) {
                attributes.add(column);
            }
        }
        return attributes.toArray(Column[]::new);
    }

    /**
     * Returns an optional description of the application schema.
     */
    @Override
    public String getRemarks() throws SQLException {
        if (id instanceof Relation) {
            try (ResultSet reflect = analyzer.metadata.getTables(id.catalog, schemaEsc, tableEsc, null)) {
                while (reflect.next()) {
                    if (filterMetadata(reflect)) {
                        String remarks = Strings.trimOrNull(analyzer.getUniqueString(reflect, Reflection.REMARKS));
                        if (remarks != null) {
                            return remarks;
                        }
                    }
                }
            }
        }
        return super.getRemarks();
    }
}
