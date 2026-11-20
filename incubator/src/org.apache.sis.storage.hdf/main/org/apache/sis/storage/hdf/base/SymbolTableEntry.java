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
import java.util.Optional;
import org.apache.sis.storage.hdf.heap.LocalHeap;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Each symbol table entry in a symbol table node is designed to allow for very
 * fast browsing of stored objects. Toward that design goal, the symbol table
 * entries include space for caching certain constant metadata from the object header.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_symboltableentry">III.C. Disk Format: Level 1C - Symbol Table Entry</a>
 * @author Johann Sorel (Geomatys)
 */
public final class SymbolTableEntry extends IOStructure {

    /**
     * This is the byte offset into the group’s local heap for the name of the link.
     * The name is null terminated.
     */
    private long linkNameOffset;
    /**
     * Every object has an object header which serves as a permanent location
     * for the object’s metadata. In addition to appearing in the object header,
     * some of the object’s metadata can be cached in the scratch-pad space.
     */
    private long objectHeaderAddress;
    /**
     * The cache type is determined from the object header.
     * It also determines the format for the scratch-pad space:
     * <ul>
     * <li>0 : No data is cached by the group entry. This is guaranteed to be
     * the case when an object header has a link count greater than one.</li>
     * <li>1 : Group object header metadata is cached in the scratch-pad space.
     * This implies that the symbol table entry refers to another group.</li>
     * <li>2 : The entry is a symbolic link. The first four bytes of the
     * scratch-pad space are the offset into the local heap for the link value.
     * The object header address will be undefined.</li>
     * </ul>
     */
    private int cacheType;
    /**
     * If the Cache Type field contains the value one (1)
     * <p>
     * This is the file address for the root of the group’s B-tree.
     */
    private long addressOfBtree;
    /**
     * If the Cache Type field contains the value one (1)
     * <p>
     * This is the file address for the group’s local heap, in which are stored
     * the group’s symbol names.
     */
    private long addressOfNameHeap;
    /**
     * If the Cache Type field contains the value two (2)
     * <p>
     * The value of a symbolic link (that is, the name of the thing to which
     * it points) is stored in the local heap. This field is the 4-byte offset
     * into the local heap for the start of the link value, which is null terminated.
     */
    public int offsetToLinkValue;

    //cache values
    private String name;

    /** Creates an empty entry, filled by {@link #read(HDF5DataInput)}. */
    public SymbolTableEntry() {
    }

    /**
     * The cache type is determined from the object header.
     * It also determines the format for the scratch-pad space:
     * <ul>
     * <li>0 : No data is cached by the group entry. This is guaranteed to be
     * the case when an object header has a link count greater than one.</li>
     * <li>1 : Group object header metadata is cached in the scratch-pad space.
     * This implies that the symbol table entry refers to another group.</li>
     * <li>2 : The entry is a symbolic link. The first four bytes of the
     * scratch-pad space are the offset into the local heap for the link value.
     * The object header address will be undefined.</li>
     * </ul>
     *
     * @return the cache type, 0, 1 or 2.
     */
    public int getCacheType() {
        return cacheType;
    }

    /**
     * Every object has an object header which serves as a permanent location
     * for the object’s metadata. In addition to appearing in the object header,
     * some of the object’s metadata can be cached in the scratch-pad space.
     *
     * @return address of the object header of the object this entry refers to.
     */
    public long getObjectHeaderAddress() {
        return objectHeaderAddress;
    }

    /**
     * Returns the link name, or empty if the name offset is undefined or no heap is given.
     * The name is read from the heap on first call and cached.
     *
     * @param channel channel on the file.
     * @param heap local heap of the parent group.
     * @return the link name, or empty if the name offset is undefined or no heap is given
     * @throws IOException if an I/O error occurred.
     */
    public synchronized Optional<String> getName(HDF5DataInput channel, LocalHeap heap) throws IOException {
        if (name == null && channel.isDefinedOffset(linkNameOffset)) {
            if (heap != null) {
                name = heap.getString(channel, linkNameOffset);
            }
        }
        return Optional.ofNullable(name);
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        linkNameOffset = channel.readOffset();
        objectHeaderAddress = channel.readOffset();
        cacheType = channel.readInt();
        channel.skipFully(4);

        /**
         * scratch pad :
         * This space is used for different purposes, depending on the value of the
         * Cache Type field. Any metadata about an object represented in the
         * scratch-pad space is duplicated in the object header for that object.
         * <p>
         * Furthermore, no data is cached in the group entry scratch-pad space if
         * the object header for the object has a link count greater than one.
         */
        if (cacheType == 0) {
            //contains nothing
            channel.skipFully(16);
        } else if (cacheType == 1) {
            addressOfBtree = channel.readOffset();
            addressOfNameHeap = channel.readOffset();
            channel.skipFully(16 - channel.getOffsetSize()*2);
        } else if (cacheType == 2) {
            offsetToLinkValue = channel.readInt();
            channel.skipFully(12);
        }
    }
}
