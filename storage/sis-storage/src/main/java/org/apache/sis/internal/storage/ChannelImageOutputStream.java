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
package org.apache.sis.internal.storage;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import javax.imageio.stream.ImageOutputStream;
import org.apache.sis.util.resources.Errors;


/**
 * Adds the missing methods in {@code ChannelDataOutput} for implementing the {@code DataOutput} interface.
 * Current implementation does not yet implements the {@code ImageOutputStream} sub-interface, but a future
 * implementation may do so.
 *
 * <p>We do not implement {@link ImageOutputStream} yet because the later inherits all read operations from
 * {@code ImageInputStream}, while the {@code org.apache.sis.internal.storage} package keeps the concerns
 * separated. Despite that, the name of this {@code ChannelImageOutputStream} anticipates a future version
 * which would implement the image I/O interface.</p>
 *
 * <p>{@code DataOutput} methods are defined in this separated class rather than in the parent class
 * because some methods are Java 1.0 legacy and should be avoided (e.g. {@link #writeBytes(String)}).</p>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class ChannelImageOutputStream extends ChannelDataOutput implements DataOutput, Closeable {
    /**
     * Creates a new output stream for the given channel and using the given buffer.
     *
     * @param  filename A file identifier used only for formatting error message.
     * @param  channel  The channel where to write data.
     * @param  buffer   The buffer from where to read the data.
     * @throws IOException If an error occurred while writing into channel.
     */
    public ChannelImageOutputStream(final String filename, final WritableByteChannel channel, final ByteBuffer buffer)
            throws IOException
    {
        super(filename, channel, buffer);
    }

    /**
     * Creates a new output source from the given {@code ChannelDataOutput}.
     * This constructor is invoked when we need to change the implementation class
     * from {@code ChannelDataOutput} to {@code ChannelImageOutputStream}.
     *
     * @param  output The existing instance from which to takes the channel and buffer.
     * @throws IOException If an error occurred while writing into channel.
     */
    public ChannelImageOutputStream(final ChannelDataOutput output) throws IOException {
        super(output.filename, output.channel, output.buffer);
    }

    /**
     * Writes a single byte to the stream at the current position.
     * The 24 high-order bits of {@code v} are ignored.
     *
     * @param  v an integer whose lower 8 bits are to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    @Override
    public final void write(final int v) throws IOException {
        writeByte(v);
    }

    /**
     * Writes boolean value (8 bits) into the steam. This method delegates to {@linkplain #writeByte(byte)}.
     * If boolean {@code v} is {@code true} the byte value 1 is written whereas if boolean is {@code false}
     * zero is written.
     *
     * @param  v boolean to be written.
     * @throws IOException if some I/O exception occurs during writing.
     */
    @Override
    public final void writeBoolean(final boolean v) throws IOException {
        writeByte(v ? 1 : 0);
    }

    /**
     * Writes the lower-order byte of each character. The high-order eight bits of each character
     * in the string are ignored - this method does <strong>not</strong> applies any encoding.
     *
     * <p>This method is provided because required by the {@link DataOutput} interface, but its
     * usage should generally be avoided.</p>
     *
     * @param  s the string to be written.
     * @throws IOException If an error occurred while writing the stream.
     */
    @Override
    public void writeBytes(final String s) throws IOException {
        final byte[] data = new byte[s.length()];
        for (int i=0; i<data.length; i++) {
            data[i] = (byte) s.charAt(i);
        }
        write(data);
    }

    /**
     * Writes all characters from the source into the stream.
     *
     * @param  s A String consider as an array of characters to be written into stream.
     * @throws IOException If an error occurred while writing the stream.
     */
    @Override
    public final void writeChars(final String s) throws IOException {
        writeChars(s.toCharArray());
    }

    /**
     * Writes two bytes of length information to the output stream, followed by the modified UTF-8 representation
     * of every character in the {@code str} string. Each character is converted to a group of one, two, or three
     * bytes, depending on the character code point value.
     *
     * @param  s the string to be written.
     * @throws IOException If an error occurred while writing the stream.
     */
    @Override
    public void writeUTF(final String s) throws IOException {
        byte[] data = s.getBytes("UTF-8");
        if (data.length > Short.MAX_VALUE) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ExcessiveStringSize));
        }
        final ByteOrder oldOrder = buffer.order();
        buffer.order(ByteOrder.BIG_ENDIAN);
        try {
            writeShort(data.length);
            write(data);
        } finally {
            buffer.order(oldOrder);
        }
    }

    /**
     * Closes the {@linkplain #channel}.
     *
     * @throws IOException If an error occurred while closing the channel.
     */
    @Override
    public final void close() throws IOException {
        channel.close();
    }
}
