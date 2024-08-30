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
import java.util.ServiceLoader;
import javax.sql.DataSource;
import org.opengis.util.FactoryException;
import org.apache.sis.setup.InstallationResources;
import org.apache.sis.metadata.sql.privy.Initializer;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.sql.epsg.ScriptProvider;

import org.apache.sis.referencing.crs.AbstractCRS;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.apache.sis.test.TestUtilities;


/**
 * Tests {@link EmbeddedResources}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final strictfp class EmbeddedResourcesTest {
    /**
     * Whether the database has been created.
     */
    private static boolean databaseCreated;

    /**
     * Creates a new test case.
     */
    public EmbeddedResourcesTest() {
    }

    /**
     * Skips the test if the EPSG scripts are not present.
     * This method uses {@code LICENSE.txt} as a sentinel file.
     */
    private static void assumeDataPresent() {
        assumeTrue(ScriptProvider.class.getResource("LICENSE.txt") != null,
                "EPSG resources not found. See `README.md` for manual installation.");
    }

    /**
     * Returns the {@link EmbeddedResources} instance declared in the {@code META-INF/services/} directory.
     * The provider may coexist with providers defined in other modules, so we need to filter them.
     */
    private static synchronized InstallationResources getInstance() {
        if (!databaseCreated) try {
            new Generator().run();
            databaseCreated = true;
        } catch (Exception e) {
            throw new AssertionError(e);
        }

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
        assumeDataPresent();
        final InstallationResources provider = getInstance();
        assertTrue(provider.getLicense("Embedded", null, "text/plain").contains("IOGP"));
        assertTrue(provider.getLicense("Embedded", null, "text/html" ).contains("IOGP"));
    }

    /**
     * Tests connecting to the database.
     *
     * @throws Exception if an error occurred while fetching the data source, or connecting to the database.
     */
    @Test
    public void testConnection() throws Exception {
        assumeDataPresent();
        final String dir = DataDirectory.getenv();
        assertTrue((dir == null) || dir.isEmpty(), "The SIS_DATA environment variable must be unset for enabling this test.");
        final DataSource ds = Initializer.getDataSource();
        assertNotNull(ds, "Cannot find the data source.");
        try (Connection c = ds.getConnection()) {
            assertEquals("jdbc:derby:classpath:SIS_DATA/Databases/spatial-metadata", c.getMetaData().getURL(), "URL");
            try (Statement s = c.createStatement()) {
                try (ResultSet r = s.executeQuery("SELECT COORD_REF_SYS_NAME FROM EPSG.\"Coordinate Reference System\" WHERE COORD_REF_SYS_CODE = 4326")) {
                    assertTrue(r.next(), "ResultSet.next()");
                    assertEquals(r.getString(1), "WGS 84");
                    assertFalse(r.next(), "ResultSet.next()");
                }
            }
        }
    }

    /**
     * Tests {@link CRS#forCode(String)} with the embedded database. This test asks for a CRS for which
     * no hard-coded fallback exists in {@link org.apache.sis.referencing.CommonCRS}. Consequently this
     * test should fail if we do not have a connection to a complete EPSG database.
     *
     * @throws FactoryException if an error occurred while creating the CRS.
     */
    @Test
    public void testCrsforCode() throws FactoryException {
        assumeDataPresent();
        var crs = assertInstanceOf(AbstractCRS.class, CRS.forCode("EPSG:6676"));
        String area = TestUtilities.getSingleton(crs.getDomains()).getDomainOfValidity().getDescription().toString();
        assertTrue(area.contains("Japan"), area);
    }
}
