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
package org.apache.sis.internal.shapefile.jdbc.resultset;

import java.io.File;
import java.sql.SQLException;

/**
 * Exception thrown when a column name is not found in a request or in the database structure.
 * @author Marc LE BIHAN
 */
public class SQLNoSuchFieldException extends SQLException {
    /** Serial ID. */
    private static final long serialVersionUID = 1118679516090878177L;

    /** The SQL Statement (if known). */
    private String m_sql;
    
    /** The database file. */
    private File m_database;
    
    /** Column name that doesn't exist. */
    private String m_columnName;
    
    /**
     * Build the exception.
     * @param message Exception message.
     * @param sql SQL Statement who encountered the trouble, if known.
     * @param database The database that was queried.
     * @param columnName The column name that doesn't exist.
     */
    public SQLNoSuchFieldException(String message, String sql, File database, String columnName) {
        super(message);
        m_sql = sql;
        m_database = database;
        m_columnName = columnName;
    }
    
    /**
     * Returns the SQL statement.
     * @return SQL statement or null.
     */
    public String getSQL() {
        return m_sql;
    }
    
    /**
     * Returns the column name that doesn't exist.
     * @return Column name.
     */
    public String getColumnName() {
        return m_columnName;
    }
    
    /**
     * Returns the database file.
     * @return Database file.
     */
    public File getDatabase() {
        return m_database;
    }
}
