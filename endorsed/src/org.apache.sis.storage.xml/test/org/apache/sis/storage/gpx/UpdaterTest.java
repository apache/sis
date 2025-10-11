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

import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import com.esri.core.geometry.Point;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;

// Test dependencies
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.xml.test.TestCase.assertXmlEquals;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;


/**
 * Tests (indirectly) the {@link Updater} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-411">SIS-411</a>
 */
public final class UpdaterTest extends TestCase {
    /**
     * The provider shared by all data stores created in this test class.
     */
    private final StoreProvider provider;

    /**
     * Temporary file where to write the GPX file.
     */
    private Path file;

    /**
     * Creates the provider to be shared by all data stores created in this test class.
     */
    public UpdaterTest() {
        provider = StoreProvider.provider();
    }

    /**
     * Creates the temporary file before test execution.
     *
     * @throws IOException if the temporary file cannot be created.
     */
    @BeforeEach
    public void createTemporaryFile() throws IOException {
        file = Files.createTempFile("GPX", ".xml");
    }

    /**
     * Deletes temporary file after test execution.
     *
     * @throws IOException if the temporary file cannot be deleted.
     */
    @AfterEach
    public void deleteTemporaryFile() throws IOException {
        if (file != null) {
            Files.delete(file);
        }
    }

    /**
     * Creates a new GPX data store which will read and write in a temporary file.
     */
    private WritableStore create() throws DataStoreException, IOException {
        final StorageConnector connector = new StorageConnector(file);
        connector.setOption(OptionKey.GEOMETRY_LIBRARY, GeometryLibrary.ESRI);
        connector.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {
                            StandardOpenOption.READ, StandardOpenOption.WRITE});
        return new WritableStore(provider, connector);
    }

    /**
     * Tests writing in an initially empty file.
     *
     * @throws IOException if an error occurred while creating the temporary file.
     * @throws DataStoreException if an error occurred while using the GPX store.
     */
    @Test
    public void testWriteEmpty() throws DataStoreException, IOException {
        try (final WritableStore store = create()) {
            final Types types = store.types;
            final AbstractFeature point1 = types.wayPoint.newInstance();
            final AbstractFeature point2 = types.wayPoint.newInstance();
            final AbstractFeature point3 = types.wayPoint.newInstance();
            point1.setPropertyValue("sis:geometry", new Point(15, 10));
            point2.setPropertyValue("sis:geometry", new Point(25, 20));
            point3.setPropertyValue("sis:geometry", new Point(35, 30));
            point1.setPropertyValue("time", Instant.parse("2010-01-10T00:00:00Z"));
            point3.setPropertyValue("time", Instant.parse("2010-01-30T00:00:00Z"));
            store.add(List.of(point1, point2, point3).iterator());
        }
        assertXmlEquals(
                "<gpx xmlns=\"" + Tags.NAMESPACE + "1/1\" version=\"1.1\">\n" +
                "  <wpt lat=\"10.0\" lon=\"15.0\">\n" +
                "    <time>2010-01-10T00:00:00Z</time>\n" +
                "  </wpt>\n" +
                "  <wpt lat=\"20.0\" lon=\"25.0\"/>\n" +
                "  <wpt lat=\"30.0\" lon=\"35.0\">\n" +
                "    <time>2010-01-30T00:00:00Z</time>\n" +
                "  </wpt>\n" +
                "</gpx>", file, "xmlns:*");
    }

    /**
     * Tests an update which requires rewriting the XML file.
     *
     * @throws IOException if an error occurred while creating the temporary file.
     * @throws DataStoreException if an error occurred while using the GPX store.
     */
    @Test
    public void testRewrite() throws DataStoreException, IOException {
        try (InputStream in = UpdaterTest.class.getResourceAsStream("1.1/waypoint.xml")) {
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
        assertTrue(containsLat20());
        try (final WritableStore store = create()) {
            store.removeIf((feature) -> {
                Object point = feature.getPropertyValue("sis:geometry");
                return ((Point) point).getY() == 20;
            });
        }
        assertFalse(containsLat20());
    }

    /**
     * Returns whether the temporary file contains the {@code lat="20"} string.
     * Also checks some invariants such as the presence of metadata.
     */
    private boolean containsLat20() throws IOException {
        final String xml = Files.readString(file);
        assertTrue(xml.contains("<bounds "));       // Sentinel value for presence of metadata.
        return xml.contains("lat=\"20");            // May have trailing ".0".
    }
}
