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

import org.apache.sis.util.ArgumentChecks;

/**
 * Description of a relation between two tables.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class RelationMetaModel {

    private final String relationName;
    private final String currentColumn;
    private final String foreignSchema;
    private final String foreignTable;
    private final String foreignColumn;
    private final boolean imported;
    private final boolean deleteCascade;

    public RelationMetaModel(final String relationName,final String currentColumn, final String foreignSchema,
            final String foreignTable, final String foreignColumn,
            boolean imported, boolean deleteCascade) {
        ArgumentChecks.ensureNonNull("relation name", relationName);
        ArgumentChecks.ensureNonNull("current column", currentColumn);
        ArgumentChecks.ensureNonNull("foreign table", foreignTable);
        ArgumentChecks.ensureNonNull("foreign column", foreignColumn);
        this.relationName = relationName;
        this.currentColumn = currentColumn;
        this.foreignSchema = foreignSchema;
        this.foreignTable = foreignTable;
        this.foreignColumn = foreignColumn;
        this.imported = imported;
        this.deleteCascade = deleteCascade;
    }

    public String getRelationName() {
        return relationName;
    }

    public String getCurrentColumn() {
        return currentColumn;
    }

    public String getForeignColumn() {
        return foreignColumn;
    }

    public String getForeignSchema() {
        return foreignSchema;
    }

    public String getForeignTable() {
        return foreignTable;
    }

    /**
     * Indicate if this key is imported.
     * @return
     */
    public boolean isImported() {
        return imported;
    }

    /**
     * @return true if relation implies a delete on cascade.
     */
    public boolean isDeleteCascade(){
        return deleteCascade;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(currentColumn);
        sb.append((imported) ? " → " : " ← ");
        sb.append(foreignSchema).append('.');
        sb.append(foreignTable).append('.').append(foreignColumn);
        return sb.toString();
    }
}
