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
package org.apache.sis.style.xml;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


/**
 * Base class of the writers of styles, symbology and filters, using the StAX cursor API.
 * This class provides helper methods for writing elements with the appropriate prefixes,
 * and collects the warnings.
 *
 * <h2>Aggregation</h2>
 * A document is written by a chain of writers for the same version: an {@link SLDWriter}
 * contains an {@link SEWriter}, which contains an {@link FilterWriter}.
 * All writers of a chain share the same StAX cursor and the same {@linkplain State state}.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class AbstractStaxWriter {
    /**
     * The state shared by all writers of the same document.
     */
    private static final class State {
        /**
         * The warnings emitted during the writing process.
         */
        final List<String> warnings = new ArrayList<>();

        /**
         * Whether the root element has been written. All namespaces are declared on the root element.
         */
        boolean rootWritten;
    }

    /**
     * The underlying StAX cursor, shared with the other writers of the chain.
     */
    protected final XMLStreamWriter writer;

    /**
     * The state shared with the other writers of the chain.
     *
     * @see #warning(String)
     * @see #warnings()
     */
    private final State state;

    /**
     * Creates the first writer of a chain, using the given StAX cursor.
     * The XML document header is written immediately by this constructor.
     *
     * @param  writer  the StAX cursor to write to.
     * @throws XMLStreamException if the XML document header cannot be written.
     */
    protected AbstractStaxWriter(final XMLStreamWriter writer) throws XMLStreamException {
        this.writer = writer;
        state = new State();
        writer.writeStartDocument("UTF-8", "1.0");
    }

    /**
     * Creates a writer sharing the cursor and the state of the given writer.
     * This is used for creating the writers aggregated by {@code parent}.
     *
     * @param  parent  the writer which contains the new writer.
     */
    protected AbstractStaxWriter(final AbstractStaxWriter parent) {
        writer = parent.writer;
        state  = parent.state;
    }

    /**
     * Returns the version of the standards written by this writer.
     * All writers of a chain shall return the same version.
     *
     * @return the version of the written documents.
     */
    abstract StyleVersion getVersion();

    /**
     * Returns whether this writer writes documents in version 1.1.
     * Many elements exist only in that version.
     */
    final boolean isVersion11() {
        return getVersion() == StyleVersion.V1_1_0;
    }

    /**
     * Returns the usual prefix for the given namespace.
     */
    private static String prefix(final String ns) {
        if (SLDTags.NAMESPACE.equals(ns))          return SLDTags.PREFIX;
        if (SETags.NAMESPACE.equals(ns))           return SETags.PREFIX;
        if (FilterTags.NAMESPACE.equals(ns))       return FilterTags.PREFIX;
        if (FilterTags.XLINK_NAMESPACE.equals(ns)) return FilterTags.XLINK_PREFIX;
        throw new IllegalArgumentException(ns);
    }

    /**
     * Declares the namespaces on the root element, if not already done.
     */
    private void declareNamespaces() throws XMLStreamException {
        if (!state.rootWritten) {
            state.rootWritten = true;
            writer.writeNamespace(SLDTags.PREFIX, SLDTags.NAMESPACE);
            if (isVersion11()) {
                writer.writeNamespace(SETags.PREFIX, SETags.NAMESPACE);
            }
            writer.writeNamespace(FilterTags.PREFIX, FilterTags.NAMESPACE);
            writer.writeNamespace(FilterTags.XLINK_PREFIX, FilterTags.XLINK_NAMESPACE);
        }
    }

    /**
     * Writes the start tag of an element. If this element is the root element,
     * then the namespaces are declared on that element.
     *
     * @param  ns     namespace of the element.
     * @param  local  local name of the element.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     */
    protected final void start(final String ns, final String local) throws XMLStreamException {
        writer.writeStartElement(prefix(ns), local, ns);
        declareNamespaces();
    }

    /**
     * Writes an empty element. Attributes can be added after this method call.
     *
     * @param  ns     namespace of the element.
     * @param  local  local name of the element.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     */
    protected final void empty(final String ns, final String local) throws XMLStreamException {
        writer.writeEmptyElement(prefix(ns), local, ns);
        declareNamespaces();
    }

    /**
     * Writes the end tag of the current element.
     *
     * @throws XMLStreamException if an error occurred while writing the XML data.
     */
    protected final void end() throws XMLStreamException {
        writer.writeEndElement();
    }

    /**
     * Writes an element containing only the given text. Does nothing if the text is null.
     *
     * @param  ns     namespace of the element.
     * @param  local  local name of the element.
     * @param  text   the text to write, or {@code null} if none.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     */
    protected final void text(final String ns, final String local, final Object text) throws XMLStreamException {
        if (text != null) {
            start(ns, local);
            writer.writeCharacters(text.toString());
            end();
        }
    }

    /**
     * Writes an attribute (without namespace) on the current element. Does nothing if the value is null.
     *
     * @param  name   local name of the attribute.
     * @param  value  value of the attribute, or {@code null} if none.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     */
    protected final void attribute(final String name, final Object value) throws XMLStreamException {
        if (value != null) {
            writer.writeAttribute(name, value.toString());
        }
    }

    /**
     * Writes an attribute in the XLink namespace on the current element.
     *
     * @param  name   local name of the attribute.
     * @param  value  value of the attribute.
     * @throws XMLStreamException if an error occurred while writing the XML data.
     */
    protected final void xlink(final String name, final String value) throws XMLStreamException {
        writer.writeAttribute(FilterTags.XLINK_PREFIX, FilterTags.XLINK_NAMESPACE, name, value);
    }

    /**
     * Formats the given value as text. Colors are formatted as {@code #RRGGBB}, or {@code #AARRGGBB} if not opaque.
     * Floating point numbers without fraction digits are formatted as integers.
     * Arrays of numbers are formatted as numbers separated by spaces.
     *
     * @param  value  the value to format, or {@code null}.
     * @return the formatted value, or {@code null} if the given value was null.
     */
    static String format(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Color c) {
            final int alpha = c.getAlpha();
            return (alpha == 0xFF) ? String.format("#%06X", c.getRGB() & 0xFFFFFF)
                                   : String.format("#%08X", c.getRGB());
        }
        if (value instanceof Double v) {
            return format(v.doubleValue());
        }
        if (value instanceof Float v) {
            return format(v.floatValue());
        }
        if (value instanceof float[] array) {
            final var buffer = new StringBuilder();
            for (int i=0; i<array.length; i++) {
                if (i != 0) buffer.append(' ');
                buffer.append(format(array[i]));
            }
            return buffer.toString();
        }
        if (value instanceof double[] array) {
            final var buffer = new StringBuilder();
            for (int i=0; i<array.length; i++) {
                if (i != 0) buffer.append(' ');
                buffer.append(format(array[i]));
            }
            return buffer.toString();
        }
        return value.toString();
    }

    /**
     * Formats the given floating point number, without fraction digits if the number is an integer.
     *
     * @param  value  the number to format.
     * @return the formatted number.
     */
    static String format(final double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1E15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    /**
     * Formats the given single-precision number, without fraction digits if the number is an integer.
     *
     * @param  value  the number to format.
     * @return the formatted number.
     */
    static String format(final float value) {
        if (value == Math.rint(value) && Math.abs(value) < 1E7f) {
            return Long.toString((long) value);
        }
        return Float.toString(value);
    }

    /**
     * Records a warning about the content that cannot be written.
     *
     * @param  message  the warning message.
     */
    protected final void warning(final String message) {
        state.warnings.add(message);
    }

    /**
     * Records a warning saying that the given element is not supported by the version of this writer.
     *
     * @param  element  local name of the element which cannot be written.
     */
    protected final void unsupported(final String element) {
        warning("The <" + element + "> element is not supported in version " + getVersion().code + ", ignored.");
    }

    /**
     * Returns the warnings emitted since this writer has been created.
     *
     * @return the warnings, as a live list.
     */
    final List<String> warnings() {
        return state.warnings;
    }

    /**
     * Writes the end of the XML document and closes the underlying StAX cursor.
     *
     * @throws XMLStreamException if an error occurred while writing or closing the cursor.
     */
    final void close() throws XMLStreamException {
        writer.writeEndDocument();
        writer.close();
    }
}
