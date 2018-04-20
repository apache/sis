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


/**
 * Constants defined by JDBC to retrieve a database meta-model.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class MetaDataConstants {

    static final class Schema {
        /** schema name. */
        public static final String TABLE_SCHEM = "TABLE_SCHEM";

        /** catalog name (values in this column may be null). */
        public static final String TABLE_CATALOG = "TABLE_CATALOG";

        private Schema() {
        }
    }

    static final class Table {
        /** Table catalog (values in this column may be null). */
        public static final String TABLE_CAT = "TABLE_CAT";

        /** Table schema (values in this column may be null). */

        public static final String TABLE_SCHEM = "TABLE_SCHEM";

        /** Table name. */
        public static final String TABLE_NAME = "TABLE_NAME";

        /**
         * Table type. Typical types are:
         * "TABLE", "VIEW", "SYSTEM TABLE",
         * "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
         */
        public static final String TABLE_TYPE = "TABLE_TYPE";

        /** Explanatory comment on the table. */
        public static final String REMARKS = "REMARKS";

        /** The types catalog (values in this column may be null). */
        public static final String TYPE_CAT = "TYPE_CAT";

        /** The types schema (values in this column may be null). */
        public static final String TYPE_SCHEM = "TYPE_SCHEM";

        /** Type name (values in this column may be null). */
        public static final String TYPE_NAME = "TYPE_NAME";

        /** Name of the designated "identifier" column of a typed table (values in this column may be null). */
        public static final String SELF_REFERENCING_COL_NAME = "SELF_REFERENCING_COL_NAME";

        /**
         * Specifies how values in SELF_REFERENCING_COL_NAME are created.
         * Values are "SYSTEM", "USER", "DERIVED". values in this column may be null.
         */
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

        private Table() {
        }
    }

    public static final class SuperTable {
        /** The type's catalog (values in this column may be null). */
        public static final String TABLE_CAT = "TABLE_CAT";

        /** Type's schema (values in this column may be null). */
        public static final String TABLE_SCHEM = "TABLE_SCHEM";

        /** Type name. */
        public static final String TABLE_NAME = "TABLE_NAME";

        /** The direct super type's name. */
        public static final String SUPERTABLE_NAME =  "SUPERTABLE_NAME";

        private SuperTable() {
        }
    }

    public static final class Column {
        /** Table catalog (values in this column may be null). */
        public static final String TABLE_CAT = "TABLE_CAT";

        /** Table schema (values in this column may be null). */
        public static final String TABLE_SCHEM = "TABLE_SCHEM";

        /** Table name. */
        public static final String TABLE_NAME = "TABLE_NAME";

        /** Column name. */
        public static final String COLUMN_NAME = "COLUMN_NAME";

        /** SQL type from {@link java.sql.Types}. */
        public static final String DATA_TYPE = "DATA_TYPE";

        /** Data source dependent type name, for a UDT the type name is fully qualified. */
        public static final String TYPE_NAME = "TYPE_NAME";

        /** Column size ({@code int} values). */
        public static final String COLUMN_SIZE = "COLUMN_SIZE";

        /** Not used. */
        public static final String BUFFER_LENGTH = "BUFFER_LENGTH";

        /**
         * The number of fractional digits as {@code int} values.
         * Null is returned for data types where DECIMAL_DIGITS is not applicable.
         */
        public static final String DECIMAL_DIGITS = "DECIMAL_DIGITS";

        /** Radix (typically either 10 or 2) as {@code int} values. */
        public static final String NUM_PREC_RADIX = "NUM_PREC_RADIX";

        /**
         * Whether NULL allowed as a {@code int} code.
         * <ul>
         *   <li>{@code columnNoNulls} - might not allow NULL values</li>
         *   <li>{@code columnNullable} - definitely allows NULL values</li>
         *   <li>{@code columnNullableUnknown} - nullability unknown</li>
         * </ul>
         */
        public static final String NULLABLE = "NULLABLE";

        /** Comment describing column (values in this column may be null). */
        public static final String REMARKS = "REMARKS";

        /**
         * Default value for the column, which should be interpreted as a
         * string when the value is enclosed in single quotes (values in this column may be null)
         */
        public static final String COLUMN_DEF = "COLUMN_DEF";

        /** Unused {@code int} values. */
        public static final String SQL_DATA_TYPE = "SQL_DATA_TYPE";

        /** Unused {@code int} values. */
        public static final String SQL_DATETIME_SUB = "SQL_DATETIME_SUB";

        /** Maximum number (as a {@code int}) of bytes in the column of type {@code char}. */
        public static final String CHAR_OCTET_LENGTH = "CHAR_OCTET_LENGTH";

        /** Index of column in table (starting at 1). */
        public static final String ORDINAL_POSITION = "ORDINAL_POSITION";

        /**
         * ISO rules are used to determine the nullability for a column.
         * YES if the parameter can include NULLs,
         * NO if the parameter cannot include NULLs,
         * empty string if the nullability for the parameter is unknown.
         */
        public static final String IS_NULLABLE = "IS_NULLABLE";

        /**
         * Catalog of table that is the scope of a reference attribute
         * (null if DATA_TYPE isn't REF).
         */
        public static final String SCOPE_CATLOG = "SCOPE_CATLOG";

        /**
         * Schema of table that is the scope of a reference attribute
         * (null if the DATA_TYPE isn't REF).
         */
        public static final String SCOPE_SCHEMA = "SCOPE_SCHEMA";

        /**
         * Table name that this the scope of a reference attribute
         * (null if the DATA_TYPE isn't REF).
         */
        public static final String SCOPE_TABLE = "SCOPE_TABLE";

        /**
         * Source type of a distinct type or user-generated Ref type.
         * This is a SQL type from {@link java.sql.Types}
         * (null if DATA_TYPE isn't DISTINCT or user-generated REF).
         */
        public static final String SOURCE_DATA_TYPE = "SOURCE_DATA_TYPE";

        /**
         * Indicates whether this column is auto incremented.
         * YES if the column is auto incremented,
         * NO if the column is not auto incremented,
         * empty string if whether the column is auto incremented is unknown.
         */
        public static final String IS_AUTOINCREMENT = "IS_AUTOINCREMENT";

        public static final String VALUE_YES = "YES";
        public static final String VALUE_NO = "NO";

        private Column() {
        }
    }

    public static final class PrimaryKey{
        /** Table catalog (values in this column may be null). */
        public static final String TABLE_CAT = "TABLE_CAT";

        /** Table schema (values in this column may be null). */
        public static final String TABLE_SCHEM = "TABLE_SCHEM";

        /** Table name. */
        public static final String TABLE_NAME = "TABLE_NAME";

        /** Column name. */
        public static final String COLUMN_NAME = "COLUMN_NAME";

        /**
         * Sequence number within primary key as a {@code short} code.
         * A value of 1 represents the first column of the primary key;
         * a value of 2 would represent the second column within the primary key.
         */
        public static final String KEY_SEQ = "KEY_SEQ";

        /** Primary key name (values in this column may be null). */
        public static final String PK_NAME = "PK_NAME";

        private PrimaryKey() {
        }
    }

    public static final class ImportedKey{
        /** Primary key table catalog being imported (values in this column may be null). */
        public static final String PKTABLE_CAT = "PKTABLE_CAT";

        /** Primary key table schema being imported (values in this column may be null). */
        public static final String PKTABLE_SCHEM = "PKTABLE_SCHEM";

        /** Primary key table name being imported. */
        public static final String PKTABLE_NAME = "PKTABLE_NAME";

        /** Primary key column name being imported. */
        public static final String PKCOLUMN_NAME = "PKCOLUMN_NAME";

        /** Foreign key table catalog (may be null). */
        public static final String FKTABLE_CAT = "FKTABLE_CAT";

        /** Foreign key table schema (may be null). */
        public static final String FKTABLE_SCHEM = "FKTABLE_SCHEM";

        /** Foreign key table name. */
        public static final String FKTABLE_NAME = "FKTABLE_NAME";

        /** Foreign key column name. */
        public static final String FKCOLUMN_NAME = "FKCOLUMN_NAME";

        /**
         * Sequence number within a foreign key as a {@code short} code.
         * A value of 1 represents the first column of the foreign key;
         * a value of 2 would represent the second column within the foreign key.
         */
        public static final String KEY_SEQ = "KEY_SEQ";

        /**
         * What happens to a foreign key when the primary key is updated, as a {@code short} code.
         * <ul>
         *   <li>{@code importedNoAction} - do not allow update of primary key if it has been imported</li>
         *   <li>{@code importedKeyCascade} - change imported key to agree with primary key update</li>
         *   <li>{@code importedKeySetNull} - change imported key to NULL if its primary key has been updated</li>
         *   <li>{@code importedKeySetDefault} - change imported key to default values if its primary key has been updated</li>
         *   <li>{@code importedKeyRestrict} - same as importedKeyNoAction (for ODBC 2.x compatibility).</li>
         * </ul>
         */
        public static final String UPDATE_RULE = "UPDATE_RULE";

        /**
         * What happens to the foreign key when primary is deleted, as a {@code short} code.
         * <ul>
         *   <li>{@code importedKeyNoAction} - do not allow delete of primary key if it has been imported</li>
         *   <li>{@code importedKeyCascade} - delete rows that import a deleted key</li>
         *   <li>{@code importedKeySetNull} - change imported key to NULL if its primary key has been deleted</li>
         *   <li>{@code importedKeyRestrict} - same as importedKeyNoAction (for ODBC 2.x compatibility)</li>
         *   <li>{@code importedKeySetDefault} - change imported key to default if its primary key has been deleted.</li>
         * </ul>
         */
        public static final String DELETE_RULE = "DELETE_RULE";

        /** Foreign key name (values in this column may be null). */
        public static final String FK_NAME = "FK_NAME";

        /** Primary key name (values in this column may be null). */
        public static final String PK_NAME = "PK_NAME";

        /**
         * Whether the evaluation of foreign key constraints can be deferred until commit (as a {@code short} code).
         * <ul>
         *   <li>{@code importedKeyInitiallyDeferred} - see SQL92 for definition</li>
         *   <li>{@code importedKeyInitiallyImmediate} - see SQL92 for definition</li>
         *   <li>{@code importedKeyNotDeferrable} - see SQL92 for definition</li>
         * </ul>
         */
        public static final String DEFERRABILITY = "DEFERRABILITY";

        private ImportedKey() {
        }
    }

    public static final class ExportedKey{
        /** Primary key table catalog (values in this column may be null). */
        public static final String PKTABLE_CAT = "PKTABLE_CAT";

        /** Primary key table schema (values in this column may be null). */
        public static final String PKTABLE_SCHEM = "PKTABLE_SCHEM";

        /** Primary key table name. */
        public static final String PKTABLE_NAME = "PKTABLE_NAME";

        /** Primary key column name. */
        public static final String PKCOLUMN_NAME = "PKCOLUMN_NAME";

        /** Foreign key table catalog (may be null) being exported (values in this column may be null). */
        public static final String FKTABLE_CAT = "FKTABLE_CAT";

        /** Foreign key table schema (may be null) being exported (values in this column may be null). */
        public static final String FKTABLE_SCHEM = "FKTABLE_SCHEM";

        /** Foreign key table name being exported. */
        public static final String FKTABLE_NAME = "FKTABLE_NAME";

        /** Foreign key column name being exported. */
        public static final String FKCOLUMN_NAME = "FKCOLUMN_NAME";

        /**
         * Sequence number within foreign key as a {@code short} code.
         * a value of 1 represents the first column of the foreign key;
         * a value of 2 would represent the second column within the foreign key.
         */
        public static final String KEY_SEQ = "KEY_SEQ";

        /**
         * What happens to foreign key when primary is updated, as a {@code short} code.
         * <ul>
         *   <li>{@code importedNoAction} - do not allow update of primary key if it has been imported</li>
         *   <li>{@code importedKeyCascade} - change imported key to agree with primary key update</li>
         *   <li>{@code importedKeySetNull} - change imported key to NULL if its primary key has been updated</li>
         *   <li>{@code importedKeySetDefault} - change imported key to default values if its primary key has been updated</li>
         *   <li>{@code importedKeyRestrict} - same as importedKeyNoAction (for ODBC 2.x compatibility)</li>
         * </ul>
         */
        public static final String UPDATE_RULE = "UPDATE_RULE";

        /**
         * What happens to the foreign key when primary is deleted, as a {@code short} code.
         * <ul>
         *   <li>{@code importedKeyNoAction} - do not allow delete of primary key if it has been imported</li>
         *   <li>{@code importedKeyCascade} - delete rows that import a deleted key</li>
         *   <li>{@code importedKeySetNull} - change imported key to NULL if its primary key has been deleted</li>
         *   <li>{@code importedKeyRestrict} - same as importedKeyNoAction (for ODBC 2.x compatibility)</li>
         *   <li>{@code importedKeySetDefault} - change imported key to default if its primary key has been deleted</li>
         * </ul>
         */
        public static final String DELETE_RULE = "DELETE_RULE";

        /** Foreign key name (values in this column may be null). */
        public static final String FK_NAME = "FK_NAME";

        /** Primary key name (values in this column may be null). */
        public static final String PK_NAME = "PK_NAME";

        /**
         * Whether the evaluation of foreign key constraints can be deferred until commit, as a {@code short} code.
         * <ul>
         *   <li>{@code importedKeyInitiallyDeferred} - see SQL92 for definition</li>
         *   <li>{@code importedKeyInitiallyImmediate} - see SQL92 for definition</li>
         *   <li>{@code importedKeyNotDeferrable} - see SQL92 for definition</li>
         * </ul>
         */
        public static final String DEFERRABILITY = "DEFERRABILITY";

        private ExportedKey() {
        }
    }

    public static final class BestRow{
        /**
         * Actual scope of result as a {@code short} code.
         *  bestRowTemporary - very temporary, while using row
         *  bestRowTransaction - valid for remainder of current transaction
         *  bestRowSession - valid for remainder of current session.
         */
        public static final String SCOPE = "SCOPE";

        /** Column name. */
        public static final String COLUMN_NAME = "COLUMN_NAME";

        /** SQL data type from {@link java.sql.Types}. */
        public static final String DATA_TYPE = "DATA_TYPE";

        /** Data source dependent type name. For a UDT the type name is fully qualified. */
        public static final String TYPE_NAME = "TYPE_NAME";

        /** Precision as an {@code int}. */
        public static final String COLUMN_SIZE = "COLUMN_SIZE";

        /** Not used. */
        public static final String BUFFER_LENGTH = "BUFFER_LENGTH";

        /** Scale - Null is returned for data types where DECIMAL_DIGITS is not applicable. */
        public static final String DECIMAL_DIGITS = "DECIMAL_DIGITS";

        /**
         * Whether this a pseudo column like an Oracle ROWID, as a {@code short} code.
         * bestRowUnknown - may or may not be pseudo column
         * bestRowNotPseudo - is NOT a pseudo column
         * bestRowPseudo - is a pseudo column.
         */
        public static final String PSEUDO_COLUMN = "PSEUDO_COLUMN";

        private BestRow() {
        }
    }

    public static final class Index {
        /** Table catalog (values in this column may be null). */
        public static final String TABLE_CAT = "TABLE_CAT";

        /** Table schema (values in this column may be null). */
        public static final String TABLE_SCHEM = "TABLE_SCHEM";

        /** Table name. */
        public static final String TABLE_NAME = "TABLE_NAME";

        /** Whether index values can be non-unique.
         * false when TYPE is tableIndexStatistic  */
        public static final String NON_UNIQUE = "NON_UNIQUE";

        /**
         * Index catalog (values in this column may be null).
         * Values are null when TYPE is tableIndexStatistic.
         */
        public static final String INDEX_QUALIFIER = "INDEX_QUALIFIER";

        /** Index name; null when TYPE is tableIndexStatistic. */
        public static final String INDEX_NAME = "INDEX_NAME";

        /**
         * Index type as a {@code short} code.
         *   tableIndexStatistic - this identifies table statistics that are
         *       returned in conjuction with a table's index descriptions
         *   tableIndexClustered - this is a clustered index
         *   tableIndexHashed - this is a hashed index
         *   tableIndexOther - this is some other style of index.
         */
        public static final String TYPE = "TYPE";

        /**
         * Column sequence number within index as a {@code short}.
         * Zero when TYPE is tableIndexStatistic.
         */
        public static final String ORDINAL_POSITION = "ORDINAL_POSITION";

        /** Column name. Values are null when TYPE is tableIndexStatistic. */
        public static final String COLUMN_NAME = "COLUMN_NAME";

        /**
         * Column sort sequence.
         * "A" for ascending,
         * "D" for descending, may be null if sort sequence is not supported;
         * null when TYPE is tableIndexStatistic.
         */
        public static final String ASC_OR_DESC = "ASC_OR_DESC";

        /**
         * When TYPE is tableIndexStatistic, then this is the number of rows in the table as an {@code int}.
         * Otherwise, it is the number of unique values in the index.
         */
        public static final String CARDINALITY = "CARDINALITY";

        /**
         * When TYPE is tableIndexStatisic then this is the number of pages used for the table as an {@code int}.
         * Otherwise it is the number of pages used for the current index.
         */
        public static final String PAGES = "PAGES";

        /** Filter condition, if any. Values in this column may be null. */
        public static final String FILTER_CONDITION = "FILTER_CONDITION";

        private Index() {
        }
    }

    private MetaDataConstants() {
    }
}
