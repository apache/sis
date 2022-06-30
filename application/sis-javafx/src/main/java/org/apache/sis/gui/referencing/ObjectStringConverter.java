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
package org.apache.sis.gui.referencing;

import java.util.Locale;
import javafx.util.StringConverter;
import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.gui.Resources;


/**
 * Converts an {@link IdentifiedObject} to {@link String} representation to show in JavaFX control.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
final class ObjectStringConverter<T extends IdentifiedObject> extends StringConverter<T> {
    /**
     * The set of items that user can choose.
     */
    private final Iterable<? extends T> items;

    /**
     * The preferred locale for displaying object name, or {@code null} for the default locale.
     */
    private final Locale locale;

    /**
     * The localized "Other…" string.
     */
    private String other;

    /**
     * Creates a new converter.
     *
     * @param  items   the set of items that user can choose.
     * @param  locale  the preferred locale for displaying object name, or {@code null} for the default locale.
     */
    ObjectStringConverter(final Iterable<? extends T> items, final Locale locale) {
        this.items  = items;
        this.locale = locale;
    }

    /**
     * Returns the display name of the given object.
     *
     * @param  object  the object for which to get a string representation.
     * @return the display name of the given object, or {@code null} if none.
     */
    @Override
    public String toString(final T object) {
        if (object != RecentReferenceSystems.OTHER) {
            return IdentifiedObjects.getDisplayName(object, locale);
        } else {
            if (other == null) {
                other = other(locale);
            }
            return other;
        }
    }

    /**
     * Returns the localized "Other…" text to use for selecting a CRS
     * which is not in the short list of proposed CRS.
     */
    static String other(final Locale locale) {
        return Resources.forLocale(locale).getString(Resources.Keys.OtherCRS) + '…';
    }

    /**
     * Returns the object for the given name.
     *
     * @param  name  name of desired object (may be {@code null}).
     * @return the desired object, or {@code null} if not found.
     */
    @Override
    public T fromString(final String name) {
        if (name != null) {
            T fallback = null;
            for (final T item : items) {
                final String candidate = toString(item);
                if (name.equals(candidate)) {
                    return item;
                }
                if (fallback == null && name.equalsIgnoreCase(candidate)) {
                    fallback = item;
                }
            }
            if (fallback != null) {
                return fallback;
            }
            // Check heuristic match only if no exact math was found.
            for (final T item : items) {
                if (IdentifiedObjects.isHeuristicMatchForName(item, name)) {
                    return item;
                }
            }
        }
        return null;
    }
}
