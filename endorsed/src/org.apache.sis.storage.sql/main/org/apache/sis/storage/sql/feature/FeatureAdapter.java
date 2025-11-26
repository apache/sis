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

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.Array;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.sis.metadata.sql.internal.shared.SQLBuilder;
import org.apache.sis.storage.InternalDataStoreException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.WeakValueHashMap;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * Converter of {@link ResultSet} rows to {@link Feature} instances.
 * Each {@code FeatureAdapter} instance is specific to the set of rows given by a <abbr>SQL</abbr> query,
 * ignoring {@code DISTINCT}, {@code ORDER BY} and filter conditions in the {@code WHERE} clause.
 * This class does not hold <abbr>JDBC</abbr> resources. Instead, the {@link ResultSet} is given by the caller.
 * This object can be prepared once and reused every time the query needs to be executed.
 *
 * <h2>Multi-threading</h2>
 * This class is immutable (except for the cache) and safe for concurrent use by many threads.
 * The content of arrays in this class shall not be modified in order to preserve immutability.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
final class FeatureAdapter {
    /**
     * An empty array of adapters, used when there is no dependency.
     */
    private static final FeatureAdapter[] EMPTY = new FeatureAdapter[0];

    /**
     * The type of features to create.
     *
     * @see Table#featureType
     */
    private final FeatureType featureType;

    /**
     * Attributes in feature instances, excluding operations and associations to other tables.
     * Elements are in the order of columns declared in the {@code SELECT <columns>} statement.
     * This array is a shared instance and shall not be modified.
     *
     * @see Table#attributes
     */
    private final Column[] attributes;

    /**
     * Name of the properties where are stored associations in feature instances.
     * The length of this array shall be equal to the {@link #dependencies} array length.
     * Imported or exported features read by {@code dependencies[i]} will be stored in
     * the association named {@code associationNames[i]}.
     */
    final String[] associationNames;

    /**
     * Name of the property where to store the association that we cannot handle with other {@link #dependencies}.
     * This deferred association may exist because of circular dependency.
     */
    final String deferredAssociation;

    /**
     * The feature sets referenced through foreigner keys, or {@link #EMPTY} if none.
     * This includes the associations inferred from both the imported and exported keys.
     * The first {@link #importCount} iterators are for imported keys, and the remaining
     * iterators are for the exported keys.
     */
    final FeatureAdapter[] dependencies;

    /**
     * Number of entries in {@link #dependencies} for {@link Relation.Direction#IMPORT}.
     * The entries immediately following the first {@code importCount} entries are for
     * {@link Relation.Direction#EXPORT}.
     */
    final int importCount;

    /**
     * One-based indices of the columns to query for each {@link #dependencies} entry.
     */
    private final int[][] foreignerKeyIndices;

    /**
     * Feature instances already created, or {@code null} if the features created by this iterator are not cached.
     * This map is used when requesting a feature by identifier, not when iterating over all features (note: we
     * could perform an opportunistic check in a future SIS version). The same map may be shared by all iterators
     * on the same {@link Table}, but {@link WeakValueHashMap} already provides the required synchronizations.
     *
     * <p>The {@link FeatureIterator} class does not require the identifiers to be built from primary key columns.
     * However if this map has been provided by {@link Table#instanceForPrimaryKeys()}, then the identifiers
     * need to be primary keys with columns in the exact same order for allowing the same map to be shared.</p>
     */
    final WeakValueHashMap<?,Object> instances;

    /**
     * The component class of the keys in the {@link #instances} map, or {@code null} if the keys are not array.
     * For example if a primary key is made of two columns of type {@code String}, then this field may be set to
     * {@code String}.
     */
    private final Class<?> keyComponentClass;

    /**
     * The SQL statement to execute for creating features, without {@code DISTINCT} or {@code ORDER BY} clauses.
     * May contain a {@code WHERE} clause for fetching a dependency, but not for user-specified filtering.
     */
    final String sql;

    /**
     * Creates a new adapter for features in the given table.
     *
     * @param table     the table for which we are creating an adapter.
     * @param metadata  metadata about the database.
     */
    FeatureAdapter(final Table table, final DatabaseMetaData metadata) throws SQLException, InternalDataStoreException {
        this(table, metadata, new ArrayList<>(), null);
    }

    /**
     * Creates a new adapter for features in the given table.
     * This constructor may be invoked recursively for creating adapters for dependencies.
     *
     * @param table      the table for which we are creating an adapter.
     * @param metadata   metadata about the database.
     * @param following  the relations that we are following. Used for avoiding never ending loop.
     * @param noFollow   relation to not follow, or {@code null} if none.
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    private FeatureAdapter(final Table table, final DatabaseMetaData metadata,
                           final List<Relation> following, final Relation noFollow)
             throws SQLException, InternalDataStoreException
    {
        this.featureType = table.featureType;
        this.attributes  = table.attributes;
        if (table.primaryKey != null) {
            keyComponentClass = table.primaryKey.valueClass.getComponentType();
        } else {
            keyComponentClass = null;
        }
        final var columnIndices = new HashMap<String,Integer>();
        /*
         * Create a SELECT clause with all columns that are ordinary attributes.
         * Order matter, because `FeatureIterator` iterator will map the columns
         * to the attributes listed in the `attributes` array in that order.
         */
        final var sql = new SQLBuilder(table.database);
        sql.setCatalogAndSchema(metadata.getConnection());
        sql.append(SQLBuilder.SELECT);
        for (final Column column : attributes) {
            String function = null;
            if (column.getGeometryType().isPresent()) {
                function = table.database.getGeometryEncodingFunction(column);
            }
            appendColumn(sql, table.database, function, column, column.label, columnIndices);
        }
        /*
         * Collect information about associations in local arrays before to assign
         * them to the final fields, because some array lengths may be adjusted.
         */
        int count = table.importedKeys.length
                  + table.exportedKeys.length;
        if (count == 0) {
            importCount         = 0;
            dependencies        = EMPTY;
            associationNames    = null;
            foreignerKeyIndices = null;
            deferredAssociation = null;
        } else {
            String deferredAssociation = null;
            final var dependencies        = new FeatureAdapter[count];
            final var associationNames    = new String[count];
            final var foreignerKeyIndices = new int[count][];
            /*
             * For each foreigner key to another table, append all columns of that foreigner key
             * and the name of the single feature property where the association will be stored.
             */
            count = 0;                                                      // We will recount.
            for (final Relation dependency : table.importedKeys) {
                if (dependency.excluded) continue;
                if (dependency != noFollow) {
                    dependency.startFollowing(following);                   // Safety against never-ending recursion.
                    associationNames   [count] = dependency.getPropertyName();
                    foreignerKeyIndices[count] = getColumnIndices(sql, dependency, columnIndices);
                    dependencies       [count] = new FeatureAdapter(dependency.getSearchTable(), metadata, following, noFollow);
                    dependency.endFollowing(following);
                    count++;
                } else {
                    deferredAssociation = dependency.getPropertyName();
                }
            }
            importCount = count;
            /*
             * Create adapters for other tables that reference the primary keys of this table. For example
             * if we have a "City" feature with attributes for the city name, population, etc. and a "Parks"
             * feature referencing the city where the park is located, in order to populate the "City.parks"
             * associations we need to iterate over all "Parks" rows referencing the city.
             */
            for (final Relation dependency : table.exportedKeys) {
                if (dependency.excluded) continue;
                dependency.startFollowing(following);                   // Safety against never-ending recursion.
                final Table foreigner  = dependency.getSearchTable();
                final Relation inverse = foreigner.getInverseOf(dependency, table.name);
                associationNames   [count] = dependency.getPropertyName();
                foreignerKeyIndices[count] = getColumnIndices(sql, dependency, columnIndices);
                dependencies       [count] = new FeatureAdapter(foreigner, metadata, following, inverse);
                dependency.endFollowing(following);
                count++;
            }
            if (count != 0) {
                this.dependencies        = ArraysExt.resize(dependencies,        count);
                this.associationNames    = ArraysExt.resize(associationNames,    count);
                this.foreignerKeyIndices = ArraysExt.resize(foreignerKeyIndices, count);
            } else {
                this.dependencies        = EMPTY;
                this.associationNames    = null;
                this.foreignerKeyIndices = null;
            }
            this.deferredAssociation = deferredAssociation;
        }
        /*
         * Prepare SQL for a `Statement` if we do not need any condition, or for a `PreparedStatement`
         * if we need to add a `WHERE` clause. In the latter case, we will cache the features already
         * created if there is a possibility that many rows reference the same feature instance.
         */
        table.appendFromClause(sql);
        if (following.isEmpty()) {
            instances = null;       // A future SIS version could use the map opportunistically if it exists.
        } else {
            final Relation componentOf = following.get(following.size() - 1);
            String separator = " WHERE ";
            for (final String primaryKey : componentOf.getSearchColumns()) {
                sql.append(separator).appendIdentifier(primaryKey).append("=?");
                separator = " AND ";
            }
            /*
             * Following assumes that the foreigner key references the primary key of this table,
             * in which case `table.primaryKey` should never be null. This assumption may not hold
             * if the relation has been defined by `DatabaseMetaData.getCrossReference(…)` instead.
             */
            if (componentOf.useFullKey()) {
                instances = table.instanceForPrimaryKeys();
            } else {
                instances = new WeakValueHashMap<>(Object.class);       // Cannot share the table cache.
            }
        }
        this.sql = sql.toString();
    }

    /**
     * Appends a columns in the given builder and remember the column indices.
     * An exception is thrown if the column has already been added (should never happen).
     *
     * @param  sql            the SQL statement where to add column identifiers after the {@code SELECT} clause.
     * @param  database       the database. May be {@code null} if {@code function} is null.
     * @param  function       a function for which the column is an argument, or {@code null} if none.
     * @param  column         the object that provide the definition of the column, or {@code null} if none.
     * @param  columnName     name of the column to add.
     * @param  columnIndices  map where to add the mapping from column name to 1-based column index.
     */
    private static int appendColumn(final SQLBuilder sql, final Database<?> database, final String function,
            final Column column, final String columnName, final Map<String,Integer> columnIndices)
            throws InternalDataStoreException
    {
        int columnCount = columnIndices.size();
        if (columnCount != 0) sql.append(", ");
        if (function != null) {
            sql.appendIdentifier(database.catalogOfSpatialTables, database.schemaOfSpatialTables, function, false).append('(');
        }
        if (column instanceof ComputedColumn) {
            sql.append(((ComputedColumn) column).sql);
        } else {
            sql.appendIdentifier(columnName);
        }
        if (function != null) {
            sql.append(')');
        }
        if (columnIndices.put(columnName, ++columnCount) == null) return columnCount;
        throw new InternalDataStoreException(Resources.format(Resources.Keys.DuplicatedColumn_1, columnName));
    }

    /**
     * Computes the 1-based indices of columns of foreigner keys of given dependency.
     * This method also ensure that the SQL statement contains all required columns,
     * adding missing columns in the given SQL builder if necessary.
     *
     * @param  sql            the SQL statement to complete if there is missing columns.
     * @param  dependency     the dependency for which to get column indices of foreigner keys.
     * @param  columnIndices  the map containing existing column indices, or where to add missing column indices.
     * @return indices of columns of foreigner keys of given dependency. Numbering starts at 1.
     */
    private static int[] getColumnIndices(final SQLBuilder sql, final Relation dependency,
            final Map<String,Integer> columnIndices) throws InternalDataStoreException
    {
        final Collection<String> columns = dependency.getOwnerColumns();
        int i = 0;
        final int[] indices = new int[columns.size()];
        for (final String column : columns) {
            final Integer pos = columnIndices.get(column);
            indices[i++] = (pos != null) ? pos : appendColumn(sql, null, null, null, column, columnIndices);
        }
        return indices;
    }


    // ────────────────────────────────────────────────────────────────────────────────────────
    //     End of adapter construction. Next methods are helper methods for feature iterator.
    // ────────────────────────────────────────────────────────────────────────────────────────


    /**
     * Creates a feature with attribute values initialized to values fetched from the given result set.
     * This method does not follow associations.
     *
     * @param  stmts   prepared statements for fetching CRS from SRID, or {@code null} if none.
     * @param  result  the result set from which to get attribute values.
     * @return the feature with attribute values initialized.
     * @throws Exception if an error occurred while reading the database or converting values.
     */
    final Feature createFeature(final InfoStatements stmts, final ResultSet result) throws Exception {
        final Feature feature = featureType.newInstance();
        for (int i=0; i < attributes.length; i++) {
            final Column column = attributes[i];
            final Object value = column.valueGetter.getValue(stmts, result, i+1);
            if (value != null) {
                feature.setPropertyValue(column.getPropertyName(), value);
            }
        }
        return feature;
    }

    /**
     * Returns the key to use for caching the feature of a dependency.
     * If the foreigner key uses only one column, we will use the foreigner key value without creating array.
     * But if the foreigner key uses more than one column, then we need to create an array holding all values.
     *
     * @param  result      the result set over rows expected by this feature adapter.
     * @param  dependency  index of the dependency for which to create a cache key.
     * @return key to use for accesses in the {@link #instances} map,
     *         or {@code null} if any component of the key is null.
     */
    final Object getCacheKey(final ResultSet result, final int dependency) throws SQLException {
        final int[] columnIndices = foreignerKeyIndices[dependency];
        final int n = columnIndices.length;
        final Object keys = (n > 1) ? Array.newInstance(dependencies[dependency].keyComponentClass, n) : null;
        Object key = null;
        for (int p=0; p<n; p++) {
            key = result.getObject(columnIndices[p]);
            if (keys != null) Array.set(keys, p, key);
            if (key == null) return null;
        }
        return (keys != null) ? keys : key;
    }

    /**
     * Sets the statement parameters for searching a dependency.
     *
     * @param  result      the result set over rows expected by this feature adapter.
     * @param  target      the statement on which to set parameters.
     * @param  dependency  index of the dependency for which to set the parameters.
     */
    final void setForeignerKeys(final ResultSet source, final PreparedStatement target, final int dependency)
            throws SQLException
    {
        final int[] columnIndices = foreignerKeyIndices[dependency];
        for (int p=0; p < columnIndices.length;) {
            final Object k = source.getObject(columnIndices[p]);
            target.setObject(++p, k);
        }
    }
}
