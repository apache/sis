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
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.Array;
import org.apache.sis.geometries.internal.shared.ArraySequence;
import org.apache.sis.geometries.internal.shared.DefaultCircularString;
import org.apache.sis.geometries.internal.shared.DefaultCompoundCurve;
import org.apache.sis.geometries.internal.shared.DefaultCurvePolygon;
import org.apache.sis.geometries.internal.shared.DefaultEmpty;
import org.apache.sis.geometries.internal.shared.DefaultGeometryCollection;
import org.apache.sis.geometries.internal.shared.DefaultLineString;
import org.apache.sis.geometries.internal.shared.DefaultLinearRing;
import org.apache.sis.geometries.internal.shared.DefaultMultiCurve;
import org.apache.sis.geometries.internal.shared.DefaultMultiLineString;
import org.apache.sis.geometries.internal.shared.DefaultMultiPoint;
import org.apache.sis.geometries.internal.shared.DefaultMultiPolygon;
import org.apache.sis.geometries.internal.shared.DefaultMultiPolyhedron;
import org.apache.sis.geometries.internal.shared.DefaultMultiSurface;
import org.apache.sis.geometries.internal.shared.DefaultPoint;
import org.apache.sis.geometries.internal.shared.DefaultPolygon;
import org.apache.sis.geometries.internal.shared.DefaultPolyhedralSurface;
import org.apache.sis.geometries.internal.shared.DefaultPolyhedron;
import org.apache.sis.geometries.internal.shared.DefaultRawMultiPoint;
import org.apache.sis.geometries.internal.shared.DefaultReversedCurve;
import org.apache.sis.geometries.internal.shared.DefaultReversedSurface;
import org.apache.sis.geometries.internal.shared.DefaultTriangle;
import org.apache.sis.geometries.internal.shared.DefaultTriangulatedSurface;
import org.apache.sis.geometries.math.DataType;
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

    public static Empty createEmpty(CoordinateReferenceSystem crs) {
        final AttributesType.Template attType = new AttributesType.Template();
        attType.addOrReplaceAttribute(AttributesType.ATT_POSITION, SampleSystem.of(crs), DataType.DOUBLE);
        return new DefaultEmpty(attType);
    }

    public static Empty createEmpty(AttributesType attType) {
        return new DefaultEmpty(attType);
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

    public static MultiPoint createMultiPoint(Point ... geometries) {
        return new DefaultRawMultiPoint(geometries);
    }

    public static MultiLineString createMultiLineString(LineString ... geometries) {
        return new DefaultMultiLineString(geometries);
    }

    public static <T extends Curve> MultiCurve<T> createMultiCurve(T ... geometries) {
        return new DefaultMultiCurve<>(geometries);
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

    /*
     * Variants taking an explicit coordinate reference system, used when the collection may be
     * empty. An aggregate normally reports the CRS of its first element; with no element there is
     * nothing to report, so the CRS has to be supplied by the caller.
     */

    public static MultiPoint createMultiPoint(CoordinateReferenceSystem crs, Point ... geometries) {
        return new DefaultRawMultiPoint(crs, geometries);
    }

    public static MultiLineString createMultiLineString(CoordinateReferenceSystem crs, LineString ... geometries) {
        return new DefaultMultiLineString(crs, geometries);
    }

    public static <T extends Curve> MultiCurve<T> createMultiCurve(CoordinateReferenceSystem crs, T ... geometries) {
        return new DefaultMultiCurve<>(crs, geometries);
    }

    public static MultiPolygon createMultiPolygon(CoordinateReferenceSystem crs, Polygon ... geometries) {
        return new DefaultMultiPolygon(crs, geometries);
    }

    public static <T extends Surface> MultiSurface<T> createMultiSurface(CoordinateReferenceSystem crs, T ... geometries) {
        return new DefaultMultiSurface<>(crs, geometries);
    }

    public static <T extends Geometry> GeometryCollection<T> createGeometryCollection(CoordinateReferenceSystem crs, T ... geometries) {
        return new DefaultGeometryCollection<>(crs, geometries);
    }

    /*
     * Curves and surfaces beyond the linear ones, and solids. These are what the GML 3 constructs
     * `gml:Curve`, `gml:CompositeCurve`, `gml:Ring`, `gml:ArcString`, `gml:Surface`,
     * `gml:CompositeSurface`, `gml:Solid`, `gml:CompositeSolid` and the two `gml:Orientable*`
     * elements map onto.
     */

    public static CompoundCurve createCompoundCurve(Curve ... curves) {
        return new DefaultCompoundCurve(curves);
    }

    public static CompoundCurve createCompoundCurve(CoordinateReferenceSystem crs, Curve ... curves) {
        return new DefaultCompoundCurve(crs, curves);
    }

    public static CircularString createCircularString(PointSequence sequence) {
        return new DefaultCircularString(sequence);
    }

    public static CurvePolygon createCurvePolygon(Curve exterior, List<Curve> interiors) {
        return new DefaultCurvePolygon(exterior, interiors);
    }

    public static <T extends Polygon> PolyhedralSurface<T> createPolyhedralSurface(T ... patches) {
        return new DefaultPolyhedralSurface<>(null, patches);
    }

    public static <T extends Polygon> PolyhedralSurface<T> createPolyhedralSurface(CoordinateReferenceSystem crs, T[] patches) {
        return new DefaultPolyhedralSurface<>(crs, patches);
    }

    public static TIN createTIN(Triangle ... patches) {
        return new DefaultTriangulatedSurface(patches);
    }

    public static TIN createTIN(CoordinateReferenceSystem crs, Triangle[] patches) {
        return new DefaultTriangulatedSurface(crs, patches);
    }

    public static Polyhedron createPolyhedron(MultiPolygon exteriorShell, List<MultiPolygon> interiorShells) {
        return new DefaultPolyhedron(exteriorShell, interiorShells);
    }

    public static MultiPolyhedron createMultiPolyhedron(Polyhedron ... solids) {
        return new DefaultMultiPolyhedron(solids);
    }

    public static MultiPolyhedron createMultiPolyhedron(CoordinateReferenceSystem crs, Polyhedron ... solids) {
        return new DefaultMultiPolyhedron(crs, solids);
    }

    /**
     * Returns a curve traversed in the opposite direction to the given one.
     * This is what a GML {@code gml:OrientableCurve} with {@code orientation="-"} describes.
     */
    public static Curve createReversed(Curve base) {
        return new DefaultReversedCurve(base);
    }

    /**
     * Returns a surface whose up-normal points the opposite way to the given one's.
     * This is what a GML {@code gml:OrientableSurface} with {@code orientation="-"} describes.
     */
    public static Surface createReversed(Surface base) {
        return new DefaultReversedSurface(base);
    }

    public static PointSequence createSequence(Array positions) {
        return createSequence(Collections.singletonMap(AttributesType.ATT_POSITION, positions));
    }

    public static PointSequence createSequence(Map<String, Array> attributes) {
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
                points = new ArraySequence(NDArrays.of(ss, (float) coordinates.get(0), (float) coordinates.get(1)));
            } else {
                points = new ArraySequence(NDArrays.of(ss, coordinates.get(0), coordinates.get(1)));
            }
        } else {
            final SampleSystem ss = SampleSystem.ofSize(3);
            if (isFloat) {
                points = new ArraySequence(NDArrays.of(ss, (float) coordinates.get(0), (float) coordinates.get(1), (float) coordinates.get(2)));
            } else {
                points = new ArraySequence(NDArrays.of(ss, coordinates.get(0), coordinates.get(1), coordinates.get(2)));
            }
        }

        if (dimensions.hasM) {
            final Array marray;
            if (isFloat) {
                marray = NDArrays.of(SampleSystem.ofSize(1), (float) coordinates.get(dimensions.hasZ ? 3 : 2));
            } else {
                marray = NDArrays.of(SampleSystem.ofSize(1), coordinates.get(dimensions.hasZ ? 3 : 2));
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
