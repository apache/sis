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

import static java.util.Collections.singletonMap;
import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultAssociation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn({
    DefaultAssociationRoleTest.class,
    DefaultFeatureTest.class
})
public final strictfp class DefaultAssociationTest extends TestCase {
    /**
     * Returns an association to use for testing purpose.
     *
     * <blockquote>“The earliest known town twinning in Europe was between Paderborn, Germany
     * and Le Mans, France in 836.” — source: Wikipedia</blockquote>
     */
    static DefaultAssociation twinTown() {
        final DefaultFeature twinTown = new DefaultFeature(DefaultFeatureTypeTest.cityPopulation());
        twinTown.setPropertyValue("city", "Le Mans");
        twinTown.setPropertyValue("population", 148169);
        final DefaultAssociation association = new DefaultAssociation(DefaultAssociationRoleTest.twinTown());
        association.setValue(twinTown);
        return association;
    }

    /**
     * Tests attempt to set a value of the wrong type.
     */
    @Test
    public void testWrongValue() {
        final DefaultAssociation association  = twinTown();
        final PropertyType       population   = association.getRole().getValueType().getProperty("population");
        final DefaultFeature     otherFeature = new DefaultFeature(new DefaultFeatureType(
                singletonMap(DefaultFeatureType.NAME_KEY, "Population"), false, null, population));
        try {
            association.setValue(otherFeature);
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("Twin town"));
            assertTrue(message, message.contains("Population"));
            assertTrue(message, message.contains("City population"));
        }
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(twinTown());
    }
}
