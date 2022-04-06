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
import java.util.Set;
import java.util.HashSet;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.text.ParseException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.internal.referencing.DefinitionVerifier;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.metadata.sql.SQLBuilder;
import org.apache.sis.internal.feature.GeometryType;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.Warnings;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Utilities;


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
 * This class is <strong>not</strong> thread-safe. Each instance should be used in a single thread.
 * Instances are created by {@link Database#createInfoStatements(Connection)}.
 *
 * @author Alexis Manin (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 * @since  1.2
 *
 * @see <a href="https://www.ogc.org/standards/sfs">OGC Simple feature access — Part 2: SQL option</a>
 *
 * @version 1.1
 * @module
 */
public class InfoStatements implements Localized, AutoCloseable {
    /**
     * The table containing CRS definitions, as specified by ISO 19125 / OGC Simple feature access part 2.
     * Note that the standard specifies table names in upper-case letters, which is also the default case
     * specified by the SQL standard. However some databases use lower cases instead. This table name can
     * be used unquoted for letting the database engine converts the case.
     */
    static final String SPATIAL_REF_SYS = "SPATIAL_REF_SYS";

    /**
     * The table containing the list of geometry columns, as specified by ISO 19125 / OGC Simple feature access part 2.
     */
    static final String GEOMETRY_COLUMNS = "GEOMETRY_COLUMNS";

    /**
     * Specifies how the geometry type is encoded in the {@code "GEOMETRY_TYPE"} column.
     * The OGC standard defines numeric values, but PostGIS uses textual values.
     *
     * @see #configureSpatialColumns(PreparedStatement, TableReference, Map, GeometryTypeEncoding)
     */
    protected enum GeometryTypeEncoding {
        /**
         * {@code "GEOMETRY_TYPE"} column is expected to contain an integer value.
         * This is the encoding used in OGC standard.
         */
        NUMERIC,

        /**
         * {@code "GEOMETRY_TYPE"} column is expected to contain a textual value.
         * This is the encoding used by PostGIS, but using a different column name
         * ({@code "TYPE"} instead of {@code "GEOMETRY_TYPE"}) for avoiding confusion.
         */
        TEXTUAL() {
            @Override GeometryType parse(final ResultSet result, final int columnIndex) throws SQLException {
                return GeometryType.forName(result.getString(columnIndex));
            }
        };

        /**
         * Decodes the geometry type encoded in the specified column of the given result set.
         * If there is no type information, then this method returns {@code null}.
         */
        GeometryType parse(final ResultSet result, final int columnIndex) throws SQLException {
            final int code = result.getInt(columnIndex);
            return result.wasNull() ? null : GeometryType.forBinaryType(code);
        }
    }

    /**
     * The database that created this set of cached statements. This object includes the
     * cache of CRS created from SRID codes and the listeners where to send warnings.
     * A {@code Database} object does <strong>not</strong> contain live JDBC {@link Connection}.
     */
    private final Database<?> database;

    /**
     * Connection to use for creating the prepared statements.
     * This connection will <strong>not</strong> be closed by this class.
     */
    private final Connection connection;

    /**
     * A statement for fetching geometric information for a specific column.
     */
    protected PreparedStatement geometryColumns;

    /**
     * The statement for fetching CRS Well-Known Text (WKT) from a SRID code.
     *
     * @see <a href="http://postgis.refractions.net/documentation/manual-1.3/ch04.html#id2571265">PostGIS documentation</a>
     */
    private PreparedStatement wktFromSrid;

    /**
     * The statement for fetching a SRID from a CRS and its set of authority codes.
     */
    private PreparedStatement sridFromCRS;

    /**
     * The object to use for parsing Well-Known Text (WKT), created when first needed.
     */
    private WKTFormat wktReader;

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
     */
    @Override
    public final Locale getLocale() {
        return database.listeners.getLocale();
    }

    /**
     * Returns a function for getting values of components in the given array.
     * If no match is found, then this method returns {@code null}.
     *
     * @param  array  the array from which to get the mapping of component values.
     * @return converter to the corresponding java type, or {@code null} if this class can not find a mapping.
     * @throws SQLException if the mapping can not be obtained.
     */
    public final ValueGetter<?> getComponentMapping(final Array array) throws SQLException {
        return database.getMapping(new Column(array.getBaseType(), array.getBaseTypeName()));
    }

    /**
     * Appends a {@code " FROM <table> WHERE "} text to the given builder.
     * The table name will be prefixed by catalog and schema name if applicable.
     */
    private void appendFrom(final SQLBuilder sql, final String table) {
        /*
         * Despite its name, `appendFunctionCall(…)` can also be used for formatting
         * table names provided that we want unquoted names (which is the case here).
         */
        database.appendFunctionCall(sql.append(" FROM "), table);
        sql.append(" WHERE ");
    }

    /**
     * Appends a statement after {@code "WHERE"} such as {@code ""F_TABLE_NAME = ?"}.
     *
     * @param  sql     the builder where to add the SQL statement.
     * @param  prefix  the column name prefix: {@code 'F'} for features or {@code 'R'} for rasters.
     * @param  column  the column name (e.g. {@code "TABLE_NAME"}.
     * @return the given SQL builder.
     */
    private static SQLBuilder appendCondition(final SQLBuilder sql, final char prefix, final String column) {
        return sql.append(prefix).append('_').append(column).append(" = ?");
    }

    /**
     * Prepares the statement for fetching information about all geometry or raster columns in a specified table.
     * This method is for {@link #completeIntrospection(TableReference, Map)} implementations.
     *
     * @param  table        name of the geometry table. Standard value is {@code "GEOMETRY_COLUMNS"}.
     * @param  prefix       column name prefix: {@code 'F'} for features or {@code 'R'} for rasters.
     * @param  column       name of the geometry column without prefix. Standard value is {@code "GEOMETRY_COLUMN"}.
     * @param  otherColumn  additional columns or {@code null} if none. Standard value is {@code "GEOMETRY_TYPE"}.
     * @return the prepared statement for querying the geometry table.
     * @throws SQLException if the statement can not be created.
     */
    protected final PreparedStatement prepareIntrospectionStatement(final String table,
            final char prefix, final String column, final String otherColumn) throws SQLException
    {
        final SQLBuilder sql = new SQLBuilder(database).append(SQLBuilder.SELECT)
                .append(prefix).append('_').append(column).append(", SRID ");
        if (otherColumn != null) sql.append(", ").append(otherColumn);
        appendFrom(sql, table);
        if (database.supportsCatalogs) appendCondition(sql, prefix, "TABLE_CATALOG").append(" AND ");
        if (database.supportsSchemas)  appendCondition(sql, prefix, "TABLE_SCHEMA" ).append(" AND ");
        appendCondition(sql, prefix, "TABLE_NAME");
        return connection.prepareStatement(sql.toString());
    }

    /**
     * Gets all geometry and raster columns for the given table and sets information on the corresponding columns.
     * Column instances in the {@code columns} map are modified in-place (the map itself is not modified).
     * This method should be invoked before the {@link Column#valueGetter} field is set.
     *
     * @param  source   the table for which to get all geometry columns.
     * @param  columns  all columns for the specified table. Keys are column names.
     * @throws DataStoreContentException if a logical error occurred in processing data.
     * @throws ParseException if the WKT can not be parsed.
     * @throws SQLException if a SQL error occurred.
     */
    public void completeIntrospection(final TableReference source, final Map<String,Column> columns) throws Exception {
        if (geometryColumns == null) {
            geometryColumns = prepareIntrospectionStatement(GEOMETRY_COLUMNS, 'F', "GEOMETRY_COLUMN", "GEOMETRY_TYPE");
        }
        configureSpatialColumns(geometryColumns, source, columns, GeometryTypeEncoding.NUMERIC);
    }

    /**
     * Implementation of {@link #completeIntrospection(TableReference, Map)} for geometries,
     * as a separated methods for allowing sub-classes to override above-cited method.
     * May also be used for non-geometric columns such as rasters, in which case the
     * {@code typeValueKind} argument shall be {@code null}.
     *
     * @param  columnQuery    a statement prepared by {@link #prepareIntrospectionStatement(String, char, String, String)}.
     * @param  source         the table for which to get all geometry columns.
     * @param  columns        all columns for the specified table. Keys are column names.
     * @param  typeValueKind  {@code NUMERIC}, {@code TEXTUAL} or {@code null} if none.
     * @throws DataStoreContentException if a logical error occurred in processing data.
     * @throws ParseException if the WKT can not be parsed.
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
        int p = 0;
        if (database.supportsCatalogs) columnQuery.setString(++p, source.catalog);
        if (database.supportsSchemas)  columnQuery.setString(++p, source.schema);
        columnQuery.setString(++p, source.table);
        try (ResultSet result = columnQuery.executeQuery()) {
            while (result.next()) {
                final Column target = columns.get(result.getString(1));
                if (target != null) {
                    final CoordinateReferenceSystem crs = fetchCRS(result.getInt(2));
                    GeometryType type = null;
                    if (typeValueKind != null) {
                        type = typeValueKind.parse(result, 3);
                        if (type == null) {
                            type = GeometryType.GEOMETRY;
                        }
                    }
                    target.makeSpatial(this, type, crs);
                }
            }
        }
    }

    /**
     * Gets a Coordinate Reference System for to given SRID.
     * If the given SRID is zero or negative, then this method returns {@code null}.
     * Otherwise the CRS is decoded from the database {@value #SPATIAL_REF_SYS} table.
     *
     * @param  srid  the Spatial Reference Identifier (SRID) to resolve as a CRS object.
     * @return the CRS associated to the given SRID, or {@code null} if the SRID is zero.
     * @throws DataStoreContentException if the CRS can not be fetched. Possible reasons are:
     *         no entry found in the {@value #SPATIAL_REF_SYS} table, or more than one entry is found,
     *         or a single entry exists but has no WKT definition and its authority code is unsupported by SIS.
     * @throws ParseException if the WKT can not be parsed.
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
     * Invoked when the requested CRS is not in the cache. This method gets the entry from the
     * {@value #SPATIAL_REF_SYS} table then gets the CRS from its authority code if possible,
     * or fallback on the WKT otherwise.
     *
     * @param  srid  the Spatial Reference Identifier (SRID) of the CRS to create from the database content.
     * @return the CRS created from database content.
     * @throws Exception if an SQL error, parsing error or other error occurred.
     */
    private CoordinateReferenceSystem parseCRS(final int srid) throws Exception {
        if (wktFromSrid == null) {
            final SQLBuilder sql = new SQLBuilder(database);
            sql.append("SELECT auth_name, auth_srid, srtext");
            appendFrom(sql, SPATIAL_REF_SYS);
            sql.append("srid=?");
            wktFromSrid = connection.prepareStatement(sql.toString());
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
                 * It the latter case, the CRS from WKT will be used only for a consistency check and
                 * the main CRS will be the one from authority.
                 */
                CoordinateReferenceSystem fromWKT = null;
                final String wkt = result.getString(3);
                if (wkt != null && !wkt.isEmpty()) {
                    final Object parsed;
                    try {
                        parsed = wktReader().parseObject(wkt);
                    } catch (ParseException e) {
                        if (authorityError != null) {
                            e.addSuppressed(authorityError);
                        }
                        throw e;
                    }
                    if (parsed instanceof CoordinateReferenceSystem) {
                        fromWKT = (CoordinateReferenceSystem) parsed;
                    } else {
                        throw invalidSRID(Resources.Keys.UnexpectedTypeForSRID_2,
                                ReferencingUtilities.getInterface(parsed), srid, authorityError);
                    }
                }
                /*
                 * If one of the CRS is null, take the non-null one. If both CRSs are defined (which is the usual case),
                 * verify that they are consistent. Inconsistency will be logged as warning if the rest of the operation
                 * succeed.
                 */
                final DefinitionVerifier v = DefinitionVerifier.compare(fromWKT, fromAuthority, getLocale());
                if (v.recommendation != null) {
                    if (crs == null) {
                        crs = v.recommendation;
                    } else if (!crs.equals(v.recommendation)) {
                        throw invalidSRID(Resources.Keys.DuplicatedSRID_2, SPATIAL_REF_SYS, srid, authorityError);
                    }
                    warning = v.warning(false);
                    if (warning == null && fromWKT != null) {
                        /*
                         * Following warnings may have occurred during WKT parsing and are considered minor.
                         * They will be reported only if there are no more important warnings to report.
                         */
                        final Warnings w = wktReader.getWarnings();
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
            throw invalidSRID(Resources.Keys.UnknownSRID_2, SPATIAL_REF_SYS, srid, null);
        }
        if (warning != null) {
            warning.setLoggerName(Modules.SQL);
            warning.setSourceClassName(getClass().getName());
            warning.setSourceMethodName("fetchCRS");
            database.listeners.warning(warning);
        }
        return crs;
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
        final DataStoreContentException e = new DataStoreContentException(
                Resources.forLocale(getLocale()).getString(message, complement, srid));
        if (suppressed != null) {
            e.addSuppressed(suppressed);
        }
        return e;
    }

    /**
     * Finds a SRID code from the spatial reference systems table for the given CRS.
     *
     * @param  crs  the CRS for which to find a SRID, or {@code null}.
     * @return SRID for the given CRS, or 0 if the given CRS was null.
     * @throws Exception if an SQL error, parsing error or other error occurred.
     */
    public final int findSRID(final CoordinateReferenceSystem crs) throws Exception {
        if (crs == null) {
            return 0;
        }
        synchronized (database.cacheOfSRID) {
            final Integer srid = database.cacheOfSRID.get(crs);
            if (srid != null) {
                return srid;
            }
        }
        final Set<SimpleImmutableEntry<String,String>> done = new HashSet<>();
        Iterator<IdentifiedObject> alternatives = null;
        IdentifiedObject candidate = crs;
        Exception error = null;
        for (;;) {
            /*
             * First, iterate over the identifiers declared in the CRS object.
             * If we can not find an identifier that we can map to a SRID, then this loop may be
             * executed more times with CRS from EPSG database that are equal, ignore axis order.
             */
            for (final Identifier id : candidate.getIdentifiers()) {
                final String authority = id.getCodeSpace();
                if (authority == null) continue;
                final String code = id.getCode();
                if (!done.add(new SimpleImmutableEntry<>(authority, code))) {
                    continue;                           // Skip "authority:code" that we already tried.
                }
                final int codeValue;
                try {
                    codeValue = Integer.parseInt(code);
                } catch (NumberFormatException e) {
                    if (error == null) error = e;
                    else error.addSuppressed(e);
                    continue;                           // Ignore codes that are not integers.
                }
                /*
                 * Found an "authority:code" pair that we did not tested before.
                 * Get the WKT and verifies if the CRS is approximately equal.
                 */
                if (sridFromCRS == null) {
                    final SQLBuilder sql = new SQLBuilder(database);
                    sql.append("SELECT srtext, srid");
                    appendFrom(sql, SPATIAL_REF_SYS);
                    sql.append("auth_name=? AND auth_srid=?");
                    sridFromCRS = connection.prepareStatement(sql.toString());
                }
                sridFromCRS.setString(1, authority);
                sridFromCRS.setInt(2, codeValue);
                try (ResultSet result = sridFromCRS.executeQuery()) {
                    while (result.next()) {
                        final String wkt = result.getString(1);
                        if (wkt != null && !wkt.isEmpty()) try {
                            final Object parsed = wktReader().parseObject(wkt);
                            if (Utilities.equalsApproximately(parsed, crs)) {
                                final int srid = result.getInt(2);
                                synchronized (database.cacheOfSRID) {
                                    database.cacheOfSRID.put(crs, srid);
                                }
                                return srid;
                            }
                        } catch (ParseException e) {
                            if (error == null) error = e;
                            else error.addSuppressed(e);
                        }
                    }
                }
            }
            /*
             * Tried all identifiers associated to the CRS and found no match.
             * It may be because the CRS has no identifier at all. Search for
             * possible identifiers in the EPSG database, then try them.
             */
            if (alternatives == null) {
                final IdentifiedObjectFinder finder = IdentifiedObjects.newFinder(Constants.EPSG);
                finder.setIgnoringAxes(true);
                alternatives = finder.find(crs).iterator();
            }
            if (!alternatives.hasNext()) break;
            candidate = alternatives.next();
        }
        throw new DataStoreReferencingException(Resources.format(
                Resources.Keys.CanNotFindSRID_1, IdentifiedObjects.getDisplayName(crs, null)), error);
    }

    /**
     * Returns the object to use for parsing Well Known Text (CRS).
     * The parser is created when first needed.
     */
    private WKTFormat wktReader() {
        if (wktReader == null) {
            wktReader = new WKTFormat(null, null);
            wktReader.setConvention(Convention.WKT1_COMMON_UNITS);
        }
        return wktReader;
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
