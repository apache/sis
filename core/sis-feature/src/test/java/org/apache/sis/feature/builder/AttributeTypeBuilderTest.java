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

import java.util.Set;
import java.util.List;
import com.esri.core.geometry.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.feature.Features;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.test.Assertions.assertSetEquals;

// Branch-dependent imports
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyType;


/**
 * Tests {@link AttributeTypeBuilder}. The tests need to create a {@link FeatureTypeBuilder} in order to allow
 * {@code AttributeTypeBuilder} instantiation, but nothing else is done with the {@code FeatureTypeBuilder}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   0.8
 */
@DependsOn(CharacteristicTypeBuilderTest.class)
public final class AttributeTypeBuilderTest extends TestCase {
    /**
     * Tests with the minimum number of parameters.
     */
    @Test
    public void testInitialization() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        assertEquals("default name", "string", builder.getName().toString());

        builder.setName("myScope", "myName");
        final AttributeType<?> att = builder.build();

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
     * Test creation of a single attribute with more values than the minimal ones.
     */
    @Test
    @DependsOnMethod("testInitialization")
    public void testBuilder() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        assertSame(builder, builder.setName        ("myScope", "myName"));
        assertSame(builder, builder.setDefinition  ("test definition"));
        assertSame(builder, builder.setDesignation ("test designation"));
        assertSame(builder, builder.setDescription ("test description"));
        assertSame(builder, builder.setDefaultValue("test default value."));
        assertSame(builder, builder.setMinimumOccurs(10).setMaximumOccurs(60));
        assertSame(builder, builder.setMaximalLength(80));
        final AttributeType<?> att = builder.build();

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
     * Tests {@link AttributeTypeBuilder#setValueClass(Class)}.
     * This implies the replacement of the builder by a new instance.
     */
    @Test
    @DependsOnMethod("testBuilder")
    public void testSetValueClass() {
        final AttributeTypeBuilder<Float> builder = new FeatureTypeBuilder().addAttribute(Float.class);
        assertSame(builder, builder.setName        ("temperature"));
        assertSame(builder, builder.setDefinition  ("test definition"));
        assertSame(builder, builder.setDesignation ("test designation"));
        assertSame(builder, builder.setDescription ("test description"));
        assertSame(builder, builder.setDefaultValue(25f));
        assertSame(builder, builder.setValueClass(Float.class));
        assertEquals("valueClass", Float.class, builder.getValueClass());
        assertSetEquals(Set.of(builder), builder.owner().properties());
        final CharacteristicTypeBuilder<Float> stddev = builder.addCharacteristic(Float.class);
        assertSame(stddev, stddev.setName("stddev"));
        assertSame(stddev, stddev.setDefaultValue(2f));
        /*
         * Pretend that we changed our mind and now want a Double type instead of Float.
         * In current implementation this requires the creation of a new builder instance,
         * but there is no guarantee that it will always be the case in future versions.
         */
        final AttributeTypeBuilder<Double> newb = builder.setValueClass(Double.class);
        assertEquals("name",          "temperature",      newb.getName().toString());
        assertEquals("definition",    "test definition",  newb.getDefinition());
        assertEquals("description",   "test description", newb.getDescription());
        assertEquals("designation",   "test designation", newb.getDesignation());
        assertEquals("valueClass",    Double.class,       newb.getValueClass());
        assertEquals("defaultValue",  Double.valueOf(25), newb.getDefaultValue());
        assertSetEquals(Set.of(newb), newb.owner().properties());
        /*
         * In order to avoid accidental misuse, the old builder should not be usable anymore.
         */
        try {
            builder.setName("new name");
            fail("Should not allow modification of disposed instance.");
        } catch (IllegalStateException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("AttributeTypeBuilder"));
        }
        /*
         * Verify the attribute created by the builder.
         */
        final AttributeType<?> att = newb.build();
        assertEquals("name",          "temperature",      att.getName().toString());
        assertEquals("definition",    "test definition",  att.getDefinition().toString());
        assertEquals("description",   "test description", att.getDescription().toString());
        assertEquals("designation",   "test designation", att.getDesignation().toString());
        assertEquals("valueClass",    Double.class,       att.getValueClass());
        assertEquals("defaultValue",  Double.valueOf(25), att.getDefaultValue());
    }

    /**
     * Tests {@link AttributeTypeBuilder#setValidValues(Object...)} and the corresponding getter method.
     */
    @Test
    @DependsOnMethod("testBuilder")
    public void testSetValidValues() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        assertEquals("length", 0, builder.getValidValues().length);
        assertSame(builder, builder.setValidValues("Blue", "Green", "Red"));
        assertArrayEquals(new String[] {"Blue", "Green", "Red"}, builder.getValidValues());
        assertSame(builder, builder.setValidValues("Yellow", "Cyan", "Magenta"));
        assertArrayEquals(new String[] {"Yellow", "Cyan", "Magenta"}, builder.getValidValues());
    }

    /**
     * Tests {@link AttributeTypeBuilder#setMaximalLength(Integer)} and
     * {@link AttributeTypeBuilder#setCRS(CoordinateReferenceSystem)}
     * together with the corresponding getter methods.
     */
    @Test
    @DependsOnMethod("testBuilder")
    public void testOtherCharacteristics() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        assertNull("maximalLength", builder.getMaximalLength());
        assertNull("crs", builder.getCRS());

        assertSame(builder, builder.setMaximalLength(20));
        assertEquals("maximalLength", Integer.valueOf(20), builder.getMaximalLength());
        assertNull("crs", builder.getCRS());

        final CoordinateReferenceSystem crs = CommonCRS.defaultGeographic();
        assertSame(builder, builder.setCRS(crs));
        assertEquals("maximalLength", Integer.valueOf(20), builder.getMaximalLength());
        assertSame("crs", crs, builder.getCRS());

        assertSame(builder, builder.setMaximalLength(30));
        assertEquals("maximalLength", Integer.valueOf(30), builder.getMaximalLength());
        assertSame("crs", crs, builder.getCRS());
    }

    /**
     * Tests {@link AttributeTypeBuilder#getCharacteristic(String)}.
     */
    @Test
    @DependsOnMethod("testOtherCharacteristics")
    public void testGetCharacteristics() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        final CharacteristicTypeBuilder<Float> a = builder.addCharacteristic(Float.class).setName("a", "temp");
        final CharacteristicTypeBuilder<Float> b = builder.addCharacteristic(Float.class).setName("b", "temp");
        final CharacteristicTypeBuilder<Float> c = builder.addCharacteristic(Float.class).setName("c");
        assertNull("dummy", builder.getCharacteristic("dummy"));
        assertSame("c", c, builder.getCharacteristic("c"));
        assertSame("b", b, builder.getCharacteristic("b:temp"));
        assertSame("a", a, builder.getCharacteristic("a:temp"));
        try {
            builder.getCharacteristic("temp");
            fail("Given name should be considered ambiguous.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("a:temp"));
            assertTrue(message, message.contains("b:temp"));
        }
    }

    /**
     * Tests {@link AttributeTypeBuilder#roles()}.
     */
    @Test
    @DependsOnMethod("testOtherCharacteristics")
    public void testRoles() {
        final AttributeTypeBuilder<Geometry> builder = new FeatureTypeBuilder().addAttribute(Geometry.class);
        final Set<AttributeRole> roles = builder.roles();
        assertTrue("isEmpty", roles.isEmpty());

        assertTrue("add(DEFAULT_GEOMETRY)", builder.addRole(AttributeRole.DEFAULT_GEOMETRY));
        assertSetEquals(Set.of(AttributeRole.DEFAULT_GEOMETRY), roles);
        assertFalse("add(DEFAULT_GEOMETRY)", builder.addRole(AttributeRole.DEFAULT_GEOMETRY));

        assertTrue("add(IDENTIFIER_COMPONENT)", roles.add(AttributeRole.IDENTIFIER_COMPONENT));
        assertSetEquals(List.of(AttributeRole.DEFAULT_GEOMETRY, AttributeRole.IDENTIFIER_COMPONENT), roles);
        assertFalse("add(IDENTIFIER_COMPONENT)", roles.add(AttributeRole.IDENTIFIER_COMPONENT));

        assertTrue("remove(DEFAULT_GEOMETRY)", roles.remove(AttributeRole.DEFAULT_GEOMETRY));
        assertSetEquals(Set.of(AttributeRole.IDENTIFIER_COMPONENT), roles);
        assertFalse("remove(DEFAULT_GEOMETRY)", roles.remove(AttributeRole.DEFAULT_GEOMETRY));

        assertTrue("remove(IDENTIFIER_COMPONENT)", roles.remove(AttributeRole.IDENTIFIER_COMPONENT));
        assertTrue("isEmpty", roles.isEmpty());
        assertFalse("remove(IDENTIFIER_COMPONENT)", roles.remove(AttributeRole.IDENTIFIER_COMPONENT));
    }

    /**
     * Verifies that {@link FeatureTypeBuilder#addAttribute(Class)} converts primitive types to their wrapper.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testBoxing() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder().setName("boxing");
        final AttributeTypeBuilder<Integer> boxBuilder = ftb.addAttribute(int.class).setName("boxed");
        assertEquals("Attribute value type should have been boxed", Integer.class, boxBuilder.getValueClass());

        final FeatureType ft = ftb.build();
        final PropertyType boxedProperty = ft.getProperty("boxed");
        assertInstanceOf("Unexpected property type.", AttributeType.class, boxedProperty);
        assertEquals("Attribute value type should have been boxed", Integer.class, ((AttributeType<?>) boxedProperty).getValueClass());
        final Feature feature = ft.newInstance();

        final Property p = feature.getProperty("boxed");
        assertInstanceOf("Unexpected property type.", Attribute.class, p);
        assertEquals("Attribute value type should have been boxed", Integer.class, ((Attribute<?>) p).getType().getValueClass());

        int value = 3;
        Features.cast((Attribute<?>) p, Integer.class).setValue(value);
        assertEquals(value, p.getValue());

        feature.setPropertyValue("boxed", Integer.valueOf(4));
        assertEquals(4, feature.getPropertyValue("boxed"));
    }
}
