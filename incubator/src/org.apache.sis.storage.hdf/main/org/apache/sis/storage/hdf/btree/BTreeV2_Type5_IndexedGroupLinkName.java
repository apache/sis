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
 * Record of a version 2 B-tree of type 5, used for indexing the ‘name’ field of the
 * links in indexed groups.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_infra_btrees_v2">III.A.2. Disk Format: Level 1A2 - Version 2 B-trees</a>
 * @author Johann Sorel (Geomatys)
 */
public final class BTreeV2_Type5_IndexedGroupLinkName {

    /**
     * This field is hash value of the name for the link. The hash value is the
     * Jenkins’ lookup3 checksum algorithm applied to the link’s name.
     */
    private int hash;
    /**
     * This is a 7-byte sequence of bytes and is the heap ID for the link record
     * in the group’s fractal heap.
     */
    private byte[] id;

    /** Creates an empty record, filled by {@link #read(HDF5DataInput)}. */
    public BTreeV2_Type5_IndexedGroupLinkName() {
    }

    /**
     * Returns the Jenkins lookup3 hash of the link name.
     *
     * @return the Jenkins lookup3 hash of the link name
     */
    public int getHash() {
        return hash;
    }

    /**
     * Returns the 7 bytes heap ID of the Link message in the group fractal heap.
     *
     * @return the 7 bytes heap ID of the Link message in the group fractal heap
     */
    public byte[] getId() {
        return id;
    }

    /**
     * Reads the record.
     *
     * @param channel channel positioned on the record
     * @throws IOException if an I/O error occurred.
     */
    public void read(HDF5DataInput channel) throws IOException {

        hash = channel.readInt();
        id = channel.readNBytes(7);
    }
}
