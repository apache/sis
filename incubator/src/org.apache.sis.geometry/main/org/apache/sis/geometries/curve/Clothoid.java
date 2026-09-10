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
 * A spiral whose curvature varies linearly with arc length, also called a Cornu spiral.
 *
 * <p>In suitably chosen coordinates it is given by the Fresnel integrals
 * <var>x</var>(<var>t</var>) = ∫cos(<var>A</var><var>τ</var>²∕2) d<var>τ</var> and
 * <var>y</var>(<var>t</var>) = ∫sin(<var>A</var><var>τ</var>²∕2) d<var>τ</var>,
 * so that <var>A</var>² = <var>R</var>⋅<var>t</var> where <var>R</var> is the local radius of
 * curvature and <var>t</var> the length along the curve.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The {@linkplain #getTorsion() torsion} is null: a clothoid is a planar spiral.</li>
 *   <li>The {@linkplain #getCurvature() curvature} is linear in the arc length measured from the
 *       point where the infinite clothoid has zero curvature.</li>
 * </ul>
 *
 * <p>Note: clothoids are used almost exclusively as transition curves in road and railway
 * construction, between a straight line and a circular arc or between two circular arcs, since they
 * achieve a C² continuous transition between arbitrary curves.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.11.3
 */
@UML(identifier="Clothoid", specification=ISO_19107)
public non-sealed interface Clothoid extends Spiral {

    /**
     * Returns {@link CurveInterpolation#CLOTHOID}.
     *
     * @see ISO 19107:2019 - 7.11.3
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.CLOTHOID;
    }

    /**
     * Returns {@code null}: a clothoid is a planar spiral.
     *
     * @see ISO 19107:2019 - 7.11.3
     */
    @UML(identifier="torsion", specification=ISO_19107)
    @Override
    default RealFunction getTorsion() {
        return null;
    }
}
