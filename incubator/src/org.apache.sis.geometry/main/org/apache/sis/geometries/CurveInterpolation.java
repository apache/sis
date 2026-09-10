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
 * The interpolation mechanisms which a {@link Curve} may use between its data points.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>Every mechanism preserves the underlying geometric object when a curve segment is cut at
 *       any of its points, at least within a controllable accuracy.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.24
 */
@UML(identifier="CurveInterpolation", specification=ISO_19107)
public enum CurveInterpolation {
    /**
     * Interpolation delegated to the segments of a composite curve,
     * each of which may use a different mechanism.
     */
    COMPOSITE_CURVE,
    /**
     * Interpolation performed separately on disjoint projections of the coordinate system,
     * as in a {@link org.apache.sis.geometries.curve.ProductCurve}.
     */
    PRODUCT_CURVE,
    /**
     * Straight line between each consecutive pair of data points,
     * in the coordinate system of the curve.
     */
    LINEAR,
    /**
     * Curve of shortest length between each consecutive pair of data points,
     * determined on the geometric reference surface of the curve.
     */
    GEODESIC,
    /**
     * Curve of constant azimuth crossing all meridians at the same angle, also called loxodrome.
     */
    RHUMB,
    /**
     * Circular arc through consecutive data points, each control point being the center of the
     * circle passing through two consecutive data points and touching the previous and next arcs.
     */
    CIRCULAR,
    /**
     * One of the classic spirals, generated at each control point so that they form a continuous curve.
     */
    SPIRAL,
    /**
     * Cornu spiral, a particular spiral whose curvature varies linearly with arc length.
     */
    CLOTHOID,
    /**
     * Elliptic arc, generated in the tangent space at the center of the ellipse
     * then projected on the reference surface by the exponential map.
     */
    ELLIPTICAL,
    /**
     * Conic section determined by five consecutive data points in the tangent space.
     */
    CONIC,
    /**
     * Piecewise polynomial function covering data points ordered as a polyline.
     * The degree of continuity is normally determined by the degree of the polynomials.
     */
    POLYNOMIAL_SPLINE,
    /**
     * Piecewise polynomial or spline function using the Bézier (Bernstein) basis.
     */
    BEZIER_SPLINE,
    /**
     * Piecewise polynomial function defined using the b-spline basis functions
     * over control points ordered as a polyline.
     */
    BSPLINE,
    /**
     * Rational b-spline, i.e. a b-spline whose control points are expressed in homogeneous
     * coordinates so that the interpolation is a quotient of polynomials.
     */
    NURBS,
    /**
     * Interpolation performed differently on the various projections of the coordinate system.
     */
    PRODUCT,
    /**
     * Interpolation delegated to the components of a composite curve.
     */
    COMPOSITE,

    /**
     * Interpolation derived from the interior parameterization of a solid.
     *
     * <p>Note: this value appears in the ISO 19107 UML (figure 2) but not in its
     * {@code CurveInterpolation} table.</p>
     */
    SOLID
}
