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
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.internal.metadata.sql.SQLUtilities;
import org.apache.sis.internal.storage.AbstractFeatureSet;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.Debug;

// Branch-dependent imports
import org.opengis.feature.FeatureType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.Feature;


/**
 * Description of a table in the database, including columns, primary keys and foreigner keys.
 * This class contains a {@link FeatureType} inferred from the table structure. The {@link FeatureType}
 * contains an {@link AttributeType} for each table column, except foreigner keys which are represented
 * by {@link FeatureAssociationRole}s.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Table extends AbstractFeatureSet {
    /**
     * The structure of this table represented as a feature. Each feature attribute is a table column,
     * except synthetic attributes like "sis:identifier". The feature may also contain associations
     * inferred from foreigner keys that are not immediately apparent in the table.
     */
    private final FeatureType featureType;

    /**
     * The primary key of this table. The boolean values tells whether the column
     * uses auto-increment, with null value meaning that we don't know.
     */
    private final Map<String,Boolean> primaryKeys;

    /**
     * The primary keys of other tables that are referenced by this table foreign key columns.
     * They are 0:1 relations.
     */
    private final List<Relation> importedKeys;

    /**
     * The foreign keys of other tables that reference this table primary key columns.
     * They are 0:N relations
     */
    private final List<Relation> exportedKeys;

    /**
     * {@code true} if this table contains at least one geometry column.
     */
    final boolean hasGeometry;

    /**
     * Creates a description of the table of the given name.
     * The table is identified by {@code id}, which contains a (catalog, schema, name) tuple.
     * The catalog and schema parts are optional and can be null, but the table is mandatory.
     *
     * @param  analyzer  helper functions, e.g. for converting SQL types to Java types.
     * @param  id        the catalog, schema and table name of the table to analyze.
     */
    Table(final Analyzer analyzer, final TableReference id) throws SQLException, DataStoreContentException {
        super(analyzer.listeners);
        final String tableEsc  = analyzer.escape(id.table);
        final String schemaEsc = analyzer.escape(id.schema);
        /*
         * Get a list of primary keys. We need to know them before to create the attributes,
         * in order to detect which attributes are used as components of Feature identifiers.
         * In the 'primaryKeys' map, the boolean tells whether the column uses auto-increment,
         * with null value meaning that we don't know.
         *
         * Note: when a table contains no primary keys, we could still look for index columns
         * with unique constraint using metadata.getIndexInfo(catalog, schema, table, true).
         * We don't do that for now because of uncertainties (which index to use if there is
         * many? If they are suitable as identifiers why they are not primary keys?).
         */
        final Map<String,Boolean> primaryKeys = new HashMap<>();
        try (ResultSet reflect = analyzer.metadata.getPrimaryKeys(id.catalog, id.schema, id.table)) {
            while (reflect.next()) {
                primaryKeys.put(reflect.getString(Reflection.COLUMN_NAME), null);
                // The actual Boolean value will be fetched in the loop on columns later.
            }
        }
        /*
         * Creates a list of associations between the table read by this method and other tables.
         * The associations are defined by the foreigner keys referencing primary keys. Note that
         * the table relations can be defined in both ways:  the foreigner keys of this table may
         * be referencing the primary keys of other tables (Direction.IMPORT) or the primary keys
         * of this table may be referenced by the foreigner keys of other tables (Direction.EXPORT).
         * However in both case, we will translate that into associations from this table to the
         * other tables. We can not rely on IMPORT versus EXPORT for determining the association
         * navigability because the database designer's choice may be driven by the need to support
         * multi-occurrences.
         */
        final List<Relation> importedKeys = new ArrayList<>();
        final List<Relation> exportedKeys = new ArrayList<>();
        final Set<String> foreignerKeys = new HashSet<>();
        try (ResultSet reflect = analyzer.metadata.getImportedKeys(id.catalog, id.schema, id.table)) {
            if (reflect.next()) do {
                final Relation relation = new Relation(Relation.Direction.IMPORT, reflect);
                relation.getForeignerKeys(foreignerKeys);
                analyzer.addDependency(relation);
                importedKeys.add(relation);
            } while (!reflect.isClosed());
        }
        try (ResultSet reflect = analyzer.metadata.getExportedKeys(id.catalog, id.schema, id.table)) {
            if (reflect.next()) do {
                final Relation relation = new Relation(Relation.Direction.IMPORT, reflect);
                analyzer.addDependency(relation);
                exportedKeys.add(relation);
            } while (!reflect.isClosed());
        }
        /*
         * For each column in the table that is not a foreigner key, create an AttributeType of the same name.
         * The Java type is inferred from the SQL type, and the attribute cardinality in inferred from the SQL
         * nullability.
         */
        boolean hasGeometry = false;
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder(analyzer.nameFactory, analyzer.functions.library, analyzer.locale);
        try (ResultSet reflect = analyzer.metadata.getColumns(id.catalog, schemaEsc, tableEsc, null)) {
            while (reflect.next()) {
                final String column = reflect.getString(Reflection.COLUMN_NAME);
                final boolean isPrimaryKey = primaryKeys.containsKey(column);
                final boolean isForeignKey = foreignerKeys.contains(column);
                if (isPrimaryKey | !isForeignKey) {
                    /*
                     * Foreign keys are excluded (they will be replaced by association), except if the column is
                     * also a primary key. In the later case we need to keep that column because it is needed for
                     * building the feature identifier.
                     */
                    final String typeName = reflect.getString(Reflection.TYPE_NAME);
                    Class<?> type = analyzer.functions.toJavaType(reflect.getInt(Reflection.DATA_TYPE), typeName);
                    if (type == null) {
                        analyzer.warning(Resources.Keys.UnknownType_1, typeName);
                        type = Object.class;
                    }
                    final AttributeTypeBuilder<?> atb = ftb.addAttribute(type).setName(column);
                    if (CharSequence.class.isAssignableFrom(type)) {
                        final int size = reflect.getInt(Reflection.COLUMN_SIZE);
                        if (!reflect.wasNull()) {
                            atb.setMaximalLength(size);
                        }
                    }
                    final Boolean nullable = SQLUtilities.parseBoolean(reflect.getString(Reflection.IS_NULLABLE));
                    if (nullable == null || nullable) {
                        atb.setMinimumOccurs(0);
                    }
                    /*
                     * Some columns have special purposes: components of primary keys will be used for creating
                     * identifiers, some columns may contain a geometric object. Adding a role on those columns
                     * may create synthetic columns, for example "sis:identifier".
                     */
                    if (isPrimaryKey) {
                        atb.addRole(AttributeRole.IDENTIFIER_COMPONENT);
                        if (primaryKeys.put(column, SQLUtilities.parseBoolean(reflect.getString(Reflection.IS_AUTOINCREMENT))) != null) {
                            throw new DataStoreContentException(Resources.format(Resources.Keys.DuplicatedEntity_2, "Column", column));
                        }
                    }
                    if (Geometries.isKnownType(type)) {
                        final CoordinateReferenceSystem crs = analyzer.functions.createGeometryCRS(reflect);
                        if (crs != null) {
                            atb.setCRS(crs);
                        }
                        if (!hasGeometry) {
                            hasGeometry = true;
                            atb.addRole(AttributeRole.DEFAULT_GEOMETRY);
                        }
                    }
                }
                /*
                 * If the column is a foreigner key, insert an association to another feature instead.
                 */
                if (isForeignKey) {
                    // TODO
                }
            }
        }
        /*
         * Global information on the feature type (name, remarks).
         * The remarks are opportunistically stored in id.remarks if available by the caller.
         * An empty string means that the caller has checked for remarks and found none.
         */
        ftb.setName(id.getName(analyzer));
        String remarks = id.remarks;
        if (remarks == null) {
            try (ResultSet reflect = analyzer.metadata.getTables(id.catalog, schemaEsc, tableEsc, null)) {
                while (reflect.next()) {
                    remarks = reflect.getString(Reflection.REMARKS);
                    if (remarks != null) {
                        remarks = remarks.trim();
                        if (remarks.isEmpty()) {
                            remarks = null;
                        } else break;
                    }
                }
            }
        }
        if (remarks != null && !remarks.isEmpty()) {
            ftb.setDescription(remarks);
        }
        this.featureType  = ftb.build();
        this.primaryKeys  = CollectionsExt.compact(primaryKeys);
        this.importedKeys = CollectionsExt.compact(importedKeys);
        this.exportedKeys = CollectionsExt.compact(exportedKeys);
        this.hasGeometry  = hasGeometry;
    }

    /**
     * Appends all children to the given parent. The children are added under a node of the given name.
     * If the children collection is empty, then this method does nothing.
     *
     * @param  parent    the node where to add children.
     * @param  name      the name of a node to insert between the parent and the children.
     * @param  children  the children to add, or an empty collection if none.
     */
    @Debug
    private static void appendAll(TreeTable.Node parent, final String name, final Collection<Relation> children) {
        if (!children.isEmpty()) {
            parent = Relation.newChild(parent, name);
            for (final Relation child : children) {
                child.appendTo(parent);
            }
        }
    }

    /**
     * Creates a tree representation of this table for debugging purpose.
     *
     * @param  parent  the parent node where to add the tree representation.
     */
    @Debug
    final void appendTo(TreeTable.Node parent) {
        parent = Relation.newChild(parent, featureType.getName().toString());
        appendAll(parent, "Imported Keys", importedKeys);
        appendAll(parent, "Exported Keys", exportedKeys);
    }

    /**
     * Formats a graphical representation of this table for debugging purpose. This representation
     * can be printed to the {@linkplain System#out standard output stream} (for example) if the
     * output device uses a monospaced font and supports Unicode.
     */
    @Override
    public String toString() {
        return TableReference.toString((n) -> appendTo(n));
    }

    /**
     * Returns the feature type inferred from the database structure analysis.
     */
    @Override
    public FeatureType getType() {
        return featureType;
    }

    /**
     * Returns the number of rows, or -1 if unknown. Note that some database drivers returns 0,
     * so it is better to consider 0 as "unknown" too.
     *
     * @param  metadata     information about the database.
     * @param  approximate  whether approximative or outdated values are acceptable.
     * @return number of rows (may be approximative), or -1 if unknown.
     */
    final long countRows(final DatabaseMetaData metadata, final boolean approximate) throws SQLException {
        long count = -1;
        final String[] names = TableReference.splitName(featureType.getName());
        try (ResultSet reflect = metadata.getIndexInfo(names[2], names[1], names[0], false, approximate)) {
            while (reflect.next()) {
                final long n = reflect.getLong(Reflection.CARDINALITY);
                if (!reflect.wasNull()) {
                    if (reflect.getShort(Reflection.TYPE) == DatabaseMetaData.tableIndexStatistic) {
                        return n;       // "Index statistic" type provides the number of rows in the table.
                    }
                    if (n > count) {    // Other index types may be inaccurate.
                        count = n;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Returns a stream of all features contained in this dataset.
     *
     * @param  parallel  {@code true} for a parallel stream (if supported), or {@code false} for a sequential stream.
     * @return all features contained in this dataset.
     * @throws DataStoreException if an error occurred while creating the stream.
     */
    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");  // TODO
    }
}
