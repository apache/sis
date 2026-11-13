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
package org.apache.sis.storage.geojson.binding;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Objects;
import org.apache.sis.temporal.TemporalObjects;
import org.opengis.temporal.TemporalPrimitive;

/**
 * An instant or an interval, as defined by the {@code JSONMDInstantOrInterval} JSON type.
 *
 * <p>An instant is written as a single date, an interval as an array of two dates where {@code ".."} is an open bound.
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.5"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#JSONMDInstantOrInterval</a>
 */
public final class JSONMDInstantOrInterval {

    /**
     * Value of an open interval bound.
     */
    public static final String OPEN = "..";

    private final JSONMDDateOrDateTime begin;
    private final JSONMDDateOrDateTime end;
    private final boolean interval;

    /**
     * Creates an instant.
     */
    public JSONMDInstantOrInterval(final JSONMDDateOrDateTime instant) {
        this.begin    = Objects.requireNonNull(instant);
        this.end      = null;
        this.interval = false;
    }

    /**
     * Creates an interval.
     *
     * @param begin start of the interval, or {@code null} if open.
     * @param end   end of the interval, or {@code null} if open.
     */
    public JSONMDInstantOrInterval(final JSONMDDateOrDateTime begin, final JSONMDDateOrDateTime end) {
        this.begin    = begin;
        this.end      = end;
        this.interval = true;
    }

    /**
     * Parses an instant (JSON string) or an interval (JSON array of two strings).
     *
     * @throws IllegalArgumentException if the JSON value has another form.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public JSONMDInstantOrInterval(final JsonNode node) {
        if (node.isTextual()) {
            begin    = new JSONMDDateOrDateTime(node.asText());
            end      = null;
            interval = false;
        } else if (node.isArray() && node.size() == 2) {
            begin    = bound(node.get(0));
            end      = bound(node.get(1));
            interval = true;
        } else {
            throw new IllegalArgumentException("Expected a date or an array of two dates, found: " + node);
        }
    }

    private static JSONMDDateOrDateTime bound(final JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        final String text = node.asText();
        return OPEN.equals(text) ? null : new JSONMDDateOrDateTime(text);
    }

    public boolean isInterval() {
        return interval;
    }

    /**
     * Returns the instant, or the start of the interval ({@code null} if open).
     */
    public JSONMDDateOrDateTime getBegin() {
        return begin;
    }

    /**
     * Returns the end of the interval ({@code null} if open or if this is an instant).
     */
    public JSONMDDateOrDateTime getEnd() {
        return end;
    }

    /**
     * Returns the JSON value: a string for an instant, an array of two strings for an interval.
     */
    @JsonValue
    public Object toJSON() {
        if (!interval) {
            return begin.toString();
        }
        return Arrays.asList(
                (begin == null) ? OPEN : begin.toString(),
                (end   == null) ? OPEN : end.toString());
    }

    /**
     * Returns this instant or interval as an ISO 19108 temporal primitive.
     *
     * <p>Returns {@code null} for an interval with two open bounds.
     */
    public TemporalPrimitive toTemporal() {
        if (!interval) {
            return TemporalObjects.createInstant(begin.getValue());
        }
        final Temporal start = (begin == null) ? null : begin.getValue();
        final Temporal stop  = (end   == null) ? null : end.getValue();
        return TemporalObjects.createPeriod(start, stop);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof JSONMDInstantOrInterval other && interval == other.interval
                && Objects.equals(begin, other.begin) && Objects.equals(end, other.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(begin, end, interval);
    }

    @Override
    public String toString() {
        return String.valueOf(toJSON());
    }
}
