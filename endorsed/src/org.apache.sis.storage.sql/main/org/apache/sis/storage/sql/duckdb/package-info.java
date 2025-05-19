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

/**
 * Specialization of {@code org.apache.sis.storage.sql.feature} for the DuckDB database.
 * Since DuckDB 1.2.1 does not provide a {@link javax.sql.DataSource} implementation,
 * users need to provide their own. The user's {@code DataSource} should load the spatial
 * extension if desired and enable the streaming. The following snippet is a suggestion:
 *
 * {@snippet lang="java" :
 * import java.util.Properties;
 * import java.sql.Connection;
 * import java.sql.DriverManager;
 * import java.sql.SQLException;
 * import java.sql.Statement;
 * import javax.sql.DataSource;
 * import org.duckdb.DuckDBDriver;
 *
 * class DuckDataSource implements DataSource {
 *     private final String url;
 *     private boolean initialized;
 *
 *     DuckDataSource(final String url) {
 *         this.url = url;
 *     }
 *
 *     @Override
 *     public Connection getConnection() throws SQLException {
 *         var info = new Properties();
 *         info.setProperty(DuckDBDriver.JDBC_STREAM_RESULTS, "true");
 *         Connection c = DriverManager.getConnection(url, info);
 *         try (Statement s = c.createStatement()) {
 *             if (!initialized) {
 *                 initialized = true;
 *                 s.execute("INSTALL spatial");
 *             }
 *             s.execute("LOAD spatial");
 *         }
 *         return c;
 *     }
 * }
 *
 * <h2>Requirements</h2>
 * Apache SIS requires DuckDB 1.2.2.0 or later. This is needed for the correction of
 * <a href="https://github.com/duckdb/duckdb-java/issues/165">DuckDB-Java issue #165</a>.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
package org.apache.sis.storage.sql.duckdb;
