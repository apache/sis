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
package org.apache.sis.storage.coveragejson.binding;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.util.Objects;

/**
 * Time is referenced by a temporal reference system (temporal RS). In the current
 * version of this Community Standard, only a string-based notation for time
 * values is defined. Future versions of this Community Standard may allow for
 * alternative notations, such as recording time values as numeric offsets from
 * a given temporal datum (e.g. “days since 1970-01-01”).
 *
 * If the calendar is based on years, months, days, then the referenced values
 * SHOULD use one of the following ISO8601-based lexical representations:
 * YYYY
 * ±XYYYY (where X stands for extra year digits)
 * YYYY-MM
 * YYYY-MM-DD
 * YYYY-MM-DDTHH:MM:SS[.F]Z where Z is either “Z” or a time scale offset +|-HH:MM
 *
 * If calendar dates with reduced precision are used in a lexical representation
 * (e.g. "2016"), then a client SHOULD interpret those dates in that reduced precision.
 *
 * If "type" is "TemporalRS" and "calendar" is "Gregorian", then the above lexical
 * representation MUST be used.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonbNillable(false)
@JsonbPropertyOrder({"type","calendar","timeScale"})
public final class TemporalRS extends CoverageJsonObject {

    /**
     * A temporal RS object MUST have a member "calendar" with value "Gregorian" or a URI.
     * If the Gregorian calendar is used, then "calendar" MUST have the value "Gregorian" and cannot be a URI.
     */
    public String calendar;
    /**
     * A temporal RS object MAY have a member "timeScale" with a URI as value.
     * If omitted, the time scale defaults to "UTC". If the time scale is UTC,
     * the "timeScale" member MUST be omitted.
     */
    public String timeScale;

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof TemporalRS)) return false;

        final TemporalRS cdt = ((TemporalRS) other);
        return super.equals(other)
            && Objects.equals(calendar, cdt.calendar)
            && Objects.equals(timeScale, cdt.timeScale);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(
                calendar,
                timeScale);
    }
}
