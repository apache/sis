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

import java.io.EOFException;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ReadableByteChannel;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;


/**
 * A {@code DataTransfer} with all data in the given buffer, without channel.
 *
 * <h4>Implementation note</h4>
 * This class implements also an empty {@link ReadableByteChannel} as safety. When using {@link ChannelDataInput}
 * without channel, only an existing {@code Buffer} pre-filled with the data should be used. If we have a bug in
 * our reading process, the empty channel will cause an {@link java.io.EOFException} to be thrown instead of a
 * {@link NullPointerException}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class MemoryDataTransfer extends DataTransfer implements ReadableByteChannel {
    /**
     * The actual {@code DataTransfer} implementation.
     */
    private final ChannelDataInput.ArrayReader reader;

    /**
     * Creates a in-memory data input for the given buffer.
     */
    MemoryDataTransfer(final String filename, final Buffer data) throws IOException {
        final ChannelDataInput input = new ChannelDataInput(filename, this,
                (data instanceof   ByteBuffer) ? (ByteBuffer) data : null, true);
             if (data instanceof   ByteBuffer) reader = input.new BytesReader  (               null);
        else if (data instanceof  ShortBuffer) reader = input.new ShortsReader ( (ShortBuffer) data);
        else if (data instanceof    IntBuffer) reader = input.new IntsReader   (   (IntBuffer) data);
        else if (data instanceof   LongBuffer) reader = input.new LongsReader  (  (LongBuffer) data);
        else if (data instanceof  FloatBuffer) reader = input.new FloatsReader ( (FloatBuffer) data);
        else if (data instanceof DoubleBuffer) reader = input.new DoublesReader((DoubleBuffer) data);
        else if (data instanceof   CharBuffer) reader = input.new CharsReader  (  (CharBuffer) data);
        else throw new IllegalArgumentException(Errors.format(Errors.Keys.UnknownType_1, Classes.getClass(data)));
    }

    /**
     * Returns the most efficient {@code DataTransfer} instance to use.
     */
    DataTransfer reader() {
        return (view() instanceof ByteBuffer) ? reader : this;
    }

    /**
     * Moves to the given byte position in the buffer.
     */
    @Override
    void seek(long position) throws IOException {
        final int dataSizeShift = dataSizeShift();
        if (position < 0 || (position & ((1 << dataSizeShift) - 1)) != 0) {
            throw new IOException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "position", position));
        }
        position >>>= dataSizeShift;
        final Buffer data = view();
        if (position > data.limit()) {
            throw new EOFException(Errors.format(Errors.Keys.UnexpectedEndOfFile_1, filename()));
        }
        data.position((int) position);
    }

    /**
     * Delegates to the actual implementation.
     */
    @Override String filename()                  {return reader.filename();}
    @Override int    dataSizeShift()             {return reader.dataSizeShift();}
    @Override Object dataArray()                 {return reader.dataArray();}
    @Override Buffer dataArrayAsBuffer()         {return reader.dataArrayAsBuffer();}
    @Override Buffer view()                      {return reader.view();}
    @Override Buffer createView()                {return reader.createView();}
    @Override void   createDataArray(int length) {reader.createDataArray(length);}
    @Override void   setDest(Object array)       {reader.setDest(array);}

    /**
     * Reads {@code length} values from the buffer and stores them into the array known to subclass,
     * starting at index {@code offset}.
     *
     * @param  view    ignored.
     * @param  offset  the starting position within {@code dest} to write.
     * @param  length  the number of values to read.
     */
    @Override
    void readFully(final Buffer view, final int offset, final int length) {
        reader.transfer(offset, length);
    }

    /**
     * Returns -1 since an empty channel has reached the end-of-stream.
     */
    @Override
    public int read(final ByteBuffer dst) {
        return -1;
    }

    /**
     * The channel is always open.
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    /**
     * Does nothing - keep the channel open.
     */
    @Override
    public void close() throws IOException {
    }
}
