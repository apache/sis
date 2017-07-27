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
package org.apache.sis.internal.metadata.sql;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.sis.util.Debug;
import org.apache.sis.internal.system.DataDirectory;

import static org.junit.Assume.*;


/**
 * Utility methods for creating temporary databases with Derby.
 * The databases are in-memory only.
 *
 * <div class="section">Inspecting the database content in a debugger</div>
 * Make sure that the classpath contains the {@code derbynet.jar} file in addition to {@code derby.jar}.
 * Then, specify the following options to the JVM (replace the 1527 port number by something else if needed):
 *
 * {@preformat text
 *   -Dderby.drda.startNetworkServer=true
 *   -Dderby.drda.portNumber=1527
 * }
 *
 * When the application is running, one can verify that the Derby server is listening:
 *
 * {@preformat text
 *   netstat -an | grep "1527"
 * }
 *
 * To connect to the in-memory database, use the {@code "jdbc:derby://localhost:1527/dbname"} URL
 * (replace {@code "dbname"} by the actual database name.
 *
 * <p><b>References:</b>
 * <ul>
 *   <li><a href="https://db.apache.org/derby/docs/10.2/adminguide/radminembeddedserverex.html">Embedded server example</a></li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.7
 * @since   0.7
 * @module
 */
public final strictfp class TestDatabase {
    /**
     * Data source to an alternative database to use for testing purpose.
     * If {@code null}, an in-memory Derby or JavaDB database will be used.
     *
     * This field is occasionally set to a non-null value (e.g. a connection to a PostgreSQL database) only for
     * debugging purpose. In such case, it is developer responsibility to ensure that the appropriate driver is
     * registered in his development environment (we may not declare them in the {@code pom.xml} file).
     */
    @Debug
    private static final DataSource TEST_DATABASE = null;

    /**
     * Do not allow (for now) instantiation of this class.
     */
    private TestDatabase() {
    }

    /**
     * Returns the path to the directory of the given name in {@code $SIS_DATA/Databases}.
     * If the directory is not found, then the test will be interrupted by an {@code org.junit.Assume} statement.
     *
     * @param  name  the name of the sub-directory.
     * @return the path to the given sub-directory.
     */
    public static Path directory(final String name) {
        Path dir = DataDirectory.DATABASES.getDirectory();
        assumeNotNull("$SIS_DATA/Databases directory not found.", dir);
        dir = dir.resolve(name);
        assumeTrue(Files.isDirectory(dir));
        return dir;
    }

    /**
     * Creates a Derby database in memory. If no Derby or JavaDB driver is not found,
     * then the test will be interrupted by an {@code org.junit.Assume} statement.
     *
     * @param  name  the database name (without {@code "memory:"} prefix).
     * @return the data source.
     * @throws Exception if an error occurred while creating the database.
     */
    public static DataSource create(final String name) throws Exception {
        if (TEST_DATABASE != null) {
            return TEST_DATABASE;
        }
        final DataSource ds;
        try {
            ds = Initializer.forJavaDB("memory:" + name);
        } catch (ClassNotFoundException e) {
            assumeNoException(e);
            throw e;
        }
        ds.getClass().getMethod("setCreateDatabase", String.class).invoke(ds, "create");
        return ds;
    }

    /**
     * Drops an in-memory Derby database after usage.
     *
     * @param  ds  the data source created by {@link #create(String)}.
     * @throws Exception if an error occurred while dropping the database.
     */
    public static void drop(final DataSource ds) throws Exception {
        if (ds == TEST_DATABASE) {
            return;
        }
        ds.getClass().getMethod("setCreateDatabase", String.class).invoke(ds, "no");
        ds.getClass().getMethod("setConnectionAttributes", String.class).invoke(ds, "drop=true");
        try {
            ds.getConnection().close();
        } catch (SQLException e) {                          // This is the expected exception.
            if (!Initializer.isSuccessfulShutdown(e)) {
                throw e;
            }
        }
    }
}
