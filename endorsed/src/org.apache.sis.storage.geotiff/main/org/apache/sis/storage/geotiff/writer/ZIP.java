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
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.storage.geotiff.base.Resources;


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
     * @param  length  number of bytes to be compressed.
     * @param  level   the compression level.
     */
    public ZIP(final ChannelDataOutput output, final long length, final int level) {
        super(output, length);
        deflater = new Deflater(level);
    }

    /**
     * Creates a buffer to use with this compression channel.
     * The byte order shall be set by the caller.
     */
    @Override
    final ByteBuffer createBuffer() {
        return ByteBuffer.allocateDirect(bufferCapacity());
    }

    /**
     * Compresses all remaining bytes from the given buffer to the {@linkplain #output output}.
     *
     * @param  source  the buffer from which bytes are to be transferred.
     * @return the number of uncompressed bytes written.
     * @throws IOException if an error occurred while writing to the underlying output channel.
     */
    @Override
    public int write(final ByteBuffer source) throws IOException {
        final ByteBuffer target = output.buffer;
        final int start = source.position();
        int remaining = source.remaining();
        deflater.setInput(source);
        /*
         * If the given buffer is the last input, notify the deflater about that. It is not strictly
         * necessary to do this notification here because `finish(…)` will flush pending data anyway.
         * But providing this information in advance may hopefully help the deflater. The condition
         * can be true when raster data are sent directly to this compressor, without `this.buffer`.
         */
        final long numBytes = deflater.getBytesRead() + remaining;
        if (numBytes >= length) {
            /*
             * To be strict, we shoud raise an exception if `deflater.finished()` is true.
             * But it is safer to do this check indirectly in the following loop instead.
             */
            deflater.finish();
        }
        while (remaining > 0) {
            assert !deflater.needsInput();
            output.ensureBufferAccepts(Math.min(remaining, target.capacity()));
            target.limit(target.capacity());        // Allow the use of all available space.
            try {
                if (deflater.deflate(target) == 0) {
                    /*
                     * According Javadoc, it may occur if `deflater.needsInput()` needs to be checked.
                     * But we already know that its value should be `false`. The other possibility is
                     * that `deflater.finished()` is true. The latter may happen if we determined that
                     * a call to this `write(…)` should be the last one, but this method is nevertheless
                     * invoked again. It may happen when there is inconsistency in the computation using
                     * pixel stride and scanline stride, sometime because of malformed sample model.
                     */
                    throw new IOException(Resources.forLocale(null)
                            .getString(Resources.Keys.UnexpectedTileLength_2, length, numBytes));
                }
            } finally {
                target.limit(target.position());        // Bytes after the position are not valid.
            }
            remaining = source.remaining();
        }
        return source.position() - start;   // Number from caller's perspective (it doesn't know about compression).
    }

    /**
     * Writes any pending data and reset the deflater for the next tile to compress.
     * Usually there is some remaining bytes in the {@code owner} buffer.
     * The call to {@code super.finish(owner)} will indirectly performs one last call
     * to {@link #write(ByteBuffer)} with the {@link Deflater#finish()} flag set.
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
            output.ensureBufferAccepts(Math.max(target.capacity() / 2, 1));
            target.limit(target.capacity());            // Allow the use of all available space.
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
