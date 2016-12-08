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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.StringTokenizer;


/**
 * Utility methods for building SQL statements.
 * This class is for internal purpose only and may change or be removed in any future SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class SQLBuilder {
    /**
     * The database dialect. This is used for a few database-dependent syntax.
     */
    private final Dialect dialect;

    /**
     * The characters used for quoting identifiers, or an empty string if none.
     * This is the value returned by {@link DatabaseMetaData#getIdentifierQuoteString()}.
     */
    private final String quote;

    /**
     * The string that can be used to escape wildcard characters.
     * This is the value returned by {@link DatabaseMetaData#getSearchStringEscape()}.
     */
    private final String escape;

    /**
     * The buffer where the SQL query is to be created.
     */
    private final StringBuilder buffer = new StringBuilder();

    /**
     * Whether the schema name should be written between quotes. If {@code false},
     * we will let the database engine uses its default lower case / upper case policy.
     *
     * @see #appendIdentifier(String, String)
     */
    private final boolean quoteSchema;

    /**
     * Creates a new {@code SQLBuilder} initialized from the given database metadata.
     *
     * @param  metadata  the database metadata.
     * @throws SQLException if an error occurred while fetching the database metadata.
     */
    public SQLBuilder(final DatabaseMetaData metadata, final boolean quoteSchema) throws SQLException {
        dialect = Dialect.guess(metadata);
        quote   = metadata.getIdentifierQuoteString();
        escape  = metadata.getSearchStringEscape();
        this.quoteSchema = quoteSchema;
    }

    /**
     * Creates a new {@code SQLBuilder} initialized to the same metadata than the given builder.
     *
     * @param other  the builder from which to copy metadata.
     */
    public SQLBuilder(final SQLBuilder other) {
        dialect     = other.dialect;
        escape      = other.escape;
        quote       = other.quote;
        quoteSchema = other.quoteSchema;
    }

    /**
     * Returns {@code true} if the builder is currently empty.
     *
     * @return {@code true} if the builder is empty.
     */
    public final boolean isEmpty() {
        return buffer.length() == 0;
    }

    /**
     * Clears this builder and make it ready for creating a new SQL statement.
     *
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder clear() {
        buffer.setLength(0);
        return this;
    }

    /**
     * Appends the given integer.
     *
     * @param  n  the integer to append.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder append(final int n) {
        buffer.append(n);
        return this;
    }

    /**
     * Appends the given character.
     *
     * @param  c  the character to append.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder append(final char c) {
        buffer.append(c);
        return this;
    }

    /**
     * Appends the given text verbatim.
     * The text should be SQL keywords like {@code "SELECT * FROM"}.
     *
     * @param  keyword  the keyword to append verbatim.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder append(final String keyword) {
        buffer.append(keyword);
        return this;
    }

    /**
     * Appends an identifier between quote characters.
     *
     * @param  identifier  the identifier to append.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendIdentifier(final String identifier) {
        buffer.append(quote).append(identifier).append(quote);
        return this;
    }

    /**
     * Appends an identifier for an element in the given schema.
     * <ul>
     *   <li>The given schema will be written only if non-null</li>
     *   <li>The given schema will be quoted only if {@code quoteSchema} is {@code true}.</li>
     *   <li>The given identifier is always quoted.</li>
     * </ul>
     *
     * @param  schema      the schema, or {@code null} if none.
     * @param  identifier  the identifier to append.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendIdentifier(final String schema, final String identifier) {
        if (schema != null) {
            if (quoteSchema) {
                appendIdentifier(schema);
            } else {
                buffer.append(schema);
            }
            buffer.append('.');
        }
        return appendIdentifier(identifier);
    }

    /**
     * Appends a value in a {@code SELECT} statement.
     * The {@code "="} string will be inserted before the value.
     *
     * @param  value  the value to append, or {@code null}.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendCondition(final Object value) {
        if (value == null) {
            buffer.append(" IS NULL");
            return this;
        }
        buffer.append('=');
        return appendValue(value);
    }

    /**
     * Appends a value in an {@code INSERT} statement.
     *
     * @param  value  the value to append, or {@code null}.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendValue(final Object value) {
        if (value == null) {
            buffer.append("NULL");
        } else if (value instanceof Boolean) {
            buffer.append((Boolean) value ? "TRUE" : "FALSE");
        } else if (value instanceof Number) {
            buffer.append(value);
        } else {
            buffer.append('\'').append(doubleQuotes(value)).append('\'');
        }
        return this;
    }

    /**
     * Appends a string as an escaped {@code LIKE} argument.
     * This method does not put any {@code '} character, and does not accept null argument.
     *
     * <p>This method does not double the simple quotes of the given string on intend, because
     * it may be used in a {@code PreparedStatement}. If the simple quotes need to be doubled,
     * then {@link #doubleQuotes(Object)} should be invoked explicitly.</p>
     *
     * @param  value  the value to append.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendEscaped(final String value) {
        final StringTokenizer tokens = new StringTokenizer(value, "_%", true);
        while (tokens.hasMoreTokens()) {
            buffer.append(tokens.nextToken());
            if (!tokens.hasMoreTokens()) {
                break;
            }
            buffer.append(escape).append(tokens.nextToken());
        }
        return this;
    }

    /**
     * Returns a SQL statement for adding a column in a table.
     * The returned statement is of the form:
     *
     * {@preformat sql
     *   ALTER TABLE "schema"."table" ADD COLUMN "column" type
     * }
     *
     * where {@code type} is some SQL keyword like {@code INTEGER} or {@code VARCHAR}
     * depending on the {@code type} argument.
     *
     * @param  schema     the schema for the table.
     * @param  table      the table to alter with the new column.
     * @param  column     the column to add.
     * @param  type       the column type, or {@code null} for {@code VARCHAR}.
     * @param  maxLength  the maximal length (used for {@code VARCHAR} only).
     * @return a SQL statement for creating the column.
     */
    public final String createColumn(final String schema, final String table,
            final String column, final Class<?> type, final int maxLength)
    {
        clear().append("ALTER TABLE ").appendIdentifier(schema, table)
               .append(" ADD COLUMN ").appendIdentifier(column).append(' ');
        final String sqlType = TypeMapper.keywordFor(type);
        if (sqlType != null) {
            append(sqlType);
        } else {
            append("VARCHAR(").append(maxLength).append(')');
        }
        return toString();
    }

    /**
     * Returns a SQL statement for creating a foreigner key constraint.
     * The returned statement is of the form:
     *
     * {@preformat sql
     *   ALTER TABLE "schema"."table" ADD CONSTRAINT "table_column_fkey" FOREIGN KEY("column")
     *   REFERENCES "schema"."target" (primaryKey) ON UPDATE CASCADE ON DELETE RESTRICT
     * }
     *
     * Note that the primary key is <strong>not</strong> quoted on intend.
     * If quoted are desired, then they must be added explicitly before to call this method.
     *
     * @param  schema      the schema for both tables.
     * @param  table       the table to alter with the new constraint.
     * @param  column      the column to alter with the new constraint.
     * @param  target      the table to reference.
     * @param  primaryKey  the primary key in the target table.
     * @param  cascade     {@code true} if updates in primary key should be cascaded.
     *                     this apply to updates only; delete is always restricted.
     * @return a SQL statement for creating the foreigner key constraint.
     */
    public final String createForeignKey(final String schema, final String table, final String column,
            final String target, final String primaryKey, boolean cascade)
    {
        if (dialect == Dialect.DERBY) {
            // Derby does not support "ON UPDATE CASCADE". It must be RESTRICT.
            cascade = false;
        }
        buffer.setLength(0);
        final String name = buffer.append(table).append('_').append(column).append("_fkey").toString();
        return clear().append("ALTER TABLE ").appendIdentifier(schema, table).append(" ADD CONSTRAINT ")
                .appendIdentifier(name).append(" FOREIGN KEY(").appendIdentifier(column).append(") REFERENCES ")
                .appendIdentifier(schema, target).append(" (").append(primaryKey)
                .append(") ON UPDATE ").append(cascade ? "CASCADE" : "RESTRICT")
                .append(" ON DELETE RESTRICT").toString();
    }

    /**
     * Returns the string representation of the given value with simple quote doubled.
     *
     * @param  value  the object for which to double the quotes in the string representation.
     * @return a string representation of the given object with simple quotes doubled.
     */
    public static String doubleQuotes(final Object value) {
        return value.toString().replace("'", "''");
    }

    /**
     * Returns the SQL statement.
     *
     * @return the SQL statement.
     */
    @Override
    public final String toString() {
        return buffer.toString();
    }
}
