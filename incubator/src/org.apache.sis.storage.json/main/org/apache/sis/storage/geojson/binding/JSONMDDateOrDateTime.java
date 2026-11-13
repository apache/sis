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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Objects;

/**
 * A date, a year-month, a year or a date-time, as defined by the {@code JSONMDDateOrDateTime} JSON type.
 *
 * <p>Constraints:
 * <ul>
 *   <li>supported temporal types are {@code Instant}, {@code OffsetDateTime}, {@code ZonedDateTime}, {@code LocalDateTime},
 *       {@code LocalDate}, {@code YearMonth} and {@code Year},</li>
 *   <li>a date-time with an offset is parsed as an {@code OffsetDateTime},</li>
 *   <li>a {@code LocalDateTime} is written without offset, which does not strictly follow the RFC 3339 {@code date-time} format.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 5.1.3 d)"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#JSONMDDateOrDateTime</a>
 */
public final class JSONMDDateOrDateTime {

    private final Temporal value;

    /**
     * Wraps the given temporal value.
     *
     * @throws IllegalArgumentException if the temporal type is not supported.
     */
    public JSONMDDateOrDateTime(final Temporal value) {
        Objects.requireNonNull(value);
        if (!(value instanceof Instant || value instanceof OffsetDateTime || value instanceof ZonedDateTime
                || value instanceof LocalDateTime || value instanceof LocalDate
                || value instanceof YearMonth || value instanceof Year)) {
            throw new IllegalArgumentException("Unsupported temporal type: " + value.getClass().getName());
        }
        this.value = value;
    }

    /**
     * Parses a date, year-month, year or date-time.
     *
     * @throws DateTimeParseException if the text cannot be parsed.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public JSONMDDateOrDateTime(final String text) {
        final String trimmed = text.trim();
        if (trimmed.indexOf('T') >= 0) {
            Temporal parsed;
            try {
                parsed = OffsetDateTime.parse(trimmed);
            } catch (DateTimeParseException e) {
                parsed = LocalDateTime.parse(trimmed);
            }
            value = parsed;
        } else {
            switch (trimmed.length()) {
                case 4  : value = Year.parse(trimmed); break;
                case 7  : value = YearMonth.parse(trimmed); break;
                default : value = LocalDate.parse(trimmed); break;
            }
        }
    }

    public Temporal getValue() {
        return value;
    }

    /**
     * Returns the ISO 8601 representation used in JSON.
     */
    @JsonValue
    @Override
    public String toString() {
        if (value instanceof Instant instant) {
            return DateTimeFormatter.ISO_INSTANT.format(instant);
        } else if (value instanceof OffsetDateTime date) {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(date);
        } else if (value instanceof ZonedDateTime date) {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(date.toOffsetDateTime());
        } else if (value instanceof LocalDateTime date) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(date);
        } else if (value instanceof LocalDate date) {
            return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
        } else {
            return value.toString();
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof JSONMDDateOrDateTime other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
