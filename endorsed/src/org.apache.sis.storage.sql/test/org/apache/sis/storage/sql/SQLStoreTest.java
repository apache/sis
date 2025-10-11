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
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.InputStream;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.storage.sql.feature.SchemaModifier;
import org.apache.sis.storage.sql.feature.TableReference;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.metadata.sql.TestDatabase;
import static org.apache.sis.test.Assertions.assertSetEquals;
import static org.apache.sis.test.Assertions.assertSingleton;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultAssociationRole;


/**
 * Tests {@link SQLStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class SQLStoreTest extends TestOnAllDatabases {
    /**
     * Data used in the {@code Features.sql} test file.
     */
    public enum City {
        /** Tokyo (Japan).     */ TOKYO   ("Tōkyō",    "JPN", "日本",    13622267, "Yoyogi-kōen", "Shinjuku Gyoen"),
        /** Paris (France).    */ PARIS   ("Paris",    "FRA", "France",   2206488, "Tuileries Garden", "Luxembourg Garden"),
        /** Montréal (Canada). */ MONTREAL("Montreal", "CAN", "Canada",   1704694, "Mount Royal"),
        /** Québec (Canada).   */ QUEBEC  ("Quebec",   "CAN", "Canada",    531902);

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
    private AbstractFeature canada;

    /**
     * Factory to use for creating filter objects.
     */
    private final DefaultFilterFactory<AbstractFeature,Object,Object> FF;

    /**
     * Creates a new test.
     */
    public SQLStoreTest() {
        FF = DefaultFilterFactory.forFeatures();
    }

    /**
     * Provides a stream for a resource in the same package as this class.
     * The implementation invokes {@code getResourceAsStream(filename)}.
     * This invocation must be done in this module because the invoked
     * method is caller-sensitive.
     */
    private static Supplier<InputStream> resource(final String filename) {
        return new Supplier<>() {
            @Override public String toString() {return filename;}
            @Override public InputStream get() {return SQLStoreTest.class.getResourceAsStream(filename);}
        };
    }

    /**
     * Returns the storage connector to use for connecting to the test database.
     * A new instance shall be created for each test, because each instance can
     * be used only once.
     */
    private static StorageConnector connector(final TestDatabase database) {
        return new StorageConnector(database.source);
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
        final var scripts = new ArrayList<Object>(2);
        if (noschema) {
            scripts.add("CREATE SCHEMA " + SCHEMA + ';');
            // Omit the "CREATE SCHEMA" statement if the schema already exists.
        }
        scripts.add(resource("Features.sql"));
        database.executeSQL(scripts);
        final ResourceDefinition table = ResourceDefinition.table(null, noschema ? null : SCHEMA, "Cities");
        testTableQuery(connector(database), table);
        /*
         * Verify using SQL statements instead of tables.
         */
        verifyFetchCityTableAsQuery(connector(database));
        verifyNestedSQLQuery(connector(database));
        verifyLimitOffsetAndColumnSelectionFromQuery(connector(database));
        verifyDistinctQuery(connector(database));
        /*
         * Test on the table again, but with cyclic associations enabled.
         */
        final StorageConnector connector = connector(database);
        connector.setOption(SchemaModifier.OPTION_KEY, new SchemaModifier() {
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
        try (var store = new SimpleFeatureStore(new SQLStoreProvider(), connector, table)) {
            verifyFeatureTypes(store);
            final var countryCount = new HashMap<String,Integer>();
            try (Stream<AbstractFeature> features = store.findResource("Cities").features(false)) {
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
            verifyLinkInProjection(store);
            verifyStreamOperations(store.findResource("Cities"));
        }
        canada = null;
    }

    /**
     * Verifies the feature types of the "Cities" resource and its dependencies.
     * Feature properties should be in same order as columns in the database table, except for
     * the generated identifier. Note that the country is an association to another feature.
     *
     * @param  isCyclicAssociationAllowed  whether dependencies are allowed to have an association
     *         to their dependent feature, which create a cyclic dependency.
     */
    private void verifyFeatureTypes(final SimpleFeatureStore store) throws DataStoreException {
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
    private static void verifyFeatureType(final DefaultFeatureType type, final String[] expectedNames, final Object[] expectedTypes) {
        int i = 0;
        for (AbstractIdentifiedType pt : type.getProperties(false)) {
            if (i >= expectedNames.length) {
                fail("Returned feature-type contains more properties than expected. Example: " + pt.getName());
            }
            assertEquals(expectedNames[i], pt.getName().toString(), "name");
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
                assertEquals(expectedType, value, label);
            }
            i++;
        }
        assertEquals(expectedNames.length, i);
    }

    /**
     * Verifies the content of the {@code Cities} table.
     * The features are in no particular order.
     *
     * @param  feature       a feature returned by the stream.
     * @param  countryCount  number of time that the each country has been seen while iterating over the cities.
     */
    private void verifyContent(final AbstractFeature feature, final Map<String,Integer> countryCount) {
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
        assertEquals(c.country,              feature.getPropertyValue("pk:country"));
        assertEquals(c.country + ':' + city, feature.getPropertyValue("sis:identifier"));
        assertEquals(c.englishName,          feature.getPropertyValue("english_name"));
        assertEquals(c.population,           feature.getPropertyValue("population"));
        /*
         * Associations using Relation.Direction.IMPORT.
         * Those associations should be cached; we verify with "Canada" case.
         */
        assertEquals(c.countryName, getIndirectPropertyValue(feature, "country", "native_name"));
        if (isCanada) {
            final var f = (AbstractFeature) feature.getPropertyValue("country");
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
        assertNotNull(actualParks);
        assertEquals(c.parks.length, actualParks.size());
        final Collection<String> expectedParks = new HashSet<>(Arrays.asList(c.parks));
        for (final Object park : actualParks) {
            final AbstractFeature pf = (AbstractFeature) park;
            final String npn = (String) pf.getPropertyValue("native_name");
            final String epn = (String) pf.getPropertyValue("english_name");
            assertNotNull(npn, "park.native_name");
            assertNotNull(epn, "park.english_name");
            assertNotEquals(npn, epn, "park.names");
            assertTrue(expectedParks.remove(epn), "park.english_name");
            /*
             * Verify the reverse association form Parks to Cities.
             * This create a cyclic graph, but SQLStore is capable to handle it.
             */
            if (isCyclicAssociationAllowed) {
                assertSame(feature, pf.getPropertyValue("FK_City"), "City → Park → City");
            }
        }
    }

    /**
     * Follows an association in the given feature.
     */
    private static Object getIndirectPropertyValue(final AbstractFeature feature, final String p1, final String p2) {
        final Object dependency = feature.getPropertyValue(p1);
        assertNotNull(dependency, p1);
        return assertInstanceOf(AbstractFeature.class, dependency, p1).getPropertyValue(p2);
    }

    /**
     * Requests a new set of features sorted by country code and park names,
     * and verifies that values are sorted as expected.
     *
     * @param  dataset  the store on which to query the features.
     * @throws DataStoreException if an error occurred during query execution.
     */
    private void verifySimpleQuerySorting(final SimpleFeatureStore dataset) throws DataStoreException {
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
        final FeatureSet subset = parks.subset(query);
        /*
         * Verify that all features have the expected property.
         */
        final Object[] values;
        try (Stream<AbstractFeature> features = subset.features(false)) {
            values = features.map(f -> {
                final AbstractIdentifiedType p = assertSingleton(f.getType().getProperties(true));
                assertEquals(desiredProperty, p.getName().toString(), "Feature has wrong property.");
                return f.getPropertyValue(desiredProperty);
            }).toArray();
        }
        assertEquals(new HashSet<>(Arrays.asList(expectedValues)), new HashSet<>(Arrays.asList(values)));
    }

    /**
     * Requests features with a limit on the number of items.
     *
     * @param  dataset  the store on which to query the features.
     * @throws DataStoreException if an error occurred during query execution.
     */
    private void verifySimpleQueryWithLimit(final SimpleFeatureStore dataset) throws DataStoreException {
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
    private void verifySimpleWhere(final SimpleFeatureStore dataset) throws Exception {
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
        try (Stream<AbstractFeature> features = cities.subset(query).features(false)) {
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
    private void verifyWhereOnLink(final SimpleFeatureStore dataset) throws Exception {
        final String   desiredProperty = "native_name";
        final String[] expectedValues  = {"Canada"};
        final FeatureSet countries     = dataset.findResource("Countries");
        final var query = new FeatureQuery();
        query.setSelection(FF.equal(FF.property("sis:identifier"), FF.literal("CAN")));
        final String executionMode;
        final Object[] names;
        try (Stream<AbstractFeature> features = countries.subset(query).features(false)) {
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
     * Requests a new set of features with a subset containing the "sis:identifier" property.
     * The difficulty is that no column named "sis:column" exists. The property is a link to
     * the "code" attribute, which is intentionally omitted in the projection (in SQL sense)
     * in order to test the capability to follow dependencies.
     *
     * @param  dataset  the store on which to query the features.
     * @throws DataStoreException if an error occurred during query execution.
     */
    private void verifyLinkInProjection(final SimpleFeatureStore dataset) throws Exception {
        final var query = new FeatureQuery();
        query.setProjection("sis:identifier", "native_name");
        final FeatureSet countries = dataset.findResource("Countries").subset(query);
        List<Object> names;
        try (Stream<AbstractFeature> features = countries.features(false)) {
            names = features.map(f -> f.getPropertyValue("sis:identifier")).toList();
        }
        assertSetEquals(List.of("CAN", "FRA", "JPN"), names);
        try (Stream<AbstractFeature> features = countries.features(false)) {
            names = features.map(f -> f.getPropertyValue("native_name")).toList();
        }
        assertSetEquals(List.of("Canada", "France", "日本"), names);
    }

    /**
     * Checks that operations stacked on feature stream are well executed.
     * This test focuses on mapping and peeking actions overloaded by SQL streams.
     * Operations used here are meaningless; we just want to ensure that the pipeline does not skip any operation.
     *
     * @param  cities  a feature set containing all cities defined for the test class.
     */
    private void verifyStreamOperations(final FeatureSet cities) throws DataStoreException {
        try (Stream<AbstractFeature> features = cities.features(false)) {
            final var peekCount = new AtomicInteger();
            final var mapCount  = new AtomicInteger();
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
            assertEquals(expectedPopulations, actualPopulations, "Overall population count via Stream pipeline");
            assertEquals(24,  mapCount.get(), "Number of mapping (by element in the stream)");
            assertEquals(20, peekCount.get(), "Number of peeking (by element in the stream)");
        }
    }

    /**
     * Tests fetching the content of the Cities table, but using a user supplied SQL query.
     */
    private void verifyFetchCityTableAsQuery(final StorageConnector connector) throws Exception {
        try (SimpleFeatureStore store = new SimpleFeatureStore(null, connector, ResourceDefinition.query("LargeCities",
                "SELECT * FROM " + SCHEMA + ".\"Cities\" WHERE \"population\" >= 1000000")))
        {
            final FeatureSet cities = store.findResource("LargeCities");
            final var countryCount = new HashMap<String,Integer>();
            try (Stream<AbstractFeature> features = cities.features(false)) {
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
        try (SimpleFeatureStore store = new SimpleFeatureStore(null, connector, ResourceDefinition.query("MyParks",
                "SELECT * FROM " + SCHEMA + ".\"Parks\" ORDER BY \"native_name\" DESC")))
        {
            final FeatureSet parks = store.findResource("MyParks");
            /*
             * Add a filter for parks in France.
             */
            final var query = new FeatureQuery();
            query.setSelection(FF.equal(FF.property("country"), FF.literal("FRA")));
            query.setProjection(FF.property("native_name"));
            final FeatureSet frenchParks = parks.subset(query);
            /*
             * Verify the feature type.
             */
            final AbstractIdentifiedType property = assertSingleton(frenchParks.getType().getProperties(true));
            assertEquals("native_name", property.getName().toString());
            assertEquals(String.class, ((DefaultAttributeType<?>) property).getValueClass());
            /*
             * Verify the values.
             */
            final Object[] result;
            try (Stream<AbstractFeature> fs = frenchParks.features(false)) {
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
        try (SimpleFeatureStore store = new SimpleFeatureStore(null, connector, ResourceDefinition.query("MyQuery",
                "SELECT \"english_name\" AS \"title\" " +
                "FROM " + SCHEMA + ".\"Parks\"\n" +             // Test that multiline text is accepted.
                "ORDER BY \"english_name\" ASC " +
                "OFFSET 2 ROWS FETCH NEXT 3 ROWS ONLY")))
        {
            final FeatureSet parks = store.findResource("MyQuery");
            final DefaultFeatureType type = parks.getType();
            final DefaultAttributeType<?> property = assertInstanceOf(DefaultAttributeType.class, assertSingleton(type.getProperties(true)));
            assertEquals("title", property.getName().toString(), "Property name should be label defined in query");
            assertEquals(String.class, property.getValueClass(), "Attribute should be a string");
            assertEquals(0, property.getMinimumOccurs(), "Column should be nullable.");
            final Integer precision = AttributeConvention.getMaximalLengthCharacteristic(type, property);
            assertEquals(Integer.valueOf(20), precision, "Column length constraint should be visible from attribute type.");
            /*
             * Get third row in the table, as query starts on second one, and we want to skip one entry from there.
             * Tries to increase limit. The test will ensure it's not possible.
             */
            assertArrayEquals(new String[] {"Tuileries Garden", "Yoyogi-kōen"}, getTitles(parks, 1, 4),
                              "Should get fourth and fifth park names from ascending order");
            /*
             * Get first row only.
             */
            assertArrayEquals(new String[] {"Shinjuku Gyoen"}, getTitles(parks, 0, 1),
                              "Only second third name should be returned");
        }
    }

    /**
     * Applies an offset and limit on the given feature set,
     * then returns the values of the "title" property of all features.
     */
    private static String[] getTitles(final FeatureSet parks, final long skip, final long limit) throws DataStoreException {
        try (Stream<AbstractFeature> in = parks.features(false).skip(skip).limit(limit)) {
            return in.map(f -> f.getPropertyValue("title").toString()).toArray(String[]::new);
        }
    }

    /**
     * Tests a query with a call to {@link Stream#distinct()} on the stream.
     */
    private void verifyDistinctQuery(final StorageConnector connector) throws Exception {
        final Object[] expected;
        try (var store = new SimpleFeatureStore(null, connector, ResourceDefinition.query("Countries",
                "SELECT \"country\" FROM " + SCHEMA + ".\"Parks\" ORDER BY \"country\"")))
        {
            final FeatureSet countries = store.findResource("Countries");
            try (Stream<AbstractFeature> features = countries.features(false).distinct()) {
                expected = features.map(f -> f.getPropertyValue("country")).toArray();
            }
        }
        assertArrayEquals(new String[] {"CAN", "FRA", "JPN"}, expected,
                          "Distinct country names, sorted in ascending order");
    }
}
