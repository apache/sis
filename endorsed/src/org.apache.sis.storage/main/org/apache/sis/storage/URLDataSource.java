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
package org.apache.sis.storage;

import java.net.URI;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import org.apache.sis.util.Classes;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.storage.internal.Resources;
import static org.apache.sis.storage.base.StoreUtilities.LOGGER;


/**
 * A data source which gets the connections from a {@link DriverManager}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
final class URLDataSource implements DataSource {
    /**
     * The driver names of the connection returned by {@code URLDataSource}.
     * This is used for logging purposes only.
     */
    private static final Set<String> DRIVERS = new HashSet<>();

    /**
     * The URL to use for connecting to the database.
     * This field can not be {@code null}.
     */
    private final String url;

    /**
     * Creates a data source for the given URL.
     * The value of {@link URI#getScheme()} should be {@code "jdbc"}, ignoring case.
     *
     * @param url The URL to use for connecting to the database.
     */
    public URLDataSource(final URI url) {
        this.url = url.toString();
    }

    /**
     * Logs the driver version if this is the first time we get a connection for that driver.
     */
    private static Connection log(final Connection connection) throws SQLException {
        if (LOGGER.isLoggable(Level.CONFIG)) {
            final DatabaseMetaData metadata = connection.getMetaData();
            final String name = metadata.getDriverName();
            final boolean log;
            synchronized (DRIVERS) {
                log = DRIVERS.add(name);
            }
            if (log) {
                final LogRecord record = Resources.forLocale(null)
                        .getLogRecord(Level.CONFIG, Resources.Keys.UseJdbcDriverVersion_3, name,
                                      metadata.getDriverMajorVersion(), metadata.getDriverMinorVersion());
                Logging.completeAndLog(LOGGER, StorageConnector.class, "getStorageAs", record);
            }
        }
        return connection;
    }

    /**
     * Delegates to {@link DriverManager}.
     *
     * @throws SQLException If the connection can not be established.
     */
    @Override
    public Connection getConnection() throws SQLException {
        return log(DriverManager.getConnection(url));
    }

    /**
     * Delegates to {@link DriverManager}.
     *
     * @throws SQLException If the connection can not be established.
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return log(DriverManager.getConnection(url, username, password));
    }

    /**
     * Delegates to {@link DriverManager}.
     */
    @Override
    public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
    }

    /**
     * Delegates to {@link DriverManager}. It is better to avoid
     * calling this method since it has a system-wide effect.
     */
    @Override
    public void setLogWriter(final PrintWriter out) {
        DriverManager.setLogWriter(out);
    }

    /**
     * Delegates to {@link DriverManager}.
     */
    @Override
    public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
    }

    /**
     * Delegates to {@link DriverManager}. It is better to avoid
     * calling this method since it has a system-wide effect.
     */
    @Override
    public void setLoginTimeout(final int seconds) {
        DriverManager.setLoginTimeout(seconds);
    }

    /**
     * Returns (@code false} in all cases, since this class is not a wrapper (omitting {@code DriverManager}).
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }

    /**
     * Throws an exception in all cases, since this class is not a wrapper (omitting {@code DriverManager}).
     *
     * @param <T> Ignored.
     */
    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        throw new SQLException();
    }

    /**
     * Compares this data source with the given object for equality.
     *
     * @param other The object to compare with this data source.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        return (other instanceof URLDataSource) && url.equals(((URLDataSource) other).url);
    }

    /**
     * Returns a hash code value for this data source.
     *
     * @return A hash code value for this data source.
     */
    @Override
    public int hashCode() {
        return url.hashCode() ^ 335483867;
    }

    /**
     * Returns a string representation of this data source.
     */
    @Override
    public String toString() {
        return Classes.getShortClassName(this) + "[\"" + url + "\"]";
    }

    /**
     * Returns the parent logger for this data source.
     *
     * @return the parent Logger for this data source
     */
    @Override
    public Logger getParentLogger() {
        return LOGGER;
    }
}
