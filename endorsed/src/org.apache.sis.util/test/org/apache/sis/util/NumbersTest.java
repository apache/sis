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

import static org.apache.sis.util.Numbers.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link Numbers} static methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class NumbersTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public NumbersTest() {
    }

    /**
     * Tests {@link Numbers#round(Number)}.
     */
    @Test
    public void testRound() {
        assertEquals(123456, Numbers.round(123456.2f));
        var e = assertThrows(ArithmeticException.class, () -> Numbers.round(Long.MAX_VALUE * 3d));
        assertMessageContains(e);
    }

    /**
     * Tests {@link Numbers#widestClass(Class, Class)}.
     */
    @Test
    public void testWidestClass() {
        assertEquals(Byte   .class, widestClass(Byte.class,    Byte.class));
        assertEquals(Integer.class, widestClass(Byte.class,    Integer.class));
        assertEquals(Integer.class, widestClass(Integer.class, Byte.class));
        assertEquals(Float  .class, widestClass(Integer.class, Float.class));
    }

    /**
     * Tests {@link Numbers#narrowestClass(Class, Class)}.
     */
    @Test
    public void testNarrowestClass() {
        assertEquals(Byte   .class, narrowestClass(Byte.class,    Byte.class));
        assertEquals(Byte   .class, narrowestClass(Byte.class,    Integer.class));
        assertEquals(Byte   .class, narrowestClass(Integer.class, Byte.class));
        assertEquals(Integer.class, narrowestClass(Integer.class, Float.class));
    }

    /**
     * Tests {@link Numbers#narrowestClass(Number)}.
     */
    @Test
    public void testNarrowestClassForValue() {
        assertEquals(Byte   .class, narrowestClass(    127.0));
        assertEquals(Short  .class, narrowestClass(    128.0));
        assertEquals(Integer.class, narrowestClass( 100000.0));
        assertEquals(Float  .class, narrowestClass(     10.5));
        assertEquals(Byte   .class, narrowestClass(   -128  ));
        assertEquals(Short  .class, narrowestClass(   -129  ));
        assertEquals(Integer.class, narrowestClass(-100000  ));
        assertEquals(Integer.class, narrowestClass((double) (1L << 30)));
        assertEquals(Float  .class, narrowestClass((double) (1L << 40)));
        assertEquals(Double .class, narrowestClass(StrictMath.PI));
    }

    /**
     * Tests {@link Numbers#narrowestNumber(Number)}.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testNarrowestNumber() {
        assertEquals(Byte   .valueOf((byte)   127),  narrowestNumber(    127.0));
        assertEquals(Short  .valueOf((short)  128),  narrowestNumber(    128.0));
        assertEquals(Integer.valueOf(      100000),  narrowestNumber( 100000.0));
        assertEquals(Float  .valueOf(       10.5f),  narrowestNumber(     10.5));
        assertEquals(Byte   .valueOf((byte)  -128),  narrowestNumber(   -128  ));
        assertEquals(Short  .valueOf((short) -129),  narrowestNumber(   -129  ));
        assertEquals(Integer.valueOf(     -100000),  narrowestNumber(-100000  ));
        assertEquals(Integer.valueOf(1  << 30),      narrowestNumber((double) (1L << 30)));
        assertEquals(Float  .valueOf(1L << 40),      narrowestNumber((double) (1L << 40)));
        assertEquals(Double .valueOf(StrictMath.PI), narrowestNumber(StrictMath.PI));
    }

    /**
     * Tests {@link Numbers#cast(Number, Class)}.
     */
    @Test
    public void testCast() {
        final Integer value = 10;
        assertEquals(Byte   .valueOf((byte)   10), cast(value, Byte   .class));
        assertEquals(Short  .valueOf((short)  10), cast(value, Short  .class));
        assertSame  (value,                        cast(value, Integer.class));
        assertEquals(Long   .valueOf((long)   10), cast(value, Long   .class));
        assertEquals(Float  .valueOf((float)  10), cast(value, Float  .class));
        assertEquals(Double .valueOf((double) 10), cast(value, Double .class));
    }

    /**
     * Tests {@link Numbers#wrap(double, Class)}.
     */
    @Test
    public void testWrap() {
        final double value = 10;
        assertEquals(Byte   .valueOf((byte)   10), wrap(value, Byte   .class));
        assertEquals(Short  .valueOf((short)  10), wrap(value, Short  .class));
        assertEquals(Integer.valueOf(         10), wrap(value, Integer.class));
        assertEquals(Long   .valueOf((long)   10), wrap(value, Long   .class));
        assertEquals(Float  .valueOf((float)  10), wrap(value, Float  .class));
        assertEquals(Double .valueOf((double) 10), wrap(value, Double .class));

        var e = assertThrows(IllegalArgumentException.class, () -> wrap(4.5, Integer.class));
        assertMessageContains(e, "Integer");
    }
}
