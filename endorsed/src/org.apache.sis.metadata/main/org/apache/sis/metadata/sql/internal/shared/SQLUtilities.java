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
package org.apache.sis.metadata.sql.internal.shared;

import java.sql.SQLException;
import java.sql.SQLDataException;
import java.sql.DatabaseMetaData;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;


/**
 * Utilities relative to the SQL language.
 *
 *     <strong>DO NOT USE</strong>
 *
 * This class is for Apache SIS internal usage and may change in any future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SQLUtilities {
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
     * An empty string is interpreted as {@code null}.
     *
     * @param  text  the characters to convert to a boolean value, or {@code null}.
     * @return the given characters as a boolean value, or {@code null} if the given text was null or empty.
     * @throws SQLDataException if the given text is non-null and non-empty but not recognized.
     *
     * @see Boolean#parseBoolean(String)
     */
    public static Boolean parseBoolean(final String text) throws SQLException {
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
     * Returns the given text with {@code '_'} and {@code '%'} characters escaped by the database-specific
     * escape characters. This method should be invoked for escaping the values of all {@link DatabaseMetaData}
     * method arguments having a name ending by {@code "Pattern"}. Note that not all arguments are patterns,
     * please check carefully the {@link DatabaseMetaData} javadoc for each method.
     *
     * <h4>Example</h4>
     * If a method expects an argument named {@code tableNamePattern}, then the value should be escaped
     * if an exact match is desired. But if the argument name is only {@code tableName}, then the value
     * should not be escaped.
     *
     * <h4>Missing escape characters</h4>
     * Some databases do not provide an escape character. If the given {@code escape} is null or empty,
     * then this method conservatively returns the pattern unchanged, with the wildcards still active.
     * It will cause the database to return more metadata rows than desired. Callers should filter by
     * comparing the table and schema name specified in each row against the original {@code name}.
     *
     * <p>Note: {@code '%'} could be replaced by {@code '_'} for reducing the number of false positives.
     * However, if a database provides no escape character, maybe it does not support wildcards at all.
     * Leaving the text unchanged and doing the filtering in the caller's code is more conservative.</p>
     *
     * @param  text    the text to escape for use in a context equivalent to the {@code LIKE} statement.
     * @param  escape  value of {@link DatabaseMetaData#getSearchStringEscape()}. May be null or empty.
     * @return the given text with wildcard characters escaped.
     */
    public static String escape(final String text, final String escape) {
        if (text != null && !Strings.isNullOrEmpty(escape)) {
            final char escapeChar = escape.charAt(0);
            StringBuilder buffer = null;
            for (int i = text.length(); --i >= 0;) {
                final char c = text.charAt(i);
                if (c == '_' || c == '%' || (c == escapeChar && text.startsWith(escape, i))) {
                    if (buffer == null) {
                        buffer = new StringBuilder(text);
                    }
                    buffer.insert(i, escape);
                }
            }
            if (buffer != null) {
                return buffer.toString();
            }
        }
        return text;
    }

    /**
     * Returns a string like the given string but with accented letters replaced by any character ({@code '_'})
     * and all characters that are not letter or digit replaced by the wildcard ({@code '%'}).
     *
     * @param  text         the text to get as a SQL LIKE pattern.
     * @param  toLowerCase  whether to convert characters to lower case.
     * @param  escape       value of {@link DatabaseMetaData#getSearchStringEscape()}. May be null or empty.
     * @return the {@code LIKE} pattern for the given text.
     */
    public static String toLikePattern(final String text, final boolean toLowerCase, final String escape) {
        final var buffer = new StringBuilder(text.length());
        toLikePattern(text, 0, text.length(), false, toLowerCase, escape, buffer);
        return buffer.toString();
    }

    /**
     * Returns a <abbr>SQL</abbr> LIKE pattern for the given text. The text is optionally returned in all lower cases
     * for allowing case-insensitive searches. Punctuations are replaced by any sequence of characters ({@code '%'})
     * and non-<abbr>ASCII</abbr> Latin letters are replaced by any single character ({@code '_'}).
     * Ideograms (Japanese, Chinese, …) and hiragana (Japanese) are kept unchanged.
     * This method avoids to put a {@code '%'} symbol as the first character
     * because such character prevents some databases to use their index.
     *
     * @param  text         the text to get as a <abbr>SQL</abbr> {@code LIKE} pattern.
     * @param  textStart    index of the first character to use in the given {@code text}.
     * @param  textEnd      index after the last character to use in the given {@code text}.
     * @param  allowSuffix  whether to append a final {@code '%'} wildcard at the end of the pattern.
     * @param  toLowerCase  whether to convert characters to lower case.
     * @param  escape       value of {@link DatabaseMetaData#getSearchStringEscape()}. May be null or empty.
     * @param  buffer       buffer where to append the <abbr>SQL</abbr> {@code LIKE} pattern.
     */
    public static void toLikePattern(final String text, int textStart, final int textEnd, final boolean allowSuffix,
                                     final boolean toLowerCase, final String escape, final StringBuilder buffer)
    {
        final int bufferStart = buffer.length();
        while (textStart < textEnd) {
            final int c = text.codePointAt(textStart);
            if (Character.isLetterOrDigit(c)) {
                // Ignore accented letters and Greek letters (before `U+0400`) in the search.
                if (c < 0x80 || c >= 0x400) {
                    buffer.appendCodePoint(toLowerCase ? Character.toLowerCase(c) : c);
                } else {
                    appendIfNotRedundant(buffer, '_');
                }
            } else {
                final int length = buffer.length();
                if (length == bufferStart) {
                    // Do not use wildcard in the first character.
                    if (escape != null && (c == '%' || c == '_' || text.startsWith(escape, textStart))) {
                        // Note: there will be bug if `escape` is a repetition of the same character.
                        // But we assume that this corner case is too rare for being worth a check.
                        buffer.append(escape);
                    }
                    buffer.appendCodePoint(c);
                } else if (buffer.charAt(length - 1) != '%') {
                    buffer.append('%');
                }
            }
            textStart += Character.charCount(c);
        }
        if (allowSuffix) {
            appendIfNotRedundant(buffer, '%');
        }
        for (int i=bufferStart; (i = buffer.indexOf("_%", i)) >= 0;) {
            buffer.deleteCharAt(i);
        }
    }

    /**
     * Appends the given wildcard character to the given buffer if the buffer does not ends with {@code '%'}.
     */
    private static void appendIfNotRedundant(final StringBuilder buffer, final char wildcard) {
        final int length = buffer.length();
        if (length == 0 || buffer.charAt(length - 1) != '%') {
            buffer.append(wildcard);
        }
    }

    /**
     * Workaround for what seems to be a Derby 10.11 bug, which seems to behave as if the LIKE pattern
     * had a trailing % wildcard. This can be verified with the following query on the EPSG database:
     *
     * {@snippet lang="sql" :
     *     SELECT COORD_REF_SYS_CODE, COORD_REF_SYS_NAME FROM EPSG."Coordinate Reference System"
     *      WHERE COORD_REF_SYS_NAME LIKE 'NTF%Paris%Lambert%zone%I'
     *     }
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
