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
import java.util.Collection;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.quality.Completeness;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.metadata.iso.quality.AbstractCompleteness;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the {@link MetadataStandard} class.
 * Unless otherwise specified, the tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@DependsOn(PropertyAccessorTest.class)
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
        final MetadataStandard std = new MetadataStandard(HardCodedCitations.ISO, "org.opengis.dummy.");
        try {
            std.getInterface(DefaultCitation.class);
            fail("No dummy interface expected.");
        } catch (ClassCastException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("DefaultCitation"));
        }
    }

    /**
     * Tests the shallow copy. For this test, we need to use a class that doesn't have any {@code getIdentifiers()}
     * method inherited from GeoAPI interfaces. The class will inherit the {@code getIdentifiers()} method defined
     * by SIS in the parent class, which doesn't have corresponding {@code setIdentifiers(...)} method.
     */
    @Test
    public void testShallowCopy() {
        final AbstractCompleteness source = new AbstractCompleteness();
        final AbstractCompleteness target = new AbstractCompleteness();
        source.setMeasureDescription(new SimpleInternationalString("Some description"));
        target.getStandard().shallowCopy(source, target);
        assertEquals("Copy of measureDescription:", "Some description", target.getMeasureDescription().toString());
        assertEquals("Copy of measureDescription:", source, target);

        source.setMeasureDescription(null);
        target.getStandard().shallowCopy(source, target);
        assertEquals("Measure description should not have been removed, since we skipped null values.",
                "Some description", target.getMeasureDescription().toString());
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
     * Tests the {@link MetadataStandard#asMap(Object, KeyNamePolicy, ValueExistencePolicy)} implementation.
     * Note: this test duplicates {@link PropertyMapTest}, but is done here again because other tests in this
     * class depend on it.
     */
    @Test
    public void testMap() {
        final DefaultCitation instance = new DefaultCitation(HardCodedCitations.EPSG);
        final Map<String,Object> map = MetadataStandard.ISO_19115.asMap(instance,
                KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
        assertFalse("The properties map shall not be empty.", map.isEmpty());
        assertEquals("Unexpected number of properties.", 4, map.size());
        /*
         * Verify the set of keys in the PropertyMap.
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
    @DependsOnMethod("testMap")
    public void testHashCode() {
        final MetadataStandard std = MetadataStandard.ISO_19115;
        final DefaultCitation instance = HardCodedCitations.EPSG;
        final Map<String,Object> map = std.asMap(instance,
                KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
        assertFalse(map.isEmpty()); // Actually 'testMap()' job, but verified for safety.
        assertEquals("hashCode()", new HashSet<>(map.values()).hashCode() + Citation.class.hashCode(),
                std.hashCode(instance));
    }
}
