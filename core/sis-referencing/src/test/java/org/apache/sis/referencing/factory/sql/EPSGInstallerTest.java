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

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.io.IOException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.metadata.sql.Reflection;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;

// Test dependencies
import org.apache.sis.test.sql.TestDatabase;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;


/**
 * Tests {@link EPSGInstaller} indirectly, through {@link EPSGFactory#install(Connection)}.
 * We do not test {@code EPSGInstaller} directly because the EPSG database creation is costly,
 * so we want to opportunistically verify the result immediately after database creation
 * by using the {@code EPSGFactory} for creating a few CRS.
 *
 * <p>This test requires that {@code $SIS_DATA/Databases/ExternalSources} directory contains
 * the {@code EPSG_Tables.sql}, {@code EPSG_Data.sql} and {@code EPSG_FKeys.sql} files.
 * Those files can be <a href="https://epsg.org/">downloaded from the source</a> or from
 * <a href="https://sis.apache.org/source.html#non-free">SIS non-free directory</a>.</p>
 *
 * <p>Every databases created by this test suite exist only in memory.
 * This class does not write anything to disk (except maybe some temporary files).</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.7
 * @module
 */
@DependsOn(EPSGFactoryTest.class)
public final strictfp class EPSGInstallerTest extends TestCase {
    /**
     * A JUnit rule for listening to log events emitted during execution of tests.
     * This rule is used by tests that verifies the log message content.
     *
     * <p>This field is public because JUnit requires us to do so, but should be considered
     * as an implementation details (it should have been a private field).</p>
     */
    @Rule
    public final LoggingWatcher loggings = new LoggingWatcher(Loggers.CRS_FACTORY);

    /**
     * Verifies that no unexpected warning has been emitted in any test defined in this class.
     */
    @After
    public void assertNoUnexpectedLog() {
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the {@link EPSGInstaller#REPLACE_STATEMENT} pattern.
     */
    @Test
    public void testReplacePattern() {
        // Statement as in the EPSG scripts since EPSG version 7.06.
        assertTrue(Pattern.matches(EPSGInstaller.REPLACE_STATEMENT,
                "UPDATE epsg_datum\n" +
                "SET datum_name = replace(datum_name, CHR(182), CHR(10))"));

        // Statement as in the EPSG scripts prior to EPSG version 7.06.
        assertTrue(Pattern.matches(EPSGInstaller.REPLACE_STATEMENT,
                "UPDATE epsg_datum\n" +
                "SET datum_name = replace(datum_name, CHAR(182), CHAR(10))"));

        // Modified statement with MS-Access table name in a schema.
        assertTrue(Pattern.matches(EPSGInstaller.REPLACE_STATEMENT,
                "UPDATE epsg.\"Alias\"\n" +
                "SET object_table_name = replace(object_table_name, CHR(182), CHR(10))"));

        // Like above, but the table name contains a space.
        assertTrue(Pattern.matches(EPSGInstaller.REPLACE_STATEMENT,
                "UPDATE epsg.\"Coordinate Axis\"\n" +
                "SET coord_axis_orientation = replace(coord_axis_orientation, CHR(182), CHR(10))"));
    }

    /**
     * Returns the SQL scripts needed for testing the database creation,
     * or skip the JUnit test if those scripts are not found.
     */
    private static InstallationScriptProvider getScripts() throws IOException {
        final InstallationScriptProvider scripts = new InstallationScriptProvider.Default(null);
        assumeTrue("EPSG scripts not found in Databases/ExternalSources directory.",
                scripts.getAuthorities().contains(Constants.EPSG));
        return scripts;
    }

    /**
     * Tests the creation of an EPSG database on Derby.
     * This test is skipped if the SQL scripts are not found.
     *
     * <p>See {@link TestDatabase} javadoc if there is a need to inspect content of that in-memory database.</p>
     *
     * @throws Exception if an error occurred while creating the database.
     */
    @Test
    public void testCreationOnDerby() throws Exception {
        final InstallationScriptProvider scripts = getScripts();            // Needs to be invoked first.
        try (TestDatabase db = TestDatabase.create("EPSGInstaller")) {
            createAndTest(db.source, scripts);
            verifyParameterValues(db.source);
        }
        loggings.assertNextLogContains("EPSG", "jdbc:derby:memory:EPSGInstaller");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the creation of an EPSG database on HSQLDB.
     * This test is skipped if the SQL scripts are not found.
     *
     * @throws Exception if an error occurred while creating the database.
     */
    @Test
    public void testCreationOnHSQLDB() throws Exception {
        final InstallationScriptProvider scripts = getScripts();            // Needs to be invoked first.
        try (TestDatabase db = TestDatabase.createOnHSQLDB("EPSGInstaller", false)) {
            createAndTest(db.source, scripts);
            verifyParameterValues(db.source);
        }
        loggings.assertNextLogContains("EPSG", "jdbc:hsqldb:mem:EPSGInstaller");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the creation of an EPSG database on H2.
     * This test is skipped if the SQL scripts are not found.
     *
     * @throws Exception if an error occurred while creating the database.
     */
    @Test
    public void testCreationOnH2() throws Exception {
        final InstallationScriptProvider scripts = getScripts();            // Needs to be invoked first.
        try (TestDatabase db = TestDatabase.createOnH2("EPSGInstaller")) {
            createAndTest(db.source, scripts);
            verifyParameterValues(db.source);
        }
        loggings.assertNextLogContains("EPSG", "jdbc:h2:mem:EPSGInstaller");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the creation of an EPSG database on PostgreSQL. This test requires a PostgreSQL server
     * running on {@code "localhost"} with an empty database named {@code "SpatialMetadataTest"}.
     * See {@linkplain TestDatabase#createOnPostgreSQL here} for more information.
     *
     * @throws Exception if an error occurred while creating the database.
     *
     * @since 0.8
     */
    @Test
    public void testCreationOnPostgreSQL() throws Exception {
        final InstallationScriptProvider scripts = getScripts();            // Needs to be invoked first.
        try (TestDatabase db = TestDatabase.createOnPostgreSQL("EPSG", false)) {
            createAndTest(db.source, scripts);
            verifyParameterValues(db.source);
        }
        loggings.assertNextLogContains("EPSG", "jdbc:postgresql://localhost/SpatialMetadataTest");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Requests the "WGS84" and the "WGS72 / UTM zone 15N" coordinate reference systems from the EPSG database
     * at the given {@code DataSource}. Those requests should trig the creation of the EPSG database.
     */
    private void createAndTest(final DataSource ds, final InstallationScriptProvider scriptProvider)
            throws SQLException, FactoryException
    {
        final Map<String,Object> properties = new HashMap<>();
        assertNull(properties.put("dataSource", ds));
        assertNull(properties.put("scriptProvider", scriptProvider));
        assertEquals("Should not contain EPSG tables before we created them.", 0, countCRSTables(ds));
        loggings.assertNoUnexpectedLog();       // Should not yet have logged anything at this point.

        try (EPSGFactory factory = new EPSGFactory(properties)) {
            /*
             * Fetch the "WGS 84" coordinate reference system.
             */
            final GeographicCRS crs = factory.createGeographicCRS("4326");
            assertTrue(Utilities.deepEquals(CommonCRS.WGS84.geographic(), crs, ComparisonMode.DEBUG));
            /*
             * Fetch the "WGS 72 / UTM zone 15" coordinate system.
             * This implies the creation of a coordinate operation.
             */
            final ProjectedCRS p = factory.createProjectedCRS("EPSG:32215");
            assertTrue(Utilities.deepEquals(CommonCRS.WGS72.universal(1, -93), p, ComparisonMode.DEBUG));
            /*
             * Get the authority codes. We choose a type that implies an SQL statement
             * with both "DEPRECATED" and "SHOW_CRS" conditions in their "WHERE" clause.
             */
            Set<String> codes = factory.getAuthorityCodes(GeographicCRS.class);
            assertTrue("4979", codes.contains("4979"));     // A non-deprecated code.
            assertTrue("4329", codes.contains("4329"));     // A deprecated code.
            /*
             * Following forces the authority factory to iterate over all codes.
             * Since the iterator returns only non-deprecated codes, EPSG:4329
             * should not be included. The intent is to verify that the fields
             * of type BOOLEAN have been properly handled.
             */
            codes = new HashSet<>(codes);
            assertTrue ("4979", codes.contains("4979"));
            assertFalse("4329", codes.contains("4329"));
        }
        assertEquals("Should contain EPSG tables after we created them.", 1, countCRSTables(ds));
    }

    /**
     * Counts the number of {@code EPSG."Coordinate Reference System"} tables.
     * It should be 0 or 1. Any schema other than "EPSG" causes a test failure.
     */
    private static int countCRSTables(final DataSource ds) throws SQLException {
        int count = 0;
        try (Connection c = ds.getConnection()) {
            try (ResultSet r = c.getMetaData().getTables(null, null, "Coordinate Reference System", null)) {
                while (r.next()) {
                    final String schema = r.getString(Reflection.TABLE_SCHEM);
                    assertTrue(schema, "EPSG".equalsIgnoreCase(schema));
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Verifies some parameter values in the database. We perform this check on a parameter which are known
     * to have small values, in order to make sure that the values have not been truncated to zero.
     */
    private static void verifyParameterValues(final DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("SELECT COORD_OP_CODE, PARAMETER_VALUE"
                    + " FROM \"EPSG\".\"Coordinate_Operation Parameter Value\""
                    + " WHERE PARAMETER_CODE = 1035 AND COORD_OP_METHOD_CODE = 1042"))
            {
                while (r.next()) {
                    switch (r.getInt(1)) {
                        case 5219:
                        case 5511: {
                            assertEquals(-3.689471323E-24, r.getDouble(2), STRICT);
                            break;
                        }
                    }
                }
            }
        }
    }
}
