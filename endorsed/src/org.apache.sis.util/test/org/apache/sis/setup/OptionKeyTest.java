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
package org.apache.sis.setup;

import java.util.Map;
import org.apache.sis.util.collection.CheckedContainer;
import static org.apache.sis.setup.OptionKey.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests {@link OptionKey}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class OptionKeyTest extends TestCase {
    /**
     * A custom subclass of {@link OptionKey} for testing the ability to create custom option.
     * This subclass implements {@link CheckedContainer} for ensuring that the {@code OptionKey}
     * API is compatible with {@code CheckedContainer}. The public class does not implement that
     * interface because a key is not a container. However, we keep this possibility open in case
     * some users find this approach convenient for their own keys.
     */
    @SuppressWarnings("serial")
    private static final class CustomKey<T> extends OptionKey<T> implements CheckedContainer<T> {
        CustomKey(final String name, final Class<T> type) {
            super(name, type);
        }

        @Override public final Mutability getMutability() {
            return Mutability.IMMUTABLE;
        }
    }

    /**
     * Creates a new test case.
     */
    public OptionKeyTest() {
    }

    /**
     * Tests the {@link OptionKey#getValueFrom(Map)} and {@link OptionKey#setValueInto(Map, Object)}
     * methods with null arguments.
     */
    @Test
    public void testNullArguments() {
        assertNull(URL_ENCODING.getValueFrom(null));
        assertNull(URL_ENCODING.setValueInto(null, null));
    }

    /**
     * Tests the {@link OptionKey#setValueInto(Map, Object)} method
     * followed by {@link OptionKey#getValueFrom(Map)}.
     */
    @Test
    public void testSetAndGet() {
        final Map<OptionKey<?>,Object> options = URL_ENCODING.setValueInto(null, "UTF-8");
        assertEquals("UTF-8", assertSingleton(options.values()));
        assertEquals("UTF-8", URL_ENCODING.getValueFrom(options));

        assertSame(options, URL_ENCODING.setValueInto(options, "ISO-8859-1"));
        assertEquals("ISO-8859-1", assertSingleton(options.values()));
        assertEquals("ISO-8859-1", URL_ENCODING.getValueFrom(options));
    }

    /**
     * Tests the serialization of constants.
     * Those constants shall be resolved to their singleton instance on deserialization.
     */
    @Test
    public void testSerialization() {
        assertSame(URL_ENCODING, assertSerializedEquals(URL_ENCODING));
        assertSame(OPEN_OPTIONS, assertSerializedEquals(OPEN_OPTIONS));
    }

    /**
     * Tests the serialization of a custom subclass. {@link OptionKey} cannot resolve
     * to a unique instance, unless the subclass provides its own resolution mechanism.
     */
    @Test
    public void testSubclassSerialization() {
        final CustomKey<Integer> key = new CustomKey<>("key", Integer.class);
        assertNotSame(key, assertSerializedEquals(key));
    }
}
