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

import java.util.List;
import org.apache.sis.geometries.Curve;
import org.apache.sis.maths.Vector;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A curve defined indirectly by its curvature, and by its torsion when it is not planar.
 *
 * <p>By the fundamental theorem of curves, a curvature function determines a plane curve, and a
 * curvature together with a torsion function determines a space curve, both up to a translation and
 * a rotation. Spirals are built in the tangent space at their start point, then projected on the
 * geometric reference surface.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>A spiral is C³ and its curvature is strictly monotonic.</li>
 * </ul>
 *
 * <p>Note: the lateral acceleration felt by a vehicle moving at speed <var>v</var> along a curve is
 * <var>v</var>²⋅<var>κ</var>, so controlling the curvature controls that force. This is why spirals
 * are used as transition curves in road and railway design.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.11.1, 7.11.2
 */
@UML(identifier="Spiral", specification=ISO_19107)
public sealed interface Spiral extends Curve
        permits Clothoid
{

    /**
     * Curvature of this spiral as a function of arc length.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The arc length is measured from a fixed point on the infinite spiral, which is not
     *       necessarily on this curve.</li>
     *   <li>The domain of this function is the interval between the first and the last
     *       {@linkplain #getKnots() knot} of this curve.</li>
     * </ul>
     *
     * @return curvature function of this spiral.
     *
     * @see ISO 19107:2019 - 7.11.2.2
     */
    @UML(identifier="curvature", specification=ISO_19107)
    RealFunction getCurvature();

    /**
     * Torsion of this spiral as a function of arc length, or {@code null} if this spiral is planar.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>If this function is absent, this spiral is planar and the
     *       {@linkplain #getCurvature() curvature} alone defines it.</li>
     *   <li>If this function is present and non-zero at least at one position,
     *       this spiral is not planar.</li>
     *   <li>The domain of this function is the same as the domain of the curvature function.</li>
     * </ul>
     *
     * @return torsion function of this spiral, or {@code null} if planar.
     *
     * @see ISO 19107:2019 - 7.11.2.3
     */
    @UML(identifier="torsion", specification=ISO_19107)
    RealFunction getTorsion();

    /**
     * Orthonormal frame located at the start point of this spiral, in which it is constructed.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Two or three mutually orthogonal unit vectors forming a right-handed frame.</li>
     *   <li>If the frame has two vectors, this spiral is planar; the first vector is its tangent
     *       and the second one its normal.</li>
     *   <li>If the frame has three vectors, this spiral is not planar and its
     *       {@linkplain #getTorsion() torsion} is not everywhere zero.</li>
     * </ul>
     *
     * @return frame at the start point of this spiral.
     *
     * @see ISO 19107:2019 - 7.11.2.4
     */
    @UML(identifier="startFrame", specification=ISO_19107)
    List<Vector> getStartFrame();
}
