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

import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.internal.sql.feature.InfoStatements;


/**
 * Base class of reader and writer of rasters encoded in PostGIS <cite>Well Known Binary</cite> (WKB) format.
 * This format is specific to PostGIS 2 (this is not yet an OGC standard at the time of writing this class),
 * but it can nevertheless be used elsewhere. We do not use "WKB" in the class name because this class would
 * be more accurately named "PostGIS raster format" rather than "Well Known Binary raster format".
 *
 * <h2>Multi-threading</h2>
 * This class is <strong>not</strong> safe for multi-threading.
 * Furthermore if a non-null {@link InfoStatements} has been specified to the constructor,
 * then this object is valid only as long as the caller holds a connection to the database.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see <a href="https://trac.osgeo.org/postgis/browser/trunk/raster/doc/RFC2-WellKnownBinaryFormat">RFC2-WellKnownBinaryFormat</a>
 * @see <a href="https://postgis.net/docs/RT_reference.html">PostGIS Raster reference</a>
 *
 * @since 1.2
 * @module
 */
abstract class RasterFormat {
    /**
     * Whether the "grid to CRS" transform maps to pixel center or pixel corner.
     * WKB specification said that the "grid to CRS" translation coefficients
     * maps to upper-left pixel's upper-left corner.
     */
    static final PixelInCell ANCHOR = PixelInCell.CELL_CORNER;

    /**
     * The object to use for building CRS from or mapping a CRS to the {@code "spatial_ref_sys"} table.
     * Can be {@code null}, which means to use the {@link #srid} as EPSG codes.
     *
     * @see InfoStatements#fetchCRS(int)
     */
    final InfoStatements spatialRefSys;

    /**
     * Creates a new reader or writer.
     *
     * @param  spatialRefSys  the object to use for building CRS from or mapping CRS to the
     *         {@code "spatial_ref_sys"} table, or {@code null} for using the SRID as EPSG codes.
     */
    RasterFormat(final InfoStatements spatialRefSys) {
        this.spatialRefSys = spatialRefSys;
    }
}
