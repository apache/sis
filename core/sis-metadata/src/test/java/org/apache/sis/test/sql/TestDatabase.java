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
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.sis.internal.metadata.sql.LocalDataSource;
import org.apache.sis.internal.metadata.sql.ScriptRunner;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.Debug;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;


/**
 * Utility methods for creating temporary databases for testing purpose.
 * The databases are in-memory when the database engine supports this mode.
 *
 * <h2>Inspecting the Derby database content in a debugger</h2>
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
 * <h2>References</h2>
 * <ul>
 *   <li><a href="http://db.apache.org/derby/docs/10.15/adminguide/radminembeddedserverex.html">Embedded server example</a></li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.2
 * @since   0.7
 * @module
 */
public strictfp class TestDatabase implements AutoCloseable {
    /**
     * Data source for connection to an alternative database for testing purpose.
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
     * <p>See class javadoc if there is a need to inspect content of that in-memory database.</p>
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
                    if (!LocalDataSource.isSuccessfulShutdown(e)) {
                        throw e;
                    }
                }
            }
        };
    }

    /**
     * Creates a in-memory database on HSQLDB. The database can optionally use a connection pool.
     * The test method can set {@code pooled} to {@code true} if it needs the data to survive when
     * the connection is closed and re-opened.
     *
     * @param  name    the database name (without {@code "jdbc:hsqldb:mem:"} prefix).
     * @param  pooled  whether the database should use a connection pool.
     * @return connection to the test database.
     * @throws SQLException if an error occurred while creating the database.
     *
     * @see <a href="http://hsqldb.org/doc/apidocs/org/hsqldb/jdbc/JDBCDataSource.html">JDBC data source for HSQL</a>
     *
     * @since 1.0
     */
    public static TestDatabase createOnHSQLDB(final String name, final boolean pooled) throws SQLException {
        final DataSource ds;
        final org.hsqldb.jdbc.JDBCPool pool;
        final String url = "jdbc:hsqldb:mem:".concat(name);
        if (pooled) {
            pool = new org.hsqldb.jdbc.JDBCPool();
            pool.setURL(url);
            ds = pool;
        } else {
            final org.hsqldb.jdbc.JDBCDataSource simple = new org.hsqldb.jdbc.JDBCDataSource();
            simple.setURL(url);
            ds = simple;
            pool = null;
        }
        return new TestDatabase(ds) {
            @Override public void close() throws SQLException {
                try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
                    s.execute("SHUTDOWN");
                }
                if (pool != null) {
                    pool.close(2);
                }
            }
        };
    }

    /**
     * Creates a in-memory database on H2. The database can optionally use a connection pool.
     *
     * @param  name  the database name (without {@code "jdbc:h2:mem:"} prefix).
     * @return connection to the test database.
     * @throws SQLException if an error occurred while creating the database.
     *
     * @since 1.2
     */
    public static TestDatabase createOnH2(final String name) throws SQLException {
        /*
         * By default closing the last connection to a database causes the content to be lost.
         * The DB_CLOSE_DELAY=-1 parameter keeps the database alive until SHUTDOWN is invoked.
         */
        final String url = "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1";
        final org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL(url);
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
     *   <li>A role with Unix user name exists and can connect to the database without password.</li>
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
     * @see <a href="https://sis.apache.org/source.html#postgres">Configuring PostgreSQL for Apache SIS tests</a>
     *
     * @since 1.0
     */
    public static TestDatabase createOnPostgreSQL(final String schema, final boolean create) throws SQLException {
        assumeTrue("Extensive tests not enabled.", TestCase.RUN_EXTENSIVE_TESTS);
        final PGSimpleDataSource ds = new PGSimpleDataSource();
        // Server default to "localhost".
        ds.setDatabaseName(NAME);
        ds.setApplicationName("Apache SIS test database");
        ds.setCurrentSchema(schema);
        ds.setProperty(PGProperty.LOGGER_LEVEL, "OFF");   // For avoiding warning when no PostgreSQL server is running.
        /*
         * Current version does not use pooling on the assumption that connections to local host are fast enough.
         * We verify that the schema does not exist, even if the `create` argument is `false`, because we assume
         * that the test is going to create the schema soon (see the contract documented in method javadoc).
         * This is also a safety for avoiding to delete a schema that was not created by the test case.
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
            assumeFalse("This test needs a PostgreSQL database named \"" + NAME + "\".", "3D000".equals(state));
            throw e;
        }
        return new TestDatabase(ds) {
            @Override public void close() throws SQLException {
                final PGSimpleDataSource ds = (PGSimpleDataSource) source;
                try (Connection c = ds.getConnection()) {
                    try (Statement s = c.createStatement()) {
                        /*
                         * Prevents test to hang indefinitely if connections are not properly released in test cases.
                         * If the limit (in seconds) is exceeded, an SQLTimeoutException is thrown and test fails.
                         */
                        s.setQueryTimeout(10);
                        s.execute("DROP SCHEMA \"" + ds.getCurrentSchema() + "\" CASCADE");
                    }
                }
            }
        };
    }

    /**
     * Executes the given SQL statements, or statements from the given resource files.
     * If an element from the {@code scripts} array begin by {@code "file:"}, then the part
     * after {@code ":"} will be read as a resource file loaded by the given {@code loader}.
     * Otherwise the script is executed as a SQL statement. Null element are ignored.
     *
     * @param loader   a class in the package of the resource file. This is usually the test class.
     * @param scripts  SQL statements or names of the SQL files to load and execute.
     * @throws IOException if an error occurred while reading a resource file.
     * @throws SQLException if an error occurred while executing a SQL statement.
     */
    public void executeSQL(final Class<?> loader, final String... scripts) throws IOException, SQLException {
        try (Connection c = source.getConnection(); ScriptRunner r = new ScriptRunner(c, 1000)) {
            for (final String sql : scripts) {
                if (sql != null) {
                    if (sql.startsWith("file:")) {
                        r.run(loader, sql.substring(5));
                    } else {
                        r.run(sql);
                    }
                }
            }
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
