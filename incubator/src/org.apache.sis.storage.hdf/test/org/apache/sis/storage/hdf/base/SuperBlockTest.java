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
package org.apache.sis.storage.hdf.base;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Tests of the detection of the HDF5 format signature.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SuperBlockTest {

    /**
     * Bytes of a file whose superblock starts at the given offset.
     */
    private static ByteBuffer file(int signatureOffset, int totalLength) {
        final byte[] datas = new byte[totalLength];
        System.arraycopy(SuperBlock.SIGNATURE, 0, datas, signatureOffset, SuperBlock.SIGNATURE.length);
        return ByteBuffer.wrap(datas);
    }

    @Test
    public void testSignatureAtOrigin() {
        assertEquals(0, SuperBlock.findSignatureOffset(file(0, 4096)));
    }

    /**
     * A user block pushes the superblock to an offset that is a power of two.
     */
    @Test
    public void testSignatureAfterUserBlock() {
        assertEquals(512, SuperBlock.findSignatureOffset(file(512, 4096)));
        assertEquals(1024, SuperBlock.findSignatureOffset(file(1024, 4096)));
        assertEquals(2048, SuperBlock.findSignatureOffset(file(2048, 8192)));
        assertEquals(8192, SuperBlock.findSignatureOffset(file(8192, 16384)));
    }

    /**
     * The library only accepts user blocks whose size is a power of two, so a
     * signature anywhere else is not a superblock.
     */
    @Test
    public void testSignatureAtInvalidOffset() {
        assertEquals(-1, SuperBlock.findSignatureOffset(file(768, 4096)));
        assertEquals(-1, SuperBlock.findSignatureOffset(file(600, 4096)));
    }

    @Test
    public void testNotHDF5() {
        //classic netCDF
        assertEquals(-1, SuperBlock.findSignatureOffset(
                ByteBuffer.wrap("CDF\u0001and some content".getBytes(StandardCharsets.US_ASCII))));
        assertEquals(-1, SuperBlock.findSignatureOffset(ByteBuffer.wrap(new byte[4096])));
    }

    /**
     * Too few bytes to hold a signature at all.
     */
    @Test
    public void testTooShort() {
        assertEquals(-1, SuperBlock.findSignatureOffset(ByteBuffer.wrap(new byte[0])));
        assertEquals(-1, SuperBlock.findSignatureOffset(ByteBuffer.wrap(
                new byte[]{(byte) 137, 72, 68, 70})));
    }

    /**
     * A user block larger than the bytes available can not be detected, and must
     * not be reported as a match either.
     */
    @Test
    public void testSignatureBeyondAvailableBytes() {
        final ByteBuffer buffer = file(8192, 8192 + SuperBlock.SIGNATURE.length);
        assertEquals(8192, SuperBlock.findSignatureOffset(buffer));
        //the same file seen through fewer bytes
        assertEquals(-1, SuperBlock.findSignatureOffset(buffer.duplicate().limit(4096)));
    }

    /**
     * The search starts at the buffer position and leaves it untouched, so that
     * probing does not consume the bytes another provider may want to look at.
     */
    @Test
    public void testBufferIsNotConsumed() {
        final ByteBuffer buffer = file(0, 4096);
        assertEquals(0, SuperBlock.findSignatureOffset(buffer));
        assertEquals(0, buffer.position());
        assertEquals(4096, buffer.remaining());

        //a buffer already positioned looks for the signature from there
        final byte[] datas = new byte[4096 + 16];
        System.arraycopy(SuperBlock.SIGNATURE, 0, datas, 16, SuperBlock.SIGNATURE.length);
        final ByteBuffer shifted = ByteBuffer.wrap(datas);
        shifted.position(16);
        assertEquals(0, SuperBlock.findSignatureOffset(shifted));
        assertEquals(16, shifted.position());
    }
}
