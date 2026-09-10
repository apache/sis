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
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.cs.Projection;
import org.apache.sis.measure.Range;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A curve composed of other curves which all share the same parameter space, each of them covering a
 * disjoint projection of the coordinate system.
 *
 * <p>A product curve is therefore both a set of curves in the various projections of the coordinate
 * reference system, and a single curve in the complete reference system. It allows a different
 * interpolation mechanism per group of coordinate offsets: a unit circle in ℝ² can be written as
 * <var>c</var>(<var>t</var>) = (sin <var>t</var>, cos <var>t</var>).</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>All the elements are curves, and each of them is a projection of this curve onto a disjoint
 *       projection of its coordinate system.</li>
 *   <li>Each coordinate offset of the coordinate system of this curve is covered by exactly one
 *       element.</li>
 *   <li>All the elements share the {@linkplain #getKnots() knot} array and the construction
 *       parameters of this curve.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.22
 */
@UML(identifier="ProductCurve", specification=ISO_19107)
public non-sealed interface ProductCurve extends Curve, GeometryCollection<Curve> {

    /**
     * Interval of the construction parameter shared by this curve and its projections,
     * i.e. the interval between the first and the last {@linkplain #getKnots() knot}.
     *
     * @return parameter range of this curve.
     *
     * @see ISO 19107:2019 - 6.4.22.2
     */
    @UML(identifier="parameterRange", specification=ISO_19107)
    Range<?> getParameterRange();

    /**
     * Projections of the coordinate system of this curve, in the same order as the element curves.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>At least one projection, and as many of them as there are element curves.</li>
     *   <li>The projections are disjoint and together cover the whole coordinate system.</li>
     * </ul>
     *
     * <p>Note: this attribute is redundant with the coordinate systems of the element curves, from
     * which the projections can be deduced; providing it is an implementation choice.</p>
     *
     * @return projections matching the element curves.
     *
     * @see ISO 19107:2019 - 6.4.22.3, 6.4.22.4
     */
    @UML(identifier="projection", specification=ISO_19107)
    List<Projection> getProjection();
}
