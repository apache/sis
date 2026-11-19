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
 * Header Message Name: Object Modification Time (Old)
 * <p>
 * Header Message Type: 0x000E
 * <p>
 * Length: Fixed
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description:
 * The object modification date and time is a timestamp which indicates (using
 * ISO-8601 date and time format) the last modification of an object. The time
 * is updated when any object header message changes according to the system
 * clock where the change was posted. All fields of this message should be
 * interpreted as coordinated universal time (UTC).
 * <p>
 * This modification time message is deprecated in favor of the “new” Object
 * Modification Time message and is no longer written to the file in versions
 * of the HDF5 Library after the 1.6.0 version.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_omodified">IV.A.2.o. The Object Modification Time (Old) Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class ModificationTimeMessageOld extends Message {

    /** The four-digit year as an ASCII string. For example, 1998. */
    private int year;
    /**
     * The month number as a two digit ASCII string where January is 01 and
     * December is 12.
     */
    private int month;
    /**
     * The day number within the month as a two digit ASCII string. The first
     * day of the month is 01.
     */
    private int dayOfMonth;
    /**
     * The hour of the day as a two digit ASCII string where midnight is 00 and
     * 11:00pm is 23.
     */
    private int hour;
    /**
     * The minute of the hour as a two digit ASCII string where the first minute
     * of the hour is 00 and the last is 59.
     */
    private int minute;
    /**
     * The second of the minute as a two digit ASCII string where the first
     * second of the minute is 00 and the last is 59.
     */
    private int second;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public ModificationTimeMessageOld() {
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        year = channel.readInt();
        month = channel.readUnsignedShort();
        dayOfMonth = channel.readUnsignedShort();
        hour = channel.readUnsignedShort();
        minute = channel.readUnsignedShort();
        second = channel.readUnsignedShort();
        channel.skipFully(2);
    }
}
