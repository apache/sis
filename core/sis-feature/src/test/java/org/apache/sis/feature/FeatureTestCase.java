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

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.util.Collections.singletonMap;


/**
 * Base class of {@link DenseFeatureTest} and {@link SparseFeatureTest}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
strictfp class FeatureTestCase extends TestCase {
    /**
     * Creates a feature for twin towns.
     */
    static AbstractFeature twinTown() {
        final DefaultAssociationRole twinTown = DefaultAssociationRoleTest.twinTown();
        final DefaultFeatureType     city     = twinTown.getValueType();
        final DefaultFeatureType     type     = new DefaultFeatureType(
                singletonMap(DefaultFeatureType.NAME_KEY, "Twin town"), false,
                new DefaultFeatureType[] {city}, twinTown);

        final AbstractFeature leMans = new DenseFeature(type);
        leMans.setPropertyValue("city", "Le Mans");
        leMans.setPropertyValue("population", 143240); // In 2011.

        final AbstractFeature paderborn = new DenseFeature(type);
        paderborn.setPropertyValue("city", "Paderborn");
        paderborn.setPropertyValue("population", 143174); // December 31th, 2011
        paderborn.setPropertyValue("twin town", leMans);
        return paderborn;
    }

    /**
     * Tests the construction of a simple feature without super-types.
     */
    @Test
    public void testSimple() {
        final AbstractFeature cityPopulation = new DenseFeature(DefaultFeatureTypeTest.city());

        assertEquals("Utopia", cityPopulation.getPropertyValue("city"));
        cityPopulation.setPropertyValue("city", "Atlantide");
        assertEquals("Atlantide", cityPopulation.getPropertyValue("city"));

        assertNull(cityPopulation.getPropertyValue("population"));
        cityPopulation.setPropertyValue("population", 1000);
        assertEquals(1000, cityPopulation.getPropertyValue("population"));
    }
}
