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
package org.apache.sis.internal.util;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;


/**
 * A calendar that simulates the ISO 8601 rules used by {@code java.time} packages.
 * This is used for compatibility on the JDK7 branch only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class ISOCalendar extends GregorianCalendar {
    /**
     * For cross-version compatibility.
     * This number must be different between the JDK8 branch and pre-JDK8 branches.
     */
    private static final long serialVersionUID = 9109360831315522569L;

    /**
     * Creates a new calendar.
     */
    ISOCalendar(final Locale locale, final TimeZone zone) {
        super(zone, locale);
        setGregorianChange(new Date(Long.MIN_VALUE));       // Set pure Gregorian calendar (no change date).
    }

    /**
     * Returns the value of the given calendar field.
     */
    @Override
    public int get(final int field) {
        int value = super.get(field);
        if (field == YEAR && super.get(ERA) == BC) {
            value = 1 - value;
        }
        return value;
    }
}
