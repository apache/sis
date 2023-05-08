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
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSetEquals;

// Branch-dependent imports
import org.opengis.feature.AttributeType;


/**
 * Tests {@link CharacteristicTypeBuilder}. The tests need to create a {@link AttributeTypeBuilder} in order to allow
 * {@code CharacteristicTypeBuilder} instantiation, but nothing else is done with the {@code AttributeTypeBuilder}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 */
public final class CharacteristicTypeBuilderTest extends TestCase {
    /**
     * Tests {@link CharacteristicTypeBuilder#setValueClass(Class)}.
     * This implies the replacement of the builder by a new instance.
     */
    @Test
    public void testSetValueClass() {
        final AttributeTypeBuilder<Float> owner = new FeatureTypeBuilder().addAttribute(Float.class);
        final CharacteristicTypeBuilder<Integer> builder = owner.addCharacteristic(Integer.class);
        assertSame(builder, builder.setName        ("stddev"));
        assertSame(builder, builder.setDefinition  ("test definition"));
        assertSame(builder, builder.setDesignation ("test designation"));
        assertSame(builder, builder.setDescription ("test description"));
        assertSame(builder, builder.setDefaultValue(2));
        assertSame(builder, builder.setValueClass(Integer.class));
        assertEquals("valueClass", Integer.class, builder.getValueClass());
        assertSetEquals(Set.of(builder), owner.characteristics());
        /*
         * Pretend that we changed our mind and now want a Float type instead of Integer.
         * In current implementation this requires the creation of a new builder instance,
         * but there is no guarantee that it will always be the case in future versions.
         */
        final CharacteristicTypeBuilder<Float> newb = builder.setValueClass(Float.class);
        assertEquals("name",          "stddev",           newb.getName().toString());
        assertEquals("definition",    "test definition",  newb.getDefinition());
        assertEquals("description",   "test description", newb.getDescription());
        assertEquals("designation",   "test designation", newb.getDesignation());
        assertEquals("valueClass",    Float.class,        newb.getValueClass());
        assertEquals("defaultValue",  Float.valueOf(2),   newb.getDefaultValue());
        assertSetEquals(Set.of(newb), owner.characteristics());
        /*
         * In order to avoid accidental misuse, the old builder should not be usable anymore.
         */
        try {
            builder.setName("new name");
            fail("Should not allow modification of disposed instance.");
        } catch (IllegalStateException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("CharacteristicTypeBuilder"));
        }
        /*
         * Verify the characteristic created by the builder.
         */
        final AttributeType<?> att = newb.build();
        assertEquals("name",          "stddev",           att.getName().toString());
        assertEquals("definition",    "test definition",  att.getDefinition().toString());
        assertEquals("description",   "test description", att.getDescription().toString());
        assertEquals("designation",   "test designation", att.getDesignation().toString());
        assertEquals("valueClass",    Float.class,        att.getValueClass());
        assertEquals("defaultValue",  Float.valueOf(2),   att.getDefaultValue());
    }
}
