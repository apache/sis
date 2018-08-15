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
package org.apache.sis.internal.metadata.sql;

import java.util.Locale;
import java.util.function.Supplier;
import java.util.concurrent.Callable;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.lang.reflect.Method;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.spi.NamingManager;
import javax.naming.event.EventContext;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.ObjectChangeListener;
import org.apache.sis.setup.InstallationResources;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.DataDirectory;
import org.apache.sis.internal.system.Shutdown;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.Configuration;


/**
 * Manages the unique {@link DataSource} instance to the {@code $SIS_DATA/Databases/SpatialMetadata} database.
 * This includes initialization of a new database if none existed. The schemas will be created by subclasses of
 * this {@code Initializer} class, which must be registered in the following file:
 *
 * {@preformat text
 *   META-INF/services/org.apache.sis.internal.metadata.sql.Initializer
 * }
 *
 * {@code Initializer} implementations should define the following methods:
 *
 * <ul>
 *   <li>{@link #createSchema(Connection)} — invoked when a new database is created.</li>
 *   <li>{@link #dataSourceChanged()} — invoked when the data source changed.</li>
 * </ul>
 *
 * All other methods are related to getting the {@code DataSource} instance, through JNDI or otherwise.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.7
 * @module
 */
public abstract class Initializer {
    /**
     * Name of the database to open in the {@code $SIS_DATA/Databases} directory or the directory given by
     * the {@code derby.system.home} property.
     *
     * <div class="note"><b>Note:</b>
     * this field is public for the needs of {@code non-free:sis-embedded-data} module.</div>
     */
    public static final String DATABASE = "SpatialMetadata";

    /**
     * The property name for the home of Derby databases.
     */
    private static final String DERBY_HOME_KEY = "derby.system.home";

    /**
     * Name of the JNDI resource to lookup in the {@code "java:comp/env"} context.
     */
    public static final String JNDI = "jdbc/" + DATABASE;

    /**
     * A pseudo-authority name used by {@link InstallationResources} for the embedded data resources.
     */
    public static final String EMBEDDED = "Embedded";

    /**
     * The class loader for JavaDB (i.e. the Derby database distributed with the JDK), created when first needed.
     * This field is never reset to {@code null} even if the classpath changed because this class loader is for
     * a JAR file the JDK installation directory, and we presume that the JDK installation do not change.
     *
     * @see #forJavaDB(String)
     *
     * @deprecated to be removed after we migrate to Java 9, since Derby is no longer distributed with the JDK.
     */
    @Deprecated
    private static URLClassLoader javadbLoader;

    /**
     * Data source specified by the user, to be used if no data source is specified by JNDI.
     *
     * @see #setDefault(Supplier)
     */
    private static Supplier<DataSource> supplier;

    /**
     * The unique, SIS-wide, data source to the {@code $SIS_DATA/Databases/SpatialMetadata} database.
     * Created when first needed, and cleared on shutdown.
     *
     * @see #getDataSource()
     */
    private static DataSource source;

    /**
     * {@code true} if {@link #connected(DatabaseMetaData)} has been invoked at least once.
     * This is reset to {@code false} if the {@link #source} is changed.
     * We use this information for logging purpose.
     */
    private static boolean connected;

    /**
     * For subclasses only.
     */
    protected Initializer() {
    }

    /**
     * Invoked for populating an initially empty database.
     *
     * @param  connection  connection to the empty database.
     * @throws SQLException if an error occurred while populating the database.
     */
    protected abstract void createSchema(Connection connection) throws SQLException;

    /**
     * Invoked when the JNDI data source associated to {@code "jdbc/SpatialMetadata"} changed.
     */
    protected abstract void dataSourceChanged();

    /**
     * A JNDI listener for being informed of changes in the {@link DataSource} associated to {@code "jdbc/SpatialMetadata"}.
     * This listener clears the {@link Initializer#source} field, so the next call to {@link Initializer#getDataSource()}
     * will fetch a new one. This listener is registered only if {@link Initializer#source} has been fetched from JNDI.
     */
    private static final class Listener implements ObjectChangeListener, Callable<Object> {
        /**
         * The context where this listener has been registered.
         * Used for unregistering the listener after the data source has been cleared.
         */
        private final EventContext context;

        /**
         * Creates a new listener for the given JNDI context.
         */
        private Listener(final EventContext context) {
            this.context = context;
        }

        /**
         * Registers a new listener for the given JNDI context.
         */
        static void register(final EventContext context) throws NamingException {
            final Listener listener = new Listener(context);
            context.addNamingListener(JNDI, EventContext.OBJECT_SCOPE, listener);
            Shutdown.register(listener);
        }

        /**
         * Invoked when the JVM is shutting down, or when the Servlet or OSGi bundle is uninstalled.
         * This method unregisters the listener from the JNDI context.
         */
        @Override
        public Object call() throws NamingException {
            synchronized (Initializer.class) {
                // Do not clear the DataSource - the shutdown hook for Derby needs it.
                context.removeNamingListener(this);
            }
            return null;
        }

        /**
         * Invoked when the data source associated to {@code "jdbc/SpatialMetadata"} changed.
         * This method clears the {@link Initializer#source}, unregisters this listener
         * and notifies other SIS modules.
         *
         * @param  event  ignored. Can be null.
         */
        @Override
        public void objectChanged(NamingEvent event) {
            try {
                synchronized (Initializer.class) {
                    source = null;
                    connected = false;
                    Shutdown.unregister(this);
                    context.removeNamingListener(this);
                }
            } catch (NamingException e) {
                /*
                 * Not a fatal error since the listener may be unregistered anyway, or may be unregistered
                 * automatically by other kinds of JNDI events. Even if the listener is not unregistered,
                 * it will hurt to badly: the DataSource would only be fetched more often than necessary.
                 */
                Logging.recoverableException(Logging.getLogger(Loggers.SYSTEM), Listener.class, "objectChanged", e);
            }
            for (Initializer init : DefaultFactories.createServiceLoader(Initializer.class)) {
                init.dataSourceChanged();
            }
        }

        /**
         * Invoked if JNDI lost connection to the server while preparing the {@code NamingEvent}.
         * Clears the data source anyway. In the worst case scenario, the application will fetch
         * it again from a the JNDI context.
         */
        @Override
        public void namingExceptionThrown(NamingExceptionEvent event) {
            Logging.unexpectedException(Logging.getLogger(Loggers.SYSTEM),
                    Listener.class, "namingExceptionThrown", event.getException());
            objectChanged(null);
        }
    }

    /**
     * Specifies the data source to use if there is no JNDI environment or if no data source is binded
     * to {@code jdbc/SpatialMetadata}. Data source specified by JNDI has precedence over this supplier
     * in order to let users control their data source. This method does nothing if the data source has
     * already been initialized.
     *
     * @param  ds  supplier of data source to set, or {@code null} for removing previous supplier.
     *             This supplier may return {@code null}, in which case it will be ignored.
     * @return whether the given data source supplier has been successfully set.
     *
     * @since 1.0
     */
    @Configuration
    public static synchronized boolean setDefault(final Supplier<DataSource> ds) {
        if (source == null) {
            supplier = ds;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the data source for the SIS-wide "SpatialMetadata" database.
     * This method returns the first of the following steps that succeed:
     *
     * <ol>
     *   <li>If a JNDI context exists, the data source registered under the {@code jdbc/SpatialMetadata} name.</li>
     *   <li>Otherwise if a default data source {@linkplain #setDefault has been supplied}, use that data source.</li>
     *   <li>Otherwise if the {@code non-free:sis-embedded-data} module is present on the classpath and there is no
     *       database already installed in the {@code SIS_DATA} directory, use the embedded database.</li>
     *   <li>If the {@code SIS_DATA} environment variable is defined, {@code jdbc:derby:$SIS_DATA/Databases/SpatialMetadata}.
     *       This database will be created if it does not exist. Note that this is the only case where we allow database
     *       creation since we are in the directory managed by SIS.</li>
     *   <li>If the {@code derby.system.home} property is defined, the data source for {@code jdbc:derby:SpatialMetadata}.
     *       This database will <strong>not</strong> be created if it does not exist.</li>
     *   <li>Otherwise (no JNDI, no environment variable, no Derby property set), {@code null}.</li>
     * </ol>
     *
     * @return the data source for the {@code $SIS_DATA/Databases/SpatialMetadata} or equivalent database, or {@code null} if none.
     * @throws javax.naming.NamingException     if an error occurred while fetching the data source from a JNDI context.
     * @throws java.net.MalformedURLException   if an error occurred while converting the {@code derby.jar} file to URL.
     * @throws java.lang.ClassNotFoundException if {@code derby.jar} has not been found on the JDK installation directory.
     * @throws java.lang.InstantiationException if an error occurred while creating {@code org.apache.derby.jdbc.EmbeddedDataSource}.
     * @throws java.lang.NoSuchMethodException  if a JDBC bean property has not been found on the data source.
     * @throws java.lang.IllegalAccessException if a JDBC bean property of the data source is not public.
     * @throws java.lang.reflect.InvocationTargetException if an error occurred while setting a data source bean property.
     * @throws Exception for any other kind of errors. This include {@link RuntimeException} not documented above like
     *         {@link IllegalArgumentException}, {@link ClassCastException}, {@link SecurityException}, <i>etc.</i>
     */
    public static synchronized DataSource getDataSource() throws Exception {
        if (source == null) {
            if (hasJNDI()) try {
                final Context env = (Context) InitialContext.doLookup("java:comp/env");
                source = (DataSource) env.lookup(JNDI);
                if (env instanceof EventContext) {
                    Listener.register((EventContext) env);
                }
                return source;
                /*
                 * No Derby shutdown hook for DataSource fetched fron JNDI.
                 * We presume that shutdowns are handled by the container.
                 * We do not clear the 'supplier' field in case 'source'
                 * is cleaned by the listener.
                 */
            } catch (NameNotFoundException e) {
                final LogRecord record = Messages.getResources(null).getLogRecord(
                        Level.CONFIG, Messages.Keys.JNDINotSpecified_1, JNDI);
                record.setLoggerName(Loggers.SQL);
                Logging.log(null, null, record);                // Let Logging.log(…) infers the public caller.
            }
            /*
             * At this point we determined that there is no JNDI context or no object binded to "jdbc/SpatialMetadata".
             * Check for programmatically supplied data source. We verify only after JNDI in order to let users control
             * their data source if desired.
             */
            if (supplier != null) {
                source = supplier.get();
                if (source != null) {
                    supplier = null;
                    return source;
                }
            }
            /*
             * As a fallback, try to open the Derby database located in $SIS_DATA/Databases/SpatialMetadata directory.
             * Only if the SIS_DATA environment variable is not set, verify first if the 'sis-embedded-data' module is
             * on the classpath. Note that if SIS_DATA is defined and valid, it has precedence.
             */
            boolean create = false;
            final boolean isEnvClear = DataDirectory.isEnvClear();
            if (!isEnvClear || (source = embedded()) == null) {
                final String home = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(DERBY_HOME_KEY));
                final Path dir = DataDirectory.DATABASES.getDirectory();
                final String dbURL;
                if (dir != null) {
                    Path path = dir.resolve(DATABASE);
                    if (home != null) try {
                        /*
                         * If a "derby.system.home" property is set, we may be able to get a shorter path by making it
                         * relative to Derby home. The intent is to have a nicer URL like "jdbc:derby:SpatialMetadata"
                         * instead than "jdbc:derby:/a/long/path/to/SIS/Data/Databases/SpatialMetadata". In addition
                         * to making loggings and EPSGDataAccess.getAuthority() output nicer, it also reduces the risk
                         * of encoding issues if the path contains spaces or non-ASCII characters.
                         */
                        path = Paths.get(home).relativize(path);
                    } catch (IllegalArgumentException | SecurityException e) {
                        // The path can not be relativized. This is okay.
                        Logging.recoverableException(Logging.getLogger(Loggers.SQL), Initializer.class, "getDataSource", e);
                    }
                    path   = path.normalize();
                    create = !Files.exists(path);
                    dbURL  = path.toString().replace(path.getFileSystem().getSeparator(), "/");
                } else if (home != null) {
                    final Path path = Paths.get(home);
                    create = !Files.exists(path.resolve(DATABASE)) && Files.isDirectory(path);
                    dbURL  = DATABASE;
                } else {
                    create = true;
                    dbURL  = null;
                }
                /*
                 * If we need to create the database, verify if an embedded database is available instead.
                 * We perform this check only if we have not already checked for embedded database at the
                 * beginning of this block.
                 */
                if (create & !isEnvClear) {
                    source = embedded();
                    create = (source == null);
                }
                if (source == null) {
                    if (dbURL == null) {
                        return null;
                    }
                    /*
                     * Create the Derby data source using the context class loader if possible,
                     * or otherwise a URL class loader to the JavaDB distributed with the JDK.
                     */
                    source = forJavaDB(dbURL);
                }
            }
            supplier = null;        // Not needed anymore.
            /*
             * Register the shutdown hook before to attempt any operation on the database in order to close
             * it properly if the schemas creation below fail.
             */
            Shutdown.register(() -> {
                shutdown();
                return null;
            });
            /*
             * If the database does not exist, create it. We allow creation only if we are inside
             * the $SIS_DATA directory. The Java code creating the schemas is provided in other
             * SIS modules. For example sis-referencing may create the EPSG dataset.
             */
            if (create) {
                final Method m = source.getClass().getMethod("setCreateDatabase", String.class);
                m.invoke(source, "create");
                try (Connection c = source.getConnection()) {
                    for (Initializer init : DefaultFactories.createServiceLoader(Initializer.class)) {
                        init.createSchema(c);
                    }
                } finally {
                    m.invoke(source, "no");     // Any value other than "create".
                }
            }
        }
        return source;
    }

    /**
     * Returns {@code true} if SIS will try to fetch the {@link DataSource} from JNDI.
     *
     * @return {@code true} if a JNDI environment seems to be present.
     */
    public static boolean hasJNDI() {
        return NamingManager.hasInitialContextFactoryBuilder() ||
               AccessController.doPrivileged((PrivilegedAction<Boolean>) () ->
                       System.getProperty(Context.INITIAL_CONTEXT_FACTORY) != null);
    }

    /**
     * If the {@code non-free:sis-embedded-data} module is present on the classpath,
     * returns the data source for embedded Derby database. Otherwise returns {@code null}.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-337">SIS-337</a>
     *
     * @since 0.8
     */
    private static DataSource embedded() {
        for (InstallationResources res : DefaultFactories.createServiceLoader(InstallationResources.class)) {
            if (res.getAuthorities().contains(EMBEDDED)) try {
                final String[] names = res.getResourceNames(EMBEDDED);
                for (int i=0; i<names.length; i++) {
                    if (DATABASE.equals(names[i])) {
                        final Object ds = res.getResource(EMBEDDED, i);
                        if (ds instanceof DataSource) {
                            return (DataSource) ds;
                        }
                    }
                }
            } catch (IOException e) {
                Logging.unexpectedException(Logging.getLogger(Loggers.SQL), Initializer.class, "getDataSource", e);
                // Continue - the system will fallback on the hard-coded subset of EPSG definitions.
            }
        }
        return null;
    }

    /**
     * Prepares a log record saying that a connection to the spatial metadata database has been created.
     * This method can be invoked after {@link DataSource#getConnection()}. When invoked for the first time,
     * the record level is set to {@link Level#CONFIG}. On next calls, the level become {@link Level#FINE}.
     *
     * @param  metadata  the value of {@code DataSource.getConnection().getMetaData()} or equivalent.
     * @return the record to log. Caller should set the source class name and source method name.
     * @throws SQLException if an error occurred while fetching the database URL.
     *
     * @since 0.8
     */
    public static LogRecord connected(final DatabaseMetaData metadata) throws SQLException {
        final Level level;
        synchronized (Initializer.class) {
            level = connected ? Level.FINE : Level.CONFIG;
            connected = true;
        }
        final LogRecord record = Messages.getResources(null).getLogRecord(level,
                Messages.Keys.ConnectedToGeospatialDatabase_1, SQLUtilities.getSimplifiedURL(metadata));
        record.setLoggerName(Loggers.SYSTEM);
        return record;
    }

    /**
     * Returns a message for unspecified data source. The message will depend on whether a JNDI context exists or not.
     * This message can be used for constructing an exception when {@link #getDataSource()} returned {@code null}.
     *
     * @param  locale  the locale for the message to produce, or {@code null} for the default one.
     * @param  asLog   {@code true} for returning the message as a {@link LogRecord}, {@code false} for a {@link String}.
     * @return message for unspecified data source.
     */
    public static Object unspecified(final Locale locale, final boolean asLog) {
        final short key;
        final String value;
        if (hasJNDI()) {
            key = Messages.Keys.JNDINotSpecified_1;
            value = "jdbc/" + DATABASE;
        } else {
            key = Messages.Keys.DataDirectoryNotSpecified_1;
            value = DataDirectory.ENV;
        }
        final Messages resources = Messages.getResources(locale);
        return asLog ? resources.getLogRecord(Level.WARNING, key, value) : resources.getString(key, value);
    }

    /**
     * Creates a data source for a Derby database at the given location. The location may be either the
     * {@code $SIS_DATA/Databases/SpatialMetadata} directory, or the {@code SpatialMetadata} database
     * in the directory given by the {@code derby.system.home} property.
     *
     * <p>This method does <strong>not</strong> create the database if it does not exist, because this
     * method does not know if we are inside the {@code $SIS_DATA} directory.</p>
     *
     * <p>It is caller's responsibility to shutdown the Derby database after usage.</p>
     *
     * @param  path  relative or absolute path to the database.
     * @return the data source.
     * @throws Exception if the data source can not be created.
     */
    static DataSource forJavaDB(final String path) throws Exception {
        try {
            return forJavaDB(path, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            URLClassLoader loader;
            synchronized (Initializer.class) {
                loader = javadbLoader;
                if (loader == null) {
                    final String home = System.getProperty("java.home");
                    if (home != null) {
                        final Path file = Paths.get(home).resolveSibling("db/lib/derby.jar");
                        if (Files.isRegularFile(file)) {
                            javadbLoader = loader = new URLClassLoader(new URL[] {
                                file.toUri().toURL(),
                                file.resolveSibling("derbynet.jar").toUri().toURL()
                            });
                        }
                    }
                }
            }
            if (loader == null) {
                throw e;
            }
            return forJavaDB(path, loader);
        }
    }

    /**
     * Creates a Derby data source for the given path using the given class loader.
     * It is caller's responsibility to shutdown the Derby database after usage.
     *
     * @throws ClassNotFoundException if Derby is not on the classpath.
     */
    private static DataSource forJavaDB(final String path, final ClassLoader loader) throws Exception {
        final Class<?> c = Class.forName("org.apache.derby.jdbc.EmbeddedDataSource", true, loader);
        final DataSource ds = (DataSource) c.getConstructor().newInstance();
        final Class<?>[] args = {String.class};
        c.getMethod("setDatabaseName", args).invoke(ds, path);
        c.getMethod("setDataSourceName", args).invoke(ds, "Apache SIS spatial metadata");
        return ds;
    }

    /**
     * Invoked when the JVM is shutting down, or when the Servlet or OSGi bundle is uninstalled.
     * This method shutdowns the Derby database.
     *
     * @throws ReflectiveOperationException if an error occurred while
     *         setting the shutdown property on the Derby data source.
     */
    private static synchronized void shutdown() throws ReflectiveOperationException {
        final DataSource ds = source;
        if (ds != null) {                       // Should never be null, but let be safe.
            source = null;                      // Clear now in case of failure in remaining code.
            connected = false;
            ds.getClass().getMethod("setShutdownDatabase", String.class).invoke(ds, "shutdown");
            try {
                ds.getConnection().close();     // Does the actual shutdown.
            } catch (SQLException e) {          // This is the expected exception.
                final LogRecord record = new LogRecord(Level.FINE, e.getMessage());
                if (!isSuccessfulShutdown(e)) {
                    record.setLevel(Level.WARNING);
                    record.setThrown(e);
                }
                record.setLoggerName(Loggers.SQL);
                Logging.log(Initializer.class, "shutdown", record);
            }
        }
    }

    /**
     * Returns {@code true} if the given exception is the one that we expect in successful shutdown of a Derby database.
     *
     * <div class="note"><b>Note:</b>
     * this method is public for the needs of {@code non-free:sis-embedded-data} module.</div>
     *
     * @param  e  the exception thrown by Derby.
     * @return {@code true} if the exception indicates a successful shutdown.
     */
    public static boolean isSuccessfulShutdown(final SQLException e) {
        final String state = e.getSQLState();
        return "08006".equals(state) ||     // Database 'SpatialMetadata' shutdown.
               "XJ004".equals(state);       // Database 'SpatialMetadata' not found (may happen if we failed to open it in the first place).
    }
}
