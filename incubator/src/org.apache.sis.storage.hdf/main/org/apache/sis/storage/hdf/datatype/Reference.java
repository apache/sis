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
package org.apache.sis.storage.hdf.datatype;

import java.io.IOException;
import org.apache.sis.storage.hdf.ObjectReference;
import org.apache.sis.storage.hdf.heap.GlobalHeapId;
import org.apache.sis.storage.hdf.heap.GlobalHeapObject;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Reference datatype : a reference to another object of the file, or to a
 * region of a dataset. Values are decoded as {@link ObjectReference} instances.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class Reference extends DataType {

    /** A reference to another object of this file. */
    public static final int TYPE_OBJECT = 0;
    /** A reference to a region within a dataset of this file. */
    public static final int TYPE_DATASET_REGION = 1;

    /**
     * This four-bit value contains the revised reference types. The values defined are:
     * <ul>
     *   <li>0 : Object Reference (H5R_OBJECT1): A reference to another object in this HDF5 file. </li>
     *   <li>1 : Dataset Region Reference (H5R_DATASET_REGION1): A reference to a region within a dataset in this HDF5 file. </li>
     *   <li>2 : Object Reference (H5R_OBJECT2): A reference to another object in this file or an external file. </li>
     *   <li>3 : Dataset Region Reference (H5R_DATASET_REGION2): A reference to a region within a dataset in this file or an external file. </li>
     *   <li>4 : Attribute Reference (H5R_ATTR): A reference to an attribute attached to an object in this file or an external file. </li>
     *   <li>5-15 : Reserved </li>
     * </ul>
     */
    public final int type;
    /**
     * This four-bit value contains the version for encoding the revised reference types. The values defined are:
     * <ul>
     *   <li>0 : Unused</li>
     *   <li>1 : The version for encoding the revised reference types: Object Reference (2), Dataset Region Reference (3) and Attribute Reference (4). </li>
     *   <li>2-15 : Reserved</li>
     * </ul>
     */
    public final int version;

    /**
     * Reads the datatype properties.
     *
     * @param byteSize element size in bytes.
     * @param version datatype message version, 4 for the revised reference types.
     * @param classBitFields class bit fields (reference type, encoding version since version 4).
     * @param channel input positioned on the datatype properties, nothing is read.
     * @throws IOException if the datatype message version is unexpected.
     */
    public Reference(int byteSize, int version, int classBitFields, HDF5DataInput channel) throws IOException {
        super(byteSize);
        if (version < 4) {
            this.type = classBitFields & 0b1111;
            this.version = 0;
        } else if (version == 4) {
            this.type = classBitFields & 0b1111;
            this.version = classBitFields & 0b11110000;
        } else {
            throw new IOException("Unexpected reference version " + version);
        }
    }

    @Override
    public Class getValueClass() {
        /*
        The decoded value is a reference holder, not a plain address : returning
        long.class here would make Dataset allocate a long[] that readData can
        not store into.
        */
        return ObjectReference.class;
    }

    @Override
    public Object readData(HDF5DataInput input, int ... compoundindexes) throws IOException {
        if (type == TYPE_DATASET_REGION) {
            /*
            The element is a global heap identifier; the heap object holds the
            address of the dataset followed by a serialized dataspace selection.
            */
            final GlobalHeapId heapId = new GlobalHeapId();
            heapId.read(input);
            final GlobalHeapObject heapObject = input.getGlobalHeapObject(heapId);
            if (heapObject == null) {
                return new ObjectReference(-1, null);
            }
            //the heap object position is a position in the file, not in this buffer
            final HDF5DataInput fileChannel = input.getFileChannel();
            fileChannel.mark();
            try {
                fileChannel.seek(heapObject.streamDataPosition);
                final long address = fileChannel.readOffset();
                final int remaining = Math.toIntExact(heapObject.objectSize - fileChannel.getOffsetSize());
                return new ObjectReference(address, remaining > 0 ? fileChannel.readNBytes(remaining) : null);
            } finally {
                fileChannel.reset();
            }
        }
        if (type == TYPE_OBJECT) {
            return new ObjectReference(input.readOffset());
        }
        /*
        Revised reference types, whose payload is a length prefixed blob this
        reader does not interpret.
        */
        return new ObjectReference(-1, input.readNBytes(getByteSize()));
    }
}
