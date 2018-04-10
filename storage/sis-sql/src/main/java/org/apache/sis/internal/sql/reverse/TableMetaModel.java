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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.sql.SQLUtilities;

/**
 * Description of a database table.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class TableMetaModel {

    public enum View {
        TABLE,
        SIMPLE_FEATURE_TYPE,
        COMPLEX_FEATURE_TYPE,
        ALLCOMPLEX
    }

    String name;
    String type;

    FeatureTypeBuilder tableType;
    FeatureTypeBuilder simpleFeatureType;
    FeatureTypeBuilder complexFeatureType;
    FeatureTypeBuilder allType;

    PrimaryKey key;
    /**
     * those are 0:1 relations
     */
    final Collection<RelationMetaModel> importedKeys = new ArrayList<>();
    /**
     * those are 0:N relations
     */
    final Collection<RelationMetaModel> exportedKeys = new ArrayList<>();
    /**
     * inherited tables
     */
    final Collection<String> parents = new ArrayList<>();

    public TableMetaModel(final String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Collection<RelationMetaModel> getExportedKeys() {
        return Collections.unmodifiableCollection(exportedKeys);
    }

    public Collection<RelationMetaModel> getImportedKeys() {
        return Collections.unmodifiableCollection(importedKeys);
    }

    /**
     * Detect if given type is a subtype. Conditions are :
     * - having a relation toward another type
     * - relation must be cascading
     *
     * @return true is type is a subtype
     */
    public boolean isSubType(){
        for (RelationMetaModel relation : getImportedKeys()) {
            if (relation.isDeleteCascade()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(name);
        if (!importedKeys.isEmpty()) {
            sb.append(SQLUtilities.toStringTree("\n Imported Keys", importedKeys)).append('\n');
        }
        if (!exportedKeys.isEmpty()) {
            sb.append(SQLUtilities.toStringTree("\n Exported Keys", exportedKeys)).append('\n');
        }
        return sb.toString();
    }

    public FeatureTypeBuilder getType(View view) {
        switch (view) {
            case TABLE:                return tableType;
            case SIMPLE_FEATURE_TYPE:  return simpleFeatureType;
            case COMPLEX_FEATURE_TYPE: return complexFeatureType;
            case ALLCOMPLEX:           return allType;
            default: throw new IllegalArgumentException("Unknowned view type : "+view);
        }
    }

}
