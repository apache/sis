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

import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import org.apache.sis.util.CharSequences;


/**
 * The SQL dialect used by a connection. This class defines also a few driver-specific operations
 * that can not (to our knowledge) be inferred from the {@link DatabaseMetaData}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
public enum Dialect {
    /**
     * The database is presumed to use ANSI SQL syntax.
     */
    ANSI(null, false),

    /**
     * The database uses Derby syntax. This is ANSI, with some constraints that PostgreSQL does not have
     * (for example column with {@code UNIQUE} constraint must explicitly be specified as {@code NOT NULL}).
     */
    DERBY("derby", false),

    /**
     * The database uses HSQL syntax. This is ANSI, but does not allow {@code INSERT} statements inserting many lines.
     * It also have a {@code SHUTDOWN} command which is specific to HSQLDB.
     */
    HSQL("hsqldb", false),

    /**
     * The database uses PostgreSQL syntax. This is ANSI, but provided an a separated
     * enumeration value because it allows a few additional commands like {@code VACUUM}.
     */
    POSTGRESQL("postgresql", true),

    /**
     * The database uses Oracle syntax. This is ANSI, but without {@code "AS"} keyword.
     */
    ORACLE("oracle", false);

    /**
     * The protocol in JDBC URL, or {@code null} if unknown.
     * This is the part after {@code "jdbc:"} and before the next {@code ':'}.
     */
    private final String protocol;

    /**
     * Whether this dialect support table inheritance.
     */
    public final boolean isTableInheritanceSupported;

    /**
     * {@code true} if child tables inherit the index of their parent tables.
     * This feature is not yet supported in PostgreSQL.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-358">SIS-358</a>
     */
    public final boolean isIndexInheritanceSupported = false;

    /**
     * Creates a new enumeration value for a SQL dialect for the given protocol.
     */
    private Dialect(final String protocol, final boolean isTableInheritanceSupported) {
        this.protocol = protocol;
        this.isTableInheritanceSupported = isTableInheritanceSupported;
    }

    /**
     * Returns the presumed SQL dialect.
     *
     * @param  metadata  the database metadata.
     * @return the presumed SQL dialect.
     * @throws SQLException if an error occurred while querying the metadata.
     */
    public static Dialect guess(final DatabaseMetaData metadata) throws SQLException {
        final String url = metadata.getURL();
        if (url != null) {
            int start = url.indexOf(':');
            if (start >= 0 && "jdbc".equalsIgnoreCase((String) CharSequences.trimWhitespaces(url, 0, start))) {
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
