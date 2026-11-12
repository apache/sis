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
package org.apache.sis.storage.hdf.btree;

import java.io.IOException;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Record of a version 2 B-tree of type 4, used for indexing directly accessed,
 * filtered ‘huge’ objects of a fractal heap.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_infra_btrees_v2">III.A.2. Disk Format: Level 1A2 - Version 2 B-trees</a>
 * @author Johann Sorel (Geomatys)
 */
public final class BTreeV2_Type4_DirectFilteredHugeObject {

    /**
     * The address of the filtered huge object in the file.
     */
    private long filteredHugeObjectAddress;
    /**
     * The length of the filtered huge object in the file.
     */
    private long filteredHugeObjectLength;
    /**
     * A 32-bit bit field indicating which filters have been skipped for this chunk.
     * Each filter has an index number in the pipeline (starting at 0, with the
     * first filter to apply) and if that filter is skipped, the bit corresponding
     * to its index is set.
     */
    private int filterMask;
    /**
     * The size of the de-filtered huge object in memory.
     */
    private long filteredHugeObjectMemorySize;

    /** Creates an empty record, filled by {@link #read(HDF5DataInput)}. */
    public BTreeV2_Type4_DirectFilteredHugeObject() {
    }

    /**
     * Reads the record.
     *
     * @param channel channel positioned on the record
     * @throws IOException if an I/O error occurred.
     */
    public void read(HDF5DataInput channel) throws IOException {

        filteredHugeObjectAddress = channel.readOffset();
        filteredHugeObjectLength = channel.readLength();
        filterMask = channel.readInt();
        filteredHugeObjectMemorySize = channel.readLength();
    }
}
