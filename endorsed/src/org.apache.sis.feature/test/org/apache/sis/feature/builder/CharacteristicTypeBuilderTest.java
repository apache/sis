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

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSetEquals;


/**
 * Tests {@link CharacteristicTypeBuilder}. The tests need to create a {@link AttributeTypeBuilder} in order to allow
 * {@code CharacteristicTypeBuilder} instantiation, but nothing else is done with the {@code AttributeTypeBuilder}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CharacteristicTypeBuilderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CharacteristicTypeBuilderTest() {
    }

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
        assertEquals(Integer.class, builder.getValueClass());
        assertSetEquals(Set.of(builder), owner.characteristics());
        /*
         * Pretend that we changed our mind and now want a Float type instead of Integer.
         * In current implementation this requires the creation of a new builder instance,
         * but there is no guarantee that it will always be the case in future versions.
         */
        final CharacteristicTypeBuilder<Float> b2 = builder.setValueClass(Float.class);
        assertEquals("stddev",           b2.getName().toString());
        assertEquals("test definition",  b2.getDefinition());
        assertEquals("test description", b2.getDescription());
        assertEquals("test designation", b2.getDesignation());
        assertEquals(Float.class,        b2.getValueClass());
        assertEquals(Float.valueOf(2),   b2.getDefaultValue());
        assertSetEquals(Set.of(b2), owner.characteristics());
        /*
         * In order to avoid accidental misuse, the old builder should not be usable anymore.
         */
        var e = assertThrows(IllegalStateException.class, () -> builder.setName("new name"),
                             "Should not allow modification of disposed instance.");
        assertMessageContains(e, "CharacteristicTypeBuilder");
        /*
         * Verify the characteristic created by the builder.
         */
        final var attribute = b2.build();
        assertEquals("stddev",           attribute.getName().toString());
        assertEquals("test definition",  attribute.getDefinition().toString());
        assertEquals("test description", attribute.getDescription().toString());
        assertEquals("test designation", attribute.getDesignation().toString());
        assertEquals(Float.class,        attribute.getValueClass());
        assertEquals(Float.valueOf(2),   attribute.getDefaultValue());
    }
}
