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
package org.apache.sis.storage.sql.postgis;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.storage.sql.feature.Database;
import org.apache.sis.storage.sql.feature.Column;
import org.apache.sis.storage.sql.feature.TableReference;
import org.apache.sis.metadata.sql.privy.SQLBuilder;


/**
 * Estimation of the extent of geometries in a given table or column using statistics if available.
 * Uses the PostGIS {@code ST_EstimatedExtent(…)} function to get a rough estimation of column extent.
 * If {@code ST_EstimatedExtent(…)} gave no result and it was the first attempt on the specified table,
 * then this class executes {@code ANALYZE} and tries again to get the extent. This strategy works well
 * when requesting envelope on newly created tables.
 *
 * <h2>Design notes</h2>
 * We do not use the most accurate {@code ST_Extent} function because it is costly on large tables.
 * At the time of writing this class (December 2021), {@code ST_Extent} does not use column index.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://postgis.net/docs/ST_EstimatedExtent.html">ST_EstimatedExtent</a>
 */
final class ExtentEstimator {
    /**
     * The database containing the table for which to estimate the extent.
     */
    private final Database<?> database;

    /**
     * The table for which to get the extent.
     */
    private final TableReference table;

    /**
     * All columns in the table (including non-geometry columns).
     * This is a reference to an internal array; <strong>do not modify</strong>.
     */
    private final Column[] columns;

    /**
     * A temporary buffer with helper methods for building the SQL statement.
     */
    private final SQLBuilder builder;

    /**
     * The union of all extents found, or {@code null} if none.
     */
    private GeneralEnvelope envelope;

    /**
     * Errors that occurred during envelope transformations, or {@code null} if none.
     */
    private TransformException error;

    /**
     * Creates a new extent estimator for the specified table.
     */
   ExtentEstimator(final Database<?> database, final TableReference table, final Column[] columns) {
        this.database = database;
        this.table    = table;
        this.columns  = columns;
        this.builder  = new SQLBuilder(database);
    }

    /**
     * Estimates the extent in the specified columns using PostgreSQL statistics.
     * If there are no statistics available, then this method executes {@code ANALYZE}
     * and tries again.
     *
     * @param  statement  statement to use for executing queries. Shall be closed by caller.
     * @param  recall     if it is at least the second time that this method is invoked for the table.
     * @return an estimation of the union of extents in given columns, or {@code null} if unknown.
     */
    GeneralEnvelope estimate(final Statement statement, final boolean recall) throws SQLException {
        query(statement);
        if (envelope == null && !recall) {
            builder.append("ANALYZE ").appendIdentifier(table.catalog, table.schema, table.table, true);
            final String sql = builder.toString();
            builder.clear();
            statement.execute(sql);
            query(statement);
        }
        if (error != null) {
            database.listeners.warning(error);
        }
        return envelope;
    }

    /**
     * Estimates the extent in the specified columns using current statistics.
     * If there are no statistics available, then this method returns {@code null}.
     *
     * @param  statement  statement to use for executing queries. Shall be closed by caller.
     * @return an estimation of the union of extents in given columns, or {@code null} if unknown.
     */
    private void query(final Statement statement) throws SQLException {
        for (final Column column : columns) {
            if (column.getGeometryType().isPresent()) {
                database.formatTableName(builder.append(SQLBuilder.SELECT), "ST_EstimatedExtent");
                builder.append('(');
                if (table.schema != null) {
                    builder.appendValue(table.schema).append(", ");
                }
                final String sql = builder.appendValue(table.table).append(", ").appendValue(column.name).append(')').toString();
                builder.clear();
                try (ResultSet result = statement.executeQuery(sql)) {
                    while (result.next()) {
                        final String wkt = result.getString(1);
                        if (wkt != null) {
                            final GeneralEnvelope env = new GeneralEnvelope(wkt);
                            column.getDefaultCRS().ifPresent(env::setCoordinateReferenceSystem);
                            if (envelope == null) {
                                envelope = env;
                            } else try {
                                envelope.add(Envelopes.transform(env, envelope.getCoordinateReferenceSystem()));
                            } catch (TransformException e) {
                                if (error == null) error = e;
                                else error.addSuppressed(e);
                            }
                        }
                    }
                }
            }
        }
    }
}
