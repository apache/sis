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
package org.apache.sis.storage.gps;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;


/**
 * Type of GPS fix (position derived from measuring external reference points).
 * The <cite>Standard Positioning Service</cite> (SPS) can be two- or three-dimensional,
 * or use differential GPS for increased accuracy.
 * The <cite>Precise Positioning Service</cite> (PPS) is a military signal.
 *
 * <p>This enumeration value can be encoded in <a href="https://en.wikipedia.org/wiki/GPS_Exchange_Format">GPS
 * Exchange Format</a> (GPX) with the following strings: {@code "none"}, {@code "2d"}, {@code "3d"}, {@code "dgps"}
 * and {@code "pps"}.
 * When reading such data, {@code Fix} instances can be a
 * {@linkplain org.apache.sis.feature.DefaultFeatureType#getProperty property value}
 * of the features returned by the GPX reader.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public enum Fix {
    /**
     * GPS has no fix.
     * Note that this is a different meaning than "the fix information is unknown".
     */
    NONE("none"),

    /**
     * Two-dimensional fix. This requires the signal of at least 3 satellites.
     */
    TWO_DIMENSIONAL("2d"),

    /**
     * Three-dimensional fix. This requires the signal of at least 4 satellites.
     */
    THREE_DIMENSIONAL("3d"),

    /**
     * Differential Global Positioning Service (DGPS) used.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Differential_GPS">Differential GPS on Wikipedia</a>
     */
    DIFFERENTIAL("dgps"),

    /**
     * Precise Positioning Service (PPS) used. This is a military signal.
     * (Note: the alternative is <cite>Standard Positioning Service</cite> – SPS).
     */
    PRECISE("pps");

    /**
     * The string representation in GPX format.
     */
    private final String gpx;

    /**
     * Creates a new fix of the given code.
     */
    private Fix(final String gpx) {
        this.gpx = gpx;
    }

    /**
     * The GPX names associated to the enumeration values.
     */
    private static final Map<String,Fix> VALUES = new HashMap<>(6);
    static {
        for (final Fix fix : values()) {
            VALUES.put(fix.gpx, fix);
        }
    }

    /**
     * Returns the enumeration value from the given GPX name, or {@code null} if none.
     * Recognized values are {@code "none"}, {@code "2d"}, {@code "3d"}, {@code "dgps"}
     * and {@code "pps"}, ignoring case.
     *
     * @param  name  the GPX name (case insensitive) for which to get an enumeration value.
     * @return the enumeration value for the given GPX name, or {@code null} if the given name
     *         was {@code null} or unrecognized.
     */
    public static Fix fromGPX(final String name) {
        if (name == null) return null;
        return VALUES.get(name.toLowerCase(Locale.US));
    }

    /**
     * Returns the string representation in <cite>GPS Exchange Format</cite> (GPX).
     * Returned value can be {@code "none"}, {@code "2d"}, {@code "3d"}, {@code "dgps"} or {@code "pps"},
     *
     * @return the GPX enumeration value.
     */
    public String toGPX() {
        return gpx;
    }
}
