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
 * Version 2 data object header, starting with the “OHDR” signature.
 * <p>
 * Note that the “total number of messages” field has been dropped from the data
 * object header prefix in this version. The number of messages in the data
 * object header is just determined by the messages encountered in all the object
 * header blocks.
 * <p>
 * Note also that the fields and messages in this version of data object headers
 * have no alignment or padding bytes inserted - they are stored packed together.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_prefix_two">IV.A.1.b. Version 2 Data Object Header Prefix</a>
 * @author Johann Sorel (Geomatys)
 */
public final class ObjectHeaderV2 extends IOStructure implements ObjectHeader {

    /**
     * The ASCII character string “OHDR” is used to indicate the beginning of an
     * object header. This gives file consistency checking utilities a better
     * chance of reconstructing a damaged file.
     */
    public static final byte[] SIGNATURE = "OHDR".getBytes(StandardCharsets.US_ASCII);

    /**
     * Mask of the flags bits giving the size of the Size of Chunk #0 field.
     */
    private static final int FLAG_CHUNK0_SIZE_MASK = 0x03;
    /**
     * Flag bit set when attribute creation order is tracked, in which case every
     * header message carries a 2 bytes creation order field.
     */
    private static final int FLAG_ATTRIBUTE_CREATION_ORDER_TRACKED = 0x04;
    /**
     * Flag bit set when non default attribute storage phase change values are stored.
     */
    private static final int FLAG_PHASE_CHANGE_STORED = 0x10;
    /**
     * Flag bit set when access, modification, change and birth times are stored.
     */
    private static final int FLAG_TIMES_STORED = 0x20;

    /**
     * This field is a bit field indicating additional information about the object header.
     * <ul>
     * <li>0-1 : This two bit field determines the size of the Size of Chunk #0 field. The values are:
     *  0 : The Size of Chunk #0 field is 1 byte.
     *  1 : The Size of Chunk #0 field is 2 bytes.
     *  2 :The Size of Chunk #0 field is 4 bytes.
     *  3 :The Size of Chunk #0 field is 8 bytes.
     * </li>
     * <li>2 : If set, attribute creation order is tracked.</li>
     * <li>3 : If set, attribute creation order is indexed.</li>
     * <li>4 : If set, non-default attribute storage phase change values are stored.</li>
     * <li>5 : If set, access, modification, change and birth times are stored.</li>
     * <li>6-7 : Reserved</li>
     * </ul>
     */
    private int flags;
    /**
     * This 32-bit value represents the number of seconds after the UNIX epoch
     * when the object’s raw data was last accessed (in other words, read or written).
     *
     * This field is present if bit 5 of flags is set.
     */
    private int accessTime;
    /**
     * This 32-bit value represents the number of seconds after the UNIX epoch
     * when the object’s raw data was last modified (in other words, written).
     *
     * This field is present if bit 5 of flags is set.
     */
    private int modificationTime;
    /**
     * This 32-bit value represents the number of seconds after the UNIX epoch
     * when the object’s metadata was last changed.
     *
     * This field is present if bit 5 of flags is set.
     */
    private int changeTime;
    /**
     * This 32-bit value represents the number of seconds after the UNIX epoch
     * when the object was created.
     *
     * This field is present if bit 5 of flags is set.
     */
    private int birthTime;
    /**
     * This is the maximum number of attributes to store in the compact format
     * before switching to the indexed format.
     *
     * This field is present if bit 4 of flags is set.
     */
    private int maximumCompactAttributes;
    /**
     * This is the minimum number of attributes to store in the indexed format
     * before switching to the compact format.
     *
     * This field is present if bit 4 of flags is set.
     */
    private int minimumDenseAttributes;

    /** All messages of this header, including those stored in continuation blocks. */
    private final List<Message> messages = new ArrayList<>();

    /** Creates an empty object header, filled by {@link #read(HDF5DataInput)}. */
    public ObjectHeaderV2() {
    }

    @Override
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Returns whether every header message carries a creation order field.
     * Continuation blocks need this information, which is only available in the header prefix.
     *
     * @return whether every header message carries a creation order field
     */
    boolean isCreationOrderTracked() {
        return (flags & FLAG_ATTRIBUTE_CREATION_ORDER_TRACKED) != 0;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException, DataStoreException {
        channel.ensureSignature(SIGNATURE);
        /*
        This field has a value of 2 indicating version 2 of the object header.
         */
        channel.ensureVersion(2);
        flags = channel.readUnsignedByte();
        if ((flags & FLAG_TIMES_STORED) != 0) {
            accessTime = channel.readInt();
            modificationTime = channel.readInt();
            changeTime = channel.readInt();
            birthTime = channel.readInt();
        }
        if ((flags & FLAG_PHASE_CHANGE_STORED) != 0) {
            maximumCompactAttributes = channel.readUnsignedShort();
            minimumDenseAttributes = channel.readUnsignedShort();
        }
        /*
         * This unsigned value specifies the number of bytes of header message
         * data following this field that contain object header information.
         * This value does not include the size of object header continuation
         * blocks for this object elsewhere in the file.
         * The length of this field varies depending on bits 0 and 1 of the flags field.
         */
        final long sizeOfChunk0 = channel.readUnsignedInteger(1 << (flags & FLAG_CHUNK0_SIZE_MASK));

        /*
        The size of chunk 0 covers the messages and the trailing gap, but not the
        checksum, which follows them.
        */
        final long end = channel.getStreamPosition() + sizeOfChunk0;
        readMessages(channel, end);

        /*
        A gap in an object header chunk is inferred by the end of the messages
        for the chunk before the beginning of the chunk’s checksum. Gaps are
        always smaller than the size of an object header message prefix
        (message type + message size + message flags).

        Gaps are formed when a message (typically an attribute message) in an earlier
        chunk is deleted and a message from a later chunk that does not quite
        fit into the free space is moved into the earlier chunk.
        */
        channel.seek(end);
        final int checksum = channel.readInt();
    }

    /**
     * Read all messages found between the current position and the given end
     * position, appending them to this header.
     * <p>
     * Reading stops as soon as the remaining bytes are too few to hold a message
     * prefix : that remainder is the gap described by the format specification,
     * which guarantees a gap is always smaller than a message prefix.
     *
     * @param channel channel positioned on the first message
     * @param end position of the first byte after the last message
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a message is invalid or unsupported.
     */
    void readMessages(HDF5DataInput channel, long end) throws IOException, DataStoreException {
        /*
        Message prefix : type (1) + size (2) + flags (1), plus the creation order
        (2) when the header tracks it.
        */
        final int prefixSize = isCreationOrderTracked() ? 6 : 4;
        while ((end - channel.getStreamPosition()) >= prefixSize) {
            readMessage(channel);
        }
    }

    /**
     * Read a single version 2 object header message.
     * <p>
     * Compared to version 1 the message type is stored on one byte instead of
     * two, there are no three reserved bytes, the size does not include any
     * padding and an optional creation order field may be present.
     *
     * @param channel channel positioned on the message prefix
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the message is invalid or unsupported.
     */
    private void readMessage(HDF5DataInput channel) throws IOException, DataStoreException {
        /*
         * Same format as version 1 of the object header, but on a single byte.
         */
        final int headerMessageType = channel.readUnsignedByte();
        /*
         * This value specifies the number of bytes of header message data
         * following the header message type and length information for the
         * current message. The size of messages in this version does not
         * include any padding bytes.
         */
        final int sizeOfHeaderMessageData = channel.readUnsignedShort();
        /*
         * Same format as version 1 of the object header.
         */
        final int headerMessageFlags = channel.readUnsignedByte();
        if (isCreationOrderTracked()) {
            /*
             * This field stores the order that a message of a given type was created in.
             * This field is present if bit 2 of the header flags is set.
             */
            channel.readUnsignedShort();
        }

        final long dataStart = channel.getStreamPosition();
        final Message message;
        if ((headerMessageFlags & 2) != 0) {
            //shared message, the data is a Shared Message structure
            final int version = channel.readUnsignedByte();
            channel.seek(dataStart);
            message = switch (version) {
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
        } else {
            final int version = channel.readUnsignedByte();
            channel.seek(dataStart);
            message = Message.forCode(headerMessageType, version);
            message.read(channel);
        }
        messages.add(message);

        /*
        Messages are packed, but a reader that decoded fewer bytes than announced
        must still resume at the right place.
        */
        channel.seek(dataStart + sizeOfHeaderMessageData);

        if (message instanceof ObjectHeaderContinuationMessage ctm) {
            //more messages at a different location
            channel.mark();
            channel.seek(ctm.offset);
            final ObjectHeaderContinuationBlockV2 block = new ObjectHeaderContinuationBlockV2();
            block.read(channel, this, ctm.length);
            channel.reset();
        }
    }
}
