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
package org.apache.sis.internal.simple;

import org.apache.sis.internal.util.Numerics;
import org.opengis.temporal.Duration;


/**
 * A temporary implementation of {@link Duration}.
 * Will probably be deleted in some future version.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class SimpleDuration implements Duration {
    /**
     * Duration in days.
     */
    public final double duration;

    /**
     * Creates a new duration.
     *
     * @param  duration  the duration in days.
     */
    public SimpleDuration(final double duration) {
        this.duration = duration;
    }

    /**
     * Returns a string representation of this duration.
     *
     * @return the duration with its unit of measurement.
     */
    @Override
    public String  toString() {
        return duration + " days";
    }

    /**
     * Returns a hash code value for this duration.
     */
    @Override
    public int hashCode() {
        return Double.hashCode(duration) ^ 37;
    }

    /**
     * Compares this duration with the given object for equality.
     *
     * @param  other  the object to compare with this duration.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof SimpleDuration) {
            return Numerics.equals(duration, ((SimpleDuration) other).duration);
        }
        return false;
    }
}
