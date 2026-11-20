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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Header Message Name: Link
 * <p>
 * Header Message Type: 0x0006
 * <p>
 * Length: Varies
 * <p>
 * Status: Optional; may be repeated.
 * <p>
 * Description:
 * This message encodes the information for a link in a group’s object header, when the group is storing its links “compactly”, or in the group’s fractal heap, when the group is storing its links “densely”.
 * <p>
 * A group is storing its links compactly when the fractal heap address in the Link Info Message is set to the “undefined address” value.
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsubsec_fmt3_dataobject_hdr_msg_link">IV.A.2.g. The Link Message</a>
 * @author Johann Sorel (Geomatys)
 */
public final class LinkMessage extends Message {

    /** A hard link, the link information is the address of the target object header. */
    public static final int LINK_TYPE_HARD = 0;
    /** A soft link, the link information is a path which may or may not resolve. */
    public static final int LINK_TYPE_SOFT = 1;
    /** An external link, the target lives in another file. */
    public static final int LINK_TYPE_EXTERNAL = 64;

    /** Mask of the flags bits giving the size of the Length of Link Name field. */
    private static final int FLAG_NAME_LENGTH_SIZE_MASK = 0x03;
    /** Flag bit set when the Creation Order field is present. */
    private static final int FLAG_CREATION_ORDER_PRESENT = 0x04;
    /** Flag bit set when the Link Type field is present, meaning the link is not a hard link. */
    private static final int FLAG_LINK_TYPE_PRESENT = 0x08;
    /** Flag bit set when the Link Name Character Set field is present. */
    private static final int FLAG_CHARACTER_SET_PRESENT = 0x10;

    /** Information carried by a link, the shape of which depends on the link type. */
    public sealed interface LinkInformation permits Hard, Soft, External, UserDefined {}

    /**
     * A hard link, pointing at an object of the same file.
     *
     * @param objectHeaderAddress address of the target object header
     */
    public record Hard(long objectHeaderAddress) implements LinkInformation {}

    /**
     * A soft link, holding a path that is resolved when the link is followed.
     * The path may be dangling.
     *
     * @param value path of the target, absolute or relative to the group holding the link
     */
    public record Soft(String value) implements LinkInformation {}

    /**
     * An external link, pointing at an object of another file.
     *
     * @param version version of the external link format
     * @param flags flags of the external link format
     * @param fileName name of the file holding the target, usually relative to the file holding the link
     * @param objectPath path of the target within that file
     */
    public record External(int version, int flags, String fileName, String objectPath) implements LinkInformation {}

    /**
     * A user defined link, whose meaning is unknown to this reader.
     *
     * @param linkType link class type, from 65 to 255
     * @param datas raw link information
     */
    public record UserDefined(int linkType, byte[] datas) implements LinkInformation {}

    /**
     * This field contains information about the link and controls the presence
     * of other fields below.
     * <ul>
     *   <li>Bits 0-1 : Determines the size of the Length of Link Name field.
     *     <ul>
     *       <li>0 : The size of the Length of Link Name field is 1 byte.</li>
     *       <li>1 : The size of the Length of Link Name field is 2 bytes.</li>
     *       <li>2 : The size of the Length of Link Name field is 4 bytes.</li>
     *       <li>3 : The size of the Length of Link Name field is 8 bytes.</li>
     *     </ul>
     *   </li>
     *   <li>Bit 2 : Creation Order Field Present: if set, the Creation Order field is present. If not set, creation order information is not stored for links in this group.</li>
     *   <li>Bit 3 : Link Type Field Present: if set, the link is not a hard link and the Link Type field is present. If not set, the link is a hard link.</li>
     *   <li>Bit 4 : Link Name Character Set Field Present: if set, the link name is not represented with the ASCII character set and the Link Name Character Set field is present. If not set, the link name is represented with the ASCII character set.</li>
     *   <li>Bits 5-7 : Reserved (zero).</li>
     * </ul>
     */
    private int flags;
    /**
     * This field is present if bit 3 of Flags is set.
     * <p>
     * This is the link class type and can be one of the following values:
     * <ul>
     *   <li>0 : A hard link (should never be stored in the file)</li>
     *   <li>1 : A soft link.</li>
     *   <li>2-63 : Reserved for future HDF5 internal use.</li>
     *   <li>64 : An external link.</li>
     *   <li>65-255 : Reserved, but available for user-defined link types.</li>
     * </ul>
     */
    private int linkType;
    /**
     * This field is present if bit 2 of Flags is set.
     * <p>
     * This 64-bit value is an index of the link’s creation time within the group.
     * Values start at 0 when the group is created and increment by one for each
     * link added to the group. Removing a link from a group does not change
     * existing links’ creation order field.
     */
    private long creationOrder;
    /**
     * This field is present if bit 4 of Flags is set.
     * <p>
     * This is the character set for encoding the link’s name:
     * <ul>
     *   <li>0 : ASCII character set encoding (this should never be stored in the file)</li>
     *   <li>1 : UTF-8 character set encoding</li>
     * </ul>
     */
    private int linkNameCharacterSet;
    /**
     * This is the length of the link’s name. The size of this field depends on
     * bits 0 and 1 of Flags.
     */
    private long lengthOfLinkName;
    /** This is the name of the link, non-NULL terminated. */
    private String linkName;
    /** The format of this field depends on the link type. */
    private LinkInformation linkInformation;

    /** Creates an empty message, filled by {@link #read(HDF5DataInput)}. */
    public LinkMessage() {
    }

    /**
     * Returns the name of this link within the group holding it.
     *
     * @return the name of this link within the group holding it
     */
    public String getName() {
        return linkName;
    }

    /**
     * Returns the link class type, one of the {@code LINK_TYPE_*} constants.
     *
     * @return the link class type, one of the {@code LINK_TYPE_*} constants
     */
    public int getLinkType() {
        return linkType;
    }

    /**
     * Returns the creation order index in the group, or 0 if not tracked.
     *
     * @return the creation order index in the group, or 0 if not tracked
     */
    public long getCreationOrder() {
        return creationOrder;
    }

    /**
     * Returns the target of this link, whose class depends on the link type.
     *
     * @return the target of this link, whose class depends on the link type
     */
    public LinkInformation getLinkInformation() {
        return linkInformation;
    }

    private static Charset charsetOf(int linkNameCharacterSet) {
        return linkNameCharacterSet == 1 ? StandardCharsets.UTF_8 : StandardCharsets.US_ASCII;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {
        /*
        The version number for this message. This document describes version 1.
        */
        channel.ensureVersion(1);
        flags = channel.readUnsignedByte();

        /*
        Every field below the flags is optional. Reading them unconditionally
        desynchronizes the whole object header, the common case being a hard
        link with an ASCII name, for which flags is zero and none of them exist.
        */
        if ((flags & FLAG_LINK_TYPE_PRESENT) != 0) {
            linkType = channel.readUnsignedByte();
        } else {
            linkType = LINK_TYPE_HARD;
        }
        if ((flags & FLAG_CREATION_ORDER_PRESENT) != 0) {
            creationOrder = channel.readLong();
        }
        if ((flags & FLAG_CHARACTER_SET_PRESENT) != 0) {
            linkNameCharacterSet = channel.readUnsignedByte();
        }

        lengthOfLinkName = channel.readUnsignedInteger(1 << (flags & FLAG_NAME_LENGTH_SIZE_MASK));
        if (lengthOfLinkName < 0 || lengthOfLinkName > Integer.MAX_VALUE) {
            throw new IOException("Unreasonable link name length " + lengthOfLinkName);
        }
        linkName = new String(channel.readNBytes((int) lengthOfLinkName), charsetOf(linkNameCharacterSet));

        linkInformation = readLinkInformation(channel);
    }

    private LinkInformation readLinkInformation(HDF5DataInput channel) throws IOException {
        switch (linkType) {
            case LINK_TYPE_HARD: {
                /*
                This is the address of the object header for the object that the
                link points to.
                */
                return new Hard(channel.readOffset());
            }
            case LINK_TYPE_SOFT: {
                /*
                This is the length of the soft link value, followed by that many
                bytes, not NULL terminated.
                */
                final int length = channel.readUnsignedShort();
                return new Soft(new String(channel.readNBytes(length), StandardCharsets.UTF_8));
            }
            case LINK_TYPE_EXTERNAL: {
                /*
                This is the length of the external link value, followed by a
                buffer holding a version and flags byte, then two NULL terminated
                strings : the name of the file and the path of the object in it.
                */
                final int length = channel.readUnsignedShort();
                final byte[] datas = channel.readNBytes(length);
                final HDF5DataInput sub = channel.subChannel(datas);
                final int versionAndFlags = sub.readUnsignedByte();
                final String fileName = sub.readNullTerminatedString(0, StandardCharsets.UTF_8);
                final String objectPath = sub.readNullTerminatedString(0, StandardCharsets.UTF_8);
                return new External((versionAndFlags >>> 4) & 0x0F, versionAndFlags & 0x0F, fileName, objectPath);
            }
            default: {
                /*
                User defined link, the data is opaque to this reader.
                */
                final int length = channel.readUnsignedShort();
                return new UserDefined(linkType, channel.readNBytes(length));
            }
        }
    }
}
