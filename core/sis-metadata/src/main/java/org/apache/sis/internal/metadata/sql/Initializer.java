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
import java.lang.reflect.Method;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.spi.NamingManager;
import javax.naming.NameNotFoundException;
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
 * Manages the unique {@link DataSource} instances to the {@code $SIS_DATA/Databases/SpatialMetadata} database.
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
    private static final String HOME_KEY = "derby.system.home";

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
            if (NamingManager.hasInitialContextFactoryBuilder()) try {
                final Context env = (Context) InitialContext.doLookup("java:comp/env");
                return source = (DataSource) env.lookup("jdbc/" + DATABASE);
            } catch (NameNotFoundException e) {
                Logging.unexpectedException(Logging.getLogger(Loggers.SQL), Initializer.class, "getDataSource", e);
            }
            final Path dir = DataDirectory.DATABASES.getDirectory();
            if (dir != null) {
                source = forJavaDB(dir.resolve(DATABASE));
            } else if (System.getProperty(HOME_KEY) != null) {
                source = forJavaDB(DATABASE);
            }
        }
        return source;
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
        if (NamingManager.hasInitialContextFactoryBuilder()) {
            key = Messages.Keys.JNDINotSpecified_1;
            value = "jdbc/" + DATABASE;
        } else {
            key = Messages.Keys.DataDirectoryNotSpecified_1;
            value = DataDirectory.ENV;
        }
        return Messages.getResources(locale).getString(key, value);
    }

    /**
     * Creates a data source for a Derby database at the given {@code $SIS_DATA/Databases/SpatialMetadata} location.
     * If the database does not exist, it will be created.
     *
     * @param  path  The {@code $SIS_DATA/Databases/SpatialMetadata} directory.
     * @return The data source.
     * @throws Exception if the data source can not be created.
     */
    private static DataSource forJavaDB(Path path) throws Exception {
        /*
         * If a "derby.system.home" property is set, we may be able to get a shorter path by making it
         * relative to Derby home. The intend is to have a nicer URL like "jdbc:derby:SpatialMetadata"
         * instead than "jdbc:derby:/a/long/path/to/SIS/Data/Databases/SpatialMetadata". In addition
         * to making loggings and EPSGDataAccess.getAuthority() output nicer, it also reduces the risk
         * of encoding issues if the path contains spaces or non-ASCII characters.
         */
        try {
            final String home = System.getProperty(HOME_KEY);
            if (home != null) {
                path = Paths.get(home).relativize(path);
            }
        } catch (RuntimeException e) {  // (IllegalArgumentException | SecurityException) on the JDK7 branch.
            // The path can not be relativized. This is okay. Use the public method as the logging source.
            Logging.recoverableException(Logging.getLogger(Loggers.SQL), Initializer.class, "getDataSource", e);
        }
        /*
         * Create the Derby data source using the context class loader if possible,
         * or otherwise a URL class loader to the JavaDB distributed with the JDK.
         */
        path = path.normalize();
        final DataSource ds = forJavaDB(path.toString());
        /*
         * If the database does not exist, create it. We allow creation only here because we are inside
         * the $SIS_DATA directory. The Java code creating the schemas is provided in other SIS modules.
         * For example sis-referencing may create the EPSG dataset.
         */
        if (!Files.exists(path)) {
            final Method m = ds.getClass().getMethod("setCreateDatabase", String.class);
            m.invoke(ds, "create");
            final Connection c = ds.getConnection();
            try {
                for (Initializer init : ServiceLoader.load(Initializer.class)) {
                    init.createSchema(c);
                }
            } finally {
                m.invoke(ds, "no");     // Any value other than "create".
                c.close();
            }
        }
        return ds;
    }

    /**
     * Creates a data source for a Derby database at the given location. The location may be either the
     * {@code $SIS_DATA/Databases/SpatialMetadata} directory, or the {@code SpatialMetadata} database
     * in the directory given by the {@code derby.system.home} property.
     *
     * <p>This method does <strong>not</strong> create the database if it does not exist, because this
     * method does not know if we are inside the {@code $SIS_DATA} directory.</p>
     *
     * @param  path  Relative or absolute path to the database.
     * @return The data source.
     * @throws Exception if the data source can not be created.
     */
    private static DataSource forJavaDB(final String path) throws Exception {
        try {
            return forJavaDB(path, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            final String home = System.getProperty("java.home");
            if (home != null) {
                final Path file = Paths.get(home).resolveSibling("db/lib/derby.jar");
                if (Files.isRegularFile(file)) {
                    return forJavaDB(path, new URLClassLoader(new URL[] {file.toUri().toURL()}));
                }
            }
            throw e;
        }
    }

    /**
     * Creates a data source for the given path using the given class loader.
     * If this method succeed in creating a data source, then it registers a shutdown hook
     * for shutting down the database at JVM shutdown time of Servlet/OSGi bundle uninstall time.
     *
     * @throws ClassNotFoundException if Derby is not on the classpath.
     */
    private static DataSource forJavaDB(final String path, final ClassLoader loader) throws Exception {
        final Class<?> c = Class.forName("org.apache.derby.jdbc.EmbeddedDataSource", true, loader);
        final DataSource ds = (DataSource) c.newInstance();
        final Class<?>[] args = {String.class};
        c.getMethod("setDatabaseName", args).invoke(ds, path);
        c.getMethod("setDataSourceName", args).invoke(ds, "Apache SIS spatial metadata");
        Shutdown.register(new Callable<Object>() {
            @Override public Object call() throws Exception {
                shutdown();
                return null;
            }
        });
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
                final LogRecord record = new LogRecord(Level.CONFIG, e.getLocalizedMessage());  // Not WARNING.
                record.setLoggerName(Loggers.SQL);
                Logging.log(Initializer.class, "shutdown", record);
            }
        }
    }
}
