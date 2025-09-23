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
package org.apache.sis.metadata.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import org.apache.sis.metadata.sql.internal.shared.SQLBuilder;


/**
 * Checks the existence of identifiers (usually primary keys) in a set of tables.
 * This class implements a very naive algorithm and is used only when some reasonably meaningful ID are wanted.
 * If "meaningful" ID is not a requirement, then it is much more efficient to rely on the ID numbers generated
 * automatically by the database.
 *
 * <p>This class checks if a given identifier exists in the database. If it exists, then it searches for an unused
 * {@code "proposal-n"} identifier, where {@code "proposal"} is the given identifier and {@code "n"} is a number.
 * The algorithm in this class takes advantage of the fact that alphabetical order is not the same as numerical
 * order for scanning a slightly smaller number of records (however the gain is significant only in special cases.
 * Generally speaking this class is not for tables having thousands of identifier beginning with the given prefix).
 * However, the selected numbers are not guaranteed to be in increasing order if there is "holes" in the sequence of
 * numbers (i.e. if some old records have been deleted). Generating strictly increasing sequence is not a goal of this
 * class, since it would be too costly.</p>
 *
 * <h2>Assumptions</h2>
 * <ul>
 *   <li>{@code SELECT DISTINCT "ID" FROM "Table" WHERE "ID" LIKE 'proposal%' ORDER BY "ID";} is assumed efficient.
 *       For example in the case of a PostgreSQL database, it requires PostgreSQL 8.0 or above with a {@code btree}
 *       index and C locale.</li>
 *   <li>The ordering of the {@code '-'} and {@code '0'} to {@code '9'} characters compared to other characters
 *       is the same as ASCII. This condition needs to hold only for those particular characters (the ordering
 *       between letters does not matter).</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class IdentifierGenerator implements AutoCloseable {
    /**
     * The character to be used as a separator between the prefix and the sequence number.
     */
    static final char SEPARATOR = '-';

    /**
     * The statement to use for searching free identifiers.
     */
    private final PreparedStatement statement;

    /**
     * A helper object for building SQL statements, determined from database metadata.
     */
    private final SQLBuilder buffer;

    /**
     * Index of the first character to parse in the identifier in order to get its sequential number.
     */
    private int parseAt;

    /**
     * The greatest sequential number found during the search for a free identifier.
     * This will be used only if we found no "hole" in the sequence of numbers.
     */
    private int maximalSequenceNumber;

    /**
     * If different than 0, the suggested sequential number to append to the identifier.
     */
    private int freeSequenceNumber;

    /**
     * Creates a new generator.
     *
     * @param  schema  the schema, or {@code null} if none.
     * @param  table   the table name where to search for an identifier.
     * @param  source  information about the metadata database.
     * @param  column  name of the identifier (primary key) column.
     * @param  buffer  a helper object for building SQL statements, determined from database metadata.
     */
    IdentifierGenerator(final MetadataSource source, final String schema, final String table, final String column,
            final SQLBuilder buffer) throws SQLException
    {
        assert Thread.holdsLock(source);
        this.buffer = buffer;
        buffer.clear().append("SELECT DISTINCT ")
              .appendIdentifier(column).append(" FROM ").appendIdentifier(schema, table).append(" WHERE ")
              .appendIdentifier(column).append(" LIKE ? ORDER BY ")
              .appendIdentifier(column);
        statement = source.connection().prepareStatement(buffer.toString());
    }

    /**
     * Searches for a free identifier. If the given proposal is already in use, then this method will search
     * for another identifier of the form {@code "proposal-n"} not in use, where {@code "n"} is a number.
     *
     * @param  proposal  the proposed identifier. It will be returned if not currently used.
     * @return an identifier which does not exist at the time this method has been invoked.
     * @throws SQLException if an error occurred while searching for an identifier.
     */
    final String identifier(String proposal) throws SQLException {
        statement.setString(1, buffer.clear().appendWildcardEscaped(proposal).append('%').toString());
        try (ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                String current = rs.getString(1);
                if (current.equals(proposal)) {
                    /*
                     * The proposed identifier is already used. If there are no other identifiers,
                     * just append "-1" and we are done. Otherwise we need to search for a "hole"
                     * in the sequence of number suffixes.
                     */
                    parseAt = proposal.length() + 1;
                    freeSequenceNumber    = 0;
                    maximalSequenceNumber = 0;
                    int expected = 0;
searchValidRecord:  while (rs.next()) {
                        current = rs.getString(1);
                        assert current.startsWith(proposal) : current;
                        while (current.length() > parseAt) {
                            int c = current.codePointBefore(parseAt);
                            if (c < SEPARATOR) continue searchValidRecord;
                            if (c > SEPARATOR) break searchValidRecord;
                            c = current.codePointAt(parseAt);
                            /*
                             * Intentionally exclude any record having leading zeros,
                             * since it would confuse our algorithm.
                             */
                            if (c < '1') continue searchValidRecord;
                            if (c > '9') break searchValidRecord;
                            final String prefix = current.substring(0, parseAt);
                            current = search(rs, current, prefix, ++expected);
                            if (current == null) {
                                break searchValidRecord;
                            }
                        }
                    }
                    int n = freeSequenceNumber;             // The hole found during iteration.
                    if (n == 0) {
                        n = maximalSequenceNumber + 1;      // If no hole, use the maximal number + 1.
                    }
                    proposal = proposal + SEPARATOR + n;
                }
            }
        }
        return proposal;
    }

    /**
     * Searches for an available identifier, assuming that the elements in the given
     * {@code ResultSet} are sorted in alphabetical (not numerical) order.
     *
     * @param rs        the result set from which to get next records. Its cursor position is the
     *                  <strong>second</strong> record to inspect (i.e. a record has already been
     *                  extracted before the call to this method).
     * @param current   the ID of the record which has been extracted before the call to this method.
     *                  It must start with {@code prefix} while not equals to {@code prefix}.
     * @param prefix    the prefix that an ID must have in order to be accepted.
     * @param expected  the next expected number. If this number is not found, then it will be assumed available.
     * @return          the ID that stopped the search (which is going to be the first element of the next iteration),
     *                  or {@code null} if we should stop the search.
     * @throws SQLException if an error occurred while querying the database.
     */
    private String search(final ResultSet rs, String current, final String prefix, int expected)
            throws SQLException
    {
        /*
         * The first condition below should have been verified by the caller. If that
         * condition holds, then the second condition is a consequence of the DISTINCT
         * keyword in the SELECT statement, which should ensure !current.equals(prefix).
         */
        assert current.startsWith(prefix);
        assert current.length() > prefix.length() : current;
        do {
            final int n;
            try {
                n = Integer.parseInt(current.substring(parseAt));
            } catch (NumberFormatException e) {
                /*
                 * We expect only records with an identifier compliant with our syntax. If we
                 * encounter a non-compliant identifier, just ignore it. There is no risk of
                 * key collision since we are not going to generate a non-compliant ID.
                 */
                if (rs.next()) {
                    current = rs.getString(1);
                    continue;
                }
                return null;
            }
            /*
             * If we found a higher number than the expected one, then we found a "hole" in the
             * sequence of numbers. Remember the value of the hole and returns null for stopping
             * the search.
             */
            if (n > expected) {
                freeSequenceNumber = expected;
                return null;
            }
            if (n != expected) {
                // Following should never happen (I think).
                throw new SQLNonTransientException(current);
            }
            expected++;
            /*
             * Remember the highest value found so far. This will be used only
             * if we failed to find any "hole" in the sequence of numbers.
             */
            if (n > maximalSequenceNumber) {
                maximalSequenceNumber = n;
            }
            if (!rs.next()) {
                return null;
            }
            /*
             * Gets the next record, skipping every ones starting with the current one.
             * For example if the current record is "proposal-1", then the following block
             * will skip "proposal-10", "proposal-11", etc. until it reaches "proposal-2".
             */
            final String next = current.substring(0, prefix.length() + 1);
            current = rs.getString(1);
            if (current.startsWith(next)) {
                current = search(rs, current, next, n*10);
                if (current == null) {
                    return null;
                }
            }
        } while (current.startsWith(prefix));
        return current;
    }

    /**
     * Releases resources used by this {@code IdentifierGenerator}.
     */
    @Override
    public void close() throws SQLException {
        statement.close();
    }
}
