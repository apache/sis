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
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.logging.Level;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.apache.sis.feature.builder.AssociationRoleBuilder;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.internal.metadata.sql.SQLUtilities;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.InternalDataStoreException;
import org.apache.sis.util.resources.ResourceInternationalString;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;

// Branch-dependent imports
import org.apache.sis.feature.DefaultFeatureType;


/**
 * Helper methods for creating {@code FeatureType}s from database structure.
 * An instance of this class is created temporarily when starting the analysis
 * of a database structure, and discarded after the analysis is finished.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
final class Analyzer {
    /**
     * Information about the spatial database.
     */
    private final Database<?> database;

    /**
     * A cache of statements for fetching spatial information such as geometry columns or SRID.
     * May be {@code null} if the database is not a spatial database, e.g. because the geometry
     * table has not been found.
     */
    private final InfoStatements spatialInformation;

    /**
     * Information about the database as a whole.
     * Used for fetching tables, columns, primary keys <i>etc.</i>
     */
    private final DatabaseMetaData metadata;

    /**
     * The factory for creating {@code FeatureType} names.
     */
    final NameFactory nameFactory;

    /**
     * A pool of strings read from database metadata. Those strings are mostly catalog, schema and column names.
     * The same names are repeated often (in primary keys, foreigner keys, <i>etc.</i>), and using a pool allows
     * us to replace equal character strings by the same {@link String} instances.
     *
     * @see #getUniqueString(ResultSet, String)
     */
    private final Map<String,String> strings;

    /**
     * The string to insert before wildcard characters ({@code '_'} or {@code '%'}) to escape.
     * This is used by {@link #escape(String)} before to pass argument values (e.g. table name)
     * to {@link DatabaseMetaData} methods expecting a pattern.
     */
    private final String escape;

    /**
     * All tables created by analysis of the database structure. A {@code null} value means that the table
     * is in process of being created. This may happen if there is cyclic dependencies between tables.
     */
    private final Map<GenericName,Table> tables;

    /**
     * Warnings found while analyzing a database structure. Duplicated warnings are omitted.
     */
    private final Set<ResourceInternationalString> warnings;

    /**
     * The last catalog and schema used for creating {@link #namespace}.
     * Used for determining if {@link #namespace} is still valid.
     */
    private transient String catalog, schema;

    /**
     * The namespace created with {@link #catalog} and {@link #schema}.
     *
     * @see #namespace(String, String)
     */
    private transient NameSpace namespace;

    /**
     * User-specified modification to the features, or {@code null} if none.
     */
    private final SchemaModifier customizer;

    /**
     * Creates a new analyzer for the database described by given metadata.
     *
     * @param  database    information about the spatial database.
     * @param  connection  an existing connection to the database, used only for the lifetime of this {@code Analyzer}.
     * @param  metadata    value of {@code connection.getMetaData()} (provided because already known by caller).
     * @param  isSpatial   whether the database contains "GEOMETRY_COLUMNS" and "SPATIAL_REF_SYS" tables.
     * @param  customizer  user-specified modification to the features, or {@code null} if none.
     */
    Analyzer(final Database<?> database, final Connection connection, final DatabaseMetaData metadata,
             final boolean isSpatial, final SchemaModifier customizer)
            throws SQLException
    {
        this.database      = database;
        this.tables        = new HashMap<>();
        this.strings       = new HashMap<>();
        this.warnings      = new LinkedHashSet<>();
        this.customizer    = customizer;
        this.metadata      = metadata;
        this.escape        = metadata.getSearchStringEscape();
        this.nameFactory   = DefaultFactories.forBuildin(NameFactory.class);
        spatialInformation = isSpatial ? database.createInfoStatements(connection) : null;
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
        return SQLUtilities.escape(pattern, escape);
    }

    /**
     * Reads a string from the given result set and return a unique instance of that string.
     * This method should be invoked only for {@code String} instances that are going to be
     * stored in {@link Table} or {@link Relation} structures; there is no point to invoke
     * this method for example before to parse the string as a boolean.
     *
     * @param  reflect  the result set from which to read a string.
     * @param  column   the column to read.
     * @return the value in the given column, returned as a unique string.
     */
    final String getUniqueString(final ResultSet reflect, final String column) throws SQLException {
        String value = reflect.getString(column);
        if (value != null) {
            final String p = strings.putIfAbsent(value, value);
            if (p != null) value = p;
        }
        return value;
    }

    /**
     * Returns a namespace for the given catalog and schema names, or {@code null} if all arguments are null.
     * The namespace sets the name separator to {@code '.'} instead of {@code ':'}.
     */
    final NameSpace namespace(final String catalog, final String schema) {
        if (!Objects.equals(this.schema, schema) || !Objects.equals(this.catalog, catalog)) {
            if (schema != null) {
                final GenericName name;
                if (catalog == null) {
                    name = nameFactory.createLocalName(null, schema);
                } else {
                    name = nameFactory.createGenericName(null, catalog, schema);
                }
                namespace = nameFactory.createNameSpace(name, Collections.singletonMap("separator", "."));
            } else {
                namespace = null;
            }
            this.catalog = catalog;
            this.schema  = schema;
        }
        return namespace;
    }

    /**
     * Returns the table of the given name if it exists, or creates it otherwise.
     * This method may be invoked recursively if the table to create is a dependency
     * of another table. If a cyclic dependency is detected, then this method returns
     * {@code null} for one of the tables.
     *
     * @param  id          identification of the table to create.
     * @param  name        the value of {@code id.getName(analyzer)}
     *                     (as an argument for avoiding re-computation when already known by the caller).
     * @param  importedBy  if this table is imported by the foreigner keys of another table, the parent table.
     *                     Otherwise {@code null}.
     * @return the table, or {@code null} if there is a cyclic dependency and
     *         the table of the given name is already in process of being created.
     */
    final Table table(final TableReference id, final GenericName name, final TableReference importedBy) throws Exception {
        Table table = tables.get(name);
        if (table == null && !tables.containsKey(name)) {
            tables.put(name, null);                       // Mark the feature as in process of being created.
            table = new Table(database, new ForTable(id, importedBy));
            if (tables.put(name, table) != null) {
                // Should never happen. If thrown, we have a bug (e.g. synchronization) in this package.
                throw new InternalDataStoreException(internalError());
            }
        }
        return table;
    }

    /**
     * Returns the localized resources for warnings and error messages.
     */
    final Resources resources() {
        return Resources.forLocale(database.listeners.getLocale());
    }

    /**
     * Returns a message for unexpected errors. Those errors are caused by a bug in this
     * {@code org.apache.sis.internal.sql.feature} package instead than a database issue.
     */
    final String internalError() {
        return resources().getString(Resources.Keys.InternalError);
    }

    /**
     * Reports a warning. Duplicated warnings will be ignored.
     *
     * @param  key       one of {@link Resources.Keys} values.
     * @param  argument  the value to substitute to {0} tag in the warning message.
     */
    private void warning(final short key, final Object argument) {
        warnings.add(Resources.formatInternational(key, argument));
    }

    /**
     * Returns the exception to throw if a column is duplicated.
     */
    private DataStoreContentException duplicatedColumn(final Column column) {
        return new DataStoreContentException(resources().getString(Resources.Keys.DuplicatedColumn_1, column.name));
    }

    /**
     * Invoked after we finished to create all tables. This method flushes the warnings
     * (omitting duplicated warnings), then returns all tables including dependencies.
     */
    final Collection<Table> finish() throws DataStoreException {
        for (final Table table : tables.values()) {
            table.setDeferredSearchTables(this, tables);
        }
        for (final ResourceInternationalString warning : warnings) {
            database.log(warning.toLogRecord(Level.WARNING));
        }
        return tables.values();
    }

    /**
     * Creates a new builder for a {@code FeatureType} inferred from a table.
     */
    private FeatureTypeBuilder createFeatureTypeBuilder() {
        return new FeatureTypeBuilder(nameFactory, database.geomLibrary.library, database.listeners.getLocale());
    }

    /**
     * Creates the feature type from the content of the given builder.
     */
    private DefaultFeatureType createFeatureType(final TableReference id, final FeatureTypeBuilder feature)
            throws DataStoreException
    {
        feature.setName(id.getName(this));
        return (customizer != null) ? customizer.editFeatureType(id, feature) : feature.build();
    }

    /**
     * Initializes the value getter on the given column.
     * This method shall be invoked only after geometry columns have been identifier.
     */
    private ValueGetter<?> setValueGetter(final Column column) {
        ValueGetter<?> getter = database.getMapping(column);
        if (getter == null) {
            getter = ValueGetter.AsObject.INSTANCE;
            warning(Resources.Keys.UnknownType_1, column.typeName);
        }
        column.valueGetter = getter;
        return getter;
    }

    /**
     * Constructor for a {@link Table} based on a "physical" table.
     * The table is identified by {@link #id}, which contains a (catalog, schema, name) tuple.
     */
    private final class ForTable extends TableAnalyzer {
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
         * The class of primary key values, or {@code null} if there is no primary key.
         * If the primary key use more than one column, then is the class of an array;
         * it may be an array of primitive type.
         *
         * <p>This field is computed as a side-effect of {@link #createAttributes(FeatureTypeBuilder)}.</p>
         *
         * @see PrimaryKey#valueClass
         */
        private Class<?> primaryKeyClass;

        /**
         * The columns that constitute the primary key, or an empty set if there is no primary key.
         */
        private final Set<String> primaryKey;

        /**
         * Foreigner keys that are referencing primary keys of other tables ({@link Relation.Direction#IMPORT}).
         * Keys are column names and values are information about the relation (referenced table, <i>etc</i>).
         * For each value, the list should contain exactly 1 element. But more elements are allowed because the
         * same column could be used as a component of more than one foreigner key. The list may contain nulls.
         *
         * <p>This map is populated as a side-effect of {@code getForeignerKeys(Direction.IMPORT, …)} call.</p>
         */
        private final Map<String, List<Relation>> foreignerKeys;

        /**
         * The builder builder where to append attributes and associations.
         */
        private final FeatureTypeBuilder feature;

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
        ForTable(final TableReference id, final TableReference importedBy) throws SQLException {
            super(id);
            this.importedBy = importedBy;
            tableEsc        = escape(id.table);
            schemaEsc       = escape(id.schema);
            primaryKey      = new LinkedHashSet<>();
            foreignerKeys   = new HashMap<>();
            feature         = createFeatureTypeBuilder();
            try (ResultSet reflect = metadata.getPrimaryKeys(id.catalog, id.schema, id.table)) {
                while (reflect.next()) {
                    primaryKey.add(getUniqueString(reflect, Reflection.COLUMN_NAME));
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
            try (ResultSet reflect = isImport ? metadata.getImportedKeys(id.catalog, id.schema, id.table)
                                              : metadata.getExportedKeys(id.catalog, id.schema, id.table))
            {
                if (reflect.next()) do {
                    final Relation relation = new Relation(Analyzer.this, direction, reflect);
                    if (isImport) {
                        Relation r = relation;
                        for (final String column : relation.getForeignerKeys()) {
                            CollectionsExt.addToMultiValuesMap(foreignerKeys, column, r);
                            r = null;       // Only the first column will be associated.
                        }
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
            try (ResultSet reflect = metadata.getColumns(id.catalog, schemaEsc, tableEsc, null)) {
                while (reflect.next()) {
                    final Column column = new Column(Analyzer.this, reflect);
                    if (columns.put(column.name, column) != null) {
                        throw duplicatedColumn(column);
                    }
                }
            }
            if (spatialInformation != null) {
                spatialInformation.completeGeometryColumns(id, columns);
            }
            /*
             * Analyze the type of each column, which may be geometric as a consequence of above call.
             */
            boolean primaryKeyNonNull = true;
            final List<Column> attributes = new ArrayList<>();
            for (final Column column : columns.values()) {
                final boolean        isPrimaryKey = primaryKey.contains(column.name);
                final List<Relation> dependencies = foreignerKeys.get(column.name);
                updateCaseHeuristic(column.label);
                /*
                 * Add the column as an attribute. Foreign keys are excluded (they will be replaced by associations),
                 * except if the column is also a primary key. In the later case we need to keep that column because
                 * it is needed for building the feature identifier.
                 */
                AttributeTypeBuilder<?> attribute = null;
                if (isPrimaryKey || dependencies == null) {
                    final ValueGetter<?> getter = setValueGetter(column);
                    attributes.add(column);
                    attribute = column.createAttribute(feature);
                    /*
                     * Some columns have special purposes: components of primary keys will be used for creating
                     * identifiers, some columns may contain a geometric object. Adding a role on those columns
                     * may create synthetic columns, for example "sis:identifier".
                     */
                    if (isPrimaryKey) {
                        attribute.addRole(AttributeRole.IDENTIFIER_COMPONENT);
                        primaryKeyNonNull &= !column.isNullable;
                        primaryKeyClass = Classes.findCommonClass(primaryKeyClass, getter.valueType);
                    }
                    /*
                     * If geometry columns are found, the first one will be defined as the default geometry.
                     * Note: a future version may allow user to select which column should be the default.
                     */
                    if (Geometries.isKnownType(getter.valueType)) {
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
                            final GenericName typeName = dependency.getName(Analyzer.this);
                            final Table table = table(dependency, typeName, id);
                            /*
                             * Use the column name as the association name, provided that the foreigner key
                             * uses only that column. If the foreigner key uses more than one column, then we
                             * do not know which column describes better the association (often there is none).
                             * In such case we use the foreigner key name as a fallback.
                             */
                            dependency.setPropertyName(column.label, count++);
                            final AssociationRoleBuilder association;
                            if (table != null) {
                                dependency.setSearchTable(Analyzer.this, table, table.primaryKey, Relation.Direction.IMPORT);
                                association = feature.addAssociation(table.featureType);
                            } else {
                                association = feature.addAssociation(typeName);     // May happen in case of cyclic dependency.
                            }
                            association.setName(dependency.propertyName);
                            if (column.isNullable) {
                                association.setMinimumOccurs(0);
                            }
                            /*
                             * If the column is also used in the primary key, then we have a name clash.
                             * Rename the primary key column with the addition of a "pk:" scope. We rename
                             * the primary key column instead than this association because the primary key
                             * column should rarely be used directly.
                             */
                            if (attribute != null) {
                                attribute.setName(nameFactory.createGenericName(null, "pk", column.label));
                                column.label = attribute.getName().toString();
                                attribute = null;
                            }
                        }
                    }
                }
            }
            if (primaryKey.size() > 1) {
                if (primaryKeyNonNull) {
                    primaryKeyClass = Numbers.wrapperToPrimitive(primaryKeyClass);
                }
                primaryKeyClass = Classes.changeArrayDimension(primaryKeyClass, 1);
            }
            return attributes.toArray(new Column[attributes.size()]);
        }

        /**
         * Adds the associations created by other tables having foreigner keys to this table.
         * We infer the column name from the target type. We may have a name clash with other columns,
         * in which case an arbitrary name change is applied.
         *
         * <h4>Side effects</h4>
         * <p><b>Required by this method:</b> {@link #primaryKeyClass}.</p>
         * <p><b>Computed by this method:</b> none.</p>
         *
         * @eturn the components of the primary key, or {@code null} if there is no primary key.
         */
        @Override
        final PrimaryKey createAssociations(final Relation[] exportedKeys) throws Exception {
            final PrimaryKey pk = PrimaryKey.create(primaryKeyClass, primaryKey);
            int count = 0;
            for (final Relation dependency : exportedKeys) {
                if (dependency != null) {
                    final GenericName typeName = dependency.getName(Analyzer.this);
                    String propertyName = toHeuristicLabel(typeName.tip().toString());
                    final String base = propertyName;
                    while (feature.isNameUsed(propertyName)) {
                        propertyName = base + '-' + ++count;
                    }
                    dependency.propertyName = propertyName;
                    final Table table = table(dependency, typeName, null);   // `null` because exported, not imported.
                    final AssociationRoleBuilder association;
                    if (table != null) {
                        dependency.setSearchTable(Analyzer.this, table, pk, Relation.Direction.EXPORT);
                        association = feature.addAssociation(table.featureType);
                    } else {
                        association = feature.addAssociation(typeName);     // May happen in case of cyclic dependency.
                    }
                    association.setName(propertyName)
                               .setMinimumOccurs(0)
                               .setMaximumOccurs(Integer.MAX_VALUE);
                }
            }
            return pk;
        }

        /**
         * Completes the creation of the feature type. This method adds:
         * <ul>
         *   <li>The feature name, which is derived from the table name when possible.</li>
         *   <li>An optional description of the application schema. This information is not used by computation,
         *       but allows to give end-user global information about the schema (s)he is manipulating.</li>
         * </ul>
         */
        @Override
        public DefaultFeatureType buildFeatureType() throws DataStoreException, SQLException {
            String remarks = id.freeText;
            if (id instanceof Relation) {
                try (ResultSet reflect = metadata.getTables(id.catalog, schemaEsc, tableEsc, null)) {
                    while (reflect.next()) {
                        remarks = Strings.trimOrNull(getUniqueString(reflect, Reflection.REMARKS));
                        if (remarks != null) {
                            break;
                        }
                    }
                }
            }
            if (remarks != null) {
                feature.setDefinition(remarks);
            }
            return createFeatureType(id, feature);
        }
    }

    /**
     * Constructor for a {@link Table} based on a query, to be considered as a virtual table.
     * Current implementation does not follow foreigner keys.
     *
     * @todo Not yet used. This is planed for future evolution.
     */
    private final class ForQuery extends TableAnalyzer {
        /**
         * The query submitted by user.
         */
        private final PreparedStatement query;

        /**
         * The builder builder where to append attributes and associations.
         */
        private final FeatureTypeBuilder feature;

        /**
         * Creates a new analyzer for a query.
         *
         * @param  name        name to give to the virtual table.
         * @param  query       the query as a prepared statement.
         * @param  definition  optional comments.
         */
        ForQuery(final String name, final PreparedStatement query, final String definition) throws SQLException {
            super(new TableReference(null, null, name, definition));
            this.query = query;
            feature = createFeatureTypeBuilder();
        }

        /**
         * Returns an empty array since current implementation does not follow foreigner keys.
         */
        @Override
        Relation[] getForeignerKeys(final Relation.Direction direction) {
            return Relation.EMPTY;
        }

        /**
         * Configures the feature builder with attributes inferred from the query.
         */
        @Override
        Column[] createAttributes() throws Exception {
            final ResultSetMetaData meta = query.getMetaData();
            final Column[] columns = new Column[meta.getColumnCount()];
            final Map<TableReference, Map<String,Column>> columnsPerTable = new HashMap<>();
            for (int i=1; i <= columns.length; i++) {
                final Column column = new Column(meta, i);
                columns[i-1] = column;
                /*
                 * In order to identify geometry columns, we need to know the table where the column come from.
                 * The `columnsPerTable` will contain the columns grouped by originating table.
                 */
                final String table = Strings.trimOrNull(meta.getTableName(i));
                if (table != null) {
                    final TableReference source = new TableReference(
                            Strings.trimOrNull(meta.getCatalogName(i)),
                            Strings.trimOrNull(meta.getSchemaName(i)),
                            table, null);
                    final Map<String,Column> c = columnsPerTable.computeIfAbsent(source, (k) -> new HashMap<>());
                    if (c.put(column.name, column) != null) {
                        throw duplicatedColumn(column);
                    }
                }
            }
            /*
             * Identify geometry columns. Must be done before the calls to `setValueGetter(column)`.
             */
            if (spatialInformation != null) {
                for (final Map.Entry<TableReference, Map<String,Column>> entry : columnsPerTable.entrySet()) {
                    spatialInformation.completeGeometryColumns(entry.getKey(), entry.getValue());
                }
            }
            /*
             * Creates attributes only after we updated all columns with geometry informations.
             */
            for (final Column column : columns) {
                setValueGetter(column);
                column.createAttribute(feature);
            }
            return columns;
        }

        /**
         * Do nothing since current implementation does not follow foreigner keys.
         */
        @Override
        PrimaryKey createAssociations(Relation[] exportedKeys) {
            return null;
        }

        /**
         * Creates the feature type.
         */
        @Override
        DefaultFeatureType buildFeatureType() throws DataStoreException {
            if (id.freeText != null) {
                feature.setDefinition(id.freeText);
            }
            return createFeatureType(id, feature);
        }
    }
}
