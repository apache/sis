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

import java.util.Optional;
import java.util.OptionalInt;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.feature.Geometries;
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
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <G> the type of geometry objects created by the factory.
 * @param <V> the type of geometry objects returned by this getter.
 *
 * @version 1.1
 * @since   1.1
 * @module
 */
final class EWKBReader<G, V extends G> extends ValueGetter<V> {
    /**
     * The factory to use for creating geometries from WKB definitions.
     */
    private final Geometries<G> geometryFactory;

    /**
     * The mapper to use for resolving a Spatial Reference Identifier (SRID) integer
     * as Coordinate Reference System (CRS) object.
     * This is {@code null} is there is no mapping to apply.
     */
    private InfoStatements fromSridToCRS;

    /**
     * The Coordinate Reference System if {@link #fromSridToCRS} can not map the SRID.
     * This is {@code null} if there is no default.
     */
    private final CoordinateReferenceSystem defaultCRS;

    /**
     * Whether binary data are encoded in an hexadecimal string.
     */
    private final boolean hexadecimal;

    /**
     * Creates a new reader. The same instance can be reused for parsing an arbitrary
     * amount of geometries sharing the same CRS.
     *
     * @param  geometryFactory  the factory to use for creating geometries from WKB definitions.
     * @param  geometryClass    the type of geometry to be returned by this {@code ValueGetter}.
     * @param  defaultCRS       the CRS to use if none can be mapped from the SRID, or {@code null} if none.
     * @param  hexadecimal      whether binary data are encoded in an hexadecimal string.
     * @return a WKB reader resolving SRID with the specified mapper and default CRS.
     */
    EWKBReader(final Geometries<G> geometryFactory, final Class<V> geometryClass,
            final CoordinateReferenceSystem defaultCRS, final boolean hexadecimal)
    {
        super(geometryClass);
        this.geometryFactory = geometryFactory;
        this.defaultCRS      = defaultCRS;
        this.hexadecimal     = hexadecimal;
    }

    /**
     * Sets the mapper to use for resolving a Spatial Reference Identifier (SRID) integer
     * as Coordinate Reference System (CRS) object.
     *
     * @param  fromSridToCRS  mapper for resolving SRID integers as CRS objects, or {@code null} if none.
     */
    final void setSridResolver(final InfoStatements fromSridToCRS) {
        this.fromSridToCRS = fromSridToCRS;
    }

    /**
     * Returns the default coordinate reference system for this column.
     * The default CRS is declared in the {@code "GEOMETRY_COLUMNS"} table.
     */
    @Override
    public Optional<CoordinateReferenceSystem> getCRS() {
        return Optional.ofNullable(defaultCRS);
    }

    /**
     * Gets the value in the column at specified index.
     * The given result set must have its cursor position on the line to read.
     * This method does not modify the cursor position.
     *
     * @param  source       the result set from which to get the value.
     * @param  columnIndex  index of the column in which to get the value.
     * @return geometry value in the given column. May be {@code null}.
     * @throws Exception if an error occurred. May be an SQL error, a WKB parsing error, <i>etc.</i>
     *
     * @todo it is not sure that database driver return WKB, so we should
     *       find a way to ensure that SQL queries use {@code ST_AsBinary} function.
     */
    @Override
    public V getValue(final ResultSet source, final int columnIndex) throws Exception {
        final GeometryWrapper<G> geom;
        if (hexadecimal) {
            final String wkb = source.getString(columnIndex);
            if (wkb == null) return null;
            geom = readHexa(wkb);
        } else {
            final byte[] wkb = source.getBytes(columnIndex);
            if (wkb == null) return null;
            geom = read(wkb);
        }
        return valueType.cast(geom.implementation());
    }

    /**
     * Parses a WKB encoded as hexadecimal numbers in a character string.
     * Each byte uses 2 characters. No separator is allowed between bytes.
     *
     * @param  wkb  the hexadecimal values to decode. Should neither be null nor empty.
     * @return geometry parsed from the given hexadecimal text. Never null, never empty.
     * @throws Exception if the WKB can not be parsed. The exception type depends on the geometry implementation.
     */
    final GeometryWrapper<G> readHexa(final String wkb) throws Exception {
        final int length = wkb.length();
        if ((length & 1) != 0) {
            throw new DataStoreContentException(Errors.format(Errors.Keys.OddArrayLength_1, "wkb"));
        }
        final byte[] data = new byte[length >>> 1];
        for (int i=0; i<length;) {
            data[i >>> 1] = (byte) ((digit(wkb.charAt(i++)) << 4) | digit(wkb.charAt(i++)));
        }
        return read(data);
    }

    /**
     * Returns the numerical value of the given hexadecimal digit.
     * The hexadecimal digit can be the decimal digits 0 to 9, or the letters A to F ignoring case.
     *
     * <div class="note"><b>Implementation note:</b>
     * we do not use {@link Character#digit(char, int)} because that method handled a large
     * range of Unicode characters, which is a wider scope than what is intended here.</div>
     *
     * @param  c  the hexadecimal digit.
     * @return numerical value of given digit.
     * @throws DataStoreContentException if the given character is not a hexadecimal digit.
     */
    private static int digit(final char c) throws DataStoreContentException {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return c - ('A' - 10);
        if (c >= 'a' && c <= 'f') return c - ('a' - 10);
        throw new DataStoreContentException(Errors.format(Errors.Keys.CanNotParse_1, String.valueOf(c)));
    }

    /**
     * Parses a WKB stored in the given byte array.
     *
     * @param  wkb  the array containing the WKB to decode. Should neither be null nor empty.
     * @return geometry parsed from the given array of bytes. Never null, never empty.
     * @throws Exception if the WKB can not be parsed. The exception type depends on the geometry implementation.
     */
    final GeometryWrapper<G> read(final byte[] wkb) throws Exception {
        final GeometryWrapper<G> wrapper = geometryFactory.parseWKB(ByteBuffer.wrap(wkb));
        CoordinateReferenceSystem crs = defaultCRS;
        if (fromSridToCRS != null) {
            final OptionalInt srid = wrapper.getSRID();
            if (srid.isPresent()) {
                crs = fromSridToCRS.fetchCRS(srid.getAsInt());
            }
        }
        if (crs != null) {
            wrapper.setCoordinateReferenceSystem(crs);
        }
        return wrapper;
    }
}
