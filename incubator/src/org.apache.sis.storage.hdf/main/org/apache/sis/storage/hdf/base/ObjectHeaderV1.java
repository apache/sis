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
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.io.HDF5DataInput;
import org.apache.sis.storage.hdf.message.Message;
import org.apache.sis.storage.hdf.message.ObjectHeaderContinuationMessage;
import org.apache.sis.storage.hdf.message.SharedMessageV1;
import org.apache.sis.storage.hdf.message.SharedMessageV2;
import org.apache.sis.storage.hdf.message.SharedMessageV3;

/**
 * The header information of an object is designed to encompass all of the
 * information about an object, except for the data itself. This information
 * includes the dataspace, the datatype, information about how the data is
 * stored on disk (in external files, compressed, broken up in blocks, and so
 * on), as well as other information used by the library to speed up access to
 * the data objects or maintain a file’s integrity. Information stored by user
 * applications as attributes is also stored in the object’s header. The header
 * of each object is not necessarily located immediately prior to the object’s
 * data in the file and in fact may be located in any position in the file. The
 * order of the messages in an object header is not significant.
 *
 * <p>
 * Object headers are composed of a prefix and a set of messages. The prefix
 * contains the information needed to interpret the messages and a small amount
 * of metadata about the object, and the messages contain the majority of the
 * metadata about the object.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_prefix_one">IV.A.1.a. Version 1 Data Object Header Prefix</a>
 * @author Johann Sorel (Geomatys)
 */
public final class ObjectHeaderV1 extends IOStructure implements ObjectHeader {

    /**
     * This value determines the total number of messages listed in object headers
     * for this object. This value includes the messages in continuation messages
     * for this object.
     */
    private int totalNumberOfHeaderMessages;
    /**
     * This value specifies the number of “hard links” to this object within the
     * current file. References to the object from external files, “soft links”
     * in this file and object references in this file are not tracked.
     */
    private int objectReferenceCount;
    /**
     * This value specifies the number of bytes of header message data following
     * this length field that contain object header messages for this object header.
     * This value does not include the size of object header continuation blocks
     * for this object elsewhere in the file.
     */
    private int objectHeaderSize;
    /** All header messages, including those read from continuation blocks. */
    public List<Message> messages;

    /** Creates an empty object header, filled by {@link #read(HDF5DataInput)}. */
    public ObjectHeaderV1() {
    }

    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException, DataStoreException {
        /*
        This value is used to determine the format of the information in the object
        header. When the format of the object header is changed, the version number
        is incremented and can be used to determine how the information in the
        object header is formatted. This is version one (1) (there was no version
        zero (0)) of the object header.
        */
        channel.ensureVersion(1);
        channel.skipFully(1);
        totalNumberOfHeaderMessages = channel.readUnsignedShort();
        objectReferenceCount = channel.readInt();
        objectHeaderSize = channel.readInt();
        channel.skipFully(4);
        messages = new ArrayList<>();
        while (messages.size() < totalNumberOfHeaderMessages) {
            readMessage(channel);
        }
    }

    private void readMessage(HDF5DataInput channel) throws IOException, DataStoreException {
        /*
         * This value specifies the type of information included in the
         * following header message data. The message types for header messages
         * are defined in sections below.
         */
        final int headerMessageType = channel.readUnsignedShort();
        /*
         * This value specifies the number of bytes of header message data
         * following the header message type and length information for the
         * current message. The size includes padding bytes to make the message
         * a multiple of eight bytes.
         */
        final int sizeOfHeaderMessageData = channel.readUnsignedShort();
        /*
         * This is a bit field with the following definition:
         * <ul>
         * <li>0 : If set, the message data is constant. This is used for
         * messages like the datatype message of a dataset. </li>
         * <li>1 : If set, the message is shared and stored in another location
         * than the object header. The Header Message Data field contains a
         * Shared Message (described in the Data Object Header Messages section
         * below) and the Size of Header Message Data field contains the size of
         * that Shared Message. </li>
         * <li>2 : If set, the message should not be shared. </li>
         * <li>3 : If set, the HDF5 decoder should fail to open this object if
         * it does not understand the message’s type and the file is open with
         * permissions allowing write access to the file. (Normally, unknown
         * messages can just be ignored by HDF5 decoders) </li>
         * <li>4 : If set, the HDF5 decoder should set bit 5 of this message’s
         * flags (in other words, this bit field) if it does not understand the
         * message’s type and the object is modified in any way. (Normally,
         * unknown messages can just be ignored by HDF5 decoders) </li>
         * <li>5 : If set, this object was modified by software that did not
         * understand this message. (Normally, unknown messages should just be
         * ignored by HDF5 decoders) (Can be used to invalidate an index or a
         * similar feature) </li>
         * <li>6 : If set, this message is shareable. </li>
         * <li>7 : If set, the HDF5 decoder should always fail to open this
         * object if it does not understand the message’s type (whether it is
         * open for read-only or read-write access). (Normally, unknown messages
         * can just be ignored by HDF5 decoders) </li>
         * </ul>
         */
        final int headerMessageFlags = channel.readUnsignedByte();
        channel.skipFully(3);
        final long position1 = channel.getStreamPosition();
        if ((headerMessageFlags & 2) != 0) {
            //shared message
            final int version = channel.readUnsignedByte();
            channel.seek(channel.getStreamPosition() - 1);
            final Message message = switch (version) {
                case 1 ->
                    new SharedMessageV1();
                case 2 ->
                    new SharedMessageV2();
                case 3 ->
                    new SharedMessageV3();
                default ->
                    throw new IOException("Unexpected shared message version " + version);
            };
            message.read(channel);
            messages.add(message);
        } else {
            /*
             * The format and length of this field is determined by the header
             * message type and size respectively. Some header message types do
             * not require any data and this information can be eliminated by
             * setting the length of the message to zero. The data is padded
             * with enough zeroes to make the size a multiple of eight.
             */
            final int version = channel.readUnsignedByte();
            channel.seek(channel.getStreamPosition() - 1);
            final Message message = Message.forCode(headerMessageType, version);
            message.read(channel);

            if (message instanceof ObjectHeaderContinuationMessage ctm) {
                //more messages at a different location
                channel.mark();
                channel.seek(ctm.offset);
                final long end = channel.getStreamPosition() + ctm.length;
                while ((end - channel.getStreamPosition()) > 0) {
                    readMessage(channel);
                }
                channel.reset();
            }
            messages.add(message);
        }
        final long position2 = channel.getStreamPosition();

        final long padding = (position1 + sizeOfHeaderMessageData) - position2;
        if (padding == 0) {
            //all good
        } else if (padding > 0) {
            //a few bytes of padding
            channel.seek(position1 + sizeOfHeaderMessageData);
        } else {
            throw new IOException("Message data size is smaller then what was decoded.");
        }
    }
}
