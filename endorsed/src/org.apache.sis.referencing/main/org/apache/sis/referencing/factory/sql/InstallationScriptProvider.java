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
import java.util.Objects;
import java.util.logging.LogRecord;
import java.sql.Connection;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.setup.InstallationResources;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.util.privy.Constants;


/**
 * Provides the <abbr>SQL</abbr> scripts needed for creating a local copy of a geodetic dataset.
 * This is used mostly for automatic installation of the <abbr>EPSG</abbr> dataset, but could also be used for other registries.
 * Implementations of this class are discovered automatically by {@link EPSGFactory} if they are declared in the
 * {@code module-info.class} file as providers of the {@code org.apache.sis.setup.InstallationResources} service.
 *
 * <h2>How this class is used</h2>
 * The first time that an {@link EPSGDataAccess} needs to be instantiated,
 * {@link EPSGFactory} verifies if the <abbr>EPSG</abbr> database exists.
 * If it does not, then:
 * <ol>
 *   <li>{@link EPSGFactory#install(Connection)} searches for the first instance of {@link InstallationResources}
 *       for which the {@linkplain #getAuthorities() set of authorities} contains {@code "EPSG"}.</li>
 *   <li>The {@linkplain #getLicense license} will be shown to the user if the application allows that
 *       (for example, when running as a {@linkplain org.apache.sis.console console application}).</li>
 *   <li>If the user accepts the <abbr>EPSG</abbr> terms of use, then this class iterates over all readers provided
 *       by {@link #openScript(String, int)} and executes the <abbr>SQL</abbr> statements (not necessarily verbatim,
 *       as the installation process may adapt some <abbr>SQL</abbr> statements to the target database).</li>
 * </ol>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.7
 */
public abstract class InstallationScriptProvider extends InstallationResources {
    /**
     * A sentinel value for the content of the script to execute before the SQL scripts provided by the authority.
     * This is an Apache <abbr>SIS</abbr> build-in script for replacing the {@code VARCHAR} type of some columns
     * by enumeration types, in order to constraint the values to the codes recognized by {@link EPSGDataAccess}.
     * Those enumerations are not mandatory for allowing {@link EPSGFactory} to work, but improve data integrity.
     *
     * @deprecated Ignored since the upgrade to version 10+ of <abbr>EPSG</abbr> because too dependent of the database schema.
     */
    @Deprecated(since = "1.5", forRemoval = true)
    protected static final String PREPARE = "Prepare";

    /**
     * A sentinel value for the content of the script to execute after the SQL scripts provided by the authority.
     * This is an Apache <abbr>SIS</abbr> build-in script for creating indexes or performing other manipulations
     * that help <abbr>SIS</abbr> to use the dataset. Those indexes are not mandatory for allowing
     * {@link EPSGFactory} to work, but improve performances.
     *
     * @deprecated Ignored since the upgrade to version 10+ of <abbr>EPSG</abbr> because too dependent of the database schema.
     */
    @Deprecated(since = "1.5", forRemoval = true)
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
     * Creates a new provider which will read script files of the given names in the given order.
     * The given names are often filenames, but not necessarily
     * (it is okay to use those names only as labels).
     *
     * <table class="sis">
     *   <caption>Example of argument values</caption>
     *   <tr>
     *     <th>Authority</th>
     *     <th class="sep">Resources</th>
     *   </tr><tr>
     *     <td>{@code EPSG}</td>
     *     <td class="sep"><code>
     *       {"Tables.sql", "Data.sql", "FKeys.sql", "Indexes.sql"}
     *     </code></td>
     *   </tr>
     * </table>
     *
     * @param  authority  the authority (typically {@code "EPSG"}), or {@code null} if not available.
     * @param  resources  names of the <abbr>SQL</abbr> scripts to read (typically filenames).
     *
     * @see #getResourceNames(String)
     * @see #openStream(String)
     */
    protected InstallationScriptProvider(final String authority, final String... resources) {
        this.resources = Objects.requireNonNull(resources);
        authorities = (authority != null) ? Set.of(authority) : Set.of();
    }

    /**
     * Returns the identifiers of the dataset installed by the <abbr>SQL</abbr> scripts.
     * The values currently recognized by <abbr>SIS</abbr> are:
     *
     * <ul>
     *   <li>{@code "EPSG"} for the <abbr>EPSG</abbr> geodetic dataset.</li>
     * </ul>
     *
     * The default implementation returns the authority given at construction time, or an empty set
     * if that authority was {@code null}. An empty set means that the provider does not have all
     * needed resources or does not have permission to distribute the installation scripts.
     *
     * @return identifiers of <abbr>SQL</abbr> scripts that this instance can distribute.
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
     * Returns the names of all <abbr>SQL</abbr> scripts to execute.
     * This is a copy of the array of names given to the constructor.
     * Those names are often filenames, but not necessarily (they may be just labels).
     *
     * @param  authority  the value given at construction time (e.g. {@code "EPSG"}).
     * @return the names of all <abbr>SQL</abbr> scripts to execute.
     * @throws IllegalArgumentException if the given {@code authority} argument is not the expected value.
     * @throws IOException if fetching the script names required an I/O operation and that operation failed.
     */
    @Override
    public String[] getResourceNames(String authority) throws IOException {
        verifyAuthority(authority);
        return resources.clone();
    }

    /**
     * Returns a reader for the <abbr>SQL</abbr> script at the given index.
     * The script may be read, for example, from a local file or from a resource in a <abbr>JAR</abbr> file.
     * The returned {@link BufferedReader} instance shall be closed by the caller.
     *
     * <h4>Default implementation</h4>
     * The default implementation delegates to {@link #openStream(String)}.
     * The input stream returned by {@code openStream(…)} is assumed encoded
     * in <abbr>UTF</abbr>-8 and is wrapped in a {@link LineNumberReader}.
     *
     * @param  authority  the value given at construction time (e.g. {@code "EPSG"}).
     * @param  resource   index of the <abbr>SQL</abbr> script to read, from 0 inclusive to
     *         <code>{@linkplain #getResourceNames getResourceNames}(authority).length</code> exclusive.
     * @return a reader for the content of <abbr>SQL</abbr> script to execute.
     * @throws IllegalArgumentException if the given {@code authority} argument is not the expected value.
     * @throws IndexOutOfBoundsException if the given {@code resource} argument is out of bounds.
     * @throws FileNotFoundException if the <abbr>SQL</abbr> script of the given name has not been found.
     * @throws IOException if an error occurred while creating the reader.
     */
    @Override
    public BufferedReader openScript(final String authority, final int resource) throws IOException {
        verifyAuthority(authority);
        if (!Constants.EPSG.equals(authority)) {
            throw new IllegalStateException(Resources.format(Resources.Keys.UnknownAuthority_1, authority));
        }
        String name = resources[resource];
        NoSuchFileException cause = null;
        try {
            final InputStream in = openStream(name);
            if (in != null) {
                return new LineNumberReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (NoSuchFileException e) {
            cause = e;
        }
        var e = new FileNotFoundException(Errors.format(Errors.Keys.FileNotFound_1, name));
        e.initCause(cause);
        throw e;
    }

    /**
     * Opens the input stream for the <abbr>SQL</abbr> script of the given name.
     * This method is invoked by the default implementation of {@link #openScript(String, int)}.
     * The returned input stream does not need to be buffered.
     *
     * <h4>Example 1</h4>
     * If this {@code InstallationScriptProvider} instance gets the <abbr>SQL</abbr> scripts from files in a well-known directory
     * and if the names given at {@linkplain #InstallationScriptProvider(String, String...) construction time} are filenames
     * in that directory, then this method can be implemented as below:
     *
     * {@snippet lang="java" :
     *      protected InputStream openStream(String name) throws IOException {
     *          return Files.newInputStream(directory.resolve(name));
     *      }
     * }
     *
     * <h4>Example 2</h4>
     * If this {@code InstallationScriptProvider} instance gets the <abbr>SQL</abbr> scripts from resources bundled in
     * the same <abbr>JAR</abbr> file and in the same package as the subclass, this method can be implemented as below:
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
}
