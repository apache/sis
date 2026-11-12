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
 * Version 2 (v2) B-trees are “traditional” B-trees with one major difference.
 * Instead of just using a simple pointer (or address in the file) to a child
 * of an internal node, the pointer to the child node contains two additional
 * pieces of information: the number of records in the child node itself, and
 * the total number of records in the child node and all its descendants.
 * Storing this additional information allows fast array-like indexing to locate
 * the nth record in the B-tree.
 * <p>
 * The entry into a version 2 B-tree is a header which contains global information
 * about the structure of the B-tree. The root node address field in the header
 * points to the B-tree root node, which is either an internal or leaf node,
 * depending on the value in the header’s depth field. An internal node consists
 * of records plus pointers to further leaf or internal nodes in the tree.
 * A leaf node consists of solely of records. The format of the records depends
 * on the B-tree type (stored in the header).
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_infra_btrees_v2">III.A.2. Disk Format: Level 1A2 - Version 2 B-trees</a>
 * @author Johann Sorel (Geomatys)
 */
public final class BTreeV2 extends IOStructure {

    /**
     * The ASCII character string “BTHD” is used to indicate the header of a
     * version 2 (v2) B-tree node.
     */
    public static final byte[] SIGNATURE = "BTHD".getBytes(StandardCharsets.US_ASCII);

    /**
     * This field indicates the type of B-tree:
     * <ul>
     * <li>0 : This B-tree is used for testing only. This value should not be used for storing records in actual HDF5 files.</li>
     * <li>1 : This B-tree is used for indexing indirectly accessed, non-filtered ‘huge’ fractal heap objects.</li>
     * <li>2 : This B-tree is used for indexing indirectly accessed, filtered ‘huge’ fractal heap objects.</li>
     * <li>3 : This B-tree is used for indexing directly accessed, non-filtered ‘huge’ fractal heap objects.</li>
     * <li>4 : This B-tree is used for indexing directly accessed, filtered ‘huge’ fractal heap objects.</li>
     * <li>5 : This B-tree is used for indexing the ‘name’ field for links in indexed groups.</li>
     * <li>6 : This B-tree is used for indexing the ‘creation order’ field for links in indexed groups.</li>
     * <li>7 : This B-tree is used for indexing shared object header messages.</li>
     * <li>8 : This B-tree is used for indexing the ‘name’ field for indexed attributes.</li>
     * <li>9 : This B-tree is used for indexing the ‘creation order’ field for indexed attributes.</li>
     * <li>10 : This B-tree is used for indexing chunks of datasets with no filters and with more than one dimension of unlimited extent.</li>
     * <li>11 : This B-tree is used for indexing chunks of datasets with filters and more than one dimension of unlimited extent. </li>
     * </ul>
     */
    private int type;
    /**
     * This is the size in bytes of all B-tree nodes.
     */
    private int nodeSize;
    /**
     * This field is the size in bytes of the B-tree record.
     */
    private int recordSize;
    /**
     * This is the depth of the B-tree.
     */
    private int depth;
    /**
     * The percent full that a node needs to increase above before it is split.
     */
    private int splitPercent;
    /**
     * The percent full that a node needs to be decrease below before it is split.
     */
    private int mergePercent;
    /**
     * This is the address of the root B-tree node. A B-tree with no records
     * will have the undefined address in this field.
     */
    private long rootNodeAddress;
    /**
     * This is the number of records in the root node.
     */
    private int numberOfRecordsInRootNode;
    /**
     * This is the total number of records in the entire B-tree.
     */
    private long totalNumberOfRecords;

    /**
     * Number of bytes of the prefix and suffix common to every node : signature,
     * version, type and checksum.
     */
    private static final int NODE_OVERHEAD = 4 + 1 + 1 + 4;

    /**
     * Width in bytes of the "number of records in child node" field, used at every
     * level of the tree.
     */
    private int childRecordCountSize;
    /**
     * Width in bytes of the "total number of records in child node" field, per
     * depth. Index 0 is zero, which is why the nodes just above the leaves omit
     * that field.
     */
    private int[] childTotalRecordCountSize;
    /**
     * Size in bytes of an address, captured when the header is read.
     */
    private int offsetSize;

    /** Creates an empty B-tree header, filled by {@link #read(HDF5DataInput)}. */
    public BTreeV2() {
    }

    /**
     * Returns the B-tree type, from 0 to 11, which defines the record format.
     *
     * @return the B-tree type, from 0 to 11, which defines the record format
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the size in bytes of each record.
     *
     * @return the size in bytes of each record
     */
    public int getRecordSize() {
        return recordSize;
    }

    /**
     * Returns the total number of records in the entire B-tree.
     *
     * @return the total number of records in the entire B-tree
     */
    public long getTotalNumberOfRecords() {
        return totalNumberOfRecords;
    }

    public void read(HDF5DataInput channel) throws IOException {

        channel.ensureSignature(SIGNATURE);
        /*
        The version number for this B-tree header. This document describes version 0.
        */
        channel.ensureVersion(0);
        type = channel.readUnsignedByte();
        nodeSize = channel.readInt();
        recordSize = channel.readUnsignedShort();
        depth = channel.readUnsignedShort();
        splitPercent = channel.readUnsignedByte();
        mergePercent = channel.readUnsignedByte();
        rootNodeAddress = channel.readOffset();
        numberOfRecordsInRootNode = channel.readUnsignedShort();
        totalNumberOfRecords = channel.readLength();
        int checksum = channel.readInt(); //This is the checksum for the B-tree header.

        offsetSize = channel.getOffsetSize();
        derive();
    }

    /**
     * Compute the width of the variable size fields of the internal nodes.
     * <p>
     * None of these widths is stored in the file, they follow from the node size,
     * the record size and the depth, as {@code H5B2__hdr_init} computes them.
     */
    private void derive() {
        final long maximumLeafRecords = (nodeSize - NODE_OVERHEAD) / recordSize;
        /*
        This width comes from the capacity of a leaf and is then used at every
        level, even though the specification reads as if it were per level.
        */
        childRecordCountSize = HDF5DataInput.limitEncodedSize(maximumLeafRecords);

        childTotalRecordCountSize = new int[depth + 1];
        childTotalRecordCountSize[0] = 0;
        long cumulated = maximumLeafRecords;
        for (int d = 1; d <= depth; d++) {
            final int pointerSize = offsetSize + childRecordCountSize + childTotalRecordCountSize[d - 1];
            final long maximumRecords = (nodeSize - (NODE_OVERHEAD + pointerSize))
                    / (recordSize + pointerSize);
            try {
                cumulated = Math.addExact(Math.multiplyExact(maximumRecords + 1, cumulated), maximumRecords);
            } catch (ArithmeticException ex) {
                //saturate rather than overflow on a corrupt header
                cumulated = Long.MAX_VALUE;
            }
            childTotalRecordCountSize[d] = HDF5DataInput.limitEncodedSize(cumulated);
        }
    }

    /**
     * Read every record of this tree, in key order.
     *
     * @param channel channel on the file, its position is restored on return
     * @return the raw bytes of each record, each {@link #getRecordSize()} long
     * @throws IOException if an I/O error occurred.
     */
    public List<byte[]> readRecords(HDF5DataInput channel) throws IOException {
        final List<byte[]> records = new ArrayList<>();
        if (channel.isUndefinedOffset(rootNodeAddress)) {
            return records;
        }
        channel.mark();
        try {
            readNode(channel, rootNodeAddress, depth, numberOfRecordsInRootNode, records);
        } finally {
            channel.reset();
        }
        return records;
    }

    private void readNode(HDF5DataInput channel, long address, int nodeDepth, int recordCount,
            List<byte[]> records) throws IOException {
        channel.seek(address);
        if (nodeDepth == 0) {
            final BTreeV2LeafNode node = new BTreeV2LeafNode();
            node.read(channel, this, recordCount);
            records.addAll(node.getRecords());
        } else {
            final BTreeV2InternalNode node = new BTreeV2InternalNode();
            node.read(channel, this, nodeDepth, recordCount);
            final List<byte[]> nodeRecords = node.getRecords();
            final long[] childAddresses = node.getChildAddresses();
            final int[] childRecordCounts = node.getChildRecordCounts();
            /*
            In order traversal : the record between two children separates them,
            so children and records alternate.
            */
            for (int i = 0; i < childAddresses.length; i++) {
                readNode(channel, childAddresses[i], nodeDepth - 1, childRecordCounts[i], records);
                if (i < nodeRecords.size()) {
                    records.add(nodeRecords.get(i));
                }
            }
        }
    }

    /**
     * Returns the width in bytes of the "number of records in child node" field.
     *
     * @return the width in bytes of the "number of records in child node" field
     */
    int getChildRecordCountSize() {
        return childRecordCountSize;
    }

    /**
     * Returns the width in bytes of the "total number of records" field of the pointers held by a node at the
     * given depth, zero when the field is absent.
     *
     * @param nodeDepth depth of the internal node holding the pointers, at least 1
     * @return the width in bytes of the "total number of records" field of the pointers held by a node at the
     *         given depth, zero when the field is absent
     */
    int getChildTotalRecordCountSize(int nodeDepth) {
        return childTotalRecordCountSize[nodeDepth - 1];
    }
}
