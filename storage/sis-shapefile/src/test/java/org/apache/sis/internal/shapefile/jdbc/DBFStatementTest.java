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

import org.apache.sis.internal.shapefile.jdbc.statement.DBFStatement;
import org.apache.sis.test.DependsOnMethod;
import org.junit.*;

import static org.junit.Assert.*;


/**
 * Tests {@link DBFStatement}.
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class DBFStatementTest extends AbstractTestBaseForInternalJDBC {
    /**
     * Open and close a statement.
     * @throws SQLException if an error occurred while opening the database or the statement.
     */
    @Test
    public void openCloseStatement() throws SQLException {
        final Driver driver = new DBFDriver();

        Connection connection = driver.connect(this.dbfFile.getAbsolutePath(), null);
        try {
            final Statement stmt = connection.createStatement();
            assertFalse("Statement should be opened", stmt.isClosed());

            stmt.close();
            assertTrue ("Statement should be closed", stmt.isClosed());
        } finally {
            connection.close();
        }
    }

    /**
     * An attempt to use a closed statement must fail with the correct exception.
     * @throws SQLException if an error occurred while opening the database or the statement.
     */
    @Test
    @DependsOnMethod("openCloseStatement")
    public void statementClosed() throws SQLException {
        // Open a connection, open and close a statement.
        Connection connection = connect();
        try {
            final Statement stmt = connection.createStatement();
            stmt.close();

            // Then, attempt to use it.
            try {
                stmt.executeQuery("Must detect that the statement is closed, and not try to parse this query.");
            }
            catch(SQLConnectionClosedException e) {
                assertEquals("The database name in this exception is not well set.", e.getDatabase().getName(), this.dbfFile.getName());
            }
            catch(SQLException e) {
                fail("Not the expected exception for using a closed statement.");
            }
        } finally {
            connection.close();
        }

        // Same, but we close the connection instead.
        connection = connect();
        final Statement stmt = connection.createStatement();

        connection.close(); // At this time, you expect also a warning on the console, telling that you have one statement still opened.

        // Then, attempt to use it.
        try {
            stmt.executeQuery("Must detect that the statement is closed, and not try to parse this query.");
        }
        catch(SQLConnectionClosedException e) {
            assertEquals("The database name in this exception is not well set.", e.getDatabase().getName(), this.dbfFile.getName());
        }
        catch(SQLException e) {
            fail("Not the expected exception for using a closed statement.");
        }
        finally {
            stmt.close();
        }
    }
}
