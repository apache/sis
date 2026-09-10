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
package org.apache.sis.geometries.surface;

import java.util.List;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Surface;
import org.apache.sis.geometries.solid.Sphere;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.ReferenceSystem;


/**
 * A surface given by a family of continuous curves,
 * <var>S</var>(<var>u</var>,<var>v</var>): [<var>a</var>…<var>b</var>] ⊗ [<var>c</var>…<var>d</var>] → position.
 *
 * <p>Fixing one parameter yields a one-parameter family of curves: fixing <var>v</var> gives the
 * {@linkplain #getHorizontalCurve(double) horizontal} sections and fixing <var>u</var> the
 * {@linkplain #getVerticalCurve(double) vertical} ones. The terms <cite>horizontal</cite> and
 * <cite>vertical</cite> refer to the parameter space and need not be horizontal or vertical in the
 * coordinate reference system.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The default {@linkplain #upNormal(DirectPosition) up-normal} is the normalized cross
 *       product of the two partial derivatives ∂<var>S</var>∕∂<var>u</var> and
 *       ∂<var>S</var>∕∂<var>v</var>, where both are non-zero.</li>
 *   <li>Where a partial derivative vanishes, the section curves are reparameterized by their local
 *       arc length.</li>
 *   <li>In a 2-dimensional coordinate system, the up-normal extends the local coordinate system with
 *       an upward elevation vector, and the basis of the two partial derivatives is right-handed.</li>
 *   <li>Because a continuous up-normal exists, such a surface is orientable.</li>
 * </ul>
 *
 * <p>Note: the horizontal and vertical curve types characterize the surface. A cylinder uses circles
 * of constant radius and line segments, a cone uses circles of increasing radius and line segments,
 * a sphere uses circles of constant latitude and of constant longitude, and a
 * {@link BilinearGrid} uses polylines in both directions.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 8.3.1, 8.3.2
 */
@UML(identifier="ParametricCurveSurface", specification=ISO_19107)
public sealed interface ParametricCurveSurface extends Surface, ReferenceSystem
        permits BilinearGrid,
                BSplineSurface,
                Sphere
{

    /**
     * Number of rows in the parameter grid, therefore the number of
     * {@linkplain #getHorizontalCurve(double) horizontal} section curves.
     *
     * @return number of rows in the parameter grid.
     *
     * @see ISO 19107:2019 - 8.3.2.3
     */
    @UML(identifier="rows", specification=ISO_19107)
    int getRows();

    /**
     * Number of columns in the parameter grid, therefore the number of
     * {@linkplain #getVerticalCurve(double) vertical} section curves.
     *
     * @return number of columns in the parameter grid.
     *
     * @see ISO 19107:2019 - 8.3.2.4
     */
    @UML(identifier="columns", specification=ISO_19107)
    int getColumns();

    /**
     * Positions used by the section curves which need control points in their definition.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The list holds {@link #getRows()} × {@link #getColumns()} positions,
     *       stored in row-major order.</li>
     *   <li>Control points do not necessarily lie on this surface.</li>
     * </ul>
     *
     * @return surface control points, possibly empty.
     *
     * @see ISO 19107:2019 - 8.3.2.6
     */
    @UML(identifier="controlPoints", specification=ISO_19107)
    @Override
    List<DirectPosition> getControlPoints();

    /**
     * Positions of this surface at the knots of the parameter grid, so that the data point at
     * (<var>i</var>,<var>j</var>) is
     * <var>S</var>(<var>u<sub>i</sub></var>,<var>v<sub>j</sub></var>).
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The sequence holds {@link #getRows()} × {@link #getColumns()} positions,
     *       stored in row-major order.</li>
     * </ul>
     *
     * @return surface data points.
     *
     * @see ISO 19107:2019 - 8.3.2.5
     */
    @UML(identifier="dataPoints", specification=ISO_19107)
    @Override
    DataPoints getDataPoints();

    /**
     * Type of the curves used to traverse this surface horizontally, i.e. at a constant
     * <var>v</var> parameter.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The returned type is declared in the local {@link GeometryType} code list.</li>
     *   <li>The returned type is a subtype of {@link Curve}.</li>
     * </ul>
     *
     * @return type of the horizontal section curves.
     *
     * @see ISO 19107:2019 - 8.3.2.2, 8.3.2.7
     */
    @UML(identifier="horizontalCurveType", specification=ISO_19107)
    GeometryType getHorizontalCurveType();

    /**
     * Type of the curves used to traverse this surface vertically, i.e. at a constant
     * <var>u</var> parameter.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The returned type is declared in the local {@link GeometryType} code list.</li>
     *   <li>The returned type is a subtype of {@link Curve}.</li>
     * </ul>
     *
     * @return type of the vertical section curves.
     *
     * @see ISO 19107:2019 - 8.3.2.2, 8.3.2.8
     */
    @UML(identifier="verticalCurveType", specification=ISO_19107)
    GeometryType getVerticalCurveType();

    /**
     * Construction parameter values matching the {@linkplain #getDataPoints() data points},
     * the first sequence being for the <var>u</var> parameter and the second one for <var>v</var>.
     *
     * @return knot values in the construction (knot) space, one sequence per surface parameter.
     *
     * @see ISO 19107:2019 - 6.4.25.10
     */
    @Override
    List<double[]> getKnots();

    /**
     * Returns the curve traversing this surface horizontally at the given <var>v</var> parameter.
     *
     * <p>Note: the returned curve is generally a transient computed value, not a part of a geometric
     * complex containing this surface.</p>
     *
     * @param  v  vertical parameter to keep constant.
     * @return horizontal section of this surface at the given parameter.
     *
     * @see ISO 19107:2019 - 8.3.2.9
     */
    @UML(identifier="horizontalCurve", specification=ISO_19107)
    Curve getHorizontalCurve(double v);

    /**
     * Returns the curve traversing this surface vertically at the given <var>u</var> parameter.
     *
     * <p>Note: the returned curve is generally a transient computed value, not a part of a geometric
     * complex containing this surface.</p>
     *
     * @param  u  horizontal parameter to keep constant.
     * @return vertical section of this surface at the given parameter.
     *
     * @see ISO 19107:2019 - 8.3.2.10
     */
    @UML(identifier="verticalCurve", specification=ISO_19107)
    Curve getVerticalCurve(double u);

    /**
     * Returns the position of this surface at the given pair of parameters.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The horizontal and the vertical evaluation orders normally give the same result;
     *       where they differ, the horizontal-then-vertical order applies.</li>
     * </ul>
     *
     * @param  u  horizontal parameter.
     * @param  v  vertical parameter.
     * @return position of this surface at the given parameters.
     *
     * @see ISO 19107:2019 - 8.3.2.11
     */
    @UML(identifier="surface", specification=ISO_19107)
    DirectPosition getSurface(double u, double v);


}
