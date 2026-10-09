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
import org.apache.sis.geometries.curve.Line;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.curve.MultiCurve;
import org.apache.sis.geometries.curve.MultiLineString;
import org.apache.sis.geometries.curve.NurbCurve;
import org.apache.sis.geometries.curve.OffsetCurve;
import org.apache.sis.geometries.curve.PolynomialSpline;
import org.apache.sis.geometries.curve.ProductCurve;
import org.apache.sis.geometries.curve.Rhumb;
import org.apache.sis.geometries.curve.Spiral;
import org.apache.sis.geometries.curve.SplineCurve;
import org.apache.sis.geometries.point.MultiPoint;
import org.apache.sis.geometries.solid.BSplineSolid;
import org.apache.sis.geometries.surface.Capsule;
import org.apache.sis.geometries.surface.Cylinder;
import org.apache.sis.geometries.surface.Ellipsoid;
import org.apache.sis.geometries.solid.Frustrum;
import org.apache.sis.geometries.solid.MultiPolyhedron;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Sphere;
import org.apache.sis.geometries.surface.BSplineSurface;
import org.apache.sis.geometries.surface.BilinearGrid;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.MultiPolygon;
import org.apache.sis.geometries.surface.MultiSurface;
import org.apache.sis.geometries.surface.NurbSurface;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.PolyhedralSurface;
import org.apache.sis.geometries.surface.TIN;
import org.apache.sis.geometries.surface.Triangle;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * The subtypes of {@link Geometry} supported by an implementation.
 *
 * <p>This code list is the software contract between an implementation and its users regarding the
 * geometry types it supports, based on dimension and interpolation mechanism.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>All the {@link Geometry} subtypes supported by an implementation are enumerated here.</li>
 *   <li>{@link #GEOMETRY}, {@link #EMPTY} and {@link #POINT} are always present.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.6
 */
@UML(identifier="GeometryType", specification=ISO_19107)
public enum GeometryType {
    /**
     * The empty set, of topological dimension −1.
     */
    EMPTY(Empty.class),
    /**
     * The abstract root of all geometry types.
     */
    GEOMETRY(Geometry.class),
    /**
     * A collection behaving as the set union of its elements.
     */
    COLLECTION(GeometryCollection.class),

    //point types
    /**
     * A single location, of topological dimension 0.
     */
    POINT(Point.class),
    /**
     * A collection of points.
     */
    MULTIPOINT(MultiPoint.class),

    //curve types
    /**
     * A curve of unspecified interpolation, of topological dimension 1.
     */
    CURVE(Curve.class),
    /**
     * A curve using a linear interpolation in the coordinate system.
     */
    LINE(Line.class),
    /**
     * A sequence of segments using a linear interpolation.
     */
    LINESTRING(LineString.class),
    /**
     * A {@linkplain #LINESTRING} whose start point is its end point.
     */
    LINEARRING(LinearRing.class),
    /**
     * A sequence of segments using a circular interpolation.
     */
    CIRCULARSTRING(CircularString.class),
    /**
     * A curve made of contiguous curves of possibly different interpolations.
     */
    COMPOUNDCURVE(CompoundCurve.class),
    /**
     * A curve following the shortest path on the geometric reference surface.
     */
    GEODESIC(Geodesic.class),
    /**
     * A curve of constant azimuth, also called loxodrome.
     */
    RHUMB(Rhumb.class),
    /**
     * A portion of a circle defined by three points.
     */
    ARC(Arc.class),
    /**
     * A portion of a circle defined by two points and a bulge factor.
     */
    ARCBYBULGE(ArcByBulge.class),
    /**
     * A portion of a circle defined by its center, its radius and two angles.
     */
    ARCBYCENTERPOINT(ArcByCenterPoint.class),
    /**
     * A closed {@linkplain #ARC}.
     */
    CIRCLE(Circle.class),
    /**
     * A portion of an ellipse.
     */
    ELLIPTICARC(EllipticArc.class),
    /**
     * A curve defined by a conic section.
     */
    CONIC(Conic.class),
    /**
     * A curve whose curvature varies linearly with its length, also called Euler spiral.
     */
    CLOTHOID(Clothoid.class),
    /**
     * A curve winding around a center point.
     */
    SPIRAL(Spiral.class),
    /**
     * A curve at a constant distance from another curve.
     */
    OFFSETCURVE(OffsetCurve.class),
    /**
     * A curve defined as the product of two other curves.
     */
    PRODUCTCURVE(ProductCurve.class),
    /**
     * A curve interpolated by spline functions.
     */
    SPLINECURVE(SplineCurve.class),
    /**
     * A curve interpolated by B-spline basis functions.
     */
    BSPLINECURVE(BSplineCurve.class),
    /**
     * A curve interpolated by cubic polynomials.
     */
    CUBICSPLINE(CubicSpline.class),
    /**
     * A curve interpolated by polynomials of arbitrary degree.
     */
    POLYNOMIALSPLINE(PolynomialSpline.class),
    /**
     * A curve interpolated by Bernstein polynomials over its control points.
     */
    BEZIER(Bezier.class),
    /**
     * A curve interpolated by non-uniform rational B-spline basis functions.
     */
    NURBSCURVE(NurbCurve.class),
    /**
     * A collection of curves.
     */
    MULTICURVE(MultiCurve.class),
    /**
     * A collection of {@linkplain #LINESTRING}.
     */
    MULTILINESTRING(MultiLineString.class),

    //surface types
    /**
     * A surface of unspecified interpolation, of topological dimension 2.
     */
    SURFACE(Surface.class),
    /**
     * A surface defined only by its boundary rings and a spanning surface.
     */
    POLYGON(Polygon.class),
    /**
     * A {@linkplain #POLYGON} with exactly three distinct non-collinear points and no interior ring.
     */
    TRIANGLE(Triangle.class),
    /**
     * A {@linkplain #POLYGON} whose rings may use any curve interpolation.
     */
    CURVEPOLYGON(CurvePolygon.class),
    /**
     * A surface made of contiguous polygon patches.
     */
    POLYHEDRALSURFACE(PolyhedralSurface.class),
    /**
     * A {@linkplain #POLYHEDRALSURFACE} whose patches are triangles.
     */
    TIN(TIN.class),
    /**
     * A surface interpolated bilinearly over a grid of control points.
     */
    BILINEARGRID(BilinearGrid.class),
    /**
     * A surface interpolated by B-spline basis functions.
     */
    BSPLINESURFACE(BSplineSurface.class),
    /**
     * A surface interpolated by non-uniform rational B-spline basis functions.
     */
    NURBSSURFACE(NurbSurface.class),
    /**
     * A collection of {@linkplain #POLYGON}.
     */
    MULTIPOLYGON(MultiPolygon.class),
    /**
     * A collection of surfaces.
     */
    MULTISURFACE(MultiSurface.class),

    //solid types
    /**
     * A solid of unspecified interpolation, of topological dimension 3.
     */
    SOLID(Solid.class),
    /**
     * A solid bounded by planar faces.
     */
    POLYHEDRON(Polyhedron.class),
    /**
     * A collection of {@linkplain #POLYHEDRON}.
     */
    MULTIPOLYHEDRON(MultiPolyhedron.class),
    /**
     * A solid interpolated by B-spline basis functions.
     */
    BSPLINESOLID(BSplineSolid.class),
    /**
     * A solid bounded by the positions at a constant distance from a center point.
     */
    SPHERE(Sphere.class),
    /**
     * A {@linkplain #SPHERE} scaled by a distinct factor along each axis of the coordinate system.
     */
    ELLIPSOID(Ellipsoid.class),
    /**
     * A solid bounded by a lateral surface and two parallel bases.
     */
    CYLINDER(Cylinder.class),
    /**
     * A {@linkplain #CYLINDER} closed by a half-sphere at each end.
     */
    CAPSULE(Capsule.class),
    /**
     * The portion of a solid lying between two parallel planes.
     */
    FRUSTRUM(Frustrum.class),
    /**
     * A solid swept by translating a surface along a direction.
     */
    PRISM(Prism.class),

    /*
     * Types having no equivalent in OGC Simple Feature Access.
     */
    /**
     * A rectangle whose sides are parallel to the axes of the coordinate system.
     */
    BBOX(BBox.class),
    /**
     * An unbounded flat surface in a three-dimensional coordinate system.
     */
    PLANE(Plane.class),
    /**
     * A half-line, defined by an origin and a direction.
     */
    RAY(Ray.class),
    /**
     * The generalization of {@link #PLANE} to a coordinate system of any dimension.
     */
    HYPERPLANE(HyperPlane.class);


    final Class<? extends Geometry> javaClass;

    private GeometryType(Class<? extends Geometry> c) {
        this.javaClass = c;
    }

    public Class<? extends Geometry> getJavaClass() {
        return javaClass;
    }

    public boolean isAssignableFrom(GeometryType type) {
        return javaClass.isAssignableFrom(type.javaClass);
    }

    public boolean isInstance(Geometry geom) {
        return javaClass.isInstance(geom);
    }

}
