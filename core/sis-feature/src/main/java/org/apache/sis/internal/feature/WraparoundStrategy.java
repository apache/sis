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
package org.apache.sis.internal.feature;


/**
 * The strategy to use for resolving the problem of an envelope spanning the anti-meridian.
 *
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public enum WraparoundStrategy {
    /**
     * Convert the coordinates without checking the anti-meridian.
     * If the envelope crosses the anti-meridian (lower corner values {@literal >} upper corner values),
     * the created polygon will be wrong since it will define a different area than the envelope.
     * Use this method only knowing that the envelopes do not cross the anti-meridian.
     *
     * <p>Example:</p>
     * {@preformat wkt
     *   ENV(+170 +10,  -170 -10)
     *   POLYGON(+170 +10,  -170 +10,  -170 -10,  +170 -10,  +170 +10)
     * }
     */
    NONE,

    /**
     * Convert the coordinates checking the anti-meridian.
     * If the envelope crosses the anti-meridian (lower corner values {@literal >} upper corner values),
     * the created polygon will go from axis minimum value to axis maximum value.
     * This ensures that the created polygon contains the envelope but is wider.
     *
     * <p>Example:</p>
     * {@preformat wkt
     *   ENV(+170 +10,  -170 -10)
     *   POLYGON(-180 +10,  +180 +10,  +180 -10,  -180 -10,  -180 +10)
     * }
     */
    EXPAND,

    /**
     * Convert the coordinates checking the anti-meridian.
     * If the envelope crosses the anti-meridian (lower corner values {@literal >} upper corner values),
     * the created polygon will be cut in 2 polygons on each side of the coordinate system.
     * This ensures that the created polygon exactly match the envelope but with a more complex geometry.
     *
     * <p>Example:</p>
     * {@preformat wkt
     *   ENV(+170 +10,  -170 -10)
     *   MULTI-POLYGON(
     *       (-180 +10,  -170 +10,  -170 -10,  -180 -10,  -180 +10)
     *       (+170 +10,  +180 +10,  +180 -10,  +170 -10,  +170 +10))
     * }
     */
    SPLIT,

    /**
     * Convert the coordinates checking the anti-meridian.
     * If the envelope crosses the anti-meridian (lower corner values {@literal >} upper corner values),
     * the created polygon coordinate will increase over the anti-meridian making a contiguous geometry.
     *
     * <p>Example:</p>
     * {@preformat wkt
     *   ENV(+170 +10,  -170 -10)
     *   POLYGON(+170 +10,  +190 +10,  +190 -10,  +170 -10,  +170 +10)
     * }
     */
    CONTIGUOUS
}
