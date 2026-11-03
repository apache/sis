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
package org.apache.sis.storage.sql.feature;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.ResultSet;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.Dimensions;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.util.resources.Errors;


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
 * @see org.apache.sis.storage.sql.postgis.RasterGetter
 */
final class GeometryGetter<G, V extends G> extends ValueGetter<V> {
    /**
     * The factory to use for creating geometries from WKB definitions.
     */
    private final Geometries<G> geometryFactory;

    /**
     * The Coordinate Reference System if {@link InfoStatements} cannot map the SRID.
     * This is {@code null} if there is no default.
     */
    private final CoordinateReferenceSystem defaultCRS;

    /**
     * The way binary data are encoded in the geometry column.
     */
    private final BinaryEncoding encoding;

    /**
     * Whether to use binary (<abbr>WKB</abbr>) or textual (<abbr>WKT</abbr>).
     * In theory, the binary format should be more efficient.
     * But this is not always well supported.
     */
    private final GeometryEncoding format;

    /**
     * Creates a new reader. The same instance can be reused for parsing an arbitrary
     * number of geometries sharing the same default CRS.
     *
     * @param  geometryFactory  the factory to use for creating geometries from WKB definitions.
     * @param  geometryClass    the type of geometry to be returned by this {@code ValueGetter}.
     * @param  defaultCRS       the CRS to use if none can be mapped from the SRID, or {@code null} if none.
     * @param  encoding         the way binary data are encoded in the geometry column.
     * @param  format           whether to use <abbr>WKB</abbr> or <abbr>WKT</abbr>.
     */
    GeometryGetter(final Geometries<G> geometryFactory, final Class<V> geometryClass,
            final CoordinateReferenceSystem defaultCRS, final BinaryEncoding encoding,
            final GeometryEncoding format)
    {
        super(geometryClass);
        this.geometryFactory = geometryFactory;
        this.defaultCRS      = defaultCRS;
        this.encoding        = encoding;
        this.format          = format;
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
        final GeometryWrapper geom;
        int gpkgSrid = 0;           // A value ≤ 0 means "no CRS" as of `stmts.fetchCRS(int)` contract.
        switch (format) {
            default: {
                return null;
            }
            case WKT: {
                final String wkt = source.getString(columnIndex);
                if (wkt == null) return null;
                geom = geometryFactory.parseWKT(wkt);
                break;
            }
            case WKB: {
                final byte[] wkb = encoding.getBytes(source, columnIndex);
                if (wkb == null) return null;
                final ByteBuffer buffer = ByteBuffer.wrap(wkb);
                /*
                 * The bytes should describe a geometry encoded in Well Known Binary (WKB) format,
                 * but this implementation accepts also the Geopackage geometry encoding:
                 *
                 *     https://www.geopackage.org/spec140/index.html#gpb_spec
                 *
                 * This is still a geometry in WKB format, but preceded by a header of at least two 32-bits integers.
                 */
                if (wkb.length > 2*Integer.BYTES && wkb[0] == 'G' && wkb[1] == 'P') {
                    final int version = Byte.toUnsignedInt(wkb[2]);     // 8-bit unsigned integer, 0 = version 1
                    if (version != 0) {
                        throw new DataStoreContentException(Errors.forLocale(stmts.getLocale())
                                .getString(Errors.Keys.UnsupportedFormatVersion_2, "Geopackage", version));
                    }
                    final int     flags        = wkb[3];
                    final boolean bigEndian    = (flags & 0b000001) == 0;
                    final int     envelopeType = (flags & 0b001110) >> 1;
                //  final boolean isEmpty      = (flags & 0b010000) != 0;
                //  final boolean extendedType = (flags & 0b100000) != 0;
                    buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
                    gpkgSrid = buffer.getInt(Integer.BYTES);
                    // Skip header and envelope.
                    final int offset;
                    switch (envelopeType) {
                        case 0: offset = 2*Integer.BYTES;                  break;   // No envelope.
                        case 1: offset = 2*Integer.BYTES + 4*Double.BYTES; break;   // 2D envelope.
                        case 2:                                                     // 3D envelope with Z.
                        case 3: offset = 2*Integer.BYTES + 6*Double.BYTES; break;   // 3D envelope with M.
                        case 4: offset = 2*Integer.BYTES + 8*Double.BYTES; break;   // 4D envelope.
                        default: throw new DataStoreContentException(Errors.forLocale(stmts.getLocale())
                                    .getString(Errors.Keys.UnexpectedValueInElement_2, "envelope contents indicator"));
                    }
                    buffer.position(offset).order(ByteOrder.BIG_ENDIAN);
                }
                geom = geometryFactory.parseWKB(buffer);
                break;
            }
        }
        /*
         * Set the CRS. This is often a constant value defined for the whole column.
         * But some formats allow to specify a SRID individually on the geometry.
         */
        CoordinateReferenceSystem crs = defaultCRS;
        if (stmts != null) {
            crs = stmts.fetchCRS(geom.getSRID().orElse(gpkgSrid));
        }
        if (crs != null) {
            /*
            * The CRS built from the SRID may be two-dimensional while the geometry may have z or M coordinates.
            * For example, a GeoPackage `srs_id` references a horizontal CRS even if the `gpkg_geometry_columns`
            * declares that z values are mandatory. The adjustement below can add or remove CRS dimensions.
            * The vertical (z) dimension, if unspecified, defaults to ellipsoidal height (3D CS) in metres.
            * The measure (M) dimension has no default. If missing, caller may have a dimension mismatch.
             */
            final Dimensions dimensions = geom.getCoordinateDimensions();
            Adjusted adjusted = lastAdjusted;
            if (adjusted == null || (adjusted.source != crs || adjusted.dimensions != dimensions)) {
                lastAdjusted = adjusted = new Adjusted(dimensions, crs);
            }
            geom.setCoordinateReferenceSystem(adjusted.target);
        }
        return valueType.cast(geometryFactory.getGeometry(geom));
    }

    /**
     * The last <abbr>CRS</abbr> computed by an adjustment of the number of dimensions, or {@code null} if none.
     */
    private volatile Adjusted lastAdjusted;

    /**
     * The last <abbr>CRS</abbr> computed by an adjustment of the number of dimensions.
     * Cached because all geometries of a column usually have the same <abbr>SRID</abbr> and number of dimensions.
     * Instances are immutable for allowing the use of this cache by concurrent threads.
     */
    private static final class Adjusted {
        /** The dimensions used for computing the target <abbr>CRS</abbr>. */
        final Dimensions dimensions;

        /** The <abbr>CRS</abbr> before and after adjustment for the number of dimensions. */
        final CoordinateReferenceSystem source, target;

        /** Creates a new cache entry. */
        Adjusted(final Dimensions dimensions, final CoordinateReferenceSystem source) throws FactoryException {
            this.dimensions = dimensions;
            this.source     = source;
            this.target     = dimensions.adjust(source, null, null, null);
        }
    }
}
