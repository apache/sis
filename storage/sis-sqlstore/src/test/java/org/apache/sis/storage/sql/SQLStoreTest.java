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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;

import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.sort.SortOrder;
import org.opengis.util.GenericName;

import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.metadata.sql.SQLBuilder;
import org.apache.sis.internal.sql.feature.QueryFeatureSet;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.sql.TestDatabase;

import org.junit.Assert;
import org.junit.Test;

import static org.apache.sis.test.Assert.assertEquals;
import static org.apache.sis.test.Assert.assertInstanceOf;
import static org.apache.sis.test.Assert.assertNotEquals;
import static org.apache.sis.test.Assert.assertNotNull;
import static org.apache.sis.test.Assert.assertSame;
import static org.apache.sis.test.Assert.assertTrue;
import static org.apache.sis.test.Assert.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

// Branch-dependent imports


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

    private static final int[] POPULATIONS = {
            13622267,  // Tokyo,    2016.
            2206488,   // Paris,    2017.
            1704694,   // Montréal, 2016.
            531902     // Québec,   2016.
    };

    private static final DefaultFilterFactory FF = new DefaultFilterFactory();

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
                        new String[] {"sis:identifier", "country",   "native_name", "english_name", "population", "sis:country", "sis:Parks"},
                        new Object[] {null,             String.class, String.class,  String.class,   Integer.class, "Countries", "Parks"});

                verifyFeatureType(((FeatureSet) store.findResource("Countries")).getType(),
                        new String[] {"sis:identifier", "code",       "native_name",  "sis:Cities"},
                        new Object[] {null,             String.class, String.class, "Cities"});

                verifyFeatureType(((FeatureSet) store.findResource("Parks")).getType(),
                        new String[] {"sis:identifier", "country", "city",       "native_name", "english_name", "sis:FK_City"},
                        new Object[] {null,             String.class,  String.class, String.class,  String.class, "Cities"});

                try (Stream<Feature> features = cities.features(false)) {
                    features.forEach((f) -> verifyContent(f));
                }

                // Now, we'll check that overloaded stream operations are functionally stable, even stacked.
                verifyStreamOperations(cities);

                verifySimpleQueries(store);

                verifySQLQueries(tmp.source);
            }
        }
        assertEquals(Integer.valueOf(2), countryCount.remove("CAN"));
        assertEquals(Integer.valueOf(1), countryCount.remove("FRA"));
        assertEquals(Integer.valueOf(1), countryCount.remove("JPN"));
        assertTrue  (countryCount.isEmpty());
    }

    private void verifySimpleQueries(SQLStore dataset) throws Exception {
        verifySimpleQuerySorting(dataset);
        verifySimpleWhere(dataset);
    }

    private void verifySimpleQuerySorting(SQLStore dataset) throws DataStoreException {
        final FeatureSet parks = (FeatureSet) dataset.findResource("Parks");
        final SimpleQuery query = new SimpleQuery();
        query.setColumns(new SimpleQuery.Column(FF.property("english_name")));
        query.setSortBy(
                FF.sort("country", SortOrder.DESCENDING),
                FF.sort("english_name", SortOrder.ASCENDING)
        );
        final FeatureSet subset = parks.subset(query);
        String[] expectedPNames = {"english_name"};
        try (Stream<Feature> features = subset.features(false)) {
            final Object[] values = features.map(f -> {
                final String[] names = f.getType().getProperties(true).stream()
                        .map(PropertyType::getName)
                        .map(GenericName::toString)
                        .toArray(size -> new String[size]);
                assertArrayEquals(expectedPNames, names);
                return f.getPropertyValue(expectedPNames[0]);
            })
                    .toArray();
            String[] expectedValues = {"Shinjuku Gyoen", "Yoyogi-kōen", "Luxembourg Garden", "Tuileries Garden", "Mount Royal"};
            assertArrayEquals("Read values are not sorted as expected.", expectedValues, values);
        }
    }

    private void verifySimpleWhere(SQLStore dataset) throws Exception {
        final SimpleQuery q = new SimpleQuery();
        q.setSortBy(FF.sort("native_name", SortOrder.ASCENDING));
        q.setFilter(FF.equals(FF.property("country"), FF.literal("CAN")));
        final FeatureSet cities = (FeatureSet) dataset.findResource("Cities");
        final Object[] names;
        try (Stream<Feature> features = cities.subset(q).features(false)) {
            names = features.map(f -> f.getPropertyValue("native_name"))
                    .toArray();
        }

        Assert.assertArrayEquals(
                "Filtered cities should only contains Canadian ones",
                new String[] {"Montréal", "Québec"},
                names
        );
    }

    private void verifySQLQueries(DataSource source) throws Exception {
        verifyFetchCityTableAsQuery(source);
        verifyLimitOffsetAndColumnSelectionFromQuery(source);
        verifyDistinctQuery(source);
    }

    private void verifyFetchCityTableAsQuery(DataSource source) throws Exception {
        final QueryFeatureSet allCities;
        final QueryFeatureSet canadaCities;
        try (Connection conn = source.getConnection()) {
            final SQLBuilder builder = new SQLBuilder(conn.getMetaData(), false)
                    .append("SELECT * FROM ").appendIdentifier(SCHEMA, "Cities");
            allCities = new QueryFeatureSet(builder, source, conn);
            /* By re-using the same builder, we ensure a defensive copy is done at feature set creation, avoiding
             * potential concurrent or security issue due to afterward modification of the query.
             */
            builder.append(" WHERE ").appendIdentifier("country").append("='CAN'");
            canadaCities = new QueryFeatureSet(builder, source, conn);
        }

        final HashMap<String, Class> expectedAttrs = new HashMap<>();
        expectedAttrs.put("country",      String.class);
        expectedAttrs.put("native_name",  String.class);
        expectedAttrs.put("english_name", String.class);
        expectedAttrs.put("population",   Integer.class);

        checkQueryType(expectedAttrs, allCities.getType());
        checkQueryType(expectedAttrs, canadaCities.getType());

        Set<Map<String, Object>> expectedResults = new HashSet<>();
        expectedResults.add(city("CAN", "Montréal", "Montreal", 1704694));
        expectedResults.add(city("CAN", "Québec", "Quebec", 531902));

        Set<Map<String, Object>> result;
        try (Stream<Feature> features = canadaCities.features(false)) {
            result = features
                    .map(SQLStoreTest::asMap)
                    .collect(Collectors.toSet());
        }
        assertEquals("Query result is not consistent with expected one", expectedResults, result);

        expectedResults.add(city("FRA", "Paris",    "Paris",    2206488));
        expectedResults.add(city("JPN", "東京",     "Tōkyō",   13622267));

        try (Stream<Feature> features = allCities.features(false)) {
            result = features
                    .map(SQLStoreTest::asMap)
                    .collect(Collectors.toSet());
        }
        assertEquals("Query result is not consistent with expected one", expectedResults, result);
    }

    private static Map<String, Object> city(String country, String nativeName, String enName, int population) {
        final Map<String, Object> result = new HashMap<>();
        result.put("country", country);
        result.put("native_name", nativeName);
        result.put("english_name", enName);
        result.put("population", population);
        return result;
    }
    
    /**
     * Differs from {@link #verifyFeatureType(FeatureType, String[], Object[])} because
     * @param expectedAttrs
     * @param target
     */
    private static void checkQueryType(final Map<String, Class> expectedAttrs, final FeatureType target) {
        final Collection<? extends PropertyType> props = target.getProperties(true);
        assertEquals("Number of attributes", expectedAttrs.size(), props.size());
        for (PropertyType p : props) {
            assertTrue("Query type should contain only attributes", p instanceof AttributeType);
            final String pName = p.getName().toString();
            final Class expectedClass = expectedAttrs.get(pName);
            assertNotNull("Unexpected property: "+pName, expectedClass);
            assertEquals("Unepected type for property: "+pName, expectedClass, ((AttributeType)p).getValueClass());
        }
    }

    private static Map<String, Object> asMap(final Feature source) {
        return source.getType().getProperties(true).stream()
                .map(PropertyType::getName)
                .map(GenericName::toString)
                .collect(Collectors.toMap(n->n, source::getPropertyValue));
    }

    /**
     * Test limit and offset. The logic is: if user provided an offset, stream {@link Stream#skip(long) skip operator}
     * does NOT override it, but stack on it (which is logic: the feature set provide user defined result, and the
     * stream navigate through it).
     *
     * Moreover, we also check filtering of columns and label usage.
     *
     * @param source Database connection provider.
     */
    private void verifyLimitOffsetAndColumnSelectionFromQuery(final DataSource source) throws Exception {
        // Ensure multiline text is accepted
        final String query = "SELECT \"english_name\" as \"title\" \n\r" +
                "FROM "+SCHEMA+".\"Parks\" \n" +
                "ORDER BY \"english_name\" ASC \n" +
                "OFFSET 2 ROWS FETCH NEXT 3 ROWS ONLY";
        final QueryFeatureSet qfs;
        try (Connection conn = source.getConnection()) {
            qfs = new QueryFeatureSet(query, source, conn);
        }

        final FeatureType type = qfs.getType();
        final Iterator<? extends PropertyType> props = type.getProperties(true).iterator();
        assertTrue("Built feature set has at least one property", props.hasNext());
        final AttributeType attr = (AttributeType) props.next();
        assertEquals("Property name should be label defined in query", "title", attr.getName().toString());
        assertEquals("Attribute should be a string", String.class, attr.getValueClass());
        assertTrue("Column should be nullable.", attr.getMinimumOccurs() == 0);
        final Object precision = attr.characteristics().get(AttributeConvention.MAXIMAL_LENGTH_CHARACTERISTIC.toString());
        assertNotNull("Length constraint should be visible from feature type", precision);
        assertEquals("Column length constraint should be visible from attribute type.", 20, ((AttributeType)precision).getDefaultValue());
        assertFalse("Built feature type should have exactly one attribute.", props.hasNext());

        Function<Stream<Feature>, String[]> getNames = in -> {
            try (Stream<Feature> closeable = in) {
                return in
                        .map(f -> f.getPropertyValue("title").toString())
                        .toArray(size -> new String[size]);
            }
        };

        String[] parkNames = getNames.apply(
                qfs.features(false)
                        // Get third row in the table, as query starts on second one, and we want to skip one entry from there
                        .skip(1)
                        // Tries to increase limit. The test will ensure it's not possible.
                        .limit(4)
        );

        assertArrayEquals(
                "Should get fourth and fifth park names from ascending order",
                new String[]{"Tuileries Garden", "Yoyogi-kōen"},
                parkNames
        );

        parkNames = getNames.apply(qfs.features(false)
                .skip(0)
                .limit(1)
        );

        assertArrayEquals("Only second third name should be returned", new String[]{"Shinjuku Gyoen"}, parkNames);
    }

    /**
     * Check that a {@link Stream#distinct()} gives coherent results. For now, no optimisation is done to delegate it to
     * database, but this test allows for non-regression test, so when an optimisation is done, we'll immediately test
     * its validity.
     */
    private void verifyDistinctQuery(DataSource source) throws SQLException {
        // Ensure multiline text is accepted
        final String query = "SELECT \"country\" FROM "+SCHEMA+".\"Parks\" ORDER BY \"country\"";
        final QueryFeatureSet qfs;
        try (Connection conn = source.getConnection()) {
            qfs = new QueryFeatureSet(query, source, conn);
        }

        final Object[] expected;
        try (Stream<Feature> features = qfs.features(false)) {
            expected = features
                    .distinct()
                    .map(f -> f.getPropertyValue("country"))
                    .toArray();
        }

        assertArrayEquals("Distinct country names, sorted in ascending order", new String[]{"CAN", "FRA", "JPN"}, expected);
    }

    /**
    * Checks that operations stacked on feature stream are well executed. This test focus on mapping and peeking
    * actions overloaded by sql streams. We'd like to test skip and limit operations too, but ignore it for now,
    * because ordering of results matters for such a test.
    *
    * @implNote Most of stream operations used here are meaningless. We just want to ensure that the pipeline does not
    * skip any operation.
    *
    * @param cities The feature set to read from. We expect a feature set containing all cities defined for the test
    *               class.
    * @throws DataStoreException Let's propagate any error raised by input feature set.
    */
    private static void verifyStreamOperations(final FeatureSet cities) throws DataStoreException {
        try (Stream<Feature> features = cities.features(false)) {
            final AtomicInteger peekCount = new AtomicInteger();
            final AtomicInteger mapCount = new AtomicInteger();
            final long populations = features.peek(f -> peekCount.incrementAndGet())
                    .peek(f -> peekCount.incrementAndGet())
                    .map(f -> {
                        mapCount.incrementAndGet();
                        return f;
                    })
                    .peek(f -> peekCount.incrementAndGet())
                    .map(f -> {
                        mapCount.incrementAndGet();
                        return f;
                    })
                    .map(f -> f.getPropertyValue("population"))
                    .mapToDouble(obj -> ((Number) obj).doubleValue())
                    .peek(f -> peekCount.incrementAndGet())
                    .peek(f -> peekCount.incrementAndGet())
                    .boxed()
                    .mapToDouble(d -> {mapCount.incrementAndGet(); return d;})
                    .mapToObj(d -> {mapCount.incrementAndGet(); return d;})
                    .mapToDouble(d -> {mapCount.incrementAndGet(); return d;})
                    .map(d -> {mapCount.incrementAndGet(); return d;})
                    .mapToLong(d -> (long) d)
                    .sum();

            long expectedPopulations = 0;
            for (long pop : POPULATIONS) expectedPopulations += pop;
            assertEquals("Overall population count via Stream pipeline", expectedPopulations, populations);
            assertEquals("Number of mapping (by element in the stream)", 24, mapCount.get());
            assertEquals("Number of peeking (by element in the stream)", 20, peekCount.get());
        }
    }

    /**
     * Verifies the result of analyzing the structure of the {@code "Cities"} table.
     */
    private static void verifyFeatureType(final FeatureType type, final String[] expectedNames, final Object[] expectedTypes) {
        int i = 0;
        for (PropertyType pt : type.getProperties(false)) {
            if (i >= expectedNames.length) fail("Returned feature-type contains more properties than expected. Example: "+pt.getName());
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
        final String country, countryName, englishName;
        final String[] parks;
        final int population;
        boolean isCanada = false;
        switch (city) {
            case "東京": {
                englishName = "Tōkyō";
                country     = "JPN";
                countryName = "日本";
                population  = POPULATIONS[0];
                parks       = new String[] {"Yoyogi-kōen", "Shinjuku Gyoen"};
                break;
            }
            case "Paris": {
                englishName = "Paris";
                country     = "FRA";
                countryName = "France";
                population  = POPULATIONS[1];
                parks       = new String[] {"Tuileries Garden", "Luxembourg Garden"};
                break;
            }
            case "Montréal": {
                englishName = "Montreal";
                country     = "CAN";
                countryName = "Canada";
                population  = POPULATIONS[2];
                isCanada    = true;
                parks       = new String[] {"Mount Royal"};
                break;
            }
            case "Québec": {
                englishName = "Quebec";
                country     = "CAN";
                countryName = "Canada";
                population  = POPULATIONS[3];
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
        assertEquals("country",        country,              feature.getPropertyValue("country"));
        assertEquals("sis:identifier", country + ':' + city, feature.getPropertyValue("sis:identifier"));
        assertEquals("english_name",   englishName,          feature.getPropertyValue("english_name"));
        assertEquals("population",     population,           feature.getPropertyValue("population"));
        /*
         * Associations using Relation.Direction.IMPORT.
         * Those associations should be cached; we verify with "Canada" case.
         */
        assertEquals("country", countryName, getIndirectPropertyValue(feature, "sis:country", "native_name"));
        if (isCanada) {
            final Feature f = (Feature) feature.getPropertyValue("sis:country");
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
        final Collection<?> actualParks = (Collection<?>) feature.getPropertyValue("sis:Parks");
        assertNotNull("parks", actualParks);
        assertEquals("parks.length", parks.length, actualParks.size());
        final Collection<String> expectedParks = new HashSet<>(Arrays.asList(parks));
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
            assertSame("City → Park → City", feature, pf.getPropertyValue("sis:FK_City"));
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
}
