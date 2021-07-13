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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.internal.storage.query.FeatureQuery;
import org.apache.sis.test.sql.TestDatabase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.SortOrder;


/**
 * Tests {@link SQLStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class SQLStoreTest extends TestCase {
    /**
     * The schema where will be stored the features to test.
     */
    private static final String SCHEMA = "features";

    /**
     * Data used in the {@code Features.sql} test file.
     */
    public enum City {
        TOKYO   ("Tōkyō",    "JPN", "日本",     13622267, "Yoyogi-kōen", "Shinjuku Gyoen"),
        PARIS   ("Paris",    "FRA", "France",   2206488, "Tuileries Garden", "Luxembourg Garden"),
        MONTREAL("Montreal", "CAN", "Canada",   1704694, "Mount Royal"),
        QUEBEC  ("Quebec",   "CAN", "Canada",    531902);

        /** City name in Latin characters.   */ public final String englishName;
        /** Country ISO code (3 letters).    */ public final String country;
        /** Country name in native language. */ public final String countryName;
        /** The population in 2016 or 2017.  */ public final int    population;
        /** Some parks in the city.          */        final String[] parks;

        /** Creates a new enumeration value. */
        private City(String englishName, String country, String countryName, int population, String... parks) {
            this.englishName = englishName;
            this.country     = country;
            this.countryName = countryName;
            this.population  = population;
            this.parks       = parks;
        }
    };

    /**
     * Number of time that the each country has been seen while iterating over the cities.
     */
    private final Map<String,Integer> countryCount = new HashMap<>();

    /**
     * The {@code Country} value for Canada, or {@code null} if not yet visited.
     * This feature should appear twice, and all those occurrences should use the exact same instance.
     * We use that for verifying the {@code Table.instanceForPrimaryKeys} caching.
     */
    private Feature canada;

    /**
     * Factory to use for creating filter objects.
     */
    private final FilterFactory<Feature,Object,Object> FF;

    /**
     * Creates a new test.
     */
    public SQLStoreTest() {
        FF = DefaultFilterFactory.forFeatures();
    }

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
     * @param  inMemory  whether the test database is in memory. If {@code true}, then the database is presumed
     *                   initially empty: a schema will be created, and we assume that there is no ambiguity
     *                   if we don't specify the schema in {@link SQLStore} constructor.
     */
    private void test(final TestDatabase database, final boolean inMemory) throws Exception {
        final String[] scripts = {
            "CREATE SCHEMA " + SCHEMA + ';',
            "file:Features.sql"
        };
        if (!inMemory) {
            scripts[0] = null;      // Omit the "CREATE SCHEMA" statement if the schema already exists.
        }
        try (TestDatabase tmp = database) {                 // TODO: omit `tmp` with JDK16.
            tmp.executeSQL(SQLStoreTest.class, scripts);
            try (SQLStore store = new SQLStore(new SQLStoreProvider(), new StorageConnector(tmp.source),
                    SQLStoreProvider.createTableName(null, inMemory ? null : SCHEMA, "Cities")))
            {
                final FeatureSet cities = (FeatureSet) store.findResource("Cities");
                /*
                 * Feature properties should be in same order than columns in the database table, except for
                 * the generated identifier. Note that the country is an association to another feature.
                 */
                verifyFeatureType(cities.getType(),
                        new String[] {"sis:identifier", "pk:country", "country",   "native_name", "english_name", "population",  "parks"},
                        new Object[] {null,             String.class, "Countries", String.class,  String.class,   Integer.class, "Parks"});

                verifyFeatureType(((FeatureSet) store.findResource("Countries")).getType(),
                        new String[] {"sis:identifier", "code",       "native_name"},
                        new Object[] {null,             String.class, String.class});

                verifyFeatureType(((FeatureSet) store.findResource("Parks")).getType(),
                        new String[] {"sis:identifier", "pk:country", "FK_City", "city",       "native_name", "english_name"},
                        new Object[] {null,             String.class, "Cities",  String.class, String.class,  String.class});

                try (Stream<Feature> features = cities.features(false)) {
                    features.forEach((f) -> verifyContent(f));
                }
                /*
                 * Verify overloaded stream operations (sorting, etc.).
                 */
                verifySimpleQuerySorting(store);
                verifySimpleWhere(store);
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
    private static void verifyFeatureType(final FeatureType type, final String[] expectedNames, final Object[] expectedTypes) {
        int i = 0;
        for (PropertyType pt : type.getProperties(false)) {
            if (i >= expectedNames.length) {
                fail("Returned feature-type contains more properties than expected. Example: " + pt.getName());
            }
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
    private void verifyContent(final Feature feature) {
        final String city = feature.getPropertyValue("native_name").toString();
        final City c;
        boolean isCanada = false;
        switch (city) {
            case "東京":      c = City.TOKYO; break;
            case "Paris":    c = City.PARIS; break;
            case "Montréal": c = City.MONTREAL; isCanada = true; break;
            case "Québec":   c = City.QUEBEC;   isCanada = true; break;
            default: fail("Unexpected feature: " + city); return;
        }
        /*
         * Verify attributes. They are the easiest properties to read.
         */
        assertEquals("pk:country",     c.country,              feature.getPropertyValue("pk:country"));
        assertEquals("sis:identifier", c.country + ':' + city, feature.getPropertyValue("sis:identifier"));
        assertEquals("english_name",   c.englishName,          feature.getPropertyValue("english_name"));
        assertEquals("population",     c.population,           feature.getPropertyValue("population"));
        /*
         * Associations using Relation.Direction.IMPORT.
         * Those associations should be cached; we verify with "Canada" case.
         */
        assertEquals("country", c.countryName, getIndirectPropertyValue(feature, "country", "native_name"));
        if (isCanada) {
            final Feature f = (Feature) feature.getPropertyValue("country");
            if (canada == null) {
                canada = f;
            } else {
                assertSame(canada, f);              // Want exact same feature instance, not just equal.
            }
        }
        countryCount.merge(c.country, 1, (o, n) -> n+1);
        /*
         * Associations using Relation.Direction.EXPORT.
         * Contrarily to the IMPORT case, those associations can contain many values.
         */
        final Collection<?> actualParks = (Collection<?>) feature.getPropertyValue("parks");
        assertNotNull("parks", actualParks);
        assertEquals("parks.length", c.parks.length, actualParks.size());
        final Collection<String> expectedParks = new HashSet<>(Arrays.asList(c.parks));
        for (final Object park : actualParks) {
            final Feature pf = (Feature) park;
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
    private static Object getIndirectPropertyValue(final Feature feature, final String p1, final String p2) {
        final Object dependency = feature.getPropertyValue(p1);
        assertNotNull(p1, dependency);
        assertInstanceOf(p1, Feature.class, dependency);
        return ((Feature) dependency).getPropertyValue(p2);
    }

    /**
     * Requests a new set of features sorted by country code and park names,
     * and verifies that values are sorted as expected.
     *
     * @param  dataset  the store on which to query the features.
     * @throws DataStoreException if an error occurred during query execution.
     */
    private void verifySimpleQuerySorting(final SQLStore dataset) throws DataStoreException {
        /*
         * Property that we are going to request and expected result.
         * Note that "english_name" below is a property of the "Park" feature,
         * not to be confused with the property of same name in "City" feature.
         */
        final String   desiredProperty = "english_name";
        final String[] expectedValues  = {
            "Shinjuku Gyoen", "Yoyogi-kōen", "Luxembourg Garden", "Tuileries Garden", "Mount Royal"
        };
        /*
         * Build the query and get a new set of features.
         */
        final FeatureSet   parks = (FeatureSet) dataset.findResource("Parks");
        final FeatureQuery query = new FeatureQuery();
        query.setProjection(new FeatureQuery.NamedExpression(FF.property(desiredProperty)));
        query.setSortBy(FF.sort(FF.property("country"),       SortOrder.DESCENDING),
                        FF.sort(FF.property(desiredProperty), SortOrder.ASCENDING));
        final FeatureSet subset = parks.subset(query);
        /*
         * Verify that all features have the expected property, then verify the sorted values.
         */
        final Object[] values;
        try (Stream<Feature> features = subset.features(false)) {
            values = features.map(f -> {
                final PropertyType p = TestUtilities.getSingleton(f.getType().getProperties(true));
                assertEquals("Feature has wrong property.", desiredProperty, p.getName().toString());
                return f.getPropertyValue(desiredProperty);
            }).toArray();
        }
        assertArrayEquals("Values are not sorted as expected.", expectedValues, values);
    }

    /**
     * Requests a new set of features filtered by an arbitrary condition,
     * and verifies that we get only the expected values.
     *
     * @param  dataset  the store on which to query the features.
     * @throws DataStoreException if an error occurred during query execution.
     */
    private void verifySimpleWhere(SQLStore dataset) throws Exception {
        /*
         * Property that we are going to request and expected result.
         */
        final String   desiredProperty = "native_name";
        final String[] expectedValues  = {
            "Montréal", "Québec"
        };
        /*
         * Build the query and get a new set of features. The values returned by the database can be in any order,
         * so we use `assertSetEquals(…)` for making the test insensitive to feature order. An alternative would be
         * to add a `query.setSortBy(…)` call, but we avoid that for making this test only about the `WHERE` clause.
         */
        final FeatureSet   cities = (FeatureSet) dataset.findResource("Cities");
        final FeatureQuery query  = new FeatureQuery();
        query.setSelection(FF.equal(FF.property("country"), FF.literal("CAN")));
        final Object[] names;
        try (Stream<Feature> features = cities.subset(query).features(false)) {
            names = features.map(f -> f.getPropertyValue(desiredProperty)).toArray();
        }
        assertSetEquals(Arrays.asList(expectedValues), Arrays.asList(names));
    }
}
