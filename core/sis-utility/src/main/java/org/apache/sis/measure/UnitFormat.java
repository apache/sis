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
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.io.IOException;
import javax.measure.Dimension;
import javax.measure.Unit;
import javax.measure.format.ParserException;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.DefinitionURI;
import org.apache.sis.internal.util.XPaths;
import org.apache.sis.math.Fraction;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Localized;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.util.collection.WeakValueHashMap;


/**
 * Parses and formats units of measurement as SI symbols, URI in OGC namespace or other symbols.
 * This class combines in a single class the API from {@link java.text} and the API from {@link javax.measure.format}.
 * In addition to the symbols of the <cite>Système international</cite> (SI), this class is also capable to handle
 * some symbols found in <cite>Well Known Text</cite> (WKT) definitions or in XML files.
 *
 * <div class="section">Parsing authority codes</div>
 * As a special case, if a character sequence given to the {@link #parse(CharSequence)} method is of the
 * {@code "EPSG:####"} or {@code "urn:ogc:def:uom:EPSG::####"} form (ignoring case and whitespaces),
 * then {@code "####"} is parsed as an integer and forwarded to the {@link Units#valueOfEPSG(int)} method.
 *
 * <div class="section">NetCDF unit symbols</div>
 * The attributes in NetCDF files often merge the axis direction with the angular unit,
 * as in {@code "degrees_east"} or {@code "degrees_north"}.
 * This class ignores those suffixes and unconditionally returns {@link Units#DEGREE} for all axis directions.
 * In particular, the units for {@code "degrees_west"} and {@code "degrees_east"} do <strong>not</strong> have
 * opposite sign. It is caller responsibility to handle the direction of axes associated to NetCDF units.
 *
 * <div class="section">Multi-threading</div>
 * {@code UnitFormat} is generally not thread-safe.
 * However if there is no call to any setter method or to {@link #label(Unit, String)} after construction,
 * then the {@link #parse(CharSequence)} and {@link #format(Unit)} methods can be invoked concurrently in
 * different threads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see Units#valueOf(String)
 */
public class UnitFormat extends Format implements javax.measure.format.UnitFormat, Localized {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3064428584419360693L;

    /**
     * The SI "“deca” prefix. This is the only SI prefix encoded on two letters instead than one.
     * It can be represented by the CJK compatibility character “㍲”, but use of those characters
     * is generally not recommended outside of Chinese, Japanese or Korean texts.
     */
    static final String DECA = "da";

    /**
     * The default instance used by {@link Units#valueOf(String)} for parsing units of measurement.
     * While {@code UnitFormat} is generally not thread-safe, this particular instance is safe if
     * we never invoke any setter method.
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
     * For example the {@link Units#CUBIC_METRE} units can be formatted in the following ways:
     *
     * <ul>
     *   <li>As a symbol using Unicode characters: <b>m³</b></li>
     *   <li>As a symbol restricted to the ASCII characters set: <b>m3</b></li>
     *   <li>As a long name:<ul>
     *     <li>in English: <cite>cubic metre</cite></li>
     *     <li>in French: <cite>mètre cube</cite></li>
     *   </ul></li>
     * </ul>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.8
     * @version 0.8
     * @module
     */
    public static enum Style {
        /**
         * Format unit symbols using Unicode characters. Units formatted in this style use superscript digits
         * for exponents (as in “m³”), the dot operator (“⋅”) for multiplications, specialized characters when
         * they exist (e.g. U+212A “K” for Kelvin sign), <i>etc.</i>
         *
         * <p>This is the default style of {@link UnitFormat}.</p>
         *
         * @see Unit#getSymbol()
         */
        SYMBOL('⋅', '∕'),

        /**
         * Format unit symbols using a syntax close to the Unified Code for Units of Measure (UCUM) one.
         * The character set is restricted to ASCII. The multiplication operator is the period (“.”).
         *
         * <div class="section">Modification to UCUM syntax rules</div>
         * UCUM does not allow floating point numbers in unit terms, so the use of period as an operator
         * should not be ambiguous. However Apache SIS relaxes this restriction in order to support the
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
                toAppendTo.append("^(").append(String.valueOf(power.numerator))
                           .append('/').append(String.valueOf(power.denominator)).append(')');
            }
        },

        /**
         * Format unit symbols as localized long names if known, or Unicode symbols otherwise.
         *
         * @see Unit#getName()
         */
        NAME('⋅', '∕');

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
            final String value = power.toString();
            if (value.length() == 1) {
                toAppendTo.append('^').append(value);
            } else {
                toAppendTo.append("^(").append(value).append(')');
            }
        }
    }

    /**
     * Symbols or names to use for formatting unit in replacement to the default unit symbols or names.
     *
     * @see #label(Unit, String)
     */
    private final Map<Unit<?>,String> unitToLabel;

    /**
     * Units associated to a given label (in addition to the system-wide {@link UnitRegistry}).
     * This map is the converse of {@link #unitToLabel}.
     *
     * @see #label(Unit, String)
     */
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
     * @see #nameToUnit()
     */
    private transient volatile Map<String,Unit<?>> nameToUnit;

    /**
     * Cached values of {@link #nameToUnit}, for avoiding to load the same information many time and for saving memory
     * if the user create many {@code UnitFormat} instances. Note that we do not cache {@link #symbolToName} because
     * {@link ResourceBundle} already provides its own caching mechanism.
     *
     * @see #nameToUnit()
     */
    private static final WeakValueHashMap<Locale, Map<String,Unit<?>>> SHARED = new WeakValueHashMap<>(Locale.class);

    /**
     * Creates the unique {@link #INSTANCE}.
     */
    private UnitFormat() {
        locale      = Locale.ROOT;
        style       = Style.SYMBOL;
        unitToLabel = Collections.emptyMap();
        labelToUnit = Collections.emptyMap();
    }

    /**
     * Creates a new format for the given locale.
     *
     * @param  locale  the locale to use for parsing and formatting units.
     */
    public UnitFormat(final Locale locale) {
        ArgumentChecks.ensureNonNull("locale", locale);
        this.locale = locale;
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
     * For example a call to <code>setLocale({@linkplain Locale#US})</code>
     * instructs this formatter to use the “meter” spelling instead of “metre”.
     *
     * @param  locale  the new locale for this {@code UnitFormat}.
     *
     * @see UnitServices#getUnitFormat(String)
     */
    public void setLocale(final Locale locale) {
        ArgumentChecks.ensureNonNull("locale", locale);
        this.locale  = locale;
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
        ArgumentChecks.ensureNonNull("style", style);
        this.style = style;
    }

    /**
     * Attaches a label to the specified unit. A <cite>label</cite> can be a substitute to either the
     * {@linkplain AbstractUnit#getSymbol() unit symbol} or the {@link AbstractUnit#getName() unit name},
     * depending on the {@linkplain #getStyle() format style}.
     * If the specified label is already associated to another unit, then the previous association is discarded.
     *
     * <div class="section">Restriction on character set</div>
     * Current implementation accepts only {@linkplain Character#isLetter(int) letters},
     * {@linkplain Characters#isSubScript(int) subscripts}, {@linkplain Character#isWhitespace(int) whitespaces}
     * and the degree sign (°),
     * but the set of legal characters may be expanded in future Apache SIS versions.
     * However the following restrictions are likely to remain:
     *
     * <ul>
     *   <li>The following characters are reserved since they have special meaning in UCUM format, in URI
     *       or in Apache SIS parser: <blockquote>" # ( ) * + - . / : = ? [ ] { } ^ ⋅ ∕</blockquote></li>
     *   <li>The symbol can not begin or end with digits, since such digits would be confused with unit power.</li>
     * </ul>
     *
     * @param  unit   the unit being labeled.
     * @param  label  the new label for the given unit.
     * @throws IllegalArgumentException if the given label is not a valid unit name.
     */
    @Override
    public void label(final Unit<?> unit, String label) {
        ArgumentChecks.ensureNonNull ("unit", unit);
        label = CharSequences.trimWhitespaces(label);
        ArgumentChecks.ensureNonEmpty("label", label);
        for (int i=0; i < label.length();) {
            final int c = label.codePointAt(i);
            if (!AbstractUnit.isSymbolChar(c) && !Character.isWhitespace(c)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "label", label));
            }
            i += Character.charCount(c);
        }
        final Unit<?> unitForOldLabel = labelToUnit.remove(unitToLabel.put(unit, label));
        final Unit<?> oldUnitForLabel = labelToUnit.put(label, unit);
        if (oldUnitForLabel != null && !oldUnitForLabel.equals(unit) && !label.equals(unitToLabel.remove(oldUnitForLabel))) {
            // Assuming there is no bug in our algorithm, this exception should never happen
            // unless this UnitFormat has been modified concurrently in another thread.
            throw new CorruptedObjectException("unitToLabel");
        }
        if (unitForOldLabel != null && !unitForOldLabel.equals(unit)) {
            // Assuming there is no bug in our algorithm, this exception should never happen
            // unless this UnitFormat has been modified concurrently in another thread.
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
     * Returns the mapping from long localized and unlocalized names to unit instances.
     * This mapping is somewhat the converse of {@link #symbolToName()}, but includes
     * international and American spelling of unit names in addition of localized names.
     * The intend is to recognize "meter" as well as "metre".
     *
     * <p>While we said that {@code UnitFormat} is not thread safe, we make an exception for this method
     * for allowing the singleton {@link #INSTANCE} to parse symbols in a multi-threads environment.</p>
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private Map<String,Unit<?>> nameToUnit() {
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
                 * property files neither. It contains longer names somtime used (for example "decimal degree" instead
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
        return map;
    }

    /**
     * Copies all entries from the given "symbols to names" mapping to the given "names to units" mapping.
     * During this copy, keys are converted from symbols to names and values are converted from symbols to
     * {@code Unit} instance. We use {@code Unit} values instead of their symbols because all {@code Unit}
     * instances are created at {@link Units} class initialization anyway (so we do not create new instance
     * here), and it avoid to retain references to the {@link String} instances loaded by the resource bundle.
     */
    private static void copy(final Locale locale, final ResourceBundle symbolToName, final Map<String,Unit<?>> nameToUnit) {
        for (final String symbol : symbolToName.keySet()) {
            nameToUnit.put(CharSequences.toASCII(symbolToName.getString(symbol).toLowerCase(locale)).toString().intern(), Units.get(symbol));
        }
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
        String label = unitToLabel.get(unit);
        if (label != null) {
            return toAppendTo.append(label);
        }
        if (style == Style.NAME) {
            /*
             * Choice 2: value specified by Unit.getName(). We skip this check if the given Unit is an instance
             * implemented by Apache SIS because  AbstractUnit.getName()  delegates to the same resource bundle
             * than the one used by this block. We are better to use the resource bundle of the UnitFormat both
             * for performance reasons and because the locale may not be the same.
             */
            if (!(unit instanceof AbstractUnit)) {
                label = unit.getName();
                if (label != null) {
                    return toAppendTo.append(label);
                }
            } else {
                label = unit.getSymbol();
                if (label != null) {
                    if (label.isEmpty()) {
                        label = "unity";
                    }
                    // Following is not thread-safe, but it is okay since we do not use INSTANCE for unit names.
                    final ResourceBundle names = symbolToName();
                    try {
                        label = names.getString(label);
                    } catch (MissingResourceException e) {
                        // Name not found; use the symbol as a fallback.
                    }
                    return toAppendTo.append(label);
                }
            }
        }
        /*
         * Choice 3: if the unit has a specific symbol, appends that symbol.
         */
        label = unit.getSymbol();
        if (label != null) {
            return style.appendSymbol(toAppendTo, label);
        }
        /*
         * Choice 4: if all the above failed, fallback on a symbol created from the base units and their power.
         * Note that this may produce more verbose symbols than needed since derived units like Volt or Watt are
         * decomposed into their base SI units.
         */
        final double scale = Units.toStandardUnit(unit);
        if (scale != 1) {
            if (Double.isNaN(scale)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.NonRatioUnit_1, "?(" + unit.getSystemUnit() + ')'));
            }
            final String text = Double.toString(scale);
            int length = text.length();
            if (text.endsWith(".0")) {
                length -= 2;
            }
            toAppendTo.append(text, 0, length).append(style.multiply);
        }
        Map<? extends Unit<?>, ? extends Number> components;
        if (unit instanceof AbstractUnit<?>) {
            // In Apache SIS implementation, the powers may be ratios.
            components = ((AbstractUnit<?>) unit).getBaseSystemUnits();
        } else {
            // Fallback for foreigner implementations (powers restricted to integers).
            components = unit.getBaseUnits();
            if (components == null) {
                components = Collections.singletonMap(unit, 1);
            }
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
        final List<Map.Entry<?,? extends Number>> deferred = new ArrayList<>(components.size());
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
        // At this point, all numerators have been appended. Now append the denominators together.
        if (!deferred.isEmpty()) {
            toAppendTo.append(style.divide);
            final boolean useParenthesis = (deferred.size() > 1);
            if (useParenthesis) {
                toAppendTo.append('(');
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
                toAppendTo.append(')');
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
        toAppendTo.append('?');
    }

    /**
     * Formats the specified unit in the given buffer.
     * This method delegates to {@link #format(Unit, Appendable)}.
     *
     * @param  unit        the unit to format.
     * @param  toAppendTo  where to format the unit.
     * @param  pos         where to store the position of a formatted field.
     * @return the given {@code toAppendTo} argument, for method calls chaining.
     */
    @Override
    public StringBuffer format(final Object unit, final StringBuffer toAppendTo, final FieldPosition pos) {
        try {
            return (StringBuffer) format((Unit<?>) unit, toAppendTo);
        } catch (IOException e) {
            throw new AssertionError(e);      // Should never happen since we are writting to a StringBuffer.
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
            throw new AssertionError(e);      // Should never happen since we are writting to a StringBuilder.
        }
    }

    /**
     * Returns {@code true} if the given unit seems to be an URI.
     * Examples:
     * <ul>
     *   <li>{@code "urn:ogc:def:uom:EPSG::9001"}</li>
     *   <li>{@code "http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"}</li>
     * </ul>
     */
    private static boolean isURI(final CharSequence uom) {
        for (int i=uom.length(); --i>=0;) {
            final char c = uom.charAt(i);
            if (c == ':' || c == '#') {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the {@code '*'} character at the given index is surrounded by digits
     * or a sign on its right side. For example this method returns {@code true} for "10*-6", which
     * means 1E-6 in UCUM syntax. This check is used for heuristic rules at parsing time.
     */
    private static boolean isExponentOperator(final CharSequence symbols, int i, final int length) {
        char c;
        return (i != 0) && isDigit(symbols.charAt(i-1)) &&
               (++i < length) && (isDigit(c = symbols.charAt(i)) || isSign(c));
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
     */
    private static boolean isDigit(final int c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Returns {@code true} if the given character is the sign of a number according the {@code UnitFormat} parser.
     */
    private static boolean isSign(final int c) {
        return c == '+' || c == '-';
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
     * @param  symbols  the unit symbols or URI to parse.
     * @return the unit parsed from the specified symbols.
     * @throws ParserException if a problem occurred while parsing the given symbols.
     *
     * @see Units#valueOf(String)
     */
    @Override
    @SuppressWarnings( {"null", "fallthrough"})
    public Unit<?> parse(CharSequence symbols) throws ParserException {
        /*
         * Check for authority codes (currently only EPSG, but more could be added later).
         * If the unit is not an authority code (which is the most common case), then we
         * will check for hard-coded unit symbols.
         *
         * DefinitionURI.codeOf(…) returns 'uom' directly (provided that whitespaces were already trimmed)
         * if no ':' character were found, in which case the string is assumed to be the code directly.
         * This is the intended behavior for AuthorityFactory, but in the particular case of this method
         * we want to try to parse as a xpointer before to give up.
         */
        if (isURI(symbols)) {
            final String uom = symbols.toString();
            String code = DefinitionURI.codeOf("uom", Constants.EPSG, uom);
            if (code != null && code != uom) try {              // Really identity check, see above comment.
                return Units.valueOfEPSG(Integer.parseInt(code));
            } catch (NumberFormatException e) {
                throw (ParserException) new ParserException(
                        Errors.format(Errors.Keys.IllegalArgumentValue_2, "symbols", symbols),
                        uom, Math.max(0, uom.indexOf(code))).initCause(e);
            }
            code = XPaths.xpointer("uom", uom);
            if (code != null) {
                symbols = code;
            }
        }
        /*
         * Split the unit around the multiplication and division operators and parse each term individually.
         */
        int offset    = 0;                  // Index of the first character of the next unit term to parse.
        int operation = NOOP;               // Enumeration value: MULTIPLY, DIVIDE.
        Unit<?> unit  = null;
        final int length = symbols.length();
        for (int i=0; i<length; i++) {
            final char c = symbols.charAt(i);   // No need to use code points because we search characters in BMP.
            final int next;
            switch (c) {
                /*
                 * Star is for exponentiation in UCUM syntax, but some symbols may use it for unit multiplications.
                 * We interpret
                 */
                case '*': if (isExponentOperator(symbols, i, length)) continue;
                          next = MULTIPLY; break;
                /*
                 * The period is the multiplication operator in UCUM format. According UCUM there is no ambiguity
                 * with the decimal separator since unit terms should not contain floating point numbers. However
                 * we relax this rule in order to support scale factor of angular units (e.g. π/180).  The period
                 * is interpreted as a decimal separator if there is a decimal digit before and after it.
                 */
                case '.': if (isDecimalSeparator(symbols, i, length)) continue;
                case '⋅': // Fallthrough
                case '×': next = MULTIPLY; break;
                case '÷':
                case '⁄': // Fraction slash
                case '/':
                case '∕': next = DIVIDE; break;
                default:  continue;
            }
            final Unit<?> term = parseSymbol(symbols, offset, i);
            switch (operation) {
                case NOOP:     unit = term; break;
                case MULTIPLY: unit = unit.multiply(term); break;
                case DIVIDE:   unit = unit.divide(term); break;
                default: throw new AssertionError(operation);
            }
            operation = next;
            offset = i+1;
        }
        final Unit<?> term = parseSymbol(symbols, offset, length);
        switch (operation) {
            case NOOP:     unit = term; break;
            case MULTIPLY: unit = unit.multiply(term); break;
            case DIVIDE:   unit = unit.divide(term); break;
            default: throw new AssertionError(operation);
        }
        return unit;
    }

    /**
     * Meaning of some characters parsed by {@link #parse(CharSequence)}.
     */
    private static final int NOOP = 0, MULTIPLY = 1, DIVIDE = 2;

    /**
     * Parses a single unit symbol with its exponent.
     * The given symbol shall not contain multiplication or division operator.
     *
     * @param  symbols  the complete string specified by the user.
     * @param  lower    index where to begin parsing in the {@code symbols} string.
     * @param  upper    index after the last character to parse in the {@code symbols} string.
     * @return the parsed unit symbol (never {@code null}).
     * @throws ParserException if a problem occurred while parsing the given symbols.
     */
    @SuppressWarnings("fallthrough")
    private Unit<?> parseSymbol(final CharSequence symbols, final int lower, final int upper) throws ParserException {
        final String uom = CharSequences.trimWhitespaces(symbols, lower, upper).toString();
        /*
         * Check for labels explicitly given by users. Those labels have precedence over the Apache SIS hard-coded
         * symbols. If no explicit label was found, check for symbols and names known to this UnitFormat instance.
         */
        Unit<?> unit = labelToUnit.get(uom);
        if (unit == null) {
            unit = getPrefixed(uom);
            if (unit == null) {
                final int length = uom.length();
                if (length == 0) {
                    return Units.UNITY;
                } else {
                    /*
                     * If the first character is a digit, presume that the term is a multiplication factor.
                     * The "*" character is used for raising the number on the left to the power on the right.
                     * Example: "10*6" is equal to one million.
                     *
                     * In principle, spaces are not allowed in unit symbols (in particular, UCUM specifies that
                     * spaces should not be interpreted as multication operators).  However in practice we have
                     * sometime units written in a form like "100 feet".
                     */
                    final char c = uom.charAt(0);
                    if (isDigit(c) || isSign(c)) {
                        final double multiplier;
                        try {
                            int s = uom.lastIndexOf(' ');
                            if (s >= 0) {
                                final int next = CharSequences.skipLeadingWhitespaces(uom, s, length);
                                if (next < length && AbstractUnit.isSymbolChar(uom.codePointAt(next))) {
                                    multiplier = Double.parseDouble(uom.substring(0, s));
                                    return parseSymbol(uom, s, length).multiply(multiplier);
                                }
                            }
                            s = uom.lastIndexOf('*');
                            if (s >= 0) {
                                final int base = Integer.parseInt(uom.substring(0, s));
                                final int exp  = Integer.parseInt(uom.substring(s+1));
                                multiplier = Math.pow(base, exp);
                            } else {
                                multiplier = Double.parseDouble(uom);
                            }
                        } catch (NumberFormatException e) {
                            throw (ParserException) new ParserException(Errors.format(
                                    Errors.Keys.UnknownUnit_1, uom), symbols, lower).initCause(e);
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
                    int  power = 1;
                    int  i = length;
                    char c = uom.charAt(--i);
                    boolean canApply = false;
                    if (Characters.isSuperScript(c)) {
                        c = Characters.toNormalScript(c);
                        if (isDigit(c)) {
                            power = c - '0';
                            canApply = true;
                        }
                    } else if (isDigit(c)) {
                        do {
                            c = uom.charAt(--i);
                            if (!isDigit(c)) {
                                if (!isSign(c)) i++;
                                try {
                                    power = Integer.parseInt(uom.substring(i));
                                } catch (NumberFormatException e) {
                                    // Should never happen unless the number is larger than 'int' capacity.
                                    throw (ParserException) new ParserException(Errors.format(
                                            Errors.Keys.UnknownUnit_1, uom), symbols, lower+i).initCause(e);
                                }
                                canApply = true;
                                break;
                            }
                        } while (i != 0);
                    }
                    if (canApply) {
                        unit = getPrefixed(CharSequences.trimWhitespaces(uom, 0, i).toString());
                        if (unit != null) {
                            return unit.pow(power);
                        }
                    }
                }
                /*
                 * Check for degrees units. Note that "deg" could be both angular and Celsius degrees.
                 * We try to resolve this ambiguity in the code below by looking for the "C" suffix.
                 * We perform a special case for those checks because the above check for unit symbol
                 * is case-sentive, the check for unit name (later) is case-insensitive, while this
                 * check for "deg" is a mix of both.
                 */
                if (uom.regionMatches(true, 0, "deg", 0, 3)) {
                    switch (length) {
                        case 3: return Units.DEGREE;                    // Exactly "deg"  (ignoring case)
                        case 5: final char c = uom.charAt(3);
                                if (c != '_' && !Character.isSpaceChar(c)) break;
                                // else fallthrough
                        case 4: switch (uom.charAt(length - 1)) {
                                    case 'K':                           // Unicode U+212A
                                    case 'K': return Units.KELVIN;      // Exactly "degK" (ignoring case except for 'K')
                                    case 'C': return Units.CELSIUS;
                                }
                    }
                }
                /*
                 * At this point, we have determined that the label is not a known unit symbol.
                 * It may be a unit name, in which case the label is not case-sensitive anymore.
                 * The 'nameToUnit' map contains plural forms (declared in UnitAliases.properties),
                 * but we make a special case for "degrees", "metres" and "meters" because they
                 * appear in numerous places.
                 */
                String lc = uom.replace('_', ' ').toLowerCase(locale);
                lc = CharSequences.replace(CharSequences.replace(CharSequences.replace(CharSequences.toASCII(lc),
                        "meters",  "meter"),
                        "metres",  "metre"),
                        "degrees", "degree").toString();
                unit = nameToUnit().get(lc);
                if (unit == null) {
                    throw new ParserException(Errors.format(Errors.Keys.UnknownUnit_1, uom), symbols, lower);
                }
            }
        }
        return unit;
    }

    /**
     * Returns the unit for the given symbol, taking the SI prefix in account.
     * This method does not perform any arithmetic operation on {@code Unit}.
     * Returns {@code null} if no unit is found.
     */
    private static Unit<?> getPrefixed(final String uom) {
        Unit<?> unit = Units.get(uom);
        if (unit == null && uom.length() >= 2) {
            int s = 1;
            char prefix = uom.charAt(0);
            if (prefix == 'd' && uom.charAt(1) == 'a') {
                prefix = '㍲';
                s = 2;
            }
            unit = Units.get(uom.substring(s));
            if (unit instanceof SystemUnit<?> && ((SystemUnit<?>) unit).scope == UnitRegistry.SI) {
                final LinearConverter c = LinearConverter.forPrefix(prefix);
                if (c != null) {
                    String symbol = unit.getSymbol();
                    if (prefix == '㍲') {
                        symbol = DECA + symbol;
                    } else {
                        symbol = prefix + symbol;
                    }
                    return new ConventionalUnit<>((SystemUnit<?>) unit, c, symbol.intern(), (byte) 0, (short) 0);
                }
            }
            unit = null;
        }
        return unit;
    }

    /**
     * Parses the given text as an instance of {@code Unit}.
     *
     * @param  symbols  the unit symbols to parse.
     * @param  pos      on input, index of the first character to parse.
     *                  On output, index after the last parsed character.
     * @return the unit parsed from the specified symbols.
     * @throws ParserException if a problem occurred while parsing the given symbols.
     */
    public Unit<?> parse(final CharSequence symbols, final ParsePosition pos) {
        final int start = pos.getIndex();
        int stop = start;
        while (stop < symbols.length()) {
            final int c = Character.codePointAt(symbols, stop);
            if (Character.isWhitespace(c) || c == ']') break;       // Temporary hack before we complete the JSR-275 replacement.
            stop += Character.charCount(c);
        }
        try {
            final Unit<?> unit = parse(symbols.subSequence(start, stop));
            pos.setIndex(stop);
            return unit;
        } catch (ParserException e) {
            pos.setErrorIndex(start);
            return null;
        }
    }

    /**
     * Parses text from a string to produce a unit. The default implementation delegates to {@link #parse(CharSequence)}
     * and wraps the {@link ParserException} into a {@link ParseException} for compatibility with {@code java.text} API.
     *
     * @param  source  the text, part of which should be parsed.
     * @return a unit parsed from the string.
     * @throws ParseException if the given string can not be fully parsed.
     */
    @Override
    public Object parseObject(final String source) throws ParseException {
        try {
            return parse(source);
        } catch (ParserException e) {
            throw (ParseException) new ParseException(e.getLocalizedMessage(), e.getPosition()).initCause(e);
        }
    }

    /**
     * Parses text from a string to produce a unit. The default implementation delegates to
     * {@link #parse(CharSequence, ParsePosition)} with no additional work.
     *
     * @param  source  the text, part of which should be parsed.
     * @param  pos     index and error index information as described above.
     * @return a unit parsed from the string, or {@code null} in case of error.
     */
    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        return parse(source, pos);
    }
}
