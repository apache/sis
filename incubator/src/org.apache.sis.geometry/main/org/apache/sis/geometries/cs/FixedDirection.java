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
 * A reference direction fixed with respect to the globe, a map, a coordinate system or a grid.
 *
 * <p>Unlike a {@link RelativeDirection}, these directions do not depend on a moving object, which
 * makes a {@linkplain Bearing bearing} measured from one of them transportable from one position to
 * another. That transport holds only where the direction exists and is unique: true north, for
 * example, does not exist at the North pole and is not unique at the South pole.</p>
 *
 * <p>Difference with ISO 19107: the standard declares this as a code list, whose values may be
 * extended by an application.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see ISO 19107:2019 - 6.2.25
 */
@UML(identifier="FixedDirection", specification=ISO_19107)
public enum FixedDirection implements ReferenceDirection {
    /**
     * Toward the geographic North pole, along the meridian of the position.
     * This is the direction from which an azimuth is measured by default.
     */
    TRUE_NORTH,

    /**
     * Toward the magnetic North pole, as indicated by a compass needle.
     * It differs from {@link #TRUE_NORTH} by the magnetic declination at the position.
     */
    MAGNETIC_NORTH,

    /**
     * Toward the north of a grid, that is along the second axis of a projected coordinate system.
     * It differs from {@link #TRUE_NORTH} by the meridian convergence at the position.
     */
    GRID_NORTH,

    /**
     * Opposite to {@link #TRUE_NORTH}.
     */
    TRUE_SOUTH,

    /**
     * Opposite to {@link #MAGNETIC_NORTH}.
     */
    MAGNETIC_SOUTH,

    /**
     * Opposite to {@link #GRID_NORTH}.
     */
    GRID_SOUTH
}
