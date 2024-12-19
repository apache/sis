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
package org.apache.sis.storage.geopackage.conformance;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.geopackage.GpkgStore;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Some of the conformance tests defined by the Geopackage standard.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://www.geopackage.org/spec140/index.html#abstract_test_suite">Geopackage specification annex A</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public final class CoreConformanceTest {
    /**
     * Temporary test file for Geopackage database.
     */
    private final Path file;

    /**
     * The initially empty data store.
     */
    private final GpkgStore store;

    /**
     * Creates a new test case.
     *
     * @throws IOException if the temporary test file cannot be created.
     * @throws DataStoreException if an error occurred while creating the data store.
     */
    public CoreConformanceTest() throws IOException, DataStoreException {
        file = Files.createTempFile("sis-test-", ".gpkg");
        final var connector = new StorageConnector(file);
        connector.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE,
            StandardOpenOption.READ
        });
        store = new GpkgStore(null, connector);
        assertTrue(store.components().isEmpty());       // Force database initialization.
    }

    /**
     * Closes the data store and deletes the temporary file after completion of all tests.
     *
     * @throws DataStoreException if an error occurred while closing the data store.
     * @throws IOException if an error occurred while deleting the temporary file.
     */
    @AfterAll
    public void delete() throws DataStoreException, IOException {
        store.close();
        Files.delete(file);
    }

    /**
     * Verify that the GeoPackage is an SQLite version_3 database.
     * Pass if the first 16 bytes of the file contain "SQLite format 3" in ASCII.
     * <ul>
     *   <li>Test Case ID: /base/core/container/data/file_format</li>
     *   <li>Reference:    Clause 1.1.1.1.1 Req 1</li>
     *   <li>Test Type:    Basic</li>
     * </ul>
     *
     * Verify that the SQLite database header application id field indicates GeoPackage version 1.0.
     * See the specification. <em>This JUnit test is more restrictive than required.</em>
     * <ul>
     *   <li>Test Case ID: /base/core/container/data/file_format/application_id</li>
     *   <li>Reference:    Clause 1.1.1.1.1 Req 2</li>
     *   <li>Test Type:    Basic</li>
     * </ul>
     *
     * @throws IOException if an error occurred while verifying the result.
     */
    @Test
    public void containerDataFileFormat() throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            final var ds = new DataInputStream(stream);
            final byte[] signature = new byte[16];
            ds.readFully(signature);
            assertArrayEquals(new byte[] {'S','Q','L','i','t','e',' ','f','o','r','m','a','t',' ','3',0x00}, signature);

            ds.skip(68 - signature.length);
            final byte[] id = new byte[4];
            ds.readFully(id);
            assertArrayEquals(new byte[] {'G','P','K','G'}, id);
        }
    }

    /**
     * Verify that the GeoPackage extension is ".gpkg".
     * Pass if the GeoPackage file extension is ".gpkg".
     * <ul>
     *   <li>Test Case ID: /base/core/container/data/file_extension_name</li>
     *   <li>Reference:    Clause 1.1.1.1.2 Req 3</li>
     *   <li>Test Type:    Basic</li>
     * </ul>
     */
    @Test
    public void containerDataFileExtensionName() {
        assertTrue(file.getFileName().toString().endsWith(".gpkg"));
    }

    /**
     * Verify that the GeoPackage only contains specified contents.
     * Note: this test has been removed in Geopackage 1.4.0.
     *
     * <ul>
     *   <li>Test Case ID: /base/core/container/data/file_contents</li>
     *   <li>Reference:    Clause 1.1.1.1.3 Req 4</li>
     *   <li>Test Type:    Basic</li>
     * </ul>
     *
     * @throws SQLException if an error occurred while verifying the database content.
     */
    @Test
    public void containerDataFileContents() throws SQLException {
        try (Connection cnx = store.getDataSource().getConnection();
             Statement stmt = cnx.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT COUNT(*) from gpkg_extensions"))
        {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));      // Because WKT 2 extension is installed by default.
            assertFalse(rs.wasNull());
        }
    }

    /**
     * Verify that the GeoPackage passes the SQLite integrity check.
     * Pass if "PRAGMA integrity_check" returns "ok".
     * <ul>
     *   <li>Test Case ID: /base/core/container/data/file_integrity</li>
     *   <li>Reference:    Clause File Integrity Req 6</li>
     *   <li>Test Type:    Capability</li>
     * </ul>
     *
     * Verify that the GeoPackage passes the SQLite foreign key check.
     * Pass if "PRAGMA foreign_key_check" (with no parameter value) returns an empty result set.
     * <ul>
     *   <li>Test Case ID: /base/core/container/data/foreign_key_integrity</li>
     *   <li>Reference:    Clause File Integrity Req 7</li>
     *   <li>Test Type:    Capability</li>
     * </ul>
     *
     * @throws SQLException if an error occurred while verifying the database content.
     */
    @Test
    public void containerDataFileIntegrity() throws SQLException {
        try (Connection cnx = store.getDataSource().getConnection();
             Statement stmt = cnx.createStatement())
        {
            try (ResultSet rs = stmt.executeQuery("PRAGMA integrity_check")) {
                assertTrue(rs.next());
                assertEquals("ok", rs.getString(1));
            }
            try (ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(rs.next());
            }
        }
    }

    /**
     * Test that the GeoPackage SQLite Extension provides the SQLite SQL API interface..
     *
     * <ul>
     *   <li>Test Case ID: /base/core/container/api/sql</li>
     *   <li>Reference: Clause 1.1.1.2.1 Req 8</li>
     *   <li>Test Type: Capability</li>
     * </ul>
     *
     * @throws SQLException if an error occurred while verifying the database content.
     */
    @Test
    public void containerApiSql() throws SQLException {
        try (Connection cnx = store.getDataSource().getConnection();
             Statement stmt = cnx.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT * FROM sqlite_master"))
        {
            assertTrue(rs.next());
        }
    }
}
