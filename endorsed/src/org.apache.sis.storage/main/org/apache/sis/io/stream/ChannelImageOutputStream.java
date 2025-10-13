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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageOutputStream;


/**
 * An {@code ImageOutputStream} backed by {@code ChannelDataInput} and {@code ChannelDataOutput}.
 * Contrarily to most other I/O frameworks in the standard JDK, {@code ImageOutputStream} is read/write.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("deprecation")
public class ChannelImageOutputStream extends OutputStream implements ImageOutputStream, Markable {
    /**
     * The object to use for reading from the channel.
     */
    private final ChannelImageInputStream input;

    /**
     * The object to use for writing to the channel.
     */
    private final ChannelDataOutput output;

    /**
     * {@code false} if the stream is reading, or {@code true} if it is writing.
     *
     * @see #current()
     */
    private boolean writing;

    /**
     * Creates a new input/output stream for the given channel and using the given buffer.
     *
     * @param  filename  a file identifier used only for formatting error message.
     * @param  channel   the channel where to read and write data.
     * @param  buffer    the buffer for data to read and write.
     * @throws IOException if the stream cannot be created.
     */
    public ChannelImageOutputStream(final String filename, final ByteChannel channel, final ByteBuffer buffer)
            throws IOException
    {
        input  = new ChannelImageInputStream(filename, channel, buffer, true);
        output = new ChannelDataOutput(filename, channel, buffer);
    }

    /**
     * Creates a new input/output stream wrapping the given output data channel.
     *
     * @param  output  the object to use for writing to the channel.
     * @throws ClassCastException if the output channel is not readable.
     * @throws IOException if the stream cannot be created.
     */
    public ChannelImageOutputStream(final ChannelDataOutput output) throws IOException {
        this.output = output;
        input = new ChannelImageInputStream(output.filename, (ReadableByteChannel) output.channel, output.buffer, true);
        writing = true;
    }

    /**
     * Returns the {@linkplain #input} or {@linkplain #output},
     * depending on whether this stream is reading or writing.
     */
    private ChannelData current() {
        return writing ? output : input;
    }

    /**
     * Returns the object to use for reading from the stream.
     * The returned object should not be used anymore after {@link #output()}
     * has been invoked, until {@code input()} is invoked again.
     *
     * @return helper object to use for reading from the stream.
     * @throws IOException if an error occurred while flushing a buffer.
     */
    public final ChannelImageInputStream input() throws IOException {
        if (writing) {
            output.yield(input);
            writing = false;
        }
        return input;
    }

    /**
     * Returns the object to use for writing to the stream.
     * The returned object should not be used anymore after {@link #input()}
     * has been invoked, until {@code output()} is invoked again.
     *
     * @return helper object to use for writing to the stream.
     * @throws IOException if an error occurred while flushing a buffer.
     */
    public final ChannelDataOutput output() throws IOException {
        if (!writing) {
            input.yield(output);
            writing = true;
        }
        return output;
    }

    /** Delegates to the reader or writer. */
    @Override public boolean   isCached()                                                {return input.isCached();}
    @Override public boolean   isCachedMemory()                                          {return input.isCachedMemory();}
    @Override public boolean   isCachedFile()                                            {return input.isCachedFile();}
    @Override public long      length()                               throws IOException {return current().length();}
    @Override public void      mark()                                                    {       current().mark();}
    @Override public void      reset()                                throws IOException {       current().reset();}
    @Override public void      reset(long p)                          throws IOException {       current().reset(p);}
    @Override public void      seek(long p)                           throws IOException {       current().seek(p);}
    @Override public void      flushBefore(long p)                    throws IOException {       current().flushBefore(p);}
    @Override public long      getFlushedPosition()                                      {return current().getFlushedPosition();}
    @Override public long      getStreamPosition()                                       {return current().getStreamPosition();}
    @Override public int       getBitOffset()                                            {return current().getBitOffset();}
    @Override public void      setBitOffset(int n)                                       {       current().setBitOffset(n);}
    @Override public ByteOrder getByteOrder()                                            {return input.getByteOrder();}
    @Override public void      setByteOrder(ByteOrder v)                                 {       input.setByteOrder(v);}
    @Override public void      readBytes(IIOByteBuffer v, int n)      throws IOException {       input().readBytes(v, n);}
    @Override public int       read()                                 throws IOException {return input().read();}
    @Override public int       readBit()                              throws IOException {return input().readBit();}
    @Override public long      readBits(int n)                        throws IOException {return input().readBits(n);}
    @Override public boolean   readBoolean()                          throws IOException {return input().readBoolean();}
    @Override public byte      readByte()                             throws IOException {return input().readByte();}
    @Override public int       readUnsignedByte()                     throws IOException {return input().readUnsignedByte();}
    @Override public short     readShort()                            throws IOException {return input().readShort();}
    @Override public int       readUnsignedShort()                    throws IOException {return input().readUnsignedShort();}
    @Override public char      readChar()                             throws IOException {return input().readChar();}
    @Override public int       readInt()                              throws IOException {return input().readInt();}
    @Override public long      readUnsignedInt()                      throws IOException {return input().readUnsignedInt();}
    @Override public long      readLong()                             throws IOException {return input().readLong();}
    @Override public float     readFloat()                            throws IOException {return input().readFloat();}
    @Override public double    readDouble()                           throws IOException {return input().readDouble();}
    @Override public String    readLine()                             throws IOException {return input().readLine();}
    @Override public String    readUTF()                              throws IOException {return input().readUTF();}
    @Override public int       read        (byte[]   v)               throws IOException {return input().read(v);}
    @Override public int       read        (byte[]   v, int s, int n) throws IOException {return input().read(v, s, n);}
    @Override public void      readFully   (byte[]   v)               throws IOException {       input().readFully(v);}
    @Override public void      readFully   (byte[]   v, int s, int n) throws IOException {       input().readFully(v, s, n);}
    @Override public void      readFully   (short[]  v, int s, int n) throws IOException {       input().readFully(v, s, n);}
    @Override public void      readFully   (char[]   v, int s, int n) throws IOException {       input().readFully(v, s, n);}
    @Override public void      readFully   (int[]    v, int s, int n) throws IOException {       input().readFully(v, s, n);}
    @Override public void      readFully   (long[]   v, int s, int n) throws IOException {       input().readFully(v, s, n);}
    @Override public void      readFully   (float[]  v, int s, int n) throws IOException {       input().readFully(v, s, n);}
    @Override public void      readFully   (double[] v, int s, int n) throws IOException {       input().readFully(v, s, n);}
    @Override public int       skipBytes   (int      n)               throws IOException {return input().skipBytes(n);}
    @Override public long      skipBytes   (long     n)               throws IOException {return input().skipBytes(n);}
    @Override public void      write       (int      v)               throws IOException {      output().write(v);}
    @Override public void      writeBit    (int      v)               throws IOException {      output().writeBit    (v);}
    @Override public void      writeBits   (long     v, int n)        throws IOException {      output().writeBits   (v, n);}
    @Override public void      writeBoolean(boolean  v)               throws IOException {      output().writeBoolean(v);}
    @Override public void      writeByte   (int      v)               throws IOException {      output().writeByte   (v);}
    @Override public void      writeShort  (int      v)               throws IOException {      output().writeShort  (v);}
    @Override public void      writeChar   (int      v)               throws IOException {      output().writeChar   (v);}
    @Override public void      writeInt    (int      v)               throws IOException {      output().writeInt    (v);}
    @Override public void      writeLong   (long     v)               throws IOException {      output().writeLong   (v);}
    @Override public void      writeFloat  (float    v)               throws IOException {      output().writeFloat  (v);}
    @Override public void      writeDouble (double   v)               throws IOException {      output().writeDouble (v);}
    @Override public void      writeBytes  (String   v)               throws IOException {      output().writeBytes  (v);}
    @Override public void      writeChars  (String   v)               throws IOException {      output().writeChars  (v);}
    @Override public void      writeUTF    (String   v)               throws IOException {      output().writeUTF    (v);}
    @Override public void      write       (byte[]   v)               throws IOException {      output().write(v);}
    @Override public void      write       (byte[]   v, int s, int n) throws IOException {      output().write(v, s, n);}
    @Override public void      writeShorts (short[]  v, int s, int n) throws IOException {      output().writeShorts (v, s, n);}
    @Override public void      writeChars  (char[]   v, int s, int n) throws IOException {      output().writeChars  (v, s, n);}
    @Override public void      writeInts   (int[]    v, int s, int n) throws IOException {      output().writeInts   (v, s, n);}
    @Override public void      writeLongs  (long[]   v, int s, int n) throws IOException {      output().writeLongs  (v, s, n);}
    @Override public void      writeFloats (float[]  v, int s, int n) throws IOException {      output().writeFloats (v, s, n);}
    @Override public void      writeDoubles(double[] v, int s, int n) throws IOException {      output().writeDoubles(v, s, n);}

    /**
     * Discards the initial position of the stream prior to the current stream position.
     * Note that this behavior is as specified by Image I/O, but different than the flush
     * method of {@link OutputStream}.
     *
     * @throws IOException if an I/O error occurred.
     */
    @Override
    public void flush() throws IOException {
        current().flushBefore(getStreamPosition());
    }

    /**
     * Closes the channel.
     *
     * @throws IOException if an error occurred while closing the channel.
     */
    @Override
    public void close() throws IOException {
        try (output.channel) {
            if (writing) {
                output.flush();
            }
        }
    }
}
