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
 * A reference direction relative to a moving object, such as a vehicle.
 *
 * <p>These directions are carried by the object and turn with it: they are meaningful only while
 * the heading of that object is known. A {@linkplain Bearing bearing} measured from one of them is
 * therefore relative, unlike a bearing measured from a {@link FixedDirection}.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.2.24
 */
@UML(identifier="RelativeDirection", specification=ISO_19107)
public enum RelativeDirection implements ReferenceDirection {
    /**
     * Toward the front of the object, in the direction of its movement.
     * Also called <dfn>fore</dfn>.
     */
    FORWARD,

    /**
     * Toward the rear of the object, opposite to the direction of its movement.
     * Also called <dfn>aft</dfn>.
     */
    BACKWARD,

    /**
     * Ninety degrees to the left of {@link #FORWARD}.
     * Also called <dfn>port</dfn>.
     */
    LEFT,

    /**
     * Ninety degrees to the right of {@link #FORWARD}.
     * Also called <dfn>starboard</dfn>.
     */
    RIGHT
}
