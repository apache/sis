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
package org.apache.sis.storage;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.storage.MemoryFeatureSet;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.MatchAction;
import org.opengis.filter.SortOrder;


/**
 * Tests {@link FeatureQuery} and (indirectly) {@link FeatureSubset}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public final strictfp class FeatureQueryTest extends TestCase {
    /**
     * An arbitrary amount of features, all of the same type.
     */
    private final Feature[] features;

    /**
     * The {@link #features} array wrapped in a in-memory feature set.
     */
    private final FeatureSet featureSet;

    /**
     * The query to be executed.
     */
    private final FeatureQuery query;

    /**
     * Creates a new test.
     */
    public FeatureQueryTest() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("Test");
        ftb.addAttribute(Integer.class).setName("value1");
        ftb.addAttribute(Integer.class).setName("value2");
        final FeatureType type = ftb.build();
        features = new Feature[] {
            feature(type, 3, 1),
            feature(type, 2, 2),
            feature(type, 2, 1),
            feature(type, 1, 1),
            feature(type, 4, 1)
        };
        featureSet = new MemoryFeatureSet(null, type, Arrays.asList(features));
        query      = new FeatureQuery();
    }

    private static Feature feature(final FeatureType type, final int value1, final int value2) {
        final Feature f = type.newInstance();
        f.setPropertyValue("value1", value1);
        f.setPropertyValue("value2", value2);
        return f;
    }

    /**
     * Executes the query and verify that the result is equal to the features at the given indices.
     *
     * @param  indices  indices of expected features.
     * @throws DataStoreException if an error occurred while executing the query.
     */
    private void verifyQueryResult(final int... indices) throws DataStoreException {
        final FeatureSet fs = query.execute(featureSet);
        final List<Feature> result = fs.features(false).collect(Collectors.toList());
        assertEquals("size", indices.length, result.size());
        for (int i=0; i<indices.length; i++) {
            final Feature expected = features[indices[i]];
            final Feature actual   = result.get(i);
            if (!expected.equals(actual)) {
                fail(String.format("Unexpected feature at index %d%n"
                                 + "Expected:%n%s%n"
                                 + "Actual:%n%s%n", i, expected, actual));
            }
        }
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setLimit(long)}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testLimit() throws DataStoreException {
        query.setLimit(2);
        verifyQueryResult(0, 1);
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setOffset(long)}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testOffset() throws DataStoreException {
        query.setOffset(2);
        verifyQueryResult(2, 3, 4);
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setSortBy(SortProperty[])}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testSortBy() throws DataStoreException {
        final FilterFactory<Feature,?,?> factory = DefaultFilterFactory.forFeatures();
        query.setSortBy(factory.sort(factory.property("value1", Integer.class), SortOrder.ASCENDING),
                        factory.sort(factory.property("value2", Integer.class), SortOrder.DESCENDING));
        verifyQueryResult(3, 1, 2, 0, 4);
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setSelection(Filter)}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testFilter() throws DataStoreException {
        final FilterFactory<Feature,?,?> factory = DefaultFilterFactory.forFeatures();
        query.setSelection(factory.equal(factory.property("value1", Integer.class),
                                      factory.literal(2), true, MatchAction.ALL));
        verifyQueryResult(1, 2);
    }

    /**
     * Verifies the effect of {@link FeatureQuery#setProjection(FeatureQuery.Column[])}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testColumns() throws DataStoreException {
        final FilterFactory<Feature,?,?> factory = DefaultFilterFactory.forFeatures();
        query.setProjection(new FeatureQuery.NamedExpression(factory.property("value1", Integer.class), (String) null),
                            new FeatureQuery.NamedExpression(factory.property("value1", Integer.class), "renamed1"),
                            new FeatureQuery.NamedExpression(factory.literal("a literal"), "computed"));
        query.setLimit(1);

        final FeatureSet fs = query.execute(featureSet);
        final Feature result = TestUtilities.getSingleton(fs.features(false).collect(Collectors.toList()));

        // Check result type.
        final FeatureType resultType = result.getType();
        assertEquals("Test", resultType.getName().toString());
        assertEquals(3, resultType.getProperties(true).size());
        final PropertyType pt1 = resultType.getProperty("value1");
        final PropertyType pt2 = resultType.getProperty("renamed1");
        final PropertyType pt3 = resultType.getProperty("computed");
        assertTrue(pt1 instanceof AttributeType);
        assertTrue(pt2 instanceof AttributeType);
        assertTrue(pt3 instanceof AttributeType);
        assertEquals(Integer.class, ((AttributeType) pt1).getValueClass());
        assertEquals(Integer.class, ((AttributeType) pt2).getValueClass());
        assertEquals(String.class,  ((AttributeType) pt3).getValueClass());

        // Check feature.
        assertEquals(3, result.getPropertyValue("value1"));
        assertEquals(3, result.getPropertyValue("renamed1"));
        assertEquals("a literal", result.getPropertyValue("computed"));
    }
}
