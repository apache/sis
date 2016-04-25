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

import java.util.Map;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.LocalizedParseException;
import org.apache.sis.internal.util.StandardDateFormat;
import org.apache.sis.measure.Units;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Parses <cite>Well Known Text</cite> (WKT). Parsers are the converse of {@link Formatter}.
 * Like the later, a parser is constructed with a given set of {@linkplain Symbols symbols}.
 * Parsers also need a set of factories to be used for instantiating the parsed objects.
 *
 * <p>In current version, parsers are not intended to be subclassed outside this package.</p>
 *
 * <p>Parsers are not synchronized. It is recommended to create separate parser instances for each thread.
 * If multiple threads access a parser concurrently, it must be synchronized externally.</p>
 *
 * @author  Rémi Eve (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
abstract class AbstractParser implements Parser {
    /**
     * A mode for the {@link Element#pullElement(int, String...)} method meaning that only the first element
     * should be checked. If the name of the first element does not match one of the specified names, then
     * {@code pullElement(…)} returns {@code null}.
     */
    static final int FIRST = 0;

    /**
     * A mode for the {@link Element#pullElement(int, String...)} method meaning that the requested element
     * is optional but is not necessarily first. If no element have a name matching one of the requested names,
     * then {@code pullElement(…)} returns {@code null}.
     */
    static final int OPTIONAL = 1;

    /**
     * A mode for the {@link Element#pullElement(int, String...)} method meaning that an exception shall be
     * thrown if no element have a name matching one of the requested names.
     */
    static final int MANDATORY = 2;

    /**
     * The locale for error messages (not for number parsing), or {@code null} for the system default.
     */
    final Locale errorLocale;

    /**
     * The symbols to use for parsing WKT.
     */
    final Symbols symbols;

    /**
     * The symbol for scientific notation, or {@code null} if none.
     * This is usually {@code "E"} (note the upper case), but could also be something like {@code "×10^"}.
     */
    private final String exponentSymbol;

    /**
     * The object to use for parsing numbers.
     */
    private final NumberFormat numberFormat;

    /**
     * The object to use for parsing dates, created when first needed.
     */
    private DateFormat dateFormat;

    /**
     * The object to use for parsing unit symbols, created when first needed.
     */
    private UnitFormat unitFormat;

    /**
     * Reference to the {@link WKTFormat#fragments} map, or an empty map if none.
     * This parser will only read this map, never write to it.
     */
    final Map<String,Element> fragments;

    /**
     * Keyword of unknown elements. The ISO 19162 specification requires that we ignore unknown elements,
     * but we will nevertheless report them as warnings.
     * The meaning of this map is:
     * <ul>
     *   <li><b>Keys</b>: keyword of ignored elements. Note that a key may be null.</li>
     *   <li><b>Values</b>: keywords of all elements containing an element identified by the above-cited key.
     *       This list is used for helping the users to locate the ignored elements.</li>
     * </ul>
     *
     * @see #getAndClearWarnings(Object)
     */
    final Map<String, List<String>> ignoredElements;

    /**
     * The warning (other than {@link #ignoredElements}) that occurred during the parsing.
     * Created when first needed and reset to {@code null} when a new parsing start.
     */
    private Warnings warnings;

    /**
     * Constructs a parser using the specified set of symbols.
     *
     * @param symbols       The set of symbols to use.
     * @param fragments     Reference to the {@link WKTFormat#fragments} map, or an empty map if none.
     * @param numberFormat  The number format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param dateFormat    The date format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param unitFormat    The unit format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param errorLocale   The locale for error messages (not for parsing), or {@code null} for the system default.
     */
    AbstractParser(final Symbols symbols, final Map<String,Element> fragments, NumberFormat numberFormat,
            final DateFormat dateFormat, final UnitFormat unitFormat, final Locale errorLocale)
    {
        ensureNonNull("symbols", symbols);
        if (numberFormat == null) {
            numberFormat = symbols.createNumberFormat();
        }
        this.symbols     = symbols;
        this.fragments   = fragments;
        this.dateFormat  = dateFormat;
        this.unitFormat  = unitFormat;
        this.errorLocale = errorLocale;
        if (Symbols.SCIENTIFIC_NOTATION && numberFormat instanceof DecimalFormat) {
            final DecimalFormat decimalFormat = (DecimalFormat) numberFormat.clone();
            exponentSymbol = decimalFormat.getDecimalFormatSymbols().getExponentSeparator();
            String pattern = decimalFormat.toPattern();
            if (!pattern.contains("E0")) {
                final StringBuilder buffer = new StringBuilder(pattern);
                final int split = pattern.indexOf(';');
                if (split >= 0) {
                    buffer.insert(split, "E0");
                }
                buffer.append("E0");
                decimalFormat.applyPattern(buffer.toString());
            }
            this.numberFormat = decimalFormat;
        } else {
            this.numberFormat = numberFormat;
            exponentSymbol = null;
        }
        ignoredElements = new LinkedHashMap<String, List<String>>();
    }

    /**
     * Returns the name of the class providing the publicly-accessible {@code createFromWKT(String)} method.
     * This information is used for logging purpose only.
     */
    abstract String getPublicFacade();

    /**
     * Returns the name of the method invoked from {@link #getPublicFacade()}.
     * This information is used for logging purpose only.
     */
    String getFacadeMethod() {
        return "createFromWKT";
    }

    /**
     * Creates the object from a string. This method is for implementation of {@code createFromWKT(String)}
     * method is SIS factories only.
     *
     * @param  text Coordinate system encoded in Well-Known Text format (version 1 or 2).
     * @return The result of parsing the given text.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createFromWKT(String)
     * @see org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#createFromWKT(String)
     */
    @Override
    public final Object createFromWKT(final String text) throws FactoryException {
        final Object value;
        try {
            value = parseObject(text, new ParsePosition(0));
        } catch (ParseException exception) {
            final Throwable cause = exception.getCause();
            if (cause instanceof FactoryException) {
                throw (FactoryException) cause;
            }
            throw new FactoryException(exception.getMessage(), exception);
        }
        final Warnings warnings = getAndClearWarnings(value);
        if (warnings != null) {
            log(new LogRecord(Level.WARNING, warnings.toString()));
        }
        return value;
    }

    /**
     * Logs the given record. This is used only when we can not use the {@link #warning warning methods},
     * or when the information is not worth to report as a warning.
     */
    final void log(final LogRecord record) {
        Logger logger = Logging.getLogger(Loggers.WKT);
        record.setSourceClassName(getPublicFacade());
        record.setSourceMethodName(getFacadeMethod());
        record.setLoggerName(logger.getName());
        logger.log(record);
    }

    /**
     * Returns the index after the end of the fragment name starting at the given index.
     * Current implementation assumes that the fragment name is a Unicode identifier.
     */
    static int endOfFragmentName(final String text, int upper) {
        final int length = text.length();
        while (upper < length) {
            final int c = text.codePointAt(upper);
            if (!Character.isUnicodeIdentifierPart(c)) break;
            upper += Character.charCount(c);
        }
        return upper;
    }

    /**
     * Parses a <cite>Well Know Text</cite> (WKT).
     *
     * @param  text The text to be parsed.
     * @param  position The position to start parsing from.
     * @return The parsed object.
     * @throws ParseException if the string can not be parsed.
     */
    public Object parseObject(final String text, final ParsePosition position) throws ParseException {
        warnings = null;
        ignoredElements.clear();
        ArgumentChecks.ensureNonEmpty("text", text);
        Element fragment;
        int lower = CharSequences.skipLeadingWhitespaces(text, position.getIndex(), text.length());
        if (lower < text.length() && text.charAt(lower) == Symbols.FRAGMENT_VALUE) {
            final int upper = endOfFragmentName(text, ++lower);
            final String id = text.substring(lower, upper);
            fragment = fragments.get(id);
            if (fragment == null) {
                position.setErrorIndex(lower);
                throw new LocalizedParseException(errorLocale, Errors.Keys.NoSuchValue_1, new Object[] {id}, lower);
            }
            position.setIndex(upper);
            fragment = new Element(fragment);
        } else {
            fragment = new Element(this, text, position, null);
        }
        final Element element = new Element("<root>", fragment);
        final Object object = parseObject(element);
        element.close(ignoredElements);
        return object;
    }

    /**
     * Parses the next element in the specified <cite>Well Know Text</cite> (WKT) tree.
     *
     * @param  element The element to be parsed.
     * @return The parsed object.
     * @throws ParseException if the element can not be parsed.
     */
    abstract Object parseObject(final Element element) throws ParseException;

    /**
     * Parses the number at the given position.
     * This is a helper method for {@link Element} only.
     */
    final Number parseNumber(String text, final ParsePosition position) {
        final int base = position.getIndex();
        Number number = numberFormat.parse(text, position);
        if (number != null && exponentSymbol != null) {
            /*
             * HACK: DecimalFormat.parse(…) does not understand lower case 'e' for scientific notation.
             *       It understands upper case 'E' only, so we may need to perform a replacement here.
             */
            int i = position.getIndex();
            if (text.regionMatches(true, i, exponentSymbol, 0, exponentSymbol.length())) {
                text = new StringBuilder(text).replace(i, i + exponentSymbol.length(), exponentSymbol).toString();
                position.setIndex(base);
                number = numberFormat.parse(text, position);
            }
        }
        return number;
    }

    /**
     * Parses the date at the given position.
     * This is a helper method for {@link Element} only.
     *
     * <p>The WKT 2 format expects dates formatted according the ISO 9075-2 standard.</p>
     */
    final Date parseDate(final String text, final ParsePosition position) {
        if (dateFormat == null) {
            dateFormat = new StandardDateFormat(symbols.getLocale());
        }
        return dateFormat.parse(text, position);
    }

    /**
     * Parses the given unit symbol.
     */
    final Unit<?> parseUnit(final String text) throws ParseException, IllegalArgumentException {
        if (unitFormat == null) {
            if (symbols.getLocale() == Locale.ROOT) {
                return Units.valueOf(text);             // Most common case, avoid the convolved code below.
            }
            unitFormat = UnitFormat.getInstance(symbols.getLocale());
        }
        /*
         * This convolved code tries to workaround JSR-275 limitations.
         */
        try {
            return (Unit<?>) unitFormat.parseObject(text);
        } catch (ParseException e) {
            try {
                return Units.valueOf(text);
            } catch (IllegalArgumentException e2) {
                throw e;
            }
        }
    }

    /**
     * Reports a non-fatal warning that occurred while parsing a WKT.
     *
     * @param parent  The parent element, or {@code null} if unknown.
     * @param element The element that we can not parse, or {@code null} if unknown.
     * @param message The message. Can be {@code null} only if {@code ex} is non-null.
     * @param ex      The non-fatal exception that occurred while parsing the element, or {@code null}.
     */
    final void warning(final Element parent, final Element element, final InternationalString message, final Exception ex) {
        warning(parent, (element != null) ? element.keyword : null, message, ex);
    }

    /**
     * Reports a non-fatal warning that occurred while parsing a WKT.
     *
     * @param parent  The parent element, or {@code null} if unknown.
     * @param element The name of the element that we can not parse, or {@code null} if unknown.
     * @param message The message. Can be {@code null} only if {@code ex} is non-null.
     * @param ex      The non-fatal exception that occurred while parsing the element, or {@code null}.
     */
    final void warning(final Element parent, final String element, final InternationalString message, final Exception ex) {
        if (warnings == null) {
            warnings = new Warnings(errorLocale, true, ignoredElements);
        }
        warnings.add(message, ex, (parent != null && element != null)
                ? new String[] {parent.keyword, element} : null);
    }

    /**
     * Returns the warnings, or {@code null} if none.
     * This method clears the warnings after the call.
     *
     * <p>The returned object is valid only before a new parsing starts. If a longer lifetime is desired,
     * then the caller <strong>must</strong> invokes {@link Warnings#publish()}.</p>
     *
     * @param object The object that resulted from the parsing operation, or {@code null}.
     */
    final Warnings getAndClearWarnings(final Object object) {
        Warnings w = warnings;
        warnings = null;
        if (w == null) {
            if (ignoredElements.isEmpty()) {
                return null;
            }
            w = new Warnings(errorLocale, true, ignoredElements);
        }
        w.setRoot(object);
        return w;
    }
}
