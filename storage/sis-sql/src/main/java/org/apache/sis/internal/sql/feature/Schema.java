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
import java.util.HashMap;
import java.util.Collection;
import org.apache.sis.util.Debug;
import org.apache.sis.util.collection.TreeTable;


/**
 * Description of a database schema.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Schema extends MetaModel {
    /**
     * The tables in the schema.
     */
    final Map<String,Table> tables;

    /**
     * Creates a new schema of the given name.
     * It is caller responsibility to populate the {@link #tables} map.
     */
    Schema(final String name) {
        super(name);
        tables = new HashMap<>();
    }

    /**
     * Returns all tables in this schema.
     */
    Collection<Table> getTables() {
        return tables.values();
    }

    /**
     * Returns the table of the given name, or {@code null} if none.
     */
    Table getTable(final String name){
        return tables.get(name);
    }

    /**
     * Creates a tree representation of this object for debugging purpose.
     *
     * @param  parent  the parent node where to add the tree representation.
     */
    @Debug
    @Override
    TreeTable.Node appendTo(final TreeTable.Node parent) {
        final TreeTable.Node node = super.appendTo(parent);
        appendAll(parent, null, getTables());
        return node;
    }
}
