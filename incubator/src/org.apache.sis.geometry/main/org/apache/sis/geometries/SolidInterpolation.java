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
 * The interpolation mechanisms which a {@link Solid} may use to parameterize its interior.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.30
 */
@UML(identifier="SolidInterpolation", specification=ISO_19107)
public enum SolidInterpolation {
    /**
     * The solid is defined by its boundary only, which leaves its interior unspecified.
     * This is the boundary representation (B-REP).
     */
    NONE,
    /**
     * Control points organized in a 3-dimensional array, each cell of which is interpolated
     * trilinearly over the unit cube of the parameter space.
     */
    LINEAR,
    /**
     * The solid is divided into tetrahedra, each of them parameterized by the barycentric
     * coordinates of its four control points. A solid using this mechanism is always the convex
     * hull of its vertices. This is the 3-dimensional counterpart of a triangulated surface.
     */
    TETRAHEDRON,
    /**
     * Control points organized in a 3-dimensional irregular grid, each grid line parallel to a
     * parameter axis being associated with a curve parameterization. The union of all the curve
     * images gives a complete parameterization of the solid.
     */
    PARAMETRIC_CURVE,
    /**
     * Parametric curve interpolation in which the interpolating curves are spline functions.
     */
    SPLINE,
    /**
     * Control points organized in a 3-dimensional irregular grid, each cell of which is covered by
     * a Bézier spline function.
     */
    BEZIER,
    /**
     * Control points organized in a 3-dimensional irregular grid, each cell of which is covered by
     * a basis spline function.
     */
    BSPLINE,
    /**
     * B-spline in homogeneous coordinates, therefore a rational spline whose associated
     * {@code isRational} flag is {@code true}.
     */
    NURBS
}
