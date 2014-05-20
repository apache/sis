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

import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static java.util.Collections.singletonMap;


/**
 * Base class of {@link DenseFeatureTest} and {@link SparseFeatureTest}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
abstract strictfp class FeatureTestCase extends TestCase {
    /**
     * The feature being tested.
     */
    private AbstractFeature feature;

    /**
     * {@code true} if {@link #getAttributeValue(String)} should invoke {@link AbstractFeature#getProperty(String)},
     * or {@code false} for invoking directly {@link AbstractFeature#getPropertyValue(String)}.
     */
    private boolean getValuesFromProperty;

    /**
     * For sub-class constructors only.
     */
    FeatureTestCase() {
    }

    /**
     * Creates a feature for twin towns.
     */
    static AbstractFeature twinTown(final boolean isSparse) {
        final DefaultAssociationRole twinTown = DefaultAssociationRoleTest.twinTown();
        final DefaultFeatureType     city     = twinTown.getValueType();
        final DefaultFeatureType     type     = new DefaultFeatureType(
                singletonMap(DefaultFeatureType.NAME_KEY, "Twin town"), false,
                new DefaultFeatureType[] {city}, twinTown);

        final AbstractFeature leMans = isSparse ? new SparseFeature(type) : new DenseFeature(type);
        leMans.setPropertyValue("city", "Le Mans");
        leMans.setPropertyValue("population", 143240); // In 2011.

        final AbstractFeature paderborn = isSparse ? new SparseFeature(type) : new DenseFeature(type);
        paderborn.setPropertyValue("city", "Paderborn");
        paderborn.setPropertyValue("population", 143174); // December 31th, 2011
        paderborn.setPropertyValue("twin town", leMans);
        return paderborn;
    }

    /**
     * Creates a new feature for the given type.
     */
    abstract AbstractFeature createFeature(final DefaultFeatureType type);

    /**
     * Returns the attribute value of the current {@link #feature} for the given name.
     */
    private Object getAttributeValue(final String name) {
        final Object value = feature.getPropertyValue(name);
        if (getValuesFromProperty) {
            final Property property = (Property) feature.getProperty(name);
            assertInstanceOf(name, DefaultAttribute.class, property);

            // The AttributeType shall be the same than the one provided by FeatureType for the given name.
            assertSame(name, feature.getType().getProperty(name), ((DefaultAttribute<?>) property).getType());

            // Attribute value shall be the same than the one provided by FeatureType convenience method.
            assertSame(name, feature.getPropertyValue(name), ((DefaultAttribute<?>) property).getValue());

            // Invoking getProperty(name) twice shall return the same Property instance.
            assertSame(name, property, feature.getProperty(name));
        }
        return value;
    }

    /**
     * Tests the {@link AbstractFeature#getPropertyValue(String)} method on a simple feature without super-types.
     */
    @Test
    public void testSimpleValues() {
        feature = createFeature(DefaultFeatureTypeTest.city());

        assertEquals("Utopia", getAttributeValue("city"));
        feature.setPropertyValue("city", "Atlantide");
        assertEquals("Atlantide", feature.getPropertyValue("city"));

        assertNull(getAttributeValue("population"));
        feature.setPropertyValue("population", 1000);
        assertEquals(1000, getAttributeValue("population"));
    }

    /**
     * Tests the {@link AbstractFeature#getProperty(String)} method on a simple feature without super-types.
     */
    @Test
    @DependsOnMethod("testSimpleValues")
    public void testSimpleProperties() {
        getValuesFromProperty = true;
        testSimpleValues();
    }
}
