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
package org.apache.sis.storage.geotiff;

import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Type} enumeration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public final strictfp class TypeTest extends TestCase {
    /**
     * Verifies that all enumeration values override either {@link Type#readLong(ChannelDataInput, long)}
     * or {@link Type#readDouble(ChannelDataInput, long)}.Failing to do so may cause stack overflow.
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
            final boolean readLong   = c.getMethod("readLong",   parameters).getDeclaringClass() == Type.class;
            final boolean readDouble = c.getMethod("readDouble", parameters).getDeclaringClass() == Type.class;
            assertFalse(type.name(), readLong & readDouble);
        }
    }

    /**
     * Tests {@link Type#valueOf(int)}.
     */
    @Test
    public void testValueOf() {
        assertEquals("UBYTE",     Type.UBYTE,     Type.valueOf( 1));
        assertEquals("ASCII",     Type.ASCII,     Type.valueOf( 2));
        assertEquals("USHORT",    Type.USHORT,    Type.valueOf( 3));
        assertEquals("UINT",      Type.UINT,      Type.valueOf( 4));
        assertEquals("URATIONAL", Type.URATIONAL, Type.valueOf( 5));      // unsigned Integer / unsigned Integer
        assertEquals("BYTE",      Type.BYTE,      Type.valueOf( 6));
        assertEquals("UNDEFINED", Type.UNDEFINED, Type.valueOf( 7));
        assertEquals("SHORT",     Type.SHORT,     Type.valueOf( 8));
        assertEquals("INT",       Type.INT,       Type.valueOf( 9));
        assertEquals("RATIONAL",  Type.RATIONAL,  Type.valueOf(10));     // signed Integer / signed Integer
        assertEquals("FLOAT",     Type.FLOAT,     Type.valueOf(11));
        assertEquals("DOUBLE",    Type.DOUBLE,    Type.valueOf(12));
        assertEquals("IFD",       Type.UINT,      Type.valueOf(13));     // IFD is like UINT.
        assertEquals("ULONG",     Type.ULONG,     Type.valueOf(16));
        assertEquals("LONG",      Type.LONG,      Type.valueOf(17));
        assertEquals("IFD8",      Type.ULONG,     Type.valueOf(18));     // IFD8 is like ULONG.
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
