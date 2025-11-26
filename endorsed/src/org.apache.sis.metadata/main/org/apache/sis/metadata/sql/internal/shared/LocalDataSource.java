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
package org.apache.sis.metadata.sql.internal.shared;

import java.util.Arrays;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.concurrent.Callable;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.Loggers;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.system.Reflect;
import org.apache.sis.util.internal.shared.Strings;


/**
 * A data source for a database stored locally in the {@code $SIS_DATA} directory.
 * This class wraps the database-provided {@link DataSource} with the addition of a shutdown method.
 * It provides our {@linkplain #initialize() starting point} for initiating the system-wide connection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class LocalDataSource implements DataSource, Comparable<LocalDataSource> {
    /**
     * Where to log warnings.
     */
    static final Logger LOGGER = Logger.getLogger(Loggers.SQL);

    /**
     * The property name for the home of Derby databases.
     */
    private static final String DERBY_HOME_KEY = "derby.system.home";

    /**
     * The database product to use.
     * Currently supported values are {@link Dialect#DERBY} and {@link Dialect#HSQL}.
     */
    private final Dialect dialect;

    /**
     * Path to the database to open on the local file system. This is the argument to give to the
     * {@code DataSource.setDatabaseName(String)} method. This value is set to {@code null} after
     * {@link #source} creation because not needed anymore.
     */
    private String dbFile;

    /**
     * The database-provided data source.
     */
    private DataSource source;

    /**
     * Whether the database needs to be created.
     */
    final boolean create;

    /**
     * Prepares a new data source for the given database file. This construction is incomplete:
     * the {@link #initialize()} method shall be invoked after construction,
     * unless the caller decides to discard this {@code LocalDataSource} instance.
     *
     * @param  dialect  {@link Dialect#DERBY} or {@link Dialect#HSQL}.
     * @param  dbFile   path to the database to open on the local file system.
     * @param  create   whether the database needs to be created.
     */
    private LocalDataSource(final Dialect dialect, final String dbFile, final boolean create) {
        this.dialect = dialect;
        this.dbFile  = dbFile;
        this.create  = create;
    }

    /**
     * Prepares potential data source for the spatial metadata database. This constructor prepares
     * the path to local file(s), but actual {@link DataSource} construction must be done by a call
     * to {@link #initialize()} after construction.
     *
     * @param  database  database name (usually {@value Initializer#DATABASE}).
     * @param  dialects  {@link Dialect#DERBY} and/or {@link Dialect#HSQL}.
     * @return the local data sources (not yet initialized), or {@code null} if none.
     *         If non-null, then the array is guaranteed to contain at least one element.
     */
    static LocalDataSource[] create(final String database, final Dialect... dialects) {
        LocalDataSource[] sources = new LocalDataSource[dialects.length];
        int count = 0;
        for (final Dialect dialect : dialects) {
            final String home;
            switch (dialect) {
                // More cases may be added in the future.
                case DERBY: home = System.getProperty(DERBY_HOME_KEY); break;
                default:    home = null; break;
            }
            final String  dbFile;
            final boolean create;
            final Path dir = DataDirectory.DATABASES.getDirectory();
            if (dir != null) {
                /*
                 * SIS_DATA directory defined: will search only there (no search in the Derby home directory).
                 * If a "derby.system.home" property is set, we may be able to get a shorter path by making it
                 * relative to Derby home. The intent is to have a nicer URL like "jdbc:derby:SpatialMetadata"
                 * instead of "jdbc:derby:/a/long/path/to/SIS/Data/Databases/SpatialMetadata".   In addition
                 * to making loggings and EPSGDataAccess.getAuthority() output nicer, it also reduces the risk
                 * of encoding issues if the path contains spaces or non-ASCII characters.
                 */
                Path path = dir.resolve(database);
                if (home != null) try {
                    path = Path.of(home).relativize(path);
                } catch (IllegalArgumentException | SecurityException e) {
                    // The path cannot be relativized. This is okay.
                    Logging.recoverableException(LOGGER, LocalDataSource.class, "<init>", e);
                }
                path   = path.normalize();
                dbFile = path.toString().replace(path.getFileSystem().getSeparator(), "/");
                switch (dialect) {
                    case HSQL: path = Path.of(path.toString() + ".data"); break;
                    // More cases may be added in the future.
                }
                create = Files.notExists(path);
            } else if (home != null) {
                /*
                 * SIS_DATA not defined, but we may be able to fallback on "derby.system.home" property.
                 * This fallback is never executed if the SIS_DATA environment variable is defined, even
                 * if the database does not exist in that directory, because otherwise users could define
                 * SIS_DATA and get the impression that their setting is ignored.
                 */
                final Path path = Path.of(home);
                create = Files.notExists(path.resolve(database)) && Files.isDirectory(path);
                dbFile = database;
            } else {
                continue;
            }
            sources[count++] = new LocalDataSource(dialect, dbFile, create);
        }
        /*
         * Sort the data source in preference order, with already existing databases first.
         * If there is no data source then we must return null, not an empty array.
         */
        if (count == 0) return null;
        sources = ArraysExt.resize(sources, count);
        Arrays.sort(sources);
        return sources;
    }

    /**
     * Wraps an existing data source for adding a shutdown method to it.
     * This method is used for source of data embedded in a separated <abbr>JAR</abbr> file.
     *
     * @param  source  the data source, usually given by {@link Initializer#embedded()}.
     * @return the data source wrapped with a shutdown method, or {@code ds}.
     */
    static DataSource wrap(final DataSource source) {
        final Dialect dialect;
        final String cn = source.getClass().getName();
        if (cn.startsWith("org.apache.derby.")) {
            dialect = Dialect.DERBY;
        } else if (cn.startsWith("org.hsqldb.")) {
            dialect = Dialect.HSQL;
        } else {
            return source;
        }
        final var local = new LocalDataSource(dialect, null, false);
        local.source = source;
        return local;
    }

    /**
     * Creates the data source using the context class loader.
     * It is caller's responsibility to {@linkplain #shutdown() shutdown} the database after usage.
     *
     * @throws ClassNotFoundException if the database driver is not on the module path.
     * @throws ReflectiveOperationException if an error occurred
     *         while setting the properties on the data source.
     */
    @SuppressWarnings("fallthrough")
    private void initialize() throws ReflectiveOperationException {
        final String classname;
        switch (dialect) {
            case DERBY: classname = "org.apache.derby.jdbc.EmbeddedDataSource"; break;
            case HSQL:  classname = "org.hsqldb.jdbc.JDBCDataSource"; break;
            default:    throw new IllegalArgumentException(dialect.toString());
        }
        Class<?> c;
        try {
            c = Class.forName(classname, true, Reflect.getContextClassLoader());
        } catch (SecurityException e) {
            Reflect.log(Initializer.class, "getDataSource", e);     // Public caller of this method.
            c = Class.forName(classname);
        }
        source = (DataSource) c.getConstructor().newInstance();
        final Class<?>[] args = {String.class};
        switch (dialect) {
            case DERBY: c.getMethod("setDataSourceName", args).invoke(source, "Apache SIS spatial metadata");   // Fall through
            case HSQL:  c.getMethod("setDatabaseName",   args).invoke(source, dbFile); break;
        }
        dbFile = null;          // Not needed anymore (let GC do its work).
    }

    /**
     * Returns the first data source from the given array that can be initialized.
     * The database may be located in the {@code $SIS_DATA/Databases/SpatialMetadata} directory,
     * or in the {@code SpatialMetadata} sub-directory of the path given by the {@code derby.system.home} property.
     *
     * <p>This method does <strong>not</strong> create the database if it does not exist,
     * because this method does not know if we are inside the {@code $SIS_DATA} directory.</p>
     *
     * <p>It is caller's responsibility to {@linkplain #shutdown() shutdown} the database after usage.</p>
     *
     * @param  sources  the data sources to try.
     * @return the first data source for which a driver is available.
     * @throws ClassNotFoundException if no database driver is not on the module path.
     * @throws Exception if the operation failed for another reason.
     */
    static LocalDataSource findDriver(final LocalDataSource[] sources) throws Exception {
        ClassNotFoundException fail = null;
        for (final LocalDataSource local : sources) try {
            local.initialize();
            return local;
        } catch (ClassNotFoundException e) {
            if (fail == null) fail = e;
            else fail.addSuppressed(e);
        }
        throw fail;
    }

    /**
     * Creates the database if needed.
     * For Derby we need to explicitly allow creation.
     * For HSQLDB the creation is enabled by default.
     */
    final void createDatabase() throws Exception {
        if (create) {
            Callable<?> finisher = null;
            switch (dialect) {
                // More cases may be added in future versions.
                case DERBY: {
                    final Class<?> c = source.getClass();
                    final Method setter = c.getMethod("setCreateDatabase", String.class);
                    finisher = () -> setter.invoke(source, "no");      // Any value other than "create".
                    setter.invoke(source, "create");
                    /*
                     * Make the database uses case-insensitive and accent-insensitive searches.
                     * https://db.apache.org/derby/docs/10.17/devguide/tdevdvlpcaseinscoll.html
                     */
                    c.getMethod("setConnectionAttributes", String.class)
                            .invoke(source, "territory=en_GB;collation=TERRITORY_BASED:PRIMARY");
                    break;
                }
            }
            try (Connection c = source.getConnection()) {
                for (Initializer init : Initializer.load()) {
                    init.createSchema(c);
                }
            } finally {
                if (finisher != null) {
                    finisher.call();
                }
            }
        }
    }

    /**
     * Shutdowns the database used by this data source.
     *
     * @throws ReflectiveOperationException if an error occurred while
     *         setting the shutdown property on the Derby data source.
     */
    final void shutdown() throws ReflectiveOperationException {
        try {
            switch (dialect) {
                case HSQL: {
                    try (Connection c = source.getConnection(); Statement stmt = c.createStatement()) {
                        stmt.execute(create ? "SHUTDOWN COMPACT" : "SHUTDOWN");
                    }
                    break;
                }
                case DERBY: {
                    source.getClass().getMethod("setShutdownDatabase", String.class).invoke(source, "shutdown");
                    source.getConnection().close();             // Does the actual shutdown.
                    break;
                }
            }
        } catch (SQLException e) {                              // This is the expected exception.
            final var record = new LogRecord(Level.FINER, e.getMessage());
            if (dialect != Dialect.DERBY || !isSuccessfulShutdown(e)) {
                record.setLevel(Level.WARNING);
                record.setThrown(e);
            }
            Logging.completeAndLog(LOGGER, LocalDataSource.class, "shutdown", record);
        }
    }

    /**
     * Returns {@code true} if the given exception is the one that we expect in successful shutdown of a Derby database.
     * While this method is primarily used for Derby shutdown, the error code tested may be applicable to other systems.
     *
     * <h4>Dependency note</h4>
     * This method is public for the needs of {@code org.apache.sis.referencing.database} module.
     *
     * @param  e  the exception thrown by Derby.
     * @return {@code true} if the exception indicates a successful shutdown.
     */
    public static boolean isSuccessfulShutdown(final SQLException e) {
        final String state = e.getSQLState();
        return "08006".equals(state) ||     // Database shutdown.
               "XJ004".equals(state);       // Database not found (may happen if we failed to open it in the first place).
    }

    /**
     * Compares this data source with the given one for preference order.
     * The preferred data sources are the ones for a database that already exists.
     *
     * @param  other  the other data source to compare with this one.
     * @return -1 if this data source is preferred to {@code other},
     *         +1 if {@code other} is preferred to {@code this}, or
     *          0 if no preference.
     */
    @Override
    public int compareTo(final LocalDataSource other) {
        return Boolean.compare(create, other.create);
    }

    /**
     * Returns whether {@link #unwrap(Class)} can be invoked for the given type.
     *
     * @param  type   the interface or implementation type of desired wrapped object.
     * @return whether {@link #unwrap(Class)} can be invoked for the given type.
     * @throws SQLException if an error occurs while checking wrappers.
     */
    @Override
    public boolean isWrapperFor(final Class<?> type) throws SQLException {
        return (type == LocalDataSource.class) || source.isWrapperFor(type);
    }

    /**
     * Returns an object of the given type to allow access to non-standard methods.
     * The type can be either {@link LocalDataSource} or any type supported by the
     * wrapped data source.
     *
     * @param  <T>   compile-time value of {@code type}.
     * @param  type  the interface or implementation type of desired wrapped object.
     * @return an object of the given type.
     * @throws SQLException if there is no object of the given type.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(final Class<T> type) throws SQLException {
        if (type == LocalDataSource.class) {
            return (T) this;
        }
        return source.unwrap(type);
    }

    /**
     * Attempts to establish a connection.
     *
     * @return a connection to the locally installed database.
     * @throws SQLException if a database access error occurs.
     */
    @Override
    public Connection getConnection() throws SQLException {
        return source.getConnection();
    }

    /**
     * Attempts to establish a connection.
     *
     * @param  username  the database user.
     * @param  password  the user's password.
     * @return a connection to the locally installed database.
     * @throws SQLException if a database access error occurs.
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return source.getConnection(username, password);
    }

    /**
     * Returns the maximum time in seconds that this data source will wait while attempting to connect to a database.
     * Initial value is 0, meaning default timeout or no timeout.
     *
     * @return the data source login time limit, or 0 for the default.
     * @throws SQLException if a database access error occurs.
     */
    @Override
    public int getLoginTimeout() throws SQLException {
        return source.getLoginTimeout();
    }

    /**
     * Sets the maximum time in seconds that this data source will wait while attempting to connect to a database.
     *
     * @param  seconds   the data source login time limit, or 0 for the default.
     * @throws SQLException if a database access error occurs.
     */
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        source.setLoginTimeout(seconds);
    }

    /**
     * Return the parent of all loggers used by this data source.
     * Can be used for configuring log messages.
     *
     * @return the parent of all loggers used by this data source.
     * @throws SQLFeatureNotSupportedException if the data source does not use logging.
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return source.getParentLogger();
    }

    /**
     * Returns the output stream to which all logging and tracing messages for this data source will be printed.
     * The default writer is {@code null} (logging disabled).
     *
     * @return the log writer, or null if logging is disabled.
     * @throws SQLException if a database access error occurs.
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return source.getLogWriter();
    }

    /**
     * Sets the output stream to which all logging and tracing messages for this data source will be printed.
     * This method needs to be invoked for enabling logging.
     *
     * @param out the log writer, or null if logging is disabled.
     * @throws SQLException if a database access error occurs.
     */
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        source.setLogWriter(out);
    }

    /**
     * Returns a string representation for debugging purpose.
     *
     * @return an arbitrary string representation.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), null, dialect, "dbFile", dbFile, "source", source);
    }
}
