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
package org.apache.sis.storage.geopose;

import java.util.HashMap;
import java.util.Map;
import org.apache.sis.storage.geopose.binding.FrameSpecification;


/**
 * Interprets the parameter string of a GeoPose frame specification.
 *
 * <p>The {@code /geopose/1.0} authority encodes the origin of a local tangent plane frame as
 * an ampersand separated list of assignments, as in
 * {@code "longitude=-122.3000000&latitude=47.7000000&height=11.000"}.</p>
 *
 * <ul>
 *   <li>Only the {@code LTP-ENU} identifier of the {@code /geopose/1.0} authority is understood.</li>
 *   <li>The {@code longitude}, {@code latitude} and {@code height} keys are all required.</li>
 *   <li>Key order is not significant and surrounding spaces are ignored.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see "OGC 21-056r11 GeoPose 1.0, definition FrameSpecification"
 */
public class FrameSpecificationParser {

    /**
     * Authority of the frame specifications defined by the GeoPose standard itself.
     */
    public static final String GEOPOSE_AUTHORITY = "/geopose/1.0";

    /**
     * Identifier of the local tangent plane east-north-up frame.
     */
    public static final String LTP_ENU = "LTP-ENU";

    /**
     * Creates a new parser.
     */
    public FrameSpecificationParser() {
    }

    /**
     * Returns the local tangent plane described by the given frame specification.
     *
     * @param  specification  the frame specification to interpret
     * @return the local tangent plane origin
     * @throws IllegalArgumentException if the authority or identifier is not understood,
     *         or if a required key is missing or not a number
     */
    public LocalTangentPlane parse(final FrameSpecification specification) {
        if (!GEOPOSE_AUTHORITY.equals(specification.getAuthority())) {
            throw new IllegalArgumentException("Unsupported frame authority: " + specification.getAuthority());
        }
        if (!LTP_ENU.equals(specification.getId())) {
            throw new IllegalArgumentException("Unsupported frame identifier: " + specification.getId());
        }
        final Map<String,String> parameters = split(specification.getParameters());
        return new LocalTangentPlane(
                value(parameters, "latitude"),
                value(parameters, "longitude"),
                value(parameters, "height"));
    }

    /**
     * Splits the parameter string into its key and value pairs.
     */
    private static Map<String,String> split(final String parameters) {
        final Map<String,String> map = new HashMap<>();
        if (parameters != null) {
            for (final String pair : parameters.split("&")) {
                final int separator = pair.indexOf('=');
                if (separator >= 0) {
                    map.put(pair.substring(0, separator).trim(),
                            pair.substring(separator + 1).trim());
                }
            }
        }
        return map;
    }

    /**
     * Returns the numeric value associated to the given key.
     */
    private static double value(final Map<String,String> parameters, final String key) {
        final String text = parameters.get(key);
        if (text == null) {
            throw new IllegalArgumentException("Missing \"" + key + "\" in frame parameters.");
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value of \"" + key + "\" is not a number: " + text, e);
        }
    }
}
