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
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.apache.sis.util.iso.DefaultNameFactory;
import static org.apache.sis.feature.DefaultAssociationRole.NAME_KEY;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests {@link DefaultAssociationRole}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultAssociationRoleTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultAssociationRoleTest() {
    }

    /**
     * Creates an association to a twin town. We arbitrarily fix the maximum number
     * of occurrences to 1, even if in reality some cities have many twin towns.
     *
     * @param  cyclic  {@code true} if in addition to the association from <var>A</var> to <var>B</var>,
     *                 we also want an association from <var>B</var> to <var>A</var>, thus creating a cycle.
     * @return the association to use for testing purpose.
     */
    static DefaultAssociationRole twinTown(final boolean cyclic) {
        final Map<String,?> properties = Map.of(NAME_KEY, "twin town");
        if (cyclic) {
            final NameFactory factory = DefaultNameFactory.provider();
            final GenericName valueType = factory.createTypeName(null, "Twin town");
            return new DefaultAssociationRole(properties, valueType, 0, 1);
        } else {
            final DefaultFeatureType valueType = DefaultFeatureTypeTest.city();
            return new DefaultAssociationRole(properties, valueType, 0, 1);
        }
    }

    /**
     * Returns a City feature type which may have a twin town.
     *
     * @param  cyclic  {@code true} if in addition to the association from <var>A</var> to <var>B</var>,
     *                 we also want an association from <var>B</var> to <var>A</var>, thus creating a cycle.
     * @return the association to use for testing purpose.
     */
    static DefaultFeatureType twinTownCity(final boolean cyclic) {
        final DefaultAssociationRole twinTown = twinTown(cyclic);
        final var parentType = cyclic ? DefaultFeatureTypeTest.city() : twinTown.getValueType();
        return createType("Twin town", parentType, twinTown);
    }

    /**
     * Convenience method creating a feature type of the given name, parent and property.
     *
     * @param  name      the name as either a {@link String} or a {@link GenericName}.
     * @param  parent    a feature type created by {@link DefaultFeatureTypeTest#city()}, or {@code null}.
     * @param  property  the association to another feature.
     * @return the feature type to use for testing purpose.
     */
    private static DefaultFeatureType createType(final Object name,
            final DefaultFeatureType parent, final DefaultAssociationRole... property)
    {
        return new DefaultFeatureType(Map.of(NAME_KEY, name),
                false, new DefaultFeatureType[] {parent}, property);
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
     * Tests {@code DefaultAssociationRole.getTitleProperty(FeatureAssociationRole)}.
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
        final var association = (DefaultAssociationRole) twinTown.getProperty("twin town");
        assertSame(twinTown, association.getValueType());
        /*
         * Creates a FeatureType copy containing the same properties. Used for verifying
         * that 'DefaultFeatureType.equals(Object)' does not fall in an infinite loop.
         */
        final DefaultFeatureType copy = createType(twinTown.getName(),
                assertSingleton(twinTown.getSuperTypes()), association);

        assertTrue(copy.equals(twinTown));
        assertTrue(twinTown.equals(copy));
        assertEquals(copy.hashCode(), twinTown.hashCode());
    }

    /**
     * Tests {@code DefaultFeatureType.isAssignableFrom(FeatureType)} and {@code DefaultFeatureType.equals(Object)}
     * on a feature type having a bidirectional association to another feature. This test will fall in an infinite
     * loop if the implementation does not have proper guard against infinite recursion.
     */
    @Test
    public void testCyclicAssociation() {
        final NameFactory factory = DefaultNameFactory.provider();
        final GenericName nameOfA = factory.createTypeName(null, "A");
        final GenericName nameOfB = factory.createTypeName(null, "B");
        final GenericName nameOfC = factory.createTypeName(null, "C");
        final GenericName nameOfD = factory.createTypeName(null, "D");
        /*
         * Associations defined only by the FeatureType name.
         */
        final var toB = new DefaultAssociationRole(Map.of(NAME_KEY, "toB"), nameOfB, 1, 1);
        final var toC = new DefaultAssociationRole(Map.of(NAME_KEY, "toC"), nameOfC, 1, 1);
        final var toD = new DefaultAssociationRole(Map.of(NAME_KEY, "toD"), nameOfD, 1, 1);
        final DefaultFeatureType typeA = createType(nameOfA, null, toB);
        final DefaultFeatureType typeB = createType(nameOfB, null, toC);
        final DefaultFeatureType typeC = createType(nameOfC, null, toD);
        /*
         * Association defined with real FeatureType instance, except for an association to itself.
         * Construction of this FeatureType shall cause the resolution of all above FeatureTypes.
         */
        final var toAr = new DefaultAssociationRole(Map.of(NAME_KEY, "toA"),         typeA, 1, 1);
        final var toBr = new DefaultAssociationRole(Map.of(NAME_KEY, toB.getName()), typeB, 1, 1);
        final var toCr = new DefaultAssociationRole(Map.of(NAME_KEY, toC.getName()), typeC, 1, 1);
        final DefaultFeatureType typeD = createType(nameOfD, null, toAr, toBr, toCr, toD);
        /*
         * Verify the property given to the constructors. There is no reason for those properties
         * to change as they are not the instances to be replaced by the name resolutions, but we
         * verify them as a paranoiac check.
         */
        assertSame(toB, assertSingleton(typeA.getProperties(false)));
        assertSame(toC, assertSingleton(typeB.getProperties(false)));
        assertSame(toD, assertSingleton(typeC.getProperties(false)));
        assertSame(toAr, typeD.getProperty("toA"));
        assertSame(toBr, typeD.getProperty("toB"));
        assertSame(toCr, typeD.getProperty("toC"));
        assertSame(toD,  typeD.getProperty("toD"));
        /*
         * CORE OF THIS TEST: verify that the values of toB, toC and toD have been replaced by the actual
         * FeatureType instances. Also verify that as a result, toB.equals(toBr) and toC.equals(toCr).
         */
        assertSame(typeA, toAr.getValueType());
        assertSame(typeB, toBr.getValueType());
        assertSame(typeB, toB .getValueType());
        assertSame(typeC, toCr.getValueType());
        assertSame(typeC, toC .getValueType());
        assertSame(typeD, toD .getValueType());
        assertEquals(toB, toBr);
        assertEquals(toC, toCr);
        /*
         * Other equality tests, mostly for verifying that we do not fall in an infinite loop here.
         */
        assertNotEquals(typeA, typeD);
        assertNotEquals(typeD, typeA);
        assertNotEquals(typeB, typeC);
        assertNotEquals(typeC, typeB);
        assertNotEquals(typeA.hashCode(), typeB.hashCode());
        assertNotEquals(typeC.hashCode(), typeD.hashCode());
    }
}
