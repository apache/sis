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

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.sql.TestDatabase;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultAssociationRole;


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
     * The schema where will be stored the features to test.
     */
    private static final String SCHEMA = "features";

    /**
     * Number of time that the each country has been seen while iterating over the cities.
     */
    private final Map<String,Integer> countryCount = new HashMap<>();

    /**
     * The {@code Country} value for Canada, or {@code null} if not yet visited.
     * This feature should appear twice, and all those occurrences should use the exact same instance.
     * We use that for verifying the {@code Table.instanceForPrimaryKeys} caching.
     */
    private AbstractFeature canada;

    /**
     * Tests on Derby.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testOnDerby() throws Exception {
        test(TestDatabase.create("SQLStore"), true);
    }

    /**
     * Tests on HSQLDB.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testOnHSQLDB() throws Exception {
        test(TestDatabase.createOnHSQLDB("SQLStore", true), true);
    }

    /**
     * Tests on PostgreSQL.
     *
     * @throws Exception if an error occurred while testing the database.
     */
    @Test
    public void testOnPostgreSQL() throws Exception {
        test(TestDatabase.createOnPostgreSQL(SCHEMA, true), false);
    }

    /**
     * Tests reading an existing schema. The schema is created and populated by the {@code Features.sql} script.
     *
     * @param  inMemory  where the test database is in memory. If {@code true}, then the database is presumed
     *                   initially empty: a schema will be created, and we assume that there is no ambiguity
     *                   if we don't specify the schema in {@link SQLStore} constructor.
     */
    private void test(final TestDatabase database, final boolean inMemory) throws Exception {
        final String[] scripts = {
            "CREATE SCHEMA " + SCHEMA + ';',
            "file:Features.sql"
        };
        if (!inMemory) {
            scripts[0] = null;      // Erase the "CREATE SCHEMA" statement if the schema already exists.
        }
        try (TestDatabase tmp = database) {
            tmp.executeSQL(SQLStoreTest.class, scripts);
            try (SQLStore store = new SQLStore(new SQLStoreProvider(), new StorageConnector(tmp.source),
                    SQLStoreProvider.createTableName(null, inMemory ? null : SCHEMA, "Cities")))
            {
                final FeatureSet cities = (FeatureSet) store.findResource("Cities");
                verifyFeatureType(cities.getType(),
                        new String[] {"sis:identifier", "pk:country", "country",   "native_name", "english_name", "population",  "parks"},
                        new Object[] {null,             String.class, "Countries", String.class,  String.class,   Integer.class, "Parks"});

                verifyFeatureType(((FeatureSet) store.findResource("Countries")).getType(),
                        new String[] {"sis:identifier", "code",       "native_name"},
                        new Object[] {null,             String.class, String.class});

                verifyFeatureType(((FeatureSet) store.findResource("Parks")).getType(),
                        new String[] {"sis:identifier", "pk:country", "FK_City", "city",       "native_name", "english_name"},
                        new Object[] {null,             String.class, "Cities",  String.class, String.class,  String.class});

                try (Stream<AbstractFeature> features = cities.features(false)) {
                    features.forEach((f) -> verifyContent(f));
                }
            }
        }
        assertEquals(Integer.valueOf(2), countryCount.remove("CAN"));
        assertEquals(Integer.valueOf(1), countryCount.remove("FRA"));
        assertEquals(Integer.valueOf(1), countryCount.remove("JPN"));
        assertTrue  (countryCount.isEmpty());
    }

    /**
     * Verifies the result of analyzing the structure of the {@code "Cities"} table.
     */
    private static void verifyFeatureType(final DefaultFeatureType type, final String[] expectedNames, final Object[] expectedTypes) {
        int i = 0;
        for (AbstractIdentifiedType pt : type.getProperties(false)) {
            assertEquals("name", expectedNames[i], pt.getName().toString());
            final Object expectedType = expectedTypes[i];
            if (expectedType != null) {
                final String label;
                final Object value;
                if (expectedType instanceof Class<?>) {
                    label = "attribute type";
                    value = ((DefaultAttributeType<?>) pt).getValueClass();
                } else {
                    label = "association type";
                    value = ((DefaultAssociationRole) pt).getValueType().getName().toString();
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
    private void verifyContent(final AbstractFeature feature) {
        final String city = feature.getPropertyValue("native_name").toString();
        final String country, countryName, englishName;
        final String[] parks;
        final int population;
        boolean isCanada = false;
        switch (city) {
            case "東京": {
                englishName = "Tōkyō";
                country     = "JPN";
                countryName = "日本";
                population  = 13622267;         // In 2016.
                parks       = new String[] {"Yoyogi-kōen", "Shinjuku Gyoen"};
                break;
            }
            case "Paris": {
                englishName = "Paris";
                country     = "FRA";
                countryName = "France";
                population  = 2206488;          // In 2017.
                parks       = new String[] {"Tuileries Garden", "Luxembourg Garden"};
                break;
            }
            case "Montréal": {
                englishName = "Montreal";
                country     = "CAN";
                countryName = "Canada";
                population  = 1704694;          // In 2016.
                isCanada    = true;
                parks       = new String[] {"Mount Royal"};
                break;
            }
            case "Québec": {
                englishName = "Quebec";
                country     = "CAN";
                countryName = "Canada";
                population  = 531902;           // In 2016.
                isCanada    = true;
                parks = new String[] {};
                break;
            }
            default: {
                fail("Unexpected feature: " + city);
                return;
            }
        }
        /*
         * Verify attributes. They are the easiest properties to read.
         */
        assertEquals("pk:country",     country,              feature.getPropertyValue("pk:country"));
        assertEquals("sis:identifier", country + ':' + city, feature.getPropertyValue("sis:identifier"));
        assertEquals("english_name",   englishName,          feature.getPropertyValue("english_name"));
        assertEquals("population",     population,           feature.getPropertyValue("population"));
        /*
         * Associations using Relation.Direction.IMPORT.
         * Those associations should be cached; we verify with "Canada" case.
         */
        assertEquals("country", countryName, getIndirectPropertyValue(feature, "country", "native_name"));
        if (isCanada) {
            final AbstractFeature f = (AbstractFeature) feature.getPropertyValue("country");
            if (canada == null) {
                canada = f;
            } else {
                assertSame(canada, f);              // Want exact same feature instance, not just equal.
            }
        }
        countryCount.merge(country, 1, (o, n) -> n+1);
        /*
         * Associations using Relation.Direction.EXPORT.
         * Contrarily to the IMPORT case, those associations can contain many values.
         */
        final Collection<?> actualParks = (Collection<?>) feature.getPropertyValue("parks");
        assertNotNull("parks", actualParks);
        assertEquals("parks.length", parks.length, actualParks.size());
        final Collection<String> expectedParks = new HashSet<>(Arrays.asList(parks));
        for (final Object park : actualParks) {
            final AbstractFeature pf = (AbstractFeature) park;
            final String npn = (String) pf.getPropertyValue("native_name");
            final String epn = (String) pf.getPropertyValue("english_name");
            assertNotNull("park.native_name",  npn);
            assertNotNull("park.english_name", epn);
            assertNotEquals("park.names", npn, epn);
            assertTrue("park.english_name", expectedParks.remove(epn));
            /*
             * Verify the reverse association form Parks to Cities.
             * This create a cyclic graph, but SQLStore is capable to handle it.
             */
            assertSame("City → Park → City", feature, pf.getPropertyValue("FK_City"));
        }
    }

    /**
     * Follows an association in the given feature.
     */
    private static Object getIndirectPropertyValue(final AbstractFeature feature, final String p1, final String p2) {
        final Object dependency = feature.getPropertyValue(p1);
        assertNotNull(p1, dependency);
        assertInstanceOf(p1, AbstractFeature.class, dependency);
        return ((AbstractFeature) dependency).getPropertyValue(p2);
    }
}
