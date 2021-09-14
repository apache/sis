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
package org.apache.sis.internal.jaxb.lan;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.AbstractSet;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.nio.charset.Charset;


/**
 * Helper methods for handling the ISO 19115 {@code defaultLocale} and {@code otherLocale} legacy properties.
 * The ISO standard defines them as two separated properties while GeoAPI handles them in a single collection
 * for integration with JDK standard API like {@link java.util.Locale#lookup(List, Collection)}.
 *
 * <p>The first element of the {@code languages} collection is taken as the {@code defaultLocale}, and all
 * remaining ones are taken as {@code otherLocale} elements. Instances of this {@code OtherLocales} class
 * are for those remaining elements and are created by the {@link #filter(Map)} method.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.5
 * @module
 */
public final class OtherLocales extends AbstractSet<PT_Locale> {
    /**
     * The default locale followed by all other locales.
     */
    private final Set<PT_Locale> locales;

    /**
     * Private constructor for {@link #filter(Map)} only.
     */
    private OtherLocales(final Set<PT_Locale> locales) {
        this.locales = locales;
    }

    /**
     * Returns a collection for all elements except the first one from the given collection.
     *
     * <h4>Null values and XML marshalling</h4>
     * The {@code locales} argument may be {@code null} at XML marshalling time. In such case, this method returns
     * {@code null} instead of an empty set in order to instruct JAXB to not marshal the {@code otherLocale} element
     * (an empty set would cause JAXB to marshal an empty element). Since the {@code locales} argument given to this
     * method should never be null except at XML marshalling time, this rule should not be a violation of public API.
     *
     * @param  locales  the collection containing the default locale followed by the other ones, or {@code null}.
     * @return a collection containing all {@code languages} elements except the first one, or {@code null}.
     */
    public static Set<PT_Locale> filter(final Map<Locale,Charset> locales) {
        final Set<PT_Locale> s = PT_Locale.wrap(locales);
        return (s != null) ? new OtherLocales(s) : null;
    }

    /**
     * Returns the number of elements in this collection.
     *
     * @return number of other locales.
     */
    @Override
    public int size() {
        int size = locales.size();
        if (size > 0) size--;
        return size;
    }

    /**
     * Returns an iterator over all elements in this collection except the first one.
     *
     * @return iterator over all other locales.
     */
    @Override
    public Iterator<PT_Locale> iterator() {
        final Iterator<PT_Locale> it = locales.iterator();
        if (it.hasNext()) it.next();                            // Skip the first element.
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
     * @param  locale  the element to add.
     * @return {@code true} if the "other locales" collection has been modified as a result of this method call.
     */
    @Override
    public boolean add(final PT_Locale locale) {
        if (locales.isEmpty()) {
            Locale defaultLocale = Locale.getDefault();
            if (defaultLocale.equals(locale.getLocale())) {
                defaultLocale = Locale.ROOT;
            }
            locales.add(new PT_Locale(defaultLocale));
        }
        return locales.add(locale);
    }

    /**
     * Returns a map containing the given {@code PT_Locale} followed by other locales in the given {@code addTo} map.
     *
     * @param  addTo     the map where to set the first locale, or {@code null}.
     * @param  newValue  the value to add in the map, or {@code null}.
     * @return a map containing this locale followed by other locales in the given map.
     */
    public static Map<Locale,Charset> setFirst(Map<Locale,Charset> addTo, final PT_Locale newValue) {
        if (newValue != null) {
            Object[] keys   = null;
            Object[] values = null;
            if (addTo == null) {
                addTo = new LinkedHashMap<>();
            } else if (!addTo.isEmpty()) {
                keys   = addTo.keySet().toArray();
                values = addTo.values().toArray();
                addTo.clear();
                if (newValue.getCharacterSet() == null) {
                    newValue.setCharacterSet((Charset) values[0]);
                }
            }
            newValue.addInto(addTo);
            if (keys != null) {
                for (int i=1; i<keys.length; i++) {                         // Skip first element.
                    addTo.put((Locale) keys[i], (Charset) values[i]);
                }
            }
        }
        return addTo;
    }
}
