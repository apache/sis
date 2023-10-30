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
package org.apache.sis.storage.geotiff.writer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.io.stream.ChannelDataOutput;


/**
 * Deflater for values encoded with the "Deflate" compression.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ZIP extends CompressionChannel {
    /**
     * Access to the ZLIB compression library.
     * Must be released by call to {@link Deflater#end()} after decompression is completed.
     */
    private final Deflater deflater;

    /**
     * Creates a new channel which will compress data to the given output.
     *
     * @param  output  the destination of compressed data.
     * @param  level   the compression level.
     */
    public ZIP(final ChannelDataOutput output, final int level) {
        super(output);
        deflater = new Deflater(level);
        deflater.setStrategy(Deflater.FILTERED);
    }

    /**
     * Compresses some bytes from the given buffer to the {@linkplain #output output}.
     *
     * @param  source  the buffer from which bytes are to be transferred.
     * @return the number of uncompressed bytes written.
     * @throws IOException if an error occurred while writing to the underlying output channel.
     */
    @Override
    public int write(final ByteBuffer source) throws IOException {
        final ByteBuffer target = output.buffer;
        final int start = source.position();
        deflater.setInput(source);
        int remaining;
        while ((remaining = source.remaining()) > 0) {
            assert !deflater.needsInput();
            output.ensureBufferAccepts(Math.min(remaining, target.capacity()));
            target.limit(target.capacity());        // Allow the use of all available space.
            deflater.deflate(target);
            target.limit(target.position());        // Bytes after the position are not valid.
        }
        return source.position() - start;   // Number from caller's perspective (it doesn't know about compression).
    }

    /**
     * Writes any pending data and reset the deflater for the next tile to compress.
     *
     * @param  owner  the data output which is writing in this channel.
     * @throws IOException if an error occurred while writing to the underlying output channel.
     */
    @Override
    public void finish(final ChannelDataOutput owner) throws IOException {
        deflater.finish();
        super.finish(owner);
        final ByteBuffer target = output.buffer;
        while (!deflater.finished()) {
            output.ensureBufferAccepts(Math.min(target.capacity(), BUFFER_SIZE));
            target.limit(target.capacity());            // Allow the use of all available space.
            deflater.setInput(ArraysExt.EMPTY_BYTE);
            deflater.finish();
            deflater.deflate(target);
            target.limit(target.position());            // Bytes after the position are not valid.
        }
        deflater.reset();
    }

    /**
     * Releases resources used by the deflater.
     */
    @Override
    public void close() {
        deflater.end();
    }
}
