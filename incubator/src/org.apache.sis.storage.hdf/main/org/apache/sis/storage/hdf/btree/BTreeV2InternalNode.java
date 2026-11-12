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
 * Internal node of a version 2 B-tree : all the records first, then one child
 * pointer more than there are records.
 * <p>
 * A child pointer carries the number of records of the child and, below the
 * nodes sitting just above the leaves, the total number of records of the whole
 * subtree. The width of both counts is computed by the header rather than stored.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_infra_btrees_v2">III.A.2. Disk Format: Level 1A2 - Version 2 B-trees</a>
 * @author Johann Sorel (Geomatys)
 */
public final class BTreeV2InternalNode extends IOStructure {

    /**
     * The ASCII character string “BTIN” is used to indicate the internal node of
     * a version 2 (v2) B-tree.
     */
    public static final byte[] SIGNATURE = "BTIN".getBytes(StandardCharsets.US_ASCII);

    /**
     * This field is the type of the B-tree node. It should always be the same as
     * the B-tree type in the header.
     */
    private int type;
    /**
     * Raw bytes of each record.
     */
    private List<byte[]> records;
    /**
     * Addresses of the children, one more than there are records.
     */
    private long[] childAddresses;
    /**
     * Number of records held by each child.
     */
    private int[] childRecordCounts;

    /** Creates an empty node, filled by {@link #read(HDF5DataInput, BTreeV2, int, int)}. */
    public BTreeV2InternalNode() {
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
     * Returns the child node addresses, one more than there are records.
     *
     * @return the child node addresses, one more than there are records
     */
    public long[] getChildAddresses() {
        return childAddresses;
    }

    /**
     * Returns the number of records in each child node, excluding its descendants.
     *
     * @return the number of records in each child node, excluding its descendants
     */
    public int[] getChildRecordCounts() {
        return childRecordCounts;
    }

    /**
     * An internal node can not be decoded without its header, depth and record count.
     *
     * @throws UnsupportedOperationException always
     * @see #read(HDF5DataInput, BTreeV2, int, int)
     */
    @Override
    public void read(HDF5DataInput channel) throws IOException {
        throw new UnsupportedOperationException("A version 2 B-tree internal node requires its"
                + " header, depth and record count to be decoded,"
                + " use read(channel, header, depth, recordCount).");
    }

    /**
     * Reads the records and the child pointers.
     *
     * @param channel channel positioned on the node signature
     * @param header owning B-tree header, not null
     * @param depth depth of this node, the leaves being at depth 0
     * @param recordCount number of records in this node
     * @throws IOException if an I/O error occurred or the node type does not match the header.
     */
    public void read(HDF5DataInput channel, BTreeV2 header, int depth, int recordCount) throws IOException {
        channel.ensureSignature(SIGNATURE);
        /*
        The version number for this B-tree internal node.
        This document describes version 0.
        */
        channel.ensureVersion(0);
        type = channel.readUnsignedByte();
        if (type != header.getType()) {
            throw new IOException("Version 2 B-tree internal node type " + type
                    + " does not match header type " + header.getType());
        }

        final int recordSize = header.getRecordSize();
        records = new ArrayList<>(recordCount);
        for (int i = 0; i < recordCount; i++) {
            records.add(channel.readNBytes(recordSize));
        }

        /*
        The total record count is omitted just above the leaves, where its computed
        width is zero.
        */
        final int countSize = header.getChildRecordCountSize();
        final int totalCountSize = header.getChildTotalRecordCountSize(depth);
        childAddresses = new long[recordCount + 1];
        childRecordCounts = new int[recordCount + 1];
        for (int i = 0; i < childAddresses.length; i++) {
            childAddresses[i] = channel.readOffset();
            childRecordCounts[i] = Math.toIntExact(channel.readUnsignedInteger(countSize));
            if (totalCountSize > 0) {
                channel.readUnsignedInteger(totalCountSize);
            }
        }

        final int checksum = channel.readInt();
    }
}
