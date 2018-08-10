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
package org.apache.sis.internal.jaxb.gco;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.xml.NilReason;


/**
 * A component of a multiplicity, consisting of an non-negative lower bound, and a potentially infinite upper bound.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@XmlType(name = "MultiplicityRange_Type", propOrder = {
    "lower",
    "upper"
})
final class MultiplicityRange {
    /**
     * The lower bound.
     */
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(GO_Integer.class)
    private Integer lower;

    /**
     * The upper bound.
     */
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(GO_UnlimitedInteger.class)
    private UnlimitedInteger upper;

    /**
     * Creates an initially empty range.
     * This is invoked by JAXB at unmarshalling time.
     */
    private MultiplicityRange() {
    }

    /**
     * Creates a range initialized to the given value.
     */
    private MultiplicityRange(final NumberRange<Integer> range) {
        if (range != null) {
            lower = range.getMinValue();
            if (lower == null) {
                lower = NilReason.UNKNOWN.createNilObject(Integer.class);
            } else if (!range.isMinIncluded()) {
                lower = Math.incrementExact(lower);
            }
            upper = new UnlimitedInteger(range.getMaxValue(), range.isMaxIncluded());
        } else {
            lower = 0;
            upper = new UnlimitedInteger();         // Initialized to missing value.
        }
    }

    /**
     * Wraps the given integer range in multiplicity range,
     * or returns {@code null} if the given range is null.
     */
    static MultiplicityRange wrap(final NumberRange<Integer> range) {
        return (range != null) ? new MultiplicityRange(range) : null;
    }

    /**
     * Returns the value as a number range, or {@code null} if none.
     */
    NumberRange<Integer> value() {
        if (lower != null && upper != null) {
            final Integer h = upper.value();
            if (h != null || upper.isInfinite()) {
                return new NumberRange<>(Integer.class, lower, true, h, true);
            }
        }
        return null;
    }

    /**
     * Returns a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return String.valueOf(value());
    }
}
