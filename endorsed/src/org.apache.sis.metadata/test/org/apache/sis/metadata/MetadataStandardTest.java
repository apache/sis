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
package org.apache.sis.metadata;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.quality.Completeness;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation;
import org.apache.sis.metadata.iso.quality.AbstractCompleteness;
import org.apache.sis.metadata.simple.SimpleIdentifiedObject;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.ComparisonMode;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests the {@link MetadataStandard} class.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * <p>The following methods are not (or few) tested by this class, because they are tested by
 * dedicated classes named according the implementation class doing the actual work:</p>
 *
 * <ul>
 *   <li>{@link MetadataStandard#asNameMap(Class, KeyNamePolicy, KeyNamePolicy)}, tested by {@link NameMapTest}</li>
 *   <li>{@link MetadataStandard#asTypeMap(Class, KeyNamePolicy, TypeValuePolicy)}, tested by {@link TypeMapTest}</li>
 *   <li>{@link MetadataStandard#asInformationMap(Class, KeyNamePolicy)}, tested by {@link InformationMapTest}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class MetadataStandardTest extends TestCase {
    /**
     * The standard being tested.
     */
    private MetadataStandard standard;

    /**
     * Creates a new test case.
     */
    public MetadataStandardTest() {
    }

    /**
     * Returns {@code true} if the given type is a metadata.
     */
    private boolean isMetadata(final Class<?> type) {
        return standard.isMetadata(type);
    }

    /**
     * Tests {@link MetadataStandard#isMetadata(Class)}.
     */
    @Test
    public void testIsMetadata() {
        standard = MetadataStandard.ISO_19115;
        assertFalse(isMetadata(String.class));
        assertTrue (isMetadata(Citation.class));
        assertTrue (isMetadata(DefaultCitation.class));
        assertFalse(isMetadata(IdentifiedObject.class));
        assertFalse(isMetadata(SimpleIdentifiedObject.class));
        assertFalse(isMetadata(GeographicCRS.class));
//      assertFalse(isMetadata(RectifiedGrid.class));
        assertFalse(isMetadata(Double.class));
        assertFalse(isMetadata(Double.TYPE));

        standard = MetadataStandard.ISO_19111;
        assertFalse(isMetadata(String.class));
        assertTrue (isMetadata(Citation.class));               // Dependency
        assertTrue (isMetadata(DefaultCitation.class));        // Dependency
        assertTrue (isMetadata(IdentifiedObject.class));
        assertTrue (isMetadata(SimpleIdentifiedObject.class));
        assertTrue (isMetadata(GeographicCRS.class));
//      assertFalse(isMetadata(RectifiedGrid.class));

        standard = MetadataStandard.ISO_19123;
        assertFalse(isMetadata(String.class));
        assertTrue (isMetadata(Citation.class));               // Transitive dependency
        assertTrue (isMetadata(DefaultCitation.class));        // Transivive dependency
        assertTrue (isMetadata(IdentifiedObject.class));       // Dependency
        assertTrue (isMetadata(SimpleIdentifiedObject.class)); // Dependency
        assertTrue (isMetadata(GeographicCRS.class));          // Dependency
//      assertTrue (isMetadata(RectifiedGrid.class));
    }

    /**
     * Returns the interface for the given metadata implementation class.
     */
    private Class<?> getInterface(final Class<?> type) {
        return standard.getInterface(type);
    }

    /**
     * Tests {@link MetadataStandard#getInterface(Class)}.
     */
    @Test
    public void testGetInterface() {
        standard = MetadataStandard.ISO_19115;
        assertEquals(Citation.class,         getInterface(Citation.class));
        assertEquals(Citation.class,         getInterface(DefaultCitation.class));
        assertEquals(Completeness.class,     getInterface(AbstractCompleteness.class));
        assertEquals(GeographicExtent.class, getInterface(GeographicExtent.class));

        standard = MetadataStandard.ISO_19111;
        assertEquals(Citation.class,         getInterface(Citation.class));
        assertEquals(Citation.class,         getInterface(DefaultCitation.class));
        assertEquals(Completeness.class,     getInterface(AbstractCompleteness.class));
        assertEquals(IdentifiedObject.class, getInterface(IdentifiedObject.class));
        assertEquals(IdentifiedObject.class, getInterface(SimpleIdentifiedObject.class));
        assertEquals(GeographicCRS.class,    getInterface(GeographicCRS.class));

        // Verify that the cache has not been updated in inconsistent way.
        testIsMetadata();
    }

    /**
     * Returns the interface type declared by the accessor for the given class.
     */
    private Class<?> getAccessor(final Class<?> type, final boolean mandatory) {
        final PropertyAccessor accessor = standard.getAccessor(new CacheKey(type), mandatory);
        return (accessor != null) ? accessor.type : null;
    }

    /**
     * Tests {@link MetadataStandard#getAccessor(CacheKey, boolean)}.
     */
    @Test
    public void testGetAccessor() {
        standard = MetadataStandard.ISO_19115;
        assertEquals(Citation.class,         getAccessor(DefaultCitation.class, true));
        assertEquals(Completeness.class,     getAccessor(AbstractCompleteness.class, true));
        assertNull  (                        getAccessor(SimpleIdentifiedObject.class, false));

        standard = MetadataStandard.ISO_19111;
        assertEquals(Citation.class,         getAccessor(DefaultCitation.class, true));
        assertEquals(Completeness.class,     getAccessor(AbstractCompleteness.class, true));
        assertEquals(IdentifiedObject.class, getAccessor(SimpleIdentifiedObject.class, true));

        // Verify that the cache has not been updated in inconsistent way.
        testGetInterface();
    }

    /**
     * Tests {@link MetadataStandard#getInterface(Class)} for an invalid type.
     * A {@link ClassCastException} is expected.
     */
    @Test
    public void testGetWrongInterface() {
        standard = new MetadataStandard("SIS", "org.apache.sis.dummy.", (MetadataStandard[]) null);
        var e = assertThrows(ClassCastException.class, () -> getInterface(DefaultCitation.class));
        assertMessageContains(e, "DefaultCitation");
    }

    /**
     * Tests the {@link MetadataStandard#equals(Object, Object, ComparisonMode)} method.
     */
    @Test
    public void testEquals() {
        standard = MetadataStandard.ISO_19115;

        // Self equality test
        DefaultCitation instance = HardCodedCitations.EPSG;
        assertFalse(standard.equals(instance, HardCodedCitations.SIS,  ComparisonMode.STRICT));
        assertTrue (standard.equals(instance, HardCodedCitations.EPSG, ComparisonMode.STRICT));

        // Test comparison with a copy
        instance = new DefaultCitation(HardCodedCitations.EPSG);
        assertFalse(standard.equals(instance, HardCodedCitations.SIS,  ComparisonMode.STRICT));
        assertTrue (standard.equals(instance, HardCodedCitations.EPSG, ComparisonMode.STRICT));

        // test comparison with a modified copy
        instance.setTitle(new SimpleInternationalString("A dummy title"));
        assertFalse(standard.equals(instance, HardCodedCitations.EPSG, ComparisonMode.STRICT));
    }

    /**
     * Creates a metadata object having a cyclic association. The cycle is between
     * {@code platform.instrument} and {@code instrument.isMountedOn}.
     */
    static DefaultAcquisitionInformation createCyclicMetadata() {
        final var instrument = new DefaultInstrument();
        instrument.setType(new SimpleInternationalString("An instrument type."));

        final var platform = new DefaultPlatform();
        platform.setDescription(new SimpleInternationalString("A platform."));
        instrument.setMountedOn(platform);
        platform.setInstruments(Set.of(instrument));

        final var acquisition = new DefaultAcquisitionInformation();
        acquisition.setPlatforms(Set.of(platform));
        return acquisition;
    }

    /**
     * Tests the {@link MetadataStandard#equals(Object, Object, ComparisonMode)} method on an object
     * having cyclic associations. In absence of safety guard against infinite recursion, this test
     * would produce {@link StackOverflowError}.
     */
    @Test
    public void testEqualsOnCyclicMetadata() {
        final DefaultAcquisitionInformation p1 = createCyclicMetadata();
        final DefaultAcquisitionInformation p2 = createCyclicMetadata();
        assertTrue(p1.equals(p2));

        final var platform   = assertInstanceOf(DefaultPlatform.class,   assertSingleton(p2.getPlatforms()));
        final var instrument = assertInstanceOf(DefaultInstrument.class, assertSingleton(platform.getInstruments()));
        instrument.setType(new SimpleInternationalString("Another instrument type."));
        assertNotEquals(p1, p2);
    }

    /**
     * Tests the {@link MetadataStandard#asValueMap(Object, Class, KeyNamePolicy, ValueExistencePolicy)} implementation.
     * This test duplicates {@link ValueMapTest}, but is done here again as an integration test and because many
     * {@code MetadataStandard} methods depend on it ({@code equals}, {@code hashCode}, {@code prune}, <i>etc.</i>).
     */
    @Test
    public void testValueMap() {
        final var instance = new DefaultCitation(HardCodedCitations.EPSG);
        final Map<String,Object> map = MetadataStandard.ISO_19115.asValueMap(instance, null,
                KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
        assertFalse(map.isEmpty());
        assertEquals(4, map.size());
        /*
         * Verify the set of keys in the ValueMap.
         *
         * Note: the iterator order (and consequently, the order of elements in the following
         * string representation) is determined by the @XmlType(…) annotation and verified by
         * PropertyAccessorTest.testConstructor().
         */
        final Set<String> keys = map.keySet();
        assertEquals("[title, identifiers, citedResponsibleParties, presentationForms]", keys.toString());
        assertTrue  (keys.contains("title"));
        assertTrue  (keys.contains("getTitle"));
        assertTrue  (keys.contains("identifier"));
        assertTrue  (keys.contains("identifiers"));
        assertTrue  (keys.contains("getIdentifiers"));
        assertTrue  (keys.contains("citedResponsibleParty"));
        assertTrue  (keys.contains("citedResponsibleParties"));
        assertFalse (keys.contains("ISBN"));
        assertFalse (keys.contains("dummy"));
        /*
         * Verifies values.
         */
        assertEquals("EPSG Geodetic Parameter Dataset", map.get("title").toString());
        assertEquals("EPSG Geodetic Parameter Dataset", map.get("getTitle").toString());
        assertEquals("EPSG", PropertyAccessorTest.getSingletonCode(map.get("identifiers")));
    }

    /**
     * Tests {@link MetadataStandard#hashCode(Object)} using {@link HashSet} as the reference
     * implementation for computing hash code values. The hash code is defined as the sum of
     * hash code values of all non-empty properties, plus the hash code of the interface.
     */
    @Test
    public void testHashCode() {
        standard = MetadataStandard.ISO_19115;
        final DefaultCitation instance = HardCodedCitations.EPSG;
        final Map<String,Object> map = standard.asValueMap(instance, null,
                KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
        assertFalse(map.isEmpty()); // Actually 'testValueMap()' job, but verified for safety.
        assertEquals(new HashSet<>(map.values()).hashCode() + Citation.class.hashCode(), standard.hashCode(instance));
    }

    /**
     * Tests the {@link MetadataStandard#hashCode(Object)} method on an object having cyclic associations.
     * In absence of safety guard against infinite recursion, this test would produce {@link StackOverflowError}.
     *
     * @see AbstractMetadataTest#testHashCodeOnCyclicMetadata()
     */
    @Test
    public void testHashCodeOnCyclicMetadata() {
        standard = MetadataStandard.ISO_19115;
        final int code = standard.hashCode(createCyclicMetadata());
        /*
         * Following line checks that the hash code is stable, just for doing something with the code.
         * The real test was actually to ensure that the above line didn't threw a StackOverflowError.
         */
        assertEquals(code, standard.hashCode(createCyclicMetadata()));
    }

    /**
     * Tests serialization of predefined constants.
     */
    @Test
    public void testSerialization() {
        assertSame(MetadataStandard.ISO_19111, assertSerializedEquals(MetadataStandard.ISO_19111));
        assertSame(MetadataStandard.ISO_19115, assertSerializedEquals(MetadataStandard.ISO_19115));
    }
}
