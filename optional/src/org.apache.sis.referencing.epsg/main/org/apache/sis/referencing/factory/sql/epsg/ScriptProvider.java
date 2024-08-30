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
package org.apache.sis.referencing.factory.sql.epsg;

import java.util.Locale;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.referencing.factory.sql.InstallationScriptProvider;


/**
 * Provides SQL scripts for creating a local copy of the EPSG geodetic dataset.
 * Provides also a copy of the <a href="https://epsg.org/terms-of-use.html">EPSG terms of use</a>,
 * which should be accepted by users before the EPSG dataset can be installed.
 *
 * <p><b>Notice</b></p>
 * EPSG is maintained by the <a href="http://www.iogp.org/">International Association of Oil and Gas Producers</a>
 * (IOGP) Surveying &amp; Positioning Committee. The SQL scripts are given by this class with identical content,
 * but in a more compact format.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.7
 *
 * @see <a href="https://epsg.org/">https://epsg.org/</a>
 */
public class ScriptProvider extends InstallationScriptProvider {
    /**
     * Creates a new EPSG scripts provider.
     */
    public ScriptProvider() {
        super(Constants.EPSG, PREPARE, "Tables.sql", "Data.sql", "FKeys.sql", FINISH);
    }

    /**
     * Returns a copy of EPSG terms of use.
     *
     * @param  authority  one of the values returned by {@link #getAuthorities()}.
     * @param  locale     the preferred locale for the terms of use.
     * @param  mimeType   either {@code "text/plain"} or {@code "text/html"}.
     * @return the terms of use in plain text or HTML, or {@code null} if the specified MIME type is not recognized.
     * @throws IOException if an error occurred while reading the license file.
     *
     * @see <a href="https://epsg.org/terms-of-use.html">https://epsg.org/terms-of-use.html</a>
     */
    @Override
    public String getLicense(final String authority, final Locale locale, final String mimeType) throws IOException {
        if (!Constants.EPSG.equals(authority)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "authority", authority));
        }
        final String filename;
        if ("text/plain".equalsIgnoreCase(mimeType)) {
            filename = "LICENSE.txt";
        } else if ("text/html".equalsIgnoreCase(mimeType)) {
            filename = "LICENSE.html";
        } else {
            return null;
        }
        final InputStream in = ScriptProvider.class.getResourceAsStream(filename);
        if (in == null) {
            throw new FileNotFoundException(filename);
        }
        final StringBuilder buffer = new StringBuilder();
        final String lineSeparator = System.lineSeparator();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) {
                buffer.append(line).append(lineSeparator);
            }
        }
        return buffer.toString();
    }

    /**
     * Returns the content for the SQL script of the given name.
     * The file encoding is UTF-8.
     *
     * @param name  either {@code "Tables.sql"}, {@code "Data.sql"} or {@code "FKeys.sql"}.
     * @return the SQL script of the given name, or {@code null} if the given name is not one of the expected names.
     */
    @Override
    protected InputStream openStream(final String name) {
        return ScriptProvider.class.getResourceAsStream(name);
    }
}
