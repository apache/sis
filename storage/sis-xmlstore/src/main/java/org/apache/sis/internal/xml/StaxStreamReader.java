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
package org.apache.sis.internal.xml;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.io.EOFException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.system.XMLInputFactory;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Classes;


/**
 * Base class of Apache SIS readers of XML files using STAX parser.
 * This is a helper class for {@link org.apache.sis.storage.DataStore} implementations.
 * Readers for a given specification should extend this class and provide appropriate read methods.
 *
 * <p>Example:</p>
 * {@preformat java
 *     public class UserObjectReader extends StaxStreamReader {
 *         public UserObject read() throws XMLStreamException {
 *             // Actual STAX read operations.
 *             return userObject;
 *         }
 *     }
 * }
 *
 * And should be used like below:
 *
 * {@preformat java
 *     UserObject obj;
 *     try (UserObjectReader reader = new UserObjectReader(input)) {
 *         obj = instance.read();
 *     }
 * }
 *
 * <div class="section">Multi-threading</div>
 * This class and subclasses are not tread-safe. Synchronization shall be done by the {@code DataStore}
 * that contains the {@code StaxStreamReader} instance.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public abstract class StaxStreamReader extends StaxStream {
    /**
     * The XML stream reader.
     */
    private XMLStreamReader reader;

    /**
     * The underlying reader or input stream to close when this {@code StaxStreamReader} is closed,
     * or {@code null} if none.
     */
    private Closeable sourceStream;

    /**
     * Creates a new XML reader from the given file, URL, stream or reader object.
     * This constructor is used in two ways depending on whether the optional {@code storage} argument is null or not:
     *
     * <ul class="verbose">
     *   <li>If the {@code storage} argument is {@code null}, then the {@code input} argument shall be an instance of
     *       {@link javax.xml.stream.XMLStreamReader}, {@link javax.xml.stream.XMLEventReader},
     *       {@link org.xml.sax.InputSource}, {@link java.io.InputStream}, {@link java.io.Reader},
     *       {@link javax.xml.transform.Source} or {@link org.w3c.dom.Node},
     *       otherwise a {@link DataStoreException} will be thrown.</li>
     *
     *   <li>If the {@code storage} argument is {@code null}, then the {@code input} argument should be the value of
     *       {@link StorageConnector#getStorage()} (i.e. the input object as given by the user). If that value is not
     *       recognized, then this constructor will fallback on {@link StorageConnector#getStorageAs(Class)} with the
     *       {@link InputStream} type. In any cases, this constructor invokes
     *       {@link StorageConnector#closeAllExcept(Object)} after the input has been set.</li>
     * </ul>
     *
     * @param  input   value of {@code storage.getStorage()}.
     * @param  storage information about the storage (URL, stream, <i>etc</i>), or {@code null} if unknown.
     * @throws DataStoreException if the input type is not recognized.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     */
    protected StaxStreamReader(final Object input, final StorageConnector storage) throws DataStoreException, XMLStreamException {
        ArgumentChecks.ensureNonNull("input", input);
        reader = XMLInputFactory.createFromAny(input);
        if (reader != null) {
            if (input instanceof Closeable) {
                sourceStream = (Closeable) input;
            }
            if (storage != null) {
                storage.closeAllExcept(input);
            }
        } else {
            final InputStream in;
            if (storage != null && (in = storage.getStorageAs(InputStream.class)) != null) {
                reader = XMLInputFactory.createXMLStreamReader(in);
                sourceStream = in;
                storage.closeAllExcept(in);
            } else {
                throw new DataStoreException(Errors.format(Errors.Keys.IllegalInputTypeForReader_2, "XML", Classes.getClass(input)));
            }
        }
    }

    /**
     * Returns the XML stream reader if it is not closed.
     *
     * @return the XML stream reader (never null).
     * @throws IOException if this XML reader has been closed.
     */
    protected final XMLStreamReader getReader() throws IOException {
        if (reader != null) {
            return reader;
        }
        throw new IOException(errors().getString(Errors.Keys.ClosedReader_1, "XML"));
    }

    /**
     * Returns a XML stream reader over only a portion of the document, from given position inclusive
     * until the end of the given element exclusive. Nested elements of the same name, if any, will be
     * ignored.
     *
     * @param  tagName name of the tag to close.
     * @return a reader over a portion of the stream.
     * @throws IOException if this XML reader has been closed.
     */
    protected final XMLStreamReader getSubReader(final String tagName) throws IOException {
        return new StreamReaderDelegate(getReader()) {
            /** Increased every time a nested element of the same name is found. */
            private int nested;

            /** Returns {@code false} if we reached the end of the sub-region. */
            @Override public boolean hasNext() throws XMLStreamException {
                return (nested >= 0) && super.hasNext();
            }

            /** Reads the next element in the sub-region. */
            @Override public int next() throws XMLStreamException {
                if (nested < 0) {
                    throw new NoSuchElementException();
                }
                final int t = super.next();
                switch (t) {
                    case START_ELEMENT: if (tagName.equals(getLocalName())) nested++; break;
                    case END_ELEMENT:   if (tagName.equals(getLocalName())) nested--; break;
                }
                return t;
            }
        };
    }

    /**
     * Skips all remaining elements until we reach the end of the given tag.
     * Nested tags of the same name, if any, are also skipped.
     *
     * @param  tagName name of the tag to close.
     * @throws IOException if this XML reader has been closed.
     * @throws EOFException if end tag could not be found.
     * @throws XMLStreamException if an error occurred while reading the XML stream.
     */
    protected final void skipUntilEnd(final String tagName) throws IOException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        int nested = 0;
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT: {
                    if (tagName.equals(reader.getLocalName())) {
                        nested++;
                    }
                    break;
                }
                case XMLStreamReader.END_ELEMENT: {
                    if (tagName.equals(reader.getLocalName())) {
                        if (--nested < 0) return;
                    }
                    break;
                }
            }
        }
        throw new EOFException(errors().getString(Errors.Keys.UnexpectedEndOfFile_1, tagName));
    }

    /**
     * Parses the given string as a boolean value. This method performs the same parsing than
     * {@link Boolean#parseBoolean(String)} with one extension: the "0" value is considered
     * as {@code false} and the "1" value as {@code true}.
     *
     * @param value The string value to parse as a boolean.
     * @return true if the boolean is equal to "true" or "1".
     */
    protected static boolean parseBoolean(final String value) {
        if (value.length() == 1) {
            switch (value.charAt(0)) {
                case '0': return false;
                case '1': return true;
            }
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Closes the input stream and releases any resources used by this XML reader.
     * This reader can not be used anymore after this method has been invoked.
     *
     * @throws IOException if an error occurred while closing the input stream.
     * @throws XMLStreamException if an error occurred while releasing XML reader/writer resources.
     */
    @Override
    public void close() throws IOException, XMLStreamException {
        if (reader != null) {
            reader.close();
            reader = null;
        }
        if (sourceStream != null) {
            sourceStream.close();
            sourceStream = null;
        }
    }
}
