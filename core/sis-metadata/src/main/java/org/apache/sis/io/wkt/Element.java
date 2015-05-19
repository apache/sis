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
package org.apache.sis.io.wkt;

import java.util.Date;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.io.PrintWriter;
import java.text.ParsePosition;
import java.text.ParseException;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.metadata.WKTKeywords;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;
import static org.apache.sis.util.CharSequences.skipLeadingWhitespaces;


/**
 * An element in a <cite>Well Know Text</cite> (WKT). An {@code Element} is made of {@link String},
 * {@link Number} and other {@link Element}. For example:
 *
 * {@preformat text
 *     PrimeMeridian[“Greenwich”, 0.0, AngleUnit[“degree”, 0.017453292519943295]]]
 * }
 *
 * Each {@code Element} object can contain an arbitrary amount of other elements.
 * The result is a tree, which can be printed with {@link #print(PrintWriter, int)} for debugging purpose.
 * Elements can be pulled in a <cite>first in, first out</cite> order.
 *
 * @author  Rémi Ève (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class Element {
    /**
     * Hard-coded list of elements in which to parse values as dates instead than numbers.
     * We may try to find a more generic approach in a future version.
     */
    private static final String[] TEMPORAL = {
        WKTKeywords.TimeOrigin,
        WKTKeywords.TimeExtent
    };

    /**
     * The position where this element starts in the string to be parsed.
     */
    private final int offset;

    /**
     * Keyword of this entity. For example: {@code "PrimeMeridian"}.
     */
    public final String keyword;

    /**
     * An ordered list of {@link String}s, {@link Number}s and other {@link Element}s.
     * May be {@code null} if the keyword was not followed by a pair of brackets (e.g. "north").
     */
    private final List<Object> list;

    /**
     * Constructs a root element.
     *
     * @param singleton The only children for this root.
     */
    Element(final Element singleton) {
        offset  = 0;
        keyword = null;
        list    = new LinkedList<>();
        list.add(singleton);
    }

    /**
     * Constructs a new {@code Element}.
     *
     * @param text     The text to parse.
     * @param position On input, the position where to start parsing from.
     *                 On output, the first character after the separator.
     */
    Element(final Parser parser, final String text, final ParsePosition position) throws ParseException {
        /*
         * Find the first keyword in the specified string. If a keyword is found, then
         * the position is set to the index of the first character after the keyword.
         */
        offset = position.getIndex();
        final int length = text.length();
        int lower = skipLeadingWhitespaces(text, offset, length);
        { // This block is for keeping some variables local.
            int c = text.codePointAt(lower);
            if (!Character.isUnicodeIdentifierStart(c)) {
                keyword = text;
                position.setErrorIndex(lower);
                throw unparsableString(parser, text, position);
            }
            int upper = lower;
            while ((upper += Character.charCount(c)) < length) {
                c = text.codePointAt(upper);
                if (!Character.isUnicodeIdentifierPart(c)) break;
            }
            keyword = text.substring(lower, upper);
            lower = skipLeadingWhitespaces(text, upper, length);
        }
        final boolean isTemporal = ArraysExt.containsIgnoreCase(TEMPORAL, keyword);
        /*
         * At this point we have extracted the keyword (e.g. "PrimeMeridian"). Now parse the opening bracket.
         * According WKT's specification, two characters are acceptable: '[' and '('. We accept both, but we
         * will require the matching closing bracket at the end of this method. For example if the opening
         * bracket was '[', then we will require that the closing bracket is ']' and not ')'.
         */
        final int openingBracket;
        final int closingBracket;
        if (lower >= length || (closingBracket = parser.symbols.matchingBracket(
                                openingBracket = text.codePointAt(lower))) < 0)
        {
            position.setIndex(lower);
            list = null;
            return;
        }
        lower = skipLeadingWhitespaces(text, lower + Character.charCount(openingBracket), length);
        /*
         * Parse all elements inside the bracket. Elements are parsed sequentially
         * and their type are selected according their first character:
         *
         *   - If the first character is a quote, then the value is returned as a String.
         *   - Otherwise, if the first character is a unicode identifier start, then the element is parsed as a chid Element.
         *   - Otherwise, if the characters are "true" of "false" (ignoring case), then the value is returned as a boolean.
         *   - Otherwise, the element is parsed as a number or as a date, depending of 'isTemporal' boolean value.
         */
        list = new LinkedList<>();
        final String separator = parser.symbols.trimmedSeparator();
        while (lower < length) {
            final int firstChar = text.codePointAt(lower);
            final int closingQuote = parser.symbols.matchingQuote(firstChar);
            if (closingQuote >= 0) {
                /*
                 * Try to parse the next element as a quoted string. We will take it as a string if the first non-blank
                 * character is a quote.  Note that a double quote means that the quote should be included as-is in the
                 * parsed text.
                 */
                final int n = Character.charCount(closingQuote);
                lower += Character.charCount(firstChar) - n;    // This will usually let 'lower' unchanged.
                CharSequence content = null;
                do {
                    final int upper = text.indexOf(closingQuote, lower += n);
                    if (upper < lower) {
                        position.setIndex(offset);
                        position.setErrorIndex(lower);
                        throw missingCharacter(parser, closingQuote, lower);
                    }
                    if (content == null) {
                        content = text.substring(lower, upper);   // First text fragment, and usually the only one.
                    } else {
                        /*
                         * We will enter in this block only if we found at least one double quote.
                         * Convert the first text fragment to a StringBuilder so we can concatenate
                         * the next text fragments with only one quote between them.
                         */
                        if (content instanceof String) {
                            content = new StringBuilder((String) content);
                        }
                        ((StringBuilder) content).appendCodePoint(closingQuote).append(text, lower, upper);
                    }
                    lower = upper + n;  // After the closing quote.
                } while (lower < text.length() && text.codePointAt(lower) == closingQuote);
                list.add(content.toString());
            } else if (!Character.isUnicodeIdentifierStart(firstChar)) {
                /*
                 * Try to parse the next element as a date or a number. We will attempt such parsing
                 * if the first non-blank character is not the beginning of an unicode identifier.
                 * Otherwise we will assume that the next element is the keyword of a child 'Element'.
                 */
                position.setIndex(lower);
                final Object value;
                if (isTemporal) {
                    value = parser.parseDate(text, position);
                } else {
                    value = parser.parseNumber(text, position);
                }
                if (value == null) {
                    position.setIndex(offset);
                    // Do not update the error index; it is already updated by NumberFormat.
                    throw unparsableString(parser, text, position);
                }
                list.add(value);
                lower = position.getIndex();
            } else if (text.regionMatches(true, lower, "true", 0, 4)) {
                list.add(Boolean.TRUE);
                lower += 4;
            } else if (text.regionMatches(true, lower, "false", 0, 5)) {
                list.add(Boolean.FALSE);
                lower += 5;
            } else {
                // Otherwise, add the element as a child element.
                position.setIndex(lower);
                list.add(new Element(parser, text, position));
                lower = position.getIndex();
            }
            /*
             * At this point we finished to parse the component. If we find a separator (usually a coma),
             * search for another element. Otherwise verify that the closing bracket is present.
             */
            lower = skipLeadingWhitespaces(text, lower, length);
            if (text.regionMatches(lower, separator, 0, separator.length())) {
                lower = skipLeadingWhitespaces(text, lower + separator.length(), length);
            } else {
                if (lower >= length) break;
                final int c = text.codePointAt(lower);
                if (c == closingBracket) {
                    position.setIndex(lower + Character.charCount(c));
                    return;
                }
                position.setIndex(offset);
                position.setErrorIndex(lower);
                throw unparsableString(parser, text, position);
            }
        }
        position.setIndex(offset);
        position.setErrorIndex(lower);
        throw missingCharacter(parser, closingBracket, lower);
    }




    ////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                        ////////
    ////////    Construction of a ParseException when a string can not be parsed    ////////
    ////////                                                                        ////////
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a {@link ParseException} with the specified cause. A localized string
     * <code>"Error in &lt;{@link #keyword}&gt;"</code> will be prepend to the message.
     * The error index will be the starting index of this {@code Element}.
     *
     * @param  cause   The cause of the failure, or {@code null} if none.
     * @param  message The message explaining the cause of the failure, or {@code null}
     *                 for reusing the same message than {@code cause}.
     * @return The exception to be thrown.
     */
    final ParseException parseFailed(final Parser parser, final Exception cause, String message) {
        if (message == null) {
            message = Exceptions.getLocalizedMessage(cause, parser.displayLocale);
        }
        return (ParseException) new ParseException(Errors.getResources(parser.displayLocale)
                .getString(Errors.Keys.ErrorIn_2, keyword, message), offset).initCause(cause);
    }

    /**
     * Returns a {@link ParseException} with a "Unparsable string" message.
     * The error message is built from the specified string starting at the specified position.
     * Properties {@link ParsePosition#getIndex()} and {@link ParsePosition#getErrorIndex()}
     * must be accurate before this method is invoked.
     *
     * @param  text The unparsable string.
     * @param  position The position in the string.
     * @return An exception with a formatted error message.
     */
    private ParseException unparsableString(final Parser parser, final String text, final ParsePosition position) {
        final String message;
        final Errors resources = Errors.getResources(parser.displayLocale);
        final int errorIndex = Math.max(position.getIndex(), position.getErrorIndex());
        final int length = text.length();
        if (errorIndex == length) {
            message = resources.getString(Errors.Keys.UnexpectedEndOfString_1, keyword);
        } else {
            message = resources.getString(Errors.Keys.UnparsableStringInElement_2, keyword,
                    CharSequences.token(text, errorIndex));
        }
        return new ParseException(message, errorIndex);
    }

    /**
     * Returns an exception saying that a character is missing.
     *
     * @param c The missing character.
     * @param position The error position.
     */
    private ParseException missingCharacter(final Parser parser, final int c, final int position) {
        final StringBuilder buffer = new StringBuilder(2).appendCodePoint(c);
        return new ParseException(Errors.getResources(parser.displayLocale)
                .getString(Errors.Keys.MissingCharacterInElement_2, keyword, buffer), position);
    }

    /**
     * Returns an exception saying that a component is missing.
     *
     * @param key The name of the missing component.
     */
    private ParseException missingParameter(final String key) {
        int error = offset;
        if (keyword != null) {
            error += keyword.length();
        }
        return new ParseException(Errors.format(Errors.Keys.MissingComponentInElement_2, keyword, key), error);
    }

    /**
     * Returns {@code true} if this element is the root element. For example in a WKT like
     * {@code "GeodeticCRS["name", Datum["name, ...]]"}, this is true for {@code "GeodeticCRS"}
     * and false for all other elements inside, like {@code "Datum"}.
     *
     * @return {@code true} if this element is the root element.
     */
    public boolean isRoot() {
        return this.offset == 0;
    }




    //////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                      ////////
    ////////    Pull elements from the tree                                       ////////
    ////////                                                                      ////////
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Removes the next {@link Date} from the list and returns it.
     *
     * @param  key The parameter name. Used for formatting an error message if no date is found.
     * @return The next {@link Date} on the list.
     * @throws ParseException if no more date is available.
     */
    public Date pullDate(final String key) throws ParseException {
        final Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            if (object instanceof Date) {
                iterator.remove();
                return (Date) object;
            }
        }
        throw missingParameter(key);
    }

    /**
     * Removes the next {@link Number} from the list and returns it.
     *
     * @param  key The parameter name. Used for formatting an error message if no number is found.
     * @return The next {@link Number} on the list as a {@code double}.
     * @throws ParseException if no more number is available.
     */
    public double pullDouble(final String key) throws ParseException {
        final Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            if (object instanceof Number) {
                iterator.remove();
                return ((Number) object).doubleValue();
            }
        }
        throw missingParameter(key);
    }

    /**
     * Removes the next {@link Number} from the list and returns it as an integer.
     *
     * @param  key The parameter name. Used for formatting an error message if no number is found.
     * @return The next {@link Number} on the list as an {@code int}.
     * @throws ParseException if no more number is available, or the number is not an integer.
     */
    public int pullInteger(final String key) throws ParseException {
        final Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            if (object instanceof Number) {
                iterator.remove();
                final Number number = (Number) object;
                if (number instanceof Float || number instanceof Double) {
                    throw new ParseException(Errors.format(
                            Errors.Keys.UnparsableStringForClass_2, Integer.class, number), offset);
                }
                return number.intValue();
            }
        }
        throw missingParameter(key);
    }

    /**
     * Removes the next {@link Boolean} from the list and returns it.
     *
     * @param  key The parameter name. Used for formatting an error message if no boolean is found.
     * @return The next {@link Boolean} on the list as a {@code boolean}.
     * @throws ParseException if no more boolean is available.
     */
    public boolean pullBoolean(final String key) throws ParseException {
        final Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            if (object instanceof Boolean) {
                iterator.remove();
                return (Boolean) object;
            }
        }
        throw missingParameter(key);
    }

    /**
     * Removes the next {@link String} from the list and returns it.
     *
     * @param  key The parameter name. Used for formatting an error message if no number is found.
     * @return The next {@link String} on the list.
     * @throws ParseException if no more string is available.
     */
    public String pullString(final String key) throws ParseException {
        final Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            if (object instanceof String) {
                iterator.remove();
                return (String) object;
            }
        }
        throw missingParameter(key);
    }

    /**
     * Removes the next {@link Object} from the list and returns it.
     *
     * @param  key The parameter name. Used for formatting an error message if no number is found.
     * @return The next {@link Object} on the list (never {@code null}).
     * @throws ParseException if no more object is available.
     */
    public Object pullObject(final String key) throws ParseException {
        final Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            if (object != null) {
                iterator.remove();
                return object;
            }
        }
        throw missingParameter(key);
    }

    /**
     * Removes the next {@link Element} from the list and returns it.
     *
     * @param  key The element name (e.g. {@code "PRIMEM"}).
     * @return The next {@link Element} on the list.
     * @throws ParseException if no more element is available.
     */
    public Element pullElement(final String key) throws ParseException {
        final Element element = pullOptionalElement(key);
        if (element != null) {
            return element;
        }
        throw missingParameter(key);
    }

    /**
     * Removes the next {@link Element} from the list and returns it.
     *
     * @param  key The element name (e.g. {@code "PRIMEM"}).
     * @return The next {@link Element} on the list, or {@code null} if no more element is available.
     */
    public Element pullOptionalElement(String key) {
        key = key.toUpperCase();
        final Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            if (object instanceof Element) {
                final Element element = (Element) object;
                if (element.list!=null && element.keyword.equals(key)) {
                    iterator.remove();
                    return element;
                }
            }
        }
        return null;
    }

    /**
     * Removes and returns the next {@link Element} with no bracket.
     * The key is used only for only for formatting an error message.
     *
     * @param  key The parameter name. Used only for formatting an error message.
     * @return The next {@link Element} in the list, with no bracket.
     * @throws ParseException if no more void element is available.
     */
    public Element pullVoidElement(final String key) throws ParseException {
        final Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            if (object instanceof Element) {
                final Element element = (Element) object;
                if (element.list == null) {
                    iterator.remove();
                    return element;
                }
            }
        }
        throw missingParameter(key);
    }

    /**
     * Returns the next element, or {@code null} if there is no more
     * element. The element is <strong>not</strong> removed from the list.
     *
     * @return The next element, or {@code null} if there is no more elements.
     */
    public Object peek() {
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Closes this element.
     *
     * @throws ParseException If the list still contains some unprocessed elements.
     */
    public void close() throws ParseException {
        if (!isNullOrEmpty(list)) {
            throw new ParseException(Errors.format(Errors.Keys.UnexpectedParameter_1, list.get(0)), offset + keyword.length());
        }
    }

    /**
     * Returns the keyword. This overriding is needed for correct
     * formatting of the error message in {@link #close}.
     */
    @Override
    public String toString() {
        return keyword;
    }

    /**
     * Prints this {@code Element} as a tree.
     * This method is used for debugging purpose only.
     *
     * @param out   The output stream.
     * @param level The indentation level (usually 0).
     */
    @Debug
    public void print(final PrintWriter out, final int level) {
        final int tabWidth = 4;
        out.print(CharSequences.spaces(tabWidth * level));
        out.println(keyword);
        if (list == null) {
            return;
        }
        final int size = list.size();
        for (int j=0; j<size; j++) {
            final Object object = list.get(j);
            if (object instanceof Element) {
                ((Element) object).print(out, level+1);
            } else {
                out.print(CharSequences.spaces(tabWidth * (level+1)));
                out.println(object);
            }
        }
    }
}
