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
package org.apache.sis.storage.hdf.heap;

import java.io.IOException;
import org.apache.sis.storage.hdf.base.IOStructure;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Fractal heap ID of a tiny object, normal form (heap IDs of 18 bytes or less).
 * The object data is embedded in the ID, after one byte holding a 4 bits length.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_fractalheap">III.G. Disk Format: Level 1G - Fractal Heap</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FractalHeapId_TinyObject_Subtype1_Normal extends IOStructure {

    /**
     * This is a bit field with the following definition:
     * <p>
     * Bits :
     * <ul>
     * <li>6-7 : The current version of ID format. This document describes version 0. </li>
     * <li>4-5 : The ID type. Tiny objects have a value of 2. </li>
     * <li>0-3 : The length of the tiny object. The value stored is one less
     * than the actual length (since zero-length objects are not allowed to be
     * stored in the heap). For example, an object of actual length 1 has an
     * encoded length of 0, an object of actual length 2 has an encoded length
     * of 1, and so on. </li>
     * </ul>
     */
    private int versionTypeLength;
    /**
     * This is the data for the object.
     */
    private byte[] data;

    /** Creates an empty heap ID, filled by {@link #read(HDF5DataInput)}. */
    public FractalHeapId_TinyObject_Subtype1_Normal() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        versionTypeLength = channel.readUnsignedByte();
        throw new IOException("TODO");
    }
}
