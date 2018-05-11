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

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.storage.MemoryFeatureSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;


/**
 * Tests {@link SimpleQuery} and (indirectly) {@link FeatureSubset}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class SimpleQueryTest extends TestCase {
    /**
     * An arbitrary amount of features, all of the same type.
     */
    private final AbstractFeature[] features;

    /**
     * The {@link #features} array wrapped in a in-memory feature set.
     */
    private final FeatureSet featureSet;

    /**
     * The query to be executed.
     */
    private final SimpleQuery query;

    /**
     * Creates a new test.
     */
    public SimpleQueryTest() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("Test");
        ftb.addAttribute(Integer.class).setName("value1");
        ftb.addAttribute(Integer.class).setName("value2");
        final DefaultFeatureType type = ftb.build();
        features = new AbstractFeature[] {
            feature(type, 3, 1),
            feature(type, 2, 2),
            feature(type, 2, 1),
            feature(type, 1, 1),
            feature(type, 4, 1)
        };
        featureSet = new MemoryFeatureSet(null, null, type, Arrays.asList(features));
        query      = new SimpleQuery();
    }

    private static AbstractFeature feature(final DefaultFeatureType type, final int value1, final int value2) {
        final AbstractFeature f = type.newInstance();
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
        final List<AbstractFeature> result = fs.features(false).collect(Collectors.toList());
        assertEquals("size", indices.length, result.size());
        for (int i=0; i<indices.length; i++) {
            final AbstractFeature expected = features[indices[i]];
            final AbstractFeature actual   = result.get(i);
            if (!expected.equals(actual)) {
                fail(String.format("Unexpected feature at index %d%n"
                                 + "Expected:%n%s%n"
                                 + "Actual:%n%s%n", i, expected, actual));
            }
        }
    }

    /**
     * Verifies the effect of {@link SimpleQuery#setLimit(long)}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testLimit() throws DataStoreException {
        query.setLimit(2);
        verifyQueryResult(0, 1);
    }

    /**
     * Verifies the effect of {@link SimpleQuery#setOffset(long)}.
     *
     * @throws DataStoreException if an error occurred while executing the query.
     */
    @Test
    public void testOffset() throws DataStoreException {
        query.setOffset(2);
        verifyQueryResult(2, 3, 4);
    }
}
