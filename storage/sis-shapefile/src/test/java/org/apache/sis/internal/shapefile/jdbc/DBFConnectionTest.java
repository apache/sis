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
import java.util.Properties;

import org.apache.sis.internal.shapefile.jdbc.connection.DBFConnection;
import org.apache.sis.test.DependsOnMethod;
import org.junit.*;

import static org.junit.Assert.*;


/**
 * Tests {@link DBFConnection}.
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class DBFConnectionTest extends AbstractTestBaseForInternalJDBC {
    /**
     * Open and close a connection.
     * @throws SQLException if an error occurred while opening the database.
     */
    @Test
    public void openCloseConnection() throws SQLException {
        final Driver driver = new DBFDriver();
        final Connection connection = driver.connect(this.dbfFile.getAbsolutePath(), null);
        assertFalse("Connection should be opened", connection.isClosed());
        assertTrue ("Connection should be valid",  connection.isValid(0));

        connection.close();
        assertTrue ("Connection should be closed", connection.isClosed());
        assertFalse("Connection should no more be valid", connection.isValid(0));
    }

    /**
     * Open and close a connection.
     * @throws SQLException if an error occurred while opening the database.
     */
    @Test
    public void openCloseConnectionWithAnotherCharset() throws SQLException {
        Properties info = new Properties();
        info.put("record_charset", "UTF-8");
        
        final Driver driver = new DBFDriver();
        final Connection connection = driver.connect(this.dbfFile.getAbsolutePath(), info);
        assertFalse("Connection should be opened", connection.isClosed());
        assertTrue ("Connection should be valid",  connection.isValid(0));

        connection.close();
        assertTrue ("Connection should be closed", connection.isClosed());
        assertFalse("Connection should no more be valid", connection.isValid(0));
    }

    /**
     * An attempt to use a closed connection must fail with the correct exception.
     * @throws SQLException if an error occurred while opening the database.
     */
    @Test(expected=SQLConnectionClosedException.class)
    @DependsOnMethod("openCloseConnection")
    public void connectionClosed() throws SQLException {
        // Open and close an connection.
        final Driver driver = new DBFDriver();
        final Connection connection = driver.connect(this.dbfFile.getAbsolutePath(), null);
        connection.close();

        // Then, attempt to use it.
        try {
            connection.createStatement();
        } catch(SQLConnectionClosedException e) {
            assertEquals("The database name in this exception is not well set.", e.getDatabase().getName(), this.dbfFile.getName());
            throw e;
        }
    }
}
