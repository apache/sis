/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License)); Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing)); software
 * distributed under the License is distributed on an "AS IS" BASIS));
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND)); either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.geotiff.reader;

import org.apache.sis.io.stream.ChannelDataInput;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link Type} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TypeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TypeTest() {
    }

    /**
     * Verifies that all enumeration values override either {@link Type#readAsLong(ChannelDataInput, long)}
     * or {@link Type#readAsDouble(ChannelDataInput, long)}. Failing to do so may cause stack overflow.
     *
     * @throws NoSuchMethodException if a reflective operation failed.
     */
    @Test
    public void testOverride() throws NoSuchMethodException {
        final Class<?>[] parameters = {
            ChannelDataInput.class,
            Long.TYPE
        };
        for (final Type type : Type.values()) {
            final Class<?> c = type.getClass();
            final boolean readLong   = c.getMethod("readAsLong",   parameters).getDeclaringClass() == Type.class;
            final boolean readDouble = c.getMethod("readAsDouble", parameters).getDeclaringClass() == Type.class;
            assertFalse(readLong & readDouble, type.name());
        }
    }

    /**
     * Tests {@link Type#valueOf(int)}.
     */
    @Test
    public void testValueOf() {
        assertEquals(Type.UBYTE,     Type.valueOf( 1), "UBYTE");
        assertEquals(Type.ASCII,     Type.valueOf( 2), "ASCII");
        assertEquals(Type.USHORT,    Type.valueOf( 3), "USHORT");
        assertEquals(Type.UINT,      Type.valueOf( 4), "UINT");
        assertEquals(Type.URATIONAL, Type.valueOf( 5), "URATIONAL");    // unsigned Integer / unsigned Integer
        assertEquals(Type.BYTE,      Type.valueOf( 6), "BYTE");
        assertEquals(Type.UNDEFINED, Type.valueOf( 7), "UNDEFINED");
        assertEquals(Type.SHORT,     Type.valueOf( 8), "SHORT");
        assertEquals(Type.INT,       Type.valueOf( 9), "INT");
        assertEquals(Type.RATIONAL,  Type.valueOf(10), "RATIONAL");     // signed Integer / signed Integer
        assertEquals(Type.FLOAT,     Type.valueOf(11), "FLOAT");
        assertEquals(Type.DOUBLE,    Type.valueOf(12), "DOUBLE");
        assertEquals(Type.UINT,      Type.valueOf(13), "IFD");          // IFD is like UINT.
        assertEquals(Type.ULONG,     Type.valueOf(16), "ULONG");
        assertEquals(Type.LONG,      Type.valueOf(17), "LONG");
        assertEquals(Type.ULONG,     Type.valueOf(18), "IFD8");         // IFD8 is like ULONG.
    }

    /**
     * Verifies values of the {@link Type#size} property.
     */
    @Test
    public void verifySize() {
        verifySize(Byte.SIZE,       Type.BYTE,  Type.UBYTE, Type.ASCII);
        verifySize(Short.SIZE,      Type.SHORT, Type.USHORT);
        verifySize(Integer.SIZE,    Type.INT,   Type.UINT);
        verifySize(Long.SIZE,       Type.LONG,  Type.ULONG);
        verifySize(Float.SIZE,      Type.FLOAT);
        verifySize(Double.SIZE,     Type.DOUBLE);
        verifySize(Integer.SIZE*2,  Type.URATIONAL, Type.RATIONAL);
    }

    /**
     * Verifies that all given values have the expected size.
     */
    private static void verifySize(final int expected, final Type... values) {
        for (final Type type : values) {
            assertEquals(expected, type.size * Byte.SIZE);
        }
    }
}
