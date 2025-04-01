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

import java.util.ArrayList;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.sis.metadata.sql.privy.SQLBuilder;
import org.apache.sis.storage.base.FeatureProjection;
import org.apache.sis.util.collection.WeakValueHashMap;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.pending.geoapi.filter.SortOrder;
import org.apache.sis.pending.geoapi.filter.SortProperty;
import org.apache.sis.pending.geoapi.filter.SortBy;


/**
 * Iterator over feature instances.
 * This iterator converters {@link ResultSet} rows to {@code Feature} instances.
 * Each {@code FeatureIterator} iterator is created for one specific SQL query
 * and can be used for only one iteration.
 *
 * <h2>Parallelism</h2>
 * Current implementation of {@code FeatureIterator} does not support parallelism.
 * This iterator is not thread-safe and the {@link #trySplit()} method always returns {@code null}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
final class FeatureIterator implements Spliterator<AbstractFeature>, AutoCloseable {
    /**
     * Characteristics of the iterator. The value returned by {@link #characteristics()}
     * must be consistent with the value given to {@code DeferredStream} constructor.
     *
     * @see #characteristics()
     */
    static final int CHARACTERISTICS = NONNULL;

    /**
     * The converter from a {@link ResultSet} row to a {@code Feature} instance.
     */
    private final FeatureAdapter adapter;

    /**
     * If this iterator returns only the features matching some condition (typically a primary key value),
     * the statement for performing that filtering. Otherwise if this iterator returns all features, then
     * this field is {@code null}.
     */
    private final PreparedStatement statement;

    /**
     * The result of executing the SQL query for a {@link Table}. If {@link #statement} is null, then
     * a single {@code ResultSet} is used for all the lifetime of this {@code FeatureIterator} instance.
     * Otherwise an arbitrary number of {@code ResultSet}s may be created from the statement.
     */
    private ResultSet result;

    /**
     * Estimated number of remaining rows, or ≤ 0 if unknown.
     * Zero is considered unknown (instead of only negative values)
     * because some databases return 0 when they cannot count.
     *
     * @see Table#countRows(DatabaseMetaData, boolean, boolean)
     */
    private final long estimatedSize;

    /**
     * A cache of statements for fetching spatial information such as geometry columns or SRID.
     * This is non-null only if the {@linkplain Database#getSpatialSchema() database is spatial}.
     * The same instance is shared by all dependencies of this {@code FeatureIterator}.
     */
    private final InfoStatements spatialInformation;

    /**
     * The feature sets referenced through foreigner keys, or an empty array if none.
     * This includes the associations inferred from both the imported and exported keys.
     * The first {@link FeatureAdapter#importCount} iterators are for imported keys,
     * and the remaining iterators are for the exported keys.
     *
     * <p>All elements in this array are initially null. Iterators are created when first needed.
     * They may be never created because those features may be in the cache.</p>
     */
    private final FeatureIterator[] dependencies;

    /**
     * Additional properties to compute from the main properties, or {@code null} if none.
     * This is usually {@code null}. It may be non-null if the user specified a query with
     * operations other than {@code ValueReference} or {@code Literal}.
     *
     * <h4>Completion</h4>
     * Usually, the expressions are executed on a source feature instance and the results
     * are copied in a target feature instance. However, if {@link #completion} is true,
     * then the source and target features are the same instance. The completion mode is
     * used when the target feature have all information needed for computing the query
     * expressions, so that the projection is only a completion.
     */
    private final FeatureProjection projection;

    /**
     * Whether the {@linkplain #projection} should be applied on the same
     */
    private final boolean completion;

    /**
     * Creates a new iterator over features.
     *
     * @param table       the source table.
     * @param connection  connection to the database, used for creating the statement.
     * @param distinct    whether the set should contain distinct feature instances.
     * @param selection   condition to append, not including the {@code WHERE} keyword.
     * @param sort        the {@code ORDER BY} clauses, or {@code null} if none.
     * @param offset      number of rows to skip in underlying SQL query, or ≤ 0 for none.
     * @param count       maximum number of rows to return, or ≤ 0 for no limit.
     * @param projection  additional properties to compute, or {@code null} if none.
     */
    FeatureIterator(final Table           table,
                    final Connection      connection,
                    final boolean         distinct,
                    final SelectionClause selection,
                    final SortBy<? super AbstractFeature> sort,
                    final long offset,
                    final long count,
                    final FeatureProjection projection)
            throws Exception
    {
        adapter = table.adapter(connection);
        String sql = adapter.sql;   // Will be completed below with `WHERE` clause if needed.

        if (table.database.getSpatialSchema().isPresent()) {
            spatialInformation = table.database.createInfoStatements(connection);
        } else {
            spatialInformation = null;
        }
        final String filter = (selection != null) ? selection.query(connection, spatialInformation) : null;
        if (distinct || filter != null || sort != null || offset > 0 || count > 0) {
            final var builder = new SQLBuilder(table.database).append(sql);
            if (distinct) {
                builder.insertDistinctAfterSelect();
            }
            if (filter != null) {
                builder.append(" WHERE ").append(filter);
            }
            if (sort != null) {
                String separator = " ORDER BY ";
                for (final SortProperty<? super AbstractFeature> s : sort.getSortProperties()) {
                    builder.append(separator).appendIdentifier(s.getValueReference().getXPath());
                    final SortOrder order = s.getSortOrder();
                    if (order != null) {
                        builder.append(' ').append(order.toSQL());
                    }
                    separator = ", ";
                }
            }
            sql = builder.appendFetchPage(offset, count).toString();
        }
        /*
         * Create the statement for the SQL query. The call to `createStatement()` should be at the end,
         * after the call to `countRows(…)`, because some JDBC drivers close the statement when we ask
         * for metadata (probably a bug, but not all JDBC drivers are mature).
         */
        if (filter == null) {
            estimatedSize = Math.min(table.countRows(connection.getMetaData(), distinct, true), offset + count) - offset;
        } else {
            estimatedSize = 0;              // Cannot estimate the size if there is filtering conditions.
        }
        this.result       = connection.createStatement().executeQuery(sql);
        this.dependencies = new FeatureIterator[adapter.dependencies.length];
        this.completion   = projection != null && projection.featureType == table.featureType;
        this.projection   = projection;
        this.statement    = null;
    }

    /**
     * Creates a new iterator over the dependencies of a feature.
     *
     * @param table       the source table, or {@code null} if we are creating an iterator for a dependency.
     * @param adapter     converter from a {@link ResultSet} row to a {@code Feature} instance.
     * @param connection  connection to the database, used for creating statement.
     * @param filter      condition to append, not including the {@code WHERE} keyword.
     * @param distinct    whether the set should contain distinct feature instances.
     * @param offset      number of rows to skip in underlying SQL query, or ≤ 0 for none.
     * @param count       maximum number of rows to return, or ≤ 0 for no limit.
     */
    private FeatureIterator(final FeatureAdapter adapter, final Connection connection,
                            final InfoStatements spatialInformation) throws SQLException
    {
        this.spatialInformation = spatialInformation;
        this.adapter  = adapter;
        statement     = connection.prepareStatement(adapter.sql);
        dependencies  = new FeatureIterator[adapter.dependencies.length];
        completion    = false;
        projection    = null;
        estimatedSize = 0;
    }

    /**
     * Returns the dependency at the given index, creating it when first needed.
     */
    private FeatureIterator dependency(final int i) throws SQLException {
        FeatureIterator dependency = dependencies[i];
        if (dependency == null) {
            dependency = new FeatureIterator(adapter.dependencies[i], result.getStatement().getConnection(), spatialInformation);
            dependencies[i] = dependency;
        }
        return dependency;
    }

    /**
     * Declares that this iterator never returns {@code null} elements.
     */
    @Override
    public int characteristics() {
        return CHARACTERISTICS;
    }

    /**
     * Returns the estimated number of remaining features, or {@link Long#MAX_VALUE} if unknown.
     */
    @Override
    public long estimateSize() {
        return (estimatedSize > 0) ? estimatedSize : Long.MAX_VALUE;
    }

    /**
     * Current version does not support split.
     *
     * @return always {@code null}.
     */
    @Override
    public Spliterator<AbstractFeature> trySplit() {
        return null;
    }

    /**
     * Gives the next feature to the given consumer.
     */
    @Override
    public boolean tryAdvance(final Consumer<? super AbstractFeature> action) {
        try {
            return fetch(action, false);
        } catch (Exception e) {
            throw FeatureStream.cannotExecute(e);
        }
    }

    /**
     * Gives all remaining features to the given consumer.
     */
    @Override
    public void forEachRemaining(final Consumer<? super AbstractFeature> action) {
        try {
            fetch(action, true);
        } catch (Exception e) {
            throw FeatureStream.cannotExecute(e);
        }
    }

    /**
     * Gives at least the next feature to the given consumer.
     * Gives all remaining features if {@code all} is {@code true}.
     *
     * @param  action  the action to execute for each {@code Feature} instances fetched by this method.
     * @param  all     {@code true} for reading all remaining feature instances, or {@code false} for only the next one.
     * @return {@code true} if we have read an instance and {@code all} is {@code false} (so there is maybe other instances).
     */
    private boolean fetch(final Consumer<? super AbstractFeature> action, final boolean all) throws Exception {
        while (result.next()) {
            AbstractFeature feature = adapter.createFeature(spatialInformation, result);
            for (int i=0; i < dependencies.length; i++) {
                WeakValueHashMap<?,Object> instances = null;
                Object key = null, value = null;
                if (i < adapter.importCount) {
                    /*
                     * Check in the cache only for `Relation.Direction.IMPORT`
                     * (when this table references another table).
                     *
                     * We do not cache dependencies for `Relation.Direction.EXPORT`
                     * (when another table references this table) because that direction can return
                     * a lot of instances, contrarily to `IMPORT` which returns only one instance.
                     * Furthermore, instances fetched from `Direction.EXPORT` cannot be
                     * shared by feature instances, so caching would be useless here.
                     */
                    key = adapter.getCacheKey(result, i);
                    if (key == null) {
                        continue;
                    }
                    instances = adapter.dependencies[i].instances;
                    value = instances.get(key);
                }
                if (value == null) {
                    final FeatureIterator dependency = dependency(i);
                    adapter.setForeignerKeys(result, dependency.statement, i);
                    value = dependency.fetchReferenced(feature);
                }
                if (instances != null) {
                    @SuppressWarnings("unchecked")         // Check is performed by putIfAbsent(…).
                    final Object previous = ((WeakValueHashMap) instances).putIfAbsent(key, value);
                    if (previous != null) value = previous;
                }
                feature.setPropertyValue(adapter.associationNames[i], value);
            }
            /*
             * At this point, we have done everything we could do using SQL statements.
             * Those statements were derived (among others) from expressions that have
             * been recognized as `ValueReference` instances. If the user specified more
             * complex expressions, we need to handle them in Java code.
             */
            if (projection != null) {
                if (completion) {
                    projection.applySelf(feature);
                } else {
                    feature = projection.apply(feature);
                }
            }
            action.accept(feature);
            if (!all) return true;
        }
        return false;
    }

    /**
     * Executes the current {@link #statement} and stores all features in a list.
     * Returns {@code null} if there are no features, or returns the feature instance
     * if there is only one such instance, or returns a list of features otherwise.
     *
     * @param  owner  if the features to fetch are components of another feature, that container feature instance.
     * @return the feature as a singleton {@code Feature} or as a {@code Collection<Feature>}.
     */
    private Object fetchReferenced(final AbstractFeature owner) throws Exception {
        final var features = new ArrayList<AbstractFeature>();
        try (ResultSet r = statement.executeQuery()) {
            result = r;
            fetch(features::add, true);
        } finally {
            result = null;
        }
        if (owner != null && adapter.deferredAssociation != null) {
            for (final AbstractFeature feature : features) {
                feature.setPropertyValue(adapter.deferredAssociation, owner);
            }
        }
        Object feature;
        switch (features.size()) {
            case 0:  feature = null; break;
            case 1:  feature = features.get(0); break;
            default: feature = features; break;
        }
        return feature;
    }

    /**
     * Closes the (pooled) connection, including the statements of all dependencies.
     */
    @Override
    @SuppressWarnings("try")
    public void close() throws SQLException {
        if (spatialInformation != null) {
            spatialInformation.close();
        }
        /*
         * Only one of `statement` and `result` should be non-null. The connection should be closed by
         * the `FeatureIterator` instance having a non-null `result` because it is the main one created
         * by `Table.features(boolean)` method. The other `FeatureIterator` instances are dependencies.
         */
        if (statement != null) {
            statement.close();
        }
        final ResultSet r = result;
        if (r != null) {
            result = null;
            final Statement s = r.getStatement();
            try (Connection c = s.getConnection()) {
                r.close();      // Implied by s.close() according JDBC javadoc, but we are paranoiac.
                s.close();
                for (final FeatureIterator dependency : dependencies) {
                    if (dependency != null) {
                        dependency.close();
                    }
                }
            }
        }
    }
}
