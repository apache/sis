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

import java.io.IOException;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Free-space section record data for a fractal heap "indirect" section,
 * describing a range of empty blocks in an indirect block of a fractal heap.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_freespaceindex">III.H. Disk Format: Level 1H - Free-space Index</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FreeSpaceFractalHeapIndirect extends IOStructure {

    /**
     * The offset of the indirect block in the fractal heap’s address space
     * containing the empty blocks.
     *
     * The number of bytes used to encode this field is the minimum number of
     * bytes needed to encode values for the Maximum Heap Size (in the fractal
     * heap’s header).
     */
    private int fractalHeapIndirectBlockOffset;
    /**
     * This is the row that the empty blocks start in.
     */
    private int blockStartRow;
    /**
     * This is the column that the empty blocks start in.
     */
    private int blockStartColumn;
    /**
     * This is the number of empty blocks covered by the section.
     */
    private int numberOfBlocks;

    /** Creates an empty structure, filled by {@link #read(HDF5DataInput)}. */
    public FreeSpaceFractalHeapIndirect() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        /*
        block offset
        */
        blockStartRow = channel.readUnsignedShort();
        blockStartRow = channel.readUnsignedShort();
        numberOfBlocks = channel.readUnsignedShort();
        throw new IOException("TODO");
    }
}
