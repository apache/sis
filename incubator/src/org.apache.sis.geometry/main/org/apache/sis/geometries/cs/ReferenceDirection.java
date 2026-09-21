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
package org.apache.sis.geometries.cs;

import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;


/**
 * A direction serving as the origin from which a {@linkplain Bearing bearing} is measured.
 *
 * <p>This interface declares no member: it exists to gather the types which can play that role,
 * and a type joins that set by implementing it. The types doing so in this package are:</p>
 * <ul>
 *   <li>{@link FixedDirection}, fixed with respect to the globe, a map or a grid;</li>
 *   <li>{@link RelativeDirection}, relative to a moving object;</li>
 *   <li>{@link CurveRelativeDirection}, relative to a curve at a position on it;</li>
 *   <li>{@link Bearing} itself, which makes the definition recursive: a bearing may be measured
 *       from another bearing. ISO 19107 bounds that recursion by requiring the reference direction
 *       of a bearing to not refer to that bearing transitively.</li>
 * </ul>
 *
 * <p>Difference with ISO 19107: the standard defines this as an empty interface which shall be
 * implemented by any datatype able to represent a direction at a position. It is therefore left
 * open here rather than sealed over the four types above, so that an application may contribute
 * its own. A consequence is that the recursion constraint cannot be enforced by the type system
 * and is checked when a bearing is created instead.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see Bearing#getReference()
 * @see ISO 19107:2019 - 6.2.21, 6.2.22.4
 */
@UML(identifier="ReferenceDirection", specification=ISO_19107)
public interface ReferenceDirection {

}
