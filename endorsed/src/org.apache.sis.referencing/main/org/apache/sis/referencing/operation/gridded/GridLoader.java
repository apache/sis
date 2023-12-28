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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.EOFException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import org.opengis.util.FactoryException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.system.DataDirectory;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.factory.FactoryDataException;
import org.apache.sis.referencing.factory.MissingFactoryResourceException;
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
    protected final URI file;

    /**
     * The channel opened on the file.
     */
    private final ReadableByteChannel channel;

    /**
     * The buffer to use for transferring data from the channel.
     */
    protected final ByteBuffer buffer;

    /**
     * Whether the tip about the location of datum shift files has been logged.
     * We log this tip only once, and only if we failed to load at least one grid.
     */
    private static final AtomicBoolean datumDirectoryLogged = new AtomicBoolean();

    /**
     * Creates a new loader for the given channel and an existing buffer.
     *
     * @param  channel  where to read data from.
     * @param  buffer   the buffer to use.
     * @param  file     path to the longitude or latitude difference file. Used for parameter declaration and error reporting.
     */
    protected GridLoader(final ReadableByteChannel channel, final ByteBuffer buffer, final URI file) throws IOException {
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
     * Creates a channel for reading bytes from the file at the specified path.
     * This method tries to open using the file system before to open from the URL.
     *
     * @param  path  the path from where to read bytes.
     * @return a channel for reading bytes from the given path.
     * @throws IOException if the channel cannot be created.
     */
    public static ReadableByteChannel newByteChannel(final URI path) throws IOException {
        try {
            return Files.newByteChannel(Path.of(path));
        } catch (FileSystemNotFoundException e) {
            Logging.ignorableException(AbstractProvider.LOGGER, GridLoader.class, "newByteChannel", e);
        }
        return Channels.newChannel(path.toURL().openStream());
    }

    /**
     * Logs a message about a grid which is about to be loaded.
     * The logger will be {@code "org.apache.sis.referencing.operation"} and the originating
     * method will be {@code "createMathTransform"} in the specified {@code caller} class.
     *
     * @param  caller  the provider to logs as the source class.
     * @param  file    the grid file, as a {@link String} or a {@link URI}.
     */
    public static void startLoading(final Class<?> caller, final Object file) {
        log(caller, Resources.forLocale(null).getLogRecord(Level.FINE, Resources.Keys.LoadingDatumShiftFile_1, file));
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

    /**
     * Creates the exception to throw when the provider failed to load the grid file.
     *
     * @param  caller  the provider to logs as the source class if a warning occurs.
     * @param  format  the format name (e.g. "NTv2" or "NADCON").
     * @param  file    the grid file that the subclass tried to load.
     * @param  cause   the cause of the failure to load the grid file.
     */
    public static FactoryException canNotLoad(final Class<?> caller, final String format, final URI file, final Exception cause) {
        if (!datumDirectoryLogged.get()) {
            final Path directory = DataDirectory.DATUM_CHANGES.getDirectory();
            if (directory != null && !datumDirectoryLogged.getAndSet(true)) {
                log(caller, Resources.forLocale(null).getLogRecord(Level.INFO,
                            Resources.Keys.DatumChangesDirectory_1, directory));
            }
        }
        final boolean notFound = (cause instanceof NoSuchFileException) || (cause instanceof FileNotFoundException);
        final String message = Resources.format(notFound ? Resources.Keys.FileNotFound_2
                                                         : Resources.Keys.FileNotReadable_2, format, file);
        if (notFound) {
            return new MissingFactoryResourceException(message, cause);
        } else {
            return new FactoryDataException(message, cause);
        }
    }
}
