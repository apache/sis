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
package org.apache.sis.storage.sql;

// Test dependencies
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.metadata.sql.TestDatabase;


/**
 * Base class of tests to be executed on all supported databases.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
public abstract class TestOnAllDatabases extends TestCase {
    /**
     * The schema where will be stored the features to test.
     */
    public static final String SCHEMA = "features";

    /**
     * Creates a new test case.
     */
    protected TestOnAllDatabases() {
    }

    /**
     * Tests on Derby.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testOnDerby() throws Exception {
        try (TestDatabase database = TestDatabase.create("SQLStore")) {
            test(database, true);
        }
    }

    /**
     * Tests on HSQLDB.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testOnHSQLDB() throws Exception {
        try (TestDatabase database = TestDatabase.createOnHSQLDB("SQLStore", true)) {
            test(database, true);
        }
    }

    /**
     * Tests on H2.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testOnH2() throws Exception {
        try (TestDatabase database = TestDatabase.createOnH2("SQLStore")) {
            test(database, true);
        }
    }

    /**
     * Tests on PostgreSQL.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testOnPostgreSQL() throws Exception {
        try (TestDatabase database = TestDatabase.createOnPostgreSQL(SCHEMA, true)) {
            test(database, false);
        }
    }

    /**
     * Runs all tests on a single database software.
     *
     * @param  database  factory for creating a test database.
     * @param  noschema  whether the test database is created without schema.
     * @throws Exception if an error occurred while executing the test.
     */
    protected abstract void test(final TestDatabase database, final boolean noschema) throws Exception;
}
