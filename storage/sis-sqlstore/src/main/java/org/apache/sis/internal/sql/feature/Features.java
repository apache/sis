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
import java.util.Spliterator;
import java.util.function.Consumer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.sis.internal.metadata.sql.SQLBuilder;
import org.apache.sis.util.collection.BackingStoreException;

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
     */
    private final String[] associationNames;

    /**
     * The feature sets referenced through foreigner keys. The length of this array shall be the same as
     * {@link #associationNames} array length. Imported features are index <var>i</var> will be stored in
     * the association named {@code associationNames[i]}.
     */
    private final Features[] importedFeatures;

    /**
     * Zero-based index of the first column to query for each {@link #importedFeatures}.
     * The length of this array shall be one more than {@link #importedFeatures}, with
     * the last value set to the zero-based index after the last column.
     */
    private final int[] foreignerKeyIndices;

    /**
     * If this iterator returns only the feature matching some condition (typically a primary key value),
     * the statement for performing that filtering. Otherwise if this iterator returns all features, then
     * this field is {@code null}.
     */
    private final PreparedStatement statement;

    /**
     * The result of executing the SQL query for a {@link Table}.
     */
    private ResultSet result;

    /**
     * Estimated number of rows, or {@literal <= 0} if unknown.
     */
    private final long estimatedSize;

    /**
     * Creates a new iterator over the feature instances.
     */
    Features(final Table table, final Connection connection, final String[] attributeNames, final String[] attributeColumns,
             final Relation[] importedKeys, final Relation componentOf) throws SQLException
    {
        this.featureType = table.featureType;
        this.attributeNames = attributeNames;
        final DatabaseMetaData metadata = connection.getMetaData();
        estimatedSize = (componentOf == null) ? table.countRows(metadata, true) : 0;
        final SQLBuilder sql = new SQLBuilder(metadata, true).append("SELECT");
        /*
         * Create a SELECT clause with all columns that are ordinary attributes.
         * Order matter, since 'Features' iterator will map the columns to the
         * attributes listed in the 'attributeNames' array in that order.
         */
        int count = 0;
        for (String column : attributeColumns) {
            if (count != 0) sql.append(',');
            sql.append(' ').append(column);
            count++;
        }
        /*
         * Append columns required for all relations to other tables.
         * A column appended here may duplicate a columns appended in above loop
         * if the same column is used both as primary key and foreigner key.
         */
        if (importedKeys != null) {
            final int n = importedKeys.length;
            associationNames    = new String[n];
            importedFeatures    = new Features[n];
            foreignerKeyIndices = new int[n + 1];
            foreignerKeyIndices[0] = count;
            for (int i=0; i<n;) {
                final Relation dependency = importedKeys[i];
                associationNames[i] = dependency.getPropertyName();
                importedFeatures[i] = dependency.getRelatedTable().features(connection, dependency);
                for (final String column : dependency.getForeignerKeys()) {
                    if (count != 0) sql.append(',');
                    sql.append(' ').append(column);
                    count++;
                }
                foreignerKeyIndices[++i] = count;
            }
        } else {
            associationNames    = null;
            importedFeatures    = null;
            foreignerKeyIndices = null;
        }
        /*
         * Create a Statement if we don't need any condition, or a PreparedStatement
         * if we need to add a "WHERE" clause.
         */
        sql.append(" FROM ").appendIdentifier(table.schema, table.table);
        if (componentOf == null) {
            statement = null;
            result = connection.createStatement().executeQuery(sql.toString());
        } else {
            String separator = " WHERE ";
            for (String primaryKey : componentOf.getPrimaryKeys()) {
                sql.append(separator).append(primaryKey).append("=?");
                separator = " AND ";
            }
            statement = connection.prepareStatement(sql.toString());
        }
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
            if (importedFeatures != null) {
                for (int i=0; i < importedFeatures.length; i++) {
                    final Features dependency = importedFeatures[i];
                    final int last = foreignerKeyIndices[i+1];
                    for (int p=1, c = foreignerKeyIndices[i]; ++c <= last; p++) {
                        dependency.statement.setObject(p, result.getObject(c));
                    }
                    final Object value = dependency.fetchReferenced();
                    feature.setPropertyValue(associationNames[i], value);
                }
            }
            action.accept(feature);
            if (!all) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes the current {@link #statement} and stores all features in a list.
     * Returns {@code null} if there is no feature, the feature instance if there
     * is only one, and a list of features otherwise.
     */
    private Object fetchReferenced() throws SQLException {
        final List<Feature> instances = new ArrayList<>();
        try (ResultSet r = statement.executeQuery()) {
            result = r;
            fetch(instances::add, true);
        }
        switch (instances.size()) {
            case 0:  return null;
            case 1:  return instances.get(0);
            default: return instances;
        }
    }

    /**
     * Closes the (pooled) connection.
     */
    @Override
    public void run() {
        try {
            final Statement s = result.getStatement();
            try (Connection c = s.getConnection()) {
                result.close();
                s.close();
                for (final Features dependency : importedFeatures) {
                    dependency.statement.close();
                }
                // No need to close this.statement because it is null.
            }
        } catch (SQLException e) {
            throw new BackingStoreException(e);
        }
    }
}
