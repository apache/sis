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
import java.util.Optional;
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
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.InternalDataStoreException;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.internal.metadata.sql.SQLUtilities;
import org.apache.sis.internal.storage.AbstractFeatureSet;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
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
     * The name in the database of this {@code Table} object, together with its schema and catalog.
     */
    final TableReference name;

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
     * The columns that constitute the primary key, or {@code null} if there is no primary key.
     */
    private final String[] primaryKeys;

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
     * The class of primary key values, or {@code null} if there is no primary keys.
     * If the primary keys use more than one column, then this field is the class of
     * an array; it may be an array of primitive type.
     */
    final Class<?> primaryKeyClass;

    /**
     * Feature instances already created for given primary keys. This map is used only when requesting feature
     * instances by identifiers (not for iterating over all features) and those identifiers are primary keys.
     * We create this map only for tables referenced by foreigner keys of other tables as enumerated by the
     * {@link Relation.Direction#IMPORT} and {@link Relation.Direction#EXPORT} cases; not for arbitrary
     * cross-reference cases. Values are usually {@code Feature} instances, but may also be {@code Collection<Feature>}.
     *
     * @see #instanceForPrimaryKeys()
     */
    private WeakValueHashMap<?,Object> instanceForPrimaryKeys;

    /**
     * {@code true} if this table contains at least one geometry column.
     */
    final boolean hasGeometry;

    /**
     * Creates a description of the table of the given name.
     * The table is identified by {@code id}, which contains a (catalog, schema, name) tuple.
     * The catalog and schema parts are optional and can be null, but the table is mandatory.
     *
     * @param  analyzer    helper functions, e.g. for converting SQL types to Java types.
     * @param  id          the catalog, schema and table name of the table to analyze.
     * @param  importedBy  if this table is imported by the foreigner keys of another table,
     *                     the parent table. Otherwise {@code null}.
     */
    Table(final Analyzer analyzer, final TableReference id, final TableReference importedBy)
            throws SQLException, DataStoreException
    {
        super(analyzer.listeners);
        this.source = analyzer.source;
        this.name   = id;
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
        final Map<String,Boolean> primaryKeys = new LinkedHashMap<>();
        try (ResultSet reflect = analyzer.metadata.getPrimaryKeys(id.catalog, id.schema, id.table)) {
            while (reflect.next()) {
                primaryKeys.put(analyzer.getUniqueString(reflect, Reflection.COLUMN_NAME), null);
                // The actual Boolean value will be fetched in the loop on columns later.
            }
        }
        this.primaryKeys = primaryKeys.isEmpty() ? null : primaryKeys.keySet().toArray(new String[primaryKeys.size()]);
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
                Relation relation = new Relation(analyzer, Relation.Direction.IMPORT, reflect);
                importedKeys.add(relation);
                for (final String column : relation.getForeignerKeys()) {
                    CollectionsExt.addToMultiValuesMap(foreignerKeys, column, relation);
                    relation = null;     // Only the first column will be associated.
                }
            } while (!reflect.isClosed());
        }
        final List<Relation> exportedKeys = new ArrayList<>();
        try (ResultSet reflect = analyzer.metadata.getExportedKeys(id.catalog, id.schema, id.table)) {
            if (reflect.next()) do {
                final Relation export = new Relation(analyzer, Relation.Direction.EXPORT, reflect);
                if (!export.equals(importedBy)) {
                    exportedKeys.add(export);
                }
            } while (!reflect.isClosed());
        }
        /*
         * For each column in the table that is not a foreigner key, create an AttributeType of the same name.
         * The Java type is inferred from the SQL type, and the attribute multiplicity in inferred from the SQL
         * nullability. Attribute names are added in the 'attributeNames' and 'attributeColumns' list. Those
         * names are usually the same, except when a column is used both as a primary key and as foreigner key.
         */
        Class<?> primaryKeyClass   = null;
        boolean  primaryKeyNonNull = true;
        boolean  hasGeometry       = false;
        int startWithLowerCase     = 0;
        final List<String> attributeNames = new ArrayList<>();
        final List<String> attributeColumns = new ArrayList<>();
        final FeatureTypeBuilder feature = new FeatureTypeBuilder(analyzer.nameFactory, analyzer.functions.library, analyzer.locale);
        try (ResultSet reflect = analyzer.metadata.getColumns(id.catalog, schemaEsc, tableEsc, null)) {
            while (reflect.next()) {
                final String         column       = analyzer.getUniqueString(reflect, Reflection.COLUMN_NAME);
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
                        primaryKeyNonNull &= mandatory;
                        primaryKeyClass = Classes.findCommonClass(primaryKeyClass, type);
                        if (primaryKeys.put(column, SQLUtilities.parseBoolean(reflect.getString(Reflection.IS_AUTOINCREMENT))) != null) {
                            throw new DataStoreContentException(Resources.forLocale(analyzer.locale)
                                    .getString(Resources.Keys.DuplicatedColumn_1, column));
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
                            final Table table = analyzer.table(dependency, typeName, id);
                            /*
                             * Use the column name as the association name, provided that the foreigner key
                             * use only that column. If the foreigner key use more than one column, then we
                             * do not know which column describes better the association (often there is none).
                             * In such case we use the foreigner key name as a fallback.
                             */
                            dependency.setPropertyName(column, count++);
                            final AssociationRoleBuilder association;
                            if (table != null) {
                                dependency.setSearchTable(analyzer, table, table.primaryKeys, Relation.Direction.IMPORT);
                                association = feature.addAssociation(table.featureType);
                            } else {
                                association = feature.addAssociation(typeName);     // May happen in case of cyclic dependency.
                            }
                            association.setName(dependency.propertyName);
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
                dependency.propertyName = propertyName;
                final Table table = analyzer.table(dependency, typeName, null);   // 'null' because exported, not imported.
                final AssociationRoleBuilder association;
                if (table != null) {
                    dependency.setSearchTable(analyzer, table, this.primaryKeys, Relation.Direction.EXPORT);
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
         * If the primary keys uses more than one column, we will need an array to store it.
         * If all columns are non-null numbers, use primitive arrays instead than array of wrappers.
         */
        if (primaryKeys.size() > 1) {
            if (primaryKeyNonNull) {
                primaryKeyClass = Numbers.wrapperToPrimitive(primaryKeyClass);
            }
            primaryKeyClass = Classes.changeArrayDimension(primaryKeyClass, 1);
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
                    remarks = Strings.trimOrNull(analyzer.getUniqueString(reflect, Reflection.REMARKS));
                    if (remarks != null) break;
                }
            }
        }
        if (remarks != null) {
            feature.setDefinition(remarks);
        }
        this.featureType      = feature.build();
        this.importedKeys     = toArray(importedKeys);
        this.exportedKeys     = toArray(exportedKeys);
        this.primaryKeyClass  = primaryKeyClass;
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
     * Sets the search tables on all {@link Relation} instances for which this operation has been deferred.
     * This happen when a table could not be obtained because of circular dependency. This method is invoked
     * after all tables have been created in order to fill such holes.
     *
     * @param  tables  all tables created.
     */
    final void setDeferredSearchTables(final Analyzer analyzer, final Map<GenericName,Table> tables) throws DataStoreException {
        for (final Relation.Direction direction : Relation.Direction.values()) {
            final Relation[] relations;
            switch (direction) {
                case IMPORT: relations = importedKeys; break;
                case EXPORT: relations = exportedKeys; break;
                default: continue;
            }
            if (relations != null) {
                for (final Relation relation : relations) {
                    if (!relation.isSearchTableDefined()) {
                        // A ClassCastException below would be a bug since 'relation.propertyName' shall be for an association.
                        FeatureAssociationRole association = (FeatureAssociationRole) featureType.getProperty(relation.propertyName);
                        final Table table = tables.get(association.getValueType().getName());
                        if (table == null) {
                            throw new InternalDataStoreException(association.toString());
                        }
                        final String[] referenced;
                        switch (direction) {
                            case IMPORT: referenced = table.primaryKeys; break;
                            case EXPORT: referenced =  this.primaryKeys; break;
                            default: throw new AssertionError(direction);
                        }
                        relation.setSearchTable(analyzer, table, referenced, direction);
                    }
                }
            }
        }
    }


    // ────────────────────────────────────────────────────────────────────────────────────────
    //     End of table construction. Next methods are for visualizing the table structure.
    // ────────────────────────────────────────────────────────────────────────────────────────


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


    // ────────────────────────────────────────────────────────────────────────────────────────
    //     End of table structure visualization. Next methods are for fetching features.
    // ────────────────────────────────────────────────────────────────────────────────────────


    /**
     * Returns the table identifier composed of catalog, schema and table name.
     */
    @Override
    public final Optional<GenericName> getIdentifier() {
        return Optional.of(featureType.getName().toFullyQualifiedName());
    }

    /**
     * Returns the feature type inferred from the database structure analysis.
     */
    @Override
    public final FeatureType getType() {
        return featureType;
    }

    /**
     * If this table imports the inverse of the given relation, returns the imported relation.
     * Otherwise returns {@code null}. This method is used for preventing infinite recursivity.
     *
     * @param  exported       the relation exported by another table.
     * @param  exportedOwner  {@code exported.owner.name}: table that contains the {@code exported} relation.
     * @return the inverse of the given relation, or {@code null} if none.
     */
    final Relation getInverseOf(final Relation exported, final TableReference exportedOwner) {
        if (importedKeys != null && name.equals(exported)) {
            for (final Relation relation : importedKeys) {
                if (relation.equals(exportedOwner) && relation.isInverseOf(exported)) {
                    return relation;
                }
            }
        }
        return null;
    }

    /**
     * Returns a cache for fetching feature instances by identifier. The map is created when this method is
     * first invoked. Keys are primary key values, typically as {@code String} or {@code Integer} instances
     * or arrays of those if the keys use more than one column. Values are usually {@code Feature} instances,
     * but may also be {@code Collection<Feature>}.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final synchronized WeakValueHashMap<?,Object> instanceForPrimaryKeys() {
        if (instanceForPrimaryKeys == null) {
            instanceForPrimaryKeys = new WeakValueHashMap<>(primaryKeyClass);
        }
        return instanceForPrimaryKeys;
    }

    /**
     * Returns the number of rows, or -1 if unknown. Note that some database drivers returns 0,
     * so it is better to consider 0 as "unknown" too. We do not cache this count because it may
     * change at any time.
     *
     * @param  metadata     information about the database.
     * @param  approximate  whether approximate or outdated values are acceptable.
     * @return number of rows (may be approximate), or -1 if unknown.
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
            final Features iter = features(connection, new ArrayList<>(), null);
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
     *
     * @param connection  connection to the database.
     * @param following   the relations that we are following. Used for avoiding never ending loop.
     * @param noFollow    relation to not follow, or {@code null} if none.
     */
    final Features features(final Connection connection, final List<Relation> following, final Relation noFollow)
            throws SQLException, InternalDataStoreException
    {
        return new Features(this, connection, attributeNames, attributeColumns, importedKeys, exportedKeys, following, noFollow);
    }
}
