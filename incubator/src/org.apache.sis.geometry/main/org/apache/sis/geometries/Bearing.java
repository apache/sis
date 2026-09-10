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
package org.apache.sis.geometries;

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A direction at a point, expressed either as a set of angles or as a tangent vector.
 *
 * <p>In the angular form, the first angle is an azimuth measured in the tangent plane from a
 * reference direction, and the second one is an altitude, positive above the horizontal and
 * negative below it. In the vector form, the direction is a unit vector of the coordinate system at
 * the point. Both forms carry the same information; only their interpretation differs, and that
 * interpretation depends on a reference direction giving the zero offset and on a rotation
 * direction.</p>
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>A bearing may be valid only at the point from which it is measured: transporting a vector
 *       to another point is valid only if the geometric reference surface is planar.</li>
 *   <li>A fixed reference direction such as true north allows some transport, but only where that
 *       reference exists and is unique. True north does not exist at the North pole and is not
 *       unique at the South pole.</li>
 *   <li>The reference direction of a bearing shall not refer to that bearing transitively.</li>
 *   <li>The magnitude of the vector has no effect: only its direction matters.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.2.22
 */
@UML(identifier="Bearing", specification=ISO_19107)
public interface Bearing {

}
