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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.lang.reflect.Method;
import javax.sql.DataSource;
import java.sql.Connection;
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
import org.apache.sis.internal.system.DataDirectory;
import org.apache.sis.internal.system.Shutdown;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.logging.Logging;

// Branch-dependent imports
import java.util.concurrent.Callable;
import org.apache.sis.internal.jdk7.Files;
import org.apache.sis.internal.jdk7.Path;
import org.apache.sis.internal.jdk7.Paths;


/**
 * Manages the unique {@link DataSource} instance to the {@code $SIS_DATA/Databases/SpatialMetadata} database.
 * This includes initialization of a new database if none existed. The schemas will be created by subclasses of
 * this {@code Initializer} class, which must be registered in the following file:
 *
 * {@preformat text
 *   META-INF/services/org.apache.sis.internal.metadata.sql.Initializer
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class Initializer {
    /**
     * Name of the database to open in the {@code $SIS_DATA/Databases} directory or the directory given by
     * the {@code derby.system.home} property.
     */
    private static final String DATABASE = "SpatialMetadata";

    /**
     * The property name for the home of Derby databases.
     */
    private static final String DERBY_HOME_KEY = "derby.system.home";

    /**
     * Name of the JNDI resource to lookup in the {@code "java:comp/env"} context.
     */
    public static final String JNDI = "jdbc/" + DATABASE;

    /**
     * The class loader for JavaDB (i.e. the Derby database distributed with the JDK), created when first needed.
     * This field is never reset to {@code null} even if the classpath changed because this class loader is for
     * a JAR file the JDK installation directory, and we presume that the JDK installation do not change.
     *
     * @see #forJavaDB(String)
     */
    private static URLClassLoader javadbLoader;

    /**
     * The unique, SIS-wide, data source to the {@code $SIS_DATA/Databases/SpatialMetadata} database.
     * Created when first needed, and cleared on shutdown.
     *
     * @see #getDataSource()
     */
    private static DataSource source;

    /**
     * For subclasses only.
     */
    protected Initializer() {
    }

    /**
     * Invoked for populating an initially empty database.
     *
     * @param connection Connection to the empty database.
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
     * will fetch a new one.
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
         * @param event Ignored. May be null.
         */
        @Override
        public void objectChanged(NamingEvent event) {
            try {
                synchronized (Initializer.class) {
                    source = null;
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
            for (Initializer init : ServiceLoader.load(Initializer.class)) {
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
     * Returns the data source for the SIS-wide "SpatialMetadata" database.
     * This method returns the first of the following steps that succeed:
     *
     * <ol>
     *   <li>If a JNDI context exists, the data source registered under the {@code jdbc/SpatialMetadata} name.</li>
     *   <li>If the {@code SIS_DATA} environment variable is defined, {@code jdbc:derby:$SIS_DATA/Databases/SpatialMetadata}.
     *       This database will be created if it does not exist. Note that this is the only case where we allow database
     *       creation since we are in the directory managed by SIS.</li>
     *   <li>If the {@code derby.system.home} property is defined, the data source for {@code jdbc:derby:SpatialMetadata}.
     *       This database will <strong>not</strong> be created if it does not exist.</li>
     *   <li>Otherwise (no JNDI, no environment variable, no Derby property set), {@code null}.</li>
     * </ol>
     *
     * @return The data source for the {@code $SIS_DATA/Databases/SpatialMetadata} or equivalent database, or {@code null} if none.
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
            } catch (NameNotFoundException e) {
                final LogRecord record = Messages.getResources(null).getLogRecord(
                        Level.CONFIG, Messages.Keys.JNDINotSpecified_1, JNDI);
                record.setLoggerName(Loggers.SQL);
                Logging.log(Initializer.class, "getDataSource", record);
            }
            /*
             * At this point we determined that there is no JNDI context or no object binded to "jdbc/SpatialMetadata".
             * As a fallback, try to open the Derby database located in $SIS_DATA/Databases/SpatialMetadata directory.
             */
            final boolean create;
            final String home = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override public String run() {
                    return System.getProperty(DERBY_HOME_KEY);
                }
            });
            final Path dir = DataDirectory.DATABASES.getDirectory();
            if (dir != null) {
                Path path = dir.resolve(DATABASE);
                if (home != null) try {
                    /*
                     * If a "derby.system.home" property is set, we may be able to get a shorter path by making it
                     * relative to Derby home. The intend is to have a nicer URL like "jdbc:derby:SpatialMetadata"
                     * instead than "jdbc:derby:/a/long/path/to/SIS/Data/Databases/SpatialMetadata". In addition
                     * to making loggings and EPSGDataAccess.getAuthority() output nicer, it also reduces the risk
                     * of encoding issues if the path contains spaces or non-ASCII characters.
                     */
                    path = Paths.get(home).relativize(path);
                } catch (Exception e) {     // (IllegalArgumentException | SecurityException e) on the JDK7 branch.
                    // The path can not be relativized. This is okay.
                    Logging.recoverableException(Logging.getLogger(Loggers.SQL), Initializer.class, "getDataSource", e);
                }
                /*
                 * Create the Derby data source using the context class loader if possible,
                 * or otherwise a URL class loader to the JavaDB distributed with the JDK.
                 */
                path   = path.normalize();
                create = !Files.exists(path);
                source = forJavaDB(path.toString());
            } else if (home != null) {
                final Path path = Paths.get(home);
                create = !Files.exists(path.resolve(DATABASE)) && Files.isDirectory(path);
                source = forJavaDB(DATABASE);
            } else {
                return null;
            }
            /*
             * Register the shutdown hook before to attempt any operation on the database in order to close
             * it properly if the schemas creation below fail.
             */
            Shutdown.register(new Callable<Object>() {
                @Override public Object call() throws Exception {
                    shutdown();
                    return null;
                }
            });
            /*
             * If the database does not exist, create it. We allow creation only if we are inside
             * the $SIS_DATA directory. The Java code creating the schemas is provided in other
             * SIS modules. For example sis-referencing may create the EPSG dataset.
             */
            if (create) {
                final Method m = source.getClass().getMethod("setCreateDatabase", String.class);
                m.invoke(source, "create");
                Connection c = source.getConnection();
                try {
                    for (Initializer init : ServiceLoader.load(Initializer.class)) {
                        init.createSchema(c);
                    }
                } finally {
                    c.close();
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
                AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                    @Override public Boolean run() {
                        return System.getProperty(Context.INITIAL_CONTEXT_FACTORY) != null;
                    }
                });
    }

    /**
     * Returns a message for unspecified data source. The message will depend on whether a JNDI context exists or not.
     * This message can be used for constructing an exception when {@link #getDataSource()} returned {@code null}.
     *
     * @param locale The locale for the message to produce, or {@code null} for the default one.
     * @return Message for unspecified data source.
     */
    public static String unspecified(final Locale locale) {
        final short key;
        final String value;
        if (hasJNDI()) {
            key = Messages.Keys.JNDINotSpecified_1;
            value = "jdbc/" + DATABASE;
        } else {
            key = Messages.Keys.DataDirectoryNotSpecified_1;
            value = DataDirectory.ENV;
        }
        return Messages.getResources(locale).getString(key, value);
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
     * @param  path  Relative or absolute path to the database.
     * @return The data source.
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
                            javadbLoader = loader = new URLClassLoader(new URL[] {file.toUri().toURL()});
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
        final DataSource ds = (DataSource) c.newInstance();
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
    private static synchronized void shutdown() throws Exception {
        final DataSource ds = source;
        if (ds != null) {                       // Should never be null, but let be safe.
            source = null;                      // Clear now in case of failure in remaining code.
            ds.getClass().getMethod("setShutdownDatabase", String.class).invoke(ds, "shutdown");
            try {
                ds.getConnection().close();     // Does the actual shutdown.
            } catch (SQLException e) {          // This is the expected exception.
                final LogRecord record = new LogRecord(Level.FINE, e.getLocalizedMessage());
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
     * @param e The exception thrown by Derby.
     * @return {@code true} if the exception indicates a successful shutdown.
     */
    static boolean isSuccessfulShutdown(final SQLException e) {
        final String state = e.getSQLState();
        return "08006".equals(state) ||     // Database 'SpatialMetadata' shutdown.
               "XJ004".equals(state);       // Database 'SpatialMetadata' not found (may happen if we failed to open it in the first place).
    }
}
