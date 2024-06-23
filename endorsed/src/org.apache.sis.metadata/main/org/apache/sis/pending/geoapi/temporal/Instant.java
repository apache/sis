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
package org.apache.sis.pending.geoapi.temporal;

import java.util.Optional;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import org.opengis.temporal.TemporalPrimitive;


/**
 * Placeholder for a GeoAPI interfaces not present in GeoAPI 3.0.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   1.5
 * @version 1.5
 */
public interface Instant extends TemporalPrimitive {
    /**
     * Returns the date, time or position on the time-scale represented by this primitive.
     */
    Temporal getPosition();

    /**
     * Returns the reason why the temporal position is missing or inaccurate.
     *
     * @return the reason why the position is indeterminate.
     */
    default Optional<IndeterminateValue> getIndeterminatePosition() {
        return Optional.empty();
    }

    /**
     * Returns the distance from this instant to another instant or a period (optional operation).
     */
    default TemporalAmount distance(TemporalPrimitive other) {
        throw new UnsupportedOperationException();
    }

    default TemporalOperatorName findRelativePosition(TemporalPrimitive other) {
        throw new UnsupportedOperationException();
    }
}
