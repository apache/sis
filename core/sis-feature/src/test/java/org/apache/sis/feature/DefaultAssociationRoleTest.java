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
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.apache.sis.feature.DefaultAssociationRole.NAME_KEY;
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
        final Map<String,?> properties = singletonMap(NAME_KEY, "twin town");
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
            final FeatureType parent, final FeatureAssociationRole... property)
    {
        return new DefaultFeatureType(singletonMap(NAME_KEY, name),
                false, new FeatureType[] {parent}, property);
    }

    /**
     * Tests serialization of an {@link DefaultAssociationRole} instance.
     * This will also indirectly tests {@link DefaultAssociationRole#equals(Object)}.
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
     * Tests a bidirectional association (a feature having an association to itself).
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
        assertTrue("equals", twinTown.equals(copy));
        assertEquals("hashCode", copy.hashCode(), twinTown.hashCode());
    }

    /**
     * Tests {@link DefaultFeatureType#isAssignableFrom(FeatureType)} and {@link DefaultFeatureType#equals(Object)}
     * on a feature type having a bidirectional association to an other feature. This test will fall in an infinite
     * loop if the implementation does not have proper guard against infinite recursivity.
     */
    @Test
    @DependsOnMethod("testBidirectionalAssociation")
    public void testCyclicAssociation() {
        final GenericName nameOfA = DefaultFactories.SIS_NAMES.createTypeName(null, "A");
        final GenericName nameOfB = DefaultFactories.SIS_NAMES.createTypeName(null, "B");
        final GenericName nameOfC = DefaultFactories.SIS_NAMES.createTypeName(null, "C");
        final GenericName nameOfD = DefaultFactories.SIS_NAMES.createTypeName(null, "D");

        final DefaultAssociationRole toB = new DefaultAssociationRole(singletonMap(NAME_KEY, "toB"), nameOfB, 1, 1);
        final DefaultAssociationRole toC = new DefaultAssociationRole(singletonMap(NAME_KEY, "toC"), nameOfC, 1, 1);
        final DefaultAssociationRole toD = new DefaultAssociationRole(singletonMap(NAME_KEY, "toD"), nameOfD, 1, 1);
        final DefaultFeatureType   typeA = createType(nameOfA, null, toB);
        final DefaultFeatureType   typeB = createType(nameOfB, null, toC);
        final DefaultFeatureType   typeC = createType(nameOfC, null, toD);

        final DefaultAssociationRole toA = new DefaultAssociationRole(singletonMap(NAME_KEY, "toA"), typeA, 1, 1);
        final DefaultFeatureType typeD = createType(nameOfD, null, toA, toB, toC, toD);

        assertSame("A.properties", toB, getSingleton(typeA.getProperties(false)));
        assertSame("B.properties", toC, getSingleton(typeB.getProperties(false)));
        assertSame("C.properties", toD, getSingleton(typeC.getProperties(false)));
        assertSame("D.properties", toA, typeD.getProperty("toA"));
        assertSame("D.properties", toB, typeD.getProperty("toB"));
        assertSame("D.properties", toC, typeD.getProperty("toC"));
        assertSame("D.properties", toD, typeD.getProperty("toD"));
        assertSame("toA", typeA, toA.getValueType());
//      assertSame("toB", typeB, toB.getValueType());
//      assertSame("toC", typeC, toC.getValueType());
        assertSame("toD", typeD, toD.getValueType());

        assertFalse("equals", typeA.equals(typeD));
        assertFalse("equals", typeD.equals(typeA));
        assertFalse("equals", typeB.equals(typeC));
        assertFalse("equals", typeC.equals(typeB));
        assertFalse("hashCode", typeA.hashCode() == typeB.hashCode());
        assertFalse("hashCode", typeC.hashCode() == typeD.hashCode());
    }
}
