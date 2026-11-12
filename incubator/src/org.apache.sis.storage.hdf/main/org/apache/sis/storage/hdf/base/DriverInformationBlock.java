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
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * The driver information block is an optional region of the file which contains
 * information needed by the file driver to reopen a file.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_boot_driver">II.B. Disk Format: Level 0B - File Driver Info</a>
 * @author Johann Sorel (Geomatys)
 */
public final class DriverInformationBlock extends IOStructure {

    /**
     * The size in bytes of the Driver Information field.
     */
    private int driverInformationSize;
    /**
     * This is an eight-byte ASCII string without null termination which identifies
     * the driver and/or version number of the Driver Information Block.
     * The predefined driver encoded in this field by the HDF5 Library is identified
     * by the letters NCSA followed by the first four characters of the driver name.
     * If the Driver Information block is not the original version then the last
     * letter(s) of the identification will be replaced by a version number in ASCII,
     * starting with 0.
     * <p>
     * Identification for user-defined drivers is also eight-byte long.
     * It can be arbitrary but should be unique to avoid the four character prefix “NCSA”.
     */
    private int driverIdentification;
    /**
     * Driver information is stored in a format defined by the file driver.
     */
    private byte[] driverInformation;

    /** Creates an empty structure, filled by {@link #read(HDF5DataInput)}. */
    public DriverInformationBlock() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number of the Driver Information Block.
        This document describes version 0.
         */
        channel.ensureVersion(0);
        channel.skipFully(3);
        driverInformationSize = channel.readInt();
        driverIdentification = channel.readInt();
        driverInformation = channel.readNBytes(driverInformationSize);
        //TODO MultiDriver or FamilyDriver
    }
}
