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
 * Description of a database table.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class Table extends MetaModel {
    enum View {
        TABLE,
        SIMPLE_FEATURE_TYPE,
        COMPLEX_FEATURE_TYPE,
        ALL_COMPLEX
    }

    String type;

    FeatureTypeBuilder tableType;
    FeatureTypeBuilder simpleFeatureType;
    FeatureTypeBuilder complexFeatureType;
    FeatureTypeBuilder allTypes;

    PrimaryKey key;

    /**
     * those are 0:1 relations
     */
    final Collection<Relation> importedKeys = new ArrayList<>();

    /**
     * those are 0:N relations
     */
    final Collection<Relation> exportedKeys = new ArrayList<>();

    /**
     * inherited tables
     */
    final Collection<String> parents = new ArrayList<>();

    Table(final String name, final String type) {
        super(name);
        this.type = type;
    }

    /**
     * Determines if given type is a subtype. Conditions are:
     * <ul>
     *   <li>having a relation toward another type</li>
     *   <li>relation must be cascading.</li>
     * </ul>
     *
     * @return true is type is a subtype.
     *
     * @todo a subtype of what?
     */
    boolean isSubType() {
        for (Relation relation : importedKeys) {
            if (relation.cascadeOnDelete) {
                return true;
            }
        }
        return false;
    }

    FeatureTypeBuilder getType(final View view) {
        switch (view) {
            case TABLE:                return tableType;
            case SIMPLE_FEATURE_TYPE:  return simpleFeatureType;
            case COMPLEX_FEATURE_TYPE: return complexFeatureType;
            case ALL_COMPLEX:          return allTypes;
            default: throw new IllegalArgumentException("Unknown view type: " + view);
        }
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
