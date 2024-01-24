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
package org.apache.sis.storage.base;

import java.util.Arrays;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.charset.Charset;
import javax.xml.transform.Source;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.io.stream.IOUtilities;


/**
 * Content of a data store auxiliary file.
 * Instances of this class should be short lived, because they hold larger arrays than necessary.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see PRJDataStore#readAuxiliaryFile(String, boolean)
 */
public final class AuxiliaryContent implements CharSequence {
    /**
     * Buffer size and initial array capacity.
     * We take a small buffer size because the files to read are typically small.
     */
    static final int BUFFER_SIZE = 1024;

    /**
     * Maximal length (in bytes) of auxiliary files. This is an arbitrary restriction, we could let
     * the buffer growth indefinitely instead. But a large auxiliary file is probably an error and
     * we do not want an {@link OutOfMemoryError} because of that.
     */
    private static final int MAXIMAL_LENGTH = 64 * 1024;

    /**
     * The XML source, or null if the file has not been identified as an XML file.
     * This can be non-null only if the auxiliary file has been read with an
     * {@code acceptXML} parameter set to {@code true}.
     */
    public final Source source;

    /**
     * {@link Path} or {@link URL} that have been read.
     */
    private final Object location;

    /**
     * The textual content of the auxiliary file.
     */
    private final char[] buffer;

    /**
     * Index of the first valid character in {@link #buffer}.
     */
    private final int offset;

    /**
     * Number of valid characters in {@link #buffer}.
     */
    private final int length;

    /**
     * Creates an auxiliary content for an XML file.
     *
     * @param location  {@link Path} or {@link URL} to read.
     * @param source    the source to use for parsing the XML file.
     */
    AuxiliaryContent(final Object location, final Source source) {
        this.source   = source;
        this.location = location;
        this.buffer   = ArraysExt.EMPTY_CHAR;
        this.offset   = 0;
        this.length   = 0;
    }

    /**
     * Wraps (without copying) the given array as the content of an auxiliary file.
     *
     * @param location  {@link Path} or {@link URL} that have been read.
     * @param buffer    the textual content of the auxiliary file.
     * @param offset    index of the first valid character in {@code buffer}.
     * @param length    number of valid characters in {@code buffer}.
     */
    private AuxiliaryContent(final Object location, final char[] buffer, final int offset, final int length) {
        this.source   = null;
        this.location = location;
        this.buffer   = buffer;
        this.offset   = offset;
        this.length   = length;
    }

    /**
     * Reads the content of the given input stream.
     * This method closes the given stream.
     *
     * @param  location  {@link Path} or {@link URL} to read.
     * @param  stream    input stream to read.
     * @param  encoding  character encoding, or {@code null} for the default.
     * @return the file content, or {@code null} if too large.
     * @throws IOException if an error occurred while reading the stream.
     */
    static AuxiliaryContent read(final Object location, final InputStream stream, final Charset encoding) throws IOException {
        try (InputStreamReader reader = (encoding != null)
                ? new InputStreamReader(stream, encoding)
                : new InputStreamReader(stream))
        {
            char[] buffer = new char[BUFFER_SIZE];
            int offset = 0, count;
            while ((count = reader.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += count;
                if (offset >= buffer.length) {
                    if (offset >= MAXIMAL_LENGTH) return null;
                    buffer = Arrays.copyOf(buffer, offset*2);
                }
            }
            return new AuxiliaryContent(location, buffer, 0, offset);
        }
    }

    /**
     * Returns the filename (without path) of the auxiliary file.
     * This information is mainly for producing error messages.
     *
     * @return name of the auxiliary file that have been read.
     */
    public String getFilename() {
        return IOUtilities.filename(location);
    }

    /**
     * Returns the source as an URI if possible.
     *
     * @return the source as an URI, or {@code null} if none.
     * @throws URISyntaxException if the URI cannot be parsed.
     */
    public URI getURI() throws URISyntaxException {
        return IOUtilities.toURI(location);
    }

    /**
     * Returns the number of valid characters in this sequence.
     */
    @Override
    public int length() {
        return length;
    }

    /**
     * Returns the character at the given index. For performance reasons this method does not check index bounds.
     * The behavior of this method is undefined if the given index is not smaller than {@link #length()}.
     * We skip bounds check because this class should be used for Apache SIS internal purposes only.
     */
    @Override
    public char charAt(final int index) {
        return buffer[offset + index];
    }

    /**
     * Returns a sub-sequence of this auxiliary file content. For performance reasons this method does not
     * perform bound checks. The behavior of this method is undefined if arguments are out of bounds.
     * We skip bounds check because this class should be used for Apache SIS internal purposes only.
     */
    @Override
    public CharSequence subSequence(final int start, final int end) {
        return new AuxiliaryContent(location, buffer, offset + start, end - start);
    }

    /**
     * Copies this auxiliary file content in a {@link String}.
     * This method does not cache the result; caller should invoke at most once.
     */
    @Override
    public String toString() {
        return new String(buffer, offset, length);
    }
}
