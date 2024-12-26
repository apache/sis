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
package org.apache.sis.storage.xml.stream;

import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.time.temporal.Temporal;
import java.time.format.DateTimeParseException;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.io.EOFException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
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
 * Readers for a given specification should extend this class and implement methods as
 * in the following example:
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 *     public class UserObjectReader extends StaxStreamReader {
 *         UserObjectReader(StaxDataStore owner) throws ... {
 *             super(owner);
 *         }
 *
 *         @Override
 *         public boolean tryAdvance(Consumer<? super Feature> action) throws BackingStoreException {
 *             if (endOfFile) {
 *                 return false;
 *             }
 *             Feature f = ...;         // Actual STAX read operations.
 *             action.accept(f);
 *             return true;
 *         }
 *     }
 *     }
 *
 * Readers can be used like below:
 *
 * {@snippet lang="java" :
 *     Consumer<Feature> consumer = ...;
 *     try (UserObjectReader reader = new UserObjectReader(dataStore)) {
 *         reader.forEachRemaining(consumer);
 *     }
 *     }
 *
 * <h2>Multi-threading</h2>
 * This class and subclasses are not tread-safe. Synchronization shall be done by the {@code DataStore}
 * that contains the {@code StaxStreamReader} instance.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class StaxStreamReader extends StaxStreamIO implements XMLStreamConstants, Spliterator<Feature>, Runnable {
    /**
     * The XML stream reader.
     */
    protected final XMLStreamReader reader;

    /**
     * {@code true} if the {@link #reader} already moved to the next element. This happen if {@link #unmarshal(Class)}
     * has been invoked. In such case, the next call to {@link XMLStreamReader#next()} needs to be replaced by a call
     * to {@link XMLStreamReader#getEventType()}.
     */
    private boolean isNextDone;

    /**
     * The unmarshaller reserved to this reader usage,
     * created only when first needed and kept until this reader is closed.
     *
     * @see #unmarshal(Class)
     */
    private Unmarshaller unmarshaller;

    /**
     * Creates a new XML reader for the given data store.
     *
     * @param  owner  the data store for which this reader is created.
     * @throws DataStoreException if the input type is not recognized or the data store is closed.
     * @throws XMLStreamException if an error occurred while opening the XML file.
     * @throws IOException if an error occurred while preparing the input stream.
     * @throws Exception if another kind of error occurred while closing a previous stream.
     */
    protected StaxStreamReader(final StaxDataStore owner) throws Exception {
        super(owner);
        reader = owner.createReader(this);      // Okay because will not store the `this` reference.
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
     * Performs the given action on the next feature instance, or returns {@code null} if there are no more
     * features to parse.
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




    //  ╔════════════════════════════════════════════════════════════════════════════════╗
    //  ║                                                                                ║
    //  ║                Convenience methods for subclass implementations                ║
    //  ║                                                                                ║
    //  ╚════════════════════════════════════════════════════════════════════════════════╝

    /**
     * Returns a XML stream reader over only a portion of the document, from given position inclusive
     * until the end of the given element exclusive. Nested elements of the same name, if any, will be
     * ignored.
     *
     * @param  tagName  name of the tag to close.
     * @return a reader over a portion of the stream.
     * @throws XMLStreamException if this XML reader has been closed.
     */
    protected final XMLStreamReader getSubReader(final QName tagName) throws XMLStreamException {
        return new StreamReaderDelegate(reader) {
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
     * Moves the cursor to the first start element and verifies that it is the expected element.
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
        if (!reader.isStartElement()) {
            do if (!reader.hasNext()) {
                throw new EOFException(endOfFile());
            } while (reader.next() != START_ELEMENT);
        }
        if (!isNamespace.test(reader.getNamespaceURI()) || !localName.equals(reader.getLocalName())) {
            throw new DataStoreContentException(errors().getString(
                    Errors.Keys.UnexpectedFileFormat_2, owner.getFormatName(), owner.getDisplayName()));
        }
    }

    /**
     * Skips all remaining elements until we reach the end of the given tag.
     * Nested tags of the same name, if any, are also skipped.
     *
     * <p>The current event when this method is invoked must be {@link #START_ELEMENT}.
     * After this method invocation, the current event will be {@link #END_ELEMENT}.</p>
     *
     * @param  tagName  name of the tag to close.
     * @throws EOFException if end tag could not be found.
     * @throws XMLStreamException if an error occurred while reading the XML stream.
     */
    protected final void skipUntilEnd(final QName tagName) throws EOFException, XMLStreamException {
        assert reader.getEventType() == START_ELEMENT;
        isNextDone = false;
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
     * Gets next parsing event. This method should be used instead of {@link XMLStreamReader#next()}
     * when the {@code while (next())} loop may contain call to the {@link #unmarshal(Class)} method.
     *
     * @return one of the {@link XMLStreamConstants}.
     * @throws XMLStreamException if an error occurred while fetching the next event.
     */
    protected final int next() throws XMLStreamException {
        if (!isNextDone) {
            return reader.next();       // This is the usual case.
        }
        isNextDone = false;
        return reader.getEventType();
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()},
     * or {@code null} if that value is null or empty.
     *
     * <p>The current event when this method is invoked must be {@link #START_ELEMENT}.
     * After this method invocation, the current event will be {@link #END_ELEMENT}.</p>
     *
     * @return the current text element, or {@code null} if empty.
     * @throws XMLStreamException if a text element cannot be returned.
     */
    protected final String getElementText() throws XMLStreamException {
        return Strings.trimOrNull(reader.getElementText());
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()} as a URI,
     * or {@code null} if that value is null or empty.
     *
     * @return the current text element as a URI, or {@code null} if empty.
     * @throws XMLStreamException if a text element cannot be returned.
     * @throws URISyntaxException if the text cannot be parsed as a URI.
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
     * @throws XMLStreamException if a text element cannot be returned.
     * @throws NumberFormatException if the text cannot be parsed as an integer.
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
     * @throws XMLStreamException if a text element cannot be returned.
     * @throws NumberFormatException if the text cannot be parsed as a floating point number.
     *
     * @see #parseDouble(String)
     */
    protected final Double getElementAsDouble() throws XMLStreamException {
        final String text = getElementText();
        return (text != null) ? parseDouble(text) : null;
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()} as a date,
     * or {@code null} if that value is null or empty.
     *
     * @return the current text element as a date, or {@code null} if empty.
     * @throws XMLStreamException if a text element cannot be returned.
     * @throws DateTimeParseException if the text cannot be parsed as a date.
     */
    protected final Date getElementAsDate() throws XMLStreamException {
        final String text = getElementText();
        return (text == null) ? null : Date.from(LenientDateFormat.parseInstantUTC(text));
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()} as a temporal object,
     * or {@code null} if that value is null or empty.
     *
     * @return the current text element as a temporal object, or {@code null} if empty.
     * @throws XMLStreamException if a text element cannot be returned.
     * @throws DateTimeParseException if the text cannot be parsed as a date.
     */
    protected final Temporal getElementAsTemporal() throws XMLStreamException {
        return LenientDateFormat.parseBest(getElementText());
    }

    /**
     * Returns the current value of {@link XMLStreamReader#getElementText()} as a list of strings,
     * or {@code null} if that value is null or empty.
     *
     * @return the current text element as a list.
     * @throws XMLStreamException if a text element cannot be returned.
     */
    protected final List<String> getElementAsList() throws XMLStreamException {
        final String text = getElementText();
        return (text != null) ? Arrays.asList(text.split(" ")) : null;
    }

    /**
     * Parses the given text as a XML floating point number. This method performs the same parsing as
     * {@link Double#valueOf(String)} with the addition of {@code INF} and {@code -INF} values.
     * The following summarizes the special values (note that parsing is case-sensitive):
     *
     * <ul>
     *   <li>{@code NaN}  — a XML value which is also understood natively by {@link Double#valueOf(String)}.</li>
     *   <li>{@code INF}  — a XML value which is processed by this method.</li>
     *   <li>{@code -INF} — a XML value which is processed by this method.</li>
     *   <li>{@code +INF} — illegal XML value, nevertheless processed by this method.</li>
     *   <li>{@code Infinity} — a {@link Double#valueOf(String)} specific value.</li>
     * </ul>
     *
     * <h4>Implementation note</h4>
     * This method duplicates {@link jakarta.xml.bind.DatatypeConverter#parseDouble(String)} work,
     * but avoid synchronization or volatile field cost of {@code DatatypeConverter}.
     *
     * @param  value  the text to parse.
     * @return the floating point value for the given text.
     * @throws NumberFormatException if parsing failed.
     *
     * @see #getElementAsDouble()
     * @see jakarta.xml.bind.DatatypeConverter#parseDouble(String)
     */
    @SuppressWarnings("fallthrough")
    protected static double parseDouble(final String value) throws NumberFormatException {
        if (!value.endsWith("INF")) {
            return Double.parseDouble(value);
        }
parse:  switch (value.length()) {
            case 4: switch (value.charAt(0)) {
                default:  break parse;
                case '-': return Double.NEGATIVE_INFINITY;
                case '+': // Fall through
            }
            case 3: return Double.POSITIVE_INFINITY;
        }
        throw new NumberFormatException(value);
    }

    /**
     * Parses the given string as a boolean value. This method performs the same parsing as
     * {@link Boolean#parseBoolean(String)} with one extension: the "0" value is considered
     * as {@code false} and the "1" value as {@code true}.
     *
     * <h4>Implementation note</h4>
     * This method duplicates {@link jakarta.xml.bind.DatatypeConverter#parseBoolean(String)} work
     * (except for its behavior in case of invalid value), but avoid synchronization or volatile
     * field cost of {@code DatatypeConverter}.
     *
     * @param value  the string value to parse as a boolean.
     * @return true if the boolean is equal to "true" or "1".
     *
     * @see jakarta.xml.bind.DatatypeConverter#parseBoolean(String)
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
     * This method assumes that the reader is on {@link #START_ELEMENT}. After this method invocation, the reader
     * will be on the event <strong>after</strong> {@link #END_ELEMENT}; this implies that the caller will need to
     * invoke {@link XMLStreamReader#getEventType()} instead of {@link XMLStreamReader#next()}.
     *
     * @param  <T>   compile-time value of the {@code type} argument.
     * @param  type  expected type of the object to unmarshal.
     * @return the unmarshalled object, or {@code null} if none.
     * @throws XMLStreamException if the XML stream is closed.
     * @throws JAXBException if an error occurred during unmarshalling.
     * @throws ClassCastException if the unmarshalling result is not of the expected type.
     *
     * @see jakarta.xml.bind.Unmarshaller#unmarshal(XMLStreamReader, Class)
     */
    protected final <T> T unmarshal(final Class<T> type) throws XMLStreamException, JAXBException {
        Unmarshaller m = unmarshaller;
        if (m == null) {
            m = getMarshallerPool().acquireUnmarshaller();
            for (final Map.Entry<String,?> entry : ((Map<String,?>) owner.configuration).entrySet()) {
                m.setProperty(entry.getKey(), entry.getValue());
            }
        }
        unmarshaller = null;
        final JAXBElement<T> element = m.unmarshal(reader, type);
        unmarshaller = m;                                           // Allow reuse or recycling only on success.
        isNextDone = true;
        return element.getValue();
    }

    /**
     * Closes the input stream and releases any resources used by this XML reader.
     * This reader cannot be used anymore after this method has been invoked.
     *
     * @throws JAXBException if an error occurred while releasing JAXB resources.
     * @throws XMLStreamException if an error occurred while releasing XML reader resources.
     * @throws IOException if an error occurred while closing the input stream.
     */
    @Override
    public void close() throws JAXBException, XMLStreamException, IOException {
        final Unmarshaller m = unmarshaller;
        if (m != null) {
            unmarshaller = null;
            getMarshallerPool().recycle(m);
        }
        reader.close();
        super.close();
    }

    /**
     * Invokes {@link #close()} and wraps checked exceptions in a {@link BackingStoreException}.
     * This method is defined for allowing this {@code StaxStreamReader} to be given to
     * {@link java.util.stream.Stream#onClose(Runnable)}.
     */
    @Override
    public final void run() throws BackingStoreException {
        try {
            close();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Returns an error message for {@link EOFException}.
     * This a convenience method for a frequently-used error.
     *
     * @return a localized error message for end of file error.
     */
    protected final String endOfFile() {
        return errors().getString(Errors.Keys.UnexpectedEndOfFile_1, owner.getDisplayName());
    }

    /**
     * Returns an error message for {@link BackingStoreException}.
     * This a convenience method for {@link #tryAdvance(Consumer)} implementations.
     * The error message will contain the current line and column number if available.
     *
     * @return a localized error message for a file that cannot be parsed.
     */
    protected final String canNotParseFile() {
        return IOUtilities.canNotReadFile(owner.getLocale(), owner.getFormatName(), owner.getDisplayName(), reader);
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
