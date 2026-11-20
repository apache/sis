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
 * Fractal heap ID of an indirectly accessed huge object.
 * The ID holds a key into the version 2 B-tree of huge objects of the heap.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_fractalheap">III.G. Disk Format: Level 1G - Fractal Heap</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FractalHeapId_HugeObject_IndirectlyAccessed extends IOStructure {

    /**
     * This is a bit field with the following definition:
     * <p>
     * Bits :
     * <ul>
     * <li>6-7 : The current version of ID format. This document describes version 0. </li>
     * <li>4-5 : The ID type. Huge objects have a value of 1. </li>
     * <li>0-3 : Reserved. </li>
     * </ul>
     */
    private int versionType;
    /**
     * This field is the key for retrieving the information about the object
     * from the v2 B-tree for huge objects.
     */
    private int v2BtreeKey;

    /** Creates an empty heap ID, filled by {@link #read(HDF5DataInput)}. */
    public FractalHeapId_HugeObject_IndirectlyAccessed() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        versionType = channel.readUnsignedByte();
        throw new IOException("TODO");
    }
}
