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
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Header Message Name: Dataspace
 * <p>
 * Header Message Type: 0x0001
 * <p>
 * Length: Varies according to the number of dimensions.
 * <p>
 * Status: Required for dataset objects; may not be repeated.
 * <p>
 * Description: The dataspace message describes the number of dimensions
 * (in other words, “rank”) and size of each dimension that the data object has.
 * This message is only used for datasets which have a simple, rectilinear,
 * array-like layout; datasets requiring a more complex layout are not yet supported.
 * <p>
 * Version 2 of the dataspace message dropped the optional permutation index
 * value support, as it was never implemented in the HDF5 Library.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_simple">IV.A.2.b. The Dataspace Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class DataspaceMessage extends Message {

    /** This value is the number of dimensions that the data object has. */
    private int dimensionality;
    /**
     * This field is used to store flags to indicate the presence of parts of
     * this message. Bit 0 (the least significant bit) is used to indicate that
     * maximum dimensions are present. Bit 1 is used to indicate that permutation
     * indices are present.
     */
    private int flags;
    /**
     * This field indicates the type of the dataspace:
     * <ul>
     *   <li>0 : A scalar dataspace; in other words, a dataspace with a single,
     *   dimensionless element.</li>
     *   <li>1 : A simple dataspace; in other words, a dataspace with a rank greater
     *   than 0 and an appropriate number of dimensions.</li>
     *   <li>2 : A null dataspace; in other words, a dataspace with no elements.</li>
     * </ul>
     * Only in version 2.
     */
    private int type;
    /**
     * This value is the current size of the dimension of the data as stored in
     * the file. The first dimension stored in the list of dimensions is the
     * slowest changing dimension and the last dimension stored is the fastest
     * changing dimension.
     */
    private long[] dimensionSizes;
    /**
     * This value is the maximum size of the dimension of the data as stored in
     * the file. This value may be the special “unlimited” size which indicates
     * that the data may expand along this dimension indefinitely. If these
     * values are not stored, the maximum size of each dimension is assumed to
     * be the dimension’s current size.
     */
    private long[] dimensionMaximumSizes;
    /**
     * This value is the index permutation used to map each dimension from the
     * canonical representation to an alternate axis for each dimension. If these
     * values are not stored, the first dimension stored in the list of dimensions
     * is the slowest changing dimension and the last dimension stored is the
     * fastest changing dimension.
     * <p>
     * This property has never been implemented by libhdf.
     */
    private long[] permutationIndexes;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public DataspaceMessage() {
    }

    /**
     * Returns a new array of the current dimension sizes, empty for a scalar dataspace.
     * The first dimension is the slowest changing one.
     *
     * @return a new array of the current dimension sizes, empty for a scalar dataspace
     * @throws ArithmeticException if a size does not fit in an {@code int}.
     */
    public int[] getDimensionSizes() {
        final int[] size = new int[dimensionSizes.length];
        for (int i = 0; i < size.length; i++) {
            size[i] = Math.toIntExact(dimensionSizes[i]);
        }
        return size;
    }

    /**
     * Returns the current dimension sizes as a grid extent starting at 0.
     *
     * @return the current dimension sizes as a grid extent starting at 0
     */
    public GridExtent getDimensionExtent() {
        return new GridExtent(null, null, dimensionSizes, false);
    }

    /**
     * Returns a new array of the maximum dimension sizes.
     *
     * @return a new array of the maximum dimension sizes
     * @throws NullPointerException if the maximum sizes are not stored in this message.
     * @throws ArithmeticException if a size, for example “unlimited”, does not fit in an {@code int}.
     */
    public int[] getDimensionMaximumSizes() {
        final int[] maxs = new int[dimensionMaximumSizes.length];
        for (int i = 0; i < maxs.length; i++) {
            maxs[i] = Math.toIntExact(dimensionMaximumSizes[i]);
        }
        return maxs;
    }

    /**
     * Returns whether this is a null dataspace, without any element (version 2 only).
     *
     * @return whether this is a null dataspace, without any element (version 2 only)
     */
    public boolean isNull() {
        return type == 2;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        final int version = channel.readUnsignedByte();

        if (version == 1) {
            dimensionality = channel.readUnsignedByte();
            flags = channel.readUnsignedByte();
            channel.skipFully(5);
            dimensionSizes = new long[dimensionality];
            for (int i = 0; i < dimensionality; i++) {
                dimensionSizes[i] = channel.readLength();
            }
            if ((flags & 1) != 0) {
                dimensionMaximumSizes = new long[dimensionality];
                for (int i = 0; i < dimensionality; i++) {
                    dimensionMaximumSizes[i] = channel.readLength();
                }
            }
            if ((flags & 2) != 0) {
                permutationIndexes = new long[dimensionality];
                for (int i = 0; i < dimensionality; i++) {
                    permutationIndexes[i] = channel.readLength();
                }
            }
        } else if (version == 2) {
            dimensionality = channel.readUnsignedByte();
            flags = channel.readUnsignedByte();
            /*
            This field indicates the type of the dataspace:
            0 : A scalar dataspace; in other words, a dataspace with a single,
                dimensionless element.
            1 : A simple dataspace; in other words, a dataspace with a rank greater
                than 0 and an appropriate number of dimensions.
            2 : A null dataspace; in other words, a dataspace with no elements.
            */
            type = channel.readUnsignedByte();
            dimensionSizes = new long[dimensionality];
            for (int i = 0; i < dimensionality; i++) {
                dimensionSizes[i] = channel.readLength();
            }
            if ((flags & 1) != 0) {
                dimensionMaximumSizes = new long[dimensionality];
                for (int i = 0; i < dimensionality; i++) {
                    dimensionMaximumSizes[i] = channel.readLength();
                }
            }
        } else {
            throw new IOException("Unexpected dataspace version " + version);
        }
    }
}
