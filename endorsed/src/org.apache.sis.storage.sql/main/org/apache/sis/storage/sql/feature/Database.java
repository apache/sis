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

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.LogRecord;
import java.util.concurrent.locks.ReadWriteLock;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import javax.sql.DataSource;
import org.opengis.util.GenericName;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.apache.sis.metadata.sql.privy.Syntax;
import org.apache.sis.metadata.sql.privy.Dialect;
import org.apache.sis.metadata.sql.privy.Reflection;
import org.apache.sis.metadata.sql.privy.SQLBuilder;
import org.apache.sis.metadata.sql.privy.SQLUtilities;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.system.Modules;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.sql.ResourceDefinition;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.Debug;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.privy.UnmodifiableArrayList;


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
 *   <li>{@link #getPossibleSpatialSchemas(Map)}    for enumerating the spatial schema conventions that may be used.</li>
 *   <li>{@link #getMapping(Column)}                for adding column types to recognize.</li>
 *   <li>{@link #createInfoStatements(Connection)}  for more info about spatial information.</li>
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
 */
public class Database<G> extends Syntax  {
    /**
     * The SQL wildcard for any characters. A string containing only this wildcard
     * means "any value" and can sometimes be replaced by {@code null}.
     */
    public static final String WILDCARD = "%";

    /**
     * Provider of (pooled) connections to the database.
     */
    protected final DataSource source;

    /**
     * The factory to use for creating geometric objects.
     * For example, the geometry implementations may be ESRI, JTS or Java2D objects.
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
     * This field is initialized by {@link #analyze analyze(…)} and shall not be modified after that point.
     */
    private Table[] tables;

    /**
     * Information about table names and column names used for the spatial schema, or {@code null}.
     * This is non-null if the database contains "GEOMETRY_COLUMNS" and/or "SPATIAL_REF_SYS" tables,
     * possibly with different name depending on the conventions of the spatial schema. May also be
     * non-null if some database-specific tables are found such as {@code "geography_columns"} and
     * {@code "raster_columns"} in PostGIS.
     *
     * This field is initialized by {@link #analyze analyze(…)} and shall not be modified after that point.
     *
     * @see #getSpatialSchema()
     */
    private SpatialSchema spatialSchema;

    /**
     * The encoding of Coordinate Reference Systems in the spatial database, in preference order.
     * This field is initialized by {@link #analyze analyze(…)} and shall not be modified after that point.
     */
    final EnumSet<CRSEncoding> crsEncodings;

    /**
     * {@code true} if this database contains at least one geometry column.
     * This field is initialized by {@link #analyze analyze(…)} and shall not be modified after that point.
     */
    private boolean hasGeometry;

    /**
     * {@code true} if this database contains at least one raster column.
     * This field is initialized by {@link #analyze analyze(…)} and shall not be modified after that point.
     */
    private boolean hasRaster;

    /**
     * Catalog and schema of the {@code "GEOMETRY_COLUMNS"} and {@code "SPATIAL_REF_SYS"} tables,
     * or null or empty string if none. The actual table names depend on {@link #spatialSchema}.
     */
    String catalogOfSpatialTables, schemaOfSpatialTables;

    /**
     * Whether catalog or schema are supported.
     */
    final boolean supportsCatalogs, supportsSchemas;

    /**
     * Whether the JDBC driver supports conversions from objects to {@code java.time} API.
     * The JDBC 4.2 specification provides a mapping from {@link java.sql.Types} to temporal objects.
     * The specification suggests that {@link java.sql.ResultSet#getObject(int, Class)} should accept
     * those temporal types in the {@link Class} argument, but not all drivers support that.
     */
    private final boolean supportsJavaTime;

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
     * The lock for read or write operations in the SQL database, or {@code null} if none.
     * The read or write lock should be obtained before to get a connection for executing
     * a statement, and released after closing the connection. Locking is assumed unneeded
     * for obtaining database metadata.
     *
     * <p>This field should be null if the database manages concurrent transactions by itself.
     * It is non-null only as a workaround for databases that do not support concurrency.</p>
     */
    protected final ReadWriteLock transactionLocks;

    /**
     * The locale to use for international texts to write in the database, or {@code null} for default.
     */
    protected final Locale contentLocale;

    /**
     * Where to send warnings.
     *
     * @see #log(LogRecord)
     */
    public final StoreListeners listeners;

    /**
     * Cache of Coordinate Reference Systems created for a given SRID.
     * SRID are primary keys in the {@code "SPATIAL_REF_SYS"} (or equivalent) table.
     * They are not EPSG codes, even if the numerical values are often the same.
     *
     * <p>This mapping depends on the content of {@code "SPATIAL_REF_SYS"} (or equivalent) table.
     * For that reason, a distinct cache exists for each database.</p>
     */
    final Cache<Integer, CoordinateReferenceSystem> cacheOfCRS;

    /**
     * Cache of SRID for a given Coordinate Reference System.
     * This is the converse of {@link #cacheOfCRS}.
     * Accesses to this map must be synchronized on the map itself.
     */
    final WeakHashMap<CoordinateReferenceSystem, Integer> cacheOfSRID;

    /**
     * Creates a new handler for a spatial database.
     *
     * @param  source         provider of (pooled) connections to the database.
     * @param  metadata       metadata about the database.
     * @param  dialect        additional information not provided by {@code metadata}.
     * @param  geomLibrary    the factory to use for creating geometric objects.
     * @param  contentLocale  the locale to use for international texts to write in the database, or {@code null} for default.
     * @param  listeners      where to send warnings.
     * @param  locks          the read/write locks, or {@code null} if none.
     * @throws SQLException if an error occurred while reading database metadata.
     */
    protected Database(final DataSource source, final DatabaseMetaData metadata, final Dialect dialect,
                       final Geometries<G> geomLibrary, final Locale contentLocale, final StoreListeners listeners,
                       final ReadWriteLock locks)
            throws SQLException
    {
        super(metadata, true);
        /*
         * Get information about whether byte are unsigned.
         * According JDBC specification, the rows shall be ordered by DATA_TYPE.
         * But the PostgreSQL driver 42.2.2 still provides rows in random order.
         * Also, if we find a row about `TINYINT` but without sign information,
         * continue looping in case the type is duplicated with more information later.
         * If no row is found for `TINYINT`, do not log any warning because it simply
         * means that the database does not support that data type.
         */
        boolean unsigned = true;
        boolean wasNull = false;    // Not the same as allowing `unsigned` to be null.
        SQLException cause = null;
        try (ResultSet reflect = metadata.getTypeInfo()) {
            while (reflect.next()) {
                if (reflect.getInt(Reflection.DATA_TYPE) == Types.TINYINT) {
                    unsigned  = reflect.getBoolean(Reflection.UNSIGNED_ATTRIBUTE);
                    wasNull   = reflect.wasNull();
                    unsigned |= wasNull;
                    if (!wasNull) break;
                }
            }
        } catch (SQLFeatureNotSupportedException e) {
            cause = e;
        }
        if (cause != null || wasNull) {
            listeners.warning(Resources.forLocale(listeners.getLocale())
                                       .getString(Resources.Keys.AssumeUnsigned), cause);
        }
        this.source        = source;
        this.isByteSigned  = !unsigned;
        this.geomLibrary   = geomLibrary;
        this.contentLocale = contentLocale;
        this.listeners     = listeners;
        this.cacheOfCRS    = new Cache<>(7, 2, false);
        this.cacheOfSRID   = new WeakHashMap<>();
        this.tablesByNames = new FeatureNaming<>();
        supportsCatalogs   = metadata.supportsCatalogsInDataManipulation();
        supportsSchemas    = metadata.supportsSchemasInDataManipulation();
        supportsJavaTime   = dialect.supportsJavaTime();
        crsEncodings       = EnumSet.noneOf(CRSEncoding.class);
        transactionLocks   = dialect.supportsConcurrency() ? null : locks;
    }

    /**
     * Detects automatically which spatial schema is in use. Detects also the catalog name and schema name.
     * This method is invoked exactly once after construction and before the analysis of feature tables.
     *
     * @param  metadata    metadata to use for verifying which tables are present.
     * @param  tableTypes  the "TABLE" and "VIEW" keywords for table types, with unsupported keywords omitted.
     * @return names of the standard tables defined by the spatial schema.
     */
    final Set<String> detectSpatialSchema(final DatabaseMetaData metadata, final String[] tableTypes) throws SQLException {
        /*
         * The following tables are defined by ISO 19125 / OGC Simple feature access part 2.
         * Note that the standard specified those names in upper-case letters, which is also
         * the default case specified by the SQL standard. However, some databases use lower
         * cases instead.
         */
        String crsTable = null;
        final var ignoredTables = new HashMap<String,Boolean>(8);
        for (SpatialSchema convention : getPossibleSpatialSchemas(ignoredTables)) {
            String geomTable;
            crsTable  = convention.crsTable;
            geomTable = convention.geometryColumns;
            if (metadata.storesLowerCaseIdentifiers()) {
                crsTable  = crsTable .toLowerCase(Locale.US).intern();
                geomTable = geomTable.toLowerCase(Locale.US).intern();
            } else if (metadata.storesUpperCaseIdentifiers()) {
                crsTable  = crsTable .toUpperCase(Locale.US).intern();
                geomTable = geomTable.toUpperCase(Locale.US).intern();
            }
            ignoredTables.put(crsTable,  Boolean.TRUE);
            ignoredTables.put(geomTable, Boolean.TRUE);
            /*
             * Check if the database contains at least one "ignored" tables associated to `Boolean.TRUE`.
             * If many tables are found, ensure that the catalog and schema names are the same. If this
             * is not the case, no (catalog,schema) will be used and the search for spatial tables will
             * rely on the database "search path".
             */
            boolean found = false;
            boolean consistent = true;
            String catalog = null, schema = null;
            for (final Map.Entry<String,Boolean> entry : ignoredTables.entrySet()) {
                if (entry.getValue()) {
                    String table = SQLUtilities.escape(entry.getKey(), escape);
                    try (ResultSet reflect = metadata.getTables(null, null, table, tableTypes)) {
                        while (reflect.next()) {
                            consistent &= consistent(catalog, catalog = reflect.getString(Reflection.TABLE_CAT));
                            consistent &= consistent(schema,  schema  = reflect.getString(Reflection.TABLE_SCHEM));
                            found = true;
                        }
                    }
                }
            }
            if (found) {
                spatialSchema = convention;
                if (consistent) {
                    catalogOfSpatialTables = catalog;
                    schemaOfSpatialTables  = schema;
                }
                break;
            }
            ignoredTables.remove(crsTable);
            ignoredTables.remove(geomTable);
        }
        /*
         * Get the columns for CRS definitions. At least one column should exist for CRS encoded in WKT1 format.
         * Some schemas have additional columns for optional encodings, for example a separated column for WKT2.
         * The preference order will be defined by the `CRSEncoding` enumeration order.
         */
        if (spatialSchema != null) {
            final String schema = SQLUtilities.escape(schemaOfSpatialTables, escape);
            final String table  = SQLUtilities.escape(crsTable, escape);
            for (Map.Entry<CRSEncoding, String> entry : spatialSchema.crsDefinitionColumn.entrySet()) {
                String column = entry.getValue();
                if (metadata.storesLowerCaseIdentifiers()) {
                    column = column.toLowerCase(Locale.US);
                }
                column = SQLUtilities.escape(column, escape);
                try (ResultSet reflect = metadata.getColumns(catalogOfSpatialTables, schema, table, column)) {
                    if (reflect.next()) {
                        crsEncodings.add(entry.getKey());
                    }
                }
            }
        }
        return ignoredTables.keySet();
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
     * @param  store               the data store for which we are creating a model. Used only in case of error.
     * @param  analyzer            the opaque temporary object used for analyzing the database schema.
     * @param  tableNames          qualified name of the tables. Specified by users at construction time.
     * @param  queries             additional resources associated to SQL queries. Specified by users at construction time.
     * @param  customizer          user-specified modification to the features, or {@code null} if none.
     * @param  spatialInformation  statements for fetching SRID, geometry types, <i>etc.</i>
     * @throws SQLException if a database error occurred while reading metadata.
     * @throws DataStoreException if a logical error occurred while analyzing the database structure.
     */
    public final void analyze(final SQLStore store, final Analyzer analyzer, final GenericName[] tableNames,
                              final ResourceDefinition[] queries, final SchemaModifier customizer,
                              final InfoStatements spatialInformation) throws Exception
    {
        if (spatialSchema != null) {
            analyzer.spatialInformation = spatialInformation;
        }
        analyzer.customizer = customizer;
        final List<Table> tableList = analyzer.findFeatureTables(tableNames, queries);
        /*
         * At this point we finished to create the tables explicitly requested by the user.
         * Register all tables only at this point, because other tables (dependencies) may
         * have been analyzed as a side-effect of above method call.
         */
        for (final Table table : analyzer.finish()) {
            tablesByNames.add(store, table.featureType.getName(), table);
            hasGeometry |= table.hasGeometry;
            hasRaster   |= table.hasRaster;
        }
        tables = tableList.toArray(Table[]::new);
    }

    /**
     * Helper method for checking if catalog or schema names are consistent.
     * If an previous (old) name existed, the new name should be the same.
     */
    private static boolean consistent(final String oldName, final String newName) {
        return (oldName == null) || oldName.equals(newName);
    }

    /**
     * Stores information about tables in the given metadata.
     * Only tables explicitly requested by the user are listed.
     *
     * @param  metadata  information about the database.
     * @param  builder   where to add information about the tables.
     * @throws SQLException if an error occurred while fetching table information.
     */
    public final void metadata(final DatabaseMetaData metadata, final MetadataBuilder builder) throws SQLException {
        if (hasGeometry) {
            builder.addSpatialRepresentation(SpatialRepresentationType.VECTOR);
        }
        if (hasRaster) {
            builder.addSpatialRepresentation(SpatialRepresentationType.GRID);
        }
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
     * Appends a table or a call to a function defined in the spatial schema.
     * The name will be prefixed by catalog and schema name if applicable.
     * The name will not be quoted.
     *
     * @param  sql   the SQL builder where to add the spatial table or function.
     * @param  name  name of the table or function to append.
     */
    public final void formatTableName(final SQLBuilder sql, final String name) {
        final String schema = schemaOfSpatialTables;
        if (schema != null && !schema.isEmpty()) {
            final String catalog = catalogOfSpatialTables;
            if (catalog != null && !catalog.isEmpty()) {
                sql.appendIdentifier(catalog).append('.');
            }
            sql.appendIdentifier(schema).append('.');
        }
        sql.append(name);
    }

    /**
     * Returns an identification of the table and column naming conventions.
     * This is absent if the database is not spatial.
     *
     * @return an identification of the table and column naming conventions.
     */
    public final Optional<SpatialSchema> getSpatialSchema() {
        return Optional.ofNullable(spatialSchema);
    }

    /**
     * If a <abbr>CRS</abbr> is available in the cache for the given SRID, returns that <abbr>CRS</abbr>.
     * Otherwise returns {@code null}. This method does not query the database.
     *
     * @param  srid  identifier of the <abbr>CRS</abbr> to get.
     * @return the requested <abbr>CRS</abbr>, or {@code null} if not in the cache.
     */
    public final CoordinateReferenceSystem getCachedCRS(final int srid) {
        return cacheOfCRS.get(srid);
    }

    /**
     * If a <abbr>SRID</abbr> is available in the cache for the given <abbr>CRS</abbr>, returns that SRID.
     * Otherwise returns {@code null}. This method does not query the database.
     *
     * @param  crs  the <abbr>CRS</abbr> for which to get the <abbr>SRID</abbr>.
     * @return the <abbr>SRID</abbr> for the given <abbr>CRS</abbr>, or {@code null} if not in the cache.
     */
    public final int getCachedSRID(final CoordinateReferenceSystem crs) {
        synchronized (cacheOfSRID) {
            return cacheOfSRID.get(crs);
        }
    }

    /**
     * Returns a function for getting values from a geometry or geography column.
     * This is a helper method for {@link #getMapping(Column)} implementations.
     *
     * @param  columnDefinition  information about the column to extract values from and expose through Java API.
     * @return converter to the corresponding java type, or {@code null} if this class cannot find a mapping,
     */
    protected final ValueGetter<?> forGeometry(final Column columnDefinition) {
        /*
         * The geometry type should not be empty. But it may still happen if the "GEOMETRY_COLUMNS"
         * table does not contain a line for the specified column. It is a server issue, but seems
         * to happen sometimes.
         */
        final GeometryType type = columnDefinition.getGeometryType().orElse(GeometryType.GEOMETRY);
        final Class<? extends G> geometryClass = geomLibrary.getGeometryClass(type).asSubclass(geomLibrary.rootClass);
        return new GeometryGetter<>(geomLibrary, geometryClass, columnDefinition.getDefaultCRS().orElse(null),
                                    getBinaryEncoding(columnDefinition));
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
     * @return converter to the corresponding java type, or {@code null} if this class cannot find a mapping.
     */
    @SuppressWarnings("fallthrough")
    protected ValueGetter<?> getMapping(final Column columnDefinition) {
        if (GeometryType.isKnown(columnDefinition.typeName)) {
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
            case Types.DATE:                      return supportsJavaTime ? ValueGetter.LOCAL_DATE       : ValueGetter.AsLocalDate.INSTANCE;
            case Types.TIME:                      return supportsJavaTime ? ValueGetter.LOCAL_TIME       : ValueGetter.AsLocalTime.INSTANCE;
            case Types.TIMESTAMP:                 return supportsJavaTime ? ValueGetter.LOCAL_DATE_TIME  : ValueGetter.AsLocalDateTime.INSTANCE;
            case Types.TIME_WITH_TIMEZONE:        return supportsJavaTime ? ValueGetter.OFFSET_TIME      : ValueGetter.AsOffsetTime.INSTANCE;
            case Types.TIMESTAMP_WITH_TIMEZONE:   return supportsJavaTime ? ValueGetter.OFFSET_DATE_TIME : ValueGetter.AsOffsetDateTime.INSTANCE;
            case Types.BLOB:                      return ValueGetter.AsBytes.INSTANCE;
            case Types.OTHER:
            case Types.JAVA_OBJECT:               return getDefaultMapping();
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY: {
                final BinaryEncoding encoding = getBinaryEncoding(columnDefinition);
                switch (encoding) {
                    case RAW:         return ValueGetter.AsBytes.INSTANCE;
                    case HEXADECIMAL: return ValueGetter.AsBytes.HEXADECIMAL;
                    default: throw new AssertionError(encoding);
                }
            }
            case Types.ARRAY: {
                final int componentType = getArrayComponentType(columnDefinition);
                final ValueGetter<?> component = getMapping(new Column(componentType, columnDefinition.typeName));
                if (component == ValueGetter.AsObject.INSTANCE) {
                    return ValueGetter.AsArray.INSTANCE;
                }
                return new ValueGetter.AsArray(component);
            }
            default: return null;
        }
    }

    /**
     * Returns a mapping for {@link Types#JAVA_OBJECT} or unrecognized types. Some JDBC drivers wrap
     * objects in implementation-specific classes, for example {@link org.postgresql.util.PGobject}.
     * This method should be overwritten in database-specific subclasses for returning a value getter
     * capable to unwrap the value.
     *
     * @return the default mapping for unknown or unrecognized types.
     */
    protected ValueGetter<Object> getDefaultMapping() {
        return ValueGetter.AsObject.INSTANCE;
    }

    /**
     * Returns the type of components in SQL arrays stored in a column.
     * This method is invoked when {@link Column#type} = {@link Types#ARRAY}.
     * The default implementation returns {@link Types#OTHER} because JDBC
     * column metadata does not provide information about component types.
     * Database-specific subclasses should override this method if they can
     * provide that information from the {@link Column#typeName} value.
     *
     * @param  columnDefinition  information about the column to extract array component type.
     * @return one of {@link Types} constants.
     *
     * @see Array#getBaseType()
     */
    protected int getArrayComponentType(final Column columnDefinition) {
        return Types.OTHER;
    }

    /**
     * Returns an identifier of the way binary data are encoded by the JDBC driver.
     *
     * @param  columnDefinition  information about the column to extract binary values from.
     * @return how the binary data are returned by the JDBC driver.
     */
    protected BinaryEncoding getBinaryEncoding(final Column columnDefinition) {
        return BinaryEncoding.RAW;
    }

    /**
     * Computes an estimation of the envelope of all geometry columns in the given table.
     * The returned envelope shall contain at least the two-dimensional spatial components.
     * Whether other dimensions (vertical and temporal) and present or not depends on the implementation.
     * This method is invoked only if the {@code columns} array contains at least one geometry column.
     *
     * @param  table    the table for which to compute an estimation of the envelope.
     * @param  columns  all columns in the table. Implementation should ignore non-geometry columns.
     *                  This is a reference to an internal array; <strong>do not modify</strong>.
     * @param  recall   if it is at least the second time that this method is invoked for the specified table.
     * @return an estimation of the spatiotemporal resource extent, or {@code null} if none.
     * @throws SQLException if an error occurred while fetching the envelope.
     */
    protected Envelope getEstimatedExtent(TableReference table, Column[] columns, boolean recall) throws SQLException {
        return null;
    }

    /**
     * Returns the spatial schema conventions that may possibly be supported by this database.
     * The default implementation returns all {@link SpatialSchema} enumeration values.
     * Subclasses may restrict to a smaller set of possibilities.
     *
     * <p>In addition, this method can declare in the supplied map which tables are used for describing
     * the spatial schema. The default implementation does nothing because the entries to add depend on
     * the {@link SpatialSchema}. For example, if Simple Features conventions are used, then the tables
     * are {@code "SPATIAL_REF_SYS"} and {@code "GEOMETRY_COLUMNS"}. Subclasses can add other entries
     * if they know in advance that they support only one convention, or that all the conventions that
     * they support use the same table names. The table added to the map will be ignored when searching
     * for feature tables.</p>
     *
     * <p>The values in the map tells whether the table can be used as a sentinel value for determining
     * that the {@link SpatialSchema} enumeration value can be accepted.</p>
     *
     * @param  tables  where to add names of tables that describe the spatial schema.
     * @return the spatial schema conventions that may be supported by this database.
     *
     * @see #getSpatialSchema()
     */
    protected SpatialSchema[] getPossibleSpatialSchemas(Map<String,Boolean> tables) {
        return SpatialSchema.values();
    }

    /**
     * Returns the converter from filters/expressions to the {@code WHERE} part of SQL statement.
     * Subclasses should override this method if their database supports an extended syntax for
     * some filters or expressions.
     *
     * <p>The returned instance is usually a singleton instance.
     * The caller of this method may create a copy of the returned instance for removing
     * some functions that are found to be unsupported by the database software.
     * Consequently, implementation of this method can assume that the database supports
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
     * Prepares a cache of statements about spatial information using the given connection.
     * Each statement in the returned object will be created only when first needed.
     *
     * @param  connection  the connection to use for creating statements.
     * @return a cache of prepared statements about spatial information.
     */
    public InfoStatements createInfoStatements(final Connection connection) {
        return new InfoStatements(this, connection);
    }

    /**
     * Sets the logger, class and method names of the given record, then logs it.
     * This method declares {@link SQLStore#components()} as the public source of the log.
     *
     * @param  record  the record to configure and log.
     */
    protected final void log(final LogRecord record) {
        record.setSourceClassName(SQLStore.class.getName());
        record.setSourceMethodName("components");                // Main public API triggering the database analysis.
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
        if (tables != null) {
            for (final Table child : tables) {
                child.appendTo(parent);
            }
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
