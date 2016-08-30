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
package org.apache.sis.internal.jdk8;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.apache.sis.internal.util.StandardDateFormat;


/**
 * Placeholder for the {@link java.time.OffsetTime} class.
 */
public final class OffsetTime extends Temporal {
    /**
     * A shared object for parsing and formatting dates.
     */
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat(StandardDateFormat.TIME_PATTERN, Locale.US);
    static {
        FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Creates a new date.
     *
     * @param millis  number of milliseconds since January 1st, 1970 midnight UTC.
     */
    private OffsetTime(final long millis) {
        super(millis);
    }

    /**
     * Parses a text string like {@code 10:15:30+01:00}.
     *
     * @param  text  the text to parse.
     * @return the parsed date.
     * @throws DateTimeParseException if the text cannot be parsed.
     */
    public static OffsetTime parse(String text) {
        text = StandardDateFormat.fix(text, true);
        final Date date;
        try {
            synchronized (FORMAT) {
                date = FORMAT.parse(text);
            }
        } catch (ParseException e) {
            throw new DateTimeParseException(e.getLocalizedMessage());
        }
        return new OffsetTime(date.getTime());
    }
}
