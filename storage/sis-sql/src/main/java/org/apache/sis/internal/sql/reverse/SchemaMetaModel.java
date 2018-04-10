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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.sis.internal.sql.SQLUtilities;

/**
 * Description of a database schema.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class SchemaMetaModel {

    final String name;
    final Map<String, TableMetaModel> tables = new HashMap<String, TableMetaModel>();

    public SchemaMetaModel(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Collection<TableMetaModel> getTables() {
        return tables.values();
    }

    public TableMetaModel getTable(final String name){
        return tables.get(name);
    }

    @Override
    public String toString() {
        return SQLUtilities.toStringTree(name, tables.values());
    }
}
