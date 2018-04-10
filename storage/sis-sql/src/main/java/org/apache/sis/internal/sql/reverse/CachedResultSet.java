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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cached a ResultSet content.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class CachedResultSet {

    private final List<Map> records = new ArrayList<>();

    public CachedResultSet() {
    }

    public CachedResultSet(ResultSet rs, String... columns) throws SQLException {
        append(rs, columns);
    }

    public void append(ResultSet rs, String... columns) throws SQLException {
        while (rs.next()) {
            final Map record = new HashMap();
            for (String col : columns) {
                record.put(col, rs.getObject(col));
            }
            records.add(record);
        }
        rs.close();
    }

    public Collection<Map> records() {
        return records;
    }

}
