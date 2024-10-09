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

import org.locationtech.jts.geom.*;


/**
 *
 * @author Hilmi Bouallegue (Geomatys)
 */
public enum OGRwkbGeometryType {
    wkbUnknown(Geometry.class),
    wkbPoint(Point.class),
    wkbLineString(LineString.class),
    wkbPolygon(Polygon.class),
    wkbMultiPoint(MultiPoint.class),
    wkbMultiLineString(MultiLineString.class),
    wkbMultiPolygon(MultiPolygon.class),
    wkbGeometryCollection(GeometryCollection.class),
    wkbCircularString(Geometry.class),
    wkbCompoundCurve(Geometry.class),
    wkbCurvePolygon(Geometry.class),
    wkbMultiCurve(Geometry.class),
    wkbMultiSurface(Geometry.class),
    wkbCurve(Geometry.class),
    wkbSurface(Geometry.class),
    wkbPolyhedralSurface(Geometry.class),
    wkbTIN(Geometry.class),
    wkbTriangle(Geometry.class),
    wkbNone(Geometry.class),
    wkbLinearRing(Geometry.class),
    wkbCircularStringZ(Geometry.class),
    wkbCompoundCurveZ(Geometry.class),
    wkbCurvePolygonZ(Geometry.class),
    wkbMultiCurveZ(Geometry.class),
    wkbMultiSurfaceZ(Geometry.class),
    wkbCurveZ(Geometry.class),
    wkbSurfaceZ(Geometry.class),
    wkbPolyhedralSurfaceZ(Geometry.class),
    wkbTINZ(Geometry.class),
    wkbTriangleZ(Geometry.class),
    wkbPointM(Geometry.class),
    wkbLineStringM(Geometry.class),
    wkbPolygonM(Geometry.class),
    wkbMultiPointM(Geometry.class),
    wkbMultiLineStringM(Geometry.class),
    wkbMultiPolygonM(Geometry.class),
    wkbGeometryCollectionM(Geometry.class),
    wkbCircularStringM(Geometry.class),
    wkbCompoundCurveM(Geometry.class),
    wkbCurvePolygonM(Geometry.class),
    wkbMultiCurveM(Geometry.class),
    wkbMultiSurfaceM(Geometry.class),
    wkbCurveM(Geometry.class),
    wkbSurfaceM(Geometry.class),
    wkbPolyhedralSurfaceM(Geometry.class),
    wkbTINM(Geometry.class),
    wkbTriangleM(Geometry.class),
    wkbPointZM(Geometry.class),
    wkbLineStringZM(Geometry.class),
    wkbPolygonZM(Geometry.class),
    wkbMultiPointZM(Geometry.class),
    wkbMultiLineStringZM(Geometry.class),
    wkbMultiPolygonZM(Geometry.class),
    wkbGeometryCollectionZM(Geometry.class),
    wkbCircularStringZM(Geometry.class),
    wkbCompoundCurveZM(Geometry.class),
    wkbCurvePolygonZM(Geometry.class),
    wkbMultiCurveZM(Geometry.class),
    wkbMultiSurfaceZM(Geometry.class),
    wkbCurveZM(Geometry.class),
    wkbSurfaceZM(Geometry.class),
    wkbPolyhedralSurfaceZM(Geometry.class),
    wkbTINZM(Geometry.class),
    wkbTriangleZM(Geometry.class),
    wkbPoint25D(Geometry.class),
    wkbLineString25D(Geometry.class),
    wkbPolygon25D(Geometry.class),
    wkbMultiPoint25D(Geometry.class),
    wkbMultiLineString25D(Geometry.class),
    wkbMultiPolygon25D(Geometry.class),
    wkbGeometryCollection25D(Geometry.class);

    private final Class javaClass;

    OGRwkbGeometryType(Class javaClass){
        this.javaClass =javaClass;
    }

    public Class getJavaClass(){
        return javaClass;
    }

    public static OGRwkbGeometryType valueOf(int value) {
        switch (value) {
            case 0 : return wkbUnknown;
            case 1 : return wkbPoint;
            case 2 : return wkbLineString;
            case 3 : return wkbPolygon;
            case 4 : return wkbMultiPoint;
            case 5 : return wkbMultiLineString;
            case 6 : return wkbMultiPolygon;
            case 7 : return wkbGeometryCollection;
            case 8 : return wkbCircularString;
            case 9 : return wkbCompoundCurve;
            case 10 : return wkbCurvePolygon;
            case 11 : return wkbMultiCurve;
            case 12 : return wkbMultiSurface;
            case 13 : return wkbCurve;
            case 14 : return wkbSurface;
            case 15 : return wkbPolyhedralSurface;
            case 16 : return wkbTIN;
            case 17 : return wkbTriangle;
            case 18 : return wkbNone;
            case 19 : return wkbLinearRing;
            case 20 : return wkbCircularStringZ;
            case 21 : return wkbCompoundCurveZ;
            case 22 : return wkbCurvePolygonZ;
            case 23 : return wkbMultiCurveZ;
            case 24 : return wkbMultiSurfaceZ;
            case 25 : return wkbCurveZ;
            case 26 : return wkbSurfaceZ;
            case 27 : return wkbPolyhedralSurfaceZ;
            case 28 : return wkbTINZ;
            case 29 : return wkbTriangleZ;
            case 30 : return wkbPointM;
            case 31 : return wkbLineStringM;
            case 32 : return wkbPolygonM;
            case 33 : return wkbMultiPointM;
            case 34 : return wkbMultiLineStringM;
            case 35 : return wkbMultiPolygonM;
            case 36 : return wkbGeometryCollectionM;
            case 37 : return wkbCircularStringM;
            case 38 : return wkbCompoundCurveM;
            case 39 : return wkbCurvePolygonM;
            case 40 : return wkbMultiCurveM;
            case 41 : return wkbMultiSurfaceM;
            case 42 : return wkbCurveM;
            case 43 : return wkbSurfaceM;
            case 44 : return wkbPolyhedralSurfaceM;
            case 45 : return wkbTINM;
            case 46 : return wkbTriangleM;
            case 47 : return wkbPointZM;
            case 48 : return wkbLineStringZM;
            case 49 : return wkbPolygonZM;
            case 50 : return wkbMultiPointZM;
            case 51 : return wkbMultiLineStringZM;
            case 52 : return wkbMultiPolygonZM;
            case 53 : return wkbGeometryCollectionZM;
            case 54 : return wkbCircularStringZM;
            case 55 : return wkbCompoundCurveZM;
            case 56 : return wkbCurvePolygonZM;
            case 57 : return wkbMultiCurveZM;
            case 58 : return wkbMultiSurfaceZM;
            case 59 : return wkbCurveZM;
            case 60 : return wkbSurfaceZM;
            case 61 : return wkbPolyhedralSurfaceZM;
            case 62 : return wkbTINZM;
            case 63 : return wkbTriangleZM;
            case 64 : return wkbPoint25D;
            case 65 : return wkbLineString25D;
            case 66 : return wkbPolygon25D;
            case 67 : return wkbMultiPoint25D;
            case 68 : return wkbMultiLineString25D;
            case 69 : return wkbMultiPolygon25D;
            case 70 : return wkbGeometryCollection25D;
        }
        throw new IllegalArgumentException("Unknown type " + value);
    }
}
