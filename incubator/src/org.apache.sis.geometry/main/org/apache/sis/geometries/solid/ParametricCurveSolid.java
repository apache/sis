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
package org.apache.sis.geometries.solid;

import java.util.List;
import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Solid;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;


/**
 * A solid built by grouping curves over a 3-dimensional parameter space.
 *
 * <p>This interface does for solids what
 * {@link org.apache.sis.geometries.surface.ParametricCurveSurface} does for surfaces. Fixing two of
 * the three parameters yields a one-parameter family of curves in each of the horizontal, vertical
 * and depth directions. Those names refer to the parameter space and need not match any direction
 * of the coordinate reference system.</p>
 *
 * <p>Note: the parameter space is rectangular and of the same dimension as the geometry, which lets
 * information attached to knot coordinates be mapped to the corresponding positions of the
 * coordinate reference system, in a way similar to linear referencing (ISO 19148).</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 9.3.1
 */
@UML(identifier="ParametricCurveSolid", specification=ISO_19107)
public sealed interface ParametricCurveSolid extends Solid
        permits BSolidSpline
{

    /**
     * Type of the curves used to traverse this solid horizontally.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The returned type is declared in the local {@link GeometryType} code list.</li>
     *   <li>The returned type is a subtype of {@link Curve}.</li>
     * </ul>
     *
     * @return type of the horizontal section curves.
     *
     * @see ISO 19107:2019 - 9.3.1.2
     */
    @UML(identifier="horizontalCurveType", specification=ISO_19107)
    GeometryType getHorizontalCurveType();

    /**
     * Type of the curves used to traverse this solid vertically.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The returned type is declared in the local {@link GeometryType} code list.</li>
     *   <li>The returned type is a subtype of {@link Curve}.</li>
     * </ul>
     *
     * @return type of the vertical section curves.
     *
     * @see ISO 19107:2019 - 9.3.1.2
     */
    @UML(identifier="verticalCurveType", specification=ISO_19107)
    GeometryType getVerticalCurveType();

    /**
     * Type of the curves used to traverse this solid in depth.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The returned type is declared in the local {@link GeometryType} code list.</li>
     *   <li>The returned type is a subtype of {@link Curve}.</li>
     * </ul>
     *
     * @return type of the depth section curves.
     *
     * @see ISO 19107:2019 - 9.3.1.2
     */
    @UML(identifier="depthCurveType", specification=ISO_19107)
    GeometryType getDepthCurveType();

    /**
     * Number of horizontal rows in the parameter grid.
     *
     * @return number of rows in the parameter grid.
     *
     * @see ISO 19107:2019 - 9.3.1.2
     */
    @UML(identifier="rows", specification=ISO_19107)
    int getRows();

    /**
     * Number of vertical columns in the parameter grid.
     *
     * @return number of columns in the parameter grid.
     *
     * @see ISO 19107:2019 - 9.3.1.2
     */
    @UML(identifier="columns", specification=ISO_19107)
    int getColumns();

    /**
     * Number of depth files in the parameter grid.
     *
     * @return number of files in the parameter grid.
     *
     * @see ISO 19107:2019 - 9.3.1.2
     */
    @UML(identifier="files", specification=ISO_19107)
    int getFiles();

    /**
     * Positions of this solid at the knots of the parameter grid, so that the data point at
     * (<var>i</var>,<var>j</var>,<var>k</var>) is
     * <var>S</var>(<var>u<sub>i</sub></var>,<var>v<sub>j</sub></var>,<var>t<sub>k</sub></var>).
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The sequence holds {@link #getRows()} × {@link #getColumns()} × {@link #getFiles()}
     *       positions, stored in row-major order.</li>
     * </ul>
     *
     * @return solid data points.
     *
     * @see ISO 19107:2019 - 9.3.1.3
     */
    @UML(identifier="dataPoints", specification=ISO_19107)
    @Override
    DataPoints getDataPoints();

    /**
     * Positions used by the section curves which need control points in their definition.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The list holds {@link #getRows()} × {@link #getColumns()} × {@link #getFiles()}
     *       positions, stored in row-major order.</li>
     *   <li>Control points do not necessarily lie inside this solid.</li>
     * </ul>
     *
     * @return solid control points, possibly empty.
     *
     * @see ISO 19107:2019 - 9.3.1.3
     */
    @UML(identifier="controlPoints", specification=ISO_19107)
    @Override
    List<DirectPosition> getControlPoints();

    /**
     * Returns the curve traversing this solid horizontally at the given vertical and depth parameters.
     *
     * @param  b  vertical parameter to keep constant.
     * @param  c  depth parameter to keep constant.
     * @return horizontal section of this solid at the given parameters.
     *
     * @see ISO 19107:2019 - 9.3.1
     */
    Curve getHorizontalCurve(double b, double c);

    /**
     * Returns the curve traversing this solid vertically at the given horizontal and depth parameters.
     *
     * @param  a  horizontal parameter to keep constant.
     * @param  c  depth parameter to keep constant.
     * @return vertical section of this solid at the given parameters.
     *
     * @see ISO 19107:2019 - 9.3.1
     */
    Curve getVerticalCurve(double a, double c);

    /**
     * Returns the curve traversing this solid in depth at the given horizontal and vertical parameters.
     *
     * @param  a  horizontal parameter to keep constant.
     * @param  b  vertical parameter to keep constant.
     * @return depth section of this solid at the given parameters.
     *
     * @see ISO 19107:2019 - 9.3.1
     */
    Curve getDepthCurve(double a, double b);

    /**
     * Returns the position of this solid at the given triplet of parameters.
     *
     * @param  a  horizontal parameter.
     * @param  b  vertical parameter.
     * @param  c  depth parameter.
     * @return position of this solid at the given parameters.
     *
     * @see ISO 19107:2019 - 9.3.1
     */
    DirectPosition getSurface(double a, double b, double c);
}
