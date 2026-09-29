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
 * The sense in which an angular measure increases.
 *
 * <p>The two senses are named as seen from the positive side of the normal to the surface on which
 * the angle is measured, that is, looking down on that surface from above. This is the convention
 * of a compass laid flat on the ground and read from above.</p>
 *
 * <p>A rotation is not a direction: it says how an angle grows, not where it starts. The origin of
 * the measure is given by a {@link ReferenceDirection} instead.</p>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see Bearing#getRotation()
 * @see ISO 19107:2019 - 6.2.23
 */
@UML(identifier="Rotation", specification=ISO_19107)
public enum Rotation {
    /**
     * Angles increase in the direction followed by the hands of a clock.
     * This is the usual sense of a compass azimuth, which grows from north toward east.
     */
    CLOCKWISE,

    /**
     * Angles increase in the direction opposite to the hands of a clock.
     * This is the usual sense of trigonometry, which grows from the first axis toward the second.
     */
    COUNTER_CLOCKWISE
}
