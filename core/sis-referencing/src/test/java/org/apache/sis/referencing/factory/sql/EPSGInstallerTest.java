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
import java.util.HashMap;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.internal.metadata.sql.TestDatabase;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Path;


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
     * Tests the creation of an EPSG database on Derby.
     * This test is skipped if no Derby or JavaDB driver has been found.
     *
     * @throws Exception if an error occurred while creating the database.
     */
    @Test
    public void testCreationOnDerby() throws Exception {
        final Path scripts = TestDatabase.directory("ExternalSources");
        final DataSource ds = TestDatabase.create("test");
        try {
            createAndTest(ds, scripts);
        } finally {
            TestDatabase.drop(ds);
        }
    }

    /**
     * Requests the WGS84 coordinate reference system from the EPSG database at the given source.
     * It should trig the creation of the EPSG database.
     */
    private static void createAndTest(final DataSource ds, final Path scripts) throws SQLException, FactoryException {
        final Map<String,Object> properties = new HashMap<String,Object>();
        assertNull(properties.put("dataSource", ds));
        assertNull(properties.put("scriptDirectory", scripts));
        assertEquals("Should not contain EPSG tables before we created them.", 0, countCRSTables(ds));
        final EPSGFactory factory = new EPSGFactory(properties);
        try {
            final GeographicCRS crs = factory.createGeographicCRS("4326");
            assertTrue(Utilities.deepEquals(CommonCRS.WGS84.geographic(), crs, ComparisonMode.DEBUG));
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
