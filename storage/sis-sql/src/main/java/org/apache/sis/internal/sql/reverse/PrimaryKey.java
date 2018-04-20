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
package org.apache.sis.internal.sql.reverse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.sis.internal.sql.Dialect;
import org.apache.sis.storage.DataStoreException;


/**
 * Description of a table primary key.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class PrimaryKey {

    final String table;
    final List<ColumnMetaModel> columns;

    PrimaryKey(final String table, List<ColumnMetaModel> columns) {
        this.table = table;
        if (columns == null) {
            columns = Collections.emptyList();
        }
        this.columns = columns;
    }

    /**
     * Creates a feature identifier from primary key column values.
     * This method uses only the current row of the given result set.
     *
     * @param  rs  the result set positioned on a row.
     * @return the feature identifier for current row of the given result set.
     */
    String buildIdentifier(final ResultSet rs) throws SQLException {
        final int size = columns.size();
        switch (size) {
            case 0: {
                // No primary key columns, generate a random id
                return UUID.randomUUID().toString();
            }
            case 1: {
                // Unique column value
                return rs.getString(columns.get(0).name);
            }
            default: {
                // Aggregate column values
                final Object[] values = new Object[size];
                for (int i=0; i<size; i++) {
                    values[i] = rs.getString(columns.get(i).name);
                }
                return buildIdentifier(values);
            }
        }
    }

    private static String buildIdentifier(final Object[] values) {
        final StringBuilder sb = new StringBuilder();
        for (int i=0; i<values.length; i++) {
            if (i > 0) sb.append('.');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    /**
     * Creates the field values for all columns of a the primary key.
     *
     * @param  dialect  handler for syntax elements specific to the database.
     * @param  cx       connection to the database.
     * @return primary key values.
     * @throws SQLException if a JDBC error occurred while executing a statement.
     * @throws DataStoreException if another error occurred while fetching the next value.
     */
    Object[] nextValues(final Dialect dialect, final Connection cx) throws SQLException, DataStoreException {
        final Object[] parts = new Object[columns.size()];
        for (int i=0; i<parts.length; i++) {
            parts[i] = columns.get(i).nextValue(dialect, cx);
        }
        return parts;
    }
}
