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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.apache.sis.storage.hdf.heap.GlobalHeap;
import org.apache.sis.storage.hdf.heap.GlobalHeapId;
import org.apache.sis.storage.hdf.heap.GlobalHeapObject;

/**
 * Binary input used to decode HDF-5 structures.
 * <p>
 * In addition to primitive reads (using the current byte order), this input knows
 * the offset and length sizes declared in the {@link org.apache.sis.storage.hdf.base.SuperBlock},
 * provides alignment and signature/version checks, and gives access to the global heap.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface HDF5DataInput extends AutoCloseable {

    /** Undefined address or length encoded on 1 byte (all bits set). */
    final long UNDEFINED_1 = 0xFFl;
    /** Undefined address or length encoded on 2 bytes (all bits set). */
    final long UNDEFINED_2 = 0xFFFFl;
    /** Undefined address or length encoded on 4 bytes (all bits set). */
    final long UNDEFINED_4 = 0xFFFFFFFFl;
    /** Undefined address or length encoded on 8 bytes (all bits set). */
    final long UNDEFINED_8 = 0xFFFFFFFFFFFFFFFFl;

    /**
     * Moves the stream position forward.
     *
     * @param nb number of bytes to skip
     * @throws IOException if an I/O error occurred.
     */
    void skipFully(long nb) throws IOException;

    /**
     * Returns the next unsigned byte, from 0 to 255.
     *
     * @return the next unsigned byte, from 0 to 255
     * @throws IOException if an I/O error occurred.
     */
    int readUnsignedByte() throws IOException;

    /**
     * Returns the next unsigned 2 bytes value, from 0 to 65535.
     *
     * @return the next unsigned 2 bytes value, from 0 to 65535
     * @throws IOException if an I/O error occurred.
     */
    int readUnsignedShort() throws IOException;

    /**
     * Returns the next signed byte.
     *
     * @return the next signed byte
     * @throws IOException if an I/O error occurred.
     */
    byte readByte() throws IOException;

    /**
     * Returns the next signed 2 bytes value, widened to an int.
     *
     * @return the next signed 2 bytes value, widened to an int
     * @throws IOException if an I/O error occurred.
     */
    int readShort() throws IOException;

    /**
     * Returns the next signed 4 bytes value.
     *
     * @return the next signed 4 bytes value
     * @throws IOException if an I/O error occurred.
     */
    int readInt() throws IOException;

    /**
     * Returns the next unsigned 4 bytes value, from 0 to 2<sup>32</sup>-1.
     *
     * @return the next unsigned 4 bytes value, from 0 to 2<sup>32</sup>-1
     * @throws IOException if an I/O error occurred.
     */
    long readUnsignedInt() throws IOException;

    /**
     * Returns the next IEEE 754 double precision value.
     *
     * @return the next IEEE 754 double precision value
     * @throws IOException if an I/O error occurred.
     */
    double readDouble() throws IOException;

    /**
     * Returns the next IEEE 754 single precision value.
     *
     * @return the next IEEE 754 single precision value
     * @throws IOException if an I/O error occurred.
     */
    float readFloat() throws IOException;

    /**
     * Returns a new array of the next {@code nb} bytes.
     *
     * @param nb number of bytes to read
     * @return a new array of the next {@code nb} bytes
     * @throws IOException if an I/O error occurred.
     */
    byte[] readNBytes(int nb) throws IOException;

    /**
     * Returns a new array of the next {@code nb} signed 4 bytes integers.
     *
     * @param nb number of integers to read
     * @return a new array of the next {@code nb} signed 4 bytes integers
     * @throws IOException if an I/O error occurred.
     */
    int[] readInts(int nb) throws IOException;

    /**
     * Returns the next signed 8 bytes value.
     *
     * @return the next signed 8 bytes value
     * @throws IOException if an I/O error occurred.
     */
    long readLong() throws IOException;

    /**
     * Pushes the current position on the stack of marks, to be restored by {@link #reset()}.
     *
     * @throws IOException if an I/O error occurred.
     */
    void mark() throws IOException;

    /**
     * Moves back to the position of the last {@link #mark()} and removes that mark.
     *
     * @throws IOException if there is no mark or if an error occurred while seeking.
     */
    void reset() throws IOException;

    /**
     * Returns the current byte position in the stream.
     *
     * @return the current byte position in the stream
     * @throws IOException if an I/O error occurred.
     */
    long getStreamPosition() throws IOException;

    /**
     * Moves the stream to the given position.
     *
     * @param position byte position
     * @throws IOException if an I/O error occurred.
     */
    void seek(long position) throws IOException;

    /**
     * Returns the byte order of multi-bytes values, little endian by default for HDF-5 metadata.
     *
     * @return the byte order of multi-bytes values, little endian by default for HDF-5 metadata
     * @throws IOException if an error occurred while accessing the input.
     */
    ByteOrder order() throws IOException;

    /**
     * Sets the byte order of multi-bytes values.
     *
     * @param order new byte order
     * @throws IOException if an error occurred while accessing the input.
     */
    void order(ByteOrder order) throws IOException;

    /**
     * Returns the next {@code nb} bits from the current bit position, as an unsigned value.
     *
     * @param nb number of bits to read, from 0 to 64
     * @return the next {@code nb} bits from the current bit position, as an unsigned value
     * @throws IOException if an I/O error occurred.
     */
    long readBits(int nb) throws IOException;

    /**
     * Skips the remaining bits of the current byte, so the next read starts on a byte boundary.
     *
     * @throws IOException if an I/O error occurred.
     */
    void skipRemainingBits() throws IOException;

    /**
     * Read an unsigned int of variable byte length.
     *
     * @param nbBytes number of bytes to read
     * @return the value
     * @throws IOException if an error occurred while reading the file,
     *         or if the value does not fit in a Java int.
     * @deprecated only supports 1, 2 and 4 bytes and returns a signed value on
     *             4 bytes. Use {@link #readUnsignedInteger(int)} instead.
     */
    @Deprecated
    int readUnsignedInt(int nbBytes) throws IOException;

    /**
     * Closes the underlying channel, if any.
     *
     * @throws IOException if an I/O error occurred.
     */
    void close() throws IOException;

    /**
     * Read an unsigned little-endian integer of {@code nbBytes} bytes.
     * <p>
     * Unlike {@link #readUnsignedInt(int)} this method supports any width from
     * 1 to 8 bytes. Widths of 3, 5, 6 and 7 bytes are common in the HDF5 format :
     * fractal heap offsets are {@code ceil(maximumHeapSize/8)} bytes wide and
     * version 2 B-tree record counts use the minimum number of bytes needed.
     *
     * @param nbBytes number of bytes to read, from 1 to 8 inclusive
     * @return the value, always positive except for a 8 bytes value with the
     *         highest bit set, which cannot be represented in a Java long.
     * @throws IOException if {@code nbBytes} is out of range or if an error
     *         occurred while reading the file.
     */
    default long readUnsignedInteger(int nbBytes) throws IOException {
        if (nbBytes < 1 || nbBytes > 8) {
            throw new IOException("Unsupported unsigned integer size " + nbBytes + ", expected 1 to 8");
        }
        long value = 0;
        for (int i = 0; i < nbBytes; i++) {
            value |= ((long) readUnsignedByte()) << (8 * i);
        }
        return value;
    }

    /**
     * Create a channel reading the given in-memory bytes, inheriting this channel
     * byte order, offset size and length size, and delegating global heap access
     * back to this channel.
     * <p>
     * This is used to decode objects extracted from a fractal heap : the heap
     * returns the raw bytes of a serialized message, which are then parsed by the
     * regular message readers. Global heap access must be delegated because a
     * variable length value stored in such a message refers to a global heap
     * collection located elsewhere in the <em>file</em>, which an in-memory
     * channel cannot seek to.
     *
     * @param datas bytes to read, not null
     * @return a new channel positioned at the beginning of the given bytes
     * @throws IOException if the channel could not be created.
     */
    HDF5DataInput subChannel(byte[] datas) throws IOException;

    /**
     * The channel reading the file itself.
     * <p>
     * For a channel created by {@link #subChannel(byte[])} this is the channel it
     * was created from, otherwise it is this channel. Positions reported by the
     * global heap are positions in the file, so they must be seeked on the channel
     * returned here rather than on an in-memory sub channel.
     *
     * @return the channel reading the file, never null
     */
    default HDF5DataInput getFileChannel() {
        return this;
    }

    /**
     * Number of bytes used by the HDF5 library to encode a value in the range
     * {@code [0 .. maxValue]}, following {@code H5VM_limit_enc_size}.
     * <p>
     * Note this is {@code floor(log2(n))/8 + 1} and <strong>not</strong>
     * {@code ceil((floor(log2(n))+1)/8)} : the two differ on exact powers of two,
     * where the latter is one byte short.
     *
     * @param maxValue maximum value to encode
     * @return number of bytes, at least 1
     */
    static int limitEncodedSize(long maxValue) {
        return log2Floor(maxValue) / 8 + 1;
    }

    /**
     * Returns {@code floor(log2(value))}, or 0 if {@code value <= 1}.
     * Follows {@code H5VM_log2_gen}.
     *
     * @param value value for which to compute the logarithm
     * @return {@code floor(log2(value))}, or 0 if {@code value <= 1}
     */
    static int log2Floor(long value) {
        if (value <= 1) return 0;
        return 63 - Long.numberOfLeadingZeros(value);
    }

    ////////////////////////////////////////////////////////////////////////////
    // specific to HDF format //////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Skips bytes until the stream position is a multiple of {@code padding}.
     *
     * @param padding padding size
     * @throws IOException if an I/O error occurred.
     */
    default void realign(int padding) throws IOException {
        if (padding > 1) {
            final long position = getStreamPosition();
            final long r = position % padding;
            if (r == 0) return;
            skipFully((int) (padding - r));
        }
    }

    /**
     * Skips bytes until the distance from {@code start} is a multiple of {@code padding}.
     *
     * @param start position from where to start padding.
     * @param padding padding size
     * @throws IOException if an I/O error occurred.
     */
    default void realign(long start, int padding) throws IOException {
        if (padding > 1) {
            final long position = getStreamPosition() - start;
            final long r = position % padding;
            if (r == 0) return;
            skipFully((int) (padding - r));
        }
    }

    /**
     * Reads the upcoming bytes and checks they match the given signature.
     *
     * @param signature expected bytes, usually a 4 characters ASCII block signature
     * @throws IOException if the bytes do not match, or if an I/O error occurred.
     */
    default void ensureSignature(byte[] signature) throws IOException {
        final byte[] array = readNBytes(signature.length);
        if (!Arrays.equals(array, signature)) {
            throw new IOException("Incorrect block signature, expected " + new String(signature) +" but was " + new String(array));
        }
    }

    /**
     * Reads the upcoming unsigned byte version and checks it is one of the expected ones.
     *
     * @param expectedVersions accepted version numbers
     * @return the version read
     * @throws IOException if the version is not expected, or if an I/O error occurred.
     */
    default int ensureVersion(int ... expectedVersions) throws IOException {
        final int version = readUnsignedByte();
        for (int expectedVersion : expectedVersions) {
            if (version == expectedVersion) return version;
        }
        throw new IOException("Incorrect version, expected " + Arrays.toString(expectedVersions) +" but was " + version);
    }

    /**
     * Returns the global heap collection at the given file address, read once and cached.
     *
     * @param address file address of the collection
     * @return the global heap collection at the given file address, read once and cached
     * @throws IOException if an I/O error occurred.
     */
    GlobalHeap getGlobalHeap(long address) throws IOException;

    /**
     * Returns the global heap object referenced by the given identifier.
     *
     * @param id collection address and object index
     * @return the global heap object referenced by the given identifier
     * @throws IOException if an I/O error occurred.
     */
    GlobalHeapObject getGlobalHeapObject(GlobalHeapId id) throws IOException;

    /**
     * Returns the next unsigned little-endian 3 bytes value, from 0 to 2<sup>24</sup>-1.
     *
     * @return the next unsigned little-endian 3 bytes value, from 0 to 2<sup>24</sup>-1
     * @throws IOException if an I/O error occurred.
     */
    default int readUnsignedInt24() throws IOException {
        final int b0 = readUnsignedByte();
        final int b1 = readUnsignedByte();
        final int b2 = readUnsignedByte();
        return b0 | (b1<<8) | (b2<<16);
    }

    /**
     * Read a null terminated string.
     * @param padding string may be padded with multiple null, indicate padding here.
     * @param charset character encoding
     * @return result string
     * @throws IOException if an I/O error occurred.
     */
    default String readNullTerminatedString(int padding, Charset charset) throws IOException {
        return readNullTerminatedString(padding, Integer.MAX_VALUE, charset);
    }

    /**
     * Read a null terminated string.
     * @param padding string may be padded with multiple null, indicate padding here.
     * @param maxSize maximum length of the String, will stop reading at this index even if a null has not been found.
     * @param charset character encoding
     * @return result string
     * @throws IOException if an I/O error occurred.
     */
    default String readNullTerminatedString(int padding, int maxSize, Charset charset) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < maxSize; i++) {
            byte b = readByte();
            if (b == 0) break;
            out.write(b);
        }
        if (padding > 1) {
            final int r = (out.size()+1) % padding; //+1 for the end null
            if (r > 0) {
                final int remaining = padding - r;
                skipFully(remaining);
            }
        }
        return new String(out.toByteArray(), charset);
    }

    /**
     * Sets the number of bytes of an address, as declared in the superblock.
     *
     * @param offsetSize 1, 2, 4 or 8
     */
    void setOffsetSize(int offsetSize);

    /**
     * Returns the number of bytes of an address.
     *
     * @return the number of bytes of an address
     */
    int getOffsetSize();

    /**
     * Sets the number of bytes of an object size, as declared in the superblock.
     *
     * @param lengthSize 1, 2, 4 or 8
     */
    void setLengthSize(int lengthSize);

    /**
     * Returns the number of bytes of an object size.
     *
     * @return the number of bytes of an object size
     */
    int getLengthSize();

    /**
     * Returns the next address, encoded on {@link #getOffsetSize()} bytes; may be the undefined address.
     *
     * @return the next address, encoded on {@link #getOffsetSize()} bytes; may be the undefined address
     * @throws IOException if the offset size is unsupported or if an I/O error occurred.
     * @see #isUndefinedOffset(long)
     */
    long readOffset() throws IOException;

    /**
     * Returns the next length, encoded on {@link #getLengthSize()} bytes; may be the undefined length.
     *
     * @return the next length, encoded on {@link #getLengthSize()} bytes; may be the undefined length
     * @throws IOException if the length size is unsupported or if an I/O error occurred.
     * @see #isUndefinedLength(long)
     */
    long readLength() throws IOException;

    /**
     * Returns whether the given address is not the undefined one.
     *
     * @return whether the given address is not the undefined one
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param offset tested address
     */
    default boolean isDefinedOffset(long offset) {
        return !isUndefinedOffset(offset);
    }

    /**
     * Returns whether the given address has all bits set for the current offset size.
     *
     * @return whether the given address has all bits set for the current offset size
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param offset tested address
     */
    default boolean isUndefinedOffset(long offset) {
        return Connector.isUndefinedOffset(offset, getOffsetSize());
    }

    /**
     * Returns whether the given length is not the undefined one.
     *
     * @return whether the given length is not the undefined one
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param length tested length
     */
    default boolean isDefinedLength(long length) {
        return !isUndefinedLength(length);
    }

    /**
     * Returns whether the given length has all bits set for the current length size.
     *
     * @return whether the given length has all bits set for the current length size
     * @see <a href="https://support.hdfgroup.org/documentation/hdf5/latest/_f_m_t3.html#sec_fmt3_appendixa">V. Appendix A: Definitions</a>
     * @param length tested length
     */
    default boolean isUndefinedLength(long length) {
        return Connector.isUndefinedLength(length, getLengthSize());
    }

}
