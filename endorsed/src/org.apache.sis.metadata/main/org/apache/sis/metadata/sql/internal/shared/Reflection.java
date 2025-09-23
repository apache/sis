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
package org.apache.sis.metadata.sql.internal.shared;

import java.sql.DatabaseMetaData;


/**
 * Column names used in database reflection API. Reflection provides information about schemas, tables, columns,
 * constraints, <i>etc.</i> in the form of database tables. The main JDBC methods for those reflections are:
 *
 * <ul>
 *   <li>{@link DatabaseMetaData#getSchemas()}</li>
 *   <li>{@link DatabaseMetaData#getTables(String, String, String, String[])}</li>
 *   <li>{@link DatabaseMetaData#getColumns(String, String, String, String)}</li>
 * </ul>
 *
 * This class enumerates all the constants used by Apache SIS, and only those constants (this give a way to have
 * an overview of which database metadata are needed by SIS). Unless specified otherwise, the columns with those
 * names contain only {@code String} values. The main exceptions are {@link #DATA_TYPE}, {@link #COLUMN_SIZE} and
 * {@link #DELETE_RULE}, which contain integers.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class Reflection {
    /**
     * The {@value} key for getting a catalog name. This column appears in all reflection
     * operations (listing schemas, tables, columns, constraints, <i>etc.</i>) used by SIS.
     * The value in that column may be null.
     */
    public static final String TABLE_CAT = "TABLE_CAT";

    /**
     * The {@value} key for getting a schema name. This column appears in all reflection
     * operations (listing schemas, tables, columns, constraints, <i>etc.</i>) used by SIS.
     * The value in that column may be null.
     */
    public static final String TABLE_SCHEM = "TABLE_SCHEM";

    /**
     * The {@value} key for getting a table name. This column appears in most reflection
     * operations (listing tables, columns, constraints, <i>etc.</i>) used by SIS.
     */
    public static final String TABLE_NAME = "TABLE_NAME";

    /**
     * The {@value} key for getting a table type. The values that may appear in this column
     * are listed by {@link DatabaseMetaData#getTableTypes()}. Typical values are "TABLE",
     * "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     */
    public static final String TABLE_TYPE = "TABLE_TYPE";

    /**
     * The {@value} key for getting a column name.
     */
    public static final String COLUMN_NAME = "COLUMN_NAME";

    /**
     * The {@value} key for getting the data type as one of {@link java.sql.Types} constants.
     *
     * <p>Values in this column are integers ({@code int}) rather than {@code String}.</p>
     */
    public static final String DATA_TYPE = "DATA_TYPE";

    /**
     * Data source dependent type name. For a UDT the type name is fully qualified.
     */
    public static final String TYPE_NAME = "TYPE_NAME";

    /**
     * The {@value} key for the size for of a column. For numeric data, this is the maximum precision.
     * For character data, this is the length in characters.
     *
     * <p>Values in this column are integers ({@code int}) rather than {@code String}.</p>
     */
    public static final String COLUMN_SIZE = "COLUMN_SIZE";

    /**
     * Whether an integer type is unsigned.
     * Values in this column are integers ({@code boolean}) rather than {@code String}.
     */
    public static final String UNSIGNED_ATTRIBUTE = "UNSIGNED_ATTRIBUTE";

    /**
     * The {@value} key for the nullability of a column. Possible values are {@code "YES"} if
     * the parameter can include NULLs, {@code "NO"} if the parameter cannot include NULLs,
     * and empty string if the nullability for the parameter is unknown.
     */
    public static final String IS_NULLABLE = "IS_NULLABLE";

    /**
     * The {@value} key for indicating whether this column is auto incremented. Possible values
     * are {@code "YES"} if the column is auto incremented, {@code "NO"} if the column is not
     * auto incremented, or empty string if whether the column is auto incremented is unknown.
     */
    public static final String IS_AUTOINCREMENT = "IS_AUTOINCREMENT";

    /**
     * The {@value} key for comment describing columns.
     * Values in this column may be null.
     */
    public static final String REMARKS = "REMARKS";

    /**
     * The {@value} key for primary key name.
     * Values in this column may be null.
     */
    public static final String PK_NAME = "PK_NAME";

    /**
     * The {@value} key for the primary key table catalog being imported.
     * Values in this column may be null.
     */
    public static final String PKTABLE_CAT = "PKTABLE_CAT";

    /**
     * The {@value} key for the primary key table schema being imported.
     * Values in this column may be null.
     */
    public static final String PKTABLE_SCHEM = "PKTABLE_SCHEM";

    /**
     * The {@value} key for the primary key table name being imported.
     */
    public static final String PKTABLE_NAME = "PKTABLE_NAME";

    /**
     * The {@value} key for the primary key column name being imported.
     */
    public static final String PKCOLUMN_NAME = "PKCOLUMN_NAME";

    /**
     * The {@value} key for foreign key name.
     * Values in this column may be null.
     */
    public static final String FK_NAME = "FK_NAME";

    /**
     * The {@value} key for the foreigner key table catalog.
     * Values in this column may be null.
     */
    public static final String FKTABLE_CAT = "FKTABLE_CAT";

    /**
     * The {@value} key for the foreign key table schema.
     * Values in this column may be null.
     */
    public static final String FKTABLE_SCHEM = "FKTABLE_SCHEM";

    /**
     * The {@value} key for the foreign key table name.
     */
    public static final String FKTABLE_NAME = "FKTABLE_NAME";

    /**
     * The {@value} key for the foreign key column name.
     */
    public static final String FKCOLUMN_NAME = "FKCOLUMN_NAME";

    /**
     * The {@value} key for what happens to the foreign key when primary is deleted.
     * Possible values are:
     * <ul>
     *   <li>{@code importedKeyNoAction}   — do not allow delete of primary key if it has been imported.</li>
     *   <li>{@code importedKeyCascade}    — delete rows that import a deleted key.</li>
     *   <li>{@code importedKeySetNull}    — change imported key to NULL if its primary key has been deleted.</li>
     *   <li>{@code importedKeySetDefault} — change imported key to default if its primary key has been deleted.</li>
     * </ul>
     *
     * <p>Values in this column are short integers ({@code short}) rather than {@code String}.</p>
     */
    public static final String DELETE_RULE = "DELETE_RULE";

    /**
     * The {@value} key for the name of the index.
     * Values in this column may be null.
     */
    public static final String INDEX_NAME = "INDEX_NAME";

    /**
     * Index type: statistics, clustered, hashed or other.
     *
     * <p>Values in this column are short integers ({@code short}) rather than {@code String}.</p>
     */
    public static final String TYPE = "TYPE";

    /**
     * Number of rows in the table, or number of unique values in the index.
     *
     * <p>Values in this column are long integers ({@code long}) rather than {@code String}.</p>
     */
    public static final String CARDINALITY = "CARDINALITY";

    /**
     * Do not allow instantiation of this class.
     */
    private Reflection() {
    }
}
