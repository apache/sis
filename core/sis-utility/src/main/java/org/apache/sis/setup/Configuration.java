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

import java.util.Optional;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.function.Supplier;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.util.MetadataServices;


/**
 * Provides system-wide configuration for Apache SIS library.
 * Methods in this class can be used for overriding SIS default values.
 * Those methods can be used in final applications, but should not be used by libraries
 * in order to avoid interfering with user's settings.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class Configuration {
    /**
     * The default configuration instance. We use instances instead than static methods in case we want
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
     *   <li>Otherwise if the {@code non-free:sis-embedded-data} module is present on the classpath,
     *       use the embedded database.</li>
     *   <li>Otherwise if the {@code "derby.system.home"} property is defined,
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
     * control their data source.
     *
     * <p>This method can be invoked only before the first attempt to {@linkplain #getDatabase() get the database}.
     * If the {@link DataSource} has already be obtained, then this method throws {@link IllegalStateException}.</p>
     *
     * @param  source  supplier of data source to set.
     *         The supplier may return {@code null}, in which case it will be ignored.
     * @throws IllegalStateException if {@link DataSource} has already be obtained before this method call.
     */
    public void setDatabase(final Supplier<DataSource> source) {
        ArgumentChecks.ensureNonNull("source", source);
        MetadataServices.getInstance().setDataSource(source);
    }
}
