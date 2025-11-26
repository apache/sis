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
     * Converts the given string to a Boolean value, or returns {@code null} if the value is unrecognized.
     * This method recognizes "true", "false", "yes", "no", "t", "f", 0 and 1 (case insensitive).
     * An empty string is interpreted as {@code null}.
     *
     * @param  text  the characters to convert to a Boolean value, or {@code null}.
     * @return the given characters as a Boolean value, or {@code null} if the given text was null or empty.
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
    public static String escapeWildcards(final String text, final String escape) {
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
}
