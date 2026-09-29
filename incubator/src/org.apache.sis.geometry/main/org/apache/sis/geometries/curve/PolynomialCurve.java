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
package org.apache.sis.geometries.curve;

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A function curve whose weights are polynomials.
 *
 * <p>Most polynomial curves are composites of a simple arc defined between each consecutive pair of
 * knots: splines, Bézier splines, b-splines and, through homogeneous coordinates, NURBS. Their
 * constructors usually take enough constraints to form a system of linear equations which can be
 * solved for the polynomial coefficients.</p>
 *
 * @param  <PolynomialArc>  type of the arcs composing this curve.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.7.9
 */
@UML(identifier="PolynomialCurve", specification=ISO_19107)
public sealed interface PolynomialCurve<PolynomialArc> extends FunctionCurve
        permits SplineCurve
{

    /**
     * Maximum degree of the real polynomials used by this curve, which is the order minus one.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The degree is generally one less than the number of control points: a line segment has
     *       two control points and a degree of 1, a quadratic curve has three and a degree of 2.</li>
     *   <li>In a composite curve, the degree gives local control: only the nearest
     *       {@code degree} + 1 control points contribute to a given position.</li>
     * </ul>
     *
     * @return degree of this curve.
     *
     * @see ISO 19107:2019 - 7.7.9.2
     */
    @UML(identifier="degree", specification=ISO_19107)
    int getDegree();
}
