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

import java.util.Set;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.function.UnaryOperator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.storage.StoreUtilities;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ForwardOnlyStorageException;
import org.apache.sis.storage.event.StoreListeners;


/**
 * Opens a readable or writable channel for a given input object (URL, input stream, <i>etc</i>).
 * The {@link #prepare prepare(…)} method analyzes the given input {@link Object} and tries to return a factory instance
 * capable to open at least a {@link ReadableByteChannel} for that input. For some kinds of input like {@link Path} or
 * {@link URL}, the {@link #readable readable(…)} method can be invoked an arbitrary amount of times for creating as many
 * channels as needed. But for other kinds of input like {@link InputStream}, only one channel can be returned.
 * In such case, only the first {@link #readable readable(…)} method invocation will succeed and all subsequent ones
 * will throw an exception.
 *
 * <h2>Multi-threading</h2>
 * This class is not thread-safe, except for the static {@link #prepare prepare(…)} method.
 * Callers are responsible for synchronizing their call to any member methods ({@link #readable readable(…)}, <i>etc</i>).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   0.8
 * @module
 */
public abstract class ChannelFactory {
    /**
     * Options to be rejected by {@link #prepare(Object, boolean, String, OpenOption[])} for safety reasons,
     * unless {@code allowWriteOnly} is {@code true}.
     */
    private static final Set<StandardOpenOption> ILLEGAL_OPTIONS = EnumSet.of(
            StandardOpenOption.APPEND, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DELETE_ON_CLOSE);

    /**
     * Whether this factory suggests to use direct buffers instead of heap buffers.
     * Direct buffer should be used for channels on the default file system or other
     * native source of data, and avoided otherwise.
     */
    public final boolean suggestDirectBuffer;

    /**
     * For subclass constructors.
     *
     * @param  suggestDirectBuffer  whether this factory suggests to use direct buffers instead of heap buffers.
     */
    protected ChannelFactory(final boolean suggestDirectBuffer) {
        this.suggestDirectBuffer = suggestDirectBuffer;
    }

    /**
     * Returns a byte channel factory from the given input or output,
     * or {@code null} if the given input/output is unsupported.
     * More specifically:
     *
     * <ul>
     *   <li>If the given storage is {@code null}, then this method returns {@code null}.</li>
     *   <li>If the given storage is a {@link ReadableByteChannel} or an {@link InputStream},
     *       then the factory will return that input directly or indirectly as a wrapper.</li>
     *   <li>If the given storage is a {@link WritableByteChannel} or an {@link OutputStream}
     *       and the {@code allowWriteOnly} argument is {@code true},
     *       then the factory will return that output directly or indirectly as a wrapper.</li>
     *   <li>If the given storage is a {@link Path}, {@link File}, {@link URL}, {@link URI}
     *       or {@link CharSequence} and the file is not a directory, then the factory will
     *       open new channels on demand.</li>
     * </ul>
     *
     * The given options are used for opening the channel on a <em>best effort basis</em>.
     * In particular, even if the caller provided the {@code WRITE} option, (s)he still needs
     * to verify if the returned channel implements {@link java.nio.channels.WritableByteChannel}.
     * This is because the channel may be opened by {@link URL#openStream()}, in which case the
     * options are ignored.
     *
     * <p>The following options are illegal for read operations and will cause an exception to be
     * thrown if provided while {@code allowWriteOnly} is {@code false}:
     * {@code APPEND}, {@code TRUNCATE_EXISTING}, {@code DELETE_ON_CLOSE}. We reject those options
     * because this method is primarily designed for readable channels, with optional data edition.
     * Since the write option is not guaranteed to be honored, we have to reject the options that
     * would alter significantly the channel behavior depending on whether we have been able to
     * honor the options or not.</p>
     *
     * @param  storage         the stream or the file to open, or {@code null}.
     * @param  allowWriteOnly  whether to allow wrapping {@link WritableByteChannel} and {@link OutputStream}.
     * @param  encoding        if the input is an encoded URL, the character encoding (normally {@code "UTF-8"}).
     *                         If the URL is not encoded, then {@code null}. This argument is ignored if the given
     *                         input does not need to be converted from URL to {@code File}.
     * @param  options         the options to use for creating a new byte channel. Can be null or empty for read-only.
     * @param  wrapper         a function for creating wrapper around the factory, or {@code null} if none.
     *                         It can be used for installing listener or for transforming data on the fly.
     * @return the channel factory for the given input, or {@code null} if the given input is of unknown type.
     * @throws IOException if an error occurred while processing the given input.
     */
    public static ChannelFactory prepare(
            final Object storage, final boolean allowWriteOnly,
            final String encoding, final OpenOption[] options,
            final UnaryOperator<ChannelFactory> wrapper) throws IOException
    {
        ChannelFactory factory = prepare(storage, allowWriteOnly, encoding, options);
        if (factory != null && wrapper != null) {
            factory = wrapper.apply(factory);
        }
        return factory;
    }

    /**
     * Returns a byte channel factory without wrappers, or {@code null} if unsupported.
     * This method performs the same work than {@linkplain #prepare(Object, boolean, String,
     * OpenOption[], UnaryOperator, UnaryOperator) above method}, but without wrappers.
     *
     * @param  storage         the stream or the file to open, or {@code null}.
     * @param  allowWriteOnly  whether to allow wrapping {@link WritableByteChannel} and {@link OutputStream}.
     * @param  encoding        if the input is an encoded URL, the character encoding (normally {@code "UTF-8"}).
     * @param  options         the options to use for creating a new byte channel. Can be null or empty for read-only.
     * @return the channel factory for the given input, or {@code null} if the given input is of unknown type.
     * @throws IOException if an error occurred while processing the given input.
     */
    private static ChannelFactory prepare(Object storage, final boolean allowWriteOnly,
            final String encoding, final OpenOption[] options) throws IOException
    {
        /*
         * Unconditionally verify the options (unless `allowWriteOnly` is true),
         * even if we may not use them.
         */
        final Set<OpenOption> optionSet;
        if (options == null || options.length == 0) {
            optionSet = Collections.singleton(StandardOpenOption.READ);
        } else {
            optionSet = new HashSet<>(Arrays.asList(options));
            optionSet.add(StandardOpenOption.READ);
            if (!allowWriteOnly && optionSet.removeAll(ILLEGAL_OPTIONS)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2,
                        "options", Arrays.toString(options)));
            }
        }
        /*
         * Check for storages that are already readable/Writable channels or input/output streams.
         * The channel or stream will be either returned directly or wrapped when first needed,
         * depending which factory method will be invoked.
         *
         * Note that Channels.newChannel(InputStream) checks for instances of FileInputStream in order to delegate
         * to its getChannel() method, but only if the input stream type is exactly FileInputStream, not a subtype.
         * If Apache SIS defines its own FileInputStream subclass someday, we may need to add a special case here.
         */
        if (storage instanceof ReadableByteChannel || (allowWriteOnly && storage instanceof WritableByteChannel)) {
            return new Stream(storage, storage instanceof FileChannel);
        } else if (storage instanceof InputStream) {
            return new Stream(storage, storage.getClass() == FileInputStream.class);
        } else if (allowWriteOnly && storage instanceof OutputStream) {
            return new Stream(storage, storage.getClass() == FileOutputStream.class);
        }
        /*
         * In the following cases, we will try hard to convert to Path objects before to fallback
         * on File, URL or URI, because only Path instances allow us to use the given OpenOptions.
         */
        if (storage instanceof URL) {
            try {
                storage = IOUtilities.toPath((URL) storage, encoding);
            } catch (IOException e) {
                /*
                 * This is normal if the URL uses HTTP or FTP protocol for instance. Log the exception at FINE
                 * level without stack trace. We will open the channel later using the URL instead of the Path.
                 */
                recoverableException(e);
            }
        } else if (storage instanceof URI) {
            /*
             * If the user gave us a URI, try to convert to a Path before to fallback to URL, in order to be
             * able to use the given OpenOptions.  Note that the conversion to Path is likely to fail if the
             * URL uses HTTP or FTP protocols, because JDK7 does not provide file systems for them by default.
             */
            final URI uri = (URI) storage;
            if (!uri.isAbsolute()) {
                /*
                 * All methods invoked in this block throws IllegalArgumentException if the URI has no scheme,
                 * so we are better to check now and provide a more appropriate exception for this method.
                 */
                throw new IOException(Resources.format(Resources.Keys.MissingSchemeInURI_1, uri));
            } else try {
                storage = Paths.get(uri);
            } catch (IllegalArgumentException | FileSystemNotFoundException e) {
                try {
                    storage = uri.toURL();
                } catch (MalformedURLException ioe) {
                    ioe.addSuppressed(e);
                    throw ioe;
                }
                /*
                 * We have been able to convert to URL, but the given OpenOptions may not be used.
                 * Log the exception at FINE level without stack trace, because the exception is
                 * probably a normal behavior in this context.
                 */
                recoverableException(e);
            }
        } else {
            if (storage instanceof CharSequence) {    // Needs to be before the check for File or URL.
                storage = IOUtilities.toFileOrURL(storage.toString(), encoding);
            }
            /*
             * If the input is a File or a CharSequence that we have been able to convert to a File,
             * try to convert to a Path in order to be able to use the OpenOptions. Only if we fail
             * to convert to a Path (which is unlikely), we will use directly the File.
             */
            if (storage instanceof File) {
                final File file = (File) storage;
                try {
                    storage = file.toPath();
                } catch (final InvalidPathException e) {
                    /*
                     * Unlikely to happen. But if it happens anyway, try to open the channel in a
                     * way less surprising for the user (closer to the object he has specified).
                     */
                    if (file.isFile()) {
                        return new Fallback(file, e);
                    }
                }
            }
        }
        /*
         * One last check for URL. The URL may be either the given input if we have not been able
         * to convert it to a Path, or a URI, File or CharSequence input converted to URL. Do not
         * try to convert the URL to a Path, because this has already been tried before this point.
         */
        if (storage instanceof URL) {
            final URL file = (URL) storage;
            return new ChannelFactory(false) {
                @Override public InputStream inputStream(String filename, StoreListeners listeners) throws IOException {
                    return file.openStream();
                }
                @Override public OutputStream outputStream(String filename, StoreListeners listeners) throws IOException {
                    return file.openConnection().getOutputStream();
                }
                @Override public ReadableByteChannel readable(String filename, StoreListeners listeners) throws IOException {
                    return Channels.newChannel(file.openStream());
                }
                @Override public WritableByteChannel writable(String filename, StoreListeners listeners) throws IOException {
                    return Channels.newChannel(file.openConnection().getOutputStream());
                }
            };
        }
        /*
         * Handle path to directory as an unsupported input. Attempts to read bytes from that channel would cause an
         * IOException to be thrown. On Java 10, that IOException does not occur at channel openning time but rather
         * at reading time. See https://bugs.openjdk.java.net/browse/JDK-8080629 for more information.
         *
         * If the file does not exist, we let NoSuchFileException to be thrown by the code below because non-existant
         * file is probably an error. This is not the same situation than a directory, which can not be opened by this
         * class but may be opened by some specialized DataStore implementations.
         */
        if (storage instanceof Path) {
            final Path path = (Path) storage;
            if (!Files.isDirectory(path)) {
                return new ChannelFactory(true) {
                    @Override public ReadableByteChannel readable(String filename, StoreListeners listeners) throws IOException {
                        return Files.newByteChannel(path, optionSet);
                    }
                    @Override public WritableByteChannel writable(String filename, StoreListeners listeners) throws IOException {
                        return Files.newByteChannel(path, optionSet);
                    }
                };
            }
        }
        return null;
    }

    /**
     * Returns whether the streams or channels created by this factory is coupled with the {@code storage} argument
     * given to the {@link #prepare prepare(…)} method. This is {@code true} if the storage is an {@link InputStream},
     * {@link OutputStream} or {@link Channel}, and {@code false} if the storage is a {@link Path}, {@link File},
     * {@link URL}, {@link URI} or equivalent.
     *
     * @return whether using the streams or channels will affect the original {@code storage} object.
     */
    public boolean isCoupled() {
        return false;
    }

    /**
     * Returns {@code true} if this factory is capable to create another readable byte channel.
     * This method returns {@code false} if this factory is capable to create only one channel
     * and {@link #readable readable(…)} has already been invoked.
     *
     * @return whether {@link #readable readable(…)} or {@link #writable writable(…)} can be invoked.
     */
    public boolean canOpen() {
        return true;
    }

    /**
     * Returns the readable channel as an input stream. The returned stream is <strong>not</strong> buffered;
     * it is caller's responsibility to wrap the stream in a {@link java.io.BufferedInputStream} if desired.
     *
     * <p>The default implementation wraps the channel returned by {@link #readable(String, StoreListeners)}.
     * This wrapping is preferred to direct instantiation of {@link FileInputStream} in order to take in account
     * the {@link OpenOption}s.</p>
     *
     * @param  filename  data store name to report in case of failure.
     * @param  listeners set of registered {@code StoreListener}s for the data store, or {@code null} if none.
     * @return the input stream.
     * @throws DataStoreException if the channel is read-once.
     * @throws IOException if the input stream or its underlying byte channel can not be created.
     */
    public InputStream inputStream(String filename, StoreListeners listeners)
            throws DataStoreException, IOException
    {
        return Channels.newInputStream(readable(filename, listeners));
    }

    /**
     * Returns the writable channel as an output stream. The returned stream is <strong>not</strong> buffered;
     * it is caller's responsibility to wrap the stream in a {@link java.io.BufferedOutputStream} if desired.
     *
     * <p>The default implementation wraps the channel returned by {@link #writable(String, StoreListeners)}.
     * This wrapping is preferred to direct instantiation of {@link FileOutputStream} in order to take in account
     * the {@link OpenOption}s.</p>
     *
     * @param  filename  data store name to report in case of failure.
     * @param  listeners set of registered {@code StoreListener}s for the data store, or {@code null} if none.
     * @return the output stream.
     * @throws DataStoreException if the channel is write-once.
     * @throws IOException if the output stream or its underlying byte channel can not be created.
     */
    public OutputStream outputStream(String filename, StoreListeners listeners)
            throws DataStoreException, IOException
    {
        return Channels.newOutputStream(writable(filename, listeners));
    }

    /**
     * Returns a byte channel from the input given to the {@link #prepare prepare(…)} method.
     * If the channel has already been created and this method can not create it twice, then
     * this method throws an exception.
     *
     * @param  filename  data store name to report in case of failure.
     * @param  listeners set of registered {@code StoreListener}s for the data store, or {@code null} if none.
     * @return the channel for the given input.
     * @throws DataStoreException if the channel is read-once.
     * @throws IOException if an error occurred while opening the channel.
     */
    public abstract ReadableByteChannel readable(String filename, StoreListeners listeners)
            throws DataStoreException, IOException;

    /**
     * Returns a byte channel from the output given to the {@link #prepare prepare(…)} method.
     * If the channel has already been created and this method can not create it twice, then
     * this method throws an exception.
     *
     * @param  filename  data store name to report in case of failure.
     * @param  listeners set of registered {@code StoreListener}s for the data store, or {@code null} if none.
     * @return the channel for the given output.
     * @throws DataStoreException if the channel is write-once.
     * @throws IOException if an error occurred while opening the channel.
     */
    public abstract WritableByteChannel writable(String filename, StoreListeners listeners)
            throws DataStoreException, IOException;

    /**
     * A factory that returns an existing channel <cite>as-is</cite>. The channel is often wrapping an
     * {@link InputStream} or {@link OutputStream} (which is the reason for {@code Stream} class name),
     * otherwise {@link org.apache.sis.storage.StorageConnector} would hare returned the storage object
     * directly instead of instantiating this factory.
     * The channel can be returned only once.
     */
    private static final class Stream extends ChannelFactory {
        /**
         * The stream or channel, or {@code null} if it has already been returned.
         * Shall be an instance of {@link InputStream}, {@link OutputStream},
         * {@link ReadableByteChannel} or {@link WritableByteChannel}.
         */
        private Object storage;

        /**
         * Creates a new factory for the given stream or channel, which will be returned only once.
         */
        Stream(final Object storage, final boolean suggestDirectBuffer) {
            super(suggestDirectBuffer);
            this.storage = storage;
        }

        /**
         * Returns {@code true} since use of channels or streams will affect the original storage object.
         */
        @Override
        public boolean isCoupled() {
            return true;
        }

        /**
         * Returns whether {@link #readable readable(…)} or {@link #writable writable(…)} can be invoked.
         */
        @Override
        public boolean canOpen() {
            return storage != null;
        }

        /**
         * Returns the storage object as an input stream. This is either the stream specified at construction
         * time if it can be returned directly, or a wrapper around the {@link ReadableByteChannel} otherwise.
         * The input stream can be returned at most once, otherwise an exception is thrown.
         */
        @Override
        public InputStream inputStream(String filename, StoreListeners listeners) throws DataStoreException, IOException {
            final Object in = storage;
            if (in instanceof InputStream) {
                storage = null;
                return (InputStream) in;
            }
            return super.inputStream(filename, listeners);
        }

        /**
         * Returns the storage object as an output stream. This is either the stream specified at construction
         * time if it can be returned directly, or a wrapper around the {@link WritableByteChannel} otherwise.
         * The output stream can be returned at most once, otherwise an exception is thrown.
         */
        @Override
        public OutputStream outputStream(String filename, StoreListeners listeners) throws DataStoreException, IOException {
            final Object out = storage;
            if (out instanceof OutputStream) {
                storage = null;
                return (OutputStream) out;
            }
            return super.outputStream(filename, listeners);
        }

        /**
         * Returns the readable channel on the first invocation or throws an exception on all subsequent invocations.
         * This is either the channel specified at construction time, or a wrapper around the {@link InputStream}.
         */
        @Override
        public ReadableByteChannel readable(final String filename, final StoreListeners listeners)
                throws DataStoreException, IOException
        {
            final Object in = storage;
            if (in instanceof ReadableByteChannel) {
                storage = null;
                return (ReadableByteChannel) in;
            } else if (in instanceof InputStream) {
                storage = null;
                return Channels.newChannel((InputStream) in);
            }
            String message = Resources.format(in != null ? Resources.Keys.StreamIsNotReadable_1
                                                         : Resources.Keys.StreamIsReadOnce_1, filename);
            if (in != null) {
                throw new IOException(message);                     // Stream is not readable.
            } else {
                throw new ForwardOnlyStorageException(message);     // Stream has already been read.
            }
        }

        /**
         * Returns the writable channel on the first invocation or throws an exception on all subsequent invocations.
         * This is either the channel specified at construction time, or a wrapper around the {@link OutputStream}.
         */
        @Override
        public WritableByteChannel writable(final String filename, final StoreListeners listeners)
                throws DataStoreException, IOException
        {
            final Object out = storage;
            if (out instanceof WritableByteChannel) {
                storage = null;
                return (WritableByteChannel) out;
            } else if (out instanceof OutputStream) {
                storage = null;
                return Channels.newChannel((OutputStream) out);
            }
            String message = Resources.format(out != null ? Resources.Keys.StreamIsNotWritable_1
                                                          : Resources.Keys.StreamIsWriteOnce_1, filename);
            if (out != null) {
                throw new IOException(message);                     // Stream is not writable.
            } else {
                throw new ForwardOnlyStorageException(message);     // Stream has already been written.
            }
        }
    }

    /**
     * A factory used as a fallback when we failed to convert a {@link File} to a {@link Path}.
     * This is used only if the conversion attempt threw an {@link InvalidPathException}. Such
     * failure is unlikely to happen, but if it happens anyway we try to open the channel in a
     * less surprising way for the user (i.e. closer to the object (s)he has specified).
     */
    private static final class Fallback extends ChannelFactory {
        /**
         * The file for which to open a channel.
         */
        private final File file;

        /**
         * The reason why we are using this fallback instead of a {@link Path}.
         * Will be reported at most once, then set to {@code null}.
         */
        private InvalidPathException cause;

        /**
         * Creates a new fallback to use if the given file can not be converted to a {@link Path}.
         */
        Fallback(final File file, final InvalidPathException cause) {
            super(true);
            this.file  = file;
            this.cause = cause;
        }

        /**
         * Opens a new input stream for the file given at construction time.
         * The returned stream is <strong>not</strong> buffered.
         *
         * <p>On the first invocation, this method reports a warning about the failure to convert the
         * {@link File} to a {@link Path}. On all subsequent invocations, the file is opened silently.</p>
         */
        @Override
        public FileInputStream inputStream(final String filename, final StoreListeners listeners)
                throws IOException
        {
            final FileInputStream in;
            try {
                in = new FileInputStream(file);
            } catch (IOException ioe) {
                if (cause != null) {
                    ioe.addSuppressed(cause);
                    cause = null;
                }
                throw ioe;
            }
            /*
             * We have been able to create a channel, maybe not with the given OpenOptions.
             * But the exception was nevertheless unexpected, so log its stack trace in order
             * to allow the developer to check if there is something wrong.
             */
            warning("inputStream", listeners);
            return in;
        }

        /**
         * Opens a new output stream for the file given at construction time.
         * The returned stream is <strong>not</strong> buffered.
         *
         * <p>On the first invocation, this method reports a warning about the failure to convert the
         * {@link File} to a {@link Path}. On all subsequent invocations, the file is opened silently.</p>
         */
        @Override
        public FileOutputStream outputStream(final String filename, final StoreListeners listeners)
                throws IOException
        {
            final FileOutputStream out;
            try {
                out = new FileOutputStream(file);
            } catch (IOException ioe) {
                if (cause != null) {
                    ioe.addSuppressed(cause);
                    cause = null;
                }
                throw ioe;
            }
            warning("outputStream", listeners);
            return out;
        }

        /**
         * Invoked when we have been able to create a channel, but maybe not with the given {@link OpenOption}s.
         * Since the exception was nevertheless unexpected, log its stack trace in order to allow the developer
         * to check if there is something wrong.
         */
        private void warning(final String method, final StoreListeners listeners) {
            if (cause != null) {
                final LogRecord record = new LogRecord(Level.WARNING, cause.toString());
                record.setLoggerName(Modules.STORAGE);
                record.setSourceMethodName(method);
                record.setSourceClassName(ChannelFactory.class.getName());
                record.setThrown(cause);
                cause = null;
                if (listeners != null) {
                    listeners.warning(record);
                } else {
                    StoreUtilities.LOGGER.log(record);
                }
            }
        }

        /**
         * Opens a new channel for the file given at construction time.
         */
        @Override
        public ReadableByteChannel readable(String filename, StoreListeners listeners) throws IOException {
            return inputStream(filename, listeners).getChannel();
        }

        /**
         * Opens a new channel for the file given at construction time.
         */
        @Override
        public WritableByteChannel writable(String filename, StoreListeners listeners) throws IOException {
            return outputStream(filename, listeners).getChannel();
        }
    }

    /**
     * Invoked for reporting exceptions that may be normal behavior. This method logs
     * the exception at {@link java.util.logging.Level#FINE} without stack trace.
     */
    private static void recoverableException(final Exception warning) {
        Logging.recoverableException(StoreUtilities.LOGGER, ChannelFactory.class, "prepare", warning);
    }
}
