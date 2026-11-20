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
 * Time datatype, decoded as the raw {@code int} or {@code long} value.
 * <p>
 * Format is obscure, the specification does not say anything on the bits definition or
 * calendar.
 * <p>
 * In HDF-5 source code version 1.12.2 file {@code H5Odtype.c} line 1275 we can see
 * the properties field is two bytes long.
 * <p>
 * In HDF-5 source code version 1.12.2 file {@code H5Tpublic.h} line 1791 there is a note :
 * <blockquote>
 * Unsupported datatype: The time datatype class, {@code H5T_TIME},
 * is not supported. If {@code H5T_TIME} is used, the resulting data will
 * be readable and modifiable only on the originating computing
 * platform; it will not be portable to other platforms.
 * </blockquote>
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class Time extends DataType {

    /**
     * Byte Order. If zero, byte order is little-endian; otherwise, byte
     * order is big endian.
     */
    public final ByteOrder byteOrder;
    /** The number of bits of precision of the time value. */
    public final int bitPrecision;

    /**
     * Reads the datatype properties.
     *
     * @param byteSize element size in bytes.
     * @param classBitFields class bit fields (byte order).
     * @param channel input positioned on the datatype properties.
     * @throws IOException if an I/O error occurred.
     */
    public Time(int byteSize, int classBitFields, HDF5DataInput channel) throws IOException {
        super(byteSize);
        byteOrder = (classBitFields & 0b1) == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        bitPrecision = channel.readUnsignedShort();
    }

    @Override
    public Class getValueClass() {
        return getByteSize() > 4 ? long.class : int.class;
    }

    @Override
    public Object readData(HDF5DataInput input, int ... compoundindexes) throws IOException {
        /*
        The element occupies the declared number of bytes, not always two, and
        only the declared number of bits is significant.
        */
        final int nbBytes = getByteSize();
        final byte[] datas = input.readNBytes(nbBytes);
        long value = 0;
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            for (int i = nbBytes - 1; i >= 0; i--) {
                value = (value << 8) | (datas[i] & 0xFF);
            }
        } else {
            for (int i = 0; i < nbBytes; i++) {
                value = (value << 8) | (datas[i] & 0xFF);
            }
        }
        if (bitPrecision > 0 && bitPrecision < 64) {
            value &= (1L << bitPrecision) - 1;
        }
        return getByteSize() > 4 ? (Object) value : (Object) (int) value;
    }
}
