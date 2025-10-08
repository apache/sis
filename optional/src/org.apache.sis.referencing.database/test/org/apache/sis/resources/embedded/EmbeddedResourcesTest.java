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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import java.util.Map;
import java.util.ServiceLoader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.setup.InstallationResources;
import org.apache.sis.metadata.sql.internal.shared.Initializer;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.referencing.factory.sql.EPSGFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.apache.sis.test.TestUtilities;


/**
 * Tests {@link EmbeddedResources}.
 * This test has the side-effect of creating the database if it does not already exists.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class EmbeddedResourcesTest {
    /**
     * Creates a new test case.
     *
     * @throws Exception if an error occurred while creating the database.
     */
    public EmbeddedResourcesTest() throws Exception {
        // TODO: use @BeforeAll annotation instead. It doesn't seem to work with current Gradle build.
        createDatabaseIfAbsent();
    }

    /**
     * Creates the database if not already done.
     *
     * @throws Exception if an error occurred while creating the database.
     */
    private static void createDatabaseIfAbsent() throws Exception {
        final var generator = new Generator();
        synchronized (Initializer.class) {
            generator.registerAsDefaultDataSource();  // Done first because `….metadata.sql` will want its data source.
            generator.createIfAbsent();
        }
    }

    /**
     * Skips the test if the <abbr>EPSG</abbr> scripts are not present.
     * This method uses {@code LICENSE.txt} as a sentinel file.
     * Note that even if the <abbr>EPSG</abbr> data are not available,
     * the database may still contain other metadata.
     */
    private static void assumeContainsEPSG() {
        assumeTrue(EmbeddedResources.class.getResource("LICENSE.txt") != null,
                "EPSG resources not found. See `README.md` for manual installation.");
    }

    /**
     * Returns the {@link EmbeddedResources} instance declared in the {@code META-INF/services/} directory.
     * The provider may coexist with providers defined in other modules, so we need to filter them.
     */
    private static InstallationResources getInstance() {
        InstallationResources provider = null;
        for (InstallationResources candidate : ServiceLoader.load(InstallationResources.class)) {
            if (candidate instanceof EmbeddedResources) {
                assertNull(provider, "Expected only one instance.");
                provider = candidate;
            }
        }
        assertNotNull(provider, "Expected an instance.");
        return provider;
    }

    /**
     * Tests fetching the licenses.
     *
     * @throws IOException if an error occurred while reading a license.
     */
    @Test
    public void testLicences() throws IOException {
        assumeContainsEPSG();
        final InstallationResources provider = getInstance();
        assertTrue(provider.getLicense("Embedded", null, "text/plain").contains("IOGP"));
        assertTrue(provider.getLicense("Embedded", null, "text/html" ).contains("IOGP"));
    }

    /**
     * Tests connecting to the database.
     *
     * @throws Exception if an error occurred while fetching the data source, or connecting to the database.
     *
     * @todo The test is currently not executed because {@code SIS_DATA} is set by the build script.
     *       We need a way to ignore it.
     */
    @Test
    public void testConnection() throws Exception {
        assumeContainsEPSG();
        final String dir = DataDirectory.getenv();
        assumeTrue((dir == null) || dir.isEmpty(), "The SIS_DATA environment variable must be unset for enabling this test.");
        final DataSource ds = Initializer.getDataSource();
        assertNotNull(ds, "Cannot find the data source.");
        try (Connection c = ds.getConnection()) {
            assertEquals("jdbc:derby:classpath:" + EmbeddedResources.DIRECTORY + "/Databases/" + Initializer.DATABASE, c.getMetaData().getURL(), "URL");
            try (Statement s = c.createStatement()) {
                try (ResultSet r = s.executeQuery("SELECT COORD_REF_SYS_NAME FROM EPSG.\"Coordinate Reference System\" WHERE COORD_REF_SYS_CODE = 4326")) {
                    assertTrue(r.next(), "ResultSet.next()");
                    assertEquals(r.getString(1), "WGS 84");
                    assertFalse(r.next(), "ResultSet.next()");
                }
            }
        }
        try (EPSGFactory factory = new EPSGFactory(Map.of("dataSource", ds))) {
            verifyEPSG_6676(factory.createCoordinateReferenceSystem("EPSG:6676"));
        }
    }

    /**
     * Verifies the <abbr>CRS</abbr> created from code EPSG:6676. This is a <abbr>CRS</abbr>
     * for which no hard-coded fallback exists in {@link org.apache.sis.referencing.CommonCRS}.
     * Consequently, this verification fails if we do not have a connection to a complete <abbr>EPSG</abbr> database.
     */
    private static void verifyEPSG_6676(final CoordinateReferenceSystem crs) {
        String area = TestUtilities.getSingleton(crs.getDomains()).getDomainOfValidity().getDescription().toString();
        assertTrue(area.contains("Japan"), area);
    }
}
