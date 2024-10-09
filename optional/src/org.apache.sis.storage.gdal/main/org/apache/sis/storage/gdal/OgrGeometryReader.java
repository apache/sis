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
package org.apache.sis.storage.gdal;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.panama.NativeFunctions;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class OgrGeometryReader implements AutoCloseable {

    private static final GeometryFactory GF = new GeometryFactory();
    //reuse direct buffer for better performances
    private MemorySegment px;
    private MemorySegment py;
    private ByteBuffer bufferx;
    private ByteBuffer buffery;

    private final Arena arena = Arena.ofConfined();
    private final GDAL gdal;

    OgrGeometryReader(GDAL gdal) {
        this.gdal = gdal;
    }

    private void resizeBuffer(int nbPoint){
        final int size = nbPoint*2*8;
        if (px == null || px.byteSize() < size) {
            px = arena.allocate(size);
            py = px.asSlice(8);
            bufferx = px.asByteBuffer();
            buffery = py.asByteBuffer();
        }
    }

    public Geometry toGeometry(MemorySegment ogrGeom) throws DataStoreException {
        if (ogrGeom == null) return null;

        try {
            MemorySegment NULL = MemorySegment.ofAddress(0);
            final String type = NativeFunctions.toString( (MemorySegment)gdal.ogrGeometryGetGeometryName.invokeExact(ogrGeom));

            switch (type) {
                case "POINT" : {
                    final double[] coords = new double[2];
                    resizeBuffer(1);
                    final int returnedNbPoints = (int) gdal.ogrGeometryGetPoints.invokeExact(ogrGeom,px,16,py,16,NULL,0);

                    bufferx.position(0);
                    bufferx.order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().get(coords);

                    final CoordinateSequence cs = new PackedCoordinateSequence.Double(coords,2,0);
                    return GF.createPoint(cs);
                }
                case "MULTIPOINT" : {
                    final int nbPoint = (int) gdal.ogrGeometryGetPointCount.invokeExact(ogrGeom);
                    final double[] coords = new double[nbPoint*2];
                    if (nbPoint != 0) {
                        resizeBuffer(nbPoint);
                        final int returnedNbPoints = (int) gdal.ogrGeometryGetPoints.invokeExact(ogrGeom,px,16,py,16,NULL,0);

                        bufferx.position(0);
                        bufferx.order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().get(coords);
                    }
                    final CoordinateSequence cs = new PackedCoordinateSequence.Double(coords,2,0);
                    return GF.createMultiPoint(cs);
                }
                case "LINESTRING" : {
                    final int nbPoint = (int) gdal.ogrGeometryGetPointCount.invokeExact(ogrGeom);
                    final double[] coords = new double[nbPoint*2];
                    resizeBuffer(nbPoint);
                    final int returnedNbPoints = (int) gdal.ogrGeometryGetPoints.invokeExact(ogrGeom,px,16,py,16,NULL,0);

                    bufferx.position(0);
                    bufferx.order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().get(coords);

                    if (nbPoint==1) {
                        //duplicate first point, JTS need at least 2 coordinates
                        final double[] cp = new double[]{coords[0],coords[1],coords[0],coords[1]};
                        final CoordinateSequence cs = new PackedCoordinateSequence.Double(cp,2,0);
                        return GF.createLineString(cs);
                    } else {
                        final CoordinateSequence cs = new PackedCoordinateSequence.Double(coords,2,0);
                        return GF.createLineString(cs);
                    }
                }
                case "LINEARRING" : {
                    final int nbPoint = (int) gdal.ogrGeometryGetPointCount.invokeExact(ogrGeom);
                    double[] coords = new double[(nbPoint+1)*2];
                    resizeBuffer(nbPoint+1);
                    final int returnedNbPoints = (int) gdal.ogrGeometryGetPoints.invokeExact(ogrGeom,px,16,py,16,NULL,0);

                    bufferx.position(0);
                    bufferx.order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().get(coords,0,nbPoint*2);

                    //JTS needs at least 3+1 points
                    if (coords.length<8) {
                        double[] cs = Arrays.copyOf(coords, 8);
                        for(int i=nbPoint;i<4;i++){
                            cs[i*2+0] = coords[0];
                            cs[i*2+1] = coords[1];
                        }
                        coords = cs;
                    }

                    //duplicate first to last point this is a JTS constraint
                    coords[coords.length-2] = coords[0];
                    coords[coords.length-1] = coords[1];

                    final CoordinateSequence cs = new PackedCoordinateSequence.Double(coords,2,0);
                    return GF.createLinearRing(cs);
                }
                case "MULTILINESTRING" : {
                    final int nbGeom = (int) gdal.ogrGeometryGetGeometryCount.invokeExact(ogrGeom);
                    final LineString[] children = new LineString[nbGeom];
                    for (int i=0;i<nbGeom;i++) {
                        final MemorySegment sub = (MemorySegment) gdal.ogrGeometryGetGeometryRef.invokeExact(ogrGeom, i);
                        children[i] = (LineString) toGeometry(sub);
                    }
                    return GF.createMultiLineString(children);
                }
                case "POLYGON" : {
                    final int nbGeom = (int) gdal.ogrGeometryGetGeometryCount.invokeExact(ogrGeom);
                    //first is the exterior ring, next ones are the interiors
                    LinearRing exterior = null;
                    final LinearRing[] interiors = new LinearRing[nbGeom-1];
                    for (int i=0;i<nbGeom;i++) {
                        final MemorySegment sub = (MemorySegment) gdal.ogrGeometryGetGeometryRef.invokeExact(ogrGeom, i);
                        if (i==0) exterior = (LinearRing) toGeometry(sub);
                        else interiors[i-1] = (LinearRing) toGeometry(sub);
                    }
                    return GF.createPolygon(exterior,interiors);
                }
                case "MULTIPOLYGON" : {
                    final int nbGeom = (int) gdal.ogrGeometryGetGeometryCount.invokeExact(ogrGeom);
                    final Polygon[] children = new Polygon[nbGeom];
                    for (int i=0;i<nbGeom;i++) {
                        final MemorySegment sub = (MemorySegment) gdal.ogrGeometryGetGeometryRef.invokeExact(ogrGeom, i);
                        children[i] = (Polygon) toGeometry(sub);
                    }
                    return GF.createMultiPolygon(children);
                }
                case "GEOMETRYCOLLECTION" : {
                    final int nbGeom = (int) gdal.ogrGeometryGetGeometryCount.invokeExact(ogrGeom);
                    final Geometry[] children = new Geometry[nbGeom];
                    for (int i=0;i<nbGeom;i++) {
                        final MemorySegment sub = (MemorySegment) gdal.ogrGeometryGetGeometryRef.invokeExact(ogrGeom, i);
                        children[i] = toGeometry(sub);
                    }
                    return GF.createGeometryCollection(children);
                }
                case "GEOMETRY" :
                case "CIRCULARSTRING" :
                case "COMPOUNDCURVE" :
                case "CURVEPOLYGON" :
                case "MULTICURVE" :
                case "MULTISURFACE" :
                case "CURVE" :
                case "SURFACE" :
                case "POLYHEDRALSURFACE" :
                case "TIN" :
                case "TRIANGLE" :
                default : {
                    throw new DataStoreException("Unsupported geometry type "+type);
                }
            }
        } catch (Throwable e) {
            throw GDAL.propagate(e);
        }

    }

    @Override
    public void close() {
        arena.close();
    }

}
