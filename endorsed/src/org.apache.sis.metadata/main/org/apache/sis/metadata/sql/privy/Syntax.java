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
package org.apache.sis.metadata.sql.privy;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;


/**
 * Information about the syntax to use for building SQL statements.
 * This object extract from {@link DatabaseMetaData} the information needed by {@link SQLBuilder}.
 * It can be cached if many {@link SQLBuilder} instances are going to be created.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class Syntax {
    /**
     * The database dialect. This is used for a few database-dependent syntax.
     */
    public final Dialect dialect;

    /**
     * The characters used for quoting identifiers, or an empty string if none.
     * This is the value returned by {@link DatabaseMetaData#getIdentifierQuoteString()}.
     */
    final String quote;

    /**
     * Whether the schema name should be written between quotes. If {@code false},
     * Apache SIS lets the database engine uses its default lower case / upper case policy.
     * This flag is usually {@code true} when the schema was specified by the user or has
     * been discovered from database metadata. This flag is {@code false} when the schema
     * has been created by an Apache SIS script, which intentionally uses unquoted schema
     * for integration with database conventions.
     *
     * @see SQLBuilder#appendIdentifier(String, String)
     */
    final boolean quoteSchema;

    /**
     * The string that can be used to escape wildcard characters.
     * This is the value returned by {@link DatabaseMetaData#getSearchStringEscape()}.
     * It may be null or empty if the database has no escape character, in which case
     * the statement should be of the form {@code WHERE "column" LIKE ? ESCAPE '\'}
     * (replace {@code '\'} by the desired escape character).
     *
     * @see #escapeWildcards(String)
     * @see SQLBuilder#appendWildcardEscaped(String)
     */
    final String escape;

    /**
     * Creates a new {@code Syntax} initialized from the given database metadata.
     *
     * @param  metadata     the database metadata, or {@code null} if unavailable.
     * @param  quoteSchema  whether the schema name should be written between quotes.
     * @throws SQLException if an error occurred while fetching the database metadata.
     */
    public Syntax(final DatabaseMetaData metadata, final boolean quoteSchema) throws SQLException {
        if (metadata != null) {
            dialect = Dialect.guess(metadata);
            quote   = metadata.getIdentifierQuoteString();
            escape  = metadata.getSearchStringEscape();
        } else {
            dialect = Dialect.ANSI;
            quote   = "\"";
            escape  = null;
        }
        this.quoteSchema = quoteSchema;
    }

    /**
     * Creates a new {@code Syntax} initialized to the same metadata as the given template.
     *
     * @param  other  the template from which to copy metadata.
     */
    Syntax(final Syntax other) {
        dialect     = other.dialect;
        escape      = other.escape;
        quote       = other.quote;
        quoteSchema = other.quoteSchema;
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
     * @param  text  the text to escape for use in a context equivalent to the {@code LIKE} statement.
     * @return the given text with wildcard characters escaped.
     */
    public final String escapeWildcards(final String text) {
        return SQLUtilities.escape(text, escape);
    }

    /**
     * Returns {@code false} if the database can <em>not</em> escape wildcard characters.
     * In such case, the string returned by {@link #escapeWildcards(String)} may produce
     * false positives, and the caller needs to apply additional filtering.
     *
     * <p>This method returns {@code true} for the vast majority of major databases,
     * but it may return {@code false} with incomplete <abbr>JDBC</abbr> drivers.</p>
     *
     * @return whether the database can escape wildcard characters.
     */
    public final boolean canEscapeWildcards() {
        return (escape != null) && !escape.isEmpty();
    }
}
