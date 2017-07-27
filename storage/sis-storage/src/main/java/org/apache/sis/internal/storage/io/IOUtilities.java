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

import java.util.Locale;
import java.io.File;
import java.io.FileInputStream;
import java.io.LineNumberReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.FileSystemNotFoundException;
import java.nio.charset.StandardCharsets;
import javax.imageio.stream.ImageInputStream;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Static;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.storage.Resources;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


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
 * @version 0.8
 * @since   0.3
 * @module
 */
public final class IOUtilities extends Static {
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
     * @param  path  the path as an instance of one of the above-cited types, or {@code null}.
     * @return the filename in the given path, or {@code null} if the given object is null or of unknown type.
     */
    public static String filename(final Object path) {
        return part(path, false);
    }

    /**
     * Returns the filename extension from a {@link Path}, {@link File}, {@link URL}, {@link URI} or
     * {@link CharSequence} instance. If no extension is found, returns an empty string. If the given
     * object is of unknown type, return {@code null}.
     *
     * @param  path  the path as an instance of one of the above-cited types, or {@code null}.
     * @return the extension in the given path, or an empty string if none, or {@code null}
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
        } else if (path instanceof Path) {
            name = ((Path) path).getFileName().toString();
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
            if (fromIndex <= 1) {
                // If the dot is the first character, do not consider as a filename extension.
                return "";
            }
        }
        return name.substring(fromIndex);
    }

    /**
     * Returns a string representation of the given path, or {@code null} if none. The current implementation
     * recognizes only the {@link Path}, {@link File}, {@link URL}, {@link URI} or {@link CharSequence} types.
     *
     * @param  path  the path for which to return a string representation.
     * @return the string representation, or {@code null} if none.
     */
    public static String toString(final Object path) {
        /*
         * For the following types, the string that we want can be obtained only by toString(),
         * or the class is final so we know that the toString(à behavior can not be changed.
         */
        if (path instanceof CharSequence || path instanceof Path || path instanceof URL || path instanceof URI) {
            return path.toString();
        }
        /*
         * While toString() would work too on the default implementation, the following
         * type is not final. So we are better to invoke the dedicated method.
         */
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
     * @param  path  the path to encode, or {@code null}.
     * @return the encoded path, or {@code null} if and only if the given path was null.
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
                    final String hex = Integer.toHexString(JDK8.toUnsignedInt(b)).toUpperCase(Locale.ROOT);
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
     * @param  url       the URL to convert, or {@code null}.
     * @param  encoding  if the URL is encoded in a {@code application/x-www-form-urlencoded} MIME format,
     *                   the character encoding (normally {@code "UTF-8"}). If the URL is not encoded,
     *                   then {@code null}.
     * @return the URI for the given URL, or {@code null} if the given URL was null.
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
            throw (MalformedURLException) new MalformedURLException(Exceptions.formatChainedMessages(null,
                    Errors.format(Errors.Keys.IllegalArgumentValue_2, "URL", path), cause)).initCause(cause);
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
     * @param  url       the URL to convert, or {@code null}.
     * @param  encoding  if the URL is encoded in a {@code application/x-www-form-urlencoded} MIME format,
     *                   the character encoding (normally {@code "UTF-8"}). If the URL is not encoded,
     *                   then {@code null}.
     * @return the file for the given URL, or {@code null} if the given URL was null.
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
     * Converts a {@link URL} to a {@link Path}. This is equivalent to a call to the standard
     * {@link URL#toURI()} method followed by a call to the {@link Paths#get(URI)} static method,
     * except for the following functionalities:
     *
     * <ul>
     *   <li>Optionally decodes the {@code "%XX"} sequences, where {@code "XX"} is a number.</li>
     *   <li>Converts various exceptions into subclasses of {@link IOException}.</li>
     * </ul>
     *
     * @param  url       the URL to convert, or {@code null}.
     * @param  encoding  if the URL is encoded in a {@code application/x-www-form-urlencoded} MIME format,
     *                   the character encoding (normally {@code "UTF-8"}). If the URL is not encoded,
     *                   then {@code null}.
     * @return the path for the given URL, or {@code null} if the given URL was null.
     * @throws IOException if the URL can not be converted to a path.
     *
     * @see Paths#get(URI)
     */
    public static Path toPath(final URL url, final String encoding) throws IOException {
        if (url == null) {
            return null;
        }
        final URI uri = toURI(url, encoding);
        try {
            return Paths.get(uri);
        } catch (IllegalArgumentException | FileSystemNotFoundException cause) {
            final String message = Exceptions.formatChainedMessages(null,
                    Errors.format(Errors.Keys.IllegalArgumentValue_2, "URL", url), cause);
            /*
             * If the exception is IllegalArgumentException, then the URI scheme has been recognized
             * but the URI syntax is illegal for that file system. So we can consider that the URL is
             * malformed in regard to the rules of that particular file system.
             */
            final IOException e;
            if (cause instanceof IllegalArgumentException) {
                e = new MalformedURLException(message);
                e.initCause(cause);
            } else {
                e = new IOException(message, cause);
            }
            throw e;
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
     * @param  path      the path to convert, or {@code null}.
     * @param  encoding  if the URL is encoded in a {@code application/x-www-form-urlencoded} MIME format,
     *                   the character encoding (normally {@code "UTF-8"}). If the URL is not encoded,
     *                   then {@code null}. This argument is ignored if the given path does not need
     *                   to be converted from URL to {@code File}.
     * @return the path as a {@link File} if possible, or a {@link URL} otherwise.
     * @throws IOException if the given path is not a file and can't be parsed as a URL.
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
     * Converts the given output stream to an input stream. It is caller's responsibility to flush
     * the stream and reset its position to the beginning of file before to invoke this method.
     * The data read by the input stream will be the data that have been written in the output stream
     * before this method is invoked.
     *
     * <p>The given output stream should not be used anymore after this method invocation, but should
     * not be closed neither since the returned input stream may be backed by the same channel.</p>
     *
     * @param  stream  the input or output stream to converts to an {@code InputStream}.
     * @return the input stream, or {@code null} if the given stream can not be converted.
     * @throws IOException if an error occurred during input stream creation.
     *
     * @since 0.8
     */
    public static InputStream toInputStream(AutoCloseable stream) throws IOException {
        if (stream != null) {
            if (stream instanceof InputStream) {
                return (InputStream) stream;
            }
            if (stream instanceof OutputStreamAdapter) {
                stream = ((OutputStreamAdapter) stream).output;
            }
            if (stream instanceof ChannelDataOutput) {
                final ChannelDataOutput c = (ChannelDataOutput) stream;
                if (c.channel instanceof ReadableByteChannel) {
                    stream = new ChannelImageInputStream(c.filename, (ReadableByteChannel) c.channel, c.buffer, true);
                }
            }
            if (stream instanceof ImageInputStream) {
                return new InputStreamAdapter((ImageInputStream) stream);
            }
        }
        return null;
    }

    /**
     * Converts the given input stream to an output stream. It is caller's responsibility to reset
     * the stream position to the beginning of file before to invoke this method. The data written
     * by the output stream will overwrite the previous data, but the caller may need to
     * {@linkplain #truncate truncate} the output stream after he finished to write in it.
     *
     * <p>The given input stream should not be used anymore after this method invocation, but should
     * not be closed neither since the returned output stream may be backed by the same channel.</p>
     *
     * @param  stream  the input or output stream to converts to an {@code OutputStream}.
     * @return the output stream, or {@code null} if the given stream can not be converted.
     * @throws IOException if an error occurred during output stream creation.
     *
     * @since 0.8
     */
    public static OutputStream toOutputStream(AutoCloseable stream) throws IOException {
        if (stream != null) {
            if (stream instanceof OutputStream) {
                return (OutputStream) stream;
            }
            if (stream instanceof InputStreamAdapter) {
                stream = ((InputStreamAdapter) stream).input;
            }
            if (stream instanceof ChannelDataInput) {
                final ChannelDataInput c = (ChannelDataInput) stream;
                if (c.channel instanceof WritableByteChannel) {
                    stream = new ChannelImageOutputStream(c.filename, (WritableByteChannel) c.channel, c.buffer);
                }
            }
            if (stream instanceof ChannelImageOutputStream) {
                return new OutputStreamAdapter((ChannelImageOutputStream) stream);
            }
        }
        return null;
    }

    /**
     * Truncates the given output stream at its current position.
     * This method works with Apache SIS implementations backed (sometime indirectly) by {@link SeekableByteChannel}.
     * Callers may need to {@linkplain java.io.Flushable#flush() flush} the stream before to invoke this method.
     *
     * @param  stream  the output stream or writable channel to truncate.
     * @return whether this method has been able to truncate the given stream.
     * @throws IOException if an error occurred while truncating the stream.
     */
    public static boolean truncate(AutoCloseable stream) throws IOException {
        if (stream instanceof OutputStreamAdapter) {
            stream = ((OutputStreamAdapter) stream).output;
        }
        if (stream instanceof ChannelDataOutput) {
            stream = ((ChannelDataOutput) stream).channel;
        }
        if (stream instanceof SeekableByteChannel) {
            final SeekableByteChannel s = (SeekableByteChannel) stream;
            s.truncate(s.position());
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the given options would open a file mostly for writing.
     * This method returns {@code true} if the following conditions are true:
     *
     * <ul>
     *   <li>The array contains {@link StandardOpenOption#WRITE}.</li>
     *   <li>The array does not contain {@link StandardOpenOption#READ}, unless the array contains also
     *       {@link StandardOpenOption#CREATE_NEW} or {@link StandardOpenOption#TRUNCATE_EXISTING} in which
     *       case the {@code READ} option is ignored (because the caller would have no data to read).</li>
     * </ul>
     *
     * @param  options  the open options to check, or {@code null} if none.
     * @return {@code true} if a file opened with the given options would be mostly for write operations.
     *
     * @since 0.8
     */
    public static boolean isWrite(final OpenOption[] options) {
        boolean isRead   = false;
        boolean isWrite  = false;
        boolean truncate = false;
        if (options != null) {
            for (final OpenOption op : options) {
                if (op instanceof StandardOpenOption) {
                    switch ((StandardOpenOption) op) {
                        case READ:              isRead   = true; break;
                        case WRITE:             isWrite  = true; break;
                        case CREATE_NEW:
                        case TRUNCATE_EXISTING: truncate = true; break;
                    }
                }
            }
        }
        return isWrite & (!isRead | truncate);
    }

    /**
     * Returns the error message for a file that can not be parsed.
     * The error message will contain the line number if available.
     *
     * @param  locale    the language for the error message.
     * @param  format    abbreviation of the file format (e.g. "CSV", "GML", "WKT", <i>etc</i>).
     * @param  filename  name of the file or the data store.
     * @param  store     the input or output object, or {@code null}.
     * @return the parameters for a localized error message for a file that can not be processed.
     *
     * @since 0.8
     */
    public static String canNotReadFile(final Locale locale, final String format, final String filename, final Object store) {
        final Object[] parameters = errorMessageParameters(format, filename, store);
        return Resources.forLocale(locale).getString(errorMessageKey(parameters), parameters);
    }

    /**
     * Returns the {@link Resources.Keys} value together with the parameters given by {@code errorMessageParameters(…)}.
     *
     * @param   parameters  the result of {@code errorMessageParameters(…)} method call.
     * @return  the {@link Resources.Keys} value to use for formatting the error message.
     *
     * @since 0.8
     */
    public static short errorMessageKey(final Object[] parameters) {
        return (parameters.length == 2) ? Resources.Keys.CanNotReadFile_2 :
               (parameters.length == 3) ? Resources.Keys.CanNotReadFile_3 :
                                          Resources.Keys.CanNotReadFile_4;
    }

    /**
     * Returns the parameters for an error message saying that an error occurred while processing a file.
     * This method uses the information provided by methods like {@link LineNumberReader#getLineNumber()}
     * or {@link XMLStreamReader#getLocation()} if the given {@code store} is one of the supported types.
     *
     * @param  format    abbreviation of the file format (e.g. "CSV", "GML", "WKT", <i>etc</i>).
     * @param  filename  name of the file or the data store.
     * @param  store     the input or output object, or {@code null}.
     * @return the parameters for a localized error message for a file that can not be processed.
     *
     * @since 0.8
     */
    @SuppressWarnings("fallthrough")
    public static Object[] errorMessageParameters(final String format, final String filename, final Object store) {
        int line   = 0;
        int column = 0;
        if (store instanceof XMLStreamReader) {
            final Location location = ((XMLStreamReader) store).getLocation();
            line   = location.getLineNumber()   + 1;
            column = location.getColumnNumber() + 1;
        } else if (store instanceof LineNumberReader) {
            line = ((LineNumberReader) store).getLineNumber();
        }
        final Object[] params = new Object[(line == 0) ? 2 : (column == 0) ? 3 : 4];
        switch (params.length) {
            default: // Fallthrough everywhere
            case 4:  params[3] = column;
            case 3:  params[2] = line;
            case 2:  params[1] = filename;
            case 1:  params[0] = format;
            case 0:  break;
        }
        return params;
    }
}
