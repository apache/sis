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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;


/**
 * Adds the missing methods in {@code ChannelDataInput} for implementing the {@code DataInput} interface.
 * This class is provided for testing the compatibility of {@code ChannelDataInput} API with {@code DataInput},
 * and as a placeholder in case we want to move this implementation in the main code in a future SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.07)
 * @version 0.3
 * @module
 */
public class ChannelDataInputCompleted extends ChannelDataInput implements DataInput {
    /**
     * Creates a new input source for the given channel and using the given buffer.
     *
     * @param  filename A file identifier used only for formatting error message.
     * @param  channel  The channel from where data are read.
     * @param  buffer   The buffer where to copy the data.
     * @param  filled   {@code true} if the buffer already contains data, or {@code false} if it needs
     *                  to be initially filled with some content read from the channel.
     * @throws IOException If an error occurred while reading the channel.
     */
    public ChannelDataInputCompleted(final String filename, final ReadableByteChannel channel,
            final ByteBuffer buffer, final boolean filled) throws IOException
    {
        super(filename, channel, buffer, filled);
    }

    /**
     * Returns the next byte from the stream as an unsigned integer between 0 and 255, or -1 if
     * we reached the end of stream.
     *
     * @return The next byte as an unsigned integer, or -1 on end of stream.
     * @throws IOException If an error occurred while reading the stream.
     */
    public final int read() throws IOException {
        return hasRemaining() ? buffer.get() & 0xFF : -1;
    }

    /**
     * Reads a byte from the stream and returns a {@code true} if it is nonzero, {@code false} otherwise.
     * The implementation is as below:
     *
     * {@preformat java
     *     return readByte() != 0;
     * }
     *
     * @return The value of the next boolean from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    /**
     * Skips over n bytes of data from the input stream.
     * This implementation does not skip more bytes than the buffer capacity.
     *
     * @param  n Maximal number of bytes to skip.
     * @return Number of bytes actually skipped.
     * @throws IOException If an error occurred while reading.
     */
    @Override
    public final int skipBytes(int n) throws IOException {
        if (!hasRemaining()) {
            return 0;
        }
        int r = buffer.remaining();
        if (n >= r) {
            n = r;
        }
        buffer.position(buffer.position() + n);
        return n;
    }

    /**
     * Reads the new bytes until the next EOL. This method can read only US-ASCII strings.
     * This method is provided for compliance with the {@link DataInput} interface,
     * but is generally not recommended.
     *
     * @return The next line, or {@code null} if the EOF has been reached.
     * @throws IOException If an error occurred while reading.
     */
    @Override
    public final String readLine() throws IOException {
        int c = read();
        if (c < 0) {
            return null;
        }
        StringBuilder line = new StringBuilder();
        line.append((char) c);
loop:   while ((c = read()) >= 0) {
            switch (c) {
                case '\r': {
                    c = read();
                    if (c >= 0 && c != '\n') {
                        buffer.position(buffer.position() - 1);
                    }
                    break loop;
                }
                case '\n': {
                    break loop;
                }
            }
            line.append((char) c);
        }
        return line.toString();
    }

    /**
     * Reads in a string that has been encoded using a UTF-8 string.
     *
     * @return The string reads from the stream.
     * @throws IOException If an error (including EOF) occurred while reading the stream.
     */
    @Override
    public final String readUTF() throws IOException {
        final ByteOrder oldOrder = buffer.order();
        buffer.order(ByteOrder.BIG_ENDIAN);
        try {
            return DataInputStream.readUTF(this);
        } finally {
            buffer.order(oldOrder);
        }
    }
}
