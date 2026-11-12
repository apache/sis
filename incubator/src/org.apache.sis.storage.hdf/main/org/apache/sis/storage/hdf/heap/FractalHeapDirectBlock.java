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
package org.apache.sis.storage.hdf.heap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.sis.storage.hdf.base.IOStructure;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Direct block of a fractal heap, holding the actual object data.
 * <p>
 * The width of the block offset field is derived from the heap header, so this
 * block can not be decoded without it.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_fractalheap">III.G. Disk Format: Level 1G - Fractal Heap</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FractalHeapDirectBlock extends IOStructure {

    /**
     * The ASCII character string “FHDB” is used to indicate the beginning of a
     * fractal heap direct block.
     */
    public static final byte[] SIGNATURE = "FHDB".getBytes(StandardCharsets.US_ASCII);

    /**
     * Address of the fractal heap header this block belongs to.
     */
    private long heapHeaderAddress;
    /**
     * Offset of the first byte of this block in the heap linear address space.
     */
    private long blockOffset;
    /**
     * Address of this block in the file.
     */
    private long address;

    /** Creates an empty direct block, filled by {@link #read(HDF5DataInput, FractalHeap)}. */
    public FractalHeapDirectBlock() {
    }

    /**
     * Returns the offset of the first byte of this block in the heap linear address space.
     *
     * @return the offset of the first byte of this block in the heap linear address space
     */
    public long getBlockOffset() {
        return blockOffset;
    }

    /**
     * Returns the address of this block in the file, that is the position of its signature.
     *
     * @return the address of this block in the file, that is the position of its signature
     */
    public long getAddress() {
        return address;
    }

    /**
     * A direct block can not be decoded without its heap header.
     *
     * @throws UnsupportedOperationException always
     * @see #read(HDF5DataInput, FractalHeap)
     */
    @Override
    public void read(HDF5DataInput channel) throws IOException {
        throw new UnsupportedOperationException("A fractal heap direct block requires"
                + " its heap header to be decoded, use read(channel, heap).");
    }

    /**
     * Read the block prefix. The object data follows and is left in place; heap
     * offsets are relative to the beginning of the block, prefix included, so the
     * caller seeks directly rather than copying the data.
     *
     * @param channel channel positioned on the block signature
     * @param heap owning heap header, not null
     * @throws IOException if an I/O error occurred or the signature or version is invalid.
     */
    public void read(HDF5DataInput channel, FractalHeap heap) throws IOException {
        address = channel.getStreamPosition();
        channel.ensureSignature(SIGNATURE);
        channel.ensureVersion(0);
        heapHeaderAddress = channel.readOffset();
        blockOffset = channel.readUnsignedInteger(heap.getHeapOffsetSize());
        if (heap.isDirectBlockChecksummed()) {
            final int checksum = channel.readInt();
        }
    }
}
