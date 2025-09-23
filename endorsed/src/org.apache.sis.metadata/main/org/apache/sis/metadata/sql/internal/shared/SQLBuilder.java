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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.internal.shared.Strings;


/**
 * Utility methods for building SQL statements.
 * This class is for internal purpose only and may change or be removed in any future SIS version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class SQLBuilder extends Syntax {
    /**
     * The {@value} keyword (with a trailing space).
     * Defined as a convenience for identifying locations in the Java code
     * where we start to write a SQL statement using a builder.
     */
    public static final String SELECT = "SELECT ";

    /**
     * The {@value} keyword (with a trailing space).
     * Defined as a convenience for identifying locations in the Java code
     * where we start to write a SQL statement using a builder.
     */
    public static final String INSERT = "INSERT INTO ";

    /**
     * The {@value} keyword (with a trailing space).
     * Defined as a convenience for identifying locations in the Java code
     * where we start to write a SQL statement using a builder.
     */
    public static final String DELETE = "DELETE FROM ";

    /**
     * The buffer where the SQL query is created.
     */
    protected final StringBuilder buffer = new StringBuilder(200);

    /**
     * Creates a new {@code SQLBuilder} initialized from the given database metadata.
     *
     * @param  metadata     the database metadata, or {@code null} if unavailable.
     * @param  quoteSchema  whether the schema name should be written between quotes.
     * @throws SQLException if an error occurred while fetching the database metadata.
     */
    public SQLBuilder(final DatabaseMetaData metadata, final boolean quoteSchema) throws SQLException {
        super(metadata, quoteSchema);
    }

    /**
     * Creates a new {@code SQLBuilder} initialized to the same metadata as the given template.
     *
     * @param  other  the template from which to copy metadata.
     */
    public SQLBuilder(final Syntax other) {
        super(other);
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
     * Appends the given long integer.
     *
     * @param  n  the long to append.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder append(final long n) {
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
     * Appends verbatim a sub-sequence of the given text.
     *
     * @param   text   the text to append verbatim.
     * @param   start  starting index of the text append, inclusive.
     * @param   end    end index of the text to append, exclusive.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder append(final String text, final int start, final int end) {
        buffer.append(text, start, end);
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
     * @param  name  the identifier to append.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendIdentifier(final String name) {
        buffer.append(identifierQuote).append(name).append(identifierQuote);
        return this;
    }

    /**
     * Appends an identifier for an element in the given schema.
     * The following rules apply:
     * <ul>
     *   <li>The given schema will be written only if non-null.</li>
     *   <li>The given schema will be quoted only if {@link #quoteSchema} is {@code true}.</li>
     *   <li>The given name is always quoted.</li>
     * </ul>
     *
     * @param  schema  the schema, or {@code null} or empty if none.
     * @param  name    the name part of the identifier to append.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendIdentifier(final String schema, final String name) {
        return appendIdentifier(null, schema, name, true);
    }

    /**
     * Appends an identifier for an element in the given schema and catalog.
     * The schema is quoted only if {@link #quoteSchema} is {@code true}.
     * The name part is quoted only if {@code quoteName} is {@code true}.
     * Unquoted names are useful when the name is for built-in functions,
     * which often use the lower/upper case convention of the database.
     *
     * <h4>Simplification</h4>
     * If the given catalog is equal to the {@linkplain Connection#getCatalog() catalog which was current} when
     * {@link #setCatalogAndSchema(Connection)} has been invoked, then the catalog name is omitted for simplicty.
     * Likewise, if the given schema is equal to the {@linkplain Connection#getSchema() current schema},
     * then the schema name will also be omitted.
     *
     * @param  catalog    the catalog, or {@code null} or empty if none.
     * @param  schema     the schema, or {@code null} or empty if none.
     * @param  name       the name part of the identifier to append.
     * @param  quoteName  whether to quote the name part.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendIdentifier(final String catalog, String schema, final String name, final boolean quoteName) {
        boolean showSchema  = !(Strings.isNullOrEmpty(schema)  || schema .equals(currentSchema));
        boolean showCatalog = !(Strings.isNullOrEmpty(catalog) || catalog.equals(currentCatalog) || Strings.isNullOrEmpty(catalogSeparator));
        if (showCatalog && isCatalogAtStart) {
            appendIdentifier(catalog).append(catalogSeparator);
            showSchema = true;
            if (schema == null) {
                schema = "";
            }
        }
        if (showSchema) {
            if (quoteSchema) {
                appendIdentifier(schema);
            } else {
                append(schema);
            }
            append('.');
        }
        if (quoteName) {
            appendIdentifier(name);
        } else {
            append(name);
        }
        if (showCatalog && !isCatalogAtStart) {
            append(catalogSeparator).appendIdentifier(catalog);
        }
        return this;
    }

    /**
     * Appends a {@code "= <value>"} string in a {@code SELECT} statement.
     * The value is written between quotes, except if it is a number or a boolean.
     *
     * @param  value  the value to append, or {@code null}.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendEqualsValue(final Object value) {
        return (value == null) ? append(" IS NULL") : append('=').appendValue(value);
    }

    /**
     * Appends a value in a {@code SELECT} or {@code INSERT} statement.
     * The value is written between quotes.
     *
     * @param  value  the value to append, or {@code null}.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendValue(final String value) {
        return (value == null) ? append("NULL") : append('\'').append(value.replace("'", "''")).append('\'');
    }

    /**
     * Appends a value in a {@code SELECT} or {@code INSERT} statement.
     * If the given value is a character string, then it is written between quotes.
     *
     * <h4>Date and time</h4>
     * The standard SQL date format for inserting or setting dates is {@code 'YYYY-MM-DD'}.
     * This format is accepted by various SQL databases, including PostgreSQL and MySQL.
     * The time format is {@code 'HH:MM:SS'}, optionally followed by a time zone offset
     * in the {@code '+HH:MM} format. If the temporal object provides both a date and a time,
     * these components are separated by a space instead of the ISO 8601 {@code 'T'} character.
     * Example of a date/time with time zone: {@code '2025-03-12 14:30:00+01:00'}.
     *
     * <h4>When to use</h4>
     * {@link java.sql.PreparedStatement} should be used instead of this method,
     * for letting the <abbr>JDBC</abbr> driver performs appropriate conversion.
     * This method is sometime useful for building a {@code WHERE} clause,
     * when the number and type of conditions are not fixed in advance.
     *
     * @param  value  the value to append, or {@code null}.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendValue(final Object value) {
        if (value instanceof Number) {
            buffer.append(value);
        } else if (value instanceof Boolean) {
            buffer.append((Boolean) value ? "TRUE" : "FALSE");
        } else if (value instanceof TemporalAccessor) {
            final var t = (TemporalAccessor) value;
            final LocalDate date = t.query(TemporalQueries.localDate());
            final LocalTime time = t.query(TemporalQueries.localTime());
            if (time == null && date == null) {
                return appendValue(value.toString());
            }
            buffer.append('\'');
            if (date != null) {
                buffer.append(date);        // `toString()` defined as "uuuu-MM-dd" ('u' is year).
                if (time != null) {
                    buffer.append(' ');
                }
            }
            if (time != null) {
                buffer.append(time);        // `toString()` defined as "HH:mm[:ss]" optionally with fractions.
                final ZoneOffset zone = t.query(TemporalQueries.offset());
                if (zone != null) {
                    buffer.append(zone);    // `toString()` defined as "Z" or "±hh:mm" optionally with seconds.
                }
            }
            buffer.append('\'');
        } else {
            return appendValue((value != null) ? value.toString() : (String) null);
        }
        return this;
    }

    /**
     * Appends a string as an escaped {@code LIKE} argument.
     * This method does not put any {@code '} character, and does not accept null argument.
     * If the database does not have predefined wildcard characters, then the query result
     * may contain false positives.
     *
     * <p>This method does not double the simple quotes of the given string on intent, because
     * it may be used in a {@code PreparedStatement}. If the simple quotes need to be doubled,
     * then {@link #appendValue(String)} should be invoked.</p>
     *
     * @param  value  the value to append.
     * @return this builder, for method call chaining.
     *
     * @see #escapeWildcards(String)
     * @see #canEscapeWildcards()
     */
    public final SQLBuilder appendWildcardEscaped(final String value) {
        final int start = buffer.length();
        buffer.append(value);
        if (canEscapeWildcards()) {
            final char escapeChar = wildcardEscape.charAt(0);
            for (int i = buffer.length(); --i >= start;) {
                final char c = buffer.charAt(i);
                if (c == '_' || c == '%' || (c == escapeChar && value.startsWith(wildcardEscape, i))) {
                    buffer.insert(i, wildcardEscape);
                }
            }
        }
        return this;
    }

    /**
     * Appends {@code OFFSET} and {@code FETCH} clauses for fetching only a page of data.
     * If a limit or an offset is appended, a space will be added before the clauses.
     * This method uses ANSI notation for better compatibility with various drivers.
     *
     * @param  offset  the offset to use. If zero or negative, no offset is written.
     * @param  count   number of rows to fetch. If zero or negative, no count is written.
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder appendFetchPage(final long offset, final long count) {
        if (offset > 0) {
            buffer.append(" OFFSET ").append(offset).append(" ROW");
            if (offset > 1) buffer.append('S');
        }
        if (count > 0) {
            buffer.append(" FETCH ").append(offset <= 0 ? "FIRST" : "NEXT").append(' ').append(count).append(" ROW");
            if (count > 1) buffer.append('S');
            buffer.append(" ONLY");
        }
        return this;
    }

    /**
     * Inserts the {@code DISTINCT} keyword after {@code SELECT}.
     * An {@link AssertionError} may be thrown if the buffer content does not starts with {@value #SELECT}.
     *
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder insertDistinctAfterSelect() {
        assert CharSequences.startsWith(buffer, SELECT, false) : buffer;
        buffer.insert(SELECT.length(), "DISTINCT ");
        return this;
    }

    /**
     * Removes the {@code WHERE} clause and everything after it.
     *
     * @return this builder, for method call chaining.
     */
    public final SQLBuilder removeWhereClause() {
        final int index = buffer.indexOf(" WHERE ");
        if (index >= 0) {
            buffer.setLength(index);
        }
        return this;
    }

    /**
     * Returns a SQL statement for adding a column in a table.
     * The returned statement is of the form:
     *
     * {@snippet lang="sql" :
     *     ALTER TABLE "schema"."table" ADD COLUMN "column" type
     *     }
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
     * {@snippet lang="sql" :
     *     ALTER TABLE "schema"."table" ADD CONSTRAINT "table_column_fkey" FOREIGN KEY("column")
     *     REFERENCES "schema"."target" (primaryKey) ON UPDATE CASCADE ON DELETE RESTRICT
     *     }
     *
     * Note that the primary key is <strong>not</strong> quoted on intent.
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
                .appendIdentifier(schema, target).append(" (").appendIdentifier(primaryKey)
                .append(") ON UPDATE ").append(cascade ? "CASCADE" : "RESTRICT")
                .append(" ON DELETE RESTRICT").toString();
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
