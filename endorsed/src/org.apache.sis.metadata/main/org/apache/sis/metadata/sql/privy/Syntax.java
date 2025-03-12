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
     * we will let the database engine uses its default lower case / upper case policy.
     *
     * @see SQLBuilder#appendIdentifier(String, String)
     */
    final boolean quoteSchema;

    /**
     * The string that can be used to escape wildcard characters.
     * This is the value returned by {@link DatabaseMetaData#getSearchStringEscape()}.
     */
    protected final String escape;

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
            escape  = "\\";
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
}
