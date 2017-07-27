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
package org.apache.sis.internal.netcdf;

import org.junit.Test;
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;


/**
 * Tests {@link DataType} enumeration values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class DataTypeTest extends TestCase {
    /**
     * Verifies the relationship between the enumeration ordinal value and the NetCDF numerical code.
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
        assertEquals(type.name(), codeNetCDF, type.ordinal());
        assertSame("DataType.valueOf(int)", type, DataType.valueOf(codeNetCDF));
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
        assertSame(  "signed",   signed,   signed.unsigned(false));
        assertSame(  "signed",   signed, unsigned.unsigned(false));
        assertSame("unsigned", unsigned, unsigned.unsigned(true));
        assertSame("unsigned", unsigned,   signed.unsigned(true));
    }
}
