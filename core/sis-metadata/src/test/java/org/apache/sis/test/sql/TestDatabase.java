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
package org.apache.sis.test.sql;

import java.io.IOException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLDataException;
import org.postgresql.PGProperty;
import org.postgresql.ds.PGSimpleDataSource;
import org.hsqldb.jdbc.JDBCDataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.sis.internal.metadata.sql.Initializer;
import org.apache.sis.internal.metadata.sql.ScriptRunner;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.Debug;

import static org.junit.Assume.*;


/**
 * Utility methods for creating temporary databases for testing purpose.
 * The databases are in-memory when the database engine supports this mode.
 *
 * <div class="section">Inspecting the Derby database content in a debugger</div>
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
 *   <li><a href="https://db.apache.org/derby/docs/10.13/adminguide/radminembeddedserverex.html">Embedded server example</a></li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.7
 * @module
 */
public strictfp class TestDatabase implements AutoCloseable {
    /**
     * Data source to an alternative database to use for testing purpose.
     * If {@code null}, an in-memory Derby database will be used.
     *
     * This field is occasionally set to a non-null value (e.g. a connection to a PostgreSQL database) only for
     * debugging purpose. In such case, it is developer responsibility to ensure that the appropriate driver is
     * registered in his development environment (we may not declare them in the {@code pom.xml} file).
     */
    @Debug
    private static final DataSource TEST_DATABASE = null;

    /**
     * Name of the database to use for testing purpose. This is used only when running tests on database engine
     * that do not support in-memory database, like PostgreSQL.
     */
    private static final String NAME = "SpatialMetadataTest";

    /**
     * The data source for the test database.
     */
    public final DataSource source;

    /**
     * Creates a new test database for the given data source.
     */
    private TestDatabase(final DataSource source) {
        this.source = source;
    }

    /**
     * Creates a temporary database. This method creates a Derby in-memory database by default,
     * but this default can be changed by setting the {@link #TEST_DATABASE} hard-coded value.
     *
     * @param  name  the database name (without {@code "memory:"} prefix).
     * @return connection to the test database (usually on Apache Derby).
     * @throws SQLException if an error occurred while creating the database.
     */
    public static TestDatabase create(final String name) throws SQLException {
        if (TEST_DATABASE != null) {
            return new TestDatabase(TEST_DATABASE);
        }
        final EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setDatabaseName("memory:" + name);
        ds.setDataSourceName("Apache SIS test database");
        ds.setCreateDatabase("create");
        return new TestDatabase(ds) {
            @Override public void close() throws SQLException {
                final EmbeddedDataSource ds = (EmbeddedDataSource) source;
                ds.setCreateDatabase("no");
                ds.setConnectionAttributes("drop=true");
                try {
                    ds.getConnection().close();
                } catch (SQLException e) {                          // This is the expected exception.
                    if (!Initializer.isSuccessfulShutdown(e)) {
                        throw e;
                    }
                }
            }
        };
    }

    /**
     * Creates a in-memory database on HSQLDB.
     *
     * @param  name  the database name (without {@code "jdbc:hsqldb:mem:"} prefix).
     * @return connection to the test database.
     * @throws SQLException if an error occurred while creating the database.
     *
     * @since 1.0
     */
    public static TestDatabase createOnHSQLDB(final String name) throws SQLException {
        final JDBCDataSource ds = new JDBCDataSource();
        ds.setDatabaseName("Apache SIS test database");
        ds.setURL("jdbc:hsqldb:mem:".concat(name));
        return new TestDatabase(ds) {
            @Override public void close() throws SQLException {
                try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
                    s.execute("SHUTDOWN");
                }
            }
        };
    }

    /**
     * Creates a connection to an existing PostgreSQL database.
     * This method returns only if all the following conditions are true:
     *
     * <ol>
     *   <li>{@link TestCase#RUN_EXTENSIVE_TESTS} is {@code true} (for reducing the risk of messing with user installation).</li>
     *   <li>A PostgreSQL server is running on the local host and listening to the default port.</li>
     *   <li>A database named {@value #NAME} exists.</li>
     *   <li>The database does not contain any schema of the given name.</li>
     * </ol>
     *
     * If the {@code create} argument is {@code false}, then the callers is responsible for creating the schema
     * soon after this method call. That schema will be deleted by {@link #close()}.
     *
     * @param  schema  temporary schema to create. Shall not contain {@code '_'} or {@code '%'} characters.
     * @param  create  whether the schema should be created by this method.
     * @return connection to a PostgreSQL database
     * @throws SQLException if an error occurred while connecting to the database or creating the schema.
     *
     * @since 1.0
     */
    public static TestDatabase createOnPostgreSQL(final String schema, final boolean create) throws SQLException {
        assumeTrue("Extensive tests not enabled.", TestCase.RUN_EXTENSIVE_TESTS);
        final PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setServerName("localhost");
        ds.setDatabaseName(NAME);
        ds.setApplicationName("Apache SIS test database");
        ds.setCurrentSchema(schema);
        ds.setProperty(PGProperty.LOGGER_LEVEL, "OFF");   // For avoiding warning when no PostgreSQL server is running.
        /*
         * Current version does not use pooling on the assumption
         * that connections to local host are fast enough.
         */
        try (Connection c = ds.getConnection()) {
            try (ResultSet reflect = c.getMetaData().getSchemas(null, schema)) {
                if (reflect.next()) {
                    throw new SQLDataException("Schema \"" + schema + "\" already exists in \"" + NAME + "\".");
                }
            }
            if (create) {
                try (Statement s = c.createStatement()) {
                    s.execute("CREATE SCHEMA \"" + schema + '"');
                }
            }
        } catch (SQLException e) {
            final String state = e.getSQLState();
            assumeFalse("This test needs a PostgreSQL server running on the local host.", "08001".equals(state));
            assumeFalse("This test needs a PostgreSQL database named \"" + NAME + "\".",  "3D000".equals(state));
            throw e;
        }
        return new TestDatabase(ds) {
            @Override public void close() throws SQLException {
                final PGSimpleDataSource ds = (PGSimpleDataSource) source;
                try (Connection c = ds.getConnection()) {
                    try (Statement s = c.createStatement()) {
                        s.execute("DROP SCHEMA \"" + ds.getCurrentSchema() + "\" CASCADE");
                    }
                }
            }
        };
    }

    /**
     * Executes the SQL statements in the given resource file.
     *
     * @param loader     a class in the package of the resource file. This is usually the test class.
     * @param queryFile  name of the SQL file to load and execute.
     * @throws IOException if an error occurred while reading the input.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public void executeSQL(final Class<?> loader, final String queryFile) throws IOException, SQLException {
        try (Connection c = source.getConnection(); ScriptRunner r = new ScriptRunner(c, 1000)) {
            r.run(loader, queryFile);
        }
    }

    /**
     * Drops the test schema (PostgreSQL) or the test database (Derby) after usage.
     *
     * @throws SQLException if an error occurred while dropping the test data.
     */
    @Override
    public void close() throws SQLException {
        // To be overriden by anonymous classes.
    }
}
