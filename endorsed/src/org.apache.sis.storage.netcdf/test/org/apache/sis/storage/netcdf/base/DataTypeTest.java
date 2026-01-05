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
package org.apache.sis.storage.netcdf.base;

import org.apache.sis.math.NumberType;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link DataType} enumeration values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DataTypeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DataTypeTest() {
    }

    /**
     * Verifies the relationship between the enumeration ordinal value and the netCDF numerical code.
     */
    @Test
    public void testOrdinalValues() {
        verifyOrdinal( 1, DataType.BYTE);
        verifyOrdinal( 2, DataType.CHAR);
        verifyOrdinal( 3, DataType.SHORT);
        verifyOrdinal( 4, DataType.INT);
        verifyOrdinal( 5, DataType.FLOAT);
        verifyOrdinal( 6, DataType.DOUBLE);
        verifyOrdinal( 7, DataType.UBYTE);
        verifyOrdinal( 8, DataType.USHORT);
        verifyOrdinal( 9, DataType.UINT);
        verifyOrdinal(10, DataType.INT64);
        verifyOrdinal(11, DataType.UINT64);
        verifyOrdinal(12, DataType.STRING);
    }

    /**
     * Verifies the ordinal value of the given data type.
     */
    private static void verifyOrdinal(final int codeNetCDF, final DataType type) {
        assertEquals(codeNetCDF, type.ordinal(), type.name());
        assertSame(type, DataType.valueOf(codeNetCDF), "DataType.valueOf(int)");
    }

    /**
     * Tests {@link DataType#unsigned(boolean)}.
     */
    @Test
    public void testUnsigned() {
        verifyUnsigned(DataType.BYTE,   DataType.UBYTE);
        verifyUnsigned(DataType.SHORT,  DataType.USHORT);
        verifyUnsigned(DataType.INT,    DataType.UINT);
        verifyUnsigned(DataType.INT64,  DataType.UINT64);
        verifyUnsigned(DataType.FLOAT,  DataType.FLOAT);
        verifyUnsigned(DataType.DOUBLE, DataType.DOUBLE);
        verifyUnsigned(DataType.STRING, DataType.STRING);
        verifyUnsigned(DataType.CHAR,   DataType.CHAR);
    }

    /**
     * Verifies the relationship between signed and unsigned data types.
     */
    private static void verifyUnsigned(final DataType signed, final DataType unsigned) {
        assertSame(  signed,   signed.unsigned(false));
        assertSame(  signed, unsigned.unsigned(false));
        assertSame(unsigned, unsigned.unsigned(true));
        assertSame(unsigned,   signed.unsigned(true));
    }

    /**
     * Tests {@link DataType#isInteger}.
     */
    @Test
    public void testIsNumber() {
        assertTrue (DataType.BYTE  .isInteger);
        assertTrue (DataType.UBYTE .isInteger);
        assertTrue (DataType.SHORT .isInteger);
        assertTrue (DataType.USHORT.isInteger);
        assertTrue (DataType.INT   .isInteger);
        assertTrue (DataType.UINT  .isInteger);
        assertTrue (DataType.INT64 .isInteger);
        assertTrue (DataType.UINT64.isInteger);
        assertFalse(DataType.FLOAT .isInteger);
        assertFalse(DataType.DOUBLE.isInteger);
        assertFalse(DataType.CHAR  .isInteger);
        assertFalse(DataType.STRING.isInteger);
    }

    /**
     * Verifies the {@link DataType#classe} values.
     */
    @Test
    public void testClasses() {
        for (final DataType dataType : DataType.values()) {
            final Class<?> c = dataType.getClass(false);
            if (Number.class.isAssignableFrom(c)) {
                final NumberType numberType = NumberType.forNumberClass(c);
                final String name = dataType.name();
                if (dataType.isInteger) {
                    if (!dataType.isUnsigned) {
                        assertEquals(dataType.number, numberType, name);
                    } else if (dataType != DataType.UINT64) {
                        assertTrue(numberType.isWiderThan(dataType.number), name);
                    }
                } else {
                    assertEquals((dataType == DataType.CHAR) ? NumberType.CHARACTER : dataType.number, numberType, name);
                }
            }
        }
    }
}
