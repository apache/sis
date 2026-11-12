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

import org.apache.sis.storage.hdf.internal.Constants;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.datatype.DataType;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Header Message Name: Attribute
 * <p>
 * Header Message Type: 0x000C
 * <p>
 * Length: Varies
 * <p>
 * Status: Optional; may be repeated.
 * <p>
 * Description:
 * The Attribute message is used to store objects in the HDF5 file
 * which are used as attributes, or “metadata” about the current object.
 * An attribute is a small dataset; it has a name, a datatype, a
 * dataspace, and raw data. Since attributes are stored in the object
 * header, they should be relatively small (in other words, less than
 * 64KB). They can be associated with any type of object which has an
 * object header (groups, datasets, or committed (named) datatypes).
 * <p>
 * In 1.8.x versions of the library, attributes can be larger than 64KB.
 * See the “Special Issues” section of the Attributes chapter in the
 * HDF5 User’s Guide for more information.
 * <p>
 * Note: Attributes on an object must have unique names: the HDF5
 * Library currently enforces this by causing the creation of an
 * attribute with a duplicate name to fail. Attributes on different
 * objects may have the same name, however.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_attribute">IV.A.2.m. The Attribute Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class AttributeMessage extends Message {

    private String name;
    /**
     * This bit field contains extra information about interpreting the
     * attribute message:
     * <ul>
     *   <li>Bit 0 : If set, datatype is shared.</li>
     *   <li>Bit 1 : If set, dataspace is shared.</li>
     * </ul>
     * This field is used in version 2 and 3.
     */
    private int flags;
    /**
     * The datatype description follows the same format as described for the
     * datatype object header message.
     */
    private DataType dataType;
    /**
     * The dataspace description follows the same format as described for the
     * dataspace object header message.
     */
    private DataspaceMessage dataspace;
    /**
     * The raw data for the attribute. The size is determined from the datatype
     * and dataspace descriptions. This field is not padded with additional bytes.
     */
    private Object data;
    private Exception dataException;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public AttributeMessage() {
    }

    /**
     * Returns the attribute name, unique among the attributes of an object.
     *
     * @return the attribute name, unique among the attributes of an object
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the decoded value, or {@code null} if the dataspace is null or decoding failed.
     *
     * @return the decoded value, or {@code null} if the dataspace is null or decoding failed
     */
    public Object getValue() {
        return data;
    }

    /**
     * Returns the exception raised while decoding the value, or {@code null}.
     *
     * @return the exception raised while decoding the value, or {@code null}
     */
    public Exception getDataException() {
        return dataException;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException, DataStoreException {

        /*
        The version number information is used for changes in the format of the
        attribute message and is described here:
        0 : Never used.
        1 : Used by the library before version 1.6 to encode attribute
        message. This version does not support shared datatypes.
        2 : Used by the library of version 1.6.x and after to encode attribute
        messages. This version supports shared datatypes. The fields of name,
        datatype, and dataspace are not padded with additional bytes of zero.
        3 : Used by the library of version 1.8.x and after to encode attribute
        messages. This version supports attributes with non-ASCII names.
         */
        final int version = channel.readUnsignedByte();

        if (version == 1) {
            channel.skipFully(1);
            /*
            The length of the attribute name in bytes including the null terminator.
            Note that the Name field below may contain additional padding not
            represented by this field.
            */
            final int nameSize = channel.readUnsignedShort();
            /*
            The length of the datatype description in the Datatype field below.
            Note that the Datatype field may contain additional padding not
            represented by this field.
            */
            final int dataTypeSize = channel.readUnsignedShort();
            /*
            The length of the dataspace description in the Dataspace field below.
            Note that the Dataspace field may contain additional padding not
            represented by this field.
            */
            final int dataspaceSize = channel.readUnsignedShort();
            /*
            The null-terminated attribute name. This field is padded with additional
            null characters to make it a multiple of eight bytes.
            */
            name = channel.readNullTerminatedString(8, StandardCharsets.US_ASCII);
            /*
            This field is padded with additional zero bytes to make it a multiple of eight bytes.
            */
            long position = channel.getStreamPosition();
            dataType = DataType.readMessageType(channel);
            channel.realign(position, 8);
            /*
            This field is padded with additional zero bytes to make it a multiple of eight bytes.
            */
            position = channel.getStreamPosition();
            dataspace = new DataspaceMessage();
            dataspace.read(channel);
            channel.realign(position, 8);

        } else if (version == 2) {
            flags = channel.readUnsignedByte();
            /*
            The length of the attribute name in bytes including the null terminator.
            */
            final int nameSize = channel.readUnsignedShort();
            /*
            The length of the datatype description in the Datatype field below.
            */
            final int dataTypeSize = channel.readUnsignedShort();
            /*
            The length of the dataspace description in the Dataspace field below.
            */
            final int dataspaceSize = channel.readUnsignedShort();

            name = channel.readNullTerminatedString(0, StandardCharsets.US_ASCII);
            dataType = DataType.readMessageType(channel);
            dataspace = new DataspaceMessage();
            dataspace.read(channel);

        } else if (version == 3) {
            flags = channel.readUnsignedByte();
            /*
            The length of the attribute name in bytes including the null terminator.
            */
            final int nameSize = channel.readUnsignedShort();
            /*
            The length of the datatype description in the Datatype field below.
            */
            final int dataTypeSize = channel.readUnsignedShort();
            /*
            The length of the dataspace description in the Dataspace field below.
            */
            final int dataspaceSize = channel.readUnsignedShort();
            /*
            The character set encoding for the attribute’s name:
            0 : ASCII character set encoding
            1 : UTF-8 character set encoding
            */
            final int nameCharacterSet = channel.readUnsignedByte();
            name = channel.readNullTerminatedString(0, nameCharacterSet == 0 ? StandardCharsets.US_ASCII : StandardCharsets.UTF_8);
            dataType = DataType.readMessageType(channel);
            dataspace = new DataspaceMessage();
            dataspace.read(channel);
        } else {
            throw new IOException("Unexpected dataspace version " + version);
        }

        if (!dataspace.isNull()) {
            try {
                data = dataType.readData(channel, dataspace.getDimensionSizes(), null);
            } catch (DataStoreException ex) {
                //decoding error, caused by a bad or broken file
                Constants.LOGGER.log(Level.WARNING, "Failed to read attribute " + name + " message data", ex);
                dataException = ex;
                data = null;
            }
        }

    }
}
