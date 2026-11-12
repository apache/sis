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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * The shared object header message table is used to locate object header
 * messages that are shared between two or more object headers in the file.
 * Shared object header messages are stored and indexed in the file in one of
 * two ways: indexed sequentially in a shared header message list or indexed
 * with a v2 B-tree. The shared messages themselves are either stored in a
 * fractal heap (when two or more objects share the message), or remain in an
 * object’s header (when only one object uses the message currently, but the
 * message can be shared in the future).
 * <p>
 * The shared object header message table contains a list of shared message index
 * headers. Each index header records information about the version of the index
 * format, the index storage type, flags for the message types indexed, the
 * number of messages in the index, the address where the index resides, and the
 * fractal heap address if shared messages are stored there.
 * <p>
 * Each index can be either a list or a v2 B-tree and may transition between
 * those two forms as the number of messages in the index varies. Each shared
 * message record contains information used to locate the shared message from
 * either a fractal heap or an object header. The types of messages that can be
 * shared are: Dataspace, Datatype, Fill Value, Filter Pipeline and Attribute.
 * <p>
 * The shared object header message table is pointed to from a shared message
 * table message in the superblock extension for a file. This message stores the
 * version of the table format, along with the number of index headers in the
 * table.
 * <p>
 * Shared messages are indexed either with a shared message record list,
 * described below, or using a v2 B-tree (using record type 7). The number of
 * records in the shared message record list is determined in the index’s entry
 * in the shared object header message table.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_sohm">III.I. Disk Format: Level 1I - Shared Object Header Message Table</a>
 * @author Johann Sorel (Geomatys)
 */
public final class SharedObjectHeaderMessageTable extends IOStructure {

    /**
     * The ASCII character string “SMTB” is used to indicate the beginning of
     * the Shared Object Header Message table. This gives file consistency
     * checking utilities a better chance of reconstructing a damaged file.
     */
    public static final byte[] SIGNATURE = "SMTB".getBytes(StandardCharsets.US_ASCII);

    /** Shared message index headers of this table. */
    public List<Index> indexes;

    /** Creates an empty table, filled by {@link #read(HDF5DataInput)}. */
    public SharedObjectHeaderMessageTable() {
    }

    public void read(HDF5DataInput channel) throws IOException {

        channel.ensureSignature(SIGNATURE);
        //TODO : skipping entries, decoding bug, will have to investigate
        int nbEntries = 0;
        indexes = new ArrayList<>(nbEntries);
        for (int i = 0; i < nbEntries; i++) {
            Index index = new Index();
            /*
            This is the version number for the list of shared object header
            message indexes and this document describes version 0.
            */
            channel.ensureVersion(0);
            index.indexType = channel.readUnsignedByte();
            index.messageTypeFlags = channel.readUnsignedShort();
            index.minimumMessageSize = channel.readInt();
            index.listCutOff = channel.readUnsignedShort();
            index.v2BtreeCutoff = channel.readUnsignedShort();
            index.numberOfMessages = channel.readUnsignedShort();
            index.indexAddress = channel.readOffset();
            index.fractalHeapAddress = channel.readOffset();
            indexes.add(index);
        }

        int checksum = channel.readInt();
    }

    /** Shared message index header, locating the list or v2 B-tree indexing some types of shared messages. */
    public static class Index {

        /** Creates an empty index header, filled when the enclosing table is read. */
        public Index() {
        }

        /**
         * The type of index can be an unsorted list or a v2 B-tree.
         */
        private int indexType;
        /**
         * This field indicates the type of messages tracked in the index, as follows:
         * <ul>
         * <li>0 : If set, the index tracks Dataspace Messages. </li>
         * <li>1 : If set, the message tracks Datatype Messages. </li>
         * <li>2 : If set, the message tracks Fill Value Messages. </li>
         * <li>3 : If set, the message tracks Filter Pipeline Messages. </li>
         * <li>4 : If set, the message tracks Attribute Messages. </li>
         * <li>5-15 : Reserved (zero). </li>
         * </ul>
         * An index can track more than one type of message, but each type of message can only by in one index.
         */
        private int messageTypeFlags;
        /**
         * This is the message size sharing threshold for the index. If the
         * encoded size of the message is less than this value, the message is
         * not shared.
         */
        private int minimumMessageSize;
        /**
         * This is the cutoff value for the indexing of messages to switch from
         * a list to a v2 B-tree. If the number of messages is greater than this
         * value, the index should be a v2 B-tree.
         */
        private int listCutOff;
        /**
         * This is the cutoff value for the indexing of messages to switch from
         * a v2 B-tree back to a list. If the number of messages is less than
         * this value, the index should be a list.
         */
        private int v2BtreeCutoff;
        /**
         * The number of shared messages being tracked for the index.
         */
        private int numberOfMessages;
        /**
         * This field is the address of the list or v2 B-tree where the
         * index nodes reside.
         */
        private long indexAddress;
        /**
         * This field is the address of the fractal heap if shared messages
         * are stored there.
         */
        private long fractalHeapAddress;
    }
}
