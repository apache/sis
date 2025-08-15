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


/**
 * Enumeration of features that may be supported by a database.
 * This is used as a complement of database metadata.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Supports {
    /**
     * Whether this dialect support table inheritance.
     */
    public static final int TABLE_INHERITANCE = 0x001;

    /**
     * Whether child tables inherit the index of their parent tables.
     * This feature is not yet supported in PostgreSQL.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-358">SIS-358</a>
     */
    public static final int INDEX_INHERITANCE = 0x002;

    /**
     * Whether this dialect support adding table constraints after creation.
     * This feature is not yet supported in SQLite.
     *
     * @see DatabaseMetaData#supportsAlterTableWithAddColumn()
     */
    public static final int ALTER_TABLE_WITH_ADD_CONSTRAINT = 0x004;

    /**
     * Whether the database supports {@code "GRANT USAGE ON SCHEMA …"}.
     */
    public static final int GRANT_USAGE_ON_SCHEMA = 0x008;

    /**
     * Whether the database supports {@code "GRANT SELECT ON TABLE …"}.
     */
    public static final int GRANT_SELECT_ON_TABLE = 0x010;

    /**
     * Whether the database supports {@code "COMMENT ON …"}.
     */
    public static final int COMMENT = 0x020;

    /**
     * Whether the JDBC driver supports configuring readOnly mode on connection instances.
     * This feature is not supported in SQLite.
     */
    public static final int READ_ONLY_UPDATE = 0x040;

    /**
     * Whether the JDBC driver supports concurrent transactions.
     */
    public static final int CONCURRENCY = 0x080;

    /**
     * Whether the JDBC driver supports conversions from objects to {@code java.time} API.
     * The JDBC 4.2 specification provides a mapping from {@link java.sql.Types} to temporal objects.
     * The specification suggests that {@link java.sql.ResultSet#getObject(int, Class)} should accept
     * those temporal types in the {@link Class} argument, but not all drivers support that.
     *
     * @see <a href="https://jcp.org/aboutJava/communityprocess/maintenance/jsr221/JDBC4.2MR-January2014.pdf">JDBC Maintenance Release 4.2</a>
     */
    public static final int JAVA_TIME = 0x100;

    /**
     * Whether the spatial extension supports <abbr>SRID</abbr> in {@code ST_*} functions.
     */
    public static final int SRID = 0x200;

    /**
     * Do not allow instantiation of this class.
     */
    private Supports() {
    }
}
