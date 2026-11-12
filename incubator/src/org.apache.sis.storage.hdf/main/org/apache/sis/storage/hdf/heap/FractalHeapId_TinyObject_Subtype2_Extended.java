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
 * Fractal heap ID of a tiny object, extended form (heap IDs longer than 18 bytes).
 * The object data is embedded in the ID, after two bytes holding a 12 bits length.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_fractalheap">III.G. Disk Format: Level 1G - Fractal Heap</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FractalHeapId_TinyObject_Subtype2_Extended extends IOStructure {

    /**
     * This is a bit field with the following definition:
     * <p>
     * Bits :
     * <ul>
     * <li>6-7 : The current version of ID format. This document describes version 0. </li>
     * <li>4-5 : The ID type. Tiny objects have a value of 2. </li>
     * <li>0-3 : These 4 bits, together with the next byte, form an unsigned
     * 12-bit integer for holding the length of the object. These 4-bits are bits
     * 8-11 of the 12-bit integer. See description for the Extended Length
     * field below.</li>
     * </ul>
     */
    private int versionTypeLength;
    /**
     * This byte, together with the 4 bits in the previous byte, forms an
     * unsigned 12-bit integer for holding the length of the tiny object.
     * These 8 bits are bits 0-7 of the 12-bit integer formed. The value stored
     * is one less than the actual length (since zero-length objects are not
     * allowed to be stored in the heap). For example, an object of actual
     * length 1 has an encoded length of 0, an object of actual length 2 has
     * an encoded length of 1, and so on.
     */
    private int extendedLength;
    /**
     * This is the data for the object.
     */
    private byte[] data;

    /** Creates an empty heap ID, filled by {@link #read(HDF5DataInput)}. */
    public FractalHeapId_TinyObject_Subtype2_Extended() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        versionTypeLength = channel.readUnsignedByte();
        extendedLength = channel.readUnsignedByte();
        throw new IOException("TODO");
    }
}
