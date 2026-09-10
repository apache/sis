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

import org.apache.sis.geometries.CurveInterpolation;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;

/**
 * An approximating spline using the Bézier (Bernstein) polynomials as partition of unity.
 *
 * <p>A sequence of <var>n</var> + 1 control points defines a segment of degree <var>n</var> as
 * <var>c</var>(<var>u</var>) = Σ <var>J</var><sub><var>n</var>,<var>i</var></sub>(<var>u</var>)⋅<var>P<sub>i</sub></var>
 * for <var>u</var> ∈ [0 … 1], where
 * <var>J</var><sub><var>n</var>,<var>i</var></sub>(<var>u</var>) = C(<var>n</var>,<var>i</var>)⋅<var>u<sup>i</sup></var>⋅(1 − <var>u</var>)<sup><var>n</var>−<var>i</var></sup>.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The only knots are 0 and 1, of multiplicity <var>n</var> − 1.</li>
 *   <li>The curve is forced to pass only through the first and the last control point.</li>
 *   <li>For a uniform knot sequence of degree <var>n</var>, the number of control points is
 *       <var>s</var>⋅<var>n</var> + 1 for some positive integer <var>s</var>, each subsequence of
 *       <var>n</var> + 1 points starting at a multiple of <var>n</var> defining one segment.</li>
 *   <li>Such a composite is always continuous, since the last control point of a segment is the
 *       first one of the next; it is C¹ when the difference of the last two control points of a
 *       segment equals the difference of the first two of the next one.</li>
 * </ul>
 *
 * <p>Note: for <var>n</var> = 1 a Bézier segment is geometrically a line segment.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.13.7
 */
@UML(identifier="Bezier", specification=ISO_19107)
public non-sealed interface Bezier extends PolynomialSpline, BSplineCurve {

    /**
     * Returns {@link CurveInterpolation#BEZIER_SPLINE}.
     * This value takes precedence over the interpolations declared by the two parent interfaces,
     * a Bézier curve being at the same time a polynomial spline and a b-spline.
     *
     * @see ISO 19107:2019 - 7.13.7
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.BEZIER_SPLINE;
    }
}
