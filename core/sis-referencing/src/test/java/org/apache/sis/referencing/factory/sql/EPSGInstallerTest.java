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
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;

// Test dependencies
import org.apache.sis.internal.metadata.sql.TestDatabase;
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.*;


/**
 * Tests {@link EPSGInstaller} indirectly, through {@link EPSGFactory#install(Connection)}.
 * We do not test {@code EPSGInstaller} directly because the EPSG database creation is costly,
 * and we want to use the {@code EPSGFactory} for creating a few CRS for testing purpose.
 *
 * <p>Every databases created by this test suite exist only in memory.
 * This class does not write anything to disk (except maybe some temporary files).</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
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
        assumeTrue(scripts.getAuthorities().contains(Constants.EPSG));
        return scripts;
    }

    /**
     * Tests the creation of an EPSG database on Derby.
     * This test is skipped if Derby/JavaDB is not found, or if the SQL scripts are not found.
     *
     * @throws Exception if an error occurred while creating the database.
     */
    @Test
    public void testCreationOnDerby() throws Exception {
        final InstallationScriptProvider scripts = getScripts();            // Needs to be invoked first.
        final DataSource ds = TestDatabase.create("test");
        try {
            createAndTest(ds, scripts);
        } finally {
            TestDatabase.drop(ds);
        }
        loggings.assertNextLogContains("EPSG", "jdbc:derby:memory:test");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the creation of an EPSG database on HSQLDB.
     * This test is skipped if the SQL scripts are not found.
     *
     * <p>This test is skipped by default because HSQLDB changes the {@code java.util.logging} configuration,
     * which causes failures in all Apache SIS tests that verify the logging messages after execution of this
     * test. This impact this {@code EPSGInstallerTest} class, but also other test classes.</p>
     *
     * @throws Exception if an error occurred while creating the database.
     */
    @Test
    @Ignore("Skipped for protecting java.util.logging configuration against changes.")
    public void testCreationOnHSQLDB() throws Exception {
        final InstallationScriptProvider scripts = getScripts();            // Needs to be invoked first.
        final DataSource ds = (DataSource) Class.forName("org.hsqldb.jdbc.JDBCDataSource").newInstance();
        ds.getClass().getMethod("setURL", String.class).invoke(ds, "jdbc:hsqldb:mem:test");
        try {
            createAndTest(ds, scripts);
        } finally {
            final Connection c = ds.getConnection(); Statement s = c.createStatement();
            try {
                s.execute("SHUTDOWN");
            } finally {
                c.close();
            }
        }
        loggings.assertNextLogContains("EPSG", "jdbc:hsqldb:mem:test");
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Requests the "WGS84" and the "WGS72 / UTM zone 15N" coordinate reference systems from the EPSG database
     * at the given {@code DataSource}. Those requests should trig the creation of the EPSG database.
     */
    private void createAndTest(final DataSource ds, final InstallationScriptProvider scriptProvider)
            throws SQLException, FactoryException
    {
        final Map<String,Object> properties = new HashMap<String,Object>();
        assertNull(properties.put("dataSource", ds));
        assertNull(properties.put("scriptProvider", scriptProvider));
        assertEquals("Should not contain EPSG tables before we created them.", 0, countCRSTables(ds));
        loggings.assertNoUnexpectedLog();       // Should not yet have logged anything at this point.

        final EPSGFactory factory = new EPSGFactory(properties);
        try {
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
            assertTrue(Utilities.deepEquals(CommonCRS.WGS72.UTM(1, -93), p, ComparisonMode.DEBUG));
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
             * should not be included. The intend is to verify that the fields
             * of type BOOLEAN have been properly handled.
             */
            codes = new HashSet<String>(codes);
            assertTrue ("4979", codes.contains("4979"));
            assertFalse("4329", codes.contains("4329"));
        } finally {
            factory.close();
        }
        assertEquals("Should contain EPSG tables after we created them.", 1, countCRSTables(ds));
    }

    /**
     * Counts the number of {@code EPSG."Coordinate Reference System"} tables.
     * It should be 0 or 1. Any schema other than "EPSG" causes a test failure.
     */
    private static int countCRSTables(final DataSource ds) throws SQLException {
        int count = 0;
        final Connection c = ds.getConnection();
        try {
            final ResultSet r = c.getMetaData().getTables(null, null, "Coordinate Reference System", null);
            try {
                while (r.next()) {
                    final String schema = r.getString("TABLE_SCHEM");
                    assertTrue(schema, "EPSG".equalsIgnoreCase(schema));
                    count++;
                }
            } finally {
                r.close();
            }
        } finally {
            c.close();
        }
        return count;
    }
}
