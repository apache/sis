package org.apache.sis.internal.shapefile.jdbc.resultset;

import java.io.File;
import java.sql.SQLException;

/**
 * Exception thrown when a column index is invalid.
 * @author Marc LE BIHAN
 */
public class SQLIllegalColumnIndexException extends SQLException {
    /** Serial ID. */
    private static final long serialVersionUID = 7525295716068215255L;

    /** The SQL Statement (if known). */
    private String sql;

    /** The database file. */
    private File database;

    /** Column Index that is invalid. */
    private int columnIndex;

    /**
     * Build the exception.
     * @param message Exception message.
     * @param sqlStatement SQL Statement who encountered the trouble, if known.
     * @param dbf The database that was queried.
     * @param colIndex The column index that is invalid.
     */
    public SQLIllegalColumnIndexException(String message, String sqlStatement, File dbf, int colIndex) {
        super(message);
        sql = sqlStatement;
        database = dbf;
        columnIndex = colIndex;
    }

    /**
     * Returns the SQL statement.
     * @return SQL statement or null.
     */
    public String getSQL() {
        return sql;
    }

    /**
     * Returns the column index.
     * @return Column index.
     */
    public int getColumnIndex() {
        return columnIndex;
    }

    /**
     * Returns the database file.
     * @return Database file.
     */
    public File getDatabase() {
        return database;
    }
}
