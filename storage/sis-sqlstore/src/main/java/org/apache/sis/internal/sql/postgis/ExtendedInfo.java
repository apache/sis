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
package org.apache.sis.internal.sql.postgis;

import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.sis.internal.sql.feature.Column;
import org.apache.sis.internal.sql.feature.Database;
import org.apache.sis.internal.sql.feature.TableReference;
import org.apache.sis.internal.sql.feature.InfoStatements;


/**
 * A specialization for PostGIS database of prepared statements about spatial information.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   1.1
 * @version 1.1
 * @module
 */
final class ExtendedInfo extends InfoStatements {
    /**
     * A statement for fetching geometric information for a specific column.
     * This statement is used for objects of type "Geography", which is a data type specific to PostGIS.
     */
    private PreparedStatement geographyColumns;

    /**
     * Creates an initially empty {@code PostgisStatements} which will use
     * the given connection for creating {@link PreparedStatement}s.
     */
    ExtendedInfo(final Database<?> session, final Connection connection) {
        super(session, connection);
    }

    /**
     * Gets all geometry columns for the given table and sets the geometry information on the corresponding columns.
     *
     * @param  source   the table for which to get all geometry columns.
     * @param  columns  all columns for the specified table. Keys are column names.
     */
    @Override
    public void completeGeometryColumns(final TableReference source, final Map<String,Column> columns) throws Exception {
        if (geometryColumns == null) {
            geometryColumns = prepareGeometryStatement("geometry_columns", "f_geometry_column", "type");
        }
        if (geographyColumns == null) {
            geographyColumns = prepareGeometryStatement("geography_columns", "f_geography_column", "type");
        }
        completeGeometryColumns(geometryColumns,  source, columns, COLUMN_TYPE_IS_TEXTUAL);
        completeGeometryColumns(geographyColumns, source, columns, COLUMN_TYPE_IS_TEXTUAL);
    }

    /**
     * Closes all prepared statements. This method does <strong>not</strong> close the connection.
     */
    @Override
    public void close() throws SQLException {
        if (geographyColumns != null) {
            geographyColumns.close();
            geographyColumns = null;
        }
        super.close();
    }
}
