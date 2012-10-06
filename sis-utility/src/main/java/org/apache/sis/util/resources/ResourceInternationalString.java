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
package org.apache.sis.util.resources;

import java.io.Serializable;
import java.util.Locale;
import java.util.MissingResourceException;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.type.AbstractInternationalString;

// Related to JDK7
import java.util.Objects;


/**
 * A copy of {@link org.apache.sis.util.type.ResourceInternationalString} specialized for
 * {@link IndexedResourceBundle}. Compared to the public class, this specialization works
 * with integer resource keys and accepts arguments.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@Immutable
abstract class ResourceInternationalString extends AbstractInternationalString implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4744571031462678126L;

    /**
     * The key for the resource to fetch.
     */
    private final int key;

    /**
     * The argument(s), or {@code Loader.class} if none. The choice of {@code Loader.class} for
     * meaning "no argument" is arbitrary - we just need a reference that the user is unlikely
     * to known, and we need the referenced object to be serializable. We can not use the
     * {@code null} value for "no argument" because the user may really wants to specify
     * {@code null} as an argument value.
     */
    private final Object arguments;

    /**
     * Creates a new international string for the given key.
     *
     * @param key The key for the resource to fetch.
     */
    ResourceInternationalString(final int key) {
        this.key  = key;
        arguments = Loader.class;
    }

    /**
     * Creates a new international string for the given key and arguments.
     *
     * @param key The key for the resource to fetch.
     * @param The argument(s).
     */
    ResourceInternationalString(final int key, final Object arguments) {
        this.key = key;
        this.arguments = arguments;
    }

    /**
     * Returns the resource bundle for the given locale.
     *
     * @param  locale The locale for which to get the resource bundle.
     * @return The resource bundle for the given locale.
     */
    abstract IndexedResourceBundle getBundle(final Locale locale);

    /**
     * Returns a string in the specified locale.
     *
     * @param  locale The locale to look for, or {@code null} for an unlocalized version.
     * @return The string in the specified locale, or in a default locale.
     * @throws MissingResourceException is the key given to the constructor is invalid.
     */
    @Override
    public String toString(Locale locale) throws MissingResourceException {
        if (locale == null) {
            // The English locale (NOT the system default) is often used
            // as the real identifier in OGC IdentifiedObject naming. If
            // a user wants a string in the system default locale, he
            // should invokes the 'toString()' method instead.
            locale = Locale.ENGLISH;
        }
        final IndexedResourceBundle resources = getBundle(locale);
        return (arguments == Loader.class)
                ? resources.getString(key)
                : resources.getString(key, arguments);
    }

    /**
     * Compares this international string with the specified object for equality.
     *
     * @param object The object to compare with this international string.
     * @return {@code true} if the given object is equal to this string.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof ResourceInternationalString) {
            final ResourceInternationalString that = (ResourceInternationalString) object;
            return this.key == that.key && Objects.equals(this.arguments, that.arguments);
        }
        return false;
    }

    /**
     * Returns a hash code value for this international text.
     *
     * @return A hash code value for this international text.
     */
    @Override
    public int hashCode() {
        return key ^ Utilities.deepHashCode(arguments) ^ (int) serialVersionUID;
    }
}
