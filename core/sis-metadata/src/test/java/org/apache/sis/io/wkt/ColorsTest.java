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
package org.apache.sis.io.wkt;

import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link Colors} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class ColorsTest extends TestCase {
    /**
     * Tests {@link Colors#getName(ElementKind)}.
     */
    @Test
    public void testGetName() {
        final Colors colors = Colors.DEFAULT;
        assertEquals("cyan",  colors.getName(ElementKind.CODE_LIST));
        assertEquals("green", colors.getName(ElementKind.METHOD));
        assertEquals("red",   colors.getName(ElementKind.IDENTIFIER));
        assertEquals("red",   colors.getName(ElementKind.ERROR));
    }

    /**
     * Tests {@link Colors#setName(ElementKind, String)}.
     */
    @Test
    @DependsOnMethod("testGetName")
    public void testSetName() {
        final Colors colors = new Colors(Colors.DEFAULT);
        assertEquals("green", colors.getName(ElementKind.METHOD));
        colors.setName(ElementKind.METHOD, "blue");
        assertEquals("blue", colors.getName(ElementKind.METHOD));
    }

    /**
     * Ensures that the static constant is immutable.
     */
    @Test
    public void testImmutability() {
        try {
            Colors.DEFAULT.setName(ElementKind.METHOD, "blue");
            fail("Constant shall be immutable.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
            final String message = e.getMessage();
            assertTrue(message, message.contains("Colors"));
        }
    }

    /**
     * Tests {@link Colors} serialization.
     */
    @Test
    public void testSerialization() {
        assertSame(Colors.DEFAULT, assertSerializedEquals(Colors.DEFAULT));
        final Colors colors = new Colors(Colors.DEFAULT);
        colors.setName(ElementKind.METHOD, "blue");
        final Colors c = assertSerializedEquals(colors);
        assertNotSame(colors, c); // Expect a new instance.
        assertEquals("blue", c.getName(ElementKind.METHOD));
    }
}
