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
package org.apache.sis.io.stream;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.io.IOException;
import org.apache.sis.math.NumberType;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.storage.DataStoreContentException;


/**
 * Helper methods for reading a rectangular area, a cube or a hyper-cube from a channel.
 * The data can be stored in an existing array, or a new array can be created.
 * This class does not handle compression; it is rather designed for efficient reading of uncompressed data.
 * It tries to read the largest possible contiguous blocks of data with single
 * {@link java.nio.channels.ReadableByteChannel#read(ByteBuffer)} and
 * {@link ByteBuffer#get(byte[], int, int)} method calls.
 *
 * <p>This reader supports subsampling in any dimension. However, subsampling in the first dimension
 * (the one with fastest varying index) is generally not efficient because it forces a large amount
 * of seek operations. This class makes no special case for making that specific subsampling faster.
 * It is generally not worth because subsampling in the first dimension is a special case anyway.
 * It is the "dimension" of bands in an image using the pixel interleaved sample model, so the caller
 * often needs to process subsampling in the first dimension in a different way than other dimensions
 * anyway.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class HyperRectangleReader {
    /**
     * The channel from which to read the values, together with a buffer for transferring data.
     */
    private final DataTransfer reader;

    /**
     * The {@code input} position of the first sample (ignoring sub-area and subsampling).
     * This is initially the {@code origin} argument given to the constructor, copied verbatim.
     *
     * @see #getOrigin()
     */
    private long origin;

    /**
     * Creates a new reader for the given input.
     *
     * @param  dataType  the type of elements to read.
     * @param  input     the channel from which to read the values, together with a buffer for transferring data.
     * @throws DataStoreContentException if the given {@code dataType} is not one of the supported values.
     */
    public HyperRectangleReader(final NumberType dataType, final ChannelDataInput input) throws DataStoreContentException {
        switch (dataType) {
            case BYTE:      reader = input.new BytesReader  ((byte[])   null); break;
            case CHARACTER: reader = input.new CharsReader  ((char[])   null); break;
            case SHORT:     reader = input.new ShortsReader ((short[])  null); break;
            case INTEGER:   reader = input.new IntsReader   ((int[])    null); break;
            case LONG:      reader = input.new LongsReader  ((long[])   null); break;
            case FLOAT:     reader = input.new FloatsReader ((float[])  null); break;
            case DOUBLE:    reader = input.new DoublesReader((double[]) null); break;
            default: throw new DataStoreContentException(Errors.format(Errors.Keys.UnknownType_1, dataType));
        }
        final ByteBuffer buffer = input.buffer;
        final int pos = buffer.position();
        final int lim = buffer.limit();
        try {
            buffer.position(0).limit(buffer.capacity());
            reader.createView();
        } finally {
            buffer.limit(lim).position(pos);
        }
    }

    /**
     * Creates a new reader for the data in an existing buffer.
     * The data will be read from the current buffer position to the buffer limit.
     *
     * @param  filename  a data source name, for error messages or debugging purpose.
     * @param  data      a buffer containing the data to read.
     * @throws IOException should never happen.
     */
    public HyperRectangleReader(final String filename, final Buffer data) throws IOException {
        reader = new MemoryDataTransfer(filename, data).reader();
    }

    /**
     * Returns a file identifier for error messages or debugging purpose.
     *
     * @return the file identifier.
     */
    public final String filename() {
        return reader.filename();
    }

    /**
     * Returns the number of bytes in each value to be read.
     *
     * @return number of bytes per value.
     */
    public final int sampleSize() {
        return 1 << reader.dataSizeShift();
    }

    /**
     * Returns the {@code input} position of the first sample (ignoring sub-area and subsampling).
     * Default value is 0.
     *
     * @return the {@code input} position of the first sample (ignoring sub-area and subsampling).
     */
    public final long getOrigin() {
        return origin;
    }

    /**
     * Sets the {@code input} position of the first sample (ignoring sub-area and subsampling).
     *
     * @param  p  the new {@code input} position of the first sample (ignoring sub-area and subsampling).
     */
    public final void setOrigin(final long p) {
        origin = p;
    }

    /**
     * Sets the destination where values will be stored.
     * It is caller's responsibility to ensure that the buffer has sufficient capacity.
     * If this method is not invoked, the destination array will be created automatically.
     *
     * <h4>Limitations</h4>
     * The current implementation accepts only buffer wrapping Java array starting at zero.
     * The buffer limit is ignored. Those limitations may be resolved in a future version.
     *
     * @param  dest  buffer wrapping the array where values will be stored.
     * @throws UnsupportedOperationException if the buffer is not backed by an accessible array or does not start at 0.
     * @throws ClassCastException if {@code array} is an array of the primitive type identified by {@code dataType}.
     */
    public final void setDestination(final Buffer dest) {
        if ((dest.arrayOffset() | dest.position()) != 0) {
            throw new UnsupportedOperationException();
        }
        reader.setDest(dest.array());
    }

    /**
     * Reads data in the given region. It is caller's responsibility to ensure that the {@code Region}
     * object has been created with a {@code size} argument equals to this hyper-rectangle size.
     *
     * @param  region  the sub-area to read and the subsampling to use.
     * @return the data in an array of primitive type.
     * @throws IOException if an error occurred while transferring data from the channel.
     * @throws ArithmeticException if the region to read is too large or too far from origin.
     */
    public final Object read(final Region region) throws IOException {
        return read(region, 0, false);
    }

    /**
     * Reads data in the given region as a buffer. This method performs the same work
     * than {@link #read(Region)} except that the array is wrapped in a heap buffer.
     * The {@code capacity} argument is the minimal length of the array to allocate.
     * The actual length of data read will be the {@linkplain Buffer#limit() limit}
     * of the returned buffer.
     *
     * @param  region    the sub-area to read and the subsampling to use.
     * @param  capacity  minimal length of the array to allocate, or 0 for automatic.
     * @return the data in a buffer backed by an array on the heap.
     * @throws IOException if an error occurred while transferring data from the channel.
     * @throws ArithmeticException if the region to read is too large or too far from origin.
     */
    public final Buffer readAsBuffer(final Region region, final int capacity) throws IOException {
        return (Buffer) read(region, capacity, true);
    }

    /**
     * Implementation of {@link #read(Region)} and {@link #readAsBuffer(Region)}.
     *
     * @param  region    the sub-area to read and the subsampling to use.
     * @param  capacity  minimal length of the array to allocate, or 0 for automatic.
     * @param  asBuffer  {@code true} for wrapping the array in a {@link Buffer}.
     * @return the data as an array or wrapped in a buffer, depending on {@code asBuffer} value.
     */
    private Object read(final Region region, final int capacity, final boolean asBuffer) throws IOException {
        final int contiguousDataDimension = region.contiguousDataDimension();
        final int contiguousDataLength = region.targetLength(contiguousDataDimension);
        final long[] strides = new long[region.getDimension() - contiguousDataDimension];
        final int[]   cursor = new int[strides.length];
        final int sampleSize = sampleSize();
        long  streamPosition = Math.addExact(origin, region.getStartByteOffset(sampleSize));
        int    arrayPosition = 0;
        for (int i=0; i<strides.length; i++) {
            strides[i] = region.stride(i + contiguousDataDimension, contiguousDataLength, sampleSize);
            assert (strides[i] > 0) : i;
        }
        final int limit = region.targetLength(region.getDimension());
        try {
            if (reader.dataArray() == null) {
                reader.createDataArray(Math.max(capacity, limit));
            }
            final Buffer view = reader.view();
loop:       do {
                reader.seek(streamPosition);
                assert reader.view() == view;
                reader.readFully(view, arrayPosition, contiguousDataLength);
                for (int i=0; i<cursor.length; i++) {
                    /*
                     * After we have read as much contiguous data as we can (may be a row, or a plane, or
                     * a cube, etc. depending if we have to skip values or not between rows/planes/cubes),
                     * search the highest dimension which is going to change (i.e. are we going to start a
                     * new row, or a new plane, or a new cube?). This determines how many bytes we have to
                     * skip.
                     */
                    if (++cursor[i] < region.getTargetSize(contiguousDataDimension + i)) {
                        streamPosition = Math.addExact(streamPosition, strides[i]);
                        arrayPosition  = Math.addExact(arrayPosition, contiguousDataLength);
                        continue loop;
                    }
                    cursor[i] = 0;
                }
                break;
            } while (true);
            return asBuffer ? reader.dataArrayAsBuffer().limit(limit) : reader.dataArray();
        } finally {
            reader.setDest(null);
        }
    }
}
