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
package org.apache.sis.internal.feature;

import java.util.Iterator;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.apache.sis.feature.AbstractIdentifiedType;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultFeatureType;


/**
 * Tests {@link FeatureTypeBuilder}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class FeatureTypeBuilderTest extends TestCase {
    /**
     * Tests a {@code FeatureTypeBuilder.Property} with the minimum number of parameters.
     */
    @Test
    public void testEmptyProperty() {
        final FeatureTypeBuilder.Property<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        try {
            builder.build();
            fail("Builder should have failed if there is not at least a name set.");
        } catch (IllegalArgumentException ex) {
            final String message = ex.getMessage();
            assertTrue(message, message.contains("name"));
        }
        builder.setName("myScope", "myName");
        final DefaultAttributeType<?> att = (DefaultAttributeType<?>) builder.build();

        assertEquals("name",       "myScope:myName", att.getName().toString());
        assertEquals("valueClass", String.class,     att.getValueClass());
        assertNull  ("defaultValue",                 att.getDefaultValue());
        assertNull  ("definition",                   att.getDefinition());
        assertNull  ("description",                  att.getDescription());
        assertNull  ("designation",                  att.getDesignation());
        assertEquals("minimumOccurs", 1,             att.getMinimumOccurs());
        assertEquals("maximumOccurs", 1,             att.getMaximumOccurs());
    }

    /**
     * Tests a {@code FeatureTypeBuilder} with the minimum number of parameters (no property and no super type).
     */
    @Test
    @DependsOnMethod("testEmptyProperty")
    public void testEmptyFeature() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        try {
            builder.build();
            fail("Builder should have failed if there is not at least a name set.");
        } catch (IllegalArgumentException ex) {
            final String message = ex.getMessage();
            assertTrue(message, message.contains("name"));
        }
        testEmptyFeature(builder);
    }

    /**
     * Implementation of {@link #testEmptyFeature()} using the given pre-existing builder.
     */
    private static void testEmptyFeature(final FeatureTypeBuilder builder) {
        builder.setName("scope", "test");
        final DefaultFeatureType type = builder.build();

        assertEquals("name", "scope:test",   type.getName().toString());
        assertFalse ("isAbstract",           type.isAbstract());
        assertEquals("properties count",  0, type.getProperties(true).size());
        assertEquals("super-types count", 0, type.getSuperTypes().size());
    }

    /**
     * Test creation of a single property.
     */
    @Test
    @DependsOnMethod("testEmptyProperty")
    public void testPropertyBuild() {
        final FeatureTypeBuilder.Property<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        builder.setName        ("myScope", "myName");
        builder.setDefinition  ("test definition");
        builder.setDesignation ("test designation");
        builder.setDescription ("test description");
        builder.setDefaultValue("test default value.");
        builder.setCardinality(10, 60);
        builder.setMaximalLengthCharacteristic(80);
        final DefaultAttributeType<?> att = (DefaultAttributeType<?>) builder.build();

        assertEquals("name",          "myScope:myName",      att.getName().toString());
        assertEquals("definition",    "test definition",     att.getDefinition().toString());
        assertEquals("description",   "test description",    att.getDescription().toString());
        assertEquals("designation",   "test designation",    att.getDesignation().toString());
        assertEquals("valueClass",    String.class,          att.getValueClass());
        assertEquals("defaultValue",  "test default value.", att.getDefaultValue());
        assertEquals("minimumOccurs", 10,                    att.getMinimumOccurs());
        assertEquals("maximumOccurs", 60,                    att.getMaximumOccurs());
        assertTrue  ("characterizedByMaximalLength", AttributeConvention.characterizedByMaximalLength(att));
        assertEquals("maximalLengthCharacteristic", Integer.valueOf(80),
                AttributeConvention.getMaximalLengthCharacteristic(att.newInstance()));
    }

    /**
     * Tests {@link FeatureTypeBuilder#addAttribute(Class)}.
     */
    @Test
    @DependsOnMethod("testEmptyFeature")
    public void testAddProperties() {
        testAddProperties(new FeatureTypeBuilder());
    }

    /**
     * Implementation of {@link #testAddProperties()} using the given pre-existing builder.
     */
    private static void testAddProperties(final FeatureTypeBuilder builder) {
        builder.setName("myScope", "myName");
        builder.setDefinition ("test definition");
        builder.setDesignation("test designation");
        builder.setDescription("test description");
        builder.setAbstract(true);
        builder.addAttribute(String .class).setName("name");
        builder.addAttribute(Integer.class).setName("age");
        builder.addAttribute(Point  .class).setName("location").setCRSCharacteristic(HardCodedCRS.WGS84);
        builder.addAttribute(Double .class).setName("score").setCardinality(5, 50).setDefaultValue(10.0);

        final DefaultFeatureType type = builder.build();
        assertEquals("name",        "myScope:myName",   type.getName().toString());
        assertEquals("definition",  "test definition",  type.getDefinition().toString());
        assertEquals("description", "test description", type.getDescription().toString());
        assertEquals("designation", "test designation", type.getDesignation().toString());
        assertTrue  ("isAbstract",                      type.isAbstract());

        final Iterator<? extends AbstractIdentifiedType> it = type.getProperties(true).iterator();
        final DefaultAttributeType<?> a0 = (DefaultAttributeType<?>) it.next();
        final DefaultAttributeType<?> a1 = (DefaultAttributeType<?>) it.next();
        final DefaultAttributeType<?> a2 = (DefaultAttributeType<?>) it.next();
        final DefaultAttributeType<?> a3 = (DefaultAttributeType<?>) it.next();
        assertFalse("properties count", it.hasNext());

        assertEquals("name", "name",     a0.getName().toString());
        assertEquals("name", "age",      a1.getName().toString());
        assertEquals("name", "location", a2.getName().toString());
        assertEquals("name", "score",    a3.getName().toString());

        assertEquals("valueClass", String.class,  a0.getValueClass());
        assertEquals("valueClass", Integer.class, a1.getValueClass());
        assertEquals("valueClass", Point.class,   a2.getValueClass());
        assertEquals("valueClass", Double.class,  a3.getValueClass());

        assertEquals("minimumOccurs",   1, a0.getMinimumOccurs());
        assertEquals("minimumOccurs",   1, a1.getMinimumOccurs());
        assertEquals("minimumOccurs",   1, a2.getMinimumOccurs());
        assertEquals("minimumOccurs",   5, a3.getMinimumOccurs());

        assertEquals("maximumOccurs",   1, a0.getMaximumOccurs());
        assertEquals("maximumOccurs",   1, a1.getMaximumOccurs());
        assertEquals("maximumOccurs",   1, a2.getMaximumOccurs());
        assertEquals("maximumOccurs",  50, a3.getMaximumOccurs());

        assertEquals("defaultValue", null, a0.getDefaultValue());
        assertEquals("defaultValue", null, a1.getDefaultValue());
        assertEquals("defaultValue", null, a2.getDefaultValue());
        assertEquals("defaultValue", 10.0, a3.getDefaultValue());

        assertFalse("characterizedByCRS", AttributeConvention.characterizedByCRS(a0));
        assertFalse("characterizedByCRS", AttributeConvention.characterizedByCRS(a1));
        assertTrue ("characterizedByCRS", AttributeConvention.characterizedByCRS(a2));
        assertFalse("characterizedByCRS", AttributeConvention.characterizedByCRS(a3));
    }

    /**
     * Test {@link FeatureTypeBuilder#clear()}.
     */
    @Test
    @DependsOnMethod({"testEmptyFeature", "testAddProperties"})
    public void testClear() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        testAddProperties(builder);
        builder.clear();
        testEmptyFeature(builder);
    }

    /**
     * Tests {@link FeatureTypeBuilder#addIdentifier(Class)}.
     */
    @Test
    @DependsOnMethod("testAddProperties")
    public void testAddIdentifier() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("scope", "test");
        builder.setIdentifierDelimiters("-", "pref.", null);
        builder.addIdentifier(String.class).setName("name");
        builder.addDefaultGeometry(Geometry.class).setName("shape").setCRSCharacteristic(HardCodedCRS.WGS84);

        final DefaultFeatureType type = builder.build();
        assertEquals("name", "scope:test", type.getName().toString());
        assertFalse ("isAbstract", type.isAbstract());

        final Iterator<? extends AbstractIdentifiedType> it = type.getProperties(true).iterator();
        final AbstractIdentifiedType a0 = it.next();
        final AbstractIdentifiedType a1 = it.next();
        final AbstractIdentifiedType a2 = it.next();
        final AbstractIdentifiedType a3 = it.next();
        final AbstractIdentifiedType a4 = it.next();
        assertFalse("properties count", it.hasNext());

        assertEquals("name", AttributeConvention.ID_PROPERTY,                a0.getName());
        assertEquals("name", AttributeConvention.ENVELOPE_PROPERTY,          a1.getName());
        assertEquals("name", AttributeConvention.DEFAULT_GEOMETRY_PROPERTY,  a2.getName());
        assertEquals("name", "name",                                         a3.getName().toString());
        assertEquals("name", "shape",                                        a4.getName().toString());
    }
}
