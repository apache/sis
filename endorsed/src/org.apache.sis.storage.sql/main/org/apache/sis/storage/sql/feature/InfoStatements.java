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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.concurrent.Callable;
import java.text.ParseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.privy.DefinitionVerifier;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.metadata.sql.privy.SQLUtilities;
import org.apache.sis.metadata.sql.privy.SQLBuilder;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.system.CommonExecutor;
import org.apache.sis.system.Modules;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.util.Workaround;

// Specific to the main branch:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * A set of prepared statements to create when first needed and to reuse as long as the connection is in scope.
 * The prepared statement tasks include:
 *
 * <ul>
 *   <li>Searching for geometric information using SQL queries specialized for Simple Feature table.</li>
 *   <li>Fetching a Coordinate Reference System (CRS) from a SRID.</li>
 *   <li>Finding a SRID from a Coordinate Reference System (CRS).</li>
 * </ul>
 *
 * Some statements are used only during the {@linkplain Analyzer analysis} of the database schema.
 * This is the case of the search for geometry information. Other statements are used every times
 * that a query is executed. This is the case of the statements for fetching the CRS.
 *
 * <h2>Thread safety</h2>
 * This class is <strong>not</strong> thread-safe. Each instance should be used in a single thread.
 * Instances are created by {@link Database#createInfoStatements(Connection)}.
 *
 * @author Alexis Manin (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://www.ogc.org/standards/sfs">OGC Simple feature access — Part 2: SQL option</a>
 */
public class InfoStatements implements Localized, AutoCloseable {
    /**
     * Upper limit of the range of low SRID codes that we may use for custom CRS.
     * This is used only if the EPSG code cannot be used.
     */
    private static final int LOW_SRID_RANGE = 1000;

    /**
     * Upper limit of the high range of SRID codes that we can use for custom CRS.
     * This is used only if the EPSG code cannot be used.
     */
    private static final int HIGH_SRID_RANGE = 40000;

    /**
     * The database that created this set of cached statements. This object includes the
     * cache of CRS created from SRID codes and the listeners where to send warnings.
     * A {@code Database} object does <strong>not</strong> contain live JDBC {@link Connection}.
     */
    protected final Database<?> database;

    /**
     * Connection to use for creating the prepared statements.
     * This connection will <strong>not</strong> be closed by this class.
     */
    private final Connection connection;

    /**
     * A statement for fetching geometric information for a specific column.
     * May be {@code null} if not yet prepared or if the table does not exist.
     * This field is valid if {@link #isAnalysisPrepared} is {@code true}.
     *
     * @see #isAnalysisPrepared
     * @see #completeIntrospection(Analyzer, TableReference, Map)
     */
    protected PreparedStatement geometryColumns;

    /**
     * Whether the statements for schema analysis have been prepared.
     * This flag tells whether the statements for the following information are valid even if {@code null}:
     *
     * <ul>
     *   <li>{@linkplain #geometryColumns Geometry columns}</li>
     *   <li>Geography columns (specific to PostGIS)</li>
     *   <li>Raster columns (specific to PostGIS)</li>
     * </ul>
     *
     * This flag does not apply to the statements working on the {@code SPATIAL_REF_SYS} table,
     * which is assumed to always exist.
     */
    protected boolean isAnalysisPrepared;

    /**
     * The statement for fetching a SRID from a geometry column table when the column's table is unknown.
     * This is a workaround for <abbr>JDBC</abbr> drivers that do not provide this information.
     * It is created only if requested.
     *
     * @see #guessCRS(String)
     */
    @Workaround(library = "DuckDB", version = "1.2.2.0")
    private PreparedStatement sridForUnknownTable;

    /**
     * The statement for fetching a SRID from a CRS and its set of authority codes.
     * Created when first needed.
     *
     * @see #findOrAddCRS(CoordinateReferenceSystem)
     */
    private PreparedStatement sridFromCRS;

    /**
     * The statement for fetching CRS Well-Known Text (<abbr>WKT</abbr>) from a <abbr>SRID</abbr> code.
     * Created when first needed.
     *
     * @see #parseCRS(int)
     * @see <a href="http://postgis.refractions.net/documentation/manual-1.3/ch04.html#id2571265">PostGIS documentation</a>
     */
    private PreparedStatement wktFromSrid;

    /**
     * The object to use for parsing or formatting Well-Known Text (<abbr>WKT</abbr>).
     * Created when first needed.
     *
     * @see #wktFormat()
     */
    private WKTFormat wktFormat;

    /**
     * Whether an error occurred while reading the geometry type.
     * In such case, the type default to {@link GeometryType#GEOMETRY}.
     * This flag is used for reporting the warning only once.
     */
    private boolean cannotReadGeometryType;

    /**
     * Creates an initially empty {@code CachedStatements} which will use
     * the given connection for creating {@link PreparedStatement}s.
     *
     * @param  database    information about the spatial database.
     * @param  connection  connection to use for creating the prepared statements.
     */
    protected InfoStatements(final Database<?> database, final Connection connection) {
        this.database   = database;
        this.connection = connection;
    }

    /**
     * Returns the locale used for warnings and error messages.
     * Not to be confused with the locale used for writing international texts in the database.
     */
    @Override
    public final Locale getLocale() {
        return database.listeners.getLocale();
    }

    /**
     * Appends a {@code " FROM <table> WHERE "} text to the given builder.
     * The table name will be prefixed by catalog and schema name if applicable.
     */
    private void appendFrom(final SQLBuilder sql, final String table) {
        database.formatTableName(sql.append(" FROM "), table);
        sql.append(" WHERE ");
    }

    /**
     * Appends the name of a geometry column or raster column.
     *
     * @param  sql     the builder where to add the column name.
     * @param  raster  whether the statement is for raster table instead of geometry table.
     * @param  column  the column name (i.e., {@code "F_TABLE_NAME"}.
     * @return the given SQL builder.
     */
    private static SQLBuilder appendColumn(final SQLBuilder sql, final boolean raster, final String column) {
        if (raster && column.startsWith("F_")) {
            return sql.append('R').append(column, 1, column.length());
        } else {
            return sql.append(column);
        }
    }

    /**
     * Prepares the statement for fetching information about all geometry or raster columns in a specified table.
     * This method is for {@link #completeIntrospection(Analyzer, TableReference, Map)} implementations.
     *
     * <h4>PostGIS special case</h4>
     * By default, the {@code geomColNameColumn} and {@code geomTypeColumn} argument values are fetched from the
     * {@link SpatialSchema}. However, PostGIS uses a non-standard {@code geomTypeColumn} value. It also has many
     * "geometry columns"-like tables. This is handled by overriding {@code completeIntrospection(…)}.
     *
     * @param  analyzer           the opaque temporary object used for analyzing the database schema.
     * @param  table              name of the geometry table. Standard value is {@code "GEOMETRY_COLUMNS"}.
     * @param  raster             whether the statement is for raster table instead of geometry table.
     * @param  geomColNameColumn  column of geometry column name, or {@code null} for the standard value.
     * @param  geomTypeColumn     column of geometry type, or {@code null} for the standard value, or "" for none.
     * @return the prepared statement for querying the geometry table, or {@code null} if the table does not exist.
     * @throws SQLException if the statement cannot be created.
     */
    protected final PreparedStatement prepareIntrospectionStatement(
            final Analyzer analyzer, final String table, final boolean raster,
            String geomColNameColumn, String geomTypeColumn) throws SQLException
    {
        if (analyzer.skipInfoTable(table)) {
            return null;
        }
        final SpatialSchema schema = database.getSpatialSchema().orElseThrow();
        final var sql = new SQLBuilder(database).append(SQLBuilder.SELECT);
        if (geomColNameColumn == null) {
            geomColNameColumn = schema.geomColNameColumn;
        }
        appendColumn(sql, raster, geomColNameColumn).append(", ").append(schema.crsIdentifierColumn);
        if (geomTypeColumn == null) {
            geomTypeColumn = schema.geomTypeColumn;
        }
        if (geomTypeColumn != null && !geomTypeColumn.isEmpty()) {
            sql.append(", ").append(geomTypeColumn);
        }
        appendFrom(sql, table);
        /*
         * In principle, all tables should be unambiguously specified with their catalog and schema name.
         * However, some JDBC drivers do not provide this information in some circumstances such as materialized views.
         * Therefore, we use the `LIKE` operator instead of `=` for making possible to disable the filtering by schema.
         */
        if (database.supportsCatalogs) appendColumn(sql, raster, schema.geomCatalogColumn).append(" LIKE ? AND ");
        if (database.supportsSchemas)  appendColumn(sql, raster, schema.geomSchemaColumn) .append(" LIKE ? AND ");
        appendColumn(sql, raster, schema.geomTableColumn).append("=?");
        return connection.prepareStatement(sql.toString());
    }

    /**
     * Sets the parameter value for a table catalog or schema. Those parameters use the {@code LIKE} statement
     * in order to ignore the catalog or schema when it is not specified. A catalog or schema is not specified
     * if the string is null or empty. The latter case may happen with some drivers with, for example,
     * materialized views.
     *
     * @param  columnQuery  the query where to set the parameter.
     * @param  p            index of the parameter to set.
     * @param  source       catalog or schema name to set, or null or empty if unknown.
     * @throws SQLException if an error occurred while setting the parameter value.
     */
    private void setCatalogOrSchema(final PreparedStatement columnQuery, final int p, String source) throws SQLException {
        if (source == null || source.isEmpty()) {
            source = "%";
        } else {
            source = database.escapeWildcards(source);
        }
        columnQuery.setString(p, source);
    }

    /**
     * Gets all geometry and raster columns for the given table and sets information on the corresponding columns.
     * Column instances in the {@code columns} map are modified in-place (the map content is not directly modified).
     * This method should be invoked at least once before the {@link Column#valueGetter} field is set.
     * It is invoked again for each table or query to analyze.
     *
     * <p>This method may be invoked with a null {@code source} and empty {@code columns}
     * for ensuring that {@link #geometryColumns} is initialized but without executing it.</p>
     *
     * @param  analyzer  the opaque temporary object used for analyzing the database schema.
     * @param  source    the table for which to get all geometry columns. May be null if {@code columns} is empty.
     * @param  columns   all columns for the specified table. Keys are column names.
     * @throws DataStoreContentException if a logical error occurred in processing data.
     * @throws ParseException if the WKT cannot be parsed.
     * @throws SQLException if a SQL error occurred.
     */
    public void completeIntrospection(final Analyzer analyzer, final TableReference source, final Map<String,Column> columns)
            throws Exception
    {
        final SpatialSchema schema = database.getSpatialSchema().orElseThrow();
        if (!isAnalysisPrepared) {
            isAnalysisPrepared = true;
            geometryColumns = prepareIntrospectionStatement(analyzer, schema.geometryColumns, false, null, null);
            // The `geometryColumns` field may still be null.
        }
        configureSpatialColumns(geometryColumns, source, columns, schema.typeEncoding);
    }

    /**
     * Sets information about the specified columns of the given table using the given query.
     * This method is the implementation of {@link #completeIntrospection(Analyzer, TableReference, Map)},
     * provided as a separated methods for allowing sub-classes to override the above-cited public method.
     * This method is used for both geometric and non-geometric columns such as rasters, in which case the
     * {@code typeValueKind} argument shall be {@code null}. The given {@code columnQuery} argument is the
     * value returned by {@link #prepareIntrospectionStatement(Analyzer, String, boolean, String, String)}.
     *
     * @param  columnQuery    the statement for fetching information, or {@code null} if none.
     * @param  source         the table for which to get all geometry columns. May be null if {@code columns} is empty.
     * @param  columns        all columns for the specified table. Keys are column names.
     * @param  typeValueKind  {@code NUMERIC}, {@code TEXTUAL} or {@code null} if none.
     * @throws DataStoreContentException if a logical error occurred in processing data.
     * @throws ParseException if the WKT cannot be parsed.
     * @throws SQLException if a SQL error occurred.
     *
     * @todo Follow column dependencies for columns from a view.
     *       Problem: for views, PostGIS will not provide neither SRID nor geometry type,
     *       unless user has statically defined its column to match a specific geometry type/SRID.
     *       Source: https://gis.stackexchange.com/a/376947/182809
     */
    protected final void configureSpatialColumns(final PreparedStatement columnQuery, final TableReference source,
            final Map<String,Column> columns, final GeometryTypeEncoding typeValueKind) throws Exception
    {
        if (columnQuery == null || columns.isEmpty()) {
            return;
        }
        int p = 0;
        if (database.supportsCatalogs) setCatalogOrSchema(columnQuery, ++p, source.catalog);
        if (database.supportsSchemas)  setCatalogOrSchema(columnQuery, ++p, source.schema);
        columnQuery.setString(++p, source.table);
        try (ResultSet result = columnQuery.executeQuery()) {
            while (result.next()) {
                final Column target = columns.get(result.getString(1));
                if (target != null) {
                    GeometryType type = null;
                    if (typeValueKind != null) {
                        try {
                            type = typeValueKind.parse(result, 3);
                        } catch (IllegalArgumentException e) {
                            if (!cannotReadGeometryType) {
                                cannotReadGeometryType = true;
                                database.warning(Resources.Keys.CanNotAnalyzeFully, e);
                            }
                        }
                        if (type == null) {
                            type = GeometryType.GEOMETRY;
                        }
                    }
                    target.makeSpatial(database, type, fetchCRS(result.getInt(2)));
                }
            }
        }
    }

    /**
     * Tries to guess the <abbr>CRS</abbr> for the specified column in an unknown table.
     * This is invoked (indirectly) by {@link QueryAnalyzer} when the <abbr>JDBC</abbr>
     * driver is an incomplete implementation.
     *
     * <h4>Algorithm</h4>
     * This method lists the <abbr>CRS</abbr> of all columns of the given name,
     * regardless the table, schema or catalog containing a column of that name.
     * If the <abbr>CRS</abbr> is equivalent in all cases, then it is returned.
     *
     * @param  column  name of the column in unknown table.
     * @return the <abbr>CRS</abbr>, or {@code null} if none or ambiguous.
     * @throws Exception if an error occurred while fetching the <abbr>CRS</abbr>.
     */
    @Workaround(library = "DuckDB", version = "1.2.2.0")
    final CoordinateReferenceSystem guessCRS(final String column) throws Exception {
        if (sridForUnknownTable == null) {
            if (geometryColumns == null) {
                return null;
            }
            final SpatialSchema schema = database.getSpatialSchema().orElseThrow();
            final var sql = new SQLBuilder(database)
                    .append(SQLBuilder.SELECT).append("DISTINCT ")
                    .appendIdentifier(schema.crsIdentifierColumn)
                    .append(" FROM ").appendIdentifier(schema.geometryColumns)
                    .append(" WHERE ").appendIdentifier(schema.geomColNameColumn).append("=?");
            sridForUnknownTable = connection.prepareStatement(sql.toString());
        }
        sridForUnknownTable.setString(1, column);
        CoordinateReferenceSystem first = null;
        try (ResultSet result = sridForUnknownTable.executeQuery()) {
            while (result.next()) {
                final int srid = result.getInt(1);
                if (!result.wasNull()) {
                    CoordinateReferenceSystem crs = fetchCRS(srid);
                    if (crs != null) {
                        if (first == null) {
                            first = crs;
                        } else if (!Utilities.equalsIgnoreMetadata(first, crs)) {
                            return null;
                        }
                    }
                }
            }
        }
        return first;
    }

    /**
     * Gets a Coordinate Reference System for to given SRID.
     * If the given SRID is zero or negative, then this method returns {@code null}.
     * Otherwise the CRS is decoded from the database {@code "SPATIAL_REF_SYS"} table
     * or equivalent (depending on the {@link SpatialSchema}).
     *
     * @param  srid  the Spatial Reference Identifier (SRID) to resolve as a CRS object.
     * @return the CRS associated to the given SRID, or {@code null} if the SRID is zero.
     * @throws DataStoreContentException if the CRS cannot be fetched. Possible reasons are:
     *         no entry found in the {@code "SPATIAL_REF_SYS"} table, or more than one entry is found,
     *         or a single entry exists but has no WKT definition and its authority code is unsupported by SIS.
     * @throws ParseException if the WKT cannot be parsed.
     * @throws SQLException if a SQL error occurred.
     */
    public final CoordinateReferenceSystem fetchCRS(final int srid) throws Exception {
        /*
         * In PostGIS 1, srid value -1 was used for "unknown CRS".
         * Since PostGIS 2, srid value for unknown CRS became 0.
         */
        if (srid <= 0) return null;
        return database.cacheOfCRS.getOrCreate(srid, () -> parseCRS(srid));
    }

    /**
     * Creates a prepared statement for getting an authority code from SRID, or the converse.
     * In both cases, the definition in all supported CRS encoding is provided in the last columns.
     *
     * @param  byAuthorityCode  {@code false} for (authority, code) from SRID, or {@code true} for the converse.
     * @return the prepared statement.
     * @throws SQLException if an error occurred while creating the prepared statement.
     */
    private PreparedStatement prepareSearchCRS(final boolean byAuthorityCode) throws SQLException {
        final SpatialSchema schema = database.getSpatialSchema().orElseThrow();
        final String search, get;
        if (byAuthorityCode) {
            search = schema.crsAuthorityCodeColumn;
            get    = schema.crsIdentifierColumn;
        } else {
            search = schema.crsIdentifierColumn;
            get    = schema.crsAuthorityCodeColumn;
        }
        final var sql = new SQLBuilder(database).append(SQLBuilder.SELECT)
                .append(schema.crsAuthorityNameColumn).append(", ").append(get);

        for (CRSEncoding encoding : database.crsEncodings) {
            sql.append(", ").append(schema.crsDefinitionColumn.get(encoding));
        }
        appendFrom(sql, schema.crsTable);
        sql.append(search).append("=?");
        if (byAuthorityCode) {
            sql.append(" AND LOWER(").append(schema.crsAuthorityNameColumn).append(") LIKE ?");
        }
        return connection.prepareStatement(sql.toString());
    }

    /**
     * Invoked when the requested CRS is not in the cache. This method gets the entry from the
     * {@link SpatialSchema#refSysTable} then gets the CRS from its authority code if possible,
     * or fallback on the WKT otherwise.
     *
     * <p>This method does not cache the returned <abbr>CRS</abbr>.
     * The caching is done by {@code Cache.getOrCreate(…)}.</p>
     *
     * @param  srid  the Spatial Reference Identifier (SRID) of the CRS to create from the database content.
     * @return the CRS created from database content.
     * @throws Exception if an SQL error, parsing error or other error occurred.
     */
    private CoordinateReferenceSystem parseCRS(final int srid) throws Exception {
        if (wktFromSrid == null) {
            wktFromSrid = prepareSearchCRS(false);
        }
        wktFromSrid.setInt(1, srid);
        CoordinateReferenceSystem crs = null;
        NoSuchAuthorityCodeException authorityError = null;
        LogRecord warning = null;
        try (ResultSet result = wktFromSrid.executeQuery()) {
            while (result.next()) {
                /*
                 * If the authority code is recognized, use that code instead of WKT definition
                 * because the EPSG database (for example) contains more information than WKT.
                 */
                CoordinateReferenceSystem fromAuthority = null;
                final String authority = result.getString(1);
                if (authority != null && !authority.isEmpty()) {
                    final int code = result.getInt(2);
                    if (!result.wasNull()) try {
                        final CRSAuthorityFactory factory = CRS.getAuthorityFactory(authority);
                        fromAuthority = factory.createCoordinateReferenceSystem(Integer.toString(code));
                    } catch (NoSuchAuthorityCodeException e) {      // Include NoSuchAuthorityFactoryException.
                        authorityError = e;
                    }
                }
                /*
                 * Parse the WKT unconditionally, even if we already got the CRS from authority code.
                 * In the latter case, the CRS from WKT will be used only for a consistency check and
                 * the main CRS will be the one from authority.
                 */
                final CoordinateReferenceSystem fromDefinition;
                try {
                    final Object parsed = parseDefinition(result, 3);
                    if (parsed == null || parsed instanceof CoordinateReferenceSystem) {
                        fromDefinition = (CoordinateReferenceSystem) parsed;
                    } else {
                        throw invalidSRID(Resources.Keys.UnexpectedTypeForSRID_2,
                                ReferencingUtilities.getInterface(parsed), srid, authorityError);
                    }
                } catch (ParseException e) {
                    if (authorityError != null) {
                        e.addSuppressed(authorityError);
                    }
                    throw e;
                }
                /*
                 * If one of the CRS is null, take the non-null one. If both CRSs are defined (which is the usual case),
                 * verify that they are consistent. Inconsistency will be logged as warning if the rest of the operation
                 * succeed.
                 */
                final DefinitionVerifier v = DefinitionVerifier.compare(fromDefinition, fromAuthority, getLocale());
                if (v.recommendation != null) {
                    if (crs == null) {
                        crs = v.recommendation;
                    } else if (!crs.equals(v.recommendation)) {
                        final SpatialSchema schema = database.getSpatialSchema().orElseThrow();
                        throw invalidSRID(Resources.Keys.DuplicatedSRID_2, schema.crsTable, srid, authorityError);
                    }
                    warning = v.warning(false);
                    if (warning == null && fromDefinition != null) {
                        /*
                         * Following warnings may have occurred during WKT parsing and are considered minor.
                         * They will be reported only if there are no more important warnings to report.
                         */
                        final Warnings w = wktFormat.getWarnings();
                        if (w != null) {
                            warning = new LogRecord(Level.WARNING, w.toString(getLocale()));
                        }
                    }
                }
            }
        }
        /*
         * Finished to parse entries from the "SPATIAL_REF_SYS" table.
         * Reports warning if any, then return the non-null CRS.
         */
        if (crs == null) {
            if (authorityError != null) {
                throw authorityError;
            }
            final SpatialSchema schema = database.getSpatialSchema().orElseThrow();
            throw invalidSRID(Resources.Keys.UnknownSRID_2, schema.crsTable, srid, null);
        }
        log("fetchCRS", warning);
        return crs;
    }

    /**
     * Logs the given warning if it is non-null.
     *
     * @param  method   name of the method logging a warning.
     * @param  warning  the warning to log, or {@code null} if none.
     */
    private void log(final String method, final LogRecord warning) {
        if (warning != null) {
            warning.setLoggerName(Modules.SQL);
            warning.setSourceClassName(getClass().getCanonicalName());
            warning.setSourceMethodName(method);
            database.listeners.warning(warning);
        }
    }

    /**
     * Creates the exception to throw for an invalid SRID. The message is expected to have two arguments,
     * {@code complement} and {@code srid} if that order, where the "complement" can be a table name or a
     * class name depending on the message.
     *
     * @param  message     key of the message to create.
     * @param  complement  first argument in message formatting.
     * @param  srid        second argument in message formatting.
     * @param  suppressed  exception to add as a suppressed exception.
     * @return the exception to throw.
     */
    private DataStoreContentException invalidSRID(final short message, final Object complement, final int srid,
            final NoSuchAuthorityCodeException suppressed)
    {
        final var e = new DataStoreContentException(Resources.forLocale(getLocale()).getString(message, complement, srid));
        if (suppressed != null) {
            e.addSuppressed(suppressed);
        }
        return e;
    }

    /**
     * Finds a SRID code from the spatial reference systems table for the given CRS.
     * If the database does not support concurrent transactions, then the caller is
     * responsible for holding a lock. It may be a read lock or write lock depending
     * on the {@link Connection#isReadOnly()} value.
     *
     * @param  crs  the CRS for which to find a SRID, or {@code null}.
     * @return SRID for the given CRS, or 0 if the given CRS was null.
     * @throws Exception if an SQL error, parsing error or other error occurred.
     */
    public final int findSRID(final CoordinateReferenceSystem crs) throws Exception {
        if (crs == null) {
            return 0;
        }
        final SRID result;
        synchronized (database.cacheOfSRID) {
            final Integer cached = database.cacheOfSRID.get(crs);
            if (cached != null) {
                return cached;
            }
            result = findOrAddCRS(crs);
            database.cacheOfSRID.put(crs, result.srid);
        }
        CommonExecutor.instance().submit((Runnable) result);
        return result.srid;
    }

    /**
     * The result of a search for the <abbr>SRID</abbr> from a <abbr>CRS</abbr>.
     * This result is stored in the {@link #srid} field, but is accompanied by information allowing to
     * opportunistically cache the result in the map for the opposite search (fetching <abbr>CRS</abbr>
     * from a <abbr>SRID</abbr>). A difficulty is that the opposite operation would query the geodetic
     * registry for more metadata. So in order to have more consistent values in the cache, we need to
     * do the same query in {@link #call()} as what would have been done with {@code fetchCRS(srid)}.
     */
    private final class SRID implements Callable<CoordinateReferenceSystem>, Runnable {
        /**
         * The <abbr>CRS</abbr> for which to provide an "authority:code pair".
         */
        final CoordinateReferenceSystem crs;

        /**
         * The authority managing the code value.
         * This is part of the key in hash map.
         */
        final String authority;

        /**
         * Code managed by the authority.
         * This is part of the key in hash map.
         */
        final int code;

        /**
         * Primary key used in the {@code SPATIAL_REF_SYS} table.
         * This is initially zero, then set to a value after the <abbr>SRID</abbr> has been computed.
         */
        int srid;

        /**
         * Creates a new "authority:code" pair for holding the <abbr>SRID</abbr> result.
         */
        SRID(final CoordinateReferenceSystem crs, final String authority, final int code) {
            this.crs       = crs;
            this.authority = authority;
            this.code      = code;
        }

        /**
         * Invoked in a background thread for opportunistically caching the <abbr>SRID</abbr> → <abbr>CRS</abbr>
         * mapping. This is done in a background thread because this is not the information asked by the user,
         * by the information for the reverse operation.
         */
        @Override
        public void run() {
            try {
                database.cacheOfCRS.getOrCreate(srid, this);
            } catch (Exception e) {
                log("findSRID", new LogRecord(Level.FINER, e.toString()));
            }
        }

        /**
         * Simulates a call to {@code fetchCRS(srid)} for cache consistency.
         */
        @Override
        public CoordinateReferenceSystem call() throws FactoryException {
            final CoordinateReferenceSystem fromAuthority;
            try {
                final CRSAuthorityFactory factory = CRS.getAuthorityFactory(authority);
                fromAuthority = factory.createCoordinateReferenceSystem(Integer.toString(code));
            } catch (NoSuchAuthorityCodeException e) {
                return crs;
            }
            for (int i=0; ; i++) {
                final CoordinateReferenceSystem candidate;
                switch (i) {
                    case 0: candidate = fromAuthority; break;
                    case 1: candidate = AbstractCRS.castOrCopy(fromAuthority).forConvention(AxesConvention.RIGHT_HANDED); break;
                    case 2: candidate = AbstractCRS.castOrCopy(fromAuthority).forConvention(AxesConvention.DISPLAY_ORIENTED); break;
                    default: return crs;
                }
                if (Utilities.equalsApproximately(crs, candidate)) {
                    return candidate;
                }
            }
        }

        /**
         * Compares the "authority:code" tuples, intentionally omitting other properties.
         * Only the "authority:code" pair is compared. Needed for use in hash map,
         * in order to avoid processing the same "authority:code" pair many times.
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof SRID) {
                final var other = (SRID) obj;
                return other.code == code && authority.equals(other.authority);
            }
            return false;
        }

        /**
         * Returns a hash code compatible with {@link #equals'Object)} contract.
         */
        @Override
        public int hashCode() {
            return authority.hashCode() + code;
        }

        /**
         * Returns a string representation for debugging purposes.
         */
        @Override
        public String toString() {
            return authority + ':' + code + " → " + srid;
        }
    }

    /**
     * Invoked when a <abbr>CRS</abbr> is not in the cache.
     * This method does not cache the result. Caching should be done by the caller.
     *
     * @param  crs  the <abbr>CRS</abbr> to search.
     * @return <abbr>SRID</abbr> associated to the given <abbr>CRS</abbr>.
     * @throws Exception if an SQL error, parsing error or other error occurred.
     */
    private SRID findOrAddCRS(final CoordinateReferenceSystem crs) throws Exception {
        /*
         * Search in the database. In the `done` map, keys are "authority:code" pairs that we
         * already tried and values tell whether we can use that pair if we need to add new CRS.
         */
        Exception error = null;
        boolean tryWithGivenCRS = true;
        final var sridFounInUse = new HashSet<Integer>();
        final var done = new LinkedHashMap<SRID, Boolean>();    // Value tells whether the SRID may be valid.
        /*
         * The following loop begins with an iterator of only one element (which makes the loop
         * apparently useless), but the iterator will be replaced by another iterator with more
         * elements during the loop.
         */
        for (Iterator<IdentifiedObject> it = Set.<IdentifiedObject>of(crs).iterator(); it.hasNext();) {
            final IdentifiedObject candidate = it.next();
            /*
             * First, iterate over the identifiers declared in the CRS object.
             * If we cannot find an identifier that we can map to a SRID, then this loop may be
             * executed more times with CRS from EPSG database that are equal, ignoring axis order.
             */
            for (final ReferenceIdentifier id : candidate.getIdentifiers()) {
                final String authority = id.getCodeSpace();
                if (authority == null) continue;
                final int code;
                try {
                    code = Integer.parseInt(id.getCode());
                } catch (NumberFormatException e) {
                    if (tryWithGivenCRS) {
                        if (error == null) error = e;
                        else error.addSuppressed(e);
                    }
                    continue;       // Ignore codes that are not integers.
                }
                final SRID search = new SRID(crs, authority, code);
                if (done.putIfAbsent(search, code > 0) != null) {
                    continue;       // Skip "authority:code" that we already tried.
                }
                /*
                 * Found an "authority:code" pair that we did not tested before.
                 * Get the WKT and verifies if the CRS is approximately equal.
                 */
                if (sridFromCRS == null) {
                    sridFromCRS = prepareSearchCRS(true);
                }
                sridFromCRS.setInt(1, code);
                sridFromCRS.setString(2, SQLUtilities.toLikePattern(authority, true));
                try (ResultSet result = sridFromCRS.executeQuery()) {
                    while (result.next()) {
                        if (SQLUtilities.filterFalsePositive(authority, result.getString(1))) {
                            final int srid = result.getInt(2);
                            if (sridFounInUse.add(srid)) try {
                                final Object parsed = parseDefinition(result, 3);
                                if (Utilities.equalsApproximately(parsed, crs)) {
                                    search.srid = srid;
                                    return search;
                                }
                            } catch (ParseException e) {
                                if (error == null) error = e;
                                else error.addSuppressed(e);
                            }
                            done.put(search, Boolean.FALSE);    // Declare this "authority:code" pair as not available.
                        }
                    }
                }
            }
            /*
             * Tried all identifiers associated to the CRS and found no match.
             * It may be because the CRS has no identifier at all. Search for
             * possible identifiers in the EPSG database, then try them.
             */
            if (tryWithGivenCRS) {
                tryWithGivenCRS = false;
                final IdentifiedObjectFinder finder = IdentifiedObjects.newFinder(Constants.EPSG);
                finder.setIgnoringAxes(true);
                it = finder.find(crs).iterator();
            }
        }
        /*
         * At this point, we found no CRS definition in the current `SPATIAL_REF_SYS` table.
         * If the caller allowed the creation of new rows in that table, creates it now.
         * It is caller's responsibility to hold a write lock if needed.
         */
        if (!connection.isReadOnly()) {
            SRID fallback = null;
            for (final Map.Entry<SRID, Boolean> entry : done.entrySet()) {
                if (entry.getValue()) {
                    final SRID search = entry.getKey();
                    if (Constants.EPSG.equalsIgnoreCase(search.authority)) {
                        search.srid = addCRS(search, sridFounInUse);
                        return search;
                    }
                    if (fallback == null) {
                        fallback = search;
                    }
                }
            }
            if (fallback != null) {
                fallback.srid = addCRS(fallback, sridFounInUse);
                return fallback;
            }
            // In the current version, we don't encode a CRS without an "authority:code" pair.
        }
        final Locale locale = getLocale();
        throw new DataStoreReferencingException(Resources.forLocale(locale).getString(
                Resources.Keys.CanNotFindSRID_1, IdentifiedObjects.getDisplayName(crs, locale)), error);
    }

    /**
     * Adds a new entry in the {@code SPATIAL_REF_SYS} table for the given <abbr>CRS</abbr>.
     * The {@code authority} and {@code code} arguments are the values to store in the {@code "AUTH_NAME"} and
     * {@code "AUTH_SRID"} columns respectively. A common usage is to prefer EPSG codes, but this is not mandatory.
     * This method does not cache the added <abbr>CRS</abbr>. Caching should be done by the caller.
     *
     * @param  search         the <abbr>CRS</abbr> to search together with its "authority:code" pair.
     * @param  sridFounInUse  SRIDs which have been found in use, for avoiding SRID that would be certain to fail.
     *                        This is only a hint. A SRID not in this set is not a guarantee that it is available.
     * @return SRID of the CRS added by this method.
     * @throws DataStoreReferencingException if the given CRS cannot be encoded in at least one supported format.
     * @throws SQLException if an error occurred while executing the SQL statement.
     */
    private int addCRS(final SRID search, final Set<Integer> sridFounInUse) throws Exception {
        final SpatialSchema schema = database.getSpatialSchema().orElseThrow();
        final var sql = new SQLBuilder(database).append(SQLBuilder.INSERT);
        database.formatTableName(sql, schema.crsTable);
        sql.append(" (").append(schema.crsIdentifierColumn)
           .append(", ").append(schema.crsAuthorityNameColumn)
           .append(", ").append(schema.crsAuthorityCodeColumn);
        /*
         * Append a variable number of columns for the CRS definitions in various formats.
         * We add columns only for the formats that we can use. At least one of them must
         * be filled, otherwise we cannot add the row.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final var wktFormat    = wktFormat();
        final var crsEncodings = database.crsEncodings;
        final var warnings     = new Warnings[crsEncodings.size()];
        final var definitions  = new String[warnings.length + 2];       // +2 columns for name and description.
        int numDefinitions = 0, numWarnings = 0;
        for (final CRSEncoding encoding : crsEncodings) {
            final String def;
            switch (encoding) {
                default: continue;          // Skip unknown formats (none at this time).
                case WKT1: def = wktFormat.format(search.crs); break;
                case WKT2: {
                    try {
                        wktFormat.setConvention(Convention.WKT2);
                        def = wktFormat.format(search.crs);
                    } finally {
                        wktFormat.setConvention(Convention.WKT1_COMMON_UNITS);
                    }
                    break;
                }
            }
            Warnings warning = wktFormat.getWarnings();
            if (warning != null) {
                warnings[numWarnings++] = warning;
            } else {
                definitions[numDefinitions++] = def;
                sql.append(", ").append(schema.crsDefinitionColumn.get(encoding));
            }
        }
        /*
         * If we have not been able to format the CRS in any encoding, throw an exception with the first warning.
         * We take the first one because the `CRSEncodng` enumeration is ordered with most complete formats first,
         * so if we fail with the first encoding the next ones should be worst.
         */
        if (numDefinitions == 0) {
            throw new DataStoreReferencingException(warnings[0].toString(getLocale()));
        }
        for (int i=0; i<numWarnings; i++) {
            log("addCRS", new LogRecord(Level.WARNING, warnings[i].toString(getLocale())));
        }
        /*
         * Optional columns for CRS name and description.
         * The following loop is executed exactly twice.
         */
        boolean description = false;
        do {
            final String column = description ? schema.crsDescriptionColumn : schema.crsNameColumn;
            if (column != null) {
                String name = description ? IdentifiedObjects.getDisplayName(search.crs, database.contentLocale)
                                          : IdentifiedObjects.getName(search.crs, null);
                if (name != null) {
                    definitions[numDefinitions++] = name;
                    sql.append(", ").append(column);
                }
            }
        } while ((description = !description) == true);
        /*
         * Complete the SQL statement with the parameters that we will need to provide.
         * In addition to the definition, there is 3 columns for (SRID, AUTHORITY, CODE)
         * columns, and one more column if we also store the CRS name.
         */
        String separator = ") VALUES (";
        for (int i = numDefinitions + 3; --i >= 0;) {
            sql.append(separator).append('?');
            separator = ", ";
        }
        int srid = search.code;
        try (PreparedStatement stmt = connection.prepareStatement(sql.append(')').toString())) {
            stmt.setString(2, search.authority);
            stmt.setInt(3, search.code);
            for (int i=0; i<numDefinitions; i++) {
                stmt.setString(4+i, definitions[i]);
            }
            /*
             * Execute the statement first by trying to use a SRID of the same value as the authority code.
             * We do that because a common common practice is to use EPSG code as SRID values. If the SRID
             * is not available (i.e., if we get an integrity violation), we will try another SRID value.
             */
            final var failures = new ArrayList<Exception>();
            if (!sridFounInUse.contains(srid)) try {
                stmt.setInt(1, srid);
                stmt.executeUpdate();
                return srid;
            } catch (SQLException e) {
                filterConstraintViolation(e);
                /*
                 * SQL state category 23: integrity constraint violation. Maybe the new CRS has been added concurrently.
                 * Or maybe the CRS for that SRID would have been suitable but `findSRID(…)` didn't saw it, for example
                 * because the authority name has different spelling or different lower/upper case.
                 */
                try {
                    final CoordinateReferenceSystem candidate = fetchCRS(srid);
                    if (Utilities.equalsIgnoreMetadata(search.crs, candidate)) {
                        return srid;
                    }
                } catch (Exception f) {
                    failures.add(f);
                }
                failures.add(e);
            }
            /*
             * Search for an available SRID, then try again. The loop should be executed only once,
             * unless the database content changed concurrently. In the latter case we retry until
             * we got a free SRID.
             */
            try {
                srid = 0;
                while (srid < (srid = findFreeSRID(schema, sql.clear().append(SQLBuilder.SELECT)))) {
                    try {
                        stmt.setInt(1, srid);
                        stmt.executeUpdate();
                        return srid;
                    } catch (SQLException e) {
                        filterConstraintViolation(e);
                        failures.add(e);
                    }
                }
            } catch (Exception e) {
                failures.add(e);
            }
            /*
             * If an unexpected error occurred, rethrow the last error and add all previous errors
             * as suppressed exceptions, in reverse order. We chose the last exception because, in
             * case of integrity violation, it is the record which was supposed to be okay.
             */
            int i = failures.size();
            final Exception e = failures.get(--i);
            while (--i >= 0) e.addSuppressed(failures.get(i));
            throw e;
        }
    }

    /**
     * Rethrows the given exception if the SQL state is not category 23: integrity constraint violation.
     * If the exception is a integrity constraint violation, do nothing.
     * The current version checks more specifically for error code 23505:
     * <q>duplicate key violates unique constraint</q>.
     *
     * @param  e  the exception to filter.
     * @throws SQLException if the given exception is not of category 23.
     */
    private static void filterConstraintViolation(final SQLException e) throws SQLException {
        final String state = e.getSQLState();
        if (state == null || !state.equals("23505")) throw e;
    }

    /**
     * Searches a free SRID for a new CRS definition.
     *
     * @param  schema  value of {@link Database#getSpatialSchema()}.
     * @param  sql     a preexisting builder initialized with the {@code "SELECT "} string.
     * @return an available SRID guaranteed to be greater than zero.
     * @throws SQLException if an error occurred while searching for a free SRID.
     */
    private int findFreeSRID(final SpatialSchema schema, final SQLBuilder sql) throws SQLException {
        appendFrom(sql.append("MAX(").append(schema.crsIdentifierColumn).append(')'), schema.crsTable);
        sql.append(schema.crsIdentifierColumn).append('<').append(LOW_SRID_RANGE);
        try (Statement stmt = connection.createStatement()) {
            for (boolean high = false;;) {                              // Loop will be executed 1 or 2 times.
                try (ResultSet result = stmt.executeQuery(sql.toString())) {
                    if (result.next()) {
                        final int srid = result.getInt(1) + 1;          // Next value after the highest one.
                        if (high) {
                            return Math.max(srid, HIGH_SRID_RANGE);
                        } else if (srid < LOW_SRID_RANGE) {
                            return Math.max(srid, 1);
                        }
                    }
                }
                if (high) {
                    return 1;
                }
                high = true;
                sql.removeWhereClause();
            }
        }
    }

    /**
     * Parses the CRS defined in the first non-blank column starting at the given index.
     * The expected encoding are given by {@link Database#crsEncodings} in that order.
     *
     * @param  result  the row containing the CRS definition to parse.
     * @param  column  column of the preferred CRS definition. Next columns may be used if needed.
     * @return the result of parsing the CRS definition, or {@code nnull} if none.
     */
    private Object parseDefinition(final ResultSet result, int column) throws SQLException, ParseException {
        for (CRSEncoding encoding : database.crsEncodings) {
            final String def = result.getString(column++);
            // Note: Geopackage stores "undefined" instead of no value.
            if (def == null || def.isBlank() || def.equalsIgnoreCase("undefined")) {
                continue;
            }
            switch (encoding) {
                default: return wktFormat().parseObject(def);
                // JSON encoding may be added in a future version.
            }
        }
        return null;
    }

    /**
     * Returns the object to use for parsing or formatting Well-Known Text of <abbr>CRS</abbr>.
     * The parser/formatter is created when first needed.
     */
    private WKTFormat wktFormat() {
        if (wktFormat == null) {
            wktFormat = new WKTFormat();
            wktFormat.setIndentation(WKTFormat.SINGLE_LINE);
            wktFormat.setConvention(Convention.WKT1_COMMON_UNITS);
        }
        return wktFormat;
    }

    /**
     * Closes all prepared statements. This method does <strong>not</strong> close the connection.
     *
     * @throws SQLException if an error occurred while closing a connection.
     */
    @Override
    public void close() throws SQLException {
        if (geometryColumns != null) {
            geometryColumns.close();
            geometryColumns = null;
        }
        if (wktFromSrid != null) {
            wktFromSrid.close();
            wktFromSrid = null;
        }
        if (sridFromCRS != null) {
            sridFromCRS.close();
            sridFromCRS = null;
        }
    }
}
