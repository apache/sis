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
package org.apache.sis.metadata.sql;

import java.sql.Statement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.sis.internal.metadata.sql.SQLBuilder;
import org.apache.sis.internal.metadata.sql.TestDatabase;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Creates an empty database and insert automatically-generated keys.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class IdentifierGeneratorTest extends TestCase {
    /**
     * The name of the table to be created for testing purpose.
     */
    private static final String TABLE = "Dummy";

    /**
     * The generator being tested.
     */
    private IdentifierGenerator generator;

    /**
     * A statement to be used for various usage.
     */
    private Statement stmt;

    /**
     * Tests the creation of identifiers with sequence numbers.
     *
     * @throws Exception if an error occurred while reading or writing in the temporary database.
     */
    @Test
    public void testSequence() throws Exception {
        final DataSource ds = TestDatabase.create("IdentifierGenerator");
        try {
            final MetadataSource source = new MetadataSource(MetadataStandard.ISO_19115, ds, null, null);
            synchronized (source) {
                stmt = source.connection().createStatement();
                stmt.executeUpdate("CREATE TABLE \"" + TABLE + "\" (ID VARCHAR(6) NOT NULL PRIMARY KEY)");
                generator = new IdentifierGenerator(source, null, TABLE, "ID",
                        new SQLBuilder(source.connection().getMetaData(), false));
                /*
                 * Actual tests.
                 */
                addRecords("TD", 324);
                removeAndAddRecords("TD");
                addRecords("OT", 30);
                /*
                 * Cleaning.
                 */
                stmt.executeUpdate("DROP TABLE \"" + TABLE + '"');
                stmt.close();
                generator.close();
                source.close();
            }
        } finally {
            TestDatabase.drop(ds);
        }
    }

    /**
     * Adds a single record.
     *
     * @param  prefix The prefix of the record to add.
     * @return The identifier of the record added.
     */
    private String addRecord(final String prefix) throws SQLException {
        final String identifier = generator.identifier(prefix);
        assertEquals(1, stmt.executeUpdate("INSERT INTO \"" + TABLE + "\" VALUES ('" + identifier + "')"));
        return identifier;
    }

    /**
     * Tests the creation of identifiers with sequence numbers.
     *
     * @param prefix The prefix of the records to add.
     * @param count The number of records to add (in addition of the "main" one).
     */
    private void addRecords(final String prefix, final int count) throws SQLException {
        assertEquals("The very first record added should not have any suffix.", prefix, addRecord(prefix));
        for (int i=1; i<=count; i++) {
            assertEquals("Any record added after the first one should have a sequential number in suffix.",
                    prefix + IdentifierGenerator.SEPARATOR + i, addRecord(prefix));
        }
    }

    /**
     * Tries to remove a few pre-selected record, then add them again.
     */
    private void removeAndAddRecords(final String prefix) throws SQLException {
        assertEquals(5, stmt.executeUpdate("DELETE FROM \"" + TABLE + "\" WHERE " +
                "ID='" + prefix + IdentifierGenerator.SEPARATOR +   "4' OR " +
                "ID='" + prefix + IdentifierGenerator.SEPARATOR +  "12' OR " +
                "ID='" + prefix + IdentifierGenerator.SEPARATOR +  "32' OR " +
                "ID='" + prefix + IdentifierGenerator.SEPARATOR + "125' OR " +
                "ID='" + prefix + IdentifierGenerator.SEPARATOR + "224'"));
        assertEquals("12 is before 4 in alphabetical order.",    prefix+"-12",  addRecord(prefix));
        assertEquals("125 is next to 12 in alphabetical order.", prefix+"-125", addRecord(prefix));
        assertEquals("224 is before 32 in alphabetical order.",  prefix+"-224", addRecord(prefix));
        assertEquals("32 is before 4 in alphabetical order.",    prefix+"-32",  addRecord(prefix));
        assertEquals("4 is last in alphabetical order.",         prefix+"-4",   addRecord(prefix));
    }
}
