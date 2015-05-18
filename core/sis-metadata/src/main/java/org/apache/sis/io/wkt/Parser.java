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
import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.apache.sis.util.Workaround;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Parses <cite>Well Known Text</cite> (WKT). Parsers are the converse of {@link Formatter}.
 * Like the later, a parser is constructed with a given set of {@linkplain Symbols symbols}.
 * Parsers also need a set of factories to be used for instantiating the parsed objects.
 *
 * <p>In current version, parsers are usually not intended to be subclassed outside this package.
 * A few exceptions exist, for example {@link ReferencingParser#alterProperties(Map)} is one of
 * the few hooks provided for overriding.</p>
 *
 * <p>Parsers are not synchronized. It is recommended to create separate parser instances for each thread.
 * If multiple threads access a parser concurrently, it must be synchronized externally.</p>
 *
 * @author  Rémi Eve (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
abstract class Parser {
    /**
     * Set to {@code true} if parsing of number in scientific notation is allowed.
     * The way to achieve that is currently a hack, because {@link NumberFormat}
     * has no API for managing that as of JDK 1.8.
     *
     * @todo See if a future version of JDK allows us to get ride of this ugly hack.
     */
    @Workaround(library = "JDK", version = "1.8")
    static final boolean SCIENTIFIC_NOTATION = true;

    /**
     * The locale for error messages (not for number parsing), or {@code null} for the system default.
     */
    final Locale displayLocale;

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
     * Constructs a parser using the specified set of symbols.
     *
     * @param symbols The set of symbols to use.
     * @param displayLocale The locale for error messages (not for number parsing),
     *        or {@code null} for the system default.
     */
    Parser(final Symbols symbols, final Locale displayLocale) {
        this.displayLocale = displayLocale;
        ensureNonNull("symbols", symbols);
        this.symbols = symbols;
        numberFormat = symbols.createNumberFormat();
        if (SCIENTIFIC_NOTATION && numberFormat instanceof DecimalFormat) {
            final DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
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
        } else {
            exponentSymbol = null;
        }
    }

    /**
     * Parses a <cite>Well Know Text</cite> (WKT).
     *
     * @param  text The text to be parsed.
     * @param  position The position to start parsing from.
     * @return The parsed object.
     * @throws ParseException if the string can not be parsed.
     */
    public final Object parseObject(final String text, final ParsePosition position) throws ParseException {
        final int origin = position.getIndex();
        try {
            final Element element = getTree(text, position);
            final Object object = parse(element);
            element.close();
            return object;
        } catch (ParseException exception) {
            position.setIndex(origin);
            if (position.getErrorIndex() < origin) {
                position.setErrorIndex(exception.getErrorOffset());
            }
            throw exception;
        }
    }

    /**
     * Parses the number at the given position.
     */
    final Number parseNumber(String text, final ParsePosition position) {
        /*
         * HACK: DecimalFormat.parse(…) does not understand lower case 'e' for scientific notation.
         *       It understands upper case 'E' only, so we need to perform the replacement here.
         */
        final int base = position.getIndex();
        Number number = numberFormat.parse(text, position);
        if (number != null && exponentSymbol != null) {
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
     */
    final Date parseDate(final String text, final ParsePosition position) {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat(WKTFormat.DATE_PATTERN, symbols.getLocale());
        }
        return dateFormat.parse(text, position);
    }

    /**
     * Parses the next element in the specified <cite>Well Know Text</cite> (WKT) tree.
     *
     * @param  element The element to be parsed.
     * @return The parsed object.
     * @throws ParseException if the element can not be parsed.
     */
    abstract Object parse(final Element element) throws ParseException;

    /**
     * Returns a tree of {@link Element} for the specified text.
     *
     * @param  text       The text to parse.
     * @param  position   In input, the position where to start parsing from.
     *                    In output, the first character after the separator.
     * @return The tree of elements to parse.
     * @throws ParseException If an parsing error occurred while creating the tree.
     */
    final Element getTree(final String text, final ParsePosition position) throws ParseException {
        return new Element(new Element(this, text, position));
    }
}
