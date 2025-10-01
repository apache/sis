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
package org.apache.sis.resources.embedded;

import java.util.Set;
import java.util.Locale;
import java.util.StringJoiner;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import javax.sql.DataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.sis.util.internal.shared.MetadataServices;
import org.apache.sis.metadata.sql.internal.shared.Initializer;
import org.apache.sis.setup.InstallationResources;
import org.apache.sis.util.resources.Errors;


/**
 * Provides an embedded database for the <abbr>EPSG</abbr> geodetic dataset and other resources.
 * Provides also a copy of the <a href="https://epsg.org/terms-of-use.html">EPSG terms of use</a>,
 * which should be accepted by users before the EPSG dataset can be installed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.8
 *
 * @see <a href="https://epsg.org/">https://epsg.org/</a>
 */
public class EmbeddedResources extends InstallationResources {
    /**
     * The root directory of data embedded in the <abbr>JAR</abbr> file.
     * It must be an invalid package name, because otherwise the Java Platform Module System (JPMS) enforces
     * encapsulation in the same way as non-exported packages, which makes the database inaccessible to Derby.
     * This naming trick is part of <abbr>JPMS</abbr> specification, so it should be reliable.
     */
    static final String DIRECTORY = "SIS-DATA";

    /**
     * Creates a new provider for connections to the embedded database.
     */
    public EmbeddedResources() {
    }

    /**
     * Returns the pseudo-authority, which is {@code "Embedded"}.
     *
     * @return {@code "Embedded"} pseudo-authority.
     */
    @Override
    public Set<String> getAuthorities() {
        return Set.of(MetadataServices.EMBEDDED);
    }

    /**
     * Verifies that the given authority is the expected values.
     */
    private void verifyAuthority(final String authority) {
        if (!MetadataServices.EMBEDDED.equalsIgnoreCase(authority)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "authority", authority));
        }
    }

    /**
     * Returns the license of embedded data.
     *
     * @param  authority  shall be {@code "Embedded"}.
     * @param  locale     the preferred locale for the terms of use.
     * @param  mimeType   either {@code "text/plain"} or {@code "text/html"}.
     * @return the terms of use in plain text or HTML, or {@code null} if none.
     * @throws IllegalArgumentException if the given {@code authority} argument is not the expected values.
     * @throws IOException if an error occurred while reading the license file.
     */
    @Override
    public String getLicense(String authority, Locale locale, String mimeType) throws IOException {
        verifyAuthority(authority);
        final String filename;
        if ("text/plain".equalsIgnoreCase(mimeType)) {
            filename = "LICENSE.txt";
        } else if ("text/html".equalsIgnoreCase(mimeType)) {
            filename = "LICENSE.html";
        } else {
            return null;
        }
        final var buffer = new StringJoiner(System.lineSeparator(), "", System.lineSeparator());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                EmbeddedResources.class.getResourceAsStream(filename), "UTF-8")))
        {
            String line;
            while ((line = in.readLine()) != null) {
                buffer.add(line);
            }
        }
        return buffer.toString();
    }

    /**
     * Returns the data source name, which is {@code "SpatialMetadata"}.
     *
     * @param  authority  shall be {@code "Embedded"}.
     * @return {@code "SpatialMetadata"}.
     */
    @Override
    public String[] getResourceNames(String authority) {
        verifyAuthority(authority);
        return new String[] {Initializer.DATABASE};
    }

    /**
     * Returns the data source for embedded database.
     *
     * @param  authority  shall be {@code "Embedded"}.
     * @param  index      shall be 0.
     * @return the embedded data source.
     */
    @Override
    public DataSource getResource(String authority, int index) {
        verifyAuthority(authority);
        final var ds = new EmbeddedDataSource();
        ds.setDataSourceName(Initializer.DATABASE);
        ds.setDatabaseName("classpath:" + DIRECTORY + "/Databases/" + Initializer.DATABASE);
        return ds;
    }

    /**
     * Unconditionally throws an exception since the embedded database is not provided as <abbr>SQL</abbr> scripts.
     *
     * @param  authority  shall be {@code "Embedded"}.
     * @param  resource   shall be 0.
     * @return never return.
     * @throws IOException always thrown.
     */
    @Override
    public BufferedReader openScript(String authority, int resource) throws IOException {
        verifyAuthority(authority);
        throw new IOException(Errors.format(Errors.Keys.CanNotConvertFromType_2, DataSource.class, BufferedReader.class));
    }
}
