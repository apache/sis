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

import org.apache.sis.geometries.Curve;
import org.apache.sis.geometries.CurveInterpolation;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.maths.Array;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A general conic section curve, canonically
 * <var>ρ</var> = <var>ed</var> ∕ (1 + <var>e</var>⋅cos <var>θ</var>) in polar coordinates,
 * where <var>e</var> is the eccentricity and <var>d</var> the distance to the directrix.
 *
 * <p>A conic is drawn in the tangent plane at its {@linkplain #getControlPoints() control point},
 * then projected on the geometric reference surface by the exponential map. On a plane this
 * projection is the identity.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>Five distinct data points determine a single conic section, so each arc consumes four new
 *       data points plus the one shared with the previous arc.</li>
 *   <li>The number of arcs equals the number of control points, and the number of data points is
 *       four times the number of arcs plus one.</li>
 *   <li>The first control point is the center of the exponential map used to build the first arc.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.9.5
 */
@UML(identifier="Conic", specification=ISO_19107)
public sealed interface Conic extends Curve
        permits Arc,
                EllipticArc
{

    /**
     * Returns {@link CurveInterpolation#CONIC}.
     *
     * @see ISO 19107:2019 - 7.9.5
     */
    @UML(identifier="interpolation", specification=ISO_19107)
    @Override
    default CurveInterpolation getInterpolation() {
        return CurveInterpolation.CONIC;
    }

    /**
     * Points lying on this conic, five of them being needed to determine each arc.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>At least five data points.</li>
     *   <li>If this conic {@linkplain #isCycle() is a cycle}, the first data point also closes the
     *       curve and therefore acts as the last one.</li>
     * </ul>
     *
     * @return conic data points.
     *
     * @see ISO 19107:2019 - 7.9.5.2
     */
    @UML(identifier="dataPoints", specification=ISO_19107)
    @Override
    DataPoints getDataPoints();

    /**
     * Centers of the exponential maps in which the arcs of this conic are constructed.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>At least one control point, and as many of them as there are arcs.</li>
     * </ul>
     *
     * @return conic control points.
     *
     * @see ISO 19107:2019 - 7.9.5.2
     */
    @UML(identifier="controlPoints", specification=ISO_19107)
    @Override
    Array getControlPoints();

    /**
     * Returns whether this conic closes on itself, in which case the first data point is reused as
     * the last one.
     *
     * @return {@code true} if this conic is a complete closed curve.
     *
     * @see ISO 19107:2019 - 7.9.5.2
     */
    @UML(identifier="isCycle", specification=ISO_19107)
    boolean isCycle();


}
