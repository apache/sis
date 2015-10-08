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
package org.apache.sis.internal.metadata;

import java.util.Locale;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.AbstractCollection;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Containers;


/**
 * Helper methods for handling the ISO 19115 {@code defaultLocale} and {@code otherLocale} legacy properties.
 * The ISO standard defines them as two separated properties while GeoAPI handles them in a single collection
 * for integration with JDK standard API like {@link Locale#lookup(List, Collection)}.
 *
 * <p>The first element of the {@code languages} collection is taken as the {@code defaultLocale}, and all
 * remaining ones are taken as {@code otherLocale} elements. Instances of this {@code OtherLocales} class
 * are for those remaining elements and are created by the {@link #filter(Collection)} method.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class OtherLocales extends AbstractCollection<Locale> {
    /**
     * The default locale followed by all other locales.
     */
    private final Collection<Locale> languages;

    /**
     * Private constructor for {@link #filter(Collection)} only.
     */
    private OtherLocales(final Collection<Locale> languages) {
        this.languages = languages;
    }

    /**
     * Returns a collection for all elements except the first one from the given collection.
     *
     * <p><b>Null values and XML marshalling:</b>
     * The {@code languages} argument may be {@code null} at XML marshalling time. In such case, this method returns
     * {@code null} instead than an empty set in order to instruct JAXB to not marshal the {@code otherLocale} element
     * (an empty set would cause JAXB to marshal an empty element). Since the {@code languages} argument given to this
     * method should never be null except at XML marshalling time, this rule should not be a violation of public API.</p>
     *
     * <p>The converse of this {@code filter} method is {@link #merge(Locale, Collection)}.</p>
     *
     * @param  languages The collection containing the default locale followed by the other ones.
     * @return A collection containing all {@code languages} elements except the first one.
     */
    public static Collection<Locale> filter(final Collection<Locale> languages) {
        return (languages != null) ? new OtherLocales(languages) : null;
    }

    /**
     * Returns the number of elements in this collection.
     *
     * @return Number of other locales.
     */
    @Override
    public int size() {
        int size = languages.size();
        if (size != 0) size--;
        return size;
    }

    /**
     * Returns an iterator over all elements in this collection except the first one.
     *
     * @return Iterator over all other locales.
     */
    @Override
    public Iterator<Locale> iterator() {
        final Iterator<Locale> it = languages.iterator();
        if (it.hasNext()) it.next(); // Skip the first element.
        return it;
    }

    /**
     * Adds a new element to the collection of "other locales". If we had no "default locale" prior this method call,
     * then this method will choose one before to add the given locale. This is needed since the other locales begin
     * only after the first element, so a first element needs to exist.
     *
     * <p>The above rule could be a risk of confusion for the users, since it could cause the apparition of a default
     * locale which has never been specified. However this risk exists only when invoking the deprecated methods, or
     * when unmarshalling a XML document having a {@code otherLocale} property without {@code defaultLocale} property,
     * which is probably invalid.</p>
     *
     * @param  locale The element to add.
     * @return {@code true} if the "other locales" collection has been modified as a result of this method call.
     */
    @Override
    public boolean add(final Locale locale) {
        ArgumentChecks.ensureNonNull("locale", locale);
        if (languages.isEmpty()) {
            Locale defaultLocale = Locale.getDefault();
            if (defaultLocale.equals(locale)) {
                defaultLocale = Locale.ROOT;  // Same default than merge(Locale, Collection).
            }
            languages.add(defaultLocale);
        }
        return languages.add(locale);
    }

    /**
     * Returns a collection containing the given {@code defaultLocale} followed by the {@code otherLocales}.
     *
     * @param  defaultLocale The first element in the collection to be returned, or {@code null} if unspecified.
     * @param  otherLocales  All remaining elements in the collection to be returned, or {@code null} if none.
     * @return A collection containing the default locale followed by all other ones.
     */
    public static Collection<Locale> merge(Locale defaultLocale, final Collection<? extends Locale> otherLocales) {
        final Collection<Locale> merged;
        if (Containers.isNullOrEmpty(otherLocales)) {
            merged = LegacyPropertyAdapter.asCollection(defaultLocale);
        } else {
            merged = new ArrayList<Locale>(otherLocales.size() + 1);
            if (defaultLocale == null) {
                defaultLocale = Locale.getDefault();
                if (otherLocales.contains(defaultLocale)) {
                    defaultLocale = Locale.ROOT;  // Same default than add(Locale).
                }
            }
            merged.add(defaultLocale);
            merged.addAll(otherLocales);
        }
        return merged;
    }

    /**
     * Sets the first element in the given collection to the given value.
     * Special cases:
     *
     * <ul>
     *   <li>If the given collection is null, a new collection will be returned.</li>
     *   <li>If the given new value  is null, then the first element in the collection is removed.</li>
     *   <li>Otherwise if the given collection is empty, the given value will be added to it.</li>
     * </ul>
     *
     * <p><b>Note:</b> while defined in {@code OtherLocales} because the primary use for this method is to
     * get the default locale, this method is also opportunistically used for other legacy properties.</p>
     *
     * @param  <T>      The type of elements in the collection.
     * @param  values   The collection where to add the new value, or {@code null}.
     * @param  newValue The new value to set, or {@code null} for instead removing the first element.
     * @return The collection (may or may not be the given {@code values} collection).
     *
     * @see org.apache.sis.internal.util.CollectionsExt#first(Iterable)
     */
    public static <T> Collection<T> setFirst(Collection<T> values, final T newValue) {
        if (values == null) {
            return LegacyPropertyAdapter.asCollection(newValue);
        }
        if (newValue == null) {
            final Iterator<T> it = values.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        } else if (values.isEmpty()) {
            values.add(newValue);
        } else {
            if (!(values instanceof List<?>)) {
                values = new ArrayList<T>(values);
            }
            ((List<T>) values).set(0, newValue);
        }
        return values;
    }
}
