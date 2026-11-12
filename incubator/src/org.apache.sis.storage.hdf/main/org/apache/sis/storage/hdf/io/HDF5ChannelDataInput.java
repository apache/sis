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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.storage.hdf.heap.GlobalHeap;
import org.apache.sis.storage.hdf.heap.GlobalHeapId;
import org.apache.sis.storage.hdf.heap.GlobalHeapObject;

/**
 * {@link HDF5DataInput} implementation backed by an Apache SIS {@link ChannelDataInput}.
 * <p>
 * The channel either reads the HDF-5 file itself, or an in-memory buffer
 * (see {@link HDF5DataInput#subChannel(byte[])}) in which case global heap
 * lookups are delegated to the enclosing file channel.
 * Global heap collections read from the file are cached by address.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class HDF5ChannelDataInput implements HDF5DataInput {

    private final ChannelDataInput input;
    private int offsetSize;
    private int lengthSize;

    /**
     * When this channel reads an in-memory buffer extracted from a heap, the
     * enclosing file channel, to which global heap lookups are delegated.
     * Null when this channel reads the file itself.
     *
     * @see HDF5DataInput#subChannel(byte[])
     */
    private final HDF5DataInput parent;

    private final Map<Long,GlobalHeap> globalheaps = new HashMap<>();

    /**
     * Create a channel over in-memory bytes.
     *
     * @param datas bytes to read, not null
     * @param parent enclosing file channel to which global heap lookups are
     *        delegated and from which the offset and length sizes are inherited,
     *        or null.
     */
    public HDF5ChannelDataInput(byte[] datas, HDF5DataInput parent) {
        ArgumentChecks.ensureNonNull("datas", datas);
        this.input = new ChannelDataInput("", ByteBuffer.wrap(datas));
        //HDF5 metadata is always little endian
        this.input.buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.parent = parent;
        if (parent != null) {
            this.offsetSize = parent.getOffsetSize();
            this.lengthSize = parent.getLengthSize();
        }
    }

    /**
     * Create a channel reading the file through the given input.
     * The offset and length sizes must be set afterward.
     *
     * @param channel input on the HDF-5 file
     */
    public HDF5ChannelDataInput(ChannelDataInput channel) {
        ArgumentChecks.ensureNonNull("input", channel);
        this.input = channel;
        this.parent = null;
    }

    @Override
    public HDF5DataInput subChannel(byte[] datas) throws IOException {
        return new HDF5ChannelDataInput(datas, this);
    }

    @Override
    public HDF5DataInput getFileChannel() {
        return parent == null ? this : parent.getFileChannel();
    }

    @Override
    public synchronized GlobalHeap getGlobalHeap(long address) throws IOException {
        if (parent != null) {
            //global heap collections live in the file, not in this buffer
            return parent.getGlobalHeap(address);
        }
        GlobalHeap heap = globalheaps.get(address);
        if (heap != null) return heap;
        heap = new GlobalHeap();
        input.mark();
        input.seek(address);
        heap.read(this);
        input.reset();
        globalheaps.put(address, heap);
        return heap;
    }

    @Override
    public GlobalHeapObject getGlobalHeapObject(GlobalHeapId id) throws IOException {
        final GlobalHeap globalHeap = getGlobalHeap(id.collectionAddress);
        return globalHeap.getHeapObject(id.objectIndex);
    }

    @Override
    public void skipFully(long nb) throws IOException {
        input.seek(input.getStreamPosition() + nb);
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return input.readUnsignedByte();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return input.readUnsignedShort();
    }

    @Override
    public byte readByte() throws IOException {
        return input.readByte();
    }

    @Override
    public int readShort() throws IOException {
        return input.readShort();
    }

    @Override
    public int readInt() throws IOException {
        return input.readInt();
    }

    @Override
    public long readUnsignedInt() throws IOException {
        return input.readUnsignedInt();
    }

    @Override
    public double readDouble() throws IOException {
        return input.readDouble();
    }

    @Override
    public float readFloat() throws IOException {
        return input.readFloat();
    }

    @Override
    public byte[] readNBytes(int nb) throws IOException {
        return input.readBytes(nb);
    }

    @Override
    public int[] readInts(int nb) throws IOException {
        return input.readInts(nb);
    }

    @Override
    public long readLong() throws IOException {
        return input.readLong();
    }

    @Override
    public void setOffsetSize(int offsetSize) {
        this.offsetSize = offsetSize;
    }

    @Override
    public int getOffsetSize() {
        return offsetSize;
    }

    @Override
    public void setLengthSize(int lengthSize) {
        this.lengthSize = lengthSize;
    }

    @Override
    public int getLengthSize() {
        return lengthSize;
    }

    @Override
    public long readOffset() throws IOException {
        switch (offsetSize) {
            case 1 : return input.readUnsignedByte();
            case 2 : return input.readUnsignedShort();
            case 4 : return input.readUnsignedInt();
            case 8 : return input.readLong();
            default: throw new IOException("Incorrect offset size " + offsetSize);
        }
    }

    @Override
    public long readLength() throws IOException {
        switch (lengthSize) {
            case 1 : return input.readUnsignedByte();
            case 2 : return input.readUnsignedShort();
            case 4 : return input.readUnsignedInt();
            case 8 : return input.readLong();
            default: throw new IOException("Incorrect offset size " + offsetSize);
        }
    }

    @Override
    public void mark() throws IOException {
        input.mark();
    }

    @Override
    public void reset() throws IOException {
        input.reset();
    }

    @Override
    public long getStreamPosition() throws IOException {
        return input.getStreamPosition();
    }

    @Override
    public void seek(long position) throws IOException {
        input.seek(position);
    }

    @Override
    public ByteOrder order() throws IOException {
        return input.buffer.order();
    }

    @Override
    public void order(ByteOrder order) throws IOException {
        input.buffer.order(order);
    }

    @Override
    public long readBits(int nb) throws IOException {
        return input.readBits(nb);
    }

    @Override
    public void skipRemainingBits() throws IOException {
        input.skipRemainingBits();
    }

    /**
     * Delegates to {@link #readUnsignedInteger(int)} and fails if the value does not fit in a Java int.
     *
     * @deprecated Use {@link #readUnsignedInteger(int)} instead.
     */
    @Override
    @Deprecated
    public int readUnsignedInt(int nbBytes) throws IOException {
        final long value = readUnsignedInteger(nbBytes);
        if (value < 0 || value > Integer.MAX_VALUE) {
            throw new IOException("Unsigned integer " + Long.toUnsignedString(value)
                    + " read on " + nbBytes + " bytes does not fit in a Java int");
        }
        return (int) value;
    }

    @Override
    public void close() throws IOException {
        if (input.channel != null) {
            input.channel.close();
        }
    }
}
