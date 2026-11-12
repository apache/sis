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
 * A local heap is a collection of small pieces of data that are particular to
 * a single object in the HDF5 file. Objects can be inserted and removed from
 * the heap at any time. The address of a heap does not change once the heap is
 * created. For example, a group stores addresses of objects in symbol table
 * nodes with the names of links stored in the group’s local heap.
 * <p>
 * Objects within a local heap should be aligned on an 8-byte boundary.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_localheap">III.D. Disk Format: Level 1D - Local Heaps</a>
 * @author Johann Sorel (Geomatys)
 */
public final class LocalHeap extends IOStructure {

    /**
     * The ASCII character string “HEAP” is used to indicate the beginning of a
     * heap. This gives file consistency checking utilities a better chance of
     * reconstructing a damaged file.
     */
    public static final byte[] SIGNATURE = "HEAP".getBytes(StandardCharsets.US_ASCII);

    /**
     * The total amount of disk memory allocated for the heap data. This may be
     * larger than the amount of space required by the objects stored in the heap.
     * The extra unused space in the heap holds a linked list of free blocks.
     */
    public long dataSegmentSize;
    /**
     * This is the offset within the heap data segment of the first free block
     * (or the undefined address if there is no free block). The free block
     * contains Size of Lengths bytes that are the offset of the next free block
     * (or the value ‘1’ if this is the last free block) followed by Size of
     * Lengths bytes that store the size of this free block. The size of the
     * free block includes the space used to store the offset of the next free
     * block and the size of the current block, making the minimum size of a
     * free block 2 * Size of Lengths.
     */
    public long offsetToHeadofFreeList;
    /**
     * The data segment originally starts immediately after the heap header,
     * but if the data segment must grow as a result of adding more objects,
     * then the data segment may be relocated, in its entirety, to another
     * part of the file.
     */
    public long addressOfDataSegment;

    /** Creates an empty local heap, filled by {@link #read(HDF5DataInput)}. */
    public LocalHeap() {
    }

    /**
     * Returns the null terminated string at the given offset.
     * The channel position is restored on return.
     *
     * @param channel channel to read from
     * @param offset offset of the string in the heap data segment
     * @return the null terminated string at the given offset
     * @throws IOException if an I/O error occurred.
     */
    public String getString(HDF5DataInput channel, long offset) throws IOException {
        channel.mark();
        try {
            channel.seek(addressOfDataSegment + offset);
            return channel.readNullTerminatedString(0, StandardCharsets.US_ASCII);
        } finally {
            channel.reset();
        }
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        channel.ensureSignature(SIGNATURE);
        /*
        Each local heap has its own version number so that new heaps can be added
        to old files. This document describes version zero (0) of the local heap.
         */
        channel.ensureVersion(0);
        channel.skipFully(3);
        dataSegmentSize = channel.readLength();
        offsetToHeadofFreeList = channel.readLength();
        addressOfDataSegment = channel.readOffset();
    }
}
