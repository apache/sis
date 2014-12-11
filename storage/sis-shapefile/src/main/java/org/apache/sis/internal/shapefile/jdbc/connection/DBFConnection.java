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
package org.apache.sis.internal.shapefile.jdbc.connection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.sis.internal.shapefile.jdbc.SQLConnectionClosedException;
import org.apache.sis.internal.shapefile.jdbc.metadata.DBFDatabaseMetaData;
import org.apache.sis.internal.shapefile.jdbc.statement.DBFStatement;
import org.apache.sis.storage.shapefile.Database;


/**
 * Connection to a DBF database.
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class DBFConnection extends AbstractConnection {
    /** The object to use for reading the database content. */
    final Database database;
    
    /** Opened statement. */
    private HashSet<DBFStatement> m_openedStatements = new HashSet<>(); 

    /**
     * Constructs a connection to the given database.
     * @param datafile Data file ({@code .dbf} extension).
     * @throws SQLException if the Database file cannot be found or is not a file. 
     */
    public DBFConnection(final File datafile) throws SQLException {
        // Check that file exists.
        if (!datafile.exists()) {
            throw new SQLException(format(Level.SEVERE, "excp.file_not_found", datafile.getAbsolutePath()));
        }
        
        // Check that its not a directory.
        if (datafile.isDirectory()) {
            throw new SQLException(format(Level.SEVERE, "excp.directory_not_expected", datafile.getAbsolutePath()));
        }
        
        try {
           database = new Database(datafile.getAbsolutePath());
           format(Level.FINE, "log.database_connection_opened", database.getFile().getAbsolutePath(), database.getFieldsDescriptor());
        }
        catch(FileNotFoundException e) {
           throw(new SQLException(e.getMessage(), e));
        }
    }

    /**
     * Closes the connection to the database.
     */
    @Override
    public void close() throws SQLClosingIOFailureException {
        if (isClosed())
            return;
        
        try {
            // Check if all the underlying connections that has been opened with this connection has been closed.
            // If not, we log a warning to help the developper.
            if (m_openedStatements.size() > 0) {
                format(Level.WARNING, "log.statements_left_opened", m_openedStatements.size(), m_openedStatements.stream().map(DBFStatement::toString).collect(Collectors.joining(", ")));  
            }
            
            database.close();
        } catch (IOException e) {
            throw new SQLClosingIOFailureException(format(e.getLocalizedMessage(), e), null, database.getFile());
        }
    }

    /**
     * Creates an object for sending SQL statements to the database.
     */
    @Override
    public Statement createStatement() throws SQLException {
        assertNotClosed();
        
        DBFStatement stmt = new DBFStatement(this);
        m_openedStatements.add(stmt);
        return stmt;
    }

    /**
     * @see java.sql.Connection#getCatalog()
     */
    @Override
    public String getCatalog() {
        return null; // DBase 3 offers no catalog.
    }

    /**
     * Returns the binary representation of the database.
     * @return Database.
     */
    public Database getDatabase() {
        return database;
    }

    /**
     * Returns the JDBC interface implemented by this class.
     * This is used for formatting error messages.
     */
    @Override
    final protected Class<?> getInterface() {
        return Connection.class;
    }

    /**
     * @see java.sql.Connection#getMetaData()
     */
    @Override
    public DatabaseMetaData getMetaData() {
        return new DBFDatabaseMetaData(this);
    }

    /**
     * Returns {@code true} if this connection has been closed.
     */
    @Override
    public boolean isClosed() {
        return database.isClosed();
    }

    /**
     * Returns {@code true} if the connection has not been closed and is still valid.
     * The timeout parameter is ignored and this method bases itself only on {@link #isClosed()} state.
     */
    @Override
    public boolean isValid(@SuppressWarnings("unused") int timeout) {
        return !isClosed();
    }

    /**
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isAssignableFrom(getInterface());
    }

    /**
     * Asserts that the connection is opened.
     * @throws SQLConnectionClosedException if the connection is closed.
     */
    public void assertNotClosed() throws SQLConnectionClosedException {
        // If closed throw an exception specifying the name if the DBF that is closed. 
        if (isClosed()) {
            throw new SQLConnectionClosedException(format(Level.SEVERE, "excp.closed_connection", database.getFile().getName()), null, database.getFile());
        }
    }
    
    /**
     * Method called by Statement class to notity this connection that a statement has been closed.
     * @param stmt Statement that has been closed.
     */
    public void notifyCloseStatement(DBFStatement stmt) {
        Objects.requireNonNull(stmt, "The statement notified being closed cannot be null.");
        
        if (m_openedStatements.remove(stmt) == false) {
            throw new RuntimeException(format(Level.SEVERE, "assert.statement_not_opened_by_me", stmt, toString()));
        }
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return format("toString", database.getFile().getAbsolutePath(), isClosed() == false);
    }
}
