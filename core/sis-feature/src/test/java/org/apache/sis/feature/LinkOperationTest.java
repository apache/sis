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
package org.apache.sis.feature;

import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.util.Collections.singletonMap;


/**
 * Tests {@link LinkOperation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn({
    AbstractOperationTest.class,
    DenseFeatureTest.class
})
public final strictfp class LinkOperationTest extends TestCase {
    /**
     * Creates a simple feature type with a link operation.
     * The feature contains the following properties:
     *
     * <ul>
     *   <li>{@code city}       as a  {@link String}  (mandatory).</li>
     *   <li>{@code population} as an {@link Integer} (mandatory).</li>
     *   <li>{@code name} as a link to the {@code city} attribute.</li>
     * </ul>
     *
     * @return The feature for a city.
     */
    private static DefaultFeatureType city() {
        final DefaultFeatureType city = DefaultFeatureTypeTest.city();
        final LinkOperation link = new LinkOperation(singletonMap(DefaultFeatureType.NAME_KEY, "name"),
                city.getProperty("city"));
        return new DefaultFeatureType(singletonMap(DefaultFeatureType.NAME_KEY, "Metropolis"),
                false, new DefaultFeatureType[] {city}, link);
    }

    /**
     * Implementation of the test methods.
     */
    private static void run(final AbstractFeature feature) {
        assertEquals("Get directly",     "Utopia", feature.getPropertyValue("city"));
        assertEquals("Get through link", "Utopia", feature.getPropertyValue("name"));
        feature.setPropertyValue("name", "Atlantide");                                          // Set through link.
        assertEquals("Get directly",     "Atlantide", feature.getPropertyValue("city"));
        assertEquals("Get through link", "Atlantide", feature.getPropertyValue("name"));
        assertSame(feature.getProperty("name"), feature.getProperty("name"));
    }

    /**
     * Tests a dense feature type with operations.
     */
    @Test
    public void testDenseFeature() {
        run(new DenseFeature(city()));
    }

    /**
     * Tests a sparse feature type with operations.
     */
    @Test
    public void testSparseFeature() {
        run(new SparseFeature(city()));
    }
}
