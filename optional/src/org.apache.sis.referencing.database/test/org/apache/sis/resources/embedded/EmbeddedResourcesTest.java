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
import java.util.ServiceLoader;
import org.opengis.util.FactoryException;
import org.apache.sis.setup.InstallationResources;
import org.apache.sis.metadata.sql.internal.shared.Initializer;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.referencing.CRS;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.apache.sis.test.TestUtilities;

// Specific to the main branch:
import org.apache.sis.referencing.crs.AbstractCRS;


/**
 * Tests {@link EmbeddedResources}.
 * This test has the side-effect of creating the database if it does not already exists.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final strictfp class EmbeddedResourcesTest {
    /**
     * Creates a new test case.
     *
     * @throws Exception if an error occurred while creating the database.
     */
    public EmbeddedResourcesTest() throws Exception {
        new Generator().createIfAbsent();
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
     */
    @Test
    public void testConnection() throws Exception {
        assumeContainsEPSG();
        final String dir = DataDirectory.getenv();
        assumeTrue((dir == null) || dir.isEmpty(), "The SIS_DATA environment variable must be unset for enabling this test.");
        final DataSource ds = Initializer.getDataSource();
        assertNotNull(ds, "Cannot find the data source.");
        try (Connection c = ds.getConnection()) {
            assertEquals("jdbc:derby:classpath:SIS_DATA/Databases/" + EmbeddedResources.EMBEDDED_DATABASE, c.getMetaData().getURL(), "URL");
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
        assumeContainsEPSG();
        var crs = assertInstanceOf(AbstractCRS.class, CRS.forCode("EPSG:6676"));
        String area = TestUtilities.getSingleton(crs.getDomains()).getDomainOfValidity().getDescription().toString();
        assertTrue(area.contains("Japan"), area);
    }
}
