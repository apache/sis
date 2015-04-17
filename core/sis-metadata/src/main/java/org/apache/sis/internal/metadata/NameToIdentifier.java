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
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.iso.DefaultNameSpace;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import java.util.Objects;


/**
 * Does the unobvious mapping between {@link Identifier} properties and {@link GenericName} ones.
 *
 * <p><b>Limitation:</b>
 * Current version does not yet work with URN or HTTP syntax.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
public final class NameToIdentifier implements Identifier {
    /**
     * The name from which to infer the identifier attributes.
     */
    private final GenericName name;

    /**
     * Infers the attributes from the given name.
     *
     * @param name The name from which to infer the identifier properties.
     */
    public NameToIdentifier(final GenericName name) {
        ensureNonNull("name", name);
        this.name = name;
    }

    /**
     * Returns the scope of the given name if it is not global.
     * This method is null-safe, including paranoiac checks against null scope.
     *
     * @param  name The name from which to get the scope, or {@code null}.
     * @return The scope of the given name, or {@code null} if the given name was null or has a global scope.
     */
    private static GenericName scope(final GenericName name) {
        if (name != null) {
            final NameSpace scope = name.scope();
            if (scope != null && !scope.isGlobal()) {
                return scope.name();
            }
        }
        return null;
    }

    /**
     * Infers the authority from the scope if any, or from the code space otherwise.
     *
     * @return The authority, or {@code null} if none.
     */
    @Override
    public Citation getAuthority() {
        GenericName scope = scope(name);
        if (scope == null) {
            scope = scope(name.tip());
            if (scope == null) {
                return null;
            }
        }
        return Citations.fromName(scope.head().toString());
    }

    /**
     * Takes the element before the tip as the code space.
     *
     * @param  name The name from which to get the code space, or {@code null}.
     * @param  locale The locale, or {@code null} for a call to {@code name.toString()}.
     * @return The code space, or {@code null} if none.
     */
    public static String getCodeSpace(final GenericName name, final Locale locale) {
        final GenericName scope = scope(name.tip());
        return (scope != null) ? toString(scope.tip(), locale) : null;
    }

    /**
     * Takes everything except the tip as the code space.
     */
    @Override
    public String getCodeSpace() {
        return getCodeSpace(name, null);
    }

    /**
     * Takes the last element as the code.
     */
    @Override
    public String getCode() {
        return name.tip().toString();
    }

    /**
     * Returns {@code null} since names are not versioned.
     */
    @Override
    public String getVersion() {
        return null;
    }

    /**
     * Returns {@code null} since we do not provide natural language description.
     *
     * @since 0.5
     */
    @Override
    public InternationalString getDescription() {
        return null;
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return ~Objects.hashCode(name);
    }

    /**
     * Compares this object with the given one for equality.
     *
     * @param object The object to compare with this identifier.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            return Objects.equals(name, ((NameToIdentifier) object).name);
        }
        return false;
    }

    /**
     * Returns the string representation of this identifier.
     *
     * @return The string representation of this identifier.
     */
    @Override
    public String toString() {
        final String code = getCode();
        final String cs = getCodeSpace();
        if (cs != null && !cs.isEmpty()) {
            return cs + DefaultNameSpace.DEFAULT_SEPARATOR + code;
        }
        return code;
    }

    /**
     * Returns a string representation of the given name in the given locale, with paranoiac checks against null value.
     * Such null values should never happen since the properties used here are mandatory, but we try to make this class
     * robust to broken implementations.
     *
     * @param  name   The name from which to get the localized string, or {@code null}.
     * @param  locale The locale, or {@code null} for a call to {@code name.toString()}.
     * @return The localized string representation, or {@code null} if the given name was null.
     */
    public static String toString(final GenericName name, final Locale locale) {
        if (name != null) {
            if (locale != null) {
                final InternationalString i18n = name.toInternationalString();
                if (i18n != null) {
                    final String s = i18n.toString(locale);
                    if (s != null) {
                        return s;
                    }
                }
            }
            return name.toString();
        }
        return null;
    }
}
