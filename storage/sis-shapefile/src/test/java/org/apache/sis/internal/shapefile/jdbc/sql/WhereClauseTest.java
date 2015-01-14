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
package org.apache.sis.internal.shapefile.jdbc.sql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.sql.*;

import org.apache.sis.internal.shapefile.jdbc.AbstractTestBaseForInternalJDBC;
import org.apache.sis.internal.shapefile.jdbc.resultset.DBFRecordBasedResultSet;
import org.junit.Test;

/**
 * Testing of the WHERE clause in SQL Statements.
 */
public class WhereClauseTest extends AbstractTestBaseForInternalJDBC {
    /**
     * Test operators.
     * @throws SQLException if a trouble occurs : all tests shall pass.
     */
    @Test
    public void operators() throws SQLException {
        Connection connection = connect();
        Statement stmt = connection.createStatement();
        DBFRecordBasedResultSet rs = (DBFRecordBasedResultSet)stmt.executeQuery("SELECT * FROM SignedBikeRoute");
        try {
            rs.next();

            assertTrue("FNODE_ = 1199", new ConditionalClauseResolver("FNODE_", 1199L, "=").isVerified(rs));
            assertFalse("FNODE_ > 1199", new ConditionalClauseResolver("FNODE_", 1199L, ">").isVerified(rs));
            assertFalse("FNODE_ < 1199", new ConditionalClauseResolver("FNODE_", 1199L, "<").isVerified(rs));
            assertTrue("FNODE_ >= 1199", new ConditionalClauseResolver("FNODE_", 1199L, ">=").isVerified(rs));
            assertTrue("FNODE_ <= 1199", new ConditionalClauseResolver("FNODE_", 1199L, "<=").isVerified(rs));

            assertTrue("FNODE_ > 1198", new ConditionalClauseResolver("FNODE_", 1198L, ">").isVerified(rs));
            assertFalse("FNODE_ < 1198", new ConditionalClauseResolver("FNODE_", 1198L, "<").isVerified(rs));
            assertTrue("FNODE_ >= 1198", new ConditionalClauseResolver("FNODE_", 1198L, ">=").isVerified(rs));
            assertFalse("FNODE_ <= 1198", new ConditionalClauseResolver("FNODE_", 1198L, "<=").isVerified(rs));

            assertFalse("FNODE_ > 1200", new ConditionalClauseResolver("FNODE_", 1200L, ">").isVerified(rs));
            assertTrue("FNODE_ < 1200", new ConditionalClauseResolver("FNODE_", 1200L, "<").isVerified(rs));
            assertFalse("FNODE_ >= 1200", new ConditionalClauseResolver("FNODE_", 1200L, ">=").isVerified(rs));
            assertTrue("FNODE_ <= 1200", new ConditionalClauseResolver("FNODE_", 1200L, "<=").isVerified(rs));

            assertTrue("ST_NAME = '36TH ST'", new ConditionalClauseResolver("ST_NAME", "'36TH ST'", "=").isVerified(rs));

            assertTrue("SHAPE_LEN = 43.0881492571", new ConditionalClauseResolver("SHAPE_LEN", 43.0881492571, "=").isVerified(rs));
            assertTrue("SHAPE_LEN > 43.088", new ConditionalClauseResolver("SHAPE_LEN", 43.088, ">").isVerified(rs));
            assertFalse("SHAPE_LEN < 43.0881492571", new ConditionalClauseResolver("SHAPE_LEN", 43.0881492571, "<").isVerified(rs));
        } finally {
            rs.close();
            stmt.close();
            connection.close();
        }
    }

    /**
     * Test where conditions : field [operator] integer.
     * @throws SQLException if a trouble occurs : all tests shall pass.
     */
    @Test
    public void whereCondition_field_literal_int() throws SQLException {
        checkAndCount("FNODE_ < 2000", new ResultSetPredicate<ResultSet>() {
            @Override public boolean test(ResultSet rs) throws SQLException {
                return rs.getInt("FNODE_") < 2000;
            }
        }, 3);
    }

    /**
     * Test where conditions : field [operator] integer.
     * @throws SQLException if a trouble occurs : all tests shall pass.
     */
    @Test
    public void whereCondition_field_literal_double() throws SQLException {
        checkAndCount("SHAPE_LEN < 70.5", new ResultSetPredicate<ResultSet>() {
            @Override public boolean test(ResultSet rs) throws SQLException {
                return rs.getDouble("SHAPE_LEN") < 70.5;
            }
        }, 3);
    }

    /**
     * Test where conditions : field [operator] String value.
     * @throws SQLException if a trouble occurs : all tests shall pass.
     */
    @Test
    public void whereCondition_field_literal_string() throws SQLException {
        checkAndCount("FNAME = '36TH'", new ResultSetPredicate<ResultSet>() {
            @Override public boolean test(ResultSet rs) throws SQLException {
                return rs.getString("FNAME").equals("36TH");
            }
        }, 1);
    }

    /**
     * Test where conditions : field [operator] field.
     * @throws SQLException if a trouble occurs : all tests shall pass.
     */
    @Test
    public void whereCondition_field_field() throws SQLException {
        checkAndCount("FNODE_ < TNODE_", new ResultSetPredicate<ResultSet>() {
            @Override public boolean test(ResultSet rs) throws SQLException {
                return rs.getInt("FNODE_") < rs.getInt("TNODE_");
            }
        }, 1);
    }

    /**
     * Trick suggested by AdiGuba (Forum des développeurs) to avoid the exception thrown by ResultSet:getInt(),
     * unhandlable by a simple Predicate.
     * @param <T> Type used.
     */
    public interface ResultSetPredicate<T> {
        /**
         * Test a condition.
         * @param condition Condition.
         * @return true is the condition passed.
         * @throws SQLException if a trouble occurs.
         */
        boolean test(T condition) throws SQLException;
    }

    /**
     * Check that all records match the conditions and count them.
     * @param whereCondition The where condition to add to a "SELECT * FROM SignedBikeRoute WHERE " statement.
     * @param condition Condition.
     * @param countExpected Count Expected, -1 if you don't want to count them.
     * @throws SQLException if a trouble occurs : all tests shall pass.
     */
    private void checkAndCount(String whereCondition, ResultSetPredicate<ResultSet> condition, int countExpected) throws SQLException {
        String sql = "SELECT * FROM SignedBikeRoute WHERE " + whereCondition;

        Connection connection = connect();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        try {
            int count = 0;

            while(rs.next()) {
                count ++;
                assertTrue(sql, condition.test(rs));
            }

            if (countExpected != -1)
                assertEquals("Wrong number of records red by : " + sql, countExpected, count);
        } finally {
            rs.close();
            stmt.close();
            connection.close();
        }
    }
}
