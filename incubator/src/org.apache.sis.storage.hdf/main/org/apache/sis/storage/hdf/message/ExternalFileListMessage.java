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
package org.apache.sis.storage.hdf.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Header Message Name: External Data Files
 * <p>
 * Header Message Type: 0x0007
 * <p>
 * Length: Varies
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description:
 * The external data storage message indicates that the data for an object is
 * stored outside the HDF5 file. The filename of the object is stored as a
 * Universal Resource Location (URL) of the actual filename containing the data.
 * An external file list record also contains the byte offset of the start of
 * the data within the file and the amount of space reserved in the file for
 * that data.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_external">IV.A.2.h. The Data Storage - External Data Files Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class ExternalFileListMessage extends Message {

    /**
     * The total number of slots allocated in the message. Its value must be at
     * least as large as the value contained in the Used Slots field.
     * (The current library simply uses the number of Used Slots for this message.)
     */
    private int allocatedSlots;
    /** The number of initial slots which contains valid information. */
    private int usedSlots;
    /**
     * This is the address of a local heap which contains the names for the
     * external files (The local heap information can be found in Disk Format
     * Level 1D in this document). The name at offset zero in the heap is always
     * the empty string.
     */
    private long heapAddress;

    /**
     * The slot definitions are stored in order according to the array addresses
     * they represent.
     */
    private List<ExternalFileListSlot> slotDefinitions;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public ExternalFileListMessage() {
    }

    /**
     * Returns the address of the local heap holding the external file names.
     *
     * @return the address of the local heap holding the external file names
     */
    public long getHeapAddress() {
        return heapAddress;
    }

    /**
     * Returns the segments of external files making up the raw data, in order.
     *
     * @return the segments of external files making up the raw data, in order
     */
    public List<ExternalFileListSlot> getSlots() {
        return slotDefinitions;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number information is used for changes in the format of
        External Data Storage Message and is described here:
        0 : Never used.
        1 : The current version used by the library.
         */
        channel.ensureVersion(1);
        channel.skipFully(3);
        allocatedSlots = channel.readUnsignedShort();
        usedSlots = channel.readUnsignedShort();
        heapAddress = channel.readOffset();
        slotDefinitions = new ArrayList<>(usedSlots);
        for (int i = 0; i < usedSlots; i++) {
            final ExternalFileListSlot slot = new ExternalFileListSlot();
            slot.read(channel);
            slotDefinitions.add(slot);
        }
    }

    /** A slot of the external file list : a segment of an external file holding raw data. */
    public static class ExternalFileListSlot {
        /**
         * The byte offset within the local name heap for the name of the file.
         * File names are stored as a URL which has a protocol name, a host name,
         * a port number, and a file name: protocol:port//host/file. If the
         * protocol is omitted then “file:” is assumed. If the port number is
         * omitted then a default port for that protocol is used. If both the
         * protocol and the port number are omitted then the colon can also be
         * omitted. If the double slash and host name are omitted then
         * “localhost” is assumed. The file name is the only mandatory part,
         * and if the leading slash is missing then it is relative to the
         * application’s current working directory (the use of relative names is
         * not recommended).
         */
        private long nameOffsetinLocalHeap;
        /**
         * This is the byte offset to the start of the data in the specified
         * file. For files that contain data for a single dataset this will
         * usually be zero.
         */
        private long offsetinExternalDataFile;
        /**
         * This is the total number of bytes reserved in the specified file for
         * raw data storage. For a file that contains exactly one complete
         * dataset which is not extendable, the size will usually be the exact
         * size of the dataset. However, by making the size larger one allows
         * HDF5 to extend the dataset. The size can be set to a value larger
         * than the entire file since HDF5 will read zeroes past the end of the
         * file without failing.
         */
        private long dataSizeinExternalFile;

        /** Creates an empty slot, filled by {@link #read(HDF5DataInput)}. */
        public ExternalFileListSlot() {
        }

        /**
         * Returns the byte offset of the file name in the local heap.
         *
         * @return the byte offset of the file name in the local heap
         */
        public long getNameOffset() {
            return nameOffsetinLocalHeap;
        }

        /**
         * Returns the byte offset of the data within the external file.
         *
         * @return the byte offset of the data within the external file
         */
        public long getOffsetInFile() {
            return offsetinExternalDataFile;
        }

        /**
         * Returns the number of bytes reserved in the external file.
         * It may exceed the file size, missing bytes reading as zeroes.
         *
         * @return the number of bytes reserved in the external file
         */
        public long getSizeInFile() {
            return dataSizeinExternalFile;
        }

        /**
         * Reads this slot.
         *
         * @param channel input positioned on the slot.
         * @throws IOException if an I/O error occurred.
         */
        public void read(HDF5DataInput channel) throws IOException {
            nameOffsetinLocalHeap = channel.readLength();
            offsetinExternalDataFile = channel.readLength();
            dataSizeinExternalFile = channel.readLength();
        }
    }
}
