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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.sql.Connection;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.setup.InstallationResources;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.system.Fallback;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.util.privy.Constants;


/**
 * Provides SQL scripts needed for creating a local copy of a dataset. This class allows Apache SIS users
 * to bundle the EPSG or other datasets in their own product for automatic installation when first needed.
 * Implementations of this class are discovered automatically by {@link EPSGFactory} if they are declared
 * in {@code module-info.java} as providers of the {@code org.apache.sis.setup.InstallationResources} service.
 *
 * <h2>How this class is used</h2>
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
 * @version 1.5
 * @since   0.7
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
     * @param  authority  the authority (typically {@code "EPSG"}), or {@code null} if not available.
     * @param  resources  names of the SQL scripts to read.
     *
     * @see #getResourceNames(String)
     * @see #openStream(String)
     */
    protected InstallationScriptProvider(final String authority, final String... resources) {
        this.resources = Objects.requireNonNull(resources);
        authorities = CollectionsExt.singletonOrEmpty(authority);
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
     * @return identifiers of SQL scripts that this instance can distribute.
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
     * @param  authority  the value given at construction time (e.g. {@code "EPSG"}).
     * @return the names of all SQL scripts to execute.
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
     * or from resources in a JAR file, or from entries in a ZIP file, or any other means at implementer choice.
     * The {@link BufferedReader} instances shall be closed by the caller.
     *
     * <h4>EPSG case</h4>
     * In the EPSG dataset case, the iterator should return {@code BufferedReader} instances for the following files
     * (replace {@code <version>} by the EPSG version number and {@code <product>} by the target database) in same order.
     * The first and last files are provided by Apache SIS.
     * All other files can be downloaded from <a href="https://epsg.org/">https://epsg.org/</a>.
     *
     * <ol>
     *   <li>Content of {@link #PREPARE}, an optional data definition script that define the enumerations expected by {@link EPSGDataAccess}.</li>
     *   <li>Content of {@code "EPSG_<version>.mdb_Tables_<product>.sql"}, a data definition script that create empty tables.</li>
     *   <li>Content of {@code "EPSG_<version>.mdb_Data_<product>.sql"}, a data manipulation script that populate the tables.</li>
     *   <li>Content of {@code "EPSG_<version>.mdb_FKeys_<product>.sql"}, a data definition script that create foreigner key constraints.</li>
     *   <li>Content of {@link #FINISH}, an optional data definition and data control script that create indexes and set permissions.</li>
     * </ol>
     *
     * Implementers are free to return a different set of scripts with equivalent content.
     *
     * <h4>Default implementation</h4>
     * The default implementation invokes {@link #openStream(String)} – except for {@link #PREPARE} and {@link #FINISH}
     * in which case an Apache SIS build-in script is used – and wrap the result in a {@link LineNumberReader}.
     * The file encoding is UTF-8 (the encoding used in the scripts distributed by EPSG since version 9.4).
     *
     * @param  authority  the value given at construction time (e.g. {@code "EPSG"}).
     * @param  resource   index of the SQL script to read, from 0 inclusive to
     *         <code>{@linkplain #getResourceNames getResourceNames}(authority).length</code> exclusive.
     * @return a reader for the content of SQL script to execute.
     * @throws IllegalArgumentException if the given {@code authority} argument is not the expected value.
     * @throws IndexOutOfBoundsException if the given {@code resource} argument is out of bounds.
     * @throws FileNotFoundException if the SQL script of the given name has not been found.
     * @throws IOException if an error occurred while creating the reader.
     */
    @Override
    public BufferedReader openScript(final String authority, final int resource) throws IOException {
        verifyAuthority(authority);
        if (!Constants.EPSG.equals(authority)) {
            throw new IllegalStateException(Resources.format(Resources.Keys.UnknownAuthority_1, authority));
        }
        String name = resources[resource];
        InputStream in;
        NoSuchFileException cause = null;
        if (PREPARE.equals(name) || FINISH.equals(name)) {
            name = authority + '_' + name + ".sql";
            in = InstallationScriptProvider.class.getResourceAsStream(name);
        } else try {
            in = openStream(name);
        } catch (NoSuchFileException e) {
            cause = e;
            in = null;
        }
        if (in == null) {
            var e = new FileNotFoundException(Errors.format(Errors.Keys.FileNotFound_1, name));
            e.initCause(cause);
            throw e;
        }
        return new LineNumberReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /**
     * Opens the input stream for the SQL script of the given name.
     * This method is invoked by the default implementation of {@link #openScript(String, int)}
     * for all scripts except {@link #PREPARE} and {@link #FINISH}.
     * The returned input stream does not need to be buffered.
     *
     * <h4>Example 1</h4>
     * if this {@code InstallationScriptProvider} instance gets the SQL scripts from files in a well-known directory
     * and if the names given at {@linkplain #InstallationScriptProvider(String, String...) construction time} are the
     * filenames in that directory, then this method can be implemented as below:
     *
     * {@snippet lang="java" :
     *      protected InputStream openStream(String name) throws IOException {
     *          return Files.newInputStream(directory.resolve(name));
     *      }
     * }
     *
     * <h4>Example 2</h4>
     * if this {@code InstallationScriptProvider} instance rather gets the SQL scripts from resources bundled
     * in the same JAR files as and in the same package, then this method can be implemented as below:
     *
     * {@snippet lang="java" :
     *      protected InputStream openStream(String name) {
     *          return MyClass.getResourceAsStream(name);
     *      }
     * }
     *
     * @param  name  name of the script file to open. Can be {@code null} if the resource is not found.
     * @return an input stream opened of the given script file.
     * @throws IOException if an error occurred while opening the file.
     */
    protected abstract InputStream openStream(final String name) throws IOException;

    /**
     * Logs the given record. This method pretend that the record has been logged by
     * {@code EPSGFactory.install(…)} because it is the public API using this class.
     */
    static void log(final LogRecord record) {
        Logging.completeAndLog(EPSGDataAccess.LOGGER, EPSGFactory.class, "install", record);
    }




    /**
     * The default implementation which uses the scripts in the {@code $SIS_DATA/Databases/ExternalSources}
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
     */
    @Fallback
    static final class Default extends InstallationScriptProvider {
        /**
         * The directory containing the scripts, or {@code null} if it does not exist.
         */
        private final Path directory;

        /**
         * Index of the first real file in the array given to the constructor.
         * We set the value to 1 for skipping the {@code PREPARE} pseudo-file.
         */
        private static final int FIRST_FILE = 1;

        /**
         * Creates a default provider.
         *
         * @param locale  the locale for warning messages, if any.
         */
        Default(final Locale locale) throws IOException {
            super(Constants.EPSG,
                    PREPARE,
                    "Tables",
                    "Data",
                    "FKeys",
                    FINISH);

            final String[] resources = super.resources;
            final String[] found = new String[resources.length - (FIRST_FILE + 1)];
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            Path directory = DataDirectory.DATABASES.getDirectory();
            if (directory == null || Files.isDirectory(directory = directory.resolve("ExternalSources"))) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "EPSG_*.sql")) {
                    for (final Path path : stream) {
                        final String name = path.getFileName().toString();
                        for (int i=0; i<found.length; i++) {
                            final String part = resources[FIRST_FILE + i];
                            if (name.contains(part)) {
                                if (found[i] == null) {
                                    found[i] = name;
                                } else {
                                    log(Errors.forLocale(locale).createLogRecord(Level.WARNING,
                                            Errors.Keys.DuplicatedFileReference_1, part));
                                }
                            }
                        }
                    }
                }
            } else {
                directory = null;       // Does not exist or is not a directory.
            }
            this.directory = directory;
            /*
             * Store the actual file names including the target database and EPSG dataset version. Example:
             *
             *   - PostgreSQL_Table_Script.sql
             *   - PostgreSQL_Data_Script.sql
             *   - PostgreSQL_FKey_Script.sql
             *
             * If a file was not found, use the "EPSG_Foo_Script.sql" name pattern for giving some hints
             * to users about which file was expected. This filename will appear in the exception message.
             */
            for (int i=0; i<found.length; i++) {
                String file = found[i];
                if (file == null) {
                    file = "EPSG_" + resources[FIRST_FILE + i] + "_Script.sql";
                }
                resources[FIRST_FILE + i] = file;
            }
        }

        /**
         * Returns {@code "EPSG"} if the scripts exist in the {@code ExternalSources} subdirectory,
         * or an empty set otherwise.
         *
         * @return {@code "EPSG"} if the SQL scripts for installing the EPSG dataset are available,
         *         or an empty set otherwise.
         */
        @Override
        public Set<String> getAuthorities() {
            return (directory != null) ? super.getAuthorities() : Set.of();
        }

        /**
         * Returns {@code null} since the user is presumed to have downloaded the files himself.
         *
         * @return the terms of use in plain text or HTML, or {@code null} if the license is presumed already accepted.
         */
        @Override
        public String getLicense(String authority, Locale locale, String mimeType) {
            return null;
        }

        /**
         * Opens the input stream for the SQL script of the given name.
         * The returned input stream does not need to be buffered.
         *
         * @param  name  name of the script file to open.
         * @return an input stream opened of the given script file, or {@code null} if the resource was not found.
         * @throws IOException if an error occurred while opening the file.
         */
        @Override
        protected InputStream openStream(final String name) throws IOException {
            return (directory != null) ? Files.newInputStream(directory.resolve(name)) : null;
        }
    }
}
