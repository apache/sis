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
package org.apache.sis.geometries;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.conics.Circle;
import org.apache.sis.geometries.conics.CircularString;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.geometries.math.TupleArray;
import org.apache.sis.geometries.math.TupleArrays;
import org.apache.sis.geometries.internal.shared.ArraySequence;
import org.apache.sis.geometries.internal.shared.DefaultGeometryCollection;
import org.apache.sis.geometries.internal.shared.DefaultLineString;
import org.apache.sis.geometries.internal.shared.DefaultLinearRing;
import org.apache.sis.geometries.internal.shared.DefaultMultiLineString;
import org.apache.sis.geometries.internal.shared.DefaultMultiPoint;
import org.apache.sis.geometries.internal.shared.DefaultMultiPolygon;
import org.apache.sis.geometries.internal.shared.DefaultMultiSurface;
import org.apache.sis.geometries.internal.shared.DefaultPoint;
import org.apache.sis.geometries.internal.shared.DefaultPolygon;
import org.apache.sis.geometries.internal.shared.DefaultTriangle;
import org.apache.sis.geometries.spirals.Clothoid;
import org.apache.sis.geometry.wrapper.Capability;
import org.apache.sis.geometry.wrapper.Dimensions;
import org.apache.sis.geometry.wrapper.GeometryType;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.setup.GeometryLibrary;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GeometryFactory extends org.apache.sis.geometry.wrapper.Geometries<Geometry> {

    public static GeometryFactory INSTANCE = new GeometryFactory();

    private GeometryFactory(){
        super(GeometryLibrary.SIS, Geometry.class, Point.class);
    }

    public static Point createPoint(CoordinateReferenceSystem crs) {
        return new DefaultPoint(crs);
    }

    public static Point createPoint(CoordinateReferenceSystem crs, double ... position) {
        return new DefaultPoint(crs, position);
    }

    public static Point createPoint(SampleSystem ss, double ... position) {
        return new DefaultPoint(ss, position);
    }

    public static Point createPoint(PointSequence sequence) {
        return new DefaultPoint(sequence);
    }

    public static LineString createLineString(PointSequence sequence) {
        return new DefaultLineString(sequence);
    }

    public static LinearRing createLinearRing(PointSequence sequence) {
        return new DefaultLinearRing(sequence);
    }

    public static Polygon createPolygon(LinearRing exterior, List<LinearRing> interiors) {
        return new DefaultPolygon(exterior, interiors);
    }

    public static Triangle createTriangle(LinearRing exterior) {
        return new DefaultTriangle(exterior);
    }

    public static MultiPoint createMultiPoint(PointSequence sequence) {
        return new DefaultMultiPoint(sequence);
    }

    public static MultiLineString createMultiLineString(LineString ... geometries) {
        return new DefaultMultiLineString(geometries);
    }

    public static MultiPolygon createMultiPolygon(Polygon ... geometries) {
        return new DefaultMultiPolygon(geometries);
    }

    public static <T extends Surface> MultiSurface<T> createMultiSurface(T ... geometries) {
        return new DefaultMultiSurface<>(geometries);
    }

    public static <T extends Geometry> GeometryCollection<T> createGeometryCollection(T ... geometries) {
        return new DefaultGeometryCollection<>(geometries);
    }

    public static PointSequence createSequence(TupleArray positions) {
        return createSequence(Collections.singletonMap(AttributesType.ATT_POSITION, positions));
    }

    public static PointSequence createSequence(Map<String, TupleArray> attributes) {
        return new ArraySequence(attributes);
    }

    // ////////////////////////////////////////////////////////////////////////
    // org.apache.sis.geometry.wrapper.Geometries methods /////////////////////
    // ////////////////////////////////////////////////////////////////////////


    @Override
    public Class<?> getGeometryClass(GeometryType type) {
        switch (type) {
            case CIRCLE : return Circle.class;
            case CIRCULARSTRING : return CircularString.class;
            case CLOTHOID : return Clothoid.class;
            case COMPOUNDCURVE : return CompoundCurve.class;
            case CURVE : return Curve.class;
            case CURVEPOLYGON : return CurvePolygon.class;
            case GEOMETRY : return Geometry.class;
            case GEOMETRYCOLLECTION : return GeometryCollection.class;
            case LINESTRING : return LineString.class;
            case MULTICURVE : return MultiCurve.class;
            case MULTILINESTRING : return MultiLineString.class;
            case MULTIPOINT : return MultiPoint.class;
            case MULTIPOLYGON : return MultiPolygon.class;
            case MULTISURFACE : return MultiSurface.class;
            case POINT : return Point.class;
            case POLYGON : return Polygon.class;
            case POLYHEDRALSURFACE : return PolyhedralSurface.class;
            case SURFACE : return Surface.class;
            case TIN : return TIN.class;
            case TRIANGLE : return Triangle.class;
            //todo
            case BREPSOLID :
            case COMPOUNDSURFACE :
            case ELLIPTICALCURVE :
            case GEODESICSTRING :
            case NURBSCURVE :
            case SPIRALCURVE :
            default: return Geometry.class;
        }
    }

    @Override
    public GeometryType getGeometryType(Class<?> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GeometryWrapper castOrWrap(Object geometry) {
        if (geometry instanceof Wrapper) return (GeometryWrapper) geometry;
        return new Wrapper((Geometry) geometry);
    }

    @Override
    public GeometryWrapper parseWKT(String wkt) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GeometryWrapper parseWKB(ByteBuffer data) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean supports(Capability feature) {
        switch (feature) {
            case Z_COORDINATE : return true;
            case M_COORDINATE : return true;
            case SINGLE_PRECISION : return true;
            default: return false;
        }
    }

    @Override
    public Point createPoint(double x, double y) {
        return new DefaultPoint(SampleSystem.ofSize(2), x, y);
    }

    @Override
    public Point createPoint(double x, double y, double z) {
        return new DefaultPoint(SampleSystem.ofSize(3), x ,y, z);
    }

    @Override
    public Point createPoint(boolean isFloat, Dimensions dimensions, DoubleBuffer coordinates) {
        final ArraySequence points;

        if (!dimensions.hasZ) {
            final SampleSystem ss = SampleSystem.ofSize(2);
            if (isFloat) {
                points = new ArraySequence(TupleArrays.of(ss, (float) coordinates.get(0), (float) coordinates.get(1)));
            } else {
                points = new ArraySequence(TupleArrays.of(ss, coordinates.get(0), coordinates.get(1)));
            }
        } else {
            final SampleSystem ss = SampleSystem.ofSize(3);
            if (isFloat) {
                points = new ArraySequence(TupleArrays.of(ss, (float) coordinates.get(0), (float) coordinates.get(1), (float) coordinates.get(2)));
            } else {
                points = new ArraySequence(TupleArrays.of(ss, coordinates.get(0), coordinates.get(1), coordinates.get(2)));
            }
        }

        if (dimensions.hasM) {
            final TupleArray marray;
            if (isFloat) {
                marray = TupleArrays.of(SampleSystem.ofSize(1), (float) coordinates.get(dimensions.hasZ ? 3 : 2));
            } else {
                marray = TupleArrays.of(SampleSystem.ofSize(1), coordinates.get(dimensions.hasZ ? 3 : 2));
            }
            points.setAttribute("m", marray);
        }

        return new DefaultPoint(points);
    }

    @Override
    public MultiPoint<?> createMultiPoint(boolean isFloat, Dimensions dimensions, DoubleBuffer coordinates) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Geometry createPolyline(boolean polygon, boolean isFloat, Dimensions dimensions, DoubleBuffer... coordinates) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GeometryWrapper createMultiPolygon(Object[] geometries) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GeometryWrapper createFromComponents(GeometryType type, Object components) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected GeometryWrapper createWrapper(Geometry geometry) {
        return new Wrapper(geometry);
    }

}
