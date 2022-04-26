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
package org.apache.sis.internal.sql.postgis;

import java.sql.Array;
import java.sql.ResultSet;
import org.apache.sis.internal.sql.feature.InfoStatements;
import org.apache.sis.internal.sql.feature.ValueGetter;
import org.postgresql.util.PGobject;


/**
 * Decoder of object of arbitrary kinds.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class ObjectGetter extends ValueGetter<Object> {
    /**
     * The singleton instance.
     */
    static final ObjectGetter INSTANCE = new ObjectGetter();

    /**
     * Creates the singleton instance.
     */
    private ObjectGetter() {
        super(Object.class);
    }

    /**
     * Gets the value in the column at specified index.
     * The given result set must have its cursor position on the line to read.
     * This method does not modify the cursor position.
     *
     * @param  stmts        prepared statements for fetching CRS from SRID, or {@code null} if none.
     * @param  source       the result set from which to get the value.
     * @param  columnIndex  index of the column in which to get the value.
     * @return Object value in the given column. May be {@code null}.
     * @throws Exception if an error occurred. May be an SQL error, a WKB parsing error, <i>etc.</i>
     */
    @Override
    public Object getValue(InfoStatements stmts, ResultSet source, int columnIndex) throws Exception {
        Object value = source.getObject(columnIndex);
        if (value instanceof PGobject) {
            final PGobject po = (PGobject) value;
            /*
             * TODO: we should invoke `getType()` and select a decoding algorithm depending on the type.
             * The driver also has a `PGBinaryObject` that we can check for more efficient data transfer
             * of points and bounding boxes. For now we just get the the wrapped value, which is always
             * a `String`.
             */
            value = po.getValue();
        }
        if (value instanceof Array) {
            value = toCollection(stmts, null, (Array) value);
        }
        return value;
    }
}
