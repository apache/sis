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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.hdf.base.IOStructure;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Leaf node of a version 2 B-tree, holding records only.
 * <p>
 * Neither the number of records nor their size is stored in the node, both come
 * from the header and from the pointer that led here, so this node can not be
 * decoded on its own.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_infra_btrees_v2">III.A.2. Disk Format: Level 1A2 - Version 2 B-trees</a>
 * @author Johann Sorel (Geomatys)
 */
public final class BTreeV2LeafNode extends IOStructure {

    /**
     * The ASCII character string “BTLF“ is used to indicate the leaf node of a version 2 (v2) B-tree.
     */
    public static final byte[] SIGNATURE = "BTLF".getBytes(StandardCharsets.US_ASCII);

    /**
     * This field is the type of the B-tree node. It should always be the
     * same as the B-tree type in the header.
     */
    private int type;
    /**
     * Raw bytes of each record, the format of which depends on the B-tree type.
     */
    private List<byte[]> records;

    /** Creates an empty node, filled by {@link #read(HDF5DataInput, BTreeV2, int)}. */
    public BTreeV2LeafNode() {
    }

    /**
     * Returns the node type, same as the B-tree type in the header.
     *
     * @return the node type, same as the B-tree type in the header
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the raw bytes of each record, in key order.
     *
     * @return the raw bytes of each record, in key order
     */
    public List<byte[]> getRecords() {
        return records;
    }

    /**
     * A leaf node can not be decoded without its header and record count.
     *
     * @throws UnsupportedOperationException always
     * @see #read(HDF5DataInput, BTreeV2, int)
     */
    @Override
    public void read(HDF5DataInput channel) throws IOException {
        throw new UnsupportedOperationException("A version 2 B-tree leaf node requires its"
                + " header and record count to be decoded, use read(channel, header, recordCount).");
    }

    /**
     * Reads the records.
     *
     * @param channel channel positioned on the node signature
     * @param header owning B-tree header, not null
     * @param recordCount number of records in this node, from the pointer that led here
     * @throws IOException if an I/O error occurred or the node type does not match the header.
     */
    public void read(HDF5DataInput channel, BTreeV2 header, int recordCount) throws IOException {
        channel.ensureSignature(SIGNATURE);
        /*
        The version number for this B-tree leaf node.
        This document describes version 0.
         */
        channel.ensureVersion(0);
        type = channel.readUnsignedByte();
        if (type != header.getType()) {
            throw new IOException("Version 2 B-tree leaf node type " + type
                    + " does not match header type " + header.getType());
        }

        final int recordSize = header.getRecordSize();
        records = new ArrayList<>(recordCount);
        for (int i = 0; i < recordCount; i++) {
            records.add(channel.readNBytes(recordSize));
        }

        final int checksum = channel.readInt();
    }
}
