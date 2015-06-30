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
package org.apache.sis.internal.metadata;

import org.junit.Test;
import java.lang.reflect.Field;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;


/**
 * Tests the {@link AxisNames} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class AxisNamesTest extends TestCase {
    /**
     * Tests {@link AxisNames#toCamelCase(String)}.
     */
    @Test
    public void testToCamelCase() {
        assertEquals("Latitude",           AxisNames.toCamelCase("latitude"));
        assertEquals("Geodetic longitude", AxisNames.toCamelCase("geodetic_longitude"));
        assertEquals("Ellipsoidal height", AxisNames.toCamelCase("ellipsoidal height"));
        assertEquals("unknown name",       AxisNames.toCamelCase("unknown name"));
    }

    /**
     * Tests the consistency between the constant names and their values.
     *
     * @throws IllegalAccessException should never happen.
     *         If it happen, the {@link AxisNames} static initializer may need to be revisited.
     */
    @Test
    public void testConsistency() throws IllegalAccessException {
        for (final Field f : AxisNames.class.getFields()) {
            final String name = f.getName();
            assertSame(name, f.get(null), AxisNames.toCamelCase(name));
        }
    }
}
