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
package org.apache.sis.internal.shapefile.jdbc;

import org.apache.sis.storage.shapefile.Database;
import static java.util.logging.Level.*;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;


/**
 * DBF Connection.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class DBFConnection extends AbstractConnection {
    /** Database file. */
    private File m_dataFile;

    /** Database file. */
    private Database m_database;

    /**
     * Return the binary representation of the database.
     * @return Database.
     */
    public Database getDatabase() {
        return m_database;
    }

    /**
     * Return the data part file of this Dbase.
     * @return *.dbf part of the database.
     */
    public File getDataFile() {
        return m_dataFile;
    }

    /**
     * Construct a connection.
     * @param datafile Data file (.dbf extension).
     * @throws SQLException if the given file is invalid.
     */
    DBFConnection(File datafile) throws SQLException
    {
        Objects.requireNonNull(datafile, "the database file to connect to cannot be null.");

        // Check that file exists.
        if (datafile.exists() == false)
            throw(new SQLException(format(SEVERE, "excp.file_not_found", datafile.getAbsolutePath())));

        // Check that its not a directory.
        if (datafile.isDirectory())
            throw(new SQLException(format(SEVERE, "excp.directory_not_expected", datafile.getAbsolutePath())));

        m_dataFile = datafile;

        try {
            m_database = new Database(m_dataFile.getAbsolutePath());
            m_database.loadDescriptor();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Bug : File existence has been checked before.");
        } catch(IOException e) {
            String message = format(Level.SEVERE, "excp.invalid_dbf_format_descriptor", datafile.getAbsolutePath(), e.getMessage());
            throw new InvalidDbaseFileFormatException(message);
        }
    }

    /**
     * @see java.sql.Connection#createStatement()
     */
    @Override
    public Statement createStatement() {
        return new DBFStatement(this);
    }

    /**
     * @see java.sql.Connection#close()
     */
    @Override
    public void close() throws SQLException {
        try {
            m_database.close();
        } catch(IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }

    /**
     * @see java.sql.Connection#isClosed()
     */
    @Override
    public boolean isClosed() {
        return m_database.isClosed();
    }

    /**
     * @see java.sql.Connection#isValid(int)
     */
    @Override
    public boolean isValid(@SuppressWarnings("unused") int timeout) {
        this.getLogger().log(WARNING, "Connection.isValid(..) timeout parameter is ignored and the function bases itself only on isClosed state.");
        return isClosed() == false;
    }
}
