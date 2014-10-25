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

import java.sql.*;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.system.Modules;


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
     * Returns the JDBC interface implemented by this class.
     * This is used for formatting error messages.
     */
    @Override
    final Class<?> getInterface() {
        return Driver.class;
    }

    /**
     * Attempts to make a database connection to the given filename.
     *
     * @param  url  The path to a {@code .dbf} file.
     * @param  info Ignored in current implementation.
     * @return A connection to the given DBF file.
     * @throws SQLException if this method can not create a connection to the given file.
     */
    @Override
    public Connection connect(final String url, @SuppressWarnings("unused") Properties info) throws SQLException {
        ArgumentChecks.ensureNonNull("url", url);
        try {
            return new DBFConnection(new File(url));
        } catch (IOException e) {
            throw new SQLNonTransientConnectionException(Resources.format(
                    Resources.Keys.InvalidDBFFormatDescriptor_2, url, e.getLocalizedMessage()));
        }
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
     * Gets information about the possible properties for this driver.
     * The current version has none.
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
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
    // No @Override on JDK6
    public Logger getParentLogger() {
        return LOGGER;
    }
}
