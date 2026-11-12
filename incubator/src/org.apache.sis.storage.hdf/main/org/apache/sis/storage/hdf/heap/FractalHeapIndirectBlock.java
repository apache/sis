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
 * Indirect block of a fractal heap. It holds no object data, only the addresses
 * of the direct and indirect blocks one level below.
 * <p>
 * The number of entries is derived from the number of rows this block spans and
 * from the heap header, so this block can not be decoded without both.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_fractalheap">III.G. Disk Format: Level 1G - Fractal Heap</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FractalHeapIndirectBlock extends IOStructure {

    /**
     * The ASCII character string “FHIB” is used to indicate the beginning of a
     * fractal heap indirect block.
     */
    public static final byte[] SIGNATURE = "FHIB".getBytes(StandardCharsets.US_ASCII);

    private long heapHeaderAddress;
    /**
     * Offset of the first byte of the range this block indexes, in the heap
     * linear address space.
     */
    private long blockOffset;
    /**
     * Addresses of the direct blocks, K entries.
     */
    private long[] childDirectBlockAddresses;
    /**
     * Addresses of the indirect blocks one level below, N entries.
     */
    private long[] childIndirectBlockAddresses;

    /** Creates an empty indirect block, filled by {@link #read(HDF5DataInput, FractalHeap, int)}. */
    public FractalHeapIndirectBlock() {
    }

    /**
     * Returns the offset of the first byte of the range this block indexes, in the heap linear address space.
     *
     * @return the offset of the first byte of the range this block indexes, in the heap linear address space
     */
    public long getBlockOffset() {
        return blockOffset;
    }

    /**
     * Returns the addresses of the K child direct blocks, row by row.
     * Unallocated blocks have the undefined address.
     *
     * @return the addresses of the K child direct blocks, row by row
     */
    public long[] getChildDirectBlockAddresses() {
        return childDirectBlockAddresses;
    }

    /**
     * Returns the addresses of the N child indirect blocks, row by row, following the direct block entries.
     *
     * @return the addresses of the N child indirect blocks, row by row, following the direct block entries
     */
    public long[] getChildIndirectBlockAddresses() {
        return childIndirectBlockAddresses;
    }

    /**
     * Returns the number K of direct block entries, which is also the index of the first indirect block
     * entry.
     *
     * @return the number K of direct block entries, which is also the index of the first indirect block entry
     */
    public int getDirectBlockCount() {
        return childDirectBlockAddresses.length;
    }

    /**
     * An indirect block can not be decoded without its heap header and row count.
     *
     * @throws UnsupportedOperationException always
     * @see #read(HDF5DataInput, FractalHeap, int)
     */
    @Override
    public void read(HDF5DataInput channel) throws IOException {
        throw new UnsupportedOperationException("A fractal heap indirect block requires"
                + " its heap header and row count to be decoded, use read(channel, heap, nrows).");
    }

    /**
     * Reads the block prefix and the addresses of the child direct and indirect blocks.
     *
     * @param channel channel positioned on the block signature
     * @param heap owning heap header, not null
     * @param nrows number of rows this block spans
     * @throws IOException if an I/O error occurred or the signature or version is invalid.
     */
    public void read(HDF5DataInput channel, FractalHeap heap, int nrows) throws IOException {
        channel.ensureSignature(SIGNATURE);
        channel.ensureVersion(0);
        heapHeaderAddress = channel.readOffset();
        blockOffset = channel.readUnsignedInteger(heap.getHeapOffsetSize());

        /*
        Rows up to the maximum direct block row point at direct blocks, the rows
        beyond that point at further indirect blocks.
        */
        final int width = heap.getTableWidth();
        final int k = Math.min(nrows, heap.getMaximumDirectRows()) * width;
        final int n = nrows > heap.getMaximumDirectRows()
                ? (nrows - heap.getMaximumDirectRows()) * width : 0;

        childDirectBlockAddresses = new long[k];
        for (int i = 0; i < k; i++) {
            childDirectBlockAddresses[i] = channel.readOffset();
            if (heap.isFiltered()) {
                //filtered size and filter mask of the direct block
                channel.readLength();
                channel.readInt();
            }
        }
        childIndirectBlockAddresses = new long[n];
        for (int i = 0; i < n; i++) {
            childIndirectBlockAddresses[i] = channel.readOffset();
        }
        final int checksum = channel.readInt();
    }
}
