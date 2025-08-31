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
package org.apache.sis.metadata.privy;

import java.util.Locale;
import java.util.Objects;
import java.util.Collection;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.iso.DefaultNameSpace;
import static org.apache.sis.util.Characters.Filter.LETTERS_AND_DIGITS;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Does the unobvious mapping between {@link Identifier} properties and {@link GenericName} ones.
 * This class also implements the {@link #isHeuristicMatchForName(Identifier, Collection, CharSequence, Simplifier)}
 * method since that method involves a mix of names and identifiers.
 *
 * <h2>Limitations</h2>
 * Current version does not yet work with URN or HTTP syntax.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class NameToIdentifier implements ReferenceIdentifier {
    /**
     * The name from which to infer the identifier attributes.
     */
    private final GenericName name;

    /**
     * Infers the attributes from the given name.
     *
     * @param  name  the name from which to infer the identifier properties.
     */
    public NameToIdentifier(final GenericName name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Returns the scope of the given name if it is not global.
     * This method is null-safe, including paranoiac checks against null scope.
     *
     * @param  name  the name from which to get the scope, or {@code null}.
     * @return the scope of the given name, or {@code null} if the given name was null or has a global scope.
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
     * @return the authority, or {@code null} if none.
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
        return Citations.predefined(scope.head().toString()).orElse(null);
    }

    /**
     * Takes the element before the tip as the code space.
     *
     * @param  name    the name from which to get the code space, or {@code null}.
     * @param  locale  the locale, or {@code null} for a call to {@code name.toString()}.
     * @return the code space, or {@code null} if none.
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
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return ~Objects.hashCode(name);
    }

    /**
     * Compares this object with the given one for equality.
     *
     * @param  object  the object to compare with this identifier.
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
     * @return the string representation of this identifier.
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
     * @param  name    the name from which to get the localized string, or {@code null}.
     * @param  locale  the locale, or {@code null} for a call to {@code name.toString()}.
     * @return the localized string representation, or {@code null} if the given name was null.
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

    /**
     * Returns {@code true} if the given identifier to search matches one of the object identifiers.
     *
     * @param  identifiers  the identifiers to compare against {@code toSearch}.
     * @param  toSearch     the identifier to check for equality.
     * @return {@code true} if the identifier to search is found in the given set of identifiers.
     */
    public static boolean isHeuristicMatchForIdentifier(final Iterable<? extends Identifier> identifiers, final String toSearch) {
        if (toSearch != null && identifiers != null) {
            int s = toSearch.indexOf(DefaultNameSpace.DEFAULT_SEPARATOR);
            if (s < 0) {
                // no codespace in searched name
                for (final Identifier id : identifiers) {
                    if (toSearch.equalsIgnoreCase(id.getCode())) {
                        return true;
                    }
                }
                return false;
            }
            do {
                final String codespace = toSearch.substring(0, s).trim();
                final String code = toSearch.substring(++s).trim();
                for (final Identifier id : identifiers) {
                    if (codespace.equalsIgnoreCase(id.getCodeSpace()) && code.equalsIgnoreCase(id.getCode())) {
                        return true;
                    }
                }
                s = toSearch.indexOf(DefaultNameSpace.DEFAULT_SEPARATOR, s);
            } while (s >= 0);
        }
        return false;
    }

    /**
     * Returns {@code true} if the given {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName()
     * primary name} or one of the given aliases matches the given name. The comparison ignores case, some Latin
     * diacritical signs and any characters that are not letters or digits.
     *
     * @param  name        the name of the {@code IdentifiedObject} to check.
     * @param  aliases     the list of aliases in the {@code IdentifiedObject} (may be {@code null}).
     *                     This method will never modify that list, so the given list can be a direct
     *                     reference to an internal list.
     * @param  toSearch    the name for which to check for equality.
     * @param  simplifier  a function for simplifying the names before comparison.
     * @return {@code true} if the primary name or at least one alias matches the given {@code name}.
     */
    public static boolean isHeuristicMatchForName(final Identifier name, final Collection<GenericName> aliases,
                                                  CharSequence toSearch, final Simplifier simplifier)
    {
        if (toSearch != null) {
            CharSequence code = (name != null) ? name.getCode() : null;
            if (toSearch.equals(code)) {
                return true;                                                    // Optimization for a common case.
            }
            toSearch = simplifier.apply(toSearch);
            code     = simplifier.apply(code);
            if (CharSequences.equalsFiltered(toSearch, code, LETTERS_AND_DIGITS, true)) {
                return true;
            }
            if (aliases != null) {
                for (final GenericName alias : aliases) {
                    if (alias != null) {                                                        // Paranoiac check.
                        final CharSequence tip = simplifier.apply(alias.tip().toString());
                        if (CharSequences.equalsFiltered(toSearch, tip, LETTERS_AND_DIGITS, true)) {
                            return true;
                        }
                        /*
                         * Note: a previous version compared also the scoped names. We removed that part,
                         * because experience has shown that this method is used only for the "code" part
                         * of an object name. If we really want to compare scoped name, it would probably
                         * be better to take a GenericName argument instead of String.
                         */
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName()
     * primary name} or an aliases of the given object matches the given name. The comparison ignores case,
     * some Latin diacritical signs and any characters that are not letters or digits.
     *
     * @param  object  the object for which to check the name or alias.
     * @param  name    the name to compare with the object name or aliases.
     * @return {@code true} if the primary name of at least one alias matches the specified {@code name}.
     */
    public static boolean isHeuristicMatchForName(final IdentifiedObject object, final String name) {
        return isHeuristicMatchForName(object.getName(), object.getAlias(), name, Simplifier.DEFAULT);
    }

    /**
     * A function for simplifying an {@link org.opengis.referencing.IdentifiedObject} name before comparison with
     * {@link NameToIdentifier#isHeuristicMatchForName(Identifier, Collection, CharSequence, Simplifier)}.
     */
    public static class Simplifier {
        /**
         * The prefix used by ESRI at the beginning of datum names.
         * This prefix should be omitted when simplifying a datum name.
         */
        public static final String ESRI_DATUM_PREFIX = "D_";

        /**
         * The default instance, which replaces some non-ASCII characters by ASCII ones.
         */
        public static final Simplifier DEFAULT = new Simplifier();

        /**
         * For subclasses and default instance only.
         */
        protected Simplifier() {
        }

        /**
         * Simplifies the given name.
         *
         * @param  name  the object name (may be {@code null}).
         * @return the name to use for comparison purpose, or {@code null}.
         */
        protected CharSequence apply(final CharSequence name) {
            return CharSequences.toASCII(name);
        }
    }
}
