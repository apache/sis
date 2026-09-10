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
 * A polynomial spline of degree 3, made of a sequence of segments each defined by its own cubic
 * polynomials, one per coordinate offset.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>The {@linkplain #getDegree() degree} is 3.</li>
 *   <li>The curve is C², i.e. its first and second derivatives are continuous everywhere, and it
 *       passes through its data points in the given order.</li>
 *   <li>The {@linkplain #getDerivativeAtStart() start} and
 *       {@linkplain #getDerivativeAtEnd() end} derivatives reduce to a single tangent vector each.</li>
 *   <li>Unlike a polyline, the arc length parameterization of a cubic spline is not necessarily
 *       polynomial.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.13.6
 */
@UML(identifier="CubicSpline", specification=ISO_19107)
public non-sealed interface CubicSpline extends PolynomialSpline {

}
