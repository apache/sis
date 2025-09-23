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
package org.apache.sis.storage.sql.duckdb;

import java.util.Locale;
import java.util.concurrent.locks.ReadWriteLock;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.metadata.sql.internal.shared.Dialect;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.sql.feature.Column;
import org.apache.sis.storage.sql.feature.Database;
import org.apache.sis.storage.sql.feature.GeometryEncoding;
import org.apache.sis.storage.sql.feature.SelectionClauseWriter;


/**
 * Information about a connection to a DuckDB database.
 * This class specializes some of the functions for converting DuckDB spatial extension objects to Java objects.
 * See the package Javadoc for version requirement and recommendation about how to connect to a DuckDB database.
 *
 * @param  <G>  the type of geometry objects. Depends on the backing implementation (ESRI, JTS, Java2D…).
 *
 * @author Guilhem Legal (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class DuckDB<G> extends Database<G> {
    /**
     * Creates a new session for a DuckDB database.
     *
     * @param  source         provider of (pooled) connections to the database.
     * @param  metadata       metadata about the database for which a session is created.
     * @param  dialect        additional information not provided by {@code metadata}.
     * @param  geomLibrary    the factory to use for creating geometric objects.
     * @param  contentLocale  the locale to use for international texts to write in the database, or {@code null} for default.
     * @param  listeners      where to send warnings.
     * @param  locks          the read/write locks, or {@code null} if none.
     * @throws SQLException if an error occurred while reading database metadata.
     */
    public DuckDB(final DataSource source, final DatabaseMetaData metadata, final Dialect dialect,
                  final Geometries<G> geomLibrary, final Locale contentLocale, final StoreListeners listeners,
                  final ReadWriteLock locks)
            throws SQLException
    {
        super(source, metadata, dialect, geomLibrary, contentLocale, listeners, locks);
    }

    /**
     * Whether to decode the geometry from <abbr>WKB</abbr> instead of <abbr>WKT</abbr>.
     * In theory, the use of binary format should be more efficient. But the DuckDB driver
     * has some issues with extracting bytes from geometry columns at the time or writing.
     * The current version extracts the geometries through <abbr>WKT</abbr> representation.
     * The reasons for not using <abbr>WKB</abbr> at this stage are:
     *
     * <ul>
     *   <li>It requires to build the query like this: {@code CAST(ST_AsWKB(geom_column) AS BLOB)}.</li>
     *   <li>It seems that for large dataset, reading from WKB is a lot slower than reading from WKT.</li>
     * </ul>
     */
    @Override
    protected GeometryEncoding getGeometryEncoding(final Column columnDefinition) {
        return GeometryEncoding.WKT;
    }

    /**
     * Returns the converter from filters/expressions to the {@code WHERE} part of SQL statement.
     */
    @Override
    protected SelectionClauseWriter getFilterToSQL() {
        return ExtendedClauseWriter.INSTANCE;
    }
}
