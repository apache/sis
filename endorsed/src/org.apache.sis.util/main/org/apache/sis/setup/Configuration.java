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
package org.apache.sis.setup;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import java.sql.SQLException;
import org.apache.sis.system.Shutdown;
import org.apache.sis.system.SystemListener;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.internal.shared.MetadataServices;


/**
 * Provides system-wide configuration for Apache SIS library.
 * Methods in this class can be used for overriding SIS default values.
 * Those methods can be used in final applications, but should not be used by libraries
 * in order to avoid interfering with user's settings.
 *
 * <h2>Other system-wide configuration</h2>
 * The following methods have system-wide effects on Apache SIS configuration,
 * but are not yet controlled through this {@code Configuration} class:
 *
 * <ul>
 *   <li>{@link org.apache.sis.util.logging.MonolineFormatter#install()}</li>
 *   <li>{@link org.apache.sis.util.logging.PerformanceLevel#setMinDuration(long, TimeUnit)}</li>
 * </ul>
 *
 * The following properties are defined by the standard Java environment.
 * Apache SIS read those properties but does not modify them:
 *
 * <ul>
 *   <li>{@link Locale#getDefault()} (sometimes using {@link Locale.Category})</li>
 *   <li>{@link java.nio.charset.Charset#defaultCharset()}</li>
 *   <li>{@link java.util.TimeZone#getDefault()}</li>
 *   <li>{@link System#lineSeparator()}</li>
 *   <li>{@link java.io.File#pathSeparator}</li>
 *   <li>{@link java.io.File#separator}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.0
 */
public final class Configuration {
    /**
     * The default configuration instance. We use instances instead of static methods in case we want
     * different configuration modes in a future Apache SIS version (for example a "strict" mode versus
     * a "lenient" mode), of for allowing configurations to be saved in a file and restored.
     */
    private static final Configuration DEFAULT = new Configuration();

    /**
     * Do not allow instantiation except by methods in this class.
     */
    private Configuration() {
    }

    /**
     * Returns the current configuration.
     *
     * @return the current configuration.
     */
    public static Configuration current() {
        return DEFAULT;
    }

    /**
     * Returns the data source for the SIS-wide "SpatialMetadata" database.
     * This method returns the first of the following steps that succeed:
     *
     * <ol>
     *   <li>If a JNDI context exists, use the data source registered under the {@code "jdbc/SpatialMetadata"} name.</li>
     *   <li>Otherwise if a default data source {@linkplain #setDatabase has been supplied}, use that data source.</li>
     *   <li>Otherwise if the {@code SIS_DATA} environment variable is defined,
     *       use the data source for {@code "jdbc:derby:$SIS_DATA/Databases/SpatialMetadata"}.
     *       That database will be created if it does not exist. Note that this is the only case where
     *       Apache SIS may create the database since it is located in the directory managed by Apache SIS.</li>
     *   <li>Otherwise if the {@code org.apache.sis.referencing.database} module is present on the module path,
     *       use the embedded database.</li>
     *   <li>Otherwise if the "{@systemProperty derby.system.home}" property is defined,
     *       use the data source for {@code "jdbc:derby:SpatialMetadata"} database.
     *       This database will <strong>not</strong> be created if it does not exist.</li>
     * </ol>
     *
     * @return the data source for the {@code "SpatialMetadata"} database.
     * @throws SQLException if an error occurred while fetching the database source.
     */
    public Optional<DataSource> getDatabase() throws SQLException {
        return Optional.of(MetadataServices.getInstance().getDataSource());
    }

    /**
     * Specifies the data source to use if no {@code "jdbc/SpatialMetadata"} source is binded to a JNDI environment.
     * Data source specified by JNDI has precedence over data source specified by this method in order to let users
     * control their data source. The following example shows how to setup a connection to a PostgreSQL database:
     *
     * {@snippet lang="java" :
     *     import org.postgresql.ds.PGSimpleDataSource;
     *
     *     class MyClass {
     *         private static DataSource createDataSource() {
     *             PGSimpleDataSource ds = new PGSimpleDataSource();
     *             ds.setDatabaseName("SpatialMetadata");
     *             // Server default to "localhost".
     *             return ds;
     *         }
     *
     *         static initialize() {
     *             if (WANT_TO_CONFIGURE_JNDI) {
     *                 // Registration assuming that a JNDI implementation is available
     *                 Context env = (Context) InitialContext.doLookup("java:comp/env");
     *                 env.bind("jdbc/SpatialMetadata", createDataSource());
     *             }
     *
     *             // Fallback if there is no JNDI or no "SpatialMetadata" entry.
     *             Configuration.current().setDatabase(MyClass::createDataSource);
     *         }
     *     }
     *     }
     *
     * This method can be invoked only before the first attempt to {@linkplain #getDatabase() get the database}.
     * If the {@link DataSource} has already be obtained, then this method throws {@link IllegalStateException}.
     *
     * @param  source  supplier of data source to set.
     *         The supplier may return {@code null}, in which case it will be ignored.
     * @throws IllegalStateException if {@link DataSource} has already be obtained before this method call.
     *
     * @see <a href="https://sis.apache.org/epsg.html#jndi">How to use EPSG geodetic dataset</a>
     */
    public void setDatabase(final Supplier<DataSource> source) {
        MetadataServices.getInstance().setDataSource(Objects.requireNonNull(source));
    }

    /**
     * Shutdowns the Apache <abbr>SIS</abbr> library.
     * This method closes database connections and stops the daemon threads that were started by <abbr>SIS</abbr>.
     * <strong>The Apache <abbr>SIS</abbr> library shall not be used anymore after this method call.</strong>
     * Any use of Apache <abbr>SIS</abbr> after this method call may have unexpected effects.
     * In particular, it may cause memory leaks.
     *
     * <h4>When to use</h4>
     * This method should generally <strong>not</strong> be invoked, because Apache <abbr>SIS</abbr> registers
     * itself a {@linkplain Runtime#addShutdownHook(Thread) shutdown hook} to the Java Virtual Machine.
     * This method may be useful in embedded environments that do not allow the use of shutdown hooks,
     * or when waiting for the <abbr>JVM</abbr> shutdown is overly conservative.
     *
     * <h4>Complete shutdown</h4>
     * This method shutdowns only the databases used by Apache <abbr>SIS</abbr>.
     * If Apache Derby is used for the <abbr>EPSG</abbr> database, some Derby daemon threads may still be running.
     * Those daemons can be ignored in standalone applications, but may need to be stopped in embedded environments.
     * A complete Derby shutdown can be requested with the following code:
     *
     * {@snippet lang="java" :
     *     Configuration.current().shutdown();
     *     try {
     *         DriverManager.getConnection("jdbc:derby:;shutdown=true");
     *     } catch (SQLException e) {
     *         // Expected exception as per Derby documentation.
     *     }
     *     }
     *
     * @see <a href="https://db.apache.org/derby/docs/10.15/devguide/tdevdvlp40464.html">Shutting down Derby</a>
     *
     * @since 1.5
     */
    public void shutdown() {
        try {
            Shutdown.stop(Configuration.class);
        } catch (Exception e) {
            Logging.unexpectedException(SystemListener.LOGGER, Configuration.class, "stop", e);
        }
    }
}
