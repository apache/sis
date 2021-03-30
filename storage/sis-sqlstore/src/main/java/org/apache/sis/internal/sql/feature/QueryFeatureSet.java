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
package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.sql.DataSource;

import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

import org.apache.sis.internal.metadata.sql.SQLBuilder;
import org.apache.sis.internal.storage.AbstractFeatureSet;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.collection.BackingStoreException;
import org.opengis.filter.SortProperty;

/**
 * Stores SQL query given at built time, and execute it when calling {@link #features(boolean) data stream}. Note that
 * {@link #getType() data type} is defined by analyzing sql statement metadata. Note that user query can be modified at
 * before execution to adapt various parameters overridable at fetch time as offset and limit through
 * {@link Stream#skip(long)} and {@link Stream#limit(long)}.
 *
 * Note that this component models query result as close as possible, so built data type will be simple feature type (no
 * association).
 *
 * TODO: move query analysis in a dedicated class.
 *
 * @author Alexis Manin (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public class QueryFeatureSet extends AbstractFeatureSet {

    /**
     * A regex searching for ANSI or PostgreSQL way of defining max number of rows to return. For details, see
     * <a href="https://www.postgresql.org/docs/current/sql-select.html#SQL-LIMIT">PostgreSQL LIMIT documentation</a>.
     * Documentation states that value could be a reference to a variable name, so we do not search for a digit.
     */
    private static final Pattern LIMIT_PATTERN = Pattern.compile("(?:FETCH|LIMIT)(?:\\s+(?:FIRST|NEXT))?\\s+([^\\s]+)(?:\\s+ROWS?)?(?:\\s+ONLY)?", Pattern.CASE_INSENSITIVE);
    /**
     * Search for ANSI or PostgreSQL way of defining a number of rows to skip when returning results. For details, see
     * <a href="https://www.postgresql.org/docs/current/sql-select.html#SQL-LIMIT">PostgreSQL LIMIT documentation</a>.
     */
    private static final Pattern OFFSET_PATTERN = Pattern.compile("OFFSET\\s+([^\\s]+)(?:\\s+ROWS?)?", Pattern.CASE_INSENSITIVE);

    /**
     * Check for a selection of distinct rows.
     */
    private static final Pattern DISTINCT_PATTERN = Pattern.compile("^\\s*SELECT\\s+DISTINCT", Pattern.CASE_INSENSITIVE);

    /**
     * Keep builder to allow native limit and offset through stream operation.
     */
    private final SQLBuilder queryBuilder;

    /**
     * SQL database handler. Used to open new connections at query time.
     */
    private final DataSource source;

    /**
     * Component in charge of conversion from SQL entry to Feature entity. Also provides output data type.
     */
    private final FeatureAdapter adapter;

    /**
     * Offset and limit defined in user query, if any. If none is found, or we cannot determine safely their value (not
     * specified as a literal but as a variable name), values will be set to -1.
     *
     * @implNote  BE CAREFUL ! We use these fields for optimisations. We remove values from user query if safely
     * identified, and add them only at query time, with additional skip/offset defined through stream or java query.
     */
    private final long originOffset, originLimit;

    /**
     * A flag indicating that we've safely identified a {@literal DISTINCT} keyword in the user query.
     */
    private final boolean distinct;

    /**
     * Debug flag to activate (use {@link PrefetchSpliterator}) or de-activate (use {@link ResultSpliterator})
     * batch loading of results.
     */
    boolean allowBatchLoading = true;
    /**
     * Profiling variable. Define the fraction (0 none, 1 all) of a single fetch (as defined by {@link ResultSet#getFetchSize()}
     * that {@link PrefetchSpliterator} will load in one go.
     */
    float fetchRatio = 0.5f;
    /**
     * Profiling variable, serves to define {{@link PreparedStatement#setFetchSize(int)} SQL result fetch size}.
     */
    int fetchSize = 100;

    /**
     * Same as {@link #QueryFeatureSet(SQLBuilder, DataSource, Connection)}, except query is provided by a fixed text
     * instead of a builder.
     */
    public QueryFeatureSet(String query, DataSource source, Connection conn) throws SQLException {
        this(fromQuery(query, conn), source, conn);
    }

    /**
     * Create a new feature set whose data is provided by given user query. Note that query will be compiled now, but
     * only executed when {@link #features(boolean) acquiring a data stream}.
     *
     * @param queryBuilder Contains user-defined SQL query. It must contains a valid and complete SQL statement, because
     *                     it will be compiled at built time to define feature type. Note that we will make a copy of
     *                     it, so any modification done after this feature set is created won't have any effect on it.
     * @param source A database pointer we'll keep, to create new connections when {@link #features(boolean) fetching data}.
     * @param conn Serves for compiling user query, thus creating output data type. The connection is not kept nor
     *             re-used after constructor, so you can close it immediately after. We require it, so we do not force
     *             opening a new connection if user already has one ready on its end.
     * @throws SQLException If input query compiling or analysis of its metadata fails.
     */
    public QueryFeatureSet(SQLBuilder queryBuilder, DataSource source, Connection conn) throws SQLException {
        this(queryBuilder, new Analyzer(source, conn, null, null), source, conn);
    }


    /**
     * See {@link #QueryFeatureSet(SQLBuilder, DataSource, Connection)} for details.
     *
     * @param analyzer SIS sql analyzer, used for query metadata analysis. Not nullable. If you do not have any, you
     *                 can use {@link #QueryFeatureSet(SQLBuilder, DataSource, Connection) another constructor}.
     */
    QueryFeatureSet(SQLBuilder queryBuilder, Analyzer analyzer, DataSource source, Connection conn) throws SQLException {
        this(queryBuilder, createAdapter(queryBuilder, analyzer, conn), analyzer.listeners, source);
    }

    QueryFeatureSet(SQLBuilder queryBuilder, FeatureAdapter adapter, StoreListeners listeners, DataSource source) {
        super(listeners);
        this.source = source;
        this.adapter = adapter;

        /* We will now try to parse offset and limit from input query. If we encounter unsupported/ambiguous case,
         * we will fallback to pure java management of additional limit and offset.
         * If we successfully retrieve offset and limit, we'll modify user query to take account of additional
         * parameters given later.
         */
        String sql = queryBuilder.toString();
        long tmpOffset = 0, tmpLimit = 0;
        try {
            Matcher matcher = OFFSET_PATTERN.matcher(sql);
            if (matcher.find()) tmpOffset = Long.parseLong(matcher.group(1));
            if (matcher.find()) throw new UnsupportedOperationException("More than one offset in the query.");
            sql = matcher.replaceFirst("");

            matcher = LIMIT_PATTERN.matcher(sql);
            if (matcher.find()) tmpLimit = Long.parseLong(matcher.group(1));
            if (matcher.find()) throw new UnsupportedOperationException("More than one limit in the query.");
            sql = matcher.replaceFirst("");
        } catch (RuntimeException e) {
            sql = source.toString();
            tmpOffset = -1;
            tmpLimit = -1;
        }

        distinct = DISTINCT_PATTERN.matcher(sql).find();

        originOffset = tmpOffset;
        originLimit = tmpLimit;

        // Defensive copy
        this.queryBuilder = new SQLBuilder(queryBuilder);
        this.queryBuilder.append(sql);
    }

    @Override
    public FeatureType getType() {
        return adapter.type;
    }

    @Override
    public Stream<Feature> features(boolean parallel) {
        return new StreamSQL(new QueryAdapter(queryBuilder, parallel), source, parallel);
    }

    @Override
    public FeatureSet subset(Query query) throws UnsupportedQueryException, DataStoreException {
        if (query instanceof SimpleQuery) {
            final org.apache.sis.internal.sql.feature.SubsetAdapter subsetAdapter = new org.apache.sis.internal.sql.feature.SubsetAdapter(fs -> new SubsetAdapter());
            return subsetAdapter.subset(this, (SimpleQuery) query);
        }

        return super.subset(query);
    }

    class SubsetAdapter extends SQLQueryAdapter {

        SubsetAdapter() {
            super(queryBuilder.dialect);
        }

        @Override
        protected FeatureSet create(CharSequence where, SortProperty[] sorting, ColumnRef[] columns) {
            // TODO: use columns.
            final SQLBuilder newQuery = amendQuery(where, sorting);
            return new QueryFeatureSet(newQuery, adapter, null, source);
        }
    }

    private SQLBuilder amendQuery(CharSequence where, SortProperty[] sorting) {
        // As we do not know user query complexity, what we'll do is make a query wrapper to ensure we won't break the
        // original query. Note that it will surely be less performant, though.
        final SQLBuilder newBuilder = new SQLBuilder(queryBuilder);
        newBuilder.append("SELECT * FROM (")
                .append(queryBuilder.toString())
                .append(')')
                .append(" AS ORIGIN_QUERY");
        if (where != null && where.length() > 0) newBuilder.append(" WHERE ").append(where.toString());
        Features.appendOrderBy(newBuilder, sorting);
        return newBuilder.append(";");
    }

    private final class QueryAdapter implements StreamSQL.QueryBuilder {

        private final SQLBuilder source;
        private final boolean parallel;

        private long additionalOffset, additionalLimit;

        QueryAdapter(SQLBuilder source, boolean parallel) {
            // defensive copy
            this.source = new SQLBuilder(source);
            this.source.append(source.toString());
            this.parallel = parallel;
        }

        @Override
        public StreamSQL.QueryBuilder limit(long limit) {
            additionalLimit = limit;
            return this;
        }

        @Override
        public StreamSQL.QueryBuilder offset(long offset) {
            additionalOffset = offset;
            return this;
        }

        @Override
        public StreamSQL.QueryBuilder distinct(boolean activate) {
            if (distinct == activate) return this;
            throw new UnsupportedOperationException("Not supported yet: modifying user query"); // "Alexis Manin (Geomatys)" on 24/09/2019
        }

        @Override
        public Connector select(ColumnRef... columns) {
            if (columns == null || columns.length < 1) {
                long javaOffset = 0, nativeOffset = 0, javaLimit = 0, nativeLimit = 0;
                if (originOffset < 0) {
                    javaOffset = this.additionalOffset;
                } else if (originOffset > 0 || additionalOffset > 0) {
                    nativeOffset = originOffset + additionalOffset;
                }

                if (originLimit <= 0) {
                    javaLimit = this.additionalLimit;
                } else if (originLimit > 0 || additionalLimit > 0) {
                    nativeLimit = Math.min(originLimit, additionalLimit);
                }

                Features.addOffsetLimit(source, nativeOffset, nativeLimit);
                return new PreparedQueryConnector(source.toString(), javaOffset, javaLimit, parallel);
            }
            throw new UnsupportedOperationException("Not supported yet: modifying user query"); // "Alexis Manin (Geomatys)" on 24/09/2019
        }
    }

    private static FeatureAdapter createAdapter(SQLBuilder queryBuilder, Analyzer analyzer, Connection conn) throws SQLException {
        String sql = queryBuilder.toString();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            final SQLTypeSpecification spec = analyzer.create(statement, sql, null);
            return analyzer.buildAdapter(spec);
        }
    }

    private final class PreparedQueryConnector implements Connector {

        final String sql;
        /**
         * In some cases, detection/modification of SQL offset and limit parameters can fail. In such cases, we amend
         * result stream with pure java {@link Stream#skip(long) offset} and {@link Stream#limit(long) limit}.
         */
        private long additionalOffset, additionalLimit;
        private final boolean parallel;

        private PreparedQueryConnector(String sql, long additionalOffset, long additionalLimit, boolean parallel) {
            this.sql = sql.replaceFirst(";\\s*$", "");
            this.additionalOffset = additionalOffset;
            this.additionalLimit = additionalLimit;
            this.parallel = parallel;
        }

        @Override
        public Stream<Feature> connect(Connection connection) throws SQLException, DataStoreException {
            final PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            statement.setFetchSize(fetchSize);
            final ResultSet result = statement.executeQuery();
            final int fetchSize = result.getFetchSize();
            final boolean withPrefetch = !allowBatchLoading || (fetchSize < 1 || fetchSize >= Integer.MAX_VALUE);
            final Spliterator<Feature> spliterator = withPrefetch ?
                    new ResultSpliterator(result, connection) : new PrefetchSpliterator(result, connection, fetchRatio);
            Stream<Feature> stream = StreamSupport.stream(spliterator, parallel && withPrefetch);
            if (additionalLimit > 0) stream = stream.limit(additionalLimit);
            if (additionalOffset > 0) stream = stream.skip(additionalOffset);

            return stream.onClose(() -> {
                try (
                        AutoCloseable rc = result::close;
                        AutoCloseable sc = statement::close;
                ) {
                    // No-op. Using try with resource allows to manage closing of second resource even if first one throws an error.
                } catch (Exception e) {
                    QueryFeatureSet.this.warning(e);
                }
            });
        }

        @Override
        public String estimateStatement(boolean count) {
            if (count) {
                return "SELECT COUNT(*) FROM ("+sql+") AS count_all";
            } else {
                return sql;
            }
        }
    }

    /**
     * Base class for loading SQL query result loading through {@link Spliterator} API. Concrete implementations comes
     * in two experimental flavors :
     * <ul>
     *     <li>Sequential streaming: {@link ResultSpliterator}</li>
     *     <li>Parallelizable batch  streaming: {@link PrefetchSpliterator}</li>
     * </ul>
     *
     * A benchmark is available in the test directory to compare both implementations, which could be useful in
     * the future to determine which implementation to priorize. For now results does not show much difference.
     * The benchmark class is {@code QuerySpliteratorsBench}.
     */
    private abstract class QuerySpliterator  implements java.util.Spliterator<Feature> {

        final ResultSet result;
        final FeatureAdapter.ResultSetAdapter adapter;

        private QuerySpliterator(ResultSet result, Connection origin) {
            this.result = result;
            this.adapter = QueryFeatureSet.this.adapter.prepare(origin);
        }

        @Override
        public long estimateSize() {
            return originLimit > 0 ? originLimit : Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            // TODO: determine if it's ordered by analysing user query. SIZED is not possible, as limit is an upper threshold.
            return Spliterator.IMMUTABLE | Spliterator.NONNULL | (distinct ? Spliterator.DISTINCT : 0);
        }
    }

    private final class ResultSpliterator extends QuerySpliterator {

        private ResultSpliterator(ResultSet result, Connection origin) {
            super(result, origin);
        }

        @Override
        public boolean tryAdvance(Consumer<? super Feature> action) {
            try {
                if (result.next()) {
                    final Feature f = adapter.read(result);
                    action.accept(f);
                    return true;
                } else return false;
            } catch (SQLException e) {
                throw new BackingStoreException("Cannot advance in SQL query result", e);
            }
        }

        @Override
        public Spliterator<Feature> trySplit() {
            return null;
        }
    }

    private static SQLBuilder fromQuery(final String query, final Connection conn) throws SQLException {
        return new SQLBuilder(conn.getMetaData(), true)
                .append(query);
    }

    /**
     * An attempt to optimize feature loading through batching and potential parallelization. For now, it looks like
     * there's not much improvement regarding to naive streaming approach. IMHO, two improvements would really impact
     * performance positively if done:
     * <ul>
     *     <li>Optimisation of batch loading through {@link FeatureAdapter.ResultSetAdapter#prefetch(int, ResultSet)}</li>
     *     <li>Better splitting balance, as stated by {@link Spliterator#trySplit()}</li>
     * </ul>
     */
    private final class PrefetchSpliterator extends QuerySpliterator {

        final int fetchSize;

        int idx;
        List<Feature> chunk;

        /**
         * According to {@link Spliterator#trySplit()} documentation, the original size estimation must be reduced after
         * split to remain consistent.
         */
        long splittedAmount;

        private PrefetchSpliterator(ResultSet result, Connection origin) throws SQLException {
            this(result, origin, 0.5f);
        }

        private PrefetchSpliterator(ResultSet result, Connection origin, float fetchRatio) throws SQLException {
            super(result, origin);
            this.fetchSize = Math.max((int) (result.getFetchSize()*fetchRatio), 1);
        }

        @Override
        public boolean tryAdvance(Consumer<? super Feature> action) {
            if (ensureChunkAvailable()) {
                action.accept(chunk.get(idx++));
                return true;
            }
            return false;
        }

        public Spliterator<Feature> trySplit() {
            if (!ensureChunkAvailable()) return null;
            final List<Feature> remainingChunk = chunk.subList(idx, chunk.size());
            splittedAmount += remainingChunk.size();
            final Spliterator<Feature> chunkSpliterator = idx == 0 ?
                    chunk.spliterator() : remainingChunk.spliterator();
            chunk = null;
            idx = 0;
            return chunkSpliterator;
        }

        @Override
        public long estimateSize() {
            return super.estimateSize() - splittedAmount;
        }

        @Override
        public int characteristics() {
            return super.characteristics() | Spliterator.CONCURRENT;
        }

        private boolean ensureChunkAvailable() {
            if (chunk == null || idx >= chunk.size()) {
                idx = 0;
                try {
                    chunk = adapter.prefetch(fetchSize, result);
                } catch (SQLException e) {
                    throw new BackingStoreException(e);
                }
                return chunk != null && !chunk.isEmpty();
            }
            return true;
        }
    }
}
