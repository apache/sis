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
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.io.HDF5DataInput;
import org.apache.sis.storage.hdf.message.Message;

/**
 * An object header contains the information needed to interpret an object (group, dataset, named datatype),
 * stored as a list of header messages. Two formats exist: version 1 ({@link ObjectHeaderV1})
 * and version 2 ({@link ObjectHeaderV2}).
 *
 * @author Johann Sorel (Geomatys)
 */
public interface ObjectHeader {

    /**
     * Returns all messages in this header.
     *
     * @return all messages in this header
     */
    List<Message> getMessages();

    /**
     * Reads this object header from the current channel position.
     *
     * @param channel channel positioned on the object header.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the content is invalid or unsupported.
     */
    void read(HDF5DataInput channel) throws IOException, DataStoreException;

    /**
     * Creates an empty object header, to be filled by {@link #read(HDF5DataInput)}.
     *
     * @param version first byte of the object header: 1 for version 1, or 79
     *        (ASCII {@code 'O'} of the {@code "OHDR"} signature) for version 2.
     * @return the new object header.
     * @throws IllegalArgumentException if the version is not supported.
     */
    static ObjectHeader forVersion(int version) {
        return switch (version) {
            case 1 -> new ObjectHeaderV1();
            case 79 -> new ObjectHeaderV2();
            default -> throw new IllegalArgumentException("Unknowned header version " + version);
        };
    }
}
