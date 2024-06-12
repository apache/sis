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
package org.apache.sis.filter;

import java.time.Instant;
import java.io.Serializable;
import org.apache.sis.temporal.TemporalUtilities;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.temporal.Period;


/**
 * A literal expression which returns a period computed from {@link #begin} and {@link #end} fields.
 * This is used for testing purpose only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("serial")
final class PeriodLiteral implements Period, Literal<Feature,Period>, Serializable {
    /**
     * Period beginning and ending, in milliseconds since Java epoch.
     */
    public long begin, end;

    /**
     * Constructs a new literal with all values initialized to zero.
     */
    public PeriodLiteral() {
    }

    /**
     * Returns the constant value held by this object.
     */
    @Override
    public Period getValue() {
        return this;
    }

    /**
     * Returns the beginning of this period.
     */
    @Override
    public org.opengis.temporal.Instant getBeginning() {
        return TemporalUtilities.createInstant(Instant.ofEpochMilli(begin));
    }

    /**
     * Returns the ending of this period.
     */
    @Override
    public org.opengis.temporal.Instant getEnding() {
        return TemporalUtilities.createInstant(Instant.ofEpochMilli(end));
    }

    /**
     * Not needed for the tests.
     */
    @Override
    public <N> Expression<Feature,N> toValueType(Class<N> target) {
        throw new UnsupportedOperationException();
    }

    /**
     * Hash code value. Used by the tests for checking the results of deserialization.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(begin) + Long.hashCode(end) * 7;
    }

    /**
     * Compares this period with given object. Used by the tests for checking the results of deserialization.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof PeriodLiteral p) {
            return begin == p.begin && end == p.end;
        }
        return false;
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return "Period[" + Instant.ofEpochMilli(begin) +
                 " ... " + Instant.ofEpochMilli(end) + ']';
    }
}
