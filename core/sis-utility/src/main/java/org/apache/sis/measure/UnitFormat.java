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

import java.util.Locale;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.io.IOException;
import javax.measure.Unit;
import javax.measure.format.ParserException;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.DefinitionURI;
import org.apache.sis.internal.util.XPaths;
import org.apache.sis.internal.jdk8.UncheckedIOException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Localized;
import org.apache.sis.util.resources.Errors;


/**
 * Parses and formats units of measurement as SI symbols, URI in OGC namespace or other symbols.
 * This class combines in a single class the API from {@link java.text} and the API from {@link javax.measure.format}.
 * In addition to the symbols of the <cite>Système international</cite> (SI), this class is also capable to handle
 * some symbols found in <cite>Well Known Text</cite> (WKT) definitions or in XML files.
*
* <div class="section">Parsing authority codes</div>
* As a special case, if a character sequence given to the {@link #parse(CharSequence)} method is of the
* {@code "EPSG:####"} or {@code "urn:ogc:def:uom:EPSG:####"} form (ignoring case and whitespaces),
* then {@code "####"} is parsed as an integer and forwarded to the {@link Units#valueOfEPSG(int)} method.
*
* <div class="section">NetCDF unit symbols</div>
* The attributes in NetCDF files often merge the axis direction with the angular unit,
* as in {@code "degrees_east"} or {@code "degrees_north"}.
* This class ignores those suffixes and unconditionally returns {@link Units#DEGREE} for all axis directions.
* In particular, the units for {@code "degrees_west"} and {@code "degrees_east"} do <strong>not</strong> have
* opposite sign. It is caller responsibility to handle the direction of axes associated to NetCDF units.
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
     * The suffixes that NetCDF files sometime put after the "degrees" unit.
     * Suffix at even index are for axes having the standard geometric direction,
     * while suffix at odd index are for axes having the reverse direction.
     */
    private static final String[] CARDINAL_DIRECTIONS = {"east", "west", "north", "south"};

    /**
     * The default instance used by {@link Units#valueOf(String)} for parsing units of measurement.
     */
    static final UnitFormat INSTANCE = new UnitFormat();

    /**
     * Temporary helper class before we replace by our own implementation.
     */
    private final javax.measure.format.UnitFormat delegate = tec.units.ri.format.SimpleUnitFormat.getInstance();

    /**
     * The locale specified at construction time.
     */
    private Locale locale;

    /**
     * {@code true} for formatting the unit names using US spelling.
     * Example: "meter" instead of "metre".
     */
    private boolean isLocaleUS;

    /**
     * Creates a new format for the given locale.
     *
     * @param   locale  the locale to use for parsing and formatting units.
     * @return  a new {@code UnitFormat} instance using the given locale.
     */
    public static UnitFormat getInstance(final Locale locale) {
        final UnitFormat f = new UnitFormat();
        f.setLocale(locale);
        return f;
    }

    /**
     * Creates a new format initialized to the {@link Locale#ROOT} locale.
     */
    private UnitFormat() {
        locale = Locale.ROOT;
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
     * Sets the locale used by this {@code UnitFormat}.
     *
     * @param locale the new locale for this {@code UnitFormat}.
     */
    public void setLocale(final Locale locale) {
        isLocaleUS = locale.getCountry().equalsIgnoreCase("US");
        this.locale = locale;
    }

    /**
     * Returns {@code true} since this {@code UnitFormat} depends on the {@link Locale}
     * given at construction time for performing its tasks.
     *
     * @return {@code true}.
     */
    @Override
    public boolean isLocaleSensitive() {
        return true;
    }

    /**
     * Attaches a label to the specified unit.
     * If the specified label is already associated to another unit, then this method does nothing.
     *
     * @param  unit   the unit being labeled.
     * @param  label  the new label for the given unit.
     */
    @Override
    public void label(final Unit<?> unit, final String label) {
        delegate.label(unit, label);
    }

    /**
     * Formats the specified unit.
     *
     * @param  unit        the unit to format.
     * @param  toAppendTo  where to format the unit.
     * @return the given {@code toAppendTo} argument, for method calls chaining.
     * @throws IOException if an error occurred while writing to the destination.
     */
    @Override
    public Appendable format(final Unit<?> unit, final Appendable toAppendTo) throws IOException {
        final String symbol = org.apache.sis.internal.util.PatchedUnitFormat.getSymbol(unit);
        if (symbol != null) {
            return toAppendTo.append(symbol);
        }
        /*
         * Following are specific to the WKT format, which is currently the only user of this method.
         * If we invoke this method for other purposes, then we would need to provide more control on
         * what kind of formatting is desired.
         */
        if (Units.ONE.equals(unit)) {
            return toAppendTo.append("unity");
        } else if (Units.DEGREE.equals(unit)) {
            return toAppendTo.append("degree");
        } else if (Units.METRE.equals(unit)) {
            return toAppendTo.append(isLocaleUS ? "meter" : "metre");
        } else if (Units.FOOT_SURVEY_US.equals(unit)) {
            return toAppendTo.append("US survey foot");
        } else if (Units.PPM.equals(unit)) {
            return toAppendTo.append("parts per million");
        }
        return delegate.format(unit, toAppendTo);
    }

    /**
     * Formats the specified unit.
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
            throw new UncheckedIOException(e);      // Should never happen since we are writting to a StringBuffer.
        }
    }

    /**
     * Formats the given unit.
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
     * Parses the given text as an instance of {@code Unit}.
     * If the parse completes without reading the entire length of the text, an exception is thrown.
     *
     * @param  symbols  the unit symbols or URI to parse.
     * @return the unit parsed from the specified symbols.
     * @throws ParserException if a problem occurred while parsing the given symbols.
     *
     * @see Units#valueOf(String)
     */
    @Override
    public Unit<?> parse(final CharSequence symbols) throws ParserException {
        String uom = CharSequences.trimWhitespaces(CharSequences.toASCII(symbols)).toString();
        final int length = uom.length();
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
        if (isURI(uom)) {
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
                uom = code;
            }
        }
        /*
         * Check for degrees units. Note that "deg" could be both angular and Celsius degrees.
         * We try to resolve this ambiguity in the code below by looking for the "Celsius" suffix.
         * Other suffixes commonly found in NetCDF files are "west", "east", "north" or "south".
         * Those suffixes are ignored.
         */
        if (uom.regionMatches(true, 0, "deg", 0, 3)) {
            switch (length) {
                case 3: return Units.DEGREE;                // Exactly "deg"
                case 4: {
                    if (uom.charAt(3) == 'K') {             // Exactly "degK"
                        return Units.KELVIN;
                    }
                    break;
                }
            }
            String prefix = uom;
            boolean isTemperature = false;
            final int s = Math.max(uom.lastIndexOf(' '), uom.lastIndexOf('_'));
            if (s >= 1) {
                final String suffix = (String) CharSequences.trimWhitespaces(uom, s+1, length);
                if (ArraysExt.containsIgnoreCase(CARDINAL_DIRECTIONS, suffix) || (isTemperature = isCelsius(suffix))) {
                    prefix = (String) CharSequences.trimWhitespaces(uom, 0, s);       // Remove the suffix only if we recognized it.
                }
            }
            if (equalsIgnorePlural(prefix, "degree")) {
                return isTemperature ? Units.CELSIUS : Units.DEGREE;
            }
        } else {
            /*
             * Check for unit symbols that do not begin with "deg". If a symbol begins
             * with "deg", then the check should be put in the above block instead.
             */
            if (uom.equals("°")                      || equalsIgnorePlural(uom, "decimal_degree")) return Units.DEGREE;
            if (uom.equalsIgnoreCase("arcsec"))                                                    return Units.ARC_SECOND;
            if (uom.equalsIgnoreCase("rad")          || equalsIgnorePlural(uom, "radian"))         return Units.RADIAN;
            if (equalsIgnorePlural(uom, "meter")     || equalsIgnorePlural(uom, "metre"))          return Units.METRE;
            if (equalsIgnorePlural(uom, "kilometer") || equalsIgnorePlural(uom, "kilometre"))      return Units.KILOMETRE;
            if (equalsIgnorePlural(uom, "week"))        return Units.WEEK;
            if (equalsIgnorePlural(uom, "day"))         return Units.DAY;
            if (equalsIgnorePlural(uom, "hour"))        return Units.HOUR;
            if (equalsIgnorePlural(uom, "minute"))      return Units.MINUTE;
            if (equalsIgnorePlural(uom, "second"))      return Units.SECOND;
            if (equalsIgnorePlural(uom, "grade"))       return Units.GRAD;
            if (equalsIgnorePlural(uom, "grad"))        return Units.GRAD;
            if (isCelsius(uom))                         return Units.CELSIUS;
            if (uom.isEmpty())                          return Units.ONE;
            if (uom.equalsIgnoreCase("US survey foot")) return Units.FOOT_SURVEY_US;
            if (uom.equalsIgnoreCase("ppm"))            return Units.PPM;
            if (uom.equalsIgnoreCase("psu"))            return Units.PSU;
            if (uom.equalsIgnoreCase("sigma"))          return Units.SIGMA;
            if (equalsIgnorePlural(uom, "pixel"))       return Units.PIXEL;
        }
        final Unit<?> unit;
        try {
            unit = delegate.parse(symbols);
        } catch (ParserException e) {
            // Provides a better error message than the default JSR-275 0.9.4 implementation.
            throw Exceptions.setMessage(e, Errors.format(Errors.Keys.IllegalArgumentValue_2, "uom", uom), true);
        }
        /*
         * Special case: JSR-275 version 0.6.1 parses "1/s" and "s-1" as "Baud", which is not what
         * we use in geoscience. Replace "Baud" by "Hertz" if the symbol was not explicitely "Bd".
         */
        if (unit.isCompatible(Units.HERTZ) && !uom.equals("Bd")) {
            return Units.HERTZ;
        }
        return UnitsMap.canonicalize(unit);
    }

    /**
     * Returns {@code true} if the given {@code uom} is equals to the given expected string,
     * ignoring trailing {@code 's'} character (if any).
     */
    @SuppressWarnings("fallthrough")
    private static boolean equalsIgnorePlural(final String uom, final String expected) {
        final int length = expected.length();
        switch (uom.length() - length) {
            case 0:  break;                                                         // uom has exactly the expected length.
            case 1:  if (Character.toLowerCase(uom.charAt(length)) == 's') break;   // else fallthrough.
            default: return false;
        }
        return uom.regionMatches(true, 0, expected, 0, length);
    }

    /**
     * Returns {@code true} if the given {@code uom} is equals to {@code "Celsius"} or {@code "Celcius"}.
     * The later is a common misspelling.
     */
    private static boolean isCelsius(final String uom) {
        return uom.equalsIgnoreCase("Celsius") || uom.equalsIgnoreCase("Celcius");
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
     * Parses the given text as an instance of {@code Unit}.
     *
     * @param  symbols  the unit symbols to parse.
     * @param  pos      on input, index of the first character to parse.
     *                  On output, index after the last parsed character.
     * @return the unit parsed from the specified symbols.
     * @throws ParserException if a problem occurred while parsing the given symbols.
     */
    @Override
    public Object parseObject(final String symbols, final ParsePosition pos) {
        final int start = pos.getIndex();
        int stop = start;
        while (stop < symbols.length()) {
            final int c = symbols.codePointAt(stop);
            if (Character.isWhitespace(c) || c == ']') break;       // Temporary hack before we implement our own parser.
            stop += Character.charCount(c);
        }
        try {
            final Unit<?> unit = parse(symbols.substring(start, stop));
            pos.setIndex(stop);
            return unit;
        } catch (ParserException e) {
            pos.setErrorIndex(start);
            return null;
        }
    }
}
