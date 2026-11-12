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
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * List of shared message records, used as a list index node of the shared object header message table.
 * Each record locates a shared message either in the fractal heap of the index or in an object header.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_sohm">III.I. Disk Format: Level 1I - Shared Object Header Message Table</a>
 * @author Johann Sorel (Geomatys)
 */
public final class SharedMessageRecordList extends IOStructure {

    /**
     * The ASCII character string “SMLI” is used to indicate the beginning of a
     * list of index nodes. This gives file consistency checking utilities a
     * better chance of reconstructing a damaged file.
     */
    public static final byte[] SIGNATURE = "SMLI".getBytes(StandardCharsets.US_ASCII);

    /**
     * The record for locating the shared message, either in the fractal heap
     * for the index, or an object header (see format for index nodes below).
     */
    public List<Object> sharedMessages;

    /** Creates an empty structure, filled by {@link #read(HDF5DataInput)}. */
    public SharedMessageRecordList() {
    }

    /**
     * Not yet implemented.
     *
     * @param channel channel positioned on the record list.
     * @throws IOException always.
     */
    public void read(HDF5DataInput channel) throws IOException {

        channel.ensureSignature(SIGNATURE);
        /*
        list
        checksum
        */
        throw new IOException("TODO");
    }
}
