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
package org.apache.sis.util.type;

import org.opengis.util.CodeList;


/**
 * The filters used by {@link CodeLists#valueOf(Class, String)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.02)
 * @version 0.3
 * @module
 */
final class CodeListFilter implements CodeList.Filter {
    /**
     * The name to compare during filtering operation.
     */
    private final String codename;

    /**
     * {@code true} if {@link CodeList#valueOf} is allowed to create new code lists.
     */
    private final boolean canCreate;

    /**
     * Creates a new filter for the specified code name.
     */
    CodeListFilter(final String codename, final boolean canCreate) {
        this.codename  = codename;
        this.canCreate = canCreate;
    }

    /**
     * Returns the name of the code to create, or {@code null} if no new code list shall be created.
     */
    @Override
    public String codename() {
        return canCreate ? codename : null;
    }

    /**
     * Returns {@code true} if the given code match the the name we are looking for.
     */
    @Override
    public boolean accept(final CodeList<?> code) {
        for (final String name : code.names()) {
            if (matches(name, codename)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the given strings are equal, ignoring case, whitespaces and the
     * {@code '_'} character.
     *
     * @param  candidate The first string to compare.
     * @param  search The second string to compare.
     * @return {@code true} if the two strings are equal.
     */
    private static boolean matches(final String candidate, final String search) {
        final int length = candidate.length();
        final int searchLength = search.length();
        int searchIndex=0, n;
        for (int i=0; i<length; i+=n) {
            int c = candidate.codePointAt(i);
            n = Character.charCount(c);
            if (isSignificant(c)) {
                // Fetch the next significant character from the expected string.
                int s;
                do {
                    if (searchIndex >= searchLength) {
                        return false; // The name has more significant characters than expected.
                    }
                    s = search.codePointAt(searchIndex);
                    searchIndex += Character.charCount(s);
                } while (!isSignificant(s));

                // Compare the characters in the same way than String.equalsIgnoreCase(String).
                if (c != s) {
                    c = Character.toUpperCase(c);
                    s = Character.toUpperCase(s);
                    if (c != s) {
                        c = Character.toLowerCase(c);
                        s = Character.toLowerCase(s);
                        if (c != s) {
                            return false;
                        }
                    }
                }
            }
        }
        while (searchIndex < searchLength) {
            final int s = search.charAt(searchIndex);
            if (isSignificant(s)) {
                return false; // The name has less significant characters than expected.
            }
            searchIndex += Character.charCount(s);
        }
        return true;
    }

    /**
     * Returns {@code true} if the given character should be taken in account when comparing two
     * strings. Current implementation ignores whitespace and the {@code '_'} character.
     */
    private static boolean isSignificant(final int c) {
        return !Character.isWhitespace(c) && !Character.isIdentifierIgnorable(c) && c != '_';
    }
}
