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
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.sql.feature.SchemaModifier;
import org.apache.sis.internal.sql.feature.TableReference;
import org.apache.sis.test.sql.TestDatabase;
import org.apache.sis.test.TestUtilities;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.test.Assertions.assertSetEquals;

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
 * @version 1.4
 * @since   1.1
 */
public final class SQLStoreTest extends TestOnAllDatabases {
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
     * Whether dependencies are allowed to have an association to their dependent feature.
     *
     * @see SchemaModifier#isCyclicAssociationAllowed(TableReference)
     */
    private boolean isCyclicAssociationAllowed;

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
     * Runs all tests on a single database software. A temporary schema is created at the beginning of this method
     * and deleted after all tests finished. The schema is created and populated by the {@code Features.sql} script.
     *
     * @param  noschema  whether the test database is in memory. If {@code true}, then the schema will be created
     *                   and will be the only schema to exist (ignoring system schema); i.e. we assume that there
     *                   is no ambiguity if we do not specify the schema in {@link SQLStore} constructor.
     */
    @Override
    protected void test(final TestDatabase database, final boolean noschema) throws Exception {
        final String[] scripts = {
            "CREATE SCHEMA " + SCHEMA + ';',
            "file:Features.sql"
        };
        if (!noschema) {
            scripts[0] = null;      // Omit the "CREATE SCHEMA" statement if the schema already exists.
        }
        database.executeSQL(SQLStoreTest.class, scripts);
        final StorageConnector connector = new StorageConnector(database.source);
        final ResourceDefinition table = ResourceDefinition.table(null, noschema ? null : SCHEMA, "Cities");
        testTableQuery(connector, table);
        /*
         * Verify using SQL statements instead of tables.
         */
        verifyFetchCityTableAsQuery(connector);
        verifyNestedSQLQuery(connector);
        verifyLimitOffsetAndColumnSelectionFromQuery(connector);
        verifyDistinctQuery(connector);
        /*
         * Test on the table again, but with cyclic associations enabled.
         */
        connector.setOption(SchemaModifier.OPTION, new SchemaModifier() {
            @Override public boolean isCyclicAssociationAllowed(TableReference dependency) {
                return true;
            }
        });
        isCyclicAssociationAllowed = true;
        testTableQuery(connector, table);
    }

    /**
     * Creates a {@link SQLStore} instance with the specified table as a resource, then tests some queries.
     */
    private void testTableQuery(final StorageConnector connector, final ResourceDefinition table) throws Exception {
        try (SQLStore store = new SQLStore(new SQLStoreProvider(), connector, table)) {
            verifyFeatureTypes(store);
            final Map<String,Integer> countryCount = new HashMap<>();
            try (Stream<Feature> features = store.findResource("Cities").features(false)) {
                features.forEach((f) -> verifyContent(f, countryCount));
            }
            assertEquals(Integer.valueOf(2), countryCount.remove("CAN"));
            assertEquals(Integer.valueOf(1), countryCount.remove("FRA"));
            assertEquals(Integer.valueOf(1), countryCount.remove("JPN"));
            assertTrue(countryCount.isEmpty());
            /*
             * Verify overloaded stream operations (sorting, etc.).
             */
            verifySimpleQuerySorting(store);
            verifySimpleQueryWithLimit(store);
            verifySimpleWhere(store);
            verifyWhereOnLink(store);
            verifyStreamOperations(store.findResource("Cities"));
        }
        canada = null;
    }

    /**
     * Verifies the feature types of the "Cities" resource and its dependencies.
     * Feature properties should be in same order than columns in the database table, except for
     * the generated identifier. Note that the country is an association to another feature.
     *
     * @param  isCyclicAssociationAllowed  whether dependencies are allowed to have an association
     *         to their dependent feature, which create a cyclic dependency.
     */
    private void verifyFeatureTypes(final SQLStore store) throws DataStoreException {
        verifyFeatureType(store.findResource("Cities").getType(),
                new String[] {"sis:identifier", "pk:country", "country",   "native_name", "english_name", "population",  "parks"},
                new Object[] {null,             String.class, "Countries", String.class,  String.class,   Integer.class, "Parks"});

        verifyFeatureType(store.findResource("Countries").getType(),
                new String[] {"sis:identifier", "code",       "native_name"},
                new Object[] {null,             String.class, String.class});
        /*
         * If cyclic dependencies are allowed, an additional properties "FK_City" is present
         * compared to the case where cyclic dependencies are avoided.
         */
        final String[] expectedNames;
        final Object[] expectedTypes;
        if (isCyclicAssociationAllowed) {
            expectedNames = new String[] {"sis:identifier", "pk:country", "FK_City", "city",       "native_name", "english_name"};
            expectedTypes = new Object[] {null,             String.class, "Cities",  String.class, String.class,  String.class};
        } else {
            expectedNames = new String[] {"sis:identifier", "country",    "city",       "native_name", "english_name"};
            expectedTypes = new Object[] {null,             String.class, String.class, String.class,  String.class};
        }
        verifyFeatureType(store.findResource("Parks").getType(), expectedNames, expectedTypes);
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
     *
     * @param  feature       a feature returned by the stream.
     * @param  countryCount  number of time that the each country has been seen while iterating over the cities.
     */
    private void verifyContent(final Feature feature, final Map<String,Integer> countryCount) {
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
            if (isCyclicAssociationAllowed) {
                assertSame("City → Park → City", feature, pf.getPropertyValue("FK_City"));
            }
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
        final FeatureSet   parks = dataset.findResource("Parks");
        final FeatureQuery query = new FeatureQuery();
        query.setProjection(FF.property(desiredProperty));
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
     * Requests features with a limit on the number of items.
     *
     * @param  dataset  the store on which to query the features.
     * @throws DataStoreException if an error occurred during query execution.
     */
    private void verifySimpleQueryWithLimit(final SQLStore dataset) throws DataStoreException {
        final FeatureSet   parks = dataset.findResource("Parks");
        final FeatureQuery query = new FeatureQuery();
        query.setLimit(2);
        final FeatureSet subset = parks.subset(query);
        assertEquals(2, subset.features(false).count());
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
        final FeatureSet   cities = dataset.findResource("Cities");
        final FeatureQuery query  = new FeatureQuery();
        query.setSelection(FF.equal(FF.property("country"), FF.literal("CAN")));
        final Object[] names;
        try (Stream<Feature> features = cities.subset(query).features(false)) {
            names = features.map(f -> f.getPropertyValue(desiredProperty)).toArray();
        }
        assertSetEquals(Arrays.asList(expectedValues), Arrays.asList(names));
    }

    /**
     * Requests a new set of features filtered by a condition on the "sis:identifier" property.
     * The optimizer should replace that link by a condition on the actual column.
     *
     * @param  dataset  the store on which to query the features.
     * @throws DataStoreException if an error occurred during query execution.
     */
    private void verifyWhereOnLink(SQLStore dataset) throws Exception {
        final String   desiredProperty = "native_name";
        final String[] expectedValues  = {"Canada"};
        final FeatureSet   countries   = dataset.findResource("Countries");
        final FeatureQuery query       = new FeatureQuery();
        query.setSelection(FF.equal(FF.property("sis:identifier"), FF.literal("CAN")));
        final String executionMode;
        final Object[] names;
        try (Stream<Feature> features = countries.subset(query).features(false)) {
            executionMode = features.toString();
            names = features.map(f -> f.getPropertyValue(desiredProperty)).toArray();
        }
        assertArrayEquals(expectedValues, names);
        /*
         * Verify that the query is executed with a SQL statement, not with Java code.
         * The use of SQL is made possible by the replacement of "sis:identifier" link
         * by a reference to "code" column. If that replacement is not properly done,
         * then the "predicates" value would be "Java" instead of "SQL".
         */
        assertEquals("FeatureStream[table=“Countries”, predicates=“SQL”]", executionMode);
    }

    /**
     * Checks that operations stacked on feature stream are well executed.
     * This test focuses on mapping and peeking actions overloaded by SQL streams.
     * Operations used here are meaningless; we just want to ensure that the pipeline does not skip any operation.
     *
     * @param  cities  a feature set containing all cities defined for the test class.
     */
    private void verifyStreamOperations(final FeatureSet cities) throws DataStoreException {
        try (Stream<Feature> features = cities.features(false)) {
            final AtomicInteger peekCount = new AtomicInteger();
            final AtomicInteger mapCount  = new AtomicInteger();
            final long actualPopulations = features.peek(f -> peekCount.incrementAndGet())
                    .peek(f -> peekCount.incrementAndGet())
                    .map (f -> {mapCount.incrementAndGet(); return f;})
                    .peek(f -> peekCount.incrementAndGet())
                    .map (f -> {mapCount.incrementAndGet(); return f;})
                    .map (f -> f.getPropertyValue("population"))
                    .mapToDouble(obj -> ((Number) obj).doubleValue())
                    .peek(f -> peekCount.incrementAndGet())
                    .peek(f -> peekCount.incrementAndGet())
                    .boxed()
                    .mapToDouble(d -> {mapCount.incrementAndGet(); return d;})
                    .mapToObj   (d -> {mapCount.incrementAndGet(); return d;})
                    .mapToDouble(d -> {mapCount.incrementAndGet(); return d;})
                    .map        (d -> {mapCount.incrementAndGet(); return d;})
                    .mapToLong  (d -> (long) d)
                    .sum();

            long expectedPopulations = 0;
            for (City city : City.values()) expectedPopulations += city.population;
            assertEquals("Overall population count via Stream pipeline", expectedPopulations, actualPopulations);
            assertEquals("Number of mapping (by element in the stream)", 24, mapCount.get());
            assertEquals("Number of peeking (by element in the stream)", 20, peekCount.get());
        }
    }

    /**
     * Tests fetching the content of the Cities table, but using a user supplied SQL query.
     */
    private void verifyFetchCityTableAsQuery(final StorageConnector connector) throws Exception {
        try (SQLStore store = new SQLStore(null, connector, ResourceDefinition.query("LargeCities",
                "SELECT * FROM " + SCHEMA + ".\"Cities\" WHERE \"population\" >= 1000000")))
        {
            final FeatureSet cities = store.findResource("LargeCities");
            final Map<String,Integer> countryCount = new HashMap<>();
            try (Stream<Feature> features = cities.features(false)) {
                features.forEach((f) -> verifyContent(f, countryCount));
            }
            assertEquals(Integer.valueOf(1), countryCount.remove("CAN"));
            assertEquals(Integer.valueOf(1), countryCount.remove("FRA"));
            assertEquals(Integer.valueOf(1), countryCount.remove("JPN"));
            assertTrue(countryCount.isEmpty());
        }
        canada = null;
    }

    /**
     * Tests a user supplied query followed by another query built from filters.
     */
    private void verifyNestedSQLQuery(final StorageConnector connector) throws Exception {
        try (SQLStore store = new SQLStore(null, connector, ResourceDefinition.query("MyParks",
                "SELECT * FROM " + SCHEMA + ".\"Parks\"")))
        {
            final FeatureSet parks = store.findResource("MyParks");
            /*
             * Add a filter for parks in France.
             */
            final FeatureQuery query = new FeatureQuery();
            query.setSortBy(FF.sort(FF.property("native_name"), SortOrder.DESCENDING));
            query.setSelection(FF.equal(FF.property("country"), FF.literal("FRA")));
            query.setProjection(FF.property("native_name"));
            final FeatureSet frenchParks = parks.subset(query);
            /*
             * Verify the feature type.
             */
            final PropertyType property = TestUtilities.getSingleton(frenchParks.getType().getProperties(true));
            assertEquals("native_name", property.getName().toString());
            assertEquals(String.class, ((AttributeType<?>) property).getValueClass());
            /*
             * Verify the values.
             */
            final Object[] result;
            try (Stream<Feature> fs = frenchParks.features(false)) {
                result = fs.map(f -> f.getPropertyValue("native_name")).toArray();
            }
            assertArrayEquals(new String[] {"Jardin du Luxembourg", "Jardin des Tuileries"}, result);
        }
    }

    /**
     * Tests a query having limit, offset, filtering of columns and label usage.
     * When user provides an offset, stream {@linkplain Stream#skip(long) skip operator} should not override it,
     * but stack on it (i.e. the feature set provide user defined result, and the stream navigate through it).
     */
    private void verifyLimitOffsetAndColumnSelectionFromQuery(final StorageConnector connector) throws Exception {
        try (SQLStore store = new SQLStore(null, connector, ResourceDefinition.query("MyQuery",
                "SELECT \"english_name\" AS \"title\" " +
                "FROM " + SCHEMA + ".\"Parks\"\n" +             // Test that multiline text is accepted.
                "ORDER BY \"english_name\" ASC " +
                "OFFSET 2 ROWS FETCH NEXT 3 ROWS ONLY")))
        {
            final FeatureSet parks = store.findResource("MyQuery");
            final FeatureType type = parks.getType();
            final AttributeType<?> property = (AttributeType<?>) TestUtilities.getSingleton(type.getProperties(true));
            assertEquals("Property name should be label defined in query", "title", property.getName().toString());
            assertEquals("Attribute should be a string", String.class, property.getValueClass());
            assertEquals("Column should be nullable.", 0, property.getMinimumOccurs());
            final Integer precision = AttributeConvention.getMaximalLengthCharacteristic(type, property);
            assertEquals("Column length constraint should be visible from attribute type.", Integer.valueOf(20), precision);
            /*
             * Get third row in the table, as query starts on second one, and we want to skip one entry from there.
             * Tries to increase limit. The test will ensure it's not possible.
             */
            assertArrayEquals(
                    "Should get fourth and fifth park names from ascending order",
                    new String[] {"Tuileries Garden", "Yoyogi-kōen"},
                    getTitles(parks, 1, 4));
            /*
             * Get first row only.
             */
            assertArrayEquals("Only second third name should be returned",
                    new String[] {"Shinjuku Gyoen"},
                    getTitles(parks, 0, 1));
        }
    }

    /**
     * Applies an offset and limit on the given feature set,
     * then returns the values of the "title" property of all features.
     */
    private static String[] getTitles(final FeatureSet parks, final long skip, final long limit) throws DataStoreException {
        try (Stream<Feature> in = parks.features(false).skip(skip).limit(limit)) {
            return in.map(f -> f.getPropertyValue("title").toString()).toArray(String[]::new);
        }
    }

    /**
     * Tests a query with a call to {@link Stream#distinct()} on the stream.
     */
    private void verifyDistinctQuery(final StorageConnector connector) throws Exception {
        final Object[] expected;
        try (SQLStore store = new SQLStore(null, connector, ResourceDefinition.query("Countries",
                "SELECT \"country\" FROM " + SCHEMA + ".\"Parks\" ORDER BY \"country\"")))
        {
            final FeatureSet countries = store.findResource("Countries");
            try (Stream<Feature> features = countries.features(false).distinct()) {
                expected = features.map(f -> f.getPropertyValue("country")).toArray();
            }
        }
        assertArrayEquals("Distinct country names, sorted in ascending order",
                new String[] {"CAN", "FRA", "JPN"}, expected);
    }
}
