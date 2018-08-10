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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.lang.reflect.Array;
import org.apache.sis.internal.metadata.sql.SQLBuilder;
import org.apache.sis.storage.InternalDataStoreException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.util.ArraysExt;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * Iterator over feature instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Features implements Spliterator<Feature>, Runnable {
    /**
     * An empty array of iterators, used when there is no dependency.
     */
    private static final Features[] EMPTY = new Features[0];

    /**
     * The type of features to create.
     */
    private final FeatureType featureType;

    /**
     * Name of attributes in feature instances, excluding operations and associations to other tables.
     * Those names are in the order of columns declared in the {@code SELECT <columns} statement.
     * This array is a shared instance and shall not be modified.
     */
    private final String[] attributeNames;

    /**
     * Name of the properties where are stored associations in feature instances.
     * The length of this array shall be equal to the {@link #dependencies} array length.
     * Imported or exported features read by {@code dependencies[i]} will be stored in
     * the association named {@code associationNames[i]}.
     */
    private final String[] associationNames;

    /**
     * Name of the property where to store the association that we can not handle with other {@link #dependencies}.
     * This deferred association may exist because of circular dependency.
     */
    private final String deferredAssociation;

    /**
     * The feature sets referenced through foreigner keys, or {@link #EMPTY} if none.
     * This includes the associations inferred from both the imported and exported keys.
     * The first {@link #importCount} iterators are for imported keys, and the remaining
     * iterators are for the exported keys.
     */
    private final Features[] dependencies;

    /**
     * Number of entries in {@link #dependencies} for {@link Relation.Direction#IMPORT}.
     * The entries immediately following the first {@code importCount} entries are for
     * {@link Relation.Direction#EXPORT}.
     */
    private final int importCount;

    /**
     * One-based indices of the columns to query for each {@link #dependencies} entry.
     */
    private final int[][] foreignerKeyIndices;

    /**
     * If this iterator returns only the feature matching some condition (typically a primary key value),
     * the statement for performing that filtering. Otherwise if this iterator returns all features, then
     * this field is {@code null}.
     */
    private final PreparedStatement statement;

    /**
     * The result of executing the SQL query for a {@link Table}. If {@link #statement} is null,
     * then a single {@code ResultSet} is used for all the lifetime of this {@code Features} instance.
     * Otherwise an arbitrary amount of {@code ResultSet}s may be created from the statement.
     */
    private ResultSet result;

    /**
     * Feature instances already created, or {@code null} if the features created by this iterator are not cached.
     * This map is used when requesting a feature by identifier, not when iterating over all features (note: we
     * could perform an opportunistic check in a future SIS version). The same map may be shared by all iterators
     * on the same {@link Table}, but {@link WeakValueHashMap} already provides the required synchronizations.
     *
     * <p>This {@code Features} class does not require the identifiers to be built from primary key columns.
     * However if this map has been provided by {@link Table#instanceForPrimaryKeys()}, then the identifiers
     * need to be primary keys with columns in the exact same order for allowing the same map to be shared.</p>
     */
    private final WeakValueHashMap<?,Object> instances;

    /**
     * The component class of the keys in the {@link #instances} map, or {@code null} if the keys are not array.
     * For example if a primary key is made of two columns of type {@code String}, then this field may be set to
     * {@code String}.
     */
    private final Class<?> keyComponentClass;

    /**
     * Estimated number of rows, or {@literal <= 0} if unknown.
     */
    private final long estimatedSize;

    /**
     * Creates a new iterator over the feature instances.
     *
     * @param table             the table for which we are creating an iterator.
     * @param connection        connection to the database.
     * @param attributeNames    value of {@link Table#attributeNames}:   where to store simple values.
     * @param attributeColumns  value of {@link Table#attributeColumns}: often the same as attribute names.
     * @param importedKeys      value of {@link Table#importedKeys}:     targets of this table foreign keys.
     * @param exportedKeys      value of {@link Table#exportedKeys}:     foreigner keys of other tables.
     * @param following         the relations that we are following. Used for avoiding never ending loop.
     * @param noFollow          relation to not follow, or {@code null} if none.
     */
    Features(final Table table, final Connection connection, final String[] attributeNames, final String[] attributeColumns,
             final Relation[] importedKeys, final Relation[] exportedKeys, final List<Relation> following, final Relation noFollow)
             throws SQLException, InternalDataStoreException
    {
        this.featureType = table.featureType;
        this.attributeNames = attributeNames;
        final DatabaseMetaData metadata = connection.getMetaData();
        estimatedSize = following.isEmpty() ? table.countRows(metadata, true) : 0;
        final SQLBuilder sql = new SQLBuilder(metadata, true).append("SELECT");
        final Map<String,Integer> columnIndices = new HashMap<>();
        /*
         * Create a SELECT clause with all columns that are ordinary attributes.
         * Order matter, since 'Features' iterator will map the columns to the
         * attributes listed in the 'attributeNames' array in that order.
         */
        for (String column : attributeColumns) {
            appendColumn(sql, column, columnIndices);
        }
        /*
         * Collect information about associations in local arrays before to assign
         * them to the final fields, because some array lengths may be adjusted.
         */
        int importCount = (importedKeys != null) ? importedKeys.length : 0;
        int exportCount = (exportedKeys != null) ? exportedKeys.length : 0;
        int totalCount  = importCount + exportCount;
        if (totalCount == 0) {
            dependencies        = EMPTY;
            associationNames    = null;
            foreignerKeyIndices = null;
            deferredAssociation = null;
        } else {
            String deferredAssociation = null;
            final Features[]     dependencies = new Features[totalCount];
            final String[]   associationNames = new String  [totalCount];
            final int[][] foreignerKeyIndices = new int     [totalCount][];
            /*
             * For each foreigner key to another table, append all columns of that foreigner key
             * and the name of the single feature property where the association will be stored.
             */
            if (importCount != 0) {
                importCount = 0;                                                    // We will recount.
                for (final Relation dependency : importedKeys) {
                    if (dependency != noFollow) {
                        dependency.startFollowing(following);                       // Safety against never-ending recursivity.
                        associationNames   [importCount] = dependency.propertyName;
                        foreignerKeyIndices[importCount] = getColumnIndices(sql, dependency.getForeignerKeys(), columnIndices);
                        dependencies       [importCount] = dependency.getSearchTable().features(connection, following, noFollow);
                        dependency.endFollowing(following);
                        importCount++;
                    } else {
                        deferredAssociation = dependency.propertyName;
                    }
                }
            }
            /*
             * Create iterators for other tables that reference the primary keys of this table. For example
             * if we have a "City" feature with attributes for the city name, population, etc. and a "Parks"
             * feature referencing the city where the park is located, in order to populate the "City.parks"
             * associations we need to iterate over all "Parks" rows referencing the city.
             */
            if (exportCount != 0) {
                int i = importCount;
                for (final Relation dependency : exportedKeys) {
                    dependency.startFollowing(following);                   // Safety against never-ending recursivity.
                    final Table foreigner  = dependency.getSearchTable();
                    final Relation inverse = foreigner.getInverseOf(dependency, table.name);
                    associationNames   [i] = dependency.propertyName;
                    foreignerKeyIndices[i] = getColumnIndices(sql, dependency.getForeignerKeys(), columnIndices);
                    dependencies       [i] = foreigner.features(connection, following, inverse);
                    dependency.endFollowing(following);
                    i++;
                }
            }
            totalCount = importCount + exportCount;
            this.dependencies        = ArraysExt.resize(dependencies,        totalCount);
            this.associationNames    = ArraysExt.resize(associationNames,    totalCount);
            this.foreignerKeyIndices = ArraysExt.resize(foreignerKeyIndices, totalCount);
            this.deferredAssociation = deferredAssociation;
        }
        this.importCount = importCount;
        /*
         * Create a Statement if we don't need any condition, or a PreparedStatement if we need to add
         * a "WHERE" clause. In the later case, we will cache the features already created if there is
         * a possibility that many rows reference the same feature instance.
         */
        sql.append(" FROM ").appendIdentifier(table.name.catalog, table.name.schema, table.name.table);
        if (following.isEmpty()) {
            statement = null;
            instances = null;       // A future SIS version could use the map opportunistically if it exists.
            keyComponentClass = null;
            result = connection.createStatement().executeQuery(sql.toString());
        } else {
            final Relation componentOf = following.get(following.size() - 1);
            String separator = " WHERE ";
            for (String primaryKey : componentOf.getSearchColumns()) {
                sql.append(separator).appendIdentifier(primaryKey).append("=?");
                separator = " AND ";
            }
            statement = connection.prepareStatement(sql.toString());
            /*
             * Following assumes that the foreigner key references the primary key of this table,
             * in which case 'table.primaryKeyClass' should never be null. This assumption may not
             * hold if the relation has been defined by DatabaseMetaData.getCrossReference(…) instead.
             */
            if (componentOf.useFullKey()) {
                instances = table.instanceForPrimaryKeys();
                keyComponentClass = table.primaryKeyClass.getComponentType();
            } else {
                instances = new WeakValueHashMap<>(Object.class);       // Can not share the table cache.
                keyComponentClass = Object.class;
            }
        }
    }

    /**
     * Appends a columns in the given builder and remember the column indices.
     * An exception is thrown if the column has already been added (should never happen).
     */
    private static int appendColumn(final SQLBuilder sql, final String column,
            final Map<String,Integer> columnIndices) throws InternalDataStoreException
    {
        int columnCount = columnIndices.size();
        if (columnCount != 0) sql.append(',');
        sql.append(' ').appendIdentifier(column);
        if (columnIndices.put(column, ++columnCount) == null) return columnCount;
        throw new InternalDataStoreException(Resources.format(Resources.Keys.DuplicatedColumn_1, column));
    }

    /**
     * Computes the 1-based indices of given columns, adding the columns in the given builder if necessary.
     */
    private static int[] getColumnIndices(final SQLBuilder sql, final Collection<String> columns,
            final Map<String,Integer> columnIndices) throws InternalDataStoreException
    {
        int i = 0;
        final int[] indices = new int[columns.size()];
        for (final String column : columns) {
            final Integer pos = columnIndices.get(column);
            indices[i++] = (pos != null) ? pos : appendColumn(sql, column, columnIndices);
        }
        return indices;
    }

    /**
     * Returns an array of the given length capable to hold the identifier,
     * or {@code null} if there is no need for an array.
     */
    private Object identifierArray(final int columnCount) {
        return (columnCount > 1) ? Array.newInstance(keyComponentClass, columnCount) : null;
    }

    /**
     * Declares that this iterator never returns {@code null} elements.
     */
    @Override
    public int characteristics() {
        return NONNULL;
    }

    /**
     * Returns the estimated number of features, or {@link Long#MAX_VALUE} if unknown.
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
    public Spliterator<Feature> trySplit() {
        return null;
    }

    /**
     * Gives the next feature to the given consumer.
     */
    @Override
    public boolean tryAdvance(final Consumer<? super Feature> action) {
        try {
            return fetch(action, false);
        } catch (SQLException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Gives all remaining features to the given consumer.
     */
    @Override
    public void forEachRemaining(final Consumer<? super Feature> action) {
        try {
            fetch(action, true);
        } catch (SQLException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Gives at least the next feature to the given consumer.
     * Gives all remaining features if {@code all} is {@code true}.
     *
     * @param  action  the action to execute for each {@link Feature} instances fetched by this method.
     * @param  all     {@code true} for reading all remaining feature instances, or {@code false} for only the next one.
     * @return {@code true} if we have read an instance and {@code all} is {@code false} (so there is maybe other instances).
     */
    private boolean fetch(final Consumer<? super Feature> action, final boolean all) throws SQLException {
        while (result.next()) {
            final Feature feature = featureType.newInstance();
            for (int i=0; i < attributeNames.length; i++) {
                final Object value = result.getObject(i+1);
                if (!result.wasNull()) {
                    feature.setPropertyValue(attributeNames[i], value);
                }
            }
            for (int i=0; i < dependencies.length; i++) {
                final Features dependency = dependencies[i];
                final int[] columnIndices = foreignerKeyIndices[i];
                final Object value;
                if (i < importCount) {
                    /*
                     * Relation.Direction.IMPORT: this table contains the foreigner keys.
                     *
                     * If the foreigner key uses only one column, we will store the foreigner key value
                     * in the 'key' variable without creating array. But if the foreigner key uses more
                     * than one column, then we need to create an array holding all values.
                     */
                    Object key = null;
                    final Object keys = dependency.identifierArray(columnIndices.length);
                    for (int p=0; p < columnIndices.length;) {
                        key = result.getObject(columnIndices[p]);
                        if (keys != null) Array.set(keys, p, key);
                        dependency.statement.setObject(++p, key);
                    }
                    if (keys != null) key = keys;
                    value = dependency.fetchReferenced(key, null);
                } else {
                    /*
                     * Relation.Direction.EXPORT: another table references this table.
                     *
                     * 'key' must stay null because we do not cache those dependencies.
                     * The reason is that this direction can return a lot of instances,
                     * contrarily to Direction.IMPORT which return only one instance.
                     * Furthermore instances fetched from Direction.EXPORT can not be
                     * shared by feature instances, so caching would be useless here.
                     */
                    for (int p=0; p < columnIndices.length;) {
                        final Object k = result.getObject(columnIndices[p]);
                        dependency.statement.setObject(++p, k);
                    }
                    value = dependency.fetchReferenced(null, feature);
                }
                feature.setPropertyValue(associationNames[i], value);
            }
            action.accept(feature);
            if (!all) return true;
        }
        return false;
    }

    /**
     * Executes the current {@link #statement} and stores all features in a list.
     * Returns {@code null} if there is no feature, or returns the feature instance
     * if there is only one such instance, or returns a list of features otherwise.
     *
     * @param  key    the key to use for referencing the feature in the cache, or {@code null} for no caching.
     * @param  owner  if the features to fetch are components of another feature, that container feature instance.
     * @return the feature as a singleton {@code Feature} or as a {@code Collection<Feature>}.
     */
    private Object fetchReferenced(final Object key, final Feature owner) throws SQLException {
        if (key != null) {
            Object existing = instances.get(key);
            if (existing != null) {
                return existing;
            }
        }
        final List<Feature> features = new ArrayList<>();
        try (ResultSet r = statement.executeQuery()) {
            result = r;
            fetch(features::add, true);
        } finally {
            result = null;
        }
        if (owner != null && deferredAssociation != null) {
            for (final Feature feature : features) {
                feature.setPropertyValue(deferredAssociation, owner);
            }
        }
        Object feature;
        switch (features.size()) {
            case 0:  feature = null; break;
            case 1:  feature = features.get(0); break;
            default: feature = features; break;
        }
        if (key != null) {
            @SuppressWarnings("unchecked")          // Check is performed by putIfAbsent(…).
            final Object previous = ((WeakValueHashMap) instances).putIfAbsent(key, feature);
            if (previous != null) {
                feature = previous;
            }
        }
        return feature;
    }

    /**
     * Closes the (pooled) connection, including the statements of all dependencies.
     */
    private void close() throws SQLException {
        /*
         * Only one of 'statement' and 'result' should be non-null. The connection should be closed
         * by the 'Features' instance having a non-null 'result' because it is the main one created
         * by 'Table.features(boolean)' method. The other 'Features' instances are dependencies.
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
                for (final Features dependency : dependencies) {
                    dependency.close();
                }
            }
        }
    }

    /**
     * Closes the (pooled) connection, including the statements of all dependencies.
     * This is a handler to be invoked by {@link java.util.stream.Stream#close()}.
     */
    @Override
    public void run() {
        try {
            close();
        } catch (SQLException e) {
            throw new BackingStoreException(e);
        }
    }
}
