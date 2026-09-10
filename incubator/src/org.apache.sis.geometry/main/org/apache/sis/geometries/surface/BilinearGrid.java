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

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A parametric curve surface using polylines as both horizontal and vertical curves.
 *
 * <p>Each cell of the parameter grid is interpolated bilinearly over the unit square, so that on a
 * 2 by 2 grid the surface is
 * <var>S</var>(<var>u</var>,<var>v</var>) =
 * (1−<var>u</var>)(1−<var>v</var>)<var>P</var><sub>00</sub> +
 * (1−<var>u</var>)<var>v</var><var>P</var><sub>01</sub> +
 * <var>u</var>(1−<var>v</var>)<var>P</var><sub>10</sub> +
 * <var>uv</var><var>P</var><sub>11</sub>.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The {@linkplain #getHorizontalCurveType() horizontal} and
 *       {@linkplain #getVerticalCurveType() vertical} curve types are both linear.</li>
 *   <li>A grid cell is planar only when its four positions are coplanar, in which case that cell is
 *       a {@link Polygon}.</li>
 * </ul>
 *
 * <p>Note: this is not a {@link PolyhedralSurface}, because a grid cell is a regular surface which
 * is not necessarily planar.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 8.3.4
 */
@UML(identifier="BilinearGrid", specification=ISO_19107)
public non-sealed interface BilinearGrid extends ParametricCurveSurface {

}
