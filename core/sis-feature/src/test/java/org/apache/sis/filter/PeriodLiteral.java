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
import org.opengis.metadata.Identifier;
import org.apache.sis.test.TestUtilities;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.temporal.Period;
import org.opengis.temporal.Duration;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalPosition;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.temporal.TemporalGeometricPrimitive;


/**
 * A literal expression which returns a period computed from {@link #begin} and {@link #end} fields.
 * This is used for testing purpose only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@SuppressWarnings("serial")
final strictfp class PeriodLiteral implements Period, Literal<Feature,Period>, Serializable {
    /**
     * Period beginning and ending, in milliseconds since Java epoch.
     */
    public long begin, end;

    /**
     * Returns the constant value held by this object.
     */
    @Override public Period getValue() {return this;}

    /** Returns a bound of this period. */
    @Override public org.opengis.temporal.Instant getBeginning() {return instant(begin);}
    @Override public org.opengis.temporal.Instant getEnding()    {return instant(end);}

    /** Wraps the value that defines a period. */
    private static org.opengis.temporal.Instant instant(final long t) {
        return new org.opengis.temporal.Instant() {
            @Override public Date   getDate()  {return new Date(t);}
            @Override public String toString() {return "Instant[" + TestUtilities.format(getDate()) + '[';}

            /** Not needed for the tests. */
            @Override public Identifier       getName()                              {throw new UnsupportedOperationException();}
            @Override public TemporalPosition getTemporalPosition()                  {throw new UnsupportedOperationException();}
            @Override public RelativePosition relativePosition(TemporalPrimitive o)  {throw new UnsupportedOperationException();}
            @Override public Duration         distance(TemporalGeometricPrimitive o) {throw new UnsupportedOperationException();}
            @Override public Duration         length()                               {throw new UnsupportedOperationException();}
        };
    }

    /** Not needed for the tests. */
    @Override public Identifier       getName()                              {throw new UnsupportedOperationException();}
    @Override public RelativePosition relativePosition(TemporalPrimitive o)  {throw new UnsupportedOperationException();}
    @Override public Duration         distance(TemporalGeometricPrimitive o) {throw new UnsupportedOperationException();}
    @Override public Duration         length()                               {throw new UnsupportedOperationException();}
    @Override public <N> Expression<Feature,N> toValueType(Class<N> target)  {throw new UnsupportedOperationException();}

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
