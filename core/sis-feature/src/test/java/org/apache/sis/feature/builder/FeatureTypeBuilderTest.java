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
package org.apache.sis.feature.builder;

import java.util.Iterator;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.feature.DefaultFeatureTypeTest;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;


/**
 * Tests {@link FeatureTypeBuilder}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final strictfp class FeatureTypeBuilderTest extends TestCase {
    /**
     * Tests a {@code AttributeTypeBuilder} with the minimum number of parameters.
     */
    @Test
    public void testAttributeTypeBuilderInitialization() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        assertEquals("default name", "string", builder.getName().toString());

        builder.setName("myScope", "myName");
        final AttributeType<?> att = (AttributeType<?>) builder.build();

        assertEquals("name", "myScope:myName",   att.getName().toString());
        assertEquals("valueClass", String.class, att.getValueClass());
        assertNull  ("defaultValue",             att.getDefaultValue());
        assertNull  ("definition",               att.getDefinition());
        assertNull  ("description",              att.getDescription());
        assertNull  ("designation",              att.getDesignation());
        assertEquals("minimumOccurs", 1,         att.getMinimumOccurs());
        assertEquals("maximumOccurs", 1,         att.getMaximumOccurs());
    }

    /**
     * Tests a {@code FeatureTypeBuilder} with the minimum number of parameters (no property and no super type).
     */
    @Test
    @DependsOnMethod("testAttributeTypeBuilderInitialization")
    public void testFeatureTypeBuilderInitialization() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        try {
            builder.build();
            fail("Builder should have failed if there is not at least a name set.");
        } catch (IllegalArgumentException ex) {
            final String message = ex.getMessage();
            assertTrue(message, message.contains("name"));
        }
        builder.setName("scope", "test");
        final FeatureType type = builder.build();

        assertEquals("name", "scope:test",   type.getName().toString());
        assertFalse ("isAbstract",           type.isAbstract());
        assertEquals("properties count",  0, type.getProperties(true).size());
        assertEquals("super-types count", 0, type.getSuperTypes().size());
    }

    /**
     * Test creation of a single attribute.
     */
    @Test
    @DependsOnMethod("testAttributeTypeBuilderInitialization")
    public void testAttributeTypeBuilder() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        builder.setName        ("myScope", "myName");
        builder.setDefinition  ("test definition");
        builder.setDesignation ("test designation");
        builder.setDescription ("test description");
        builder.setDefaultValue("test default value.");
        builder.setCardinality(10, 60);
        builder.setMaximalLength(80);
        final AttributeType<?> att = (AttributeType<?>) builder.build();

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
    @DependsOnMethod("testAttributeTypeBuilder")
    public void testAddAttribute() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("myScope", "myName");
        builder.setDefinition ("test definition");
        builder.setDesignation("test designation");
        builder.setDescription("test description");
        builder.setAbstract(true);
        builder.addAttribute(String .class).setName("name");
        builder.addAttribute(Integer.class).setName("age");
        builder.addAttribute(Point  .class).setName("location").setCRS(HardCodedCRS.WGS84);
        builder.addAttribute(Double .class).setName("score").setDefaultValue(10.0).setCardinality(5, 50);

        final FeatureType type = builder.build();
        assertEquals("name",        "myScope:myName",   type.getName().toString());
        assertEquals("definition",  "test definition",  type.getDefinition().toString());
        assertEquals("description", "test description", type.getDescription().toString());
        assertEquals("designation", "test designation", type.getDesignation().toString());
        assertTrue  ("isAbstract",                      type.isAbstract());

        final Iterator<? extends PropertyType> it = type.getProperties(true).iterator();
        final AttributeType<?> a0 = (AttributeType<?>) it.next();
        final AttributeType<?> a1 = (AttributeType<?>) it.next();
        final AttributeType<?> a2 = (AttributeType<?>) it.next();
        final AttributeType<?> a3 = (AttributeType<?>) it.next();
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
     * Tests {@link FeatureTypeBuilder#addAttribute(Class)} where one property is an identifier
     * and another property is the geometry.
     */
    @Test
    @DependsOnMethod("testAddAttribute")
    public void testAddIdentifierAndGeometry() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("scope", "test");
        builder.setIdentifierDelimiters("-", "pref.", null);
        builder.addAttribute(String.class).setName("name")
                .addRole(AttributeRole.IDENTIFIER_COMPONENT);
        builder.addAttribute(Geometry.class).setName("shape")
                .setCRS(HardCodedCRS.WGS84)
                .addRole(AttributeRole.DEFAULT_GEOMETRY);

        final FeatureType type = builder.build();
        assertEquals("name", "scope:test", type.getName().toString());
        assertFalse ("isAbstract", type.isAbstract());

        final Iterator<? extends PropertyType> it = type.getProperties(true).iterator();
        final PropertyType a0 = it.next();
        final PropertyType a1 = it.next();
        final PropertyType a2 = it.next();
        final PropertyType a3 = it.next();
        final PropertyType a4 = it.next();
        assertFalse("properties count", it.hasNext());

        assertEquals("name", AttributeConvention.IDENTIFIER_PROPERTY, a0.getName());
        assertEquals("name", AttributeConvention.ENVELOPE_PROPERTY,   a1.getName());
        assertEquals("name", AttributeConvention.GEOMETRY_PROPERTY,   a2.getName());
        assertEquals("name", "name",                                  a3.getName().toString());
        assertEquals("name", "shape",                                 a4.getName().toString());
    }

    /**
     * Tests {@link FeatureTypeBuilder#addAttribute(Class)} where one attribute is an identifier that already has
     * the {@code "@identifier"} name. This is called "anonymous" because identifiers with an explicit name in the
     * data file should use that name instead in the feature type.
     */
    @Test
    @DependsOnMethod("testAddIdentifierAndGeometry")
    public void testAddAnonymousIdentifier() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("City");
        builder.addAttribute(String.class).setName(AttributeConvention.IDENTIFIER_PROPERTY).addRole(AttributeRole.IDENTIFIER_COMPONENT);
        builder.addAttribute(Integer.class).setName("population");
        final FeatureType type = builder.build();
        final Iterator<? extends PropertyType> it = type.getProperties(true).iterator();
        final PropertyType a0 = it.next();
        final PropertyType a1 = it.next();
        assertFalse("properties count", it.hasNext());
        assertEquals("name", AttributeConvention.IDENTIFIER_PROPERTY, a0.getName());
        assertEquals("type", String.class,  ((AttributeType<?>) a0).getValueClass());
        assertEquals("name", "population", a1.getName().toString());
        assertEquals("type", Integer.class, ((AttributeType<?>) a1).getValueClass());
    }

    /**
     * Tests {@link FeatureTypeBuilder#addAttribute(Class)} where one attribute is a geometry that already has
     * the {@code "@geometry"} name. This is called "anonymous" because geometries with an explicit name in the
     * data file should use that name instead in the feature type.
     */
    @Test
    @DependsOnMethod("testAddIdentifierAndGeometry")
    public void testAddAnonymousGeometry() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("City");
        builder.addAttribute(Point.class).setName(AttributeConvention.GEOMETRY_PROPERTY).addRole(AttributeRole.DEFAULT_GEOMETRY);
        builder.addAttribute(Integer.class).setName("population");
        final FeatureType type = builder.build();
        final Iterator<? extends PropertyType> it = type.getProperties(true).iterator();
        final PropertyType a0 = it.next();
        final PropertyType a1 = it.next();
        final PropertyType a2 = it.next();
        assertFalse("properties count", it.hasNext());
        assertEquals("name", AttributeConvention.ENVELOPE_PROPERTY, a0.getName());
        assertEquals("name", AttributeConvention.GEOMETRY_PROPERTY, a1.getName());
        assertEquals("type", Point.class,   ((AttributeType<?>) a1).getValueClass());
        assertEquals("name", "population", a2.getName().toString());
        assertEquals("type", Integer.class, ((AttributeType<?>) a2).getValueClass());
    }

    /**
     * Tests creation of a builder from an existing feature type.
     * This method also acts as a test of {@code FeatureTypeBuilder} getter methods.
     */
    @Test
    public void testCreateFromTemplate() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder(DefaultFeatureTypeTest.capital());
        assertEquals("name",       "Capital", builder.getName().toString());
        assertEquals("superTypes", "City",    TestUtilities.getSingleton(builder.getSuperTypes()).getName().toString());
        assertFalse ("isAbstract",            builder.isAbstract());
    }
}
