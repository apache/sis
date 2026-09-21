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
    EMPTY,
    /**
     * The abstract root of all geometry types.
     */
    GEOMETRY,
    /**
     * A collection behaving as the set union of its elements.
     */
    COLLECTION,

    //point types
    /**
     * A single location, of topological dimension 0.
     */
    POINT,
    /**
     * A collection of points.
     */
    MULTIPOINT,

    //curve types
    /**
     * A curve of unspecified interpolation, of topological dimension 1.
     */
    CURVE,
    /**
     * A curve using a linear interpolation in the coordinate system.
     */
    LINE,
    /**
     * A sequence of segments using a linear interpolation.
     */
    LINESTRING,
    /**
     * A {@linkplain #LINESTRING} whose start point is its end point.
     */
    LINEARRING,
    /**
     * A sequence of segments using a circular interpolation.
     */
    CIRCULARSTRING,
    /**
     * A curve made of contiguous curves of possibly different interpolations.
     */
    COMPOUNDCURVE,
    /**
     * A curve following the shortest path on the geometric reference surface.
     */
    GEODESIC,
    /**
     * A curve of constant azimuth, also called loxodrome.
     */
    RHUMB,
    /**
     * A portion of a circle defined by three points.
     */
    ARC,
    /**
     * A portion of a circle defined by two points and a bulge factor.
     */
    ARCBYBULGE,
    /**
     * A portion of a circle defined by its center, its radius and two angles.
     */
    ARCBYCENTERPOINT,
    /**
     * A closed {@linkplain #ARC}.
     */
    CIRCLE,
    /**
     * A portion of an ellipse.
     */
    ELLIPTICARC,
    /**
     * A curve defined by a conic section.
     */
    CONIC,
    /**
     * A curve whose curvature varies linearly with its length, also called Euler spiral.
     */
    CLOTHOID,
    /**
     * A curve winding around a center point.
     */
    SPIRAL,
    /**
     * A curve at a constant distance from another curve.
     */
    OFFSETCURVE,
    /**
     * A curve defined as the product of two other curves.
     */
    PRODUCTCURVE,
    /**
     * A curve interpolated by spline functions.
     */
    SPLINECURVE,
    /**
     * A curve interpolated by B-spline basis functions.
     */
    BSPLINECURVE,
    /**
     * A curve interpolated by cubic polynomials.
     */
    CUBICSPLINE,
    /**
     * A curve interpolated by polynomials of arbitrary degree.
     */
    POLYNOMIALSPLINE,
    /**
     * A curve interpolated by Bernstein polynomials over its control points.
     */
    BEZIER,
    /**
     * A curve interpolated by non-uniform rational B-spline basis functions.
     */
    NURBSCURVE,
    /**
     * A collection of curves.
     */
    MULTICURVE,
    /**
     * A collection of {@linkplain #LINESTRING}.
     */
    MULTILINESTRING,

    //surface types
    /**
     * A surface of unspecified interpolation, of topological dimension 2.
     */
    SURFACE,
    /**
     * A surface defined only by its boundary rings and a spanning surface.
     */
    POLYGON,
    /**
     * A {@linkplain #POLYGON} with exactly three distinct non-collinear points and no interior ring.
     */
    TRIANGLE,
    /**
     * A {@linkplain #POLYGON} whose rings may use any curve interpolation.
     */
    CURVEPOLYGON,
    /**
     * A surface made of contiguous polygon patches.
     */
    POLYHEDRALSURFACE,
    /**
     * A {@linkplain #POLYHEDRALSURFACE} whose patches are triangles.
     */
    TIN,
    /**
     * A surface interpolated bilinearly over a grid of control points.
     */
    BILINEARGRID,
    /**
     * A surface interpolated by spline functions.
     */
    SPLINESURFACE,
    /**
     * A surface interpolated by B-spline basis functions.
     */
    BSPLINESURFACE,
    /**
     * A surface interpolated by non-uniform rational B-spline basis functions.
     */
    NURBSSURFACE,
    /**
     * A collection of {@linkplain #POLYGON}.
     */
    MULTIPOLYGON,
    /**
     * A collection of surfaces.
     */
    MULTISURFACE,

    //solid types
    /**
     * A solid of unspecified interpolation, of topological dimension 3.
     */
    SOLID,
    /**
     * A solid bounded by planar faces.
     */
    POLYHEDRON,
    /**
     * A collection of {@linkplain #POLYHEDRON}.
     */
    MULTIPOLYHEDRON,
    /**
     * A solid interpolated by spline functions, of topological dimension 3.
     */
    SPLINESOLID,
    /**
     * A solid interpolated by B-spline basis functions.
     */
    BSPLINESOLID,
    /**
     * A solid bounded by the positions at a constant distance from a center point.
     */
    SPHERE,
    /**
     * A solid bounded by a lateral surface and two parallel bases.
     */
    CYLINDER,
    /**
     * A {@linkplain #CYLINDER} closed by a half-sphere at each end.
     */
    CAPSULE,
    /**
     * The portion of a solid lying between two parallel planes.
     */
    FRUSTRUM,
    /**
     * A solid swept by translating a surface along a direction.
     */
    PRISM,

    /*
     * Types having no equivalent in OGC Simple Feature Access.
     */
    /**
     * A rectangle whose sides are parallel to the axes of the coordinate system.
     */
    BBOX,
    /**
     * An unbounded flat surface in a three-dimensional coordinate system.
     */
    PLANE,
    /**
     * A half-line, defined by an origin and a direction.
     */
    RAY,
    /**
     * The generalization of {@link #PLANE} to a coordinate system of any dimension.
     */
    HYPERPLANE,
}
