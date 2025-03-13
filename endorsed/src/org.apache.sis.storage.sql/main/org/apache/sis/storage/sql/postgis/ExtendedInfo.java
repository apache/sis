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

import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.sis.storage.sql.feature.Analyzer;
import org.apache.sis.storage.sql.feature.Column;
import org.apache.sis.storage.sql.feature.Database;
import org.apache.sis.storage.sql.feature.TableReference;
import org.apache.sis.storage.sql.feature.InfoStatements;
import org.apache.sis.storage.sql.feature.GeometryTypeEncoding;


/**
 * A specialization for PostGIS database of prepared statements about spatial information.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ExtendedInfo extends InfoStatements {
    /**
     * A statement for fetching geometric information for a specific column.
     * This statement is used for objects of type "Geography", which is a data type specific to PostGIS.
     * May be {@code null} if not yet prepared or if the table does not exist.
     * This field is valid if {@link #isAnalysisPrepared} is {@code true}.
     */
    private PreparedStatement geographyColumns;

    /**
     * A statement for fetching raster information for a specific column.
     * May be {@code null} if not yet prepared or if the table does not exist.
     * This field is valid if {@link #isAnalysisPrepared} is {@code true}.
     */
    private PreparedStatement rasterColumns;

    /**
     * The object for reading a raster, or {@code null} if not yet created.
     */
    private RasterReader rasterReader;

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
     * @param  analyzer  the opaque temporary object used for analyzing the database schema.
     * @param  source    the table for which to get all geometry columns.
     * @param  columns   all columns for the specified table. Keys are column names.
     */
    @Override
    public void completeIntrospection(final Analyzer analyzer, final TableReference source, final Map<String,Column> columns) throws Exception {
        if (!isAnalysisPrepared) {
            isAnalysisPrepared = true;
            geometryColumns  = prepareIntrospectionStatement(analyzer, "geometry_columns",  false, "f_geometry_column",  "type");
            geographyColumns = prepareIntrospectionStatement(analyzer, "geography_columns", false, "f_geography_column", "type");
            rasterColumns    = prepareIntrospectionStatement(analyzer, "raster_columns",    true,  "r_raster_column",    "");
        }
        configureSpatialColumns(geometryColumns,  source, columns, GeometryTypeEncoding.TEXTUAL);
        configureSpatialColumns(geographyColumns, source, columns, GeometryTypeEncoding.TEXTUAL);
        configureSpatialColumns(rasterColumns,    source, columns, null);
    }

    /**
     * Returns a reader for decoding PostGIS Raster binary format to grid coverage instances.
     */
    final RasterReader getRasterReader() {
        if (rasterReader == null) {
            rasterReader = new RasterReader(this);
        }
        return rasterReader;
    }

    /**
     * Closes all prepared statements. This method does <strong>not</strong> close the connection.
     */
    @Override
    @SuppressWarnings("ConvertToTryWithResources")
    public void close() throws SQLException {
        if (geographyColumns != null) {
            geographyColumns.close();
            geographyColumns = null;
        }
        if (rasterColumns != null) {
            rasterColumns.close();
            rasterColumns = null;
        }
        super.close();
    }
}
