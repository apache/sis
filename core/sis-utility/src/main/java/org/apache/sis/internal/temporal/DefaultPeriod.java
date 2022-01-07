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
package org.apache.sis.internal.temporal;

import java.util.Objects;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;


/**
 * Default implementation of GeoAPI period. This is a temporary class;
 * GeoAPI temporal interfaces are expected to change a lot in a future revision.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class DefaultPeriod extends Primitive implements Period {
    /** Bounds making this period. */
    private final Instant beginning, ending;

    /** Creates a new period with the given bounds. */
    DefaultPeriod(final Instant beginning, final Instant ending) {
        this.beginning = beginning;
        this.ending    = ending;
    }

    /** The beginning instant at which this period starts. */
    @Override public Instant getBeginning() {
        return beginning;
    }

    /** The ending instant at which this period ends. */
    @Override public Instant getEnding() {
        return ending;
    }

    /** String representation. */
    @Override public String toString() {
        return "[" + beginning + " … " + ending + ']';
    }

    /** Hash code value of the time position. */
    @Override public int hashCode() {
        return Objects.hash(beginning, ending);
    }

    /** Compares with given object for equality. */
    @Override public boolean equals(final Object obj) {
        if (obj instanceof DefaultPeriod) {
            DefaultPeriod other = (DefaultPeriod) obj;
            return Objects.equals(other.beginning, beginning) &&
                   Objects.equals(other.ending, ending);
        }
        return false;
    }
}
