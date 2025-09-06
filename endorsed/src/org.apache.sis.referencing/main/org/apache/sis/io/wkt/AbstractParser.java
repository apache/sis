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

import java.net.URI;
import java.util.Map;
import java.util.Set;
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
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.UnitFormat;


/**
 * Parses <i>Well Known Text</i> (WKT). Parsers are the converse of {@link Formatter}.
 * Like the latter, a parser is constructed with a given set of {@linkplain Symbols symbols}.
 * Parsers also need a set of factories to be used for instantiating the parsed objects.
 *
 * <p>In current version, parsers are not intended to be subclassed outside this package.</p>
 *
 * <p>Parsers are not synchronized. It is recommended to create separate parser instances for each thread.
 * If many threads access the same parser instance concurrently, it must be synchronized externally.</p>
 *
 * @author  Rémi Eve (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
abstract class AbstractParser implements Parser {
    /**
     * The logger for Well Known Text operations.
     *
     * @see #log(LogRecord)
     */
    private static final Logger LOGGER = Logger.getLogger(Loggers.WKT);

    /**
     * A mode for the {@link Element#pullElement(int, String...)} method meaning that only the first element
     * should be checked. If the name of the first element does not match one of the specified names, then
     * {@code pullElement(…)} returns {@code null}.
     */
    static final int FIRST = 0;

    /**
     * A mode for the {@link Element#pullElement(int, String...)} method meaning that the requested element
     * is optional but not necessarily first. If no element has a name matching one of the requested names,
     * then {@code pullElement(…)} returns {@code null}.
     */
    static final int OPTIONAL = 1;

    /**
     * A mode for the {@link Element#pullElement(int, String...)} method meaning that an exception shall be
     * thrown if no element has a name matching one of the requested names.
     */
    static final int MANDATORY = 2;

    /**
     * The URI to declare as the source of the WKT definitions, or {@code null} if unknown.
     * This information is not used directly by the parser, but will be stored in parameter values
     * as a hint for resolving relative paths as absolute paths.
     */
    final URI sourceFile;

    /**
     * The locale for formatting error messages if parsing fails, or {@code null} for system default.
     * This is <strong>not</strong> the locale for parsing number or date values.
     * The locale for numbers and dates is contained in {@link #symbols}.
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
     * Shall be used in read-only mode; never write through this reference.
     *
     * @see WKTFormat#addFragment(String, StoredTree)
     */
    final Map<String,StoredTree> fragments;

    /**
     * Keyword of unknown elements. The ISO 19162 specification requires that we ignore unknown elements,
     * but we will nevertheless report them as {@linkplain #warnings}. The meaning of this map is:
     *
     * <ul>
     *   <li><b>Keys</b>: keyword of ignored elements. Note that a key may be null.</li>
     *   <li><b>Values</b>: keywords of all elements containing an element identified by the above-cited key.
     *       This collection is used for helping the users to locate the ignored elements.</li>
     * </ul>
     *
     * Content of this map is not discarded immediately {@linkplain #getAndClearWarnings(Object) after parsing}.
     * It is kept for some time because {@link Warnings} will copy its content only when first needed.
     *
     * @see #getAndClearWarnings(Object)
     */
    final Map<String, Set<String>> ignoredElements;

    /**
     * The warning (other than {@link #ignoredElements}) that occurred during the parsing.
     * Created when first needed and reset to {@code null} when a new parsing start.
     * Warnings are reported when {@link #getAndClearWarnings(Object)} is invoked.
     */
    private Warnings warnings;

    /**
     * Constructs a parser using the specified set of symbols.
     *
     * @param  sourceFile    URI to declare as the source of the WKT definitions, or {@code null} if unknown.
     * @param  fragments     reference to the {@link WKTFormat#fragments} map, or an empty map if none.
     * @param  symbols       the set of symbols to use. Cannot be null.
     * @param  numberFormat  the number format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param  dateFormat    the date format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param  unitFormat    the unit format provided by {@link WKTFormat}, or {@code null} for a default format.
     * @param  errorLocale   the locale for error messages (not for parsing), or {@code null} for the system default.
     */
    AbstractParser(final URI sourceFile, final Map<String,StoredTree> fragments, final Symbols symbols,
                   NumberFormat numberFormat, final DateFormat dateFormat, final UnitFormat unitFormat,
                   final Locale errorLocale)
    {
        if (numberFormat == null) {
            numberFormat = symbols.createNumberFormat();
        }
        this.sourceFile  = sourceFile;
        this.fragments   = fragments;
        this.symbols     = symbols;
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
        ignoredElements = new LinkedHashMap<>();
    }

    /**
     * Returns the name of the class providing the publicly-accessible {@code createFromWKT(String)} method.
     * This information is used for logging purposes only. Values can be:
     *
     * <ul>
     *   <li>{@code "org.apache.sis.io.wkt.WKTFormat"}</li>
     *   <li>{@code "org.apache.sis.referencing.factory.GeodeticObjectFactory"}</li>
     *   <li>{@code "org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory"}</li>
     * </ul>
     */
    abstract String getPublicFacade();

    /**
     * Returns the name of the method invoked from {@link #getPublicFacade()}.
     * This information is used for logging purposes only.
     * Another possible value is {@code "parse"}.
     */
    String getFacadeMethod() {
        return "createFromWKT";
    }

    /**
     * Logs the given record for a warning that occurred during parsing.
     * This is used when we cannot use the {@link #warning warning methods},
     * or when the information is not worth to report as a warning.
     */
    final void log(final LogRecord record) {
        record.setSourceClassName (getPublicFacade());
        record.setSourceMethodName(getFacadeMethod());
        record.setLoggerName(Loggers.WKT);
        LOGGER.log(record);
    }

    /**
     * Creates the object from a WKT string and logs the warnings if any.
     * This method is for implementation of {@code createFromWKT(String)} method in SIS factories only.
     * Callers should ensure that {@code wkt} is non-null and non-empty (this method does not verify).
     *
     * @param  wkt  object encoded in Well-Known Text format (version 1 or 2).
     * @return the result of parsing the given text.
     * @throws FactoryException if the object creation failed.
     *
     * @see org.apache.sis.referencing.factory.GeodeticObjectFactory#createFromWKT(String)
     * @see org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory#createFromWKT(String)
     */
    @Override
    public final Object createFromWKT(final String wkt) throws FactoryException {
        final ParsePosition position = new ParsePosition(0);
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Warnings warnings;
        Object result = null;
        try {
            result = createFromWKT(wkt, position);
        } catch (ParseException exception) {
            final Throwable cause = exception.getCause();
            if (cause instanceof FactoryException) {
                throw (FactoryException) cause;
            }
            throw new FactoryException(exception.getLocalizedMessage(), exception);
        } finally {
            warnings = getAndClearWarnings(result);
        }
        final CharSequence unparsed = CharSequences.token(wkt, position.getIndex());
        if (unparsed.length() != 0) {
            throw new FactoryException(Errors.forLocale(errorLocale).getString(
                        Errors.Keys.UnexpectedCharactersAfter_2,
                        CharSequences.token(wkt, 0) + "[…]", unparsed));
        }
        if (warnings != null) {
            log(new LogRecord(Level.WARNING, warnings.toString()));
        }
        return result;
    }

    /**
     * Parses a <i>Well-Know Text</i> from specified position as a geodetic object.
     * Caller should invoke {@link #getAndClearWarnings(Object)} in a {@code finally} block
     * after this method and should decide what to do with remaining character at the end of the string.
     *
     * <p>If this method is invoked from {@link WKTFormat}, then {@link WKTFormat#clear()}
     * should be invoked before this method for making sure that no {@link Warnings} instance
     * is referencing {@link #ignoredElements}.</p>
     *
     * @param  text       the Well-Known Text (WKT) to parse.
     * @param  position   index of the first character to parse (on input) or after last parsed character (on output).
     * @return the parsed object.
     * @throws ParseException if the string cannot be parsed.
     */
    Object createFromWKT(final String text, final ParsePosition position) throws ParseException {
        warnings = null;
        ignoredElements.clear();
        final Element root = new Element(textToTree(text, position));
        final Object result = buildFromTree(root);
        root.close(ignoredElements);
        return result;
    }

    /**
     * Returns the index after the end of the fragment name starting at the given index.
     * Current implementation assumes that the fragment name is a Unicode identifier,
     * except for the first character which is not required to be an identifier start.
     */
    static int endOfFragmentName(final String text, int position) {
        final int length = text.length();
        while (position < length) {
            final int c = text.codePointAt(position);
            if (!Character.isUnicodeIdentifierPart(c)) break;
            position += Character.charCount(c);
        }
        return position;
    }

    /**
     * Parses the <i>Well Know Text</i> from specified position as a tree of {@link Element}s.
     * This tree can be given to {@link #buildFromTree(Element)} for producing a geodetic object.
     *
     * @param  wkt       the Well-Known Text to be parsed.
     * @param  position  before parsing, provides index of the first character to parse in the {@code wkt} string.
     *                   After parsing completion, provides index after the last character parsed.
     * @return the parsed object as a tree of {@link Element}s.
     * @throws ParseException if the string cannot be parsed.
     *
     * @see WKTFormat#textToTree(String, ParsePosition, String)
     */
    final Element textToTree(final String wkt, final ParsePosition position) throws ParseException {
        int lower = CharSequences.skipLeadingWhitespaces(wkt, position.getIndex(), wkt.length());
        if (lower >= wkt.length() || wkt.charAt(lower) != Symbols.FRAGMENT_VALUE) {
            return new Element(this, wkt, position);    // This is the usual case.
        }
        /*
         * Aliases for fragments (e.g. "FOO" in ProjectedCRS["something", $FOO]) are expanded by `Element`
         * constructor invoked above, except if the alias appears at the begining of the WKT string.
         * In such case the alias is the whole text and is handled in a special way below.
         */
        final int upper = endOfFragmentName(wkt, ++lower);
        final String id = wkt.substring(lower, upper);
        StoredTree fragment = fragments.get(id);
        if (fragment == null) {
            position.setErrorIndex(--lower);
            throw new UnparsableObjectException(errorLocale, Errors.Keys.NoSuchValue_1, new Object[] {id}, lower);
        }
        position.setIndex(upper);
        final SingletonElement singleton = new SingletonElement();
        fragment.toElements(this, singleton, ~0);
        return singleton.value;
    }

    /**
     * Parses the next element in the specified <i>Well Know Text</i> (WKT) tree.
     * Subclasses will typically get the name of the first element and delegate to a specialized method
     * such as {@code parseAxis(…)}, {@code parseEllipsoid(…)}, {@code parseTimeDatum(…)}, <i>etc</i>.
     *
     * <p>Callers should clear {@link #ignoredElements} before to invoke this method.
     * Cleaning {@link #warnings} can be done for safety but should not be necessary.</p>
     *
     * @param  element  the element to be parsed.
     * @return the parsed object.
     * @throws ParseException if the element cannot be parsed.
     */
    abstract Object buildFromTree(Element element) throws ParseException;

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
     *
     * <h4>Limitations</h4>
     * The WKT 2 specification allows dates in the form {@code YYYY-DDD} where {@code DDD} is the ordinal day.
     * This is not supported in the current implementation. It can be distinguished from the {@code YYYY-MM}
     * case by the fact that the ordinal day must have 3 digits according the specification.
     *
     * @todo Need to replace {@code Date} by {@code java.time} with the precision used in the WKT string.
     * The WKT 2 specification allows any precision.
     */
    final Date parseDate(final String text, final ParsePosition position) {
        if (dateFormat == null) {
            dateFormat = new LenientDateFormat(symbols.getLocale());
        }
        return dateFormat.parse(text, position);
    }

    /**
     * Parses the given unit name or symbol. Contrarily to other {@code parseFoo()} methods,
     * this method has no {@link ParsePosition} and expects the given string to be the full unit symbol.
     */
    final Unit<?> parseUnit(final String text) throws MeasurementParseException {
        if (unitFormat == null) {
            final Locale locale = symbols.getLocale();
            if (locale == Locale.ROOT) {
                return Units.valueOf(text);             // Most common case.
            }
            unitFormat = new UnitFormat(locale);
            unitFormat.setStyle(UnitFormat.Style.NAME);
        }
        return unitFormat.parse(text);
    }

    /**
     * Pulls an optional element which contains only a floating-point value.
     *
     * @param  parent  the element from which to pull a child element.
     * @param  key     name of the element to pull.
     * @return the value, or {@code null} if none.
     * @throws ParseException if an element cannot be parsed.
     */
    final Double pullElementAsDouble(final Element parent, final String key) throws ParseException {
        Element element = parent.pullElement(OPTIONAL, key);
        if (element != null) {
            double value = element.pullDouble(key);
            element.close(ignoredElements);
            return value;
        }
        return null;
    }

    /**
     * Pulls an optional element which contains only an integer value.
     *
     * @param  parent  the element from which to pull a child element.
     * @param  key     name of the element to pull.
     * @return the value, or {@code null} if none.
     * @throws ParseException if an element cannot be parsed.
     */
    final Integer pullElementAsInteger(final Element parent, final String key) throws ParseException {
        Element element = parent.pullElement(OPTIONAL, key);
        if (element != null) {
            int value = element.pullInteger(key);
            element.close(ignoredElements);
            return value;
        }
        return null;
    }

    /**
     * Pulls an optional element which contains only a string value.
     *
     * @param  parent  the element from which to pull a child element.
     * @param  key     name of the element to pull.
     * @return the value, or {@code null} if none.
     * @throws ParseException if an element cannot be parsed.
     */
    final String pullElementAsString(final Element parent, final String key) throws ParseException {
        Element element = parent.pullElement(OPTIONAL, key);
        if (element != null) {
            String value = element.pullString(key);
            element.close(ignoredElements);
            return value;
        }
        return null;
    }

    /**
     * Pulls an optional element which contains only an enumeration value.
     *
     * @param  parent  the element from which to pull a child element.
     * @param  key     name of the element to pull.
     * @return the value, or {@code null} if none.
     * @throws ParseException if an element cannot be parsed.
     */
    final String pullElementAsEnum(final Element parent, final String key) throws ParseException {
        Element element = parent.pullElement(OPTIONAL, key);
        if (element != null) {
            Element value = element.pullVoidElement(key);
            value  .close(ignoredElements);
            element.close(ignoredElements);
            return value.keyword;
        }
        return null;
    }

    /**
     * Reports a non-fatal warning that occurred while parsing a WKT.
     *
     * @param  parent   the parent element, or {@code null} if unknown.
     * @param  element  the element that we cannot parse, or {@code null} if unknown.
     * @param  message  the message. Can be {@code null} only if {@code ex} is non-null.
     * @param  ex       the non-fatal exception that occurred while parsing the element, or {@code null}.
     */
    final void warning(final Element parent, final Element element, final InternationalString message, final Exception ex) {
        warning(parent, (element != null) ? element.keyword : null, message, ex);
    }

    /**
     * Reports a non-fatal warning that occurred while parsing a WKT.
     *
     * @param  parent   the parent element, or {@code null} if unknown.
     * @param  element  the name of the element that we cannot parse, or {@code null} if unknown.
     * @param  message  the message. Can be {@code null} only if {@code ex} is non-null.
     * @param  ex       the non-fatal exception that occurred while parsing the element, or {@code null}.
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
     * <p>The returned object is valid only until a new parsing starts. If a longer lifetime
     * is desired, then the caller <strong>must</strong> invokes {@link Warnings#publish()}.</p>
     *
     * @param  result  the object that resulted from the parsing operation, or {@code null}.
     * @return the warnings, or {@code null} if none.
     */
    final Warnings getAndClearWarnings(final Object result) {
        Warnings w = warnings;
        warnings = null;
        if (w == null) {
            if (ignoredElements.isEmpty()) {
                return null;
            }
            w = new Warnings(errorLocale, true, ignoredElements);
            // Do not clear `ignoredElements` now.
        }
        w.setRoot(result);
        return w;
    }
}
