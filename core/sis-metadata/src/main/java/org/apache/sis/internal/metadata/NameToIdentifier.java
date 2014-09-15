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
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.iso.DefaultNameSpace;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Does the unobvious mapping between {@link ReferenceIdentifier} properties and {@link GenericName} ones.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 */
public final class NameToIdentifier implements ReferenceIdentifier {
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
     * Infers the authority from the scope.
     *
     * @return The authority, or {@code null} if none.
     */
    @Override
    public Citation getAuthority() {
        final NameSpace scope = name.scope();
        if (scope != null && !scope.isGlobal()) {
            return Citations.fromName(scope.name().tip().toString());
        }
        return null;
    }

    /**
     * Returns the code space of the given name, or its authority if there is no code space.
     *
     * @param  name The name from which to get the code space or authority, or {@code null}.
     * @param  locale The locale, or {@code null} for a call to {@code name.toString()}.
     * @return The code space or authority, or {@code null} if none.
     */
    public static String getCodespaceOrAuthority(final GenericName name, final Locale locale) {
        String codespace = getCodeSpace(name, locale);
        if (codespace == null) {
            final NameSpace scope = name.scope();
            if (scope != null && !scope.isGlobal()) {
                codespace = toString(scope.name().tip(), locale);
            }
        }
        return codespace;
    }

    /**
     * Takes everything except the tip as the code space.
     *
     * @param  name The name from which to get the code space, or {@code null}.
     * @param  locale The locale, or {@code null} for a call to {@code name.toString()}.
     * @return The code space, or {@code null} if none.
     */
    public static String getCodeSpace(final GenericName name, final Locale locale) {
        if (name != null) {
            if (name instanceof ScopedName) {
                return toString(((ScopedName) name).path(), locale);
            }
            if (name.depth() == 2) {
                // May happen on GenericName implementation that do not implement the ScopedName interface.
                // The most importance case is org.apache.sis.referencing.NamedIdentifier.
                return toString(name.head(), locale);
            }
        }
        return null;
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
     * Names are not versioned.
     */
    @Override
    public String getVersion() {
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
     * @param  name   The name from which to get the localized string.
     * @param  locale The locale, or {@code null} for a call to {@code name.toString()}.
     * @return The localized string representation.
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
