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
     * @see java.sql.Driver#connect(java.lang.String, java.util.Properties)
     */
    @Override
    public Connection connect(String url, @SuppressWarnings("unused") Properties info) throws SQLException {
        return new DBFConnection(new File(url));
    }

    /**
     * @see java.sql.Driver#acceptsURL(java.lang.String)
     */
    @Override
    public boolean acceptsURL(@SuppressWarnings("unused") String url) {
        return true;
    }

    /**
     * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(@SuppressWarnings("unused") String url, @SuppressWarnings("unused") Properties info) {
        return new DriverPropertyInfo[]{};
    }

    /**
     * @see java.sql.Driver#getMajorVersion()
     */
    @Override
    public int getMajorVersion() {
        return 0;
    }

    /**
     * @see java.sql.Driver#getMinorVersion()
     */
    @Override
    public int getMinorVersion() {
        return 1;
    }

    /**
     * This driver is currently not compliant. It has to succeed these tests first : <a href="http://www.oracle.com/technetwork/java/jdbctestsuite-1-3-1-140675.html">Compliance tests</a>
     * @see java.sql.Driver#jdbcCompliant()
     */
    @Override
    public boolean jdbcCompliant() {
        return false; // No, and for some time...
    }

    /**
     * @see java.sql.Driver#getParentLogger()
     */
    @Override
    public Logger getParentLogger() {
        return getLogger();
    }
}
