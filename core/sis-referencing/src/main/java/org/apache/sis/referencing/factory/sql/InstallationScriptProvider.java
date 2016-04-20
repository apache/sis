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

import java.util.Set;
import java.util.Locale;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.sql.Connection;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.setup.InstallationResources;
import org.apache.sis.internal.system.DataDirectory;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.Constants;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.StandardCharsets;
import org.apache.sis.internal.jdk7.DirectoryStream;
import org.apache.sis.internal.jdk7.Files;
import org.apache.sis.internal.jdk7.Path;


/**
 * Provides SQL scripts needed for creating a local copy of a dataset. This class allows Apache SIS users
 * to bundle the EPSG or other datasets in their own product for automatic installation when first needed.
 * Implementations of this class can be declared in the following file for automatic discovery by {@link EPSGFactory}:
 *
 * {@preformat text
 *     META-INF/services/org.apache.sis.setup.InstallationResources
 * }
 *
 * <div class="section">How this class is used</div>
 * The first time that an {@link EPSGDataAccess} needs to be instantiated,
 * {@link EPSGFactory} verifies if the EPSG database exists. If it does not, then:
 * <ol>
 *   <li>{@link EPSGFactory#install(Connection)} searches for the first instance of {@link InstallationResources}
 *       (the parent of this class) for which the {@linkplain #getAuthorities() set of authorities} contains {@code "EPSG"}.</li>
 *   <li>The {@linkplain #getLicense license} may be shown to the user if the application allows that
 *       (for example when running as a {@linkplain org.apache.sis.console console application}).</li>
 *   <li>If the installation process is allowed to continue, it will iterate over all readers provided by
 *       {@link #openScript(String, int)} and execute the SQL statements (not necessarily verbatim;
 *       the installation process may adapt to the target database).</li>
 * </ol>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class InstallationScriptProvider extends InstallationResources {
    /**
     * A sentinel value for the content of the script to execute before the SQL scripts provided by the authority.
     * This is an Apache SIS build-in script for constraining the values of some {@code VARCHAR} columns
     * to enumerations of values recognized by {@link EPSGDataAccess}. Those enumerations are not required
     * for proper working of {@link EPSGFactory}, but can improve data integrity.
     */
    protected static final String PREPARE = "Prepare";

    /**
     * A sentinel value for the content of the script to execute after the SQL scripts provided by the authority.
     * This is an Apache SIS build-in script for creating indexes or performing any other manipulation that help
     * SIS to use the dataset. Those indexes are not required for proper working of {@link EPSGFactory},
     * but can significantly improve performances.
     */
    protected static final String FINISH = "Finish";

    /**
     * The authorities to be returned by {@link #getAuthorities()}.
     */
    private final Set<String> authorities;

    /**
     * The names of the SQL scripts to read.
     */
    private final String[] resources;

    /**
     * Creates a new provider which will read script files of the given names in that order.
     * The given names are often filenames, but not necessarily
     * (it is okay to use those names only as labels).
     *
     * <table class="sis">
     *   <caption>Typical argument values</caption>
     *   <tr>
     *     <th>Authority</th>
     *     <th class="sep">Argument values</th>
     *   </tr><tr>
     *     <td>{@code EPSG}</td>
     *     <td class="sep"><code>
     *       {{@linkplain #PREPARE}, "EPSG_Tables.sql", "EPSG_Data.sql", "EPSG_FKeys.sql", {@linkplain #FINISH}}
     *     </code></td>
     *   </tr>
     * </table>
     *
     * @param authority The authority (typically {@code "EPSG"}), or {@code null} if not available.
     * @param resources Names of the SQL scripts to read.
     *
     * @see #getResourceNames(String)
     * @see #openStream(String)
     */
    protected InstallationScriptProvider(final String authority, final String... resources) {
        ArgumentChecks.ensureNonNull("resources", resources);
        authorities = CollectionsExt.singletonOrEmpty(authority);
        this.resources = resources;
    }

    /**
     * Returns the identifiers of the dataset installed by the SQL scripts.
     * The values currently recognized by SIS are:
     *
     * <ul>
     *   <li>{@code "EPSG"} for the EPSG geodetic dataset.</li>
     * </ul>
     *
     * The default implementation returns the authority given at construction time, or an empty set
     * if that authority was {@code null}. An empty set means that the provider does not have all
     * needed resources or does not have permission to distribute the installation scripts.
     *
     * @return Identifiers of SQL scripts that this instance can distribute.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Set<String> getAuthorities() {
        return authorities;
    }

    /**
     * Verifies that the given authority is one of the expected values.
     */
    private void verifyAuthority(final String authority) {
        if (!authorities.contains(authority)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "authority", authority));
        }
    }

    /**
     * Returns the names of all SQL scripts to execute.
     * This is a copy of the array of names given to the constructor.
     * Those names are often filenames, but not necessarily (they may be just labels).
     *
     * @param  authority The value given at construction time (e.g. {@code "EPSG"}).
     * @return The names of all SQL scripts to execute.
     * @throws IllegalArgumentException if the given {@code authority} argument is not the expected value.
     * @throws IOException if fetching the script names required an I/O operation and that operation failed.
     */
    @Override
    public String[] getResourceNames(String authority) throws IOException {
        verifyAuthority(authority);
        return resources.clone();
    }

    /**
     * Returns a reader for the SQL script at the given index. Contents may be read from files in a local directory,
     * or from resources in a JAR file, or from entries in a ZIP file, or any other means at implementor choice.
     * The {@link BufferedReader} instances shall be closed by the caller.
     *
     * <div class="section">EPSG case</div>
     * In the EPSG dataset case, the iterator should return {@code BufferedReader} instances for the following files
     * (replace {@code <version>} by the EPSG version number and {@code <product>} by the target database) in same order.
     * The first and last files are provided by Apache SIS.
     * All other files can be downloaded from <a href="http://www.epsg.org/">http://www.epsg.org/</a>.
     *
     * <ol>
     *   <li>Content of {@link #PREPARE}, an optional data definition script that define the enumerations expected by {@link EPSGDataAccess}.</li>
     *   <li>Content of {@code "EPSG_<version>.mdb_Tables_<product>.sql"}, a data definition script that create empty tables.</li>
     *   <li>Content of {@code "EPSG_<version>.mdb_Data_<product>.sql"}, a data manipulation script that populate the tables.</li>
     *   <li>Content of {@code "EPSG_<version>.mdb_FKeys_<product>.sql"}, a data definition script that create foreigner key constraints.</li>
     *   <li>Content of {@link #FINISH}, an optional data definition and data control script that create indexes and set permissions.</li>
     * </ol>
     *
     * Implementors are free to return a different set of scripts with equivalent content.
     *
     * <div class="section">Default implementation</div>
     * The default implementation invokes {@link #openStream(String)} – except for {@link #PREPARE} and {@link #FINISH}
     * in which case an Apache SIS build-in script is used – and wrap the result in a {@link LineNumberReader}.
     * The file encoding is ISO LATIN-1 (the encoding used in the scripts distributed by EPSG).
     *
     * @param  authority The value given at construction time (e.g. {@code "EPSG"}).
     * @param  resource Index of the SQL script to read, from 0 inclusive to
     *         <code>{@linkplain #getResourceNames getResourceNames}(authority).length</code> exclusive.
     * @return A reader for the content of SQL script to execute.
     * @throws IllegalArgumentException if the given {@code authority} argument is not the expected value.
     * @throws IndexOutOfBoundsException if the given {@code resource} argument is out of bounds.
     * @throws IOException if an error occurred while creating the reader.
     */
    @Override
    public BufferedReader openScript(final String authority, final int resource) throws IOException {
        verifyAuthority(authority);
        ArgumentChecks.ensureValidIndex(resources.length, resource);
        if (!Constants.EPSG.equals(authority)) {
            throw new IllegalStateException(Errors.format(Errors.Keys.UnknownAuthority_1, authority));
        }
        String name = resources[resource];
        final Charset charset;
        final InputStream in;
        if (PREPARE.equals(name) || FINISH.equals(name)) {
            name = authority + '_' + name + ".sql";
            in = InstallationScriptProvider.class.getResourceAsStream(name);
            charset = StandardCharsets.UTF_8;
        } else {
            in = openStream(name);
            charset = StandardCharsets.ISO_8859_1;
        }
        if (in == null) {
            throw new FileNotFoundException(Errors.format(Errors.Keys.FileNotFound_1, name));
        }
        return new LineNumberReader(new InputStreamReader(in, charset));
    }

    /**
     * Opens the input stream for the SQL script of the given name.
     * This method is invoked by the default implementation of {@link #openScript(String, int)}
     * for all scripts except {@link #PREPARE} and {@link #FINISH}.
     *
     * <div class="note"><b>Example 1:</b>
     * if this {@code InstallationScriptProvider} instance gets the SQL scripts from files in a well-known directory
     * and if the names given at {@linkplain #InstallationScriptProvider(String, String...) construction time} are the
     * filenames in that directory, then this method can be implemented as below:
     *
     * {@preformat java
     *    protected InputStream openStream(String name) throws IOException {
     *        return Files.newInputStream(directory.resolve(name));
     *    }
     * }
     * </div>
     *
     * <div class="note"><b>Example 2:</b>
     * if this {@code InstallationScriptProvider} instance rather gets the SQL scripts from resources bundled
     * in the same JAR files than and in the same package, then this method can be implemented as below:
     *
     * {@preformat java
     *    protected InputStream openStream(String name) {
     *        return MyClass.getResourceAsStream(name);
     *    }
     * }
     * </div>
     *
     * @param  name Name of the script file to open. Can be {@code null} if the resource is not found.
     * @return An input stream opened of the given script file.
     * @throws IOException if an error occurred while opening the file.
     */
    protected abstract InputStream openStream(final String name) throws IOException;

    /**
     * Logs the given record. This method pretend that the record has been logged by
     * {@code EPSGFactory.install(…)} because it is the public API using this class.
     */
    static void log(final LogRecord record) {
        record.setLoggerName(Loggers.CRS_FACTORY);
        Logging.log(EPSGFactory.class, "install", record);
    }




    /**
     * The default implementation which use the scripts in the {@code $SIS_DATA/Databases/ExternalSources}
     * directory, if present. This class expects the files to have those exact names where {@code *} stands
     * for any characters provided that there is no ambiguity:
     *
     * <ul>
     *   <li>{@code EPSG_*Tables.sql}</li>
     *   <li>{@code EPSG_*Data.sql}</li>
     *   <li>{@code EPSG_*FKeys.sql}</li>
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
        private Path directory;

        /**
         * Index of the first real file in the array given to the constructor.
         * We set the value to 1 for skipping the {@code PREPARE} pseudo-file.
         */
        private static final int FIRST_FILE = 1;

        /**
         * Creates a default provider.
         */
        Default(final Locale locale) throws IOException {
            super(Constants.EPSG,
                    PREPARE,
                    "Tables",
                    "Data",
                    "FKeys",
                    FINISH);

            Path dir = DataDirectory.DATABASES.getDirectory();
            if (dir != null) {
                dir = dir.resolve("ExternalSources");
                if (Files.isDirectory(dir)) {
                    final String[] resources = super.resources;
                    final String[] found = new String[resources.length - FIRST_FILE - 1];
                    DirectoryStream stream = Files.newDirectoryStream(dir, "EPSG_*.sql");
                    try {
                        for (final Path path : stream) {
                            final String name = path.getFileName().toString();
                            for (int i=0; i<found.length; i++) {
                                final String part = resources[FIRST_FILE + i];
                                if (name.contains(part)) {
                                    if (found[i] != null) {
                                        log(Errors.getResources(locale)
                                                  .getLogRecord(Level.WARNING, Errors.Keys.DuplicatedElement_1, part));
                                        return;                         // Stop the search because of duplicated file.
                                    }
                                    found[i] = name;
                                }
                            }
                        }
                    } finally {
                        stream.close();
                    }
                    for (int i=0; i<found.length; i++) {
                        final String file = found[i];
                        if (file != null) {
                            resources[FIRST_FILE + i] = file;
                        } else {
                            dir = null;
                        }
                    }
                    directory = dir;
                }
            }
        }

        /**
         * Returns {@code "EPSG"} if the scripts exist in the {@code ExternalSources} subdirectory,
         * or {@code "unavailable"} otherwise.
         *
         * @return {@code "EPSG"} if the SQL scripts for installing the EPSG dataset are available,
         *         or {@code "unavailable"} otherwise.
         */
        @Override
        public Set<String> getAuthorities() {
            return (directory != null) ? super.getAuthorities() : Collections.<String>emptySet();
        }

        /**
         * Returns {@code null} since the user is presumed to have downloaded the files himself.
         *
         * @return The terms of use in plain text or HTML, or {@code null} if the license is presumed already accepted.
         */
        @Override
        public String getLicense(String authority, Locale locale, String mimeType) {
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
        protected InputStream openStream(final String name) throws IOException {
            return (directory != null) ? Files.newInputStream(directory.resolve(name)) : null;
        }
    }
}
