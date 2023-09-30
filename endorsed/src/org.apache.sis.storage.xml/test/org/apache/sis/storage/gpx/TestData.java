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
package org.apache.sis.storage.gpx;

import java.net.URL;
import java.io.InputStream;
import org.apache.sis.util.Version;


/**
 * Identification of the data to use for testing purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum TestData {
    /**
     * Test for GPX 1.0.
     */
    V1_0(StoreProvider.V1_0, "1.0/"),

    /**
     * Test for GPX 1.1.
     */
    V1_1(StoreProvider.V1_1, "1.1/");

    /**
     * Version of the XML schema used by this format.
     */
    final Version schemaVersion;

    /**
     * The directory (relative to the {@code TestData.class} file) of the XML document.
     */
    private final String directory;

    /**
     * Filename of the metadata test file.
     */
    public static final String METADATA = "metadata.xml";

    /**
     * Filename of a test data file.
     */
    static final String WAYPOINT = "waypoint.xml", ROUTE = "route.xml", TRACK = "track.xml";

    /**
     * Creates a new enumeration for documents in the specified sub-directory.
     */
    private TestData(final Version schemaVersion, final String directory) {
        this.schemaVersion = schemaVersion;
        this.directory = directory;
    }

    /**
     * Returns the URL to the specified XML file.
     *
     * @param  filename  name of the file to open.
     * @return URL to the XML document to use for testing purpose.
     */
    public final URL getURL(final String filename) {
        // Call to `getResource(…)` is caller sensitive: it must be in the same module.
        return TestData.class.getResource(directory.concat(filename));
    }

    /**
     * Opens the stream to the specified XML file.
     *
     * @param  filename  name of the file to open.
     * @return stream opened on the XML document to use for testing purpose.
     */
    public final InputStream openStream(final String filename) {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return TestData.class.getResourceAsStream(directory.concat(filename));
    }
}
