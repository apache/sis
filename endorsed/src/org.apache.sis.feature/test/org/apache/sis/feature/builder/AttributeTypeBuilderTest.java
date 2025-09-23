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
import org.apache.sis.feature.internal.shared.AttributeConvention;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSetEquals;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.apache.sis.feature.AbstractAttribute;
import org.apache.sis.feature.DefaultAttributeType;


/**
 * Tests {@link AttributeTypeBuilder}. The tests need to create a {@link FeatureTypeBuilder} in order to allow
 * {@code AttributeTypeBuilder} instantiation, but nothing else is done with the {@code FeatureTypeBuilder}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
public final class AttributeTypeBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public AttributeTypeBuilderTest() {
    }

    /**
     * Tests with the minimum number of parameters.
     */
    @Test
    public void testInitialization() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        assertEquals("string", builder.getName().toString());

        builder.setName("myScope", "myName");
        final var attribute = builder.build();

        assertEquals("myScope:myName", attribute.getName().toString());
        assertEquals(String.class, attribute.getValueClass());
        assertNull(attribute.getDefaultValue());
        assertNull(attribute.getDefinition());
        assertTrue(attribute.getDescription().isEmpty());
        assertTrue(attribute.getDesignation().isEmpty());
        assertEquals(1, attribute.getMinimumOccurs());
        assertEquals(1, attribute.getMaximumOccurs());
    }

    /**
     * Test creation of a single attribute with more values than the minimal ones.
     */
    @Test
    public void testBuilder() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        assertSame(builder, builder.setName        ("myScope", "myName"));
        assertSame(builder, builder.setDefinition  ("test definition"));
        assertSame(builder, builder.setDesignation ("test designation"));
        assertSame(builder, builder.setDescription ("test description"));
        assertSame(builder, builder.setDefaultValue("test default value."));
        assertSame(builder, builder.setMinimumOccurs(10).setMaximumOccurs(60));
        assertSame(builder, builder.setMaximalLength(80));
        final var attribute = builder.build();

        assertEquals("myScope:myName",      attribute.getName().toString());
        assertEquals("test definition",     attribute.getDefinition().toString());
        assertEquals("test description",    attribute.getDescription().orElseThrow().toString());
        assertEquals("test designation",    attribute.getDesignation().orElseThrow().toString());
        assertEquals(String.class,          attribute.getValueClass());
        assertEquals("test default value.", attribute.getDefaultValue());
        assertEquals(10,                    attribute.getMinimumOccurs());
        assertEquals(60,                    attribute.getMaximumOccurs());
        assertTrue  (AttributeConvention.characterizedByMaximalLength(attribute));
        assertEquals(Integer.valueOf(80), AttributeConvention.getMaximalLengthCharacteristic(null, attribute));
    }

    /**
     * Tests {@link AttributeTypeBuilder#setValueClass(Class)}.
     * This implies the replacement of the builder by a new instance.
     */
    @Test
    public void testSetValueClass() {
        final AttributeTypeBuilder<Float> builder = new FeatureTypeBuilder().addAttribute(Float.class);
        assertSame(builder, builder.setName        ("temperature"));
        assertSame(builder, builder.setDefinition  ("test definition"));
        assertSame(builder, builder.setDesignation ("test designation"));
        assertSame(builder, builder.setDescription ("test description"));
        assertSame(builder, builder.setDefaultValue(25f));
        assertSame(builder, builder.setValueClass(Float.class));
        assertEquals(Float.class, builder.getValueClass());
        assertSetEquals(Set.of(builder), builder.owner().properties());
        final CharacteristicTypeBuilder<Float> stddev = builder.addCharacteristic(Float.class);
        assertSame(stddev, stddev.setName("stddev"));
        assertSame(stddev, stddev.setDefaultValue(2f));
        /*
         * Pretend that we changed our mind and now want a Double type instead of Float.
         * In current implementation this requires the creation of a new builder instance,
         * but there is no guarantee that it will always be the case in future versions.
         */
        final AttributeTypeBuilder<Double> b2 = builder.setValueClass(Double.class);
        assertEquals("temperature",      b2.getName().toString());
        assertEquals("test definition",  b2.getDefinition());
        assertEquals("test description", b2.getDescription());
        assertEquals("test designation", b2.getDesignation());
        assertEquals(Double.class,       b2.getValueClass());
        assertEquals(Double.valueOf(25), b2.getDefaultValue());
        assertSetEquals(Set.of(b2), b2.owner().properties());
        /*
         * In order to avoid accidental misuse, the old builder should not be usable anymore.
         */
        var e = assertThrows(IllegalStateException.class, () -> builder.setName("new name"),
                             "Should not allow modification of disposed instance.");
        assertMessageContains(e, "AttributeTypeBuilder");
        /*
         * Verify the attribute created by the builder.
         */
        final var attribute = b2.build();
        assertEquals("temperature",      attribute.getName().toString());
        assertEquals("test definition",  attribute.getDefinition().toString());
        assertEquals("test description", attribute.getDescription().orElseThrow().toString());
        assertEquals("test designation", attribute.getDesignation().orElseThrow().toString());
        assertEquals(Double.class,       attribute.getValueClass());
        assertEquals(Double.valueOf(25), attribute.getDefaultValue());
    }

    /**
     * Tests {@link AttributeTypeBuilder#setValidValues(Object...)} and the corresponding getter method.
     */
    @Test
    public void testSetValidValues() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        assertEquals(0, builder.getValidValues().length);
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
    public void testOtherCharacteristics() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        assertNull(builder.getMaximalLength());
        assertNull(builder.getCRS());

        assertSame(builder, builder.setMaximalLength(20));
        assertEquals(Integer.valueOf(20), builder.getMaximalLength());
        assertNull(builder.getCRS());

        final CoordinateReferenceSystem crs = CommonCRS.defaultGeographic();
        assertSame(builder, builder.setCRS(crs));
        assertEquals(Integer.valueOf(20), builder.getMaximalLength());
        assertSame(crs, builder.getCRS());

        assertSame(builder, builder.setMaximalLength(30));
        assertEquals(Integer.valueOf(30), builder.getMaximalLength());
        assertSame(crs, builder.getCRS());
    }

    /**
     * Tests {@link AttributeTypeBuilder#getCharacteristic(String)}.
     */
    @Test
    public void testGetCharacteristics() {
        final AttributeTypeBuilder<String> builder = new FeatureTypeBuilder().addAttribute(String.class);
        final CharacteristicTypeBuilder<Float> a = builder.addCharacteristic(Float.class).setName("a", "temp");
        final CharacteristicTypeBuilder<Float> b = builder.addCharacteristic(Float.class).setName("b", "temp");
        final CharacteristicTypeBuilder<Float> c = builder.addCharacteristic(Float.class).setName("c");
        assertNull(builder.getCharacteristic("dummy"));
        assertSame(c, builder.getCharacteristic("c"));
        assertSame(b, builder.getCharacteristic("b:temp"));
        assertSame(a, builder.getCharacteristic("a:temp"));
        var e = assertThrows(IllegalArgumentException.class, () -> builder.getCharacteristic("temp"),
                             "Given name should be considered ambiguous.");
        assertMessageContains(e, "a:temp", "b:temp");
    }

    /**
     * Tests {@link AttributeTypeBuilder#roles()}.
     */
    @Test
    public void testRoles() {
        final AttributeTypeBuilder<Geometry> builder = new FeatureTypeBuilder().addAttribute(Geometry.class);
        final Set<AttributeRole> roles = builder.roles();
        assertTrue(roles.isEmpty());

        assertTrue(builder.addRole(AttributeRole.DEFAULT_GEOMETRY));
        assertSetEquals(Set.of(AttributeRole.DEFAULT_GEOMETRY), roles);
        assertFalse(builder.addRole(AttributeRole.DEFAULT_GEOMETRY));

        assertTrue(roles.add(AttributeRole.IDENTIFIER_COMPONENT));
        assertSetEquals(List.of(AttributeRole.DEFAULT_GEOMETRY, AttributeRole.IDENTIFIER_COMPONENT), roles);
        assertFalse(roles.add(AttributeRole.IDENTIFIER_COMPONENT));

        assertTrue(roles.remove(AttributeRole.DEFAULT_GEOMETRY));
        assertSetEquals(Set.of(AttributeRole.IDENTIFIER_COMPONENT), roles);
        assertFalse(roles.remove(AttributeRole.DEFAULT_GEOMETRY));

        assertTrue(roles.remove(AttributeRole.IDENTIFIER_COMPONENT));
        assertTrue(roles.isEmpty());
        assertFalse(roles.remove(AttributeRole.IDENTIFIER_COMPONENT));
    }

    /**
     * Verifies that {@link FeatureTypeBuilder#addAttribute(Class)} converts primitive types to their wrapper.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testBoxing() {
        final var ftb = new FeatureTypeBuilder().setName("boxing");
        final AttributeTypeBuilder<Integer> boxBuilder = ftb.addAttribute(int.class).setName("boxed");
        assertEquals(Integer.class, boxBuilder.getValueClass(), "Attribute value type should have been boxed");

        final var featureType  = ftb.build();
        final var propertyType = assertInstanceOf(DefaultAttributeType.class, featureType.getProperty("boxed"));
        assertEquals(Integer.class, propertyType.getValueClass(), "Attribute value type should have been boxed");

        final var feature  = featureType.newInstance();
        final var property = assertInstanceOf(AbstractAttribute.class, feature.getProperty("boxed"));
        assertEquals(Integer.class, property.getType().getValueClass(), "Attribute value type should have been boxed");

        int value = 3;
        Features.cast(property, Integer.class).setValue(value);
        assertEquals(value, property.getValue());

        feature.setPropertyValue("boxed", Integer.valueOf(4));
        assertEquals(4, feature.getPropertyValue("boxed"));
    }
}
