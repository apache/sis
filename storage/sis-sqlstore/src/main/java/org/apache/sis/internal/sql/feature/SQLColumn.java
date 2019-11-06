package org.apache.sis.internal.sql.feature;

import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.sql.Types;

import org.apache.sis.internal.metadata.sql.Reflection;

/**
 * A simple POJO to hold information about an SQL column. This mainly represents information extracted from
 * {@link DatabaseMetaData#getColumns(String, String, String, String) database metadata}.
 * Note that for now, only a few selected information are represented. If needed, new fields could be added if needed.
 * The aim is to describe as well as possible all SQL related information about a column, to allow mapping to feature
 * model as accurate as possible.
 */
class SQLColumn {

    /**
     * Value type as specified in {@link Types}
     */
    final int type;
    /**
     * A name for the value type, free-text from the database engine. For more information about this, please see
     * {@link DatabaseMetaData#getColumns(String, String, String, String)} and {@link Reflection#TYPE_NAME}.
     */
    final String typeName;
    final boolean isNullable;

    /**
     * Name of the column, optionally with an alias, in case of a query analysis.
     */
    final ColumnRef naming;

    /**
     * Same as {@link ResultSetMetaData#getPrecision(int)}. It will be 0 if unknown. For texts, it represents maximum
     * number of characters allowed. For numbers, its maximum precision. For blobs, a limit in allowed number of bytes.
     */
    final int precision;

    /**
     * Optional. The table that contains this column. It could be null in case this column specification is done from
     * query analysis.
     */
    final TableReference origin;

    SQLColumn(int type, String typeName, boolean isNullable, ColumnRef naming, int precision) {
        this(type, typeName, isNullable, naming, precision, null);
    }

    SQLColumn(int type, String typeName, boolean isNullable, ColumnRef naming, int precision, TableReference origin) {
        this.type = type;
        this.typeName = typeName;
        this.isNullable = isNullable;
        this.naming = naming;
        this.precision = precision;
        this.origin = origin;
    }
}
