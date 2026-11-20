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
package org.apache.sis.storage.hdf.datatype;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.internal.StringUtilities;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * The Properties field of a compound datatype is a list of the member definitions
 * of the compound datatype. The member definitions appear one after another
 * with no intervening bytes. The member types are described with a (recursively)
 * encoded datatype message.
 * <p>
 * Note that the property descriptions are different for different versions
 * of the datatype version. Additionally note that the version 0 datatype
 * encoding is deprecated and has been replaced with later encodings in versions
 * of the HDF5 Library from the 1.4 release onward.
 * <p>
 * Values are decoded as a {@code double[]} when all members are {@code double},
 * or as an {@code Object[]} otherwise, with one entry per requested member.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_dtmessage">IV.A.2.d. The Datatype Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class Compound extends DataType {

    /** Member definitions, in declaration order. */
    public final Member[] members;

    private final Class componentClass;
    private final Class valueClass;
    private final int[] allMembers;

    /**
     * Reads the datatype properties, which are the member definitions.
     *
     * @param byteSize size in bytes of a whole compound element.
     * @param version datatype message version (1, 2 or 3).
     * @param classBitFields class bit fields (number of members).
     * @param channel input positioned on the datatype properties.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the version is unexpected or a member datatype is invalid.
     */
    public Compound(int byteSize, int version, int classBitFields, HDF5DataInput channel) throws IOException, DataStoreException {
        super(byteSize);
        /*
        Number of Members. This field contains the number of members defined
        for the compound datatype. The member definitions are listed in the
        Properties field of the data type message.
         */
        final int numberOfMembers = classBitFields & 0b1111111111111111;
        members = new Member[numberOfMembers];
        for (int i = 0; i < numberOfMembers; i++) {
            final Member member = new Member();
            member.read(version, byteSize, channel);
            members[i] = member;
        }

        //compute value class
        //we have a optimized version for double array.
        Class c = double.class;
        for (int i = 0; i < members.length; i++) {
            Class mc = members[i].memberType.getValueClass();
            if (mc != c) {
                c = Object.class;
                break;
            }
        }
        componentClass = c;
        valueClass = java.lang.reflect.Array.newInstance(c, 1).getClass();

        allMembers = new int[members.length];
        for (int i=0;i<members.length;i++) {
            allMembers[i] = i;
        }
    }

    @Override
    public Class getValueClass() {
        return valueClass;
    }

    @Override
    public Object readData(HDF5DataInput input, int size, int ... compoundindexes) throws IOException, DataStoreException {
        if (double.class.equals(componentClass)) {
            final double[][] array = new double[size][0];
            for (int x = 0; x < size; x++) {
                array[x] = (double[]) readData(input, compoundindexes);
            }
            return array;
        } else {
            final Object[][] array = new Object[size][0];
            for (int x = 0; x < size; x++) {
                array[x] = (Object[]) readData(input, compoundindexes);
            }
            return array;
        }
    }

    @Override
    public Object readData(HDF5DataInput channel, int ... compoundindexes) throws IOException, DataStoreException {
        final long position = channel.getStreamPosition();

        if (compoundindexes == null || compoundindexes.length == 0) compoundindexes = allMembers;

        if (double.class.equals(componentClass)) {
            final double[] array = new double[compoundindexes.length];
            for (int i = 0; i < compoundindexes.length; i++) {
                channel.seek(position + members[compoundindexes[i]].byteOffsetOfMember);
                array[i] = (double) members[compoundindexes[i]].memberType.readData(channel);
            }
            //move to end of data
            channel.seek(position + byteSize);
            return array;
        } else {
            final Object[] array = new Object[compoundindexes.length];
            for (int i = 0; i < compoundindexes.length; i++) {
                channel.seek(position + members[compoundindexes[i]].byteOffsetOfMember);
                array[i] = members[compoundindexes[i]].memberType.readData(channel);
            }
            //move to end of data
            channel.seek(position + byteSize);
            return array;
        }
    }

    @Override
    public String toString() {
        return StringUtilities.toStringTree(this.getClass().getSimpleName(), Arrays.asList(members));
    }

    /** Member of a compound datatype : name, offset and datatype. */
    public static class Member {
       /**
        * This NUL-terminated string is the name of the member, which must be
        * unique within the compound datatype.
        */
       public String name;
       /** This is the byte offset of the member within the datatype. */
       public int byteOffsetOfMember;
       /**
        * This field is the size of a dimension of the array field as stored in
        * the file. The first dimension stored in the list of dimensions is the
        * slowest changing dimension and the last dimension stored is the fastest
        * changing dimension.
        * <p>
        * Defined in version 1 only.
        */
       public int[] dimensionSizes;
       /** This field is a datatype message describing the datatype of the member. */
       public DataType memberType;

       /** Creates an empty member, filled when the enclosing compound is read. */
       public Member() {
       }

       private void read(int version, int size, HDF5DataInput channel) throws IOException, DataStoreException {
           if (version == 1) {
                name = channel.readNullTerminatedString(8, StandardCharsets.US_ASCII);
                /*
                This is the byte offset of the member within the datatype.
                 */
                byteOffsetOfMember = channel.readInt();
                /*
                If set to zero, this field indicates a scalar member. If set to
                a value greater than zero, this field indicates that the member
                is an array of values. For array members, the size of the array
                is indicated by the ‘Size of Dimension n’ field in this message.
                 */
                final int dimensionality = channel.readUnsignedByte();
                channel.skipFully(3);
                /*
                This field was intended to allow an array field to have its
                dimensions permuted, but this was never implemented. This field
                should always be set to zero.
                 */
                final int dimensionPermutation = channel.readInt();
                channel.skipFully(4);
                dimensionSizes = new int[dimensionality];
                for (int i = 0; i < 4; i++) {
                    final int s = channel.readInt();
                    if (i < dimensionSizes.length) dimensionSizes[i] = s;
                }
                memberType = DataType.readMessageType(channel);
            } else if (version == 2) {
                name = channel.readNullTerminatedString(8, StandardCharsets.US_ASCII);
                byteOffsetOfMember = channel.readInt();
                memberType = DataType.readMessageType(channel);
            } else if (version == 3) {
                name = channel.readNullTerminatedString(0, StandardCharsets.US_ASCII);
                if (size < 256) {
                    byteOffsetOfMember = channel.readUnsignedByte();
                } else if (size < 65536) {
                    byteOffsetOfMember = channel.readUnsignedShort();
                } else if (size < 16777216) {
                    byteOffsetOfMember = channel.readUnsignedInt24();
                } else {
                    byteOffsetOfMember = channel.readInt();
                }
                memberType = DataType.readMessageType(channel);
            } else {
                throw new DataStoreException("Unexpected Compound datatype version " + version);
            }
        }

       @Override
       public String toString() {
           final StringBuilder sb = new StringBuilder(name);
           sb.append(" offset:").append(byteOffsetOfMember);
           if (dimensionSizes != null && dimensionSizes.length > 0) {
               sb.append(" size:").append(Arrays.toString(dimensionSizes));
           }
           sb.append(" datatype:").append(memberType);
           return sb.toString();
       }
    }
}
