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
 * Header Message Name: Attribute Info
 * <p>
 * Header Message Type: 0x0015
 * <p>
 * Length: Varies
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description:
 * This message stores information about the attributes on an object, such as
 * the maximum creation index for the attributes created and the location of
 * the attribute storage when the attributes are stored “densely”.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_attrinfo">IV.A.2.v. The Attribute Info Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class AttributeInfoMessage extends Message {

    /**
     * This is the attribute index information flag with the following
     * definition:
     * <ul>
     *   <li>Bit 0 : If set, creation order for attributes is tracked.</li>
     *   <li>Bit 1 : If set, creation order for attributes is indexed.</li>
     *   <li>Bits 2-7 : Reserved</li>
     * </ul>
     */
    private int flags;
    /**
     * This field is present if bit 0 of Flags is set.
     * <p>
     * This is the maximum creation order index value for the attributes on the
     * object.
     */
    private int maximumCreationIndex;
    /**
     * This is the address of the fractal heap to store dense attributes. Each
     * attribute stored in the fractal heap is described by the Attribute
     * Message.
     */
    private long fractalHeapAddress;
    /**
     * This is the address of the version 2 B-tree to index the names of densely
     * stored attributes.
     */
    private long attributeNameV2btreeAddress;
    /**
     * This field is present if bit 1 of Flags is set.
     * <p>
     * This is the address of the version 2 B-tree to index the creation order
     * of densely stored attributes.
     */
    private long attributeCreationOrderV2btreeAddress;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public AttributeInfoMessage() {
    }

    /**
     * Returns whether creation order for attributes is tracked.
     *
     * @return whether creation order for attributes is tracked
     */
    public boolean isCreationOrderTracked() {
        return (flags & 1) != 0;
    }

    /**
     * Returns whether creation order for attributes is indexed.
     *
     * @return whether creation order for attributes is indexed
     */
    public boolean isCreationOrderIndexed() {
        return (flags & 2) != 0;
    }

    /**
     * Returns the address of the fractal heap storing dense attributes.
     * Undefined when attributes are stored compactly as object header messages.
     *
     * @return the address of the fractal heap storing dense attributes
     */
    public long getFractalHeapAddress() {
        return fractalHeapAddress;
    }

    /**
     * Returns the address of the version 2 B-tree indexing dense attribute names.
     *
     * @return the address of the version 2 B-tree indexing dense attribute names
     */
    public long getAttributeNameV2btreeAddress() {
        return attributeNameV2btreeAddress;
    }

    /**
     * Returns the address of the version 2 B-tree indexing dense attribute creation order.
     * Only meaningful when {@link #isCreationOrderIndexed()}.
     *
     * @return the address of the version 2 B-tree indexing dense attribute creation order
     */
    public long getAttributeCreationOrderV2btreeAddress() {
        return attributeCreationOrderV2btreeAddress;
    }

    /**
     * Returns the maximum creation order index of the attributes.
     * Only meaningful when {@link #isCreationOrderTracked()}.
     *
     * @return the maximum creation order index of the attributes
     */
    public int getMaximumCreationIndex() {
        return maximumCreationIndex;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number for this message. This document describes version 0.
        */
        channel.ensureVersion(0);
        flags = channel.readUnsignedByte();
        if ((flags & 1) != 0) maximumCreationIndex = channel.readUnsignedShort();
        fractalHeapAddress = channel.readOffset();
        attributeNameV2btreeAddress = channel.readOffset();
        if ((flags & 2) != 0) attributeCreationOrderV2btreeAddress = channel.readOffset();
    }
}
