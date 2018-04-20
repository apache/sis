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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A cache of {@link ResultSet} content.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 *
 * @todo Current implementation consumes more memory than needed, with the construction of a hash map for each record.
 *       The construction of {@code DenseFeature} instances would be more efficient. Furthermore this construct reads
 *       all records at construction time. We should consider lazy population instead.
 */
final class CachedResultSet {
    /**
     * All records read by the SQL query, as (column, value) pairs.
     */
    final List<Map<String,Object>> records;

    /**
     * Creates an initially empty set.
     */
    CachedResultSet() {
        records = new ArrayList<>();
    }

    /**
     * Creates a set initialized with the given content.
     */
    CachedResultSet(final ResultSet rs, final String... columns) throws SQLException {
        records = new ArrayList<>(columns.length);
        append(rs, columns);
    }

    /**
     * Appends the given content to this set.
     */
    void append(final ResultSet rs, final String... columns) throws SQLException {
        while (rs.next()) {
            final Map<String,Object> record = new HashMap<>();
            for (final String col : columns) {
                record.put(col, rs.getObject(col));
            }
            records.add(record);
        }
        rs.close();
    }
}
