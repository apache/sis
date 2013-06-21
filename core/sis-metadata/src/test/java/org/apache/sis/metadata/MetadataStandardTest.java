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
import java.util.List;
import java.util.HashSet;
import java.util.Collection;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.quality.Completeness;
import org.opengis.coverage.grid.RectifiedGrid;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.metadata.iso.acquisition.DefaultPlatform;
import org.apache.sis.metadata.iso.acquisition.DefaultInstrument;
import org.apache.sis.metadata.iso.quality.AbstractCompleteness;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

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
 * @since   0.3 (derived from geotk-2.4)
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
     * Tests {@link MetadataStandard#getInterface(Class)}.
     */
    @Test
    public void testGetInterface() {
        final MetadataStandard std = MetadataStandard.ISO_19115;
        assertEquals(Citation.class,     std.getInterface(DefaultCitation.class));
        assertEquals(Completeness.class, std.getInterface(AbstractCompleteness.class));
    }

    /**
     * Tests {@link MetadataStandard#getInterface(Class)} for an invalid type.
     * A {@link ClassCastException} is expected.
     */
    @Test
    public void testGetWrongInterface() {
        final MetadataStandard std = new MetadataStandard("SIS", "org.apache.sis.dummy.");
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
    public void testEquals() {
        final MetadataStandard std = MetadataStandard.ISO_19115;

        // Self equality test
        DefaultCitation instance = HardCodedCitations.EPSG;
        assertFalse(std.equals(instance, HardCodedCitations.GEOTIFF, ComparisonMode.STRICT));
        assertTrue (std.equals(instance, HardCodedCitations.EPSG,    ComparisonMode.STRICT));

        // Test comparison with a copy
        instance = new DefaultCitation(HardCodedCitations.EPSG);
        assertFalse(std.equals(instance, HardCodedCitations.GEOTIFF, ComparisonMode.STRICT));
        assertTrue (std.equals(instance, HardCodedCitations.EPSG,    ComparisonMode.STRICT));

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
        platform.getInstruments().add(instrument);
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
        assertEquals("[title, alternateTitles, identifiers, presentationForms]", keys.toString());
        assertTrue  ("Shall exist and be defined.",   keys.contains("title"));
        assertTrue  ("Shall exist and be defined.",   keys.contains("getTitle"));
        assertTrue  ("Shall exist and be defined.",   keys.contains("identifier"));
        assertTrue  ("Shall exist and be defined.",   keys.contains("identifiers"));
        assertTrue  ("Shall exist and be defined.",   keys.contains("getIdentifiers"));
        assertFalse ("Shall exist but be undefined.", keys.contains("ISBN"));
        assertFalse ("Shall not exists.",             keys.contains("dummy"));
        /*
         * Verifies values.
         */
        assertEquals("title", "European Petroleum Survey Group", map.get("title").toString());
        assertEquals("title", "European Petroleum Survey Group", map.get("getTitle").toString());
        final Object identifiers = map.get("identifiers");
        assertInstanceOf("identifiers", Collection.class, identifiers);
        HardCodedCitations.assertIdentifiersFor("EPSG", (Collection<?>) identifiers);
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
     * Tests the {@link MetadataStandard#ISO_19123} constant. Getters shall
     * be accessible even if there is no implementation on the classpath.
     */
    @Test
    public void testWithoutImplementation() {
        final MetadataStandard std = MetadataStandard.ISO_19123;
        assertFalse("isMetadata(Citation)",        std.isMetadata(Citation.class));
        assertFalse("isMetadata(DefaultCitation)", std.isMetadata(DefaultCitation.class));
        assertTrue ("isMetadata(RectifiedGrid)",   std.isMetadata(RectifiedGrid.class));
        /*
         * Ensure that the getters have been found.
         */
        final Map<String,String> names = std.asNameMap(RectifiedGrid.class, KeyNamePolicy.UML_IDENTIFIER, KeyNamePolicy.JAVABEANS_PROPERTY);
        assertFalse("Getters should have been found even if there is no implementation.", names.isEmpty());
        assertEquals("dimension", names.get("dimension"));
        assertEquals("cells", names.get("cell"));
        /*
         * Ensure that the type are recognized, especially RectifiedGrid.getOffsetVectors()
         * which is of type List<double[]>.
         */
        Map<String,Class<?>> types;
        types = std.asTypeMap(RectifiedGrid.class, KeyNamePolicy.UML_IDENTIFIER, TypeValuePolicy.PROPERTY_TYPE);
        assertEquals("The return type is the int primitive type.", Integer.TYPE, types.get("dimension"));
        assertEquals("The offset vectors are stored in a List.",   List.class,   types.get("offsetVectors"));

        types = std.asTypeMap(RectifiedGrid.class, KeyNamePolicy.UML_IDENTIFIER, TypeValuePolicy.ELEMENT_TYPE);
        assertEquals("As elements in a list of dimensions.",       Integer.class,  types.get("dimension"));
        assertEquals("As elements in the list of offset vectors.", double[].class, types.get("offsetVectors"));
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
