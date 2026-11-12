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
import java.nio.ByteBuffer;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * The superblock may begin at certain predefined offsets within the HDF5 file,
 * allowing a block of unspecified content for users to place additional information
 * at the beginning (and end) of the HDF5 file without limiting the HDF5 Library’s
 * ability to manage the objects within the file itself.
 * This feature was designed to accommodate wrapping an HDF5 file in another file
 * format or adding descriptive information to an HDF5 file without requiring the
 * modification of the actual file’s information. The superblock is located by
 * searching for the HDF5 format signature at byte offset 0, byte offset 512,
 * and at successive locations in the file, each a multiple of two of the
 * previous location; in other words, at these byte offsets: 0, 512, 1024, 2048, and so on.
 *
 * <p>
 * The superblock is composed of the format signature, followed by a superblock
 * version number and information that is specific to each version of the superblock.
 *
 * <p>
 * Currently, there are four versions of the superblock format:
 * <ul>
 *   <li>Version 0 is the default format.</li>
 *   <li>Version 1 is the same as version 0 but with the “Indexed Storage Internal Node K”
 * field for storing non-default B-tree ‘K’ value.</li>
 *   <li>Version 2 has some fields eliminated and compressed from superblock format
 * versions 0 and 1. It has added checksum support and superblock extension to store additional superblock metadata.</li>
 *   <li>Version 3 is the same as version 2 except that the field “File Consistency Flags”
 * is used for file locking. This format version will enable support for the latest version.</li>
 * </ul>
 *
 * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#subsec_fmt3_boot_super">II.A. Disk Format: Level 0A - Format Signature and Superblock</a>
 * @author Johann Sorel (Geomatys)
 */
public final class SuperBlock extends IOStructure {

    /**
     * This field contains a constant value and can be used to quickly identify
     * a file as being an HDF5 file. The constant value is designed to allow easy
     * identification of an HDF5 file and to allow certain types of data corruption to be detected.
     * <p>
     * This signature both identifies the file as an HDF5 file and provides for
     * immediate detection of common file-transfer problems.
     * The first two bytes distinguish HDF5 files on systems that expect the first
     * two bytes to identify the file type uniquely.
     * The first byte is chosen as a non-ASCII value to reduce the probability
     * that a text file may be misrecognized as an HDF5 file; also, it catches
     * bad file transfers that clear bit 7. Bytes two through four name the format.
     * The CR-LF sequence catches bad file transfers that alter newline sequences.
     * The control-Z character stops file display under MS-DOS.
     * The final line feed checks for the inverse of the CR-LF translation problem.
     * (This is a direct descendent of the PNG file signature.)
     * This field is present in version 0+ of the superblock.
     */
    public static final byte[] SIGNATURE = new byte[]{(byte) 137, 72, 68, 70, 13, 10, 26, 10};

    /**
     * This value is used to determine the format of the information in the superblock.
     * When the format of the information in the superblock is changed,
     * the version number is incremented to the next integer and can be used to
     * determine how the information in the superblock is formatted.
     * <p>
     * This field is present in version 0+ of the superblock.
     */
    private int superblockVersion;
    /**
     * This value is used to determine the format of the file’s free space information.
     * The only value currently valid in this field is ‘0’,
     * <p>
     * This field is present in versions 0 and 1 of the superblock.
     */
    private int freespaceVersion;
    /**
     * This value is used to determine the format of the information in the Root
     * Group Symbol Table Entry. When the format of the information in that field
     * is changed, the version number is incremented to the next integer and can
     * be used to determine how the information in the field is formatted.
     * <p>
     * The only value currently valid in this field is ‘0’.
     * <p>
     * This field is present in version 0 and 1 of the superblock.
     */
    private int rootGroupSymbolTableVersion;
    /**
     * This value is used to determine the format of the information in a shared
     * object header message. Since the format of the shared header messages
     * differs from the other private header messages, a version number is used
     * to identify changes in the format.
     * <p>
     * The only value currently valid in this field is ‘0’.
     * <p>
     * This field is present in version 0 and 1 of the superblock.
     */
    private int sharedHeaderMessageVersion;
    /**
     * This value contains the number of bytes used to store addresses in the file.
     * The values for the addresses of objects in the file are offsets relative
     * to a base address, usually the address of the superblock signature.
     * This allows a wrapper to be added after the file is created without
     * invalidating the internal offset locations.
     * <p>
     * This field is present in version 0+ of the superblock.
     */
    private int sizeOfOffsets;
    /**
     * This value contains the number of bytes used to store the size of an object.
     * <p>
     * This field is present in version 0+ of the superblock.
     */
    private int sizeOfLengths;
    /**
     * Each leaf node of a group B-tree will have at least this many entries but
     * not more than twice this many.
     * If a group has a single leaf node then it may have fewer entries.
     * <p>
     * This value must be greater than zero.
     * <p>
     * This field is present in version 0 and 1 of the superblock.
     */
    private int groupLeafNodeK;
    /**
     * Each internal node of a group B-tree will have at least this many entries
     * but not more than twice this many. If the group has only one internal node
     * then it might have fewer entries.
     * <p>
     * This value must be greater than zero.
     * <p>
     * This field is present in version 0 and 1 of the superblock.
     */
    public int groupInternalNodeK;
    /**
     * For superblock version 0,1,2:This field is unused and should be ignored.
     * <p>
     * For superblock version 3: This value contains flags to ensure file consistency
     * for file locking. Currently, the following bit flags are defined:
     * Bit 0 if set indicates that the file has been opened for write access.
     * Bit 1 is reserved for future use.
     * Bit 2 if set indicates that the file has been opened for single-writer/multiple-reader (SWMR) write access.
     * Bits 3-7 are reserved for future use.
     * <p>
     * Bit 0 should be set as the first action when a file has been opened for write access. Bit 2 should be set when a file has been opened for SWMR write access. These two bits should be cleared only as the final action when closing a file.
     * The size of this field has been reduced from 4 bytes in superblock format versions 0 and 1 to 1 byte.
     * <p>
     * This field is present in version 0+ of the superblock.
     */
    private int fileConsistencyFlags;
    /**
     * Each internal node of an indexed storage B-tree will have at least this
     * many entries but not more than twice this many. If the index storage B-tree
     * has only one internal node then it might have fewer entries.
     * <p>
     * This value must be greater than zero.
     * <p>
     * This field is present in version 1 of the superblock.
     */
    private int indexedStorageInternalNodeK;
    /**
     * This is the absolute file address of the first byte of the HDF5 data within the file.
     * The library currently constrains this value to be the absolute file address
     * of the superblock itself when creating new files; future versions of the
     * library may provide greater flexibility. When opening an existing file and
     * this address does not match the offset of the superblock, the library assumes
     * that the entire contents of the HDF5 file have been adjusted in the file and
     * adjusts the base address and end of file address to reflect their new positions
     * in the file. Unless otherwise noted, all other file addresses are relative to this base address.
     * <p>
     * This field is present in version 0+ of the superblock.
     */
    private long baseAddress;
    /**
     * The file’s free space is not persistent for version 0 and 1 of the superblock.
     * Currently this field always contains the undefined address.
     * <p>
     * This field is present in version 0 and 1 of the superblock.
     */
    private long globalFreeSpaceIndexAddress;
    /**
     * This is the absolute file address of the first byte past the end of all HDF5 data.
     * It is used to determine whether a file has been accidentally truncated and
     * as an address where file data allocation can occur if space from the free list is not used.
     * <p>
     * This field is present in version 0+ of the superblock.
     */
    private long endOfFileAddress;
    /**
     * This is the relative file address of the file driver information block
     * which contains driver-specific information needed to reopen the file.
     * If there is no driver information block then this entry should be the undefined address.
     * <p>
     * This field is present in version 0 and 1 of the superblock.
     */
    private long driverInformationBlockAddress;
    /**
     * This is the symbol table entry of the root group, which serves as the entry
     * point into the group graph for the file.
     * <p>
     * This field is present in version 0 and 1 of the superblock.
     */
    public SymbolTableEntry rootGroupSymbolTableEntry;
    /**
     * The field is the address of the object header for the superblock extension.
     * If there is no extension then this entry should be the undefined address.
     * <p>
     * This field is present in version 2 and 3 of the superblock.
     * <p>
     * The extension can be :
     * <ul>
     * <li>Shared Message Table message containing information to locate the
     * master table of shared object header message indices.</li>
     * <li>B-tree ‘K’ Values message containing non-default B-tree ‘K’ values.</li>
     * <li>Driver Info message containing information needed by the file driver
     * in order to reopen a file. See also the “Disk Format: Level 0B - File Driver Info” section above.</li>
     * <li>File Space Info message containing information about file space handling in the file.</li>
     * </ul>
     */
    private long superblockExtensionAddress;
    /**
     * This is the address of the root group object header, which serves as the
     * entry point into the group graph for the file.
     * <p>
     * This field is present in version 2 and 3 of the superblock.
     */
    private long rootGroupObjectHeaderAddress;

    /** Creates an empty superblock, filled by {@link #read(HDF5DataInput)}. */
    public SuperBlock() {
    }

    /**
     * Returns the number of bytes used to store the size of an object in the file.
     *
     * @return the number of bytes used to store the size of an object in the file
     */
    public int getSizeOfLengths() {
        return sizeOfLengths;
    }

    /**
     * Returns the number of bytes used to store addresses in the file.
     *
     * @return the number of bytes used to store addresses in the file
     */
    public int getSizeOfOffsets() {
        return sizeOfOffsets;
    }

    @Override
    public void read(HDF5DataInput channel) throws IOException {

        channel.ensureSignature(SIGNATURE);
        superblockVersion = channel.readUnsignedByte();
        if (superblockVersion == 0 || superblockVersion == 1) {
            freespaceVersion = channel.readUnsignedByte();
            rootGroupSymbolTableVersion = channel.readUnsignedByte();
            channel.skipFully(1); //reserved
            sharedHeaderMessageVersion = channel.readUnsignedByte();
            sizeOfOffsets = channel.readUnsignedByte();
            sizeOfLengths = channel.readUnsignedByte();
            channel.setOffsetSize(sizeOfOffsets);
            channel.setLengthSize(sizeOfLengths);
            channel.skipFully(1); //reserved
            groupLeafNodeK = channel.readUnsignedShort();
            groupInternalNodeK = channel.readUnsignedShort();
            fileConsistencyFlags = channel.readInt();
            if (superblockVersion == 1) {
                indexedStorageInternalNodeK = channel.readUnsignedShort();
                channel.skipFully(2);
            }
            baseAddress = channel.readOffset();
            globalFreeSpaceIndexAddress = channel.readOffset();
            endOfFileAddress = channel.readOffset();
            driverInformationBlockAddress = channel.readOffset();

            rootGroupSymbolTableEntry = new SymbolTableEntry();
            rootGroupSymbolTableEntry.read(channel);
        } else if (superblockVersion == 2 || superblockVersion == 3) {
            sizeOfOffsets = channel.readUnsignedByte();
            sizeOfLengths = channel.readUnsignedByte();
            channel.setOffsetSize(sizeOfOffsets);
            channel.setLengthSize(sizeOfLengths);
            fileConsistencyFlags = channel.readUnsignedByte();
            baseAddress = channel.readOffset();
            superblockExtensionAddress = channel.readOffset();
            endOfFileAddress = channel.readOffset();
            rootGroupObjectHeaderAddress = channel.readOffset();
            int checksum = channel.readInt();//ignored for now
        } else {
            throw new IOException("Unsupported superblock version " + superblockVersion);
        }

        /*
        Every address stored in the file is relative to the base address. This
        reader treats them as absolute, which is only correct when the base
        address is zero, that is when the file has no user block. Fail clearly
        rather than decode every structure at the wrong place.
        */
        if (baseAddress != 0) {
            throw new IOException("This file declares a base address of " + baseAddress
                    + ", meaning it has a user block. Such files are not supported yet.");
        }
    }

    /** Offset of the first byte a superblock may start at after offset zero. */
    private static final int FIRST_ALTERNATE_OFFSET = 512;

    /**
     * Look for the format signature at the offsets where a superblock may start.
     * <p>
     * A file may begin with a user block of unspecified content, so the signature
     * is searched at byte offset 0, then 512, then at each successive multiple of
     * two of the previous one. The user block size is a power of two, which is why
     * no other offset needs to be tried.
     * <p>
     * The buffer is read with absolute accesses, so its position is left untouched
     * and only the bytes it already holds are looked at. A file whose user block is
     * larger than that is reported as not being HDF5.
     *
     * @param buffer bytes from the beginning of the file, not {@code null}
     * @return offset of the signature, or -1 if the buffer holds no signature
     */
    public static long findSignatureOffset(final ByteBuffer buffer) {
        final int start = buffer.position();
        final int limit = buffer.limit();
        for (long offset = 0; ; offset = (offset == 0) ? FIRST_ALTERNATE_OFFSET : offset * 2) {
            final long position = start + offset;
            if (position + SIGNATURE.length > limit) {
                return -1;
            }
            int i = 0;
            while (i < SIGNATURE.length && buffer.get((int) position + i) == SIGNATURE[i]) {
                i++;
            }
            if (i == SIGNATURE.length) {
                return offset;
            }
        }
    }

    /**
     * Returns the version of this superblock, 0 to 3.
     *
     * @return the version of this superblock, 0 to 3
     */
    public int getVersion() {
        return superblockVersion;
    }

    /**
     * Returns the address of the root group object header.
     * Only meaningful for superblock versions 2 and 3, versions 0 and 1 use
     * {@link #rootGroupSymbolTableEntry} instead.
     *
     * @return the address of the root group object header
     * @see #resolveRootObjectHeaderAddress()
     */
    public long getRootGroupObjectHeaderAddress() {
        return rootGroupObjectHeaderAddress;
    }

    /**
     * Returns the address of the superblock extension object header, or the undefined address if none.
     * Only present in versions 2 and 3.
     *
     * @return the address of the superblock extension object header, or the undefined address if none
     */
    public long getSuperblockExtensionAddress() {
        return superblockExtensionAddress;
    }

    /**
     * Returns the address of the root group object header, whichever way this superblock version stores it.
     *
     * @return the address of the root group object header, whichever way this superblock version stores it
     * @throws IOException if this superblock is of version 0 or 1 and has no root group symbol table entry.
     */
    public long resolveRootObjectHeaderAddress() throws IOException {
        if (superblockVersion == 0 || superblockVersion == 1) {
            if (rootGroupSymbolTableEntry == null) {
                throw new IOException("Superblock version " + superblockVersion
                        + " has no root group symbol table entry.");
            }
            return rootGroupSymbolTableEntry.getObjectHeaderAddress();
        }
        return rootGroupObjectHeaderAddress;
    }

    @Override
    public String toString() {
        return IOStructure.reflectionToString(this);
    }
}
