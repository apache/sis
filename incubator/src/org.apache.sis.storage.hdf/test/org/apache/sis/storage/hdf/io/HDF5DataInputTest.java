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
package org.apache.sis.storage.hdf.io;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 * Tests of the variable width integer reader and of the encoded size rule, both
 * used throughout the version 2 structures of the HDF5 format.
 *
 * @author Johann Sorel (Geomatys)
 */
public class HDF5DataInputTest {

    /**
     * Widths of 3, 5, 6 and 7 bytes are routine in fractal heaps and in the
     * fixed and extensible array indexes; the previous implementation supported
     * only 1, 2 and 4.
     */
    @Test
    public void testReadUnsignedIntegerAllWidths() throws IOException {
        final byte[] datas = new byte[]{
            (byte) 0x01,
            (byte) 0x02, (byte) 0x03,
            (byte) 0x04, (byte) 0x05, (byte) 0x06,
            (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0A,
            (byte) 0x0B, (byte) 0x0C, (byte) 0x0D, (byte) 0x0E, (byte) 0x0F};
        try (HDF5DataInput channel = new HDF5ChannelDataInput(datas, null)) {
            assertEquals(0x01L, channel.readUnsignedInteger(1));
            assertEquals(0x0302L, channel.readUnsignedInteger(2));
            assertEquals(0x060504L, channel.readUnsignedInteger(3));
            assertEquals(0x0A090807L, channel.readUnsignedInteger(4));
            assertEquals(0x0F0E0D0C0BL, channel.readUnsignedInteger(5));
        }
    }

    /**
     * A four bytes value with the high bit set must stay positive; the previous
     * implementation returned a signed int for that width.
     */
    @Test
    public void testReadUnsignedIntegerIsUnsigned() throws IOException {
        final byte[] datas = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        try (HDF5DataInput channel = new HDF5ChannelDataInput(datas, null)) {
            assertEquals(0xFFFFFFFFL, channel.readUnsignedInteger(4));
        }
    }

    @Test
    public void testReadUnsignedIntegerRejectsBadWidth() throws IOException {
        try (HDF5DataInput channel = new HDF5ChannelDataInput(new byte[16], null)) {
            for (int nbBytes : new int[]{0, -1, 9}) {
                try {
                    channel.readUnsignedInteger(nbBytes);
                    fail("Expected an IOException for width " + nbBytes);
                } catch (IOException ex) {
                    //expected
                }
            }
        }
    }

    /**
     * The HDF5 library encodes a count on {@code floor(log2(n))/8 + 1} bytes.
     * The values at exact powers of two are what distinguishes this rule from
     * the more obvious {@code ceil((floor(log2(n))+1)/8)}, which is one byte
     * short there.
     */
    @Test
    public void testLimitEncodedSize() {
        assertEquals(1, HDF5DataInput.limitEncodedSize(0));
        assertEquals(1, HDF5DataInput.limitEncodedSize(1));
        assertEquals(1, HDF5DataInput.limitEncodedSize(123));
        assertEquals(1, HDF5DataInput.limitEncodedSize(255));
        assertEquals(2, HDF5DataInput.limitEncodedSize(256));
        assertEquals(2, HDF5DataInput.limitEncodedSize(20000));
        assertEquals(2, HDF5DataInput.limitEncodedSize(65535));
        assertEquals(3, HDF5DataInput.limitEncodedSize(65536));
        assertEquals(3, HDF5DataInput.limitEncodedSize(16777215));
        assertEquals(4, HDF5DataInput.limitEncodedSize(16777216));
    }

    @Test
    public void testLog2Floor() {
        assertEquals(0, HDF5DataInput.log2Floor(0));
        assertEquals(0, HDF5DataInput.log2Floor(1));
        assertEquals(1, HDF5DataInput.log2Floor(2));
        assertEquals(1, HDF5DataInput.log2Floor(3));
        assertEquals(2, HDF5DataInput.log2Floor(4));
        assertEquals(9, HDF5DataInput.log2Floor(512));
        assertEquals(16, HDF5DataInput.log2Floor(131072 - 1));
        assertEquals(17, HDF5DataInput.log2Floor(131072));
    }

    /**
     * A sub channel inherits the offset and length sizes of the channel it was
     * created from, so that messages extracted from a heap decode with the same
     * address widths as the rest of the file.
     */
    @Test
    public void testSubChannelInheritsSizes() throws IOException {
        try (HDF5DataInput parent = new HDF5ChannelDataInput(new byte[16], null)) {
            parent.setOffsetSize(4);
            parent.setLengthSize(2);
            try (HDF5DataInput sub = parent.subChannel(new byte[]{
                    (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x05, (byte) 0x00})) {
                assertEquals(4, sub.getOffsetSize());
                assertEquals(2, sub.getLengthSize());
                assertEquals(1L, sub.readOffset());
                assertEquals(5L, sub.readLength());
            }
        }
    }
}
