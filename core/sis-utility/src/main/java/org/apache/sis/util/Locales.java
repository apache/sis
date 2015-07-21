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
package org.apache.sis.util;

import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.Locale;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.MissingResourceException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.Loggers;

import static org.apache.sis.util.CharSequences.trimWhitespaces;
import static org.apache.sis.util.collection.Containers.hashMapCapacity;


/**
 * Static methods working on {@link Locale} instances. While this class is documented as
 * providing static methods, a few methods are actually non-static. Those methods need to be
 * invoked on the {@link #ALL} or {@link #SIS} instance in order to specify the scope.
 * Examples:
 *
 * {@preformat java
 *     Locales[] lc1 = Locales.ALL.getAvailableLanguages();  // All languages installed on the JavaVM.
 *     Locales[] lc2 = Locales.SIS.getAvailableLanguages();  // Only the languages known to Apache SIS.
 * }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final class Locales extends Static {
    /**
     * A read-only map for canonicalizing the locales. Filled on class
     * initialization in order to avoid the need for synchronization.
     */
    private static final Map<Locale,Locale> POOL;
    static {
        final Locale[] locales = Locale.getAvailableLocales();
        POOL = new HashMap<Locale,Locale>(hashMapCapacity(locales.length));
        for (final Locale lc : locales) {
            POOL.put(lc, lc);
        }
        /*
         * Add the static field constants. This operation may replace some values which
         * were returned by Locale.getAvailableLocales(). This is the desired effect,
         * since we want to give precedence to references to the static constants.
         */
        try {
            for (final Field field : Locale.class.getFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    if (Locale.class.isAssignableFrom(field.getType())) {
                        final Locale toAdd = (Locale) field.get(null);
                        POOL.put(toAdd, toAdd);
                    }
                }
            }
        } catch (IllegalAccessException exception) {
            /*
             * Not a big deal if this operation fails (this is actually just an
             * optimization for reducing memory usage). Log a warning and stop.
             */
            Logging.unexpectedException(Logging.getLogger(Loggers.LOCALIZATION), Locales.class, "<clinit>", exception);
        }
    }

    /**
     * Bit mask for differentiating language codes from country codes in the {@link #ISO2} and {@link #ISO3} arrays.
     */
    private static final short LANGUAGE = 0, COUNTRY = Short.MIN_VALUE;

    /**
     * Mapping from 3-letters codes to 2-letters codes. We use {@code short} type instead of {@link String}
     * for compactness (conversions is done by {@link #toNumber(String, short)}) and for avoiding references
     * to {@code String} instances.
     *
     * <div class="note"><b>Implementation note:</b>
     * Oracle JDK8 implementation computes the 3-letters codes on-the-fly instead of holding references
     * to pre-existing strings. If we were holding string references here, we would prevent the garbage
     * collector to collect the strings for all languages and countries. This would probably be a waste
     * of resources.</div>
     */
    private static final short[] ISO3, ISO2;
    static {
        final Short CONFLICT = 0; // Sentinal value for conflicts (paranoiac safety).
        final Map<Short,Short> map = new TreeMap<Short,Short>();
        for (final Locale locale : POOL.values()) {
            short type = LANGUAGE; // 0 for language, or leftmost bit set for country.
            do { // Executed exactly twice: once for language, than once for country.
                final short alpha2 = toNumber((type == LANGUAGE) ? locale.getLanguage() : locale.getCountry(), type);
                if (alpha2 != 0) {
                    final short alpha3;
                    try {
                        alpha3 = toNumber((type == LANGUAGE) ? locale.getISO3Language() : locale.getISO3Country(), type);
                    } catch (MissingResourceException e) {
                        continue; // No 3-letters code to map for this locale.
                    }
                    if (alpha3 != 0 && alpha3 != alpha2) {
                        final Short p = map.put(alpha3, alpha2);
                        if (p != null && p != alpha2) {
                            // We do not expect any conflict. But if it happen anyway, conservatively
                            // remember that we should not perform any substitution for that code.
                            map.put(alpha3, CONFLICT);
                        }
                    }
                }
            } while ((type ^= COUNTRY) != LANGUAGE);
        }
        while (map.values().remove(CONFLICT)); // Remove all conflicts that we may have found.
        ISO3 = new short[map.size()];
        ISO2 = new short[map.size()];
        int i = 0;
        for (final Map.Entry<Short,Short> entry : map.entrySet()) {
            ISO3[i]   = entry.getKey();
            ISO2[i++] = entry.getValue();
        }
    }

    /**
     * All locales available on the JavaVM.
     */
    public static final Locales ALL = new Locales();

    /**
     * Only locales available in the Apache SIS library.
     */
    public static final Locales SIS = new Locales();

    /**
     * Do not allow instantiation of this class,
     * except for the constants defined in this class.
     */
    private Locales() {
    }

    /**
     * Returns the languages known to the JavaVM ({@link #ALL}) or to the Apache SIS library
     * ({@link #SIS}). In the later case, this method returns only the languages for which
     * localized resources are provided in the {@code org.apache.sis.util.resources} package.
     *
     * @return The list of supported languages.
     */
    public Locale[] getAvailableLanguages() {
        if (this == ALL) {
            return getLanguages(Locale.getAvailableLocales());
        }
        return new Locale[] {
            Locale.ENGLISH,
            Locale.FRENCH
        };
    }

    /**
     * Returns the locales known to the JavaVM ({@link #ALL}) or to the Apache SIS library
     * ({@link #SIS}). In the later case, this method returns only the locales for which
     * localized resources are provided in the {@code org.apache.sis.util.resources} package.
     *
     * @return The list of supported locales.
     */
    public Locale[] getAvailableLocales() {
        if (this == ALL) {
            return Locale.getAvailableLocales();
        }
        Locale[] locales = getAvailableLanguages();
        final String[] languages = new String[locales.length];
        for (int i=0; i<languages.length; i++) {
            languages[i] = locales[i].getLanguage();
        }
        int count = 0;
        locales = Locale.getAvailableLocales();
filter: for (final Locale locale : locales) {
            final String code = locale.getLanguage();
            for (final String language : languages) {
                if (code.equals(language)) {
                    locales[count++] = unique(locale);
                    continue filter;
                }
            }
        }
        locales = ArraysExt.resize(locales, count);
        return locales;
    }

    /**
     * Returns the languages of the given locales, without duplicated values.
     * The instances returned by this method have no {@linkplain Locale#getCountry() country}
     * and no {@linkplain Locale#getVariant() variant} information.
     *
     * @param  locales The locales from which to get the languages.
     * @return The languages, without country or variant information.
     */
    private static Locale[] getLanguages(final Locale... locales) {
        final Set<String> codes = new LinkedHashSet<String>(hashMapCapacity(locales.length));
        for (final Locale locale : locales) {
            codes.add(locale.getLanguage());
        }
        int i=0;
        final Locale[] languages = new Locale[codes.size()];
        for (final String code : codes) {
            languages[i++] = unique(new Locale(code));
        }
        return languages;
    }

    /**
     * Parses the given language code, optionally followed by country code and variant. The given string can be either
     * the 2 letters or the 3 letters ISO 639 code. It can optionally be followed by the {@code '_'} character and the
     * country code (again either as 2 or 3 letters), optionally followed by {@code '_'} and the variant.
     *
     * <p>This method can be used when the caller wants the same {@code Locale} constants no matter if the language
     * and country codes use 2 or 3 letters. This method tries to convert 3-letters codes to 2-letters code on a
     * <cite>best effort</cite> basis.</p>
     *
     * @param  code The language code, optionally followed by country code and variant.
     * @return The language for the given code (never {@code null}).
     * @throws RuntimeException If the given code is not valid ({@code IllformedLocaleException} on the JDK7 branch).
     *
     * @see Locale#forLanguageTag(String)
     */
    public static Locale parse(final String code) {
        return parse(code, 0);
    }

    /**
     * Parses the given language code and optional complements (country, variant), starting at the given index.
     * All characters before {@code fromIndex} are ignored. Characters from {@code fromIndex} to the end of the
     * string are parsed as documented in the {@link #parse(String)} method. In particular, this method tries to
     * convert 3-letters codes to 2-letters code on a <cite>best effort</cite> basis.
     *
     * <div class="note"><b>Example:</b>
     * This method is useful when language codes are appended to a base property or resource name.
     * For example a dictionary may define the {@code "remarks"} property by values associated to the
     * {@code "remarks_en"} and {@code "remarks_fr"} keys, for English and French locales respectively.</div>
     *
     * @param  code The language code, which may be followed by country code.
     * @param  fromIndex Index of the first character to parse.
     * @return The language for the given code (never {@code null}).
     * @throws RuntimeException If the given code is not valid ({@code IllformedLocaleException} on the JDK7 branch).
     *
     * @see Locale#forLanguageTag(String)
     * @see org.apache.sis.util.iso.Types#toInternationalString(Map, String)
     */
    public static Locale parse(final String code, final int fromIndex) {
        ArgumentChecks.ensureNonNull("code", code);
        ArgumentChecks.ensurePositive("fromIndex", fromIndex);
        int p1 = code.indexOf('_', fromIndex);
        // JDK7 branch contains a code here with the following comment:
            /*
             * IETF BCP 47 language tag string. This syntax uses the '-' separator instead of '_'.
             * Note that the '_' character is illegal for the language code, but is legal for the
             * variant. Consequently we require the '-' character to appear before the first '_'.
             */
        // End of JDK7-specific.
        /*
         * Old syntax (e.g. "en_US"). Split in (language, country, variant) components,
         * then convert the 3-letters codes to the 2-letters ones.
         */
        String language, country = "", variant = "";
        if (p1 < 0) {
            p1 = code.length();
        } else {
            final int s = p1 + 1;
            int p2 = code.indexOf('_', s);
            if (p2 < 0) {
                p2 = code.length();
            } else {
                variant = (String) trimWhitespaces(code, p2+1, code.length());
            }
            country = (String) trimWhitespaces(code, s, p2);
        }
        language = (String) trimWhitespaces(code, fromIndex, p1);
        language = toISO2(language, LANGUAGE);
        country  = toISO2(country,  COUNTRY);
        if (language.length() > 8 || !isAlphaNumeric(language) ||
             country.length() > 3 || !isAlphaNumeric(country))
        {
            throw new RuntimeException( // IllformedLocaleException (indirectly) on the JDK7 branch.
                    Errors.format(Errors.Keys.IllegalLanguageCode_1, code.substring(fromIndex)));
        }
        return unique(new Locale(language, country, variant));
    }

    /**
     * Returns {@code true} if the given text contains only Latin alphabetic or numeric characters.
     * We use this method for simulating the check performed by {@code Locale.Builder} on JDK7. Our
     * test is not as accurate as the JDK7 one however - we are more permissive. But it is not our
     * intend to reproduce all the JDK7 syntax checks here.
     */
    private static boolean isAlphaNumeric(final String text) {
        for (int i=text.length(); --i>=0;) {
            final char c = text.charAt(i);
            if (!(c >= 'A' && c <= 'Z') && !(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a 3-letters ISO code to a 2-letters one. If the given code is not recognized,
     * then this method returns {@code code} unmodified.
     *
     * @param  code The 3-letters code.
     * @param  type Either {@link #LANGUAGE} or {@link #COUNTRY}.
     * @return The 2-letters code, or {@code null} if none.
     */
    private static String toISO2(final String code, final short type) {
        final short alpha3 = toNumber(code, type);
        if (alpha3 != 0) {
            int alpha2 = Arrays.binarySearch(ISO3, alpha3);
            if (alpha2 >= 0) {
                alpha2 = ISO2[alpha2];
                final int base = (alpha2 & COUNTRY) != 0 ? ('A' - 1) : ('a' - 1);
                alpha2 &= ~COUNTRY;
                int i = 0;
                final char[] c = new char[3]; // 2 should be enough, but our impl. actually allows 3-letters codes too.
                do c[i++] = (char) ((alpha2 & 0x1F) + base);
                while ((alpha2 >>>= 5) != 0);
                return String.valueOf(c, 0, i);
            }
        }
        return code;
    }

    /**
     * Converts the given 1-, 2- or 3- letters alpha code to a 15 bits numbers. Each letter uses 5 bits.
     * If an invalid character is found, then this method returns 0.
     *
     * <p>This method does not use the sign bit. Callers can use it for differentiating language codes
     * from country codes, using the {@link #LANGUAGE} or {@link #COUNTRY} bit masks.</p>
     *
     * @param  code The 1-, 2- or 3- letters alpha code to convert.
     * @param  n Initial bit pattern, either {@link #LANGUAGE} or {@link #COUNTRY}.
     * @return A number for the given code, or 0 if a non alpha characters were found.
     */
    private static short toNumber(final String code, short n) {
        final int length = code.length();
        if (length >= 1 && length <= 3) {
            int shift = 0;
            for (int i=0; i<length; i++) {
                int c = code.charAt(i);
                if (c < 'A' || (c -= (c >= 'a') ? ('a' - 1) : ('A' - 1)) > 26) {
                    return 0;
                }
                n |= c << shift;
                shift += 5;
            }
            return n;
        }
        return 0;
    }

    /**
     * Returns a unique instance of the given locale, if one is available.
     * Otherwise returns the {@code locale} unchanged.
     *
     * @param  locale The locale to canonicalize.
     * @return A unique instance of the given locale, or {@code locale} if the given locale is not cached.
     */
    public static Locale unique(final Locale locale) {
        final Locale candidate = POOL.get(locale);
        return (candidate != null) ? candidate : locale;
    }
}
