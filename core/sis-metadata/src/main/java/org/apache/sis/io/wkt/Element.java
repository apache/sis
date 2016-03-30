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
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Locale;
import java.io.Serializable;
import java.text.ParsePosition;
import java.text.ParseException;
import org.opengis.referencing.cs.CoordinateSystem;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.util.LocalizedParseException;

import static org.apache.sis.util.CharSequences.skipLeadingWhitespaces;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;
import org.apache.sis.internal.jdk7.JDK7;


/**
 * An element in a <cite>Well Know Text</cite> (WKT). An {@code Element} is made of {@link String},
 * {@link Number} and other {@link Element}. For example:
 *
 * {@preformat text
 *     PrimeMeridian[“Greenwich”, 0.0, AngleUnit[“degree”, 0.017453292519943295]]]
 * }
 *
 * Each {@code Element} object can contain an arbitrary amount of other elements.
 * The result is a tree, which can be seen with {@link #toString()} for debugging purpose.
 * Elements can be pulled in a <cite>first in, first out</cite> order.
 *
 * @author  Rémi Ève (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
final class Element implements Serializable {
    /**
     * Indirectly for {@link WKTFormat} serialization compatibility.
     */
    private static final long serialVersionUID = 2366817541632131955L;

    /**
     * Kind of value expected in the element. Value 0 means "not yet determined".
     */
    private static final int NUMERIC = 1, TEMPORAL = 2;

    /**
     * Hard-coded list of elements in which to parse values as dates instead than numbers.
     * We may try to find a more generic approach in a future version.
     */
    private static final String[] TIME_KEYWORDS = {
        WKTKeywords.TimeOrigin,
        WKTKeywords.TimeExtent
    };

    /**
     * The position where this element starts in the string to be parsed.
     */
    final int offset;

    /**
     * Index of the keyword in the array given to the {@link #pullElement(String...)}
     * or {@link #pullOptionalElement(String...)} method.
     *
     * @see #getKeywordIndex()
     */
    private byte keywordIndex;

    /**
     * Keyword of this entity. For example: {@code "PrimeMeridian"}.
     */
    public final String keyword;

    /**
     * An ordered sequence of {@link String}s, {@link Number}s and other {@link Element}s.
     * May be {@code null} if the keyword was not followed by a pair of brackets (e.g. "north").
     *
     * <p>Access to this list should be done using the iterator, not by random access.</p>
     */
    private final List<Object> list;

    /**
     * The locale to be used for formatting an error message if the parsing fails, or {@code null} for
     * the system default. This is <strong>not</strong> the locale for parting number or date values.
     */
    private final Locale locale;

    /**
     * Constructs a root element.
     *
     * @param name An arbitrary name for the root element.
     * @param singleton The only children for this root.
     */
    Element(final String name, final Element singleton) {
        keyword = name;
        offset  = singleton.offset;
        locale  = singleton.locale;
        list    = new LinkedList<Object>();                     // Needs to be a modifiable list.
        list.add(singleton);
    }

    /**
     * Creates a modifiable copy of the given element.
     */
    Element(final Element toCopy) {
        keyword = toCopy.keyword;
        offset  = toCopy.offset;
        locale  = toCopy.locale;
        list    = new LinkedList<Object>(toCopy.list);          // Needs to be a modifiable list.
        final ListIterator<Object> it = list.listIterator();
        while (it.hasNext()) {
            final Object value = it.next();
            if (value instanceof Element) {
                final Element fragment = (Element) value;
                if (fragment.list != null) {
                    it.set(new Element(fragment));
                }
            }
        }
    }

    /**
     * Constructs a new {@code Element}.
     * The {@code sharedValues} argument have two meanings:
     *
     * <ul class="verbose">
     *   <li>If {@code null}, then the caller is parsing a WKT string. The {@code Element}
     *     must be mutable because its content will be emptied as the parsing progress.</li>
     *
     *   <li>If non-null, then the caller is storing a WKT fragment. We create the elements but the caller will
     *     not parse them immediately. The {@code Element} should be immutable because the fragment will potentially
     *     be reused many time. Since the fragment may be stored for a long time, the {@code sharedValues} map will
     *     be used for sharing unique instance of each value if possible.</li>
     * </ul>
     *
     * @param text         The text to parse.
     * @param position     On input, the position where to start parsing from.
     *                     On output, the first character after the separator.
     * @param sharedValues If parsing a fragment, a map with the values found in other elements. Otherwise {@code null}.
     */
    Element(final AbstractParser parser, final String text, final ParsePosition position,
            final Map<Object,Object> sharedValues) throws ParseException
    {
        /*
         * Find the first keyword in the specified string. If a keyword is found, then
         * the position is set to the index of the first character after the keyword.
         */
        locale = parser.errorLocale;
        offset = position.getIndex();
        final int length = text.length();
        int lower = skipLeadingWhitespaces(text, offset, length);
        { // This block is for keeping some variables local.
            int c = (lower < length) ? text.codePointAt(lower) : 0;
            if (!Character.isUnicodeIdentifierStart(c)) {
                keyword = text;
                position.setErrorIndex(lower);
                throw unparsableString(text, position);
            }
            int upper = lower;
            while ((upper += Character.charCount(c)) < length) {
                c = text.codePointAt(upper);
                if (!Character.isUnicodeIdentifierPart(c)) break;
            }
            keyword = text.substring(lower, upper);
            lower = skipLeadingWhitespaces(text, upper, length);
        }
        int valueType = 0;
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
         *   - If the first character is '$', insert the named WKT fragment in-place.
         *   - Otherwise, if the characters are "true" of "false" (ignoring case), then the value is taken as a boolean.
         *   - Otherwise, if the first character is a unicode identifier start, then the element is parsed as a chid Element.
         *   - Otherwise, if the first character is a quote, then the value is taken as a String.
         *   - Otherwise, the element is parsed as a number or as a date, depending of 'isTemporal' boolean value.
         */
        final List<Object> list = new LinkedList<Object>();
        final String separator = parser.symbols.trimmedSeparator();
        while (lower < length) {
            final int firstChar = text.codePointAt(lower);
            if (firstChar == Symbols.FRAGMENT_VALUE) {
                /*
                 * WKTFormat allows to substitute strings like "$FOO" by a WKT fragment. This is something similar
                 * to environment variables in Unix. If we find the "$" character, get the identifier behind "$"
                 * and insert the corresponding WKT fragment here.
                 */
                final int upper = AbstractParser.endOfFragmentName(text, ++lower);
                final String id = text.substring(lower, upper);
                Element fragment = parser.fragments.get(id);
                if (fragment == null) {
                    position.setIndex(offset);
                    position.setErrorIndex(lower);
                    throw new LocalizedParseException(locale, Errors.Keys.NoSuchValue_1, new Object[] {id}, lower);
                }
                if (fragment.list != null) {
                    fragment = new Element(fragment);
                }
                list.add(fragment);
                lower = upper;
            } else if (Character.isUnicodeIdentifierStart(firstChar)) {
                /*
                 * If the character is the beginning of a Unicode identifier, add as a child element
                 * except for the boolean "true" and "false" values which are handled in a special way.
                 */
                if (lower != (lower = regionMatches(text, lower, "true"))) {
                    list.add(Boolean.TRUE);
                } else if (lower != (lower = regionMatches(text, lower, "false"))) {
                    list.add(Boolean.FALSE);
                } else {
                    position.setIndex(lower);
                    list.add(new Element(parser, text, position, sharedValues));
                    lower = position.getIndex();
                }
            } else {
                Object value;
                final int closingQuote = parser.symbols.matchingQuote(firstChar);
                if (closingQuote >= 0) {
                    /*
                     * Try to parse the next element as a quoted string. We will take it as a string if the first non-blank
                     * character is a quote.  Note that a double quote means that the quote should be included as-is in the
                     * parsed text.
                     */
                    final int n = Character.charCount(closingQuote);
                    lower += Character.charCount(firstChar) - n;        // This will usually let 'lower' unchanged.
                    CharSequence content = null;
                    do {
                        final int upper = text.indexOf(closingQuote, lower += n);
                        if (upper < lower) {
                            throw missingCharacter(closingQuote, lower, position);
                        }
                        if (content == null) {
                            content = text.substring(lower, upper);     // First text fragment, and usually the only one.
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
                    /*
                     * Leading and trailing spaces should be ignored according ISO 19162 §B.4.
                     * Note that the specification suggests also to replace consecutive white
                     * spaces by a single space, but we don't do that yet.
                     */
                    value = CharSequences.trimWhitespaces(content).toString();
                } else {
                    /*
                     * Try to parse the next element as a date or a number. We attempt such parsing when
                     * the first non-blank character is not the beginning of an unicode identifier.
                     * Otherwise we assume that the next element is the keyword of a child 'Element'.
                     */
                    position.setIndex(lower);
                    if (valueType == 0) {
                        valueType = ArraysExt.containsIgnoreCase(TIME_KEYWORDS, keyword) ? TEMPORAL : NUMERIC;
                    }
                    switch (valueType) {
                        case TEMPORAL: value = parser.parseDate  (text, position); break;
                        case NUMERIC:  value = parser.parseNumber(text, position); break;
                        default: throw new AssertionError(valueType);                       // Should never happen.
                    }
                    if (value == null) {
                        // Do not update the error index; it is already updated by NumberFormat.
                        throw unparsableString(text, position);
                    }
                    lower = position.getIndex();
                }
                /*
                 * Store the value, using shared instances if this Element may be stored for a long time.
                 */
                if (sharedValues != null) {
                    final Object e = JDK8.putIfAbsent(sharedValues, value, value);
                    if (e != null) {
                        value = e;
                    }
                }
                list.add(value);
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
                    if (sharedValues != null) {
                        this.list = UnmodifiableArrayList.wrap(list.toArray());
                    } else {
                        this.list = list;
                    }
                    return;
                }
                position.setErrorIndex(lower);
                throw unparsableString(text, position);
            }
        }
        throw missingCharacter(closingBracket, lower, position);
    }

    /**
     * Increments the given {@code index} if and only if the word at that position is the given word,
     * ignoring case. Otherwise returns the index unchanged.
     */
    private static int regionMatches(final String text, final int index, final String word) {
        if (text.regionMatches(true, index, word, 0, word.length())) {
            final int end = index + word.length();
            if (end >= text.length() || !Character.isUnicodeIdentifierPart(text.codePointAt(end))) {
                return end;
            }
        }
        return index;
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
     * @param  cause The cause of the failure, or {@code null} if none.
     * @return The exception to be thrown.
     */
    final ParseException parseFailed(final Exception cause) {
        return (ParseException) new LocalizedParseException(locale, Errors.Keys.ErrorIn_2,
                new String[] {keyword, Exceptions.getLocalizedMessage(cause, locale)}, offset).initCause(cause);
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
    private ParseException unparsableString(final String text, final ParsePosition position) {
        final short errorKey;
        final CharSequence[] arguments;
        final int errorIndex = Math.max(offset, position.getErrorIndex());
        final int length = text.length();
        if (errorIndex == length) {
            errorKey  = Errors.Keys.UnexpectedEndOfString_1;
            arguments = new String[] {keyword};
        } else {
            errorKey  = Errors.Keys.UnparsableStringInElement_2;
            arguments = new CharSequence[] {keyword, CharSequences.token(text, errorIndex)};
        }
        position.setIndex(offset);
        return new LocalizedParseException(locale, errorKey, arguments, errorIndex);
    }

    /**
     * Returns an exception saying that a character is missing.
     *
     * @param c          The missing character.
     * @param errorIndex The error position.
     * @param position   The position to update with the error index.
     */
    private ParseException missingCharacter(final int c, final int errorIndex, final ParsePosition position) {
        position.setIndex(offset);
        position.setErrorIndex(errorIndex);
        final StringBuilder buffer = new StringBuilder(2).appendCodePoint(c);
        return new LocalizedParseException(locale, Errors.Keys.MissingCharacterInElement_2,
                new CharSequence[] {keyword, buffer}, errorIndex);
    }

    /**
     * Returns an exception saying that a sub-element is missing.
     *
     * @param key The name of the missing sub-element.
     */
    final ParseException missingComponent(final String key) {
        int error = offset;
        if (keyword != null) {
            error += keyword.length();
        }
        return new LocalizedParseException(locale, Errors.Keys.MissingComponentInElement_2,
                new String[] {keyword, key}, error);
    }

    /**
     * Returns a {@link ParseException} for a child keyword which is unknown.
     *
     * @param  expected Keyword of a typical element. Used only if this element contains no child element.
     * @return The exception to be thrown.
     */
    final ParseException missingOrUnknownComponent(final String expected) {
        String name = null;
        for (final Object child : list) {
            if (child instanceof Element) {
                name = ((Element) child).keyword;
                if (name != null) {
                    break;
                }
            }
        }
        final short res;
        final String[] args;
        if (name != null) {
            res  = Errors.Keys.UnknownKeyword_1;
            args = new String[] {name};
        } else {
            res  = Errors.Keys.MissingComponentInElement_2;
            args = new String[] {keyword, expected};
        }
        return new LocalizedParseException(locale, res, args, offset);
    }

    /**
     * Returns a {@link ParseException} for an illegal coordinate system.
     *
     * <p>The given {@code cs} argument should never be null with Apache SIS implementation of
     * {@link org.opengis.referencing.cs.CSFactory}, but could be null with user-supplied implementation.
     * But it would be a {@code CSFactory} contract violation, so the user would get a {@link NullPointerException}
     * later. For making easier to trace the cause, we throw here an exception with a similar error message.</p>
     *
     * @param  cs The illegal coordinate system.
     * @return The exception to be thrown.
     */
    final ParseException illegalCS(final CoordinateSystem cs) {
        final short key;
        final String value;
        if (cs == null) {
            key   = Errors.Keys.NullArgument_1;   // See javadoc.
            value = "coordinateSystem";
        } else {
            key   = Errors.Keys.IllegalCoordinateSystem_1;
            value = cs.getName().getCode();
        }
        return new LocalizedParseException(locale, key, new String[] {value}, offset);
    }




    //////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                      ////////
    ////////    Pull elements from the tree                                       ////////
    ////////                                                                      ////////
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the next value (not a child element) without removing it.
     *
     * @return The next value, or {@code null} if none.
     */
    public Object peekValue() {
        final Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            if (!(object instanceof Element)) {
                return object;
            }
        }
        return null;
    }

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
        throw missingComponent(key);
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
        throw missingComponent(key);
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
                    throw new LocalizedParseException(locale, Errors.Keys.UnparsableStringForClass_2,
                            new Object[] {Integer.class, number}, offset);
                }
                return number.intValue();
            }
        }
        throw missingComponent(key);
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
        throw missingComponent(key);
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
        throw missingComponent(key);
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
            if (object != null && !(object instanceof Element)) {
                iterator.remove();
                return object;
            }
        }
        throw missingComponent(key);
    }

    /**
     * Removes the next {@link Element} of the given name from the list and returns it.
     * If the element was mandatory but is missing, then the first entry in the given {@code keys}
     * array will be taken as the name of the missing element to report in the exception message.
     *
     * <p>The given {@code mode} argument can be one of the following constants:</p>
     * <ul>
     *   <li>{@link AbstractParser#MANDATORY} throw an exception if no matching element is found.</li>
     *   <li>{@link AbstractParser#OPTIONAL} return {@code null} if no matching element is found.</li>
     *   <li>{@link AbstractParser#FIRST} return {@code null} if the first element (ignoring all others)
     *       does not match.</li>
     * </ul>
     *
     * @param  mode {@link AbstractParser#FIRST}, {@link AbstractParser#OPTIONAL} or {@link AbstractParser#MANDATORY}.
     * @param  keys The element names (e.g. {@code "PrimeMeridian"}).
     * @return The next {@link Element} of the given names found on the list, or {@code null} if none.
     * @throws ParseException if {@code mode} is {@code MANDATORY} and no element of the given names was found.
     */
    public Element pullElement(final int mode, final String... keys) throws ParseException {
        final Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            if (object instanceof Element) {
                final Element element = (Element) object;
                if (element.list != null) {
                    for (int i=0; i<keys.length; i++) {
                        if (element.keyword.equalsIgnoreCase(keys[i])) {
                            element.keywordIndex = (byte) i;
                            iterator.remove();
                            return element;
                        }
                    }
                    if (mode == AbstractParser.FIRST) {
                        return null;
                    }
                }
            }
        }
        if (mode != AbstractParser.MANDATORY) {
            return null;
        }
        throw missingComponent(keys[0]);
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
        throw missingComponent(key);
    }

    /**
     * Removes the next object of the given type from the list and returns it, if presents.
     *
     * @param  type The object type.
     * @return The next object on the list, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    public <T> T pullOptional(final Class<T> type) {
        final Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            if (type.isInstance(object) && !(object instanceof Element)) {
                iterator.remove();
                return (T) object;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if this element does not contains any remaining child.
     *
     * @return {@code true} if there is no child remaining.
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * Returns the index of the keyword in the array given to the {@link #pullElement(int, String...)} method.
     */
    final int getKeywordIndex() {
        return keywordIndex;
    }

    /**
     * Closes this element. This method verifies that there is no unprocessed value (dates,
     * numbers, booleans or strings), but ignores inner elements as required by ISO 19162.
     *
     * This method add the keywords of ignored elements in the {@code ignoredElements} map as below:
     * <ul>
     *   <li><b>Keys</b>: keyword of ignored elements. Note that a key may be null.</li>
     *   <li><b>Values</b>: keywords of all elements containing an element identified by the above-cited key.
     *       This list is used for helping the users to locate the ignored elements.</li>
     * </ul>
     *
     * @param  ignoredElements The collection where to declare ignored elements.
     * @throws ParseException If the list still contains some unprocessed values.
     */
    final void close(final Map<String, List<String>> ignoredElements) throws ParseException {
        if (list != null) {
            for (final Object value : list) {
                if (value instanceof Element) {
                    CollectionsExt.addToMultiValuesMap(ignoredElements, ((Element) value).keyword, keyword);
                } else {
                    throw new LocalizedParseException(locale, Errors.Keys.UnexpectedValueInElement_2,
                            new Object[] {keyword, value}, offset + keyword.length());
                }
            }
        }
    }

    /**
     * Formats this {@code Element} as a tree.
     * This method is used for debugging purpose only.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        format(buffer, 0, JDK7.lineSeparator());
        return buffer.toString();
    }

    /**
     * Implementation of {@link #toString()} to be invoked recursively.
     *
     * @param buffer Where to format.
     * @param margin Number of space to put in the left margin.
     */
    @Debug
    private void format(final StringBuilder buffer, int margin, final String lineSeparator) {
        buffer.append(CharSequences.spaces(margin)).append(keyword);
        if (list != null) {
            buffer.append('[');
            margin += 4;
            boolean addSeparator = false;
            for (final Object value : list) {
                if (value instanceof Element) {
                    if (addSeparator) buffer.append(',');
                    buffer.append(lineSeparator);
                    ((Element) value).format(buffer, margin, lineSeparator);
                } else {
                    final boolean quote = (value instanceof CharSequence);
                    if (addSeparator) buffer.append(", ");
                    if (quote) buffer.append('“');
                    buffer.append(value);
                    if (quote) buffer.append('”');
                }
                addSeparator = true;
            }
            buffer.append(']');
        }
    }
}
