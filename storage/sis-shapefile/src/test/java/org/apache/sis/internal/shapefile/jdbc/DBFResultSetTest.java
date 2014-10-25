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
package org.apache.sis.internal.shapefile.jdbc;

import java.sql.*;
import java.io.File;
import java.net.URL;
import java.net.URISyntaxException;
import org.apache.sis.storage.shapefile.Database;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.*;

import static org.junit.Assert.*;


/**
 * Tests {@link DBFResultSet}.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class DBFResultSetTest extends TestCase {
    /**
     * The database file to use for testing purpose.
     */
    private File dbfFile;

    /**
     * Test setup.
     *
     * @throws URISyntaxException If an error occurred while getting the file to the test database.
     */
    @Before
    public void setup() throws URISyntaxException {
        final URL url = Database.class.getResource("SignedBikeRoute_4326_clipped.dbf");
        assertNotNull("The database file used for testing doesn't exist.", url);
        dbfFile = new File(url.toURI());
        assertTrue(dbfFile.isFile());
    }

    /**
     * Open and close a connection.
     *
     * @throws SQLException if an error occurred while querying the database.
     */
    @Test
    public void openCloseConnection() throws SQLException {
        final Driver     driver     = new DBFDriver();
        final Connection connection = driver.connect(dbfFile.getAbsolutePath(), null);
        assertFalse("Connection should be opened", connection.isClosed());
        assertTrue ("Connection should be valid",  connection.isValid(0));

        connection.close();
        assertTrue ("Connection should be closed", connection.isClosed());
        assertFalse("Connection should no more be valid", connection.isValid(0));
    }

    /**
     * Reads the first record.
     *
     * @throws SQLException if an error occurred while querying the database.
     */
    @Test
    @DependsOnMethod("openCloseConnection")
    public void readFirstRecord() throws SQLException {
        final Driver     driver     = new DBFDriver();
        final Connection connection = driver.connect(dbfFile.getAbsolutePath(), null);
        final Statement  stmt       = connection.createStatement();
        final ResultSet  rs         = stmt.executeQuery("SELECT * FROM SignedBikeRoute");
        // We don't care currently of the request: we are returning everything each time.

        rs.next();
        assertEquals("getString(\"ST_NAME\")", "336TH ST", rs.getString("ST_NAME"));

        rs.close();
        stmt.close();
        connection.close();
    }

    /**
     * Read all the DBF records.
     *
     * @throws SQLException if an error occurred while querying the database.
     */
    @Test
    @DependsOnMethod("readFirstRecord")
    public void readAllRecords() throws SQLException {
        final Driver     driver     = new DBFDriver();
        final Connection connection = driver.connect(dbfFile.getAbsolutePath(), null);
        final Statement  stmt       = connection.createStatement();
        final ResultSet  rs         = stmt.executeQuery("SELECT * FROM SignedBikeRoute");
        // We don't care currently of the request: we are returning everything each time.

        int count = 0;
        while (rs.next()) {
            count++;
        }
        assertTrue("Less than one record was readed.", count > 1);

        rs.close();
        stmt.close();
        connection.close();
    }
}
