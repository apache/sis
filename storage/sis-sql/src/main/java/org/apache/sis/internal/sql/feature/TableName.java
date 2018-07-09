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

import java.util.Objects;


/**
 * A (catalog, schema, table) name tuple, which can be used as keys in hash map.
 * The {@link #name} field is for informative purpose only and ignored by this class.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
class TableName extends MetaModel {
    /**
     * The catalog, schema and table name of a table.
     * The table name is mandatory, but the schema and catalog names may be null.
     */
    final String catalog, schema, table;

    /**
     * Creates a new tuple with the give names.
     */
    TableName(final String name, final String catalog, final String schema, final String table) {
        super(name);
        this.catalog = catalog;
        this.schema  = schema;
        this.table   = table;
    }

    /**
     * Returns {@code true} if the given object is a {@code Relation} with equal table, schema and catalog names.
     * All other properties (column names, action on delete…) are ignored; this method is <strong>not</strong> for
     * testing if two {@code Relation} are fully equal. The purpose of this method is only to use {@code Relation}
     * as keys in {@link Analyzer#dependencies} map for remembering full coordinates of tables that may need to be
     * analyzed later.
     */
    @Override
    public final boolean equals(final Object obj) {
        if (obj instanceof TableName) {
            final TableName other = (TableName) obj;
            return table.equals(other.table) && Objects.equals(schema, other.schema) && Objects.equals(catalog, other.catalog);
            // Other properties (columns, cascadeOnDelete) intentionally omitted.
        }
        return false;
    }

    /**
     * Computes a hash code from the catalog, schema and table names.
     * See {@link #equals(Object)} for information about the purpose.
     */
    @Override
    public final int hashCode() {
        return table.hashCode() + 31*Objects.hashCode(schema) + 37*Objects.hashCode(catalog);
    }
}
