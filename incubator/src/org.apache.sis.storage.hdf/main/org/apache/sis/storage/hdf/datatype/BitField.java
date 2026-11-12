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
import java.nio.ByteOrder;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Bit field datatype : a sequence of bits, decoded as a {@code long}.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class BitField extends DataType {

    /**
     * Byte Order. If zero, byte order is little-endian; otherwise, byte order
     * is big endian.
     */
    private ByteOrder byteOrder;
    /**
     * Padding type. Bit 1 is the lo_pad type and bit 2 is the hi_pad type.
     * If a datum has unused bits at either end, then the lo_pad or hi_pad bit
     * is copied to those locations.
     */
    private int paddingType;
    /**
     * The bit offset of the first significant bit of the bit field within the
     * datatype. The bit offset specifies the number of bits “to the right of”
     * the value.
     */
    public final int bitOffset;
    /** The number of bits of precision of the bit field within the datatype. */
    public final int bitPrecision;

    /**
     * Reads the datatype properties.
     *
     * @param byteSize element size in bytes.
     * @param classBitFields class bit fields (byte order, padding type).
     * @param channel input positioned on the datatype properties.
     * @throws IOException if an I/O error occurred.
     */
    public BitField(int byteSize, int classBitFields, HDF5DataInput channel) throws IOException {
        super(byteSize);
        byteOrder = (classBitFields & 0b1) == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        paddingType = (classBitFields & 0b110) >> 1;

        bitOffset = channel.readUnsignedShort();
        bitPrecision = channel.readUnsignedShort();
    }

    @Override
    public Class getValueClass() {
        return Long.class;
    }

    @Override
    public Object readData(HDF5DataInput input, int ... compoundindexes) throws IOException {
        final ByteOrder previous = input.order();
        input.order(byteOrder);
        try {
            long position1 = input.getStreamPosition();
            input.readBits(bitOffset);
            long value = input.readBits(bitPrecision);
            input.skipRemainingBits();
            long position2 = input.getStreamPosition();
            long remain = getByteSize() - (position2 - position1);
            if (remain > 0) {
                input.skipFully((int) remain);
            } else if (remain < 0) {
                throw new IOException();
            }

            return value;
        } finally {
            input.order(previous);
        }


    }
}
