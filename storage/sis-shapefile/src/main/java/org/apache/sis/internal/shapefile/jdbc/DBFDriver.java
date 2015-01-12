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

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;

import org.apache.sis.internal.shapefile.jdbc.connection.DBFConnection;
import org.apache.sis.internal.system.*;


/**
 * Database driver for DBF 3.
 *
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class DBFDriver extends AbstractJDBC implements Driver {
    /**
     * Creates a new driver.
     */
    public DBFDriver() {
    }

    /**
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isAssignableFrom(getInterface());
    }

    /**
     * Returns the JDBC interface implemented by this class.
     * This is used for formatting error messages.
     */
    @Override
    final protected Class<?> getInterface() {
        return Driver.class;
    }

    /**
     * Attempts to make a database connection to the given filename.
     *
     * @param  url  The path to a {@code .dbf} file.
     * @param  info Ignored in current implementation.
     * @return A connection to the given DBF file.
     * @throws InvalidDbaseFileFormatException if the database file format is invalid.
     * @throws DbaseFileNotFoundException if the database file doesn't exist.
     * @throws InvalidDbaseFileFormatException if the database file has a wrong format.
     */
    @Override
    @SuppressWarnings("resource") // the function opens a connection.
    public Connection connect(final String url, @SuppressWarnings("unused") Properties info) throws InvalidDbaseFileFormatException, DbaseFileNotFoundException {
        Objects.requireNonNull(url, "the DBase3 url cannot be null");
        File file = new File(url);

        return new DBFConnection(file, new MappedByteReader(file));
    }

    /**
     * Returns {@code true} if this driver thinks that it can open the given URL.
     */
    @Override
    public boolean acceptsURL(final String url) {
        if (!url.endsWith(".dbf")) {
            return false;
        }

        final File datafile = new File(url);
        return datafile.isFile(); // Future version should check for magic number.
    }

    /**
     * @see org.apache.sis.internal.shapefile.jdbc.AbstractJDBC#getFile()
     */
    @Override protected File getFile() {
        return null;
    }

    /**
     * Gets information about the possible properties for this driver.
     * The current version has none.
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(@SuppressWarnings("unused") String url, @SuppressWarnings("unused") Properties info) {
        return new DriverPropertyInfo[0];
    }

    /**
     * The major version number of this driver.
     * This is set to the Apache SIS version.
     */
    @Override
    public int getMajorVersion() {
        return Modules.MAJOR_VERSION;
    }

    /**
     * The minor version number of this driver.
     * This is set to the Apache SIS version.
     */
    @Override
    public int getMinorVersion() {
        return Modules.MINOR_VERSION;
    }

    /**
     * This driver is currently not compliant.
     * It has to succeed these tests first:
     * <a href="http://www.oracle.com/technetwork/java/jdbctestsuite-1-3-1-140675.html">Compliance tests</a>.
     */
    @Override
    public boolean jdbcCompliant() {
        return false; // No, and for some time...
    }

    /**
     * The logger used by this driver.
     */
    @Override
    public Logger getParentLogger() {
        return super.getLogger();
    }
}
