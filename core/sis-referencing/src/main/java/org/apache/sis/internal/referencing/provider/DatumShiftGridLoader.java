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
package org.apache.sis.internal.referencing.provider;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import org.opengis.util.FactoryException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.MissingFactoryResourceException;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Path;
import java.io.FileNotFoundException;


/**
 * Base class of datum shift grid loaders.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
class DatumShiftGridLoader {
    /**
     * Conversion factor from degrees to seconds.
     */
    static final double DEGREES_TO_SECONDS = 3600;

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
    static final double SECOND_PRECISION = 1E-4;

    /**
     * The file to load, used only if we have errors to report.
     */
    final Path file;

    /**
     * The channel opened on the file.
     */
    private final ReadableByteChannel channel;

    /**
     * The buffer to use for transferring data from the channel.
     */
    final ByteBuffer buffer;

    /**
     * Creates a new loader for the given channel and an existing buffer.
     *
     * @param  channel Where to read data from.
     * @param  buffer  The buffer to use.
     * @param  file    Path to the longitude or latitude difference file. Used only for error reporting.
     */
    DatumShiftGridLoader(final ReadableByteChannel channel, final ByteBuffer buffer, final Path file) throws IOException {
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
     * @param  n The minimal number of bytes needed in the {@linkplain #buffer}.
     * @throws EOFException If the channel has reached the end of stream.
     * @throws IOException If an other kind of error occurred while reading.
     */
    final void ensureBufferContains(int n) throws IOException {
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
     */
    final void skip(int n) throws IOException {
        int p;
        while ((p = buffer.position() + n) > buffer.limit()) {
            n -= buffer.remaining();
            buffer.clear();
            ensureBufferContains(Math.min(n, buffer.capacity()));
        }
        buffer.position(p);
    }

    /**
     * Logs a message about a grid which is about to be loaded.
     *
     * @param caller The provider to logs as the source class.
     *               The source method will be set to {@code "createMathTransform"}.
     * @param file   The grid file, as a {@link String} or a {@link Path}.
     */
    static void log(final Class<?> caller, final Object file) {
        final LogRecord record = Messages.getResources(null).getLogRecord(Level.FINE, Messages.Keys.LoadingDatumShiftFile_1, file);
        record.setLoggerName(Loggers.COORDINATE_OPERATION);
        Logging.log(caller, "createMathTransform", record);
    }

    /**
     * Creates the exception to thrown when the provider failed to load the grid file.
     *
     * @param format   The format name (e.g. "NTv2" or "NADCON").
     * @param file     The grid file that the subclass tried to load.
     * @param cause    The cause of the failure to load the grid file.
     */
    static FactoryException canNotLoad(final String format, final Path file, final Exception cause) {
        final String message = Errors.format(Errors.Keys.CanNotParseFile_2, format, file);
        if (cause instanceof FileNotFoundException) {
            return new MissingFactoryResourceException(message, cause);
        } else {
            return new FactoryDataException(message, cause);
        }
    }
}
