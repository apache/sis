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
package org.apache.sis.referencing.cs;

import org.opengis.referencing.cs.AxisDirection;
import javax.measure.unit.Unit;


/**
 * Modifications to apply on the axes of a coordinate system in order to produce a new coordinate system.
 * Possible modifications include changes of axis unit direction.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public interface AxisFilter {
    /**
     * Returns a replacement for the given axis direction.
     *
     * @param  direction The original axis direction.
     * @return The new axis direction, or {@code direction} if there is no change.
     */
    default AxisDirection getDirectionReplacement(AxisDirection direction) {
        return direction;
    }

    /**
     * Returns a replacement for the given axis unit.
     *
     * @param  unit The original axis unit.
     * @return The new axis unit, or {@code unit} if there is no change.
     */
    default Unit<?> getUnitReplacement(Unit<?> unit) {
        return unit;
    }
}
