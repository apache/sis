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
package org.apache.sis.util;

import java.util.Locale;
import static org.apache.sis.util.DefaultInternationalStringTest.MESSAGE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the {@link SimpleInternationalString} implementation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class SimpleInternationalStringTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public SimpleInternationalStringTest() {
    }

    /**
     * Tests the {@link SimpleInternationalString} implementation.
     */
    @Test
    public void testSimple() {
        final var toTest = new SimpleInternationalString(MESSAGE);
        assertSame(MESSAGE, toTest.toString());
        assertSame(MESSAGE, toTest.toString(null));
        assertSame(MESSAGE, toTest.toString(Locale.ROOT));
        assertSame(MESSAGE, toTest.toString(Locale.JAPANESE));
        validate(toTest);
    }

    /**
     * Tests the {@link SimpleInternationalString} serialization.
     */
    @Test
    public void testSerialization() {
        final var before = new SimpleInternationalString(MESSAGE);
        final var after  = assertSerializedEquals(before);
        assertEquals(MESSAGE, after.toString());
        assertEquals(MESSAGE, after.toString(null));
        assertEquals(MESSAGE, after.toString(Locale.ROOT));
        assertEquals(MESSAGE, after.toString(Locale.JAPANESE));
        validate(after);
    }

    /**
     * Tests the formatting in a {@code printf} statement.
     */
    @Test
    public void testPrintf() {
        final var toTest = new SimpleInternationalString(MESSAGE);
        assertEquals(MESSAGE,                               String.format("%s", toTest));
        assertEquals("    This is an unlocalized message.", String.format("%35s", toTest));
        assertEquals("This is an unlocalized message.    ", String.format("%-35s", toTest));
        assertEquals("This is a…",                          String.format("%1.10s", toTest));
        assertEquals("This is a…  ",                        String.format("%-12.10s", toTest));
    }
}
