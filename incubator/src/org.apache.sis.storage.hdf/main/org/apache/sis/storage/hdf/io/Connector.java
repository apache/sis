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
package org.apache.sis.storage.hdf.io;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.hdf.base.SuperBlock;
import static org.apache.sis.storage.hdf.io.HDF5DataInput.UNDEFINED_1;
import static org.apache.sis.storage.hdf.io.HDF5DataInput.UNDEFINED_2;
import static org.apache.sis.storage.hdf.io.HDF5DataInput.UNDEFINED_4;
import static org.apache.sis.storage.hdf.io.HDF5DataInput.UNDEFINED_8;

/**
 * Access point to an HDF-5 file.
 * <p>
 * The connector reads the {@link SuperBlock} once at creation, then creates
 * new independent {@link HDF5DataInput} channels on the file on request,
 * configured with the offset and length sizes declared by the superblock.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Connector implements AutoCloseable {

    private final SuperBlock superblock;
    private final Path path;

    /**
     * Returns the HDF-5 file path.
     *
     * @return the HDF-5 file path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the superblock read at creation.
     *
     * @return the superblock read at creation
     */
    public SuperBlock getSuperblock() {
        return superblock;
    }

    /**
     * Opens the given file and reads its superblock.
     *
     * @param path HDF-5 file
     * @throws IllegalArgumentException if the path can not be opened as a channel.
     * @throws DataStoreException if the storage could not be opened.
     * @throws IOException if an error occurred while reading the superblock.
     */
    public Connector(Path path) throws IllegalArgumentException, DataStoreException, IOException {
        this.path = path;

        final StorageConnector cnx = new StorageConnector(path);
        final ChannelDataInput c = cnx.getStorageAs(ChannelDataInput.class);
        cnx.closeAllExcept(c);
        c.buffer.order(ByteOrder.LITTLE_ENDIAN);
        try (final HDF5DataInput channel = new HDF5ChannelDataInput(c)) {
            superblock = new SuperBlock();
            superblock.read(channel);
        }
    }

    /**
     * Creates a new channel on the file, configured with the superblock offset and length sizes.
     * The caller is responsible for closing it.
     *
     * @return a new channel positioned at the beginning of the file
     * @throws IllegalArgumentException if the path can not be opened as a channel.
     * @throws DataStoreException if the storage could not be opened.
     */
    public HDF5DataInput createChannel() throws IllegalArgumentException, DataStoreException {
        final StorageConnector cnx = new StorageConnector(path);
        final ChannelDataInput c = cnx.getStorageAs(ChannelDataInput.class);
        cnx.closeAllExcept(c);
        c.buffer.order(ByteOrder.LITTLE_ENDIAN);
        final HDF5DataInput channel = new HDF5ChannelDataInput(c);
        channel.setOffsetSize(superblock.getSizeOfOffsets());
        channel.setLengthSize(superblock.getSizeOfLengths());
        return channel;
    }

    @Override
    public void close() {
    }


    /**
     * Returns whether the given address is defined, for the superblock offset size.
     *
     * @return whether the given address is defined, for the superblock offset size
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param offset tested address
     */
    public boolean isDefinedOffset(long offset) {
        return !isUndefinedOffset(offset);
    }

    /**
     * Returns whether the given address has all bits set, for the superblock offset size.
     *
     * @return whether the given address has all bits set, for the superblock offset size
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param offset tested address
     */
    public boolean isUndefinedOffset(long offset) {
        return isUndefinedOffset(offset, superblock.getSizeOfOffsets());
    }

    /**
     * Returns whether the given length is defined, for the superblock length size.
     *
     * @return whether the given length is defined, for the superblock length size
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param length tested length
     */
    public boolean isDefinedLength(long length) {
        return !isUndefinedLength(length);
    }

    /**
     * Returns whether the given length has all bits set, for the superblock length size.
     *
     * @return whether the given length has all bits set, for the superblock length size
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param length tested length
     */
    public boolean isUndefinedLength(long length) {
        return isUndefinedLength(length, superblock.getSizeOfLengths());
    }

    /**
     * Returns whether the given address is defined, for the given offset size.
     *
     * @return whether the given address is defined, for the given offset size
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param offset tested address
     * @param offsetSize 1, 2, 4 or 8 bytes
     * @throws IllegalArgumentException if the offset size is not supported.
     */
    public static boolean isDefinedOffset(long offset, int offsetSize) {
        return !isUndefinedOffset(offset, offsetSize);
    }

    /**
     * Returns whether the given address has all bits set, for the given offset size.
     *
     * @return whether the given address has all bits set, for the given offset size
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param offset tested address
     * @param offsetSize 1, 2, 4 or 8 bytes
     * @throws IllegalArgumentException if the offset size is not supported.
     */
    public static boolean isUndefinedOffset(long offset, int offsetSize) {
        return switch (offsetSize) {
            case 1 -> (offset == UNDEFINED_1);
            case 2 -> (offset == UNDEFINED_2);
            case 4 -> (offset == UNDEFINED_4);
            case 8 -> (offset == UNDEFINED_8);
            default -> throw new IllegalArgumentException("Unsupported size " + offsetSize);
        };
    }

    /**
     * Returns whether the given length is defined, for the given length size.
     *
     * @return whether the given length is defined, for the given length size
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param length tested length
     * @param lengthSize 1, 2, 4 or 8 bytes
     * @throws IllegalArgumentException if the length size is not supported.
     */
    public static boolean isDefinedLength(long length, int lengthSize) {
        return !isUndefinedLength(length, lengthSize);
    }

    /**
     * Returns whether the given length has all bits set, for the given length size.
     *
     * @return whether the given length has all bits set, for the given length size
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param length tested length
     * @param lengthSize 1, 2, 4 or 8 bytes
     * @throws IllegalArgumentException if the length size is not supported.
     */
    public static boolean isUndefinedLength(long length, int lengthSize) {
        return switch (lengthSize) {
            case 1 -> (length == UNDEFINED_1);
            case 2 -> (length == UNDEFINED_2);
            case 4 -> (length == UNDEFINED_4);
            case 8 -> (length == UNDEFINED_8);
            default -> throw new IllegalArgumentException("Unsupported size " + lengthSize);
        };
    }
}
