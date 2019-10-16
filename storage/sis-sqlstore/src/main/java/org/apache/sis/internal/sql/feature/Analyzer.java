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

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.sql.DataSource;

import org.opengis.feature.Feature;
import org.opengis.feature.PropertyType;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;

import org.apache.sis.feature.builder.AssociationRoleBuilder;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.metadata.sql.Dialect;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.internal.metadata.sql.SQLUtilities;
import org.apache.sis.internal.sql.feature.FeatureAdapter.PropertyMapper;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.InternalDataStoreException;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.logging.WarningListeners;
import org.apache.sis.util.resources.ResourceInternationalString;


/**
 * Helper methods for creating {@code FeatureType}s from database structure.
 * An instance of this class is created temporarily when starting the analysis
 * of a database structure, and discarded once the analysis is finished.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Analyzer {
    /**
     * Provider of (pooled) connections to the database. This is the main argument provided by users
     * when creating a {@link org.apache.sis.storage.sql.SQLStore}. This data source should be pooled,
     * because {@code SQLStore} will frequently opens and closes connections.
     */
    final DataSource source;

    /**
     * Information about the database as a whole.
     * Used for fetching tables, columns, primary keys <i>etc.</i>
     */
    final DatabaseMetaData metadata;

    /**
     * Functions that may be specific to the geospatial database in use.
     */
    final SpatialFunctions functions;

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
     * Names of tables to ignore when inspecting a database schema.
     * Those tables are used for database internal working (for example by PostGIS).
     */
    private final Set<String> ignoredTables;

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
     * Where to send warnings after we finished to collect them, or when reading the feature instances.
     */
    final WarningListeners<DataStore> listeners;

    /**
     * The locale for warning messages.
     */
    final Locale locale;

    /**
     * The last catalog and schema used for creating {@link #namespace}.
     * Used for determining if {@link #namespace} is still valid.
     */
    private transient String catalog, schema;

    /**
     * The namespace created with {@link #catalog} and {@link #schema}.
     */
    private transient NameSpace namespace;
    public static final Supplier<GenericName> RANDOME_NAME = () -> Names.createGenericName("sis", ":", UUID.randomUUID().toString());

    /**
     * Creates a new analyzer for the database described by given metadata.
     *
     * @param  source     the data source, usually given by user at {@code SQLStore} creation time.
     * @param  metadata   Value of {@code source.getConnection().getMetaData()}.
     * @param  listeners  Value of {@code SQLStore.listeners}.
     * @param  locale     Value of {@code SQLStore.getLocale()}.
     */
    Analyzer(final DataSource source, final DatabaseMetaData metadata, final WarningListeners<DataStore> listeners,
             final Locale locale) throws SQLException
    {
        this.source      = source;
        this.metadata    = metadata;
        this.listeners   = listeners;
        this.locale      = locale;
        this.strings     = new HashMap<>();
        this.escape      = metadata.getSearchStringEscape();
        this.functions   = new SpatialFunctions(metadata);
        this.nameFactory = DefaultFactories.forBuildin(NameFactory.class);
        /*
         * The following tables are defined by ISO 19125 / OGC Simple feature access part 2.
         * Note that the standard specified those names in upper-case letters, which is also
         * the default case specified by the SQL standard.  However some databases use lower
         * cases instead.
         */
        String crs  = "SPATIAL_REF_SYS";
        String geom = "GEOMETRY_COLUMNS";
        if (metadata.storesLowerCaseIdentifiers()) {
            crs  = crs .toLowerCase(Locale.US).intern();
            geom = geom.toLowerCase(Locale.US).intern();
        }
        ignoredTables = new HashSet<>(4);
        ignoredTables.add(crs);
        ignoredTables.add(geom);
        final Dialect dialect = Dialect.guess(metadata);
        if (dialect == Dialect.POSTGRESQL) {
            ignoredTables.add("geography_columns");     // Postgis 1+
            ignoredTables.add("raster_columns");        // Postgis 2
            ignoredTables.add("raster_overviews");
        }
        /*
         * Information to be collected during table analysis.
         */
        tables   = new HashMap<>();
        warnings = new LinkedHashSet<>();
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
    final String escape(final String pattern) {
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
     * Returns whether a table is reserved for database internal working.
     * If this method returns {@code false}, then the given table is a candidate
     * for use as a {@code FeatureType}.
     *
     * @param  name  database table name to test (case sensitive).
     * @return {@code true} if the named table should be ignored when looking for feature types.
     */
    final boolean isIgnoredTable(final String name) {
        return ignoredTables.contains(name);
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
     * Returns the feature of the given name if it exists, or creates it otherwise.
     * This method may be invoked recursively if the table to create as a dependency
     * to another table. If a cyclic dependency is detected, then this method return
     * {@code null} for one of the tables.
     *
     * @param  id          identification of the table to create.
     * @param  name        the value of {@code id.getName(analyzer)}
     *                     (as an argument for avoiding re-computation when already known by the caller).
     * @param  importedBy  if this table is imported by the foreigner keys of another table,
     *                     the parent table. Otherwise {@code null}.
     * @return the table, or {@code null} if there is a cyclic dependency and the table of the given
     *         name is already in process of being created.
     */
    final Table table(final TableReference id, final GenericName name, final TableReference importedBy)
            throws SQLException, DataStoreException
    {
        Table table = tables.get(name);
        if (table == null && !tables.containsKey(name)) {
            tables.put(name, null);                       // Mark the feature as in process of being created.
            table = new Table(this, id, importedBy);
            if (tables.put(name, table) != null) {
                // Should never happen. If thrown, we have a bug (e.g. synchronization) in this package.
                throw new InternalDataStoreException(internalError());
            }
        }
        return table;
    }

    /**
     * Returns a message for unexpected errors. Those errors are caused by a bug in this
     * {@code org.apache.sis.internal.sql.feature} package instead than a database issue.
     */
    final String internalError() {
        return Resources.forLocale(locale).getString(Resources.Keys.InternalError);
    }

    /**
     * Reports a warning. Duplicated warnings will be ignored.
     *
     * @param  key       one of {@link Resources.Keys} values.
     * @param  argument  the value to substitute to {0} tag in the warning message.
     */
    final void warning(final short key, final Object argument) {
        warnings.add(Resources.formatInternational(key, argument));
    }

    private PropertyAdapter analyze(SQLColumn target) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invoked after we finished to create all tables. This method flush the warnings
     * (omitting duplicated warnings), then returns all tables including dependencies.
     */
    final Collection<Table> finish() throws DataStoreException {
        for (final Table table : tables.values()) {
            table.setDeferredSearchTables(this, tables);
        }
        for (final ResourceInternationalString warning : warnings) {
            final LogRecord record = warning.toLogRecord(Level.WARNING);
            record.setSourceClassName(SQLStore.class.getName());
            record.setSourceMethodName("components");                // Main public API trigging the database analysis.
            listeners.warning(record);
        }
        return tables.values();
    }

    public SQLTypeSpecification create(final TableReference table, final TableReference importedBy) throws SQLException {
        return new TableMetadata(table, importedBy);
    }

    public SQLTypeSpecification create(final PreparedStatement target, final String sourceQuery, final GenericName optName) throws SQLException {
        return new QuerySpecification(target, sourceQuery, optName);
    }

    public FeatureAdapter buildAdapter(final SQLTypeSpecification spec) throws SQLException {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder(nameFactory, functions.library, locale);
        builder.setName(spec.getName().orElseGet(RANDOME_NAME));
        spec.getDefinition().ifPresent(builder::setDefinition);
        final String geomCol = spec.getPrimaryGeometryColumn().map(ColumnRef::getAttributeName).orElse("");
        final List pkCols = spec.getPK().map(PrimaryKey::getColumns).orElse(Collections.EMPTY_LIST);
        List<PropertyMapper> attributes = new ArrayList<>();
        // JDBC column indices are 1 based.
        int i = 0;
        for (SQLColumn col : spec.getColumns()) {
            i++;
            final SpatialFunctions.ColumnAdapter<?> colAdapter = functions.toJavaType(col.getType(), col.getTypeName());
            Class<?> type = colAdapter.javaType;
            final String colName = col.getName().getColumnName();
            final String attrName = col.getName().getAttributeName();
            if (type == null) {
                warning(Resources.Keys.UnknownType_1, colName);
                type = Object.class;
            }

            final AttributeTypeBuilder<?> attribute = builder
                    .addAttribute(type)
                    .setName(attrName);
            if (col.isNullable()) attribute.setMinimumOccurs(0);
            final int precision = col.getPrecision();
            /* TODO: we should check column type. Precision for numbers or blobs is meaningfull, but the convention
             * exposed by SIS does not allow to distinguish such cases.
             */
            if (precision > 0) attribute.setMaximalLength(precision);

            col.getCrs().ifPresent(attribute::setCRS);
            if (geomCol.equals(attrName)) attribute.addRole(AttributeRole.DEFAULT_GEOMETRY);

            if (pkCols.contains(colName)) attribute.addRole(AttributeRole.IDENTIFIER_COMPONENT);
            attributes.add(new PropertyMapper(attrName, i, colAdapter));
        }

        addImports(spec, builder);

        addExports(spec, builder);

        return new FeatureAdapter(builder.build(), attributes);
    }

    private void addExports(SQLTypeSpecification spec, FeatureTypeBuilder builder) throws SQLException {
        final List<Relation> exports;
        try {
            exports = spec.getExports();
        } catch (DataStoreContentException e) {
            throw new BackingStoreException(e);
        }

        for (final Relation r : exports) {
            try {
                final GenericName foreignTypeName = r.getName(Analyzer.this);
                final Table foreignTable = table(r, foreignTypeName, null); // 'null' because exported, not imported.
                final AssociationRoleBuilder association;
                if (foreignTable != null) {
                    r.setSearchTable(Analyzer.this, foreignTable, spec.getPK().map(PrimaryKey::getColumns).map(l -> l.toArray(new String[0])).orElse(null), Relation.Direction.EXPORT);
                    association = builder.addAssociation(foreignTable.featureType);
                } else {
                    association = builder.addAssociation(foreignTypeName);     // May happen in case of cyclic dependency.
                }
                association.setName(r.propertyName)
                        .setMinimumOccurs(0)
                        .setMaximumOccurs(Integer.MAX_VALUE);
            } catch (DataStoreException e) {
                throw new BackingStoreException(e);
            }
        }
    }

    private void addImports(SQLTypeSpecification spec, FeatureTypeBuilder target) throws SQLException {
        final List<Relation> imports = spec.getImports();
        for (Relation r : imports) {
            final GenericName foreignTypeName = r.getName(Analyzer.this);
            final Table foreignTable;
            try {
                foreignTable = table(r, foreignTypeName, spec instanceof TableMetadata ? ((TableMetadata) spec).id : null);
            } catch (DataStoreException e) {
                throw new BackingStoreException(e);
            }
            final AssociationRoleBuilder association = foreignTable == null ?
                    target.addAssociation(foreignTypeName) : target.addAssociation(foreignTable.featureType);
            association.setName(r.propertyName);
        }
    }

    private interface PropertyAdapter {
        PropertyType getType();
        void fill(ResultSet source, final Feature target);
    }

    private final class TableMetadata implements SQLTypeSpecification {
        final TableReference id;
        private final String tableEsc;
        private final String schemaEsc;

        private final Optional<PrimaryKey> pk;

        private final TableReference importedBy;

        private final List<SQLColumn> columns;

        private TableMetadata(TableReference source, TableReference importedBy) throws SQLException {
            this.id = source;
            this.importedBy = importedBy;
            tableEsc = escape(source.table);
            schemaEsc = escape(source.schema);

            try (ResultSet reflect = metadata.getPrimaryKeys(id.catalog, id.schema, id.table)) {
                final List<String> cols = new ArrayList<>();
                while (reflect.next()) {
                    cols.add(getUniqueString(reflect, Reflection.COLUMN_NAME));
                    // The actual Boolean value will be fetched in the loop on columns later.
                }
                pk = PrimaryKey.create(cols);
            }

            try (ResultSet reflect = metadata.getColumns(source.catalog, schemaEsc, tableEsc, null)) {

                final ArrayList<SQLColumn> tmpList = new ArrayList<>();
                while (reflect.next()) {
                    final int type = reflect.getInt(Reflection.DATA_TYPE);
                    final String typeName = reflect.getString(Reflection.TYPE_NAME);
                    final boolean isNullable = Boolean.TRUE.equals(SQLUtilities.parseBoolean(reflect.getString(Reflection.IS_NULLABLE)));
                    final ColumnRef name = new ColumnRef(getUniqueString(reflect, Reflection.COLUMN_NAME));
                    final int precision = reflect.getInt(Reflection.COLUMN_SIZE);
                    final SQLColumn col = new SQLColumn(type, typeName, isNullable, name, precision);
                    tmpList.add(col);
                }

                columns = Collections.unmodifiableList(tmpList);
            }
        }

        @Override
        public Optional<GenericName> getName() {
            return Optional.of(id.getName(Analyzer.this));
        }

        /**
         * The remarks are opportunistically stored in id.freeText if known by the caller.
         */
        @Override
        public Optional<String> getDefinition() throws SQLException {
            String remarks = id.freeText;
            if (id instanceof Relation) {
                try (ResultSet reflect = metadata.getTables(id.catalog, schemaEsc, tableEsc, null)) {
                    while (reflect.next()) {
                        remarks = getUniqueString(reflect, Reflection.REMARKS);
                        if (remarks != null) {
                            remarks = remarks.trim();
                            if (remarks.isEmpty()) {
                                remarks = null;
                            } else break;
                        }
                    }
                }
            }
            return Optional.ofNullable(remarks);
        }

        @Override
        public Optional<PrimaryKey> getPK() throws SQLException {
            return pk;
        }

        @Override
        public List<SQLColumn> getColumns() {
            return columns;
        }

        @Override
        public List<Relation> getImports() throws SQLException {
            try (ResultSet reflect = metadata.getImportedKeys(id.catalog, id.schema, id.table)) {
                if (!reflect.next()) return Collections.EMPTY_LIST;
                final List<Relation> imports = new ArrayList<>(2);
                do {
                    Relation r = new Relation(Analyzer.this, Relation.Direction.IMPORT, reflect);
                    final GenericName foreignTypeName = r.getName(Analyzer.this);
                    final Collection<String> fks = r.getForeignerKeys();
                    /* If the link is composed of a single foreign key, we'll name it after that name. Otherwise,
                     * we'll use constraint title if present. As a fallback, we take referenced table name, as it will
                     * surely be more explicit than a concatenation of column names.
                     * In all cases, we set "sis" name space, as we are making arbitrary choices specific to this
                     * framework.
                     */
                    if (fks.size() == 1) r.propertyName = Names.createGenericName(null, ":", "sis", fks.iterator().next());
                    else if (r.freeText != null && !r.freeText.isEmpty()) r.propertyName = Names.createGenericName(null,":","sis", r.freeText);
                    else r.propertyName = Names.createGenericName(null, ":", "sis", foreignTypeName.tip().toString());
                    imports.add(r);
                } while (!reflect.isClosed());
                return imports;
            } catch (DataStoreContentException e) {
                throw new BackingStoreException(e);
            }
        }

        @Override
        public List<Relation> getExports() throws SQLException, DataStoreContentException {
            try (ResultSet reflect = metadata.getExportedKeys(id.catalog, id.schema, id.table)) {
                if (!reflect.next()) return Collections.EMPTY_LIST;
                final List<Relation> exports = new ArrayList<>(2);
                do {
                    final Relation export = new Relation(Analyzer.this, Relation.Direction.EXPORT, reflect);
                    final GenericName foreignTypeName = export.getName(Analyzer.this);
                    final String propertyName = foreignTypeName.tip().toString();
                    export.propertyName = Names.createGenericName(null, ":", "sis", propertyName);
                    if (!export.equals(importedBy)) {
                        exports.add(export);
                    }
                } while (!reflect.isClosed());
                return exports;
            }
        }

        @Override
        public Optional<ColumnRef> getPrimaryGeometryColumn() {
            return Optional.empty();
            //throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 20/09/2019
        }
    }

    private final class QuerySpecification implements SQLTypeSpecification {

        final int total;
        final PreparedStatement source;
        private final ResultSetMetaData meta;
        private final String query;
        private final GenericName name;

        private final List<SQLColumn> columns;

        public QuerySpecification(PreparedStatement source, String sourceQuery, GenericName optName) throws SQLException {
            this.source = source;
            meta = source.getMetaData();
            total = meta.getColumnCount();
            query = sourceQuery;
            name = optName;

            final ArrayList<SQLColumn> tmpCols = new ArrayList<>(total);
            for (int i = 1 ; i <= total ; i++) {
                tmpCols.add(new SQLColumn(
                        meta.getColumnType(i),
                        meta.getColumnTypeName(i),
                        meta.isNullable(i) == ResultSetMetaData.columnNullable,
                        new ColumnRef(meta.getColumnName(i)).as(meta.getColumnLabel(i)),
                        meta.getPrecision(i)
                ));
            }

            columns = Collections.unmodifiableList(tmpCols);
        }

        @Override
        public Optional<GenericName> getName() throws SQLException {
            return Optional.ofNullable(name);
        }

        @Override
        public Optional<String> getDefinition() throws SQLException {
            return Optional.of(query);
        }

        @Override
        public Optional<PrimaryKey> getPK() throws SQLException {
            return Optional.empty();
        }

        @Override
        public List<SQLColumn> getColumns() {
            return columns;
        }

        @Override
        public List<Relation> getImports() throws SQLException {
            return Collections.EMPTY_LIST;
        }

        @Override
        public List<Relation> getExports() throws SQLException, DataStoreContentException {
            return Collections.EMPTY_LIST;
        }
    }

}
