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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.opengis.util.GenericName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.AssociationRoleBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.internal.metadata.sql.SQLUtilities;
import org.apache.sis.internal.storage.AbstractFeatureSet;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Debug;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;


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
     * Provider of (pooled) connections to the database.
     */
    private final DataSource source;

    /**
     * The structure of this table represented as a feature. Each feature attribute is a table column,
     * except synthetic attributes like "sis:identifier". The feature may also contain associations
     * inferred from foreigner keys that are not immediately apparent in the table.
     */
    final FeatureType featureType;

    /**
     * The name in the database of this {@code Table} object, together with its schema.
     */
    final String schema, table;

    /**
     * Name of attributes in feature instances, excluding operations and associations to other tables.
     * Those names are in the order of columns declared in the {@code SELECT <columns} statement.
     * This array shall not be modified after construction.
     */
    private final String[] attributeNames;

    /**
     * Name of columns corresponding to each {@link #attributeNames}. This is often a reference to the
     * same array than {@link #attributeNames}, but may be different if some attributes have been renamed
     * for avoiding name collisions.
     */
    private final String[] attributeColumns;

    /**
     * The primary keys of other tables that are referenced by this table foreign key columns.
     * They are 0:1 relations. May be {@code null} if there is no imported keys.
     */
    private final Relation[] importedKeys;

    /**
     * The foreign keys of other tables that reference this table primary key columns.
     * They are 0:N relations. May be {@code null} if there is no exported keys.
     */
    private final Relation[] exportedKeys;

    /**
     * {@code true} if this table contains at least one geometry column.
     */
    final boolean hasGeometry;

    /**
     * Creates a description of the table of the given name.
     * The table is identified by {@code id}, which contains a (catalog, schema, name) tuple.
     * The catalog and schema parts are optional and can be null, but the table is mandatory.
     *
     * @param  analyzer      helper functions, e.g. for converting SQL types to Java types.
     * @param  id            the catalog, schema and table name of the table to analyze.
     * @param  isDependency  {@code false} if this table has been explicitly requested by the user,
     *                       or {@code true} if this is a dependency discovered while analyzing.
     */
    Table(final Analyzer analyzer, final TableReference id, final boolean isDependency)
            throws SQLException, DataStoreException
    {
        super(analyzer.listeners);
        this.source = analyzer.source;
        this.schema = id.schema;
        this.table  = id.table;
        final String tableEsc  = analyzer.escape(table);
        final String schemaEsc = analyzer.escape(schema);
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
        final Map<String,Boolean> primaryKeys = new LinkedHashMap<>();
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
        final Map<String, List<Relation>> foreignerKeys = new HashMap<>();
        try (ResultSet reflect = analyzer.metadata.getImportedKeys(id.catalog, id.schema, id.table)) {
            if (reflect.next()) do {
                Relation relation = new Relation(Relation.Direction.IMPORT, reflect);
                importedKeys.add(relation);
                for (final String column : relation.getForeignerKeys()) {
                    CollectionsExt.addToMultiValuesMap(foreignerKeys, column, relation);
                    relation = null;     // Only the first column will be associated.
                }
            } while (!reflect.isClosed());
        }
        final List<Relation> exportedKeys;
        if (isDependency) {
            exportedKeys = Collections.emptyList();
        } else {
            exportedKeys = new ArrayList<>();
            try (ResultSet reflect = analyzer.metadata.getExportedKeys(id.catalog, id.schema, id.table)) {
                if (reflect.next()) do {
                    exportedKeys.add(new Relation(Relation.Direction.EXPORT, reflect));
                } while (!reflect.isClosed());
            }
        }
        /*
         * For each column in the table that is not a foreigner key, create an AttributeType of the same name.
         * The Java type is inferred from the SQL type, and the attribute cardinality in inferred from the SQL
         * nullability. Attribute names are added in the 'attributeNames' and 'attributeColumns' list. Those
         * names are usually the same, except when a column is used both as a primary key and as foreigner key.
         */
        boolean hasGeometry = false;
        int startWithLowerCase = 0;
        final List<String> attributeNames = new ArrayList<>();
        final List<String> attributeColumns = new ArrayList<>();
        final FeatureTypeBuilder feature = new FeatureTypeBuilder(analyzer.nameFactory, analyzer.functions.library, analyzer.locale);
        try (ResultSet reflect = analyzer.metadata.getColumns(id.catalog, schemaEsc, tableEsc, null)) {
            while (reflect.next()) {
                final String         column       = reflect.getString(Reflection.COLUMN_NAME);
                final boolean        mandatory    = Boolean.FALSE.equals(SQLUtilities.parseBoolean(reflect.getString(Reflection.IS_NULLABLE)));
                final boolean        isPrimaryKey = primaryKeys.containsKey(column);
                final List<Relation> dependencies = foreignerKeys.get(column);
                /*
                 * Heuristic rule for determining if the column names starts with lower case or upper case.
                 * Words that are all upper-case are ignored on the assumption that they are acronyms.
                 */
                if (!column.isEmpty()) {
                    final int firstLetter = column.codePointAt(0);
                    if (Character.isLowerCase(firstLetter)) {
                        startWithLowerCase++;
                    } else if (Character.isUpperCase(firstLetter) && !CharSequences.isUpperCase(column)) {
                        startWithLowerCase--;
                    }
                }
                /*
                 * Add the column as an attribute. Foreign keys are excluded (they will be replaced by associations),
                 * except if the column is also a primary key. In the later case we need to keep that column because
                 * it is needed for building the feature identifier.
                 */
                AttributeTypeBuilder<?> attribute = null;
                if (isPrimaryKey || dependencies == null) {
                    attributeNames.add(column);
                    attributeColumns.add(column);
                    final String typeName = reflect.getString(Reflection.TYPE_NAME);
                    Class<?> type = analyzer.functions.toJavaType(reflect.getInt(Reflection.DATA_TYPE), typeName);
                    if (type == null) {
                        analyzer.warning(Resources.Keys.UnknownType_1, typeName);
                        type = Object.class;
                    }
                    attribute = feature.addAttribute(type).setName(column);
                    if (CharSequence.class.isAssignableFrom(type)) {
                        final int size = reflect.getInt(Reflection.COLUMN_SIZE);
                        if (!reflect.wasNull()) {
                            attribute.setMaximalLength(size);
                        }
                    }
                    if (!mandatory) {
                        attribute.setMinimumOccurs(0);
                    }
                    /*
                     * Some columns have special purposes: components of primary keys will be used for creating
                     * identifiers, some columns may contain a geometric object. Adding a role on those columns
                     * may create synthetic columns, for example "sis:identifier".
                     */
                    if (isPrimaryKey) {
                        attribute.addRole(AttributeRole.IDENTIFIER_COMPONENT);
                        if (primaryKeys.put(column, SQLUtilities.parseBoolean(reflect.getString(Reflection.IS_AUTOINCREMENT))) != null) {
                            throw new DataStoreContentException(Resources.format(Resources.Keys.DuplicatedEntity_2, "Column", column));
                        }
                    }
                    if (Geometries.isKnownType(type)) {
                        final CoordinateReferenceSystem crs = analyzer.functions.createGeometryCRS(reflect);
                        if (crs != null) {
                            attribute.setCRS(crs);
                        }
                        if (!hasGeometry) {
                            hasGeometry = true;
                            attribute.addRole(AttributeRole.DEFAULT_GEOMETRY);
                        }
                    }
                }
                /*
                 * If the column is a foreigner key, insert an association to another feature instead.
                 * If the foreigner key uses more than one column, only one of those columns will become
                 * an association and other columns will be omitted from the FeatureType (but there will
                 * still be used in SQL queries). Note that columns may be used by more than one relation.
                 */
                if (dependencies != null) {
                    int count = 0;
                    for (final Relation dependency : dependencies) {
                        if (dependency != null) {
                            final GenericName typeName = dependency.getName(analyzer);
                            final Table table = analyzer.table(dependency, typeName, true);
                            final String propertyName = (count++ == 0) ? column : column + '-' + count;
                            final AssociationRoleBuilder association;
                            if (table != null) {
                                dependency.setRelatedTable(table, propertyName);
                                association = feature.addAssociation(table.featureType);
                            } else {
                                association = feature.addAssociation(typeName);     // May happen in case of cyclic dependency.
                            }
                            association.setName(propertyName);
                            if (!mandatory) {
                                association.setMinimumOccurs(0);
                            }
                            /*
                             * If the column is also used in the primary key, then we have a name clash.
                             * Rename the primary key column with the addition of a "pk:" scope. We rename
                             * the primary key column instead than this association because the primary key
                             * column should rarely be used directly.
                             */
                            if (attribute != null) {
                                attribute.setName(analyzer.nameFactory.createGenericName(null, "pk", column));
                                attributeNames.set(attributeNames.size() - 1, attribute.getName().toString());
                                attribute = null;
                            }
                        }
                    }
                }
            }
        }
        /*
         * Add the associations created by other tables having foreigner keys to this table.
         * We infer the column name from the target type. We may have a name clash with other
         * columns, in which case an arbitrary name change is applied.
         */
        int count = 0;
        for (final Relation dependency : exportedKeys) {
            if (dependency != null) {
                final GenericName typeName = dependency.getName(analyzer);
                String propertyName = typeName.tip().toString();
                if (startWithLowerCase > 0) {
                    final CharSequence words = CharSequences.camelCaseToWords(propertyName, true);
                    final int first = Character.codePointAt(words, 0);
                    propertyName = new StringBuilder(words.length())
                            .appendCodePoint(Character.toLowerCase(first))
                            .append(words, Character.charCount(first), words.length())
                            .toString();
                }
                final String base = propertyName;
                while (feature.isNameUsed(propertyName)) {
                    propertyName = base + '-' + ++count;
                }
                final Table table = analyzer.table(dependency, typeName, true);
                final AssociationRoleBuilder association;
                if (table != null) {
                    dependency.setRelatedTable(table, propertyName);
                    association = feature.addAssociation(table.featureType);
                } else {
                    association = feature.addAssociation(typeName);     // May happen in case of cyclic dependency.
                }
                association.setName(propertyName)
                           .setMinimumOccurs(0)
                           .setMaximumOccurs(Integer.MAX_VALUE);
            }
        }
        /*
         * Global information on the feature type (name, remarks).
         * The remarks are opportunistically stored in id.freeText if known by the caller.
         */
        feature.setName(id.getName(analyzer));
        String remarks = id.freeText;
        if (id instanceof Relation) {
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
        if (remarks != null) {
            feature.setDefinition(remarks);
        }
        this.featureType      = feature.build();
        this.importedKeys     = toArray(importedKeys);
        this.exportedKeys     = toArray(exportedKeys);
        this.hasGeometry      = hasGeometry;
        this.attributeNames   = attributeNames.toArray(new String[attributeNames.size()]);
        this.attributeColumns = attributeColumns.equals(attributeNames) ? this.attributeNames
                              : attributeColumns.toArray(new String[attributeColumns.size()]);
    }

    /**
     * Returns the given relations as an array, or {@code null} if none.
     */
    private static Relation[] toArray(final Collection<Relation> relations) {
        final int size = relations.size();
        return (size != 0) ? relations.toArray(new Relation[size]) : null;
    }

    /**
     * Appends all children to the given parent. The children are added under the given node.
     * If the children array is null, then this method does nothing.
     *
     * @param  parent    the node where to add children.
     * @param  children  the children to add, or {@code null} if none.
     * @param  arrow     the symbol to use for relating the columns of two tables in a foreigner key.
     */
    @Debug
    private static void appendAll(final TreeTable.Node parent, final Relation[] children, final String arrow) {
        if (children != null) {
            for (final Relation child : children) {
                child.appendTo(parent, arrow);
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
        for (final String attribute : attributeNames) {
            TableReference.newChild(parent, attribute);
        }
        appendAll(parent, importedKeys, " → ");
        appendAll(parent, exportedKeys, " ← ");
    }

    /**
     * Formats a graphical representation of this table for debugging purpose. This representation
     * can be printed to the {@linkplain System#out standard output stream} (for example) if the
     * output device uses a monospaced font and supports Unicode.
     */
    @Override
    public String toString() {
        return TableReference.toString(this, (n) -> appendTo(n));
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
     * so it is better to consider 0 as "unknown" too. We do not cache this count because it may
     * change at any time.
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
    public Stream<Feature> features(final boolean parallel) throws DataStoreException {
        DataStoreException ex;
        Connection connection = null;
        try {
            connection = source.getConnection();
            final Features iter = features(connection, null);
            return StreamSupport.stream(iter, parallel).onClose(iter);
        } catch (SQLException cause) {
            ex = new DataStoreException(Exceptions.unwrap(cause));
        }
        if (connection != null) try {
            connection.close();
        } catch (SQLException e) {
            ex.addSuppressed(e);
        }
        throw ex;
    }

    /**
     * Returns an iterator over the features.
     */
    final Features features(final Connection connection, final Relation componentOf) throws SQLException {
        return new Features(this, connection, attributeNames, attributeColumns, importedKeys, componentOf);
    }
}
