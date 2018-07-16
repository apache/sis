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

import java.util.stream.Stream;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.sql.TestDatabase;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
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
                verifyCityType(cities.getType());
                try (Stream<Feature> features = cities.features(false)) {
                    features.forEach(SQLStoreTest::verifyContent);
                }
            }
        }
    }

    /**
     * Verifies the result of analyzing the structure of the {@code "Cities"} table.
     */
    private static void verifyCityType(final FeatureType cities) {
        final String[] expectedNames = {"sis:identifier", "pk:country", "country",   "native_name", "translation", "population",  "parks"};
        final Object[] expectedTypes = {null,             String.class, "Countries", String.class,  String.class,  Integer.class, "Parks"};
        int i = 0;
        for (PropertyType pt : cities.getProperties(false)) {
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

    /**
     * Verifies the content of the {@code Cities} table.
     * The features are in no particular order.
     */
    private static void verifyContent(final Feature feature) {
        final String city = feature.getPropertyValue("native_name").toString();
        final String country, countryName, cityLatin;
        final int population;
        switch (city) {
            case "東京": {
                cityLatin   = "Tōkyō";
                country     = "JPN";
                countryName = "日本";
                population  = 13622267;         // In 2016.
                break;
            }
            case "Paris": {
                cityLatin   = "Paris";
                country     = "FRA";
                countryName = "France";
                population  = 2206488;          // In 2017.
                break;
            }
            case "Montréal": {
                cityLatin   = "Montreal";
                country     = "CAN";
                countryName = "Canada";
                population  = 1704694;          // In 2016.
                break;
            }
            case "Québec": {
                cityLatin   = "Quebec";
                country     = "CAN";
                countryName = "Canada";
                population  = 531902;           // In 2016.
                break;
            }
            default: {
                fail("Unexpected feature: " + city);
                return;
            }
        }
        // Attributes
        assertEquals("pk:country",     country,              feature.getPropertyValue("pk:country"));
        assertEquals("sis:identifier", country + ':' + city, feature.getPropertyValue("sis:identifier"));
        assertEquals("translation",    cityLatin,            feature.getPropertyValue("translation"));
        assertEquals("population",     population,           feature.getPropertyValue("population"));

        // Associations
        assertEquals("country", countryName, getIndirectPropertyValue(feature, "country", "native_name"));
    }

    /**
     * Follows an association in the given feature.
     */
    private static Object getIndirectPropertyValue(final Feature feature, final String p1, final String p2) {
        final Object dependency = feature.getPropertyValue(p1);
        assertNotNull(p1, dependency);
        assertInstanceOf(p1, Feature.class, dependency);
        return ((Feature) dependency).getPropertyValue(p2);
    }
}
