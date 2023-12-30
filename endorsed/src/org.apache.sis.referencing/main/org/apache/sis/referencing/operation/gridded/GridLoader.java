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
package org.apache.sis.referencing.operation.gridded;

import java.util.logging.LogRecord;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.referencing.operation.provider.AbstractProvider;


/**
 * Base class of datum shift grid loaders.
 * This loader uses {@link ReadableByteChannel}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class GridLoader {
    /**
     * Conversion factor from degrees to seconds.
     */
    public static final double DEGREES_TO_SECONDS = 3600;

    /**
     * Possible precision for offset values in seconds of angle. This value is used only as a hint
     * when attempting to compress the grid in arrays of {@code short} values. It does not hurt if
     * this value is wrong, as it will only cause the grid to not be compressed.
     *
     * <p>Some interesting values:</p>
     * <ul>
     *   <li>1E-4 is about 3 millimetres on Earth.</li>
     *   <li>1E-6 matches the precision found in ASCII outputs of NADCON grids.</li>
     *   <li>1E-7 is about 1 ULP of 1 second of angle.</li>
     * </ul>
     *
     * We use a value of 1E-4 because more accurate values tend to cause overflows in the compression algorithm,
     * in which case the compression fails. With a more reasonable value, we have better chances of success.
     */
    protected static final double SECOND_PRECISION = 1E-4;

    /**
     * The file to load, used for parameter declaration and if we have errors to report.
     */
    protected final GridFile file;

    /**
     * The channel opened on the file.
     */
    private final ReadableByteChannel channel;

    /**
     * The buffer to use for transferring data from the channel.
     */
    protected final ByteBuffer buffer;

    /**
     * Creates a new loader for the given channel and an existing buffer.
     *
     * @param  channel  where to read data from.
     * @param  buffer   the buffer to use.
     * @param  file     path to the longitude or latitude difference file. Used for parameter declaration and error reporting.
     */
    protected GridLoader(final ReadableByteChannel channel, final ByteBuffer buffer, final GridFile file) throws IOException {
        this.file    = file;
        this.buffer  = buffer;
        this.channel = channel;
        channel.read(buffer);
        buffer.flip();
    }

    /**
     * Makes sure that the buffer contains at least <var>n</var> remaining bytes.
     * It is caller's responsibility to ensure that the given number of bytes is
     * not greater than the {@linkplain ByteBuffer#capacity() buffer capacity}.
     *
     * @param  n  the minimal number of bytes needed in the {@linkplain #buffer}.
     * @throws EOFException if the channel has reached the end of stream.
     * @throws IOException if another kind of error occurred while reading.
     */
    protected final void ensureBufferContains(int n) throws IOException {
        assert n >= 0 && n <= buffer.capacity() : n;
        n -= buffer.remaining();
        if (n > 0) {
            buffer.compact();
            do {
                final int c = channel.read(buffer);
                if (c <= 0) {
                    if (c != 0) {
                        throw new EOFException(Errors.format(Errors.Keys.UnexpectedEndOfFile_1, file));
                    } else {
                        throw new IOException(Errors.format(Errors.Keys.CanNotRead_1, file));
                    }
                }
                n -= c;
            } while (n > 0);
            buffer.flip();
        }
    }

    /**
     * Skips exactly <var>n</var> bytes.
     *
     * @param  n  the number of bytes to skip.
     */
    protected final void skip(int n) throws IOException {
        int p;
        while ((p = buffer.position() + n) > buffer.limit()) {
            n -= buffer.remaining();
            buffer.clear();
            ensureBufferContains(Math.min(n, buffer.capacity()));
        }
        buffer.position(p);
    }

    /**
     * Logs the given record.
     * The logger will be {@code "org.apache.sis.referencing.operation"} and the originating
     * method will be {@code "createMathTransform"} in the specified {@code caller} class.
     *
     * @param  caller  the provider to logs as the source class.
     * @param  record  the record to complete and log.
     */
    protected static void log(final Class<?> caller, final LogRecord record) {
        Logging.completeAndLog(AbstractProvider.LOGGER, caller, "createMathTransform", record);
    }
}
