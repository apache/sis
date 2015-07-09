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
import java.util.List;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;
import java.util.Locale;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.Modules;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Files;
import org.apache.sis.internal.jdk7.StandardCharsets;
import org.apache.sis.internal.jdk7.StandardOpenOption;


/**
 * Utility methods related to I/O operations. Many methods in this class accept arbitrary {@link Object} argument
 * and perform a sequence of {@code instanceof} checks. Since this approach provides no type safety and since the
 * sequence of {@code instanceof} checks is somewhat arbitrary, those methods can not be in public API.
 *
 * <p>Unless otherwise specified, giving an instance of unknown type or a {@code null} value cause the methods to
 * return {@code null}. No exception is thrown for unknown type - callers must check that the return value is not
 * null. However exceptions may be thrown for malformed URI or URL.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final class IOUtilities extends Static {
    /**
     * Options to be rejected by {@link #open(Object, String, OpenOption[])} for safety reasons.
     */
    private static final List<String> ILLEGAL_OPTIONS = Arrays.asList( // EnumSet of StandardOpenOption on JDK7 branch
            StandardOpenOption.APPEND, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DELETE_ON_CLOSE);

    /**
     * Do not allow instantiation of this class.
     */
    private IOUtilities() {
    }

    /**
     * Returns the filename from a {@link Path}, {@link File}, {@link URL}, {@link URI} or {@link CharSequence}
     * instance. If the given argument is specialized type like {@code Path} or {@code File}, then this method uses
     * dedicated API like {@link Path#getFileName()}. Otherwise this method gets a string representation of the path
     * and returns the part after the last {@code '/'} or platform-dependent name separator character, if any.
     *
     * @param  path The path as an instance of one of the above-cited types, or {@code null}.
     * @return The filename in the given path, or {@code null} if the given object is null or of unknown type.
     */
    public static String filename(final Object path) {
        return part(path, false);
    }

    /**
     * Returns the filename extension from a {@link Path}, {@link File}, {@link URL}, {@link URI} or
     * {@link CharSequence} instance. If no extension is found, returns an empty string. If the given
     * object is of unknown type, return {@code null}.
     *
     * @param  path The path as an instance of one of the above-cited types, or {@code null}.
     * @return The extension in the given path, or an empty string if none, or {@code null}
     *         if the given object is null or of unknown type.
     */
    public static String extension(final Object path) {
        return part(path, true);
    }

    /**
     * Implementation of {@link #filename(Object)} and {@link #extension(Object)} methods.
     */
    private static String part(final Object path, final boolean extension) {
        int fromIndex = 0;
        final String name;
        if (path instanceof File) {
            name = ((File) path).getName();
        } else {
            char separator = '/';
            if (path instanceof URL) {
                name = ((URL) path).getPath();
            } else if (path instanceof URI) {
                final URI uri = (URI) path;
                name = uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath();
            } else if (path instanceof CharSequence) {
                name = path.toString();
                separator = File.separatorChar;
            } else {
                return null;
            }
            fromIndex = name.lastIndexOf('/') + 1;
            if (separator != '/') {
                // Search for platform-specific character only if the object is neither a URL or a URI.
                fromIndex = Math.max(fromIndex, CharSequences.lastIndexOf(name, separator, fromIndex, name.length()) + 1);
            }
        }
        if (extension) {
            fromIndex = CharSequences.lastIndexOf(name, '.', fromIndex, name.length()) + 1;
            if (fromIndex <= 1) { // If the dot is the first character, do not consider as a filename extension.
                return "";
            }
        }
        return name.substring(fromIndex);
    }

    /**
     * Returns a string representation of the given path, or {@code null} if none. The current implementation
     * recognizes only the {@link Path}, {@link File}, {@link URL}, {@link URI} or {@link CharSequence} types.
     *
     * @param  path The path for which to return a string representation.
     * @return The string representation, or {@code null} if none.
     */
    public static String toString(final Object path) {
        // For the following types, the string that we want can be obtained only by toString(),
        // or the class is final so we know that the toString(à behavior can not be changed.
        if (path instanceof CharSequence || path instanceof URL || path instanceof URI) {
            return path.toString();
        }
        // While toString() would work too on the default implementation, the following
        // type is not final. So we are better to invoke the dedicated method.
        if (path instanceof File) {
            return ((File) path).getPath();
        }
        return null;
    }

    /**
     * Encodes the characters that are not legal for the {@link URI#URI(String)} constructor.
     * Note that in addition to unreserved characters ("{@code _-!.~'()*}"), the reserved
     * characters ("{@code ?/[]@}") and the punctuation characters ("{@code ,;:$&+=}")
     * are left unchanged, so they will be processed with their special meaning by the
     * URI constructor.
     *
     * <p>The current implementations replaces only the space characters, control characters
     * and the {@code %} character. Future versions may replace more characters as we learn
     * from experience.</p>
     *
     * @param  path The path to encode, or {@code null}.
     * @return The encoded path, or {@code null} if and only if the given path was null.
     */
    public static String encodeURI(final String path) {
        if (path == null) {
            return null;
        }
        StringBuilder buffer = null;
        final int length = path.length();
        for (int i=0; i<length;) {
            final int c = path.codePointAt(i);
            final int n = Character.charCount(c);
            if (!Character.isSpaceChar(c) && !Character.isISOControl(c) && c != '%') {
                /*
                 * The character is valid, or is punction character, or is a reserved character.
                 * All those characters should be handled properly by the URI(String) constructor.
                 */
                if (buffer != null) {
                    buffer.appendCodePoint(c);
                }
            } else {
                /*
                 * The character is invalid, so we need to escape it. Note that the encoding
                 * is fixed to UTF-8 as of java.net.URI specification (see its class javadoc).
                 */
                if (buffer == null) {
                    buffer = new StringBuilder(path);
                    buffer.setLength(i);
                }
                for (final byte b : path.substring(i, i+n).getBytes(StandardCharsets.UTF_8)) {
                    buffer.append('%');
                    final String hex = Integer.toHexString(b & 0xFF).toUpperCase(Locale.ROOT);
                    if (hex.length() < 2) {
                        buffer.append('0');
                    }
                    buffer.append(hex);
                }
            }
            i += n;
        }
        return (buffer != null) ? buffer.toString() : path;
    }

    /**
     * Converts a {@link URL} to a {@link URI}. This is equivalent to a call to the standard {@link URL#toURI()}
     * method, except for the following functionalities:
     *
     * <ul>
     *   <li>Optionally decodes the {@code "%XX"} sequences, where {@code "XX"} is a number.</li>
     *   <li>Converts various exceptions into subclasses of {@link IOException}.</li>
     * </ul>
     *
     * @param  url The URL to convert, or {@code null}.
     * @param  encoding If the URL is encoded in a {@code application/x-www-form-urlencoded}
     *         MIME format, the character encoding (normally {@code "UTF-8"}). If the URL is
     *         not encoded, then {@code null}.
     * @return The URI for the given URL, or {@code null} if the given URL was null.
     * @throws IOException if the URL can not be converted to a URI.
     *
     * @see URI#URI(String)
     */
    public static URI toURI(final URL url, final String encoding) throws IOException {
        if (url == null) {
            return null;
        }
        /*
         * Convert the URL to an URI, taking in account the encoding if any.
         *
         * Note: URL.toURI() is implemented as new URI(URL.toString()) where toString()
         * delegates to toExternalForm(), and all those methods are final. So we really
         * don't lost anything by doing those steps ourself.
         */
        String path = url.toExternalForm();
        if (encoding != null) {
            path = URLDecoder.decode(path, encoding);
        }
        path = encodeURI(path);
        try {
            return new URI(path);
        } catch (URISyntaxException cause) {
            /*
             * Occurs only if the URL is not compliant with RFC 2396. Otherwise every URL
             * should succeed, so a failure can actually be considered as a malformed URL.
             */
            final MalformedURLException e = new MalformedURLException(Exceptions.formatChainedMessages(
                    null, Errors.format(Errors.Keys.IllegalArgumentValue_2, "URL", path), cause));
            e.initCause(cause);
            throw e;
        }
    }

    /**
     * Converts a {@link URL} to a {@link File}. This is equivalent to a call to the standard
     * {@link URL#toURI()} method followed by a call to the {@link File#File(URI)} constructor,
     * except for the following functionalities:
     *
     * <ul>
     *   <li>Optionally decodes the {@code "%XX"} sequences, where {@code "XX"} is a number.</li>
     *   <li>Converts various exceptions into subclasses of {@link IOException}.</li>
     * </ul>
     *
     * @param  url The URL to convert, or {@code null}.
     * @param  encoding If the URL is encoded in a {@code application/x-www-form-urlencoded}
     *         MIME format, the character encoding (normally {@code "UTF-8"}). If the URL is
     *         not encoded, then {@code null}.
     * @return The file for the given URL, or {@code null} if the given URL was null.
     * @throws IOException if the URL can not be converted to a file.
     *
     * @see File#File(URI)
     */
    public static File toFile(final URL url, final String encoding) throws IOException {
        if (url == null) {
            return null;
        }
        final URI uri = toURI(url, encoding);
        /*
         * We really want to call the File constructor expecting a URI argument,
         * not the constructor expecting a String argument, because the one for
         * the URI argument performs additional platform-specific parsing.
         */
        try {
            return new File(uri);
        } catch (IllegalArgumentException cause) {
            /*
             * Typically happen when the URI scheme is not "file". But may also happen if the
             * URI contains fragment that can not be represented in a File (e.g. a Query part).
             * The IllegalArgumentException does not allow us to distinguish those cases.
             */
            throw new IOException(Exceptions.formatChainedMessages(null,
                    Errors.format(Errors.Keys.IllegalArgumentValue_2, "URL", url), cause), cause);
        }
    }

    /**
     * Parses the following path as a {@link File} if possible, or a {@link URL} otherwise.
     * In the special case where the given {@code path} is a URL using the {@code "file"} protocol,
     * the URL is converted to a {@link File} object using the given {@code encoding} for decoding
     * the {@code "%XX"} sequences, if any.
     *
     * <div class="section">Rational</div>
     * A URL can represent a file, but {@link URL#openStream()} appears to return a {@code BufferedInputStream}
     * wrapping the {@link FileInputStream}, which is not a desirable feature when we want to obtain a channel.
     *
     * @param  path The path to convert, or {@code null}.
     * @param  encoding If the URL is encoded in a {@code application/x-www-form-urlencoded}
     *         MIME format, the character encoding (normally {@code "UTF-8"}). If the URL is
     *         not encoded, then {@code null}. This argument is ignored if the given path does
     *         not need to be converted from URL to {@code File}.
     * @return The path as a {@link File} if possible, or a {@link URL} otherwise.
     * @throws IOException If the given path is not a file and can't be parsed as a URL.
     */
    public static Object toFileOrURL(final String path, final String encoding) throws IOException {
        if (path == null) {
            return null;
        }
        /*
         * Check if the path seems to be a local file. Those paths are assumed never encoded.
         * The heuristic rules applied here may change in any future SIS version.
         */
        if (path.indexOf('?') < 0 && path.indexOf('#') < 0) {
            final int s = path.indexOf(':');
            /*
             * If the ':' character is found, the part before it is probably a protocol in a URL,
             * except in the particular case where there is just one letter before ':'. In such
             * case, it may be the drive letter of a Windows file.
             */
            if (s<0 || (s==1 && Character.isLetter(path.charAt(0)) && !path.regionMatches(2, "//", 0, 2))) {
                return new File(path);
            }
        }
        final URL url = new URL(path);
        final String scheme = url.getProtocol();
        if (scheme != null && scheme.equalsIgnoreCase("file")) {
            return toFile(url, encoding);
        }
        /*
         * Leave the URL in its original encoding on the assumption that this is the encoding expected by
         * the server. This is different than the policy for URI, because the later are always in UTF-8.
         * If a URI is needed, callers should use toURI(url, encoding).
         */
        return url;
    }

    /**
     * Returns a byte channel from the given input, or {@code null} if the input is of unknown type.
     * More specifically:
     *
     * <ul>
     *   <li>If the given input is {@code null}, then this method returns {@code null}.</li>
     *   <li>If the given input is a {@link ReadableByteChannel} or an {@link InputStream},
     *       then it is returned directly, or indirectly as a wrapper.</li>
     *   <li>If the given input if a {@link Path}, {@link File}, {@link URL}, {@link URI}
     *       or {@link CharSequence}, then a new channel is opened.</li>
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
     * @param  input The file to open, or {@code null}.
     * @param  encoding If the input is an encoded URL, the character encoding (normally {@code "UTF-8"}).
     *         If the URL is not encoded, then {@code null}. This argument is ignored if the given input
     *         does not need to be converted from URL to {@code File}.
     * @param  options The options to use for creating a new byte channel. Can be null or empty for read-only.
     * @return The channel for the given input, or {@code null} if the given input is of unknown type.
     * @throws IOException If an error occurred while opening the given file.
     */
    public static ReadableByteChannel open(Object input, final String encoding, Object... options) throws IOException {
        /*
         * Unconditionally verify the options, even if we may not use them.
         */
        final Set<Object> optionSet;
        if (options == null || options.length == 0) {
            optionSet = Collections.emptySet();
        } else {
            optionSet = new HashSet<Object>(Arrays.asList(options));
            optionSet.add(StandardOpenOption.READ);
            if (optionSet.removeAll(ILLEGAL_OPTIONS)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2,
                        "options", Arrays.toString(options)));
            }
        }
        /*
         * Check for inputs that are already readable channels or input streams. We perform an explicit check
         * for FileInputStream even if  Channels.newChannel(InputStream)  does this check implicitly, because
         * Channels.newChannel(…) restricts itself to the exact FileInputStream class while we want to invoke
         * getChannel() for any subclasses.
         */
        if (input instanceof ReadableByteChannel) {
            return (ReadableByteChannel) input;
        }
        if (input instanceof InputStream) {
            if (input instanceof FileInputStream) {
                return ((FileInputStream) input).getChannel();
            }
            return Channels.newChannel((InputStream) input);
        }
        // NOTE: Many comments below this point actually apply to the JDK7 branch.
        //       We keep them here for making easier the synchonization between the branches.
        /*
         * In the following cases, we will try hard to convert to Path objects before to fallback
         * on File, URL or URI, because only Path instances allow us to use the given OpenOptions.
         */
        if (input instanceof URL) {
            try {
                input = toFile((URL) input, encoding);
            } catch (IOException e) {
                // This is normal if the URL uses HTTP or FTP protocol for instance.
                // Log the exception at FINE level without stack trace. We will open
                // the channel later using the URL instead than using the Path.
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
                // All methods invoked in this block throws IllegalArgumentException if the URI has no scheme,
                // so we are better to check now and provide a more appropriate exception for this method.
                throw new IOException(Errors.format(Errors.Keys.MissingSchemeInURI));
            } else try {
                input = new File(uri);
            } catch (IllegalArgumentException e) {
                input = uri.toURL();
                // We have been able to convert to URL, but the given OpenOptions may not be used.
                // Log the exception at FINE level without stack trace, because the exception is
                // probably a normal behavior in this context.
                recoverableException(e);
            }
        } else {
            if (input instanceof CharSequence) { // Needs to be before the check for File or URL.
                input = toFileOrURL(input.toString(), encoding);
            }
        }
        /*
         * One last check for URL. The URL may be either the given input if we have not been able
         * to convert it to a Path, or a URI, File or CharSequence input converted to URL. Do not
         * try to convert the URL to a Path, because this has already been tried before this point.
         */
        if (input instanceof URL) {
            return Channels.newChannel(((URL) input).openStream());
        }
        if (input instanceof File) {
            return Files.newByteChannel((File) input, optionSet);
        }
        return null;
    }

    /**
     * Invoked for reporting exceptions that may be normal behavior. This method logs
     * the exception at {@link java.util.logging.Level#FINE} without stack trace.
     */
    private static void recoverableException(final Exception warning) {
        Logging.recoverableException(Logging.getLogger(Modules.STORAGE), IOUtilities.class, "open", warning);
    }
}
