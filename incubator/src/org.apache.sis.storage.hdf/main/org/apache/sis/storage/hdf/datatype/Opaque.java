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
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Opaque datatype : raw bytes the format does not interpret, carrying an ASCII
 * tag that says what the application means by them.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class Opaque extends DataType {

    /** Application provided description of the data, may be empty. */
    public final String tag;

    /**
     * Reads the datatype properties, which are the ASCII tag.
     *
     * @param byteSize element size in bytes.
     * @param classBitFields class bit fields (tag length).
     * @param channel input positioned on the datatype properties.
     * @throws IOException if an I/O error occurred.
     */
    public Opaque(int byteSize, int classBitFields, HDF5DataInput channel) throws IOException {
        super(byteSize);
        /*
        The low byte of the class bit field is the length of the tag, already
        padded to a multiple of eight bytes.
        */
        final int tagLength = classBitFields & 0xFF;
        final byte[] datas = channel.readNBytes(tagLength);
        int end = 0;
        while (end < datas.length && datas[end] != 0) end++;
        tag = new String(datas, 0, end, StandardCharsets.US_ASCII);
    }

    @Override
    public Class getValueClass() {
        return byte[].class;
    }

    @Override
    public Object readData(HDF5DataInput input, int ... compoundindexes) throws IOException {
        return input.readNBytes(getByteSize());
    }
}
