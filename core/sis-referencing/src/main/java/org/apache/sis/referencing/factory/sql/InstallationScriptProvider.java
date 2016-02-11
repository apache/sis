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
package org.apache.sis.referencing.factory.sql;

import java.sql.Connection;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.system.DataDirectory;

// Branch-dependent imports
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Provides SQL scripts needed for creating a copy of the EPSG dataset. This interface allows Apache SIS
 * users to bundle the EPSG dataset in their own product for automatic installation when first needed.
 * That dataset is not included directly in Apache SIS for
 * <a href="https://issues.apache.org/jira/browse/LEGAL-183">licensing reasons</a>.
 *
 * <p>Implementations of this interface can be declared in the following file for automatic discovery
 * by {@link EPSGFactory} (see {@link java.util.ServiceLoader} for more information):</p>
 *
 * {@preformat text
 *     META-INF/services/org.apache.sis.referencing.factory.sql.InstallationScriptProvider
 * }
 *
 * <div class="section">How this interface is used</div>
 * The first time that an {@link EPSGDataAccess} needs to be instantiated, {@link EPSGFactory} verifies
 * if the EPSG database exists. If it does not, then:
 * <ol>
 *   <li>{@link EPSGFactory#install(Connection)} searches for the first {@code InstallationScriptProvider} instance
 *       for which {@link #getAuthority()} returns {@code "EPSG"}.</li>
 *   <li>The {@linkplain #getLicense license} may be shown to the user if the application allows that
 *       (for example when running as a {@linkplain org.apache.sis.console console application}).</li>
 *   <li>If the installation process is allowed to continue, it will iterate over all readers provided by
 *       {@link #getScriptContent(int)} and execute the SQL statements (not necessarily verbatim;
 *       the installation process may adapt to the target database).</li>
 * </ol>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class InstallationScriptProvider {
    /**
     * A sentinel value for the content of the script to execute after the official EPSG scripts.
     * This is an Apache SIS build-in script for creating indexes or performing any other manipulation
     * that help SIS to use the EPSG dataset. Those indexes are not required for proper working of
     * {@link EPSGFactory}, but can significantly improve performances.
     */
    protected static final String POST_CREATE = "(post create)";

    /**
     * The names of the SQL scripts to read.
     */
    private final String[] names;

    /**
     * Creates a new provider which will read script files of the given names in that order.
     * The given names are often filenames, but not necessarily
     * (it is okay to use those names only as labels).
     *
     * <p>For the EPSG dataset, the {@code names} argument is usually
     * (potentially completed with EPSG dataset version and database software name):</p>
     *
     * <blockquote><code>
     *   "EPSG_Tables.sql", "EPSG_Data.sql", "EPSG_FKeys.sql", {@linkplain #POST_CREATE}
     * </code></blockquote>
     *
     * @param names Names of the SQL scripts to read.
     *
     * @see #getScriptNames()
     * @see #open(String)
     */
    protected InstallationScriptProvider(final String... names) {
        ArgumentChecks.ensureNonNull("filenames", names);
        this.names = names;
    }

    /**
     * Returns the identifier of the dataset installed by the SQL scripts, or {@code "unavailable"}
     * if the SQL scripts are not available.
     *
     * <p>Currently, the only allowed return values are {@code "EPSG"} and {@code "unavailable"}.
     * This list may be expanded in future SIS versions if more authorities are supported.</p>
     *
     * @return {@code "EPSG"} if the SQL scripts for installing the EPSG dataset are available,
     *         or {@code "unavailable"} otherwise.
     */
    public abstract String getAuthority();

    /**
     * Returns the terms of use of the dataset, or {@code null} if presumed already accepted.
     * The terms of use can be returned in either plain text or HTML.
     *
     * <p>For the EPSG dataset, this method should return the content of the
     * <a href="http://www.epsg.org/TermsOfUse">http://www.epsg.org/TermsOfUse</a> page.</p>
     *
     * @param  mimeType Either {@code "text/plain"} or {@code "text/html"}.
     * @return The terms of use in plain text or HTML, or {@code null} if the license is presumed already accepted.
     * @throws IOException if an error occurred while reading the license file.
     */
    public abstract InternationalString getLicense(String mimeType) throws IOException;

    /**
     * Returns the names of all SQL scripts to execute.
     * Those names are often filenames, but not necessarily (they may be just labels).
     *
     * @return The names of all SQL scripts to execute.
     */
    public String[] getScriptNames() {
        return names.clone();
    }

    /**
     * Returns a reader for the SQL script at the given index. Contents may be read from files in a local directory,
     * or from resources in a JAR file, or from entries in a ZIP file, or any other means at implementor choice.
     * The {@link BufferedReader} instances shall be closed by the caller.
     *
     * <div class="section">EPSG case</div>
     * In the EPSG dataset case, the iterator should return {@code BufferedReader} instances for the following files
     * (replace {@code <version>} by the EPSG version number and {@code <product>} by the target database) in same order.
     * The first 3 files can be downloaded from <a href="http://www.epsg.org/">http://www.epsg.org/</a>.
     * The fourth file is provided by Apache SIS.
     *
     * <ol>
     *   <li>Content of {@code "EPSG_<version>.mdb_Tables_<product>.sql"}, a data definition script that create empty tables.</li>
     *   <li>Content of {@code "EPSG_<version>.mdb_Data_<product>.sql"}, a data manipulation script that populate the tables.</li>
     *   <li>Content of {@code "EPSG_<version>.mdb_FKeys_<product>.sql"}, a data definition script that create foreigner key constraints.</li>
     *   <li>Content of {@link #POST_CREATE}, a data definition and data control script that create indexes and set permissions.</li>
     * </ol>
     *
     * Implementors are free to return a different set of scripts with equivalent content.
     *
     * <div class="section">Default implementation</div>
     * The default implementation invokes {@link #open(String)} – except for {@link #POST_CREATE} in which case
     * an Apache SIS build-in script is used – and wrap the result in a {@link LineNumberReader}.
     *
     * @param  index Index of the SQL script to read, from 0 inclusive to
     *         <code>{@linkplain #getScriptNames()}.length</code> exclusive.
     * @return A reader for the content of SQL script to execute.
     * @throws IOException if an error occurred while creating the reader.
     */
    public BufferedReader getScriptContent(final int index) throws IOException {
        ArgumentChecks.ensureValidIndex(names.length, index);
        final String authority = getAuthority();
        if (!Constants.EPSG.equals(authority)) {
            throw new IllegalStateException(Errors.format(Errors.Keys.UnknownAuthority_1, authority));
        }
        String name = names[index];
        final Charset charset;
        final InputStream in;
        if (POST_CREATE.equals(name)) {
            name = authority.concat(".sql");
            in = InstallationScriptProvider.class.getResourceAsStream(name);
            charset = StandardCharsets.UTF_8;
        } else {
            in = open(name);
            charset = StandardCharsets.ISO_8859_1;
        }
        if (in == null) {
            throw new FileNotFoundException(Errors.format(Errors.Keys.FileNotFound_1, name));
        }
        return new LineNumberReader(new InputStreamReader(in, charset));
    }

    /**
     * Opens the input stream for the SQL script of the given name.
     * This method is invoked by the default implementation of {@link #getScriptContent(int)}
     * for all scripts except {@link #POST_CREATE}.
     *
     * <div class="note"><b>Examples:</b>
     * If this {@code InstallationScriptProvider} instance gets the SQL scripts from files in a well-known directory
     * and if the names given at {@linkplain #InstallationScriptProvider(String...) construction time} are the
     * filenames in that directory, then this method can be implemented as below:
     *
     * {@preformat java
     *    protected InputStream open(String name) throws IOException {
     *        return Files.newInputStream(directory.resolve(name));
     *    }
     * }
     *
     * If this {@code InstallationScriptProvider} instance rather gets the SQL scripts from resources bundled
     * in the same JAR files than and in the same package, then this method can be implemented as below:
     *
     * {@preformat java
     *    protected InputStream open(String name) {
     *        return MyClass.getResourceAsStream(name);
     *    }
     * }
     * </div>
     *
     * @param  name Name of the script file to open.
     * @return An input stream opened of the given script file.
     * @throws IOException if an error occurred while opening the file.
     */
    protected abstract InputStream open(final String name) throws IOException;




    /**
     * The default implementation which use the scripts in the {@code $SIS_DATA/Databases/ExternalSources}
     * directory, if present. This class expects the files to have those exact names:
     *
     * <ul>
     *   <li>{@code EPSG_Tables.sql}</li>
     *   <li>{@code EPSG_Data.sql}</li>
     *   <li>{@code EPSG_FKeys.sql}</li>
     * </ul>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    static final class Default extends InstallationScriptProvider {
        /**
         * The directory containing the scripts, or {@code null} if it does not exist.
         */
        private final Path directory;

        /**
         * Creates a default provider.
         */
        public Default() {
            super("EPSG_Tables.sql",
                  "EPSG_Data.sql",
                  "EPSG_FKeys.sql",
                  POST_CREATE);

            Path dir = DataDirectory.DATABASES.getDirectory();
            if (dir != null) {
                dir = dir.resolve("ExternalSources");
                if (!Files.isRegularFile(dir.resolve("EPSG_Tables.sql"))) {
                    dir = null;
                }
            }
            directory = dir;
        }

        /**
         * Returns {@code "EPSG"} if the scripts exist in the {@code ExternalSources} subdirectory,
         * or {@code "unavailable"} otherwise.
         *
         * @return {@code "EPSG"} if the SQL scripts for installing the EPSG dataset are available,
         *         or {@code "unavailable"} otherwise.
         */
        @Override
        public String getAuthority() {
            return (directory != null) ? Constants.EPSG : "unavailable";
        }

        /**
         * Returns {@code null} since the user is presumed to have downloaded the files himself.
         *
         * @return The terms of use in plain text or HTML, or {@code null} if the license is presumed already accepted.
         */
        @Override
        public InternationalString getLicense(String mimeType) {
            return null;
        }

        /**
         * Opens the input stream for the SQL script of the given name.
         *
         * @param  name Name of the script file to open.
         * @return An input stream opened of the given script file, or {@code null} if the resource was not found.
         * @throws IOException if an error occurred while opening the file.
         */
        @Override
        protected InputStream open(final String name) throws IOException {
            return Files.newInputStream(directory.resolve(name));
        }
    }
}
