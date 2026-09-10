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

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A parametric curve solid whose three families of curves are b-splines, so that its interior is
 * <var>s</var>(<var>u</var>,<var>v</var>,<var>t</var>) =
 * ΣΣΣ <var>N<sub>i,p</sub></var>(<var>u</var>)⋅<var>N<sub>j,q</sub></var>(<var>v</var>)⋅<var>N<sub>k,r</sub></var>(<var>t</var>)⋅<var>P<sub>i,j,k</sub></var>.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The {@linkplain #getHorizontalCurveType() horizontal},
 *       {@linkplain #getVerticalCurveType() vertical} and
 *       {@linkplain #getDepthCurveType() depth} curve types are all b-splines.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 9.3.2
 */
@UML(identifier="BSolidSpline", specification=ISO_19107)
public non-sealed interface BSolidSpline extends ParametricCurveSolid {

}
