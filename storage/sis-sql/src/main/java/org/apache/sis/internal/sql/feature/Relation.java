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


/**
 * Description of a relation between two tables.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Relation extends MetaModel {

    final String  currentColumn;
    final String  foreignSchema;
    final String  foreignTable;
    final String  foreignColumn;
    final boolean isImported;
    final boolean cascadeOnDelete;

    Relation(final String name, final String currentColumn, final String foreignSchema,
            final String foreignTable, final String foreignColumn,
            final boolean isImported, final boolean cascadeOnDelete)
    {
        super(name);
        this.currentColumn   = currentColumn;
        this.foreignSchema   = foreignSchema;
        this.foreignTable    = foreignTable;
        this.foreignColumn   = foreignColumn;
        this.isImported      = isImported;
        this.cascadeOnDelete = cascadeOnDelete;
    }

    @Override
    public String toString() {
        return new StringBuilder(currentColumn)
                .append(isImported ? " → " : " ← ")
                .append(foreignSchema).append('.')
                .append(foreignTable).append('.').append(foreignColumn)
                .toString();
    }
}
