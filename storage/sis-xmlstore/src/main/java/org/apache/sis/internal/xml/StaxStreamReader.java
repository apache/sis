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

import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.NoSuchElementException;
import java.net.URI;
import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.net.URISyntaxException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.bind.JAXBException;
import org.xml.sax.InputSource;
import org.w3c.dom.Node;
import org.apache.sis.xml.XML;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.util.StandardDateFormat;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;

// Branch-dependent imports
import java.util.Spliterator;
import java.util.function.Consumer;
import java.time.temporal.Temporal;
import java.time.format.DateTimeParseException;
import org.opengis.feature.Feature;


/**
 * Base class of Apache SIS readers of XML files using STAX parser.
 * This class is itself an spliterator over all {@code Feature} instances found in the XML file,
 * with the following restrictions:
 *
 * <ul>
 *   <li>{@link #tryAdvance(Consumer)} shall returns the features in the order they are declared in the XML file.</li>
 *   <li>{@code tryAdvance(Consumer)} shall not return {@code null} value.</li>
 *   <li>Modifications of the XML file are not allowed while an iteration is in progress.</li>
 *   <li>A {@code StaxStreamReader} instance can iterate over the features only once;
 *       if a new iteration is wanted, a new {@code StaxStreamReader} instance must be created.</li>
 * </ul>
 *
 * This is a helper class for {@link org.apache.sis.storage.DataStore} implementations.
 * Readers for a given specification should extend this class and provide the following methods:
 *
 * <p>Example:</p>
 * {@preformat java
 *     public class UserObjectReader extends StaxStreamReader {
 *         public boolean tryAdvance(Consumer<? super Feature> action) throws BackingStoreException {
 *             if (endOfFile) {
 *                 return false;
 *             }
 *             Feature f = ...;         // Actual STAX read operations.
 *             action.accept(f);
 *             return true;
 *         }
 *     }
 * }
 *
 * And can be used like below:
 *
 * {@preformat java
 *     Consumer<Feature> consumer = ...;
 *     try (UserObjectReader reader = new UserObjectReader(input)) {
 *         reader.forEachRemaining(consumer);
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
public abstract class StaxStreamReader extends StaxStreamIO implements XMLStreamConstants, Spliterator<Feature> {
    /**
     * The XML stream reader.
     */
    private XMLStreamReader reader;

    /**
     * Input name (typically filename) for formatting error messages.
     */
    protected final String inputName;

    /**
     * Creates a new XML reader from the given file, URL, stream or reader object.
     *
     * @param  owner      the data store for which this reader is created.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if the input type is not recognized.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     */
    protected StaxStreamReader(final StaxDataStore owner, final StorageConnector connector)
            throws DataStoreException, XMLStreamException
    {
        super(owner);
        inputName = connector.getStorageName();
        final Object input = connector.getStorage();
        if      (input instanceof XMLStreamReader) reader = (XMLStreamReader) input;
        else if (input instanceof XMLEventReader)  reader = factory().createXMLStreamReader(new StAXSource((XMLEventReader) input));
        else if (input instanceof InputSource)     reader = factory().createXMLStreamReader(new SAXSource((InputSource) input));
        else if (input instanceof InputStream)     reader = factory().createXMLStreamReader((InputStream) input);
        else if (input instanceof Reader)          reader = factory().createXMLStreamReader((Reader) input);
        else if (input instanceof Source)          reader = factory().createXMLStreamReader((Source) input);
        else if (input instanceof Node)            reader = factory().createXMLStreamReader(new DOMSource((Node) input));
        else {
            final InputStream in = connector.getStorageAs(InputStream.class);
            connector.closeAllExcept(in);
            if (in == null) {
                throw new UnsupportedStorageException(errors().getString(Errors.Keys.IllegalInputTypeForReader_2,
                        owner.getFormatName(), Classes.getClass(input)));
            }
            reader = factory().createXMLStreamReader(in);
            initCloseable(in);
            return;
        }
        initCloseable(input);
        connector.closeAllExcept(input);
    }

    /**
     * Returns the characteristics of the iteration over feature instances.
     * The iteration is assumed {@link #ORDERED} in the declaration order in the XML file.
     * The iteration is {@link #NONNULL} (i.e. {@link #tryAdvance(Consumer)} is not allowed
     * to return null value) and {@link #IMMUTABLE} (i.e. we do not support modification of
     * the XML file while an iteration is in progress).
     *
     * @return characteristics of iteration over the features in the XML file.
     */
    @Override
    public int characteristics() {
        return ORDERED | NONNULL | IMMUTABLE;
    }

    /**
     * Performs the given action on the next feature instance, or returns {@code null} if there is no more
     * feature to parse.
     *
     * @param  action  the action to perform on the next feature instances.
     * @return {@code true} if a feature has been found, or {@code false} if we reached the end of XML file.
     * @throws BackingStoreException if an error occurred while parsing the next feature instance.
     *         The cause may be {@link DataStoreException}, {@link IOException}, {@link URISyntaxException}
     *         or various {@link RuntimeException} among others.
     */
    @Override
    public abstract boolean tryAdvance(Consumer<? super Feature> action) throws BackingStoreException;

    /**
     * Returns {@code null} by default since non-binary XML files are hard to split.
     *
     * @return {@code null}.
     */
    @Override
    public Spliterator<Feature> trySplit() {
        return null;
    }

    /**
     * Returns the sentinel value meaning that the number of elements is too expensive to compute.
     *
     * @return {@link Long#MAX_VALUE}.
     */
    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }




    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                ////////
    ////////                Convenience methods for subclass implementations                ////////
    ////////                                                                                ////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Convenience method invoking {@link StaxDataStore#inputFactory()}.
     */
    private XMLInputFactory factory() {
        return owner.inputFactory();
    }

    /**
     * Returns the XML stream reader if it is not closed.
     *
     * @return the XML stream reader (never null).
     * @throws XMLStreamException if this XML reader has been closed.
     */
    protected final XMLStreamReader getReader() throws XMLStreamException {
        if (reader != null) {
            return reader;
        }
        throw new XMLStreamException(errors().getString(Errors.Keys.ClosedReader_1, owner.getFormatName()));
    }

    /**
     * Returns a XML stream reader over only a portion of the document, from given position inclusive
     * until the end of the given element exclusive. Nested elements of the same name, if any, will be
     * ignored.
     *
     * @param  tagName name of the tag to close.
     * @return a reader over a portion of the stream.
     * @throws XMLStreamException if this XML reader has been closed.
     */
    protected final XMLStreamReader getSubReader(final QName tagName) throws XMLStreamException {
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
                    case START_ELEMENT: if (tagName.equals(getName())) nested++; break;
                    case END_ELEMENT:   if (tagName.equals(getName())) nested--; break;
                }
                return t;
            }
        };
    }

    /**
     * Moves the cursor the the first start element and verifies that it is the expected element.
     * This method is useful for skipping comments, entity declarations, <i>etc.</i> before the root element.
     *
     * <p>If the reader is already on a start element, then this method does not move forward.
     * Once a root element has been found, this method verifies that the namespace and local name
     * are the expected ones, or throws an exception otherwise.</p>
     *
     * @param  isNamespace  a predicate receiving the namespace in argument (which may be null)
     *                      and returning whether that namespace is the expected one.
     * @param  localName    the expected name of the root element.
     * @throws EOFException if no start element has been found before we reached the end of file.
     * @throws XMLStreamException if an error occurred while reading the XML stream.
     * @throws DataStoreContentException if the root element is not the expected one.
     */
    protected final void moveToRootElement(final Predicate<String> isNamespace, final String localName)
            throws EOFException, XMLStreamException, DataStoreContentException
    {
        final XMLStreamReader reader = getReader();
        if (!reader.isStartElement()) {
            do if (!reader.hasNext()) {
                throw new EOFException(endOfFile());
            } while (reader.next() != START_ELEMENT);
        }
        if (!isNamespace.test(reader.getNamespaceURI()) || !localName.equals(reader.getLocalName())) {
            throw new DataStoreContentException(errors().getString(
                    Errors.Keys.UnexpectedFileFormat_2, owner.getFormatName(), inputName));
        }
    }

    /**
     * Skips all remaining elements until we reach the end of the given tag.
     * Nested tags of the same name, if any, are also skipped.
     * After this method invocation, the current event is {@link #END_DOCUMENT}.
     *
     * @param  tagName name of the tag to close.
     * @throws EOFException if end tag could not be found.
     * @throws XMLStreamException if an error occurred while reading the XML stream.
     */
    protected final void skipUntilEnd(final QName tagName) throws EOFException, XMLStreamException {
        final XMLStreamReader reader = getReader();
        int nested = 0;
        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: {
                    if (tagName.equals(reader.getName())) {
                        nested++;
                    }
                    break;
                }
                case END_ELEMENT: {
                    if (tagName.equals(reader.getName())) {
                        if (--nested < 0) return;
                    }
                    break;
                }
            }
        }
        throw new EOFException(endOfFile());
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()},
     * or {@code null} if that value is null or empty.
     *
     * @return the current text element, or {@code null} if empty.
     * @throws XMLStreamException if a text element can not be returned.
     */
    protected final String getElementText() throws XMLStreamException {
        String text = getReader().getElementText();
        if (text != null) {
            text = text.trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()} as an URI,
     * or {@code null} if that value is null or empty.
     *
     * @return the current text element as an URI, or {@code null} if empty.
     * @throws XMLStreamException if a text element can not be returned.
     * @throws URISyntaxException if the text can not be parsed as an URI.
     */
    protected final URI getElementAsURI() throws XMLStreamException, URISyntaxException {
        final Context context = Context.current();
        return Context.converter(context).toURI(context, getElementText());
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()} as an integer,
     * or {@code null} if that value is null or empty.
     *
     * @return the current text element as an integer, or {@code null} if empty.
     * @throws XMLStreamException if a text element can not be returned.
     * @throws NumberFormatException if the text can not be parsed as an integer.
     */
    protected final Integer getElementAsInteger() throws XMLStreamException {
        final String text = getElementText();
        return (text != null) ? Integer.valueOf(text) : null;
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()} as a floating point number,
     * or {@code null} if that value is null or empty.
     *
     * @return the current text element as a floating point number, or {@code null} if empty.
     * @throws XMLStreamException if a text element can not be returned.
     * @throws NumberFormatException if the text can not be parsed as a floating point number.
     */
    protected final Double getElementAsDouble() throws XMLStreamException {
        final String text = getElementText();
        return (text != null) ? Numerics.valueOf(Double.parseDouble(text)) : null;
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()} as a date,
     * or {@code null} if that value is null or empty.
     *
     * @return the current text element as a date, or {@code null} if empty.
     * @throws XMLStreamException if a text element can not be returned.
     * @throws DateTimeParseException if the text can not be parsed as a date.
     */
    protected final Date getElementAsDate() throws XMLStreamException {
        final String text = getElementText();
        return (text != null) ? StandardDateFormat.toDate(StandardDateFormat.FORMAT.parse(text)) : null;
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()} as a temporal object,
     * or {@code null} if that value is null or empty.
     *
     * @return the current text element as a temporal object, or {@code null} if empty.
     * @throws XMLStreamException if a text element can not be returned.
     * @throws DateTimeParseException if the text can not be parsed as a date.
     */
    protected final Temporal getElementAsTemporal() throws XMLStreamException {
        return StandardDateFormat.parseBest(getElementText());
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()} as a list of strings,
     * or {@code null} if that value is null or empty.
     *
     * @return the current text element as a list.
     * @throws XMLStreamException if a text element can not be returned.
     */
    protected final List<String> getElementAsList() throws XMLStreamException {
        final String text = getElementText();
        return (text != null) ? Arrays.asList(text.split(" ")) : null;
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
     * Delegates to JAXB the unmarshalling of a part of XML document, starting from the current element (inclusive).
     *
     * @param  <T>   compile-time value of the {@code type} argument.
     * @param  type  expected type of the object to unmarshal.
     * @return the unmarshalled object, or {@code null} if none.
     * @throws XMLStreamException if the XML stream is closed.
     * @throws JAXBException if an error occurred during unmarshalling.
     * @throws ClassCastException if the unmarshalling result is not of the expected type.
     *
     * @see javax.xml.bind.Unmarshaller#unmarshal(XMLStreamReader, Class)
     */
    protected final <T> T unmarshal(final Class<T> type) throws XMLStreamException, JAXBException {
        return XML.unmarshal(new StAXSource(getReader()), type, owner.configuration(this)).getValue();
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
        super.close();
    }

    /**
     * Returns an error message for {@link EOFException}.
     * This a convenience method for a frequently-used error.
     *
     * @return a localized error message for end of file error.
     */
    protected final String endOfFile() {
        return errors().getString(Errors.Keys.UnexpectedEndOfFile_1, inputName);
    }

    /**
     * Returns an error message for {@link BackingStoreException}.
     * This a convenience method for {@link #tryAdvance(Consumer)} implementations.
     *
     * @return a localized error message for a file that can not be parsed.
     */
    protected final String canNotReadFile() {
        return errors().getString(Errors.Keys.CanNotParseFile_2, owner.getFormatName(), inputName);
    }

    /**
     * Returns an error message saying that nested elements are not allowed.
     *
     * @param  name  the name of the nested element found.
     * @return a localized error message for forbidden nested element.
     */
    protected final String nestedElement(final String name) {
        return errors().getString(Errors.Keys.NestedElementNotAllowed_1, name);
    }
}
