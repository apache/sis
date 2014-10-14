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

import java.util.Map;
import org.opengis.feature.FeatureAssociationRole;
import org.opengis.util.GenericName;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.opengis.feature.FeatureType;


/**
 * Tests {@link DefaultAssociationRole}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(DefaultFeatureTypeTest.class)
public final strictfp class DefaultAssociationRoleTest extends TestCase {
    /**
     * Creates an association to a twin town. We arbitrarily fix the maximum number
     * of occurrences to 1, even if in reality some cities have many twin towns.
     *
     * @param cyclic {@code true} if in addition to the association from <var>A</var> to <var>B</var>,
     *        we also want an association from <var>B</var> to <var>A</var>, thus creating a cycle.
     */
    static DefaultAssociationRole twinTown(final boolean cyclic) {
        final Map<String,?> properties = singletonMap(DefaultAssociationRole.NAME_KEY, "twin town");
        if (cyclic) {
            final GenericName valueType = DefaultFactories.SIS_NAMES.createTypeName(null, "Twin town");
            return new DefaultAssociationRole(properties, valueType, 0, 1);
        } else {
            final DefaultFeatureType valueType = DefaultFeatureTypeTest.city();
            return new DefaultAssociationRole(properties, valueType, 0, 1);
        }
    }

    /**
     * Returns a City feature type which may have a twin town.
     *
     * @param cyclic {@code true} if in addition to the association from <var>A</var> to <var>B</var>,
     *        we also want an association from <var>B</var> to <var>A</var>, thus creating a cycle.
     */
    static DefaultFeatureType twinTownCity(final boolean cyclic) {
        final DefaultAssociationRole twinTown = twinTown(cyclic);
        final FeatureType parent = cyclic ? DefaultFeatureTypeTest.city() : twinTown.getValueType();
        return createType("Twin town", parent, twinTown);
    }

    /**
     * Convenience method creating a feature type of the given name, parent and property.
     *
     * @param name     The name as either a {@link String} or a {@link GenericName}.
     * @param parent   A feature type created by {@link DefaultFeatureTypeTest#city()}, or {@code null}.
     * @param property The association to an other feature.
     */
    private static DefaultFeatureType createType(final Object name,
            final FeatureType parent, final FeatureAssociationRole property)
    {
        return new DefaultFeatureType(singletonMap(DefaultFeatureType.NAME_KEY, name),
                false, new FeatureType[] {parent}, property);
    }

    /**
     * Tests serialization of an {@link DefaultAssociationRole} instance.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(twinTown(false));
    }

    /**
     * Tests {@link DefaultAssociationRole#getTitleProperty(FeatureAssociationRole)}.
     */
    @Test
    public void testGetTitleProperty() {
        final DefaultAssociationRole twinTown = twinTown(false);
        assertEquals("city", DefaultAssociationRole.getTitleProperty(twinTown));
    }

    /**
     * Tests {@link DefaultAssociationRole#toString()}.
     */
    @Test
    public void testToString() {
        final DefaultAssociationRole twinTown = twinTown(false);
        assertEquals("FeatureAssociationRole[“twin town” : City]", twinTown.toString());
    }

    /**
     * Tests a bidirectional association (an feature having an association to itself).
     */
    @Test
    public void testBidirectionalAssociation() {
        final DefaultFeatureType twinTown = twinTownCity(true);
        final FeatureAssociationRole association = (FeatureAssociationRole) twinTown.getProperty("twin town");
        assertSame("twinTown.property(“twin town”).valueType", twinTown, association.getValueType());
        /*
         * Creates a FeatureType copy containing the same properties. Used for verifying
         * that 'DefaultFeatureType.equals(Object)' does not fall in an infinite loop.
         */
        final DefaultFeatureType copy = createType(twinTown.getName(),
                getSingleton(twinTown.getSuperTypes()), association);

        assertTrue("equals", copy.equals(twinTown));
        assertEquals("hashCode", copy.hashCode(), twinTown.hashCode());
    }

    /**
     * Tests {@link DefaultFeatureType#isAssignableFrom(FeatureType)} and {@link DefaultFeatureType#equals(Object)}
     * on a feature type having a bidirectional association to an other feature. This test will fall in an infinite
     * loop if the implementation does not have proper guard against infinite recursivity.
     */
    @Test
    public void testCyclicAssociation() {
        // TODO
    }
}
