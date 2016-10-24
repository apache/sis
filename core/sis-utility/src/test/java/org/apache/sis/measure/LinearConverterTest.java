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
package org.apache.sis.measure;

import java.lang.reflect.Field;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.ArraysExt;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link LinearConverter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final strictfp class LinearConverterTest extends TestCase {
    /**
     * Ensures that the characters in the {@link LinearConverter#PREFIXES} array are in strictly increasing order,
     * and that {@link LinearConverter#POWERS} has the same length.
     *
     * @throws ReflectiveOperationException if this test can not access the private fields of {@link LinearConverter}.
     */
    @Test
    public void verifyPrefixes() throws ReflectiveOperationException {
        Field f = LinearConverter.class.getDeclaredField("PREFIXES");
        f.setAccessible(true);
        final char[] prefixes = (char[]) f.get(null);
        assertTrue(ArraysExt.isSorted(prefixes, true));

        f = LinearConverter.class.getDeclaredField("POWERS");
        f.setAccessible(true);
        assertEquals("length", prefixes.length, ((byte[]) f.get(null)).length);
    }
}
