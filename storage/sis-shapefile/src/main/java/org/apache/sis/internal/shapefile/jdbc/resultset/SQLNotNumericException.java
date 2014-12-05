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
 * Exception thrown when a column value was expected numeric but wasn't.
 * @author Marc LE BIHAN
 */
public class SQLNotNumericException extends SQLException {
    /** Serial ID. */
    private static final long serialVersionUID = -1065338463289030584L;

    /** The SQL Statement (if known). */
    private String m_sql;
    
    /** The database file. */
    private File m_database;
    
    /** Column name. */
    private String m_columnName;
    
    /** The value that is not numeric. */
    private String m_value;
    
    /**
     * Build the exception.
     * @param message Exception message.
     * @param sql SQL Statement who encountered the trouble, if known.
     * @param database The database that was queried.
     * @param columnName The column name that has a non numeric value.
     * @param value The wrong value.
     */
    public SQLNotNumericException(String message, String sql, File database, String columnName, String value) {
        super(message);
        m_sql = sql;
        m_database = database;
        m_columnName = columnName;
        m_value = value;
    }
    
    /**
     * Returns the SQL statement.
     * @return SQL statement or null.
     */
    public String getSQL() {
        return m_sql;
    }
    
    /**
     * Returns the column name.
     * @return Column name.
     */
    public String getColumnName() {
        return m_columnName;
    }
    
    /**
     * Returns the value that is not numeric.
     * @return Value.
     */
    public String getValue() {
        return m_value;
    }
    
    /**
     * Returns the database file.
     * @return Database file.
     */
    public File getDatabase() {
        return m_database;
    }
}
