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
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.sis.coverage.BandedCoverage;
import org.apache.sis.storage.DataStoreContentException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;


/**
 * Base class of the readers of styles, symbology and filters, using the StAX cursor API.
 * This class provides the methods for walking in the XML tree, the conversions of expressions
 * to the types expected by the style elements, and collects the warnings.
 *
 * <h2>Aggregation</h2>
 * A document is read by a chain of readers for the same version: an {@link SLDReader}
 * contains an {@link SEReader}, which contains an {@link FilterReader}.
 * All readers of a chain share the same StAX cursor, the same factories and the same list of warnings.
 *
 * <h2>Leniency</h2>
 * The readers dispatch on the local names of the elements, without checking whether the elements
 * are in the SLD or SE namespace. This allows the same code to read SLD 1.0 documents (where the
 * symbology is in the SLD namespace) and SLD 1.1 / SE 1.1 documents (where the symbology is in
 * the SE namespace). The differences between versions, such as {@code <sld:CssParameter>} versus
 * {@code <se:SvgParameter>}, are all accepted by the readers of all versions.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class AbstractStaxReader implements XMLStreamConstants {
    /**
     * The underlying StAX cursor, shared with the other readers of the chain and with the GML readers.
     */
    protected final XMLStreamReader reader;

    /**
     * The warnings emitted during the reading process, shared with the other readers of the chain.
     *
     * @see #warning(String)
     * @see #warnings()
     */
    private final List<String> warnings;

    /**
     * The factories to use for features.
     */
    final Factories<Feature> features;

    /**
     * The factories to use for coverages.
     */
    final Factories<BandedCoverage> coverages;

    /**
     * Creates the first reader of a chain, using the given StAX cursor.
     *
     * @param  reader         the StAX cursor to read from.
     * @param  filterFactory  the factory to use for creating filters and expressions.
     */
    protected AbstractStaxReader(final XMLStreamReader reader, final FilterFactory<Feature,Object,Object> filterFactory) {
        this.reader = reader;
        warnings  = new ArrayList<>();
        features  = Factories.forFeatures(filterFactory);
        coverages = Factories.forCoverages(filterFactory);
    }

    /**
     * Creates a reader sharing the cursor, the factories and the warnings of the given reader.
     * This is used for creating the readers aggregated by {@code parent}.
     *
     * @param  parent  the reader which contains the new reader.
     */
    protected AbstractStaxReader(final AbstractStaxReader parent) {
        reader    = parent.reader;
        warnings  = parent.warnings;
        features  = parent.features;
        coverages = parent.coverages;
    }

    /**
     * Moves the cursor to the first {@link #START_ELEMENT}, skipping the XML prologue
     * (declaration, comments, DTD, etc.). Does nothing if the cursor is already there.
     *
     * @param  reader  the StAX cursor to move.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreContentException if no root element was found.
     */
    static void moveToRoot(final XMLStreamReader reader) throws XMLStreamException, DataStoreContentException {
        int type = reader.getEventType();
        while (type != START_ELEMENT) {
            if (!reader.hasNext()) {
                throw new DataStoreContentException("No style element found.");
            }
            type = reader.next();
        }
    }

    /**
     * Detects the version of the document from its root element.
     * The cursor shall be on the {@link #START_ELEMENT} event of the root element.
     * The version is given by the {@code version} attribute if present. Otherwise, the version is inferred
     * from the namespaces: an element in the SE namespace, or an element declaring the SE namespace,
     * is assumed to be in version 1.1. Other elements in the SLD namespace are assumed to be in version 1.0.
     *
     * @param  reader  the StAX cursor positioned on the root element.
     * @return the version, or {@code null} if the root element is neither in the SLD nor in the SE namespace.
     */
    static StyleVersion detectVersion(final XMLStreamReader reader) {
        final String ns = reader.getNamespaceURI();
        final boolean isSE = SETags.NAMESPACE.equals(ns);
        if (!isSE && !SLDTags.NAMESPACE.equals(ns)) {
            return null;
        }
        final StyleVersion declared = StyleVersion.forCode(reader.getAttributeValue(null, SETags.VERSION));
        if (declared != null) {
            return declared;
        }
        if (isSE) {
            return StyleVersion.V1_1_0;
        }
        for (int i = reader.getNamespaceCount(); --i >= 0;) {
            if (SETags.NAMESPACE.equals(reader.getNamespaceURI(i))) {
                return StyleVersion.V1_1_0;
            }
        }
        return StyleVersion.V1_0_0;
    }

    /**
     * Moves the cursor to the next child element of the current element.
     * This method shall be invoked when the cursor is on the {@link #START_ELEMENT} event of the parent,
     * or on the {@link #END_ELEMENT} event of a previous child. Text and comments are skipped.
     *
     * <p>Typical usage:</p>
     * {@snippet lang="java" :
     *     while (nextChild()) {
     *         switch (reader.getLocalName()) {
     *             // Each case shall leave the cursor on the END_ELEMENT event of the child.
     *         }
     *     }
     *     // The cursor is now on the END_ELEMENT event of the parent.
     *     }
     *
     * @return {@code true} if the cursor is on the {@code START_ELEMENT} event of a child,
     *         or {@code false} if the cursor is on the {@code END_ELEMENT} event of the parent.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreContentException if the end of document has been reached.
     */
    protected final boolean nextChild() throws XMLStreamException, DataStoreContentException {
        while (true) {
            switch (reader.next()) {
                case START_ELEMENT: return true;
                case END_ELEMENT:   return false;
                case END_DOCUMENT:  throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Skips the element at the current cursor position, including all its children.
     * The cursor shall be on a {@link #START_ELEMENT} event. After this method returns,
     * the cursor is on the matching {@link #END_ELEMENT} event.
     *
     * @throws XMLStreamException if an error occurred while reading the XML data.
     */
    protected final void skipElement() throws XMLStreamException {
        int depth = 1;
        while (depth > 0 && reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT: depth++; break;
                case END_ELEMENT:   depth--; break;
                case END_DOCUMENT:  return;
            }
        }
    }

    /**
     * Skips the element at the current cursor position and emits a warning saying that it is not supported.
     *
     * @param  parent  local name of the parent element, for the warning message.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     */
    protected final void unexpected(final String parent) throws XMLStreamException {
        warning("Unexpected element <" + reader.getLocalName() + "> in <" + parent + ">, ignored.");
        skipElement();
    }

    /**
     * Reads the text content of the element at the current cursor position. The cursor shall be on a
     * {@link #START_ELEMENT} event. After this method returns, the cursor is on the matching {@link #END_ELEMENT}.
     * For leniency, the text of child elements (for example {@code <ogc:Literal>}) is included.
     *
     * @return the text content, trimmed. May be empty but never null.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreContentException if the end of document has been reached.
     */
    protected final String readText() throws XMLStreamException, DataStoreContentException {
        final var buffer = new StringBuilder();
        int depth = 0;
        while (true) {
            switch (reader.next()) {
                case CHARACTERS:
                case CDATA:
                case SPACE:         buffer.append(reader.getText()); break;
                case START_ELEMENT: depth++; break;
                case END_ELEMENT:   if (--depth < 0) return buffer.toString().trim(); break;
                case END_DOCUMENT:  throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Reads the text content of the element at the current cursor position, or returns {@code null} if empty.
     *
     * @return the text content, trimmed, or {@code null} if empty.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreContentException if the end of document has been reached.
     */
    protected final String readTextOrNull() throws XMLStreamException, DataStoreContentException {
        final String text = readText();
        return text.isEmpty() ? null : text;
    }

    /**
     * Reads the content of an element which is either a text, or the name of an empty child element.
     * The first form is used in SE 1.1 (e.g. {@code <OverlapBehavior>AVERAGE</OverlapBehavior>}) and
     * the second form in SLD 1.0 (e.g. {@code <OverlapBehavior><AVERAGE/></OverlapBehavior>}).
     *
     * @return the text or the name of the first child element, or {@code null} if none.
     * @throws XMLStreamException if an error occurred while reading the XML data.
     * @throws DataStoreContentException if the end of document has been reached.
     */
    protected final String readTextOrChildName() throws XMLStreamException, DataStoreContentException {
        final var buffer = new StringBuilder();
        String name = null;
        while (true) {
            switch (reader.next()) {
                case CHARACTERS:
                case CDATA:
                case SPACE: {
                    buffer.append(reader.getText());
                    break;
                }
                case START_ELEMENT: {
                    if (name == null) name = reader.getLocalName();
                    skipElement();
                    break;
                }
                case END_ELEMENT: {
                    final String text = buffer.toString().trim();
                    return text.isEmpty() ? name : text;
                }
                case END_DOCUMENT: throw new DataStoreContentException(endOfDocument());
            }
        }
    }

    /**
     * Returns the value of the given attribute (without namespace) of the current element.
     *
     * @param  name  local name of the attribute.
     * @return value of the attribute, or {@code null} if none.
     */
    protected final String attribute(final String name) {
        final String value = reader.getAttributeValue(null, name);
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    /**
     * Parses the given text as a floating point number.
     *
     * @param  text     the text to parse.
     * @param  element  name of the element or attribute, for the error message.
     * @return the parsed number.
     * @throws DataStoreContentException if the text cannot be parsed.
     */
    protected static double parseDouble(final String text, final String element) throws DataStoreContentException {
        try {
            return Double.parseDouble(text.trim());
        } catch (NullPointerException | NumberFormatException e) {
            throw new DataStoreContentException("Invalid number in <" + element + ">: \"" + text + "\".", e);
        }
    }

    /**
     * Parses the given text as a boolean value.
     *
     * @param  text     the text to parse.
     * @param  element  name of the element, for the error message.
     * @return the parsed boolean.
     * @throws DataStoreContentException if the text is not a boolean.
     */
    protected static boolean parseBoolean(final String text, final String element) throws DataStoreContentException {
        if (text != null) {
            switch (text.trim()) {
                case "true":  case "1": return true;
                case "false": case "0": return false;
            }
        }
        throw new DataStoreContentException("Invalid boolean in <" + element + ">: \"" + text + "\".");
    }

    /**
     * Returns the enumeration value matching the given keyword. The comparison ignores case and underscores,
     * so that the {@code "toUpper"} keyword of Symbology Encoding matches the {@code TO_UPPER} enumeration value.
     *
     * @param  <E>   type of enumeration.
     * @param  text  the keyword, or {@code null}.
     * @param  type  type of enumeration.
     * @return the enumeration value, or {@code null} if the keyword is null or unrecognized.
     */
    protected final <E extends Enum<E>> E keyword(final String text, final Class<E> type) {
        if (text == null) {
            return null;
        }
        final String key = text.trim().replace("_", "");
        for (final E value : type.getEnumConstants()) {
            if (value.name().replace("_", "").equalsIgnoreCase(key)) {
                return value;
            }
        }
        warning("Unknown " + type.getSimpleName() + " value: \"" + text + "\".");
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    //  Conversions of expressions to the types expected by the style elements
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the given expression as an expression producing values of the given type.
     * Literals are converted immediately. Property values are converted with {@link Expression#toValueType(Class)},
     * except for colors and arrays which are not supported by the Apache SIS converters. Other expressions,
     * such as functions, are returned unchanged because their values will be converted by the renderer.
     * This approach preserves the original functions, which is necessary for writing them back.
     *
     * @param  <R>   type of resources on which the expression applies.
     * @param  <V>   type of values.
     * @param  f     the factories to use.
     * @param  e     the expression to convert, or {@code null}.
     * @param  type  type of values.
     * @return the expression producing values of the given type, or {@code null} if the given expression was null.
     */
    @SuppressWarnings("unchecked")
    final <R,V> Expression<R,V> typed(final Factories<R> f, final Expression<R,?> e, final Class<V> type) {
        if (e == null) {
            return null;
        }
        if (e instanceof Literal<R,?> literal) {
            final Object value = literal.getValue();
            if (value == null || type.isInstance(value)) {
                return (Expression<R,V>) e;
            }
            if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
                final V converted = convert(value.toString(), type);
                if (converted != null) {
                    return f.filter().literal(converted);
                }
            }
            warning("Cannot convert \"" + value + "\" to " + type.getSimpleName() + '.');
        } else if (e instanceof ValueReference<R,?> ref && type != Color.class && !type.isArray()) {
            try {
                return ref.toValueType(type);
            } catch (RuntimeException ex) {
                // Fallback on the unchanged expression, to be converted by the renderer.
            }
        }
        return (Expression<R,V>) e;
    }

    /**
     * Converts the given text to a value of the given type.
     *
     * @return the converted value, or {@code null} if the conversion failed.
     */
    @SuppressWarnings("unchecked")
    private <V> V convert(String text, final Class<V> type) {
        text = text.trim();
        try {
            if (type == Double.class) {
                return (V) Double.valueOf(text);
            }
            if (type == Integer.class) {
                final double value = Double.parseDouble(text);
                final long rounded = Math.round(value);
                if (rounded != value) {
                    warning("The value " + text + " has been rounded to " + rounded + '.');
                }
                return (V) Integer.valueOf(Math.toIntExact(rounded));
            }
            if (type == Boolean.class) {
                switch (text) {
                    case "true":  case "1": return (V) Boolean.TRUE;
                    case "false": case "0": return (V) Boolean.FALSE;
                    default: return null;
                }
            }
            if (type == Color.class) {
                return (V) parseColor(text);
            }
            if (type == float[].class) {
                return (V) parseFloats(text);
            }
            if (type == String.class) {
                return (V) text;
            }
        } catch (ArithmeticException | NumberFormatException e) {
            // Conversion failed, return null.
        }
        return null;
    }

    /**
     * Parses a color encoded as {@code #RRGGBB} or {@code #AARRGGBB}.
     *
     * @param  text  the color to parse.
     * @return the color.
     * @throws NumberFormatException if the given text is not a color.
     */
    static Color parseColor(final String text) {
        if (text.startsWith("#")) {
            final String hex = text.substring(1);
            switch (hex.length()) {
                case 6: return new Color(Integer.parseInt(hex, 16));
                case 8: return new Color((int) Long.parseLong(hex, 16), true);
            }
        }
        return Color.decode(text);
    }

    /**
     * Parses an array of numbers separated by spaces or commas, as used by {@code stroke-dasharray}.
     *
     * @param  text  the numbers to parse.
     * @return the numbers.
     * @throws NumberFormatException if a number cannot be parsed.
     */
    static float[] parseFloats(final String text) {
        if (text.isBlank()) {
            return new float[0];
        }
        final String[] items = text.trim().split("[\\s,]+");
        final float[] values = new float[items.length];
        for (int i=0; i<items.length; i++) {
            values[i] = Float.parseFloat(items[i]);
        }
        return values;
    }

    /** Returns the given expression as an expression producing numbers. */
    final <R> Expression<R,Double> asNumber(Factories<R> f, Expression<R,?> e) {return typed(f, e, Double.class);}

    /** Returns the given expression as an expression producing integers. */
    final <R> Expression<R,Integer> asInteger(Factories<R> f, Expression<R,?> e) {return typed(f, e, Integer.class);}

    /** Returns the given expression as an expression producing booleans. */
    final <R> Expression<R,Boolean> asBoolean(Factories<R> f, Expression<R,?> e) {return typed(f, e, Boolean.class);}

    /** Returns the given expression as an expression producing strings. */
    final <R> Expression<R,String> asString(Factories<R> f, Expression<R,?> e) {return typed(f, e, String.class);}

    /** Returns the given expression as an expression producing colors. */
    final <R> Expression<R,Color> asColor(Factories<R> f, Expression<R,?> e) {return typed(f, e, Color.class);}

    /** Returns the given expression as an expression producing arrays of numbers. */
    final <R> Expression<R,float[]> asFloats(Factories<R> f, Expression<R,?> e) {return typed(f, e, float[].class);}

    // ---------------------------------------------------------------------------------------------
    //  Warnings
    // ---------------------------------------------------------------------------------------------

    /**
     * Records a warning about the content of the document.
     *
     * @param  message  the warning message.
     */
    protected final void warning(final String message) {
        warnings.add(message);
    }

    /**
     * Returns the warnings emitted since this reader has been created.
     *
     * @return the warnings, as a live list.
     */
    final List<String> warnings() {
        return warnings;
    }

    /**
     * Returns the error message for a truncated document.
     */
    protected static String endOfDocument() {
        return "Unexpected end of document while parsing a style.";
    }
}
