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
package org.apache.sis.sql;

import org.apache.sis.storage.Query;
import org.apache.sis.util.ArgumentChecks;

/**
 * An SQL query executed directly on the database.
 * Such query should be transfered to the JDBC without or with minimum modifications.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class SQLQuery implements Query {

    private final String statement;
    private final String name;

    /**
     * Create a new SQL query object.
     *
     * @param statement SQL query text, not null
     * @param name common name, used for returned feature types, not null
     */
    public SQLQuery(String statement, String name) {
        ArgumentChecks.ensureNonNull("statement", statement);
        ArgumentChecks.ensureNonNull("name", name);
        this.statement = statement;
        this.name = name;
    }

    /**
     * Returns the query text.
     * Example : SELECT * FROM Road
     *
     * @return SQL query text, not null
     */
    public String getStatement() {
        return statement;
    }

    /**
     * Returns the query name.
     * This name is used to build the returned feature type.
     *
     * @return common query name
     */
    public String getName() {
        return name;
    }
}
