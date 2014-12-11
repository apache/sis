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
    private String m_sql;
    
    /** The database file. */
    private File m_database;
    
    /** Column Index that is invalid. */
    private int m_columnIndex;
    
    /**
     * Build the exception.
     * @param message Exception message.
     * @param sql SQL Statement who encountered the trouble, if known.
     * @param database The database that was queried.
     * @param columnIndex The column index that is invalid.
     */
    public SQLIllegalColumnIndexException(String message, String sql, File database, int columnIndex) {
        super(message);
        m_sql = sql;
        m_database = database;
        m_columnIndex = columnIndex;
    }
    
    /**
     * Returns the SQL statement.
     * @return SQL statement or null.
     */
    public String getSQL() {
        return m_sql;
    }
    
    /**
     * Returns the column index.
     * @return Column index.
     */
    public int getColumnIndex() {
        return m_columnIndex;
    }
    
    /**
     * Returns the database file.
     * @return Database file.
     */
    public File getDatabase() {
        return m_database;
    }
}
