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
package org.apache.sis.internal.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.util.InternationalString;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.util.Static;

import static org.apache.sis.util.CharSequences.equalsFiltered;
import static org.apache.sis.util.CharSequences.trimWhitespaces;
import static org.apache.sis.util.CharSequences.isUnicodeIdentifier;
import static org.apache.sis.util.Characters.Filter.LETTERS_AND_DIGITS;

// Branch-dependent imports
import java.util.Objects;


/**
 * Utility methods working on {@link Citation} objects. The public facade of those methods is
 * defined in the {@link org.apache.sis.metadata.iso.citation.Citations} class, but the actual
 * implementation is defined here since it is needed by some utility methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
public final class Citations extends Static {
    /**
     * Do not allows instantiation of this class.
     */
    private Citations() {
    }

    /**
     * Returns the collection iterator, or {@code null} if the given collection is null
     * or empty. We use this method as a paranoiac safety against broken implementations.
     *
     * @param  <E> The type of elements in the collection.
     * @param  collection The collection from which to get the iterator, or {@code null}.
     * @return The iterator over the given collection elements, or {@code null}.
     */
    public static <E> Iterator<E> iterator(final Collection<E> collection) {
        return (collection != null && !collection.isEmpty()) ? collection.iterator() : null;
    }

    /**
     * Returns a "unlocalized" string representation of the given international string,
     * or {@code null} if none. This method is used by {@link #getIdentifier(Citation, boolean)},
     * which is why we don't want the localized string.
     */
    private static String toString(final InternationalString title) {
        return (title != null) ? trimWhitespaces(title.toString(Locale.ROOT)) : null;
    }

    /**
     * Returns {@code true} if the two citations have at least one title in common,
     * ignoring case and non-alphanumeric characters.
     * See {@link org.apache.sis.metadata.iso.citation.Citations#titleMatches(Citation, Citation)}
     * for the public documentation of this method.
     *
     * @param  c1 The first citation to compare, or {@code null}.
     * @param  c2 the second citation to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and at least one title or alternate title matches.
     */
    public static boolean titleMatches(final Citation c1, final Citation c2) {
        if (c1 != null && c2 != null) {
            if (c1 == c2) {
                return true; // Optimisation for a common case.
            }
            InternationalString candidate = c2.getTitle();
            Iterator<? extends InternationalString> iterator = null;
            do {
                if (candidate != null) {
                    final String unlocalized = candidate.toString(Locale.ROOT);
                    if (titleMatches(c1, unlocalized)) {
                        return true;
                    }
                    final String localized = candidate.toString();
                    if (!Objects.equals(localized, unlocalized) // Slight optimization for a common case.
                            && titleMatches(c1, localized))
                    {
                        return true;
                    }
                }
                if (iterator == null) {
                    iterator = iterator(c2.getAlternateTitles());
                    if (iterator == null) break;
                }
                if (!iterator.hasNext()) break;
                candidate = iterator.next();
            } while (true);
        }
        return false;
    }

    /**
     * Returns {@code true} if the given citation has at least one title equals to the given string,
     * ignoring case and non-alphanumeric characters.
     * See {@link org.apache.sis.metadata.iso.citation.Citations#titleMatches(Citation, String)}
     * for the public documentation of this method.
     *
     * @param  citation The citation to check for, or {@code null}.
     * @param  title The title or alternate title to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and the title or an alternate
     *         title matches the given string.
     */
    public static boolean titleMatches(final Citation citation, final CharSequence title) {
        if (citation != null && title != null) {
            InternationalString candidate = citation.getTitle();
            Iterator<? extends InternationalString> iterator = null;
            do {
                if (candidate != null) {
                    final String unlocalized = candidate.toString(Locale.ROOT);
                    if (equalsFiltered(unlocalized, title, LETTERS_AND_DIGITS, true)) {
                        return true;
                    }
                    final String localized = candidate.toString();
                    if (!Objects.equals(localized, unlocalized) // Slight optimization for a common case.
                            && equalsFiltered(localized, title, LETTERS_AND_DIGITS, true))
                    {
                        return true;
                    }
                }
                if (iterator == null) {
                    iterator = iterator(citation.getAlternateTitles());
                    if (iterator == null) break;
                }
                if (!iterator.hasNext()) break;
                candidate = iterator.next();
            } while (true);
        }
        return false;
    }

    /**
     * Returns {@code true} if the two citations have at least one identifier in common,
     * ignoring case and non-alphanumeric characters. If and <em>only</em> if the citations
     * do not contain any identifier, then this method fallback on titles comparison.
     * See {@link org.apache.sis.metadata.iso.citation.Citations#identifierMatches(Citation, Citation)}
     * for the public documentation of this method.
     *
     * @param  c1 The first citation to compare, or {@code null}.
     * @param  c2 the second citation to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and at least one identifier matches.
     */
    public static boolean identifierMatches(Citation c1, Citation c2) {
        if (c1 != null && c2 != null) {
            if (c1 == c2) {
                return true; // Optimisation for a common case.
            }
            /*
             * If there is no identifier in both citations, fallback on title comparisons.
             * If there is identifiers in only one citation, make sure that this citation
             * is the second one (c2) in order to allow at least one call to
             * 'identifierMatches(c1, String)'.
             */
            Iterator<? extends Identifier> iterator = iterator(c2.getIdentifiers());
            if (iterator == null) {
                iterator = iterator(c1.getIdentifiers());
                if (iterator == null) {
                    return titleMatches(c1, c2);
                }
                c1 = c2;
            }
            do {
                final Identifier id = iterator.next();
                if (id != null && identifierMatches(c1, id, id.getCode())) {
                    return true;
                }
            } while (iterator.hasNext());
        }
        return false;
    }

    /**
     * Returns {@code true} if the given citation has at least one identifier equals to the given string,
     * ignoring case and non-alphanumeric characters. If and <em>only</em> if the citations do not contain
     * any identifier, then this method fallback on titles comparison.
     * See {@link org.apache.sis.metadata.iso.citation.Citations#identifierMatches(Citation, String)}
     * for the public documentation of this method.
     *
     * @param  citation   The citation to check for, or {@code null}.
     * @param  identifier The identifier to compare, or {@code null} to unknown.
     * @param  code       The identifier code to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and an identifier matches the given string.
     */
    public static boolean identifierMatches(final Citation citation, final Identifier identifier, final CharSequence code) {
        if (citation != null && code != null) {
            final Iterator<? extends Identifier> identifiers = iterator(citation.getIdentifiers());
            if (identifiers == null) {
                return titleMatches(citation, code);
            }
            while (identifiers.hasNext()) {
                final Identifier id = identifiers.next();
                if (id != null && equalsFiltered(code, id.getCode(), LETTERS_AND_DIGITS, true)) {
                    if (identifier != null) {
                        final String codeSpace = identifier.getCodeSpace();
                        if (codeSpace != null) {
                            final String cs = id.getCodeSpace();
                            if (cs != null) {
                                return equalsFiltered(codeSpace, cs, LETTERS_AND_DIGITS, true);
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Infers an identifier from the given citation, or returns {@code null} if no identifier has been found.
     * This method removes leading and trailing {@linkplain Character#isWhitespace(int) whitespaces}.
     * See {@link org.apache.sis.metadata.iso.citation.Citations#getIdentifier(Citation)}
     * for the public documentation of this method.
     *
     * <p><b>Which method to use:</b></p>
     * <ul>
     *   <li>For information purpose (e.g. some {@code toString()} methods), use {@code getIdentifier(…, false)}.</li>
     *   <li>For WKT formatting, use {@code getIdentifier(…, true)} in order to preserve formatting characters.</li>
     *   <li>For assigning a value to a {@code codeSpace} field, use {@link #getUnicodeIdentifier(Citation)}.</li>
     * </ul>
     *
     * @param  citation The citation for which to get the identifier, or {@code null}.
     * @param  strict {@code true} for returning a non-null value only if the identifier is a valid Unicode identifier.
     * @return A non-empty identifier for the given citation without leading or trailing whitespaces,
     *         or {@code null} if the given citation is null or does not declare any identifier or title.
     */
    public static String getIdentifier(final Citation citation, final boolean strict) {
        boolean isUnicode = false; // Whether 'identifier' is a Unicode identifier.
        String identifier = null;
        if (citation != null) {
            final Iterator<? extends Identifier> it = iterator(citation.getIdentifiers());
            if (it != null) while (it.hasNext()) {
                final Identifier id = it.next();
                if (id != null) {
                    final String candidate = trimWhitespaces(id.getCode());
                    if (candidate != null) {
                        final int length = candidate.length();
                        if (length != 0 && (identifier == null || length < identifier.length())) {
                            final boolean s = isUnicodeIdentifier(candidate);
                            if (s || !isUnicode) {
                                identifier = candidate;
                                isUnicode = s;
                            }
                        }
                    }
                }
            }
            /*
             * If no identifier has been found, fallback on the shortest title or alternate title.
             * We search for alternate titles because ISO specification said that those titles are
             * often used for abbreviations.
             */
            if (identifier == null) {
                identifier = toString(citation.getTitle()); // Whitepaces removed by toString(…).
                if (identifier != null) {
                    if (identifier.isEmpty()) {
                        identifier = null;
                    } else {
                        isUnicode = isUnicodeIdentifier(identifier);
                    }
                }
                final Iterator<? extends InternationalString> iterator = iterator(citation.getAlternateTitles());
                if (iterator != null) while (iterator.hasNext()) {
                    final String candidate = toString(iterator.next());
                    if (candidate != null) {
                        final int length = candidate.length();
                        if (length != 0 && (identifier == null || length < identifier.length())) {
                            final boolean s = isUnicodeIdentifier(candidate);
                            if (s || !isUnicode) {
                                identifier = candidate;
                                isUnicode = s;
                            }
                        }
                    }
                }
            }
        }
        return (isUnicode || !strict) ? identifier : null;
    }

    /**
     * Infers a valid Unicode identifier from the given citation, or returns {@code null} if none.
     * This method removes {@linkplain Character#isIdentifierIgnorable(int) ignorable characters}.
     * See {@link org.apache.sis.metadata.iso.citation.Citations#getUnicodeIdentifier(Citation)}
     * for the public documentation of this method.
     *
     * <div class="section">When to use</div>
     * Use this method when assigning values to be returned by methods like {@link Identifier#getCodeSpace()},
     * since those values are likely to be compared without special care about ignorable identifier characters.
     * But if the intend is to format a more complex string like WKT or {@code toString()}, then we suggest to
     * use {@code getIdentifier(citation, true)} instead, which will produce the same result but preserving the
     * ignorable characters, which can be useful for formatting purpose.
     *
     * @param  citation The citation for which to get the Unicode identifier, or {@code null}.
     * @return A non-empty Unicode identifier for the given citation without leading or trailing whitespaces,
     *         or {@code null} if the given citation is null or does not have any Unicode identifier or title.
     *
     * @since 0.6
     */
    public static String getUnicodeIdentifier(final Citation citation) {
        if (citation instanceof IdentifierSpace<?>) {
            return ((IdentifierSpace<?>) citation).getName();
        }
        final String identifier = getIdentifier(citation, true);
        if (identifier != null) {
            /*
             * First perform a quick check to see if there is any ignorable characters.
             * We make this check because those characters are valid according Unicode
             * but not according XML. However there is usually no such characters, so
             * we will avoid the StringBuilder creation in the vast majority of times.
             *
             * Note that 'µ' and its friends are not ignorable, so we do not remove them.
             * This method is "getUnicodeIdentifier", not "getXmlIdentifier".
             */
            final int length = identifier.length();
            for (int i=0; i<length;) {
                int c = identifier.codePointAt(i);
                int n = Character.charCount(c);
                if (Character.isIdentifierIgnorable(c)) {
                    /*
                     * Found an ignorable character. Create the buffer and copy non-ignorable characters.
                     * Following algorithm is inefficient, since we fill the buffer character-by-character
                     * (a more efficient approach would be to perform bulk appends). However we presume
                     * that this block will be rarely executed, so it is not worth to optimize it.
                     */
                    final StringBuilder buffer = new StringBuilder(length - n).append(identifier, 0, i);
                    while ((i += n) < length) {
                        c = identifier.codePointAt(i);
                        n = Character.charCount(c);
                        if (!Character.isIdentifierIgnorable(c)) {
                            buffer.appendCodePoint(c);
                        }
                    }
                    // No need to verify if the buffer is empty, because ignorable
                    // characters are not legal Unicode identifier start.
                    return buffer.toString();
                }
                i += n;
            }
        }
        return identifier;
    }
}
