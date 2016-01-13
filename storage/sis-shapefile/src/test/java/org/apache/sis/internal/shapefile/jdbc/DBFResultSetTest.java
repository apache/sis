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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.*;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.apache.sis.internal.shapefile.jdbc.resultset.DBFRecordBasedResultSet;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;


/**
 * Tests {@link DBFRecordBasedResultSet}.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class DBFResultSetTest extends AbstractTestBaseForInternalJDBC {
    /**
     * Reads the first record.
     * @throws SQLException if an error occurred while querying the database.
     */
    @Test
    public void readFirstRecord() throws SQLException {
        Connection connection = connect();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM SignedBikeRoute");
        try {
            rs.next();
            assertEquals("getString(\"ST_NAME\")", "36TH ST", rs.getString("ST_NAME"));                                    // ST_NAME Character(29)
            assertEquals("getInt(\"FNODE_\")", 1199, rs.getInt("FNODE_"));                                                 // FNODE_ Number(10, 0)
            assertEquals("getDouble(\"SHAPE_LEN\")", 43.0881492571, rs.getDouble("SHAPE_LEN"), 0.1);                       // SHAPE_LEN Number(19, 11)
            assertEquals("getBigDecimal(\"SHAPE_LEN\")", 43.0881492571, rs.getBigDecimal("SHAPE_LEN").doubleValue(), 0.1); // SHAPE_LEN Number(19, 11)
            assertEquals("getDate(\"TR_DATE\")", null, rs.getDate("TR_DATE"));                       // TR_DATE Date(8)
        } finally {
            rs.close();
            stmt.close();
            connection.close();
        }
    }

    /**
     * Read all the DBF records.
     * @throws SQLException if an error occurred while querying the database.
     */
    @Test
    @DependsOnMethod("readFirstRecord")
    public void readAllRecords() throws SQLException {
        Connection connection = connect();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM SignedBikeRoute");
        try {
            int count = 0;

            while(rs.next()) {
                ArrayList<Object> record = new ArrayList<Object>();

                record.add(rs.getLong("OBJECTID"));         // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getLong("FNODE_"));           // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getLong("TNODE_"));           // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getLong("LPOLY_"));           // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getLong("RPOLY_"));           // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getLong("SCL_"));             // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getLong("SCL_ID"));           // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getLong("SCL_CODE"));         // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getString("DXF_LAYER"));      // Type : Character, Field length : 16, Decimal positions : 0
                record.add(rs.getString("SIS_ID"));         // Type : Character, Field length : 12, Decimal positions : 0
                record.add(rs.getString("QUAD_CODE"));      // Type : Character, Field length : 1, Decimal positions : 0
                record.add(rs.getString("PRIME_ST"));       // Type : Character, Field length : 4, Decimal positions : 0
                record.add(rs.getString("INT_SEQ"));        // Type : Character, Field length : 3, Decimal positions : 0
                record.add(rs.getString("ST_NAME"));        // Type : Character, Field length : 29, Decimal positions : 0
                record.add(rs.getString("FDPRE"));          // Type : Character, Field length : 2, Decimal positions : 0
                record.add(rs.getString("FNAME"));          // Type : Character, Field length : 30, Decimal positions : 0
                record.add(rs.getString("FTYPE"));          // Type : Character, Field length : 4, Decimal positions : 0
                record.add(rs.getString("FDSUF"));          // Type : Character, Field length : 2, Decimal positions : 0
                record.add(rs.getLong("LEFTRANGE1"));       // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getLong("LEFTRANGE2"));       // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getLong("RGTRANGE1"));        // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getLong("RGTRANGE2"));        // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getString("STREET"));         // Type : Character, Field length : 26, Decimal positions : 0
                record.add(rs.getString("QUAD"));           // Type : Character, Field length : 2, Decimal positions : 0
                record.add(rs.getString("FROM_ST"));        // Type : Character, Field length : 100, Decimal positions : 0
                record.add(rs.getString("TO_ST"));          // Type : Character, Field length : 100, Decimal positions : 0
                record.add(rs.getString("ODD_WARD"));       // Type : Character, Field length : 1, Decimal positions : 0
                record.add(rs.getString("EVEN_WARD"));      // Type : Character, Field length : 1, Decimal positions : 0
                record.add(rs.getString("WARD"));           // Type : Character, Field length : 3, Decimal positions : 0
                record.add(rs.getString("FC"));             // Type : Character, Field length : 40, Decimal positions : 0
                record.add(rs.getInt("NHS"));               // Type : Number, Field length : 5, Decimal positions : 0
                record.add(rs.getString("SIGNEDRTE1"));     // Type : Character, Field length : 5, Decimal positions : 0
                record.add(rs.getString("RTETYPE1"));       // Type : Character, Field length : 10, Decimal positions : 0
                record.add(rs.getString("SIGNEDRTE2"));     // Type : Character, Field length : 5, Decimal positions : 0
                record.add(rs.getString("RTETYPE2"));       // Type : Character, Field length : 10, Decimal positions : 0
                record.add(rs.getBigDecimal("AADT"));       // Type : Number, Field length : 19, Decimal positions : 8
                record.add(rs.getBigDecimal("AADT_YEAR"));  // Type : Number, Field length : 19, Decimal positions : 8
                record.add(rs.getBigDecimal("COM_SING_P")); // Type : Number, Field length : 19, Decimal positions : 8
                record.add(rs.getBigDecimal("COM_SING_A")); // Type : Number, Field length : 19, Decimal positions : 8
                record.add(rs.getBigDecimal("COM_COMB_P")); // Type : Number, Field length : 19, Decimal positions : 8
                record.add(rs.getBigDecimal("COM_COMB_A")); // Type : Number, Field length : 19, Decimal positions : 8
                record.add(rs.getString("IS_ONEWAY"));      // Type : Number, Field length : 5, Decimal positions : 0
                record.add(rs.getString("TRAVEL_DIR"));     // Type : Character, Field length : 20, Decimal positions : 0
                record.add(rs.getBigDecimal("LEN_MI"));     // Type : Number, Field length : 19, Decimal positions : 8
                record.add(rs.getLong("STUDY_NET"));        // Type : Number, Field length : 10, Decimal positions : 0
                record.add(rs.getDate("TR_DATE"));          // Type : Date, Field length : 8, Decimal positions : 0
                record.add(rs.getBigDecimal("AADT_2"));     // Type : Number, Field length : 19, Decimal positions : 8
                record.add(rs.getBigDecimal("AADT_FINAL")); // Type : Number, Field length : 19, Decimal positions : 8
                record.add(rs.getBigDecimal("ROUTENET"));   // Type : Number, Field length : 19, Decimal positions : 8
                record.add(rs.getString("NOTES"));          // Type : Character, Field length : 50, Decimal positions : 0
                record.add(rs.getBigDecimal("LENGTH_MI"));  // Type : Number, Field length : 19, Decimal positions : 8
                record.add(rs.getInt("NET_MARCH"));         // Type : Number, Field length : 5, Decimal positions : 0
                record.add(rs.getString("SIGNED_JOI"));     // Type : Character, Field length : 5, Decimal positions : 0
                record.add(rs.getString("SIGNED_FAC"));     // Type : Character, Field length : 30, Decimal positions : 0
                record.add(rs.getString("NEW_USE"));        // Type : Character, Field length : 30, Decimal positions : 0
                record.add(rs.getBigDecimal("SHAPE_LEN"));  // Type : Number, Field length : 19, Decimal positions : 11

                count ++;
                assertEquals("The record number returned by the ResultSet is not the same of the manual counting we are doing." , count, ((DBFRecordBasedResultSet)rs).getRowNum());
                this.log.info(MessageFormat.format("Record {0,number} : {1}\n", count, record));
            }

            assertTrue("Less than one record was readed.", count > 1);
        } finally {
            rs.close();
            stmt.close();
            connection.close();
        }
    }

    /**
     * An attempt to use a closed resultSet must fail with the correct exception and message.
     * @throws SQLException if an error occurred while opening the database, the statement or the resultset.
     */
    @Test
    public void resultSetClosed() throws SQLException {
        // 1) Open a connection, open a statement, open and close a ResultSet.
        String sql = "SELECT * FROM SignedBikeRoute";
        Connection connection = connect();
        Statement stmt = connection.createStatement();
        try {
            // Then, attempt to use it.
            try {
                ResultSet rs = stmt.executeQuery(sql);
                rs.close();
            }
            catch(SQLConnectionClosedException e) {
                assertEquals("The database name in this exception is not well set.", e.getDatabase().getName(), this.dbfFile.getName());
                assertEquals("The SQL Query is exception is not well set.", e.getSQL(), sql);
            }
            catch(SQLException e) {
                fail("Not the expected exception for using a closed ResultSet.");
            }
        } finally {
            stmt.close();
            connection.close();
        }

        // 2) Same, but we close the connection instead.
        connection = connect();
        stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        connection.close(); // At this time, you expect also a warning on the console, telling that you have one statement and one ResultSet still opened.

        // Then, attempt to use it.
        try {
            rs.next();
        }
        catch(SQLConnectionClosedException e) {
            assertEquals("The database name is exception message is not well set.", e.getDatabase().getName(), this.dbfFile.getName());
        }
        catch(SQLException e) {
            fail("Not the expected exception for using a closed ResultSet.");
        }
        finally {
            rs.close();
            stmt.close();
        }

        // 3) Same, but we close the statement instead .
        Connection cnt = connect();
        try {
            stmt = cnt.createStatement();
            rs = stmt.executeQuery(sql);

            stmt.close(); // At this time, you expect also a information message on the console, telling that the statement has closed its current ResultSet.

            // Then, attempt to use it.
            try {
                rs.next();
            }
            catch(SQLConnectionClosedException e) {
                assertEquals("The database name is exception message is not well set.", e.getDatabase().getName(), this.dbfFile.getName());
            }
            catch(SQLException e) {
                fail("Not the expected exception for using a closed ResultSet.");
            }
            finally {
                rs.close();
                stmt.close();
            }
        } finally {
            cnt.close();
        }
    }
}
