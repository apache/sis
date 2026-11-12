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
import org.apache.sis.storage.hdf.base.IOStructure;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * The layout for the global heap block used with virtual datasets is described
 * below. For more information on global heaps,
 * see {@link GlobalHeap} (“Disk Format: Level 1E - Global Heap”).
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_globalheapvds">III.F. Disk Format: Level 1F - Global Heap Block for Virtual Datasets</a>
 * @author Johann Sorel (Geomatys)
 */
public final class GlobalHeapBlockForVirtualDatasets extends IOStructure {

    /**
     * The number of entries in the block.
     */
    private long numEntries;

    /** Creates an empty block, filled by {@link #read(HDF5DataInput)}. */
    public GlobalHeapBlockForVirtualDatasets() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        /*
        The version number for the block; the value is 0.
        */
        channel.ensureVersion(0);
        numEntries = channel.readLength();

        /*
        Source Filename #n
        The source file name where the source dataset is located.

        Source Dataset #n
        The source dataset name that is mapped to the virtual dataset.

        Source Selection #n
        The dataspace selection in the source dataset that is mapped to the virtual selection.

        Virtual Selection #n
        This is the dataspace selection in the virtual dataset that is mapped to the source selection.

        Checksum
        This is the checksum for the block.
        */

        throw new IOException("TODO");
    }
}
