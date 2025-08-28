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
package org.apache.sis.referencing.factory.sql;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.apache.sis.metadata.sql.privy.SQLUtilities;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.IntegerList;
import org.apache.sis.util.privy.AbstractMap;
import org.apache.sis.util.privy.Strings;


/**
 * A map of <abbr>EPSG</abbr> authority codes as keys and object names as values.
 * This map requires a valid connection to the <abbr>EPSG</abbr> database.
 *
 * <h2>Serialization</h2>
 * Serialization of this class stores a copy of all authority codes.
 * The serialization does not preserve any connection to the database.
 *
 * <h2>Garbage collection</h2>
 * This method does not implement {@link AutoCloseable} because the same instance may be shared by many users,
 * since {@link EPSGDataAccess#getAuthorityCodes(Class)} caches {@code AuthorityCodes} instances. Furthermore, we can
 * not rely on the users closing {@code AuthorityCodes} themselves because this is not part of the usual contract
 * for Java collection classes (we could document that recommendation in method Javadoc, but not every developers
 * read Javadoc). Relying on the garbage collector for disposing this resource is far from ideal, but alternatives
 * are not very convincing either (load the same codes many time, have the risk that users do not dispose resources,
 * have the risk to return to user an already closed {@code AuthorityCodes} instance).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 *
 * @see EPSGDataAccess#canClose()
 * @see CloseableReference#close()
 */
@SuppressWarnings("serial")   // serialVersionUID not needed because of writeReplace().
final class AuthorityCodes extends AbstractMap<String,String> implements Serializable {
    /**
     * Highest code value (inclusive) that this {@code AuthorityCodes} support during iterations.
     * This is based on the upper value of the highest range of codes once used by EPSG.
     */
    private static final int MAX_CODE = 69999999;

    /**
     * Index in the {@link #sql} and {@link #statements} arrays.
     */
    private static final int ALL_CODES = 0, NAME_FOR_CODE = 1, CODES_FOR_NAME = 2;

    /**
     * Number of queries stored in the {@link #sql} and {@link #statements} arrays.
     */
    private static final int NUM_QUERIES = 3;

    /**
     * The factory which is the owner of this map. One purpose of this field is to prevent
     * garbage collection of that factory as long as this map is in use. This is required
     * because {@link CloseableReference#dispose()} closes the JDBC connections.
     */
    private final transient EPSGDataAccess factory;

    /**
     * The interface of referencing objects for which this map contains the code.
     * May be a super-interface of the type specified to the constructor.
     */
    final Class<?> type;

    /**
     * The SQL commands that this {@code AuthorityCodes} may need to execute.
     * In this array:
     *
     * <ul>
     *   <li>{@code sql[ALL_CODES]}      is a statement for querying all codes.</li>
     *   <li>{@code sql[NAME_FOR_CODE]}  is a statement for querying the name associated to a single code.</li>
     *   <li>{@code sql[CODES_FOR_NAME]} is a statement for querying the code(s) for an object of a given name.</li>
     * </ul>
     *
     * The array length may be only 1 instead of 3 if there is no <abbr>SQL</abbr> statement for fetching the name.
     */
    private final transient String[] sql;

    /**
     * The JDBC statements for the SQL commands in the {@link #sql} array, created when first needed.
     * All usages of those statements shall be synchronized on the {@linkplain #factory}.
     * This array will also be stored in {@link CloseableReference} for closing the statements
     * when the garbage collector detected that {@code AuthorityCodes} is no longer in use.
     */
    private final transient Statement[] statements;

    /**
     * The result of {@code statements[ALL_CODES]}, created only if requested.
     * The codes will be queried at most once and cached in the {@link #codes} list.
     *
     * <p>Note that if this result set is not closed explicitly, it will be closed implicitly when
     * {@code statements[ALL_CODES]} will be closed. This is because <abbr>JDBC</abbr> specification
     * said that closing a statement also close its result set.</p>
     */
    private transient ResultSet results;

    /**
     * A cache of integer codes. Created only if the user wants to iterate over all codes or asked for the map size.
     */
    private transient IntegerList codes;

    /**
     * Creates a new map of authority codes for the specified type.
     *
     * @param table    the table to query.
     * @param type     the type to query.
     * @param factory  the factory originator.
     */
    AuthorityCodes(final TableInfo table, final Class<?> type, final EPSGDataAccess factory) throws SQLException {
        this.factory = factory;
        sql = new String[NUM_QUERIES];
        statements = new Statement[NUM_QUERIES];
        /*
         * Build the SQL query for fetching the codes of all object. It is of the form:
         *
         *     SELECT code FROM table WHERE DEPRECATED=FALSE ORDER BY code;
         */
        final var buffer = new StringBuilder(100);
        final int columnNameStart = buffer.append("SELECT ").length();
        final int columnNameEnd = buffer.append(table.codeColumn).length();
        buffer.append(" FROM ").append(table.fromClause);
        this.type = table.where(factory, type, buffer);
        final int conditionStart = buffer.length();
        if (table.showColumn != null) {
            buffer.append(table.showColumn).append("=TRUE AND ");
        }
        // Do not put spaces around "=" - SQLTranslator searches for this exact match.
        sql[ALL_CODES] = buffer.append("DEPRECATED=FALSE ORDER BY ").append(table.codeColumn).toString();
        /*
         * Build the SQL query for fetching the codes of object having a name matching a pattern.
         * It is of the form:
         *
         *     SELECT code FROM table WHERE name LIKE ? AND DEPRECATED=FALSE ORDER BY code;
         */
        if (NUM_QUERIES > CODES_FOR_NAME) {
            sql[CODES_FOR_NAME] = buffer.insert(conditionStart, table.nameColumn + " LIKE ? AND ").toString();
            /*
             * Workaround for Derby bug. See `SQLUtilities.filterFalsePositive(…)`.
             */
            String t = sql[CODES_FOR_NAME];
            t = t.substring(0, columnNameEnd) + ", " + table.nameColumn + t.substring(columnNameEnd);
            sql[CODES_FOR_NAME] = t;
        }
        /*
         * Build the SQL query for fetching the name of a single object for a given code.
         * This query will also be used for testing object existence. It is of the form:
         *
         *     SELECT name FROM table WHERE code = ?
         */
        if (NUM_QUERIES > NAME_FOR_CODE) {
            buffer.setLength(conditionStart);
            buffer.replace(columnNameStart, columnNameEnd, table.nameColumn);
            sql[NAME_FOR_CODE] = buffer.append(table.codeColumn).append(" = ?").toString();
        }
        for (int i=0; i<NUM_QUERIES; i++) {
            sql[i] = factory.translator.apply(sql[i]);
        }
    }

    /**
     * Creates a weak reference to this map. That reference will also be in charge of closing the JDBC statements
     * when the garbage collector determined that this {@code AuthorityCodes} instance is no longer in use.
     * See class Javadoc for more information.
     */
    final CloseableReference createReference() {
        return new CloseableReference(this, factory, statements);
    }

    /**
     * Returns the prepared statement at the given index, creating it when first needed.
     * This method must be invoked in a block synchronized on {@link #factory}.
     */
    private PreparedStatement prepareStatement(final int index) throws SQLException {
        var statement = (PreparedStatement) statements[index];
        if (statement == null) {
            statements[index] = statement = factory.connection.prepareStatement(sql[index]);
            sql[index] = null;    // Not needed anymore.
        }
        return statement;
    }

    /**
     * Puts codes associated to the given name in the given collection.
     *
     * @param  pattern  the {@code LIKE} pattern of the name to search.
     * @param  name     the original name. This is a temporary workaround for a Derby bug (see {@code filterFalsePositive(…)}).
     * @param  addTo    the collection where to add the codes.
     * @throws SQLException if an error occurred while querying the database.
     */
    final void findCodesFromName(final String pattern, final String name, final Collection<Integer> addTo) throws SQLException {
        synchronized (factory) {
            final PreparedStatement statement = prepareStatement(CODES_FOR_NAME);
            statement.setString(1, pattern);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final int code = result.getInt(1);
                    if (!result.wasNull() && SQLUtilities.filterFalsePositive(name, result.getString(2))) {
                        addTo.add(code);
                    }
                }
            }
        }
    }

    /**
     * Puts all codes in the given collection. This method is used only as a fallback when {@link EPSGCodeFinder}
     * cannot get a list of authority codes in a more selective way, with some conditions on property values.
     * This method should not be invoked for the most common objects such as <abbr>CRS</abbr> and datum.
     *
     * @param  addTo  the collection where to add all codes.
     * @return whether the collection has changed as a result of this method call.
     * @throws SQLException if an error occurred while querying the database.
     */
    final boolean getAllCodes(final Collection<Integer> addTo) throws SQLException {
        boolean changed = false;
        synchronized (factory) {
            int code;
            for (int index=0; (code = getCodeAt(index)) >= 0; index++) {
                changed |= addTo.add(code);
            }
        }
        return changed;
    }

    /**
     * Returns the code at the given index, or -1 if the index is out of bounds.
     *
     * @param  index  index of the code to fetch.
     * @return the code at the given index, or -1 if out of bounds.
     * @throws SQLException if an error occurred while querying the database.
     */
    private int getCodeAt(final int index) throws SQLException {
        int code;
        synchronized (factory) {
            if (codes == null) {
                codes = new IntegerList(100, MAX_CODE);
                results = (statements[ALL_CODES] = factory.connection.createStatement()).executeQuery(sql[ALL_CODES]);
                sql[ALL_CODES] = null;          // Not needed anymore.
            }
            int more = index - codes.size();    // Positive as long as we need more data.
            if (more < 0) {
                code = codes.getInt(index);     // Get a previously cached value.
            } else {
                final ResultSet r = results;
                if (r == null) {
                    code = -1;                  // Already reached iteration end in a previous call.
                } else do {
                    if (!r.next()) {
                        results = null;
                        r.close();
                        statements[ALL_CODES].close();
                        statements[ALL_CODES] = null;
                        return -1;
                    }
                    code = r.getInt(1);
                    codes.addInt(code);
                    more--;
                } while (more >= 0);
            }
        }
        return code;
    }

    /**
     * Returns {@code true} if this map contains no element.
     * This method fetches at most one row instead of counting all rows.
     */
    @Override
    public boolean isEmpty() {
        try {
            return getCodeAt(0) < 0;
        } catch (SQLException exception) {
            throw factoryFailure(exception);
        }
    }

    /**
     * Counts the number of elements in this map.
     */
    @Override
    public int size() {
        try {
            getCodeAt(Integer.MAX_VALUE);       // Force counting all elements, if not already done.
        } catch (SQLException exception) {
            throw factoryFailure(exception);
        }
        return codes.size();
    }

    /**
     * Returns the object name associated to the given authority code, or {@code null} if none.
     * If there is no name for the {@linkplain #type} of object being queried, then this method
     * returns {@code null}.
     *
     * @param  code  the code for which to get the description. May be a string or an integer.
     * @return the description for the given code, or {@code null} if none.
     */
    @Override
    public String get(final Object code) {
        if (code != null) {
            final int n;
            if (code instanceof Number) {
                n = ((Number) code).intValue();
            } else try {
                n = Integer.parseInt(code.toString());
            } catch (NumberFormatException e) {
                return null;    // Okay by this method contract (the given key does not exist in this map).
            }
            try {
                synchronized (factory) {
                    final PreparedStatement statement = prepareStatement(NAME_FOR_CODE);
                    statement.setInt(1, n);
                    try (ResultSet r = statement.executeQuery()) {
                        while (r.next()) {
                            String name = r.getString(1);
                            if (name != null) {
                                return name;
                            }
                        }
                    }
                }
            } catch (SQLException exception) {
                throw factoryFailure(exception);
            }
        }
        return null;
    }

    /**
     * Returns an iterator over the entries.
     */
    @Override
    public EntryIterator<String,String> entryIterator() {
        return new EntryIterator<String,String>() {
            /** Index of current position. */
            private int index = -1;

            /** The authority code at current position of the iterator, or -1 if we reached iteration end. */
            private int code;

            /** Moves to the next element in the iteration. */
            @Override protected boolean next() {
                try {
                    code = getCodeAt(++index);
                } catch (SQLException exception) {
                    throw factoryFailure(exception);
                }
                return code >= 0;
            }

            /** Returns the key at the current iterator position. */
            @Override protected String getKey() {
                return String.valueOf(code);
            }

            /**
             * Returns pseudo-value at the current iterator position. We do not query the real value because it
             * is costly and useless in the context where this method is used. It should be okay since the users
             * never see the map directly, but only the key set.
             */
            @Override protected String getValue() {
                return "";
            }
        };
    }

    /**
     * Returns a string representation of this map for debugging purpose.
     * This method does not let the default implementation formats all entries,
     * because it would be a costly operation.
     */
    @Override
    public String toString() {
        String size = null;
        synchronized (factory) {
            if (codes != null) {
                size = "size" + (results != null ? " ≥ " : " = ") + codes.size();
            }
        }
        return Strings.toString(getClass(), "type", type.getSimpleName(), null, size);
    }

    /**
     * Invoked when a SQL statement cannot be executed, or the result retrieved.
     */
    private static BackingStoreException factoryFailure(final SQLException exception) {
        return new BackingStoreException(exception.getLocalizedMessage(), exception);
    }

    /**
     * Returns a serializable copy of this set. This method is invoked automatically during serialization.
     * The serialized map of authority codes is disconnected from the underlying database.
     */
    protected Object writeReplace() throws ObjectStreamException {
        return new LinkedHashMap<>(this);
    }

    /*
     * No close() or finalize() method - see class Javadoc for an explanation why.
     */
}
