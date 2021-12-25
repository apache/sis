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
package org.apache.sis.internal.sql.feature;

import java.util.OptionalInt;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.feature.Geometries;


/**
 * Reader of geometries encoded in Well Known Binary (WKB) format.
 * This class expects the WKB format as defined by OGC specification,
 * but the extended EWKB format (specific to PostGIS) is also accepted
 * if the {@link org.apache.sis.setup.GeometryLibrary} can handle it.
 *
 * <p>The WKB format is what we get from a spatial database (at least PostGIS)
 * when querying a geometry field without using any {@code ST_X} method.</p>
 *
 * <p>References:</p>
 * <ul>
 *   <li><a href="https://portal.ogc.org/files/?artifact_id=25355">OGC Simple feature access - Part 1: Common architecture</a></li>
 *   <li><a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry#Well-known_binary">Well-known binary on Wikipedia</a></li>
 *   <li><a href="http://postgis.net/docs/using_postgis_dbmanagement.html#EWKB_EWKT">PostGIS extended format</a></li>
 * </ul>
 *
 * <h2>Multi-threading</h2>
 * {@code GeometryGetter} instances shall be thread-safe.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <G> the type of geometry objects created by the factory.
 * @param <V> the type of geometry objects returned by this getter.
 *
 * @version 1.2
 *
 * @see org.apache.sis.internal.sql.postgis.RasterGetter
 *
 * @since 1.1
 * @module
 */
final class GeometryGetter<G, V extends G> extends ValueGetter<V> {
    /**
     * The factory to use for creating geometries from WKB definitions.
     */
    private final Geometries<G> geometryFactory;

    /**
     * The Coordinate Reference System if {@link InfoStatements} can not map the SRID.
     * This is {@code null} if there is no default.
     */
    private final CoordinateReferenceSystem defaultCRS;

    /**
     * The way binary data are encoded in the geometry column.
     */
    private final BinaryEncoding encoding;

    /**
     * Creates a new reader. The same instance can be reused for parsing an arbitrary
     * amount of geometries sharing the same default CRS.
     *
     * @param  geometryFactory  the factory to use for creating geometries from WKB definitions.
     * @param  geometryClass    the type of geometry to be returned by this {@code ValueGetter}.
     * @param  defaultCRS       the CRS to use if none can be mapped from the SRID, or {@code null} if none.
     * @param  encoding         the way binary data are encoded in the geometry column.
     */
    GeometryGetter(final Geometries<G> geometryFactory, final Class<V> geometryClass,
            final CoordinateReferenceSystem defaultCRS, final BinaryEncoding encoding)
    {
        super(geometryClass);
        this.geometryFactory = geometryFactory;
        this.defaultCRS      = defaultCRS;
        this.encoding        = encoding;
    }

    /**
     * Gets the value in the column at specified index.
     * The given result set must have its cursor position on the line to read.
     * This method does not modify the cursor position.
     *
     * @param  stmts        prepared statements for fetching CRS from SRID, or {@code null} if none.
     * @param  source       the result set from which to get the value.
     * @param  columnIndex  index of the column in which to get the value.
     * @return geometry value in the given column. May be {@code null}.
     * @throws Exception if an error occurred. May be an SQL error, a WKB parsing error, <i>etc.</i>
     *
     * @todo it is not sure that database driver return WKB, so we should
     *       find a way to ensure that SQL queries use {@code ST_AsBinary} function.
     */
    @Override
    public V getValue(final InfoStatements stmts, final ResultSet source, final int columnIndex) throws Exception {
        final byte[] wkb = encoding.getBytes(source, columnIndex);
        if (wkb == null) return null;
        final GeometryWrapper<G> geom = geometryFactory.parseWKB(ByteBuffer.wrap(wkb));
        CoordinateReferenceSystem crs = defaultCRS;
        if (stmts != null) {
            final OptionalInt srid = geom.getSRID();
            if (srid.isPresent()) {
                crs = stmts.fetchCRS(srid.getAsInt());
            }
        }
        if (crs != null) {
            geom.setCoordinateReferenceSystem(crs);
        }
        return valueType.cast(geom.implementation());
    }
}
