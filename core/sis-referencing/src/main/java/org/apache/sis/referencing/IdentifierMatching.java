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
package org.apache.sis.referencing;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.util.Citations;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;


/**
 * Whether two {@code IdentifiedObject} instances contain matching, mismatching or contradictory identifiers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public enum IdentifierMatching {
    /**
     * The two identified objects share at least one common identifier and no contradiction was found.
     * In such case, we can consider that the two {@code IdentifiedObject} instances are for the same object.
     *//**
     * The two identified objects share at least one common identifier and no contradiction was found.
     * In such case, we can consider that the two {@code IdentifiedObject} instances are for the same object.
     */
    MATCH,

    /**
     * The two identified objects have at least one identifier with different codes in the same code space.
     * In such case, we can consider that the two {@code IdentifiedObject} instances are for different objects.
     */
    MISMATCH,

    /**
     * The two identified objects have some identifiers meeting the {@code MATCH} condition,
     * and other identifiers meeting the {@code MISMATCH} condition.
     */
    CONTRADICTION,

    /**
     * The two identified objects do not have any identifier meeting the {@code MATCH} or {@code MISMATCH} condition.
     */
    UNKNOWN;

    /**
     * Determines whether a match or mismatch is found between the two given collection of identifiers.
     * If any of the given collection is {@code null} or empty, this method returns {@link #UNKNOWN}.
     *
     * @param  id1 The first collection of identifiers, or {@code null}.
     * @param  id2 The second collection of identifiers, or {@code null}.
     * @return {@link #MATCH} or {@link #MISMATCH} if (mis)matching can be determined without contradiction,
     *         or {@link #CONTRADICTION} or {@link #UNKNOWN} otherwise.
     */
    public static IdentifierMatching determine(final Iterable<? extends Identifier> id1,
                                               final Iterable<? extends Identifier> id2)
    {
        if (id1 != null && id2 != null) {
            Boolean match = null;
            for (final Identifier identifier : id1) {
                final Citation authority = identifier.getAuthority();
                final String   codeSpace = identifier.getCodeSpace();
                for (final Identifier other : id2) {
                    if (authorityMatches(identifier, authority, codeSpace)) {
                        final boolean m = CharSequences.equalsFiltered(identifier.getCode(),
                                other.getCode(), Characters.Filter.UNICODE_IDENTIFIER, true);
                        if (match == null) {
                            match = m;
                        } else if (match != m) {
                            return CONTRADICTION;
                        }
                    }
                }
            }
            if (match != null) {
                return match ? MATCH : MISMATCH;
            }
        }
        return UNKNOWN;
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
                return Citations.identifierMatches(authority, other);
            }
        }
        if (codeSpace != null) {
            final String other = identifier.getCodeSpace();
            if (other != null) {
                return CharSequences.equalsFiltered(codeSpace, other, Characters.Filter.UNICODE_IDENTIFIER, true);
            }
        }
        return false;
    }
}
