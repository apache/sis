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
package org.apache.sis.openoffice;

import java.util.Locale;
import java.text.ParseException;
import com.sun.star.uno.AnyConverter;
import com.sun.star.lang.IllegalArgumentException;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.util.collection.Cache;


/**
 * The pattern for parsing and formatting angle values.
 * The expected pattern is as described by {@link AngleFormat} with the following extension:
 *
 * <ul>
 *   <li>If the pattern ends with E or W, then the angle is formatted as a longitude.</li>
 *   <li>If the pattern ends with N or S, then the angle is formatted as a latitude.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class AnglePattern {
    /**
     * Enumeration of possible {@link #type} values.
     */
    private static final byte LATITUDE = 1, LONGITUDE = 2;

    /**
     * A pattern to be given to {@link AngleFormat} constructor.
     */
    private String pattern;

    /**
     * {@value #LATITUDE} for latitude, {@value #LONGITUDE} for longitude, or 0 otherwise.
     */
    private byte type;

    /**
     * Converts the given argument to a pattern valid for {@link AngleFormat}.
     *
     * @param  patternOrVoid the optional pattern argument from the OpenOffice formula.
     * @throws IllegalArgumentException if {@code patternOrVoid} is not a string value or void.
     */
    AnglePattern(final Object patternOrVoid) throws IllegalArgumentException {
        if (AnyConverter.isVoid(patternOrVoid)) {
            pattern = "D°MM'SS.s\"";
        } else {
            pattern = AnyConverter.toString(patternOrVoid);
            final int lc = pattern.length() - 1;
            if (lc > 0) {
                final char c = pattern.charAt(lc);
                switch (c) {
                    case 'N': case 'n': case 'S': case 's': type = LATITUDE;  break;
                    case 'E': case 'e': case 'W': case 'w': type = LONGITUDE; break;
                    default: return;
                }
                pattern = pattern.substring(0, lc);
            }
        }
    }

    /**
     * Returns the angle format to use for this pattern. The formatter is cached on the assumption
     * that the same pattern will be used for formatting more than once.
     *
     * @param  locale the locale.
     * @return the angle format for this pattern and the given locale.
     */
    private AngleFormat getAngleFormat(final Locale locale) {
        final CacheKey<AngleFormat> key = new CacheKey<>(AngleFormat.class, pattern, locale, null);
        AngleFormat format = key.peek();
        if (format == null) {
            final Cache.Handler<AngleFormat> handler = key.lock();
            try {
                format = handler.peek();
                if (format == null) {
                    format = new AngleFormat(pattern, locale);
                }
            } finally {
                handler.putAndUnlock(format);
            }
        }
        return format;
    }

    /**
     * Parses the given angle.
     *
     * @param text    the angle to parse.
     * @param locale  the expected locale of the text to parse.
     */
    double parse(final String text, final Locale locale) throws ParseException {
        AngleFormat format = getAngleFormat(locale);
        Angle angle;
        try {
            synchronized (format) {
                angle = format.parse(text);
            }
        } catch (ParseException exception) {
            // Parse failed. Try to parse as an unlocalized string.
            format = getAngleFormat(Locale.ROOT);
            try {
                synchronized (format) {
                    angle = format.parse(text);
                }
            } catch (ParseException ignore) {
                throw exception;
            }
        }
        return angle.degrees();
    }

    /**
     * Formats the given angle.
     *
     * @param value   the value to format.
     * @param locale  the target locale.
     */
    String format(final double value, final Locale locale) {
        final AngleFormat format = getAngleFormat(locale);
        final Angle angle;
        switch (type) {
            default:        angle = new Angle    (value); break;
            case LATITUDE:  angle = new Latitude (value); break;
            case LONGITUDE: angle = new Longitude(value); break;
        }
        synchronized (format) {
            return format.format(angle);
        }
    }
}
