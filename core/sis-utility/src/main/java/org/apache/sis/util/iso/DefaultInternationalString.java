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
package org.apache.sis.util.iso;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.opengis.util.InternationalString;
import org.apache.sis.util.Locales;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.internal.system.Modules;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * An international string using a map of strings for different locales.
 * Strings for new locales can be {@linkplain #add(Locale, String) added},
 * but existing strings can not be removed or modified.
 * This behavior is a compromise between making constructions easier, and being suitable for
 * use in immutable objects.
 *
 * <div class="section">Thread safety</div>
 * Instances of {@code DefaultInternationalString} are thread-safe. While those instances are not strictly immutable,
 * SIS typically references them as if they were immutable because of their <cite>add-only</cite> behavior.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see Types#toInternationalString(Map, String)
 */
public class DefaultInternationalString extends AbstractInternationalString implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3663160836923279819L;

    /**
     * The string values in different locales (never {@code null}).
     */
    private Map<Locale,String> localeMap;

    /**
     * An unmodifiable view of the entry set in {@link #localeMap}. This is the set of locales
     * defined in this international string. Will be constructed only when first requested.
     */
    private transient Set<Locale> localeSet;

    /**
     * Creates an initially empty international string. Localized strings can been added
     * using one of {@link #add add(…)} methods.
     */
    public DefaultInternationalString() {
        localeMap = Collections.emptyMap();
    }

    /**
     * Creates an international string initialized with the given string.
     * Additional localized strings can been added using one of {@link #add add(…)} methods.
     * The string specified to this constructor is the one that will be returned if no localized
     * string is found for the {@code Locale} argument in a call to {@link #toString(Locale)}.
     *
     * @param string The string in no specific locale, or {@code null} if none.
     */
    public DefaultInternationalString(final String string) {
        if (string != null) {
            localeMap = Collections.singletonMap(Locale.ROOT, string);
        } else {
            localeMap = Collections.emptyMap();
        }
    }

    /**
     * Creates an international string initialized with the given localized strings.
     * The content of the given map is copied, so changes to that map after construction
     * will not be reflected into this international string.
     *
     * @param strings The strings in various locales, or {@code null} if none.
     *
     * @see Types#toInternationalString(Map, String)
     */
    public DefaultInternationalString(final Map<Locale,String> strings) {
        if (Containers.isNullOrEmpty(strings)) {
            localeMap = Collections.emptyMap();
        } else {
            final Iterator<Map.Entry<Locale,String>> it = strings.entrySet().iterator();
            final Map.Entry<Locale,String> entry = it.next();
            if (!it.hasNext()) {
                localeMap = Collections.singletonMap(entry.getKey(), entry.getValue());
            } else {
                localeMap = new LinkedHashMap<Locale,String>(strings);
                // If HashMap is replaced by an other type, please revisit 'getLocales()'.
            }
        }
        final boolean nullMapKey = localeMap.containsKey(null);
        if (nullMapKey || localeMap.containsValue(null)) {
            throw new IllegalArgumentException(Errors.format(nullMapKey
                    ? Errors.Keys.NullMapKey : Errors.Keys.NullMapValue));
        }
    }

    /**
     * Adds a string for the given locale.
     *
     * @param  locale The locale for the {@code string} value.
     * @param  string The localized string.
     * @throws IllegalArgumentException if a different string value was already set for the given locale.
     */
    public synchronized void add(final Locale locale, final String string) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("locale", locale);
        ArgumentChecks.ensureNonNull("string", string);
        switch (localeMap.size()) {
            case 0: {
                localeMap = Collections.singletonMap(locale, string);
                localeSet = null;
                defaultValue = null; // Will be recomputed when first needed.
                return;
            }
            case 1: {
                // If HashMap is replaced by an other type, please revisit 'getLocales()'.
                localeMap = new LinkedHashMap<Locale,String>(localeMap);
                localeSet = null;
                break;
            }
        }
        final String old = localeMap.put(locale, string);
        if (old != null) {
            localeMap.put(locale, old);
            if (string.equals(old)) {
                return;
            }
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.ValueAlreadyDefined_1, locale));
        }
        defaultValue = null; // Will be recomputed when first needed.
    }

    /**
     * Adds the given character sequence. If the given sequence is an other {@link InternationalString} instance,
     * then only the string for the given locale is added. This method is for {@link Types} internal usage only.
     *
     * @param  locale The locale for the {@code string} value.
     * @param  string The character sequence to add.
     * @throws IllegalArgumentException if a different string value was already set for the given locale.
     */
    final void add(final Locale locale, final CharSequence string) throws IllegalArgumentException {
        final boolean i18n = (string instanceof InternationalString);
        add(locale, i18n ? ((InternationalString) string).toString(locale) : string.toString());
        if (i18n && !(string instanceof SimpleInternationalString)) {
            /*
             * If the string may have more than one locale, log a warning telling that some locales
             * may have been ignored. We declare Types.toInternationalString(…) as the source since
             * it is the public facade invoking this method. We declare the source class using only
             * its name rather than Types.class in order to avoid unnecessary real dependency.
             */
            final LogRecord record = Messages.getResources(null).getLogRecord(Level.WARNING, Messages.Keys.LocalesDiscarded);
            record.setSourceClassName("org.apache.sis.util.iso.Types");
            record.setSourceMethodName("toInternationalString");
            record.setLoggerName(Modules.UTILITIES);
            Logging.getLogger(Modules.UTILITIES).log(record);
        }
    }

    /**
     * Returns the set of locales defined in this international string.
     *
     * @return The set of locales.
     *
     * @todo Current implementation does not return a synchronized set. We should synchronize
     *       on the same lock than the one used for accessing the internal locale map.
     */
    public synchronized Set<Locale> getLocales() {
        Set<Locale> locales = localeSet;
        if (locales == null) {
            locales = localeMap.keySet();
            if (localeMap instanceof HashMap<?,?>) {
                locales = Collections.unmodifiableSet(locales);
            }
            localeSet = locales;
        }
        return locales;
    }

    /**
     * Returns a string in the specified locale. If there is no string for that {@code locale},
     * then this method search for a locale without the {@linkplain Locale#getVariant() variant}
     * part. If no string are found, then this method search for a locale without the
     * {@linkplain Locale#getCountry() country} part. If none are found, then this method returns
     * {@code null}.
     *
     * @param  locale The locale to look for, or {@code null}.
     * @return The string in the specified locale, or {@code null} if none was found.
     */
    private String getString(Locale locale) {
        while (locale != null) {
            final String text = localeMap.get(locale);
            if (text != null) {
                return text;
            }
            final String language = locale.getLanguage();
            final String country  = locale.getCountry ();
            final String variant  = locale.getVariant ();
            if (!variant.isEmpty()) {
                locale = new Locale(language, country);
                continue;
            }
            if (!country.isEmpty()) {
                locale = new Locale(language);
                continue;
            }
            break;
        }
        return null;
    }

    /**
     * Returns a string in the specified locale. If there is no string for that {@code locale},
     * then this method searches for a locale without the {@linkplain Locale#getVariant() variant}
     * part. If no string are found, then this method searches for a locale without the
     * {@linkplain Locale#getCountry() country} part. For example if the {@code "fr_CA"} locale
     * was requested but not found, then this method looks for the {@code "fr"} locale.
     * The {@linkplain Locale#ROOT root locale} is tried last.
     *
     * <div class="section">Handling of {@code Locale.ROOT} argument value</div>
     * {@link Locale#ROOT} can be given to this method for requesting a "unlocalized" string,
     * typically some programmatic values like enumerations or identifiers.
     * While identifiers often look like English words, {@code Locale.ROOT} is not considered
     * synonymous to {@link Locale#ENGLISH} because the values may differ in the way numbers and
     * dates are formatted (e.g. using the ISO 8601 standard for dates instead than English conventions).
     * In order to produce a value close to the common practice, this method handles {@code Locale.ROOT}
     * as below:
     *
     * <ul>
     *   <li>If a string has been explicitly {@linkplain #add(Locale, String) added} for
     *       {@code Locale.ROOT}, then that string is returned.</li>
     *   <li>Otherwise, acknowledging that UML identifiers in OGC/ISO specifications are primarily
     *       expressed in the English language, this method looks for strings associated to
     *       {@link Locale#US} as an approximation of "unlocalized" strings.</li>
     *   <li>If no English string was found, then this method looks for a string for the
     *       {@linkplain Locale#getDefault() system default locale}.</li>
     *   <li>If none of the above steps found a string, then this method returns
     *       an arbitrary string.</li>
     * </ul>
     *
     * <div class="section">Handling of {@code null} argument value</div>
     * In the default implementation, the {@code null} locale is handled as a synonymous of
     * {@code Locale.ROOT}. However subclasses are free to use a different fallback. Client
     * code are encouraged to specify only non-null values for more determinist behavior.
     *
     * @param  locale The desired locale for the string to be returned.
     * @return The string in the given locale if available, or in an
     *         implementation-dependent fallback locale otherwise.
     */
    @Override
    public synchronized String toString(final Locale locale) {
        String text = getString(locale);
        if (text == null) {
            /*
             * No localized string for the requested locale.
             * Try fallbacks in the following order:
             *
             *  1) Locale.ROOT
             *  2) Locale.US, as an approximation of "unlocalized" strings.
             *  3) Locale.getDefault()
             *
             * Locale.getDefault() must be last because the i18n string is often constructed with
             * an English sentence for the 'ROOT' locale (the unlocalized text), without explicit
             * entry for the English locale since the 'ROOT' locale is the fallback. If we were
             * looking for the default locale first on a system having French as the default locale,
             * we would get a sentence in French when the user asked for a sentence in English or
             * any language not explicitly declared.
             */
            text = localeMap.get(Locale.ROOT);
            if (text == null) {
                Locale fallback = Locale.US; // The fallback language for "unlocalized" string.
                if (fallback != locale) { // Avoid requesting the same locale twice (optimization).
                    text = getString(fallback);
                    if (text != null) {
                        return text;
                    }
                }
                fallback = Locale.getDefault();
                if (fallback != locale && fallback != Locale.US) {
                    text = getString(fallback);
                    if (text != null) {
                        return text;
                    }
                }
                // Every else failed; pickup a random string.
                // This behavior may change in future versions.
                final Iterator<String> it = localeMap.values().iterator();
                if (it.hasNext()) {
                    text = it.next();
                }
            }
        }
        return text;
    }

    /**
     * Returns {@code true} if all localized texts stored in this international string are
     * contained in the specified object. More specifically:
     *
     * <ul>
     *   <li>If {@code candidate} is an instance of {@link InternationalString}, then this method
     *       returns {@code true} if, for all <var>{@linkplain Locale locale}</var>-<var>{@linkplain
     *       String string}</var> pairs contained in {@code this}, <code>candidate.{@linkplain
     *       InternationalString#toString(Locale) toString}(locale)</code> returns a string
     *       {@linkplain String#equals equals} to {@code string}.</li>
     *
     *   <li>If {@code candidate} is an instance of {@link CharSequence}, then this method
     *       returns {@code true} if {@link #toString(Locale)} returns a string {@linkplain
     *       String#equals equals} to <code>candidate.{@linkplain CharSequence#toString()
     *       toString()}</code> for all locales.</li>
     *
     *   <li>If {@code candidate} is an instance of {@link Map}, then this methods returns
     *       {@code true} if all <var>{@linkplain Locale locale}</var>-<var>{@linkplain String
     *       string}</var> pairs are contained into {@code candidate}.</li>
     *
     *   <li>Otherwise, this method returns {@code false}.</li>
     * </ul>
     *
     * @param  candidate The object which may contains this international string.
     * @return {@code true} if the given object contains all localized strings found in this
     *         international string.
     */
    public synchronized boolean isSubsetOf(final Object candidate) {
        if (candidate instanceof InternationalString) {
            final InternationalString string = (InternationalString) candidate;
            for (final Map.Entry<Locale,String> entry : localeMap.entrySet()) {
                final Locale locale = entry.getKey();
                final String text   = entry.getValue();
                if (!text.equals(string.toString(locale))) {
                    return false;
                }
            }
        } else if (candidate instanceof CharSequence) {
            final String string = candidate.toString();
            for (final String text : localeMap.values()) {
                if (!text.equals(string)) {
                    return false;
                }
            }
        } else if (candidate instanceof Map<?,?>) {
            final Map<?,?> map = (Map<?,?>) candidate;
            return map.entrySet().containsAll(localeMap.entrySet());
        } else {
            return false;
        }
        return true;
    }

    /**
     * Compares this international string with the specified object for equality.
     *
     * @param object The object to compare with this international string.
     * @return {@code true} if the given object is equal to this string.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final DefaultInternationalString that = (DefaultInternationalString) object;
            return Objects.equals(this.localeMap, that.localeMap);
        }
        return false;
    }

    /**
     * Returns a hash code value for this international text.
     *
     * @return A hash code value for this international text.
     */
    @Override
    public synchronized int hashCode() {
        return localeMap.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Canonicalize the locales after deserialization.
     *
     * @param  in The input stream from which to deserialize an international string.
     * @throws IOException If an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException If the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final int size = localeMap.size();
        if (size == 0) {
            return;
        }
        @SuppressWarnings({"unchecked","rawtypes"}) // Generic array creation.
        Map.Entry<Locale,String>[] entries = new Map.Entry[size];
        entries = localeMap.entrySet().toArray(entries);
        if (size == 1) {
            final Map.Entry<Locale,String> entry = entries[0];
            localeMap = Collections.singletonMap(Locales.unique(entry.getKey()), entry.getValue());
        } else {
            localeMap.clear();
            for (final Map.Entry<Locale,String> entry : entries) {
                localeMap.put(Locales.unique(entry.getKey()), entry.getValue());
            }
        }
    }
}
