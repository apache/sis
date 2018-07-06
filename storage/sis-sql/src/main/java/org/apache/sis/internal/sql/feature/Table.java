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
import java.util.Collection;
import org.apache.sis.util.Debug;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.feature.builder.FeatureTypeBuilder;


/**
 * Description of a table in the database. The description is provided as a {@code FeatureType}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Table extends MetaModel {
    /**
     * @deprecated to be replaced by {@link #featureType} only (TODO).
     */
    @Deprecated
    FeatureTypeBuilder tableType;

    /**
     * A temporary object used for building the {@code FeatureType}.
     */
    FeatureTypeBuilder featureType;

    /**
     * The primary key of this table.
     */
    PrimaryKey key;

    /**
     * The primary keys of other tables that are referenced by this table foreign key columns.
     * They are 0:1 relations.
     */
    final Collection<Relation> importedKeys;

    /**
     * The foreign keys of other tables that reference this table primary key columns.
     * They are 0:N relations
     */
    final Collection<Relation> exportedKeys;

    /**
     * Creates a new table of the given name.
     */
    Table(final String name) {
        super(name);
        importedKeys = new ArrayList<>();
        exportedKeys = new ArrayList<>();
    }

    /**
     * Determines if this table is a component of another table. Conditions are:
     * <ul>
     *   <li>having a relation toward another type</li>
     *   <li>relation must be cascading.</li>
     * </ul>
     *
     * @return whether this table is a component of another table.
     */
    boolean isComponent() {
        for (Relation relation : importedKeys) {
            if (relation.cascadeOnDelete) {
                return true;
            }
        }
        return false;
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
        appendAll(parent, "Imported Keys", importedKeys);
        appendAll(parent, "Exported Keys", exportedKeys);
        return node;
    }
}
