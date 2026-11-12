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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * String datatype : a fixed length string, decoded as a {@link String}
 * with its padding removed.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class Chars extends DataType {

    /**
     * Padding type. This four-bit value determines the type of padding to
     * use for the string. The values are:
     * <ul>
     *   <li>0 : Null Terminate: A zero byte marks the end of the string and is
     *   guaranteed to be present after converting a long string to a short
     *   string. When converting a short string to a long string the value
     *   is padded with additional null characters as necessary.</li>
     *   <li>1 : Null Pad: Null characters are added to the end of the value during
     *   conversions from short values to long values but conversion in the
     *   opposite direction simply truncates the value.</li>
     *   <li>2 : Space Pad: Space characters are added to the end of the value
     *   during conversions from short values to long values but conversion
     *   in the opposite direction simply truncates the value. This is the
     *   Fortran representation of the string.</li>
     *   <li>3-15 : Reserved</li>
     * </ul>
     */
    public final int paddingType;
    /**
     * Character Set. The character set used to encode the string:
     * <ul>
     *   <li>0 : ASCII character set encoding</li>
     *   <li>1 : UTF-8 character set encoding</li>
     *   <li>2-15 : Reserved</li>
     * </ul>
     */
    public final int charset;

    /**
     * Reads the datatype properties.
     *
     * @param byteSize string size in bytes.
     * @param classBitFields class bit fields (padding type, character set).
     * @param channel input positioned on the datatype properties, nothing is read.
     * @throws IOException if an I/O error occurred.
     */
    public Chars(int byteSize, int classBitFields, HDF5DataInput channel) throws IOException {
        super(byteSize);
        paddingType = classBitFields & 0b1111;
        charset = (classBitFields & 0b11110000) >> 4;
    }

    @Override
    public Class getValueClass() {
        return String.class;
    }

    @Override
    public Object readData(HDF5DataInput input, int ... compoundindexes) throws IOException {
        byte[] raw = input.readNBytes(getByteSize());
        int end = raw.length;
        endsearch:
        for (int i = raw.length-1; i > 0; i--, end--) {
            switch (paddingType) {
                case 0:
                case 1:
                    if (raw[i] != 0) break endsearch;
                    break;
                case 2:
                    if (raw[i] != ' ') break endsearch;
                    break;
                default:
                    throw new AssertionError();
            }
        }
        raw = Arrays.copyOf(raw, end);
        return new String(raw, (charset == 0) ? StandardCharsets.US_ASCII : StandardCharsets.UTF_8);
    }
}
