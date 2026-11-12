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
 * Fixed-point datatype : signed or unsigned integers. Only the 8, 16, 32 and
 * 64 bits integers without bit offset are decoded, as Java primitive values
 * large enough to hold unsigned values.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FixedPoint extends DataType {

    /**
     * Byte Order. If zero, byte order is little-endian; otherwise,
     * byte order is big endian.
     */
    public final ByteOrder byteOrder;
    /**
     * Padding type. Bit 1 is the lo_pad bit and bit 2 is the hi_pad bit.
     * If a datum has unused bits at either end, then the lo_pad or hi_pad
     * bit is copied to those locations.
     */
    public final int paddingType;
    /**
     * Signed. If this bit is set then the fixed-point number is in 2’s
     * complement form.
     */
    public final boolean signed;
    /**
     * The bit offset of the first significant bit of the fixed-point value
     * within the datatype. The bit offset specifies the number of bits
     * “to the right of” the value (which are set to the lo_pad bit value).
     */
    public final int byteOffset;
    /**
     * The number of bits of precision of the fixed-point value within the
     * datatype. This value, combined with the datatype element’s size and
     * the Bit Offset field specifies the number of bits “to the left of”
     * the value (which are set to the hi_pad bit value).
     */
    public final int bytePrecision;

    //detect if the fixed point type is a standard one.
    private enum KnownType {
        EMPTY,
        INT8,
        INT16,
        INT32,
        INT64,
        UINT8,
        UINT16,
        UINT32,
        OTHER
    }
    private KnownType knownType = KnownType.OTHER;

    /**
     * Reads the datatype properties.
     *
     * @param byteSize element size in bytes.
     * @param classBitFields class bit fields (byte order, padding type, sign).
     * @param channel input positioned on the datatype properties.
     * @throws IOException if an I/O error occurred or the precision is not supported.
     */
    public FixedPoint(int byteSize, int classBitFields, HDF5DataInput channel) throws IOException {
        super(byteSize);
        byteOrder = (classBitFields & 0b1) == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        paddingType = (classBitFields & 0b110) >> 1;
        signed = ((classBitFields & 0b1000) >> 3) != 0;
        byteOffset = channel.readUnsignedShort();
        bytePrecision = channel.readUnsignedShort();

        if (byteOffset == 0) {
            switch (bytePrecision) {
                case 8: knownType = signed ? KnownType.INT8 : KnownType.UINT8; break;
                case 16: knownType = signed ? KnownType.INT16 : KnownType.UINT16; break;
                case 32: knownType = signed ? KnownType.INT32 : KnownType.UINT32; break;
                case 64: knownType = KnownType.INT64; break;
                default:
                    throw new IOException("Unsupported fixed point structure");
            }
        }
    }

    @Override
    public Class getValueClass() {
        return switch(knownType) {
            case INT8 -> byte.class;
            case INT16 -> short.class;
            case INT32 -> int.class;
            case INT64 -> long.class;
            case UINT8 -> int.class;
            case UINT16 -> int.class;
            case UINT32 -> long.class;
            default -> long.class;
        };
    }

    @Override
    public Object readData(HDF5DataInput input, int ... compoundindexes) throws IOException {
        final ByteOrder previous = input.order();
        input.order(byteOrder);
        try {
            switch (knownType) {
                case INT8 :
                    return input.readByte();
                case INT16 :
                    /*
                    readShort returns an int, which would box to an Integer and be
                    rejected by the short[] array that getValueClass announces.
                    The value is already sign extended, so the cast is lossless.
                    */
                    return (short) input.readShort();
                case INT32 :
                    return input.readInt();
                case INT64 :
                    return input.readLong();
                case UINT8 :
                    return input.readUnsignedByte();
                case UINT16 :
                    return input.readUnsignedShort();
                case UINT32 :
                    return input.readUnsignedInt();
                default:
                    throw new IOException("Unsupported type " + knownType);
            }
        } finally {
            input.order(previous);
        }
    }
}
