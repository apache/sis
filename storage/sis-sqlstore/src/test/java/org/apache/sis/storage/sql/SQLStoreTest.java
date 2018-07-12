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

import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.sql.TestDatabase;
import org.apache.sis.test.TestCase;
import org.junit.Test;


/**
 * Tests {@link SQLStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class SQLStoreTest extends TestCase {
    /**
     * Tests reading an existing schema. The schema is created and populated by the {@code Features.sql} script.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testReadStructure() throws Exception {
        try (TestDatabase tmp = TestDatabase.createOnPostgreSQL("features", true)) {
            tmp.executeSQL(SQLStoreTest.class, "Features.sql");
            try (SQLStore store = new SQLStore(new SQLStoreProvider(), new StorageConnector(tmp.source),
                    SQLStoreProvider.createTableName(null, "features", "Cities")))
            {
                System.out.println(store.getMetadata());
                final FeatureSet cities = (FeatureSet) store.findResource("Cities");
                System.out.println(cities.getType());
            }
        }
    }
}
