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
import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.LogRecord;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.DataSource;
import org.opengis.util.GenericName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.internal.metadata.sql.Syntax;
import org.apache.sis.internal.metadata.sql.Dialect;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryType;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.sql.postgis.Postgres;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.sql.ResourceDefinition;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.collection.FrequencySortedSet;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.Debug;


/**
 * Information about a connection to a spatial database and the structure of features in the database.
 * This class provides functions for converting objects between the types used in the Java language and the types or
 * SQL expressions expected by JDBC. Conversions may be straightforward (e.g. invoke {@link ResultSet#getInt(int)}
 * and wraps the result in an {@link Integer} if it {@linkplain ResultSet#wasNull() was not null}) or can be a more
 * complex process (e.g. decode a geometry Well-Known Binary). This class does not perform the conversions directly,
 * but instead provides a converter object for each specified SQL type.
 *
 * <p>This base class provides mapping for common types (text, numbers, temporal objects, <i>etc.</i>)
 * and for geometry types as specified in <a href="https://www.ogc.org/standards/sfs">OpenGIS®
 * Implementation Standard for Geographic information — Simple feature access — Part 2: SQL option</a>.
 * Subclasses can override some functions if a particular database software (e.g. PostGIS) provides
 * specialized methods or have non-standard behavior for some data types.</p>
 *
 * <h2>Specializations</h2>
 * Subclasses may be defined for some database engines. Methods that can be overridden are:
 * <ul>
 *   <li>{@link #getMapping(Column)}                for adding column types to recognize.</li>
 *   <li>{@link #createInfoStatements(Connection)}  for more info about spatial information.</li>
 *   <li>{@link #addIgnoredTables(Set)}             for specifying more tables to ignore.</li>
 * </ul>
 *
 * <h2>Multi-threading</h2>
 * This class is safe for concurrent use by many threads. This class does not hold JDBC resources such as
 * {@link Connection}. Those resources are created temporarily when needed by {@link InfoStatements}.
 *
 * <h2>Schema updates</h2>
 * Current implementation does not track changes in the database schema.
 * if the database schema changes, then a new {@code Database} instance shall be created.
 *
 * @param  <G>  the type of geometry objects. Depends on the backing implementation (ESRI, JTS, Java2D…).
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class Database<G> extends Syntax  {
    /**
     * The SQL wildcard for any characters. A string containing only this wildcard
     * means "any value" and can sometime be replaced by {@code null}.
     */
    public static final String WILDCARD = "%";

    /**
     * Provider of (pooled) connections to the database.
     */
    protected final DataSource source;

    /**
     * The factory to use for creating geometric objects.
     * For example the geometry implementations may be ESRI, JTS or Java2D objects.
     */
    final Geometries<G> geomLibrary;

    /**
     * Whether {@link Types#TINYINT} is a signed integer. Both conventions (-128 … 127 range and 0 … 255 range)
     * are found on the web. If unspecified, we conservatively assume unsigned bytes.
     * All other integer types are presumed signed.
     */
    private final boolean isByteSigned;

    /**
     * All tables known to this {@code Database}. Populated in the constructor,
     * and shall not be modified after construction for preserving thread-safety.
     */
    private final FeatureNaming<Table> tablesByNames;

    /**
     * All tables known to this {@code Database} in declaration order.
     * This array contains only the tables specified at initialization time, not the dependencies.
     * This field is initialized by {@link #analyze(SQLStore, Connection, GenericName[])} and shall
     * not be modified after that point.
     */
    private Table[] tables;

    /**
     * {@code true} if this database contains at least one geometry column.
     * This field is initialized by {@link #analyze(SQLStore, Connection, GenericName[])}
     * and shall not be modified after that point.
     *
     * @see #hasGeometry()
     */
    private boolean hasGeometry;

    /**
     * Catalog and schema of the {@value InfoStatements#GEOMETRY_COLUMNS} and
     * {@value InfoStatements#SPATIAL_REF_SYS} tables, or null or empty string if none.
     */
    String catalogOfSpatialTables, schemaOfSpatialTables;

    /**
     * Whether catalog or schema are supported.
     */
    final boolean supportsCatalogs, supportsSchemas;

    /**
     * The converter from filters/expressions to the {@code WHERE} part of SQL statement.
     * This is initialized when first needed, then kept unmodified for the database lifetime.
     * Subclasses may provide a specialized instance if their database supports an extended
     * syntax for some filters or expressions.
     *
     * @see #getFilterToSupportedSQL()
     */
    private SelectionClauseWriter filterToSQL;

    /**
     * Where to send warnings.
     *
     * @see #log(LogRecord)
     */
    final StoreListeners listeners;

    /**
     * Cache of Coordinate Reference Systems created for a given SRID.
     * SRID are primary keys in the {@value InfoStatements#SPATIAL_REF_SYS} table.
     * They are not EPSG codes, even if the numerical values are often the same.
     *
     * <p>This mapping depend on the content of {@value InfoStatements#SPATIAL_REF_SYS} table.
     * For that reason, a distinct cache exists for each database.</p>
     */
    final Cache<Integer, CoordinateReferenceSystem> cacheOfCRS;

    /**
     * Creates a new handler for a spatial database.
     *
     * @param  source       provider of (pooled) connections to the database.
     * @param  metadata     metadata about the database.
     * @param  geomLibrary  the factory to use for creating geometric objects.
     * @param  listeners    where to send warnings.
     * @throws SQLException if an error occurred while reading database metadata.
     */
    protected Database(final DataSource source, final DatabaseMetaData metadata,
                       final Geometries<G> geomLibrary, final StoreListeners listeners)
            throws SQLException
    {
        super(metadata, true);
        /*
         * Get information about whether byte are unsigned.
         * According JDBC specification, the rows shall be ordered by DATA_TYPE.
         * But the PostgreSQL driver 42.2.2 still provides rows in random order.
         */
        boolean unsigned = true;
        try (ResultSet reflect = metadata.getTypeInfo()) {
            while (reflect.next()) {
                if (reflect.getInt(Reflection.DATA_TYPE) == Types.TINYINT) {
                    unsigned = reflect.getBoolean(Reflection.UNSIGNED_ATTRIBUTE);
                    if (unsigned) break;        // Give precedence to "true" value.
                }
            }
        }
        this.source        = source;
        this.isByteSigned  = !unsigned;
        this.geomLibrary   = geomLibrary;
        this.listeners     = listeners;
        this.cacheOfCRS    = new Cache<>(7, 2, false);
        this.tablesByNames = new FeatureNaming<>();
        supportsCatalogs   = metadata.supportsCatalogsInDataManipulation();
        supportsSchemas    = metadata.supportsSchemasInDataManipulation();
    }

    /**
     * Creates a new handler for a spatial database.
     *
     * @param  store        the data store for which we are creating a model. Used only in case of error.
     * @param  source       provider of (pooled) connections to the database.
     * @param  connection   connection to the database. Sometime the caller already has a connection at hand.
     * @param  geomLibrary  the factory to use for creating geometric objects.
     * @param  tableNames   qualified name of the tables. Specified by users at construction time.
     * @param  queries      additional resources associated to SQL queries. Specified by users at construction time.
     * @param  customizer   user-specified modification to the features, or {@code null} if none.
     * @param  listeners    where to send warnings.
     * @return handler for the spatial database.
     * @throws SQLException if a database error occurred while reading metadata.
     * @throws DataStoreException if a logical error occurred while analyzing the database structure.
     */
    public static Database<?> create(final SQLStore store, final DataSource source, final Connection connection,
            final GeometryLibrary geomLibrary, final GenericName[] tableNames, final ResourceDefinition[] queries,
            final SchemaModifier customizer, final StoreListeners listeners)
            throws Exception
    {
        final DatabaseMetaData metadata = connection.getMetaData();
        final Geometries<?> g = Geometries.implementation(geomLibrary);
        final Database<?> db;
        switch (Dialect.guess(metadata)) {
            case POSTGRESQL: db = new Postgres<>(source, connection, metadata, g, listeners); break;
            default: {
                db = new Database<>(source, metadata, g, listeners);
                break;
            }
        }
        db.analyze(store, connection, tableNames, queries, customizer);
        return db;
    }

    /**
     * Creates a model about the specified tables in the database.
     * This method shall be invoked exactly once after {@code Database} construction.
     * It requires a list of tables to include in the model, but this list should not
     * include the dependencies; this method will follow foreigner keys automatically.
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
     * @param  store       the data store for which we are creating a model. Used only in case of error.
     * @param  connection  connection to the database. Sometime the caller already has a connection at hand.
     * @param  tableNames  qualified name of the tables. Specified by users at construction time.
     * @param  queries     additional resources associated to SQL queries. Specified by users at construction time.
     * @param  customizer  user-specified modification to the features, or {@code null} if none.
     * @throws SQLException if a database error occurred while reading metadata.
     * @throws DataStoreException if a logical error occurred while analyzing the database structure.
     */
    private void analyze(final SQLStore store, final Connection connection, final GenericName[] tableNames,
                         final ResourceDefinition[] queries, final SchemaModifier customizer) throws Exception
    {
        final DatabaseMetaData metadata = connection.getMetaData();
        final String[] tableTypes = getTableTypes(metadata);
        /*
         * The following tables are defined by ISO 19125 / OGC Simple feature access part 2.
         * Note that the standard specified those names in upper-case letters, which is also
         * the default case specified by the SQL standard.  However some databases use lower
         * cases instead.
         */
        String tableCRS  = InfoStatements.SPATIAL_REF_SYS;
        String tableGeom = InfoStatements.GEOMETRY_COLUMNS;
        if (metadata.storesLowerCaseIdentifiers()) {
            tableCRS  = tableCRS .toLowerCase(Locale.US).intern();
            tableGeom = tableGeom.toLowerCase(Locale.US).intern();
        }
        final Set<String> ignoredTables = new HashSet<>(8);
        ignoredTables.add(tableCRS);
        ignoredTables.add(tableGeom);
        addIgnoredTables(ignoredTables);
        final boolean isSpatial = hasTable(metadata, tableTypes, ignoredTables);
        /*
         * Collect the names of all tables specified by user, ignoring the tables
         * used for database internal working (for example by PostGIS).
         */
        final Analyzer analyzer = new Analyzer(this, connection, metadata, isSpatial, customizer);
        final Set<TableReference> declared = new LinkedHashSet<>();
        for (final GenericName tableName : tableNames) {
            final String[] names = TableReference.splitName(tableName);
            try (ResultSet reflect = metadata.getTables(names[2], names[1], names[0], tableTypes)) {
                while (reflect.next()) {
                    final String table = analyzer.getUniqueString(reflect, Reflection.TABLE_NAME);
                    if (ignoredTables.contains(table)) {
                        continue;
                    }
                    declared.add(new TableReference(
                            analyzer.getUniqueString(reflect, Reflection.TABLE_CAT),
                            analyzer.getUniqueString(reflect, Reflection.TABLE_SCHEM), table,
                            analyzer.getUniqueString(reflect, Reflection.REMARKS)));
                }
            }
        }
        /*
         * At this point we got the list of tables requested by the user. Now create the Table objects for each
         * specified name. During this iteration, we may discover new tables to analyze because of dependencies
         * (foreigner keys).
         */
        final List<Table> tableList;
        tableList = new ArrayList<>(tableNames.length);
        for (final TableReference reference : declared) {
            // Adds only the table explicitly required by the user.
            tableList.add(analyzer.table(reference, reference.getName(analyzer), null));
        }
        /*
         * Add queries if any.
         */
        for (final ResourceDefinition resource : queries) {
            // Optional value should always be present in this context.
            tableList.add(analyzer.query(resource.getName(), resource.getQuery().get()));
        }
        /*
         * At this point we finished to create the tables explicitly requested by the user.
         * Register all tables only at this point, because other tables (dependencies) may
         * have been analyzed as a side-effect of above loop.
         */
        for (final Table table : analyzer.finish()) {
            tablesByNames.add(store, table.featureType.getName(), table);
            hasGeometry |= table.hasGeometry;
        }
        tables = tableList.toArray(new Table[tableList.size()]);
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
     * Returns {@code true} if the database contains all specified tables. This method updates
     * {@link #schemaOfSpatialTables} and {@link #catalogOfSpatialTables} for the tables found.
     * If many occurrences of the same table are found, this method searches for a common pair
     * of catalog and schema names. All tables should be in the same (catalog, schema) pair.
     * If this is not the case, no (catalog,schema) will be used and the search for the tables
     * will rely on the database "search path".
     *
     * @param  metadata    value of {@code connection.getMetaData()}.
     * @param  tableTypes  value of {@link #getTableTypes(DatabaseMetaData)}.
     * @param  tables      name of the table to search.
     * @return whether the given table has been found.
     */
    private boolean hasTable(final DatabaseMetaData metadata, final String[] tableTypes, final Set<String> tables)
            throws SQLException
    {
        // `SimpleImmutableEntry` used as a way to store a (catalog,schema) pair of strings.
        final FrequencySortedSet<SimpleImmutableEntry<String,String>> schemas = new FrequencySortedSet<>(true);
        int count = 0;
        for (final String name : tables) {
            boolean found = false;
            try (ResultSet reflect = metadata.getTables(null, null, name, tableTypes)) {
                while (reflect.next()) {
                    found = true;
                    schemas.add(new SimpleImmutableEntry<>(
                            reflect.getString(Reflection.TABLE_CAT),
                            reflect.getString(Reflection.TABLE_SCHEM)));
                }
            }
            if (found) count++;
        }
        if (count == 0) {
            return false;
        }
        final SimpleImmutableEntry<String,String> f = schemas.first();      // Most frequent pair.
        if (schemas.frequency(f) == count) {
            catalogOfSpatialTables = f.getKey();
            schemaOfSpatialTables  = f.getValue();
        }
        return true;
    }

    /**
     * Stores information about tables in the given metadata.
     * Only tables explicitly requested by the user are listed.
     *
     * @param  metadata  information about the database.
     * @param  builder   where to add information about the tables.
     * @throws SQLException if an error occurred while fetching table information.
     */
    public final void listTables(final DatabaseMetaData metadata, final MetadataBuilder builder) throws SQLException {
        for (final Table table : tables) {
            builder.addFeatureType(table.featureType, table.countRows(metadata, false, false));
        }
    }

    /**
     * Returns all tables in declaration order.
     * The list contains only the tables explicitly requested at construction time.
     *
     * @return all tables in an unmodifiable list.
     */
    public final List<FeatureSet> tables() {
        return UnmodifiableArrayList.wrap(tables);
    }

    /**
     * Returns the table for the given name.
     * The given name may be one of the tables specified at construction time, or one of its dependencies.
     *
     * @param  store  the data store for which we are fetching a table. Used only in case of error.
     * @param  name   name of the table to fetch.
     * @return the table (never null).
     * @throws IllegalNameException if no table of the given name is found or if the name is ambiguous.
     */
    public final FeatureSet findTable(final SQLStore store, final String name) throws IllegalNameException {
        return tablesByNames.get(store, name);
    }

    /**
     * Returns {@code true} if this database contains at least one geometry column.
     *
     * @return whether at least one geometry column has been found.
     */
    public final boolean hasGeometry() {
        return hasGeometry;
    }

    /**
     * Returns a function for getting values from a column having the given definition.
     * The given definition should include data SQL type and type name.
     * If no match is found, then this method returns {@code null}.
     *
     * <p>The default implementation handles types declared in the {@link Types} class
     * and the geometry types defined in the spatial extensions defined by OGC standard.
     * Subclasses should override if some types need to be handle in a non-standard way
     * for a particular database product.</p>
     *
     * @param  columnDefinition  information about the column to extract values from and expose through Java API.
     * @return converter to the corresponding java type, or {@code null} if this class can not find a mapping.
     */
    @SuppressWarnings("fallthrough")
    protected ValueGetter<?> getMapping(final Column columnDefinition) {
        if ("geometry".equalsIgnoreCase(columnDefinition.typeName)) {
            return forGeometry(columnDefinition);
        }
        switch (columnDefinition.type) {
            case Types.BIT:
            case Types.BOOLEAN:                   return ValueGetter.AsBoolean.INSTANCE;
            case Types.TINYINT: if (isByteSigned) return ValueGetter.AsByte.INSTANCE;       // else fallthrough.
            case Types.SMALLINT:                  return ValueGetter.AsShort.INSTANCE;
            case Types.INTEGER:                   return ValueGetter.AsInteger.INSTANCE;
            case Types.BIGINT:                    return ValueGetter.AsLong.INSTANCE;
            case Types.REAL:                      return ValueGetter.AsFloat.INSTANCE;
            case Types.FLOAT:                     // Despite the name, this is implemented as DOUBLE in major databases.
            case Types.DOUBLE:                    return ValueGetter.AsDouble.INSTANCE;
            case Types.NUMERIC:                   // Similar to DECIMAL except that it uses exactly the specified precision.
            case Types.DECIMAL:                   return ValueGetter.AsBigDecimal.INSTANCE;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:               return ValueGetter.AsString.INSTANCE;
            case Types.DATE:                      return ValueGetter.AsDate.INSTANCE;
            case Types.TIME:                      return ValueGetter.AsLocalTime.INSTANCE;
            case Types.TIMESTAMP:                 return ValueGetter.AsInstant.INSTANCE;
            case Types.TIME_WITH_TIMEZONE:        return ValueGetter.AsOffsetTime.INSTANCE;
            case Types.TIMESTAMP_WITH_TIMEZONE:   return ValueGetter.AsOffsetDateTime.INSTANCE;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:             return ValueGetter.AsBytes.INSTANCE;
            case Types.ARRAY:                     // TODO
            case Types.OTHER:
            case Types.JAVA_OBJECT:               return ValueGetter.AsObject.INSTANCE;
            default:                              return null;
        }
    }

    /**
     * Returns a function for getting values from a geometry column.
     * This is a helper method for {@link #getMapping(Column)} implementations.
     *
     * @param  columnDefinition  information about the column to extract values from and expose through Java API.
     * @return converter to the corresponding java type, or {@code null} if this class can not find a mapping,
     */
    protected final ValueGetter<?> forGeometry(final Column columnDefinition) {
        final GeometryType type = columnDefinition.getGeometryType();
        final Class<? extends G> geometryClass = geomLibrary.getGeometryClass(type).asSubclass(geomLibrary.rootClass);
        /*
         * TODO: verify if the condition below works. We should have `hexadecimal = true` on PostGIS.
         */
        final boolean hexadecimal = (columnDefinition.type != Types.BLOB);
        return new EWKBReader<>(geomLibrary, geometryClass, columnDefinition.getGeometryCRS(), hexadecimal);
        // TODO: need to invoke EWKBReader.setSridResolver(statements(…)) somewhere.
    }

    /**
     * Prepares a cache of statements about spatial information using the given connection.
     * Statements will be created only when first needed.
     *
     * @param  connection  the connection to use for creating statements.
     * @return a cache of prepared statements about spatial information.
     */
    protected InfoStatements createInfoStatements(final Connection connection) {
        return new InfoStatements(this, connection);
    }

    /**
     * Adds to the given set a list of tables to ignore when searching for feature tables.
     * The given set already contains the {@code "SPATIAL_REF_SYS"} and {@code "GEOMETRY_COLUMNS"}
     * entries when this method is invoked. The default implementation adds nothing.
     *
     * @param  ignoredTables  where to add names of tables to ignore.
     */
    protected void addIgnoredTables(final Set<String> ignoredTables) {
    }

    /**
     * Returns the converter from filters/expressions to the {@code WHERE} part of SQL statement.
     * Subclasses should override this method if their database supports an extended syntax for
     * some filters or expressions.
     *
     * <p>The returned instance is usually a singleton instance.
     * The caller of this method may create a copy of the returned instance for removing
     * some functions that are found to be unsupported by the database software.
     * Consequently implementation of this method can assume that the database supports
     * all the spatial operations managed by the returned writer.</p>
     *
     * @return the converter from filters/expressions to the {@code WHERE} part of SQL statement.
     */
    protected SelectionClauseWriter getFilterToSQL() {
        return SelectionClauseWriter.DEFAULT;
    }

    /**
     * Returns the converter from filters/expressions to the {@code WHERE} part of SQL statement
     * without the functions that are unsupported by the database software.
     */
    final synchronized SelectionClauseWriter getFilterToSupportedSQL() {
        if (filterToSQL == null) {
            filterToSQL = getFilterToSQL().removeUnsupportedFunctions(this);
        }
        return filterToSQL;
    }

    /**
     * Sets the logger, class and method names of the given record, then logs it.
     * This method declares {@link SQLStore#components()} as the public source of the log.
     *
     * @param  record  the record to configure and log.
     */
    protected final void log(final LogRecord record) {
        record.setSourceClassName(SQLStore.class.getName());
        record.setSourceMethodName("components");                // Main public API trigging the database analysis.
        record.setLoggerName(Modules.SQL);
        listeners.warning(record);
    }

    /**
     * Creates a tree representation of this database for debugging purpose.
     *
     * @param  parent  the parent node where to add the tree representation.
     */
    @Debug
    final void appendTo(final TreeTable.Node parent) {
        for (final Table child : tables) {
            child.appendTo(parent);
        }
    }

    /**
     * Formats a graphical representation of this database for debugging purpose. This representation can
     * be printed to the {@linkplain System#out standard output stream} (for example) if the output device
     * uses a monospaced font and supports Unicode.
     *
     * @return string representation of this database.
     */
    @Override
    public String toString() {
        return TableReference.toString(this, (n) -> appendTo(n));
    }
}
