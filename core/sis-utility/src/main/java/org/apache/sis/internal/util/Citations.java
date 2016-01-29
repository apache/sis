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
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.Static;

import static org.apache.sis.util.iso.DefaultNameSpace.DEFAULT_SEPARATOR;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Utility methods working on {@link Citation} objects. The public facade of those methods is
 * defined in the {@link org.apache.sis.metadata.iso.citation.Citations} class, but the actual
 * implementation is defined here since it is needed by some utility methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public final class Citations extends Static {
    /**
     * Do not allows instantiation of this class.
     */
    private Citations() {
    }

    /**
     * Returns {@code true} if the given code is {@code "EPSG"} while the codespace is {@code "IOGP"} or {@code "OGP"}
     * (ignoring case). This particular combination of code and codespace is handled in a special way.
     *
     * <p>This method can be used for identifying where in Apache SIS source code the relationship between
     * EPSG authority and IOGP code space is hard-coded.</p>
     *
     * @param  codeSpace The identifier code space, or {@code null}.
     * @param  code The identifier code, or {@code null}.
     * @return {@code true} if the given identifier is {@code "IOGP:EPSG"}.
     *
     * @see org.apache.sis.metadata.iso.citation.Citations#EPSG
     */
    public static boolean isEPSG(final String codeSpace, final String code) {
        return Constants.EPSG.equalsIgnoreCase(code) &&
              (Constants.IOGP.equalsIgnoreCase(codeSpace) || "OGP".equalsIgnoreCase(codeSpace) ||
               Constants.EPSG.equalsIgnoreCase(codeSpace));
        // "OGP" is a legacy abbreviation that existed before "IOGP".
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
     * Return {@code true} if the given object is deprecated.
     */
    private static boolean isDeprecated(final Object object) {
        return (object instanceof Deprecable) && ((Deprecable) object).isDeprecated();
    }

    /**
     * Returns a "unlocalized" string representation of the given international string, or {@code null} if none
     * or if the string is deprecated. This method is used by {@link #getIdentifier(Citation, boolean)}, which
     * is why we don't want the localized string.
     */
    private static String toString(final InternationalString title) {
        return (title != null && !isDeprecated(title))
               ? CharSequences.trimWhitespaces(title.toString(Locale.ROOT)) : null;
    }

    /**
     * The method to be used consistently for comparing titles or identifiers in all {@code fooMathes(…)}
     * methods declared in this class.
     *
     * @param  s1 The first characters sequence to compare, or {@code null}.
     * @param  s2 The second characters sequence to compare, or {@code null}.
     * @return {@code true} if both arguments are {@code null} or if the two given texts are equal,
     *         ignoring case and any characters other than digits and letters.
     *
     * @since 0.6
     */
    public static boolean equalsFiltered(final CharSequence s1, final CharSequence s2) {
        return CharSequences.equalsFiltered(s1, s2, Characters.Filter.LETTERS_AND_DIGITS, true);
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
                    if (!Objects.equals(localized, unlocalized)             // Slight optimization for a common case.
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
                    if (equalsFiltered(unlocalized, title)) {
                        return true;
                    }
                    final String localized = candidate.toString();
                    if (!Objects.equals(localized, unlocalized)             // Slight optimization for a common case.
                            && equalsFiltered(localized, title))
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
                return true;                            // Optimisation for a common case.
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
     * ignoring case and non-alphanumeric characters. If and <em>only</em> if the citation does not contain
     * any identifier, then this method fallback on titles comparison.
     * See {@link org.apache.sis.metadata.iso.citation.Citations#identifierMatches(Citation, String)}
     * for the public documentation of this method.
     *
     * @param  citation   The citation to check for, or {@code null}.
     * @param  identifier The identifier to compare, or {@code null} if unknown.
     * @param  code       Value of {@code identifier.getCode()}, or {@code null}.
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
                if (id != null && equalsFiltered(code, id.getCode())) {
                    if (identifier instanceof ReferenceIdentifier) {
                        final String codeSpace = ((ReferenceIdentifier) identifier).getCodeSpace();
                        if (codeSpace != null && id instanceof ReferenceIdentifier) {
                            final String cs = ((ReferenceIdentifier) id).getCodeSpace();
                            if (cs != null) {
                                return equalsFiltered(codeSpace, cs);
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
     * Returns {@code true} if the given identifier authority matches the given {@code authority}.
     * If one of the authority is null, then the comparison fallback on the given {@code codeSpace}.
     * If the code spaces are also null, then this method conservatively returns {@code false}.
     *
     * @param  identifier The identifier to compare.
     * @param  authority  The desired authority, or {@code null}.
     * @param  codeSpace  The desired code space or {@code null}, used as a fallback if an authority is null.
     * @return {@code true} if the authority or code space (as a fallback only) matches.
     */
    private static boolean authorityMatches(final Identifier identifier, final Citation authority, final String codeSpace) {
        if (authority != null) {
            final Citation other = identifier.getAuthority();
            if (other != null) {
                return identifierMatches(authority, other);
            }
        }
        if (codeSpace != null && identifier instanceof ReferenceIdentifier) {
            final String other = ((ReferenceIdentifier) identifier).getCodeSpace();
            if (other != null) {
                return CharSequences.equalsFiltered(codeSpace, other, Characters.Filter.UNICODE_IDENTIFIER, true);
            }
        }
        return false;
    }

    /**
     * Determines whether a match or mismatch is found between the two given collections of identifiers.
     * If any of the given collections is {@code null} or empty, then this method returns {@code null}.
     *
     * <p>According ISO 19162 (<cite>Well known text representation of coordinate reference systems</cite>),
     * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getIdentifiers() identifiers} should have precedence over
     * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName() name} for identifying {@code IdentifiedObject}s,
     * at least in the case of {@linkplain org.apache.sis.referencing.operation.DefaultOperationMethod operation methods} and
     * {@linkplain org.apache.sis.parameter.AbstractParameterDescriptor parameters}.</p>
     *
     * @param  id1 The first collection of identifiers, or {@code null}.
     * @param  id2 The second collection of identifiers, or {@code null}.
     * @return {@code TRUE} or {@code FALSE} on match or mismatch respectively, or {@code null} if this method
     *         can not determine if there is a match or mismatch.
     */
    public static Boolean hasCommonIdentifier(final Iterable<? extends Identifier> id1,
                                              final Iterable<? extends Identifier> id2)
    {
        if (id1 != null && id2 != null) {
            boolean hasFound = false;
            for (final Identifier identifier : id1) {
                final Citation authority = identifier.getAuthority();
                final String codeSpace = (identifier instanceof ReferenceIdentifier) ? ((ReferenceIdentifier) identifier).getCodeSpace() : null;
                for (final Identifier other : id2) {
                    if (authorityMatches(identifier, authority, codeSpace)) {
                        if (CharSequences.equalsFiltered(identifier.getCode(), other.getCode(), Characters.Filter.UNICODE_IDENTIFIER, true)) {
                            return Boolean.TRUE;
                        }
                        hasFound = true;
                    }
                }
            }
            if (hasFound) {
                return Boolean.FALSE;
            }
        }
        return null;
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
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-201">SIS-201</a>
     */
    public static String getIdentifier(final Citation citation, final boolean strict) {
        if (citation != null) {
            boolean isUnicode = false;      // Whether 'identifier' is a Unicode identifier.
            String identifier = null;       // The best identifier found so far.
            String codeSpace  = null;       // Code space of the identifier, or null if none.
            final Iterator<? extends Identifier> it = iterator(citation.getIdentifiers());
            if (it != null) while (it.hasNext()) {
                final Identifier id = it.next();
                if (id != null && !isDeprecated(id)) {
                    final String candidate = CharSequences.trimWhitespaces(id.getCode());
                    if (candidate != null && !candidate.isEmpty()) {
                        /*
                         * For a non-empty identifier, verify if both the code and its codespace are valid
                         * Unicode identifiers. If a codespace exists, then the code does not need to begin
                         * with a "Unicode identifier start" (it may be a "Unicode identifier part").
                         */
                        String cs = (id instanceof ReferenceIdentifier)
                                    ? CharSequences.trimWhitespaces(((ReferenceIdentifier) id).getCodeSpace()) : null;
                        if (cs == null || cs.isEmpty()) {
                            cs = null;
                            isUnicode = CharSequences.isUnicodeIdentifier(candidate);
                        } else {
                            isUnicode = CharSequences.isUnicodeIdentifier(cs);
                            if (isUnicode) for (int i = 0; i < candidate.length();) {
                                final int c = candidate.codePointAt(i);
                                if (!Character.isUnicodeIdentifierPart(c) &&
                                        (strict || (c != '.' && c != '-')))
                                {
                                    // Above special case for '.' and '-' characters is documented
                                    // in the public Citations.getIdentifier(Citation) method.
                                    isUnicode = false;
                                    break;
                                }
                                i += Character.charCount(c);
                            }
                        }
                        /*
                         * If we found a Unicode identifier, we are done and we can exit the loop.
                         * Otherwise retain the first identifier and continue the search for Unicode identifier.
                         */
                        if (identifier == null || isUnicode) {
                            identifier = candidate;
                            codeSpace  = cs;
                            if (isUnicode) break;
                        }
                    }
                }
            }
            /*
             * If no identifier has been found, fallback on the first title or alternate title.
             * We search for alternate titles because ISO specification said that those titles
             * are often used for abbreviations. Again we give preference to Unicode identifiers,
             * which are typically alternate titles.
             */
            if (identifier == null) {
                identifier = toString(citation.getTitle());     // Whitepaces removed by toString(…).
                if (identifier != null) {
                    if (identifier.isEmpty()) {
                        identifier = null;
                    } else {
                        isUnicode = CharSequences.isUnicodeIdentifier(identifier);
                    }
                }
                if (!isUnicode) {
                    final Iterator<? extends InternationalString> iterator = iterator(citation.getAlternateTitles());
                    if (iterator != null) while (iterator.hasNext()) {
                        final String candidate = toString(iterator.next());
                        if (candidate != null && !candidate.isEmpty()) {
                            isUnicode = CharSequences.isUnicodeIdentifier(candidate);
                            if (identifier == null || isUnicode) {
                                identifier = candidate;
                                if (isUnicode) break;
                            }
                        }
                    }
                }
            }
            /*
             * Finished searching in the identifiers, title and alternate titles. If the identifier that
             * we found is not a valid Unicode identifier, we will return it only if the caller did not
             * asked for strictly valid Unicode identifier.
             */
            if (isUnicode || !strict) {
                if (codeSpace != null && !isEPSG(codeSpace, identifier)) {
                    return codeSpace + (strict ? '_' : DEFAULT_SEPARATOR) + identifier;
                } else {
                    return identifier;
                }
            }
        }
        return null;
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

    /**
     * Infers a code space from the given citation, or returns {@code null} if none.
     * This method is very close to {@link #getUnicodeIdentifier(Citation)}, except that it looks for
     * {@link IdentifierSpace#getName()} before to scan the identifiers and titles. The result should
     * be the same in most cases, except some cases like the {@link org.apache.sis.metadata.iso.citation.Citations}
     * constant for {@code "Proj.4"} in which case this method returns {@code "Proj4"} instead of {@code null}.
     * As a side effect, using this method also avoid constructing {@code DefaultCitation} objects which were deferred.
     *
     * <p>We do not put this method in public API for now because the actions performed by this method could be
     * revisited in any future SIS version depending on the experience gained. However we should try to keep the
     * behavior of this method close to the behavior of {@link #getUnicodeIdentifier(Citation)}, which is the
     * method having a public facade.</p>
     *
     * @param  citation The citation for which to infer the code space, or {@code null}.
     * @return A non-empty code space for the given citation without leading or trailing whitespaces,
     *         or {@code null} if the given citation is null or does not have any Unicode identifier or title.
     *
     * @since 0.6
     */
    public static String getCodeSpace(final Citation citation) {
        if (citation instanceof IdentifierSpace<?>) {
            return ((IdentifierSpace<?>) citation).getName();
        } else {
            return getUnicodeIdentifier(citation);
        }
    }
}
