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
import java.text.ParseException;
import org.apache.sis.internal.util.StandardDateFormat;


/**
 * Placeholder for the {@link java.time.Instant} class.
 */
public final class Instant extends Temporal {
    /**
     * The parser to use for the {@link #parse(CharSequence)} method.
     */
    private static final StandardDateFormat parser = new StandardDateFormat();

    /**
     * Creates a new instant.
     *
     * @param  millis  number of milliseconds since January 1st, 1970 midnight UTC.
     */
    private Instant(final long millis) {
        super(millis);
    }

    /**
     * Not a JDK method - used as a replacement of {@code Date.toInstant()}.
     *
     * @param  date  the date to convert, or {@code null}.
     * @return the date as an instant, or {@code null}.
     *
     * @see #toDate()
     */
    public static Instant create(final Date date) {
        return (date != null) ? new Instant(date.getTime()) : null;
    }

    /**
     * Parses the given text.
     *
     * @param  text  the text to parse.
     * @return the instant.
     * @throws DateTimeException if the text can not be parsed.
     */
    public static Instant parse(final CharSequence text) {
        final Date time;
        try {
            synchronized (parser) {
                time = parser.parse(text.toString());
            }
        } catch (ParseException e) {
            throw new DateTimeException(e.getMessage());
        }
        return new Instant(time.getTime());
    }

    /**
     * Creates a new instant for the given time in milliseconds.
     *
     * @param  millis  number of milliseconds since January 1st, 1970 midnight UTC.
     * @return the instant for the given time.
     */
    public static Instant ofEpochMilli(final long millis) {
        return new Instant(millis);
    }

    /**
     * Returns the number of milliseconds since January 1st, 1970 midnight UTC.
     *
     * @return number of milliseconds since January 1st, 1970 midnight UTC.
     */
    public long toEpochMilli() {
        return millis;
    }

    /**
     * Not a JDK method - used as a replacement of {@code Date.from(Instant)}.
     *
     * @return this instant as a legacy date object.
     *
     * @see #create(Date)
     */
    public Date toDate() {
        return new Date(millis);
    }

    /**
     * Returns a new instant with the given amount of nanoseconds added.
     *
     * @param   n  amount of nanoseconds to add to this instant.
     * @return  an instant for a point in time after the given amount of nanoseconds.
     */
    public Instant plusNanos(final long n) {
        return (n == 0) ? this : new Instant(JDK8.addExact(millis, n/1000000));
    }

    /**
     * Returns this instant in XML format.
     *
     * @return string representation of this instant.
     */
    @Override
    public String toString() {
        return JDK8.printDateTime(toDate());
    }
}
