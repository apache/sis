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
package org.apache.sis.image;

import java.awt.image.DataBuffer;
import java.awt.image.RasterFormatException;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Verifies {@link DataType}.
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
     * Verifies {@link DataType#toDataBufferType()}.
     */
    @Test
    public void verifyToDataBufferType() {
        assertEquals(DataBuffer.TYPE_BYTE  , DataType.BYTE  .toDataBufferType());
        assertEquals(DataBuffer.TYPE_USHORT, DataType.USHORT.toDataBufferType());
        assertEquals(DataBuffer.TYPE_SHORT , DataType.SHORT .toDataBufferType());
        assertEquals(DataBuffer.TYPE_INT   , DataType.INT   .toDataBufferType());
        assertEquals(DataBuffer.TYPE_INT   , DataType.UINT  .toDataBufferType());
        assertEquals(DataBuffer.TYPE_FLOAT , DataType.FLOAT .toDataBufferType());
        assertEquals(DataBuffer.TYPE_DOUBLE, DataType.DOUBLE.toDataBufferType());
    }

    /**
     * Tests {@link DataType#size()}.
     */
    @Test
    public void testSize() {
        assertEquals(Byte   .SIZE, DataType.BYTE  .size());
        assertEquals(Short  .SIZE, DataType.USHORT.size());
        assertEquals(Short  .SIZE, DataType.SHORT .size());
        assertEquals(Integer.SIZE, DataType.INT   .size());
        assertEquals(Integer.SIZE, DataType.UINT  .size());
        assertEquals(Float  .SIZE, DataType.FLOAT .size());
        assertEquals(Double .SIZE, DataType.DOUBLE.size());
    }

    /**
     * Tests {@link DataType#isUnsigned()}.
     */
    @Test
    public void testIsUnsigned() {
        assertTrue (DataType.BYTE  .isUnsigned());
        assertTrue (DataType.USHORT.isUnsigned());
        assertFalse(DataType.SHORT .isUnsigned());
        assertFalse(DataType.INT   .isUnsigned());
        assertTrue (DataType.UINT  .isUnsigned());
        assertFalse(DataType.FLOAT .isUnsigned());
        assertFalse(DataType.DOUBLE.isUnsigned());
    }

    /**
     * Tests {@link DataType#isInteger()}.
     */
    @Test
    public void testIsInteger() {
        assertTrue (DataType.BYTE  .isInteger());
        assertTrue (DataType.USHORT.isInteger());
        assertTrue (DataType.SHORT .isInteger());
        assertTrue (DataType.INT   .isInteger());
        assertTrue (DataType.UINT  .isInteger());
        assertFalse(DataType.FLOAT .isInteger());
        assertFalse(DataType.DOUBLE.isInteger());
    }

    /**
     * Tests {@link DataType#toPrimitive()}.
     */
    @Test
    public void testToPrimitive() {
        assertEquals(DataType.BYTE,   DataType.BYTE  .toPrimitive());
        assertEquals(DataType.SHORT,  DataType.USHORT.toPrimitive());
        assertEquals(DataType.SHORT,  DataType.SHORT .toPrimitive());
        assertEquals(DataType.INT,    DataType.INT   .toPrimitive());
        assertEquals(DataType.INT,    DataType.UINT  .toPrimitive());
        assertEquals(DataType.FLOAT,  DataType.FLOAT .toPrimitive());
        assertEquals(DataType.DOUBLE, DataType.DOUBLE.toPrimitive());
    }

    /**
     * Tests {@link DataType#toFloat()}.
     */
    @Test
    public void testToFloat() {
        assertEquals(DataType.FLOAT,  DataType.BYTE  .toFloat());
        assertEquals(DataType.FLOAT,  DataType.USHORT.toFloat());
        assertEquals(DataType.FLOAT,  DataType.SHORT .toFloat());
        assertEquals(DataType.DOUBLE, DataType.INT   .toFloat());
        assertEquals(DataType.DOUBLE, DataType.UINT  .toFloat());
        assertEquals(DataType.FLOAT,  DataType.FLOAT .toFloat());
        assertEquals(DataType.DOUBLE, DataType.DOUBLE.toFloat());
    }

    /**
     * Tests {@link DataType#forNumberOfBits(int, boolean, boolean)}.
     */
    @Test
    public void testForNumberOfBits() {
        assertEquals(DataType.BYTE,   DataType.forNumberOfBits(1,            false, false));
        assertEquals(DataType.BYTE,   DataType.forNumberOfBits(Byte.SIZE,    false, false));
        assertEquals(DataType.USHORT, DataType.forNumberOfBits(Short.SIZE,   false, false));
        assertEquals(DataType.SHORT,  DataType.forNumberOfBits(Short.SIZE,   false, true));
        assertEquals(DataType.INT,    DataType.forNumberOfBits(Integer.SIZE, false, true));
        assertEquals(DataType.UINT,   DataType.forNumberOfBits(Integer.SIZE, false, false));
        assertEquals(DataType.FLOAT,  DataType.forNumberOfBits(Float.SIZE,   true,  true));
        assertEquals(DataType.DOUBLE, DataType.forNumberOfBits(Double.SIZE,  true,  true));

        var e = assertThrows(RasterFormatException.class,
                () -> DataType.forNumberOfBits(Byte.SIZE, false, true),
                "Signed bytes should be invalid.");
        assertMessageContains(e, "signed", "true");
    }

    /**
     * Tests {@link DataType#forBands(SampleModel)}.
     */
    @Test
    public void testForBands() {
        SampleModel sm = new BandedSampleModel(DataBuffer.TYPE_INT, 1, 1, 3);
        assertEquals(DataType.INT, DataType.forBands(sm));

        sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, 1, 1, new int[] {0x7F0000, 0x00FF00, 0x00007F});
        assertEquals(DataType.BYTE, DataType.forBands(sm));

        sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, 1, 1, new int[] {0x7F0000, 0x00FF80, 0x00007F});
        assertEquals(DataType.USHORT, DataType.forBands(sm));
    }
}
