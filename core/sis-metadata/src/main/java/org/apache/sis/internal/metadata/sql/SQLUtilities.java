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
package org.apache.sis.internal.metadata.sql;

import java.sql.SQLException;
import java.sql.SQLDataException;
import java.sql.DatabaseMetaData;
import org.apache.sis.util.Static;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.resources.Errors;


/**
 * Utilities relative to the SQL language.
 *
 *     <strong>DO NOT USE</strong>
 *
 * This class is for Apache SIS internal usage and may change in any future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
public final class SQLUtilities extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private SQLUtilities() {
    }

    /**
     * Returns a simplified form of the URL (truncated before the first {@code ?} or {@code ;} character),
     * for logging or informative purpose only.
     *
     * @param  metadata  the metadata of the database.
     * @return a simplified version of database URL.
     * @throws SQLException if an error occurred while fetching the URL.
     */
    public static String getSimplifiedURL(final DatabaseMetaData metadata) throws SQLException {
        String url = metadata.getURL();
        int s1 = url.indexOf('?'); if (s1 < 0) s1 = url.length();
        int s2 = url.indexOf(';'); if (s2 < 0) s2 = url.length();
        return url.substring(0, Math.min(s1, s2));
    }

    /**
     * Converts the given string to a boolean value, or returns {@code null} if the value is unrecognized.
     * This method recognizes "true", "false", "yes", "no", "t", "f", 0 and 1 (case insensitive).
     *
     * @param  text  the characters to convert to a boolean value, or {@code null}.
     * @return the given characters as a boolean value, or {@code null} if the given text was null or empty.
     * @throws SQLDataException if the given text is non-null and non-empty but not recognized.
     *
     * @since 0.8
     */
    public static Boolean toBoolean(final String text) throws SQLException {
        if (text == null) {
            return null;
        }
        switch (text.length()) {
            case 0: return null;
            case 1: {
                switch (text.charAt(0)) {
                    case '0': case 'n': case 'N': case 'f': case 'F': return Boolean.FALSE;
                    case '1': case 'y': case 'Y': case 't': case 'T': return Boolean.TRUE;
                }
                break;
            }
            default: {
                if (text.equalsIgnoreCase("true")  || text.equalsIgnoreCase("yes")) return Boolean.TRUE;
                if (text.equalsIgnoreCase("false") || text.equalsIgnoreCase("no"))  return Boolean.FALSE;
                break;
            }
        }
        throw new SQLDataException(Errors.format(Errors.Keys.CanNotConvertValue_2, text, Boolean.class));
    }

    /**
     * Returns a string like the given string but with all characters that are not letter or digit
     * replaced by the wildcard % character.
     *
     * <p>This method avoid to put a % symbol as the first character, since it prevent some databases
     * to use their index.</p>
     *
     * @param  identifier the identifier to get as a SQL LIKE pattern.
     * @return the given identifier as a SQL LIKE pattern.
     */
    public static String toLikePattern(final String identifier) {
        boolean isLetterOrDigit = false;
        final StringBuilder buffer = new StringBuilder(identifier.length());
        for (int c, i = 0; i < identifier.length(); i += Character.charCount(c)) {
            c = identifier.codePointAt(i);
            if (Character.isLetterOrDigit(c)) {
                buffer.appendCodePoint(c);
                isLetterOrDigit = true;
            } else if (isLetterOrDigit) {
                isLetterOrDigit = false;
                buffer.append('%');
            } else {
                final int p = buffer.length();
                if (p == 0 || buffer.charAt(p-1) != '%') {
                    buffer.appendCodePoint(c != '%' ? c : '_');
                }
            }
        }
        return buffer.toString();
    }

    /**
     * Workaround for what seems to be a Derby 10.11 bug, which seems to behave as if the LIKE pattern
     * had a trailing % wildcard. This can be verified with the following query on the EPSG database:
     *
     * {@preformat sql
     *   SELECT COORD_REF_SYS_CODE, COORD_REF_SYS_NAME FROM EPSG."Coordinate Reference System"
     *    WHERE COORD_REF_SYS_NAME LIKE 'NTF%Paris%Lambert%zone%I'
     * }
     *
     * which returns "NTF (Paris) / Lambert zone I" as expected but also zones II and III.
     *
     * @param  expected  the string to search.
     * @param  actual    the string found in the database.
     * @return {@code true} if the given string can be accepted.
     */
    @Workaround(library = "Derby", version = "10.11")
    public static boolean filterFalsePositive(final String expected, final String actual) {
        return CharSequences.equalsFiltered(expected, actual, Characters.Filter.LETTERS_AND_DIGITS, false);
    }
}
