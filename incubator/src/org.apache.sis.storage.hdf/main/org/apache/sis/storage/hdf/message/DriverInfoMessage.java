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
package org.apache.sis.storage.hdf.message;

import java.io.IOException;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Header Message Name: Driver Info
 * <p>
 * Header Message Type: 0x0014
 * <p>
 * Length: Varies
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description:
 * This message contains information needed by the file driver to reopen a file.
 * This message is only found in the superblock extension: see the “Disk Format:
 * Level 0C - Superblock Extension” section for more information. For more
 * information on the fields in the driver info message, see the “Disk Format:
 * Level 0B - File Driver Info” section; those who use the multi and family
 * file drivers will find this section particularly helpful.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_drvinfo">IV.A.2.u. The Driver Info Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class DriverInfoMessage extends Message {

    /**
     * This is an eight-byte ASCII string without null termination which
     * identifies the driver.
     */
    private int driverIdentification;
    /** The size in bytes of the Driver Information field of this message. */
    private int driverInformationSize;
    /** Driver information is stored in a format defined by the file driver. */
    private int driverInformation;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public DriverInfoMessage() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number for this message. This document describes version 0.
        */
        channel.ensureVersion(0);
        throw new IOException("TODO");
    }
}
