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
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Header Message Name: Link Info
 * <p>
 * Header Message Type: 0x0002
 * <p>
 * Length: Varies
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description: The link info message tracks variable information about
 * the current state of the links for a “new style” group’s behavior. Variable
 * information will be stored in this message and constant information will be
 * stored in the Group Info message.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_linkinfo">IV.A.2.c. The Link Info Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class LinkInfo extends Message {

    /**
     * This field determines various optional aspects of the link info message:
     * <ul>
     *   <li>Bit 0 : If set, creation order for the links is tracked.</li>
     *   <li>Bit 1 : If set, creation order for the links is indexed.</li>
     *   <li>Bits 2-7 : Reserved</li>
     * </ul>
     */
    private int flags;
    /**
     * This 64-bit value is the maximum creation order index value stored for a
     * link in this group.
     * This field is present if bit 0 of flags is set.
     */
    private long maximumCreationIndex;
    /**
     * This is the address of the fractal heap to store dense links. Each link
     * stored in the fractal heap is stored as a Link Message.
     * <p>
     * If there are no links in the group, or the group’s links are stored
     * “compactly” (as object header messages), this value will be the undefined address.
     */
    private long fractalHeapAddress;
    /**
     * This is the address of the version 2 B-tree to index names of links.
     * If there are no links in the group, or the group’s links are stored
     * “compactly” (as object header messages), this value will be the
     * undefined address.
     */
    private long addressOfv2btreeNameIndex;
    /**
     * This is the address of the version 2 B-tree to index creation order of links.
     * <p>
     * If there are no links in the group, or the group’s links are stored
     * “compactly” (as object header messages), this value will be the undefined address.
     * <p>
     * This field exists if bit 1 of flags is set.
     */
    private long addressOfv2btreeCreationOrderIndex;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public LinkInfo() {
    }

    /**
     * Returns whether creation order for the links is tracked.
     *
     * @return whether creation order for the links is tracked
     */
    public boolean isCreationOrderTracked() {
        return (flags & 1) != 0;
    }

    /**
     * Returns whether creation order for the links is indexed.
     *
     * @return whether creation order for the links is indexed
     */
    public boolean isCreationOrderIndexed() {
        return (flags & 2) != 0;
    }

    /**
     * Returns the address of the fractal heap storing dense links.
     * Undefined when the group has no link or stores them compactly as object header messages.
     *
     * @return the address of the fractal heap storing dense links
     */
    public long getFractalHeapAddress() {
        return fractalHeapAddress;
    }

    /**
     * Returns the address of the version 2 B-tree indexing link names.
     * Undefined when the group has no link or stores them compactly.
     *
     * @return the address of the version 2 B-tree indexing link names
     */
    public long getV2BtreeNameIndexAddress() {
        return addressOfv2btreeNameIndex;
    }

    /**
     * Returns the address of the version 2 B-tree indexing link creation order.
     * Only meaningful when {@link #isCreationOrderIndexed()}.
     *
     * @return the address of the version 2 B-tree indexing link creation order
     */
    public long getV2BtreeCreationOrderIndexAddress() {
        return addressOfv2btreeCreationOrderIndex;
    }

    /**
     * Returns the maximum creation order index of the links in this group.
     * Only meaningful when {@link #isCreationOrderTracked()}.
     *
     * @return the maximum creation order index of the links in this group
     */
    public long getMaximumCreationIndex() {
        return maximumCreationIndex;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number for this message. This document describes version 0.
        */
        channel.ensureVersion(0);
        flags = channel.readUnsignedByte();
        if ((flags & 1) != 0) maximumCreationIndex = channel.readLong();
        fractalHeapAddress = channel.readOffset();
        addressOfv2btreeNameIndex = channel.readOffset();
        if ((flags & 2) != 0) addressOfv2btreeCreationOrderIndex = channel.readOffset();
    }
}
