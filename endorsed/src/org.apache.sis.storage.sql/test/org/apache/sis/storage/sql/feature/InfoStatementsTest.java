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
package org.apache.sis.storage.sql.feature;

import java.util.Map;
import java.util.List;
import java.sql.Connection;
import java.sql.SQLException;
import java.lang.reflect.Field;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.metadata.sql.internal.shared.Dialect;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.event.StoreListeners;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.storage.DataStoreMock;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;
import org.apache.sis.metadata.sql.TestDatabase;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.datum.HardCodedDatum;


/**
 * Tests {@link InfoStatements}.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class InfoStatementsTest extends TestCase {
    /**
     * A in-memory database on Derby.
     */
    private final TestDatabase test;

    /**
     * SQL connection used for the tests.
     */
    private Connection connection;

    /**
     * The database to test.
     */
    private Database<?> database;

    /**
     * Creates a new test case.
     *
     * @throws SQLException if an error occurred while creating a temporary in-memory database.
     */
    public InfoStatementsTest() throws SQLException {
        test = TestDatabase.create("InfoStatements");
    }

    /**
     * Returns the SQL statement for creating the {@code SPATIAL_REF_SYS} table.
     *
     * @return the {@code CREATE TABLE SPATIAL_REF_SYS} statement.
     */
    public static String createSpatialRefSys() {
        return "CREATE TABLE SPATIAL_REF_SYS ("
                + "SRID INTEGER NOT NULL PRIMARY KEY, "
                + "AUTH_NAME VARCHAR(100), "
                + "AUTH_SRID INTEGER, "
                + "SRTEXT VARCHAR(2000));";
    }

    /**
     * Creates some tables needed by the tests, then creates a {@link Database} instance needed for the tests.
     *
     * @throws Exception if an error occurred while creating the tables.
     */
    @BeforeAll
    public void initialize() throws Exception {
        test.executeSQL(List.of(createSpatialRefSys()));
        connection = test.source.getConnection();
        database = new Database<>(test.source, connection.getMetaData(), Dialect.DERBY,
                                  Geometries.factory(GeometryLibrary.JAVA2D), null,
                                  new StoreListeners(null, new DataStoreMock("Unused")), null);
        /*
         * The `spatialSchema` is private, so we need to use reflection for setting its value.
         * Normally that field would be set by `Database.analyze(…)`, bur we want to avoid that
         * complexity for more isolated tests.
         */
        Field field = Database.class.getDeclaredField("spatialSchema");
        field.setAccessible(true);
        field.set(database, SpatialSchema.SIMPLE_FEATURE);
        assertTrue(database.crsEncodings.add(CRSEncoding.WKT1));
    }

    /**
     * Tests {@link InfoStatements#findSRID(CoordinateReferenceSystem)}.
     *
     * @throws Exception if a SQL, WKT or other error occurred.
     */
    @Test
    public void testFindSRID() throws Exception {
        final Connection c = connection;
        try (InfoStatements info = new InfoStatements(database, c)) {
            c.setReadOnly(true);
            final CoordinateReferenceSystem crs = HardCodedCRS.WGS84;
            var e = assertThrows(DataStoreReferencingException.class, () -> info.findSRID(crs));
            assertMessageContains(e, crs.getName().getCode());

            // Now do the actual insertion.
            c.setReadOnly(false); assertEquals(4326, info.findSRID(crs));
            c.setReadOnly(true);  assertEquals(4326, info.findSRID(crs));

            // CRS with the same code (intentional clash with EPSG:4326).
            final CoordinateReferenceSystem clash = new DefaultGeographicCRS(
                    Map.of(CoordinateReferenceSystem.NAME_KEY, "Sphere",
                           CoordinateReferenceSystem.IDENTIFIERS_KEY, new ImmutableIdentifier(null, "FOO", "4326")),
                    HardCodedDatum.SPHERE, null, HardCodedCS.GEODETIC_2D);

            c.setReadOnly(false);
            final int code = info.findSRID(clash);
            assertNotEquals(4326, code);
            assertTrue(code > 0);
            c.setReadOnly(true);
            assertEquals(code, info.findSRID(clash));
        }
    }

    /**
     * Closes the temporary in-memory database.
     *
     * @throws SQLException if an error occurred while closing the database.
     */
    @AfterAll
    public void close() throws SQLException {
        try (test) {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        }
    }
}
