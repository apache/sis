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
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Vector;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A chain of circular arcs, each of them centred on a control point and joining two consecutive
 * data points. It is the circular counterpart of a {@link LineString}.
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.9.2
 */
@UML(identifier="Arc", specification=ISO_19107)
public sealed interface Arc extends Conic
        permits Circle
{

    /**
     * Number of circular arcs in this chain.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>Equal to the number of {@linkplain #getControlPoints() control points}, and one less
     *       than the number of {@linkplain #getDataPoints() data points}.</li>
     *   <li>The angular measure of one arc is always less than π radians.</li>
     *   <li>Consecutive arcs sharing the same centre may combine up to a full circle, i.e. 2π
     *       radians, and never reverse their rotation direction.</li>
     * </ul>
     *
     * @return number of arcs in this chain.
     *
     * @see ISO 19107:2019 - 7.9.2.2
     */
    @UML(identifier="numArc", specification=ISO_19107)
    int getNumArc();

    /**
     * Centres of the circles carrying the arcs of this chain, the arc at index <var>i</var> being
     * centred on the control point at index <var>i</var>.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>There are as many control points as {@linkplain #getNumArc() arcs}.</li>
     *   <li>A control point is equidistant from the two data points of its arc.</li>
     *   <li>A control point may be repeated in order to build an arc longer than π radians.</li>
     * </ul>
     *
     * @return centres of the arcs of this chain.
     *
     * @see ISO 19107:2019 - 7.9.2.3
     */
    @UML(identifier="controlPoints", specification=ISO_19107)
    @Override
    Array getControlPoints();

    /**
     * Start and end points of the arcs of this chain, two consecutive arcs sharing a point.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>There is one more data point than {@linkplain #getNumArc() arcs}.</li>
     * </ul>
     *
     * @return arc end points.
     *
     * @see ISO 19107:2019 - 7.9.2.4
     */
    @UML(identifier="dataPoints", specification=ISO_19107)
    @Override
    DataPoints getDataPoints();

    /**
     * Radius vectors giving, together with the two end points, the plane and the rotation direction
     * of each arc.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The vector at index <var>i</var> lies in the tangent space of the control point at
     *       index <var>i</var>.</li>
     *   <li>The arc runs from its first data point, through the position at that vector from its
     *       centre, to its second data point.</li>
     *   <li>Two consecutive radius vectors sharing the same centre are not collinear; the rotation
     *       direction is seen from above their cross product.</li>
     * </ul>
     *
     * @return radius vectors of the arcs, possibly empty.
     *
     * @see ISO 19107:2019 - 7.9.2.5, 7.9.3
     */
    @UML(identifier="radius", specification=ISO_19107)
    List<Vector> getRadius();

}
