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
package org.apache.sis.storage.sql.feature;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.sis.filter.Optimization;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.SortByComparator;
import org.apache.sis.metadata.sql.internal.shared.SQLBuilder;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.stream.DeferredStream;
import org.apache.sis.util.stream.PaginedStream;
import org.apache.sis.feature.internal.shared.FeatureProjection;

// Specific to the main branch:
import org.apache.sis.filter.Filter;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.pending.geoapi.filter.SortBy;


/**
 * A stream of {@code Feature} instances from a table. This implementation intercepts some {@link Stream}
 * method calls such as {@link #count()}, {@link #distinct()}, {@link #skip(long)} and {@link #limit(long)}
 * in order to delegate the operation to the underlying SQL database.
 *
 * <p>Optimization strategies are also propagated to streams obtained using {@link #map(Function)} and
 * {@link #mapToDouble(ToDoubleFunction)}. However, for result consistency, no optimization is stacked
 * anymore after either {@link #filter(Predicate)} or {@link #flatMap(Function)} operations are called,
 * because they modify volumetry (the count of stream elements is not bound 1 to 1 to query result rows).</p>
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class FeatureStream extends DeferredStream<AbstractFeature> {
    /**
     * The table which is the source of features.
     */
    private final Table table;

    /**
     * The columns that the user wants to keep, or {@code null} for all columns.
     * The word "projection" in this context is in the <abbr>SQL</abbr> database sense.
     *
     * @see #map(Function)
     */
    private FeatureProjection projection;

    /**
     * The visitor to use for converting filters/expressions to SQL statements.
     * This is used for writing the content of the {@link SelectionClause}.
     * It is usually a singleton instance shared by all databases.
     * It is fetched when first needed.
     *
     * @see #getFilterToSQL()
     */
    private SelectionClauseWriter filterToSQL;

    /**
     * The SQL fragment on the right side of the {@code WHERE} keyword.
     * This buffer does not including the {@code WHERE} keyword.
     * It is created when first needed and discarded after the iterator is created.
     */
    private SelectionClause selection;

    /**
     * {@code true} if at least one predicate given to {@link #filter(Predicate)}
     * is implemented using Java code instead of using SQL statements.
     */
    private boolean hasPredicates;

    /**
     * {@code true} if at least one comparator given to {@link #sorted(Comparator)}
     * is implemented using Java code instead of using SQL statements.
     */
    private boolean hasComparator;

    /**
     * Whether all returned feature instances should be unique.
     */
    private boolean distinct;

    /**
     * The {@code ORDER BY} clauses, or {@code null} if none.
     */
    private SortBy<? super AbstractFeature> sort;

    /**
     * Number of rows to skip in underlying SQL query, or 0 for none.
     *
     * @see #skip(long)
     */
    private long offset;

    /**
     * Maximum number of rows to return, or 0 for no limit. Note: the use of 0 for "no limit" is unusual
     * (a more usual value is -1) because 0 could be valid value for the limit. However, the methods in
     * this class should return {@link #empty()} eagerly when they detect that the stream will be empty.
     * Therefore, we should not have instances of {@code FeatureStream} with a limit which is really zero.
     *
     * @see #limit(long)
     */
    private long count;

    /**
     * Creates a new stream of features.
     *
     * @param table     the source table.
     * @param parallel  whether the stream should be initially parallel.
     */
    FeatureStream(final Table table, final boolean parallel) {
        super(FeatureIterator.CHARACTERISTICS, parallel);
        listener = table.database.createFilterListener();
        this.table = table;
    }

    /**
     * Marks this stream as inactive and returns an empty stream.
     * This method is invoked when an operation resulted in an empty stream.
     */
    private Stream<AbstractFeature> empty() {
        count = 0;
        delegate();                 // Mark this stream as inactive.
        return Stream.empty();
    }

    /**
     * Returns {@code true} if either {@link #count} or {@link #offset} is set.
     * In such case, we cannot continue to build the SQL statement because the
     * {@code OFFSET ... FETCH NEXT} clauses in SQL are executed last.
     * Consequently, in order to have consistent results, the {@link #offset(long)} and
     * {@link #limit(long)} methods need to be the last methods invoked on this stream.
     */
    private boolean isPagined() {
        return (offset | count) != 0;
    }

    /**
     * Returns a stream with features of this stream that match the given predicate.
     * If the given predicate is an instance of {@link Filter}, then this method tries
     * to express the filter using SQL statements.
     */
    @Override
    public Stream<AbstractFeature> filter(final Predicate<? super AbstractFeature> predicate) {
        Objects.requireNonNull(predicate);
        if (predicate == Filter.include()) return this;
        if (predicate == Filter.exclude()) return empty();
        if (isPagined()) {
            /*
             * Offset/limit executed before the filter. Cannot continue to build an SQL statement
             * because the SQL `OFFSET ... FETCH NEXT` clause would be executed after the filter.
             */
            return delegate().filter(predicate);
        }
        if (!(predicate instanceof Filter<?>)) {
            hasPredicates = true;
            return super.filter(predicate);
        }
        if (selection == null) {
            selection = new SelectionClause(table);
        }
        /*
         * Simplify/optimize the filter (it may cause `include` or `exclude` filters to emerge) and try
         * to convert the filter to SQL statements. This is not necessarily an all or nothing operation:
         * if we have a "F₀ AND F₁ AND F₂" chain, it is possible to have some Fₙ as SQL statements and
         * other Fₙ executed in Java code.
         */
        return execute(() -> {
            Stream<AbstractFeature> stream = this;
            final var optimization = new Optimization();
            optimization.setFinalFeatureType(table.featureType);
            for (final var filter : optimization.applyAndDecompose((Filter<? super AbstractFeature>) predicate)) {
                if (filter == Filter.include()) continue;
                if (filter == Filter.exclude()) return empty();
                if (!selection.tryAppend(getFilterToSQL(), filter)) {
                    // Delegate to Java code all filters that we cannot translate to SQL statement.
                    if (stream == this) {
                        stream = super.filter(filter);
                    } else {
                        stream = stream.filter(filter);
                    }
                    hasPredicates = true;
                }
            }
            return stream;
        });
    }

    /**
     * Requests this stream to return distinct feature instances.
     * This operation will be done with a SQL {@code DISTINCT} clause if possible.
     */
    @Override
    public Stream<AbstractFeature> distinct() {
        if (isPagined()) {
            return delegate().distinct();
        } else {
            distinct = true;
            return this;
        }
    }

    /**
     * Returns an equivalent stream that is unordered.
     */
    @Override
    public Stream<AbstractFeature> unordered() {
        if (isPagined()) {
            return delegate().unordered();
        } else {
            sort = null;
            return super.unordered();
        }
    }

    /**
     * Returns an equivalent stream that is sorted by feature natural order.
     * This is defined as a matter of principle, but will cause a {@link ClassCastException} to be thrown
     * when a terminal operation will be executed because {@code Feature} instances are not comparable.
     */
    @Override
    public Stream<AbstractFeature> sorted() {
        if (isPagined()) {
            return delegate().sorted();
        } else {
            return super.sorted();
        }
    }

    /**
     * Returns a stream with features of this stream sorted using the given comparator.
     */
    @Override
    public Stream<AbstractFeature> sorted(final Comparator<? super AbstractFeature> comparator) {
        if (isPagined() || hasComparator) {
            return delegate().sorted(comparator);
        }
        final SortBy<? super AbstractFeature> c = SortByComparator.concatenate(sort, comparator);
        if (c != null) {
            sort = c;
            return this;
        }
        hasComparator = true;
        return super.sorted(comparator);
    }

    /**
     * Discards the specified number of elements.
     * This operation will be done with a SQL {@code OFFSET} clause.
     */
    @Override
    public Stream<AbstractFeature> skip(final long n) {
        // Do not require this stream to be active because this method may be invoked by `PaginedStream`.
        ArgumentChecks.ensurePositive("n", n);
        offset = Math.addExact(offset, n);
        if (count != 0) {
            if (n >= count) {
                return empty();
            }
            count -= n;
        }
        return this;
    }

    /**
     * Truncates this stream to the given number of elements.
     * This operation will be done with a SQL {@code FETCH NEXT} clause.
     */
    @Override
    public Stream<AbstractFeature> limit(final long maxSize) {
        // Do not require this stream to be active because this method may be invoked by `PaginedStream`.
        ArgumentChecks.ensurePositive("maxSize", maxSize);
        if (maxSize == 0) {
            return empty();
        }
        count = (count != 0) ? Math.min(count, maxSize) : maxSize;
        return this;
    }

    /**
     * Returns a stream with results of applying the given function to the elements of this stream.
     * The {@code skip} and {@code limit} operations applied on the returned stream may continue to
     * be optimized.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <R> Stream<R> map(final Function<? super AbstractFeature, ? extends R> mapper) {
        if (projection == null && mapper instanceof FeatureProjection) {
            projection = (FeatureProjection) mapper;
            return (Stream) this;
        }
        final var stream = new PaginedStream<R>(super.map(mapper), this);
        stream.listener = listener;
        return stream;
    }

    /**
     * Counts the number of elements in the table. This method uses a simpler SQL statement than the one
     * associated to the table. For example if a property is an association to another feature, the SQL
     * statement will contain only the foreigner key values, not an inner join to the other feature table.
     */
    @Override
    public long count() {
        /*
         * If at least one filter is implemented by Java code (i.e. has not been translated to SQL statement),
         * then we cannot count using SQL only. We have to rely on the more costly default implementation.
         */
        if (hasPredicates || count != 0) {
            return super.count();
        }
        lock(table.database.transactionLocks);
        try (Connection connection = getConnection()) {
            makeReadOnly(connection);
            /*
             * Build the full SQL statement here, without using `FeatureAdapter.sql`,
             * because we do not need to follow foreigner keys.
             */
            final var sql = new SQLBuilder(table.database);
            sql.setCatalogAndSchema(connection);
            sql.append(SQLBuilder.SELECT).append("COUNT(");
            if (distinct) {
                String separator = "DISTINCT ";
                for (final Column attribute : table.attributes) {
                    sql.append(separator).appendIdentifier(attribute.label);
                    separator = ", ";
                }
            } else {
                // If we want a count and no distinct clause is specified, a single column is sufficient.
                sql.appendIdentifier(table.attributes[0].label);
            }
            table.appendFromClause(sql.append(')'));
            if (selection != null) {
                final String filter = selection.query(connection, null);
                if (filter != null) {
                    sql.append(" WHERE ").append(filter);
                }
            }
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(sql.toString()))
            {
                while (rs.next()) {
                    final long n = rs.getLong(1);
                    if (!rs.wasNull()) return n;
                }
            }
        } catch (Exception e) {
            throw cannotExecute(e);
        } finally {
            unlock();
        }
        return Math.max(super.count() - offset, 0);
    }

    /**
     * Acquires a connection to the database. The {@link #makeReadOnly(Connection)} method should be invoked
     * after this method. Those two methods are separated for allowing the immediate use of the connection
     * in a {@code try ... finally} block.
     *
     * @return a new connection to the database.
     * @throws SQLException if we cannot create a new connection. See {@link DataSource#getConnection()} for details.
     */
    private Connection getConnection() throws SQLException {
        return table.database.source.getConnection();
    }

    /**
     * Makes the given connection read-only and apply some configuration for better performances.
     * Current configurations are:
     *
     * <ul>
     *   <li>{@linkplain Connection#setReadOnly(boolean) querying read-only}.</li>
     * </ul>
     *
     * @param  connection  the connection to configure.
     */
    private void makeReadOnly(final Connection connection) throws SQLException {
        if (table.database.dialect.supportsReadOnlyUpdate()) {
            connection.setReadOnly(true);
        }
        /*
         * Do not invoke `setAutoCommit(false)` because it causes the database to hold read locks,
         * even if we are doing only SELECT statements. On Derby database it causes the following
         * exception to be thrown when closing the connection because we do not invoke `commit()`:
         *
         *     ERROR 25001: Cannot close a connection while a transaction is still active.
         */
    }

    /**
     * Creates the iterator which will provide the actual feature instances.
     * The {@linkplain Spliterator#characteristics() characteristics} of the returned iterator
     * must be the characteristics declared in the {@code FeatureStream} constructor.
     *
     * <p>This method is invoked at most once, generally when a stream terminal operation is invoked.
     * After this method is invoked, this stream will not be active anymore.</p>
     *
     * @return an iterator over the feature elements.
     * @throws DataStoreException if a data model dependent error occurs.
     * @throws SQLException if an error occurs while executing the SQL statement.
     */
    @Override
    protected Spliterator<AbstractFeature> createSourceIterator() throws Exception {
        Table projected = table;
        final Database<?> database = projected.database;
        lock(database.transactionLocks);
        final Connection connection = getConnection();
        setCloseHandler(connection);  // Executed only if an exception occurs in the middle of this method.
        makeReadOnly(connection);
        /*
         * Helper methods for spatial functions which expect or return geometry objects:
         * find SRID codes of CRSs and conversely by searching in "spatial_ref_sys" table.
         */
        final InfoStatements spatialInformation;
        if (database.getSpatialSchema().isPresent()) {
            spatialInformation = database.createInfoStatements(connection);
        } else {
            spatialInformation = null;
        }
        /*
         * The `projection` is what the user requested by a call to `Stream.map(Function)`.
         * The `queriedProjection` is what we will use for building a SQL query, possibly
         * with some expressions replaced by SQL fragments. Then, `completion` is what we
         * could not do in SQL and will need to finish in Java.
         */
        FeatureProjection completion = null;
        FeatureProjection queriedProjection = projection;
        if (queriedProjection != null) {
            /*
             * First, if the projection contains expressions other than value references,
             * try to replace them by SQL functions written in the "SELECT" statement.
             */
            if (queriedProjection.hasOperations()) {
                final var columnSQL = new SelectionClause(projected);
                @SuppressWarnings("LocalVariableHidesMemberVariable")
                final SelectionClauseWriter filterToSQL = getFilterToSQL();
                queriedProjection = queriedProjection.replaceExpressions((name, expression) -> {
                    final JDBCType type = filterToSQL.writeFunction(columnSQL, expression);
                    if (type != null) try {
                        columnSQL.append(" AS ").appendIdentifier(name);
                        String sql = columnSQL.query(connection, spatialInformation);
                        expression = new ComputedColumn(database, type, name, sql);
                    } catch (Exception e) {
                        throw cannotExecute(e);
                    }
                    columnSQL.clear();
                    return expression;
                });
            }
            /*
             * Build a pseudo-table (a view) with the subset of columns specified by the projection.
             */
            final var unhandled   = new BitSet();
            final var reusedNames = new HashSet<String>();
            projected = new Table(projected, queriedProjection, reusedNames, unhandled);
            completion = queriedProjection.forPreexistingFeatureInstances(unhandled.stream().toArray());
            if (completion != null && !reusedNames.containsAll(completion.requiredSourceProperties())) {
                /*
                 * Cannot use `projected` because some expressions need properties available only
                 * in the source features. Request full feature instances from the original table.
                 */
                projected  = table;
                completion = queriedProjection;
            }
        }
        final var features = new FeatureIterator(projected, connection, spatialInformation,
                                    distinct, selection, sort, offset, count, completion);
        setCloseHandler(features);
        selection = null;             // Let the garbage collector do its work.
        return features;
    }

    /**
     * Returns the converter from filters/expressions to the {@code WHERE} part of <abbr>SQL</abbr> statements.
     * The value is cached for avoiding synchronization.
     */
    private SelectionClauseWriter getFilterToSQL() {
        if (filterToSQL == null) {
            filterToSQL = table.database.getFilterToSupportedSQL();
        }
        return filterToSQL;
    }

    /**
     * Returns a string representation of this stream for debugging purposes.
     * The returned string tells whether filtering and sorting are done using
     * SQL statement, Java code, or a mix of both.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "table", table.name.table,
                "predicates", hasPredicates ? (filterToSQL != null ? "mixed" : "Java") : (selection != null ? "SQL" : null),
                "comparator", hasComparator ? (sort != null ? "mixed" : "Java") : (sort != null ? "SQL" : null),
                "distinct",   distinct ? Boolean.TRUE : null,
                "offset",     offset != 0 ? offset : null,
                "count",      count  != 0 ? count  : null);
    }
}
