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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.hdf.filter.Deflate;
import org.apache.sis.storage.hdf.filter.Fletcher32;
import org.apache.sis.storage.hdf.filter.Filter;
import org.apache.sis.storage.hdf.filter.Shuffle;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Header Message Name: Data Storage - Filter Pipeline
 * <p>
 * Header Message Type: 0x000B
 * <p>
 * Length: Varies
 * <p>
 * Status: Optional; may not be repeated.
 * <p>
 * Description:
 * This message describes the filter pipeline which should be applied to the
 * data stream by providing filter identification numbers, flags, a name, and
 * client data.
 * <p>
 * This message may be present in the object headers of both dataset and group
 * objects. For datasets, it specifies the filters to apply to raw data. For
 * groups, it specifies the filters to apply to the group’s fractal heap.
 * Currently, only datasets using chunked data storage use the filter pipeline
 * on their raw data.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_filter">IV.A.2.l. The Data Storage - Filter Pipeline Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class FilterPipelineMessage extends Message {

    /** deflate : GZIP deflate compression */
    public static final int DEFLATE = 1;
    /** shuffle : Data element shuffling */
    public static final int SHUFFLE = 2;
    /** fletcher32 : Fletcher32 checksum */
    public static final int FLETCHER32 = 3;
    /** szip : SZIP compression */
    public static final int SZIP = 4;
    /** nbit : N-bit packing */
    public static final int NBITPACKING = 5;
    /** scaleoffset : Scale and offset encoded values */
    public static final int SCALEOFFSET = 6;

    /** A description of each filter. */
    private List<FilterDescription> filters;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public FilterPipelineMessage() {
    }

    /**
     * Returns the filters, in writing order; decoding applies them in reverse order.
     *
     * @return the filters, in writing order; decoding applies them in reverse order
     */
    public List<FilterDescription> getFilters() {
        return filters;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number for this message.
        */
        final int version = channel.ensureVersion(1,2);
        /*
        The total number of filters described in this message. The maximum
        possible number of filters in a message is 32.
        */
        final int numberOfFilters = channel.readUnsignedByte();
        filters = new ArrayList<>(numberOfFilters);

        if (version == 1) {
            channel.skipFully(6);
            for (int i = 0; i < numberOfFilters; i++) {
                final FilterDescription desc = new FilterDescription();
                desc.filteridentificationValue = channel.readUnsignedShort();
                desc.nameLength = channel.readUnsignedShort();
                desc.flags = channel.readUnsignedShort();
                desc.numberOfClientDataValues = channel.readUnsignedShort();
                if (desc.nameLength > 0) {
                    desc.name = channel.readNullTerminatedString(8, StandardCharsets.US_ASCII);
                }
                desc.clientData = channel.readInts(desc.numberOfClientDataValues);
                /*
                Four bytes of zeroes are added to the message at this point if the
                Client Data Number of Values field contains an odd number.
                */
                if (desc.numberOfClientDataValues % 2 != 0) {
                    channel.skipFully(4);
                }
                filters.add(desc);
            }
        } else if (version == 2) {
            for (int i = 0; i < numberOfFilters; i++) {
                final FilterDescription desc = new FilterDescription();
                desc.filteridentificationValue = channel.readUnsignedShort();
                if (desc.filteridentificationValue >= 256) {
                    desc.nameLength = channel.readUnsignedShort();
                }
                desc.flags = channel.readUnsignedShort();
                desc.numberOfClientDataValues = channel.readUnsignedShort();
                if (desc.filteridentificationValue >= 256 && desc.nameLength > 0) {
                    desc.name = channel.readNullTerminatedString(0, StandardCharsets.US_ASCII);
                }
                desc.clientData = channel.readInts(desc.numberOfClientDataValues);
                filters.add(desc);
            }
        }

    }

    /** Description of a filter of the pipeline. */
    public static class FilterDescription {

        /**
         * This value, often referred to as a filter identifier, is designed to
         * be a unique identifier for the filter. Values from zero through
         * 32,767 are reserved for filters supported by The HDF Group in the
         * HDF5 Library and for filters requested and supported by third parties.
         * Filters supported by The HDF Group are documented immediately below.
         * Information on 3rd-party filters can be found at The HDF Group’s
         * Contributions page.
         * <p>
         * To request a filter identifier, please contact The HDF Group’s Help
         * Desk at The HDF Group Help Desk. You will be asked to provide the
         * following information:
         * <ul>
         *   <li>Contact information for the developer requesting the new identifier</li>
         *   <li>A short description of the new filter</li>
         *   <li>Links to any relevant information, including licensing information</li>
         * </ul>
         * <p>
         * Values from 32768 to 65535 are reserved for non-distributed uses
         * (for example, internal company usage) or for application usage when
         * testing a feature. The HDF Group does not track or document the use
         * of the filters with identifiers from this range.
         * <p>
         * The filters currently in library version 1.8.0 are listed below:
         * <ul>
         *   <li>0 : N/A : Reserved</li>
         *   <li>1 : deflate : GZIP deflate compression</li>
         *   <li>2 : shuffle : Data element shuffling</li>
         *   <li>3 : fletcher32 : Fletcher32 checksum</li>
         *   <li>4 : szip : SZIP compression</li>
         *   <li>5 : nbit : N-bit packing</li>
         *   <li>6 : scaleoffset : Scale and offset encoded values</li>
         * </ul>
         */
        public int filteridentificationValue;
        /**
         * Each filter has an optional null-terminated ASCII name and this field
         * holds the length of the name including the null termination padded
         * with nulls to be a multiple of eight. If the filter has no name then
         * a value of zero is stored in this field.
         */
        private int nameLength;
        /**
         * The flags indicate certain properties for a filter. The bit values
         * defined so far are:
         * <ul>
         *   <li>Bit 0 : If set then the filter is an optional filter. During output,
         *   if an optional filter fails it will be silently skipped in the
         *   pipeline.</li>
         *   <li>Bits 1-15 : Reserved (zero)</li>
         * </ul>
         */
        private int flags;
        /**
         * Each filter can store integer values to control how the filter
         * operates. The number of entries in the Client Data array is stored
         * in this field.
         */
        private int numberOfClientDataValues;
        /**
         * If the Name Length field is non-zero then it will contain the size
         * of this field, padded to a multiple of eight. This field contains
         * a null-terminated, ASCII character string to serve as a comment/name
         * for the filter.
         */
        private String name;
        /**
         * This is an array of four-byte integers which will be passed to the
         * filter function. The Client Data Number of Values determines the
         * number of elements in the array.
         */
        public int[] clientData;

        private Filter filter;

        /** Creates an empty description, filled when the enclosing message is read. */
        public FilterDescription() {
        }

        /**
         * Returns the filter implementation, created on first call.
         *
         * @return the filter implementation, created on first call
         * @throws IOException if the filter is unsupported or its client data are incomplete.
         */
        public Filter getFilter() throws IOException {
            if (filter == null) {
                filter = switch (filteridentificationValue) {
                    case FilterPipelineMessage.DEFLATE -> new Deflate();
                    case FilterPipelineMessage.SHUFFLE -> {
                        if (clientData == null || clientData.length == 0) {
                            throw new IOException("Shuffle filter declares no element size");
                        }
                        yield new Shuffle(clientData[0]);
                    }
                    case FilterPipelineMessage.FLETCHER32 -> new Fletcher32();
                    case FilterPipelineMessage.SZIP -> throw new IOException(
                            "SZIP compressed data is not supported by this pure Java reader");
                    default -> throw new IOException("Unsupported filter "
                            + filteridentificationValue + " (" + name + ").");
                };
            }
            return filter;
        }
    }
}
