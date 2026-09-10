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
 * The interpolation mechanisms which a {@link Surface} may use between its data points.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>When several values match the method actually used, the most restrictive one applies.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.27
 */
@UML(identifier="SurfaceInterpolation", specification=ISO_19107)
public enum SurfaceInterpolation {
    /**
     * The interior of the surface is unspecified, and assumed to follow the geometric reference
     * surface defined by the coordinate reference system.
     */
    NONE,
    /**
     * The surface lies in a single plane, which shall also contain its boundary.
     */
    PLANAR,
    /**
     * Control points organized in a 2-dimensional array, each cell of which is interpolated
     * bilinearly over the unit square of the parameter space.
     */
    LINEAR,
    /**
     * The surface is a section of a sphere.
     */
    SPHERICAL,
    /**
     * The surface is a section of an ellipsoid.
     */
    ELLIPTICAL,
    /**
     * The surface is a section of a cone.
     */
    CONIC,
    /**
     * Control points organized in adjacent triangles forming small planar segments.
     */
    TRIANGULAR,
    /**
     * Control points organized in a 2-dimensional grid, each cell of which is covered by a surface
     * defined by a family of curves.
     */
    PARAMETRIC_CURVE,
    /**
     * Control points organized in a 2-dimensional irregular grid, each cell of which is covered by
     * a piecewise polynomial spline function.
     */
    POLYNOMIAL_SPLINE,
    /**
     * B-spline in homogeneous coordinates, therefore a rational spline whose associated
     * {@code isRational} flag is {@code true}.
     */
    NURBS,
    /**
     * Control points organized in a 2-dimensional irregular grid, each cell of which is covered by
     * a Bézier spline function.
     */
    BEZIER_SPLINE,
    /**
     * Triangulated irregular network, a triangular interpolation whose triangulation is derived
     * from the data points themselves.
     *
     * <p>Note: this value appears in the ISO 19107 UML (figure 17) but not in its
     * {@code SurfaceInterpolation} table.</p>
     */
    TIN,
    /**
     * Triangular subdivision whose patches are covered by spline functions rather than planes.
     *
     * <p>Note: this value appears in the ISO 19107 UML (figure 17) but not in its
     * {@code SurfaceInterpolation} table.</p>
     */
    TRIANGULATED_SPLINE
}
