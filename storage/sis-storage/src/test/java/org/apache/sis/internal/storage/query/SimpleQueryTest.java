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
package org.apache.sis.internal.storage.query;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.internal.storage.MemoryFeatureSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.test.TestCase;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.MatchAction;
import org.opengis.filter.sort.SortOrder;

/**
 * Tests {@link SimpleQuery}.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class SimpleQueryTest extends TestCase {

    private static final FeatureType TYPE;
    private static final Feature[] FEATURES;
    private static final FeatureSet FEATURESET;
    static {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("Test");
        ftb.addAttribute(Integer.class).setName("value1");
        ftb.addAttribute(Integer.class).setName("value2");
        TYPE = ftb.build();

        final Feature f1 = TYPE.newInstance();
        f1.setPropertyValue("value1", 3);
        f1.setPropertyValue("value2", 1);
        final Feature f2 = TYPE.newInstance();
        f2.setPropertyValue("value1", 2);
        f2.setPropertyValue("value2", 2);
        final Feature f3 = TYPE.newInstance();
        f3.setPropertyValue("value1", 2);
        f3.setPropertyValue("value2", 1);
        final Feature f4 = TYPE.newInstance();
        f4.setPropertyValue("value1", 1);
        f4.setPropertyValue("value2", 1);
        final Feature f5 = TYPE.newInstance();
        f5.setPropertyValue("value1", 4);
        f5.setPropertyValue("value2", 1);

        FEATURES = new Feature[]{f1,f2,f3,f4,f5};
        FEATURESET = new MemoryFeatureSet(null, null, TYPE, Arrays.asList(FEATURES));
    }

    /**
     * Verify query limit.
     *
     * @throws DataStoreException
     */
    @Test
    public void testLimit() throws DataStoreException {

        final SimpleQuery query = new SimpleQuery();
        query.setLimit(2);

        final FeatureSet fs = SimpleQuery.executeOnCPU(FEATURESET, query);
        final Feature[] result = fs.features(false).collect(Collectors.toList()).toArray(new Feature[0]);

        assertEquals(FEATURES[0], result[0]);
        assertEquals(FEATURES[1], result[1]);
    }

    /**
     * Verify query offset.
     *
     * @throws DataStoreException
     */
    @Test
    public void testOffset() throws DataStoreException {

        final SimpleQuery query = new SimpleQuery();
        query.setOffset(2);

        final FeatureSet fs = SimpleQuery.executeOnCPU(FEATURESET, query);
        final Feature[] result = fs.features(false).collect(Collectors.toList()).toArray(new Feature[0]);

        assertEquals(FEATURES[2], result[0]);
        assertEquals(FEATURES[3], result[1]);
        assertEquals(FEATURES[4], result[2]);
    }

    /**
     * Verify query sortby.
     *
     * @throws DataStoreException
     */
    @Test
    public void testSortBy() throws DataStoreException {
        final DefaultFilterFactory factory = new DefaultFilterFactory();

        final SimpleQuery query = new SimpleQuery();
        query.setSortBy(
                factory.sort("value1", SortOrder.ASCENDING),
                factory.sort("value2", SortOrder.DESCENDING)
        );

        final FeatureSet fs = SimpleQuery.executeOnCPU(FEATURESET, query);
        final Feature[] result = fs.features(false).collect(Collectors.toList()).toArray(new Feature[0]);

        assertEquals(FEATURES[3], result[0]);
        assertEquals(FEATURES[1], result[1]);
        assertEquals(FEATURES[2], result[2]);
        assertEquals(FEATURES[0], result[3]);
        assertEquals(FEATURES[4], result[4]);
    }

    /**
     * Verify query filter.
     *
     * @throws DataStoreException
     */
    @Test
    public void testFilter() throws DataStoreException {
        final DefaultFilterFactory factory = new DefaultFilterFactory();

        final SimpleQuery query = new SimpleQuery();
        query.setFilter(factory.equal(factory.property("value1"), factory.literal(2), true, MatchAction.ALL));

        final FeatureSet fs = SimpleQuery.executeOnCPU(FEATURESET, query);
        final Feature[] result = fs.features(false).collect(Collectors.toList()).toArray(new Feature[0]);

        assertEquals(FEATURES[1], result[0]);
        assertEquals(FEATURES[2], result[1]);
    }

    /**
     * Verify query columns.
     *
     * @throws DataStoreException
     */
    @Test
    public void testColumns() throws DataStoreException {
        final DefaultFilterFactory factory = new DefaultFilterFactory();

        final SimpleQuery query = new SimpleQuery();
        query.setColumns(Arrays.asList(
                new SimpleQuery.Column(factory.property("value1"), (String)null),
                new SimpleQuery.Column(factory.property("value1"), "renamed1"),
                new SimpleQuery.Column(factory.literal("a literal"), "computed")
            ));
        query.setLimit(1);

        final FeatureSet fs = SimpleQuery.executeOnCPU(FEATURESET, query);
        final Feature[] results = fs.features(false).collect(Collectors.toList()).toArray(new Feature[0]);
        assertEquals(1, results.length);

        final Feature result = results[0];

        //check result type
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
        assertEquals(String.class, ((AttributeType) pt3).getValueClass());

        //check feature
        assertEquals(3, result.getPropertyValue("value1"));
        assertEquals(3, result.getPropertyValue("renamed1"));
        assertEquals("a literal", result.getPropertyValue("computed"));
    }
}
