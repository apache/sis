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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.postgis.binary.ByteGetter;
import org.postgis.binary.ValueGetter;
import static org.postgis.Geometry.*;

/**
 * PostGIS Hexa-EWKB Geometry reader/write classes.
 *
 * This format is the natural form returned by a query selection a geometry
 * field whithout using any ST_X method.
 *
 * @see <a href="http://postgis.net/docs/using_postgis_dbmanagement.html#EWKB_EWKT">4.2.1. PostGIS EWKB and EWKT</a>
 *
 * @author Johann Sorel (Geomatys)
 * @module
 */
final class PostgisHexEWKB {

    private static final int MASK_Z = 0x80000000;
    private static final int MASK_M = 0x40000000;
    private static final int MASK_SRID = 0x20000000;
    private static final int MASK_GEOMTYPE = 0x1FFFFFFF;

    private final GeometryFactory gf;

    PostgisHexEWKB(final GeometryFactory gf) {
        this.gf = gf;
    }

    public Geometry read(final String value) {
        if (value == null) {
            return null;
        }

        final ByteGetter.StringByteGetter bytes = new ByteGetter.StringByteGetter(value);
        final ValueGetter vg;
        final int endianess = bytes.get(0);
        if (endianess == ValueGetter.XDR.NUMBER) {
            vg = new ValueGetter.XDR(bytes);
        } else if (endianess == ValueGetter.NDR.NUMBER) {
            vg = new ValueGetter.NDR(bytes);
        } else {
            throw new IllegalArgumentException("Illegal endianess value : " + endianess);
        }

        return readGeometry(vg, 0);
    }

    private Geometry readGeometry(final ValueGetter data, int srid) {
        byte endian = data.getByte(); // skip and test endian flag
        if (endian != data.endian) {
            throw new IllegalArgumentException("Endian inconsistency!");
        }

        //parse flags
        final int flags        = data.getInt();
        final boolean flagZ    = (flags & MASK_Z) != 0;
        final boolean flagM    = (flags & MASK_M) != 0;
        final boolean flagSRID = (flags & MASK_SRID) != 0;
        final int geomType     = (flags & MASK_GEOMTYPE);
        final int nbDim        = 2 + ((flagZ) ? 1 : 0) + ((flagM) ? 1 : 0);

        if (flagSRID) {
            srid = data.getInt();
        }

        final Geometry geom;
        switch (geomType) {
            case POINT:              geom = readPoint(data, nbDim);           break;
            case LINESTRING:         geom = readLineString(data, nbDim);      break;
            case POLYGON:            geom = readPolygon(data, nbDim, srid);   break;
            case MULTIPOINT:         geom = readMultiPoint(data, srid);       break;
            case MULTILINESTRING:    geom = readMultiLineString(data, srid);  break;
            case MULTIPOLYGON:       geom = readMultiPolygon(data, srid);     break;
            case GEOMETRYCOLLECTION: geom = readCollection(data, srid);       break;
            default: throw new IllegalArgumentException("Unknown geometry type : "+geomType);
        }

        geom.setSRID(srid);
        return geom;
    }

    private Point readPoint(final ValueGetter data, final int nbDim) {
        switch (nbDim) {
            case 2:
                return gf.createPoint(new Coordinate(data.getDouble(), data.getDouble()));
            case 3:
                return gf.createPoint(new Coordinate(data.getDouble(), data.getDouble(), data.getDouble()));
            case 4:
                final CoordinateSequence cs = new PackedCoordinateSequence.Double(1, nbDim, 0);
                cs.setOrdinate(0, 0, data.getDouble());
                cs.setOrdinate(0, 1, data.getDouble());
                cs.setOrdinate(0, 2, data.getDouble());
                cs.setOrdinate(0, 3, data.getDouble());
                return gf.createPoint(cs);
            default:
                throw new IllegalArgumentException("Invalid dimension number : " + nbDim);
        }
    }

    private CoordinateSequence readCS(final ValueGetter data, final int nbDim) {
        final int nb = data.getInt();
        final CoordinateSequence cs = new PackedCoordinateSequence.Double(nb, nbDim, 0);
        for (int index = 0; index < nb; index++) {
            for (int ordinal = 0; ordinal < nbDim; ordinal++) {
                cs.setOrdinate(index, ordinal, data.getDouble());
            }
        }
        return cs;
    }

    private MultiPoint readMultiPoint(final ValueGetter data, final int srid) {
        final Point[] geoms = new Point[data.getInt()];
        for (int i = 0; i < geoms.length; i++) {
            geoms[i] = (Point) readGeometry(data, srid);
        }
        return gf.createMultiPoint(geoms);
    }

    private LineString readLineString(final ValueGetter data, final int nbDim) {
        return gf.createLineString(readCS(data, nbDim));
    }

    private LinearRing readLinearRing(final ValueGetter data, final int nbDim) {
        return gf.createLinearRing(readCS(data, nbDim));
    }

    private Polygon readPolygon(final ValueGetter data, final int nbDim, final int srid) {
        final LinearRing[] inners = new LinearRing[data.getInt() - 1];
        final LinearRing outter = readLinearRing(data, nbDim);
        outter.setSRID(srid);
        for (int i = 0; i < inners.length; i++) {
            inners[i] = readLinearRing(data, nbDim);
            inners[i].setSRID(srid);
        }
        return gf.createPolygon(outter, inners);
    }

    private MultiLineString readMultiLineString(final ValueGetter data, final int srid) {
        final LineString[] geoms = new LineString[data.getInt()];
        for (int i = 0; i < geoms.length; i++) {
            geoms[i] = (LineString) readGeometry(data, srid);
        }
        return gf.createMultiLineString(geoms);
    }

    private MultiPolygon readMultiPolygon(final ValueGetter data, final int srid) {
        final Polygon[] geoms = new Polygon[data.getInt()];
        for (int i = 0; i < geoms.length; i++) {
            geoms[i] = (Polygon) readGeometry(data, srid);
        }
        return gf.createMultiPolygon(geoms);
    }

    private GeometryCollection readCollection(final ValueGetter data, final int srid) {
        final Geometry[] geoms = new Geometry[data.getInt()];
        for (int i = 0; i < geoms.length; i++) {
            geoms[i] = (Point) readGeometry(data, srid);
        }
        return gf.createGeometryCollection(geoms);
    }

}
