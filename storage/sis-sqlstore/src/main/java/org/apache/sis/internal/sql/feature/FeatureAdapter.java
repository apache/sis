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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.sis.util.ArgumentChecks;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * Convert data from a specific query into Feature entities. This object can be prepared once for a specific statement,
 * and reused each time it is executed.
 *
 * @implNote For now, only attributes (values) are converted. Associations are delegated to the specific table reading
 * case, through {@link Features} class.
 *
 * This object has an initialization phase, to prepare it for a specific ResultSet, through {@link #prepare(Connection)}
 * method. It allows mappers to fetch specific information from the database when needed.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
class FeatureAdapter {

    final FeatureType type;

    private final List<PropertyMapper> attributeMappers;

    /**
     * Creates an adapter producing features of the given type, and populating its attributes with input mappers.
     *
     * @param type              the data type to produce as output. Can not be null.
     * @param attributeMappers  attribute mappers to use to decode SQL values. Mandatory but can be empty.
     */
    FeatureAdapter(FeatureType type, List<PropertyMapper> attributeMappers) {
        ArgumentChecks.ensureNonNull("type", type);
        ArgumentChecks.ensureNonNull("attributeMappers", attributeMappers);
        this.type = type;
        this.attributeMappers = Collections.unmodifiableList(new ArrayList<>(attributeMappers));
    }

    /**
     * Get a worker for a specific connection. Note that any number of result sets can be parsed with this, as long as
     * the connection is open.
     * @param target Connection usable by the mapper all along its lifecycle.
     * @return A mapper ready-to-read SQL result set.
     */
    ResultSetAdapter prepare(final Connection target) {
        final List<ReadyMapper> rtu = attributeMappers.stream()
                .map(mapper -> mapper.prepare(target))
                .collect(Collectors.toList());
        return new ResultSetAdapter(rtu);
    }

    /**
     * Specialization of {@link FeatureAdapter} as a short-live object, able to use a database connection to load third-
     * party data.
     */
    final class ResultSetAdapter {
        final List<ReadyMapper> mappers;

        ResultSetAdapter(List<ReadyMapper> mappers) {
            this.mappers = mappers;
        }

        /**
         * Read current row as a Feature. For repeated calls, please consider using {@link #prefetch(int, ResultSet)}
         * instead.
         *
         * @param cursor The result set containing query result. It must be positioned on the row you want to read. It
         *               is also your responsability to move on cursor to another row after this call.
         * @return A feature holding values of the current row of input result set. Never null.
         * @throws SQLException If an error occurs while querying a column value.
         */
        Feature read(final ResultSet cursor) throws SQLException {
            final Feature result = readAttributes(cursor);
            addImports(result, cursor);
            addExports(result);
            return result;
        }

        private Feature readAttributes(final ResultSet cursor) throws SQLException {
            final Feature result = type.newInstance();
            for (ReadyMapper mapper : mappers) mapper.read(cursor, result);
            return result;
        }

        /**
         * Load a number of rows in one go. Beware, behavior of this method is different from {@link #read(ResultSet)},
         * as it WON'T read currentl row. It wil start by moving to next row, and then read sequentially all rows until
         * given count is done, or the given result set is over.
         *
         * @param size Maximum number of elements to read from given result set. If negative or zero, this function is a
         *            no-op.
         * @param cursor Result set to extract data from. To read first entry of it, you must NOT have called {@link ResultSet#next()}
         *               on it.
         * @return A modifiable list of read elements. Never null, can be empty. It can contain less elements than asked
         * but never more.
         * @throws SQLException If extracting values from input result set fails.
         */
        List<Feature> prefetch(final int size, final ResultSet cursor) throws SQLException {
            // TODO: optimize by resolving import associations by  batch import fetching.
            final ArrayList<Feature> features = new ArrayList<>(size);
            for (int i = 0 ; i < size && cursor.next() ; i++) {
                features.add(read(cursor));
            }

            return features;
        }

        private void addImports(final Feature target, final ResultSet cursor) {
            // TODO: see Features class
        }

        private void addExports(final Feature target) {
            // TODO: see Features class
        }
    }

    static final class PropertyMapper {
        // TODO: by using a indexed implementation of Feature, we could avoid the name mapping. However, a JMH benchmark
        // would be required in order to be sure it's impacting performance positively. also, features are sparse by
        // nature, and an indexed implementation could (to verify, still) be bad on memory footprint.
        final String propertyName;
        final int columnIndex;
        final ColumnAdapter fetchValue;

        PropertyMapper(String propertyName, int columnIndex, ColumnAdapter fetchValue) {
            this.propertyName = propertyName;
            this.columnIndex = columnIndex;
            this.fetchValue = fetchValue;
        }

        ReadyMapper prepare(final Connection target) {
            return new ReadyMapper(this, fetchValue.prepare(target));
        }
    }

    private static class ReadyMapper {
        final SQLBiFunction<ResultSet, Integer, ?> reader;
        final PropertyMapper parent;

        public ReadyMapper(PropertyMapper parent, SQLBiFunction<ResultSet, Integer, ?> reader) {
            this.reader = reader;
            this.parent = parent;
        }

        private void read(ResultSet cursor, Feature target) throws SQLException {
            final Object value = reader.apply(cursor, parent.columnIndex);
            if (value != null) target.setPropertyValue(parent.propertyName, value);
        }
    }
}
