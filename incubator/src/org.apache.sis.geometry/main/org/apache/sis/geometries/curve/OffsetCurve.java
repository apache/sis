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

import javax.measure.quantity.Length;
import org.apache.sis.geometries.Bearing;
import org.apache.sis.geometries.Curve;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A curve at a predictable distance and bearing from a base curve.
 *
 * <p>It is useful for curves which are offsets by definition: if the centreline of a highway is
 * stored, offsetting it by a constant distance gives the left and the right side of the road. If the
 * {@linkplain #getRefDirection() reference direction} is absolute, the offset is the geographic
 * equivalent of a parallel translation; if it is relative, the base direction is the tangent to the
 * base curve.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>Every position of the offset curve is at exactly the given distance from the base curve:
 *       the small loops produced by the usual buffer algorithm are removed.</li>
 *   <li>All the non-positional attributes, such as the coordinate system, are the same as those of
 *       the {@linkplain #getBaseCurve() base curve}.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.4.20
 */
@UML(identifier="OffsetCurve", specification=ISO_19107)
public non-sealed interface OffsetCurve extends Curve {

    /**
     * Distance at which this curve is generated from the {@linkplain #getBaseCurve() base curve}.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>The distance is constant along the base curve.</li>
     *   <li>In a 2-dimensional coordinate system, a positive distance designates the left side of
     *       the base curve and a negative distance its right side, with respect to the tangent.</li>
     * </ul>
     *
     * <p>Note: extensions may replace this constant by a function of the position along the base
     * curve, as in linear referencing (ISO 19148) or moving features (ISO 19141).</p>
     *
     * @return offset distance from the base curve.
     *
     * @see ISO 19107:2019 - 6.4.20.2
     */
    @UML(identifier="distance", specification=ISO_19107)
    Length getDistance();

    /**
     * Direction in which this curve is offset from the {@linkplain #getBaseCurve() base curve},
     * or {@code null} if the spatial dimension is 2.
     *
     * <p>Constraints:</p>
     * <ul>
     *   <li>In a 3-dimensional coordinate system, the offset direction at a position of the base
     *       curve is the cross product of this reference direction with the tangent of the base
     *       curve at that position.</li>
     *   <li>This direction shall at no position of the base curve be parallel or antiparallel to
     *       that tangent, which generally means that the base curve is nowhere purely vertical.</li>
     *   <li>The base curve shall have a well-defined tangent at every position; where it is not
     *       differentiable, a smoothly varying approximation may be used.</li>
     *   <li>This direction is not necessarily constant along the base curve.</li>
     * </ul>
     *
     * @return reference direction of the offset, or {@code null} if unspecified.
     *
     * @see ISO 19107:2019 - 6.4.20.3
     */
    @UML(identifier="refDirection", specification=ISO_19107)
    Bearing getRefDirection();

    /**
     * Curve from which this curve is defined as an offset.
     *
     * @return base curve of this offset curve.
     *
     * @see ISO 19107:2019 - 6.4.20.4
     */
    @UML(identifier="baseCurve", specification=ISO_19107)
    Curve getBaseCurve();
}
