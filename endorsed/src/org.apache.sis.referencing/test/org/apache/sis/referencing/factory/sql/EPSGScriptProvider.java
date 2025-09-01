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
import java.io.InputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Files;
import org.apache.sis.pending.jdk.JDK22;
import org.apache.sis.util.privy.Constants;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 * A script provider which uses the scripts in the {@code org.apache.sis.referencing.epsg} optional module, if the scripts are present.
 * For licensing reasons, those scripts must be installed manually as documented in the
 * <a href="https://sis.apache.org/source.html#non-free">source checkout web page</a>.
 * This class expects the files to have those exact names:
 *
 * <ul>
 *   <li>{@code Prepare.sql} (from Apache <abbr>SIS</abbr>)</li>
 *   <li>{@code Tables.sql} (derived from <abbr>EPSG</abbr>)</li>
 *   <li>{@code Data.sql}   (derived from <abbr>EPSG</abbr>)</li>
 *   <li>{@code FKeys.sql}  (derived from <abbr>EPSG</abbr>)</li>
 *   <li>{@code Finish.sql}  (from Apache <abbr>SIS</abbr>)</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class EPSGScriptProvider extends InstallationScriptProvider {
    /**
     * The source code directory where to look for the <abbr>SQL</abbr> scripts.
     */
    private final Path directory;

    /**
     * Creates the provider.
     *
     * @param locale  the locale for warning messages, if any.
     */
    EPSGScriptProvider() throws IOException, URISyntaxException {
        super(Constants.EPSG, "Prepare.sql", "Tables.sql", "Data.sql", "FKeys.sql", "Finish.sql");
        final Class<?> member = EPSGScriptProvider.class;
        final URL url = member.getResource(member.getSimpleName() + ".class");
        assertNotNull(url, "Class not found.");
        assertNotEquals("jar", url.getProtocol());
        Path parent = Path.of(url.toURI());
        Path subproject;
        do {
            parent = parent.getParent();
            assertNotNull(parent, "Project root not found.");
            subproject = parent.resolve("optional");
        } while (!Files.isDirectory(subproject));
        directory = JDK22.resolve(subproject, "src", "org.apache.sis.referencing.epsg", "main",
                            "org", "apache", "sis", "referencing", "factory", "sql", "epsg");
        assertTrue(Files.isDirectory(directory));
    }

    /**
     * Returns {@code "EPSG"} if the non-free scripts exist in the {@code org.apache.sis.referencing.epsg} optional module,
     * or an empty set otherwise.
     *
     * @return {@code "EPSG"} if the <abbr>SQL</abbr> scripts for installing the EPSG dataset are available,
     *         or an empty set otherwise.
     */
    @Override
    public Set<String> getAuthorities() {
        return Files.isReadable(directory.resolve("Data.sql")) ? super.getAuthorities() : Set.of();
    }

    /**
     * Returns {@code null} since the users are presumed to have downloaded the files themselves.
     *
     * @return the terms of use in plain text or HTML, or {@code null} if the license is presumed already accepted.
     */
    @Override
    public String getLicense(String authority, Locale locale, String mimeType) {
        return null;
    }

    /**
     * Opens the input stream for the <abbr>SQL</abbr> script of the given name.
     * The returned input stream does not need to be buffered.
     *
     * @param  name  name of the script file to open.
     * @return an input stream opened of the given script file.
     * @throws IOException if an error occurred while opening the file.
     */
    @Override
    protected InputStream openStream(final String name) throws IOException {
        return Files.newInputStream(directory.resolve(name));
    }
}
