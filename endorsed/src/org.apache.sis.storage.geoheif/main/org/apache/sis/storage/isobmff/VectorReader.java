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
package org.apache.sis.storage.isobmff;

import java.io.IOException;
import org.apache.sis.math.Vector;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreContentException;


/**
 * Helper class for reading and storing unsigned integers of a size (in bytes) that is known only at runtime.
 * <abbr>ISO</abbr> requires support of 0, 4 and 8 bytes, but this implementation supports also 1 and 2 bytes.
 *
 * @author Martin Desruisseaux (Geomatys)
 */
public abstract class VectorReader {
    /**
     * The vector where the result is stored.
     */
    public final Vector result;

    /**
     * Creates a new reader.
     *
     * @param  array  the array where the result will be stored.
     */
    VectorReader(final Object array) {
        result = Vector.create(array, true);
    }

    /**
     * Reads and store the next value.
     *
     * @param  input  the input stream from which to read one integer.
     * @param  index  index where to store the integer that has been read.
     */
    public abstract void read(ChannelDataInput input, int index) throws IOException;

    /**
     * Reads a single value from the input stream.
     * This is a shortcut for reading a vector of length 1.
     *
     * @param  input  the input stream from which to read one integer.
     * @param  size   size in bytes of the integer to read. Should be 0, 1, 2, 4 or 8.
     * @return the integer read from the stream, or 0 if the specified size is 0.
     * @throws IOException if an error occurred while reading from the stream.
     */
    public static long readSingle(ChannelDataInput input, final int size) throws IOException, DataStoreContentException {
        switch (size) {
            case 0:             return 0;
            case Byte.BYTES:    return input.readUnsignedByte();
            case Short.BYTES:   return input.readUnsignedShort();
            case Integer.BYTES: return input.readUnsignedInt();
            case Long.BYTES:    return input.readLong();
            default: {
                long value = input.readBits(size);
                input.skipRemainingBits();
                return value;
            }
        }
    }

    /**
     * Creates a new reader for a sequence of bytes, shorts, integers or long integers.
     * A size of 0 means that no value will be read.
     *
     * @param  size   size in bytes of the integers to read. Should be 0, 1, 2, 4 or 8.
     * @param  count  number of integer values to read.
     * @return a reader for integers of the given size, or {@code null} if the given size is zero.
     */
    public static VectorReader create(final int size, final int count) throws DataStoreContentException {
        switch (size) {
            case 0: {
                return null;
            }
            case Byte.BYTES: {      // Not supported by the ISO standard, but defined as a matter of principle.
                final byte[] array = new byte[count];
                return new VectorReader(array) {
                    @Override public void read(ChannelDataInput input, int index) throws IOException {
                        array[index] = input.readByte();
                    }
                };
            }
            case Short.BYTES: {     // Not supported by the ISO standard, but defined as a matter of principle.
                final short[] array = new short[count];
                return new VectorReader(array) {
                    @Override public void read(ChannelDataInput input, int index) throws IOException {
                        array[index] = input.readShort();
                    }
                };
            }
            case Integer.BYTES: {
                final int[] array = new int[count];
                return new VectorReader(array) {
                    @Override public void read(ChannelDataInput input, int index) throws IOException {
                        array[index] = input.readInt();
                    }
                };
            }
            case Long.BYTES: {
                final long[] array = new long[count];
                return new VectorReader(array) {
                    @Override public void read(ChannelDataInput input, int index) throws IOException {
                        array[index] = input.readLong();
                    }
                };
            }
            default: {
                final long[] array = new long[count];
                return new VectorReader(array) {
                    @Override public void read(ChannelDataInput input, int index) throws IOException {
                        array[index] = input.readBits(size);
                        input.skipRemainingBits();
                    }
                };
            }
        }
    }
}
