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
 * A piecewise polynomial or rational parametric curve described by control points and by the
 * b-spline basis functions built recursively from a knot sequence.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>If the control points are not in homogeneous form, or are but with all weights equal,
 *       the curve is piecewise polynomial; otherwise it is piecewise rational.</li>
 *   <li>Knot values are non-decreasing, the <var>i</var>-th knot interval being
 *       [<var>u<sub>i</sub></var> … <var>u</var><sub><var>i</var>+1</sub>).</li>
 *   <li>A quasi-uniform b-spline whose interior knots have a multiplicity equal to its degree is a
 *       piecewise Bézier curve; with only two knots of multiplicity degree + 1 it is a single
 *       {@link Bezier} curve.</li>
 *   <li>When the knot distribution is left unspecified, knots are evenly spaced with multiplicity
 *       1, except at the ends where their multiplicity is degree + 1.</li>
 * </ul>
 *
 * <p>Note: a uniform b-spline of degree 1 is equivalent to a {@link LineString}.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.13.8
 */
@UML(identifier="BSplineCurve", specification=ISO_19107)
public sealed interface BSplineCurve extends SplineCurve
        permits Bezier,
                NurbCurve
{

    /**
     * Returns {@link CurveInterpolation#BSPLINE}.
     *
     * @see ISO 19107:2019 - 7.13.8
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.BSPLINE;
    }
}
