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
package org.apache.sis.measure;

import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Objects;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InaccessibleObjectException;
import javax.measure.Dimension;
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import org.apache.sis.system.Configuration;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Localized;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.DefinitionURI;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.math.Fraction;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.logging.Logging;


/**
 * Parses and formats units of measurement as SI symbols, URI in OGC namespace or other symbols.
 * This class combines in a single class the API from {@link java.text} and the API from {@link javax.measure.format}.
 * In addition to the symbols of the <cite>Système international</cite> (SI), this class is also capable to handle
 * some symbols found in <i>Well Known Text</i> (WKT) definitions or in XML files.
 *
 * <h2>Parsing authority codes</h2>
 * If a character sequence given to the {@link #parse(CharSequence)} method is of the form {@code "EPSG:####"},
 * {@code "urn:ogc:def:uom:EPSG::####"} or {@code "http://www.opengis.net/def/uom/EPSG/0/####"} (ignoring case
 * and whitespaces around path separators), then {@code "####"} is parsed as an integer and forwarded to the
 * {@link Units#valueOfEPSG(int)} method.
 *
 * <p>If a character sequence starts with {@code "http://www.opengis.net/def/uom/UCUM/0/####"} (ignoring case
 * and whitespaces around path separators), then {@code "####"} is parsed as a symbol as if the URL before it
 * was absent.</p>
 *
 * <h2>Note on netCDF unit symbols</h2>
 * In netCDF files, values of "unit" attribute are concatenations of an angular unit with an axis direction,
 * as in {@code "degrees_east"} or {@code "degrees_north"}. This class ignores those suffixes and unconditionally
 * returns {@link Units#DEGREE} for all axis directions.
 *
 * <h2>Multi-threading</h2>
 * {@code UnitFormat} is generally not thread-safe. If units need to be parsed or formatted in different threads,
 * each thread should have its own {@code UnitFormat} instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see Units#valueOf(String)
 *
 * @since 0.8
 */
public class UnitFormat extends Format implements javax.measure.format.UnitFormat, Localized {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3064428584419360693L;

    /**
     * The authorities to accept, or {@code null} for disabling authority parsing.
     * Codes may be URLs such as {@code "http://www.opengis.net/def/uom/UCUM/0/d"}
     * or URNs such as {@code "EPSG:9001"}.
     */
    @Configuration
    private static final String[] AUTHORITIES = {Constants.EPSG, Constants.UCUM};

    /**
     * The unit name for degrees (not necessarily angular), to be handled in a special way.
     * Must contain only ASCII lower case letters ([a … z]).
     */
    private static final String DEGREES = "degrees";

    /**
     * The unit name for dimensionless unit.
     */
    private static final String UNITY = "unity";

    /**
     * The default instance used by {@link Units#valueOf(String)} for parsing units of measurement.
     * While {@code UnitFormat} is generally not thread-safe, this particular instance is safe if
     * we never invoke any setter method and we do not format with {@link Style#NAME}.
     */
    static final UnitFormat INSTANCE = new UnitFormat();

    /**
     * The locale specified at construction time or modified by {@link #setLocale(Locale)}.
     *
     * @see #getLocale()
     */
    private Locale locale;

    /**
     * Whether this {@code UnitFormat} should format long names like "metre" or use unit symbols.
     *
     * @see #getStyle()
     */
    private Style style;

    /**
     * Identify whether unit formatting uses ASCII symbols, Unicode symbols or full localized names.
     * For example, the {@link Units#CUBIC_METRE} units can be formatted in the following ways:
     *
     * <ul>
     *   <li>As a symbol using Unicode characters: <b>m³</b></li>
     *   <li>As a symbol restricted to the ASCII characters set: <b>m3</b></li>
     *   <li>As a long name:<ul>
     *     <li>in English: <q>cubic metre</q></li>
     *     <li>in French: <q>mètre cube</q></li>
     *   </ul></li>
     * </ul>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 0.8
     * @since   0.8
     */
    public enum Style {
        /**
         * Format unit symbols using Unicode characters. Units formatted in this style use superscript digits
         * for exponents (as in “m³”), the dot operator (“⋅”) for multiplications, specialized characters when
         * they exist (e.g. U+212A “K” for Kelvin sign), <i>etc.</i>
         *
         * <p>This is the default style of {@link UnitFormat}.</p>
         *
         * @see Unit#getSymbol()
         */
        SYMBOL(AbstractUnit.MULTIPLY, AbstractUnit.DIVIDE),

        /**
         * Format unit symbols using a syntax close to the Unified Code for Units of Measure (UCUM) one.
         * The character set is restricted to ASCII. The multiplication operator is the period (“.”).
         *
         * <h4>Modification to UCUM syntax rules</h4>
         * UCUM does not allow floating point numbers in unit terms, so the use of period as an operator
         * should not be ambiguous. However, Apache SIS relaxes this restriction in order to support the
         * scale factors commonly found in angular units (e.g. π/180). The meaning of a period in a string
         * is resolved with two SIS-specific rules:
         *
         * <ul>
         *   <li>Unit symbols shall not begin or end with a decimal digit or a superscript.</li>
         *   <li>A period between two decimal digits is interpreted as a decimal separator.</li>
         * </ul>
         *
         * @see org.apache.sis.util.CharSequences#toASCII(CharSequence)
         */
        UCUM('.', '/') {
            /** Replace non-ASCII characters on a "best effort" basis. */
            @Override Appendable appendSymbol(final Appendable toAppendTo, final String value) throws IOException {
                if (value.startsWith("°")) {
                    final int length = value.length();
                    if (length == 2) {
                        switch (value.charAt(1)) {
                            case 'C': return toAppendTo.append("Cel");
                            case 'K': // U+212A (Kelvin symbol)
                            case 'K': return toAppendTo.append('K');
                        }
                    }
                    return toAppendTo.append("deg").append(value, 1, length);
                }
                final CharSequence cs = CharSequences.toASCII(value);
                final int length = cs.length();
                for (int i=0; i<length; i++) {
                    toAppendTo.append(Characters.toNormalScript(cs.charAt(i)));
                }
                return toAppendTo;
            }

            /** Formats the power for a unit symbol. */
            @Override void appendPower(final Appendable toAppendTo, final int power) throws IOException {
                toAppendTo.append(String.valueOf(power));
            }

            /** Actually illegal for UCUM, but at least ensure that it contains only ASCII characters. */
            @Override void appendPower(final Appendable toAppendTo, final Fraction power) throws IOException {
                toAppendTo.append(EXPONENT).append(OPEN).append(String.valueOf(power.numerator))
                           .append('/').append(String.valueOf(power.denominator)).append(CLOSE);
            }
        },

        /**
         * Format unit symbols as localized long names if known, or Unicode symbols otherwise.
         *
         * @see Unit#getName()
         */
        NAME(AbstractUnit.MULTIPLY, AbstractUnit.DIVIDE);

        /**
         * Other symbols not in the {@link Style} enumeration because common to all.
         */
        static final char EXPONENT_OR_MULTIPLY = '*', EXPONENT = '^', OPEN = '(', CLOSE = ')';

        /**
         * Symbols to use for unit multiplications or divisions.
         */
        final char multiply, divide;

        /**
         * Creates a new style using the given symbols.
         */
        private Style(final char multiply, final char divide) {
            this.multiply = multiply;
            this.divide   = divide;
        }

        /**
         * Appends a string that may contains Unicode characters. The enumeration is responsible
         * for converting the Unicode characters into ASCII ones if needed.
         */
        Appendable appendSymbol(final Appendable toAppendTo, final String value) throws IOException {
            return toAppendTo.append(value);
        }

        /**
         * Appends an integer power. The power may be added as an exponent if allowed by the format style.
         */
        void appendPower(final Appendable toAppendTo, final int power) throws IOException {
            if (power >= 0 && power <= 9) {
                toAppendTo.append(Characters.toSuperScript((char) (power + '0')));
            } else {
                toAppendTo.append(String.valueOf(power));
            }
        }

        /**
         * Appends a rational power.
         */
        void appendPower(final Appendable toAppendTo, final Fraction power) throws IOException {
            toAppendTo.append(EXPONENT);
            final String value = power.toString();
            if (value.length() == 1) {
                toAppendTo.append(value);
            } else {
                toAppendTo.append(OPEN).append(value).append(CLOSE);
            }
        }
    }

    /**
     * Symbols or names to use for formatting units in replacement to the default unit symbols or names.
     * The {@link Unit} instances are the ones specified by user in calls to {@link #label(Unit, String)}.
     *
     * @see #label(Unit, String)
     */
    @SuppressWarnings("serial")                         // Various serializable implementations.
    private final Map<Unit<?>,String> unitToLabel;

    /**
     * Units associated to a given label (in addition to the system-wide {@link UnitRegistry}).
     * This map is the converse of {@link #unitToLabel}. The {@link Unit} instances may differ from the ones
     * specified by user since {@link AbstractUnit#symbol} may have been set to the label specified by the user.
     * The labels may contain some characters normally not allowed in unit symbols, like white spaces.
     *
     * @see #label(Unit, String)
     */
    @SuppressWarnings("serial")                         // Various serializable implementations.
    private final Map<String,Unit<?>> labelToUnit;

    /**
     * The mapping from unit symbols to long localized names.
     * Those resources are locale-dependent and loaded when first needed.
     *
     * @see #symbolToName()
     */
    private transient volatile ResourceBundle symbolToName;

    /**
     * Mapping from long localized and unlocalized names to unit instances.
     * This map is used only for parsing and created when first needed.
     *
     * @see #fromName(String)
     */
    private transient volatile Map<String,Unit<?>> nameToUnit;

    /**
     * Cached values of {@link #nameToUnit}, for avoiding to load the same information many time and for saving memory
     * if the user create many {@code UnitFormat} instances. Note that we do not cache {@link #symbolToName} because
     * {@link ResourceBundle} already provides its own caching mechanism.
     *
     * @see #fromName(String)
     */
    private static final WeakValueHashMap<Locale, Map<String,Unit<?>>> SHARED = new WeakValueHashMap<>(Locale.class);

    /**
     * Creates the unique {@link #INSTANCE}.
     */
    private UnitFormat() {
        locale      = Locale.ROOT;
        style       = Style.SYMBOL;
        unitToLabel = Map.of();
        labelToUnit = Map.of();
    }

    /**
     * Creates a new format for the given locale.
     *
     * @param  locale  the locale to use for parsing and formatting units.
     */
    public UnitFormat(final Locale locale) {
        this.locale = Objects.requireNonNull(locale);
        style       = Style.SYMBOL;
        unitToLabel = new HashMap<>();
        labelToUnit = new HashMap<>();
    }

    /**
     * Returns the locale used by this {@code UnitFormat}.
     *
     * @return the locale of this {@code UnitFormat}.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale that this {@code UnitFormat} will use for long names.
     * For example, a call to <code>setLocale({@linkplain Locale#US})</code>
     * instructs this formatter to use the “meter” spelling instead of “metre”.
     *
     * @param  locale  the new locale for this {@code UnitFormat}.
     *
     * @see UnitServices#getUnitFormat(String)
     */
    public void setLocale(final Locale locale) {
        this.locale  = Objects.requireNonNull(locale);
        symbolToName = null;            // Force reloading for the new locale.
        nameToUnit   = null;
    }

    /**
     * Returns whether this {@code UnitFormat} depends on the {@link Locale} given at construction time
     * for performing its tasks. This method returns {@code true} if formatting long names (e.g. “metre”
     * or “meter”} and {@code false} if formatting only the unit symbol (e.g. “m”).
     *
     * @return {@code true} if formatting depends on the locale.
     */
    @Override
    public boolean isLocaleSensitive() {
        return style == Style.NAME;
    }

    /**
     * Returns whether unit formatting uses ASCII symbols, Unicode symbols or full localized names.
     *
     * @return the style of units formatted by this {@code UnitFormat} instance.
     */
    public Style getStyle() {
        return style;
    }

    /**
     * Sets whether unit formatting should use ASCII symbols, Unicode symbols or full localized names.
     *
     * @param  style  the desired style of units.
     */
    public void setStyle(final Style style) {
        this.style = Objects.requireNonNull(style);
    }

    /**
     * Attaches a label to the specified unit. A <i>label</i> can be a substitute to either the
     * {@linkplain AbstractUnit#getSymbol() unit symbol} or the {@link AbstractUnit#getName() unit name},
     * depending on the {@linkplain #getStyle() format style}.
     * If the specified label is already associated to another unit, then the previous association is discarded.
     *
     * <h4>Restriction on character set</h4>
     * Current implementation accepts only {@linkplain Character#isLetter(int) letters},
     * {@linkplain Characters#isSubScript(int) subscripts}, {@linkplain Character#isSpaceChar(int) spaces}
     * (including non-breaking spaces but not CR/LF characters),
     * the degree sign (°) and a few other characters like underscore.
     * The set of legal characters may be expanded in future Apache SIS versions,
     * but the following restrictions are likely to remain:
     *
     * <ul>
     *   <li>The following characters are reserved since they have special meaning in UCUM format, in URI
     *       or in Apache SIS parser: <blockquote>" # ( ) * + - . / : = ? [ ] { } ^ ⋅ ∕</blockquote></li>
     *   <li>The symbol cannot begin or end with digits, since such digits would be confused with unit power.</li>
     * </ul>
     *
     * @param  unit   the unit being labeled.
     * @param  label  the new label for the given unit.
     * @throws IllegalArgumentException if the given label is not a valid unit name.
     */
    @Override
    public void label(final Unit<?> unit, String label) {
        ArgumentChecks.ensureNonNull("unit", unit);
        ArgumentChecks.ensureNonEmpty("label", label = label.strip());
        for (int i=0; i < label.length();) {
            final int c = label.codePointAt(i);
            if (!AbstractUnit.isSymbolChar(c) && !Character.isSpaceChar(c)) {       // NOT Character.isWhitespace(int)
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "label", label));
            }
            i += Character.charCount(c);
        }
        Unit<?> labeledUnit = unit;
        if (labeledUnit instanceof ConventionalUnit<?>) {
            labeledUnit = ((ConventionalUnit<?>) labeledUnit).alternate(label);
        }
        final Unit<?> unitForOldLabel = labelToUnit.remove(unitToLabel.put(unit, label));
        final Unit<?> oldUnitForLabel = labelToUnit.put(label, labeledUnit);
        if (oldUnitForLabel != null && !oldUnitForLabel.equals(labeledUnit) && !label.equals(unitToLabel.remove(oldUnitForLabel))) {
            /*
             * Assuming there is no bug in our algorithm, this exception should never happen
             * unless this UnitFormat has been modified concurrently in another thread.
             */
            throw new CorruptedObjectException("unitToLabel");
        }
        if (unitForOldLabel != null && !unitForOldLabel.getSystemUnit().equals(unit.getSystemUnit())) {
            /*
             * Assuming there is no bug in our algorithm, this exception should never happen
             * unless this UnitFormat has been modified concurrently in another thread.
             * We compared system units because the units may not be strictly equal
             * as a result of the call to ConventionalUnit.alternate(label).
             */
            throw new CorruptedObjectException("labelToUnit");
        }
    }

    /**
     * Loads the {@code UnitNames} resource bundle for the given locale.
     */
    static ResourceBundle getBundle(final Locale locale) {
        return ResourceBundle.getBundle("org.apache.sis.measure.UnitNames", locale, UnitFormat.class.getClassLoader());
    }

    /**
     * Returns the mapping from unit symbols to long localized names.
     * This mapping is loaded when first needed and memorized as long as the locale does not change.
     */
    private ResourceBundle symbolToName() {
        ResourceBundle r = symbolToName;
        if (r == null) {
            symbolToName = r = getBundle(locale);
        }
        return r;
    }

    /**
     * Returns the unit instance for the given long (un)localized name.
     * This method is somewhat the converse of {@link #symbolToName()}, but recognizes also
     * international and American spelling of unit names in addition of localized names.
     * The intent is to recognize "meter" as well as "metre".
     *
     * <p>While we said that {@code UnitFormat} is not thread safe, we make an exception for this method
     * for allowing the singleton {@link #INSTANCE} to parse symbols in a multi-threads environment.</p>
     *
     * @param  uom  the unit symbol, without leading or trailing spaces.
     * @return the unit for the given name, or {@code null} if unknown.
     */
    private Unit<?> fromName(String uom) {
        /*
         * Before to search in resource bundles, check for degrees units. The "deg" unit can be both angular
         * and Celsius degrees. We try to resolve this ambiguity by looking for the "C" suffix. We perform a
         * special case for the degrees units because SI symbols are case-sentive and unit names in resource
         * bundles are case-insensitive, but the "deg" case is a mix of both.
         */
        final int length = uom.length();
        for (int i=0; ; i++) {
            if (i != DEGREES.length()) {
                if (i != length && (uom.charAt(i) | ('a' - 'A')) == DEGREES.charAt(i)) {
                    continue;                           // Loop as long as the characters are the same, ignoring case.
                }
                if (i != 3 && i != 6) {
                    break;                              // Exit if not "deg" (3) or "degree" (6 characters).
                }
            }
            if (length == i) {
                return Units.DEGREE;                    // Exactly "deg", "degree" or "degrees" (ignoring case).
            }
            final int c = uom.codePointAt(i);
            if (c == '_' || Character.isSpaceChar(c)) {
                i += Character.charCount(c);            // Ignore space in "degree C", "deg C", "deg K", etc.
            }
            if (length - i == 1) {
                switch (uom.charAt(i)) {
                    case 'K':                           // Unicode U+212A
                    case 'K': return Units.KELVIN;      // "degK" (ignoring case except for 'K')
                    case 'C': return Units.CELSIUS;
                    case 'N':                           // degree_N, degrees_N, degreeN, degreesN.
                    case 'E': return Units.DEGREE;      // degree_E, degrees_E, degreeE, degreesE.
                }
            }
            break;
        }
        /*
         * At this point, we determined that the given unit symbol is not degrees (of angle or of temperature).
         * Remaining code is generic to all other kinds of units: a check in a HashMap loaded when first needed.
         */
        Map<String,Unit<?>> map = nameToUnit;
        if (map == null) {
            map = SHARED.get(locale);
            if (map == null) {
                map = new HashMap<>(128);
                copy(locale, symbolToName(), map);
                if (!locale.equals(Locale.US))   copy(Locale.US,   getBundle(Locale.US),   map);
                if (!locale.equals(Locale.ROOT)) copy(Locale.ROOT, getBundle(Locale.ROOT), map);
                /*
                 * The UnitAliases file contains names that are not unit symbols and are not included in the UnitNames
                 * property files neither. It contains longer names sometimes used (for example "decimal degree" instead
                 * of "degree"), some plural forms (for example "feet" instead of "foot") and a few common misspellings
                 * (for exemple "Celcius" instead of "Celsius").
                 */
                final ResourceBundle r = ResourceBundle.getBundle("org.apache.sis.measure.UnitAliases", locale, UnitFormat.class.getClassLoader());
                for (final String name : r.keySet()) {
                    map.put(name.intern(), Units.get(r.getString(name)));
                }
                map = Collections.unmodifiableMap(map);
                /*
                 * Cache the map so we can share it with other UnitFormat instances.
                 * Sharing is safe if the map is unmodifiable.
                 */
                synchronized (SHARED) {
                    for (final Map<String,Unit<?>> existing : SHARED.values()) {
                        if (map.equals(existing)) {
                            map = existing;
                            break;
                        }
                    }
                    SHARED.put(locale, map);
                }
            }
            nameToUnit = map;
        }
        /*
         * The `nameToUnit` map contains plural forms (declared in "UnitAliases.properties" file),
         * but we make a special case for common units such as "degrees", "radians", "seconds",
         * "metres" and "meters" because they are repeated in units such as "kilometers".
         */
        uom = uom.replace('_', ' ').toLowerCase(locale);
        uom = removePlural(CharSequences.toASCII(uom).toString());
        /*
         * Returns the unit with application of the power if it is part of the name.
         * For example, this method interprets "meter2" as "meter" raised to power 2.
         */
        Unit<?> unit = map.get(uom);
appPow: if (unit == null) {
            int s = uom.length();
            if (--s > 0 && isDigit(uom.charAt(s))) {
                do if (--s < 0) break appPow;
                while (isDigit(uom.charAt(s)));
                if (uom.charAt(s) == '-') {
                    if (--s < 0) break appPow;
                }
                unit = map.get(uom.substring(0, ++s));
                if (unit != null) {
                    unit = unit.pow(Integer.parseInt(uom.substring(s)));
                }
            }
        }
        return unit;
    }

    /**
     * Copies all entries from the given "symbols to names" mapping to the given "names to units" mapping.
     * During this copy, keys are converted from symbols to names and values are converted from symbols to
     * {@code Unit} instances. We use {@code Unit} values instead of their symbols because all {@code Unit}
     * instances are created at {@link Units} class initialization anyway (so we do not create new instance
     * here), and it avoids to retain references to the {@link String} instances loaded by the resource bundle.
     */
    private static void copy(final Locale locale, final ResourceBundle symbolToName, final Map<String,Unit<?>> nameToUnit) {
        for (final String symbol : symbolToName.keySet()) {
            String name = CharSequences.toASCII(symbolToName.getString(symbol).toLowerCase(locale)).toString().intern();
            nameToUnit.put(removePlural(name), Units.get(symbol));
        }
    }

    /**
     * Returns the given string with common units such as "degrees", "radians", "seconds", "metres" and "meters"
     * replaced by their singular forms. All these units may have a prefix, for example as in "kilometres".
     * The result may not be grammatically correct English, but those strings will not be visible to users.
     * This is similar to making a string in lower cases before comparison in order to be case-insensitive.
     */
    private static String removePlural(String uom) {
        uom = uom.replace (DEGREES,  "degree");
        uom = uom.replace("radians", "radian");
        uom = uom.replace("seconds", "second");
        uom = uom.replace("meters",  "meter");
        uom = uom.replace("metres",  "metre");
        return uom;
    }

    /**
     * Formats the specified unit.
     * This method performs the first of the following actions that can be done.
     *
     * <ol>
     *   <li>If a {@linkplain #label(Unit, String) label has been specified} for the given unit,
     *       then that label is appended unconditionally.</li>
     *   <li>Otherwise if the formatting style is {@link Style#NAME} and the {@link Unit#getName()} method
     *       returns a non-null value, then that value is appended. {@code Unit} instances implemented by
     *       Apache SIS are handled in a special way for localizing the name according the
     *       {@linkplain #setLocale(Locale) locale specified to this format}.</li>
     *   <li>Otherwise if the {@link Unit#getSymbol()} method returns a non-null value,
     *       then that value is appended.</li>
     *   <li>Otherwise a default symbol is created from the entries returned by {@link Unit#getBaseUnits()}.</li>
     * </ol>
     *
     * @param  unit        the unit to format.
     * @param  toAppendTo  where to format the unit.
     * @return the given {@code toAppendTo} argument, for method calls chaining.
     * @throws IOException if an error occurred while writing to the destination.
     */
    @Override
    public Appendable format(final Unit<?> unit, final Appendable toAppendTo) throws IOException {
        ArgumentChecks.ensureNonNull("unit", unit);
        ArgumentChecks.ensureNonNull("toAppendTo", toAppendTo);
        /*
         * Choice 1: label specified by a call to label(Unit, String).
         */
        {
            final String label = unitToLabel.get(unit);
            if (label != null) {
                return toAppendTo.append(label);
            }
        }
        /*
         * Choice 2: value specified by Unit.getName(). We skip this check if the given Unit is an instance
         * implemented by Apache SIS because  AbstractUnit.getName()  delegates to the same resource bundle
         * than the one used by this block. We are better to use the resource bundle of the UnitFormat both
         * for performance reasons and because the locale may not be the same.
         */
        if (style == Style.NAME) {
            if (!(unit instanceof AbstractUnit)) {
                final String label = unit.getName();
                if (label != null) {
                    return toAppendTo.append(label);
                }
            } else {
                String label = unit.getSymbol();
                if (label != null) {
                    if (label.isEmpty()) {
                        label = UNITY;
                    }
                    // Following is not thread-safe, but it is okay since we do not use INSTANCE for unit names.
                    final ResourceBundle names = symbolToName();
                    try {
                        label = names.getString(label);
                    } catch (MissingResourceException e) {
                        Logging.ignorableException(AbstractUnit.LOGGER, UnitFormat.class, "format", e);
                        // Name not found; use the symbol as a fallback.
                    }
                    return toAppendTo.append(label);
                }
            }
        }
        /*
         * Choice 3: if the unit has a specific symbol, appends that symbol.
         * Apache SIS implementation uses Unicode characters in the symbol, which are not valid for UCUM.
         * But Styme.UCUM.appendSymbol(…) performs required replacements.
         */
        {
            final String symbol = unit.getSymbol();
            if (symbol != null) {
                return style.appendSymbol(toAppendTo, symbol);
            }
        }
        /*
         * Choice 4: if all the above failed, fallback on a symbol created from the base units and their power.
         * Note that this may produce more verbose symbols than needed because derived units like Volt or Watt
         * are decomposed into their base SI units. The scale factor will be inserted before the unit components,
         * e.g. "30⋅m∕s". Note that a scale factor relative to system unit may not be what we want if the unit
         * contains "kg", since it block us from using SI prefixes. But in many cases (not all), a symbol will
         * have been created by SystemUnit.transform(…), in which case "Choice 3" above would have been executed.
         */
        final Unit<?> unscaled = unit.getSystemUnit();
        @SuppressWarnings("unchecked")          // Both `unit` and `unscaled` are `Unit<Q>`.
        final double scale = AbstractConverter.scale(unit.getConverterTo((Unit) unscaled));
        if (Double.isNaN(scale)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NonRatioUnit_1,
                    "?⋅" + Style.OPEN + unscaled + Style.CLOSE));
        }
        /*
         * In addition of the scale, we will need to know:
         *
         *   - The components (for example "m" and "s" in "m∕s").
         *   - Whether we have at least one component on the left side of "∕" operation.
         *     Used for determining if we should prepend "1" before the "∕" symbol.
         *   - If there is exactly one component on the left side of "∕" and that component
         *     is prefixable, the power raising that component. Used for choosing a prefix.
         */
        int prefixPower = 0;
        boolean hasNumerator = false;
        final Map<? extends Unit<?>, ? extends Number> components;
        if (unscaled instanceof AbstractUnit<?>) {
            // In Apache SIS implementation, power may be fractional.
            final Map<SystemUnit<?>, Fraction> c = ((AbstractUnit<?>) unscaled).getBaseSystemUnits();
            components = c;
            for (final Map.Entry<SystemUnit<?>, Fraction> e : c.entrySet()) {
                final Fraction power = e.getValue();
                if (power.signum() > 0) {
                    hasNumerator = true;
                    if (prefixPower == 0 && power.denominator == 1 && e.getKey().isPrefixable()) {
                        prefixPower = power.numerator;
                    } else {
                        prefixPower = 0;
                        break;
                    }
                }
            }
        } else {
            // Fallback for foreigner implementations (power restricted to integer).
            Map<? extends Unit<?>, Integer> c = unscaled.getBaseUnits();
            if (c == null) c = Map.of(unit, 1);
            components = c;
            for (final Map.Entry<? extends Unit<?>, Integer> e : c.entrySet()) {
                final int power = e.getValue();
                if (power > 0) {
                    hasNumerator = true;
                    if (prefixPower == 0 && AbstractUnit.isPrefixable(e.getKey())) {
                        prefixPower = power;
                    } else {
                        prefixPower = 0;
                        break;
                    }
                }
            }
        }
        /*
         * Append the scale factor. If we can use a prefix (e.g. "km" instead of "1000⋅m"), we will do that.
         * Otherwise if the scale is a power of 10 and we are allowed to use Unicode symbols, we will write
         * for example 10⁵⋅m instead of 100000⋅m. If the scale is not a power of 10, or if we are requested
         * to format UCUM symbol, then we fallback on the usual `Double.toString(double)` representation.
         */
        if (scale != 1) {
            final char prefix = Prefixes.symbol(scale, prefixPower);
            if (prefix != 0) {
                toAppendTo.append(Prefixes.concat(prefix, ""));
            } else {
                boolean asPowerOf10 = (style != Style.UCUM);
                if (asPowerOf10) {
                    double power = Math.log10(scale);
                    asPowerOf10 = AbstractConverter.epsilonEquals(power, power = Math.round(power));
                    if (asPowerOf10) {
                        toAppendTo.append("10");
                        final String text = Integer.toString((int) power);
                        for (int i=0; i<text.length(); i++) {
                            toAppendTo.append(Characters.toSuperScript(text.charAt(i)));
                        }
                    }
                }
                if (!asPowerOf10) {
                    final String text = Double.toString(scale);
                    int length = text.length();
                    if (text.endsWith(".0")) length -= 2;
                    toAppendTo.append(text, 0, length);
                }
                /*
                 * The `formatComponents` method appends division symbol only, no multiplication symbol.
                 * If we have formatted a scale factor and there is at least one component to multiply,
                 * we need to append the multiplication symbol ourselves. Note that `formatComponents`
                 * put numerators before denominators, so we are sure that the first term after the
                 * multiplication symbol is a numerator.
                 */
                if (hasNumerator) {
                    toAppendTo.append(style.multiply);
                }
            }
        } else if (!hasNumerator) {
            toAppendTo.append('1');
        }
        formatComponents(components, style, toAppendTo);
        return toAppendTo;
    }

    /**
     * Creates a new symbol (e.g. "m/s") from the given symbols and factors.
     * Keys in the given map can be either {@link Unit} or {@link Dimension} instances.
     * Values in the given map are either {@link Integer} or {@link Fraction} instances.
     *
     * @param  components  the components of the symbol to format.
     * @param  style       whether to allow Unicode characters.
     * @param  toAppendTo  where to write the symbol.
     */
    static void formatComponents(final Map<?, ? extends Number> components, final Style style, final Appendable toAppendTo)
            throws IOException
    {
        boolean isFirst = true;
        final var deferred = new ArrayList<Map.Entry<?,? extends Number>>(components.size());
        for (final Map.Entry<?,? extends Number> entry : components.entrySet()) {
            final Number power = entry.getValue();
            final int n = (power instanceof Fraction) ? ((Fraction) power).numerator : power.intValue();
            if (n > 0) {
                if (!isFirst) {
                    toAppendTo.append(style.multiply);
                }
                isFirst = false;
                formatComponent(entry, false, style, toAppendTo);
            } else if (n != 0) {
                deferred.add(entry);
            }
        }
        /*
         * At this point, all numerators have been appended. Now append the denominators together.
         * For example, pressure dimension is formatted as M∕(L⋅T²) no matter if 'M' was the first
         * dimension in the given `components` map or not.
         */
        if (!deferred.isEmpty()) {
            toAppendTo.append(style.divide);
            final boolean useParenthesis = (deferred.size() > 1);
            if (useParenthesis) {
                toAppendTo.append(Style.OPEN);
            }
            isFirst = true;
            for (final Map.Entry<?,? extends Number> entry : deferred) {
                if (!isFirst) {
                    toAppendTo.append(style.multiply);
                }
                isFirst = false;
                formatComponent(entry, true, style, toAppendTo);
            }
            if (useParenthesis) {
                toAppendTo.append(Style.CLOSE);
            }
        }
    }

    /**
     * Formats a single unit or dimension raised to the given power.
     *
     * @param  entry    the base unit or base dimension to format, together with its power.
     * @param  inverse  {@code true} for inverting the power sign.
     * @param  style    whether to allow Unicode characters.
     */
    private static void formatComponent(final Map.Entry<?,? extends Number> entry, final boolean inverse,
            final Style style, final Appendable toAppendTo) throws IOException
    {
        formatSymbol(entry.getKey(), style, toAppendTo);
        final Number power = entry.getValue();
        int n;
        if (power instanceof Fraction) {
            Fraction f = (Fraction) power;
            if (f.denominator != 1) {
                if (inverse) {
                    f = f.negate();
                }
                style.appendPower(toAppendTo, f);
                return;
            }
            n = f.numerator;
        } else {
            n = power.intValue();
        }
        if (inverse) n = -n;
        if (n != 1) {
            style.appendPower(toAppendTo, n);
        }
    }

    /**
     * Appends the symbol for the given base unit of base dimension, or "?" if no symbol was found.
     * If the given object is a unit, then it should be an instance of {@link SystemUnit}.
     *
     * @param  base        the base unit or base dimension to format.
     * @param  style       whether to allow Unicode characters.
     * @param  toAppendTo  where to append the symbol.
     */
    private static void formatSymbol(final Object base, final Style style, final Appendable toAppendTo) throws IOException {
        if (base instanceof UnitDimension) {
            final char symbol = ((UnitDimension) base).symbol;
            if (symbol != 0) {
                toAppendTo.append(symbol);
                return;
            }
        }
        if (base instanceof Unit<?>) {
            final String symbol = ((Unit<?>) base).getSymbol();
            if (symbol != null) {
                style.appendSymbol(toAppendTo, symbol);
                return;
            }
        }
        toAppendTo.append(SystemUnit.UNFORMATTABLE);
    }

    /**
     * Formats the specified unit in the given buffer.
     * This method delegates to {@link #format(Unit, Appendable)}.
     *
     * @param  unit        the unit to format.
     * @param  toAppendTo  where to format the unit.
     * @param  pos         where to store the position of a formatted field, or {@code null} if none.
     * @return the given {@code toAppendTo} argument, for method calls chaining.
     */
    @Override
    public StringBuffer format(final Object unit, final StringBuffer toAppendTo, final FieldPosition pos) {
        try {
            return (StringBuffer) format((Unit<?>) unit, toAppendTo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);          // Should never happen since we are writting to a StringBuffer.
        }
    }

    /**
     * Formats the given unit.
     * This method delegates to {@link #format(Unit, Appendable)}.
     *
     * @param  unit  the unit to format.
     * @return the formatted unit.
     */
    @Override
    public String format(final Unit<?> unit) {
        try {
            return format(unit, new StringBuilder()).toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);      // Should never happen since we are writting to a StringBuilder.
        }
    }

    /**
     * Returns {@code 0} or {@code 1} if the {@code '*'} character at the given index stands for exponentiation
     * instead of multiplication, or a negative value if the character stands for multiplication. This check
     * is used for heuristic rules at parsing time. Current implementation applies the following rules:
     *
     * <ul>
     *   <li>The operation is presumed an exponentiation if the '*' symbol is doubled, as in {@code "m**s-1"}.</li>
     *   <li>The operation is presumed an exponentiation if it is surrounded by digits or a sign on its right side.
     *       Example: {@code "10*-6"}, which means 1E-6 in UCUM syntax.</li>
     *   <li>All other cases are currently presumed multiplication.
     *       Example: {@code "m*s"}.</li>
     * </ul>
     *
     * @return -1 for parsing as a multiplication, or a positive value for exponentiation.
     *         If positive, this is the number of characters in the exponent symbol minus 1.
     */
    private static int exponentOperator(final CharSequence symbols, int i, final int length) {
        if (i >= 0 && ++i < length) {
            final char c = symbols.charAt(i);           // No need for code point because next conditions are true only in BMP.
            if (c == Style.EXPONENT_OR_MULTIPLY) {
                return 1;                               // "**" operator: need to skip one character after '*'.
            }
            if ((isDigit(c) || isSign(c)) && isDigit(symbols.charAt(i-2))) {
                return 0;                               // "*" operator surrounded by digits: no character to skip.
            }
        }
        return -1;
    }

    /**
     * Returns {@code true} if the {@code '.'} character at the given index is surrounded by digits or
     * is at the beginning or the end of the character sequences. This check is used for heuristic rules.
     *
     * @see Style#UCUM
     */
    private static boolean isDecimalSeparator(final CharSequence symbols, int i, final int length) {
        return (i   == 0      || isDigit(symbols.charAt(i-1)) &&
               (++i >= length || isDigit(symbols.charAt(i))));
    }

    /**
     * Returns {@code true} if the given character is a digit in the sense of the {@code UnitFormat} parser.
     * Note that "digit" is taken here in a much more restrictive way than {@link Character#isDigit(int)}.
     *
     * <p>A return value of {@code true} guarantees that the given character is in the Basic Multilingual Plane (BMP).
     * Consequently, the {@code c} argument value does not need to be the result of {@link String#codePointAt(int)};
     * the result of {@link String#charAt(int)} is sufficient. We nevertheless use the {@code int} type for avoiding
     * the need to cast if caller uses code points for another reason.</p>
     *
     * @see Character#isBmpCodePoint(int)
     */
    private static boolean isDigit(final int c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Returns {@code true} if the given character is the sign of a number according the {@code UnitFormat} parser.
     * A return value of {@code true} guarantees that the given character is in the Basic Multilingual Plane (BMP).
     * Consequently, the {@code c} argument value does not need to be the result of {@link String#codePointAt(int)}.
     */
    private static boolean isSign(final int c) {
        return c == '+' || c == '-';
    }

    /**
     * Returns {@code true} if the given character is the sign of a division operator.
     * A return value of {@code true} guarantees that the given character is in the Basic Multilingual Plane (BMP).
     * Consequently, the {@code c} argument value does not need to be the result of {@link String#codePointAt(int)}.
     */
    private static boolean isDivisor(final int c) {
        return c == '/' || c == AbstractUnit.DIVIDE;
    }

    /**
     * Returns {@code true} if the given character sequence contains at least one digit.
     * This is a hack for allowing to recognize units like "100 feet" (in principle not
     * legal, but seen in practice). This verification has some value if digits are not
     * allowed as unit label or symbol.
     */
    private static boolean hasDigit(final CharSequence symbol, int lower, final int upper) {
        while (lower < upper) {
            if (isDigit(symbol.charAt(lower++))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse position when text to be parsed is expected to contain nothing else than a unit symbol.
     * This is used for recording whether another term (separated from the previous term by a space)
     * is allowed or not.
     */
    private static final class Position extends ParsePosition {
        /** {@code true} if we do not expect any more content after the last term parsed. */
        boolean finished;

        /** Creates a new position initialized to the beginning of the text to parse. */
        Position() {
            super(0);
        }
    }

    /**
     * Reports that the parsing is finished and no more content should be parsed.
     * This method is invoked when the last parsed term is possibly one or more words instead of unit symbols.
     * The intent is to avoid trying to parse "degree minute" as "degree × minute". By contrast, this method is
     * not invoked if the string to parse is "m kg**-2" because it can be interpreted as "m × kg**-2".
     */
    private static void finish(final ParsePosition pos) {
        if (pos instanceof Position) {
            ((Position) pos).finished = true;
        }
    }

    /**
     * Parses the given text as an instance of {@code Unit}.
     * If the parse completes without reading the entire length of the text, an exception is thrown.
     *
     * <p>The parsing is lenient: symbols can be products or quotients of units like “m∕s”,
     * words like “meters per second”, or authority codes like {@code "urn:ogc:def:uom:EPSG::1026"}.
     * The product operator can be either {@code '.'} (ASCII) or {@code '⋅'} (Unicode) character.
     * Exponent after symbol can be decimal digits as in “m2” or a superscript as in “m²”.</p>
     *
     * <p>This method differs from {@link #parse(CharSequence, ParsePosition)} in the treatment of white spaces:
     * that method with a {@link ParsePosition} argument stops parsing at the first white space,
     * while this {@code parse(…)} method treats white spaces as multiplications.
     * The reason for this difference is that white space is normally not a valid multiplication symbol;
     * it could be followed by a text which is not part of the unit symbol.
     * But in the case of this {@code parse(CharSequence)} method, the whole {@code CharSequence} shall be a unit symbol.
     * In such case, white spaces are less ambiguous.</p>
     *
     * <p>The default implementation delegates to
     * <code>{@linkplain #parse(CharSequence, ParsePosition) parse}(symbols, new ParsePosition(0))</code>
     * and verifies that all non-white characters have been parsed.
     * Units separated by spaces are multiplied; for example "kg m**-2" is parsed as kg/m².</p>
     *
     * @param  symbols  the unit symbols or URI to parse.
     * @return the unit parsed from the specified symbols.
     * @throws MeasurementParseException if a problem occurred while parsing the given symbols.
     *
     * @see Units#valueOf(String)
     */
    @Override
    public Unit<?> parse(final CharSequence symbols) throws MeasurementParseException {
        final var position = new Position();
        Unit<?> unit = parse(symbols, position);
        final int length = symbols.length();
        int unrecognized;
        while ((unrecognized = CharSequences.skipLeadingWhitespaces(symbols, position.getIndex(), length)) < length) {
            if (position.finished || !Character.isLetter(Character.codePointAt(symbols, unrecognized))) {
                throw new MeasurementParseException(Errors.format(Errors.Keys.UnexpectedCharactersAfter_2,
                        CharSequences.trimWhitespaces(symbols, 0, unrecognized),
                        CharSequences.trimWhitespaces(symbols, unrecognized, length)),
                        symbols, unrecognized);
            }
            position.setIndex(unrecognized);
            unit = unit.multiply(parse(symbols, position));
        }
        return unit;
    }

    /**
     * Parses a portion of the given text as an instance of {@code Unit}.
     * Parsing begins at the index given by {@link ParsePosition#getIndex()}.
     * After parsing, the above-cited index is updated to the first unparsed character.
     *
     * <p>The parsing is lenient: symbols can be products or quotients of units like “m∕s”,
     * words like “meters per second”, or authority codes like {@code "urn:ogc:def:uom:EPSG::1026"}.
     * The product operator can be either {@code '.'} (ASCII) or {@code '⋅'} (Unicode) character.
     * Exponent after symbol can be decimal digits as in “m2” or a superscript as in “m²”.</p>
     *
     * <p>Note that contrarily to {@link #parseObject(String, ParsePosition)}, this method never return {@code null}.
     * If an error occurs at parsing time, an unchecked {@link MeasurementParseException} is thrown.</p>
     *
     * @param  symbols  the unit symbols to parse.
     * @param  position on input, index of the first character to parse.
     *                  On output, index after the last parsed character.
     * @return the unit parsed from the specified symbols.
     * @throws MeasurementParseException if a problem occurred while parsing the given symbols.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public Unit<?> parse(CharSequence symbols, final ParsePosition position) throws MeasurementParseException {
        ArgumentChecks.ensureNonNull("symbols",  symbols);
        ArgumentChecks.ensureNonNull("position", position);
        /*
         * Check for authority codes (currently only EPSG, but more could be added later).
         * Example: "urn:ogc:def:uom:EPSG::9001". If the unit is not an authority code
         * (which is the most common case), only then we will parse the unit symbols.
         */
        int end   = symbols.length();
        int start = CharSequences.skipLeadingWhitespaces(symbols, position.getIndex(), end);
        if (AUTHORITIES != null) {
            final Map.Entry<String, String> entry = DefinitionURI.codeOf("uom", AUTHORITIES, symbols);
            if (entry != null) {
                Unit<?> unit = null;
                NumberFormatException failure = null;
                final String code = entry.getValue();
                final String authority = entry.getKey();
                switch (authority) {
                    case Constants.EPSG: {
                        try {
                            unit = Units.valueOfEPSG(Integer.parseInt(code));
                        } catch (NumberFormatException e) {
                            failure = e;
                        }
                        break;
                    }
                    case Constants.UCUM: {
                        unit = parse(code);
                        break;
                    }
                }
                if (unit != null) {
                    position.setIndex(end);
                    finish(position);
                    return unit;
                }
                String message = Errors.format(Errors.Keys.UnknownUnit_1, authority + Constants.DEFAULT_SEPARATOR + code);
                int errorOffset = start + Math.max(0, symbols.toString().lastIndexOf(code));
                throw (MeasurementParseException) new MeasurementParseException(message, symbols, errorOffset).initCause(failure);
            }
        }
        /*
         * Split the unit around the multiplication and division operators and parse each term individually.
         * Note that exponentation need to be kept as part of a single unit symbol.
         *
         * The `start` variable is the index of the first character of the next unit term to parse.
         */
        final var operation = new Operation(symbols);    // Enumeration value: NOOP, IMPLICIT, MULTIPLY, DIVIDE.
        Unit<?> unit = null;
        boolean hasSpaces = false;
        int i = start;
scan:   for (int n; i < end; i += n) {
            final int c = Character.codePointAt(symbols, i);
            n = Character.charCount(c);
            final int next;
            switch (c) {
                /*
                 * The minus sign can be both part of a number or part of a symbol. If the minus sign if followed
                 * by a digit, then handle it as part of a number, in which case the action is only "continue".
                 * Otherwise handle as part of a symbol, in which case the action is in the default case below.
                 * The intent is to prevent the replacement of Operation.IMPLICIT by Operation.MULTIPLY in symbol
                 * like "(m²⋅s)-1" because we want the "-1" part to be handled as Operation.EXPONENT instead.
                 */
                case '-': {
                    if (i + n < end && Character.isDigit(Character.codePointAt(symbols, i + n))) {
                        continue;
                    }
                    // else fall through.
                }
                /*
                 * For any character that is not an operator or parenthesis, either continue the scanning of
                 * characters or stop it, depending on whether the character is valid for a unit symbol or not.
                 * In the latter case, we consider that we reached the end of a unit symbol.
                 */
                default:  {
                    if (AbstractUnit.isSymbolChar(c)) {
                        if (operation.code == Operation.IMPLICIT) {
                            operation.code =  Operation.MULTIPLY;
                        }
                        continue;
                    }
                    if (Character.isDigit(c) || Characters.isSuperScript(c)) {
                        continue;
                    }
                    if (Character.isSpaceChar(c)) {                         // NOT Character.isWhitespace(int)
                        hasSpaces = true;
                        continue;
                    }
                    break scan;
                }
                /*
                 * Star is for exponentiation in UCUM syntax, but some symbols may use it for unit multiplication.
                 * We interpret the symbol as a multiplication if the characters before or after it seem to be for
                 * a unit symbol.
                 */
                case Style.EXPONENT_OR_MULTIPLY: {
                    final int w = exponentOperator(symbols, i, end);
                    if (w < 0) {
                        next = Operation.MULTIPLY;
                        break;
                    }
                    i += w;
                    // else fall through.
                }
                case Style.EXPONENT: {
                    if (operation.code == Operation.IMPLICIT) {
                        next = Operation.EXPONENT;
                        break;
                    }
                    continue;
                }
                /*
                 * The period is the multiplication operator in UCUM format. According UCUM there is no ambiguity
                 * with the decimal separator since unit terms should not contain floating point numbers. However
                 * we relax this rule in order to support scale factor of angular units (e.g. π/180).  The period
                 * is interpreted as a decimal separator if there is a decimal digit before and after it.
                 */
                case '.': if (isDecimalSeparator(symbols, i, end)) continue;
                case '×': // Fall through
                case AbstractUnit.MULTIPLY: next = Operation.MULTIPLY; break;
                case '÷':
                case '⁄': // Fraction slash
                case '/':
                case AbstractUnit.DIVIDE: next = Operation.DIVIDE; break;
                /*
                 * If we find an '(' parenthesis, invoke recursively this method for the part inside parenthesis.
                 * The parsing should end at the ')' parenthesis since it is not a valid unit symbol. If we do not
                 * find that closing parenthesis, this will be considered an error.
                 */
                case Style.OPEN: {
                    final int pos = i + Character.charCount(c);
                    final var sub = new ParsePosition(pos);
                    final Unit<?> term = parse(symbols, sub);
                    i = CharSequences.skipLeadingWhitespaces(symbols, sub.getIndex(), end);
                    if (i >= end || Character.codePointAt(symbols, i) != Style.CLOSE) {
                        throw new MeasurementParseException(Errors.format(Errors.Keys.NonEquilibratedParenthesis_2,
                                    symbols.subSequence(start, i), Style.CLOSE), symbols, start);
                    }
                    unit = operation.apply(unit, term, pos);
                    operation.code = Operation.IMPLICIT;    // Default operation if there is no × or / symbol after parenthesis.
                    start = i + (n = 1);                    // Skip the number of characters in the '(' Unicode code point.
                    continue;
                }
            }
            /*
             * We reach this point only if we found some operator (division or multiplication).
             * If the operator has been found between two digits, we consider it as part of the
             * term. For example, "m2/3" is considered as a single term where "2/3" is the exponent.
             */
            if (i > start && i+n < end
                    && Character.isDigit(Character.codePointBefore(symbols, i))
                    && Character.isDigit(Character.codePointAt(symbols, i+n)))
            {
                continue;
            }
            /*
             * At this point, we have either a first unit to parse (NOOP), or a multiplication or division to apply
             * between the previously parsed units and the next unit to parse. A special case is IMPLICIT, which is
             * a multiplication without explicit × symbol after the parenthesis. The implicit multiplication can be
             * overridden by an explicit × or / symbol, which is what happened if we reach this point (tip: look in
             * the above `switch` statement all cases that end with `break`, not `break scan` or `continue`).
             */
            if (operation.code != Operation.IMPLICIT) {
                unit = operation.apply(unit, parseTerm(symbols, start, i, operation), start);
            }
            hasSpaces = false;
            operation.code = next;
            start = i + n;
        }
        /*
         * At this point we either found an unrecognized character or reached the end of string. We will
         * parse the remaining characters as a unit and apply the pending unit operation (multiplication
         * or division). But before, we need to check if the parsing should stop at the first whitespace.
         * This verification assumes that spaces are allowed only in labels specified by the label(…)
         * method and in resource bundles, not in labels specified by AbstractUnit.alternate(String).
         */
        Unit<?> component = null;
        if (hasSpaces) {
            end = i;
            start = CharSequences.skipLeadingWhitespaces(symbols, start, i);
search:     while ((i = CharSequences.skipTrailingWhitespaces(symbols, start, i)) > start) {
                final String uom = symbols.subSequence(start, i).toString();
                if ((component = labelToUnit.get(uom)) != null) break;
                if ((component =        fromName(uom)) != null) break;
                int j=i, c;
                do {
                    c = Character.codePointBefore(symbols, j);
                    j -= Character.charCount(c);
                    if (j <= start) break search;
                } while (!Character.isWhitespace(c));
                /*
                 * Really use Character.isWhitespace(c) above, not Character.isSpaceChar(c), because we want
                 * to exclude non-breaking spaces.   This block should be the only place in UnitFormat class
                 * where we use isWhitespace(c) instead of isSpaceChar(c).
                 */
                i = j;                  // Will become the index of first space after search loop completion.
            }
            /*
             * At this point we did not found any user-specified label or localized name matching the substring.
             * Assume that the parsing should stop at the first space, on the basis that spaces are not allowed
             * in unit symbols. We make an exception if we detect that the part before the first space contains
             * digits (not allowed in unit symbols neither), in which case the substring may be something like
             * "100 feet".
             */
            if (hasDigit(symbols, start, i)) {
                i = end;                        // Restore the full length (until the first illegal character).
            }
        }
        if (!(operation.finished = (component != null))) {
            component = parseTerm(symbols, start, i, operation);            // May set `operation.finished` flag.
        }
        if (operation.finished) {
            finish(position);           // For preventing interpretation of "degree minute" as "degree × minute".
        }
        unit = operation.apply(unit, component, start);
        position.setIndex(i);
        return unit;
    }

    /**
     * Represents an operation to be applied between two terms parsed by
     * {@link UnitFormat#parseTerm(CharSequence, int, int, Operation)}.
     */
    private static final class Operation {
        /**
         * Meaning of some characters parsed by {@link UnitFormat#parse(CharSequence)}.
         * The {@code IMPLICIT} case is a multiplication without symbol, which can be
         * overridden by an explicit × or / symbol.
         */
        static final int NOOP = 0, IMPLICIT = 1, MULTIPLY = 2, DIVIDE = 3, EXPONENT = 4;

        /**
         * The operation as one of the {@link #NOOP}, {@link #IMPLICIT}, {@link #MULTIPLY}
         * or {@link #DIVIDE} values.
         */
        int code;

        /**
         * The symbols being parsed. Used only for formatting error message if needed.
         */
        private final CharSequence symbols;

        /**
         * {@code true} if the parsed terms may be one or more words, possibly containing white spaces.
         * In such case, the parsing should not continue after those words.
         *
         * @see Position#finished
         */
        boolean finished;

        /**
         * Creates an operation initialized to {@link #NOOP}.
         */
        Operation(final CharSequence symbols) {
            this.symbols = symbols;
        }

        /**
         * Applies a multiplication or division operation between the given units.
         *
         * @param  unit      the left operand, which is the unit parsed so far.
         * @param  term      the right operation, which is the newly parsed unit.
         * @param  position  the parse position to report if parsing fail.
         */
        Unit<?> apply(final Unit<?> unit, final Unit<?> term, final int position) {
            switch (code) {
                case NOOP:     return term;
                case IMPLICIT:
                case MULTIPLY: return unit.multiply(term);
                case DIVIDE:   return unit.divide(term);
                case EXPONENT: {
                    if (UnitDimension.isDimensionless(term.getDimension())
                             && Strings.isNullOrEmpty(term.getSymbol()))
                    {
                        final double scale = Units.toStandardUnit(term);
                        final int power = (int) scale;
                        if (power == scale) {
                            return unit.pow(power);
                        }
                    }
                    throw new MeasurementParseException(Errors.format(Errors.Keys.NotAnInteger_1, term), symbols, position);
                }
                default: throw new AssertionError(code);
            }
        }

        /**
         * If this operation is a multiplication, replaces by division. Otherwise do nothing
         * (we do <strong>not</strong> replace division by multiplication). The intent is to
         * replace units like "m⋅s-1" by "m/s".
         *
         * @return whether the operation has been inverted.
         */
        boolean invert() {
            switch (code) {
                case IMPLICIT:
                case MULTIPLY: code = DIVIDE; return true;
                default: return false;
            }
        }
    }

    /**
     * Parses a single unit symbol with its exponent.
     * The given symbol shall not contain multiplication or division operator except in exponent.
     * Parsing of fractional exponent as in "m2/3" is supported; other operations in the exponent
     * will cause an exception to be thrown.
     *
     * @param  symbols    the complete string specified by the user.
     * @param  lower      index where to begin parsing in the {@code symbols} string.
     * @param  upper      index after the last character to parse in the {@code symbols} string.
     * @param  operation  the operation to be applied (e.g. the term to be parsed is a multiplier or divisor of another unit).
     * @return the parsed unit symbol (never {@code null}).
     * @throws MeasurementParseException if a problem occurred while parsing the given symbols.
     */
    @SuppressWarnings("fallthrough")
    private Unit<?> parseTerm(final CharSequence symbols, final int lower, final int upper, final Operation operation)
            throws MeasurementParseException
    {
        final String uom = CharSequences.trimWhitespaces(symbols, lower, upper).toString();
        /*
         * Check for labels explicitly given by users. Those labels have precedence over the Apache SIS hard-coded
         * symbols. If no explicit label was found, check for symbols and names known to this UnitFormat instance.
         */
        Unit<?> unit = labelToUnit.get(uom);
        operation.finished = (unit != null);
        if (unit == null) {
            unit = Prefixes.getUnit(uom);
            if (unit == null) {
                final int length = uom.length();
                if (length == 0) {
                    return Units.UNITY;
                } else {
                    /*
                     * If the first character is a digit, presume that the term is a multiplication factor.
                     * The "*" character is used for raising the number on the left to the power on the right.
                     * Example: "10*6" is equal to one million. SIS also handles the "^" character as "*".
                     *
                     * In principle, spaces are not allowed in unit symbols (in particular, UCUM specifies that
                     * spaces should not be interpreted as multication operators). However, in practice we have
                     * sometimes units written in a form like "100 feet".
                     *
                     * If the last character is a super-script, then we assume a notation like "10⁻⁴".
                     */
                    final char c = uom.charAt(0);       // No need for code point because next condition is true only for BMP.
                    if (isDigit(c) || isSign(c)) {
                        final double multiplier;
                        try {
                            int s = uom.indexOf(' ');
                            if (s >= 0) {
                                final int next = CharSequences.skipLeadingWhitespaces(uom, s, length);
                                if (next < length && AbstractUnit.isSymbolChar(uom.codePointAt(next))) {
                                    operation.finished = true;  // For preventing attempt to continue parsing after "100 feet".
                                    multiplier = Double.parseDouble(uom.substring(0, s));
                                    return parseTerm(uom, s, length, new Operation(uom)).multiply(multiplier);
                                }
                            }
                            multiplier = parseMultiplicationFactor(uom);
                        } catch (NumberFormatException e) {
                            throw (MeasurementParseException) new MeasurementParseException(Errors.format(
                                    Errors.Keys.UnknownUnit_1, uom), symbols, lower).initCause(e);
                        }
                        if (operation.code == Operation.IMPLICIT) {
                            operation.code = Operation.EXPONENT;
                        }
                        return Units.UNITY.multiply(multiplier);
                    }
                }
                if (length >= 2) {
                    /*
                     * If the symbol ends with a digit (normal script or superscript), presume that this is the unit
                     * exponent.  That exponent can be a Unicode character (only one character in current UnitFormat
                     * implementation) or a number parseable with Integer.parseInt(String).
                     */
                    Fraction power = null;
                    int i = length;
                    int c = uom.codePointBefore(i);
                    i -= Character.charCount(c);
                    if (Characters.isSuperScript(c)) {
                        c = Characters.toNormalScript(c);
                        if (isDigit(c)) {
                            power = new Fraction(c - '0', 1);
                        }
                    } else if (isDigit(c)) {
                        while (i != 0) {
                            c = uom.codePointBefore(i);
                            final boolean isExponent = isDigit(c) || isDivisor(c);
                            if (isExponent || isSign(c)) {
                                i -= Character.charCount(c);
                            }
                            if (!isExponent) {
                                try {
                                    power = new Fraction(uom.substring(i));
                                } catch (NumberFormatException e) {
                                    // Should never happen unless the number is larger than `int` capacity.
                                    throw (MeasurementParseException) new MeasurementParseException(Errors.format(
                                            Errors.Keys.UnknownUnit_1, uom), symbols, lower+i).initCause(e);
                                }
                                break;
                            }
                        }
                    }
                    if (power != null) {
                        /*
                         * At this point we have parsed the exponent. Before to parse the raw unit symbol,
                         * skip the exponent symbol (^, * or **) if any.
                         */
                        i = CharSequences.skipTrailingWhitespaces(uom, 0, i);
                        if (i != 0) {
                            // No need for code point because next conditions are true only in BMP.
                            switch (uom.charAt(i-1)) {
                                case Style.EXPONENT_OR_MULTIPLY: {
                                    if (i != 1 && uom.charAt(i-2) == Style.EXPONENT_OR_MULTIPLY) i--;
                                    // Fallthrough for skipping the next character and whitespaces.
                                }
                                case Style.EXPONENT: {
                                    i = CharSequences.skipTrailingWhitespaces(uom, 0, i - 1);
                                    break;
                                }
                            }
                        }
                        final String symbol = uom.substring(CharSequences.skipLeadingWhitespaces(uom, 0, i), i);
                        unit = labelToUnit.get(symbol);
                        operation.finished = (unit != null);
                        if (unit == null) {
                            unit = Prefixes.getUnit(symbol);
                        }
                        if (unit != null) {
                            int numerator   = power.numerator;
                            int denominator = power.denominator;
                            if (numerator < 0 && operation.invert()) {
                                numerator = -numerator;
                            }
                            if (numerator   != 1) unit = unit.pow (numerator);
                            if (denominator != 1) unit = unit.root(denominator);
                            return unit;
                        }
                    }
                }
                /*
                 * At this point, we have determined that the label is not a known unit symbol.
                 * It may be a unit name, in which case the label is not case-sensitive anymore.
                 */
                operation.finished = true;
                unit = fromName(uom);
                if (unit == null) {
                    if (CharSequences.regionMatches(symbols, lower, UNITY, true)) {
                        return Units.UNITY;
                    }
                    throw new MeasurementParseException(Errors.format(Errors.Keys.UnknownUnit_1, uom), symbols, lower);
                }
            }
        }
        return unit;
    }

    /**
     * Parses a multiplication factor, which may be a single number or a base raised to an exponent.
     * For example, all the following strings are equivalent: "1000", "1000.0", "1E3", "10*3", "10^3", "10³".
     */
    private static double parseMultiplicationFactor(final String term) throws NumberFormatException {
        final String exponent;
        int s = term.lastIndexOf(Style.EXPONENT_OR_MULTIPLY);        // Check standard UCUM symbol first.
        if (s >= 0 || (s = term.lastIndexOf(Style.EXPONENT)) >= 0) {
            exponent = term.substring(s + 1);
        } else {
            s = term.length();
            int c = term.codePointBefore(s);
            if (!Characters.isSuperScript(c)) {
                return Double.parseDouble(term);                     // No exponent symbol and no superscript found.
            }
            // Example: "10⁻⁴". Split in base and exponent.
            final var buffer = new StringBuilder(s);
            do {
                buffer.appendCodePoint(Characters.toNormalScript(c));
                if ((s -= Character.charCount(c)) <= 0) break;
                c = term.codePointBefore(s);
            } while (Characters.isSuperScript(c));
            exponent = buffer.reverse().toString();
        }
        final int base = Integer.parseInt(term.substring(0, s));
        final int exp  = Integer.parseInt(exponent);
        return (base == 10) ? MathFunctions.pow10(exp) : Math.pow(base, exp);
    }

    /**
     * Parses text from a string to produce a unit. The default implementation delegates
     * to {@link #parse(CharSequence)} and wraps the {@link MeasurementParseException}
     * into a {@link ParseException} for compatibility with {@code java.text} API.
     *
     * @param  source  the text, part of which should be parsed.
     * @return a unit parsed from the string.
     * @throws ParseException if the given string cannot be fully parsed.
     */
    @Override
    public Object parseObject(final String source) throws ParseException {
        try {
            return parse(source);
        } catch (MeasurementParseException e) {
            throw (ParseException) new ParseException(e.getLocalizedMessage(), e.getPosition()).initCause(e);
        }
    }

    /**
     * Parses text from a string to produce a unit, or returns {@code null} if the parsing failed.
     * The default implementation delegates to {@link #parse(CharSequence, ParsePosition)} and catches
     * the {@link MeasurementParseException}.
     *
     * @param  source  the text, part of which should be parsed.
     * @param  pos     index and error index information as described above.
     * @return a unit parsed from the string, or {@code null} in case of error.
     */
    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        try {
            return parse(source, pos);
        } catch (MeasurementParseException e) {
            pos.setErrorIndex(e.getPosition());
            return null;
        }
    }

    /**
     * Returns a clone of this unit format. The new unit format will be initialized to the same
     * {@linkplain #getLocale() locale} and {@linkplain #label(Unit, String) labels} than this format.
     *
     * @return a clone of this unit format.
     */
    @Override
    public UnitFormat clone() {
        final UnitFormat f = (UnitFormat) super.clone();
        try {
            f.clone("unitToLabel");
            f.clone("labelToUnit");
        } catch (ReflectiveOperationException e) {
            throw (InaccessibleObjectException) new InaccessibleObjectException().initCause(e);
        }
        return f;
    }

    /**
     * Clones the map in the specified field.
     * The map can be either a {@link HashMap} or the instance returned by {@link Map#of()}.
     */
    private void clone(final String field) throws ReflectiveOperationException {
        final var f = UnitFormat.class.getDeclaredField(field);
        f.setAccessible(true);
        Object value = f.get(this);
        if (value instanceof HashMap<?,?>) {
            value = ((HashMap<?,?>) value).clone();
        } else {
            value = new HashMap<>();
        }
        f.set(this, value);
    }
}
