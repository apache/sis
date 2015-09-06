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
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.quality.AbstractCompleteness;
import org.apache.sis.internal.simple.SimpleIdentifiedObject;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


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
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn({
    PropertyAccessorTest.class,
    InformationMapTest.class,
    NameMapTest.class,
    TypeMapTest.class,
    ValueMapTest.class})
public final strictfp class MetadataStandardTest extends TestCase {
    /**
     * Tests {@link MetadataStandard#isMetadata(Class)}.
     */
    @Test
    public void testIsMetadata() {
        MetadataStandard std = MetadataStandard.ISO_19115;
        assertFalse("isMetadata(String)",                 std.isMetadata(String.class));
        assertTrue ("isMetadata(Citation)",               std.isMetadata(Citation.class));
        assertTrue ("isMetadata(DefaultCitation)",        std.isMetadata(DefaultCitation.class));
        assertFalse("isMetadata(IdentifiedObject)",       std.isMetadata(IdentifiedObject.class));
        assertFalse("isMetadata(SimpleIdentifiedObject)", std.isMetadata(SimpleIdentifiedObject.class));
        assertFalse("isMetadata(GeographicCRS)",          std.isMetadata(GeographicCRS.class));

        std = MetadataStandard.ISO_19111;
        assertFalse("isMetadata(String)",                 std.isMetadata(String.class));
        assertTrue ("isMetadata(Citation)",               std.isMetadata(Citation.class));          // Dependency
        assertTrue ("isMetadata(DefaultCitation)",        std.isMetadata(DefaultCitation.class));   // Dependency
        assertTrue ("isMetadata(IdentifiedObject)",       std.isMetadata(IdentifiedObject.class));
        assertTrue ("isMetadata(SimpleIdentifiedObject)", std.isMetadata(SimpleIdentifiedObject.class));
        assertTrue ("isMetadata(GeographicCRS)",          std.isMetadata(GeographicCRS.class));

        std = MetadataStandard.ISO_19123;
        assertFalse("isMetadata(String)",                 std.isMetadata(String.class));
        assertTrue ("isMetadata(Citation)",               std.isMetadata(Citation.class));               // Transitive dependency
        assertTrue ("isMetadata(DefaultCitation)",        std.isMetadata(DefaultCitation.class));        // Transivive dependency
        assertTrue ("isMetadata(IdentifiedObject)",       std.isMetadata(IdentifiedObject.class));       // Dependency
        assertTrue ("isMetadata(SimpleIdentifiedObject)", std.isMetadata(SimpleIdentifiedObject.class)); // Dependency
        assertTrue ("isMetadata(GeographicCRS)",          std.isMetadata(GeographicCRS.class));          // Dependency
    }

    /**
     * Tests {@link MetadataStandard#getInterface(Class)}.
     */
    @Test
    @DependsOnMethod("testIsMetadata")
    public void testGetInterface() {
        MetadataStandard std = MetadataStandard.ISO_19115;
        assertEquals("getInterface(Citation)",             Citation.class,     std.getInterface(Citation.class));
        assertEquals("getInterface(DefaultCitation)",      Citation.class,     std.getInterface(DefaultCitation.class));
        assertEquals("getInterface(AbstractCompleteness)", Completeness.class, std.getInterface(AbstractCompleteness.class));

        std = MetadataStandard.ISO_19111;
        assertEquals("getInterface(Citation)",               Citation.class,         std.getInterface(Citation.class));
        assertEquals("getInterface(DefaultCitation)",        Citation.class,         std.getInterface(DefaultCitation.class));
        assertEquals("getInterface(AbstractCompleteness)",   Completeness.class,     std.getInterface(AbstractCompleteness.class));
        assertEquals("getInterface(IdentifiedObject)",       IdentifiedObject.class, std.getInterface(IdentifiedObject.class));
        assertEquals("getInterface(SimpleIdentifiedObject)", IdentifiedObject.class, std.getInterface(SimpleIdentifiedObject.class));
        assertEquals("getInterface(GeographicCRS)",          GeographicCRS.class,    std.getInterface(GeographicCRS.class));

        // Verify that the cache has not been updated in inconsistent way.
        testIsMetadata();
    }

    /**
     * Tests {@link MetadataStandard#getAccessor(Class, boolean)}.
     */
    @Test
    @DependsOnMethod("testGetInterface")
    public void testGetAccessor() {
        MetadataStandard std = MetadataStandard.ISO_19115;
        assertEquals("getAccessor(DefaultCitation)",      Citation.class,     std.getAccessor(DefaultCitation.class, true).type);
        assertEquals("getAccessor(AbstractCompleteness)", Completeness.class, std.getAccessor(AbstractCompleteness.class, true).type);
        assertNull  ("getAccessor(SimpleIdentifiedObject)",                   std.getAccessor(SimpleIdentifiedObject.class, false));

        std = MetadataStandard.ISO_19111;
        assertEquals("getAccessor(DefaultCitation)",        Citation.class,         std.getAccessor(DefaultCitation.class, true).type);
        assertEquals("getAccessor(AbstractCompleteness)",   Completeness.class,     std.getAccessor(AbstractCompleteness.class, true).type);
        assertEquals("getAccessor(SimpleIdentifiedObject)", IdentifiedObject.class, std.getAccessor(SimpleIdentifiedObject.class, true).type);

        // Verify that the cache has not been updated in inconsistent way.
        testGetInterface();
    }

    /**
     * Tests {@link MetadataStandard#getInterface(Class)} for an invalid type.
     * A {@link ClassCastException} is expected.
     */
    @Test
    @DependsOnMethod("testGetInterface")
    public void testGetWrongInterface() {
        final MetadataStandard std = new MetadataStandard("SIS", "org.apache.sis.dummy.", null);
        try {
            std.getInterface(DefaultCitation.class);
            fail("No dummy interface expected.");
        } catch (ClassCastException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("DefaultCitation"));
        }
    }

    /**
     * Tests the {@link MetadataStandard#equals(Object, Object, ComparisonMode)} method.
     */
    @Test
    @DependsOnMethod("testGetAccessor")
    public void testEquals() {
        final MetadataStandard std = MetadataStandard.ISO_19115;

        // Self equality test
        DefaultCitation instance = HardCodedCitations.EPSG;
        assertFalse(std.equals(instance, HardCodedCitations.SIS,  ComparisonMode.STRICT));
        assertTrue (std.equals(instance, HardCodedCitations.EPSG, ComparisonMode.STRICT));

        // Test comparison with a copy
        instance = new DefaultCitation(HardCodedCitations.EPSG);
        assertFalse(std.equals(instance, HardCodedCitations.SIS,  ComparisonMode.STRICT));
        assertTrue (std.equals(instance, HardCodedCitations.EPSG, ComparisonMode.STRICT));

        // test comparison with a modified copy
        instance.setTitle(new SimpleInternationalString("A dummy title"));
        assertFalse(std.equals(instance, HardCodedCitations.EPSG,    ComparisonMode.STRICT));
    }

    /**
     * Creates a metadata object having a cyclic association. The cycle is between
     * {@code platform.instrument} and {@code instrument.isMountedOn}.
     */
    static DefaultPlatform createCyclicMetadata() {
        final DefaultInstrument instrument = new DefaultInstrument();
        instrument.setType(new SimpleInternationalString("An instrument type."));
        final DefaultPlatform platform = new DefaultPlatform();
        platform.setDescription(new SimpleInternationalString("A platform."));
        instrument.setMountedOn(platform);
        platform.setInstruments(singleton(instrument));
        return platform;
    }

    /**
     * Tests the {@link MetadataStandard#equals(Object, Object, ComparisonMode)} method on an object
     * having cyclic associations. In absence of safety guard against infinite recursivity, this test
     * would produce {@link StackOverflowError}.
     */
    @Test
    @DependsOnMethod("testEquals")
    public void testEqualsOnCyclicMetadata() {
        final DefaultPlatform p1 = createCyclicMetadata();
        final DefaultPlatform p2 = createCyclicMetadata();
        assertTrue(p1.equals(p2));
        ((DefaultInstrument) getSingleton(p2.getInstruments()))
                .setType(new SimpleInternationalString("An other instrument type."));
        assertFalse(p1.equals(p2));
    }

    /**
     * Tests the {@link MetadataStandard#asValueMap(Object, KeyNamePolicy, ValueExistencePolicy)} implementation.
     * This test duplicates {@link ValueMapTest}, but is done here again as an integration test and because many
     * {@code MetadataStandard} methods depend on it ({@code equals}, {@code hashCode}, {@code prune}, <i>etc.</i>).
     */
    @Test
    @DependsOnMethod("testGetAccessor")
    public void testValueMap() {
        final DefaultCitation instance = new DefaultCitation(HardCodedCitations.EPSG);
        final Map<String,Object> map = MetadataStandard.ISO_19115.asValueMap(instance,
                KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
        assertFalse("The properties map shall not be empty.", map.isEmpty());
        assertEquals("Unexpected number of properties.", 4, map.size());
        /*
         * Verify the set of keys in the ValueMap.
         *
         * Note: the iterator order (and consequently, the order of elements in the following
         * string representation) is determined by the @XmlType(…) annotation and verified by
         * PropertyAccessorTest.testConstructor().
         */
        final Set<String> keys = map.keySet();
        assertEquals("[title, identifiers, citedResponsibleParties, presentationForms]", keys.toString());
        assertTrue  ("Shall exist and be defined.",   keys.contains("title"));
        assertTrue  ("Shall exist and be defined.",   keys.contains("getTitle"));
        assertTrue  ("Shall exist and be defined.",   keys.contains("identifier"));
        assertTrue  ("Shall exist and be defined.",   keys.contains("identifiers"));
        assertTrue  ("Shall exist and be defined.",   keys.contains("getIdentifiers"));
        assertTrue  ("Shall exist and be defined.",   keys.contains("citedResponsibleParty"));
        assertTrue  ("Shall exist and be defined.",   keys.contains("citedResponsibleParties"));
        assertFalse ("Shall exist but be undefined.", keys.contains("ISBN"));
        assertFalse ("Shall not exists.",             keys.contains("dummy"));
        /*
         * Verifies values.
         */
        assertEquals("title", "EPSG Geodetic Parameter Dataset", map.get("title").toString());
        assertEquals("title", "EPSG Geodetic Parameter Dataset", map.get("getTitle").toString());
        assertEquals("EPSG", PropertyAccessorTest.getSingletonCode(map.get("identifiers")));
    }

    /**
     * Tests {@link MetadataStandard#hashCode(Object)} using {@link HashSet} as the reference
     * implementation for computing hash code values. The hash code is defined as the sum of
     * hash code values of all non-empty properties, plus the hash code of the interface.
     */
    @Test
    @DependsOnMethod("testValueMap")
    public void testHashCode() {
        final MetadataStandard std = MetadataStandard.ISO_19115;
        final DefaultCitation instance = HardCodedCitations.EPSG;
        final Map<String,Object> map = std.asValueMap(instance,
                KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
        assertFalse(map.isEmpty()); // Actually 'testValueMap()' job, but verified for safety.
        assertEquals("hashCode()", new HashSet<Object>(map.values()).hashCode() + Citation.class.hashCode(),
                std.hashCode(instance));
    }

    /**
     * Tests the {@link MetadataStandard#hashCode(Object)} method on an object having cyclic associations.
     * In absence of safety guard against infinite recursivity, this test would produce {@link StackOverflowError}.
     *
     * @see AbstractMetadataTest#testHashCodeOnCyclicMetadata()
     */
    @Test
    @DependsOnMethod("testHashCode")
    public void testHashCodeOnCyclicMetadata() {
        final MetadataStandard std = MetadataStandard.ISO_19115;
        final int code = std.hashCode(createCyclicMetadata());
        /*
         * Following line checks that the hash code is stable, just for doing something with the code.
         * The real test was actually to ensure that the above line didn't threw a StackOverflowError.
         */
        assertEquals(code, std.hashCode(createCyclicMetadata()));
    }

    /**
     * Tests serialization of pre-defined constants.
     */
    @Test
    public void testSerialization() {
        assertSame(MetadataStandard.ISO_19111, assertSerializedEquals(MetadataStandard.ISO_19111));
        assertSame(MetadataStandard.ISO_19115, assertSerializedEquals(MetadataStandard.ISO_19115));
    }
}
