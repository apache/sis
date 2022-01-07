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
package org.apache.sis.internal.storage.io;

import java.io.OutputStream;
import java.io.IOException;


/**
 * Wraps a {@link ChannelDataOutput} as a standard {@link OutputStream}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see InputStreamAdapter
 *
 * @since 0.8
 * @module
 */
final class OutputStreamAdapter extends OutputStream implements Markable {
    /**
     * The underlying data output stream. In principle, public access to this field breaks encapsulation.
     * But since {@code OutputStreamAdapter} does not hold any state and just forwards every method calls
     * to that {@code ChannelDataOutput}, using on object or the other does not make a difference.
     *
     * @todo to be replaced by a reference to {@link javax.imageio.stream.ImageOutputStream} if the
     *       {@link ChannelImageOutputStream} class implements that interface in a future version.
     */
    final ChannelImageOutputStream output;

    /**
     * Constructs a new output stream.
     *
     * @param output  the stream to wrap.
     */
    OutputStreamAdapter(final ChannelImageOutputStream output) {
        this.output = output;
    }

    /**
     * Writes the specified byte to the output stream.
     *
     * @param  b  the byte to write.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(final int b) throws IOException {
        output.write(b);
    }

    /**
     * Writes the specified bytes to the output stream.
     *
     * @param  b  the bytes to write.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(final byte[] b) throws IOException {
        output.write(b);
    }

    /**
     * Writes the specified sub-array to the output stream.
     *
     * @param  b    the bytes to write.
     * @param  off  the start offset in the data.
     * @param  len  the number of bytes to write.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        output.write(b, off, len);
    }

    /**
     * Marks the current position in this output stream.
     */
    @Override
    public void mark() {
        output.mark();
    }

    /**
     * Resets this stream to the position at the time the {@code mark} method was last called.
     *
     * @throws IOException if this stream can not move to the last mark position.
     */
    @Override
    public void reset() throws IOException {
        output.reset();
    }

    /**
     * Moves to the given position in the stream and discards all marks at or after that position.
     *
     * @param  mark  position where to seek.
     * @throws IOException if this stream can not move to the specified mark position.
     */
    @Override
    public void reset(final long mark) throws IOException {
        output.reset(mark);
    }

    /**
     * Returns the current byte position of the stream.
     *
     * @return the position of the stream.
     * @throws IOException if the position can not be obtained.
     */
    @Override
    public long getStreamPosition() throws IOException {
        return output.getStreamPosition();
    }

    /**
     * Forces any buffered output bytes to be written out.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        output.flush();
    }

    /**
     * Releases any system resources associated with the output stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        output.close();
    }
}
