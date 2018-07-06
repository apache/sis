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

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import org.apache.sis.util.Debug;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.storage.DataStoreContentException;


/**
 * Description of a database schema.
 * Each schema contains a collection of {@link Table}s.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Schema extends MetaModel {
    /**
     * The tables in this schema.
     */
    private final Map<String,Table> tables;

    /**
     * Creates a new, initially empty, schema of the given name.
     *
     * @param  schemaName  name of this schema.
     */
    Schema(final String schemaName) {
        super(schemaName);
        tables = new LinkedHashMap<>();
    }

    /**
     * Adds a table in this schema.
     *
     * @param  table  the table to add.
     * @throws DataStoreContentException if a table of the same name has already been added.
     */
    void addTable(final Table table) throws DataStoreContentException {
        if (tables.putIfAbsent(table.name, table) != null) {
            throw new DataStoreContentException(Resources.format(Resources.Keys.DuplicatedEntity_2, "Table", table.name));
        }
    }

    /**
     * Returns the table of the given name, or {@code null} if none.
     */
    Table getTable(final String name) {
        return tables.get(name);
    }

    /**
     * Returns all tables in this schema.
     */
    Collection<Table> getTables() {
        return tables.values();
    }

    /**
     * Creates a tree representation of this schema with the list of all tables.
     */
    @Debug
    @Override
    TreeTable.Node appendTo(final TreeTable.Node parent) {
        final TreeTable.Node node = super.appendTo(parent);
        appendAll(parent, null, getTables());
        return node;
    }
}
