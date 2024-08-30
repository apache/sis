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

import java.util.List;
import org.apache.sis.storage.StorageConnector;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.metadata.sql.TestDatabase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.storage.sql.feature.InfoStatementsTest;


/**
 * Tests {@link DataAccess}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DataAccessTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DataAccessTest() {
    }

    /**
     * Tests on Derby.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testOnDerby() throws Exception {
        try (TestDatabase database = TestDatabase.create("SQL-DataAccess")) {
            test(database);
        }
    }

    /**
     * Runs all tests on a single database software.
     *
     * @param  database  factory for creating a test database.
     * @throws Exception if an error occurred while executing the test.
     */
    private void test(final TestDatabase database) throws Exception {
        database.executeSQL(List.of(InfoStatementsTest.createSpatialRefSys()));
        try (SQLStore store = new SimpleFeatureStore(null, new StorageConnector(database.source), ResourceDefinition.table("%"));
             DataAccess dao = store.newDataAccess(true))
        {
            assertEquals(4326, dao.findSRID(HardCodedCRS.WGS84));
        }
    }
}
