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
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A curve whose coordinates are given by real functions of a single construction parameter,
 * <var>c</var>(<var>t</var>) = Σ <var>f<sub>i</sub></var>(<var>t</var>)⋅<var>P<sub>i</sub></var>.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>Coordinates are treated as vectors in a Euclidean space, therefore the geometric reference
 *       surface is planar or the coordinate system is locally treated as such.</li>
 *   <li>The natural engineering space is the tangent space at the initial control point, whose
 *       basis is made of the differentials of the coordinate curves.</li>
 * </ul>
 *
 * <p>Note: interpolation areas are usually kept small so that the scales of the different
 * directions stay approximately constant.</p>
 *
 * @param  <T>  type of the arcs composing this curve.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.7.5
 */
@UML(identifier="FunctionCurve", specification=ISO_19107)
public sealed interface FunctionCurve<T extends FunctionArc> extends Curve
        permits PolynomialCurve
{

    /**
     * Number of arcs of this curve, therefore the number of real functions needed.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>This is the number of intervals in the {@linkplain #getKnots() knot} array,
     *       i.e. its length minus one.</li>
     * </ul>
     *
     * @return number of arcs of this curve.
     *
     * @see ISO 19107:2019 - 7.7.5.2
     */
    @UML(identifier="numArc", specification=ISO_19107)
    Integer getNumArc();

    /**
     * Returns the arc of this curve associated with the knot interval at the given index.
     *
     * @param  idx  index of the arc, from 0 inclusive to {@link #getNumArc()} exclusive.
     * @return arc covering the knot interval at the given index.
     *
     * @see ISO 19107:2019 - 7.7.5.4
     */
    @UML(identifier="segment", specification=ISO_19107)
    T getSegment(int idx);
}
