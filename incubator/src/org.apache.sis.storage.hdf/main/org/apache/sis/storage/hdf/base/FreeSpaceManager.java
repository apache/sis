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
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Free-space managers are used to describe space within a heap or the entire
 * HDF5 file that is not currently used for that heap or file.
 * <p>
 * The free-space manager header contains metadata information about the space
 * being tracked, along with the address of the list of free space sections which
 * actually describes the free space. The header records information about
 * free-space sections being tracked, creation parameters for handling free-space
 * sections of a client, and section information used to locate the collection
 * of free-space sections.
 * <p>
 * The free-space section list stores a collection of free-space sections that
 * is specific to each client of the free-space manager. For example, the fractal
 * heap is a client of the free space manager and uses it to track unused space
 * within the heap. There are 4 types of section records for the fractal heap,
 * each of which has its own format.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_infra_freespaceindex">III.H. Disk Format: Level 1H - Free-space Index</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FreeSpaceManager extends IOStructure {

    /**
     * The ASCII character string “FSHD” is used to indicate the beginning of
     * the Free-space Manager Header. This gives file consistency checking
     * utilities a better chance of reconstructing a damaged file.
     */
    public static final byte[] SIGNATURE = "FSHD".getBytes(StandardCharsets.US_ASCII);

    /**
     * This is the client ID for identifying the user of this free-space manager:
     * <ul>
     * <li>0 : Fractal heap</li>
     * <li>1 : File</li>
     * <li>2+ : Reserved</li>
     * </ul>
     */
    private int clientId;
    /**
     * This is the total amount of free space being tracked, in bytes.
     */
    private long totalSpaceTracked;
    /**
     * This is the total number of free-space sections being tracked.
     */
    private long totalNumberOfSections;
    /**
     * This is the number of serialized free-space sections being tracked.
     */
    private long numberOfSerializedSections;
    /**
     * This is the number of un-serialized free-space sections being managed.
     * Un-serialized sections are created by the free-space client when the
     * list of sections is read in.
     */
    private long numberOfUnserializedSections;
    /**
     * This is the number of section classes handled by this free space manager
     * for the free-space client.
     */
    private int numberOfSectionClasses;
    /**
     * This is the percent of current size to shrink the allocated serialized
     * free-space section list.
     */
    private int shrinkPercent;
    /**
     * This is the percent of current size to expand the allocated serialized
     * free-space section list.
     */
    private int expandPercent;
    /**
     * This is the size of the address space that free-space sections are within.
     * This is stored as the log2 of the actual value (in other words, the number
     * of bits required to store values within that address space).
     */
    private int sizeOfAddressSpace;
    /**
     * This is the maximum size of a section to be tracked.
     */
    private long maximumSectionSize;
    /**
     * This is the address where the serialized free-space section list is stored.
     */
    private long addressOfSerializedSectionList;
    /**
     * This is the size of the serialized free-space section list used (in bytes).
     * This value must be less than or equal to the allocated size of serialized
     * section list, below.
     */
    private long sizeOfSerializedSectionListUsed;
    /**
     * This is the size of serialized free-space section list actually allocated
     * (in bytes).
     */
    private long allocatedSizeOfSerializedSectionList;

    /** Creates an empty structure, filled by {@link #read(HDF5DataInput)}. */
    public FreeSpaceManager() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        channel.ensureSignature(SIGNATURE);
        /*
        This is the version number for the Free-space Manager Header and this
        document describes version 0.
        */
        channel.ensureVersion(0);
        clientId = channel.readUnsignedByte();
        totalSpaceTracked = channel.readLength();
        totalNumberOfSections = channel.readLength();
        numberOfSerializedSections = channel.readLength();
        numberOfUnserializedSections = channel.readLength();
        numberOfSectionClasses = channel.readUnsignedShort();
        shrinkPercent = channel.readUnsignedShort();
        expandPercent = channel.readUnsignedShort();
        sizeOfAddressSpace = channel.readUnsignedShort();
        maximumSectionSize = channel.readLength();
        addressOfSerializedSectionList = channel.readOffset();
        sizeOfSerializedSectionListUsed = channel.readLength();
        allocatedSizeOfSerializedSectionList = channel.readLength();
        int checksum = channel.readInt();
    }
}
