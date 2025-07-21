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
package org.apache.sis.metadata.sql.privy;

import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.privy.Constants;


/**
 * The SQL dialect used by a connection. This class defines also a few driver-specific operations
 * that cannot (to our knowledge) be inferred from the {@link DatabaseMetaData}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public enum Dialect {
    /**
     * The database is presumed to use ANSI SQL syntax.
     *
     * @see DatabaseMetaData#supportsANSI92EntryLevelSQL()
     */
    ANSI(null, Supports.ALTER_TABLE_WITH_ADD_CONSTRAINT
             | Supports.JAVA_TIME
             | Supports.READ_ONLY_UPDATE
             | Supports.CONCURRENCY
             | Supports.SRID),

    /**
     * The database uses Derby syntax. This is ANSI, with some constraints that PostgreSQL does not have
     * (for example column with {@code UNIQUE} constraint must explicitly be specified as {@code NOT NULL}).
     * Furthermore, conversions to {@link java.time} objects are not supported.
     *
     * <a href="https://issues.apache.org/jira/browse/DERBY-6445">DERBY-6445</a>
     */
    DERBY("derby", Supports.ALTER_TABLE_WITH_ADD_CONSTRAINT
                 | Supports.READ_ONLY_UPDATE
                 | Supports.CONCURRENCY),

    /**
     * The database uses HSQL syntax. This is ANSI, but does not allow {@code INSERT} statements inserting many lines.
     * It also have a {@code SHUTDOWN} command which is specific to HSQLDB.
     */
    HSQL("hsqldb", Supports.ALTER_TABLE_WITH_ADD_CONSTRAINT
                 | Supports.JAVA_TIME
                 | Supports.READ_ONLY_UPDATE
                 | Supports.CONCURRENCY),

    /**
     * The database uses PostgreSQL syntax. This is ANSI, but provided an a separated
     * enumeration value because it allows a few additional commands like {@code VACUUM}.
     */
    POSTGRESQL("postgresql", Supports.TABLE_INHERITANCE
                           | Supports.ALTER_TABLE_WITH_ADD_CONSTRAINT
                           | Supports.JAVA_TIME
                           | Supports.READ_ONLY_UPDATE
                           | Supports.CONCURRENCY
                           | Supports.SRID),

    /**
     * The database uses Oracle syntax. This is ANSI, but without {@code "AS"} keyword.
     */
    ORACLE("oracle", Supports.ALTER_TABLE_WITH_ADD_CONSTRAINT
                   | Supports.JAVA_TIME
                   | Supports.READ_ONLY_UPDATE
                   | Supports.CONCURRENCY
                   | Supports.SRID),

    /**
     * The database uses SQLite syntax. This is ANSI, but with several limitations.
     *
     * @see <a href="https://www.sqlite.org/omitted.html">SQL Features That SQLite Does Not Implement</a>
     */
    SQLITE("sqlite", Supports.SRID),

    /**
     * The database uses DuckDB syntax. This is subset of SQL. DuckDB is not designed for transactional
     * applications, but rather for analytical processing. It runs on the local machine without server.
     *
     * <h4>Spatial extension</h4>
     * The following <abbr>SQL</abbr> statement needs to be executed at least once when DuckDB
     * is used for the first time. It can be executed with a {@link java.sql.Statement}.
     *
     * {@snippet lang="sql" :
     *     INSTALL spatial
     *     }
     *
     * Then, the following <abbr>SQL</abbr> statement should be executed on every new connection.
     * Actually, in our tests, it appears sometime necessary, sometime not.
     *
     * {@snippet lang="sql" :
     *     LOAD spatial
     *     }
     *
     * <h4>Requirements</h4>
     * Apache SIS requires DuckDB 1.2.2.0 or later. This is needed for the correction of
     * <a href="https://github.com/duckdb/duckdb-java/issues/165">DuckDB-Java issue #165</a>.
     */
    DUCKDB("duckdb", 0);

    /**
     * The protocol in JDBC URL, or {@code null} if unknown.
     * This is the part after {@code "jdbc:"} and before the next {@code ':'}.
     */
    private final String protocol;

    /**
     * Bit mask of supported features.
     */
    private final int flags;

    /**
     * Creates a new enumeration value for a SQL dialect for the given protocol.
     */
    private Dialect(final String  protocol, final int flags) {
        this.protocol = protocol;
        this.flags = flags;
    }

    /**
     * Whether this dialect support table inheritance.
     */
    public final boolean supportsTableInheritance() {
        return (flags & Supports.TABLE_INHERITANCE) != 0;
    }

    /**
     * {@code true} if child tables inherit the index of their parent tables.
     * This feature is not yet supported in PostgreSQL.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-358">SIS-358</a>
     */
    public final boolean supportsIndexInheritance() {
        return (flags & Supports.INDEX_INHERITANCE) != 0;
    }

    /**
     * Whether this dialect support adding table constraints after creation.
     * This feature is not yet supported in SQLite.
     *
     * @see DatabaseMetaData#supportsAlterTableWithAddColumn()
     */
    public final boolean supportsAlterTableWithAddConstraint() {
        return (flags & Supports.ALTER_TABLE_WITH_ADD_CONSTRAINT) != 0;
    }

    /**
     * Whether the JDBC driver supports conversions from objects to {@code java.time} API.
     * The JDBC 4.2 specification provides a mapping from {@link java.sql.Types} to temporal objects.
     * The specification suggests that {@link java.sql.ResultSet#getObject(int, Class)} should accept
     * those temporal types in the {@link Class} argument, but not all drivers support that.
     *
     * @see <a href="https://jcp.org/aboutJava/communityprocess/maintenance/jsr221/JDBC4.2MR-January2014.pdf">JDBC Maintenance Release 4.2</a>
     */
    public final boolean supportsJavaTime() {
        return (flags & Supports.JAVA_TIME) != 0;
    }

    /**
     * Whether the JDBC driver supports configuring read-only mode on connection instances.
     * This feature is not supported in SQLite.
     */
    public final boolean supportsReadOnlyUpdate() {
        return (flags & Supports.READ_ONLY_UPDATE) != 0;
    }

    /**
     * Whether the JDBC driver supports concurrent transactions.
     * This feature is not well supported in SQLite.
     */
    public final boolean supportsConcurrency() {
        return (flags & Supports.CONCURRENCY) != 0;
    }

    /**
     * Whether the spatial extension supports <abbr>SRID</abbr> in {@code ST_*} functions.
     */
    public final boolean supportsSRID() {
        return (flags & Supports.SRID) != 0;
    }

    /**
     * Returns the presumed SQL dialect.
     * If this method cannot guess the dialect, than {@link #ANSI} is presumed.
     *
     * @param  metadata  the database metadata.
     * @return the presumed SQL dialect (never {@code null}).
     * @throws SQLException if an error occurred while querying the metadata.
     */
    public static Dialect guess(final DatabaseMetaData metadata) throws SQLException {
        final String url = metadata.getURL();
        if (url != null) {
            int start = url.indexOf(':');
            if (start >= 0 && Constants.JDBC.equalsIgnoreCase((String) CharSequences.trimWhitespaces(url, 0, start))) {
                final int end = url.indexOf(':', ++start);
                if (end >= 0) {
                    final String protocol = (String) CharSequences.trimWhitespaces(url, start, end);
                    for (final Dialect candidate : values()) {
                        if (protocol.equalsIgnoreCase(candidate.protocol)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return ANSI;
    }
}
