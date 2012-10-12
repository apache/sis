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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.MissingResourceException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.Arrays.resize;
import static org.apache.sis.util.collection.Collections.hashMapCapacity;


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
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
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
        POOL = new HashMap<>(hashMapCapacity(locales.length));
        for (final Locale lc : locales) {
            POOL.put(lc, lc);
        }
        /*
         * Add the static field constants. This operation may replace some values which
         * were returned by Locale.getAvailableLocales(). This is the desired effect,
         * since we want to give precedence to references to the static constants.
         */
        try {
            final Field[] fields = Locale.class.getFields();
            for (int i=0; i<fields.length; i++) {
                final Field field = fields[i];
                if (Modifier.isStatic(field.getModifiers())) {
                    if (Locale.class.isAssignableFrom(field.getType())) {
                        final Locale toAdd = (Locale) field.get(null);
                        POOL.put(toAdd, toAdd);
                    }
                }
            }
        } catch (ReflectiveOperationException exception) {
            /*
             * Not a big deal if this operation fails (this is actually just an
             * optimization for reducing memory usage). Log a warning and stop.
             */
            Logging.unexpectedException(Locales.class, "<clinit>", exception);
        }
    }

    /**
     * All locales available on the JavaVM.
     */
    public static final Locales ALL = new Locales();

    /**
     * Only locales available in the Apache SIS library. They are the locales for which localized
     * resources are provided in the {@link org.apache.sis.util.resources} package.
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
     * localized resources are provided in the {@link org.apache.sis.util.resources} package.
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
     * localized resources are provided in the {@link org.apache.sis.util.resources} package.
     *
     * @return The list of supported locales.
     */
    public Locale[] getAvailableLocales() {
        if (this == ALL) {
            return Locale.getAvailableLocales();
        }
        final Locale[] languages = getAvailableLanguages();
        Locale[] locales = Locale.getAvailableLocales();
        int count = 0;
        for (int i=0; i<locales.length; i++) {
            final Locale locale = locales[i];
            if (containsLanguage(languages, locale)) {
                locales[count++] = unique(locale);
            }
        }
        locales = resize(locales, count);
        return locales;
    }

    /**
     * Returns {@code true} if the specified array of locales contains at least
     * one element with the specified language.
     */
    private static boolean containsLanguage(final Locale[] locales, final Locale language) {
        final String code = language.getLanguage();
        for (int i=0; i<locales.length; i++) {
            if (code.equals(locales[i].getLanguage())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the list of {@linkplain #getAvailableLocales() available locales} formatted
     * as strings in the specified locale.
     *
     * @param  locale The locale to use for formatting the strings to be returned.
     * @return String descriptions of available locales.
     */
    public String[] getAvailableLocales(final Locale locale) {
        final Locale[] locales = getAvailableLocales();
        final String[] display = new String[locales.length];
        for (int i=0; i<locales.length; i++) {
            display[i] = locales[i].getDisplayName(locale);
        }
        Arrays.sort(display);
        return display;
    }

    /**
     * Returns the languages of the given locales, without duplicated values.
     * The instances returned by this method have no {@linkplain Locale#getCountry() country}
     * and no {@linkplain Locale#getVariant() variant} information.
     *
     * @param  locales The locales from which to get the languages.
     * @return The languages, without country or variant information.
     */
    public static Locale[] getLanguages(final Locale... locales) {
        final Set<String> codes = new LinkedHashSet<>(hashMapCapacity(locales.length));
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
     * Returns the {@linkplain Locale#getISO3Language() 3-letters ISO language code} if available,
     * or the {@linkplain Locale#getLanguage() 2-letters code} otherwise.
     *
     * @param  locale The locale for which we want the language.
     * @return The language code, 3 letters if possible or 2 letters otherwise.
     *
     * @see Locale#getISO3Language()
     */
    public static String getLanguageCode(final Locale locale) {
        try {
            return locale.getISO3Language();
        } catch (MissingResourceException e) {
            Logging.recoverableException(Locales.class, "getLanguage", e);
            return locale.getLanguage();
        }
    }

    /**
     * Parses the given locale. The string is the language code either as the 2 letters or the
     * 3 letters ISO code. It can optionally be followed by the {@code '_'} character and the
     * country code (again either as 2 or 3 letters), optionally followed by {@code '_'} and
     * the variant.
     *
     * @param  code The language code, which may be followed by country code.
     * @return The language for the given code.
     * @throws IllegalArgumentException If the given code doesn't seem to be a valid locale.
     */
    public static Locale parse(final String code) throws IllegalArgumentException {
        final String language, country, variant;
        int ci = code.indexOf('_');
        if (ci < 0) {
            language = code.trim();
            country  = "";
            variant  = "";
        } else {
            language = code.substring(0, ci).trim();
            int vi = code.indexOf('_', ++ci);
            if (vi < 0) {
                country = code.substring(ci).trim();
                variant = "";
            } else {
                country = code.substring(ci, vi).trim();
                variant = code.substring(++vi).trim();
                if (code.indexOf('_', vi) >= 0) {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalLanguageCode_1, code));
                }
            }
        }
        final boolean language3 = isThreeLetters(language);
        final boolean country3  = isThreeLetters(country);
        /*
         * Perform a linear scan only if we need to compare some 3-letters ISO code.
         * Otherwise (if every code are 2 letters), it will be faster to create a new
         * locale and check for an existing instance in the hash map.
         */
        if (language3 || country3) {
            String language2 = language;
            String country2  = country;
            for (Locale locale : Locale.getAvailableLocales()) {
                String c = (language3) ? locale.getISO3Language() : locale.getLanguage();
                if (language.equals(c)) {
                    if (country2 == country) { // Really identity comparison.
                        // Remember the 2-letters ISO code in an opportunist way.
                        // If the 2-letters ISO code has been set for the country
                        // as well, we will not change the language code because
                        // it has already been set with the code associated with
                        // the right country.
                        language2 = locale.getLanguage();
                    }
                    c = (country3) ? locale.getISO3Country() : locale.getCountry();
                    if (country.equals(c)) {
                        country2 = locale.getCountry();
                        if (variant.equals(locale.getVariant())) {
                            return unique(locale);
                        }
                    }
                }
            }
            return unique(new Locale(language2, country2, variant));
        }
        return unique(new Locale(language, country, variant));
    }

    /**
     * Returns {@code true} if the following code is 3 letters, or {@code false} if 2 letters.
     */
    private static boolean isThreeLetters(final String code) {
        switch (code.length()) {
            case 0: // fall through
            case 2: return false;
            case 3: return true;
            default: {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalLanguageCode_1, code));
            }
        }
    }

    /**
     * Returns a unique instance of the given locale, if one is available.
     * Otherwise returns the {@code locale} unchanged.
     *
     * @param  locale The locale to canonicalize.
     * @return A unique instance of the given locale, or {@code locale} if
     *         the given locale is not cached.
     */
    public static Locale unique(final Locale locale) {
        final Locale candidate = POOL.get(locale);
        return (candidate != null) ? candidate : locale;
    }
}
