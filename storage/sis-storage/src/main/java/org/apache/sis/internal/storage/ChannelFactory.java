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

import java.util.Set;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.Modules;


/**
 * Opens a readable channel for a given input object (URL, input stream, <i>etc</i>).
 * The {@link #prepare prepare(…)} method analyzes the given input {@link Object} and tries to return a factory instance
 * capable to open at least one {@link ReadableByteChannel} for that input. For some kinds of input like {@link Path} or
 * {@link URL}, the {@link #reader()} method can be invoked an arbitrary amount of times for creating as many channels
 * as needed. But for other kinds of input like {@link InputStream}, only one channel can be returned. In such case,
 * only the first {@link #reader()} method invocation will succeed and all subsequent ones will throw an exception.
 *
 * <div class="section">Multi-threading</div>
 * This class is not thread-safe, except for the static {@link #prepare prepare(…)} method.
 * Callers are responsible for synchronizing their call to any member methods ({@link #reader()}, <i>etc</i>).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public abstract class ChannelFactory {
    /**
     * Options to be rejected by {@link #create(Object, String, OpenOption[])} for safety reasons.
     */
    private static final Set<StandardOpenOption> ILLEGAL_OPTIONS = EnumSet.of(
            StandardOpenOption.APPEND, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DELETE_ON_CLOSE);

    /**
     * For subclass constructors.
     */
    ChannelFactory() {
    }

    /**
     * Returns a byte channel factory from the given input, or {@code null} if the input is of unknown type.
     * More specifically:
     *
     * <ul>
     *   <li>If the given input is {@code null}, then this method returns {@code null}.</li>
     *   <li>If the given input is a {@link ReadableByteChannel} or an {@link InputStream},
     *       then the factory will return that input directly or indirectly as a wrapper.</li>
     *   <li>If the given input if a {@link Path}, {@link File}, {@link URL}, {@link URI}
     *       or {@link CharSequence}, then the factory will open new channels on demand.</li>
     * </ul>
     *
     * The given options are used for opening the channel on a <em>best effort basis</em>.
     * In particular, even if the caller provided the {@code WRITE} option, he still needs
     * to verify if the returned channel implements {@link java.nio.channels.WritableByteChannel}.
     * This is because the channel may be opened by {@link URL#openStream()}, in which case the
     * options are ignored.
     *
     * <p>The following options are illegal and will cause an exception to be thrown if provided:
     * {@code APPEND}, {@code TRUNCATE_EXISTING}, {@code DELETE_ON_CLOSE}. We reject those options
     * because this method is primarily designed for readable channels, with optional data edition.
     * Since the write option is not guaranteed to be honored, we have to reject the options that
     * would alter significatively the channel behavior depending on whether we have been able to
     * honor the options or not.</p>
     *
     * @param  input     the file to open, or {@code null}.
     * @param  encoding  if the input is an encoded URL, the character encoding (normally {@code "UTF-8"}).
     *                   If the URL is not encoded, then {@code null}. This argument is ignored if the given
     *                   input does not need to be converted from URL to {@code File}.
     * @param  options   the options to use for creating a new byte channel. Can be null or empty for read-only.
     * @return the channel factory for the given input, or {@code null} if the given input is of unknown type.
     * @throws IOException if an error occurred while processing the given input.
     */
    public static ChannelFactory prepare(Object input, final String encoding, OpenOption... options) throws IOException {
        /*
         * Unconditionally verify the options, even if we may not use them.
         */
        final Set<OpenOption> optionSet;
        if (options == null || options.length == 0) {
            optionSet = Collections.singleton(StandardOpenOption.READ);
        } else {
            optionSet = new HashSet<>(Arrays.asList(options));
            optionSet.add(StandardOpenOption.READ);
            if (optionSet.removeAll(ILLEGAL_OPTIONS)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2,
                        "options", Arrays.toString(options)));
            }
        }
        /*
         * Check for inputs that are already readable channels or input streams.
         * Note that Channels.newChannel(InputStream) checks for instances of FileInputStream in order to delegate
         * to its getChannel() method, but only if the input stream type is exactly FileInputStream, not a subtype.
         * If Apache SIS defines its own FileInputStream subclass someday, we may need to add a special case here.
         */
        if (input instanceof ReadableByteChannel) {
            return new Stream((ReadableByteChannel) input);
        } else if (input instanceof InputStream) {
            return new Stream(Channels.newChannel((InputStream) input));
        }
        /*
         * In the following cases, we will try hard to convert to Path objects before to fallback
         * on File, URL or URI, because only Path instances allow us to use the given OpenOptions.
         */
        if (input instanceof URL) {
            try {
                input = IOUtilities.toPath((URL) input, encoding);
            } catch (IOException e) {
                /*
                 * This is normal if the URL uses HTTP or FTP protocol for instance. Log the exception at FINE
                 * level without stack trace. We will open the channel later using the URL instead than the Path.
                 */
                recoverableException(e);
            }
        } else if (input instanceof URI) {
            /*
             * If the user gave us a URI, try to convert to a Path before to fallback to URL, in order to be
             * able to use the given OpenOptions.  Note that the conversion to Path is likely to fail if the
             * URL uses HTTP or FTP protocols, because JDK7 does not provide file systems for them by default.
             */
            final URI uri = (URI) input;
            if (!uri.isAbsolute()) {
                /*
                 * All methods invoked in this block throws IllegalArgumentException if the URI has no scheme,
                 * so we are better to check now and provide a more appropriate exception for this method.
                 */
                throw new IOException(Resources.format(Resources.Keys.MissingSchemeInURI_1, uri));
            } else try {
                input = Paths.get(uri);
            } catch (IllegalArgumentException | FileSystemNotFoundException e) {
                try {
                    input = uri.toURL();
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
            if (input instanceof CharSequence) {    // Needs to be before the check for File or URL.
                input = IOUtilities.toFileOrURL(input.toString(), encoding);
            }
            /*
             * If the input is a File or a CharSequence that we have been able to convert to a File,
             * try to convert to a Path in order to be able to use the OpenOptions. Only if we fail
             * to convert to a Path (which is unlikely), we will use directly the File.
             */
            if (input instanceof File) {
                final File file = (File) input;
                try {
                    input = file.toPath();
                } catch (final InvalidPathException e) {
                    /*
                     * Unlikely to happen. But if it happens anyway, try to open the channel in a
                     * way less surprising for the user (closer to the object he has specified).
                     */
                    return new Fallback(file, e);
                }
            }
        }
        /*
         * One last check for URL. The URL may be either the given input if we have not been able
         * to convert it to a Path, or a URI, File or CharSequence input converted to URL. Do not
         * try to convert the URL to a Path, because this has already been tried before this point.
         */
        if (input instanceof URL) {
            final URL file = (URL) input;
            return new ChannelFactory() {
                @Override public ReadableByteChannel reader() throws IOException {
                    return Channels.newChannel(file.openStream());
                }
            };
        }
        if (input instanceof Path) {
            final Path path = (Path) input;
            return new ChannelFactory() {
                @Override public ReadableByteChannel reader() throws IOException {
                    return Files.newByteChannel(path, optionSet);
                }
            };
        }
        return null;
    }

    /**
     * Returns {@code true} if this factory is capable to create another reader. This method returns {@code false}
     * if this factory is capable to create only one channel and {@link #reader()} has already been invoked.
     *
     * @return whether {@link #reader()} can be invoked.
     */
    public boolean canOpen() {
        return true;
    }

    /**
     * Returns the channel as an input stream. The returned stream is <strong>not</strong> buffered;
     * it is caller's responsibility to wrap the stream in a {@link java.io.BufferedInputStream} if desired.
     *
     * @return the input stream.
     * @throws IOException if the input stream or its underlying byte channel can not be created.
     */
    public InputStream inputStream() throws IOException {
        return Channels.newInputStream(reader());
    }

    /**
     * Returns a byte channel from the input given to the {@link #prepare prepare(…)} method.
     * If the channel has already been created and this method can not create it twice, then
     * this method throw an exception.
     *
     * @return the channel for the given input.
     * @throws IOException if an error occurred while opening the channel.
     */
    public abstract ReadableByteChannel reader() throws IOException;

    /**
     * A factory that returns an existing channel <cite>as-is</cite>.
     * The channel can be returned only once.
     */
    private static final class Stream extends ChannelFactory {
        /**
         * The channel, or {@code null} if it has already been returned.
         */
        private ReadableByteChannel input;

        /**
         * Creates a new factory for the given channel, which will be returned only once.
         */
        Stream(final ReadableByteChannel input) {
            this.input = input;
        }

        /**
         * Returns whether {@link #reader()} can be invoked.
         */
        @Override
        public boolean canOpen() {
            return input != null;
        }

        /**
         * Returns the channel on the first invocation or
         * throws an exception on all subsequent invocations.
         */
        @Override
        public ReadableByteChannel reader() throws IOException {
            final ReadableByteChannel in = input;
            if (in != null) {
                input = null;
                return in;
            }
            throw new IOException(Resources.format(Resources.Keys.StreamIsReadOnce));
        }
    }

    /**
     * A factory used as a fallback when we failed to convert a {@link File} to a {@link Path}.
     * This is used only if the conversion attempt threw an {@link InvalidPathException}. Such
     * failure is unlikely to happen, but if it happens anyway we try to open the channel in a
     * way less surprising for the user (closer to the object he has specified).
     */
    private static final class Fallback extends ChannelFactory {
        /**
         * The file for which to open a channel.
         */
        private final File file;

        /**
         * The reason why we are using this fallback instead than a {@link Path}.
         * Will be reported at most once, then set to {@code null}.
         */
        private InvalidPathException cause;

        /**
         * Creates a new fallback to use if the given file can not be converted to a {@link Path}.
         */
        Fallback(final File file, final InvalidPathException cause) {
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
        public FileInputStream inputStream() throws IOException {
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
            if (cause != null) {
                Logging.unexpectedException(Logging.getLogger(Modules.STORAGE), ChannelFactory.class, "prepare", cause);
                cause = null;
            }
            return in;
        }

        /**
         * Opens a new channel for the file given at construction time.
         */
        @Override
        public ReadableByteChannel reader() throws IOException {
            return inputStream().getChannel();
        }
    }

    /**
     * Invoked for reporting exceptions that may be normal behavior. This method logs
     * the exception at {@link java.util.logging.Level#FINE} without stack trace.
     */
    private static void recoverableException(final Exception warning) {
        Logging.recoverableException(Logging.getLogger(Modules.STORAGE), ChannelFactory.class, "prepare", warning);
    }
}
