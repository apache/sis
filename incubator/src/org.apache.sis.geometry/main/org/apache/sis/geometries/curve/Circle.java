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
 * A complete circle, i.e. an {@link Arc} whose arcs share a single centre and close on themselves.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>All the {@linkplain #getControlPoints() control points} are the same position,
 *       the centre of the circle.</li>
 *   <li>Because a single arc must stay below a full turn, at least two arcs and therefore two
 *       control points are needed.</li>
 *   <li>All the {@linkplain #getDataPoints() data points} are at the same distance from the centre,
 *       and the first and last ones are equal.</li>
 *   <li>By default the first data point, the centre and the second data point lie on a common
 *       geodesic diameter; the curve turns from the first toward the second by the shorter of the
 *       two arcs, then closes by the longer one.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 7.9.4
 */
@UML(identifier="Circle", specification=ISO_19107)
public non-sealed interface Circle extends Arc {

}
