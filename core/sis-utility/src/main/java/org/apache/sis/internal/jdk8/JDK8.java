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
import javax.xml.bind.DatatypeConverter;
import org.apache.sis.util.CharSequences;


/**
 * Place holder for some functionalities defined only in JDK8.
 * This file will be deleted on the SIS JDK8 branch.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class JDK8 {
    /**
     * Do not allow instantiation of this class.
     */
    private JDK8() {
    }

    /**
     * Parses a date from a string in ISO 8601 format. More specifically, this method expects the
     * format defined by <cite>XML Schema Part 2: Datatypes for {@code xsd:dateTime}</cite>, with
     * some additional flexibility (e.g. missing minutes or seconds fields are automatically added).
     *
     * <p>This method will be replaced by {@link java.time.format.DateTimeFormatter} on the JDK8 branch.</p>
     *
     * @param  date The date to parse, or {@code null}.
     * @param  defaultToUTC {@code true} if the default timezone shall be UTL, or {@code false} for locale default.
     * @return The parsed date, or {@code null} if the given string was {@code null}.
     * @throws IllegalArgumentException if the given date can not be parsed.
     *
     * @see DatatypeConverter#parseDateTime(String)
     */
    public static Date parseDateTime(String date, final boolean defaultToUTC) throws IllegalArgumentException {
        if (date != null) {
            date = CharSequences.trimWhitespaces(date);
            if (!date.isEmpty()) {
                /*
                 * Check for missing time fields and time zone. For example if the given date is
                 * "2005-09-22T00:00", then this block will complete it as "2005-09-22T00:00:00".
                 * In addition, a 'Z' suffix will be appended if 'defaultToUTC' is true.
                 */
                int timeFieldStart  = date.lastIndexOf('T') + 1; // 0 if there is no time field.
                int timeFieldEnd    = date.length();             // To be updated if there is a time field.
                int missingFields   = 0;                         // Number of missing time fields.
                boolean hasTimeZone = date.charAt(timeFieldEnd - 1) == 'Z';
                if (timeFieldStart != 0) {
                    if (hasTimeZone) {
                        timeFieldEnd--;
                    } else {
                        final int s = Math.max(date.indexOf('+', timeFieldStart),
                                               date.indexOf('-', timeFieldStart));
                        hasTimeZone = (s >= 0);
                        if (hasTimeZone) {
                            timeFieldEnd = s;
                        }
                    }
                    missingFields = 2;
                    for (int i=timeFieldStart; i<timeFieldEnd; i++) {
                        if (date.charAt(i) == ':') {
                            if (--missingFields == 0) break;
                        }
                    }
                }
                /*
                 * At this point, we have determined if there is some missing fields.
                 * The timezone will be considered missing only if 'defaultToUTC' is true.
                 */
                CharSequence modified = date;
                hasTimeZone |= !defaultToUTC;
                if (missingFields != 0 || !hasTimeZone) {
                    final StringBuilder buffer = new StringBuilder(date);
                    while (--missingFields >= 0) {
                        buffer.append(":00");
                    }
                    if (!hasTimeZone) {
                        buffer.append('Z');
                    }
                    modified = buffer;
                }
                /*
                 * Now ensure that all numbers have at least two digits.
                 */
                int indexOfLastDigit = 0;
                for (int i=modified.length(); --i >= 0;) {
                    char c = modified.charAt(i);
                    final boolean isDigit = (c >= '0' && c <= '9'); // Do not use Character.isDigit(char).
                    if (indexOfLastDigit == 0) {
                        // We were not scaning a number. Check if we are now starting doing so.
                        if (isDigit) {
                            indexOfLastDigit = i;
                        }
                    } else {
                        // We were scaning a number. Check if we found the begining.
                        if (!isDigit) {
                            if (indexOfLastDigit - i == 1) {
                                // Reuse the buffer if it exists, or create a new one otherwise.
                                final StringBuilder buffer;
                                if (modified == date) {
                                    modified = buffer = new StringBuilder(date);
                                } else {
                                    buffer = (StringBuilder) modified;
                                }
                                buffer.insert(i+1, '0');
                            }
                            indexOfLastDigit = 0;
                        }
                    }
                }
                /*
                 * Now delegate to the utility method provided in the JAXB packages.
                 */
                return DatatypeConverter.parseDateTime(modified.toString()).getTime();
            }
        }
        return null;
    }
}
