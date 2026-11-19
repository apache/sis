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
 * Fractal heap ID of a managed object, stored in a direct block of the heap.
 * The ID holds the object offset and length in the heap linear address space.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_fractalheap">III.G. Disk Format: Level 1G - Fractal Heap</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FractalHeapId_ManagedObject extends IOStructure {

    /**
     * This is a bit field with the following definition:
     * <p>
     * Bits :
     * <ul>
     * <li>6-7 : The current version of ID format. This document describes version 0. </li>
     * <li>4-5 : The ID type. Managed objects have a value of 0. </li>
     * <li>0-3 : Reserved. </li>
     * </ul>
     */
    private int versionType;
    /**
     * This field is the offset of the object in the heap. This field’s size is
     * the minimum number of bytes necessary to encode the Maximum Heap Size
     * value (from the Fractal Heap Header). For example, if the value of the
     * Maximum Heap Size is less than 256 bytes, this field is 1 byte in length,
     * a Maximum Heap Size of 256-65535 bytes uses a 2 byte length, and so on.
     */
    private int offset;
    /**
     * This field is the length of the object in the heap. It is determined by
     * taking the minimum value of Maximum Direct Block Size and Maximum Size
     * of Managed Objects in the Fractal Heap Header. Again, the minimum number
     * of bytes needed to encode that value is used for the size of this field.
     */
    private int length;

    /** Creates an empty heap ID, filled by {@link #read(HDF5DataInput)}. */
    public FractalHeapId_ManagedObject() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        versionType = channel.readUnsignedByte();
        throw new IOException("TODO");
    }
}
