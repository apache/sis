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

import java.util.Date;
import java.io.Serializable;
import org.apache.sis.test.TestUtilities;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.internal.geoapi.filter.Literal;
import org.apache.sis.internal.geoapi.temporal.Period;


/**
 * A literal expression which returns a period computed from {@link #begin} and {@link #end} fields.
 * This is used for testing purpose only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
@SuppressWarnings("serial")
final class PeriodLiteral implements Period, Literal<AbstractFeature,Period>, Serializable {
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
    @Override public Period getValue() {return this;}
    @Override public Period apply(AbstractFeature input) {return this;}

    /** Returns a bound of this period. */
    @Override public org.apache.sis.internal.geoapi.temporal.Instant getBeginning() {return instant(begin);}
    @Override public org.apache.sis.internal.geoapi.temporal.Instant getEnding()    {return instant(end);}

    /** Wraps the value that defines a period. */
    private static org.apache.sis.internal.geoapi.temporal.Instant instant(final long t) {
        return new org.apache.sis.internal.geoapi.temporal.Instant() {
            @Override public Date   getDate()  {return new Date(t);}
            @Override public String toString() {return "Instant[" + TestUtilities.format(getDate()) + '[';}
        };
    }

    /** Not needed for the tests. */
    @Override public <N> Expression<AbstractFeature,N> toValueType(Class<N> target) {throw new UnsupportedOperationException();}
    @Override public Class<AbstractFeature> getResourceClass() {return AbstractFeature.class;}

    /**
     * Hash code value. Used by the tests for checking the results of deserialization.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(begin) + Long.hashCode(end) * 7;
    }

    /**
     * Compare this period with given object. Used by the tests for checking the results of deserialization.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof PeriodLiteral) {
            final PeriodLiteral p = (PeriodLiteral) other;
            return begin == p.begin && end == p.end;
        }
        return false;
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return "Period[" + TestUtilities.format(new Date(begin)) +
                 " ... " + TestUtilities.format(new Date(end)) + ']';
    }
}
