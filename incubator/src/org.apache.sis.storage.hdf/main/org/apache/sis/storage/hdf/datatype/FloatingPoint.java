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
 * Floating-point datatype. Only the IEEE 754 single and double precision
 * layouts, in little or big endian byte order, are decoded.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FloatingPoint extends DataType {

    /**
     * Byte Order. These two non-contiguous bits specify the “endianness” of the bytes in the datatype element.
     * This field stores bit 6 of the class bit fields as bit 1, and bit 0 as bit 0:
     * <ul>
     *   <li>0 : Byte order is little-endian</li>
     *   <li>1 : Byte order is big-endian</li>
     *   <li>2 : Reserved</li>
     *   <li>3 : Byte order is VAX-endian</li>
     * </ul>
     */
    public int byteOrder;
    /**
     * Padding type. Bit 1 is the low bits pad type, bit 2 is the high bits
     * pad type, and bit 3 is the internal bits pad type. If a datum has
     * unused bits at either end or between the sign bit, exponent, or mantissa,
     * then the value of bit 1, 2, or 3 is copied to those locations.
     */
    public int paddingType;
    /**
     * Mantissa Normalization. This 2-bit bit field specifies how the most
     * significant bit of the mantissa is managed:
     * <ul>
     *   <li>0 : No normalization</li>
     *   <li>1 : The most significant bit of the mantissa is always set (except for 0.0).</li>
     *   <li>2 : The most significant bit of the mantissa is not stored, but is implied to be set.</li>
     *   <li>3 : Reserved.</li>
     * </ul>
     */
    public int mantissaNormalisation;
    /**
     * Sign Location. This is the bit position of the sign bit. Bits are
     * numbered with the least significant bit zero.
     */
    public int signLocation;
    /**
     * The bit offset of the first significant bit of the floating-point
     * value within the datatype. The bit offset specifies the number of
     * bits “to the right of” the value.
     */
    public int bitOffset;
    /**
     * The number of bits of precision of the floating-point value within
     * the datatype.
     */
    public int bitPrecision;
    /**
     * The bit position of the exponent field. Bits are numbered with the
     * least significant bit number zero.
     */
    public int exponentLocation;
    /** The size of the exponent field in bits. */
    public int exponentSize;
    /**
     * The bit position of the mantissa field. Bits are numbered with the
     * least significant bit number zero.
     */
    public int mantissaLocation;
    /** The size of the mantissa field in bits. */
    public int mantissaSize;
    /** The bias of the exponent field. */
    public int exponentBias;


    //detect if the floating point type is a standard one.
    private enum KnownType {
        FLOAT32,
        FLOAT64,
        OTHER
    }
    private KnownType knownType = KnownType.OTHER;

    /**
     * Reads the datatype properties.
     *
     * @param byteSize element size in bytes.
     * @param classBitFields class bit fields (byte order, padding, mantissa normalization, sign location).
     * @param channel input positioned on the datatype properties.
     * @throws IOException if an I/O error occurred.
     */
    public FloatingPoint(int byteSize, int classBitFields, HDF5DataInput channel) throws IOException {
        super(byteSize);
        byteOrder = (classBitFields & 0b1) | ((classBitFields & 0b1000000) >> 5);
        paddingType = (classBitFields & 0b1110) >> 1;
        mantissaNormalisation = (classBitFields & 0b110000) >> 4;
        signLocation = (classBitFields & 0b1111111100000000) >> 8;
        bitOffset = channel.readUnsignedShort();
        bitPrecision = channel.readUnsignedShort();
        exponentLocation = channel.readUnsignedByte();
        exponentSize = channel.readUnsignedByte();
        mantissaLocation = channel.readUnsignedByte();
        mantissaSize = channel.readUnsignedByte();
        exponentBias = channel.readInt();

        if (       signLocation     == 63
                && bitOffset        == 0
                && bitPrecision     == 64
                && exponentLocation == 52
                && exponentSize     == 11
                && mantissaLocation == 0
                && mantissaSize     == 52) {
            knownType = KnownType.FLOAT64;
        } else if (signLocation     == 31
                && bitOffset        == 0
                && bitPrecision     == 32
                && exponentLocation == 23
                && exponentSize     == 8
                && mantissaLocation == 0
                && mantissaSize     == 23) {
            knownType = KnownType.FLOAT32;
        }

    }

    @Override
    public Class getValueClass() {
        return switch(knownType) {
            case FLOAT32 -> float.class;
            case FLOAT64 -> double.class;
            default -> double.class;
        };
    }

    @Override
    public Object readData(HDF5DataInput input, int ... compoundindexes) throws IOException {
        final ByteOrder previous = input.order();
        switch (byteOrder) {
            case 0:
                //little endian
                input.order(ByteOrder.LITTLE_ENDIAN);
                break;
            case 1:
                //big endian
                input.order(ByteOrder.BIG_ENDIAN);
                break;
            default:
                throw new IOException("Unsupported endian " + byteOrder);
        }

        try {
            switch (knownType) {
                case FLOAT64 :
                    return input.readDouble();
                case FLOAT32 :
                    return input.readFloat();
                default:
                    throw new IOException("Unsupported type " + knownType);
            }
        } finally {
            input.order(previous);
        }
    }
}
