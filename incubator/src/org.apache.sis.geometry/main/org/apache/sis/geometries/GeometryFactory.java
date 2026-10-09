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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.measure.Quantity;
import javax.measure.Unit;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.apache.sis.geometries.cs.Bearing;
import org.apache.sis.geometries.cs.Projection;
import org.apache.sis.geometries.curve.Arc;
import org.apache.sis.geometries.curve.ArcByBulge;
import org.apache.sis.geometries.curve.ArcByCenterPoint;
import org.apache.sis.geometries.curve.BSplineCurve;
import org.apache.sis.geometries.curve.Bezier;
import org.apache.sis.geometries.curve.Circle;
import org.apache.sis.geometries.curve.CircularString;
import org.apache.sis.geometries.curve.Clothoid;
import org.apache.sis.geometries.curve.CompoundCurve;
import org.apache.sis.geometries.curve.Conic;
import org.apache.sis.geometries.curve.CubicSpline;
import org.apache.sis.geometries.curve.EllipticArc;
import org.apache.sis.geometries.curve.Geodesic;
import org.apache.sis.geometries.curve.KnotType;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.curve.MultiCurve;
import org.apache.sis.geometries.curve.MultiLineString;
import org.apache.sis.geometries.curve.NurbCurve;
import org.apache.sis.geometries.curve.OffsetCurve;
import org.apache.sis.geometries.curve.PolynomialSpline;
import org.apache.sis.geometries.curve.ProductCurve;
import org.apache.sis.geometries.curve.RealFunction;
import org.apache.sis.geometries.curve.Rhumb;
import org.apache.sis.geometries.curve.Spiral;
import org.apache.sis.geometries.curve.SplineCurveForm;
import org.apache.sis.geometries.internal.shared.ArrayDataPoints;
import org.apache.sis.geometries.internal.shared.DefaultArc;
import org.apache.sis.geometries.internal.shared.DefaultArcByBulge;
import org.apache.sis.geometries.internal.shared.DefaultArcByCenterPoint;
import org.apache.sis.geometries.internal.shared.DefaultBSplineSolid;
import org.apache.sis.geometries.internal.shared.DefaultBSplineCurve;
import org.apache.sis.geometries.internal.shared.DefaultBSplineSurface;
import org.apache.sis.geometries.internal.shared.DefaultBezier;
import org.apache.sis.geometries.internal.shared.DefaultBilinearGrid;
import org.apache.sis.geometries.internal.shared.DefaultCircle;
import org.apache.sis.geometries.internal.shared.DefaultCircularString;
import org.apache.sis.geometries.internal.shared.DefaultClothoid;
import org.apache.sis.geometries.internal.shared.DefaultCompoundCurve;
import org.apache.sis.geometries.internal.shared.DefaultConic;
import org.apache.sis.geometries.internal.shared.DefaultCubicSpline;
import org.apache.sis.geometries.internal.shared.DefaultCurvePolygon;
import org.apache.sis.geometries.internal.shared.DefaultEllipticArc;
import org.apache.sis.geometries.internal.shared.DefaultEmpty;
import org.apache.sis.geometries.internal.shared.DefaultGeodesic;
import org.apache.sis.geometries.internal.shared.DefaultGeometryCollection;
import org.apache.sis.geometries.internal.shared.DefaultLineString;
import org.apache.sis.geometries.internal.shared.DefaultLinearRing;
import org.apache.sis.geometries.internal.shared.DefaultMultiCurve;
import org.apache.sis.geometries.internal.shared.DefaultMultiLineString;
import org.apache.sis.geometries.internal.shared.DefaultMultiPoint;
import org.apache.sis.geometries.internal.shared.DefaultMultiPolygon;
import org.apache.sis.geometries.internal.shared.DefaultMultiPolyhedron;
import org.apache.sis.geometries.internal.shared.DefaultMultiSurface;
import org.apache.sis.geometries.internal.shared.DefaultNurbCurve;
import org.apache.sis.geometries.internal.shared.DefaultNurbSurface;
import org.apache.sis.geometries.internal.shared.DefaultOffsetCurve;
import org.apache.sis.geometries.internal.shared.DefaultPoint;
import org.apache.sis.geometries.internal.shared.DefaultPolygon;
import org.apache.sis.geometries.internal.shared.DefaultPolyhedralSurface;
import org.apache.sis.geometries.internal.shared.DefaultPolyhedron;
import org.apache.sis.geometries.internal.shared.DefaultPolynomialSpline;
import org.apache.sis.geometries.internal.shared.DefaultPrism;
import org.apache.sis.geometries.internal.shared.DefaultProductCurve;
import org.apache.sis.geometries.internal.shared.DefaultRawMultiPoint;
import org.apache.sis.geometries.internal.shared.DefaultReversedCurve;
import org.apache.sis.geometries.internal.shared.DefaultReversedSurface;
import org.apache.sis.geometries.internal.shared.DefaultRhumb;
import org.apache.sis.geometries.internal.shared.DefaultSpiral;
import org.apache.sis.geometries.internal.shared.DefaultTriangle;
import org.apache.sis.geometries.internal.shared.DefaultTriangulatedSurface;
import org.apache.sis.geometries.point.MultiPoint;
import org.apache.sis.geometries.solid.Ellipsoid;
import org.apache.sis.geometries.solid.MultiPolyhedron;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.solid.Sphere;
import org.apache.sis.geometries.solid.BSplineSolid;
import org.apache.sis.geometries.surface.BSplineSurface;
import org.apache.sis.geometries.surface.BSplineSurfaceForm;
import org.apache.sis.geometries.surface.BilinearGrid;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.MultiPolygon;
import org.apache.sis.geometries.surface.MultiSurface;
import org.apache.sis.geometries.surface.NurbSurface;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.geometries.surface.TIN;
import org.apache.sis.geometries.surface.Triangle;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.SampleSystem;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Range;
import org.apache.sis.maths.Vector;


/**
 * Factory for SIS Geometry.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GeometryFactory {

    public static GeometryFactory DEFAULT = new GeometryFactory();

    private GeometryFactory(){
    }

    // ////////////////////////////////////////////////////////////////////////
    // DataPoints /////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Create a new DataPoints from a 2D array of coordinates.
     *
     * @param positions in 2D
     * @return created DataPoints
     */
    public DataPoints createDataPoints(double ... positions) {
        return createDataPoints(2, positions);
    }

    public DataPoints createDataPoints(int dimension, double ... positions) {
        return createDataPoints(Geometries.getUndefinedCRS(dimension), positions);
    }

    public DataPoints createDataPoints(CoordinateReferenceSystem crs, double ... positions) {
        final Array array = NDArrays.of(crs, positions);
        return createDataPoints(array);
    }

    /**
     * Create a new DataPoints from a 2D array of coordinates.
     *
     * @param positions in 2D
     * @return created DataPoints
     */
    public DataPoints createDataPoints(float ... positions) {
        return createDataPoints(2, positions);
    }

    public DataPoints createDataPoints(int dimension, float ... positions) {
        return createDataPoints(Geometries.getUndefinedCRS(dimension), positions);
    }

    public DataPoints createDataPoints(CoordinateReferenceSystem crs, float ... positions) {
        final Array array = NDArrays.of(crs, positions);
        return createDataPoints(array);
    }

    public DataPoints createDataPoints(Array positions) {
        return GeometryFactory.this.createDataPoints(Collections.singletonMap(DataPointsType.ATT_POSITION, positions));
    }

    public DataPoints createDataPoints(Map<String, Array> attributes) {
        return new ArrayDataPoints(attributes);
    }

    /**
     * Returns a copy of the given DataPoints.
     */
    public DataPoints createDataPoints(final DataPoints source) {
        final Map<String,Array> attributes = new LinkedHashMap<>();
        for (final String name : source.getType().getAttributeNames()) {
            attributes.put(name, source.getAttributeArray(name));
        }
        return createDataPoints(attributes);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Empty //////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    public Empty createEmpty(CoordinateReferenceSystem crs) {
        final DataPointsType.Template attType = new DataPointsType.Template();
        attType.addOrReplaceAttribute(DataPointsType.ATT_POSITION, SampleSystem.of(crs), DataType.DOUBLE);
        return createEmpty(attType);
    }

    public Empty createEmpty(DataPointsType attType) {
        return new DefaultEmpty(attType);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Points /////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    public Point createPoint(CoordinateReferenceSystem crs) {
        return new DefaultPoint(crs);
    }

    public Point createPoint(CoordinateReferenceSystem crs, double ... position) {
        return new DefaultPoint(crs, position);
    }

    public Point createPoint(SampleSystem ss, double ... position) {
        return new DefaultPoint(ss, position);
    }

    public Point createPoint(DataPoints sequence) {
        return new DefaultPoint(sequence);
    }

    public MultiPoint createMultiPoint(DataPoints sequence) {
        return new DefaultMultiPoint(sequence);
    }

    public MultiPoint createMultiPoint(Point ... geometries) {
        return new DefaultRawMultiPoint(geometries);
    }

    public MultiPoint createMultiPoint(CoordinateReferenceSystem crs, Point ... geometries) {
        return new DefaultRawMultiPoint(crs, geometries);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Curves /////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    public LineString createLineString(DataPoints sequence) {
        return new DefaultLineString(sequence);
    }

    public LinearRing createLinearRing(DataPoints sequence) {
        return new DefaultLinearRing(sequence);
    }

    public Geodesic createGeodesic(DataPoints sequence) {
        return new DefaultGeodesic(sequence);
    }

    public Rhumb createRhumb(DataPoints sequence) {
        return new DefaultRhumb(sequence);
    }

    public CircularString createCircularString(DataPoints sequence) {
        return new DefaultCircularString(sequence);
    }

    public CompoundCurve createCompoundCurve(Curve ... curves) {
        return new DefaultCompoundCurve(curves);
    }

    public CompoundCurve createCompoundCurve(CoordinateReferenceSystem crs, Curve ... curves) {
        return new DefaultCompoundCurve(crs, curves);
    }

    /**
     * Creates a circular arc from the centre of its circle, that circle's radius expressed in the
     * given unit, and the bearings at which the arc starts and ends.
     *
     * @param  center      centre of the circle the arc is a part of.
     * @param  radius      radius of that circle, expressed in {@code radiusUnit}. Must be greater than zero.
     * @param  radiusUnit  unit of {@code radius}, or {@code null} for the units of the coordinate system axes.
     * @param  startAngle  bearing at which the arc starts, in decimal degrees.
     * @param  endAngle    bearing at which the arc ends, in decimal degrees.
     */
    public ArcByCenterPoint createArcByCenterPoint(Point center, double radius, Unit<?> radiusUnit,
            double startAngle, double endAngle)
    {
        return new DefaultArcByCenterPoint(center, radius, radiusUnit, startAngle, endAngle);
    }

    /**
     * Creates a circular arc from its two end points, the distance by which it bulges away from the
     * chord joining them, and the direction of that bulge.
     *
     * @param  points  the start point followed by the end point. Its size must be exactly 2.
     * @param  bulge   distance from the midpoint of the chord to the arc, along {@code normal}.
     * @param  normal  direction the arc bulges towards, perpendicular to the chord.
     */
    public ArcByBulge createArcByBulge(DataPoints points, double bulge, Vector<?> normal) {
        return new DefaultArcByBulge(points, bulge, normal);
    }

    /**
     * Creates a chain of conic section arcs, each of them determined by five data points.
     *
     * @param  points         points lying on the conic. At least five of them, the first one also
     *                        acting as the last one when {@code cycle} is {@code true}.
     * @param  controlPoints  centres of the exponential maps in which the arcs are constructed,
     *                        one per arc, or {@code null} if none.
     * @param  cycle          whether the conic closes on itself.
     */
    public Conic createConic(DataPoints points, Array controlPoints, boolean cycle) {
        return new DefaultConic(points, controlPoints, cycle);
    }

    /**
     * Creates a chain of circular arcs, each of them centred on a control point and joining two
     * consecutive data points.
     *
     * @param  points         start and end points of the arcs, two consecutive arcs sharing a point.
     *                        There is one more point than there are arcs.
     * @param  controlPoints  centres of the circles carrying the arcs, one per arc.
     * @param  radius         radius vectors giving the plane and the rotation direction of each arc,
     *                        possibly empty.
     * @param  cycle          whether the chain closes on itself.
     */
    public Arc createArc(DataPoints points, Array controlPoints, List<Vector> radius, boolean cycle) {
        return new DefaultArc(points, controlPoints, radius, cycle);
    }

    /**
     * Creates a complete circle, i.e. a chain of circular arcs sharing a single centre and closing
     * on itself.
     *
     * @param  points         points of the circle, all at the same distance from the centre,
     *                        the first and the last ones being equal.
     * @param  controlPoints  the centre of the circle, repeated once per arc. Because a single arc
     *                        must stay below a full turn, at least two of them are needed.
     * @param  radius         radius vectors of the arcs, possibly empty.
     */
    public Circle createCircle(DataPoints points, Array controlPoints, List<Vector> radius) {
        return new DefaultCircle(points, controlPoints, radius);
    }

    /**
     * Creates a conic without a cross term, therefore an arc of ellipse, each arc being determined
     * by four data points instead of five.
     *
     * @param  points         points lying on the ellipse.
     * @param  controlPoints  centres of the ellipses of the arcs, or {@code null} if none.
     * @param  cycle          whether the arc closes on itself, making it a complete ellipse.
     */
    public EllipticArc createEllipticArc(DataPoints points, Array controlPoints, boolean cycle) {
        return new DefaultEllipticArc(points, controlPoints, cycle);
    }

    /**
     * Creates a curve defined indirectly by its curvature, and by its torsion when it is not planar.
     *
     * @param  points      points of the spiral, the first one being its start point.
     * @param  curvature   curvature as a function of arc length.
     * @param  torsion     torsion as a function of arc length, or {@code null} if the spiral is planar.
     * @param  startFrame  two or three mutually orthogonal unit vectors forming a right-handed frame
     *                     at the start point.
     */
    public Spiral createSpiral(DataPoints points, RealFunction curvature,
            RealFunction torsion, List<Vector> startFrame)
    {
        return new DefaultSpiral(points, curvature, torsion, startFrame);
    }

    /**
     * Creates a spiral whose curvature varies linearly with arc length, also called a Cornu spiral.
     *
     * @param  points      points of the clothoid, the first one being its start point.
     * @param  curvature   curvature as a function of arc length. It shall be linear in the arc
     *                     length measured from the point where the infinite clothoid has zero
     *                     curvature.
     * @param  startFrame  two mutually orthogonal unit vectors forming a right-handed frame at the
     *                     start point, a clothoid being planar.
     */
    public Clothoid createClothoid(DataPoints points, RealFunction curvature, List<Vector> startFrame) {
        return new DefaultClothoid(points, curvature, startFrame);
    }

    /**
     * Creates a spline which interpolates its data points, i.e. a polynomial curve passing through them.
     *
     * @param  points              points the spline passes through, in order.
     * @param  controlPoints       control points of the spline, or {@code null} if none.
     * @param  knots               knot values, strictly increasing, one per data point.
     * @param  degree              degree of the interpolating polynomials.
     * @param  curveForm           kind of curve approximated by the spline, or {@code null} if none.
     * @param  knotSpec            distribution of the knots, or {@code null} if unspecified.
     * @param  derivativeAtStart   derivative imposed at the start point, or {@code null} if none.
     * @param  derivativeAtEnd     derivative imposed at the end point, or {@code null} if none.
     * @param  derivativeInterior  number of continuous derivatives at the interior knots,
     *                             at most {@code degree} − 1.
     */
    public PolynomialSpline createPolynomialSpline(DataPoints points, Array controlPoints,
            double[] knots, int degree, SplineCurveForm curveForm, KnotType knotSpec,
            Vector derivativeAtStart, Vector derivativeAtEnd, int derivativeInterior)
    {
        return new DefaultPolynomialSpline(points, controlPoints, knots, degree, curveForm,
                knotSpec, derivativeAtStart, derivativeAtEnd, derivativeInterior);
    }

    /**
     * Creates a polynomial spline of degree 3, C² everywhere and passing through its data points.
     *
     * @param  points             points the spline passes through, in order.
     * @param  controlPoints      control points of the spline, or {@code null} if none.
     * @param  knots              knot values, strictly increasing, one per data point.
     * @param  curveForm          kind of curve approximated by the spline, or {@code null} if none.
     * @param  knotSpec           distribution of the knots, or {@code null} if unspecified.
     * @param  derivativeAtStart  tangent imposed at the start point, or {@code null} if none.
     * @param  derivativeAtEnd    tangent imposed at the end point, or {@code null} if none.
     */
    public CubicSpline createCubicSpline(DataPoints points, Array controlPoints, double[] knots,
            SplineCurveForm curveForm, KnotType knotSpec, Vector derivativeAtStart, Vector derivativeAtEnd)
    {
        return new DefaultCubicSpline(points, controlPoints, knots, curveForm, knotSpec,
                derivativeAtStart, derivativeAtEnd);
    }

    /**
     * Creates an approximating spline using the Bézier (Bernstein) polynomials as partition of unity.
     *
     * @param  points             end points of the segments of the curve.
     * @param  controlPoints      control points of the curve, each subsequence of {@code degree} + 1
     *                            points starting at a multiple of {@code degree} defining one segment.
     * @param  knots              knot values. For a Bézier curve the only knots are 0 and 1.
     * @param  degree             degree of the Bernstein polynomials.
     * @param  curveForm          kind of curve approximated by the spline, or {@code null} if none.
     * @param  knotSpec           distribution of the knots, or {@code null} if unspecified.
     * @param  derivativeAtStart  derivative imposed at the start point, or {@code null} if none.
     * @param  derivativeAtEnd    derivative imposed at the end point, or {@code null} if none.
     */
    public Bezier createBezier(DataPoints points, Array controlPoints, double[] knots, int degree,
            SplineCurveForm curveForm, KnotType knotSpec, Vector derivativeAtStart, Vector derivativeAtEnd)
    {
        return new DefaultBezier(points, controlPoints, knots, degree, curveForm, knotSpec,
                derivativeAtStart, derivativeAtEnd);
    }

    /**
     * Creates an approximating spline using the b-spline basis functions as partition of unity.
     * For the rational flavour carrying a weight per control point, see
     * {@link #createNurbCurve(DataPoints, double[], double[], int)}.
     *
     * @param  points         points of the curve.
     * @param  controlPoints  control points of the curve, or {@code null} if none.
     * @param  knots          knot values, strictly increasing, repeated knots being expressed by
     *                        their multiplicity rather than by repetition.
     * @param  degree         degree of the b-spline basis functions.
     * @param  curveForm      kind of curve approximated by the spline, or {@code null} if none.
     * @param  knotSpec       distribution of the knots, or {@code null} if unspecified.
     * @param  rational       whether the control points are expressed in homogeneous coordinates.
     */
    public BSplineCurve createBSplineCurve(DataPoints points, Array controlPoints, double[] knots,
            int degree, SplineCurveForm curveForm, KnotType knotSpec, boolean rational)
    {
        return new DefaultBSplineCurve(points, controlPoints, knots, degree, curveForm, knotSpec, rational);
    }

    /**
     * Creates a curve defined by control points, weights, a knot vector and a degree. Depending on
     * the given values it is a Bézier curve, a b-spline or a NURBS.
     *
     * @param  points   control points of the curve.
     * @param  weights  weight of each control point, making the curve rational.
     * @param  knots    knot values, whose multiplicities make the curve clamped or periodic.
     * @param  degree   degree of the basis functions.
     */
    public NurbCurve createNurbCurve(DataPoints points, double[] weights, double[] knots, int degree) {
        return new DefaultNurbCurve(points, weights, knots, degree);
    }

    /**
     * Creates a curve at a constant distance and bearing from a base curve.
     *
     * @param  baseCurve     curve from which the returned curve is offset. It shall have a
     *                       well-defined tangent at every position.
     * @param  distance      constant offset distance. In a 2-dimensional coordinate system, a
     *                       positive distance designates the left side of the base curve with
     *                       respect to the tangent, and a negative distance its right side.
     * @param  refDirection  reference direction of the offset in a 3-dimensional coordinate system,
     *                       or {@code null} if the spatial dimension is 2.
     */
    public OffsetCurve createOffsetCurve(Curve baseCurve, Quantity<?> distance, Bearing refDirection) {
        return new DefaultOffsetCurve(baseCurve, distance, refDirection);
    }

    /**
     * Creates a curve composed of other curves which all share the same parameter space, each of
     * them covering a disjoint projection of the coordinate system.
     *
     * @param  parameterRange  interval of the construction parameter shared by all the elements.
     * @param  projections     projections of the coordinate system matching the element curves.
     *                         They are disjoint and together cover the whole coordinate system.
     * @param  elements        projections of the curve, one per projection of the coordinate system.
     */
    public ProductCurve createProductCurve(Range<?> parameterRange,
            List<Projection> projections, Curve ... elements)
    {
        return new DefaultProductCurve(parameterRange, projections, elements);
    }

    /**
     * Returns a curve traversed in the opposite direction to the given one.
     * This is what a GML {@code gml:OrientableCurve} with {@code orientation="-"} describes.
     */
    public Curve createReversed(Curve base) {
        return new DefaultReversedCurve(base);
    }

    public MultiLineString createMultiLineString(LineString ... geometries) {
        return new DefaultMultiLineString(geometries);
    }

    public MultiLineString createMultiLineString(CoordinateReferenceSystem crs, LineString ... geometries) {
        return new DefaultMultiLineString(crs, geometries);
    }

    public <T extends Curve> MultiCurve<T> createMultiCurve(T ... geometries) {
        return new DefaultMultiCurve<>(geometries);
    }

    public <T extends Curve> MultiCurve<T> createMultiCurve(CoordinateReferenceSystem crs, T ... geometries) {
        return new DefaultMultiCurve<>(crs, geometries);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Surfaces ///////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    public Polygon createPolygon(LinearRing exterior, List<LinearRing> interiors) {
        return new DefaultPolygon(exterior, interiors);
    }

    public Triangle createTriangle(LinearRing exterior) {
        return new DefaultTriangle(exterior);
    }

    public CurvePolygon createCurvePolygon(Curve exterior, List<Curve> interiors) {
        return new DefaultCurvePolygon(exterior, interiors);
    }

    public <T extends Polygon> PolyhedralSurface<T> createPolyhedralSurface(T ... patches) {
        return new DefaultPolyhedralSurface<>(null, patches);
    }

    public <T extends Polygon> PolyhedralSurface<T> createPolyhedralSurface(CoordinateReferenceSystem crs, T[] patches) {
        return new DefaultPolyhedralSurface<>(crs, patches);
    }

    public TIN createTIN(Triangle ... patches) {
        return new DefaultTriangulatedSurface(patches);
    }

    public TIN createTIN(CoordinateReferenceSystem crs, Triangle[] patches) {
        return new DefaultTriangulatedSurface(crs, patches);
    }

    /**
     * Creates a parametric curve surface using polylines as both horizontal and vertical curves,
     * each cell of the parameter grid being interpolated bilinearly over the unit square.
     *
     * @param  points         positions at the knots of the parameter grid, in row-major order.
     *                        There shall be {@code rows} × {@code columns} of them.
     * @param  controlPoints  control points of the section curves in row-major order,
     *                        or {@code null} if none.
     * @param  rows           number of rows in the parameter grid.
     * @param  columns        number of columns in the parameter grid.
     * @param  knots          knot values, one sequence per surface parameter, or {@code null} if none.
     */
    public BilinearGrid createBilinearGrid(DataPoints points, List<DirectPosition> controlPoints,
            int rows, int columns, List<double[]> knots)
    {
        return new DefaultBilinearGrid(points, controlPoints, rows, columns, knots);
    }

    /**
     * Creates a rational or polynomial parametric surface represented by control points, b-spline
     * basis functions and possibly weights. For the rational flavour carrying a weight per control
     * point, see {@link #createNurbSurface(Vector[][], double[][], double[], double[], int)}.
     *
     * @param  points         positions at the knots of the parameter grid, in row-major order.
     *                        There shall be {@code rows} × {@code columns} of them.
     * @param  controlPoints  control points in row-major order, or {@code null} if none.
     * @param  rows           number of rows in the parameter grid.
     * @param  columns        number of columns in the parameter grid.
     * @param  knots          exactly two knot sequences, one per surface parameter, knots with a
     *                        multiplicity greater than one being repeated in the sequence.
     * @param  degree         algebraic degree of the b-spline basis functions.
     * @param  knotSpec       distribution of the knots, or {@code null} if unspecified.
     * @param  surfaceForm    kind of surface approximated by the spline, or {@code null} if none.
     * @param  polynomial     {@code true} if the surface is polynomial, {@code false} if the control
     *                        points are expressed in homogeneous coordinates, making it rational.
     */
    public BSplineSurface createBSplineSurface(DataPoints points, List<DirectPosition> controlPoints,
            int rows, int columns, List<double[]> knots, int degree, KnotType knotSpec,
            BSplineSurfaceForm surfaceForm, boolean polynomial)
    {
        return new DefaultBSplineSurface(points, controlPoints, rows, columns, knots, degree,
                knotSpec, surfaceForm, polynomial);
    }

    /**
     * Creates a NURBS surface, i.e. the tensor product of two directions each having its own knot
     * vector, over a grid of weighted control points.
     *
     * @param  controlPoints  control points as a grid, the first index running along <var>u</var>
     *                        and the second one along <var>v</var>.
     * @param  weights        weight of each control point, in the same layout as the control points.
     * @param  knotsU         knot values along the <var>u</var> parameter.
     * @param  knotsV         knot values along the <var>v</var> parameter.
     * @param  degree         degree of the basis functions.
     */
    public NurbSurface createNurbSurface(Vector<?>[][] controlPoints, double[][] weights,
            double[] knotsU, double[] knotsV, int degree)
    {
        return new DefaultNurbSurface(controlPoints, weights, knotsU, knotsV, degree);
    }

    /**
     * Creates a sphere of the given radius centered on the single position of the given sequence.
     * The sequence is taken as-is, so the caller may give the sphere the attributes carried by it.
     *
     * @param  points  the center of the sphere, as a sequence of exactly one position.
     * @param  radius  radius of the sphere, must be positive.
     * @return the sphere of the given center and radius.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position.
     */
    public Sphere createSphere(DataPoints points, double radius) {
        return new Sphere(points, radius);
    }

    /**
     * Creates an ellipsoid of the given semi-axes centered on the single position of the given
     * sequence. The sequence is taken as-is, so the caller may give the ellipsoid the attributes
     * carried by it. An ellipsoid whose semi-axes are all equal is a {@linkplain #createSphere
     * sphere} of that radius.
     *
     * @param  points    the center of the ellipsoid, as a sequence of exactly one position.
     * @param  semiAxes  length of the semi-axis along each axis of the coordinate system,
     *                   one value per dimension. Each of them must be positive.
     * @return the ellipsoid of the given center and semi-axes.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position,
     *         or if the number of semi-axes is not the number of dimensions.
     */
    public Ellipsoid createEllipsoid(DataPoints points, double... semiAxes) {
        return new Ellipsoid(points, semiAxes);
    }

    /**
     * Returns a surface whose up-normal points the opposite way to the given one's.
     * This is what a GML {@code gml:OrientableSurface} with {@code orientation="-"} describes.
     */
    public Surface createReversed(Surface base) {
        return new DefaultReversedSurface(base);
    }

    public MultiPolygon createMultiPolygon(Polygon ... geometries) {
        return new DefaultMultiPolygon(geometries);
    }

    public MultiPolygon createMultiPolygon(CoordinateReferenceSystem crs, Polygon ... geometries) {
        return new DefaultMultiPolygon(crs, geometries);
    }

    public <T extends Surface> MultiSurface<T> createMultiSurface(T ... geometries) {
        return new DefaultMultiSurface<>(geometries);
    }

    public <T extends Surface> MultiSurface<T> createMultiSurface(CoordinateReferenceSystem crs, T ... geometries) {
        return new DefaultMultiSurface<>(crs, geometries);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Solids /////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    public Polyhedron createPolyhedron(MultiPolygon exteriorShell, List<MultiPolygon> interiorShells) {
        return new DefaultPolyhedron(exteriorShell, interiorShells);
    }

    /**
     * Creates a parametric curve solid whose three families of curves are b-splines.
     *
     * @param  points         positions at the knots of the parameter grid, in row-major order.
     *                        There shall be {@code rows} × {@code columns} × {@code files} of them.
     * @param  controlPoints  control points in row-major order, or {@code null} if none.
     * @param  rows           number of horizontal rows in the parameter grid.
     * @param  columns        number of vertical columns in the parameter grid.
     * @param  files          number of depth files in the parameter grid.
     */
    public BSplineSolid createBSolidSpline(DataPoints points, List<DirectPosition> controlPoints,
            int rows, int columns, int files)
    {
        return new DefaultBSplineSolid(points, controlPoints, rows, columns, files);
    }

    /**
     * Creates a prism by extruding the given base shape between two limits.
     *
     * @param  base            base shape of the prism, for example a polygon or a circle.
     * @param  extrusionRange  lower and upper limits of the extrusion.
     * @param  extrusionCrs    coordinate reference system of the extrusion range.
     */
    public Prism createPrism(Geometry base, NumberRange<?> extrusionRange, SingleCRS extrusionCrs) {
        return new DefaultPrism(base, extrusionRange, extrusionCrs);
    }

    public MultiPolyhedron createMultiPolyhedron(Polyhedron ... solids) {
        return new DefaultMultiPolyhedron(solids);
    }

    public MultiPolyhedron createMultiPolyhedron(CoordinateReferenceSystem crs, Polyhedron ... solids) {
        return new DefaultMultiPolyhedron(crs, solids);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Heterogeneous collections //////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    public <T extends Geometry> GeometryCollection<T> createGeometryCollection(T ... geometries) {
        return new DefaultGeometryCollection<>(geometries);
    }

    public <T extends Geometry> GeometryCollection<T> createGeometryCollection(CoordinateReferenceSystem crs, T ... geometries) {
        return new DefaultGeometryCollection<>(crs, geometries);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Implicit shapes ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////

    /**
     * Creates a ray starting from the given position.
     * A ray is a line which extends to infinity in one direction only.
     * The reference system of the ray is the one of the given position.
     *
     * @param  position   the position the ray starts from, not null.
     * @param  direction  the direction the ray extends toward, not null.
     * @return the ray starting from the given position.
     */
    public Ray createRay(Vector<?> position, Vector<?> direction) {
        return new Ray(position, direction);
    }

    /**
     * Creates a ray starting from the single position of the given sequence.
     * The sequence is taken as-is, so the caller may give the ray the attributes carried by it.
     *
     * @param  points     the position the ray starts from, as a sequence of exactly one position.
     * @param  direction  the direction the ray extends toward, not null.
     * @return the ray starting from the position of the given sequence.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position.
     */
    public Ray createRay(DataPoints points, Vector<?> direction) {
        return new Ray(points, direction);
    }

    /**
     * Creates a plane centered on the given position and perpendicular to the given normal.
     * A plane is a sheet with no side, both of its faces belonging to it; for a surface which
     * divides the space in two, see {@link #createHyperPlane(Vector, Vector)}. The plane is
     * unbounded until {@link Plane#setSizeX(double)} and {@link Plane#setSizeZ(double)} give it
     * finite extents, making it a quad.
     * The reference system of the plane is the one of the given position.
     *
     * @param  position  the position the plane is centered on, not null.
     * @param  normal    the direction the plane is perpendicular to, not null.
     * @return the plane centered on the given position.
     */
    public Plane createPlane(Vector<?> position, Vector<?> normal) {
        return new Plane(position, normal);
    }

    /**
     * Creates a plane centered on the single position of the given sequence.
     * The sequence is taken as-is, so the caller may give the plane the attributes carried by it.
     *
     * @param  points  the position the plane is centered on, as a sequence of exactly one position.
     * @param  normal  the direction the plane is perpendicular to, not null.
     * @return the plane centered on the position of the given sequence.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position.
     */
    public Plane createPlane(DataPoints points, Vector<?> normal) {
        return new Plane(points, normal);
    }

    /**
     * Creates a hyperplane passing by the given position and perpendicular to the given normal.
     * Unlike a {@linkplain #createPlane(Vector, Vector) plane}, a hyperplane divides the geometric
     * space in two, and therefore has a side.
     * The reference system of the hyperplane is the one of the given position.
     *
     * @param  position  a position the hyperplane passes by, not null.
     * @param  normal    the direction the hyperplane is perpendicular to, not null.
     * @return the hyperplane passing by the given position.
     */
    public HyperPlane createHyperPlane(Vector<?> position, Vector<?> normal) {
        return new HyperPlane(position, normal);
    }

    /**
     * Creates a hyperplane passing by the single position of the given sequence.
     * The sequence is taken as-is, so the caller may give the hyperplane the attributes carried by it.
     *
     * @param  points  a position the hyperplane passes by, as a sequence of exactly one position.
     * @param  normal  the direction the hyperplane is perpendicular to, not null.
     * @return the hyperplane passing by the position of the given sequence.
     * @throws IllegalArgumentException if the given sequence does not hold exactly one position.
     */
    public HyperPlane createHyperPlane(DataPoints points, Vector<?> normal) {
        return new HyperPlane(points, normal);
    }
}
