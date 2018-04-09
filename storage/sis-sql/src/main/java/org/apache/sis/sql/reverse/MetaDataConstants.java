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

package org.apache.sis.sql.reverse;

/**
 * Constants defined by JDBC to retrieve a database meta-model.
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class MetaDataConstants {

    public static final class Schema{
        /** schema name */
        public static final String TABLE_SCHEM = "TABLE_SCHEM";
        /** catalog name (may be null) */
        public static final String TABLE_CATALOG = "TABLE_CATALOG";

        private Schema(){}
    }

    public static final class Table{
        /** table catalog (may be null) */
        public static final String TABLE_CAT = "TABLE_CAT";
        /** table schema (may be null) */
        public static final String TABLE_SCHEM = "TABLE_SCHEM";
        /** table name */
        public static final String TABLE_NAME = "TABLE_NAME";
        /** table type. Typical types are : <br>
         * "TABLE", "VIEW", "SYSTEM TABLE",
         * "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM". */
        public static final String TABLE_TYPE = "TABLE_TYPE";
        /** explanatory comment on the table */
        public static final String REMARKS = "REMARKS";
        /** the types catalog (may be null) */
        public static final String TYPE_CAT = "TYPE_CAT";
        /** the types schema (may be null) */
        public static final String TYPE_SCHEM = "TYPE_SCHEM";
        /** type name (may be null) */
        public static final String TYPE_NAME = "TYPE_NAME";
        /** name of the designated "identifier" column of a typed table (may be null) */
        public static final String SELF_REFERENCING_COL_NAME = "SELF_REFERENCING_COL_NAME";
        /** specifies how values in SELF_REFERENCING_COL_NAME are created.<br>
         * Values are "SYSTEM", "USER", "DERIVED". (may be null) */
        public static final String REF_GENERATION =  "REF_GENERATION";

        public static final String VALUE_TYPE_TABLE             = "TABLE";
        public static final String VALUE_TYPE_VIEW              = "VIEW";
        public static final String VALUE_TYPE_SYSTEMTABLE       = "SYSTEM TABLE";
        public static final String VALUE_TYPE_GLOBALTEMPORARY   = "GLOBAL TEMPORARY";
        public static final String VALUE_TYPE_LOCALTEMPORARY    = "LOCAL TEMPORARY";
        public static final String VALUE_TYPE_ALIAS             = "ALIAS";
        public static final String VALUE_TYPE_SYNONYM           = "SYNONYM";

        public static final String VALUE_REFGEN_SYSTEM          = "SYSTEM";
        public static final String VALUE_REFGEN_USER            = "USER";
        public static final String VALUE_REFGEN_DERIVED         = "DERIVED";

        private Table(){}
    }

    public static final class SuperTable{
        /** the type's catalog (may be null) */
        public static final String TABLE_CAT = "TABLE_CAT";
        /** type's schema (may be null) */
        public static final String TABLE_SCHEM = "TABLE_SCHEM";
        /** type name */
        public static final String TABLE_NAME = "TABLE_NAME";
        /** the direct super type's name */
        public static final String SUPERTABLE_NAME =  "SUPERTABLE_NAME";

        private SuperTable(){}
    }

    public static final class Column{

        /** table catalog (may be null) */
        public static final String TABLE_CAT = "TABLE_CAT";
        /** table schema (may be null) */
        public static final String TABLE_SCHEM = "TABLE_SCHEM";
        /** table name */
        public static final String TABLE_NAME = "TABLE_NAME";
        /** column name */
        public static final String COLUMN_NAME = "COLUMN_NAME";
        /** int : SQL type from java.sql.Types */
        public static final String DATA_TYPE = "DATA_TYPE";
        /** Data source dependent type name, for a UDT the type name is fully qualified */
        public static final String TYPE_NAME = "TYPE_NAME";
        /** int : column size. */
        public static final String COLUMN_SIZE = "COLUMN_SIZE";
        /** not used. */
        public static final String BUFFER_LENGTH = "BUFFER_LENGTH";
        /** int : the number of fractional digits. Null is returned for
         * data types where DECIMAL_DIGITS is not applicable. */
        public static final String DECIMAL_DIGITS = "DECIMAL_DIGITS";
        /** int : Radix (typically either 10 or 2) */
        public static final String NUM_PREC_RADIX = "NUM_PREC_RADIX";
        /** int : is NULL allowed. <br>
         * columnNoNulls - might not allow NULL values <br>
         * columnNullable - definitely allows NULL values<br>
         * columnNullableUnknown - nullability unknown */
        public static final String NULLABLE = "NULLABLE";
        /** comment describing column (may be null) */
        public static final String REMARKS = "REMARKS";
        /** default value for the column, which should be interpreted as a
         * string when the value is enclosed in single quotes (may be null) */
        public static final String COLUMN_DEF = "COLUMN_DEF";
        /** int : unused */
        public static final String SQL_DATA_TYPE = "SQL_DATA_TYPE";
        /** int : unused */
        public static final String SQL_DATETIME_SUB = "SQL_DATETIME_SUB";
        /** int : for char types the maximum number of bytes in the column */
        public static final String CHAR_OCTET_LENGTH = "CHAR_OCTET_LENGTH";
        /** int : index of column in table (starting at 1) */
        public static final String ORDINAL_POSITION = "ORDINAL_POSITION";
        /** ISO rules are used to determine the nullability for a column.<br>
         * YES --- if the parameter can include NULLs<br>
         * NO --- if the parameter cannot include NULLs<br>
         * empty string --- if the nullability for the parameter is unknown */
        public static final String IS_NULLABLE = "IS_NULLABLE";
        /** catalog of table that is the scope of a reference attribute
         * (null if DATA_TYPE isn't REF) */
        public static final String SCOPE_CATLOG = "SCOPE_CATLOG";
        /** schema of table that is the scope of a reference attribute
         * (null if the DATA_TYPE isn't REF) */
        public static final String SCOPE_SCHEMA = "SCOPE_SCHEMA";
        /** table name that this the scope of a reference attribute
         * (null if the DATA_TYPE isn't REF) */
        public static final String SCOPE_TABLE = "SCOPE_TABLE";
        /** short : source type of a distinct type or user-generated Ref type, SQL type
         * from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF) */
        public static final String SOURCE_DATA_TYPE = "SOURCE_DATA_TYPE";
        /** Indicates whether this column is auto incremented<br>
         * YES --- if the column is auto incremented<br>
         * NO --- if the column is not auto incremented<br>
         * empty string --- if it cannot be determined whether<br>
         * the column is auto incremented parameter is unknown
         */
        public static final String IS_AUTOINCREMENT = "IS_AUTOINCREMENT";

        public static final String VALUE_YES = "YES";
        public static final String VALUE_NO = "NO";

        private Column(){}
    }

    public static final class PrimaryKey{
        /** table catalog (may be null) */
        public static final String TABLE_CAT = "TABLE_CAT";
        /** table schema (may be null) */
        public static final String TABLE_SCHEM = "TABLE_SCHEM";
        /** table name */
        public static final String TABLE_NAME = "TABLE_NAME";
        /** column name */
        public static final String COLUMN_NAME = "COLUMN_NAME";
        /** short : sequence number within primary key.<br>
         * - a value of 1 represents the first column of the primary key<br>
         * - a value of 2 would represent the second column within the primary key */
        public static final String KEY_SEQ = "KEY_SEQ";
        /** primary key name (may be null) */
        public static final String PK_NAME = "PK_NAME";

        private PrimaryKey(){}
    }

    public static final class ImportedKey{
        /** primary key table catalog being imported (may be null) */
        public static final String PKTABLE_CAT = "PKTABLE_CAT";
        /** primary key table schema being imported (may be null) */
        public static final String PKTABLE_SCHEM = "PKTABLE_SCHEM";
        /** primary key table name being imported */
        public static final String PKTABLE_NAME = "PKTABLE_NAME";
        /** primary key column name being imported */
        public static final String PKCOLUMN_NAME = "PKCOLUMN_NAME";
        /** foreign key table catalog (may be null) */
        public static final String FKTABLE_CAT = "FKTABLE_CAT";
        /** foreign key table schema (may be null) */
        public static final String FKTABLE_SCHEM = "FKTABLE_SCHEM";
        /** foreign key table name */
        public static final String FKTABLE_NAME = "FKTABLE_NAME";
        /** foreign key column name */
        public static final String FKCOLUMN_NAME = "FKCOLUMN_NAME";
        /** short : sequence number within a foreign key <br>
         * - a value of 1 represents the first column of the foreign key<br>
         * - a value of 2 would represent the second column within the foreign key */
        public static final String KEY_SEQ = "KEY_SEQ";
        /** short : What happens to a foreign key when the primary key is updated:<br>
         * importedNoAction - do not allow update of primary key if it has been imported<br>
         * importedKeyCascade - change imported key to agree with primary key update<br>
         * importedKeySetNull - change imported key to NULL if its primary key has been updated<br>
         * importedKeySetDefault - change imported key to default values if its primary key has been updated<br>
         * importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility) */
        public static final String UPDATE_RULE = "UPDATE_RULE";
        /** short : What happens to the foreign key when primary is deleted.<br>
         * importedKeyNoAction - do not allow delete of primary key if it has been imported<br>
         * importedKeyCascade - delete rows that import a deleted key<br>
         * importedKeySetNull - change imported key to NULL if its primary key has been deleted<br>
         * importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility)<br>
         * importedKeySetDefault - change imported key to default if its primary key has been deleted */
        public static final String DELETE_RULE = "DELETE_RULE";
        /** foreign key name (may be null) */
        public static final String FK_NAME = "FK_NAME";
        /** primary key name (may be null) */
        public static final String PK_NAME = "PK_NAME";
        /** short : can the evaluation of foreign key constraints be deferred until commit<br>
         * importedKeyInitiallyDeferred - see SQL92 for definition<br>
         * importedKeyInitiallyImmediate - see SQL92 for definition<br>
         * importedKeyNotDeferrable - see SQL92 for definition */
        public static final String DEFERRABILITY = "DEFERRABILITY";

        private ImportedKey(){}
    }

    public static final class ExportedKey{
        /** primary key table catalog (may be null) */
        public static final String PKTABLE_CAT = "PKTABLE_CAT";
        /** primary key table schema (may be null) */
        public static final String PKTABLE_SCHEM = "PKTABLE_SCHEM";
        /** primary key table name */
        public static final String PKTABLE_NAME = "PKTABLE_NAME";
        /** primary key column name */
        public static final String PKCOLUMN_NAME = "PKCOLUMN_NAME";
        /** foreign key table catalog (may be null) being exported (may be null) */
        public static final String FKTABLE_CAT = "FKTABLE_CAT";
        /** foreign key table schema (may be null) being exported (may be null) */
        public static final String FKTABLE_SCHEM = "FKTABLE_SCHEM";
        /** foreign key table name being exported */
        public static final String FKTABLE_NAME = "FKTABLE_NAME";
        /** foreign key column name being exported */
        public static final String FKCOLUMN_NAME = "FKCOLUMN_NAME";
        /** short : sequence number within foreign key :<br>
         * a value of 1 represents the first column of the foreign key<br>
         * a value of 2 would represent the second column within the foreign key */
        public static final String KEY_SEQ = "KEY_SEQ";
        /** short : What happens to foreign key when primary is updated:<br>
         * importedNoAction - do not allow update of primary key if it has been imported<br>
         * importedKeyCascade - change imported key to agree with primary key update<br>
         * importedKeySetNull - change imported key to NULL if its primary key has been updated<br>
         * importedKeySetDefault - change imported key to default values if its primary key has been updated<br>
         * importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility) */
        public static final String UPDATE_RULE = "UPDATE_RULE";
        /** short : What happens to the foreign key when primary is deleted. <br>
         * importedKeyNoAction - do not allow delete of primary key if it has been imported<br>
         * importedKeyCascade - delete rows that import a deleted key<br>
         * importedKeySetNull - change imported key to NULL if its primary key has been deleted<br>
         * importedKeyRestrict - same as importedKeyNoAction (for ODBC 2.x compatibility)<br>
         * importedKeySetDefault - change imported key to default if its primary key has been deleted */
        public static final String DELETE_RULE = "DELETE_RULE";
        /** foreign key name (may be null) */
        public static final String FK_NAME = "FK_NAME";
        /** primary key name (may be null) */
        public static final String PK_NAME = "PK_NAME";
        /** short : can the evaluation of foreign key constraints be deferred until commit<br>
         * importedKeyInitiallyDeferred - see SQL92 for definition<br>
         * importedKeyInitiallyImmediate - see SQL92 for definition<br>
         * importedKeyNotDeferrable - see SQL92 for definition */
        public static final String DEFERRABILITY = "DEFERRABILITY";

        private ExportedKey(){}
    }

    public static final class BestRow{

        /** short =&gt; actual scope of result
         *  bestRowTemporary - very temporary, while using row
         *  bestRowTransaction - valid for remainder of current transaction
         *  bestRowSession - valid for remainder of current session */
        public static final String SCOPE = "SCOPE";
        /** String =&gt; column name */
        public static final String COLUMN_NAME = "COLUMN_NAME";
        /** int =&gt; SQL data type from java.sql.Types */
        public static final String DATA_TYPE = "DATA_TYPE";
        /** String =&gt; Data source dependent type name,
         * for a UDT the type name is fully qualified */
        public static final String TYPE_NAME = "TYPE_NAME";
        /** int =&gt; precision  */
        public static final String COLUMN_SIZE = "COLUMN_SIZE";
        /** int =&gt; not used  */
        public static final String BUFFER_LENGTH = "BUFFER_LENGTH";
        /** short =&gt; scale - Null is returned for data types where DECIMAL_DIGITS is not applicable. */
        public static final String DECIMAL_DIGITS = "DECIMAL_DIGITS";
        /** short =&gt; is this a pseudo column like an Oracle ROWID
            bestRowUnknown - may or may not be pseudo column
            bestRowNotPseudo - is NOT a pseudo column
            bestRowPseudo - is a pseudo column */
        public static final String PSEUDO_COLUMN = "PSEUDO_COLUMN";

        private BestRow(){}
    }

    public static final class Index{

        /** String =&gt; table catalog (may be null) */
        public static final String TABLE_CAT = "TABLE_CAT";
        /** String =&gt; table schema (may be null)  */
        public static final String TABLE_SCHEM = "TABLE_SCHEM";
        /** String =&gt; table name  */
        public static final String TABLE_NAME = "TABLE_NAME";
        /** boolean =&gt; Can index values be non-unique.
         * false when TYPE is tableIndexStatistic  */
        public static final String NON_UNIQUE = "NON_UNIQUE";
        /** String =&gt; index catalog (may be null);
         * null when TYPE is tableIndexStatistic  */
        public static final String INDEX_QUALIFIER = "INDEX_QUALIFIER";
        /** String =&gt; index name;
         * null when TYPE is tableIndexStatistic  */
        public static final String INDEX_NAME = "INDEX_NAME";
        /** short =&gt; index type:
         *   tableIndexStatistic - this identifies table statistics that are
         *       returned in conjuction with a table's index descriptions
         *   tableIndexClustered - this is a clustered index
         *   tableIndexHashed - this is a hashed index
         *   tableIndexOther - this is some other style of index  */
        public static final String TYPE = "TYPE";
        /** short =&gt; column sequence number within index;
         * zero when TYPE is tableIndexStatistic  */
        public static final String ORDINAL_POSITION = "ORDINAL_POSITION";
        /** String =&gt; column name;
         * null when TYPE is tableIndexStatistic  */
        public static final String COLUMN_NAME = "COLUMN_NAME";
        /** String =&gt; column sort sequence,
         * "A" =&gt; ascending,
         * "D" =&gt; descending, may be null if sort sequence is not supported;
         * null when TYPE is tableIndexStatistic  */
        public static final String ASC_OR_DESC = "ASC_OR_DESC";
        /** int =&gt; When TYPE is tableIndexStatistic, then this is the number of rows in the table;
         * otherwise, it is the number of unique values in the index.  */
        public static final String CARDINALITY = "CARDINALITY";
        /** int =&gt; When TYPE is tableIndexStatisic then this is the number of pages used for the table,
         * otherwise it is the number of pages used for the current index.  */
        public static final String PAGES = "PAGES";
        /** String =&gt; Filter condition, if any. (may be null) */
        public static final String FILTER_CONDITION = "FILTER_CONDITION";

        private Index(){}
    }

    private MetaDataConstants(){}

}
