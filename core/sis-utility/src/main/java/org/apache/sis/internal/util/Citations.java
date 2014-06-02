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
import org.apache.sis.util.Static;

import static org.apache.sis.util.CharSequences.equalsFiltered;
import static org.apache.sis.util.CharSequences.trimWhitespaces;
import static org.apache.sis.util.CharSequences.isUnicodeIdentifier;
import static org.apache.sis.util.Characters.Filter.LETTERS_AND_DIGITS;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Utility methods working on {@link Citation} objects. The public facade of those methods is
 * defined in the {@link org.apache.sis.metadata.iso.citation.Citations} class, but the actual
 * implementation is defined here since it is needed by some utility methods.
 *
 * {@section Argument checks}
 * Every methods in this class accept {@code null} argument. This is different from the methods
 * in the {@link org.apache.sis.metadata.iso.citation.Citations} facade, which perform checks
 * against null argument for trapping user errors.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.2)
 * @version 0.3
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
     * or {@code null} if none. This method is used by {@link #getIdentifier(Citation)},
     * which is why we don't want the localized string.
     */
    private static String toString(final InternationalString title) {
        return (title != null) ? trimWhitespaces(title.toString(Locale.ROOT)) : null;
    }

    /**
     * Returns {@code true} if at least one {@linkplain Citation#getTitle() title} or
     * {@linkplain Citation#getAlternateTitles() alternate title} in {@code c1} is leniently
     * equal to a title or alternate title in {@code c2}. The comparison is case-insensitive
     * and ignores every character which is not a {@linkplain Character#isLetterOrDigit(int)
     * letter or a digit}. The titles ordering is not significant.
     *
     * @param  c1 The first citation to compare, or {@code null}.
     * @param  c2 the second citation to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and at least one title or
     *         alternate title matches.
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
     * Returns {@code true} if the {@linkplain Citation#getTitle() title} or any
     * {@linkplain Citation#getAlternateTitles() alternate title} in the given citation
     * matches the given string. The comparison is case-insensitive and ignores every character
     * which is not a {@linkplain Character#isLetterOrDigit(int) letter or a digit}.
     *
     * @param  citation The citation to check for, or {@code null}.
     * @param  title The title or alternate title to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and the title or alternate
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
     * Returns {@code true} if at least one {@linkplain Citation#getIdentifiers() identifier} in
     * {@code c1} is equal to an identifier in {@code c2}. The comparison is case-insensitive
     * and ignores every character which is not a {@linkplain Character#isLetterOrDigit(int)
     * letter or a digit}. The identifier ordering is not significant.
     *
     * <p>If (and <em>only</em> if) the citations do not contains any identifier, then this method
     * fallback on titles comparison using the {@link #titleMatches(Citation,Citation) titleMatches}
     * method. This fallback exists for compatibility with client codes using the citation
     * {@linkplain Citation#getTitle() titles} without identifiers.</p>
     *
     * @param  c1 The first citation to compare, or {@code null}.
     * @param  c2 the second citation to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and at least one identifier,
     *         title or alternate title matches.
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
                if (id != null && identifierMatches(c1, id.getCode())) {
                    return true;
                }
            } while (iterator.hasNext());
        }
        return false;
    }

    /**
     * Returns {@code true} if any {@linkplain Citation#getIdentifiers() identifiers} in the given
     * citation matches the given string. The comparison is case-insensitive and ignores every
     * character which is not a {@linkplain Character#isLetterOrDigit(int) letter or a digit}.
     *
     * <p>If (and <em>only</em> if) the citation does not contain any identifier, then this method
     * fallback on titles comparison using the {@link #titleMatches(Citation,String) titleMatches}
     * method. This fallback exists for compatibility with client codes using citation
     * {@linkplain Citation#getTitle() titles} without identifiers.</p>
     *
     * @param  citation The citation to check for, or {@code null}.
     * @param  identifier The identifier to compare, or {@code null}.
     * @return {@code true} if both arguments are non-null, and the title or alternate title
     *         matches the given string.
     */
    public static boolean identifierMatches(final Citation citation, final CharSequence identifier) {
        if (citation != null && identifier != null) {
            final Iterator<? extends Identifier> identifiers = iterator(citation.getIdentifiers());
            if (identifiers == null) {
                return titleMatches(citation, identifier);
            }
            while (identifiers.hasNext()) {
                final Identifier id = identifiers.next();
                if (id != null && equalsFiltered(identifier, id.getCode(), LETTERS_AND_DIGITS, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Infers an identifier from the given citation, or returns {@code null} if no identifier has been found.
     * This method is useful for extracting the namespace from an authority, for example {@code "EPSG"}.
     * The implementation performs the following choices:
     *
     * <ul>
     *   <li>If the given citation is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the citation contains at least one {@linkplain Citation#getIdentifiers() identifier}, then:
     *     <ul>
     *       <li>If at least one identifier is a {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier
     *           unicode identifier}, then the shortest of those identifiers is returned.</li>
     *       <li>Otherwise the shortest identifier is returned, despite not being a Unicode identifier.</li>
     *     </ul></li>
     *   <li>Otherwise if the citation contains at least one {@linkplain Citation#getTitle() title} or
     *       {@linkplain Citation#getAlternateTitles() alternate title}, then:
     *     <ul>
     *       <li>If at least one title is a {@linkplain org.apache.sis.util.CharSequences#isUnicodeIdentifier
     *           unicode identifier}, then the shortest of those titles is returned.</li>
     *       <li>Otherwise the shortest title is returned, despite not being a Unicode identifier.</li>
     *     </ul></li>
     *   <li>Otherwise this method returns {@code null}.</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * This method searches in alternate titles as a fallback because ISO specification said
     * that those titles are often used for abbreviations.</div>
     *
     * This method ignores leading and trailing whitespaces of every character sequences.
     * Null references, empty character sequences and sequences of whitespaces only are ignored.
     *
     * @param  citation The citation for which to get the identifier, or {@code null}.
     * @return A non-empty identifier for the given citation without leading or trailing whitespaces,
     *         or {@code null} if the given citation is null or does not declare any identifier or title.
     */
    public static String getIdentifier(final Citation citation) {
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
        return identifier;
    }
}
