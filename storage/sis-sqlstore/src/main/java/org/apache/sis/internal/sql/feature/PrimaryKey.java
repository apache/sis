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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.sis.util.ArgumentChecks;

/**
 * Represents SQL primary key constraint. Main information is columns composing the key.
 *
 * @implNote For now, only list of columns composing the key are returned. However, in the future it would be possible
 * to add other information, as a value type to describe how to expose primary key value.
 *
 * @author "Alexis Manin (Geomatys)"
 */
interface PrimaryKey {

    static Optional<PrimaryKey> create(List<String> cols) {
        if (cols == null || cols.isEmpty()) return Optional.empty();
        if (cols.size() == 1) return Optional.of(new Simple(cols.get(0)));
        return Optional.of(new Composite(cols));
    }

    /**
     *
     * @return List of column names composing the key. Should neither be null nor empty.
     */
    List<String> getColumns();

    class Simple implements PrimaryKey {
        final String column;

        Simple(String column) {
            this.column = column;
        }

        @Override
        public List<String> getColumns() { return Collections.singletonList(column); }
    }

    class Composite implements PrimaryKey {
        /**
         * Name of columns composing primary keys.
         */
        private final List<String> columns;

        Composite(List<String> columns) {
            ArgumentChecks.ensureNonEmpty("Primary key column names", columns);
            this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        }

        @Override
        public List<String> getColumns() {
            return columns;
        }
    }
}
