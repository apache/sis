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

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;


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
                final FeatureSet cities = (FeatureSet) store.findResource("Cities");
                final String[] expectedNames = {"sis:identifier", "pk:country", "country",   "native_name", "translation", "population",  "parks"};
                final Object[] expectedTypes = {null,             String.class, "Countries", String.class,  String.class,  Integer.class, "Parks"};
                int i = 0;
                for (PropertyType pt : cities.getType().getProperties(false)) {
                    assertEquals("name", expectedNames[i], pt.getName().toString());
                    final Object expectedType = expectedTypes[i];
                    if (expectedType != null) {
                        final String label;
                        final Object value;
                        if (expectedType instanceof Class<?>) {
                            label = "attribute type";
                            value = ((AttributeType<?>) pt).getValueClass();
                        } else {
                            label = "association type";
                            value = ((FeatureAssociationRole) pt).getValueType().getName().toString();
                        }
                        assertEquals(label, expectedType, value);
                    }
                    i++;
                }
                assertEquals("count", expectedNames.length, i);
            }
        }
    }
}
