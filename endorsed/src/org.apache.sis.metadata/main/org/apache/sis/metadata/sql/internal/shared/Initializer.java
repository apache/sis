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

import java.util.Locale;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.NamingManager;
import javax.naming.event.EventContext;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.ObjectChangeListener;
import org.apache.sis.setup.InstallationResources;
import org.apache.sis.system.Configuration;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.system.SystemListener;
import org.apache.sis.system.Shutdown;
import org.apache.sis.system.Reflect;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.logging.Logging;
import static org.apache.sis.util.internal.shared.MetadataServices.EMBEDDED;


/**
 * Manages the unique {@link DataSource} instance to the {@code $SIS_DATA/Databases/SpatialMetadata} database.
 * This includes initialization of a new database if none existed. The schemas will be created by subclasses
 * of this {@code Initializer} class, which must be registered in {@code module-info.java} file as a provider
 * of the {@code org.apache.sis.metadata.sql.internal.shared.Initializer} service.
 *
 * <p>{@code Initializer} implementations should define the following methods:</p>
 * <ul>
 *   <li>{@link #createSchema(Connection)} — invoked when a new database is created.</li>
 *   <li>{@link #dataSourceChanged()} — invoked when the data source changed.</li>
 * </ul>
 *
 * All other methods are related to getting the {@code DataSource} instance, through JNDI or otherwise.
 *
 * <h2>Note for maintainers about synchronization</h2>
 * Some operations are synchronized on {@code Initializer} not only in this class, but also in other modules.
 * If the synchronization lock is changed, search {@code synchronized (Initializer.class)} in the whole project.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class Initializer {
    /**
     * Name of the database to open in the {@code $SIS_DATA/Databases} directory or the directory given by
     * the {@code derby.system.home} property.
     *
     * <h4>Dependency note</h4>
     * This field is public for the needs of {@code org.apache.sis.referencing.database} module.
     */
    public static final String DATABASE = "SpatialMetadata";

    /**
     * Name of the JNDI resource to lookup in the {@code "java:comp/env"} context.
     */
    public static final String JNDI = "jdbc/" + DATABASE;

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
     * {@code true} if {@link #connected(DatabaseMetaData, Class, String)} has been invoked at least once.
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
     * Returns initializers found on the module path.
     *
     * @return all registered initializers.
     */
    public static ServiceLoader<Initializer> load() {
        try {
            return ServiceLoader.load(Initializer.class, Reflect.getContextClassLoader());
        } catch (SecurityException e) {
            Reflect.log(Initializer.class, "load", e);
            return ServiceLoader.load(Initializer.class);
        }
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
         * This method forgets the data source and unregisters the listener from the JNDI context.
         * Note that there is no need to shutdown a Derby or HDQLDB engine since this shutdown is
         * only for {@link DataSource} obtained from JNDI context, in which case shuting down the
         * database engine should be container job.
         *
         * @see Initializer#shutdown()
         */
        @Override
        public Object call() throws NamingException {
            synchronized (Initializer.class) {
                source = null;
                connected = false;
                context.removeNamingListener(this);
            }
            return null;
        }

        /**
         * Invoked when the data source associated to {@code "jdbc/SpatialMetadata"} changed.
         * This method clears the {@link Initializer#source}, unregisters this listener (a new one
         * will be registered later if a new data source is created) and notifies other SIS modules.
         *
         * @param  event  ignored. Can be null.
         */
        @Override
        public void objectChanged(NamingEvent event) {
            Shutdown.unregister(this);
            try {
                call();
            } catch (NamingException e) {
                /*
                 * Not a fatal error since the listener may be unregistered anyway, or may be unregistered
                 * automatically by other kinds of JNDI events. Even if the listener is not unregistered,
                 * it will not hurt too badly: the DataSource would only be fetched more often than necessary.
                 */
                Logging.recoverableException(SystemListener.LOGGER, Listener.class, "objectChanged", e);
            }
            for (Initializer init : load()) {
                init.dataSourceChanged();
            }
        }

        /**
         * Invoked if JNDI lost connection to the server while preparing the {@code NamingEvent}.
         * Clears the data source anyway. In the worst case scenario, the application will fetch
         * it again from the JNDI context.
         */
        @Override
        public void namingExceptionThrown(NamingExceptionEvent event) {
            Logging.unexpectedException(SystemListener.LOGGER,
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
     * <p>{@code Initializer} will not register any shutdown process for user supplied data source.
     * We presume that database life cycle is managed by the caller.</p>
     *
     * @param  ds  supplier of data source to set, or {@code null} for removing previous supplier.
     *             This supplier may return {@code null}, in which case it will be ignored.
     * @return whether the given data source supplier has been successfully set.
     *
     * @see org.apache.sis.setup.Configuration#setDatabase(Supplier)
     */
    @Configuration(writeAccess = Configuration.Access.GLOBAL)
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
     * <ol class="verbose">
     *   <li>If a JNDI context exists, use the data source registered under the {@code "jdbc/SpatialMetadata"} name.</li>
     *   <li>Otherwise if a default data source {@linkplain #setDefault has been supplied}, use that data source.</li>
     *   <li>Otherwise if the {@code SIS_DATA} environment variable is defined,
     *       use the data source for {@code "jdbc:derby:$SIS_DATA/Databases/SpatialMetadata"}.
     *       That database will be created if it does not exist. Note that this is the only case where
     *       Apache SIS may create the database since it is located in the directory managed by Apache SIS.</li>
     *   <li>Otherwise if the {@code org.apache.sis.referencing.database} module is present on the module path,
     *       use the embedded database.</li>
     *   <li>Otherwise if the {@code "derby.system.home"} property is defined,
     *       use the data source for {@code "jdbc:derby:SpatialMetadata"}.
     *       This database will <strong>not</strong> be created if it does not exist.</li>
     *   <li>Otherwise (no JNDI, no environment variable, no Derby property set), {@code null}.</li>
     * </ol>
     *
     * The Derby database may be replaced by a HSQLDB database in above steps.
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
     *
     * @see org.apache.sis.setup.Configuration#getDatabase()
     */
    public static synchronized DataSource getDataSource() throws Exception {
        if (source == null) {
            if (hasJNDI()) try {
                final Context env = (Context) InitialContext.doLookup("java:comp/env");
                if (env != null) {
                    source = (DataSource) env.lookup(JNDI);
                    if (env instanceof EventContext) {
                        Listener.register((EventContext) env);
                    }
                    return source;
                    /*
                     * No Derby shutdown hook for DataSource fetched from JNDI.
                     * We presume that shutdowns are handled by the container.
                     * We do not clear the `supplier` field in case `source`
                     * is cleaned by the listener.
                     */
                }
            } catch (NoInitialContextException | NameNotFoundException e) {
                final LogRecord record = Messages.forLocale(null).createLogRecord(
                        Level.CONFIG, Messages.Keys.JNDINotSpecified_1, JNDI);

                // The null arguments let `Logging` infers the public caller.
                Logging.completeAndLog(LocalDataSource.LOGGER, null, null, record);
            }
            /*
             * At this point we determined that there is no JNDI context or no object binded to "jdbc/SpatialMetadata".
             * Check for programmatically supplied data source. We verify only after JNDI in order to let users control
             * their data source if desired. We do not provide shutdown hook for user supplied data source; we presume
             * that users manage themselves their database life cycle.
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
             * Only if the SIS_DATA environment variable is not set, verify first if the `sis-embedded-data` module is
             * on the module path. Note that if SIS_DATA is defined and valid, it has precedence.
             */
            DataSource        embedded   = null;
            LocalDataSource[] candidates = null;
            final boolean     isEnvClear = DataDirectory.isUndefined();
            if (isEnvClear) {
                embedded = embedded();                  // Check embedded data first only if SIS_DATA is not defined.
            }
            if (embedded == null) {
                candidates = LocalDataSource.create(DATABASE, Dialect.DERBY, Dialect.HSQL);     // Null or non-empty.
                if (!isEnvClear && (candidates == null || candidates[0].create)) {
                    // Check for embedded data only if not already checked and if no local database already exists.
                    embedded = embedded();
                }
            }
            if (embedded != null) {
                source = LocalDataSource.wrap(embedded);
            } else if (candidates != null) {
                source = LocalDataSource.findDriver(candidates);
            } else {
                return null;
            }
            supplier = null;        // Not needed anymore.
            /*
             * Register the shutdown hook before to attempt any operation on the database in order to close
             * it properly if the schemas creation below fail.
             */
            if (source.isWrapperFor(LocalDataSource.class)) {
                Shutdown.register(() -> {
                    shutdown();
                    return null;
                });
            }
            /*
             * If the database does not exist, create it. We allow creation only if we are inside
             * the $SIS_DATA directory. The Java code creating the schemas is provided in other
             * SIS modules. For example, `org.apache.sis.referencing` may create the EPSG dataset.
             */
            if (source instanceof LocalDataSource) {
                ((LocalDataSource) source).createDatabase();
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
                System.getProperty(Context.INITIAL_CONTEXT_FACTORY) != null;
    }

    /**
     * If the {@code org.apache.sis.referencing.database} module is present on the module path,
     * returns the data source for embedded Derby database. Otherwise returns {@code null}.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-337">SIS-337</a>
     */
    private static DataSource embedded() {
        for (InstallationResources res : InstallationResources.load()) {
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
                Logging.unexpectedException(LocalDataSource.LOGGER, Initializer.class, "getDataSource", e);
                // Continue - the system will fallback on the hard-coded subset of EPSG definitions.
            }
        }
        return null;
    }

    /**
     * Logs a record saying that a connection to the spatial metadata database has been created.
     * This method can be invoked after {@link DataSource#getConnection()}. When invoked for the first time,
     * the record level is set to {@link Level#CONFIG}. On next calls, the level become {@link Level#FINE}.
     *
     * @param  metadata  the value of {@code DataSource.getConnection().getMetaData()} or equivalent.
     * @param  classe    the class to report as the source of the logging message.
     * @param  method    the method to report as the source of the logging message.
     * @throws SQLException if an error occurred while fetching the database URL.
     */
    public static void connected(final DatabaseMetaData metadata, final Class<?> classe, final String method)
            throws SQLException
    {
        final Level level;
        synchronized (Initializer.class) {
            level = connected ? Level.FINE : Level.CONFIG;
            connected = true;
        }
        final LogRecord record = Messages.forLocale(null).createLogRecord(level,
                Messages.Keys.ConnectedToGeospatialDatabase_1, SQLUtilities.getSimplifiedURL(metadata));
        Logging.completeAndLog(SystemListener.LOGGER, classe, method, record);
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
        final Messages resources = Messages.forLocale(locale);
        return asLog ? resources.createLogRecord(Level.WARNING, key, value) : resources.getString(key, value);
    }

    /**
     * Invoked when the JVM is shutting down, or when the Servlet or OSGi bundle is uninstalled.
     * This method shutdowns the Derby database.
     *
     * @throws ReflectiveOperationException if an error occurred while
     *         setting the shutdown property on the Derby data source.
     * @throws SQLException if call to {@link DataSource#unwrap(Class)} failed.
     *         This exception should never happen since {@link #source} should always be an instance of
     *         {@link LocalDataSource} when this method is invoked, and {@link SQLException} thrown by
     *         the database are not propagated here.
     */
    private static synchronized void shutdown() throws ReflectiveOperationException, SQLException {
        final DataSource ds = source;
        source    = null;                       // Clear now in case of failure in remaining code.
        connected = false;
        ds.unwrap(LocalDataSource.class).shutdown();
    }
}
