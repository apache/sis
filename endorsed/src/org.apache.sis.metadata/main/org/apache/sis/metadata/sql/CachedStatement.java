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
package org.apache.sis.metadata.sql;

import java.util.logging.Level;
import java.util.logging.Filter;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.system.Loggers;


/**
 * The result of a query for metadata attributes. This object {@linkplain PreparedStatement prepares a statement}
 * only once for a given table, until a certain period of inactivity is elapsed. When a particular record in the
 * table is fetched, the {@link ResultSet} is automatically constructed. If many attributes are fetched consecutively
 * for the same record, then the same {@link ResultSet} is reused.
 *
 * <h2>Synchronization</h2>
 * This class is <strong>not</strong> thread-safe. Callers must perform their own synchronization in such a way
 * that only one query is executed on the same connection (JDBC connections cannot be assumed thread-safe).
 * The synchronization lock shall be the {@link MetadataSource} which contain this entry.
 *
 * <h2>Closing</h2>
 * While this class implements {@link java.lang.AutoCloseable}, it should not be used in a try-finally block.
 * This is because {@code CachedStatement} is typically closed by a different thread than the one that created
 * the {@code CachedStatement} instance. This object is closed by a background thread of {@link MetadataSource}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class CachedStatement implements AutoCloseable {
    /**
     * Where to log warnings.
     */
    static final Logger LOGGER = Logger.getLogger(Loggers.SQL);

    /**
     * The interface for which the prepared statement has been created.
     */
    final Class<?> type;

    /**
     * The identifier (usually the primary key) for current results. If the record to fetch does not
     * have the same identifier, then the {@link #results} will need to be closed and reconstructed.
     */
    private String identifier;

    /**
     * The statement associated with this entry. The SQL query depends on the {@link #type},
     * which cannot be changed, and the {@link #identifier}, which can be changed at any time.
     * The first parameter of the statement shall be the identifier.
     */
    private final PreparedStatement statement;

    /**
     * The results of last call to {@link PreparedStatement#executeQuery()},
     * or {@code null} if not yet determined.
     */
    private ResultSet results;

    /**
     * The expiration time of this result, in nanoseconds as given by {@link System#nanoTime()}.
     * This is read and updated by {@link MetadataSource} only.
     */
    long expireTime;

    /**
     * Where to report the warnings before to eventually log them.
     */
    private final Filter logFilter;

    /**
     * Constructs a metadata result from the specified connection.
     *
     * @param type       the GeoAPI interface to implement.
     * @param statement  the prepared statement.
     * @param logFilter  where to report the warnings.
     */
    CachedStatement(final Class<?> type, final PreparedStatement statement, final Filter logFilter) {
        this.type      = type;
        this.statement = statement;
        this.logFilter = logFilter;
    }

    /**
     * Returns the attribute value in the given column for the given record.
     *
     * @param  id         the object identifier, usually the primary key value.
     * @param  attribute  the column name of the attribute to fetch.
     * @return the value of the requested attribute for the row identified by the given key.
     * @throws SQLException if an SQL operation failed.
     * @throws MetadataStoreException if no record has been found for the given key.
     */
    final Object getValue(final String id, final String attribute) throws SQLException, MetadataStoreException {
        if (!id.equals(identifier)) {
            closeResultSet();
        }
        ResultSet r = results;
        if (r == null) {
            statement.setString(1, id);
            r = statement.executeQuery();
            if (!r.next()) {
                final String table = r.getMetaData().getTableName(1);
                r.close();
                throw new MetadataStoreException(Errors.format(Errors.Keys.RecordNotFound_2, table, id));
            }
            results = r;
            identifier = id;
        }
        /*
         * As of Java 10, enumerations have no constants defined in java.sql.Types.
         * Consequently, databases returns an implementation-specific object, e.g.
         * org.postgresql.util.PGobject. To avoid implementation-specific code,
         * we are better to get those enumeration values as strings.
         */
        final int column = r.findColumn(attribute);
        switch (r.getMetaData().getColumnType(column)) {
            case Types.OTHER: return r.getString(column);       // For enumeration values.
            default:          return r.getObject(column);       // For all standard types.
        }
    }

    /**
     * Closes the current {@link ResultSet}. Before doing so, we make an opportunist check for duplicated values
     * in the table. If a duplicate is found, a warning is logged. The log message pretends to be emitted by the
     * interface constructor, which does not exist. But this is the closest we can get from a public API.
     */
    private void closeResultSet() throws SQLException {
        final ResultSet r = results;
        results = null;               // Make sure that this field is cleared even if an exception occurs below.
        if (r != null && !r.isClosed()) {
            final boolean hasNext = r.next();
            r.close();
            if (hasNext) {
                warning(type, "<init>", Errors.forLocale(null).createLogRecord(
                        Level.WARNING, Errors.Keys.DuplicatedIdentifier_1, identifier));
            }
            identifier = null;
        }
    }

    /**
     * Closes the statement and free all resources.
     * After this method has been invoked, this object cannot be used anymore.
     *
     * <p>This method is not invoked by the method or thread that created this {@code CachedStatement} instance.
     * This method is invoked by {@link MetadataSource#close()} instead.</p>
     *
     * @throws SQLException if an error occurred while closing the statement.
     */
    @Override
    public void close() throws SQLException {
        closeResultSet();
        statement.close();
    }

    /**
     * Reports a warning.
     *
     * @param source  the class to report as the warning emitter.
     * @param method  the method to report as the warning emitter.
     * @param record  the warning to report.
     */
    private void warning(final Class<?> source, final String method, final LogRecord record) {
        record.setSourceClassName(source.getCanonicalName());
        record.setSourceMethodName(method);
        record.setLoggerName(Loggers.SQL);
        if (logFilter == null || logFilter.isLoggable(record)) {
            LOGGER.log(record);
        }
    }
}
