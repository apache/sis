package org.apache.sis.internal.sql.feature;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.BackingStoreException;

/**
 * Stores SQL query given at built time, and execute it when calling {@link #features(boolean) data stream}. Note that
 * {@link #getType() data type} is defined by analyzing sql statement metadata. Note that user query can be modified at
 * before execution to adapt various parameters overridable at fetch time as offset and limit through
 * {@link Stream#skip(long)} and {@link Stream#limit(long)}.
 *
 * Note that this component models query result as close as possible, so built data type will be simple feature type (no
 * association).
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
        this(queryBuilder, new Analyzer(source, conn.getMetaData(), null, null), source, conn);
    }


    /**
     * See {@link #QueryFeatureSet(SQLBuilder, DataSource, Connection)} for details.
     *
     * @param analyzer SIS sql analyzer, used for query metadata analysis. Not nullable. If you do not have any, you
     *                 can use {@link #QueryFeatureSet(SQLBuilder, DataSource, Connection) another constructor}.
     */
    QueryFeatureSet(SQLBuilder queryBuilder, Analyzer analyzer, DataSource source, Connection conn) throws SQLException {
        super(analyzer.listeners);
        this.source = source;

        String sql = queryBuilder.toString();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            final SQLTypeSpecification spec = analyzer.create(statement, sql, null);
            adapter = analyzer.buildAdapter(spec);
        }

        /* We will now try to parse offset and limit from input query. If we encounter unsupported/ambiguous case,
         * we will fallback to pure java management of additional limit and offset.
         * If we successfully retrieve offset and limit, we'll modify user query to take account of additional
         * parameters given later.
         */
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

    /**
     * Acquire a connection over parent database, forcing a few parameters to ensure optimal read performance and
     * limiting user rights :
     * <ul>
     *     <li>{@link Connection#setAutoCommit(boolean) auto-commit} to false</li>
     *     <li>{@link Connection#setReadOnly(boolean) querying read-only}</li>
     * </ul>
     *
     * @param source Database pointer to create connection from.
     * @return A new connection to database, with deactivated auto-commit.
     * @throws SQLException If we cannot create a new connection. See {@link DataSource#getConnection()} for details.
     */
    public static Connection connectReadOnly(final DataSource source) throws SQLException {
        final Connection c = source.getConnection();
        try {
            c.setAutoCommit(false);
            c.setReadOnly(true);
        } catch (SQLException e) {
            try {
                c.close();
            } catch (RuntimeException | SQLException bis) {
                e.addSuppressed(bis);
            }
            throw e;
        }
        return c;
    }

    @Override
    public FeatureType getType() {
        return adapter.type;
    }

    @Override
    public Stream<Feature> features(boolean parallel) {
        return new StreamSQL(new QueryAdapter(queryBuilder), source);
    }

    private final class QueryAdapter implements QueryBuilder {

        private final SQLBuilder source;

        private long additionalOffset, additionalLimit;

        QueryAdapter(SQLBuilder source) {
            // defensive copy
            this.source = new SQLBuilder(source);
            this.source.append(source.toString());
        }

        @Override
        public QueryBuilder limit(long limit) {
            additionalLimit = limit;
            return this;
        }

        @Override
        public QueryBuilder offset(long offset) {
            additionalOffset = offset;
            return this;
        }

        @Override
        public QueryBuilder distinct(boolean activate) {
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

                if (originLimit < 0) {
                    javaLimit = this.additionalLimit;
                } else if (originLimit > 0 || additionalLimit > 0) {
                    nativeLimit = Math.min(originLimit, additionalLimit);
                }

                Features.addOffsetLimit(source, nativeOffset, nativeLimit);
                return new PreparedQueryConnector(source.toString(), javaOffset, javaLimit);
            }
            throw new UnsupportedOperationException("Not supported yet: modifying user query"); // "Alexis Manin (Geomatys)" on 24/09/2019
        }
    }

    private final class PreparedQueryConnector implements Connector {

        final String sql;
        private long additionalOffset, additionalLimit;

        private PreparedQueryConnector(String sql, long additionalOffset, long additionalLimit) {
            this.sql = sql;
            this.additionalOffset = additionalOffset;
            this.additionalLimit = additionalLimit;
        }

        @Override
        public Stream<Feature> connect(Connection connection) throws SQLException, DataStoreException {
            final PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            final ResultSet result = statement.executeQuery();

            Stream<Feature> stream = StreamSupport.stream(new ResultSpliterator(result), false);
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
            throw new UnsupportedOperationException("Not supported yet"); // "Alexis Manin (Geomatys)" on 24/09/2019
        }
    }

    private final class ResultSpliterator implements Spliterator<Feature> {

        final ResultSet result;

        private ResultSpliterator(ResultSet result) {
            this.result = result;
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

        @Override
        public long estimateSize() {
            // TODO: economic size estimation ? A count query seems overkill for the aim of this API. Howver, we could
            // analyze user query in search for a limit value.
            return originLimit > 0 ? originLimit : Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            // TODO: determine if it's sorted by analysing user query. SIZED is not possible, as limit is an upper threshold.
            return Spliterator.IMMUTABLE | Spliterator.NONNULL;
        }
    }

    private static SQLBuilder fromQuery(final String query, final Connection conn) throws SQLException {
        return new SQLBuilder(conn.getMetaData(), true)
                .append(query);
    }
}
