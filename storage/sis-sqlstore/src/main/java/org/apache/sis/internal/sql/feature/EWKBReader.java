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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.math.Vector;
import org.apache.sis.setup.GeometryLibrary;

import static java.lang.Character.digit;
import static org.apache.sis.util.ArgumentChecks.ensureNonEmpty;

/**
 * PostGIS EWKB Geometry reader/write classes.
 * http://postgis.net/docs/using_postgis_dbmanagement.html#EWKB_EWKT
 *
 * This format is the natural form returned by a query selection a geometry field
 * whithout using any ST_X method.
 *
 * @implNote This format is almost equivalent to standard WKB. In fact, it should already be compatible with standard
 * WKB, as the only added  component by EWKB is srid, and it's hidden in geometry type using bitmask.
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
class EWKBReader {

    private static final int MASK_Z         = 0x80000000;
    private static final int MASK_M         = 0x40000000;
    private static final int MASK_SRID      = 0x20000000;
    private static final int MASK_GEOMTYPE  = 0x1FFFFFFF;

    // Copied from PostGIS JDBC source code to avoid dependency for too little information.
    private static final int POINT = 1;
    private static final int LINESTRING = 2;
    private static final int POLYGON = 3;
    private static final int MULTIPOINT = 4;
    private static final int MULTILINESTRING = 5;
    private static final int MULTIPOLYGON = 6;
    private static final int GEOMETRYCOLLECTION = 7;

    final Geometries factory;

    private final Function<ByteBuffer, ?> decoder;

    EWKBReader() {
        this((GeometryLibrary) null);
    }

    EWKBReader(GeometryLibrary library) {
        this(Geometries.implementation(library));
    }

    EWKBReader(Geometries geometryFactory) {
        this(geometryFactory, bytes -> new Reader(geometryFactory, bytes).read());
    }

    private EWKBReader(final Geometries factory, Function<ByteBuffer, ?> decoder) {
        this.factory = factory;
        this.decoder = decoder;
    }

    /**
     *
     * @param defaultCrs The coordinate reference system to associate to each geometry.
     * @return A NEW instance of reader, with a fixed CRS resolution, applying constant value to all read geometries.
     */
    public EWKBReader forCrs(final CoordinateReferenceSystem defaultCrs) {
        if (defaultCrs == null) return new EWKBReader(factory);
        return new EWKBReader(factory, bytes -> {
            final Object geom = new Reader(factory, bytes).read();
            Geometries.wrap(geom).ifPresent((w) -> w.setCoordinateReferenceSystem(defaultCrs));
            return geom;
        });
    }

    public EWKBReader withResolver(final IntFunction<CoordinateReferenceSystem> fromPgSridToCrs) {
        return new EWKBReader(factory, bytes -> {
            final Reader reader = new Reader(factory, bytes);
            final Object geom = reader.read();
            if (reader.srid > 0) {
                final CoordinateReferenceSystem crs = fromPgSridToCrs.apply(reader.srid);
                if (crs != null) {
                    Geometries.wrap(geom).ifPresent((w) -> w.setCoordinateReferenceSystem(crs));
                }
            }
            return geom;
        });
    }

    Object readHexa(final String hexaEWkb) {
        return read(decodeHex(hexaEWkb));
    }

    Object read(final byte[] eWkb) {
        return decoder.apply(ByteBuffer.wrap(eWkb));
    }

    Object read(final ByteBuffer eWkb) {
        return decoder.apply(eWkb);
    }

    private static final class Reader {
        final Geometries factory;
        final ByteBuffer buffer;
        final int geomType;
        final int dimension;
        final int srid;

        private Reader(Geometries factory, ByteBuffer buffer) {
            this.factory = factory;
            final byte endianess = buffer.get();
            if (isLittleEndian(endianess)) {
                this.buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
            } else this.buffer = buffer;
            final int     flags     = buffer.getInt();
            final boolean flagZ     = (flags & MASK_Z)    != 0;
            final boolean flagM     = (flags & MASK_M)    != 0;
            final boolean flagSRID  = (flags & MASK_SRID) != 0;
                          geomType  = (flags & MASK_GEOMTYPE);
                          dimension = 2 + ((flagZ) ? 1 : 0) + ((flagM) ? 1 : 0);
                          srid      = flagSRID ? buffer.getInt() : 0;
        }

        Object read() {
            switch (geomType) {
                case POINT:              return readPoint();
                case LINESTRING:         return readLineString();
                case POLYGON:            return readPolygon();
                case MULTIPOINT:         return readMultiPoint();
                case MULTILINESTRING:    return readMultiLineString();
                case MULTIPOLYGON:       return readMultiPolygon();
                case GEOMETRYCOLLECTION: return readCollection();
            }
            throw new UnsupportedOperationException("Unsupported geometry type: "+geomType);
        }

        private Object readMultiLineString() {
            final Object geometry = Geometries.mergePolylines(IntStream.range(0, readCount())
                    .mapToObj(i -> new Reader(factory, buffer).read())
                    .iterator());
            if (geometry != null) {
                return geometry;
            }
            throw new IllegalStateException("No geometry decoded");
        }

        private Object readMultiPolygon() {
            final Object[] polygons = new Object[readCount()];
            for (int i=0; i<polygons.length; i++) {
                polygons[i] = new Reader(factory, buffer).read();
            }
            return factory.createMultiPolygon(polygons).implementation();
        }

        private Object readCollection() {
            throw new UnsupportedOperationException();
        }

        final Object readPoint() {
            final double[] ordinates = readCoordinateSequence(1);
            // TODO: we lose information here ! We need to evolve Geometry API.
            return factory.createPoint(ordinates[0], ordinates[1]);
        }

        final Object readLineString() {
            final double[] ordinates = readCoordinateSequence();
            return factory.createPolyline(false, dimension, Vector.create(ordinates));
        }

        private Object readPolygon() {
            final int nbRings=readCount();
            final Vector outerShell = Vector.create(readCoordinateSequence());

            final double[] nans = new double[dimension];
            Arrays.fill(nans, Double.NaN);
            final Vector separator = Vector.create(nans);
            final Vector[] allShells = new Vector[Math.addExact(nbRings, nbRings -1)]; // include ring separators
            allShells[0] = outerShell;
            for (int i = 1 ; i < nbRings ;) {
                allShells[i++] = separator;
                allShells[i++] = Vector.create(readCoordinateSequence());
            }
            return factory.createPolyline(true, dimension, outerShell);
        }

        private double[] readMultiPoint() {
            throw new UnsupportedOperationException();
        }

        private double[] readCoordinateSequence() {
            return readCoordinateSequence(readCount());
        }

        private double[] readCoordinateSequence(int nbPts) {
            final double[] brutPoint = new double[Math.multiplyExact(dimension, nbPts)];
            for (int i = 0 ; i < brutPoint.length ; i++) brutPoint[i] = buffer.getDouble();
            return brutPoint;
        }

        /**
         * @implNote WKB specification declares lengths as uint32. However, the way to handle it in Java would be to
         * return a long value, which is not possible anyway, because current implementation needs to put all geometry
         * data in  memory, in an array whose number of elements is limited to {@link Integer#MAX_VALUE}. So for now,
         * we will just ensure that read value is compatible with our limitations.
         *
         * For details, see <a href="https://www.ibm.com/support/knowledgecenter/SSGU8G_14.1.0/com.ibm.spatial.doc/ids_spat_285.htm">IBM</a>
         * or <a href="https://trac.osgeo.org/postgis/browser/trunk/doc/ZMSgeoms.txt">OSGEO</a> documentation.
         *
         * @return read count.
         * @throws IllegalStateException If read count is 0 or above {@link Integer#MAX_VALUE}.
         */
        private int readCount() {
            final int count = buffer.getInt();
            if (count == 0) throw new IllegalStateException("Read a 0 point/geometry count in WKB.");
            else if (count < 0) throw new IllegalStateException("Read a count overflowing Java integer max value: "+Integer.toUnsignedLong(count));
            return count;
        }
    }

    private static boolean isLittleEndian(byte endianess) {
        return endianess == 1; // org.postgis.binary.ValueGetter.NDR.NUMBER
    }

    /**
     * Convert a text representing an hexadecimal set of values (no separator, each 2 characters form a value).
     *
     * @param hexa The hexadecimal values to decode. Should neither be null nor empty.
     * @return Real values encoded by input hexadecimal text. Never null, never empty.
     */
    static byte[] decodeHex(String hexa) {
        ensureNonEmpty("Hexadecimal text", hexa);
        int len = hexa.length();
        // Handle odd length by considering last character as a lone value
        byte[] data = new byte[(len+1) / 2];
        int limit = (len % 2 == 0) ? len : len - 1;
        for (int i = 0, j=0 ; i < limit ; ) {
            data[j++] = (byte) ((digit(hexa.charAt(i++), 16) << 4)
                    + digit(hexa.charAt(i++), 16));
        }

        if (limit < len) data[data.length - 1] = (byte) digit(hexa.charAt(limit), 16);

        return data;
    }
}
