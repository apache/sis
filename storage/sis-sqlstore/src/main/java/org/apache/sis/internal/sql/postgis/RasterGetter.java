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

import java.sql.ResultSet;
import java.io.InputStream;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.sql.feature.BinaryEncoding;
import org.apache.sis.internal.sql.feature.InfoStatements;
import org.apache.sis.internal.sql.feature.ValueGetter;
import org.apache.sis.coverage.grid.GridCoverage;


/**
 * Reader of rasters encoded in Well Known Binary (WKB) format.
 * At the time of writing this class, raster WKB is a PostGIS-specific format.
 *
 * <h2>Multi-threading</h2>
 * {@code RasterGetter} instances shall be thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see org.apache.sis.internal.sql.feature.GeometryGetter
 *
 * @since 1.2
 * @module
 */
final class RasterGetter extends ValueGetter<GridCoverage> {
    /**
     * The Coordinate Reference System if {@link InfoStatements} can not map the SRID.
     * This is {@code null} if there is no default.
     */
    private final CoordinateReferenceSystem defaultCRS;

    /**
     * The way binary data are encoded in the raster column.
     */
    private final BinaryEncoding encoding;

    /**
     * Creates a new reader. The same instance can be reused for parsing an arbitrary
     * amount of rasters sharing the same default CRS.
     *
     * @param  defaultCRS  the CRS to use if none can be mapped from the SRID, or {@code null} if none.
     * @param  encoding    the way binary data are encoded in the raster column.
     */
    RasterGetter(final CoordinateReferenceSystem defaultCRS, final BinaryEncoding encoding) {
        super(GridCoverage.class);
        this.defaultCRS = defaultCRS;
        this.encoding   = encoding;
    }

    /**
     * Gets the value in the column at specified index.
     * The given result set must have its cursor position on the line to read.
     * This method does not modify the cursor position.
     *
     * @param  stmts        prepared statements for fetching CRS from SRID, or {@code null} if none.
     * @param  source       the result set from which to get the value.
     * @param  columnIndex  index of the column in which to get the value.
     * @return raster value in the given column. May be {@code null}.
     * @throws Exception if an error occurred. May be an SQL error, a WKB parsing error, <i>etc.</i>
     */
    @Override
    public GridCoverage getValue(final InfoStatements stmts, final ResultSet source, final int columnIndex) throws Exception {
        InputStream stream = source.getBinaryStream(columnIndex);
        if (stream != null && stmts instanceof ExtendedInfo) {
            stream = encoding.decode(stream);
            final RasterReader reader = ((ExtendedInfo) stmts).getRasterReader();
            reader.defaultCRS = defaultCRS;
            return reader.readAsCoverage(reader.channel(stream));
        }
        return null;
    }
}
